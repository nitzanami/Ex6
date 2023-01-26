package symbol_managment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    public ArrayList<VarType> getParameterTypes(String functionName) throws NoSuchMethodException {
        if (doesFunctionExist(functionName)) {
            return functionToInputMapping.get(functionName);
        }
        throw new NoSuchMethodException(String.format("{0} Method Not Found", functionName));
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
    
    /**
     * @param funcName               give a func name and
     * @param parameteresGivenToFunc a list of the VarType it takes as args
     * @return is that function call legit
     */
    public boolean isLegitCall(String funcName, List<VarType> parameteresGivenToFunc) {
        try {
            List<VarType> funcParameters = getParameterTypes(funcName);
            if (funcParameters.size() != parameteresGivenToFunc.size()) return false;
            for (int i = 0; i < funcParameters.size(); i++) {
                if (!DownCaster.firstAcceptsSecond(funcParameters.get(i), parameteresGivenToFunc.get(i)))
                    return false;
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace(); // todo
            return false;
        }
        return true;
    }
}
