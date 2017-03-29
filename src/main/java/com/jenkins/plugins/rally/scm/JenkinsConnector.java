package com.jenkins.plugins.rally.scm;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;

import com.google.inject.Inject;
import com.jenkins.plugins.rally.RallyException;
import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import com.jenkins.plugins.rally.utils.CommitMessageParser;
import com.jenkins.plugins.rally.utils.TemplatedUriResolver;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;

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

    public List<RallyUpdateData> getChanges(AbstractBuild build, PrintStream out) throws RallyException {
        Changes changes;
        // TODO: if a third is added it might be time to inheritance it up
        switch (this.buildConfig.getCaptureRangeAsEnum()) {
            case SinceLastBuild:
                changes = getChangesSinceLastBuild(build);
                break;
            case SinceLastSuccessfulBuild:
                changes = getChangesSinceLastSuccessfulBuild(build);
                break;
            default:
                throw new RallyException("Looking at invalid capture range");
        }

        List<RallyUpdateData> detailsBeans = new ArrayList<>();
        for (ChangeInformation info : changes.getChangeInformation()) {
            for (Object item : info.getChangeLogSet().getItems()) {
                ChangeLogSet.Entry entry = (ChangeLogSet.Entry) item;
                detailsBeans.add(createRallyDetailsDTO(info, entry, build, out));
            }
        }

        return detailsBeans;
    }

    private Changes getChangesSinceLastBuild(AbstractBuild build) {
        Run run = build.getPreviousBuild();
        return new Changes(build, run != null ? run.getNumber() + 1 : build.getNumber());
    }

    @SuppressWarnings(
            value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "I believe findbugs to be in error on this one")
    private Changes getChangesSinceLastSuccessfulBuild(AbstractBuild build) {
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
            AbstractBuild build,
            PrintStream out) {
        String message = changeLogEntry.getMsg();
        RallyUpdateData details = CommitMessageParser.parse(message);
        details.setOrigBuildNumber(changeInformation.getBuildNumber());
        details.setCurrentBuildNumber(String.valueOf(build.number));
        details.setMsg(getMessage(changeLogEntry, details.getOrigBuildNumber(), details.getCurrentBuildNumber()));
        details.setFilenamesAndActions(getFileNameAndTypes(changeLogEntry, out));
        details.setOut(out);
        details.setBuildDuration((DateTime.now().getMillis() - build.getStartTimeInMillis()) / 1000D);
        details.setBuildName(build.getProject().getName());
        try {
            details.setBuildUrl(build.getAbsoluteUrl());
        } catch (Exception exception) {
            // thrown if user hasn't configured jenkins root url; can ignore
        }
        details.setRevision(changeLogEntry.getCommitId());
        details.setBuildStatus(build.getResult().toString());
        details.setBuildMessage("build for " + details.getBuildName() + " finished with status: " + details.getBuildStatus());

        if (changeLogEntry.getTimestamp() == -1) {
            details.setTimeStamp(changeInformation.getBuildTimeStamp());
        } else {
            details.setTimeStamp(toTimeZoneTimeStamp(changeLogEntry.getTimestamp()));
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

    private List<RallyUpdateData.FilenameAndAction> getFileNameAndTypes(ChangeLogSet.Entry cse, PrintStream out) {
        List<RallyUpdateData.FilenameAndAction> list = new ArrayList<>();
        for (ChangeLogSet.AffectedFile files : cse.getAffectedFiles()) {
            RallyUpdateData.FilenameAndAction filenameAndAction = new RallyUpdateData.FilenameAndAction();

            Class<?> fileClass = files.getClass();
            if ("hudson.scm.CVSChangeLogSet.File".equals(fileClass.getCanonicalName())) {
            	String fileRevision = "";
				try {
					fileRevision = (String)fileClass.getDeclaredMethod("getRevision", new Class[] {}).invoke(files, null);
				} catch (Exception e) {
					e.printStackTrace(out);
				}
            	filenameAndAction.filename = files.getPath() + " " + fileRevision;
            } else {
            	filenameAndAction.filename = files.getPath();
            }
            filenameAndAction.action = files.getEditType();

            list.add(filenameAndAction);
        }

        return list;
    }

    private String toTimeZoneTimeStamp(long time) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");

        return df.format(new Date(time));
    }

    public String getRevisionUriFor(String revision) {
        return this.uriResolver.resolveCommitUri(this.config.getCommitTemplate(), revision);
    }

    public String getFileUriFor(String revision, String filename) {
        return this.uriResolver.resolveFileCommitUri(this.config.getFileTemplate(), revision, filename);
    }
}
