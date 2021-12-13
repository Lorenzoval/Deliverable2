package it.lorenzoval.deliverable2;

import java.util.HashSet;
import java.util.Set;

public class Issue {

    private final String key;
    private final Set<String> affectedFiles;

    public Issue(String key) {
        this.key = key;
        this.affectedFiles = new HashSet<>();
    }

    public String getKey() {
        return this.key;
    }

    public Set<String> getAffectedFiles() {
        return this.affectedFiles;
    }

    public void addAffectedFile(String file) {
        this.affectedFiles.add(file);
    }

}
