// Catálogo da loja: seções, busca de produtos e detalhe.
//
// **Estas funções lançam quando a API não responde.** Devolver lista vazia faria a tela dizer
// "nenhum produto encontrado", que afirma *procuramos e não há* quando a verdade é *não
// conseguimos procurar* — e quem chama precisa saber a diferença. Ver D-77.

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * GET /api/v1/produtos/secoes
 *
 * Lista vazia é resposta legítima, não falha — antes era confundida com erro.
 */
export async function fetchSecoes() {
  const response = await fetch(`${API_BASE_URL}/api/v1/produtos/secoes`, {
    headers: { Accept: 'application/json' }
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ao buscar seções`)
  }

  const data = await response.json()
  return Array.isArray(data) ? data : []
}

/**
 * As características viajam como `atributo=CHAVE:valor` repetido, e aceitam os dois formatos
 * que a tela usa: o mapa de seleção das facetas e a lista já pronta.
 */
function parametrosDaBusca({ query, secao, apenasDisponiveis, atributos, page, size }) {
  const params = new URLSearchParams()

  if (query && query.trim() !== '') params.append('query', query.trim())
  if (secao && secao.trim() !== '' && secao !== 'todos') params.append('secao', secao.trim())
  if (apenasDisponiveis) params.append('apenasDisponiveis', 'true')

  if (Array.isArray(atributos)) {
    atributos.forEach(item => {
      if (item && item.includes(':')) params.append('atributo', item)
    })
  } else if (atributos && typeof atributos === 'object') {
    Object.entries(atributos).forEach(([chave, valores]) => {
      const lista = Array.isArray(valores) ? valores : [valores]
      lista.forEach(valor => {
        if (valor) params.append('atributo', `${chave}:${valor}`)
      })
    })
  }

  params.append('page', String(page))
  params.append('size', String(size))
  return params
}

/**
 * Busca paginada do catálogo, com filtro por seção, disponibilidade e características.
 * GET /api/v1/produtos
 */
export async function fetchProdutos({
  query = '',
  secao = '',
  apenasDisponiveis = false,
  atributos = {},
  page = 0,
  size = 50
} = {}) {
  const params = parametrosDaBusca({ query, secao, apenasDisponiveis, atributos, page, size })

  const response = await fetch(`${API_BASE_URL}/api/v1/produtos?${params.toString()}`, {
    headers: { Accept: 'application/json' }
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ao buscar produtos`)
  }

  const data = await response.json()
  const content = data.content || []

  return {
    content,
    page: data.page || 0,
    size: data.size || size,
    totalElements: data.totalElements ?? content.length,
    totalPages: data.totalPages || 1,
    facetas: data.facetas || []
  }
}

/**
 * Detalhe de um produto, com descrição e características.
 * GET /api/v1/produtos/{id}
 *
 * Quem chama já parte do produto que a listagem entregou e usa esta resposta para enriquecê-lo,
 * então a falha aqui é menos grave: a tela continua mostrando o que já tinha em mãos.
 */
export async function fetchProdutoDetalhe(produtoId) {
  const response = await fetch(`${API_BASE_URL}/api/v1/produtos/${produtoId}`, {
    headers: { Accept: 'application/json' }
  })

  if (!response.ok) {
    throw new Error(`HTTP ${response.status} ao buscar o detalhe do produto`)
  }

  return await response.json()
}
