package com.jenkins.plugins.rally.integration;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(plugin = {"pretty", "html:target/cucumber"}, features = "src/test/java/com/jenkins/plugins/rally/integration/features")
public class IntegrationTest {
    // Empty
}
