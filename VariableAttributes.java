public class VariableAttributes {
    VariableType type;
    boolean isFinal;
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
