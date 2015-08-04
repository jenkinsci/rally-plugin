package com.jenkins.plugins.rally.service;

import com.google.inject.Inject;
import com.jenkins.plugins.rally.RallyAssetNotFoundException;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.AdvancedConfiguration;
import com.jenkins.plugins.rally.config.RallyConfiguration;
import com.jenkins.plugins.rally.connector.AlmConnector;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.utils.RallyQueryBuilder;
import com.jenkins.plugins.rally.utils.RallyUpdateBean;

public class RallyService implements AlmConnector {
    private ScmConnector scmConnector;
    private RallyConnector rallyConnector;
    private RallyConfiguration rallyConfiguration;

    @Inject
    public RallyService(RallyConnector connector, ScmConnector scmConnector, AdvancedConfiguration configuration, RallyConfiguration rallyConfiguration) throws RallyException {
        this.scmConnector = scmConnector;
        this.rallyConnector = connector;
        this.rallyConnector.configureProxy(configuration.getProxyUri());
        this.rallyConfiguration = rallyConfiguration;
    }

    public void closeConnection() throws RallyException {
        this.rallyConnector.close();
    }

    public void updateChangeset(RallyUpdateData details) throws RallyException {
        String repositoryRef;

        try {
            repositoryRef = this.rallyConnector.queryForRepository();
        } catch (RallyAssetNotFoundException exception) {
            if (this.rallyConfiguration.shouldCreateIfAbsent()) {
                repositoryRef = this.rallyConnector.createRepository();
            } else {
                throw exception;
            }
        }

        for (RallyUpdateData.RallyId id : details.getIds()) {
            String artifactRef = id.isStory()
                    ? this.rallyConnector.queryForStory(id.getName())
                    : this.rallyConnector.queryForDefect(id.getName());
            String revisionUri = this.scmConnector.getRevisionUriFor(details.getRevision());
            String changesetRef = this.rallyConnector.createChangeset(repositoryRef, details.getRevision(), revisionUri, details.getTimeStamp(), details.getMsg(), artifactRef);

            for (RallyUpdateData.FilenameAndAction filenameAndAction : details.getFilenamesAndActions()) {
                String fileName = filenameAndAction.filename;
                String fileType = filenameAndAction.action.getName();
                String revision = details.getRevision();
                String fileUri = this.scmConnector.getFileUriFor(revision, fileName);

                this.rallyConnector.createChange(changesetRef, fileName, fileType, fileUri);
            }
        }
    }

    public void updateRallyTaskDetails(RallyUpdateData details) throws RallyException {
        if (hasNoTasks(details)) {
            return;
        }

        for (RallyUpdateData.RallyId id : details.getIds()) {
            String storyRef = this.rallyConnector.queryForStory(id.getName());
            RallyQueryBuilder.RallyQueryResponseObject taskObject = getTaskObjectByStoryRef(details, storyRef);

            RallyUpdateBean updateInfo = new RallyUpdateBean();

            updateInfo.setState(details.getTaskStatus().isEmpty()
                    ? "In-Progress"
                    : details.getTaskStatus());

            if (!details.getTaskToDO().isEmpty()) {
                updateInfo.setTodo(details.getTaskToDO());
            }

            if (!details.getTaskActuals().isEmpty()) {
                Double actuals = Double.parseDouble(details.getTaskActuals());
                actuals = actuals + taskObject.getTaskAttributeAsDouble("Actuals");
                updateInfo.setActual(Double.toString(actuals));
            }

            if (!details.getTaskEstimates().isEmpty()) {
                updateInfo.setEstimate(details.getTaskEstimates());
            }

            this.rallyConnector.updateTask(taskObject.getRef(), updateInfo);
        }
    }

    private boolean hasNoTasks(RallyUpdateData details) {
        return details.getTaskIndex().isEmpty() && details.getTaskID().isEmpty();
    }

    private RallyQueryBuilder.RallyQueryResponseObject getTaskObjectByStoryRef(RallyUpdateData details, String storyRef) throws RallyException {
        return details.getTaskIndex().isEmpty()
                ? this.rallyConnector.queryForTaskById(storyRef, details.getTaskID())
                : this.rallyConnector.queryForTaskByIndex(storyRef, Integer.parseInt(details.getTaskIndex()));
    }
}
