package com.jenkins.plugins.rally.connector;

import com.jenkins.plugins.rally.RallyException;

public interface AlmConnector {
    void updateChangeset(RallyUpdateData details) throws RallyException;
    void updateRallyTaskDetails(RallyUpdateData details) throws RallyException;
    void closeConnection() throws RallyException;
}
