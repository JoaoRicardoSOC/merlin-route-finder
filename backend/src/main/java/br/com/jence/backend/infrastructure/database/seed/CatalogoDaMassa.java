package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.ValorDeAtributo;

import java.util.ArrayList;
import java.util.List;

import static br.com.jence.backend.domain.entity.AtributoProduto.ACABAMENTO;
import static br.com.jence.backend.domain.entity.AtributoProduto.AMPERAGEM;
import static br.com.jence.backend.domain.entity.AtributoProduto.BITOLA;
import static br.com.jence.backend.domain.entity.AtributoProduto.COMPRIMENTO;
import static br.com.jence.backend.domain.entity.AtributoProduto.COR;
import static br.com.jence.backend.domain.entity.AtributoProduto.DIMENSAO;
import static br.com.jence.backend.domain.entity.AtributoProduto.FORMATO;
import static br.com.jence.backend.domain.entity.AtributoProduto.GRAO;
import static br.com.jence.backend.domain.entity.AtributoProduto.LARGURA;
import static br.com.jence.backend.domain.entity.AtributoProduto.MARCA;
import static br.com.jence.backend.domain.entity.AtributoProduto.MATERIAL;
import static br.com.jence.backend.domain.entity.AtributoProduto.PESO;
import static br.com.jence.backend.domain.entity.AtributoProduto.POLOS;
import static br.com.jence.backend.domain.entity.AtributoProduto.POTENCIA;
import static br.com.jence.backend.domain.entity.AtributoProduto.QUANTIDADE;
import static br.com.jence.backend.domain.entity.AtributoProduto.TEMPERATURA_DE_COR;
import static br.com.jence.backend.domain.entity.AtributoProduto.TIPO;
import static br.com.jence.backend.domain.entity.AtributoProduto.VOLUME;
import static br.com.jence.backend.infrastructure.database.seed.ProdutoDaMassa.de;

/**
 * A loja Leroy Merlin em miniatura: 111 produtos distribuidos pelas dez secoes da planta.
 *
 * <ul>
 *   <li>Tintas: 12</li>
 *   <li>Ferragens: 11</li>
 *   <li>Eletrica: 11</li>
 *   <li>Encanamento: 12</li>
 *   <li>Cozinhas: 10</li>
 *   <li>Iluminacao: 11</li>
 *   <li>Jardim: 11</li>
 *   <li>Ferramentas: 12</li>
 *   <li>Decoracao: 10</li>
 *   <li>Materiais de construcao: 11</li>
 * </ul>
 *
 * <p><b>Cada produto e declarado uma vez, inteiro.</b> Nome, preco, descricao e caracteristicas
 * vivem na mesma entrada - antes estavam espalhados por dois arquivos, e acrescentar um produto
 * sem as caracteristicas nao dava erro nenhum: ele simplesmente sumia do filtro. Ver D-66.
 *
 * <p><b>O volume existe para a busca, o filtro e o mapa fazerem sentido.</b> Com cinco produtos
 * por secao, paginacao nao pagina, faceta nao filtra e corredor nao parece corredor.
 *
 * <p><b>Um unico produto nasce com estoque zero</b> - a lixa grao 120 -, e e ele que encena a
 * ruptura na demonstracao. Manter esse cenario intacto ao ampliar o catalogo e o que o teste
 * de pre-filtragem espacial protege.
 *
 * <p>As marcas sao reais e coerentes com o produto: a Leroy vende essas marcas, e um catalogo
 * com marca inventada nao se parece com uma loja.
 */
final class CatalogoDaMassa {

    private CatalogoDaMassa() {
    }

    private static final List<ProdutoDaMassa> PRODUTOS = new ArrayList<>();

    static List<ProdutoDaMassa> produtos() {
        return List.copyOf(PRODUTOS);
    }

    private static void produto(String sku, String nome, String secao, String preco, int estoque,
                                String descricao, List<ValorDeAtributo> atributos) {
        PRODUTOS.add(new ProdutoDaMassa(sku, nome, secao, preco, estoque, descricao, atributos));
    }

    static {
        // ---------------------------------------------------------------- Tintas
        produto("SKU-TIN-001", "Tinta Acrílica Fosca Branca 18L", "Tintas", "289.90", 12,
                "Tinta acrílica de acabamento fosco para paredes internas e externas. O balde de 18 litros rende cerca de 350 m² por demão e disfarça imperfeições da superfície melhor que os acabamentos brilhantes.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Tinta acrílica"),de(ACABAMENTO, "Fosco"),
                        de(COR, "Branco"),de(VOLUME, "18 L")));
        produto("SKU-TIN-002", "Rolo de Lã 23cm com Cabo", "Tintas", "34.90", 25,
                "Rolo de lã de carneiro 23 cm com cabo, para aplicar tinta acrílica ou látex em grandes áreas. A lã solta pouco pelo e não deixa marca de emenda em parede lisa.",
                List.of(de(MARCA, "Atlas"),de(TIPO, "Rolo de pintura"),de(MATERIAL, "Lã"),
                        de(LARGURA, "23 cm")));
        produto("SKU-TIN-003", "Lixa para Parede Grão 120", "Tintas", "3.50", 0,
                "Lixa de papel grão 120 para preparar parede antes da pintura. Grão médio: remove respingos e nivela massa corrida sem abrir sulcos no reboco.",
                List.of(de(MARCA, "Norton"),de(TIPO, "Lixa para parede"),de(GRAO, "120")));
        produto("SKU-TIN-004", "Lixa d'Água Grão 150", "Tintas", "4.20", 40,
                "Lixa d'água grão 150 para acabamento fino em parede, madeira e metal. Usada úmida, produz menos pó e entope menos que a lixa comum.",
                List.of(de(MARCA, "Norton"),de(TIPO, "Lixa d água"),de(GRAO, "150")));
        produto("SKU-TIN-005", "Fita Crepe 48mm x 50m", "Tintas", "12.90", 30,
                "Fita crepe 48 mm x 50 m para proteger rodapé, batente e tomada durante a pintura. Sai sem deixar resíduo se removida em até 24 horas.",
                List.of(de(MARCA, "3M"),de(TIPO, "Fita crepe"),de(LARGURA, "48 mm"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-TIN-006", "Tinta Acrílica Fosca Branca 3,6L", "Tintas", "79.90", 30,
                "Versão de 3,6 litros da acrílica fosca, para um cômodo único ou retoque. Rende cerca de 70 m² por demão.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Tinta acrílica"),de(ACABAMENTO, "Fosco"),
                        de(COR, "Branco"),de(VOLUME, "3,6 L")));
        produto("SKU-TIN-007", "Tinta Acrílica Acetinada Branca 18L", "Tintas", "329.90", 8,
                "Acabamento acetinado, que reflete um pouco de luz e resiste à limpeza com pano úmido. Indicada para cozinha, banheiro e corredor.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Tinta acrílica"),de(ACABAMENTO, "Acetinado"),
                        de(COR, "Branco"),de(VOLUME, "18 L")));
        produto("SKU-TIN-008", "Esmalte Sintético Branco Brilhante 900ml", "Tintas", "64.90", 18,
                "Esmalte à base de solvente para madeira e metal, com brilho alto. Protege portão e grade contra ferrugem.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Esmalte sintético"),
                        de(ACABAMENTO, "Brilhante"),de(COR, "Branco"),de(VOLUME, "900 ml")));
        produto("SKU-TIN-009", "Massa Corrida PVA 18L", "Tintas", "89.90", 22,
                "Massa para corrigir imperfeições de parede interna antes da pintura. Seca em cerca de 3 horas entre demãos.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Massa corrida"),de(VOLUME, "18 L")));
        produto("SKU-TIN-010", "Selador Acrílico 18L", "Tintas", "119.90", 14,
                "Uniformiza a absorção da parede antes da tinta, o que reduz o número de demãos necessárias.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Selador"),de(VOLUME, "18 L")));
        produto("SKU-TIN-011", "Bandeja para Pintura 23cm", "Tintas", "14.90", 40,
                "Bandeja plástica compatível com rolos de 23 cm, com área ondulada para tirar o excesso de tinta.",
                List.of(de(MARCA, "Atlas"),de(TIPO, "Bandeja de pintura"),de(MATERIAL, "Plástico"),
                        de(LARGURA, "23 cm")));
        produto("SKU-TIN-012", "Pincel Chato 2 Polegadas", "Tintas", "18.90", 35,
                "Pincel de cerdas naturais para cantos, batentes e áreas que o rolo não alcança.",
                List.of(de(MARCA, "Atlas"),de(TIPO, "Pincel"),de(LARGURA, "2 pol")));

        // ---------------------------------------------------------------- Ferragens
        produto("SKU-FRG-001", "Parafuso Chipboard 4x40mm - 100un", "Ferragens", "19.90", 80,
                "Caixa com 100 parafusos chipboard 4x40 mm, cabeça chata e rosca soberba. Indicados para MDF, aglomerado e madeira maciça sem necessidade de pre-furo em muitos casos.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Parafuso"),de(BITOLA, "4 mm"),
                        de(COMPRIMENTO, "40 mm"),de(QUANTIDADE, "100 un")));
        produto("SKU-FRG-002", "Bucha de Nylon 8mm - 50un", "Ferragens", "15.90", 65,
                "Pacote com 50 buchas de nylon 8 mm para fixação em alvenaria, concreto e bloco. Acompanham o furo de broca 8 mm e suportam prateleiras e suportes de TV leves.",
                List.of(de(MARCA, "Fischer"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "8 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-003", "Parafuso Chipboard 3,5x30mm - 100un", "Ferragens", "14.90", 90,
                "Caixa com 100 parafusos 3,5x30 mm para montagem de móveis e peças finas de madeira.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Parafuso"),de(BITOLA, "3,5 mm"),
                        de(COMPRIMENTO, "30 mm"),de(QUANTIDADE, "100 un")));
        produto("SKU-FRG-004", "Parafuso Chipboard 5x60mm - 50un", "Ferragens", "24.90", 55,
                "Caixa com 50 parafusos 5x60 mm, para fixações que pedem mais penetração na madeira.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Parafuso"),de(BITOLA, "5 mm"),
                        de(COMPRIMENTO, "60 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-005", "Bucha de Nylon 6mm - 50un", "Ferragens", "12.90", 70,
                "Pacote com 50 buchas 6 mm para fixações leves em alvenaria, como quadros e suportes pequenos.",
                List.of(de(MARCA, "Fischer"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "6 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-006", "Bucha de Nylon 10mm - 25un", "Ferragens", "18.90", 45,
                "Pacote com 25 buchas 10 mm para cargas maiores, como armário aéreo e mão francesa.",
                List.of(de(MARCA, "Fischer"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "10 mm"),de(QUANTIDADE, "25 un")));
        produto("SKU-FRG-007", "Dobradiça de Aço 3 Polegadas - par", "Ferragens", "22.90", 38,
                "Par de dobradiças com pino solto, que permite retirar a porta sem desparafusar a folha.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Dobradiça"),de(MATERIAL, "Aço"),
                        de(DIMENSAO, "3 pol"),de(QUANTIDADE, "2 un")));
        produto("SKU-FRG-008", "Fechadura de Embutir para Porta Interna", "Ferragens", "89.90", 20,
                "Fechadura com maçaneta e trinco para porta de cômodo, com espelho cromado.",
                List.of(de(MARCA, "Papaiz"),de(TIPO, "Fechadura"),de(ACABAMENTO, "Cromado")));
        produto("SKU-FRG-009", "Cadeado de Latão 40mm", "Ferragens", "34.90", 42,
                "Cadeado de latão maciço 40 mm com duas chaves. O latão resiste melhor a ferrugem em área externa.",
                List.of(de(MARCA, "Papaiz"),de(TIPO, "Cadeado"),de(MATERIAL, "Latão"),
                        de(DIMENSAO, "40 mm")));
        produto("SKU-FRG-010", "Suporte Mão Francesa 30cm - par", "Ferragens", "29.90", 33,
                "Par de suportes para prateleira até 30 cm de profundidade, com furos para bucha 8 mm.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Suporte de prateleira"),de(MATERIAL, "Aço"),
                        de(COMPRIMENTO, "30 cm"),de(QUANTIDADE, "2 un")));
        produto("SKU-FRG-011", "Arruela Lisa 8mm - 100un", "Ferragens", "9.90", 85,
                "Pacote com 100 arruelas 8 mm, que distribuem a pressão do parafuso e evitam marcar a peça.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Arruela"),de(MATERIAL, "Aço"),
                        de(BITOLA, "8 mm"),de(QUANTIDADE, "100 un")));

        // ---------------------------------------------------------------- Eletrica
        produto("SKU-ELE-001", "Cabo Flexível 2,5mm 100m Azul 750V Megatron", "Elétrica", "189.90", 8,
                "Rolo de 100 m de cabo flexível 2,5 mm², isolação 750 V. Bitola usada em circuitos de tomadas de uso geral em residências.",
                List.of(de(MARCA, "Megatron"),de(TIPO, "Cabo flexível"),de(BITOLA, "2,5 mm²"),
                        de(COMPRIMENTO, "100 m")));
        produto("SKU-ELE-002", "Interruptor Simples 4x2 C/ 1 Tecla 10a 250v Branco Tramontina", "Elétrica", "14.90", 50,
                "Interruptor simples de embutir, uma tecla, acabamento branco. Liga e desliga um ponto de luz a partir de um único local.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Interruptor simples"),de(COR, "Branco")));
        produto("SKU-ELE-003", "Disjuntor Bipolar 25A", "Elétrica", "42.90", 15,
                "Disjuntor termomagnético bipolar 25 A, padrão DIN. Protege circuitos de chuveiro e ar-condicionado contra sobrecarga e curto-circuito.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "25 A"),
                        de(POLOS, "Bipolar")));
        produto("SKU-ELE-004", "Cabo Flexível 1,5mm 100m", "Elétrica", "129.90", 12,
                "Rolo de 100 m de cabo 1,5 mm², bitola usada em circuitos de iluminação residencial.",
                List.of(de(MARCA, "Sil"),de(TIPO, "Cabo flexível"),de(BITOLA, "1,5 mm²"),
                        de(COMPRIMENTO, "100 m")));
        produto("SKU-ELE-005", "Cabo Flexível 4mm 50m", "Elétrica", "179.90", 9,
                "Rolo de 50 m de cabo 4 mm², para circuitos de maior corrente como chuveiro e forno.",
                List.of(de(MARCA, "Sil"),de(TIPO, "Cabo flexível"),de(BITOLA, "4 mm²"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-ELE-006", "Interruptor Paralelo Branco", "Elétrica", "19.90", 40,
                "Permite acender e apagar o mesmo ponto de luz de dois lugares diferentes, como nas pontas de um corredor.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Interruptor paralelo"),de(COR, "Branco")));
        produto("SKU-ELE-007", "Tomada 2P+T 10A Branca", "Elétrica", "16.90", 60,
                "Tomada padrão brasileiro 10 A com aterramento, para uso geral em qualquer cômodo.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Tomada"),de(AMPERAGEM, "10 A"),
                        de(COR, "Branco")));
        produto("SKU-ELE-008", "Tomada 2P+T 20A Branca", "Elétrica", "22.90", 35,
                "Tomada 20 A com aterramento, obrigatória para ar-condicionado e eletrodomésticos de maior consumo.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Tomada"),de(AMPERAGEM, "20 A"),
                        de(COR, "Branco")));
        produto("SKU-ELE-009", "Disjuntor Unipolar 16A", "Elétrica", "18.90", 48,
                "Disjuntor de um polo 16 A, padrão DIN, para proteger circuitos de tomadas de uso geral.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "16 A"),
                        de(POLOS, "Unipolar")));
        produto("SKU-ELE-010", "Disjuntor Tripolar 40A", "Elétrica", "89.90", 11,
                "Disjuntor de três polos 40 A, usado na entrada de quadros trifásicos.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "40 A"),
                        de(POLOS, "Tripolar")));
        produto("SKU-ELE-011", "Quadro de Distribuição 12 Disjuntores", "Elétrica", "159.90", 7,
                "Quadro de embutir para até 12 disjuntores DIN, com barramento de neutro e terra.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Quadro de distribuição"),
                        de(QUANTIDADE, "12 un")));

        // ---------------------------------------------------------------- Encanamento
        produto("SKU-ENC-001", "Cano PVC Soldável 25mm 6m", "Encanamento", "28.90", 35,
                "Barra de 6 m de cano PVC soldável 25 mm para água fria. Bitola mais comum em ramais de banheiro e cozinha em residências.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Cano soldável"),de(MATERIAL, "PVC"),
                        de(BITOLA, "25 mm"),de(COMPRIMENTO, "6 m")));
        produto("SKU-ENC-002", "Cola para PVC 175g", "Encanamento", "18.90", 22,
                "Adesivo plástico 175 g para soldar conexões de PVC rígido. A junta pode receber água depois de 12 horas de cura.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Adesivo para PVC"),de(PESO, "175 g")));
        produto("SKU-ENC-003", "Torneira Cromada para Banheiro", "Encanamento", "129.90", 10,
                "Torneira de mesa para lavatório de banheiro, acabamento cromado, bica baixa. Rosca padrão de 1/2 polegada.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Torneira de lavatório"),
                        de(ACABAMENTO, "Cromado"),de(BITOLA, "1/2 pol")));
        produto("SKU-ENC-004", "Sifão Sanfonado Universal", "Encanamento", "22.50", 18,
                "Sifão sanfonado universal para pia e lavatório. O corpo flexível se ajusta a distâncias diferentes entre o ralo e a parede, o que resolve instalações fora do esquadro.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Sifão sanfonado"),de(MATERIAL, "Plástico")));
        produto("SKU-ENC-005", "Sifão Copo Cromado Universal", "Encanamento", "39.90", 12,
                "Sifão copo cromado universal para pia e lavatório. O copo retém resíduos e pode ser aberto para limpeza sem desmontar a instalação.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Sifão copo"),de(MATERIAL, "Metal"),
                        de(ACABAMENTO, "Cromado")));
        produto("SKU-ENC-006", "Cano PVC Soldável 32mm 6m", "Encanamento", "39.90", 28,
                "Barra de 6 m em 32 mm, bitola usada em ramais que alimentam mais de um ponto de água.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Cano soldável"),de(MATERIAL, "PVC"),
                        de(BITOLA, "32 mm"),de(COMPRIMENTO, "6 m")));
        produto("SKU-ENC-007", "Joelho PVC Soldável 25mm - 10un", "Encanamento", "14.90", 50,
                "Pacote com 10 joelhos de 90 graus em 25 mm, para mudar a direção do ramal.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Joelho"),de(MATERIAL, "PVC"),
                        de(BITOLA, "25 mm"),de(QUANTIDADE, "10 un")));
        produto("SKU-ENC-008", "Te PVC Soldável 25mm - 10un", "Encanamento", "17.90", 44,
                "Pacote com 10 conexões em T de 25 mm, para derivar um ramal em dois.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Te"),de(MATERIAL, "PVC"),de(BITOLA, "25 mm"),
                        de(QUANTIDADE, "10 un")));
        produto("SKU-ENC-009", "Registro de Gaveta Bruto 25mm", "Encanamento", "49.90", 24,
                "Registro de gaveta para embutir na parede, com acabamento a ser instalado por cima.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Registro de gaveta"),de(MATERIAL, "Metal"),
                        de(BITOLA, "25 mm")));
        produto("SKU-ENC-010", "Válvula para Pia Inox", "Encanamento", "34.90", 30,
                "Válvula de escoamento em inox para pia de cozinha, compatível com cuba de 3,5 polegadas.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Válvula de escoamento"),de(MATERIAL, "Inox"),
                        de(BITOLA, "3,5 pol")));
        produto("SKU-ENC-011", "Fita Veda Rosca 18mm x 50m", "Encanamento", "8.90", 75,
                "Fita de vedação para roscas metálicas de água fria e quente. Enrolar no sentido da rosca antes de apertar.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Fita veda rosca"),de(LARGURA, "18 mm"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-ENC-012", "Caixa Sifonada 100x100x50mm", "Encanamento", "27.90", 26,
                "Caixa sifonada de piso com grelha, que retém resíduos e bloqueia o cheiro do esgoto.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Caixa sifonada"),de(MATERIAL, "PVC"),
                        de(DIMENSAO, "100x100x50 mm")));

        // ---------------------------------------------------------------- Cozinhas
        produto("SKU-COZ-001", "Cuba Retangular Tramontina Em Aço Inox Acetinado 56x34cm 56 Bl Com Válvula", "Cozinhas", "249.90", 6,
                "Cuba de aço inox 56x34 cm para bancada de cozinha, com válvula. Profundidade que acomoda panela grande sem respingar.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Cuba de bancada"),de(MATERIAL, "Inox"),
                        de(DIMENSAO, "56x34 cm")));
        produto("SKU-COZ-002", "Torneira Monocomando Gourmet de Pia para Cozinha com Bica Alta Flexível Cromada Tomas Delinia", "Cozinhas", "389.90", 4,
                "Torneira gourmet de mesa com bica alta móvel e acabamento cromado. A altura livre facilita encher panelas e jarras.",
                List.of(de(MARCA, "Delinia"),de(TIPO, "Torneira gourmet"),de(ACABAMENTO, "Cromado")));
        produto("SKU-COZ-003", "Cuba De Embutir Retangular 40 Bl Standard 40x34 Cm Sem Válvula Tramontina Inox", "Cozinhas", "179.90", 10,
                "Cuba de aço inox 40x34 cm, dimensão que cabe em bancadas mais estreitas sem abrir mão da profundidade.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Cuba de bancada"),de(MATERIAL, "Inox"),
                        de(DIMENSAO, "40x34 cm")));
        produto("SKU-COZ-004", "Cuba para Cozinha Dupla de Embutir ou Sobrepor em Aço Inox 304 Fosco Retangular 70x40x17cm 0,6mm 3.1/2\" Mekal", "Cozinhas", "429.90", 5,
                "Cuba dupla em inox, que permite lavar de um lado e escorrer do outro sem trocar a água.",
                List.of(de(MARCA, "Mekal"),de(TIPO, "Cuba de bancada"),de(MATERIAL, "Inox"),
                        de(DIMENSAO, "70x40x17 cm")));
        produto("SKU-COZ-005", "Torneira Misturador de Parede para Cozinha com Bica Alta Cromada Sao Delinia", "Cozinhas", "189.90", 14,
                "Torneira de parede com bica móvel, indicada quando a instalação de água sai acima da bancada.",
                List.of(de(MARCA, "Delinia"),de(TIPO, "Torneira de parede"),de(ACABAMENTO, "Cromado")));
        produto("SKU-COZ-006", "Torneira Monocomando de Pia para Cozinha com Bica Alta Extensível Preta Fosca Econocozi Jiwi", "Cozinhas", "449.90", 6,
                "Mesma bica alta móvel da versão cromada, com acabamento preto fosco.",
                List.of(de(MARCA, "Jiwi"),de(TIPO, "Torneira gourmet"),
                        de(ACABAMENTO, "Preto fosco"),de(COR, "Preto")));
        produto("SKU-COZ-007", "Lixeira Inox Escovado 5 Litros Embutir Pia Cozinha Cesto Lixo Bancada Granito Mármore Tampa Oculta", "Cozinhas", "129.90", 12,
                "Lixeira embutida no tampo, com tampa que fecha rente a bancada.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Lixeira"),de(MATERIAL, "Inox"),
                        de(VOLUME, "5 L")));
        produto("SKU-COZ-008", "Escorredor De Louças De Embutir Bandeja Inox 77x26cm Schmitt", "Cozinhas", "159.90", 9,
                "Escorredor de embutir na bancada, com bandeja de 77x26 cm que recolhe a água da louça.",
                List.of(de(MARCA, "Schmitt"),de(TIPO, "Escorredor"),de(MATERIAL, "Inox")));
        produto("SKU-COZ-009", "Puxador para Móveis Alumínio Preto Alça 128mm 4 Peças Java Inspire", "Cozinhas", "24.90", 40,
                "Jogo de 4 puxadores com furação padrão de 128 mm, compatível com a maioria das portas de armário.",
                List.of(de(MARCA, "Inspire"),de(TIPO, "Puxador"),de(MATERIAL, "Alumínio"),
                        de(DIMENSAO, "128 mm"),de(QUANTIDADE, "4 un")));
        produto("SKU-COZ-010", "Rejunte Epóxi Quartzolit Cores 1kg Cerâmica Porcelanato Branco", "Cozinhas", "79.90", 18,
                "Rejunte epóxi para área molhada de cozinha: não absorve gordura e resiste à limpeza pesada.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Rejunte epóxi"),de(COR, "Branco"),
                        de(PESO, "1 kg")));

        // ---------------------------------------------------------------- Iluminacao
        produto("SKU-ILU-001", "Lâmpada LED 9W Branca - kit 3", "Iluminação", "39.90", 60,
                "Kit com 3 lâmpadas LED 9 W, luz branca, soquete E27. Cerca de 900 lumens cada, equivalentes a lâmpadas incandescentes de 60 W.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "9 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-002", "Luminária de Embutir Quadrada", "Iluminação", "69.90", 14,
                "Luminária quadrada de embutir para forro de gesso, com recorte de 17 cm. Acompanha soquete e mola de fixação.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Luminária de embutir"),
                        de(FORMATO, "Quadrado"),de(DIMENSAO, "17x17 cm")));
        produto("SKU-ILU-003", "Lâmpada LED 12W Branca - kit 3", "Iluminação", "49.90", 35,
                "Kit com 3 lâmpadas LED 12 W, luz branca, soquete E27. Cerca de 1.250 lumens cada, indicadas para sala e cozinha.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "12 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-004", "Lâmpada LED 9W Amarela - kit 3", "Iluminação", "39.90", 45,
                "Mesma potência da versão branca, com luz amarela de 3000 K - mais aconchegante para quarto e sala.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "9 W"),
                        de(TEMPERATURA_DE_COR, "Amarela"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-005", "Lâmpada LED 15W Branca - kit 2", "Iluminação", "44.90", 28,
                "Kit com 2 lâmpadas de 15 W, cerca de 1.500 lumens cada, para cômodos grandes ou pé-direito alto.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "15 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "2 un")));
        produto("SKU-ILU-006", "Lâmpada LED Filamento 4W Âmbar", "Iluminação", "29.90", 22,
                "Lâmpada decorativa de filamento aparente, luz âmbar. Indicada para luminária de vidro transparente.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "4 W"),
                        de(TEMPERATURA_DE_COR, "Âmbar")));
        produto("SKU-ILU-007", "Luminária de Embutir Redonda", "Iluminação", "64.90", 16,
                "Versão redonda da luminária de embutir, com recorte de 17 cm no forro de gesso.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Luminária de embutir"),
                        de(FORMATO, "Redondo"),de(DIMENSAO, "17 cm")));
        produto("SKU-ILU-008", "Painel LED de Sobrepor 24W Quadrado", "Iluminação", "99.90", 13,
                "Painel de sobrepor para teto sem forro, com 24 W e luz branca uniforme.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Painel LED"),de(POTENCIA, "24 W"),
                        de(FORMATO, "Quadrado"),de(TEMPERATURA_DE_COR, "Branca")));
        produto("SKU-ILU-009", "Spot Trilho LED 7W Preto", "Iluminação", "79.90", 19,
                "Spot direcionável para trilho eletrificado, com corpo preto e foco ajustável.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Spot de trilho"),de(POTENCIA, "7 W"),
                        de(COR, "Preto")));
        produto("SKU-ILU-010", "Fita LED 5m Branca com Fonte", "Iluminação", "89.90", 21,
                "Rolo de 5 m de fita LED com fonte inclusa, para sanca de gesso ou iluminação de nicho.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Fita LED"),de(COMPRIMENTO, "5 m"),
                        de(TEMPERATURA_DE_COR, "Branca")));
        produto("SKU-ILU-011", "Arandela Externa Preta", "Iluminação", "119.90", 10,
                "Arandela com vedação para área externa, resistente à chuva. Ilumina fachada e corredor lateral.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Arandela"),de(COR, "Preto"),
                        de(MATERIAL, "Alumínio")));

        // ---------------------------------------------------------------- Jardim
        produto("SKU-JAR-001", "Vaso de Cerâmica 30cm", "Jardim", "79.90", 12,
                "Vaso de cerâmica esmaltada 30 cm de diâmetro, com furo de drenagem. Indicado para plantas de porte médio em área interna ou varanda.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Vaso"),de(MATERIAL, "Cerâmica"),
                        de(DIMENSAO, "30 cm")));
        produto("SKU-JAR-002", "Terra Vegetal 20kg", "Jardim", "24.90", 30,
                "Saco de 20 kg de terra vegetal adubada, pronta para uso em vasos, canteiros e replantio. Não precisa de correção antes do plantio.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Terra vegetal"),de(PESO, "20 kg")));
        produto("SKU-JAR-003", "Vaso de Cerâmica 20cm", "Jardim", "49.90", 20,
                "Vaso de cerâmica esmaltada 20 cm com furo de drenagem, para plantas de porte pequeno.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Vaso"),de(MATERIAL, "Cerâmica"),
                        de(DIMENSAO, "20 cm")));
        produto("SKU-JAR-004", "Vaso de Polietileno 45cm", "Jardim", "109.90", 11,
                "Vaso grande em polietileno, bem mais leve que a cerâmica no mesmo tamanho - facilita mover a planta.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Vaso"),de(MATERIAL, "Polietileno"),
                        de(DIMENSAO, "45 cm")));
        produto("SKU-JAR-005", "Substrato para Plantas 5kg", "Jardim", "16.90", 45,
                "Substrato leve para vasos, com boa drenagem. Indicado para plantas de interior.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Substrato"),de(PESO, "5 kg")));
        produto("SKU-JAR-006", "Adubo NPK 10-10-10 1kg", "Jardim", "22.90", 38,
                "Adubo mineral equilibrado para manutenção, com nitrogênio, fósforo e potássio em partes iguais.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Adubo"),de(PESO, "1 kg")));
        produto("SKU-JAR-007", "Mangueira de Jardim 20m", "Jardim", "79.90", 17,
                "Mangueira reforçada de 20 m com engate rápido, que resiste à dobra sem estrangular o fluxo.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Mangueira"),de(COMPRIMENTO, "20 m")));
        produto("SKU-JAR-008", "Regador Plástico 5L", "Jardim", "24.90", 32,
                "Regador de 5 litros com crivo removível, para regar mudas sem revolver a terra.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Regador"),de(MATERIAL, "Plástico"),
                        de(VOLUME, "5 L")));
        produto("SKU-JAR-009", "Tesoura de Poda 8 Polegadas", "Jardim", "59.90", 23,
                "Tesoura de poda com lâmina de aço carbono, para galhos de até 2 cm.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Tesoura de poda"),de(MATERIAL, "Aço"),
                        de(COMPRIMENTO, "8 pol")));
        produto("SKU-JAR-010", "Pedra Britada Decorativa Branca 20kg", "Jardim", "34.90", 26,
                "Saco de 20 kg de pedra decorativa branca, para cobrir canteiro e reduzir mato.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Pedra decorativa"),de(COR, "Branco"),
                        de(PESO, "20 kg")));
        produto("SKU-JAR-011", "Grama Sintética 2x1m", "Jardim", "89.90", 14,
                "Placa de grama sintética de 2 m², com base drenante para uso em varanda e área externa.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Grama sintética"),de(DIMENSAO, "2x1 m")));

        // ---------------------------------------------------------------- Ferramentas
        produto("SKU-FER-001", "Furadeira de Impacto 650W", "Ferramentas", "299.90", 7,
                "Furadeira de impacto 650 W com mandril de 1/2 polegada e reversão. O modo impacto perfura concreto e alvenaria; sem impacto, madeira e metal.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Furadeira de impacto"),de(POTENCIA, "650 W"),
                        de(BITOLA, "1/2 pol")));
        produto("SKU-FER-002", "Trena 5m", "Ferramentas", "24.90", 45,
                "Trena de 5 m com fita de aço, trava e clipe de cinto. Fita de 19 mm, que mantem a rigidez em medidas longas sem apoio.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Trena"),de(COMPRIMENTO, "5 m"),
                        de(LARGURA, "19 mm")));
        produto("SKU-FER-003", "Trena 7,5m", "Ferramentas", "34.90", 20,
                "Trena de 7,5 m com fita de aço, trava e clipe de cinto. O alcance extra cobre cômodos inteiros e vãos de parede numa medida só.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Trena"),de(COMPRIMENTO, "7,5 m"),
                        de(LARGURA, "25 mm")));
        produto("SKU-FER-004", "Furadeira de Impacto 850W", "Ferramentas", "429.90", 6,
                "Modelo de 850 W com mandril de 1/2 polegada, para furações longas em concreto.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Furadeira de impacto"),de(POTENCIA, "850 W"),
                        de(BITOLA, "1/2 pol")));
        produto("SKU-FER-005", "Parafusadeira a Bateria 12V", "Ferramentas", "349.90", 8,
                "Parafusadeira sem fio 12 V com bateria de lítio, para montagem de móveis sem depender de tomada.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Parafusadeira"),de(POTENCIA, "12 V")));
        produto("SKU-FER-006", "Jogo de Brocas para Concreto 5 Peças", "Ferramentas", "49.90", 30,
                "Cinco brocas com ponta de widia, de 5 a 10 mm, para alvenaria e concreto.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Jogo de brocas"),de(QUANTIDADE, "5 un")));
        produto("SKU-FER-007", "Jogo de Chaves de Fenda e Philips 6 Peças", "Ferramentas", "39.90", 34,
                "Seis chaves com cabo emborrachado, cobrindo as bitolas mais usadas em manutenção doméstica.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Jogo de chaves"),de(QUANTIDADE, "6 un")));
        produto("SKU-FER-008", "Martelo Unha 27mm com Cabo de Madeira", "Ferramentas", "44.90", 28,
                "Martelo de 27 mm com cabo de madeira, que absorve melhor o impacto do que o cabo metálico.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Martelo"),de(MATERIAL, "Madeira"),
                        de(DIMENSAO, "27 mm")));
        produto("SKU-FER-009", "Alicate Universal 8 Polegadas", "Ferramentas", "54.90", 31,
                "Alicate com área de corte e isolamento no cabo, para uso geral e pequenos serviços elétricos.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Alicate"),de(COMPRIMENTO, "8 pol")));
        produto("SKU-FER-010", "Nível de Bolha 40cm", "Ferramentas", "34.90", 29,
                "Nível de alumínio com três bolhas, para conferir horizontal, vertical e 45 graus.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Nível"),de(MATERIAL, "Alumínio"),
                        de(COMPRIMENTO, "40 cm")));
        produto("SKU-FER-011", "Serrote 20 Polegadas", "Ferramentas", "64.90", 18,
                "Serrote com dentes temperados para corte em madeira maciça e compensado.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Serrote"),de(COMPRIMENTO, "20 pol")));
        produto("SKU-FER-012", "Escada de Alumínio 5 Degraus", "Ferramentas", "279.90", 9,
                "Escada dobrável de alumínio com 5 degraus e sapatas antiderrapantes. Alcança cerca de 2,7 m.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Escada"),de(MATERIAL, "Alumínio"),
                        de(QUANTIDADE, "5 un")));

        // ---------------------------------------------------------------- Decoracao
        produto("SKU-DEC-001", "Espelho para Banheiro Redondo com LED Bivolt 60cm Gavix", "Decoração", "159.90", 9,
                "Espelho redondo de 60 cm com iluminação LED embutida, bivolt. Amplia visualmente ambientes pequenos como lavabo e corredor.",
                List.of(de(MARCA, "Gavix"),de(TIPO, "Espelho"),de(FORMATO, "Redondo"),
                        de(DIMENSAO, "60 cm")));
        produto("SKU-DEC-002", "Espelho Decorativo Redondo 40cm Preto sem Moldura Adnet Arte Própria", "Decoração", "99.90", 15,
                "Versão menor do espelho redondo, sem moldura, no formato Adnet. Cabe em lavabo estreito.",
                List.of(de(MARCA, "Arte Própria"),de(TIPO, "Espelho"),de(FORMATO, "Redondo"),
                        de(DIMENSAO, "40 cm")));
        produto("SKU-DEC-003", "Espelho Retangular Decorativo Lumina 80x60cm Corino Preto", "Decoração", "229.90", 8,
                "Espelho retangular com moldura preta, indicado para acima de bancada de banheiro.",
                List.of(de(MARCA, "Lumina"),de(TIPO, "Espelho"),de(FORMATO, "Retangular"),
                        de(DIMENSAO, "80x60 cm"),de(COR, "Preto")));
        produto("SKU-DEC-004", "Quadro Decorativo Arte Manual com Moldura Dourado Retangular com Vidro 40x60cm", "Decoração", "119.90", 12,
                "Quadro com vidro e moldura dourada, pronto para pendurar.",
                List.of(de(MARCA, "Arte Própria"),de(TIPO, "Quadro"),de(MATERIAL, "Madeira"),
                        de(DIMENSAO, "40x60 cm")));
        produto("SKU-DEC-005", "Prateleira Suspensa 60cm Parede Nicho De Madeira + Suporte", "Decoração", "69.90", 22,
                "Prateleira de 60 cm em madeira, no formato nicho, com suporte de fixação incluso.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Prateleira"),de(MATERIAL, "Madeira"),
                        de(COMPRIMENTO, "60 cm")));
        produto("SKU-DEC-006", "Cortina Blackout Alycia Cinza 2,60x1,80m 2 Folhas Inspire", "Decoração", "189.90", 10,
                "Cortina com tecido blackout, que bloqueia a maior parte da luz externa. Ideal para quarto.",
                List.of(de(MARCA, "Inspire"),de(TIPO, "Cortina"),de(COR, "Cinza"),
                        de(DIMENSAO, "2,60x1,80 m")));
        produto("SKU-DEC-007", "Tapete de Banheiro em Microfibra Retangular Bege 1 Peça Oikos", "Decoração", "89.90", 16,
                "Tapete com base emborrachada que não desliza no piso frio.",
                List.of(de(MARCA, "Oikos"),de(TIPO, "Tapete"),de(DIMENSAO, "1,20x0,60 m")));
        produto("SKU-DEC-008", "Papel De Parede Autocolante Azulejo Ladrilho Mármore Calacatta 3m", "Decoração", "79.90", 20,
                "Rolo adesivo de 3 m com padrão mármore, aplicável sobre parede lisa sem cola.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Papel de parede"),de(COMPRIMENTO, "3 m")));
        produto("SKU-DEC-009", "Cabideiro De Parede Com 5 Ganchos Para Pendurar Roupas E Bolsas Industrial Em Aço Preto", "Decoração", "49.90", 25,
                "Cabideiro de parede em aço preto, com 5 ganchos, no estilo industrial.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Cabideiro"),de(MATERIAL, "Aço"),
                        de(QUANTIDADE, "5 un")));
        produto("SKU-DEC-010", "Vaso Decorativo Vidro Tubo Transparente 25cm Único", "Decoração", "59.90", 18,
                "Vaso de vidro transparente 25 cm, para flores ou como peça isolada em aparador.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Vaso decorativo"),de(MATERIAL, "Vidro"),
                        de(DIMENSAO, "25 cm")));

        // ---------------------------------------------------------------- Materiais de construcao
        produto("SKU-MAT-001", "Argamassa AC-II 20kg", "Materiais de construção", "28.90", 50,
                "Argamassa colante AC-II, saco de 20 kg, para assentamento de cerâmica em área interna e externa. Suporta variação de temperatura e umidade.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Argamassa AC-II"),de(PESO, "20 kg")));
        produto("SKU-MAT-002", "Cimento CP-II 50kg", "Materiais de construção", "42.90", 40,
                "Saco de 50 kg de cimento Portland CP-II, de uso geral em concreto, argamassa e assentamento de blocos.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cimento CP-II"),de(PESO, "50 kg")));
        produto("SKU-MAT-003", "Argamassa AC-III 20kg", "Materiais de construção", "36.90", 28,
                "Argamassa colante AC-III, saco de 20 kg, de aderência reforçada. Indicada para porcelanato, peças grandes e assentamento sobre piso antigo.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Argamassa AC-III"),de(PESO, "20 kg")));
        produto("SKU-MAT-004", "Cimento CP-IV 50kg", "Materiais de construção", "44.90", 35,
                "Cimento pozolânico, que gera menos calor na cura - indicado para peças de concreto maiores.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cimento CP-IV"),de(PESO, "50 kg")));
        produto("SKU-MAT-005", "Cal Hidratada 20kg", "Materiais de construção", "18.90", 42,
                "Cal para argamassa de assentamento e reboco, que melhora a plasticidade da mistura.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cal hidratada"),de(PESO, "20 kg")));
        produto("SKU-MAT-006", "Areia Média Ensacada 20kg", "Materiais de construção", "14.90", 60,
                "Saco de 20 kg de areia média lavada, para argamassa e concreto em pequenas quantidades.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Areia"),de(PESO, "20 kg")));
        produto("SKU-MAT-007", "Bloco Cerâmico 9x19x39cm", "Materiais de construção", "3.90", 200,
                "Bloco cerâmico de vedação, medida padrão para paredes internas sem função estrutural.",
                List.of(de(MARCA, "Selecta"),de(TIPO, "Bloco cerâmico"),de(DIMENSAO, "9x19x39 cm")));
        produto("SKU-MAT-008", "Tijolo Maciço 5x10x20cm", "Materiais de construção", "1.90", 300,
                "Tijolo maciço para paredes que precisam de mais resistência, como churrasqueira e muro baixo.",
                List.of(de(MARCA, "Selecta"),de(TIPO, "Tijolo maciço"),de(DIMENSAO, "5x10x20 cm")));
        produto("SKU-MAT-009", "Rejunte Acrílico Cinza 1kg", "Materiais de construção", "12.90", 55,
                "Rejunte acrílico para juntas de até 3 mm em área seca, pronto para uso.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Rejunte acrílico"),de(COR, "Cinza"),
                        de(PESO, "1 kg")));
        produto("SKU-MAT-010", "Impermeabilizante Manta Líquida 18L", "Materiais de construção", "289.90", 8,
                "Manta líquida para laje e área externa, aplicada com rolo em três demãos cruzadas.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Impermeabilizante"),de(VOLUME, "18 L")));
        produto("SKU-MAT-011", "Tela Soldada para Contrapiso 2x3m", "Materiais de construção", "69.90", 19,
                "Tela de aço soldada 2x3 m, que distribui a carga e reduz trincas no contrapiso.",
                List.of(de(MARCA, "Selecta"),de(TIPO, "Tela soldada"),de(MATERIAL, "Aço"),
                        de(DIMENSAO, "2x3 m")));
    }
}
