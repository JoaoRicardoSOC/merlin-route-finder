import React from 'react'

export default function BottomNav({ abaAtiva, aoTrocarDeAba }) {
  return (
    <nav className="bottom-nav-mobile" aria-label="Navegação inferior mobile">
      <button
        type="button"
        className={`bottom-nav-item ${abaAtiva === 'home' ? 'active' : ''}`}
        onClick={() => aoTrocarDeAba('home')}
      >
        <span className={`material-symbols-outlined ${abaAtiva === 'home' ? 'filled' : ''}`}>
          home
        </span>
        <span className="bottom-nav-label">Home</span>
      </button>

      <button
        type="button"
        className={`bottom-nav-item ${abaAtiva === 'map' ? 'active' : ''}`}
        onClick={() => aoTrocarDeAba('map')}
      >
        <span className={`material-symbols-outlined ${abaAtiva === 'map' ? 'filled' : ''}`}>
          map
        </span>
        <span className="bottom-nav-label">Mapa</span>
      </button>

      <button
        type="button"
        className={'bottom-nav-item'}
        onClick={() => aoTrocarDeAba('scan')}
      >
        <span className={'material-symbols-outlined'}>
          qr_code_scanner
        </span>
        <span className="bottom-nav-label">Scan</span>
      </button>

      <button
        type="button"
        className={`bottom-nav-item ${abaAtiva === 'sectors' ? 'active' : ''}`}
        onClick={() => aoTrocarDeAba('sectors')}
      >
        <span className={`material-symbols-outlined ${abaAtiva === 'sectors' ? 'filled' : ''}`}>
          grid_view
        </span>
        <span className="bottom-nav-label">Setores</span>
      </button>

      <button
        type="button"
        className={'bottom-nav-item'}
        onClick={() => aoTrocarDeAba('support')}
      >
        <span className={'material-symbols-outlined'}>
          smart_toy
        </span>
        <span className="bottom-nav-label">Assistente</span>
      </button>
    </nav>
  )
}

