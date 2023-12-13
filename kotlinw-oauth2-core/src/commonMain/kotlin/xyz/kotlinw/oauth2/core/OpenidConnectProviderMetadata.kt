package xyz.kotlinw.oauth2.core

import kotlinw.util.stdlib.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * See: https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
 */
@Serializable
data class OpenidConnectProviderMetadata(

    //
    // OpenID Connect Discovery 1.0
    //

    val issuer: Url,

    @SerialName("authorization_endpoint")
    val authorizationEndpoint: Url,

    @SerialName("token_endpoint")
    val tokenEndpoint: Url? = null,

    @SerialName("userinfo_endpoint")
    val userinfoEndpoint: Url? = null,

    @SerialName("jwks_uri")
    val jwksUri: Url,

    @SerialName("registration_endpoint")
    val registrationEndpoint: Url? = null,

    @SerialName("scopes_supported")
    val scopesSupported: List<String>? = null,

    @SerialName("response_types_supported")
    val responseTypesSupported: List<String>,

    @SerialName("response_modes_supported")
    val responseModesSupported: List<String>? = null,

    @SerialName("grant_types_supported")
    val grantTypesSupported: List<String>? = null,

    @SerialName("acr_values_supported")
    val acrValuesSupported: List<String>? = null,

    @SerialName("subject_types_supported")
    val subjectTypesSupported: List<String>,

    @SerialName("id_token_signing_alg_values_supported")
    val idTokenSigningAlgValuesSupported: List<String>,

    @SerialName("id_token_encryption_alg_values_supported")
    val idTokenEncryptionAlgValuesSupported: List<String>? = null,

    @SerialName("id_token_encryption_enc_values_supported")
    val idTokenEncryptionEncValuesSupported: List<String>? = null,

    @SerialName("userinfo_signing_alg_values_supported")
    val userinfoSigningAlgValuesSupported: List<String>? = null,

    @SerialName("userinfo_encryption_alg_values_supported")
    val userinfoEncryptionAlgValuesSupported: List<String>? = null,

    @SerialName("userinfo_encryption_enc_values_supported")
    val userinfoEncryptionEncValuesSupported: List<String>? = null,

    @SerialName("request_object_signing_alg_values_supported")
    val requestObjectSigningAlgValuesSupported: List<String>? = null,

    @SerialName("request_object_encryption_alg_values_supported")
    val requestObjectEncryptionAlgValuesSupported: List<String>? = null,

    @SerialName("request_object_encryption_enc_values_supported")
    val requestObjectEncryptionEncValuesSupported: List<String>? = null,

    @SerialName("token_endpoint_auth_methods_supported")
    val tokenEndpointAuthMethodsSupported: List<String>? = null,

    @SerialName("token_endpoint_auth_signing_alg_values_supported")
    val tokenEndpointAuthSigningAlgValuesSupported: List<String>? = null,

    @SerialName("display_values_supported")
    val displayValuesSupported: List<String>? = null,

    @SerialName("claim_types_supported")
    val claimTypesSupported: List<String>? = null,

    @SerialName("claims_supported")
    val claimsSupported: List<String>? = null,

    @SerialName("service_documentation")
    val serviceDocumentation: String? = null,

    @SerialName("claims_locales_supported")
    val claimsLocalesSupported: List<String>? = null,

    @SerialName("ui_locales_supported")
    val uiLocalesSupported: List<String>? = null,

    @SerialName("claims_parameter_supported")
    val claimsParameterSupported: Boolean = false,

    @SerialName("request_parameter_supported")
    val requestParameterSupported: Boolean = false,

    @SerialName("request_uri_parameter_supported")
    val requestUriParameterSupported: Boolean = true,

    @SerialName("require_request_uri_registration")
    val requireRequestUriRegistration: Boolean = false,

    @SerialName("op_policy_uri")
    val opPolicyUri: String? = null,

    @SerialName("op_tos_uri")
    val opTosUri: String? = null,

    //
    // Provider specific
    //

    @SerialName("introspection_endpoint")
    val introspectionEndpoint: Url? = null,

    @SerialName("end_session_endpoint")
    val endSessionEndpoint: Url? = null,

    @SerialName("frontchannel_logout_session_supported")
    val frontchannelLogoutSessionSupported: Boolean? = null,

    @SerialName("frontchannel_logout_supported")
    val frontchannelLogoutSupported: Boolean? = null,

    @SerialName("check_session_iframe")
    val checkSessionIframe: Url? = null,

    @SerialName("introspection_endpoint_auth_methods_supported")
    val introspectionEndpointAuthMethodsSupported: List<String>? = null,

    @SerialName("introspection_endpoint_auth_signing_alg_values_supported")
    val introspectionEndpointAuthSigningAlgValuesSupported: List<String>? = null,

    @SerialName("authorization_signing_alg_values_supported")
    val authorizationSigningAlgValuesSupported: List<String>? = null,

    @SerialName("authorization_encryption_alg_values_supported")
    val authorizationEncryptionAlgValuesSupported: List<String>? = null,

    @SerialName("authorization_encryption_enc_values_supported")
    val authorizationEncryptionEncValuesSupported: List<String>? = null,

    @SerialName("code_challenge_methods_supported")
    val codeChallengeMethodsSupported: List<String>? = null,

    @SerialName("tls_client_certificate_bound_access_tokens")
    val tlsClientCertificateBoundAccessTokens: Boolean? = null,

    @SerialName("dpop_signing_alg_values_supported")
    val dpopSigningAlgValuesSupported: List<String>? = null,

    @SerialName("revocation_endpoint")
    val revocationEndpoint: Url? = null,

    @SerialName("revocation_endpoint_auth_methods_supported")
    val revocationEndpointAuthMethodsSupported: List<String>? = null,

    @SerialName("revocation_endpoint_auth_signing_alg_values_supported")
    val revocationEndpointAuthSigningAlgValuesSupported: List<String>? = null,

    @SerialName("backchannel_logout_supported")
    val backchannelLogoutSupported: Boolean? = null,

    @SerialName("backchannel_logout_session_supported")
    val backchannelLogoutSessionSupported: Boolean? = null,

    @SerialName("device_authorization_endpoint")
    val deviceAuthorizationEndpoint: Url? = null,

    @SerialName("backchannel_token_delivery_modes_supported")
    val backchannelTokenDeliveryModesSupported: List<String>? = null,

    @SerialName("backchannel_authentication_endpoint")
    val backchannelAuthenticationEndpoint: Url? = null,

    @SerialName("backchannel_authentication_request_signing_alg_values_supported")
    val backchannelAuthenticationRequestSigningAlgValuesSupported: List<String>? = null,

    @SerialName("require_pushed_authorization_requests")
    val requirePushedAuthorizationRequests: Boolean? = null,

    @SerialName("pushed_authorization_request_endpoint")
    val pushedAuthorizationRequestEndpoint: Url? = null,

    @SerialName("mtls_endpoint_aliases")
    val mtlsEndpointAliases: MtlsEndpointAliases? = null,
) {

    @Serializable
    data class MtlsEndpointAliases(

        @SerialName("token_endpoint")
        val tokenEndpoint: Url,

        @SerialName("revocation_endpoint")
        val revocationEndpoint: Url? = null,

        @SerialName("introspection_endpoint")
        val introspectionEndpoint: Url? = null,

        @SerialName("device_authorization_endpoint")
        val deviceAuthorizationEndpoint: Url? = null,

        @SerialName("registration_endpoint")
        val registrationEndpoint: Url? = null,

        @SerialName("userinfo_endpoint")
        val userinfoEndpoint: Url? = null,

        @SerialName("pushed_authorization_request_endpoint")
        val pushedAuthorizationRequestEndpoint: Url? = null,

        @SerialName("backchannel_authentication_endpoint")
        val backchannelAuthenticationEndpoint: Url? = null
    )
}

val OpenidConnectProviderMetadata.tokenEndpointOrThrow: Url
    get() = tokenEndpoint
        ?: throw IllegalStateException("Authorization server $issuer does not expose a token endpoint.")
