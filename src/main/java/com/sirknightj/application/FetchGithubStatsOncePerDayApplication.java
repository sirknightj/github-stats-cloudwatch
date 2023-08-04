package com.sirknightj.application;

import com.sirknightj.GithubAPI;
import com.sirknightj.utils.RepoInfo;
import lombok.extern.log4j.Log4j2;
import com.sirknightj.cloudwatch.GitHubStatsCloudWatchPublisher;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log4j2
public class FetchGithubStatsOncePerDayApplication implements AutoCloseable {

    private final ScheduledExecutorService scheduler;
    private ZonedDateTime lastTime;
    private final GitHubStatsCloudWatchPublisher ghSCWP;

    // Interval to fetch stats.
    private final Duration interval = Duration.ofHours(24);

    // Fetch stats at midnight+offset time each day
    public FetchGithubStatsOncePerDayApplication(final GithubAPI githubAPI, final List<Map.Entry<String, String>> reposToFetch, final Duration midnightOffset) {
        final ZonedDateTime now = ZonedDateTime.now();
        final ZonedDateTime startOfDay = now.toLocalDate().atStartOfDay(now.getZone())
                .plus(midnightOffset);

        this.lastTime = startOfDay;
        ZonedDateTime laterTonight = startOfDay.plus(this.interval);

        while (laterTonight.isBefore(now)) {
            this.lastTime = laterTonight;
            laterTonight = laterTonight.plus(this.interval);
        }

        log.info("Will fetch stats at: {}", laterTonight);

        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        this.ghSCWP = new GitHubStatsCloudWatchPublisher();

        this.scheduler.scheduleAtFixedRate(() -> {
                    log.info("Starting to fetch stats!");
                    final ZonedDateTime thisTime = this.lastTime.plus(this.interval);
                    try {
                        final List<RepoInfo> stats = githubAPI.fetchGithubIssuesFrom(reposToFetch, this.lastTime, thisTime.minusSeconds(1));
                        this.ghSCWP.publish(stats, thisTime);
                    } catch (final Exception ex) {
                        log.error(ex);
                    }
                    this.lastTime = thisTime;
                    log.info("Finished fetching stats! Will next fire at: {}", thisTime.plus(this.interval));

                },
                Duration.between(now, laterTonight).getSeconds(),
                this.interval.getSeconds(),
                TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        this.scheduler.shutdownNow();
        this.ghSCWP.close();
    }
}
