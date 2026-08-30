# Fluxo do cliente — escopo revisado após a mentoria de 24/08/2026

> **Este documento substitui a jornada descrita em [`contexto-e-planejamento.md`](contexto-e-planejamento.md).** Ele nasceu de duas orientações dadas pelos representantes da Leroy Merlin na mentoria, que mudaram a raiz do produto.
>
> Serve a três leitores: o **backend**, para saber o que precisa existir; a **dupla de frontend**, para saber que telas construir; e a **banca**, para entender por que o produto é assim.

---

## O que mudou, e por quê

**1. Não existe mais totem.** Tudo acontece no celular do cliente, numa página web — sem instalar aplicativo. O cliente entra escaneando um **QR Code afixado na loja**, e cada QR carrega a posição onde está colado. É assim que o sistema sabe onde o cliente começou.

**2. Não existe mais rota calculada.** Perguntamos se otimizar o percurso não trabalharia contra a venda por impulso, já que a loja é organizada para o cliente passar por coisas que não veio buscar. A resposta foi que a Leroy já tem um circuito que o cliente percorre, e a orientação foi: **mostrar no mapa apenas a posição do cliente e a de cada produto da lista, e deixar o próprio cliente decidir o caminho.**

**Isso aproxima o projeto do desafio, não afasta.** O enunciado da FIAP nomeia o "problema do último metro" — achar o produto exato num layout complexo. Mostrar onde o cliente está e onde está cada item **é** a resposta a isso. A rota otimizada era acréscimo nosso.

E o nome do projeto sobrevive com outra leitura: quem encontra a rota agora é o cliente.

---

## O mapa

Toda a experiência gira em torno de uma tela de mapa, então vale definir de onde ela vem.

O **backend serve a geometria da loja**: uma lista de blocos, cada um com posição, dimensões e rótulo, no mesmo grid 0–100 em que vivem os produtos e os QR Codes. O frontend desenha retângulos e textos a partir dessa lista.

Duas consequências que fazem essa escolha valer:

- ninguém precisa desenhar uma planta em SVG à mão;
- **o mapa não pode divergir dos produtos**, porque os dois saem da mesma fonte de dados.

Sobre o mapa, o frontend desenha três camadas: os blocos da loja, o **marcador do cliente** e um marcador para **cada item da lista** — com aparência diferente para o que já foi coletado.

---

## Duas fases, e o trânsito entre elas

A jornada tem duas fases com naturezas diferentes: **escolher** o que buscar e **caminhar** para achar. Sem o totem, não existe mais um momento que separe uma da outra — o handoff fazia esse papel.

A decisão é que **não há transição**: as duas ficam disponíveis o tempo todo, e o cliente alterna quando quiser. A navegação é permanente, com quatro destinos:

| Destino | Para quê |
|---|---|
| **Catálogo** | procurar e escolher produtos |
| **Lista** | ver e ajustar o que já escolheu |
| **Mapa** | ver onde está e onde estão os itens |
| **Assistente** | perguntar em linguagem natural |

Isso importa porque o cliente real vai e volta: escolhe três itens, caminha, lembra de mais um, escolhe, volta ao mapa. Um fluxo em linha reta obrigaria a "terminar de escolher" antes de andar, o que não é como se compra numa loja.

---

## O fluxo, ação por ação

Cada passo traz o que o cliente faz, o que a tela mostra e o que o backend precisa ter.

> **Sobre o 🆕.** Ele marca o que não existia **quando este documento foi escrito**, em 24/08/2026 — serve para mostrar o tamanho da virada, não o estado de hoje. Boa parte já foi construída desde então. **Para saber o que está pronto, o histórico do repositório é a fonte**; o [`backlog-escopo-revisado.md`](backlog-escopo-revisado.md) diz o que falta.

---

### 1. Entrada pela placa de localização 🆕

O cliente entra por uma placa afixada num corredor de passagem ou num cruzamento. A placa traz **três coisas**, e cada uma existe por um motivo:

| Na placa | Para quê |
|---|---|
| **QR Code** | o caminho normal — escaneou, abriu, pronto |
| **URL curta e legível** | quando escanear não dá: câmera ruim, leitor que não abre, permissão negada |
| **Código de localização** (`TIN-02`) | diz ao sistema **em qual placa** o cliente está, quando ele chegou pela URL |

**Plano A — escanear.** O QR codifica a URL já com o código do ponto. O celular abre a página sabendo de onde o cliente partiu, sem ele digitar nada.

**Plano B — digitar.** O cliente digita a URL da placa, e a página pergunta o código de localização. O código está ali ao lado, na mesma placa. O resultado é idêntico ao plano A.

**Por que o plano B precisou da URL na placa.** O código curto sozinho não resolvia nada: se o único caminho para o sistema fosse o QR, quem não conseguisse escanear não teria onde digitar o código. A URL impressa é o que torna o código alcançável.

**Tela:** carregamento breve e a home do catálogo, com um "você está em: *Corredor central, próximo a Tintas*". No plano B, antes disso, uma tela pedindo o código.

**Backend:** `POST /sessoes` passa a receber **o código do ponto**, num campo só — os dois planos chegam pelo mesmo caminho. A sessão nasce sabendo a posição inicial.

**Se o código não existir** — placa velha, loja remanejada, erro de digitação — a sessão nasce **assim mesmo, sem posição**, e continua utilizável. Melhor um mapa sem "você está aqui" do que nenhum sistema.

> **Em aberto:** quantos QR Codes e onde exatamente, e a arte da placa. A definição é do time, e a massa de demonstração acompanha.

---

### 2. Retomada da sessão 🆕 *(frontend)*

Se o cliente sai da página e volta, a sessão é retomada com a lista intacta. O identificador fica no `localStorage` do navegador, que sobrevive a fechar a aba e até o navegador.

**Só se perde** se o cliente limpar os dados do site ou trocar de aparelho — e aí ele escaneia um QR de novo e começa do zero.

**Backend:** nada novo. `GET /sessoes/{id}` já responde se a sessão continua válida.

> **Cuidado para o frontend:** antes de reusar a sessão guardada, confirmar que ela ainda está ativa. Uma sessão expirada precisa levar a um recomeço limpo, não a uma tela com erro.

---

### 3. Home: o catálogo

O cliente vê os produtos como veria numa loja online — mas com um propósito diferente: **escolher o que vai buscar dentro desta loja, agora.**

**Tela:** vitrine com cartão por produto — imagem, nome, preço e um sinal de disponibilidade. Busca no topo e filtros ao lado.

**Backend:** `GET /produtos` já lista paginado. **Falta a imagem e a descrição** nos dados.

---

### 4. Busca por nome

O cliente digita o que procura, inclusive errado. "tnta" acha "Tinta"; "furadera" acha "Furadeira".

**Tela:** resultados atualizando conforme digita.

**Backend:** pronto. É a busca tolerante a erro de digitação que já existe, feita com `UTL_MATCH` do Oracle.

---

### 5. Filtros 🆕

**Tela:** filtrar por **seção** — Tintas, Elétrica, Jardim —, alternar "mostrar só o que está disponível" e escolher **características**: marca, bitola, amperagem, grão, potência.

**Backend:** `GET /produtos` aceita `secao`, `apenasDisponiveis` e `atributo=CHAVE:valor` (repetível), tudo combinável com o termo de busca. `GET /produtos/secoes` alimenta o menu de corredores com a contagem de cada um.

**Os filtros de característica são dinâmicos**, e essa é a parte que mais importa para a tela. A resposta traz as **facetas** do recorte atual: quem navega em Tintas recebe *Grão*, quem navega em Elétrica recebe *Amperagem*, e ninguém recebe os dois. Cada valor vem com a contagem, do mais comum para o menos comum.

Cada faceta traz o próprio `rotulo`, então o frontend **não precisa manter tradução nenhuma** — característica nova aparece na tela sozinha.

**Semântica:** valores da mesma chave são "ou", chaves diferentes são "e". Marcar duas marcas mostra as duas; acrescentar uma bitola restringe às que atendem as duas coisas.

**Cuidado:** as facetas ignoram as características já escolhidas de propósito, para o cliente conseguir trocar de marca sem limpar o filtro antes. Se a tela esconder as opções não selecionadas, ela desfaz isso.

---

### 6. Detalhe do produto

O cliente toca num produto e vê a página dele.

**Tela:** imagem, nome, preço, descrição, **tabela de especificações** — marca, material, medidas — , disponibilidade e, o que diferencia de um e-commerce, **em que corredor ele está**, com um botão para ver no mapa.

**Backend:** `GET /produtos/{id}` traz tudo isso, incluindo `atributos` com o rótulo de cada característica pronto para exibir.

> **Só o detalhe traz as especificações.** Carregá-las em cada item da listagem custaria uma consulta por produto, e a aplicação está a 5.000 km do banco.

> **Decidido:** sem avaliações. Nota inventada numa demonstração é fácil de perceber e custa credibilidade.

---

### 7. Conversa com o assistente

O cliente pergunta em linguagem natural: *"o que eu preciso para pintar uma parede?"*. O assistente responde citando **produtos que existem nesta loja**, com o corredor de cada um.

**Tela:** chat, com os produtos citados aparecendo como cartões clicáveis que levam ao detalhe ou adicionam à lista direto.

**Backend:** pronto e publicado. O assistente consulta o catálogo por *function calling* e não consegue citar produto que a busca não devolveu.

> **Ganho do novo escopo:** antes o assistente vivia no totem, e o cliente precisava estar parado nele. Agora ele acompanha o cliente pela loja — inclusive na frente da prateleira.

---

### 8. Adicionar à lista

De qualquer lugar: da vitrine, da busca, do detalhe ou de uma sugestão do assistente.

**Tela:** confirmação discreta e o contador da lista subindo.

**Backend:** pronto. **Sem limite de itens** — decisão tomada pensando no cliente profissional que monta a lista de uma obra inteira. Produto repetido não vira item novo.

> **Um produto entra uma vez só, sem quantidade.** Ver o passo 9 para o motivo.

---

### 9. Ver e ajustar a lista

**Tela:** os itens escolhidos, cada um com foto, nome, corredor e um botão de remover. No rodapé, **quantos itens** e o **valor total estimado**.

**Backend:** pronto — consultar e remover já existem, e o preço de cada item vem junto. O total pode ser somado na tela.

> **Sem quantidade, e por um motivo de produto.** O sistema existe para dizer **onde** achar as coisas. Duas trenas iguais estão no mesmo lugar: o cliente chega lá e pega duas. Quantidade só faria sentido se mudasse o destino, e não muda — acrescentaria campo, tela e complexidade sem mudar uma única decisão do cliente dentro da loja.

---

### 10. O mapa 🆕

A tela central do produto.

**Tela:** a planta da loja com o marcador do cliente e um marcador por item da lista. Tocar num marcador mostra qual produto é. Item já coletado aparece diferente.

**Backend:** `GET /mapa` devolve a planta pronta para desenhar — os corredores como retângulos com rótulo, e os pontos que não são prateleira (caixas, banheiro e as placas de QR), cada um com seu tipo.

Tudo no mesmo grid `0..100` das coordenadas dos produtos e da posição do cliente, então as três camadas se desenham na mesma escala sem conversão nenhuma.

**Não depende de sessão**, de propósito: o mapa é igual para todo cliente, e o frontend pode buscá-lo uma vez e guardar no aparelho — inclusive para sobreviver a uma queda de conexão dentro da loja.

**A garantia que vale conhecer:** as coordenadas das seções são **derivadas** dos blocos, e não escritas ao lado deles. Um produto não tem como aparecer fora do próprio corredor, porque o ponto da seção **é** o centro do bloco.

---

### 11. Marcar item como coletado

O cliente pega o produto da prateleira e marca.

**Tela:** o item ganha um risco na lista e o marcador muda no mapa. A posição do cliente passa a ser a daquele corredor.

E pode **desmarcar** 🆕, se tiver tocado por engano — algo comum num celular, andando.

**Backend:** `PATCH /roteiro/itens/{id}/coletar` e `PATCH /roteiro/itens/{id}/desmarcar`, os dois idempotentes.

> **A posição volta sozinha.** Ela não é gravada em lugar nenhum — é deduzida do item coletado mais recente comparado com a placa lida. Desmarcar só apaga o instante da coleta, e a dedução encontra o item anterior, ou a placa, sem nenhuma lógica de reversão.
>
> Inclusive no caso torto: se o cliente pegou a tinta, se perdeu, leu a placa do cruzamento e depois pegou algo em Jardim, desfazer a coleta de Jardim o deixa **no cruzamento** — e não de volta em Tintas.

---

### 12. Prateleira vazia

O cliente chega ao corredor e o produto não está lá.

**Tela:** um botão no item — *"não encontrei este produto"*. Em seguida, o substituto sugerido, com foto, corredor e uma explicação em linguagem natural.

**Backend:** pronto e publicado. O sistema filtra no banco o que está disponível fisicamente perto, e o assistente elege entre esses candidatos o que cumpre a mesma função. **Nunca sugere produto inventado**, porque o código só aceita um código que estava entre os candidatos.

Se o assistente estiver fora do ar, cai para o disponível mais próximo — e a resposta traz um campo dizendo qual dos dois foi, para a tela não chamar de recomendação inteligente o que foi só proximidade.

**Aceitar é uma ação só** 🆕 — `POST /roteiro/itens/{id}/substituir`. O cliente toca em "levar este" e, de uma vez: o substituto entra na lista e o produto que faltou sai. Fazer isso em duas ações — adicionar um, remover o outro — seria trabalho de sistema jogado no colo de quem está em pé no corredor.

O substituto entra **não coletado**, porque nem sempre está na mesma prateleira: pode estar alguns metros adiante, e o mapa é que vai dizer onde.

**O produto vai no corpo da requisição**, e não é deduzido da sugestão: o assistente pode responder diferente numa segunda chamada, e a troca precisa valer sobre o que o cliente **viu na tela**. De quebra, ele não fica preso à sugestão — se achou outra coisa na prateleira que resolve, pode trocar por ela.

**O registro da ruptura permanece.** Ele é evidência do que aconteceu na gôndola, e vale tenha o cliente aceitado a troca ou não — comparar as duas coisas é o que diz à loja se as sugestões estão boas.

> **Também é o que fecha o ciclo do produto.** A promessa é converter uma ruptura em venda; enquanto aceitar dá trabalho, a conversão não acontece.

---

### 13. Recentrar a posição

Se o cliente se perder, ele lê qualquer placa da loja — escaneando ou digitando o código, como na entrada — e o mapa se recentra.

**Tela:** a mesma da entrada, mas sem criar sessão nova. **A lista e o que já foi coletado permanecem intactos:** ele se perdeu, não recomeçou.

**Backend:** `PUT /sessoes/{sessaoId}/posicao` com o código da placa. A placa nova passa a valer sobre o último item coletado, por ser a evidência mais recente.

**Aqui código desconhecido é erro**, ao contrário da entrada — o cliente já tem sessão funcionando, e avisar que a placa não foi encontrada é acionável. A posição anterior continua valendo.

---

### 14. Fim da jornada 🆕

Há **dois caminhos** para terminar, e o segundo é tão comum quanto o primeiro.

**Coletou tudo.** Ao marcar o último item da lista, o sistema pergunta se o cliente quer ajuda para encontrar os caixas.

**Desistiu do que faltou** 🆕. O cliente não achou dois itens, ou mudou de ideia, e quer ir embora com o que tem. Um botão **"encerrar"**, disponível a qualquer momento, faz a mesma pergunta.

Esse segundo caminho não é detalhe: **o sistema tem um fluxo inteiro dedicado a produtos que não estão na prateleira**. Seria incoerente exigir que o cliente colete tudo para conseguir terminar — quem não achou um item ficaria preso numa jornada que nunca fecha, e a sessão morreria como abandonada mesmo tendo dado certo.

**De qualquer forma a jornada é concluída.** Muda só o que aparece:

- **Quer ajuda:** o caixa é marcado no mapa, com uma mensagem de agradecimento.
- **Não quer:** só a mensagem de agradecimento.

**Backend:** concluir a sessão já existe; falta **sinalizar que o último item foi coletado** e devolver a posição do caixa. Os pontos de caixa já estão no mapa.

> **Por que concluir importa:** é o que separa "jornada completa" de "carrinho abandonado" na varredura de sessões. Sem isso, toda sessão terminaria como abandonada e a métrica perderia o sentido.

---

## Resumo das telas, para o frontend

> **Atualizado em 30/08/2026.** Esta tabela pedia oito telas quando não existia nenhuma. **As oito existem.** O que a coluna de estado registra agora é o que foi verificado no navegador, contra o backend real.

| # | Tela | Estado |
|---|---|---|
| 1 | Entrada pelo QR / carregamento | **feita** — com plano B: digitar o código, e com exemplo do formato |
| 2 | Home do catálogo, com busca e filtros | **feita** — facetas dinâmicas incluídas. Falta **paginação**: 61 dos 111 produtos não são alcançáveis na visão "Todos" |
| 3 | Detalhe do produto | **feita** |
| 4 | Chat com o assistente | **feita** — e desde 30/08 ela admite quando não consegue perguntar, em vez de inventar a resposta |
| 5 | Lista de compras | **feita** — *local-first*, com coleta funcionando desde 28/08 |
| 6 | **Mapa** | **feita** — e desde 28/08 deixa de apontar corredor errado quando não sabe onde o produto está |
| 7 | Prateleira vazia e substituto | **feita** em 28/08 — era a última funcionalidade sem porta de entrada |
| 8 | Encerramento | **feita** — reconhece o encerramento com itens pendentes sem cobrar |

Mais a **navegação permanente** entre catálogo, lista, mapa e assistente, presente em todas elas — **feita**.

**O que falta na tela** não é tela nova: são nove acabamentos, listados em [`quebras-de-fluxo.md`](quebras-de-fluxo.md), e o catálogo inventado que precisa sair antes de qualquer aviso de indisponibilidade fazer sentido.

Nenhuma tela de totem, nenhuma tela de leitura de QR de transferência, nenhuma tela de rota.

---

## Resumo do backend

### Some

| O quê | Porque |
|---|---|
| Handoff inteiro — token JWT, uso único, regeneração de QR | não há mais transição entre dispositivos |
| Algoritmo de rota — vizinho mais próximo e refinamento 2-opt | o cliente decide o caminho |
| Ordem de caminho nos itens | idem |
| Ponto de interesse no meio da rota | sem rota, não há onde inserir desvio |

### Nasce

| O quê |
|---|
| Pontos de QR Code, com posição, e a sessão sabendo onde começou |
| Atualizar a posição do cliente |
| Geometria da loja para desenhar o mapa |
| Listar seções e filtrar produtos por seção e disponibilidade |
| Descrição e imagem nos produtos |
| Sinalizar que o último item foi coletado, e devolver a posição do caixa |
| Desmarcar item coletado |
| Aceitar o substituto numa ação só |

### Continua igual

Busca tolerante a erro de digitação, detalhe de produto, lista de compras sem limite, **assistente de IA**, **ruptura com substituto**, marcação de coletado, ciclo de vida e expiração de sessão, e toda a infraestrutura já publicada — Oracle, Gemini, deploy e a suíte de testes.

---

## Pontos que ainda precisam de decisão

| # | Assunto | Situação |
|---|---|---|
| 1 | Quantos QR Codes e onde | **em aberto** — definição do time, a massa acompanha |
| 2 | Renomear "roteiro" para "lista de compras" | **adiado** — ver abaixo |
| 3 | Quantidade por item | **decidido: não haverá** |
| 4 | Desmarcar item coletado | **decidido: entra** |
| 5 | Aceitar o substituto numa ação só | **decidido: entra** |
| 6 | Imagens dos produtos | **decidido: URLs públicas da Leroy**, coletadas manualmente pelo time a partir da lista de produtos da massa |
| 7 | Avaliações de produto | **decidido: fora** — nota inventada custa credibilidade |

**O item 2 merece explicação** — e ele venceu por circunstância: `ListaRoteiro`, `ItemRoteiro` e as rotas `/roteiro/...` foram batizados quando existia uma rota calculada. Sem ela, "roteiro" descreve algo que o sistema não faz mais. Renomear deixa o código honesto, mas atinge entidades, endpoints, contrato e testes — e o contrato é o que a dupla de frontend consome. **Se for feito, tem que ser antes de a integração começar.**

> [!NOTE]
> **Atualizado em 30/08/2026: o prazo passou.** A integração começou em 27/08 e está concluída — o frontend inteiro consome `/roteiro/...`. Renomear agora atinge entidades, endpoints, contrato, testes **e a tela**, a duas semanas do vídeo, sem mudar nada que o cliente veja.
>
> **A recomendação passa a ser não renomear**, e explicar na banca: o nome guarda a história do produto, e a história — a mentoria ter derrubado a rota calculada — é um dos pontos mais fortes que temos para contar.
