package xyz.kotlinw.oauth2.core

data class OAuth2BearerTokens(val accessToken: OAuth2AccessToken, val refreshToken: OAuth2RefreshToken?)
