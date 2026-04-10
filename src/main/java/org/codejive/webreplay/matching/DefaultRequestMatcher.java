package org.codejive.webreplay.matching;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.codejive.tproxy.ProxyRequest;

/**
 * Default request matching strategy that compares requests based on:
 *
 * <ul>
 *   <li>HTTP method (GET, POST, etc.)
 *   <li>URI (including scheme, host, port, path, and query)
 *   <li>Optionally, specific headers (configurable)
 *   <li>Optionally, request body (configurable)
 * </ul>
 *
 * <p>By default, only method and URI are compared. Headers and body matching can be enabled via the
 * builder.
 */
public class DefaultRequestMatcher implements RequestMatcher {
    private final boolean matchBody;
    private final Set<String> headersToMatch;

    private DefaultRequestMatcher(Builder builder) {
        this.matchBody = builder.matchBody;
        this.headersToMatch = new HashSet<>(builder.headersToMatch);
    }

    /**
     * Creates a new builder for configuring a DefaultRequestMatcher.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default matcher that only compares method and URI.
     *
     * @return a default matcher
     */
    public static DefaultRequestMatcher methodAndUri() {
        return new Builder().build();
    }

    @Override
    public boolean matches(ProxyRequest request1, ProxyRequest request2) {
        if (request1 == request2) {
            return true;
        }
        if (request1 == null || request2 == null) {
            return false;
        }

        // Method must match
        if (!Objects.equals(request1.method(), request2.method())) {
            return false;
        }

        // URI must match (including query parameters)
        if (!urisMatch(request1.uri(), request2.uri())) {
            return false;
        }

        // Check specific headers if configured
        for (String headerName : headersToMatch) {
            String value1 = request1.headers().first(headerName);
            String value2 = request2.headers().first(headerName);
            if (!Objects.equals(value1, value2)) {
                return false;
            }
        }

        // Check body if configured
        if (matchBody) {
            if (!Arrays.equals(request1.body(), request2.body())) {
                return false;
            }
        }

        return true;
    }

    private boolean urisMatch(URI uri1, URI uri2) {
        if (uri1 == uri2) {
            return true;
        }
        if (uri1 == null || uri2 == null) {
            return false;
        }

        // Compare all URI components
        return Objects.equals(uri1.getScheme(), uri2.getScheme())
                && Objects.equals(uri1.getHost(), uri2.getHost())
                && uri1.getPort() == uri2.getPort()
                && Objects.equals(uri1.getPath(), uri2.getPath())
                && Objects.equals(uri1.getQuery(), uri2.getQuery());
    }

    /** Builder for creating configured DefaultRequestMatcher instances. */
    public static class Builder {
        private boolean matchBody = false;
        private final Set<String> headersToMatch = new HashSet<>();

        /**
         * Enables request body matching. When enabled, requests must have identical bodies to be
         * considered a match.
         *
         * @return this builder
         */
        public Builder matchBody() {
            this.matchBody = true;
            return this;
        }

        /**
         * Adds a header to be included in matching. The specified header must have the same value
         * in both requests for them to match.
         *
         * @param headerName the case-insensitive header name
         * @return this builder
         */
        public Builder matchHeader(String headerName) {
            this.headersToMatch.add(headerName);
            return this;
        }

        /**
         * Adds multiple headers to be included in matching.
         *
         * @param headerNames the case-insensitive header names
         * @return this builder
         */
        public Builder matchHeaders(String... headerNames) {
            this.headersToMatch.addAll(Arrays.asList(headerNames));
            return this;
        }

        /**
         * Builds the configured DefaultRequestMatcher.
         *
         * @return a new DefaultRequestMatcher
         */
        public DefaultRequestMatcher build() {
            return new DefaultRequestMatcher(this);
        }
    }
}
