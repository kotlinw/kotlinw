package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType.Text
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.RootRoute
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
                    setupRouting()
                }
            } else {
                setupRouting()
            }
        }
    }

    private fun RootRoute.setupRouting() {
        route("/") { // FIXME WHOCOS-77
            serveMainHtmlPage()
        }
        route(Regex("admin/.+")) { // FIXME WHOCOS-77
            serveMainHtmlPage()
        }
    }

    private fun Route.serveMainHtmlPage() {
        // TODO további oldalakat is kiszolgálni, ne csak a root-ot
        // TODO átnézni: https://gist.github.com/hal0gen/5852bd9db240c477f20c
        // TODO fontawesome CSS beégetve
        val title = "PWA" // TODO paraméterként + i18n
        val applicationFilePath = "/app/pwa/js/app.js" // TODO konfigurálható

        get {
            val initialWebAppClientEnvironmentData = with(call) {
                InitialWebAppClientEnvironmentData(
                    webAppServerEnvironmentProvider.localeId,
                    webAppServerEnvironmentProvider.authenticationStatus
                )
            }

            // TODO html/lang
            // TODO theme-color
            // TODO további meta tag-ek
            // TODO service worker registration in 'load' event: https://web.dev/articles/service-workers-registration
            // TODO splash
            // TODO workaround using 'originalFetch': https://youtrack.jetbrains.com/issue/KTOR-539/Ability-to-use-browser-cookie-storage

            // TODO roboto font beégetve
            val serviceWorkerWebPath = "/app/pwa/js/sw.js" // TODO konfigurálható
            call.respondText(
                """
                    <!doctype html>
                    <html lang="hu">
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, height=device-height, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
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
                                <style>
                                @import url('https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,100;0,300;0,400;0,500;0,700;0,900;1,100;1,300;1,400;1,500;1,700;1,900&display=swap');
                                </style>
                                    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
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
