package it.lorenzoval.deliverable2;

import java.time.LocalDate;
import java.util.Objects;

public class Release implements Comparable<Release> {

    private final String name;
    private final LocalDate releaseDate;

    public Release(String name, LocalDate releaseDate) {
        this.name = name;
        this.releaseDate = releaseDate;
    }

    public String getName() {
        return this.name;
    }

    public LocalDate getReleaseDate() {
        return this.releaseDate;
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
