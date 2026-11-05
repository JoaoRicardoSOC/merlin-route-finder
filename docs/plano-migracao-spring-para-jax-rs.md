# Merlin Route Finder — Plano de Implementação da Migração para JAX-RS

> Passo a passo para cumprir [`requisitos-migracao-spring-para-jax-rs.md`](requisitos-migracao-spring-para-jax-rs.md). Leia os requisitos primeiro: este plano não repete o porquê de cada escolha.
>
> Criado em 04/11/2026. Marque cada passo como concluído (`[x]`) no mesmo commit que o conclui.

---

## 1. Como o trabalho é conduzido

**Estratégia: migrar por fases, com o Spring ainda rodando até a última.** As fases 1 a 9 tiram do código, uma a uma, as peças que não estão no material (Lombok, JPA, Bean Validation, AssertJ...) enquanto o Spring continua servindo a aplicação. Só a fase 10 troca o Spring pelo Jersey. Assim, ao fim de cada fase a aplicação funciona e os testes passam, e a troca de alto risco fica pequena e isolada no final.

**Regras de commit e push**
- Tudo direto na `main`.
- Um commit por subtarefa: uma mudança lógica, específica e com mensagem que descreve só ela. Mesmo arquivo com duas mudanças vira dois commits (`git add -p`).
- Todo commit compila. Os testes da suíte padrão (`mvnw test`) passam em todo commit, exceto onde o plano avisa.
- **Push só ao fim de cada fase, e só depois de perguntar ao time.** Se o Render publica a cada push na `main`, cada push é um deploy.

**Pontos de verificação com banco 🔒**
Esta máquina não tem acesso ao Oracle da FIAP. Os testes de integração (`mvnw test -Pintegracao`, 22 classes) e a coleção do Insomnia só rodam na máquina de quem tem acesso. Os passos marcados com 🔒 param o trabalho até essa pessoa rodar e confirmar. **Nenhuma fase é dada como concluída sem o seu 🔒.**

**Linha de base da integração (04/11/2026, código ainda com Spring, depois das fases 2 e 3):** 285 testes, 278 passam. As 7 falhas não vêm da migração:
- 6 dependem do Gemini, que nesse dia levava 15 s para responder "ok" (a D-37 mediu ~1 s) e estourava o limite de 20 s: `AssistenteIntegracaoTest` (2), `GeminiIntegracaoTest` (1), `JornadaCompletaIntegracaoTest` (2) e `RupturaIntegracaoTest.elegeOSubstitutoSemantico...` (1);
- 1 tem premissa desatualizada: `RupturaIntegracaoTest.produtoIsoladoNaLoja...` supõe o espelho de Decoração sem vizinho num raio de 25, mas o catálogo cresceu e hoje há outros 9 produtos no mesmo ponto (88,55).

Uma fase só é dada como concluída se a integração não tiver falha **além dessas**.

**Dúvidas:** toda decisão não prevista aqui é perguntada ao time antes de ser tomada, e registrada neste documento.

**Pendências que bloqueiam o push** (cada uma sai desta lista quando for resolvida)
- [ ] Apagar `VITE_API_BASE_URL` no painel da Vercel e publicar o frontend de novo (passo 1.2). Sem isso, o backend publicado sem CORS deixa o frontend sem conseguir chamar a API.

---

## 2. Visão geral das fases

| Fase | O que sai | Spring ainda roda? | Tamanho aproximado |
|---|---|---|---|
| 0. Preparação | — (só levanta a base de comparação) | Sim | Coleção do Insomnia, DDL |
| 1. CORS por rewrite | `CorsConfig` | Sim | 3 arquivos + painel da Vercel |
| 2. Asserções | AssertJ | Sim | 38 testes |
| 3. Lombok | Lombok (exceto no que a fase 7 apaga) | Sim | ~40 arquivos |
| 4. Log | SLF4J | Sim | ~10 arquivos |
| 5. Configuração | `@Value` | Sim | ~5 arquivos |
| 6. Estrutura de pacotes | Pacotes `domain`/`application`/`presentation` | Sim | Só movimentação |
| 7. Persistência JDBC | JPA, Hibernate, Spring Data, HikariCP, Lombok, interfaces de repositório | Sim | 8 DAOs, 19 services |
| 8. Cliente do Gemini | `RestClient`, interface `AssistenteIA` | Sim | 4 arquivos + testes |
| 9. Exceções, DTOs e validação | Bean Validation, records nos DTOs | Sim | 4 exceções, 21 DTOs |
| 10. Troca do Spring pelo Jersey | Spring, springdoc, Swagger, Mockito | **Não** | 6 Resources, 29 testes, pom, `Main` |
| 11. Documentação e publicação | — | Não | Docs + deploy |

---

## 3. Fases

### Fase 0 — Preparação

Objetivo: ter com o que comparar o antes e o depois, e ter o schema escrito antes de o Hibernate sair.

- [x] **0.1** Commitar os documentos de requisitos e este plano.
- [x] **0.2** Criar a coleção do Insomnia em `docs/insomnia/merlin-paridade.json`: os 18 endpoints, em ordem de jornada (iniciar sessão → buscar produto → montar roteiro → coletar → ruptura → substituir → concluir), mais um pedido por caso de erro de RF-03 a RF-07.
- [ ] **0.3** 🔒 Rodar a coleção contra a versão atual e registrar status e corpo de cada pedido em `docs/insomnia/base-antes.md`.
- [x] **0.4** Gerar o DDL a partir das entidades JPA com a exportação de schema do próprio Hibernate, em modo offline e com dialeto Oracle. Reescrever o resultado no formato do material (`CREATE SEQUENCE`/`CREATE TABLE`) em `database/migrations/V001__schema_inicial.sql`, incluindo chaves estrangeiras, `UNIQUE` (`sku`, `codigo_curto`, produto + atributo) e as restrições `CHECK` dos enums.
- [x] **0.5** 🔒 Comparar o script com o schema real no SQL Developer e corrigir as diferenças.
- [x] **0.6** Medir o tempo de resposta atual dos endpoints de busca de produtos, mapa e roteiro (RNF-06) e registrar em `docs/insomnia/base-antes.md`. *Perguntar antes, porque as medições chamam a API publicada.*

### Fase 1 — CORS por rewrite na Vercel

Objetivo: o frontend publicado chamar a API no mesmo domínio, para o backend não precisar mais de CORS. Vem cedo porque precisa ser confirmado em produção antes de o Spring sair.

- [x] **1.1** Criar `frontend/vercel.json` com rewrite de `/api/:caminho*` para `https://merlin-route-finder-api.onrender.com/api/:caminho*`.
- [ ] **1.2** Push (perguntar antes) e, **no painel da Vercel**, apagar `VITE_API_BASE_URL` e publicar de novo. *Manual, feito por quem administra o projeto na Vercel.*
- [ ] **1.3** Percorrer a jornada no frontend publicado e confirmar, nas ferramentas do navegador, que as chamadas vão para o domínio da Vercel.
- [x] **1.4** Remover `CorsConfig` e a propriedade `merlin.cors.allowed-origins`.
- [x] **1.5** Remover `CORS_ALLOWED_ORIGINS` de `render.yaml` e atualizar `docs/deploy.md`.

### Fase 2 — Asserções só com JUnit

- [x] **2.1** Trocar AssertJ (`assertThat`) pelos métodos de `org.junit.jupiter.api.Assertions`, **um commit por classe de teste**. Começar pelos testes de domínio, que não usam Spring.
- [x] **2.2** Confirmar que nenhum arquivo importa `org.assertj`. A dependência sai na fase 10, porque vem junto com o starter de testes do Spring.

### Fase 3 — Lombok fora

Ficam de fora desta fase os arquivos que a fase 7 apaga (entidades JPA, adaptadores, fábricas de conversão, repositórios JPA).

- [x] **3.1** `domain/entity` (7 arquivos, só `@Getter`): getters escritos à mão. As regras e a imutabilidade não mudam.
- [x] **3.2** `application` (19 arquivos): construtores escritos à mão no lugar de `@RequiredArgsConstructor`.
- [x] **3.3** `infrastructure` (os que sobrevivem à fase 7: IA, agendador, carga de dados, configuração).
- [x] **3.4** `presentation` (6 controllers).

Um commit por pacote ou subpacote. A dependência do Lombok sai na fase 7, quando o último arquivo que a usa for apagado.

### Fase 4 — Log com `System.out` e `System.err`

- [x] **4.1** Trocar `@Slf4j` e `log.info`/`log.warn` por `System.out.println`, e `log.error` por `System.err.println` seguido de `printStackTrace` quando houver exceção. Um commit por classe (10 classes).

### Fase 5 — Configuração por variável de ambiente

- [x] **5.1** Criar `Configuracao` no pacote `config`, com um método por chave de RC-13, lendo `System.getenv` e usando os mesmos valores padrão do `application.yml`.
- [x] **5.2** *Mudança de rumo (04/11/2026):* trocar agora o `@Value` do `GeminiClient` e do `RestClientConfig` exigiria configuração do Spring provisória (métodos `@Bean`), justamente o que está saindo, e as duas classes são reescritas nas fases 8 e 10. Cada classe passa a usar a `Configuracao` quando for reescrita: `ConnectionFactory` (7.1), Gemini (8.2), agendador, carga de dados e `Main` (fase 10).

### Fase 6 — Estrutura de pacotes do material

Só movimentação e renomeação, sem mudar comportamento. Um commit por item, com os testes movidos no mesmo commit. Fica de fora o que a fase 7 apaga.

- [x] **6.1** `domain/entity` → `model`.
- [x] **6.2** Tipos de valor de `domain/repository` (`Pagina`, `FiltroDeProdutos`, `FacetaDeProdutos`, `SecaoDoCatalogo`, `AfinidadeDeProduto`) e de `domain/service` (`MensagemIA`, `PapelIA`, `FerramentaIA`) → `model`.
- [x] **6.3** `domain/exception` → `exception`.
- [x] **6.4** `application/dto` e `presentation/response/StandardError` → `dto`.
- [x] **6.5** `application/usecase` e `domain/service/PosicaoDoCliente` → `service`.
- [x] **6.6** `presentation/controller/*Controller` → `resource/*Resource` (ainda com anotações do Spring).
- [x] **6.7** `infrastructure/ia` e `domain/service/ExecutorDeFerramenta` → `ia`.
- [x] **6.8** `infrastructure/scheduler` → `scheduler`; `infrastructure/database/seed` → `seed`; `Configuracao` → `config`.

Ficam temporariamente no lugar antigo: `domain/repository/*Repository`, `domain/service/AssistenteIA`, `infrastructure/database/*`, `infrastructure/config`, `infrastructure/web` e `presentation/advice`.

### Fase 7 — Persistência com JDBC

A fase mais longa. Os DAOs novos são escritos primeiro, ao lado do JPA; depois cada service troca de lado, um por vez; por fim, o JPA é apagado.

**7.A Infraestrutura**
- [x] **7.1** `factory/ConnectionFactory`: `getConnection()` como no material (`DriverManager`, credenciais de `Configuracao`). Guarda a conexão da transação em andamento na thread (`ThreadLocal`) e a devolve a quem pedir durante a transação.
- [x] **7.2** `factory/Transacao`: executa um trabalho com `setAutoCommit(false)`, `commit` no sucesso, `rollback` em qualquer exceção, e sempre fecha a conexão. Modos: normal, somente leitura (`setReadOnly(true)`) e nova (conexão própria, para a ruptura).
- [x] **7.3** Testes de `Transacao` com tag `integracao`: commit, rollback e transação nova independente. 🔒

**7.B DAOs (sem trocar quem usa ainda)**

Um commit por DAO, com o seu teste de integração. Regras de todos: SQL com `?`, *try-with-resources*, `throws SQLException`, UUID como `varchar2(36)` (D-11), `setFetchSize(100)` nas listas (D-45), `JOIN` explícito onde hoje há `@EntityGraph`, paginação com `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY`.

- [x] **7.4** `PontoMapaDao` (inclui as três consultas nativas de `PontoMapaJpaRepository`).
- [x] **7.5** `ProdutoDao`: busca por termo, busca com filtros opcionais (SQL montado com parâmetros), proximidade espacial, facetas, seções, atributos e estoque. É o maior: dividir em um commit por grupo de consultas.
- [x] **7.6** `SessaoDao`.
- [x] **7.7** `ListaRoteiroDao` (itens, produtos e pontos num só `JOIN`).
- [x] **7.8** `ItemRoteiroDao`.
- [x] **7.9** `ChatMensagemDao`.
- [x] **7.10** `RegistroRupturaDao`, gravando em `Transacao` nova (D-39).
- [x] **7.11** `RestricaoDeEnumDao`: a lógica de `RestricaoDeEnumNoBanco` com JDBC no lugar do `JdbcTemplate`.
- [x] **7.12** 🔒 Rodar os testes de integração dos DAOs.

**7.C Troca dos services**

Um commit por service (19). Em cada um: passa a receber os DAOs concretos pelo construtor (com construtor sem argumentos que faz `new`), troca `@Transactional` por `Transacao` e, se tiver teste unitário, troca o Mockito por subclasses dos DAOs escritas à mão. Enquanto o Spring estiver rodando, os DAOs ganham `@Component` provisório para serem injetados; ele sai na fase 10.

- [x] **7.13** Services de consulta primeiro (`ConsultarSessao`, `ConsultarListaRoteiro`, `ConsultarProduto`, `BuscarProdutos`, `ListarSecoes`, `ConsultarMapa`, `ConsultarHistoricoChat`).
- [x] **7.14** Services de escrita (`InicializarSessao`, `RecentrarSessao`, `ConcluirRota`, `AdicionarProdutoAoRoteiro`, `RemoverProdutoDoRoteiro`, `MarcarItemColetado`, `DesmarcarItemColetado`, `SubstituirItemDoRoteiro`, `SimularEstoque`, `ExpirarSessoesInativas`).
- [x] **7.15** Services com IA e ruptura (`ConversarComAssistente`, `TratarRupturaEstoque`). O Gemini continua como está até a fase 8.
- [x] **7.16** Carga de dados (`seed`) passa a usar os DAOs.
- [x] **7.17** 🔒 `mvnw test -Pintegracao` completo e coleção do Insomnia.

**Resultado da 7.17 (04/11/2026):** 322 testes de integração, todos passam, exceto os da linha de base (Gemini lento ou sem cota, e a premissa desatualizada do produto isolado). Três diferenças de comportamento apareceram e ficaram registradas:
- O JDBC grava na hora, e não no commit como o Hibernate. Duas transações na mesma thread que gravam a mesma linha travam uma à outra; em produção são threads diferentes, e a segunda só espera a primeira confirmar. O `EscritaObsoletaIntegracaoTest` passou a simular a requisição paralela em outra thread.
- A chave estrangeira `tb_sessao.ponto_escaneado_id` não tem índice. Apagar em `tb_ponto_mapa`, como faz a carga de dados, espera qualquer transação aberta que tenha mexido em `tb_sessao`. Uma JVM de teste travada segurou isso por meia hora.
- No chat, se a busca no catálogo falhar no meio da conversa, o fallback agora grava a pergunta do cliente; com o Spring, a transação inteira era desfeita.

**7.D Remoção do JPA**
- [x] **7.18** Apagar adaptadores, repositórios JPA, entidades JPA, fábricas de conversão, `RestricaoDeEnumNoBanco` e as interfaces `domain/repository/*Repository`. Um commit por grupo.
- [x] **7.19** Tirar do `pom.xml` `spring-boot-starter-data-jpa` e o starter de teste correspondente; trocar `ojdbc11` por `ojdbc8` `23.2.0.0`; tirar o Lombok.
- [x] **7.20** Tirar `spring.datasource` e `spring.jpa` dos `application*.yml`.
- [x] **7.21** 🔒 `mvnw test -Pintegracao` completo de novo, agora sem nenhum JPA.

**Resultado da 7.21 (04/11/2026):** 315 testes de integração, 312 passam. As três falhas são do Gemini: nas duas, ele respondeu que não havia substituto para a lixa, embora a Lixa d'Água Grão 150 estivesse entre os candidatos enviados. Nas outras execuções do mesmo cenário ele a escolheu. É a mesma categoria da linha de base, que tinha sete falhas.

### Fase 8 — Cliente do Gemini com o `HttpClient` do JDK

- [x] **8.1** Adicionar `jersey-media-json-binding` (dependência do material, traz o JSON-B).
- [x] **8.2** `GeminiClient`: `RestClient` → `java.net.http.HttpClient` (instância única, tempos limite da `Configuracao`); corpo montado e lido com JSON-B. Apagar `RestClientConfig`.
- [x] **8.3** `GeminiClientTest`: `MockRestServiceServer` → servidor falso com `com.sun.net.httpserver.HttpServer` do JDK.
- [x] **8.4** Apagar a interface `AssistenteIA`; `ConversarComAssistente` e `TratarRupturaEstoque` passam a depender de `GeminiClient`. Testes com subclasse escrita à mão.

### Fase 9 — Exceções checadas, DTOs e validação

- [x] **9.1** Uma exceção por commit (`RecursoNaoEncontrado`, `OperacaoNaoPermitida`, `SubstitutoIndisponivel`, `AssistenteIAIndisponivel`): passa a estender `Exception` e cada método que a lança declara `throws`. O `GlobalExceptionHandler` do Spring continua tratando até a fase 10.
- [x] **9.2** Adicionar `modelmapper` `3.0.0` e `factory/MapperFactory`, com instância única.
- [x] **9.3** Um DTO por commit (21): `record` → classe com construtor sem argumentos, getters e setters. A conversão no service passa a usar `ModelMapper` onde a cópia é direta e setters nos campos calculados.
- [x] **9.4** Validação manual: cada DTO de requisição ganha um método `validar()` que devolve a lista de erros de campo, com as mesmas regras e mensagens das anotações de hoje. O Resource devolve 400 com `StandardError` quando a lista não vem vazia. Um commit por DTO.
- [x] **9.5** Tirar `spring-boot-starter-validation` do `pom.xml`.

### Fase 10 — Troca do Spring pelo Jersey

Nesta fase o Spring e o Jersey convivem no `pom.xml` até o passo 10.12. **Atenção:** a partir de 10.4, os testes de integração (tag `integracao`) deixam de passar até 10.9, porque os endpoints vão saindo do Spring um a um. A suíte padrão continua passando em todo commit. Não há push no meio desta fase.

- [x] **10.1** Adicionar `jersey-bom` `4.0.3`, `jersey-container-grizzly2-http` e `jersey-hk2`.
- [x] **10.2** Criar `Main` a partir da do material: `startServer(porta)` estático (usado também pelos testes), `0.0.0.0`, porta de `PORT`, fuso UTC, `ResourceConfig().packages("br.com.jence.backend.resource")`, thread principal aguardando, *shutdown hook* que para o servidor e o agendador. Ordem de subida: restrição de enum → carga de dados → agendador → servidor.
- [x] **10.3** `HealthResource` (`GET /health`, sem tocar no banco).
- [x] **10.4** Agendador com `ScheduledExecutorService.scheduleWithFixedDelay`, iniciado pela `Main`.
- [x] **10.5** Um Resource por commit (6), junto com o seu teste web: anotações do Spring → JAX-RS; `try/catch` em cada método montando `StandardError` com o status certo (mesmos status do `GlobalExceptionHandler`); `@PathParam` como `String` convertido com `UUID.fromString`; `Location` com `UriBuilder` só onde existe hoje; segundo construtor que recebe os services. O teste sobe o Grizzly com o Resource registrado com services falsos e chama com o `HttpClient` do JDK.
- [x] **10.6** `ErrosDeRequisicaoTest` e `FalhaDeInfraestruturaTest` reescritos no mesmo formato. Confirmar RF-06 e RF-07; se o JSON malformado não der 400, aplicar a alternativa de RF-07.
- [x] **10.7** Teste de contrato reescrito sem Spring e sem SnakeYAML: as rotas são lidas por reflexão das anotações `@Path`/`@GET`/... dos Resources, e as do `openapi.yaml`, com leitura linha a linha em Java puro. Sem banco, ele passa a rodar na suíte padrão.
- [x] **10.8** Os 21 testes de integração restantes: um commit por classe, subindo o servidor com `Main.startServer` numa porta livre e chamando com o `HttpClient` do JDK.
- [x] **10.9** 🔒 `mvnw test -Pintegracao` completo. **Resultado (05/11/2026):** 351 testes, 2 falhas. Uma era regressão do 9.3 (`MapaIntegracaoTest.mapaEstavel` comparava DTOs com `equals`), corrigida e confirmada. A outra foi a já conhecida do 7.21: o Gemini respondeu que não havia substituto para a lixa na `JornadaCompletaIntegracaoTest`; o mesmo teste passou nas duas execuções seguintes.
- [x] **10.10** Apagar `BackendApplication`, `GlobalExceptionHandler`, `AgendamentoConfig`, os `@Component`/`@Service` provisórios e os `application*.yml`.
- [ ] **10.11** Apagar as anotações do Swagger (`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`).
- [ ] **10.12** `pom.xml` final (RC-01, RC-02): sem `spring-boot-starter-parent` e sem nenhuma dependência do Spring, springdoc, Mockito, AssertJ ou SnakeYAML; `junit-jupiter` `5.10.1`; `maven-compiler-plugin` com Java 21; `exec-maven-plugin` apontando para `Main`; `maven-shade-plugin`; JaCoCo e o profile `integracao` mantidos.
- [ ] **10.13** `Dockerfile`: mesmo `java -jar`, agora com o jar do shade.
- [ ] **10.14** Verificações de RNF-01 por busca no código; `mvnw test`; subir a imagem Docker localmente e chamar `/health`.

### Fase 11 — Documentação e publicação

- [ ] **11.1** `render.yaml`: `healthCheckPath: /health`; sem `SPRING_PROFILES_ACTIVE`.
- [ ] **11.2** `docs/deploy.md` atualizado (variáveis, *health check*, execução manual de scripts de schema).
- [ ] **11.3** `decisoes-tecnicas.md`: novas decisões (migração para JAX-RS com adesão total ao material; JDBC sem pool; CORS por rewrite; exceções checadas; DTOs com ModelMapper; schema por script) e revisão das que mudam: D-01, D-03, D-10, D-11, D-16, D-30, D-33, D-34, D-45 e D-46. Um commit por decisão.
- [ ] **11.4** `docs/arquitetura` (C4) atualizado com a nova estrutura.
- [ ] **11.5** Push e deploy (perguntar antes).
- [ ] **11.6** 🔒 Coleção do Insomnia contra a produção, comparada com `base-antes.md`; medição de tempo de resposta comparada com a 0.6.
- [ ] **11.7** Marcar a definição de pronto dos requisitos.

---

## 4. Riscos

| Risco | Efeito | O que fazer |
|---|---|---|
| Sem pool, cada transação abre conexão com o Oracle no Brasil a partir do Render nos EUA | Requisições mais lentas | Medir na 0.6 e na 11.6. Se ficar inaceitável, levar ao time; o plano não decide sozinho |
| Testes de integração só rodam na máquina de outra pessoa | Erros de SQL aparecem tarde | Pontos 🔒 em toda fase que toca o banco; não avançar sem eles |
| Schema por script manual | Um integrante esquece de rodar uma alteração | `RestricaoDeEnumDao` continua corrigindo os enums sozinho; demais mudanças anunciadas no commit e no `deploy.md` |
| SQL escrito à mão em ~30 consultas | Regressão em busca, filtros e paginação | Testes de integração existentes (`BuscaComFiltros`, `AtributosEFacetas`, `PreFiltragemEspacial`) cobrem essas consultas |
| Exceções checadas espalham `throws` | Assinaturas maiores | Esperado; é o padrão do material |
| Fase 10 deixa os testes de integração quebrados por vários commits | Regressão passa despercebida no meio da fase | Sem push até 10.9; 🔒 obrigatório antes de seguir |

---

## 5. Registro de decisões tomadas com o time (04/11/2026)

| Tema | Decisão |
|---|---|
| Rigor | Exatamente como o material, inclusive para o que não é Spring. Java puro é permitido |
| Estrutura | Pacotes do material sob `br.com.jence.backend`, mais `service`, `ia`, `scheduler`, `seed` e `config` |
| Repositórios | Só DAO concreto, sem interface |
| Modelos | Mantêm as regras e a imutabilidade; só perdem o Lombok |
| DTOs | Classes com getters/setters; `ModelMapper` + setters nos campos calculados |
| Exceções | Checadas, como no material |
| Versões | Java 21, Jersey 4.0.3, `ojdbc8` |
| Empacotamento | `maven-shade-plugin` |
| Conexão e transação | `ConnectionFactory` guarda a conexão da transação da thread; services recebem os DAOs pelo construtor |
| Testes web | Grizzly com services falsos, sem banco |
| Banco | Esta máquina não tem acesso; DDL gerado das entidades; integração rodada por quem tem acesso |
| Paridade | Testes de integração + coleção do Insomnia |
| Git | Direto na `main`, um commit por subtarefa |
