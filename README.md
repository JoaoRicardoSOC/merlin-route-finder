# Merlin Route Finder

Aplicativo de jornada em loja para a **Leroy Merlin**, feito para o Challenge 2026 da FIAP.
O cliente escaneia a placa de um corredor, monta sua lista de compras, encontra cada produto
na planta da loja e conversa com um assistente que só responde o que consegue verificar no
catálogo.

**No ar:** [merlin-route-finder.vercel.app](https://merlin-route-finder.vercel.app)

> [!IMPORTANT]
> O backend roda no plano gratuito do Render e **hiberna quando ninguém usa**. A primeira
> abertura leva cerca de **três minutos** — medimos 176 s, 180 s e 183 s em dias diferentes.
> O app avisa e mostra um cronômetro enquanto espera; não está travado.

---

## O problema

A FIAP e a Leroy Merlin mapearam três dores na loja física. A que este projeto ataca é o
**"último metro"**: numa loja de até 10.000 m², encontrar o produto exato é difícil, e o
cliente depende de achar um vendedor.

## O que o app faz

| | |
|---|---|
| **Sabe onde você está** | Cada corredor tem uma placa com QR. A câmera do celular abre o app já posicionado. |
| **Monta a lista** | Busca no catálogo por nome, com filtros por seção e característica. |
| **Mostra onde cada item fica** | Na planta real da loja, com as gôndolas desenhadas. Item que o app não sabe localizar **não ganha alfinete** — não saber precisa parecer não saber. |
| **Responde perguntas** | Assistente com Google Gemini, ancorado por *function calling*: ele consulta o catálogo antes de responder, em vez de inventar. |
| **Resolve ruptura** | Produto em falta na prateleira? O app sugere o substituto mais próximo por tipo e atributo. |

### O mapa é a planta de verdade

A planta não foi desenhada "parecida" nem gerada por fórmula: foi **decalcada vértice a
vértice** sobre a planta técnica que a Leroy compartilhou no kickoff — **21 seções, 2
contornos e 212 gôndolas**, com formas de 4, 6 e 8 lados e ocupação variando de 14% a 48%
por seção.

Três tentativas de aproximar falharam antes dessa, e o motivo de cada uma está registrado
na [D-89](docs/decisoes-tecnicas.md#d-89-a-planta-é-decalcada-da-loja-real-não-gerada-por-fórmula). As ferramentas do decalque ficaram em
[`ferramentas/planta/`](ferramentas/planta/).

---

## Rodando localmente

**Precisa de:** Java 21, Maven, Node 20+ e acesso ao Oracle da FIAP.

### Backend

As credenciais entram como variáveis de ambiente, **nunca em arquivo**:

```bash
cd backend
DB_URL=... DB_USER=... DB_PASSWORD=... GEMINI_API_KEY=... ./mvnw spring-boot:run
```

Sobe em `http://localhost:8080`. Swagger em [`/swagger.html`](http://localhost:8080/swagger.html).

> `GEMINI_API_KEY` é opcional: sem ela o assistente responde que está indisponível, em vez
> de inventar. O resto do app funciona.

> A carga inicial roda a cada subida e **reescreve as posições das seções no banco**. Para
> subir sem tocar nos dados, acrescente `--merlin.seed.enabled=false`.

### Frontend

```bash
cd frontend && npm ci && npm run dev
```

Sobe em `http://localhost:5173` e faz proxy de `/api` para a porta 8080.

### Testes

```bash
cd backend && ./mvnw test
```

São 274 testes. **Os de integração ficam de fora por padrão** — exigem Oracle e o perfil
`-Pintegracao`. Isso já deixou passar oito falhas de uma vez; ver
[O-36](docs/observacoes.md#o-36-a-suíte-padrão-não-roda-os-testes-de-integração-e-isso-já-deixou-passar-oito-falhas).

---

## Como o repositório está organizado

```
backend/          Spring Boot 4.1 + Java 21, arquitetura hexagonal
  domain/           entidades e regras, sem framework
  application/      casos de uso e DTOs
  infrastructure/   JPA, Gemini, carga inicial
frontend/         React 19 + Vite 8
  components/       telas e modais
  services/         conversa com a API, planta da loja, fila offline
  hooks/            comportamento reaproveitado
docs/             o porquê de cada decisão, e o que ainda está aberto
ferramentas/      utilitários fora do build (o decalque da planta)
```

O domínio não conhece Spring: entidades e regras são Java puro, e a infraestrutura depende
delas, nunca o contrário. O motivo está na [D-01](docs/decisoes-tecnicas.md#d-01-arquitetura-hexagonal-com-domínio-livre-de-framework).

---

## Documentação

Este README é a porta de entrada. O detalhe está em [`docs/`](docs/):

| Documento | Para quê |
|---|---|
| [`decisoes-tecnicas.md`](docs/decisoes-tecnicas.md) | **Por que** o código está assim — 91 decisões, com o que foi descartado e por quê |
| [`observacoes.md`](docs/observacoes.md) | O que ainda está **aberto**, com dono e urgência |
| [`fluxo-do-cliente.md`](docs/fluxo-do-cliente.md) | A jornada, ação por ação |
| [`quebras-de-fluxo.md`](docs/quebras-de-fluxo.md) | O que acontece quando dá errado, e o que o app faz a respeito |
| [`roteiro-de-qa.md`](docs/roteiro-de-qa.md) | **Verificação do ambiente publicado, para rodar na véspera da gravação** |
| [`deploy.md`](docs/deploy.md) | Publicação no Render, passo a passo |
| [`contexto-e-planejamento.md`](docs/contexto-e-planejamento.md) | O desafio, os prazos e a rubrica |

**A regra que atravessa o projeto:** quando o sistema não sabe, ele diz que não sabe. Foi
preciso escrever isso porque o contrário aconteceu várias vezes — o mapa apontando um
corredor inventado, o catálogo devolvendo produtos que não existem, uma sessão inteira
fabricada quando o servidor não respondia. Cada um desses está documentado com o conserto.

## O que este projeto **não** faz

- **Não integra com o ERP.** O estoque exibido é o do nosso banco.
- **Não calcula rota ótima.** A linha no mapa liga os pontos na ordem da lista.
- **Não tem leitor de QR próprio** — quem lê é a câmera do celular, que abre o app pela URL.
- **Não desenha as gôndolas de Materiais de construção nem da frente de caixas.** A
  organização do pátio não é distinguível na planta técnica, e preencher com prateleira
  imaginária repetiria o problema que o resto do projeto corrige. As duas seções aparecem
  no mapa como área, com o contorno e o nome, e sem prateleira.

## Equipe

| | |
|---|---|
| João Ricardo | Backend |
| Caio | Backend |
| Vicentini | Banco de dados |
| Bielecky | Frontend e design |
| Marcela | Frontend e design |
