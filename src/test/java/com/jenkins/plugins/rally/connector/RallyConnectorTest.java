package com.jenkins.plugins.rally.connector;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.utils.RallyQueryBuilder;
import com.jenkins.plugins.rally.utils.RallyUpdateBean;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.CreateResponse;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.response.UpdateResponse;
import com.rallydev.rest.util.QueryFilter;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;

import static com.google.common.collect.Lists.newArrayList;
import static com.jenkins.plugins.rally.utils.JsonMatcher.hasJsonPathValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RallyConnectorTest {
    private static final String WORKSPACE_NAME = "WORKSPACE_NAME";
    private static final String SCM_NAME = "SCM_NAME";

    @Mock
    private RallyRestApi rallyRestApi;

    @Mock
    private RallyConnector.FactoryHelper factoryHelper;

    private RallyConnector connector;

    @Before
    public void setUp() throws Exception {
        when(this.factoryHelper.createConnection(anyString(), anyString())).thenReturn(this.rallyRestApi);
        RallyConfiguration rallyConfiguration = new RallyConfiguration("API_KEY", WORKSPACE_NAME, SCM_NAME, "false");
        this.connector = new RallyConnector(factoryHelper, rallyConfiguration, "http://rally", "API VERSION", "APP NAME");
    }

    @Test
    public void shouldConfigureVanillaProxyUri() throws Exception {
        URI sampleUri = new URI("http://proxy.net:1234");
        this.connector.configureProxy(sampleUri);

        verify(this.rallyRestApi).setProxy(sampleUri);
        verify(this.rallyRestApi).setApplicationVersion("API VERSION");
        verify(this.rallyRestApi).setApplicationName("APP NAME");
    }

    @Test
    public void shouldConfigureProxyWithUsernameAndPassword() throws Exception {
        URI sampleUri = new URI("http://username:password@proxy.net:1234");
        this.connector.configureProxy(sampleUri);

        verify(this.rallyRestApi).setProxy(sampleUri, "username", "password");
    }

    @Test(expected = RallyException.class)
    public void shouldThrowExceptionWhenSendingConfusingUsernamePasswordData() throws Exception {
        URI sampleUri = new URI("http://sample:username:password@proxy.net:1234");
        this.connector.configureProxy(sampleUri);
    }

    @Test
    public void shouldHandleNullUriGracefully() throws Exception {
        this.connector.configureProxy(new URI(""));
    }

    @Test
    public void shouldCloseRallyApiConnection() throws Exception {
        this.connector.close();

        verify(this.rallyRestApi).close();
    }

    @Test
    public void shouldGetStoryObjectAndReturnRef() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForStory("US12345");

        String expectedFilterString = new QueryFilter("FormattedID", "=", "US12345").toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test
    public void shouldGetDefectObjectAndReturnRef() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForDefect("DE12345");

        String expectedFilterString = new QueryFilter("FormattedID", "=", "DE12345").toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfStoryIsNotFound() throws Exception {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(response);

        this.connector.queryForStory("US12345");
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfDefectIsNotFound() throws Exception {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(response);

        this.connector.queryForDefect("DE12345");
    }

    @Test
    public void shouldGetScmRepositoryObjectAndReturnRef() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForRepository();

        String expectedFilterString = new QueryFilter("Name", "=", SCM_NAME).toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfRepositoryIsNotFound() throws Exception {
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(response);

        this.connector.queryForRepository();
    }

    @Test
    public void shouldCreateChangesetObject() throws Exception {
        ArgumentCaptor<CreateRequest> createCaptor = ArgumentCaptor.forClass(CreateRequest.class);
        CreateResponse response = mock(CreateResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithRef("_ref"));
        when(response.wasSuccessful()).thenReturn(true);
        when(this.rallyRestApi.create(createCaptor.capture())).thenReturn(response);

        String ref = this.connector.createChangeset("_ref", "revision", "uri", "commitTimestamp", "message", "artifactRef");

        assertThat(ref, is(equalTo("_ref")));

        String json = createCaptor.getValue().getBody();
        assertThat(json, hasJsonPathValue("$.Changeset.SCMRepository._ref", "_ref"));
        assertThat(json, hasJsonPathValue("$.Changeset.Revision", "revision"));
        assertThat(json, hasJsonPathValue("$.Changeset.Uri", "uri"));
        assertThat(json, hasJsonPathValue("$.Changeset.CommitTimestamp", "commitTimestamp"));
        assertThat(json, hasJsonPathValue("$.Changeset.Message", "message"));
        assertThat(json, hasJsonPathValue("$.Changeset.Artifacts[0]._ref", "artifactRef"));
    }

    @Test(expected = RallyException.class)
    public void shouldThrowExceptionIfCreateChangesetOperationNotSuccessful() throws Exception {
        CreateResponse response = mock(CreateResponse.class);
        when(response.wasSuccessful()).thenReturn(false);
        when(this.rallyRestApi.create(any(CreateRequest.class))).thenReturn(response);

        this.connector.createChangeset(null, null, null, null, null, null);
    }

    @Test
    public void shouldCreateChangeObject() throws Exception {
        ArgumentCaptor<CreateRequest> createCaptor = ArgumentCaptor.forClass(CreateRequest.class);
        CreateResponse response = mock(CreateResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithRef("_ref"));
        when(response.wasSuccessful()).thenReturn(true);
        when(this.rallyRestApi.create(createCaptor.capture())).thenReturn(response);

        String ref = this.connector.createChange("_ref", "file.txt", "create", "http://scm.org/file.txt");

        assertThat(ref, is(equalTo("_ref")));

        String json = createCaptor.getValue().getBody();
        assertThat(json, hasJsonPathValue("$.Change.Changeset._ref", "_ref"));
        assertThat(json, hasJsonPathValue("$.Change.PathAndFilename", "file.txt"));
        assertThat(json, hasJsonPathValue("$.Change.Action", "create"));
        assertThat(json, hasJsonPathValue("$.Change.Uri", "http://scm.org/file.txt"));
    }

    @Test(expected = RallyException.class)
    public void shouldThrowExceptionIfCreateChangeOperationNotSuccessful() throws Exception {
        CreateResponse response = mock(CreateResponse.class);
        when(response.wasSuccessful()).thenReturn(false);
        when(this.rallyRestApi.create(any(CreateRequest.class))).thenReturn(response);

        this.connector.createChange(null, null, null, null);
    }

    @Test
    public void shouldQueryForTaskById() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        RallyQueryBuilder.RallyQueryResponseObject responseObject = this.connector.queryForTaskById("storyRef", "TA12345");

        String expectedFilterString = new QueryFilter("WorkProduct", "=", "storyRef").and(new QueryFilter("FormattedID", "=", "TA12345")).toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(responseObject.getRef(), is(equalTo("_ref")));
    }

    @Test
    public void shouldQueryForTaskByIndex() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        RallyQueryBuilder.RallyQueryResponseObject responseObject = this.connector.queryForTaskByIndex("storyRef", 2);

        String expectedFilterString = new QueryFilter("WorkProduct", "=", "storyRef").and(new QueryFilter("TaskIndex", "=", "1")).toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(requestCaptor.getValue().getWorkspace(), is(equalTo(WORKSPACE_NAME)));
        assertThat(responseObject.getRef(), is(equalTo("_ref")));
    }

    @Test
    public void shouldQueryForTaskByIdWithPropertiesAccess() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        JsonArray arrayWithRef = createJsonArrayWithRef("_ref");
        arrayWithRef.get(0).getAsJsonObject().addProperty("Attribute", "2.5");
        when(response.getResults()).thenReturn(arrayWithRef);
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        RallyQueryBuilder.RallyQueryResponseObject responseObject = this.connector.queryForTaskById("storyRef", "TA12345");

        assertThat(responseObject.getTaskAttributeAsDouble("Attribute"), is(equalTo(2.5)));
    }

    @Test
    public void shouldUpdateTask() throws Exception {
        ArgumentCaptor<UpdateRequest> requestCaptor = ArgumentCaptor.forClass(UpdateRequest.class);
        UpdateResponse response = mock(UpdateResponse.class);
        when(response.wasSuccessful()).thenReturn(true);
        when(this.rallyRestApi.update(requestCaptor.capture())).thenReturn(response);

        RallyUpdateBean updateInfo = new RallyUpdateBean();
        updateInfo.setActual("1");
        updateInfo.setEstimate("1");
        updateInfo.setTodo("2");
        this.connector.updateTask("https://rally1.rallydev.com/slm/webservice/v2.0/task/123456", updateInfo);

        String json = requestCaptor.getValue().getBody();
        assertThat(json, hasJsonPathValue("$.task.State", "In-Progress"));
        assertThat(json, hasJsonPathValue("$.task.ToDo", "2"));
        assertThat(json, hasJsonPathValue("$.task.Actuals", "1"));
        assertThat(json, hasJsonPathValue("$.task.Estimate", "1"));
    }

    @Test
    public void shouldCreateRepositoryObject() throws Exception {
        ArgumentCaptor<CreateRequest> createCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        CreateResponse response = mock(CreateResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithRef("_ref"));
        when(response.wasSuccessful()).thenReturn(true);

        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getTotalResultCount()).thenReturn(1);
        JsonArray arrayWithRef = createJsonArrayWithRef("_ref");
        when(queryResponse.getResults()).thenReturn(arrayWithRef);

        when(this.rallyRestApi.create(createCaptor.capture())).thenReturn(response);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(queryResponse);

        String ref = this.connector.createRepository();

        assertThat(ref, is(equalTo("_ref")));

        String json = createCaptor.getValue().getBody();
        assertThat(json, hasJsonPathValue("$.SCMRepository.Name", "SCM_NAME"));
        assertThat(json, hasJsonPathValue("$.SCMRepository.Workspace", "_ref"));
        assertThat(json, hasJsonPathValue("$.SCMRepository.SCMType", "Jenkins-Created"));
    }

    @Test(expected = RallyAssetNotFoundException.class)
    public void shouldThrowExceptionIfWorkspaceNotFound() throws Exception {
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getTotalResultCount()).thenReturn(0);
        when(this.rallyRestApi.query(any(QueryRequest.class))).thenReturn(queryResponse);

        this.connector.createRepository();
    }

    @Test
    public void shouldQueryForBuildDefinitionByNameAndProject() throws Exception {
        ArgumentCaptor<QueryRequest> requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
        QueryResponse response = mock(QueryResponse.class);
        when(response.getTotalResultCount()).thenReturn(1);
        when(response.getResults()).thenReturn(createJsonArrayWithRef("_ref"));
        when(this.rallyRestApi.query(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.queryForBuildDefinition("buildDefinition", "_projectRef");

        String expectedFilterString = new QueryFilter("Name", "=", "buildDefinition").and(new QueryFilter("Project", "=", "_projectRef")).toString();
        assertThat(requestCaptor.getValue().getQueryFilter().toString(), is(equalTo(expectedFilterString)));
        assertThat(ref, is(equalTo("_ref")));
    }

    @Test
    public void shouldCreateBuildDefinitionWithName() throws Exception {
        ArgumentCaptor<CreateRequest> createCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        CreateResponse response = mock(CreateResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithRef("_ref"));
        when(response.wasSuccessful()).thenReturn(true);
        when(this.rallyRestApi.create(createCaptor.capture())).thenReturn(response);

        String ref = this.connector.createBuildDefinition("buildDefinition", "_projectRef");

        assertThat(ref, is(equalTo("_ref")));

        String json = createCaptor.getValue().getBody();
        assertThat(json, hasJsonPathValue("$.BuildDefinition.Name", "buildDefinition"));
        assertThat(json, hasJsonPathValue("$.BuildDefinition.Project", "_projectRef"));
    }

    @Test
    public void shouldCreateBuild() throws Exception {
        ArgumentCaptor<CreateRequest> createCaptor = ArgumentCaptor.forClass(CreateRequest.class);

        CreateResponse response = mock(CreateResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithRef("_ref"));
        when(response.wasSuccessful()).thenReturn(true);
        when(this.rallyRestApi.create(createCaptor.capture())).thenReturn(response);

        String ref = this.connector.createBuild(
                "_buildDefinitionRef",
                newArrayList("_changesetRef1", "_changesetRef2"),
                "123",
                0.123,
                "2015-01-01T00:00:00.000Z",
                "SUCCESS",
                "message",
                "http://build/123");

        assertThat(ref, is(equalTo("_ref")));

        String json = createCaptor.getValue().getBody();
        assertThat(json, hasJsonPathValue("$.Build.BuildDefinition", "_buildDefinitionRef"));
        assertThat(json, hasJsonPathValue("$.Build.Changesets[0]", "_changesetRef1"));
        assertThat(json, hasJsonPathValue("$.Build.Changesets[1]", "_changesetRef2"));
        assertThat(json, hasJsonPathValue("$.Build.Number", "123"));
        assertThat(json, hasJsonPathValue("$.Build.Duration", 0.123));
        assertThat(json, hasJsonPathValue("$.Build.Start", "2015-01-01T00:00:00.000Z"));
        assertThat(json, hasJsonPathValue("$.Build.Status", "SUCCESS"));
        assertThat(json, hasJsonPathValue("$.Build.Message", "message"));
        assertThat(json, hasJsonPathValue("$.Build.Uri", "http://build/123"));
    }

    @Test
    public void shouldGetStoryObjectAndReturnProjectRef() throws Exception {
        ArgumentCaptor<GetRequest> requestCaptor = ArgumentCaptor.forClass(GetRequest.class);
        GetResponse response = mock(GetResponse.class);
        when(response.getObject()).thenReturn(createJsonObjectWithNestedObject("Project", "_ref"));
        when(this.rallyRestApi.get(requestCaptor.capture())).thenReturn(response);

        String ref = this.connector.getObjectAndReturnInternalRef("_ref", "Project");

        assertThat(ref, is(equalTo("_ref")));
    }

    private JsonObject createJsonObjectWithRef(String ref) {
        JsonObject object = new JsonObject();
        object.addProperty("_ref", ref);
        return object;
    }

    private JsonObject createJsonObjectWithNestedObject(String propertyName, String internalRef) {
        JsonObject object = new JsonObject();
        object.add(propertyName, createJsonObjectWithRef(internalRef));
        return object;
    }

    private JsonArray createJsonArrayWithRef(String ref) {
        JsonArray array = new JsonArray();
        JsonObject object = new JsonObject();
        object.addProperty("_ref", ref);
        array.add(object);
        return array;
    }
}
