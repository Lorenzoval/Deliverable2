package it.lorenzoval.deliverable2;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Deliverable2 {

    public static void buildDataset(Project project) throws IOException, InterruptedException {
        File outFile = new File(project.getProjectName() + ".csv");
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        lines.add("Version,File Name,LOC");
        List<Release> releases = JIRAHandler.getReleases(project);
        for (Release release : releases) {
            GitHandler.changeRelease(project, release);
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
