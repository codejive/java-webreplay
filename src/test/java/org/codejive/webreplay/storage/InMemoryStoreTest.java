package org.codejive.webreplay.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.codejive.tproxy.Headers;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;
import org.codejive.webreplay.RecordedExchange;
import org.codejive.webreplay.matching.DefaultRequestMatcher;
import org.codejive.webreplay.matching.RequestMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryStore Tests")
class InMemoryStoreTest {

    private InMemoryStore store;
    private RequestMatcher matcher;

    @BeforeEach
    void setUp() {
        store = new InMemoryStore();
        matcher = DefaultRequestMatcher.methodAndUri();
    }

    @Test
    @DisplayName("Should start empty")
    void shouldStartEmpty() throws Exception {
        assertThat(store.count()).isZero();
        assertThat(store.listAll()).isEmpty();
    }

    @Test
    @DisplayName("Should save and retrieve exchange")
    void shouldSaveAndRetrieve() throws Exception {
        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyResponse response =
                ProxyResponse.fromBytes(
                        200, Headers.of("Content-Type", "application/json"), "{}".getBytes());
        RecordedExchange exchange = new RecordedExchange(request, response);

        store.save(exchange);

        assertThat(store.count()).isEqualTo(1);
        Optional<RecordedExchange> found = store.find(request, matcher);
        assertThat(found).isPresent();
        assertThat(found.get().request()).isEqualTo(request);
        assertThat(found.get().response().statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should not find non-matching request")
    void shouldNotFindNonMatching() throws Exception {
        ProxyRequest request1 =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyResponse response =
                ProxyResponse.fromBytes(
                        200, Headers.of("Content-Type", "application/json"), "{}".getBytes());
        store.save(new RecordedExchange(request1, response));

        ProxyRequest request2 =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/other"), Headers.empty(), null);
        Optional<RecordedExchange> found = store.find(request2, matcher);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should list all exchanges")
    void shouldListAll() throws Exception {
        ProxyRequest request1 =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api1"), Headers.empty(), null);
        ProxyRequest request2 =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api2"), Headers.empty(), null);
        ProxyResponse response = ProxyResponse.fromBytes(200, Headers.empty(), new byte[0]);

        store.save(new RecordedExchange(request1, response));
        store.save(new RecordedExchange(request2, response));

        List<RecordedExchange> all = store.listAll();
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("Should clear all exchanges")
    void shouldClear() throws Exception {
        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyResponse response = ProxyResponse.fromBytes(200, Headers.empty(), new byte[0]);
        store.save(new RecordedExchange(request, response));

        assertThat(store.count()).isEqualTo(1);

        store.clear();

        assertThat(store.count()).isZero();
        assertThat(store.listAll()).isEmpty();
    }

    @Test
    @DisplayName("Should handle multiple saves of same request")
    void shouldHandleMultipleSaves() throws Exception {
        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyResponse response1 =
                ProxyResponse.fromBytes(200, Headers.empty(), "response1".getBytes());
        ProxyResponse response2 =
                ProxyResponse.fromBytes(200, Headers.empty(), "response2".getBytes());

        store.save(new RecordedExchange(request, response1, Instant.now()));
        store.save(new RecordedExchange(request, response2, Instant.now()));

        // Should find the first matching exchange
        Optional<RecordedExchange> found = store.find(request, matcher);
        assertThat(found).isPresent();
        assertThat(store.count()).isEqualTo(2);
    }
}
