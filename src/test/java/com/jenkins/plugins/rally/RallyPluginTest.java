package com.jenkins.plugins.rally;

import com.cloudbees.hudson.plugins.folder.AbstractFolder;
import com.cloudbees.hudson.plugins.folder.Folder;
import com.cloudbees.hudson.plugins.folder.properties.FolderCredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.google.common.base.Joiner;
import com.jenkins.plugins.rally.credentials.RallyCredentials;
import com.jenkins.plugins.rally.credentials.RallyCredentialsImpl;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.util.ListBoxModel;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class RallyPluginTest {
    private static final String API_KEY = "API_KEY";
    private static final String WORKSPACE_NAME = "WORKSPACE_NAME";
    private static final String COMMIT_URI_STRING = "COMMIT_URI_STRING";
    private static final String FILE_URI_STRING = "FILE_URI_STRING";
    private static final String SCM_NAME = "SCM_NAME";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldStoreConfigurationForRecall() throws Exception {
        String[] keysToTest = {
                "rallyWorkspaceName",
                "rallyScmName",
                "shouldCreateIfAbsent",
                "scmCommitTemplate",
                "scmFileTemplate",
                "buildCaptureRange",
                "advancedProxyUri",
                "shouldCaptureBuildStatus"
        };

        FreeStyleProject p = jenkins.getInstance().createProject(FreeStyleProject.class, "testProject");
        RallyPlugin before = new RallyPlugin(API_KEY, WORKSPACE_NAME, SCM_NAME, "true", COMMIT_URI_STRING, FILE_URI_STRING, "SinceLastBuild", "http://proxy.url", "true");
        p.getPublishersList().add(before);

        jenkins.submit(jenkins.createWebClient().getPage(p,"configure").getFormByName("config"));

        RallyPlugin after = p.getPublishersList().get(RallyPlugin.class);

        jenkins.assertEqualBeans(before, after, Joiner.on(',').join(keysToTest));
    }

    @Test
    public void shouldStoreOtherConfigurationForRecall() throws Exception {
        String[] keysToTest = {
                "rallyWorkspaceName",
                "rallyScmName",
                "shouldCreateIfAbsent",
                "scmCommitTemplate",
                "scmFileTemplate",
                "buildCaptureRange",
                "advancedProxyUri",
                "shouldCaptureBuildStatus"
        };

        FreeStyleProject job = jenkins.getInstance().createProject(FreeStyleProject.class, "testProject");
        RallyPlugin before = new RallyPlugin(API_KEY, WORKSPACE_NAME, SCM_NAME, "false", COMMIT_URI_STRING, FILE_URI_STRING, "SinceLastSuccessfulBuild", "http://proxy.url", "false");
        job.getPublishersList().add(before);
        job.save();

        jenkins.submit(jenkins.createWebClient().getPage(job,"configure").getFormByName("config"));

        RallyPlugin after = job.getPublishersList().get(RallyPlugin.class);

        jenkins.assertEqualBeans(before, after, Joiner.on(',').join(keysToTest));
    }

    @Test
    public void testWithGlobalCredentials() throws Exception {

        String globalCredentialsId = "global-rally-creds";

        RallyCredentials key = new RallyCredentialsImpl(CredentialsScope.GLOBAL, globalCredentialsId, "test-global-creds",
                "global-test-creds", "global-rally-secret-key");
        SystemCredentialsProvider.getInstance().getCredentials().add(key);
        SystemCredentialsProvider.getInstance().save();

        FreeStyleProject job = jenkins.getInstance().createProject(FreeStyleProject.class, "testWithGlobalCredentials");
        RallyPlugin before = new RallyPlugin(globalCredentialsId, WORKSPACE_NAME, SCM_NAME, "false", COMMIT_URI_STRING, FILE_URI_STRING, "SinceLastSuccessfulBuild", "http://proxy.url", "false");
        job.getPublishersList().add(before);
        job.save();
        jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    public void testWithFolderCredentials() throws Exception {

        String folderCredentialsId = "folders-rally-creds";

        // Create a folder with credentials in its store
        Folder folder = jenkins.getInstance().createProject(Folder.class, "folder" + jenkins.getInstance().getItems().size());
        CredentialsStore folderStore = getFolderStore(folder);

        RallyCredentials inFolderCredentials = new RallyCredentialsImpl(CredentialsScope.GLOBAL, folderCredentialsId, "test-folder-creds", "folder-test-creds", "folder-rally-secret-key");
        folderStore.addCredentials(Domain.global(), inFolderCredentials);
        SystemCredentialsProvider.getInstance().save();

        AbstractProject job = folder.createProject(FreeStyleProject.class, "testWithFolderCredentials");
        RallyPlugin before = new RallyPlugin(folderCredentialsId, WORKSPACE_NAME, SCM_NAME, "false", COMMIT_URI_STRING, FILE_URI_STRING, "SinceLastSuccessfulBuild", "http://proxy.url", "false");
        job.getPublishersList().add(before);
        job.save();
        jenkins.assertBuildStatusSuccess(job.scheduleBuild2(0));
    }

    @Test
    public void testListCredentials() throws Exception {
        Folder folder = jenkins.getInstance().createProject(Folder.class, "folder" + jenkins.getInstance().getItems().size());
        CredentialsStore folderStore = getFolderStore(folder);

        RallyCredentials folderCredentials = new RallyCredentialsImpl(CredentialsScope.GLOBAL,"folder-creds", "test-creds", "test-creds", "rally-secret-key");
        RallyCredentials globalCredentials = new RallyCredentialsImpl(CredentialsScope.GLOBAL,"global-creds", "test-creds", "test-creds", "rally-secret-key");

        folderStore.addCredentials(Domain.global(), folderCredentials);
        SystemCredentialsProvider.getInstance().getCredentials().add(globalCredentials);
        SystemCredentialsProvider.getInstance().save();

        AbstractProject job = folder.createProject(FreeStyleProject.class, "testWithFolderCredentials");
        final RallyPlugin.DescriptorImpl descriptor = jenkins.getInstance().getDescriptorByType(RallyPlugin.DescriptorImpl.class);

        // 3 options: Root credentials, folder credentials and "none"
        ListBoxModel list = descriptor.doFillCredentialsIdItems(job, null);
        Assert.assertEquals(3, list.size());

        RallyCredentials systemCredentials = new RallyCredentialsImpl(CredentialsScope.SYSTEM,
                "system-creds", "test-creds", "test-creds", "rally-secret-key");
        SystemCredentialsProvider.getInstance().getCredentials().add(systemCredentials);

        // 3 options: Root credentials, folder credentials and "none" but no system scoped credentials
        list = descriptor.doFillCredentialsIdItems(job, null);
        Assert.assertEquals(3, list.size());
    }

    private CredentialsStore getFolderStore(AbstractFolder f) {
        Iterable<CredentialsStore> stores = CredentialsProvider.lookupStores(f);
        CredentialsStore folderStore = null;
        for (CredentialsStore s : stores) {
            if (s.getProvider() instanceof FolderCredentialsProvider && s.getContext() == f) {
                folderStore = s;
                break;
            }
        }
        return folderStore;
    }
}