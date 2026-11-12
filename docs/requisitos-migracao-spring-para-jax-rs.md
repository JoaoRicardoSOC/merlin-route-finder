# Merlin Route Finder — Requisitos da Migração de Spring Boot para JAX-RS (Jersey)

> Documento de requisitos para substituir o Spring Boot do backend pelas tecnologias do material **"Estudo de API — Criação de recursos RESTful, Manipulação e Respostas"** (FIAP, cap. 17).
>
> Este documento diz **o que** precisa mudar e **como saber que a mudança deu certo**. O **como fazer**, passo a passo, está em [`plano-migracao-spring-para-jax-rs.md`](plano-migracao-spring-para-jax-rs.md).
>
> Levantamento feito sobre o código em `main` em 04/11/2026.

---

## 1. Objetivo e regra geral

**Objetivo:** o backend deixa de usar o Spring e passa a ser escrito **exatamente como o material ensina**, sem que o frontend perceba a troca.

**Regra geral de tecnologia** (decidida com o time em 04/11/2026):

1. **Pode usar:** o que o material mostra, as bibliotecas que ele instala e classes do Java puro (`java.*`, `javax.*` do JDK), mesmo as que ele não mostra.
2. **Não pode usar:** nenhuma outra biblioteca. Isso vale também para o que **não é Spring**: Lombok, Hibernate/JPA, Bean Validation, SLF4J, Swagger, Mockito, AssertJ, SnakeYAML e HikariCP saem.
3. **Exceções aceitas**, todas justificadas na seção 8:
   - Jersey **4.0.3** no lugar do 4.0.0-M1 do material (versão de teste);
   - Java **21** no lugar do 1.8 do material;
   - `maven-shade-plugin`, só no build, para gerar o jar executável do deploy;
   - credenciais lidas de variável de ambiente, nunca fixas no código como no material.

---

## 2. Tecnologias de destino

| Tecnologia | Origem no material | Uso no Merlin |
|---|---|---|
| JAX-RS (`jakarta.ws.rs`) | `@Path`, `@GET`, `@POST`, `@PUT`, `@PATCH`, `@DELETE`, `@PathParam`, `@QueryParam`, `@Consumes`, `@Produces`, `@Context UriInfo`, `Response`, `UriBuilder` | Camada web (pacote `resource`) |
| Jersey + Grizzly | `ResourceConfig`, `GrizzlyHttpServerFactory`, classe `Main` | Servidor HTTP |
| JSON-B (`jersey-media-json-binding`) | Conversão automática de objeto em JSON | Corpo das requisições e respostas; JSON do Gemini |
| JDBC (`ojdbc8`) | `ConnectionFactory`, `PreparedStatement`, `ResultSet`, sequência, DAO | Persistência (pacote `dao`) |
| Padrão DTO + ModelMapper | Classes com getters/setters, `mapper.map(...)` | Pacote `dto` |
| Exceção checada | `EntidadeNaoEcontradaException extends Exception` | Pacote `exception` |
| JUnit 5 | Dependência do pom do material | Toda a suíte de testes |
| SQL Developer | Execução do DDL | Criação e alteração do schema |
| Insomnia | Teste manual dos endpoints | Coleção de paridade |
| Java puro | — | `HttpClient` (Gemini e testes), `ScheduledExecutorService` (agendador), `System.getenv` (configuração), `com.sun.net.httpserver` (servidor falso do Gemini nos testes), `System.out`/`System.err` (log) |

---

## 3. Estrutura de pacotes de destino

Raiz `br.com.jence.backend`, com os pacotes do material e quatro pacotes próprios para o que o material não cobre:

| Pacote | Conteúdo | Origem atual |
|---|---|---|
| *(raiz)* | `Main` | `BackendApplication` |
| `resource` | 6 recursos JAX-RS (`*Resource`) + `HealthResource` | `presentation/controller` |
| `service` | 19 casos de uso (nomes mantidos) + `PosicaoDoCliente` | `application/usecase`, `domain/service` |
| `dao` | 7 DAOs JDBC + `RestricaoDeEnumDao` | `infrastructure/database/{adapter,repository,schema}` |
| `model` | Entidades e tipos de valor do domínio, com as regras de hoje | `domain/entity`, `domain/repository` (tipos de valor), `domain/service` (tipos da IA) |
| `dto` | 21 DTOs como classes + `StandardError` | `application/dto`, `presentation/response` |
| `exception` | Exceções checadas | `domain/exception` |
| `factory` | `ConnectionFactory`, controle de transação, instância única do `ModelMapper` | — |
| `ia` *(próprio)* | Cliente do Gemini e textos de instrução | `infrastructure/ia` |
| `scheduler` *(próprio)* | Agendador de expiração | `infrastructure/scheduler` |
| `seed` *(próprio)* | Carga de dados iniciais | `infrastructure/database/seed` |
| `config` *(próprio)* | Leitura de variáveis de ambiente | `application.yml` + `@Value` |

**Deixam de existir:** entidades JPA (`infrastructure/database/entity`), fábricas de conversão JPA ↔ domínio (`infrastructure/database/factory`), interfaces de repositório (`domain/repository/*Repository`), a interface `AssistenteIA`, `GlobalExceptionHandler`, `CorsConfig`, `RestClientConfig`, `AgendamentoConfig` e os arquivos `application*.yml`.

---

## 4. Requisitos funcionais de paridade

O frontend não pode precisar de nenhuma mudança de código.

| ID | Requisito | Critério de aceite |
|---|---|---|
| **RF-01** | Os 18 endpoints continuam nos mesmos caminhos e métodos HTTP, sob `/api/v1`. | O teste de contrato (`ContratoOpenApiIntegracaoTest`, reescrito) confirma que as rotas dos Resources batem com o `openapi.yaml`. |
| **RF-02** | As respostas de sucesso mantêm os nomes dos campos, os tipos e o formato. | Coleção do Insomnia rodada antes e depois, mais os testes de integração. Datas em ISO-8601, `UUID` como texto. |
| **RF-03** | Os códigos de status ficam iguais nos mesmos casos: 200, 201, 204, 400, 404, 405, 409, 415, 422 e 500. | `RespostasDeErroIntegracaoTest` e `ErrosDeRequisicaoTest` passam com as mesmas expectativas de status. |
| **RF-04** | Erros tratados pelos Resources respondem com o corpo `StandardError`. | Todo `catch` dos Resources monta `StandardError`. Erros que o Jersey responde antes de chegar ao método (rota inexistente, método não permitido, `Content-Type` errado) mantêm o status, mas podem vir sem corpo. Isso é aceito: o frontend só lê o status (verificado em `frontend/src/services`). |
| **RF-05** | Dados inválidos na entrada respondem 400 com a lista `errors` (campo e mensagem). | Validação manual nos DTOs de requisição, sem Bean Validation. |
| **RF-06** | UUID malformado na URL continua respondendo 400, e não 404. | O `@PathParam` chega como `String` e é convertido com `UUID.fromString` dentro do `try`. |
| **RF-07** | JSON malformado no corpo continua respondendo 400. | Teste dedicado. Se o Jersey responder outro status, o Resource recebe o corpo como `String` e converte com JSON-B dentro do `try`. |
| **RF-08** | O frontend publicado continua chamando a API sem erro de CORS. | A Vercel repassa `/api/*` para o Render (rewrite em `frontend/vercel.json`), frontend e API ficam no mesmo domínio e o backend deixa de precisar de CORS. |
| **RF-09** | A varredura de sessões vencidas roda em intervalo fixo contado do fim da execução anterior, com atraso inicial, e pode ser desligada. Uma falha não cancela as execuções seguintes. | `ExpiracaoDeSessaoIntegracaoTest` e `ExpirarSessoesInativasUseCaseTest` passam. |
| **RF-10** | A carga de dados iniciais roda antes de o servidor aceitar requisições e pode ser desligada. | `CargaDeDadosIntegracaoTest` passa. |
| **RF-11** | O registro de ruptura continua em transação própria, separada da transação do caso de uso (D-39). | Uma falha depois do registro não desfaz o registro. |
| **RF-12** | O cliente do Gemini mantém os tempos limite (conexão 5 s, leitura 20 s, configuráveis) e o fallback do caso de uso (D-35). | `GeminiClientTest` (com servidor falso do JDK) e `ConversarComAssistenteUseCaseTest` passam. |
| **RF-13** | A aplicação roda em UTC, independentemente do fuso da máquina. | Datas gravadas e devolvidas iguais às de hoje. |
| **RF-14** | Existe um endereço de saúde que responde sem tocar no banco, para o *health check* do Render. | `GET /health` responde 200; `render.yaml` aponta para ele. |
| **RF-15** | As restrições de enum no banco continuam se ajustando sozinhas quando um enum muda (motivo na classe `RestricaoDeEnumNoBanco`). | Mesma lógica, reescrita com JDBC. |

**Mudanças de comportamento aceitas** (consequência direta da regra geral):
- O Swagger UI e o `/v3/api-docs` deixam de existir. O `openapi.yaml` continua no repositório como contrato (revisa D-30 e D-34).
- Campos `null` deixam de aparecer no JSON, porque o JSON-B os omite por padrão. O frontend não distingue `null` de campo ausente (nenhuma comparação com `null` ou `undefined` em `frontend/src`).
- A ordem dos campos no JSON pode mudar (o JSON-B ordena alfabeticamente). Nenhum cliente JSON depende disso.
- O schema deixa de ser criado pelo Hibernate. Tabelas novas e alterações de coluna passam a ser feitas por script em `database/migrations`, executado no SQL Developer, como no material.
- Sem pool de conexões, cada transação abre e fecha a própria conexão. Isso deixa as requisições mais lentas; o quanto, precisa ser medido (RNF-06).

---

## 5. Requisitos de construção

| ID | Requisito |
|---|---|
| **RC-01** | `pom.xml` sem nenhum artefato `org.springframework*`, Lombok, Hibernate, Bean Validation, SLF4J, Swagger, Mockito, AssertJ, SnakeYAML ou HikariCP. |
| **RC-02** | Dependências do material: `jersey-bom`, `jersey-container-grizzly2-http`, `jersey-hk2`, `jersey-media-json-binding`, `ojdbc8` (`23.2.0.0`), `modelmapper` (`3.0.0`), `junit-jupiter` (`5.10.1`), `maven-compiler-plugin`, `exec-maven-plugin`, `maven-surefire-plugin`. Mais `maven-shade-plugin` e o JaCoCo, que já existe. *(O JaCoCo saiu em 12/11/2026, por não estar no material: ver D-95 em `decisoes-tecnicas.md`.)* |
| **RC-03** | A classe `Main` segue a do material, com três correções: escuta em `0.0.0.0` (não `localhost`), lê a porta de `PORT` e **não** usa `System.in.read()` (no container ele desligaria o servidor logo depois de subir). |
| **RC-04** | Os Resources seguem o `ProdutoResource` do material: `@Path` na classe, `@Consumes`/`@Produces(APPLICATION_JSON)`, `try/catch` em cada método devolvendo `Response`. Construtor sem argumentos que cria os services com `new` e um segundo construtor que recebe os services, usado nos testes. |
| **RC-05** | Os DAOs seguem o `ProdutoDao` do material: `PreparedStatement` com parâmetros, `ResultSet` lido coluna a coluna, `throws SQLException`. Correções obrigatórias em relação ao material: nenhum DAO guarda conexão aberta; `PreparedStatement` e `ResultSet` fechados com *try-with-resources*. |
| **RC-06** | A `ConnectionFactory` segue a do material (`DriverManager.getConnection`), lendo URL, usuário e senha de `DB_URL`, `DB_USER` e `DB_PASSWORD`. Ela guarda a conexão da transação em andamento na thread, para que todos os DAOs de um caso de uso usem a mesma. |
| **RC-07** | O controle de transação usa só `java.sql.Connection`: `setAutoCommit(false)`, `commit`, `rollback`, `setReadOnly`. O registro de ruptura usa uma segunda conexão, com transação própria. |
| **RC-08** | Os DTOs são classes com construtor sem argumentos, getters e setters, como no material. A conversão usa `ModelMapper` onde a cópia é direta; campos calculados são preenchidos pelo setter. O `ModelMapper` é criado uma vez só. |
| **RC-09** | As exceções do projeto estendem `Exception` (checadas), como no material, e cada método que as lança declara `throws`. |
| **RC-10** | As entidades do domínio mantêm as regras, a imutabilidade e os construtores de hoje (D-04, D-05). Só perdem o Lombok. |
| **RC-11** | Os services recebem os DAOs pelo construtor. O construtor sem argumentos cria os DAOs com `new`. |
| **RC-12** | Log com `System.out` (informação) e `System.err` (erro), como o material faz na `Main`. |
| **RC-13** | Configuração lida por uma classe do pacote `config` com `System.getenv` e os mesmos valores padrão de hoje: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `PORT`, `GEMINI_API_KEY`, `GEMINI_MODEL`, `AGENDADOR_INTERVALO_MS`, `AGENDADOR_ATRASO_INICIAL_MS`, mais as chaves de liga/desliga da carga de dados e do agendador e os tempos limite do HTTP. |
| **RC-14** | Testes só com JUnit 5. Dublês escritos à mão (subclasses dos DAOs e services). Testes de API sobem o Grizzly e chamam com o `HttpClient` do JDK. |
| **RC-15** | O DDL de todas as tabelas, restrições e índices fica em `database/migrations`, no formato do material (`CREATE SEQUENCE`/`CREATE TABLE`), pronto para rodar no SQL Developer. |

---

## 6. Requisitos não funcionais

| ID | Requisito |
|---|---|
| **RNF-01** | Nenhum arquivo do backend importa `org.springframework`, `lombok`, `jakarta.persistence`, `jakarta.validation`, `org.slf4j`, `io.swagger`, `org.mockito`, `org.assertj` ou `org.yaml`. |
| **RNF-02** | Nenhuma conexão fica aberta fora de uma transação ou consulta. A instância publicada e a máquina de quem cedeu a credencial dividem 20 sessões no Oracle da FIAP (D-46). |
| **RNF-03** | A imagem continua cabendo no plano gratuito do Render (512 MB), com os mesmos `JAVA_OPTS`. |
| **RNF-04** | Nenhuma credencial no código nem no repositório. O repositório é público no GitHub. |
| **RNF-05** | A cobertura medida pelo JaCoCo não diminui. *(Sem medição desde 12/11/2026, quando o JaCoCo saiu do `pom.xml` (D-95). A migração já tinha sido verificada antes disso.)* |
| **RNF-06** | O tempo de resposta é medido antes e depois nos endpoints de busca de produtos, mapa e roteiro. Se a falta de pool piorar demais, o time decide o que fazer; o plano não resolve isso sozinho. |
| **RNF-07** | Nenhum teste existente é apagado sem justificativa no commit. Testes que dependiam do Spring são reescritos, não descartados. |

---

## 7. Pontos do material que não devem ser copiados

| No material | Problema no Merlin | Como fica |
|---|---|---|
| `BASE_URI = "http://localhost:8080/"` | Não responde de fora do container | `0.0.0.0` e porta de `PORT` |
| `System.in.read()` para manter o servidor no ar | No container, o servidor desliga logo depois de subir | Thread principal aguarda; *shutdown hook* desliga |
| Usuário e senha fixos na `ConnectionFactory` | Credencial do Oracle num repositório público | Variáveis de ambiente |
| DAO abre conexão no construtor e nunca fecha | Esgota as 20 sessões do schema | Conexão por transação, sempre fechada |
| `PreparedStatement` e `ResultSet` sem fechar | Vazamento de recursos no banco | *try-with-resources* |
| `new ModelMapper()` a cada requisição | Custo desnecessário | Instância única no pacote `factory` |
| Jersey `4.0.0-M1`, Java 1.8 | Versão de teste; Java antigo não compila o projeto | Jersey `4.0.3`, Java 21 |

---

## 8. Exceções à regra geral

| Exceção | Por quê |
|---|---|
| Jersey 4.0.3 | O 4.0.0-M1 do material é versão de teste. O 4.0.3 é a versão final da mesma linha. |
| Java 21 | Com 1.8, records, text blocks e `switch` com seta, usados no projeto, não compilam. O `Dockerfile` já usa Java 21. |
| `maven-shade-plugin` | O material roda pelo IntelliJ ou `exec:java`. No container, `exec:java` exige o Maven na imagem e soma a memória dele à da aplicação, arriscando os 512 MB. O plugin só atua no build e não aparece no código. |
| Credenciais por ambiente | Seguir o material publicaria a senha do Oracle da FIAP no GitHub. |
| Pacotes `ia`, `scheduler`, `seed`, `config` | O material não tem lugar para essas peças. |
| Pacote `service` | O material não tem casos de uso; os 19 do Merlin não cabem nos Resources. |

---

## 9. Definição de pronto

- [x] Nenhuma ocorrência das importações listadas em RNF-01 em `backend/src`, e nenhuma das dependências de RC-01 no `pom.xml`. *(Conferido por busca em 12/11/2026.)*
- [x] `mvnw test` passa nesta máquina e `mvnw test -Pintegracao` passa na máquina de quem tem acesso ao Oracle. *(195 testes em 12/11/2026; integração no passo 10.9, em 05/11/2026.)*
- [x] O teste de contrato passa sem mudança no `openapi.yaml`.
- [x] A coleção do Insomnia dá os mesmos status e corpos de antes, com as mudanças aceitas da seção 4. *(Comparada com o esperado, porque a execução de antes não foi registrada. Ver `insomnia/base-antes.md`.)*
- [x] A aplicação sobe no Render, passa no *health check* e o frontend publicado funciona sem mudança de código. *(Com uma ressalva: o frontend precisou de uma mudança, que foi parar de ler `VITE_API_BASE_URL`. A variável continuava no painel da Vercel e mandava as chamadas direto ao Render. Ver a revisão da D-91.)*
- [x] `decisoes-tecnicas.md`, `deploy.md` e os documentos de arquitetura estão atualizados.
