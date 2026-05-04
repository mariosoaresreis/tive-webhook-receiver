variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "Default GCP region."
  type        = string
  default     = "us-central1"
}

variable "gke_cluster_name" {
  description = "Existing GKE cluster name."
  type        = string
}

variable "gke_cluster_location" {
  description = "GKE cluster location (region or zone)."
  type        = string
}

variable "webhook_namespace" {
  description = "Namespace for tive-webhook-receiver."
  type        = string
  default     = "tive-webhook"
}

variable "query_namespace" {
  description = "Namespace for tive-query."
  type        = string
  default     = "tive-query"
}

variable "webhook_image" {
  description = "Container image for tive-webhook-receiver."
  type        = string
}

variable "query_image" {
  description = "Container image for tive-query."
  type        = string
}

variable "webhook_replicas" {
  description = "Replica count for tive-webhook-receiver."
  type        = number
  default     = 2
}

variable "query_replicas" {
  description = "Replica count for tive-query."
  type        = number
  default     = 2
}

variable "webhook_port" {
  description = "HTTP container port for tive-webhook-receiver."
  type        = number
  default     = 8080
}

variable "query_port" {
  description = "HTTP container port for tive-query."
  type        = number
  default     = 8081
}

variable "kafka_bootstrap_servers" {
  description = "Shared Kafka bootstrap servers used by both services."
  type        = string
}

variable "kafka_security_protocol" {
  description = "Kafka security protocol (for example SASL_SSL). Leave empty to skip."
  type        = string
  default     = ""
}

variable "kafka_sasl_mechanism" {
  description = "Kafka SASL mechanism."
  type        = string
  default     = "PLAIN"
}

variable "kafka_sasl_username" {
  description = "Kafka SASL username."
  type        = string
  default     = ""
  sensitive   = true
}

variable "kafka_sasl_password" {
  description = "Kafka SASL password."
  type        = string
  default     = ""
  sensitive   = true
}

variable "db_host" {
  description = "PostgreSQL host."
  type        = string
}

variable "db_port" {
  description = "PostgreSQL port."
  type        = number
  default     = 5432
}

variable "db_name" {
  description = "PostgreSQL database name shared by both services."
  type        = string
  default     = "tive"
}

variable "webhook_db_user" {
  description = "PostgreSQL username for tive-webhook-receiver."
  type        = string
  default     = "tive"
}

variable "query_db_user" {
  description = "PostgreSQL username for tive-query."
  type        = string
  default     = "tive_reader"
}

variable "db_password" {
  description = "PostgreSQL password."
  type        = string
  sensitive   = true
}

variable "redis_host" {
  description = "Redis host shared by both services."
  type        = string
}

variable "redis_port" {
  description = "Redis port."
  type        = number
  default     = 6379
}

variable "cloud_sql_instance" {
  description = "Cloud SQL instance connection name (<project>:<region>:<instance>)."
  type        = string
}

variable "tive_client_id" {
  description = "Tive webhook client ID for incoming authentication."
  type        = string
}

variable "tive_client_secret" {
  description = "Tive webhook client secret for incoming authentication."
  type        = string
  sensitive   = true
}

variable "tive_recovery_enabled" {
  description = "Enables scheduled Tive REST API recovery in tive-webhook-receiver."
  type        = bool
  default     = false
}

variable "tive_recovery_base_url" {
  description = "Base URL used by the recovery poller."
  type        = string
  default     = ""
}

variable "tive_recovery_positions_path" {
  description = "Recovery API path for position events."
  type        = string
  default     = ""
}

variable "tive_recovery_alerts_path" {
  description = "Recovery API path for alert events."
  type        = string
  default     = ""
}

variable "tive_recovery_client_secret" {
  description = "Optional dedicated secret for Tive recovery API auth."
  type        = string
  default     = ""
  sensitive   = true
}

variable "query_api_key" {
  description = "API key required by tive-query (X-Api-Key)."
  type        = string
  sensitive   = true
}

variable "webhook_gsa_email" {
  description = "Optional GSA email to bind to webhook KSA via Workload Identity."
  type        = string
  default     = ""
}

variable "query_gsa_email" {
  description = "Optional GSA email to bind to query KSA via Workload Identity."
  type        = string
  default     = ""
}

