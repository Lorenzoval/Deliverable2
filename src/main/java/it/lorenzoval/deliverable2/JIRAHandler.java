package it.lorenzoval.deliverable2;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JIRAHandler {

    private static final String API_URL = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22{0}%22" +
            "AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR%22status%22=%22resolved%22)AND" +
            "%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt={1}&maxResults={2}";
    private static final Logger logger = Logger.getLogger(JIRAHandler.class.getName());

    private JIRAHandler() {
    }

    public static List<Issue> getBugs(Project project) {
        int i = 0;
        int j;
        int total = 1;
        String url;
        List<Issue> bugs = new ArrayList<>();

        do {
            j = i + 1000;
            url = MessageFormat.format(API_URL, project.getProjectName().toUpperCase(Locale.ROOT), Integer.toString(i),
                    Integer.toString(j));

            try (InputStream in = new URL(url).openStream()) {
                JSONObject json = new JSONObject(IOUtils.toString(in, StandardCharsets.UTF_8));
                JSONArray issues = json.getJSONArray("issues");
                total = json.getInt("total");

                for (; i < total && i < j; i++) {
                    bugs.add(new Issue(issues.getJSONObject(i % 1000).get("key").toString()));
                }

            } catch (IOException ioe) {
                logger.log(Level.WARNING, ioe.toString());
            }

        } while (i < total);

        return bugs;
    }

}
