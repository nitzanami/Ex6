package symbol_managment;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * a class that handles the scope at any given moment.
 * <p>
 * uses an array
 */


public class MemoryManager {
    private final ArrayList<HashMap<String, VariableAttribute>> memoryScopes;

    public MemoryManager() {
        memoryScopes = new ArrayList<>();
        // adds the global scope as empty:
        memoryScopes.add(new HashMap<>());
    }

    /**
     * check if an item is familiar in the scope.
     * used in case of using a variable - and answer the query of what type it is.
     *
     * @param x the item searched for
     * @return in case it is, return its type.
     * else returns NULL;
     */
    public VariableAttribute inScope(String x) {
        for (HashMap h : memoryScopes) {   //todo fix this loop
            for (VariableAttribute var : memoryScopes.get(memoryScopes.size() - 1).values())
                if (var.getName().equals(x)) return var;
        }
        return null;
    }

    /**
     * return true if x named variable is not declared in the outer scope of this memory manager.
     *
     * @param x the name of the variable
     * @return true if x named variable is not declared in the outer scope.
     */
    public boolean declareable(String x) {
        for (VariableAttribute var : (memoryScopes.get(memoryScopes.size() - 1).values()))
            if (var.getName().equals(x)) return false;
        return true;
    }

    /**
     * @return true if ArrayList hold another scope, that's not the global one.
     */
    public boolean isOuterScope() {
        return 1 < memoryScopes.size();
    }

    public int getScopeDepth() {
        return memoryScopes.size();
    }

    public void declareVariable(VariableAttribute variableAttributes){
        if(declareable(variableAttributes.getName())){
            memoryScopes.get(memoryScopes.size() - 1).put(variableAttributes.getName(), variableAttributes);
        }
    }

    public void declareVariable(String variableName, boolean isFinal, VarType type, boolean isInitiated) {
        declareVariable(new VariableAttribute(variableName, isFinal, type, isInitiated));
    }

    
    /**
     * add depth to the memoryScope.
     * it means that we entered a new scope,
     * in the 1st version: while,if, or a function.
     */
    public void increaseScopeDepth(){memoryScopes.add(new HashMap<>());}
    
    /**
     * decrease depth from the memoryScope.
     * it means that we exited the current scope,
     * in the 1st version: while,if, or a function just ended with a '}' sign.
     */
    public void decreaseScopeDepth(){memoryScopes.remove(memoryScopes.size()-1);}

    public VariableAttribute getVarAttributes(String value) {
        for(int i = memoryScopes.size() - 1; i >= 0 ; i--){
            for (VariableAttribute var:memoryScopes.get(i).values()) {
                if (var.getName().equals(value))
                    return var;
            }
        }
        return null;
    }
}

