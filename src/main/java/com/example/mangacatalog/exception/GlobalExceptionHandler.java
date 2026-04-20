package com.example.mangacatalog.exception;

import com.example.mangacatalog.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // <-- НОВЫЙ ОБРАБОТЧИК для сбора всех ошибок
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleCustomValidation(ValidationException ex) {
        LOG.warn("Множественные ошибки валидации: {}", ex.getErrors());
        ErrorResponse body = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Ошибка валидации входящих данных",
            LocalDateTime.now(),
            ex.getErrors()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        LOG.warn("Ресурс не найден: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        LOG.warn("Ошибка валидации данных @RequestBody");
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));

        ErrorResponse body = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Ошибка валидации входящих данных",
            LocalDateTime.now(),
            errors
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleJsonError(final HttpMessageNotReadableException ex) {
        LOG.warn("Ошибка чтения JSON или неверный формат данных: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Ошибка в формате JSON-запроса. Проверьте правильность заполнения полей и синтаксис.",
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        LOG.warn("Запрошен несуществующий путь: {}", ex.getResourcePath());
        ErrorResponse body = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Эндпоинт или файл не найден: " + ex.getResourcePath(),
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

// Обработчик для остальных ошибок остается без изменений...
}