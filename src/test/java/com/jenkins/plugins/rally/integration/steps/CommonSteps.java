package com.jenkins.plugins.rally.integration.steps;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.integration.steps.matchers.IsCreateRequestForChangeset;
import com.jenkins.plugins.rally.integration.steps.matchers.IsGetRequestForObject;
import com.jenkins.plugins.rally.integration.steps.matchers.IsQueryRequestForStory;
import com.jenkins.plugins.rally.utils.CommitMessageParser;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import cucumber.api.java.en.When;

import javax.inject.Inject;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CommonSteps {
    private final StepStateContainer stateContainer;

    @Inject
    public CommonSteps(StepStateContainer stateContainer) {
        this.stateContainer = stateContainer;
    }

    @When("^a job is executed from the build for commit message \"(.*)\"$")
    public void a_job_is_executed_from_the_build(String commitMessage) throws Throwable {
        try {
            RallyUpdateData details = CommitMessageParser.parse(commitMessage);
            details.setFilenamesAndActions(Lists.<RallyUpdateData.FilenameAndAction>newArrayList());

            QueryResponse storyQueryResponse = mock(QueryResponse.class);
            when(storyQueryResponse.getTotalResultCount()).thenReturn(1);
            when(storyQueryResponse.getResults()).thenReturn(createQueryResultsForRef());
            when(stateContainer.getRallyApi().query(argThat(new IsQueryRequestForStory()))).thenReturn(storyQueryResponse);

            CreateResponse changesetCreateResponse = mock(CreateResponse.class);
            when(changesetCreateResponse.wasSuccessful()).thenReturn(true);
            when(changesetCreateResponse.getObject()).thenReturn(createRepositoryResponseObject());
            when(stateContainer.getRallyApi().create(argThat(new IsCreateRequestForChangeset()))).thenReturn(changesetCreateResponse);

            GetResponse getResponse = mock(GetResponse.class);
            when(getResponse.getObject()).thenReturn(createGetResponseObject());
            when(stateContainer.getRallyApi().get(argThat(new IsGetRequestForObject()))).thenReturn(getResponse);

            this.stateContainer.getRallyService().updateChangeset(details);
        } catch (Exception exception) {
            this.stateContainer.setCaughtException(exception);
        }
    }

    public static JsonObject createRepositoryResponseObject() {
        JsonObject object = new JsonObject();
        object.addProperty("_ref", "_ref");

        return object;
    }

    public static JsonArray createQueryResultsForRef() {
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("_ref", "_ref");
        array.add(object);

        return array;
    }

    public static JsonObject createGetResponseObject() {
        JsonObject object = new JsonObject();

        JsonObject refObject = new JsonObject();
        refObject.addProperty("_ref", "_ref");

        object.add("Project", refObject);

        return object;
    }
}
