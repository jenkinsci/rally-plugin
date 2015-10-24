package com.jenkins.plugins.rally.config;

import com.google.inject.Inject;

public class RallyConfiguration {
    private String apiKey;
    private final String workspaceName;
    private final String scmName;
    private final String shouldCreateIfAbsent;

    @Inject
    public RallyConfiguration(String workspaceName, String scmName, String shouldCreateIfAbsent) {
        this.workspaceName = workspaceName;
        this.scmName = scmName;
        this.shouldCreateIfAbsent = shouldCreateIfAbsent;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public String getScmName() {
        return scmName;
    }

    public Boolean shouldCreateIfAbsent() {
        return Boolean.parseBoolean(this.shouldCreateIfAbsent);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
