import { alternarParaMoldura } from './decisao.js'
/* Precisa da folha aqui também: em tela cheia a moldura não é montada, e sem este import
   o botão sairia sem estilo nenhum. O Vite deduplica, então importar nos dois não custa. */
import './moldura.css'

/*
 * O caminho de volta. Sem ele, quem pede a tela cheia fica sem saída visível — teria de
 * saber que basta apagar `?telaCheia=1` do endereço, o que ninguém sabe.
 *
 * Mora fora do `App` e é renderizado ao lado dele, para a moldura inteira continuar sendo
 * removível sem tocar em uma linha do produto.
 */
export default function BotaoVoltarParaMoldura() {
  return (
    <button type="button" className="moldura-voltar" onClick={alternarParaMoldura}>
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
           strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <rect x="7" y="2.5" width="10" height="19" rx="2.5" />
        <path d="M10.8 5.2h2.4" />
      </svg>
      Ver como celular
    </button>
  )
}
