package org.codejive.webreplay.storage;

import org.codejive.tproxy.ProxyRequest;

/**
 * Strategy for generating filenames from HTTP requests for file-based storage.
 *
 * <p>Implementations should generate deterministic, filesystem-safe filenames that are unique for
 * different requests but consistent for equivalent requests.
 */
@FunctionalInterface
public interface FileNamingStrategy {
    /**
     * Generates a filename for storing the given request's exchange.
     *
     * @param request the HTTP request
     * @return a filesystem-safe filename (without directory path)
     */
    String generateFilename(ProxyRequest request);
}
