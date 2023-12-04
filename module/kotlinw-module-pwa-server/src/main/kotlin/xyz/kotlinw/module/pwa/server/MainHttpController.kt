package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType.Text
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer

@Component
class MainHttpController(private val deploymentMode: DeploymentMode): KtorServerApplicationConfigurer() {

    override fun Context.setup() {
        application.routing {
            route("/") {
                // TODO további oldalakat is kiszolgálni, ne csak a root-ot
                // TODO átnézni: https://gist.github.com/hal0gen/5852bd9db240c477f20c
                val title = "PWA" // TODO paraméterként + i18n
                val applicationFilePath = if (deploymentMode == Development)
                    "/appman-hub-webapp.js" // TODO általánosabb, pl. app.js
                else
                    TODO()
                get {
                    // TODO html/lang
                    // TODO theme-color
                    // TODO további meta tag-ek
                    // TODO service worker registration in 'load' event: https://web.dev/articles/service-workers-registration
                    // TODO splash

                    val serviceWorkerWebPath = "/app/pwa/js/sw.js" // TODO konfigurálható
                    call.respondText(
                        """
                            <!doctype html>
                            <html lang="hu">
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width; initial-scale=1.0">
                                    <meta name="theme-color" content="#000000" />
                                    <meta http-equiv="X-UA-Compatible" content="ie=edge">
                                    <link rel="manifest" href="/manifest.webmanifest">
                                    <title>$title</title>
                                    <script>
                                        if ('serviceWorker' in navigator) {
                                            navigator.serviceWorker.register('$serviceWorkerWebPath');
                                        }
                                    </script>
                                </head>
                                <body>
                                asdasdsa
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
}
