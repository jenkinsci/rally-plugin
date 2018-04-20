package com.jenkins.plugins.rally.connector;

import com.jenkins.plugins.rally.RallyArtifact;
import com.jenkins.plugins.rally.RallyException;

import java.util.List;

public interface AlmConnector {
    List<RallyArtifact> updateChangeset(RallyUpdateData details) throws RallyException;
    void updateRallyTaskDetails(RallyUpdateData details) throws RallyException;
    void closeConnection() throws RallyException;
}
