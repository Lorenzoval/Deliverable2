package it.lorenzoval.deliverable2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

    public static void buildDataset(Project project) throws IOException, InterruptedException {
        File outFile = new File(project.getProjectName() + ".csv");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        lines.add("Version,File Name,LOC");
        List<Release> allReleases = JIRAHandler.getReleases(project);
        Collections.sort(allReleases);
        List<Release> mainReleases = dropBackwardCompatibility(allReleases);
        List<Release> releases = dropLastFiftyPercent(mainReleases);
        for (Release release : releases) {
            if (GitHandler.changeRelease(project, release) != 0)
                continue;
            List<String> files = GitHandler.getFiles(project);
            for (String file : files) {
                line.setLength(0);
                long loc;
                try (Stream<String> fileLines = Files.lines(Paths.get(project.getProjectName(), file))) {
                    loc = fileLines.count();
                }
                line.append(release.getName()).append(",").append(file).append(",").append(loc);
                lines.add(line.toString());
            }
        }
        FileUtils.writeLines(outFile, lines);
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
