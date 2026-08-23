package com.moyu.reader.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test fun coldStartup() = measure(StartupMode.COLD)
    @Test fun warmStartup() = measure(StartupMode.WARM)

    private fun measure(mode: StartupMode) = benchmarkRule.measureRepeated(
        packageName = "com.moyu.reader",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.Partial(),
        startupMode = mode,
        iterations = 5,
        setupBlock = { pressHome() },
    ) { startActivityAndWait() }
}
