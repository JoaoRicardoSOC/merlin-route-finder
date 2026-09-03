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

        {/*
          * O logo leva para o inicio, e isso e convencao que todo mundo espera de um site.
          * Ele era um <div> com onClick: funcionava no dedo e nao existia para o teclado, que
          * e a mesma promessa quebrada dos cartoes de produto (D-84). Como <button> ele ganha
          * Enter e Espaco do navegador, sem uma linha de tratador.
          *
          * O `alt` da imagem sai: com o botao nomeado, ele seria lido duas vezes.
          */}
        <button
          type="button"
          className="brand-logo"
          onClick={() => aoTrocarDeAba('home')}
          aria-label="Ir para o início"
        >
          <img src={leroyLogo} alt="" className="brand-logo-img" />
        </button>

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
