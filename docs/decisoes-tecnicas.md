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
- [D-14. Salvar item carrega a entidade gerenciada em vez de remapear](#d-14-salvar-item-carrega-a-entidade-gerenciada-em-vez-de-remapear)
- [D-15. Query nativa com `UTL_MATCH` para busca tolerante a erro de digitação](#d-15-query-nativa-com-utl_match-para-busca-tolerante-a-erro-de-digitação)
- [D-16. Carga inicial em Java em vez de SQL](#d-16-carga-inicial-em-java-em-vez-de-sql)

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
./mvnw test -Pintegracao # 53 testes, exige DB_URL/DB_USER/DB_PASSWORD
```

**Motivo.** Um clone novo do repositório precisa passar nos testes sem configuração prévia. Se a suíte padrão exige credencial de um banco específico, ela deixa de ser rede de proteção e vira obstáculo: as pessoas param de rodá-la, ou pior, aprendem a ignorar build vermelho.

A separação também deixa explícita uma distinção real: 52 testes verificam **lógica** (domínio, algoritmo, camada web com mocks) e não deveriam depender de infraestrutura; os de integração verificam **a costura com o banco**, e aí a dependência é legítima.

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

**Contexto.** Assinar o token de handoff exige uma chave secreta. Cada integrante roda a aplicação na própria máquina, e o projeto vai para deploy público.

**Decisão.** A chave vem de `JWT_SECRET` (via `merlin.jwt.secret`). Se estiver ausente ou vazia, a aplicação **gera uma chave aleatória no startup** e registra um aviso no log.

**Alternativas.** Um segredo padrão embutido no `application.yml` — descartada, e essa é a decisão central aqui: um segredo commitado é exatamente o tipo de coisa que passa despercebida e chega em produção. Qualquer pessoa com acesso ao repositório poderia forjar tokens válidos. Falhar o startup quando o segredo falta — descartada por atritar o dia a dia dos cinco integrantes sem ganho real de segurança em desenvolvimento.

**Consequências.** Em desenvolvimento, tokens não sobrevivem a um restart da aplicação. Irrelevante, dado o TTL de 5 minutos: um QR Code gerado antes de reiniciar já estaria perto de expirar de qualquer forma. Em produção, basta definir a variável de ambiente.

**Onde no código.** `infrastructure/security/GeradorTokenJwt.java`, `src/main/resources/application.yml`.

---

### D-28. A rota parte do primeiro ponto do tipo TOTEM

**Contexto.** O algoritmo de roteamento precisa de uma origem (D-26). No handoff, a origem natural é o totem onde o cliente está montando a lista.

**Decisão.** O caso de uso busca pontos do tipo `TOTEM` e usa o primeiro encontrado.

**Limitação assumida.** Numa loja com vários totens, o roteiro sairia calculado a partir do totem errado. Resolver exigiria o Totem se identificar na requisição, e o contrato hoje envia apenas `sessaoId` — mudar isso afetaria a dupla de frontend, o que pela política da D-25 exige combinar antes.

É uma limitação sem impacto no escopo atual (a massa de demonstração tem um totem) e de correção simples quando for necessário: acrescentar `totemId` opcional ao `HandoffRequest`.

**Onde no código.** `application/usecase/GerarHandoffUseCase.java`.

---

### D-29. Uso único do token pela ausência no banco

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

**Decisão.** Um `ApplicationRunner` que verifica se o catálogo está vazio e só então popula, usando os ports de domínio.

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

**Contexto.** O UC-012 permite ao cliente pedir um banheiro ou caixa durante a caminhada. Mas um ponto de apoio **não é um produto**: não cabe como `ItemRoteiro`, que exige um `Produto`, e o DER entregue à banca não tem tabela para esse conceito.

**Decisão.** O ponto de apoio é inserido apenas na rota **devolvida** pelo endpoint. Nada é gravado no banco.

**Alternativas.** Criar tabela e entidade próprias para pontos de interesse — a modelagem correta e duradoura, descartada por três motivos combinados: divergiria do DER já entregue na documentação (exigindo atualizar o diagrama), seria o card mais caro da fase, e o prazo do vídeo estava a três semanas. Guardar um campo na `ListaRoteiro` — descartada por poluir a entidade com um conceito que não é dela e suportar só um desvio por vez.

**Custo assumido.** Se o app recarregar, o desvio some e o cliente precisa tocar de novo no botão. Aceitável porque o desvio é transitório por natureza — depois de passar no banheiro, ele deixa de importar — e porque o celular mantém a rota em cache (`sessionStorage`, conforme o diagrama de sequência).

**O que mudaria para persistir no futuro.** Uma tabela `TB_PONTO_INTERESSE_ROTEIRO` com `lista_id`, `ponto_mapa_id` e a posição de inserção, mais entidade, mapper, repositório e atualização do DER. A lógica de inserção do caso de uso não mudaria.

**Consequências no contrato.** `PontoRota.item` vem **nulo** para o ponto de apoio — o contrato já previa isso ("item de compra **ou ponto de interesse**"), e é por essa ausência que o celular distingue uma parada de compra de um desvio.

**Onde no código.** `application/usecase/IncluirPontoDeInteresseUseCase.java`, `application/dto/PontoRotaResponse.java`.

---

### D-24. TTL da sessão é renovado a cada interação

**Contexto.** A sessão nasce com TTL de 30 minutos (card 4). Adicionar e remover itens são as ações que o cliente executa enquanto monta o roteiro no Totem.

**Decisão.** Adicionar ou remover item chama `Sessao.renovarSessao()`, empurrando o TTL para 30 minutos à frente da interação. Consultar a lista **não** renova.

**Motivo — e por que isso é obrigatório, não um extra.** Sem renovação, teríamos uma contradição silenciosa com a [D-17](#d-17-carrinho-de-roteiro-sem-limite-de-itens): removemos o limite de itens do carrinho justamente para atender o empreiteiro que monta uma lista grande para uma obra inteira, mas um TTL fixo de 30 minutos expulsaria exatamente esse cliente no meio da montagem. As duas decisões só funcionam juntas.

**Por que consultar não renova.** Consulta é leitura; renovar exigiria gravar a cada `GET`, e listar a lista não é sinal forte de atividade (uma tela aberta e esquecida continuaria consultando). As ações de escrita são evidência real de que há alguém interagindo.

**Consequências.** Uma sessão só expira após 30 minutos de **inatividade** real, não 30 minutos de duração total. O job de expiração da Fase 3 continua funcionando normalmente: ele varre por `expiracaoTtl` vencido, que agora reflete a última interação.

**Onde no código.** `application/usecase/AdicionarProdutoAoRoteiroUseCase.java`, `RemoverProdutoDoRoteiroUseCase.java`, `domain/entity/Sessao.java`.

---

### D-32. Coletar item passa pela raiz do agregado e não encerra a jornada

**Contexto.** No UC-014 o cliente confirma ter pego um produto da prateleira. O caminho direto seria carregar o `ItemRoteiro` pelo id, marcar e salvar — e existe até um `ItemRoteiroRepository` pronto para isso.

**Decisão 1: carregar pela `ListaRoteiro`, não pelo item.** Um novo método `buscarPorItem(itemId)` no port devolve o agregado inteiro; o item é marcado dentro dele e a lista é salva.

Dois motivos. O primeiro é de modelagem: alterações devem passar pela raiz do agregado. O segundo é prático — pela [D-09](#d-09-relação-com-a-sessão-é-unidirecional), o `ItemRoteiro` não conhece a lista que o contém, e sem chegar à lista não há como alcançar a sessão, necessária para a decisão 2.

O `ItemRoteiroRepository` continua no projeto: o fluxo de ruptura de estoque (Fase 2) precisa localizar um item isolado.

**Decisão 2: coletar renova o TTL da sessão.** Aplicação da [D-24](#d-24-ttl-da-sessão-é-renovado-a-cada-interação) ao trecho da caminhada, e aqui ela é ainda mais necessária.

Sem isso havia um bug silencioso esperando: a sessão era renovada no handoff e **nada mais a renovava durante a caminhada**. Um cliente com lista grande numa loja de 10.000m² leva mais de 30 minutos com facilidade — a sessão morreria no meio da compra, quebrando a marcação dos itens seguintes e o tratamento de ruptura, sem nenhum erro aparente até o cliente tentar a próxima ação.

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
- **Lista vazia ou inexistente → `EXPIRED`.** Alguém encostou no totem e foi embora sem adicionar nada. Nada estava em jogo.

**Motivo.** Mandar tudo para `EXPIRED` seria mais simples, mas jogaria fora justamente a métrica que o produto promete melhorar. "Quantos clientes montaram uma lista e desistiram no meio" é um número que a loja quer ver; "quantos encostaram no totem" é outro, e misturá-los não ajuda ninguém.

**A varredura não protege regra de negócio nenhuma.** `Sessao.isValida()` já compara com o relógio, então uma sessão vencida é recusada mesmo que ninguém a tenha varrido. O job existe para que o **banco reflita a realidade**: sem ele, qualquer contagem de jornadas em andamento seria ficção.

**Regra no caso de uso, gatilho na infraestrutura.** `ExpirarSessoesInativasUseCase` decide o que fazer com cada sessão; `AgendadorDeExpiracao` apenas dispara. A regra fica testável sem envolver agendamento — é a mesma separação entre porta e adaptador usada no resto do projeto, só que aqui o "adaptador" é o relógio.

**Três detalhes que não são óbvios:**

1. **`fixedDelay`, não `fixedRate`.** O intervalo conta a partir do fim da execução anterior. Com `fixedRate`, uma varredura lenta começaria a se sobrepor à seguinte e duas passagens disputariam as mesmas sessões.
2. **O agendador engole a exceção.** O Spring **aposenta** uma tarefa agendada que lança — as execuções seguintes simplesmente param. Como o motivo mais provável de falha aqui é o banco estar momentaneamente fora, deixar a exceção subir mataria a varredura até o próximo restart.
3. **Cada sessão é tratada isoladamente**, com o `salvar` do adaptador abrindo a própria transação. Uma sessão problemática não impede a varredura das demais, e não há o que desfazer em bloco.

**Duas limitações aceitas, e por quê.**

**O job não roda com a aplicação dormindo.** Provedores gratuitos suspendem a instância por inatividade, então numa loja parada a varredura não acontece. É best-effort — e como o job não protege regra nenhuma, o custo é apenas o banco demorar mais para se acertar.

**Há uma janela de corrida de milissegundos** entre a varredura ler as sessões vencidas e gravá-las: se o cliente renovar exatamente nesse intervalo, a renovação se perde e a próxima ação dele recebe 409. Para isso acontecer ele teria que estar inativo há 30 minutos e agir justo naquele milissegundo. Resolver exigiria bloqueio otimista — desproporcional ao risco.

**A varredura é naturalmente idempotente**, porque a consulta filtra por `ACTIVE`: a segunda passagem não encontra o que a primeira já tratou. Verificado contra o Oracle real.

**O guard da D-06 sob teste.** Uma sessão `COMPLETED` com TTL vencido **não** é sobrescrita. Sem isso, o cliente que concluiu a rota às 10h00 viraria `ABANDONED` às 10h05, e o sistema perderia a informação de que a jornada foi completada — que é a métrica de sucesso do produto.

**Onde no código.** `application/usecase/ExpirarSessoesInativasUseCase.java`, `infrastructure/scheduler/AgendadorDeExpiracao.java`, `infrastructure/config/AgendamentoConfig.java`.

---

### D-43. 2-opt sobre o Nearest Neighbor, na variante de caminho aberto

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

## Como manter este documento

Toda decisão técnica não convencional deve ser registrada aqui **antes do commit** que a implementa, como parte do fluxo de trabalho do time (planejar → aprovar → implementar → **documentar** → revisar → commitar).

O critério para registrar: *alguém que chegue depois ficaria em dúvida sobre por que isso foi feito assim?* Se sim, documente. Decisões óbvias ou padrão de mercado não precisam entrar.
