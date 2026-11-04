# Progresso da migração Spring Boot → JAX-RS — 04/11/2026

Situação ao fim do dia, para retomar o trabalho. O plano completo, com cada passo, está em
[plano-migracao-spring-para-jax-rs.md](plano-migracao-spring-para-jax-rs.md), e os requisitos em
[requisitos-migracao-spring-para-jax-rs.md](requisitos-migracao-spring-para-jax-rs.md).

## Resumo

- **Feito:** fases 0 a 8 inteiras, exceto os passos manuais 0.3, 1.2 e 1.3. Da fase 9, estão
  prontos os passos 9.1 e 9.2, e 2 dos 21 DTOs do 9.3.
- **Testes:** a suíte padrão (`mvnw clean test`) passa inteira, com 161 testes. A suíte de
  integração contra o Oracle passou depois da remoção do JPA: 312 de 315. As 3 falhas são do
  Gemini e já aconteciam antes (ver 7.21).
- **Git:** mais de 160 commits feitos hoje, todos na `main` **local**. Nada foi enviado ao
  GitHub. O push espera a liberação (ver "Pendências").
- **A aplicação ainda sobe com o Spring Boot.** O banco já é acessado por JDBC puro: DAOs,
  `ConnectionFactory` e `Transacao`. A camada web (controllers, `@Service`, tratamento de erros)
  continua no Spring até a fase 10.

## O que foi implementado hoje

| Fase | O que mudou | Situação |
|---|---|---|
| 0 | Requisitos, plano, coleção do Insomnia (`docs/insomnia/`), script do schema (`database/migrations/V001__schema_inicial.sql`), tempos de resposta antes da migração | ✅ (falta a 0.3, abaixo) |
| 1 | CORS removido; a Vercel repassa `/api/*` para o Render (`frontend/vercel.json`) | ✅ no código; 1.2 e 1.3 são manuais |
| 2 | AssertJ trocado por `Assertions` do JUnit em todos os testes | ✅ |
| 3 | Lombok removido: getters e construtores escritos à mão | ✅ |
| 4 | SLF4J trocado por `System.out` | ✅ |
| 5 | `config/Configuracao` (variáveis de ambiente), `factory/ConnectionFactory` e `factory/Transacao` | ✅ |
| 6 | Pacotes do material: `model`, `dto`, `exception`, `service`, `resource`, `ia`, `scheduler`, `seed`, `dao`, `factory`, `config` | ✅ |
| 7 | 8 DAOs com JDBC; os 19 casos de uso e a carga de dados passaram a usar DAOs + `Transacao`; JPA, Hibernate, entidades e repositórios apagados; `ojdbc8 23.2.0.0` | ✅ |
| 8 | `GeminiClient` com o `HttpClient` do JDK e JSON-B; interface `AssistenteIA` apagada | ✅ |
| 9.1 | As 4 exceções viraram checadas (`extends Exception`), com `throws` declarado em quem as lança ou repassa | ✅ |
| 9.2 | `modelmapper 3.0.0` e `factory/MapperFactory` (instância única) | ✅ |
| 9.3 | DTOs de `record` para classe | 🟡 2 de 21: `PontoMapaResponse`, `ChatMensagemResponse` |

### Resultados registrados no plano

- **7.17:** três diferenças de comportamento entre o JDBC e o Hibernate.
  - O JDBC grava na hora, e não no commit.
  - Falta um índice na chave estrangeira `tb_sessao.ponto_escaneado_id`.
  - Se a busca no catálogo falhar no meio da conversa, o fallback do chat agora grava a pergunta do cliente.
- **7.21:** 315 testes de integração, 312 passam. Nas três falhas, o Gemini disse que não havia substituto para a lixa, embora o substituto estivesse entre os candidatos. Nas outras execuções do mesmo cenário ele o escolheu.

## Onde parei: passo 9.3 (DTOs)

Cada DTO convertido vira um commit `refactor(dto): <Nome> vira classe ...`. O roteiro de cada um:

1. Trocar o `record` por uma classe com campos privados, construtor sem argumentos, getters e
   setters. Para `boolean`, o getter é `isX()`. As anotações (`@NotBlank`, `@Schema`...) vão para os
   campos. As do Swagger saem só no 10.11, e as de validação no 9.4.
2. Reescrever o método `de(...)`:
   - **ModelMapper** (`MapperFactory.getMapper().map(origem, X.class)`) quando todos os campos
     vêm direto de getters de uma classe do `model`.
   - **Setters** quando a origem é um `record`, que o ModelMapper 3.0.0 não lê porque os
     acessores não começam com `get`, ou quando o campo é calculado.
3. Trocar nas chamadas `x.campo()` por `x.getCampo()` e `X::campo` por `X::getCampo`. Os testes
   que montavam o DTO com `new X(a, b, c)` passam a usar setters, ou o `X.de(...)` a partir de um
   objeto do `model`.
4. Quando o DTO usa ModelMapper, criar um teste em `src/test/java/.../dto/` que confere campo a
   campo. Esse teste protege contra o casamento implícito de nomes do ModelMapper.
5. Rodar `mvnw clean test` e fazer o commit.

Ordem sugerida para os 19 que faltam, com a forma de conversão de cada um:

| DTO | Conversão |
|---|---|
| `AtributoResponse` | setters (origem `ValorDeAtributo` é record) |
| `SecaoResponse` | setters (origem `SecaoDoCatalogo` é record) |
| `BlocoMapaResponse` | setters (origem `BlocoMapa` é record) |
| `MapaResponse` | setters, montado em `ConsultarMapaUseCase` |
| `ProdutoResponse` | ModelMapper + setters para `pontoMapaId` e `corredor` |
| `ProdutoDetalhadoResponse` | ModelMapper + setters para `pontoMapaId`, `corredor`, `pontoMapa`, `atributos` |
| `ItemRoteiroDetalhadoResponse` | ModelMapper + setters para `produtoId` e `produto` |
| `ListaRoteiroResponse` | setters (o ModelMapper tentaria copiar `itens` sozinho) |
| `SessaoResponse` | ModelMapper + setter para `posicaoAtual` |
| `RupturaEstoqueResponse` | setters |
| `FacetaResponse` (e o `ValorResponse` interno) | setters |
| `PaginaResponse` | setters |
| `CatalogoResponse` | setters |
| `StandardError` (e o `ValidationError` interno) | setters, montado no `GlobalExceptionHandler` |
| `AdicionarItemRequest`, `EnviarMensagemRequest`, `EstoqueUpdateRequest`, `RecentrarSessaoRequest`, `SubstituirItemRequest`, `IniciarSessaoRequest` | só a classe (não têm `de`) |

**Cuidados conhecidos:**

- O `PontoMapa` normaliza o código curto (`"TIN-01"` vira `"TIN01"`). Em teste, compare com `ponto.getCodigoCurto()`.
- O enum `Remetente` tem `USER` e `ASSISTANT`.
- No Windows, rode sempre `mvnw clean`. Classes compiladas pelo VS Code em `target/` já causaram falha falsa.

## O que falta depois do 9.3

- **9.4:** validação manual. Cada DTO de requisição ganha um `validar()` que devolve os erros de
  campo, com as mesmas mensagens das anotações. Um commit por DTO.
- **9.5:** tirar `spring-boot-starter-validation` do `pom.xml`.
- **Fase 10, sair do Spring:**
  - Jersey 4.0.3, Grizzly e hk2;
  - `Main`;
  - `HealthResource`;
  - agendador com `ScheduledExecutorService`;
  - os 6 Resources em JAX-RS, com testes web em Grizzly e services falsos;
  - teste de contrato por reflexão;
  - testes de integração subindo com `Main.startServer`;
  - apagar `BackendApplication`, `GlobalExceptionHandler`, `AgendamentoConfig`, os `@Component`/`@Service` provisórios, os `application*.yml` e as anotações do Swagger;
  - `pom.xml` final, sem o parent do Spring;
  - `Dockerfile` com o jar do shade.
- **Fase 11, publicação:**
  - `render.yaml` com `healthCheckPath: /health`;
  - `deploy.md`, `decisoes-tecnicas.md` e o C4 atualizados;
  - push e deploy, perguntando antes;
  - coleção do Insomnia contra a produção e comparação dos tempos de resposta.

## Pendências

- **Bloqueia o push:** apagar `VITE_API_BASE_URL` no painel da Vercel e publicar o frontend de
  novo (passo 1.2). Sem isso, o frontend publicado chamaria o backend sem CORS e falharia.
  Depois, conferir no navegador que as chamadas vão para o domínio da Vercel (1.3).
- **0.3:** rodar a coleção do Insomnia contra a versão atual em produção e preencher
  `docs/insomnia/base-antes.md`. Precisa ser feito **antes** do deploy da versão migrada, senão
  não há base de comparação.
- **Credenciais:** as do banco e a chave do Gemini foram usadas só por variável de ambiente e não
  estão em nenhum arquivo do repositório. Como foram compartilhadas em captura de tela, a
  recomendação de trocá-las continua de pé.
- **Testes de integração** rodam no mesmo schema Oracle da produção. Eles limpam o que criam, mas
  não devem rodar com a apresentação no ar.
- **Ferramentas de apoio:** os scripts usados hoje para converter em massa (propagar `throws`,
  converter record em classe, trocar acessores) ficaram numa pasta temporária, fora do
  repositório. Se ela tiver sumido amanhã, o roteiro acima basta para continuar à mão.
