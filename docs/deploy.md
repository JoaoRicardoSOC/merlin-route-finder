# Publicação do backend no Render

> Guia para publicar a API do Merlin Route Finder. Quem executa é alguém do time — criar conta e informar credenciais em serviço externo é ação de pessoa, não de ferramenta.
>
> O que já está pronto no repositório: [`backend/Dockerfile`](../backend/Dockerfile), [`backend/.dockerignore`](../backend/.dockerignore) e [`render.yaml`](../render.yaml).
>
> **Backend publicado em 23/08/2026** em `https://merlin-route-finder-api.onrender.com`.
>
> **Frontend publicado em 30/08/2026** em `https://merlin-route-finder.vercel.app` — raiz `frontend`, preset Vite, com `VITE_API_BASE_URL` apontando para o Render. A jornada inteira foi verificada no ambiente publicado no mesmo dia, incluindo a ruptura com o Gemini real.
>
> O guia segue válido para recriar qualquer um dos dois.

---

## Por que container, e por que separado do frontend

O backend é uma **aplicação Java com Jersey sobre o Grizzly** (desde 05/11/2026, no lugar do Spring Boot): **um processo que fica de pé o tempo todo**, atendendo HTTP e rodando o agendador de TTL. Isso não cabe no modelo serverless da Vercel, que liga e desliga a cada requisição — por isso o backend vai para o Render e o frontend (build estático do Vite) para a Vercel.

Não é gambiarra: **frontend e backend já são dois programas separados hoje**, conversando por HTTP em `localhost:5173` e `localhost:8080`. O deploy só troca `localhost` por URLs públicas.

O que liga os dois é o **rewrite da Vercel** (`frontend/vercel.json`): o navegador chama `/api/...` no próprio domínio do frontend, e a Vercel repassa para o Render. Para o navegador, frontend e API são o mesmo site, e por isso o backend não precisa de CORS. Em desenvolvimento, o proxy do Vite (`frontend/vite.config.js`) faz o mesmo papel com `localhost:8080`.

**Container em vez do runtime Java do Render:** o ambiente do servidor passa a ser o mesmo da máquina de qualquer integrante, e a versão do Java e do Maven vem do que o repositório declara.

---

## Passo a passo

### 1. Criar a conta e conectar o repositório

Criar conta em [render.com](https://render.com) (o plano gratuito basta) e conectar a conta do GitHub onde o repositório está.

### 2. Criar o serviço a partir do blueprint

O `render.yaml` na raiz descreve o serviço inteiro. No Render, criar um **Blueprint** apontando para o repositório — ele lê o arquivo e monta o serviço sozinho, sem preenchimento manual.

Se preferir criar como **Web Service** avulso, os valores são:

| Campo | Valor |
|---|---|
| Runtime | Docker |
| Root Directory | `backend` |
| Dockerfile Path | `./Dockerfile` |
| Health Check Path | `/health` |
| Plan | Free |

### 3. Escolher a região

**Escolha a região mais próxima do Brasil que aparecer** — normalmente uma das opções do leste dos Estados Unidos, não Oregon.

Isso não é preciosismo. A latência medida daqui até o Oracle da FIAP é de **~30 ms**; de um datacenter no exterior ela sobe para algo entre 130 e 190 ms, e **cada consulta ao banco paga esse pedágio**. Uma requisição que monta a rota faz várias consultas.

### 4. Definir as variáveis de ambiente

No painel do serviço, em *Environment*. As marcadas com `sync: false` no blueprint aparecem em branco esperando valor.

| Variável | Valor | Observação |
|---|---|---|
| `DB_URL` | `jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl` | igual para todo mundo; não é segredo |
| `DB_USER` | seu RM | credencial pessoal da FIAP |
| `DB_PASSWORD` | sua senha | **ver [O-02](observacoes.md#o-02-senha-do-oracle-passou-por-canal-de-conversa)** — trocar antes de usar aqui |
| `GEMINI_API_KEY` | chave do Google AI Studio | **usar a chave nova** ([O-01](observacoes.md#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)) |
| `PORT` | **não definir** | o Render injeta sozinho; a `Main` lê |
| `GEMINI_MODEL` | opcional | padrão `gemini-3.5-flash-lite` (D-37) |
| `MERLIN_SEED_ENABLED` | opcional | `false` desliga a carga de dados no startup; padrão ligada |
| `MERLIN_AGENDADOR_ENABLED` | opcional | `false` desliga a varredura de TTL; padrão ligada |
| `AGENDADOR_INTERVALO_MS` / `AGENDADOR_ATRASO_INICIAL_MS` | opcionais | padrões 1.800.000 (30 min) e 60.000 |
| `DB_POOL_SIZE` | opcional | quantas conexões ficam guardadas entre requisições; padrão 3, e `0` desliga ([D-96](decisoes-tecnicas.md#d-96-conexões-reaproveitadas-entre-transações-em-java-puro)). Na publicada, 5; somada aos 3 da máquina de quem cedeu a credencial, fica dentro das 20 sessões do schema |
| `MERLIN_HTTP_CONNECT_TIMEOUT_SECONDS` / `MERLIN_HTTP_READ_TIMEOUT_SECONDS` | opcionais | tempos limite do Gemini; padrões 5 e 20 |

**Variáveis que sobraram da versão Spring e podem ser apagadas do painel:** `SPRING_PROFILES_ACTIVE`, `CORS_ALLOWED_ORIGINS` (o CORS saiu; o rewrite da Vercel o substitui) e `JWT_SECRET` (nenhum código a lê). Deixá-las não quebra nada, mas engana quem ler o painel.

Nenhum desses valores vai para o repositório. O `render.yaml` declara apenas os nomes.

### 5. Ler o log do primeiro deploy

É aqui que se responde a pergunta que motivou publicar cedo: **o Oracle da FIAP aceita conexão de fora do Brasil?**

**Deu certo** — o log traz, nesta ordem:

```
Massa de demonstracao ja esta completa. Nada a carregar.
Started listener bound to [0.0.0.0:...]
[HttpServer] Started.
Servidor no ar na porta ...
```

`Massa de demonstracao ja esta completa` (ou `Carga incremental: ...`) é a prova definitiva: a aplicação consultou o catálogo no Oracle da FIAP a partir do servidor, antes de abrir a porta.

**Não deu** — o log fica preso em erro de conexão (`IO Error`, `Connection refused`, `ORA-12170`) e a aplicação não sobe. Ver a seção seguinte.

### 6. Confirmar pelo navegador

Com a URL que o Render atribuir (algo como `https://merlin-route-finder-api.onrender.com`):

- `/health` — responde `OK`, sem tocar no banco
- `/api/v1/produtos?query=tinta` — deve devolver a Tinta Acrílica da massa de demonstração

O Swagger UI e o `/v3/api-docs` não existem mais. A documentação da API é o [`openapi.yaml`](../backend/src/main/resources/openapi/openapi.yaml), conferido contra os Resources pelo `ContratoOpenApiTest`.

---

## Mudanças no schema do banco

Desde a saída do Hibernate, **a aplicação não cria nem altera tabelas**. O schema está em [`database/migrations`](../database/migrations), um arquivo por mudança, no formato do material (`CREATE SEQUENCE`/`CREATE TABLE`).

Quem muda o schema:

1. cria um script novo em `database/migrations`, com o número seguinte (`V002__...sql`);
2. roda o script no SQL Developer, no schema usado pela instância publicada, **antes** de publicar o código que depende dele;
3. avisa no commit e aqui, porque o schema é compartilhado: a instância publicada, os testes de integração e a máquina de cada integrante usam o mesmo.

A exceção são as restrições de enum (`CHECK ... IN (...)`): a carga de dados as refaz sozinha no startup, pelo `RestricaoDeEnumDao`, quando um enum ganha ou perde um valor (RF-15).

---

## Se o Oracle não conectar do servidor

O que já foi verificado, e que torna esse cenário improvável: `oracle.fiap.com.br` resolve para **187.8.12.142**, um IP público, e o acesso local sai de uma conexão residencial comum, sem VPN. Não há lista de IPs autorizados — se houvesse, a máquina de casa também não entraria.

Se ainda assim falhar, a causa mais provável é **filtro por país de origem**. Nesse caso a saída é um provedor com região no Brasil: o **Fly.io tem São Paulo (`gru`)**. O `Dockerfile` serve igual, muda só o provedor — e a latência inclusive melhora.

---

## Duas coisas a saber antes da apresentação

**A instância dorme, e acordar demora mais do que parece.** No plano gratuito o serviço é suspenso após alguns minutos sem tráfego, e a requisição seguinte espera o processo subir. Medido no primeiro deploy real:

```
Started BackendApplication in 134.503 seconds
```

**Dois minutos e quinze**, contra 21 segundos na máquina de desenvolvimento. A causa é o plano gratuito dar **0.1 CPU** — um décimo de núcleo — e o Spring inicializar na mesma proporção. **As medições desta seção são da versão Spring**; a versão com Jersey ainda precisa ser medida no Render (passo 11.6 do plano de migração). Na máquina de desenvolvimento ela sobe em poucos segundos.

> **Medido de novo em 25/08/2026, depois de o catálogo crescer: 176 segundos** — quase três minutos até a primeira resposta. A carga inicial passou a percorrer 111 produtos em vez de 29, e cada verificação é uma ida ao Oracle a 5.000 km.
>
> **Para a demonstração, isso é o número que importa.** Acordar a instância três minutos antes de começar deixou de ser recomendação e virou obrigação — e vale acordá-la de novo se a apresentação atrasar, porque o Render suspende após 15 minutos parado.

Numa demonstração ao vivo isso é fatal se pegar de surpresa. **Abrir a aplicação pelo menos cinco minutos antes de gravar ou apresentar**, e deixá-la aquecida.

A mesma CPU limitada aparece nas respostas. Medido contra a instância publicada:

| | Local | Render (gratuito) |
|---|---|---|
| Busca no catálogo | ~30 ms | ~500 ms |
| Chat com o assistente | ~1-2 s | ~8 s |

O time decidiu (23/08/2026) seguir no plano gratuito e assumir o aquecimento manual. O plano pago mais barato resolveria as duas coisas — mais CPU e sem suspensão —, e a decisão pode ser revista mais perto de 13/09, já que 4,5 dos 5 pontos do item Deploy são pela usabilidade do MVP publicado.

É também o que a [D-42](decisoes-tecnicas.md#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem) já registrava sobre a varredura de sessões não rodar com a aplicação dormindo.

---

## Depois que o frontend for publicado

O frontend chama `/api/...` sempre no próprio domínio, e o rewrite de `frontend/vercel.json` repassa para o Render. O backend não envia cabeçalhos de CORS, então o frontend nunca pode chamar o Render direto.

Desde 12/11/2026, os serviços do frontend não leem mais a variável `VITE_API_BASE_URL` (revisão da [D-91](decisoes-tecnicas.md#d-91-o-cors-sai-do-backend-a-vercel-repassa-api-para-o-render)). Se ela ainda existir em *Settings → Environment Variables*, não tem efeito, e pode ser apagada quando for conveniente.

---

## O que foi verificado antes de escrever este guia

Sem Docker nesta máquina, a imagem em si só será construída pelo Render. O que dava para verificar localmente foi verificado:

- `./mvnw clean package` gera `target/backend-0.0.1-SNAPSHOT.jar` pelo `maven-shade-plugin`, com as dependências dentro, e casa com o `backend-*.jar` que o `Dockerfile` copia (o `original-backend-*.jar`, sem as dependências, não casa);
- o jar sobe com **os mesmos parâmetros de JVM do container** (`-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC`), respeita `PORT` e escuta em `0.0.0.0`;
- `/health`, o caminho do health check, responde `200`, e as rotas de mapa, catálogo, sessão e roteiro respondem contra o Oracle (05/11/2026).

O que só o build do Render confirma: disponibilidade das imagens base e a conexão com o Oracle a partir de fora do país.
