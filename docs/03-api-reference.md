# 03. API reference

Base URL: `http://localhost:8080`. All calculator endpoints use **GET** and return JSON. Numbers are plain decimals such as `42`, `-3.5` or `0.001`, up to 40 characters. Exponent notation (`1e5`), commas, `NaN` and `Infinity` are rejected.

## Calculator endpoints

### Binary operations

`GET /api/{add|subtract|multiply|divide}?a=<number>&b=<number>`

```bash
curl "http://localhost:8080/api/add?a=0.1&b=0.2"
```

```json
{"operation":"add","a":0.1,"b":0.2,"result":0.3,"expression":"0.1 + 0.2 = 0.3"}
```

Division rounds to 10 decimal places: `/api/divide?a=1&b=3` gives `0.3333333333`.

### Square root

`GET /api/sqrt?a=<number>`

```json
{"operation":"sqrt","a":144,"result":12,"expression":"sqrt(144) = 12"}
```

Irrational results are returned with 15 significant digits.

### Percentage

`GET /api/percentage?mode=<of|change|what>&a=<number>&b=<number>` (`mode` defaults to `of`)

| Mode | Meaning | Example | Result |
|---|---|---|---|
| `of` | a% of b | `mode=of&a=25&b=80` | `20` |
| `change` | percentage change from a to b | `mode=change&a=80&b=100` | `25` |
| `what` | a is what % of b | `mode=what&a=20&b=80` | `25` |

## Operational endpoints

| Endpoint | Purpose |
|---|---|
| `GET /health` | `{"status":"UP","name":"...","version":"..."}`. Used by Docker, Kubernetes and Ansible. |
| `GET /metrics` | Prometheus text format. See [10-monitoring.md](10-monitoring.md). |
| `GET /` | Web UI |

## Errors

Errors return HTTP status and `{"error":"<message>"}`.

| Situation | Status | Message |
|---|---|---|
| Missing, blank or non-numeric input | 400 | `Please provide valid numbers for this operation.` |
| Division by zero (also `change` with a=0, `what` with b=0) | 400 | `Division by zero is undefined.` |
| Square root of a negative number | 400 | `Square root of a negative number is not real.` |
| Unknown percentage mode | 400 | `Unknown percentage mode. Use of, change or what.` |
| Unknown operation | 404 | `Unknown operation. Supported: ...` |
| Unknown path | 404 | `Not found.` |
| Method other than GET or HEAD | 405 | `Method not allowed. Use GET.` |
| Unexpected server error | 500 | `Internal server error.` |

## Security headers

Every response carries `X-Content-Type-Options: nosniff` and `Cache-Control: no-store`.
