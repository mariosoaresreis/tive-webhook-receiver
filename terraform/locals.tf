locals {
  common_labels = {
    managed-by = "terraform"
    project    = "tive"
  }

  shared_kafka_env = merge(
    {
      KAFKA_BROKERS = var.kafka_bootstrap_servers
    },
    trimspace(var.kafka_security_protocol) == "" ? {} : {
      SPRING_KAFKA_PROPERTIES_SECURITY_PROTOCOL = var.kafka_security_protocol
    },
    (
      trimspace(var.kafka_sasl_username) == "" || trimspace(var.kafka_sasl_password) == ""
      ) ? {} : {
      SPRING_KAFKA_PROPERTIES_SASL_MECHANISM = var.kafka_sasl_mechanism
      SPRING_KAFKA_PROPERTIES_SASL_JAAS_CONFIG = format(
        "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
        var.kafka_sasl_username,
        var.kafka_sasl_password
      )
    }
  )

  webhook_env = merge(local.shared_kafka_env, {
    SPRING_PROFILES_ACTIVE                = "gcp"
    PORT                                  = tostring(var.webhook_port)
    DB_HOST                               = var.db_host
    DB_PORT                               = tostring(var.db_port)
    DB_NAME                               = var.db_name
    DB_USER                               = var.webhook_db_user
    REDIS_HOST                            = var.redis_host
    REDIS_PORT                            = tostring(var.redis_port)
    CLOUD_SQL_INSTANCE                    = var.cloud_sql_instance
    TIVE_CLIENT_ID                        = var.tive_client_id
    TIVE_RECOVERY_ENABLED                 = tostring(var.tive_recovery_enabled)
    TIVE_RECOVERY_BASE_URL                = var.tive_recovery_base_url
    TIVE_RECOVERY_POSITIONS_PATH          = var.tive_recovery_positions_path
    TIVE_RECOVERY_ALERTS_PATH             = var.tive_recovery_alerts_path
    TIVE_ALERT_PERSISTENCE_ENABLED        = "true"
    TIVE_ALERT_PERSISTENCE_CONSUMER_GROUP = "tive-alert-persistence"
  })

  query_env = merge(local.shared_kafka_env, {
    SPRING_PROFILES_ACTIVE = "gcp"
    PORT                   = tostring(var.query_port)
    DB_HOST                = var.db_host
    DB_PORT                = tostring(var.db_port)
    DB_NAME                = var.db_name
    DB_USER                = var.query_db_user
    REDIS_HOST             = var.redis_host
    REDIS_PORT             = tostring(var.redis_port)
    CLOUD_SQL_INSTANCE     = var.cloud_sql_instance
  })

  webhook_sa_annotations = trimspace(var.webhook_gsa_email) == "" ? {} : {
    "iam.gke.io/gcp-service-account" = var.webhook_gsa_email
  }

  query_sa_annotations = trimspace(var.query_gsa_email) == "" ? {} : {
    "iam.gke.io/gcp-service-account" = var.query_gsa_email
  }

  webhook_recovery_secret = trimspace(var.tive_recovery_client_secret) == "" ? var.tive_client_secret : var.tive_recovery_client_secret
}
