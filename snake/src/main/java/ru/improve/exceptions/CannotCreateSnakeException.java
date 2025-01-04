package ru.improve.exceptions;

public class CannotCreateSnakeException extends Exception {

    public CannotCreateSnakeException() {
        super("cannot create snake, not enough place on game field");
    }
}
