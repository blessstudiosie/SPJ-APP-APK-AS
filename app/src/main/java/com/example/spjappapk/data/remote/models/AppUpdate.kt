package com.example.spjappapk.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdate(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String,
    @SerialName("release_notes") val releaseNotes: String? = "",
    @SerialName("download_url") val downloadUrl: String,
    @SerialName("is_mandatory") val isMandatory: Boolean = false,
    @SerialName("platform") val platform: String = "android",
    @SerialName("updated_at") val updatedAt: String? = null // Tambahan kolom updated_at
)
