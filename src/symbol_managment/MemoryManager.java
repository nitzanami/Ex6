package symbol_managment;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * a class that handles the scope at any given moment.
 *
 * uses an array
 */


public class MemoryManager {
    private ArrayList<HashMap<String, VariableAttribute>> memoryScopes;
    
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
     *         else returns NULL;
     */
    public VariableAttribute inScope(String x) {
        for (HashMap h : memoryScopes) {
            for (VariableAttribute var : memoryScopes.get(memoryScopes.size() - 1).values())
                if (var.equals(x)) return var;
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
    public boolean isOuterScope(){return 1 < memoryScopes.size();}
    
    public int getScopeDepth(){return memoryScopes.size();}
}

