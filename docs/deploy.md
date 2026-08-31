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

O backend é uma **aplicação Spring Boot: um processo Java que fica de pé o tempo todo**, mantendo um pool de conexões com o Oracle e rodando o agendador de TTL. Isso não cabe no modelo serverless da Vercel, que liga e desliga a cada requisição — por isso o backend vai para o Render e o frontend (build estático do Vite) para a Vercel.

Não é gambiarra: **frontend e backend já são dois programas separados hoje**, conversando por HTTP em `localhost:5173` e `localhost:8080`. O deploy só troca `localhost` por URLs públicas.

O que liga os dois são duas variáveis apontando uma para a outra:

| Quem | Variável | Aponta para |
|---|---|---|
| Frontend | a variável de API do Vite (definida pela dupla de frontend) | URL pública da API |
| Backend | `CORS_ALLOWED_ORIGINS` | URL pública do frontend |

Sem a segunda, o navegador **bloqueia** as respostas da API mesmo com o backend funcionando — é a causa nº 1 de "funciona no Postman mas não no site".

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
| Health Check Path | `/v3/api-docs` |
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
| `SPRING_PROFILES_ACTIVE` | `prod` | já vem do blueprint |
| `DB_POOL_SIZE` | `5` | o schema da FIAP permite **20 sessões por usuário**, divididas entre a instância publicada e a máquina de quem cedeu a credencial ([D-46](decisoes-tecnicas.md#d-46-o-pool-de-conexões-é-dimensionado-pelos-limites-reais-do-schema-da-fiap)) |
| `CORS_ALLOWED_ORIGINS` | deixar em branco por ora | preenchido quando o frontend for publicado |
| `PORT` | **não definir** | o Render injeta sozinho |

Nenhum desses valores vai para o repositório. O `render.yaml` declara apenas os nomes.

### 5. Ler o log do primeiro deploy

É aqui que se responde a pergunta que motivou publicar cedo: **o Oracle da FIAP aceita conexão de fora do Brasil?**

**Deu certo** — o log traz, nesta ordem:

```
The following 1 profile is active: "prod"
HikariPool-1 - Start completed.
Catalogo ja possui dados. Carga inicial ignorada.
Tomcat started on port ...
Started BackendApplication in 134.503 seconds
```

`Catalogo ja possui dados` é a prova definitiva: a aplicação consultou a tabela `TB_PRODUTO` no Oracle da FIAP a partir do servidor.

**Não deu** — o log fica preso em erro de conexão (`IO Error`, `Connection refused`, `ORA-12170`) e a aplicação não sobe. Ver a seção seguinte.

### 6. Confirmar pelo navegador

Com a URL que o Render atribuir (algo como `https://merlin-route-finder-api.onrender.com`):

- `/swagger.html` — a documentação navegável da API
- `/api/v1/produtos?query=tinta` — deve devolver a Tinta Acrílica da massa de demonstração

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

**Dois minutos e quinze**, contra 21 segundos na máquina de desenvolvimento. A causa é o plano gratuito dar **0.1 CPU** — um décimo de núcleo — e o Spring inicializar na mesma proporção.

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

Dois valores no painel do Render, sem recompilar nada:

| Variável | Valor |
|---|---|
| `CORS_ALLOWED_ORIGINS` | a URL do frontend, sem barra no fim — ex.: `https://merlin-route-finder.vercel.app` |

`CORS_ALLOWED_ORIGINS` aceita várias origens separadas por vírgula, se houver mais de um endereço de frontend.

> [!WARNING]
> **Enquanto o frontend não estiver publicado, esta variável tem que continuar vazia.** Vazia, o backend cai no padrão do `application.yml`, que libera `localhost:5173` — e é assim que o time desenvolve. Preencher com a URL de produção **antes** de haver produção derruba o ambiente local de todo mundo, com o sintoma clássico e enganoso: a API responde no Postman e o navegador bloqueia.
>
> Medido em 30/08: um servidor de desenvolvimento subiu na porta 5174 em vez da 5173 e todas as chamadas voltaram **403**. A causa foi a mesma — origem fora da lista.

Salvar reinicia o serviço. **Este é o passo que fecha a integração** — antes dele o navegador bloqueia as chamadas do frontend, mesmo com a API respondendo normalmente.

---

## O que foi verificado antes de escrever este guia

Sem Docker nesta máquina, a imagem em si só será construída pelo Render. O que dava para verificar localmente foi verificado:

- `./mvnw clean package -DskipTests` gera `target/backend-0.0.1-SNAPSHOT.jar`, que casa com o `backend-*.jar` que o `Dockerfile` copia;
- `./mvnw dependency:go-offline`, a camada de cache do build, funciona;
- o jar empacotado sobe com **os mesmos parâmetros de JVM do container** (`-XX:MaxRAMPercentage=70.0 -XX:+UseSerialGC`), respeita `PORT`, ativa o perfil `prod`, e não emite log de SQL;
- `/v3/api-docs`, o caminho do health check, responde `200`.

O que só o build do Render confirma: disponibilidade das imagens base e a conexão com o Oracle a partir de fora do país.
