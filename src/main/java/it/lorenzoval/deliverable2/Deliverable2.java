package it.lorenzoval.deliverable2;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Deliverable2 {

    private static final Logger logger = Logger.getLogger(Deliverable2.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        Project syncope = new Syncope();
        Project bookkeeper = new Bookkeeper();
        GitHandler.cloneOrPull(syncope);
        GitHandler.cloneOrPull(bookkeeper);

        /* List<Issue> bugs = JIRAHandler.getBugs(syncope);

        for (Issue bug : bugs) {
            logger.log(Level.INFO, bug.getKey());
        } */

        List<Release> syncopeReleases = JIRAHandler.getReleases(syncope);
        int i = 0;

        for (Release release : syncopeReleases) {
            i++;
            logger.log(Level.INFO, "{0} {1}", new Object[]{release.getName(), release.getReleaseDate()});
        }
        logger.log(Level.INFO, "Total releases: {0}", i);

        List<Release> bookkeeperReleases = JIRAHandler.getReleases(bookkeeper);
        i = 0;

        for (Release release : bookkeeperReleases) {
            i++;
            logger.log(Level.INFO, "{0} {1}", new Object[]{release.getName(), release.getReleaseDate()});
        }
        logger.log(Level.INFO, "Total releases: {0}", i);
    }

}
