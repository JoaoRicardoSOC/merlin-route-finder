// Service for Session & Location Code lifecycle (POST /api/v1/sessoes, GET /api/v1/sessoes/{id}, PUT /api/v1/sessoes/{id}/posicao)

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const SESSION_STORAGE_KEY = 'merlin_route_finder_session_id'
const SESSION_DATA_KEY = 'merlin_route_finder_session_data'

// Known physical QR Code plates seeded in backend (CarregadorDadosIniciais.java)
export const KNOWN_PLATES = [
  { codigo: 'ENT-01', nome: 'Entrada Principal da Loja', x: 50, y: 92, setor: 'Entrada' },
  { codigo: 'TIN-02', nome: 'Corredor de Tintas & Vernizes', x: 32, y: 18, setor: 'Tintas' },
  { codigo: 'CEN-03', nome: 'Cruzamento Central da Loja', x: 41, y: 40, setor: 'Geral' },
  { codigo: 'ILU-04', nome: 'Corredor Leste (Iluminação & Lustres)', x: 76, y: 42, setor: 'Iluminação' },
  { codigo: 'FER-05', nome: 'Corredor Oeste (Ferramentas & EPIs)', x: 20, y: 65, setor: 'Ferramentas' },
  { codigo: 'CAI-06', nome: 'Frente de Loja (Antes dos Caixas)', x: 62, y: 80, setor: 'Caixas' }
]

/**
 * Normalizes plate code to canonical format (e.g. "tin-02" -> "TIN02")
 */
export function normalizarCodigo(codigo) {
  if (!codigo) return null
  const canonico = codigo.toUpperCase().replace(/[^A-Z0-9]/g, '')
  return canonico.length > 0 ? canonico : null
}

/**
 * Finds local metadata for a canonical code
 */
function findPlateMeta(codigo) {
  const norm = normalizarCodigo(codigo)
  return KNOWN_PLATES.find(p => normalizarCodigo(p.codigo) === norm) || null
}

/**
 * Generates local fallback session
 */
function createLocalSession(codigoPonto) {
  const plate = findPlateMeta(codigoPonto)
  return {
    id: 'sess-' + Math.random().toString(36).substring(2, 11),
    status: 'ACTIVE',
    criadoEm: new Date().toISOString(),
    expiracaoTtl: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    posicaoAtual: plate ? {
      id: 'ponto-' + plate.codigo,
      tipo: 'QR_CODE',
      corredor: plate.nome,
      coordenadaX: plate.x,
      coordenadaY: plate.y,
      codigoCurto: normalizarCodigo(plate.codigo)
    } : null
  }
}

/**
 * Initializes a new session on backend (POST /api/v1/sessoes)
 */
export async function inicializarSessao(codigoPonto = null) {
  try {
    const payload = codigoPonto ? { codigoPonto: codigoPonto.trim() } : {}
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(payload)
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status} ao inicializar sessão`)
    }

    const data = await response.json()
    localStorage.setItem(SESSION_STORAGE_KEY, data.id)
    localStorage.setItem(SESSION_DATA_KEY, JSON.stringify(data))
    return data
  } catch (err) {
    console.warn('API /sessoes indisponível, criando sessão local:', err.message)
    const localData = createLocalSession(codigoPonto)
    localStorage.setItem(SESSION_STORAGE_KEY, localData.id)
    localStorage.setItem(SESSION_DATA_KEY, JSON.stringify(localData))
    return localData
  }
}

/**
 * Consults current session status (GET /api/v1/sessoes/{id})
 */
export async function consultarSessao(sessaoId) {
  if (!sessaoId) return null
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}`, {
      headers: { 'Accept': 'application/json' }
    })

    if (!response.ok) {
      if (response.status === 404 || response.status === 410) {
        localStorage.removeItem(SESSION_STORAGE_KEY)
        localStorage.removeItem(SESSION_DATA_KEY)
        return null
      }
      throw new Error(`HTTP ${response.status} ao consultar sessão`)
    }

    const data = await response.json()
    localStorage.setItem(SESSION_DATA_KEY, JSON.stringify(data))
    return data
  } catch (err) {
    console.warn('API /sessoes/{id} indisponível, resgatando sessão local:', err.message)
    const saved = localStorage.getItem(SESSION_DATA_KEY)
    return saved ? JSON.parse(saved) : null
  }
}

/**
 * Recenters the user's position by scanning/entering a new plate (PUT /api/v1/sessoes/{id}/posicao)
 */
export async function recentrarPosicao(sessaoId, codigoPonto) {
  if (!codigoPonto) throw new Error('Código do ponto obrigatório')

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/posicao`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify({ codigoPonto: codigoPonto.trim() })
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status} ao recentrar posição`)
    }

    const data = await response.json()
    localStorage.setItem(SESSION_DATA_KEY, JSON.stringify(data))
    return data
  } catch (err) {
    console.warn('API /sessoes/{id}/posicao indisponível, atualizando localmente:', err.message)
    const plate = findPlateMeta(codigoPonto)
    const saved = localStorage.getItem(SESSION_DATA_KEY)
    let session = saved ? JSON.parse(saved) : createLocalSession(codigoPonto)

    session.posicaoAtual = plate ? {
      id: 'ponto-' + plate.codigo,
      tipo: 'QR_CODE',
      corredor: plate.nome,
      coordenadaX: plate.x,
      coordenadaY: plate.y,
      codigoCurto: normalizarCodigo(plate.codigo)
    } : session.posicaoAtual

    localStorage.setItem(SESSION_DATA_KEY, JSON.stringify(session))
    return session
  }
}

/**
 * Gets or recovers the current active session
 * Priority:
 * 1. Code in URL (?ponto=TIN-02 ou ?codigo=TIN-02) -> Initializes or recenters
 * 2. Existing valid session in localStorage -> Resumes
 * 3. Default initialization
 */
export async function obterOuCriarSessao(codigoUrl = null) {
  const storedId = localStorage.getItem(SESSION_STORAGE_KEY)

  if (codigoUrl) {
    if (storedId) {
      // If we already have a session, recenter it with the new plate
      try {
        const updated = await recentrarPosicao(storedId, codigoUrl)
        if (updated && updated.status === 'ACTIVE') return updated
      } catch (e) {
        // Fallback to initializing new if recenter fails
      }
    }
    // No prior session, create with this plate code
    return await inicializarSessao(codigoUrl)
  }

  // No code in URL, try resuming existing session
  if (storedId) {
    const existing = await consultarSessao(storedId)
    if (existing && existing.status === 'ACTIVE') {
      return existing
    }
  }

  // Default new session (e.g. Entrance ENT-01 by default or blank)
  return await inicializarSessao('ENT-01')
}

/**
 * Concludes the active shopping session (POST /api/v1/sessoes/{sessaoId}/concluir)
 */
export async function concluirSessao(sessaoId) {
  if (!sessaoId) {
    limparSessao()
    return { status: 'CONCLUIDA' }
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/concluir`, {
      method: 'POST',
      headers: {
        'Accept': 'application/json'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status} ao concluir sessão`)
    }

    const data = await response.json()
    limparSessao()
    return data
  } catch (e) {
    console.warn('Erro ao concluir sessão no backend, finalizando localmente:', e.message)
    limparSessao()
    return { id: sessaoId, status: 'CONCLUIDA' }
  }
}

/**
 * Clears stored session
 */
export function limparSessao() {
  localStorage.removeItem(SESSION_STORAGE_KEY)
  localStorage.removeItem(SESSION_DATA_KEY)
}

