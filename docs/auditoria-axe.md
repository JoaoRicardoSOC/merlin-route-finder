# Auditoria automatizada de acessibilidade — axe-core 4.13.0

**Rodada em 31/08/2026**, no `frontend` servido em desenvolvimento, viewport de 375 x 812.
Ferramenta: `axe-core` instalado como `devDependency` e injetado na página.

O axe é o mesmo motor que o Lighthouse usa por dentro. Ele cobre o que a auditoria
manual de 30/08 **não** cobria: papéis ARIA, rótulos de controle, ordem de cabeçalhos,
estrutura de marcos e nome/papel/valor.

---

> [!NOTE]
> **Tudo desta página foi corrigido em 31/08/2026, e a varredura foi ampliada.** As quatro
> telas e os modais fecham com **zero violações**. As decisões estão em
> [D-82, D-83 e D-84](decisoes-tecnicas.md). O que a segunda rodada encontrou está no fim.

## O resumo em uma linha

**O axe achou uma violação.** As outras três coisas desta lista ele **não** acha — e são
mais graves do que a que ele achou.

Isso é o resultado mais útil da rodada: ferramenta automática mede conformidade,
não intenção. As três abaixo saíram de olhar o que o axe devolveu *passando*.

---

## 1. `aria-modal="true"` é uma promessa que o app não cumpre — **alta**

Os **oito** modais declaram corretamente `role="dialog"` e `aria-modal="true"`:

| Modal | `role` | `aria-modal` | trava de foco | fecha com Esc |
|---|---|---|---|---|
| AIChatModal | sim | sim | **não** | **não** |
| FacetFiltersModal | sim | sim | **não** | **não** |
| FimJornadaModal | sim | sim | **não** | **não** |
| LocationCodeModal | sim | sim | **não** | **não** |
| ProductDetailModal | sim | sim | **não** | **não** |
| RupturaModal | sim | sim | **não** | **não** |
| RoteiroDrawer | sim | sim | **não** | **não** |
| SectorsDrawer | sim | sim | **não** | **não** |

**`Escape` não aparece uma única vez em todo o `src`** — há **um** tratador de teclado no
projeto inteiro.

**Por que isso é pior do que simplesmente faltar o atributo.** `aria-modal="true"` diz à
tecnologia assistiva: *ignore todo o resto da página, ela não existe agora*. O leitor de tela
obedece. Mas o **teclado** não obedece, porque nada trava o foco — então o Tab sai do modal e
entra em botões que o leitor de tela foi instruído a fingir que não estão lá. O usuário navega
para dentro de conteúdo que, para ele, não existe.

Faltar o atributo seria um app sem suporte. Ter o atributo sem a trava é um app que **mente**
sobre o próprio estado. O axe não pega porque a marcação está formalmente correta.

**Correção.** Um hook único — `useModalAcessivel(ref, aoFechar)` — que ao abrir move o foco
para o modal, prende o Tab dentro dele, fecha no `Escape` e devolve o foco ao botão de origem
ao fechar. Escrito uma vez, aplicado nos oito.

---

## 2. O nome dos ícones é lido junto com o rótulo — **alta**

Os ícones Material Symbols são renderizados por **ligadura de texto**: o `<span>` contém
literalmente a palavra `home`, `map`, `qr_code_scanner`. Esse texto entra no nome acessível
do botão que o envolve.

O que o leitor de tela anuncia na barra inferior, hoje:

| O que se vê | O que se ouve |
|---|---|
| Home | **"home Home"** |
| Mapa | **"map Mapa"** |
| Scan | **"qr_code_scanner Scan"** |
| Setores | **"grid_view Setores"** |
| Assistente | **"smart_toy Assistente"** |

O botão do banner promocional é anunciado como **"Explorar Novidades arrow_forward"**.

**Alcance medido:** **140** spans de ícone no código, em **24** componentes,
**zero** com `aria-hidden`. Só na tela inicial, **11 de 14** ícones estão dentro de um
controle e contaminam o nome dele.

**Por que o axe não pega.** Para ele o botão *tem* nome acessível — e tem. Ele não julga se o
nome faz sentido. É preciso alguém ler o resultado em voz alta para perceber.

**Correção.** `aria-hidden="true"` em todo span de ícone — é a recomendação da própria
documentação do Material Symbols. Onde o ícone é a única coisa dentro do botão, o botão
precisa de `aria-label` próprio. Vários já têm.

---

## 3. Ordem de cabeçalhos quebrada — **média** — *(esta o axe achou)*

```
heading-order — "Heading levels should only increase by one"
<h3 class="promo-title">Iluminação Inteligente &amp; Sustentável</h3>
```

O título do banner é `<h3>` sem que exista um `<h2>` antes dele. Para quem navega por
cabeçalhos — que é como se lê uma tela desconhecida com leitor de tela — o salto sugere uma
seção que não existe.

**Detalhe que vale registrar:** é o **mesmo elemento** da armadilha 2 do D-79, onde a regra
global `h1..h6 { color }` vencia a herança e deixava o título em 2,52:1. Duas falhas
diferentes, no mesmo `<h3>`, achadas por dois métodos diferentes. Vale como sinal de que
esse componente merece uma olhada inteira.

---

## 4. O que o axe confirmou que está certo

- **Zero violações de contraste** nas telas auditadas. A correção do D-79 passa numa
  ferramenta independente, que usa a mesma fórmula de luminância mas implementação diferente
  da minha. As duas concordam.
- **Nenhum problema de marcos, idioma do documento, ordem de tabulação ou rótulo de campo.**
- Os oito modais **declaram** `role` e `aria-modal` — a metade declarativa está feita, o que
  torna a correção do item 1 menor do que parece.

---

## Segunda rodada: o que apareceu ao varrer as outras telas

O relatório original dizia que só a tela inicial tinha sido varrida. Ao corrigir os três itens,
a varredura foi estendida para setores, catálogo e mapa — e **achou mais duas coisas, uma
delas causada pela própria correção**.

### 4. 39 controles mudos no catálogo — **séria**, e fomos nós

`aria-command-name` — os `<div role="button">` do cartão de produto ficaram sem nome
acessível quando o ícone recebeu `aria-hidden`. O script estático que rodou antes varreu
`<button>` e `<a>` e devolveu zero, porque **não olhava `[role="button"]`**.

**A lição é sobre o método, não sobre o defeito:** a verificação estática tinha uma lacuna que
só a medição no navegador expôs. Se a varredura tivesse parado na tela inicial — como o
relatório original — a regressão teria ido para o commit.

Ao consertar, apareceu o problema maior por trás: **quatro** controles idênticos por cartão,
nenhum acionável por teclado. Ver [D-84](decisoes-tecnicas.md).

### 5. Páginas sem `<h1>` — **moderada**

`page-has-heading-one` no catálogo e em setores: os dois abriam em `<h2>`. Cada tela passou a
ter um `<h1>` — o seu próprio título — e os níveis abaixo desceram junto para não abrir salto
novo.

### Estado final, medido

| Tela | Violações |
|---|---|
| Inicial | **0** |
| Setores | **0** |
| Catálogo (50 cartões) | **0** |
| Mapa | **0** |
| Com modal aberto | **0** |

Também verificado à mão, porque o `axe` não mede: `Escape` fecha os modais, o Tab dá a volta
dentro deles, o foco volta para quem abriu, e `Enter` no cartão abre o produto certo.

---

## O que isto não cobre

- **O detalhe do produto e o interior dos demais modais não foram varridos um a um.** Home,
  setores, catálogo, mapa e dois modais foram.
- **O axe cobre cerca de um terço dos critérios da WCAG.** Passar nele não é ser acessível —
  as três coisas mais graves desta lista saíram dele passando.
- **Nada foi testado com leitor de tela de verdade.** As leituras da tabela do item 2 são
  derivadas da árvore de acessibilidade, não ouvidas.
- **Nada foi testado com teclado físico.** A trava de foco foi exercitada por eventos
  disparados no navegador, que acionam o nosso tratador mas não movem o foco como o Tab real
  move. O comportamento nativo depende do navegador, não de nós — mas continua não medido.

---

## Como rodar de novo

Com o servidor de desenvolvimento no ar, no console da página:

```js
const s = document.createElement('script')
s.src = '/node_modules/axe-core/axe.min.js'
document.head.appendChild(s)
// depois de carregar:
const r = await axe.run(document, { resultTypes: ['violations'] })
console.table(r.violations.map(v => ({ id: v.id, impacto: v.impact, nos: v.nodes.length })))
```
