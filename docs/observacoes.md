# Merlin Route Finder — Observações em Aberto

> Coisas que alguém precisa **fazer, decidir ou saber**, e que não cabem no código.
>
> Complementa [`decisoes-tecnicas.md`](decisoes-tecnicas.md), que registra o oposto: decisões já **fechadas**, com o porquê de cada uma.
>
> **Para executar:** [`backlog-fechamento.md`](backlog-fechamento.md) transforma o que está aberto aqui em cards.
>
> Última atualização: **12/11/2026**. A seletiva, a banca e o NEXT já passaram; **só falta a entrega final no portal da faculdade**. Esta revisão tirou do texto tudo que servia às apresentações ao vivo e tudo que já estava resolvido. As encerradas ficam numa tabela no fim, e o texto completo de cada uma continua no histórico do git.

---

## Índice

| # | Observação | De quem | Urgência |
|---|---|---|---|
| [O-01](#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada) | Chave do Gemini e cota gratuita (afeta a suíte) | João Ricardo | **Alta — antes da entrega final** |
| [O-02](#o-02-senha-do-oracle-passou-por-canal-de-conversa) | Senha do Oracle exposta | João Ricardo | Média |
| [O-17](#o-17-documentos-de-trabalho-precisam-sair-antes-da-entrega-final) | Tirar o briefing do vídeo depois da gravação | Time | **fim do ano** |
| [O-20](#o-20-rodar-a-suíte-deixa-um-resto-de-sessões-no-banco-de-demonstração) | Limpar sessões de teste antes de gravar o vídeo | Backend | Média |
| [O-21](#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema) | Um schema só para tudo | Time | Média |
| [O-23](#o-23-processo-java-abandonado-trava-a-suíte-inteira-no-oracle) | Encerrar Maven e servidor local pela raiz | Backend | Média |
| [O-37](#o-37-quatro-famílias-tipográficas-seguram-a-primeira-pintura-por-quase-nove-segundos) | Quatro fontes seguram a primeira pintura | Frontend e time | Média |
| [O-22](#o-22-a-tela-inicial-do-catálogo-leva-24-segundos-no-ambiente-publicado) | Catálogo caiu de 2,4 s para 1,2 s; abrir sem filtro ainda é a tela mais lenta | Frontend | Baixa |
| [O-31](#o-31-o-assistente-não-nomeia-produtos-por-extenso-e-por-isso-nenhum-cartão-aparece) | Cartões do chat estão dormentes — **medido** | Backend, se o time quiser | Baixa |
| [O-36](#o-36-a-suíte-padrão-não-roda-os-testes-de-integração-e-isso-já-deixou-passar-oito-falhas) | A suíte padrão não roda os testes de integração | Time | Média |
| [O-10](#o-10-o-estoque-exibido-é-o-do-nosso-banco-e-só) | Estoque sem ERP — dizer no vídeo | Time | aceita |
| [O-12](#o-12-o-raio-de-busca-da-ruptura-é-um-palpite-informado--agora-medido) | Raio de 25 unidades — medido | — | aceita |
| [O-15](#o-15-o-endpoint-de-simulação-de-estoque-não-tem-proteção-nenhuma) | Simulação de estoque sem proteção | — | aceita |
| [O-30](#o-30-a-sessão-duplicada-na-abertura-é-do-strictmode-e-só-em-desenvolvimento) | Sessão duplicada é do StrictMode, **não é defeito** | — | esclarecida |

---

## Antes da entrega no portal

### O-01. Chave do Gemini precisa ser trocada, e a cota gratuita é apertada

**O quê.** Gerar uma chave nova no Google AI Studio e descartar a atual. E decidir se vale habilitar o faturamento no Google Cloud — o que **mantém o uso gratuito** e apenas eleva os limites.

**Por que importa.** Dois motivos somados:

1. A chave atual passou por conversa e teve a cota diária consumida durante o desenvolvimento — e a substituta, gerada em 24/08, passou pelo mesmo canal em 25/08. A troca continua pendente.
2. O tier gratuito limita **por dia e por minuto**. No modelo de topo são **20 requisições por dia** — uma única sessão de testes esgotou. Trocamos para o `gemini-3.5-flash-lite`, que tem folga maior e responde em ~1s, mas o limite por minuto continua valendo: durante os testes de integração da ruptura, o terceiro teste seguido **caiu no fallback determinístico** por causa dele.

O fallback funcionou como projetado e o cliente recebeu resposta útil. Mas, na gravação do vídeo, cair para "produto disponível mais próximo" justamente no momento mais forte da demonstração seria uma pena evitável.

**Isso já atrapalha a verificação, e não só a demonstração.** Confirmado em 25/08/2026: com `GEMINI_API_KEY` definida, a suíte de integração completa **não fecha verde de forma confiável** — os testes que conversam com o modelo falham em sequência e passam quando rodados isoladamente. Não é defeito do código; é o limite por minuto sendo atingido por uma suíte que dispara várias chamadas seguidas.

Enquanto a cota não subir, valem dois cuidados:

- a suíte **sem** `GEMINI_API_KEY` é determinística — os testes de IA são pulados e o resto fecha verde. É a forma de verificar um card;
- os testes de IA precisam ser rodados **por classe**, e não junto com o resto.

Habilitar o faturamento resolveria os dois problemas de uma vez.

**De quem.** João Ricardo. **Prazo que pressiona:** a gravação do vídeo da entrega final.

---

### O-02. Senha do Oracle passou por canal de conversa

**O quê.** Trocar a senha do usuário `rm563609` no Oracle da FIAP.

**Por que importa.** A credencial foi compartilhada por chat durante a verificação de conectividade. Ela nunca foi versionada: hoje vive no painel do Render e num `.env` na raiz do repositório, na máquina de desenvolvimento, coberto pelo `.gitignore`. Ainda assim, uma credencial que trafegou por um canal de conversa não deveria continuar valendo.

**Ao trocar, atualizar os dois lugares:** o painel do Render (`DB_PASSWORD`) e o `.env` local. Sem o segundo, a suíte de integração para de rodar nesta máquina.

**De quem.** João Ricardo (é a credencial pessoal dele; cada integrante usa a sua).

---

### O-17. Documentos de trabalho precisam sair antes da entrega final

**O quê.** Só falta um documento: o `prompt-video-apresentacao.md`, que é ferramenta para produzir o vídeo e **sai depois da gravação**.

**Já feito em 12/11/2026:**

| Documento | Destino |
|---|---|
| `progresso-migracao-2026-11-04.md` e `progresso-migracao-2026-11-05.md` | **Saíram.** Eram o diário de dois dias de trabalho; o que importa está nas decisões D-89 a D-96 |
| `plano-migracao-spring-para-jax-rs.md` e `requisitos-migracao-spring-para-jax-rs.md` | **Ficam.** As decisões D-89 a D-96 citam os requisitos pelo número (RF-04, RNF-02...), e os dois mostram como a troca foi verificada |
| `insomnia/base-antes.md` | **Fica**: traz a comparação de tempos que defende a D-96 |
| O protótipo do Stitch, as imagens do template do Vite, as pastas vazias e a configuração do editor | **Saíram** |

**O critério**, o mesmo da limpeza de 01/09/2026: **fica** o que explica o que o projeto é, por que ficou assim e como se sabe que funciona (`contexto-e-planejamento.md`, `planejamento-tecnico.md`, `decisoes-tecnicas.md`, os READMEs, `dados/`, os diagramas). **Sai** o que estava amarrado a um momento do trabalho.

**Ao apagar,** procurar os links que apontam para cada arquivo (`grep -rn` pelo nome) e redirecioná-los ou reescrevê-los.

**De quem.** Time. **Quando:** antes do envio ao portal.

---

### O-20. Rodar a suíte deixa um resto de sessões no banco de demonstração

> [!NOTE]
> **Corrigida em 25/08/2026.** A versão original afirmava que a limpeza de vários testes falhava e que sessões órfãs iam se acumulando. **Isso foi escrito sem verificação, e a medição não sustenta.** Os testes que geram ruptura já apagavam os registros antes da sessão; o erro observado veio de um teste que falhou no meio, não do caminho normal.

**O que a medição mostra.** Depois de um dia inteiro de execuções da suíte completa, o schema tinha **23 sessões, nenhuma lista órfã e nenhuma sessão `ACTIVE` vencida**. A limpeza funciona e a varredura de TTL faz o resto.

**O que sobra, e por que importa mesmo sendo pouco.** Das 23, **17 estão `ABANDONED`** — sessões que vazaram de execuções interrompidas e que a varredura classificou como carrinho abandonado, exatamente como faria com um cliente real.

`ABANDONED` é a métrica de negócio que o projeto usa para dizer que uma venda quase aconteceu ([D-42](decisoes-tecnicas.md#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem)). **Misturada com resíduo de teste, ela deixa de significar o que promete** — e é justamente o tipo de número que se cita numa banca.

**O que fazer.** Nada urgente no código. Antes da gravação do vídeo, vale **limpar as sessões de teste** para que qualquer número citado seja verdade. Um `delete` das sessões finalizadas resolve, respeitando a ordem das chaves estrangeiras: registros de ruptura, mensagens de chat, itens, lista, sessão.

**De quem.** Backend. **Urgência:** baixa até a véspera da gravação.

**Aumentou em 12/11/2026:** a coleção de paridade, a suíte de integração e as conferências em produção criaram e concluíram mais sessões de teste no schema. Antes de gravar o vídeo final, vale rodar a limpeza.

---

### O-21. Desenvolvimento, testes e demonstração usam o mesmo schema

**O quê.** Não existe separação entre "banco de desenvolvimento" e "banco de produção": a instância publicada no Render e a suíte de testes de integração apontam para o **mesmo schema Oracle da FIAP**.

**Como isso ficou evidente.** Ao verificar o deploy em 25/08, a API publicada respondeu com **111 produtos** — embora o código que ela roda declare 29. Ela estava lendo a massa que as execuções da suíte tinham gravado.

**Por que é estrutural, e não descuido.** A FIAP dá **um schema por aluno**. Criar um segundo banco exigiria outro provedor, e a restrição de custo zero fecha essa porta.

**O que de fato pode dar errado.** Menos do que parece, e vale ser preciso:

- **estoque** — o teste que zera a trena restaura em `finally`, e isso está no código desde o começo;
- **sessões** — sobra um resto, tratado na [O-20](#o-20-rodar-a-suíte-deixa-um-resto-de-sessões-no-banco-de-demonstração);
- **catálogo** — a carga é incremental e idempotente, e produtos que ela não conhece são deixados em paz.

**O risco real é de momento, não de dados.** Rodar a suíte completa **durante** a gravação do vídeo mexeria na massa que está sendo demonstrada — e a partida a frio de 176 segundos já torna a instância sensível a qualquer sobressalto.

**O que fazer.** Combinar uma regra simples: **ninguém roda a suíte de integração enquanto alguém estiver gravando.** Não custa nada e elimina o único cenário que machuca.

**De quem.** Time. **Urgência:** média — vale para a gravação do vídeo final.

---

### O-23. Processo Java abandonado trava a suíte inteira no Oracle

**O quê.** Duas maneiras de deixar JVM órfã segurando transação aberta no schema compartilhado. As duas custaram tempo em 28/08, e as duas voltam a acontecer sozinhas.

**Parar o Maven não para o Surefire.** Interromper `mvnw test` mata o processo do Maven, mas o **JVM filho que roda os testes sobrevive** — e sobrevive no meio de uma transação. Ele vira uma sessão `INACTIVE` que nunca faz commit nem rollback, e qualquer execução seguinte que toque as mesmas linhas fica presa esperando.

Foi exatamente isso: uma sessão ociosa bloqueando outra, que bloqueava mais duas. A suíte ficou **20 minutos parada** sem sair do lugar e sem falhar — o pior formato de erro, porque não avisa.

**O servidor de desenvolvimento recarregava sozinho** (vale para a versão com Spring; o DevTools saiu na migração, mas a carga inicial continua rodando a cada subida da `Main`). Com o `spring-boot:run` aberto, o DevTools reinicia a aplicação a cada compilação — e **cada reinício executa a carga inicial**. Numa dessas, a planta já estava acentuada e a migração de renomeação ainda não existia: a carga criou as quatro seções novas vazias, e o banco ficou com a seção duplicada. Ver [D-70](decisoes-tecnicas.md#d-70-renomear-uma-seção-é-migração-não-edição-de-string), que passou a apagar a prateleira vazia justamente por causa disso.

**Por que dói aqui e não doeria em outro projeto.** Porque o schema é um só para tudo ([O-21](#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema)). Num banco local descartável, um lock preso se resolve derrubando o banco. No da FIAP, **não temos privilégio para derrubar sessão** — `alter system kill session` responde `ORA-01031`. Só resta esperar o Oracle colher a conexão morta, o que levou cerca de meia hora.

**O que fazer.**

- **Não deixar o servidor local subindo e descendo enquanto edita o backend.** Cada subida da `Main` executa a carga inicial contra o banco de verdade, com a versão do código daquele momento. Para medir ou depurar sem isso, `MERLIN_SEED_ENABLED=false`.
- **Ao interromper a suíte, conferir se sobrou JVM.** No Windows: `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` mostra o horário de início e a linha de comando — as do Surefire aparecem com `-jar ...\surefire`. Encerrar essas pela raiz libera o lock na hora.
- **Não confundir com falha de teste.** Suíte parada sem sair do lugar por mais de dois minutos é lock, não lentidão. `select sid, blocking_session from v$session where username = user` responde em um segundo.

**De quem.** Backend. **Urgência:** média — vale lembrar na véspera de qualquer gravação.

---

## Acabamentos de código, se houver tempo

### O-37. Quatro famílias tipográficas seguram a primeira pintura por quase nove segundos

**Medido em 01/09/2026** com o Lighthouse 12.8 contra o ambiente publicado, perfil de celular:

| | |
|---|---|
| First Contentful Paint | **8,6 s** |
| Largest Contentful Paint | 8,8 s |
| **Total Blocking Time** | **0 ms** |
| Nota de performance | **56** |

**O bloqueio de zero milissegundo é o que aponta o culpado.** Não é JavaScript: nada trava a
linha principal. É a folha de estilo das fontes, que bloqueia a pintura enquanto baixa.

O `index.html` carrega **quatro famílias tipográficas** — Hanken Grotesk, IBM Plex Sans,
Source Sans 3 e Work Sans — mais os Material Symbols. O Lighthouse estima cerca de **7 s** de
bloqueio somado no perfil de celular.

**O que já foi feito, e não resolve a raiz.** As duas requisições ao Google Fonts viraram uma
só, tirando uma ida e volta do caminho crítico sem mudar nada na tela.

**O que resolveria.** Usar menos famílias. Quatro tipografias num app de uma tela e meia é
muito, e cada uma custa uma fonte para baixar antes de o cliente ver qualquer coisa — dentro
de uma loja, em dado móvel.

**Por que não fiz.** Escolha de tipografia é identidade visual, e é da dupla de frontend. O
caminho barato é ver quais famílias aparecem em poucos lugares e realocá-las para as que já
carregam.

**Atenuante:** a medição usa a rede móvel lenta simulada do Lighthouse. Numa conexão boa o
número é bem melhor. Mas o app é **feito para ser usado dentro de uma loja**, que é
exatamente onde a rede é ruim.

**De quem.** Frontend, com o time.

---

### O-22. A tela inicial do catálogo leva 2,4 segundos no ambiente publicado

> [!NOTE]
> **Remedida em 12/11/2026, depois da D-96** (conexões reaproveitadas), cinco amostras com a instância quente:
>
> | Endpoint | 25/08 (Spring) | 12/11 (JAX-RS + D-96) |
> |---|---|---|
> | `GET /produtos?page=0&size=50` (o que a tela inicial pede) | ~2.421 ms | **1.221 ms** |
> | `GET /produtos?secao=Tintas` | 814 ms | 1.052 ms |
> | `GET /produtos/secoes` | 355 ms | 562 ms |
> | `GET /mapa` | 669 ms | 628 ms |
>
> A tela inicial ficou duas vezes mais rápida e continua sendo a mais lenta. Os itens 1 e 2 abaixo continuam abertos: o app ainda abre pedindo o catálogo sem filtro.

**Medido em 25/08/2026** contra a instância no Render, já quente, sete amostras por endpoint:

| Endpoint | Mediana | Resposta |
|---|---|---|
| `GET /produtos` (catálogo sem filtro) | **2.421 ms** | 15,3 KB |
| `GET /produtos?size=100` | 2.946 ms | 40,0 KB |
| `GET /produtos?atributo=MARCA:Tigre` | 1.099 ms | 11,3 KB |
| `GET /produtos?query=tinta` | 931 ms | 1,7 KB |
| `GET /produtos?secao=Tintas` | **814 ms** | 5,6 KB |
| `GET /mapa` | 669 ms | 1,9 KB |
| `GET /produtos/{id}` | 509 ms | 0,6 KB |
| `GET /produtos/secoes` | **355 ms** | 0,5 KB |

**As consultas não são o problema.** Medidas direto no Oracle, as três que compõem `GET /produtos` somam **240 ms**: conteúdo 90 ms, contagem 30 ms, facetas 120 ms. A busca com `UTL_MATCH` custa 31 ms.

Ou seja: **240 ms de banco viram 2.421 ms de resposta.** Cerca de 90% da latência é o ambiente — 0,1 CPU serializando JSON e mapeando entidades, mais o trânsito entre o Render (Estados Unidos) e o Oracle (Brasil) a cada consulta.

**Otimizar SQL aqui não renderia nada.** É importante dizer isso porque é o instinto errado óbvio: mexer no índice ou reescrever a consulta atacaria os 10%.

### O que dá para fazer, em ordem de retorno

**1. Não abrir na tela do catálogo sem filtro — é frontend, custa zero e vale 3×.**

A tela de entrada é a mais lenta de todas, e é a única de 2,4 segundos. Abrir pelo **menu de seções** (355 ms) e só então mostrar produtos de uma seção (814 ms) dá ao cliente duas telas rápidas em vez de uma lenta. É como um e-commerce de material de construção funciona de qualquer forma: ninguém navega 111 produtos sem categoria.

**2. Guardar o mapa no aparelho.** `GET /mapa` não depende de sessão justamente para isso ([D-58](decisoes-tecnicas.md#d-58-a-planta-da-loja-não-vive-no-banco-e-é-dela-que-saem-as-coordenadas-das-seções)). Buscar uma vez e reusar economiza 669 ms a cada abertura do mapa, que é a tela central do produto.

**3. Cache do mapa no backend** — não feito, e vale discutir antes. Os pontos só mudam quando a carga roda, no startup, então guardar a resposta em memória seria seguro. Economiza os 669 ms para quem não guardou no aparelho. É código novo com uma premissa declarada, e por isso não entrou sem conversa.

**O que não vale.** Aumentar o plano do Render resolveria de vez, e o time já decidiu ficar no gratuito. Vale lembrar que **a partida a frio (163–176 s) continua sendo o número dominante** para qualquer demonstração: 2,4 segundos incomoda, três minutos inviabiliza.

**De quem.** Frontend (itens 1 e 2), time (item 3). **Urgência:** baixa depois da D-96, mas a tela lenta continua sendo a primeira que o avaliador vê.

---

### O-31. O assistente não nomeia produtos por extenso, e por isso nenhum cartão aparece

**O quê.** Uma decisão em aberto para o time, não um defeito. O chat pode mostrar cartões dos produtos que o assistente citou, mas na prática **nenhum aparece** — e isso agora está medido, não suposto.

**A medição, no ambiente publicado, com o Gemini real** (30/08/2026). Pergunta: *"O que eu preciso para pintar uma parede?"*. Resposta:

> *"Para pintar uma parede, você precisará de tinta acrílica e lixa para parede, encontradas no corredor de Tintas. Também recomendamos o uso de rolo de lã, bandeja para pintura e fita crepe, todos disponíveis no mesmo corredor de Tintas."*

**Cartões exibidos: zero.** A resposta é boa e está ancorada — cita o corredor **Tintas**, que existe na nossa planta. Mas nomeia os produtos **genericamente** ("tinta acrílica", "rolo de lã"), e a regra da [D-76](decisoes-tecnicas.md#d-76-o-cartão-de-produto-no-chat-exige-que-a-ia-tenha-escrito-o-nome) exige nome completo ou SKU.

**Isso é o comportamento projetado, e a D-76 previu exatamente este resultado**: *"se a resposta não nomear nenhum produto por extenso, o certo é não mostrar cartão nenhum. Não afrouxar a regra para fazê-los aparecer."*

**A decisão que fica para o time.** Duas saídas honestas, e nenhuma é urgente:

1. **deixar como está** — respostas em texto, sem cartões. É o mais simples e não mente;
2. **pedir ao assistente que cite os produtos pelo nome completo**, ajustando o prompt no backend. Aí os cartões passam a aparecer, com a mesma regra estrita. É card de backend, pequeno, e melhora a demonstração.

**Não é saída:** afrouxar a regra do cartão. Foi o que existia antes, e pendurava produto que a IA nunca recomendou.

---

### O-36. A suíte padrão não roda os testes de integração, e isso já deixou passar oito falhas

> **Números de 12/11/2026:** `mvnw test` roda **208** testes; com `-Pintegracao`, **365**, todos
> passando (8 pulados sem `GEMINI_API_KEY`). O JaCoCo, citado em outros documentos como quem revelou
> isto, saiu do projeto (D-95). O resto do texto continua valendo: a suíte padrão ainda não avisa o
> que deixou de rodar.

**O quê.** `mvnw test` roda **150 testes**. A suíte inteira tem **285**. Os 19 testes de
integração ficam de fora por padrão — exigem `-Pintegracao` e credenciais do Oracle, e a
exclusão está declarada no `pom` de propósito, porque nem todo ambiente tem banco.

**A decisão é boa. O risco é o que ela não diz.** Quem roda `mvnw test`, vê "150 testes,
0 falhas" e conclui que está tudo certo, está errado — e não tem como saber disso pela saída.

**Já aconteceu.** O commit do [D-85](decisoes-tecnicas.md) foi dado como verificado com "150
testes passam". Rodando a suíte completa, **8 falhavam** — inclusive a que garante que o
substituto oferecido na demonstração faz sentido. Nenhuma delas era visível pela suíte padrão.

**O que reduz o risco, em ordem de esforço:**

1. **Combinar no time**: nenhum commit que toque a massa de dados vai sem `-Pintegracao`. Custo
   zero, depende de disciplina.
2. **Fazer a suíte padrão dizer o que não rodou** — uma linha no fim do `mvnw test` avisando
   que 19 testes ficaram de fora e como rodá-los. É o mesmo princípio que o app inteiro segue:
   silêncio sobre o que não se sabe é pior que o aviso.
3. **Integração contínua** rodando o perfil completo a cada envio. É o certo, e é o único que
   não depende de ninguém lembrar.

**De quem.** Time.

---

## Limitações aceitas conscientemente

### O-10. O estoque exibido é o do nosso banco, e só

Não há integração com ERP ou WMS — não temos acesso a sistema real da Leroy Merlin, e a restrição de custo zero inviabiliza qualquer intermediário. **Vale dizer isso no vídeo**, porque a resposta é forte: o sistema resolve o problema de *localizar* o produto, e o fluxo de ruptura física existe justamente por assumir que a divergência entre sistema e prateleira **vai** acontecer — em vez de fingir que o estoque é confiável. Ver [D-23](decisoes-tecnicas.md#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp).

---

### O-12. O raio de busca da ruptura é um palpite informado — agora medido

As 25 unidades do grid 0–100 foram escolhidas por julgamento: é a ordem de grandeza de um desvio que um cliente aceita fazer a pé.

**Medido em 25/08/2026, com o catálogo de 111 produtos**, o alcance por seção é bem desigual:

| Seção | Produtos vizinhos ao alcance | Corredores alcançados |
|---|---|---|
| Elétrica, Ferragens, Jardim | 45–46 | 4 |
| Encanamento | 32 | 3 |
| Cozinhas, Ferramentas, Tintas | 22–23 | 2 |
| Iluminação | 10 | 1 |
| Decoração, Materiais de construção | **0** | **0** |

**A boa notícia é que a preocupação original se dissolveu sozinha.** A versão anterior desta observação dizia que produtos isolados — o espelho de Decoração era o exemplo — ficavam sem vizinho e devolviam 422. Com o catálogo maior, **Decoração tem dez produtos próprios**, e a busca não exclui a própria seção: um espelho em falta agora tem nove candidatos plausíveis. **Não há mais beco sem saída.**

**Por que o raio fica em 25 mesmo assim.** Aumentá-lo faria Elétrica alcançar quase a loja inteira, e "perto" deixaria de significar perto. O teto de 20 candidatos ordenados por distância ([D-67](decisoes-tecnicas.md#d-67-o-teto-de-candidatos-da-ruptura-envelheceu-com-o-catalogo)) já garante que os mais próximos venham primeiro — e sem nenhum beco sem saída para corrigir, mexer no valor seria mudar código sem causa.

---

### O-15. O endpoint de simulação de estoque não tem proteção nenhuma

`PATCH /api/v1/produtos/{produtoId}/estoque` altera o catálogo e **qualquer pessoa com a URL da API publicada consegue chamar**. É o único endpoint que escreve no catálogo e o de maior impacto do sistema.

Aceito por três motivos: a API não tem autenticação em endpoint nenhum, então ele não abre uma categoria nova de exposição; desligá-lo em produção derrotaria o propósito, já que é justamente o ambiente publicado que será demonstrado; e o dano é reversível pelo próprio endpoint, sobre massa de demonstração.

O que existe no lugar de proteção: marcação explícita como demonstração no contrato OpenAPI e uma linha de log com `AVISO:` a cada alteração. **Se o projeto ganhar autenticação em algum momento, este endpoint é o primeiro que precisa entrar atrás dela.** Ver [D-40](decisoes-tecnicas.md#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido).

---

### O-30. A sessão duplicada na abertura é do StrictMode, e só em desenvolvimento

**O quê.** Fica registrado para ninguém reabrir: **abrir o app não cria duas sessões em produção.** Cria uma.

**Por que este item existe.** A observação circulou duas vezes como defeito grave — "cada abertura do app cria duas sessões, e a órfã fica no banco de demonstração". **É falso**, e a correção veio de medir a build de produção em vez da de desenvolvimento.

**O que foi medido em 30/08/2026**, servindo o `dist` na mesma porta e repetindo o teste:

| Cenário | Desenvolvimento | Produção |
|---|---|---|
| Primeira abertura, armazenamento limpo | 2 × `POST /sessoes` | **1 × `POST`** |
| Recarga com sessão guardada | trio de `GET` repetido 4× | **1 `GET` sessão + 1 `GET` roteiro** |

**A causa é o React StrictMode**, que invoca cada efeito duas vezes **apenas em desenvolvimento**, de propósito, para revelar efeitos não idempotentes. A demonstração e o ambiente publicado rodam a build de produção, onde isso não acontece.

**Por que não pôr uma guarda para silenciar.** Seria trocar um custo pequeno por perder um sinal: o StrictMode existe justamente para denunciar efeito que cria recurso. O custo real — cada recarga em desenvolvimento gasta duas sessões no schema compartilhado — já está coberto pela [O-20](#o-20-rodar-a-suíte-deixa-um-resto-de-sessões-no-banco-de-demonstração), que manda limpar antes da banca.

**Lição de método, e é o motivo de isto ficar escrito.** A versão de 28/08 deste registro trazia a ressalva certa — *"a compilação de produção não faria isso"*. Ela foi **perdida numa reescrita** e o item voltou à lista como grave. Medir de novo antes de planejar é o que impediu o card de existir.

---

## Encerradas

Resolvidas, sem objeto ou ligadas às apresentações ao vivo, que já aconteceram. O texto completo de cada uma está no histórico do git deste arquivo.

| # | Desfecho |
|---|---|
| <a id="o-03-o-deploy-ainda-não-foi-feito--no-ar-e-verificado"></a>O-03 | Deploy no ar e verificado (25/08/2026). |
| <a id="o-19-a-entrada-tem-um-plano-b-e-ele-é-uma-tela-que-ainda-não-existe"></a>O-19 | Tela de código manual feita; a arte da placa era para as apresentações ao vivo. |
| <a id="o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade--resolvida-na-tela"></a>O-04 | O selo da ruptura distingue sugestão da IA de proximidade. |
| <a id="o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição--resolvida-com-trava-síncrona"></a>O-05 | O botão de prateleira vazia trava durante a requisição. |
| <a id="o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar--já-existia-e-foi-verificada"></a>O-06 | O celular retoma a sessão se a aba fechar. |
| <a id="o-07-ponto-de-interesse-some-se-a-página-recarregar--sem-objeto"></a>O-07 | Sem objeto: o ponto de interesse saiu com o totem. |
| <a id="o-25-o-frontend-carrega-um-catálogo-inventado-e-o-mostra-quando-a-api-cai--apagado"></a>O-25 | O catálogo inventado do frontend foi apagado. |
| <a id="o-26-o-convite-do-mapa-vazio-promete-uma-rota-que-não-existe--corrigido"></a>O-26 | O texto do mapa vazio foi corrigido. |
| <a id="o-27-as-22-imagens-coletadas-não-têm-para-onde-ir--medição-errada-o-mecanismo-já-existia"></a>O-27 | As imagens coletadas já estavam aplicadas. |
| <a id="o-28-a-visão-todos-do-catálogo-não-pagina--os-111-passaram-a-ser-alcançáveis"></a>O-28 | O catálogo pagina; os 111 são alcançáveis. |
| <a id="o-08-o-der-não-tem-a-tabela-de-registro-de-ruptura--resolvida-em-markdown"></a>O-08 | DER vigente em `dados/der-atualizado.md`. |
| <a id="o-16-o-token-continua-na-url-do-pwa--a-preocupação-deixou-de-existir"></a>O-16 | Sem objeto: não há mais token na URL. |
| <a id="o-24-os-diagramas-da-sprint-1-desenham-o-produto-que-a-mentoria-derrubou--resolvida-em-markdown"></a>O-24 | C4 e DER vigentes em Markdown; PDFs rotulados como Sprint 1. |
| <a id="o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens"></a>O-18 | Catálogo com 111 produtos, todos com foto. |
| <a id="o-14-a-massa-de-dados-só-tem-um-par-de-substitutos-que-faz-sentido--resolvida"></a>O-14 | A massa tem cinco pares de substituição, em cinco seções. |
| <a id="o-09-a-rota-sempre-parte-do-primeiro-totem-encontrado--sem-objeto"></a>O-09 | Sem objeto: não há mais totem nem rota calculada. |
| <a id="o-11-o-swagger-fica-exposto-no-ambiente-publicado--sem-objeto"></a>O-11 | Sem objeto: o Swagger saiu com o Spring (05/11/2026). |
| <a id="o-29-duas-pastas-de-documentação-estão-vazias-desde-o-primeiro-dia--viraram-índices"></a>O-29 | As pastas vazias viraram índices. |
| <a id="o-32-a-paleta-de-cores-por-setor-reprova-no-contraste--três-tons-escurecidos"></a>O-32 | A paleta por setor passou no contraste. |
| <a id="o-33-oito-alvos-de-toque-abaixo-de-44px--corrigidos"></a>O-33 | Alvos de toque com 44 px. |
| <a id="o-34-a-aba-do-qr-code-manda-apontar-uma-câmera-que-não-existe--corrigida"></a>O-34 | O texto da aba do QR Code foi corrigido. |
| <a id="o-35-a-afinidade-empata-quando-muitos-produtos-dividem-o-tipo-e-o-desempate-é-alfabético"></a>O-35 | Resolvida em 12/11/2026: entre produtos do mesmo tipo, o desempate é pelas características em comum, e não pelo nome (D-99). |
| <a id="o-38-o-lint-do-frontend-acusa-37-problemas"></a>O-38 | Resolvida em 12/11/2026: `npm run lint` sem nenhum problema. |
| <a id="o-13-a-fase-3-inteira-continua-planejada-e-não-feita--superada-pela-virada-de-escopo"></a>O-13 | Sem objeto: a Fase 3 foi superada pela virada de escopo. |

---

## Como manter este documento

Sempre que uma observação **for resolvida**, tire o texto dela daqui, ponha uma linha na tabela de encerradas e, se ela virou uma decisão, registre-a em [`decisoes-tecnicas.md`](decisoes-tecnicas.md). Este documento só é útil enquanto tudo nele estiver de fato em aberto.
