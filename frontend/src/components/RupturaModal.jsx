import { formatPrice } from '../utils/format'
import useModalAcessivel from '../hooks/useModalAcessivel'

/**
 * Prateleira vazia: o cliente relata que não achou o produto e recebe um substituto.
 *
 * Três estados, e eles não são variações um do outro — cada um pede uma tela diferente:
 * esperando a resposta, sugestão encontrada, e nada plausível por perto.
 *
 * A origem da sugestão é mostrada de propósito. O backend devolve `origemSugestao` justamente
 * para a tela não chamar de recomendação inteligente o que foi só o produto disponível mais
 * próximo — são dois selos diferentes, e não um texto genérico que serve para os dois.
 */
export default function RupturaModal({
  isOpen,
  onClose,
  itemEmFalta,
  resultado,
  isCarregando,
  isTrocando,
  onAceitar
}) {
  // Antes do retorno antecipado: hook nao pode ficar atras de um `return`.
  const refModal = useModalAcessivel(isOpen, onClose)

  if (!isOpen) return null

  const sugestao = resultado?.estado === 'sugerido' ? resultado.sugestao : null
  const produto = sugestao?.produtoSugerido
  const veioDaIA = sugestao?.origemSugestao === 'ASSISTENTE_IA'

  // O corredor vem plano na resposta desde que a listagem passou a levá-lo; o ponto do mapa
  // fica como recuo para respostas antigas em cache.
  const corredorDe = (p) => p?.corredor || p?.pontoMapa?.corredor || null

  return (
    <div className="modal-backdrop ruptura-backdrop" onClick={onClose}>
      <div
        className="ruptura-modal"
        onClick={(e) => e.stopPropagation()}
        ref={refModal}
        role="dialog"
        aria-modal="true"
        aria-label="Produto não encontrado na prateleira"
      >
        <header className="ruptura-header">
          <span className="material-symbols-outlined ruptura-header-icon" aria-hidden="true">production_quantity_limits</span>
          <div>
            <h3>Prateleira vazia</h3>
            <p>{itemEmFalta?.nome}</p>
          </div>
          <button
            type="button"
            className="ruptura-fechar"
            onClick={onClose}
            aria-label="Fechar"
          >
            <span className="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        </header>

        <div className="ruptura-corpo">
          {isCarregando && (
            <div className="ruptura-esperando">
              <div className="typing-dots">
                <span></span><span></span><span></span>
              </div>
              <p className="typing-text">
                Procurando algo que resolva, entre o que a loja tem por perto…
              </p>
            </div>
          )}

          {!isCarregando && sugestao && (
            <>
              <span className={`ruptura-selo ${veioDaIA ? 'selo-ia' : 'selo-proximidade'}`}>
                <span className="material-symbols-outlined" aria-hidden="true">
                  {veioDaIA ? 'auto_awesome' : 'near_me'}
                </span>
                {veioDaIA ? 'Sugestão do assistente' : 'Disponível mais próximo'}
              </span>

              <div className="ruptura-produto">
                <div className="ruptura-produto-visual">
                  {produto?.imagemUrl ? (
                    <img src={produto.imagemUrl} alt={produto.nome} />
                  ) : (
                    <span className="material-symbols-outlined" aria-hidden="true">category</span>
                  )}
                </div>
                <div className="ruptura-produto-info">
                  <h4>{produto?.nome}</h4>
                  {corredorDe(produto) && (
                    <div className="ruptura-produto-corredor">
                      <span className="material-symbols-outlined" aria-hidden="true">location_on</span>
                      <span>{corredorDe(produto)}</span>
                    </div>
                  )}
                  <span className="ruptura-produto-preco">{formatPrice(produto?.preco)}</span>
                </div>
              </div>

              <p className="ruptura-justificativa">{sugestao.justificativa}</p>
            </>
          )}

          {!isCarregando && resultado?.estado === 'sem-substituto' && (
            <div className="ruptura-vazio">
              <span className="material-symbols-outlined" aria-hidden="true">search_off</span>
              <h4>Não achamos um substituto por perto</h4>
              <p>
                Nada equivalente está disponível a uma distância curta daqui. O aviso de
                prateleira vazia <strong>foi registrado</strong> — a loja recebe essa
                informação mesmo assim.
              </p>
            </div>
          )}

          {!isCarregando && resultado?.estado === 'erro' && (
            <div className="ruptura-vazio">
              <span className="material-symbols-outlined" aria-hidden="true">cloud_off</span>
              <h4>Não deu para consultar agora</h4>
              <p>{resultado.mensagem}</p>
            </div>
          )}
        </div>

        <footer className="ruptura-acoes">
          {sugestao ? (
            <>
              <button
                type="button"
                className="ruptura-btn-principal"
                onClick={() => onAceitar(sugestao.produtoSugeridoId)}
                disabled={isTrocando}
              >
                <span className="material-symbols-outlined" aria-hidden="true">swap_horiz</span>
                {isTrocando ? 'Trocando…' : 'Levar este'}
              </button>
              <button
                type="button"
                className="ruptura-btn-secundario"
                onClick={onClose}
                disabled={isTrocando}
              >
                Não, obrigado
              </button>
            </>
          ) : (
            !isCarregando && (
              <button type="button" className="ruptura-btn-secundario" onClick={onClose}>
                Entendi
              </button>
            )
          )}
        </footer>
      </div>
    </div>
  )
}
