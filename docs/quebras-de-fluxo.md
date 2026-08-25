# Quebras de fluxo — o que pode dar errado, e o que o cliente vê

> Complementa [`fluxo-do-cliente.md`](fluxo-do-cliente.md), que descreve o caminho quando tudo dá certo. Este documento é o outro lado: **tudo que pode interromper esse caminho**, e o que o sistema faz em cada caso.
>
> Escrito depois do escopo revisado de 24/08/2026 — celular único, sem totem e sem rota calculada.
>
> Cada quebra traz: **o que acontece**, **o que o cliente vê** e **de quem é a solução**. O estado de cada uma está marcado:
> **✅ resolvido** · **🔧 falta implementar** · **❓ precisa de decisão**

---

## As duas que mudam decisões já tomadas

Antes da lista, duas quebras que o novo escopo criou e que não existiam quando o totem existia.

### ❓ A sessão morre em 30 minutos parada, e agora ela é a única coisa que o cliente tem

Antes, a sessão vivia no totem e o cliente levava a rota no celular. Se a sessão expirasse, ele ainda tinha a lista na tela.

**Agora a sessão é tudo.** Ela guarda a lista inteira, e o TTL de 30 minutos de inatividade foi dimensionado para um totem — onde o objetivo era **liberar o equipamento** para o próximo cliente.

Esse motivo desapareceu. Não há equipamento para liberar: o aparelho é do cliente.

E o cenário é banal — o cliente monta quinze itens, recebe uma ligação, conversa quarenta minutos, volta e **perdeu tudo**. Nada no sistema avisa; ele simplesmente encontra uma sessão inválida.

> **Recomendação: subir o TTL para algo entre 2 e 4 horas.** É mais tempo do que qualquer compra realista leva, e não custa nada — a varredura de sessões vencidas continua limpando o banco do mesmo jeito. Mexe numa constante e na [D-24](decisoes-tecnicas.md#d-24-ttl-da-sessão-é-renovado-a-cada-interação).

### ❓ Loja de 10.000 m², paredes de concreto, e um produto que só funciona online

Toda ação do cliente é uma chamada de rede: ver o mapa, marcar item, perguntar ao assistente. **No fundo de um corredor de materiais de construção, o sinal cai.**

Hoje o cliente veria erro em tudo, sem entender por quê — e a lista dele está no servidor, não no bolso.

Saídas possíveis, em ordem de custo:

1. **Avisar bem.** Detectar que está offline e mostrar "sem conexão, tentando de novo" em vez de erro cru. Barato, e evita a pior impressão.
2. **Guardar a lista e o mapa no aparelho** e mostrá-los mesmo offline. O cliente continua vendo para onde ir, que é o essencial.
3. **Enfileirar as marcações** feitas offline e enviá-las quando o sinal voltar.

As três são trabalho de frontend. **A 1 eu trataria como obrigatória**; a 2 é o que separa um protótipo de algo que funcionaria numa loja de verdade — e é um ótimo argumento para a banca, mesmo que só descrito.

---

## Entrada e sessão

### ❓ O QR Code está rasgado, sujo ou riscado

Adesivo em corredor de loja não sobrevive muito tempo. Sem conseguir ler, o cliente não entra.

**O que o cliente vê:** nada — ele desiste e vai embora, que é o pior desfecho possível.

**Saída:** imprimir junto ao QR um **código curto digitável** — algo como `TIN-04`. Mesma função, sem depender da câmera nem do estado do adesivo. O backend passa a aceitar os dois: identificador do ponto ou código curto.

Isso também resolve o cliente cuja câmera não funciona ou que não sabe usar leitor de QR.

### 🔧 O QR aponta para um ponto que não existe mais

A loja remanejou uma seção, o ponto saiu do banco, mas o adesivo continua na parede.

**O que o cliente vê:** hoje, `404` cru.

**Deveria ver:** "não reconhecemos este ponto — procure outro QR Code por perto". E a sessão deveria começar mesmo assim, **sem posição inicial**, em vez de não começar. Melhor um mapa sem o "você está aqui" do que nenhum sistema.

### 🔧 A sessão guardada no aparelho já expirou

**O que o cliente vê:** hoje, erro na primeira ação.

**Deveria ver:** ao abrir, o app verifica a sessão. Se estiver morta, mostra "sua sessão anterior expirou" e começa uma nova — limpa, sem tela de erro.

O backend já responde se a sessão vale; falta o frontend perguntar **antes** de tentar usar.

### ✅ O cliente volta a uma sessão já concluída

Ele encerrou, agradeceu, e abre a página de novo pelo aparelho.

**O que acontece:** leitura funciona, escrita não. Ele consegue ver a lista e o histórico do que fez, mas não adicionar nem marcar nada — recebe `409`.

Está correto e já implementado ([D-41](decisoes-tecnicas.md#d-41-sessão-encerrada-continua-legível-mas-não-gravável)). **O frontend precisa tratar esse 409 como "jornada encerrada, quer começar outra?"**, e não como falha.

### 🔧 Duas abas abertas na mesma sessão

O cliente abre o link duas vezes sem perceber.

**O que acontece no banco:** nada de ruim. As escritas não se atropelam — está medido e sob teste ([D-48](decisoes-tecnicas.md#d-48-gravar-pelo-agregado-é-seguro-aqui--e-a-investigação-que-provou-isso-depois-de-duas-hipóteses-erradas)).

**O que o cliente vê:** duas telas discordando. Ele marca um item numa aba, volta para a outra e o item ainda aparece pendente.

**Solução:** recarregar a lista ao voltar para a aba. É frontend.

---

## Catálogo e busca

### 🔧 A busca não acha nada

Mesmo com a tolerância a erro de digitação, o cliente pode procurar algo que a loja não tem.

**Deveria ver:** "não encontramos *pergolado*" e, junto, um caminho de saída — **perguntar ao assistente**. Ele entende pedido aberto e pode sugerir o que serve para aquele projeto.

Transformar o beco sem saída na porta de entrada do recurso mais forte do sistema.

### 🔧 O filtro devolve lista vazia

"Jardim" + "só disponíveis" pode não sobrar nada.

**Deveria ver:** aviso de que o filtro está restringindo, com um toque para limpá-lo. Nunca uma tela branca.

### ✅ O produto abriu, mas o id não existe

Link velho, produto retirado do catálogo.

Já devolve `404` limpo, com JSON no formato de sempre. **O frontend precisa mostrar "produto não disponível" em vez de repassar o erro.**

---

## Assistente

### ✅ A IA está fora do ar ou a cota estourou

**O que o cliente vê:** *"Não consegui consultar o assistente agora. Você pode buscar o produto direto pela tela de busca, ou tentar novamente em instantes."*

Implementado. E a resposta de indisponibilidade **não entra no histórico** — o assistente não deve "lembrar" de ter estado fora do ar.

### 🔧 A resposta demora oito segundos

Medido na instância publicada. Sem aviso, o cliente acha que travou e toca de novo.

**Deveria ver:** indicação de que o assistente está pensando, e o campo bloqueado enquanto isso. **Bloquear importa duas vezes**: evita a impressão de travamento e evita gastar duas chamadas da cota gratuita, que é de cinco por minuto.

### ✅ O cliente pergunta sobre futebol

Recusa educada em uma frase, e oferta de ajuda com o projeto dele. Está sob teste.

### 🔧 A conexão cai no meio da pergunta

A pergunta já foi salva; a resposta não veio.

**O que acontece:** o histórico fica com uma pergunta sem resposta — que é exatamente o que aconteceu, e está correto.

**O frontend precisa** exibir isso de forma compreensível, com um botão de tentar de novo, em vez de uma conversa que parece corrompida.

---

## Lista de compras

### ✅ Adicionar produto que saiu do catálogo

Entre a busca e o toque, o produto sumiu. Devolve `404`.

### 🔧 Remover item que a outra aba já removeu

Devolve `404`. **O frontend deveria tratar como sucesso**: o cliente queria que o item saísse, e ele saiu. Mostrar erro para quem conseguiu o que queria é confuso.

### 🔧 O mapa com a lista vazia

O cliente vai ao mapa antes de escolher qualquer coisa.

**Deveria ver:** a loja e o marcador dele, com um convite — "escolha produtos para vê-los aqui". Não um mapa vazio sem explicação.

### 🔧 Um produto da lista não tem posição no mapa

Dado incompleto no catálogo. Hoje o sistema aguenta, mas o item simplesmente não aparece no mapa — e some sem explicação.

**Deveria:** aparecer na lista com um aviso de "localização indisponível". Some da tela é pior do que aparecer com defeito assumido.

---

## Coleta

### ✅ Marcar o mesmo item duas vezes

Idempotente. Toque duplo ou reenvio da rede não é erro.

### 🔧 Desmarcar quando não há item marcado antes

A posição do cliente vem do último item coletado. Se ele desmarcar o único que tinha, a posição precisa voltar **para o último QR Code escaneado**.

Se não houver nem isso — sessão que começou sem posição —, o mapa fica sem o "você está aqui", e a tela deve dizer isso em vez de mostrar um marcador inventado.

### 🔧 Marcar item que outra aba removeu

Devolve `404`. O frontend recarrega a lista e segue.

---

## Ruptura de estoque

### ✅ Não há substituto plausível por perto

Devolve `422`, e a ruptura **fica registrada mesmo assim** — para a loja, "o cliente foi até a prateleira e não havia nada, nem alternativa" é o relato mais grave.

**O cliente vê:** "não encontramos nada equivalente por perto". **O frontend precisa distinguir isso de erro do sistema.**

### ✅ A IA está fora do ar na hora da ruptura

Cai para o produto disponível mais próximo, com justificativa honesta e o campo `origemSugestao` marcando `PROXIMIDADE`.

**A tela não pode chamar isso de recomendação inteligente** — ver [O-04](observacoes.md#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade).

### ❓ O substituto sugerido acabou também

Entre a sugestão e a chegada do cliente, alguém levou o último.

**O que acontece hoje:** ele foi adicionado à lista, então o cliente pode relatar ruptura nele também, e recebe outra sugestão. **O fluxo se auto-resolve**, mas ninguém verificou se a experiência disso é boa — duas rupturas seguidas podem passar a impressão de que a loja não tem nada.

Vale limitar? Depois de duas sugestões recusadas, talvez o certo seja oferecer chamar um vendedor.

### 🔧 O cliente aceita um substituto que já está na lista dele

Ele já tinha escolhido aquele produto antes. Aceitar criaria duplicata.

O sistema já ignora produto repetido, então **o item em falta sai e nada é duplicado** — mas o cliente precisa entender o que aconteceu, senão parece que a ação não funcionou.

### 🔧 O cliente relata ruptura no mesmo item duas vezes

Cada toque é um relato novo, **de propósito** — duas visitas frustradas à prateleira são dois dados para a loja.

Mas cada relato custa duas chamadas ao Gemini. **O botão precisa travar enquanto a requisição está em voo** — ver [O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição).

---

## Encerramento

### 🔧 O cliente encerra com itens não coletados

Permitido, e precisa ser. **A tela deve reconhecer**: "você está encerrando com 2 itens não coletados. Quer mesmo?" — e aceitar o sim sem insistir.

### ✅ Ele encerra e volta pelo aparelho

Sessão concluída: leitura sim, escrita não. Já tratado.

---

## Infraestrutura

### ❓ A instância dorme e demora dois minutos para acordar

Medido: **134 segundos** no plano gratuito, por causa do décimo de CPU.

O time decidiu seguir no gratuito e aquecer a aplicação antes de apresentar. **Mas o cliente numa loja de verdade seria o primeiro acesso do dia** — e esperaria dois minutos olhando uma tela em branco.

Para o vídeo e a banca, o aquecimento resolve. Vale a tela mostrar "preparando o sistema..." em vez de parecer travada, porque **isso vai acontecer em alguma demonstração**.

### 🔧 O backend está fora do ar

Já devolve erro limpo, sem vazar detalhe interno — está sob teste. Falta o frontend transformar isso numa mensagem humana.

---

## O que sai daqui como trabalho

**Decisões pendentes:**

| # | Assunto |
|---|---|
| 1 | Subir o TTL da sessão de 30 minutos para 2–4 horas |
| 2 | Quanto de suporte a offline vale construir |
| 3 | Código curto digitável ao lado de cada QR Code |
| 4 | Limitar sugestões seguidas de substituto |

**Backend:** aceitar código curto além do ponto, começar sessão sem posição quando o ponto não existe, e desmarcar item revertendo a posição.

**Frontend:** a maior parte. Praticamente toda quebra termina em "o backend já responde certo, falta a tela traduzir". Vale a dupla ler este documento inteiro — ele é, na prática, a especificação dos estados de erro de cada tela.
