import React from 'react'
import leroyLogo from '../assets/leroy_logo.png'

export default function Header({
  activeTab,
  setActiveTab,
  onMenuClick,
  onOpenSectors,
  onOpenRoteiro,
  cartCount = 0
}) {
  return (
    <header className="app-header">
      <div className="header-inner">
        <button 
          className="icon-button mobile-menu-btn" 
          aria-label="Abrir menu de setores" 
          onClick={onMenuClick || onOpenSectors}
        >
          <span className="material-symbols-outlined">menu</span>
        </button>

        <div className="brand-logo" onClick={() => setActiveTab('home')}>
          <img src={leroyLogo} alt="Leroy Merlin" className="brand-logo-img" />
        </div>

        <nav className="desktop-nav">
          <button 
            className={`nav-link ${activeTab === 'home' ? 'active' : ''}`}
            onClick={() => setActiveTab('home')}
          >
            Home
          </button>
          <button 
            className="nav-link"
            onClick={onOpenSectors}
            title="Ver catálogo físico e todos os setores da loja"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px', marginRight: '4px' }}>storefront</span>
            Setores da Loja
          </button>
          <button 
            className={`nav-link ${activeTab === 'scan' ? 'active' : ''}`}
            onClick={() => setActiveTab('scan')}
          >
            Scan & Rota
          </button>
          <button 
            className={`nav-link ${activeTab === 'projects' ? 'active' : ''}`}
            onClick={() => setActiveTab('projects')}
          >
            Projetos
          </button>
          <button 
            className={`nav-link ${activeTab === 'support' ? 'active' : ''}`}
            onClick={() => setActiveTab('support')}
          >
            Atendimento
          </button>
        </nav>

        {/* Header Actions: Cart / Roteiro Button with Notification Badge */}
        <div className="header-actions">
          <button 
            className="icon-button cart-header-btn" 
            aria-label={`Meu roteiro com ${cartCount} itens`}
            title="Abrir meu roteiro de compras"
            onClick={onOpenRoteiro}
          >
            <span className="material-symbols-outlined">shopping_cart</span>
            {cartCount > 0 && (
              <span className="cart-badge-counter animate-pop" aria-hidden="true">
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </button>
        </div>
      </div>
    </header>
  )
}
