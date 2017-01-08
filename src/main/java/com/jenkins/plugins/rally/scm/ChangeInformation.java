package com.jenkins.plugins.rally.scm;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;

public class ChangeInformation {

    private String buildTimeStamp;
    private String buildNumber;
    private ChangeLogSet changeLogSet;
    private Run build;

    public String getBuildTimeStamp() {
        return buildTimeStamp;
    }

    public void setBuildTimeStamp(String buildTimeStamp) {
        this.buildTimeStamp = buildTimeStamp;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public ChangeLogSet getChangeLogSet() {
        return changeLogSet;
    }

    public void setChangeLogSet(ChangeLogSet changeLogSet) {
        this.changeLogSet = changeLogSet;
    }

    public Run getBuild() {
        return build;
    }

    public void setBuild(Run build) {
        this.build = build;
    }
}
