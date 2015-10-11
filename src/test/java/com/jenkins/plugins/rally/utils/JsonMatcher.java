package com.jenkins.plugins.rally.utils;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class JsonMatcher<T> extends TypeSafeMatcher<String> {
    private String jsonPath;
    private T value;

    private JsonMatcher(String jsonPath, T value) {
        this.value = value;
        this.value = value;
        this.jsonPath = jsonPath;
    }

    @Factory
    public static <T> Matcher<String> hasJsonPathValue(String jsonPath, T value) {
        return new JsonMatcher<T>(jsonPath, value);
    }

    @Override
    protected boolean matchesSafely(String json) {
        Object actual = JsonPath.read(json, jsonPath);
        return value != null && value.equals(actual);
    }

    public void describeTo(Description description) {
        description.appendText("Value '" + value + "' for path '" + jsonPath + "'");
    }
}
