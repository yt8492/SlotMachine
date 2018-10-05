package com.yt8492.slotmachine

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import java.util.*

/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity(), Runnable {

    val segment = RainbowHat.openDisplay()
    val buttonA = RainbowHat.openButtonA()
    val buttonB = RainbowHat.openButtonB()
    val buttonC = RainbowHat.openButtonC()
    val ledStrip = RainbowHat.openLedStrip()
    var slotRunning = false
    var thread: Thread? = Thread(this)
    var slotArray = arrayOf(Pair(false, 0), Pair(false, 0), Pair(false, 0), Pair(false, 0))
    var touchCnt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        thread?.start()
        segment.setBrightness((Ht16k33.HT16K33_BRIGHTNESS_MAX))
        setSegmentValue()
        segment.setEnabled(true)
        buttonC.setOnButtonEventListener { _, _ ->
            log("touch C")
            if (!slotRunning) {
                for (i in slotArray.indices) {
                    slotArray[i] = Pair(true, Random().nextInt(10))
                }
                log("ButtonC")
                setSegmentValue()
                slotRunning = true
            }
        }
        buttonB.setOnButtonEventListener { _, _ ->
            log("touch B")
            if (slotRunning) {
                for (i in slotArray.indices) {
                    if (slotArray[i].first) {
                        slotArray[i] = Pair(false, Random().nextInt(10))
                    }
                }
                log("ButtonB")
                setSegmentValue()
                checkSlotResult()
                slotRunning = false
            }
        }
        buttonA.setOnButtonEventListener { _, _ ->
            log("touch A")
            touchCnt++
            if (slotRunning && touchCnt % 2 == 1) {
                for (i in slotArray.indices) {
                    if (slotArray[i].first) {
                        slotArray[i] = Pair(false, slotArray[i].second)
                        log("ButtonA")
                        setSegmentValue()
                        if (i == slotArray.lastIndex) {
                            slotRunning = false
                            checkSlotResult()
                        }
                        break
                    }
                }
            }
        }
    }

    private fun setSegmentValue() {
        val displayValue = buildString { append(slotArray[0].second, slotArray[1].second, slotArray[2].second, slotArray[3].second) }
        segment.display(displayValue)
    }

    private fun checkSlotResult() {
        if (slotArray[0] == slotArray[1] &&
                slotArray[1] ==slotArray[2] &&
                 slotArray[2] == slotArray[3]) {
            ledStrip.brightness = 1
            var rainbow = IntArray(RainbowHat.LEDSTRIP_LENGTH)
            for (i in rainbow.indices) {
                rainbow[i] = Color.HSVToColor(127, floatArrayOf(i * 360f / rainbow.size, 1f, 1f))
            }
            ledStrip.write(rainbow)
            Thread.sleep(2000)
            rainbow.fill(0, 0, rainbow.size)
            ledStrip.write(rainbow)
        }
    }

    override fun run() {
        while (true) {
            if (slotRunning) {
                for (i in slotArray.indices) {
                    if (slotArray[i].first) {
                        slotArray[i] = Pair(true, (slotArray[i].second + 1) % 10)
                    }
                }
                log("Thread")
                setSegmentValue()
            }
            Thread.sleep(100)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        segment.close()
        ledStrip.close()
        thread = null
    }

}

fun log(msg: String) {
    Log.d("MyApp", msg)
}