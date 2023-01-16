public class VariableAttributes {
    private final VariableType type;
    private final boolean isFinal;
    public VariableAttributes(VariableType type, boolean isFinal ){
        this.type = type;
        this.isFinal = isFinal;
    }
    public VariableType getType(){
        return type;
    }
    public boolean isFinal(){
        return isFinal;
    }
}
