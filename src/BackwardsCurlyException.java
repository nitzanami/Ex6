import exceptions.SyntaxException;

public class BackwardsCurlyException extends SyntaxException {
    public BackwardsCurlyException(String msg) {
        super(msg);
    }
}
