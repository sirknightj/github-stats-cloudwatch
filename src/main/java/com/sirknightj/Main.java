package com.sirknightj;

import com.sirknightj.application.FetchGithubStatsOncePerDayApplication;
import lombok.extern.log4j.Log4j2;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Log4j2
public class Main {

    public static void main(final String[] args) throws InterruptedException {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));

        // Optional. But GitHub's rate limits are extremely low, so adding this is recommended.
        // https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-fine-grained-personal-access-token
        final String githubToken = System.getenv("GITHUB_TOKEN");

        final GithubAPI githubAPI = new GithubAPI(githubToken);

        // List of repositories to fetch
        // Each entry of the list is a key-value pairs with the key as the owner and the value as the repository name
        final List<Map.Entry<String, String>> reposToFetch = List.of(
                Map.entry("facebookresearch", "audiocraft")
        );

        // 9:30 AM each day
        final Duration midnightOffset = Duration.ofHours(9).plusMinutes(30);

        try (final FetchGithubStatsOncePerDayApplication application = new FetchGithubStatsOncePerDayApplication(githubAPI, reposToFetch, midnightOffset)) {
            // Basically keep main() alive.
            Thread.sleep(1_000_000_000_000_000_000L);
        } catch (final Exception ex) {
            log.error(ex);
        }
        log.info("Finished!");
    }
}
