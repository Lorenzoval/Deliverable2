package it.lorenzoval.deliverable2;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Deliverable2 {

    public static final String PROJECT_URL = "https://github.com/apache/syncope";

    private static final Logger logger = Logger.getLogger(Deliverable2.class.getName());

    public static void main( String[] args ) throws IOException, InterruptedException {
        ProcessBuilder pb;
        final String projectName = PROJECT_URL.substring(PROJECT_URL.lastIndexOf('/') + 1);
        File file = new File(projectName);
        String logMsg;
        if (file.exists()) {
            if (!file.isDirectory()) {
                String errorMsg = MessageFormat.format("File {0} exists in process path and is not a directory",
                        projectName);
                logger.log(Level.SEVERE, errorMsg);
                return;
            } else {
                logMsg = MessageFormat.format("Updating {0} source code",
                        projectName);
                pb = new ProcessBuilder("git", "pull");
            }
        } else {
            logMsg = MessageFormat.format("Downloading {0} source code",
                    projectName);
            pb = new ProcessBuilder("git", "clone", PROJECT_URL);
        }
        logger.log(Level.INFO, logMsg);
        pb.inheritIO();
        Process pr = pb.start();
        pr.waitFor();
    }

}
