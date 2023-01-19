import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.function.Predicate;
    
    public class Main {
        // todo read file twice -> first time, remove comment line(start with //), remove empty
        //  lines, save functions and globals
        
        // todo the second time - ignore outer scope? and
        public static void main(String[] args) {
            if(args.length != 2) throw new InputMismatchException("Wrong number of parameters");
            try(BufferedReader br = new BufferedReader(new FileReader(args[1]))) {
                String line;
                LineProcessor p = new LineProcessor();
                while ((line = br.readLine()) != null) {
                    p.procesLine(line);
                }
                
            } catch (IOException e) { // todo check which exception is needed to be thrown away.
                e.printStackTrace();
            }
        }
    }

