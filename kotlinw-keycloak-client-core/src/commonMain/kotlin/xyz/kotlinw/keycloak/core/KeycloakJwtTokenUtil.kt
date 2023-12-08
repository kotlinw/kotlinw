package xyz.kotlinw.keycloak.core

import kotlinx.serialization.json.JsonObject
import xyz.kotlinw.jwt.model.JwtToken.Converters.asStringList
import xyz.kotlinw.jwt.model.JwtToken.JwtTokenPayload

val JwtTokenPayload.keycloakRoles get() = (jsonObject["realm_access"] as? JsonObject)?.get("roles")?.asStringList
