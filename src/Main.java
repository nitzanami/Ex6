/**
 * Writen by
 */

import exceptions.SyntaxException;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        Status S_JavaStatus;
        if (args.length != 1) {
            System.out.println(2);
            System.err.println("Invalid arguments for program");
            return;
        }
        try {
            LineProcessor p = new LineProcessor(args[0]);
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
            System.err.println("Invalid arguments for program");
            return;
        } catch (SyntaxException e) {
            System.out.println(Status.SYNTAX);
            System.err.println(e.getMessage());
            return;
        }
        System.out.println(Status.VALID);
    }
}


