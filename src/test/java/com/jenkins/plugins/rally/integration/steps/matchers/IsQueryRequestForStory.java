package com.jenkins.plugins.rally.integration.steps.matchers;

import com.rallydev.rest.request.QueryRequest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsQueryRequestForStory extends TypeSafeMatcher<QueryRequest> {
    @Override
    protected boolean matchesSafely(QueryRequest getRequest) {
        return getRequest.toUrl().startsWith("/hierarchicalrequirement.js?");
    }

    public void describeTo(Description description) {

    }
}