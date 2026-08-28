import React, { useEffect, useState } from 'react'
import { fetchProdutoDetalhe } from '../services/catalogService'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../constants/setores'
import { formatPrice } from '../utils/format'

export default function ProductDetailModal({
  isOpen,
  onClose,
  product,
  onAddToCart,
  onNavigateToProduct,
  onViewOnMap
}) {
  const [detailedProduct, setDetailedProduct] = useState(null)
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    if (isOpen && product) {
      setDetailedProduct(product)
      if (product.id) {
        setIsLoading(true)
        fetchProdutoDetalhe(product.id)
          .then(data => {
            if (data) setDetailedProduct(prev => ({ ...prev, ...data }))
          })
          .catch(err => console.warn('Erro ao carregar detalhes:', err))
          .finally(() => setIsLoading(false))
      }
    } else {
      setDetailedProduct(null)
    }
  }, [isOpen, product])

  if (!isOpen || !detailedProduct) return null

  const name = detailedProduct.nome || detailedProduct.name || 'Produto'
  const specs = detailedProduct.descricao || detailedProduct.specs || ''
  const price = detailedProduct.preco ?? detailedProduct.price ?? 0
  const stock = detailedProduct.saldoEstoque ?? detailedProduct.stock ?? 0
  const secao = detailedProduct.secao || ''
  const corredor = detailedProduct.pontoMapa?.corredor || detailedProduct.corredor || (secao && SECTOR_METADATA[secao]?.corredor) || 'Corredor da Loja'
  const image = detailedProduct.imagemUrl || detailedProduct.image || null
  const sku = detailedProduct.sku || 'SKU-000'
  const atributos = detailedProduct.atributos || []

  const isOutOfStock = stock <= 0
  const meta = SECTOR_METADATA[secao] || DEFAULT_SECTOR_META

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="product-detail-modal-container animate-fade-in"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label={`Detalhes de ${name}`}
      >
        {/* Header */}
        <div className="product-detail-header">
          <div className="detail-sku-badge">SKU: {sku}</div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Fechar detalhes"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Body */}
        <div className="product-detail-body">
          {/* Product Hero Visual */}
          <div className="product-detail-visual">
            {image ? (
              <img src={image} alt={name} className="product-detail-img" />
            ) : (
              <div className="detail-icon-fallback" style={{ color: meta.color }}>
                <span className="material-symbols-outlined">{meta.icon || 'inventory_2'}</span>
              </div>
            )}
            {secao && <span className="product-detail-sector-pill">{secao}</span>}
          </div>

          {/* Title and Description */}
          <div className="product-detail-main-info">
            <h2 className="product-detail-title">{name}</h2>
            {specs && <p className="product-detail-description">{specs}</p>}
          </div>

          {/* Physical Location in Store & Map Button */}
          <div className="product-detail-location-card">
            <div className="detail-loc-icon">
              <span className="material-symbols-outlined filled">location_on</span>
            </div>
            <div className="detail-loc-text">
              <span className="detail-loc-label">Localização Física na Loja</span>
              <strong className="detail-loc-value">{corredor}</strong>
            </div>
            <button
              type="button"
              className="detail-map-btn"
              onClick={() => {
                onClose()
                if (onViewOnMap) {
                  onViewOnMap(detailedProduct)
                } else if (onNavigateToProduct) {
                  onNavigateToProduct(detailedProduct)
                }
              }}
              title="Ver no mapa inteligente da loja"
            >
              <span className="material-symbols-outlined">map</span>
              <span>Ver no Mapa</span>
            </button>
          </div>

          {/* Stock & Availability status */}
          <div className={`product-detail-stock-badge ${isOutOfStock ? 'out-of-stock' : ''}`}>
            <span className="material-symbols-outlined">
              {isOutOfStock ? 'warning' : 'check_circle'}
            </span>
            <span>
              {isOutOfStock
                ? 'Produto esgotado nesta unidade (Simulação de Ruptura)'
                : `${stock} unidades disponíveis para pronta entrega na loja`}
            </span>
          </div>

          {/* Specifications Table (Atributos do Backend - GET /produtos/{id}) */}
          <div className="product-detail-attributes-section">
            <h4 className="detail-attributes-heading">Especificações Técnicas</h4>
            {isLoading ? (
              <div className="attributes-loading-skeleton">
                <div className="skeleton-line"></div>
                <div className="skeleton-line"></div>
                <div className="skeleton-line short"></div>
              </div>
            ) : atributos && atributos.length > 0 ? (
              <div className="attributes-grid">
                {atributos.map((attr, index) => (
                  <div key={index} className="attribute-row">
                    <span className="attribute-key">{attr.rotulo || attr.chave}:</span>
                    <span className="attribute-val">{attr.valor}</span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="attributes-grid">
                <div className="attribute-row">
                  <span className="attribute-key">Departamento:</span>
                  <span className="attribute-val">{secao || 'Geral'}</span>
                </div>
                <div className="attribute-row">
                  <span className="attribute-key">Corredor:</span>
                  <span className="attribute-val">{corredor}</span>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer with Price & Actions */}
        <div className="product-detail-footer">
          <div className="detail-price-box">
            <span className="detail-price-label">Preço na loja física</span>
            <span className="detail-price-value">{formatPrice(price)}</span>
          </div>

          <div className="detail-footer-actions">
            <button
              type="button"
              className={`detail-add-cart-btn ${isOutOfStock ? 'disabled' : ''}`}
              disabled={isOutOfStock}
              onClick={() => {
                onAddToCart(detailedProduct)
                onClose()
              }}
            >
              <span className="material-symbols-outlined">add_shopping_cart</span>
              <span>{isOutOfStock ? 'Indisponível' : 'Adicionar ao Roteiro'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
