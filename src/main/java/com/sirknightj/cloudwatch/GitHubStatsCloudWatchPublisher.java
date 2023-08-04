package com.sirknightj.cloudwatch;

import com.sirknightj.utils.CloudwatchConstants;
import com.sirknightj.utils.RepoInfo;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.CloudWatchException;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class GitHubStatsCloudWatchPublisher implements AutoCloseable {

    final CloudWatchClient cw;

    public GitHubStatsCloudWatchPublisher() {
        this.cw = CloudWatchClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }

    @Override
    public void close() throws Exception {
        this.cw.close();
    }

    public void publish(final List<RepoInfo> repoInfo) {
        log.info("Starting to publish to CloudWatch: {}", repoInfo);
        try {
            final List<MetricDatum> metricDataList = new ArrayList<>();

            for (final RepoInfo repo : repoInfo) {
                final String repositoryName = repo.getRepositoryName();
                final String repositoryOwner = repo.getRepositoryOwner();
                final int openIssues = repo.getCurrentIssuesOpen();
                final int openPRs = repo.getCurrentPullRequestsOpen();

                // Set an Instant object.
                final String time = ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                final Instant instant = Instant.parse(time);

                final Dimension[] dimensions = {
                        Dimension.builder().name(CloudwatchConstants.repositoryNameDimensionName).value(repositoryName).build(),
                        Dimension.builder().name(CloudwatchConstants.repositoryOwnerDimensionName).value(repositoryOwner).build()
                };

                final MetricDatum openIssuesDatam = MetricDatum.builder()
                        .metricName(CloudwatchConstants.openIssuesMetricName)
                        .dimensions(dimensions)
                        .unit(StandardUnit.COUNT)
                        .value((double) openIssues)
                        .timestamp(instant)
                        .build();

                final MetricDatum openPRsDatam = MetricDatum.builder()
                        .metricName(CloudwatchConstants.openPullRequestsMetricName)
                        .dimensions(dimensions)
                        .unit(StandardUnit.COUNT)
                        .value((double) openPRs)
                        .timestamp(instant)
                        .build();

                final MetricDatum openedPRsDatam = MetricDatum.builder()
                        .metricName(CloudwatchConstants.openedPullRequestsMetricName)
                        .dimensions(dimensions)
                        .unit(StandardUnit.COUNT)
                        .value((double) repo.getPullRequestsOpened())
                        .timestamp(instant)
                        .build();

                final MetricDatum closedPRsDatam = MetricDatum.builder()
                        .metricName(CloudwatchConstants.closedPullRequestsMetricName)
                        .dimensions(dimensions)
                        .unit(StandardUnit.COUNT)
                        .value((double) repo.getPullRequestsClosed())
                        .timestamp(instant)
                        .build();

                final MetricDatum openedIssuesDatam = MetricDatum.builder()
                        .metricName(CloudwatchConstants.openedIssuesMetricName)
                        .dimensions(dimensions)
                        .unit(StandardUnit.COUNT)
                        .value((double) repo.getIssuesOpened())
                        .timestamp(instant)
                        .build();

                final MetricDatum closedIssuesDatam = MetricDatum.builder()
                        .metricName(CloudwatchConstants.closedIssuesMetricName)
                        .dimensions(dimensions)
                        .unit(StandardUnit.COUNT)
                        .value((double) repo.getIssuesClosed())
                        .timestamp(instant)
                        .build();

                metricDataList.add(openIssuesDatam);
                metricDataList.add(openPRsDatam);
                metricDataList.add(openedPRsDatam);
                metricDataList.add(closedPRsDatam);
                metricDataList.add(openedIssuesDatam);
                metricDataList.add(closedIssuesDatam);
            }

            final PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(CloudwatchConstants.customMetricNamespace)
                    .metricData(metricDataList)
                    .build();

            this.cw.putMetricData(request);
        } catch (final CloudWatchException e) {
            log.error(e.awsErrorDetails().errorMessage());
        }
        log.info("Finished publishing to CloudWatch: {}", repoInfo);
    }
}
