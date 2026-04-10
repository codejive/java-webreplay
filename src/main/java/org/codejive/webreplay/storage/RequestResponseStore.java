package org.codejive.webreplay.storage;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.webreplay.RecordedExchange;
import org.codejive.webreplay.matching.RequestMatcher;

/**
 * Storage abstraction for recorded HTTP request/response exchanges.
 *
 * <p>Implementations may store exchanges in memory, on disk, in a database, or any other storage
 * mechanism.
 */
public interface RequestResponseStore {
    /**
     * Saves a recorded exchange to storage.
     *
     * @param exchange the exchange to save
     * @throws IOException if an I/O error occurs during save
     */
    void save(RecordedExchange exchange) throws IOException;

    /**
     * Finds a recorded exchange that matches the given request according to the provided matcher.
     *
     * @param request the request to match
     * @param matcher the matching strategy
     * @return an Optional containing the matching exchange, or empty if no match found
     * @throws IOException if an I/O error occurs during search
     */
    Optional<RecordedExchange> find(ProxyRequest request, RequestMatcher matcher)
            throws IOException;

    /**
     * Returns all recorded exchanges in storage.
     *
     * @return a list of all recorded exchanges
     * @throws IOException if an I/O error occurs during retrieval
     */
    List<RecordedExchange> listAll() throws IOException;

    /**
     * Clears all recorded exchanges from storage.
     *
     * @throws IOException if an I/O error occurs during clear
     */
    void clear() throws IOException;

    /**
     * Returns the number of recorded exchanges in storage.
     *
     * @return the count of recorded exchanges
     * @throws IOException if an I/O error occurs during counting
     */
    int count() throws IOException;
}
