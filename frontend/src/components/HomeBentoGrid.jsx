import React from 'react'

export default function HomeBentoGrid({
  onOpenMap,
  onOpenSectors,
  onCallSpecialist
}) {
  return (
    <section className="home-bento-section" aria-label="Ações Rápidas">
      <div className="home-bento-grid home-bento-grid-3">
        {/* Card 1: Mapa Inteligente */}
        <button
          type="button"
          className="bento-card bento-card-green"
          onClick={onOpenMap}
        >
          <div className="bento-card-header">
            <div className="bento-icon-box">
              <span className="material-symbols-outlined filled">map</span>
            </div>
            <span className="bento-arrow">
              <span className="material-symbols-outlined">arrow_forward</span>
            </span>
          </div>
          <div className="bento-card-body">
            <h3 className="bento-card-title">Mapa da Loja</h3>
            <p className="bento-card-desc">Localize corredores e prateleiras em tempo real</p>
          </div>
        </button>

        {/* Card 2: Setores da Loja */}
        <button
          type="button"
          className="bento-card bento-card-green"
          onClick={onOpenSectors}
        >
          <div className="bento-card-header">
            <div className="bento-icon-box">
              <span className="material-symbols-outlined filled">storefront</span>
            </div>
            <span className="bento-arrow">
              <span className="material-symbols-outlined">arrow_forward</span>
            </span>
          </div>
          <div className="bento-card-body">
            <h3 className="bento-card-title">Setores</h3>
            <p className="bento-card-desc">Navegue pelas 10 seções do catálogo físico</p>
          </div>
        </button>

        {/* Card 3: Chamar Especialista */}
        <button
          type="button"
          className="bento-card bento-card-green"
          onClick={onCallSpecialist}
        >
          <div className="bento-card-header">
            <div className="bento-icon-box">
              <span className="material-symbols-outlined filled">support_agent</span>
            </div>
            <span className="bento-arrow">
              <span className="material-symbols-outlined">arrow_forward</span>
            </span>
          </div>
          <div className="bento-card-body">
            <h3 className="bento-card-title">Chamar Especialista</h3>
            <p className="bento-card-desc">Solicite ajuda presencial no corredor</p>
          </div>
        </button>
      </div>
    </section>
  )
}
