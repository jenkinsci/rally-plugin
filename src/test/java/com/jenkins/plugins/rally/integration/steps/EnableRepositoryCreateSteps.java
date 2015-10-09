package com.jenkins.plugins.rally.integration.steps;

import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.integration.steps.matchers.IsCreateRequestForChangeset;
import com.jenkins.plugins.rally.integration.steps.matchers.IsQueryRequestForRepository;
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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class EnableRepositoryCreateSteps {
    private final StepStateContainer stateContainer;

    @Inject
    public EnableRepositoryCreateSteps(StepStateContainer stateContainer) {
        this.stateContainer = stateContainer;
    }

    @Given("^a build that is configured not to create an SCMRepository object$")
    public void a_build_that_is_configured_not_to_create_an_SCMRepository_object() throws Throwable {
        this.stateContainer.setRallyApi(mock(RallyRestApi.class));

        RallyConnector.FactoryHelper factoryHelper = mock(RallyConnector.FactoryHelper.class);
        when(factoryHelper.createConnection(anyString(), anyString())).thenReturn(this.stateContainer.getRallyApi());

        this.stateContainer.setPreexistingRepositoryObjectQueryResponse(mock(QueryResponse.class));
        when(this.stateContainer.getRallyApi().query(argThat(new IsQueryRequestForRepository()))).thenReturn(this.stateContainer.getPreexistingRepositoryObjectQueryResponse());

        RallyConfiguration rallyConfig = new RallyConfiguration("", "Workspace", "Scm", "false");
        RallyConnector rallyConnector = new RallyConnector(factoryHelper, rallyConfig, "", "", "");
        ScmConfiguration scmConfig = new ScmConfiguration("http://${revision}", "http://${revision}/${file}");
        BuildConfiguration buildConfig = new BuildConfiguration("SinceLastBuild");
        ScmConnector scmConnector = new JenkinsConnector(scmConfig, buildConfig);
        AdvancedConfiguration advancedConfig = new AdvancedConfiguration("", "false");
        this.stateContainer.setRallyService(new RallyService(rallyConnector, scmConnector, advancedConfig, rallyConfig));
    }

    @Given("^a build that is configured to create an SCMRepository object$")
    public void a_build_that_is_configured_to_create_an_SCMRepository_object() throws Throwable {
        this.stateContainer.setRallyApi(mock(RallyRestApi.class));

        RallyConnector.FactoryHelper factoryHelper = mock(RallyConnector.FactoryHelper.class);
        when(factoryHelper.createConnection(anyString(), anyString())).thenReturn(this.stateContainer.getRallyApi());

        this.stateContainer.setPreexistingRepositoryObjectQueryResponse(mock(QueryResponse.class));
        when(this.stateContainer.getRallyApi().query(argThat(new IsQueryRequestForRepository()))).thenReturn(this.stateContainer.getPreexistingRepositoryObjectQueryResponse());

        CreateResponse createRepositoryResponse = mock(CreateResponse.class);
        when(createRepositoryResponse.wasSuccessful()).thenReturn(true);
        when(createRepositoryResponse.getObject()).thenReturn(CommonSteps.createRepositoryResponseObject());
        when(this.stateContainer.getRallyApi().create(argThat(new IsCreateRequestForRepository()))).thenReturn(createRepositoryResponse);

        QueryResponse workspaceQueryResponse = mock(QueryResponse.class);
        when(workspaceQueryResponse.getTotalResultCount()).thenReturn(1);
        when(workspaceQueryResponse.getResults()).thenReturn(CommonSteps.createQueryResultsForRef());
        when(this.stateContainer.getRallyApi().query(argThat(new IsQueryRequestForWorkspace()))).thenReturn(workspaceQueryResponse);

        RallyConfiguration rallyConfig = new RallyConfiguration("", "Workspace", "Scm", "true");
        RallyConnector rallyConnector = new RallyConnector(factoryHelper, rallyConfig, "", "", "");
        ScmConfiguration scmConfig = new ScmConfiguration("", "");
        BuildConfiguration buildConfig = new BuildConfiguration("SinceLastBuild");
        ScmConnector scmConnector = new JenkinsConnector(scmConfig, buildConfig);
        AdvancedConfiguration advancedConfig = new AdvancedConfiguration("", "false");
        this.stateContainer.setRallyService(new RallyService(rallyConnector, scmConnector, advancedConfig, rallyConfig));
    }

    @Given("^that SCMRepository object does not exist$")
    public void that_SCMRepository_object_does_not_exist() throws Throwable {
        when(this.stateContainer.getPreexistingRepositoryObjectQueryResponse().getTotalResultCount()).thenReturn(0);
    }

    @Given("^that SCMRepository object already exists$")
    public void that_SCMRepository_object_already_exists() throws Throwable {
        when(this.stateContainer.getPreexistingRepositoryObjectQueryResponse().getTotalResultCount()).thenReturn(1);
        when(this.stateContainer.getPreexistingRepositoryObjectQueryResponse().getResults()).thenReturn(CommonSteps.createQueryResultsForRef());
    }

    @Then("^an error should be thrown indicating that the configured repository does not exist$")
    public void an_error_should_be_thrown_indicating_that_the_configured_repository_does_not_exist() throws Throwable {
        assertThat(this.stateContainer.getCaughtException(), is(notNullValue()));
        assertThat(this.stateContainer.getCaughtException(), is(instanceOf(RallyAssetNotFoundException.class)));
    }

    @Then("^a request to create the SCMRepository object should be sent$")
    public void a_request_to_create_the_SCMRepository_object_should_be_sent() throws Throwable {
        verify(this.stateContainer.getRallyApi()).create(argThat(new IsCreateRequestForRepository()));
    }

    @Then("^a request to create a Changeset object associated with the SCMRepository object should be sent$")
    public void a_request_to_create_a_Changeset_object_associated_with_the_SCMRepository_object_should_be_sent() throws Throwable {
        verify(this.stateContainer.getRallyApi()).create(argThat(new IsCreateRequestForChangeset()));
        assertThat(this.stateContainer.getCaughtException(), is(nullValue()));
    }

    @Then("^a request to create the SCMRepository object should not be sent$")
    public void a_request_to_create_the_SCMRepository_object_should_not_be_sent() throws Throwable {
        verify(this.stateContainer.getRallyApi(), never()).create(argThat(new IsCreateRequestForRepository()));
    }

    class IsCreateRequestForRepository extends TypeSafeMatcher<CreateRequest> {
        @Override
        protected boolean matchesSafely(CreateRequest createRequest) {
            return createRequest.toUrl().startsWith("/scmrepository/create.js?");
        }

        public void describeTo(Description description) {

        }
    }

    class IsQueryRequestForWorkspace extends TypeSafeMatcher<QueryRequest> {
        @Override
        protected boolean matchesSafely(QueryRequest queryRequest) {
            return queryRequest.toUrl().startsWith("/workspace.js?");
        }

        public void describeTo(Description description) {

        }
    }
}
