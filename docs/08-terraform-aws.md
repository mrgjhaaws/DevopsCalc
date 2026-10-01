# 08. Terraform on AWS

## What it creates

| Resource | Purpose |
|---|---|
| VPC `10.0.0.0/16`, internet gateway, public subnet `10.0.1.0/24`, route table | Network |
| Security group | SSH (22) from **your IP only**, app port (8080) from the internet, all outbound |
| ECR repository `devops-calculator` | Stores Docker images (scan on push, keeps the last 10) |
| IAM role and instance profile | EC2 can pull from ECR and be reached through Systems Manager |
| EC2 instance (Amazon Linux 2023, `t3.micro`) | Runs the container. IMDSv2 required, encrypted 20 GB gp3 disk |

Not included: a Kubernetes cluster, a load balancer, a database. This project needs none of them.

## Files

| File | Contents |
|---|---|
| `main.tf` | Provider and all resources |
| `variables.tf` | Inputs with descriptions and validation |
| `outputs.tf` | Values you need afterwards (ECR URL, public IP, app URL) |
| `terraform.tfvars` | Your values (contains no secrets) |

## Before you start

1. Install Terraform and the AWS CLI, then run `aws configure` (or use SSO) so `aws sts get-caller-identity` works.
2. Create an EC2 key pair in the target region (Console > EC2 > Key Pairs) and save the `.pem` file, for example as `~/.ssh/my-ec2-key.pem` (`chmod 400` it).
3. Edit `terraform/terraform.tfvars`:
   - `key_name`: your key pair name
   - `admin_cidr`: your public IP with `/32`, found with `curl ifconfig.me`. Open-to-the-world (`0.0.0.0/0`) is rejected by validation.

## Run it

```bash
cd terraform
terraform init       # download the AWS provider
terraform fmt        # tidy formatting
terraform validate   # check the configuration
terraform plan       # preview: read this carefully
terraform apply      # create everything (type yes)
terraform output     # ecr_repository_url, ec2_public_ip, app_url
```

Commit the `.terraform.lock.hcl` file that `init` creates. Never commit `*.tfstate` (it can contain sensitive values); `.gitignore` already excludes it.

## Cost and cleanup

A `t3.micro` and an ECR repository cost little, but not nothing, and a public IPv4 address is billed. When you finish practising:

```bash
terraform destroy
```

If the ECR repository still holds images, empty it first (or add `force_delete = true` to the `aws_ecr_repository` resource).

## Growing this setup

- **Remote state:** store state in S3 with locking (an S3 backend block) once more than one person works on it.
- **Modules:** split networking, compute and registry into modules under `terraform/modules/`.
- **Environments:** add `dev.tfvars` and `prod.tfvars` and pick one with `-var-file`.
- **EKS:** add the `terraform-aws-modules/eks/aws` module to create a cluster, then use the Kubernetes deploy path.

## Next

Deploy the container onto the new server with Ansible: [09-ansible.md](09-ansible.md).
