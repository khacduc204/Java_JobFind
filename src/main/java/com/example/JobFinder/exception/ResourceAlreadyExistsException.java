package com.example.JobFinder.exception;

/**
 * Sử dụng khi tài nguyên (ví dụ email) đã tồn tại.
 */
public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
