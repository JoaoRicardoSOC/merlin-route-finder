import React, { useState, useEffect, useCallback, useRef } from 'react'
import Header from './components/Header'
import LocationStatus from './components/LocationStatus'
import SearchBar from './components/SearchBar'
import BentoActions from './components/BentoActions'
import SectorExplorer from './components/SectorExplorer'
import SectorBanner from './components/SectorBanner'
import SectorsDrawer from './components/SectorsDrawer'
import LocationCodeModal from './components/LocationCodeModal'
import ProductDetailModal from './components/ProductDetailModal'
import RoteiroDrawer from './components/RoteiroDrawer'
import ProductCard from './components/ProductCard'
import PromoBanner from './components/PromoBanner'
import RouteModal from './components/RouteModal'
import SplashScreen from './components/SplashScreen'
import BottomNav from './components/BottomNav'
import {
  fetchSecoes,
  fetchProdutos,
  SECTOR_METADATA,
  DEFAULT_SECTOR_META
} from './services/catalogService'
import {
  obterOuCriarSessao,
  recentrarPosicao,
  normalizarCodigo
} from './services/sessionService'
import {
  consultarRoteiro,
  adicionarAoRoteiro,
  removerDoRoteiro,
  limparRoteiroLocal
} from './services/roteiroService'
import './App.css'

const SEARCH_SUGGESTIONS = [
  'Lâmpada LED',
  'Tinta Acrílica',
  'Parafusadeira',
  'Fita LED Smart',
  'Disjuntor 32A',
  'Cimento 50kg'
]

function App() {
  const [activeTab, setActiveTab] = useState('home')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedSecao, setSelectedSecao] = useState('todos')
  const [apenasDisponiveis, setApenasDisponiveis] = useState(false)
  const [toastMessage, setToastMessage] = useState(null)

  // Data states
  const [secoes, setSecoes] = useState([])
  const [produtos, setProdutos] = useState([])
  const [totalProductsCount, setTotalProductsCount] = useState(0)
  const [isLoadingSecoes, setIsLoadingSecoes] = useState(true)
  const [isLoadingProdutos, setIsLoadingProdutos] = useState(true)

  // Roteiro / Cart items state
  const [roteiroItems, setRoteiroItems] = useState([])
  const [isRoteiroDrawerOpen, setIsRoteiroDrawerOpen] = useState(false)

  // Session & Location state (UC-001)
  const [session, setSession] = useState(null)
  const [currentLocation, setCurrentLocation] = useState({
    sector: 'Entrada Principal da Loja',
    aisle: 'Entrada da Loja',
    code: 'ENT-01',
    coords: '50, 92'
  })

  // Drawer & Modals state
  const [isSectorsDrawerOpen, setIsSectorsDrawerOpen] = useState(false)
  const [isLocationModalOpen, setIsLocationModalOpen] = useState(false)
  const [selectedProductForDetail, setSelectedProductForDetail] = useState(null)
  const [modalConfig, setModalConfig] = useState({
    isOpen: false,
    title: '',
    type: '',
    data: null
  })

  const productsSectionRef = useRef(null)

  const showToast = (msg) => {
    setToastMessage(msg)
    setTimeout(() => {
      setToastMessage(null)
    }, 3200)
  }

  // Parse location code from URL (?ponto=TIN-02 or ?codigo=TIN-02)
  const getUrlPlateCode = () => {
    try {
      const params = new URLSearchParams(window.location.search)
      return params.get('ponto') || params.get('codigo') || params.get('plate') || null
    } catch (e) {
      return null
    }
  }

  // 1. Initialize or Resume Session (UC-001) & Load Roteiro
  useEffect(() => {
    let isMounted = true
    async function initSession() {
      const urlCode = getUrlPlateCode()
      try {
        const sess = await obterOuCriarSessao(urlCode)
        if (isMounted && sess) {
          setSession(sess)
          if (sess.posicaoAtual) {
            setCurrentLocation({
              sector: sess.posicaoAtual.corredor || 'Loja Leroy Merlin',
              aisle: sess.posicaoAtual.corredor || 'Corredor',
              code: sess.posicaoAtual.codigoCurto || urlCode,
              coords: sess.posicaoAtual.coordenadaX != null ? `${sess.posicaoAtual.coordenadaX}, ${sess.posicaoAtual.coordenadaY}` : null
            })
            if (urlCode) {
              showToast(`Sessão iniciada na placa: ${urlCode} (${sess.posicaoAtual.corredor})`)
            }
          }

          // Load existing items in roteiro for this session
          const items = await consultarRoteiro(sess.id)
          if (isMounted) setRoteiroItems(items)
        }
      } catch (err) {
        console.error('Erro ao inicializar sessão:', err)
      }
    }
    initSession()
    return () => { isMounted = false }
  }, [])

  // 2. Load sections from backend (ListarSecoesUseCase / SecaoResponse)
  useEffect(() => {
    let isMounted = true
    async function loadSecoes() {
      setIsLoadingSecoes(true)
      try {
        const data = await fetchSecoes()
        if (isMounted) {
          setSecoes(data)
          const sum = data.reduce((acc, curr) => acc + (curr.quantidadeProdutos || 0), 0)
          setTotalProductsCount(sum)
        }
      } catch (err) {
        console.error('Erro ao carregar seções:', err)
      } finally {
        if (isMounted) setIsLoadingSecoes(false)
      }
    }
    loadSecoes()
    return () => { isMounted = false }
  }, [])

  // 3. Load products based on selected sector, search query and availability
  const loadProdutos = useCallback(async () => {
    setIsLoadingProdutos(true)
    try {
      const response = await fetchProdutos({
        query: searchQuery,
        secao: selectedSecao === 'todos' ? '' : selectedSecao,
        apenasDisponiveis: apenasDisponiveis,
        page: 0,
        size: 50
      })
      setProdutos(response.content || [])
    } catch (err) {
      console.error('Erro ao buscar produtos:', err)
    } finally {
      setIsLoadingProdutos(false)
    }
  }, [searchQuery, selectedSecao, apenasDisponiveis])

  // Debounced search & filtering
  useEffect(() => {
    const timer = setTimeout(() => {
      loadProdutos()
    }, 200)
    return () => clearTimeout(timer)
  }, [loadProdutos])

  // Handle manual or scanned location update (PUT /api/v1/sessoes/{id}/posicao)
  const handleUpdateLocation = async (codigoPonto) => {
    if (!codigoPonto) return
    try {
      if (session && session.id) {
        const updated = await recentrarPosicao(session.id, codigoPonto)
        setSession(updated)
        if (updated.posicaoAtual) {
          setCurrentLocation({
            sector: updated.posicaoAtual.corredor,
            aisle: updated.posicaoAtual.corredor,
            code: updated.posicaoAtual.codigoCurto || codigoPonto,
            coords: updated.posicaoAtual.coordenadaX != null ? `${updated.posicaoAtual.coordenadaX}, ${updated.posicaoAtual.coordenadaY}` : null
          })
          showToast(`Posição atualizada para: ${updated.posicaoAtual.corredor} (Placa ${codigoPonto})`)
        } else {
          showToast(`Placa ${codigoPonto} registrada.`)
        }
      } else {
        const sess = await obterOuCriarSessao(codigoPonto)
        setSession(sess)
        if (sess.posicaoAtual) {
          setCurrentLocation({
            sector: sess.posicaoAtual.corredor,
            aisle: sess.posicaoAtual.corredor,
            code: sess.posicaoAtual.codigoCurto || codigoPonto,
            coords: `${sess.posicaoAtual.coordenadaX}, ${sess.posicaoAtual.coordenadaY}`
          })
          showToast(`Sessão iniciada na placa: ${codigoPonto}`)
        }
      }
    } catch (e) {
      console.error('Erro ao atualizar posição:', e)
      showToast(`Placa ${codigoPonto} configurada localmente.`)
    }
  }

  const handleSelectSecao = (secaoNome) => {
    setSelectedSecao(secaoNome)
    if (secaoNome !== 'todos') {
      const meta = SECTOR_METADATA[secaoNome] || DEFAULT_SECTOR_META
      showToast(`Filtrando pelo setor: ${secaoNome} (${meta.corredor})`)
    } else {
      showToast('Exibindo catálogo completo de todas as seções')
    }

    if (productsSectionRef.current) {
      const yOffset = -80
      const element = productsSectionRef.current
      const y = element.getBoundingClientRect().top + window.pageYOffset + yOffset
      window.scrollTo({ top: y, behavior: 'smooth' })
    }
  }

  // Roteiro Actions
  const handleAddToCart = async (product) => {
    const updated = await adicionarAoRoteiro(session?.id, product)
    setRoteiroItems(updated)
    const prodName = product.nome || product.name
    const corredor = product.corredor || 'Corredor da Loja'
    showToast(`"${prodName}" adicionado ao seu roteiro! (${corredor})`)
  }

  const handleRemoveFromRoteiro = async (itemId) => {
    const updated = await removerDoRoteiro(session?.id, itemId)
    setRoteiroItems(updated)
    showToast('Item removido do roteiro')
  }

  const handleClearRoteiro = () => {
    limparRoteiroLocal()
    setRoteiroItems([])
    showToast('Roteiro esvaziado')
  }

  const handleNavigateToProduct = (product) => {
    setModalConfig({
      isOpen: true,
      title: `Rota até: ${product.nome || product.name}`,
      type: 'route',
      data: product
    })
  }

  const handleStartFullRoute = (items) => {
    setModalConfig({
      isOpen: true,
      title: `Rota Otimizada da Compra (${items.length} itens)`,
      type: 'route',
      data: {
        name: `${items.length} paradas na loja`,
        corredor: items.map(i => i.corredor).filter(Boolean).slice(0, 3).join(' ➔ ') + (items.length > 3 ? '...' : '')
      }
    })
  }

  const handleOpenMap = () => {
    setModalConfig({
      isOpen: true,
      title: 'Planta Inteligente & Corredores da Loja',
      type: 'map',
      data: null
    })
  }

  const handleCallSpecialist = () => {
    setModalConfig({
      isOpen: true,
      title: 'Solicitação de Especialista Presencial',
      type: 'specialist',
      data: null
    })
  }

  // Handle bottom navigation tab clicks
  const handleTabChange = (tabKey) => {
    setActiveTab(tabKey)
    if (tabKey === 'scan') {
      setIsLocationModalOpen(true)
    } else if (tabKey === 'projects') {
      setIsRoteiroDrawerOpen(true)
    }
  }

  return (
    <div className="app-root">
      {/* Intro Split Splash Screen */}
      <SplashScreen />

      {/* Header / TopAppBar with Cart / Roteiro Counter */}
      <Header
        activeTab={activeTab}
        setActiveTab={handleTabChange}
        onMenuClick={() => setIsSectorsDrawerOpen(true)}
        onOpenSectors={() => setIsSectorsDrawerOpen(true)}
        onOpenRoteiro={() => setIsRoteiroDrawerOpen(true)}
        cartCount={roteiroItems.length}
      />

      {/* Main Canvas */}
      <main className="main-content">
        {/* Welcome Section */}
        <section className="welcome-section">
          <h1 className="welcome-title">Bem-vindo à Leroy Merlin!</h1>
          <p className="welcome-subtitle">
            Encontre produtos com facilidade, explore os corredores temáticos pelo mapa inteligente e trace a rota ideal para sua compra na loja.
          </p>
        </section>

        {/* Location Status Chip (QR Code & Placa Indicator) */}
        <LocationStatus
          location={currentLocation}
          onChangeLocation={() => setIsLocationModalOpen(true)}
          onViewMap={handleOpenMap}
        />

        {/* AI Smart Search */}
        <SearchBar
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          suggestions={SEARCH_SUGGESTIONS}
          onSearch={(query) => {
            if (query) showToast(`Buscando no catálogo: "${query}"`)
          }}
        />

        {/* Bento Grid Actions */}
        <BentoActions
          currentSectorName={selectedSecao !== 'todos' ? selectedSecao : 'Iluminação'}
          onViewProducts={() => {
            if (selectedSecao === 'todos') {
              handleSelectSecao('Iluminação')
            } else {
              handleSelectSecao(selectedSecao)
            }
          }}
          onCallSpecialist={handleCallSpecialist}
          onViewMap={handleOpenMap}
        />

        {/* Physical Store Sector Explorer (ListarSecoesUseCase) */}
        <SectorExplorer
          secoes={secoes}
          selectedSecao={selectedSecao}
          onSelectSecao={handleSelectSecao}
          totalProductsCount={totalProductsCount}
          isLoading={isLoadingSecoes}
        />

        {/* Active Sector Contextual Banner */}
        <SectorBanner
          selectedSecao={selectedSecao}
          productCount={produtos.length}
          onClearFilter={() => handleSelectSecao('todos')}
          onOpenMap={handleOpenMap}
        />

        {/* Products Grid Section (Vitrine do Catálogo) */}
        <section className="products-section" ref={productsSectionRef}>
          <div className="section-header-wrap">
            <div className="products-header-title-bar">
              <h2 className="section-heading">
                {selectedSecao === 'todos' ? 'Vitrine de Produtos' : `Produtos de ${selectedSecao}`}
              </h2>
              <span className="products-count-badge">
                {produtos.length} {produtos.length === 1 ? 'item' : 'itens'}
              </span>
            </div>

            {/* Quick Filter Bar: Availability switch & Drawer button */}
            <div className="catalog-filters-bar">
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

              <button
                type="button"
                className="view-all-sectors-btn"
                onClick={() => setIsSectorsDrawerOpen(true)}
              >
                <span className="material-symbols-outlined">menu_open</span>
                <span>Todos os Setores</span>
              </button>
            </div>
          </div>

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
          ) : produtos.length === 0 ? (
            <div className="empty-catalog-state">
              <span className="material-symbols-outlined empty-icon">search_off</span>
              <h3 className="empty-title">Nenhum produto encontrado</h3>
              <p className="empty-desc">
                Não encontramos itens para o filtro selecionado{' '}
                {selectedSecao !== 'todos' && <strong>em {selectedSecao}</strong>}
                {searchQuery && <span> com o termo "<em>{searchQuery}</em>"</span>}.
              </p>
              <div className="empty-actions">
                {selectedSecao !== 'todos' && (
                  <button
                    type="button"
                    className="empty-action-btn primary"
                    onClick={() => handleSelectSecao('todos')}
                  >
                    Ver todas as seções
                  </button>
                )}
                {searchQuery && (
                  <button
                    type="button"
                    className="empty-action-btn"
                    onClick={() => setSearchQuery('')}
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
                  onAddToCart={handleAddToCart}
                  onNavigateToProduct={handleNavigateToProduct}
                  onViewDetails={(product) => setSelectedProductForDetail(product)}
                />
              ))}
            </div>
          )}
        </section>

        {/* Featured Promotional Banner */}
        <PromoBanner
          onExplore={() => {
            handleSelectSecao('Iluminação')
            showToast('Exibindo novidades do setor de Iluminação inteligente!')
          }}
        />
      </main>

      {/* Bottom Navigation for Mobile */}
      <BottomNav
        activeTab={activeTab}
        setActiveTab={handleTabChange}
      />

      {/* Roteiro / Shopping List Drawer */}
      <RoteiroDrawer
        isOpen={isRoteiroDrawerOpen}
        onClose={() => setIsRoteiroDrawerOpen(false)}
        items={roteiroItems}
        onRemoveItem={handleRemoveFromRoteiro}
        onClearAll={handleClearRoteiro}
        onStartRoute={handleStartFullRoute}
      />

      {/* Product Detail Modal (UC-003) */}
      <ProductDetailModal
        isOpen={!!selectedProductForDetail}
        onClose={() => setSelectedProductForDetail(null)}
        product={selectedProductForDetail}
        onAddToCart={handleAddToCart}
        onNavigateToProduct={handleNavigateToProduct}
      />

      {/* Location Code & QR Scanner Modal (Plan A & Plan B) */}
      <LocationCodeModal
        isOpen={isLocationModalOpen}
        onClose={() => setIsLocationModalOpen(false)}
        currentLocation={currentLocation}
        onUpdateLocation={handleUpdateLocation}
      />

      {/* Sectors Full Drawer / Modal */}
      <SectorsDrawer
        isOpen={isSectorsDrawerOpen}
        onClose={() => setIsSectorsDrawerOpen(false)}
        secoes={secoes}
        selectedSecao={selectedSecao}
        onSelectSecao={handleSelectSecao}
        onOpenMap={handleOpenMap}
      />

      {/* Interactive Modal (Map, Specialist, Route) */}
      <RouteModal
        isOpen={modalConfig.isOpen}
        onClose={() => setModalConfig({ ...modalConfig, isOpen: false })}
        title={modalConfig.title}
        type={modalConfig.type}
        data={modalConfig.data}
      />

      {/* Toast Notification */}
      {toastMessage && (
        <div className="toast-notice">
          <span className="material-symbols-outlined filled">check_circle</span>
          <span>{toastMessage}</span>
        </div>
      )}
    </div>
  )
}

export default App
