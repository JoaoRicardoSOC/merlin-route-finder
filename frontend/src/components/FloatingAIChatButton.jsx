import React from 'react'

export default function FloatingAIChatButton({ onClick, hasUnread = false, isProductPage = false }) {
  return (
    <button
      type="button"
      className={`floating-ai-chat-btn ${isProductPage ? 'on-product-page' : ''}`}
      onClick={onClick}
      aria-label="Abrir Assistente Virtual de IA"
      title="Pergunte ao Assistente da Loja (IA)"
    >
      <div className="floating-ai-icon-box">
        <span className="material-symbols-outlined filled floating-ai-sparkle">auto_awesome</span>
        <span className="floating-ai-beacon">
          <span className="ai-beacon-ping"></span>
          <span className="ai-beacon-dot"></span>
        </span>
      </div>
      <span className="floating-ai-label">Assistente IA</span>
    </button>
  )
}
