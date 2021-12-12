package it.lorenzoval.deliverable2;

public class Commit {

    private final String hash;
    private final String author;
    private final String subject;

    public Commit(String hash, String author, String subject) {
        this.hash = hash;
        this.author = author;
        this.subject = subject;
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

}
