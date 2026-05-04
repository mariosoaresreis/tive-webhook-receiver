output "webhook_namespace" {
  description = "Namespace where tive-webhook-receiver is deployed."
  value       = kubernetes_namespace_v1.webhook.metadata[0].name
}

output "query_namespace" {
  description = "Namespace where tive-query is deployed."
  value       = kubernetes_namespace_v1.query.metadata[0].name
}

output "webhook_service" {
  description = "ClusterIP service name for tive-webhook-receiver."
  value       = kubernetes_service_v1.webhook.metadata[0].name
}

output "query_service" {
  description = "ClusterIP service name for tive-query."
  value       = kubernetes_service_v1.query.metadata[0].name
}

output "shared_kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers injected into both services."
  value       = var.kafka_bootstrap_servers
}

