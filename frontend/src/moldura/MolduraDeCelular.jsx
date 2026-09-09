import { useEffect, useState } from 'react'
import './moldura.css'

/*
 * O app dentro de um celular desenhado, para demonstrar num monitor.
 *
 * POR QUE UM IFRAME, E NÃO UMA DIV DE 390px.
 *
 * Media query responde ao VIEWPORT, não ao contêiner. Numa div estreita, as regras de
 * `min-width: 640px` continuariam valendo e o app renderizaria o layout de desktop espremido
 * em 390px — pior que a tela cheia de hoje. O iframe cria um viewport real de 390px, então o
 * app se comporta exatamente como no celular, sem que uma linha do app precise saber disso.
 *
 * É também o que mantém esta funcionalidade sem risco: o `App` não foi tocado. Apagar esta
 * pasta e a decisão do `main.jsx` devolve o comportamento anterior.
 */

/** Medidas do aparelho, em pixels de CSS. 390×844 é o iPhone 14/15 de referência. */
const TELA_LARGURA = 390
const TELA_ALTURA = 844
const BORDA = 12
const APARELHO_LARGURA = TELA_LARGURA + BORDA * 2
const APARELHO_ALTURA = TELA_ALTURA + BORDA * 2

/*
 * O que a janela precisa ceder antes de sobrar espaço para o aparelho: o respiro das bordas,
 * a altura do cabeçalho e o vão entre ele e o celular.
 *
 * Contar só o respiro deixava a página com rolagem vertical — o aparelho cabia, o cabeçalho
 * acima dele não.
 */
const RESERVA_VERTICAL = 128
const RESERVA_HORIZONTAL = 56

function calcularEscala() {
  const cabeNaAltura = (window.innerHeight - RESERVA_VERTICAL) / APARELHO_ALTURA
  const cabeNaLargura = (window.innerWidth - RESERVA_HORIZONTAL) / APARELHO_LARGURA
  /*
   * Nunca aumenta: um aparelho maior que o real não ajuda ninguém e deixa a tipografia
   * grande demais para o que a câmera vai mostrar.
   */
  return Math.min(1, cabeNaAltura, cabeNaLargura)
}

function horaAgora() {
  return new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })
}

export default function MolduraDeCelular({ aoPedirTelaCheia }) {
  const [escala, setEscala] = useState(calcularEscala)
  const [hora, setHora] = useState(horaAgora)

  useEffect(() => {
    const aoRedimensionar = () => setEscala(calcularEscala())
    window.addEventListener('resize', aoRedimensionar)
    return () => window.removeEventListener('resize', aoRedimensionar)
  }, [])

  useEffect(() => {
    /*
     * Meio minuto, e não um minuto: com um minuto cheio o relógio pode ficar até 59 segundos
     * atrasado, e numa gravação isso aparece se a tomada durar.
     */
    const id = setInterval(() => setHora(horaAgora()), 30_000)
    return () => clearInterval(id)
  }, [])

  /*
   * O iframe carrega o MESMO endereço, com a query intacta — é assim que `?ponto=TIN-02`
   * chega ao app lá dentro. Não precisa de parâmetro para evitar recursão: lá dentro
   * `window.self !== window.top`, e o `main.jsx` decide por isso.
   */
  const endereco = window.location.pathname + window.location.search

  return (
    <div className="moldura-palco">
      <header className="moldura-cabecalho">
        <div>
          <p className="moldura-titulo">Merlin Route Finder</p>
          <p className="moldura-legenda">Visualização em celular — 390 × 844</p>
        </div>
        <button
          type="button"
          className="moldura-saida"
          onClick={aoPedirTelaCheia}
        >
          Ver em tela cheia
        </button>
      </header>

      <div
        className="moldura-encaixe"
        style={{
          width: APARELHO_LARGURA * escala,
          height: APARELHO_ALTURA * escala,
        }}
      >
        <div
          className="moldura-aparelho"
          style={{
            width: APARELHO_LARGURA,
            height: APARELHO_ALTURA,
            padding: BORDA,
            transform: `scale(${escala})`,
          }}
        >
          <div className="moldura-tela">
            <div className="moldura-barra-status" aria-hidden="true">
              <span className="moldura-hora">{hora}</span>
              <span className="moldura-ilha" />
              <span className="moldura-indicadores">
                <SinalDeRede />
                <Wifi />
                <Bateria />
              </span>
            </div>

            <iframe
              className="moldura-quadro"
              src={endereco}
              title="Merlin Route Finder em tela de celular"
            />
          </div>
        </div>
      </div>
    </div>
  )
}

/*
 * Os três ícones são SVG e não Material Symbols de propósito: a fonte de ícones do app é um
 * RECORTE gerado por ferramentas/fontes/gerar.py, e ícone fora do recorte não desenha e não
 * dá erro. Depender dela aqui criaria uma dependência entre a moldura e a regeneração da
 * fonte, para desenhar três formas triviais.
 */
function SinalDeRede() {
  return (
    <svg viewBox="0 0 18 12" width="17" height="11" fill="currentColor">
      <rect x="0" y="8.5" width="3" height="3.5" rx="1" />
      <rect x="5" y="6" width="3" height="6" rx="1" />
      <rect x="10" y="3" width="3" height="9" rx="1" />
      <rect x="15" y="0" width="3" height="12" rx="1" />
    </svg>
  )
}

function Wifi() {
  return (
    <svg viewBox="0 0 16 12" width="16" height="12" fill="none" stroke="currentColor"
         strokeWidth="1.7" strokeLinecap="round">
      <path d="M1 4.2a10.5 10.5 0 0 1 14 0" />
      <path d="M3.6 7a6.8 6.8 0 0 1 8.8 0" />
      <path d="M6.2 9.7a3 3 0 0 1 3.6 0" />
    </svg>
  )
}

function Bateria() {
  return (
    <svg viewBox="0 0 26 12" width="25" height="11" fill="none">
      <rect x="0.6" y="0.6" width="21" height="10.8" rx="3" stroke="currentColor"
            strokeWidth="1.2" opacity="0.45" />
      <rect x="2.2" y="2.2" width="16" height="7.6" rx="1.8" fill="currentColor" />
      <path d="M23.4 4.3v3.4a2.2 2.2 0 0 0 0-3.4z" fill="currentColor" opacity="0.45" />
    </svg>
  )
}
