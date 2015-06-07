package com.jenkins.plugins.rally.credentials;

import com.cloudbees.plugins.credentials.Credentials;
import hudson.util.Secret;

public interface RallyCredentials extends Credentials {

    String getName();

    String getDescription();

    Secret getApiKey();

}
