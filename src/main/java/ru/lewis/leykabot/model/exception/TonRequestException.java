package ru.lewis.leykabot.model.exception;

public class TonRequestException extends RuntimeException {
    public TonRequestException(String message) {
        super(message);
    }
    public TonRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
