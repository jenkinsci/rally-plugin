package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import hudson.model.AbstractBuild;
import hudson.model.Run;

import java.io.PrintStream;
import java.util.List;

public interface ScmConnector {
    String getRevisionUriFor(String repository, String revision);
    String getFileUriFor(String repository, String revision, String filename);

    List<RallyUpdateData> getChanges(Run<?, ?> run, PrintStream out) throws RallyException;
}
