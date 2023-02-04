package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class IllegalVarDecException extends SyntaxException {
    public IllegalVarDecException(String s) {
        super(s);
    }
}
