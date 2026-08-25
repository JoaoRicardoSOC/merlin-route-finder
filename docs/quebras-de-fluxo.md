# Quebras de fluxo — o que pode dar errado, e o que o cliente vê

> Complementa [`fluxo-do-cliente.md`](fluxo-do-cliente.md), que descreve o caminho quando tudo dá certo. Este documento é o outro lado: **tudo que pode interromper esse caminho**, e o que o sistema faz em cada caso.
>
> Escrito depois do escopo revisado de 24/08/2026 — celular único, sem totem e sem rota calculada.
>
> Cada quebra traz: **o que acontece**, **o que o cliente vê** e **de quem é a solução**.
>
> **Auditado contra o código em 25/08/2026**, depois de os treze cards do escopo revisado ficarem prontos. Cada cenário abre com uma linha dizendo de quem ele é:
>
> | | |
> |---|---|
> | ✅ | **resolvido no backend** — com a decisão e o teste que provam |
> | 🎨 | **é do frontend** — o backend já entrega o que a tela precisa |
> | 🚫 | **não pode acontecer** — o esquema ou um teste impedem |
> | 💬 | **decidido** — escolhemos não tratar, e o motivo está dito |

---

## As duas que mudam decisões já tomadas

Antes da lista, duas quebras que o novo escopo criou e que não existiam quando o totem existia.

### ✅ A sessão morre em 30 minutos parada, e agora ela é a única coisa que o cliente tem

> **Backend — resolvido.** TTL de 4 horas ([D-24](decisoes-tecnicas.md#d-24-ttl-da-sessão-é-renovado-a-cada-interação)), sob teste em `SessaoTest`.

Antes, a sessão vivia no totem e o cliente levava a rota no celular. Se a sessão expirasse, ele ainda tinha a lista na tela.

**Agora a sessão é tudo.** Ela guarda a lista inteira, e o TTL de 30 minutos de inatividade foi dimensionado para um totem — onde o objetivo era **liberar o equipamento** para o próximo cliente.

Esse motivo desapareceu. Não há equipamento para liberar: o aparelho é do cliente.

E o cenário é banal — o cliente monta quinze itens, recebe uma ligação, conversa quarenta minutos, volta e **perdeu tudo**. Nada no sistema avisa; ele simplesmente encontra uma sessão inválida.

> **Resolvido em 25/08/2026: o TTL passou para 4 horas.** É mais tempo do que qualquer compra realista leva, e a varredura de sessões vencidas continua limpando o banco — só que a cada 30 minutos em vez de 5, porque com o prazo novo varrer de cinco em cinco seriam 48 consultas para cada uma que encontra algo. Ver [D-24](decisoes-tecnicas.md#d-24-ttl-da-sessão-é-renovado-a-cada-interação).

### 🎨 Loja de 10.000 m², paredes de concreto, e um produto que só funciona online

> **Frontend.** Guardar lista e mapa no aparelho, avisar quando cair, enfileirar as marcações. O backend não muda — `GET /mapa` já é livre de sessão justamente para poder ser guardado.

Toda ação do cliente é uma chamada de rede: ver o mapa, marcar item, perguntar ao assistente. **No fundo de um corredor de materiais de construção, o sinal cai.**

Hoje o cliente veria erro em tudo, sem entender por quê — e a lista dele está no servidor, não no bolso.

Saídas possíveis, em ordem de custo:

1. **Avisar bem.** Detectar que está offline e mostrar "sem conexão, tentando de novo" em vez de erro cru. Barato, e evita a pior impressão.
2. **Guardar a lista e o mapa no aparelho** e mostrá-los mesmo offline. O cliente continua vendo para onde ir, que é o essencial.
3. **Enfileirar as marcações** feitas offline e enviá-las quando o sinal voltar.

As três são trabalho de frontend. **A 1 eu trataria como obrigatória**; a 2 é o que separa um protótipo de algo que funcionaria numa loja de verdade — e é um ótimo argumento para a banca, mesmo que só descrito.

---

## Entrada e sessão

### ✅ O QR Code está rasgado, sujo ou riscado

> **Backend — resolvido.** Código curto digitável em cada placa ([D-52](decisoes-tecnicas.md#d-52-o-código-curto-do-qr-code-é-normalizado-na-gravação-não-só-na-busca)), aceito em qualquer grafia. **Falta a tela de digitação** ([O-19](observacoes.md#o-19-a-entrada-tem-um-plano-b-e-ele-é-uma-tela-que-ainda-não-existe)) e a URL impressa na placa.

Adesivo em corredor de loja não sobrevive muito tempo. Sem conseguir ler, o cliente não entra.

**O que o cliente vê:** nada — ele desiste e vai embora, que é o pior desfecho possível.

**Saída:** imprimir na placa, junto do QR, uma **URL curta legível** e um **código de localização digitável** — algo como `TIN-02`.

A URL é a parte que costuma ser esquecida, e sem ela o resto não funciona: o código sozinho não resolve nada se o único caminho até o sistema for o próprio QR. Quem não consegue escanear precisa de um lugar onde digitar.

Isso também resolve o cliente cuja câmera não funciona ou que não sabe usar leitor de QR.

### ✅ O QR aponta para um ponto que não existe mais

> **Backend — resolvido.** A sessão nasce **sem posição** em vez de recusar ([D-54](decisoes-tecnicas.md#d-54-a-entrada-aceita-o-código-da-placa-num-campo-só-e-código-desconhecido-não-recusa-a-sessão)), sob teste em `SessaoComPosicaoIntegracaoTest`. **A tela precisa perceber `posicaoAtual` nula e avisar**, porque não vem erro HTTP.

A loja remanejou uma seção, o ponto saiu do banco, mas o adesivo continua na parede.

**O que o cliente vê:** hoje, `404` cru.

**Deveria ver:** "não reconhecemos este ponto — procure outro QR Code por perto". E a sessão deveria começar mesmo assim, **sem posição inicial**, em vez de não começar. Melhor um mapa sem o "você está aqui" do que nenhum sistema.

### 🎨 A sessão guardada no aparelho já expirou

> **Frontend.** `GET /sessoes/{id}` já responde o status antes de qualquer escrita. Ver [O-06](observacoes.md#o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar).

**O que o cliente vê:** hoje, erro na primeira ação.

**Deveria ver:** ao abrir, o app verifica a sessão. Se estiver morta, mostra "sua sessão anterior expirou" e começa uma nova — limpa, sem tela de erro.

O backend já responde se a sessão vale; falta o frontend perguntar **antes** de tentar usar.

### ✅ O cliente volta a uma sessão já concluída

> **Backend — resolvido.** Leitura sim, escrita 409 ([D-41](decisoes-tecnicas.md#d-41-sessão-encerrada-continua-legível-mas-não-gravável)). **A tela trata o 409 como "jornada encerrada"**, não como falha.

Ele encerrou, agradeceu, e abre a página de novo pelo aparelho.

**O que acontece:** leitura funciona, escrita não. Ele consegue ver a lista e o histórico do que fez, mas não adicionar nem marcar nada — recebe `409`.

Está correto e já implementado ([D-41](decisoes-tecnicas.md#d-41-sessão-encerrada-continua-legível-mas-não-gravável)). **O frontend precisa tratar esse 409 como "jornada encerrada, quer começar outra?"**, e não como falha.

### 🎨 Duas abas abertas na mesma sessão

> **Frontend.** O banco está seguro e medido ([D-48](decisoes-tecnicas.md#d-48-gravar-pelo-agregado-é-seguro-aqui--e-a-investigação-que-provou-isso-depois-de-duas-hipóteses-erradas)); o que falta é recarregar a lista ao voltar para a aba.

O cliente abre o link duas vezes sem perceber.

**O que acontece no banco:** nada de ruim. As escritas não se atropelam — está medido e sob teste ([D-48](decisoes-tecnicas.md#d-48-gravar-pelo-agregado-é-seguro-aqui--e-a-investigação-que-provou-isso-depois-de-duas-hipóteses-erradas)).

**O que o cliente vê:** duas telas discordando. Ele marca um item numa aba, volta para a outra e o item ainda aparece pendente.

**Solução:** recarregar a lista ao voltar para a aba. É frontend.

---

## Catálogo e busca

### 🎨 A busca não acha nada

> **Frontend.** O backend devolve página vazia, como deve. A oportunidade é da tela: transformar o beco sem saída na porta de entrada do assistente.

Mesmo com a tolerância a erro de digitação, o cliente pode procurar algo que a loja não tem.

**Deveria ver:** "não encontramos *pergolado*" e, junto, um caminho de saída — **perguntar ao assistente**. Ele entende pedido aberto e pode sugerir o que serve para aquele projeto.

Transformar o beco sem saída na porta de entrada do recurso mais forte do sistema.

### 🎨 O filtro devolve lista vazia

> **Frontend — e com uma ajuda que ele talvez não saiba que tem.** Junto da página vazia vêm as **facetas do recorte**: a tela pode mostrar exatamente quais filtros ainda têm resultado, em vez de só oferecer "limpar tudo" ([D-63](decisoes-tecnicas.md#d-63-as-facetas-ignoram-a-escolha-do-cliente-sobre-elas-mesmas)).

"Jardim" + "só disponíveis" pode não sobrar nada.

**Deveria ver:** aviso de que o filtro está restringindo, com um toque para limpá-lo. Nunca uma tela branca.

### ✅ O produto abriu, mas o id não existe

> **Backend — resolvido.** 404 com o JSON de erro padrão.

Link velho, produto retirado do catálogo.

Já devolve `404` limpo, com JSON no formato de sempre. **O frontend precisa mostrar "produto não disponível" em vez de repassar o erro.**

---

## Assistente

### ✅ A IA está fora do ar ou a cota estourou

> **Backend — resolvido.** Mensagem honesta, e a indisponibilidade **não entra no histórico**.

**O que o cliente vê:** *"Não consegui consultar o assistente agora. Você pode buscar o produto direto pela tela de busca, ou tentar novamente em instantes."*

Implementado. E a resposta de indisponibilidade **não entra no histórico** — o assistente não deve "lembrar" de ter estado fora do ar.

### 🎨 A resposta demora oito segundos

> **Frontend.** Indicar que o assistente está pensando e **travar o campo** — o limite por minuto da cota gratuita torna isso proteção, não só estética ([O-01](observacoes.md#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)).

Medido na instância publicada. Sem aviso, o cliente acha que travou e toca de novo.

**Deveria ver:** indicação de que o assistente está pensando, e o campo bloqueado enquanto isso. **Bloquear importa duas vezes**: evita a impressão de travamento e evita gastar duas chamadas da cota gratuita, que é de cinco por minuto.

### ✅ O cliente pergunta sobre futebol

> **Backend — resolvido.** Recusa educada, sob teste.

Recusa educada em uma frase, e oferta de ajuda com o projeto dele. Está sob teste.

### 🎨 A conexão cai no meio da pergunta

> **Frontend.** O histórico com pergunta sem resposta está correto — é o que de fato aconteceu. Falta exibir isso com um botão de tentar de novo.

A pergunta já foi salva; a resposta não veio.

**O que acontece:** o histórico fica com uma pergunta sem resposta — que é exatamente o que aconteceu, e está correto.

**O frontend precisa** exibir isso de forma compreensível, com um botão de tentar de novo, em vez de uma conversa que parece corrompida.

---

## Lista de compras

### ✅ Adicionar produto que saiu do catálogo

> **Backend — resolvido.** 404.

Entre a busca e o toque, o produto sumiu. Devolve `404`.

### 🎨 Remover item que a outra aba já removeu

> **Frontend — e a decisão de manter o 404 é deliberada.** Devolver 204 sempre esconderia um id errado, que é um defeito de verdade; o 404 carrega informação que o 204 apaga. A tela trata "item não está na lista" como sucesso, porque o cliente conseguiu o que queria.

Devolve `404`. **O frontend deveria tratar como sucesso**: o cliente queria que o item saísse, e ele saiu. Mostrar erro para quem conseguiu o que queria é confuso.

### 🎨 O mapa com a lista vazia

> **Frontend.** O backend devolve a planta e a posição normalmente; o convite é da tela.

O cliente vai ao mapa antes de escolher qualquer coisa.

**Deveria ver:** a loja e o marcador dele, com um convite — "escolha produtos para vê-los aqui". Não um mapa vazio sem explicação.

### 🚫 Um produto da lista não tem posição no mapa

> **Não pode acontecer.** `ponto_mapa_id` é `NOT NULL` e a relação é `optional = false`: um produto sem posição não entra no banco. Além disso, `MapaIntegracaoTest` verifica que **todo produto do catálogo cai dentro do bloco da própria seção** — e a carga recusa produto cuja seção não exista na planta.

Dado incompleto no catálogo. Hoje o sistema aguenta, mas o item simplesmente não aparece no mapa — e some sem explicação.

**Deveria:** aparecer na lista com um aviso de "localização indisponível". Some da tela é pior do que aparecer com defeito assumido.

---

## Coleta

### ✅ Marcar o mesmo item duas vezes

> **Backend — resolvido.** Idempotente, e vale a hora da **primeira** confirmação para não mover a posição do cliente.

Idempotente. Toque duplo ou reenvio da rede não é erro.

### ✅ Desmarcar quando não há item marcado antes

> **Backend — resolvido.** A posição volta sozinha para a placa lida ([D-64](decisoes-tecnicas.md#d-64-desmarcar-um-item-não-precisa-mexer-na-posição-do-cliente)), sob teste. Se a sessão nasceu sem posição, `posicaoAtual` vem nula — **e a tela precisa dizer isso em vez de inventar um marcador**.

A posição do cliente vem do último item coletado. Se ele desmarcar o único que tinha, a posição precisa voltar **para o último QR Code escaneado**.

Se não houver nem isso — sessão que começou sem posição —, o mapa fica sem o "você está aqui", e a tela deve dizer isso em vez de mostrar um marcador inventado.

### 🎨 Marcar item que outra aba removeu

> **Frontend.** 404; recarregar a lista e seguir.

Devolve `404`. O frontend recarrega a lista e segue.

---

## Ruptura de estoque

### ✅ Não há substituto plausível por perto

> **Backend — resolvido.** 422, e a ruptura fica registrada mesmo assim. **A tela precisa distinguir isso de erro do sistema.**

Devolve `422`, e a ruptura **fica registrada mesmo assim** — para a loja, "o cliente foi até a prateleira e não havia nada, nem alternativa" é o relato mais grave.

**O cliente vê:** "não encontramos nada equivalente por perto". **O frontend precisa distinguir isso de erro do sistema.**

### ✅ A IA está fora do ar na hora da ruptura

> **Backend — resolvido.** Cai para o mais próximo, com `origemSugestao = PROXIMIDADE`. **A tela não pode chamar isso de recomendação inteligente** ([O-04](observacoes.md#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade)).

Cai para o produto disponível mais próximo, com justificativa honesta e o campo `origemSugestao` marcando `PROXIMIDADE`.

**A tela não pode chamar isso de recomendação inteligente** — ver [O-04](observacoes.md#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade).

### 💬 O substituto sugerido acabou também

> **Decidido: não limitar.** A cadeia termina sozinha quando o raio se esgota e o sistema devolve 422. Um contador seria regra a mais para um caso que o cliente abandona antes.

Entre a sugestão e a chegada do cliente, alguém levou o último.

**O que acontece hoje:** ele foi adicionado à lista, então o cliente pode relatar ruptura nele também, e recebe outra sugestão. **O fluxo se auto-resolve**, mas ninguém verificou se a experiência disso é boa — duas rupturas seguidas podem passar a impressão de que a loja não tem nada.

Vale limitar? Depois de duas sugestões recusadas, talvez o certo seja oferecer chamar um vendedor.

### ✅ O cliente aceita um substituto que já está na lista dele

> **Backend — resolvido.** O item em falta sai, nada é duplicado ([D-65](decisoes-tecnicas.md#d-65-aceitar-o-substituto-e-uma-acao-so-e-o-substituto-entra-nao-coletado)), sob teste em `SubstituicaoDeItemIntegracaoTest`. **A tela precisa explicar o que aconteceu**, senão parece que a ação não funcionou.

Ele já tinha escolhido aquele produto antes. Aceitar criaria duplicata.

O sistema já ignora produto repetido, então **o item em falta sai e nada é duplicado** — mas o cliente precisa entender o que aconteceu, senão parece que a ação não funcionou.

### 🎨 O cliente relata ruptura no mesmo item duas vezes

> **Frontend.** Repetir é proposital — dois relatos são dois dados para a loja. O que falta é **travar o botão em voo**, porque cada relato custa duas chamadas da cota ([O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição)).

Cada toque é um relato novo, **de propósito** — duas visitas frustradas à prateleira são dois dados para a loja.

Mas cada relato custa duas chamadas ao Gemini. **O botão precisa travar enquanto a requisição está em voo** — ver [O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição).

---

## Encerramento

### 🎨 O cliente encerra com itens não coletados

> **Frontend.** O backend permite, e precisa permitir. A confirmação é da tela.

Permitido, e precisa ser. **A tela deve reconhecer**: "você está encerrando com 2 itens não coletados. Quer mesmo?" — e aceitar o sim sem insistir.

### ✅ Ele encerra e volta pelo aparelho

> **Backend — resolvido.**

Sessão concluída: leitura sim, escrita não. Já tratado.

---

## Infraestrutura

### 🎨 A instância dorme e demora dois minutos para acordar

> **Frontend, e o time já decidiu seguir no plano gratuito.** Medido de novo em 25/08 com o catálogo maior: **176 segundos**. A tela precisa mostrar "preparando o sistema" em vez de parecer travada — **isso vai acontecer em alguma demonstração**.

Medido: **134 segundos** no plano gratuito, por causa do décimo de CPU.

O time decidiu seguir no gratuito e aquecer a aplicação antes de apresentar. **Mas o cliente numa loja de verdade seria o primeiro acesso do dia** — e esperaria dois minutos olhando uma tela em branco.

Para o vídeo e a banca, o aquecimento resolve. Vale a tela mostrar "preparando o sistema..." em vez de parecer travada, porque **isso vai acontecer em alguma demonstração**.

### 🎨 O backend está fora do ar

> **Frontend.** O erro já sai limpo, sem vazar detalhe interno, e está sob teste.

Já devolve erro limpo, sem vazar detalhe interno — está sob teste. Falta o frontend transformar isso numa mensagem humana.

---

## O que sai daqui como trabalho

**Nada de backend.** Os 30 cenários foram auditados contra o código em 25/08/2026, depois dos treze cards do escopo revisado: 14 estão resolvidos e provados por teste, 1 é impossível pelo esquema, 1 foi decidido conscientemente, e **os 14 restantes são de tela**.

### O que o frontend precisa tratar

Nenhum destes é trabalho grande. O que eles têm em comum é que **o backend já entrega a informação certa** — falta a tela usá-la em vez de repassar o erro cru.

**A forma do erro é sempre a mesma.** Auditado por teste em 25/08/2026: todo erro da API — de 400 a 500, em qualquer endpoint — devolve os mesmos seis campos, com `status` repetido no corpo, o `path` que falhou, e **nunca detalhe interno**. Um tratamento só resolve todos.

> **Decida pelo `status`, não pelo texto de `error`.** O status é contrato; o texto é para humano ler e pode ser reescrito.

**Erros que não são erros.** Três respostas que a tela precisa traduzir em vez de mostrar:

| Resposta | O que significa | O que mostrar |
|---|---|---|
| `409` em qualquer escrita | jornada já encerrada | "quer começar uma nova?" |
| `404` ao remover ou marcar item | a outra aba já mexeu | recarregar a lista — sem alarme |
| `422` na ruptura | não há substituto por perto | "não encontramos nada equivalente" — não é falha do sistema |

**Ausências que não vêm como erro.** Duas situações em que a resposta é `200` e a informação está no corpo:

- **`posicaoAtual` nula** — o código da placa não foi reconhecido, ou a sessão nasceu sem placa. Precisa avisar; se ignorar, o cliente digita errado e não entende por que o mapa não o localiza.
- **`imagemUrl` nula** — a coleta das URLs é incremental ([O-18](observacoes.md#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens)). Produto sem foto é normal, não defeito.

**Travas em botão.** Duas, pelo mesmo motivo — cada toque custa cota do Gemini, que é de cinco por minuto: o botão de **prateleira vazia** ([O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição)) e o campo do **assistente**, que leva cerca de oito segundos.

**Telas que ainda não existem.** A de **digitar o código de localização** ([O-19](observacoes.md#o-19-a-entrada-tem-um-plano-b-e-ele-é-uma-tela-que-ainda-não-existe)) e a de **"preparando o sistema"** para a partida a frio de 176 segundos, que vai acontecer em alguma demonstração.

**Estado no aparelho.** Guardar o `sessaoId` no `localStorage` e conferir a sessão ao abrir, antes de tentar escrever ([O-06](observacoes.md#o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar)). E recarregar a lista ao voltar para a aba, que resolve o caso das duas abas.

**Uma ajuda que talvez passe despercebida.** Quando o filtro devolve lista vazia, a resposta traz junto as **facetas do recorte** — a tela consegue dizer quais filtros ainda têm resultado, em vez de só oferecer "limpar tudo".

### Decisões do time, fora do código

- **Arte da placa:** QR + URL curta + código de localização. Tem custo de impressão.
- **Quantos QR Codes e onde.** A massa tem seis pontos provisórios; trocar as coordenadas não afeta nada no código.
- **Não rodar a suíte de integração durante gravação ou banca** ([O-21](observacoes.md#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema)).
