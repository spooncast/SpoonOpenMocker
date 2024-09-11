package net.spooncast.openmocker.lib.model

import kotlinx.serialization.Serializable

@Serializable
internal data class CachedKey(
    val method: String,
    val path: String
)