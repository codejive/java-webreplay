package org.codejive.webreplay.storage;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.codejive.tproxy.ProxyRequest;
import org.codejive.webreplay.RecordedExchange;
import org.codejive.webreplay.matching.RequestMatcher;

/**
 * File system-based storage implementation for recorded exchanges.
 *
 * <p>This implementation stores each exchange as a separate JSON file in a flat directory
 * structure. Filenames are generated using a configurable {@link FileNamingStrategy}.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>Persistent storage on disk
 *   <li>Human-readable JSON format
 *   <li>Pluggable filename generation strategy
 *   <li>Atomic file writes to prevent corruption
 * </ul>
 */
public class FileSystemStore implements RequestResponseStore {
    private final Path storageDirectory;
    private final FileNamingStrategy namingStrategy;

    /**
     * Creates a new FileSystemStore with default naming strategy.
     *
     * @param storageDirectory the directory where recordings will be stored
     * @throws IOException if the directory cannot be created
     */
    public FileSystemStore(Path storageDirectory) throws IOException {
        this(storageDirectory, new DefaultFileNamingStrategy());
    }

    /**
     * Creates a new FileSystemStore with custom naming strategy.
     *
     * @param storageDirectory the directory where recordings will be stored
     * @param namingStrategy the strategy for generating filenames
     * @throws IOException if the directory cannot be created
     */
    public FileSystemStore(Path storageDirectory, FileNamingStrategy namingStrategy)
            throws IOException {
        this.storageDirectory = storageDirectory;
        this.namingStrategy = namingStrategy;
        ensureDirectoryExists();
    }

    @Override
    public void save(RecordedExchange exchange) throws IOException {
        String filename = namingStrategy.generateFilename(exchange.request());
        Path filePath = storageDirectory.resolve(filename);

        String json = ExchangeJsonSerializer.toJson(exchange);

        // Write atomically using temp file and move
        Path tempFile = Files.createTempFile(storageDirectory, ".tmp", ".json");
        try {
            Files.writeString(
                    tempFile,
                    json,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tempFile, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // Clean up temp file if move failed
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    @Override
    public Optional<RecordedExchange> find(ProxyRequest request, RequestMatcher matcher)
            throws IOException {
        // First try direct lookup by filename
        String filename = namingStrategy.generateFilename(request);
        Path filePath = storageDirectory.resolve(filename);

        if (Files.exists(filePath)) {
            RecordedExchange exchange = loadExchange(filePath);
            if (matcher.matches(request, exchange.request())) {
                return Optional.of(exchange);
            }
        }

        // If direct lookup fails, scan all files (slower but handles hash collisions)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.json")) {
            for (Path path : stream) {
                if (path.equals(filePath)) {
                    continue; // Already checked above
                }
                try {
                    RecordedExchange exchange = loadExchange(path);
                    if (matcher.matches(request, exchange.request())) {
                        return Optional.of(exchange);
                    }
                } catch (IOException e) {
                    // Skip corrupted files
                    continue;
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<RecordedExchange> listAll() throws IOException {
        List<RecordedExchange> exchanges = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.json")) {
            for (Path path : stream) {
                try {
                    exchanges.add(loadExchange(path));
                } catch (IOException e) {
                    // Skip corrupted files
                    continue;
                }
            }
        }
        return exchanges;
    }

    @Override
    public void clear() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.json")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
    }

    @Override
    public int count() throws IOException {
        int count = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.json")) {
            for (Path ignored : stream) {
                count++;
            }
        }
        return count;
    }

    private RecordedExchange loadExchange(Path filePath) throws IOException {
        String json = Files.readString(filePath);
        return ExchangeJsonSerializer.fromJson(json);
    }

    private void ensureDirectoryExists() throws IOException {
        if (!Files.exists(storageDirectory)) {
            Files.createDirectories(storageDirectory);
        }
        if (!Files.isDirectory(storageDirectory)) {
            throw new IOException(
                    "Storage path exists but is not a directory: " + storageDirectory);
        }
    }
}
