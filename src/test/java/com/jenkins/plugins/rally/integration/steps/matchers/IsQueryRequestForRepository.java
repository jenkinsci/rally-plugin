package com.jenkins.plugins.rally.integration.steps.matchers;

import com.rallydev.rest.request.QueryRequest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsQueryRequestForRepository extends TypeSafeMatcher<QueryRequest> {

    @Override
    protected boolean matchesSafely(QueryRequest queryRequest) {
        return queryRequest.toUrl().startsWith("/scmrepository.js?");
    }

    public void describeTo(Description description) {

    }
}