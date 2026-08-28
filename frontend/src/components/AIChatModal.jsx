import React, { useState, useEffect, useRef } from 'react'
import { consultarHistoricoChat, enviarMensagemChat } from '../services/chatService'
import { formatPrice } from '../utils/format'

export default function AIChatModal({
  isOpen,
  onClose,
  sessionId,
  catalogProducts = [],
  screenContext = null,
  onAddToCart,
  onViewProductDetails
}) {
  const [messages, setMessages] = useState([])
  const [inputText, setInputText] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const messagesEndRef = useRef(null)
  const inputRef = useRef(null)

  const isProductContext = screenContext?.view === 'product-detail' && screenContext?.product

  const SUGGESTED_QUESTIONS = isProductContext
    ? [
        `Para que serve este item?`,
        `Onde fica o corredor deste produto?`,
        `Quais produtos complementares eu preciso?`,
        `Especificações técnicas`
      ]
    : [
        'O que eu preciso para pintar uma parede?',
        'Quais materiais para instalar uma tomada 220V?',
        'Como montar um jardim com vasos e regador?',
        'Ferramentas para furar parede de alvenaria'
      ]

  // Initialize or load conversation history
  useEffect(() => {
    if (isOpen) {
      if (sessionId) {
        consultarHistoricoChat(sessionId)
          .then((history) => {
            if (history && history.length > 0) {
              setMessages(history)
            } else if (messages.length === 0) {
              setMessages([
                {
                  id: 'welcome',
                  remetente: 'ASSISTANT',
                  conteudo: isProductContext
                    ? `Olá! Vejo que você está olhando **${screenContext.product.nome || screenContext.product.name}** no **${screenContext.product.corredor || 'corredor da loja'}**. Como posso te ajudar com este produto ou aplicação?`
                    : 'Olá! Sou o assistente inteligente da Leroy Merlin. Como posso te ajudar hoje com seu projeto, reforma ou localização de produtos na loja?',
                  enviadoEm: new Date().toISOString()
                }
              ])
            }
          })
          .catch(() => {
            if (messages.length === 0) {
              setMessages([
                {
                  id: 'welcome',
                  remetente: 'ASSISTANT',
                  conteudo: isProductContext
                    ? `Olá! Estou pronto para tirar dúvidas sobre **${screenContext.product.nome || screenContext.product.name}**. O que gostaria de saber?`
                    : 'Olá! Sou o assistente inteligente da Leroy Merlin. O que você procura ou gostaria de fazer na sua casa hoje?',
                  enviadoEm: new Date().toISOString()
                }
              ])
            }
          })
      } else if (messages.length === 0) {
        setMessages([
          {
            id: 'welcome',
            remetente: 'ASSISTANT',
            conteudo: isProductContext
              ? `Olá! Estou ciente de que você está na página de **${screenContext.product.nome || screenContext.product.name}** (${screenContext.product.corredor || 'corredor'}). O que deseja saber sobre ele?`
              : 'Olá! Sou o assistente virtual da Leroy Merlin. Pergunte sobre qualquer material, ferramenta ou projeto que indico os produtos e seus respectivos corredores!',
            enviadoEm: new Date().toISOString()
          }
        ])
      }

      setTimeout(() => {
        if (inputRef.current) inputRef.current.focus()
      }, 150)
    }
  }, [isOpen, sessionId, isProductContext])

  // Scroll to bottom on message updates
  useEffect(() => {
    if (isOpen && messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' })
    }
  }, [messages, isLoading, isOpen])

  if (!isOpen) return null

  // Function to extract or correlate products cited in message content
  const extractCitedProducts = (msg) => {
    if (msg.produtosRecomendados && msg.produtosRecomendados.length > 0) {
      return msg.produtosRecomendados
    }

    if (!msg.conteudo || msg.remetente === 'USER' || catalogProducts.length === 0) {
      return []
    }

    const content = msg.conteudo.toLowerCase()
    return catalogProducts.filter(p => {
      const name = (p.nome || p.name || '').toLowerCase()
      const sku = (p.sku || '').toLowerCase()
      if (sku && content.includes(sku)) return true
      
      const words = name.split(' ').filter(w => w.length > 3)
      const matches = words.filter(w => content.includes(w))
      return matches.length >= 2 || (name.length > 5 && content.includes(name))
    }).slice(0, 3)
  }

  const handleSendMessage = async (textToSend) => {
    const text = (textToSend || inputText).trim()
    if (!text || isLoading) return

    const userMessage = {
      id: 'user-' + Date.now(),
      remetente: 'USER',
      conteudo: text,
      enviadoEm: new Date().toISOString()
    }

    setMessages(prev => [...prev, userMessage])
    setInputText('')
    setIsLoading(true)

    try {
      const response = await enviarMensagemChat(sessionId, text, catalogProducts, screenContext)
      if (response) {
        setMessages(prev => [...prev, response])
      }
    } catch (e) {
      setMessages(prev => [
        ...prev,
        {
          id: 'err-' + Date.now(),
          remetente: 'ASSISTANT',
          conteudo: 'Desculpe, tive uma instabilidade momentânea na conexão. Por favor, tente novamente ou consulte um de nossos vendedores no corredor.',
          enviadoEm: new Date().toISOString()
        }
      ])
    } finally {
      setIsLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  return (
    <div className="modal-backdrop chat-modal-backdrop" onClick={onClose}>
      <div 
        className="chat-modal-container animate-fade-in"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-label="Assistente Virtual de Compras"
      >
        {/* Chat Header */}
        <div className="chat-modal-header">
          <div className="chat-header-info">
            <div className="chat-avatar-box">
              <span className="material-symbols-outlined filled chat-avatar-icon">smart_toy</span>
              <span className="chat-status-dot"></span>
            </div>
            <div className="chat-title-wrap">
              <h3 className="chat-title">Assistente IA da Loja</h3>
              <span className="chat-status-text">
                {isProductContext ? 'Contexto de Produto Ativo' : 'Grounded no Catálogo'}
              </span>
            </div>
          </div>
          <button 
            type="button" 
            className="modal-close-btn" 
            onClick={onClose}
            aria-label="Fechar conversa"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Active Screen Context Banner */}
        {isProductContext && (
          <div className="chat-context-badge-bar">
            <div className="chat-context-icon">
              <span className="material-symbols-outlined filled">visibility</span>
            </div>
            <div className="chat-context-info">
              <span className="chat-context-label">Visualizando agora:</span>
              <strong className="chat-context-prod-name">
                {screenContext.product.nome || screenContext.product.name}
              </strong>
            </div>
            <span className="chat-context-aisle-pill">
              {screenContext.product.corredor || 'Corredor da Loja'}
            </span>
          </div>
        )}

        {/* Quick Suggestions Pills */}
        <div className="chat-quick-suggestions-wrap">
          <span className="chat-suggestions-title">
            {isProductContext ? 'Perguntas sobre este item:' : 'Sugestões de projetos:'}
          </span>
          <div className="chat-suggestions-scroll">
            {SUGGESTED_QUESTIONS.map((q, idx) => (
              <button
                key={idx}
                type="button"
                className="chat-suggestion-chip"
                onClick={() => handleSendMessage(q)}
              >
                <span className="material-symbols-outlined filled">auto_awesome</span>
                <span>{q}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Messages Stream Area */}
        <div className="chat-messages-container">
          {messages.map((msg, index) => {
            const isAssistant = msg.remetente === 'ASSISTANT' || msg.remetente === 'assistant'
            const citedProducts = isAssistant ? extractCitedProducts(msg) : []

            return (
              <div 
                key={msg.id || index}
                className={`chat-message-row ${isAssistant ? 'assistant-row' : 'user-row'}`}
              >
                {isAssistant && (
                  <div className="chat-msg-avatar">
                    <span className="material-symbols-outlined filled">smart_toy</span>
                  </div>
                )}

                <div className={`chat-bubble ${isAssistant ? 'assistant-bubble' : 'user-bubble'}`}>
                  <p className="chat-bubble-text">{msg.conteudo}</p>

                  {/* Cited / Recommended Products Cards */}
                  {citedProducts && citedProducts.length > 0 && (
                    <div className="chat-cited-products-grid">
                      <span className="cited-products-label">Produtos encontrados nesta loja:</span>
                      {citedProducts.map((prod) => (
                        <div key={prod.id || prod.sku} className="chat-product-card">
                          <div className="chat-product-img-box">
                            {prod.imagemUrl || prod.image ? (
                              <img src={prod.imagemUrl || prod.image} alt={prod.nome || prod.name} />
                            ) : (
                              <span className="material-symbols-outlined">inventory_2</span>
                            )}
                          </div>

                          <div className="chat-product-details">
                            <h4 className="chat-product-name">{prod.nome || prod.name}</h4>
                            <div className="chat-product-meta-row">
                              <span className="chat-product-corredor">
                                <span className="material-symbols-outlined">location_on</span>
                                {prod.pontoMapa?.corredor || prod.corredor || 'Corredor Loja'}
                              </span>
                              <span className="chat-product-price">
                                {formatPrice(prod.preco || prod.price)}
                              </span>
                            </div>
                          </div>

                          <div className="chat-product-actions">
                            <button
                              type="button"
                              className="chat-view-detail-btn"
                              onClick={() => {
                                onClose()
                                onViewProductDetails(prod)
                              }}
                              title="Ver página de detalhes do produto"
                            >
                              <span>Detalhes</span>
                            </button>
                            <button
                              type="button"
                              className="chat-add-cart-btn"
                              onClick={() => onAddToCart(prod)}
                              title="Adicionar ao Roteiro"
                            >
                              <span className="material-symbols-outlined">add_shopping_cart</span>
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            )
          })}

          {/* Typing Indicator */}
          {isLoading && (
            <div className="chat-message-row assistant-row">
              <div className="chat-msg-avatar">
                <span className="material-symbols-outlined filled">smart_toy</span>
              </div>
              <div className="chat-bubble assistant-bubble typing-bubble">
                <div className="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <span className="typing-text">Consultando catálogo e especificações...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <div className="chat-modal-footer">
          <form 
            onSubmit={(e) => {
              e.preventDefault()
              handleSendMessage()
            }} 
            className="chat-input-form"
          >
            <input
              ref={inputRef}
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={isProductContext ? `Pergunte sobre este item...` : "Digite sua dúvida ou projeto (ex: pintar parede...)"}
              className="chat-text-input"
              disabled={isLoading}
              autoComplete="off"
            />
            <button
              type="submit"
              className="chat-send-btn"
              disabled={!inputText.trim() || isLoading}
              aria-label="Enviar mensagem"
              title="Enviar"
            >
              <span className="material-symbols-outlined">send</span>
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
