package ru.app.draft.exceptions;

import ru.app.draft.models.ErrorData;

public class AuthorizationException extends RuntimeException {
    public AuthorizationException(ErrorData errorData) {
        super(errorData.getMessage());
    }

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Exception cause) {
        super(message, cause);
    }
}