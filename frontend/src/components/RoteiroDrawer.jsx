import React from 'react'

export default function RoteiroDrawer({
  isOpen,
  onClose,
  items = [],
  onRemoveItem,
  onToggleCollectItem,
  onClearAll,
  onStartRoute
}) {
  if (!isOpen) return null

  const formatPrice = (price) => {
    const val = typeof price === 'number' ? price : parseFloat(price) || 0
    return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
  }

  const totalPrice = items.reduce((acc, item) => {
    const p = typeof item.preco === 'number' ? item.preco : parseFloat(item.preco) || 0
    return acc + p
  }, 0)

  const collectedCount = items.filter(i => i.coletado).length

  return (
    <div className="modal-backdrop roteiro-drawer-backdrop" onClick={onClose}>
      <div
        className="roteiro-drawer-container"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Meu Roteiro de Compras na Loja"
      >
        {/* Header */}
        <div className="roteiro-drawer-header">
          <div className="roteiro-drawer-title-wrap">
            <div className="roteiro-drawer-icon-wrap">
              <span className="material-symbols-outlined filled">shopping_cart</span>
            </div>
            <div>
              <h3>Meu Roteiro</h3>
              <p>
                {items.length} {items.length === 1 ? 'produto' : 'produtos'} • {collectedCount} {collectedCount === 1 ? 'coletado' : 'coletados'}
              </p>
            </div>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Fechar roteiro"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* List of Items */}
        <div className="roteiro-drawer-body">
          {items.length === 0 ? (
            <div className="roteiro-empty-state">
              <span className="material-symbols-outlined empty-cart-icon">remove_shopping_cart</span>
              <h4>Seu roteiro está vazio</h4>
              <p>Explore os setores ou busque produtos na vitrine e adicione os itens que deseja buscar na loja.</p>
              <button
                type="button"
                className="empty-action-btn primary"
                onClick={onClose}
              >
                Explorar Catálogo
              </button>
            </div>
          ) : (
            <div className="roteiro-items-list">
              {items.map((item) => (
                <div key={item.id} className={`roteiro-item-card ${item.coletado ? 'is-collected' : ''}`}>
                  {/* Collection Toggle Checkbox */}
                  <button
                    type="button"
                    className={`roteiro-check-btn ${item.coletado ? 'checked' : ''}`}
                    onClick={() => onToggleCollectItem && onToggleCollectItem(item.id)}
                    title={item.coletado ? 'Desmarcar coleta' : 'Marcar como coletado na prateleira'}
                    aria-label={`Marcar ${item.nome} como coletado`}
                  >
                    <span className="material-symbols-outlined">
                      {item.coletado ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                  </button>

                  <div className="roteiro-item-visual">
                    {item.imagemUrl ? (
                      <img src={item.imagemUrl} alt={item.nome} className="roteiro-item-img" />
                    ) : (
                      <span className="material-symbols-outlined roteiro-item-icon">inventory_2</span>
                    )}
                  </div>

                  <div className="roteiro-item-info">
                    <h4 className={`roteiro-item-name ${item.coletado ? 'strikethrough' : ''}`}>
                      {item.nome}
                    </h4>
                    <div className="roteiro-item-corredor">
                      <span className="material-symbols-outlined">location_on</span>
                      <span>{item.corredor}</span>
                      {item.coletado && <span className="coletado-badge">Coletado</span>}
                    </div>
                    <span className="roteiro-item-price">{formatPrice(item.preco)}</span>
                  </div>

                  <button
                    type="button"
                    className="roteiro-remove-btn"
                    onClick={() => onRemoveItem(item.id)}
                    title="Remover do roteiro"
                    aria-label={`Remover ${item.nome} do roteiro`}
                  >
                    <span className="material-symbols-outlined">delete_outline</span>
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>


        {/* Footer with totals and action buttons */}
        {items.length > 0 && (
          <div className="roteiro-drawer-footer">
            <div className="roteiro-totals-row">
              <div className="roteiro-total-items">
                <span>Total ({items.length} {items.length === 1 ? 'item' : 'itens'})</span>
                <button
                  type="button"
                  className="roteiro-clear-all-btn"
                  onClick={onClearAll}
                >
                  Limpar lista
                </button>
              </div>
              <div className="roteiro-total-price">
                <span className="total-label">Estimativa:</span>
                <strong className="total-value">{formatPrice(totalPrice)}</strong>
              </div>
            </div>

            <button
              type="button"
              className="roteiro-start-route-btn"
              onClick={() => {
                onClose()
                if (onStartRoute) onStartRoute(items)
              }}
            >
              <span className="material-symbols-outlined">directions_walk</span>
              <span>Traçar Rota Otimizada da Compra</span>
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
