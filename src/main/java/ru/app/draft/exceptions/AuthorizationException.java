package ru.app.draft.exceptions;

public class AuthorizationException extends RuntimeException {
    public AuthorizationException() {
        super("Ошибка авторизации");
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Exception cause) {
        super(message, cause);
    }
}