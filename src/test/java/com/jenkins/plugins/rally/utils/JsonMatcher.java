package com.jenkins.plugins.rally.utils;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class JsonMatcher extends TypeSafeMatcher<String> {
    private String jsonPath;
    private String value;

    private JsonMatcher(String jsonPath, String value) {
        this.value = value;
        this.jsonPath = jsonPath;
    }

    @Factory
    public static Matcher<String> hasJsonPathValue(String jsonPath, String value) {
        return new JsonMatcher(jsonPath, value);
    }

    @Override
    protected boolean matchesSafely(String json) {
        String actual = JsonPath.read(json, jsonPath);
        return value != null && value.equals(actual);
    }

    public void describeTo(Description description) {
        description.appendText("Value '" + value + "' for path '" + jsonPath + "'");
    }
}
