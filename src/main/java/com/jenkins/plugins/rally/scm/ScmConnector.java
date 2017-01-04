package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import hudson.model.Run;

import java.io.PrintStream;
import java.util.List;

public interface ScmConnector {
    String getRevisionUriFor(String revision);
    String getFileUriFor(String revision, String filename);

    List<RallyUpdateData> getChanges(Run build, PrintStream out) throws RallyException;
}
