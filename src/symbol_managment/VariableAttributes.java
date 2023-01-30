package symbol_managment;

/**
 * A class that represents an atomic Variable in s-java, it include the variable:
 *      [name] of the variable.
 *      [isFinal] indicase if this variable is final
 *      [Type] from Enum VarType
 *      [initiate] whether this variable was initiated or not.
 *
 *      include getters and a setter for initiated.
 *      exclude the value the variable holds
 */
public class VariableAttributes {
    
    private final String name;
    private final boolean isFinal;
    private final VarType vType;
    private boolean initiated;
    
    public VariableAttributes(String name, boolean isFinal, VarType t, boolean initiated){
        this.name = name;
        this.isFinal = isFinal;
        this.vType = t;
        this.initiated  = initiated;
    }
    
    public String getName() {return name;}
    public boolean isInitialized(){return initiated;}
    public VarType getVariableType(){return vType;}
    public boolean isFinal(){return isFinal;}
    public void setInitiated(boolean initiated){this.initiated = initiated;}
}
