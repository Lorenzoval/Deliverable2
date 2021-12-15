package it.lorenzoval.deliverable2;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GitHandler {

    private static final Logger logger = Logger.getLogger(GitHandler.class.getName());
    private static final String NP = "--no-pager";
    private static final String DATE_FORMAT = "--format=%cs";
    private static final String COMMIT_FORMAT = "--pretty=format:$%h$%an$%s";
    private static final String NUMSTAT = "--numstat";
    private static final String NO_MERGES = "--no-merges";

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
        pb.command("git", "log", "-1", DATE_FORMAT, MessageFormat.format(project.getReleaseString(), releaseName));
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
        ProcessBuilder pb = new ProcessBuilder("git", "log", "-1", "--diff-filter=A", DATE_FORMAT,
                "--", fileName);
        pb.directory(file);
        Process pr = pb.start();
        String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        if (output.isEmpty()) {
            // If it was not possible to find commit in which file got added, take first commit of file
            pb.command("git", NP, "log", "--reverse", DATE_FORMAT, fileName);
            pr = pb.start();
            output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
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

    public static void addCommitIfNotEmpty(Release release, Commit commit) {
        // Only consider commits related to at least one java file
        if (!commit.getFiles().isEmpty())
            release.addCommit(commit);
    }

    public static void parseMainLines(String output, Release release) {
        String[] lines = output.split("\n");
        String hash = null;
        String author = null;
        String subject = null;
        int locAdded;
        int locDeleted;
        int chgSetSize = 0;
        List<String> files = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String str = lines[i];
            if (!str.isEmpty()) {
                if (str.charAt(0) == '$') {
                    // Add previous commit
                    if (hash != null) {
                        addCommitIfNotEmpty(release, new Commit(hash, author, subject, files));
                        files = new ArrayList<>();
                    }

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
                        files.add(fileName);
                        release.updateMetrics(fileName, author, chgSetSize, locAdded, locDeleted);
                    }
                }
            }
        }
        // Add last commit
        addCommitIfNotEmpty(release, new Commit(hash, author, subject, files));
    }

    public static void parseDroppedLines(String output, Release release) {
        String[] lines = output.split("\n");
        String hash = null;
        String author = null;
        String subject = null;
        List<String> files = new ArrayList<>();
        for (String str : lines) {
            if (!str.isEmpty()) {
                if (str.charAt(0) == '$') {
                    // Add previous commit
                    if (hash != null) {
                        addCommitIfNotEmpty(release, new Commit(hash, author, subject, files));
                        files = new ArrayList<>();
                    }

                    String[] values = str.substring(1).split("\\$");
                    hash = values[0];
                    author = values[1];
                    subject = values[2];
                } else {
                    // Compute metrics for java files
                    if (str.endsWith(".java")) {
                        String[] temp = str.split("\t");
                        String fileName = temp[2];
                        files.add(fileName);
                    }
                }
            }
        }
        // Add last commit
        addCommitIfNotEmpty(release, new Commit(hash, author, subject, files));
    }

    public static void getReleaseCommitRelatedMetrics(Project project, Release... releases)
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
            pb.command("git", NP, "log", "--boundary", NUMSTAT, NO_MERGES, COMMIT_FORMAT,
                    firstCommit + ".." + MessageFormat.format(project.getReleaseString(), releases[0].getName()));
        } else if (releases.length == 2) {
            pb.command("git", NP, "log", NUMSTAT, NO_MERGES, COMMIT_FORMAT,
                    MessageFormat.format(project.getReleaseString(), releases[0].getName()) + ".." +
                            MessageFormat.format(project.getReleaseString(), releases[1].getName()));
        }
        Process pr = pb.start();
        String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        if (releases.length == 1) {
            parseMainLines(output, releases[0]);
        } else if (releases.length == 2) {
            parseMainLines(output, releases[1]);
        }
    }

    public static void getReleaseCommits(Project project, Release first, Release second)
            throws IOException, InterruptedException {
        String projectName = project.getProjectName();
        File file = new File(projectName);
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(file);
        pb.command("git", NP, "log", NUMSTAT, NO_MERGES, COMMIT_FORMAT,
                MessageFormat.format(project.getReleaseString(), first.getName()) + ".." +
                        MessageFormat.format(project.getReleaseString(), second.getName()));
        Process pr = pb.start();
        String output = IOUtils.toString(pr.getInputStream(), StandardCharsets.UTF_8);
        pr.waitFor();
        parseDroppedLines(output, second);
    }

    public static void getCommitRelatedMetrics(Project project, List<Release> releases)
            throws IOException, InterruptedException {
        // Get commits for first release
        getReleaseCommitRelatedMetrics(project, releases.get(0));
        for (int i = 1; i < releases.size(); i++) {
            getReleaseCommitRelatedMetrics(project, releases.get(i - 1), releases.get(i));
        }
    }

    public static void getCommits(Project project, List<Release> releases, Release lastMain)
            throws IOException, InterruptedException {
        // Get commits for first release
        getReleaseCommits(project, lastMain, releases.get(0));
        for (int i = 1; i < releases.size(); i++) {
            getReleaseCommits(project, releases.get(i - 1), releases.get(i));
        }
    }

}
