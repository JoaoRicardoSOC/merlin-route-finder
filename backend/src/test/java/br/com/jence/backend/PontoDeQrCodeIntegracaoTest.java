package br.com.jence.backend;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertFalse(pontos.isEmpty());
        for (PontoMapa ponto : pontos) {
            assertFalse(ponto.getCodigoCurto() == null || ponto.getCodigoCurto().isBlank(),
                    "um QR Code sem codigo curto nao tem plano B");
        }
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

        assertEquals(codigos.size(), new HashSet<>(codigos).size());
    }

    @ParameterizedTest(name = "digitar \"{0}\" encontra o ponto")
    @ValueSource(strings = {"ENT-01", "ent-01", "ENT01", "ent 01"})
    @DisplayName("a busca aceita a grafia que o cliente digitar")
    void buscaTolerante(String digitado) {
        Optional<PontoMapa> encontrado = pontoMapaRepository.buscarPorCodigoCurto(digitado);

        assertTrue(encontrado.isPresent());
        PontoMapa ponto = encontrado.get();
        assertEquals(TipoPonto.QR_CODE, ponto.getTipo());
        assertEquals("ENT01", ponto.getCodigoCurto());
    }

    @Test
    @DisplayName("codigo inexistente devolve vazio, sem estourar")
    void codigoInexistente() {
        assertTrue(pontoMapaRepository.buscarPorCodigoCurto("ZZZ-99").isEmpty());
        assertTrue(pontoMapaRepository.buscarPorCodigoCurto("").isEmpty());
        assertTrue(pontoMapaRepository.buscarPorCodigoCurto(null).isEmpty());
    }

    @Test
    @DisplayName("cada QR Code cai dentro do grid do mapa")
    void dentroDoGrid() {
        // As coordenadas alimentam o desenho do mapa: um ponto fora do grid apareceria
        // encostado na borda da tela, ou fora dela.
        for (PontoMapa ponto : qrCodes()) {
            assertTrue(ponto.getCoordenadaX() >= 0 && ponto.getCoordenadaX() <= 100);
            assertTrue(ponto.getCoordenadaY() >= 0 && ponto.getCoordenadaY() <= 100);
        }
    }
}
