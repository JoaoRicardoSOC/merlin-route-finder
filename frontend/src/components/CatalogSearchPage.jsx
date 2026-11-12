import { useRef, useEffect } from 'react'
import ActiveFilterChips from './ActiveFilterChips'
import ProductCard from './ProductCard'

export default function CatalogSearchPage({
  searchQuery,
  setSearchQuery,
  onBackToHome,
  secoes = [],
  selectedSecao,
  onSelectSecao,
  totalProductsCount,
  produtos = [],
  totalDeProdutos = 0,
  onCarregarMais,
  isCarregandoMais = false,
  isLoadingProdutos,
  falhaAoCarregar = false,
  onTentarNovamente,
  onPerguntarAoAssistente,
  apenasDisponiveis,
  setApenasDisponiveis,
  activeFiltersCount,
  onOpenFacetModal,
  selectedAtributos,
  facetas,
  onRemoveAtributo,
  onClearAllFilters,
  onOpenSectorsDrawer,
  onAddToCart,
  onNavigateToProduct,
  onViewProductDetails,
  autoFocusSearch = false
}) {
  const searchInputRef = useRef(null)

  /*
   * Lê o que está na tela contra o total do recorte, e não a página contra o total de páginas.
   * Se uma resposta vier menor que o esperado, o botão some sozinho em vez de prometer uma
   * página que não existe.
   */
  const temMaisParaCarregar = produtos.length > 0 && produtos.length < totalDeProdutos

  // Ensure persistent focus upon arrival and cursor positioning at the end
  useEffect(() => {
    const focusInput = () => {
      if (searchInputRef.current) {
        searchInputRef.current.focus()
        try {
          const len = searchInputRef.current.value.length
          searchInputRef.current.setSelectionRange(len, len)
        } catch {
          // ignore if not supported
        }
      }
    }

    focusInput()
    const t1 = setTimeout(focusInput, 50)
    const t2 = setTimeout(focusInput, 200)

    return () => {
      clearTimeout(t1)
      clearTimeout(t2)
    }
  }, [autoFocusSearch])

  return (
    <div className="search-page-view">
      {/* Search Page Sticky Header (Glides to top smoothly) */}
      <div className="search-page-top-bar search-bar-glided-top">
        <button
          type="button"
          className="search-back-btn"
          onClick={onBackToHome}
          aria-label="Voltar para o início"
          title="Voltar para a tela inicial"
        >
          <span className="material-symbols-outlined" aria-hidden="true">arrow_back</span>
        </button>

        <div className="search-page-input-wrap">
          <span className="material-symbols-outlined search-input-icon" aria-hidden="true">search</span>
          <input
            ref={searchInputRef}
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Pesquisar produtos, materiais ou ferramentas..."
            className="search-page-input"
            autoComplete="off"
            autoFocus
          />
          {searchQuery && (
            <button
              type="button"
              className="search-page-clear-btn"
              onClick={() => {
                setSearchQuery('')
                if (searchInputRef.current) searchInputRef.current.focus()
              }}
              aria-label="Limpar pesquisa"
            >
              <span className="material-symbols-outlined" aria-hidden="true">close</span>
            </button>
          )}
        </div>
      </div>

      {/* Main Results Container (Fades/Glides in from below) */}
      <div className="search-page-content search-results-content-appear">
        {/* Minimalist Sector Pills */}
        <div className="minimal-sector-tabs-wrap">
          <div className="minimal-sector-tabs">
            <button
              type="button"
              className={`minimal-sector-pill ${selectedSecao === 'todos' ? 'active' : ''}`}
              onClick={() => onSelectSecao('todos')}
            >
              <span>Todos</span>
              <span className="minimal-pill-count">({totalProductsCount})</span>
            </button>
            {secoes.map((s) => (
              <button
                key={s.nome}
                type="button"
                className={`minimal-sector-pill ${selectedSecao === s.nome ? 'active' : ''}`}
                onClick={() => onSelectSecao(s.nome)}
              >
                <span>{s.nome}</span>
                <span className="minimal-pill-count">({s.quantidadeProdutos})</span>
              </button>
            ))}
          </div>
        </div>

        {/* Quick Filter Bar: Characteristics Modal Button, Availability Toggle & Sectors Drawer */}
        <div className="catalog-filters-bar">
          <div className="filters-left-group">
            <button
              type="button"
              className={`characteristics-filter-btn ${activeFiltersCount > 0 ? 'has-active' : ''}`}
              onClick={onOpenFacetModal}
              title="Filtrar por marca, grão, bitola, amperagem, etc."
            >
              <span className="material-symbols-outlined" aria-hidden="true">tune</span>
              <span>Filtros</span>
              {activeFiltersCount > 0 && (
                <span className="filter-count-badge">{activeFiltersCount}</span>
              )}
            </button>

            <label className="toggle-availability-label">
              <input
                type="checkbox"
                checked={apenasDisponiveis}
                onChange={(e) => setApenasDisponiveis(e.target.checked)}
                className="toggle-checkbox"
              />
              <span className="toggle-custom-slider"></span>
              <span className="toggle-text">Apenas disponíveis hoje</span>
            </label>
          </div>

          <button
            type="button"
            className="view-all-sectors-btn"
            onClick={onOpenSectorsDrawer}
          >
            <span className="material-symbols-outlined" aria-hidden="true">menu_open</span>
            <span>Todos os Setores</span>
          </button>
        </div>

        {/* Active Filter Chips */}
        <ActiveFilterChips
          selectedAtributos={selectedAtributos}
          facetas={facetas}
          apenasDisponiveis={apenasDisponiveis}
          onToggleDisponiveis={setApenasDisponiveis}
          onRemoveAtributo={onRemoveAtributo}
          onClearAll={onClearAllFilters}
        />

        {/* Products Results Header */}
        <div className="search-results-info-bar">
          <h1 className="search-results-heading">
            {searchQuery
              ? `Resultados para "${searchQuery}"`
              : selectedSecao === 'todos'
              ? 'Todos os Produtos'
              : `Produtos de ${selectedSecao}`}
          </h1>
          {/* O "de" só aparece quando há mais do que está na tela: com tudo carregado,
              "111 de 111" seria ruído. */}
          <span className="products-count-badge">
            {temMaisParaCarregar
              ? `${produtos.length} de ${totalDeProdutos} itens`
              : `${produtos.length} ${produtos.length === 1 ? 'item' : 'itens'}`}
          </span>
        </div>

        {/* Products Grid / Skeleton / Empty State */}
        {isLoadingProdutos ? (
          <div className="products-grid">
            {[1, 2, 3, 4, 5, 6].map((idx) => (
              <div key={idx} className="product-card-skeleton">
                <div className="skeleton-visual"></div>
                <div className="skeleton-body">
                  <div className="skeleton-line title"></div>
                  <div className="skeleton-line text"></div>
                  <div className="skeleton-line short"></div>
                </div>
              </div>
            ))}
          </div>
        ) : falhaAoCarregar ? (
          /* Antes do estado vazio, e nunca junto dele: "nenhum produto encontrado" afirma que
             procuramos e não há. Quando a loja não responde, não chegamos a procurar. */
          <div className="empty-catalog-state">
            <span className="material-symbols-outlined empty-icon" aria-hidden="true">cloud_off</span>
            <h2 className="empty-title">Não conseguimos falar com a loja</h2>
            <p className="empty-desc">
              O catálogo não respondeu agora, então não dá para mostrar os produtos. Não é que
              a loja não tenha o que você procura — é que não conseguimos consultar.
            </p>
            <div className="empty-actions">
              <button
                type="button"
                className="empty-action-btn primary"
                onClick={onTentarNovamente}
              >
                Tentar de novo
              </button>
            </div>
          </div>
        ) : produtos.length === 0 ? (
          <div className="empty-catalog-state">
            <span className="material-symbols-outlined empty-icon" aria-hidden="true">search_off</span>
            <h2 className="empty-title">Nenhum produto encontrado</h2>
            <p className="empty-desc">
              Não encontramos itens para a combinação de filtros selecionada{' '}
              {selectedSecao !== 'todos' && <strong>em {selectedSecao}</strong>}
              {searchQuery && <span> com o termo "<em>{searchQuery}</em>"</span>}.
            </p>
            {/*
              * A saída mais útil quando a loja não tem o que foi procurado: o assistente é o
              * único que responde "o que eu uso para isso?". Sem isto o estado vazio oferecia
              * três formas de refazer a mesma busca, e nenhuma de mudar de pergunta.
              */}
            {onPerguntarAoAssistente && (
              <div className="empty-actions">
                <button
                  type="button"
                  className="empty-action-btn primary"
                  onClick={onPerguntarAoAssistente}
                >
                  <span className="material-symbols-outlined" aria-hidden="true">smart_toy</span>
                  Perguntar ao assistente
                </button>
              </div>
            )}
            <div className="empty-actions">
              {activeFiltersCount > 0 && (
                <button
                  type="button"
                  className="empty-action-btn primary"
                  onClick={onClearAllFilters}
                >
                  Limpar características e filtros
                </button>
              )}
              {selectedSecao !== 'todos' && (
                <button
                  type="button"
                  className="empty-action-btn"
                  onClick={() => onSelectSecao('todos')}
                >
                  Ver todas as seções
                </button>
              )}
              {searchQuery && (
                <button
                  type="button"
                  className="empty-action-btn"
                  onClick={() => {
                    setSearchQuery('')
                    if (searchInputRef.current) searchInputRef.current.focus()
                  }}
                >
                  Limpar pesquisa
                </button>
              )}
            </div>
          </div>
        ) : (
          <div className="products-grid">
            {produtos.map((prod) => (
              <ProductCard
                key={prod.id || prod.sku}
                product={prod}
                onAddToCart={onAddToCart}
                onNavigateToProduct={onNavigateToProduct}
                onViewDetails={onViewProductDetails}
              />
            ))}
          </div>
        )}

        {temMaisParaCarregar && !isLoadingProdutos && (
          <div className="carregar-mais">
            <button
              type="button"
              className="empty-action-btn"
              onClick={onCarregarMais}
              disabled={isCarregandoMais}
            >
              {isCarregandoMais
                ? 'Carregando…'
                : `Carregar mais (${totalDeProdutos - produtos.length} restantes)`}
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
