package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class UnknownIdentifierException extends SyntaxException {
    public UnknownIdentifierException(String s) {
        super(s);
    }
}
