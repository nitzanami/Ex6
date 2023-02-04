package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class IllegalFuncDecException extends SyntaxException {
    public IllegalFuncDecException(String msg) {
        super(msg);
    }
}
