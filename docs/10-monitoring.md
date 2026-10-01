# 10. Monitoring with Prometheus and Grafana

## How it fits

```
Calculator  ──(exposes /metrics)──►  Prometheus (scrapes every 15 s, stores, evaluates alerts)
                                            │
                                            ▼
                                        Grafana (dashboards)
```

Start the stack: `docker compose up --build`, then generate some traffic (use the UI or `requests.http`).

## The metrics the app exposes

| Metric | Type | Meaning |
|---|---|---|
| `calculator_requests_total{operation,status}` | counter | Requests per operation; `status` is `success` or `error` |
| `calculator_request_duration_seconds_sum` / `_count` | summary | Total time and number of requests, giving average latency |
| `process_uptime_seconds` | gauge | Seconds since start |
| `jvm_memory_used_bytes` | gauge | Approximate heap in use |

Prometheus also creates `up{job="calculator"}`: 1 when the last scrape worked, 0 when it failed.

## Prometheus

Open http://localhost:9090.

- **Status > Targets**: the `calculator` job should be **UP**.
- **Graph**: try these PromQL queries.

| Question | Query |
|---|---|
| Requests per second | `sum(rate(calculator_requests_total[1m]))` |
| Requests per minute by operation | `sum by (operation) (rate(calculator_requests_total[1m])) * 60` |
| Error ratio | `sum(rate(calculator_requests_total{status="error"}[5m])) / sum(rate(calculator_requests_total[5m]))` |
| Average latency | `rate(calculator_request_duration_seconds_sum[5m]) / rate(calculator_request_duration_seconds_count[5m])` |
| Is it up? | `up{job="calculator"}` |

Note: a `rate()` over five minutes needs a few scrapes of data, and the ratio queries return nothing until there is traffic.

## Alerts (`monitoring/prometheus/alert-rules.yml`)

| Alert | Fires when | Severity |
|---|---|---|
| `CalculatorDown` | the target cannot be scraped for 1 minute | critical |
| `CalculatorHighErrorRate` | more than 50% of requests fail for 5 minutes | warning |
| `CalculatorSlowRequests` | average latency above 500 ms for 5 minutes | warning |

See them under **Alerts** in Prometheus. To test `CalculatorDown`, run `docker compose stop calculator` and wait about a minute. Prometheus only *evaluates* alerts here; to send email or Slack messages, add Alertmanager. Note that client mistakes such as dividing by zero count as `error` requests.

## Grafana

Open http://localhost:3000 (login `admin`; password `admin` unless you set `GRAFANA_ADMIN_PASSWORD`). The Prometheus data source and the **DevOps Calculator** dashboard are provisioned automatically from files:

- `monitoring/grafana/datasources.yml`: connects Grafana to `http://prometheus:9090`.
- `monitoring/grafana/dashboards.yml`: tells Grafana to load dashboards from a folder.
- `monitoring/grafana/dashboards/dashboard.json`: the dashboard.

Panels: total requests, error rate, uptime, target up/down, requests per minute by operation, errors per minute, average response time and JVM memory.

To edit the dashboard, change it in the UI, then **Share > Export > Save to file**, and replace `dashboard.json` so the change is versioned in Git.

## Monitoring in Kubernetes

The Deployment carries `prometheus.io/scrape`, `path` and `port` annotations. If Prometheus runs in the cluster (for example via the `kube-prometheus-stack` Helm chart), uncomment the `calculator-k8s` job in `prometheus.yml`, or use a ServiceMonitor.

## Add your own metric

1. Call `metrics.record(...)` or add a counter in `Metrics.java` and print it in `render()`.
2. Add a test in `AppTest`.
3. Add a panel in Grafana and export the JSON.

## Beyond this project

Logs (Loki or CloudWatch Logs), traces (OpenTelemetry), host metrics (`node_exporter`), and Alertmanager for notifications are the natural next steps.
