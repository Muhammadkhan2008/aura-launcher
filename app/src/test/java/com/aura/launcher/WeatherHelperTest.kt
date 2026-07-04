package com.aura.launcher

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URLConnection
import java.net.URLStreamHandler
import java.net.URLStreamHandlerFactory

object TestStreamHandlerFactory : URLStreamHandlerFactory {
    var mockConnection: HttpURLConnection? = null
    override fun createURLStreamHandler(protocol: String?): URLStreamHandler? {
        if (protocol == "https" || protocol == "http") {
            return object : URLStreamHandler() {
                override fun openConnection(u: URL?): URLConnection {
                    return mockConnection ?: throw IllegalStateException("mockConnection not set")
                }
            }
        }
        return null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class WeatherHelperTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun setupClass() {
            try {
                URL.setURLStreamHandlerFactory(TestStreamHandlerFactory)
            } catch (e: Error) {
                // Factory already set
            }
        }
    }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        TestStreamHandlerFactory.mockConnection = null
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testHasLocationPermission_Granted() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED

        val result = WeatherHelper.hasLocationPermission(context)
        assertTrue(result)
    }

    @Test
    fun testHasLocationPermission_Denied() {
        val context = mockk<Context>()
        every { context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_DENIED

        val result = WeatherHelper.hasLocationPermission(context)
        assertFalse(result)
    }

    @Test
    fun testGetWeather_NoPermissionReturnsCached() = runTest {
        val context = mockk<Context>()
        val sharedPrefs = mockk<SharedPreferences>()

        every { context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_DENIED
        every { context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE) } returns sharedPrefs

        // Mock AuraPrefs cached values
        every { sharedPrefs.getInt("last_weather_temp", 999) } returns 25
        every { sharedPrefs.getInt("last_weather_code", -1) } returns 0
        every { sharedPrefs.getString("last_weather_desc", "") } returns "Clear"
        every { sharedPrefs.getString("last_weather_emoji", "") } returns "☀️"

        val result = WeatherHelper.getWeather(context)

        assertNotNull(result)
        assertEquals(25, result?.tempC)
        assertEquals(0, result?.code)
        assertEquals("Clear", result?.description)
        assertEquals("☀️", result?.emoji)
    }

    @Test
    fun testGetWeather_NoPermissionNoCacheReturnsNull() = runTest {
        val context = mockk<Context>()
        val sharedPrefs = mockk<SharedPreferences>()

        every { context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_DENIED
        every { context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE) } returns sharedPrefs

        // Mock AuraPrefs returning 999 for temp means no cache
        every { sharedPrefs.getInt("last_weather_temp", 999) } returns 999

        val result = WeatherHelper.getWeather(context)

        assertNull(result)
    }

    @Test
    fun testGetWeather_SuccessNetworkCall() = runTest {
        val context = mockk<Context>()
        val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
        val sharedPrefsEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        val locationManager = mockk<LocationManager>()

        every { context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE) } returns sharedPrefs
        every { sharedPrefs.edit() } returns sharedPrefsEditor

        // Mock Location
        val location = mockk<Location>()
        every { location.latitude } returns 40.7128
        every { location.longitude } returns -74.0060
        every { location.accuracy } returns 10f
        every { locationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns location

        val mockConnection = mockk<HttpURLConnection>()
        every { mockConnection.connectTimeout = any() } just Runs
        every { mockConnection.readTimeout = any() } just Runs
        every { mockConnection.responseCode } returns 200

        val jsonResponse = """
            {
                "current_weather": {
                    "temperature": 22.5,
                    "weathercode": 1
                }
            }
        """.trimIndent()
        every { mockConnection.inputStream } returns ByteArrayInputStream(jsonResponse.toByteArray())

        TestStreamHandlerFactory.mockConnection = mockConnection

        val result = WeatherHelper.getWeather(context)

        assertNotNull(result)
        assertEquals(22, result?.tempC)
        assertEquals(1, result?.code)
        assertEquals("Partly cloudy", result?.description)
        assertEquals("⛅", result?.emoji)
    }

    @Test
    fun testGetWeather_NetworkErrorReturnsCached() = runTest {
        val context = mockk<Context>()
        val sharedPrefs = mockk<SharedPreferences>()
        val locationManager = mockk<LocationManager>()

        every { context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) } returns PackageManager.PERMISSION_GRANTED
        every { context.getSystemService(Context.LOCATION_SERVICE) } returns locationManager
        every { context.getSharedPreferences("aura_settings", Context.MODE_PRIVATE) } returns sharedPrefs

        // Mock Location
        val location = mockk<Location>()
        every { location.latitude } returns 40.7128
        every { location.longitude } returns -74.0060
        every { location.accuracy } returns 10f
        every { locationManager.getProviders(true) } returns listOf(LocationManager.GPS_PROVIDER)
        every { locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) } returns location

        val mockConnection = mockk<HttpURLConnection>()
        every { mockConnection.connectTimeout = any() } just Runs
        every { mockConnection.readTimeout = any() } just Runs
        every { mockConnection.responseCode } returns 500

        TestStreamHandlerFactory.mockConnection = mockConnection

        // Mock Cache
        every { sharedPrefs.getInt("last_weather_temp", 999) } returns 15
        every { sharedPrefs.getInt("last_weather_code", -1) } returns 3
        every { sharedPrefs.getString("last_weather_desc", "") } returns "Partly cloudy"
        every { sharedPrefs.getString("last_weather_emoji", "") } returns "⛅"

        val result = WeatherHelper.getWeather(context)

        assertNotNull(result)
        assertEquals(15, result?.tempC)
        assertEquals(3, result?.code)
        assertEquals("Partly cloudy", result?.description)
        assertEquals("⛅", result?.emoji)
    }
}
