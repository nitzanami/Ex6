package oop.ex6.symbol_managment;

import oop.ex6.SyntaxException;

class IllegalTypeException extends SyntaxException {
    public IllegalTypeException(String s) {
        super(s);
    }
}
