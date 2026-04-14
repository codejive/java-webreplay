package org.codejive.webreplay;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.IOException;
import java.net.URI;
import org.codejive.tproxy.Headers;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;
import org.junit.jupiter.api.*;

/**
 * Integration tests for WebReplayProxy using WireMock as a backend server to verify RECORD, CACHE,
 * and REPLAY modes.
 */
@DisplayName("WebReplayProxy Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebReplayProxyIntegrationTest {

    private static final int PROXY_PORT = 9990;
    private static final int BACKEND_PORT = 9991;

    private WebReplayProxy proxy;
    private WireMockServer mockServer;

    @BeforeEach
    void setUp() throws IOException {
        // Start WireMock backend server
        mockServer =
                new WireMockServer(
                        WireMockConfiguration.options()
                                .port(BACKEND_PORT)
                                .bindAddress("127.0.0.1"));
        mockServer.start();

        // Wait for server to be ready
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterEach
    void tearDown() {
        if (proxy != null) {
            proxy.stop();
        }
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("RECORD mode: Should pass through and record requests")
    void testRecordMode() throws Exception {
        // Setup
        proxy = WebReplayProxy.builder().mode(ReplayMode.RECORD).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        mockServer.stubFor(
                get(urlEqualTo("/api/test"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"status\":\"ok\"}")));

        // Execute
        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET",
                        URI.create("http://127.0.0.1:" + BACKEND_PORT + "/api/test"),
                        Headers.of("User-Agent", "TestClient"),
                        null);

        ProxyResponse response = proxy.getHttpProxy().execute(request);

        // Verify
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body())).isEqualTo("{\"status\":\"ok\"}");

        // Verify backend was called
        mockServer.verify(1, getRequestedFor(urlEqualTo("/api/test")));

        // Verify it was recorded
        assertThat(proxy.getRecordingCount()).isEqualTo(1);
    }

    @Test
    @Order(2)
    @DisplayName("CACHE mode: Should return cached response on second request")
    void testCacheMode() throws Exception {
        // Setup
        proxy = WebReplayProxy.builder().mode(ReplayMode.CACHE).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        mockServer.stubFor(
                get(urlEqualTo("/api/cached"))
                        .willReturn(aResponse().withStatus(200).withBody("original response")));

        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET",
                        URI.create("http://127.0.0.1:" + BACKEND_PORT + "/api/cached"),
                        Headers.empty(),
                        null);

        // First request - should hit backend and cache
        ProxyResponse response1 = proxy.getHttpProxy().execute(request);
        assertThat(new String(response1.body())).isEqualTo("original response");
        mockServer.verify(1, getRequestedFor(urlEqualTo("/api/cached")));

        // Change backend response
        mockServer.stubFor(
                get(urlEqualTo("/api/cached"))
                        .willReturn(aResponse().withStatus(200).withBody("modified response")));

        // Second request - should return cached response without hitting backend
        ProxyResponse response2 = proxy.getHttpProxy().execute(request);
        assertThat(new String(response2.body())).isEqualTo("original response"); // Cached response

        // Verify backend was still only called once
        mockServer.verify(1, getRequestedFor(urlEqualTo("/api/cached")));
    }

    @Test
    @Order(3)
    @DisplayName("CACHE mode: Should pass through on cache miss")
    void testCacheModeMiss() throws Exception {
        // Setup
        proxy = WebReplayProxy.builder().mode(ReplayMode.CACHE).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        mockServer.stubFor(
                get(urlEqualTo("/api/new"))
                        .willReturn(aResponse().withStatus(200).withBody("new response")));

        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET",
                        URI.create("http://127.0.0.1:" + BACKEND_PORT + "/api/new"),
                        Headers.empty(),
                        null);

        // Request not in cache - should hit backend
        ProxyResponse response = proxy.getHttpProxy().execute(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body())).isEqualTo("new response");
        mockServer.verify(1, getRequestedFor(urlEqualTo("/api/new")));
    }

    @Test
    @Order(4)
    @DisplayName("REPLAY mode: Should return 404 for non-cached requests")
    void testReplayModeNotFound() throws Exception {
        // Setup
        proxy = WebReplayProxy.builder().mode(ReplayMode.REPLAY).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        mockServer.stubFor(
                get(urlEqualTo("/api/missing"))
                        .willReturn(aResponse().withStatus(200).withBody("should not see this")));

        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET",
                        URI.create("http://127.0.0.1:" + BACKEND_PORT + "/api/missing"),
                        Headers.empty(),
                        null);

        // Request not in cache - should return 404 without hitting backend
        ProxyResponse response = proxy.getHttpProxy().execute(request);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(new String(response.body())).contains("Recording not found");

        // Verify backend was never called
        mockServer.verify(0, getRequestedFor(urlEqualTo("/api/missing")));
    }

    @Test
    @Order(5)
    @DisplayName("REPLAY mode: Should return cached response when available")
    void testReplayModeWithCache() throws Exception {
        // Setup - first record a response
        proxy = WebReplayProxy.builder().mode(ReplayMode.RECORD).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        mockServer.stubFor(
                get(urlEqualTo("/api/replay"))
                        .willReturn(aResponse().withStatus(200).withBody("recorded response")));

        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET",
                        URI.create("http://127.0.0.1:" + BACKEND_PORT + "/api/replay"),
                        Headers.empty(),
                        null);

        // Record the request
        proxy.getHttpProxy().execute(request);
        mockServer.verify(1, getRequestedFor(urlEqualTo("/api/replay")));

        // Switch to REPLAY mode
        proxy.setMode(ReplayMode.REPLAY);

        // Change backend (to prove we're not hitting it)
        mockServer.stubFor(
                get(urlEqualTo("/api/replay"))
                        .willReturn(aResponse().withStatus(200).withBody("different response")));

        // Request again - should get cached response without hitting backend
        ProxyResponse response = proxy.getHttpProxy().execute(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(new String(response.body())).isEqualTo("recorded response");

        // Verify backend was still only called once (during recording)
        mockServer.verify(1, getRequestedFor(urlEqualTo("/api/replay")));
    }

    @Test
    @Order(6)
    @DisplayName("Should switch modes dynamically")
    void testModeSwitching() throws Exception {
        proxy = WebReplayProxy.builder().mode(ReplayMode.RECORD).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        assertThat(proxy.getMode()).isEqualTo(ReplayMode.RECORD);

        proxy.setMode(ReplayMode.CACHE);
        assertThat(proxy.getMode()).isEqualTo(ReplayMode.CACHE);

        proxy.setMode(ReplayMode.REPLAY);
        assertThat(proxy.getMode()).isEqualTo(ReplayMode.REPLAY);
    }

    @Test
    @Order(7)
    @DisplayName("Should clear recordings")
    void testClearRecordings() throws Exception {
        proxy = WebReplayProxy.builder().mode(ReplayMode.RECORD).inMemoryStorage().build();
        proxy.start(PROXY_PORT);

        mockServer.stubFor(
                get(urlEqualTo("/api/clear"))
                        .willReturn(aResponse().withStatus(200).withBody("test")));

        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET",
                        URI.create("http://127.0.0.1:" + BACKEND_PORT + "/api/clear"),
                        Headers.empty(),
                        null);

        proxy.getHttpProxy().execute(request);
        assertThat(proxy.getRecordingCount()).isEqualTo(1);

        proxy.clearRecordings();
        assertThat(proxy.getRecordingCount()).isZero();
    }
}
