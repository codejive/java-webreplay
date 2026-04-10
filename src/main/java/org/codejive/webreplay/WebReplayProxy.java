package org.codejive.webreplay;

import java.io.IOException;
import java.nio.file.Path;
import org.codejive.tproxy.HttpProxy;
import org.codejive.webreplay.matching.DefaultRequestMatcher;
import org.codejive.webreplay.matching.RequestMatcher;
import org.codejive.webreplay.storage.DefaultFileNamingStrategy;
import org.codejive.webreplay.storage.FileNamingStrategy;
import org.codejive.webreplay.storage.FileSystemStore;
import org.codejive.webreplay.storage.InMemoryStore;
import org.codejive.webreplay.storage.RequestResponseStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level API for recording and replaying web traffic.
 *
 * <p>This class wraps {@link HttpProxy} and provides three operational modes:
 *
 * <ul>
 *   <li><b>RECORD</b>: All traffic passes through and is recorded
 *   <li><b>CACHE</b>: Cached responses are returned when available, otherwise traffic passes
 *       through and is recorded
 *   <li><b>REPLAY</b>: Only cached responses are returned; non-matching requests return 404
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // File-based storage
 * WebReplayProxy proxy = WebReplayProxy.builder()
 *     .mode(ReplayMode.RECORD)
 *     .storageDirectory(Path.of("recordings"))
 *     .build();
 * proxy.start(8080);
 *
 * // In-memory storage
 * WebReplayProxy proxy = WebReplayProxy.builder()
 *     .mode(ReplayMode.CACHE)
 *     .inMemoryStorage()
 *     .build();
 * proxy.start(8080);
 * }</pre>
 */
public class WebReplayProxy {
    private static final Logger logger = LoggerFactory.getLogger(WebReplayProxy.class);

    private final HttpProxy httpProxy;
    private final RecordingInterceptor recordingInterceptor;
    private final RequestResponseStore store;
    private boolean running = false;

    private WebReplayProxy(Builder builder) throws IOException {
        // Create storage
        this.store = createStore(builder);

        // Create matcher
        RequestMatcher matcher =
                builder.matcher != null ? builder.matcher : DefaultRequestMatcher.methodAndUri();

        // Create recording interceptor
        this.recordingInterceptor = new RecordingInterceptor(store, matcher, builder.mode);

        // Create and configure HTTP proxy
        this.httpProxy = new HttpProxy();
        this.httpProxy.addInterceptor(recordingInterceptor);

        // Enable HTTPS interception for RECORD and CACHE modes
        if (builder.mode == ReplayMode.RECORD || builder.mode == ReplayMode.CACHE) {
            this.httpProxy.enableHttpsInterception();
            logger.info("HTTPS interception enabled for {} mode", builder.mode);
        }

        if (builder.caStorageDir != null) {
            this.httpProxy.caStorageDir(builder.caStorageDir);
        }
    }

    /**
     * Creates a new builder for configuring a WebReplayProxy.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Starts the proxy on the specified port.
     *
     * @param port the port to listen on
     * @throws IOException if the proxy cannot be started
     */
    public void start(int port) throws IOException {
        httpProxy.start(port);
        running = true;
        logger.info("WebReplayProxy started on port {} in {} mode", port, getMode());
    }

    /** Stops the proxy. */
    public void stop() {
        httpProxy.stop();
        running = false;
        logger.info("WebReplayProxy stopped");
    }

    /**
     * Returns whether the proxy is currently running.
     *
     * @return true if running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Changes the replay mode.
     *
     * @param mode the new mode
     */
    public void setMode(ReplayMode mode) {
        recordingInterceptor.setMode(mode);
    }

    /**
     * Returns the current replay mode.
     *
     * @return the current mode
     */
    public ReplayMode getMode() {
        return recordingInterceptor.getMode();
    }

    /**
     * Clears all recorded exchanges from storage.
     *
     * @throws IOException if an error occurs during clearing
     */
    public void clearRecordings() throws IOException {
        store.clear();
        logger.info("Cleared all recordings");
    }

    /**
     * Returns the number of recorded exchanges in storage.
     *
     * @return the count of recorded exchanges
     * @throws IOException if an error occurs during counting
     */
    public int getRecordingCount() throws IOException {
        return store.count();
    }

    /**
     * Returns the underlying HTTP proxy for advanced configuration.
     *
     * @return the HTTP proxy
     */
    public HttpProxy getHttpProxy() {
        return httpProxy;
    }

    /**
     * Returns the storage instance.
     *
     * @return the storage
     */
    public RequestResponseStore getStore() {
        return store;
    }

    private static RequestResponseStore createStore(Builder builder) throws IOException {
        if (builder.store != null) {
            return builder.store;
        } else if (builder.useInMemoryStorage) {
            return new InMemoryStore();
        } else if (builder.storageDirectory != null) {
            FileNamingStrategy naming =
                    builder.namingStrategy != null
                            ? builder.namingStrategy
                            : new DefaultFileNamingStrategy();
            return new FileSystemStore(builder.storageDirectory, naming);
        } else {
            // Default to in-memory if nothing specified
            return new InMemoryStore();
        }
    }

    /** Builder for creating configured WebReplayProxy instances. */
    public static class Builder {
        private ReplayMode mode = ReplayMode.RECORD;
        private Path storageDirectory;
        private boolean useInMemoryStorage = false;
        private RequestResponseStore store;
        private RequestMatcher matcher;
        private FileNamingStrategy namingStrategy;
        private Path caStorageDir;

        /**
         * Sets the replay mode.
         *
         * @param mode the replay mode
         * @return this builder
         */
        public Builder mode(ReplayMode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets the directory for file-based storage.
         *
         * @param directory the storage directory
         * @return this builder
         */
        public Builder storageDirectory(Path directory) {
            this.storageDirectory = directory;
            this.useInMemoryStorage = false;
            this.store = null;
            return this;
        }

        /**
         * Enables in-memory storage (non-persistent).
         *
         * @return this builder
         */
        public Builder inMemoryStorage() {
            this.useInMemoryStorage = true;
            this.storageDirectory = null;
            this.store = null;
            return this;
        }

        /**
         * Sets a custom storage implementation.
         *
         * @param store the storage implementation
         * @return this builder
         */
        public Builder store(RequestResponseStore store) {
            this.store = store;
            this.storageDirectory = null;
            this.useInMemoryStorage = false;
            return this;
        }

        /**
         * Sets a custom request matcher.
         *
         * @param matcher the request matcher
         * @return this builder
         */
        public Builder matcher(RequestMatcher matcher) {
            this.matcher = matcher;
            return this;
        }

        /**
         * Sets a custom file naming strategy (only used with FileSystemStore).
         *
         * @param strategy the naming strategy
         * @return this builder
         */
        public Builder namingStrategy(FileNamingStrategy strategy) {
            this.namingStrategy = strategy;
            return this;
        }

        /**
         * Sets the directory for CA certificate storage (used for HTTPS interception).
         *
         * @param directory the CA storage directory
         * @return this builder
         */
        public Builder caStorageDirectory(Path directory) {
            this.caStorageDir = directory;
            return this;
        }

        /**
         * Builds the WebReplayProxy.
         *
         * @return a configured WebReplayProxy instance
         * @throws IOException if storage initialization fails
         */
        public WebReplayProxy build() throws IOException {
            return new WebReplayProxy(this);
        }
    }
}
