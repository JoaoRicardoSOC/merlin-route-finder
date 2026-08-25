package br.com.jence.backend;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os pontos de QR Code contra o Oracle real: e por um deles que o cliente entra na jornada, e
 * o codigo curto e o unico caminho de volta quando o adesivo esta rasgado ou a camera falha.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class PontoDeQrCodeIntegracaoTest {

    @Autowired PontoMapaRepository pontoMapaRepository;

    private List<PontoMapa> qrCodes() {
        return pontoMapaRepository.buscarPorTipo(TipoPonto.QR_CODE);
    }

    @Test
    @DisplayName("a massa tem pontos de QR Code, e todo um deles tem codigo curto")
    void todoQrCodeTemCodigo() {
        List<PontoMapa> pontos = qrCodes();

        System.out.println(">>> QR Codes: " + pontos.stream()
                .map(p -> p.getCodigoCurto() + " @ " + p.getCorredor()).toList());

        assertThat(pontos).isNotEmpty();
        assertThat(pontos).allSatisfy(ponto ->
                assertThat(ponto.getCodigoCurto())
                        .as("um QR Code sem codigo curto nao tem plano B")
                        .isNotBlank());
    }

    @Test
    @DisplayName("nenhum codigo curto se repete")
    void codigosSaoUnicos() {
        /*
         * A coluna e unique, entao um codigo repetido nem chegaria a ser gravado. O teste
         * existe para o caso oposto: se alguem duplicar um codigo na massa, a carga falha no
         * startup e aqui e onde a causa fica legivel.
         */
        List<String> codigos = qrCodes().stream().map(PontoMapa::getCodigoCurto).toList();

        assertThat(codigos).doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "digitar \"{0}\" encontra o ponto")
    @ValueSource(strings = {"ENT-01", "ent-01", "ENT01", "ent 01"})
    @DisplayName("a busca aceita a grafia que o cliente digitar")
    void buscaTolerante(String digitado) {
        assertThat(pontoMapaRepository.buscarPorCodigoCurto(digitado))
                .isPresent()
                .get()
                .satisfies(ponto -> {
                    assertThat(ponto.getTipo()).isEqualTo(TipoPonto.QR_CODE);
                    assertThat(ponto.getCodigoCurto()).isEqualTo("ENT01");
                });
    }

    @Test
    @DisplayName("codigo inexistente devolve vazio, sem estourar")
    void codigoInexistente() {
        assertThat(pontoMapaRepository.buscarPorCodigoCurto("ZZZ-99")).isEmpty();
        assertThat(pontoMapaRepository.buscarPorCodigoCurto("")).isEmpty();
        assertThat(pontoMapaRepository.buscarPorCodigoCurto(null)).isEmpty();
    }

    @Test
    @DisplayName("cada QR Code cai dentro do grid do mapa")
    void dentroDoGrid() {
        // As coordenadas alimentam o desenho do mapa: um ponto fora do grid apareceria
        // encostado na borda da tela, ou fora dela.
        assertThat(qrCodes()).allSatisfy(ponto -> {
            assertThat(ponto.getCoordenadaX()).isBetween(0, 100);
            assertThat(ponto.getCoordenadaY()).isBetween(0, 100);
        });
    }
}
