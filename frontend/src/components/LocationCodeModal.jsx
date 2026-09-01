import React, { useState } from 'react'
import { KNOWN_PLATES, normalizarCodigo } from '../services/sessionService'
import useModalAcessivel from '../hooks/useModalAcessivel'

export default function LocationCodeModal({
  isOpen,
  onClose,
  currentLocation,
  onUpdateLocation
}) {
  const [codigoInput, setCodigoInput] = useState('')
  const [activeMode, setActiveMode] = useState('type') // 'type' | 'scan'
  const [isScanning, setIsScanning] = useState(false)
  const [feedbackError, setFeedbackError] = useState('')

  // Antes do retorno antecipado: hook nao pode ficar atras de um `return`.
  const refModal = useModalAcessivel(isOpen, onClose)

  if (!isOpen) return null

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!codigoInput.trim()) {
      setFeedbackError('Por favor, informe o código da placa.')
      return
    }

    setFeedbackError('')
    onUpdateLocation(codigoInput.trim())
    setCodigoInput('')
    onClose()
  }

  const handleSelectQuickPlate = (plate) => {
    setFeedbackError('')
    onUpdateLocation(plate.codigo)
    onClose()
  }

  const handleSimulateScan = (plate) => {
    setIsScanning(true)
    setTimeout(() => {
      setIsScanning(false)
      onUpdateLocation(plate.codigo)
      onClose()
    }, 900)
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div
        className="location-modal-container"
        onClick={(e) => e.stopPropagation()}
        ref={refModal}
        role="dialog"
        aria-modal="true"
        aria-label="Definir ou Atualizar Localização na Loja"
      >
        <div className="modal-header">
          <div className="modal-title-wrap">
            <div className="location-modal-icon-badge">
              <span className="material-symbols-outlined filled" aria-hidden="true">qr_code_scanner</span>
            </div>
            <div>
              <h3>Localização na Loja</h3>
              <p>Escaneie o QR Code ou digite o código da placa</p>
            </div>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="Fechar modal"
          >
            <span className="material-symbols-outlined" aria-hidden="true">close</span>
          </button>
        </div>

        {/* Mode switcher tabs */}
        <div className="location-mode-tabs">
          <button
            type="button"
            className={`location-mode-tab ${activeMode === 'type' ? 'active' : ''}`}
            onClick={() => setActiveMode('type')}
          >
            <span className="material-symbols-outlined" aria-hidden="true">pin</span>
            <span>Plano B: Digitar Código</span>
          </button>
          <button
            type="button"
            className={`location-mode-tab ${activeMode === 'scan' ? 'active' : ''}`}
            onClick={() => setActiveMode('scan')}
          >
            <span className="material-symbols-outlined" aria-hidden="true">qr_code_2</span>
            <span>Plano A: QR Code</span>
          </button>
        </div>

        <div className="modal-body location-modal-body">
          {activeMode === 'type' ? (
            <div className="type-mode-content">
              <p className="location-mode-description">
                Procure a placa afixada no corredor ou cruzamento mais próximo e digite o código impresso (ex: <strong>TIN-02</strong>, <strong>ENT-01</strong>).
              </p>

              <form onSubmit={handleSubmit} className="location-code-form">
                <div className="location-input-wrap">
                  <span className="material-symbols-outlined location-input-icon" aria-hidden="true">location_on</span>
                  <input
                    type="text"
                    placeholder="Ex: TIN-02, ENT-01, ILU-04..."
                    value={codigoInput}
                    onChange={(e) => {
                      setCodigoInput(e.target.value.toUpperCase())
                      setFeedbackError('')
                    }}
                    className="location-code-input"
                    maxLength={10}
                    autoFocus
                  />
                  {codigoInput && (
                    <button
                      type="button"
                      className="clear-location-btn"
                      onClick={() => setCodigoInput('')}
                    >
                      <span className="material-symbols-outlined" aria-hidden="true">close</span>
                    </button>
                  )}
                </div>

                {feedbackError && (
                  <p className="location-error-msg">
                    <span className="material-symbols-outlined" aria-hidden="true">error</span>
                    {feedbackError}
                  </p>
                )}

                <button type="submit" className="location-submit-btn">
                  <span className="material-symbols-outlined" aria-hidden="true">check_circle</span>
                  <span>Atualizar Minha Posição</span>
                </button>
              </form>

              {/* Quick test plates */}
              <div className="quick-plates-section">
                {/* Numa loja de verdade, oferecer a lista de todas as placas derrotaria o
                    propósito do QR. Ela existe para demonstrar e para o time testar, e o
                    rótulo diz isso — mesma honestidade dos botões "simule a leitura". */}
                <span className="quick-plates-title">Atalho de demonstração — ir direto para uma placa:</span>
                <div className="quick-plates-grid">
                  {KNOWN_PLATES.map((p) => {
                    const isCurrent = currentLocation && normalizarCodigo(currentLocation.code) === normalizarCodigo(p.codigo)

                    return (
                      <button
                        key={p.codigo}
                        type="button"
                        className={`quick-plate-btn ${isCurrent ? 'current' : ''}`}
                        onClick={() => handleSelectQuickPlate(p)}
                      >
                        <div className="plate-badge">{p.codigo}</div>
                        <div className="plate-text">
                          {/* As coordenadas saíram: são os eixos do nosso mapa interno e não
                              significam nada para quem está na loja. O nome do corredor sim. */}
                          <span className="plate-name">{p.nome}</span>
                        </div>
                        {isCurrent && <span className="plate-check">📍 Aqui</span>}
                      </button>
                    )
                  })}
                </div>
              </div>
            </div>
          ) : (
            <div className="scan-mode-content">
              <div className={`qr-camera-sim ${isScanning ? 'scanning' : ''}`}>
                <div className="camera-viewfinder">
                  <div className="viewfinder-corner top-left"></div>
                  <div className="viewfinder-corner top-right"></div>
                  <div className="viewfinder-corner bottom-left"></div>
                  <div className="viewfinder-corner bottom-right"></div>
                  <div className="laser-scan-line"></div>
                  <span className="material-symbols-outlined qr-viewfinder-icon" aria-hidden="true">qr_code_2</span>
                </div>
                {/*
                  * O app NÃO tem leitor de QR — não há vídeo nem canvas aqui, e o visor acima
                  * é enquadramento visual, não câmera ligada.
                  *
                  * E não precisa ter: o QR da placa carrega a URL do app com `?ponto=TIN-02`,
                  * então quem lê é a câmera nativa do celular, e o app abre já posicionado.
                  * O fluxo sempre funcionou; era a frase que mandava apontar uma câmera que
                  * não existe. Ver O-34.
                  */}
                <p className="camera-guide-text">
                  {isScanning
                    ? 'Lendo código da placa...'
                    : 'Use a câmera do seu celular na placa do corredor — ela abre o app já na sua posição.'}
                </p>
              </div>

              <div className="quick-scan-triggers">
                <span className="quick-plates-title">Ou simule a leitura de uma placa física:</span>
                <div className="quick-scan-chips">
                  {KNOWN_PLATES.map((p) => (
                    <button
                      key={p.codigo}
                      type="button"
                      className="scan-chip-btn"
                      onClick={() => handleSimulateScan(p)}
                      disabled={isScanning}
                    >
                      <span className="material-symbols-outlined" aria-hidden="true">qr_code</span>
                      <span>Ler Placa <strong>{p.codigo}</strong></span>
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="modal-footer">
          <button type="button" className="modal-secondary-btn" onClick={onClose}>
            Cancelar
          </button>
        </div>
      </div>
    </div>
  )
}
