package org.codejive.webreplay.storage;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.codejive.tproxy.ProxyRequest;

/**
 * Default file naming strategy that generates flat filenames in the format:
 * {host}_{port}_{method}_{path}_{hash}.json
 *
 * <p>Where:
 *
 * <ul>
 *   <li>host: The hostname from the request URI
 *   <li>port: The port number (or default port for scheme)
 *   <li>method: The HTTP method (GET, POST, etc.)
 *   <li>path: The URI path with slashes replaced by hyphens
 *   <li>hash: A short hash of the full URI to handle query parameters and disambiguation
 * </ul>
 *
 * <p>Special characters are sanitized to ensure filesystem compatibility.
 */
public class DefaultFileNamingStrategy implements FileNamingStrategy {

    @Override
    public String generateFilename(ProxyRequest request) {
        URI uri = request.uri();

        String host = sanitize(uri.getHost() != null ? uri.getHost() : "unknown");
        int port = getEffectivePort(uri);
        String method = sanitize(request.method());
        String path = sanitizePath(uri.getPath());
        String hash = generateHash(uri.toString());

        return String.format("%s_%d_%s_%s_%s.json", host, port, method, path, hash);
    }

    private int getEffectivePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        // Return default port based on scheme
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return 443;
        } else if ("http".equalsIgnoreCase(uri.getScheme())) {
            return 80;
        }
        return 0;
    }

    private String sanitize(String str) {
        if (str == null || str.isEmpty()) {
            return "empty";
        }
        // Replace filesystem-unsafe characters with underscores
        return str.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizePath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "root";
        }
        // Remove leading/trailing slashes and replace remaining slashes with hyphens
        String sanitized = path.replaceAll("^/+|/+$", "").replace("/", "-");
        // Sanitize remaining characters
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Limit length to avoid excessively long filenames
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return sanitized.isEmpty() ? "root" : sanitized;
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Return first 8 characters of hex representation
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(4, hashBytes.length); i++) {
                String hex = Integer.toHexString(0xff & hashBytes[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simpler hash if SHA-256 is not available
            return Integer.toHexString(input.hashCode());
        }
    }
}
