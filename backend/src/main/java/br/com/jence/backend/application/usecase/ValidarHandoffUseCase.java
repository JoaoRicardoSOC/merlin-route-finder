package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.RotaCalculadaResponse;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.TokenHandoffInvalidoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.GeradorTokenHandoff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC-011: valida o token lido do QR Code e libera a rota para o celular do cliente.
 */
@Service
@RequiredArgsConstructor
public class ValidarHandoffUseCase {

    private final GeradorTokenHandoff geradorTokenHandoff;
    private final ListaRoteiroRepository listaRoteiroRepository;
    private final SessaoRepository sessaoRepository;

    @Transactional
    public RotaCalculadaResponse executar(String token) {
        // Primeira camada: assinatura e prazo. Falha aqui nao chega a consultar o banco.
        geradorTokenHandoff.extrairListaRoteiroId(token);

        /*
         * Segunda camada, e o que garante o uso unico: procurar a lista *pelo token*. Depois
         * do consumo o campo fica nulo, entao um segundo escaneamento do mesmo QR nao
         * encontra nada - mesmo que o token continue criptograficamente valido.
         */
        ListaRoteiro lista = listaRoteiroRepository.buscarPorToken(token)
                .orElseThrow(() -> new TokenHandoffInvalidoException(
                        "Token de handoff invalido ou ja utilizado"));

        Sessao sessao = sessaoRepository.buscarPorId(lista.getSessaoId())
                .orElseThrow(() -> new TokenHandoffInvalidoException(
                        "Token de handoff invalido ou expirado"));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessao.getId(), sessao.getStatus()));
        }

        RotaCalculadaResponse rota = RotaCalculadaResponse.de(lista);

        lista.invalidarToken();
        listaRoteiroRepository.salvar(lista);

        // A caminhada esta comecando: a sessao precisa durar o percurso inteiro.
        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        return rota;
    }
}
