# Ferramentas de análise: o que cada uma achou

**Rodadas em 31/08 e 01/09/2026.** Este documento existe para responder duas perguntas que a
banca pode fazer — *"como vocês sabem que está bom?"* e *"o que vocês mediram?"* — e para
poupar o próximo que pensar em instalar alguma delas.

## O placar

| Ferramenta | O que mede | Achou | Adotada? |
|---|---|---|---|
| **axe-core 4.13** | Acessibilidade em tempo de execução | **3 problemas reais** + 2 numa segunda varredura | **sim**, devDependency |
| **JaCoCo 0.8.13** | Cobertura de testes | Que a suíte padrão **não roda os testes de integração** | **sim**, no `pom` |
| **Lighthouse 12.8** | Performance, acessibilidade, boas práticas, SEO | Acessibilidade **100**; performance **56** | pontual |
| **eslint-plugin-jsx-a11y** | Acessibilidade no JSX | 45 avisos, **1 defeito real** | **não** — ver abaixo |
| **SpotBugs 4.9** | Análise estática Java | 30 achados, **0 acionáveis** | **não** |

---

## axe-core — a que mais rendeu

Na primeira varredura ela achou **uma** violação — um salto de nível de cabeçalho. As duas
piores da lista ela deixou passar:

- **`aria-modal="true"` sem trava de foco** nos oito modais. A marcação estava formalmente
  correta, então nenhuma regra dispara; o defeito era o teclado sair de um modal que o leitor
  de tela tinha sido mandado tratar como a única coisa na página ([D-86](decisoes-tecnicas.md)).
- **O nome do ícone lido junto com o rótulo** — "home Home", "qr_code_scanner Scan". Para o
  `axe` o botão *tem* nome acessível, e tem; ele não julga se o nome faz sentido
  ([D-82](decisoes-tecnicas.md)).

Estender a varredura para as outras telas achou mais duas, e uma foi **causada pela própria
correção**: 39 controles ficaram mudos no catálogo porque o script estático que conferiu antes
varria `<button>` e `<a>` e não `[role="button"]` ([D-84](decisoes-tecnicas.md)).

**Ferramenta automática mede conformidade, não intenção** — e varredura parcial mede menos
ainda.

Estado final, medido: **zero violações** na tela inicial, em setores, no catálogo, no mapa, com
modal aberto e na tela de falha de sessão.

Como rodar, com o servidor de desenvolvimento no ar:

```js
const s = document.createElement('script')
s.src = '/node_modules/axe-core/axe.min.js'
document.head.appendChild(s)
// depois de carregar:
const r = await axe.run(document, { resultTypes: ['violations'] })
console.table(r.violations.map(v => ({ id: v.id, impacto: v.impact, nos: v.nodes.length })))
```

## JaCoCo — o achado não foi o número

O que ele mostrou primeiro não foi cobertura: foi que **`mvnw test` roda 150 testes e a suíte
inteira tem 285**. Os 19 testes de integração exigem `-Pintegracao` e Oracle. Rodando a
completa, **8 falhavam** — introduzidas por um commit dado como verificado com "150 testes
passam". Ver `O-36`.

```
285 testes, 0 falhas
LINHAS 93,2%  ·  INSTRUÇÕES 91,6%  ·  RAMOS 75,4%  ·  CLASSES 98,2%
Sem o perfil de integração: 150 testes, 35,1% de linhas
```

Como rodar:

```bash
DB_URL=... DB_USER=... DB_PASSWORD=... ./mvnw -Pintegracao verify
```

O relatório sai em `backend/target/site/jacoco/index.html`.

## Lighthouse — o número da acessibilidade, e um problema de fonte

Medido contra o ambiente publicado, perfil de **celular**:

| Categoria | Nota |
|---|---|
| **Acessibilidade** | **100** |
| **Boas práticas** | **100** |
| SEO | 91 → 100 depois da `meta description` |
| Performance | **56** |

**A performance é uma coisa só:** 8,6 s até a primeira pintura, com `Total Blocking Time` de
**0 ms** — ou seja, não é JavaScript. São as fontes. O `index.html` carrega **quatro famílias
tipográficas** — Hanken Grotesk, IBM Plex Sans, Source Sans 3 e Work Sans — mais os Material
Symbols, em duas folhas que bloqueiam a pintura. O Lighthouse estima ~7 s de bloqueio no perfil
de celular.

Foi feito o que não mexe no desenho: **as duas requisições viraram uma**. O resto é `O-37`,
porque reduzir famílias é decisão da dupla de frontend.

Como rodar (usa o Edge, porque não há Chrome nesta máquina):

```bash
CHROME_PATH="/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe" npx lighthouse@12 https://merlin-route-finder.vercel.app --output=json --output-path=./lh.json --form-factor=mobile --chrome-flags="--headless=new"
```

## jsx-a11y — não adotada, e o motivo é o deploy

**Ela só declara suporte até o ESLint 9, e o projeto está no 10.** Instalar com
`--legacy-peer-deps` reescreve o `package-lock`, e a Vercel instala as `devDependencies` no
build — o risco é o deploy quebrar a doze dias da entrega, por um plugin.

Foi rodada **como análise pontual** e desfeita. Dos **45 avisos**:

- **~20 são decisões que já tínhamos tomado.** O fundo escuro dos sete modais (que agora fecha
  com `Escape`, [D-86](decisoes-tecnicas.md)) e os três blocos do cartão de produto que ficaram
  só para o mouse de propósito ([D-84](decisoes-tecnicas.md)). O linter não tem como saber.
- **1 defeito real:** o logo do cabeçalho levava para o início no clique e **não existia para o
  teclado**. Virou `<button>`.
- **1 lacuna conhecida:** o mapa faz *pan* com o mouse e não tem equivalente por teclado. É
  funcionalidade, não acabamento.
- O resto são variações dos mesmos pontos.

**Vale adotar quando o plugin suportar o ESLint 10** — ela pega na escrita a classe de defeito
que só apareceu numa varredura manual inteira.

## SpotBugs — o backend passou limpo

30 achados, **nenhum acionável**:

- **27 são `EI_EXPOSE_REP`** em *records* que devolvem `List`. É o falso positivo clássico do
  SpotBugs com records imutáveis.
- **3 são `VA_FORMAT_STRING_USES_NEWLINE`**, pedindo `%n` no lugar de `\n`. E aí a ferramenta
  está errada pelo motivo mais interessante: **duas dessas strings são o prompt do LLM**. `%n`
  emite a quebra de linha do sistema operacional, então o prompt passaria a ser diferente entre
  a máquina de quem desenvolve e o servidor publicado. `\n` é o certo ali.

**Não foi adotada**: adicioná-la ao `pom` custaria 30 supressões para não encontrar nada.

---

## O que isso ensina sobre ferramentas

As duas melhores descobertas da semana **não vieram de ferramenta**:

- os 39 controles mudos do catálogo apareceram porque a varredura foi **estendida** para além
  da tela inicial;
- o defeito da navegação apareceu porque **alguém usou o app no celular**.

E a ferramenta que mais rendeu — o Oracle numa execução local — não é ferramenta de análise: é
**acesso ao que não estava sendo medido**. Ela não achou nada sozinha; ela deixou os testes que
já existiam finalmente rodarem.

Ferramenta levanta o piso. Ela não substitui olhar, e não alcança o que ninguém apontou para
ela.
