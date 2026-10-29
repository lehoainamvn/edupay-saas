package com.edupay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném khi tạo resource bị trùng lặp (VD: SĐT đã đăng ký)
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends BusinessException {

    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message);
    }
}
