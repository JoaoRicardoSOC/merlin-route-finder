# Progresso da migração Spring Boot → JAX-RS — 05/11/2026

Continuação de [progresso-migracao-2026-11-04.md](progresso-migracao-2026-11-04.md). O plano
completo está em [plano-migracao-spring-para-jax-rs.md](plano-migracao-spring-para-jax-rs.md).

## Resumo

- **Feito hoje:** fase 9 inteira (9.3 a 9.5) e a fase 10 até o passo 10.8.
- **Parado em:** 🔒 **10.9**, a suíte de integração contra o Oracle. O plano não segue para o
  10.10 sem ela, e esta máquina não tem as credenciais do banco.
- **Testes:** a suíte padrão (`mvnw clean test`) passa inteira, com 195 testes. Os testes de
  integração compilam, mas **não foram executados** depois da reescrita.
- **Git:** 68 commits hoje, na `main` **local**. Nada foi enviado ao GitHub.
- **A aplicação ainda sobe com o Spring em produção** (o `Dockerfile` usa o jar do Spring até o
  10.13). Mas os 6 Resources já são JAX-RS: o Spring sobe sem nenhum endpoint de negócio. A
  aplicação nova sobe pela `Main`, com Jersey e Grizzly.

## O que foi implementado hoje

| Passo | O que mudou |
|---|---|
| 9.3 | Os 19 DTOs restantes viraram classes. ModelMapper em `ProdutoResponse`, `ProdutoDetalhadoResponse`, `ItemRoteiroDetalhadoResponse` e `SessaoResponse`, cada um com teste campo a campo; setters nos demais |
| 9.4 | `validar()` nos 5 DTOs de requisição com validação, com as mesmas regras e mensagens; `StandardError.de(...)` e `falhaDeValidacao(...)` |
| 9.5 | `spring-boot-starter-validation` (e o de teste) fora do `pom.xml` |
| 10.1 | `jersey-bom 4.0.3`, `jersey-container-grizzly2-http`, `jersey-hk2` |
| 10.2 | `Main`: `0.0.0.0`, porta de `PORT`, UTC, carga de dados antes do servidor, *shutdown hook* |
| 10.3 | `HealthResource` (`GET /health`, sem banco) |
| 10.4 | `AgendadorDeExpiracao.iniciar/parar` com `ScheduledExecutorService`, ligado pela `Main` |
| 10.5 | Os 6 Resources em JAX-RS, cada um com teste web no Grizzly e casos de uso falsos |
| 10.6 | `ErrosDeRequisicaoTest` e `FalhaDeInfraestruturaTest` no Grizzly; RF-06 e RF-07 confirmados |
| 10.7 | `ContratoOpenApiTest`: rotas por reflexão contra o `openapi.yaml` lido linha a linha (18 = 18), na suíte padrão |
| 10.8 | Os testes de integração sem Spring: `BancoDeTeste.carregarMassa()` no lugar da carga do `@SpringBootTest`, casos de uso montados com os DAOs, testes HTTP pela `Main.startServer` |

## O que foi medido, e o que se fez

Cada item abaixo foi medido com o Jersey antes de decidir.

- **UUID malformado no `@PathParam` responde 404, e JSON malformado num parâmetro DTO responde
  500** (página HTML do Grizzly). Por isso os Resources recebem o id e o corpo como texto e
  convertem dentro do `try`, pela `LeituraDaRequisicao`. É a alternativa que o próprio RF-07
  previa. O mesmo vale para `page`, `size` e `apenasDisponiveis`: com `Integer`/`Boolean` no
  `@QueryParam` o Jersey responderia 404. O lógico aceita as grafias que o Spring aceitava.
- **Query fora de UTF-8** (`?query=L%E2mpada`): o Grizzly aceita e troca o byte por U+FFFD, e a
  busca respondia 200. No Tomcat era 400 (D-74). O `ProdutoResource` detecta o caractere e
  devolve o mesmo 400 de antes.
- **`HttpClient` do JDK nos testes:** por padrão tenta HTTP/2 (h2c), o Grizzly não aceita e o
  corpo do POST nunca chega. O `ServidorDeTeste` fixa HTTP/1.1. Clientes de verdade não são
  afetados.
- **Cliente do Gemini:** o Jersey cria um Resource por requisição. O `ChatResource` e o
  `ItemRoteiroResource` guardam o `GeminiClient` num campo estático, para não abrir um
  `HttpClient` novo a cada chamada.
- **`Location` do `POST /sessoes`:** passa a ser absoluto (o JAX-RS exige). O frontend não lê
  esse cabeçalho.
- **Campos nulos:** o JSON-B os omite, como os requisitos já aceitavam. Os testes de integração
  que liam `posicaoAtual` ou `validationErrors` nulos passaram a aceitar o campo ausente.
- **405, 415 e endereço inexistente** saem do Jersey sem corpo, como o RF-04 aceita. Os testes
  conferem só o status nesses casos.

## O que falta

1. 🔒 **10.9:** rodar `mvnw test -Pintegracao` numa máquina com as variáveis `DB_URL`, `DB_USER`,
   `DB_PASSWORD` (e `GEMINI_API_KEY`, para os testes do assistente). A base é 312 de 315, com 3
   falhas conhecidas do Gemini (ver 7.21). Falha além dessas precisa ser investigada antes do
   10.10. Pontos de maior risco, por terem sido reescritos sem poder rodar:
   - `JornadaCompletaIntegracaoTest` e `RespostasDeErroIntegracaoTest`, que agora chamam a
     aplicação pela `Main`;
   - qualquer leitura de campo que antes vinha `null` e agora vem ausente.
2. **10.10 a 10.14:** apagar `BackendApplication`, `GlobalExceptionHandler`, `AgendamentoConfig`,
   as anotações `@Component`/`@Service`, `@Scheduled` e `ApplicationRunner`, os `application*.yml`,
   as anotações do Swagger; `pom.xml` final sem o parent do Spring, com shade; `Dockerfile`;
   verificação com Docker e `/health`.
3. **Fase 11:** `render.yaml` (`healthCheckPath: /health`, sem `SPRING_PROFILES_ACTIVE` e
   `DB_POOL_SIZE`), documentação, push e deploy (perguntar antes), coleção do Insomnia contra a
   produção.

## Pendências que continuam de pé

- **Bloqueia o push:** apagar `VITE_API_BASE_URL` na Vercel e publicar o frontend (1.2/1.3).
- **0.3:** preencher `docs/insomnia/base-antes.md` com a coleção contra a produção atual,
  **antes** do deploy da versão migrada.
- **Credenciais:** a recomendação de trocar as do banco e a chave do Gemini continua.
- **Testes de integração** usam o schema da produção; não rodar com a apresentação no ar.
- **Ferramentas de apoio:** os scripts de conversão de hoje ficaram numa pasta temporária, fora
  do repositório. Não são necessários para continuar.
