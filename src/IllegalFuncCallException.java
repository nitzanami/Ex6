import exceptions.SyntaxException;

public class IllegalFuncCallException extends SyntaxException {
    public IllegalFuncCallException(String msg) {
        super(msg);
    }
}
