package com.jenkins.plugins.rally.scm;

import com.jenkins.plugins.rally.config.BuildConfiguration;
import com.jenkins.plugins.rally.config.ScmConfiguration;
import com.jenkins.plugins.rally.connector.RallyUpdateData;
import hudson.model.AbstractBuild;
import hudson.model.Project;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MyChangeLogSet extends ChangeLogSet {
    List<Entry> detailsList;

    @SuppressWarnings({"unchecked", "deprecation"})
    protected MyChangeLogSet(AbstractBuild build, List<Entry> detailsList) {
        super(build);

        this.detailsList = detailsList;
    }

    @Override
    public boolean isEmptySet() {
        return false;
    }

    public Iterator iterator() {
        return detailsList.iterator();
    }
}

@RunWith(MockitoJUnitRunner.class)
public class JenkinsConnectorTest {
    @Test
    public void shouldCaptureChangesSinceLastBuild() throws Exception {
        String timestamp = "1970-01-01 00:00:00+0000";

        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getCommitId()).thenReturn("12345");
        when(entry.getMsg()).thenReturn("message");

        ChangeLogSet changeLogSet = new MyChangeLogSet(null, Collections.singletonList(entry));

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getPreviousBuild()).thenReturn(null);
        when(build.getNumber()).thenReturn(5);
        when(build.getTimestampString2()).thenReturn(timestamp);
        when(build.getChangeSet()).thenReturn(changeLogSet);
        when(build.getProject()).thenReturn(mock(Project.class));
        when(build.getResult()).thenReturn(Result.SUCCESS);

        ScmConfiguration scmConfiguration = new ScmConfiguration(null, null);
        BuildConfiguration buildConfiguration = new BuildConfiguration("SinceLastBuild");
        ScmConnector connector = new JenkinsConnector(scmConfiguration, buildConfiguration);

        List<RallyUpdateData> detailsList = connector.getChanges(build, null);

        assertThat(detailsList, hasSize(1));
    }

    @Test
    public void shouldCaptureChangesSinceLastSuccessfulBuild() throws Exception {
        String timestamp = "1970-01-01 00:00:00+0000";

        final ChangeLogSet.Entry firstEntry = mock(ChangeLogSet.Entry.class);
        when(firstEntry.getCommitId()).thenReturn("12345");
        when(firstEntry.getMsg()).thenReturn("message");

        final ChangeLogSet.Entry secondEntry = mock(ChangeLogSet.Entry.class);
        when(secondEntry.getCommitId()).thenReturn("12345");
        when(secondEntry.getMsg()).thenReturn("message");

        ChangeLogSet firstChangeLogSet = new MyChangeLogSet(null, Collections.singletonList(firstEntry));
        ChangeLogSet secondChangeLogSet = new MyChangeLogSet(null, Collections.singletonList(secondEntry));

        AbstractBuild lastSuccessfulBuild = mock(AbstractBuild.class);
        when(lastSuccessfulBuild.getPreviousBuild()).thenReturn(null);
        when(lastSuccessfulBuild.getNumber()).thenReturn(5);
        when(lastSuccessfulBuild.getTimestampString2()).thenReturn(timestamp);
        when(lastSuccessfulBuild.getChangeSet()).thenReturn(secondChangeLogSet);
        when(lastSuccessfulBuild.getProject()).thenReturn(mock(Project.class));
        when(lastSuccessfulBuild.getResult()).thenReturn(Result.FAILURE);

        AbstractBuild build = mock(AbstractBuild.class);
        when(build.getPreviousBuild()).thenReturn(lastSuccessfulBuild);
        when(build.getNumber()).thenReturn(5);
        when(build.getTimestampString2()).thenReturn(timestamp);
        when(build.getChangeSet()).thenReturn(firstChangeLogSet);
        when(build.getProject()).thenReturn(mock(Project.class));
        when(build.getResult()).thenReturn(Result.SUCCESS);

        ScmConfiguration scmConfiguration = new ScmConfiguration(null, null);
        BuildConfiguration buildConfiguration = new BuildConfiguration("SinceLastSuccessfulBuild");
        ScmConnector connector = new JenkinsConnector(scmConfiguration, buildConfiguration);

        List<RallyUpdateData> detailsList = connector.getChanges(build, null);

        assertThat(detailsList, hasSize(2));
    }
}
