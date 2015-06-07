package com.jenkins.plugins.rally.config;

import com.google.inject.Inject;

import java.net.URI;
import java.net.URISyntaxException;

public class AdvancedConfiguration {
    private final URI proxyUri;

    @Inject
    public AdvancedConfiguration(String proxyUri) throws URISyntaxException {
        this.proxyUri = new URI(proxyUri);
    }

    public URI getProxyUri() {
        return proxyUri;
    }
}
