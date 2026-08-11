package br.com.jence.backend.presentation.response;

import java.time.LocalDateTime;
import java.util.List;

public record StandardError(
        LocalDateTime timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<ValidationError> validationErrors // Nova lista para guardar os campos problemáticos
) {
    // Sub-record para mapear campo a campo (ex: field="nome", message="não pode ser vazio")
    public record ValidationError(String field, String message) {}
}