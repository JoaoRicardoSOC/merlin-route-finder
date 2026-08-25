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
        produto("SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", "Tintas", "289.90", 12,
                "Tinta acrilica de acabamento fosco para paredes internas e externas. O balde de 18 litros rende cerca de 350 m2 por demao e disfarca imperfeicoes da superficie melhor que os acabamentos brilhantes.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Tinta acrilica"),de(ACABAMENTO, "Fosco"),
                        de(COR, "Branco"),de(VOLUME, "18 L")));
        produto("SKU-TIN-002", "Rolo de La 23cm com Cabo", "Tintas", "34.90", 25,
                "Rolo de la de carneiro 23 cm com cabo, para aplicar tinta acrilica ou latex em grandes areas. A la solta pouco pelo e nao deixa marca de emenda em parede lisa.",
                List.of(de(MARCA, "Atlas"),de(TIPO, "Rolo de pintura"),de(MATERIAL, "La"),
                        de(LARGURA, "23 cm")));
        produto("SKU-TIN-003", "Lixa para Parede Grao 120", "Tintas", "3.50", 0,
                "Lixa de papel grao 120 para preparar parede antes da pintura. Grao medio: remove respingos e nivela massa corrida sem abrir sulcos no reboco.",
                List.of(de(MARCA, "Norton"),de(TIPO, "Lixa para parede"),de(GRAO, "120")));
        produto("SKU-TIN-004", "Lixa d'Agua Grao 150", "Tintas", "4.20", 40,
                "Lixa d'agua grao 150 para acabamento fino em parede, madeira e metal. Usada umida, produz menos po e entope menos que a lixa comum.",
                List.of(de(MARCA, "Norton"),de(TIPO, "Lixa d agua"),de(GRAO, "150")));
        produto("SKU-TIN-005", "Fita Crepe 48mm x 50m", "Tintas", "12.90", 30,
                "Fita crepe 48 mm x 50 m para proteger rodape, batente e tomada durante a pintura. Sai sem deixar residuo se removida em ate 24 horas.",
                List.of(de(MARCA, "3M"),de(TIPO, "Fita crepe"),de(LARGURA, "48 mm"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-TIN-006", "Tinta Acrilica Fosca Branca 3,6L", "Tintas", "79.90", 30,
                "Versao de 3,6 litros da acrilica fosca, para um comodo unico ou retoque. Rende cerca de 70 m2 por demao.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Tinta acrilica"),de(ACABAMENTO, "Fosco"),
                        de(COR, "Branco"),de(VOLUME, "3,6 L")));
        produto("SKU-TIN-007", "Tinta Acrilica Acetinada Branca 18L", "Tintas", "329.90", 8,
                "Acabamento acetinado, que reflete um pouco de luz e resiste a limpeza com pano umido. Indicada para cozinha, banheiro e corredor.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Tinta acrilica"),de(ACABAMENTO, "Acetinado"),
                        de(COR, "Branco"),de(VOLUME, "18 L")));
        produto("SKU-TIN-008", "Esmalte Sintetico Branco Brilhante 900ml", "Tintas", "64.90", 18,
                "Esmalte a base de solvente para madeira e metal, com brilho alto. Protege portao e grade contra ferrugem.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Esmalte sintetico"),
                        de(ACABAMENTO, "Brilhante"),de(COR, "Branco"),de(VOLUME, "900 ml")));
        produto("SKU-TIN-009", "Massa Corrida PVA 18L", "Tintas", "89.90", 22,
                "Massa para corrigir imperfeicoes de parede interna antes da pintura. Seca em cerca de 3 horas entre demaos.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Massa corrida"),de(VOLUME, "18 L")));
        produto("SKU-TIN-010", "Selador Acrilico 18L", "Tintas", "119.90", 14,
                "Uniformiza a absorcao da parede antes da tinta, o que reduz o numero de demaos necessarias.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Selador"),de(VOLUME, "18 L")));
        produto("SKU-TIN-011", "Bandeja para Pintura 23cm", "Tintas", "14.90", 40,
                "Bandeja plastica compativel com rolos de 23 cm, com area ondulada para tirar o excesso de tinta.",
                List.of(de(MARCA, "Atlas"),de(TIPO, "Bandeja de pintura"),de(MATERIAL, "Plastico"),
                        de(LARGURA, "23 cm")));
        produto("SKU-TIN-012", "Pincel Chato 2 Polegadas", "Tintas", "18.90", 35,
                "Pincel de cerdas naturais para cantos, batentes e areas que o rolo nao alcanca.",
                List.of(de(MARCA, "Atlas"),de(TIPO, "Pincel"),de(LARGURA, "2 pol")));

        // ---------------------------------------------------------------- Ferragens
        produto("SKU-FRG-001", "Parafuso Chipboard 4x40mm - 100un", "Ferragens", "19.90", 80,
                "Caixa com 100 parafusos chipboard 4x40 mm, cabeca chata e rosca soberba. Indicados para MDF, aglomerado e madeira macica sem necessidade de pre-furo em muitos casos.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Parafuso"),de(BITOLA, "4 mm"),
                        de(COMPRIMENTO, "40 mm"),de(QUANTIDADE, "100 un")));
        produto("SKU-FRG-002", "Bucha de Nylon 8mm - 50un", "Ferragens", "15.90", 65,
                "Pacote com 50 buchas de nylon 8 mm para fixacao em alvenaria, concreto e bloco. Acompanham o furo de broca 8 mm e suportam prateleiras e suportes de TV leves.",
                List.of(de(MARCA, "Fischer"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "8 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-003", "Parafuso Chipboard 3,5x30mm - 100un", "Ferragens", "14.90", 90,
                "Caixa com 100 parafusos 3,5x30 mm para montagem de moveis e pecas finas de madeira.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Parafuso"),de(BITOLA, "3,5 mm"),
                        de(COMPRIMENTO, "30 mm"),de(QUANTIDADE, "100 un")));
        produto("SKU-FRG-004", "Parafuso Chipboard 5x60mm - 50un", "Ferragens", "24.90", 55,
                "Caixa com 50 parafusos 5x60 mm, para fixacoes que pedem mais penetracao na madeira.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Parafuso"),de(BITOLA, "5 mm"),
                        de(COMPRIMENTO, "60 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-005", "Bucha de Nylon 6mm - 50un", "Ferragens", "12.90", 70,
                "Pacote com 50 buchas 6 mm para fixacoes leves em alvenaria, como quadros e suportes pequenos.",
                List.of(de(MARCA, "Fischer"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "6 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-006", "Bucha de Nylon 10mm - 25un", "Ferragens", "18.90", 45,
                "Pacote com 25 buchas 10 mm para cargas maiores, como armario aereo e mao francesa.",
                List.of(de(MARCA, "Fischer"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "10 mm"),de(QUANTIDADE, "25 un")));
        produto("SKU-FRG-007", "Dobradica de Aco 3 Polegadas - par", "Ferragens", "22.90", 38,
                "Par de dobradicas com pino solto, que permite retirar a porta sem desparafusar a folha.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Dobradica"),de(MATERIAL, "Aco"),
                        de(DIMENSAO, "3 pol"),de(QUANTIDADE, "2 un")));
        produto("SKU-FRG-008", "Fechadura de Embutir para Porta Interna", "Ferragens", "89.90", 20,
                "Fechadura com macaneta e trinco para porta de comodo, com espelho cromado.",
                List.of(de(MARCA, "Papaiz"),de(TIPO, "Fechadura"),de(ACABAMENTO, "Cromado")));
        produto("SKU-FRG-009", "Cadeado de Latao 40mm", "Ferragens", "34.90", 42,
                "Cadeado de latao macico 40 mm com duas chaves. O latao resiste melhor a ferrugem em area externa.",
                List.of(de(MARCA, "Papaiz"),de(TIPO, "Cadeado"),de(MATERIAL, "Latao"),
                        de(DIMENSAO, "40 mm")));
        produto("SKU-FRG-010", "Suporte Mao Francesa 30cm - par", "Ferragens", "29.90", 33,
                "Par de suportes para prateleira ate 30 cm de profundidade, com furos para bucha 8 mm.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Suporte de prateleira"),de(MATERIAL, "Aco"),
                        de(COMPRIMENTO, "30 cm"),de(QUANTIDADE, "2 un")));
        produto("SKU-FRG-011", "Arruela Lisa 8mm - 100un", "Ferragens", "9.90", 85,
                "Pacote com 100 arruelas 8 mm, que distribuem a pressao do parafuso e evitam marcar a peca.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Arruela"),de(MATERIAL, "Aco"),
                        de(BITOLA, "8 mm"),de(QUANTIDADE, "100 un")));

        // ---------------------------------------------------------------- Eletrica
        produto("SKU-ELE-001", "Cabo Flexivel 2,5mm 100m", "Eletrica", "189.90", 8,
                "Rolo de 100 m de cabo flexivel 2,5 mm2, isolacao 750 V. Bitola usada em circuitos de tomadas de uso geral em residencias.",
                List.of(de(MARCA, "Sil"),de(TIPO, "Cabo flexivel"),de(BITOLA, "2,5 mm2"),
                        de(COMPRIMENTO, "100 m")));
        produto("SKU-ELE-002", "Interruptor Simples Branco", "Eletrica", "14.90", 50,
                "Interruptor simples de embutir, uma tecla, acabamento branco. Liga e desliga um ponto de luz a partir de um unico local.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Interruptor simples"),de(COR, "Branco")));
        produto("SKU-ELE-003", "Disjuntor Bipolar 25A", "Eletrica", "42.90", 15,
                "Disjuntor termomagnetico bipolar 25 A, padrao DIN. Protege circuitos de chuveiro e ar-condicionado contra sobrecarga e curto-circuito.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "25 A"),
                        de(POLOS, "Bipolar")));
        produto("SKU-ELE-004", "Cabo Flexivel 1,5mm 100m", "Eletrica", "129.90", 12,
                "Rolo de 100 m de cabo 1,5 mm2, bitola usada em circuitos de iluminacao residencial.",
                List.of(de(MARCA, "Sil"),de(TIPO, "Cabo flexivel"),de(BITOLA, "1,5 mm2"),
                        de(COMPRIMENTO, "100 m")));
        produto("SKU-ELE-005", "Cabo Flexivel 4mm 50m", "Eletrica", "179.90", 9,
                "Rolo de 50 m de cabo 4 mm2, para circuitos de maior corrente como chuveiro e forno.",
                List.of(de(MARCA, "Sil"),de(TIPO, "Cabo flexivel"),de(BITOLA, "4 mm2"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-ELE-006", "Interruptor Paralelo Branco", "Eletrica", "19.90", 40,
                "Permite acender e apagar o mesmo ponto de luz de dois lugares diferentes, como nas pontas de um corredor.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Interruptor paralelo"),de(COR, "Branco")));
        produto("SKU-ELE-007", "Tomada 2P+T 10A Branca", "Eletrica", "16.90", 60,
                "Tomada padrao brasileiro 10 A com aterramento, para uso geral em qualquer comodo.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Tomada"),de(AMPERAGEM, "10 A"),
                        de(COR, "Branco")));
        produto("SKU-ELE-008", "Tomada 2P+T 20A Branca", "Eletrica", "22.90", 35,
                "Tomada 20 A com aterramento, obrigatoria para ar-condicionado e eletrodomesticos de maior consumo.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Tomada"),de(AMPERAGEM, "20 A"),
                        de(COR, "Branco")));
        produto("SKU-ELE-009", "Disjuntor Unipolar 16A", "Eletrica", "18.90", 48,
                "Disjuntor de um polo 16 A, padrao DIN, para proteger circuitos de tomadas de uso geral.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "16 A"),
                        de(POLOS, "Unipolar")));
        produto("SKU-ELE-010", "Disjuntor Tripolar 40A", "Eletrica", "89.90", 11,
                "Disjuntor de tres polos 40 A, usado na entrada de quadros trifasicos.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "40 A"),
                        de(POLOS, "Tripolar")));
        produto("SKU-ELE-011", "Quadro de Distribuicao 12 Disjuntores", "Eletrica", "159.90", 7,
                "Quadro de embutir para ate 12 disjuntores DIN, com barramento de neutro e terra.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Quadro de distribuicao"),
                        de(QUANTIDADE, "12 un")));

        // ---------------------------------------------------------------- Encanamento
        produto("SKU-ENC-001", "Cano PVC Soldavel 25mm 6m", "Encanamento", "28.90", 35,
                "Barra de 6 m de cano PVC soldavel 25 mm para agua fria. Bitola mais comum em ramais de banheiro e cozinha em residencias.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Cano soldavel"),de(MATERIAL, "PVC"),
                        de(BITOLA, "25 mm"),de(COMPRIMENTO, "6 m")));
        produto("SKU-ENC-002", "Cola para PVC 175g", "Encanamento", "18.90", 22,
                "Adesivo plastico 175 g para soldar conexoes de PVC rigido. A junta pode receber agua depois de 12 horas de cura.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Adesivo para PVC"),de(PESO, "175 g")));
        produto("SKU-ENC-003", "Torneira Cromada para Banheiro", "Encanamento", "129.90", 10,
                "Torneira de mesa para lavatorio de banheiro, acabamento cromado, bica baixa. Rosca padrao de 1/2 polegada.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Torneira de lavatorio"),
                        de(ACABAMENTO, "Cromado"),de(BITOLA, "1/2 pol")));
        produto("SKU-ENC-004", "Sifao Sanfonado Universal", "Encanamento", "22.50", 18,
                "Sifao sanfonado universal para pia e lavatorio. O corpo flexivel se ajusta a distancias diferentes entre o ralo e a parede, o que resolve instalacoes fora do esquadro.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Sifao sanfonado"),de(MATERIAL, "Plastico")));
        produto("SKU-ENC-005", "Sifao Copo Cromado Universal", "Encanamento", "39.90", 12,
                "Sifao copo cromado universal para pia e lavatorio. O copo retem residuos e pode ser aberto para limpeza sem desmontar a instalacao.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Sifao copo"),de(MATERIAL, "Metal"),
                        de(ACABAMENTO, "Cromado")));
        produto("SKU-ENC-006", "Cano PVC Soldavel 32mm 6m", "Encanamento", "39.90", 28,
                "Barra de 6 m em 32 mm, bitola usada em ramais que alimentam mais de um ponto de agua.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Cano soldavel"),de(MATERIAL, "PVC"),
                        de(BITOLA, "32 mm"),de(COMPRIMENTO, "6 m")));
        produto("SKU-ENC-007", "Joelho PVC Soldavel 25mm - 10un", "Encanamento", "14.90", 50,
                "Pacote com 10 joelhos de 90 graus em 25 mm, para mudar a direcao do ramal.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Joelho"),de(MATERIAL, "PVC"),
                        de(BITOLA, "25 mm"),de(QUANTIDADE, "10 un")));
        produto("SKU-ENC-008", "Te PVC Soldavel 25mm - 10un", "Encanamento", "17.90", 44,
                "Pacote com 10 conexoes em T de 25 mm, para derivar um ramal em dois.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Te"),de(MATERIAL, "PVC"),de(BITOLA, "25 mm"),
                        de(QUANTIDADE, "10 un")));
        produto("SKU-ENC-009", "Registro de Gaveta Bruto 25mm", "Encanamento", "49.90", 24,
                "Registro de gaveta para embutir na parede, com acabamento a ser instalado por cima.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Registro de gaveta"),de(MATERIAL, "Metal"),
                        de(BITOLA, "25 mm")));
        produto("SKU-ENC-010", "Valvula para Pia Inox", "Encanamento", "34.90", 30,
                "Valvula de escoamento em inox para pia de cozinha, compativel com cuba de 3,5 polegadas.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Valvula de escoamento"),de(MATERIAL, "Inox"),
                        de(BITOLA, "3,5 pol")));
        produto("SKU-ENC-011", "Fita Veda Rosca 18mm x 50m", "Encanamento", "8.90", 75,
                "Fita de vedacao para roscas metalicas de agua fria e quente. Enrolar no sentido da rosca antes de apertar.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Fita veda rosca"),de(LARGURA, "18 mm"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-ENC-012", "Caixa Sifonada 100x100x50mm", "Encanamento", "27.90", 26,
                "Caixa sifonada de piso com grelha, que retem residuos e bloqueia o cheiro do esgoto.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Caixa sifonada"),de(MATERIAL, "PVC"),
                        de(DIMENSAO, "100x100x50 mm")));

        // ---------------------------------------------------------------- Cozinhas
        produto("SKU-COZ-001", "Cuba Inox 56x33cm", "Cozinhas", "249.90", 6,
                "Cuba de aco inox 56x33 cm para bancada de cozinha, com valvula. Profundidade que acomoda panela grande sem respingar.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Cuba de bancada"),de(MATERIAL, "Inox"),
                        de(DIMENSAO, "56x33 cm")));
        produto("SKU-COZ-002", "Torneira Gourmet Cromada", "Cozinhas", "389.90", 4,
                "Torneira gourmet de mesa com bica alta movel e acabamento cromado. A altura livre facilita encher panelas e jarras.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Torneira gourmet"),de(ACABAMENTO, "Cromado")));
        produto("SKU-COZ-003", "Cuba Inox 40x34cm", "Cozinhas", "179.90", 10,
                "Cuba de aco inox 40x34 cm, dimensao que cabe em bancadas mais estreitas sem abrir mao da profundidade.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Cuba de bancada"),de(MATERIAL, "Inox"),
                        de(DIMENSAO, "40x34 cm")));
        produto("SKU-COZ-004", "Cuba Dupla Inox 84x40cm", "Cozinhas", "429.90", 5,
                "Cuba dupla em inox, que permite lavar de um lado e escorrer do outro sem trocar a agua.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Cuba de bancada"),de(MATERIAL, "Inox"),
                        de(DIMENSAO, "84x40 cm")));
        produto("SKU-COZ-005", "Torneira de Parede para Cozinha Cromada", "Cozinhas", "189.90", 14,
                "Torneira de parede com bica movel, indicada quando a instalacao de agua sai acima da bancada.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Torneira de parede"),de(ACABAMENTO, "Cromado")));
        produto("SKU-COZ-006", "Torneira Gourmet Preta Fosca", "Cozinhas", "449.90", 6,
                "Mesma bica alta movel da versao cromada, com acabamento preto fosco.",
                List.of(de(MARCA, "Docol"),de(TIPO, "Torneira gourmet"),
                        de(ACABAMENTO, "Preto fosco"),de(COR, "Preto")));
        produto("SKU-COZ-007", "Lixeira de Embutir para Bancada 5L", "Cozinhas", "129.90", 12,
                "Lixeira embutida no tampo, com tampa que fecha rente a bancada.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Lixeira"),de(MATERIAL, "Inox"),
                        de(VOLUME, "5 L")));
        produto("SKU-COZ-008", "Escorredor de Loucas de Embutir Inox", "Cozinhas", "159.90", 9,
                "Escorredor que encaixa sobre a cuba, liberando a bancada enquanto a louca seca.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Escorredor"),de(MATERIAL, "Inox")));
        produto("SKU-COZ-009", "Puxador de Aluminio 128mm - par", "Cozinhas", "24.90", 40,
                "Par de puxadores com furacao padrao de 128 mm, compativel com a maioria das portas de armario.",
                List.of(de(MARCA, "Ciser"),de(TIPO, "Puxador"),de(MATERIAL, "Aluminio"),
                        de(DIMENSAO, "128 mm"),de(QUANTIDADE, "2 un")));
        produto("SKU-COZ-010", "Rejunte Epoxi Branco 1kg", "Cozinhas", "79.90", 18,
                "Rejunte epoxi para area molhada de cozinha: nao absorve gordura e resiste a limpeza pesada.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Rejunte epoxi"),de(COR, "Branco"),
                        de(PESO, "1 kg")));

        // ---------------------------------------------------------------- Iluminacao
        produto("SKU-ILU-001", "Lampada LED 9W Branca - kit 3", "Iluminacao", "39.90", 60,
                "Kit com 3 lampadas LED 9 W, luz branca, soquete E27. Cerca de 900 lumens cada, equivalentes a lampadas incandescentes de 60 W.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lampada LED"),de(POTENCIA, "9 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-002", "Luminaria de Embutir Quadrada", "Iluminacao", "69.90", 14,
                "Luminaria quadrada de embutir para forro de gesso, com recorte de 17 cm. Acompanha soquete e mola de fixacao.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Luminaria de embutir"),
                        de(FORMATO, "Quadrado"),de(DIMENSAO, "17x17 cm")));
        produto("SKU-ILU-003", "Lampada LED 12W Branca - kit 3", "Iluminacao", "49.90", 35,
                "Kit com 3 lampadas LED 12 W, luz branca, soquete E27. Cerca de 1.250 lumens cada, indicadas para sala e cozinha.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lampada LED"),de(POTENCIA, "12 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-004", "Lampada LED 9W Amarela - kit 3", "Iluminacao", "39.90", 45,
                "Mesma potencia da versao branca, com luz amarela de 3000 K - mais aconchegante para quarto e sala.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lampada LED"),de(POTENCIA, "9 W"),
                        de(TEMPERATURA_DE_COR, "Amarela"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-005", "Lampada LED 15W Branca - kit 2", "Iluminacao", "44.90", 28,
                "Kit com 2 lampadas de 15 W, cerca de 1.500 lumens cada, para comodos grandes ou pe-direito alto.",
                List.of(de(MARCA, "Philips"),de(TIPO, "Lampada LED"),de(POTENCIA, "15 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "2 un")));
        produto("SKU-ILU-006", "Lampada LED Filamento 4W Ambar", "Iluminacao", "29.90", 22,
                "Lampada decorativa de filamento aparente, luz ambar. Indicada para luminaria de vidro transparente.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Lampada LED"),de(POTENCIA, "4 W"),
                        de(TEMPERATURA_DE_COR, "Ambar")));
        produto("SKU-ILU-007", "Luminaria de Embutir Redonda", "Iluminacao", "64.90", 16,
                "Versao redonda da luminaria de embutir, com recorte de 17 cm no forro de gesso.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Luminaria de embutir"),
                        de(FORMATO, "Redondo"),de(DIMENSAO, "17 cm")));
        produto("SKU-ILU-008", "Painel LED de Sobrepor 24W Quadrado", "Iluminacao", "99.90", 13,
                "Painel de sobrepor para teto sem forro, com 24 W e luz branca uniforme.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Painel LED"),de(POTENCIA, "24 W"),
                        de(FORMATO, "Quadrado"),de(TEMPERATURA_DE_COR, "Branca")));
        produto("SKU-ILU-009", "Spot Trilho LED 7W Preto", "Iluminacao", "79.90", 19,
                "Spot direcionavel para trilho eletrificado, com corpo preto e foco ajustavel.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Spot de trilho"),de(POTENCIA, "7 W"),
                        de(COR, "Preto")));
        produto("SKU-ILU-010", "Fita LED 5m Branca com Fonte", "Iluminacao", "89.90", 21,
                "Rolo de 5 m de fita LED com fonte inclusa, para sanca de gesso ou iluminacao de nicho.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Fita LED"),de(COMPRIMENTO, "5 m"),
                        de(TEMPERATURA_DE_COR, "Branca")));
        produto("SKU-ILU-011", "Arandela Externa Preta", "Iluminacao", "119.90", 10,
                "Arandela com vedacao para area externa, resistente a chuva. Ilumina fachada e corredor lateral.",
                List.of(de(MARCA, "Taschibra"),de(TIPO, "Arandela"),de(COR, "Preto"),
                        de(MATERIAL, "Aluminio")));

        // ---------------------------------------------------------------- Jardim
        produto("SKU-JAR-001", "Vaso de Ceramica 30cm", "Jardim", "79.90", 12,
                "Vaso de ceramica esmaltada 30 cm de diametro, com furo de drenagem. Indicado para plantas de porte medio em area interna ou varanda.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Vaso"),de(MATERIAL, "Ceramica"),
                        de(DIMENSAO, "30 cm")));
        produto("SKU-JAR-002", "Terra Vegetal 20kg", "Jardim", "24.90", 30,
                "Saco de 20 kg de terra vegetal adubada, pronta para uso em vasos, canteiros e replantio. Nao precisa de correcao antes do plantio.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Terra vegetal"),de(PESO, "20 kg")));
        produto("SKU-JAR-003", "Vaso de Ceramica 20cm", "Jardim", "49.90", 20,
                "Vaso de ceramica esmaltada 20 cm com furo de drenagem, para plantas de porte pequeno.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Vaso"),de(MATERIAL, "Ceramica"),
                        de(DIMENSAO, "20 cm")));
        produto("SKU-JAR-004", "Vaso de Polietileno 45cm", "Jardim", "109.90", 11,
                "Vaso grande em polietileno, bem mais leve que a ceramica no mesmo tamanho - facilita mover a planta.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Vaso"),de(MATERIAL, "Polietileno"),
                        de(DIMENSAO, "45 cm")));
        produto("SKU-JAR-005", "Substrato para Plantas 5kg", "Jardim", "16.90", 45,
                "Substrato leve para vasos, com boa drenagem. Indicado para plantas de interior.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Substrato"),de(PESO, "5 kg")));
        produto("SKU-JAR-006", "Adubo NPK 10-10-10 1kg", "Jardim", "22.90", 38,
                "Adubo mineral equilibrado para manutencao, com nitrogenio, fosforo e potassio em partes iguais.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Adubo"),de(PESO, "1 kg")));
        produto("SKU-JAR-007", "Mangueira de Jardim 20m", "Jardim", "79.90", 17,
                "Mangueira reforcada de 20 m com engate rapido, que resiste a dobra sem estrangular o fluxo.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Mangueira"),de(COMPRIMENTO, "20 m")));
        produto("SKU-JAR-008", "Regador Plastico 5L", "Jardim", "24.90", 32,
                "Regador de 5 litros com crivo removivel, para regar mudas sem revolver a terra.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Regador"),de(MATERIAL, "Plastico"),
                        de(VOLUME, "5 L")));
        produto("SKU-JAR-009", "Tesoura de Poda 8 Polegadas", "Jardim", "59.90", 23,
                "Tesoura de poda com lamina de aco carbono, para galhos de ate 2 cm.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Tesoura de poda"),de(MATERIAL, "Aco"),
                        de(COMPRIMENTO, "8 pol")));
        produto("SKU-JAR-010", "Pedra Britada Decorativa Branca 20kg", "Jardim", "34.90", 26,
                "Saco de 20 kg de pedra decorativa branca, para cobrir canteiro e reduzir mato.",
                List.of(de(MARCA, "Vitaplan"),de(TIPO, "Pedra decorativa"),de(COR, "Branco"),
                        de(PESO, "20 kg")));
        produto("SKU-JAR-011", "Grama Sintetica 2x1m", "Jardim", "89.90", 14,
                "Placa de grama sintetica de 2 m2, com base drenante para uso em varanda e area externa.",
                List.of(de(MARCA, "Vasart"),de(TIPO, "Grama sintetica"),de(DIMENSAO, "2x1 m")));

        // ---------------------------------------------------------------- Ferramentas
        produto("SKU-FER-001", "Furadeira de Impacto 650W", "Ferramentas", "299.90", 7,
                "Furadeira de impacto 650 W com mandril de 1/2 polegada e reversao. O modo impacto perfura concreto e alvenaria; sem impacto, madeira e metal.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Furadeira de impacto"),de(POTENCIA, "650 W"),
                        de(BITOLA, "1/2 pol")));
        produto("SKU-FER-002", "Trena 5m", "Ferramentas", "24.90", 45,
                "Trena de 5 m com fita de aco, trava e clipe de cinto. Fita de 19 mm, que mantem a rigidez em medidas longas sem apoio.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Trena"),de(COMPRIMENTO, "5 m"),
                        de(LARGURA, "19 mm")));
        produto("SKU-FER-003", "Trena 7,5m", "Ferramentas", "34.90", 20,
                "Trena de 7,5 m com fita de aco, trava e clipe de cinto. O alcance extra cobre comodos inteiros e vaos de parede numa medida so.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Trena"),de(COMPRIMENTO, "7,5 m"),
                        de(LARGURA, "25 mm")));
        produto("SKU-FER-004", "Furadeira de Impacto 850W", "Ferramentas", "429.90", 6,
                "Modelo de 850 W com mandril de 1/2 polegada, para furacoes longas em concreto.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Furadeira de impacto"),de(POTENCIA, "850 W"),
                        de(BITOLA, "1/2 pol")));
        produto("SKU-FER-005", "Parafusadeira a Bateria 12V", "Ferramentas", "349.90", 8,
                "Parafusadeira sem fio 12 V com bateria de litio, para montagem de moveis sem depender de tomada.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Parafusadeira"),de(POTENCIA, "12 V")));
        produto("SKU-FER-006", "Jogo de Brocas para Concreto 5 Pecas", "Ferramentas", "49.90", 30,
                "Cinco brocas com ponta de widia, de 5 a 10 mm, para alvenaria e concreto.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Jogo de brocas"),de(QUANTIDADE, "5 un")));
        produto("SKU-FER-007", "Jogo de Chaves de Fenda e Philips 6 Pecas", "Ferramentas", "39.90", 34,
                "Seis chaves com cabo emborrachado, cobrindo as bitolas mais usadas em manutencao domestica.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Jogo de chaves"),de(QUANTIDADE, "6 un")));
        produto("SKU-FER-008", "Martelo Unha 27mm com Cabo de Madeira", "Ferramentas", "44.90", 28,
                "Martelo de 27 mm com cabo de madeira, que absorve melhor o impacto do que o cabo metalico.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Martelo"),de(MATERIAL, "Madeira"),
                        de(DIMENSAO, "27 mm")));
        produto("SKU-FER-009", "Alicate Universal 8 Polegadas", "Ferramentas", "54.90", 31,
                "Alicate com area de corte e isolamento no cabo, para uso geral e pequenos servicos eletricos.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Alicate"),de(COMPRIMENTO, "8 pol")));
        produto("SKU-FER-010", "Nivel de Bolha 40cm", "Ferramentas", "34.90", 29,
                "Nivel de aluminio com tres bolhas, para conferir horizontal, vertical e 45 graus.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Nivel"),de(MATERIAL, "Aluminio"),
                        de(COMPRIMENTO, "40 cm")));
        produto("SKU-FER-011", "Serrote 20 Polegadas", "Ferramentas", "64.90", 18,
                "Serrote com dentes temperados para corte em madeira macica e compensado.",
                List.of(de(MARCA, "Stanley"),de(TIPO, "Serrote"),de(COMPRIMENTO, "20 pol")));
        produto("SKU-FER-012", "Escada de Aluminio 5 Degraus", "Ferramentas", "279.90", 9,
                "Escada dobravel de aluminio com 5 degraus e sapatas antiderrapantes. Alcanca cerca de 2,7 m.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Escada"),de(MATERIAL, "Aluminio"),
                        de(QUANTIDADE, "5 un")));

        // ---------------------------------------------------------------- Decoracao
        produto("SKU-DEC-001", "Espelho Redondo 60cm", "Decoracao", "159.90", 9,
                "Espelho redondo de 60 cm com moldura fina e sistema de fixacao incluso. Amplia visualmente ambientes pequenos como lavabo e corredor.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Espelho"),de(FORMATO, "Redondo"),
                        de(DIMENSAO, "60 cm")));
        produto("SKU-DEC-002", "Espelho Redondo 40cm", "Decoracao", "99.90", 15,
                "Versao menor do espelho redondo, com moldura fina. Cabe em lavabo estreito.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Espelho"),de(FORMATO, "Redondo"),
                        de(DIMENSAO, "40 cm")));
        produto("SKU-DEC-003", "Espelho Retangular 80x60cm", "Decoracao", "229.90", 8,
                "Espelho retangular com moldura preta, indicado para acima de bancada de banheiro.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Espelho"),de(FORMATO, "Retangular"),
                        de(DIMENSAO, "80x60 cm"),de(COR, "Preto")));
        produto("SKU-DEC-004", "Quadro Decorativo com Moldura 40x60cm", "Decoracao", "119.90", 12,
                "Quadro com vidro e moldura em madeira, pronto para pendurar.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Quadro"),de(MATERIAL, "Madeira"),
                        de(DIMENSAO, "40x60 cm")));
        produto("SKU-DEC-005", "Prateleira de Madeira 60cm", "Decoracao", "69.90", 22,
                "Prateleira de 60 cm com suportes invisiveis - a fixacao fica escondida atras da peca.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Prateleira"),de(MATERIAL, "Madeira"),
                        de(COMPRIMENTO, "60 cm")));
        produto("SKU-DEC-006", "Cortina Blackout 2,00x1,80m Cinza", "Decoracao", "189.90", 10,
                "Cortina com tecido blackout, que bloqueia a maior parte da luz externa. Ideal para quarto.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Cortina"),de(COR, "Cinza"),
                        de(DIMENSAO, "2,00x1,80 m")));
        produto("SKU-DEC-007", "Tapete Antiderrapante 1,20x0,60m", "Decoracao", "89.90", 16,
                "Tapete com base emborrachada que nao desliza no piso frio.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Tapete"),de(DIMENSAO, "1,20x0,60 m")));
        produto("SKU-DEC-008", "Papel de Parede Adesivo Marmore 3m", "Decoracao", "79.90", 20,
                "Rolo adesivo de 3 m com padrao marmore, aplicavel sobre parede lisa sem cola.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Papel de parede"),de(COMPRIMENTO, "3 m")));
        produto("SKU-DEC-009", "Cabideiro de Parede 5 Ganchos", "Decoracao", "49.90", 25,
                "Cabideiro de parede com 5 ganchos metalicos e base de madeira.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Cabideiro"),de(MATERIAL, "Madeira"),
                        de(QUANTIDADE, "5 un")));
        produto("SKU-DEC-010", "Vaso Decorativo de Vidro 25cm", "Decoracao", "59.90", 18,
                "Vaso de vidro transparente 25 cm, para flores ou como peca isolada em aparador.",
                List.of(de(MARCA, "Evolux"),de(TIPO, "Vaso decorativo"),de(MATERIAL, "Vidro"),
                        de(DIMENSAO, "25 cm")));

        // ---------------------------------------------------------------- Materiais de construcao
        produto("SKU-MAT-001", "Argamassa AC-II 20kg", "Materiais de construcao", "28.90", 50,
                "Argamassa colante AC-II, saco de 20 kg, para assentamento de ceramica em area interna e externa. Suporta variacao de temperatura e umidade.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Argamassa AC-II"),de(PESO, "20 kg")));
        produto("SKU-MAT-002", "Cimento CP-II 50kg", "Materiais de construcao", "42.90", 40,
                "Saco de 50 kg de cimento Portland CP-II, de uso geral em concreto, argamassa e assentamento de blocos.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cimento CP-II"),de(PESO, "50 kg")));
        produto("SKU-MAT-003", "Argamassa AC-III 20kg", "Materiais de construcao", "36.90", 28,
                "Argamassa colante AC-III, saco de 20 kg, de aderencia reforcada. Indicada para porcelanato, pecas grandes e assentamento sobre piso antigo.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Argamassa AC-III"),de(PESO, "20 kg")));
        produto("SKU-MAT-004", "Cimento CP-IV 50kg", "Materiais de construcao", "44.90", 35,
                "Cimento pozolanico, que gera menos calor na cura - indicado para pecas de concreto maiores.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cimento CP-IV"),de(PESO, "50 kg")));
        produto("SKU-MAT-005", "Cal Hidratada 20kg", "Materiais de construcao", "18.90", 42,
                "Cal para argamassa de assentamento e reboco, que melhora a plasticidade da mistura.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cal hidratada"),de(PESO, "20 kg")));
        produto("SKU-MAT-006", "Areia Media Ensacada 20kg", "Materiais de construcao", "14.90", 60,
                "Saco de 20 kg de areia media lavada, para argamassa e concreto em pequenas quantidades.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Areia"),de(PESO, "20 kg")));
        produto("SKU-MAT-007", "Bloco Ceramico 9x19x39cm", "Materiais de construcao", "3.90", 200,
                "Bloco ceramico de vedacao, medida padrao para paredes internas sem funcao estrutural.",
                List.of(de(MARCA, "Selecta"),de(TIPO, "Bloco ceramico"),de(DIMENSAO, "9x19x39 cm")));
        produto("SKU-MAT-008", "Tijolo Macico 5x10x20cm", "Materiais de construcao", "1.90", 300,
                "Tijolo macico para paredes que precisam de mais resistencia, como churrasqueira e muro baixo.",
                List.of(de(MARCA, "Selecta"),de(TIPO, "Tijolo macico"),de(DIMENSAO, "5x10x20 cm")));
        produto("SKU-MAT-009", "Rejunte Acrilico Cinza 1kg", "Materiais de construcao", "12.90", 55,
                "Rejunte acrilico para juntas de ate 3 mm em area seca, pronto para uso.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Rejunte acrilico"),de(COR, "Cinza"),
                        de(PESO, "1 kg")));
        produto("SKU-MAT-010", "Impermeabilizante Manta Liquida 18L", "Materiais de construcao", "289.90", 8,
                "Manta liquida para laje e area externa, aplicada com rolo em tres demaos cruzadas.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Impermeabilizante"),de(VOLUME, "18 L")));
        produto("SKU-MAT-011", "Tela Soldada para Contrapiso 2x3m", "Materiais de construcao", "69.90", 19,
                "Tela de aco soldada 2x3 m, que distribui a carga e reduz trincas no contrapiso.",
                List.of(de(MARCA, "Selecta"),de(TIPO, "Tela soldada"),de(MATERIAL, "Aco"),
                        de(DIMENSAO, "2x3 m")));
    }
}
