package br.com.jence.backend.presentation.advice;

import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.presentation.response.StandardError;
import br.com.jence.backend.presentation.response.StandardError.ValidationError;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ValidationError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Falha de Validação",
                "Os dados enviados na requisição estão inválidos. Verifique a lista de erros.",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /*
     * Sem este handler, um UUID malformado na URL cai no handler generico e vira 500 com a
     * mensagem "erro inesperado, equipe notificada" - culpando o servidor por um erro que e
     * do cliente e disparando alarme falso.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<StandardError> handleParametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Parâmetro Inválido",
                "O valor '%s' não é válido para o parâmetro '%s'.".formatted(ex.getValue(), ex.getName()),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(response);
    }

    /*
     * Mesma familia do handler acima: corpo JSON malformado e erro do cliente. Sem isto vira
     * 500 e o frontend nao consegue distinguir "eu mandei errado" de "o servidor caiu".
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> handleCorpoIlegivel(HttpMessageNotReadableException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Corpo da Requisição Inválido",
                "O corpo enviado não pode ser lido. Verifique se é um JSON válido.",
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(response);
    }

    /*
     * Terceiro caso da mesma familia (apos UUID malformado e corpo ilegivel): parametro
     * obrigatorio ausente e erro do cliente, nao do servidor. A varredura completa das demais
     * excecoes padrao do Spring MVC esta prevista para o card de prontidao de producao.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<StandardError> handleParametroAusente(MissingServletRequestParameterException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Parâmetro Obrigatório Ausente",
                "O parâmetro '%s' é obrigatório.".formatted(ex.getParameterName()),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<StandardError> handleMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Método Não Suportado",
                "O método %s não é aceito neste endereço.".formatted(ex.getMethod()),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<StandardError> handleMediaTypeNaoSuportado(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "Formato Não Suportado",
                "O formato %s não é aceito. Envie application/json.".formatted(ex.getContentType()),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<StandardError> handleCaminhoInexistente(NoResourceFoundException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Endereço Não Encontrado",
                "Não existe endpoint em %s.".formatted(request.getRequestURI()),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<StandardError> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Recurso Não Encontrado",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(OperacaoNaoPermitidaException.class)
    public ResponseEntity<StandardError> handleOperacaoNaoPermitida(OperacaoNaoPermitidaException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Operação Não Permitida",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /*
     * 422 e nao 404: o item existe e a ruptura foi processada e registrada com sucesso - o
     * que nao existe e um substituto a oferecer. O Mobile precisa distinguir os dois casos
     * para mostrar "nao encontramos nada equivalente por perto" em vez de "item inexistente".
     */
    @ExceptionHandler(SubstitutoIndisponivelException.class)
    public ResponseEntity<StandardError> handleSubstitutoIndisponivel(SubstitutoIndisponivelException ex, HttpServletRequest request) {

        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Substituto Indisponível",
                ex.getMessage(),
                request.getRequestURI(),
                null
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Requisicao cujo texto nao e decodificavel - tipicamente a query string em latin-1.
     *
     * <p>Cai aqui, e nao no tratador generico, porque <b>e erro do pedido e nao nosso</b>. Antes
     * respondia 500 com "Ocorreu um erro inesperado", o que joga no cliente a impressao de
     * defeito no servidor e ainda esconde a causa real: os bytes enviados nao formam texto
     * valido no charset da conexao.
     *
     * <p>A excecao e do Tomcat, e nao do Spring - por isso a auditoria de respostas de erro nao
     * a tinha coberto: ela nasce antes de qualquer controlador existir. Tratar o tipo especifico
     * em vez da superclasse e deliberado: {@code InvalidParameterException} estende
     * {@code IllegalStateException}, que em todo o resto do codigo significa bug nosso e deve
     * mesmo continuar virando 500.
     *
     * <p>Acoplar o tratador ao Tomcat tem uma falha benigna: trocando de container, este
     * tratador simplesmente nunca dispara, e a resposta volta a ser a generica. Nada quebra.
     */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<StandardError> handleTextoIndecodificavel(InvalidParameterException ex,
                                                                    HttpServletRequest request) {
        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Requisição Inválida",
                "Não foi possível ler os parâmetros enviados. Verifique se a requisição está "
                        + "codificada em UTF-8.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleGenericException(Exception ex, HttpServletRequest request) {
        StandardError response = new StandardError(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro Interno do Servidor",
                "Ocorreu um erro inesperado. Tente novamente em instantes.",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}