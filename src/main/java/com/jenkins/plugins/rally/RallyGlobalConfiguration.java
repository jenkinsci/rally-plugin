package com.jenkins.plugins.rally;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.jenkins.plugins.rally.credentials.RallyCredentials;
import com.jenkins.plugins.rally.credentials.RallyCredentialsUIHelper;
import hudson.Extension;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@Extension(ordinal = 100)
@Symbol("rallyGlobalConfiguration")
@SuppressWarnings("unused")
public class RallyGlobalConfiguration extends GlobalConfiguration{
    private String credentialsId;
    private String rallyWorkspaceName;
    private String shouldCreateIfAbsent = String.valueOf(true);
    private String scmCommitTemplate;
    private String scmFileTemplate;
    private String buildCaptureRange;
    private String advancedProxyUri;
    private String shouldCaptureBuildStatus = String.valueOf(true);
    private String failOnErrors = String.valueOf(false);

    public RallyGlobalConfiguration(){
        load();
    }

    public static RallyGlobalConfiguration get(){
        return GlobalConfiguration.all().get(RallyGlobalConfiguration.class);
    }

    public String getShouldCreateIfAbsent() {
        return shouldCreateIfAbsent;
    }

    public void setShouldCreateIfAbsent(String shouldCreateIfAbsent) {
        this.shouldCreateIfAbsent = shouldCreateIfAbsent;
        save();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
        save();
    }

    public String getRallyWorkspaceName() {
        return rallyWorkspaceName;
    }

    public void setRallyWorkspaceName(String rallyWorkspaceName) {
        this.rallyWorkspaceName = rallyWorkspaceName;
        save();
    }

    public String getScmCommitTemplate() {
        return scmCommitTemplate;
    }

    public void setScmCommitTemplate(String scmCommitTemplate) {
        this.scmCommitTemplate = scmCommitTemplate;
        save();
    }

    public String getScmFileTemplate() {
        return scmFileTemplate;
    }

    public void setScmFileTemplate(String scmFileTemplate) {
        this.scmFileTemplate = scmFileTemplate;
        save();
    }

    public String getBuildCaptureRange() {
        return buildCaptureRange;
    }

    public void setBuildCaptureRange(String buildCaptureRange) {
        this.buildCaptureRange = buildCaptureRange;
        save();
    }

    public String getAdvancedProxyUri() {
        return advancedProxyUri;
    }

    public void setAdvancedProxyUri(String advancedProxyUri) {
        this.advancedProxyUri = advancedProxyUri;
        save();
    }

    public String getShouldCaptureBuildStatus() {
        return shouldCaptureBuildStatus;
    }

    public void setShouldCaptureBuildStatus(String shouldCaptureBuildStatus) {
        this.shouldCaptureBuildStatus = shouldCaptureBuildStatus;
        save();
    }

    public void setFailOnErrors(String failOnErrors) {
        this.failOnErrors = failOnErrors;
        save();
    }

    public String getFailOnErrors() {
        return failOnErrors;
    }

    @SuppressWarnings("unused") // used by stapler
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context,
                                                 @QueryParameter String remoteBase) {
        return RallyCredentialsUIHelper.doFillCredentialsIdItems(context, remoteBase);
    }
}

