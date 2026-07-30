package com.inkwisp.app.model

fun applicationLocaleTags(preference: String?): String = when (preference) {
    null, "system" -> ""
    else -> preference
}
