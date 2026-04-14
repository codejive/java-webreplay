package org.codejive.webreplay.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.codejive.tproxy.Headers;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;
import org.codejive.webreplay.RecordedExchange;
import org.codejive.webreplay.matching.DefaultRequestMatcher;
import org.codejive.webreplay.matching.RequestMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("FileSystemStore Tests")
class FileSystemStoreTest {

    @TempDir Path tempDir;

    private FileSystemStore store;
    private RequestMatcher matcher;

    @BeforeEach
    void setUp() throws IOException {
        store = new FileSystemStore(tempDir);
        matcher = DefaultRequestMatcher.methodAndUri();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (store != null) {
            store.clear();
        }
    }

    @Test
    @DisplayName("Should create storage directory")
    void shouldCreateDirectory() {
        assertThat(Files.exists(tempDir)).isTrue();
        assertThat(Files.isDirectory(tempDir)).isTrue();
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

        // Verify file was created
        assertThat(store.count()).isEqualTo(1);

        // Verify can retrieve
        Optional<RecordedExchange> found = store.find(request, matcher);
        assertThat(found).isPresent();
        assertThat(found.get().request().method()).isEqualTo("GET");
        assertThat(found.get().request().uri()).isEqualTo(request.uri());
        assertThat(found.get().response().statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Should persist across store instances")
    void shouldPersist() throws Exception {
        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyResponse response = ProxyResponse.fromBytes(200, Headers.empty(), "test".getBytes());

        store.save(new RecordedExchange(request, response));

        // Create new store pointing to same directory
        FileSystemStore newStore = new FileSystemStore(tempDir);
        Optional<RecordedExchange> found = newStore.find(request, matcher);

        assertThat(found).isPresent();
        assertThat(new String(found.get().response().body())).isEqualTo("test");
    }

    @Test
    @DisplayName("Should handle custom naming strategy")
    void shouldUseCustomNaming() throws Exception {
        FileNamingStrategy customStrategy = request -> "custom-name.json";
        FileSystemStore customStore = new FileSystemStore(tempDir, customStrategy);

        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyResponse response = ProxyResponse.fromBytes(200, Headers.empty(), new byte[0]);

        customStore.save(new RecordedExchange(request, response));

        // Verify file exists with custom name
        Path expectedFile = tempDir.resolve("custom-name.json");
        assertThat(Files.exists(expectedFile)).isTrue();
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
    @DisplayName("Should clear all files")
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
    @DisplayName("Should preserve headers and body")
    void shouldPreserveData() throws Exception {
        ProxyRequest request =
                ProxyRequest.fromBytes(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.of(
                                "Content-Type",
                                "application/json",
                                "Authorization",
                                "Bearer token"),
                        "{\"key\":\"value\"}".getBytes());
        ProxyResponse response =
                ProxyResponse.fromBytes(
                        201, Headers.of("Location", "/resource/123"), "{\"id\":123}".getBytes());

        store.save(new RecordedExchange(request, response));

        Optional<RecordedExchange> found = store.find(request, matcher);
        assertThat(found).isPresent();

        RecordedExchange loaded = found.get();
        assertThat(loaded.request().headers().first("Content-Type")).isEqualTo("application/json");
        assertThat(loaded.request().headers().first("Authorization")).isEqualTo("Bearer token");
        assertThat(new String(loaded.request().body())).isEqualTo("{\"key\":\"value\"}");
        assertThat(loaded.response().statusCode()).isEqualTo(201);
        assertThat(loaded.response().headers().first("Location")).isEqualTo("/resource/123");
        assertThat(new String(loaded.response().body())).isEqualTo("{\"id\":123}");
    }
}
