resource "azurerm_role_assignment" "environment_terraform_subscription" {
  scope                = data.azurerm_subscription.current.id
  role_definition_name = "Reader"
  principal_id         = azuread_service_principal.action.object_id
}

resource "azurerm_role_assignment" "environment_terraform_storage_account_tfstate_app" {
  scope                = data.azurerm_storage_account.tfstate_app.id
  role_definition_name = "Contributor"
  principal_id         = azuread_service_principal.action.object_id
}

resource "azurerm_role_assignment" "environment_terraform_resource_group_dashboards" {
  scope                = data.azurerm_resource_group.dashboards.id
  role_definition_name = "Contributor"
  principal_id         = azuread_service_principal.action.object_id
}

resource "azurerm_role_assignment" "environment_runner_github_runner_rg" {
  scope                = data.azurerm_resource_group.github_runner_rg.id
  role_definition_name = "Contributor"
  principal_id         = azuread_service_principal.action.object_id
}

resource "azurerm_role_assignment" "environment_key_vault" {
  scope                = data.azurerm_key_vault.domain_key_vault[0].id
  role_definition_name = "Reader"
  principal_id         = azuread_service_principal.action.object_id
}

resource "azurerm_key_vault_access_policy" "ad_group_policy" {
  key_vault_id = data.azurerm_key_vault.domain_key_vault[0].id

  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azuread_service_principal.action.object_id

  key_permissions         = ["Get", "List", "Import" ]
  secret_permissions      = ["Get", "List"]
  storage_permissions     = []
  certificate_permissions = []
}
