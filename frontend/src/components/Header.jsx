import React from 'react'
import leroyLogo from '../assets/leroy_logo.png'

export default function Header({
  abaAtiva,
  aoTrocarDeAba,
  onOpenSectors,
  onOpenRoteiro,
  onSearchClick,
  cartCount = 0
}) {
  return (
    <header className="app-header">
      <div className="header-inner">
        {/* Left Action: Search Lupa Button */}
        <button 
          className="icon-button header-search-btn" 
          aria-label="Buscar produtos no catálogo" 
          title="Pesquisar produtos na loja"
          onClick={onSearchClick}
          type="button"
        >
          <span className="material-symbols-outlined" aria-hidden="true">search</span>
        </button>

        <div className="brand-logo" onClick={() => aoTrocarDeAba('home')}>
          <img src={leroyLogo} alt="Leroy Merlin" className="brand-logo-img" />
        </div>

        <nav className="desktop-nav">
          <button 
            className={`nav-link ${abaAtiva === 'home' ? 'active' : ''}`}
            onClick={() => aoTrocarDeAba('home')}
          >
            Home
          </button>
          <button 
            className={`nav-link ${abaAtiva === 'sectors' ? 'active' : ''}`}
            onClick={onOpenSectors}
            title="Ver catálogo físico e todos os setores da loja"
          >
            <span className="material-symbols-outlined" style={{ fontSize: '18px', marginRight: '4px' }} aria-hidden="true">storefront</span>
            Setores da Loja
          </button>
          {/*
            * Estes dois abrem MODAL, e por isso não recebem destaque de "você está aqui" —
            * um modal acontece por cima da tela em que o cliente já está, não no lugar dela.
            * Marcá-los como destino era a origem do destaque que ficava preso. Ver D-81.
            */}
          <button 
            className="nav-link"
            onClick={() => aoTrocarDeAba('scan')}
          >
            Scan & Rota
          </button>
          <button 
            className="nav-link"
            onClick={() => aoTrocarDeAba('support')}
          >
            Atendimento
          </button>
        </nav>

        {/* Right Action: Cart / Roteiro Button with Notification Badge */}
        <div className="header-actions">
          <button 
            className="icon-button cart-header-btn" 
            aria-label={`Meu roteiro com ${cartCount} ${cartCount === 1 ? 'item' : 'itens'}`}
            title="Abrir meu roteiro de compras"
            onClick={onOpenRoteiro}
            type="button"
          >
            <span className="material-symbols-outlined" aria-hidden="true">shopping_cart</span>
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
