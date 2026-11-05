package br.com.jence.backend.dto;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import br.com.jence.backend.model.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A copia da sessao. O ModelMapper casa os campos pelo nome, entao o teste confere campo a
 * campo: um campo novo com nome parecido nao pode receber o valor errado.
 */
class SessaoResponseTest {

    @Test
    @DisplayName("copia cada campo da sessao, e a posicao cai na placa lida")
    void copiaOsCampos() {
        UUID id = UUID.randomUUID();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 11, 5, 10, 0);
        LocalDateTime expiracao = criadoEm.plusMinutes(30);
        PontoMapa placa = new PontoMapa(UUID.randomUUID(), TipoPonto.QR_CODE, "Tintas", 32, 10, "TIN-02");
        Sessao sessao = Sessao.reconstituir(id, StatusSessao.ACTIVE, criadoEm, expiracao,
                placa, criadoEm.plusMinutes(1));

        SessaoResponse resposta = SessaoResponse.de(sessao);

        assertEquals(id, resposta.getId());
        assertEquals(StatusSessao.ACTIVE, resposta.getStatus());
        assertEquals(criadoEm, resposta.getCriadoEm());
        assertEquals(expiracao, resposta.getExpiracaoTtl());
        assertEquals(placa.getId(), resposta.getPosicaoAtual().getId());
        assertEquals(placa.getCodigoCurto(), resposta.getPosicaoAtual().getCodigoCurto());
    }

    @Test
    @DisplayName("sessao sem placa lida fica sem posicao")
    void semPosicao() {
        assertNull(SessaoResponse.de(Sessao.iniciar(UUID.randomUUID())).getPosicaoAtual());
    }
}
