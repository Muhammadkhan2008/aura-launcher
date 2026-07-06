package com.aura.launcher

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class GroqClientTest {

    @Test
    fun ask_returnsNoKey_whenApiKeyBlank() = runTest {
        assertTrue(GroqClient.ask(apiKey = "", prompt = "hi") is GroqClient.Result.NoKey)
        assertTrue(GroqClient.ask(apiKey = "   ", prompt = "hi") is GroqClient.Result.NoKey)
    }

    @Test
    fun result_successAndErrorCarryData() {
        val success = GroqClient.Result.Success("hello")
        val error = GroqClient.Result.Error("boom")

        assertTrue(success.text == "hello")
        assertTrue(error.message == "boom")
    }
}
