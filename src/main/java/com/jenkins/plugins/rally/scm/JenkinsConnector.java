package com.jenkins.plugins.rally.scm;

import com.google.inject.Inject;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.utils.CommitMessageParser;
import com.jenkins.plugins.rally.utils.RallyGeneralUtils;
import com.jenkins.plugins.rally.utils.TemplatedUriResolver;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.User;
import hudson.scm.ChangeLogSet;
import org.joda.time.DateTime;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JenkinsConnector implements ScmConnector {
    private final TemplatedUriResolver uriResolver;
    private ScmConfiguration config;
    private BuildConfiguration buildConfig;

    @Inject
    public JenkinsConnector(ScmConfiguration scmConfig, BuildConfiguration buildConfig) {
        this.uriResolver = new TemplatedUriResolver();

        this.config = scmConfig;
        this.buildConfig = buildConfig;
    }

    public List<RallyUpdateData> getChanges(Run<?, ?> run, PrintStream out) throws RallyException {
        Changes changes;
        // TODO: if a third is added it might be time to inheritance it up
        switch (this.buildConfig.getCaptureRangeAsEnum()) {
            case SinceLastBuild:
                changes = getChangesSinceLastBuild(run);
                break;
            case SinceLastSuccessfulBuild:
                changes = getChangesSinceLastSuccessfulBuild(run);
                break;
            default:
                throw new RallyException("Looking at invalid capture range");
        }

        List<RallyUpdateData> detailsBeans = new ArrayList<>();
        for (ChangeInformation info : changes.getChangeInformation()) {
            for (Object item : info.getChangeLogSet().getItems()) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) item;
                detailsBeans.add(createRallyDetailsDTO(info, entry, run, out));
            }
        }

        return detailsBeans;
    }

    private Changes getChangesSinceLastBuild(Run<?, ?> run) {
        Run pvsRun = run.getPreviousBuild();
        return new Changes(run, pvsRun != null ? pvsRun.getNumber() + 1 : run.getNumber());
    }

    @SuppressWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "I believe findbugs to be in error on this one")
    private Changes getChangesSinceLastSuccessfulBuild(Run<?, ?> build) {
        Run run = build.getPreviousBuild();
        while (run != null && (run.getResult() == null || run.getResult().isWorseThan(Result.SUCCESS)))
            run = run.getPreviousBuild();

        return new Changes(build, run != null ? run.getNumber() + 1 : build.getNumber());
    }

    @SuppressWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "I believe findbugs to be in error on this one")
    private RallyUpdateData createRallyDetailsDTO(
            ChangeInformation changeInformation,
            ChangeLogSet.Entry changeLogEntry,
            Run<?, ?> build,
            PrintStream out) {
        String message = changeLogEntry.getMsg();
        RallyUpdateData details = CommitMessageParser.parse(message);
        details.setOrigBuildNumber(changeInformation.getBuildNumber());
        details.setCurrentBuildNumber(String.valueOf(build.number));
        details.setMsg(getMessage(changeLogEntry, details.getOrigBuildNumber(), details.getCurrentBuildNumber()));
        details.setFilenamesAndActions(getFileNameAndTypes(changeLogEntry));
        details.setOut(out);
        details.setBuildDuration((DateTime.now().getMillis() - build.getStartTimeInMillis()) / 1000D);
        details.setBuildName(build.getParent().getName());
        try {
            details.setBuildUrl(build.getAbsoluteUrl());
        } catch (Exception exception) {
            // thrown if user hasn't configured jenkins root url; can ignore
        }
        details.setRevision(changeLogEntry.getCommitId());
        // if no result is found, it means that it is in progress. will set it to success, since it did not fail
        String jenkinsResult = build.getResult() != null ? build.getResult().toString() : "SUCCESS";
        details.setBuildStatus(RallyGeneralUtils.jenkinsResultToRallyBuildResult(jenkinsResult));
        details.setBuildMessage("build for " + details.getBuildName() + " finished with status: " + jenkinsResult);

        if (changeLogEntry.getTimestamp() == -1) {
            details.setTimeStamp(changeInformation.getBuildTimeStamp());
        } else {
            details.setTimeStamp(toTimeZoneTimeStamp(changeLogEntry.getTimestamp()));
        }

        User jenkinsAuthor = changeLogEntry.getAuthor();
        if(jenkinsAuthor != null) {
            String authorFullName = jenkinsAuthor.getFullName();
            hudson.tasks.Mailer.UserProperty authorMailProperty = jenkinsAuthor.getProperty(hudson.tasks.Mailer.UserProperty.class);
            String authorMaillAddress = authorMailProperty != null ? authorMailProperty.getAddress() : null;
            details.setAuthorFullName(authorFullName);
            details.setAuthorEmailAddress(authorMaillAddress);
        }

        return details;
    }

    private String getMessage(ChangeLogSet.Entry cse, String origBuildNumber, String currentBuildNumber) {
        String msg;
        if (origBuildNumber.equals(currentBuildNumber))
            msg = cse.getAuthor() + " # " + cse.getMsg() + " (Build #" + origBuildNumber + ")";
        else
            msg = cse.getAuthor() + " # " + cse.getMsg() + " (Builds #" + currentBuildNumber + " - " + origBuildNumber + ")";
        return msg;
    }

    private List<RallyUpdateData.FilenameAndAction> getFileNameAndTypes(ChangeLogSet.Entry cse) {
        List<RallyUpdateData.FilenameAndAction> list = new ArrayList<>();
        for (ChangeLogSet.AffectedFile files : cse.getAffectedFiles()) {
            RallyUpdateData.FilenameAndAction filenameAndAction = new RallyUpdateData.FilenameAndAction();
            filenameAndAction.filename = files.getPath();
            filenameAndAction.action = files.getEditType();

            list.add(filenameAndAction);
        }

        return list;
    }

    private String toTimeZoneTimeStamp(long time) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

        return df.format(new Date(time));
    }

    public String getRevisionUriFor(String repository, String revision) {
        return this.uriResolver.resolveCommitUri(this.config.getCommitTemplate(), repository, revision);
    }

    public String getFileUriFor(String repository, String revision, String filename) {
        return this.uriResolver.resolveFileCommitUri(this.config.getFileTemplate(), repository, revision, filename);
    }
}
