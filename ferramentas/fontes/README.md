# As fontes do projeto

As fontes ficam em `frontend/public/fontes/` e são servidas pelo próprio app. Não há
requisição ao Google Fonts. O porquê está na [O-37](../../docs/observacoes.md).

| Arquivo | O que é | Tamanho |
|---|---|---|
| `hanken-grotesk-latin.woff2` | variável, pesos 100–900 | 34 KB |
| `hanken-grotesk-latin-ext.woff2` | idem, acentuação estendida | 19 KB |
| `material-symbols.woff2` | **só os ícones que o app usa** | 23,5 KB |

O tamanho do recorte acompanha o código e muda quando um ícone entra ou sai — ele já passou
de 19,1 KB para 23,5 KB numa única correção. **O número que vale é o que `gerar.py` imprime**,
não o desta tabela.

> [!WARNING]
> ## Ícone novo não aparece sozinho
>
> `material-symbols.woff2` é um **recorte**: contém só os ícones que o código usa, não os
> milhares que a fonte completa tem. Escrever `<span class="material-symbols-outlined">rocket</span>`
> com um ícone fora da lista **não desenha nada** — e não dá erro nenhum.
>
> **Isto já aconteceu.** O extrator do `gerar.py` aceitava apenas aspas simples, e
> `plantaInterlagos.js` é gerado com duplas: 13 ícones de seção ficaram de fora e apareceram
> como palavra escrita nos chips do mapa — *"FOREST Madeiras"*, *"BOLT Elétrica"*. O recorte
> não avisa; quem percebe é quem olha a tela.
>
> Ao acrescentar um ícone:
>
> ```bash
> python ferramentas/fontes/gerar.py
> ```
>
> O script varre o código, encontra os ícones em uso e regera o recorte. Confira o
> número que ele imprime: se subiu, o ícone novo entrou.

## Por que recortar

A fonte variável completa do Material Symbols tem **1.103 KB** e era baixada inteira, a cada
visita sem cache, para desenhar algumas dezenas de ícones. O recorte fica em torno de **2% disso**.

## Por que a variável, e não cinco pesos

O CSS do app usa peso 750 e 900, que não estavam entre os cinco pedidos ao Google
(400, 500, 600, 700, 800) e caíam no vizinho mais próximo. Um arquivo variável cobre
100 a 900 exatamente, e ainda pesa 53 KB contra os 265 KB dos cinco estáticos.

## Por que só `latin` e `latin-ext`

O app é em português. Os recortes cyrillic e vietnamese que o Google serve nunca seriam
pedidos — o `unicode-range` já os impedia de baixar — e só ocupariam espaço no
repositório.
