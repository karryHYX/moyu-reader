package com.moyu.reader.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    @Test
    fun criticalUserJourney() = baselineProfileRule.collect(
        packageName = "com.moyu.reader",
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        val device = UiDevice.getInstance(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation())
        device.wait(Until.hasObject(By.text("我的书架")), 5_000)
        device.findObject(By.text("导入"))?.click()
        device.waitForIdle()
        device.findObject(By.text("书架"))?.click()
        device.waitForIdle()
        device.findObject(By.textContains("山海之间"))?.click()
        device.waitForIdle()
        device.findObject(By.textContains("继续阅读"))?.click()
        device.waitForIdle()
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.waitForIdle()
        device.findObject(By.text("目录"))?.click()
        device.waitForIdle()
    }
}

