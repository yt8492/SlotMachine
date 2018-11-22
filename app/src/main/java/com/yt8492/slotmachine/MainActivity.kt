package com.yt8492.slotmachine

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import java.nio.BufferUnderflowException
import kotlin.random.Random

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

    data class Slot(var running: Boolean, var number: Int)
    private var slotRunning = false
    private var canTouch =true
    private var canWrite = true
    private var thread: Thread? = null
    private val slotList = List(4){Slot(false, 0)}
    private var touchCnt = 0

    private val segment by lazy {
        RainbowHat.openDisplay().apply {
            setBrightness((Ht16k33.HT16K33_BRIGHTNESS_MAX))
            setEnabled(true)
        }
    }
    private val buttonA by lazy {
        RainbowHat.openButtonA()
    }
    private val buttonB by lazy {
        RainbowHat.openButtonB()
    }
    private val buttonC by lazy {
        RainbowHat.openButtonC()
    }
    private val ledStrip by lazy {
        RainbowHat.openLedStrip().apply {
            brightness = 1
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        log("start activity")
        buttonA.apply {
            setOnButtonEventListener { _, _ ->
                log("touch A")
                touchCnt++
                if (slotRunning && touchCnt % 2 == 1 && canTouch) {
                    canTouch = false
                    slotList.find { it.running }
                            ?.let {
                                it.running = false
                                if (it == slotList.last()) {
                                    slotRunning = false
                                }
                            }
                    canTouch = true
                }
            }
        }
        buttonB.apply {
            setOnButtonEventListener { _, _ ->
                log("touch B")
                if (slotRunning && canTouch) {
                    slotList.filter { it.running }
                            .forEach {
                                it.running = false
                                it.number = Random.nextInt(10)
                            }
                    log("ButtonB")
                    slotRunning = false
                }
            }
        }
        buttonC.apply {
            setOnButtonEventListener { _, _ ->
                log("touch C")
                if (!slotRunning) {
                    slotList.forEach {
                        it.running = true
                        it.number = Random.nextInt(10)
                    }
                    log("ButtonC")
                    slotRunning = true
                }
            }
        }
        thread = Thread(this)
        thread?.start()
        setSegmentValue()
    }

    private fun setSegmentValue() {
        if (canWrite) {
            canWrite = false
            try {
                val displayValue = slotList.map { it.number }.joinToString("")
                segment.display(displayValue)
            } catch (e: BufferUnderflowException) {
                e.printStackTrace()
            }
            canWrite = true
        }
    }

    private fun checkSlotResult() {
        if (slotList.all { slotList.first() == it }) {
            val rainbow = IntArray(RainbowHat.LEDSTRIP_LENGTH){Color.HSVToColor(127, floatArrayOf(it * 360f / RainbowHat.LEDSTRIP_LENGTH, 1f, 1f))}
            ledStrip.write(rainbow)
            Thread.sleep(2000)
            rainbow.fill(0, 0, rainbow.size)
            ledStrip.write(rainbow)

        }
    }

    override fun run() {
        while (true) {
            if (slotRunning) {
                slotList.filter { it.running }
                        .forEach { it.number = (it.number + 1) % 10 }
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