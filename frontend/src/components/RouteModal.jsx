import React from 'react'

export default function RouteModal({ isOpen, onClose, title, type, data }) {
  if (!isOpen) return null

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-container" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title-wrap">
            <span className="material-symbols-outlined modal-header-icon filled">
              {type === 'map' ? 'map' : type === 'specialist' ? 'support_agent' : type === 'route' ? 'directions_walk' : 'info'}
            </span>
            <h3>{title}</h3>
          </div>
          <button className="modal-close-btn" onClick={onClose} aria-label="Fechar">
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        <div className="modal-body">
          {type === 'map' && (
            <div className="map-modal-content">
              <div className="map-visual-placeholder">
                <div className="store-grid">
                  <div className="aisle-block active-aisle">
                    <span className="aisle-title">A12 - Iluminação</span>
                    <span className="pin-indicator">📍 Você está aqui</span>
                  </div>
                  <div className="aisle-block">
                    <span className="aisle-title">A11 - Ferramentas</span>
                  </div>
                  <div className="aisle-block">
                    <span className="aisle-title">B04 - Tintas & Silicones</span>
                  </div>
                  <div className="aisle-block">
                    <span className="aisle-title">C02 - Decoração</span>
                  </div>
                </div>
              </div>
              <p className="map-tip">
                💡 O assistente inteligente calcula a rota mais rápida evitando corredores cheios.
              </p>
            </div>
          )}

          {type === 'specialist' && (
            <div className="specialist-modal-content">
              <div className="specialist-status">
                <span className="material-symbols-outlined specialist-avatar filled">engineering</span>
                <div>
                  <h4>Especialista Carlos Silva</h4>
                  <p>Setor de Elétrica & Iluminação • Loja Marginal Tietê</p>
                </div>
              </div>
              <p className="specialist-msg">
                O especialista foi notificado e está a caminho do <strong>Corredor A12</strong>. Tempo estimado de chegada: <strong>~2 minutos</strong>.
              </p>
            </div>
          )}

          {type === 'route' && data && (
            <div className="route-modal-content">
              <div className="route-target-info">
                <h4>{data.name}</h4>
                <p className="route-aisle">Destino: <strong>{data.corredor}</strong></p>
              </div>
              <div className="route-steps">
                <div className="route-step completed">
                  <span className="step-number">1</span>
                  <span>Siga em frente no corredor atual (A12) por 15 metros.</span>
                </div>
                <div className="route-step active">
                  <span className="step-number">2</span>
                  <span>Vire à direita na gôndola 4 para encontrar o produto.</span>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button className="modal-primary-btn" onClick={onClose}>
            Entendido
          </button>
        </div>
      </div>
    </div>
  )
}
