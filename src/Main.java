import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;

public class Main {
    // todo read file twice -> first time, remove comment line(start with //), remove empty
    //  lines, save functions and globals
    // todo the second time - ignore outer scope? and
    public static void main(String[] args) {
        char S_JavaStatus = '0';
        if (args.length != 1) {
            S_JavaStatus = '2';
            throw new InputMismatchException("Wrong number of parameters");
        }
        
        try (BufferedReader br = new BufferedReader(new FileReader(args[0]))) {
            String line;
            LineProcessor p = new LineProcessor();
            while ((line = br.readLine()) != null) {
                if (!p.processLineFirstIteration(line))
                    S_JavaStatus = '1'; // for illegal code
            }
            
        } catch (IOException | SyntaxException e) { // todo check which exception is needed to be thrown away.
            e.printStackTrace();
        } finally {
            System.out.println(S_JavaStatus);
        }
    }
}

