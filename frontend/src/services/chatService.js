// Chat & Virtual AI Assistant Service with Screen Context Awareness (UC-007 a UC-009 / Passo 7)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

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
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/sessoes/${sessaoId}/chat/mensagens`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify({ conteudo: mensagemEnriquecida })
      })

      if (response.ok) {
        return await response.json()
      }

      console.warn('O backend recusou a mensagem de chat. Status:', response.status)
    } catch (err) {
      console.warn('Falha na requisição de chat ao backend:', err)
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
 */
function assistenteIndisponivel() {
  return {
    id: 'local-' + Date.now(),
    remetente: 'ASSISTANT',
    conteudo: 'Não consegui falar com a loja agora, então não posso responder sua pergunta. '
      + 'Enquanto isso, você pode procurar o que precisa direto pela busca do catálogo — '
      + 'ou chamar um de nossos vendedores no corredor.',
    enviadoEm: new Date().toISOString()
  }
}
