# 09. Ansible

Terraform answers "how do I **create** infrastructure?". Ansible answers "how do I **configure** what is running on it?".

## Layout

```
ansible/
├── ansible.cfg              defaults (inventory path, no retry files)
├── inventory.ini            which servers, how to connect
├── playbook.yml             what to do
└── roles/common/tasks/main.yml   reusable base setup
```

## What the playbook does

1. Checks that you passed `-e image=...`.
2. Runs role **common**: installs `docker`, `git`, `curl`, `jq` with dnf, enables and starts Docker, lets `ec2-user` use Docker without sudo.
3. Logs Docker in to ECR (using the server's IAM role, so no passwords).
4. Pulls the image, replaces the old container, starts the new one with `--restart unless-stopped`.
5. Waits until `http://localhost:8080/health` returns 200.

## Run it

Ansible needs Linux, macOS or WSL2.

```bash
# 1. Put the EC2 IP into the inventory (from `terraform output ec2_public_ip`)
sed -i 's/REPLACE_WITH_EC2_PUBLIC_IP/<the-ip>/' ansible/inventory.ini

cd ansible
ansible calculator -m ping                       # can Ansible reach the server?
ansible-playbook playbook.yml --syntax-check -e image=x
ansible-playbook playbook.yml -e image=<ecr_repository_url>:latest
```

Then open the `app_url` from Terraform's outputs. Run the playbook again after pushing a new image to update the app. It is safe to re-run: package installs and service settings are idempotent.

## Concepts used

| Concept | Where |
|---|---|
| Inventory | `inventory.ini`, group `[calculator]` with connection variables |
| Playbook | `playbook.yml`, a list of plays |
| Role | `roles/common`, tasks you can reuse in any playbook |
| Module | `ansible.builtin.dnf`, `service`, `user`, `command`, `uri`, `assert` |
| `become: true` | run tasks with sudo |
| Variables | `image`, `app_name`, `app_port`, `aws_region` (override with `-e`) |
| `register`, `until`, `retries` | wait for the health check |

## Notes

- The playbook targets **Amazon Linux 2023** (`dnf`). For Ubuntu, switch the role to `ansible.builtin.apt` and change `ansible_user` to `ubuntu`.
- `host_key_checking = False` in `ansible.cfg` is convenient for a throwaway practice server. Turn it on (and use `known_hosts`) for real servers.
- Docker is driven with `command` tasks to avoid extra collections. A next step is the `community.docker.docker_container` module for finer idempotency.
- Keep secrets out of the repo. When you need them, use Ansible Vault (`ansible-vault encrypt_string`).

## Ideas to extend

Add roles for `nginx` (reverse proxy on port 80), `cloudwatch-agent`, or `node_exporter` so Prometheus can also scrape server-level metrics.
