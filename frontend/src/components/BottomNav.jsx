import React from 'react'

export default function BottomNav({ activeTab, setActiveTab }) {
  return (
    <nav className="bottom-nav-mobile" aria-label="Navegação inferior mobile">
      <button
        type="button"
        className={`bottom-nav-item ${activeTab === 'home' ? 'active' : ''}`}
        onClick={() => setActiveTab('home')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'home' ? 'filled' : ''}`}>
          home
        </span>
        <span className="bottom-nav-label">Home</span>
      </button>

      <button
        type="button"
        className={`bottom-nav-item ${activeTab === 'map' ? 'active' : ''}`}
        onClick={() => setActiveTab('map')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'map' ? 'filled' : ''}`}>
          map
        </span>
        <span className="bottom-nav-label">Mapa</span>
      </button>

      <button
        type="button"
        className={`bottom-nav-item ${activeTab === 'scan' ? 'active' : ''}`}
        onClick={() => setActiveTab('scan')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'scan' ? 'filled' : ''}`}>
          qr_code_scanner
        </span>
        <span className="bottom-nav-label">Scan</span>
      </button>

      <button
        type="button"
        className={`bottom-nav-item ${activeTab === 'sectors' ? 'active' : ''}`}
        onClick={() => setActiveTab('sectors')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'sectors' ? 'filled' : ''}`}>
          grid_view
        </span>
        <span className="bottom-nav-label">Setores</span>
      </button>

      <button
        type="button"
        className={`bottom-nav-item ${activeTab === 'support' ? 'active' : ''}`}
        onClick={() => setActiveTab('support')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'support' ? 'filled' : ''}`}>
          smart_toy
        </span>
        <span className="bottom-nav-label">Assistente</span>
      </button>
    </nav>
  )
}

