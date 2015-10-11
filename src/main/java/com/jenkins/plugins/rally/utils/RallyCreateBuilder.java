package com.jenkins.plugins.rally.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jenkins.plugins.rally.RallyException;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.CreateRequest;
import com.rallydev.rest.response.CreateResponse;

import java.io.IOException;
import java.util.List;

public class RallyCreateBuilder {
    private JsonObject createObject;
    private RallyRestApi rallyRestApi;

    public static RallyCreateBuilder createObjectWith(RallyRestApi restApi) {
        RallyCreateBuilder rallyCreateBuilder = new RallyCreateBuilder();
        rallyCreateBuilder.rallyRestApi = restApi;
        rallyCreateBuilder.createObject = new JsonObject();
        return rallyCreateBuilder;
    }

    public RallyCreateBuilder withReference(String refName, JsonElementBuilder value) {
        this.createObject.add(refName, value.build());
        return this;
    }

    public RallyCreateBuilder andProperty(String name, String value) {
        this.createObject.addProperty(name, value);
        return this;
    }

    public RallyCreateBuilder andProperty(String name, Number value) {
        this.createObject.addProperty(name, value);
        return this;
    }

    public RallyCreateBuilder andArrayContaining(JsonElementBuilder builder) {
        JsonArray array = new JsonArray();
        array.add(builder.build());
        this.createObject.add(builder.getArrayName(), array);
        return this;
    }

    public String andExecuteReturningRefFor(String type) throws RallyException {
        CreateRequest request = new CreateRequest(type, this.createObject);
        try {
            CreateResponse createResponse = this.rallyRestApi.create(request);

            if (!createResponse.wasSuccessful()) {
                throw new RallyException();
            }

            return createResponse.getObject().get("_ref").getAsString();
        } catch (IOException exception) {
            throw new RallyException(exception);
        }
    }

    public RallyCreateBuilder andPropertyContainingArray(String name, List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            JsonElement element = new JsonPrimitive(value);
            array.add(element);
        }

        this.createObject.add(name, array);

        return this;
    }
}