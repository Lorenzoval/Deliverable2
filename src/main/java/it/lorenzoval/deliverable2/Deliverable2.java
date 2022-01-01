package it.lorenzoval.deliverable2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Deliverable2 {

    private static final Logger logger = Logger.getLogger(Deliverable2.class.getName());

    public static void writeDatasetToCSV(Project project, List<Release> releases) throws IOException {
        File outFile = new File(project.getProjectName() + "_metrics.csv");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        lines.add("Version,File Name,LOC,LOC_touched,NR,NFix,NAuth,LOC_added,MAX_LOC_added,AVG_LOC_added,Churn," +
                "MAX_Churn,AVG_Churn,ChgSetSize,MAX_ChgSet,AVG_ChgSet,Age,WeightedAge,Buggy");
        for (Release release : releases) {
            Map<String, Metrics> fileMetrics = release.getFiles();
            for (Map.Entry<String, Metrics> entry : fileMetrics.entrySet()) {
                Metrics metrics = entry.getValue();
                line.setLength(0);
                line.append(release.getId()).append(",").append(entry.getKey()).append(",").append(metrics.getLoc())
                        .append(",").append(metrics.getLocTouched()).append(",").append(metrics.getNumRevs())
                        .append(",").append(metrics.getNumFixes()).append(",").append(metrics.getNumAuthors())
                        .append(",").append(metrics.getLocAdded()).append(",").append(metrics.getMaxLocAdded())
                        .append(",").append(metrics.getAvgLocAdded()).append(",").append(metrics.getChurn())
                        .append(",").append(metrics.getMaxChurn()).append(",").append(metrics.getAvgChurn())
                        .append(",").append(metrics.getChgSetSize()).append(",").append(metrics.getMaxChgSetSize())
                        .append(",").append(metrics.getAvgChgSetSize()).append(",").append(metrics.getAge())
                        .append(",").append(metrics.getWeightedAge()).append(",")
                        .append(metrics.isBuggy() ? "Yes" : "No");
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

    public static void setBuggyFiles(ReleasesList releasesList, List<Issue> bugs) {
        int lastId = releasesList.getMain().get(releasesList.getMain().size() - 1).getId();
        for (Issue bug : bugs) {
            for (Release release : bug.getAffectedVersions()) {
                if (release.getId() <= lastId) {
                    for (String file : bug.getAffectedFiles()) {
                        release.setBuggy(file);
                    }
                } else {
                    break;
                }
            }
        }
    }

    public static void buildDataset(Project project) throws IOException, InterruptedException {
        ReleasesList releasesList = new ReleasesList(JIRAHandler.getReleases(project));
        logger.log(Level.INFO, "Gathering metrics for {0}", project.getProjectName());
        getFiles(project, releasesList);
        GitHandler.getCommitRelatedMetrics(project, releasesList.getMain());
        GitHandler.getCommits(project, releasesList.getDropped(),
                releasesList.getMain().get(releasesList.getMain().size() - 1));
        logger.log(Level.INFO, "Gathering issues for {0}", project.getProjectName());
        List<Issue> bugs = JIRAHandler.getBugs(project, releasesList);
        setBuggyFiles(releasesList, bugs);
        writeDatasetToCSV(project, releasesList.getMain());
    }

    public static void main(String[] args) throws Exception {
        Project syncope = new Syncope();
        Project bookkeeper = new Bookkeeper();
        logger.log(Level.INFO, "Updating projects");
        GitHandler.cloneOrPull(syncope);
        GitHandler.cloneOrPull(bookkeeper);
        logger.log(Level.INFO, "Generating datasets");
        buildDataset(syncope);
        buildDataset(bookkeeper);
        WekaHandler.evaluateDataset(syncope);
        WekaHandler.evaluateDataset(bookkeeper);
    }

}
