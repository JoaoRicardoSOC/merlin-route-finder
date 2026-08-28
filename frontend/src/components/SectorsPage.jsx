import React, { useState } from 'react'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../constants/setores'

export default function SectorsPage({
  secoes = [],
  totalProductsCount = 0,
  onSelectSector,
  onBackToHome,
  isLoading = false
}) {
  const [sectorSearch, setSectorSearch] = useState('')

  const filteredSecoes = secoes.filter(s => 
    s.nome.toLowerCase().includes(sectorSearch.toLowerCase()) ||
    (SECTOR_METADATA[s.nome]?.descricao || '').toLowerCase().includes(sectorSearch.toLowerCase()) ||
    (SECTOR_METADATA[s.nome]?.corredor || '').toLowerCase().includes(sectorSearch.toLowerCase())
  )

  return (
    <div className="sectors-page-view animate-fade-in">
      {/* Top Navigation Bar */}
      <div className="sectors-page-top-bar">
        <button
          type="button"
          className="search-back-btn"
          onClick={onBackToHome}
          aria-label="Voltar para o início"
          title="Voltar para a tela inicial"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>

        <div className="sectors-page-title-wrap">
          <h2 className="sectors-page-title">Setores da Loja</h2>
          <span className="sectors-page-subtitle">
            {secoes.length} seções físicas • {totalProductsCount} produtos
          </span>
        </div>
      </div>

      {/* Filter / Search within Sectors */}
      <div className="sectors-search-bar-wrap">
        <span className="material-symbols-outlined search-input-icon">search</span>
        <input
          type="text"
          value={sectorSearch}
          onChange={(e) => setSectorSearch(e.target.value)}
          placeholder="Buscar seção ou corredor (ex: Tintas, Elétrica, A12...)"
          className="sectors-search-input"
          autoComplete="off"
        />
        {sectorSearch && (
          <button
            type="button"
            className="search-page-clear-btn"
            onClick={() => setSectorSearch('')}
            aria-label="Limpar busca de setores"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        )}
      </div>

      {/* Grid of Sector Cards */}
      {isLoading ? (
        <div className="sectors-cards-grid">
          {[1, 2, 3, 4, 5, 6].map((idx) => (
            <div key={idx} className="sector-card-skeleton"></div>
          ))}
        </div>
      ) : filteredSecoes.length === 0 ? (
        <div className="empty-catalog-state">
          <span className="material-symbols-outlined empty-icon">store_mall_directory</span>
          <h3 className="empty-title">Nenhum setor encontrado</h3>
          <p className="empty-desc">Não encontramos seções com o termo "{sectorSearch}".</p>
          <button
            type="button"
            className="empty-action-btn primary"
            onClick={() => setSectorSearch('')}
          >
            Limpar filtro
          </button>
        </div>
      ) : (
        <div className="sectors-cards-grid">
          {filteredSecoes.map((secao) => {
            const meta = SECTOR_METADATA[secao.nome] || DEFAULT_SECTOR_META
            return (
              <button
                key={secao.nome}
                type="button"
                className="sector-hub-card"
                onClick={() => onSelectSector(secao.nome)}
              >
                <div className="sector-hub-card-header">
                  <div 
                    className="sector-hub-icon-box"
                    style={{ backgroundColor: meta.bgLight, color: meta.color }}
                  >
                    <span className="material-symbols-outlined">{meta.icon}</span>
                  </div>
                  <span className="sector-hub-corredor">
                    <span className="material-symbols-outlined">location_on</span>
                    {meta.corredor}
                  </span>
                </div>

                <div className="sector-hub-card-body">
                  <h3 className="sector-hub-name">{secao.nome}</h3>
                  <p className="sector-hub-desc">{meta.descricao}</p>
                </div>

                <div className="sector-hub-card-footer">
                  <span className="sector-hub-count">
                    <strong>{secao.quantidadeProdutos}</strong> produtos no setor
                  </span>
                  <span className="sector-hub-action-arrow">
                    <span>Ver Produtos</span>
                    <span className="material-symbols-outlined">arrow_forward</span>
                  </span>
                </div>
              </button>
            )
          })}
        </div>
      )}
    </div>
  )
}
