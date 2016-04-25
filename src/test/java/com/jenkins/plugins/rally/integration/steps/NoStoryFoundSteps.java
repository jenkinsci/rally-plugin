package com.jenkins.plugins.rally.integration.steps;

import com.google.common.collect.Lists;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.integration.steps.matchers.IsCreateRequestForChangeset;
import com.jenkins.plugins.rally.integration.steps.matchers.IsQueryRequestForStory;
import com.jenkins.plugins.rally.utils.CommitMessageParser;
import com.rallydev.rest.response.QueryResponse;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class NoStoryFoundSteps {

    private StepStateContainer stateContainer;

    @Inject
    public NoStoryFoundSteps(StepStateContainer stateContainer) {
        this.stateContainer = stateContainer;
    }

    @When("^a job is executed from the build for commit message \"([^\"]*)\" but no Story exists$")
    public void a_job_is_executed_from_the_build_for_commit_message_but_no_Story_exists(String commitMessage) throws Throwable {
        try {
            RallyUpdateData details = CommitMessageParser.parse(commitMessage);
            details.setFilenamesAndActions(Lists.<RallyUpdateData.FilenameAndAction>newArrayList());

            QueryResponse emptyQueryResponse = mock(QueryResponse.class);
            when(emptyQueryResponse.getTotalResultCount()).thenReturn(0);

            when(stateContainer.getRallyApi().query(argThat(new IsQueryRequestForStory())))
                    .thenReturn(emptyQueryResponse);

            this.stateContainer.getRallyService().updateChangeset(details);
        } catch (Exception exception) {
            this.stateContainer.setCaughtException(exception);
        }
    }

    @Then("^no Changeset object should be created$")
    public void no_Changeset_object_should_be_created() throws Throwable {
        verify(this.stateContainer.getRallyApi(), never()).create(argThat(new IsCreateRequestForChangeset()));
    }

    @Then("^the build should not fail$")
    public void the_build_should_not_fail() throws Throwable {
        assertThat(this.stateContainer.getCaughtException(), is(equalTo(null)));
    }
}
