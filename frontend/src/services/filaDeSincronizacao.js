/*
 * A fila do que o cliente fez e o servidor ainda não soube.
 *
 * O problema que ela resolve: marcar um item como coletado atualizava a tela e tentava o
 * PATCH; quando o PATCH falhava, o erro ia para o console e **a marca ficava na tela**. O
 * servidor nunca soube, e na primeira reconciliação — trocar de aba e voltar já basta — a
 * marca sumia sozinha. O cliente marcou, viu marcado, e depois não estava mais.
 *
 * Num corredor de loja isso não é hipótese: é o lugar onde o sinal cai.
 *
 * **Por que uma fila e não um simples "tenta de novo".** O cliente continua andando e
 * marcando enquanto está sem sinal. Uma tentativa isolada por ação perderia todas as outras,
 * e reenviar tudo em paralelo quando o sinal volta inverteria a ordem: desmarcar e marcar de
 * novo chegariam trocados, e o item terminaria no estado errado.
 *
 * **A regra de colisão é a última intenção vence.** Se o cliente marcou, desmarcou e marcou
 * de novo o mesmo item enquanto estava offline, o servidor só precisa saber do último estado
 * — os intermediários não aconteceram para ninguém. Guardar os três e reenviar em sequência
 * daria o mesmo resultado com três vezes mais chances de falhar no meio.
 *
 * A fila é do aparelho, não da sessão: se a aba fechar antes de o sinal voltar, ela continua
 * lá. Ver [D-86].
 */

const CHAVE = 'merlin_route_finder_fila_sincronizacao'

/** Lê a fila gravada. Nunca lança: fila corrompida vira fila vazia. */
export function lerFila() {
  try {
    const cru = localStorage.getItem(CHAVE)
    const fila = cru ? JSON.parse(cru) : []
    return Array.isArray(fila) ? fila : []
  } catch {
    return []
  }
}

function gravar(fila) {
  try {
    if (fila.length === 0) {
      localStorage.removeItem(CHAVE)
    } else {
      localStorage.setItem(CHAVE, JSON.stringify(fila))
    }
  } catch (erro) {
    // Aparelho sem espaço ou em navegação privada. Perder a fila é ruim, mas quebrar a tela
    // no meio de uma compra é pior — e o item continua marcado localmente de qualquer forma.
    console.warn('Não deu para gravar a fila de sincronização:', erro?.message)
  }
}

/**
 * Guarda a intenção de deixar um item num estado de coleta.
 *
 * Substitui qualquer pendência anterior do mesmo item: ver a regra da última intenção no
 * cabeçalho.
 */
export function enfileirarColeta(idBackend, coletado) {
  if (!idBackend) return
  const fila = lerFila().filter(p => p.idBackend !== idBackend)
  fila.push({ idBackend, coletado, em: new Date().toISOString() })
  gravar(fila)
}

/** Quantas ações esperam conexão. A tela usa para avisar em vez de ficar calada. */
export function pendencias() {
  return lerFila().length
}

/** Um item específico tem pendência? A reconciliação usa para não sobrescrever o que ela não sabe. */
export function temPendencia(idBackend) {
  return lerFila().some(p => p.idBackend === idBackend)
}

export function limparFila() {
  gravar([])
}

/**
 * Tenta reenviar tudo, em ordem, e devolve o que aconteceu.
 *
 * **Para no primeiro erro de rede, de propósito.** Se a conexão ainda não voltou, insistir nos
 * outros só gasta bateria e tempo — e mantém a ordem intacta para a próxima tentativa. Já um
 * erro do *servidor* (4xx) tira o item da fila: reenviar para sempre algo que o servidor
 * recusa é uma fila que nunca esvazia.
 *
 * @param {(idBackend: string, coletado: boolean) => Promise<void>} enviar como falar com a API
 */
export async function drenarFila(enviar) {
  const fila = lerFila()
  if (fila.length === 0) {
    return { enviadas: 0, descartadas: 0, restantes: 0 }
  }

  let enviadas = 0
  let descartadas = 0

  for (let i = 0; i < fila.length; i++) {
    const pendencia = fila[i]
    try {
      await enviar(pendencia.idBackend, pendencia.coletado)
      enviadas++
    } catch (erro) {
      if (erro?.recusadoPeloServidor) {
        console.warn('O servidor recusou uma marcação da fila; ela sai da fila:',
          pendencia.idBackend, erro.message)
        descartadas++
        continue
      }
      // Ainda sem conexão: o que sobrou fica para a próxima, na mesma ordem.
      const sobraram = fila.slice(i)
      gravar(sobraram)
      return { enviadas, descartadas, restantes: sobraram.length }
    }
  }

  gravar([])
  return { enviadas, descartadas, restantes: 0 }
}
