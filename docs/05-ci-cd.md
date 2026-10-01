# 05. CI/CD with GitHub Actions

The workflow is `.github/workflows/ci-cd.yml`.

## Jobs

| Job | Runs on | Does |
|---|---|---|
| `build-test` | every push and pull request | checkout, JDK 17 + Maven cache, `mvn clean install`, SonarQube scan (if configured), uploads the jar |
| `docker` | push to `main` only, when `AWS_ROLE_ARN` is set | builds the image, pushes `:<commit-sha>` and `:latest` to ECR |
| `deploy` | after `docker`, when `EKS_CLUSTER_NAME` is set | updates the image in `k8s/deployment.yaml`, runs `kubectl apply`, waits for the rollout |

Without any configuration the pipeline runs `build-test` only, and it is green. Each later stage switches on when you add its settings below, so you can adopt the pipeline gradually.

## Settings to add (repo > Settings > Secrets and variables > Actions)

| Name | Type | Used by | Value |
|---|---|---|---|
| `SONAR_TOKEN` | Secret | SonarQube step | token from your SonarQube server |
| `SONAR_HOST_URL` | Secret | SonarQube step | for example `https://sonar.example.com` |
| `AWS_ROLE_ARN` | Variable | docker, deploy | ARN of the IAM role GitHub assumes |
| `EKS_CLUSTER_NAME` | Variable | deploy | name of your EKS cluster |

Also edit `AWS_REGION` and `ECR_REPOSITORY` in the workflow's `env:` block if you use a different region or repository name.

## SonarQube

Local server for practice:

```bash
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

1. Open http://localhost:9000, log in with `admin` / `admin` and set a new password.
2. Create a project with key `devops-calculator`, then generate a token.
3. Scan from your machine:

```bash
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
  -Dsonar.host.url=http://localhost:9000 -Dsonar.token=<your-token>
```

A `localhost` server cannot be reached from GitHub's hosted runners. For the pipeline, use a SonarQube server with a public URL, a self-hosted runner, or SonarCloud (which also needs `sonar.organization`).

Add a **Quality Gate** check in SonarQube (or the `sonar.qualitygate.wait=true` property) if you want the pipeline to fail when quality drops.

## Connecting GitHub to AWS with OIDC (no stored keys)

1. In AWS IAM, add an identity provider: URL `https://token.actions.githubusercontent.com`, audience `sts.amazonaws.com`.
2. Create a role with this trust policy (replace the account ID and `OWNER/REPO`):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "arn:aws:iam::123456789012:oidc-provider/token.actions.githubusercontent.com" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "token.actions.githubusercontent.com:aud": "sts.amazonaws.com" },
      "StringLike": { "token.actions.githubusercontent.com:sub": [
        "repo:OWNER/REPO:ref:refs/heads/main",
        "repo:OWNER/REPO:environment:production"
      ] }
    }
  }]
}
```

The second `sub` value is needed because the `deploy` job uses the `production` environment, which changes the token subject.

3. Give the role permissions to push to ECR (`AmazonEC2ContainerRegistryPowerUser` is the quick option) and, for deployment, `eks:DescribeCluster`.
4. For EKS, also grant the role access to the cluster (an EKS access entry, or an entry in the `aws-auth` ConfigMap) with rights to manage Deployments in the target namespace.
5. Save the role ARN as the `AWS_ROLE_ARN` variable.

## Why the manifest is edited at deploy time

The `deploy` job runs `sed` on `k8s/deployment.yaml` to insert the exact image tag (the commit SHA). The change exists only in the runner, not in Git. Deploying a unique tag per commit, instead of `:latest`, makes rollouts predictable and rollbacks easy (`kubectl rollout undo deployment/devops-calculator`).

## Protect `main` (recommended)

Repo > Settings > Branches > add a rule: require a pull request and require the `Build, test and scan` check to pass. This is the core CI habit: broken code cannot reach `main`.

## Reading a failed run

Open the **Actions** tab, click the red run, open the failed job and expand the failing step. The last lines usually say why. Reproduce locally with the same command (`mvn -B clean install`).
