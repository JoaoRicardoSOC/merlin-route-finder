# Base de comparação — antes da migração

> Preenchido por quem tem acesso ao Oracle, rodando [`merlin-paridade.json`](merlin-paridade.json) contra a versão **ainda com Spring** (passo 0.3 do [plano](../plano-migracao-spring-para-jax-rs.md)). Depois da migração, a mesma coleção é rodada de novo e comparada com esta tabela (passo 11.6).
>
> **Como rodar:** importe a coleção no Insomnia, selecione o ambiente *Base*, rode a pasta *Jornada* em ordem e depois a pasta *Erros*. A descrição de cada pedido diz qual variável do ambiente preencher com a resposta (`sessaoId`, `produtoId`, `produtoSubstitutoId`, `itemId`).
>
> Na coluna **Corpo**, anote o que importa para comparar: os nomes dos campos de primeiro nível ou, nos erros, o `error` do `StandardError`. Não é preciso colar o JSON inteiro.

| Pedido | Método | Esperado | Status obtido | Corpo |
|---|---|---|---|---|
| 01 Consultar mapa | `GET` | 200 | | |
| 02 Iniciar sessao com placa | `POST` | 201 - copie o id para sessaoId | | |
| 03 Iniciar sessao sem corpo | `POST` | 201 | | |
| 04 Consultar sessao | `GET` | 200 | | |
| 05 Recentrar posicao | `PUT` | 200 | | |
| 06 Listar secoes | `GET` | 200 | | |
| 07 Buscar por termo | `GET` | 200 - copie um id para produtoId e outro para produtoSubstitutoId | | |
| 08 Buscar com filtros | `GET` | 200 | | |
| 09 Consultar produto | `GET` | 200 | | |
| 10 Consultar roteiro vazio | `GET` | 200 | | |
| 11 Adicionar item ao roteiro | `POST` | 201 - copie o id do item para itemId | | |
| 12 Consultar roteiro com item | `GET` | 200 | | |
| 13 Marcar item coletado | `PATCH` | 200 | | |
| 14 Desmarcar item coletado | `PATCH` | 200 | | |
| 15 Relatar ruptura | `POST` | 200 ou 422 | | |
| 16 Substituir item | `POST` | 200 - copie o novo id do item para itemId | | |
| 17 Historico do chat | `GET` | 200 | | |
| 18 Enviar mensagem ao chat | `POST` | 200 ou 201 | | |
| 19 Simular estoque | `PATCH` | 200 | | |
| 20 Remover item do roteiro | `DELETE` | 204 | | |
| 21 Concluir rota | `POST` | 200 | | |
| E01 UUID malformado na URL | `GET` | 400 | | |
| E02 Sessao inexistente | `GET` | 404 | | |
| E03 Produto inexistente | `GET` | 404 | | |
| E04 JSON malformado | `POST` | 400 | | |
| E05 Corpo sem campo obrigatorio | `POST` | 400 com lista errors | | |
| E06 Placa em branco | `PUT` | 400 com lista errors | | |
| E07 Placa desconhecida | `PUT` | 404 | | |
| E08 Estoque negativo | `PATCH` | 400 com lista errors | | |
| E09 Mensagem longa demais | `POST` | 400 com lista errors | | |
| E10 Parametro de pagina invalido | `GET` | 400 | | |
| E11 Metodo nao suportado | `DELETE` | 405 | | |
| E12 Content-Type nao suportado | `POST` | 415 | | |
| E13 Caminho inexistente | `GET` | 404 | | |
| E14 Coletar em sessao concluida (rodar depois do 21) | `PATCH` | 409 | | |
| E15 Query string fora de UTF-8 | `GET` | 400 | | |

## Tempo de resposta (passo 0.6)

Média de 5 chamadas seguidas, depois de uma chamada de aquecimento.

| Endpoint | Antes (ms) | Depois (ms) |
|---|---|---|
| `GET /api/v1/produtos?query=tinta` | | |
| `GET /api/v1/mapa` | | |
| `GET /api/v1/sessoes/{id}/roteiro` | | |
