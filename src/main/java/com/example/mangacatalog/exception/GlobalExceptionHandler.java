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

    // 404: Ресурс не найден (ваша кастомная ошибка)
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
    // 404: Эндпоинт не существует
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

    // 400: Ошибка валидации тела запроса (@RequestBody)
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

    // 400: Некорректные аргументы (ЭТОТ БЛОК РЕШАЕТ ВАШУ ПРОБЛЕМУ)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.warn("Недопустимый аргумент: {}", ex.getMessage());
        ErrorResponse body = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Некорректный запрос: " + ex.getMessage(), // Сообщение "ID must not be null" будет здесь
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 500: Все остальные непредвиденные ошибки
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        LOG.error("Критическая ошибка сервера: ", ex); // Логируем полный стектрейс для отладки
        ErrorResponse body = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            // НЕ отправляем ex.getMessage() клиенту!
            "Произошла внутренняя ошибка сервера. Пожалуйста, попробуйте позже.",
            LocalDateTime.now(),
            null
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}