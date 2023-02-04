

import exceptions.SyntaxException;

import java.io.IOException;
/**
 * Main class for the exercise, gets a file name as a parameter and checks if is valid
 */
public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println(2);
            System.err.println("Invalid arguments for program");
            return;
        }
        parseFile(args[0]);
    }

    private static void parseFile(String fileName) {
        Status S_JavaStatus;
        try {
            LineProcessor p = new LineProcessor(fileName);
            S_JavaStatus = p.runFirstIteration();
            if (S_JavaStatus != Status.VALID) {
                System.out.println(S_JavaStatus);
                System.err.println("Invalid line");
                return;
            }
            S_JavaStatus = p.runSecondIteration();
            if (S_JavaStatus != Status.VALID) {
                System.out.println(S_JavaStatus);
                System.err.println("Invalid line");
                return;
            }
        } catch (IOException e) {
            System.out.println(Status.IOERROR);
            System.err.println("An IO error occurred");
            return;
        } catch (SyntaxException e) {
            System.out.println(Status.SYNTAX);
            System.err.println(e.getMessage());
            return;
        }
        System.out.println(Status.VALID);
    }
}


