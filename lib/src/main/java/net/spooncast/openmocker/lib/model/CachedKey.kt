package net.spooncast.openmocker.lib.model

import kotlinx.serialization.Serializable

@Serializable
data class CachedKey(
    val method: String,
    val path: String
)