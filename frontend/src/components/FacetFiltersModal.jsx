import useModalAcessivel from '../hooks/useModalAcessivel'

export default function FacetFiltersModal({
  isOpen,
  onClose,
  facetas = [],
  selectedAtributos = {},
  onToggleAtributo,
  onClearAtributos,
  apenasDisponiveis,
  onToggleDisponiveis,
  totalResultsCount = 0
}) {
  // Antes do retorno antecipado: hook nao pode ficar atras de um `return`.
  const refModal = useModalAcessivel(isOpen, onClose)

  if (!isOpen) return null

  // Calculate count of selected attribute filters
  const activeFiltersCount = Object.values(selectedAtributos).reduce(
    (acc, vals) => acc + (Array.isArray(vals) ? vals.length : vals ? 1 : 0),
    0
  ) + (apenasDisponiveis ? 1 : 0)

  return (
    <div className="modal-backdrop facet-modal-backdrop" onClick={onClose}>
      <div
        className="facet-modal-container"
        onClick={(e) => e.stopPropagation()}
        ref={refModal}
        role="dialog"
        aria-modal="true"
        aria-label="Filtros e Características dos Produtos"
      >
        {/* Header */}
        <div className="facet-modal-header">
          <div className="facet-header-title-wrap">
            <span className="material-symbols-outlined filled" aria-hidden="true">tune</span>
            <div>
              <h3>Filtros & Características</h3>
              <p>Refine os produtos por especificações técnicas</p>
            </div>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Fechar filtros"
          >
            <span className="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        </div>

        {/* Body */}
        <div className="facet-modal-body">
          {/* Quick Availability Section */}
          <div className="facet-group availability-group">
            <h4 className="facet-group-title">Disponibilidade na Loja</h4>
            <label className="facet-checkbox-row availability-checkbox-row">
              <input
                type="checkbox"
                checked={apenasDisponiveis}
                onChange={(e) => onToggleDisponiveis(e.target.checked)}
                className="facet-native-checkbox"
              />
              <span className="facet-custom-checkbox">
                <span className="material-symbols-outlined" aria-hidden="true">check</span>
              </span>
              <span className="facet-value-label">Apenas produtos com estoque disponível</span>
            </label>
          </div>

          {/* Dynamic Facets from Backend */}
          {facetas && facetas.length > 0 ? (
            facetas.map((faceta) => {
              const chave = faceta.atributo
              const rotulo = faceta.rotulo || chave
              const valores = faceta.valores || []
              const selectedVals = selectedAtributos[chave] || []

              if (valores.length === 0) return null

              return (
                <div key={chave} className="facet-group">
                  <div className="facet-group-header">
                    <h4 className="facet-group-title">{rotulo}</h4>
                    {selectedVals.length > 0 && (
                      <span className="facet-group-badge">
                        {selectedVals.length} selecionado{selectedVals.length > 1 ? 's' : ''}
                      </span>
                    )}
                  </div>

                  <div className="facet-values-list">
                    {valores.map((valObj) => {
                      const valorStr = valObj.valor
                      const isChecked = selectedVals.includes(valorStr)
                      const count = valObj.quantidade

                      return (
                        <label
                          key={valorStr}
                          className={`facet-checkbox-row ${isChecked ? 'selected' : ''}`}
                        >
                          <input
                            type="checkbox"
                            checked={isChecked}
                            onChange={() => onToggleAtributo(chave, valorStr)}
                            className="facet-native-checkbox"
                          />
                          <span className="facet-custom-checkbox">
                            <span className="material-symbols-outlined" aria-hidden="true">check</span>
                          </span>
                          <span className="facet-value-label">{valorStr}</span>
                          <span className="facet-value-count">({count})</span>
                        </label>
                      )
                    })}
                  </div>
                </div>
              )
            })
          ) : (
            <div className="facet-empty-notice">
              <span className="material-symbols-outlined" aria-hidden="true">filter_list_off</span>
              <p>Nenhuma característica adicional disponível para este recorte de produtos.</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="facet-modal-footer">
          {activeFiltersCount > 0 ? (
            <button
              type="button"
              className="facet-clear-btn"
              onClick={onClearAtributos}
            >
              Limpar filtros ({activeFiltersCount})
            </button>
          ) : (
            <span className="facet-footer-hint">Nenhum filtro aplicado</span>
          )}

          <button
            type="button"
            className="facet-apply-btn"
            onClick={onClose}
          >
            <span>Ver {totalResultsCount} {totalResultsCount === 1 ? 'produto' : 'produtos'}</span>
          </button>
        </div>
      </div>
    </div>
  )
}
