package com.jenkins.plugins.rally.integration.steps.matchers;

import com.rallydev.rest.request.CreateRequest;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class IsCreateRequestForChangeset extends TypeSafeMatcher<CreateRequest> {
    @Override
    protected boolean matchesSafely(CreateRequest createRequest) {
        return createRequest.toUrl().startsWith("/changeset/create.js?");
    }

    public void describeTo(Description description) {

    }
}