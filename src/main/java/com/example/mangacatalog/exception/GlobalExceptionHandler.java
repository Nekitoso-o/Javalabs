package com.example.mangacatalog.exception;

import com.example.mangacatalog.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
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


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final ResourceNotFoundException ex) {
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
    public ResponseEntity<ErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        LOG.warn("Ошибка валидации данных");

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ErrorResponse body = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Ошибка валидации входящих данных",
            LocalDateTime.now(),
            errors
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(final NoResourceFoundException ex) {
        LOG.warn("Запрошен несуществующий путь: {}", ex.getResourcePath());

        ErrorResponse body = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Эндпоинт или файл не найден: " + ex.getResourcePath(),
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(final Exception ex) {
        LOG.error("Критическая ошибка сервера: ", ex);

        ErrorResponse body = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Внутренняя ошибка сервера: " + ex.getMessage(),
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}