import { useState, useEffect } from 'react'
import leroyLogo from '../assets/leroy_logo.png'

export default function SplashScreen({ onFinish }) {
  const [stage, setStage] = useState('initial') // 'initial' | 'opening' | 'finished'

  useEffect(() => {
    // Stage 1: Hold the brand logo for a brief moment (500ms)
    const timerOpen = setTimeout(() => {
      setStage('opening')
    }, 550)

    // Stage 2: Complete split animation and remove from DOM (1200ms)
    const timerDone = setTimeout(() => {
      setStage('finished')
      if (onFinish) onFinish()
    }, 1250)

    return () => {
      clearTimeout(timerOpen)
      clearTimeout(timerDone)
    }
  }, [onFinish])

  if (stage === 'finished') return null

  return (
    <div 
      className={`splash-screen ${stage === 'opening' ? 'is-opening' : ''}`}
      aria-hidden="true"
    >
      {/* Left split door */}
      <div className="splash-door splash-door-left" />

      {/* Right split door */}
      <div className="splash-door splash-door-right" />

      {/* Center Brand Elements */}
      <div className="splash-center-content">
        <div className="splash-logo-card">
          <img 
            src={leroyLogo} 
            alt="Leroy Merlin" 
            className="splash-logo-img" 
          />
        </div>
        <div className="splash-brand-text">
          <span className="splash-tagline">Localizador Inteligente & Rotas</span>
          <div className="splash-loader-bar">
            <div className="splash-loader-fill" />
          </div>
        </div>
      </div>
    </div>
  )
}
