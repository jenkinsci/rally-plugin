package com.jenkins.plugins.rally.utils;

import com.jenkins.plugins.rally.connector.RallyUpdateData;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;

public final class CommitMessageParser {
    public static RallyUpdateData parse(String commitMessage) {
        RallyUpdateData details = new RallyUpdateData();
        details.addIds(getWorkItemFromCommitMessage(commitMessage));
        details.setTaskID(getTaskItemFromCommitMessage(commitMessage));
        details.setTaskIndex(getTaskIndexFromCommitMessage(commitMessage));
        details.setTaskActuals(getTaskActualsFromCommitMessage(commitMessage));
        details.setTaskStatus(getTaskStatusFromCommitMessage(commitMessage));
        details.setTaskToDO(getTaskToDoFromCommitMessage(commitMessage));
        details.setTaskEstimates(getTaskEstimatesFromCommitMessage(commitMessage));

        if (details.getTaskStatus().equals("Completed")) {
            details.setTaskToDO("0");
        }

        return details;
    }

    private static List<String> getWorkItemFromCommitMessage(String commitMessage) {
        return executeRegularExpressionReturningList("\\b((?:US|DE)\\d+)\\b", commitMessage);
    }

    private static String getTaskItemFromCommitMessage(String commitMessage) {
        return executeRegularExpression("\\b(TA\\d+)\\b", commitMessage);
    }

    private static String getTaskIndexFromCommitMessage(String commitMessage) {
        return executeRegularExpression("(?:^|\\s)# ?(\\d+)\\b", commitMessage);
    }

    private static String getTaskActualsFromCommitMessage(String commitMessage) {
        return executeRegularExpression("\\bactuals? ?: ?(\\d+)\\b", commitMessage);
    }

    private static String getTaskToDoFromCommitMessage(String commitMessage) {
        return executeRegularExpression("\\bto ?do ?: ?(\\d+)\\b", commitMessage);
    }

    private static String getTaskEstimatesFromCommitMessage(String commitMessage) {
        return executeRegularExpression("\\bestimat(?:es?|ions?) ?: ?(\\d+)\\b", commitMessage);
    }

    private static String getTaskStatusFromCommitMessage(String commitMessage) {
        String status = "";
        if (matches("\\bstatus ?: ?in[\\- ]progress?\\b", commitMessage)) {
            status = "In-Progress";
        }

        if (matches("\\bstatus ?: ?completed?\\b", commitMessage)) {
            status = "Completed";
        }

        if (matches("\\bstatus ?: ?defined?\\b", commitMessage)) {
            status = "Defined";
        }

        return status;
    }

    private static boolean matches(String regex, String commitMessage) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(commitMessage);
        return matcher.find();
    }

    private static String executeRegularExpression(String regex, String commitMessage) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(commitMessage);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static List<String> executeRegularExpressionReturningList(String regex, String commitMessage) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(commitMessage);
        List<String> matches = newArrayList();

        while (matcher.find()) {
            matches.add(matcher.group(1));
        }

        return matches;
    }
}
