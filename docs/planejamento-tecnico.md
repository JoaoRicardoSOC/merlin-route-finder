# Merlin Route Finder — Planejamento Técnico (Backend + Integração)

> Documento de registro do planejamento técnico do backend e da integração, realizado em 17/08/2026, na sequência do alinhamento de negócio registrado em [`contexto-e-planejamento.md`](contexto-e-planejamento.md). Escopo: João, Caio e Claude (backend/integração) — frontend fica com Bielecky/Marcela, banco com Vicentini.

## 1. Contexto

O fator decisivo do planejamento é o prazo: **13/09/2026** é o corte do vídeo "seletiva" (precisa mostrar um MVP funcionando) e a rubrica da Sprint 2 (25 pts) pesa quase tudo em "ter algo funcionando, usável, implantado e bem demonstrado" — não em sofisticação técnica (ver seção 7 de `contexto-e-planejamento.md`). O backlog atual no Trello (25 cards, listados na seção 6 do mesmo documento) foi escrito antes dessa análise e de forma isolada, então este planejamento prioriza e reagrupa esse trabalho em fases, resolve as decisões técnicas que estavam em aberto, e responde ao problema mais urgente: como demonstrar a navegação/rota na banca final (21/09), que é 100% online e sem acesso a uma loja real.

## 2. Decisões tomadas

- **LLM:** Google Gemini (tier gratuito), usado com function calling tanto no chat/RAG quanto na sugestão de substituto por ruptura de estoque (grounding em dados reais de `Produto`/`PontoMapa`, evitando alucinação).
- **Demo da banca final:** simulação animada da caminhada dentro do próprio Mobile PWA — usa as coordenadas reais já calculadas pelo algoritmo de rota para animar um ponto avançando pelos pontos do roteiro numa tela de mapa simples (SVG/canvas), sem depender de bússola/acelerômetro reais. Pode ser gravada ou mostrada ao vivo por compartilhamento de tela. O sistema autoral de posicionamento real (bússola + acelerômetro) fica reservado para o NEXT, que tem ~10 semanas a mais e só importa se o grupo for selecionado na banca.

## 3. Estratégia de priorização (rumo ao MVP de 13/09)

Reagrupamento dos 25 cards do Trello em fases, da mais bloqueante para a mais opcional:

**Fase 0 — Fundacional (não demonstrável sozinha, mas bloqueia tudo):**
- Card 1 (Contrato OpenAPI).
- Cards 2, 3, 4, 6 (entidades puras: Produto, PontoMapa, ItemRoteiro+enums, Sessao, ChatMensagem).
- Card 5 **reescrito** — "Implementação da Entidade ListaRoteiro e Gestão de Itens" (sem "Validação de Limites Estruturais" — carrinho não tem mais teto fixo).
- Cards 11, 12, 13 (mapeamento JPA + adaptadores de persistência hexagonal).

**Fase 1 — Caminho crítico do MVP (essencial até 13/09):**
- Card 7 (inicialização de sessão), Card 8 (busca de produtos — fuzzy search simples para começar), Card 9 (detalhamento de produto/inventário).
- Card 10 **reescrito** — "Orquestração de Gestão de Itens da Lista de Roteiro" (sem "Controle Capacitivo").
- Card 14 (algoritmo Nearest Neighbor — versão funcional básica, sem 2-opt ainda).
- Cards 15, 16 (geração e validação do handoff JWT).
- Cards 21, 22, 23, 24 (endpoints REST correspondentes).

**Fase 2 — Diferenciais de IA (dentro do prazo do MVP, com escopo enxuto se o tempo apertar):**
- Card 18 (cliente HTTP para Gemini).
- Card 19 (motor RAG/chat — versão inicial pode ser um único prompt com contexto do catálogo).
- Card 20 (tratamento de ruptura + substituto via function calling) — **prioridade alta**, é a peça central da demo.
- Card 25 (endpoint do chat).
- Novo item: endpoint interno para zerar `saldo_estoque` de um produto sob demanda, para disparar a ruptura de forma confiável na demo.

**Fase 3 — Robustez/polish (pode ficar para depois de 13/09):**
- Card 17 (cron job de TTL/sessões abandonadas).
- Refinamento 2-opt sobre o Nearest Neighbor.
- Hardening do token de handoff (tirar da query string, caminho de regeneração de QR).

**Fase 4 — Exclusivo do NEXT (fora do escopo até a seleção da banca final):**
- Sistema autoral de posicionamento indoor via bússola + acelerômetro.

## 4. Convenções de arquitetura

> **Contrato de API:** o contrato REST completo (card 1, Fase 0) está em [`../backend/src/main/resources/openapi/openapi.yaml`](../backend/src/main/resources/openapi/openapi.yaml), escrito à mão em OpenAPI 3.0 seguindo o padrão API-First — é a fonte de verdade para as duplas de frontend (Totem e Mobile) integrarem, mesmo antes da implementação dos controllers.
>
> **Decisões técnicas:** o raciocínio por trás de cada escolha não convencional (por que o domínio não usa JPA, por que o UUID é `varchar2`, por que a busca usa query nativa do Oracle, etc.) está registrado em [`decisoes-tecnicas.md`](decisoes-tecnicas.md). O que ainda está **em aberto** — pendências, limitações aceitas e avisos por responsável — fica em [`observacoes.md`](observacoes.md).

O próprio backlog já denuncia uma arquitetura hexagonal com **entidades de domínio puras** separadas do **mapeamento JPA** (cards 2-6 vs. 11-13) — confirma o padrão de portas e adaptadores que o esqueleto de pastas em `backend/src/main/java/br/com/jence/backend` já sugere:

- `domain/entity` — POJOs puros (Sessao, ListaRoteiro, ItemRoteiro, Produto, PontoMapa, ChatMensagem), sem anotações JPA, com os métodos de comportamento do diagrama de classes (`isValida()`, `gerarTokenHandoff()`, `temDisponibilidade()` etc.).
- `domain/exception` — exceções de domínio, capturadas pelo `GlobalExceptionHandler` (`presentation/advice/GlobalExceptionHandler.java`, já existente e reaproveitável).
- `domain/repository` — interfaces (ports) dos repositórios, sem dependência de JPA.
- `domain/factory` — fábricas que garantem invariantes na construção de agregados.
- `application/dto` — DTOs de request/response, alinhados ao contrato OpenAPI (card 1).
- `application/usecase` — um caso de uso por operação de negócio, mapeando quase 1:1 com os cards de "Orquestração".
- `infrastructure/database/repository` + `infrastructure/database/factory` — implementações JPA (`@Entity`) e mapeadores entidade JPA ↔ entidade de domínio.
- `infrastructure/ia/client` + `infrastructure/ia/factory` — cliente HTTP para o Gemini e construção/parsing de prompts e function calls.
- `infrastructure/web` — configuração de `RestClient`/`WebClient` para chamadas de saída (Gemini).
- `infrastructure/config` — `CorsConfig` (já existe), mais `SchedulingConfig` (Fase 3) e configuração do JWT.
- `presentation/controller` — os controllers REST (cards 21-25).
- `presentation/response` / `presentation/advice` — já existem (`StandardError`, `GlobalExceptionHandler`), só estendidos.

## 5. Decisões técnicas para os itens que estavam em aberto

- **JWT do handoff:** adicionar `io.jsonwebtoken:jjwt-api` + `jjwt-impl` + `jjwt-jackson` ao `backend/pom.xml` (não há biblioteca de JWT nem Spring Security no `pom.xml` atual). TTL de 5 min mantido na Fase 1; regeneração de QR e retirada do token da query string ficam na Fase 3.
- **Fuzzy search (card 8):** começar com `LIKE UPPER('%termo%')` (zero infraestrutura) na Fase 1; evoluir depois para `UTL_MATCH.EDIT_DISTANCE`/`JARO_WINKLER_SIMILARITY` (nativos do Oracle) se sobrar tempo — sem precisar de infraestrutura extra.
- **Scheduler de TTL (card 17):** `@Scheduled` + `@EnableScheduling` do próprio Spring — nenhuma dependência nova.
- **Integração com Gemini:** `RestClient` do Spring (já disponível via `spring-boot-starter-webmvc`) chamando a API REST do Gemini com function calling; chave via variável de ambiente (`GEMINI_API_KEY`), nunca hardcoded — mesmo padrão de `DB_URL`/`DB_USER`/`DB_PASSWORD` já usado em `application.yml`.
- **Simulação de caminhada (demo):** não precisa de endpoint novo — a resposta do handoff/validate já devolve a matriz de rota com itens e coordenadas. O Mobile PWA anima a caminhada no cliente; o backend só precisa garantir que a ordem/coordenadas retornadas estejam corretas (responsabilidade do card 14).
- **Simulação de estoque para demo:** pequeno endpoint interno (ex. `PATCH /api/v1/produtos/{id}/estoque`) para zerar `saldo_estoque` sob demanda.

## 6. Verificação

- Rodar o backend localmente (`./mvnw spring-boot:run`) e validar os contratos no Swagger UI (`/swagger.html`).
- Testes unitários para o algoritmo de roteamento e para os casos de uso críticos de handoff.
- Teste manual ponta a ponta via Swagger/Postman seguindo o fluxo do diagrama de sequência: criar sessão → buscar produto → adicionar ao roteiro → concluir (gerar handoff) → validar token → obter a matriz de rota.
- Validar a chamada de function calling do Gemini simulando um produto com `saldo_estoque = 0` e conferindo se o substituto sugerido é fisicamente próximo e existe no catálogo.

## 7. Próximos passos

- Reescrever os cards do Trello conforme esta reorganização em fases — precedido por uma análise de ponta a ponta do backlog revisado (cobertura completa dos casos de uso, brechas, redundâncias) antes da reescrita real dos cards.
- Iniciar a implementação pela Fase 0.
