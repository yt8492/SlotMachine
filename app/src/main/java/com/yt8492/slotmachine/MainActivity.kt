package com.yt8492.slotmachine

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import java.nio.BufferUnderflowException
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

    private val segment = RainbowHat.openDisplay().apply {
        setBrightness((Ht16k33.HT16K33_BRIGHTNESS_MAX))
        setEnabled(true)
    }
    private val buttonA = RainbowHat.openButtonA().apply {
        setOnButtonEventListener { _, _ ->
            log("touch A")
            touchCnt++
            if (slotRunning && touchCnt % 2 == 1 && canTouch) {
                canTouch = false
                for (i in slotArray.indices) {
                    if (slotArray[i].first) {
                        slotArray[i] = Pair(false, slotArray[i].second)
                        log("ButtonA")
                        if (i == slotArray.lastIndex) {
                            slotRunning = false
                        }
                        break
                    }
                }
                canTouch = true
            }
        }
    }
    private val buttonB = RainbowHat.openButtonB().apply {
        setOnButtonEventListener { _, _ ->
            log("touch B")
            if (slotRunning && canTouch) {
                for (i in slotArray.indices) {
                    if (slotArray[i].first) {
                        slotArray[i] = false to Random().nextInt(10)
                    }
                }
                log("ButtonB")
                slotRunning = false
            }
        }
    }
    private val buttonC = RainbowHat.openButtonC().apply {
        setOnButtonEventListener { _, _ ->
            log("touch C")
            if (!slotRunning) {
                for (i in slotArray.indices) {
                    slotArray[i] = true to Random().nextInt(10)
                }
                log("ButtonC")
                slotRunning = true
            }
        }
    }
    private val ledStrip = RainbowHat.openLedStrip().apply {
        brightness = 1
    }
    private var slotRunning = false
    private var canTouch =true
    private var canWrite = true
    private var thread: Thread? = null
    private val slotArray = Array(4) {false to 0}
    private var touchCnt = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        thread = Thread(this)
        thread?.start()
        setSegmentValue()
    }

    private fun setSegmentValue() {
        if (canWrite) {
            canWrite = false
            try {
                val displayValue = slotArray.map { it.second }.joinToString("")
                segment.display(displayValue)
            } catch (e: BufferUnderflowException) {
                e.printStackTrace()
            }
            canWrite = true
        }
    }

    private fun checkSlotResult() {
        if (slotArray.all { it == slotArray.first() }) {
            val rainbow = IntArray(RainbowHat.LEDSTRIP_LENGTH){Color.HSVToColor(127, floatArrayOf(it * 360f / RainbowHat.LEDSTRIP_LENGTH, 1f, 1f))}
            ledStrip.write(rainbow)
            Thread.sleep(2000)
            rainbow.fill(0)
            ledStrip.write(rainbow)
        }
    }

    override fun run() {
        while (true) {
            if (slotRunning) {
                for (i in slotArray.indices) {
                    if (slotArray[i].first) {
                        slotArray[i] = true to (slotArray[i].second + 1) % 10
                    }
                }
                log("Thread")
                setSegmentValue()
            } else {
                checkSlotResult()
            }
            Thread.sleep(125)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        segment.close()
        buttonA.close()
        buttonB.close()
        buttonC.close()
        ledStrip.close()
        thread = null
    }

}

fun log(msg: String) {
    Log.d("MyApp", msg)
}