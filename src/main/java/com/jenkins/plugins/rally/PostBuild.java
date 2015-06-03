package com.jenkins.plugins.rally;

import hudson.model.Build;

import java.net.URISyntaxException;

/**
 * Shim to assist data migration.
 * 
 * @author Tushar Shinde
 * @author Kohsuke Kawaguchi
 */
public class PostBuild {
    private String userName;
   	private String password;
   	private String workspace;
   	private String project;
   	private String scmuri;
   	private String scmRepoName;
   	private String changesSince;
   	private String startDate;
   	private String endDate;
   	private String debugOn;
   	private String proxy;

    /**
     * Called when this is read from persisted form.
     * Create equivalent {@link RallyPlugin} instance.
     */
    public RallyPlugin readResolve() {
        try {
            return new RallyPlugin(
                "Pleasse set valid API key for "+userName,
                workspace, scmRepoName,
                null, null, null, null,null,null // TODO
            );
        } catch (Exception e) {
            // not sure what the right thing to do here.
            return null;
        }
    }
}
