import React, { useState } from 'react'

export default function FimJornadaModal({
  isOpen,
  onClose,
  items = [],
  onConfirmAjudaCaixa,
  onConcluirSemCaixa
}) {
  if (!isOpen) return null

  const collectedItems = items.filter(i => i.coletado)
  const isFullCollection = items.length > 0 && collectedItems.length === items.length
  
  const totalPrice = collectedItems.reduce((acc, item) => {
    const p = typeof item.preco === 'number' ? item.preco : parseFloat(item.preco) || 0
    return acc + p
  }, 0)

  const formatPrice = (val) => {
    return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
  }

  return (
    <div className="modal-backdrop fim-jornada-backdrop" onClick={onClose}>
      <div
        className="fim-jornada-modal-container"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Fim da Jornada de Compra"
      >
        {/* Decorative Header */}
        <div className="fim-jornada-header">
          <div className="celebration-icon-wrap">
            <span className="material-symbols-outlined celebration-icon filled">
              {isFullCollection ? 'task_alt' : 'shopping_bag'}
            </span>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="fim-jornada-body">
          <h3 className="fim-jornada-title">
            {isFullCollection
              ? 'Todos os itens coletados!'
              : 'Deseja finalizar suas compras?'}
          </h3>
          <p className="fim-jornada-subtitle">
            {isFullCollection
              ? 'Parabéns! Você encontrou todos os produtos da sua lista de compras na loja.'
              : `Você coletou ${collectedItems.length} de ${items.length} itens da sua lista e pode seguir para o pagamento com o que já tem.`}
          </p>

          {/* Mini Stats Card */}
          <div className="fim-jornada-stats-card">
            <div className="stat-box">
              <span className="stat-label">Itens Coletados</span>
              <strong className="stat-value">{collectedItems.length} de {items.length}</strong>
            </div>
            <div className="stat-divider"></div>
            <div className="stat-box">
              <span className="stat-label">Total Estimado</span>
              <strong className="stat-value price">{formatPrice(totalPrice)}</strong>
            </div>
          </div>

          {/* Question Box */}
          <div className="fim-jornada-question-card">
            <div className="question-icon">
              <span className="material-symbols-outlined filled">point_of_sale</span>
            </div>
            <div className="question-text">
              <h4>Deseja ajuda para encontrar os caixas?</h4>
              <p>Podemos traçar no mapa o caminho mais rápido até a frente de caixas de pagamento.</p>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="fim-jornada-footer">
          <button
            type="button"
            className="fim-action-btn primary"
            onClick={() => {
              onConfirmAjudaCaixa()
              onClose()
            }}
          >
            <span className="material-symbols-outlined">directions_walk</span>
            <span>Sim, traçar rota até o Caixa</span>
          </button>

          <button
            type="button"
            className="fim-action-btn secondary"
            onClick={() => {
              onConcluirSemCaixa()
              onClose()
            }}
          >
            <span className="material-symbols-outlined">check</span>
            <span>Não preciso, finalizar compra</span>
          </button>
        </div>
      </div>
    </div>
  )
}
