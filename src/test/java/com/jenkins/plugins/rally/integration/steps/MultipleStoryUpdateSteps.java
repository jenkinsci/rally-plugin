package com.jenkins.plugins.rally.integration.steps;

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
import com.rallydev.rest.response.QueryResponse;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.mockito.ArgumentCaptor;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class MultipleStoryUpdateSteps {
    private final StepStateContainer stateContainer;

    @Inject
    public MultipleStoryUpdateSteps(StepStateContainer stateContainer) {
        this.stateContainer = stateContainer;
    }

    @Given("^an unremarkable build configuration$")
    public void an_unremarkable_build_configuration() throws Throwable {
        this.stateContainer.setRallyApi(mock(RallyRestApi.class));

        RallyConnector.FactoryHelper factoryHelper = mock(RallyConnector.FactoryHelper.class);
        when(factoryHelper.createConnection(anyString(), anyString())).thenReturn(this.stateContainer.getRallyApi());

        this.stateContainer.setPreexistingRepositoryObjectQueryResponse(mock(QueryResponse.class));
        when(this.stateContainer.getRallyApi().query(argThat(new IsQueryRequestForRepository()))).thenReturn(this.stateContainer.getPreexistingRepositoryObjectQueryResponse());

        when(this.stateContainer.getPreexistingRepositoryObjectQueryResponse().getTotalResultCount()).thenReturn(1);
        when(this.stateContainer.getPreexistingRepositoryObjectQueryResponse().getResults()).thenReturn(CommonSteps.createQueryResultsForRef());

        RallyConfiguration rallyConfig = new RallyConfiguration("", "Workspace", "Scm", "false");
        RallyConnector rallyConnector = new RallyConnector(factoryHelper, rallyConfig, "", "", "");
        ScmConfiguration scmConfig = new ScmConfiguration("http://${revision}", "http://${revision}/${file}");
        BuildConfiguration buildConfig = new BuildConfiguration("SinceLastBuild");
        ScmConnector scmConnector = new JenkinsConnector(scmConfig, buildConfig);
        AdvancedConfiguration advancedConfig = new AdvancedConfiguration("", "false");
        this.stateContainer.setRallyService(new RallyService(rallyConnector, scmConnector, advancedConfig, rallyConfig));
    }

    @Then("^both stories should receive associations with the same Changeset$")
    public void both_stories_should_receive_associations_with_the_same_Changeset() throws Throwable {
        ArgumentCaptor<CreateRequest> createRequestCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        verify(this.stateContainer.getRallyApi(), times(2)).create(argThat(new IsCreateRequestForChangeset()));

        verify(this.stateContainer.getRallyApi(), times(2)).create(createRequestCaptor.capture());

        List<CreateRequest> capturedRequests = createRequestCaptor.getAllValues();
        assertThat(capturedRequests.get(0).getBody(), is(equalTo(capturedRequests.get(1).getBody())));
    }
}
