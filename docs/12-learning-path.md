# 12. Zero to Hero learning path

Don't learn every tool at once. Learn them in the order a real application travels through the DevOps lifecycle. Each level below says where to find it in this repository and gives a small exercise.

```
Linux ► Networking ► Git/GitHub ► Java ► Maven ► CI/CD ► SonarQube ► Docker
      ► AWS ► Terraform ► Kubernetes ► Ansible ► Prometheus + Grafana ► Production
```

| Level | Topic | Where in this repo | Exercise |
|---|---|---|---|
| 0 | What DevOps is: SDLC, CI vs CD, IaC, monitoring | [02-architecture.md](02-architecture.md) | Draw the code-to-monitoring flow from memory |
| 1 | Linux | EC2 server, `docker exec`, `ansible` | SSH into the EC2 server; find the container with `docker ps`; read logs with `docker logs` |
| 2 | Networking | ports in docs, security group, Ingress | Explain why 8080 is open but 22 is restricted; run `curl -v localhost:8080/health` |
| 3 | Git and GitHub | whole repo | Create a branch, change a message, open a pull request, merge it |
| 4 | Java basics | `src/main/java` | Read `CalculatorService`, then add the `power` operation ([04](04-code-walkthrough.md)) |
| 5 | Maven | `pom.xml` | Run `mvn clean`, `test`, `package`; open the jar with `jar tf` |
| 6 | CI/CD with GitHub Actions | `.github/workflows/ci-cd.yml` | Break a test on purpose and watch the pipeline fail |
| 7 | SonarQube | `pom.xml` Sonar properties | Run the local scan and fix one code smell |
| 8 | Docker | `Dockerfile`, `docker-compose.yml` | Build the image; compare its size with a single-stage build |
| 9 | AWS | `terraform/`, ECR, IAM, VPC | Find each Terraform resource in the AWS Console |
| 10 | Terraform | `terraform/` | Add a tag, run `plan`, read the diff, `apply`, then `destroy` |
| 11 | Kubernetes | `k8s/` | Deploy to kind, scale to 4 replicas, kill a pod and watch it return |
| 12 | Ansible | `ansible/` | Add a task that installs `htop`; re-run and confirm it is idempotent |
| 13 | Monitoring | `monitoring/` | Stop the app, watch `CalculatorDown` fire, restart it |
| 14 | Everything together | the CI/CD flow in the README | Change one line of code, push, and watch it reach Kubernetes and appear in Grafana |

## The ten progressive projects

Build these in order. Each one is a slice of this repository.

| # | Project | Slice |
|---|---|---|
| 1 | Linux server | An EC2 instance (Terraform), SSH, install Docker |
| 2 | GitHub project | Push this code, work on branches, use pull requests |
| 3 | Java + Maven | `pom.xml`, JUnit tests, the JAR |
| 4 | CI pipeline | `build-test` job in the workflow |
| 5 | Quality pipeline | SonarQube scan and coverage |
| 6 | Docker | Dockerfile, image, container |
| 7 | AWS | ECR, IAM, VPC, EC2 |
| 8 | Terraform | Everything in `terraform/` |
| 9 | Kubernetes | Deployment, Service, Ingress |
| 10 | Complete DevOps project | GitHub, Actions, Maven, SonarQube, Docker, ECR, Kubernetes, AWS, Prometheus, Grafana together |

## What "hero" means

Not knowing twenty tools, but being able to take source code through build, test, quality, container, infrastructure, deployment, scaling, monitoring and troubleshooting, and to **explain, automate, deploy and troubleshoot each stage**.

## After this project

Try Spring Boot with Actuator instead of the hand-written server, Helm charts, GitOps with Argo CD, Terraform modules with remote state, log aggregation, and Alertmanager notifications.
