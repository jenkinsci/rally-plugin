package com.jenkins.plugins.rally;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.net.URISyntaxException;

/**
 * Shim to assist data migration.
 * 
 * @author Tushar Shinde
 * @author Kohsuke Kawaguchi
 * @deprecated
 *      The logic has been refactored to {@link RallyPlugin}
 */
@Deprecated
public class PostBuild {
    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String userName;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String password;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String workspace;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String project;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String scmuri;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String scmRepoName;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String changesSince;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String startDate;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String endDate;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String debugOn;

    @SuppressWarnings(
            value="UUF_UNUSED_FIELD",
            justification="Field must remain for legacy purposes")
    private String proxy;

    /**
     * Called when this is read from persisted form.
     * Create equivalent {@link RallyPlugin} instance.
     */
    public RallyPlugin readResolve() {
        try {
            return new RallyPlugin(
                    "",
                    workspace,
                    scmRepoName,
                    "false",
                    "",
                    "",
                    "changesSinceLastSuccessfulBuild".equals(changesSince) ? "SinceLastSuccessfulBuild" : "SinceLastBuild",
                    proxy,
                    "false"
            );
        } catch (RallyException | URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
