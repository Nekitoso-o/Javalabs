package com.example.mangacatalog.exception;

import com.example.mangacatalog.dto.BulkErrorItem;
import java.util.List;

public class BulkValidationException extends RuntimeException {

    private final List<BulkErrorItem> errors;

    public BulkValidationException(String message, List<BulkErrorItem> errors) {
        super(message);
        this.errors = errors;
    }

    public List<BulkErrorItem> getErrors() {
        return errors;
    }
}