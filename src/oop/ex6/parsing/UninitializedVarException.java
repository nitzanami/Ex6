package oop.ex6.parsing;

import oop.ex6.SyntaxException;

public class UninitializedVarException extends SyntaxException {
    public UninitializedVarException(String s) {
        super(s);
    }
}
