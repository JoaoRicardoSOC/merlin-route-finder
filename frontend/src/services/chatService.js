// Chat & Virtual AI Assistant Service with Screen Context Awareness (UC-007 a UC-009 / Passo 7)
import { comSessao } from './sessaoViva'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * Teto de espera pela resposta do assistente.
 *
 * Nao havia nenhum, e sem teto um socket pendurado deixa o cliente esperando para sempre —
 * sem resposta, sem erro e sem nada para tocar. O backend tenta o Gemini ate tres vezes antes
 * de devolver o texto de indisponibilidade, e o pior caso medido passou de trinta segundos;
 * por isso 45 s, o mesmo valor da ruptura, e nao menos.
 */
const ESPERA_MAXIMA_MS = 45000

/**
 * Consultar histórico de mensagens da sessão
 * GET /api/v1/sessoes/{sessaoId}/chat/mensagens
 */
export async function consultarHistoricoChat(sessaoId) {
  if (!sessaoId) return []
  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/chat/mensagens`, {
      headers: { 'Accept': 'application/json' }
    })
    if (response.ok) {
      return await response.json()
    }
  } catch (err) {
    console.warn('Erro ao consultar histórico do chat no backend:', err)
  }
  return []
}

/**
 * Enviar mensagem ao assistente virtual com Contexto de Tela
 * POST /api/v1/sessoes/{sessaoId}/chat/mensagens
 */
export async function enviarMensagemChat(sessaoId, conteudo, screenContext = null) {
  if (!conteudo || !conteudo.trim()) return null

  // Construir mensagem contextualizada para o modelo de IA
  let mensagemEnriquecida = conteudo
  if (screenContext) {
    if (screenContext.view === 'product-detail' && screenContext.product) {
      const p = screenContext.product
      const attrsStr = p.atributos && p.atributos.length > 0
        ? p.atributos.map(a => `${a.rotulo || a.chave}: ${a.valor}`).join(', ')
        : ''
      mensagemEnriquecida = `[Contexto da tela: O cliente está visualizando o produto "${p.nome || p.name}" (SKU: ${p.sku}, Corredor: ${p.corredor}, Descrição: ${p.descricao || ''}${attrsStr ? ', Especificações: ' + attrsStr : ''})]. Pergunta do cliente: ${conteudo}`
    } else if (screenContext.selectedSecao && screenContext.selectedSecao !== 'todos') {
      mensagemEnriquecida = `[Contexto da tela: O cliente está navegando na seção de "${screenContext.selectedSecao}"]. Pergunta do cliente: ${conteudo}`
    }
  }

  // Sem sessao nao ha a quem perguntar, e o caminho abaixo ja e o unico honesto.
  if (sessaoId) {
    const relogio = new AbortController()
    const expira = setTimeout(() => relogio.abort(), ESPERA_MAXIMA_MS)
    try {
      /*
       * Por `comSessao`: a sessão vive 4 horas de inatividade, e quando ela vence o servidor
       * recusa a mensagem com 409. Antes isso virava "não consegui falar com a loja" — a frase
       * do servidor fora do ar — e o cliente não tinha como saber que bastava recomeçar. Agora
       * o app abre outra sessão, leva a lista junto e refaz a pergunta.
       */
      const { resposta } = await comSessao(sessaoId, id => fetch(
        `${API_BASE_URL}/api/v1/sessoes/${id}/chat/mensagens`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
          },
          body: JSON.stringify({ conteudo: mensagemEnriquecida }),
          signal: relogio.signal
        }))

      if (resposta.ok) {
        return await resposta.json()
      }

      console.warn('O backend recusou a mensagem de chat. Status:', resposta.status)
    } catch (err) {
      console.warn('Falha na requisição de chat ao backend:', err)
    } finally {
      clearTimeout(expira)
    }
  }

  return assistenteIndisponivel()
}

/**
 * A resposta quando não foi possível falar com a loja.
 *
 * Existia aqui, no lugar dela, um motor de palavras-chave que escrevia a resposta do assistente
 * à mão e a devolvia marcada como `ASSISTANT` — com corredores que não existem na nossa planta
 * ("A12 a A16", "C01 a C03") e produtos tirados da lista que a tela tivesse em mãos, que nesse
 * exato cenário é o catálogo de desenvolvimento. Ele fabricava a funcionalidade principal do
 * projeto justamente quando ela não estava disponível, e a tela não tinha como distinguir.
 *
 * Repare que este texto não é o mesmo do backend: quando o assistente cai mas o servidor
 * responde, quem fala é o backend, e ele diz que o assistente está fora. Aqui a loja inteira
 * não respondeu — são situações diferentes e o cliente precisa saber qual das duas.
 *
 * Sem `produtosRecomendados`: não sabemos nada sobre a pergunta, então não há o que sugerir.
 *
 * O campo `falhou` existe para a tela poder oferecer "tentar de novo" só aqui, e não embaixo
 * de toda resposta. É um campo local, que nunca vem do servidor.
 */
function assistenteIndisponivel() {
  return {
    id: 'local-' + Date.now(),
    falhou: true,
    remetente: 'ASSISTANT',
    conteudo: 'Não consegui falar com a loja agora, então não posso responder sua pergunta. '
      + 'Enquanto isso, você pode procurar o que precisa direto pela busca do catálogo — '
      + 'ou chamar um de nossos vendedores no corredor.',
    enviadoEm: new Date().toISOString()
  }
}
