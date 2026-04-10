# Java Web Replay

A library for recording and replaying web traffic for testing purposes. Built on top of [java-tproxy](https://github.com/codejive/java-tproxy), it provides a simple way to record HTTP/HTTPS traffic and replay it later for deterministic testing.

## Features

- **Three operational modes**: Record, Cache, and Replay
- **HTTP/HTTPS support**: Automatic HTTPS interception for recording encrypted traffic
- **Pluggable storage**: In-memory or file-based persistence
- **Flexible request matching**: Configurable matching strategies (method + URI, headers, body)
- **Custom file naming**: Pluggable naming strategy for stored recordings
- **Thread-safe**: Handles concurrent requests safely
- **Minimal dependencies**: Lightweight with only essential dependencies
- **Java 21+**: Modern Java features and APIs

## Modes

- **RECORD**: All traffic passes through to the actual server and is recorded. Perfect for creating test fixtures from real API responses.
- **CACHE**: Returns cached responses when available, otherwise passes through and records. Ideal for speeding up tests while allowing new endpoints to be recorded.
- **REPLAY**: Only returns cached responses; non-matching requests return 404. Great for fully isolated tests that don't require network access.

## Installation

Add as a dependency to your project:

```xml
<dependency>
    <groupId>org.codejive.webreplay</groupId>
    <artifactId>java-webreplay</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Quick Start

### Basic Recording and Replay

```java
import org.codejive.webreplay.WebReplayProxy;
import org.codejive.webreplay.ReplayMode;
import java.nio.file.Path;

// Start in RECORD mode to capture traffic
WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.RECORD)
    .storageDirectory(Path.of("recordings"))
    .build();
proxy.start(8080);

// Configure your HTTP client to use the proxy on port 8080
// Make requests to your API - they will be recorded

// Later, switch to REPLAY mode for testing
proxy.setMode(ReplayMode.REPLAY);

// Now requests will use cached responses without hitting the real server
```

### In-Memory Storage (for testing)

```java
WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.CACHE)
    .inMemoryStorage()  // No disk persistence
    .build();
proxy.start(8080);
```

## Usage Examples

### RECORD Mode: Capture Real API Responses

```java
import org.codejive.webreplay.WebReplayProxy;
import org.codejive.webreplay.ReplayMode;
import java.nio.file.Path;

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.RECORD)
    .storageDirectory(Path.of("test/recordings"))
    .build();

proxy.start(8080);

// Configure your application or test to use proxy
// All HTTP traffic will be recorded to disk

// When done
proxy.stop();
```

Recordings are saved as JSON files with names like:
```
example.com_443_GET_api-users_a3f8b9.json
api.service.com_80_POST_data_create_7e2f45.json
```

### CACHE Mode: Speed Up Tests

```java
WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.CACHE)
    .storageDirectory(Path.of("test/recordings"))
    .build();

proxy.start(8080);

// First request: cache miss -> hits real server and records
// makeRequest("http://api.example.com/users");

// Second request: cache hit -> returns cached response instantly
// makeRequest("http://api.example.com/users");
```

### REPLAY Mode: Fully Isolated Tests

```java
// Perfect for CI/CD environments with no external network access
WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.REPLAY)
    .storageDirectory(Path.of("test/recordings"))
    .build();

proxy.start(8080);

// Only cached requests succeed
// Non-cached requests return 404 "Recording not found"
```

### Dynamic Mode Switching

```java
WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.RECORD)
    .storageDirectory(Path.of("recordings"))
    .build();

proxy.start(8080);

// Record some traffic
// ...

// Switch to replay mode
proxy.setMode(ReplayMode.REPLAY);

// Now uses cached responses only
```

## Advanced Configuration

### Custom Request Matching

By default, requests are matched on **method + URI only**. You can customize this:

```java
import org.codejive.webreplay.matching.DefaultRequestMatcher;

// Match requests including specific headers
RequestMatcher matcher = DefaultRequestMatcher.builder()
    .matchHeader("Authorization")
    .matchHeader("Content-Type")
    .build();

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.CACHE)
    .matcher(matcher)
    .storageDirectory(Path.of("recordings"))
    .build();
```

Match request bodies:

```java
RequestMatcher matcher = DefaultRequestMatcher.builder()
    .matchBody()  // Include body in matching
    .build();
```

Create a custom matcher:

```java
RequestMatcher customMatcher = (request1, request2) -> {
    // Your custom matching logic
    return request1.method().equals(request2.method())
        && request1.uri().getPath().equals(request2.uri().getPath());
};

WebReplayProxy proxy = WebReplayProxy.builder()
    .matcher(customMatcher)
    .storageDirectory(Path.of("recordings"))
    .build();
```

### Custom File Naming Strategy

```java
import org.codejive.webreplay.storage.FileNamingStrategy;

// Create a custom naming strategy
FileNamingStrategy customNaming = request -> {
    String method = request.method();
    String path = request.uri().getPath().replace("/", "_");
    return String.format("%s%s.json", method, path);
};

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.RECORD)
    .storageDirectory(Path.of("recordings"))
    .namingStrategy(customNaming)
    .build();
```

### Custom Storage Implementation

```java
import org.codejive.webreplay.storage.RequestResponseStore;

// Implement your own storage (e.g., database, Redis, S3)
RequestResponseStore customStore = new MyDatabaseStore();

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.CACHE)
    .store(customStore)
    .build();
```

### HTTPS Certificate Management

```java
import java.nio.file.Path;

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.RECORD)
    .storageDirectory(Path.of("recordings"))
    .caStorageDirectory(Path.of("certs"))  // Custom CA cert location
    .build();
```

The proxy automatically generates a CA certificate for HTTPS interception. Import `tproxy-ca.crt` into your browser or trust store to avoid certificate warnings.

## Integration with Testing Frameworks

### JUnit 5 Example

```java
import org.junit.jupiter.api.*;
import java.nio.file.Path;

class MyApiTest {
    private WebReplayProxy proxy;

    @BeforeAll
    static void recordFixtures() throws Exception {
        // One-time recording of test fixtures
        WebReplayProxy recorder = WebReplayProxy.builder()
            .mode(ReplayMode.RECORD)
            .storageDirectory(Path.of("src/test/resources/recordings"))
            .build();
        recorder.start(8080);

        // Make real API calls to record
        // ...

        recorder.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        proxy = WebReplayProxy.builder()
            .mode(ReplayMode.REPLAY)
            .storageDirectory(Path.of("src/test/resources/recordings"))
            .build();
        proxy.start(8080);
    }

    @AfterEach
    void tearDown() {
        proxy.stop();
    }

    @Test
    void testWithReplayedResponses() {
        // Your tests here - all HTTP calls use cached responses
        // No actual network requests are made
    }
}
```

### Testing with In-Memory Storage

```java
@Test
void testWithTemporaryRecordings() throws Exception {
    WebReplayProxy proxy = WebReplayProxy.builder()
        .mode(ReplayMode.CACHE)
        .inMemoryStorage()  // Temporary recordings
        .build();
    proxy.start(8080);

    // Make requests - they're cached in memory
    // ...

    // Verify recording count
    assertThat(proxy.getRecordingCount()).isGreaterThan(0);

    // Clear recordings
    proxy.clearRecordings();
    assertThat(proxy.getRecordingCount()).isZero();

    proxy.stop();
}
```

## Working with the Proxy

### Using with HttpClient

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.InetSocketAddress;

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.CACHE)
    .storageDirectory(Path.of("recordings"))
    .build();
proxy.start(8080);

// Configure HttpClient to use the proxy
HttpClient client = HttpClient.newBuilder()
    .proxy(java.net.ProxySelector.of(
        new InetSocketAddress("localhost", 8080)))
    .build();

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .build();

HttpResponse<String> response = client.send(
    request,
    HttpResponse.BodyHandlers.ofString());
```

### Direct Execution (without HttpClient proxy configuration)

```java
import org.codejive.tproxy.ProxyRequest;
import org.codejive.tproxy.ProxyResponse;
import org.codejive.tproxy.Headers;

WebReplayProxy proxy = WebReplayProxy.builder()
    .mode(ReplayMode.REPLAY)
    .storageDirectory(Path.of("recordings"))
    .build();
proxy.start(8080);

// Execute requests directly through the proxy API
ProxyRequest request = new ProxyRequest(
    "GET",
    URI.create("http://api.example.com/users"),
    Headers.of("Accept", "application/json"),
    null
);

ProxyResponse response = proxy.getHttpProxy().execute(request);
System.out.println("Status: " + response.statusCode());
System.out.println("Body: " + new String(response.body()));
```

## API Reference

### WebReplayProxy.Builder

| Method | Description |
|--------|-------------|
| `mode(ReplayMode)` | Set the operational mode (RECORD, CACHE, REPLAY) |
| `storageDirectory(Path)` | Use file-based storage at the specified directory |
| `inMemoryStorage()` | Use in-memory storage (non-persistent) |
| `store(RequestResponseStore)` | Use a custom storage implementation |
| `matcher(RequestMatcher)` | Use a custom request matching strategy |
| `namingStrategy(FileNamingStrategy)` | Use a custom file naming strategy |
| `caStorageDirectory(Path)` | Set directory for CA certificate storage |
| `build()` | Build the WebReplayProxy instance |

### WebReplayProxy Methods

| Method | Description |
|--------|-------------|
| `start(int port)` | Start the proxy on the specified port |
| `stop()` | Stop the proxy |
| `setMode(ReplayMode)` | Change the operational mode |
| `getMode()` | Get the current mode |
| `clearRecordings()` | Clear all recorded exchanges |
| `getRecordingCount()` | Get the number of recorded exchanges |
| `getHttpProxy()` | Get the underlying HttpProxy instance |
| `getStore()` | Get the storage instance |

### ReplayMode Enum

- `RECORD` - Pass through and record all traffic
- `CACHE` - Return cached responses when available, otherwise pass through and record
- `REPLAY` - Return only cached responses, 404 for non-cached

## Storage Format

Recordings are stored as JSON files with the following structure:

```json
{
  "request": {
    "method": "GET",
    "uri": "https://api.example.com/users/123",
    "headers": {
      "Accept": ["application/json"],
      "User-Agent": ["MyApp/1.0"]
    },
    "body": "base64-encoded-body"
  },
  "response": {
    "statusCode": 200,
    "headers": {
      "Content-Type": ["application/json"],
      "Cache-Control": ["no-cache"]
    },
    "body": "base64-encoded-body"
  },
  "recordedAt": {
    "epochSecond": 1712743200,
    "nano": 123456789
  }
}
```

## Building

Build the project:

```bash
./mvnw clean install
```

Run tests:

```bash
./mvnw test
```

Format code:

```bash
./mvnw spotless:apply
```

## Requirements

- Java 21 or higher
- Maven 3.6 or higher

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
