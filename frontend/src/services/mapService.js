// Mapa da loja Interlagos: geometria traçada + o que cada bloco significa.
//
// A geometria vem de `plantaInterlagos.js`, decalcada sobre a planta técnica do
// kickoff. Aqui ela ganha nome, cor, ícone e o vínculo com a seção do catálogo,
// e é convertida uma única vez para as coordenadas do desenho.
//
// O que saiu daqui, e por quê:
//
// - Os RETÂNGULOS INVENTADOS. Eram 18 blocos com posição e tamanho arbitrários,
//   espalhados num contorno de doze vértices que não era a loja. Viraram 21
//   polígonos traçados, com 212 gôndolas.
// - Os CÓDIGOS DE CORREDOR ("Corredor B01 - Tintas"). Nunca soubemos os códigos
//   reais da unidade. O mapa os afirmava com a mesma confiança de um dado
//   verificado, e ninguém conseguiria conferi-los na loja.
// - `getStoreMapData` e `gridToCanvas`. Código morto: nenhum componente os
//   chamava. A primeira buscava /api/v1/mapa e descartava `data.blocos`,
//   devolvendo os retângulos inventados — mas nunca chegou a executar.

import { SECOES, CONTORNOS, TELA, gradeParaTela } from './plantaInterlagos'

/** Tamanho do desenho, para o viewBox e para o centro do zoom. */
export const CANVAS = TELA

const paraCadeia = pontos => pontos.map(p => gradeParaTela(p).join(',')).join(' ')

/**
 * Uma gôndola é uma barra: eixo (a → b) mais espessura. Vira os quatro cantos.
 *
 * O cálculo é feito na grade e só então convertido, e não o contrário: converter
 * ponta e espessura em separado deformaria a barra, porque a escala não é a mesma
 * nos dois eixos.
 */
function barraParaPoligono({ a, b, esp }) {
  const dx = b[0] - a[0], dy = b[1] - a[1]
  const comprimento = Math.hypot(dx, dy) || 1
  const nx = (-dy / comprimento) * (esp / 2)
  const ny = (dx / comprimento) * (esp / 2)
  return paraCadeia([
    [a[0] + nx, a[1] + ny], [b[0] + nx, b[1] + ny],
    [b[0] - nx, b[1] - ny], [a[0] - nx, a[1] - ny],
  ])
}

/** O envelope do prédio: o galpão fechado e o pátio coberto, que são dois corpos. */
export const STORE_OUTLINE = CONTORNOS.map(c => ({
  nome: c.nome,
  patio: c.patio,
  pontos: paraCadeia(c.pontos),
}))

export const STORE_SECTORS = SECOES.map(s => {
  const tela = s.pontos.map(gradeParaTela)
  const xs = tela.map(p => p[0]), ys = tela.map(p => p[1])
  const [cx, cy] = gradeParaTela(s.centro)
  return {
    id: s.id,
    nome: s.nome,
    papel: s.papel,
    icon: s.icone,
    color: s.cor,
    secaoRef: s.secaoRef,
    descricao: s.descricao,
    pontos: paraCadeia(s.pontos),
    gondolas: s.gondolas.map(barraParaPoligono),
    // O rótulo vai no centroide, não no meio da caixa: numa seção em L o meio da
    // caixa cai fora da própria seção. Madeiras e Cerâmica são exatamente esse caso.
    rotuloX: cx,
    rotuloY: cy,
    // Caixa envolvente, para quem precisa de um ponto e não da forma — pino de
    // roteiro, foco do zoom, centro do painel lateral.
    x: Math.min(...xs),
    y: Math.min(...ys),
    w: Math.max(...xs) - Math.min(...xs),
    h: Math.max(...ys) - Math.min(...ys),
  }
})

const acharSetor = id => STORE_SECTORS.find(s => s.id === id)

/**
 * Serviços marcados por ícone. Só os três que foram traçados.
 *
 * Não há banheiro nem cafeteria aqui de propósito: a planta do kickoff não os
 * identifica, e a versão anterior os afirmava em coordenadas inventadas. "Sanitários"
 * na planta é o departamento de louças e metais, que é outra coisa — mandar um
 * cliente ao banheiro por causa dessa confusão seria pior que não oferecer.
 */
export const STORE_AMENITIES = ['caixas', 'servicos', 'entrada']
  .map(acharSetor)
  .filter(Boolean)
  .map(s => ({
    id: s.id,
    tipo: { caixas: 'CAIXA', servicos: 'SERVICOS', entrada: 'ENTRADA' }[s.id],
    nome: s.nome,
    icon: s.icon,
    color: s.color,
    descricao: s.descricao,
    x: s.rotuloX,
    y: s.rotuloY,
  }))

/**
 * As placas de QR. Os seis códigos são fixos no backend, que os valida — mudar
 * qualquer um quebraria a entrada por `?ponto=`. Só a posição na tela mudou.
 */
const PLACAS = [
  { codigo: 'ENT-01', nome: 'Entrada Principal', setor: 'entrada' },
  { codigo: 'TIN-02', nome: 'Corredor de Tintas', setor: 'tintas' },
  // Não fica dentro de seção nenhuma: é o corredor entre Jardim e Organização,
  // a circulação que liga os dois lados do galpão.
  { codigo: 'CEN-03', nome: 'Cruzamento Central', grade: [52.25, 25] },
  { codigo: 'ILU-04', nome: 'Iluminação & Lustres', setor: 'iluminacao' },
  { codigo: 'FER-05', nome: 'Ferramentas Elétricas', setor: 'ferramentas' },
  { codigo: 'CAI-06', nome: 'Frente de Loja / Caixas', setor: 'caixas' },
]

export const STORE_QR_POINTS = PLACAS.map(p => {
  const s = p.setor ? acharSetor(p.setor) : null
  const [x, y] = p.grade ? gradeParaTela(p.grade) : [s.rotuloX, s.rotuloY]
  return { codigo: p.codigo, nome: p.nome, corredor: s ? s.nome : 'Circulação central', x, y }
})

/**
 * Reduz um texto à forma usada na comparação: sem acento e em minúsculas.
 *
 * A comparação era por igualdade crua, e foi ela que quebrou "Materiais de construção" no dia
 * em que o catálogo ganhou acento — o secaoRef daqui continuou sem. Consertar a string resolve
 * o caso; normalizar resolve a classe, que já mordeu duas vezes.
 */
function comparavel(texto) {
  return (texto || '').normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
}

/**
 * Encontra o bloco do mapa onde o produto está, ou null quando não sabe.
 *
 * <b>Devolver null é a parte que importa.</b> Antes esta função devolvia STORE_SECTORS[0] —
 * Pintura — com a mesma confiança com que devolveria o certo, e quem pedia rota para um sifão
 * era mandado ao corredor de tintas. Não saber precisa parecer não saber.
 *
 * <b>A ordem das tentativas passou a importar.</b> Antes tudo estava num `find` só, com `||`
 * entre os critérios: vencia o primeiro SETOR da lista que satisfizesse qualquer critério, e
 * não a melhor correspondência. Com os blocos traçados isso virou risco concreto — "Jardim" e
 * "Jardim externo" agora convivem, e um produto de Jardim casaria por substring com o externo
 * dependendo só da ordem do arranjo. Agora o exato vence o parcial, sempre.
 */
export function findSectorForProduct(product) {
  if (!product) return null

  // O corredor gravado no item E o nome da secao: e assim que o backend o devolve
  // ("Encanamento", "Tintas"). Olhar so para `secao` fazia a busca falhar com o produto
  // que vem da API, que nao tem esse campo - e foi o que a verificacao pela tela pegou,
  // depois de a medicao direta passar porque eu mesmo alimentava `secao`.
  const alvoSecao = comparavel(product.secao || product.corredor)
  const alvoCorredor = comparavel(product.corredor)
  const alvoNome = comparavel(product.nome || product.name)

  // 1. A seção do catálogo, batendo exato. É o caminho das dez seções reais.
  if (alvoSecao) {
    const exato = STORE_SECTORS.find(s =>
      (s.secaoRef && comparavel(s.secaoRef) === alvoSecao) || comparavel(s.nome) === alvoSecao
    )
    if (exato) return exato
  }

  // 2. Só então por pedaço de texto, que é onde "Jardim" e "Jardim externo" se confundem.
  if (alvoSecao) {
    const parcial = STORE_SECTORS.find(s =>
      alvoSecao.includes(comparavel(s.nome)) || comparavel(s.nome).includes(alvoSecao)
    )
    if (parcial) return parcial
  }

  // 3. Pelo corredor gravado no item.
  if (alvoCorredor) {
    const porCorredor = STORE_SECTORS.find(s => alvoCorredor.includes(comparavel(s.nome)))
    if (porCorredor) return porCorredor
  }

  // 4. Último recurso: o nome do produto citar o bloco.
  if (alvoNome) {
    const porNome = STORE_SECTORS.find(s => alvoNome.includes(comparavel(s.nome)))
    if (porNome) return porNome
  }

  return null
}
