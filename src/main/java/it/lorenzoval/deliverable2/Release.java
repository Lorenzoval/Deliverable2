package it.lorenzoval.deliverable2;

import java.time.LocalDate;
import java.util.*;

public class Release implements Comparable<Release> {

    private final String name;
    private final LocalDate gitReleaseDate; // Used for file age
    private final LocalDate jiraReleaseDate; // Used for operations related to bugs
    private final Map<String, Metrics> files;
    private final List<Commit> commits;

    public Release(String name, LocalDate gitReleaseDate, LocalDate jiraReleaseDate) {
        this.name = name;
        this.gitReleaseDate = gitReleaseDate;
        this.jiraReleaseDate = jiraReleaseDate;
        this.files = new HashMap<>();
        this.commits = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public LocalDate getGitReleaseDate() {
        return this.gitReleaseDate;
    }

    public LocalDate getJiraReleaseDate() {
        return this.jiraReleaseDate;
    }

    public Map<String, Metrics> getFiles() {
        return this.files;
    }

    public List<Commit> getCommits() {
        return this.commits;
    }

    public void addFile(String fileName, long loc, LocalDate creationDate) {
        files.put(fileName, new Metrics(loc, creationDate, gitReleaseDate));
    }

    public void addFile(String fileName) {
        // Dummy addFile for dropped releases
        files.put(fileName, new Metrics());
    }

    public void addCommit(Commit commit) {
        this.commits.add(commit);
    }

    public void updateMetrics(String fileName, String author, int chgSetSize, int locAdded, int locDeleted) {
        files.computeIfPresent(fileName, (k, v) -> v.updateFromCommit(author, chgSetSize, locAdded, locDeleted));
    }

    public void increaseFixes(String fileName) {
        files.computeIfPresent(fileName, (k, v) -> v.increaseFixes());
    }

    @Override
    public int compareTo(Release release) {
        return this.gitReleaseDate.compareTo(release.getGitReleaseDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;
        Release release = (Release) o;
        return this.name.equals(release.name) && this.gitReleaseDate.equals(release.gitReleaseDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.gitReleaseDate);
    }

}
