package ru.app.draft.exception;

/**
 * Выбрасываем если произошла ошибка при исполнении ордера
 */
public class OrderNotExecutedException extends RuntimeException{
    public OrderNotExecutedException(String message) {
        super(message);
    }
}
