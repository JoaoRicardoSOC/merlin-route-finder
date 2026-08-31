import React, { useEffect, useState } from 'react'
import { fetchProdutoDetalhe } from '../services/catalogService'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../constants/setores'
import { formatPrice } from '../utils/format'

export default function ProductDetailPage({
  product,
  onBack,
  onAddToCart,
  onNavigateToProduct,
  onViewOnMap
}) {
  const [detailedProduct, setDetailedProduct] = useState(product || {})
  const [isLoading, setIsLoading] = useState(false)
  const [addedAnimation, setAddedAnimation] = useState(false)

  useEffect(() => {
    window.scrollTo({ top: 0, behavior: 'instant' })
    if (product?.id) {
      setIsLoading(true)
      fetchProdutoDetalhe(product.id)
        .then(data => {
          if (data) setDetailedProduct(prev => ({ ...prev, ...data }))
        })
        .catch(err => console.warn('Erro ao carregar especificações do produto:', err))
        .finally(() => setIsLoading(false))
    }
  }, [product])

  if (!product) return null

  const name = detailedProduct.nome || detailedProduct.name || 'Produto'
  const specs = detailedProduct.descricao || detailedProduct.specs || ''
  const price = detailedProduct.preco ?? detailedProduct.price ?? 0
  const stock = detailedProduct.saldoEstoque ?? detailedProduct.stock ?? 0
  const secao = detailedProduct.secao || ''
  /*
   * Sem valor genérico de reserva: espaço em branco comunica ausência, "Corredor da Loja"
   * comunica presença — e mente. Onde não sabemos, a tela diz "não informado".
   */
  const corredor = detailedProduct.pontoMapa?.corredor || detailedProduct.corredor || (secao && SECTOR_METADATA[secao]?.corredor) || null
  const image = detailedProduct.imagemUrl || detailedProduct.image || null
  const sku = detailedProduct.sku || 'SKU-000'
  const atributos = detailedProduct.atributos || []

  const isOutOfStock = stock <= 0
  const meta = SECTOR_METADATA[secao] || DEFAULT_SECTOR_META

  const handleAddWithFeedback = () => {
    onAddToCart(detailedProduct)
    setAddedAnimation(true)
    setTimeout(() => setAddedAnimation(false), 1200)
  }

  return (
    <div className="product-page-view animate-fade-in">
      {/* Sticky Top Header Bar */}
      <div className="product-page-top-bar">
        <button
          type="button"
          className="search-back-btn"
          onClick={onBack}
          aria-label="Voltar para a página anterior"
          title="Voltar"
        >
          <span className="material-symbols-outlined">arrow_back</span>
        </button>

        <div className="product-page-title-wrap">
          <span className="product-page-crumb-section">{secao || 'Catálogo'}</span>
          <span className="product-page-crumb-sku">SKU: {sku}</span>
        </div>

        <button
          type="button"
          className="product-page-quick-cart-btn"
          onClick={handleAddWithFeedback}
          aria-label="Adicionar item ao roteiro"
          title="Adicionar ao Roteiro"
          disabled={isOutOfStock}
        >
          <span className="material-symbols-outlined filled">
            {addedAnimation ? 'check' : 'add_shopping_cart'}
          </span>
        </button>
      </div>

      {/* Hero Visual Container */}
      <div className="product-page-hero">
        <div className="product-page-image-card">
          {image ? (
            <img src={image} alt={name} className="product-page-img" />
          ) : (
            <div className="product-page-icon-fallback" style={{ color: meta.color, backgroundColor: meta.bgLight }}>
              <span className="material-symbols-outlined">{meta.icon || 'inventory_2'}</span>
            </div>
          )}
          {secao && <span className="product-page-sector-badge">{secao}</span>}
        </div>
      </div>

      {/* Main Info & Pricing Section */}
      <div className="product-page-info-section">
        <h1 className="product-page-title">{name}</h1>

        <div className="product-page-price-row">
          <div className="product-page-price-box">
            <span className="product-page-price-label">Preço nesta loja</span>
            <span className="product-page-price-value">{formatPrice(price)}</span>
          </div>

          <div className={`product-page-stock-pill ${isOutOfStock ? 'out-of-stock' : 'in-stock'}`}>
            <span className="material-symbols-outlined filled">
              {isOutOfStock ? 'cancel' : 'check_circle'}
            </span>
            <span>
              {isOutOfStock
                ? 'Esgotado nesta loja'
                : `${stock} un. disponíveis`}
            </span>
          </div>
        </div>

        {/* Physical Location Card with View on Map & Route */}
        <div className="product-page-location-card">
          <div className="product-page-loc-beacon">
            <span className="beacon-ping"></span>
            <span className="beacon-dot"></span>
          </div>
          <div className="product-page-loc-content">
            <span className="product-page-loc-label">Localização Física na Loja</span>
            <strong className="product-page-loc-value">{corredor || 'Não informada'}</strong>
          </div>
          <div className="product-page-loc-actions">
            <button
              type="button"
              className="product-page-map-action-btn"
              onClick={() => onViewOnMap(detailedProduct)}
              title="Ver localização na planta da loja"
            >
              <span className="material-symbols-outlined">map</span>
              <span>Ver no Mapa</span>
            </button>
          </div>
        </div>

        {/* Product Description */}
        {specs && (
          <div className="product-page-section-block">
            <h3 className="product-page-section-title">Descrição do Produto</h3>
            <p className="product-page-description-text">{specs}</p>
          </div>
        )}

        {/* Dynamic Specifications Table (GET /produtos/{id}) */}
        <div className="product-page-section-block">
          <h3 className="product-page-section-title">Especificações Técnicas</h3>
          {isLoading ? (
            <div className="product-page-specs-skeleton">
              <div className="skeleton-spec-line"></div>
              <div className="skeleton-spec-line"></div>
              <div className="skeleton-spec-line"></div>
            </div>
          ) : atributos && atributos.length > 0 ? (
            <div className="product-page-specs-table">
              {atributos.map((attr, index) => (
                <div key={index} className="product-spec-row">
                  <span className="product-spec-key">{attr.rotulo || attr.chave}</span>
                  <span className="product-spec-value">{attr.valor}</span>
                </div>
              ))}
            </div>
          ) : (
            <div className="product-page-specs-table">
              <div className="product-spec-row">
                <span className="product-spec-key">Seção / Departamento</span>
                <span className="product-spec-value">{secao || 'Geral'}</span>
              </div>
              <div className="product-spec-row">
                <span className="product-spec-key">Corredor de Armazenamento</span>
                <span className="product-spec-value">{corredor || 'Não informado'}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Floating Bottom Sticky Action Bar */}
      <div className="product-page-bottom-bar">
        <div className="product-page-bottom-price">
          <span className="bottom-price-label">Total unitário</span>
          <span className="bottom-price-val">{formatPrice(price)}</span>
        </div>

        <button
          type="button"
          className={`product-page-add-roteiro-btn ${isOutOfStock ? 'disabled' : ''} ${addedAnimation ? 'added-success' : ''}`}
          disabled={isOutOfStock}
          onClick={handleAddWithFeedback}
        >
          <span className="material-symbols-outlined filled">
            {addedAnimation ? 'done' : 'add_shopping_cart'}
          </span>
          <span>
            {addedAnimation 
              ? 'Adicionado!' 
              : isOutOfStock 
              ? 'Item Indisponível' 
              : 'Adicionar ao Roteiro'}
          </span>
        </button>
      </div>
    </div>
  )
}
