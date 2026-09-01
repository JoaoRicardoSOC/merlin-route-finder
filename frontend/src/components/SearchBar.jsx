import React, { useState } from 'react'

export default function SearchBar({
  searchQuery,
  setSearchQuery,
  onSearch,
  suggestions = [],
  inputRef,
  isGliding = false
}) {
  const [isFocused, setIsFocused] = useState(false)

  const handleSubmit = (e) => {
    e.preventDefault()
    if (onSearch) onSearch(searchQuery)
  }

  const handleInputClick = () => {
    if (onSearch) onSearch(searchQuery)
  }

  const handleSuggestionClick = (item) => {
    setSearchQuery(item)
    if (onSearch) onSearch(item)
  }

  return (
    <div className={`search-section ${isGliding ? 'search-bar-gliding' : ''}`} id="store-search-section">
      <div className="search-header">
        <label className="search-label" htmlFor="store-search">
          <span className="material-symbols-outlined search-ai-sparkle filled" aria-hidden="true">search</span>
          Buscar no Catálogo
        </label>
      </div>

      <form 
        onSubmit={handleSubmit} 
        onClick={handleInputClick}
        className={`search-bar-form ${isFocused ? 'focused' : ''}`}
      >
        <span className="material-symbols-outlined search-icon" aria-hidden="true">search</span>
        <input
          ref={inputRef}
          id="store-search"
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onClick={handleInputClick}
          onFocus={() => {
            setIsFocused(true)
            handleInputClick()
          }}
          onBlur={() => setIsFocused(false)}
          placeholder="Pesquisar produtos, materiais ou ferramentas..."
          className="search-input"
          autoComplete="off"
        />
        {searchQuery && (
          <button 
            type="button" 
            className="clear-search-btn" 
            onClick={(e) => {
              e.stopPropagation()
              setSearchQuery('')
            }}
            aria-label="Limpar busca"
          >
            <span className="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        )}
        <button 
          type="submit" 
          className="search-submit-btn" 
          aria-label="Pesquisar e abrir catálogo"
          title="Pesquisar produtos"
        >
          <span className="material-symbols-outlined" aria-hidden="true">arrow_forward</span>
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
