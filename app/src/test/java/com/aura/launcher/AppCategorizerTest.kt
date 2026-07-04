package com.aura.launcher

import android.content.Context
import org.junit.Test
import org.junit.Assert.assertEquals
import org.mockito.Mockito

class AppCategorizerTest {
    @Test
    fun testGroupApps_emptyList() {
        val mockContext = Mockito.mock(Context::class.java)
        val result = AppCategorizer.groupApps(mockContext, emptyList())
        assertEquals(emptyMap<AppCategorizer.Category, List<AppInfo>>(), result)
    }
}
