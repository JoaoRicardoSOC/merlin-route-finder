import { useEffect, useState } from 'react'

/**
 * A faixa que aparece enquanto o servidor publicado acorda.
 *
 * <b>Por que existe um componente só para isto.</b> O aviso conta o tempo, e contar exige
 * um render por segundo. Deixar esse relógio no `App` faria o aplicativo inteiro se
 * redesenhar de segundo em segundo durante a espera — que é justamente o momento em que ele
 * já está com menos fôlego.
 *
 * <b>Por que contar o tempo em vez de prometer um prazo.</b> O texto anterior dizia
 * <i>"a primeira abertura do dia leva até dois minutos"</i>. Era verdade quando foi medido —
 * 106 s e 109 s em 30/08 — mas <b>"até" é um teto</b>, e o teto foi rompido. A partida a frio,
 * medida contra o ambiente publicado:
 *
 * <pre>
 *   25/08    176 s
 *   30/08    106 s e 109 s
 *   02/09    183 s   &lt;- 63 s além do teto prometido
 * </pre>
 *
 * Um número fixo numa tela envelhece sozinho e vira mentira sem que ninguém tenha mudado
 * uma linha.
 *
 * Um cronômetro não envelhece. <b>"Esperando há 2:14" é verdade no instante em que é lido</b>,
 * qualquer que seja o comportamento do provedor — e ainda resolve o problema real de quem
 * espera, que não é saber o prazo: é saber que o aplicativo não travou.
 */
export default function AvisoAcordando() {
  const [segundos, setSegundos] = useState(0)

  useEffect(() => {
    const relogio = setInterval(() => setSegundos(s => s + 1), 1000)
    return () => clearInterval(relogio)
  }, [])

  const minutos = Math.floor(segundos / 60)
  const resto = String(segundos % 60).padStart(2, '0')
  const decorrido = `${minutos}:${resto}`

  /*
   * Passando de tres minutos o texto muda de tom. Nao e enfeite: quem esperou tanto ja
   * desconfia que quebrou, e continuar repetindo a mesma frase e o que faz a pessoa fechar
   * o aplicativo. Dizer "esta demorando mais que o normal" e menos confortavel e mais util.
   */
  const alemDoNormal = segundos >= 180

  return (
    <div className="aviso-acordando" role="status">
      <span className="material-symbols-outlined" aria-hidden="true">hourglass_top</span>
      <span>
        {alemDoNormal
          ? 'Ainda preparando o sistema — está demorando mais que o normal, mas não travou.'
          : 'Preparando o sistema… o servidor hiberna quando ninguém está usando, e acordar leva alguns minutos.'}
      </span>
      {/*
        * O cronometro fica FORA do que e anunciado. O `role="status"` avisa o leitor de tela a
        * cada mudanca do conteudo, e um numero que muda de segundo em segundo viraria uma
        * interrupcao por segundo — o aviso deixaria de informar e passaria a atrapalhar.
        * A frase acima ja diz tudo que precisa ser ouvido; o relogio e para quem olha.
        */}
      <span className="aviso-acordando-relogio" aria-hidden="true">{decorrido}</span>
    </div>
  )
}
