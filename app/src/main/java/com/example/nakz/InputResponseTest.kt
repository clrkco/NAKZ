package com.example.nakz

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.input_response.*

import java.io.IOException
import java.lang.Exception
import java.util.*

class InputResponseTest : Activity(), SensorEventListener {

    //compass
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    var degreesOrientationAngles = DoubleArray(3)

    var init_y_constant: Double = 0.0
    var init_z_constant: Double = 0.0
    var new_z_constant: Double = 0.0     //z constant to be added to z orientation
    var new_y_constant: Double = 0.0    //y constant to be added to y orientation
    var add_or_sub_z: Boolean = false //true add, false sub
    var add_or_sub_y: Boolean = false //true add, false sub

    companion object {
        var timerSecs = (2000..6000).random()
        lateinit var timer: CountDownTimer
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var address: String
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.input_response)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        startBtn.setOnClickListener { start() }
        pauseBtn.setOnClickListener { pause() }
        resetBtn.setOnClickListener { reset() }

        try {
            address = intent.getStringExtra(BluetoothConnect.EXTRA_ADDRESS)!!
        } catch (e: Exception) {
            Log.e("onCreate", "address not found.")
        }
//        timer = object : CountDownTimer(timerSecs.toLong(), 1000) {
//            override fun onFinish() {
//                this.cancel()
//                timerSecs = (2000..6000).random()
//                Log.e("OnFinish", "Start again")
//                timer.start()
//                imageButton2.setBackgroundResource(R.drawable.scharlotte)
//                object : CountDownTimer(1000, 1000) {
//                    override fun onFinish() {
//                        this.cancel()
//                        imageButton2.setBackgroundResource(R.drawable.eyes)
//                    }
//
//                    override fun onTick(p0: Long) {
//                    }
//                }.start()
//            }
//
//            override fun onTick(p0: Long) {
//                textView2.text = p0.toString()
//            }
//        }
        ConnectToDevice(this).execute()

    }
    override fun onBackPressed() {
        super.onBackPressed()
        disconnect()
        val intent = Intent(this, BluetoothConnect::class.java)
        startActivity(intent)
    }

    private fun disconnect() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.close()
                bluetoothSocket = null
                isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private fun sendCommand(input: String) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private class ConnectToDevice(c: Context) :
        AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context = c
//        private var blinker = timer

        override fun onPreExecute() {
            super.onPreExecute()
            progress = ProgressDialog.show(context, "Connecting", "Please wait.")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (bluetoothSocket == null || !isConnected) {
                    bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(address)
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                val t = Toast.makeText(context, "Could not connect", Toast.LENGTH_SHORT)
                t.show()
//                blinker.start()
                isConnected = false
            } else {
                //send init servos to arduino
                isConnected = true
            }
            progress.dismiss()

        }
    }

    fun start() {
        Log.e("Sent: ", "Sent Command")
        sendCommand("~d_${200}_${315}_#!")
//        timerSecs = (3000..7000).random()

    }

    fun pause() {
        sendCommand("~d_${900}_${315}_#!")
//        textView2.text = textView2.text.toString()
//        textView2.text = (500 - textView2.text.toString().toInt()).toString()
////        timer.cancel()
        timer = object : CountDownTimer(7000, 20) {
            override fun onFinish() {
                this.cancel()
//                Log.e("OnFinish", "Start again")
//                timer.start()
            }

            override fun onTick(p0: Long) {
                Log.i("Angle", getAngle(0).toString())
//                textView2.text =
//                    p0.toString()//To change body of created functions use File | Settings | File Templates.
            }

        }
        timer.start()
    }

    fun reset() {
        calibrateSensors()
    }

    fun getAngle(index: Int): Float {
        //0 for z axis, 2 for y axis
        //additional minDegrees(360f,*angle*) in order to invert coordinate system for AX12 = ID 14
        val cur = degreesOrientationAngles[index]
        if (index == 0) {
//            return if (add_or_sub_z) {
//                addDegrees(cur.toFloat(), new_z_constant.toFloat())
//            } else {
//                minDegrees(cur.toFloat(), new_z_constant.toFloat())
//            }
            return if (add_or_sub_z) {
                minDegrees(360f, addDegrees(cur.toFloat(), new_z_constant.toFloat()))
            } else {
                minDegrees(360f, minDegrees(cur.toFloat(), new_z_constant.toFloat()))
            }
        } else if (index == 2) {
            return if (add_or_sub_y) {
                addDegrees(cur.toFloat(), new_y_constant.toFloat())
            } else {
                minDegrees(cur.toFloat(), new_y_constant.toFloat())
            }
        }
        return 0f
    }

    private fun addDegrees(a: Float, b: Float): Float {
        var temp = a + b
        if (temp > 359)
            temp -= 360
        return temp
    }

    private fun minDegrees(a: Float, b: Float): Float {
        var temp = a - b
        if (temp < 0)
            temp += 360
        return temp
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onResume() {
        super.onResume()

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.

        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }
    override fun onSensorChanged(event: SensorEvent) {
        textView2.text = getAngle(0).toString()
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }
        updateOrientationAngles()
        orientationAngles.forEachIndexed { index, a ->
            if (orientationAngles[index] < 0)
                degreesOrientationAngles[index] = a * 57.2958 + 360
            else
                degreesOrientationAngles[index] = a * 57.2958
        }
    }

    private fun calibrateSensors() {
        //210f to compensate when inverting the Z- coordinate map in order to properly send the right angles to the Dynamixel AX-12+
        //150f (512 in bits) is the middle (starting angle of AX-12+). When inverted, needs to be subtracted by 210 => 150f
        init_z_constant = degreesOrientationAngles[0]
        init_y_constant = degreesOrientationAngles[2]

        if (init_z_constant < 150f) {
            new_z_constant = 210f - init_z_constant
            add_or_sub_z = true
        } else if (init_z_constant > 150f) {
            new_z_constant = init_z_constant - 210f
            add_or_sub_z = false
        }
//        if (init_z_constant < 150f) {
//            new_z_constant = 150f - init_z_constant
//            add_or_sub_z = true
//        } else if (init_z_constant > 150f) {
//            new_z_constant = init_z_constant - 150f
//            add_or_sub_z = false
//        }

        if (init_y_constant < 150f) {
            new_y_constant = 150f - init_y_constant
            add_or_sub_y = true
        } else if (init_y_constant > 150f) {
            new_y_constant = init_y_constant - 150f
            add_or_sub_y = false
        }
    }

    private fun updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )

        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        // "mOrientationAngles" now has up-to-date information.
    }
}