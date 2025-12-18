package com.example.JobFinder.exception;

/**
 * Ném ra khi người dùng gửi dữ liệu không hợp lệ.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
