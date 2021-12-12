package it.lorenzoval.deliverable2;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Release implements Comparable<Release> {

    private final String name;
    private final LocalDate releaseDate;
    private final Map<String, Metrics> files;

    public Release(String name, LocalDate releaseDate) {
        this.name = name;
        this.releaseDate = releaseDate;
        this.files = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public LocalDate getReleaseDate() {
        return this.releaseDate;
    }

    public Map<String, Metrics> getFiles() {
        return this.files;
    }

    public void addFile(String fileName, long loc, LocalDate creationDate) {
        files.put(fileName, new Metrics(loc, creationDate, releaseDate));
    }

    public void updateMetrics(String fileName, String author, int chgSetSize, int locAdded, int locDeleted) {
        files.computeIfPresent(fileName, (k, v) -> v.updateFromCommit(author, chgSetSize, locAdded, locDeleted));
    }

    @Override
    public int compareTo(Release release) {
        return this.releaseDate.compareTo(release.getReleaseDate());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;
        Release release = (Release) o;
        return this.name.equals(release.name) && this.releaseDate.equals(release.releaseDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name, this.releaseDate);
    }

}
