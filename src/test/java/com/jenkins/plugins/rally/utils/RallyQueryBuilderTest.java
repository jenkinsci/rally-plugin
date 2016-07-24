package com.jenkins.plugins.rally.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public final class RallyQueryBuilderTest {

    final String stubAttribute = "NOT-THERE";
    final double defaultDouble = 0.0d;


    @Test
    public void notDefinedTaskAttributeAsDoubleShouldFailSafelyToDefault() {
        JsonObject fakeJsonObject = new JsonObject();
        RallyQueryBuilder.RallyQueryResponseObject sut = new RallyQueryBuilder.RallyQueryResponseObject(fakeJsonObject);

        double response = sut.getTaskAttributeAsDouble(stubAttribute);

        assertThat(response, is(equalTo(defaultDouble)));
    }

    @Test
    public void nullJsonTaskAttributeAsDoubleShouldFailSafelyToDefault() {
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.add(stubAttribute, JsonNull.INSTANCE);
        RallyQueryBuilder.RallyQueryResponseObject sut = new RallyQueryBuilder.RallyQueryResponseObject(fakeJsonObject);

        double response = sut.getTaskAttributeAsDouble(stubAttribute);

        assertThat(response, is(equalTo(defaultDouble)));
    }

    @Test
    public void properTaskAttributeAsDoubleShouldReturnCorrectly() {
        double properDouble = 1.1d;
        JsonObject fakeJsonObject = new JsonObject();
        fakeJsonObject.addProperty(stubAttribute, properDouble );
        RallyQueryBuilder.RallyQueryResponseObject sut = new RallyQueryBuilder.RallyQueryResponseObject(fakeJsonObject);

        double response = sut.getTaskAttributeAsDouble(stubAttribute);

        assertThat(response, is(equalTo(properDouble)));
    }
}
