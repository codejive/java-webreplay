package org.codejive.webreplay.matching;

import org.codejive.tproxy.ProxyRequest;

/**
 * Strategy for determining if two HTTP requests should be considered equivalent for caching and
 * replay purposes.
 *
 * <p>Implementations define what constitutes a "match" between a live request and a recorded
 * request. Common strategies include matching on:
 *
 * <ul>
 *   <li>HTTP method and URI
 *   <li>Query parameters
 *   <li>Specific headers (e.g., Authorization, Content-Type)
 *   <li>Request body
 * </ul>
 */
@FunctionalInterface
public interface RequestMatcher {
    /**
     * Determines if two requests match.
     *
     * @param request1 the first request
     * @param request2 the second request
     * @return true if the requests match, false otherwise
     */
    boolean matches(ProxyRequest request1, ProxyRequest request2);
}
