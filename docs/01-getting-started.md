# 01. Getting started

## Tools to install

| Tool | Version | Needed for | Check |
|---|---|---|---|
| Git | any recent | version control | `git --version` |
| JDK | 17 or newer (Temurin recommended) | building and running | `java -version` |
| Maven | 3.9+ | build and test | `mvn -version` |
| VS Code | latest | editing | |
| Docker Desktop | latest | images, compose | `docker --version` |
| kubectl and kind (or minikube) | latest | local Kubernetes (optional) | `kubectl version --client` |
| Terraform | 1.5+ | AWS infrastructure (optional) | `terraform -version` |
| Ansible | 2.15+ | server configuration (optional) | `ansible --version` |
| AWS CLI | v2 | AWS access (optional) | `aws --version` |

You only need Git, JDK and Maven to run and test the application. The rest unlock later stages.

**Windows tip:** install Ansible inside WSL2 (Ubuntu). Ansible does not run natively on Windows.

## Open the project in VS Code

1. `File > Open Folder` and choose the project folder.
2. Click **Install** on the "recommended extensions" prompt (Java Extension Pack, Maven, Docker, Kubernetes, Terraform, Ansible, YAML, SonarLint, REST Client, GitHub Actions).
3. Wait for the Java extension to finish importing the Maven project (bottom-right status).

## Run the tests

```bash
mvn test
```

Expected: `Tests run: 25, Failures: 0, Errors: 0`. A coverage report is written to `target/site/jacoco/index.html`.

## Run the application

Three ways, from simplest to most complete:

**A. From VS Code:** open `App.java`, press **F5** (uses `.vscode/launch.json`, port 8080).

**B. From the terminal:**

```bash
mvn -q clean package
java -jar target/devops-calculator.jar
```

**C. Full stack with Docker:**

```bash
docker compose up --build
```

Then open http://localhost:8080. To change the port: `PORT=9090 java -jar target/devops-calculator.jar`.

## Try the API

- In the browser: use the calculator UI at `/`.
- With curl: `curl "http://localhost:8080/api/divide?a=10&b=4"`
- In VS Code: open `requests.http` and click **Send Request** above any line (needs the REST Client extension).

## Useful VS Code tasks

`Terminal > Run Task...` offers: Maven test, Maven package, Docker build, Compose up/down, Terraform validate, Ansible syntax check.

## Put it on GitHub

```bash
git init
git add .
git commit -m "Initial commit: DevOps calculator"
git branch -M main
git remote add origin https://github.com/<you>/devops-calculator.git
git push -u origin main
```

Pushing triggers the CI part of the pipeline automatically. See [05-ci-cd.md](05-ci-cd.md) for the secrets needed by the later stages.

## Next

Read [02-architecture.md](02-architecture.md) to see how all the pieces connect.
