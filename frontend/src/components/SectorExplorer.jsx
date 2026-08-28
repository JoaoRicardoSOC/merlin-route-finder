import React, { useRef } from 'react'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../services/catalogService'

export default function SectorExplorer({
  secoes = [],
  selectedSecao = 'todos',
  onSelectSecao,
  totalProductsCount = 0,
  isLoading = false
}) {
  const scrollContainerRef = useRef(null)

  const handleScroll = (direction) => {
    if (scrollContainerRef.current) {
      const offset = direction === 'left' ? -240 : 240
      scrollContainerRef.current.scrollBy({ left: offset, behavior: 'smooth' })
    }
  }

  // Calculate total count across all sections if totalProductsCount is not provided
  const computedTotal = totalProductsCount || secoes.reduce((acc, s) => acc + (s.quantidadeProdutos || 0), 0)

  return (
    <section className="sector-explorer-section" aria-label="Exploração por Setores do Catálogo">
      <div className="sector-explorer-header">
        <div className="sector-explorer-title-wrap">
          <div className="sector-title-badge">
            <span className="material-symbols-outlined sector-title-icon filled">storefront</span>
            <span className="sector-title-tag">Catálogo Físico da Loja</span>
          </div>
          <h2 className="sector-explorer-heading">Explorar por Setores</h2>
          <p className="sector-explorer-subheading">
            Navegue pelos corredores temáticos e encontre produtos com facilidade na planta da loja
          </p>
        </div>

        {/* Desktop Carousel Arrows */}
        <div className="sector-carousel-controls">
          <button
            type="button"
            className="carousel-arrow-btn"
            onClick={() => handleScroll('left')}
            aria-label="Rolar seções para a esquerda"
          >
            <span className="material-symbols-outlined">chevron_left</span>
          </button>
          <button
            type="button"
            className="carousel-arrow-btn"
            onClick={() => handleScroll('right')}
            aria-label="Rolar seções para a direita"
          >
            <span className="material-symbols-outlined">chevron_right</span>
          </button>
        </div>
      </div>

      {isLoading ? (
        <div className="sector-chips-track skeleton-track">
          {[1, 2, 3, 4, 5, 6].map(i => (
            <div key={i} className="sector-chip-skeleton"></div>
          ))}
        </div>
      ) : (
        <div className="sector-scroll-wrapper">
          <div className="sector-chips-track" ref={scrollContainerRef}>
            {/* "Todas as Seções" Option */}
            <button
              type="button"
              className={`sector-chip ${selectedSecao === 'todos' ? 'active' : ''}`}
              onClick={() => onSelectSecao('todos')}
              aria-pressed={selectedSecao === 'todos'}
            >
              <div className="sector-chip-icon-box all-sectors-icon">
                <span className="material-symbols-outlined">dashboard</span>
              </div>
              <div className="sector-chip-content">
                <span className="sector-chip-name">Todas as Seções</span>
                <span className="sector-chip-count-badge">
                  {computedTotal} {computedTotal === 1 ? 'item' : 'itens'}
                </span>
              </div>
            </button>

            {/* Dynamic Physical Sections from SecaoResponse */}
            {secoes.map((secao) => {
              const meta = SECTOR_METADATA[secao.nome] || DEFAULT_SECTOR_META
              const isSelected = selectedSecao === secao.nome

              return (
                <button
                  key={secao.nome}
                  type="button"
                  className={`sector-chip ${isSelected ? 'active' : ''}`}
                  onClick={() => onSelectSecao(secao.nome)}
                  aria-pressed={isSelected}
                  title={`${secao.nome} (${secao.quantidadeProdutos} produtos disponíveis)`}
                >
                  <div
                    className="sector-chip-icon-box"
                    style={{
                      backgroundColor: isSelected ? 'var(--primary)' : meta.bgLight,
                      color: isSelected ? '#ffffff' : meta.color
                    }}
                  >
                    <span className="material-symbols-outlined">{meta.icon}</span>
                  </div>
                  <div className="sector-chip-content">
                    <span className="sector-chip-name">{secao.nome}</span>
                    <span className="sector-chip-count-badge">
                      {secao.quantidadeProdutos} {secao.quantidadeProdutos === 1 ? 'item' : 'itens'}
                    </span>
                  </div>
                  {isSelected && (
                    <span className="sector-active-indicator" aria-hidden="true"></span>
                  )}
                </button>
              )
            })}
          </div>
        </div>
      )}
    </section>
  )
}
