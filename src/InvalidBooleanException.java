import exceptions.SyntaxException;

public class InvalidBooleanException extends SyntaxException {
    public InvalidBooleanException(String s) {
        super(s);
    }
}
