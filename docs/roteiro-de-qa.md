# Roteiro de QA do ambiente publicado

> Verificação a ser feita **contra <https://merlin-route-finder.vercel.app>**, não em ambiente
> local. Existe porque a auditoria de [`quebras-de-fluxo.md`](quebras-de-fluxo.md) foi feita
> localmente, e há defeitos que só aparecem publicado — partida a frio, cota do Gemini, sessão
> guardada no navegador.
>
> **Quando rodar:** na véspera da gravação, e de novo logo antes de gravar. Leva cerca de 30
> minutos.
>
> **Este roteiro não substitui a auditoria.** Ela cobre 34 cenários de falha; aqui estão os que
> o vídeo atravessa, mais as armadilhas que só existem em produção.

---

## Antes de começar

Três coisas que, se ignoradas, invalidam o resultado.

| # | O quê | Como |
|---|---|---|
| 0.1 | **O publicado é o que está na `main`?** | Procurar **marcas de conteúdo** no pacote servido, não comparar o hash do arquivo. Ver abaixo. |
| 0.2 | **Limpar o armazenamento do navegador** | `localStorage.clear()` no console, ou aba anônima. Sem isso, a sessão de um teste anterior contamina o seguinte. |
| 0.3 | **Confirmar que o backend responde** | `curl -s -o /dev/null -w "%{http_code} %{time_total}\n" --max-time 20 https://merlin-route-finder-api.onrender.com/api/v1/produtos/secoes`. Se não responder em 20 s, ele hibernou. |

### Como conferir se o publicado é o atual (item 0.1)

**Não compare o nome do arquivo.** A Vercel compila com uma versão de Node diferente da local, e
o hash do pacote JavaScript diverge mesmo quando o código é idêntico — o que produz alarme falso.
Procure texto que só existe na versão nova:

```bash
U=https://merlin-route-finder.vercel.app
JS=$(curl -s $U | grep -oP 'assets/index-[A-Za-z0-9_-]+\.js')
curl -s "$U/$JS" | grep -c "hiberna quando"        # 1 = versão atual
curl -s "$U/$JS" | grep -c "até dois minutos"      # 0 = o texto antigo saiu
```

> [!CAUTION]
> **Não comece as seções 2 a 7 com o backend dormindo.** Ele hiberna em cerca de 15 minutos sem
> uso, e volta a dormir **no meio de um QA demorado**. Com ele frio, a tela fica no estado de
> espera e **tudo parece quebrado**: o chip diz que não sabe a posição, a busca não devolve nada,
> o assistente não responde.
>
> Isso aconteceu na primeira execução deste roteiro: o item 2.2 foi dado como reprovado, e era o
> servidor dormindo. Reconfira o item 0.3 sempre que um passo falhar de forma estranha.
>
> A partida a frio (seção 1) é testada **de propósito**, uma vez, e depois o backend fica quente
> para o resto.
>
> Para o QA longo e para a gravação, deixe rodando em outro terminal:
> `node ferramentas/gravacao/manter-acordado.mjs`

> [!WARNING]
> **A armadilha mais cara é a sessão guardada.** O `localStorage` mantém `sessaoId`, roteiro e
> dados da sessão. Testar código inválido **sem limpar** faz a tela exibir a posição da sessão
> anterior — e isso parece exatamente um defeito grave que não existe.

---

## 1. Partida a frio — testar uma vez, de propósito

O servidor hiberna. **A primeira abertura do dia é o que o cliente real enfrenta.**

| # | Passo | Esperado |
|---|---|---|
| 1.1 | Não usar o app por 20 minutos. Abrir. | Faixa no topo: *"Preparando o sistema… o servidor hiberna quando ninguém está usando"*, com **cronômetro** contando. |
| 1.2 | Esperar. Cronometrar. | Carrega. Medições anteriores: 106 s, 176 s, 180 s, 183 s. |
| 1.3 | Passando de 3 minutos | O texto muda para *"está demorando mais que o normal, mas não travou"*. |

**Reprovado se** a faixa prometer prazo fixo, ou se o cronômetro não andar.

---

## 2. Entrada e posição

| # | Passo | Esperado |
|---|---|---|
| 2.1 | Abrir sem parâmetro nenhum | Chip diz *"Ainda não sabemos onde você está"*. **Não pode afirmar a entrada.** |
| 2.2 | Abrir com `?ponto=TIN-02` | Chip: *"Você está em — Corredor de Tintas"*. |
| 2.3 | Abrir com `?ponto=ZZZ-99` | Aviso: *"Não encontramos a localização…"*. O app **entra assim mesmo**, sem posição. |
| 2.4 | Modal de localização → Plano B → digitar `tin02` | Aceita minúscula e sem hífen. Posição atualiza. |
| 2.5 | Recarregar a página | A sessão e a lista voltam. |

> [!NOTE]
> **O aviso de 2.3 é um _toast_ e desaparece.** Olhar a tela dez segundos depois faz parecer que
> ele não existe. Ficar de olho desde o carregamento.

---

## 3. Catálogo

| # | Passo | Esperado |
|---|---|---|
| 3.1 | Buscar `tinta` | Resultados com foto, preço e corredor. |
| 3.2 | Buscar `tintta` (erro de digitação) | Ainda acha. A busca tolera erro. |
| 3.3 | Buscar `xyzabc` | *"Nenhum produto encontrado"* — e **não** uma tela de erro. |
| 3.4 | Aplicar filtro que zera o resultado | Oferece limpar, e diz quais filtros ainda têm resultado. |
| 3.5 | Abrir um produto | Detalhe com corredor, estoque e botão de adicionar. |

---

## 4. Assistente — o eixo de IA da avaliação

| # | Passo | Esperado |
|---|---|---|
| 4.1 | *"O que eu preciso para pintar uma parede?"* | Resposta citando produtos **pelo nome completo**, e **cartões** abaixo com foto, corredor e preço. |
| 4.2 | Conferir os cartões | Sem sobreposição entre preço e botões. Nome legível em até duas linhas. |
| 4.3 | Tocar em *Adicionar ao Roteiro* num cartão | Entra na lista, com aviso citando o corredor. |
| 4.4 | *"Quem ganhou o jogo ontem?"* | Recusa educada em uma frase, e oferta de ajuda com o projeto. **Não responde ao mérito.** |

> [!CAUTION]
> **A resposta varia entre chamadas.** No mesmo dia, a mesma pergunta devolveu 4 cartões numa
> execução e 3 em outra. É comportamento do modelo. **Ensaiar mais de uma vez antes de gravar**,
> e não contar com um número exato de cartões na narração.

> [!CAUTION]
> **A cota gratuita é de 5 chamadas por minuto.** Um QA que faça seis perguntas seguidas esgota
> a cota e o assistente responde que está indisponível — o que parece defeito e não é. Espaçar.

---

## 5. Lista e mapa

| # | Passo | Esperado |
|---|---|---|
| 5.1 | Abrir o mapa com a lista vazia | Diz que a lista está vazia; não desenha rota. |
| 5.2 | Com 3 itens, abrir o mapa | Planta com **21 seções e as gôndolas desenhadas**; alfinetes numerados nos itens. |
| 5.3 | Tocar numa seção | Realce da forma. Repare em **Madeiras**, que tem 6 lados: o clique respeita o recorte. |
| 5.4 | Zoom e arraste | Respondem. |
| 5.5 | Marcar um item como coletado | Contador sobe. Marcar duas vezes não duplica. |

> [!NOTE]
> **Materiais de construção e a frente de caixas aparecem sem gôndola nenhuma** — só o contorno
> e o nome. É [decisão registrada](decisoes-tecnicas.md#d-91-departamento-sem-gôndola-traçada-fica-vazio-e-o-vazio-é-a-resposta),
> não defeito: a organização do pátio não é distinguível na planta. Materiais é a maior área da
> tela, então **vazio chama atenção** — não vale dar close nele no vídeo.

---

## 6. Ruptura e encerramento

> [!IMPORTANT]
> **Escolha o produto certo para este teste, ou ele "falha" sem haver defeito.**
>
> O botão não exige estoque zero: **qualquer item do roteiro** pode ser relatado como
> prateleira vazia — é o cliente quem constata, não o sistema ([D-23](decisoes-tecnicas.md)).
> Mas a qualidade da sugestão depende de existir outro produto do **mesmo tipo** por perto, e
> **64 dos 111 produtos não têm nenhum** ([O-40](observacoes.md)). Para esses, o assistente
> **recusa** — e recusar é o comportamento correto, não um defeito.
>
> **Use um dos cinco pares plantados na massa**, que existem exatamente para isto:
>
> | relate ruptura em | e deve vir |
> |---|---|
> | `SKU-ILU-001` lâmpada LED branca | `SKU-ILU-005`, outra branca |
> | `SKU-TIN-003` lixa grão 120 | `SKU-TIN-004`, lixa d'água 150 |
> | `SKU-ENC-004` sifão sanfonado | `SKU-ENC-005`, sifão copo |
> | `SKU-FER-002` trena 5 m | `SKU-FER-003`, trena 7,5 m |
> | `SKU-MAT-001` argamassa AC-II | `SKU-MAT-003`, AC-III |
>
> **Verificado no ambiente publicado em 08/09:** o `SKU-ILU-004` (lâmpada 3000 K) rendeu
> *"possui a mesma tonalidade amarela (3000k) e está logo ao lado na mesma prateleira"*. Já o
> `SKU-TIN-012` (pincel), que não tem outro pincel na loja, devolveu **422 com recusa
> honesta** — *"não temos outro pincel ou trincha disponível"*. **Não grave a cena da
> ruptura com o pincel.**
>
> Para saber quais produtos estão em cada grupo hoje:
> `DB_USER=... DB_PASSWORD=... python ferramentas/banco/medir-substitutos.py`

| # | Passo | Esperado |
|---|---|---|
| 6.1 | Num item **de um par plantado**, tocar em *"não encontrei este produto"* | Substituto sugerido, com foto, corredor e explicação. |
| 6.1b | Repetir num produto **sem par** — o pincel `SKU-TIN-012` serve | Recusa com motivo, e não um substituto qualquer. **É o esperado.** |
| 6.2 | Tocar duas vezes rápido no botão | Uma requisição só. Há trava síncrona. |
| 6.3 | Aceitar o substituto | Numa ação: o substituto entra **não coletado** e o item que faltou sai. |
| 6.4 | Tocar em *Encerrar* com itens pendentes | Encerra assim mesmo, perguntando sobre os caixas. |

---

## 7. Acessibilidade e telas estreitas

| # | Passo | Esperado |
|---|---|---|
| 7.1 | Percorrer com Tab, sem mouse | Foco visível sempre. Nenhum controle inalcançável. |
| 7.2 | Abrir um modal e apertar Tab várias vezes | O foco **não sai** do modal. `Esc` fecha. |
| 7.3 | Fechar o modal | O foco volta para quem o abriu. |
| 7.4 | Largura de 375 px | Nada transborda. Nenhum retângulo branco atrás de barra. |

---

## Critérios de parada

**Não grave se:**

- a partida a frio não mostrar a faixa com cronômetro;
- o chip afirmar uma posição sem placa lida;
- o assistente citar produto que não existe no catálogo;
- algum cartão de produto estiver com texto sobreposto;
- o mapa não desenhar as gôndolas.

**Pode gravar mesmo com:**

- Materiais e Caixas sem gôndola nenhuma ([O-39](observacoes.md#o-39-materiais-de-construção-e-caixas-aparecem-sem-gôndola));
- a partida a frio de até três minutos — é limitação do plano gratuito, e o app avisa;
- produto sem foto ([O-18](observacoes.md));
- número de cartões do assistente variando entre execuções.

---

## Registro da execução

### 08/09/2026 — primeira execução, parcial *(superada pela execução completa acima)*

Contra o ambiente publicado, com o backend quente exceto onde indicado.

| Item | Resultado | Medido |
|---|---|---|
| 0.1 publicado é o atual | **passa** | Sete marcas de conteúdo presentes; `"até dois minutos"` ausente |
| 0.2 limpar armazenamento | — | Feito antes de cada seção |
| 0.3 backend responde | **reprovou uma vez** | Hibernou no meio do QA. Ver abaixo |
| 1.1–1.3 partida a frio | **não verificado** | O backend acordou durante a captura; a faixa foi verificada em bancada isolada, não em produção |
| 2.1 sem parâmetro | **passa** | *"Ainda não sabemos onde você está"* |
| 2.2 `?ponto=TIN-02` | **passa** | *"Você está em — Placa TIN02 — Corredor de Tintas"* |
| 2.3 `?ponto=ZZZ-99` | **passa** | *"Não encontramos a localização "ZZZ-99"…"* |
| 2.4 digitar `tin02` | **passa** | *"Posição atualizada para: Corredor de Tintas"* |
| 3.1 buscar `tinta` | **passa** | 3 resultados |
| 3.2 buscar `tintta` | **passa** | 3 resultados — tolera o erro de digitação |
| 3.3 buscar `xyzabc` | **passa** | *"Nenhum produto encontrado"*, sem tela de erro |
| 4.1–4.3 assistente | **passa** | 4 cartões; nomes completos; sem sobreposição |
| 4.4 pergunta fora de escopo | **não verificado** | Cota do Gemini |
| 5.2 mapa com itens | **passa** | 21 seções, 212 gôndolas, 2 contornos, alfinetes presentes |
| 6.x ruptura e encerramento | **não verificado** | — |
| 7.4 largura de 375 px | **passa** | Sem rolagem horizontal |

**Falso positivo descartado em 7.4.** Os chips de setor do mapa ultrapassam os 375 px, mas vivem
num carrossel com `overflow-x: auto` e a página não rola. É o comportamento pretendido.

**O que esta execução ensinou, e virou regra acima:** o backend hibernou no meio do QA e o item
2.2 foi dado como reprovado. Com o servidor frio, **tudo parece quebrado ao mesmo tempo** — e
esse padrão é a assinatura da hibernação, não de um defeito.

### 08/09/2026 — segunda execução, completa

Contra o ambiente publicado, em viewport de 390×844 e 375×812, com `manter-acordado.mjs` em
outro terminal. **Cobriu as 7 seções do roteiro e mais os caminhos que ele não previa** — as 5
telas, os 7 modais e os 23 manipuladores de `App.jsx`.

**Dois defeitos encontrados**, um deles na lista de "não grave se". Ambos detalhados depois da tabela.

| Item | Resultado | Medido |
|---|---|---|
| 0.1 publicado é o atual | **passa** | Marcas de conteúdo presentes. *Cuidado:* `"nenhum produto do mesmo tipo"` mora no **backend**, não no pacote JS — ausência ali é correta |
| 0.3 backend responde | **hibernado no início** | HTTP 000 em 25 s — o que permitiu testar a seção 1 de verdade |
| 1.1 faixa da partida a frio | **passa** | *"Preparando o sistema… o servidor hiberna quando ninguém está usando, e acordar leva alguns minutos"* — sem prazo fixo |
| 1.2 cronômetro anda | **passa** | 0:13 → 0:17. **Partida a frio: 204,9 s**, a maior já medida |
| 1.3 texto muda aos 3 min | **não observável** | Ver a armadilha registrada abaixo |
| 2.1 sem parâmetro | **reprovou, corrigido no mesmo dia** | Chip afirmava *"Você está em — Placa ENT01 — Entrada da loja"*. **Defeito 1**, corrigido e reverificado — ver abaixo |
| 2.2 `?ponto=TIN-02` | **passa** | *"Placa TIN02 — Corredor de Tintas"* |
| 2.3 `?ponto=ZZZ-99` | **passa** | Toast em **1,1 s**; chip diz *"Ainda não sabemos onde você está"* |
| 2.4 digitar `tin02` | **passa** | Vira `TIN02`; *"Posição atualizada para: Corredor de Tintas (Placa TIN02)"* |
| 2.5 recarregar | **passa** | Mesma sessão, posição preservada |
| 3.1 buscar `tinta` | **passa** | 3 resultados, com preço, corredor e estoque |
| 3.2 buscar `tintta` | **passa** | Mesmos 3 — tolera o erro de digitação |
| 3.3 buscar `xyzabc` | **passa** | *"Nenhum produto encontrado"* + oferta do assistente |
| 3.4 filtro que zera | **passa** | Facetas com contagem; chips removíveis; três saídas oferecidas |
| 3.5 abrir produto | **passa** | SKU, preço, estoque, corredor, especificações, dois botões |
| 4.1 assistente | **passa** | 4 cartões; nomes por extenso na resposta |
| 4.2 cartões | **passa** | `line-clamp: 2`; preço **nunca** sobrepõe as ações, a 375 px |
| 4.3 adicionar do cartão | **passa** | Toast cita o corredor |
| 4.4 fora de escopo | **passa** | Recusa em uma frase, oferece ajuda, **não gera cartões** |
| 5.1 mapa com lista vazia | **passa** | 0 alfinetes, 0 rota, mensagem explicando |
| 5.2 mapa com itens | **passa** | **235 polígonos** = 21 seções + 212 gôndolas + 2 contornos; viewBox `0 0 950 616` |
| 5.3 clique respeita o recorte | **passa** | O entalhe de Madeiras (L de 6 vértices) devolve **Ferragens**, não Madeiras |
| 5.4 zoom e centralizar | **passa** | 1 → 1,25 → 1,5 → 1,25 → reset; *centralizar em mim* desloca |
| 5.5 coletar | **passa** | Contador acompanha; **toque duplo = 1 coleta**, estável a 5 s |
| 6.1 ruptura com par plantado | **passa** | Trena 5 m → **Trena Stein 7,5 m**, com justificativa técnica |
| 6.1b ruptura sem par | **passa** | Recusa honesta, e avisa que o relato foi registrado |
| 6.2 toque duplo | **passa** | **1 requisição** — a trava síncrona funciona |
| 6.3 aceitar o substituto | **passa** | Troca em uma ação; substituto entra **não coletado**; lista não cresce |
| 6.4 encerrar com pendências | **passa** | Pergunta sobre os caixas, com as duas saídas |
| 7.1 foco visível | **passa, com ressalva** | Pelo **anel padrão do navegador**: só há 4 regras de foco na folha, todas `:focus-within` em campos de texto |
| 7.2 foco preso no modal | **REPROVA** | **Defeito 2** |
| 7.3 foco volta ao gatilho | **REPROVA** | **Defeito 2** |
| 7.4 375 px | **passa** | Zero vazamento em mapa, home, setores e busca |

#### Defeito 1 — o chip afirma a entrada sem placa lida — **CORRIGIDO em 08/09**

> [!NOTE]
> **Corrigido e reverificado contra o ambiente publicado**, com o pacote novo servido pela
> Vercel (`index-B-1eECw8.js`).
>
> | caminho | depois da correção |
> |---|---|
> | sem parâmetro | *"Ainda não sabemos onde você está"*, e `posicaoAtual` nula no servidor |
> | `?ponto=TIN-02` | *"Placa TIN02 — Corredor de Tintas"* — sem regressão |
> | `?ponto=ZZZ-99` | *"Ainda não sabemos onde você está"* — sem regressão |
> | recarga sem parâmetro, com sessão ativa | mesma sessão, TIN02 preservado — **sem regressão**, que era o risco real da mudança |
>
> **Um ramo morto ganhou vida.** O toast *"Não sabemos onde você está. Escaneie uma placa para o
> mapa mostrar sua posição."* estava escrito em `App.jsx` desde 30/08 e **nunca havia sido
> alcançado**, porque a linha errada garantia que sempre houvesse posição. Capturado rodando
> pela primeira vez nesta reverificação.
>
> **A lição:** corrigir o estado inicial de uma tela não corrige o dado que chega depois — e uma
> verificação feita nos primeiros instantes da carga aprova as duas. Ver
> [O-19](observacoes.md#o-19-o-plano-b-funciona-falta-a-placa-que-aponta-para-ele).

**Estava na lista de "não grave se".** Primeira visita, sem `?ponto=` e sem sessão guardada: o chip
diz *"Você está em — Placa ENT01 — Entrada da loja"*. Reproduzido três vezes, sempre.

**A causa é uma linha do frontend**, não do backend: `sessionService.js` chama
`inicializarSessao('ENT-01')` quando não há código. Verificado que o backend faz o certo — `POST
/sessoes` com corpo vazio devolve `posicaoAtual: null`, como a
[D-54](decisoes-tecnicas.md#d-54-a-entrada-aceita-o-código-da-placa-num-campo-só-e-código-desconhecido-não-recusa-a-sessão) prevê.

**O caminho honesto existe e funciona:** com código **inválido** (`ZZZ-99`) o chip diz *"Ainda não
sabemos onde você está"*. Só o caminho **sem parâmetro nenhum** mente — que é exatamente o estado
de quem abre o link pela primeira vez, a banca inclusive.

A [O-19](observacoes.md#o-19-o-plano-b-funciona-falta-a-placa-que-aponta-para-ele) registrou esta
correção em 30/08, mas ela foi feita apenas no estado inicial do chip em `App.jsx`; o serviço
continuou com o valor fixo.

#### Defeito 2 — não há armadilha de foco nem restauração

`Esc` fecha os modais, e `role="dialog"` com `aria-modal="true"` estão corretos. Mas:

- com um modal aberto, **40 elementos do fundo continuam alcançáveis por Tab** — nenhum está
  `inert` nem `aria-hidden`;
- ao fechar, o foco vai para o `body` e **não volta para quem abriu**.

Confirmado no código: não existe armadilha de foco nem restauração em componente nenhum — apenas
`input.focus()` para focar o campo ao abrir.

**Não é regressão.** O bloco de acessibilidade fechado em 30/08 cobriu contraste, movimento
reduzido e barreira de erro; gestão de foco nunca esteve nele.

#### Armadilhas desta execução, para a próxima não repetir

> [!CAUTION]
> **Não ligue o `manter-acordado.mjs` antes de testar a seção 1.** Ele dispara o despertar, e o
> navegador entra numa partida **já em curso**: mediu 136 s quando a partida real foi de 204,9 s,
> e a faixa limpou antes dos 3 minutos, tornando o **item 1.3 inobservável**. Para testar a
> seção 1, o navegador tem de ser quem acorda o servidor.

> [!CAUTION]
> **Foto de produto não é verificável de dentro de um navegador embutido.** As três imagens do
> catálogo apareceram como falha (`naturalWidth` 0, `complete: false`). **Não é defeito:** o CDN
> serve normalmente por `curl` (HTTP 200, 32 KB, inclusive com o Referer do app), imagem de mesma
> origem carrega, e **nenhuma requisição chega a ser disparada** — o painel bloqueia imagem de
> terceiro. **Confira as fotos num navegador de verdade.**

> [!CAUTION]
> **Nada que dependa de `document.visibilityState` é verificável com o painel do navegador
> recolhido.** A reconciliação de duas abas desiste de propósito quando o documento está
> oculto (`App.jsx:321`), e um painel recolhido reporta `visibilityState: "hidden"`,
> `hasFocus: false` e largura zero. Disparar `visibilitychange` à mão **não contorna** — o
> guarda lê o estado, não o evento.
>
> Custou um falso negativo em 08/09: o item adicionado pelo servidor não apareceu na lista, e
> por um instante pareceu que a reconciliação tinha quebrado. **Verifique duas abas num
> navegador de verdade**, alternando entre elas.

> [!NOTE]
> **O `localStorage` fica temporariamente atrás do servidor** durante a sessão — chegou a mostrar
> zero coletados enquanto o servidor tinha um. **Sincroniza na recarga**, e o mapa sempre leu o
> estado certo. Não é defeito, mas atrapalha quem for depurar pelo armazenamento.

#### Achados fora do roteiro, que importam para a gravação

| O quê | Por que importa |
|---|---|
| **Coletar um item move a posição do cliente** para a seção daquele item ([D-64](decisoes-tecnicas.md)) | O chip muda sozinho durante a demonstração. É desenho, não defeito — mas surpreende quem narra |
| **A página de setores diz "10 seções físicas"; o mapa desenha 21** | Os dois estão certos no próprio contexto — 10 departamentos com produto, 21 áreas traçadas. **Não cite um número sem dizer de qual se trata** |
| **O filtro de seção persiste na busca** | Buscar depois de navegar por uma seção devolve *"nenhum resultado… em Tintas"*. A tela explica e oferece saída, mas na câmera parece falha |
| **Encerrar sem coletar mostra "TOTAL ESTIMADO R$ 0,00"** | Correto — só conta o que foi coletado. Na câmera parece defeito: **colete ao menos um item antes de mostrar essa tela** |
| **Item coletado perde o botão de ruptura** | Coerente. Para gravar a cena da prateleira vazia, o item **não** pode estar marcado |
| **Produto sem estoque mostra `block` no lugar de adicionar**, com o selo *"Sem Estoque (Ruptura)"* | Bom de mostrar, e explica por que o botão muda |

#### Verificações que confirmam correções anteriores

- **[O-34](observacoes.md#o-34-a-aba-do-qr-code-manda-apontar-uma-câmera-que-não-existe--corrigida)** —
  a aba do QR diz *"Use a câmera do seu celular na placa do corredor — ela abre o app já na sua
  posição"*, a lista se assume como *"simule a leitura de uma placa física"*, e não há
  coordenadas cruas na tela.
- **Fundo branco atrás da barra de busca** — `.search-page-top-bar` tem exatamente o fundo da
  página (`rgb(250,250,248)`).
- **Contagem do catálogo** — os chips de seção somam 111 produtos, batendo com o total exibido.
- **Total do roteiro** — R$ 14,90 + 24,90 + 39,90 = R$ 79,70, conferido na tela.

### Próximas execuções

| Data | Quem | Resultado | Observações |
|---|---|---|---|
| | | | |
