package com.jenkins.plugins.rally;

import com.google.common.base.Joiner;
import hudson.model.FreeStyleProject;
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
                "advancedProxyUri"
        };

        FreeStyleProject p = jenkins.getInstance().createProject(FreeStyleProject.class, "testProject");
        RallyPlugin before = new RallyPlugin(API_KEY, WORKSPACE_NAME, SCM_NAME, "true", COMMIT_URI_STRING, FILE_URI_STRING, "SinceLastBuild", "http://proxy.url");
        p.getBuildersList().add(before);

        jenkins.submit(jenkins.createWebClient().getPage(p,"configure").getFormByName("config"));

        RallyPlugin after = p.getBuildersList().get(RallyPlugin.class);

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
                "advancedProxyUri"
        };

        FreeStyleProject p = jenkins.getInstance().createProject(FreeStyleProject.class, "testProject");
        RallyPlugin before = new RallyPlugin(API_KEY, WORKSPACE_NAME, SCM_NAME, "false", COMMIT_URI_STRING, FILE_URI_STRING, "SinceLastSuccessfulBuild", "http://proxy.url");
        p.getBuildersList().add(before);

        jenkins.submit(jenkins.createWebClient().getPage(p,"configure").getFormByName("config"));

        RallyPlugin after = p.getBuildersList().get(RallyPlugin.class);

        jenkins.assertEqualBeans(before, after, Joiner.on(',').join(keysToTest));
    }
}