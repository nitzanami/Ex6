package oop.ex6.symbol_managment;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * a class that handles the scope at any given moment.
 * <p>
 * uses an array
 */


public class MemoryManager {
    private final ArrayList<HashMap<String, VariableAttributes>> memoryScopes;

    public MemoryManager() {
        memoryScopes = new ArrayList<>();
        // adds the global scope as empty:
        memoryScopes.add(new HashMap<>());
    }


    /**
     * return true if x named variable is not declared in the outer scope of this memory manager.
     *
     * @param x the name of the variable
     * @return true if x named variable is not declared in the outer scope.
     */
    public boolean declarable(String x) {
        for (VariableAttributes var : (memoryScopes.get(memoryScopes.size() - 1).values()))
            if (var.getName().equals(x)) return false;
        return true;
    }

    /**
     * @return true if ArrayList hold another scope, that's not the global one.
     */
    public boolean isOuterScope() {
        return 1 == memoryScopes.size();
    }

    /**
     * check if we are in the outer scope of a function
     * @return true if the scope we are in is a function, and not an if or while inside it.
     */
    public boolean isFunctionScope() {
        return 2 == memoryScopes.size();
    }

    /**
     * declare a variable if it is not defined in the current scope yet, if it is, throw an exception
     *
     * @param variableAttributes The variable that we want to declare.
     */
    public void declareVariable(VariableAttributes variableAttributes) {
        if (!declarable(variableAttributes.getName()))
            throw new AlreadyDefinedException("The variable " + variableAttributes.getName() +
                    " is already defined in this scope");
        memoryScopes.get(memoryScopes.size() - 1).put(variableAttributes.getName(), variableAttributes);
    }


    /**
     * get a list of all uninitialized global variables.
     *
     * @return the list
     */
    public List<VariableAttributes> getUninitializedGlobals() {
        ArrayList<VariableAttributes> result = new ArrayList<>();
        for (VariableAttributes var : memoryScopes.get(0).values()) {
            if (var.isUninitialized())
                result.add(var);
        }
        return result;
    }

    /**
     * make all global variables in the list uninitialized
     *
     * @param vars the globals to unInitialize
     */
    public void unInitializeGlobals(List<VariableAttributes> vars) {
        for (VariableAttributes var : vars) {
            memoryScopes.get(0).get(var.getName()).setInitiated(false);
        }
    }

    /**
     * add depth to the memoryScope.
     * it means that we entered a new scope,
     * in the 1st version: while,if, or a function.
     */
    public void increaseScopeDepth() {
        memoryScopes.add(new HashMap<>());
    }

    /**
     * decrease depth from the memoryScope.
     * it means that we exited the current scope,
     * in the 1st version: while,if, or a function just ended with a '}' sign.
     */
    public void decreaseScopeDepth() {
        memoryScopes.remove(memoryScopes.size() - 1);
    }

    /**
     * check if an item is familiar in the scope.
     * used in case of using a variable - and answer the query of what type it is.
     *
     * @param value the item searched for
     * @return in case it is, return its type.
     * else returns NULL;
     */
    public VariableAttributes getVarAttributes(String value) {
        for (int i = memoryScopes.size() - 1; i >= 0; i--) {
            for (VariableAttributes var : memoryScopes.get(i).values()) {
                if (var.getName().equals(value))
                    return var;
            }
        }
        return null;
    }
}

