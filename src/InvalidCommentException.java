import exceptions.SyntaxException;

public class InvalidCommentException extends SyntaxException {
    public InvalidCommentException() {
        super("Comment line must start with //");
    }
}
