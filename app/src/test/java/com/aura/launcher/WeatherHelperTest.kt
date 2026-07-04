package com.aura.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherHelperTest {

    @Test
    fun testCodeToEmojiSunny() {
        assertEquals("☀️", WeatherHelper.codeToEmoji(0))
    }

    @Test
    fun testCodeToEmojiCloudy() {
        assertEquals("⛅", WeatherHelper.codeToEmoji(1))
        assertEquals("⛅", WeatherHelper.codeToEmoji(2))
        assertEquals("⛅", WeatherHelper.codeToEmoji(3))
    }

    @Test
    fun testCodeToEmojiFoggy() {
        assertEquals("🌫️", WeatherHelper.codeToEmoji(45))
        assertEquals("🌫️", WeatherHelper.codeToEmoji(48))
    }

    @Test
    fun testCodeToEmojiRainy() {
        assertEquals("🌧️", WeatherHelper.codeToEmoji(51))
        assertEquals("🌧️", WeatherHelper.codeToEmoji(55))
        assertEquals("🌧️", WeatherHelper.codeToEmoji(67))
    }

    @Test
    fun testCodeToEmojiSnowy() {
        assertEquals("❄️", WeatherHelper.codeToEmoji(71))
        assertEquals("❄️", WeatherHelper.codeToEmoji(75))
        assertEquals("❄️", WeatherHelper.codeToEmoji(77))
    }

    @Test
    fun testCodeToEmojiRainShowers() {
        assertEquals("🌦️", WeatherHelper.codeToEmoji(80))
        assertEquals("🌦️", WeatherHelper.codeToEmoji(81))
        assertEquals("🌦️", WeatherHelper.codeToEmoji(82))
    }

    @Test
    fun testCodeToEmojiThunderstorm() {
        assertEquals("⛈️", WeatherHelper.codeToEmoji(95))
        assertEquals("⛈️", WeatherHelper.codeToEmoji(96))
        assertEquals("⛈️", WeatherHelper.codeToEmoji(99))
    }

    @Test
    fun testCodeToEmojiFallback() {
        assertEquals("🌡️", WeatherHelper.codeToEmoji(-1))
        assertEquals("🌡️", WeatherHelper.codeToEmoji(100))
        assertEquals("🌡️", WeatherHelper.codeToEmoji(50))
    }
}
