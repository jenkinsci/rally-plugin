package com.jenkins.plugins.rally.config;

import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class AdvancedConfiguration {
    private final URI proxyUri;
    private String shouldCaptureBuildStatus;

    @Inject
    public AdvancedConfiguration(String proxyUri, String shouldCaptureBuildStatus) throws URISyntaxException {
        this.shouldCaptureBuildStatus = shouldCaptureBuildStatus;
        this.proxyUri = StringUtils.isEmpty(proxyUri) ? null : new URI(proxyUri);
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public Boolean shouldCaptureBuildStatus() {
        return Boolean.parseBoolean(shouldCaptureBuildStatus);
    }
}
