package br.com.jence.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class StandardError {

    private LocalDateTime timestamp;
    private Integer status;
    private String error;
    private String message;
    private String path;

    /** Os campos problematicos, quando o erro e de validacao. */
    private List<ValidationError> validationErrors;

    public StandardError() {
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /** Um campo com problema (ex.: field="nome", message="nao pode ser vazio"). */
    public static class ValidationError {

        private String field;
        private String message;

        public ValidationError() {
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public static ValidationError de(String field, String message) {
            ValidationError erro = new ValidationError();
            erro.setField(field);
            erro.setMessage(message);
            return erro;
        }
    }
}
