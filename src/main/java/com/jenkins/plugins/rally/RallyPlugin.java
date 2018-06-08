package com.jenkins.plugins.rally;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.jenkins.plugins.rally.actions.RallyArtifactsAction;
import com.jenkins.plugins.rally.config.*;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.credentials.RallyCredentials;
import com.jenkins.plugins.rally.credentials.RallyCredentialsUIHelper;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.service.RallyService;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.List;

/**
 * @author Tushar Shinde
 * @author R. Michael Rogers
 */
public class RallyPlugin extends Publisher implements SimpleBuildStep{
    private final RallyPluginConfiguration config;
    private RallyService rallyService;
    private ScmConnector jenkinsConnector;
    private String credentialsId;
    private boolean failOnErrors;

    @DataBoundConstructor
    public RallyPlugin(String credentialsId, String rallyWorkspaceName, String rallyScmName, String shouldCreateIfAbsent, String scmCommitTemplate, String scmFileTemplate, String buildCaptureRange, String advancedProxyUri, String shouldCaptureBuildStatus) throws RallyException, URISyntaxException {
        this.credentialsId = credentialsId;

        RallyConfiguration rally = new RallyConfiguration(rallyWorkspaceName, rallyScmName, shouldCreateIfAbsent);
        ScmConfiguration scm = new ScmConfiguration(scmCommitTemplate, scmFileTemplate);
        BuildConfiguration build = new BuildConfiguration(buildCaptureRange);
        AdvancedConfiguration advanced = new AdvancedConfiguration(advancedProxyUri, shouldCaptureBuildStatus);

        this.config = new RallyPluginConfiguration(rally, scm, build, advanced);
    }

    @DataBoundSetter
    public void setFailOnErrors(boolean failOnErrors) {
        this.failOnErrors = failOnErrors;
    }

    public boolean isFailOnErrors() {
        return failOnErrors;
    }

    private String getRallyCredentials(String credentialsId) {
        List<RallyCredentials> rallyCredentialsList = CredentialsProvider.lookupCredentials(RallyCredentials.class, Jenkins.getInstance(), ACL.SYSTEM);
        RallyCredentials rallyCredentials = CredentialsMatchers.firstOrNull(rallyCredentialsList, CredentialsMatchers.allOf(CredentialsMatchers.withId(credentialsId)));

        return rallyCredentials == null ? null : rallyCredentials.getApiKey().getPlainText();
    }

    private void initialize() throws RallyException {
        AbstractModule module = new AbstractModule() {
            @Override
            protected void configure() {
                config.getRally().setApiKey(getRallyCredentials(credentialsId));

                bind(AdvancedConfiguration.class).toInstance(config.getAdvanced());
                bind(BuildConfiguration.class).toInstance(config.getBuild());
                bind(RallyConfiguration.class).toInstance(config.getRally());
                bind(ScmConfiguration.class).toInstance(config.getScm());
            }
        };

        Injector injector = Guice.createInjector(Modules.override(getDescriptor().getRallyGuiceModule()).with(module));
        injector.injectMembers(this);
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener) throws InterruptedException, IOException {
        doPerform(run, listener);
    }

    public void doPerform(@Nonnull Run<?, ?> run, @Nonnull TaskListener listener){
        try {
            initialize();
        } catch (RallyException exception) {
            System.out.println(exception.getMessage());
            exception.printStackTrace(System.out);
            return;
        }

        boolean shouldBuildSucceed = true;
        PrintStream out = listener.getLogger();
        List<RallyUpdateData> detailsList;
        try {
            detailsList = this.jenkinsConnector.getChanges(run, out);
        } catch (RallyException exception) {
            out.println("Unable to retrieve SCM changes from Jenkins: " + exception.getMessage());
            return;
        }

        RallyArtifactsAction rallyArtifactsAction = new RallyArtifactsAction();
        for (RallyUpdateData details : detailsList) {
            try {
                rallyArtifactsAction.addRallyArtifacts(this.rallyService.updateChangeset(details));
            } catch (Exception e) {
                out.println("\trally update plug-in error: could not update changeset entry: " + e.getClass().getName() + " " + e.getMessage());
                e.printStackTrace(out);
                shouldBuildSucceed = false;
            }
            if(shouldBuildSucceed) {
                try {
                    this.rallyService.updateRallyTaskDetails(details);
                } catch (Exception e) {
                    out.println("\trally update plug-in error: could not update TaskDetails entry: " + e.getClass().getName() + " " + e.getMessage());
                    e.printStackTrace(out);
                    shouldBuildSucceed = false;
                }
            }

            if (details.getIds().size() == 0) {
                out.println("Could not update rally due to absence of id in a comment " + details.getMsg());
            }
        }
        if(!rallyArtifactsAction.getRallyArtifacts().isEmpty()){
            run.addAction(rallyArtifactsAction);
        }

        try {
            this.rallyService.closeConnection();
        } catch (RallyException exception) {
            // Ignore
        }
        if (!shouldBuildSucceed && failOnErrors) {
            run.setResult(Result.FAILURE);
            throw new IllegalStateException("Rally plugin has failed to update artifacts status");
        }

    }

    @Inject
    public void setRallyService(RallyService service) {
        this.rallyService = service;
    }

    @Inject
    public void setScmConnector(ScmConnector connector) {
        this.jenkinsConnector = connector;
    }

    public RallyPluginConfiguration getConfig() {
        return config;
    }

    public String getCredentialsId() {
        return this.credentialsId;
    }

    public String getRallyWorkspaceName() {
        return this.config.getRally().getWorkspaceName();
    }

    public String getRallyScmName() {
        return this.config.getRally().getScmName();
    }

    public String getShouldCreateIfAbsent() {
        return this.config.getRally().shouldCreateIfAbsent().toString();
    }

    public String getScmCommitTemplate() {
        return this.config.getScm().getCommitTemplate();
    }

    public String getScmFileTemplate() {
        return this.config.getScm().getFileTemplate();
    }

    public String getBuildCaptureRange() {
        return this.config.getBuild().getCaptureRange();
    }

    public String getAdvancedProxyUri() {
        return this.config.getAdvanced().getProxyUri() != null ?
                this.config.getAdvanced().getProxyUri().toString() : null;
    }

    public String getShouldCaptureBuildStatus() {
        return this.config.getAdvanced().shouldCaptureBuildStatus().toString();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        // for testing purpose
        private transient RallyGuiceModule rallyGuiceModule;

        public DescriptorImpl(){
            this.rallyGuiceModule = new RallyGuiceModule();
        }

        public RallyGuiceModule getRallyGuiceModule(){
            return rallyGuiceModule;
        }
        public void setRallyGuiceModule(RallyGuiceModule rallyGuiceModule){
            this.rallyGuiceModule = rallyGuiceModule;
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This get displayed at 'Add build step' button.
         */
        @Override
        public String getDisplayName() {
            return "Update Rally Task and ChangeSet";
        }


        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Jenkins context,
                                                     @QueryParameter String remoteBase) {
            return RallyCredentialsUIHelper.doFillCredentialsIdItems(context, remoteBase);
        }
    }
}
