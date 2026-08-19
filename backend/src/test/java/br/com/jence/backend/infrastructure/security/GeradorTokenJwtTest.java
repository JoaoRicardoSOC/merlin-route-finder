package br.com.jence.backend.infrastructure.security;

import br.com.jence.backend.domain.exception.TokenHandoffInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Nao precisa de banco nem de contexto Spring: instancia o gerador diretamente. */
class GeradorTokenJwtTest {

    private static final String SEGREDO = "segredo-de-teste-com-no-minimo-256-bits-para-hs256";

    @Test
    @DisplayName("token gerado carrega a lista de roteiro e pode ser validado")
    void geraEValida() {
        GeradorTokenJwt gerador = new GeradorTokenJwt(SEGREDO);
        UUID listaId = UUID.randomUUID();

        String token = gerador.gerar(listaId, UUID.randomUUID());

        assertThat(token).isNotBlank();
        assertThat(gerador.extrairListaRoteiroId(token)).isEqualTo(listaId);
    }

    @Test
    @DisplayName("token adulterado e rejeitado")
    void rejeitaTokenAdulterado() {
        GeradorTokenJwt gerador = new GeradorTokenJwt(SEGREDO);
        String token = gerador.gerar(UUID.randomUUID(), UUID.randomUUID());

        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> gerador.extrairListaRoteiroId(adulterado))
                .isInstanceOf(TokenHandoffInvalidoException.class);
    }

    @Test
    @DisplayName("token assinado com outra chave e rejeitado")
    void rejeitaTokenDeOutraChave() {
        String tokenDeOutraOrigem = new GeradorTokenJwt("outro-segredo-completamente-diferente-com-256-bits")
                .gerar(UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> new GeradorTokenJwt(SEGREDO).extrairListaRoteiroId(tokenDeOutraOrigem))
                .isInstanceOf(TokenHandoffInvalidoException.class);
    }

    @Test
    @DisplayName("token ausente ou malformado e rejeitado sem vazar detalhe")
    void rejeitaTokenInvalido() {
        GeradorTokenJwt gerador = new GeradorTokenJwt(SEGREDO);

        assertThatThrownBy(() -> gerador.extrairListaRoteiroId(null))
                .isInstanceOf(TokenHandoffInvalidoException.class);
        assertThatThrownBy(() -> gerador.extrairListaRoteiroId("   "))
                .isInstanceOf(TokenHandoffInvalidoException.class);
        assertThatThrownBy(() -> gerador.extrairListaRoteiroId("isso-nao-e-um-jwt"))
                .isInstanceOf(TokenHandoffInvalidoException.class)
                .hasMessageNotContainingAny("assinatura", "signature", "malformed");
    }

    @Test
    @DisplayName("sem segredo configurado, ainda gera token valido com chave aleatoria")
    void funcionaSemSegredoConfigurado() {
        GeradorTokenJwt gerador = new GeradorTokenJwt("");
        UUID listaId = UUID.randomUUID();

        assertThat(gerador.extrairListaRoteiroId(gerador.gerar(listaId, UUID.randomUUID())))
                .isEqualTo(listaId);

        // Instancias diferentes tem chaves diferentes: e o efeito colateral esperado de nao
        // ter segredo fixo (tokens nao sobrevivem a um restart).
        GeradorTokenJwt outro = new GeradorTokenJwt("");
        String token = gerador.gerar(listaId, UUID.randomUUID());
        assertThatThrownBy(() -> outro.extrairListaRoteiroId(token))
                .isInstanceOf(TokenHandoffInvalidoException.class);
    }
}
