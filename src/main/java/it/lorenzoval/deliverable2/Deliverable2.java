package it.lorenzoval.deliverable2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Deliverable2 {

    private static final Logger logger = Logger.getLogger(Deliverable2.class.getName());

    public static void writeToCSV(Project project, List<Release> releases) throws IOException {
        File outFile = new File(project.getProjectName() + ".csv");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        lines.add("Version,File Name,LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,AVG_LOC_added,Churn," +
                "MAX_Churn,AVG_Churn,ChgSetSize,MAX_ChgSet,AVG_ChgSet,Age,WeightedAge");
        for (Release release : releases) {
            Map<String, Metrics> fileMetrics = release.getFiles();
            for (Map.Entry<String, Metrics> entry : fileMetrics.entrySet()) {
                Metrics metrics = entry.getValue();
                line.setLength(0);
                line.append(release.getName()).append(",").append(entry.getKey()).append(",").append(metrics.getLoc())
                        .append(",").append(metrics.getLocTouched()).append(",").append(metrics.getNumRevs())
                        .append(",").append(metrics.getNumFixes()).append(",").append(metrics.getNumAuthors())
                        .append(",").append(metrics.getLocAdded()).append(",").append(metrics.getMaxLocAdded())
                        .append(",").append(metrics.getAvgLocAdded()).append(",").append(metrics.getChurn())
                        .append(",").append(metrics.getMaxChurn()).append(",").append(metrics.getAvgChurn())
                        .append(",").append(metrics.getChgSetSize()).append(",").append(metrics.getMaxChgSetSize())
                        .append(",").append(metrics.getAvgChgSetSize()).append(",").append(metrics.getAge())
                        .append(",").append(metrics.getWeightedAge());
                lines.add(line.toString());
            }
        }
        FileUtils.writeLines(outFile, lines);
    }

    public static void getFiles(Project project, List<Release> releases, boolean dropped)
            throws IOException, InterruptedException {
        for (Release release : releases) {
            GitHandler.changeRelease(project, release);
            List<String> files = GitHandler.getFiles(project);
            for (String file : files) {
                long loc;
                if (file.endsWith(".java")) {
                    if (!dropped) {
                        try (Stream<String> fileLines = Files.lines(Paths.get(project.getProjectName(), file))) {
                            loc = fileLines.count();
                        }
                        LocalDate creationDate = GitHandler.getFileCreationDate(project, file);
                        release.addFile(file, loc, creationDate);
                    } else {
                        release.addFile(file);
                    }
                }
            }
        }
    }

    public static void getFiles(Project project, ReleasesList releasesList) throws IOException, InterruptedException {
        getFiles(project, releasesList.getMain(), false);
        getFiles(project, releasesList.getDropped(), true);
    }

    public static void updateAffectedFiles(List<Release> releases, Issue bug, Pattern p, boolean dropped) {
        for (Release release : releases) {
            for (Commit commit : release.getCommits()) {
                Matcher m = p.matcher(commit.getSubject());
                if (m.find()) {
                    for (String file : commit.getFiles()) {
                        if (!dropped)
                            release.increaseFixes(file);
                        bug.addAffectedFile(file);
                    }
                }
            }
        }
    }

    public static void getAffectedFiles(ReleasesList releasesList, List<Issue> bugs) {
        ListIterator<Issue> iterator = bugs.listIterator();
        while (iterator.hasNext()) {
            Issue bug = iterator.next();
            Pattern p = Pattern.compile("\\b" + bug.getKey() + "\\b", Pattern.CASE_INSENSITIVE);
            updateAffectedFiles(releasesList.getMain(), bug, p, false);
            updateAffectedFiles(releasesList.getDropped(), bug, p, true);
            if (bug.getAffectedFiles().isEmpty()) {
                logger.log(Level.INFO, "Issue {0} has no commit associated, discarded", bug.getKey());
                iterator.remove();
            }
        }
    }

    public static void buildDataset(Project project) throws IOException, InterruptedException {
        ReleasesList releasesList = new ReleasesList(JIRAHandler.getReleases(project));
        getFiles(project, releasesList);
        GitHandler.getCommitRelatedMetrics(project, releasesList.getMain());
        GitHandler.getCommits(project, releasesList.getDropped(),
                releasesList.getMain().get(releasesList.getMain().size() - 1));
        List<Issue> bugs = JIRAHandler.getBugs(project);
        getAffectedFiles(releasesList, bugs);
        writeToCSV(project, releasesList.getMain());
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Project syncope = new Syncope();
        Project bookkeeper = new Bookkeeper();
        GitHandler.cloneOrPull(syncope);
        GitHandler.cloneOrPull(bookkeeper);
        buildDataset(syncope);
        buildDataset(bookkeeper);
    }

}
