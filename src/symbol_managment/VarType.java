package symbol_managment;

import javax.lang.model.type.UnknownTypeException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum VarType {
    BOOLEAN,
    CHAR,
    INT,
    DOUBLE,
    STRING;

    /**
     * get the var type that matches the given string
     *
     * @param name the string name of the type
     * @return the VarType of this string
     * @throws IllegalTypeException if the name is not a valid type name
     */
    public static VarType getVarType(String name) {
        for (VarType v : VarType.values())
            if (name.equalsIgnoreCase(v.name())) {
                return v;
            }
        throw new IllegalTypeException("Type " + name + " is not a valid type");
    }

    /**
     * hold all the acceptable variables
     *
     * @return the regec for the variables
     */
    public static String getVarTypesRegex() {
        return "(int|String|boolean|char|double)";
    }

    /**
     * check if the given string is a String value inside double quotes
     *
     * @param value The value we want to check
     * @return true if the value is in string format, else false
     */
    public static boolean isString(String value) {
        Matcher m = Pattern.compile("^\"[^\"]*\"$").matcher(value);
        return m.find();
    }

    /**
     * check if the given string is a valid char inside single quotes
     *
     * @param value the value we want to check
     * @return true if the value is in char format, else false
     */
    public static boolean isChar(String value) {
        Matcher m = Pattern.compile("^'[^']*'$").matcher(value);
        return m.find();
    }

    /**
     * check if the given string is a valid int
     *
     * @param value the value we want to check
     * @return true if the value can be convereted to an int, else false
     */
    public static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    /**
     * check if the given string is a valid double
     *
     * @param value the value we want to check
     * @return true if the value can be converted to a double, else false.
     */
    public static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * check if the given string is a valid boolean value: true or false.
     *
     * @param value the value we want to check
     * @return true if the value is one of the strings:true,false. else false.
     */
    public static boolean isBoolean(String value) {
        return value.equals("true") || value.equals("false");
    }

    /**
     * return the type of the given value, if it can be converted to one
     *
     * @param value the value we want to convert to a type
     * @return BOOLEAN if the value is true or false. INT if the value is a valid integer.
     * STRING if the value is a valid string. CHAR if the value is a valid character.
     * DOUBLE if the value is a valid double. if all fail, null.
     */
    public static VarType getTypeOfValue(String value) {
        if (isBoolean(value))
            return BOOLEAN;
        if (isInt(value))
            return INT;
        if (isChar(value))
            return CHAR;
        if (isString(value))
            return STRING;
        if (isDouble(value))
            return DOUBLE;
        return  null;
    }

}
