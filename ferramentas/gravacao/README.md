# Ferramentas da gravação

## `manter-acordado.mjs`

Mantém o backend publicado acordado enquanto o vídeo é gravado.

```bash
node ferramentas/gravacao/manter-acordado.mjs
```

Uma requisição a cada 10 minutos, com folga dentro dos ~15 que o Render espera antes de
hibernar. `Ctrl+C` encerra. Sem dependência nenhuma.

### Por que é necessário

O plano gratuito hiberna o serviço, e acordar leva de **106 a 183 segundos** nas medições que
temos. Durante o QA de 08/09 isso aconteceu **duas vezes** — inclusive no meio da execução, o
que fez todos os passos falharem ao mesmo tempo e parecerem defeito.

Numa gravação, o intervalo entre ensaiar e gravar é justamente onde a hibernação acontece.

### Escopo: a janela da gravação, não permanentemente

Manter um serviço gratuito acordado o tempo todo é abusar do plano. Ligar durante o ensaio e a
tomada, não. O script imprime há quanto tempo está no ar em cada linha, para ninguém esquecê-lo
rodando.

### O que ele bate

`GET /api/v1/produtos/secoes` — 355 ms de mediana, a resposta mais barata da API. **Não cria
sessão**, o que sujaria a métrica de carrinho abandonado ([O-20](../../docs/observacoes.md)).

### Lendo a saída

```
00:30:43  200  0.6s   (0 min no ar, 1 ok / 0 falhas)
00:40:44  200  0.5s   (10 min no ar, 2 ok / 0 falhas)
```

Uma resposta acima de **30 s** significa que o servidor tinha dormido e esta requisição o
acordou — o script avisa na linha. Se acontecer, reduza o intervalo:

```bash
INTERVALO_MIN=7 node ferramentas/gravacao/manter-acordado.mjs
```
