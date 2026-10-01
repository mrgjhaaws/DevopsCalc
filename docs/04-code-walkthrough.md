# 04. Code walkthrough and testing

## Maven layout

```
src/main/java        production code
src/main/resources   application.properties, static/index.html
src/test/java        tests
pom.xml              dependencies, plugins, Sonar settings
```

Maven lifecycle you will use most:

| Command | What it does |
|---|---|
| `mvn clean` | delete `target/` |
| `mvn test` | compile and run tests, write the coverage report |
| `mvn package` | test, then build `target/devops-calculator.jar` |
| `mvn clean install` | package and copy the jar to your local Maven repo (what CI runs) |

## The classes

### `CalculatorService`
Pure logic with no HTTP and no I/O. Methods: `add`, `subtract`, `multiply`, `divide`, `sqrt`, `percentOf`, `percentChange`, `whatPercent`, plus the static `parse`.

- Uses `BigDecimal` for exact decimal arithmetic.
- Division rounds to 10 decimal places (`HALF_UP`); square root uses 15 significant digits.
- `clean()` strips trailing zeros (`12.500` becomes `12.5`).
- `parse()` accepts only `-?digits(.digits)?` up to 40 characters.
- Domain errors throw `ArithmeticException` (division by zero, negative root). Bad input throws `IllegalArgumentException`. The HTTP layer turns both into HTTP 400.

### `CalculatorServer`
Wraps the JDK `com.sun.net.httpserver.HttpServer`. One handler routes `/`, `/health`, `/metrics` and `/api/{operation}`. For each API call it parses the query, calls the service, records a metric and writes JSON. `start(0)` picks a free port, which is how tests run without clashes.

### `Metrics`
Thread-safe counters (`LongAdder`) rendered in Prometheus text format: `calculator_requests_total{operation,status}`, `calculator_request_duration_seconds_sum/_count`, `process_uptime_seconds`, `jvm_memory_used_bytes`.

### `Json`
A 60-line JSON writer. It escapes strings and prints `BigDecimal` with `toPlainString()` so no scientific notation appears in responses.

### `App`
Reads `application.properties`, lets the `PORT` environment variable override the port, starts the server and registers a shutdown hook. The version shown in `/health` comes from the jar manifest.

## Adding a new operation (worked example: power)

1. `CalculatorService`: add `public BigDecimal power(BigDecimal base, int exponent)`.
2. `CalculatorServer`: add `"power"` to `OPERATIONS`, add `case "power" -> ...` in `calculate`.
3. Tests: add cases to `CalculatorServiceTest` and an HTTP case to `AppTest`.
4. Docs: add it to [03-api-reference.md](03-api-reference.md) and `requests.http`.
5. Push. CI runs the tests, and Grafana shows a new `operation="power"` series automatically.

## Tests

`mvn test` runs 25 tests:

- **`CalculatorServiceTest`** (unit): exact decimals, rounding, division by zero, negative roots, all percentage modes, and a list of bad inputs the parser must reject.
- **`AppTest`** (end-to-end): starts the real server on a random port and calls it with `java.net.http.HttpClient`. It checks results, error statuses (400, 404, 405), the metrics output, the UI page and port resolution.

Reading the coverage report: after `mvn test` open `target/site/jacoco/index.html`.

## Code quality (SonarQube)

SonarQube reads the JaCoCo coverage file and flags bugs, code smells, vulnerabilities and duplication. See [05-ci-cd.md](05-ci-cd.md#sonarqube) for setup. SonarLint in VS Code gives the same feedback while you type.
