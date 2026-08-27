import React, { useState } from 'react'

export default function SearchBar({ searchQuery, setSearchQuery, onSearch, suggestions = [] }) {
  const [isFocused, setIsFocused] = useState(false)

  const handleSubmit = (e) => {
    e.preventDefault()
    if (onSearch) onSearch(searchQuery)
  }

  const handleSuggestionClick = (item) => {
    setSearchQuery(item)
    if (onSearch) onSearch(item)
  }

  return (
    <div className="search-section">
      <div className="search-header">
        <label className="search-label" htmlFor="store-search">
          <span className="material-symbols-outlined search-ai-sparkle filled">auto_awesome</span>
          Busca Inteligente com IA
        </label>
        <span className="search-badge">Assistente em Tempo Real</span>
      </div>

      <form onSubmit={handleSubmit} className={`search-bar-form ${isFocused ? 'focused' : ''}`}>
        <span className="material-symbols-outlined search-icon">search</span>
        <input
          id="store-search"
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          placeholder="Ex: Como trocar lâmpada, fita LED, disjuntor..."
          className="search-input"
          autoComplete="off"
        />
        {searchQuery && (
          <button 
            type="button" 
            className="clear-search-btn" 
            onClick={() => setSearchQuery('')}
            aria-label="Limpar busca"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        )}
        <button type="submit" className="search-submit-btn" aria-label="Pesquisar">
          <span className="material-symbols-outlined">arrow_forward</span>
        </button>
      </form>

      {suggestions && suggestions.length > 0 && (
        <div className="search-suggestions">
          <span className="suggestions-title">Sugestões rápidas:</span>
          <div className="suggestion-chips">
            {suggestions.map((item, idx) => (
              <button 
                key={idx} 
                className="suggestion-chip"
                onClick={() => handleSuggestionClick(item)}
              >
                {item}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
