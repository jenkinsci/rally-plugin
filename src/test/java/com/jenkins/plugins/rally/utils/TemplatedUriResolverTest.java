package com.jenkins.plugins.rally.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class TemplatedUriResolverTest {

    private static final String REPOSITORY = "http://repository";
    private static final String REVISION = "12345";
    private static final String FILENAME = "file.txt";

    @Test
    public void shouldInjectRevisionVariableIntoUri() throws Exception {
        TemplatedUriResolver uriResolver = new TemplatedUriResolver();
        String resolvedUri = uriResolver.resolveCommitUri("http://test.com/${revision}", REPOSITORY, REVISION);

        assertThat(resolvedUri, is(equalTo("http://test.com/12345")));
    }

    @Test
    public void shouldNotInjectRevisionIntoUntemplatedVariable() throws Exception {
        TemplatedUriResolver uriResolver = new TemplatedUriResolver();
        String resolvedUri = uriResolver.resolveCommitUri("http://test.com/23456", REPOSITORY, REVISION);

        assertThat(resolvedUri, is(equalTo("http://test.com/23456")));
    }

    @Test
    public void shouldInjectFileVariableIntoUri() throws Exception {
        TemplatedUriResolver uriResolver = new TemplatedUriResolver();
        String resolvedUri = uriResolver.resolveFileCommitUri("http://test.com/${revision}/${file}", REPOSITORY, REVISION, FILENAME);

        assertThat(resolvedUri, is(equalTo("http://test.com/12345/file.txt")));
    }

    @Test
    public void shouldInjectFileVariableIntoUriWithoutCommitVariable() throws Exception {
        TemplatedUriResolver uriResolver = new TemplatedUriResolver();
        String resolvedUri = uriResolver.resolveFileCommitUri("http://test.com/${file}", REPOSITORY, REVISION, FILENAME);

        assertThat(resolvedUri, is(equalTo("http://test.com/file.txt")));
    }

    @Test
    public void shouldInjectCommitVariableIntoUriWithoutFileVariable() throws Exception {
        TemplatedUriResolver uriResolver = new TemplatedUriResolver();
        String resolvedUri = uriResolver.resolveFileCommitUri("http://test.com/${revision}", REPOSITORY, REVISION, FILENAME);

        assertThat(resolvedUri, is(equalTo("http://test.com/12345")));
    }

    @Test
    public void shouldInjectRepoVariableAndCommitVariableIntoUriWithoutFileVariable() throws Exception {
        TemplatedUriResolver uriResolver = new TemplatedUriResolver();
        String resolvedUri = uriResolver.resolveFileCommitUri("${repository}/${revision}", REPOSITORY, REVISION, FILENAME);

        assertThat(resolvedUri, is(equalTo("http://repository/12345")));
    }
}
