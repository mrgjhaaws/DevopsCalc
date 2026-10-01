# 11. Troubleshooting

## Application and Maven

| Problem | Likely cause and fix |
|---|---|
| `release version 17 not supported` | JDK is older than 17. Install JDK 17+ and check `java -version` and `mvn -version` (Maven uses `JAVA_HOME`). |
| `Address already in use` | Something else uses port 8080. Run with another port: `PORT=9090 java -jar target/devops-calculator.jar`. |
| Tests fail only on your machine with connection errors | A firewall or proxy blocks `localhost`. Tests bind a random local port. Allow Java, or run tests in Docker. |
| VS Code shows red errors on valid code | Java project not imported yet. Command Palette > "Java: Clean Java Language Server Workspace". |
| `mvn: command not found` | Maven not on `PATH`. Reopen the terminal after installing. |
| No Maven? | You can still compile: `mkdir out && javac -d out $(find src/main/java -name '*.java') && cp -r src/main/resources/* out/ && java -cp out com.example.app.App` (macOS, Linux, Git Bash). |

## Docker

| Problem | Fix |
|---|---|
| `Cannot connect to the Docker daemon` | Start Docker Desktop. |
| Build is slow every time | Keep `pom.xml` copied before `src/` in the Dockerfile (already done) so dependency layers cache. |
| Container exits immediately | `docker logs <container>`. Usually a port conflict or a wrong `PORT`. |
| Container shows `unhealthy` | `docker inspect --format '{{json .State.Health}}' <container>` and check `/health`. |

## GitHub Actions

| Problem | Fix |
|---|---|
| Only `build-test` runs | Expected until you set the `AWS_ROLE_ARN` (and `EKS_CLUSTER_NAME`) variables. See [05-ci-cd.md](05-ci-cd.md). |
| `Not authorized to perform sts:AssumeRoleWithWebIdentity` | The role trust policy does not match the repo, branch or environment. Check the `sub` values, including `environment:production` for the deploy job. |
| SonarQube step skipped | `SONAR_TOKEN` or `SONAR_HOST_URL` secret missing. It is skipped on purpose. |
| SonarQube step cannot connect | The server URL is not reachable from GitHub's runners (`localhost` will not work). |
| `denied: User is not authorized to perform: ecr:...` | The role lacks ECR push permissions. |
| `repository ... does not exist` on push | Create it with Terraform first, and make sure `ECR_REPOSITORY` matches its name. |

## Terraform

| Problem | Fix |
|---|---|
| `No value for required variable "key_name"` | Edit `terraform/terraform.tfvars` or pass `-var key_name=...`. |
| `admin_cidr must be a valid CIDR block` | Use the form `203.0.113.10/32`, and not `0.0.0.0/0`. |
| `InvalidKeyPair.NotFound` | The key pair must already exist in the same region as `aws_region`. |
| `UnauthorizedOperation` | Your AWS user or role lacks permissions. Check `aws sts get-caller-identity`. |
| `RepositoryNotEmptyException` on destroy | Delete the images in ECR, or set `force_delete = true` on the repository. |
| SSH stopped working | Your public IP changed. Update `admin_cidr` and run `terraform apply`. |

## Ansible

| Problem | Fix |
|---|---|
| `UNREACHABLE! Permission denied (publickey)` | Wrong key path or user in `inventory.ini`. Amazon Linux uses `ec2-user`. Run `chmod 400` on the key. |
| `UNREACHABLE` timeout | Wrong IP, instance stopped, or your IP not in `admin_cidr`. |
| `Pass the image with -e image=...` | Add `-e image=<ecr_repository_url>:latest`. |
| ECR login fails on the server | The instance needs its IAM instance profile (Terraform creates it), and the region must match. |
| Ansible on Windows | Use WSL2. |

## Kubernetes

| Problem | Fix |
|---|---|
| `ImagePullBackOff` | Wrong image name, or the cluster cannot pull from ECR. For kind, `kind load docker-image` and use `imagePullPolicy: IfNotPresent`. |
| `CrashLoopBackOff` | `kubectl logs <pod> --previous`. Check the port and probes. |
| Pod stays `0/1 Ready` | Readiness probe failing. `kubectl describe pod <pod>`. |
| Ingress does nothing | No ingress controller installed, or wrong `ingressClassName`. |

## Prometheus and Grafana

| Problem | Fix |
|---|---|
| Target `calculator` is DOWN | Check `docker compose ps`. Inside compose the target is `calculator:8080`, not `localhost:8080`. |
| Grafana panels say "No data" | Generate traffic first, wait about a minute, and check the time range. |
| Dashboard missing | Check the volume mounts in `docker-compose.yml` and `docker compose logs grafana`. |
| Forgot the Grafana password | `docker compose down -v` resets it (this also deletes Grafana data). |

## Still stuck?

Work outward from the error: read the last lines of the log, reproduce with the smallest command, and change one thing at a time. This is the "troubleshooting" step of the DevOps lifecycle, and it is a skill you build by doing.
