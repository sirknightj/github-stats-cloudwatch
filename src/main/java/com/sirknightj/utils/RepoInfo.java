package com.sirknightj.utils;

import lombok.Data;

@Data
public class RepoInfo {
    private final String repositoryOwner;
    private final String repositoryName;
    private final int currentIssuesOpen;
    private final int currentPullRequestsOpen;

    private final int issuesClosed;
    private final int issuesOpened;

    private final int pullRequestsClosed;
    private final int pullRequestsOpened;
}
