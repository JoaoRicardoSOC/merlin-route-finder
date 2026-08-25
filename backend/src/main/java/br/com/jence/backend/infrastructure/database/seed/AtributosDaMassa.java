package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

/**
 * As caracteristicas de cada produto da massa de demonstracao.
 * <p>
 * Vive num arquivo proprio para nao inchar a carga: sao trinta linhas de dado, sem nenhuma
 * regra. As marcas sao <b>reais e coerentes com o produto</b> - a Leroy vende essas marcas -,
 * pelo mesmo motivo de os precos serem plausiveis: um catalogo com marca inventada nao se
 * parece com uma loja.
 * <p>
 * <b>Nem todo produto tem todo atributo</b>, e e isso que torna as facetas interessantes:
 * quem navega em Eletrica ve Amperagem, quem navega em Tintas ve Grao, e ninguem ve os dois.
 */
final class AtributosDaMassa {

    private AtributosDaMassa() {
    }

    private static ValorDeAtributo de(AtributoProduto atributo, String valor) {
        return new ValorDeAtributo(atributo, valor);
    }

    static Map<String, List<ValorDeAtributo>> porSku() {
        Map<String, List<ValorDeAtributo>> massa = new LinkedHashMap<>();

        // ---------------------------------------------------------------- Tintas
        massa.put("SKU-TIN-001", List.of(
                de(MARCA, "Suvinil"), de(TIPO, "Tinta acrilica"),
                de(ACABAMENTO, "Fosco"), de(COR, "Branco"), de(VOLUME, "18 L")));
        massa.put("SKU-TIN-002", List.of(
                de(MARCA, "Atlas"), de(TIPO, "Rolo de pintura"),
                de(MATERIAL, "La"), de(LARGURA, "23 cm")));
        massa.put("SKU-TIN-003", List.of(
                de(MARCA, "Norton"), de(TIPO, "Lixa para parede"), de(GRAO, "120")));
        massa.put("SKU-TIN-004", List.of(
                de(MARCA, "Norton"), de(TIPO, "Lixa d agua"), de(GRAO, "150")));
        massa.put("SKU-TIN-005", List.of(
                de(MARCA, "3M"), de(TIPO, "Fita crepe"),
                de(LARGURA, "48 mm"), de(COMPRIMENTO, "50 m")));

        // ---------------------------------------------------------------- Ferragens
        massa.put("SKU-FRG-001", List.of(
                de(MARCA, "Ciser"), de(TIPO, "Parafuso"),
                de(BITOLA, "4 mm"), de(COMPRIMENTO, "40 mm"), de(QUANTIDADE, "100 un")));
        massa.put("SKU-FRG-002", List.of(
                de(MARCA, "Fischer"), de(TIPO, "Bucha"), de(MATERIAL, "Nylon"),
                de(BITOLA, "8 mm"), de(QUANTIDADE, "50 un")));

        // ---------------------------------------------------------------- Eletrica
        massa.put("SKU-ELE-001", List.of(
                de(MARCA, "Sil"), de(TIPO, "Cabo flexivel"),
                de(BITOLA, "2,5 mm2"), de(COMPRIMENTO, "100 m")));
        massa.put("SKU-ELE-002", List.of(
                de(MARCA, "Tramontina"), de(TIPO, "Interruptor simples"), de(COR, "Branco")));
        massa.put("SKU-ELE-003", List.of(
                de(MARCA, "Steck"), de(TIPO, "Disjuntor"),
                de(AMPERAGEM, "25 A"), de(POLOS, "Bipolar")));

        // ---------------------------------------------------------------- Encanamento
        massa.put("SKU-ENC-001", List.of(
                de(MARCA, "Tigre"), de(TIPO, "Cano soldavel"), de(MATERIAL, "PVC"),
                de(BITOLA, "25 mm"), de(COMPRIMENTO, "6 m")));
        massa.put("SKU-ENC-002", List.of(
                de(MARCA, "Tigre"), de(TIPO, "Adesivo para PVC"), de(PESO, "175 g")));
        massa.put("SKU-ENC-003", List.of(
                de(MARCA, "Docol"), de(TIPO, "Torneira de lavatorio"),
                de(ACABAMENTO, "Cromado"), de(BITOLA, "1/2 pol")));
        massa.put("SKU-ENC-004", List.of(
                de(MARCA, "Blukit"), de(TIPO, "Sifao sanfonado"), de(MATERIAL, "Plastico")));
        massa.put("SKU-ENC-005", List.of(
                de(MARCA, "Blukit"), de(TIPO, "Sifao copo"),
                de(MATERIAL, "Metal"), de(ACABAMENTO, "Cromado")));

        // ---------------------------------------------------------------- Cozinhas
        massa.put("SKU-COZ-001", List.of(
                de(MARCA, "Tramontina"), de(TIPO, "Cuba de bancada"),
                de(MATERIAL, "Inox"), de(DIMENSAO, "56x33 cm")));
        massa.put("SKU-COZ-002", List.of(
                de(MARCA, "Docol"), de(TIPO, "Torneira gourmet"), de(ACABAMENTO, "Cromado")));

        // ---------------------------------------------------------------- Iluminacao
        massa.put("SKU-ILU-001", List.of(
                de(MARCA, "Philips"), de(TIPO, "Lampada LED"), de(POTENCIA, "9 W"),
                de(TEMPERATURA_DE_COR, "Branca"), de(QUANTIDADE, "3 un")));
        massa.put("SKU-ILU-002", List.of(
                de(MARCA, "Taschibra"), de(TIPO, "Luminaria de embutir"),
                de(FORMATO, "Quadrado"), de(DIMENSAO, "17x17 cm")));
        massa.put("SKU-ILU-003", List.of(
                de(MARCA, "Philips"), de(TIPO, "Lampada LED"), de(POTENCIA, "12 W"),
                de(TEMPERATURA_DE_COR, "Branca"), de(QUANTIDADE, "3 un")));

        // ---------------------------------------------------------------- Jardim
        massa.put("SKU-JAR-001", List.of(
                de(MARCA, "Vasart"), de(TIPO, "Vaso"),
                de(MATERIAL, "Ceramica"), de(DIMENSAO, "30 cm")));
        massa.put("SKU-JAR-002", List.of(
                de(MARCA, "Vitaplan"), de(TIPO, "Terra vegetal"), de(PESO, "20 kg")));

        // ---------------------------------------------------------------- Ferramentas
        massa.put("SKU-FER-001", List.of(
                de(MARCA, "Bosch"), de(TIPO, "Furadeira de impacto"),
                de(POTENCIA, "650 W"), de(BITOLA, "1/2 pol")));
        massa.put("SKU-FER-002", List.of(
                de(MARCA, "Stanley"), de(TIPO, "Trena"),
                de(COMPRIMENTO, "5 m"), de(LARGURA, "19 mm")));
        massa.put("SKU-FER-003", List.of(
                de(MARCA, "Stanley"), de(TIPO, "Trena"),
                de(COMPRIMENTO, "7,5 m"), de(LARGURA, "25 mm")));

        // ---------------------------------------------------------------- Decoracao
        massa.put("SKU-DEC-001", List.of(
                de(MARCA, "Evolux"), de(TIPO, "Espelho"),
                de(FORMATO, "Redondo"), de(DIMENSAO, "60 cm")));

        // ---------------------------------------------------------------- Materiais
        massa.put("SKU-MAT-001", List.of(
                de(MARCA, "Quartzolit"), de(TIPO, "Argamassa AC-II"), de(PESO, "20 kg")));
        massa.put("SKU-MAT-002", List.of(
                de(MARCA, "Votoran"), de(TIPO, "Cimento CP-II"), de(PESO, "50 kg")));
        massa.put("SKU-MAT-003", List.of(
                de(MARCA, "Quartzolit"), de(TIPO, "Argamassa AC-III"), de(PESO, "20 kg")));

        return massa;
    }
}
