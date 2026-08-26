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

## Como está — 25/08/2026

**22 de 111 preenchidos**, todos de Cozinhas, Decoração e as duas primeiras da Elétrica. Para chegar nelas foram visitadas cerca de 115 fotos: o site tem várias por produto, e aqui **fica só a primeira**, porque o produto guarda uma imagem só.

**As duas lixas continuam vazias.** `SKU-TIN-003` e `SKU-TIN-004` são as que aparecem no cenário de ruptura, o momento mais visto da apresentação — se der para pegar só mais duas, são essas.

> **Os nomes mudaram junto.** Nas 22 linhas preenchidas, a coluna *Produto* deixou de ser o nome inventado da massa e passou a ser **o nome real do produto no site**. O SKU continua o mesmo, e é ele que amarra tudo: nenhum teste e nenhuma rota dependem do nome. Aplicar esses nomes à massa é uma decisão à parte — ver *O que muda se os nomes reais entrarem*, abaixo.

## CÓD e EAN, os dois números da página do produto

A página de cada produto mostra um `CÓD.` de 8 dígitos e um `EAN:` de 13. **Nenhum dos dois muda nada no que já está construído**, mas vale saber o que são, porque um deles responde uma pergunta que a banca costuma fazer.

**O `CÓD.` é o SKU da Leroy** — o identificador interno deles, equivalente ao nosso `SKU-TIN-003`. E ele aparece dentro do endereço da própria foto: em `..._tomas_delinia_92308153_057c_600x600.png`, o `92308153` tem exatamente a forma de um CÓD. Nas 22 linhas coletadas o padrão se divide em dois: 12 endereços trazem um número de 8 dígitos como esse, e 10 trazem um de 10 dígitos que é um *timestamp* de 2019 — produtos mais antigos, cadastrados antes de a convenção mudar. **Isso não foi confirmado na página**, porque o site recusa acesso automatizado (403); é o padrão que os dados mostram, e uma olhada na página do Tomas Delinia confirma ou derruba em dois segundos.

**O `EAN` é o código de barras**, o mesmo GTIN-13 impresso na embalagem e lido no caixa. É global: o mesmo produto tem o mesmo EAN em qualquer loja de qualquer rede.

**Por que isso importa para a defesa.** Se perguntarem *"como isso conversaria com o sistema real da Leroy?"*, a resposta fica concreta: o nosso `SKU-XXX-000` é um substituto do `CÓD.` deles, e a chave de integração com fornecedor e com o caixa seria o **EAN**. Também é o que tornaria possível, num passo seguinte, apontar a câmera para o código de barras do produto na prateleira em vez de digitar o nome — o aparelho já lê QR para se localizar, e ler um EAN é o mesmo gesto.

**O que não fazemos.** Não entram campos de CÓD nem de EAN na massa. Seriam 111 números a coletar à mão para nenhuma tela mostrar, e o SKU já cumpre o papel de identificador.

## Onde isso entra

As URLs vão para o mapa `IMAGENS` em `CarregadorDadosIniciais`. Acrescentar uma URL ali chega sozinha aos bancos que já existem, inclusive o publicado: a carga completa a apresentação de quem já está gravado sem recriar nada.

## O que muda se os nomes reais entrarem

Nada quebra — **o SKU é o contrato**, e nenhum teste nem nenhuma rota depende do nome do produto. Mas duas coisas precisam entrar junto, senão a tela passa a se contradizer.

### 1. A marca do nome briga com a marca do filtro

Em **18 das 22 linhas**, o produto real é de uma marca diferente da que está gravada no atributo `MARCA`. Se só o nome mudar, o cliente filtra por *Evolux* e recebe um espelho chamado *Gavix* — e a tabela de especificações, logo abaixo da foto, diz *Evolux*.

| SKU | `MARCA` na massa | Marca no nome real |
|---|---|---|
| `SKU-COZ-002` | Docol | Delinia |
| `SKU-COZ-004` | Tramontina | Mekal |
| `SKU-COZ-005` | Docol | Delinia |
| `SKU-COZ-006` | Docol | Jiwi |
| `SKU-COZ-007` | Tramontina | (nenhuma no nome) |
| `SKU-COZ-008` | Tramontina | Schmitt |
| `SKU-COZ-009` | Ciser | Inspire |
| `SKU-DEC-001` | Evolux | Gavix |
| `SKU-DEC-002` | Evolux | Arte Própria |
| `SKU-DEC-003` | Evolux | Lumina |
| `SKU-DEC-004` | Evolux | Arte Manual |
| `SKU-DEC-005` | Evolux | (nenhuma no nome) |
| `SKU-DEC-006` | Evolux | Inspire |
| `SKU-DEC-007` | Evolux | Oikos |
| `SKU-DEC-008` | Evolux | (nenhuma no nome) |
| `SKU-DEC-009` | Evolux | (nenhuma no nome) |
| `SKU-DEC-010` | Evolux | (nenhuma no nome) |
| `SKU-ELE-001` | Sil | Megatron |

Isso não é só cosmético: `MARCA` é uma das duas chaves que ordenam o substituto quando falta produto ([D-68](decisoes-tecnicas.md#d-68-o-substituto-é-escolhido-por-semelhança-antes-de-proximidade)), e é o primeiro filtro da lista de facetas.

**A correção é acompanhar a realidade:** trocar `MARCA` pela marca do produto de verdade. O que isso custa:

- **Decoração perde a marca única.** As 10 viram 8 marcas diferentes, várias com um produto só, e cinco produtos ficam sem marca nenhuma. Parece perda, mas não é: uma faceta com **um valor que cobre tudo** nunca filtrou nada — clicar em *Evolux (10)* devolvia os mesmos 10. As facetas que a demonstração usa são grão em Tintas, amperagem na Elétrica e potência na Iluminação, e nenhuma delas encosta em Decoração.
- **Docol cai de 5 para 2**, restando em Encanamento. O teste que usa Docol só exige que a marca exista e tenha menos produtos que Tigre — continua passando.

### 2. Quatro medidas descrevem outro produto

O item escolhido no site nem sempre tem a medida que a massa inventou:

| SKU | Medida na massa | Medida do produto real |
|---|---|---|
| `SKU-COZ-001` | 56x33 cm | 56x34 cm |
| `SKU-COZ-004` | 84x40 cm | 70x40x17 cm |
| `SKU-COZ-009` | 2 un | 4 peças |
| `SKU-DEC-006` | 2,00x1,80 m | 2,60x1,80 m |


Aqui a foto entrega: uma cuba anunciada como 84x40 com a imagem de uma 70x40 é visível na tela. Os atributos precisam seguir o produto escolhido.

### 3. Os acentos funcionam, mas a busca exata deixa de achar

Os nomes reais têm acento e a massa inteira, hoje, não tem nenhum. Medido contra o Oracle da FIAP:

- **Guardar é seguro.** O banco é `AL32UTF8` e o texto acentuado volta idêntico ao que foi enviado; o Maven já compila em UTF-8.
- **Cabe.** O maior nome novo tem 109 caracteres e a coluna aceita 200; a maior URL tem 130 e a coluna aceita 500.
- **O `LIKE` para de achar.** Quem digita `flexivel` não encontra *Flexível* — para o Oracle são letras diferentes.
- **A busca tolerante segura.** O `JARO_WINKLER` entre a forma acentuada e a sem acento ficou entre **85 e 94**, e o nosso corte é 70. Ou seja: a busca continua achando, só deixa de ser por correspondência exata e passa a ser por semelhança.

Se um dia isso incomodar, existe saída medida: `convert(nome, 'US7ASCII')` dos dois lados do `LIKE` faz `flexivel` achar *Flexível*. Não vale mexer agora — não há caso falhando.

### 4. Vinte e duas de 111 é um catálogo desparelho

Aplicando só o que está coletado, a grade mistura 22 nomes longos e reais com 89 curtos e inventados — *Cuba para Cozinha Dupla de Embutir ou Sobrepor em Aço Inox 304 Fosco Retangular 70x40x17cm 0,6mm 3.1/2" Mekal* ao lado de *Trena 5m*. Vale decidir se entram agora ou quando a cobertura estiver maior.

## A lista

| SKU | Produto | Seção | URL da imagem |
|---|---|---|---|
| `SKU-COZ-001` | Cuba Retangular Tramontina Em Aco Inox Acetinado 56x34cm 56 Bl Com Valvula | Cozinhas | https://cdn.leroymerlin.com.br/products/cuba_retangular_tramontina_em_aco_inox_acetinado_56x34cm_56_b_1566754993_5d96_600x600.jpg |
| `SKU-COZ-002` | Torneira Monocomando Gourmet de Pia para Cozinha com Bica Alta Flexível Cromada Tomas Delinia | Cozinhas | https://cdn.leroymerlin.com.br/products/torneira_monocomando_de_pia_bica_alta_cromado_tomas_delinia_92308153_057c_600x600.png |
| `SKU-COZ-003` | Cuba De Embutir Retangular 40 Bl Standard 40x34 Cm Sem Válvula Tramontina Inox | Cozinhas | https://cdn.leroymerlin.com.br/products/cuba_de_embutir_retangular_40_bl_standard_40x34_cm_sem_valvul_1571090792_985a_600x600.jpg |
| `SKU-COZ-004` | Cuba para Cozinha Dupla de Embutir ou Sobrepor em Aço Inox 304 Fosco Retangular 70x40x17cm 0,6mm 3.1/2" Mekal | Cozinhas | https://cdn.leroymerlin.com.br/products/cuba_para_cozinha_dupla_de_embutir_40x17x70cm_escovado_90834422_b6be_600x600.jpg |
| `SKU-COZ-005` | Torneira Misturador de Parede para Cozinha com Bica Alta Cromada Sao Delinia | Cozinhas | https://cdn.leroymerlin.com.br/products/torneira_misturador_de_parede_bica_alta_cromada_sao_delinia_92420762_4610_600x600.jpg |
| `SKU-COZ-006` | Torneira Monocomando de Pia para Cozinha com Bica Alta Extensível Preta Fosca Econocozi Jiwi | Cozinhas | https://cdn.leroymerlin.com.br/products/torneira_monocomando_de_pia_bica_alta_preto_econocozi_jiwi_92323371_e005_600x600.JPG |
| `SKU-COZ-007` | Lixeira Inox Escovado 5 Litros Embutir Pia Cozinha Cesto Lixo Bancada Granito Marmore Tampa Oculta | Cozinhas | https://cdn.leroymerlin.com.br/products/lixeira_inox_escovado_5_litros_embutir_pia_cozinha_cesto_lixo_1572480011_ea6a_600x600.png |
| `SKU-COZ-008` | Escorredor De Louças De Embutir Bandeja Inox 77x26cm Schmitt | Cozinhas | https://cdn.leroymerlin.com.br/products/escorredor_de_loucas_de_embutir_bandeja_inox_77x26cm_schmitt_1571389776_e170_600x600.png |
| `SKU-COZ-009` | Puxador para Móveis Alumínio Preto Alça 128mm 4 Peças Java Inspire | Cozinhas | https://cdn.leroymerlin.com.br/products/puxador_de_movel_aluminio_preto_128mm_java_92315902_56bf_600x600.jpg |
| `SKU-COZ-010` | Rejunte Epoxi Quartzolit Cores 1kg Cerâmica Porcelanato Branco | Cozinhas | https://cdn.leroymerlin.com.br/products/rejunte_epoxi_quartzolit_cores_1kg_ceramica_porcelanato_branco_1567293979_78db_600x600.png |
| `SKU-DEC-001` | Espelho para Banheiro Redondo com LED Bivolt 60cm Gavix | Decoracao | https://cdn.leroymerlin.com.br/products/espelho_redondo_led_bivolt_60cm_com_led_gavix_92462440_dcc4_600x600.jpg |
| `SKU-DEC-002` | Espelho Decorativo Redondo 40cm Preto sem Moldura Adnet Arte Própria | Decoracao | https://cdn.leroymerlin.com.br/products/_92358392_fa7a_600x600.jpg |
| `SKU-DEC-003` | Espelho Retangular Decorativo Lumina 80x60cm Corino Preto | Decoracao | https://cdn.leroymerlin.com.br/products/espelho_decorativo_retangular_80x60cm_corino_lumina_92527960_0cb1_600x600.jpg |
| `SKU-DEC-004` | Quadro Decorativo Arte Manual com Moldura Dourado Retangular com Vidro 40x60cm | Decoracao | https://cdn.leroymerlin.com.br/products/quadro_arte_manual_dourado_40x60cm_arte_propria_92052016_a21b_600x600.jpg |
| `SKU-DEC-005` | Prateleira Suspensa 60cm Parede Nicho De Madeira + Suporte | Decoracao | https://cdn.leroymerlin.com.br/products/prateleira_suspensa_60cm_parede_nicho_de_madeira___suporte_1572341279_3c70_600x600.jpg |
| `SKU-DEC-006` | Cortina Blackout Alycia Cinza 2,60x1,80m 2 Folhas Inspire | Decoracao | https://cdn.leroymerlin.com.br/products/cortina_alycia_2,60x1,80m_moon_inspire_91903350_0001_600x600.jpg |
| `SKU-DEC-007` | Tapete de Banheiro em Microfibra Retangular Bege 1 Peça Oikos | Decoracao | https://cdn.leroymerlin.com.br/products/tapete_de_banheiro_em_microfibra_retangular_bege_1_peca_oikos_92425396_7e77_600x600.jpg |
| `SKU-DEC-008` | Papel De Parede Autocolante Azulejo Ladrilho Mármore Calacatta 3m | Decoracao | https://cdn.leroymerlin.com.br/products/papel_de_parede_autocolante_azulejo_ladrilho_marmore_calacatt_1570857265_fbf5_600x600.jpg |
| `SKU-DEC-009` | Cabideiro De Parede Com 5 Ganchos Para Pendurar Roupas E Bolsas Industrial Em Aço Preto | Decoracao | https://cdn.leroymerlin.com.br/products/cabideiro_de_parede_com_5_ganchos_para_pendurar_roupas_e_bols_1570051837_06ed_600x600.jpg |
| `SKU-DEC-010` | Vaso Decorativo Vidro Tubo Transparente 25cm Único | Decoracao | https://cdn.leroymerlin.com.br/products/vaso_decorativo_vidro_tubo_transparente_25cm_unico_1571745978_e474_600x600.jpg |
| `SKU-ELE-001` | Cabo Flexível 2,5mm 100m Azul 750V Megatron | Eletrica | https://cdn.leroymerlin.com.br/products/cabo_flexivel_azul_2_50_rolo_100m_87807090_0002_600x600.jpg |
| `SKU-ELE-002` | Interruptor Simples 4x2 C/ 1 Tecla 10a 250v Branco Tramontina | Eletrica | https://cdn.leroymerlin.com.br/products/interruptor_simples_4x2_c__1_tecla_10a_250v_branco_tramontina_1568977079_2a81_600x600.jpg |
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
