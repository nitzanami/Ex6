package symbol_managment;

import java.util.ArrayList;
import java.util.HashMap;

public class FunctionManager {
    private final HashMap<String, ArrayList<VarType>> functionToInputMapping;
    
    public FunctionManager() {
        functionToInputMapping = new HashMap<>();
    }
    
    /**
     * return the parameters for a given function
     *
     * @param functionName the function name
     * @return the parameters
     */
    public ArrayList<VarType> getParameterTypes(String functionName) throws NoSuchFunctionException {
        if (doesFunctionExist(functionName)) {
            return functionToInputMapping.get(functionName);
        }
        throw new NoSuchFunctionException(String.format("%s Method Not Found", functionName));
    }
    
    /**
     * @param lookedForFunction name of function
     * @return true if the function exists, false otherwise
     */
    public boolean doesFunctionExist(String lookedForFunction) {
        for (String funcName : functionToInputMapping.keySet()) {
            if (funcName.equals(lookedForFunction)) return true;
        }
        return false;
    }
    
    /**
     * add a function with the parameter type to the functions list
     *
     * @param functionName   name
     * @param parameterTypes list Of params
     */
    public void addFunction(String functionName, ArrayList<VarType> parameterTypes) throws RuntimeException {
        if (!doesFunctionExist(functionName))
            functionToInputMapping.put(functionName, parameterTypes);
        else throw new RuntimeException(String.format("overloading of %s was found", functionName));
    }
}
