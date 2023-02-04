package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class IllegalFuncCallException extends SyntaxException {
    public IllegalFuncCallException(String msg) {
        super(msg);
    }
}
