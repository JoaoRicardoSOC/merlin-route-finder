import React from 'react'

export default function ProductCard({ product, onAddToCart, onNavigateToProduct }) {
  const formatPrice = (price) => {
    return price.toLocaleString('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    })
  }

  return (
    <div className="product-card">
      <div className="product-visual">
        {product.image ? (
          <img src={product.image} alt={product.name} className="product-img" />
        ) : (
          <div className="product-icon-fallback">
            <span className="material-symbols-outlined">{product.icon || 'lightbulb'}</span>
          </div>
        )}
        {product.tag && (
          <span className="product-badge">{product.tag}</span>
        )}
      </div>

      <div className="product-content">
        <div className="product-header">
          <h3 className="product-name">{product.name}</h3>
          <p className="product-specs">{product.specs}</p>
        </div>

        <div className="product-stock-location">
          <div className="stock-info">
            <span className="material-symbols-outlined stock-icon">inventory_2</span>
            <span className="stock-text">{product.stock} un. disponíveis</span>
          </div>
          <div className="location-info-tag">
            <span className="material-symbols-outlined loc-pin-icon filled">location_on</span>
            <span className="aisle-name">{product.corredor}</span>
          </div>
        </div>

        <div className="product-footer">
          <div className="price-block">
            <span className="price-label">À vista</span>
            <span className="price-value">{formatPrice(product.price)}</span>
          </div>

          <div className="product-actions">
            <button 
              className="action-btn route-btn" 
              onClick={() => onNavigateToProduct(product)}
              title="Traçar rota até este produto"
            >
              <span className="material-symbols-outlined">near_me</span>
            </button>
            <button 
              className="action-btn cart-btn" 
              onClick={() => onAddToCart(product)}
              title="Adicionar à lista de compras"
            >
              <span className="material-symbols-outlined">add_shopping_cart</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
