import { useState, useMemo, useRef, useEffect } from 'react'
import { formatPrice } from '../utils/format'
import {
  STORE_SECTORS,
  STORE_AMENITIES,
  STORE_QR_POINTS,
  STORE_OUTLINE,
  CANVAS,
  findSectorForProduct
} from '../services/mapService'

/*
 * Onde o cliente aparece quando ainda nao escaneou nada.
 *
 * Vem da placa ENT-01, que e a entrada tracada na planta - nao de um par de numeros
 * escrito a mao. O desenho anterior tinha 195,550 cravado em dois lugares, e ao
 * trocar a geometria esses dois pontos passaram a cair no patio de materiais: o
 * pino aparecia longe da porta e ninguem seria avisado.
 */
const ENTRADA = STORE_QR_POINTS.find(q => q.codigo === 'ENT-01')

export default function StoreMapPage({
  roteiroItems = [],
  currentLocation,
  onUpdateLocation,
  onToggleCollectItem,
  onViewProductDetails,
  onOpenRoteiro,
  onOpenLocationModal,
  onEncerrarJornada,
  focusedProduct = null
}) {

  /*
   * ZOOM E DESLOCAMENTO VIVEM EM DOIS SISTEMAS DE COORDENADAS, e confundi-los era a raiz de
   * três defeitos que a demonstração de 09/09 revelou.
   *
   * O SVG desenha em UNIDADES DO VIEWBOX (0–950 × 0–616). O `translate()` do CSS desloca em
   * PIXELS DE TELA. Entre um e outro existe a escala com que o SVG se ajusta ao palco — cerca
   * de 0,41 num celular. Contas feitas em unidades do viewBox e aplicadas como pixels erravam
   * por um fator de 2,4:
   *
   * - o botão de localização jogava a planta para fora da tela;
   * - o chip de setor deslocava para o lugar errado;
   * - o `transform-origin` era `475px 308px`, um ponto FORA de um elemento de 390 × 512, então
   *   o zoom pivotava por um canto imaginário.
   *
   * Agora existe um só caminho: `centralizarEm`, que converte de viewBox para pixels.
   */
  const palcoRef = useRef(null)
  const [palco, setPalco] = useState({ largura: 0, altura: 0 })

  const [zoomLevel, setZoomLevel] = useState(1)
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 })
  const [isDragging, setIsDragging] = useState(false)
  /* Verdadeiro enquanto o dedo ou o mouse estao sobre o mapa. Desliga a transicao. */
  const [emGesto, setEmGesto] = useState(false)
  const [dragStart, setDragStart] = useState({ x: 0, y: 0 })
  const [selectedSector, setSelectedSector] = useState(null)
  const [selectedPinItem, setSelectedPinItem] = useState(null)
  const [showRoute, setShowRoute] = useState(true)
  const [showAmenities, setShowAmenities] = useState(true)
  const [searchSectorTerm, setSearchSectorTerm] = useState('')
  const svgRef = useRef(null)

  //
  // Item cuja secao nao casa com bloco nenhum NAO ganha alfinete. Antes caia em
  // STORE_SECTORS[0] - Pintura -, e o mapa afirmava um corredor que nao era: quem pedia rota
  // para um sifao era mandado ao corredor de tintas. Nao desenhar e pior que desenhar certo, e
  // muito melhor que desenhar errado com confianca.
  const mappedRoteiroPins = useMemo(() => {
    return roteiroItems.map((item, idx) => {
      const sector = findSectorForProduct(item)

      if (!sector) {
        console.warn('Sem bloco no mapa para "%s" (secao %s): o item fica sem alfinete.',
          item.nome, item.corredor || item.secao || '?')
        return null
      }

      // Espalha os pinos dentro do setor: sem isso, dois itens da mesma seção ficam um
      // exatamente em cima do outro e o cliente só vê um.
      const offsetX = (idx % 3 - 1) * 16
      const offsetY = (Math.floor(idx / 3) % 3 - 1) * 14
      return {
        ...item,
        sector,
        pinX: sector.rotuloX + offsetX,
        pinY: sector.rotuloY + offsetY,
        orderIndex: idx + 1
      }
    }).filter(Boolean)
  }, [roteiroItems])

  const userPosition = useMemo(() => {
    if (!currentLocation) return { x: ENTRADA.x, y: ENTRADA.y, name: ENTRADA.nome }
    
    const qrMatch = STORE_QR_POINTS.find(
      q => q.codigo === currentLocation.code || currentLocation.code?.includes(q.codigo)
    )
    if (qrMatch) {
      return { x: qrMatch.x, y: qrMatch.y, name: qrMatch.nome }
    }

    const sectorMatch = STORE_SECTORS.find(
      s => s.nome.toLowerCase() === (currentLocation.sector || '').toLowerCase() ||
           (currentLocation.aisle || '').toLowerCase().includes(s.nome.toLowerCase())
    )
    if (sectorMatch) {
      return { x: sectorMatch.rotuloX, y: sectorMatch.rotuloY, name: sectorMatch.nome }
    }

    return { x: ENTRADA.x, y: ENTRADA.y, name: currentLocation.aisle || ENTRADA.nome }
  }, [currentLocation])

  useEffect(() => {
    if (focusedProduct) {
      const foundPin = mappedRoteiroPins.find(p => p.id === focusedProduct.id || p.produtoId === focusedProduct.id)
      if (foundPin) {
        setSelectedPinItem(foundPin)
        setSelectedSector(foundPin.sector)
      } else {
        const sector = findSectorForProduct(focusedProduct)
        if (sector) {
          setSelectedSector(sector)
        }
      }
    }
  }, [focusedProduct, mappedRoteiroPins])

  const routePoints = useMemo(() => {
    if (!showRoute || mappedRoteiroPins.length === 0) return []

    const points = [{ x: userPosition.x, y: userPosition.y, label: 'Início' }]

    const pendingPins = mappedRoteiroPins.filter(p => !p.coletado)
    pendingPins.forEach(p => {
      points.push({ x: p.pinX, y: p.pinY, label: p.nome })
    })

    // Termina nos caixas. Se a planta nao trouxer a frente de caixas, a rota
    // simplesmente nao ganha esse trecho - antes havia um 260,575 de reserva, que
    // com a geometria nova apontaria para dentro do patio de materiais.
    const caixas = STORE_AMENITIES.find(a => a.tipo === 'CAIXA')
    if (caixas) points.push({ x: caixas.x, y: caixas.y, label: 'Caixas' })

    return points
  }, [showRoute, mappedRoteiroPins, userPosition])

  const routePolylineString = useMemo(() => {
    return routePoints.map(p => `${p.x},${p.y}`).join(' ')
  }, [routePoints])

  /* Quantos pixels de tela vale uma unidade do viewBox, com o SVG ajustado ao palco. */
  const escalaDeAjuste = useMemo(() => {
    if (!palco.largura || !palco.altura) return 0
    return Math.min(palco.largura / CANVAS.largura, palco.altura / CANVAS.altura)
  }, [palco])

  /*
   * O zoom que faz a planta PREENCHER o palco em vez de caber nele.
   *
   * A planta é deitada (1,54:1) e a tela do celular é em pé: cabendo inteira, ela usava 49%
   * da área e as gôndolas decalcadas viravam traços de 3 px. Preenchendo, o desenho dobra de
   * tamanho e o cliente arrasta para ver as pontas — que é como se lê mapa no celular.
   *
   * É sempre ≥ 1, então nunca encolhe a planta abaixo do ajuste.
   */
  const zoomDePreenchimento = useMemo(() => {
    if (!palco.largura || !palco.altura) return 1
    const ajuste = Math.min(palco.largura / CANVAS.largura, palco.altura / CANVAS.altura)
    const preenchimento = Math.max(palco.largura / CANVAS.largura, palco.altura / CANVAS.altura)
    return preenchimento / ajuste
  }, [palco])

  useEffect(() => {
    const alvo = palcoRef.current
    if (!alvo) return

    const medir = () => {
      const r = alvo.getBoundingClientRect()
      setPalco({ largura: r.width, altura: r.height })
    }
    medir()

    /*
     * A referência precisa ficar numa variável. Um `new ResizeObserver(...).observe(...)` sem
     * nome já foi coletado pelo navegador antes de disparar uma vez — defeito que este projeto
     * já teve na bancada da planta.
     */
    const observador = new ResizeObserver(medir)
    observador.observe(alvo)
    return () => observador.disconnect()
  }, [])

  /* O zoom inicial acompanha o palco enquanto ninguém tiver mexido nos controles. */
  const zoomTocadoPeloCliente = useRef(false)
  useEffect(() => {
    if (!zoomTocadoPeloCliente.current) setZoomLevel(zoomDePreenchimento)
  }, [zoomDePreenchimento])

  /** Traz um ponto do viewBox para o centro do palco, convertendo para pixels de tela. */
  const centralizarEm = (x, y, zoom = zoomLevel) => {
    setPanOffset({
      x: -(x - CANVAS.largura / 2) * escalaDeAjuste * zoom,
      y: -(y - CANVAS.altura / 2) * escalaDeAjuste * zoom
    })
  }

  // Zoom handlers
  const aoMexerNoZoom = () => { zoomTocadoPeloCliente.current = true }
  /*
   * O teto subiu de 2,5 para 4: com o padrão em ~2 num celular, 2,5 dava meio passo de folga.
   * O piso de 0,75 passou a significar "ver a loja inteira com margem", já que 1 é o ajuste.
   */
  const handleZoomIn = () => { aoMexerNoZoom(); setZoomLevel(prev => Math.min(prev + 0.25, 4)) }
  const handleZoomOut = () => { aoMexerNoZoom(); setZoomLevel(prev => Math.max(prev - 0.25, 0.75)) }
  const handleResetZoom = () => {
    zoomTocadoPeloCliente.current = false
    setZoomLevel(zoomDePreenchimento)
    setPanOffset({ x: 0, y: 0 })
    setSelectedSector(null)
    setSelectedPinItem(null)
  }

  // Pan dragging handlers
  const handleMouseDown = (e) => {
    setIsDragging(true)
    setEmGesto(true)
    setDragStart({ x: e.clientX - panOffset.x, y: e.clientY - panOffset.y })
  }

  const handleMouseMove = (e) => {
    if (!isDragging) return
    setPanOffset({
      x: e.clientX - dragStart.x,
      y: e.clientY - dragStart.y
    })
  }

  const handleMouseUp = () => { setIsDragging(false); setEmGesto(false) }

  // Touch handlers for mobile
  /*
   * PINÇA DE DOIS DEDOS.
   *
   * Os manipuladores só olhavam `touches.length === 1`, então dois dedos não faziam nada — e
   * pinçar para aproximar é o primeiro gesto que qualquer pessoa tenta num mapa de celular.
   *
   * O estado do gesto mora num ref, e não em `useState`, porque ele é lido e escrito a cada
   * `touchmove`: guardá-lo em estado dispararia um render só para anotar a distância entre os
   * dedos, sem nada novo para desenhar.
   */
  const pincaRef = useRef(null)

  const distanciaEntre = (a, b) => Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY)
  const meioEntre = (a, b) => ({ x: (a.clientX + b.clientX) / 2, y: (a.clientY + b.clientY) / 2 })

  const handleTouchStart = (e) => {
    if (e.touches.length === 2) {
      setIsDragging(false)
      zoomTocadoPeloCliente.current = true
      pincaRef.current = {
        distancia: distanciaEntre(e.touches[0], e.touches[1]),
        zoom: zoomLevel,
        pan: { ...panOffset },
        meio: meioEntre(e.touches[0], e.touches[1])
      }
      setEmGesto(true)
      return
    }
    if (e.touches.length === 1) {
      pincaRef.current = null
      setIsDragging(true)
      setEmGesto(true)
      setDragStart({
        x: e.touches[0].clientX - panOffset.x,
        y: e.touches[0].clientY - panOffset.y
      })
    }
  }

  const handleTouchMove = (e) => {
    const pinca = pincaRef.current
    if (e.touches.length === 2 && pinca && pinca.distancia > 0) {
      const proporcao = distanciaEntre(e.touches[0], e.touches[1]) / pinca.distancia
      const novoZoom = Math.min(4, Math.max(0.75, pinca.zoom * proporcao))

      /*
       * O ponto entre os dedos fica parado enquanto o zoom muda — sem isto, aproximar
       * empurraria a planta para um canto e o cliente perderia o que estava olhando.
       */
      const palcoRect = palcoRef.current?.getBoundingClientRect()
      if (palcoRect) {
        const k = novoZoom / pinca.zoom
        const centroX = palcoRect.x + palcoRect.width / 2
        const centroY = palcoRect.y + palcoRect.height / 2
        setPanOffset({
          x: pinca.pan.x + (pinca.meio.x - centroX - pinca.pan.x) * (1 - k),
          y: pinca.pan.y + (pinca.meio.y - centroY - pinca.pan.y) * (1 - k)
        })
      }
      setZoomLevel(novoZoom)
      return
    }

    if (!isDragging || e.touches.length !== 1) return
    setPanOffset({
      x: e.touches[0].clientX - dragStart.x,
      y: e.touches[0].clientY - dragStart.y
    })
  }

  const handleTouchEnd = (e) => {
    if (e.touches.length === 1) {
      /*
       * Saiu de dois dedos para um. Sem reancorar aqui, o arraste continuaria com o
       * `dragStart` de antes da pinça e a planta daria um salto.
       */
      pincaRef.current = null
      setIsDragging(true)
      setDragStart({
        x: e.touches[0].clientX - panOffset.x,
        y: e.touches[0].clientY - panOffset.y
      })
      return
    }
    pincaRef.current = null
    setIsDragging(false)
    setEmGesto(false)
  }

  // Filtered sectors for search chip bar
  const visibleSectors = useMemo(() => {
    if (!searchSectorTerm) return STORE_SECTORS
    const term = searchSectorTerm.toLowerCase()
    return STORE_SECTORS.filter(s =>
      s.nome.toLowerCase().includes(term) ||
      s.descricao.toLowerCase().includes(term)
    )
  }, [searchSectorTerm])

  // Stats
  const totalItems = roteiroItems.length
  const collectedCount = roteiroItems.filter(i => i.coletado).length
  const nextPendingPin = mappedRoteiroPins.find(p => !p.coletado)

  return (
    <div className="store-map-page-container">
      {/* Top Floating Control Bar */}
      <div className="store-map-header">
        {/*
          * O título saiu daqui em 09/09.
          *
          * Ele ocupava 143 px dos 800 de um celular — e renderizava a 30 px porque a regra
          * dizia `.map-title-info h2` e a marcação era `h1`, então caía no tamanho de herói
          * global. Numa tela onde o mapa é o assunto, o nome da tela é a informação menos
          * útil: o item aceso na navegação de baixo já diz "Mapa".
          *
          * O nome da loja não se perdeu — virou etiqueta sobre a própria planta, que é onde
          * um mapa costuma se identificar, e ali não custa altura nenhuma.
          */}
        <div className="map-title-row">
          <div
            className="map-user-beacon-pill"
            onClick={onOpenLocationModal}
            onKeyDown={(evento) => {
              if (evento.key === 'Enter' || evento.key === ' ') {
                evento.preventDefault()
                onOpenLocationModal()
              }
            }}
            role="button"
            tabIndex={0}
            title="Alterar ponto de localização"
          >
            <span className="beacon-dot"></span>
            <div className="beacon-text">
              <span className="beacon-label">Você está em:</span>
              <strong className="beacon-value">{currentLocation?.aisle || 'Entrada da Loja'}</strong>
            </div>
            <span className="material-symbols-outlined beacon-edit-icon" aria-hidden="true">qr_code_scanner</span>
          </div>
        </div>

        {/* Action Toggles and Filters */}
        <div className="map-actions-bar">
          <div className="map-toggles-group">
            <button
              type="button"
              className={`map-toggle-btn ${showRoute ? 'active' : ''}`}
              onClick={() => setShowRoute(!showRoute)}
            >
              <span className="material-symbols-outlined" aria-hidden="true">directions_walk</span>
              <span>{showRoute ? 'Rota Ativa' : 'Ocultar Rota'}</span>
            </button>

            <button
              type="button"
              className={`map-toggle-btn ${showAmenities ? 'active' : ''}`}
              onClick={() => setShowAmenities(!showAmenities)}
            >
              <span className="material-symbols-outlined" aria-hidden="true">storefront</span>
              <span>Serviços & Caixas</span>
            </button>
          </div>

          <div className="map-search-sector-input-wrap">
            <span className="material-symbols-outlined" aria-hidden="true">search</span>
            <input
              type="text"
              placeholder="Buscar setor no mapa..."
              value={searchSectorTerm}
              onChange={(e) => setSearchSectorTerm(e.target.value)}
              className="map-sector-search-input"
            />
            {searchSectorTerm && (
              <button
                type="button"
                className="clear-search-btn"
                onClick={() => setSearchSectorTerm('')}
              >
                <span className="material-symbols-outlined" aria-hidden="true">close</span>
              </button>
            )}
          </div>
        </div>

        {/* Sector Quick Chips */}
        <div className="map-sector-chips-carousel">
          <button
            type="button"
            className={`map-sector-chip ${!selectedSector ? 'active' : ''}`}
            onClick={() => { setSelectedSector(null); setSelectedPinItem(null); }}
          >
            Visão Geral
          </button>
          {visibleSectors.map(sec => (
            <button
              key={sec.id}
              type="button"
              className={`map-sector-chip ${selectedSector?.id === sec.id ? 'active' : ''}`}
              onClick={() => {
                setSelectedSector(sec)
                centralizarEm(sec.rotuloX, sec.rotuloY)
              }}
            >
              <span className="material-symbols-outlined chip-icon" style={{ color: sec.color }} aria-hidden="true">
                {sec.icon}
              </span>
              <span>{sec.nome}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Interactive SVG Canvas Area */}
      <div
        ref={palcoRef}
        className="store-map-canvas-viewport"
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        {/* A loja se identifica sobre a própria planta, como num mapa impresso. */}
        <span className="map-etiqueta-da-loja">Leroy Merlin Interlagos</span>

        {/* Floating Zoom & Pan Controls */}
        <div className="map-floating-controls">
          <button
            type="button"
            className="map-control-btn"
            onClick={handleZoomIn}
            title="Aumentar zoom"
            aria-label="Aumentar zoom"
          >
            <span className="material-symbols-outlined" aria-hidden="true">add</span>
          </button>
          <button
            type="button"
            className="map-control-btn"
            onClick={handleZoomOut}
            title="Diminuir zoom"
            aria-label="Diminuir zoom"
          >
            <span className="material-symbols-outlined" aria-hidden="true">remove</span>
          </button>
          <button
            type="button"
            className="map-control-btn"
            onClick={handleResetZoom}
            title="Reiniciar a visão do mapa"
            aria-label="Reiniciar a visão: desfaz zoom, arraste e seleção"
          >
            <span className="material-symbols-outlined" aria-hidden="true">restart_alt</span>
          </button>
          <button
            type="button"
            className="map-control-btn highlight-user-btn"
            onClick={() => centralizarEm(userPosition.x, userPosition.y)}
            title="Centralizar em mim"
            aria-label="Centralizar o mapa na minha posição"
          >
            <span className="material-symbols-outlined filled" aria-hidden="true">my_location</span>
          </button>
        </div>

        {/* SVG Drawing Canvas */}
        <svg
          ref={svgRef}
          viewBox={`0 0 ${CANVAS.largura} ${CANVAS.altura}`}
          className="store-map-svg"
          style={{
            transform: `translate(${panOffset.x}px, ${panOffset.y}px) scale(${zoomLevel})`,
            /*
             * NENHUMA transicao enquanto o dedo esta na tela.
             *
             * A folha declarava `transition: transform 0.1s ease-out` sempre, entao cada
             * atualizacao do arraste virava uma animacao de 100 ms: a planta perseguia o dedo
             * com atraso e nunca o alcancava. Era o que parecia travamento.
             *
             * Fora do gesto ela fica, porque e ela que suaviza o zoom pelos botoes e o
             * "centralizar em mim".
             */
            transition: emGesto ? 'none' : 'transform 0.12s ease-out',
            /* O centro do elemento, e nao `475px 308px`: aquilo era um ponto do viewBox
               aplicado a uma caixa de CSS de 390 x 512, ou seja, fora do proprio elemento. */
            transformOrigin: 'center'
          }}
        >
          <defs>
            {/* Grid Pattern */}
            <pattern id="store-grid-pattern" width="40" height="40" patternUnits="userSpaceOnUse">
              <path d="M 40 0 L 0 0 0 40" fill="none" stroke="#E2E8F0" strokeWidth="0.75" />
            </pattern>

            {/* Glowing route line filter */}
            <filter id="route-glow" x="-20%" y="-20%" width="140%" height="140%">
              <feGaussianBlur stdDeviation="3" result="blur" />
              <feComposite in="SourceGraphic" in2="blur" operator="over" />
            </filter>

            {/* Pulsing Beacon Animation */}
            <radialGradient id="user-pulse-gradient" cx="50%" cy="50%" r="50%">
              <stop offset="0%" stopColor="#2563EB" stopOpacity="0.8" />
              <stop offset="100%" stopColor="#2563EB" stopOpacity="0" />
            </radialGradient>
          </defs>

          {/* Background & Surroundings */}
          <rect width={CANVAS.largura} height={CANVAS.altura} fill="#F8FAFC" />
          <rect width={CANVAS.largura} height={CANVAS.altura} fill="url(#store-grid-pattern)" />


          {/* O predio: galpao fechado e patio coberto, que sao dois corpos e nao
              um. O patio vai tracejado porque e coberto e aberto nas laterais. */}
          {STORE_OUTLINE.map(corpo => (
            <polygon
              key={corpo.nome}
              points={corpo.pontos}
              fill={corpo.patio ? '#FDFDFB' : '#FFFFFF'}
              stroke="#94A3B8"
              strokeWidth={corpo.patio ? '2.5' : '3.5'}
              strokeDasharray={corpo.patio ? '9 6' : undefined}
              strokeLinejoin="round"
              className="store-outer-hull"
            />
          ))}

          {/* Corridors and Aisles */}
          <g className="store-sectors-group">
            {STORE_SECTORS.map((sec) => {
              const isSelected = selectedSector?.id === sec.id
              const hasItems = mappedRoteiroPins.some(p => p.sector.id === sec.id)

              return (
                <g
                  key={sec.id}
                  className={`store-sector-block ${isSelected ? 'selected' : ''} ${hasItems ? 'has-items' : ''}`}
                  onClick={() => {
                    setSelectedSector(sec)
                    const itemInSector = mappedRoteiroPins.find(p => p.sector.id === sec.id)
                    setSelectedPinItem(itemInSector || null)
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  {/* A secao, com a forma que ela tem na planta: 4, 6 ou 8 lados */}
                  <polygon
                    points={sec.pontos}
                    fill={isSelected ? `${sec.color}25` : hasItems ? '#FEF3C7' : '#F1F5F9'}
                    stroke={isSelected ? sec.color : hasItems ? '#F59E0B' : '#CBD5E1'}
                    strokeWidth={isSelected ? '2.5' : hasItems ? '2' : '1.2'}
                    strokeLinejoin="round"
                    className="sector-rect"
                  />

                  {/* As gondolas: o que faz o desenho parecer loja em vez de caixa
                      vazia. Vem tracadas uma a uma da planta, por isso variam de
                      espessura e de arranjo entre uma secao e outra. Nao recebem
                      evento -- quem escuta o clique e o grupo da secao. */}
                  <g className="sector-shelves" pointerEvents="none">
                    {sec.gondolas.map((pontos, i) => (
                      <polygon
                        key={i}
                        points={pontos}
                        fill={sec.color}
                        fillOpacity={isSelected ? '0.42' : '0.26'}
                      />
                    ))}
                  </g>

                  {/* Sector Title & Corredor text */}
                  {/* Tarjeta atras do rotulo: sem ela o texto briga com a hachura
                      das gondolas e nenhum dos dois se le. */}
                  <rect
                    x={sec.rotuloX - (sec.nome.length * 3.3 + 8)}
                    y={sec.rotuloY - 9}
                    width={sec.nome.length * 6.6 + 16}
                    height="18"
                    rx="9"
                    fill="#FFFFFF"
                    fillOpacity="0.9"
                    pointerEvents="none"
                  />
                  <text
                    x={sec.rotuloX}
                    y={sec.rotuloY + 4}
                    textAnchor="middle"
                    fill="#1E293B"
                    fontSize={sec.nome.length > 16 ? '9' : '11'}
                    fontWeight="700"
                    pointerEvents="none"
                    className="sector-title-text"
                  >
                    {sec.nome}
                  </text>

                  {/* Highlight Glow if items inside */}
                  {hasItems && (
                    <circle
                      cx={sec.x + sec.w - 10}
                      cy={sec.y + 12}
                      r="5"
                      fill="#F59E0B"
                    />
                  )}
                </g>
              )
            })}
          </g>

          {/* Store Amenities & Services (Caixas, Banheiros, Café, Entrada) */}
          {showAmenities && (
            <g className="store-amenities-group">
              {STORE_AMENITIES.map((amenity) => (
                <g
                  key={amenity.id}
                  className="amenity-marker"
                  transform={`translate(${amenity.x}, ${amenity.y})`}
                  style={{ cursor: 'pointer' }}
                >
                  <circle r="16" fill={amenity.color} fillOpacity="0.15" />
                  <circle r="12" fill={amenity.color} />
                  <text
                    x="0"
                    y="4"
                    textAnchor="middle"
                    fill="#FFFFFF"
                    fontSize="10"
                    fontWeight="bold"
                  >
                    {amenity.tipo === 'BANHEIRO' ? 'WC' : amenity.tipo === 'CAFE' ? '☕' : amenity.tipo === 'CAIXA' ? '$' : 'i'}
                  </text>
                  <text
                    x="0"
                    y="24"
                    textAnchor="middle"
                    fill="#334155"
                    fontSize="9"
                    fontWeight="600"
                  >
                    {amenity.nome.split(' ')[0]}
                  </text>
                </g>
              ))}

              {/* QR Code Scan Totems */}
              {STORE_QR_POINTS.map((qr) => (
                <g
                  key={qr.codigo}
                  className="qr-totem-marker"
                  transform={`translate(${qr.x}, ${qr.y})`}
                  onClick={() => onUpdateLocation && onUpdateLocation(qr.codigo)}
                  style={{ cursor: 'pointer' }}
                  title={`Totem QR Code: ${qr.codigo} (${qr.nome})`}
                >
                  <rect x="-8" y="-8" width="16" height="16" rx="3" fill="#0F172A" />
                  <text x="0" y="3" textAnchor="middle" fill="#38BDF8" fontSize="7" fontWeight="bold">
                    QR
                  </text>
                </g>
              ))}
            </g>
          )}

          {/* Dynamic Walking Route Polyline */}
          {showRoute && routePoints.length > 1 && (
            <g className="store-route-layer">
              {/* Glow backdrop */}
              <polyline
                points={routePolylineString}
                fill="none"
                stroke="#10B981"
                strokeWidth="6"
                strokeOpacity="0.4"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              {/* Animated Dashed Main Route */}
              <polyline
                points={routePolylineString}
                fill="none"
                stroke="#059669"
                strokeWidth="3.5"
                strokeDasharray="8 6"
                strokeLinecap="round"
                strokeLinejoin="round"
                className="animated-route-line"
              />

              {/* Route Waypoint Dots */}
              {routePoints.map((pt, idx) => (
                <circle
                  key={idx}
                  cx={pt.x}
                  cy={pt.y}
                  r="4"
                  fill="#059669"
                  stroke="#FFFFFF"
                  strokeWidth="1.5"
                />
              ))}
            </g>
          )}

          {/* Roteiro Shopping List Item Pins */}
          <g className="store-pins-layer">
            {mappedRoteiroPins.map((item) => {
              const isSelected = selectedPinItem?.id === item.id
              const isCollected = item.coletado

              return (
                <g
                  key={item.id}
                  className={`product-pin-marker ${isSelected ? 'selected' : ''} ${isCollected ? 'collected' : ''}`}
                  transform={`translate(${item.pinX}, ${item.pinY})`}
                  onClick={() => {
                    setSelectedPinItem(item)
                    setSelectedSector(item.sector)
                  }}
                  style={{ cursor: 'pointer' }}
                >
                  {/* Pin Circle */}
                  <circle
                    r={isSelected ? '16' : '13'}
                    fill={isCollected ? '#10B981' : '#F59E0B'}
                    stroke="#FFFFFF"
                    strokeWidth="2.5"
                    className="pin-bubble"
                  />

                  {/* Pin Icon / Number */}
                  <text
                    x="0"
                    y="4"
                    textAnchor="middle"
                    fill="#FFFFFF"
                    fontSize="10"
                    fontWeight="800"
                  >
                    {isCollected ? '✓' : item.orderIndex}
                  </text>

                  {/* Floating Mini Label */}
                  <text
                    x="0"
                    y="-16"
                    textAnchor="middle"
                    fill="#1E293B"
                    fontSize="9"
                    fontWeight="700"
                    className="pin-mini-label"
                  >
                    {item.nome?.length > 14 ? item.nome.substring(0, 12) + '...' : item.nome}
                  </text>
                </g>
              )
            })}
          </g>

          {/* Customer Live Position Beacon */}
          <g className="customer-beacon-layer" transform={`translate(${userPosition.x}, ${userPosition.y})`}>
            {/* Animated Radar Waves */}
            <circle r="26" fill="#3B82F6" fillOpacity="0.2" className="radar-wave" />
            <circle r="18" fill="#2563EB" fillOpacity="0.35" className="radar-wave-inner" />
            {/* Core Pin */}
            <circle r="8" fill="#2563EB" stroke="#FFFFFF" strokeWidth="2.5" />
            <circle r="3" fill="#FFFFFF" />

            {/* Label */}
            <g transform="translate(0, -18)">
              <rect x="-42" y="-12" width="84" height="18" rx="9" fill="#1E293B" />
              <text x="0" y="1" textAnchor="middle" fill="#FFFFFF" fontSize="8.5" fontWeight="700">
                📍 Você está aqui
              </text>
            </g>
          </g>
        </svg>
      </div>

      {/* Floating Bottom Card for Selected Pin / Next Route Stop */}
      <div className="store-map-bottom-drawer">
        {selectedPinItem ? (
          <div className="map-selected-product-card">
            <div className="selected-product-visual">
              {selectedPinItem.imagemUrl ? (
                <img src={selectedPinItem.imagemUrl} alt={selectedPinItem.nome} />
              ) : (
                <span className="material-symbols-outlined default-prod-icon" aria-hidden="true">inventory_2</span>
              )}
            </div>

            <div className="selected-product-details">
              <div className="product-loc-tag">
                <span className="material-symbols-outlined" aria-hidden="true">location_on</span>
                <span>{selectedPinItem.corredor}</span>
              </div>
              <h2 className="product-name">{selectedPinItem.nome}</h2>
              <span className="product-price">
                {formatPrice(selectedPinItem.preco)}
              </span>
            </div>

            <div className="selected-product-actions">
              <button
                type="button"
                className={`collect-toggle-btn ${selectedPinItem.coletado ? 'collected' : ''}`}
                onClick={() => {
                  if (onToggleCollectItem) onToggleCollectItem(selectedPinItem.id)
                }}
              >
                <span className="material-symbols-outlined" aria-hidden="true">
                  {selectedPinItem.coletado ? 'check_circle' : 'radio_button_unchecked'}
                </span>
                <span>{selectedPinItem.coletado ? 'Coletado' : 'Marcar Coleta'}</span>
              </button>

              <button
                type="button"
                className="view-details-btn"
                onClick={() => {
                  if (onViewProductDetails) onViewProductDetails(selectedPinItem)
                }}
              >
                <span className="material-symbols-outlined" aria-hidden="true">info</span>
                <span>Detalhes</span>
              </button>

              <button
                type="button"
                className="close-selection-btn"
                onClick={() => setSelectedPinItem(null)}
              >
                <span className="material-symbols-outlined" aria-hidden="true">close</span>
              </button>
            </div>
          </div>
        ) : (
          /* Route Summary Banner */
          <div className="map-route-summary-bar">
            <div className="route-summary-info">
              <div className="route-status-badge">
                <span className="material-symbols-outlined" aria-hidden="true">shopping_basket</span>
                <span>{collectedCount} de {totalItems} itens coletados</span>
              </div>
              {nextPendingPin ? (
                <p className="next-stop-text">
                  Próxima parada: <strong>{nextPendingPin.nome}</strong> no <em>{nextPendingPin.corredor}</em>
                </p>
              ) : totalItems > 0 ? (
                <p className="next-stop-text success">
                  🎉 Todos os itens coletados! Siga para os <strong>Caixas de Pagamento</strong>.
                </p>
              ) : (
                <p className="next-stop-text">
                  Sua lista está vazia. Adicione produtos e eles aparecem aqui no mapa, junto de onde você está.
                </p>
              )}
            </div>

            <div className="map-summary-actions-group">
              <button
                type="button"
                className="open-roteiro-action-btn secondary"
                onClick={onOpenRoteiro}
              >
                <span className="material-symbols-outlined" aria-hidden="true">format_list_bulleted</span>
                <span>Roteiro</span>
              </button>

              {totalItems > 0 && (
                <button
                  type="button"
                  className={`open-roteiro-action-btn ${collectedCount === totalItems ? 'success' : 'primary'}`}
                  onClick={() => {
                    if (onEncerrarJornada) onEncerrarJornada()
                  }}
                >
                  <span className="material-symbols-outlined" aria-hidden="true">
                    {collectedCount === totalItems ? 'task_alt' : 'point_of_sale'}
                  </span>
                  <span>{collectedCount === totalItems ? 'Ir para o Caixa' : 'Encerrar'}</span>
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>

  )
}
