# 06. Docker

## The Dockerfile

Two stages:

1. **build** (`maven:3.9-eclipse-temurin-17`): copies `pom.xml` first and runs `dependency:go-offline` so dependency layers are cached, then copies `src/` and runs `mvn clean package` (which also runs the tests).
2. **runtime** (`eclipse-temurin:17-jre-alpine`): only the JRE and the jar. Runs as a **non-root** user, exposes 8080 and has a `HEALTHCHECK` on `/health`.

The result is a small image with no build tools and no source code.

## Commands

```bash
# Build
docker build -t devops-calculator:local .

# Run
docker run -d --name calc -p 8080:8080 devops-calculator:local

# Look around
docker ps
docker logs -f calc
docker exec -it calc sh
docker inspect --format '{{.State.Health.Status}}' calc

# Stop and remove
docker stop calc && docker rm calc
```

## docker compose

`docker compose up --build` starts three containers on one network, so they reach each other by service name (`calculator`, `prometheus`, `grafana`):

| Service | Image | Port |
|---|---|---|
| calculator | built from `Dockerfile` | 8080 |
| prometheus | `prom/prometheus:v2.53.0` | 9090 |
| grafana | `grafana/grafana:11.1.0` | 3000 |

Other commands: `docker compose logs -f calculator`, `docker compose down`, `docker compose down -v` (also deletes data volumes).

Set your own Grafana password: `GRAFANA_ADMIN_PASSWORD=change-me docker compose up`.

## Pushing to ECR by hand

```bash
REGION=ap-south-1
REPO=<account-id>.dkr.ecr.$REGION.amazonaws.com/devops-calculator

aws ecr get-login-password --region $REGION | docker login --username AWS --password-stdin ${REPO%%/*}
docker tag devops-calculator:local $REPO:latest
docker push $REPO:latest
```

The ECR repository is created by Terraform ([08-terraform-aws.md](08-terraform-aws.md)); CI does this automatically after setup ([05-ci-cd.md](05-ci-cd.md)).

## `.dockerignore`

Keeps `target/`, `.git/`, infrastructure folders and docs out of the build context, which makes builds faster and images cleaner.
