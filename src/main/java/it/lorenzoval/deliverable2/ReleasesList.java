package it.lorenzoval.deliverable2;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.min;

public class ReleasesList {

    private static final Logger logger = Logger.getLogger(ReleasesList.class.getName());
    private final List<Release> main;
    private final List<Release> dropped;

    public ReleasesList(List<Release> releases) {
        Collections.sort(releases);
        List<Release> temp = dropBackwardCompatibility(releases);
        int i = 1;
        for (Release release : temp) {
            release.setId(i);
            i++;
        }
        this.main = dropLastFiftyPercent(temp);
        this.dropped = new ArrayList<>(temp.subList(this.main.size(), temp.size()));
    }

    public List<Release> getMain() {
        return this.main;
    }

    public List<Release> getDropped() {
        return this.dropped;
    }

    private boolean isHigherVersionNumber(String version1, String version2) {
        List<String> version1Digits = Arrays.asList(version1.split("\\."));
        List<String> version2Digits = Arrays.asList(version2.split("\\."));

        int minSize = min(version1Digits.size(), version2Digits.size());
        boolean lower = false;
        boolean higher = false;
        for (int i = 0; i < minSize; i++) {
            if (Integer.parseInt(version1Digits.get(i)) < Integer.parseInt(version2Digits.get(i))) {
                lower = true;
            } else if (Integer.parseInt(version1Digits.get(i)) > Integer.parseInt(version2Digits.get(i))) {
                higher = true;
            }
            if (lower || higher)
                break;
        }
        if (!lower && !higher)
            return version1Digits.size() >= version2Digits.size();
        else
            return higher;
    }

    private List<Release> dropBackwardCompatibility(List<Release> releases) {
        Release prev = null;
        final Pattern pattern = Pattern.compile("\\d+[.\\d]*");
        List<Release> retList = new ArrayList<>();
        for (Release release : releases) {
            if (prev == null) {
                prev = release;
                retList.add(release);
                continue;
            }
            String prevNumber = prev.getName();
            String currNumber = release.getName();
            Matcher matcher = pattern.matcher(prevNumber);
            if (matcher.find())
                prevNumber = matcher.group();
            else
                logger.log(Level.SEVERE, "Matching not found");
            matcher = pattern.matcher(currNumber);
            if (matcher.find())
                currNumber = matcher.group();
            else
                logger.log(Level.SEVERE, "Matching not found");
            if (isHigherVersionNumber(currNumber, prevNumber)) {
                prev = release;
                retList.add(release);
            } else {
                logger.log(Level.INFO, "Release {0} released after {1}, discarded", new Object[]{release.getName(),
                        prev.getName()});
            }
        }
        return retList;
    }

    private List<Release> dropLastFiftyPercent(List<Release> releases) {
        List<Release> retList = new ArrayList<>();
        for (int i = 0; i < releases.size() / 2; i++)
            retList.add(releases.get(i));
        return retList;
    }

    public Release getReleaseByDate(LocalDate date) {
        // Check last releaseDate of main releases
        if (main.get(main.size() - 1).getJiraReleaseDate().isBefore(date)) {
            for (Release release : dropped) {
                if (release.getJiraReleaseDate().isAfter(date))
                    return release;
            }
        } else {
            for (Release release : main) {
                if (release.getJiraReleaseDate().isAfter(date))
                    return release;
            }
        }
        return null;
    }

    public Release getReleaseByName(String name) {
        for (Release release : main) {
            if (release.getName().equals(name))
                return release;
        }
        for (Release release : dropped) {
            if (release.getName().equals(name))
                return release;
        }
        return null;
    }

    private void avInDropped(List<Release> releases, double iv, int fv) {
        for (Release release : dropped) {
            if (release.getId() == fv)
                break;
            if (release.getId() >= iv)
                releases.add(release);
        }
    }

    private void avBetweenMainAndDropped(List<Release> releases, double iv, int fv) {
        for (Release release : main)
            if (release.getId() >= iv)
                releases.add(release);
        for (Release release : dropped) {
            if (release.getId() == fv)
                break;
            releases.add(release);
        }
    }

    private void avInMain(List<Release> releases, double iv, int fv) {
        for (Release release : main) {
            if (release.getId() == fv)
                break;
            if (release.getId() >= iv)
                releases.add(release);
        }
    }

    public List<Release> getReleasesBetween(double iv, int fv) {
        List<Release> releases = new ArrayList<>();
        if (iv > main.get(main.size() - 1).getId()) {
            avInDropped(releases, iv, fv);
        } else {
            if (fv > main.get(main.size() - 1).getId()) {
                avBetweenMainAndDropped(releases, iv, fv);
            } else {
                avInMain(releases, iv, fv);
            }
        }
        return releases;
    }

}
