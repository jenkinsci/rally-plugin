package com.jenkins.plugins.rally.credentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class RallyCredentialsUIHelper {
    private RallyCredentialsUIHelper(){}

    @SuppressWarnings("unused")
    public static ListBoxModel doFillCredentialsIdItems(Jenkins context, String remoteBase) {
        if (context == null || !context.hasPermission(Item.CONFIGURE)) {
            return new StandardListBoxModel();
        }

        List<DomainRequirement> domainRequirements = newArrayList();
        return new StandardListBoxModel()
                .withEmptySelection()
                .withMatching(
                        CredentialsMatchers.anyOf(
                                CredentialsMatchers.instanceOf(RallyCredentials.class)),
                        CredentialsProvider.lookupCredentials(
                                StandardCredentials.class,
                                context,
                                ACL.SYSTEM,
                                domainRequirements));
    }
}
