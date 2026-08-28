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
export async function enviarMensagemChat(sessaoId, conteudo, catalogProducts = [], screenContext = null) {
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
    } catch (err) {
      console.warn('Falha na requisição de chat ao backend, usando motor inteligente com contexto de tela:', err)
    }
  }

  // Fallback inteligente com consciência de contexto de tela
  return fallbackAssistenteIAComContexto(conteudo, catalogProducts, screenContext)
}

/**
 * Motor de Fallback Grounded com Consciência Contextual
 */
function fallbackAssistenteIAComContexto(pergunta, catalogProducts = [], screenContext = null) {
  const p = pergunta.toLowerCase()

  // 1. Contexto de Produto Específico (quando o usuário está na página de produto)
  if (screenContext?.view === 'product-detail' && screenContext.product) {
    const prod = screenContext.product
    const nome = prod.nome || prod.name || 'este produto'
    const desc = prod.descricao || ''
    const corredor = prod.corredor || 'Corredor da Loja'
    const secao = prod.secao || 'Geral'

    let respostaContextual = ''
    if (p.includes('serve') || p.includes('pra que') || p.includes('para que') || p.includes('o que é') || p.includes('funciona') || p.includes('aplicação')) {
      respostaContextual = `O produto **${nome}** ${desc ? 'é ' + desc.toLowerCase() : 'é indicado para uso em reformas e instalações'}. Ele fica localizado no **${corredor}** (Setor de ${secao}).`
    } else if (p.includes('onde') || p.includes('corredor') || p.includes('local') || p.includes('achar')) {
      respostaContextual = `O item **${nome}** está localizado no **${corredor}** da loja. Você pode clicar no botão de rota ou adicionar ao seu roteiro para encontrá-lo!`
    } else if (p.includes('voltagem') || p.includes('especifica') || p.includes('marca') || p.includes('tamanho') || p.includes('medida')) {
      const attrs = prod.atributos?.map(a => `• ${a.rotulo || a.chave}: ${a.valor}`).join('\n') || ''
      respostaContextual = `Aqui estão as especificações técnicas de **${nome}**:\n${attrs || desc}\nEle está disponível no **${corredor}**.`
    } else {
      respostaContextual = `Sobre o item **${nome}** (${corredor}): ${desc ? desc + '.' : 'Excelente opção para o seu projeto.'} Posso te ajudar com os materiais complementares ou ferramentas para sua instalação?`
    }

    return {
      id: 'local-' + Date.now(),
      remetente: 'ASSISTANT',
      conteudo: respostaContextual,
      enviadoEm: new Date().toISOString(),
      produtosRecomendados: [prod]
    }
  }

  // 2. Contexto Geral de Loja e Projetos
  let respostaTexto = ''
  let produtosSugeridos = []

  if (p.includes('pintar') || p.includes('parede') || p.includes('tinta') || p.includes('pincel') || p.includes('rolo')) {
    respostaTexto = 'Para pintar uma parede com acabamento profissional e sem sujeira, você precisará preparar a superfície e aplicar a tinta adequada. Na nossa loja você encontra tudo nos corredores de Tintas (C01 a C03):'
    produtosSugeridos = catalogProducts.filter(item => 
      ['SKU-TIN-001', 'SKU-TIN-002', 'SKU-TIN-004', 'SKU-TIN-006', 'SKU-TIN-008'].includes(item.sku) ||
      (item.secao === 'Tintas' && ['Tinta', 'Rolo', 'Fita', 'Lixa'].some(k => item.nome.includes(k)))
    ).slice(0, 4)
  } else if (p.includes('tomada') || p.includes('fio') || p.includes('eletric') || p.includes('disjuntor') || p.includes('220v') || p.includes('luz') || p.includes('lâmpada')) {
    respostaTexto = 'Para iluminação e instalações elétricas seguras, temos disjuntores, cabos e lâmpadas econômicas na seção de Elétrica e Iluminação (Corredores A12 a A16):'
    produtosSugeridos = catalogProducts.filter(item => 
      item.secao === 'Elétrica' || item.secao === 'Iluminação'
    ).slice(0, 4)
  } else if (p.includes('jardim') || p.includes('planta') || p.includes('vaso') || p.includes('rega')) {
    respostaTexto = 'Para cuidar do seu jardim ou montar um espaço verde aconchegante, separei os materiais essenciais localizados no setor de Jardinagem (Corredores E01 a E03):'
    produtosSugeridos = catalogProducts.filter(item => item.secao === 'Jardim').slice(0, 4)
  } else if (p.includes('furar') || p.includes('ferramenta') || p.includes('parafus') || p.includes('furadeira')) {
    respostaTexto = 'Para montagens e perfurações em alvenaria ou madeira, você encontra ferramentas elétricas de alta performance e fixadores no Corredor A08 a A11:'
    produtosSugeridos = catalogProducts.filter(item => item.secao === 'Ferramentas' || item.secao === 'Ferragens').slice(0, 4)
  } else if (p.includes('pia') || p.includes('cozinha') || p.includes('torneira') || p.includes('sifao') || p.includes('tubo')) {
    respostaTexto = 'Para reformas hidráulicas e instalação de bancadas na cozinha ou banheiro, os materiais indicados estão nos Corredores B04 a B06 e D01 a D04:'
    produtosSugeridos = catalogProducts.filter(item => item.secao === 'Encanamento' || item.secao === 'Cozinhas').slice(0, 4)
  } else {
    respostaTexto = `Entendi sua dúvida sobre "${pergunta}". No catálogo desta loja temos materiais técnicos e ferramentas especializadas. Veja algumas opções diretamente disponíveis nos nossos corredores:`
    produtosSugeridos = catalogProducts.slice(0, 3)
  }

  return {
    id: 'local-' + Date.now(),
    remetente: 'ASSISTANT',
    conteudo: respostaTexto,
    enviadoEm: new Date().toISOString(),
    produtosRecomendados: produtosSugeridos
  }
}
