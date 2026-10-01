package com.example.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BinaryOperator;

/**
 * HTTP layer built on the JDK's own {@link HttpServer}, so the app has zero
 * runtime dependencies.
 *
 * <pre>
 * GET /                     web UI
 * GET /health               liveness / readiness probe
 * GET /metrics              Prometheus metrics
 * GET /api/{operation}      add | subtract | multiply | divide | sqrt | percentage
 * </pre>
 */
public final class CalculatorServer {

    private static final List<String> OPERATIONS = List.of("add", "subtract", "multiply", "divide", "sqrt",
            "percentage", "power");

    private final CalculatorService service;
    private final Metrics metrics;
    private final String appName;
    private final String appVersion;

    private HttpServer server;
    private ExecutorService executor;
    private byte[] indexHtml;

    public CalculatorServer(CalculatorService service, Metrics metrics, String appName, String appVersion) {
        this.service = service;
        this.metrics = metrics;
        this.appName = appName;
        this.appVersion = appVersion;
    }

    /**
     * Starts the server. Pass port 0 to let the OS pick a free port (used by
     * tests).
     */
    public void start(int port) throws IOException {
        indexHtml = loadResource("/static/index.html");
        executor = Executors.newFixedThreadPool(8);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::route);
        server.setExecutor(executor);
        server.start();
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    // ------------------------------------------------------------------ routing

    private void route(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            if (!"GET".equals(method) && !"HEAD".equals(method)) {
                ex.getResponseHeaders().set("Allow", "GET, HEAD");
                sendJson(ex, 405, Json.error("Method not allowed. Use GET."));
                return;
            }
            String path = ex.getRequestURI().getPath();
            if ("/health".equals(path)) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("status", "UP");
                body.put("name", appName);
                body.put("version", appVersion);
                sendJson(ex, 200, Json.object(body));
            } else if ("/metrics".equals(path)) {
                send(ex, 200, "text/plain; version=0.0.4; charset=utf-8",
                        metrics.render().getBytes(StandardCharsets.UTF_8));
            } else if ("/".equals(path) || "/index.html".equals(path)) {
                if (indexHtml.length == 0) {
                    sendJson(ex, 404, Json.error("UI not found."));
                } else {
                    send(ex, 200, "text/html; charset=utf-8", indexHtml);
                }
            } else if (path.startsWith("/api/")) {
                handleApi(ex, path.substring("/api/".length()));
            } else {
                sendJson(ex, 404, Json.error("Not found."));
            }
        } catch (RuntimeException e) {
            sendJson(ex, 500, Json.error("Internal server error."));
        } finally {
            ex.close();
        }
    }

    private void handleApi(HttpExchange ex, String operation) throws IOException {
        if (!OPERATIONS.contains(operation)) {
            sendJson(ex, 404, Json.error("Unknown operation. Supported: " + String.join(", ", OPERATIONS) + "."));
            return;
        }
        long start = System.nanoTime();
        try {
            Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
            String body = calculate(operation, query);
            metrics.record(operation, "success", System.nanoTime() - start);
            sendJson(ex, 200, body);
        } catch (IllegalArgumentException | ArithmeticException e) {
            metrics.record(operation, "error", System.nanoTime() - start);
            sendJson(ex, 400, Json.error(e.getMessage()));
        }
    }

    // --------------------------------------------------------------- operations

    private String calculate(String operation, Map<String, String> query) {
        return switch (operation) {
            case "add" -> binary(operation, "+", query, service::add);
            case "subtract" -> binary(operation, "-", query, service::subtract);
            case "multiply" -> binary(operation, "*", query, service::multiply);
            case "divide" -> binary(operation, "/", query, service::divide);
            case "sqrt" -> squareRoot(query);
            case "percentage" -> percentage(query);
            case "power" -> binary(operation, "^", query, service::power);
            default -> throw new IllegalArgumentException("Unknown operation.");
        };
    }

    private String binary(String operation, String symbol, Map<String, String> query, BinaryOperator<BigDecimal> fn) {
        BigDecimal a = number(query, "a");
        BigDecimal b = number(query, "b");
        BigDecimal result = fn.apply(a, b);
        String expression = a.toPlainString() + " " + symbol + " " + b.toPlainString() + " = " + result.toPlainString();
        return response(operation, expression, result, "a", a, "b", b);
    }

    private String squareRoot(Map<String, String> query) {
        BigDecimal a = number(query, "a");
        BigDecimal result = service.sqrt(a);
        return response("sqrt", "sqrt(" + a.toPlainString() + ") = " + result.toPlainString(), result, "a", a, null,
                null);
    }

    private String percentage(Map<String, String> query) {
        String mode = query.getOrDefault("mode", "of");
        BigDecimal a = number(query, "a");
        BigDecimal b = number(query, "b");
        BigDecimal result;
        String expression;
        switch (mode) {
            case "of" -> {
                result = service.percentOf(a, b);
                expression = a.toPlainString() + "% of " + b.toPlainString() + " = " + result.toPlainString();
            }
            case "change" -> {
                result = service.percentChange(a, b);
                expression = "Change from " + a.toPlainString() + " to " + b.toPlainString()
                        + " = " + result.toPlainString() + "%";
            }
            case "what" -> {
                result = service.whatPercent(a, b);
                expression = a.toPlainString() + " is " + result.toPlainString() + "% of " + b.toPlainString();
            }
            default -> throw new IllegalArgumentException("Unknown percentage mode. Use of, change or what.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operation", "percentage");
        body.put("mode", mode);
        body.put("a", a);
        body.put("b", b);
        body.put("result", result);
        body.put("expression", expression);
        return Json.object(body);
    }

    private static String response(String operation, String expression, BigDecimal result,
            String nameA, BigDecimal a, String nameB, BigDecimal b) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("operation", operation);
        body.put(nameA, a);
        if (nameB != null) {
            body.put(nameB, b);
        }
        body.put("result", result);
        body.put("expression", expression);
        return Json.object(body);
    }

    private static BigDecimal number(Map<String, String> query, String name) {
        return CalculatorService.parse(query.get(name));
    }

    // ------------------------------------------------------------------ helpers

    static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        try {
            for (String pair : rawQuery.split("&")) {
                int idx = pair.indexOf('=');
                String key = URLDecoder.decode(idx < 0 ? pair : pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = idx < 0 ? "" : URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.putIfAbsent(key, value);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(CalculatorService.MSG_INVALID_NUMBER);
        }
        return params;
    }

    private static byte[] loadResource(String path) throws IOException {
        try (InputStream in = CalculatorServer.class.getResourceAsStream(path)) {
            return in == null ? new byte[0] : in.readAllBytes();
        }
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int status, String contentType, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        if ("HEAD".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(status, -1);
            return;
        }
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }
}
