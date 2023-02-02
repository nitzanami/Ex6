package exceptions;

public class MissingSemicolonException extends RuntimeException {
    public MissingSemicolonException(){
        super("Missing ; at end of line");
    }
}
