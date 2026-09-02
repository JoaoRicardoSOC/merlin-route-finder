/*
 * Servidor estático para as bancadas de traçado da planta.
 *
 * Por que existe, em vez de simplesmente abrir os arquivos:
 *
 * - `file://` não serve. O navegador bloqueia o `localStorage` (SecurityError) e
 *   não carrega o `tracado.js` — as bancadas guardam o desenho no armazenamento
 *   do navegador, então sem origem de verdade elas não funcionam.
 * - O servidor do Vite também não serve, porque a raiz dele é `frontend/` e estas
 *   ferramentas ficam fora, de propósito: elas não são parte do app e não devem
 *   ir para dentro do `dist/`. Liberar `server.fs.allow` seria afrouxar a
 *   configuração do app por causa de uma bancada.
 *
 * Sem dependência nenhuma: só o `node:http` e o `node:fs` que já vêm com o Node.
 *
 *   node ferramentas/planta/servir.mjs
 */
import { createServer } from 'node:http'
import { readFile } from 'node:fs/promises'
import { extname, join, normalize } from 'node:path'
import { fileURLToPath } from 'node:url'

const RAIZ = fileURLToPath(new URL('.', import.meta.url))
const PORTA = Number(process.env.PORTA) || 5180

const TIPOS = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
}

createServer(async (req, res) => {
  const caminho = decodeURIComponent(new URL(req.url, 'http://x').pathname)
  const alvo = normalize(join(RAIZ, caminho === '/' ? '/index.html' : caminho))

  // Não sair da pasta das ferramentas, mesmo com `..` no caminho.
  if (!alvo.startsWith(RAIZ)) {
    res.writeHead(403).end('fora da pasta')
    return
  }

  try {
    const corpo = await readFile(alvo)
    res.writeHead(200, { 'Content-Type': TIPOS[extname(alvo)] || 'application/octet-stream' })
    res.end(corpo)
  } catch {
    res.writeHead(404, { 'Content-Type': 'text/html; charset=utf-8' })
    res.end('<meta charset="utf-8"><p style="font:15px system-ui">Não encontrei esse arquivo. '
      + 'As bancadas são <a href="/__tracar.html">__tracar.html</a>, '
      + '<a href="/__gondolas.html">__gondolas.html</a> e '
      + '<a href="/__render.html">__render.html</a>.')
  }
}).listen(PORTA, () => {
  console.log(`Bancadas da planta em http://localhost:${PORTA}/`)
  console.log('  /__tracar.html    traçar as seções')
  console.log('  /__gondolas.html  traçar as gôndolas')
  console.log('  /__render.html    ver o resultado, com a conferência')
})
