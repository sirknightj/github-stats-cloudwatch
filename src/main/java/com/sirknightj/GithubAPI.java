package com.sirknightj;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sirknightj.utils.DateHelper;
import com.sirknightj.utils.RepoInfo;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

@Log4j2
public class GithubAPI {

    private final String githubToken;
    private final Gson gson;

    private final long sleepMs;

    public GithubAPI() {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param authToken GitHub personal access token. Can be {@code null} or empty string if you don't
     *                  want to use one.
     */
    public GithubAPI(final String authToken) {
        this.githubToken = authToken;
        this.gson = new Gson();

        if (githubToken == null || githubToken.length() == 0) {
            // Rate limit to search api is 10 requests per minute, unauthenticated
            sleepMs = Duration.ofSeconds(6).toMillis();
        } else {
            // Rate limit to search api is 30 requests per minute, authenticated
            sleepMs = Duration.ofSeconds(2).toMillis();
        }
    }

    public List<RepoInfo> fetchGithubIssuesFrom(final List<Map.Entry<String, String>> reposToFetch, final ZonedDateTime start, final ZonedDateTime end) throws IOException, InterruptedException {
        final List<RepoInfo> repoInfo = new ArrayList<>();
        for (final Map.Entry<String, String> repo : reposToFetch) {
            log.info("Working on: {}/{}.", repo.getKey(), repo.getValue());

            try {
                final int currentIssuesCount = fetchOpenIssuesCount(repo.getKey(), repo.getValue());
                log.info("{}/{} - Open pull requests: {}", repo.getKey(), repo.getValue(), currentIssuesCount);

                // Doesn't use search API, so not required.
                final int currentPullRequestsCount = fetchOpenPullRequestsCount(repo.getKey(), repo.getValue());
                log.info("{}/{} - Open issues: {}", repo.getKey(), repo.getValue(), currentPullRequestsCount);

                Thread.sleep(sleepMs);

                final int issuesOpenedInterval = fetchIssuesOpenedBetween(repo.getKey(), repo.getValue(), start, end);
                log.info("{}/{} - Issues opened between {} and {}: {}", repo.getKey(), repo.getValue(), start, end, issuesOpenedInterval);

                Thread.sleep(sleepMs);

                final int issuesClosedInterval = fetchIssuesClosedBetween(repo.getKey(), repo.getValue(), start, end);
                log.info("{}/{} - Issues closed between {} and {}: {}", repo.getKey(), repo.getValue(), start, end, issuesClosedInterval);

                Thread.sleep(sleepMs);

                final int pullRequestsOpenedInterval = fetchPullRequestsOpenedBetween(repo.getKey(), repo.getValue(), start, end);
                log.info("{}/{} - Pull requests opened between {} and {}: {}", repo.getKey(), repo.getValue(), start, end, pullRequestsOpenedInterval);

                Thread.sleep(sleepMs);

                final int pullRequestsClosedInterval = fetchPullRequestsClosedBetween(repo.getKey(), repo.getValue(), start, end);
                log.info("{}/{} - Pull requests closed between {} and {}: {}", repo.getKey(), repo.getValue(), start, end, pullRequestsClosedInterval);

                Thread.sleep(sleepMs);

                repoInfo.add(new RepoInfo(repo.getKey(), repo.getValue(), currentIssuesCount, currentPullRequestsCount,
                        issuesClosedInterval, issuesOpenedInterval, pullRequestsClosedInterval, pullRequestsOpenedInterval));

            } catch (final Exception ex) {
                log.error("Encountered error working on: {}/{}.", repo.getKey(), repo.getValue(), ex);
            }
        }

        return repoInfo;
    }

    /**
     * Fetch the current number of open issues (only issues, not pull requests) in a GitHub Repository.
     *
     * @param repositoryOwner Repository owner.
     * @param repositoryName  Name of the repository.
     * @return A number (0+) if successful. -1 if there was an error.
     * @throws IOException If something went wrong.
     */
    public int fetchOpenIssuesCount(final String repositoryOwner, final String repositoryName) throws IOException {
        final String githubApiLink = new StringBuilder()
                .append("https://api.github.com/search/issues?q=repo:")
                .append(repositoryOwner)
                .append("/")
                .append(repositoryName)
                .append("+is%3Aissue+state%3Aopen")
                .toString();


        final URL url = new URL(githubApiLink);
        final HttpResult result = fetch(url);

        final JsonObject jsonObject = this.gson.fromJson(result.getMessage(), JsonObject.class);

        return jsonObject.get("total_count").getAsInt();
    }

    /**
     * Fetch the current number of open pull requests in a GitHub Repository.
     *
     * @param repositoryOwner Repository owner.
     * @param repositoryName  Name of the repository.
     * @return A number (0+) if successful. -1 if there was an error.
     * @throws IOException If something went wrong.
     */
    public int fetchOpenPullRequestsCount(final String repositoryOwner, final String repositoryName) throws IOException {
        final String githubApiLink = new StringJoiner("/")
                .add("https://api.github.com/repos")
                .add(repositoryOwner)
                .add(repositoryName)
                .add("pulls?state=open")
                .toString();

        final URL url = new URL(githubApiLink);
        final HttpResult result = fetch(url);

        final JsonArray jsonArray = this.gson.fromJson(result.getMessage(), JsonArray.class);
        return jsonArray.size();
    }

    public int fetchIssuesOpenedBetween(final String repositoryOwner, final String repositoryName,
                                        final ZonedDateTime startInclusive, final ZonedDateTime endInclusive) throws IOException {
        final String githubApiLink = new StringBuilder()
                .append("https://api.github.com/search/issues?q=repo:")
                .append(repositoryOwner)
                .append("/")
                .append(repositoryName)
                .append("+is%3Aissue+created%3A")
                .append(DateHelper.getFormattedAndURLEncodedDateString(startInclusive))
                .append("..")
                .append(DateHelper.getFormattedAndURLEncodedDateString(endInclusive))
                .toString();


        final URL url = new URL(githubApiLink);
        final HttpResult result = fetch(url);

        final JsonObject jsonObject = this.gson.fromJson(result.getMessage(), JsonObject.class);

        return jsonObject.get("total_count").getAsInt();
    }

    public int fetchIssuesClosedBetween(final String repositoryOwner, final String repositoryName,
                                        final ZonedDateTime startInclusive, final ZonedDateTime endInclusive) throws IOException {

        final String githubApiLink = new StringBuilder()
                .append("https://api.github.com/search/issues?q=repo:")
                .append(repositoryOwner)
                .append("/")
                .append(repositoryName)
                .append("+is%3Aissue+closed%3A")
                .append(DateHelper.getFormattedAndURLEncodedDateString(startInclusive))
                .append("..")
                .append(DateHelper.getFormattedAndURLEncodedDateString(endInclusive))
                .toString();


        final URL url = new URL(githubApiLink);
        final HttpResult result = fetch(url);

        final JsonObject jsonObject = this.gson.fromJson(result.getMessage(), JsonObject.class);

        return jsonObject.get("total_count").getAsInt();
    }

    public int fetchPullRequestsOpenedBetween(final String repositoryOwner, final String repositoryName,
                                              final ZonedDateTime startInclusive, final ZonedDateTime endInclusive) throws IOException {

        final String githubApiLink = new StringBuilder()
                .append("https://api.github.com/search/issues?q=repo:")
                .append(repositoryOwner)
                .append("/")
                .append(repositoryName)
                .append("+is%3Apr+created%3A")
                .append(DateHelper.getFormattedAndURLEncodedDateString(startInclusive))
                .append("..")
                .append(DateHelper.getFormattedAndURLEncodedDateString(endInclusive))
                .toString();


        final URL url = new URL(githubApiLink);
        final HttpResult result = fetch(url);

        final JsonObject jsonObject = this.gson.fromJson(result.getMessage(), JsonObject.class);

        return jsonObject.get("total_count").getAsInt();
    }

    public int fetchPullRequestsClosedBetween(final String repositoryOwner, final String repositoryName,
                                              final ZonedDateTime startInclusive, final ZonedDateTime endInclusive) throws IOException {

        final String githubApiLink = new StringBuilder()
                .append("https://api.github.com/search/issues?q=repo:")
                .append(repositoryOwner)
                .append("/")
                .append(repositoryName)
                .append("+is%3Apr+closed%3A")
                .append(DateHelper.getFormattedAndURLEncodedDateString(startInclusive))
                .append("..")
                .append(DateHelper.getFormattedAndURLEncodedDateString(endInclusive))
                .toString();


        final URL url = new URL(githubApiLink);
        final HttpResult result = fetch(url);

        final JsonObject jsonObject = this.gson.fromJson(result.getMessage(), JsonObject.class);

        return jsonObject.get("total_count").getAsInt();
    }

    private HttpResult fetch(final URL url) throws IOException {
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        attachAuthHeader(conn);
        conn.connect();
        try {
            final StringBuilder result = new StringBuilder();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    for (String line; (line = reader.readLine()) != null; ) {
                        result.append(line);
                    }
                }

                final String response = result.toString();
                log.error("Error fetching {}. Received HTTP error code {}. Message: {}", url, conn.getResponseCode(), response);

                return new HttpResult(conn.getResponseCode(), response);
            }
            // conn.responseCode == HTTP_OK

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    result.append(line);
                }
            }
            return new HttpResult(HttpURLConnection.HTTP_OK, result.toString());
        } finally {
            conn.disconnect();
        }
    }

    private void attachAuthHeader(final HttpURLConnection conn) {
        if (this.githubToken != null && this.githubToken.length() > 0) {
            conn.setRequestProperty("Authorization", "token " + this.githubToken);
        }
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
    }

    @Data
    private static class HttpResult {
        private final int statusCode;
        private final String message;
    }
}
