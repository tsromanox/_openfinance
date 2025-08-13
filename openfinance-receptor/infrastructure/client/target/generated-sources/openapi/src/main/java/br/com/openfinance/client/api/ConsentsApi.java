package br.com.openfinance.client.api;

import br.com.openfinance.client.ApiClient;

import br.com.openfinance.client.model.CreateConsentRequest;
import br.com.openfinance.client.model.Error;
import br.com.openfinance.client.model.ExtendConsentRequest;
import java.time.OffsetDateTime;
import br.com.openfinance.client.model.ResponseConsent;
import br.com.openfinance.client.model.ResponseConsentCreated;
import br.com.openfinance.client.model.ResponseConsentExtended;
import br.com.openfinance.client.model.ResponseConsentExtensions;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient.ResponseSpec;
import org.springframework.web.client.RestClientResponseException;

@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2025-08-12T15:58:57.215261-03:00[America/Sao_Paulo]", comments = "Generator version: 7.14.0")
public class ConsentsApi {
    private ApiClient apiClient;

    public ConsentsApi() {
        this(new ApiClient());
    }

    public ConsentsApi(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Criar novo consentimento
     * Cria novo consentimento para compartilhamento de dados
     * <p><b>201</b> - Consentimento criado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>415</b> - Tipo de mídia não suportado
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param xJwsSignature Assinatura JWS da requisição
     * @param createConsentRequest The createConsentRequest parameter
     * @return ResponseConsentCreated
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec createConsentRequestCreation(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String xJwsSignature, @jakarta.annotation.Nonnull CreateConsentRequest createConsentRequest) throws RestClientResponseException {
        Object postBody = createConsentRequest;
        // verify the required parameter 'authorization' is set
        if (authorization == null) {
            throw new RestClientResponseException("Missing the required parameter 'authorization' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiAuthDate' is set
        if (xFapiAuthDate == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiAuthDate' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiCustomerIpAddress' is set
        if (xFapiCustomerIpAddress == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiCustomerIpAddress' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiInteractionId' is set
        if (xFapiInteractionId == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiInteractionId' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xCustomerUserAgent' is set
        if (xCustomerUserAgent == null) {
            throw new RestClientResponseException("Missing the required parameter 'xCustomerUserAgent' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xJwsSignature' is set
        if (xJwsSignature == null) {
            throw new RestClientResponseException("Missing the required parameter 'xJwsSignature' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'createConsentRequest' is set
        if (createConsentRequest == null) {
            throw new RestClientResponseException("Missing the required parameter 'createConsentRequest' when calling createConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<>();

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();


        if (authorization != null)
        headerParams.add("Authorization", apiClient.parameterToString(authorization));
        if (xFapiAuthDate != null)
        headerParams.add("x-fapi-auth-date", apiClient.parameterToString(xFapiAuthDate));
        if (xFapiCustomerIpAddress != null)
        headerParams.add("x-fapi-customer-ip-address", apiClient.parameterToString(xFapiCustomerIpAddress));
        if (xFapiInteractionId != null)
        headerParams.add("x-fapi-interaction-id", apiClient.parameterToString(xFapiInteractionId));
        if (xCustomerUserAgent != null)
        headerParams.add("x-customer-user-agent", apiClient.parameterToString(xCustomerUserAgent));
        if (xJwsSignature != null)
        headerParams.add("x-jws-signature", apiClient.parameterToString(xJwsSignature));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "OAuth2Security", "OAuth2Security" };

        ParameterizedTypeReference<ResponseConsentCreated> localVarReturnType = new ParameterizedTypeReference<>() {};
        return apiClient.invokeAPI("/consents", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Criar novo consentimento
     * Cria novo consentimento para compartilhamento de dados
     * <p><b>201</b> - Consentimento criado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>415</b> - Tipo de mídia não suportado
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param xJwsSignature Assinatura JWS da requisição
     * @param createConsentRequest The createConsentRequest parameter
     * @return ResponseConsentCreated
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseConsentCreated createConsent(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String xJwsSignature, @jakarta.annotation.Nonnull CreateConsentRequest createConsentRequest) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsentCreated> localVarReturnType = new ParameterizedTypeReference<>() {};
        return createConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, xJwsSignature, createConsentRequest).body(localVarReturnType);
    }

    /**
     * Criar novo consentimento
     * Cria novo consentimento para compartilhamento de dados
     * <p><b>201</b> - Consentimento criado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>415</b> - Tipo de mídia não suportado
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param xJwsSignature Assinatura JWS da requisição
     * @param createConsentRequest The createConsentRequest parameter
     * @return ResponseEntity&lt;ResponseConsentCreated&gt;
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ResponseConsentCreated> createConsentWithHttpInfo(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String xJwsSignature, @jakarta.annotation.Nonnull CreateConsentRequest createConsentRequest) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsentCreated> localVarReturnType = new ParameterizedTypeReference<>() {};
        return createConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, xJwsSignature, createConsentRequest).toEntity(localVarReturnType);
    }

    /**
     * Criar novo consentimento
     * Cria novo consentimento para compartilhamento de dados
     * <p><b>201</b> - Consentimento criado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>415</b> - Tipo de mídia não suportado
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param xJwsSignature Assinatura JWS da requisição
     * @param createConsentRequest The createConsentRequest parameter
     * @return ResponseSpec
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec createConsentWithResponseSpec(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String xJwsSignature, @jakarta.annotation.Nonnull CreateConsentRequest createConsentRequest) throws RestClientResponseException {
        return createConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, xJwsSignature, createConsentRequest);
    }
    /**
     * Revogar consentimento
     * Revoga um consentimento específico
     * <p><b>204</b> - Consentimento revogado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec deleteConsentRequestCreation(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        Object postBody = null;
        // verify the required parameter 'authorization' is set
        if (authorization == null) {
            throw new RestClientResponseException("Missing the required parameter 'authorization' when calling deleteConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiAuthDate' is set
        if (xFapiAuthDate == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiAuthDate' when calling deleteConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiCustomerIpAddress' is set
        if (xFapiCustomerIpAddress == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiCustomerIpAddress' when calling deleteConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiInteractionId' is set
        if (xFapiInteractionId == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiInteractionId' when calling deleteConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xCustomerUserAgent' is set
        if (xCustomerUserAgent == null) {
            throw new RestClientResponseException("Missing the required parameter 'xCustomerUserAgent' when calling deleteConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'consentId' is set
        if (consentId == null) {
            throw new RestClientResponseException("Missing the required parameter 'consentId' when calling deleteConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<>();

        pathParams.put("consentId", consentId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();


        if (authorization != null)
        headerParams.add("Authorization", apiClient.parameterToString(authorization));
        if (xFapiAuthDate != null)
        headerParams.add("x-fapi-auth-date", apiClient.parameterToString(xFapiAuthDate));
        if (xFapiCustomerIpAddress != null)
        headerParams.add("x-fapi-customer-ip-address", apiClient.parameterToString(xFapiCustomerIpAddress));
        if (xFapiInteractionId != null)
        headerParams.add("x-fapi-interaction-id", apiClient.parameterToString(xFapiInteractionId));
        if (xCustomerUserAgent != null)
        headerParams.add("x-customer-user-agent", apiClient.parameterToString(xCustomerUserAgent));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "OAuth2Security", "OAuth2Security" };

        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
        return apiClient.invokeAPI("/consents/{consentId}", HttpMethod.DELETE, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Revogar consentimento
     * Revoga um consentimento específico
     * <p><b>204</b> - Consentimento revogado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public void deleteConsent(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
        deleteConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId).body(localVarReturnType);
    }

    /**
     * Revogar consentimento
     * Revoga um consentimento específico
     * <p><b>204</b> - Consentimento revogado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<Void> deleteConsentWithHttpInfo(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        ParameterizedTypeReference<Void> localVarReturnType = new ParameterizedTypeReference<>() {};
        return deleteConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId).toEntity(localVarReturnType);
    }

    /**
     * Revogar consentimento
     * Revoga um consentimento específico
     * <p><b>204</b> - Consentimento revogado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseSpec
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec deleteConsentWithResponseSpec(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        return deleteConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId);
    }
    /**
     * Renovar consentimento
     * Renova um consentimento existente
     * <p><b>200</b> - Consentimento renovado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @param extendConsentRequest The extendConsentRequest parameter
     * @return ResponseConsentExtended
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec extendConsentRequestCreation(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId, @jakarta.annotation.Nonnull ExtendConsentRequest extendConsentRequest) throws RestClientResponseException {
        Object postBody = extendConsentRequest;
        // verify the required parameter 'authorization' is set
        if (authorization == null) {
            throw new RestClientResponseException("Missing the required parameter 'authorization' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiAuthDate' is set
        if (xFapiAuthDate == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiAuthDate' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiCustomerIpAddress' is set
        if (xFapiCustomerIpAddress == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiCustomerIpAddress' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiInteractionId' is set
        if (xFapiInteractionId == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiInteractionId' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xCustomerUserAgent' is set
        if (xCustomerUserAgent == null) {
            throw new RestClientResponseException("Missing the required parameter 'xCustomerUserAgent' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'consentId' is set
        if (consentId == null) {
            throw new RestClientResponseException("Missing the required parameter 'consentId' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'extendConsentRequest' is set
        if (extendConsentRequest == null) {
            throw new RestClientResponseException("Missing the required parameter 'extendConsentRequest' when calling extendConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<>();

        pathParams.put("consentId", consentId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();


        if (authorization != null)
        headerParams.add("Authorization", apiClient.parameterToString(authorization));
        if (xFapiAuthDate != null)
        headerParams.add("x-fapi-auth-date", apiClient.parameterToString(xFapiAuthDate));
        if (xFapiCustomerIpAddress != null)
        headerParams.add("x-fapi-customer-ip-address", apiClient.parameterToString(xFapiCustomerIpAddress));
        if (xFapiInteractionId != null)
        headerParams.add("x-fapi-interaction-id", apiClient.parameterToString(xFapiInteractionId));
        if (xCustomerUserAgent != null)
        headerParams.add("x-customer-user-agent", apiClient.parameterToString(xCustomerUserAgent));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { 
            "application/json"
        };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "OAuth2Security", "OAuth2Security" };

        ParameterizedTypeReference<ResponseConsentExtended> localVarReturnType = new ParameterizedTypeReference<>() {};
        return apiClient.invokeAPI("/consents/{consentId}/extends", HttpMethod.POST, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Renovar consentimento
     * Renova um consentimento existente
     * <p><b>200</b> - Consentimento renovado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @param extendConsentRequest The extendConsentRequest parameter
     * @return ResponseConsentExtended
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseConsentExtended extendConsent(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId, @jakarta.annotation.Nonnull ExtendConsentRequest extendConsentRequest) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsentExtended> localVarReturnType = new ParameterizedTypeReference<>() {};
        return extendConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId, extendConsentRequest).body(localVarReturnType);
    }

    /**
     * Renovar consentimento
     * Renova um consentimento existente
     * <p><b>200</b> - Consentimento renovado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @param extendConsentRequest The extendConsentRequest parameter
     * @return ResponseEntity&lt;ResponseConsentExtended&gt;
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ResponseConsentExtended> extendConsentWithHttpInfo(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId, @jakarta.annotation.Nonnull ExtendConsentRequest extendConsentRequest) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsentExtended> localVarReturnType = new ParameterizedTypeReference<>() {};
        return extendConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId, extendConsentRequest).toEntity(localVarReturnType);
    }

    /**
     * Renovar consentimento
     * Renova um consentimento existente
     * <p><b>200</b> - Consentimento renovado com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>422</b> - Entidade não processável
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @param extendConsentRequest The extendConsentRequest parameter
     * @return ResponseSpec
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec extendConsentWithResponseSpec(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId, @jakarta.annotation.Nonnull ExtendConsentRequest extendConsentRequest) throws RestClientResponseException {
        return extendConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId, extendConsentRequest);
    }
    /**
     * Obter detalhes do consentimento
     * Obtém detalhes de um consentimento específico
     * <p><b>200</b> - Consentimento obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseConsent
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getConsentRequestCreation(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        Object postBody = null;
        // verify the required parameter 'authorization' is set
        if (authorization == null) {
            throw new RestClientResponseException("Missing the required parameter 'authorization' when calling getConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiAuthDate' is set
        if (xFapiAuthDate == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiAuthDate' when calling getConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiCustomerIpAddress' is set
        if (xFapiCustomerIpAddress == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiCustomerIpAddress' when calling getConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiInteractionId' is set
        if (xFapiInteractionId == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiInteractionId' when calling getConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xCustomerUserAgent' is set
        if (xCustomerUserAgent == null) {
            throw new RestClientResponseException("Missing the required parameter 'xCustomerUserAgent' when calling getConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'consentId' is set
        if (consentId == null) {
            throw new RestClientResponseException("Missing the required parameter 'consentId' when calling getConsent", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<>();

        pathParams.put("consentId", consentId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();


        if (authorization != null)
        headerParams.add("Authorization", apiClient.parameterToString(authorization));
        if (xFapiAuthDate != null)
        headerParams.add("x-fapi-auth-date", apiClient.parameterToString(xFapiAuthDate));
        if (xFapiCustomerIpAddress != null)
        headerParams.add("x-fapi-customer-ip-address", apiClient.parameterToString(xFapiCustomerIpAddress));
        if (xFapiInteractionId != null)
        headerParams.add("x-fapi-interaction-id", apiClient.parameterToString(xFapiInteractionId));
        if (xCustomerUserAgent != null)
        headerParams.add("x-customer-user-agent", apiClient.parameterToString(xCustomerUserAgent));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "OAuth2Security", "OAuth2Security" };

        ParameterizedTypeReference<ResponseConsent> localVarReturnType = new ParameterizedTypeReference<>() {};
        return apiClient.invokeAPI("/consents/{consentId}", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Obter detalhes do consentimento
     * Obtém detalhes de um consentimento específico
     * <p><b>200</b> - Consentimento obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseConsent
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseConsent getConsent(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsent> localVarReturnType = new ParameterizedTypeReference<>() {};
        return getConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId).body(localVarReturnType);
    }

    /**
     * Obter detalhes do consentimento
     * Obtém detalhes de um consentimento específico
     * <p><b>200</b> - Consentimento obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseEntity&lt;ResponseConsent&gt;
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ResponseConsent> getConsentWithHttpInfo(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsent> localVarReturnType = new ParameterizedTypeReference<>() {};
        return getConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId).toEntity(localVarReturnType);
    }

    /**
     * Obter detalhes do consentimento
     * Obtém detalhes de um consentimento específico
     * <p><b>200</b> - Consentimento obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseSpec
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getConsentWithResponseSpec(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        return getConsentRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId);
    }
    /**
     * Obter histórico de extensões
     * Obtém histórico de extensões de um consentimento
     * <p><b>200</b> - Histórico de extensões obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseConsentExtensions
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    private ResponseSpec getConsentExtensionsRequestCreation(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        Object postBody = null;
        // verify the required parameter 'authorization' is set
        if (authorization == null) {
            throw new RestClientResponseException("Missing the required parameter 'authorization' when calling getConsentExtensions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiAuthDate' is set
        if (xFapiAuthDate == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiAuthDate' when calling getConsentExtensions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiCustomerIpAddress' is set
        if (xFapiCustomerIpAddress == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiCustomerIpAddress' when calling getConsentExtensions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xFapiInteractionId' is set
        if (xFapiInteractionId == null) {
            throw new RestClientResponseException("Missing the required parameter 'xFapiInteractionId' when calling getConsentExtensions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'xCustomerUserAgent' is set
        if (xCustomerUserAgent == null) {
            throw new RestClientResponseException("Missing the required parameter 'xCustomerUserAgent' when calling getConsentExtensions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // verify the required parameter 'consentId' is set
        if (consentId == null) {
            throw new RestClientResponseException("Missing the required parameter 'consentId' when calling getConsentExtensions", HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), null, null, null);
        }
        // create path and map variables
        final Map<String, Object> pathParams = new HashMap<>();

        pathParams.put("consentId", consentId);

        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        final HttpHeaders headerParams = new HttpHeaders();
        final MultiValueMap<String, String> cookieParams = new LinkedMultiValueMap<>();
        final MultiValueMap<String, Object> formParams = new LinkedMultiValueMap<>();


        if (authorization != null)
        headerParams.add("Authorization", apiClient.parameterToString(authorization));
        if (xFapiAuthDate != null)
        headerParams.add("x-fapi-auth-date", apiClient.parameterToString(xFapiAuthDate));
        if (xFapiCustomerIpAddress != null)
        headerParams.add("x-fapi-customer-ip-address", apiClient.parameterToString(xFapiCustomerIpAddress));
        if (xFapiInteractionId != null)
        headerParams.add("x-fapi-interaction-id", apiClient.parameterToString(xFapiInteractionId));
        if (xCustomerUserAgent != null)
        headerParams.add("x-customer-user-agent", apiClient.parameterToString(xCustomerUserAgent));
        final String[] localVarAccepts = { 
            "application/json"
        };
        final List<MediaType> localVarAccept = apiClient.selectHeaderAccept(localVarAccepts);
        final String[] localVarContentTypes = { };
        final MediaType localVarContentType = apiClient.selectHeaderContentType(localVarContentTypes);

        String[] localVarAuthNames = new String[] { "OAuth2Security", "OAuth2Security" };

        ParameterizedTypeReference<ResponseConsentExtensions> localVarReturnType = new ParameterizedTypeReference<>() {};
        return apiClient.invokeAPI("/consents/{consentId}/extensions", HttpMethod.GET, pathParams, queryParams, postBody, headerParams, cookieParams, formParams, localVarAccept, localVarContentType, localVarAuthNames, localVarReturnType);
    }

    /**
     * Obter histórico de extensões
     * Obtém histórico de extensões de um consentimento
     * <p><b>200</b> - Histórico de extensões obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseConsentExtensions
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseConsentExtensions getConsentExtensions(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsentExtensions> localVarReturnType = new ParameterizedTypeReference<>() {};
        return getConsentExtensionsRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId).body(localVarReturnType);
    }

    /**
     * Obter histórico de extensões
     * Obtém histórico de extensões de um consentimento
     * <p><b>200</b> - Histórico de extensões obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseEntity&lt;ResponseConsentExtensions&gt;
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseEntity<ResponseConsentExtensions> getConsentExtensionsWithHttpInfo(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        ParameterizedTypeReference<ResponseConsentExtensions> localVarReturnType = new ParameterizedTypeReference<>() {};
        return getConsentExtensionsRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId).toEntity(localVarReturnType);
    }

    /**
     * Obter histórico de extensões
     * Obtém histórico de extensões de um consentimento
     * <p><b>200</b> - Histórico de extensões obtido com sucesso
     * <p><b>400</b> - Requisição inválida
     * <p><b>401</b> - Token de autorização inválido
     * <p><b>403</b> - Acesso negado
     * <p><b>404</b> - Recurso não encontrado
     * <p><b>405</b> - Método não permitido
     * <p><b>406</b> - Não aceito
     * <p><b>429</b> - Muitas requisições
     * <p><b>500</b> - Erro interno do servidor
     * <p><b>504</b> - Timeout do gateway
     * @param authorization Token de autorização
     * @param xFapiAuthDate Data de autenticação
     * @param xFapiCustomerIpAddress Endereço IP do cliente
     * @param xFapiInteractionId Identificador da interação
     * @param xCustomerUserAgent User agent do cliente
     * @param consentId Identificador único do consentimento
     * @return ResponseSpec
     * @throws RestClientResponseException if an error occurs while attempting to invoke the API
     */
    public ResponseSpec getConsentExtensionsWithResponseSpec(@jakarta.annotation.Nonnull String authorization, @jakarta.annotation.Nonnull OffsetDateTime xFapiAuthDate, @jakarta.annotation.Nonnull String xFapiCustomerIpAddress, @jakarta.annotation.Nonnull String xFapiInteractionId, @jakarta.annotation.Nonnull String xCustomerUserAgent, @jakarta.annotation.Nonnull String consentId) throws RestClientResponseException {
        return getConsentExtensionsRequestCreation(authorization, xFapiAuthDate, xFapiCustomerIpAddress, xFapiInteractionId, xCustomerUserAgent, consentId);
    }
}
