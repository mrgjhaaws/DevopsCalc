# 07. Kubernetes

## Manifests in `k8s/`

| File | Kind | Purpose |
|---|---|---|
| `configmap.yaml` | ConfigMap | Settings injected as environment variables (`PORT`, `APP_ENV`) |
| `deployment.yaml` | Deployment | 2 replicas, rolling updates, probes, resource limits, non-root, Prometheus annotations |
| `service.yaml` | Service (ClusterIP) | Stable internal address, port 80 to container port 8080 |
| `ingress.yaml` | Ingress (optional) | HTTP routing for `calculator.example.com` through an ingress controller |

Key ideas in the Deployment:

- **Rolling update** with `maxUnavailable: 0`: a new pod must be ready before an old one is removed, so a deployment causes no downtime.
- **Readiness probe** (`/health`): a pod receives traffic only when it is ready.
- **Liveness probe** (`/health`): a stuck pod is restarted automatically.
- **Requests and limits**: the scheduler knows what the pod needs, and one pod cannot starve the node.
- **Security context**: non-root, no privilege escalation, all Linux capabilities dropped.

## Try it locally with kind

```bash
# 1. Cluster and image
kind create cluster --name calc
docker build -t devops-calculator:local .
kind load docker-image devops-calculator:local --name calc

# 2. Deploy (point the Deployment at the local image without editing the file)
kubectl apply -f k8s/configmap.yaml -f k8s/service.yaml
sed 's|image: .*|image: devops-calculator:local|; s|imagePullPolicy: Always|imagePullPolicy: IfNotPresent|' \
  k8s/deployment.yaml | kubectl apply -f -

# 3. Check
kubectl get pods
kubectl rollout status deployment/devops-calculator

# 4. Open it
kubectl port-forward svc/devops-calculator 8080:80
# now browse http://localhost:8080
```

minikube works the same way (`minikube image load devops-calculator:local`).

## Everyday commands

```bash
kubectl get pods,svc,deploy
kubectl describe pod <pod>
kubectl logs -f deploy/devops-calculator
kubectl scale deploy/devops-calculator --replicas=4
kubectl rollout history deploy/devops-calculator
kubectl rollout undo deploy/devops-calculator
kubectl delete -f k8s/
```

## Deploying to EKS

Terraform in this repo does not create a cluster. With an existing EKS cluster:

```bash
aws eks update-kubeconfig --name <cluster> --region ap-south-1
# put your ECR image in k8s/deployment.yaml (or let CI do it), then:
kubectl apply -f k8s/
```

The pipeline's `deploy` job does exactly this. See [05-ci-cd.md](05-ci-cd.md). The ECR image must be pullable by the cluster's nodes (EKS worker nodes normally have ECR read access by default).

The Ingress needs a controller installed in the cluster (ingress-nginx, or the AWS Load Balancer Controller, in which case change `ingressClassName` and add its annotations). Replace `calculator.example.com` with your domain and point DNS at the load balancer.

## Ideas to extend

Add a HorizontalPodAutoscaler (`kubectl autoscale deploy devops-calculator --cpu-percent=70 --min=2 --max=6`), put the manifests in a Helm chart, or split environments with Kustomize overlays.
