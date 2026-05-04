resource "kubernetes_namespace_v1" "webhook" {
  metadata {
    name   = var.webhook_namespace
    labels = local.common_labels
  }
}

resource "kubernetes_namespace_v1" "query" {
  metadata {
    name   = var.query_namespace
    labels = local.common_labels
  }
}

resource "kubernetes_service_account_v1" "webhook" {
  metadata {
    name        = "tive-webhook-receiver-sa"
    namespace   = kubernetes_namespace_v1.webhook.metadata[0].name
    labels      = local.common_labels
    annotations = local.webhook_sa_annotations
  }
}

resource "kubernetes_service_account_v1" "query" {
  metadata {
    name        = "tive-query-sa"
    namespace   = kubernetes_namespace_v1.query.metadata[0].name
    labels      = local.common_labels
    annotations = local.query_sa_annotations
  }
}

resource "kubernetes_config_map_v1" "webhook" {
  metadata {
    name      = "tive-webhook-config"
    namespace = kubernetes_namespace_v1.webhook.metadata[0].name
    labels    = local.common_labels
  }

  data = local.webhook_env
}

resource "kubernetes_config_map_v1" "query" {
  metadata {
    name      = "tive-query-config"
    namespace = kubernetes_namespace_v1.query.metadata[0].name
    labels    = local.common_labels
  }

  data = local.query_env
}

resource "kubernetes_secret_v1" "webhook" {
  metadata {
    name      = "tive-webhook-secrets"
    namespace = kubernetes_namespace_v1.webhook.metadata[0].name
    labels    = local.common_labels
  }

  type = "Opaque"

  data = {
    "db-password"                 = var.db_password
    "tive-client-secret"          = var.tive_client_secret
    "tive-recovery-client-secret" = local.webhook_recovery_secret
  }
}

resource "kubernetes_secret_v1" "query" {
  metadata {
    name      = "tive-query-secrets"
    namespace = kubernetes_namespace_v1.query.metadata[0].name
    labels    = local.common_labels
  }

  type = "Opaque"

  data = {
    "db-password"        = var.db_password
    "tive-query-api-key" = var.query_api_key
  }
}

resource "kubernetes_deployment_v1" "webhook" {
  metadata {
    name      = "tive-webhook-receiver"
    namespace = kubernetes_namespace_v1.webhook.metadata[0].name
    labels    = merge(local.common_labels, { app = "tive-webhook-receiver" })
  }

  spec {
    replicas = var.webhook_replicas

    selector {
      match_labels = {
        app = "tive-webhook-receiver"
      }
    }

    template {
      metadata {
        labels = merge(local.common_labels, { app = "tive-webhook-receiver" })
        annotations = {
          "prometheus.io/scrape" = "true"
          "prometheus.io/path"   = "/actuator/prometheus"
          "prometheus.io/port"   = tostring(var.webhook_port)
        }
      }

      spec {
        service_account_name = kubernetes_service_account_v1.webhook.metadata[0].name

        container {
          name              = "tive-webhook-receiver"
          image             = var.webhook_image
          image_pull_policy = "IfNotPresent"

          port {
            name           = "http"
            container_port = var.webhook_port
          }

          env_from {
            config_map_ref {
              name = kubernetes_config_map_v1.webhook.metadata[0].name
            }
          }

          env {
            name = "DB_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.webhook.metadata[0].name
                key  = "db-password"
              }
            }
          }

          env {
            name = "TIVE_CLIENT_SECRET"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.webhook.metadata[0].name
                key  = "tive-client-secret"
              }
            }
          }

          env {
            name = "TIVE_RECOVERY_CLIENT_SECRET"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.webhook.metadata[0].name
                key  = "tive-recovery-client-secret"
              }
            }
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = var.webhook_port
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = var.webhook_port
            }
            initial_delay_seconds = 30
            period_seconds        = 20
          }
        }
      }
    }
  }
}

resource "kubernetes_deployment_v1" "query" {
  metadata {
    name      = "tive-query"
    namespace = kubernetes_namespace_v1.query.metadata[0].name
    labels    = merge(local.common_labels, { app = "tive-query" })
  }

  spec {
    replicas = var.query_replicas

    selector {
      match_labels = {
        app = "tive-query"
      }
    }

    template {
      metadata {
        labels = merge(local.common_labels, { app = "tive-query" })
        annotations = {
          "prometheus.io/scrape" = "true"
          "prometheus.io/path"   = "/actuator/prometheus"
          "prometheus.io/port"   = tostring(var.query_port)
        }
      }

      spec {
        service_account_name = kubernetes_service_account_v1.query.metadata[0].name

        container {
          name              = "tive-query"
          image             = var.query_image
          image_pull_policy = "IfNotPresent"

          port {
            name           = "http"
            container_port = var.query_port
          }

          env_from {
            config_map_ref {
              name = kubernetes_config_map_v1.query.metadata[0].name
            }
          }

          env {
            name = "DB_PASSWORD"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.query.metadata[0].name
                key  = "db-password"
              }
            }
          }

          env {
            name = "TIVE_QUERY_API_KEY"
            value_from {
              secret_key_ref {
                name = kubernetes_secret_v1.query.metadata[0].name
                key  = "tive-query-api-key"
              }
            }
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = var.query_port
            }
            initial_delay_seconds = 15
            period_seconds        = 10
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = var.query_port
            }
            initial_delay_seconds = 30
            period_seconds        = 20
          }
        }
      }
    }
  }
}

resource "kubernetes_service_v1" "webhook" {
  metadata {
    name      = "tive-webhook-receiver"
    namespace = kubernetes_namespace_v1.webhook.metadata[0].name
    labels    = merge(local.common_labels, { app = "tive-webhook-receiver" })
  }

  spec {
    selector = {
      app = "tive-webhook-receiver"
    }

    port {
      name        = "http"
      port        = 80
      target_port = var.webhook_port
      protocol    = "TCP"
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_service_v1" "query" {
  metadata {
    name      = "tive-query"
    namespace = kubernetes_namespace_v1.query.metadata[0].name
    labels    = merge(local.common_labels, { app = "tive-query" })
  }

  spec {
    selector = {
      app = "tive-query"
    }

    port {
      name        = "http"
      port        = 80
      target_port = var.query_port
      protocol    = "TCP"
    }

    type = "ClusterIP"
  }
}

