package core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class to print information for reproducibility debugging into two files (one per run).
 * Created by Ansgar Mährlein on 21.04.2017.
 */
public class DebugPrinter {
    private String filepath1, filepath2, path;

    /**
     *  Creates a new DebugPrinter that prints into the two files located at
     *  "C:\Users\Byte\IdeaProjects\Simulator\reports\" and named name1.log and name2.log.
     *  If these two files are already present, they both are deleted and name1.log is created and written to.
     *  If only name1.log exists, name2.log will be created and selected as the file to write to.
     *  If only name2.log exists, name2.log will be deleted and name1.log is created and written to.
     *  If none of the two files exists, name1.log is created and written to.
     *
     *  Also note for multiple runs in batch mode, that static Classes are not reinitialized between runs,
     *  so that static or statically created versions of this DebugPrinter will just keep appending
     *  to name1.log/name2.log, whichever was last used, even across runs.
     *  To avoid this problem, just manually start single runs.
     * @param name Name of the two output files (without index)
     */
    public DebugPrinter(String name) {
        //generate the filepaths
        filepath1 = "C:\\Users\\Byte\\IdeaProjects\\Simulator\\reports\\" + name + "1.log";
        filepath2 = "C:\\Users\\Byte\\IdeaProjects\\Simulator\\reports\\" + name + "2.log";
        //check if any of the two files already exists
        File f = new File(filepath1);
        File f2 = new File(filepath2);
        if((f.exists() && !f.isDirectory()) && (f2.exists() && !f2.isDirectory())) {
            try {
                f.delete();
                f2.delete();
            } catch (Exception e) {
                System.err.println(e.toString());
            }
            path = filepath1;
        } else if(f.exists() && !f.isDirectory()) {
            path = filepath2;
        } else if(f2.exists() && !f2.isDirectory()) {
            try {
                f2.delete();
            } catch (Exception e) {
                System.err.println(e.toString());
            }
            path = filepath1;
        } else {
            path = filepath1;
        }
    }

    /**
     * print a new line to the currently selected file.
     * The SimTime at calling of this method is appended in front of the string that is written.
     * @param s the String to add to the file.
     */
    public void println(String s){
        try(FileWriter fw = new FileWriter(path, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            out.println(SimClock.getTime() + ": " + s);
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}
