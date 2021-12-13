package it.lorenzoval.deliverable2;

import java.util.List;

public class Commit {

    private final String hash;
    private final String author;
    private final String subject;
    private final List<String> files;

    public Commit(String hash, String author, String subject, List<String> files) {
        this.hash = hash;
        this.author = author;
        this.subject = subject;
        this.files = files;
    }

    public String getHash() {
        return this.hash;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getSubject() {
        return this.subject;
    }

    public List<String> getFiles() {
        return this.files;
    }

}
