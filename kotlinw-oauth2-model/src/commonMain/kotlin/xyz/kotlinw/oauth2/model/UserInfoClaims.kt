package xyz.kotlinw.oauth2.model

import kotlinx.serialization.Serializable

/**
 * See: https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
 */
@Serializable
data class UserInfoClaims(
    val sub: String,
    val name: String? = null,
    val given_name: String? = null,
    val family_name: String? = null,
    val middle_name: String? = null,
    val nickname: String? = null,
    val preferred_username: String? = null,
    val profile: String? = null,
    val picture: String? = null,
    val website: String? = null,
    val email: String? = null,
    val email_verified: Boolean? = null,
    val gender: String? = null,
    val birthdate: String? = null,
    val zoneinfo: String? = null,
    val locale: String? = null,
    val phone_number: String? = null,
    val phone_number_verified: Boolean? = null,
    val address: Address? = null,
    val updated_at: Int? = null
) {

    @Serializable
    data class Address(
        val formatted: String? = null,
        val street_address: String? = null,
        val locality: String? = null,
        val region: String? = null,
        val postal_code: String? = null,
        val country: String? = null
    )
}