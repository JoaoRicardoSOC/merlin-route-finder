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
> **Materiais de construção e a frente de caixas** aparecem com gôndolas geradas por fórmula —
> cinco barras iguais. É [decisão registrada](decisoes-tecnicas.md), não defeito. Não vale a pena
> dar close nelas no vídeo.

---

## 6. Ruptura e encerramento

| # | Passo | Esperado |
|---|---|---|
| 6.1 | Num item, tocar em *"não encontrei este produto"* | Substituto sugerido, com foto, corredor e explicação. |
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

- Materiais e Caixas com gôndolas por fórmula ([O-39](observacoes.md));
- a partida a frio de até três minutos — é limitação do plano gratuito, e o app avisa;
- produto sem foto ([O-18](observacoes.md));
- número de cartões do assistente variando entre execuções.

---

## Registro da execução

### 08/09/2026 — primeira execução, parcial

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

### Próximas execuções

| Data | Quem | Resultado | Observações |
|---|---|---|---|
| | | | |
