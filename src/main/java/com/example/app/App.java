package com.example.app;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Application entry point: {@code java -jar devops-calculator.jar}. */
public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(String[] args) throws IOException {
        Properties props = loadProperties();
        int port = resolvePort(System.getenv("PORT"), props.getProperty("server.port", "8080"));
        String name = props.getProperty("app.name", "devops-calculator");
        String version = App.class.getPackage().getImplementationVersion();

        CalculatorServer server = new CalculatorServer(
                new CalculatorService(), new Metrics(), name, version == null ? "dev" : version);
        server.start(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        LOG.log(Level.INFO, "{0} started on port {1}", new Object[] {name, String.valueOf(server.port())});
    }

    /** The PORT environment variable wins over application.properties. */
    static int resolvePort(String envPort, String propertyPort) {
        String raw = (envPort == null || envPort.isBlank()) ? propertyPort : envPort;
        try {
            int port = Integer.parseInt(raw.trim());
            if (port < 0 || port > 65535) {
                throw new IllegalArgumentException("Port out of range: " + port);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port: " + raw, e);
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = App.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        }
        return props;
    }
}
