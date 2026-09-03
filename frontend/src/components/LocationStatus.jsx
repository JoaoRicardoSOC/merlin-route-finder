
/**
 * Onde o cliente está — ou o convite para descobrir, quando ainda não sabemos.
 *
 * Sem posição, este cartão não inventa a entrada da loja: ele diz que não sabe e convida a
 * escanear uma placa. É o dado que separa este produto de um catálogo online, e afirmá-lo
 * errado é pior que admitir a ausência.
 */
export default function LocationStatus({ location, onClick }) {
  const semPosicao = !location?.aisle
  const aisleText = location?.aisle || 'Escaneie uma placa para aparecer aqui'
  const codeBadge = location?.code ? `Placa ${location.code}` : null

  return (
    <div 
      className="modern-location-status-card"
      onClick={onClick}
      onKeyDown={(evento) => {
        // Vide ProductCard: `role="button"` num <div> não ganha Enter/Espaço do navegador.
        if (onClick && (evento.key === 'Enter' || evento.key === ' ')) {
          evento.preventDefault()
          onClick()
        }
      }}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
    >
      <div className="modern-location-beacon">
        <span className="beacon-ping"></span>
        <span className="beacon-dot"></span>
      </div>

      <div className="modern-location-content">
        <div className="modern-location-top">
          <span className="modern-location-label">
            {semPosicao ? 'Ainda não sabemos onde você está' : 'Você está em'}
          </span>
          {codeBadge && <span className="modern-plate-tag">{codeBadge}</span>}
        </div>
        <div className="modern-location-name">
          <span className="material-symbols-outlined modern-pin-icon filled" aria-hidden="true">location_on</span>
          <strong>{aisleText}</strong>
        </div>
        {/*
          * As coordenadas saíram daqui.
          *
          * `(50, 92)` é saída de depuração: números do nosso sistema de eixos, que não
          * significam nada para quem está na loja e competiam por atenção com o corredor, que
          * é a única informação acionável do cartão.
          *
          * No lugar, o convite — porque o cartão sempre foi tocável e nada dizia isso.
          */}
        <span className="modern-location-acao">
          {semPosicao ? 'Toque para informar onde você está' : 'Toque para atualizar'}
        </span>
      </div>
    </div>
  )
}
