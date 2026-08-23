package br.com.jence.backend.infrastructure.security;

import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.exception.TokenHandoffExpiradoException;
import br.com.jence.backend.domain.exception.TokenHandoffInvalidoException;
import br.com.jence.backend.domain.service.GeradorTokenHandoff;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Implementacao do token de handoff usando JWT assinado (HS256). Unica classe do projeto que
 * conhece a biblioteca jjwt.
 */
@Component
@Slf4j
public class GeradorTokenJwt implements GeradorTokenHandoff {

    private static final String CLAIM_LISTA = "listaRoteiroId";
    private static final String CLAIM_SESSAO = "sessaoId";

    private final SecretKey chave;

    public GeradorTokenJwt(@Value("${merlin.jwt.secret:}") String segredoConfigurado) {
        if (segredoConfigurado == null || segredoConfigurado.isBlank()) {
            /*
             * Sem segredo configurado, gera uma chave aleatoria em vez de cair num valor
             * padrao embutido no codigo: um default commitado permitiria a qualquer pessoa
             * com acesso ao repositorio forjar tokens validos em producao. O efeito colateral
             * (tokens nao sobrevivem a um restart) e irrelevante para um TTL de 5 minutos.
             */
            this.chave = Jwts.SIG.HS256.key().build();
            log.warn("JWT_SECRET nao configurado: usando chave aleatoria desta execucao. "
                    + "Tokens de handoff serao invalidados a cada reinicio da aplicacao.");
        } else {
            this.chave = Keys.hmacShaKeyFor(segredoConfigurado.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public String gerar(UUID listaRoteiroId, UUID sessaoId) {
        Instant agora = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .claim(CLAIM_LISTA, listaRoteiroId.toString())
                .claim(CLAIM_SESSAO, sessaoId.toString())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(ListaRoteiro.TTL_TOKEN_HANDOFF)))
                .signWith(chave)
                .compact();
    }

    @Override
    public UUID extrairListaRoteiroId(String token) {
        if (token == null || token.isBlank()) {
            throw new TokenHandoffInvalidoException("Token de handoff ausente");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(chave)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UUID.fromString(claims.get(CLAIM_LISTA, String.class));

        } catch (ExpiredJwtException e) {
            /*
             * Unica falha que o sistema distingue, e a excecao a mensagem generica abaixo.
             * Nao e vazamento relevante: so um token de verdade, assinado por nos, chega
             * aqui - quem forja recebe a resposta indistinguivel. E e o que permite ao Totem
             * oferecer um QR Code novo em vez de mandar o cliente recomecar. Ver D-44.
             */
            throw new TokenHandoffExpiradoException("Token de handoff expirado");

        } catch (JwtException | IllegalArgumentException e) {
            // Mensagem generica de proposito: detalhar por que falhou ajudaria quem tenta
            // forjar um token.
            throw new TokenHandoffInvalidoException("Token de handoff invalido ou expirado");
        }
    }
}
