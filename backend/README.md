# Merlin Route Finder — Backend

API do Merlin Route Finder (Java 21 + Spring Boot). Contexto de negócio e planejamento técnico completos estão em [`../docs/contexto-e-planejamento.md`](../docs/contexto-e-planejamento.md) e [`../docs/planejamento-tecnico.md`](../docs/planejamento-tecnico.md).

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

Stack de testes já disponível via `pom.xml`: JUnit 5, Mockito e AssertJ.

## Arquitetura

O projeto segue arquitetura hexagonal (portas e adaptadores), com entidades de domínio puras separadas da camada de persistência JPA — ver a seção 4 de [`../docs/planejamento-tecnico.md`](../docs/planejamento-tecnico.md) para a convenção completa de pacotes.
