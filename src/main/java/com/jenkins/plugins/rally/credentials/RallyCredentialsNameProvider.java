package com.jenkins.plugins.rally.credentials;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import edu.umd.cs.findbugs.annotations.NonNull;

public class RallyCredentialsNameProvider extends CredentialsNameProvider<RallyCredentialsImpl>{
    @NonNull
    @Override
    public String getName(@NonNull RallyCredentialsImpl rallyCredentials) {
        return rallyCredentials.getName();
    }
}
