# Merlin Route Finder — Backend

API do Merlin Route Finder: Java 21 com JAX-RS (Jersey sobre o Grizzly), JSON-B, JDBC e ModelMapper, seguindo o material de *Estudo de API* da FIAP. Até 05/11/2026 o backend era Spring Boot; o porquê da troca está na [D-89](../docs/decisoes-tecnicas.md#d-89-o-backend-sai-do-spring-boot-e-segue-o-material-da-fiap-à-risca).

**Documentação do projeto:**
- [`../docs/contexto-e-planejamento.md`](../docs/contexto-e-planejamento.md) — contexto de negócio, equipe, cronograma e rubrica do Challenge
- [`../docs/planejamento-tecnico.md`](../docs/planejamento-tecnico.md) — arquitetura e organização do trabalho em fases
- [`../docs/decisoes-tecnicas.md`](../docs/decisoes-tecnicas.md) — **por que** cada escolha não convencional foi feita (leitura recomendada antes de mexer no código)
- [`../docs/observacoes.md`](../docs/observacoes.md) — pendências, limitações aceitas e avisos que precisam chegar a quem não escreveu o código
- [`../docs/deploy.md`](../docs/deploy.md) — como publicar a API no Render, com a tabela de variáveis de ambiente e o processo de mudança de schema
- [`src/main/resources/openapi/openapi.yaml`](src/main/resources/openapi/openapi.yaml) — contrato REST completo (API-First)

## Pré-requisitos

- Java 21
- Não é necessário instalar Maven — use o wrapper (`./mvnw` no Linux/Mac, `mvnw.cmd` no Windows).
- Uma credencial Oracle FIAP (cada integrante usa a própria, individualmente — decisão do time em 18/08/2026).

## Configuração local

Toda a configuração vem de **variável de ambiente**, lida pela classe `config/Configuracao` — não há arquivo de configuração. Defina pelo menos `DB_URL`, `DB_USER` e `DB_PASSWORD` com a sua credencial Oracle FIAP (no IntelliJ: Run/Debug Configurations → Environment variables). Para o assistente de IA, também `GEMINI_API_KEY`.

> O antigo `application-local.yml` do Spring não é mais lido. Se você tinha um, as credenciais dele precisam virar variáveis de ambiente.

## Rodando a aplicação

```bash
./mvnw compile exec:java
```

Ou, como o Render faz, pelo jar único gerado pelo `maven-shade-plugin`:

```bash
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

A `Main` roda a carga de dados, liga o agendador de sessões vencidas e abre a API em `http://localhost:8080` (ou na porta de `PORT`). `GET /health` responde `OK` sem tocar no banco.

## Testes

```bash
./mvnw test
```

Roda a suíte padrão **sem precisar de banco nem de credencial** — funciona em qualquer máquina recém-clonada.

Os testes que usam o Oracle são marcados com `@Tag("integracao")` e ficam de fora dessa execução. Para incluí-los, com as variáveis de banco definidas:

```bash
./mvnw test -Pintegracao
```

Eles usam o mesmo schema da instância publicada: limpam o que criam, mas não devem rodar durante uma apresentação.

A suíte tem três níveis, com papéis diferentes — ver [D-33](../docs/decisoes-tecnicas.md#d-33-suíte-de-testes-roda-sem-banco-integração-fica-separada-por-tag):

| Nível | Precisa de | O que verifica |
|---|---|---|
| Unidade e camada web | nada | regra de negócio, status HTTP, validação, erros, contrato de rotas |
| Integração por caso de uso | Oracle | consultas SQL, transação, agregado |
| Jornada por HTTP | Oracle | os passos entre si, do QR Code ao caixa |

Os testes web sobem o Grizzly com o Resource e casos de uso falsos, escritos à mão (`resource/ServidorDeTeste`). O `ContratoOpenApiTest` confere as rotas dos Resources contra o `openapi.yaml`. `JornadaCompletaIntegracaoTest` percorre a jornada inteira por HTTP com a aplicação de pé, e imprime cada etapa — é também a forma mais rápida de ver o sistema funcionando ponta a ponta.

Stack de testes: JUnit 5, e nada mais.

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Para quê |
|---|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | sim | — | conexão com o Oracle da FIAP |
| `PORT` | não | `8080` | porta HTTP (provedores de hospedagem injetam) |
| `GEMINI_API_KEY` | para a IA | — | assistente de IA; sem ela os recursos de IA ficam indisponíveis |
| `GEMINI_MODEL` | não | `gemini-3.5-flash-lite` | modelo usado (trocável sem recompilar; ver [D-37](../docs/decisoes-tecnicas.md#d-37-escolha-do-modelo-por-medição-e-o-limite-do-tier-gratuito)) |
| `MERLIN_SEED_ENABLED` | não | `true` | `false` desliga a carga de dados no startup |
| `MERLIN_AGENDADOR_ENABLED` | não | `true` | `false` desliga a varredura de sessões vencidas |
| `AGENDADOR_INTERVALO_MS` | não | `1800000` (30 min) | intervalo da varredura |
| `AGENDADOR_ATRASO_INICIAL_MS` | não | `60000` | espera antes da primeira varredura |
| `MERLIN_HTTP_CONNECT_TIMEOUT_SECONDS`, `MERLIN_HTTP_READ_TIMEOUT_SECONDS` | não | `5`, `20` | tempos limite das chamadas ao Gemini |

Nenhuma credencial é versionada: todas vêm de variável de ambiente, inclusive em produção. Não há CORS no backend: o frontend chama `/api` pelo rewrite da Vercel (ver [D-91](../docs/decisoes-tecnicas.md#d-91-o-cors-sai-do-backend-a-vercel-repassa-api-para-o-render)).

## Organização

Pacotes sob `br.com.jence.backend`, os do material mais quatro próprios:

| Pacote | O que tem |
|---|---|
| `resource` | os Resources JAX-RS, `RespostaDeErro` e `LeituraDaRequisicao` |
| `service` | os casos de uso, que recebem os DAOs pelo construtor |
| `dao` | os DAOs com JDBC |
| `factory` | `ConnectionFactory`, `Transacao` e `MapperFactory` |
| `dto` | os DTOs de entrada e saída, com `validar()` nos de entrada |
| `model` | o domínio, sem dependência de framework |
| `exception` | as exceções checadas do domínio |
| `ia`, `scheduler`, `seed`, `config` | cliente do Gemini, agendador, carga de dados e leitura das variáveis |

O schema do banco é mantido por script em [`../database/migrations`](../database/migrations) ([D-94](../docs/decisoes-tecnicas.md#d-94-o-schema-do-banco-passa-a-ser-mantido-por-script)).
