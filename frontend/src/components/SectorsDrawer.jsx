import React, { useState } from 'react'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../services/catalogService'

export default function SectorsDrawer({
  isOpen,
  onClose,
  secoes = [],
  selectedSecao = 'todos',
  onSelectSecao,
  onOpenMap
}) {
  const [filterText, setFilterText] = useState('')

  if (!isOpen) return null

  const filteredSecoes = secoes.filter(s =>
    s.nome.toLowerCase().includes(filterText.toLowerCase()) ||
    (SECTOR_METADATA[s.nome]?.corredor && SECTOR_METADATA[s.nome].corredor.toLowerCase().includes(filterText.toLowerCase()))
  )

  const handleSelect = (secaoNome) => {
    onSelectSecao(secaoNome)
    onClose()
  }

  const totalProducts = secoes.reduce((acc, s) => acc + (s.quantidadeProdutos || 0), 0)

  return (
    <div className="modal-backdrop sectors-drawer-backdrop" onClick={onClose}>
      <div
        className="sectors-drawer-container"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Menu de Setores e Corredores"
      >
        <div className="sectors-drawer-header">
          <div className="sectors-drawer-title-wrap">
            <div className="sectors-drawer-icon-wrap">
              <span className="material-symbols-outlined filled">storefront</span>
            </div>
            <div>
              <h3>Setores da Loja</h3>
              <p>Navegue pelas {secoes.length} seções físicas do catálogo</p>
            </div>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Fechar menu de setores"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Search inside sectors drawer */}
        <div className="sectors-drawer-search">
          <span className="material-symbols-outlined">search</span>
          <input
            type="text"
            placeholder="Filtrar setor ou corredor (ex: Tintas, A12)..."
            value={filterText}
            onChange={(e) => setFilterText(e.target.value)}
            className="sectors-search-input"
          />
          {filterText && (
            <button
              type="button"
              className="clear-drawer-search"
              onClick={() => setFilterText('')}
              aria-label="Limpar filtro"
            >
              <span className="material-symbols-outlined">close</span>
            </button>
          )}
        </div>

        <div className="sectors-drawer-list">
          {/* Option for All Sections */}
          <button
            type="button"
            className={`sectors-drawer-item ${selectedSecao === 'todos' ? 'active' : ''}`}
            onClick={() => handleSelect('todos')}
          >
            <div className="drawer-item-icon all-sectors-icon">
              <span className="material-symbols-outlined">dashboard</span>
            </div>
            <div className="drawer-item-info">
              <span className="drawer-item-name">Todas as Seções</span>
              <span className="drawer-item-desc">Catálogo completo da loja</span>
            </div>
            <span className="drawer-item-count">{totalProducts} itens</span>
          </button>

          {/* List of physical sections from SecaoResponse */}
          {filteredSecoes.map((secao) => {
            const meta = SECTOR_METADATA[secao.nome] || DEFAULT_SECTOR_META
            const isSelected = selectedSecao === secao.nome

            return (
              <button
                key={secao.nome}
                type="button"
                className={`sectors-drawer-item ${isSelected ? 'active' : ''}`}
                onClick={() => handleSelect(secao.nome)}
              >
                <div
                  className="drawer-item-icon"
                  style={{
                    backgroundColor: isSelected ? 'var(--primary)' : meta.bgLight,
                    color: isSelected ? '#ffffff' : meta.color
                  }}
                >
                  <span className="material-symbols-outlined">{meta.icon}</span>
                </div>
                <div className="drawer-item-info">
                  <span className="drawer-item-name">{secao.nome}</span>
                  <span className="drawer-item-corredor">
                    <span className="material-symbols-outlined">signpost</span>
                    {meta.corredor}
                  </span>
                </div>
                <span className="drawer-item-count">
                  {secao.quantidadeProdutos} {secao.quantidadeProdutos === 1 ? 'item' : 'itens'}
                </span>
              </button>
            )
          })}

          {filteredSecoes.length === 0 && (
            <div className="drawer-empty-state">
              <span className="material-symbols-outlined">search_off</span>
              <p>Nenhuma seção encontrada com "{filterText}"</p>
            </div>
          )}
        </div>

        <div className="sectors-drawer-footer">
          <button
            type="button"
            className="drawer-map-btn"
            onClick={() => {
              onClose()
              if (onOpenMap) onOpenMap()
            }}
          >
            <span className="material-symbols-outlined">map</span>
            <span>Ver Planta Geral da Loja</span>
          </button>
        </div>
      </div>
    </div>
  )
}
