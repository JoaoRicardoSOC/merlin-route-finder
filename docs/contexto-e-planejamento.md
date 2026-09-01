# Merlin Route Finder — Contexto de Negócio e Planejamento Inicial

> Documento de registro da etapa de alinhamento de contexto (negócio, equipe, metodologia e regras do Challenge) realizada em 17/08/2026, antes do início do planejamento técnico. Serve como fonte única de verdade para decisões já tomadas nesta fase.

> [!IMPORTANT]
> **Registro histórico, de 17/08/2026. Parte dele foi superada pela mentoria de 24/08.** Fica aqui inteiro, e não reescrito, porque o raciocínio que levou até aqui é parte da defesa do projeto — apagar faria parecer que nunca mudamos de ideia.
>
> **O que continua valendo, e não existe em nenhum outro lugar:**
>
> - **Seção 1** — o desafio da FIAP, as três dores mapeadas e os eixos de avaliação. Nada disso mudou.
> - **Seção 5** — a equipe e a divisão de responsabilidades.
> - **Seção 7** — os **prazos oficiais**, a **rubrica de 30 pontos** e a restrição de tecnologia da Fase 7. Esta é a **única fonte** desses dados no repositório.
> - **Seção 4**, em três dos seis pontos: carrinho sem limite de itens, IA ancorada por *function calling* e escopo fechado do assistente. Os três foram implementados como previsto.
>
> **O que caiu, e onde está a verdade de hoje:**
>
> | Seção | O que dizia | Onde ler o que vale |
> |---|---|---|
> | 2 | jornada em dois aparelhos, com totem e handoff por QR | [`fluxo-do-cliente.md`](fluxo-do-cliente.md) |
> | 3 | os diagramas da Sprint 1 como fonte de verdade | desenham o totem, o handoff e a rota — ver O-24 em [`observacoes.md`](observacoes.md) |
> | 4 | refinamento 2-opt e *hardening* do token de handoff | o handoff não existe mais |
> | 6 | backlog de 25 cards | substituído duas vezes, e os dois substitutos já foram concluídos e removidos; o que falta está em [`backlog-fechamento.md`](backlog-fechamento.md) |
> | 8 | demonstração da banca "em aberto" | **resolvida**: simulação animada dentro do próprio app |
> | 9 | cinco perguntas em aberto | **todas respondidas** — Gemini como LLM, Jaro-Winkler na busca, uma credencial Oracle para o time, e a demo acima |
>
> O motivo das duas quedas de raiz — **sem totem** e **sem rota calculada** — está em [`fluxo-do-cliente.md`](fluxo-do-cliente.md) e na D-49 de [`decisoes-tecnicas.md`](decisoes-tecnicas.md#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).

## 1. O desafio (FIAP Challenge 2026)

**Empresa parceira:** Leroy Merlin — maior rede de varejo do Brasil em casa/construção/reforma/bricolagem. Lojas de até 10.000m², grande variedade de produtos técnicos, clientes com níveis de conhecimento muito distintos, forte dependência de vendedores especializados.

**Desafio proposto:** *Experiência em Loja — Otimização da jornada do cliente em loja com IA.* Objetivo: melhorar a jornada do cliente dentro das lojas físicas, facilitando a localização de produtos e o autoatendimento.

**Por que o cliente ainda vai à loja física:** consultoria especializada em projetos complexos, experiência humana, inspiração, retirada imediata de produto, resolução de problemas com pessoas.

**Por que pode deixar de ir:** experiência frustrante, tempo perdido sem valor agregado, inconsistência na jornada, custo/tempo de deslocamento.

**Dores mapeadas pela FIAP/Leroy Merlin:**
1. **Assimetria de informação** — cliente não sabe o que procurar; conhecimento técnico fica com o vendedor.
2. **Problema do "último metro"** — encontrar o produto exato é difícil num layout complexo de corredores/categorias.
3. **Inércia do suporte** — sistemas legados, linguagem técnica, cliente não entende termos → abandono.

**Eixos de avaliação de escopo sugeridos pela FIAP:** User Flow (nova jornada), Stack Tecnológica (quais IAs usar), Viabilidade (integração com estoque em tempo real e identificação de produtos na loja), e o "pulo do gato" (diferencial disruptivo).

## 2. A solução proposta — Merlin Route Finder

**Personas:**
- **B2C** — consumidor final, compra pontual ou pequena reforma, busca agilidade/autonomia.
- **B2B** — empreiteiros/arquitetos/pedreiros, listas de compra específicas, tempo é dinheiro.

**Jornada (dois dispositivos, handoff único):**
- **Fase 1 — Totem:** busca de produtos, "Carrinho de Roteiro" **sem limite fixo de itens** (decisão revista — ver seção 4), assistente conversacional (RAG) que interpreta pedidos abertos (ex.: "o que preciso para pintar uma parede?") e sugere itens, cálculo de rota otimizada (Nearest Neighbor) para coletar os itens sem ziguezague.
- **Handoff:** Totem gera QR Code → cliente escaneia com o próprio celular → abre Web App (PWA, zero instalação) com mapa e rota.
- **Fase 2 — Mobile:** navegação indoor sequencial; em caso de ruptura física na prateleira, o cliente sinaliza e o sistema sugere via IA um substituto fisicamente próximo naquele momento.

**Objetivos estratégicos:** reduzir tempo não produtivo em loja, aumentar ticket médio convertendo rupturas em vendas de substitutos, liberar consultores para vendas consultivas de maior valor.

## 3. Material técnico já produzido (1º semestre)

Diagramas já existentes (fonte de verdade até serem revistos no planejamento técnico):

- **Casos de Uso (UC-001 a UC-014):** ator Cliente (Totem) cobre sessão, busca, detalhe, adicionar/remover item, jornada do ChatAssistente (UC-007–009) e conclusão/handoff; ator Cliente (Mobile) cobre início de navegação, inclusão de ponto de interesse, tratamento de ruptura física e conclusão da rota.
- **C4 (Container):** Totem (React/TypeScript) e Mobile PWA (React/Vite) como containers separados, ambos consumindo a API Backend (Java 21/Spring Boot), que fala JDBC/TCP com Oracle DB e HTTPS com um provedor externo de IA (LLM) para chat e sugestões.
- **Sequência (Handoff):** Totem faz `POST /api/v1/handoff`, backend valida sessão, roda Nearest Neighbor, gera JWT (TTL 5 min, uso único) e retorna URL+token; QR é exibido; celular chama `GET /api/v1/handoff/validate?token=...`; token inválido/expirado → 401 e alerta; token válido → é invalidado (uso único), matriz de rota retornada e cacheada em `sessionStorage`, app pede acesso a bússola/acelerômetro e inicia navegação.
- **Classes:** `Sessao` (1) possui `ListaRoteiro` (1) e `historico_chat` (`ChatMensagem` *); `ListaRoteiro` contém `ItemRoteiro` (cardinalidade original 0..15 — **revista, ver seção 4**); `ItemRoteiro` encapsula 1 `Produto`; `Produto` é alocado em 1 `PontoMapa`. Enums: `StatusSessao` (ACTIVE/COMPLETED/EXPIRED/ABANDONED), `TipoPonto` (PRATELEIRA/BANHEIRO/CAIXA/TOTEM), `Remetente` (USER/ASSISTANT).
- **DER:** `TB_SESSAO`, `TB_LISTA_ROTEIRO` (handoff_token, token_expiracao), `TB_ITEM_ROTEIRO` (ordem_caminho, coletado), `TB_PRODUTO` (sku UK, saldo_estoque, ponto_mapa_id FK), `TB_PONTO_MAPA` (tipo, corredor, coordenada_x/y), `TB_CHAT_MENSAGEM`.

## 4. Decisões de refinamento confirmadas (17/08/2026)

- **Carrinho de roteiro sem limite fixo de itens.** O material original previa um teto de 15 itens (limitação física de um carrinho padrão); decidiu-se remover, pois clientes/profissionais (B2B) podem precisar de listas grandes para uma obra inteira. Impacto: `ListaRoteiro` passa de 0..15 para 0..*; o algoritmo de rota e a UI mobile precisam lidar bem com listas potencialmente longas.
- **Algoritmo de roteamento:** manter Nearest Neighbor como base, com plano de adicionar um refinamento leve de 2-opt (a ser detalhado no planejamento técnico), documentado como decisão justificável perante a banca.
- **Handoff:** hardening planejado — caminho de regeneração de QR quando o token expira (em vez de reiniciar a sessão do zero) e retirar o token da query string (header/body, ou código curto digitável como fallback caso a câmera falhe durante demonstração ao vivo).
- **Sugestão de substituto por IA (ruptura de estoque):** deve ser "tool/function-call grounded" — o LLM aciona uma função que consulta `PontoMapa`/`Produto` reais por proximidade e estoque, em vez de gerar texto livre com nome de produto (evita alucinação).
- **Escopo do ChatAssistente:** deve ser explicitamente limitado (conjunto definido de "projetos"/categorias mapeados a categorias de produto), reduzindo risco de comportamento imprevisível numa demonstração ao vivo.
- **Simulação de estoque para demo:** planejado um mecanismo (endpoint/tela interna) para zerar `saldo_estoque` de um produto sob demanda, permitindo disparar o fluxo de ruptura (UC-013) de forma confiável durante apresentações.
- **Reestruturação de pastas do frontend (rejeitada como decisão do time de backend).** O C4 prevê dois containers de frontend (Totem em TS, Mobile PWA); hoje existe só uma pasta `frontend/` genérica. Essa decisão pertence à dupla de frontend (Bielecky e Marcela), via reuniões e commits próprios — não é escopo do time de backend/integração propor.

## 5. Equipe e responsabilidades

Grupo de 5 integrantes:

| Integrante | Foco principal |
|---|---|
| João Ricardo (usuário) | Backend (apoio a frontend se necessário) |
| Caio | Backend |
| Bielecky | Frontend/Design (apoio a backend se necessário) |
| Marcela | Frontend/Design |
| Vicentini | Banco de dados |

João + Caio + Claude conduzem backend, banco e a camada de integração (backend ↔ BD ↔ frontend). Bielecky e Marcela avançam de forma independente no frontend, coordenando via reuniões de equipe e commits — decisões de estrutura/arquitetura de frontend não são tratadas neste planejamento. O acesso de Claude ao repositório completo (incluindo frontend) existe para dar visão geral e ajudar tudo a se encaixar depois, não para conduzir decisões de frontend.

## 6. Metodologia e backlog atual (Trello)

Metodologia ágil via Trello. Backlog do backend já existe (25 cards, criados **antes** deste alinhamento e de forma isolada uns dos outros — portanto sujeitos a revisão):

1. Definição de Contrato de API (OpenAPI Specification 3.0) - Padrão API-First
2. Criar Entidades Puras Produto e PontoMapa
3. Implementação da Entidade ItemRoteiro e Enumerações de Sistema
4. Implementação da Entidade Sessao e Controle de Ciclo de Vida
5. Implementação da Entidade ListaRoteiro e Validação de Limites Estruturais *(precisa reescrita — carrinho não tem mais limite fixo, ver seção 4)*
6. Implementação da Entidade ChatMensagem e Gestão de Histórico Conversacional
7. Implementação da Orquestração de Inicialização de Sessão
8. Implementação da Orquestração de Busca Paginada e Filtragem de Produtos (Fuzzy Search) *(abordagem técnica ainda em aberto)*
9. Orquestração de Consulta de Detalhamento e Resolução Síncrona de Inventário
10. Orquestração de Gestão de Itens e Controle Capacitivo da Lista de Roteiro *(idem #5 — revisar por conta do limite removido)*
11. Mapeamento Relacional (JPA/Hibernate) e Repositórios para Produto e PontoMapa
12. Mapeamento Relacional e Composição de Agregados: Sessao, ListaRoteiro e ItemRoteiro
13. Implementação de Adaptadores de Persistência (Arquitetura Hexagonal)
14. Implementação do Algoritmo de Roteamento Espacial (Nearest Neighbor)
15. Orquestração de Segurança e Assinatura Criptográfica de Transição (JWT Handoff)
16. Orquestração de Validação e Consumo de Token Transitivo (Single-Use JWT)
17. Orquestração de Auditoria de TTL e Invalidação de Sessões Abandonadas (Cron Job)
18. Implementação de Cliente HTTP para Comunicação com API de Modelo de Linguagem (LLM) *(provedor ainda não escolhido — ver seção 8)*
19. Orquestração do Motor RAG e Engenharia de Prompt (System Prompt)
20. Orquestração de Tratamento de Ruptura de Estoque e Recomendação Semântica Espacial *(nome de origem "Webhook Interno" era sugestão de IA/Gemini e é impreciso — é um caso de uso síncrono comum, `TratarRupturaEstoqueUseCase`, acionado pelo botão "Prateleira Vazia" do mobile, não um webhook real; renomear na revisão do backlog)*
21. Exposição de Endpoints RESTful para Gestão do Ciclo de Vida da Sessão
22. Exposição de Endpoints RESTful para Consulta e Detalhamento do Catálogo de Produtos
23. Exposição de Endpoints RESTful para Gestão de Itens da Lista de Roteiro (Carrinho Temporário)
24. Exposição de Endpoints RESTful para Orquestração de Transição de Dispositivos (Handoff Token JWT)
25. Exposição de Endpoints RESTful para Interação Conversacional e Gestão do Histórico do Assistente Virtual (Chat RAG)

Nenhum card está em andamento — todos no backlog. Não existe nenhum protótipo/POC além do esqueleto já presente no repositório (`CorsConfig`, `GlobalExceptionHandler`, `StandardError`, `application.yml`, demais pastas vazias/`.gitkeep`).

**Banco de dados:** Oracle fornecido pela FIAP; cada um dos 5 integrantes tem credencial/schema individual. Ainda não decidido como o projeto vai consolidar isso (ex.: usar a credencial de uma pessoa fixa no projeto). Vicentini (responsável por banco) ainda não iniciou a modelagem — o DER existente é a única fonte de verdade até o momento.

## 7. Regras oficiais do Challenge (fonte: deck FIAP "2ESOA - Challenge 2026")

**Cronograma (hoje: 17/08/2026):**
- 06/04/2026 — Kickoff (já ocorrido).
- 24/05/2026 — Entrega da Sprint do 1º semestre (vídeo pitch ≤3min, já entregue — 5 pts já garantidos).
- **24/08/2026** — Mentoria com profissionais técnicos da Leroy Merlin (dúvidas gerais).
- **13/09/2026** — Prazo do vídeo "seletiva" (≤5min: ideia + MVP funcionando + proposta de valor). Define quem é selecionado para a 2ª mentoria e a banca final. Critérios: clareza/objetividade, funcionalidade do MVP, alinhamento ao desafio, inovação/impacto.
- **21/09/2026** — Banca Final (se selecionados), para professores + representantes Leroy Merlin. **100% online (curso EaD)** — sem acesso a loja real, sem poder caminhar fisicamente mostrando o app.
- **24/10/2026** — NEXT (feira de tecnologia FIAP), se selecionados na banca: apresentação presencial para alunos/professores/Leroy Merlin, concorrendo a prêmios (1º R$5.000, 2º R$3.000, 3º R$2.000).

**Pontuação do Challenge (30 pts totais):**
- Sprint 1º semestre → 5 pts (já entregue).
- Sprint 2º semestre → 25 pts, dividido em:
  - **Vídeo Pitch (5 pts):** 0,5 duração ≤3min; 0,5 PDF com nomes + link do vídeo + link do deploy; 1,0 criatividade/edição; **3,0 clareza na apresentação da solução e demonstração do projeto**.

> [!NOTE]
> **Os dois limites de duração não se contradizem: são dois vídeos.** Confirmado pelo time.
>
> O de **até 5 minutos**, com prazo em **13/09**, é o da *seletiva* — um portão, que define quem passa para a banca final e, dela, para o NEXT. O de **até 3 minutos** é a *entrega avaliada*, que vale os 5 pontos desta linha e vai ao portal da faculdade no fim do ano, junto do repositório; é o mais básico dos dois.
>
> Os cards de cada um estão em [`backlog-fechamento.md`](backlog-fechamento.md), bloco `[ENTREGA]`.
  - **Deploy (5 pts):** 0,5 link do deploy funcionando (qualquer provedor, ex. Vercel); **4,5 usabilidade/UX do MVP implantado**.
  - **Projeto (15 pts):** **10 pts implementação da solução** (MVP funcional demonstrando a proposta de valor, fluxo básico testável, resolve o problema de forma clara e aplicável); **5 pts usabilidade/UX**.

**Leitura da rubrica:** 22,5 dos 25 pontos do 2º semestre dependem diretamente de "ter algo funcionando, usável, implantado e bem demonstrado" — não de sofisticação algorítmica ou hardening de segurança. Essas melhorias continuam relevantes para a conversa qualitativa com a banca e para o NEXT, mas não devem competir por tempo com entregar um fluxo ponta a ponta funcionando antes de 13/09.

**Restrição de tecnologia:** o material formal diz "somente tecnologias vistas até a Fase 7 podem ser usadas" na entrega da Sprint 2, mas com a ressalva "para a construção do MVP para a Leroy Merlin, pode-se utilizar outras tecnologias." O que já foi coberto em aula no eixo de backend: **Java + Spring Boot, Python**. Orientação confirmada pelo time (17/08/2026): não é necessário se policiar rigidamente quanto a tecnologias adicionais, mas é preciso manter em mente a diferença de escopo entre um projeto corporativo real (produção) e um projeto acadêmico (demonstração da proposta de solução) — ou seja, priorizar o suficiente para demonstrar bem a ideia, sem tentar entregar robustez de nível produção.

**Restrição de orçamento:** zero custo. Nenhum provedor de LLM foi escolhido ainda; a escolha deve ser feita dentro de opções gratuitas/free-tier.

## 8. Estratégia de demonstração — problema em aberto

Dois problemas distintos, com prazos e naturezas diferentes:

- **Banca Final (21/09/2026) — sem solução definida ainda.** Apresentação 100% online, sem acesso a loja real, sem poder caminhar fisicamente com o celular mostrando a navegação acontecendo. O time confirmou (17/08/2026) que **não tem preferência entre demo ao vivo ou gravada** e está aberto a sugestões de como apresentar o funcionamento das rotas remotamente, todos em chamada de casa. Este é o item mais urgente a resolver no planejamento técnico, por ter o prazo mais curto e impactar diretamente itens avaliados da rubrica (seção 7).
- **NEXT (24/10/2026, se selecionados) — ideia inicial existe, não construída.** Usar o próprio mapa físico do evento como "loja simulada": definir pontos de interesse (ex.: do estande do grupo até o estande do 3º ano de Ciência da Computação) e gerar rota otimizada entre eles. Posicionamento planejado como sistema autoral, sem hardware externo — fusão de bússola (direção do celular/cliente) e acelerômetro (contagem de passos/distância) para estimar a posição do cliente ao longo do trajeto. É um item de P&D real, ainda a ser desenhado e implementado.

**Importante:** a solução de posicionamento pensada para o NEXT não deve travar o plano de demonstração da banca final — são relógios diferentes e o segundo só importa se o grupo for selecionado no primeiro.

## 9. Perguntas ainda em aberto para o planejamento técnico

- Consolidação das credenciais Oracle FIAP entre os 5 integrantes (usar uma fixa, ou outra abordagem).
- Escolha do provedor de LLM gratuito/zero-custo.
- Abordagem técnica de fuzzy search (nenhuma decidida ainda — aberto a sugestões).
- Interface entre a modelagem de banco do Vicentini e o mapeamento JPA do time de backend (ainda não iniciada por nenhum dos lados).
- Solução concreta para a demonstração de rotas na banca final online (seção 8).

---
*Próximo passo: planejamento técnico (arquitetura de pastas do backend, ordem de implementação, reorganização dos cards do Trello, e a resposta ao problema da demo da banca final).*
