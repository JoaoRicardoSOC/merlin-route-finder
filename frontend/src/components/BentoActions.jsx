import React from 'react'

export default function BentoActions({ onViewProducts, onCallSpecialist, onViewMap, currentSectorName }) {
  return (
    <section className="bento-actions-grid">
      <button 
        className="bento-card" 
        onClick={onViewProducts}
        type="button"
      >
        <div className="bento-icon-wrapper">
          <span className="material-symbols-outlined bento-icon">lightbulb</span>
        </div>
        <div className="bento-text-wrapper">
          <h3 className="bento-title">Produtos do Setor</h3>
          <p className="bento-desc">Explorar itens em {currentSectorName}</p>
        </div>
        <span className="bento-arrow">→</span>
      </button>

      <button 
        className="bento-card highlighted" 
        onClick={onCallSpecialist}
        type="button"
      >
        <div className="bento-icon-wrapper specialist-icon">
          <span className="material-symbols-outlined bento-icon">support_agent</span>
        </div>
        <div className="bento-text-wrapper">
          <h3 className="bento-title">Chamar Especialista</h3>
          <p className="bento-desc">Ajuda técnica presencial no corredor</p>
        </div>
        <span className="bento-arrow">→</span>
      </button>

      <button 
        className="bento-card" 
        onClick={onViewMap}
        type="button"
      >
        <div className="bento-icon-wrapper map-icon">
          <span className="material-symbols-outlined bento-icon">map</span>
        </div>
        <div className="bento-text-wrapper">
          <h3 className="bento-title">Mapa Interativo</h3>
          <p className="bento-desc">Traçar rota inteligente na loja</p>
        </div>
        <span className="bento-arrow">→</span>
      </button>
    </section>
  )
}
