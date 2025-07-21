resource "azurerm_cosmosdb_account" "openfinance" {
  name                = "openfinance-cosmos-${var.environment}"
  location            = var.location
  resource_group_name = azurerm_resource_group.openfinance.name
  offer_type          = "Standard"
  kind                = "GlobalDocumentDB"

  enable_automatic_failover = true
  enable_multiple_write_locations = true

  consistency_policy {
    consistency_level       = "Session"
    max_interval_in_seconds = 5
    max_staleness_prefix    = 100
  }

  geo_location {
    location          = var.location
    failover_priority = 0
  }

  geo_location {
    location          = var.secondary_location
    failover_priority = 1
  }

  capabilities {
    name = "EnableServerless"
  }
}

resource "azurerm_cosmosdb_sql_database" "openfinance" {
  name                = "openfinance"
  resource_group_name = azurerm_resource_group.openfinance.name
  account_name        = azurerm_cosmosdb_account.openfinance.name
}

resource "azurerm_cosmosdb_sql_container" "accounts" {
  name                  = "accounts"
  resource_group_name   = azurerm_resource_group.openfinance.name
  account_name          = azurerm_cosmosdb_account.openfinance.name
  database_name         = azurerm_cosmosdb_sql_database.openfinance.name
  partition_key_path    = "/partitionKey"
  partition_key_version = 2

  throughput = 20000

  indexing_policy {
    indexing_mode = "consistent"

    included_path {
      path = "/*"
    }

    excluded_path {
      path = "/\"_etag\"/?"
    }
  }

  unique_key {
    paths = ["/accountId", "/institutionId"]
  }
}