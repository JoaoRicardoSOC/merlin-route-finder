import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'
import MolduraDeCelular from './moldura/MolduraDeCelular.jsx'
import BotaoVoltarParaMoldura from './moldura/BotaoVoltarParaMoldura.jsx'
import { deveMostrarMoldura, podeVoltarParaMoldura, alternarParaTelaCheia } from './moldura/decisao.js'

/*
 * A escolha entre o palco e o app acontece AQUI, antes de montar qualquer um dos dois.
 *
 * Num monitor, o app aparece dentro de um celular desenhado (ver `moldura/`); no celular e
 * em janela estreita, ele é o próprio conteúdo. A moldura não sabe nada do app, e o app não
 * sabe nada da moldura — a fronteira é um iframe.
 *
 * A decisão é tomada uma vez, na carga, e NÃO reage a redimensionar a janela: trocar de modo
 * no meio de uma demonstração desmontaria o app e perderia a sessão, o roteiro e a posição.
 * Redimensionar apenas reescala o aparelho.
 */
const raiz = createRoot(document.getElementById('root'))

raiz.render(
  <StrictMode>
    {/*
      * A barreira fica DENTRO do StrictMode, não fora.
      *
      * Assim ela não interfere com as verificações de desenvolvimento — o StrictMode continua
      * propagando o erro para o console antes de a barreira desenhar a tela de recuperação. Ela é
      * a última linha, não a primeira.
      */}
    <ErrorBoundary>
      {deveMostrarMoldura() ? (
        <MolduraDeCelular aoPedirTelaCheia={alternarParaTelaCheia} />
      ) : (
        <>
          <App />
          {podeVoltarParaMoldura() && <BotaoVoltarParaMoldura />}
        </>
      )}
    </ErrorBoundary>
  </StrictMode>,
)
