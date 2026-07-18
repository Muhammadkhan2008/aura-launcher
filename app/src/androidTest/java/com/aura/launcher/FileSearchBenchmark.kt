package com.aura.launcher

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

@RunWith(AndroidJUnit4::class)
class FileSearchBenchmark {

    @Test
    fun benchmarkSearch() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Warmup
        for (i in 0..5) {
            FileSearch.search(context, "a")
        }

        // Benchmark
        var totalTime = 0L
        val iterations = 50
        for (i in 0 until iterations) {
            val time = measureTimeMillis {
                FileSearch.search(context, "a")
            }
            totalTime += time
        }

        println("BENCHMARK FileSearch.search average time: ${totalTime / iterations} ms")
    }
}
