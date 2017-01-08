package com.jenkins.plugins.rally.scm;

import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.model.Api;
import hudson.scm.ChangeLogSet;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;

@ExportedBean
public class Changes {

    private List<ChangeInformation> changeInformation = new LinkedList<ChangeInformation>();

    private final Run build;

    public Changes(Run<?, ?> build, int buildNumber) {
        this.build = build;
        Run<?, ?> b = build;
        
        // TODO: is this logic necessary? if so, write a test.
        while (b != null && b.getNumber() >= buildNumber) {
            if(build instanceof AbstractBuild<?,?>) {
                AbstractBuild<?,?> ab = (AbstractBuild<?,?>)build;
                populateChangeInformation(b, ab.getChangeSet());
            } else {
                try {
                    // checking for WorkflowRun's getChangeSets method
                    List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets = (List<ChangeLogSet<? extends ChangeLogSet.Entry>>)b.getClass().getMethod("getChangeSets").invoke(b);
                    if(!changeSets.isEmpty()) {
                        populateChangeInformation(b, changeSets.get(0));
                    }
                } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                    //
                }
            }
            b = b.getPreviousBuild();
        }
    }

    private void populateChangeInformation(Run build, ChangeLogSet changeLogSet) {
        ChangeInformation ci = new ChangeInformation();
        ci.setBuildNumber(String.valueOf(build.getNumber()));
        ci.setBuildTimeStamp(build.getTimestampString2());
        ci.setChangeLogSet(changeLogSet);
        ci.setBuild(build);
        changeInformation.add(ci);
    }

    /**
     * Remote API access.
     */
    public final Api getApi() {
        return new Api(this);
    }

    public Run getBuild() {
        return build;
    }

    @Exported
    public List<ChangeInformation> getChangeInformation() {
        return changeInformation;
    }
}
