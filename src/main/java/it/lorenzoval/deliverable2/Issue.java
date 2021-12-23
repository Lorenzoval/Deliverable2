package it.lorenzoval.deliverable2;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Issue {

    private final String key;
    private final Set<String> affectedFiles;
    private final Release openingVersion;
    private final List<Release> affectedVersions;
    private final OffsetDateTime resolutionDate;
    private Release fixedVersion;

    public Issue(String key, Release openingVersion, OffsetDateTime resolutionDate) {
        this.key = key;
        this.affectedFiles = new HashSet<>();
        this.openingVersion = openingVersion;
        this.affectedVersions = new ArrayList<>();
        this.resolutionDate = resolutionDate;
    }

    public String getKey() {
        return this.key;
    }

    public Set<String> getAffectedFiles() {
        return this.affectedFiles;
    }

    public Release getOpeningVersion() {
        return this.openingVersion;
    }

    public List<Release> getAffectedVersions() {
        return this.affectedVersions;
    }

    public Release getInjectedVersion() {
        return this.affectedVersions.get(0);
    }

    public Release getFixedVersion() {
        return this.fixedVersion;
    }

    public void setFixedVersion(Release fixedVersion) {
        this.fixedVersion = fixedVersion;
    }

    public OffsetDateTime getResolutionDate() {
        return this.resolutionDate;
    }

    public void addAffectedFile(String file) {
        this.affectedFiles.add(file);
    }

    public void addAffectedVersions(List<Release> affectedVersions) {
        this.affectedVersions.addAll(affectedVersions);
    }

}
