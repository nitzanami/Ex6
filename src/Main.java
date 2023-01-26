import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;

public class Main {
    // todo check right return values
    public static void main(String[] args) {
        char S_JavaStatus = '0';
        if (args.length != 1) {
            S_JavaStatus = '2';
            throw new InputMismatchException("Wrong number of parameters");
        }
        
        LineProcessor p = new LineProcessor();
        
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            ArrayList<String> lines = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            for (String l : lines) {
                if (!p.processLineFirstIteration(l)) {
                    S_JavaStatus = '1'; // for illegal code
                    break;
                }
            }
            for (String l : lines) {
                if (!p.processLineSecondIteration(l)) {
                    S_JavaStatus = '1'; // for illegal code
                    break;
                }
            }
            System.out.println(S_JavaStatus);
            
        } catch (IOException | SyntaxException | NoSuchMethodException e) { // todo check which exception is needed to be thrown away.
            S_JavaStatus = '1'; // for illegal code
            e.printStackTrace();
            System.out.println(S_JavaStatus);
            System.out.println(e.getMessage());
        }
    }
}

