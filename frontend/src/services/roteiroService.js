// Service for Roteiro / Shopping List (POST /api/v1/sessoes/{sessaoId}/roteiro/itens, GET, DELETE)

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const ROTEIRO_STORAGE_KEY = 'merlin_route_finder_roteiro_itens'

/**
 * Gets local stored roteiro items
 */
export function getLocalRoteiro() {
  try {
    const data = localStorage.getItem(ROTEIRO_STORAGE_KEY)
    return data ? JSON.parse(data) : []
  } catch (e) {
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
    const itens = (data.itens || []).map(item => ({
      id: item.id,
      produtoId: item.produtoId,
      coletado: item.coletado,
      nome: item.produto?.nome,
      preco: item.produto?.preco,
      corredor: item.produto?.corredor || 'Corredor da Loja',
      imagemUrl: item.produto?.imagemUrl,
      sku: item.produto?.sku
    }))
    saveLocalRoteiro(itens)
    return itens
  } catch (err) {
    console.warn('API /roteiro indisponível, usando lista local:', err.message)
    return getLocalRoteiro()
  }
}

/**
 * Adds a product to the roteiro (POST /api/v1/sessoes/{sessaoId}/roteiro/itens)
 */
export async function adicionarAoRoteiro(sessaoId, product) {
  const produtoId = product.id
  let currentLocal = getLocalRoteiro()

  // Avoid duplicate entries locally
  const alreadyExists = currentLocal.some(i => i.produtoId === produtoId || i.id === product.id)
  if (alreadyExists) {
    return currentLocal
  }

  const newItem = {
    id: 'item-' + (produtoId || Math.random().toString(36).substring(2, 9)),
    produtoId: produtoId,
    coletado: false,
    nome: product.nome || product.name,
    preco: product.preco ?? product.price ?? 0,
    corredor: product.corredor || 'Corredor da Loja',
    imagemUrl: product.imagemUrl || product.image || null,
    sku: product.sku || ''
  }

  const updated = [...currentLocal, newItem]
  saveLocalRoteiro(updated)

  if (sessaoId && produtoId && !sessaoId.startsWith('sess-') && !produtoId.startsWith('prod-')) {
    try {
      await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/roteiro/itens`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({ produtoId })
      })
    } catch (e) {
      console.warn('Erro na requisição de adicionar item ao backend:', e)
    }
  }

  return updated
}

/**
 * Removes an item from the roteiro (DELETE /api/v1/sessoes/{sessaoId}/roteiro/itens/{itemId})
 */
export async function removerDoRoteiro(sessaoId, itemId) {
  let currentLocal = getLocalRoteiro()
  const updated = currentLocal.filter(i => i.id !== itemId && i.produtoId !== itemId)
  saveLocalRoteiro(updated)

  if (sessaoId && itemId && !sessaoId.startsWith('sess-') && !itemId.startsWith('item-')) {
    try {
      await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/roteiro/itens/${itemId}`, {
        method: 'DELETE'
      })
    } catch (e) {
      console.warn('Erro na requisição de remover item do backend:', e)
    }
  }

  return updated
}

/**
 * Clears all items from the roteiro
 */
export function limparRoteiroLocal() {
  saveLocalRoteiro([])
  return []
}
