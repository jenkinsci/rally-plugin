package com.jenkins.plugins.rally;

import hudson.model.Build;

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

    public RallyPlugin readResolve() {
        throw new UnsupportedOperationException();
    }
}
