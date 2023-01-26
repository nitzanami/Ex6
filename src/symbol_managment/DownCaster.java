package symbol_managment;

public class DownCaster {
    /**
     * @param A Vartype
     * @param B Vartype
     * @return if "A = B;" is ok
     */
    public static boolean firstAcceptsSecond(VarType A, VarType B) {
        if (A == B) return true;
        else if (A == VarType.BOOLEAN) return B == VarType.INT || B == VarType.DOUBLE;
        else if (A == VarType.DOUBLE) return B==VarType.INT;
        return false;
    }
}
