package com.jenkins.plugins.rally.utils;

import com.jenkins.plugins.rally.connector.RallyDetailsDTO;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public final class CommitMessageParserTest {

    @Test
    public void shouldParseWorkItemIdFromCommitMessage() {
        String commitMessage = "US12345: do a thing";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getId(), is(equalTo("US12345")));
    }

    @Test
    public void shouldParseDefectFromCommitMessage() {
        String commitMessage = "de12345: fix a bug";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getId(), is(equalTo("de12345")));
    }

    @Test
    public void shouldParseWorkItemFromMultiLineCommitMessage() {
        String commitMessage = "Do a thing\nfixes US12345";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getId(), is(equalTo("US12345")));
    }

    @Test
    public void shouldParseDefectFromMultiLineCommitMessage() {
        String commitMessage = "fix a bug\ncorrects de12345";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getId(), is(equalTo("de12345")));
    }

    @Test
    public void shouldNotCatchOddballWorkItemCases() {
        String[] commitMessages = new String[]
                {
                        "HELLO BUS1, DO YOU READ?",
                        "IDE1 IS BETTER THAN IDE2",
                        "\"BASTA1!!\" él gritó"
                };

        for (String commitMessage : commitMessages) {
            RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);
            assertThat(details.getId(), is(isEmptyString()));
        }
    }

    @Test
    public void shouldParseTaskIdFromCommitMessage() {
        String commitMessage = "US12345: (TA54321) do a thing";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskID(), is(equalTo("TA54321")));
    }

    @Test
    public void shouldParseTaskIndexFromCommitMessage() {
        String commitMessage = "US12345: fixes #3";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskIndex(), is(equalTo("3")));
    }

    @Test
    public void shouldParseTaskActualsFromCommitMessage() {
        String commitMessage = "US12345: fixes #3 with actuals: 15";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskActuals(), is(equalTo("15")));
    }

    @Test
    public void shouldParseTaskStatusFromCommitMessage() {
        String[][] commitMessageAndStatusPairs = new String[][]
                {
                        { "US12345: #2\n status: in progress", "In-Progress" },
                        { "US12345: #2\n status: complete", "Completed" },
                        { "US12345: #2\n status: define", "Defined" }
                };

        for (String[] pair : commitMessageAndStatusPairs) {
            RallyDetailsDTO details = CommitMessageParser.parse(pair[0]);

            assertThat(details.getTaskStatus(), is(equalTo(pair[1])));
        }
    }

    @Test
    public void shouldParseTaskToDoHoursFromCommitMessage() {
        String commitMessage = "US12345: fixes #3 with to do: 15";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskToDO(), is(equalTo("15")));
    }

    @Test
    public void shouldMarkTaskToDoAsZeroWhenTaskStatusIsCompleted() {
        String commitMessage = "US12345: fixes #3 with status: completed";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskToDO(), is(equalTo("0")));
    }

    @Test
    public void shouldParseTaskEstimationFromCommitMessage() {
        String commitMessage = "US12345: fixes #3 with estimate: 15";

        RallyDetailsDTO details = CommitMessageParser.parse(commitMessage);

        assertThat(details.getTaskEstimates(), is(equalTo("15")));
    }
}
