package com.jenkins.plugins.rally.service;

import com.google.common.collect.Lists;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RallyService implements AlmConnector {
    private ScmConnector scmConnector;
    private RallyConnector rallyConnector;
    private RallyConfiguration rallyConfiguration;
    private Boolean shouldCaptureBuildStatus;

    @Inject
    public RallyService(RallyConnector connector, ScmConnector scmConnector, AdvancedConfiguration configuration, RallyConfiguration rallyConfiguration) throws RallyException {
        this.scmConnector = scmConnector;
        this.rallyConnector = connector;
        this.rallyConnector.configureProxy(configuration.getProxyUri());
        this.shouldCaptureBuildStatus = configuration.shouldCaptureBuildStatus();
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

        Map<String, List<String>> changesetRefsWithProjectRefs = new HashMap<String, List<String>>();
        for (RallyUpdateData.RallyId id : details.getIds()) {
            String artifactRef;

            try {
                artifactRef = id.isStory()
                        ? this.rallyConnector.queryForStory(id.getName())
                        : this.rallyConnector.queryForDefect(id.getName());
            } catch (RallyAssetNotFoundException exception) {
                continue;
            }

            String revisionUri = this.scmConnector.getRevisionUriFor(details.getRevision());
            String changesetRef = this.rallyConnector.createChangeset(repositoryRef, details.getRevision(), revisionUri, details.getTimeStamp(), details.getMsg(), artifactRef);
            String projectRef = rallyConnector.getObjectAndReturnInternalRef(artifactRef, "Project");

            if (!changesetRefsWithProjectRefs.containsKey(projectRef)) {
                changesetRefsWithProjectRefs.put(projectRef, Lists.<String>newArrayList());
            }
            changesetRefsWithProjectRefs.get(projectRef).add(changesetRef);

            for (RallyUpdateData.FilenameAndAction filenameAndAction : details.getFilenamesAndActions()) {
                String fileName = filenameAndAction.filename;
                String fileType = filenameAndAction.action.getName();
                String revision = details.getRevision();
                String fileUri = this.scmConnector.getFileUriFor(revision, fileName);

                this.rallyConnector.createChange(changesetRef, fileName, fileType, fileUri);
            }
        }

        if (shouldCaptureBuildStatus) {
            for (Map.Entry<String, List<String>> entry : changesetRefsWithProjectRefs.entrySet()) {
                String projectRef = entry.getKey();
                List<String> changesetRefs = entry.getValue();

                String buildDefinitionRef;
                try {
                    buildDefinitionRef = rallyConnector.queryForBuildDefinition(details.getBuildName(), projectRef);
                } catch (RallyAssetNotFoundException exception) {
                    buildDefinitionRef = rallyConnector.createBuildDefinition(details.getBuildName(), projectRef);
                }

                rallyConnector.createBuild(
                        buildDefinitionRef,
                        changesetRefs,
                        details.getCurrentBuildNumber(),
                        details.getBuildDuration(),
                        details.getTimeStamp(),
                        details.getBuildStatus(),
                        details.getBuildMessage(),
                        details.getBuildUrl());
            }
        }
    }

    public void updateRallyTaskDetails(RallyUpdateData details) throws RallyException {
        if (hasNoTasks(details)) {
            return;
        }

        for (RallyUpdateData.RallyId id : details.getIds()) {
        	boolean anyChange = false;
        	
            String storyRef = this.rallyConnector.queryForStory(id.getName());
            RallyQueryBuilder.RallyQueryResponseObject taskObject = getTaskObjectByStoryRef(details, storyRef);

            RallyUpdateBean updateInfo = new RallyUpdateBean();

            if (!details.getTaskStatus().isEmpty()) {
            	updateInfo.setState(details.getTaskStatus());
            	anyChange = true;
            }

            if (!details.getTaskToDO().isEmpty()) {
                updateInfo.setTodo(details.getTaskToDO());
                anyChange = true;
            }

            if (!details.getTaskActuals().isEmpty()) {
                Double actuals = Double.parseDouble(details.getTaskActuals());
                actuals = actuals + taskObject.getTaskAttributeAsDouble("Actuals");
                updateInfo.setActual(Double.toString(actuals));
                anyChange = true;
            }

            if (!details.getTaskEstimates().isEmpty()) {
                updateInfo.setEstimate(details.getTaskEstimates());
                anyChange = true;
            }

            if (anyChange) {
            	this.rallyConnector.updateTask(taskObject.getRef(), updateInfo);
            }
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
