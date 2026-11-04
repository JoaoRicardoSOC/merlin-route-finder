package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.infrastructure.database.entity.PontoMapaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A traducao entre dominio e persistencia, campo a campo.
 * <p>
 * O teste existe por um erro concreto: {@code PontoMapaEntity} usa {@code @AllArgsConstructor},
 * que segue a ordem de declaracao dos campos, e o codigo curto entrou entre {@code corredor} e
 * as coordenadas. Da primeira vez os argumentos foram passados na ordem do dominio e o
 * compilador reclamou - por sorte, porque os tipos diferiam.
 * <p>
 * <b>Corredor e codigo curto sao dois String vizinhos:</b> troca-los nao daria erro nenhum. O
 * mapa apareceria com os rotulos no lugar dos codigos, e o codigo digitado nunca encontraria
 * ponto algum.
 */
class PontoMapaFactoryTest {

    private final PontoMapaFactory factory = new PontoMapaFactory();

    private static final UUID ID = UUID.randomUUID();

    @Test
    @DisplayName("ida e volta preserva cada campo no seu lugar")
    void idaEVolta() {
        PontoMapa original = new PontoMapa(ID, TipoPonto.QR_CODE, "Corredor de Tintas", 32, 18, "TIN-02");

        PontoMapa voltou = factory.paraDominio(factory.paraPersistencia(original));

        assertEquals(ID, voltou.getId());
        assertEquals(TipoPonto.QR_CODE, voltou.getTipo());
        assertEquals("Corredor de Tintas", voltou.getCorredor());
        assertEquals("TIN02", voltou.getCodigoCurto());
        assertEquals(32, voltou.getCoordenadaX());
        assertEquals(18, voltou.getCoordenadaY());
    }

    @Test
    @DisplayName("a entidade recebe cada valor na coluna certa")
    void colunasCertas() {
        // Verifica o lado da gravacao diretamente: uma troca simetrica entre corredor e codigo
        // passaria despercebida por um teste que so olha a ida e a volta.
        PontoMapaEntity entity = factory.paraPersistencia(
                new PontoMapa(ID, TipoPonto.QR_CODE, "Corredor de Tintas", 32, 18, "TIN-02"));

        assertEquals("Corredor de Tintas", entity.getCorredor());
        assertEquals("TIN02", entity.getCodigoCurto());
        assertEquals(32, entity.getCoordenadaX());
        assertEquals(18, entity.getCoordenadaY());
    }

    @Test
    @DisplayName("ponto sem codigo curto atravessa a traducao como nulo")
    void semCodigo() {
        PontoMapa prateleira = new PontoMapa(ID, TipoPonto.PRATELEIRA, "Tintas", 32, 10);

        assertNull(factory.paraPersistencia(prateleira).getCodigoCurto());
        assertNull(factory.paraDominio(factory.paraPersistencia(prateleira)).getCodigoCurto());
    }

    @Test
    @DisplayName("nulo entra e nulo sai, nos dois sentidos")
    void nulos() {
        assertNull(factory.paraDominio(null));
        assertNull(factory.paraPersistencia(null));
    }
}
