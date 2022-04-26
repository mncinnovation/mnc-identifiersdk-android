package id.mncinnovation.ocr.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt

class LightSensor(
    private val context: Context,
    private val listener: LightSensorListener
) {
    private var lightSensor: Sensor? = null

    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val lightSensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent?) {
            if (sensorEvent?.sensor?.type == Sensor.TYPE_LIGHT) {
                val currentLight: Int = sensorEvent.values[0].roundToInt()
                listener.onCurrentLightChanged(currentLight)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

    }

    init {
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        lightSensor?.apply {
            sensorManager.registerListener(
                lightSensorEventListener,
                lightSensor,
                SensorManager.SENSOR_DELAY_UI
            )
        }


    }

    fun closeSensor() {
        sensorManager.unregisterListener(lightSensorEventListener, lightSensor)
    }

    fun startDetectingSensor() {
        sensorManager.registerListener(
            lightSensorEventListener, lightSensor,
            SensorManager.SENSOR_DELAY_UI
        )
    }
}

interface LightSensorListener {
    fun onCurrentLightChanged(value: Int)
}