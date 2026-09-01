# Quebras de fluxo — o que pode dar errado, e o que o cliente vê

> Complementa [`fluxo-do-cliente.md`](fluxo-do-cliente.md), que descreve o caminho quando tudo dá certo. Este documento é o outro lado: **tudo que pode interromper esse caminho**, e o que o sistema faz em cada caso.
>
> Escrito depois do escopo revisado de 24/08/2026 — celular único, sem totem e sem rota calculada.
>
> Cada quebra traz: **o que acontece**, **o que o cliente vê** e **de quem é a solução**.
>
> **Auditado duas vezes.** Em **25/08/2026** contra o backend, depois de os treze cards do escopo revisado ficarem prontos — e naquele momento **não existia frontend**, então tudo que dependia de tela ficou pendente por definição. Em **30/08/2026** contra o app que passou a existir: as quatorze pendências de tela foram abertas uma a uma no código.
>
> **Cinco já estavam resolvidas** e ninguém tinha registrado. As outras nove continuam abertas, agora com o que **já existe** anotado em cada uma — o que resta é menor do que a marcação sugeria.
>
> Cada cenário abre com uma linha dizendo de quem ele é:
>
> | | |
> |---|---|
> | ✅ | **resolvido** — com a decisão, o teste ou a tela que provam |
> | 🎨 | **falta na tela** — o backend já entrega o que ela precisa |
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

> **Frontend — uma das três saídas está pronta.** Reauditado em 30/08.
>
> - **Saída 2, guardar a lista no aparelho: feita.** O roteiro é *local-first* — grava no aparelho primeiro, e o servidor é espelho. Foi o que fez o app continuar funcionando quando derrubamos o backend de propósito.
> - **Saída 1, avisar bem: feita.** Catálogo, setores e chat dizem que não conseguiram falar com a loja, cada um com a frase certa para o seu caso ([D-75](decisoes-tecnicas.md#d-75-a-tela-não-fabrica-resposta-da-ia-quando-não-consegue-perguntar) e [D-77](decisoes-tecnicas.md#d-77-o-catálogo-lança-quando-não-consegue-perguntar-e-a-tela-distingue-isso-de-não-há)).
> - **Saída 3, enfileirar as marcações: não feita.** `alternarColetaItem` grava local e ignora a resposta; o servidor fica para trás sem nada reenviar. **É o que resta deste cenário.**
>
> O catálogo inventado que preenchia o silêncio com produtos falsos foi apagado em 30/08 — era o que impedia qualquer aviso honesto de ser verdade.

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

### ✅ A sessão guardada no aparelho já expirou

> **Resolvido.** A mecânica já existia: `obterOuCriarSessao` consulta a sessão guardada antes de usá-la e cria outra se o status não for `ACTIVE`.
>
> **Entrou a frase que faltava** — *"Sua sessão anterior expirou. Começamos uma nova."* A detecção não exigiu mexer no serviço: o `App` guarda o id **antes** da chamada e compara com o que volta. Id diferente ⇒ houve troca. Ver [O-06](observacoes.md#o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar--já-existia-e-foi-verificada).

**O que o cliente vê:** hoje, erro na primeira ação.

**Deveria ver:** ao abrir, o app verifica a sessão. Se estiver morta, mostra "sua sessão anterior expirou" e começa uma nova — limpa, sem tela de erro.

O backend já responde se a sessão vale; falta o frontend perguntar **antes** de tentar usar.

### ✅ O cliente volta a uma sessão já concluída

> **Backend — resolvido.** Leitura sim, escrita 409 ([D-41](decisoes-tecnicas.md#d-41-sessão-encerrada-continua-legível-mas-não-gravável)). **A tela trata o 409 como "jornada encerrada"**, não como falha.

Ele encerrou, agradeceu, e abre a página de novo pelo aparelho.

**O que acontece:** leitura funciona, escrita não. Ele consegue ver a lista e o histórico do que fez, mas não adicionar nem marcar nada — recebe `409`.

Está correto e já implementado ([D-41](decisoes-tecnicas.md#d-41-sessão-encerrada-continua-legível-mas-não-gravável)). **O frontend precisa tratar esse 409 como "jornada encerrada, quer começar outra?"**, e não como falha.

### ✅ O cliente toca num item de navegação que abre modal

> **Resolvido em 31/08/2026, e este cenário não existia na auditoria.** Ele veio de uso real: tocar em "Scan & Rota" abria o modal e o item continuava marcado depois de fechá-lo — **e continuava marcado mesmo indo para Setores**.
>
> **A raiz eram duas verdades sobre a mesma coisa:** `currentView`, que sabe a tela desenhada, e um `activeTab` paralelo, que nem todo caminho atualizava. O destaque passou a ser **derivado** da tela, e o estado paralelo saiu.
>
> **A regra que fica:** *modal não é lugar*. Um destaque de navegação afirma "você está aqui"; um modal acontece por cima de onde o cliente já está. Por isso "Scan" e "Atendimento" não destacam nada — tratá-los como destino era a origem do defeito. Ver [D-81](decisoes-tecnicas.md#d-81-o-destaque-da-navegação-é-derivado-da-tela-e-modal-não-é-lugar).
>
> **Apareceu junto:** o "Setores da Loja" do cabeçalho não tinha classe de ativo nenhuma e **nunca acendia**, mesmo com a tela aberta.

**O que o cliente via:** um item de navegação aceso apontando para um lugar onde ele não estava.

---

### ✅ Duas abas abertas na mesma sessão

> **Resolvido na tela.** A lista se reconcilia com o servidor quando a aba volta a ficar visível — `visibilitychange` para troca de aba e volta de outro aplicativo, `focus` para troca de janela.
>
> **Verificado em 30/08**, sem recarregar a página: item adicionado, removido pelo servidor como se fosse a outra aba, e ao voltar o foco a lista foi de **1 para 0** — inclusive no armazenamento local.
>
> Usa o mesmo caminho da abertura do app: `consultarRoteiro` traz a lista do servidor e grava por cima. O banco nunca esteve em risco, e continua medido ([D-48](decisoes-tecnicas.md#d-48-gravar-pelo-agregado-é-seguro-aqui--e-a-investigação-que-provou-isso-depois-de-duas-hipóteses-erradas)).

O cliente abre o link duas vezes sem perceber.

**O que acontece no banco:** nada de ruim. As escritas não se atropelam — está medido e sob teste ([D-48](decisoes-tecnicas.md#d-48-gravar-pelo-agregado-é-seguro-aqui--e-a-investigação-que-provou-isso-depois-de-duas-hipóteses-erradas)).

**O que o cliente vê:** duas telas discordando. Ele marca um item numa aba, volta para a outra e o item ainda aparece pendente.

**Solução:** recarregar a lista ao voltar para a aba. É frontend.

---

## Catálogo e busca

### ✅ A busca não acha nada

> **Resolvido, e era o que faltava.** O estado vazio já dizia o que foi procurado e oferecia três saídas — mas as três eram formas de **refazer a mesma busca**, e nenhuma de mudar de pergunta.
>
> Entrou **"Perguntar ao assistente"**, em destaque, acima das outras. Ele é o único que responde *"o que eu uso para isso?"*, que é a pergunta de quem não achou o que procurava.
>
> **Verificado:** buscar "pergolado de bambu" e chegar ao chat em um toque.

Mesmo com a tolerância a erro de digitação, o cliente pode procurar algo que a loja não tem.

**Deveria ver:** "não encontramos *pergolado*" e, junto, um caminho de saída — **perguntar ao assistente**. Ele entende pedido aberto e pode sugerir o que serve para aquele projeto.

Transformar o beco sem saída na porta de entrada do recurso mais forte do sistema.

### ✅ O filtro devolve lista vazia

> **Resolvido na tela.** Verificado em 30/08: `CatalogSearchPage.jsx:209` oferece "Limpar características e filtros" quando há filtro ativo, e "Ver todas as seções" quando o recorte é de seção. O cliente nunca fica sem saída, que era o que este cenário exigia.
>
> **Fica a oportunidade, não a pendência:** as **facetas do recorte** vêm na mesma resposta e permitiriam dizer *quais* filtros ainda têm resultado, em vez de só oferecer limpar ([D-63](decisoes-tecnicas.md#d-63-as-facetas-ignoram-a-escolha-do-cliente-sobre-elas-mesmas)). Melhoria, não correção.

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

### ✅ A resposta demora oito segundos

> **Resolvido na tela, inclusive na parte que protege a cota.** Verificado em 30/08 no `AIChatModal.jsx`: indicador de "pensando" enquanto espera (linha 322), campo e botão desabilitados por `disabled={isLoading}` (linhas 358 e 364), e `if (!text || isLoading) return` barrando o reenvio na origem (linha 133).
>
> As duas razões do cenário estão cobertas: o cliente vê que não travou, e um toque duplo não gasta duas chamadas da cota gratuita ([O-01](observacoes.md#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)).

Medido na instância publicada. Sem aviso, o cliente acha que travou e toca de novo.

**Deveria ver:** indicação de que o assistente está pensando, e o campo bloqueado enquanto isso. **Bloquear importa duas vezes**: evita a impressão de travamento e evita gastar duas chamadas da cota gratuita, que é de cinco por minuto.

### ✅ O cliente pergunta sobre futebol

> **Backend — resolvido.** Recusa educada, sob teste.

Recusa educada em uma frase, e oferta de ajuda com o projeto dele. Está sob teste.

### ✅ A conexão cai no meio da pergunta

> **Resolvido.** A pergunta não fica pendurada sozinha ([D-75](decisoes-tecnicas.md#d-75-a-tela-não-fabrica-resposta-da-ia-quando-não-consegue-perguntar)), e agora há um **"tentar de novo"** que reenvia a última pergunta sem redigitar.
>
> **O botão aparece só onde faz sentido.** A mensagem que a tela gera quando não conseguiu falar com a loja carrega um campo `falhou`, e é ele que decide — não o texto. Resposta que veio do servidor não ganha botão, mesmo quando informa que o assistente está fora: aí quem falou foi a loja, e a mensagem dela se sustenta sozinha.
>
> **Verificado** derrubando só a chamada de chat: o botão aparece, e ao restaurar a conexão ele reenvia a pergunta e traz resposta.

**Antes disso havia coisa pior que uma conversa corrompida:** a tela inventava uma resposta e a assinava como sendo da IA, com corredores que não existem na nossa planta.

A pergunta já foi salva; a resposta não veio.

**O que acontece:** o histórico fica com uma pergunta sem resposta — que é exatamente o que aconteceu, e está correto.

**O frontend precisa** exibir isso de forma compreensível, com um botão de tentar de novo, em vez de uma conversa que parece corrompida.

---

## Lista de compras

### ✅ Adicionar produto que saiu do catálogo

> **Backend — resolvido.** 404.

Entre a busca e o toque, o produto sumiu. Devolve `404`.

### ✅ Remover item que a outra aba já removeu

> **Resolvido — e por construção, não por tratamento de erro.** Verificado em 30/08: `removerDoRoteiro` tira o item da lista local **antes** da chamada e nunca lê a resposta (`roteiroService.js:177`). Um 404 do servidor é estruturalmente incapaz de virar erro na tela, porque o item já saiu.
>
> É mais forte do que o que este cenário pedia. Ele sugeria tratar o 404 como sucesso; o desenho *local-first* fez o caso desaparecer.
>
> **A decisão de o backend manter o 404 continua deliberada:** devolver 204 sempre esconderia id errado, que é defeito de verdade. O 404 carrega informação que o 204 apaga.

Devolve `404`. **O frontend deveria tratar como sucesso**: o cliente queria que o item saísse, e ele saiu. Mostrar erro para quem conseguiu o que queria é confuso.

### ✅ O mapa com a lista vazia

> **Resolvido na tela, com um defeito de texto anexo.** Verificado em 30/08: `StoreMapPage.jsx:711` mostra a loja com o convite "Sua lista está vazia. Adicione produtos na vitrine para traçar a melhor rota." Não é mais um mapa vazio sem explicação.
>
> **Mas a frase promete o que o sistema não faz.** "Traçar a melhor rota" é justamente o recurso que a mentoria mandou tirar — o mapa mostra posições, e quem escolhe o caminho é o cliente ([D-49](decisoes-tecnicas.md#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada)). É o mesmo tipo de afirmação falsa que o card de honestidade da tela inicial já limpou em outros cinco lugares, e este passou. Registrado como [O-26](observacoes.md#o-26-o-convite-do-mapa-vazio-promete-uma-rota-que-não-existe--corrigido).

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

### ✅ Marcar item que outra aba removeu

> **Resolvido, e pelas duas pontas.** O erro nunca aparecia: `alternarColetaItem` muda o estado local antes da chamada e não lê a resposta (`roteiroService.js:204`), então o 404 não vira erro na tela.
>
> **O que faltava** era a divergência entre as abas, e ela caiu junto com o cenário acima: ao voltar o foco, a lista se reconcilia com o servidor.

Devolve `404`, e a tela nunca o expõe. Ao reganhar o foco, a lista se corrige.

---

## Ruptura de estoque

### ✅ Não há substituto plausível por perto

> **Backend — resolvido.** 422, e a ruptura fica registrada mesmo assim. **A tela precisa distinguir isso de erro do sistema.**

Devolve `422`, e a ruptura **fica registrada mesmo assim** — para a loja, "o cliente foi até a prateleira e não havia nada, nem alternativa" é o relato mais grave.

**O cliente vê:** "não encontramos nada equivalente por perto". **O frontend precisa distinguir isso de erro do sistema.**

### ✅ A IA está fora do ar na hora da ruptura

> **Backend — resolvido.** Cai para o mais próximo, com `origemSugestao = PROXIMIDADE`. **A tela não pode chamar isso de recomendação inteligente** ([O-04](observacoes.md#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade--resolvida-na-tela)).

Cai para o produto disponível mais próximo, com justificativa honesta e o campo `origemSugestao` marcando `PROXIMIDADE`.

**A tela não pode chamar isso de recomendação inteligente** — ver [O-04](observacoes.md#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade--resolvida-na-tela).

### 💬 O substituto sugerido acabou também

> **Decidido: não limitar.** A cadeia termina sozinha quando o raio se esgota e o sistema devolve 422. Um contador seria regra a mais para um caso que o cliente abandona antes.

Entre a sugestão e a chegada do cliente, alguém levou o último.

**O que acontece hoje:** ele foi adicionado à lista, então o cliente pode relatar ruptura nele também, e recebe outra sugestão. **O fluxo se auto-resolve**, mas ninguém verificou se a experiência disso é boa — duas rupturas seguidas podem passar a impressão de que a loja não tem nada.

Vale limitar? Depois de duas sugestões recusadas, talvez o certo seja oferecer chamar um vendedor.

### ✅ O cliente aceita um substituto que já está na lista dele

> **Backend — resolvido.** O item em falta sai, nada é duplicado ([D-65](decisoes-tecnicas.md#d-65-aceitar-o-substituto-e-uma-acao-so-e-o-substituto-entra-nao-coletado)), sob teste em `SubstituicaoDeItemIntegracaoTest`. **A tela precisa explicar o que aconteceu**, senão parece que a ação não funcionou.

Ele já tinha escolhido aquele produto antes. Aceitar criaria duplicata.

O sistema já ignora produto repetido, então **o item em falta sai e nada é duplicado** — mas o cliente precisa entender o que aconteceu, senão parece que a ação não funcionou.

### ✅ O cliente relata ruptura no mesmo item duas vezes

> **Resolvido, e a primeira tentativa não bastou.** O toque duplo é barrado por uma trava **síncrona**, num `useRef`.
>
> **Por que não deu para usar estado.** Havia uma guarda por `useState` e o botão ganhou `disabled` — as duas falharam no teste: cinco toques no mesmo tique dispararam cinco chamadas. `useState` só chega ao próximo render, e até lá o fecho do manipulador continua vendo o valor antigo. Com o `useRef`, os mesmos cinco toques viram **uma** chamada.
>
> **Repetir de propósito continua funcionando**, e é o comportamento certo: relatos separados por segundos passam, porque duas visitas frustradas à prateleira são dois dados para a loja. O que a trava impede é a rajada acidental, que custaria duas chamadas do Gemini por toque.
>
> O `disabled` continua no botão, mas pelo outro motivo: mostrar que está em andamento. Ver [O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição--resolvida-com-trava-síncrona).

Cada toque é um relato novo, **de propósito** — duas visitas frustradas à prateleira são dois dados para a loja.

Mas cada relato custa duas chamadas ao Gemini. **O botão precisa travar enquanto a requisição está em voo** — ver [O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição--resolvida-com-trava-síncrona).

---

## Encerramento

### ✅ O cliente encerra com itens não coletados

> **Resolvido na tela, e com o tom certo.** Verificado em 30/08: `FimJornadaModal.jsx:57` reconhece o caso sem cobrar — *"Você coletou 3 de 5 itens da sua lista e pode seguir para o pagamento com o que já tem."* Aceita o encerramento sem insistir, que era a parte difícil do pedido.

Permitido, e precisa ser. A tela reconhece e segue.

### ✅ Ele encerra e volta pelo aparelho

> **Backend — resolvido.**

Sessão concluída: leitura sim, escrita não. Já tratado.

---

## Infraestrutura

### ✅ A instância dorme e demora dois minutos para acordar

> **Resolvido na tela.** Passando de quatro segundos sem resposta na primeira carga, aparece uma faixa no topo: *"Preparando o sistema… a primeira abertura do dia leva até dois minutos."*
>
> **Não é um estado de erro, e o desenho separa os dois:** a indisponibilidade tem aviso próprio, com outro texto. Este diz "está vindo, espere", e some no primeiro desfecho — sucesso ou falha.
>
> **Verificado** atrasando a resposta de propósito: a faixa aparece durante a espera e some quando a carga termina. A partida a frio foi medida em **106 s e 109 s** em 30/08, contra os 176 s de 25/08.

Medido: **134 segundos** no plano gratuito, por causa do décimo de CPU.

O time decidiu seguir no gratuito e aquecer a aplicação antes de apresentar. **Mas o cliente numa loja de verdade seria o primeiro acesso do dia** — e esperaria dois minutos olhando uma tela em branco.

Para o vídeo e a banca, o aquecimento resolve. Vale a tela mostrar "preparando o sistema..." em vez de parecer travada, porque **isso vai acontecer em alguma demonstração**.

### ✅ O backend está fora do ar

> **Resolvido nas três frentes.** Verificado em 30/08 **com o backend derrubado de verdade**, não simulado:
>
> - **O catálogo e os setores:** dizem que não conseguiram falar com a loja, com zero produtos na tela e um botão de tentar de novo que funciona quando o servidor volta ([D-77](decisoes-tecnicas.md#d-77-o-catálogo-lança-quando-não-consegue-perguntar-e-a-tela-distingue-isso-de-não-há)).
> - **O chat:** admite que não conseguiu perguntar, e oferece o caminho que funciona ([D-75](decisoes-tecnicas.md#d-75-a-tela-não-fabrica-resposta-da-ia-quando-não-consegue-perguntar)).
> - **O roteiro:** continua funcionando pelo desenho *local-first*.
>
> **A parte difícil não era mostrar erro, era escolher a frase.** "Nenhum produto encontrado" afirma que procuramos e não há; a tela precisa dizer que não conseguiu procurar. São dois estados separados no código, e um teste confirma que não se confundem.

O backend devolve erro limpo, sem vazar detalhe interno, e está sob teste. A tela transforma isso numa mensagem humana, sem preencher o vazio com dado falso.

---

## O que sai daqui como trabalho

**Nada de backend.** Os 30 cenários foram auditados em 25/08/2026 contra o código do servidor, e **reauditados em 30/08/2026 contra o app**, que não existia na primeira passagem.

| | 25/08 | 30/08 |
|---|---|---|
| Resolvidos | 14 | **28** |
| Impossível pelo esquema | 1 | 1 |
| Decidido conscientemente | 1 | 1 |
| Falta na tela | 14 | **1** |

**Cinco pendências já estavam resolvidas** e ninguém tinha registrado: o filtro sem resultado, a espera do assistente, remover item que a outra aba já removeu, o mapa com a lista vazia e o encerramento com itens pendentes. Duas delas — remover item e marcar item — foram resolvidas **por construção**: o desenho *local-first* fez o caso deixar de existir, em vez de tratá-lo.

### O que o frontend precisa tratar

> **O total subiu de 30 para 31.** O cenário do item de navegação que abre modal **não existia na auditoria** — veio de você usando o app, depois dela. Vale como lembrete de que auditoria de código não substitui alguém com o aparelho na mão.

**Um cenário**, e ele não é acabamento: **reenviar as marcações feitas offline**. É funcionalidade nova, com fila no aparelho e reconciliação, e fica de fora do lote de acabamentos por decisão — é invisível na demonstração, porque a lista do cliente está sempre certa; quem fica desatualizado é o servidor.

O trabalho de **avisar quando a loja não responde** saiu da lista em 30/08, e ele dependia de uma limpeza antes: enquanto o catálogo inventado preenchesse o silêncio com produtos falsos, nenhum aviso honesto poderia ser verdade.

O que os restantes têm em comum continua sendo que **o backend já entrega a informação certa** — falta a tela usá-la em vez de repassar o erro cru.

Os oito viraram cards em [`backlog-fechamento.md`](backlog-fechamento.md), no bloco `[FRONTEND]`, cada um com o que fazer e como verificar.

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

**Travas em botão.** Duas, pelo mesmo motivo — cada toque custa cota do Gemini, que é de cinco por minuto: o botão de **prateleira vazia** ([O-05](observacoes.md#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição--resolvida-com-trava-síncrona)) e o campo do **assistente**, que leva cerca de oito segundos.

**Telas que ainda não existem.** A de **digitar o código de localização** ([O-19](observacoes.md#o-19-a-entrada-tem-um-plano-b-e-ele-é-uma-tela-que-ainda-não-existe)) e a de **"preparando o sistema"** para a partida a frio de 176 segundos, que vai acontecer em alguma demonstração.

**Estado no aparelho.** Guardar o `sessaoId` no `localStorage` e conferir a sessão ao abrir, antes de tentar escrever ([O-06](observacoes.md#o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar--já-existia-e-foi-verificada)). E recarregar a lista ao voltar para a aba, que resolve o caso das duas abas.

**Uma ajuda que talvez passe despercebida.** Quando o filtro devolve lista vazia, a resposta traz junto as **facetas do recorte** — a tela consegue dizer quais filtros ainda têm resultado, em vez de só oferecer "limpar tudo".

### Duas escolhas de tela que valem segundos

Medido no ambiente publicado ([O-22](observacoes.md#o-22-a-tela-inicial-do-catálogo-leva-24-segundos-no-ambiente-publicado)):

- **não abrir no catálogo sem filtro.** Ele é o endpoint mais lento de todos — **2,4 s**. Abrir pelo menu de seções (355 ms) e depois mostrar uma seção (814 ms) dá duas telas rápidas em vez de uma lenta;
- **buscar `/mapa` uma vez e guardar.** Ele não depende de sessão justamente para isso, e economiza 669 ms a cada abertura da tela central do produto.

### Decisões do time, fora do código

- **Arte da placa:** QR + URL curta + código de localização. Tem custo de impressão.
- **Quantos QR Codes e onde.** A massa tem seis pontos provisórios; trocar as coordenadas não afeta nada no código.
- **Não rodar a suíte de integração durante gravação ou banca** ([O-21](observacoes.md#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema)).
