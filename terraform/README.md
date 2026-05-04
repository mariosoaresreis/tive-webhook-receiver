# Terraform - GKE deployment for tive services

This folder deploys both applications into the same GCP project and same GKE cluster, but in separate namespaces:

- `tive-webhook-receiver` -> namespace `tive-webhook`
- `tive-query` -> namespace `tive-query`

Both workloads receive the same Kafka bootstrap/auth settings from Terraform, so they share the same Kafka cluster.

## What this Terraform creates

- Kubernetes namespaces for webhook/query
- Kubernetes service accounts (with optional Workload Identity annotation)
- ConfigMaps with app environment variables
- Secrets with sensitive values
- Deployments and ClusterIP services for both apps

## Prerequisites

- Terraform `>= 1.6`
- `gcloud` authenticated to the target project
- Access to an existing GKE cluster
- Container images already pushed to Artifact Registry

## Usage

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars with real values
terraform init
terraform plan
terraform apply
```

## Shared Kafka contract

Terraform injects the same values into both Deployments:

- `KAFKA_BROKERS`
- optional `SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL`
- optional `SPRING_KAFKA_PROPERTIES_SASL_MECHANISM`
- optional `SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG`

This guarantees both services point to the same Kafka cluster and auth settings.

## Security note

`kubernetes_secret_v1` stores secret values in Terraform state. Use a protected remote state backend and restricted IAM.

