package com.jenkins.plugins.rally.pipeline;

import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.jenkins.plugins.rally.*;
import com.jenkins.plugins.rally.actions.RallyArtifactsAction;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.credentials.RallyCredentialsImpl;
import hudson.model.Result;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import org.eclipse.jgit.api.Git;
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class RallyBuildStatusStepTest {
    private static final String COMMIT_MESSAGE = "test US12345 DE12345";
    private static final String ERROR_STORY = "US44444";
    private static final String ERROR_DEFECT = "DE44444";
    private static final String ERROR_COMMIT_MESSAGE = "error in " + ERROR_DEFECT + " " + ERROR_STORY;


    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public RuleChain chain = RuleChain.outerRule(jRule).around(tmp);

    @Inject
    @SuppressWarnings({"UnusedDeclaration"})
    private RallyPlugin.DescriptorImpl descriptor;

    @Mock
    private RallyConnector rallyConnector;

    @Before
    public void setUp() throws Exception{
        SystemCredentialsProvider.getInstance().getCredentials().add(
            new RallyCredentialsImpl("iii", "iii", "iii", "iii")
        );
        SystemCredentialsProvider.getInstance().save();
        RallyGlobalConfiguration.get().setBuildCaptureRange("SinceLastBuild");
        RallyGlobalConfiguration.get().setShouldCaptureBuildStatus("true");
        RallyGlobalConfiguration.get().setRallyWorkspaceName("sss");
        RallyGlobalConfiguration.get().setShouldCreateIfAbsent("true");
        RallyGlobalConfiguration.get().setCredentialsId("iii");
        jRule.jenkins.getInjector().injectMembers(this);
        descriptor.setRallyGuiceModule(new RallyGuiceModule(){
            @Override
            protected void bindRallyConnector() {
                bind(RallyConnector.class).toInstance(rallyConnector);
            }
        });
        when(rallyConnector.queryForStory(anyString())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            String id = invocationOnMock.getArgumentAt(0, String.class);
            if(id.equals(ERROR_STORY)){
                throw new RallyException("Failure to get from Rally!");
            }
            return new RallyArtifact("_ref", id, "name");
        });
        when(rallyConnector.queryForDefect(anyString())).thenAnswer((InvocationOnMock invocationOnMock) -> {
            String id = invocationOnMock.getArgumentAt(0, String.class);
            if(id.equals(ERROR_DEFECT)){
                throw new RallyException("Failure to get from Rally!");
            }
            return new RallyArtifact("_ref", id, "name");
        });

    }

    @Test
    public void shouldDetectRallyArtifactsUsingPipeline() throws Exception{
        File root = tmp.getRoot();
        File jenkinsFile = new File(root, "Jenkinsfile");
        Files.write(jenkinsFile.toPath(), Arrays.asList(
            "echo 'hello world'",
            "node{ checkout scm }"
        ));
        Git.init().setDirectory(tmp.getRoot()).call();
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("Jenkinsfile").call();
        git.commit().setMessage(COMMIT_MESSAGE).call();
        String repositoryUrl = tmp.getRoot().toURI().toString();
        SCM scm = new GitSCM(repositoryUrl);

        WorkflowJob job = jRule.jenkins.createProject(WorkflowJob.class, "test");
        CpsScmFlowDefinition definition = new CpsScmFlowDefinition(scm, "Jenkinsfile");
        job.setDefinition(definition);

        WorkflowRun run = job.scheduleBuild2(0).get();
        jRule.assertBuildStatusSuccess(run);

        // Second time build for getting changelog
        Files.write(jenkinsFile.toPath(), Arrays.asList(
            "echo 'hello world'",
            "node{ checkout scm }",
            "rallyBuildStatus()"
        ));
        git.add().addFilepattern("Jenkinsfile").call();
        git.commit().setMessage(COMMIT_MESSAGE).call();

        run = job.scheduleBuild2(0).get();
        jRule.assertBuildStatusSuccess(run);

        RallyArtifactsAction action = run.getAction(RallyArtifactsAction.class);
        assertThat(action, is(notNullValue()));
        assertThat(action.getRallyArtifacts(), is(not(empty())));
        assertThat(action.getRallyArtifacts().size(), is(equalTo(2)));

    }

    private void testFailedRallyPlugin(boolean failOnError, Result expectedResult) throws Exception{
        File root = tmp.getRoot();
        File jenkinsFile = new File(root, "Jenkinsfile");
        Files.write(jenkinsFile.toPath(), Arrays.asList(
                "echo 'hello world'",
                "node{ checkout scm }"
        ));
        Git.init().setDirectory(tmp.getRoot()).call();
        Git git = Git.open(tmp.getRoot());
        git.add().addFilepattern("Jenkinsfile").call();
        git.commit().setMessage(ERROR_COMMIT_MESSAGE).call();
        String repositoryUrl = tmp.getRoot().toURI().toString();
        SCM scm = new GitSCM(repositoryUrl);

        WorkflowJob job = jRule.jenkins.createProject(WorkflowJob.class, "test");
        CpsScmFlowDefinition definition = new CpsScmFlowDefinition(scm, "Jenkinsfile");
        job.setDefinition(definition);

        WorkflowRun run = job.scheduleBuild2(0).get();
        jRule.assertBuildStatusSuccess(run);

        // Second time build for getting changelog
        Files.write(jenkinsFile.toPath(), Arrays.asList(
                "echo 'hello world'",
                "node{ checkout scm }",
                "rallyBuildStatus(failOnErrors: " + failOnError + " )",
                "currentBuild.result = 'SUCCESS'"
        ));
        git.add().addFilepattern("Jenkinsfile").call();
        git.commit().setMessage(ERROR_COMMIT_MESSAGE).call();
        run = job.scheduleBuild2(0).get();
        if(expectedResult == Result.SUCCESS){
            jRule.assertBuildStatusSuccess(run);
        }else {
            jRule.assertBuildStatus(expectedResult, run);
        }

    }

    @Test
    public void shouldFailOnErrors() throws Exception{
        testFailedRallyPlugin(true, Result.FAILURE);
    }

    @Test
    public void shouldNotFailOnErrors() throws Exception{
        testFailedRallyPlugin(false, Result.SUCCESS);
    }

}
