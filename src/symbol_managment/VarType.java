package symbol_managment;

import javax.lang.model.type.UnknownTypeException;
import java.util.Locale;

public enum VarType {
    BOOLEAN,
    CHAR,
    INT,
    DOUBLE,
    STRING;
    /**
     * get the var type that matches the given string
     * @return the VarType of this string
     * @param name the string name of the type
     * @throws IllegalTypeException if the name is not a valid type name
     */
    public static VarType getVarType(String name){
        for (VarType v : VarType.values())
            if (name.equalsIgnoreCase(v.name())) {
                return v;
            }
        throw new IllegalTypeException("Type " + name + "is not a valid type");
    }
    
    /**
     * hold all the acceptable variables
     * @return the regec for the variables
     */
    public static String geVarTypesRegex(){
        return "(int|String|boolean|char|double)";
    }
}
