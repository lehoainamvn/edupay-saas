package com.edupay.exception;

public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message);
    }
}
