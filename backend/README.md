# Merlin Route Finder — Backend

API do Merlin Route Finder (Java 21 + Spring Boot).

**Documentação do projeto:**
- [`../docs/contexto-e-planejamento.md`](../docs/contexto-e-planejamento.md) — contexto de negócio, equipe, cronograma e rubrica do Challenge
- [`../docs/planejamento-tecnico.md`](../docs/planejamento-tecnico.md) — arquitetura, convenções de pacotes e organização do trabalho em fases
- [`../docs/decisoes-tecnicas.md`](../docs/decisoes-tecnicas.md) — **por que** cada escolha não convencional foi feita (leitura recomendada antes de mexer no código)
- [`../docs/observacoes.md`](../docs/observacoes.md) — pendências, limitações aceitas e avisos que precisam chegar a quem não escreveu o código
- [`../docs/deploy.md`](../docs/deploy.md) — como publicar a API no Render, com a tabela de variáveis de ambiente
- [`src/main/resources/openapi/openapi.yaml`](src/main/resources/openapi/openapi.yaml) — contrato REST completo (API-First)

## Pré-requisitos

- Java 21
- Não é necessário instalar Maven — use o wrapper (`./mvnw` no Linux/Mac, `mvnw.cmd` no Windows).
- Uma credencial Oracle FIAP (cada integrante usa a própria, individualmente — decisão do time em 18/08/2026).

## Configuração local (variáveis de ambiente)

A aplicação lê as credenciais do banco via variável de ambiente (nunca hardcoded). Você tem duas formas de configurar isso localmente — escolha uma:

**Opção A — variáveis de ambiente do sistema/IDE**
Defina `DB_URL`, `DB_USER` e `DB_PASSWORD` com a sua credencial Oracle FIAP antes de rodar a aplicação (no IntelliJ: Run/Debug Configurations → Environment variables).

**Opção B — profile local (recomendado para não esquecer de configurar toda vez)**
Crie `src/main/resources/application-local.yml` (já está no `.gitignore` — nunca será commitado) com:

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@<host>:<porta>:<sid_ou_service>
    username: <seu_usuario_fiap>
    password: <sua_senha_fiap>
```

E rode com o profile `local` ativo (`--spring.profiles.active=local` ou `SPRING_PROFILES_ACTIVE=local`).

> A partir da Fase 2 do planejamento técnico (integração com IA), também será necessária a variável `GEMINI_API_KEY` — mesma lógica acima, nunca commitada.

## Rodando a aplicação

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Documentação interativa (Swagger UI) em `http://localhost:8080/swagger.html`.

## Testes

```bash
./mvnw test
```

Roda a suíte completa **sem precisar de banco nem de credencial** — funciona em qualquer máquina recém-clonada.

Os testes que sobem o contexto contra o Oracle são marcados com `@Tag("integracao")` e ficam de fora dessa execução. Para incluí-los, com as variáveis de banco configuradas:

```bash
./mvnw test -Pintegracao
```

A suíte tem três níveis, com papéis diferentes — ver [D-33](../docs/decisoes-tecnicas.md#d-33-suíte-de-testes-roda-sem-banco-integração-fica-separada-por-tag):

| Nível | Precisa de | O que verifica |
|---|---|---|
| Unidade e camada web | nada | regra de negócio, status HTTP, validação, erros |
| Integração por caso de uso | Oracle | consultas nativas, transação, agregado |
| Jornada por HTTP | Oracle | os passos entre si, do totem ao caixa |

`JornadaCompletaIntegracaoTest` percorre a jornada inteira por HTTP com a aplicação de pé, e imprime cada etapa — é também a forma mais rápida de ver o sistema funcionando ponta a ponta. Alguns testes exigem `GEMINI_API_KEY` e são pulados sem ela; a jornada não exige.

Stack de testes disponível via `pom.xml`: JUnit 5, Mockito e AssertJ.

## Variáveis de ambiente

| Variável | Obrigatória | Padrão | Para quê |
|---|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | sim | — | conexão com o Oracle da FIAP |
| `PORT` | não | `8080` | porta HTTP (provedores de hospedagem injetam) |
| `CORS_ALLOWED_ORIGINS` | não | `localhost:3000,localhost:5173` | origens do Totem e do Mobile, separadas por vírgula |
| `JWT_SECRET` | não | chave aleatória por execução | assinatura do token de handoff |
| `HANDOFF_BASE_URL` | não | `http://localhost:5173` | URL do PWA codificada no QR Code |
| `GEMINI_API_KEY` | para a IA | — | assistente de IA; sem ela os recursos de IA ficam indisponíveis |
| `GEMINI_MODEL` | não | `gemini-3.5-flash-lite` | modelo usado (trocável sem recompilar; ver [D-37](../docs/decisoes-tecnicas.md#d-37-escolha-do-modelo-por-medição-e-o-limite-do-tier-gratuito)) |
| `DB_POOL_SIZE` | não | `3` | tamanho máximo do pool; o schema da FIAP permite 20 sessões por usuário (ver [D-46](../docs/decisoes-tecnicas.md#d-46-o-pool-de-conexões-é-dimensionado-pelos-limites-reais-do-schema-da-fiap)) |
| `AGENDADOR_INTERVALO_MS` | não | `300000` (5 min) | intervalo da varredura de sessões vencidas; `merlin.agendador.enabled=false` desliga o job |

Nenhuma credencial é versionada. Ver [D-27](../docs/decisoes-tecnicas.md#d-27-segredo-do-jwt-por-ambiente-com-chave-aleatória-em-desenvolvimento) sobre o tratamento do segredo do JWT.

## Produção

Ativar o perfil `prod` (`SPRING_PROFILES_ACTIVE=prod`), que desliga o log de SQL. Todo o resto vem das variáveis acima.

## Arquitetura

O projeto segue arquitetura hexagonal (portas e adaptadores), com entidades de domínio puras separadas da camada de persistência JPA — ver a seção 4 de [`../docs/planejamento-tecnico.md`](../docs/planejamento-tecnico.md) para a convenção completa de pacotes.
