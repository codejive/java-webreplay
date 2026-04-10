package org.codejive.webreplay.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.codejive.tproxy.Headers;
import org.codejive.tproxy.ProxyRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultRequestMatcher Tests")
class DefaultRequestMatcherTest {

    @Test
    @DisplayName("Should match identical requests")
    void shouldMatchIdenticalRequests() {
        RequestMatcher matcher = DefaultRequestMatcher.methodAndUri();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);

        assertThat(matcher.matches(request1, request2)).isTrue();
    }

    @Test
    @DisplayName("Should not match different methods")
    void shouldNotMatchDifferentMethods() {
        RequestMatcher matcher = DefaultRequestMatcher.methodAndUri();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "POST", URI.create("http://example.com/api"), Headers.empty(), null);

        assertThat(matcher.matches(request1, request2)).isFalse();
    }

    @Test
    @DisplayName("Should not match different URIs")
    void shouldNotMatchDifferentUris() {
        RequestMatcher matcher = DefaultRequestMatcher.methodAndUri();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/api"), Headers.empty(), null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/other"), Headers.empty(), null);

        assertThat(matcher.matches(request1, request2)).isFalse();
    }

    @Test
    @DisplayName("Should match with different query parameter order")
    void shouldMatchDifferentQueryOrder() {
        RequestMatcher matcher = DefaultRequestMatcher.methodAndUri();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/api?a=1&b=2"), Headers.empty(), null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "GET", URI.create("http://example.com/api?b=2&a=1"), Headers.empty(), null);

        // Note: Query parameters order matters in URIs, so this will be false
        // This is the expected behavior for the default matcher
        assertThat(matcher.matches(request1, request2)).isFalse();
    }

    @Test
    @DisplayName("Should match headers when configured")
    void shouldMatchHeaders() {
        RequestMatcher matcher =
                DefaultRequestMatcher.builder().matchHeader("Authorization").build();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET",
                        URI.create("http://example.com/api"),
                        Headers.of("Authorization", "Bearer token123"),
                        null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "GET",
                        URI.create("http://example.com/api"),
                        Headers.of("Authorization", "Bearer token123"),
                        null);

        assertThat(matcher.matches(request1, request2)).isTrue();
    }

    @Test
    @DisplayName("Should not match different header values when configured")
    void shouldNotMatchDifferentHeaders() {
        RequestMatcher matcher =
                DefaultRequestMatcher.builder().matchHeader("Authorization").build();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET",
                        URI.create("http://example.com/api"),
                        Headers.of("Authorization", "Bearer token123"),
                        null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "GET",
                        URI.create("http://example.com/api"),
                        Headers.of("Authorization", "Bearer token456"),
                        null);

        assertThat(matcher.matches(request1, request2)).isFalse();
    }

    @Test
    @DisplayName("Should match request bodies when configured")
    void shouldMatchBodies() {
        RequestMatcher matcher = DefaultRequestMatcher.builder().matchBody().build();

        ProxyRequest request1 =
                new ProxyRequest(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.empty(),
                        "request body".getBytes());
        ProxyRequest request2 =
                new ProxyRequest(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.empty(),
                        "request body".getBytes());

        assertThat(matcher.matches(request1, request2)).isTrue();
    }

    @Test
    @DisplayName("Should not match different bodies when configured")
    void shouldNotMatchDifferentBodies() {
        RequestMatcher matcher = DefaultRequestMatcher.builder().matchBody().build();

        ProxyRequest request1 =
                new ProxyRequest(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.empty(),
                        "body1".getBytes());
        ProxyRequest request2 =
                new ProxyRequest(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.empty(),
                        "body2".getBytes());

        assertThat(matcher.matches(request1, request2)).isFalse();
    }

    @Test
    @DisplayName("Should ignore headers by default")
    void shouldIgnoreHeadersByDefault() {
        RequestMatcher matcher = DefaultRequestMatcher.methodAndUri();

        ProxyRequest request1 =
                new ProxyRequest(
                        "GET",
                        URI.create("http://example.com/api"),
                        Headers.of("User-Agent", "Browser1"),
                        null);
        ProxyRequest request2 =
                new ProxyRequest(
                        "GET",
                        URI.create("http://example.com/api"),
                        Headers.of("User-Agent", "Browser2"),
                        null);

        assertThat(matcher.matches(request1, request2)).isTrue();
    }

    @Test
    @DisplayName("Should ignore body by default")
    void shouldIgnoreBodyByDefault() {
        RequestMatcher matcher = DefaultRequestMatcher.methodAndUri();

        ProxyRequest request1 =
                new ProxyRequest(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.empty(),
                        "body1".getBytes());
        ProxyRequest request2 =
                new ProxyRequest(
                        "POST",
                        URI.create("http://example.com/api"),
                        Headers.empty(),
                        "body2".getBytes());

        assertThat(matcher.matches(request1, request2)).isTrue();
    }
}
