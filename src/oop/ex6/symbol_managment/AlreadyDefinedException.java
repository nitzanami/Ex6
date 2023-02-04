package oop.ex6.symbol_managment;

import oop.ex6.SyntaxException;

public class AlreadyDefinedException extends SyntaxException {
    public AlreadyDefinedException(String s) {
        super(s);
    }
}
