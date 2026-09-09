/*
 * Quem decide se o app aparece dentro de um celular desenhado.
 *
 * Fica separado do componente para o `main.jsx` conseguir escolher ANTES de montar o `App`.
 * Se a decisão morasse dentro do App, ele já teria aberto uma sessão — e a moldura abriria
 * uma segunda, dentro do iframe, deixando um resto no banco a cada visita de monitor.
 */

/**
 * Abaixo disto, ninguém está num monitor: é celular, tablet em pé, ou janela estreita —
 * e nesses casos o app é a própria demonstração, sem moldura.
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

export function deveMostrarMoldura() {
  return (
    !estaDentroDaMoldura() &&
    window.innerWidth >= LIMIAR_DE_MONITOR &&
    !pediuTelaCheia()
  )
}

/** Verdadeiro quando o app está em tela cheia num monitor, e cabe oferecer a volta. */
export function podeVoltarParaMoldura() {
  return (
    !estaDentroDaMoldura() &&
    window.innerWidth >= LIMIAR_DE_MONITOR &&
    pediuTelaCheia()
  )
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
