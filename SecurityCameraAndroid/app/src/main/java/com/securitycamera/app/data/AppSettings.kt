package com.securitycamera.app.data

data class AppSettings(
    val baseUrl: String = "",
    val functionKey: String = "",
    val notifyEmail: String = "",
    val roiLeft: Float = 0f,
    val roiTop: Float = 0f,
    val roiWidth: Float = 1f,
    val roiHeight: Float = 1f,
    val enrolledSubjects: Set<String> = emptySet(),
)

/** Normalized ROI covers the full frame — overlay matches preview, no guide rectangle needed. */
fun AppSettings.roiCoversFullNormalizedFrame(): Boolean {
    val e = 0.02f
    return roiLeft <= e &&
        roiTop <= e &&
        roiWidth >= 1f - e &&
        roiHeight >= 1f - e
}
