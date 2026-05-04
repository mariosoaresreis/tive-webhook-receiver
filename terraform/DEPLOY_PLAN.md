# Deploy Plan (GCP + GKE)

## Goal

Deploy both services to the same GCP project and same GKE cluster, each in a different namespace, sharing one Kafka cluster.

- Namespace A: `tive-webhook`
- Namespace B: `tive-query`
- Shared Kafka: same `KAFKA_BROKERS` and SASL settings in both Deployments

## Phase 0 - Preconditions

1. Build and push both images to Artifact Registry:
   - `tive-webhook-receiver`
   - `tive-query`
2. Ensure PostgreSQL and Redis are reachable from GKE.
3. Confirm Kafka endpoint and credentials.
4. Grant your deploy identity permissions for:
   - `container.clusters.get`
   - Kubernetes API access (through cluster RBAC)

## Phase 1 - Configure Terraform inputs

1. Copy `terraform.tfvars.example` to `terraform.tfvars`.
2. Fill cluster and image values.
3. Fill shared Kafka values once:
   - `kafka_bootstrap_servers`
   - `kafka_security_protocol` (if required)
   - `kafka_sasl_username` and `kafka_sasl_password` (if required)
4. Fill secrets:
   - `db_password`
   - `tive_client_secret`
   - `query_api_key`
5. (Optional) Set Workload Identity annotations:
   - `webhook_gsa_email`
   - `query_gsa_email`

## Phase 2 - Plan and apply

Run from `terraform/`:

```bash
terraform init
terraform plan
terraform apply
```

Expected result:

- `tive-webhook-receiver` deployment/service in `tive-webhook`
- `tive-query` deployment/service in `tive-query`
- Both pods configured with the same Kafka connection values

## Phase 3 - Post-deploy validation

1. Check namespaces and pods:

```bash
kubectl get ns | grep tive
kubectl get pods -n tive-webhook
kubectl get pods -n tive-query
```

2. Confirm both deployments received the same Kafka broker value:

```bash
kubectl -n tive-webhook get configmap tive-webhook-config -o yaml | grep KAFKA_BROKERS
kubectl -n tive-query get configmap tive-query-config -o yaml | grep KAFKA_BROKERS
```

3. Basic health checks:

```bash
kubectl -n tive-webhook port-forward svc/tive-webhook-receiver 18080:80
kubectl -n tive-query port-forward svc/tive-query 18081:80
```

Then call:

- `http://localhost:18080/actuator/health`
- `http://localhost:18081/actuator/health`

## Phase 4 - Rollout strategy

1. Deploy `tive-webhook-receiver` first.
2. Validate ingress/webhook traffic and Kafka publishing.
3. Deploy `tive-query` and validate read APIs.
4. Enable `tive.recovery` only after baseline webhook flow is healthy.

## Rollback

- Fast rollback: point `webhook_image`/`query_image` back to previous tags and re-apply.
- Full rollback: `terraform destroy` (only if environment is dedicated and disposable).

