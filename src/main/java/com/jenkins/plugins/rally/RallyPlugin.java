package com.jenkins.plugins.rally;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.jenkins.plugins.rally.config.*;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.credentials.RallyCredentials;
import com.jenkins.plugins.rally.credentials.RallyCredentialsImpl;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.service.RallyService;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * @author Tushar Shinde
 * @author R. Michael Rogers
 */
public class RallyPlugin extends Publisher {
    private final RallyPluginConfiguration config;
    private RallyService rallyService;
    private ScmConnector jenkinsConnector;
    private String credentialsId;

    @DataBoundConstructor
    public RallyPlugin(String credentialsId, String rallyWorkspaceName, String rallyScmName, String shouldCreateIfAbsent, String scmCommitTemplate, String scmFileTemplate, String buildCaptureRange, String advancedProxyUri, String shouldCaptureBuildStatus) throws RallyException, URISyntaxException {
        this.credentialsId = credentialsId;

        RallyConfiguration rally = new RallyConfiguration(rallyWorkspaceName, rallyScmName, shouldCreateIfAbsent);
        ScmConfiguration scm = new ScmConfiguration(scmCommitTemplate, scmFileTemplate);
        BuildConfiguration build = new BuildConfiguration(buildCaptureRange);
        AdvancedConfiguration advanced = new AdvancedConfiguration(advancedProxyUri, shouldCaptureBuildStatus);

        this.config = new RallyPluginConfiguration(rally, scm, build, advanced);
    }

    private String getRallyCredentials(String credentialsId, AbstractBuild context) {
        RallyCredentials rallyCredentials = CredentialsProvider.findCredentialById(credentialsId,
                RallyCredentialsImpl.class, context, Collections.<DomainRequirement>emptyList());
        CredentialsProvider.track(context, rallyCredentials);
        return rallyCredentials == null ? null : rallyCredentials.getApiKey().getPlainText();
    }

    private void initialize(final AbstractBuild build) throws RallyException {
        AbstractModule module = new AbstractModule() {
            @Override
            protected void configure() {
                config.getRally().setApiKey(getRallyCredentials(credentialsId, build));

                bind(AdvancedConfiguration.class).toInstance(config.getAdvanced());
                bind(BuildConfiguration.class).toInstance(config.getBuild());
                bind(RallyConfiguration.class).toInstance(config.getRally());
                bind(ScmConfiguration.class).toInstance(config.getScm());
            }
        };

        Injector injector = Guice.createInjector(Modules.override(new RallyGuiceModule()).with(module));
        injector.injectMembers(this);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        try {
            initialize(build);
        } catch (RallyException exception) {
            System.out.println(exception.getMessage());
            exception.printStackTrace(System.out);
            return false;
        }

        boolean shouldBuildSucceed = true;
        PrintStream out = listener.getLogger();

        List<RallyUpdateData> detailsList;
        try {
            detailsList = this.jenkinsConnector.getChanges(build, out);
        } catch (RallyException exception) {
            out.println("Unable to retrieve SCM changes from Jenkins: " + exception.getMessage());
            return false;
        }

        for (RallyUpdateData details : detailsList) {
            try {
                this.rallyService.updateChangeset(details);
            } catch (Exception e) {
                out.println("\trally update plug-in error: could not update changeset entry: " + e.getMessage());
                e.printStackTrace(out);
                shouldBuildSucceed = false;
            }

            try {
                this.rallyService.updateRallyTaskDetails(details);
            } catch (Exception e) {
                out.println("\trally update plug-in error: could not update TaskDetails entry: " + e.getMessage());
                e.printStackTrace(out);
                shouldBuildSucceed = false;
            }

            if (details.getIds().size() == 0) {
                out.println("Could not update rally due to absence of id in a comment " + details.getMsg());
            }
        }

        try {
            this.rallyService.closeConnection();
        } catch (RallyException exception) {
            // Ignore
        }

        return shouldBuildSucceed;
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
        return this.config.getAdvanced().getProxyUri().toString();
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

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This get displayed at 'Add build step' button.
         */
        public String getDisplayName() {
            return "Update Rally Task and ChangeSet";
        }

        @SuppressWarnings("unused") // used by stapler
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item context,
                                                     @QueryParameter String credential) {

            if(context == null && !Jenkins.getActiveInstance().hasPermission(Jenkins.ADMINISTER) ||
                    context != null && !context.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel().includeCurrentValue(credential);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            context,
                            RallyCredentialsImpl.class,
                            Collections.<DomainRequirement>emptyList(),
                            CredentialsMatchers.instanceOf(RallyCredentialsImpl.class));
        }
    }
}
