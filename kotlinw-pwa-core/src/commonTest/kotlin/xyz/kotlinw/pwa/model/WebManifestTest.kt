package xyz.kotlinw.pwa.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.encodeToString
import xyz.kotlinw.pwa.model.WebManifest.Display.Standalone
import xyz.kotlinw.pwa.model.WebManifest.ImageResource
import xyz.kotlinw.pwa.model.WebManifest.ImageResourceSize
import xyz.kotlinw.pwa.model.WebManifest.ImageResourceSizes
import xyz.kotlinw.pwa.model.WebManifest.ShortcutItem
import xyz.kotlinw.serialization.json.standardLongTermJson

class WebManifestTest {

    private val json = standardLongTermJson {
        prettyPrint = true
    }

    @Test
    fun testSerialization() {
        assertEquals(
            """
            {
                "name": "Application",
                "short_name": "App",
                "icons": [
                    {
                        "src": "/icon/app-icon-vector.svg",
                        "type": "image/svg+xml",
                        "sizes": "512x512"
                    },
                    {
                        "src": "/icon/app-icon-192.png",
                        "type": "image/png",
                        "sizes": "192x192",
                        "purpose": "maskable"
                    },
                    {
                        "src": "/icon/app-icon-512.png",
                        "type": "image/png",
                        "sizes": "512x512",
                        "purpose": "maskable"
                    }
                ],
                "start_url": "/?source=pwa",
                "display": "standalone",
                "theme_color": "black",
                "background_color": "white",
                "scope": "/",
                "description": "Application description",
                "shortcuts": [
                    {
                        "name": "Kedvencek",
                        "short_name": "Kedvencek",
                        "description": "Kedvenc elemek listája",
                        "url": "/favorites?source=pwa",
                        "icons": [
                            {
                                "src": "/icon/shortcut-favorites-icon.svg",
                                "type": "image/svg+xml",
                                "sizes": "512x512"
                            }
                        ]
                    },
                    {
                        "name": "Felhasználói fiók",
                        "short_name": "Felhasználói fiók",
                        "description": "Felhasználói fiók megnyitása",
                        "url": "/profile?source=pwa",
                        "icons": [
                            {
                                "src": "/icon/shortcut-profile-icon.svg",
                                "type": "image/svg+xml",
                                "sizes": "512x512"
                            }
                        ]
                    }
                ]
            }
            """.trimIndent(),
            json.encodeToString(
                WebManifest(
                    shortName = "App",
                    name = "Application",
                    startUrl = "/?source=pwa",
                    backgroundColor = "white",
                    display = Standalone,
                    scope = "/",
                    themeColor = "black",
                    description = "Application description",
                    icons = listOf(
                        ImageResource(
                            sizes = ImageResourceSizes(ImageResourceSize(512, 512)),
                            src = "/icon/app-icon-vector.svg",
                            type = "image/svg+xml"
                        ),
                        ImageResource(
                            sizes = ImageResourceSizes(ImageResourceSize(192, 192)),
                            src = "/icon/app-icon-192.png",
                            type = "image/png",
                            purpose = "maskable"
                        ),
                        ImageResource(
                            sizes = ImageResourceSizes(ImageResourceSize(512, 512)),
                            src = "/icon/app-icon-512.png",
                            type = "image/png",
                            purpose = "maskable"
                        )
                    ),
                    shortcuts = listOf(
                        ShortcutItem(
                            name = "Kedvencek",
                            shortName = "Kedvencek",
                            description = "Kedvenc elemek listája",
                            url = "/favorites?source=pwa",
                            icons = listOf(
                                ImageResource(
                                    sizes = ImageResourceSizes(ImageResourceSize(512, 512)),
                                    src = "/icon/shortcut-favorites-icon.svg",
                                    type = "image/svg+xml"
                                )
                            )
                        ),
                        ShortcutItem(
                            name = "Felhasználói fiók",
                            shortName = "Felhasználói fiók",
                            description = "Felhasználói fiók megnyitása",
                            url = "/profile?source=pwa",
                            icons = listOf(
                                ImageResource(
                                    sizes = ImageResourceSizes(ImageResourceSize(512, 512)),
                                    src = "/icon/shortcut-profile-icon.svg",
                                    type = "image/svg+xml"
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}
