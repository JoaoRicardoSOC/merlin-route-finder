import React from 'react'

export default function PromoBanner({ onExplore }) {
  return (
    <div className="promo-banner">
      <div className="promo-overlay"></div>
      <div className="promo-content">
        <div className="promo-badge">
          <span className="material-symbols-outlined filled">star</span>
          Destaque do Mês
        </div>
        <h3 className="promo-title">Iluminação Inteligente & Sustentável</h3>
        <p className="promo-subtitle">
          Fitas LED, lâmpadas de filamento e painéis de sobrepor, no corredor de Iluminação.
        </p>
        <button className="promo-cta-btn" onClick={onExplore}>
          <span>Explorar Novidades</span>
          <span className="material-symbols-outlined">arrow_forward</span>
        </button>
      </div>
    </div>
  )
}
