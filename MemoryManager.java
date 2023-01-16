import java.util.ArrayList;
import java.util.HashMap;

/**
 * a class that handles the scope at any given moment.
 *
 * uses an array
 */


public class MemoryManager {
    ArrayList<HashMap<String, String>> memoryScopes;
    
    public MemoryManager() {
        memoryScopes = new ArrayList<>();
    }
    
    /**
     * check if an item is familiar in the scope.
     * used in case of using a variable - and answer the query of what type it is.
     *
     * @param x the item searched for
     * @return in case it is, return its type.
     *         else returns NULL;
     *
     *
     * example:
     *      "FI" - final int
     *      "U"  - wasn't initiated
     *      "D" - double
     *      "f" - float
     *      "b" - boolean
     *      "c" - char
     */
    public String inScope(String x) {
        for (HashMap h : memoryScopes) {
            for (String s : memoryScopes.get(memoryScopes.size() - 1).values())
                if (s.equals(x)) return s;
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
        for (String s : (memoryScopes.get(memoryScopes.size() - 1).values()))
            if (s.equals(x)) return false;
        return true;
    }
    
    
}
