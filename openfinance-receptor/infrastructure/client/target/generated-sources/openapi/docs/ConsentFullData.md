

# ConsentFullData


## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
|**consentId** | **String** | Identificador único do consentimento |  |
|**status** | **EnumConsentStatus** |  |  |
|**statusUpdateDateTime** | **OffsetDateTime** | Data e hora da última atualização do status |  |
|**creationDateTime** | **OffsetDateTime** | Data e hora de criação do consentimento |  |
|**expirationDateTime** | **OffsetDateTime** | Data e hora de expiração do consentimento |  |
|**permissions** | **List&lt;EnumConsentPermissions&gt;** |  |  |
|**loggedUser** | [**LoggedUser**](LoggedUser.md) |  |  [optional] |
|**businessEntity** | [**BusinessEntity**](BusinessEntity.md) |  |  [optional] |
|**transactionFromDateTime** | **OffsetDateTime** | Data e hora de início para consulta de transações |  [optional] |
|**transactionToDateTime** | **OffsetDateTime** | Data e hora de fim para consulta de transações |  [optional] |



