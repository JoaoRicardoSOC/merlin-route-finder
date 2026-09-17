/*
 * Quando o servidor diz que a sessão acabou, o app abre outra e refaz a chamada.
 *
 * O problema que isto resolve: a sessão tem TTL de 4 horas de inatividade ([D-24]), e o
 * frontend não tinha uma linha sequer tratando o fim dela. Uma aba esquecida de um dia para o
 * outro passava a receber `409 Sessao ... nao esta mais ativa (status EXPIRED)` em tudo —
 * chat, roteiro, coleta, ruptura — e cada tela traduzia isso para "não consegui falar com a
 * loja", que é a frase do caso em que o servidor não respondeu. O cliente lia que a loja
 * estava fora do ar enquanto ela respondia em menos de um segundo.
 *
 * **"Acabou" não é uma coisa só, e é por isso que aqui se lê o status em vez de adivinhar.**
 * O mesmo 409 cobre três situações que pedem respostas opostas:
 *
 *   EXPIRED, ABANDONED, ou sessão que sumiu (404)  -> renova calado. O cliente não pediu para
 *                                                     acabar; quem acabou foi o relógio.
 *   COMPLETED                                      -> NÃO renova. Ele encerrou de propósito, e
 *                                                     recomeçar sozinho apagaria essa decisão.
 *   ACTIVE                                         -> NÃO renova. O 409 veio de outra coisa, e
 *                                                     renovar esconderia o defeito de verdade.
 *
 * A consulta extra só acontece no caminho de falha, que é raro por definição.
 */

import { inicializarSessao } from './sessionService'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const SESSION_STORAGE_KEY = 'merlin_route_finder_session_id'
const SESSION_DATA_KEY = 'merlin_route_finder_session_data'

/** As respostas em que vale perguntar se a sessão ainda existe. */
const TALVEZ_TENHA_ACABADO = new Set([404, 409, 410])

/** Os status em que abrir outra sessão é o que o cliente esperaria. */
const MORREU_SOZINHA = new Set(['EXPIRED', 'ABANDONED'])

const ouvintes = new Set()
const ouvintesDeEncerramento = new Set()

/*
 * Uma renovação por vez.
 *
 * Duas chamadas podem falhar no mesmo instante — o chat e a lista, por exemplo. Sem esta
 * promessa compartilhada, cada uma abriria uma sessão, e a segunda sobrescreveria o id da
 * primeira no armazenamento: o cliente terminaria com uma sessão órfã no banco e metade das
 * ações apontando para ela.
 */
let renovacaoEmCurso = null

/**
 * Registra quem precisa reagir a uma troca de sessão.
 *
 * Os ouvintes são AGUARDADOS antes de a renovação terminar, e isso não é detalhe: é o que
 * garante que a lista já esteja readotada na sessão nova quando a chamada que falhou for
 * repetida. Sem a espera, a repetição sairia com o `idBackend` da sessão morta.
 *
 * @param {(sessao: object) => void | Promise<void>} fn
 * @returns {() => void} como cancelar o registro
 */
export function aoRenovar(fn) {
  ouvintes.add(fn)
  return () => ouvintes.delete(fn)
}

/**
 * Registra quem precisa saber que a jornada foi ENCERRADA — e não que ela venceu.
 *
 * São coisas diferentes para o cliente: vencer é acidente do relógio, encerrar foi escolha
 * dele na frente de caixa. Por isso este caminho não abre sessão nova sozinho; ele avisa a
 * tela, que pergunta.
 *
 * @param {() => void} fn
 * @returns {() => void} como cancelar o registro
 */
export function aoEncerrarJornada(fn) {
  ouvintesDeEncerramento.add(fn)
  return () => ouvintesDeEncerramento.delete(fn)
}

function sessaoGuardada() {
  try {
    const cru = localStorage.getItem(SESSION_DATA_KEY)
    return cru ? JSON.parse(cru) : null
  } catch {
    return null
  }
}

/**
 * O id da sessão corrente, para quem chama uma rota que não o carrega na URL.
 *
 * Coletar, relatar ruptura e aceitar substituto endereçam o ITEM, não a sessão — mas o item
 * pertence a uma, e é ela que pode ter acabado.
 */
function idGuardado() {
  try {
    return localStorage.getItem(SESSION_STORAGE_KEY)
  } catch {
    return null
  }
}

/** `true` quando a recusa pode ser o fim da sessão, e vale perguntar ao servidor. */
export function podeSerFimDeSessao(status) {
  return TALVEZ_TENHA_ACABADO.has(status)
}

/**
 * A última placa lida, para a sessão nova nascer onde o cliente está.
 *
 * O código guardado vem normalizado ("TIN02"); o backend normaliza de novo antes de procurar
 * (`PontoMapa.normalizarCodigo`), então "TIN02" e "TIN-02" chegam ao mesmo ponto. Sem isto a
 * renovação custaria ao cliente o "você está aqui" do mapa, e ele não teria como saber por quê.
 */
function ultimaPlacaConhecida() {
  return sessaoGuardada()?.posicaoAtual?.codigoCurto || null
}

/**
 * Pergunta ao servidor em que estado a sessão está.
 *
 * @returns {Promise<string|null>} o status, ou `null` se ela não existe mais lá
 */
async function statusNoServidor(sessaoId) {
  const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}`, {
    headers: { Accept: 'application/json' }
  })

  if (response.status === 404 || response.status === 410) {
    return null
  }

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ao consultar a sessão`)
  }

  return (await response.json()).status
}

async function avisarOuvintes(sessao, motivo) {
  for (const ouvinte of ouvintes) {
    try {
      await ouvinte(sessao, motivo)
    } catch (erro) {
      // Um ouvinte que falha não pode impedir a renovação: a sessão nova já existe, e a
      // alternativa seria o app continuar apontando para a antiga, que ninguém mais aceita.
      console.warn('Um ouvinte da renovação de sessão falhou:', erro?.message)
    }
  }
}

/**
 * Abre uma sessão nova e avisa quem precisa reagir.
 *
 * Exportada porque a tela de jornada encerrada também a usa: ali quem manda recomeçar é o
 * cliente, e a verificação de status não tem o que decidir.
 */
export async function abrirOutraSessao(motivo = 'expirou') {
  const sessao = await inicializarSessao(ultimaPlacaConhecida())
  await avisarOuvintes(sessao, motivo)
  return sessao
}

/**
 * Abre outra sessão se — e só se — a atual tiver acabado sem o cliente pedir.
 *
 * @returns {Promise<{sessao?: object, encerrada?: boolean}>} `sessao` quando renovou,
 *   `encerrada` quando a jornada foi concluída de propósito, objeto vazio quando não cabia
 *   renovar (sessão viva, ou não deu para saber).
 */
export async function renovarSePreciso(sessaoId = idGuardado()) {
  if (!sessaoId) return {}
  if (renovacaoEmCurso) return renovacaoEmCurso

  renovacaoEmCurso = (async () => {
    try {
      const status = await statusNoServidor(sessaoId)

      if (status === 'COMPLETED') {
        for (const ouvinte of ouvintesDeEncerramento) {
          try {
            ouvinte()
          } catch (erro) {
            console.warn('Um ouvinte do encerramento falhou:', erro?.message)
          }
        }
        return { encerrada: true }
      }

      if (status !== null && !MORREU_SOZINHA.has(status)) {
        // ACTIVE: a recusa não era sobre a sessão. Deixa a falha original aparecer.
        return {}
      }

      return { sessao: await abrirOutraSessao() }
    } catch (erro) {
      // Sem rede não há renovação possível, e insistir só atrasaria a resposta da tela. O app
      // segue no caminho local que já existe para esse caso.
      console.warn('Não deu para renovar a sessão:', erro?.message)
      return {}
    } finally {
      renovacaoEmCurso = null
    }
  })()

  return renovacaoEmCurso
}

/**
 * Faz uma chamada que depende da sessão, e a refaz UMA vez se a sessão tiver acabado.
 *
 * Uma repetição só, de propósito: se a segunda também falhar, o problema não era a sessão, e
 * repetir de novo viraria um laço que o cliente vê como tela travada.
 *
 * @param {string|null} sessaoId
 * @param {(id: string) => Promise<Response>} chamar monta e dispara a requisição
 * @returns {Promise<{resposta: Response, sessaoId: string|null, renovou: boolean,
 *   encerrada: boolean}>}
 */
export async function comSessao(sessaoId, chamar) {
  const resposta = await chamar(sessaoId)

  if (!sessaoId || !TALVEZ_TENHA_ACABADO.has(resposta.status)) {
    return { resposta, sessaoId, renovou: false, encerrada: false }
  }

  const { sessao, encerrada } = await renovarSePreciso(sessaoId)

  if (!sessao) {
    return { resposta, sessaoId, renovou: false, encerrada: Boolean(encerrada) }
  }

  return {
    resposta: await chamar(sessao.id),
    sessaoId: sessao.id,
    renovou: true,
    encerrada: false
  }
}
