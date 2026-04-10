package org.codejive.webreplay;

/**
 * Operating modes for the web replay proxy.
 *
 * <ul>
 *   <li>{@link #RECORD} - All traffic passes through and is recorded to storage
 *   <li>{@link #CACHE} - Traffic is served from cache when available, otherwise passed through and
 *       recorded
 *   <li>{@link #REPLAY} - Only cached traffic is served; non-matching requests return 404
 * </ul>
 */
public enum ReplayMode {
    /**
     * Record mode: all requests are passed through to the actual server and both requests and
     * responses are recorded to storage.
     */
    RECORD,

    /**
     * Cache mode: requests are first checked against storage. If a matching recorded exchange
     * exists, the cached response is returned. Otherwise, the request is passed through to the
     * actual server and the exchange is recorded.
     */
    CACHE,

    /**
     * Replay mode: requests are checked against storage. If a matching recorded exchange exists,
     * the cached response is returned. If no match is found, a 404 error response is returned
     * instead of passing the request through.
     */
    REPLAY
}
