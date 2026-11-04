# Prompt para IA de vídeo — apresentação do Merlin Route Finder

> Este documento é o briefing completo para gerar, com uma IA de vídeo (Sora, Veo, Runway,
> Kling, Pika, Synthesia, HeyGen ou similar), o vídeo de apresentação do projeto.
>
> Está em três partes:
>
> 1. **Prompt mestre** — o briefing inteiro, para ferramentas que aceitam texto longo ou para
>    um editor humano.
> 2. **Prompts por cena** — um bloco curto e autocontido por cena, para ferramentas que geram
>    clipes de 5 a 20 segundos e aceitam pouco texto.
> 3. **Checklist de pós-produção** — o que a IA não faz bem e precisa ser montado na edição
>    (telas reais do app, textos na tela, logo).

---

## 1. Prompt mestre

### 1.1 Resumo em uma frase

Vídeo de apresentação de **1min45s a 2min**, em **português do Brasil**, do **Merlin Route
Finder**: um aplicativo web para celular que ajuda o cliente da **Leroy Merlin** a encontrar,
dentro de uma loja física de até 10.000 m², o produto exato que veio buscar — sem instalar
nada, a partir de uma placa com QR Code no corredor.

### 1.2 Contexto do projeto (para a IA entender o que está mostrando)

- **O que é:** projeto do **FIAP Challenge 2026**, com a Leroy Merlin como empresa parceira.
  Desafio: *"Experiência em Loja — otimização da jornada do cliente em loja com IA"*.
- **A dor atacada:** o **"problema do último metro"**. A loja é enorme, tem milhares de itens
  técnicos, e o cliente sabe o que quer mas não sabe **onde** está. Acaba dependendo de achar
  um vendedor.
- **A solução:** uma página web aberta no próprio celular do cliente, que:
  1. **Sabe onde ele está** — cada corredor tem uma placa com QR Code, URL curta e um código
     de localização (ex.: `TIN-02`). A câmera do celular lê o QR e o app abre já posicionado.
  2. **Monta a lista de compras** — catálogo com busca tolerante a erro de digitação
     ("tnta" encontra "Tinta", "furadera" encontra "Furadeira") e filtros que mudam conforme
     a seção (em Tintas aparece *Grão*; em Elétrica aparece *Amperagem*).
  3. **Mostra cada item na planta real da loja** — a planta foi **decalcada vértice a vértice**
     da planta técnica da Leroy: **21 seções, 212 gôndolas**. Um marcador para o cliente
     ("você está aqui") e um para cada item da lista. Item coletado muda de aparência.
  4. **Tem um assistente de IA** (Google Gemini) que responde perguntas como *"o que eu
     preciso para pintar uma parede?"* citando **somente produtos que existem nesta loja**,
     com o corredor de cada um — ele consulta o catálogo antes de responder, em vez de inventar.
  5. **Resolve a prateleira vazia** — se o produto não está na gôndola, o cliente toca
     *"não encontrei este produto"* e o app sugere o substituto mais próximo que cumpre a mesma
     função. Um toque em *"levar este"* troca o item na lista.
  6. **Fecha a jornada** — ao coletar o último item (ou ao encerrar com o que tem), o app
     agradece e, se o cliente quiser, marca os caixas no mapa.
- **O cliente decide o caminho.** Por orientação da própria Leroy Merlin, o app **não calcula
  rota otimizada**: a loja tem um circuito pensado para o cliente passar por outras coisas. O
  app mostra onde o cliente está e onde está cada item. O nome continua fazendo sentido:
  *quem encontra a rota é o cliente*.
- **Princípio que atravessa o projeto:** *"quando o sistema não sabe, ele diz que não sabe."*
  Item sem localização conhecida não ganha alfinete no mapa; o assistente nunca cita produto
  inventado; sem conexão, o app avisa em vez de fingir.
- **Stack técnica:** backend em **Java 21 + Spring Boot**, arquitetura **hexagonal** (o
  domínio é Java puro, sem framework); banco **Oracle**; IA **Google Gemini com function
  calling**; frontend **React 19 + Vite**, *mobile-first*; lista *local-first* (funciona
  mesmo se o sinal cair no fundo do corredor); publicado em **Vercel** (frontend) e
  **Render** (backend). **274 testes automatizados** e **91 decisões técnicas documentadas**.
- **Equipe (5 pessoas):** João Ricardo e Caio (backend), Vicentini (banco de dados), Bielecky
  e Marcela (frontend e design).
- **Endereço:** merlin-route-finder.vercel.app

### 1.3 Público e objetivo

- **Público principal:** a banca da FIAP e os representantes da Leroy Merlin.
- **Público secundário:** colegas, recrutadores, quem visitar o repositório.
- **O que o espectador precisa sair sabendo:**
  1. qual é a dor (achar o produto numa loja gigante);
  2. como o app resolve, em quatro gestos: **escanear → escolher → localizar → coletar**;
  3. o diferencial: **planta real + IA que não inventa + prateleira vazia resolvida**;
  4. que é um produto funcionando, publicado, com engenharia séria por trás.
- **Sensação desejada:** alívio e confiança. Do "estou perdido num galpão" para "sei
  exatamente para onde ir". Moderno, humano, prático — nunca futurista demais nem frio.

### 1.4 Tom, linguagem e narração

- **Idioma:** português do Brasil, linguagem simples, frases curtas.
- **Voz:** narrador(a) adulto(a), 25–40 anos, timbre caloroso e confiante, ritmo médio
  (≈150 palavras/minuto), sorriso na voz. Nada de tom de comercial de TV gritado.
- **Tom:** próximo, prático, levemente bem-humorado na abertura; seguro e claro na parte
  técnica; inspirador no fechamento.
- **Evitar:** jargão sem explicação, superlativos vazios ("revolucionário", "o melhor do
  mundo"), promessas que o app não cumpre (rota otimizada, integração com ERP, estoque em tempo
  real, leitor de QR próprio, avaliações de produto).

### 1.5 Identidade visual

**Paleta (extraída do design system do app — usar estes valores):**

| Papel | Cor | Hex | Uso no vídeo |
|---|---|---|---|
| Verde da marca | verde-limão Leroy | `#78BE20` | cor dominante: marcadores, destaques, transições, CTA |
| Verde escuro | verde floresta | `#00702F` | textos sobre fundo claro, títulos, linhas da planta |
| Verde claro | verde névoa | `#F3F9EA` | fundos de cartões e telas de título |
| Texto principal | quase preto | `#1D1D1B` | tipografia principal |
| Texto sobre verde | verde profundo | `#1B3800` | texto em cima do `#78BE20` (branco não tem contraste) |
| Fundo | off-white | `#FAFAF8` | fundo geral das telas de título |
| Superfície | branco | `#FFFFFF` | cartões, telas do celular |
| Destaque | dourado | `#E5A900` | uso raro: substituto sugerido, "item encontrado" |
| Alerta | vermelho | `#BA1A1A` | só na cena da prateleira vazia, com moderação |
| Neutro | cinza-oliva | `#5D6457` | legendas, textos secundários |

- **Proporção de cor:** ~60% branco/off-white, ~25% verdes, ~10% preto/cinza, ~5% dourado e
  vermelho somados.
- **Tipografia:** **Hanken Grotesk** (a fonte do app). Títulos em peso 700–800, corpo em 400–500.
  Ícones no estilo **Material Symbols Outlined**, traço arredondado.
- **Formas:** cantos arredondados (12–20 px), sombras suaves e difusas, muito espaço em branco.
  Estética *flat* com leve profundidade — nada de neon, *glitch* ou cyberpunk.
- **Logo:** logo oficial da Leroy Merlin apenas na abertura e no fechamento, inserido na edição
  (não deixar a IA gerar ou redesenhar o logo).

### 1.6 Direção de arte e câmera

- **Mundo físico:** loja de materiais de construção e decoração **genérica**, ampla, bem
  iluminada, pé-direito alto, gôndolas longas com tintas, ferramentas, elétrica, jardim. Luz
  natural e fluorescente suave, temperatura neutra/levemente quente. Sem logotipos legíveis de
  outras marcas.
- **Pessoas:** elenco diverso brasileiro. Protagonista: pessoa de 30–45 anos, roupa casual,
  fazendo uma pequena reforma em casa. Participação breve de um profissional de obra
  (empreiteiro com lista longa) para representar o público B2B.
- **Celular:** smartphone genérico, sem marca visível, segurado na vertical. As telas do app
  devem ser **as telas reais**, inseridas na pós-produção (ver seção 3). Na geração, pedir tela
  verde/neutra ou tela brilhante desfocada.
- **Movimento de câmera:** gimbal suave, *dolly-in* lento em momentos de revelação, câmera na
  mão leve nas cenas de "estar perdido". Planos-detalhe nas mãos e no celular.
- **Gráficos animados (motion graphics):** linhas finas verdes, marcadores em forma de gota que
  "caem" com leve quique, planta da loja vista de cima desenhando-se traço a traço.
- **Ritmo de edição:** cortes de 2 a 4 segundos na abertura; planos mais longos na
  demonstração; montagem rápida na parte técnica; respiro no final.
- **Formato:** principal **16:9, 1920×1080, 30 fps**. Versão alternativa **9:16, 1080×1920**
  para redes sociais (reenquadrar no celular).

### 1.7 Trilha e som

- **Música:** instrumental, *lo-fi/indie pop* otimista, 95–110 BPM, com violão ou piano leve e
  percussão discreta. Começa contida (pad grave, levemente tenso na cena do problema), abre na
  primeira aparição do app e cresce até o fechamento.
- **Efeitos:** ambiente de loja suave (passos, carrinho, burburinho baixo) nas cenas físicas;
  "pop" curto quando um marcador cai no mapa; "clique" ao escanear o QR; "plim" suave ao marcar
  item coletado.
- **Mixagem:** narração sempre em primeiro plano; música a −18 dB sob a voz.
- **Legendas:** embutidas em português, branco com contorno sutil ou caixa `#1D1D1B` a 70%,
  terço inferior, Hanken Grotesk 500.

### 1.8 Fluxo do vídeo (roteiro cena a cena)

| # | Tempo | Cena | Visual | Narração (PT-BR) | Texto na tela |
|---|---|---|---|---|---|
| 1 | 0:00–0:06 | **Abertura** | Fundo `#FAFAF8`. Uma linha verde `#78BE20` corre pela tela e desenha a silhueta de uma planta de loja vista de cima. Logo Leroy Merlin + título surgem. | *(sem narração — só música)* | **Merlin Route Finder** · *FIAP Challenge 2026 × Leroy Merlin* |
| 2 | 0:06–0:20 | **O problema** | Protagonista entra numa loja enorme. Plano aberto mostrando a escala. Câmera na mão; ela olha de um lado ao outro, consulta um papel com a lista, para num corredor errado, procura um vendedor que não está por perto. | "Dez mil metros quadrados. Milhares de produtos. E você só precisa de três. O difícil não é escolher o que comprar — é achar onde está." | **Até 10.000 m²** · *"o último metro"* |
| 3 | 0:20–0:30 | **A placa** | *Dolly-in* numa placa afixada numa coluna do corredor: QR Code, URL curta e o código `TIN-02`. A mão aponta o celular, a câmera nativa lê, o app abre. Flash verde suave. | "Agora, cada corredor tem uma placa. Aponte a câmera do celular e pronto: sem instalar nada, o app já sabe onde você está." | **Sem instalar nada** · *"Você está em: Corredor central, próximo a Tintas"* |
| 4 | 0:30–0:45 | **Escolher** | Tela real do app no celular: catálogo, digitação de "tnta" virando "Tinta", filtro por seção, cartão de produto, toque em "adicionar à lista", contador sobe. Corte para o empreiteiro adicionando muitos itens rapidamente. | "Procure como você fala — até digitando errado. Filtre por seção, por marca, por característica. Monte a lista inteira, de uma lâmpada a uma obra." | **Busca tolerante a erro** · **Lista sem limite de itens** |
| 5 | 0:45–1:02 | **Localizar — o mapa** | A planta da loja se desenha de cima, traço a traço, em verde-escuro sobre branco: seções, gôndolas. Cai o marcador "você está aqui"; depois caem, um a um, os marcadores dos itens da lista. Volta para a protagonista caminhando com confiança, celular na mão. | "Cada item aparece na planta de verdade da loja — decalcada da planta técnica: vinte e uma seções, duzentas e doze gôndolas. Você vê onde está, onde está cada produto, e escolhe o seu caminho." | **Planta real: 21 seções · 212 gôndolas** · *"Quem encontra a rota é você."* |
| 6 | 1:02–1:15 | **Perguntar — o assistente** | Tela do chat: a protagonista digita *"o que eu preciso para pintar uma parede?"*. A resposta aparece com cartões de produtos (tinta, rolo, fita crepe, lixa), cada um com o corredor. Ela toca para adicionar. | "Na dúvida, pergunte. O assistente com IA consulta o catálogo desta loja antes de responder — e só indica o que existe aqui, com o corredor de cada item." | **IA que não inventa** · *Google Gemini + catálogo da loja* |
| 7 | 1:15–1:27 | **Prateleira vazia** | Ela chega à gôndola: um espaço vazio na prateleira (destaque discreto em `#BA1A1A`). Toca *"não encontrei este produto"*. Surge um cartão com borda dourada `#E5A900`: o substituto, com foto e corredor ao lado. Toca *"levar este"*; o item troca na lista e um novo marcador cai no mapa. | "Acabou na prateleira? Um toque e o app sugere o substituto mais próximo que faz a mesma coisa. Mais um toque, e ele já está na sua lista." | **Ruptura vira solução** |
| 8 | 1:27–1:37 | **Fim da jornada** | Ela marca o último item: animação de check verde, todos os marcadores do mapa ficam "coletados". Tela de agradecimento, com os caixas destacados no mapa. Plano dela saindo sorrindo com as compras. | "Pegou tudo — ou decidiu levar só o que encontrou? Tanto faz: o app agradece e mostra o caminho até o caixa." | ✔ **Jornada concluída** |
| 9 | 1:37–1:50 | **Por trás** | Montagem rápida de *motion graphics* sobre fundo `#F3F9EA`: camadas de arquitetura hexagonal (domínio no centro), ícones de Java, Spring Boot, React, Oracle, Gemini; um contador sobe até **274 testes**; páginas de documentação deslizando; um celular perdendo sinal e a lista continuando lá. | "Por trás, engenharia de verdade: Java e Spring Boot com arquitetura hexagonal, React, Oracle, Gemini. Duzentos e setenta e quatro testes automatizados. Noventa e uma decisões técnicas documentadas. E uma regra: quando o sistema não sabe, ele diz que não sabe." | **Java 21 · Spring Boot · React 19 · Oracle · Gemini** · **274 testes** · **91 decisões documentadas** |
| 10 | 1:50–2:00 | **Fechamento** | Volta para a planta vista de cima; a câmera se afasta e a planta vira o ícone do app. Logo Leroy Merlin, nome do projeto, URL, nomes da equipe. Fade para branco. | "Merlin Route Finder. Menos tempo procurando, mais tempo construindo." | **merlin-route-finder.vercel.app** · *Equipe: João Ricardo · Caio · Vicentini · Bielecky · Marcela* · *FIAP Challenge 2026* |

> **Duração total: 2:00.** Para uma versão de **60 s**, manter as cenas 1, 2 (curta), 3, 5,
> 6, 7 e 10, e resumir a cena 9 numa única frase.

### 1.9 O que **não** mostrar (restrições de veracidade)

O projeto é rigoroso com isso; o vídeo precisa ser também.

- **Não** mostrar uma linha de rota otimizada traçando o caminho "ideal" — o app não calcula
  rota. No máximo, marcadores e o cliente caminhando por escolha própria.
- **Não** mostrar estrelas, notas ou avaliações de produto — o app não tem, de propósito.
- **Não** mostrar estoque "em tempo real" nem integração com sistemas da loja (ERP).
- **Não** mostrar um leitor de QR dentro do app — quem lê é a câmera nativa do celular.
- **Não** mostrar totem, quiosque ou tela fixa na loja — tudo acontece no celular do cliente.
- **Não** mostrar gôndolas desenhadas nas áreas de Materiais de construção e frente de caixas
  — na planta do app essas áreas aparecem só com contorno e nome.
- **Não** usar logos de outras redes varejistas nem marcas de celular.
- **Não** gerar o logo da Leroy Merlin pela IA — inserir o arquivo oficial na edição.

---

## 2. Prompts por cena (curtos, para colar na ferramenta)

Cada bloco é autocontido. Usar com o **sufixo de estilo** abaixo colado no fim de todos, para
manter consistência entre clipes.

**Sufixo de estilo (colar no fim de cada prompt):**

> Bright, clean, modern commercial look. Color palette dominated by white and lime green
> #78BE20 with deep green #00702F accents, warm neutral lighting, soft shadows, shallow depth
> of field, smooth gimbal camera, 16:9, 30fps, photorealistic, no visible brand logos, no text
> generated in frame.

**Cena 1 — Abertura (motion graphics)**
> Minimalist motion graphic on an off-white #FAFAF8 background: a single thin lime-green
> #78BE20 line travels across the screen and draws the top-down floor plan of a large retail
> store — outer walls, then rows of rectangular shelving aisles in deep green #00702F, drawn
> stroke by stroke. Camera slowly pulls back as the plan completes. Elegant, calm, 6 seconds.

**Cena 2 — O problema**
> A Brazilian woman in her late 30s, casual clothes, walks into a huge, brightly lit home
> improvement warehouse store with very high ceilings and endless long aisles of paint cans,
> tools and electrical supplies. She holds a small paper shopping list, looks left and right,
> slightly lost and overwhelmed. Wide establishing shot emphasizing the scale, then handheld
> medium shot of her confused expression. No store employees nearby. 12 seconds.

**Cena 3 — A placa**
> Slow dolly-in on a clean green-and-white sign mounted on a store column at the end of an
> aisle; the sign has a large QR code and a short code label. A hand enters the frame holding a
> generic smartphone vertically and points its camera at the QR code; the phone screen glows
> softly with a bright neutral screen (to be replaced in post). Close-up, shallow depth of
> field, 8 seconds.

**Cena 4 — Escolher**
> Over-the-shoulder close-up of a woman standing in a home improvement store aisle, scrolling
> and tapping on a smartphone held vertically; the screen shows a bright white app interface
> with green accents (screen to be replaced in post). Quick cut to a Brazilian construction
> contractor in work clothes rapidly tapping items on his phone next to a cart. 12 seconds.

**Cena 5 — O mapa (motion graphics)**
> Top-down 2D floor plan of a large retail store drawn in deep green #00702F lines on white:
> 21 labeled zones and many rows of shelving. A lime-green #78BE20 teardrop pin with a pulsing
> halo drops in labeled as the shopper's position, then several smaller pins drop one by one
> with a gentle bounce over different aisles. No route line connecting them. Flat, clean
> infographic style, soft shadows, 10 seconds.

**Cena 5b — Caminhando com confiança**
> Tracking shot following a woman walking confidently down a bright home improvement store
> aisle, glancing at the smartphone in her hand and then at the shelves, smiling slightly,
> turning into the correct aisle. Smooth gimbal, warm light, 6 seconds.

**Cena 6 — O assistente**
> Close-up of a woman's hands typing a question on a smartphone held vertically in a store
> aisle, bright screen with a chat interface feel (to be replaced in post); she pauses, reads,
> nods, then taps the screen to add an item. Background softly blurred shelves of paint cans,
> rollers and tape. 10 seconds.

**Cena 7 — Prateleira vazia**
> Medium shot: a woman arrives at a shelf in a home improvement store and finds an empty gap
> where a product should be, the rest of the shelf fully stocked. She looks at her phone, taps
> it, reads, then smiles and walks a few meters to a neighboring shelf and picks up a similar
> product. Natural, realistic, 12 seconds.

**Cena 8 — Fim da jornada**
> A woman with a full shopping basket taps her phone one last time, smiles with relief, and
> walks toward the checkout area of a bright home improvement store. Warm, uplifting mood,
> slow motion at the end, 10 seconds.

**Cena 9 — Por trás (motion graphics)**
> Clean tech motion graphic on a very light green #F3F9EA background: concentric hexagonal
> layers assemble around a central core labeled "domain", connected by thin deep-green lines;
> abstract code cards and document pages slide in and stack neatly; a counter animates upward;
> a phone icon loses its signal bars while a checklist on its screen stays intact. Flat design,
> rounded corners, soft shadows, 13 seconds.

**Cena 10 — Fechamento (motion graphics)**
> The top-down green store floor plan from the opening, viewed from above; the camera pulls
> back smoothly as the plan shrinks and settles into a rounded-square app icon in lime green
> #78BE20 centered on an off-white background, leaving clean empty space below for a logo and
> credits. Calm, conclusive, 10 seconds.

---

## 3. Checklist de pós-produção

O que a IA não deve inventar e que a edição precisa inserir:

- [ ] **Telas reais do app** (gravar o celular com o app publicado ou capturar a tela do
      navegador em modo mobile, 414 px de largura) para as cenas 3, 4, 6, 7 e 8. Fazer
      *screen replacement* sobre a tela neutra gerada. Gravar na véspera seguindo
      [`roteiro-de-qa.md`](roteiro-de-qa.md).
- [ ] **Acordar o backend antes de gravar:** ele hiberna no plano gratuito e leva cerca de
      **3 minutos** para subir na primeira abertura.
- [ ] **Planta real** para a cena 5: exportar a planta desenhada pelo app (tela Mapa) e animá-la
      traço a traço no After Effects/CapCut, em vez de usar a planta genérica da IA — é o
      diferencial do projeto, vale mostrar a verdadeira.
- [ ] **Logo oficial da Leroy Merlin** (`frontend/src/assets/leroy_logo.png`) nas cenas 1 e 10.
- [ ] **Textos na tela** da coluna "Texto na tela" do roteiro, em Hanken Grotesk
      (`frontend/public/fontes/`), animados com *fade + slide* de 8 px, 250 ms, curva
      `cubic-bezier(0.4, 0, 0.2, 1)` — a mesma do app.
- [ ] **Narração**: gravar com voz humana ou TTS em PT-BR (ElevenLabs, Azure Neural —
      vozes como "Francisca" ou "Antonio"), com o texto da coluna "Narração".
- [ ] **Legendas** embutidas em PT-BR.
- [ ] **Créditos**: nomes da equipe e a URL `merlin-route-finder.vercel.app`.
- [ ] **Revisão de veracidade** contra a seção 1.9 antes de exportar.
- [ ] **Exportar** em 16:9 1080p (H.264, ~12 Mbps) e uma versão 9:16 para redes.
