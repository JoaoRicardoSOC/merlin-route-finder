// Service for Catalog & Sections exploration (ListarSecoesUseCase & SecaoResponse)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

// Sector thematic metadata (icons, colors, and corridor descriptions)
export const SECTOR_METADATA = {
  'Tintas': {
    icon: 'format_paint',
    color: '#0284c7',
    bgLight: '#e0f2fe',
    corredor: 'Corredor C01-C03',
    descricao: 'Tintas imobiliárias, vernizes, rolos e acessórios de pintura'
  },
  'Ferragens': {
    icon: 'hardware',
    color: '#64748b',
    bgLight: '#f1f5f9',
    corredor: 'Corredor B01-B03',
    descricao: 'Fechaduras, dobradiças, parafusos, puxadores e trilhos'
  },
  'Elétrica': {
    icon: 'electrical_services',
    color: '#f59e0b',
    bgLight: '#fef3c7',
    corredor: 'Corredor A14-A16',
    descricao: 'Disjuntores, fios, quadros elétricos, tomadas e conduítes'
  },
  'Encanamento': {
    icon: 'plumbing',
    color: '#0ea5e9',
    bgLight: '#e0f2fe',
    corredor: 'Corredor B04-B06',
    descricao: 'Tubos, conexões PVC, registros, ralos e sifões'
  },
  'Cozinhas': {
    icon: 'countertops',
    color: '#d97706',
    bgLight: '#fef3c7',
    corredor: 'Corredor D01-D04',
    descricao: 'Pias, cubas, torneiras gourmet, gabinetes e módulos'
  },
  'Iluminação': {
    icon: 'lightbulb',
    color: '#16a34a',
    bgLight: '#dcfce7',
    corredor: 'Corredor A12-A13',
    descricao: 'Lâmpadas LED, plafons, fitas smart, lustres e arandelas'
  },
  'Jardim': {
    icon: 'potted_plant',
    color: '#15803d',
    bgLight: '#dcfce7',
    corredor: 'Corredor E01-E03',
    descricao: 'Mangueiras, vasos, adubos, ferramentas de jardinagem e regadores'
  },
  'Ferramentas': {
    icon: 'handyman',
    color: '#ea580c',
    bgLight: '#ffedd5',
    corredor: 'Corredor A08-A11',
    descricao: 'Furadeiras, serras, jogos de chaves, trenas e EPIs'
  },
  'Decoração': {
    icon: 'brush',
    color: '#9333ea',
    bgLight: '#f3e8ff',
    corredor: 'Corredor C04-C06',
    descricao: 'Quadros, espelhos, tapetes, cortinas e almofadas'
  },
  'Materiais de construção': {
    icon: 'foundation',
    color: '#b91c1c',
    bgLight: '#fee2e2',
    corredor: 'Corredor F01-F05',
    descricao: 'Cimento, argamassa, tijolos, impermeabilizantes e gesso'
  }
}

export const DEFAULT_SECTOR_META = {
  icon: 'category',
  color: '#006b27',
  bgLight: '#e8f5e9',
  corredor: 'Loja',
  descricao: 'Itens do catálogo Leroy Merlin'
}

// Fallback sections dataset mirroring backend's ListarSecoesUseCase response
const FALLBACK_SECOES = [
  { nome: 'Tintas', quantidadeProdutos: 12 },
  { nome: 'Ferragens', quantidadeProdutos: 11 },
  { nome: 'Elétrica', quantidadeProdutos: 11 },
  { nome: 'Encanamento', quantidadeProdutos: 12 },
  { nome: 'Cozinhas', quantidadeProdutos: 10 },
  { nome: 'Iluminação', quantidadeProdutos: 11 },
  { nome: 'Jardim', quantidadeProdutos: 11 },
  { nome: 'Ferramentas', quantidadeProdutos: 12 },
  { nome: 'Decoração', quantidadeProdutos: 10 },
  { nome: 'Materiais de construção', quantidadeProdutos: 11 }
]

// Fallback products dataset mirroring CatalogoDaMassa
const FALLBACK_PRODUTOS = [
  // Iluminação
  {
    id: 'prod-ilu-01',
    sku: 'ILU-001',
    nome: 'Lâmpada LED Bulbo 9W 6500K Bivolt',
    descricao: 'Lâmpada LED econômica com luz branca fria de alta eficiência e vida útil de 25.000h.',
    preco: 14.90,
    saldoEstoque: 12,
    secao: 'Iluminação',
    corredor: 'Corredor A12',
    tag: 'Mais Vendido'
  },
  {
    id: 'prod-ilu-02',
    sku: 'ILU-002',
    nome: 'Fita LED Smart RGB 5 Metros Wi-Fi',
    descricao: 'Fita inteligente compatível com Alexa e Google Assistant com 16 milhões de cores.',
    preco: 89.90,
    saldoEstoque: 6,
    secao: 'Iluminação',
    corredor: 'Corredor A12',
    tag: 'Smart Home'
  },
  {
    id: 'prod-ilu-03',
    sku: 'ILU-003',
    nome: 'Plafon LED Sobrepor Quadrado 24W 4000K',
    descricao: 'Plafon moderno em alumínio branco com luz neutra, ideal para salas e cozinhas.',
    preco: 49.90,
    saldoEstoque: 9,
    secao: 'Iluminação',
    corredor: 'Corredor A13',
    tag: 'Recomendado'
  },
  // Tintas
  {
    id: 'prod-tin-01',
    sku: 'TIN-001',
    nome: 'Tinta Acrílica Fosca Branco Neve 18L',
    descricao: 'Tinta premium lavável para ambientes internos e externos, alto rendimento.',
    preco: 389.90,
    saldoEstoque: 15,
    secao: 'Tintas',
    corredor: 'Corredor C01',
    tag: 'Destaque'
  },
  {
    id: 'prod-tin-02',
    sku: 'TIN-002',
    nome: 'Rolo de Pintura Antigota 23cm com Cabo',
    descricao: 'Rolo de microfibra sintética ideal para tintas acrílicas e látex sem respingos.',
    preco: 22.50,
    saldoEstoque: 18,
    secao: 'Tintas',
    corredor: 'Corredor C02',
    tag: 'Essencial'
  },
  {
    id: 'prod-tin-03',
    sku: 'TIN-003',
    nome: 'Lixa para Madeira Grão 120 (Folha)',
    descricao: 'Lixa para acabamento fino em madeira e nivelamento de massa.',
    preco: 2.90,
    saldoEstoque: 0,
    secao: 'Tintas',
    corredor: 'Corredor C03',
    tag: 'Ruptura (Simulação)'
  },
  // Ferramentas
  {
    id: 'prod-fer-01',
    sku: 'FER-001',
    nome: 'Parafusadeira e Furadeira de Impacto 12V',
    descricao: 'Bateria de lítio, mandril de aperto rápido, 2 velocidades e luz de LED integrada.',
    preco: 299.90,
    saldoEstoque: 8,
    secao: 'Ferramentas',
    corredor: 'Corredor A08',
    tag: 'Oferta Especial'
  },
  {
    id: 'prod-fer-02',
    sku: 'FER-002',
    nome: 'Trena Manual Emborrachada 5m com Trava',
    descricao: 'Fita de aço fosco antirreflexo com ponta magnética e corpo anatômico antichoque.',
    preco: 24.90,
    saldoEstoque: 22,
    secao: 'Ferramentas',
    corredor: 'Corredor A09',
    tag: 'Mais Vendido'
  },
  // Elétrica
  {
    id: 'prod-ele-01',
    sku: 'ELE-001',
    nome: 'Disjuntor Bipolar Din 32A Curva C',
    descricao: 'Proteção confiável contra sobrecarga e curto-circuito em redes residenciais.',
    preco: 34.90,
    saldoEstoque: 20,
    secao: 'Elétrica',
    corredor: 'Corredor A14',
    tag: 'Segurança'
  },
  {
    id: 'prod-ele-02',
    sku: 'ELE-002',
    nome: 'Cabo Flexível 2,5mm² 750V Rolo 100m Azul',
    descricao: 'Condutor de cobre eletrolítico antichama para instalações elétricas seguras.',
    preco: 189.90,
    saldoEstoque: 14,
    secao: 'Elétrica',
    corredor: 'Corredor A15',
    tag: 'Destaque'
  },
  // Encanamento
  {
    id: 'prod-enc-01',
    sku: 'ENC-001',
    nome: 'Tubo Soldável PVC 25mm (3/4") Barra 3m',
    descricao: 'Tubo para condução de água fria predial com alta resistência e fácil soldagem.',
    preco: 18.90,
    saldoEstoque: 35,
    secao: 'Encanamento',
    corredor: 'Corredor B04',
    tag: 'Construção'
  },
  // Ferragens
  {
    id: 'prod-frg-01',
    sku: 'FRG-001',
    nome: 'Fechadura Externa de Embutir Inox Escovado',
    descricao: 'Fechadura de alta segurança com cilindro monobloco e maçaneta anatômica.',
    preco: 129.90,
    saldoEstoque: 11,
    secao: 'Ferragens',
    corredor: 'Corredor B01',
    tag: 'Qualidade'
  },
  // Cozinhas
  {
    id: 'prod-coz-01',
    sku: 'COZ-001',
    nome: 'Torneira Gourmet Monocomando Extensível Inox',
    descricao: 'Ducha flexível com 2 jatos, rotação 360° e acabamento anticorrosivo.',
    preco: 249.90,
    saldoEstoque: 7,
    secao: 'Cozinhas',
    corredor: 'Corredor D02',
    tag: 'Design Moderno'
  },
  // Jardim
  {
    id: 'prod-jar-01',
    sku: 'JAR-001',
    nome: 'Mangueira de Jardim Flexível Trançada 20m',
    descricao: 'Acompanha esguicho regulável e conexões de engate rápido com proteção UV.',
    preco: 59.90,
    saldoEstoque: 16,
    secao: 'Jardim',
    corredor: 'Corredor E02',
    tag: 'Jardinagem'
  },
  // Decoração
  {
    id: 'prod-dec-01',
    sku: 'DEC-001',
    nome: 'Espelho Redondo Adnet com Alça de Couro 60cm',
    descricao: 'Design contemporâneo com moldura em alumínio preto e fivela regulável.',
    preco: 119.90,
    saldoEstoque: 9,
    secao: 'Decoração',
    corredor: 'Corredor C05',
    tag: 'Tendência'
  },
  // Materiais de Construção
  {
    id: 'prod-mat-01',
    sku: 'MAT-001',
    nome: 'Cimento CP II-E-32 Todas as Obras 50kg',
    descricao: 'Cimento versátil de alta resistência e secagem rápida para concreto e argamassa.',
    preco: 36.90,
    saldoEstoque: 40,
    secao: 'Materiais de construção',
    corredor: 'Corredor F01',
    tag: 'Básico da Obra'
  }
]

/**
 * Lists physical catalog sections with product count (GET /api/v1/produtos/secoes)
 * Backed by backend's ListarSecoesUseCase -> List<SecaoResponse>
 */
export async function fetchSecoes() {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/produtos/secoes`, {
      headers: {
        'Accept': 'application/json'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP error ${response.status} ao buscar seções`)
    }

    const data = await response.json()
    if (Array.isArray(data) && data.length > 0) {
      return data
    }
    return FALLBACK_SECOES
  } catch (err) {
    console.warn('API /produtos/secoes indisponível, usando catálogo local:', err.message)
    return FALLBACK_SECOES
  }
}

/**
 * Searches and filters catalog products (GET /api/v1/produtos)
 * Supports secao, query, apenasDisponiveis, page, size
 */
export async function fetchProdutos({ query = '', secao = '', apenasDisponiveis = false, page = 0, size = 50 } = {}) {
  try {
    const params = new URLSearchParams()
    if (query && query.trim() !== '') params.append('query', query.trim())
    if (secao && secao.trim() !== '' && secao !== 'todos') params.append('secao', secao.trim())
    if (apenasDisponiveis) params.append('apenasDisponiveis', 'true')
    params.append('page', page.toString())
    params.append('size', size.toString())

    const response = await fetch(`${API_BASE_URL}/api/v1/produtos?${params.toString()}`, {
      headers: {
        'Accept': 'application/json'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP error ${response.status} ao buscar produtos`)
    }

    const data = await response.json()
    return {
      content: data.content || [],
      page: data.page || 0,
      size: data.size || size,
      totalElements: data.totalElements ?? (data.content ? data.content.length : 0),
      totalPages: data.totalPages || 1,
      facetas: data.facetas || []
    }
  } catch (err) {
    console.warn('API /produtos indisponível, usando filtro local:', err.message)
    
    // Filter fallback data locally
    let filtered = [...FALLBACK_PRODUTOS]

    if (secao && secao.trim() !== '' && secao !== 'todos') {
      const targetSecao = secao.trim().toLowerCase()
      filtered = filtered.filter(p => p.secao && p.secao.toLowerCase() === targetSecao)
    }

    if (query && query.trim() !== '') {
      const q = query.trim().toLowerCase()
      filtered = filtered.filter(p => 
        (p.nome && p.nome.toLowerCase().includes(q)) ||
        (p.descricao && p.descricao.toLowerCase().includes(q)) ||
        (p.corredor && p.corredor.toLowerCase().includes(q)) ||
        (p.sku && p.sku.toLowerCase().includes(q))
      )
    }

    if (apenasDisponiveis) {
      filtered = filtered.filter(p => p.saldoEstoque > 0)
    }

    return {
      content: filtered,
      page: 0,
      size: size,
      totalElements: filtered.length,
      totalPages: 1,
      facetas: []
    }
  }
}

/**
 * Gets product details by ID (GET /api/v1/produtos/{id})
 */
export async function fetchProdutoDetalhe(produtoId) {
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/produtos/${produtoId}`)
    if (!response.ok) {
      throw new Error(`HTTP error ${response.status} ao buscar detalhe do produto`)
    }
    return await response.json()
  } catch (err) {
    console.warn('API /produtos/{id} indisponível, buscando no fallback:', err.message)
    return FALLBACK_PRODUTOS.find(p => p.id === produtoId) || null
  }
}
