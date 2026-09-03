import { useEffect, useState } from 'react'

/**
 * A faixa que aparece enquanto o servidor publicado acorda.
 *
 * <b>Componente próprio</b> porque contar exige um render por segundo, e esse relógio no
 * `App` redesenharia o aplicativo inteiro a cada segundo — no momento em que ele já está com
 * menos fôlego.
 *
 * <b>Conta o tempo em vez de prometer prazo.</b> O texto anterior dizia "leva até dois
 * minutos" — um teto, estourado nas medições de 176 s, 180 s e 183 s. Número fixo em tela
 * envelhece sozinho e vira mentira sem ninguém mexer numa linha; um cronômetro não envelhece.
 * E resolve o problema que a pessoa de fato tem, que não é saber o prazo: é saber que o
 * aplicativo não travou. Ver quebras-de-fluxo.md.
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
