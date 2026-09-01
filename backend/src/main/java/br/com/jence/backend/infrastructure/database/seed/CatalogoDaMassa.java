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
 * <p><b>A lixa grao 120 nasce COM estoque, e e ela que encena a ruptura.</b> Nao ha
 * contradicao: a ruptura que o sistema trata e "o estoque dizia que tinha e a prateleira
 * estava vazia", entao o produto precisa poder entrar no roteiro antes de faltar. Produto
 * esgotado nao entra - essa regra e da tela. Ver D-72.
 *
 * <p><b>Dois produtos nascem zerados</b> - o pincel chato e a lampada amarela -, escolhidos
 * por nao participarem de nenhum par de substituicao. Sem nenhum zerado, o filtro "apenas
 * disponiveis" nao mudaria nada na tela nem teria o que afirmar no teste.
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
        produto("SKU-TIN-001", "Tinta Acrílica Fosca Super Lavável Premium Ambiente Interno Gelo 18L Coral", "Tintas", "289.90", 12,
                "Tinta acrílica de acabamento fosco para paredes internas e externas. O balde de 18 litros rende cerca de 350 m² por demão e disfarça imperfeições da superfície melhor que os acabamentos brilhantes.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Tinta acrílica"),de(ACABAMENTO, "Fosco"),
                        de(COR, "Branco"),de(VOLUME, "18 L")));
        produto("SKU-TIN-002", "Rolo Lã de Carneiro 9cm 1379 Tigre", "Tintas", "34.90", 25,
                "Rolo de lã de carneiro 23 cm com cabo, para aplicar tinta acrílica ou látex em grandes áreas. A lã solta pouco pelo e não deixa marca de emenda em parede lisa.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Rolo de pintura"),de(MATERIAL, "Lã"),
                        de(LARGURA, "23 cm")));
        produto("SKU-TIN-003", "Lixa para Parede Grão 120", "Tintas", "3.50", 4,
                "Lixa de papel grão 120 para preparar parede antes da pintura. Grão médio: remove respingos e nivela massa corrida sem abrir sulcos no reboco.",
                List.of(de(MARCA, "WBR"),de(TIPO, "Lixa para parede"),de(GRAO, "120")));
        produto("SKU-TIN-004", "Lixa d'Água Grão 150", "Tintas", "4.20", 40,
                "Lixa d'água grão 150 para acabamento fino em parede, madeira e metal. Usada úmida, produz menos pó e entope menos que a lixa comum.",
                List.of(de(MARCA, "Dexter"),de(TIPO, "Lixa d água"),de(GRAO, "150")));
        produto("SKU-TIN-005", "Fita Crepe 48mm X 50m - Tigre", "Tintas", "12.90", 30,
                "Fita crepe 48 mm x 50 m para proteger rodapé, batente e tomada durante a pintura. Sai sem deixar resíduo se removida em até 24 horas.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Fita crepe"),de(LARGURA, "48 mm"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-TIN-006", "Tinta Acrílica Fosca Klasse Econômica Interior Branca 3,6L Qualyvinil", "Tintas", "79.90", 30,
                "Versão de 3,6 litros da acrílica fosca, para um cômodo único ou retoque. Rende cerca de 70 m² por demão.",
                List.of(de(MARCA, "Qualyvinil"),de(TIPO, "Tinta acrílica"),de(ACABAMENTO, "Fosco"),
                        de(COR, "Branco"),de(VOLUME, "3,6 L")));
        produto("SKU-TIN-007", "Tinta Acrílica Anti Bactéria e Mofo Semi Acetinada Super Lavável Premium Ambientes Internos Branca 20L Coral", "Tintas", "329.90", 8,
                "Acabamento acetinado, que reflete um pouco de luz e resiste à limpeza com pano úmido. Indicada para cozinha, banheiro e corredor.",
                List.of(de(MARCA, "Coral"),de(TIPO, "Tinta acrílica"),de(ACABAMENTO, "Acetinado"),
                        de(COR, "Branco"),de(VOLUME, "18 L")));
        produto("SKU-TIN-008", "Esmalte Sintético Standard Maza Branco Brilhante 900ml", "Tintas", "64.90", 18,
                "Esmalte à base de solvente para madeira e metal, com brilho alto. Protege portão e grade contra ferrugem.",
                List.of(de(MARCA, "Maza"),de(TIPO, "Esmalte sintético"),
                        de(ACABAMENTO, "Brilhante"),de(COR, "Branco"),de(VOLUME, "900 ml")));
        produto("SKU-TIN-009", "Massa Pva Corrida Eucatex Balde 25kg", "Tintas", "89.90", 22,
                "Massa para corrigir imperfeições de parede interna antes da pintura. Seca em cerca de 3 horas entre demãos.",
                List.of(de(MARCA, "Eucatex"),de(TIPO, "Massa corrida"),de(VOLUME, "18 L")));
        produto("SKU-TIN-010", "Selador Acrílico Pré-pintura 18l Suvinil", "Tintas", "119.90", 14,
                "Uniformiza a absorção da parede antes da tinta, o que reduz o número de demãos necessárias.",
                List.of(de(MARCA, "Suvinil"),de(TIPO, "Selador"),de(VOLUME, "18 L")));
        produto("SKU-TIN-011", "Bandeja Plástica Preta 23cm Tigre", "Tintas", "14.90", 40,
                "Bandeja plástica compatível com rolos de 23 cm, com área ondulada para tirar o excesso de tinta.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Bandeja de pintura"),de(MATERIAL, "Plástico"),
                        de(LARGURA, "23 cm")));
        produto("SKU-TIN-012", "Pincel Chato Tigre 815 - 2 Embalagem Com 12 Unidades", "Tintas", "18.90", 0,
                "Pincel de cerdas naturais para cantos, batentes e áreas que o rolo não alcança.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Pincel"),de(LARGURA, "2 pol")));

        // ---------------------------------------------------------------- Ferragens
        produto("SKU-FRG-001", "Parafuso Chipboard Cabeça Chata 4x40mm 20 Peças Standers", "Ferragens", "19.90", 80,
                "Caixa com 100 parafusos chipboard 4x40 mm, cabeça chata e rosca soberba. Indicados para MDF, aglomerado e madeira maciça sem necessidade de pre-furo em muitos casos.",
                List.of(de(MARCA, "Standers"),de(TIPO, "Parafuso"),de(BITOLA, "4 mm"),
                        de(COMPRIMENTO, "40 mm"),de(QUANTIDADE, "100 un")));
        produto("SKU-FRG-002", "Bucha Nylon 8mm para Bases Maciças SF com Parafuso 15 peças Standers", "Ferragens", "15.90", 65,
                "Pacote com 50 buchas de nylon 8 mm para fixação em alvenaria, concreto e bloco. Acompanham o furo de broca 8 mm e suportam prateleiras e suportes de TV leves.",
                List.of(de(MARCA, "Standers"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "8 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-003", "Parafuso Chipboard Cabeça Chata para Madeira 3,5x30mm com 30 peças Standers", "Ferragens", "14.90", 90,
                "Caixa com 100 parafusos 3,5x30 mm para montagem de móveis e peças finas de madeira.",
                List.of(de(MARCA, "Standers"),de(TIPO, "Parafuso"),de(BITOLA, "3,5 mm"),
                        de(COMPRIMENTO, "30 mm"),de(QUANTIDADE, "100 un")));
        produto("SKU-FRG-004", "Parafuso Chipboard Cabeça Flangeada Phillips 5x30mm Caixa Com 500un - Jomarca", "Ferragens", "24.90", 55,
                "Caixa com 50 parafusos 5x60 mm, para fixações que pedem mais penetração na madeira.",
                List.of(de(MARCA, "Jomarca"),de(TIPO, "Parafuso"),de(BITOLA, "5 mm"),
                        de(COMPRIMENTO, "60 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-005", "Kit Cabo De Aço Varal 1,6mm Plastificado 15 Metros Com 4 Ganchos Bucha 10 Reforçado", "Ferragens", "12.90", 70,
                "Pacote com 50 buchas 6 mm para fixações leves em alvenaria, como quadros e suportes pequenos.",
                List.of(de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "6 mm"),de(QUANTIDADE, "50 un")));
        produto("SKU-FRG-006", "Bucha Nylon 10mm para Bases Maciças SF com Parafuso 10 peças Standers", "Ferragens", "18.90", 45,
                "Pacote com 25 buchas 10 mm para cargas maiores, como armário aéreo e mão francesa.",
                List.of(de(MARCA, "Standers"),de(TIPO, "Bucha"),de(MATERIAL, "Nylon"),
                        de(BITOLA, "10 mm"),de(QUANTIDADE, "25 un")));
        produto("SKU-FRG-007", "Dobradica Aco Vai E Vem Bang Bang Retorno Mola Automatico 3 Polegadas Porta Madeira Ferro Aluminio Cromado", "Ferragens", "22.90", 38,
                "Par de dobradiças com pino solto, que permite retirar a porta sem desparafusar a folha.",
                List.of(de(TIPO, "Dobradiça"),de(MATERIAL, "Aço"),
                        de(DIMENSAO, "3 pol"),de(QUANTIDADE, "2 un")));
        produto("SKU-FRG-008", "Fechadura para Porta Interna Pado Concept 413 Cromado 40mm Aço Inox Chave Simples", "Ferragens", "89.90", 20,
                "Fechadura com maçaneta e trinco para porta de cômodo, com espelho cromado.",
                List.of(de(MARCA, "Pado"),de(TIPO, "Fechadura"),de(ACABAMENTO, "Cromado")));
        produto("SKU-FRG-009", "Cadeado com Chave Simples 40mm Latão Maciço Pado", "Ferragens", "34.90", 42,
                "Cadeado de latão maciço 40 mm com duas chaves. O latão resiste melhor a ferrugem em área externa.",
                List.of(de(MARCA, "Pado"),de(TIPO, "Cadeado"),de(MATERIAL, "Latão"),
                        de(DIMENSAO, "40 mm")));
        produto("SKU-FRG-010", "Mão Francesa Normo 30cm Branca", "Ferragens", "29.90", 33,
                "Par de suportes para prateleira até 30 cm de profundidade, com furos para bucha 8 mm.",
                List.of(de(MARCA, "Normo"),de(TIPO, "Suporte de prateleira"),de(MATERIAL, "Aço"),
                        de(COMPRIMENTO, "30 cm"),de(QUANTIDADE, "2 un")));
        produto("SKU-FRG-011", "Arruela Plana Grande 8mm Aço Standers 10 peças", "Ferragens", "9.90", 85,
                "Pacote com 100 arruelas 8 mm, que distribuem a pressão do parafuso e evitam marcar a peça.",
                List.of(de(MARCA, "Standers"),de(TIPO, "Arruela"),de(MATERIAL, "Aço"),
                        de(BITOLA, "8 mm"),de(QUANTIDADE, "100 un")));

        // ---------------------------------------------------------------- Eletrica
        produto("SKU-ELE-001", "Cabo Flexível 2,5mm 100m Azul 750V Megatron", "Elétrica", "189.90", 8,
                "Rolo de 100 m de cabo flexível 2,5 mm², isolação 750 V. Bitola usada em circuitos de tomadas de uso geral em residências.",
                List.of(de(MARCA, "Megatron"),de(TIPO, "Cabo flexível"),de(BITOLA, "2,5 mm²"),
                        de(COMPRIMENTO, "100 m")));
        produto("SKU-ELE-002", "Interruptor Simples 4x2 C/ 1 Tecla 10a 250v Branco Tramontina", "Elétrica", "14.90", 50,
                "Interruptor simples de embutir, uma tecla, acabamento branco. Liga e desliga um ponto de luz a partir de um único local.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Interruptor simples"),de(COR, "Branco")));
        produto("SKU-ELE-003", "Disjuntor Din Bipolar Curva C 25A Steck", "Elétrica", "42.90", 15,
                "Disjuntor termomagnético bipolar 25 A, padrão DIN. Protege circuitos de chuveiro e ar-condicionado contra sobrecarga e curto-circuito.",
                List.of(de(MARCA, "Steck"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "25 A"),
                        de(POLOS, "Bipolar")));
        produto("SKU-ELE-004", "Cabo Flexível 1,5mm 100m Amarelo 750V Cobrecom", "Elétrica", "129.90", 12,
                "Rolo de 100 m de cabo 1,5 mm², bitola usada em circuitos de iluminação residencial.",
                List.of(de(MARCA, "Cobrecom"),de(TIPO, "Cabo flexível"),de(BITOLA, "1,5 mm²"),
                        de(COMPRIMENTO, "100 m")));
        produto("SKU-ELE-005", "Cabo Flexível 4mm 50m Preto 750V Cobrecom", "Elétrica", "179.90", 9,
                "Rolo de 50 m de cabo 4 mm², para circuitos de maior corrente como chuveiro e forno.",
                List.of(de(MARCA, "Cobrecom"),de(TIPO, "Cabo flexível"),de(BITOLA, "4 mm²"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-ELE-006", "Interruptor Simples 4x2 C/ 1 Tecla Paralelo 10a 250v Branco Tramontina", "Elétrica", "19.90", 40,
                "Permite acender e apagar o mesmo ponto de luz de dois lugares diferentes, como nas pontas de um corredor.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Interruptor paralelo"),de(COR, "Branco")));
        produto("SKU-ELE-007", "Tomada 2p+t 10a 250v Evidence Fame", "Elétrica", "16.90", 60,
                "Tomada padrão brasileiro 10 A com aterramento, para uso geral em qualquer cômodo.",
                List.of(de(MARCA, "Fame"),de(TIPO, "Tomada"),de(AMPERAGEM, "10 A"),
                        de(COR, "Branco")));
        produto("SKU-ELE-008", "Tomada Dupla Fame Habitat 2p+t 20a Com Placa 4x2 Branco", "Elétrica", "22.90", 35,
                "Tomada 20 A com aterramento, obrigatória para ar-condicionado e eletrodomésticos de maior consumo.",
                List.of(de(MARCA, "Fame"),de(TIPO, "Tomada"),de(AMPERAGEM, "20 A"),
                        de(COR, "Branco")));
        produto("SKU-ELE-009", "Disjuntor Mini Din Unipolar 16a Curva B 127/220v Siemens", "Elétrica", "18.90", 48,
                "Disjuntor de um polo 16 A, padrão DIN, para proteger circuitos de tomadas de uso geral.",
                List.of(de(MARCA, "Siemens"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "16 A"),
                        de(POLOS, "Unipolar")));
        produto("SKU-ELE-010", "Disjuntor Din Tripolar 220-400V 40A Eletromar", "Elétrica", "89.90", 11,
                "Disjuntor de três polos 40 A, usado na entrada de quadros trifásicos.",
                List.of(de(MARCA, "Eletromar"),de(TIPO, "Disjuntor"),de(AMPERAGEM, "40 A"),
                        de(POLOS, "Tripolar")));
        produto("SKU-ELE-011", "Quadro Pvc Embutir 12 Disjuntores Branco", "Elétrica", "159.90", 7,
                "Quadro de embutir para até 12 disjuntores DIN, com barramento de neutro e terra.",
                List.of(de(TIPO, "Quadro de distribuição"),
                        de(QUANTIDADE, "12 un")));

        // ---------------------------------------------------------------- Encanamento
        produto("SKU-ENC-001", "Cano PVC Marrom Soldável 3m 3/4\" 25mm Tigre", "Encanamento", "28.90", 35,
                "Barra de 6 m de cano PVC soldável 25 mm para água fria. Bitola mais comum em ramais de banheiro e cozinha em residências.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Cano soldável"),de(MATERIAL, "PVC"),
                        de(BITOLA, "25 mm"),de(COMPRIMENTO, "6 m")));
        produto("SKU-ENC-002", "Cola para PVC Incolor Frasco 175g Tigre", "Encanamento", "18.90", 22,
                "Adesivo plástico 175 g para soldar conexões de PVC rígido. A junta pode receber água depois de 12 horas de cura.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Adesivo para PVC"),de(PESO, "175 g")));
        produto("SKU-ENC-003", "Torneira de Pia para Banheiro com Bica Baixa Cromada Remix Sensea", "Encanamento", "129.90", 10,
                "Torneira de mesa para lavatório de banheiro, acabamento cromado, bica baixa. Rosca padrão de 1/2 polegada.",
                List.of(de(MARCA, "Sensea"),de(TIPO, "Torneira de lavatório"),
                        de(ACABAMENTO, "Cromado"),de(BITOLA, "1/2 pol")));
        produto("SKU-ENC-004", "Sifão Universal Plástico Branco Tigre", "Encanamento", "22.50", 18,
                "Sifão sanfonado universal para pia e lavatório. O corpo flexível se ajusta a distâncias diferentes entre o ralo e a parede, o que resolve instalações fora do esquadro.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Sifão sanfonado"),de(MATERIAL, "Plástico")));
        produto("SKU-ENC-005", "Sifão Extensível Universal Com Copo (blukit Cromado)", "Encanamento", "39.90", 12,
                "Sifão copo cromado universal para pia e lavatório. O copo retém resíduos e pode ser aberto para limpeza sem desmontar a instalação.",
                List.of(de(MARCA, "Blukit"),de(TIPO, "Sifão copo"),de(MATERIAL, "Metal"),
                        de(ACABAMENTO, "Cromado")));
        produto("SKU-ENC-006", "Cano PVC Marrom Soldável 1m 1\" 32mm Equation", "Encanamento", "39.90", 28,
                "Barra de 6 m em 32 mm, bitola usada em ramais que alimentam mais de um ponto de água.",
                List.of(de(MARCA, "Equation"),de(TIPO, "Cano soldável"),de(MATERIAL, "PVC"),
                        de(BITOLA, "32 mm"),de(COMPRIMENTO, "6 m")));
        produto("SKU-ENC-007", "Joelho 90° PVC Marrom Soldável 3/4\" 25mm Tigre", "Encanamento", "14.90", 50,
                "Pacote com 10 joelhos de 90 graus em 25 mm, para mudar a direção do ramal.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Joelho"),de(MATERIAL, "PVC"),
                        de(BITOLA, "25 mm"),de(QUANTIDADE, "10 un")));
        produto("SKU-ENC-008", "Tê PVC Marrom Soldável 3/4\" 25mm Tigre", "Encanamento", "17.90", 44,
                "Pacote com 10 conexões em T de 25 mm, para derivar um ramal em dois.",
                List.of(de(MARCA, "Tigre"),de(TIPO, "Te"),de(MATERIAL, "PVC"),de(BITOLA, "25 mm"),
                        de(QUANTIDADE, "10 un")));
        produto("SKU-ENC-009", "Registro de Gaveta Bruto 25mm ou 1\" Deca", "Encanamento", "49.90", 24,
                "Registro de gaveta para embutir na parede, com acabamento a ser instalado por cima.",
                List.of(de(MARCA, "Deca"),de(TIPO, "Registro de gaveta"),de(MATERIAL, "Metal"),
                        de(BITOLA, "25 mm")));
        produto("SKU-ENC-010", "Valvula Pia Cozinha 4 1/2 Aço Inox Com Cesto Higiênico Removível Ralo Basket Escoamento Cuba", "Encanamento", "34.90", 30,
                "Válvula de escoamento em inox para pia de cozinha, compatível com cuba de 3,5 polegadas.",
                List.of(de(TIPO, "Válvula de escoamento"),de(MATERIAL, "Inox"),
                        de(BITOLA, "3,5 pol")));
        produto("SKU-ENC-011", "Fita Veda Rosca 18mm x 50m Equation", "Encanamento", "8.90", 75,
                "Fita de vedação para roscas metálicas de água fria e quente. Enrolar no sentido da rosca antes de apertar.",
                List.of(de(MARCA, "Equation"),de(TIPO, "Fita veda rosca"),de(LARGURA, "18 mm"),
                        de(COMPRIMENTO, "50 m")));
        produto("SKU-ENC-012", "Caixa Sifonada Quadrada Com 3 Entradas Branca 100x100x50mm - Krona", "Encanamento", "27.90", 26,
                "Caixa sifonada de piso com grelha, que retém resíduos e bloqueia o cheiro do esgoto.",
                List.of(de(MARCA, "Krona"),de(TIPO, "Caixa sifonada"),de(MATERIAL, "PVC"),
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
        produto("SKU-ILU-001", "Kit Lâmpada LED Tubular Luz Branca 9W Kian Bivolt", "Iluminação", "39.90", 60,
                "Kit com 3 lâmpadas LED 9 W, luz branca, soquete E27. Cerca de 900 lumens cada, equivalentes a lâmpadas incandescentes de 60 W.",
                List.of(de(MARCA, "Kian"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "9 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-002", "Luminária Bivolt Embutir Led Slim Quadrada Startec Preto", "Iluminação", "69.90", 14,
                "Luminária quadrada de embutir para forro de gesso, com recorte de 17 cm. Acompanha soquete e mola de fixação.",
                List.of(de(MARCA, "Startec"),de(TIPO, "Luminária de embutir"),
                        de(FORMATO, "Quadrado"),de(DIMENSAO, "17x17 cm")));
        produto("SKU-ILU-003", "Kit 3 Lâmpada Led Bulbo 12w Branco Quente (3000k) - Ourolux", "Iluminação", "49.90", 35,
                "Kit com 3 lâmpadas LED 12 W, luz branca, soquete E27. Cerca de 1.250 lumens cada, indicadas para sala e cozinha.",
                List.of(de(MARCA, "Ourolux"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "12 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-004", "Kit 10 Lâmpadas Led Kian Classic 9w Bivolt 3000k Amarela", "Iluminação", "39.90", 0,
                "Mesma potência da versão branca, com luz amarela de 3000 K - mais aconchegante para quarto e sala.",
                List.of(de(MARCA, "Kian"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "9 W"),
                        de(TEMPERATURA_DE_COR, "Amarela"),de(QUANTIDADE, "3 un")));
        produto("SKU-ILU-005", "Kit 10 Lâmpadas de LED E27 Bulbo 15W 1350lm Luz Branca Bivolt Elgin", "Iluminação", "44.90", 28,
                "Kit com 2 lâmpadas de 15 W, cerca de 1.500 lumens cada, para cômodos grandes ou pé-direito alto.",
                List.of(de(MARCA, "Elgin"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "15 W"),
                        de(TEMPERATURA_DE_COR, "Branca"),de(QUANTIDADE, "2 un")));
        produto("SKU-ILU-006", "Lâmpada de LED Filamento E27 Pêra 4W 300lm Luz Âmbar Bivolt Elgin", "Iluminação", "29.90", 22,
                "Lâmpada decorativa de filamento aparente, luz âmbar. Indicada para luminária de vidro transparente.",
                List.of(de(MARCA, "Elgin"),de(TIPO, "Lâmpada LED"),de(POTENCIA, "4 W"),
                        de(TEMPERATURA_DE_COR, "Âmbar")));
        produto("SKU-ILU-007", "Luminária Led Embutir Redonda 18w", "Iluminação", "64.90", 16,
                "Versão redonda da luminária de embutir, com recorte de 17 cm no forro de gesso.",
                List.of(de(TIPO, "Luminária de embutir"),
                        de(FORMATO, "Redondo"),de(DIMENSAO, "17 cm")));
        produto("SKU-ILU-008", "Painel de Led Sobrepor Quadrado Luz Branca 24W Elgin 26,5x26,5cm Branco Bivolt", "Iluminação", "99.90", 13,
                "Painel de sobrepor para teto sem forro, com 24 W e luz branca uniforme.",
                List.of(de(MARCA, "Elgin"),de(TIPO, "Painel LED"),de(POTENCIA, "24 W"),
                        de(FORMATO, "Quadrado"),de(TEMPERATURA_DE_COR, "Branca")));
        produto("SKU-ILU-009", "Trilho Eletrificado 2m + 6 Spot Led 7w 6000k Bivolt Preto", "Iluminação", "79.90", 19,
                "Spot direcionável para trilho eletrificado, com corpo preto e foco ajustável.",
                List.of(de(TIPO, "Spot de trilho"),de(POTENCIA, "7 W"),
                        de(COR, "Preto")));
        produto("SKU-ILU-010", "Fita Led Rolo 5m Branco Frio 3528 Dupla Face Ip65 Com Fonte", "Iluminação", "89.90", 21,
                "Rolo de 5 m de fita LED com fonte inclusa, para sanca de gesso ou iluminação de nicho.",
                List.of(de(TIPO, "Fita LED"),de(COMPRIMENTO, "5 m"),
                        de(TEMPERATURA_DE_COR, "Branca")));
        produto("SKU-ILU-011", "Arandela Solar Externa IP65 LED Branco Inspire Preta", "Iluminação", "119.90", 10,
                "Arandela com vedação para área externa, resistente à chuva. Ilumina fachada e corredor lateral.",
                List.of(de(MARCA, "Inspire"),de(TIPO, "Arandela"),de(COR, "Preto"),
                        de(MATERIAL, "Alumínio")));

        // ---------------------------------------------------------------- Jardim
        produto("SKU-JAR-001", "Vaso De Cerâmica Decorativo Para Flores Laranja Terra G 30 Cm", "Jardim", "79.90", 12,
                "Vaso de cerâmica esmaltada 30 cm de diâmetro, com furo de drenagem. Indicado para plantas de porte médio em área interna ou varanda.",
                List.of(de(TIPO, "Vaso"),de(MATERIAL, "Cerâmica"),
                        de(DIMENSAO, "30 cm")));
        produto("SKU-JAR-002", "Terra Vegetal para Flores, Vasos e Jardim Natural Geolia 20kg", "Jardim", "24.90", 30,
                "Saco de 20 kg de terra vegetal adubada, pronta para uso em vasos, canteiros e replantio. Não precisa de correção antes do plantio.",
                List.of(de(MARCA, "Geolia"),de(TIPO, "Terra vegetal"),de(PESO, "20 kg")));
        produto("SKU-JAR-003", "Vaso de Planta Pequeno em Cerâmica 9x15,2cm Azul|Preto Orquídea Cerâmica Artística", "Jardim", "49.90", 20,
                "Vaso de cerâmica esmaltada 20 cm com furo de drenagem, para plantas de porte pequeno.",
                List.of(de(MARCA, "Cerâmica Artística"),de(TIPO, "Vaso"),de(MATERIAL, "Cerâmica"),
                        de(DIMENSAO, "20 cm")));
        produto("SKU-JAR-004", "Vaso Planta 65x40 Oval Moderno Polietileno - Cinza Cimento 004", "Jardim", "109.90", 11,
                "Vaso grande em polietileno, bem mais leve que a cerâmica no mesmo tamanho - facilita mover a planta.",
                List.of(de(TIPO, "Vaso"),de(MATERIAL, "Polietileno"),
                        de(DIMENSAO, "45 cm")));
        produto("SKU-JAR-005", "Substrato para Folhagens Orgânico Geolia Granulado 5kg", "Jardim", "16.90", 45,
                "Substrato leve para vasos, com boa drenagem. Indicado para plantas de interior.",
                List.of(de(MARCA, "Geolia"),de(TIPO, "Substrato"),de(PESO, "5 kg")));
        produto("SKU-JAR-006", "Fertilizante Premium NPK 10 10 10 Uso Geral CPC Garden - 1kg", "Jardim", "22.90", 38,
                "Adubo mineral equilibrado para manutenção, com nitrogênio, fósforo e potássio em partes iguais.",
                List.of(de(MARCA, "CPC Garden"),de(TIPO, "Adubo"),de(PESO, "1 kg")));
        produto("SKU-JAR-007", "Mangueira GeoConfort 20m Geolia", "Jardim", "79.90", 17,
                "Mangueira reforçada de 20 m com engate rápido, que resiste à dobra sem estrangular o fluxo.",
                List.of(de(MARCA, "Geolia"),de(TIPO, "Mangueira"),de(COMPRIMENTO, "20 m")));
        produto("SKU-JAR-008", "REGADOR PLÁSTICO 4,5L PRETO", "Jardim", "24.90", 32,
                "Regador de 5 litros com crivo removível, para regar mudas sem revolver a terra.",
                List.of(de(MARCA, "Famastil"),de(TIPO, "Regador"),de(MATERIAL, "Plástico"),
                        de(VOLUME, "5 L")));
        produto("SKU-JAR-009", "Alicate Tesoura Para Poda Manual Com Trava E Mola 8 Polegadas Danmi®", "Jardim", "59.90", 23,
                "Tesoura de poda com lâmina de aço carbono, para galhos de até 2 cm.",
                List.of(de(MARCA, "Danmi"),de(TIPO, "Tesoura de poda"),de(MATERIAL, "Aço"),
                        de(COMPRIMENTO, "8 pol")));
        produto("SKU-JAR-010", "Pedra para Jardim Decorativa Dolomita Branca Pequena 10kg Geolia", "Jardim", "34.90", 26,
                "Saco de 20 kg de pedra decorativa branca, para cobrir canteiro e reduzir mato.",
                List.of(de(MARCA, "Geolia"),de(TIPO, "Pedra decorativa"),de(COR, "Branco"),
                        de(PESO, "20 kg")));
        produto("SKU-JAR-011", "Grama Sintetica Softgrass 10mm - 2x1m - 2m2 - Decortech", "Jardim", "89.90", 14,
                "Placa de grama sintética de 2 m², com base drenante para uso em varanda e área externa.",
                List.of(de(MARCA, "Decortech"),de(TIPO, "Grama sintética"),de(DIMENSAO, "2x1 m")));

        // ---------------------------------------------------------------- Ferramentas
        produto("SKU-FER-001", "Furadeira de Impacto Dexter 650ID2.5AA1 650W 1/2\" 127V (110V) com Empunhadura, Chave de Mandril e Guia de Profundidade", "Ferramentas", "299.90", 7,
                "Furadeira de impacto 650 W com mandril de 1/2 polegada e reversão. O modo impacto perfura concreto e alvenaria; sem impacto, madeira e metal.",
                List.of(de(MARCA, "Dexter"),de(TIPO, "Furadeira de impacto"),de(POTENCIA, "650 W"),
                        de(BITOLA, "1/2 pol")));
        produto("SKU-FER-002", "Trena Profissional Irwin 5m/16Ftx3/4\" Irwin", "Ferramentas", "24.90", 45,
                "Trena de 5 m com fita de aço, trava e clipe de cinto. Fita de 19 mm, que mantem a rigidez em medidas longas sem apoio.",
                List.of(de(MARCA, "Irwin"),de(TIPO, "Trena"),de(COMPRIMENTO, "5 m"),
                        de(LARGURA, "19 mm")));
        produto("SKU-FER-003", "Trena Standard Amarela Stein 7,5m/25mm", "Ferramentas", "34.90", 20,
                "Trena de 7,5 m com fita de aço, trava e clipe de cinto. O alcance extra cobre cômodos inteiros e vãos de parede numa medida só.",
                List.of(de(MARCA, "Stein"),de(TIPO, "Trena"),de(COMPRIMENTO, "7,5 m"),
                        de(LARGURA, "25 mm")));
        produto("SKU-FER-004", "Furadeira de Impacto Bosch GSB16 RE 850W 1/2\" 110V com Maleta, Chave de Mandril, Empunhadura e Limitador de Profundidade", "Ferramentas", "429.90", 6,
                "Modelo de 850 W com mandril de 1/2 polegada, para furações longas em concreto.",
                List.of(de(MARCA, "Bosch"),de(TIPO, "Furadeira de impacto"),de(POTENCIA, "850 W"),
                        de(BITOLA, "1/2 pol")));
        produto("SKU-FER-005", "Parafusadeira Ranger a Bateria 12V 3/8\" com Carregador e 1 Bateria", "Ferramentas", "349.90", 8,
                "Parafusadeira sem fio 12 V com bateria de lítio, para montagem de móveis sem depender de tomada.",
                List.of(de(MARCA, "Ranger"),de(TIPO, "Parafusadeira"),de(POTENCIA, "12 V")));
        produto("SKU-FER-006", "Jogo de Broca para Concreto 4mm a 10mm 5 Peças Dexter", "Ferramentas", "49.90", 30,
                "Cinco brocas com ponta de widia, de 5 a 10 mm, para alvenaria e concreto.",
                List.of(de(MARCA, "Dexter"),de(TIPO, "Jogo de brocas"),de(QUANTIDADE, "5 un")));
        produto("SKU-FER-007", "Jogo Chave Fenda E Philips Cromo Vanádio C/ 6 Peças Vonder", "Ferramentas", "39.90", 34,
                "Seis chaves com cabo emborrachado, cobrindo as bitolas mais usadas em manutenção doméstica.",
                List.of(de(MARCA, "Vonder"),de(TIPO, "Jogo de chaves"),de(QUANTIDADE, "6 un")));
        produto("SKU-FER-008", "Martelo Borracha Cabo de Madeira 26cm Dexter", "Ferramentas", "44.90", 28,
                "Martelo de 27 mm com cabo de madeira, que absorve melhor o impacto do que o cabo metálico.",
                List.of(de(MARCA, "Dexter"),de(TIPO, "Martelo"),de(MATERIAL, "Madeira"),
                        de(DIMENSAO, "27 mm")));
        produto("SKU-FER-009", "Alicate Universal Eletricista 8 Polegadas Corneta", "Ferramentas", "54.90", 31,
                "Alicate com área de corte e isolamento no cabo, para uso geral e pequenos serviços elétricos.",
                List.of(de(MARCA, "Corneta"),de(TIPO, "Alicate"),de(COMPRIMENTO, "8 pol")));
        produto("SKU-FER-010", "Nível Manual de Alumínio 16\" (400mm) 2 Bolhas Dexter", "Ferramentas", "34.90", 29,
                "Nível de alumínio com três bolhas, para conferir horizontal, vertical e 45 graus.",
                List.of(de(MARCA, "Dexter"),de(TIPO, "Nível"),de(MATERIAL, "Alumínio"),
                        de(COMPRIMENTO, "40 cm")));
        produto("SKU-FER-011", "Serrote Profissional Tamanho 20 Cabo Em Madeira Lamina Em Aco 5 Dentes Por Polegada Tramontina", "Ferramentas", "64.90", 18,
                "Serrote com dentes temperados para corte em madeira maciça e compensado.",
                List.of(de(MARCA, "Tramontina"),de(TIPO, "Serrote"),de(COMPRIMENTO, "20 pol")));
        produto("SKU-FER-012", "Escada Alumínio 5 Degraus 1,53m 120kg Prata e Vermelho Reisam", "Ferramentas", "279.90", 9,
                "Escada dobrável de alumínio com 5 degraus e sapatas antiderrapantes. Alcança cerca de 2,7 m.",
                List.of(de(MARCA, "Reisam"),de(TIPO, "Escada"),de(MATERIAL, "Alumínio"),
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
        produto("SKU-MAT-001", "Argamassa ACII Interno e Externo 20kg Cinza Varandas e Quintais CimentCola Quartzolit", "Materiais de construção", "28.90", 50,
                "Argamassa colante AC-II, saco de 20 kg, para assentamento de cerâmica em área interna e externa. Suporta variação de temperatura e umidade.",
                List.of(de(MARCA, "Quartzolit"),de(TIPO, "Argamassa AC-II"),de(PESO, "20 kg")));
        produto("SKU-MAT-002", "Cimento Todas as Obras 50kg Votoran", "Materiais de construção", "42.90", 40,
                "Saco de 50 kg de cimento Portland CP-II, de uso geral em concreto, argamassa e assentamento de blocos.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cimento CP-II"),de(PESO, "50 kg")));
        produto("SKU-MAT-003", "Argamassa ACIII Interno e Externo 20kg Branco Axton", "Materiais de construção", "36.90", 28,
                "Argamassa colante AC-III, saco de 20 kg, de aderência reforçada. Indicada para porcelanato, peças grandes e assentamento sobre piso antigo.",
                List.of(de(MARCA, "Axton"),de(TIPO, "Argamassa AC-III"),de(PESO, "20 kg")));
        produto("SKU-MAT-004", "Cimento CP II Branco 1kg Fortaleza", "Materiais de construção", "44.90", 35,
                "Cimento pozolânico, que gera menos calor na cura - indicado para peças de concreto maiores.",
                List.of(de(MARCA, "Fortaleza"),de(TIPO, "Cimento CP-IV"),de(PESO, "50 kg")));
        produto("SKU-MAT-005", "Cal Hidratada para Construção Civil 20Kg Votoran", "Materiais de construção", "18.90", 42,
                "Cal para argamassa de assentamento e reboco, que melhora a plasticidade da mistura.",
                List.of(de(MARCA, "Votoran"),de(TIPO, "Cal hidratada"),de(PESO, "20 kg")));
        produto("SKU-MAT-006", "Areia Fina Saco 20kg Três Lagoas", "Materiais de construção", "14.90", 60,
                "Saco de 20 kg de areia média lavada, para argamassa e concreto em pequenas quantidades.",
                List.of(de(MARCA, "Três Lagoas"),de(TIPO, "Areia"),de(PESO, "20 kg")));
        produto("SKU-MAT-007", "Bloco Cerâmico 9x19x19cm Cerâmica Volpini", "Materiais de construção", "3.90", 200,
                "Bloco cerâmico de vedação, medida padrão para paredes internas sem função estrutural.",
                List.of(de(MARCA, "Volpini"),de(TIPO, "Bloco cerâmico"),de(DIMENSAO, "9x19x39 cm")));
        produto("SKU-MAT-008", "Tijolo Refratário 11,4x5,1x22,9cm 05 Unidades por fardo Gabriella", "Materiais de construção", "1.90", 300,
                "Tijolo maciço para paredes que precisam de mais resistência, como churrasqueira e muro baixo.",
                List.of(de(MARCA, "Gabriella"),de(TIPO, "Tijolo maciço"),de(DIMENSAO, "5x10x20 cm")));
        produto("SKU-MAT-009", "Rejunte Acrílico Cinza 1kg Axton", "Materiais de construção", "12.90", 55,
                "Rejunte acrílico para juntas de até 3 mm em área seca, pronto para uso.",
                List.of(de(MARCA, "Axton"),de(TIPO, "Rejunte acrílico"),de(COR, "Cinza"),
                        de(PESO, "1 kg")));
        produto("SKU-MAT-010", "Manta Líquida Impermeabilizante Vedacit Vedapren 18L Preto", "Materiais de construção", "289.90", 8,
                "Manta líquida para laje e área externa, aplicada com rolo em três demãos cruzadas.",
                List.of(de(MARCA, "Vedacit"),de(TIPO, "Impermeabilizante"),de(VOLUME, "18 L")));
        produto("SKU-MAT-011", "Tela Soldada Tag Malha 5x10cm Fio 1,60mm Rl 25x1,0m", "Materiais de construção", "69.90", 19,
                "Tela de aço soldada 2x3 m, que distribui a carga e reduz trincas no contrapiso.",
                List.of(de(MARCA, "Tag"),de(TIPO, "Tela soldada"),de(MATERIAL, "Aço"),
                        de(DIMENSAO, "2x3 m")));
    }
}
