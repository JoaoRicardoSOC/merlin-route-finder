# Imagens dos produtos — coleta

> **Documento de trabalho.** Some antes da entrega final, junto com os outros ([O-17](observacoes.md#o-17-documentos-de-trabalho-precisam-sair-antes-da-entrega-final)).

## O que fazer

Para cada produto abaixo, achar o item equivalente no site da Leroy Merlin e colar a **URL da imagem** na última coluna.

**Como pegar a URL da imagem:** abrir a página do produto, clicar com o botão direito na foto e escolher *Copiar endereço da imagem*. O que serve é o endereço que termina em `.jpg`, `.png` ou `.webp` — **não** o endereço da página do produto.

**Não precisa ser o produto exato.** Marca e modelo não importam: a massa é de demonstração. Um rolo de lã 23 cm qualquer serve para `SKU-TIN-002`.

**Não precisa completar tudo de uma vez.** Imagem nula é um estado normal — o produto continua funcionando sem foto, e as URLs entram de forma incremental. Pode devolver a tabela com dez preenchidas e completar o resto depois.

**Prioridade, e agora ela importa mais.** O catálogo passou de 29 para 111 produtos, e coletar 111 URLs à mão não é razoável. Comece pelos que aparecem na tela durante a demonstração:

1. `SKU-TIN-003` e `SKU-TIN-004` — as duas lixas, que encenam o cenário de ruptura. **São as mais importantes de todas.**
2. Os demais produtos de **Tintas**, que é a seção do roteiro ensaiado.
3. Um punhado em cada outra seção, para a navegação não parecer vazia.

O resto pode ficar sem foto indefinidamente — imagem nula é estado normal e testado.

## Onde isso entra

As URLs vão para o mapa `IMAGENS` em `CarregadorDadosIniciais`. Acrescentar uma URL ali chega sozinha aos bancos que já existem, inclusive o publicado: a carga completa a apresentação de quem já está gravado sem recriar nada.

## A lista

| SKU | Produto | Seção | URL da imagem |
|---|---|---|---|
| `SKU-COZ-001` | Cuba Inox 56x33cm | Cozinhas |  |
| `SKU-COZ-002` | Torneira Gourmet Cromada | Cozinhas |  |
| `SKU-COZ-003` | Cuba Inox 40x34cm | Cozinhas |  |
| `SKU-COZ-004` | Cuba Dupla Inox 84x40cm | Cozinhas |  |
| `SKU-COZ-005` | Torneira de Parede para Cozinha Cromada | Cozinhas |  |
| `SKU-COZ-006` | Torneira Gourmet Preta Fosca | Cozinhas |  |
| `SKU-COZ-007` | Lixeira de Embutir para Bancada 5L | Cozinhas |  |
| `SKU-COZ-008` | Escorredor de Loucas de Embutir Inox | Cozinhas |  |
| `SKU-COZ-009` | Puxador de Aluminio 128mm - par | Cozinhas |  |
| `SKU-COZ-010` | Rejunte Epoxi Branco 1kg | Cozinhas |  |
| `SKU-DEC-001` | Espelho Redondo 60cm | Decoracao |  |
| `SKU-DEC-002` | Espelho Redondo 40cm | Decoracao |  |
| `SKU-DEC-003` | Espelho Retangular 80x60cm | Decoracao |  |
| `SKU-DEC-004` | Quadro Decorativo com Moldura 40x60cm | Decoracao |  |
| `SKU-DEC-005` | Prateleira de Madeira 60cm | Decoracao |  |
| `SKU-DEC-006` | Cortina Blackout 2,00x1,80m Cinza | Decoracao |  |
| `SKU-DEC-007` | Tapete Antiderrapante 1,20x0,60m | Decoracao |  |
| `SKU-DEC-008` | Papel de Parede Adesivo Marmore 3m | Decoracao |  |
| `SKU-DEC-009` | Cabideiro de Parede 5 Ganchos | Decoracao |  |
| `SKU-DEC-010` | Vaso Decorativo de Vidro 25cm | Decoracao |  |
| `SKU-ELE-001` | Cabo Flexivel 2,5mm 100m | Eletrica |  |
| `SKU-ELE-002` | Interruptor Simples Branco | Eletrica |  |
| `SKU-ELE-003` | Disjuntor Bipolar 25A | Eletrica |  |
| `SKU-ELE-004` | Cabo Flexivel 1,5mm 100m | Eletrica |  |
| `SKU-ELE-005` | Cabo Flexivel 4mm 50m | Eletrica |  |
| `SKU-ELE-006` | Interruptor Paralelo Branco | Eletrica |  |
| `SKU-ELE-007` | Tomada 2P+T 10A Branca | Eletrica |  |
| `SKU-ELE-008` | Tomada 2P+T 20A Branca | Eletrica |  |
| `SKU-ELE-009` | Disjuntor Unipolar 16A | Eletrica |  |
| `SKU-ELE-010` | Disjuntor Tripolar 40A | Eletrica |  |
| `SKU-ELE-011` | Quadro de Distribuicao 12 Disjuntores | Eletrica |  |
| `SKU-ENC-001` | Cano PVC Soldavel 25mm 6m | Encanamento |  |
| `SKU-ENC-002` | Cola para PVC 175g | Encanamento |  |
| `SKU-ENC-003` | Torneira Cromada para Banheiro | Encanamento |  |
| `SKU-ENC-004` | Sifao Sanfonado Universal | Encanamento |  |
| `SKU-ENC-005` | Sifao Copo Cromado Universal | Encanamento |  |
| `SKU-ENC-006` | Cano PVC Soldavel 32mm 6m | Encanamento |  |
| `SKU-ENC-007` | Joelho PVC Soldavel 25mm - 10un | Encanamento |  |
| `SKU-ENC-008` | Te PVC Soldavel 25mm - 10un | Encanamento |  |
| `SKU-ENC-009` | Registro de Gaveta Bruto 25mm | Encanamento |  |
| `SKU-ENC-010` | Valvula para Pia Inox | Encanamento |  |
| `SKU-ENC-011` | Fita Veda Rosca 18mm x 50m | Encanamento |  |
| `SKU-ENC-012` | Caixa Sifonada 100x100x50mm | Encanamento |  |
| `SKU-FRG-001` | Parafuso Chipboard 4x40mm - 100un | Ferragens |  |
| `SKU-FRG-002` | Bucha de Nylon 8mm - 50un | Ferragens |  |
| `SKU-FRG-003` | Parafuso Chipboard 3,5x30mm - 100un | Ferragens |  |
| `SKU-FRG-004` | Parafuso Chipboard 5x60mm - 50un | Ferragens |  |
| `SKU-FRG-005` | Bucha de Nylon 6mm - 50un | Ferragens |  |
| `SKU-FRG-006` | Bucha de Nylon 10mm - 25un | Ferragens |  |
| `SKU-FRG-007` | Dobradica de Aco 3 Polegadas - par | Ferragens |  |
| `SKU-FRG-008` | Fechadura de Embutir para Porta Interna | Ferragens |  |
| `SKU-FRG-009` | Cadeado de Latao 40mm | Ferragens |  |
| `SKU-FRG-010` | Suporte Mao Francesa 30cm - par | Ferragens |  |
| `SKU-FRG-011` | Arruela Lisa 8mm - 100un | Ferragens |  |
| `SKU-FER-001` | Furadeira de Impacto 650W | Ferramentas |  |
| `SKU-FER-002` | Trena 5m | Ferramentas |  |
| `SKU-FER-003` | Trena 7,5m | Ferramentas |  |
| `SKU-FER-004` | Furadeira de Impacto 850W | Ferramentas |  |
| `SKU-FER-005` | Parafusadeira a Bateria 12V | Ferramentas |  |
| `SKU-FER-006` | Jogo de Brocas para Concreto 5 Pecas | Ferramentas |  |
| `SKU-FER-007` | Jogo de Chaves de Fenda e Philips 6 Pecas | Ferramentas |  |
| `SKU-FER-008` | Martelo Unha 27mm com Cabo de Madeira | Ferramentas |  |
| `SKU-FER-009` | Alicate Universal 8 Polegadas | Ferramentas |  |
| `SKU-FER-010` | Nivel de Bolha 40cm | Ferramentas |  |
| `SKU-FER-011` | Serrote 20 Polegadas | Ferramentas |  |
| `SKU-FER-012` | Escada de Aluminio 5 Degraus | Ferramentas |  |
| `SKU-ILU-001` | Lampada LED 9W Branca - kit 3 | Iluminacao |  |
| `SKU-ILU-002` | Luminaria de Embutir Quadrada | Iluminacao |  |
| `SKU-ILU-003` | Lampada LED 12W Branca - kit 3 | Iluminacao |  |
| `SKU-ILU-004` | Lampada LED 9W Amarela - kit 3 | Iluminacao |  |
| `SKU-ILU-005` | Lampada LED 15W Branca - kit 2 | Iluminacao |  |
| `SKU-ILU-006` | Lampada LED Filamento 4W Ambar | Iluminacao |  |
| `SKU-ILU-007` | Luminaria de Embutir Redonda | Iluminacao |  |
| `SKU-ILU-008` | Painel LED de Sobrepor 24W Quadrado | Iluminacao |  |
| `SKU-ILU-009` | Spot Trilho LED 7W Preto | Iluminacao |  |
| `SKU-ILU-010` | Fita LED 5m Branca com Fonte | Iluminacao |  |
| `SKU-ILU-011` | Arandela Externa Preta | Iluminacao |  |
| `SKU-JAR-001` | Vaso de Ceramica 30cm | Jardim |  |
| `SKU-JAR-002` | Terra Vegetal 20kg | Jardim |  |
| `SKU-JAR-003` | Vaso de Ceramica 20cm | Jardim |  |
| `SKU-JAR-004` | Vaso de Polietileno 45cm | Jardim |  |
| `SKU-JAR-005` | Substrato para Plantas 5kg | Jardim |  |
| `SKU-JAR-006` | Adubo NPK 10-10-10 1kg | Jardim |  |
| `SKU-JAR-007` | Mangueira de Jardim 20m | Jardim |  |
| `SKU-JAR-008` | Regador Plastico 5L | Jardim |  |
| `SKU-JAR-009` | Tesoura de Poda 8 Polegadas | Jardim |  |
| `SKU-JAR-010` | Pedra Britada Decorativa Branca 20kg | Jardim |  |
| `SKU-JAR-011` | Grama Sintetica 2x1m | Jardim |  |
| `SKU-MAT-001` | Argamassa AC-II 20kg | Materiais de construcao |  |
| `SKU-MAT-002` | Cimento CP-II 50kg | Materiais de construcao |  |
| `SKU-MAT-003` | Argamassa AC-III 20kg | Materiais de construcao |  |
| `SKU-MAT-004` | Cimento CP-IV 50kg | Materiais de construcao |  |
| `SKU-MAT-005` | Cal Hidratada 20kg | Materiais de construcao |  |
| `SKU-MAT-006` | Areia Media Ensacada 20kg | Materiais de construcao |  |
| `SKU-MAT-007` | Bloco Ceramico 9x19x39cm | Materiais de construcao |  |
| `SKU-MAT-008` | Tijolo Macico 5x10x20cm | Materiais de construcao |  |
| `SKU-MAT-009` | Rejunte Acrilico Cinza 1kg | Materiais de construcao |  |
| `SKU-MAT-010` | Impermeabilizante Manta Liquida 18L | Materiais de construcao |  |
| `SKU-MAT-011` | Tela Soldada para Contrapiso 2x3m | Materiais de construcao |  |
| `SKU-TIN-001` | Tinta Acrilica Fosca Branca 18L | Tintas |  |
| `SKU-TIN-002` | Rolo de La 23cm com Cabo | Tintas |  |
| `SKU-TIN-003` | Lixa para Parede Grao 120 | Tintas |  |
| `SKU-TIN-004` | Lixa d'Agua Grao 150 | Tintas |  |
| `SKU-TIN-005` | Fita Crepe 48mm x 50m | Tintas |  |
| `SKU-TIN-006` | Tinta Acrilica Fosca Branca 3,6L | Tintas |  |
| `SKU-TIN-007` | Tinta Acrilica Acetinada Branca 18L | Tintas |  |
| `SKU-TIN-008` | Esmalte Sintetico Branco Brilhante 900ml | Tintas |  |
| `SKU-TIN-009` | Massa Corrida PVA 18L | Tintas |  |
| `SKU-TIN-010` | Selador Acrilico 18L | Tintas |  |
| `SKU-TIN-011` | Bandeja para Pintura 23cm | Tintas |  |
| `SKU-TIN-012` | Pincel Chato 2 Polegadas | Tintas |  |
