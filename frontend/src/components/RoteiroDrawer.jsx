import React from 'react'
import { formatPrice } from '../utils/format'
import useModalAcessivel from '../hooks/useModalAcessivel'

export default function RoteiroDrawer({
  isOpen,
  onClose,
  items = [],
  onRemoveItem,
  onToggleCollectItem,
  onRelatarRuptura,
  isBuscandoSubstituto = false,
  onClearAll,
  onStartRoute,
  onEncerrarJornada
}) {

  // Antes do retorno antecipado: hook nao pode ficar atras de um `return`.
  const refModal = useModalAcessivel(isOpen, onClose)

  if (!isOpen) return null

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
        ref={refModal}
        role="dialog"
        aria-modal="true"
        aria-label="Meu Roteiro de Compras na Loja"
      >
        {/* Header */}
        <div className="roteiro-drawer-header">
          <div className="roteiro-drawer-title-wrap">
            <div className="roteiro-drawer-icon-wrap">
              <span className="material-symbols-outlined filled" aria-hidden="true">shopping_cart</span>
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
            <span className="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        </div>

        {/* List of Items */}
        <div className="roteiro-drawer-body">
          {items.length === 0 ? (
            <div className="roteiro-empty-state">
              <span className="material-symbols-outlined empty-cart-icon" aria-hidden="true">remove_shopping_cart</span>
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
                    <span className="material-symbols-outlined" aria-hidden="true">
                      {item.coletado ? 'check_circle' : 'radio_button_unchecked'}
                    </span>
                  </button>

                  <div className="roteiro-item-visual">
                    {item.imagemUrl ? (
                      <img src={item.imagemUrl} alt={item.nome} className="roteiro-item-img" />
                    ) : (
                      <span className="material-symbols-outlined roteiro-item-icon" aria-hidden="true">inventory_2</span>
                    )}
                  </div>

                  <div className="roteiro-item-info">
                    <h4 className={`roteiro-item-name ${item.coletado ? 'strikethrough' : ''}`}>
                      {item.nome}
                    </h4>
                    <div className="roteiro-item-corredor">
                      <span className="material-symbols-outlined" aria-hidden="true">location_on</span>
                      <span>{item.corredor}</span>
                      {item.coletado && <span className="coletado-badge">Coletado</span>}
                    </div>
                    <span className="roteiro-item-price">{formatPrice(item.preco)}</span>
                  </div>

                  <div className="roteiro-item-acoes">
                    {/* Só faz sentido para item que o cliente ainda está procurando: quem
                        coletou, achou. E a rota exige o id do servidor, então item que ainda
                        não sincronizou avisa em vez de falhar depois do toque. */}
                    {!item.coletado && (
                      <button
                        type="button"
                        className="roteiro-ruptura-btn"
                        onClick={() => onRelatarRuptura && onRelatarRuptura(item.id)}
                        /*
                         * Duas guardas com motivos diferentes, e antes só existia a primeira:
                         *
                         * `!item.idBackend` impede tocar em item que ainda não chegou ao
                         * servidor — a chamada de ruptura precisa do id de lá.
                         *
                         * `isBuscandoSubstituto` impede o toque duplo com a requisição em voo.
                         * Repetir o relato é proposital (duas visitas frustradas são dois dados
                         * para a loja), mas o toque acidental custa DUAS chamadas ao Gemini, e
                         * a cota gratuita é de cinco por minuto.
                         */
                        disabled={!item.idBackend || isBuscandoSubstituto}
                        title={!item.idBackend
                          ? 'Item ainda não sincronizado com a loja'
                          : isBuscandoSubstituto
                          ? 'Procurando um substituto…'
                          : 'Não encontrei este produto na prateleira'}
                        aria-label={`Não encontrei ${item.nome} na prateleira`}
                      >
                        <span className="material-symbols-outlined" aria-hidden="true">production_quantity_limits</span>
                      </button>
                    )}

                    <button
                      type="button"
                      className="roteiro-remove-btn"
                      onClick={() => onRemoveItem(item.id)}
                      title="Remover do roteiro"
                      aria-label={`Remover ${item.nome} do roteiro`}
                    >
                      <span className="material-symbols-outlined" aria-hidden="true">delete_outline</span>
                    </button>
                  </div>
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

            <div className="roteiro-actions-stack">
              <button
                type="button"
                className="roteiro-start-route-btn"
                onClick={() => {
                  onClose()
                  if (onStartRoute) onStartRoute(items)
                }}
              >
                <span className="material-symbols-outlined" aria-hidden="true">directions_walk</span>
                <span>Traçar Rota no Mapa</span>
              </button>

              <button
                type="button"
                className="roteiro-finish-btn"
                onClick={() => {
                  onClose()
                  if (onEncerrarJornada) onEncerrarJornada()
                }}
              >
                <span className="material-symbols-outlined" aria-hidden="true">point_of_sale</span>
                <span>Encerrar Compra & Ir ao Caixa</span>
              </button>
            </div>
          </div>
        )}
      </div>
    </div>

  )
}
