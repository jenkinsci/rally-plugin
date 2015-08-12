package com.jenkins.plugins.rally.service;

import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.utils.RallyQueryBuilder;
import com.jenkins.plugins.rally.utils.RallyUpdateBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RallyServiceTest {

    @Mock
    private ScmConnector scmConnector;

    @Mock
    private RallyConnector connector;

    @Mock
    private RallyConfiguration rallyConfiguration;

    private RallyService service;

    @Before
    public void setUp() throws Exception {
        this.service = new RallyService(this.connector, scmConnector, new AdvancedConfiguration("http://proxy.url"), this.rallyConfiguration);
    }

    @Test(expected=RallyAssetNotFoundException.class)
    public void shouldThrowErrorIfAttemptToUpdateWithoutValidStoryRef() throws Exception {
        when(this.connector.queryForStory("US12345")).thenThrow(new RallyAssetNotFoundException());

        RallyUpdateData details = new RallyUpdateData();
        details.setTaskID("TA54321");
        details.addId("US12345");

        this.service.updateRallyTaskDetails(details);
    }

    @Test
    public void shouldBeConfigurableToCreateNonExistentRepository() throws RallyException {
        when(this.connector.queryForRepository()).thenThrow(new RallyAssetNotFoundException());
        when(this.rallyConfiguration.shouldCreateIfAbsent()).thenReturn(true);

        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setFilenamesAndActions(new ArrayList<RallyUpdateData.FilenameAndAction>());
        this.service.updateChangeset(details);

        verify(this.connector).createRepository();
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldBeConfigurableToNotCreateNonExistentRepository() throws RallyException {
        when(this.connector.queryForRepository()).thenThrow(new RallyAssetNotFoundException());
        when(this.rallyConfiguration.shouldCreateIfAbsent()).thenReturn(false);

        RallyUpdateData details = new RallyUpdateData();
        details.setFilenamesAndActions(new ArrayList<RallyUpdateData.FilenameAndAction>());
        this.service.updateChangeset(details);

        verify(this.connector, never()).createRepository();
    }

    @Test
    public void shouldDoNothingIfNoTasksDefined() throws RallyException {
        RallyUpdateData details = new RallyUpdateData();

        this.service.updateRallyTaskDetails(details);

        verify(this.connector, never()).updateTask(anyString(), any(RallyUpdateBean.class));
    }

    @Test
    public void shouldSetTaskStateToInProgressIfNoStateIsGiven() throws RallyException {
        ArgumentCaptor<RallyUpdateBean> beanCaptor = ArgumentCaptor.forClass(RallyUpdateBean.class);
        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setTaskID("TA54321");

        JsonObject taskJsonObject = new JsonObject();
        taskJsonObject.addProperty("_ref", "_ref");
        when(this.connector.queryForStory(anyString())).thenReturn("_ref");
        when(this.connector.queryForTaskById(anyString(), anyString())).thenReturn(new RallyQueryBuilder.RallyQueryResponseObject(taskJsonObject));

        this.service.updateRallyTaskDetails(details);

        verify(this.connector).updateTask(anyString(), beanCaptor.capture());

        assertThat(beanCaptor.getValue().getJsonObject().get("State").getAsString(), containsString("In-Progress"));
    }

    @Test
    public void shouldSetTaskStateToGivenState() throws RallyException {
        ArgumentCaptor<RallyUpdateBean> beanCaptor = ArgumentCaptor.forClass(RallyUpdateBean.class);
        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setTaskID("TA54321");
        details.setTaskStatus("My Status");

        JsonObject taskJsonObject = new JsonObject();
        taskJsonObject.addProperty("_ref", "_ref");
        when(this.connector.queryForStory(anyString())).thenReturn("_ref");
        when(this.connector.queryForTaskById(anyString(), anyString())).thenReturn(new RallyQueryBuilder.RallyQueryResponseObject(taskJsonObject));

        this.service.updateRallyTaskDetails(details);

        verify(this.connector).updateTask(anyString(), beanCaptor.capture());

        assertThat(beanCaptor.getValue().getJsonObject().get("State").getAsString(), containsString("My Status"));
    }

    @Test
    public void shouldUpdateTaskTodoValue() throws RallyException {
        ArgumentCaptor<RallyUpdateBean> beanCaptor = ArgumentCaptor.forClass(RallyUpdateBean.class);
        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setTaskID("TA54321");
        details.setTaskToDO("5");

        JsonObject taskJsonObject = new JsonObject();
        taskJsonObject.addProperty("_ref", "_ref");
        when(this.connector.queryForStory(anyString())).thenReturn("_ref");
        when(this.connector.queryForTaskById(anyString(), anyString())).thenReturn(new RallyQueryBuilder.RallyQueryResponseObject(taskJsonObject));

        this.service.updateRallyTaskDetails(details);

        verify(this.connector).updateTask(anyString(), beanCaptor.capture());

        assertThat(beanCaptor.getValue().getJsonObject().get("ToDo").getAsString(), containsString("5"));
    }

    @Test
    public void shouldCumulitivelyUpdateTaskActualHours() throws RallyException {
        ArgumentCaptor<RallyUpdateBean> beanCaptor = ArgumentCaptor.forClass(RallyUpdateBean.class);
        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setTaskID("TA54321");
        details.setTaskActuals("5");

        JsonObject taskJsonObject = new JsonObject();
        taskJsonObject.addProperty("_ref", "_ref");
        taskJsonObject.addProperty("Actuals", "3");
        when(this.connector.queryForStory(anyString())).thenReturn("_ref");

        when(this.connector.queryForTaskById(anyString(), anyString())).thenReturn(new RallyQueryBuilder.RallyQueryResponseObject(taskJsonObject));

        this.service.updateRallyTaskDetails(details);

        verify(this.connector).updateTask(anyString(), beanCaptor.capture());

        assertThat(beanCaptor.getValue().getJsonObject().get("Actuals").getAsString(), containsString("8"));
    }

    @Test
    public void shoudlUpdateTaskEstimate() throws RallyException {
        ArgumentCaptor<RallyUpdateBean> beanCaptor = ArgumentCaptor.forClass(RallyUpdateBean.class);
        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setTaskID("TA54321");
        details.setTaskEstimates("3");

        JsonObject taskJsonObject = new JsonObject();
        taskJsonObject.addProperty("_ref", "_ref");
        when(this.connector.queryForStory(anyString())).thenReturn("_ref");
        when(this.connector.queryForTaskById(anyString(), anyString())).thenReturn(new RallyQueryBuilder.RallyQueryResponseObject(taskJsonObject));

        this.service.updateRallyTaskDetails(details);

        verify(this.connector).updateTask(anyString(), beanCaptor.capture());

        assertThat(beanCaptor.getValue().getJsonObject().get("Estimate").getAsString(), containsString("3"));
    }

    @Test
    public void shouldFetchTaskByIndex() throws RallyException {
        RallyUpdateData details = new RallyUpdateData();
        details.addId("US12345");
        details.setTaskIndex("1");

        JsonObject taskJsonObject = new JsonObject();
        taskJsonObject.addProperty("_ref", "_ref");
        when(this.connector.queryForStory(anyString())).thenReturn("_ref");
        when(this.connector.queryForTaskByIndex(anyString(), eq(1))).thenReturn(new RallyQueryBuilder.RallyQueryResponseObject(taskJsonObject));

        this.service.updateRallyTaskDetails(details);

        verify(this.connector).updateTask(anyString(), any(RallyUpdateBean.class));
    }
}
