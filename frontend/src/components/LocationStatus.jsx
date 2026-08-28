import React from 'react'

export default function LocationStatus({ location, onChangeLocation, onViewMap }) {
  const hasLocation = location && (location.sector || location.aisle || location.code)
  const aisleText = location?.aisle || 'Corredor não identificado'
  const codeBadge = location?.code ? `[Placa ${location.code}]` : ''

  return (
    <div className="location-chip-card">
      <div className="location-icon-wrapper">
        <span className="material-symbols-outlined filled">location_on</span>
      </div>
      <div className="location-info">
        <div className="location-tag-row">
          <span className="location-label">Sua Localização Atual</span>
          {codeBadge && <span className="location-plate-pill">{codeBadge}</span>}
        </div>
        <p className="location-value">
          {aisleText}{' '}
          {location?.coords && (
            <span className="aisle-badge">({location.coords})</span>
          )}
        </p>
      </div>

      <div className="location-card-buttons">
        <button 
          className="location-change-btn" 
          onClick={onChangeLocation}
          title="Alterar ou escanear placa de corredor (QR Code / Código)"
          type="button"
        >
          <span className="material-symbols-outlined">qr_code_scanner</span>
          <span className="btn-text">Ler Placa</span>
        </button>
        <button 
          className="location-map-action-btn" 
          onClick={onViewMap}
          title="Ver localização no mapa da loja"
          type="button"
        >
          <span className="material-symbols-outlined">map</span>
          <span className="btn-text">Mapa</span>
        </button>
      </div>
    </div>
  )
}
