import React from 'react'

export default function BottomNav({ activeTab, setActiveTab }) {
  return (
    <nav className="bottom-nav-mobile" aria-label="Navegação inferior mobile">
      <button
        className={`bottom-nav-item ${activeTab === 'home' ? 'active' : ''}`}
        onClick={() => setActiveTab('home')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'home' ? 'filled' : ''}`}>
          home
        </span>
        <span className="bottom-nav-label">Home</span>
      </button>

      <button
        className={`bottom-nav-item ${activeTab === 'scan' ? 'active' : ''}`}
        onClick={() => setActiveTab('scan')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'scan' ? 'filled' : ''}`}>
          barcode_scanner
        </span>
        <span className="bottom-nav-label">Scan</span>
      </button>

      <button
        className={`bottom-nav-item ${activeTab === 'projects' ? 'active' : ''}`}
        onClick={() => setActiveTab('projects')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'projects' ? 'filled' : ''}`}>
          architecture
        </span>
        <span className="bottom-nav-label">Projetos</span>
      </button>

      <button
        className={`bottom-nav-item ${activeTab === 'support' ? 'active' : ''}`}
        onClick={() => setActiveTab('support')}
      >
        <span className={`material-symbols-outlined ${activeTab === 'support' ? 'filled' : ''}`}>
          support_agent
        </span>
        <span className="bottom-nav-label">Atendimento</span>
      </button>
    </nav>
  )
}
