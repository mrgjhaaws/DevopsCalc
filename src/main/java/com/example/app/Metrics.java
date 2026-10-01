package com.example.app;

import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Minimal Prometheus metrics registry that renders the text exposition format
 * served at {@code /metrics}. Prometheus scrapes it, Grafana visualises it.
 */
public final class Metrics {

    private final ConcurrentHashMap<String, LongAdder> requests = new ConcurrentHashMap<>();
    private final LongAdder durationNanos = new LongAdder();
    private final LongAdder durationCount = new LongAdder();

    /** Records one calculator API request. {@code status} is "success" or "error". */
    public void record(String operation, String status, long nanos) {
        requests.computeIfAbsent(operation + "|" + status, k -> new LongAdder()).increment();
        durationNanos.add(nanos);
        durationCount.increment();
    }

    public long totalRequests() {
        return durationCount.sum();
    }

    public String render() {
        StringBuilder sb = new StringBuilder();

        sb.append("# HELP calculator_requests_total Total calculator API requests.\n");
        sb.append("# TYPE calculator_requests_total counter\n");
        Map<String, Long> sorted = new TreeMap<>();
        requests.forEach((key, count) -> sorted.put(key, count.sum()));
        sorted.forEach((key, count) -> {
            String[] parts = key.split("\\|", 2);
            sb.append("calculator_requests_total{operation=\"").append(parts[0])
                    .append("\",status=\"").append(parts[1]).append("\"} ").append(count).append('\n');
        });

        sb.append("# HELP calculator_request_duration_seconds Time spent handling calculator API requests.\n");
        sb.append("# TYPE calculator_request_duration_seconds summary\n");
        sb.append("calculator_request_duration_seconds_sum ")
                .append(String.format(Locale.ROOT, "%.6f", durationNanos.sum() / 1_000_000_000.0)).append('\n');
        sb.append("calculator_request_duration_seconds_count ").append(durationCount.sum()).append('\n');

        sb.append("# HELP process_uptime_seconds Seconds since the application started.\n");
        sb.append("# TYPE process_uptime_seconds gauge\n");
        sb.append("process_uptime_seconds ")
                .append(String.format(Locale.ROOT, "%.3f", ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0))
                .append('\n');

        Runtime rt = Runtime.getRuntime();
        sb.append("# HELP jvm_memory_used_bytes Approximate JVM heap memory in use.\n");
        sb.append("# TYPE jvm_memory_used_bytes gauge\n");
        sb.append("jvm_memory_used_bytes ").append(rt.totalMemory() - rt.freeMemory()).append('\n');

        return sb.toString();
    }
}
