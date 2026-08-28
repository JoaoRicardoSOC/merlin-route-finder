# Merlin Route Finder — Decisões Técnicas

> Registro das decisões técnicas não convencionais tomadas no backend, com o raciocínio por trás de cada uma. Existe para que qualquer integrante do time — ou um avaliador do projeto — entenda **por que** o código está do jeito que está, sem precisar vasculhar arquivo por arquivo.
>
> Complementa [`contexto-e-planejamento.md`](contexto-e-planejamento.md) (o que o projeto é) e [`planejamento-tecnico.md`](planejamento-tecnico.md) (como o trabalho foi organizado).
>
> O contrário deste documento é [`observacoes.md`](observacoes.md): aqui ficam as decisões **fechadas**; lá, o que ainda está em aberto.
>
> Cada decisão traz: **Contexto** (o problema), **Decisão** (o que foi feito), **Alternativas** (o que foi descartado e por quê), **Consequências** (o que isso custa) e **Onde no código**.

## Índice

**Arquitetura**
- [D-01. Arquitetura hexagonal com domínio livre de framework](#d-01-arquitetura-hexagonal-com-domínio-livre-de-framework)
- [D-02. Casos de uso devolvem DTO, não entidade de domínio](#d-02-casos-de-uso-devolvem-dto-não-entidade-de-domínio)
- [D-03. Tipo de paginação próprio em vez de `Page` do Spring](#d-03-tipo-de-paginação-próprio-em-vez-de-page-do-spring)
- [D-26. Nearest Neighbor como heurística de roteamento](#d-26-nearest-neighbor-como-heurística-de-roteamento)
- [D-43. 2-opt sobre o Nearest Neighbor, na variante de caminho aberto](#d-43-2-opt-sobre-o-nearest-neighbor-na-variante-de-caminho-aberto)
- [D-22. Exceção única para "não encontrado", tratada centralmente](#d-22-exceção-única-para-não-encontrado-tratada-centralmente)
- [D-25. 409 para sessão inativa, e quando é aceitável evoluir o contrato](#d-25-409-para-sessão-inativa-e-quando-é-aceitável-evoluir-o-contrato)
- [D-30. Dois artefatos de documentação de API, com propósitos diferentes](#d-30-dois-artefatos-de-documentação-de-api-com-propósitos-diferentes)
- [D-33. Suíte de testes roda sem banco; integração fica separada por tag](#d-33-suíte-de-testes-roda-sem-banco-integração-fica-separada-por-tag)
- [D-34. Swagger UI permanece exposto em produção](#d-34-swagger-ui-permanece-exposto-em-produção)
- [D-45. O deploy mudou quais avisos do Hibernate importavam](#d-45-o-deploy-mudou-quais-avisos-do-hibernate-importavam)
- [D-46. O pool de conexões é dimensionado pelos limites reais do schema da FIAP](#d-46-o-pool-de-conexões-é-dimensionado-pelos-limites-reais-do-schema-da-fiap)

**Domínio**
- [D-04. Entidades imutáveis por padrão](#d-04-entidades-imutáveis-por-padrão)
- [D-05. Construtor privado com fábricas de criação e de reconstrução](#d-05-construtor-privado-com-fábricas-de-criação-e-de-reconstrução)
- [D-06. `encerrar()` desdobrado em três métodos nomeados por evento](#d-06-encerrar-desdobrado-em-três-métodos-nomeados-por-evento)
- [D-07. `ItemRoteiro.reconstituir` para leitura do banco](#d-07-itemroteiroreconstituir-para-leitura-do-banco)
- [D-08. O domínio registra o token de handoff, nunca o assina](#d-08-o-domínio-registra-o-token-de-handoff-nunca-o-assina)
- [D-35. O cliente de IA falha explicitamente; o fallback é de quem chama](#d-35-o-cliente-de-ia-falha-explicitamente-o-fallback-é-de-quem-chama)
- [D-36. Assistente busca no catálogo por ferramenta, com escopo fechado](#d-36-assistente-busca-no-catálogo-por-ferramenta-com-escopo-fechado)
- [D-37. Escolha do modelo por medição, e o limite do tier gratuito](#d-37-escolha-do-modelo-por-medição-e-o-limite-do-tier-gratuito)
- [D-27. Segredo do JWT por ambiente, com chave aleatória em desenvolvimento](#d-27-segredo-do-jwt-por-ambiente-com-chave-aleatória-em-desenvolvimento)
- [D-28. A rota parte do primeiro ponto do tipo TOTEM](#d-28-a-rota-parte-do-primeiro-ponto-do-tipo-totem)
- [D-29. Uso único do token pela ausência no banco](#d-29-uso-único-do-token-pela-ausência-no-banco)
- [D-44. O token de handoff sai da URL, e o QR Code passa a ser regenerável](#d-44-o-token-de-handoff-sai-da-url-e-o-qr-code-passa-a-ser-regenerável)
- [D-09. Relação com a sessão é unidirecional](#d-09-relação-com-a-sessão-é-unidirecional)

**Persistência**
- [D-10. Entidades JPA espelho, separadas das de domínio](#d-10-entidades-jpa-espelho-separadas-das-de-domínio)
- [D-11. UUID gravado como `varchar2(36)`, não `raw(16)`](#d-11-uuid-gravado-como-varchar236-não-raw16)
- [D-12. `sessao_id` mapeado duas vezes na mesma entidade](#d-12-sessao_id-mapeado-duas-vezes-na-mesma-entidade)
- [D-13. Cascata e remoção de órfãos no agregado da lista](#d-13-cascata-e-remoção-de-órfãos-no-agregado-da-lista)
- [D-48. Gravar pelo agregado é seguro aqui — e a investigação que provou isso](#d-48-gravar-pelo-agregado-é-seguro-aqui--e-a-investigação-que-provou-isso-depois-de-duas-hipóteses-erradas)
- [D-14. Salvar item carrega a entidade gerenciada em vez de remapear](#d-14-salvar-item-carrega-a-entidade-gerenciada-em-vez-de-remapear)
- [D-15. Query nativa com `UTL_MATCH` para busca tolerante a erro de digitação](#d-15-query-nativa-com-utl_match-para-busca-tolerante-a-erro-de-digitação)
- [D-16. Carga inicial em Java em vez de SQL](#d-16-carga-inicial-em-java-em-vez-de-sql)
- [D-47. A massa ganhou pares de substituição, e a carga passou a ser incremental](#d-47-a-massa-ganhou-pares-de-substituição-e-a-carga-passou-a-ser-incremental)

**Produto e negócio**
- [D-17. Carrinho de roteiro sem limite de itens](#d-17-carrinho-de-roteiro-sem-limite-de-itens)
- [D-18. Produto duplicado é ignorado no carrinho](#d-18-produto-duplicado-é-ignorado-no-carrinho)
- [D-19. Coordenadas do seed seguem a planta real da loja](#d-19-coordenadas-do-seed-seguem-a-planta-real-da-loja)
- [D-31. Ponto de interesse não é persistido](#d-31-ponto-de-interesse-não-é-persistido)
- [D-24. TTL da sessão é renovado a cada interação](#d-24-ttl-da-sessão-é-renovado-a-cada-interação)
- [D-32. Coletar item passa pela raiz do agregado e não encerra a jornada](#d-32-coletar-item-passa-pela-raiz-do-agregado-e-não-encerra-a-jornada)
- [D-20. Google Gemini como provedor de LLM](#d-20-google-gemini-como-provedor-de-llm)
- [D-21. Demo da banca por simulação animada, não posicionamento real](#d-21-demo-da-banca-por-simulação-animada-não-posicionamento-real)
- [D-23. "Resolução síncrona de inventário" não é integração com ERP](#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp)
- [D-49. O escopo revisado retirou o totem e a rota calculada](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada)
- [D-50. Sem rota, a lista passa a ser agrupada por seção](#d-50-sem-rota-a-lista-passa-a-ser-agrupada-por-seção)
- [D-51. Um valor de enum removido precisa sumir também do banco](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco)
- [D-52. O código curto do QR Code é normalizado na gravação, não só na busca](#d-52-o-código-curto-do-qr-code-é-normalizado-na-gravação-não-só-na-busca)
- [D-53. A aplicação repara a restrição de enum que o `ddl-auto: update` deixa envelhecer](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer)
- [D-54. A entrada aceita o código da placa num campo só, e código desconhecido não recusa a sessão](#d-54-a-entrada-aceita-o-código-da-placa-num-campo-só-e-código-desconhecido-não-recusa-a-sessão)
- [D-55. A posição do cliente vem de duas pistas, e vale a mais recente](#d-55-a-posição-do-cliente-vem-de-duas-pistas-e-vale-a-mais-recente)
- [D-56. A coluna `coletado` continua sendo gravada, mesmo redundante](#d-56-a-coluna-coletado-continua-sendo-gravada-mesmo-redundante)
- [D-57. O mesmo código inválido é aceito na entrada e recusado no recentrar](#d-57-o-mesmo-código-inválido-é-aceito-na-entrada-e-recusado-no-recentrar)
- [D-58. A planta da loja não vive no banco, e é dela que saem as coordenadas das seções](#d-58-a-planta-da-loja-não-vive-no-banco-e-é-dela-que-saem-as-coordenadas-das-seções)
- [D-59. A carga completa a apresentação de produtos que já estavam gravados](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados)
- [D-60. Uma consulta montada substitui as duas buscas de catálogo](#d-60-uma-consulta-montada-substitui-as-duas-buscas-de-catálogo)
- [D-61. As seções do menu saem do catálogo, não da planta](#d-61-as-seções-do-menu-saem-do-catálogo-não-da-planta)
- [D-62. As características dos produtos vivem numa tabela, não em colunas](#d-62-as-caracteristicas-dos-produtos-vivem-numa-tabela-nao-em-colunas)
- [D-63. As facetas ignoram a escolha do cliente sobre elas mesmas](#d-63-as-facetas-ignoram-a-escolha-do-cliente-sobre-elas-mesmas)
- [D-64. Desmarcar um item não precisa mexer na posição do cliente](#d-64-desmarcar-um-item-não-precisa-mexer-na-posição-do-cliente)
- [D-65. Aceitar o substituto é uma ação só, e o substituto entra não coletado](#d-65-aceitar-o-substituto-e-uma-acao-so-e-o-substituto-entra-nao-coletado)
- [D-66. Cada produto da massa é declarado uma vez, inteiro](#d-66-cada-produto-da-massa-e-declarado-uma-vez-inteiro)
- [D-67. O teto de candidatos da ruptura envelheceu com o catálogo](#d-67-o-teto-de-candidatos-da-ruptura-envelheceu-com-o-catalogo)
- [D-68. O substituto é escolhido por semelhança antes de proximidade](#d-68-o-substituto-é-escolhido-por-semelhança-antes-de-proximidade)
- [D-69. A massa passou a ser a fonte do nome e da descrição, e sobrescreve o banco](#d-69-a-massa-passou-a-ser-a-fonte-do-nome-e-da-descrição-e-sobrescreve-o-banco)
- [D-70. Renomear uma seção é migração, não edição de string](#d-70-renomear-uma-seção-é-migração-não-edição-de-string)
- [D-71. O corredor viaja na listagem, e não só o id do ponto](#d-71-o-corredor-viaja-na-listagem-e-não-só-o-id-do-ponto)
- [D-72. O produto da ruptura nasce com estoque](#d-72-o-produto-da-ruptura-nasce-com-estoque)
- [D-38. Ruptura de estoque: o modelo escolhe, mas quem responde é o banco](#d-38-ruptura-de-estoque-o-modelo-escolhe-mas-quem-responde-é-o-banco)
- [D-39. A ruptura vira registro no banco, e o relato não altera o estoque](#d-39-a-ruptura-vira-registro-no-banco-e-o-relato-não-altera-o-estoque)
- [D-40. Existe um endpoint que só serve à demonstração, e ele é assumidamente desprotegido](#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido)
- [D-41. Sessão encerrada continua legível, mas não gravável](#d-41-sessão-encerrada-continua-legível-mas-não-gravável)
- [D-42. A varredura de TTL distingue carrinho abandonado de quem só encostou no totem](#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem)

---

## Arquitetura

### D-01. Arquitetura hexagonal com domínio livre de framework

**Contexto.** O caminho comum em Spring Boot é anotar a entidade de negócio com `@Entity` e usá-la em todas as camadas. É mais rápido de escrever, mas amarra as regras de negócio ao Hibernate: a entidade precisa de construtor vazio, de setters e de campos mutáveis para o framework popular por reflexão — exatamente o oposto do que uma entidade com invariantes deveria permitir.

**Decisão.** Duas hierarquias separadas. As entidades de domínio (`domain/entity`) são POJOs puros, sem nenhuma anotação, com métodos de comportamento e imutabilidade onde faz sentido. A persistência tem classes espelho próprias (`infrastructure/database/entity`), e mapeadores traduzem entre os dois mundos.

O domínio também não conhece Spring: os repositórios são interfaces declaradas em `domain/repository` (ports), implementadas em `infrastructure/database/adapter` (adapters).

**Alternativas.** Entidade única anotada com JPA — descartada porque forçaria abrir mão da imutabilidade e das invariantes protegidas, e faria uma mudança de esquema de banco virar mudança de regra de negócio.

**Consequências.** Mais arquivos e um mapeamento a manter para cada entidade. Em troca, o domínio é testável sem subir contexto Spring nem banco, e mudanças no banco não vazam para as regras.

**Verificação objetiva.** O comando abaixo deve sempre retornar vazio:
```bash
grep -rn "org.springframework\|jakarta.persistence\|org.hibernate" backend/src/main/java/br/com/jence/backend/domain/
```

**Onde no código.** `domain/entity/`, `domain/repository/`, `infrastructure/database/`.

---

### D-02. Casos de uso devolvem DTO, não entidade de domínio

**Contexto.** Um caso de uso poderia devolver a entidade de domínio e deixar o Jackson serializá-la direto na resposta HTTP.

**Decisão.** Cada caso de uso devolve um `record` de `application/dto`, com os campos correspondentes ao schema do contrato OpenAPI. A conversão fica num método estático `de(...)` no próprio DTO.

**Alternativas.** Devolver a entidade — descartada porque acoplaria o JSON público ao modelo interno: renomear um campo do domínio, ou adicionar um getter, mudaria o contrato da API sem ninguém perceber, quebrando o frontend.

**Consequências.** Um DTO a mais por operação. Em troca, o contrato da API é explícito e verificável contra o `openapi.yaml`.

**Onde no código.** `application/dto/SessaoResponse.java`, `ProdutoResponse.java`, `PaginaResponse.java`.

---

### D-03. Tipo de paginação próprio em vez de `Page` do Spring

**Contexto.** A busca paginada de produtos precisa devolver conteúdo mais metadados (página atual, total). O tipo natural em Spring seria `Page<T>`.

**Decisão.** Um `record Pagina<T>` declarado em `domain/repository`. O adaptador traduz o `Page` do Spring para ele, num ponto só.

**Alternativas.** Usar `Page`/`Pageable` do Spring nas interfaces de repositório — descartada porque faria o pacote `domain` importar `org.springframework.data`, contradizendo a D-01. Devolver apenas `List` — descartada porque perderia o total de registros, e o Totem não teria como exibir "página 2 de 3".

**Consequências.** ~15 linhas de código e uma tradução no adaptador.

**Onde no código.** `domain/repository/Pagina.java`, `infrastructure/database/adapter/ProdutoRepositoryAdapter.java`.

---

### D-22. Exceção única para "não encontrado", tratada centralmente

**Contexto.** Vários casos de uso recebem um identificador e podem não encontrar o recurso (produto, sessão, lista, item). O contrato manda responder 404.

**Decisão.** Uma `RecursoNaoEncontradoException(recurso, identificador)` em `domain/exception`, lançada pelo caso de uso, capturada pelo `GlobalExceptionHandler` e convertida em 404 com o `StandardError` padrão do projeto.

**Alternativas.** Cada caso de uso devolver `Optional` e cada controller decidir o status — descartada porque espalharia a mesma decisão por ~8 controllers, e bastaria um esquecer para a API responder 200 com corpo vazio em vez de 404.

Uma classe de exceção por entidade (`ProdutoNaoEncontradoException`, `SessaoNaoEncontradaException`, ...) — descartada porque seis classes que diferem apenas no nome são repetição, não design. A mensagem carrega o contexto: `"Produto nao encontrado(a): <uuid>"`.

**Consequências.** Nenhum controller precisa tratar ausência de recurso: basta o caso de uso lançar. Se no futuro alguma resposta 404 precisar de corpo diferente, é um `@ExceptionHandler` novo, não uma mudança espalhada.

**Onde no código.** `domain/exception/RecursoNaoEncontradoException.java`, `presentation/advice/GlobalExceptionHandler.java`.

---

### D-25. 409 para sessão inativa, e quando é aceitável evoluir o contrato

**Contexto.** Adicionar ou remover produto do roteiro de uma sessão já encerrada ou expirada precisa falhar. Mas 404 seria mentira: a sessão existe, o que impede a operação é o **estado** dela.

**Decisão.** Uma segunda exceção de domínio, `OperacaoNaoPermitidaException`, mapeada para HTTP 409 no `GlobalExceptionHandler`. O contrato OpenAPI ganhou uma resposta reutilizável `SessaoInativa` aplicada aos três endpoints de roteiro.

A separação entre as duas exceções é semântica e vale registrar: **404 = o recurso não existe; 409 = existe, mas o estado atual não permite**. A distinção importa para o frontend, que deve reagir de formas diferentes (404: erro de navegação; 409: sessão precisa ser reiniciada no Totem).

**Política sobre o contrato.** O contrato é fonte de verdade (padrão API-First), mas é um documento vivo. O critério adotado:
- **Adicionar** uma resposta de erro ou um campo opcional é aceitável — não quebra quem já integra.
- **Mudar ou remover** um campo existente exige combinar com a dupla de frontend antes.

Foi por esse critério que no UC-003 optamos por **não** adicionar um campo `disponivel` ao `ProdutoDetalhado` (mudança sem necessidade real), mas aqui adicionamos o 409 (sem ele, o frontend receberia um status não documentado e não saberia tratar).

**Onde no código.** `domain/exception/OperacaoNaoPermitidaException.java`, `presentation/advice/GlobalExceptionHandler.java`, `backend/src/main/resources/openapi/openapi.yaml`.

---

### D-30. Dois artefatos de documentação de API, com propósitos diferentes

**Contexto.** O projeto tem um contrato OpenAPI escrito à mão (padrão API-First, card 1) **e** o springdoc, que gera documentação automaticamente a partir dos controllers. Ter duas fontes parece redundante e convida à divergência.

**Decisão.** Manter os dois, com papéis distintos e explícitos:

| Artefato | O que é | Para quê |
|---|---|---|
| `openapi/openapi.yaml` | escrito à mão | **o que prometemos** — fonte de verdade do design, base da integração do frontend |
| `/swagger.html` (springdoc) | gerado do código | **o que o código faz** — permite testar endpoints interativamente |

**Como isso é útil, e não apenas duplicado.** Comparar os dois é o mecanismo de detecção de divergência: se o gerado não bate com o escrito à mão, ou o código saiu do combinado, ou o contrato mudou sem ninguém avisar. Em ambos os casos, é conversa a ter — e a regra do card 1 permanece: **ajusta-se o código, não o contrato**, a menos que a mudança passe pela política da D-25.

**Alternativas.** Servir o YAML estático dentro do Swagger UI, eliminando o gerado — descartada porque o frontend passaria a testar contra o que foi prometido, sem enxergar o que o código realmente responde. Reproduzir o contrato inteiro em anotações nos controllers — descartada por dobrar a manutenção do mesmo conteúdo.

**Consequência prática.** Os controllers levam apenas `@Tag` e `@Operation`, o suficiente para a documentação gerada ficar legível.

**Onde no código.** `presentation/controller/`, `backend/src/main/resources/openapi/openapi.yaml`.

---

### D-33. Suíte de testes roda sem banco; integração fica separada por tag

**Contexto.** O `BackendApplicationTests`, herdado do esqueleto inicial, sobe o contexto Spring inteiro — inclusive a conexão com o Oracle. Com ele na execução padrão, `./mvnw test` **falhava** na máquina de quem ainda não tinha credencial configurada, e inviabilizaria qualquer CI.

**Decisão.** Testes que exigem banco são marcados com `@Tag("integracao")` e excluídos por padrão pelo Surefire. Um profile Maven os reativa:

```bash
./mvnw test              # 52 testes, sem banco, em qualquer maquina
./mvnw test -Pintegracao # 174 testes, exige DB_URL/DB_USER/DB_PASSWORD
```

**Motivo.** Um clone novo do repositório precisa passar nos testes sem configuração prévia. Se a suíte padrão exige credencial de um banco específico, ela deixa de ser rede de proteção e vira obstáculo: as pessoas param de rodá-la, ou pior, aprendem a ignorar build vermelho.

A separação também deixa explícita uma distinção real: os testes da execução padrão verificam **lógica** (domínio, algoritmo, camada web com casos de uso simulados) e não deveriam depender de infraestrutura; os de integração verificam **a costura com o banco**, e aí a dependência é legítima.

**Três níveis, com papéis distintos** — a suíte cresceu e a divisão ficou mais nítida:

| Nível | Como roda | O que pega |
|---|---|---|
| Unidade e `@WebMvcTest` | sem banco | regra de negócio, status HTTP, validação, tratamento de erro |
| Integração por caso de uso | com Oracle | a costura com o banco: consultas nativas, transação, agregado |
| **Jornada por HTTP** | com Oracle, aplicação de pé | os passos **entre si**: serialização, ordem, estado que atravessa requisições |

O terceiro nível existe porque os dois primeiros são cegos ao que acontece **entre** os passos. Nenhum teste de unidade percebe que o campo devolvido pelo handoff não é o que o passo seguinte espera receber — e é exatamente esse tipo de desencontro que trava uma integração de frontend.

**Por que a jornada é um método só, e não vários.** Os passos dependem uns dos outros: não existe "validar handoff" sem antes montar o roteiro e gerar o token. Quebrar em métodos independentes obrigaria cada um a refazer o percurso inteiro no preparo, o que multiplica o custo e esconde justamente a sequência que se quer proteger. O método imprime cada etapa, então quando falha o log mostra até onde a jornada chegou.

**A jornada não exige `GEMINI_API_KEY`.** O passo da ruptura aceita tanto a escolha do assistente quanto o substituto por proximidade — o fallback da [D-38](#d-38-ruptura-de-estoque-o-modelo-escolhe-mas-quem-responde-é-o-banco). Um teste que só roda para quem tem chave de IA acaba não sendo rodado por ninguém.

**Consequência prática.** Rodar `./mvnw test` antes de commitar passou a ser viável para os cinco integrantes, independente de quem configurou o quê.

**Onde no código.** `backend/pom.xml` (Surefire e profile), `BackendApplicationTests.java`, `PerfilProducaoTest.java`.

---

### D-34. Swagger UI permanece exposto em produção

**Contexto.** A prática usual é desabilitar documentação interativa de API em produção: ela revela a superfície de ataque e facilita exploração.

**Decisão.** Manter `/swagger.html` acessível no ambiente publicado.

**Motivo.** O "produção" aqui é um deploy acadêmico de demonstração, e a rubrica avalia o MVP publicado. Poder abrir a API na banca e mostrar os endpoints funcionando é ganho concreto de apresentação. Do outro lado da balança: não há dado real de cliente, não há endpoint destrutivo, e o conteúdo já é público no repositório.

**Quando esta decisão deveria ser revista.** Se o projeto for adiante com dados reais, integração com sistemas da Leroy Merlin, ou qualquer endpoint administrativo — aí o cálculo inverte e a documentação deve ser restrita a ambientes internos.

**Onde no código.** `src/main/resources/application-prod.yml` (ausência deliberada de restrição).

---

### D-26. Nearest Neighbor como heurística de roteamento

> [!NOTE]
> **Superada em 25/08/2026.** A rota calculada foi removida: a orientação da Leroy é mostrar apenas onde o cliente está e onde está cada produto, deixando o caminho por conta dele. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque descreve a análise que sustentou a escolha — reconhecer o problema como uma instância do TSP, e recusar tanto a força bruta quanto uma biblioteca de otimização por desproporção ao ganho.

**Contexto.** Ordenar as paradas de uma lista de compras para minimizar o percurso é uma instância do Problema do Caixeiro Viajante (TSP), que é NP-difícil: não existe algoritmo exato eficiente conhecido para o caso geral.

**Decisão.** Heurística do vizinho mais próximo (Nearest Neighbor): a partir do ponto atual, sempre seguir para o item ainda não visitado que estiver mais perto. Complexidade O(n²).

**Alternativas.**
- **Solver exato (força bruta ou programação dinâmica)** — descartada: força bruta é O(n!), inviável já com ~12 itens, e o carrinho não tem limite (D-17). Além disso, a diferença entre a rota ótima e uma boa aproximação é irrelevante para um cliente andando numa loja.
- **Bibliotecas de otimização (OR-Tools e afins)** — descartada por adicionar dependência pesada para um ganho marginal no contexto do projeto.

**Consequências assumidas.** Nearest Neighbor é guloso: decide o melhor passo imediato sem enxergar o todo, então pode "se pintar num canto" — deixar um item isolado para o fim e obrigar uma travessia longa. Não produz a rota ótima, e sim uma rota boa.

Medido com as coordenadas reais da loja (6 itens, cenário de reforma de banheiro), a heurística sozinha entregava **197 unidades contra 320 da ordem em que o cliente adicionou os itens — 38,6% de redução**.

**A limitação acima foi atacada na Fase 3.** O refinamento 2-opt entrou como segunda etapa e desfaz os cruzamentos que a heurística gulosa deixa para trás, sem o custo de um solver exato: o mesmo cenário passou a **189,2 unidades, 41,0% de redução**. Ver [D-43](#d-43-2-opt-sobre-o-nearest-neighbor-na-variante-de-caminho-aberto).

**A origem é parâmetro, não constante.** `calcularRota(origem, itens)` — no handoff a origem é o totem da entrada; no tratamento de ruptura de estoque (UC-013) será a posição onde o cliente está naquele momento. Com origem fixa internamente, o segundo caso exigiria outro algoritmo.

**Efeito emergente útil.** Itens que dividem o mesmo `PontoMapa` ficam a distância zero entre si, então a heurística os agrupa naturalmente: o cliente resolve o corredor inteiro de uma vez em vez de voltar nele depois. Não foi programado explicitamente — cai fora da própria regra do vizinho mais próximo.

**Testes versionados.** Por ser domínio puro, é a primeira parte do sistema com testes no repositório: rodam com `./mvnw test` sem banco nem configuração, em ~0,2s.

**Onde no código.** `domain/service/CalculadoraRota.java`, `src/test/java/.../domain/service/CalculadoraRotaTest.java`.

---

## Domínio

### D-04. Entidades imutáveis por padrão

**Contexto.** Entidades com setters públicos permitem que qualquer parte do código altere o estado, contornando as regras de negócio.

**Decisão.** Campos `final` e apenas `@Getter` por padrão. Mutação só existe onde o negócio exige, e sempre por um método com nome de evento (`marcarComoColetado()`, `definirOrdem()`, `encerrar()`), nunca por setter genérico.

`ChatMensagem` é totalmente imutável — uma mensagem enviada é um fato histórico. Isso também é o que torna o histórico confiável como contexto do motor RAG: nada consegue reescrever o passado da conversa.

**Consequências.** Reconstruir uma entidade a partir do banco exige uma via própria (ver D-05 e D-07).

**Onde no código.** Todo o pacote `domain/entity/`.

---

### D-05. Construtor privado com fábricas de criação e de reconstrução

**Contexto.** Uma entidade nasce de dois jeitos muito diferentes: criada agora pela aplicação (regras de negócio devem valer — sessão nova é sempre `ACTIVE` com TTL a partir de agora), ou reconstruída a partir de uma linha do banco (pode estar `COMPLETED`, com datas antigas).

**Decisão.** Construtor privado e duas fábricas estáticas com nomes que revelam a intenção:
- `Sessao.iniciar(id)` / `ListaRoteiro.criarPara(id, sessaoId)` / `ChatMensagem.doCliente(...)` — criação nova, com as regras aplicadas.
- `reconstituir(...)` — restaura o estado exato vindo do banco.

**Alternativas.** Um construtor público único — descartada porque ou o mapeador não conseguiria restaurar o estado real, ou a criação de uma entidade nova exigiria que o chamador informasse status e datas, que é justamente a regra que a entidade deveria garantir.

**Consequências.** Quem lê o código precisa saber qual fábrica usar; os nomes tornam isso evidente.

**Onde no código.** `domain/entity/Sessao.java`, `ListaRoteiro.java`, `ChatMensagem.java`, `ItemRoteiro.java`.

---

### D-06. `encerrar()` desdobrado em três métodos nomeados por evento

**Contexto.** O diagrama de classes previa um `encerrar()` em `Sessao`. Mas ele tem dois consumidores com desfechos diferentes: a conclusão da rota pelo cliente (deve virar `COMPLETED`) e o job de expiração de TTL (deve virar `EXPIRED` ou `ABANDONED`).

**Decisão.** Três métodos, um por evento de negócio: `encerrar()` → `COMPLETED`, `expirar()` → `EXPIRED`, `abandonar()` → `ABANDONED`. Os três compartilham um guard privado que **só age se a sessão ainda estiver `ACTIVE`**.

**Alternativas.** `encerrar(StatusSessao statusFinal)` — descartada porque permitiria chamadas sem sentido (`encerrar(ACTIVE)`) e esconderia o evento de negócio atrás de um parâmetro.

**Consequências.** O guard evita um problema real: o cliente conclui a rota às 10h00 (`COMPLETED`) e às 10h05 o cron encontra a sessão com TTL vencido. Sem o guard, o cron sobrescreveria para `EXPIRED` e o sistema perderia a informação de que o cliente **completou** a jornada — que é a métrica de sucesso do produto.

**Onde no código.** `domain/entity/Sessao.java`.

---

### D-07. `ItemRoteiro.reconstituir` para leitura do banco

**Contexto.** O construtor público de `ItemRoteiro` cria um item recém-adicionado no Totem: sempre com `ordemCaminho = null` e `coletado = false`, que é o estado correto para um item novo.

**Decisão.** Uma fábrica `reconstituir(id, produto, ordemCaminho, coletado)` para o mapeador de persistência.

**Alternativas.** Usar o construtor público e depois chamar `definirOrdem()` e `marcarComoColetado()` — funciona, mas usaria métodos que representam **eventos de negócio** ("o cliente pegou o produto da prateleira") para restaurar estado, o que confunde quem lê.

**Consequências.** Sem essa fábrica, recarregar uma lista do banco descartaria a rota já calculada e o progresso de coleta do cliente a cada leitura.

**Onde no código.** `domain/entity/ItemRoteiro.java`, `infrastructure/database/factory/ItemRoteiroFactory.java`.

---

### D-08. O domínio registra o token de handoff, nunca o assina

> [!NOTE]
> **Superada em 25/08/2026.** O handoff entre dispositivos foi removido: sem totem, a jornada inteira acontece no celular do cliente e nao ha transicao a autorizar. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque descreve raciocinio que continua valendo — a separacao entre contrato de negocio no dominio e tecnologia na infraestrutura, que segue viva no `AssistenteIA`.

**Contexto.** O DER define `handoff_token` como um JWT assinado. Assinar JWT exige a biblioteca `jjwt` — que é infraestrutura.

**Decisão.** `ListaRoteiro.registrarTokenHandoff(String tokenAssinado)` recebe o token já pronto e apenas o guarda, calculando a expiração (5 minutos). A assinatura acontece na camada de infraestrutura.

Complementos: `isTokenValido()` e `invalidarToken()` — este último é o que viabiliza o requisito de **uso único** do QR Code.

**Alternativas.** Um `gerarTokenHandoff()` que assinasse o JWT dentro da entidade — descartada por importar biblioteca no domínio, violando a D-01. Gerar um token opaco (UUID aleatório) no domínio — descartada por abandonar o JWT previsto no DER e no diagrama de sequência.

**Consequências.** O caso de uso de handoff precisa orquestrar dois passos (assinar na infraestrutura, registrar no domínio) em vez de um.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

**Implementação do lado da infraestrutura.** O contrato ficou em `domain/service/GeradorTokenHandoff` — uma interface escrita em termos de negócio ("produza um token de transição confiável"), que não menciona JWT. A implementação `infrastructure/security/GeradorTokenJwt` é a única classe do projeto que conhece a biblioteca `jjwt`. Mesmo arranjo dos repositórios: contrato no domínio, tecnologia na infraestrutura.

---

### D-35. O cliente de IA falha explicitamente; o fallback é de quem chama

**Contexto.** A IA pode falhar de várias formas: cota esgotada, tempo esgotado, filtro de conteúdo do provedor, resposta em formato inesperado. Ficou decidido que o sistema deve **degradar** em vez de estourar erro na cara do cliente.

**Decisão.** O cliente (`GeminiClient`) **não** implementa fallback: ele lança `AssistenteIAIndisponivelException` em qualquer falha. Cada caso de uso decide o que fazer.

**Motivo.** O fallback correto depende do contexto e só quem conhece o negócio sabe escolher:
- na ruptura de estoque, cair para "produto disponível mais próximo", calculado por nós;
- no chat, uma mensagem honesta de indisponibilidade.

Um fallback genérico dentro do cliente teria que escolher um dos dois — e esconderia a falha de quem precisava decidir.

**Onde o ciclo de function calling é resolvido.** Dentro do cliente, não no caso de uso. A interface recebe um `ExecutorDeFerramenta`; quem chama informa apenas *como executar* a consulta, e o vai-e-vem com o provedor fica encapsulado. Falar o protocolo é responsabilidade do cliente; conhecer o negócio é do caso de uso.

**Detalhe de implementação que não é óbvio.** O turno do modelo é devolvido **inteiro** no histórico da chamada seguinte, sem reconstrução. A resposta carrega campos próprios do provedor (assinatura de raciocínio, identificador da chamada) que precisam ser preservados para a conversa continuar. Reconstruir só os campos que entendemos quebraria o ciclo — e foi verificado por chamadas reais, não presumido.

**Limite de ciclos.** O laço para em 5 idas e voltas. Sem isso, um modelo que insistisse em pedir ferramentas prenderia a requisição indefinidamente.

**Onde no código.** `domain/service/AssistenteIA.java`, `infrastructure/ia/client/GeminiClient.java`, `infrastructure/web/RestClientConfig.java` (tempos limite).

---

### D-36. Assistente busca no catálogo por ferramenta, com escopo fechado

**Contexto.** O card previa "um único prompt contendo o contexto do catálogo". Com 25 produtos, empurrar tudo no prompt caberia.

**Decisão.** O assistente **consulta** o catálogo por ferramenta, em vez de recebê-lo pronto. Reaproveita a busca tolerante a erro de digitação do UC-002.

**Motivo.** Escala para um catálogo real (milhares de SKUs não cabem num prompt), mantém o mesmo grounding da ruptura de estoque, e unifica a explicação dos dois recursos de IA na banca: *o modelo consulta nosso sistema*.

**A ferramenta aceita vários termos numa chamada só** — e isso não é detalhe de estilo. O tier gratuito limita as requisições por minuto **e** por dia (ver [D-37](#d-37-escolha-do-modelo-por-medição-e-o-limite-do-tier-gratuito)), e cada ida e volta consome uma. Na primeira versão o assistente buscava produto por produto e **estourava o limite de ciclos numa única pergunta** ("o que preciso para pintar?" virava quatro buscas). Pedindo `"tinta, rolo, lixa, fita crepe"` de uma vez, a mesma pergunta custa duas requisições.

**Escopo fechado por instrução, verificado por teste.** A decisão de refinamento falava em "conjunto fechado de projetos"; uma lista rígida de temas seria pior de usar do que um escopo bem descrito na instrução de sistema. O que protege a demonstração não é a forma da regra, é ela funcionar — então há teste de integração perguntando sobre futebol e verificando a recusa. Resposta real obtida: *"Não posso ajudar com assuntos de futebol ou eventos esportivos, pois meu foco é auxiliar no seu projeto de reforma, construção ou decoração."*

**A falha da IA não entra no histórico.** O cliente vê a mensagem de indisponibilidade, mas ela não é persistida: o assistente não deveria "lembrar" de ter dito que estava fora do ar ao montar o contexto das próximas perguntas. A pergunta do cliente fica salva sem resposta, que é o que de fato aconteceu.

**O assistente não altera o carrinho.** Ele sugere; adicionar produto continua sendo ação do cliente (UC-004). Dar poder de escrita ao modelo seria um salto de escopo com risco desproporcional.

**Onde no código.** `application/usecase/ConversarComAssistenteUseCase.java`, `infrastructure/ia/factory/InstrucaoDoAssistente.java`.

---

### D-37. Escolha do modelo por medição, e o limite do tier gratuito

**Contexto.** A escolha inicial (`gemini-3.6-flash`, o topo de linha disponível) parecia óbvia: mais capaz é melhor. Os testes de integração mostraram o contrário.

**O que a medição revelou.**

| Modelo | Tempo de resposta | Cota gratuita |
|---|---|---|
| `gemini-3.6-flash` | mais de 20s — estourava o tempo limite | **20 requisições por dia** |
| `gemini-3.5-flash-lite` | ~1s | bem mais folgada |

O modelo de topo **raciocina antes de responder** (centenas de tokens de "pensamento" por resposta), o que explica a lentidão. Para sugerir produtos de uma lista que nós fornecemos, esse raciocínio não agrega — só custa tempo e cota.

**Decisão.** `gemini-3.5-flash-lite` como padrão, configurável por `GEMINI_MODEL`.

**Consequência que o time precisa conhecer.** O tier gratuito tem limite **diário** por modelo, e ele é facilmente esgotado: uma sessão de testes de desenvolvimento consumiu as 20 requisições diárias do modelo de topo. Como a cota é por modelo, trocar de modelo dá cota nova — mas isso é contorno, não solução. Antes da gravação do vídeo e da banca, vale confirmar a cota do modelo em uso e considerar habilitar faturamento no Google Cloud (que mantém o uso gratuito, mas eleva os limites).

**Mitigações já no código.** O cliente tenta novamente até três vezes em falhas transitórias (cota por minuto e picos de demanda do provedor, ambos observados), com espera crescente. E quando ainda assim falha, o caso de uso degrada com mensagem honesta em vez de erro — a demonstração não quebra.

**Lição que vale para além deste card.** A listagem de modelos da API não indica disponibilidade, latência nem cota. `gemini-2.5-flash` aparece na lista e responde 404; `gemini-3.6-flash` responde mas é lento e tem cota diária baixa. Só a chamada real informa.

**Onde no código.** `src/main/resources/application.yml`, `infrastructure/ia/client/GeminiClient.java`.

---

### D-27. Segredo do JWT por ambiente, com chave aleatória em desenvolvimento

> [!NOTE]
> **Superada em 25/08/2026.** O handoff entre dispositivos foi removido: sem totem, a jornada inteira acontece no celular do cliente e nao ha transicao a autorizar. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque descreve raciocinio que continua valendo — nunca commitar segredo padrao, e preferir falhar visivelmente a ter um valor embutido no codigo.

**Contexto.** Assinar o token de handoff exige uma chave secreta. Cada integrante roda a aplicação na própria máquina, e o projeto vai para deploy público.

**Decisão.** A chave vem de `JWT_SECRET` (via `merlin.jwt.secret`). Se estiver ausente ou vazia, a aplicação **gera uma chave aleatória no startup** e registra um aviso no log.

**Alternativas.** Um segredo padrão embutido no `application.yml` — descartada, e essa é a decisão central aqui: um segredo commitado é exatamente o tipo de coisa que passa despercebida e chega em produção. Qualquer pessoa com acesso ao repositório poderia forjar tokens válidos. Falhar o startup quando o segredo falta — descartada por atritar o dia a dia dos cinco integrantes sem ganho real de segurança em desenvolvimento.

**Consequências.** Em desenvolvimento, tokens não sobrevivem a um restart da aplicação. Irrelevante, dado o TTL de 5 minutos: um QR Code gerado antes de reiniciar já estaria perto de expirar de qualquer forma. Em produção, basta definir a variável de ambiente.

**Onde no código.** `infrastructure/security/GeradorTokenJwt.java`, `src/main/resources/application.yml`.

---

### D-28. A rota parte do primeiro ponto do tipo TOTEM

> [!NOTE]
> **Superada em 25/08/2026.** A rota calculada foi removida: a orientação da Leroy é mostrar apenas onde o cliente está e onde está cada produto, deixando o caminho por conta dele. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> Sem totem e sem rota, some tanto a origem quanto o que ela originava. O tipo `TOTEM` saiu do enum `TipoPonto` e as linhas gravadas com ele são apagadas na carga — ver [D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco).

**Contexto.** O algoritmo de roteamento precisa de uma origem (D-26). No handoff, a origem natural é o totem onde o cliente está montando a lista.

**Decisão.** O caso de uso busca pontos do tipo `TOTEM` e usa o primeiro encontrado.

**Limitação assumida.** Numa loja com vários totens, o roteiro sairia calculado a partir do totem errado. Resolver exigiria o Totem se identificar na requisição, e o contrato hoje envia apenas `sessaoId` — mudar isso afetaria a dupla de frontend, o que pela política da D-25 exige combinar antes.

É uma limitação sem impacto no escopo atual (a massa de demonstração tem um totem) e de correção simples quando for necessário: acrescentar `totemId` opcional ao `HandoffRequest`.

**Onde no código.** `application/usecase/GerarHandoffUseCase.java`.

---

### D-29. Uso único do token pela ausência no banco

> [!NOTE]
> **Superada em 25/08/2026.** O handoff entre dispositivos foi removido: sem totem, a jornada inteira acontece no celular do cliente e nao ha transicao a autorizar. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque descreve raciocinio que continua valendo — resolver revogacao pela ausencia do dado em vez de criar estrutura para isso.

**Contexto.** O diagrama de sequência especifica que o token do QR Code é de **uso único**: escaneou uma vez, não vale mais. JWT é, por natureza, um token *stateless* — a assinatura continua válida até a expiração, e nada nele impede reutilização.

**Decisão.** O uso único não vem do JWT, e sim da persistência. A validação tem duas camadas:

1. **Criptográfica** — confere assinatura e prazo, sem tocar o banco (falha rápido e barato).
2. **Persistência** — procura a lista **pelo token** (`buscarPorToken`). Depois de consumido, `invalidarToken()` zera o campo e a mesma consulta não encontra mais nada.

A consequência é que mesmo um token criptograficamente perfeito — copiado da URL, por exemplo — só funciona uma vez.

**Alternativas.** Manter uma tabela de tokens revogados — descartada por adicionar estrutura para resolver algo que a ausência do campo já resolve, com o agravante de exigir limpeza periódica dos registros vencidos. Confiar apenas na expiração do JWT — descartada por não atender o requisito de uso único.

**Risco de UX assumido, e a saída que já existe.** Se o cliente fechar a aba sem querer, escanear o mesmo QR de novo não funciona — foi um dos riscos levantados na análise inicial do projeto.

Mas existe um caminho de recuperação já disponível: a resposta traz o `sessaoId`, e `GET /sessoes/{id}/roteiro` devolve a lista **com a ordem da rota já calculada**, sem exigir token. O celular consegue se recuperar sozinho, sem o cliente refazer o planejamento no Totem.

**O caso mais grave — a troca de aparelho — deixou de estar em aberto na Fase 3.** `POST /handoff` na mesma sessão regenera o QR Code, e o aparelho novo continua a jornada de onde ela parou. Ver [D-44](#d-44-o-token-de-handoff-sai-da-url-e-o-qr-code-passa-a-ser-regenerável).

**Onde no código.** `application/usecase/ValidarHandoffUseCase.java`, `domain/entity/ListaRoteiro.java`.

---

### D-09. Relação com a sessão é unidirecional

**Contexto.** O diagrama de classes mostra `Sessao` 1-1 `ListaRoteiro` e 1-N `ChatMensagem`. Uma modelagem literal criaria referências nos dois sentidos.

**Decisão.** `ListaRoteiro` e `ChatMensagem` guardam apenas `sessaoId (UUID)`. `Sessao` não conhece nenhum dos dois.

**Alternativas.** Referência bidirecional — descartada porque criaria um ciclo, que gera recursão infinita na serialização JSON e complica o mapeamento JPA, sem benefício prático aqui.

**Consequências.** Para obter a lista de uma sessão usa-se `listaRoteiroRepository.buscarPorSessao(id)` em vez de `sessao.getListaRoteiro()`.

**Onde no código.** `domain/entity/ListaRoteiro.java`, `ChatMensagem.java`.

---

## Persistência

### D-10. Entidades JPA espelho, separadas das de domínio

**Contexto.** Consequência direta da D-01: o Hibernate precisa de construtor sem argumentos, setters e campos mutáveis.

**Decisão.** Classes `*Entity` em `infrastructure/database/entity`, anotadas com JPA e com `@Getter @Setter @NoArgsConstructor @AllArgsConstructor`. Os mapeadores (`*Factory`) traduzem nos dois sentidos.

**Consequências.** Toda entidade nova exige três arquivos (domínio, JPA, mapeador). É o custo assumido da D-01.

**Onde no código.** `infrastructure/database/entity/`, `infrastructure/database/factory/`.

---

### D-11. UUID gravado como `varchar2(36)`, não `raw(16)`

**Contexto.** O padrão do Hibernate para `UUID` no Oracle é `raw(16)` — compacto, porém ilegível: consultar vira `where id = hextoraw('A1B2...')`.

**Decisão.** `@JdbcTypeCode(SqlTypes.VARCHAR)` em todos os ids, resultando em `varchar2(36)`.

**Alternativas.** Manter `raw(16)` — descartada porque tornaria a inspeção manual do banco e a escrita de dados de teste impraticáveis, prejudicando tanto a depuração quanto o trabalho do integrante responsável pelo banco.

**Consequências.** ~20 bytes a mais por id, irrelevante na escala do projeto.

**Onde no código.** Todas as classes de `infrastructure/database/entity/`.

---

### D-12. `sessao_id` mapeado duas vezes na mesma entidade

**Contexto.** Tensão entre a D-09 (domínio guarda só o UUID) e o DER (que especifica chave estrangeira de verdade). Um campo `UUID` simples não faz o Hibernate criar a constraint de FK; uma associação JPA faria, mas obrigaria o mapeador a construir entidades-stub para escrever.

**Decisão.** Dois campos apontando para a mesma coluna:
```java
@Column(name = "sessao_id", nullable = false)
private UUID sessaoId;                       // grava

@ManyToOne(fetch = LAZY)
@JoinColumn(name = "sessao_id", insertable = false, updatable = false)
private SessaoEntity sessao;                 // só existe para gerar a FK
```

**Alternativas.** Só o UUID — descartada por deixar o banco sem integridade referencial e divergente do DER entregue. Só a associação — descartada por exigir que o adaptador passasse referências gerenciadas ao mapeador, aumentando o acoplamento.

**Consequências.** Parece erro para quem lê sem contexto — por isso há comentário explicativo nas duas classes. O `insertable=false, updatable=false` é obrigatório: sem ele, o Hibernate acusa dois campos disputando a mesma coluna.

**Onde no código.** `infrastructure/database/entity/ListaRoteiroEntity.java`, `ChatMensagemEntity.java`.

---

### D-13. Cascata e remoção de órfãos no agregado da lista

**Contexto.** Um `ItemRoteiro` não existe fora de uma `ListaRoteiro`. Quando o cliente remove um produto do carrinho, a linha precisa sumir do banco.

**Decisão.** `@OneToMany(mappedBy = "listaRoteiro", cascade = ALL, orphanRemoval = true)`.

**Consequência não óbvia.** `ItemRoteiroEntity` é o lado proprietário da relação (é ele que carrega a coluna `lista_id`). Por isso o mapeador **precisa criar a entidade da lista antes dos itens** e passá-la a cada um; caso contrário, a FK seria gravada nula e o insert falharia. É o motivo de `ListaRoteiroFactory.paraPersistencia` não usar o construtor cheio como os outros mapeadores.

**Validação.** Comportamento confirmado contra o Oracle real: remover um item do domínio e salvar emite `delete from tb_item_roteiro where id=?`.

**Onde no código.** `infrastructure/database/entity/ListaRoteiroEntity.java`, `factory/ListaRoteiroFactory.java`.

---

### D-14. Salvar item carrega a entidade gerenciada em vez de remapear

**Contexto.** Todos os adaptadores seguem o padrão "mapeia → delega → mapeia de volta". `ItemRoteiroRepositoryAdapter.salvar` é a exceção.

**Decisão.** Ele carrega a entidade existente pelo id, altera apenas `ordemCaminho` e `coletado`, e salva. Se o item não existir, lança exceção explícita.

**Motivo.** Pela D-09, `ItemRoteiro` no domínio **não conhece a lista que o contém**. Remapear um grafo novo gravaria `lista_id` nula e desligaria o item do roteiro — na prática, o cliente perderia aquele produto da rota ao marcá-lo como coletado.

**Consequências.** Este método não serve para criar itens novos; a criação passa por `ListaRoteiro.adicionarProduto()` + salvar a lista, via cascata (D-13). A exceção explícita evita que alguém tente o caminho errado e fique depurando FK nula.

**Onde no código.** `infrastructure/database/adapter/ItemRoteiroRepositoryAdapter.java`.

---

### D-15. Query nativa com `UTL_MATCH` para busca tolerante a erro de digitação

**Contexto.** O UC-002 pede busca "fuzzy". Um `LIKE '%termo%'` cobre busca parcial (`"tint"` acha `"Tinta"`), mas **não** tolera erro de digitação: `"tnta"` retorna vazio.

**Decisão.** Query nativa combinando as duas estratégias, ordenada por similaridade decrescente:
```sql
where upper(p.nome) like upper('%' || :termo || '%')
   or utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) > 70
order by utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) desc, p.nome
```

**Alternativas.** Só `LIKE` — descartada porque o card promete tolerância a erro e não entregaria: se um avaliador digitar errado na demonstração ao vivo, a tela voltaria vazia. Elasticsearch ou similar — descartada por exigir infraestrutura adicional, contra a restrição de custo zero.

**Consequências.** Este é o **único ponto do código amarrado ao Oracle** — `UTL_MATCH` não tem equivalente em JPQL nem em outros bancos. Aceitável porque o banco é fixo neste projeto (instância fornecida pela FIAP), mas é o primeiro lugar a mexer numa eventual troca de SGBD.

O limiar de 70 foi calibrado empiricamente: mais alto perde `"furadera"` → `"Furadeira"`, mais baixo traz ruído. Uma consequência conhecida: `"tnta"` também retorna `"Trena 5m"`, porque as palavras são de fato próximas na métrica — a ordenação por similaridade garante que a Tinta apareça primeiro.

**Validação.** Confirmado contra o Oracle real: `"tint"`, `"tinta"`, `"tnta"` e `"furadera"` retornam os produtos esperados.

**Onde no código.** `infrastructure/database/repository/ProdutoJpaRepository.java`.

---

### D-16. Carga inicial em Java em vez de SQL

**Contexto.** O esqueleto do repositório tem uma pasta `database/seeds/`, sugerindo scripts SQL. Mas o projeto usa `ddl-auto: update` (sem Flyway), e cada integrante tem seu próprio schema Oracle.

**Decisão.** Um `ApplicationRunner` que popula o banco usando os ports de domínio. Na Fase 0 ele só agia com o catálogo **vazio**; desde 23/08/2026 a carga é **incremental**, item a item, para que produtos acrescentados depois cheguem aos bancos que já tinham a massa antiga — ver [D-47](#d-47-a-massa-ganhou-pares-de-substituição-e-a-carga-passou-a-ser-incremental).

**Alternativas.** `data.sql` — descartada porque roda a cada inicialização e, para não falhar com chave duplicada no Oracle, exigiria `MERGE INTO`, bem mais verboso que `INSERT`. Script SQL manual em `database/seeds/` — descartada porque exigiria cada integrante rodar à mão em seu schema.

**Consequências.** A massa de dados vive em código Java, não em SQL — menos acessível para quem só mexe com banco. Em troca, é idempotente por natureza e cada integrante popula o próprio schema automaticamente ao subir a aplicação. Desligável com `merlin.seed.enabled=false`.

Efeito colateral positivo: por usar os ports de domínio, o seeder funciona como uma validação de que a arquitetura funciona ponta a ponta em volume.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java`.

---

## Produto e negócio

### D-17. Carrinho de roteiro sem limite de itens

**Contexto.** O material original do projeto previa teto de 15 itens, justificado pela capacidade física de um carrinho de compras.

**Decisão.** Sem limite. `ListaRoteiro` aceita qualquer quantidade.

**Motivo.** O público B2B (empreiteiros, pedreiros, arquitetos) compra listas grandes para uma obra inteira — e é justamente o perfil mais sensível a perda de tempo, que a solução pretende atender. Um teto arbitrário excluiria o caso de uso mais valioso.

**Consequências.** O algoritmo de roteamento e a interface do mapa no celular precisam lidar bem com listas potencialmente longas. Os cards do backlog que falavam em "validação de limites estruturais" e "controle capacitivo" foram reescritos.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

---

### D-18. Produto duplicado é ignorado no carrinho

**Contexto.** Nada impede o cliente de adicionar o mesmo produto duas vezes. O DER não tem campo de quantidade.

**Decisão.** `adicionarProduto` devolve o item já existente em vez de criar outro.

**Alternativas.** Permitir duplicatas — descartada porque geraria duas paradas na mesma coordenada da rota (o cliente andaria até a mesma prateleira duas vezes) e ordens de caminho conflitantes. Adicionar campo `quantidade` — descartada por divergir do DER já entregue e aumentar o escopo.

**Consequências.** Não há como o cliente indicar "quero 2 unidades" pelo roteiro. Aceitável: o roteiro serve para **localizar** produtos, e a quantidade é resolvida na prateleira.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

---

### D-19. Coordenadas do seed seguem a planta real da loja

**Contexto.** A massa de demonstração precisa de coordenadas para cada seção. O caminho fácil seria distribuí-las aleatoriamente.

**Decisão.** As posições seguem aproximadamente a planta real compartilhada pela Leroy Merlin no kickoff — Tintas no topo, Cozinhas e Iluminação no centro-direita, Materiais de construção no canto inferior esquerdo, totem próximo à entrada.

**Motivo.** Com coordenadas sem relação com o layout, a rota "otimizada" pareceria um emaranhado sem lógica quando o frontend desenhasse o mapa — e a demonstração perderia credibilidade exatamente no ponto que ela quer provar.

**Consequências.** As distâncias resultantes são realistas (Tintas → Materiais de construção dá 72 unidades num grid de 100), dando ao algoritmo de roteamento um problema de verdade para otimizar.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java`.

---

### D-31. Ponto de interesse não é persistido

> [!NOTE]
> **Superada em 25/08/2026.** O recurso foi removido junto com a rota calculada — sem rota, não existe posição em que inserir um desvio, e o banheiro virou apenas mais um ponto desenhado no mapa. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque a **lógica de inferir a posição do cliente** nasceu aqui e sobreviveu à remoção, agora como conceito da sessão.

**Contexto.** O UC-012 permite ao cliente pedir um banheiro ou caixa durante a caminhada. Mas um ponto de apoio **não é um produto**: não cabe como `ItemRoteiro`, que exige um `Produto`, e o DER entregue à banca não tem tabela para esse conceito.

**Decisão.** O ponto de apoio é inserido apenas na rota **devolvida** pelo endpoint. Nada é gravado no banco.

**Alternativas.** Criar tabela e entidade próprias para pontos de interesse — a modelagem correta e duradoura, descartada por três motivos combinados: divergiria do DER já entregue na documentação (exigindo atualizar o diagrama), seria o card mais caro da fase, e o prazo do vídeo estava a três semanas. Guardar um campo na `ListaRoteiro` — descartada por poluir a entidade com um conceito que não é dela e suportar só um desvio por vez.

**Custo assumido.** Se o app recarregar, o desvio some e o cliente precisa tocar de novo no botão. Aceitável porque o desvio é transitório por natureza — depois de passar no banheiro, ele deixa de importar — e porque o celular mantém a rota em cache (`sessionStorage`, conforme o diagrama de sequência).

**O que mudaria para persistir no futuro.** Uma tabela `TB_PONTO_INTERESSE_ROTEIRO` com `lista_id`, `ponto_mapa_id` e a posição de inserção, mais entidade, mapper, repositório e atualização do DER. A lógica de inserção do caso de uso não mudaria.

**Consequências no contrato.** `PontoRota.item` vem **nulo** para o ponto de apoio — o contrato já previa isso ("item de compra **ou ponto de interesse**"), e é por essa ausência que o celular distingue uma parada de compra de um desvio.

**Onde no código.** `application/usecase/IncluirPontoDeInteresseUseCase.java`, `application/dto/PontoRotaResponse.java`.

---

### D-24. TTL da sessão é renovado a cada interação

> [!NOTE]
> **Atualizada em 25/08/2026.** O TTL passou de 30 minutos para **4 horas**. O texto abaixo já reflete a mudança; o motivo dela está no primeiro parágrafo.

**Contexto.** A sessão nasce com um TTL de inatividade. Os 30 minutos originais foram dimensionados para **liberar o totem** para o próximo cliente — perder a sessão era um incômodo, mas a lista podia ser refeita ali mesmo, no equipamento.

**Esse motivo desapareceu com o totem.** O aparelho é do cliente, ninguém está na fila esperando por ele, e agora **a sessão guarda a lista inteira**: perdê-la significa mandar quem está no meio de uma loja começar tudo de novo. Um cliente que atende uma ligação de quarenta minutos perdia tudo, sem aviso — e essa é uma quebra de fluxo comum, não um caso extremo.

**Decisão.** TTL de **4 horas** de inatividade, e adicionar, remover, coletar ou recentrar chamam `Sessao.renovarSessao()`, empurrando o vencimento para 4 horas à frente da interação. Consultar a lista **não** renova.

**Por que 4 horas, e não eterno.** Sem vencimento, o banco acumularia sessões `ACTIVE` para sempre e a varredura da [D-42](#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem) não teria o que classificar — perderíamos a métrica de carrinho abandonado, que é informação de negócio. Quatro horas cobrem com folga qualquer compra real e ainda deixam o dado dizer algo.

**Por que renovar continua sendo necessário, mesmo com folga.** Vale a mesma contradição de antes com a [D-17](#d-17-carrinho-de-roteiro-sem-limite-de-itens): o carrinho não tem limite justamente para atender o empreiteiro que monta uma lista para uma obra inteira, e um TTL fixo expulsaria exatamente esse cliente. As duas decisões só funcionam juntas.

**Por que consultar não renova.** Consulta é leitura; renovar exigiria gravar a cada `GET`, e listar a lista não é sinal forte de atividade (uma tela aberta e esquecida continuaria consultando). As ações de escrita são evidência real de que há alguém interagindo.

**A varredura acompanhou.** O intervalo passou de 5 para **30 minutos**: com um TTL de 4 horas, varrer a cada cinco seriam 48 consultas para cada uma que encontra algo — contra um banco a 5.000 km e num tier gratuito de 0,1 CPU ([D-45](#d-45-o-deploy-mudou-quais-avisos-do-hibernate-importavam)). A varredura não protege regra de negócio: `Sessao.isValida()` compara com o relógio a cada requisição.

**Consequências.** Uma sessão só expira após 4 horas de **inatividade** real, não de duração total. O job de expiração continua funcionando normalmente: ele varre por `expiracaoTtl` vencido, que reflete a última interação.

**Onde no código.** `application/usecase/AdicionarProdutoAoRoteiroUseCase.java`, `RemoverProdutoDoRoteiroUseCase.java`, `domain/entity/Sessao.java`.

---

### D-32. Coletar item passa pela raiz do agregado e não encerra a jornada

**Contexto.** No UC-014 o cliente confirma ter pego um produto da prateleira. O caminho direto seria carregar o `ItemRoteiro` pelo id, marcar e salvar — e existe até um `ItemRoteiroRepository` pronto para isso.

**Decisão 1: carregar pela `ListaRoteiro`, não pelo item.** Um novo método `buscarPorItem(itemId)` no port devolve o agregado inteiro; o item é marcado dentro dele e a lista é salva.

Dois motivos. O primeiro é de modelagem: alterações devem passar pela raiz do agregado. O segundo é prático — pela [D-09](#d-09-relação-com-a-sessão-é-unidirecional), o `ItemRoteiro` não conhece a lista que o contém, e sem chegar à lista não há como alcançar a sessão, necessária para a decisão 2.

O `ItemRoteiroRepository` continua no projeto: o fluxo de ruptura de estoque (Fase 2) precisa localizar um item isolado.

**Decisão 2: coletar renova o TTL da sessão.** Aplicação da [D-24](#d-24-ttl-da-sessão-é-renovado-a-cada-interação) ao trecho da caminhada, e aqui ela é ainda mais necessária.

Sem isso havia um bug silencioso esperando: a sessão era renovada no handoff e **nada mais a renovava durante a caminhada**. Um cliente com lista grande numa loja de 10.000m² leva mais de meia hora com facilidade — a sessão morreria no meio da compra, quebrando a marcação dos itens seguintes e o tratamento de ruptura, sem nenhum erro aparente até o cliente tentar a próxima ação.

**Decisão 3: marcar item já coletado é no-op com 200.** O cliente pode tocar duas vezes, ou a rede pode reenviar. Recusar não protege nada e atrapalha.

**Decisão 4: coletar o último item NÃO encerra a sessão.** É tentador automatizar, mas erraria: depois de pegar tudo, o cliente ainda precisa chegar ao caixa, e encerrar ali mataria a navegação justamente no trecho final. Quem encerra é o cliente, pelo botão — o celular pode apenas sugerir quando perceber que tudo foi coletado.

Pelo mesmo raciocínio, concluir **não exige** que todos os itens estejam coletados: desistir de um produto e ir ao caixa é comportamento normal.

**Onde no código.** `application/usecase/MarcarItemColetadoUseCase.java`, `ConcluirRotaUseCase.java`, `domain/repository/ListaRoteiroRepository.java`.

---

### D-20. Google Gemini como provedor de LLM

**Contexto.** O projeto usa IA em dois pontos: o assistente conversacional e a sugestão de substituto em caso de ruptura de estoque. Restrição de custo zero (projeto acadêmico).

**Decisão.** Google Gemini, no tier gratuito, com *function calling*.

**Motivo do function calling.** A sugestão de substituto **não** pode ser texto livre do modelo: ele inventaria produtos inexistentes ou fisicamente distantes. Com function calling, o modelo só consegue sugerir chamando uma função que consulta `Produto`/`PontoMapa` reais, filtrados por proximidade e estoque. A restrição fica garantida pela arquitetura, não pela boa vontade do prompt.

**Alternativas.** OpenAI (citada no C4 original) — sem tier gratuito adequado. Groq — também gratuito e mais rápido, mas com qualidade de resposta mais variável.

**Alternativa considerada e descartada na implementação: saída estruturada.** Em vez de function calling, poderíamos fazer a pré-filtragem espacial e mandar os candidatos prontos, pedindo ao modelo apenas que escolhesse um id da lista em JSON. Daria a mesma garantia de não-alucinação com uma chamada só, mais barata e mais simples de testar.

Optamos por manter o function calling porque é o que a documentação entregue descreve, e porque a diferença é relevante na explicação à banca: "o modelo consulta nosso sistema" é substancialmente mais forte que "o modelo escolhe de uma lista que já preparamos". O custo aceito são duas chamadas por sugestão e mais orquestração.

**Consequências.** A chave de API vem de variável de ambiente (`GEMINI_API_KEY`), nunca versionada.

**Modelo: `gemini-3.6-flash`.** O `gemini-2.5-flash` aparece na listagem de modelos da API, mas **não é utilizável por chaves novas** — a própria API responde 404 com a orientação de migrar. Descoberto ao testar; a lição é que a listagem de modelos não garante disponibilidade, e só a chamada real confirma. O modelo é configurável por `GEMINI_MODEL` para que a troca não exija recompilar.

**Status.** Implementado e verificado com chamadas reais (Fase 2, card 1).

---

### D-21. Demo da banca por simulação animada, não posicionamento real

**Contexto.** A banca final (21/09/2026) é 100% online. Não há como caminhar por uma loja real mostrando o celular navegando.

**Decisão.** O Mobile PWA replay a rota já calculada, animando um marcador avançando pelos pontos num mapa simples, sem depender de bússola ou acelerômetro.

**Alternativas.** Antecipar o sistema autoral de posicionamento (bússola + acelerômetro) previsto para o NEXT — descartada por ser P&D não iniciado, com risco alto de não estar confiável no prazo, e por depender de condições físicas impossíveis numa chamada de vídeo.

**Consequências.** A simulação usa as coordenadas reais calculadas pelo algoritmo, então o que se vê na tela é a rota de verdade — não uma animação fabricada. O sistema de posicionamento real fica reservado para o NEXT (24/10/2026), se o grupo for selecionado.

**Dependência.** É o que torna o endpoint de marcação de item coletado (UC-014) parte do caminho crítico: é ele que faz o marcador avançar.

---

### D-23. "Resolução síncrona de inventário" não é integração com ERP

**Contexto.** O card do UC-003 no backlog se chama "Consulta de Detalhamento e **Resolução Síncrona de Inventário**". O nome sugere consulta em tempo real a um sistema de estoque externo.

**Decisão.** Não há integração alguma. O `saldoEstoque` é lido da nossa própria tabela `TB_PRODUTO` no momento da requisição, sem cache. "Síncrono" significa apenas que o valor não é pré-calculado nem defasado em relação ao nosso banco.

**Motivo.** Integração com ERP/WMS está fora do escopo de um projeto acadêmico — não temos acesso a sistema real da Leroy Merlin, e a restrição de custo zero inviabiliza qualquer serviço intermediário.

**Consequências.** É importante ser honesto sobre isso na apresentação: o sistema resolve o problema de **localizar** o produto, mas a confiabilidade do estoque exibido depende do dado no banco estar atualizado. Esse é justamente o risco de negócio levantado no início do projeto — o Merlin poderia reproduzir a mesma "ruptura silenciosa" que promete resolver, se o estoque não refletir a prateleira.

É por isso que o fluxo de ruptura física (UC-013) existe e é tratado como diferencial: ele assume que a divergência **vai** acontecer e converte a falha em oportunidade de venda, em vez de fingir que o estoque é confiável.

**Onde no código.** `application/usecase/ConsultarProdutoUseCase.java`.

---

### D-38. Ruptura de estoque: o modelo escolhe, mas quem responde é o banco

**Contexto.** O cliente chega à prateleira e ela está vazia (UC-013). O sistema tem segundos para transformar essa frustração numa venda de substituto, com o cliente parado ali. É a peça central da demonstração — e a que mais depende de a IA não inventar produto.

**Decisão.** Três etapas encadeadas, cada uma restringindo a seguinte:

1. **Pré-filtragem espacial no banco.** Uma query nativa devolve os produtos com saldo em estoque dentro de um raio do `PontoMapa` do produto em falta, ordenados por distância euclidiana. Nativa pelo mesmo motivo da [D-15](#d-15-query-nativa-com-utl_match-para-busca-tolerante-a-erro-de-digitação): JPQL não tem `sqrt`/`power`, e ordenar por distância em memória exigiria carregar o catálogo inteiro.
2. **Eleição semântica pelo assistente**, por *function calling*, entre esses candidatos e somente eles.
3. **Validação da escolha contra a lista de candidatos.**

**O grounding está na etapa 3, não na 1 nem na 2.** O modelo responde em texto (`SKU-XXX-NNN | justificativa`), e o formato não garante nada sozinho — um modelo pode devolver um SKU inventado no formato certo. O que garante é a validação: o código só aceita o SKU se ele estiver entre os candidatos que **nós** oferecemos, e o produto entregue ao cliente é sempre o objeto vindo do nosso banco, resolvido por aquele SKU. Do texto do modelo sobrevive apenas a justificativa. Um código inventado — ou de um produto que existe mas não estava na lista — é descartado como se a IA não tivesse respondido.

Há teste para exatamente isso: o assistente responde `SKU-MAT-999 | Leve a massa corrida premium`, e o teste verifica que o cliente recebe outro produto e que a frase inventada não chega até ele.

**A ferramenta não recebe parâmetros, de propósito.** Quais produtos entram na lista é decisão do sistema. Se o modelo pudesse informar o corredor ou o termo de busca, estaria escolhendo o próprio universo de opções — justamente o que a pré-filtragem existe para impedir.

**Fallback determinístico (aplicação da [D-35](#d-35-o-cliente-de-ia-falha-explicitamente-o-fallback-é-de-quem-chama)).** Assistente fora do ar, SKU inválido ou resposta ilegível → o sistema sugere o **disponível mais próximo**, calculado por nós, marcado como tal e com justificativa honesta: *"Este é o produto disponível mais próximo de onde você está. Confira na embalagem se ele atende ao seu caso antes de levar."* Dado o limite do tier gratuito ([D-37](#d-37-escolha-do-modelo-por-medição-e-o-limite-do-tier-gratuito)), é o que impede a demonstração de quebrar ao vivo se a cota estourar — e isso não é hipotético: aconteceu durante os próprios testes de integração, com o terceiro teste seguido caindo no fallback por limite por minuto.

**Mas "nenhum serve" não cai no fallback.** Se o assistente avaliar os candidatos e concluir que nenhum cumpre a mesma função, a resposta é 422, não uma sugestão qualquer. Sugerir um disjuntor para quem procurava lixa é pior do que não sugerir nada. É um caso diferente de "não havia candidatos", e por isso são caminhos distintos no código.

**A origem da sugestão vai no contrato.** O campo `origemSugestao` (`ASSISTENTE_IA` ou `PROXIMIDADE`) não estava no contrato original e foi acrescentado: sem ele, o frontend rotularia como recomendação inteligente o que foi apenas o item mais perto. Evolução de contrato pelo critério da [D-25](#d-25-409-para-sessão-inativa-e-quando-é-aceitável-evoluir-o-contrato).

**Alternativas.** Saída estruturada em vez de function calling — já considerada e descartada na [D-20](#d-20-google-gemini-como-provedor-de-llm). Pedir ao modelo que chamasse uma segunda ferramenta para registrar a escolha, em vez de responder em texto: mais rigoroso na forma, mas custaria uma terceira ida e volta ao provedor sem melhorar a garantia real, que já está na validação.

**Consequências.** Duas chamadas ao provedor por sugestão. E o raio de busca (25 unidades no grid 0–100 da planta) é um número escolhido por julgamento, não medido: é a ordem de grandeza de um desvio que o cliente aceita fazer a pé. Produtos isolados no mapa — o espelho de Decoração, por exemplo — não têm vizinho dentro do raio e caem em 422.

**Onde no código.** `application/usecase/TratarRupturaEstoqueUseCase.java`, `infrastructure/ia/factory/InstrucaoDeRuptura.java`, `infrastructure/database/repository/ProdutoJpaRepository.java`.

---

### D-39. A ruptura vira registro no banco, e o relato não altera o estoque

**Contexto.** O card pedia "registrar a ruptura", mas o DER entregue não tem tabela para isso. Duas saídas: log estruturado, ou uma entidade nova.

**Decisão.** Entidade nova — `RegistroRuptura` / `TB_REGISTRO_RUPTURA`, com sessão, item, produto em falta, produto sugerido, justificativa, origem e data.

**Motivo, que é de negócio e não técnico.** Sem persistir, a funcionalidade ajuda um cliente e a loja não aprende nada. Com o registro, o botão "Prateleira Vazia" vira um **relato contínuo de divergência entre o estoque do sistema e a gôndola, vindo de quem está olhando a prateleira** — que é exatamente o risco de "ruptura silenciosa" levantado no início do projeto ([D-23](#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp)). A leitura muda de "sugerimos um substituto" para "convertemos a falha em venda **e** entregamos à operação a lista do que está errado na prateleira".

**Custo assumido.** O DER em PDF fica desatualizado e precisa de revisão (área do Vicentini). Não há trabalho de banco: `ddl-auto: update` cria a tabela.

**O relato não zera o estoque do produto.** É tentador — inventário que se autocorrige soa muito bem numa apresentação. Mas um cliente olhando a gôndola errada zeraria estoque real, e um único relato equivocado passaria a mentir para todos os clientes seguintes. **O relato é evidência, não verdade.** O que o sistema faz é acumular as evidências num lugar onde a operação possa revisá-las; corrigir o saldo continua sendo decisão de quem foi conferir.

**A ruptura é registrada mesmo sem substituto**, e só depois a requisição falha com 422. Para a loja, *"o cliente foi até a prateleira, não havia nada, e nem substituto por perto"* é justamente o relato mais grave a chegar — perdê-lo seria perder o caso mais importante.

**Isso obrigou a uma transação própria.** O `salvar` do registro roda com `REQUIRES_NEW`: como o caso de uso registra o relato e só então lança a exceção que vira 422, o rollback dessa exceção levaria embora exatamente o registro que documenta a falha. O relato é um fato independente do desfecho da requisição. O teste de integração do produto isolado no mapa é o que prova que ele sobrevive.

**Sem FK para `TB_ITEM_ROTEIRO`.** O `item_roteiro_id` é gravado como valor solto. O cliente pode remover o item do carrinho depois de relatar a ruptura, e o registro precisa sobreviver a isso: para a loja, a informação de que a gôndola estava vazia continua valendo.

**O sistema não mexe no carrinho.** Ele sugere; aceitar o substituto é ação do cliente, pelos endpoints que já existem. Mesma linha da [D-36](#d-36-assistente-busca-no-catálogo-por-ferramenta-com-escopo-fechado).

**Onde no código.** `domain/entity/RegistroRuptura.java`, `infrastructure/database/adapter/RegistroRupturaRepositoryAdapter.java`, `infrastructure/database/entity/RegistroRupturaEntity.java`.


### D-40. Existe um endpoint que só serve à demonstração, e ele é assumidamente desprotegido

**Contexto.** O fluxo de ruptura (UC-013) é o momento mais forte da apresentação, e depende de um produto estar com saldo zero na hora certa. Deixar isso por conta do estado do banco significa que, na segunda gravação do vídeo — ou na segunda pergunta da banca —, o cenário já não está mais lá.

**Decisão.** `PATCH /api/v1/produtos/{produtoId}/estoque` zera ou restaura o saldo de qualquer produto sob demanda. Não corresponde a nenhum caso de uso do cliente final e não existiria num sistema real, onde o saldo viria do ERP.

**É o único endpoint que altera o catálogo**, e o único que não serve a um caso de uso. Fica no `ProdutoController` porque o contrato o coloca sob `/produtos`; um controller "interno" separado daria a impressão de existir uma área protegida, que **não existe**.

**Sobre a proteção: não há nenhuma, e isso é uma escolha.** Qualquer pessoa com a URL da API publicada consegue zerar o estoque da loja inteira. Três motivos para aceitar:

1. A API não tem autenticação em nenhum endpoint — sessões são apenas UUIDs. Este endpoint não abre uma categoria nova de exposição, embora seja o de maior impacto.
2. Desligá-lo em produção derrotaria o propósito: **é justamente o ambiente publicado que será demonstrado**.
3. O dano é reversível pelo próprio endpoint, e o catálogo é massa de demonstração.

O que fizemos em vez de proteger: o endpoint está marcado como `[Demonstracao]` no Swagger, com a descrição dizendo explicitamente que é ferramenta interna, e a alteração é registrada em log de **nível WARN** — não `INFO`. É uma alteração manual de catálogo feita por fora de qualquer regra de negócio; se aparecer num log sem ninguém ter pedido, alguém precisa reparar.

Registrado como limitação aceita em [`observacoes.md`](observacoes.md), para que a decisão não se perca caso o projeto ganhe autenticação depois.

**Alternativas.** Proteger com um token próprio ou uma variável de ambiente — descartado por adicionar cerimônia justamente na hora da apresentação, que é quando o endpoint é usado. Uma tela administrativa — escopo de frontend que ninguém tem tempo de construir.

**O saldo muda por cópia, não por mutação.** `Produto.comSaldoEstoque(int)` devolve um novo produto, preservando a imutabilidade da entidade ([D-04](#d-04-entidades-imutáveis-por-padrão)), e rejeita saldo negativo. A validação `@Min(0)` no corpo da requisição já barra isso com 400 antes de chegar lá; a guarda na entidade é a segunda linha, para o caso de alguém chamar o caso de uso por outro caminho no futuro.

**Verificado sobre HTTP** contra Oracle e Gemini reais, no ciclo completo da demonstração: restaurar a lixa grão 120 (desfazendo o cenário plantado na massa), zerá-la de novo, relatar a ruptura e receber a lixa d'água eleita pelo assistente. O cenário deixou de depender do estado em que o banco por acaso está.

**Onde no código.** `application/usecase/SimularEstoqueUseCase.java`, `presentation/controller/ProdutoController.java`, `domain/entity/Produto.java`.

---

### D-41. Sessão encerrada continua legível, mas não gravável

**Contexto.** Ao expor o histórico do chat surgiu uma pergunta que vale para todo o sistema: depois que a jornada termina — cliente passou no caixa, sessão `COMPLETED` —, o que ainda pode ser feito com aquela sessão?

**Decisão.** **Leitura sim, escrita não.** `GET /sessoes/{id}/chat/mensagens` devolve 200 numa sessão encerrada; `POST` na mesma URL devolve 409. Mesma regra já valia para a lista de roteiro.

**Motivo.** As duas metades têm naturezas diferentes. O histórico é **registro do que já aconteceu** — negar acesso a ele depois do encerramento não protege nada e quebraria o celular do cliente que reabre a conversa no estacionamento para reler qual lixa o assistente recomendou. Já escrever numa sessão encerrada é incoerente: a jornada acabou, e uma nova pergunta pertence a uma nova sessão.

Verificado sobre HTTP na jornada completa: depois de `POST /sessoes/{id}/concluir`, o `GET` do histórico segue em 200 e o `POST` passa a 409.

**Consequência que vale conhecer.** Como não há autenticação, quem tiver o `sessaoId` lê o histórico daquela conversa para sempre. No escopo atual isso é aceitável — não há dado pessoal no chat, apenas perguntas sobre materiais de construção. Se o projeto passar a identificar clientes, esta decisão precisa ser revista junto com a autenticação.

**Sessão inexistente devolve 404, não lista vazia.** O caso de uso verifica a sessão antes de buscar as mensagens, mesmo sem usá-la depois. Sem essa checagem, um id errado devolveria `[]` — indistinguível de uma conversa que ainda não começou, e o frontend não teria como perceber que estava perguntando pelo lugar errado.

**Consultar não renova o TTL.** Mesma regra da [D-24](#d-24-ttl-da-sessão-é-renovado-a-cada-interação): leitura não deveria gravar nada. Quem renova é o envio de mensagem, que é a ação que de fato indica um cliente ativo.

**Onde no código.** `application/usecase/ConsultarHistoricoChatUseCase.java`, `presentation/controller/ChatController.java`.

---

### D-42. A varredura de TTL distingue carrinho abandonado de quem só encostou no totem

**Contexto.** Nada varria as sessões vencidas: elas ficavam `ACTIVE` no banco para sempre. O card previa transicioná-las "para `ABANDONED` ou `EXPIRED`", sem dizer qual em cada caso — e a [D-06](#d-06-encerrar-desdobrado-em-três-métodos-nomeados-por-evento) já havia criado os dois métodos exatamente para este consumidor.

**Decisão.**

- **Lista com itens → `ABANDONED`.** É o carrinho abandonado no sentido clássico do varejo: o cliente montou a lista e não concluiu. Uma venda que quase aconteceu.
- **Lista vazia ou inexistente → `EXPIRED`.** Alguém abriu a página — escaneou uma placa, olhou — e foi embora sem escolher nada. Nada estava em jogo.

> [!NOTE]
> **Ajustada em 25/08/2026.** O título e o exemplo falavam em totem, que não existe mais. A distinção continua valendo inteira: o que muda é o aparelho, não a diferença entre um carrinho abandonado e uma visita que não virou nada.

**Motivo.** Mandar tudo para `EXPIRED` seria mais simples, mas jogaria fora justamente a métrica que o produto promete melhorar. "Quantos clientes montaram uma lista e desistiram no meio" é um número que a loja quer ver; "quantos encostaram no totem" é outro, e misturá-los não ajuda ninguém.

**A varredura não protege regra de negócio nenhuma.** `Sessao.isValida()` já compara com o relógio, então uma sessão vencida é recusada mesmo que ninguém a tenha varrido. O job existe para que o **banco reflita a realidade**: sem ele, qualquer contagem de jornadas em andamento seria ficção.

**Regra no caso de uso, gatilho na infraestrutura.** `ExpirarSessoesInativasUseCase` decide o que fazer com cada sessão; `AgendadorDeExpiracao` apenas dispara. A regra fica testável sem envolver agendamento — é a mesma separação entre porta e adaptador usada no resto do projeto, só que aqui o "adaptador" é o relógio.

**Três detalhes que não são óbvios:**

1. **`fixedDelay`, não `fixedRate`.** O intervalo conta a partir do fim da execução anterior. Com `fixedRate`, uma varredura lenta começaria a se sobrepor à seguinte e duas passagens disputariam as mesmas sessões.
2. **O agendador engole a exceção.** O Spring **aposenta** uma tarefa agendada que lança — as execuções seguintes simplesmente param. Como o motivo mais provável de falha aqui é o banco estar momentaneamente fora, deixar a exceção subir mataria a varredura até o próximo restart.
3. **Cada sessão é tratada isoladamente**, com o `salvar` do adaptador abrindo a própria transação. Uma sessão problemática não impede a varredura das demais, e não há o que desfazer em bloco.

**Duas limitações aceitas, e por quê.**

**O job não roda com a aplicação dormindo.** Provedores gratuitos suspendem a instância por inatividade, então numa loja parada a varredura não acontece. É best-effort — e como o job não protege regra nenhuma, o custo é apenas o banco demorar mais para se acertar.

**Há uma janela de corrida de milissegundos** entre a varredura ler as sessões vencidas e gravá-las: se o cliente renovar exatamente nesse intervalo, a renovação se perde e a próxima ação dele recebe 409. Para isso acontecer ele teria que estar inativo há 4 horas e agir justo naquele milissegundo. Resolver exigiria bloqueio otimista — desproporcional ao risco.

**A varredura é naturalmente idempotente**, porque a consulta filtra por `ACTIVE`: a segunda passagem não encontra o que a primeira já tratou. Verificado contra o Oracle real.

**O guard da D-06 sob teste.** Uma sessão `COMPLETED` com TTL vencido **não** é sobrescrita. Sem isso, o cliente que concluiu a rota às 10h00 viraria `ABANDONED` às 10h05, e o sistema perderia a informação de que a jornada foi completada — que é a métrica de sucesso do produto.

**Onde no código.** `application/usecase/ExpirarSessoesInativasUseCase.java`, `infrastructure/scheduler/AgendadorDeExpiracao.java`, `infrastructure/config/AgendamentoConfig.java`.

---

### D-43. 2-opt sobre o Nearest Neighbor, na variante de caminho aberto

> [!NOTE]
> **Superada em 25/08/2026.** A rota calculada foi removida: a orientação da Leroy é mostrar apenas onde o cliente está e onde está cada produto, deixando o caminho por conta dele. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque é o trabalho técnico mais denso da Fase 3, e porque a medição que o justificou — 500 roteiros sorteados, ganho em 54% deles, piora em nenhum — é o exemplo de como o time decidiu tratar otimização: medindo antes de afirmar.

**Contexto.** A [D-26](#d-26-nearest-neighbor-como-heurística-de-roteamento) já registrava a limitação do vizinho mais próximo: por ser guloso, ele decide o melhor passo imediato sem enxergar o todo e às vezes "se pinta num canto" — pega o item mais perto da entrada e depois precisa voltar por onde veio. O caminho se cruza.

**Decisão.** Uma segunda etapa de **melhoria local 2-opt** sobre a rota construída: procurar dois trechos que se cruzam, inverter o pedaço entre eles — o que desfaz o cruzamento — e repetir enquanto houver ganho. É o par clássico *heurística construtiva + melhoria local*.

**A variante importa, e não é a que se vê nos exemplos.** O 2-opt é quase sempre ilustrado sobre um **ciclo fechado**, em que o caixeiro volta ao ponto de partida. A nossa rota é um **caminho aberto**: o cliente parte de um ponto fixo — o totem — mas não precisa voltar até ele. Isso muda o cálculo do ganho: quando a inversão vai até o último item da rota, existe aresta de **entrada** no trecho mas não de saída, e só a primeira entra na conta.

Tratar como ciclo faria o algoritmo otimizar um retorno à entrada da loja que ninguém vai percorrer — e, pior, rejeitar inversões boas no fim do caminho por causa de uma aresta imaginária.

**Por que só duas arestas entram na conta.** Inverter o trecho `[i..j]` troca no máximo a aresta que entra nele e a que sai. Todas as arestas internas continuam existindo, apenas percorridas ao contrário — e como a distância é simétrica, elas se cancelam. É isso que torna cada avaliação O(1) em vez de exigir recalcular a rota inteira.

**Medição, que é o que justifica o card.** Sobre 500 roteiros sorteados nas dez seções da planta real:

| | |
|---|---|
| Roteiros em que o 2-opt encontrou ganho | **54%** |
| Redução média quando encontra | **4,7%** |
| Maior redução observada | **19,7%** |
| Roteiros em que piorou | **0** |

E o número de vitrine subiu: no cenário de reforma de banheiro, a redução contra a ordem em que o cliente adicionou os itens passou de **38,6% para 41,0%**.

**O exemplo que explica o algoritmo em uma frase.** Com Jardim, Iluminação e Ferramentas no carrinho, o vizinho mais próximo começa por Jardim (36,50), o mais perto da entrada — e então precisa voltar para o oeste até Ferramentas (20,55) antes de atravessar a loja inteira até Iluminação (76,32). O 2-opt inverte o trecho: Ferramentas primeiro, Jardim de passagem a caminho do leste, Iluminação por último. **124,4 → 110,6 unidades, sem nenhuma volta.**

**Duas guardas que não são detalhe.**

1. **Limiar de ganho mínimo (`1e-9`).** As distâncias são raízes quadradas. Comparar dois `double` por "menor que" puro faria o algoritmo aceitar um ganho de `1e-15` e alternar para sempre entre dois caminhos de mesmo comprimento.
2. **Teto de 50 passadas.** Cada passada é O(n²) e o laço só continua enquanto houver ganho, então na prática ele para sozinho em poucas rodadas. O teto existe para que o carrinho sem limite da [D-17](#d-17-carrinho-de-roteiro-sem-limite-de-itens) nunca prenda a geração do QR Code.

**O agrupamento por corredor sobrevive.** Separar itens que dividem o mesmo `PontoMapa` sempre alonga o caminho — estão a distância zero entre si —, então o 2-opt nunca aceita essa inversão. É a propriedade que mais incomodaria o cliente se quebrasse: ele voltaria ao mesmo corredor duas vezes. Está sob teste, não deduzida.

**A garantia sob teste.** O teste de 500 roteiros sorteados (semente fixa, reproduzível) verifica que a rota refinada **nunca** é mais longa que a gulosa. Uma única piora significaria erro no cálculo do ganho — e o cliente andaria mais por causa da "otimização".

**Alternativa considerada.** Or-opt (mover um trecho para outra posição, sem inverter) resolveria casos que o 2-opt não alcança, como um item isolado que ficou no meio da rota. Descartada por ora: o card pedia 2-opt, a medição mostra que ele já entrega, e cada heurística a mais precisa ser justificada por um ganho medido, não por completude.

**Onde no código.** `domain/service/CalculadoraRota.java` — `refinarCom2Opt` e `ganhoDaInversao`.

---

### D-44. O token de handoff sai da URL, e o QR Code passa a ser regenerável

> [!NOTE]
> **Superada em 25/08/2026.** O handoff entre dispositivos foi removido: sem totem, a jornada inteira acontece no celular do cliente e nao ha transicao a autorizar. Ver [D-49](#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).
>
> O registro abaixo fica porque descreve raciocinio que continua valendo — e principalmente a licao de que **uma operacao com efeito colateral nao pode ser um GET**, que vale para qualquer endpoint futuro.

**Contexto.** Último card do backlog. Duas fragilidades conhecidas desde a Fase 1, registradas em [D-29](#d-29-uso-único-do-token-pela-ausência-no-banco): o token viajava na query string, e um QR Code que expirasse antes de ser escaneado obrigava o cliente a recomeçar.

---

#### O token sai da URL

**Decisão.** `GET /handoff/validate?token=...` virou `POST /handoff/validate` com o token no corpo. **Mudança quebrante**, combinada com a dupla de frontend antes de entrar, conforme a [D-25](#d-25-409-para-sessão-inativa-e-quando-é-aceitável-evoluir-o-contrato).

**Duas razões que se somam** — e a segunda é a mais forte:

1. URL fica gravada em histórico de navegador, em log de servidor e em cabeçalho `Referer`. Um token de 5 minutos ali é exposição desnecessária.
2. **A operação consome o token e renova a sessão.** Um `GET` com efeito colateral está errado independentemente de segurança: qualquer *prefetch* do navegador, robô de indexação ou clique duplo queimaria o QR Code do cliente. Isso não é hipótese remota — é o comportamento padrão de navegadores móveis.

**O caminho antigo foi retirado, não mantido em paralelo.** Deixar o `GET` funcionando "por compatibilidade" anularia o propósito: o token continuaria em URL sempre que alguém usasse a rota velha. Há teste verificando que a query string agora responde 405.

**O que isto NÃO resolve, e precisa ser dito.** O QR Code codifica a URL do PWA — `https://.../rota?token=...` —, então o token continua na URL **do frontend**, no histórico do celular do cliente. Nosso lado ficou limpo; o do navegador não. A saída é o frontend limpar a URL logo após ler o token (`history.replaceState`), registrado como observação para a dupla. Eliminar de vez exigiria um código curto opaco com consulta separada — mais escopo do que o card pedia.

---

#### Regeneração do QR Code

**Decisão.** Chamar `POST /handoff` de novo para a mesma sessão emite um token novo e invalida o anterior. Não foi preciso endpoint novo: `registrarTokenHandoff` já sobrescreve, e a busca por token deixa de encontrar o antigo. O que faltava era **tornar isso intencional, seguro e visível no contrato**.

**A regra que impede a regeneração de estragar a jornada.** Se a caminhada já começou — algum item marcado como coletado —, a rota **não** é recalculada. Recalcular partindo do totem renumeraria paradas que o cliente já visitou e embaralharia a navegação em curso. Regenerar precisa devolver o **acesso**, não reiniciar o **percurso**.

Verificado sobre HTTP: com o primeiro item já coletado, o QR regenerado devolve a mesma ordem e preserva o `coletado`. O cliente retoma exatamente de onde parou.

**Isto fecha o caso que a D-29 deixou em aberto:** a troca de aparelho. O celular novo escaneia um QR novo e continua a jornada.

**Expiração passa a ser distinguível.** `TokenHandoffExpiradoException` produz 401 com o rótulo `Token de Handoff Expirado`, separado de `Token de Handoff Invalido`. É o único modo de o Totem saber que basta gerar outro QR Code em vez de mandar o cliente montar a lista de novo.

**Isso não contradiz a mensagem genérica da D-27.** Só um token **de verdade, assinado por nós**, chega a ser reconhecido como expirado — quem forja recebe a resposta indistinguível de sempre. O que se revela a um atacante que já possui um token legítimo vencido é que ele está vencido, o que ele descobriria de qualquer forma. As demais falhas — adulterado, malformado, já consumido — continuam sem distinção entre si, de propósito.

---

**Efeito colateral no varredor de erros.** Depois desta mudança nenhum endpoint usa parâmetro de query obrigatório, então o handler de `MissingServletRequestParameterException` ficou sem caso vivo para exercitar. Ele permanece no lugar como guarda para endpoints futuros, e o `ErrosDeRequisicaoTest` passou a cobrir o corpo sem campo obrigatório e o 405 da rota retirada.

**Onde no código.** `presentation/controller/HandoffController.java`, `application/dto/ValidarHandoffRequest.java`, `application/usecase/GerarHandoffUseCase.java`, `infrastructure/security/GeradorTokenJwt.java`, `domain/exception/TokenHandoffExpiradoException.java`.

---

### D-45. O deploy mudou quais avisos do Hibernate importavam

**Contexto.** Três avisos apareciam no log desde a Fase 0 e eram ruído tolerável enquanto tudo rodava na mesma máquina. O primeiro deploy mudou a topologia — **aplicação em Ohio, banco em São Paulo, ~130 ms por ida e volta, e um décimo de núcleo de CPU** — e dois deles deixaram de ser cosméticos.

```
HHH90000025: OracleDialect does not need to be specified explicitly
spring.jpa.open-in-view is enabled by default
HHH100123: Low default JDBC fetch size: 10
```

---

**1. `open-in-view` desligado explicitamente.**

O padrão do Spring mantém a sessão do JPA aberta até a resposta ser serializada, para que carregamento preguiçoso ainda funcione na camada web. **Nesta arquitetura isso nunca serviu para nada:** os adaptadores convertem entidade JPA em objeto de domínio dentro da própria transação, com `@EntityGraph` trazendo o grafo completo, e **nenhuma entidade gerenciada chega à camada web** — o que sai do caso de uso é DTO montado a partir do domínio ([D-02](#d-02-casos-de-uso-devolvem-dto-não-entidade-de-domínio), [D-10](#d-10-entidades-jpa-espelho-separadas-das-de-domínio)).

Ou seja, o único efeito de deixá-lo ligado era **segurar a conexão com o banco por mais tempo do que o necessário** — o que numa instância de 512 MB com pool limitado é desperdício real.

Foi verificado, não deduzido: com a opção desligada, os DTOs mais aninhados do sistema foram exercitados sobre HTTP — detalhe de produto, lista de roteiro, rota do handoff, sugestão de ruptura e histórico do chat. Todos `200`, nenhum `LazyInitializationException`, nenhum `500`.

**2. O dialeto deixou de ser declarado.**

Declarar `org.hibernate.dialect.OracleDialect` fixa a variante genérica. Sem a propriedade, o Hibernate descobre o dialeto pela conexão e escolhe a **variante da versão real do banco** — Oracle 19.3, no nosso caso —, que gera SQL melhor. Tirar a linha não é só calar um aviso: é deixar o Hibernate usar a informação que ele já tem.

**3. `fetch_size` de 10 para 100.**

O padrão do driver Oracle faz o cursor buscar de dez em dez linhas, e **cada busca é uma ida e volta até o banco**. Localmente, com ~30 ms de latência, ninguém percebia. Com a aplicação publicada, uma página de 25 produtos passou a custar **três viagens de mais de 100 ms cada** em vez de uma.

O valor 100 não é arbitrário: é o teto de página que a busca de produtos permite ([D-03](#d-03-tipo-de-paginação-próprio-em-vez-de-page-do-spring)), então uma página cheia cabe numa viagem só.

---

**A lição que vale além destes três.** Aviso de framework não é lixo a ser silenciado, mas também não tem urgência fixa: **o mesmo aviso pode ser cosmético num ambiente e caro em outro.** Os três estavam no log desde o começo e só o deploy mostrou quais deles custavam dinheiro. Vale reler o log da aplicação publicada de vez em quando com esse olhar.

**Onde no código.** `backend/src/main/resources/application.yml`.

---

### D-46. O pool de conexões é dimensionado pelos limites reais do schema da FIAP

**Contexto.** O pool ficou no padrão do Hikari — **10 conexões** — até o primeiro deploy. Com a aplicação publicada usando a credencial de um integrante, passou a existir um segundo consumidor da mesma cota.

**Os limites, medidos e não presumidos.** Consultando `user_resource_limits` no próprio schema:

| Recurso | Limite |
|---|---|
| `SESSIONS_PER_USER` | **20** |
| `IDLE_TIME` | **30 minutos** |
| `CONNECT_TIME` | **240 minutos** |

E, no momento da medição, `select count(*) from v$session where username = user` devolveu **20**. Ou seja: **o teto já estava estourado**, com a instância publicada segurando 10 e a máquina local outras 10. A conexão seguinte teria recebido `ORA-02391`.

Não era risco futuro — era um defeito ativo, invisível porque ninguém tinha tentado usar as duas coisas ao mesmo tempo.

**Os 20 são por usuário do banco, não por equipe.** Cada integrante tem a sua cota. Como o deploy usa a credencial de um deles, a instância publicada e a máquina daquela pessoa dividem os mesmos 20 — os outros quatro não são afetados.

---

**Decisão.**

```yaml
hikari:
  maximum-pool-size: ${DB_POOL_SIZE:3}
  minimum-idle: 1
  max-lifetime: 1500000
  connection-timeout: 10000
```

**`maximum-pool-size` com padrão 3 e sobreposição por ambiente.** A instância publicada define `DB_POOL_SIZE=5`; a máquina de desenvolvimento fica com 3. Total de 8 dos 20, com folga real.

**Por que nunca 1.** O registro de ruptura roda em `REQUIRES_NEW` ([D-39](#d-39-a-ruptura-vira-registro-no-banco-e-o-relato-não-altera-o-estoque)) e precisa de uma **segunda** conexão enquanto a transação de fora ainda segura a primeira. Com pool de 1, esse fluxo travaria esperando uma conexão que só seria liberada quando ele mesmo terminasse — um impasse silencioso até estourar o tempo limite. Verificado: quatro rupturas simultâneas contra um pool de 5, todas `200`.

**`minimum-idle: 1`, e este é o ajuste menos óbvio.** No Hikari, `minimumIdle` vem por padrão **igual** ao tamanho do pool — o que significa que ele mantém todas as conexões vivas e **nunca devolve as ociosas**. Sem esta linha, uma máquina de desenvolvimento aberta durante o almoço continuaria segurando o pool inteiro.

**`max-lifetime` em 25 minutos, e este é o que evitaria uma falha real.** O padrão do Hikari é **30 minutos — exatamente o `IDLE_TIME` do Oracle**. Dois prazos empatados significam que o servidor pode matar a sessão no mesmo instante em que o pool ainda a considera boa. O Hikari valida no momento do uso e se recupera, mas ao custo de uma requisição lenta — justamente a primeira depois de um período parado, que num tier gratuito já é a pior de todas ([D-42](#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem)). Reciclar aos 25 garante que a conexão morra do nosso lado, de forma controlada.

**`connection-timeout` em 10 segundos** contra os 30 do padrão: com o pool esgotado, é melhor a requisição falhar rápido e legível do que a tela do cliente ficar meio minuto pensando.

---

**Medido depois da mudança.** Com `DB_POOL_SIZE=5` e tráfego real, a aplicação segurava **3 sessões** — o pool cresce sob demanda em vez de reservar o teto. Antes, o padrão de 10 mantinha as 10 abertas independentemente de uso.

**A lição.** O padrão de uma biblioteca é dimensionado para o caso comum dela, não para o nosso banco. Aqui bastavam três consultas ao próprio Oracle para trocar um chute por um número — e uma delas mostrou que o problema já estava acontecendo.

**Onde no código.** `backend/src/main/resources/application.yml`; `DB_POOL_SIZE` na tabela de [`deploy.md`](deploy.md).

---

### D-47. A massa ganhou pares de substituição, e a carga passou a ser incremental

**Contexto.** A ferramenta de simulação ([D-40](#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido)) permite zerar o estoque de **qualquer** produto, mas a massa tinha **um único par realmente substituível** — as duas lixas. Zerar qualquer outro item fazia o assistente recusar e devolver 422.

A recusa está correta: verificado zerando o Cano PVC, ele respondeu *"Nenhum dos produtos disponíveis na proximidade cumpre a função do cano de PVC para instalação hidráulica"* — sifão e torneira não substituem um cano. Certo pelo desenho, **mas não é a cena que se quer gravar**, e a demonstração ficava presa a um único produto.

---

#### Os pares acrescentados

Quatro produtos, cada um cumprindo a **mesma função** de um item já existente **na mesma seção**, variando só em especificação — que é como a substituição acontece numa loja de verdade:

| Produto em falta | Substituto acrescentado | Seção |
|---|---|---|
| Lâmpada LED 9W - kit 3 | **Lâmpada LED 12W - kit 3** | Iluminação |
| Sifão Sanfonado Universal | **Sifão Copo Cromado Universal** | Encanamento |
| Trena 5m | **Trena 7,5m** | Ferramentas |
| Argamassa AC-II 20kg | **Argamassa AC-III 20kg** | Materiais de construção |

Quatro seções espalhadas pela loja, para a demonstração poder partir de qualquer canto do mapa — somadas ao par original em Tintas, são **cinco cenários**.

**O que faz o cenário valer a pena não é o par existir, é ter concorrência.** Se o substituto fosse o único candidato no raio, o modelo não estaria escolhendo nada. Medido contra o Oracle real:

| Cenário | Candidatos no raio |
|---|---|
| Sifão Sanfonado | 10 |
| Lixa Grão 120 | 9 |
| Trena 5m | 6 |
| Lâmpada LED 9W | 4 |
| Argamassa AC-II | 2 |

Confirmado com o Gemini real, na instância publicada: *"A trena de 7,5 metros cumpre perfeitamente a mesma função de medição, oferece um alcance ainda maior e está bem aqui na mesma prateleira."*

---

#### A carga precisou virar incremental

**O obstáculo.** O carregador só rodava com o catálogo **vazio** ([D-16](#d-16-carga-inicial-em-java-em-vez-de-sql)). Isso significava que os quatro produtos novos **nunca chegariam** aos bancos que já tinham a massa antiga — inclusive o da instância publicada. O card entregaria nada.

**Decisão.** Cada seção e cada produto passa a ser criado **apenas se ainda não existir**, em vez de tudo-ou-nada:

- **Produtos:** uma única leitura paginada do catálogo monta o conjunto de SKUs existentes, e só os ausentes são inseridos. Uma consulta a mais no startup, não uma por produto — o que importa agora que aplicação e banco estão a 5.000 km ([D-45](#d-45-o-deploy-mudou-quais-avisos-do-hibernate-importavam)).
- **Seções:** resolvidas pelo corredor entre os `PontoMapa` do tipo `PRATELEIRA` que já existem. Sem isso, cada reinício criaria dez seções novas e os produtos de um corredor ficariam divididos entre dois pontos do mapa.
- **Pontos de serviço:** criados só se não houver nenhum daquele tipo. Um totem duplicado mudaria a origem da rota ([D-28](#d-28-a-rota-parte-do-primeiro-ponto-do-tipo-totem)).

**A guarda contra o caso que quebraria tudo.** A leitura tem teto de 1000 produtos. Se o catálogo passar disso, o conjunto de SKUs conhecidos ficaria incompleto e a carga tentaria inserir SKU que já existe, violando a chave única. Então ela **se recusa a rodar** e avisa no log, em vez de arriscar.

**Consequência boa, além do card:** acrescentar produto à massa passou a ser uma linha, e ela chega sozinha a todos os bancos no próximo restart. Antes exigiria cada integrante apagar o catálogo.

---

**Um defeito que o teste encontrou.** Os contadores do log viviam em campos do componente — que é um *singleton* do Spring. Rodando a carga uma segunda vez, eles se somavam e o log dizia *"4 produtos criados"* quando não havia criado nenhum. O banco estava certo; o relato, não. Agora a contagem vive numa instância por execução.

Não teria aparecido em produção, onde o `ApplicationRunner` roda uma vez só — apareceu porque o teste de idempotência chama a carga de novo, que é exatamente o tipo de uso que estado mutável compartilhado não suporta.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java`, `src/test/java/.../CargaDeDadosIntegracaoTest.java`.

---

### D-48. Gravar pelo agregado é seguro aqui — e a investigação que provou isso depois de duas hipóteses erradas

**Contexto.** `ListaRoteiroRepositoryAdapter.salvar` recebe um `ListaRoteiro` de domínio, **reconstrói o grafo de entidades JPA inteiro** e faz `merge`. Com `orphanRemoval` ligado ([D-13](#d-13-cascata-e-remoção-de-órfãos-no-agregado-da-lista)), essa forma parece perigosa: bastaria o objeto de domínio estar desatualizado para que o `merge` sobrescrevesse valores novos e o `orphanRemoval` apagasse itens que aquela visão não conhecia.

Numa revisão de código, essa leitura levou à conclusão de que havia um defeito real de perda de dado sob concorrência. **A conclusão estava errada**, e a investigação que mostrou isso vale mais registrada do que a hipótese.

---

#### Por que a escrita é segura

**Leitura e gravação acontecem na mesma transação.** Todo caso de uso que grava o roteiro carrega o agregado logo antes, dentro do próprio `@Transactional`. As entidades ficam **gerenciadas** no mesmo contexto de persistência.

E o Hibernate, ao decidir o que escrever, **não compara com o banco** — compara com o *snapshot* tirado no momento em que carregou. Um campo que não mudou em memória simplesmente não entra no `UPDATE`. Logo, um item que outra requisição alterou nesse intervalo **nunca é tocado**, porque para esta transação ele nunca mudou.

O `merge` do grafo reconstruído também não estraga isso: as entidades já estão no contexto, então ele copia valores idênticos sobre elas e nada fica sujo.

**Onde o perigo seria real:** fazer `merge` de um grafo remontado num contexto de persistência **vazio** — quando ninguém carregou o agregado antes na mesma transação. Aí o `merge` dispara um `SELECT`, encontra o estado atual do banco e o sobrescreve com os valores da visão velha. Nenhum caso de uso faz isso hoje. O [`open-in-view: false`](#d-45-o-deploy-mudou-quais-avisos-do-hibernate-importavam) ajuda a manter assim, ao impedir que um agregado sobreviva fora da transação que o carregou.

---

#### As duas hipóteses que não sobreviveram à medição

A exigência do time foi explícita: *provar que a mudança melhorou algo, ou reconhecer que foi placebo*. Foram necessárias três tentativas para chegar a uma medição que separasse um comportamento do outro.

**1. Sondas no nível do repositório — falharam, mas provavam a coisa errada.** Carregar o agregado duas vezes em objetos separados e salvar em sequência de fato destrói dados: a segunda gravação desfaz a primeira e apaga o que a primeira inseriu. Só que **cada `salvar` ali abria a própria transação, com o contexto vazio** — a situação que nenhum caso de uso produz. As sondas confirmaram uma propriedade do repositório isolado, não um defeito do sistema.

**2. Concorrência com *threads* — passou dos dois lados.** Duas requisições disparando juntas contra um banco local raramente se sobrepõem na janela exata. **Corrida não é prova reproduzível**: um teste que passa com e sem a mudança não mede a mudança.

**3. Contagem de linhas escritas — idêntica dos dois lados.** A hipótese era que gravar pelo agregado reescrevia todos os irmãos. Medido com as estatísticas do Hibernate: **1 update em ambos**, tanto num roteiro de 3 itens quanto num de 8. O *dirty checking* já evitava o excesso.

**4. O teste que finalmente distinguiu.** Forçar a obsolescência sem depender de sorte: a operação externa roda numa transação que o teste controla e ainda não confirmou, enquanto uma segunda operação roda e confirma numa transação própria. Quando a externa grava, o banco já mudou embaixo dela. Resultado: **nada se perde** — e a explicação está na seção acima.

---

#### O que foi feito, e o que foi desfeito

A correção proposta — estreitar as escritas para item a item, em vez de gravar o agregado — **foi implementada e depois revertida**. Ela não mudava nenhum comportamento observável, acrescentava dois métodos a uma porta de domínio e, no caminho, introduziu um defeito próprio: apagar a linha do item diretamente não funciona quando o agregado já está carregado na mesma transação, porque no *flush* o Hibernate encontra o filho ainda na coleção do pai gerenciado e o **ressuscita**. Mudança sem ganho medido é risco sem contrapartida.

**O que ficou** foram os testes, que passaram a fixar o comportamento verificado:

| Teste | O que protege |
|---|---|
| `EscritaObsoletaIntegracaoTest` | gravação com visão desatualizada não perde nada |
| `AmplitudeDeEscritaIntegracaoTest` | cada operação escreve uma linha, independente do tamanho do roteiro |
| `ConcorrenciaNoRoteiroIntegracaoTest` | duas requisições simultâneas no mesmo roteiro |
| `ContratoOpenApiIntegracaoTest` | o contrato escrito à mão descreve o que a API expõe |
| `FalhaDeInfraestruturaTest` | banco fora do ar não vaza detalhe interno ao cliente |

**A lição.** Ler o código e concluir que há um defeito não é o mesmo que demonstrar o defeito. Aqui a inferência era plausível, o mecanismo perigoso existe de verdade — e mesmo assim o sistema estava correto, por uma razão que só apareceu ao medir. **Sem a exigência de prova, teríamos commitado uma correção elegante para um problema inexistente, com uma justificativa bem escrita para acompanhá-la.**

**Onde no código.** `infrastructure/database/adapter/ListaRoteiroRepositoryAdapter.java`, `infrastructure/database/factory/ListaRoteiroFactory.java`, e os cinco testes acima.

---

### D-49. O escopo revisado retirou o totem e a rota calculada

**Contexto.** Na mentoria de 24/08/2026, os representantes técnicos da Leroy Merlin deram duas orientações que mudaram a raiz do produto. As duas vieram de perguntas que o time levou preparadas.

**1. Sem totem.** A jornada inteira passa a acontecer no celular do cliente, numa página web. Ele entra escaneando um **QR Code afixado na loja**, e cada QR carrega a posição onde está colado — é assim que o sistema sabe de onde ele partiu.

**2. Sem rota calculada.** Perguntamos se otimizar o percurso não trabalharia contra a venda por impulso, já que a loja é organizada para o cliente passar por coisas que não veio buscar. A resposta foi que a Leroy já tem um circuito próprio, e a orientação foi **mostrar apenas a posição do cliente e a de cada produto da lista**, deixando o caminho por conta dele.

**Isso aproxima o projeto do desafio.** O enunciado da FIAP nomeia o "problema do último metro" — achar o produto exato num layout complexo. Mostrar onde tudo está **é** a resposta a isso; a rota otimizada era acréscimo nosso. E some o totem, que era a parte menos realista da proposta.

---

**O que sai, e o que isso custa.** Três remoções, feitas em cards separados:

| O que sai | Decisões que passam a descrever algo inexistente |
|---|---|
| Ponto de interesse no meio da rota | [D-31](#d-31-ponto-de-interesse-não-é-persistido) |
| Handoff entre dispositivos, com JWT de uso único | [D-08](#d-08-o-domínio-registra-o-token-de-handoff-nunca-o-assina), [D-27](#d-27-segredo-do-jwt-por-ambiente-com-chave-aleatória-em-desenvolvimento), [D-29](#d-29-uso-único-do-token-pela-ausência-no-banco), [D-44](#d-44-o-token-de-handoff-sai-da-url-e-o-qr-code-passa-a-ser-regenerável) |
| Nearest Neighbor e refinamento 2-opt | [D-26](#d-26-nearest-neighbor-como-heurística-de-roteamento), [D-28](#d-28-a-rota-parte-do-primeiro-ponto-do-tipo-totem), [D-43](#d-43-2-opt-sobre-o-nearest-neighbor-na-variante-de-caminho-aberto) |

Junto vai o número de vitrine de **41% de redução de percurso**, que era um dos dois argumentos técnicos mais fortes para a banca. Não é desperdício: é o preço de ouvir quem conhece a operação da loja. **As decisões antigas ficam no documento**, marcadas como superadas — apagá-las esconderia o raciocínio que levou até aqui, e a banca pode perguntar exatamente isso.

**A ordem das remoções não é arbitrária.** O ponto de interesse depende da ordem de caminho que o handoff calcula, e o handoff é o único consumidor do algoritmo de rota. Removendo nessa sequência — ponto de interesse, handoff, rota — nenhum estado intermediário fica quebrado. A ordem inversa deixaria o endpoint de ponto de interesse respondendo com ordem nula entre um card e outro.

---

**O que precisa sobreviver às remoções.** Uma lógica não óbvia mora dentro do caso de uso do ponto de interesse e **não pode ser perdida**: a posição atual do cliente é inferida do **último item marcado como coletado**; se nada foi coletado, ele ainda está a caminho da primeira parada.

No escopo novo essa regra vale igual, com uma origem a mais: **a posição atual é a do último item coletado; na ausência dele, a do QR Code escaneado.** Ela renasce como conceito da própria `Sessao` — que é onde deveria ter estado desde o começo, em vez de detalhe interno de um desvio de rota.

**O que sobrevive inteiro.** Busca tolerante a erro de digitação, detalhe de produto, lista de compras sem limite, assistente de IA, tratamento de ruptura com substituto, marcação de coletado, ciclo de vida da sessão e toda a infraestrutura publicada.

**Onde no código.** As remoções acontecem nos três primeiros cards de [`backlog-escopo-revisado.md`](backlog-escopo-revisado.md). O escopo novo está descrito em [`fluxo-do-cliente.md`](fluxo-do-cliente.md).

---

### D-50. Sem rota, a lista passa a ser agrupada por seção

**Contexto.** A ordem dos itens da lista vinha do campo `ordemCaminho`, gravado pelo algoritmo de rota. Removido o algoritmo, o campo perde sentido — mas a lista não pode simplesmente **ficar sem ordem**: a coleção vem do banco por um `@OneToMany` sem `@OrderBy`, e o SQL não garante ordem nenhuma. Na prática, a lista de compras do cliente poderia se rearranjar a cada consulta.

**Decisão.** `ListaRoteiro.getItensParaExibicao()` ordena por **seção da loja** e, dentro dela, por **nome do produto**.

O nome do método mudou de propósito: `getItensOrdenados` sugeria uma ordem de percurso, que é justamente o que deixou de existir.

**Por que agrupar por seção, e não por ordem de inclusão.** O cliente agora monta o próprio caminho, e para isso ele precisa enxergar o que está junto. Ver *três itens em Tintas* numa linha só é o que permite decidir passar por Tintas; os mesmos três espalhados pela lista escondem exatamente a informação que ele usaria.

**Alternativa considerada.** Ordem de inclusão, que é o comportamento clássico de lista de compras. Descartada por dois motivos: exigiria uma coluna nova de timestamp (os UUIDs são aleatórios, não guardam sequência), e ordenaria pelo que o cliente fez **antes** de entrar na loja, quando o que importa agora é onde as coisas estão.

**Consequência assumida.** Dois produtos da mesma seção com o mesmo nome ficariam em ordem indefinida entre si. Irrelevante: o SKU é único e a massa não tem nomes repetidos.

**Onde no código.** `domain/entity/ListaRoteiro.java`.

---

### D-51. Um valor de enum removido precisa sumir também do banco

**Contexto.** `TipoPonto.TOTEM` saiu do enum junto com a rota. Mas `PontoMapaEntity.tipo` é `@Enumerated(EnumType.STRING)`: a linha `tipo = 'TOTEM'` continua gravada nos bancos de quem já rodou a versão anterior — incluindo o da instância publicada — e **qualquer leitura que a alcance falha na conversão para o enum**.

Hoje nada a alcança: os pontos só são lidos por id ou por tipo, e nenhum dos dois chega até ela. A armadilha estouraria no card da geometria da loja, que precisa listar todos os pontos para desenhar o mapa — longe daqui, e sem pista de onde veio.

**Decisão.** A carga inicial apaga, a cada execução, os pontos gravados com tipo aposentado. A lista de tipos vive numa constante (`TIPOS_APOSENTADOS`) e a remoção é feita por **consulta nativa**.

**Por que nativa, e não pelo JPA.** Qualquer leitura via JPA tentaria converter a coluna para o enum e falharia justamente nas linhas que precisam ser removidas. O `delete` acontece pelo nome da coluna, sem passar pelo mapeamento.

**Por que na carga inicial.** Ela é o único lugar do sistema que já tem a responsabilidade de deixar o banco compatível com o código atual, e roda em todos os ambientes — o de cada integrante e o publicado. Com `ddl-auto: update` e sem Flyway ([D-16](#d-16-carga-inicial-em-java-em-vez-de-sql)), não existe migração onde pendurar uma limpeza como esta.

**O `@Transactional` fica no repositório, não no chamador.** Uma consulta `@Modifying` exige transação, e a carga chama o método de dentro da própria classe — auto-invocação não passa pelo proxy do Spring, então a anotação no `ApplicationRunner` seria silenciosamente ignorada e a operação falharia por falta de transação.

**Onde no código.** `infrastructure/database/repository/PontoMapaJpaRepository.java` e `infrastructure/database/seed/CarregadorDadosIniciais.java`.

---

### D-52. O código curto do QR Code é normalizado na gravação, não só na busca

**Contexto.** Cada QR Code afixado na loja carrega a posição onde está colado. Mas o adesivo rasga, a câmera falha e nem todo cliente sabe usar leitor de QR — daí o **código curto digitável** ao lado, no formato `TIN-02`.

**Decisão.** O código vive em `PontoMapa`, como coluna própria: nulo em prateleira, caixa e banheiro; preenchido só em pontos `QR_CODE`. E o `PontoMapa` **normaliza no construtor** — maiúsculas, sem separadores — de modo que o banco guarda `TIN02` e a busca normaliza a digitação antes de consultar.

**Por que normalizar na entrada, e não só na consulta.** Normalizar apenas ao buscar deixaria o banco aceitar `TIN-02` e `tin 02` como linhas diferentes, e a constraint `unique` da coluna passaria a não garantir nada — dois adesivos poderiam existir com o "mesmo" código. Normalizando na construção, **a unicidade da coluna é a unicidade do código.**

O hífen do adesivo vira, então, pura tipografia: existe para o código ser legível e ditável, e o sistema não depende dele.

**Por que em `PontoMapa` e não numa entidade própria.** Um ponto de QR Code é um ponto do mapa com coordenada, como qualquer outro — reaproveita a busca, a resposta REST e o desenho do mapa inteiros. Uma entidade separada duplicaria coordenada e rótulo para ganhar apenas um campo obrigatório.

**Alternativa considerada.** Guardar a forma impressa (`TIN-02`) e normalizar na consulta com `regexp_replace`. Descartada por perder a unicidade real e por impedir o uso do índice.

**Consequência assumida.** Trocar o formato impresso depois exige recarregar os códigos. Aceitável: os adesivos são físicos e trocá-los já é trabalho de campo.

**Onde no código.** `domain/entity/PontoMapa.java` — `normalizarCodigo`; `infrastructure/database/adapter/PontoMapaRepositoryAdapter.java`.

---

### D-53. A aplicação repara a restrição de enum que o `ddl-auto: update` deixa envelhecer

**Contexto.** Trocar `TOTEM` por `QR_CODE` no enum `TipoPonto` fez o banco recusar toda gravação de ponto de QR Code, com `ORA-02290`. A causa levou um tempo para aparecer:

```
SYS_C006965723  C  (tipo in ('PRATELEIRA','BANHEIRO','CAIXA','TOTEM'))
```

Para uma coluna `@Enumerated(EnumType.STRING)`, o Hibernate **cria** uma restrição de verificação com os valores do enum. Mas `ddl-auto: update` só acrescenta: nunca altera nem remove o que já existe. A restrição congela no dia da criação da tabela, e no dia em que um valor entra ou sai do enum o banco passa a recusar gravações perfeitamente válidas — com um erro que não diz qual coluna nem por quê.

**Isso não era um problema do card.** Era uma armadilha para **toda** mudança futura de enum, em cada schema do time e na instância publicada.

**Decisão.** Um componente compara, no startup, os literais da restrição com `values()` do enum. Se divergirem, ele derruba a restrição e a recria a partir do enum.

**Por que refazer em vez de apenas apagar.** Apagar resolveria o erro e perderia a garantia. Refazer mantém a proteção no banco e faz a próxima mudança de enum se resolver sozinha — que é o ponto: o problema não é este valor, é o mecanismo.

**Por que na aplicação, e não num `ALTER` combinado com o time.** Sem Flyway ([D-16](#d-16-carga-inicial-em-java-em-vez-de-sql)) não há migração onde pendurar isso. Cada integrante tem o próprio schema, e há ainda o publicado — pedir que cada um rode o comando à mão deixaria o ambiente de alguém quebrado sem aviso, e o do Render quebrado no deploy seguinte.

**Duas guardas.**

1. **Só age quando diverge.** Comparar antes evita DDL a cada inicialização, que seria ruído no log e tempo a mais num cold start que já leva mais de dois minutos ([D-45](#d-45-o-deploy-mudou-quais-avisos-do-hibernate-importavam)).
2. **Falhar aqui não derruba a aplicação.** Se o `ALTER` for recusado — permissão, lock —, fica um aviso no log e a subida continua. O erro reapareceria na primeira gravação de qualquer forma, e agora com uma pista.

**A ordem em relação à [D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco) não é acidental.** As linhas de tipo aposentado são apagadas **antes**: uma linha com valor fora do enum faria o `add check` ser recusado.

**Onde no código.** `infrastructure/database/schema/RestricaoDeEnumNoBanco.java`.

---

### D-54. A entrada aceita o código da placa num campo só, e código desconhecido não recusa a sessão

**Contexto.** O cliente entra pela placa afixada no corredor. Existem dois caminhos até a mesma tela: escanear o QR — que já leva o código na URL — ou digitar a URL impressa e informar o código à mão, quando escanear não dá certo.

**A razão de a URL estar impressa na placa.** O código curto sozinho não resolvia nada. Se o único acesso ao sistema fosse o QR Code, quem não conseguisse escanear não teria **onde** digitar o código — o plano B só funcionaria para quem já tinha executado o plano A. A URL legível é o que torna o código alcançável.

**Decisão.** `POST /sessoes` recebe um corpo opcional com **um campo só**, `codigoPonto`. Os dois planos chegam pelo mesmo caminho, e o backend não sabe — nem precisa saber — qual deles o cliente usou.

**Alternativa considerada.** Aceitar o identificador do ponto **ou** o código curto, em dois campos. Descartada: dois caminhos para a mesma coisa, dois conjuntos de erro possíveis, e nenhum ganho — o QR pode perfeitamente codificar o código curto, que é mais legível na URL e ainda serve para quem digita.

**Código desconhecido não é erro de cliente.** Placa velha, loja remanejada, erro de digitação. A sessão nasce **sem posição** e continua inteiramente utilizável: catálogo, lista, assistente e coleta funcionam. Devolver 404 aqui barraria a entrada no sistema por causa de um adesivo — o cliente ficaria sem nada, quando poderia ficar apenas sem o "você está aqui".

Fica um `INFO` no log com o código recusado, que é o que permite descobrir uma placa arrancada sem depender de alguém reclamar.

**Onde no código.** `application/usecase/InicializarSessaoUseCase.java`, `application/dto/IniciarSessaoRequest.java`.

---

### D-55. A posição do cliente vem de duas pistas, e vale a mais recente

**Contexto.** Sem GPS dentro da loja, a posição nunca é medida — é sempre deduzida de algo que o cliente fez. Existem duas pistas, e o sistema não tem outra:

- **a placa lida** — ele estava ali quando escaneou ou digitou o código;
- **o último item coletado** — ele esteve na prateleira daquele produto.

**Decisão.** Vale a **mais recente das duas**, comparadas por data. `PosicaoDoCliente.estimar(sessao, lista)` faz essa escolha, no domínio, sem conhecer banco nem HTTP.

**Por que comparar, em vez de eleger uma preferida.** Dar preferência fixa à coleta quebraria o recentrar: quem se perdeu e leu uma placa nova veria o marcador de volta na prateleira do último item que pegou. Dar preferência fixa à placa quebraria o caso normal: o cliente andaria a loja inteira e continuaria marcado na entrada.

**O que isso exigiu de `ItemRoteiro`.** O campo `coletado` era um booleano, e "o **último** item coletado" precisa de ordem. Até a Fase 3 essa ordem vinha de `ordemCaminho`, que saiu junto com o cálculo de rota ([D-50](#d-50-sem-rota-a-lista-passa-a-ser-agrupada-por-seção)). O booleano virou **`coletadoEm`**, um instante — e `isColetado()` passou a ser derivado dele. O contrato não mudou: continua expondo apenas `coletado`, porque o cliente precisa saber se já pegou, não quando.

**A marcação é idempotente pela primeira confirmação.** Tocar duas vezes, ou a rede reenviar, não pode mover a posição do cliente — nem para frente nem para trás. `marcarComoColetado` só grava quando o campo ainda está nulo.

**Consequência assumida.** A posição é uma estimativa, e pode estar velha: o cliente que pegou a tinta e caminhou dez metros continua marcado em Tintas até fazer outra coisa. É o limite honesto do que se sabe sem hardware de posicionamento. O sistema autoral de posicionamento por bússola e acelerômetro continua reservado para o NEXT, como a [D-21](#d-21-demo-da-banca-por-simulação-animada-não-posicionamento-real) registrou — e o que existe hoje é justamente o que torna a demonstração possível sem ele.

**Onde no código.** `domain/service/PosicaoDoCliente.java`, `domain/entity/ItemRoteiro.java`, `domain/entity/Sessao.java`.

---

### D-56. A coluna `coletado` continua sendo gravada, mesmo redundante

**Contexto.** Com `coletadoEm` no domínio ([D-55](#d-55-a-posição-do-cliente-vem-de-duas-pistas-e-vale-a-mais-recente)), a coluna booleana `coletado` passou a ser dedutível: basta olhar se o instante é nulo.

**Decisão.** A entidade JPA grava **as duas colunas**. O domínio continua sendo fonte única — as duas saem do mesmo `ItemRoteiro`.

**Por que não simplesmente remover a coluna antiga.** Ela nasceu `NOT NULL` e sem valor padrão. Como `ddl-auto: update` nunca remove nada, parar de gravá-la faria **todo insert de item falhar** nos bancos que já existem — o de cada integrante e o publicado. O erro apareceria como violação de restrição no primeiro produto adicionado à lista, longe da mudança que o causou.

**É a terceira vez que o mesmo mecanismo cobra.** Linhas de tipo aposentado ([D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco)), restrição de enum congelada ([D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer)) e agora uma coluna que não dá para aposentar. O padrão vale registrar: **com `ddl-auto: update`, o esquema só cresce** — toda mudança precisa ser pensada como adição, ou vir com uma reparação explícita.

**Onde no código.** `infrastructure/database/entity/ItemRoteiroEntity.java`.

---

### D-57. O mesmo código inválido é aceito na entrada e recusado no recentrar

**Contexto.** Dois endpoints recebem o código da placa: `POST /sessoes`, quando o cliente entra, e `PUT /sessoes/{id}/posicao`, quando ele se perde e lê outra. Um código que não corresponde a placa alguma — adesivo velho, loja remanejada, erro de digitação — pode chegar nos dois.

**Decisão.** Comportamentos opostos, de propósito:

| | Código desconhecido |
|---|---|
| `POST /sessoes` | **aceita** — a sessão nasce sem posição e continua utilizável |
| `PUT /sessoes/{id}/posicao` | **404** — a posição anterior continua valendo |

**O que muda entre os dois é o custo de recusar.** Na entrada, o cliente ainda não tem nada: recusar o barraria do sistema inteiro por causa de um adesivo. Ele perderia catálogo, busca, lista e assistente — tudo que funciona perfeitamente sem saber onde ele está. O preço de aceitar é apenas um mapa sem "você está aqui".

No recentrar, ele **já tem** uma sessão funcionando. Dizer "não encontramos essa localização" é acionável: ele confere a placa e tenta de novo. E aceitar em silêncio seria pior do que recusar — ele acharia que funcionou, e continuaria vendo o marcador no lugar errado sem entender por quê.

**A regra por trás, que vale além destes dois endpoints:** *recusar só quando o cliente pode fazer algo a respeito, e o silêncio custaria mais que o erro.*

**Consequência para o frontend.** As duas telas precisam tratar o mesmo dado de formas diferentes, e a da entrada é a que engana: como não há erro HTTP, ela precisa **perceber `posicaoAtual` nula e avisar**. Registrado em [O-19](observacoes.md#o-19-a-entrada-tem-um-plano-b-e-ele-é-uma-tela-que-ainda-não-existe).

**Por que `PUT` e não `POST`.** A operação define o valor de um recurso — onde o cliente está —, e reenviar "estou em CEN-03" duas vezes leva ao mesmo lugar. Cliente ou proxy podem repetir sem risco, que é o que se quer de alguém andando por uma loja com sinal ruim.

**Onde no código.** `application/usecase/RecentrarSessaoUseCase.java`, `application/usecase/InicializarSessaoUseCase.java`.

---

### D-58. A planta da loja não vive no banco, e é dela que saem as coordenadas das seções

**Contexto.** A tela de mapa é o centro do produto revisado, e o frontend não tem como construí-la sozinho: ninguém no time desenha planta em SVG. O backend precisa servir a geometria — cada corredor como um retângulo, no mesmo grid `0..100` em que já vivem os produtos, as placas e a posição do cliente.

**Decisão 1: a planta é código, não tabela.** `PlantaDaLoja` é uma lista constante de `BlocoMapa` no domínio.

O reflexo seria criar `TB_BLOCO_MAPA`, porque todo o resto do mapa está no banco. Mas **a planta descreve um prédio, não um dado de aplicação**: não muda em execução, nada a referencia por chave estrangeira e nenhum caso de uso a escreve. Persisti-la custaria entidade de domínio, entidade JPA, factory, porta, adapter, repositório e carga — mais uma quarta oportunidade de esbarrar nas armadilhas do `ddl-auto: update` ([D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco), [D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer), [D-56](#d-56-a-coluna-coletado-continua-sendo-gravada-mesmo-redundante)) — para entregar exatamente o mesmo conteúdo. E mudar a planta continuaria sendo mudar código, porque a carga é código.

**Decisão 2, que é a que importa: a carga deriva as seções da planta.** A coordenada de cada ponto de prateleira é o **centro do bloco** correspondente, e não um número digitado ao lado.

A promessa do card era "mapa e produtos não podem divergir". Escrever as duas listas em paralelo e verificar que batem seria uma promessa mantida por disciplina — alguém acrescenta uma seção, esquece o bloco, e o teste avisa depois. Derivando, **a divergência deixa de ser possível**: o ponto da seção *é* o centro do bloco, e acrescentar uma seção começa por acrescentar um bloco.

**Os blocos foram desenhados em volta das coordenadas que a massa já tinha**, e não o contrário. Assim a carga incremental continua não tocando no que existe, e nenhum banco do time muda de estado por causa deste card.

**Decisão 3: o endpoint não conhece sessão.** `GET /mapa` devolve a mesma coisa para todo mundo. O que varia entre um cliente e outro — onde ele está, o que escolheu — vem das respostas de sessão e de lista.

Isso não é purismo: é o que permite ao frontend buscar o mapa uma vez e guardar no aparelho. Dentro de uma loja de 10.000 m², a conexão cai — e a tela mais importante do produto continuar desenhando é a diferença entre um app inútil e um app com dado velho.

**O que os testes protegem.** Sobreposição entre blocos é o defeito mais traiçoeiro: dois corredores desenhados um sobre o outro fazem o produto de um aparecer visualmente dentro do outro, e o cliente procura na seção errada — exatamente o problema que o produto existe para resolver. Também está sob teste que **nenhuma placa de QR cai dentro de um bloco**, porque a decisão do [card dos QR Codes](#d-52-o-código-curto-do-qr-code-é-normalizado-na-gravação-não-só-na-busca) é que elas ficam em corredor de passagem.

**Consequência assumida.** Blocos são retângulos alinhados aos eixos. Uma loja real tem recortes, corredores diagonais e ilhas — nada disso é representável. É deliberado: retângulos com rótulo bastam para o cliente reconhecer onde está, e polígonos arbitrários custariam trabalho de frontend que o time não tem para dar.

**Onde no código.** `domain/entity/PlantaDaLoja.java`, `domain/entity/BlocoMapa.java`, `application/usecase/ConsultarMapaUseCase.java`.

---

### D-59. A carga completa a apresentação de produtos que já estavam gravados

**Contexto.** `Produto` ganhou descrição e URL de imagem. A carga inicial é **incremental** e nunca reescreve um SKU que já existe ([D-47](#d-47-a-massa-ganhou-pares-de-substituição-e-a-carga-passou-a-ser-incremental)) — foi assim que produtos novos passaram a chegar aos bancos com a massa antiga.

Aqui a mesma regra vira armadilha: os 29 produtos criados antes destes campos **nunca receberiam descrição**. Em todos os schemas do time e no publicado, o catálogo ficaria com a tela de detalhe vazia — sem erro nenhum, sem nada no log.

**Decisão.** Depois de criar o que falta, a carga faz um segundo passo: para cada produto do catálogo declarado, **preenche descrição e imagem que estiverem nulas** no banco.

**Só preenche o que está vazio, nunca sobrescreve.** Se alguém ajustar um texto direto no banco, a próxima inicialização não desfaz. O custo é que corrigir um typo na descrição do código não se propaga sozinho para bancos que já a receberam — aceitável, e a alternativa (sobrescrever sempre) apagaria trabalho manual sem avisar.

> [!NOTE]
> **Esta regra foi revista em 25/08/2026.** O custo que aqui era aceitável deixou de ser quando os nomes reais dos produtos entraram na massa: a carga passou a **sobrescrever** nome, descrição e imagem. Ver [D-69](#d-69-a-massa-passou-a-ser-a-fonte-do-nome-e-da-descrição-e-sobrescreve-o-banco).

**A quarta vez que o mesmo padrão aparece.** [D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco), [D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer), [D-56](#d-56-a-coluna-coletado-continua-sendo-gravada-mesmo-redundante) e agora esta. Sem Flyway e com `ddl-auto: update`, **a carga inicial é o único lugar do sistema que pode reconciliar banco e código** — e toda mudança em dado existente precisa passar por ela explicitamente.

**Imagem nula é estado normal, não defeito.** As URLs vêm do site público da Leroy e são coletadas à mão pelo time ([O-18](observacoes.md#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens)), de forma incremental. O contrato marca o campo como anulável e o teste verifica que um produto sem foto continua respondendo nome, descrição e localização. A coleta está organizada em [`imagens-dos-produtos.md`](imagens-dos-produtos.md).

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java` — `completarApresentacoes`; `domain/entity/Produto.java` — `comApresentacao`.

---

### D-60. Uma consulta montada substitui as duas buscas de catálogo

**Contexto.** Existiam dois caminhos de busca: `buscarPaginado` (JPA, para navegar) e `buscarPorTermo` (nativa com `UTL_MATCH`, para buscar por nome). O card dos filtros pedia **seção** e **disponibilidade** — e a decisão do time foi ir mais longe, até atributos por categoria.

Com `@Query` fixas, cada filtro novo **dobra** o número de variantes: dois filtros já dariam quatro consultas, e cada faceta dobraria de novo.

**Decisão.** Uma implementação só, montada em SQL nativo no adaptador, com predicados acrescentados conforme o filtro. `buscarPaginado` e `buscarPorTermo` **continuam na porta** e delegam para ela — os dois expressam intenções diferentes (listar tudo para a carga, buscar sem filtro para fundamentar o assistente de IA), e mantê-los evitou mexer no caminho da IA, cujos testes já são frágeis por causa da cota ([O-01](observacoes.md#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)).

**Montar SQL exige dizer o que protege contra injeção.** Nenhum valor vindo do cliente entra na string: o SQL só cresce com trechos **constantes**, decididos por `if`, e todo valor vai como parâmetro nomeado. O que varia é a forma da consulta, nunca o conteúdo dela.

**Três detalhes do Oracle que viraram teste.**

1. **`''` é `NULL` no Oracle.** Usar string vazia como sentinela de "sem filtro" funcionaria aqui por acidente e quebraria em qualquer outro banco. `FiltroDeProdutos` normaliza branco para `null` **no domínio**, e o predicado simplesmente não é montado.
2. **Sem termo não há similaridade a ordenar.** Chamar `UTL_MATCH` com nulo não significa nada, então a ordenação alterna: por similaridade quando há termo, alfabética quando não há — que é o que se espera ao navegar.
3. **A paginação passou a ser aplicada pelo Hibernate** sobre a consulta montada, em vez de vir do `Pageable` do Spring Data. Está sob teste que a segunda página não repete a primeira e que o total de páginas bate.

**Combinação sem resultado devolve página vazia, não 404.** O cliente filtrou demais; ele não pediu um recurso inexistente. A tela mostra "nenhum produto encontrado" com os filtros ainda ali para ele afrouxar um.

**O filtro de disponibilidade tem padrão falso.** O catálogo mostra produto zerado de propósito: a ruptura tem tratamento próprio no produto, e escondê-la apagaria o cenário que a demonstração encena. O filtro serve a quem quer só o que dá para levar hoje.

**Onde no código.** `infrastructure/database/adapter/ProdutoRepositoryAdapter.java` — `buscar`; `domain/repository/FiltroDeProdutos.java`.

---

### D-61. As seções do menu saem do catálogo, não da planta

**Contexto.** `GET /produtos/secoes` alimenta a navegação por corredor. Os nomes existem em dois lugares: os blocos de `PlantaDaLoja` ([D-58](#d-58-a-planta-da-loja-não-vive-no-banco-e-é-dela-que-saem-as-coordenadas-das-seções)) e o campo `corredor` dos pontos de prateleira.

**Decisão.** A lista é derivada dos **produtos**, agrupando por corredor, com a contagem de cada seção.

**Por quê.** Uma seção sem produto é um beco sem saída num menu: o cliente toca, espera ver algo e encontra vazio. Derivando do catálogo, ela simplesmente não aparece — sem precisar de regra para isso.

Não há risco de divergir da planta: o teste do mapa já garante que os dois conjuntos de nomes são iguais.

**A contagem não estava no card, e vale o custo.** É ela que faz um menu de navegação valer: *"Tintas (5)"* diz ao cliente se vale entrar antes de ele gastar um toque para descobrir. Sai de graça, na mesma consulta agrupada.

**Onde no código.** `application/usecase/ListarSecoesUseCase.java`, `domain/repository/SecaoDoCatalogo.java`.

---

### D-62. As caracteristicas dos produtos vivem numa tabela, nao em colunas

**Contexto.** O time decidiu levar os filtros ate onde um e-commerce de material de construcao chega: marca, medida, amperagem, grao. A alternativa barata — marca e faixa de preco como colunas — foi apresentada e recusada conscientemente, sabendo que esta e a maior peca isolada de backend do projeto.

**O que impede colunas.** Os atributos **nao sao os mesmos para todo produto**: amperagem so existe em disjuntor, grao so em lixa, temperatura de cor so em lampada. Como colunas, `TB_PRODUTO` ficaria larga e quase toda nula — e cada caracteristica nova exigiria alterar a tabela.

**Decisao.** `TB_PRODUTO_ATRIBUTO` com `(produto_id, chave, valor)`, chave unica por produto.

**A chave e um enum, e nao texto livre.** Tres motivos: o rotulo de exibicao vive junto da chave, entao o frontend nao mantem traducao; a massa nao consegue gravar `Marca` e `marca` como coisas diferentes; e a lista fechada permite ordenar os filtros de forma previsivel. **A ordem de declaracao do enum e a ordem de exibicao** — marca primeiro, porque e o filtro mais usado numa loja de construcao, e medidas depois.

O custo dessa escolha e a restricao de verificacao que o Hibernate cria para colunas de enum — e ela ja tem quem cuide, desde a [D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer).

**A semantica do filtro sai da forma da consulta.** Valores da mesma chave viram um `IN`, portanto "ou"; chaves diferentes viram `EXISTS` separados encadeados por `AND`, portanto "e". Um unico `JOIN` com todos os valores num `IN` daria so o "ou", e marcar *Tigre* mais *Bitola 25 mm* devolveria tudo que fosse Tigre **ou** 25 mm — que nao e o que ninguem espera de um filtro. Esta sob teste.

**As caracteristicas so aparecem no detalhe.** Carrega-las junto de cada item de uma listagem custaria uma consulta por produto, e a aplicacao esta a 5.000 km do banco ([D-45](#d-45-o-deploy-mudou-quais-avisos-do-hibernate-importavam)). Na tela de um produto so, uma consulta a mais nao pesa.

**A carga compara antes de gravar.** Reescrever os atributos de todo produto a cada inicializacao seriam dezenas de idas ao banco por boot. Uma leitura unica resolve, e a gravacao so acontece no que mudou — o log confirma "nada a carregar" a partir da segunda execucao.

Diferente das descricoes ([D-59](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados)), aqui a massa **sobrescreve** em vez de so completar: atributo e dado estruturado que alimenta filtro, e um valor divergente no banco quebraria a faceta em silencio.

**As marcas sao reais**, e coerentes com o produto. A Leroy vende essas marcas, e um catalogo com marca inventada nao se parece com uma loja — pelo mesmo motivo de os precos serem plausiveis.

**O que isso cobra.** Cada produto novo passa a exigir caracteristicas junto, o que agrava o esforco de massa que a [O-18](observacoes.md#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens) ja registrava como apertado.

**Onde no codigo.** `domain/entity/AtributoProduto.java`, `infrastructure/database/seed/AtributosDaMassa.java`, `infrastructure/database/adapter/ProdutoRepositoryAdapter.java`.

---

### D-63. As facetas ignoram a escolha do cliente sobre elas mesmas

**Contexto.** A tela de catalogo precisa saber **quais filtros oferecer**. A lista nao pode ser fixa: mostrar "Amperagem" para quem navega em Tintas e pior do que nao mostrar filtro nenhum — o cliente experimenta e nada acontece.

**Decisao.** As facetas sao calculadas a cada busca, sobre o resultado. E o recorte usado tem uma sutileza que decide se o filtro e agradavel ou irritante:

> As facetas saem do filtro **com** termo, secao e disponibilidade, mas **sem** as caracteristicas ja escolhidas.

**Por que ignorar a propria escolha.** Calculando sobre o resultado final, escolher *Tigre* faria as outras marcas sumirem da lista — e para trocar para *Docol* o cliente teria que limpar o filtro primeiro. E o comportamento que mais incomoda num filtro de e-commerce, e o teste que o protege esta escrito exatamente nesses termos.

**A contagem vai junto de cada valor**, e o valor mais comum vem primeiro: e o que o cliente provavelmente procura, e a contagem evita que ele escolha uma opcao que devolve um produto so.

**Pagina e facetas vem na mesma resposta.** Um endpoint separado obrigaria o celular a repetir todos os filtros na URL e a fazer duas viagens a cada toque — dentro de uma loja, com sinal ruim. O `Catalogo` repete os campos da pagina em vez de aninha-los, para que o consumidor continue lendo `content` no mesmo lugar.

**Chave desconhecida no filtro e ignorada, nao recusada.** Ela vem de um link antigo ou de uma caracteristica que saiu do sistema, e o cliente nao tem o que fazer a respeito: ignorar apenas alarga o resultado, enquanto um 400 deixaria a tela em branco por causa de um parametro que ele nem sabe que existe. Mesmo criterio da [D-57](#d-57-o-mesmo-código-inválido-é-aceito-na-entrada-e-recusado-no-recentrar).

**Onde no codigo.** `domain/repository/FacetaDeProdutos.java`, `domain/repository/FiltroDeProdutos.java` — `semAtributos`.

---

### D-64. Desmarcar um item não precisa mexer na posição do cliente

**Contexto.** Tocar por engano num celular, andando por uma loja, e comum — e ate agora nao havia volta: o item ficava marcado, e ainda **arrastava a posicao do cliente para uma prateleira onde ele nunca esteve**.

O card previa o cuidado: *"a posicao vem do ultimo item coletado; desmarcar precisa reverte-la de forma coerente — para o item marcado anterior, ou para o ultimo QR escaneado se nao houver nenhum"*.

**Decisao.** `desmarcarComoColetado()` apaga o instante da coleta. **E so.**

**Por que nao ha nada a reverter.** A posicao nunca foi gravada em lugar nenhum: ela e deduzida, a cada consulta, do item coletado mais recente comparado com a placa lida ([D-55](#d-55-a-posição-do-cliente-vem-de-duas-pistas-e-vale-a-mais-recente)). Apagando o instante, o item simplesmente deixa de ser candidato, e a deducao encontra sozinha o anterior — ou a placa, se nao houver nenhum.

**E o caso que uma reversao manual erraria.** Um codigo que guardasse "a posicao anterior" para restaurar acertaria o caso simples e erraria este: o cliente pega a tinta, se perde, le a placa do cruzamento, depois pega algo em Jardim. Desfazer a coleta de Jardim deve deixa-lo **no cruzamento** — onde ele comprovadamente esteve depois de Tintas —, e nao de volta em Tintas. A comparacao por data acerta isso sem saber que o caso existe. Esta sob teste.

**A licao, que vale alem deste card.** Modelar o **fato** — *quando* o item foi coletado — em vez da **conclusao** — *onde* o cliente esta — foi o que fez a reversao sair de graca. Se `Sessao` guardasse `posicaoAtual` como campo, este card seria uma maquina de estados com casos de borda.

**Idempotente por natureza.** Apagar um campo ja nulo nao faz nada, entao rede reenviando ou toque duplo nao viram erro — sem precisar de guarda.

**Espelha o marcar, inclusive na renovacao da sessao:** corrigir um engano e atividade do cliente como qualquer outra.

**Onde no codigo.** `domain/entity/ItemRoteiro.java` — `desmarcarComoColetado`; `application/usecase/DesmarcarItemColetadoUseCase.java`.

---

### D-65. Aceitar o substituto e uma acao so, e o substituto entra nao coletado

**Contexto.** O tratamento de ruptura sugeria um substituto e parava ali. Aceitar exigiria do cliente **duas acoes** — adicionar um produto e remover outro — em pe no corredor, com o celular na mao.

**Isso nao e detalhe de conveniencia.** A promessa do recurso e converter prateleira vazia em venda ([D-23](#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp)). Enquanto aceitar da trabalho, a conversao nao acontece e o recurso vira decoracao — bonito na demonstracao, inutil na loja.

**Decisao.** `POST /roteiro/itens/{itemId}/substituir` faz a troca inteira numa chamada: o substituto entra e o item que faltou sai.

**O substituto entra NAO coletado, e nao herda o estado do item que saiu.** E o detalhe que decide se o cliente sai da loja com o produto: o substituto nem sempre esta na mesma prateleira, e pode estar alguns metros adiante. Marca-lo como coletado faria o cliente ir embora sem ele — e mentiria sobre onde ele esta ([D-55](#d-55-a-posição-do-cliente-vem-de-duas-pistas-e-vale-a-mais-recente)).

**O produto vem no corpo, e nao e deduzido da sugestao.** Dois motivos: o assistente pode responder diferente numa segunda chamada, e a troca precisa valer sobre o que o cliente **viu na tela**; e ele nao fica preso a sugestao — se encontrou outra coisa na prateleira que resolve, pode trocar por ela.

**O registro da ruptura nao e tocado.** Ele e evidencia do que aconteceu na gondola, e continua valendo tenha o cliente aceitado a troca ou nao. Mais que isso: **comparar quantas rupturas viraram troca e o que diz a loja se as sugestoes estao boas** — apagar o registro destruiria exatamente o dado que o recurso existe para produzir. O registro passa a apontar para um item que ja nao existe, e tudo bem: ele descreve o que aconteceu, nao o estado de agora.

**Uma guarda que evita apagar um item em silencio.** Trocar um produto por ele mesmo devolve 409. Sem ela, o `adicionarProduto` devolveria o item existente ([D-18](#d-18-produto-duplicado-é-ignorado-no-carrinho)) e o `removerProduto` o apagaria em seguida — o cliente ficaria sem o produto sem ter pedido isso.

**Onde no codigo.** `application/usecase/SubstituirItemDoRoteiroUseCase.java`.

---

### D-66. Cada produto da massa e declarado uma vez, inteiro

**Contexto.** O catalogo passou de 29 para **111 produtos**, cerca de onze por secao. Com cinco por corredor, paginacao nao paginava, faceta nao filtrava e corredor nao parecia corredor.

**O problema que aparecia ao acrescentar em escala.** A massa estava espalhada: nome e preco no carregador, descricao ao lado deles, caracteristicas noutro arquivo. Acrescentar oitenta produtos assim seria editar dois lugares em paralelo — e **esquecer as caracteristicas de um produto nao daria erro nenhum**: ele simplesmente sumiria do filtro no dia em que alguem escolhesse qualquer marca.

**Decisao.** `CatalogoDaMassa` declara cada produto numa entrada so, com SKU, nome, secao, preco, estoque, descricao e caracteristicas juntos. A carga le dessa fonte unica para criar, completar apresentacao e sincronizar atributos.

**O que isso torna impossivel.** Produto sem descricao ou sem caracteristica deixa de ser um esquecimento silencioso e passa a ser visivel na propria entrada — e ha teste exigindo que todo produto tenha ao menos marca e tipo.

**Uma guarda na carga.** Produto que declara uma secao inexistente na planta ([D-58](#d-58-a-planta-da-loja-não-vive-no-banco-e-é-dela-que-saem-as-coordenadas-das-seções)) e ignorado com aviso no log, em vez de estourar. A massa de demonstracao nao pode impedir a aplicacao de subir.

**Um unico produto continua nascendo com estoque zero** — a lixa grao 120 —, e e ele que encena a ruptura. Manter esse cenario intacto ao ampliar o catalogo foi a parte dificil, e esta na [D-67](#d-67-o-teto-de-candidatos-da-ruptura-envelheceu-com-o-catalogo).

**As marcas sao reais**, decidido com o time: a Leroy vende essas marcas, e um catalogo com marca inventada nao se parece com uma loja — pelo mesmo motivo de os precos serem plausiveis.

**Onde no codigo.** `infrastructure/database/seed/CatalogoDaMassa.java`, `ProdutoDaMassa.java`.

---

### D-67. O teto de candidatos da ruptura envelheceu com o catalogo

**Contexto.** A pre-filtragem espacial da ruptura ([D-38](#d-38-ruptura-de-estoque-o-modelo-escolhe-mas-quem-responde-é-o-banco)) manda ao assistente os produtos disponiveis mais proximos, limitados a um teto. Ele era **10**, dimensionado quando uma secao tinha de dois a cinco produtos — nessa escala, dez candidatos naturalmente atravessavam varios corredores.

**O que quebrou.** Ao passar para onze produtos por secao, um teste falhou: **a trena de 7,5 m saiu da lista de candidatos da trena de 5 m**.

O motivo e sutil e vale entender. Todos os produtos de uma secao compartilham a coordenada do bloco, entao **empatam em distancia** — e o desempate acaba sendo o nome. "Trena" esta no fim do alfabeto de Ferramentas, e caiu fora do top 10.

**O diagnostico e maior que o sintoma.** Com uma dezena de produtos por corredor, um teto de 10 **nunca sai do corredor atual**. Isso anula a razao de existir da pre-filtragem espacial: ela passaria a oferecer o que esta ao lado na prateleira, e nao o que esta perto na loja — que e outra coisa.

**Decisao.** Teto de **20**. Cobre a secao inteira com folga e ainda deixa entrar o corredor vizinho, que e exatamente o que a busca por proximidade promete.

**Sob teste, e nao por confianca.** Entrou uma verificacao de que os candidatos alcancam **mais de um corredor** — a propriedade que se perdeu, e nao o numero que a restaurou. Medido a partir de Tintas: os candidatos chegam a Eletrica.

**O que isso ensina sobre constantes dimensionadas.** O 10 nao estava errado quando foi escolhido; ele envelheceu com o dado. Uma constante que depende do tamanho da massa merece um teste que verifique a **propriedade** que ela deveria garantir, e nao o valor dela.

**Onde no codigo.** `application/usecase/TratarRupturaEstoqueUseCase.java` — `LIMITE_DE_CANDIDATOS`.

---

### D-68. O substituto é escolhido por semelhança antes de proximidade

**Contexto.** A pré-filtragem espacial ordenava os candidatos por distância. Isso funcionava com 29 produtos — mas **todos os produtos de uma seção compartilham a coordenada do bloco** ([D-58](#d-58-a-planta-da-loja-não-vive-no-banco-e-é-dela-que-saem-as-coordenadas-das-seções)), então empatam em distância e o desempate acabava sendo o nome.

Com onze produtos por corredor, *"o mais próximo"* virou na prática **"o primeiro do corredor em ordem alfabética"**.

**Como isso apareceu.** A jornada completa reescrita imprimiu a sugestão, e ela era absurda: **bandeja de pintura para uma lixa**. Medido nos cinco pares plantados na massa, o resultado foi **errado em cinco de cinco**:

| Em falta | Esperado | O que saía |
|---|---|---|
| Lixa grão 120 | Lixa d'água 150 | Bandeja para Pintura |
| Lâmpada LED 9W | Lâmpada LED 12W | Arandela Externa |
| Sifão sanfonado | Sifão copo | Caixa Sifonada |
| Trena 5m | Trena 7,5m | Alicate Universal |
| Argamassa AC-II | Argamassa AC-III | Areia Ensacada |

**Por que isso é grave, e não cosmético.** O primeiro candidato é exatamente o que o cliente recebe **quando o assistente está fora do ar ou a cota estourou** ([D-35](#d-35-o-cliente-de-ia-falha-explicitamente-o-fallback-é-de-quem-chama)) — e o tier gratuito permite cinco chamadas por minuto ([O-01](observacoes.md#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)). É o caminho **mais provável de rodar durante a banca**.

**Decisão.** A consulta passa a ordenar por **mesmo tipo, depois mesma marca, depois distância, depois nome**. A afinidade vem de `AtributoProduto.TIPO` e `MARCA`, que existem desde os filtros por característica ([D-62](#d-62-as-caracteristicas-dos-produtos-vivem-numa-tabela-nao-em-colunas)) — o dado já estava lá, faltava usá-lo.

**Tipo pesa mais que marca** porque duas lixas continuam sendo lixas mesmo de fabricantes diferentes, enquanto duas Norton podem ser uma lixa e uma serra.

**Por que na consulta, e não reordenando depois.** O teto de candidatos é aplicado pelo banco. Reordenar em memória chegaria tarde: os semelhantes já teriam sido cortados — que é precisamente o defeito.

**Degrada sem quebrar.** Produto sem tipo nem marca faz os dois `CASE` devolverem 1 para todos, e a ordem cai de volta para distância e nome. Massa incompleta não vira erro em produção, e está sob teste.

**Uma nuance que a medição revelou, e vale registrar.** Dos cinco pares, **três foram resolvidos pela marca, não pelo tipo**: `Lixa para parede` e `Lixa d'água` são tipos diferentes, assim como `Sifão sanfonado`/`Sifão copo` e `Argamassa AC-II`/`AC-III`.

Isso expõe uma tensão real: `TIPO` serve a **dois propósitos**. Como faceta de filtro, o cliente quer que seja específico — distinguir AC-II de AC-III é útil na hora de escolher. Como sinal de afinidade, quereríamos algo mais geral.

Não foi resolvido porque não há defeito a corrigir: o resultado está certo nos cinco casos. Se um dia aparecer um par de marcas diferentes com tipos específicos distintos, a saída é uma categoria acima do tipo — e aí sim com um caso concreto para justificá-la.

**O assistente também ganha com isso.** A lista que chega ao modelo passa a vir ordenada por semelhança, então os candidatos mais plausíveis sobrevivem ao teto de 20 ([D-67](#d-67-o-teto-de-candidatos-da-ruptura-envelheceu-com-o-catalogo)).

**Onde no código.** `domain/repository/AfinidadeDeProduto.java`, `infrastructure/database/repository/ProdutoJpaRepository.java`.

### D-69. A massa passou a ser a fonte do nome e da descrição, e sobrescreve o banco

**Contexto.** O time coletou as fotos dos produtos no site da Leroy e, no caminho, trocou os nomes inventados da massa pelos **nomes reais** dos produtos que fotografou. Isso obrigou a corrigir junto o atributo `MARCA` de 13 produtos — um espelho batizado *Gavix* não pode ter *Evolux* na ficha técnica — e quatro medidas que descreviam outro item.

Aí a armadilha apareceu. Os dois caminhos de reconciliação da carga tinham regras opostas:

| O que | Como chegava a um banco que já tinha o produto |
|---|---|
| Atributos (`MARCA`, medidas) | **Sobrescreve sempre** ([D-62](#d-62-as-caracteristicas-dos-produtos-vivem-numa-tabela-nao-em-colunas)) |
| Nome | **Nunca** — a carga não reescreve SKU existente ([D-47](#d-47-a-massa-ganhou-pares-de-substituição-e-a-carga-passou-a-ser-incremental)) |
| Descrição e imagem | Só se estivessem nulas ([D-59](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados)) |

O resultado seria um banco meio corrigido: **marca nova embaixo de nome velho**, em todos os schemas do time e no publicado, que é o que a banca vê. Sem erro, sem log, visível só olhando a tela.

**Decisão.** A massa é a fonte de nome, descrição e imagem, e a carga **sobrescreve** os três quando divergem. `completarApresentacoes` virou `sincronizarApresentacoes`, e ficou com a mesma forma que os atributos já tinham: comparar o declarado com o gravado, e gravar quando diferente.

**O que se perde.** A proteção que a [D-59](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados) dava a um texto ajustado à mão direto no banco. Deixou de valer a pena: ninguém edita descrição por SQL, e o preço de manter a proteção era não conseguir corrigir nada depois de gravado. **Editar a massa no código passou a ser o único caminho** — e é o certo, porque é o que está versionado.

**O que continua igual.** A carga não recria produto que existe, não mexe em preço nem em estoque, e imagem nula segue sendo estado normal ([O-18](observacoes.md#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens)).

**A quinta vez que o mesmo padrão aparece.** [D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco), [D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer), [D-56](#d-56-a-coluna-coletado-continua-sendo-gravada-mesmo-redundante), [D-59](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados) e agora esta. Sem Flyway e com `ddl-auto: update`, **a carga é o único lugar que reconcilia banco e código** — e a lição que se repete é que *qualquer* campo deixado de fora dela envelhece em silêncio.

**Medido.** Rodando contra o Oracle da FIAP, a carga relatou 22 apresentações e 19 conjuntos de atributos atualizados num banco que já tinha os 111 produtos. Dois testes seguram a regra: um compara nome e descrição de todos os produtos com o que a massa declara, outro altera um produto à mão, recarrega e verifica que voltou.

**Uma tentativa que a suíte recusou, e fez bem.** Cinco dos nomes reais não declaram marca nenhuma, e a primeira versão tirou o atributo `MARCA` deles por coerência. O teste que exige marca em todo produto falhou — e a razão dele está escrita na própria justificativa: **produto sem marca desaparece assim que o cliente escolhe qualquer marca no filtro**. Os cinco voltaram a ter a marca inventada, que não contradiz nada porque o nome real não menciona marca alguma. Está anotado em [`imagens-dos-produtos.md`](imagens-dos-produtos.md#o-que-entrou-junto-com-os-nomes-reais--aplicado-em-25082026) que a marca verdadeira desses cinco está no campo *Marca* da página do site.

**Sobre os acentos, que entraram junto.** Os nomes reais têm acento e a massa não tinha nenhum. Medido no Oracle: o banco é `AL32UTF8`, o texto volta idêntico e o Maven já compila em UTF-8. O `LIKE` deixa de achar quem digita sem acento — `flexivel` não encontra *Flexível* —, mas o `JARO_WINKLER` entre as duas formas ficou entre **85 e 94**, bem acima do corte de 70 da busca ([D-15](#d-15-query-nativa-com-utl_match-para-busca-tolerante-a-erro-de-digitação)). A busca continua achando, por semelhança em vez de correspondência exata. Existe saída se um dia incomodar — `convert(nome, 'US7ASCII')` dos dois lados do `LIKE` —, e não foi aplicada porque não há caso falhando.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java` — `sincronizarApresentacoes` e o mapa `IMAGENS`; `domain/entity/Produto.java` — `comApresentacao`.

---

### D-70. Renomear uma seção é migração, não edição de string

**Contexto.** A massa inteira foi escrita sem acento, e isso vazava para a tela do cliente em cinco lugares: chips de seção, rótulos de faceta, descrições, nomes de produto e a mensagem que o assistente mostra quando está fora do ar.

Não era só feiura. O frontend indexa os metadados de setor por **nome acentuado** — `'Elétrica'`, `'Iluminação'`, `'Decoração'`, `'Materiais de construção'` —, com igualdade exata. Quatro dos dez cartões de setor caíam num texto genérico, *"Itens do catálogo Leroy Merlin"*, porque o nosso dado não casava. O Bielecky escreveu o português certo; o dado errado era o nosso.

**A armadilha.** `carregarOuCriarSecoes` casa seção existente **pelo nome do corredor**. Trocar `Decoracao` por `Decoração` na [`PlantaDaLoja`](#d-58-a-planta-da-loja-não-vive-no-banco-e-é-dela-que-saem-as-coordenadas-das-seções) faria a carga concluir que a seção não existe e **criar um ponto novo, vazio**. Os dez produtos de Decoração, presos ao ponto antigo pela chave estrangeira, continuariam no ponto de nome velho.

O resultado seria a seção aparecendo **duas vezes no mapa**: uma com todos os produtos e nome errado, outra com o nome certo e nada dentro. Sem erro, sem log, em todos os bancos do time e no publicado.

**Decisão.** Um mapa `CORREDORES_RENOMEADOS` (nome antigo → nome atual) aplicado **antes** de `carregarOuCriarSecoes`, que atualiza `TB_PONTO_MAPA.corredor` no lugar por consulta nativa.

**Vale para todo corredor, não só para seção — e pelo motivo oposto.** A placa da Iluminação e o ponto do banheiro são casados por código curto e por tipo, então nunca duplicariam. Justamente por isso o texto antigo ficaria gravado para sempre: a carga só os cria quando não existem, e nunca reescreve. E é texto que o cliente lê na tela de localização — *Sanitarios*, *Corredor leste, junto a Iluminacao*.

**Renomear preserva o id**, então a chave estrangeira dos produtos não se move e nada mais precisa mudar. A ordem importa: depois de `carregarOuCriarSecoes`, o ponto de nome novo já teria sido criado e a renomeação esbarraria na unicidade do corredor.

**Renomear não pode ser cego, e isso custou um banco sujo para aprender.** O servidor de desenvolvimento estava rodando com *hot reload* durante a edição: a cada compilação ele reiniciava e executava a carga. Numa dessas execuções a planta já estava acentuada e a migração ainda não existia, então a carga criou as quatro seções novas **vazias**. Quando a migração entrou, ela renomeou por cima e o banco ficou com **duas linhas por seção** — uma com todos os produtos e nome novo, outra vazia com o mesmo nome novo.

Por isso a migração apaga antes de renomear: prateleira com o nome de destino e **nenhum produto dentro** é resíduo de execução parcial, ninguém a referencia, e a carga a recria em seguida se ela de fato pertencer à planta. Com isso o passo virou idempotente — roda quantas vezes for, em qualquer estado, e converge.

**A sexta vez que o mesmo padrão aparece.** [D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco), [D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer), [D-56](#d-56-a-coluna-coletado-continua-sendo-gravada-mesmo-redundante), [D-59](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados), [D-69](#d-69-a-massa-passou-a-ser-a-fonte-do-nome-e-da-descrição-e-sobrescreve-o-banco) e agora esta. Sem Flyway e com `ddl-auto: update`, **a carga é o único lugar que reconcilia banco e código** — e a lição que se repete é que renomear qualquer coisa usada como chave de busca é migração, mesmo quando parece edição de texto.

**Uma entrada aqui pode sair um dia.** Quando nenhum banco tiver mais o nome antigo, a linha vira peso morto. Não há como saber isso do código, então elas ficam até alguém confirmar que todo mundo rodou a versão nova — o custo de manter é uma consulta que não atualiza nada.

**A ponta que ficou do outro lado.** `findSectorForProduct`, no `mapService.js`, casa a seção contra `secaoRef` **e** contra o nome do bloco. Depois desta mudança, Decoração, Elétrica e Iluminação passam a casar pelo nome do bloco, que já era acentuado — sem regressão. Mas **Materiais de construção deixa de casar**: o `secaoRef` está sem acento e o bloco se chama *Material de Construção*, no singular. São duas strings no frontend, junto do `secaoRef` do Encanamento, que nunca casou.

**Onde no código.** `infrastructure/database/seed/CarregadorDadosIniciais.java` — `CORREDORES_RENOMEADOS` e `renomearCorredores`; `infrastructure/database/repository/PontoMapaJpaRepository.java` — `renomearCorredor`.

---

### D-71. O corredor viaja na listagem, e não só o id do ponto

**Contexto.** `ProdutoResponse` levava `pontoMapaId` — um UUID — e mais nada sobre onde o produto fica. O nome do corredor só existia no detalhe, dentro de `pontoMapa`.

O frontend, sem ter de onde tirar o nome, imprimiu um texto fixo: **`Corredor da Loja`**, nos 50 cards do catálogo, em cada item do roteiro e nas respostas do assistente. O cliente não tinha como saber que ali deveria haver um corredor de verdade — dado ausente vestido de conteúdo.

**Por que isso é o produto, e não um detalhe.** *"Em que corredor ele está"* é a frase que separa o Merlin de um e-commerce qualquer. Ela sumiu exatamente das duas telas onde importa: a que o cliente usa para navegar e a lista com que ele anda pela loja.

**Decisão.** A listagem passa a levar o **nome do corredor** junto do id.

**Por que no backend e não cruzando no frontend.** A tela já busca `GET /mapa` e poderia casar cada produto pelo `pontoMapaId`. Mas aí *cada* tela que mostrasse produto precisaria lembrar de cruzar — catálogo, roteiro, resultado de busca, sugestão de substituto —, e esquecer significa voltar a imprimir o texto genérico, sem erro nenhum que denuncie. O dado é do backend, e quem esquece de cruzar é quem paga.

**Custo real: nenhum.** A consulta do catálogo já faz `join tb_ponto_mapa` para poder filtrar por seção ([D-60](#d-60-uma-consulta-montada-substitui-as-duas-buscas-de-catálogo)), então o corredor já vinha carregado — ele só não estava sendo exposto. Nenhuma consulta a mais, nenhum campo novo em banco.

**Nulo continua sendo estado válido.** Produto sem ponto no mapa existe: a carga avisa e segue quando uma seção não está na planta. O campo é anulável no contrato, e há teste para o caso.

**O `pontoMapaId` continua vindo.** O mapa precisa dele para casar o marcador com o bloco; o nome é para o texto. Os dois têm usos diferentes e não se substituem.

**Onde no código.** `application/dto/ProdutoResponse.java`; `src/main/resources/openapi/openapi.yaml` — schema `Produto`, herdado por `ProdutoDetalhado` via `allOf`.

---

### D-72. O produto da ruptura nasce com estoque

**Contexto.** A lixa grão 120 nascia com estoque zero, e era isso que disparava a demonstração de ruptura. Fazia sentido enquanto ninguém tinha decidido se produto esgotado podia entrar no roteiro.

Quando a tela chegou, a resposta veio pronta: o botão *adicionar ao roteiro* vem `disabled` para produto sem estoque. E o time confirmou a regra. Com ela, **a lixa zerada nunca chegaria à lista**, e o cenário mais importante da apresentação não teria como começar.

**Decisão.** A lixa passa a nascer com **4 unidades**. Produto esgotado continua não entrando no roteiro.

**Não há contradição, e a história fica melhor.** A ruptura que este sistema trata nunca foi "o app sabia que estava esgotado e deixou eu adicionar" — isso é constrangedor. É **"o estoque dizia que tinha e a prateleira estava vazia"**, que é a divergência que a [D-23](#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp) assume desde o começo que *vai* acontecer. O cliente adiciona o produto porque o sistema afirma que ele existe, caminha até lá, e descobre a falta na gôndola.

**O backend já funcionava assim.** `TratarRupturaEstoqueUseCase` nunca verificou estoque zero em lugar nenhum: ele recebe um item do roteiro e trata como "o cliente foi até lá e não achou". Só a massa estava desenhada para o modelo antigo — a mudança foi um número.

**Dois produtos passam a nascer zerados.** Sem nenhum, o filtro *apenas disponíveis* deixaria de mudar qualquer coisa na tela, e o teste que o cobre ficaria sem o que afirmar. Foram escolhidos o **pincel chato** e a **lâmpada amarela**, por não participarem de nenhum par de substituição — zerar um par quebraria a ruptura em vez de enfeitar o filtro. Um teste guarda exatamente isso.

**Verificado antes de mexer:** Tintas fica a mais de 25 unidades de todas as outras origens de par — a mais próxima, Encanamento, dá 25,6 —, então a lixa voltar a ficar disponível **não** a coloca na lista de candidatos de nenhum par existente.

**O que a demonstração ganha.** Deixa de ser um app que exibe falta de estoque e passa a ser um app que **descobre a falta pelo cliente**, registra o relato para a loja e ainda salva a venda. É o risco que levantamos no início do projeto — o Merlin poderia reproduzir a mesma ruptura silenciosa que promete resolver — resolvido em cena.

**E a carga precisou aprender a reconciliar estoque.** Mudar o número na massa não chegava a banco nenhum que já tivesse o produto — e como o schema é um só para desenvolvimento, testes e demonstração ([O-21](observacoes.md#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema)), isso significa não chegar a lugar nenhum. Os testes viram a massa antiga e falharam, que é exatamente o que deveriam fazer.

É a **sétima vez** que o mesmo padrão cobra: campo deixado de fora da reconciliação envelhece em silêncio ([D-51](#d-51-um-valor-de-enum-removido-precisa-sumir-também-do-banco), [D-53](#d-53-a-aplicação-repara-a-restrição-de-enum-que-o-ddl-auto-update-deixa-envelhecer), [D-56](#d-56-a-coluna-coletado-continua-sendo-gravada-mesmo-redundante), [D-59](#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados), [D-69](#d-69-a-massa-passou-a-ser-a-fonte-do-nome-e-da-descrição-e-sobrescreve-o-banco), [D-70](#d-70-renomear-uma-seção-é-migração-não-edição-de-string)).

**Estoque aqui é dado declarado, não estado de um ERP.** Não existe integração de inventário, então a massa é a única fonte. O endpoint de simulação ([D-40](#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido)) escreve por cima para encenar, e a carga devolve o valor ensaiado no próximo start. Isso deixou de atrapalhar justamente porque a ruptura não depende mais de zerar produto — e passou a ajudar: reiniciar a instância antes da banca devolve a loja ao estado ensaiado, sem ninguém precisar lembrar de nada.

**Onde no código.** `infrastructure/database/seed/CatalogoDaMassa.java` — `SKU-TIN-003`, `SKU-TIN-012` e `SKU-ILU-004`; `CarregadorDadosIniciais.java` — `sincronizarEstoque`.

---

---

## Como manter este documento

Toda decisão técnica não convencional deve ser registrada aqui **antes do commit** que a implementa, como parte do fluxo de trabalho do time (planejar → aprovar → implementar → **documentar** → revisar → commitar).

O critério para registrar: *alguém que chegue depois ficaria em dúvida sobre por que isso foi feito assim?* Se sim, documente. Decisões óbvias ou padrão de mercado não precisam entrar.
