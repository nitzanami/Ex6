package oop.ex6.symbol_managment;

import oop.ex6.SyntaxException;

public class NoSuchFunctionException extends SyntaxException {
    public NoSuchFunctionException(String message) {
        super(message);
    }
}
