// Roteiro do cliente: POST/GET/DELETE em /api/v1/sessoes/{sessaoId}/roteiro/itens.
//
// O item do roteiro carrega DUAS identidades, e elas não são a mesma coisa:
//
//   id         identidade local, estável desde que o item aparece na tela. É a chave de React
//              e o que os componentes passam de volta nas ações.
//   idBackend  id do item no servidor. Nulo enquanto o POST não respondeu, ou se ele falhou.
//
// Antes as duas viviam no mesmo campo, e daí vinha um defeito que não parecia defeito: o item
// nascia com um id inventado ('item-' + produtoId), o POST era feito e a RESPOSTA DESCARTADA,
// e as ações seguintes só chamavam o servidor se o id não começasse com 'item-'. Como sempre
// começava, marcar coletado e remover nunca saíam da tela - não às vezes, nunca.

import {
  drenarFila,
  enfileirarColeta,
  lerFila,
  limparFila
} from './filaDeSincronizacao'
import { comSessao, podeSerFimDeSessao, renovarSePreciso } from './sessaoViva'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const ROTEIRO_STORAGE_KEY = 'merlin_route_finder_roteiro_itens'

const FORMATO_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

/** Um item que ainda não existe no servidor não pode ser coletado nem removido lá. */
function sincronizado(item) {
  return Boolean(item && item.idBackend)
}

/**
 * Converte um item vindo da API para a forma local.
 * Usado tanto pelo GET do roteiro quanto pela resposta do POST - as duas têm a mesma forma.
 */
function daApi(item) {
  return {
    id: item.id,
    idBackend: item.id,
    produtoId: item.produtoId,
    coletado: item.coletado,
    nome: item.produto?.nome,
    preco: item.produto?.preco,
    corredor: item.produto?.corredor || null,
    imagemUrl: item.produto?.imagemUrl,
    sku: item.produto?.sku
  }
}

/**
 * Lê os itens guardados no aparelho.
 *
 * Migra o formato antigo: quem já usou o app tem itens gravados sem `idBackend`. Quando o `id`
 * tem forma de UUID ele veio do servidor, então é também o id de lá. Sem esta linha, esses
 * itens perderiam a capacidade de sincronizar até o próximo carregamento - regressão que
 * ninguém veria acontecer.
 */
export function getLocalRoteiro() {
  try {
    const data = localStorage.getItem(ROTEIRO_STORAGE_KEY)
    const itens = data ? JSON.parse(data) : []

    return itens.map(item => ({
      ...item,
      idBackend: item.idBackend ?? (FORMATO_UUID.test(item.id) ? item.id : null)
    }))
  } catch {
    // localStorage indisponivel ou conteudo corrompido: lista vazia e um estado valido.
    return []
  }
}

/**
 * Saves local roteiro items
 */
export function saveLocalRoteiro(itens) {
  try {
    localStorage.setItem(ROTEIRO_STORAGE_KEY, JSON.stringify(itens))
  } catch (e) {
    console.warn('Erro ao salvar roteiro local:', e)
  }
}

/**
 * Lê, transforma e grava a lista num único passo síncrono.
 *
 * Ser síncrono é o ponto: sem `await` entre a leitura e a escrita, o ciclo é atômico frente a
 * qualquer outra ação do usuário, porque JavaScript é single-thread. Duas adições rápidas não
 * têm como sobrescrever uma à outra - a rede acontece antes ou depois, nunca no meio.
 */
function atualizarItens(transformar) {
  const atualizados = transformar(getLocalRoteiro())
  saveLocalRoteiro(atualizados)
  return atualizados
}

/** Põe um produto na lista do servidor. Usado pela adição e pela readoção. */
function postarItem(sessaoId, produtoId) {
  return fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/roteiro/itens`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
    body: JSON.stringify({ produtoId })
  })
}

/**
 * Refaz uma ação de ITEM quando a recusa foi a sessão ter acabado.
 *
 * As rotas de coleta, ruptura e substituição endereçam o item, não a sessão, então não dá para
 * usar `comSessao` aqui: o id novo não entra na URL, ele vem do item REPOSTO. Por isso a
 * repetição relê o `idBackend` da lista, que a readoção já atualizou.
 */
async function comItemVivo(alvo, chamar) {
  const resposta = await chamar(alvo.idBackend)
  if (!podeSerFimDeSessao(resposta.status)) return resposta

  const { sessao } = await renovarSePreciso()
  if (!sessao) return resposta

  const renascido = getLocalRoteiro().find(i => i.id === alvo.id)
  if (!sincronizado(renascido) || renascido.idBackend === alvo.idBackend) return resposta

  return chamar(renascido.idBackend)
}

/**
 * Repõe a lista que está na tela numa sessão recém-aberta.
 *
 * Chamada pelo `App` quando a sessão é renovada. Sem ela, a renovação salvaria o chat e
 * custaria a lista: os itens continuariam desenhados, mas com `idBackend` de uma sessão que não
 * existe mais — marcar, remover e relatar ruptura falhariam em silêncio até o próximo
 * recarregamento apagar tudo.
 *
 * Em série, e na ordem da tela: é a ordem que o cliente montou, e o servidor guarda a ordem de
 * chegada. O item que falhar fica com `idBackend` nulo, que é o estado que `sincronizado()` já
 * sabe tratar — ele continua na lista, só não sincroniza.
 */
export async function readotarRoteiro(sessaoId) {
  const locais = getLocalRoteiro()
  if (!sessaoId || locais.length === 0) return locais

  /*
   * A fila vai embora ANTES da reposição, não depois.
   *
   * O que estava nela guarda `idBackend` da sessão que morreu — endereços que não levam a
   * lugar nenhum. E limpar depois apagaria justamente o que a reposição acabou de enfileirar.
   */
  limparFila()

  const readotados = []
  for (const item of locais) {
    if (!item.produtoId) {
      readotados.push({ ...item, idBackend: null })
      continue
    }

    try {
      const resposta = await postarItem(sessaoId, item.produtoId)
      if (!resposta.ok) {
        throw new Error(`HTTP ${resposta.status} ao readotar item`)
      }
      const salvo = await resposta.json()

      /*
       * A marca de coletado não viaja no POST: o item nasce por coletar dos dois lados, e
       * reaplicá-la aqui é o que impede o cliente de ver a própria marca sumir.
       *
       * Num `try` próprio de propósito: se só a marca falhar, o item JÁ está na sessão nova, e
       * perder o `idBackend` por causa disso deixaria órfão um item que existe no servidor. A
       * intenção vai para a fila, que é o caminho que já existe para marca que não chegou.
       */
      if (item.coletado) {
        try {
          await enviarColeta(salvo.id, true)
        } catch (erroDaMarca) {
          console.warn('A marca de coletado não acompanhou o item:', erroDaMarca?.message)
          enfileirarColeta(salvo.id, true)
        }
      }

      readotados.push({ ...item, idBackend: salvo.id, coletado: item.coletado })
    } catch (erro) {
      console.warn('Item não voltou para a sessão nova:', item.nome, erro?.message)
      readotados.push({ ...item, idBackend: null })
    }
  }

  saveLocalRoteiro(readotados)
  return readotados
}

/**
 * Consults the roteiro from backend (GET /api/v1/sessoes/{sessaoId}/roteiro)
 */
export async function consultarRoteiro(sessaoId) {
  if (!sessaoId) return getLocalRoteiro()

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/roteiro`, {
      headers: { 'Accept': 'application/json' }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status} ao consultar roteiro`)
    }

    const data = await response.json()
    const itens = comPendenciasPorCima((data.itens || []).map(daApi))
    saveLocalRoteiro(itens)
    return itens
  } catch (err) {
    console.warn('API /roteiro indisponível, usando lista local:', err.message)
    return getLocalRoteiro()
  }
}

/**
 * Acrescenta um produto ao roteiro (POST /api/v1/sessoes/{sessaoId}/roteiro/itens).
 *
 * Insere primeiro e sincroniza depois, para a lista responder na hora mesmo com a conexão ruim
 * de dentro de uma loja. O item entra com id temporário e `idBackend` nulo; quando o servidor
 * responde, o id de lá é gravado no item que já está na tela.
 */
export async function adicionarAoRoteiro(sessaoId, product) {
  const produtoId = product.id

  const jaEstaNaLista = getLocalRoteiro()
    .some(i => i.produtoId === produtoId || i.id === produtoId)
  if (jaEstaNaLista) {
    return getLocalRoteiro()
  }

  const itens = atualizarItens(atuais => [...atuais, {
    id: 'item-' + (produtoId || Math.random().toString(36).substring(2, 9)),
    idBackend: null,
    produtoId,
    coletado: false,
    nome: product.nome || product.name,
    preco: product.preco ?? product.price ?? 0,
    corredor: product.corredor || null,
    imagemUrl: product.imagemUrl || product.image || null,
    sku: product.sku || ''
  }])

  const podeSincronizar = sessaoId && produtoId
    && !sessaoId.startsWith('sess-') && !produtoId.startsWith('prod-')
  if (!podeSincronizar) {
    return itens
  }

  try {
    const { resposta: response } = await comSessao(sessaoId, id => postarItem(id, produtoId))

    if (!response.ok) {
      throw new Error(`HTTP ${response.status} ao adicionar item`)
    }

    // Casa por produtoId, e não por posição: é o que sobrevive a duas adições simultâneas. E o
    // backend devolve o item existente quando o produto já está na lista (D-18), então
    // reconciliar nunca duplica.
    const salvo = await response.json()
    return atualizarItens(atuais => atuais.map(
      item => item.produtoId === produtoId ? { ...item, ...daApi(salvo), id: item.id } : item
    ))
  } catch (e) {
    // O item continua na lista, sem id do servidor. As ações seguintes simplesmente não
    // tentam sincronizar, em vez de tentar com um id que não existe lá.
    console.warn('Item adicionado apenas localmente:', e.message)
    return getLocalRoteiro()
  }
}

/**
 * Remove um item do roteiro (DELETE /api/v1/sessoes/{sessaoId}/roteiro/itens/{itemId}).
 */
export async function removerDoRoteiro(sessaoId, itemId) {
  // Resolve o id do servidor ANTES de tirar da lista: depois de remover, não há mais onde ler.
  const alvo = getLocalRoteiro().find(i => i.id === itemId || i.produtoId === itemId)
  const itens = atualizarItens(atuais =>
    atuais.filter(i => i.id !== itemId && i.produtoId !== itemId))

  if (!sessaoId || sessaoId.startsWith('sess-') || !sincronizado(alvo)) {
    return itens
  }

  try {
    // O item já saiu da lista local, então a readoção não o repõe: se a sessão tiver acabado, a
    // repetição encontra uma sessão nova que legitimamente não tem esse item, e o 404 dela é
    // tão inofensivo quanto o sucesso.
    await comSessao(sessaoId, id => fetch(
      `${API_BASE_URL}/api/v1/sessoes/${id}/roteiro/itens/${alvo.idBackend}`,
      { method: 'DELETE' }))
  } catch (e) {
    console.warn('Item removido apenas localmente:', e.message)
  }

  return itens
}

/**
 * Marca ou desmarca um item como coletado
 * (PATCH /api/v1/roteiro/itens/{itemId}/coletar | /desmarcar).
 *
 * As duas rotas são idempotentes no backend, então repetir o toque não desalinha nada.
 */
/**
 * Devolve a lista do servidor com as marcações que ainda não chegaram lá por cima.
 *
 * Sem isto, a reconciliação desfaz na tela o que o cliente acabou de marcar sem sinal: o
 * servidor responde o estado antigo — que é o estado correto do ponto de vista dele — e a
 * marca some. O cliente veria a própria ação ser revertida sem explicação.
 */
function comPendenciasPorCima(itens) {
  const fila = lerFila()
  if (fila.length === 0) return itens
  const porItem = new Map(fila.map(p => [p.idBackend, p.coletado]))
  return itens.map(i => (porItem.has(i.idBackend)
    ? { ...i, coletado: porItem.get(i.idBackend) }
    : i))
}

/**
 * Fala com a API de coleta. Lança marcando se a recusa foi do servidor ou da rede, porque a
 * fila trata os dois de formas opostas: recusa do servidor sai da fila, falta de rede espera.
 */
async function enviarColeta(idBackend, coletado) {
  const acao = coletado ? 'coletar' : 'desmarcar'
  const response = await fetch(`${API_BASE_URL}/api/v1/roteiro/itens/${idBackend}/${acao}`, {
    method: 'PATCH',
    headers: { Accept: 'application/json' }
  })

  if (!response.ok) {
    const erro = new Error(`HTTP ${response.status} ao ${acao} item`)
    // O status sobe junto: quem chamou precisa distinguir "este item não existe mais" de
    // "o servidor recusou", e a mensagem não é lugar de ler isso.
    erro.status = response.status
    // 4xx é o servidor dizendo que este pedido não vale — repeti-lo não vai passar a valer.
    // 408 e 429 são exceção: os dois pedem justamente para tentar de novo mais tarde.
    erro.recusadoPeloServidor =
      response.status >= 400 && response.status < 500 &&
      response.status !== 408 && response.status !== 429
    throw erro
  }
}

/**
 * Reenvia o que ficou pendente. Chamada quando a conexão volta e ao reabrir a aba.
 *
 * @returns {Promise<{enviadas: number, descartadas: number, restantes: number}>}
 */
export async function sincronizarPendencias() {
  return drenarFila(enviarColeta)
}

export async function alternarColetaItem(itemId) {
  const alvo = getLocalRoteiro().find(i => i.id === itemId || i.produtoId === itemId)
  if (!alvo) return getLocalRoteiro()

  const passaAEstarColetado = !alvo.coletado
  const itens = atualizarItens(atuais => atuais.map(
    i => (i.id === itemId || i.produtoId === itemId)
      ? { ...i, coletado: passaAEstarColetado }
      : i
  ))

  if (!sincronizado(alvo)) {
    return itens
  }

  let falha = null
  try {
    await enviarColeta(alvo.idBackend, passaAEstarColetado)
  } catch (e) {
    falha = e
  }

  // A sessão pode ter acabado: renova, relê o id que a readoção deu ao item e manda de novo.
  // As duas rotas são idempotentes, então repetir uma marca que a readoção já aplicou é inócuo.
  if (falha && podeSerFimDeSessao(falha.status)) {
    const { sessao } = await renovarSePreciso()
    const renascido = sessao
      && getLocalRoteiro().find(i => i.id === itemId || i.produtoId === itemId)

    if (sincronizado(renascido) && renascido.idBackend !== alvo.idBackend) {
      try {
        await enviarColeta(renascido.idBackend, passaAEstarColetado)
        falha = null
      } catch (segunda) {
        falha = segunda
      }
    }
  }

  if (falha) {
    const e = falha
    if (e.recusadoPeloServidor) {
      // Não entra na fila: reenviar o que o servidor recusa é fila que nunca esvazia.
      console.warn('O servidor recusou a marcação de coleta:', e.message)
    } else {
      /*
       * Antes daqui saía só um console.warn, e a marca ficava na tela para sempre sem nunca
       * chegar ao servidor — até a próxima reconciliação desfazê-la sozinha. Agora a intenção
       * fica guardada e volta a ser tentada quando a conexão voltar.
       */
      // Pelo id que o item tem AGORA: se houve renovação no meio, o de `alvo` ficou na sessão
      // morta, e uma fila apontando para lá nunca chegaria a lugar nenhum.
      const atual = getLocalRoteiro().find(i => i.id === itemId || i.produtoId === itemId)
      enfileirarColeta(atual?.idBackend || alvo.idBackend, passaAEstarColetado)
    }
  }

  // Relida do armazenamento: a readoção pode ter trocado os `idBackend` no meio do caminho.
  return getLocalRoteiro()
}

/**
 * Tempo maximo de espera pela sugestao de substituto.
 *
 * A chamada passa pelo assistente, que o backend tenta ate tres vezes antes de cair no
 * calculo de proximidade - o pior caso medido passou de trinta segundos. Sem um teto, um
 * socket pendurado deixaria o modal girando para sempre.
 *
 * Abortar aqui NAO cancela o servidor: ele pode concluir e registrar a ruptura assim mesmo.
 * Por isso a mensagem de expiracao diz que a busca demorou, e nao que falhou.
 */
const ESPERA_MAXIMA_MS = 45000

/**
 * Relata prateleira vazia e pede um substituto
 * (POST /api/v1/roteiro/itens/{itemId}/ruptura).
 *
 * Devolve um resultado discriminado, porque a tela precisa distinguir tres desfechos que nao
 * sao variacoes um do outro: houve sugestao, nao havia nada plausivel por perto, ou nao deu
 * para perguntar.
 */
export async function relatarRuptura(itemId) {
  const alvo = getLocalRoteiro().find(i => i.id === itemId || i.produtoId === itemId)

  if (!sincronizado(alvo)) {
    return {
      estado: 'erro',
      mensagem: 'Este item ainda não foi sincronizado com a loja. Verifique a conexão e tente de novo.'
    }
  }

  const relogio = new AbortController()
  const expira = setTimeout(() => relogio.abort(), ESPERA_MAXIMA_MS)

  try {
    const response = await comItemVivo(alvo, id => fetch(
      `${API_BASE_URL}/api/v1/roteiro/itens/${id}/ruptura`,
      { method: 'POST', headers: { Accept: 'application/json' }, signal: relogio.signal }))

    if (response.status === 422) {
      // A ruptura FOI registrada; o que faltou foi substituto plausivel no raio caminhavel.
      return { estado: 'sem-substituto', itemEmFalta: alvo }
    }

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const sugestao = await response.json()
    return { estado: 'sugerido', itemEmFalta: alvo, sugestao }
  } catch (e) {
    if (e.name === 'AbortError') {
      return {
        estado: 'erro',
        mensagem: 'A busca por um substituto demorou mais que o esperado. Você pode tentar de novo.'
      }
    }
    console.warn('Falha ao relatar ruptura:', e.message)
    return {
      estado: 'erro',
      mensagem: 'Não foi possível falar com a loja agora. Tente novamente em instantes.'
    }
  } finally {
    clearTimeout(expira)
  }
}

/**
 * Aceita o substituto, trocando o item da lista numa chamada so
 * (POST /api/v1/roteiro/itens/{itemId}/substituir).
 *
 * O produto vai no corpo, e nao e deduzido da sugestao: o assistente pode responder diferente
 * numa segunda chamada, e a troca precisa valer sobre o que o cliente VIU na tela.
 *
 * A lista devolvida pela API substitui a local inteira. Montar a troca aqui seria repetir o
 * erro que originou o B-1 - escrever no servidor e seguir com uma versao propria da verdade.
 */
export async function aceitarSubstituto(itemId, produtoSubstitutoId) {
  const alvo = getLocalRoteiro().find(i => i.id === itemId || i.produtoId === itemId)

  if (!sincronizado(alvo) || !produtoSubstitutoId) {
    return { ok: false, itens: getLocalRoteiro() }
  }

  try {
    const response = await comItemVivo(alvo, id => fetch(
      `${API_BASE_URL}/api/v1/roteiro/itens/${id}/substituir`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ produtoSubstitutoId })
      }))

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const lista = await response.json()
    const itens = (lista.itens || []).map(daApi)
    saveLocalRoteiro(itens)
    return { ok: true, itens }
  } catch (e) {
    console.warn('Falha ao aceitar o substituto:', e.message)
    return { ok: false, itens: getLocalRoteiro() }
  }
}

export function limparRoteiroLocal() {
  saveLocalRoteiro([])
  return []
}
