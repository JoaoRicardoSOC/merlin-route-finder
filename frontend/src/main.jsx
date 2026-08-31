import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import ErrorBoundary from './components/ErrorBoundary.jsx'

/*
 * A barreira fica DENTRO do StrictMode, não fora.
 *
 * Assim ela não interfere com as verificações de desenvolvimento — o StrictMode continua
 * propagando o erro para o console antes de a barreira desenhar a tela de recuperação. Ela é
 * a última linha, não a primeira.
 */
createRoot(document.getElementById('root')).render(
  <StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </StrictMode>,
)
