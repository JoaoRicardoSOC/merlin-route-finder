package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.exception.TokenHandoffInvalidoException;

import java.util.UUID;

/**
 * Produz o token que autoriza a transicao do roteiro do Totem para o celular do cliente.
 * <p>
 * Declarado em termos de negocio de proposito: o dominio precisa de "um token de transicao
 * confiavel", nao de um JWT. A tecnologia de assinatura fica na implementacao, em
 * infraestrutura, mantendo a regra da D-08 (o dominio registra o token, nunca o assina).
 */
public interface GeradorTokenHandoff {

    /**
     * @return token assinado, valido por {@code ListaRoteiro.TTL_TOKEN_HANDOFF}
     */
    String gerar(UUID listaRoteiroId, UUID sessaoId);

    /**
     * Verifica assinatura e prazo do token.
     *
     * @return o identificador da lista de roteiro contida no token
     * @throws TokenHandoffInvalidoException se estiver adulterado, expirado ou malformado
     */
    UUID extrairListaRoteiroId(String token);
}
