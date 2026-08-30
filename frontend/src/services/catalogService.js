// Catálogo da loja: seções, busca de produtos e detalhe.
//
// Estas funções lançam quando não conseguem falar com a API, em vez de devolver alguma coisa.
// Existiam aqui 202 linhas de produtos e seções escritos à mão, devolvidas sempre que a
// chamada falhava — um catálogo inventado, com SKUs que não são os nossos e corredores que
// não existem na planta, apresentado como se fosse a loja. Aparecia exatamente quando ninguém
// tinha como conferir.
//
// Devolver lista vazia no lugar seria a mesma doença em grau menor: a tela diria "nenhum
// produto encontrado", que afirma *procuramos e não há* quando a verdade é *não conseguimos
// procurar*. Quem chama precisa saber a diferença, e só uma exceção carrega essa informação.
//
// Ver D-77.

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * Seções físicas do catálogo, com a contagem de produtos de cada uma.
 * GET /api/v1/produtos/secoes
 *
 * Lista vazia é resposta legítima, não falha: uma loja sem seção cadastrada é um estado
 * possível, e antes ele era confundido com erro.
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
 * Monta os parâmetros da busca de produtos.
 *
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
