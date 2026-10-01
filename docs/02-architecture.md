# 02. Architecture

## The big picture

```
DEVELOPER
    │  git push
    ▼
 GitHub ──► GitHub Actions
                 │
     ┌───────────┴────────────┐
     ▼                        ▼
  Maven build + tests     SonarQube scan
     └───────────┬────────────┘
                 ▼
          Docker image ──► Amazon ECR
                                │
                                ▼
                  Kubernetes (kubectl apply)
                                │
                     Deployment ► Service ► Ingress ► Internet

Terraform ─► AWS base layer (VPC, subnet, security group, ECR, IAM, EC2)
Ansible   ─► server configuration (Docker) and container deployment on EC2
Prometheus scrapes /metrics ─► Grafana dashboards and alerts
```

## Two ways to run the container

The project supports two deployment targets so you can learn both:

| Target | How | Good for |
|---|---|---|
| **EC2 + Docker** | Terraform creates the server, Ansible installs Docker and runs the container | Understanding servers, simple deployments |
| **Kubernetes** | GitHub Actions applies `k8s/` to a cluster | Scaling, rolling updates, self-healing |

Terraform in this repo provisions the AWS **base layer**: VPC, public subnet, internet gateway, security group, ECR repository, IAM role and one EC2 instance. It does **not** create a Kubernetes cluster. For Kubernetes use a local cluster (kind or minikube) or an existing EKS cluster; see [07-kubernetes.md](07-kubernetes.md).

## The application

```
Browser / curl / Prometheus
        │  HTTP GET
        ▼
 CalculatorServer  (JDK HttpServer, routes requests)
   ├── /               ─► static/index.html
   ├── /health         ─► status JSON (used by probes)
   ├── /metrics        ─► Metrics.render() (Prometheus format)
   └── /api/{op}       ─► parse input ─► CalculatorService ─► JSON
                                   └────► Metrics.record(op, status, time)
```

Design choices, and why:

- **BigDecimal maths** so `0.1 + 0.2` is exactly `0.3`.
- **Strict input parsing** (plain decimals up to 40 characters, no exponents) so a request such as `1E999999999` cannot exhaust memory.
- **No frameworks**: the build is fast, the image is small and there are no third-party CVEs to patch. When you outgrow it, moving to Spring Boot only touches `CalculatorServer`.
- **Stateless**: any number of replicas can run behind a load balancer.

## AWS layout (Terraform)

```
VPC 10.0.0.0/16
└── public subnet 10.0.1.0/24 ── route to Internet Gateway
      └── EC2 (Amazon Linux 2023, IMDSv2, encrypted disk)
            ├── Security group: SSH 22 from your IP only, app 8080 from the internet
            └── IAM role: read from ECR, Systems Manager access
ECR repository: devops-calculator (scan on push, keeps last 10 images)
```

## Ports

| Port | Service |
|---|---|
| 8080 | Calculator |
| 9090 | Prometheus |
| 3000 | Grafana |
| 22 | SSH (EC2, restricted to your IP) |
