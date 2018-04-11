package com.jenkins.plugins.rally.credentials;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

@NameWith(value = RallyCredentialsNameProvider.class, priority = 50)
public class RallyCredentialsImpl extends BaseStandardCredentials implements RallyCredentials {
    @NonNull
    private final Secret apiKey;

    @NonNull
    private final String name;

    @DataBoundConstructor
    public RallyCredentialsImpl(@CheckForNull CredentialsScope scope,
                                @CheckForNull String id,
                                @NonNull String name,
                                @CheckForNull String description,
                                @CheckForNull String apiKey) {
        super(scope, id, description);
        this.apiKey = Secret.fromString(apiKey);
        this.name = name;
    }

    @NonNull
    public Secret getApiKey() {
        return this.apiKey;
    }

    @NonNull
    public String getName() {
        return this.name;
    }

    @Extension public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        /** {@inheritDoc} */
        @Override
        public String getDisplayName() {
            return "Rally API Key";
        }
    }
}
