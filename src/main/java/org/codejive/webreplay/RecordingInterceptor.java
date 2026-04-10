package org.codejive.webreplay;

import java.io.IOException;
import java.util.Optional;
import org.codejive.tproxy.Headers;
import org.codejive.tproxy.Interceptor;
import org.codejive.tproxy.InterceptorChain;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;
import org.codejive.webreplay.matching.RequestMatcher;
import org.codejive.webreplay.storage.RequestResponseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core interceptor that implements recording and replay functionality.
 *
 * <p>Behavior depends on the configured {@link ReplayMode}:
 *
 * <ul>
 *   <li>{@link ReplayMode#RECORD}: Always passes requests through and records all exchanges
 *   <li>{@link ReplayMode#CACHE}: Returns cached responses when available, otherwise passes through
 *       and records
 *   <li>{@link ReplayMode#REPLAY}: Returns cached responses when available, otherwise returns 404
 * </ul>
 *
 * <p>Thread-safe for concurrent request handling.
 */
public class RecordingInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(RecordingInterceptor.class);

    private final RequestResponseStore store;
    private final RequestMatcher matcher;
    private volatile ReplayMode mode;

    /**
     * Creates a new recording interceptor.
     *
     * @param store the storage for recorded exchanges
     * @param matcher the strategy for matching requests
     * @param mode the initial replay mode
     */
    public RecordingInterceptor(
            RequestResponseStore store, RequestMatcher matcher, ReplayMode mode) {
        this.store = store;
        this.matcher = matcher;
        this.mode = mode;
    }

    /**
     * Changes the replay mode.
     *
     * @param mode the new mode
     */
    public void setMode(ReplayMode mode) {
        this.mode = mode;
        logger.info("Replay mode changed to: {}", mode);
    }

    /**
     * Returns the current replay mode.
     *
     * @return the current mode
     */
    public ReplayMode getMode() {
        return mode;
    }

    @Override
    public ProxyResponse intercept(ProxyRequest request, InterceptorChain chain) throws Exception {
        ReplayMode currentMode = mode; // Capture to avoid race conditions

        switch (currentMode) {
            case RECORD:
                return handleRecordMode(request, chain);
            case CACHE:
                return handleCacheMode(request, chain);
            case REPLAY:
                return handleReplayMode(request, chain);
            default:
                throw new IllegalStateException("Unknown replay mode: " + currentMode);
        }
    }

    private ProxyResponse handleRecordMode(ProxyRequest request, InterceptorChain chain)
            throws Exception {
        logger.debug("RECORD mode: Passing through request and recording: {}", request.uri());

        // Always pass through in record mode
        ProxyResponse response = chain.proceed(request);

        // Record the exchange
        recordExchange(request, response);

        return response;
    }

    private ProxyResponse handleCacheMode(ProxyRequest request, InterceptorChain chain)
            throws Exception {
        // Try to find cached response
        Optional<RecordedExchange> cached = findCachedExchange(request);

        if (cached.isPresent()) {
            logger.debug("CACHE mode: Returning cached response for: {}", request.uri());
            return cached.get().response();
        }

        // Cache miss - pass through and record
        logger.debug("CACHE mode: Cache miss, passing through and recording: {}", request.uri());
        ProxyResponse response = chain.proceed(request);
        recordExchange(request, response);

        return response;
    }

    private ProxyResponse handleReplayMode(ProxyRequest request, InterceptorChain chain)
            throws Exception {
        // Try to find cached response
        Optional<RecordedExchange> cached = findCachedExchange(request);

        if (cached.isPresent()) {
            logger.debug("REPLAY mode: Returning cached response for: {}", request.uri());
            return cached.get().response();
        }

        // No cached response - return 404 in replay mode
        logger.debug("REPLAY mode: No cached response, returning 404 for: {}", request.uri());
        return new ProxyResponse(
                404, Headers.of("Content-Type", "text/plain"), "Recording not found".getBytes());
    }

    private Optional<RecordedExchange> findCachedExchange(ProxyRequest request) {
        try {
            return store.find(request, matcher);
        } catch (IOException e) {
            logger.error("Error searching storage for request: " + request.uri(), e);
            return Optional.empty();
        }
    }

    private void recordExchange(ProxyRequest request, ProxyResponse response) {
        try {
            RecordedExchange exchange = new RecordedExchange(request, response);
            store.save(exchange);
            logger.debug("Recorded exchange: {} -> {}", request.uri(), response.statusCode());
        } catch (IOException e) {
            logger.error("Error saving recorded exchange: " + request.uri(), e);
        }
    }
}
