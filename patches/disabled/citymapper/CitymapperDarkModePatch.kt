package app.shinicell.morphepatches.patches.citymapper

import app.morphe.patcher.patch.resourcePatch
import app.shinicell.morphepatches.patches.shared.Constants.COMPATIBILITY_CITYMAPPER
import org.w3c.dom.Element

@Suppress("unused")
val citymapperDarkModePatch = resourcePatch(
    name = "Citymapper dark mode",
    description = "Opts Citymapper's light theme into Android Force Dark.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_CITYMAPPER)

    execute {
        // Force Dark is a theme item, not an <application> manifest attribute. The app's
        // activities inherit AppBaseTheme through AppDeviceVersionTheme/AppTheme.
        document("res/values/styles.xml").use { styles ->
            val styleElements = styles.getElementsByTagName("style")
            val baseTheme = (0 until styleElements.length)
                .map { styleElements.item(it) as Element }
                .firstOrNull { it.getAttribute("name") == "AppBaseTheme" }
                ?: error("Citymapper AppBaseTheme was not found")

            val children = baseTheme.childNodes
            val forceDarkItem = (0 until children.length)
                .mapNotNull { children.item(it) as? Element }
                .firstOrNull {
                    it.tagName == "item" && it.getAttribute("name") == "android:forceDarkAllowed"
                }
                ?: styles.createElement("item").also {
                    it.setAttribute("name", "android:forceDarkAllowed")
                    baseTheme.appendChild(it)
                }
            forceDarkItem.textContent = "true"
        }

        // Citymapper 11.59.3 ships a values-v29 override that explicitly disables
        // Force Dark for JustRideSDKTheme. On Android 10+, that higher-priority
        // resource wins over AppBaseTheme, so update the opt-out as well.
        document("res/values-v29/styles.xml").use { styles ->
            val styleElements = styles.getElementsByTagName("style")
            for (index in 0 until styleElements.length) {
                val style = styleElements.item(index) as Element
                val children = style.childNodes
                for (childIndex in 0 until children.length) {
                    val item = children.item(childIndex) as? Element ?: continue
                    if (
                        item.tagName == "item" &&
                        item.getAttribute("name") == "android:forceDarkAllowed" &&
                        item.textContent.trim() == "false"
                    ) {
                        item.textContent = "true"
                    }
                }
            }
        }
    }
}
