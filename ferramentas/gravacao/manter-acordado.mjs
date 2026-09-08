/*
 * Mantém o backend publicado acordado durante a gravação.
 *
 *   node ferramentas/gravacao/manter-acordado.mjs
 *
 * O plano gratuito do Render hiberna o serviço depois de cerca de 15 minutos sem
 * requisição, e acordar leva de 106 a 183 segundos nas medições que temos. Isso
 * aconteceu duas vezes durante o QA de 08/09 -- inclusive no meio dele, o que fez
 * todos os passos falharem ao mesmo tempo e parecerem defeito.
 *
 * ESCOPO: a janela da gravação, e não permanentemente. Manter um serviço gratuito
 * acordado o tempo todo é abusar do plano; ligar durante o ensaio e a tomada, não.
 * O script imprime há quanto tempo está rodando justamente para ninguém esquecer
 * dele ligado.
 *
 * Usa um endpoint de leitura barato -- a lista de seções -- em vez de criar sessão,
 * que sujaria a métrica de carrinho abandonado (ver O-20).
 */

const ALVO = process.env.ALVO
  || 'https://merlin-route-finder-api.onrender.com/api/v1/produtos/secoes'

/* 10 minutos: com folga dentro dos ~15 que o Render espera antes de hibernar. */
const INTERVALO_MS = Number(process.env.INTERVALO_MIN || 10) * 60_000

const inicio = Date.now()
let acertos = 0
let erros = 0

function agora() {
  return new Date().toLocaleTimeString('pt-BR')
}

function duracao() {
  const min = Math.round((Date.now() - inicio) / 60_000)
  return min < 60 ? `${min} min` : `${Math.floor(min / 60)} h ${min % 60} min`
}

async function bater() {
  const t = Date.now()
  try {
    const resposta = await fetch(ALVO, {
      headers: { Accept: 'application/json' },
      signal: AbortSignal.timeout(240_000),
    })
    const ms = Date.now() - t
    acertos++

    /*
     * Passando de 30 s, o servidor estava dormindo: esta requisicao acordou ele, e
     * na proxima vez vale reduzir o intervalo. Dizer isso e o ponto -- um script que
     * so imprime "ok" nao avisa que chegou tarde.
     */
    const aviso = ms > 30_000 ? '   <- estava dormindo; acordou agora' : ''
    console.log(`${agora()}  ${resposta.status}  ${(ms / 1000).toFixed(1)}s`
      + `   (${duracao()} no ar, ${acertos} ok / ${erros} falhas)${aviso}`)
  } catch (erro) {
    erros++
    console.log(`${agora()}  FALHOU: ${erro.message}`
      + `   (${duracao()} no ar, ${acertos} ok / ${erros} falhas)`)
  }
}

console.log(`Mantendo acordado: ${ALVO}`)
console.log(`Uma requisição a cada ${INTERVALO_MS / 60_000} min. Ctrl+C encerra.`)
console.log('')

await bater()
setInterval(bater, INTERVALO_MS)
