package org.codejive.webreplay.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.webreplay.RecordedExchange;
import org.codejive.webreplay.matching.RequestMatcher;

/**
 * In-memory storage implementation for recorded exchanges.
 *
 * <p>This implementation stores exchanges in memory only and does not persist them to disk. It is
 * suitable for:
 *
 * <ul>
 *   <li>Testing scenarios where persistence is not needed
 *   <li>Temporary recording during a single session
 *   <li>Short-lived processes where disk I/O should be avoided
 * </ul>
 *
 * <p>Thread-safe for concurrent access using a {@link CopyOnWriteArrayList}.
 */
public class InMemoryStore implements RequestResponseStore {
    private final List<RecordedExchange> exchanges = new CopyOnWriteArrayList<>();

    @Override
    public void save(RecordedExchange exchange) {
        exchanges.add(exchange);
    }

    @Override
    public Optional<RecordedExchange> find(ProxyRequest request, RequestMatcher matcher) {
        return exchanges.stream()
                .filter(exchange -> matcher.matches(request, exchange.request()))
                .findFirst();
    }

    @Override
    public List<RecordedExchange> listAll() {
        return new ArrayList<>(exchanges);
    }

    @Override
    public void clear() {
        exchanges.clear();
    }

    @Override
    public int count() {
        return exchanges.size();
    }
}
