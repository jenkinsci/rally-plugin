package com.jenkins.plugins.rally.integration.steps;

import com.jenkins.plugins.rally.service.RallyService;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.response.QueryResponse;
import cucumber.runtime.java.guice.ScenarioScoped;

@ScenarioScoped
public class StepStateContainer {
    private QueryResponse preexistingRepositoryObjectQueryResponse;
    private RallyService rallyService;
    private Exception caughtException;
    private RallyRestApi rallyApi;

    public QueryResponse getPreexistingRepositoryObjectQueryResponse() {
        return preexistingRepositoryObjectQueryResponse;
    }

    public void setPreexistingRepositoryObjectQueryResponse(QueryResponse preexistingRepositoryObjectQueryResponse) {
        this.preexistingRepositoryObjectQueryResponse = preexistingRepositoryObjectQueryResponse;
    }

    public RallyService getRallyService() {
        return rallyService;
    }

    public void setRallyService(RallyService rallyService) {
        this.rallyService = rallyService;
    }

    public Exception getCaughtException() {
        return caughtException;
    }

    public void setCaughtException(Exception caughtException) {
        this.caughtException = caughtException;
    }

    public RallyRestApi getRallyApi() {
        return rallyApi;
    }

    public void setRallyApi(RallyRestApi rallyApi) {
        this.rallyApi = rallyApi;
    }
}
