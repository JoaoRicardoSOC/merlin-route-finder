import React from 'react'

export default function LocationStatus({ location, onSectorChange, onViewMap }) {
  return (
    <div className="location-chip-card">
      <div className="location-icon-wrapper">
        <span className="material-symbols-outlined filled">location_on</span>
      </div>
      <div className="location-info">
        <span className="location-label">Sua Localização Atual</span>
        <p className="location-value">{location.sector} <span className="aisle-badge">({location.aisle})</span></p>
      </div>
      <button 
        className="location-change-btn" 
        onClick={onViewMap}
        title="Ver localização no mapa da loja"
      >
        <span className="material-symbols-outlined">map</span>
        <span className="btn-text">Ver no Mapa</span>
      </button>
    </div>
  )
}
