package com.jenkins.plugins.rally.utils;


import com.google.gson.JsonObject;

public class RallyUpdateBean {
    private String state = "In-Progress";
    private String todo = null;
    private String actual = null;
    private String estimate = null;

    public void setEstimate(String estimate) {
        this.estimate = estimate;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTodo(String todo) {
        this.todo = todo;
    }

    public void setActual(String actual) {
        this.actual = actual;
    }

    public JsonObject getJsonObject() {
        JsonObject object = new JsonObject();
        if (state != null) {
        	object.addProperty("State", state);
        }

        if (todo != null) {
            object.addProperty("ToDo", todo);
        }

        if (actual != null) {
            object.addProperty("Actuals", actual);
        }

        if (estimate != null) {
            object.addProperty("Estimate", estimate);
        }

        return object;
    }
}
