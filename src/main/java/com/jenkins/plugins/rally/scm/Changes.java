package com.jenkins.plugins.rally.scm;

import hudson.model.AbstractBuild;
import hudson.model.Api;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.util.LinkedList;
import java.util.List;

@ExportedBean
public class Changes {

    private List<ChangeInformation> changeInformation = new LinkedList<>();

    private final Run<?, ?> build;

    public Changes(Run<?, ?> build, int buildNumber) {
        this.build = build;
        Run<?, ?> b = build;
        // TODO: is this logic necessary? if so, write a test.
        while (b != null && b.getNumber() >= buildNumber) {
            populateChangeInformation(b, getBuildChangeSet(b));
            b = b.getPreviousBuild();
        }
    }

    private static ChangeLogSet<? extends ChangeLogSet.Entry>  getBuildChangeSet(Run<?, ?> build){
        if(build instanceof WorkflowRun){
            List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = ((WorkflowRun)build).getChangeSets();
            return changeSets.isEmpty() ? ChangeLogSet.createEmpty(build) : changeSets.get(0);
        }else if(build instanceof AbstractBuild<?,?>){
            return ((AbstractBuild<?,?>)build).getChangeSet();
        }
        return ChangeLogSet.createEmpty(build);
    }

    private void populateChangeInformation(Run<?, ?> build, ChangeLogSet changeLogSet) {
        ChangeInformation ci = new ChangeInformation();
        ci.setBuildNumber(String.valueOf(build.getNumber()));
        ci.setBuildTimeStamp(build.getTimestampString2());
        ci.setChangeLogSet(changeLogSet);
        ci.setBuild(build);
        changeInformation.add(ci);
    }

    /**
     * Remote API access.
     * @return Api
     */
    public final Api getApi() {
        return new Api(this);
    }

    public Run<?, ?> getBuild() {
        return build;
    }

    @Exported
    public List<ChangeInformation> getChangeInformation() {
        return changeInformation;
    }
}
