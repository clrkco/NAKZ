package com.example.nakz

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.cloud.dialogflow.v2beta1.SessionName
import com.google.cloud.dialogflow.v2beta1.SessionsClient
import com.otaliastudios.cameraview.Facing
import husaynhakeem.io.facedetector.FaceBoundsOverlay
import husaynhakeem.io.facedetector.FaceDetector
import husaynhakeem.io.facedetector.models.Frame
import husaynhakeem.io.facedetector.models.Size
import kotlinx.android.synthetic.main.compass_main.*
import kotlinx.android.synthetic.main.compass_main.cameraView
import kotlinx.android.synthetic.main.compass_main.facesBoundsOverlay
import java.io.IOException
import java.lang.Exception
import java.util.*

class MAIN : Activity(), SensorEventListener {
    //TTS
    private val uuid = UUID.randomUUID().toString()
    private var chatLayout: LinearLayout? = null
    private var queryEditText: EditText? = null

    // Java V2 Dialogflow
    private var sessionsClient: SessionsClient? = null
    private var session: SessionName? = null
    lateinit var mTTS: TextToSpeech

    val webIntent: Intent = Uri.parse("www.google.com").let { webpage ->
        Intent(Intent.ACTION_VIEW, webpage)
    }
    //Screen size
    private var width: Int = 0
    private var length: Int = 0

    //compass
    private lateinit var sensorManager: SensorManager
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)


    //FACE DETECTION MODULE
    private val faceDetector: FaceDetector by lazy {
        FaceDetector(facesBoundsOverlay)
    }


    companion object {
        private var degreesOrientationAngles = DoubleArray(3)
        var myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var bluetoothSocket: BluetoothSocket? = null
        lateinit var progress: ProgressDialog
        lateinit var bluetoothAdapter: BluetoothAdapter
        var isConnected: Boolean = false
        lateinit var address: String

        //sensor error margin
        const val margin_of_error = 10f

        //degrees per ms depending on voltage level (ENDLESS MODE PAN-TILT)
        const val deg_per_ms = 0.03f

        //map of object coordinates (Z,Y)
        var obj_coordinate_map: MutableMap<String, FloatArray> = mutableMapOf(
            "medicinebox" to floatArrayOf(82f, 72f),
            "aircon" to floatArrayOf(190f, 72f),
            "tv" to floatArrayOf(300f, 50f)
        )

        //degrees conversion rate
        val deg_from_px = 8.37f

        //offset (in px) for pan-tilt face detection
        val offset_x = 500f
        val offset_y = 200f

        //regulating sending of commands 
        var AllowSend: Boolean = true

        //for sensor calibration
        private var init_y_constant: Double = 0.0
        private var init_z_constant: Double = 0.0
        private var new_z_constant: Double = 0.0     //z constant to be added to z orientation
        private var new_y_constant: Double = 0.0    //y constant to be added to y orientation
        private var add_or_sub_z: Boolean = false //true add, false sub
        private var add_or_sub_y: Boolean = false //true add, false sub
        private var phone_to_servo_deg_errbitsz: Int = 25
        private var phone_to_servo_deg_errbitsy: Int = 0

        //motor positions
        private var pan_servo = 512
        private var tilt_servo = 512
        private var right_shoulder = 512
        private var left_shoulder = 512
        private var right_arm = 512
        private var left_arm = 512
        private var right_elbow = 512
        private var left_elbow = 512
    }


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compass_main)
        try {
            address = intent.getStringExtra(BluetoothConnect.EXTRA_ADDRESS)!!
        } catch (e: Exception) {
            Log.e("onCreate", "address not found.")
        }

        ConnectToDevice(this).execute()
        //compass
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //bt module
//        findObjectBtn.setOnClickListener { findObject(objectEntry.text.toString()) }
//        objectEntry.setOnFocusChangeListener { _: View, b: Boolean ->
//            if (b) {
//                objectEntry.text.clear()
//            } else
//                objectEntry.setText("Object Name")
//        }
//        setConstantBtn.setOnClickListener { calibrateSensors() }
//        DisconnectBtn.setOnClickListener { disconnect() }
//        regObjectBtn.setOnClickListener { registerObject(objectEntry.text.toString())}

        imageButton.setOnClickListener{
            Log.e("Press","press")
        }
        val display: Display = windowManager.defaultDisplay
        width = display.width
        length = display.height
//        Log.i("l and w", "$width $length")
        setupCamera()

    }

    //FACE DETECT MODULE
    private fun setupCamera() {
        cameraView.facing = Facing.FRONT
        cameraView.addFrameProcessor {
//            Log.i(
//                "orientation",
//                FaceBoundsOverlay.centerX.toString() + " " + FaceBoundsOverlay.centerY.toString()
//            )
//            Log.i(
//                "offset",
//                FaceBoundsOverlay.xOffset.toString() + " " + FaceBoundsOverlay.yOffset.toString()
//            )
            faceDetector.process(
                Frame(
                    data = it.data,
                    rotation = it.rotation,
                    size = Size(it.size.width, it.size.height),
                    format = it.format,
                    isCameraFacingBack = cameraView.facing == Facing.BACK
                )
            )
        }
    }

    // Bluetooth Module
    private fun sendCommand(input: String) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
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

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        override fun onPreExecute() {
            super.onPreExecute()
            progress = ProgressDialog.show(context, "Connecting", "Please wait.")
            AllowSend = false
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
                //Log.i("data","couldn't connect")
//                AllowSend = true //delete after testing

                Log.i("Before", "Before Thread 3000")
                //wait for motors to configure
                Thread.sleep(3000)
                Log.i("After", "After Thread 3000")
                //calibrate sensors
                init_z_constant = degreesOrientationAngles[0]
                init_y_constant = degreesOrientationAngles[2]

                if (init_z_constant < 150f) {
                    new_z_constant = 210f - init_z_constant
                    add_or_sub_z = true
                } else if (init_z_constant > 150f) {
                    new_z_constant = init_z_constant - 210f
                    add_or_sub_z = false
                }
                if (init_y_constant < 150f) {
                    new_y_constant = 150f - init_y_constant
                    add_or_sub_y = true
                } else if (init_y_constant > 150f) {
                    new_y_constant = init_y_constant - 150f
                    add_or_sub_y = false
                }
                Log.i("Calibrate Sensors", "Calibrated")

                Thread.sleep(3000)
                Log.i("After 1k", "After Thread 1k")
                isConnected = true
                AllowSend = true    //only allow sending after initializing servos
            } else {
                //send init servos to arduino
                var input = "z_"
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket!!.outputStream.write(input.toByteArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                //wait for motors to configure
                Thread.sleep(4000)

                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket!!.outputStream.write(input.toByteArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                //wait for motors to configure
                Thread.sleep(4000)
                //calibrate sensors
                init_z_constant = degreesOrientationAngles[0]
                init_y_constant = degreesOrientationAngles[2]

                if (init_z_constant < 150f) {
                    new_z_constant = 210f - init_z_constant
                    add_or_sub_z = true
                } else if (init_z_constant > 150f) {
                    new_z_constant = init_z_constant - 210f
                    add_or_sub_z = false
                }
                if (init_y_constant < 150f) {
                    new_y_constant = 150f - init_y_constant
                    add_or_sub_y = true
                } else if (init_y_constant > 150f) {
                    new_y_constant = init_y_constant - 150f
                    add_or_sub_y = false
                }
                Log.i("Calibrate Sensors", "Calibrated")

                tilt_servo = 250
                input = "~d_512_250_#!"
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket!!.outputStream.write(input.toByteArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                Thread.sleep(3000)
                isConnected = true
                AllowSend = true    //only allow sending after initializing servos
            }
            progress.dismiss()
        }
    }

    //END BLUETOOTH MODULE
    /* ----------------------------------------------------------------------------------*/
    //Object Pointing Module
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
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
        cameraView.start()
    }

    override fun onPause() {
        super.onPause()

        // Don't receive any more updates from either sensor.
        sensorManager.unregisterListener(this)
        cameraView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings aggs unit vectors.
    override fun onSensorChanged(event: SensorEvent) {
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
//
//        Log.i("AllowSend" , AllowSend.toString())
        if (AllowSend) {
            //degrees/Px conversion rate 8.37
            if (FaceBoundsOverlay.centerX > width / 2f + offset_x || FaceBoundsOverlay.centerX < width / 2f - offset_x ||
                FaceBoundsOverlay.centerY > length / 2f + offset_y || FaceBoundsOverlay.centerY < length / 2f - offset_y
            ) {
                //center x, center y
//                Log.i("width", (width / 2f + offset_x).toString())
//                Log.i("width", (width / 2f - offset_x).toString())
//                Log.i("length", (length / 2f + offset_y).toString())
//                Log.i("length", (length / 2f - offset_y).toString())
                if (FaceBoundsOverlay.centerX > width / 2f + offset_x) { //ccw
                    val diff_in_pixels = FaceBoundsOverlay.centerX - width / 2f
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    pan_servo += deg_from_pixels.toInt()
                } else {
                    val diff_in_pixels = width / 2f - FaceBoundsOverlay.centerX
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    pan_servo -= deg_from_pixels.toInt()
                }
                if (FaceBoundsOverlay.centerY > length / 2f + offset_y) {
                    val diff_in_pixels = FaceBoundsOverlay.centerY - length / 2f
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    tilt_servo -= deg_from_pixels.toInt()
                } else {
                    val diff_in_pixels = length / 2f - FaceBoundsOverlay.centerY
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    tilt_servo += deg_from_pixels.toInt()
                }
//                Log.i("isConnected" , isConnected.toString() + " " + AllowSend.toString())
                if (isConnected && AllowSend) { //change isConnected when Arduino is present
                    Log.i("sending command", "sending")
                    sendCommand("~d_${pan_servo}" + "_${tilt_servo}_#!")
                    AllowSend = false
                    object : CountDownTimer(1500, 1000) { //change if want to decrease interval
                        override fun onFinish() {
                            AllowSend = true
                            this.cancel()
                        }

                        override fun onTick(millisUntilFinished: Long) {
                        }
                    }.start()
//                    Log.e("AllowSend", AllowSend.toString())
                }
            }
        }


//        compass_txt.text = "z: " + getAngle(0).toString() + "\ny: " +
//               getAngle(2).toString()
//
//        determineObject(objectEntry.text.toString())
        /*Log.d("z: " + (degrees_orientationAngles[0]).toString() + "x: "+
                (degrees_orientationAngles[1]).toString()+ "y: " +
                (degrees_orientationAngles[2]).toString())*/
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
        if (init_y_constant < 150f) {
            new_y_constant = 150f - init_y_constant
            add_or_sub_y = true
        } else if (init_y_constant > 150f) {
            new_y_constant = init_y_constant - 150f
            add_or_sub_y = false
        }
    }

    private fun getAngle(index: Int): Float {
        //additional minDegrees(360f,*angle*) in order to invert coordinate system for AX12 = ID 14
        val cur = degreesOrientationAngles[index]
        if (index == 0) {
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

    private fun findObject(objectName: String) {
        //0.293 deg/bit - conversion from angle to bit for AX-12+
        var objName = objectName.toLowerCase().trim().replace("\\s".toRegex(), "")

        try {
            val objCoords = obj_coordinate_map[objName]
            val obj_z = objCoords?.get(0)
            val obj_y = objCoords?.get(1)
            val bit_z: Double
            bit_z = if (getAngle(0) < 150f) {
                obj_z!!.div(0.293) - phone_to_servo_deg_errbitsz
            } else {
                obj_z!!.div(0.293) + phone_to_servo_deg_errbitsz
            }

            val bit_y = obj_y!!.div(0.293) + phone_to_servo_deg_errbitsy


            Log.e("Sending Command: ", "Z: ${bit_z.toInt()}" + "Y: ${bit_y.toInt()}")
            pan_servo = bit_z.toInt()
            tilt_servo = bit_y.toInt()

            if (AllowSend) {
                sendCommand("~p_${pan_servo}" + "_${tilt_servo}_#!")
                AllowSend = false //give motors time to move before sending again
                object : CountDownTimer(5000, 1000) {
                    override fun onFinish() {
                        AllowSend = true
                        this.cancel()
                    }
                    override fun onTick(millisUntilFinished: Long) {
                    }
                }.start()
            }
            /* Using endless turn mode
            var degrees_turnZ = curZ - obj_z!!
            var turn_timeZ = 0.0
            if (degrees_turnZ > 0) {
                //degrees_turnZ is CCW
                val inv_degrees_turnZ = 360 - degrees_turnZ
                if(degrees_turnZ < inv_degrees_turnZ){
                    turn_timeZ = degrees_turnZ / deg_per_ms
                    sendCommand("z_ccw_$turn_timeZ")
                } else {
                    turn_timeZ = inv_degrees_turnZ / deg_per_ms
                    sendCommand("z_cw_$turn_timeZ")
                }
            } else if (degrees_turnZ < 0) {
                //degrees_turnZ is CW
                degrees_turnZ *= -1
                val inv_degrees_turnZ = 360 - degrees_turnZ
                if(degrees_turnZ < inv_degrees_turnZ){
                    turn_timeZ = degrees_turnZ / deg_per_ms
                    sendCommand("z_cw_$turn_timeZ")
                } else {
                    turn_timeZ = inv_degrees_turnZ / deg_per_ms
                    sendCommand("z_ccw_$turn_timeZ")
                }
            }*/
        } catch (e: Exception) {
            Toast.makeText(this, "Object not registered.", Toast.LENGTH_SHORT).show()
        }
    }

    /*Use this function to register POIs */
//    private fun registerObject(newObject : String){
//        var objName = newObject.toLowerCase().trim().replace("\\s".toRegex(),"")
//        if(!obj_coordinate_map.containsKey(objName)){
//            obj_coordinate_map[objName] = floatArrayOf(getAngle(0),getAngle(2))
//        } else
//            Toast.makeText(this, "Object already registered.", Toast.LENGTH_SHORT).show()
//    }
//
//    private fun compareCoord(curCoord: Float, obj_yz: Float): Boolean {
//        val lowerV = minDegrees(obj_yz, margin_of_error)
//        val higherV = addDegrees(obj_yz, margin_of_error)
//        var objectWithinBounds = false
//        if (higherV < lowerV) {
//            if (curCoord in lowerV..359f || curCoord in 0f..higherV) //if lowerV reduces beyond 0
//                objectWithinBounds = true
//        } else {
//            if (curCoord in lowerV..higherV)
//                objectWithinBounds = true
//        }
//        return objectWithinBounds
//    }
//
//    private fun determineObject(objectName: String): Boolean { //return true if object is found
//        var objectFound = false
//        //MEDICINE BOX
//        try {
//            val objCoords = obj_coordinate_map[objectName]
//            val obj_z = objCoords?.get(0)
//            val obj_y = objCoords?.get(1)
//            //                curY <= obj_y!! + margin_of_error && curY >= obj_y - margin_of_error)
//
//            if (compareCoord(getAngle(0), obj_z!!) && compareCoord(getAngle(2), obj_y!!)) {
//                object_name.text = objectName
//                objectFound = true
//            } else {
//                object_name.text = ""
//                objectFound = false
//            }
//        } catch (ex: Exception) {
//
//        }
//        return objectFound
//    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
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
    //END OBJ POINTING MODULE
    /* ----------------------------------------------------------------------------------*/
    //CHATBOT MODULE
}
