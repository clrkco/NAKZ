package com.example.nakz

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Display
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.nakz.models.ContactModel
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2beta1.*
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
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class HotwordMain : Activity(), SensorEventListener {
    //CALLING
    private var contactModelArrayList: ArrayList<ContactModel>? = null

    //TTS
    private val uuid = UUID.randomUUID().toString()
    private var chatLayout: LinearLayout? = null
    private var queryEditText: EditText? = null
    private var isSpeaking = false
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

    var showingReminder = false

    companion object {
        //To Prevent unnecessary movement in face tracking
        private var prevCenterX = 0f
        private var prevCenterY = 0f
        private var tempCenterX = 0
        private var tempCenterY = 0

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
//        var obj_coordinate_map: HashMap<String, FloatArray> = hashMapOf()

        //degrees conversion rate
        val deg_from_px = 13.37f

        //offset (in px) for pan-tilt face detection
        val offset_x_left = 350f
        val offset_x_right = 650f
        val camera_placement_offset = 20f
        val offset_y = 100f

        //regulating sending of commands
        var AllowSend: Boolean = true
        var AllowFaceTracking: Boolean = true  //disable during Dialogflow interaction

        //for sensor calibration
        private var init_y_constant: Double = 0.0
        private var init_z_constant: Double = 0.0
        private var new_z_constant: Double = 0.0     //z constant to be added to z orientation
        private var new_y_constant: Double = 0.0    //y constant to be added to y orientation
        private var add_or_sub_z: Boolean = false //true add, false sub
        private var add_or_sub_y: Boolean = false //true add, false sub
        private var phone_to_servo_deg_errbitsz: Int = 30
        private var phone_to_servo_deg_errbitsy: Int = 0

        //motor positions for pan tilt
        private var pan_servo = 512
        private var tilt_servo = 512


        //TTS
        const val RECORD_AUDIO_REQUEST_CODE = 101
        //STT
        const val REQUEST_CODE_SPEECH_INPUT = 1000
        var start = false

        //reminder
//        var rTitle: ArrayList<String> = ArrayList()
//        var rTime: ArrayList<String> = ArrayList()
//        var count = 0
//        var loop = 0

        var timerSecs = (2000..5000).random()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compass_main)
//        getContacts()
        //Blinking
        var timer = object : CountDownTimer(timerSecs.toLong(),1000){
            override fun onFinish() {
                this.cancel()
                timerSecs = (2000..5000).random()
                Log.e("OnFinish", "Start again")
                this.start()
                imageButton.setBackgroundResource(R.drawable.blink)
                object : CountDownTimer(700,1000){
                    override fun onFinish() {
                        this.cancel()
                        imageButton.setBackgroundResource(R.drawable.eyes)
                    }
                    override fun onTick(p0: Long) {
                    }
                }.start()
            }
            override fun onTick(p0: Long) {
            }
        }.start()
        try {
            address = intent.getStringExtra(BluetoothConnect.EXTRA_ADDRESS)!!
        } catch (e: Exception) {
            Log.e("onCreate", "address not found.")
        }
        //CONNECT TO BT
        ConnectToDevice(this).execute()
        //COMPASS INIT
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        button.setOnClickListener {
            calibrateSensors()
            tilt_servo = 320
            val input = "~d_512_320_#!"
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket!!.outputStream.write(input.toByteArray())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            AllowSend = true    //only allow sending after initializing servos
            AllowFaceTracking = true
            button.visibility = View.INVISIBLE
            imageButton.visibility = View.VISIBLE
        }
        imageButton.setOnClickListener {
            //            Log.e("Press","press")
//            start = false
            tempCenterX = pan_servo
            tempCenterY = tilt_servo
            Log.e("Temps: ", "TempCenterX: $tempCenterX TempCenterY: $tempCenterY")
            AllowFaceTracking = false
            speak()
        }

        // Java V2
        initV2Chatbot()
        //TTS Init
        mTTS = TextToSpeech(applicationContext, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                mTTS.language = Locale.UK
            }
        })
        //FACE DETECT INIT
        val display: Display = windowManager.defaultDisplay
        width = display.width
        length = display.height
//        Log.i("Border Pan: ", "Left: "+ (width/2- offset_x_left) + " Right: " + (width/2+ offset_x_right))
//        Log.i("Border Tilt: ", "Up: " + (length/2+ offset_y) + " Down: " + (length/2- offset_y))
//        Log.i("l and w", "$width $length")
        setupCamera()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("coordinate_map", BluetoothConnect.obj_coordinate_map)
        outState.putStringArrayList("rTitle", BluetoothConnect.rTitle)
        outState.putStringArrayList("rTime",BluetoothConnect. rTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        try {
            BluetoothConnect.obj_coordinate_map =
                savedInstanceState.getSerializable("coordinate_map") as HashMap<String, FloatArray>
            BluetoothConnect.rTitle = savedInstanceState.getStringArrayList("rTitle")!!
            BluetoothConnect.rTime = savedInstanceState.getStringArrayList("rName")!!
        } catch (e : Exception) {
            Log.e("OnRestore", "Error restoring state.")
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        disconnect()
        val intent = Intent(this, BluetoothConnect::class.java)
        startActivity(intent)
    }

    private fun speak() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Hi There! :) ")
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30000)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: Exception) {
            Toast.makeText(this, "" + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SPEECH_INPUT -> {
                if (resultCode == RESULT_OK && null != data) {
                    val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
//                    if (result[0].contains("hello", ignoreCase = true) && !start) {
//                        start = true
//                        sendMessage(result[0])
                    start = true
                    sendMessage(result[0])
                } else if (resultCode == RESULT_CANCELED) {
                    AllowSend = true
                    start = false
                    setPTCoords()
                }
            }
        }
    }

    private fun sendMessage(message: String) {

        // Java V2
        val queryInput = QueryInput.newBuilder()
            .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()
        RequestJavaV2Task(this, session!!, sessionsClient!!, queryInput).execute()

    }

    private fun setPTCoords() { //function to stabilize pan tilt servo coords after talking with chatbot during thread sleep
        object : CountDownTimer(4000, 1) {
            override fun onFinish() {
                AllowFaceTracking = true
                this.cancel()
            }
            override fun onTick(p0: Long) {
                pan_servo = tempCenterX
                tilt_servo = tempCenterY
                Log.i("Setting: ", "pan_servo = $pan_servo tilt_servo = $tilt_servo")
            }
        }.start()
    }

    //CHATBOT MODULE
    fun callbackV2(response: DetectIntentResponse?) {
        if (response != null) {
            // process aiResponse here
            Log.e("Callback:", response.queryResult.intent.displayName.toString())
            var botIntent = response.queryResult.intent.displayName.toString()
            var botReply = response.queryResult.fulfillmentText
            var gesture = ""
            if (botReply.contains("off", ignoreCase = true)) {
                start = false
                botReply = "Okay! Talk to you later!"
                setPTCoords()
                gesture = "~g_a_#!"
//                AllowFaceTracking = true
            } else if (botReply.contains("f_showreminder", ignoreCase = true)) {
                showingReminder = true
                BluetoothConnect.loop = 0
                botReply = ""
                while (BluetoothConnect.loop != BluetoothConnect.count) {
                    val temp5 = "Name " + BluetoothConnect.rTitle[BluetoothConnect.loop] + " time " + BluetoothConnect.rTime[BluetoothConnect.loop]
                    mTTS.speak(temp5, TextToSpeech.QUEUE_ADD, null, null)
                    while (mTTS.isSpeaking) {
                    }
                    BluetoothConnect.loop += 1
                }
                setPTCoords()
                start = false
                AllowSend = true

//                gesture = "~g_2_#!"
            } else if (botReply.contains("f_addreminder", ignoreCase = true)) {
                val temp1 = botReply.substring(14, botReply.indexOf(',', 0, ignoreCase = true))
                val temp2 = botReply.substring(
                    botReply.indexOf(',', 0, ignoreCase = true) + 13,
                    botReply.indexOf(')', 0, ignoreCase = true) - 6
                )
                if (BluetoothConnect.count == 0) {
                    BluetoothConnect.rTitle.add(BluetoothConnect.count, temp1)
                    BluetoothConnect.rTime.add(BluetoothConnect.count, temp2)
                } else {
                    BluetoothConnect.loop = 0
                    while (BluetoothConnect.loop < 999) {
                        if (BluetoothConnect.loop < BluetoothConnect.count) {
                            if (BluetoothConnect.rTime[BluetoothConnect.loop].substring(0, 1).toInt() - temp2.substring(
                                    0,
                                    1
                                ).toInt() > 0
                            ) {
                                BluetoothConnect.rTitle.add(BluetoothConnect.loop, temp1)
                                BluetoothConnect.rTime.add(BluetoothConnect.loop, temp2)
                                BluetoothConnect.loop = 999
                            } else if (BluetoothConnect.rTime[BluetoothConnect.loop].substring(0, 1).toInt() - temp2.substring(
                                    0,
                                    1
                                ).toInt() == 0
                            ) {
                                if (BluetoothConnect.rTime[BluetoothConnect.loop].substring(3, 4).toInt() - temp2.substring(
                                        3,
                                        4
                                    ).toInt() > 0
                                ) {
                                    BluetoothConnect.rTitle.add(BluetoothConnect.loop, temp1)
                                    BluetoothConnect.rTime.add(BluetoothConnect.loop, temp2)
                                    BluetoothConnect.loop = 999
                                }
                            }
                        } else if (BluetoothConnect.loop != 999) {
                            BluetoothConnect.rTitle.add(BluetoothConnect.loop, temp1)
                            BluetoothConnect.rTime.add(BluetoothConnect.loop, temp2)
                            BluetoothConnect.loop = 999
                        }
                        BluetoothConnect.loop += 1
                    }
                }

                BluetoothConnect.count += 1
                botReply = "Reminder name $temp1 is set at $temp2"
                gesture = "~g_4_#!"
            } else if (botReply.contains("f_findobject", ignoreCase = true)) {
                val objectExists = findObject(botReply.substring(13, botReply.length))
                //findObject has setPTCoords built-in
                if (objectExists) {
                    botReply = "Finding" + botReply.substring(13, botReply.length)
                    start = false
                } else {
                    botReply =
                        "I still don't know where that is yet."
                }
            } else if (botReply.contains("f_registerObject", ignoreCase = true)) {
                val objectRegistered = registerObject(botReply.substring(17, botReply.length))
                botReply = if(objectRegistered)
                    botReply.substring(17, botReply.length) + " registered"
                else
                    "You have already registered this object before. Please choose another name for this object and try again!"
                gesture = "~g_2_#!"
            } else if (botIntent.contains("smalltalk.greetings.hello", ignoreCase = true)) {
                gesture = "~g_a_#!"
            } else if(botIntent.contains("jokes.get", ignoreCase = true)){
                gesture = "~g_1_#!"
            } else {
                val rnds = (1..4).random()
                gesture ="~g_${rnds}_#!"
            }
            if (!showingReminder)
                mTTS.speak(botReply, TextToSpeech.QUEUE_FLUSH, null, null)
//            AllowSend = false
            sendCommand(gesture)
            var sentAlready = false
            object : CountDownTimer(3000, 1000) {
                override fun onFinish() {
//                    Log.e("Timer:", "timer finished")
                    if (!sentAlready && !start) {
                        sendCommand("x_")
                        sentAlready = true
                    }
                    object : CountDownTimer(1500, 1000) {
                        override fun onFinish() {
                            this.cancel()
                            AllowSend = true
                        }

                        override fun onTick(p0: Long) {
                        }
                    }.start()
                    this.cancel() //remove this if it bugs out
                }

                override fun onTick(p0: Long) {
//                    Log.i("ticking", "ticking")
                }
            }.start()
            while (mTTS.isSpeaking) {
            }

        } else {
            val botReply = "There was some communication issue. Please Try again!"
            mTTS.speak(botReply, TextToSpeech.QUEUE_FLUSH, null, null)
            while (mTTS.isSpeaking) {
            }
        }
        //by this point, no more tts commands and done saying reminders
        showingReminder = false
        object : CountDownTimer(1500,1000){
            override fun onFinish() {
                this.cancel()
                if (start)
                    speak()
            }
            override fun onTick(p0: Long) {
            }
        }.start()

    }


    private fun initV2Chatbot() {
        try {
            val stream = resources.openRawResource(R.raw.test_agent_credentials)
            val credentials = GoogleCredentials.fromStream(stream)
            val projectId = (credentials as ServiceAccountCredentials).projectId

            val settingsBuilder = SessionsSettings.newBuilder()
            val sessionsSettings =
                settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build()
            sessionsClient = SessionsClient.create(sessionsSettings)
            session = SessionName.of(projectId, uuid)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    //FACE DETECT MODULE
    private fun setupCamera() {
        try {
            cameraView.facing = Facing.FRONT
            prevCenterX = FaceBoundsOverlay.centerX
            prevCenterY = FaceBoundsOverlay.centerY

            cameraView.addFrameProcessor {
                //                Log.i(
//                    "orientation",
//                    FaceBoundsOverlay.centerX.toString() + " " + FaceBoundsOverlay.centerY.toString()
//                )
//                Log.i(
//                    "offset",
//                    FaceBoundsOverlay.xOffset.toString() + " " + FaceBoundsOverlay.yOffset.toString()
//                )
                if (prevCenterX == FaceBoundsOverlay.centerX)
                    FaceBoundsOverlay.centerX = 0f
                if (prevCenterY == FaceBoundsOverlay.centerY)
                    FaceBoundsOverlay.centerY = 0f
                if (FaceBoundsOverlay.centerX < 150f || FaceBoundsOverlay.centerX > width - 200f || FaceBoundsOverlay.centerY == 0f)
                    FaceBoundsOverlay.centerX = 0f
                if (FaceBoundsOverlay.centerY < 150f || FaceBoundsOverlay.centerY > length - 150f || FaceBoundsOverlay.centerX == 0f)
                    FaceBoundsOverlay.centerY = 0f
                faceDetector.process(
                    Frame(
                        data = it.data,
                        rotation = it.rotation,
                        size = Size(it.size.width, it.size.height),
                        format = it.format,
                        isCameraFacingBack = cameraView.facing == Facing.BACK
                    )
                )
                prevCenterX = FaceBoundsOverlay.centerX
                prevCenterY = FaceBoundsOverlay.centerY
            }
        } catch (e: Exception) {
            disconnect()
            val intent = Intent(this, BluetoothConnect::class.java)
            startActivity(intent)
        }
    }

    /* START OF BLUETOOTH MODULE */
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
                val intent = Intent(this.context, BluetoothConnect::class.java)
                context.startActivity(intent)
            } else {
                //send init servos to arduino
                var input = "z_"
//                if (bluetoothSocket != null) {
//                    try {
//                        bluetoothSocket!!.outputStream.write(input.toByteArray())
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//                //wait for motors to configure
//                Thread.sleep(3500)
//
//                if (bluetoothSocket != null) {
//                    try {
//                        bluetoothSocket!!.outputStream.write(input.toByteArray())
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//                //wait for motors to configure
//                Thread.sleep(3500)
                input = "y_"
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket!!.outputStream.write(input.toByteArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                Thread.sleep(3500)
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket!!.outputStream.write(input.toByteArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                Thread.sleep(2500)
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
                isConnected = true
                tempCenterY = 0
                tempCenterX = 0
                prevCenterX = 0f
                prevCenterY = 0f

                pan_servo = 512
                tilt_servo = 320
            }
            progress.dismiss()
        }
    }

    // Bluetooth Module
    //Syntax ~{type}_{params}_#!
    //p -  object pointing, g - gesture, d- pan tilt
    //see arduino code for params
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
//        Log.i("AllowFaceTracking: ", AllowFaceTracking.toString())
//        Log.i("AllowSend", AllowSend.toString())
//        if(!start){
//            AllowFaceTracking = true
//        }
//        Log.i("FaceBounds: ", "z: " + FaceBoundsOverlay.centerX + " y: " + FaceBoundsOverlay.centerY)
//        Log.i("coordinates: ", "z: " + getAngle(0) + " y: " + getAngle(2))
//        Log.i("servo positions: " , "pan servo: " + pan_servo + " tilt_servo: " + tilt_servo)
//        if (FaceBoundsOverlay.centerX > 1023 || FaceBoundsOverlay.centerX < 0) {
//            FaceBoundsOverlay.centerX = 512f
//            tilt_servo = 512
//        }
//        Log.i("Sensor Coordinates: ", "Z: " + getAngle(0) + " Y: " + getAngle(2))
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
        if (AllowFaceTracking && AllowSend && FaceBoundsOverlay.centerX != 0f && FaceBoundsOverlay.centerY != 0f && FaceBoundsOverlay.xOffset != 0f && FaceBoundsOverlay.yOffset != 0f && !start) {
            //degrees/Px conversion rate 8.37
//            Log.e("Allow FaceTracking: ", AllowFaceTracking.toString())
//            Log.e("AllowSend: ", AllowSend.toString())
//            Log.i(
//                "FaceBoundsOverlay",
//                FaceBoundsOverlay.centerX.toString() + " " + FaceBoundsOverlay.centerY.toString()
//            )
            if (FaceBoundsOverlay.centerX > width / 2f + offset_x_right || FaceBoundsOverlay.centerX < width / 2f - offset_x_left ||
                FaceBoundsOverlay.centerY > length / 2f + offset_y || FaceBoundsOverlay.centerY < length / 2f - offset_y
            ) {
                if (FaceBoundsOverlay.centerX > width / 2f + offset_x_right) { //ccw
                    val diff_in_pixels = FaceBoundsOverlay.centerX - width / 2f
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    if (pan_servo + deg_from_pixels.toInt() < 900)
                        pan_servo += deg_from_pixels.toInt()
                } else if (FaceBoundsOverlay.centerX < width / 2f - offset_x_left) {
                    val diff_in_pixels = width / 2f - FaceBoundsOverlay.centerX
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    if (pan_servo - (deg_from_pixels.toInt() + camera_placement_offset) > 100)
                        pan_servo -= (deg_from_pixels.toInt() + camera_placement_offset.toInt())
                }
                if (FaceBoundsOverlay.centerY > length / 2f + offset_y) {
                    val diff_in_pixels = FaceBoundsOverlay.centerY - length / 2f
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    if (tilt_servo - deg_from_pixels.toInt() > 159)
                        tilt_servo -= deg_from_pixels.toInt()
                } else {
                    val diff_in_pixels = length / 2f - FaceBoundsOverlay.centerY
                    val deg_from_pixels = diff_in_pixels / deg_from_px
                    if (tilt_servo + deg_from_pixels.toInt() < 430)
                        tilt_servo += deg_from_pixels.toInt()
                }
//                Log.i("isConnected" , isConnected.toString() + " " + AllowSend.toString())
                if (isConnected && AllowSend && AllowFaceTracking) { //change isConnected when Arduino is present
//                    Log.d("Moving Face Track", "Sending Command")
                    Log.i("Face Track", "sending")
                    sendCommand("~d_${pan_servo}_${tilt_servo}_#!")
                    AllowSend = false
                    object : CountDownTimer(900, 1000) { //change if want to decrease interval
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

    private fun getAngle(index: Int): Float {
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

    private fun findObject(objectName: String): Boolean {
        //0.293 deg/bit - conversion from angle to bit for AX-12+
        Log.e("Finding Object", "In Here")
        var objName = objectName.toLowerCase().trim().replace("\\s".toRegex(), "")
        Log.i("Finding", objName)
        try {
            val objCoords = BluetoothConnect.obj_coordinate_map[objName]
            val obj_z = objCoords?.get(0)
            val obj_y = objCoords?.get(1)
            val bit_z: Double
            bit_z = if (getAngle(0) < 150f) {
                obj_z!!.div(0.293) + phone_to_servo_deg_errbitsz
            } else {
                obj_z!!.div(0.293) + phone_to_servo_deg_errbitsz
            }
            val bit_y = obj_y!!.div(0.293) + phone_to_servo_deg_errbitsy

            pan_servo = bit_z.toInt()
            tilt_servo = bit_y.toInt()
            Log.e("pan_servo", " $pan_servo")
            Log.e("tilt_servo", " $tilt_servo")
            Log.e("Sending Command: ", "Z: ${bit_z.toInt()}" + "Y: ${bit_y.toInt()}")

            //set FaceBoundsOverlay to these coords until finish
            object : CountDownTimer(3000, 50) {
                override fun onFinish() {
                    this.cancel()
                }

                override fun onTick(p0: Long) {
//                    Log.e("ticking", "ticking")
                    pan_servo = bit_z.toInt() + phone_to_servo_deg_errbitsz
                    tilt_servo = bit_y.toInt()
                }
            }.start()
            AllowSend = false //give motors time to move before sending again
            sendCommand("~p_${pan_servo}" + "_${tilt_servo}_#!")
            object : CountDownTimer(5000, 1000) {
                override fun onFinish() {
                    FaceBoundsOverlay.facesBounds.clear()
                    sendCommand("x_")
                    AllowSend = true
                    AllowFaceTracking = true
//                    this.cancel()
                }

                override fun onTick(millisUntilFinished: Long) {
                }
            }.start()
            return true
        } catch (e: Exception) {
            Toast.makeText(this, "Object not registered.", Toast.LENGTH_SHORT).show()
            AllowSend = false
            sendCommand("x_")
            AllowSend = true
            AllowFaceTracking = true
            return false
        }
    }

    /*Use this function to register POIs */
    private fun registerObject(newObject: String) : Boolean {
        var objName = newObject.toLowerCase().trim().replace("\\s".toRegex(), "")
        Log.e("Object Registered: ", objName)
        if (!BluetoothConnect.obj_coordinate_map.containsKey(objName)) {
            BluetoothConnect.obj_coordinate_map[objName] = floatArrayOf(getAngle(0), getAngle(2))
            return true
        } else {
            Toast.makeText(this, "Object already registered.", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun compareCoord(curCoord: Float, obj_yz: Float): Boolean {
        val lowerV = minDegrees(obj_yz, margin_of_error)
        val higherV = addDegrees(obj_yz, margin_of_error)
        var objectWithinBounds = false
        if (higherV < lowerV) {
            if (curCoord in lowerV..359f || curCoord in 0f..higherV) //if lowerV reduces beyond 0
                objectWithinBounds = true
        } else {
            if (curCoord in lowerV..higherV)
                objectWithinBounds = true
        }
        return objectWithinBounds
    }

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
    //CALLING MODULE
    private fun findCallContact(name: String) {
        val contactName = name.toLowerCase().trim().replace("\\s".toRegex(), "")

    }

    private fun getContacts() {
        contactModelArrayList = ArrayList()
        val phones = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        while (phones!!.moveToNext()) {
            val name =
                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            val phoneNumber =
                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))

            val contactModel = ContactModel()
            contactModel.setNames(name)
            contactModel.setNumbers(phoneNumber)
            Log.d("name>>", "$name  $phoneNumber")
        }
        phones.close()
    }
}
