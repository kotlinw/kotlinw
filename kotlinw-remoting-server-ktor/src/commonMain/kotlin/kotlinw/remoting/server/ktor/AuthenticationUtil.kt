package kotlinw.remoting.server.ktor

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import kotlinw.remoting.server.ktor.RemotingConfiguration.AuthenticationConfiguration
import kotlinw.remoting.server.ktor.RemotingConfiguration.AuthenticationConfiguration.OptionalAuthenticationConfiguration
import kotlinw.remoting.server.ktor.RemotingConfiguration.AuthenticationConfiguration.RequiredAuthenticationConfiguration

internal fun <P: Principal> extractMessagingPeerId(authenticationConfiguration: AuthenticationConfiguration<P>?, call: ApplicationCall) =
    authenticationConfiguration?.let {
        when (authenticationConfiguration) {
            is RequiredAuthenticationConfiguration ->
                authenticationConfiguration.identifyClient(call, authenticationConfiguration.extractPrincipal(call))

            is OptionalAuthenticationConfiguration ->
                authenticationConfiguration.identifyClient(call, authenticationConfiguration.extractPrincipal(call))
        }
    }
