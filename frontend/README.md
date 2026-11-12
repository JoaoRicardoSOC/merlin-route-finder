# Merlin Route Finder — Frontend

Web app para celular do Merlin Route Finder, em React 19 com Vite, sem biblioteca de rotas nem de estado. Publicado na Vercel em [merlin-route-finder.vercel.app](https://merlin-route-finder.vercel.app).

**Documentação do projeto:**
- [`../README.md`](../README.md) — visão geral, links de produção e equipe
- [`../docs/guia-de-avaliacao-mvp.md`](../docs/guia-de-avaliacao-mvp.md) — roteiro passo a passo para testar o app
- [`../docs/fluxo-do-cliente.md`](../docs/fluxo-do-cliente.md) — a jornada do cliente na loja, tela por tela
- [`../docs/decisoes-tecnicas.md`](../docs/decisoes-tecnicas.md) — **por que** cada escolha não convencional foi feita

## Pré-requisitos

- Node.js 20 ou mais novo, com npm
- O backend rodando em `http://localhost:8080` (ver [`../backend/README.md`](../backend/README.md))

## Rodando

```bash
npm install
npm run dev
```

O app abre em `http://localhost:5173`. Para entrar já posicionado num corredor, como faria o QR Code da placa: `http://localhost:5173/?ponto=TIN-02`.

**Como o app chega à API.** Os serviços chamam sempre `/api/...` no próprio domínio. Em desenvolvimento, o proxy do Vite (`vite.config.js`) repassa para o backend local; em produção, o rewrite de `vercel.json` repassa para o Render. O backend não trata CORS, então o app nunca chama o Render direto ([D-91](../docs/decisoes-tecnicas.md#d-91-o-cors-sai-do-backend-a-vercel-repassa-api-para-o-render)).

## Outros comandos

| Comando | O que faz |
|---|---|
| `npm run build` | Gera a versão de produção em `dist/` |
| `npm run preview` | Serve o `dist/` localmente, para conferir a build |
| `npm run lint` | Roda o ESLint; termina sem nenhum problema |

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `src/App.jsx` | Estado da sessão, do roteiro e da navegação entre as telas |
| `src/components/` | Telas e componentes: catálogo, produto, mapa, roteiro, ruptura, chat |
| `src/services/` | Chamadas à API: sessão, catálogo, mapa, roteiro e chat. `filaDeSincronizacao.js` guarda as coletas feitas sem conexão e as reenvia depois |
| `src/hooks/` | `useModalAcessivel`: trava de foco e `Escape` nos modais |
| `src/constants/` e `src/utils/` | Metadados dos setores e formatação de valores |
| `public/` | Ícone do app |
