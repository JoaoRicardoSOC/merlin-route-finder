# Backlog de fechamento — o que falta para o projeto ficar completo

> **Para colar no Trello.** Cada card tem título, o que fazer, como verificar que ficou pronto e de quem é. Escritos em 30/08/2026, contra o código que existe hoje — cada afirmação aqui foi medida, não estimada.
>
> **Prazo que manda em tudo: 13/09/2026**, o vídeo da seletiva. Depois dele, 21/09 é a banca.
>
> Os prefixos entre colchetes servem para etiqueta no Trello. A ordem dentro de cada bloco é a sugerida.

---

## Placar

| Bloco | Cards | Quem |
|---|---|---|
| Entrega e publicação | 3 | João Ricardo e time | *(2 fechados em 30/08)*
| Frontend — acabamentos | 1 | Bielecky e Marcela | *(10 fechados em 30/08)*
| Banco e diagramas | 3 | Vicentini |
| Decisões e trabalho do time | 4 | Time |
| Backend e operação | 3 | João Ricardo e Caio |
| Limpeza antes da entrega | 2 | Time |

**Nada aqui bloqueia a demonstração.** O sistema roda de ponta a ponta: entrar pela placa, buscar, montar a lista, ver no mapa, coletar, relatar prateleira vazia, aceitar o substituto e encerrar. O que falta é acabamento, publicação e material de apresentação.

---

## [ENTREGA] — vale ponto direto na rubrica

Este bloco vem primeiro porque **é o único que vale nota por si só**. A rubrica dá 25 pontos à Sprint 2, e 10 deles dependem de coisas que ainda não existem.

> ### São dois vídeos, com finalidades diferentes
>
> Confirmado com o time. Não é contradição no material — são duas entregas:
>
> | | Duração | Quando | Para quê |
> |---|---|---|---|
> | **Seletiva** | até **5 min** | **13/09/2026** | Passar para a **banca final** (21/09) e, dela, para o **NEXT** (24/10). É um portão, não uma nota. |
> | **Pitch do portal** | até **3 min** | fim do ano, junto do repositório | É a **entrega avaliada**: vale os 5 pontos de Vídeo Pitch da rubrica, e é o vídeo mais básico dos dois. |
>
> **O que isso muda na prática:** o de 13/09 é o que corre contra o relógio e o que precisa mostrar o MVP funcionando de verdade — ele decide se existe banca. O de 3 minutos é feito depois, com calma, e é onde entram os critérios de duração, PDF e edição.

### E-1. ~~Publicar o frontend na Vercel~~ — **FEITO em 30/08/2026**

> ### ✅ No ar: https://merlin-route-finder.vercel.app
>
> Publicado a partir de `main`, raiz `frontend`, preset Vite, com `VITE_API_BASE_URL` apontando para o Render. **Verificado que a variável entrou na build**, e não só no painel: a URL do Render está embutida no `assets/index-*.js` servido.
>
> **A jornada inteira foi percorrida no ambiente publicado**, contra o backend real e o Gemini real:
>
> | Passo | Resultado |
> |---|---|
> | Sessão criada na entrada | `b6eb52c8-…`, posição "Placa ENT01" |
> | Catálogo com paginação | 50 de 111 → **100 de 111** num toque |
> | Adicionar ao roteiro | item nasce **com `idBackend`** — a correção do B-1 vale em produção |
> | **Prateleira vazia** | selo **"Sugestão do assistente"**, sugeriu `Lixa d'Água Grão 150` para a `Lixa para Parede Grão 120`, corredor Tintas, R$ 4,20, com justificativa coerente do Gemini |
> | Aceitar o substituto | lista trocou, item novo sincronizado |
> | Assistente no chat | respondeu citando o corredor **Tintas**, que existe na nossa planta |
> | Sem substituto plausível | testado com o adubo: recusou corretamente, e registrou o aviso |
>
> Os 5 pontos de deploy da rubrica deixam de estar zerados, e o PDF de entrega já tem o link que faltava.

**O texto abaixo fica como registro do que era o problema.**

O backend está publicado e responde (`https://merlin-route-finder-api.onrender.com`, verificado em 30/08: **200 em 109 s** de partida a frio). **O frontend não está publicado em lugar nenhum** — não há `vercel.json` nem projeto criado.

**Por que é grave:** a rubrica reserva **5 pontos** para o deploy (0,5 pelo link funcionando, 4,5 pela usabilidade do MVP publicado), e o PDF de entrega **exige o link do deploy**. Sem isso, os 5 pontos são zero e o PDF fica incompleto.

**O que fazer.** Criar o projeto na Vercel apontando para `frontend/`, com a variável `VITE_API_BASE_URL` valendo a URL pública do Render.

**Verificar:** abrir o link publicado num celular, fora do wi-fi de casa, e chegar até a tela do mapa.

**De quem:** João Ricardo (tem as credenciais) ou a dupla de frontend.

---

### E-2. ~~Preencher `CORS_ALLOWED_ORIGINS` no Render~~ — **FEITO em 30/08/2026**

> ✅ **Preenchido com as duas origens**, e as duas foram verificadas por requisição direta:
>
> ```
> Origin: https://merlin-route-finder.vercel.app  →  access-control-allow-origin: https://merlin-route-finder.vercel.app
> Origin: http://localhost:5173                  →  access-control-allow-origin: http://localhost:5173
> ```
>
> **O ambiente do time continua vivo** — era o risco principal deste card.
>
> **O texto abaixo fica como registro**, e continua valendo para quem for mexer nessa variável de novo.

Enquanto o frontend não estiver publicado, essa variável **tem que continuar vazia**: vazia, o backend libera `localhost:5173`, e é assim que o time desenvolve.

**O que fazer.** No painel do Render, `CORS_ALLOWED_ORIGINS` = a URL da Vercel, **sem barra no fim**. Salvar reinicia o serviço.

> **Cuidado que já custou tempo:** preencher com a URL de produção **remove** o localhost da lista e derruba o ambiente de todo mundo, com o sintoma clássico e enganoso — a API responde no Postman e o navegador bloqueia. Em 30/08 isso aconteceu por outro motivo (um servidor subiu na porta 5174) e todas as chamadas voltaram **403**.
>
> **Se o time ainda for desenvolver depois de publicar**, a variável aceita várias origens separadas por vírgula: pôr a da Vercel **e** `http://localhost:5173`.

**Verificar:** o app publicado carrega o catálogo, e o `localhost` do time continua funcionando.

**De quem:** João Ricardo.

---

### E-3. Escrever o roteiro do vídeo da seletiva — até 5 minutos, 13/09

**O que fazer.** Roteiro cena a cena, com o tempo de cada bloco. Sugestão de espinha, que segue o caminho que o sistema realmente faz:

1. o problema — o "último metro" numa loja de 10.000 m²;
2. entrada pela placa de QR, com o plano B de digitar o código;
3. busca e filtro por característica;
4. montar a lista e ver tudo no mapa;
5. **a prateleira vazia** — é a cena mais forte, e é a única que mostra a IA decidindo com dados reais;
6. encerramento.

> **Duas coisas que precisam entrar no roteiro como comportamento esperado, e não como acidente:**
>
> - **o assistente pode ficar indisponível ao vivo.** Já aconteceu duas vezes (503 do Google em 28/08). O sistema degrada com uma mensagem honesta, e mostrar isso é ponto a favor, não contra;
> - **a partida a frio leva ~110 segundos.** O primeiro passo do roteiro, antes de a câmera ligar, é aquecer a aplicação.

**Verificar:** roteiro lido em voz alta e cronometrado, **abaixo de 5 minutos** com folga — cronometrar lendo, não estimando.

**De quem:** time.

---

### E-4. Gravar e editar o vídeo da seletiva

**O que fazer.** Gravar seguindo o E-3, com a aplicação **aquecida** e o banco limpo (ver O-3).

**Por que a ordem importa:** este é o vídeo que decide se existe banca final. Se algo tiver que ser cortado por tempo, corte a introdução — nunca a cena da prateleira vazia, que é a única que mostra a IA decidindo com dados reais da loja.

**Verificar:** duração abaixo de 5 minutos; a demonstração mostra o MVP funcionando de verdade, não slides.

**De quem:** time.

---

### E-5. O vídeo do portal (até 3 min) e o PDF — a entrega avaliada

> **Não é para agora.** Fica registrado para não ser esquecido depois da seletiva. Sem data confirmada — vai junto do repositório, no fim do ano.

**O que fazer.** Duas peças que andam juntas na rubrica:

1. **o vídeo pitch de até 3 minutos** — mais básico que o da seletiva, e provavelmente uma redução dele;
2. **um PDF** com **nomes de todos os integrantes**, **link do vídeo** e **link do deploy**.

**O que a rubrica paga (5 pontos no total):** 0,5 pela duração dentro de 3 minutos · 0,5 pelo PDF completo · 1,0 por criatividade e edição · **3,0 por clareza na apresentação da solução e na demonstração** — de longe a maior linha, e a que depende do que já está construído.

**Verificar:** os dois links do PDF abrem de uma máquina que não é a de quem montou; o vídeo fecha abaixo de 3 minutos.

**De quem:** time.

---

## [FRONTEND] — acessibilidade, fechada em 30/08

> ### A-1, A-2 e A-3 — **FEITOS**
>
> Três correções que vieram da avaliação de interface, e que a rubrica paga (5 pontos de usabilidade no Projeto, 4,5 no Deploy):
>
> **A-1. O verde da marca parou de reprovar.** De **13 elementos reprovados para 1** na tela inicial — e esse é o ícone de estrela, decorativo, cujo mínimo é 3:1 e ele passa com 4,23. Catálogo, mapa, setores e chat: zero. Quatro tokens fizeram o trabalho, e o verde vivo continua pintando tudo. Ver [D-79](decisoes-tecnicas.md#d-79-o-texto-sobre-o-verde-da-marca-é-escuro-e-o-verde-que-carrega-texto-é-outro).
>
> **A-2. O movimento respeita quem pediu para parar.** As 14 animações infinitas param sob `prefers-reduced-motion`, e os esqueletos de carregamento **continuam visíveis** — o detalhe que a versão ingênua erra. Ver [D-80](decisoes-tecnicas.md#d-80-movimento-reduzido-usa-duração-de-001ms-e-não-animation-none).
>
> **A-3. Um erro deixou de derrubar o app para tela branca.** Testado quebrando um componente de propósito: a tela de recuperação aparece, e a pilha completa vai para o console — a barreira não engole o defeito.
>
> **Três achados de brinde, que só apareceram medindo:** o título do banner era cinza-escuro sobre verde escuro (**2,52:1**) desde antes, porque a regra global de `h1..h6` vence a herança de cor; seis lugares escreviam branco à mão, fora do alcance do token; e `--text-muted` reprovava em toda superfície tingida.
>
> **O que a auditoria achou e ficou de fora, com motivo:** [O-32](observacoes.md#o-32-a-paleta-de-cores-por-setor-reprova-no-contraste-e-é-decisão-de-identidade) (cores por setor — é identidade visual, decisão da dupla), [O-33](observacoes.md#o-33-oito-alvos-de-toque-abaixo-de-44px-e-o-projeto-define-esse-mínimo) (alvos de toque) e [O-34](observacoes.md#o-34-a-aba-do-qr-code-manda-apontar-uma-câmera-que-não-existe--corrigida) (o texto do QR).

## [FRONTEND] — acabamentos

Onze cards. **Nenhum é trabalho grande**, e o que eles têm em comum é que o backend já entrega a informação certa — falta a tela usá-la. Três pares compartilham causa, então são menos correções do que itens.

### F-1. ~~Recarregar a lista ao voltar para a aba~~ — **FEITO em 30/08/2026**

> ✅ Dois gatilhos, porque cobrem situações diferentes: `visibilitychange` para troca de aba e volta de outro aplicativo no celular, `focus` para troca de janela.
>
> **Verificado sem recarregar a página:** item adicionado, removido pelo servidor como se fosse a outra aba, e ao voltar o foco a lista foi de **1 para 0** — inclusive no armazenamento local. Fecha também o cenário "marcar item que outra aba removeu".

**O que fazer.** Um ouvinte de `visibilitychange` (ou `focus`) que recarrega o roteiro quando a aba volta a ficar visível.

**Por quê.** Não existe nenhum hoje. Duas abas abertas discordam entre si, e marcar um item que a outra aba já removeu deixa as duas em estados diferentes. O banco está seguro — o problema é só de tela.

**Verificar:** abrir o app em duas abas, remover um item numa, voltar para a outra: a lista se corrige sozinha.

---

### F-2. ~~Travar o botão de "prateleira vazia" durante a requisição~~ — **FEITO em 30/08/2026**

> ✅ E a correção óbvia não bastou. `disabled` mais guarda por `useState` deixaram passar **cinco** chamadas em cinco toques no mesmo tique: estado só chega ao próximo render. Com uma trava em `useRef`, síncrona, os mesmos cinco toques viram **uma**.
>
> Relatos separados por segundos continuam passando, e devem — duas visitas frustradas são dois dados para a loja.

**O que fazer.** Descer o estado de carregamento até o botão em `RoteiroDrawer.jsx`. O `App` já tem esse estado e já o passa para o modal.

**Por quê.** O botão tem um `disabled`, mas ele guarda outra coisa: impede tocar em item ainda não sincronizado. **O toque duplo continua passando**, e cada relato custa **duas chamadas ao Gemini** — com cota gratuita de cinco por minuto. Dois toques acidentais podem derrubar a sugestão seguinte para o modo de proximidade bem no meio da demonstração.

**Verificar:** tocar duas vezes rápido gera **um** registro de ruptura, não dois.

---

### F-3. ~~Avisar quando o código da placa não é reconhecido~~ — **FEITO em 30/08/2026**

> ✅ Entrou nos **dois** caminhos, não só no previsto: ao entrar pela URL **e** ao recentrar pelo modal. O segundo dizia *"Placa X registrada"*, que soava como sucesso enquanto a posição não mudava.
>
> **E um defeito maior apareceu no caminho:** o chip de localização tinha `ENT-01 / Entrada da Loja` como valor inicial fixo, então afirmava que o cliente estava na entrada antes de qualquer placa ser lida. A posição passa a nascer nula e o chip a dizer *"Ainda não sabemos onde você está"*.
>
> Verificado: `QQQ-77` avisa e o chip admite; `TIN-02` atualiza para "Corredor de Tintas".

**O que fazer.** Um `else` no `if (sess.posicaoAtual)` do `App.jsx`, com uma mensagem do tipo *"não encontramos essa localização; você pode continuar e tentar de novo depois"*.

**Por quê.** O backend cria a sessão **sem posição** quando o código é desconhecido, de propósito, e **não devolve erro** — então a tela não tem o que tratar e hoje não diz nada. O cliente digita errado, entra sem posição e não entende por que o mapa não mostra onde ele está.

**Verificar:** entrar com um código inventado mostra o aviso, e a sessão começa mesmo assim.

---

### F-4. ~~Avisar quando a sessão anterior expirou~~ — **FEITO em 30/08/2026**

> ✅ *"Sua sessão anterior expirou. Começamos uma nova."* A detecção não exigiu mexer no `sessionService`: o `App` guarda o id **antes** da chamada e compara com o que volta.

**O que fazer.** Uma frase — *"sua sessão anterior expirou, começamos uma nova"* — quando a sessão guardada não vale mais.

**Por quê.** A mecânica já está certa: o app consulta a sessão antes de usar e cria outra se ela não estiver ativa. Mas a troca é **silenciosa**: a lista anterior desaparece e o cliente não fica sabendo por quê.

**Verificar:** com uma sessão vencida no aparelho, abrir o app mostra o aviso antes da lista nova.

---

### F-5. ~~Oferecer o assistente quando a busca não acha nada~~ — **FEITO em 30/08/2026**

> ✅ As três saídas que já existiam eram formas de **refazer a mesma busca**; nenhuma de mudar de pergunta. "Perguntar ao assistente" entrou em destaque, acima delas. Verificado: "pergolado de bambu" chega ao chat em um toque.

**O que fazer.** Acrescentar um botão "perguntar ao assistente" ao estado vazio da busca, ao lado dos que já existem.

**Por quê.** O estado vazio é bom — diz o que foi procurado, em qual seção, e oferece três saídas. Mas nenhuma leva ao assistente, que é o recurso mais forte do sistema e o único capaz de responder *"o que eu uso para isso?"*. O beco deixou de ser sem saída; falta virar porta de entrada.

**Verificar:** procurar por algo que a loja não tem e chegar ao chat em um toque.

---

### F-6. ~~Botão de "tentar de novo" no chat~~ — **FEITO em 30/08/2026**

> ✅ Reenvia a última pergunta sem redigitar. Aparece **só** na mensagem que a tela gerou por não conseguir falar com a loja — decidido por um campo `falhou`, e não por interpretar o texto. Resposta que veio do servidor não ganha botão.
>
> Entrou junto um sexto `'Corredor Loja'` — grafia diferente, que o grep anterior não pegou.

**O que fazer.** Quando a mensagem falha, mostrar um botão que reenvia a mesma pergunta.

**Por quê.** A conversa já não parece corrompida — o assistente admite que não conseguiu falar com a loja. Falta o atalho: hoje o cliente precisa redigitar tudo.

**Verificar:** com a rede caída, enviar uma pergunta, restaurar a rede e tocar em "tentar de novo".

---

### F-7. ~~Avisar que o sistema está acordando~~ — **FEITO em 30/08/2026**

> ✅ Faixa no topo passando de quatro segundos na primeira carga: *"Preparando o sistema… a primeira abertura do dia leva até dois minutos."* Some no primeiro desfecho, sucesso ou falha.
>
> Verificado atrasando a resposta de propósito. A partida a frio foi medida hoje em **106 s e 109 s**.

**O que fazer.** Detectar a espera longa da primeira chamada e mostrar "preparando o sistema…" em vez de parecer travado.

**Por quê.** Medido em 30/08: a instância publicada leva **109 segundos** para acordar. O time decidiu seguir no plano gratuito e aquecer antes de apresentar — mas **isso vai acontecer com algum cliente**, e hoje a tela não diz nada durante quase dois minutos.

**Verificar:** abrir o app publicado depois de horas parado e ver a mensagem.

---

### F-8. ~~Reenviar as marcações feitas offline~~ — feito

> [!NOTE]
> **Concluído em 01/09/2026.** As marcações que não chegam ao servidor entram numa fila no
> aparelho e são reenviadas quando a conexão volta — com última-intenção-vence por item, parada
> no primeiro erro de rede para preservar a ordem, e descarte do que o servidor recusa.
>
> O defeito original era pior do que "não reenviava": a marca **ficava na tela** sem chegar ao
> servidor, e sumia sozinha na reconciliação seguinte. Ver [D-86](decisoes-tecnicas.md).

**O que era.**

**O que fazer.** Enfileirar as coletas marcadas sem rede e reenviá-las quando a conexão voltar.

**Por quê.** É o que resta do cenário "loja de 10.000 m², paredes de concreto". As outras duas saídas já estão feitas — a lista vive no aparelho e a tela avisa quando não consegue falar com a loja. Mas `alternarColetaItem` grava local e ignora a resposta: **o servidor fica para trás sem nada reenviar**.

**Verificar:** marcar itens com a rede desligada, religar, e conferir que o servidor recebeu.

---

### F-9. ~~Corrigir o convite do mapa vazio~~ — **FEITO em 30/08/2026**

> ✅ *"Adicione produtos e eles aparecem aqui no mapa, junto de onde você está."* Verificado: nenhuma das frases que prometiam rota aparece mais.

**O que fazer.** Trocar *"Adicione produtos na vitrine para traçar a melhor rota"* por uma frase que descreva o que o mapa faz: mostrar onde o cliente está e onde está cada item.

**Por quê.** "Traçar a melhor rota" é justamente o recurso que a mentoria da Leroy mandou tirar. A frase promete à banca o que decidimos não fazer. Cinco textos assim já saíram da tela; este sobreviveu porque só aparece com a lista vazia.

**Verificar:** abrir o mapa sem itens e ler a frase nova.

---

### F-10. ~~Remover os `'Corredor da Loja'`~~ — **FEITO em 30/08/2026**

> ✅ Eram **cinco**, não quatro. Nos cartões e no chat a linha some quando não há corredor; nas duas telas de detalhe, onde o campo tem rótulo próprio, vira *"Não informado"*.
>
> **O `App.jsx` que eu temia já estava protegido:** ali o corredor viraria a posição do cliente, mas um `if (posicaoAtual)` já barrava o caso. O que havia era um aviso de "adicionado ao roteiro" com o corredor genérico — esse sim corrigido.
>
> Sem regressão: os seis primeiros cartões do catálogo continuam mostrando Jardim, Ferramentas, Iluminação.

**O que fazer.** Em `App.jsx`, `ProductCard.jsx`, `ProductDetailModal.jsx` e `AIChatModal.jsx`, omitir a linha do corredor quando não houver corredor, em vez de escrever um texto genérico.

**Por quê.** Espaço em branco comunica ausência; texto genérico comunica presença, e mente. São da mesma família dos defeitos que já limpamos — um valor plausível no lugar de "não sei".

**Verificar:** um produto sem corredor não mostra linha de corredor nenhuma.

---

### F-11. ~~Reduzir o ruído da tela inicial~~ — **FEITO em 30/08/2026**

> ✅ **20 → 17 alvos de toque**, medidos em 375 px, com os cinco destinos da barra inferior intactos.
>
> **O botão flutuante não foi removido, e o plano estava errado.** A barra inferior é **escondida na página de produto** — remover o flutuante por completo deixaria o assistente inalcançável justamente na tela onde ele mais serve, que é onde ele recebe o produto como contexto. Ele passou a aparecer **só ali**. Verificado nas cinco telas: ausente onde a barra existe, presente onde ela não existe.
>
> **Saíram os dois cartões de "Ações Rápidas"** e, com eles, o componente `HomeBentoGrid` e **240 linhas de CSS órfão** — parte já estava morta desde que o `BentoActions` foi removido em 28/08 e ninguém tinha limpado.
>
> **O chip de localização** perdeu as coordenadas cruas (`(50, 92)`, que são saída de depuração), ganhou peso no nome do corredor e passou a dizer que é tocável — o que ele sempre foi, sem nada indicar.

<details><summary>O registro original</summary>

Três ajustes que andam juntos e cabem num card só:

1. **Vinte e dois alvos de toque na home**, com Mapa, Setores, Scan e Assistente aparecendo **duas vezes cada**. A barra inferior já resolve os quatro destinos — os cartões de "Ações Rápidas" podem sair.
2. **O chip de "você está aqui" está desenhado como enfeite**, com coordenadas cruas visíveis (`(50, 92)`). É a informação que separa este produto de um catálogo online: tirar as coordenadas, aumentar o peso do corredor, deixar claro que dá para tocar.
3. **O botão flutuante do assistente cobre conteúdo** em toda tela, e o assistente já está na barra inferior.

**Verificar:** contar os alvos de toque da home depois — a meta é ficar abaixo de 15, sem perder nenhum destino.

</details>

---

## [BANCO E DIAGRAMAS] — Vicentini

Três cards, e o primeiro é o mais urgente da lista inteira depois do E-1: **diagrama é o que a banca abre antes do código**.

### D-1. Atualizar o DER com a tabela de registro de ruptura

**O que fazer.** O DER não tem a tabela que registra os relatos de prateleira vazia, e ela existe no banco desde 25/08.

**Verificar:** o DER lista todas as tabelas que o Hibernate cria.

---

### D-2. Refazer o C4 para o produto que existe

**O que fazer.** O C4 atual mostra **dois containers de frontend** (Totem e Mobile PWA) e um fluxo de handoff. Hoje existe **um** frontend, e o handoff foi removido.

**Por quê.** Um avaliador que compare o C4 com a demonstração vê dois produtos diferentes, e a conclusão natural dele é que não entregamos o que projetamos — quando o que houve foi o contrário: mudamos porque a Leroy mandou, na mentoria de 24/08.

**Verificar:** o C4 mostra um frontend, o backend, o Oracle e o Gemini — e nada mais.

---

### D-3. Carimbar os outros três diagramas como Sprint 1

**O que fazer.** Casos de uso, classes e sequência ainda desenham totem, handoff e rota calculada. Refazer os três não cabe no prazo; **rotular** cada um como *"Sprint 1 — anterior à revisão de escopo de 24/08"* custa minutos e evita que sejam lidos como o produto atual.

**Verificar:** os três arquivos trazem o carimbo visível na primeira página.

---

## [TIME] — decisões e trabalho manual

### T-1. Decidir a planta da loja

**O que fazer.** Escolher entre levantar a planta real da loja de Interlagos ou assumir a atual como "loja de referência" e dizer isso à banca.

**Por quê.** Hoje o mapa é genérico dos dois lados, e funciona. É o único item do projeto que não mudou de estado desde 28/08, porque depende de decisão, não de código. **A segunda opção é defensável e custa zero** — a primeira exige levantamento que ninguém começou.

**Verificar:** decisão registrada, com data.

---

### T-2. Arte da placa de QR Code

**O que fazer.** A placa precisa dos **três** elementos: o QR Code, uma **URL curta e legível** e o **código de localização** (`TIN-02`).

**Por quê.** O QR é o único acesso ao sistema. Se o cliente não consegue escanear — câmera ruim, adesivo riscado — ele fica sem nada. O código curto sozinho não resolve: sem a URL impressa, não há onde digitá-lo. Os dois planos precisam existir juntos.

**Verificar:** uma placa impressa de teste, escaneada e digitada, leva ao app.

---

### T-3. ~~Coletar as 89 fotos de produto restantes~~ — feito

> [!NOTE]
> **Concluído em 01/09/2026.** O time coletou as 89 restantes, e com as 22 anteriores os
> **111 produtos do catálogo têm foto**. A coleta veio com o **nome real** de cada produto,
> que entrou junto com a marca correspondente — ver [D-85](decisoes-tecnicas.md).
>
> Dados em `docs/dados/produtos-imagens-t2.md`, incluindo os quatro SKUs em que o produto
> não existia na loja e foi substituído pelo mais próximo.

**O que fazia.** Continuar a coleta. **22 de 111 estavam coletadas e no ar.**

**Boa notícia que vale saber antes de começar:** acrescentar uma foto é **uma linha** no mapa `IMAGENS` do `CarregadorDadosIniciais.java`, e ela chega sozinha aos bancos que já existem. Preencher a tabela e avisar é suficiente.

**Prioridade:** os produtos que aparecem na tela durante a demonstração — em especial **as duas lixas**, que são as protagonistas da cena de ruptura e continuam sem foto.

**Verificar:** os produtos do roteiro do vídeo têm imagem.

---

### T-4. Escrever a resposta pronta sobre estoque sem ERP

**O que fazer.** Preparar a frase para a pergunta que a banca vai fazer: *"o estoque é real?"*.

**Por quê.** O estoque exibido é o do nosso banco, e só — não há integração com ERP, e não haveria como haver num projeto acadêmico. A resposta honesta e forte já existe no material: o sistema **trata a ruptura justamente porque o estoque nunca bate com a prateleira**, e é isso que a funcionalidade de substituto resolve.

**Verificar:** a frase cabe em 20 segundos e alguém do time consegue dizê-la sem ler.

---

## [BACKEND E OPERAÇÃO]

### O-1. Trocar a chave do Gemini

**O que fazer.** Gerar uma chave nova e usar só como variável de ambiente.

**Por quê.** A chave atual foi usada em linha de comando ao longo do desenvolvimento. A cota gratuita é de **5 requisições por minuto**, e uma chave conhecida por mais gente é uma cota disputada bem na hora da apresentação.

---

### O-2. Trocar a senha do Oracle

**O que fazer.** A senha atual passou por canal de conversa. Trocar no portal da FIAP e atualizar a variável no Render.

**Verificar:** o serviço publicado volta a responder depois da troca.

---

### O-3. Limpar as sessões de teste antes da banca

**O que fazer.** Apagar as sessões criadas por desenvolvimento e pela suíte, antes da gravação.

**Por quê.** Desenvolvimento, testes e demonstração usam **o mesmo schema** — a FIAP dá um por aluno e o orçamento é zero. Não incomoda no dia a dia, mas incomoda no dia da gravação.

> **Não é o caso de mexer no StrictMode.** A duplicação de sessão que se vê em desenvolvimento **não acontece em produção** — medido em 30/08. Ver O-30 nas observações.

---

## [LIMPEZA] — antes de enviar ao portal

### L-1. Remover os documentos de trabalho

**O que fazer.** `docs/perguntas-mentoria.md` e `docs/imagens-dos-produtos.md` servem à preparação, não ao produto. Devem sair antes de o repositório ir ao portal da faculdade.

**Verificar:** nenhum documento de trabalho no repositório entregue.

---

### L-2. ~~Resolver as duas pastas vazias~~ — feito

> [!NOTE]
> **Concluído em 01/09/2026.** As duas viraram índices que apontam para onde o conteúdo mora.
> Ver `O-29` em [`observacoes.md`](observacoes.md).

**O que era.**

**O que fazer.** `docs/arquitetura/` e `docs/casos_de_uso/` só têm `.gitkeep` desde 10/08. Apagar as duas, **ou** pôr em cada uma um arquivo curto apontando para onde o assunto está documentado.

**Por quê.** Pasta vazia num repositório avaliado sugere trabalho planejado e não feito — e aqui é o contrário: o conteúdo existe, só mora em outro lugar.

---

## O que **não** entra nesta lista, e por quê

Três coisas foram avaliadas e ficam de fora de propósito:

| | Por quê |
|---|---|
| **Renomear "roteiro" para "lista"** no código | O prazo era "antes de a integração começar", e ela começou e terminou. Renomear agora atinge entidades, endpoints, contrato, testes e tela, a duas semanas do vídeo, sem mudar nada que o cliente veja. Na **tela** vale unificar (está no F-11); no código, não |
| **Guarda contra a sessão duplicada** | Não é defeito: é o StrictMode em desenvolvimento, e não acontece em produção. Silenciá-lo perderia o sinal que ele existe para dar |
| **Swagger exposto, raio da ruptura, simulação de estoque sem proteção** | Limitações **aceitas conscientemente**, com o motivo registrado. Não são pendências — são respostas prontas para a banca |
