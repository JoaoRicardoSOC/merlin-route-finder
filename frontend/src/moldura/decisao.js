/*
 * Quem decide se o app aparece dentro de um celular desenhado.
 *
 * Fica separado do componente para o `main.jsx` conseguir escolher ANTES de montar o `App`.
 * Se a decisão morasse dentro do App, ele já teria aberto uma sessão — e a moldura abriria
 * uma segunda, dentro do iframe, deixando um resto no banco a cada visita de monitor.
 */

/**
 * Largura mínima para a moldura caber com folga. **É o segundo critério, não o primeiro** —
 * quem decide se isto é um monitor é o ponteiro, em `pareceMonitor()`.
 *
 * 1024 e não 768: entre 768 e 1024 o app ainda tem um layout próprio que serve, e a moldura
 * de 414 px de largura não caberia com folga ao lado do cabeçalho.
 */
export const LIMIAR_DE_MONITOR = 1024

/** O parâmetro que a pessoa liga ao pedir a tela cheia. Explícito na URL, e compartilhável. */
export const PARAMETRO_TELA_CHEIA = 'telaCheia'

/**
 * `true` quando este documento é o app rodando DENTRO da moldura.
 *
 * É o que impede a recursão infinita: sem esta checagem, o documento do iframe montaria
 * outra moldura, com outro iframe, sem fim.
 */
export function estaDentroDaMoldura() {
  try {
    return window.self !== window.top
  } catch {
    // Origem cruzada ao ler window.top só acontece se alguém embutir o app em outro site.
    // Nesse caso ele é o conteúdo, nunca o palco.
    return true
  }
}

export function pediuTelaCheia() {
  return new URLSearchParams(window.location.search).has(PARAMETRO_TELA_CHEIA)
}

/** Força a moldura mesmo onde a detecção diria que não. Escape para demonstração. */
export function pediuMoldura() {
  return new URLSearchParams(window.location.search).has('moldura')
}

/**
 * `true` só num computador de verdade.
 *
 * **Largura não basta, e supor que bastava foi um erro.** Um celular com o navegador em
 * "site para computador" ignora o `meta viewport` e passa a reportar 980 a 1100 px; um tablet
 * em pé reporta 1024. Nos dois casos a moldura aparecia — celular desenhado dentro de um
 * celular de verdade.
 *
 * O que separa um monitor de um aparelho de toque não é o tamanho, é o **ponteiro**: mouse e
 * trackpad são `fine` e sabem pairar; dedo é `coarse` e não paira. Notebook com tela sensível
 * continua sendo `fine`, porque o ponteiro primário dele é o trackpad.
 */
function pareceMonitor() {
  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}

export function deveMostrarMoldura() {
  if (estaDentroDaMoldura() || pediuTelaCheia()) return false
  if (pediuMoldura()) return true
  return pareceMonitor() && window.innerWidth >= LIMIAR_DE_MONITOR
}

/**
 * Verdadeiro quando o app está em tela cheia num monitor, e cabe oferecer a volta.
 *
 * A mesma condição de `deveMostrarMoldura`, com o `telaCheia` invertido — senão o botão de
 * voltar apareceria em aparelho que nunca teve moldura para onde voltar.
 */
export function podeVoltarParaMoldura() {
  if (estaDentroDaMoldura() || !pediuTelaCheia()) return false
  if (pediuMoldura()) return true
  return pareceMonitor() && window.innerWidth >= LIMIAR_DE_MONITOR
}

/*
 * A troca é uma NAVEGAÇÃO, e não um estado de React, de propósito: o app precisa remontar
 * dentro de um viewport novo para as media queries valerem. Trocar por estado deixaria o
 * layout de desktop desenhado num viewport de celular, que é justamente o defeito que a
 * moldura existe para não ter.
 */
function irPara(parametros) {
  const busca = parametros.toString()
  window.location.assign(window.location.pathname + (busca ? '?' + busca : ''))
}

export function alternarParaTelaCheia() {
  const p = new URLSearchParams(window.location.search)
  p.set(PARAMETRO_TELA_CHEIA, '1')
  irPara(p)
}

export function alternarParaMoldura() {
  const p = new URLSearchParams(window.location.search)
  p.delete(PARAMETRO_TELA_CHEIA)
  irPara(p)
}
