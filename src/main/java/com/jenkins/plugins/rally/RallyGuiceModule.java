package com.jenkins.plugins.rally;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.jenkins.plugins.rally.connector.RallyConnector;
import com.jenkins.plugins.rally.scm.JenkinsConnector;
import com.jenkins.plugins.rally.scm.ScmConnector;
import com.jenkins.plugins.rally.service.RallyService;

public class RallyGuiceModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class)
                .annotatedWith(Names.named("SERVER URL"))
                .toInstance("https://rally1.rallydev.com");

        bind(String.class)
                .annotatedWith(Names.named("APP NAME"))
                .toInstance("RallyConnect");

        bind(String.class)
                .annotatedWith(Names.named("API VERSION"))
                .toInstance("v2.0");

        bind(RallyConnector.FactoryHelper.class).in(Singleton.class);

        bind(RallyService.class).in(Singleton.class);

        bind(ScmConnector.class).to(JenkinsConnector.class).in(Singleton.class);
        bindRallyConnector();
    }

    protected void bindRallyConnector(){
        bind(RallyConnector.class).in(Singleton.class);
    }
}
