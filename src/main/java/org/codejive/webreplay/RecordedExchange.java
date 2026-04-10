package org.codejive.webreplay;

import java.time.Instant;
import java.util.Objects;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;

/**
 * Immutable record of an HTTP request/response exchange. Contains the original request, the
 * corresponding response, and metadata about when the exchange was recorded.
 */
public class RecordedExchange {
    private final ProxyRequest request;
    private final ProxyResponse response;
    private final Instant recordedAt;

    /**
     * Creates a new recorded exchange.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param recordedAt the timestamp when this exchange was recorded
     */
    public RecordedExchange(ProxyRequest request, ProxyResponse response, Instant recordedAt) {
        this.request = Objects.requireNonNull(request, "request cannot be null");
        this.response = Objects.requireNonNull(response, "response cannot be null");
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt cannot be null");
    }

    /**
     * Creates a new recorded exchange with the current timestamp.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     */
    public RecordedExchange(ProxyRequest request, ProxyResponse response) {
        this(request, response, Instant.now());
    }

    /**
     * Returns the recorded request.
     *
     * @return the request
     */
    public ProxyRequest request() {
        return request;
    }

    /**
     * Returns the recorded response.
     *
     * @return the response
     */
    public ProxyResponse response() {
        return response;
    }

    /**
     * Returns the timestamp when this exchange was recorded.
     *
     * @return the recording timestamp
     */
    public Instant recordedAt() {
        return recordedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordedExchange that = (RecordedExchange) o;
        return Objects.equals(request, that.request)
                && Objects.equals(response, that.response)
                && Objects.equals(recordedAt, that.recordedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(request, response, recordedAt);
    }

    @Override
    public String toString() {
        return "RecordedExchange{"
                + "request="
                + request
                + ", response="
                + response
                + ", recordedAt="
                + recordedAt
                + '}';
    }
}
