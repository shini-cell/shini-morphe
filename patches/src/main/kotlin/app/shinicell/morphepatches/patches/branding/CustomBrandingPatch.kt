package app.shinicell.morphepatches.patches.branding

import app.morphe.patcher.patch.folderOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.shinicell.morphepatches.patches.shared.Constants.COMPATIBILITY_NAGRAM_XF
import org.w3c.dom.Element
import java.io.File

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val ADAPTIVE_ICON_NAME = "morphe_adaptive_icon_custom"
private const val TELEGRAM_ICON = "@mipmap/ic_launcher_dr"
private const val TELEGRAM_ROUND_ICON = "@mipmap/ic_launcher_dr_round"
private val iconDensities = listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")

@Suppress("unused")
val customBrandingPatch = resourcePatch(
    name = "Custom branding",
    description = "Changes the Nagram XF launcher name and optionally replaces its launcher icon.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_NAGRAM_XF)

    val appName by stringOption(
        key = "customName",
        title = "App name",
        description = "The display name shown under the launcher icon.",
        required = false,
    )
    val iconFolder by folderOption(
        key = "customIcon",
        title = "Custom icon",
        description = "Folder containing custom adaptive icon files.",
        required = false,
    )

    execute {
        val manifestFile = document("AndroidManifest.xml")
        manifestFile.use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as Element

            appName?.trim()?.takeIf(String::isNotEmpty)?.let { name ->
                application.setAttribute("android:label", name)
            }

            val customIconFolder = iconFolder?.trim()?.takeIf(String::isNotEmpty)
            val launcherIcon = if (customIconFolder != null) {
                installCustomIcon(File(customIconFolder))
                "@mipmap/$ADAPTIVE_ICON_NAME"
            } else {
                TELEGRAM_ICON
            }
            val roundLauncherIcon = if (customIconFolder != null) launcherIcon else TELEGRAM_ROUND_ICON

            application.setAttribute("android:icon", launcherIcon)
            application.setAttribute("android:roundIcon", roundLauncherIcon)

            val aliases = manifest.getElementsByTagName("activity-alias")
            for (index in 0 until aliases.length) {
                val alias = aliases.item(index) as Element
                // Icon aliases can override the application icon, even when only one alias is enabled.
                if (alias.getAttribute("android:name").endsWith("Icon")) {
                    alias.setAttribute("android:icon", launcherIcon)
                    alias.setAttribute("android:roundIcon", roundLauncherIcon)
                }
            }
        }
    }
}

private fun app.morphe.patcher.patch.ResourcePatchContext.installCustomIcon(source: File) {
    require(source.isDirectory) { "Custom icon folder does not exist: ${source.path}" }

    var copiedIcon = false
    iconDensities.forEach { density ->
        val sourceDensity = File(source, "mipmap-$density")
        if (!sourceDensity.isDirectory) return@forEach

        val outputDensity = get("res/mipmap-$density").apply { mkdirs() }
        listOf(
            "morphe_adaptive_background_custom.png",
            "morphe_adaptive_foreground_custom.png",
        ).forEach { fileName ->
            val input = File(sourceDensity, fileName)
            require(input.isFile) { "Missing custom icon file: ${input.path}" }
            input.copyTo(File(outputDensity, fileName), overwrite = true)
        }
        copiedIcon = true
    }
    require(copiedIcon) { "No custom icon density files were found in ${source.path}" }

    val suppliedMonochrome = File(source, "drawable/morphe_adaptive_monochrome_custom.xml")
    val monochromeAttribute = if (suppliedMonochrome.isFile) {
        val drawableDirectory = get("res/drawable").apply { mkdirs() }
        suppliedMonochrome.copyTo(File(drawableDirectory, suppliedMonochrome.name), overwrite = true)
        "<monochrome android:drawable=\"@drawable/morphe_adaptive_monochrome_custom\" />"
    } else {
        ""
    }

    val adaptiveIconDirectory = get("res/mipmap-anydpi-v26").apply { mkdirs() }
    File(adaptiveIconDirectory, "$ADAPTIVE_ICON_NAME.xml").writeText(
        """
        <?xml version="1.0" encoding="utf-8"?>
        <adaptive-icon xmlns:android="$ANDROID_NAMESPACE">
            <background android:drawable="@mipmap/morphe_adaptive_background_custom" />
            <foreground android:drawable="@mipmap/morphe_adaptive_foreground_custom" />
            $monochromeAttribute
        </adaptive-icon>
        """.trimIndent(),
    )
}
