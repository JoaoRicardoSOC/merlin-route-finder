import React from 'react'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../constants/setores'

export default function SectorBanner({
  selectedSecao,
  productCount = 0,
  onClearFilter,
  onOpenMap
}) {
  if (!selectedSecao || selectedSecao === 'todos') {
    return null
  }

  const meta = SECTOR_METADATA[selectedSecao] || DEFAULT_SECTOR_META

  return (
    <div className="sector-banner-active">
      <div className="sector-banner-visual" style={{ backgroundColor: meta.bgLight, color: meta.color }}>
        <span className="material-symbols-outlined sector-banner-icon">{meta.icon}</span>
      </div>

      <div className="sector-banner-info">
        <div className="sector-banner-meta-row">
          <span className="sector-banner-tag">Setor Selecionado</span>
          <span className="sector-banner-aisle">
            <span className="material-symbols-outlined">signpost</span>
            {meta.corredor}
          </span>
        </div>
        <h3 className="sector-banner-title">{selectedSecao}</h3>
        <p className="sector-banner-desc">{meta.descricao}</p>
        <div className="sector-banner-count-row">
          <span className="sector-banner-count-badge">
            <span className="material-symbols-outlined">inventory_2</span>
            {productCount} {productCount === 1 ? 'produto encontrado' : 'produtos encontrados'}
          </span>
        </div>
      </div>

      <div className="sector-banner-actions">
        <button
          type="button"
          className="sector-banner-btn-map"
          onClick={onOpenMap}
          title="Ver localização deste setor no mapa da loja"
        >
          <span className="material-symbols-outlined">map</span>
          <span>Ver no Mapa</span>
        </button>
        <button
          type="button"
          className="sector-banner-btn-clear"
          onClick={onClearFilter}
          title="Remover filtro e ver todas as seções"
        >
          <span className="material-symbols-outlined">close</span>
          <span>Limpar Filtro</span>
        </button>
      </div>
    </div>
  )
}
