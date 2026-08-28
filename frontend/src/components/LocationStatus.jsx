import React from 'react'

export default function LocationStatus({ location, onClick }) {
  const aisleText = location?.aisle || 'Entrada Principal da Loja'
  const codeBadge = location?.code ? `Placa ${location.code}` : null

  return (
    <div 
      className="modern-location-status-card"
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className="modern-location-beacon">
        <span className="beacon-ping"></span>
        <span className="beacon-dot"></span>
      </div>

      <div className="modern-location-content">
        <div className="modern-location-top">
          <span className="modern-location-label">Você está em</span>
          {codeBadge && <span className="modern-plate-tag">{codeBadge}</span>}
        </div>
        <div className="modern-location-name">
          <span className="material-symbols-outlined modern-pin-icon filled">location_on</span>
          <strong>{aisleText}</strong>
          {location?.coords && (
            <span className="modern-coords-tag">({location.coords})</span>
          )}
        </div>
      </div>
    </div>
  )
}
