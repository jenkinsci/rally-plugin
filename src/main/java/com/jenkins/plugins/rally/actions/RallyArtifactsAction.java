package com.jenkins.plugins.rally.actions;

import com.jenkins.plugins.rally.RallyArtifact;
import hudson.model.Action;
import hudson.model.Run;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.CheckForNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@ExportedBean
public class RallyArtifactsAction implements Action, Serializable {
    private static final long serialVersionUID = 1L;

    private final List<RallyArtifact> rallyArtifacts = new ArrayList<>();

    public RallyArtifactsAction(){}

    public void addRallyArtifacts(Collection<RallyArtifact> artifacts){
        rallyArtifacts.addAll(artifacts);
    }

    public List<RallyArtifact> getRallyArtifacts() {
        return Collections.unmodifiableList(rallyArtifacts);
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "Rally Artifacts";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "rally-artifacts";
    }

    @Restricted(NoExternalUse.class) // only used from stapler/jelly
    @CheckForNull
    @SuppressWarnings("unused")
    public Run<?,?> getOwningRun() {
        StaplerRequest req = Stapler.getCurrentRequest();
        if (req == null) {
            return null;
        }
        return req.findAncestorObject(Run.class);
    }
}
