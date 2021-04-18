package it.lorenzoval.deliverable2;

import java.time.LocalDate;

public class Release {

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

}
