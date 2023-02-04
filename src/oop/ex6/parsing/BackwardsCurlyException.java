package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class BackwardsCurlyException extends SyntaxException {
    public BackwardsCurlyException(String msg) {
        super(msg);
    }
}
