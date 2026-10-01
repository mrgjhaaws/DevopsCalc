package com.example.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end tests: start the real server on a random port and call it over
 * HTTP.
 */
class AppTest {

    private static CalculatorServer server;
    private static HttpClient client;
    private static String base;

    @BeforeAll
    static void startServer() throws IOException {
        server = new CalculatorServer(new CalculatorService(), new Metrics(), "test-app", "1.2.3");
        server.start(0);
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + server.port();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private static HttpResponse<String> get(String pathAndQuery) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + pathAndQuery)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthReportsUp() throws Exception {
        HttpResponse<String> res = get("/health");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"status\":\"UP\""));
        assertTrue(res.body().contains("\"version\":\"1.2.3\""));
    }

    @Test
    void addReturnsResultAndExpression() throws Exception {
        HttpResponse<String> res = get("/api/add?a=2&b=3");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"result\":5"), res.body());
        assertTrue(res.body().contains("\"expression\":\"2 + 3 = 5\""), res.body());
    }

    @Test
    void allBinaryOperationsWork() throws Exception {
        assertTrue(get("/api/subtract?a=10&b=4").body().contains("\"result\":6"));
        assertTrue(get("/api/multiply?a=6&b=7").body().contains("\"result\":42"));
        assertTrue(get("/api/divide?a=10&b=4").body().contains("\"result\":2.5"));
    }

    @Test
    void sqrtWorksWithSingleArgument() throws Exception {
        HttpResponse<String> res = get("/api/sqrt?a=144");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"result\":12"), res.body());
    }

    @Test
    void percentageSupportsAllModes() throws Exception {
        assertTrue(get("/api/percentage?a=25&b=80").body().contains("\"result\":20"));
        assertTrue(get("/api/percentage?mode=change&a=80&b=100").body().contains("\"result\":25"));
        assertTrue(get("/api/percentage?mode=what&a=20&b=80").body().contains("\"result\":25"));
        assertEquals(400, get("/api/percentage?mode=bogus&a=1&b=2").statusCode());
    }

    @Test
    void divisionByZeroReturns400() throws Exception {
        HttpResponse<String> res = get("/api/divide?a=1&b=0");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("Division by zero is undefined."));
    }

    @Test
    void negativeSqrtReturns400() throws Exception {
        HttpResponse<String> res = get("/api/sqrt?a=-9");
        assertEquals(400, res.statusCode());
        assertTrue(res.body().contains("not real"));
    }

    @Test
    void invalidAndMissingNumbersReturn400() throws Exception {
        assertEquals(400, get("/api/add?a=abc&b=1").statusCode());
        assertEquals(400, get("/api/add?a=1").statusCode());
        assertEquals(400, get("/api/add").statusCode());
    }

    @Test
    void unknownRoutesReturn404() throws Exception {
        assertEquals(404, get("/api/modulo?a=2&b=3").statusCode());
        assertEquals(404, get("/nope").statusCode());
    }

    @Test
    void powerWorksOverHttp() throws Exception {
        HttpResponse<String> res = get("/api/power?a=2&b=10");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("\"result\":1024"), res.body());
        assertEquals(400, get("/api/power?a=2&b=-1").statusCode());
    }

    @Test
    void nonGetMethodsReturn405() throws Exception {
        HttpRequest post = HttpRequest.newBuilder(URI.create(base + "/api/add?a=1&b=2"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        assertEquals(405, client.send(post, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void metricsExposePrometheusFormat() throws Exception {
        get("/api/add?a=1&b=1");
        get("/api/divide?a=1&b=0");
        HttpResponse<String> res = get("/metrics");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("calculator_requests_total{operation=\"add\",status=\"success\"}"));
        assertTrue(res.body().contains("calculator_requests_total{operation=\"divide\",status=\"error\"}"));
        assertTrue(res.body().contains("calculator_request_duration_seconds_count"));
        assertTrue(res.body().contains("process_uptime_seconds"));
    }

    @Test
    void indexPageIsServed() throws Exception {
        HttpResponse<String> res = get("/");
        assertEquals(200, res.statusCode());
        assertTrue(res.body().contains("DevOps Calculator"));
    }

    @Test
    void parseQueryDecodesAndRejectsBadEncoding() {
        assertEquals("a b", CalculatorServer.parseQuery("x=a%20b&y=2").get("x"));
        assertEquals("1", CalculatorServer.parseQuery("a=1&a=2").get("a"));
        assertTrue(CalculatorServer.parseQuery(null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> CalculatorServer.parseQuery("a=%ZZ"));
    }

    @Test
    void resolvePortPrefersEnvironment() {
        assertEquals(9000, App.resolvePort("9000", "8080"));
        assertEquals(8080, App.resolvePort(null, "8080"));
        assertEquals(8080, App.resolvePort(" ", "8080"));
        assertThrows(IllegalArgumentException.class, () -> App.resolvePort("abc", "8080"));
        assertThrows(IllegalArgumentException.class, () -> App.resolvePort("70000", "8080"));
    }
}
