# Backlog do backend — escopo revisado

> Nasce das duas mudanças de raiz definidas na mentoria de 24/08/2026: **sem totem** e **sem rota calculada**. Substitui as fases 2 e 3 do [`backlog-trello-revisado.md`](backlog-trello-revisado.md), que descrevem um produto que não existe mais.
>
> Base de referência: [`fluxo-do-cliente.md`](fluxo-do-cliente.md) para o caminho feliz e [`quebras-de-fluxo.md`](quebras-de-fluxo.md) para os desvios.
>
> **Cada card faz uma coisa só**, tem verificação própria e cabe num commit. A ordem importa onde está dito.

> [!NOTE]
> **Estado em 30/08/2026: os treze cards estão feitos**, entre 24 e 25/08, cada um com o seu commit e a sua verificação. Este documento fica como registro do que foi pedido e por quê.
>
> **O que veio depois deles não está aqui**, e foi mais do que os treze:
>
> **28/08 — cinco correções que só a integração com a tela revelou.** Os acentos que quebravam a ligação de 4 dos 10 cartões de setor; o corredor que a listagem não levava; a lixa da ruptura que nascia sem estoque; a busca que não comparava sem acento dos dois lados; e um 500 que devia ser 400.
>
> **28 a 30/08 — seis cards de frontend**, conduzidos por aqui porque nasceram da integração:
>
> | | O quê |
> |---|---|
> | B-1 | o roteiro guarda o id do servidor — sem isso, coletar e remover **nunca** saíam da tela |
> | B-2 | **a tela da ruptura**, a única funcionalidade que não tinha porta de entrada |
> | — | dois cortes de organização: uma função de formatar preço no lugar de oito cópias, e os metadados de setor fora da camada de serviço |
> | B-3 | o mapa para de apontar o corredor errado, e passa a admitir que não sabe |
> | G-1/G-2 | a tela para de afirmar coisas que não acontecem — dois botões mortos e cinco textos removidos |
> | — | o chat para de fabricar a resposta da IA e os produtos dela |
>
> **Os dois primeiros da fila caíram em 30/08:** o catálogo inventado do frontend foi apagado e o catálogo passou a paginar, alcançando os 111 produtos.
>
> **Tudo que resta, de todas as áreas, está em [`backlog-fechamento.md`](backlog-fechamento.md)** — cards prontos para o Trello, com verificação própria.

---

## Antes de tudo

**Commitar o teste de jornada que está pronto.** Ele exercita handoff e rota — que vão morrer nos três primeiros cards. Entrando agora, ele fica como o retrato do sistema anterior, e a remoção aparece no histórico como uma mudança consciente, não como código que sumiu.

---

## Bloco 1 — Remover o que o novo escopo dispensou

Três cards, **nesta ordem**, que não é arbitrária: o ponto de interesse depende da ordem de caminho que o handoff calcula, e o handoff é o único consumidor do algoritmo de rota. Removendo nessa sequência, nenhum estado intermediário fica quebrado — a ordem inversa deixaria o endpoint de ponto de interesse respondendo com ordem nula entre um card e outro.

### Card 1. Remover o ponto de interesse no meio da rota

Sem rota, não existe posição em que inserir um desvio. O banheiro vira mais um ponto desenhado no mapa.

**Sai:** `IncluirPontoDeInteresseUseCase`, o endpoint de pontos de interesse, o DTO de requisição, a rota do contrato e os testes.

**Preservar:** a lógica que infere a posição do cliente a partir do último item coletado vive dentro deste caso de uso e **não pode ser perdida** — ela migra para a sessão no card 5. Ficou registrada na [D-49](decisoes-tecnicas.md#d-49-o-escopo-revisado-retirou-o-totem-e-a-rota-calculada) antes da remoção.

**Verificar:** suíte verde; contrato sem a rota; a inferência de posição está copiada em local seguro antes da remoção.

### Card 2. Remover o handoff entre dispositivos

Não há mais transição entre aparelhos: a jornada inteira acontece no celular do cliente.

**Sai:** `HandoffController`, `GerarHandoffUseCase`, `ValidarHandoffUseCase`, os DTOs de handoff, `GeradorTokenHandoff` e `GeradorTokenJwt`, os campos de token em `ListaRoteiro` e na tabela, a variável `JWT_SECRET`, a dependência `jjwt` no `pom.xml`, as rotas do contrato e os testes correspondentes.

**Some junto:** o único uso de JWT no projeto.

**Cuidado:** `GerarHandoffUseCase` é quem chama o cálculo de rota. Depois deste card, o algoritmo fica sem nenhum consumidor — o que torna o card 3 uma remoção limpa.

**Verificar:** suíte verde; o contrato não descreve mais `/handoff`; `grep -ri jwt` no código de produção não devolve nada.

**Documentar:** D-49 registrando o que saiu e por quê — D-08, D-27, D-29 e D-44 passam a descrever um mecanismo que não existe mais e precisam ser marcadas como superadas.

### Card 3. Remover o cálculo de rota

O cliente decide o caminho. O sistema mostra onde ele está e onde estão os produtos.

**Sai:** `CalculadoraRota` e seus testes, o campo `ordemCaminho` de `ItemRoteiro` e da tabela, `RotaCalculadaResponse` e `PontoRotaResponse`, o tipo de ponto `TOTEM`, e as rotas do contrato que devolviam rota.

**Custo real, e vale reconhecer:** sai o Nearest Neighbor, o refinamento 2-opt e o número de vitrine de **41% de redução de percurso**. Foi trabalho bom, descartado por orientação de quem conhece a operação.

**Verificar:** suíte verde; nenhuma referência a `ordemCaminho`; a lista de compras continua sendo devolvida, agora sem ordem.

**Documentar:** D-26, D-28 e D-43 marcadas como superadas, com a razão de negócio — a loja é organizada para o cliente passar por coisas que não veio buscar, e encurtar o percurso trabalharia contra o ticket médio.

---

## Bloco 2 — A fundação do escopo novo

### Card 4. Pontos de QR Code, com código curto

O cliente entra na loja escaneando um QR afixado num corredor de passagem ou num cruzamento. Cada um carrega a posição onde está colado.

**Entra:** um tipo novo de ponto no mapa; um **código curto digitável** — algo como `TIN-04` — único, para quando o adesivo estiver rasgado, a câmera falhar ou o cliente não souber usar leitor de QR; e a consulta que resolve tanto o identificador quanto o código curto.

**Verificar:** o código curto é único e a busca por ele funciona; código inexistente devolve 404 limpo.

> **Pendente do time:** quantos QR Codes e onde. A massa acompanha a decisão; o card não depende dela para ser feito.

### Card 5. A sessão sabe onde o cliente está

**Entra:** `POST /sessoes` passa a aceitar o ponto escaneado; `Sessao` guarda a posição inicial; e a **posição atual** passa a ser exposta — vinda do último item coletado quando houver, e do ponto escaneado caso contrário.

**É aqui que a inferência de posição preservada no card 1 volta a viver**, agora como conceito próprio da sessão em vez de detalhe de um desvio de rota.

**Cuidado:** o ponto pode não existir mais — adesivo velho, loja remanejada. A sessão deve nascer **assim mesmo, sem posição**, e não recusar. Melhor um mapa sem "você está aqui" do que nenhum sistema.

**Verificar:** sessão criada com ponto válido devolve a posição; com ponto inexistente, nasce sem posição e continua utilizável; a posição acompanha o item coletado mais recente.

### Card 6. Recentrar a posição

O cliente se perde e escaneia qualquer QR da loja.

**Entra:** endpoint que atualiza a posição da sessão a partir de um ponto ou código curto, **sem criar sessão nova**.

**Verificar:** a posição muda; a lista permanece intacta; sessão encerrada recusa a operação.

### Card 7. Geometria da loja para desenhar o mapa

A tela central do produto, e a que o frontend não tem como construir sozinho.

**Entra:** blocos da loja — cada corredor um retângulo com posição, dimensões e rótulo, no mesmo grid 0–100 dos produtos — e um endpoint que devolve a planta junto dos pontos de serviço, como caixas e banheiros.

**Por que o backend serve isso:** ninguém no time desenha planta em SVG, e — mais importante — **mapa e produtos saem da mesma fonte**, então não podem divergir. Se um produto mudar de corredor, o marcador acompanha sozinho.

**Verificar:** os blocos cobrem todas as seções da massa; nenhum produto cai fora de um bloco; o endpoint responde sem tocar em dado de sessão.

### Card 8. Descrição e imagem nos produtos

A tela de detalhe hoje mostraria nome e preço num espaço vazio.

**Entra:** campos de descrição e URL de imagem em `Produto`, na tabela, nas respostas e no contrato.

**Verificar:** os dois campos chegam ao detalhe e à listagem; produto sem imagem não quebra a resposta.

> **Combinado:** as URLs vêm do site público da Leroy, coletadas pelo time. Ver [O-18](observacoes.md#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens) — o volume dessa coleta depende do tamanho que o catálogo vai ter.

### Card 9. Navegação por seção e filtros

**Entra:** endpoint que lista as seções da loja, e filtro por seção e por disponibilidade na busca de produtos, combinável com o termo de busca já existente.

**Verificar:** filtrar por seção devolve só produtos dela; o filtro de disponibilidade exclui os zerados; filtro e busca funcionam juntos; combinação sem resultado devolve página vazia, não erro.

---

## Bloco 3 — Ajustes que as quebras de fluxo exigiram

### Card 10. TTL da sessão para 4 horas

Os 30 minutos foram dimensionados para **liberar o totem** para o próximo cliente. Esse motivo desapareceu: o aparelho é do cliente.

E a sessão passou a guardar a lista inteira — um cliente que atende uma ligação de quarenta minutos hoje perde tudo, sem aviso.

**Verificar:** sessão sem interação sobrevive além de 30 minutos; a varredura continua encerrando o que de fato venceu.

**Documentar:** atualizar a D-24, que registra o TTL e o motivo dele.

### Card 11. Desmarcar item coletado

Toque por engano num celular, andando, é comum. Hoje não há volta.

**Cuidado:** a posição do cliente vem do último item coletado. Desmarcar precisa revertê-la de forma coerente — para o item marcado anterior, ou para o último QR escaneado se não houver nenhum.

**Verificar:** desmarcar reverte o item e a posição; desmarcar item já desmarcado é idempotente; desmarcar o único item coletado devolve a posição ao ponto escaneado.

### Card 12. Aceitar o substituto numa ação só

Hoje a ruptura sugere, e trocar exigiria do cliente duas ações — adicionar um produto e remover outro — em pé no corredor.

**Entra:** endpoint que aceita a sugestão: o substituto entra na lista **não coletado**, e o produto que faltou sai.

**Por que não coletado:** o substituto nem sempre está na mesma prateleira; pode estar alguns metros adiante, e o mapa é que vai dizer onde.

**É o que fecha o ciclo do produto** — a promessa é converter ruptura em venda, e enquanto aceitar der trabalho, a conversão não acontece.

**Verificar:** o substituto entra e o item em falta sai numa chamada; substituto que já estava na lista não vira duplicata; a ruptura permanece registrada.

---

## Bloco 4 — Massa de demonstração

### Card 13. Ampliar o catálogo

Vinte e nove produtos deixam a busca sem sentido, a navegação por seção vazia e o mapa ralo. Ver [O-18](observacoes.md#o-18-o-catálogo-de-29-produtos-é-pequeno-demais-para-a-banca--resolvido-no-volume-pendente-nas-imagens).

**Entra:** produtos suficientes para cada seção parecer uma seção de verdade, com descrição, e imagem ao menos nos que aparecem no roteiro da demonstração.

**Ganho de graça:** com volume, os pares de substituição aparecem sozinhos — hoje são cinco, plantados à mão.

**Cuidado:** a carga é incremental, então produtos novos chegam sozinhos aos bancos que já têm a massa antiga. Não apagar nada.

**Verificar:** cada seção tem densidade parecida; nenhum SKU duplicado; a carga roda duas vezes sem criar nada na segunda.

---

## O que NÃO precisa de card

**O fim da jornada.** O frontend já consegue saber que o último item foi coletado — a lista devolve `coletado` em cada item. A posição dos caixas vem junto do mapa (card 7). E encerrar a sessão já existe. **Nenhum trabalho de backend.**

**Limite de sugestões seguidas de substituto.** A cadeia termina sozinha: quando o raio se esgota, o sistema devolve 422. Um contador seria regra a mais para um caso que o cliente abandona antes.

**Suporte a offline.** É trabalho de frontend — guardar lista e mapa no aparelho, avisar quando cair, enfileirar as marcações. O backend não muda.

**Retomada de sessão.** O identificador fica no `localStorage`. O backend já responde se a sessão continua válida.

---

## Ordem sugerida

```
     commit do teste de jornada  (retrato do "antes")
                 |
     1  ponto de interesse  ->  2  handoff  ->  3  rota
                 |
     4  pontos de QR  ->  5  sessao com posicao  ->  6  recentrar
                 |
     7  geometria do mapa        (destrava o frontend)
     8  descricao e imagem       (destrava o frontend)
     9  secoes e filtros         (destrava o frontend)
                 |
     10 TTL   11 desmarcar   12 aceitar substituto
                 |
     13 ampliar o catalogo
```

O bloco 1 vem primeiro porque **remover é mais barato antes de construir em cima**: cada card do bloco 2 mexe em arquivos que o bloco 1 encolhe.

Os cards **7, 8 e 9** são os que o frontend espera para começar de verdade. Se houver pressa da dupla, eles podem subir na fila — não dependem dos blocos anteriores.

O card **13 fica por último** de propósito: ampliar a massa antes de os campos de descrição e imagem existirem obrigaria a mexer nela duas vezes.
