# DevOps Calculator: a complete Zero to Hero DevOps project

An arithmetic calculator (add, subtract, multiply, divide, percentage, square root) built as a **complete DevOps project**. The calculator is deliberately simple so you can focus on the real subject: taking source code through **build, test, quality, container, infrastructure, deployment, scaling and monitoring**.

It follows the "Complete DevOps Project File Structure": GitHub, GitHub Actions, Maven, SonarQube, Docker, Kubernetes, Terraform, Prometheus, Grafana and Ansible, on AWS.

## What you get

- A **Java 17 + Maven** application with a REST API, a web UI and Prometheus metrics. It has **no runtime dependencies**: it uses the JDK's built-in HTTP server.
- **25 automated tests** (JUnit 5): unit tests for the maths and end-to-end tests over real HTTP.
- A **GitHub Actions** pipeline: build, test, SonarQube scan, Docker image to ECR, deploy to Kubernetes.
- A multi-stage **Dockerfile** and a **docker-compose.yml** that starts the app, Prometheus and Grafana together.
- **Kubernetes** manifests (Deployment, Service, ConfigMap, Ingress).
- **Terraform** for AWS (VPC, subnet, security group, ECR, IAM, EC2).
- **Ansible** to configure the EC2 server and run the container.
- **Prometheus** scrape config and alert rules, plus a provisioned **Grafana** dashboard.
- Ready-made **VS Code** settings, debug config, tasks and a `requests.http` file to try the API.

## Quick start (VS Code)

**Prerequisites:** JDK 17+, Maven 3.9+, Git, Docker Desktop (optional but recommended). Full install steps are in [docs/01-getting-started.md](docs/01-getting-started.md).

```bash
# 1. Open the project
code devops-project           # or File > Open Folder in VS Code
                              # accept the "install recommended extensions" prompt

# 2. Run the tests
mvn test

# 3. Run the app (or press F5 in VS Code)
mvn -q clean package
java -jar target/devops-calculator.jar

# 4. Open http://localhost:8080  and try:
curl "http://localhost:8080/api/add?a=0.1&b=0.2"
```

Run the whole stack (app + Prometheus + Grafana) with one command:

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Calculator | http://localhost:8080 |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin / admin, change it for anything beyond a local demo) |

## API at a glance

| Endpoint | Example | Result |
|---|---|---|
| `GET /api/add?a=&b=` | `/api/add?a=0.1&b=0.2` | `0.3` |
| `GET /api/subtract?a=&b=` | `/api/subtract?a=10&b=4` | `6` |
| `GET /api/multiply?a=&b=` | `/api/multiply?a=6&b=7` | `42` |
| `GET /api/divide?a=&b=` | `/api/divide?a=10&b=4` | `2.5` |
| `GET /api/sqrt?a=` | `/api/sqrt?a=144` | `12` |
| `GET /api/percentage?mode=&a=&b=` | `/api/percentage?mode=of&a=25&b=80` | `20` |
| `GET /health` | | `{"status":"UP",...}` |
| `GET /metrics` | | Prometheus text format |

Errors return HTTP 400 with `{"error": "..."}`. Details: [docs/03-api-reference.md](docs/03-api-reference.md).

## Project structure

```
devops-project/
├── .github/workflows/ci-cd.yml      GitHub Actions pipeline
├── .vscode/                         VS Code settings, debug config, tasks, extensions
├── src/
│   ├── main/java/com/example/app/
│   │   ├── App.java                 entry point
│   │   ├── CalculatorServer.java    HTTP routes
│   │   ├── CalculatorService.java   the arithmetic (pure logic)
│   │   ├── Metrics.java             Prometheus metrics
│   │   └── Json.java                tiny JSON writer
│   ├── main/resources/
│   │   ├── application.properties
│   │   └── static/index.html        web UI
│   └── test/java/com/example/app/
│       ├── AppTest.java             end-to-end HTTP tests
│       └── CalculatorServiceTest.java
├── pom.xml                          Maven build (JUnit, JaCoCo, Sonar settings)
├── Dockerfile                       multi-stage image build
├── docker-compose.yml               app + Prometheus + Grafana locally
├── k8s/                             deployment, service, configmap, ingress
├── terraform/                       AWS infrastructure as code
├── ansible/                         server configuration and container deploy
├── monitoring/                      Prometheus and Grafana configuration
├── docs/                            full documentation (start here)
├── requests.http                    try the API from VS Code
├── .gitignore
└── README.md
```

## CI/CD flow

1. Code push to the GitHub repository
2. GitHub Actions trigger
3. Checkout code
4. Set up JDK and Maven
5. Build and test with Maven (`mvn clean install`)
6. Run SonarQube scan
7. Build Docker image and push to ECR
8. Update Kubernetes manifest with the new image tag
9. Deploy to Kubernetes (`kubectl apply`)
10. Monitor with Prometheus and Grafana

Setup instructions and required secrets: [docs/05-ci-cd.md](docs/05-ci-cd.md).

## Documentation

| Doc | What it covers |
|---|---|
| [01 Getting started](docs/01-getting-started.md) | Install tools, open in VS Code, run and test |
| [02 Architecture](docs/02-architecture.md) | How all the pieces fit together |
| [03 API reference](docs/03-api-reference.md) | Every endpoint, parameter and error |
| [04 Code walkthrough](docs/04-code-walkthrough.md) | How the Java code and tests work |
| [05 CI/CD](docs/05-ci-cd.md) | GitHub Actions, SonarQube, AWS OIDC setup |
| [06 Docker](docs/06-docker.md) | Dockerfile, image, compose |
| [07 Kubernetes](docs/07-kubernetes.md) | Manifests, local cluster, EKS |
| [08 Terraform on AWS](docs/08-terraform-aws.md) | Provision the AWS base layer |
| [09 Ansible](docs/09-ansible.md) | Configure the server and deploy |
| [10 Monitoring](docs/10-monitoring.md) | Prometheus, Grafana, alerts, PromQL |
| [11 Troubleshooting](docs/11-troubleshooting.md) | Common problems and fixes |
| [12 Learning path](docs/12-learning-path.md) | Zero to Hero roadmap mapped onto this repo |

## What has been verified

Everything that could be run without your accounts was run before this project was handed over:

- The Java code compiles, and all **25 tests pass** (JUnit 5 console runner on JDK 21, targeting Java 17).
- The packaged app was started and its endpoints, UI and `/metrics` were called with `curl`.
- All YAML (workflow, Kubernetes, Ansible, Prometheus, Compose), Terraform HCL and JSON files parse cleanly. The Kubernetes manifests pass schema validation against Kubernetes 1.30, and the Ansible playbook passes `--syntax-check`.

Not run, because they need your machine or cloud accounts: `mvn` itself (the `pom.xml` is standard but unexecuted), `docker build`, `terraform plan/apply`, the Ansible run against a real server, and the GitHub Actions workflow. Expect to fix small environment-specific things the first time you run those (region, account IDs, key pair names). [docs/11-troubleshooting.md](docs/11-troubleshooting.md) lists the likely ones.
