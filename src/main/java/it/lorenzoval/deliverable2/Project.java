package it.lorenzoval.deliverable2;

public abstract class Project {

    private final String url;
    private final String projectName;
    private final String releaseString;
    private final double movingWindow;

    protected Project(String url, String projectName, String releaseString, double movingWindow) {
        this.url = url;
        this.projectName = projectName;
        this.releaseString = releaseString;
        this.movingWindow = movingWindow;
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

    public double getMovingWindow() {
        return this.movingWindow;
    }
}
