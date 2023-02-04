import exceptions.SyntaxException;

class UnknownIdentifierException extends SyntaxException {
    public UnknownIdentifierException(String s) {
        super(s);
    }
}
