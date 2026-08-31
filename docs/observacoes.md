# Merlin Route Finder — Observações em Aberto

> Coisas que alguém precisa **fazer, decidir ou saber**, e que não cabem no código.
>
> Complementa [`decisoes-tecnicas.md`](decisoes-tecnicas.md), que registra o oposto: decisões já **fechadas**, com o porquê de cada uma. Ali fica o que foi resolvido; aqui, o que ainda está aberto — pendências, limitações aceitas conscientemente e avisos que precisam chegar a quem não escreveu aquele trecho.
>
> Cada item traz: **o quê**, **por que importa**, **de quem é** e o **prazo** que o pressiona, quando há.
>
> **Para executar**, e não só entender: [`backlog-fechamento.md`](backlog-fechamento.md) transforma tudo que está aberto aqui — mais o que não é observação, como a publicação do frontend e o vídeo — em cards prontos para o Trello.
>
> Última atualização: **30/08/2026** — depois de fechar dez dos onze acabamentos de tela. Das trinta quebras de fluxo, **27 estão resolvidas**; a única de tela que resta é reenviar as marcações feitas offline, que é funcionalidade nova e não acabamento.

---

## Índice

| # | Observação | De quem | Urgência |
|---|---|---|---|
| [O-01](#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada) | Chave do Gemini e cota gratuita (afeta a suíte) | João Ricardo | **Alta — 13/09** |
| [O-02](#o-02-senha-do-oracle-passou-por-canal-de-conversa) | Senha do Oracle exposta | João Ricardo | Média |
| [O-06](#o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar) | Retomar a sessão pelo `localStorage` | Bielecky e Marcela | Alta |
| [O-08](#o-08-o-der-não-tem-a-tabela-de-registro-de-ruptura) | DER desatualizado | Vicentini | **Alta** |
| [O-24](#o-24-os-diagramas-da-sprint-1-desenham-o-produto-que-a-mentoria-derrubou) | Diagramas desenham totem, handoff e rota | Vicentini e time | **Alta — antes da banca** |
| [O-29](#o-29-duas-pastas-de-documentação-estão-vazias-desde-o-primeiro-dia) | `docs/arquitetura` e `docs/casos_de_uso` vazias | Time | Baixa |
| [O-30](#o-30-a-sessão-duplicada-na-abertura-é-do-strictmode-e-só-em-desenvolvimento) | Sessão duplicada é do StrictMode, **não é defeito** | — | esclarecida |
| [O-31](#o-31-o-assistente-não-nomeia-produtos-por-extenso-e-por-isso-nenhum-cartão-aparece) | Cartões do chat estão dormentes — **medido** | Backend, se o time quiser | Baixa |
| [O-32](#o-32-a-paleta-de-cores-por-setor-reprova-no-contraste-e-é-decisão-de-identidade) | Cores por setor reprovam — **medidas** | Frontend e time | Média |
| [O-33](#o-33-oito-alvos-de-toque-abaixo-de-44px-e-o-projeto-define-esse-mínimo) | Alvos de toque abaixo de 44px | Frontend | Média |
| [O-19](#o-19-a-entrada-tem-um-plano-b-e-ele-é-uma-tela-que-ainda-não-existe) | ~~Tela de código manual~~ — **feita**; falta a **arte da placa** | Time | Alta |
| [O-18](#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens) | Coletar as imagens dos produtos | Time | Média |
| [O-10](#o-10-o-estoque-exibido-é-o-do-nosso-banco-e-só) | Estoque sem ERP — argumento de banca | Time (discurso) | Média |
| [O-17](#o-17-documentos-de-trabalho-precisam-sair-antes-da-entrega-final) | Limpar documentos de trabalho | Time | fim do ano |
| [O-20](#o-20-rodar-a-suíte-deixa-um-resto-de-sessões-no-banco-de-demonstração) | Limpar sessões de teste antes da banca | Backend | Baixa |
| [O-21](#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema) | Um schema só para tudo | Time | Média |
| [O-22](#o-22-a-tela-inicial-do-catálogo-leva-24-segundos-no-ambiente-publicado) | Catálogo leva 2,4 s — a **espera longa** já avisa | Time | Baixa |
| [O-23](#o-23-processo-java-abandonado-trava-a-suíte-inteira-no-oracle) | Encerrar servidor e Maven pela raiz | Backend | Alta |
| [O-11](#o-11-o-swagger-fica-exposto-no-ambiente-publicado) | Swagger em produção | — | aceita |
| [O-12](#o-12-o-raio-de-busca-da-ruptura-é-um-palpite-informado--agora-medido) | Raio de 25 unidades — medido | — | aceita |
| [O-15](#o-15-o-endpoint-de-simulação-de-estoque-não-tem-proteção-nenhuma) | Simulação de estoque sem proteção | — | aceita |

**Encerradas:** [O-03](#o-03-o-deploy-ainda-não-foi-feito--no-ar-e-verificado) (deploy no ar) · [O-04](#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade--resolvida-na-tela) (selo da ruptura) · [O-27](#o-27-as-22-imagens-coletadas-não-têm-para-onde-ir--medição-errada-o-mecanismo-já-existia) (imagens já aplicadas) · [O-25](#o-25-o-frontend-carrega-um-catálogo-inventado-e-o-mostra-quando-a-api-cai--apagado) (catálogo inventado apagado) · [O-28](#o-28-a-visão-todos-do-catálogo-não-pagina--os-111-passaram-a-ser-alcançáveis) (catálogo pagina) · [O-26](#o-26-o-convite-do-mapa-vazio-promete-uma-rota-que-não-existe--corrigido) (texto do mapa) · [O-05](#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição--resolvida-com-trava-síncrona) (trava da ruptura) · [O-34](#o-34-a-aba-do-qr-code-manda-apontar-uma-câmera-que-não-existe--corrigida) (texto do QR) · [O-07](#o-07-ponto-de-interesse-some-se-a-página-recarregar--sem-objeto) · [O-09](#o-09-a-rota-sempre-parte-do-primeiro-totem-encontrado--sem-objeto) · [O-13](#o-13-a-fase-3-inteira-continua-planejada-e-não-feita--superada-pela-virada-de-escopo) · [O-14](#o-14-a-massa-de-dados-só-tem-um-par-de-substitutos-que-faz-sentido--resolvida) · [O-16](#o-16-o-token-continua-na-url-do-pwa--a-preocupação-deixou-de-existir)

---

## Ações fora do código

### O-01. Chave do Gemini precisa ser trocada, e a cota gratuita é apertada

**O quê.** Gerar uma chave nova no Google AI Studio e descartar a atual. E decidir, antes de 13/09, se vale habilitar o faturamento no Google Cloud — o que **mantém o uso gratuito** e apenas eleva os limites.

**Por que importa.** Dois motivos somados:

1. A chave atual passou por conversa e teve a cota diária consumida durante o desenvolvimento — e a substituta, gerada em 24/08, passou pelo mesmo canal em 25/08. A troca continua pendente.
2. O tier gratuito limita **por dia e por minuto**. No modelo de topo são **20 requisições por dia** — uma única sessão de testes esgotou. Trocamos para o `gemini-3.5-flash-lite`, que tem folga maior e responde em ~1s, mas o limite por minuto continua valendo: durante os testes de integração da ruptura, o terceiro teste seguido **caiu no fallback determinístico** por causa dele.

O fallback funcionou como projetado e o cliente recebeu resposta útil. Mas numa apresentação ao vivo, cair para "produto disponível mais próximo" justamente no momento mais forte da demonstração seria uma pena evitável.

**Isso já atrapalha a verificação, e não só a demonstração.** Confirmado em 25/08/2026: com `GEMINI_API_KEY` definida, a suíte de integração completa **não fecha verde de forma confiável** — os testes que conversam com o modelo falham em sequência e passam quando rodados isoladamente. Não é defeito do código; é o limite por minuto sendo atingido por uma suíte que dispara várias chamadas seguidas.

Enquanto a cota não subir, valem dois cuidados:

- a suíte **sem** `GEMINI_API_KEY` é determinística — os testes de IA são pulados e o resto fecha verde. É a forma de verificar um card;
- os testes de IA precisam ser rodados **por classe**, e não junto com o resto.

Habilitar o faturamento resolveria os dois problemas de uma vez.

**De quem.** João Ricardo. **Prazo que pressiona:** 13/09 (vídeo da seletiva, com o MVP funcionando).

---

### O-02. Senha do Oracle passou por canal de conversa

**O quê.** Trocar a senha do usuário `rm563609` no Oracle da FIAP.

**Por que importa.** A credencial foi compartilhada por chat durante a verificação de conectividade. Ela nunca foi escrita em arquivo nem versionada — sempre passada como variável de ambiente de linha de comando, e `backend/.gitignore` cobre `.env` e `application-local.yml`. Ainda assim, uma credencial que trafegou por um canal de conversa não deveria continuar valendo.

**De quem.** João Ricardo (é a credencial pessoal dele; cada integrante usa a sua).

---

### O-03. ~~O deploy ainda não foi feito~~ — no ar, e verificado

> [!NOTE]
> **Encerrada em 25/08/2026.** A API está publicada no Render e **respondendo com o código atual** — verificado por chamada real: `GET /api/v1/produtos/secoes` devolveu as dez seções com os 111 produtos, e o contrato publicado já traz `/mapa`, `desmarcar`, `posicao` e `substituir`.
>
> O receio de que a instância estivesse rodando código anterior à virada de escopo **não se confirmou**: o repositório vinha sendo empurrado ao longo do caminho e o Render acompanhou.

**O que sobrou de aprendizado, e continua valendo.** O plano gratuito dá 0,1 CPU, e a partida a frio **subiu de 134 para 176 segundos** com o catálogo maior — quase três minutos até a primeira resposta. Acordar a instância antes de qualquer demonstração deixou de ser recomendação e virou obrigação; ver [`deploy.md`](deploy.md).

**Verificado antes de escolher o provedor:** `oracle.fiap.com.br` resolve para IP público e aceita conexão de fora sem VPN — não há lista de IPs autorizados. O plano B era Fly.io, com região em São Paulo, caso houvesse filtro por país. Não houve.

### O-19. A entrada tem um plano B, e ele é uma tela que ainda não existe

> [!NOTE]
> **Atualizada em 30/08/2026 — a metade de tela está fechada.**
>
> **Feito:** o aviso de código não reconhecido entrou, nos **dois** caminhos — ao entrar pela URL e ao recentrar pelo modal. Verificado no navegador: `QQQ-77` devolve *"Não encontramos a localização; confira o código na placa e tente de novo"*, e `TIN-02` continua atualizando para "Corredor de Tintas".
>
> **E um defeito maior apareceu no caminho:** o chip de localização tinha `ENT-01 / Entrada da Loja` como valor inicial fixo, então afirmava que o cliente estava na entrada **antes de qualquer placa ser lida** — e continuava afirmando com código desconhecido. O aviso dizia a verdade e o chip ao lado dizia o contrário. A posição passa a nascer **nula**, e o chip a dizer *"Ainda não sabemos onde você está"*.
>
> **Continua aberto, e é do time:** a **arte da placa** com QR, URL curta e código legível.

<details><summary>O registro original, de quando a tela não existia</summary>

> **Reauditada em 30/08/2026 — a tela existe, o aviso não.**
>
> **Feito:** o modal de localização tem os dois planos, com exemplo do formato — escanear o QR ou digitar o código. É uma das partes mais bem resolvidas do frontend, porque antecipa a falha que de fato acontece na loja.
>
> **Continua aberto, e é a metade que este item avisava:** `App.jsx:130` faz `if (sess.posicaoAtual)` **sem `else`**. Quando o código é desconhecido, o backend cria a sessão com posição nula de propósito ([D-54](decisoes-tecnicas.md#d-54-a-entrada-aceita-o-código-da-placa-num-campo-só-e-código-desconhecido-não-recusa-a-sessão)) e **não devolve erro** — então a tela não tem o que tratar e simplesmente não diz nada. O cliente digita errado, entra sem posição, e não entende por que o mapa não mostra onde ele está. É exatamente o cenário descrito abaixo.
>
> **A arte da placa** continua com o time, e continua não feita.

</details>

**O quê.** Duas coisas, decididas em 25/08/2026:

1. **uma tela de entrada manual** — o cliente digita o código de localização quando escanear o QR não deu certo;
2. **a arte da placa** precisa trazer **três** elementos: o QR Code, uma **URL curta e legível**, e o **código de localização** (`TIN-02`). Isso tem custo de impressão e é decisão do time, não só do frontend.

**Por que importa.** O QR Code é o único acesso ao sistema. Se o cliente não consegue escanear — câmera ruim, leitor que não abre, permissão negada, adesivo sujo — ele fica sem nada. O código curto sozinho **não resolvia**: sem a URL impressa, não havia onde digitá-lo. Os dois planos precisam existir juntos ou nenhum funciona.

**O que o backend já entrega.** `POST /api/v1/sessoes` recebe `{"codigoPonto": "TIN-02"}`, num campo só — os dois planos chegam pelo mesmo endpoint. A grafia não importa: `TIN-02`, `tin02` e `TIN 02` são equivalentes.

**Cuidado com o caso de código errado.** A API **não** devolve erro para código desconhecido: a sessão é criada com `posicaoAtual` nula. Isso é proposital ([D-54](decisoes-tecnicas.md#d-54-a-entrada-aceita-o-código-da-placa-num-campo-só-e-código-desconhecido-não-recusa-a-sessão)) — barrar a entrada por causa de um adesivo seria pior. **Mas a tela precisa perceber isso e avisar**, algo como "não encontramos essa localização; você pode continuar e tentar de novo depois". Se o frontend ignorar, o cliente digita errado, entra sem posição e não entende por que o mapa não mostra onde ele está.

**De quem.** Bielecky e Marcela (a tela); time (a arte da placa).

---
### O-04. ~~`origemSugestao` não pode ser rotulado como IA quando for `PROXIMIDADE`~~ — resolvida na tela

> [!NOTE]
> **Encerrada em 30/08/2026.** O `RupturaModal` mostra **dois selos diferentes**, escolhidos pelo campo: *"Sugestão do assistente"* com ícone de brilho quando a origem é `ASSISTENTE_IA`, e *"Disponível mais próximo"* com ícone de bússola quando é `PROXIMIDADE` (`RupturaModal.jsx:71`). A tela não afirma mais análise que não houve.
>
> O texto abaixo fica como registro do porquê.

**O quê.** A resposta de `POST /api/v1/roteiro/itens/{itemId}/ruptura` traz o campo `origemSugestao`, com dois valores possíveis:

- `ASSISTENTE_IA` — o assistente avaliou os candidatos e elegeu um.
- `PROXIMIDADE` — o assistente estava fora do ar (ou respondeu fora do combinado) e o sistema caiu para o produto disponível mais próximo, **sem nenhuma análise de compatibilidade**.

**Por que importa.** Se a tela mostrar um selo de "recomendação inteligente" nos dois casos, ela vai estar afirmando algo falso — e é o tipo de detalhe que um avaliador atento consegue provocar ao vivo, bastando esgotar a cota. A justificativa que acompanha a sugestão já é honesta ("confira na embalagem se ele atende ao seu caso"); a tela só precisa não contradizê-la.

**De quem.** Bielecky e Marcela. Ver [D-38](decisoes-tecnicas.md#d-38-ruptura-de-estoque-o-modelo-escolhe-mas-quem-responde-é-o-banco).

---

### O-05. ~~O botão "Prateleira Vazia" precisa travar durante a requisição~~ — resolvida com trava síncrona

> [!NOTE]
> **Encerrada em 30/08/2026, e a primeira tentativa não bastou.**
>
> **O que falhou:** uma guarda por `useState` mais o `disabled` no botão. Cinco toques no mesmo tique dispararam **cinco** chamadas — `useState` só chega ao próximo render, e até lá o fecho do manipulador vê o valor antigo.
>
> **O que resolveu:** uma trava em `useRef`, que é síncrona. Os mesmos cinco toques passaram a virar **uma** chamada.
>
> **Repetir de propósito continua funcionando**, e deve: relatos separados por segundos passam, porque duas visitas frustradas à prateleira são dois dados para a loja. O que a trava impede é a rajada acidental.
>
> **Achado de brinde, durante o teste:** com a cota do Gemini esgotada pelos próprios testes, o backend caiu para proximidade e respondeu em **402 ms** — e o selo mudou corretamente para "Disponível mais próximo". A [O-04](#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade--resolvida-na-tela) foi vista funcionando sem ter sido provocada.

<details><summary>O registro original</summary>

> [!IMPORTANT]
> **Reauditada em 30/08/2026 — continua aberta, e agora está medida.** O botão tem `disabled={!item.idBackend}` (`RoteiroDrawer.jsx:118`), que é outra guarda: impede tocar em item **ainda não sincronizado** com o servidor. Nenhum estado de carregamento chega até ele, então **o toque duplo continua passando**.
>
> A correção é pequena: o `App.jsx` já carrega o estado `isCarregando` do fluxo de ruptura para o modal — falta descê-lo até o botão que dispara.

**O quê.** Desabilitar o botão enquanto a chamada de ruptura estiver em voo.

**Por que importa.** O endpoint **não é idempotente por decisão**: dois toques são duas visitas frustradas à prateleira, e a loja precisa das duas no registro. Mas num celular, um toque duplo acidental custa **duas chamadas ao Gemini** — e a cota gratuita é de 5 por minuto ([O-01](#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)). Duas chamadas desperdiçadas podem ser o que derruba a sugestão seguinte para o fallback bem no meio da demonstração.

**Alternativa no servidor, se o time preferir.** Devolver a mesma sugestão quando a ruptura do mesmo item foi relatada nos últimos segundos. Não foi implementado: é escopo além do card, e a defesa no frontend é a convencional. Fica registrado como opção, não como pendência.

**De quem.** Bielecky e Marcela.

</details>

---

### O-06. O celular tem um caminho de recuperação se a aba fechar

> [!NOTE]
> **Reescrita em 25/08/2026.** A versão anterior descrevia a recuperação via token de handoff, que não existe mais. **O problema continua real** — a aba fecha, o cliente perde o contexto —, só que a saída mudou.

**O quê.** Guardar o `sessaoId` no `localStorage` assim que a sessão nasce, e tentar retomá-la ao abrir a página.

**Por que importa.** A sessão agora guarda **a lista inteira**. Um cliente que fecha a aba sem querer, ou cujo navegador descarta a página por falta de memória, precisaria montar tudo de novo — e ao vivo, na frente da banca, seria um desastre.

**Como retomar.** `GET /api/v1/sessoes/{sessaoId}` devolve o status e a `posicaoAtual`; `GET /api/v1/sessoes/{sessaoId}/roteiro` devolve a lista com o que já foi coletado. Nada disso exige token.

**Se a sessão não valer mais** — status diferente de `ACTIVE`, ou TTL vencido —, a tela precisa perceber e oferecer começar de novo, em vez de deixar o cliente batendo em 409 sem entender. Com o TTL em 4 horas ([D-24](decisoes-tecnicas.md#d-24-ttl-da-sessão-é-renovado-a-cada-interação)) isso ficou raro, mas não impossível.

**E se nem o `sessaoId` sobreviveu** — troca de aparelho, navegador anônimo —, não há recuperação: é começar do zero escaneando uma placa. Aceito: guardar mais que isso exigiria identificar o cliente, o que o projeto deliberadamente não faz.

**De quem.** Bielecky e Marcela.


### O-07. ~~Ponto de interesse some se a página recarregar~~ — sem objeto

> [!NOTE]
> **Encerrada em 25/08/2026.** O recurso foi removido junto com a rota calculada: sem rota, não existe posição em que inserir um desvio. O banheiro virou apenas mais um ponto do mapa, e `GET /mapa` o devolve sem depender de sessão — não há estado a perder num reload.
>
> Ver [D-49](decisoes-tecnicas.md#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada).

---


### O-25. ~~O frontend carrega um catálogo inventado, e o mostra quando a API cai~~ — apagado

> [!NOTE]
> **Encerrada em 30/08/2026.** As 202 linhas de produtos e seções escritos à mão saíram, e com elas o filtro local e uma implementação de Levenshtein que só existia para servi-lo. O arquivo caiu de 383 para 120 linhas.
>
> **No lugar, as funções lançam** — e a tela distingue *"procuramos e não há"* de *"não conseguimos procurar"*, que era o ponto. Devolver lista vazia teria sido a mesma doença em grau menor. Ver [D-77](decisoes-tecnicas.md#d-77-o-catálogo-lança-quando-não-consegue-perguntar-e-a-tela-distingue-isso-de-não-há).
>
> **Verificado com o backend derrubado de verdade**, não simulado: catálogo e setores mostram o aviso honesto, zero produtos na tela, e "tentar de novo" recupera quando o servidor volta.
>
> O texto abaixo fica como registro do porquê.

**O quê.** `catalogService.js` tem uma lista de produtos escrita à mão e a devolve quando a chamada à API não dá certo. Ela precisa sair.

**Por que importa, e por que é a mais urgente da lista.** Não é dado desatualizado: são **produtos que a loja não tem, em corredores que não existem**, apresentados como se fossem o catálogo real. E aparecem exatamente quando ninguém consegue conferir — com o servidor fora do ar.

O efeito se espalha. Enquanto essa lista existir:

- a **busca** devolve resultados falsos e o cliente sai procurando algo que não está lá;
- o **mapa** desenha marcadores em produtos inventados;
- qualquer aviso honesto de indisponibilidade que a gente escreva vira mentira, porque a tela ao lado continua mostrando uma loja cheia.

**O motivo de ela existir é legítimo, e acabou.** Bielecky precisava de dados para desenvolver antes de o backend estar de pé. Ele está de pé, publicado, e com 111 produtos reais.

**Ordem.** Este é o primeiro da fila, e vem antes de qualquer trabalho de "avisar quando a loja não responde" ([`quebras-de-fluxo.md`](quebras-de-fluxo.md)) — não adianta escrever o aviso enquanto o silêncio continua sendo preenchido com ficção.

**De quem.** Backend/integração.

---

### O-26. ~~O convite do mapa vazio promete uma rota que não existe~~ — corrigido

> [!NOTE]
> **Encerrada em 30/08/2026.** O texto passou a descrever o que o mapa faz: *"Adicione produtos e eles aparecem aqui no mapa, junto de onde você está."*
>
> Verificado no navegador: com a lista vazia, nenhuma das três frases que prometiam rota aparece na tela.

**O quê.** Com a lista vazia, o mapa diz *"Adicione produtos na vitrine para traçar a melhor rota"* (`StoreMapPage.jsx:711`). Trocar por uma frase que descreva o que o mapa faz: mostrar onde o cliente está e onde está cada item.

**Por que importa.** "Traçar a melhor rota" é o recurso que a mentoria da Leroy mandou tirar ([D-49](decisoes-tecnicas.md#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada)). A frase promete à banca justamente aquilo que decidimos não fazer — e se alguém adicionar produtos esperando ver um caminho traçado, o que vai encontrar são marcadores.

Cinco textos assim já saíram da tela no card de honestidade de 28/08. Este passou porque só aparece com a lista vazia, que é um estado que ninguém revisita depois da primeira vez.

**De quem.** Frontend.

---

### O-27. ~~As 22 imagens coletadas não têm para onde ir~~ — medição errada, o mecanismo já existia

> [!NOTE]
> **Encerrada em 30/08/2026, no mesmo dia em que foi aberta, porque estava errada.**
>
> **O que a medição contra a API mostrou:** 22 dos 111 produtos respondem com `imagemUrl` preenchida — exatamente os 22 declarados, nenhum faltando. O documento de coleta e o código concordam SKU a SKU, e nenhuma URL coletada pelo time se perdeu.
>
> **Por que o erro aconteceu.** Procurei `http` no `CatalogoDaMassa.java` e achei zero, e concluí que não havia caminho. As URLs não moram lá: moram num `Map<String,String> IMAGENS` no `CarregadorDadosIniciais.java`, aplicado quando o produto nasce e propagado aos bancos já existentes por `sincronizarApresentacoes`.
>
> **A separação é deliberada**, e o próprio código explica: acrescentar uma foto não deve exigir mexer na lista de 111 produtos. Uma linha no mapa basta, e ela chega sozinha aos bancos que já rodaram.
>
> **Fica registrada em vez de apagada**, porque o erro tem valor: é a segunda vez em uma semana que confundi *"o arquivo não tem"* com *"o sistema não faz"* — a primeira foi o mapa, quando medi a função em vez do caminho. É exatamente o que o fluxo de trabalho do time já manda não fazer.
>
> **O que continua aberto** é só a coleta manual das outras 89, que é do time e está em [`imagens-dos-produtos.md`](imagens-dos-produtos.md).

---

### O-28. ~~A visão "Todos" do catálogo não pagina~~ — os 111 passaram a ser alcançáveis

> [!NOTE]
> **Encerrada em 30/08/2026.** Entrou "Carregar mais" abaixo da grade, e os três contadores passaram a dizer a verdade.
>
> **Medido no navegador, com o backend real:**
>
> | | Antes | Depois |
> |---|---|---|
> | Produtos alcançáveis em "Todos" | 50 | **111**, em dois toques |
> | Selo acima da grade | `50 itens` | `50 de 111` → `100 de 111` → `111 itens` |
> | Botão do modal de filtros | `Ver 50 produtos` | `Ver 111 produtos` |
> | Repetidos ao paginar | — | **nenhum**: 111 cartões, 111 nomes distintos |
>
> **A guarda de corrida foi testada de propósito:** pedir mais e trocar de seção no mesmo instante deixa só os itens da seção nova. Ver [D-78](decisoes-tecnicas.md#d-78-paginação-com-duas-funções-separadas-e-botão-em-vez-de-rolagem-infinita).
>
> **Fica um ajuste possível, com outra medição:** o tamanho da página continua 50, e baixá-lo para 20 aliviaria os 2,4 s de abertura da [O-22](#o-22-a-tela-inicial-do-catálogo-leva-24-segundos-no-ambiente-publicado). Não foi feito aqui porque é outra decisão.

**O quê.** A tela de catálogo carrega uma página só. Com 111 produtos, **61 não são alcançáveis** sem usar filtro ou busca.

**Por que importa.** O catálogo foi de 29 para 111 produtos justamente para a paginação paginar, a faceta filtrar e o corredor parecer corredor ([O-18](#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens)). Sem paginar na tela, mais da metade desse esforço não chega ao cliente — e a demonstração mostra uma loja menor do que a que construímos.

O backend já pagina: a resposta traz total, página atual e tamanho. É trabalho de tela.

**De quem.** Frontend.

---

## Para o banco e os diagramas (Vicentini)

### O-08. O DER não tem a tabela de registro de ruptura

**O quê.** O card de ruptura criou `TB_REGISTRO_RUPTURA` (sessão, item, produto em falta, produto sugerido, justificativa, origem, data). A tabela **já existe no Oracle** — o `ddl-auto: update` a criou automaticamente —, então não há trabalho de banco. Falta apenas atualizar o diagrama.

**Por que importa.** O DER é um dos artefatos entregues e serve de fonte de verdade para o time. Um diagrama que não bate com o banco confunde quem chega depois e é fácil de notar numa mentoria.

**De quem.** Vicentini. **Prazo que pressiona:** 24/08 (mentoria com os representantes técnicos da Leroy Merlin).

---

### O-16. ~~O token continua na URL do PWA~~ — a preocupação deixou de existir

> [!NOTE]
> **Encerrada em 25/08/2026.** Não há mais token: o handoff foi removido com o totem.
>
> **O sucessor não tem o mesmo problema, e vale entender por quê.** O QR Code hoje codifica algo como `.../?p=TIN-02` — o código da placa. Ele é **público por natureza**: está impresso na parede da loja, ao lado do próprio QR, justamente para quem não consegue escanear poder digitá-lo.
>
> Ficar no histórico do navegador não expõe nada, porque ele não autoriza nada: informar uma placa não dá acesso a sessão alguma, apenas diz onde a pessoa está. **Limpar a URL deixou de ser hardening e virou preferência estética.**


### O-17. Documentos de trabalho precisam sair antes da entrega final

**O quê.** Apagar [`perguntas-mentoria.md`](perguntas-mentoria.md) e [`imagens-dos-produtos.md`](imagens-dos-produtos.md) — e qualquer arquivo da mesma natureza que venha a existir — antes de enviar o repositório ao portal da faculdade.

**Por que importa.** Nem tudo que ajuda o time a trabalhar pertence à documentação do produto. O arquivo de perguntas foi escrito para preparar **uma reunião específica**, em uma data específica; passada ela, só acrescenta ruído a quem for avaliar o projeto.

A distinção vale a pena manter em mente conforme o repositório cresce:

- **Permanece:** `contexto-e-planejamento.md`, `planejamento-tecnico.md`, `decisoes-tecnicas.md`, `deploy.md`, `backlog-trello-revisado.md`, os diagramas, os READMEs. Explicam o que o projeto é e por que ficou assim.
- **Sai:** artefatos amarrados a um evento pontual — roteiro de reunião, lista de perguntas, anotações de preparação.
- **Caso à parte:** este próprio `observacoes.md`. As pendências resolvidas até lá devem ter saído da lista; o que sobrar são limitações assumidas conscientemente, e **isso conta a favor** — mostra que o time sabia onde estavam os limites.

**De quem.** Time. **Quando:** antes do envio ao portal, no fim do ano.

---

### O-24. Os diagramas da Sprint 1 desenham o produto que a mentoria derrubou

**O quê.** Os cinco PDFs em `docs/diagramas/` — C4, DER, casos de uso, classes e sequência — são de antes de 24/08/2026. Precisam ser refeitos ou explicitamente rotulados como Sprint 1.

**Por que importa.** Eles ainda desenham **totem**, **handoff com JWT** e **rota calculada**: três coisas que foram removidas do sistema de propósito. O diagrama de sequência inteiro descreve o handoff, que não existe. O C4 mostra dois containers de frontend, e existe um.

E o problema não é interno: **diagrama é o que a banca olha primeiro**, antes de qualquer código. Um avaliador que compare o C4 com a demonstração vai ver dois produtos diferentes — e a conclusão natural dele será que não entregamos o que projetamos, quando o que aconteceu foi o contrário: mudamos o projeto porque a Leroy mandou.

**O DER tem um problema a mais**, já registrado em separado: falta a tabela de registro de ruptura ([O-08](#o-08-o-der-não-tem-a-tabela-de-registro-de-ruptura)).

**O caminho mais barato**, se refazer os cinco não couber no prazo: carimbar cada um como "Sprint 1 — anterior à revisão de escopo" e refazer só o **C4** e o **DER**, que são os dois que a banca costuma abrir.

**De quem.** Vicentini (DER) e time (os demais).

---

## Para a demonstração

### O-18. O catálogo de 29 produtos é pequeno demais para a banca — ~~resolvido no volume~~, pendente nas imagens

> [!NOTE]
> **Volume resolvido em 25/08/2026: o catálogo passou de 29 para 111 produtos**, cerca de onze por seção. O que continua aberto é a **coleta das imagens**, e ela ficou maior junto.
>
> **Coleta em andamento: 22 de 111 em 25/08/2026** — Cozinhas, Decoração e o início da Elétrica. Junto das fotos vieram os **nomes reais** dos produtos do site, e os dois já estão na massa, com as marcas e medidas reconciliadas ([registro](imagens-dos-produtos.md#o-que-entrou-junto-com-os-nomes-reais--aplicado-em-25082026)). **As duas lixas da ruptura continuam sem foto** — são as que encenam a ruptura, e as mais importantes de todas.

**O quê.** Ampliar a massa de demonstração, e coletar as URLs das imagens.

**Por que importa.** Na banca não se controla o que os avaliadores vão querer ver. Com 29 produtos, qualquer busca fora do roteiro ensaiado devolve pouco ou nada, e a navegação por seção mostra três itens por corredor — o que passa a impressão de projeto inacabado, não de protótipo enxuto.

Um catálogo maior também melhora o produto de graça:

- **a busca passa a valer** — paginação, filtros e tolerância a erro de digitação só ficam convincentes com volume;
- **os pares de substituição aparecem sozinhos** — hoje há cinco, plantados à mão ([O-14](#o-14-a-massa-de-dados-só-tem-um-par-de-substitutos-que-faz-sentido--resolvida)); com trezentos produtos, quase todo item passa a ter vizinho plausível;
- **o mapa fica com densidade de loja de verdade.**

**A descrição já está resolvida.** Os 29 produtos atuais têm descrição escrita, e a carga completa quem já estava gravado ([D-59](decisoes-tecnicas.md#d-59-a-carga-completa-a-apresentação-de-produtos-que-já-estavam-gravados)). Produtos novos entram com a sua junto.

**A tensão que continua de pé é a imagem.** As URLs vêm do site público da Leroy, coletadas à mão — a lista está pronta para preencher em [`imagens-dos-produtos.md`](imagens-dos-produtos.md), agora com as 111 linhas.

**Coletar 111 à mão não é razoável, e não precisa ser.** Imagem nula é estado normal e testado. A recomendação está no próprio documento: as duas lixas primeiro, porque encenam a ruptura; depois o resto de Tintas, que é a seção do roteiro ensaiado; depois um punhado em cada outra seção, para a navegação não parecer vazia. O restante pode ficar sem foto indefinidamente.

**Descrição e características dos 111 já estão escritas**, e cada produto é declarado numa entrada só ([D-66](decisoes-tecnicas.md#d-66-cada-produto-da-massa-e-declarado-uma-vez-inteiro)) — acrescentar mais produtos continua barato do lado do código.

**Um efeito colateral que o volume revelou** e vale lembrar: ampliar o catálogo quebrou a pré-filtragem da ruptura, porque o teto de candidatos tinha sido dimensionado para uma massa pequena ([D-67](decisoes-tecnicas.md#d-67-o-teto-de-candidatos-da-ruptura-envelheceu-com-o-catalogo)). Se o catálogo crescer de novo, vale rodar a suíte inteira e olhar esse teste em particular.

Três saídas, e vale escolher antes de começar:

1. **Catálogo grande, imagens só nos produtos do roteiro da demonstração** — os outros usam uma imagem genérica por seção. Barato, e ninguém percebe numa vitrine.
2. **Catálogo médio, umas 60 a 80 unidades**, todas com imagem própria. Já dá densidade e a coleta manual continua viável.
3. **Catálogo grande com imagens todas genéricas por seção.** O mais rápido, e o que menos ajuda no vídeo.

A opção 1 é a que eu recomendaria: dá volume onde ele conta e concentra o trabalho manual onde a câmera vai olhar.

**De quem.** Time. **Prazo que pressiona:** 21/09 (banca final), mas convém antes de 13/09 para o vídeo já mostrar densidade.

---

### O-14. ~~A massa de dados só tem um par de substitutos que faz sentido~~ — resolvida

**Resolvida em 23/08/2026.** A massa passou a ter **cinco cenários de ruptura** em cinco seções espalhadas pela loja, e não mais um só. Ver [D-47](decisoes-tecnicas.md#d-47-a-massa-ganhou-pares-de-substituição-e-a-carga-passou-a-ser-incremental).

| Produto a zerar | Substituto esperado | Seção |
|---|---|---|
| Lixa para Parede Grão 120 | Lixa d'Água Grão 150 | Tintas |
| Lâmpada LED 9W - kit 3 | Lâmpada LED 12W - kit 3 | Iluminação |
| Sifão Sanfonado Universal | Sifão Copo Cromado Universal | Encanamento |
| Trena 5m | Trena 7,5m | Ferramentas |
| Argamassa AC-II 20kg | Argamassa AC-III 20kg | Materiais de construção |

Qualquer um deles pode ser disparado pela ferramenta de simulação ([D-40](decisoes-tecnicas.md#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido)), inclusive ao vivo, se alguém da banca pedir para tentar com outro produto.

**O que continua valendo:** zerar um produto **fora** desta lista provavelmente resulta em 422, porque o assistente recusa corretamente quando nada por perto cumpre a mesma função. Isso não é defeito — é o comportamento que a [D-38](decisoes-tecnicas.md#d-38-ruptura-de-estoque-o-modelo-escolhe-mas-quem-responde-é-o-banco) descreve, e defendê-lo ao vivo é um bom argumento. Só convém escolher da tabela acima na hora de gravar.

---

## Limitações aceitas conscientemente

> Estas não são pendências. São escolhas de escopo que valem ser lembradas — principalmente porque é melhor citá-las antes que alguém as descubra.

### O-20. Rodar a suíte deixa um resto de sessões no banco de demonstração

> [!NOTE]
> **Corrigida em 25/08/2026.** A versão original afirmava que a limpeza de vários testes falhava e que sessões órfãs iam se acumulando. **Isso foi escrito sem verificação, e a medição não sustenta.** Os testes que geram ruptura já apagavam os registros antes da sessão; o erro observado veio de um teste que falhou no meio, não do caminho normal.

**O que a medição mostra.** Depois de um dia inteiro de execuções da suíte completa, o schema tinha **23 sessões, nenhuma lista órfã e nenhuma sessão `ACTIVE` vencida**. A limpeza funciona e a varredura de TTL faz o resto.

**O que sobra, e por que importa mesmo sendo pouco.** Das 23, **17 estão `ABANDONED`** — sessões que vazaram de execuções interrompidas e que a varredura classificou como carrinho abandonado, exatamente como faria com um cliente real.

`ABANDONED` é a métrica de negócio que o projeto usa para dizer que uma venda quase aconteceu ([D-42](decisoes-tecnicas.md#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem)). **Misturada com resíduo de teste, ela deixa de significar o que promete** — e é justamente o tipo de número que se cita numa banca.

**O que fazer.** Nada urgente no código. Antes da gravação do vídeo e da banca, vale **limpar as sessões de teste** para que qualquer número citado seja verdade. Um `delete` das sessões finalizadas resolve, respeitando a ordem das chaves estrangeiras: registros de ruptura, mensagens de chat, itens, lista, sessão.

**De quem.** Backend. **Urgência:** baixa até a véspera da demonstração.

---

### O-09. ~~A rota sempre parte do primeiro totem encontrado~~ — sem objeto

> [!NOTE]
> **Encerrada em 25/08/2026.** Não há totem nem rota calculada. A origem deixou de ser um ponto fixo do banco e passou a ser a placa que o cliente leu, informada por ele na entrada — "qual totem" não tem mais como ser uma pergunta.
>
> Ver [D-54](decisoes-tecnicas.md#d-54-a-entrada-aceita-o-código-da-placa-num-campo-só-e-código-desconhecido-não-recusa-a-sessão).


### O-10. O estoque exibido é o do nosso banco, e só

Não há integração com ERP ou WMS — não temos acesso a sistema real da Leroy Merlin, e a restrição de custo zero inviabiliza qualquer intermediário. **Vale dizer isso na apresentação antes de perguntarem**, porque a resposta é forte: o sistema resolve o problema de *localizar* o produto, e o fluxo de ruptura física existe justamente por assumir que a divergência entre sistema e prateleira **vai** acontecer — em vez de fingir que o estoque é confiável. Ver [D-23](decisoes-tecnicas.md#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp).

### O-11. O Swagger fica exposto no ambiente publicado

Decisão deliberada: a documentação navegável é o que permite ao time e aos avaliadores explorarem a API sem cliente HTTP, e não há dado sensível atrás dela. Num sistema real com dados de cliente, seria fechado. Ver [D-34](decisoes-tecnicas.md#d-34-swagger-ui-permanece-exposto-em-produção).

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

### O-15. O endpoint de simulação de estoque não tem proteção nenhuma

`PATCH /api/v1/produtos/{produtoId}/estoque` altera o catálogo e **qualquer pessoa com a URL da API publicada consegue chamar**. É o único endpoint que escreve no catálogo e o de maior impacto do sistema.

Aceito por três motivos: a API não tem autenticação em endpoint nenhum, então ele não abre uma categoria nova de exposição; desligá-lo em produção derrotaria o propósito, já que é justamente o ambiente publicado que será demonstrado; e o dano é reversível pelo próprio endpoint, sobre massa de demonstração.

O que existe no lugar de proteção: marcação explícita como `[Demonstracao]` no Swagger e log em nível **WARN** a cada alteração. **Se o projeto ganhar autenticação em algum momento, este endpoint é o primeiro que precisa entrar atrás dela.** Ver [D-40](decisoes-tecnicas.md#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido).

---

### O-29. Duas pastas de documentação estão vazias desde o primeiro dia

**O quê.** `docs/arquitetura/` e `docs/casos_de_uso/` só têm `.gitkeep`, criados em 10/08/2026, e nunca receberam arquivo.

**Por que importa.** Pouco, e é por isso que está aqui e não acima. Mas pasta vazia num repositório que vai ser avaliado sugere trabalho planejado e não feito — e neste caso é o contrário: o conteúdo existe, só mora em outro lugar. A arquitetura está em [`decisoes-tecnicas.md`](decisoes-tecnicas.md) e em `planejamento-tecnico.md`; os casos de uso, no contrato OpenAPI e em [`fluxo-do-cliente.md`](fluxo-do-cliente.md).

**Duas saídas, e as duas servem:** apagar as pastas, ou pôr em cada uma um arquivo curto apontando para onde o assunto está documentado. A segunda é melhor para quem chega procurando pelo nome da pasta.

**De quem.** Time.

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

### O-32. A paleta de cores por setor reprova no contraste, e é decisão de identidade

**O quê.** Os dez tons de `constants/setores.js` — um por seção — são usados como cor de ícone sobre fundos tingidos da mesma família. **Medidos em 30/08/2026**, no mapa e na tela de setores:

| Ícone | Cor | Contraste | Mínimo |
|---|---|---|---|
| `bolt`, `lightbulb`, `construction` | `#f59e0b` | **1,93** | 3,0 |
| `bathtub` | `#06b6d4` | **2,18** | 3,0 |
| `countertops`, `yard` | `#10b981` | **2,28** | 3,0 |
| `plumbing` | `#0ea5e9` | **2,42** | 3,0 |
| `texture` | `#d97706` | **2,86** | 3,0 |
| outros seis | — | 3,16 a 4,42 | 3,0 |

**Por que não entrou no card de acessibilidade de 30/08.** Aquele card corrigiu o verde da marca, que é **um** sistema de cor com regra clara. Este é outro: **dez cores escolhidas para diferenciar seções**, e escurecê-las mexe na identidade visual que a dupla de frontend desenhou. Corrigir sem conversar seria decidir por eles.

**Duas saídas, e a segunda é mais barata do que parece.**

1. **Escurecer os dez tons** até 3:1 sobre os fundos tingidos. Mantém a lógica de "uma cor por seção", muda o visual.
2. **Escurecer só o fundo tingido**, mantendo as cores. O contraste sobe sem tocar na paleta — e o ícone continua colorido do mesmo jeito.

**Atenuante honesto:** são ícones **acompanhados do nome da seção escrito ao lado**. Ninguém depende da cor para saber que corredor é aquele — ela é reforço, não a informação. Por isso é média, e não alta.

**De quem.** Frontend, com o time.

---

### O-33. Oito alvos de toque abaixo de 44px, e o projeto define esse mínimo

**O quê.** Medido em 375 px na tela inicial:

- **seis chips de sugestão** ("Lâmpada LED", "Tinta Acrílica"…): **30px de altura**
- **botão de busca**: 40 × 40
- **campo de busca**: 40 de altura

**Por que importa.** O app é usado **em pé, num corredor, com uma mão** — muitas vezes a outra segurando um produto. Alvo pequeno erra mais nessa situação do que sentado no sofá.

**O detalhe que chama atenção:** `--min-touch-target: 44px` **está definido nos tokens e é usado em doze lugares**. Não é desconhecimento do padrão — é aplicação incompleta dele.

**Correção sugerida, que não muda o desenho.** Nos chips, `min-height: 44px` com o preenchimento vertical distribuído mantém a aparência de pílula fina e dobra a área de toque. É o tipo de erro que só aparece com a mão, e não no monitor.

**De quem.** Frontend.

---

### O-34. ~~A aba do QR Code manda apontar uma câmera que não existe~~ — corrigida

> [!NOTE]
> **Encerrada em 31/08/2026, e era só texto.**
>
> - A frase passou a descrever o que de fato acontece: *"Use a câmera do seu celular na placa do corredor — ela abre o app já na sua posição."*
> - As coordenadas cruas (`Coord: (50, 92)`) saíram da lista de placas.
> - A lista passou a se assumir: *"Atalho de demonstração — ir direto para uma placa"*.
>
> **O visor animado ficou.** Ele não afirma nada falso — é enquadramento visual do que o cliente vai fazer com a câmera do próprio celular, e a varredura só anima quando uma simulação é disparada.

**O quê.** A aba "Plano A: QR Code" do modal de localização diz *"Aponte a câmera para o QR Code da placa do corredor"*. **Não há `<video>`, não há `<canvas>`, não há leitor** — verificado. A instrução não leva a lugar nenhum.

**E o mais interessante: o fluxo do QR funciona.** O cliente escaneia com a **câmera nativa do celular**, que abre a URL com `?ponto=TIN-02`, e o app entra já posicionado — testado com placa válida e inválida. O aplicativo **nunca precisou de leitor próprio**. Só a tela descreve errado.

**É correção de texto, não de código.** Algo como *"escaneie a placa com a câmera do seu celular — ela abre o app já na sua posição"*. Os botões de simulação abaixo são honestos e podem ficar: dizem "SIMULE" com todas as letras.

**Dois detalhes na mesma tela.** Ela lista **todas as placas da loja** com `Coord: (50, 92)` — coordenadas cruas do nosso sistema de eixos, que não significam nada para o cliente. Numa loja real, uma lista de todas as placas derrotaria o propósito do QR.

**De quem.** Frontend.

---

## Riscos de ambiente

### O-22. A tela inicial do catálogo leva 2,4 segundos no ambiente publicado

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

**De quem.** Frontend (itens 1 e 2), time (item 3). **Urgência:** média — a tela lenta é a primeira que a banca vê.

---

### O-23. Processo Java abandonado trava a suíte inteira no Oracle

**O quê.** Duas maneiras de deixar JVM órfã segurando transação aberta no schema compartilhado. As duas custaram tempo em 28/08, e as duas voltam a acontecer sozinhas.

**Parar o Maven não para o Surefire.** Interromper `mvnw test` mata o processo do Maven, mas o **JVM filho que roda os testes sobrevive** — e sobrevive no meio de uma transação. Ele vira uma sessão `INACTIVE` que nunca faz commit nem rollback, e qualquer execução seguinte que toque as mesmas linhas fica presa esperando.

Foi exatamente isso: uma sessão ociosa bloqueando outra, que bloqueava mais duas. A suíte ficou **20 minutos parada** sem sair do lugar e sem falhar — o pior formato de erro, porque não avisa.

**O servidor de desenvolvimento recarrega sozinho.** Com o `spring-boot:run` aberto, o DevTools reinicia a aplicação a cada compilação — e **cada reinício executa a carga inicial**. Numa dessas, a planta já estava acentuada e a migração de renomeação ainda não existia: a carga criou as quatro seções novas vazias, e o banco ficou com a seção duplicada. Ver [D-70](decisoes-tecnicas.md#d-70-renomear-uma-seção-é-migração-não-edição-de-string), que passou a apagar a prateleira vazia justamente por causa disso.

**Por que dói aqui e não doeria em outro projeto.** Porque o schema é um só para tudo ([O-21](#o-21-desenvolvimento-testes-e-demonstração-usam-o-mesmo-schema)). Num banco local descartável, um lock preso se resolve derrubando o banco. No da FIAP, **não temos privilégio para derrubar sessão** — `alter system kill session` responde `ORA-01031`. Só resta esperar o Oracle colher a conexão morta, o que levou cerca de meia hora.

**O que fazer.**

- **Fechar o servidor de desenvolvimento antes de editar o backend.** Se ele estiver no ar durante as edições, ele executa versões intermediárias do código contra o banco de verdade.
- **Ao interromper a suíte, conferir se sobrou JVM.** No Windows: `Get-CimInstance Win32_Process -Filter "Name='java.exe'"` mostra o horário de início e a linha de comando — as do Surefire aparecem com `-jar ...\surefire`. Encerrar essas pela raiz libera o lock na hora.
- **Não confundir com falha de teste.** Suíte parada sem sair do lugar por mais de dois minutos é lock, não lentidão. `select sid, blocking_session from v$session where username = user` responde em um segundo.

**De quem.** Backend. **Urgência:** alta — um lock preso na véspera da gravação custaria meia hora que não vamos ter.

---

### O-21. Desenvolvimento, testes e demonstração usam o mesmo schema

**O quê.** Não existe separação entre "banco de desenvolvimento" e "banco de produção": a instância publicada no Render e a suíte de testes de integração apontam para o **mesmo schema Oracle da FIAP**.

**Como isso ficou evidente.** Ao verificar o deploy em 25/08, a API publicada respondeu com **111 produtos** — embora o código que ela roda declare 29. Ela estava lendo a massa que as execuções da suíte tinham gravado.

**Por que é estrutural, e não descuido.** A FIAP dá **um schema por aluno**. Criar um segundo banco exigiria outro provedor, e a restrição de custo zero fecha essa porta.

**O que de fato pode dar errado.** Menos do que parece, e vale ser preciso:

- **estoque** — o teste que zera a trena restaura em `finally`, e isso está no código desde o começo;
- **sessões** — sobra um resto, tratado na [O-20](#o-20-rodar-a-suíte-deixa-um-resto-de-sessões-no-banco-de-demonstração);
- **catálogo** — a carga é incremental e idempotente, e produtos que ela não conhece são deixados em paz.

**O risco real é de momento, não de dados.** Rodar a suíte completa **durante** a gravação do vídeo ou a banca mexeria na massa que está sendo demonstrada — e a partida a frio de 176 segundos já torna a instância sensível a qualquer sobressalto.

**O que fazer.** Combinar uma regra simples: **ninguém roda a suíte de integração enquanto alguém estiver gravando ou apresentando.** Não custa nada e elimina o único cenário que machuca.

**De quem.** Time. **Urgência:** média — até a gravação de 13/09.

---

## Planejamento superado

### O-13. ~~A Fase 3 inteira continua planejada e não feita~~ — superada pela virada de escopo

> [!NOTE]
> **Encerrada em 25/08/2026.** A Fase 3 foi concluída em 22/08 e, dois dias depois, **dois dos seus três itens deixaram de existir**: o refinamento 2-opt saiu com a rota calculada, e o hardening do handoff saiu com o totem. Só a varredura de TTL sobreviveu — e foi reajustada desde então ([D-24](decisoes-tecnicas.md#d-24-ttl-da-sessão-é-renovado-a-cada-interação)).
>
> O planejamento por fases foi substituído pelo [`backlog-escopo-revisado.md`](backlog-escopo-revisado.md), cujos treze cards estão concluídos. **Manter esta observação viva sugeriria um trabalho pendente que não existe.**

---

## Como manter este documento

Sempre que uma observação **for resolvida**, remova o item e — se ela virou uma decisão — registre-a em [`decisoes-tecnicas.md`](decisoes-tecnicas.md). Este documento só é útil enquanto tudo nele estiver de fato em aberto.

O critério para entrar: *alguém precisa agir, decidir ou ser avisado disso, e o código sozinho não conta essa história?* Se sim, anote aqui, com o dono e o prazo.
