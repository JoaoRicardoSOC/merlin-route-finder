import React from 'react'

export default function ActiveFilterChips({
  selectedAtributos = {},
  facetas = [],
  apenasDisponiveis,
  onToggleDisponiveis,
  onRemoveAtributo,
  onClearAll
}) {
  // Build a list of active chip objects
  const chips = []

  if (apenasDisponiveis) {
    chips.push({
      type: 'disponivel',
      label: 'Apenas Disponíveis',
      onRemove: () => onToggleDisponiveis(false)
    })
  }

  Object.entries(selectedAtributos).forEach(([chave, valores]) => {
    if (!valores || valores.length === 0) return
    const faceta = facetas.find(f => f.atributo === chave)
    const rotulo = faceta ? faceta.rotulo : chave

    valores.forEach((val) => {
      chips.push({
        type: 'atributo',
        chave,
        valor: val,
        label: `${rotulo}: ${val}`,
        onRemove: () => onRemoveAtributo(chave, val)
      })
    })
  })

  if (chips.length === 0) return null

  return (
    <div className="active-filter-chips-bar">
      <div className="chips-scroll-container">
        <span className="chips-label">Filtros ativos:</span>
        {chips.map((chip, idx) => (
          <div key={idx} className="active-filter-chip">
            <span className="chip-text">{chip.label}</span>
            <button
              type="button"
              className="chip-remove-btn"
              onClick={chip.onRemove}
              aria-label={`Remover filtro ${chip.label}`}
            >
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
        ))}
        {chips.length > 1 && (
          <button
            type="button"
            className="chips-clear-all-btn"
            onClick={onClearAll}
          >
            Limpar todos
          </button>
        )}
      </div>
    </div>
  )
}
