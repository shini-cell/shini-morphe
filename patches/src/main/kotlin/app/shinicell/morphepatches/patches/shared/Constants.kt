package app.shinicell.morphepatches.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    val COMPATIBILITY_NAGRAM_XF = Compatibility(
        name = "Nagram XF",
        packageName = "fork.risin42.nagramx",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF5C4D,
        targets = listOf(
            AppTarget(
                version = "12.10.1-30dcd6c",
                versionCodes = mapOf(SupportedAbi.ARM64_V8A to 1251),
            ),
        ),
    )
}
