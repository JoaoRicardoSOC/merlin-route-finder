# Merlin Route Finder — Backlog do Backend Revisado (para colar no Trello)

> Reescrita completa do backlog de backend (34 cards), organizada nas 4 fases definidas em [`planejamento-tecnico.md`](planejamento-tecnico.md), após a análise de cobertura de ponta a ponta feita em 17/08/2026. Cada card traz uma tag de status: **[NOVO]**, **[REESCRITO]**, **[MODIFICADO]** (descrição ajustada, título mantido) ou **[INALTERADO]** (descrição escrita aqui pela primeira vez, título e escopo mantidos). Prefixo `[BACKEND]` omitido abaixo por brevidade — manter ao colar no Trello.

> [!IMPORTANT]
> **Registro histórico, de 18/08/2026. Metade deste backlog descreve um produto que não existe.**
>
> **Fases 0 e 1** — entidades de domínio, mapeamento JPA, adaptadores, sessão, catálogo, roteiro e os endpoints REST correspondentes: **implementadas**, exceto os cards de handoff e do algoritmo de rota.
>
> **Fases 2 e 3** — **substituídas** por [`backlog-escopo-revisado.md`](backlog-escopo-revisado.md), escrito depois da mentoria de 24/08. Todo card que fale de **totem**, **handoff**, **JWT** ou **cálculo de rota** descreve funcionalidade que foi removida do sistema de propósito.
>
> **Fase 4** — o posicionamento autoral por bússola e acelerômetro continua reservado ao NEXT, e continua não construído.

## Fase 0 — Fundacional

**Definição de Contrato de API (OpenAPI Specification 3.0) - Padrão API-First** `[INALTERADO]`
Especificar, antes de qualquer implementação, o contrato completo da API REST do Merlin Route Finder em OpenAPI 3.0 (via springdoc, já presente no `pom.xml`, exposto em `/swagger.html`), cobrindo todos os recursos previstos nas fases 0 a 3 deste planejamento (sessão, catálogo, roteiro, handoff, ruptura de estoque e chat). O contrato serve como fonte de verdade para as duplas de frontend (Totem e Mobile) integrarem sem depender da implementação estar pronta.

**Criar Entidades Puras Produto e PontoMapa** `[INALTERADO]`
Implementar as entidades de domínio `Produto` e `PontoMapa` como POJOs puros em `domain/entity`, sem qualquer anotação de persistência, incluindo os métodos de comportamento do diagrama de classes (`Produto.temDisponibilidade()`, `PontoMapa.calcularDistanciaPara(PontoMapa)`). Essas entidades não devem conhecer a camada de infraestrutura — o mapeamento para tabelas fica a cargo dos cards de Mapeamento Relacional, abaixo.

**Implementação da Entidade ItemRoteiro e Enumerações de Sistema** `[INALTERADO]`
Implementar a entidade de domínio `ItemRoteiro` (POJO puro) com os campos `ordemCaminho` e `coletado`, e os métodos `marcarComoColetado()` e `definirOrdem(Integer)`. Implementar também as enumerações `StatusSessao` (ACTIVE, COMPLETED, EXPIRED, ABANDONED), `TipoPonto` (PRATELEIRA, BANHEIRO, CAIXA, TOTEM) e `Remetente` (USER, ASSISTANT).

**Implementação da Entidade Sessao e Controle de Ciclo de Vida** `[INALTERADO]`
Implementar a entidade de domínio `Sessao` (POJO puro) com os campos `status`, `criadoEm`, `expiracaoTtl` e os métodos `isValida()`, `encerrar()` e `renovarSessao()`. Observação: `encerrar()` será consumido tanto pela expiração automática (Fase 3, Cron Job) quanto pela orquestração de Conclusão da Rota do mobile (card de UC-014, Fase 1) — considerar ambos os consumidores ao definir a assinatura.

**Implementação da Entidade ListaRoteiro e Gestão de Itens** `[REESCRITO]`
Implementar a entidade de domínio `ListaRoteiro` (POJO puro) com `handoffToken`, `tokenExpiracao` e a coleção `itens`, **sem limite máximo de tamanho** (decisão de negócio confirmada em 17/08/2026 — clientes B2B podem precisar de listas grandes para uma obra inteira). Implementar `adicionarProduto(Produto)`, `removerProduto(UUID)`, `gerarTokenHandoff()`, `isTokenValido()` e `getItensOrdenados()`. Removido do escopo original: qualquer validação de limite estrutural/capacidade máxima de itens.

**Implementação da Entidade ChatMensagem e Gestão de Histórico Conversacional** `[INALTERADO]`
Implementar a entidade de domínio `ChatMensagem` (POJO puro) com `remetente`, `conteudo` e `enviadoEm`, e o método `isDoCliente()`. Cada `Sessao` acumula um histórico de `ChatMensagem`, usado tanto para exibir a conversa ao cliente quanto como contexto para o motor RAG (Fase 2).

**Mapeamento Relacional (JPA/Hibernate) e Repositórios para Produto e PontoMapa** `[INALTERADO]`
Criar as entidades JPA (`@Entity`) correspondentes a `TB_PRODUTO` e `TB_PONTO_MAPA` em `infrastructure/database`, incluindo os mapeadores entre a entidade JPA e a entidade de domínio pura equivalente, e os repositórios Spring Data JPA de baixo nível usados pelos adaptadores de persistência.

**Mapeamento Relacional e Composição de Agregados: Sessao, ListaRoteiro e ItemRoteiro** `[INALTERADO]`
Criar as entidades JPA para `TB_SESSAO`, `TB_LISTA_ROTEIRO`, `TB_ITEM_ROTEIRO` e `TB_CHAT_MENSAGEM`, respeitando a composição de agregados do DER (`Sessao` 1-1 `ListaRoteiro`, `Sessao` 1-N `ChatMensagem`, `ListaRoteiro` 1-N `ItemRoteiro`), e os respectivos mapeadores para as entidades de domínio puras.

**Implementação de Adaptadores de Persistência (Arquitetura Hexagonal)** `[MODIFICADO]`
Implementar as classes adaptadoras que implementam as interfaces de repositório de `domain/repository` (ports), delegando para os repositórios Spring Data JPA e mapeadores dos dois cards de Mapeamento Relacional acima. Esclarecimento de escopo: este card é a camada de "cola" entre o domínio (que não conhece JPA) e a infraestrutura — deve ser feito depois dos cards de Mapeamento Relacional, não em paralelo.

**Massa de Dados de Demonstração (Seed) para Produtos, Pontos de Mapa e Corredores** `[NOVO]`
Criar um mecanismo de carga inicial de dados (ex.: `data.sql` consumido automaticamente pelo Hibernate no startup, já que o projeto usa `ddl-auto: update` e não Flyway) contendo um catálogo mínimo e coerente de produtos, corredores e coordenadas de `PontoMapa`, cobrindo obrigatoriamente: (a) um cenário de rota com pelo menos 5-6 produtos em corredores distintos, suficiente para o algoritmo produzir um caminho não trivial, e (b) pelo menos um produto com estoque zerado e um substituto plausível fisicamente próximo, para exercitar o fluxo de ruptura. Sem este card, nenhuma demonstração tem dados para funcionar.

## Fase 1 — Caminho crítico do MVP (essencial até 13/09)

**Implementação da Orquestração de Inicialização de Sessão** `[INALTERADO]`
Implementar `InicializarSessaoUseCase` (cobre UC-001), acionado quando o cliente inicia interação no Totem, criando uma `Sessao` com `StatusSessao.ACTIVE` e TTL inicial, associada a uma `ListaRoteiro` vazia.

**Implementação da Orquestração de Busca Paginada e Filtragem de Produtos (Fuzzy Search)** `[MODIFICADO]`
Implementar `BuscarProdutosUseCase` (cobre UC-002), com paginação e busca textual tolerante a pequenos erros de digitação. Abordagem técnica definida: usar `LIKE UPPER('%termo%')` via Spring Data JPA para a Fase 1 (zero infraestrutura adicional); evoluir depois, se sobrar tempo, para as funções nativas do Oracle `UTL_MATCH.EDIT_DISTANCE`/`JARO_WINKLER_SIMILARITY`, sem depender de ferramentas externas.

**Orquestração de Consulta de Detalhamento e Resolução Síncrona de Inventário** `[INALTERADO]`
Implementar o caso de uso de consulta detalhada de um `Produto` (cobre UC-003), incluindo a resolução síncrona do `saldoEstoque` atual e do `PontoMapa` onde ele está fisicamente alocado.

**Orquestração de Gestão de Itens da Lista de Roteiro** `[REESCRITO]`
Implementar `AdicionarProdutoAoRoteiroUseCase` (UC-004), `ConsultarListaDeComprasUseCase` (UC-005) e `RemoverProdutoDaListaUseCase` (UC-006), operando sobre a `ListaRoteiro` da sessão ativa. Removido do escopo original: qualquer controle de capacidade máxima de itens.

**Implementação do Algoritmo de Roteamento Espacial (Nearest Neighbor)** `[MODIFICADO]`
Implementar Nearest Neighbor para calcular a ordem de visita dos `PontoMapa` dos itens da `ListaRoteiro` no momento do handoff, minimizando deslocamento. Escopo da Fase 1: versão gulosa pura é suficiente para o MVP; o refinamento 2-opt fica em card separado na Fase 3, para não bloquear o caminho crítico.

**Orquestração de Segurança e Assinatura Criptográfica de Transição (JWT Handoff)** `[MODIFICADO]`
Implementar `GerarHandoffUseCase` (parte do UC-010): valida a sessão, executa o roteamento, gera um JWT assinado com TTL de 5 minutos e uso único (usar `io.jsonwebtoken:jjwt-api`/`jjwt-impl`/`jjwt-jackson`, ainda não presentes no `pom.xml`), e persiste o token e sua expiração na `ListaRoteiro`. O hardening do token (remoção da query string, regeneração de QR) fica em card separado na Fase 3.

**Orquestração de Validação e Consumo de Token Transitivo (Single-Use JWT)** `[MODIFICADO]`
Implementar `ValidarHandoffUseCase` (cobre UC-011): valida o JWT recebido quando o Mobile escaneia o QR, invalida-o imediatamente após uso (uso único), e retorna a matriz de rota, itens e coordenadas. Token expirado ou já usado → 401, conforme o diagrama de sequência.

**Exposição de Endpoints RESTful para Gestão do Ciclo de Vida da Sessão** `[INALTERADO]`
Expor via REST a inicialização de sessão e consultas de status. Não inclui a conclusão de rota do lado mobile — ver card dedicado de UC-014, abaixo.

**Exposição de Endpoints RESTful para Consulta e Detalhamento do Catálogo de Produtos** `[INALTERADO]`
Expor via REST a busca paginada/fuzzy e o detalhamento de produto dos dois cards de orquestração correspondentes.

**Exposição de Endpoints RESTful para Gestão de Itens da Lista de Roteiro (Carrinho Temporário)** `[MODIFICADO]`
Expor via REST a adição, consulta e remoção de itens da `ListaRoteiro` **durante a fase de montagem no Totem**. Esclarecimento de escopo: cobre apenas a montagem da lista (Totem); a marcação de itens como coletados durante a execução da rota (Mobile) tem endpoint próprio — ver card de UC-014.

**Exposição de Endpoints RESTful para Orquestração de Transição de Dispositivos (Handoff Token JWT)** `[INALTERADO]`
Expor via REST a geração do handoff (`POST /api/v1/handoff`) e a validação do token (`GET /api/v1/handoff/validate`), conforme o diagrama de sequência.

**Orquestração e Endpoint de Inclusão de Ponto de Interesse na Rota (UC-012)** `[NOVO]`
Implementar `IncluirPontoDeInteresseUseCase`, acionado pelo Mobile quando o cliente quer adicionar um ponto de apoio (banheiro ou caixa, `TipoPonto.BANHEIRO`/`CAIXA`) ao trajeto em andamento, sem que isso seja um produto do roteiro. Deve localizar o `PontoMapa` do tipo solicitado mais próximo da posição atual do cliente, inserir esse ponto na sequência de navegação sem alterar a ordem dos itens de compra já calculada, e devolver a rota atualizada. Expor via endpoint REST dedicado. Cobre o UC-012, sem nenhuma cobertura no backlog original.

**Orquestração e Endpoint de Marcação de Item Coletado e Conclusão da Rota (UC-014)** `[NOVO]`
Implementar `MarcarItemColetadoUseCase` (chama `ItemRoteiro.marcarComoColetado()` quando o cliente confirma ter pego um produto durante a caminhada) e `ConcluirRotaUseCase` (chama `Sessao.encerrar()` quando todos os itens foram coletados ou o cliente finaliza manualmente, transicionando para `StatusSessao.COMPLETED`). Expor via endpoints REST dedicados. Fecha o UC-014, sem cobertura no backlog original, e é pré-requisito direto da simulação de caminhada da demo da banca final — é o mecanismo que faz o "ponto" avançar visualmente no mapa do Mobile PWA.

**Configuração de Perfis de Ambiente e CORS para Produção** `[NOVO]`
Adicionar um profile Spring (ex. `application-prod.yml`) e atualizar `CorsConfig` (hoje limitado a `localhost:3000`/`5173`) para aceitar as origens reais do Totem e do Mobile PWA publicados, mantendo o profile de desenvolvimento local intacto. Garantir que toda credencial sensível (`DB_URL`, `DB_USER`, `DB_PASSWORD`, `GEMINI_API_KEY`) continue vindo de variável de ambiente também em produção. Pré-requisito direto do card de Deploy abaixo.

**Publicação (Deploy) da API em Ambiente Gratuito** `[NOVO]`
Publicar a API Spring Boot em um provedor gratuito compatível com Java (ex.: Render, Railway ou Fly.io — Vercel, sugerido pela FIAP para o frontend, não hospeda bem uma aplicação Java de longa duração), com conectividade configurada para o Oracle DB da FIAP. O link do deploy é item obrigatório na entrega da Sprint 2 e sua usabilidade vale 4,5 dos 5 pontos do item "Deploy" da rubrica.

## Fase 2 — Diferenciais de IA

**Implementação de Cliente HTTP para Comunicação com API de Modelo de Linguagem (LLM)** `[MODIFICADO]`
Implementar o cliente HTTP (`infrastructure/ia/client`) para a API REST do Google Gemini (tier gratuito, decisão confirmada em 17/08/2026), usando `RestClient` do Spring (já disponível, sem dependência nova). A chave de API deve vir de variável de ambiente (`GEMINI_API_KEY`), nunca hardcoded. O cliente deve suportar function calling/tool use — pré-requisito dos cards de RAG e de Tratamento de Ruptura de Estoque abaixo.

**Orquestração do Motor RAG e Engenharia de Prompt (System Prompt)** `[MODIFICADO]`
Implementar a orquestração do assistente conversacional, incluindo o system prompt que define o escopo do ChatAssistente (conjunto fechado de "projetos"/categorias mapeados a categorias de produto — não um chat totalmente aberto). Escopo da Fase 2: uma versão inicial com um único prompt contendo o contexto relevante do catálogo é aceitável para o MVP; um pipeline completo de embeddings/busca vetorial pode ficar para depois.

**Orquestração de Tratamento de Ruptura de Estoque e Recomendação Semântica Espacial** `[RENOMEADO E MODIFICADO — antes "Webhook Interno"]`
Desenvolver `TratarRupturaEstoqueUseCase`, acionado de forma síncrona quando o cliente aciona o alerta de "Prateleira Vazia" no Mobile (não é um webhook real — o nome original era sugestão de IA e foi corrigido nesta revisão). Deve registrar a ruptura, efetuar uma pré-filtragem espacial no banco buscando itens em corredores adjacentes ao `PontoMapa` do produto em falta, acionar o cliente Gemini via function calling para eleger o substituto semântico mais adequado dentre os candidatos já filtrados pela consulta espacial (nunca por texto livre do modelo), e devolver uma recomendação acionável em tempo real.

**Exposição de Endpoint RESTful para Tratamento de Ruptura de Estoque** `[NOVO]`
Expor via REST o caso de uso `TratarRupturaEstoqueUseCase` do card acima. O único endpoint de IA previsto no backlog original (Chat RAG) cobre apenas o histórico conversacional — o gatilho do botão "Prateleira Vazia" (UC-013) não tinha endpoint dedicado.

**Endpoint Interno de Simulação de Ruptura de Estoque (Ferramenta de Demonstração)** `[NOVO]`
Implementar um endpoint simples (ex.: `PATCH /api/v1/produtos/{produtoId}/estoque`) para zerar ou restaurar o `saldoEstoque` de um produto sob demanda. Não faz parte de nenhum caso de uso do cliente final — é uma ferramenta operacional para garantir que o fluxo de ruptura (UC-013) possa ser disparado de forma confiável durante a gravação do vídeo ou a apresentação ao vivo.

**Exposição de Endpoints RESTful para Interação Conversacional e Gestão do Histórico do Assistente Virtual (Chat RAG)** `[INALTERADO]`
Expor via REST o envio de mensagens do cliente ao ChatAssistente e a consulta do histórico de `ChatMensagem` de uma sessão, consumindo a orquestração RAG.

## Fase 3 — Robustez e polish (pode ficar para depois de 13/09)

**Orquestração de Auditoria de TTL e Invalidação de Sessões Abandonadas (Cron Job)** `[INALTERADO]`
Implementar um job agendado (`@Scheduled` + `@EnableScheduling`, sem dependência nova) que varre periodicamente sessões com `expiracaoTtl` vencido e ainda `ACTIVE`, transicionando-as para `ABANDONED` ou `EXPIRED`.

**Refinamento de Rota com 2-opt sobre o Nearest Neighbor** `[NOVO]`
Adicionar uma passada de refinamento 2-opt sobre a rota gerada pelo Nearest Neighbor, eliminando cruzamentos óbvios de trajeto sem o custo de um solver exato de TSP. Documentar a decisão (heurística construtiva + melhoria local) como justificativa técnica para a banca.

**Hardening do Token de Handoff (Remoção da Query String e Regeneração de QR)** `[NOVO]`
Mover o token de handoff da query string (`GET /handoff/validate?token=...`) para um mecanismo mais seguro (header ou corpo da requisição), reduzindo exposição em histórico de navegador e logs. Implementar um caminho de regeneração de QR quando o token expira, evitando forçar reinício do planejamento do zero — e servindo como rede de segurança para demos ao vivo caso a câmera falhe.

## Fase 4 — Exclusivo do NEXT (não criar no Trello ainda)

**Sistema Autoral de Posicionamento Indoor (Bússola + Acelerômetro)** `[NOVO — fora de escopo até seleção na banca]`
Ainda não deve virar card formal. Só deve ser desenhado e priorizado se o grupo for selecionado para o NEXT (24/10/2026). Objetivo: estimar a posição do cliente ao longo da rota usando apenas bússola (direção) e acelerômetro (contagem de passos) do próprio celular, sem hardware externo.

---

## Resumo por status

| Status | Quantidade |
|---|---|
| Inalterado (descrição nova, escopo igual) | 15 |
| Modificado (escopo/abordagem ajustado) | 8 |
| Reescrito (mudança de conteúdo, título mantido) | 2 |
| Novo | 9 |
| **Total de cards formais (Fases 0-3)** | **34** |
| Fase 4 (não formalizar ainda) | 1 |
