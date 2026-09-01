import React from 'react'

/**
 * A última rede de segurança da tela.
 *
 * Sem isto, um erro de renderização em qualquer componente desmonta a árvore inteira e o
 * cliente fica com a página em branco, sem nada para tocar. As falhas de *rede* já são
 * tratadas uma a uma, com mensagem própria; o buraco era o outro tipo — erro de programação
 * em tempo de execução, que é justamente o que ninguém prevê.
 *
 * **Precisa ser componente de classe.** Não existe equivalente em hook para
 * `getDerivedStateFromError` — é a única classe do projeto, e é por isso. Não "modernize".
 *
 * Duas decisões dentro dela:
 *
 * 1. **Não engole o erro.** Ele vai para o console antes de a tela aparecer; sem isso o
 *    defeito fica invisível para quem for consertar.
 * 2. **O botão recarrega a página**, em vez de tentar se recuperar no lugar. Depois de um erro
 *    de renderização o estado do React não é confiável, e fingir recuperação entregaria ao
 *    cliente um app meio quebrado que parece inteiro.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props)
    this.state = { falhou: false }
  }

  static getDerivedStateFromError() {
    return { falhou: true }
  }

  componentDidCatch(erro, info) {
    console.error('Erro não tratado na interface:', erro, info?.componentStack)
  }

  render() {
    if (!this.state.falhou) {
      return this.props.children
    }

    return (
      <main className="tela-de-erro">
      {/*
          * <main> por fora e role="alert" por dentro, e os dois precisam ser elementos
          * diferentes: `role="alert"` SUBSTITUI o papel de marco do <main>, então pô-los no
          * mesmo elemento deixaria a página sem conteúdo principal. O invólucro usa
          * `display: contents` para não entrar no layout — ele existe só para a leitura.
          */}
        <div className="tela-de-erro-anuncio" role="alert">
        <span className="material-symbols-outlined tela-de-erro-icone" aria-hidden="true">warning</span>
        <h1 className="tela-de-erro-titulo">Alguma coisa quebrou aqui</h1>
        <p className="tela-de-erro-texto">
          O problema é nosso, não seu — e nada do que você montou até agora foi perdido:
          sua lista fica guardada no aparelho.
        </p>
        <button
          type="button"
          className="tela-de-erro-botao"
          onClick={() => window.location.reload()}
        >
          Recarregar o app
        </button>
        </div>
      </main>
    )
  }
}
