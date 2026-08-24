# Merlin Route Finder — Observações em Aberto

> Coisas que alguém precisa **fazer, decidir ou saber**, e que não cabem no código.
>
> Complementa [`decisoes-tecnicas.md`](decisoes-tecnicas.md), que registra o oposto: decisões já **fechadas**, com o porquê de cada uma. Ali fica o que foi resolvido; aqui, o que ainda está aberto — pendências, limitações aceitas conscientemente e avisos que precisam chegar a quem não escreveu aquele trecho.
>
> Cada item traz: **o quê**, **por que importa**, **de quem é** e o **prazo** que o pressiona, quando há.
>
> Última atualização: 22/08/2026 (backlog concluído; resta o deploy).

---

## Índice

| # | Observação | De quem | Urgência |
|---|---|---|---|
| [O-01](#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada) | Chave do Gemini e cota gratuita | João Ricardo | **Alta — 13/09** |
| [O-02](#o-02-senha-do-oracle-passou-por-canal-de-conversa) | Senha do Oracle exposta | João Ricardo | Média |
| [O-03](#o-03-o-deploy-ainda-não-foi-feito-e-vale-5-pontos) | Deploy pendente | Time | **Alta — 13/09** |
| [O-04](#o-04-origemsugestao-não-pode-ser-rotulado-como-ia-quando-for-proximidade) | `origemSugestao` na tela | Bielecky e Marcela | Alta |
| [O-05](#o-05-o-botão-prateleira-vazia-precisa-travar-durante-a-requisição) | Botão de ruptura sem trava | Bielecky e Marcela | Alta |
| [O-06](#o-06-o-celular-tem-um-caminho-de-recuperação-se-a-aba-fechar) | Recuperação do handoff | Bielecky e Marcela | Média |
| [O-07](#o-07-ponto-de-interesse-some-se-a-página-recarregar) | POI não persistido | Bielecky e Marcela | Média |
| [O-08](#o-08-o-der-não-tem-a-tabela-de-registro-de-ruptura) | DER desatualizado | Vicentini | **Alta — 24/08** |
| [O-09](#o-09-a-rota-sempre-parte-do-primeiro-totem-encontrado) | Limitação multi-totem | — | Baixa |
| [O-10](#o-10-o-estoque-exibido-é-o-do-nosso-banco-e-só) | Estoque sem ERP | Time (discurso) | Média |
| [O-11](#o-11-o-swagger-fica-exposto-no-ambiente-publicado) | Swagger em produção | — | Baixa |
| [O-12](#o-12-o-raio-de-busca-da-ruptura-é-um-palpite-informado) | Raio de 25 unidades | — | Baixa |
| [O-13](#o-13-a-fase-3-inteira-continua-planejada-e-não-feita) | Fase 3 não iniciada | Time | Média |
| [O-14](#o-14-a-massa-de-dados-só-tem-um-par-de-substitutos-que-faz-sentido--resolvida) | ~~Massa com um único par de substitutos~~ | — | resolvida |
| [O-15](#o-15-o-endpoint-de-simulação-de-estoque-não-tem-proteção-nenhuma) | Simulação de estoque sem proteção | — | Baixa |
| [O-16](#o-16-o-token-continua-na-url-do-pwa-mesmo-fora-da-nossa-api) | Token na URL do PWA | Bielecky e Marcela | Média |
| [O-17](#o-17-documentos-de-trabalho-precisam-sair-antes-da-entrega-final) | Limpar documentos de trabalho | Time | fim do ano |

---

## Ações fora do código

### O-01. Chave do Gemini precisa ser trocada, e a cota gratuita é apertada

**O quê.** Gerar uma chave nova no Google AI Studio e descartar a atual. E decidir, antes de 13/09, se vale habilitar o faturamento no Google Cloud — o que **mantém o uso gratuito** e apenas eleva os limites.

**Por que importa.** Dois motivos somados:

1. A chave atual passou por conversa e teve a cota diária consumida durante o desenvolvimento.
2. O tier gratuito limita **por dia e por minuto**. No modelo de topo são **20 requisições por dia** — uma única sessão de testes esgotou. Trocamos para o `gemini-3.5-flash-lite`, que tem folga maior e responde em ~1s, mas o limite por minuto continua valendo: durante os testes de integração da ruptura, o terceiro teste seguido **caiu no fallback determinístico** por causa dele.

O fallback funcionou como projetado e o cliente recebeu resposta útil. Mas numa apresentação ao vivo, cair para "produto disponível mais próximo" justamente no momento mais forte da demonstração seria uma pena evitável.

**De quem.** João Ricardo. **Prazo que pressiona:** 13/09 (vídeo da seletiva, com o MVP funcionando).

---

### O-02. Senha do Oracle passou por canal de conversa

**O quê.** Trocar a senha do usuário `rm563609` no Oracle da FIAP.

**Por que importa.** A credencial foi compartilhada por chat durante a verificação de conectividade. Ela nunca foi escrita em arquivo nem versionada — sempre passada como variável de ambiente de linha de comando, e `backend/.gitignore` cobre `.env` e `application-local.yml`. Ainda assim, uma credencial que trafegou por um canal de conversa não deveria continuar valendo.

**De quem.** João Ricardo (é a credencial pessoal dele; cada integrante usa a sua).

---

### O-03. O deploy ainda não foi feito, e vale 5 pontos

**O quê.** Publicar a API num provedor gratuito compatível com Java — Render, Railway ou Fly.io. Vercel, sugerido pela FIAP, não hospeda bem uma aplicação Java de longa duração.

**Por que importa.** É o único card da Fase 1 que não é código: exige criar conta e publicar, o que ninguém além do time pode fazer. Foi adiado conscientemente para não travar o avanço nos diferenciais de IA, e nada no backend depende dele. Mas a rubrica dá **5 pontos ao item Deploy**, dos quais **4,5 são pela usabilidade do MVP publicado** — e o link do deploy é item obrigatório da entrega.

**Tudo do lado do código está pronto** (23/08/2026): `Dockerfile`, `.dockerignore`, `render.yaml` e o guia passo a passo em [`deploy.md`](deploy.md), com a tabela exata de variáveis. Falta a ação que só uma pessoa pode fazer: criar a conta no Render e informar as credenciais lá.

**Verificado antes:** `oracle.fiap.com.br` resolve para um IP público (187.8.12.142) e o acesso local sai de conexão residencial sem VPN — ou seja, **não há lista de IPs autorizados**. Resta uma única incógnita, que só um servidor no exterior resolve: filtro por país de origem. Se aparecer, o plano B é o Fly.io, que tem região em São Paulo.

**Ordem definida em 22/08/2026:** o deploy é o **último card**, feito depois de o backlog inteiro terminar.

**De quem.** Time. **Prazo que pressiona:** 13/09.

---

## Para a dupla de frontend (Bielecky e Marcela)

### O-04. `origemSugestao` não pode ser rotulado como IA quando for `PROXIMIDADE`

**O quê.** A resposta de `POST /api/v1/roteiro/itens/{itemId}/ruptura` traz o campo `origemSugestao`, com dois valores possíveis:

- `ASSISTENTE_IA` — o assistente avaliou os candidatos e elegeu um.
- `PROXIMIDADE` — o assistente estava fora do ar (ou respondeu fora do combinado) e o sistema caiu para o produto disponível mais próximo, **sem nenhuma análise de compatibilidade**.

**Por que importa.** Se a tela mostrar um selo de "recomendação inteligente" nos dois casos, ela vai estar afirmando algo falso — e é o tipo de detalhe que um avaliador atento consegue provocar ao vivo, bastando esgotar a cota. A justificativa que acompanha a sugestão já é honesta ("confira na embalagem se ele atende ao seu caso"); a tela só precisa não contradizê-la.

**De quem.** Bielecky e Marcela. Ver [D-38](decisoes-tecnicas.md#d-38-ruptura-de-estoque-o-modelo-escolhe-mas-quem-responde-é-o-banco).

---

### O-05. O botão "Prateleira Vazia" precisa travar durante a requisição

**O quê.** Desabilitar o botão enquanto a chamada de ruptura estiver em voo.

**Por que importa.** O endpoint **não é idempotente por decisão**: dois toques são duas visitas frustradas à prateleira, e a loja precisa das duas no registro. Mas num celular, um toque duplo acidental custa **duas chamadas ao Gemini** — e a cota gratuita é de 5 por minuto ([O-01](#o-01-chave-do-gemini-precisa-ser-trocada-e-a-cota-gratuita-é-apertada)). Duas chamadas desperdiçadas podem ser o que derruba a sugestão seguinte para o fallback bem no meio da demonstração.

**Alternativa no servidor, se o time preferir.** Devolver a mesma sugestão quando a ruptura do mesmo item foi relatada nos últimos segundos. Não foi implementado: é escopo além do card, e a defesa no frontend é a convencional. Fica registrado como opção, não como pendência.

**De quem.** Bielecky e Marcela.

---

### O-06. O celular tem um caminho de recuperação se a aba fechar

**O quê.** Depois que o QR Code é lido, o token de handoff é consumido e **não vale uma segunda vez**. Mas `GET /api/v1/sessoes/{sessaoId}/roteiro` continua devolvendo a lista **com a ordem da rota já calculada**, e não exige token nenhum.

**Por que importa.** Sem usar isso, um cliente que fecha a aba sem querer precisaria reiniciar o planejamento do zero — e ao vivo, na frente da banca, seria um desastre. Com isso, o app se recupera sozinho desde que tenha guardado o `sessaoId`.

**A segunda saída, para quando nem o `sessaoId` sobreviveu** (troca de aparelho, QR expirado antes de alguém escanear): o Totem chama `POST /api/v1/handoff` de novo para a mesma sessão e exibe um QR Code novo. O token anterior deixa de valer, e **se a caminhada já tiver começado a ordem das paradas e os itens coletados são preservados** — o cliente retoma de onde parou. Quando o erro do `POST /handoff/validate` vier com `error: "Token de Handoff Expirado"`, é exatamente esse o caminho a oferecer.

**De quem.** Bielecky e Marcela. Ver [D-29](decisoes-tecnicas.md#d-29-uso-único-do-token-pela-ausência-no-banco) e [D-44](decisoes-tecnicas.md#d-44-o-token-de-handoff-sai-da-url-e-o-qr-code-passa-a-ser-regenerável).

---

### O-07. Ponto de interesse some se a página recarregar

**O quê.** O desvio para o banheiro (UC-012) é calculado e devolvido, mas **não é persistido** — não existe tabela para ele no DER entregue, e ele não é um produto que caiba em `ItemRoteiro`.

**Por que importa.** Um reload da página perde o desvio. Se isso incomodar na prática, o app pode guardar o estado localmente; alternativamente, a [D-31](decisoes-tecnicas.md#d-31-ponto-de-interesse-não-é-persistido) descreve exatamente o que uma tabela `TB_PONTO_INTERESSE_ROTEIRO` exigiria, caso o time decida pagar esse custo.

**De quem.** Bielecky e Marcela (decidir se compensa tratar no cliente).

---

## Para o banco e os diagramas (Vicentini)

### O-08. O DER não tem a tabela de registro de ruptura

**O quê.** O card de ruptura criou `TB_REGISTRO_RUPTURA` (sessão, item, produto em falta, produto sugerido, justificativa, origem, data). A tabela **já existe no Oracle** — o `ddl-auto: update` a criou automaticamente —, então não há trabalho de banco. Falta apenas atualizar o diagrama.

**Por que importa.** O DER é um dos artefatos entregues e serve de fonte de verdade para o time. Um diagrama que não bate com o banco confunde quem chega depois e é fácil de notar numa mentoria.

**De quem.** Vicentini. **Prazo que pressiona:** 24/08 (mentoria com os representantes técnicos da Leroy Merlin).

---

### O-16. O token continua na URL do PWA, mesmo fora da nossa API

**O quê.** Limpar a URL logo depois de ler o token, com `history.replaceState`.

**Por que importa.** A Fase 3 tirou o token da URL **da nossa API** — ele agora vai no corpo de `POST /handoff/validate`. Mas o QR Code codifica a URL do PWA, `https://.../rota?token=...`, então o token continua na barra de endereço e no **histórico do navegador do cliente**, que é onde ele sobrevive por mais tempo.

Nosso lado ficou limpo; o do navegador não. Como o token vale 5 minutos e é de uso único, o risco real é baixo — mas é uma linha de código no frontend, e sem ela metade do hardening fica pela metade.

Eliminar de vez exigiria um código curto opaco com consulta separada, o que é mais escopo do que o card pedia.

**De quem.** Bielecky e Marcela. Ver [D-44](decisoes-tecnicas.md#d-44-o-token-de-handoff-sai-da-url-e-o-qr-code-passa-a-ser-regenerável).

---

### O-17. Documentos de trabalho precisam sair antes da entrega final

**O quê.** Apagar [`perguntas-mentoria.md`](perguntas-mentoria.md) — e qualquer arquivo da mesma natureza que venha a existir — antes de enviar o repositório ao portal da faculdade.

**Por que importa.** Nem tudo que ajuda o time a trabalhar pertence à documentação do produto. O arquivo de perguntas foi escrito para preparar **uma reunião específica**, em uma data específica; passada ela, só acrescenta ruído a quem for avaliar o projeto.

A distinção vale a pena manter em mente conforme o repositório cresce:

- **Permanece:** `contexto-e-planejamento.md`, `planejamento-tecnico.md`, `decisoes-tecnicas.md`, `deploy.md`, `backlog-trello-revisado.md`, os diagramas, os READMEs. Explicam o que o projeto é e por que ficou assim.
- **Sai:** artefatos amarrados a um evento pontual — roteiro de reunião, lista de perguntas, anotações de preparação.
- **Caso à parte:** este próprio `observacoes.md`. As pendências resolvidas até lá devem ter saído da lista; o que sobrar são limitações assumidas conscientemente, e **isso conta a favor** — mostra que o time sabia onde estavam os limites.

**De quem.** Time. **Quando:** antes do envio ao portal, no fim do ano.

---

## Para a demonstração

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

### O-09. A rota sempre parte do primeiro totem encontrado

O ponto de origem é o primeiro `TipoPonto.TOTEM` do banco. Numa loja com vários totens, o cliente receberia uma rota partindo do totem errado. Resolver exigiria o totem se identificar ao abrir a sessão — mudança de contrato e de UI que não se justifica no escopo atual. Ver [D-28](decisoes-tecnicas.md#d-28-a-rota-parte-do-primeiro-ponto-do-tipo-totem).

### O-10. O estoque exibido é o do nosso banco, e só

Não há integração com ERP ou WMS — não temos acesso a sistema real da Leroy Merlin, e a restrição de custo zero inviabiliza qualquer intermediário. **Vale dizer isso na apresentação antes de perguntarem**, porque a resposta é forte: o sistema resolve o problema de *localizar* o produto, e o fluxo de ruptura física existe justamente por assumir que a divergência entre sistema e prateleira **vai** acontecer — em vez de fingir que o estoque é confiável. Ver [D-23](decisoes-tecnicas.md#d-23-resolução-síncrona-de-inventário-não-é-integração-com-erp).

### O-11. O Swagger fica exposto no ambiente publicado

Decisão deliberada: a documentação navegável é o que permite ao time e aos avaliadores explorarem a API sem cliente HTTP, e não há dado sensível atrás dela. Num sistema real com dados de cliente, seria fechado. Ver [D-34](decisoes-tecnicas.md#d-34-swagger-ui-permanece-exposto-em-produção).

### O-12. O raio de busca da ruptura é um palpite informado

As 25 unidades do grid 0–100 da planta foram escolhidas por julgamento — é a ordem de grandeza de um desvio que um cliente aceita fazer a pé —, não medidas. Na massa de demonstração o valor funciona: alcança Ferragens e Elétrica a partir de Tintas, e não alcança o outro extremo da loja. Produtos isolados no mapa, como o espelho de Decoração, ficam sem vizinho e devolvem 422.

### O-15. O endpoint de simulação de estoque não tem proteção nenhuma

`PATCH /api/v1/produtos/{produtoId}/estoque` altera o catálogo e **qualquer pessoa com a URL da API publicada consegue chamar**. É o único endpoint que escreve no catálogo e o de maior impacto do sistema.

Aceito por três motivos: a API não tem autenticação em endpoint nenhum, então ele não abre uma categoria nova de exposição; desligá-lo em produção derrotaria o propósito, já que é justamente o ambiente publicado que será demonstrado; e o dano é reversível pelo próprio endpoint, sobre massa de demonstração.

O que existe no lugar de proteção: marcação explícita como `[Demonstracao]` no Swagger e log em nível **WARN** a cada alteração. **Se o projeto ganhar autenticação em algum momento, este endpoint é o primeiro que precisa entrar atrás dela.** Ver [D-40](decisoes-tecnicas.md#d-40-existe-um-endpoint-que-só-serve-à-demonstração-e-ele-é-assumidamente-desprotegido).

---

## Planejado e ainda não feito

### O-13. A Fase 3 inteira continua planejada e não feita

**Fase 3 concluída em 22/08/2026.** O rumo definido no mesmo dia era terminar o backlog primeiro e só então publicar — resta apenas o deploy ([O-03](#o-03-o-deploy-ainda-não-foi-feito-e-vale-5-pontos)), que é o último card.

1. ~~**Cron de TTL**~~ — concluído em 22/08/2026, ver [D-42](decisoes-tecnicas.md#d-42-a-varredura-de-ttl-distingue-carrinho-abandonado-de-quem-só-encostou-no-totem).
2. ~~**Refinamento 2-opt**~~ — concluído em 22/08/2026, ver [D-43](decisoes-tecnicas.md#d-43-2-opt-sobre-o-nearest-neighbor-na-variante-de-caminho-aberto). A redução contra a ordem de inserção passou de 38,6% para **41,0%**.
3. ~~**Hardening do handoff**~~ — concluído em 22/08/2026, ver [D-44](decisoes-tecnicas.md#d-44-o-token-de-handoff-sai-da-url-e-o-qr-code-passa-a-ser-regenerável). `GET /handoff/validate?token=` virou `POST /handoff/validate` com o token no corpo (**mudança quebrante**, com o aval do time), e a regeneração de QR passou a ser um caminho intencional e documentado.

**De quem.** Time, na priorização pós-13/09.

---

## Como manter este documento

Sempre que uma observação **for resolvida**, remova o item e — se ela virou uma decisão — registre-a em [`decisoes-tecnicas.md`](decisoes-tecnicas.md). Este documento só é útil enquanto tudo nele estiver de fato em aberto.

O critério para entrar: *alguém precisa agir, decidir ou ser avisado disso, e o código sozinho não conta essa história?* Se sim, anote aqui, com o dono e o prazo.
