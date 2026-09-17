package com.guanguanhua.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {
    @Test
    fun blankStoredUrlFallsBackToProduction() {
        assertEquals("http://8.153.195.112:8080", ApiConfig.resolvedServerUrl(null))
        assertEquals("http://8.153.195.112:8080", ApiConfig.resolvedServerUrl("  "))
        assertEquals("http://8.153.195.112:8080", ApiConfig.resolvedServerUrl("/"))
    }

    @Test
    fun storedUrlIsNormalizedAndKept() {
        assertEquals("http://10.0.2.2:8080", ApiConfig.resolvedServerUrl("http://10.0.2.2:8080/"))
        assertEquals("http://192.168.1.8:8080", ApiConfig.resolvedServerUrl("  http://192.168.1.8:8080  "))
    }
}
