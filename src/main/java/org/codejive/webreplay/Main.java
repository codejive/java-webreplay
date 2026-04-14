package org.codejive.webreplay;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main entry point for running the WebReplayProxy as a standalone application.
 *
 * <p>Usage:
 *
 * <pre>
 * java -jar webreplay.jar [options]
 *
 * Options:
 *   -p, --port &lt;port&gt;         Port to run the proxy on (default: 3128)
 *   -d, --dir &lt;directory&gt;     Directory for storing cached requests (default: proxy-cache)
 *   -m, --mode &lt;mode&gt;         Replay mode: RECORD, CACHE, or REPLAY (default: CACHE)
 *   -h, --help                Show this help message
 * </pre>
 */
public class Main {
    private static final int DEFAULT_PORT = 3128;
    private static final String DEFAULT_DIRECTORY = "proxy-cache";
    private static final ReplayMode DEFAULT_MODE = ReplayMode.CACHE;

    public static void main(String[] args) {
        try {
            Config config = parseArgs(args);

            if (config.showHelp) {
                showHelp();
                return;
            }

            startProxy(config);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println();
            showHelp();
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Failed to start proxy: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void startProxy(Config config) throws IOException {
        System.out.println("Starting WebReplayProxy...");
        System.out.println("  Mode: " + config.mode);
        System.out.println("  Port: " + config.port);
        System.out.println("  Storage: " + config.storageDirectory);
        System.out.println();

        WebReplayProxy proxy =
                WebReplayProxy.builder()
                        .mode(config.mode)
                        .storageDirectory(config.storageDirectory)
                        .build();

        proxy.start(config.port);

        System.out.println("Proxy started successfully!");
        System.out.println(
                "Configure your browser or application to use proxy: localhost:" + config.port);
        System.out.println("Press Ctrl+C to stop");

        // Add shutdown hook to gracefully stop the proxy
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    System.out.println("\nStopping proxy...");
                                    proxy.stop();
                                }));

        // Keep the main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Config parseArgs(String[] args) {
        Config config = new Config();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-h":
                case "--help":
                    config.showHelp = true;
                    return config;

                case "-p":
                case "--port":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        config.port = Integer.parseInt(args[++i]);
                        if (config.port < 1 || config.port > 65535) {
                            throw new IllegalArgumentException("Port must be between 1 and 65535");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid port number: " + args[i]);
                    }
                    break;

                case "-d":
                case "--dir":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    config.storageDirectory = Paths.get(args[++i]);
                    break;

                case "-m":
                case "--mode":
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    try {
                        config.mode = ReplayMode.valueOf(args[++i].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException(
                                "Invalid mode: " + args[i] + ". Must be RECORD, CACHE, or REPLAY");
                    }
                    break;

                default:
                    throw new IllegalArgumentException("Unknown option: " + arg);
            }
        }

        return config;
    }

    private static void showHelp() {
        System.out.println(
                "WebReplayProxy - HTTP/HTTPS proxy with recording and replay capabilities");
        System.out.println();
        System.out.println("Usage: java -jar webreplay.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println(
                "  -p, --port <port>         Port to run the proxy on (default: "
                        + DEFAULT_PORT
                        + ")");
        System.out.println(
                "  -d, --dir <directory>     Directory for storing cached requests (default: "
                        + DEFAULT_DIRECTORY
                        + ")");
        System.out.println(
                "  -m, --mode <mode>         Replay mode: RECORD, CACHE, or REPLAY (default: "
                        + DEFAULT_MODE
                        + ")");
        System.out.println("  -h, --help                Show this help message");
        System.out.println();
        System.out.println("Modes:");
        System.out.println("  RECORD - All requests pass through and are recorded");
        System.out.println(
                "  CACHE  - Cached responses returned when available, otherwise pass through and"
                        + " record");
        System.out.println(
                "  REPLAY - Only cached responses returned; non-matching requests return 404");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  # Start with defaults (CACHE mode on port 3128)");
        System.out.println("  java -jar webreplay.jar");
        System.out.println();
        System.out.println("  # Start in RECORD mode on port 8080");
        System.out.println("  java -jar webreplay.jar --port 8080 --mode RECORD");
        System.out.println();
        System.out.println("  # Use custom storage directory");
        System.out.println("  java -jar webreplay.jar --dir /path/to/cache");
    }

    private static class Config {
        int port = DEFAULT_PORT;
        Path storageDirectory = Paths.get(DEFAULT_DIRECTORY);
        ReplayMode mode = DEFAULT_MODE;
        boolean showHelp = false;
    }
}
