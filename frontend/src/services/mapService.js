// Map Service - Leroy Merlin Interlagos Store Floor Plan & Navigation
// Integrates with backend GET /api/v1/mapa and local enriched geometry

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export const STORE_SECTORS = [
  {
    id: 'pintura',
    nome: 'Pintura',
    corredor: 'Corredor B01 - Tintas',
    icon: 'format_paint',
    color: '#3B82F6',
    x: 470,
    y: 430,
    w: 120,
    h: 80,
    secaoRef: 'Tintas',
    descricao: 'Tintas imobiliárias, vernizes, rolos e acessórios de pintura'
  },
  {
    id: 'ferramentas',
    nome: 'Ferramentas',
    corredor: 'Corredor A11 - Ferramentas',
    icon: 'handyman',
    color: '#EF4444',
    x: 510,
    y: 330,
    w: 115,
    h: 70,
    secaoRef: 'Ferramentas',
    descricao: 'Ferramentas elétricas, manuais, brocas e bancadas'
  },
  {
    id: 'madeiras',
    nome: 'Madeiras',
    corredor: 'Corredor M01 - Madeiras',
    icon: 'forest',
    color: '#8B5A2B',
    x: 650,
    y: 320,
    w: 125,
    h: 85,
    secaoRef: 'Madeiras',
    descricao: 'Chapas de MDF, compensados, vigas, painéis e corte sob medida'
  },
  {
    id: 'ferragens',
    nome: 'Ferragens',
    corredor: 'Corredor F02 - Ferragens',
    icon: 'construction',
    color: '#F59E0B',
    x: 620,
    y: 435,
    w: 110,
    h: 70,
    secaoRef: 'Ferragens',
    descricao: 'Parafusos, buchas, fechaduras, dobradiças e fixações'
  },
  {
    id: 'eletrica',
    nome: 'Elétrica',
    corredor: 'Corredor E03 - Elétrica',
    icon: 'bolt',
    color: '#F59E0B',
    x: 580,
    y: 525,
    w: 105,
    h: 70,
    secaoRef: 'Eletrica',
    descricao: 'Fios, cabos, disjuntores, conduítes e interruptores'
  },
  {
    id: 'cozinha',
    nome: 'Cozinha',
    corredor: 'Corredor K01 - Cozinhas',
    icon: 'countertops',
    color: '#10B981',
    x: 360,
    y: 590,
    w: 105,
    h: 80,
    secaoRef: 'Cozinhas',
    descricao: 'Armários planejados, pias, torneiras gourmet e depuradores'
  },
  {
    id: 'sanitario',
    nome: 'Sanitário',
    corredor: 'Corredor S02 - Sanitários',
    icon: 'bathtub',
    color: '#06B6D4',
    x: 530,
    y: 605,
    w: 100,
    h: 75,
    secaoRef: 'Sanitarios',
    descricao: 'Vasos sanitários, cubas, assentos, gabinetes e chuveiros'
  },
  {
    id: 'organizacao',
    nome: 'Organização',
    corredor: 'Corredor O01 - Organização',
    icon: 'shelves',
    color: '#8B5CF6',
    x: 320,
    y: 690,
    w: 95,
    h: 60,
    secaoRef: 'Organizacao',
    descricao: 'Prateleiras, caixas organizadoras, cabides e gaveteiros'
  },
  {
    id: 'ceramica',
    nome: 'Cerâmica',
    corredor: 'Corredor C05 - Pisos & Revestimentos',
    icon: 'grid_view',
    color: '#EC4899',
    x: 455,
    y: 720,
    w: 110,
    h: 70,
    secaoRef: 'Ceramica',
    descricao: 'Porcelanatos, revestimentos cerâmicos, argamassas e rejuntes'
  },
  {
    id: 'jardim',
    nome: 'Jardim',
    corredor: 'Corredor J01 - Jardinagem',
    icon: 'yard',
    color: '#10B981',
    x: 370,
    y: 310,
    w: 90,
    h: 160,
    secaoRef: 'Jardim',
    descricao: 'Plantas, vasos, mangueiras, aparadores e adubos'
  },
  {
    id: 'iluminacao',
    nome: 'Iluminação',
    corredor: 'Corredor A12 - Iluminação',
    icon: 'lightbulb',
    color: '#F59E0B',
    x: 160,
    y: 740,
    w: 95,
    h: 65,
    secaoRef: 'Iluminacao',
    descricao: 'Lustres, pendentes, painéis LED, spots e fitas de LED'
  },
  {
    id: 'enquadramento',
    nome: 'Enquadramento',
    corredor: 'Corredor D01 - Molduras',
    icon: 'crop_original',
    color: '#6366F1',
    x: 235,
    y: 805,
    w: 65,
    h: 55,
    secaoRef: 'Decoracao',
    descricao: 'Molduras prontas, confecção sob medida e quadros'
  },
  {
    id: 'decoracao',
    nome: 'Decoração',
    corredor: 'Corredor D03 - Decoração',
    icon: 'palette',
    color: '#8B5CF6',
    x: 295,
    y: 835,
    w: 70,
    h: 55,
    secaoRef: 'Decoracao',
    descricao: 'Almofadas, cortinas, papéis de parede e espelhos'
  },
  {
    id: 'tapetes',
    nome: 'Tapetes',
    corredor: 'Corredor T01 - Tapetes',
    icon: 'texture',
    color: '#D97706',
    x: 355,
    y: 775,
    w: 60,
    h: 50,
    secaoRef: 'Decoracao',
    descricao: 'Tapetes decorativos, passadeiras e capachos'
  },
  {
    id: 'pisos_laminados',
    nome: 'Pisos Laminados',
    corredor: 'Corredor P02 - Pisos Vinílicos & Laminados',
    icon: 'layers',
    color: '#A16207',
    x: 400,
    y: 855,
    w: 85,
    h: 55,
    secaoRef: 'Ceramica',
    descricao: 'Pisos laminados, vinílicos, rodapés e mantas'
  },
  {
    id: 'ofertas',
    nome: 'Ofertas',
    corredor: 'Área Promocional',
    icon: 'local_offer',
    color: '#EF4444',
    x: 265,
    y: 660,
    w: 50,
    h: 40,
    secaoRef: 'Ofertas',
    descricao: 'Pontas de estoque e promoções da semana'
  },
  {
    id: 'material_construcao',
    nome: 'Material de Construção',
    corredor: 'Corredor Básico C01',
    icon: 'home_repair_service',
    color: '#64748B',
    x: 520,
    y: 215,
    w: 100,
    h: 80,
    secaoRef: 'Materiais de construcao',
    descricao: 'Cimento, cal, blocos, areia, telhas e impermeabilizantes'
  },
  {
    id: 'drive_thru',
    nome: 'Drive Thru da Construção',
    corredor: 'Pátio Externo de Carga',
    icon: 'local_shipping',
    color: '#475569',
    x: 615,
    y: 125,
    w: 130,
    h: 65,
    secaoRef: 'Materiais de construcao',
    descricao: 'Retirada rápida de materiais pesados no pátio'
  }
]

export const STORE_AMENITIES = [
  {
    id: 'caixas_frente',
    tipo: 'CAIXA',
    nome: 'Caixas de Pagamento (Frente)',
    icon: 'point_of_sale',
    x: 260,
    y: 575,
    color: '#16A34A',
    descricao: 'Caixas de autoatendimento e atendimento preferencial'
  },
  {
    id: 'caixas_saida',
    tipo: 'CAIXA',
    nome: 'Caixas Rápidos',
    icon: 'point_of_sale',
    x: 510,
    y: 155,
    color: '#16A34A',
    descricao: 'Caixas de saída rápida'
  },
  {
    id: 'banheiro_principal',
    tipo: 'BANHEIRO',
    nome: 'Sanitários / Banheiro',
    icon: 'wc',
    x: 235,
    y: 525,
    color: '#2563EB',
    descricao: 'Sanitários masculino, feminino e família'
  },
  {
    id: 'cafeteria',
    tipo: 'CAFE',
    nome: 'Cafeteria & Café',
    icon: 'local_cafe',
    x: 295,
    y: 485,
    color: '#92400E',
    descricao: 'Pausa para café, salgados e bebidas'
  },
  {
    id: 'servicos_cliente',
    tipo: 'SERVICOS',
    nome: 'Balcão de Serviços',
    icon: 'room_service',
    x: 475,
    y: 175,
    color: '#0284C7',
    descricao: 'Trocas, garantia estendida e contratação de instalação'
  },
  {
    id: 'entrada_loja',
    tipo: 'ENTRADA',
    nome: 'Entrada Principal da Loja',
    icon: 'login',
    x: 185,
    y: 535,
    color: '#15803D',
    descricao: 'Acesso pelo estacionamento com carrinhos e cestos'
  }
]

export const STORE_QR_POINTS = [
  { codigo: 'ENT-01', nome: 'Entrada Principal', corredor: 'Entrada da loja', x: 195, y: 550 },
  { codigo: 'TIN-02', nome: 'Corredor de Tintas', corredor: 'Tintas B01', x: 475, y: 440 },
  { codigo: 'CEN-03', nome: 'Cruzamento Central', corredor: 'Galeria Central', x: 420, y: 520 },
  { codigo: 'ILU-04', nome: 'Iluminação & Lustres', corredor: 'Iluminação A12', x: 175, y: 755 },
  { codigo: 'FER-05', nome: 'Ferramentas Elétricas', corredor: 'Ferramentas A11', x: 525, y: 350 },
  { codigo: 'CAI-06', nome: 'Frente de Loja / Caixas', corredor: 'Frente de Caixas', x: 275, y: 590 }
]

/**
 * Searches the closest sector for a given product or section name
 */
export function findSectorForProduct(product) {
  if (!product) return null
  const targetSecao = (product.secao || '').toLowerCase()
  const targetCorredor = (product.corredor || '').toLowerCase()
  const targetNome = (product.nome || product.name || '').toLowerCase()

  // Match by secaoRef
  const bySecao = STORE_SECTORS.find(s => 
    s.secaoRef.toLowerCase() === targetSecao ||
    s.nome.toLowerCase() === targetSecao ||
    targetSecao.includes(s.nome.toLowerCase()) ||
    s.nome.toLowerCase().includes(targetSecao)
  )
  if (bySecao) return bySecao

  // Match by corredor name
  const byCorredor = STORE_SECTORS.find(s =>
    targetCorredor.includes(s.nome.toLowerCase()) ||
    s.corredor.toLowerCase().includes(targetCorredor)
  )
  if (byCorredor) return byCorredor

  // Match keywords in product name
  for (const sector of STORE_SECTORS) {
    if (targetNome.includes(sector.nome.toLowerCase())) {
      return sector
    }
  }

  return STORE_SECTORS[0] // fallback to Pintura
}

/**
 * Maps coordinate percentage (0..100) from backend to SVG canvas scale (1000 x 950)
 */
export function gridToCanvas(x100, y100) {
  return {
    x: Math.round((x100 / 100) * 850 + 75),
    y: Math.round((y100 / 100) * 800 + 75)
  }
}

/**
 * Fetches store map data from backend or returns Interlagos blueprint
 */
export async function getStoreMapData() {
  try {
    const res = await fetch(`${API_BASE_URL}/api/v1/mapa`, {
      headers: { Accept: 'application/json' }
    })
    if (res.ok) {
      const data = await res.json()
      return {
        largura: data.largura || 100,
        altura: data.altura || 100,
        blocos: STORE_SECTORS,
        amenities: STORE_AMENITIES,
        qrPoints: STORE_QR_POINTS
      }
    }
  } catch (e) {
    console.warn('Usando mapa estático da loja Interlagos:', e.message)
  }

  return {
    largura: 100,
    altura: 100,
    blocos: STORE_SECTORS,
    amenities: STORE_AMENITIES,
    qrPoints: STORE_QR_POINTS
  }
}
