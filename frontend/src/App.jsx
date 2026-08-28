import React, { useState, useEffect, useCallback } from 'react'
import Header from './components/Header'
import LocationStatus from './components/LocationStatus'
import SearchBar from './components/SearchBar'
import HomeBentoGrid from './components/HomeBentoGrid'
import SectorsPage from './components/SectorsPage'
import CatalogSearchPage from './components/CatalogSearchPage'
import ProductDetailPage from './components/ProductDetailPage'
import StoreMapPage from './components/StoreMapPage'
import SectorsDrawer from './components/SectorsDrawer'
import LocationCodeModal from './components/LocationCodeModal'
import RoteiroDrawer from './components/RoteiroDrawer'
import FacetFiltersModal from './components/FacetFiltersModal'
import PromoBanner from './components/PromoBanner'
import RouteModal from './components/RouteModal'
import SplashScreen from './components/SplashScreen'
import BottomNav from './components/BottomNav'
import FloatingAIChatButton from './components/FloatingAIChatButton'
import AIChatModal from './components/AIChatModal'
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
  alternarColetaItem,
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
  const [currentView, setCurrentView] = useState('home') // 'home' | 'search' | 'sectors' | 'product-detail'
  const [previousView, setPreviousView] = useState('home')
  const [activeTab, setActiveTab] = useState('home')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedSecao, setSelectedSecao] = useState('todos')
  const [apenasDisponiveis, setApenasDisponiveis] = useState(false)
  const [selectedAtributos, setSelectedAtributos] = useState({})
  const [toastMessage, setToastMessage] = useState(null)
  const [autoFocusSearch, setAutoFocusSearch] = useState(false)
  const [isGlidingSearch, setIsGlidingSearch] = useState(false)

  // Data states
  const [secoes, setSecoes] = useState([])
  const [produtos, setProdutos] = useState([])
  const [facetas, setFacetas] = useState([])
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
  const [isFacetModalOpen, setIsFacetModalOpen] = useState(false)
  const [isAIChatOpen, setIsAIChatOpen] = useState(false)
  const [selectedProductForDetail, setSelectedProductForDetail] = useState(null)
  const [focusedProductForMap, setFocusedProductForMap] = useState(null)
  const [modalConfig, setModalConfig] = useState({
    isOpen: false,
    title: '',
    type: '',
    data: null
  })

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

  // 3. Load products based on sector, query, availability and dynamic attribute facets (UC-002 / Passo 5)
  const loadProdutos = useCallback(async () => {
    setIsLoadingProdutos(true)
    try {
      const response = await fetchProdutos({
        query: searchQuery,
        secao: selectedSecao === 'todos' ? '' : selectedSecao,
        apenasDisponiveis: apenasDisponiveis,
        atributos: selectedAtributos,
        page: 0,
        size: 50
      })
      setProdutos(response.content || [])
      setFacetas(response.facetas || [])
    } catch (err) {
      console.error('Erro ao buscar produtos:', err)
    } finally {
      setIsLoadingProdutos(false)
    }
  }, [searchQuery, selectedSecao, apenasDisponiveis, selectedAtributos])

  // Debounced search & filtering
  useEffect(() => {
    const timer = setTimeout(() => {
      loadProdutos()
    }, 200)
    return () => clearTimeout(timer)
  }, [loadProdutos])

  // Navigation to search view with smooth glide transition
  const handleOpenSearchPage = (shouldFocus = true, initialQuery = '') => {
    if (initialQuery) setSearchQuery(initialQuery)
    setPreviousView(currentView)
    setIsGlidingSearch(true)
    setTimeout(() => {
      setAutoFocusSearch(shouldFocus ? Date.now() : false)
      setCurrentView('search')
      setIsGlidingSearch(false)
      window.scrollTo({ top: 0, behavior: 'instant' })
    }, 180)
  }

  // Navigation to Sectors Page
  const handleOpenSectorsPage = () => {
    setPreviousView(currentView)
    setCurrentView('sectors')
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  // Navigation to Dedicated Product Detail Page
  const handleOpenProductDetailPage = (product) => {
    setSelectedProductForDetail(product)
    setPreviousView(currentView)
    setCurrentView('product-detail')
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  const handleBackToHome = () => {
    setCurrentView('home')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleBackFromSearch = () => {
    if (previousView === 'sectors') {
      setCurrentView('sectors')
    } else {
      setCurrentView('home')
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleBackFromProductDetail = () => {
    if (previousView === 'sectors') {
      setCurrentView('sectors')
    } else if (previousView === 'search') {
      setCurrentView('search')
    } else {
      setCurrentView('home')
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

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
    setSelectedAtributos({})
    setPreviousView(currentView)
    setCurrentView('search')
    if (secaoNome !== 'todos') {
      const meta = SECTOR_METADATA[secaoNome] || DEFAULT_SECTOR_META
      showToast(`Filtrando pelo setor: ${secaoNome} (${meta.corredor})`)
    } else {
      showToast('Exibindo catálogo completo de todas as seções')
    }
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  // Attribute facet toggle handler (Semântica: valores da mesma chave são "ou", chaves diferentes são "e")
  const handleToggleAtributo = (chave, valor) => {
    setSelectedAtributos(prev => {
      const currentList = prev[chave] || []
      const updatedList = currentList.includes(valor)
        ? currentList.filter(v => v !== valor)
        : [...currentList, valor]

      if (updatedList.length === 0) {
        const next = { ...prev }
        delete next[chave]
        return next
      }
      return { ...prev, [chave]: updatedList }
    })
  }

  const handleRemoveAtributo = (chave, valor) => {
    handleToggleAtributo(chave, valor)
  }

  const handleClearAllFilters = () => {
    setSelectedAtributos({})
    setApenasDisponiveis(false)
    showToast('Filtros de características limpos')
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

  const handleToggleCollectItem = async (itemId) => {
    const updated = await alternarColetaItem(itemId)
    setRoteiroItems(updated)
    const item = updated.find(i => i.id === itemId || i.produtoId === itemId)
    if (item?.coletado) {
      showToast(`Item "${item.nome}" marcado como coletado!`)
    } else {
      showToast('Item desmarcado da coleta.')
    }
  }

  const handleOpenMap = (product = null) => {
    setFocusedProductForMap(product || null)
    setPreviousView(currentView)
    setCurrentView('map')
    setActiveTab('map')
    window.scrollTo({ top: 0, behavior: 'instant' })
  }

  const handleNavigateToProduct = (product) => {
    handleOpenMap(product)
    const prodName = product.nome || product.name || 'Produto'
    showToast(`Exibindo no mapa da loja: ${prodName}`)
  }

  const handleStartFullRoute = (items) => {
    handleOpenMap()
    showToast(`Traçando rota otimizada com ${items.length} paradas!`)
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
    if (tabKey === 'home') {
      setCurrentView('home')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } else if (tabKey === 'map') {
      handleOpenMap()
    } else if (tabKey === 'scan') {
      setIsLocationModalOpen(true)
    } else if (tabKey === 'sectors') {
      handleOpenSectorsPage()
    } else if (tabKey === 'support') {
      setIsAIChatOpen(true)
    }
  }


  // Count active facet filters
  const activeFiltersCount = Object.values(selectedAtributos).reduce(
    (acc, vals) => acc + (Array.isArray(vals) ? vals.length : vals ? 1 : 0),
    0
  ) + (apenasDisponiveis ? 1 : 0)

  return (
    <div className="app-root">
      {/* Intro Split Splash Screen */}
      <SplashScreen />

      {/* Header / TopAppBar with Search Lupa & Cart / Roteiro Counter */}
      <Header
        activeTab={activeTab}
        setActiveTab={handleTabChange}
        onOpenSectors={handleOpenSectorsPage}
        onOpenRoteiro={() => setIsRoteiroDrawerOpen(true)}
        onSearchClick={() => handleOpenSearchPage(true)}
        cartCount={roteiroItems.length}
      />

      {/* Main Canvas Viewport */}
      <main className="main-content">
        {currentView === 'home' ? (
          /* ========================================================
             1. HOME VIEW (Clean, minimalist green cards, search & hero)
             ======================================================== */
          <div className={`home-view-container animate-fade-in ${isGlidingSearch ? 'is-transitioning' : ''}`}>
            {/* Welcome Section */}
            <section className="welcome-section">
              <h1 className="welcome-title">Bem-vindo à Leroy Merlin!</h1>
              <p className="welcome-subtitle">
                Encontre produtos com facilidade, explore os corredores pelo mapa inteligente e trace sua rota de compras.
              </p>
            </section>

            {/* Location Status Chip (QR Code & Placa Indicator) */}
            <LocationStatus
              location={currentLocation}
              onClick={() => setIsLocationModalOpen(true)}
            />

            {/* Home Search Bar (Submitting / searching transitions to search view) */}
            <SearchBar
              searchQuery={searchQuery}
              setSearchQuery={setSearchQuery}
              suggestions={SEARCH_SUGGESTIONS}
              onSearch={(query) => handleOpenSearchPage(false, query)}
              isGliding={isGlidingSearch}
            />

            {/* Minimalist Green Bento Action Grid (Mapa, Setores, Chamar Especialista) */}
            <HomeBentoGrid
              onOpenMap={handleOpenMap}
              onOpenSectors={handleOpenSectorsPage}
              onCallSpecialist={handleCallSpecialist}
            />

            {/* Featured Promotional Banner */}
            <PromoBanner
              onExplore={() => {
                handleSelectSecao('Iluminação')
                showToast('Exibindo novidades do setor de Iluminação!')
              }}
            />
          </div>
        ) : currentView === 'map' ? (
          /* ========================================================
             2. DEDICATED STORE MAP PAGE (Interactive Interlagos Plan)
             ======================================================== */
          <StoreMapPage
            roteiroItems={roteiroItems}
            currentLocation={currentLocation}
            onUpdateLocation={handleUpdateLocation}
            onToggleCollectItem={handleToggleCollectItem}
            onViewProductDetails={handleOpenProductDetailPage}
            onOpenRoteiro={() => setIsRoteiroDrawerOpen(true)}
            onOpenLocationModal={() => setIsLocationModalOpen(true)}
            focusedProduct={focusedProductForMap}
          />
        ) : currentView === 'sectors' ? (
          /* ========================================================
             3. SECTORS PAGE VIEW (Dedicated list of departments)
             ======================================================== */
          <SectorsPage
            secoes={secoes}
            totalProductsCount={totalProductsCount}
            onSelectSector={handleSelectSecao}
            onBackToHome={handleBackToHome}
            isLoading={isLoadingSecoes}
          />
        ) : currentView === 'product-detail' ? (
          /* ========================================================
             4. DEDICATED PRODUCT DETAIL PAGE (Full page with specs & map)
             ======================================================== */
          <ProductDetailPage
            product={selectedProductForDetail}
            onBack={handleBackFromProductDetail}
            onAddToCart={handleAddToCart}
            onNavigateToProduct={handleNavigateToProduct}
            onViewOnMap={(product) => handleOpenMap(product)}
          />
        ) : (
          /* ========================================================
             5. SEARCH & CATALOG RESULTS VIEW (Dedicated results page)
             ======================================================== */
          <CatalogSearchPage
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            onBackToHome={handleBackFromSearch}
            secoes={secoes}
            selectedSecao={selectedSecao}
            onSelectSecao={handleSelectSecao}
            totalProductsCount={totalProductsCount}
            produtos={produtos}
            isLoadingProdutos={isLoadingProdutos}
            apenasDisponiveis={apenasDisponiveis}
            setApenasDisponiveis={setApenasDisponiveis}
            activeFiltersCount={activeFiltersCount}
            onOpenFacetModal={() => setIsFacetModalOpen(true)}
            selectedAtributos={selectedAtributos}
            facetas={facetas}
            onRemoveAtributo={handleRemoveAtributo}
            onClearAllFilters={handleClearAllFilters}
            onOpenSectorsDrawer={() => setIsSectorsDrawerOpen(true)}
            onAddToCart={handleAddToCart}
            onNavigateToProduct={handleNavigateToProduct}
            onViewProductDetails={handleOpenProductDetailPage}
            autoFocusSearch={autoFocusSearch}
          />
        )}

      </main>

      {/* Bottom Navigation for Mobile (hidden on dedicated product detail page) */}
      {currentView !== 'product-detail' && (
        <BottomNav
          activeTab={activeTab}
          setActiveTab={handleTabChange}
        />
      )}

      {/* Floating AI Assistant Chat Action Button (available in all pages) */}
      <FloatingAIChatButton
        isProductPage={currentView === 'product-detail'}
        onClick={() => setIsAIChatOpen(true)}
      />

      {/* AI Assistant Chat Modal / Drawer (Passo 7 / UC-007 a UC-009) */}
      <AIChatModal
        isOpen={isAIChatOpen}
        onClose={() => setIsAIChatOpen(false)}
        sessionId={session?.id}
        catalogProducts={produtos}
        screenContext={{
          view: currentView,
          product: selectedProductForDetail,
          selectedSecao: selectedSecao,
          currentLocation: currentLocation
        }}
        onAddToCart={handleAddToCart}
        onViewProductDetails={handleOpenProductDetailPage}
      />

      {/* Dynamic Facet Filters Modal (Passo 5 do Fluxo / UC-002) */}
      <FacetFiltersModal
        isOpen={isFacetModalOpen}
        onClose={() => setIsFacetModalOpen(false)}
        facetas={facetas}
        selectedAtributos={selectedAtributos}
        onToggleAtributo={handleToggleAtributo}
        onClearAtributos={handleClearAllFilters}
        apenasDisponiveis={apenasDisponiveis}
        onToggleDisponiveis={setApenasDisponiveis}
        totalResultsCount={produtos.length}
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
