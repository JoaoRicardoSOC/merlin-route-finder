// Service for Roteiro / Shopping List (POST /api/v1/sessoes/{sessaoId}/roteiro/itens, GET, DELETE)
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
  lerFila
} from './filaDeSincronizacao'

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
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/roteiro/itens`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({ produtoId })
    })

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
    await fetch(
      `${API_BASE_URL}/api/v1/sessoes/${sessaoId}/roteiro/itens/${alvo.idBackend}`,
      { method: 'DELETE' })
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

  try {
    await enviarColeta(alvo.idBackend, passaAEstarColetado)
  } catch (e) {
    if (e.recusadoPeloServidor) {
      // Não entra na fila: reenviar o que o servidor recusa é fila que nunca esvazia.
      console.warn('O servidor recusou a marcação de coleta:', e.message)
    } else {
      /*
       * Antes daqui saía só um console.warn, e a marca ficava na tela para sempre sem nunca
       * chegar ao servidor — até a próxima reconciliação desfazê-la sozinha. Agora a intenção
       * fica guardada e volta a ser tentada quando a conexão voltar.
       */
      enfileirarColeta(alvo.idBackend, passaAEstarColetado)
    }
  }

  return itens
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
    const response = await fetch(
      `${API_BASE_URL}/api/v1/roteiro/itens/${alvo.idBackend}/ruptura`,
      { method: 'POST', headers: { Accept: 'application/json' }, signal: relogio.signal })

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
    const response = await fetch(
      `${API_BASE_URL}/api/v1/roteiro/itens/${alvo.idBackend}/substituir`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ produtoSubstitutoId })
      })

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
