import React, { useState, useMemo } from 'react'
import Header from './components/Header'
import LocationStatus from './components/LocationStatus'
import SearchBar from './components/SearchBar'
import BentoActions from './components/BentoActions'
import ProductCard from './components/ProductCard'
import PromoBanner from './components/PromoBanner'
import RouteModal from './components/RouteModal'
import SplashScreen from './components/SplashScreen'
import './App.css'

const INITIAL_PRODUCTS = [
  {
    id: 1,
    name: 'Lâmpada LED Bulbo 9W',
    specs: 'LED, 9W, 6500K (Luz Branca Fria), Bivolt',
    category: 'iluminacao',
    corredor: 'Corredor A12',
    stock: 12,
    price: 14.90,
    icon: 'lightbulb',
    tag: 'Mais Vendido'
  },
  {
    id: 2,
    name: 'Selante de Silicone Acético',
    specs: 'Incolor, 280g, Anti-mofo e Fungicida',
    category: 'construcao',
    corredor: 'Corredor B04',
    stock: 8,
    price: 29.90,
    icon: 'hardware',
    tag: 'Destaque'
  },
  {
    id: 3,
    name: 'Rolo de Pintura Antigota 23cm',
    specs: 'Lã Sintética Microfibra, 23cm com cabo',
    category: 'pintura',
    corredor: 'Corredor C02',
    stock: 15,
    price: 22.50,
    icon: 'imagesearch_roller',
    tag: 'Oferta'
  },
  {
    id: 4,
    name: 'Fita LED Smart RGB 5 Metros',
    specs: 'Wi-Fi, Compatível com Alexa/Google, 16M cores',
    category: 'iluminacao',
    corredor: 'Corredor A12',
    stock: 6,
    price: 89.90,
    icon: 'flare',
    tag: 'Smart Home'
  },
  {
    id: 5,
    name: 'Disjuntor Bipolar Din 32A',
    specs: 'Curva C, 3kA 230/400V, Proteção Elétrica',
    category: 'eletrica',
    corredor: 'Corredor A14',
    stock: 20,
    price: 34.90,
    icon: 'electrical_services',
    tag: 'Segurança'
  },
  {
    id: 6,
    name: 'Plafon LED Sobrepor Quadrado 24W',
    specs: 'Luz Neutra 4000K, Alumínio Branco, 30x30cm',
    category: 'iluminacao',
    corredor: 'Corredor A13',
    stock: 9,
    price: 49.90,
    icon: 'highlight',
    tag: 'Recomendado'
  }
]

const SEARCH_SUGGESTIONS = [
  'Lâmpada LED',
  'Fita LED Smart',
  'Plafon sobrepor',
  'Silicone anti-mofo',
  'Disjuntor 32A'
]

const CATEGORIES = [
  { id: 'todos', label: 'Todos os Produtos' },
  { id: 'iluminacao', label: 'Iluminação (A12/A13)' },
  { id: 'eletrica', label: 'Elétrica (A14)' },
  { id: 'pintura', label: 'Pintura (C02)' },
  { id: 'construcao', label: 'Construção (B04)' }
]

function App() {
  const [activeTab, setActiveTab] = useState('home')
  const [searchQuery, setSearchQuery] = useState('')
  const [selectedCategory, setSelectedCategory] = useState('todos')
  const [cartCount, setCartCount] = useState(0)
  const [toastMessage, setToastMessage] = useState(null)

  // Modals state
  const [modalConfig, setModalConfig] = useState({
    isOpen: false,
    title: '',
    type: '',
    data: null
  })

  // Location state
  const [currentLocation] = useState({
    sector: 'Setor de Iluminação',
    aisle: 'Corredor A12'
  })

  const showToast = (msg) => {
    setToastMessage(msg)
    setTimeout(() => {
      setToastMessage(null)
    }, 3000)
  }

  const handleAddToCart = (product) => {
    setCartCount(prev => prev + 1)
    showToast(`"${product.name}" adicionado à lista! (${product.corredor})`)
  }

  const handleNavigateToProduct = (product) => {
    setModalConfig({
      isOpen: true,
      title: `Rota até: ${product.name}`,
      type: 'route',
      data: product
    })
  }

  const handleOpenMap = () => {
    setModalConfig({
      isOpen: true,
      title: 'Mapa da Loja & Corredores',
      type: 'map',
      data: null
    })
  }

  const handleCallSpecialist = () => {
    setModalConfig({
      isOpen: true,
      title: 'Solicitação de Atendimento',
      type: 'specialist',
      data: null
    })
  }

  const filteredProducts = useMemo(() => {
    return INITIAL_PRODUCTS.filter(item => {
      const matchesSearch = searchQuery.trim() === '' || 
        item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.specs.toLowerCase().includes(searchQuery.toLowerCase()) ||
        item.corredor.toLowerCase().includes(searchQuery.toLowerCase())

      const matchesCategory = selectedCategory === 'todos' || item.category === selectedCategory

      return matchesSearch && matchesCategory
    })
  }, [searchQuery, selectedCategory])

  return (
    <div className="app-root">
      {/* Intro Split Splash Screen */}
      <SplashScreen />

      {/* Header / TopAppBar */}
      <Header
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onMenuClick={() => showToast('Menu de categorias aberto.')}
        onProfileClick={() => showToast('Perfil do cliente Leroy Merlin')}
      />

      {/* Main Canvas */}
      <main className="main-content">
        {/* Welcome Section */}
        <section className="welcome-section">
          <h1 className="welcome-title">Bem-vindo à loja!</h1>
          <p className="welcome-subtitle">
            Como podemos ajudar no seu projeto hoje? Encontre produtos, navegue pelo mapa inteligente ou fale com um de nossos especialistas no corredor.
          </p>
        </section>

        {/* Location Status Chip */}
        <LocationStatus
          location={currentLocation}
          onViewMap={handleOpenMap}
        />

        {/* AI Smart Search */}
        <SearchBar
          searchQuery={searchQuery}
          setSearchQuery={setSearchQuery}
          suggestions={SEARCH_SUGGESTIONS}
          onSearch={(query) => {
            if (query) showToast(`Buscando com IA por: "${query}"`)
          }}
        />

        {/* Bento Grid Actions */}
        <BentoActions
          currentSectorName="Iluminação"
          onViewProducts={() => setSelectedCategory('iluminacao')}
          onCallSpecialist={handleCallSpecialist}
          onViewMap={handleOpenMap}
        />

        {/* Sector Recommended Products */}
        <section className="products-section">
          <div className="section-header-wrap">
            <h2 className="section-heading">Produtos Recomendados</h2>
            <div className="filter-category-tabs">
              {CATEGORIES.map(cat => (
                <button
                  key={cat.id}
                  className={`filter-tab ${selectedCategory === cat.id ? 'active' : ''}`}
                  onClick={() => setSelectedCategory(cat.id)}
                >
                  {cat.label}
                </button>
              ))}
            </div>
          </div>

          {filteredProducts.length === 0 ? (
            <div style={{ padding: '40px 20px', textAlign: 'center', color: 'var(--text-muted)' }}>
              <span className="material-symbols-outlined" style={{ fontSize: '48px', marginBottom: '10px' }}>search_off</span>
              <p>Nenhum produto encontrado para o termo ou filtro selecionado.</p>
            </div>
          ) : (
            <div className="products-grid">
              {filteredProducts.map(prod => (
                <ProductCard
                  key={prod.id}
                  product={prod}
                  onAddToCart={handleAddToCart}
                  onNavigateToProduct={handleNavigateToProduct}
                />
              ))}
            </div>
          )}
        </section>

        {/* Featured Promotional Banner */}
        <PromoBanner
          onExplore={() => {
            setSelectedCategory('iluminacao')
            showToast('Exibindo novidades do setor de Iluminação inteligente!')
          }}
        />
      </main>

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
