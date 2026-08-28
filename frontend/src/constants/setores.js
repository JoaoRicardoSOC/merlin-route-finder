// Metadados de apresentação de cada seção da loja: ícone, cor e o texto que descreve o que
// se encontra ali.
//
// Vivia dentro do catalogService, que é o módulo que fala HTTP - e era importado por oito
// arquivos de tela. Nada disto vem da API nem vai para ela: é decisão de interface, e a
// camada de serviço nunca chegou a usar estas constantes, só as declarava.
//
// ATENÇÃO: as chaves precisam ser exatamente os nomes de seção que o backend devolve, com
// acento. A busca é por igualdade, e foi por causa dela que quatro cartões de setor caíram
// no texto genérico enquanto a massa estava sem acentuação.
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
