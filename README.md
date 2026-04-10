# Java Web Replay

A library for recording and replaying web traffic for testing purposes.
It functions as a caching proxy but with two specific modes of operation:

- **Record**: in this mode all web traffic that flows through the proxy works as normal and at the same time all requests and responses will be recorded on disk.
- **Cache**: this mode works like Record, except that any requests that have previously been handled and stored on disk will not be passed on but the recorded response will be returned immediately.
- **Replay**: this mode is similar to Cache but any requests that can not be matched to a previously cached response will result in a 404 error to be returned.

## Building

```bash
./mvnw clean install
```

## Usage

Add as a dependency to your project:

```xml
<dependency>
    <groupId>org.codejive.webreplay</groupId>
    <artifactId>java-webreplay</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```
