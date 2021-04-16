package it.lorenzoval.deliverable2;

public abstract class Project {

    private final String url;
    private final String projectName;
    private final String releaseString;

    protected Project(String url, String projectName, String releaseString) {
        this.url = url;
        this.projectName = projectName;
        this.releaseString = releaseString;
    }

    public String getUrl() {
        return this.url;
    }

    public String getProjectName() {
        return this.projectName;
    }

    public String getReleaseString() {
        return this.releaseString;
    }

}
