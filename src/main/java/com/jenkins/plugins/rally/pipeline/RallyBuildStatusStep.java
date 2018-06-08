package com.jenkins.plugins.rally.pipeline;

import com.google.common.collect.ImmutableSet;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.RallyGlobalConfiguration;
import com.jenkins.plugins.rally.RallyPlugin;
import com.jenkins.plugins.rally.credentials.RallyCredentialsUIHelper;
import hudson.Extension;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.net.URISyntaxException;
import java.util.Set;

@SuppressWarnings("unused")
public class RallyBuildStatusStep extends Step {
    private String credentialsId;
    private String rallyWorkspaceName;
    private String rallyScmName;
    private Boolean shouldCreateIfAbsent;
    private String scmCommitTemplate;
    private String scmFileTemplate;
    private String buildCaptureRange;
    private String advancedProxyUri;
    private Boolean shouldCaptureBuildStatus;
    private Boolean failOnErrors;

    @DataBoundConstructor
    public RallyBuildStatusStep(){

    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialsId) {
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    public String getRallyWorkspaceName() {
        return rallyWorkspaceName;
    }

    @DataBoundSetter
    public void setRallyWorkspaceName(String rallyWorkspaceName) {
        this.rallyWorkspaceName = Util.fixEmpty(rallyWorkspaceName);
    }

    public String getRallyScmName() {
        return rallyScmName;
    }

    @DataBoundSetter
    public void setRallyScmName(@Nonnull String rallyScmName) {
        this.rallyScmName = Util.fixEmpty(rallyScmName);
    }

    public Boolean isShouldCreateIfAbsent() {
        return shouldCreateIfAbsent;
    }

    @DataBoundSetter
    public void setShouldCreateIfAbsent(Boolean shouldCreateIfAbsent) {
        this.shouldCreateIfAbsent = shouldCreateIfAbsent;
    }

    public String getScmCommitTemplate() {
        return scmCommitTemplate;
    }

    @DataBoundSetter
    public void setScmCommitTemplate(String scmCommitTemplate) {
        this.scmCommitTemplate = Util.fixEmpty(scmCommitTemplate);
    }

    public String getScmFileTemplate() {
        return scmFileTemplate;
    }

    @DataBoundSetter
    public void setScmFileTemplate(String scmFileTemplate) {
        this.scmFileTemplate = Util.fixEmpty(scmFileTemplate);
    }

    public String getBuildCaptureRange() {
        return buildCaptureRange;
    }

    @DataBoundSetter
    public void setBuildCaptureRange(String buildCaptureRange) {
        this.buildCaptureRange = Util.fixEmpty(buildCaptureRange);
    }

    public String getAdvancedProxyUri() {
        return advancedProxyUri;
    }

    @DataBoundSetter
    public void setAdvancedProxyUri(String advancedProxyUri) {
        this.advancedProxyUri = Util.fixEmpty(advancedProxyUri);
    }

    public Boolean isShouldCaptureBuildStatus() {
        return shouldCaptureBuildStatus;
    }

    @DataBoundSetter
    public void setShouldCaptureBuildStatus(Boolean shouldCaptureBuildStatus) {
        this.shouldCaptureBuildStatus = shouldCaptureBuildStatus;
    }

    @DataBoundSetter
    public void setFailOnErrors(Boolean failOnErrors) {
        this.failOnErrors = failOnErrors;
    }

    public Boolean isFailOnErrors(){
        return this.failOnErrors;
    }

    @Override
    public StepExecution start(StepContext stepContext){
        return new Execution(stepContext, this);
    }

    @Extension
    @SuppressWarnings("unused")
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return ImmutableSet.of(Run.class, TaskListener.class);
        }

        @Override
        public String getFunctionName() {
            return "rallyBuildStatus";
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return "Analyze change-sets and update rally artifacts";
        }

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context,
                                                     @QueryParameter String remoteBase) {
            return RallyCredentialsUIHelper.doFillCredentialsIdItems(context, remoteBase);
        }

    }

    public static class Execution extends SynchronousNonBlockingStepExecution<Void> {
        private static final long serialVersionUID = 0L;

        private transient final RallyBuildStatusStep step;

        Execution(@Nonnull StepContext context, @Nonnull RallyBuildStatusStep step) {
            super(context);
            this.step = step;
        }

        @Override
        @SuppressWarnings("ConstantConditions")
        protected Void run() throws Exception {
            Run<?,?> run = getContext().get(Run.class);
            TaskListener taskListener = getContext().get(TaskListener.class);

            RallyGlobalConfiguration globalConfiguration = RallyGlobalConfiguration.get();

            String credentialsId = StringUtils.isEmpty(step.getCredentialsId()) ? globalConfiguration.getCredentialsId() : step.getCredentialsId();
            String rallyWorkspaceName = StringUtils.isEmpty(step.getRallyWorkspaceName()) ? globalConfiguration.getRallyWorkspaceName() : step.getRallyWorkspaceName();
            String rallyScmName = StringUtils.isEmpty(step.getRallyScmName()) ? run.getParent().getFullName() : step.getRallyScmName();
            boolean shouldCreateRepoIfAbsent = step.isShouldCreateIfAbsent() == null ? "true".equals(globalConfiguration.getShouldCreateIfAbsent()) : step.isShouldCreateIfAbsent();
            String scmCommitTemplate = StringUtils.isEmpty(step.getScmCommitTemplate()) ? globalConfiguration.getScmCommitTemplate() : step.getScmCommitTemplate();
            String scmFileTemplate = StringUtils.isEmpty(step.getScmFileTemplate()) ? globalConfiguration.getScmFileTemplate() : step.getScmFileTemplate();
            String buildCaptureRange = StringUtils.isEmpty(step.getBuildCaptureRange()) ? globalConfiguration.getBuildCaptureRange() : step.getBuildCaptureRange();
            String advancedProxyUri = StringUtils.isEmpty(step.getAdvancedProxyUri()) ? globalConfiguration.getAdvancedProxyUri() : step.getAdvancedProxyUri();
            boolean shouldCaptureBuildStatus = step.isShouldCaptureBuildStatus() == null ? "true".equals(globalConfiguration.getShouldCaptureBuildStatus()) : step.isShouldCaptureBuildStatus();
            boolean failOnErrors = step.isFailOnErrors() == null ? "true".equals(globalConfiguration.getFailOnErrors()) : step.isFailOnErrors();

            RallyPlugin plugin = getRallyPlugin(
                credentialsId,
                rallyWorkspaceName,
                rallyScmName,
                shouldCreateRepoIfAbsent,
                scmCommitTemplate,
                scmFileTemplate,
                buildCaptureRange,
                advancedProxyUri,
                shouldCaptureBuildStatus
            );
            plugin.setFailOnErrors(failOnErrors);

            plugin.doPerform(run, taskListener);

            return null;
        }

        RallyPlugin getRallyPlugin(String credentialsId, String rallyWorkspaceName, String rallyScmName, boolean shouldCreateRepoIfAbsent,
                                   String scmCommitTemplate, String scmFileTemplate, String buildCaptureRange, String advancedProxyUri,
                                   boolean shouldCaptureBuildStatus)  throws RallyException, URISyntaxException {
            return new RallyPlugin(
                    credentialsId,
                    rallyWorkspaceName,
                    rallyScmName,
                    String.valueOf(shouldCreateRepoIfAbsent),
                    scmCommitTemplate,
                    scmFileTemplate,
                    buildCaptureRange,
                    advancedProxyUri,
                    String.valueOf(shouldCaptureBuildStatus)
            );
        }
    }

}
