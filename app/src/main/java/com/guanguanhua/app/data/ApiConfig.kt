package com.guanguanhua.app.data

object ApiConfig {
    const val DEFAULT_SERVER = "http://8.153.195.112:8080"

    fun resolvedServerUrl(stored: String?): String {
        val url = stored?.trim()?.trimEnd('/').orEmpty()
        return url.ifBlank { DEFAULT_SERVER }
    }
}
