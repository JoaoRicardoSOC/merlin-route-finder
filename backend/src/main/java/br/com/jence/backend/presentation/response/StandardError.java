package br.com.jence.backend.presentation.response;

import java.time.LocalDateTime;

public record StandardError(LocalDateTime timestamp, Integer status, String error, String message, String path) {}