package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class InvalidBooleanException extends SyntaxException {
    public InvalidBooleanException(String s) {
        super(s);
    }
}
