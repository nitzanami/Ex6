package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class InvalidCommentException extends SyntaxException {
    public InvalidCommentException() {
        super("Comment line must start with //");
    }
}
