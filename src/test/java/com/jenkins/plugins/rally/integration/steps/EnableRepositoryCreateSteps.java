package com.jenkins.plugins.rally.integration.steps;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.scm.JenkinsConnector;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.service.RallyService;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.QueryResponse;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class EnableRepositoryCreateSteps {
    private RallyService rallyService;
    private Exception caughtException;
    private QueryResponse preexistingRepositoryObjectQueryResponse;
    private RallyRestApi rallyApi;

    @Given("^a build that is configured not to create an SCMRepository object$")
    public void a_build_that_is_configured_not_to_create_an_SCMRepository_object() throws Throwable {
        rallyApi = mock(RallyRestApi.class);

        RallyConnector.FactoryHelper factoryHelper = mock(RallyConnector.FactoryHelper.class);
        when(factoryHelper.createConnection(anyString(), anyString())).thenReturn(rallyApi);

        preexistingRepositoryObjectQueryResponse = mock(QueryResponse.class);
        when(rallyApi.query(argThat(new IsQueryRequestForRepository()))).thenReturn(preexistingRepositoryObjectQueryResponse);

        RallyConfiguration rallyConfig = new RallyConfiguration("", "Workspace", "Scm", "false");
        RallyConnector rallyConnector = new RallyConnector(factoryHelper, rallyConfig, "", "", "");
        ScmConfiguration scmConfig = new ScmConfiguration("http://${revision}", "http://${revision}/${file}");
        BuildConfiguration buildConfig = new BuildConfiguration("SinceLastBuild");
        ScmConnector scmConnector = new JenkinsConnector(scmConfig, buildConfig);
        AdvancedConfiguration advancedConfig = new AdvancedConfiguration("");
        this.rallyService = new RallyService(rallyConnector, scmConnector, advancedConfig, rallyConfig);
    }

    @Given("^a build that is configured to create an SCMRepository object$")
    public void a_build_that_is_configured_to_create_an_SCMRepository_object() throws Throwable {
        rallyApi = mock(RallyRestApi.class);

        RallyConnector.FactoryHelper factoryHelper = mock(RallyConnector.FactoryHelper.class);
        when(factoryHelper.createConnection(anyString(), anyString())).thenReturn(rallyApi);

        preexistingRepositoryObjectQueryResponse = mock(QueryResponse.class);
        when(rallyApi.query(argThat(new IsQueryRequestForRepository()))).thenReturn(preexistingRepositoryObjectQueryResponse);

        CreateResponse createRepositoryResponse = mock(CreateResponse.class);
        when(createRepositoryResponse.wasSuccessful()).thenReturn(true);
        when(createRepositoryResponse.getObject()).thenReturn(createRepositoryResponseObject());
        when(rallyApi.create(argThat(new IsCreateRequestForRepository()))).thenReturn(createRepositoryResponse);

        QueryResponse workspaceQueryResponse = mock(QueryResponse.class);
        when(workspaceQueryResponse.getTotalResultCount()).thenReturn(1);
        when(workspaceQueryResponse.getResults()).thenReturn(createQueryResultsForRef());
        when(rallyApi.query(argThat(new IsQueryRequestForWorkspace()))).thenReturn(workspaceQueryResponse);

        RallyConfiguration rallyConfig = new RallyConfiguration("", "Workspace", "Scm", "true");
        RallyConnector rallyConnector = new RallyConnector(factoryHelper, rallyConfig, "", "", "");
        ScmConfiguration scmConfig = new ScmConfiguration("", "");
        BuildConfiguration buildConfig = new BuildConfiguration("SinceLastBuild");
        ScmConnector scmConnector = new JenkinsConnector(scmConfig, buildConfig);
        AdvancedConfiguration advancedConfig = new AdvancedConfiguration("");
        this.rallyService = new RallyService(rallyConnector, scmConnector, advancedConfig, rallyConfig);
    }

    @Given("^that SCMRepository object does not exist$")
    public void that_SCMRepository_object_does_not_exist() throws Throwable {
        when(preexistingRepositoryObjectQueryResponse.getTotalResultCount()).thenReturn(0);
    }

    @Given("^that SCMRepository object already exists$")
    public void that_SCMRepository_object_already_exists() throws Throwable {
        when(preexistingRepositoryObjectQueryResponse.getTotalResultCount()).thenReturn(1);
        when(preexistingRepositoryObjectQueryResponse.getResults()).thenReturn(createQueryResultsForRef());
    }

    @When("^a job is executed from the build$")
    public void a_job_is_executed_from_the_build() throws Throwable {
        try {
            RallyUpdateData details = new RallyUpdateData();
            details.setFilenamesAndActions(Lists.<RallyUpdateData.FilenameAndAction>newArrayList());
            details.addId("US12345");

            QueryResponse storyQueryResponse = mock(QueryResponse.class);
            when(storyQueryResponse.getTotalResultCount()).thenReturn(1);
            when(storyQueryResponse.getResults()).thenReturn(createQueryResultsForRef());
            when(rallyApi.query(argThat(new IsQueryRequestForStory()))).thenReturn(storyQueryResponse);

            CreateResponse changesetCreateResponse = mock(CreateResponse.class);
            when(changesetCreateResponse.wasSuccessful()).thenReturn(true);
            when(changesetCreateResponse.getObject()).thenReturn(createRepositoryResponseObject());
            when(rallyApi.create(argThat(new IsCreateRequestForChangeset()))).thenReturn(changesetCreateResponse);

            this.rallyService.updateChangeset(details);
        } catch (Exception exception) {
            this.caughtException = exception;
        }
    }

    @Then("^an error should be thrown indicating that the configured repository does not exist$")
    public void an_error_should_be_thrown_indicating_that_the_configured_repository_does_not_exist() throws Throwable {
        assertThat(this.caughtException, is(notNullValue()));
        assertThat(this.caughtException, is(instanceOf(RallyAssetNotFoundException.class)));
    }

    @Then("^a request to create the SCMRepository object should be sent$")
    public void a_request_to_create_the_SCMRepository_object_should_be_sent() throws Throwable {
        verify(rallyApi).create(argThat(new IsCreateRequestForRepository()));
    }

    @Then("^a request to create a Changeset object associated with the SCMRepository object should be sent$")
    public void a_request_to_create_a_Changeset_object_associated_with_the_SCMRepository_object_should_be_sent() throws Throwable {
        verify(rallyApi).create(argThat(new IsCreateRequestForChangeset()));
        assertThat(this.caughtException, is(nullValue()));
    }

    @Then("^a request to create the SCMRepository object should not be sent$")
    public void a_request_to_create_the_SCMRepository_object_should_not_be_sent() throws Throwable {
        verify(rallyApi, never()).create(argThat(new IsCreateRequestForRepository()));
    }

    private JsonObject createRepositoryResponseObject() {
        JsonObject object = new JsonObject();
        object.addProperty("_ref", "_ref");

        return object;
    }

    private JsonArray createQueryResultsForRef() {
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("_ref", "_ref");
        array.add(object);

        return array;
    }

    class IsQueryRequestForRepository extends TypeSafeMatcher<QueryRequest> {

        @Override
        protected boolean matchesSafely(QueryRequest queryRequest) {
            return queryRequest.toUrl().startsWith("/scmrepository.js?");
        }

        public void describeTo(Description description) {

        }
    }

    private class IsCreateRequestForRepository extends TypeSafeMatcher<CreateRequest> {
        @Override
        protected boolean matchesSafely(CreateRequest createRequest) {
            return createRequest.toUrl().startsWith("/scmrepository/create.js?");
        }

        public void describeTo(Description description) {

        }
    }

    private class IsQueryRequestForWorkspace extends TypeSafeMatcher<QueryRequest> {
        @Override
        protected boolean matchesSafely(QueryRequest queryRequest) {
            return queryRequest.toUrl().startsWith("/workspace.js?");
        }

        public void describeTo(Description description) {

        }
    }

    private class IsCreateRequestForChangeset extends TypeSafeMatcher<CreateRequest> {
        @Override
        protected boolean matchesSafely(CreateRequest createRequest) {
            return createRequest.toUrl().startsWith("/changeset/create.js?");
        }

        public void describeTo(Description description) {

        }
    }

    private class IsQueryRequestForStory extends TypeSafeMatcher<QueryRequest> {
        @Override
        protected boolean matchesSafely(QueryRequest getRequest) {
            return getRequest.toUrl().startsWith("/hierarchicalrequirement.js?");
        }

        public void describeTo(Description description) {

        }
    }
}
