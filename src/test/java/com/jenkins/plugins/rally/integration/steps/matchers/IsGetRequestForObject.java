package com.jenkins.plugins.rally.integration.steps.matchers;

import com.rallydev.rest.request.GetRequest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsGetRequestForObject extends TypeSafeMatcher<GetRequest> {
    @Override
    protected boolean matchesSafely(GetRequest getRequest) {
        return getRequest.toUrl().startsWith("_ref.js?");
    }

    public void describeTo(Description description) {

    }
}
