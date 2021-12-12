package it.lorenzoval.deliverable2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Math.min;

public class Deliverable2 {

    private static final Logger logger = Logger.getLogger(Deliverable2.class.getName());

    public static boolean isHigherVersionNumber(String version1, String version2) {
        List<String> version1Digits = Arrays.asList(version1.split("\\."));
        List<String> version2Digits = Arrays.asList(version2.split("\\."));

        int minSize = min(version1Digits.size(), version2Digits.size());
        boolean lower = false;
        boolean higher = false;
        for (int i = 0; i < minSize; i++) {
            if (Integer.parseInt(version1Digits.get(i)) < Integer.parseInt(version2Digits.get(i))) {
                lower = true;
            } else if (Integer.parseInt(version1Digits.get(i)) > Integer.parseInt(version2Digits.get(i))) {
                higher = true;
            }
            if (lower || higher)
                break;
        }
        if (!lower && !higher)
            return version1Digits.size() >= version2Digits.size();
        else
            return higher;
    }

    public static List<Release> dropBackwardCompatibility(List<Release> releases) {
        Release prev = null;
        final Pattern pattern = Pattern.compile("\\d+[.\\d]*");
        List<Release> retList = new ArrayList<>();
        for (Release release : releases) {
            if (prev == null) {
                prev = release;
                retList.add(release);
                continue;
            }
            String prevNumber = prev.getName();
            String currNumber = release.getName();
            Matcher matcher = pattern.matcher(prevNumber);
            if (matcher.find())
                prevNumber = matcher.group();
            else
                logger.log(Level.SEVERE, "Matching not found");
            matcher = pattern.matcher(currNumber);
            if (matcher.find())
                currNumber = matcher.group();
            else
                logger.log(Level.SEVERE, "Matching not found");
            if (isHigherVersionNumber(currNumber, prevNumber)) {
                prev = release;
                retList.add(release);
            } else {
                logger.log(Level.INFO, "Release {0} released after {1}, discarded", new Object[]{release.getName(),
                        prev.getName()});
            }
        }
        return retList;
    }

    public static List<Release> dropLastFiftyPercent(List<Release> releases) {
        List<Release> retList = new ArrayList<>();
        for (int i = 0; i < releases.size() / 2; i++)
            retList.add(releases.get(i));
        return retList;
    }

    public static void writeToCSV(Project project, List<Release> releases) throws IOException {
        File outFile = new File(project.getProjectName() + ".csv");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        lines.add("Version,File Name,LOC,LOC_touched,NR,NAuth,LOC_added,MAX_LOC_added,AVG_LOC_added,Churn,MAX_Churn," +
                "AVG_Churn,ChgSetSize,MAX_ChgSet,AVG_ChgSet,Age,WeightedAge");
        for (Release release : releases) {
            Map<String, Metrics> fileMetrics = release.getFiles();
            for (Map.Entry<String, Metrics> entry : fileMetrics.entrySet()) {
                Metrics metrics = entry.getValue();
                line.setLength(0);
                line.append(release.getName()).append(",").append(entry.getKey()).append(",").append(metrics.getLoc())
                        .append(",").append(metrics.getLocTouched()).append(",").append(metrics.getNumRevs())
                        .append(",").append(metrics.getNumAuthors()).append(",").append(metrics.getLocAdded())
                        .append(",").append(metrics.getMaxLocAdded()).append(",").append(metrics.getAvgLocAdded())
                        .append(",").append(metrics.getChurn()).append(",").append(metrics.getMaxChurn())
                        .append(",").append(metrics.getAvgChurn()).append(",").append(metrics.getChgSetSize())
                        .append(",").append(metrics.getMaxChgSetSize()).append(",").append(metrics.getAvgChgSetSize())
                        .append(",").append(metrics.getAge()).append(",").append(metrics.getWeightedAge());
                lines.add(line.toString());
            }
        }
        FileUtils.writeLines(outFile, lines);
    }

    public static void getFiles(Project project, List<Release> releases) throws IOException, InterruptedException {
        for (Release release : releases) {
            GitHandler.changeRelease(project, release);
            List<String> files = GitHandler.getFiles(project);
            for (String file : files) {
                long loc;
                if (file.endsWith(".java")) {
                    try (Stream<String> fileLines = Files.lines(Paths.get(project.getProjectName(), file))) {
                        loc = fileLines.count();
                    }
                    LocalDate creationDate = GitHandler.getFileCreationDate(project, file);
                    release.addFile(file, loc, creationDate);
                }
            }
        }
    }

    public static void buildDataset(Project project) throws IOException, InterruptedException {
        List<Release> allReleases = JIRAHandler.getReleases(project);
        Collections.sort(allReleases);
        List<Release> mainReleases = dropBackwardCompatibility(allReleases);
        List<Release> releases = dropLastFiftyPercent(mainReleases);
        getFiles(project, releases);
        List<Commit> commits = new ArrayList<>();
        GitHandler.getCommitRelatedMetrics(project, commits, releases);
        writeToCSV(project, releases);
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
