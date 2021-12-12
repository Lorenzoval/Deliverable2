package it.lorenzoval.deliverable2;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitHandler {

    private static final Logger logger = Logger.getLogger(GitHandler.class.getName());
    private static final String NP = "--no-pager";
    private static final String DATE_SHORT = "--format=%cs";

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
                pb = new ProcessBuilder("git", "fetch", "--all");
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

    public static LocalDate getReleaseDate(Project project, String releaseName) throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        File file = new File(projectName);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(file);
        pb.command("git", "log", "-1", DATE_SHORT, MessageFormat.format(project.getReleaseString(), releaseName));
        Process pr = pb.start();
        String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        if (pr.exitValue() != 0) {
            return null;
        } else {
            // Remove \n
            output = output.substring(0, output.length() - 1);
            return LocalDate.parse(output);
        }
    }

    public static void changeRelease(Project project, Release release) throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        String tagName = MessageFormat.format(project.getReleaseString(), release.getName());
        File file = new File(projectName);
        ProcessBuilder pb = new ProcessBuilder("git", "checkout",
                MessageFormat.format("tags/{0}", tagName));
        pb.directory(file);
        pb.inheritIO();
        Process pr = pb.start();
        pr.waitFor();
    }

    public static List<String> getFiles(Project project) throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        File file = new File(projectName);
        ProcessBuilder pb = new ProcessBuilder("git", "ls-files", "*.java");
        pb.directory(file);
        Process pr = pb.start();
        String files = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        return Arrays.asList(files.split("\n"));
    }

    public static LocalDate getFileCreationDate(Project project, String fileName) throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        File file = new File(projectName);
        ProcessBuilder pb = new ProcessBuilder("git", "log", "-1", "--diff-filter=A", DATE_SHORT,
                "--", fileName);
        pb.directory(file);
        Process pr = pb.start();
        String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        if (output.isEmpty()) {
            // If it was not possible to find commit in which file got added, take first commit of file
            pb.command("git", NP, "log", "--reverse", DATE_SHORT, fileName);
            pr = pb.start();
            output = IOUtils.toString(pr.getInputStream(),  StandardCharsets.UTF_8);
            pr.waitFor();
            output = output.substring(0, output.indexOf("\n"));
        } else {
            // Remove \n
            output = output.substring(0, output.length() - 1);
        }
        return LocalDate.parse(output);
    }

    private static int countFiles(String[] lines, int i) {
        int j = i;
        while (j < lines.length - 1) {
            if (lines[j + 1].isEmpty() || lines[j + 1].charAt(0) == '$')
                break;
            j++;
        }
        return j - i;
    }

    public static void parseLines(String output, List<Commit> commits, Release release) {
        String[] lines = output.split("\n");
        String hash = null;
        String author = null;
        String subject = null;
        int locAdded;
        int locDeleted;
        int chgSetSize = 0;
        for (int i = 0; i < lines.length; i++) {
            String str = lines[i];
            if (!str.isEmpty()) {
                if (str.charAt(0) == '$') {
                    // Add previous commit
                    if (hash != null)
                        commits.add(new Commit(hash, author, subject));

                    chgSetSize = countFiles(lines, i) - 1; // Files committed together with C
                    String[] values = str.substring(1).split("\\$");
                    hash = values[0];
                    author = values[1];
                    subject = values[2];
                } else {
                    // Compute metrics for java files
                    if (str.endsWith(".java")) {
                        String[] temp = str.split("\t");
                        locAdded = Integer.parseInt(temp[0]);
                        locDeleted = Integer.parseInt(temp[1]);
                        String fileName = temp[2];
                        assert chgSetSize != 0;
                        release.updateMetrics(fileName, author, chgSetSize, locAdded, locDeleted);
                    }
                }
            }
        }
        // Add last commit
        commits.add(new Commit(hash, author, subject));
    }

    public static void getReleaseCommitRelatedMetrics(Project project, List<Commit> commits, Release... releases)
            throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        File file = new File(projectName);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(file);
        if (releases.length == 1) {
            // Get first commit
            pb.command("git", NP, "log", "--reverse");
            Process pr = pb.start();
            String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
            pr.waitFor();
            String firstCommit = output.substring("commit ".length(), output.indexOf("\n"));
            pb.command("git", NP, "log", "--boundary", "--numstat", "--no-merges", "--pretty=format:$%h$%an$%s",
                    firstCommit + ".." + MessageFormat.format(project.getReleaseString(), releases[0].getName()));
        } else if (releases.length == 2) {
            pb.command("git", NP, "log", "--numstat", "--no-merges", "--pretty=format:$%h$%an$%s",
                    MessageFormat.format(project.getReleaseString(), releases[0].getName()) + ".." +
                            MessageFormat.format(project.getReleaseString(), releases[1].getName()));
        }
        Process pr = pb.start();
        String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        if (releases.length == 1) {
            parseLines(output, commits, releases[0]);
        } else if (releases.length == 2) {
            parseLines(output, commits, releases[1]);
        }
    }

    public static void getCommitRelatedMetrics(Project project, List<Commit> commits, List<Release> releases)
            throws IOException, InterruptedException {
        // Get commits for first release
        getReleaseCommitRelatedMetrics(project, commits, releases.get(0));
        for (int i = 1; i < releases.size(); i++) {
            getReleaseCommitRelatedMetrics(project, commits, releases.get(i - 1), releases.get(i));
        }
    }
}
