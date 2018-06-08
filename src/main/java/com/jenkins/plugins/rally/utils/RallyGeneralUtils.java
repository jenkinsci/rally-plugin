package com.jenkins.plugins.rally.utils;

import hudson.model.Result;

public class RallyGeneralUtils {
    public static boolean isStory(String name) {
        return name.toLowerCase().startsWith("us");
    }

    public static String jenkinsResultToRallyBuildResult(String jenkinsResult) {
        switch (jenkinsResult){
            case "ABORTED":
            case "FAILURE":
                return "FAILURE";
            case "SUCCESS":
                return "SUCCESS";
            case "NOT_BUILT":
                return "UNKNOWN";
            case "UNSTABLE":
                return "INCOMPLETE";
        }
        throw new IllegalArgumentException("Illegal jenkins result: " + jenkinsResult);
    }
}
