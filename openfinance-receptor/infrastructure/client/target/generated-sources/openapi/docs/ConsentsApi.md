# ConsentsApi

All URIs are relative to *https://api.openfinance.org.br/open-banking/consents/v3*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createConsent**](ConsentsApi.md#createConsent) | **POST** /consents | Criar novo consentimento |
| [**deleteConsent**](ConsentsApi.md#deleteConsent) | **DELETE** /consents/{consentId} | Revogar consentimento |
| [**extendConsent**](ConsentsApi.md#extendConsent) | **POST** /consents/{consentId}/extends | Renovar consentimento |
| [**getConsent**](ConsentsApi.md#getConsent) | **GET** /consents/{consentId} | Obter detalhes do consentimento |
| [**getConsentExtensions**](ConsentsApi.md#getConsentExtensions) | **GET** /consents/{consentId}/extensions | Obter histórico de extensões |



## createConsent

> ResponseConsentCreated createConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, xJwsSignature, createConsentRequest)

Criar novo consentimento

Cria novo consentimento para compartilhamento de dados

### Example

```java
// Import classes:
import br.com.openfinance.client.ApiClient;
import br.com.openfinance.client.ApiException;
import br.com.openfinance.client.Configuration;
import br.com.openfinance.client.auth.*;
import br.com.openfinance.client.models.*;
import br.com.openfinance.client.api.ConsentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.openfinance.org.br/open-banking/consents/v3");
        
        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        ConsentsApi apiInstance = new ConsentsApi(defaultClient);
        String authorization = "authorization_example"; // String | Token de autorização
        OffsetDateTime xFapiAuthDate = OffsetDateTime.now(); // OffsetDateTime | Data de autenticação
        String xFapiCustomerIpAddress = "xFapiCustomerIpAddress_example"; // String | Endereço IP do cliente
        String xFapiInteractionId = "xFapiInteractionId_example"; // String | Identificador da interação
        String xCustomerUserAgent = "xCustomerUserAgent_example"; // String | User agent do cliente
        String xJwsSignature = "xJwsSignature_example"; // String | Assinatura JWS da requisição
        CreateConsentRequest createConsentRequest = new CreateConsentRequest(); // CreateConsentRequest | 
        try {
            ResponseConsentCreated result = apiInstance.createConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, xJwsSignature, createConsentRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsentsApi#createConsent");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **authorization** | **String**| Token de autorização | |
| **xFapiAuthDate** | **OffsetDateTime**| Data de autenticação | |
| **xFapiCustomerIpAddress** | **String**| Endereço IP do cliente | |
| **xFapiInteractionId** | **String**| Identificador da interação | |
| **xCustomerUserAgent** | **String**| User agent do cliente | |
| **xJwsSignature** | **String**| Assinatura JWS da requisição | |
| **createConsentRequest** | [**CreateConsentRequest**](CreateConsentRequest.md)|  | |

### Return type

[**ResponseConsentCreated**](ResponseConsentCreated.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security), [OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Consentimento criado com sucesso |  * x-fapi-interaction-id -  <br>  |
| **400** | Requisição inválida |  -  |
| **401** | Token de autorização inválido |  -  |
| **403** | Acesso negado |  -  |
| **405** | Método não permitido |  -  |
| **406** | Não aceito |  -  |
| **415** | Tipo de mídia não suportado |  -  |
| **422** | Entidade não processável |  -  |
| **429** | Muitas requisições |  -  |
| **500** | Erro interno do servidor |  -  |
| **504** | Timeout do gateway |  -  |


## deleteConsent

> deleteConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId)

Revogar consentimento

Revoga um consentimento específico

### Example

```java
// Import classes:
import br.com.openfinance.client.ApiClient;
import br.com.openfinance.client.ApiException;
import br.com.openfinance.client.Configuration;
import br.com.openfinance.client.auth.*;
import br.com.openfinance.client.models.*;
import br.com.openfinance.client.api.ConsentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.openfinance.org.br/open-banking/consents/v3");
        
        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        ConsentsApi apiInstance = new ConsentsApi(defaultClient);
        String authorization = "authorization_example"; // String | Token de autorização
        OffsetDateTime xFapiAuthDate = OffsetDateTime.now(); // OffsetDateTime | Data de autenticação
        String xFapiCustomerIpAddress = "xFapiCustomerIpAddress_example"; // String | Endereço IP do cliente
        String xFapiInteractionId = "xFapiInteractionId_example"; // String | Identificador da interação
        String xCustomerUserAgent = "xCustomerUserAgent_example"; // String | User agent do cliente
        String consentId = "consentId_example"; // String | Identificador único do consentimento
        try {
            apiInstance.deleteConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsentsApi#deleteConsent");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **authorization** | **String**| Token de autorização | |
| **xFapiAuthDate** | **OffsetDateTime**| Data de autenticação | |
| **xFapiCustomerIpAddress** | **String**| Endereço IP do cliente | |
| **xFapiInteractionId** | **String**| Identificador da interação | |
| **xCustomerUserAgent** | **String**| User agent do cliente | |
| **consentId** | **String**| Identificador único do consentimento | |

### Return type

null (empty response body)

### Authorization

[OAuth2Security](../README.md#OAuth2Security), [OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Consentimento revogado com sucesso |  * x-fapi-interaction-id -  <br>  |
| **400** | Requisição inválida |  -  |
| **401** | Token de autorização inválido |  -  |
| **403** | Acesso negado |  -  |
| **404** | Recurso não encontrado |  -  |
| **405** | Método não permitido |  -  |
| **406** | Não aceito |  -  |
| **429** | Muitas requisições |  -  |
| **500** | Erro interno do servidor |  -  |
| **504** | Timeout do gateway |  -  |


## extendConsent

> ResponseConsentExtended extendConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId, extendConsentRequest)

Renovar consentimento

Renova um consentimento existente

### Example

```java
// Import classes:
import br.com.openfinance.client.ApiClient;
import br.com.openfinance.client.ApiException;
import br.com.openfinance.client.Configuration;
import br.com.openfinance.client.auth.*;
import br.com.openfinance.client.models.*;
import br.com.openfinance.client.api.ConsentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.openfinance.org.br/open-banking/consents/v3");
        
        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        ConsentsApi apiInstance = new ConsentsApi(defaultClient);
        String authorization = "authorization_example"; // String | Token de autorização
        OffsetDateTime xFapiAuthDate = OffsetDateTime.now(); // OffsetDateTime | Data de autenticação
        String xFapiCustomerIpAddress = "xFapiCustomerIpAddress_example"; // String | Endereço IP do cliente
        String xFapiInteractionId = "xFapiInteractionId_example"; // String | Identificador da interação
        String xCustomerUserAgent = "xCustomerUserAgent_example"; // String | User agent do cliente
        String consentId = "consentId_example"; // String | Identificador único do consentimento
        ExtendConsentRequest extendConsentRequest = new ExtendConsentRequest(); // ExtendConsentRequest | 
        try {
            ResponseConsentExtended result = apiInstance.extendConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId, extendConsentRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsentsApi#extendConsent");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **authorization** | **String**| Token de autorização | |
| **xFapiAuthDate** | **OffsetDateTime**| Data de autenticação | |
| **xFapiCustomerIpAddress** | **String**| Endereço IP do cliente | |
| **xFapiInteractionId** | **String**| Identificador da interação | |
| **xCustomerUserAgent** | **String**| User agent do cliente | |
| **consentId** | **String**| Identificador único do consentimento | |
| **extendConsentRequest** | [**ExtendConsentRequest**](ExtendConsentRequest.md)|  | |

### Return type

[**ResponseConsentExtended**](ResponseConsentExtended.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security), [OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Consentimento renovado com sucesso |  * x-fapi-interaction-id -  <br>  |
| **400** | Requisição inválida |  -  |
| **401** | Token de autorização inválido |  -  |
| **403** | Acesso negado |  -  |
| **404** | Recurso não encontrado |  -  |
| **405** | Método não permitido |  -  |
| **406** | Não aceito |  -  |
| **422** | Entidade não processável |  -  |
| **429** | Muitas requisições |  -  |
| **500** | Erro interno do servidor |  -  |
| **504** | Timeout do gateway |  -  |


## getConsent

> ResponseConsent getConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId)

Obter detalhes do consentimento

Obtém detalhes de um consentimento específico

### Example

```java
// Import classes:
import br.com.openfinance.client.ApiClient;
import br.com.openfinance.client.ApiException;
import br.com.openfinance.client.Configuration;
import br.com.openfinance.client.auth.*;
import br.com.openfinance.client.models.*;
import br.com.openfinance.client.api.ConsentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.openfinance.org.br/open-banking/consents/v3");
        
        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        ConsentsApi apiInstance = new ConsentsApi(defaultClient);
        String authorization = "authorization_example"; // String | Token de autorização
        OffsetDateTime xFapiAuthDate = OffsetDateTime.now(); // OffsetDateTime | Data de autenticação
        String xFapiCustomerIpAddress = "xFapiCustomerIpAddress_example"; // String | Endereço IP do cliente
        String xFapiInteractionId = "xFapiInteractionId_example"; // String | Identificador da interação
        String xCustomerUserAgent = "xCustomerUserAgent_example"; // String | User agent do cliente
        String consentId = "consentId_example"; // String | Identificador único do consentimento
        try {
            ResponseConsent result = apiInstance.getConsent(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsentsApi#getConsent");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **authorization** | **String**| Token de autorização | |
| **xFapiAuthDate** | **OffsetDateTime**| Data de autenticação | |
| **xFapiCustomerIpAddress** | **String**| Endereço IP do cliente | |
| **xFapiInteractionId** | **String**| Identificador da interação | |
| **xCustomerUserAgent** | **String**| User agent do cliente | |
| **consentId** | **String**| Identificador único do consentimento | |

### Return type

[**ResponseConsent**](ResponseConsent.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security), [OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Consentimento obtido com sucesso |  * x-fapi-interaction-id -  <br>  |
| **400** | Requisição inválida |  -  |
| **401** | Token de autorização inválido |  -  |
| **403** | Acesso negado |  -  |
| **404** | Recurso não encontrado |  -  |
| **405** | Método não permitido |  -  |
| **406** | Não aceito |  -  |
| **429** | Muitas requisições |  -  |
| **500** | Erro interno do servidor |  -  |
| **504** | Timeout do gateway |  -  |


## getConsentExtensions

> ResponseConsentExtensions getConsentExtensions(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId)

Obter histórico de extensões

Obtém histórico de extensões de um consentimento

### Example

```java
// Import classes:
import br.com.openfinance.client.ApiClient;
import br.com.openfinance.client.ApiException;
import br.com.openfinance.client.Configuration;
import br.com.openfinance.client.auth.*;
import br.com.openfinance.client.models.*;
import br.com.openfinance.client.api.ConsentsApi;

public class Example {
    public static void main(String[] args) {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://api.openfinance.org.br/open-banking/consents/v3");
        
        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        // Configure OAuth2 access token for authorization: OAuth2Security
        OAuth OAuth2Security = (OAuth) defaultClient.getAuthentication("OAuth2Security");
        OAuth2Security.setAccessToken("YOUR ACCESS TOKEN");

        ConsentsApi apiInstance = new ConsentsApi(defaultClient);
        String authorization = "authorization_example"; // String | Token de autorização
        OffsetDateTime xFapiAuthDate = OffsetDateTime.now(); // OffsetDateTime | Data de autenticação
        String xFapiCustomerIpAddress = "xFapiCustomerIpAddress_example"; // String | Endereço IP do cliente
        String xFapiInteractionId = "xFapiInteractionId_example"; // String | Identificador da interação
        String xCustomerUserAgent = "xCustomerUserAgent_example"; // String | User agent do cliente
        String consentId = "consentId_example"; // String | Identificador único do consentimento
        try {
            ResponseConsentExtensions result = apiInstance.getConsentExtensions(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling ConsentsApi#getConsentExtensions");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
    }
}
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **authorization** | **String**| Token de autorização | |
| **xFapiAuthDate** | **OffsetDateTime**| Data de autenticação | |
| **xFapiCustomerIpAddress** | **String**| Endereço IP do cliente | |
| **xFapiInteractionId** | **String**| Identificador da interação | |
| **xCustomerUserAgent** | **String**| User agent do cliente | |
| **consentId** | **String**| Identificador único do consentimento | |

### Return type

[**ResponseConsentExtensions**](ResponseConsentExtensions.md)

### Authorization

[OAuth2Security](../README.md#OAuth2Security), [OAuth2Security](../README.md#OAuth2Security)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Histórico de extensões obtido com sucesso |  * x-fapi-interaction-id -  <br>  |
| **400** | Requisição inválida |  -  |
| **401** | Token de autorização inválido |  -  |
| **403** | Acesso negado |  -  |
| **404** | Recurso não encontrado |  -  |
| **405** | Método não permitido |  -  |
| **406** | Não aceito |  -  |
| **429** | Muitas requisições |  -  |
| **500** | Erro interno do servidor |  -  |
| **504** | Timeout do gateway |  -  |

