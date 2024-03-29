package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType.Text
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.serialize
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.module.webapp.core.InitialWebAppClientEnvironmentData
import xyz.kotlinw.module.webapp.core.initialWebAppClientEnvironmentJsDataVariableName

class WebAppMainHttpController(
    private val deploymentMode: DeploymentMode,
    private val webAppServerEnvironmentProvider: WebAppServerEnvironmentProvider,
    private val serializerService: SerializerService,
    private val authenticationProviderId: String? = null
) : KtorServerApplicationConfigurer() {

    override fun Context.setup() {
        ktorApplication.routing {
            if (authenticationProviderId != null) {
                authenticate(authenticationProviderId) {
                    setupRouting(this, this@WebAppMainHttpController)
                }
            } else {
                setupRouting(this, this@WebAppMainHttpController)
            }
        }
    }

    private fun setupRouting(
        route: Route,
        webAppMainHttpController: WebAppMainHttpController
    ) {
        route.route("/") {
            // TODO további oldalakat is kiszolgálni, ne csak a root-ot
            // TODO átnézni: https://gist.github.com/hal0gen/5852bd9db240c477f20c
            val title = "PWA" // TODO paraméterként + i18n
            val applicationFilePath = if (deploymentMode == Development)
                "/app.js" // TODO lehetne valami directory is, de eddig nem sikerült úgy bekonfigolni a webapp build-et :\
            else
                "/app/pwa/js/app.js" // TODO konfigurálható

            get {
                val initialWebAppClientEnvironmentData = with(call) {
                    InitialWebAppClientEnvironmentData(
                        webAppMainHttpController.webAppServerEnvironmentProvider.localeId,
                        webAppMainHttpController.webAppServerEnvironmentProvider.authenticationStatus
                    )
                }

                // TODO html/lang
                // TODO theme-color
                // TODO további meta tag-ek
                // TODO service worker registration in 'load' event: https://web.dev/articles/service-workers-registration
                // TODO splash
                // TODO workaround using 'originalFetch': https://youtrack.jetbrains.com/issue/KTOR-539/Ability-to-use-browser-cookie-storage

                val serviceWorkerWebPath = "/app/pwa/js/sw.js" // TODO konfigurálható
                call.respondText(
                    """
                                <!doctype html>
                                <html lang="hu">
                                    <head>
                                        <meta charset="UTF-8">
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                        <meta name="theme-color" content="#000000" />
                                        <meta http-equiv="X-UA-Compatible" content="ie=edge">
                                        <link rel="manifest" href="/manifest.webmanifest">
                                        <title>$title</title>
                                        <script>
    
                                            window.originalFetch = window.fetch;
                                            window.fetch = function (resource, init) {
                                                return window.originalFetch(resource, Object.assign({ credentials: 'include' }, init || {}));
                                            };
                                        
                                            if ('serviceWorker' in navigator) {
                                                navigator.serviceWorker.register('$serviceWorkerWebPath');
                                            }
                                            var $initialWebAppClientEnvironmentJsDataVariableName = `${
                        serializerService.serialize(
                            initialWebAppClientEnvironmentData
                        )
                    }`;
                                        </script>
                                    </head>
                                    <body>
                                        <div id="root"></div>
                                        <script src="$applicationFilePath"></script>
                                    </body>
                                </html>
                            """.trimIndent(),
                    Text.Html
                )
            }
        }
    }
}
