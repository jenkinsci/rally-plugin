package com.jenkins.plugins.rally.connector;

import hudson.scm.EditType;

import java.io.PrintStream;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class RallyUpdateData {

    public static class FilenameAndAction {
        public String filename;
        public EditType action;
    }

    public static class RallyId {
        private String name;

        public RallyId(String id) {
            this.name = id;
        }

        public String getName() {
            return this.name;
        }

        public boolean isStory() {
            return this.name.toLowerCase().startsWith("us");
        }
    }

    private String msg;

    private String revision;
    private String timeStamp;
    private List<RallyId> ids = newArrayList();
    private List<FilenameAndAction> filenamesAndActions;
    private PrintStream out;
    private String origBuildNumber;
    private String currentBuildNumber;
    private String taskID = "";
    private String taskIndex = "";
    private String taskStatus = "";
    private String taskToDO = "";
    private String taskEstimates = "";
    private String taskActuals = "";
    private String buildUrl = "";
    private String buildName = "Default Build Name";

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }

    private String buildStatus = "";

    public String getBuildUrl() {
        return buildUrl;
    }

    public void setBuildUrl(String buildUrl) {
        this.buildUrl = buildUrl;
    }

    public double getBuildDuration() {
        return buildDuration;
    }

    public void setBuildDuration(double buildDuration) {
        this.buildDuration = buildDuration;
    }

    public String getBuildMessage() {
        return buildMessage;
    }

    public void setBuildMessage(String buildMessage) {
        this.buildMessage = buildMessage;
    }

    private double buildDuration = 0;
    private String buildMessage = "";

    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }
    public String getRevision() {
        return revision;
    }
    public void setRevision(String revision) {
        if(revision == null)
            this.revision = "0";
        else
            this.revision = revision;
    }
    public String getTimeStamp() {
        return timeStamp;
    }
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
    public List<RallyId> getIds() {
        return ids;
    }
    public void addId(String id) {
        this.ids.add(new RallyId(id));
    }
    public void addIds(List<String> ids) {
        for (String id : ids) {
            this.ids.add(new RallyId(id));
        }
    }
    public List<FilenameAndAction> getFilenamesAndActions() {
        return filenamesAndActions;
    }
    public void setFilenamesAndActions(List<FilenameAndAction> filenamesAndActions) {
        this.filenamesAndActions = filenamesAndActions;
    }
    public PrintStream getOut() {
        return out;
    }
    public void setOut(PrintStream out) {
        this.out = out;
    }
    public String getOrigBuildNumber() {
        return origBuildNumber;
    }
    public void setOrigBuildNumber(String origBuildNumber) {
        this.origBuildNumber = origBuildNumber;
    }
    public String getCurrentBuildNumber() {
        return currentBuildNumber;
    }
    public void setCurrentBuildNumber(String currentBuildNumber) {
        this.currentBuildNumber = currentBuildNumber;
    }
    public String getTaskID() {
        return taskID;
    }
    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }
    public String getTaskIndex() {
        return taskIndex;
    }
    public void setTaskIndex(String taskIndex) {
        this.taskIndex = taskIndex;
    }
    public String getTaskStatus() {
        return taskStatus;
    }
    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }
    public String getTaskToDO() {
        return taskToDO;
    }
    public void setTaskToDO(String taskToDO) {
        this.taskToDO = taskToDO;
    }
    public String getTaskEstimates() {
        return taskEstimates;
    }
    public void setTaskEstimates(String taskEstimates) {
        this.taskEstimates = taskEstimates;
    }
    public String getTaskActuals() {
        return taskActuals;
    }
    public void setTaskActuals(String taskActuals) {
        this.taskActuals = taskActuals;
    }
}
