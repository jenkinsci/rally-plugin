package com.jenkins.plugins.rally.connector;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.utils.RallyCreateBuilder;
import com.jenkins.plugins.rally.utils.RallyQueryBuilder;
import com.jenkins.plugins.rally.utils.RallyUpdateBean;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.UpdateRequest;
import com.rallydev.rest.response.UpdateResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.jenkins.plugins.rally.utils.JsonElementBuilder.anObjectWithProperty;
import static com.jenkins.plugins.rally.utils.JsonElementBuilder.thatReferencesObject;


public class RallyConnector {
    public static class FactoryHelper {

        public RallyRestApi createConnection(String uriAsString, String apiKey) throws URISyntaxException {
            return new RallyRestApi(new URI(uriAsString), apiKey);
        }
    }
    private final RallyRestApi rallyRestApi;

    private final RallyConfiguration rallyConfiguration;
    @Inject
    public RallyConnector(FactoryHelper factoryHelper,
                          RallyConfiguration rallyConfiguration,
                          @Named("SERVER URL") String rallyUrl,
                          @Named("API VERSION") String apiVersion,
                          @Named("APP NAME") String appName) throws RallyException {
        try {
            this.rallyRestApi = factoryHelper.createConnection(rallyUrl, rallyConfiguration.getApiKey());
        } catch (URISyntaxException exception) {
            throw new RallyException(exception);
        }

        this.rallyRestApi.setApplicationName(appName);
        this.rallyRestApi.setApplicationVersion(apiVersion);

        this.rallyConfiguration = rallyConfiguration;
    }

    public void configureProxy(URI uri) throws RallyException {
        if (uri == null || uri.getHost() == null) {
            return;
        }

        String userInfo = uri.getUserInfo();

        if (userInfo == null) {
            this.rallyRestApi.setProxy(uri);
        } else {
            String[] usernamePassword = userInfo.split(":");
            if (usernamePassword.length != 2) {
                throw new RallyException("Unable to parse username/password from proxy URL.");
            }
            this.rallyRestApi.setProxy(uri, usernamePassword[0], usernamePassword[1]);
        }
    }

    public void close() {
        try {
            this.rallyRestApi.close();
        } catch (IOException ignored) {
            // ignored
        }
    }

    public String queryForStory(String formattedId) throws RallyException {
        return this.queryForWorkItem("HierarchicalRequirement", formattedId);
    }

    public String queryForDefect(String formattedId) throws RallyException {
        return this.queryForWorkItem("Defect", formattedId);
    }

    public RallyQueryBuilder.RallyQueryResponseObject queryForTaskByIndex(String storyRef, Integer index) throws RallyException {
        return RallyQueryBuilder
                .createQueryFrom(this.rallyRestApi)
                .ofType("Task")
                .inWorkspace(this.rallyConfiguration.getWorkspaceName())
                .withFetchValues("FormattedID", "Actuals", "State")
                .withQueryFilter("WorkProduct", "=", storyRef)
                .andQueryFilter("TaskIndex", "=", Integer.toString(getRallyIndexFor(index)))
                .andExecuteReturningObject();
    }

    public RallyQueryBuilder.RallyQueryResponseObject queryForTaskById(String storyRef, String formattedId) throws RallyException {
        return RallyQueryBuilder
                .createQueryFrom(this.rallyRestApi)
                .ofType("Task")
                .inWorkspace(this.rallyConfiguration.getWorkspaceName())
                .withFetchValues("FormattedID", "Actuals", "State")
                .withQueryFilter("WorkProduct", "=", storyRef)
                .andQueryFilter("FormattedID", "=", formattedId)
                .andExecuteReturningObject();
    }

    private String queryForWorkItem(String workItemType, String formattedId) throws RallyException {
        return RallyQueryBuilder
                .createQueryFrom(this.rallyRestApi)
                .ofType(workItemType)
                .inWorkspace(this.rallyConfiguration.getWorkspaceName())
                .withFetchValues("FormattedID", "Name", "Changesets")
                .withQueryFilter("FormattedID", "=", formattedId)
                .andExecuteReturningRef();
    }

    public String queryForRepository() throws RallyException {
        return RallyQueryBuilder
                .createQueryFrom(this.rallyRestApi)
                .ofType("SCMRepository")
                .inWorkspace(this.rallyConfiguration.getWorkspaceName())
                .withFetchValues("ObjectID", "Name", "SCMType")
                .withQueryFilter("Name", "=", this.rallyConfiguration.getScmName())
                .andExecuteReturningRef();
    }

    public String createChangeset(String scmRepositoryRef, String revision, String uri, String commitTimestamp, String message, String artifactRef) throws RallyException{
        return RallyCreateBuilder
                .createObjectWith(this.rallyRestApi)
                .withReference("SCMRepository",
                        thatReferencesObject()
                .withPropertyAndValue("_ref", scmRepositoryRef))
                .andProperty("Revision", revision)
                .andProperty("Uri", uri)
                .andProperty("CommitTimestamp", commitTimestamp)
                .andProperty("Message", message)
                .andArrayContaining(anObjectWithProperty("_ref", artifactRef)
                        .withName("Artifacts"))
                .andExecuteReturningRefFor("Changeset");

    }

    public String createChange(String changesetRef, String filename, String action, String uri) throws RallyException {
        return RallyCreateBuilder
                .createObjectWith(this.rallyRestApi)
                .withReference("Changeset",
                        thatReferencesObject()
                                .withPropertyAndValue("_ref", changesetRef))
                .andProperty("PathAndFilename", filename)
                .andProperty("Action", action)
                .andProperty("Uri", uri)
                .andExecuteReturningRefFor("Change");
    }

    public void updateTask(String taskRef, RallyUpdateBean updateInfo) throws RallyException {
        UpdateRequest request = new UpdateRequest(taskRef, updateInfo.getJsonObject());
        try {
            UpdateResponse response = this.rallyRestApi.update(request);

            if (!response.wasSuccessful()) {
                throw new RallyException(response.getErrors());
            }

        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public String createRepository() throws RallyException {
        String workspaceRef = RallyQueryBuilder
                .createQueryFrom(this.rallyRestApi)
                .ofType("Workspace")
                .withFetchValues("Name")
                .withQueryFilter("Name", "=", this.rallyConfiguration.getWorkspaceName())
                .andExecuteReturningRef();

        return RallyCreateBuilder
                .createObjectWith(this.rallyRestApi)
                .andProperty("Name", this.rallyConfiguration.getScmName())
                .andProperty("Workspace", workspaceRef)
                .andProperty("SCMType", "Jenkins-Created")
                .andExecuteReturningRefFor("SCMRepository");
    }

    private int getRallyIndexFor(Integer index) {
        return index - 1;
    }
}