package it.lorenzoval.deliverable2;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitHandler {

    private static final Logger logger = Logger.getLogger(GitHandler.class.getName());

    private GitHandler() {
    }

    public static void cloneOrPull(Project project) throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        String url = project.getUrl();
        ProcessBuilder pb;
        File file = new File(projectName);
        String logMsg;
        if (file.exists()) {
            if (!file.isDirectory()) {
                String errorMsg = MessageFormat.format("File {0} exists in process path and is not a directory",
                        projectName);
                logger.log(Level.SEVERE, errorMsg);
                throw new IOException();
            } else {
                logMsg = MessageFormat.format("Updating {0} source code",
                        projectName);
                pb = new ProcessBuilder("git", "pull");
                pb.directory(file);
            }
        } else {
            logMsg = MessageFormat.format("Downloading {0} source code",
                    projectName);
            pb = new ProcessBuilder("git", "clone", url);
        }
        logger.log(Level.INFO, logMsg);
        pb.inheritIO();
        Process pr = pb.start();
        pr.waitFor();
    }

}
