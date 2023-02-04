package oop.ex6.parsing;

import oop.ex6.SyntaxException;

class MissingSemicolonException extends SyntaxException {
    public MissingSemicolonException(){
        super("Missing ; at end of line");
    }
}
