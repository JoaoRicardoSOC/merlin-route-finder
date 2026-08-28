import React from 'react'
import { SECTOR_METADATA, DEFAULT_SECTOR_META } from '../constants/setores'
import { formatPrice } from '../utils/format'

export default function ProductCard({
  product,
  onAddToCart,
  onNavigateToProduct,
  onViewDetails
}) {
  const name = product.nome || product.name || 'Produto sem nome'
  const specs = product.descricao || product.specs || ''
  const price = product.preco ?? product.price ?? 0
  const stock = product.saldoEstoque ?? product.stock ?? 0
  const secao = product.secao || ''
  const corredor = product.corredor || (secao && SECTOR_METADATA[secao]?.corredor) || 'Corredor da Loja'
  const image = product.imagemUrl || product.image || null
  const tag = product.tag || (stock === 0 ? 'Sem Estoque (Ruptura)' : stock <= 5 ? 'Últimas Unidades' : null)

  const meta = SECTOR_METADATA[secao] || DEFAULT_SECTOR_META
  const icon = product.icon || meta.icon || 'inventory_2'
  const isOutOfStock = stock <= 0

  const handleCardClick = () => {
    if (onViewDetails) {
      onViewDetails(product)
    }
  }

  return (
    <div className={`product-card ${isOutOfStock ? 'out-of-stock' : ''}`}>
      {/* Clickable Visual Area */}
      <div className="product-visual" onClick={handleCardClick} role="button" tabIndex={0}>
        {image ? (
          <img src={image} alt={name} className="product-img" loading="lazy" />
        ) : (
          <div className="product-icon-fallback" style={{ color: meta.color }}>
            <span className="material-symbols-outlined">{icon}</span>
          </div>
        )}
        {tag && (
          <span className={`product-badge ${isOutOfStock ? 'badge-out-of-stock' : ''}`}>
            {tag}
          </span>
        )}
        {secao && (
          <span className="product-sector-pill">
            {secao}
          </span>
        )}
      </div>

      <div className="product-content">
        <div className="product-header" onClick={handleCardClick} role="button" tabIndex={0}>
          <h3 className="product-name">{name}</h3>
          {specs && <p className="product-specs">{specs}</p>}
        </div>

        <div className="product-stock-location" onClick={handleCardClick} role="button" tabIndex={0}>
          <div className={`stock-info ${isOutOfStock ? 'stock-zero' : ''}`}>
            <span className="material-symbols-outlined stock-icon">
              {isOutOfStock ? 'warning' : 'inventory_2'}
            </span>
            <span className="stock-text">
              {isOutOfStock ? 'Estoque Esgotado' : `${stock} un. disponíveis`}
            </span>
          </div>
          <div className="location-info-tag">
            <span className="material-symbols-outlined loc-pin-icon filled">location_on</span>
            <span className="aisle-name">{corredor}</span>
          </div>
        </div>

        <div className="product-footer">
          <div className="price-block" onClick={handleCardClick} role="button" tabIndex={0}>
            <span className="price-label">À vista</span>
            <span className="price-value">{formatPrice(price)}</span>
          </div>

          <div className="product-actions">
            <button
              type="button"
              className="action-btn route-btn"
              onClick={(e) => {
                e.stopPropagation()
                onNavigateToProduct({ ...product, name, corredor, price, stock })
              }}
              title="Traçar rota até este produto na loja"
              aria-label={`Traçar rota até ${name}`}
            >
              <span className="material-symbols-outlined">near_me</span>
            </button>
            <button
              type="button"
              className={`action-btn cart-btn ${isOutOfStock ? 'btn-disabled' : ''}`}
              onClick={(e) => {
                e.stopPropagation()
                if (!isOutOfStock) {
                  onAddToCart({ ...product, name, corredor, price, stock })
                }
              }}
              title={isOutOfStock ? 'Produto indisponível' : 'Adicionar ao roteiro de compras'}
              disabled={isOutOfStock}
              aria-label={`Adicionar ${name} ao roteiro`}
            >
              <span className="material-symbols-outlined">
                {isOutOfStock ? 'block' : 'add_shopping_cart'}
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
