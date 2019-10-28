package com.example.nakz

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.dialogflow.v2beta1.DetectIntentResponse
import com.google.cloud.dialogflow.v2beta1.QueryInput
import com.google.cloud.dialogflow.v2beta1.SessionName
import com.google.cloud.dialogflow.v2beta1.SessionsClient
import com.google.cloud.dialogflow.v2beta1.SessionsSettings
import com.google.cloud.dialogflow.v2beta1.TextInput
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Text

import java.lang.Exception
import java.util.*
import kotlin.coroutines.coroutineContext

class Dialogflow : AppCompatActivity(), RecognitionCallback {


    lateinit var mTTS: TextToSpeech
    private val uuid = UUID.randomUUID().toString()
    private var chatLayout: LinearLayout? = null
    private var queryEditText: EditText? = null

    // Java V2
    private var sessionsClient: SessionsClient? = null
    private var session: SessionName? = null

    private var textToSpeechIsInitialized = false

    val webIntent: Intent = Uri.parse("www.google.com").let { webpage ->
        Intent(Intent.ACTION_VIEW, webpage)
    }

    internal val userLayout: FrameLayout
        get() {
            val inflater = LayoutInflater.from(this@Dialogflow)
            return inflater.inflate(R.layout.user_msg_layout, null) as FrameLayout
        }

    internal val botLayout: FrameLayout
        get() {
            val inflater = LayoutInflater.from(this@Dialogflow)
            return inflater.inflate(R.layout.bot_msg_layout, null) as FrameLayout
        }

    private val recognitionManager: KontinuousRecognitionManager by lazy {
        KontinuousRecognitionManager(this, activationKeyword = ACTIVATION_KEYWORD, callback = this)
    }

    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chat_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_REQUEST_CODE
            )
        }

        val scrollview = findViewById<ScrollView>(R.id.chatScrollView)
        scrollview.post { scrollview.fullScroll(ScrollView.FOCUS_DOWN) }

        chatLayout = findViewById(R.id.chatLayout)

        //val sendBtn = findViewById<ImageView>(R.id.sendBtn)
        //sendBtn.setOnClickListener{ this.speak() }

        queryEditText = findViewById(R.id.queryEditText)
        queryEditText!!.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
//                        speak()
                    }
                    else -> {
                    }
                }
            }
            false
        }

        // Java V2
        initV2Chatbot()
        mTTS = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                mTTS.language = Locale.US
            }
        })

    }


    //    private class TTS (mTTS : TextToSpeech) : AsyncTask<String, Void, Void>() {
//        val tts : TextToSpeech = mTTS
//
//        override fun doInBackground(vararg speechText: String): Void? {
//            tts.speak(speechText[0], TextToSpeech.QUEUE_FLUSH, null, null)
//            return null
//        }
//    }

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

    private fun sendMessage(message: String) {
        if (message.trim() == "") {
            Toast.makeText(this@Dialogflow, "Please enter your query!", Toast.LENGTH_LONG).show()
        } else {
            showTextView(message, USER)
            queryEditText!!.setText("")

            // Java V2
            val queryInput = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build()
            RequestJavaV2Task(this@Dialogflow, session!!, sessionsClient!!, queryInput).execute()
        }
    }

    fun callbackV2(response: DetectIntentResponse?) {
//        recognitionManager!!.destroyRecognizer()
//        recognitionManager = null

        if (response != null) {
            // process aiResponse here
            val botReply = response.queryResult.fulfillmentText
            Log.d(TAG, "V2 Bot Reply: $botReply")
            //PERFORM CHECK ON BOTREPLY IF FUNCTION
            //if not function
            showTextView(botReply, BOT)
//            TTS(mTTS).execute(botReply)
            mTTS.speak(botReply, TextToSpeech.QUEUE_FLUSH, null, null)
            while (mTTS.isSpeaking) {
            }
            //else
            //code
            startRecognition()

        } else {
            Log.d(TAG, "Bot Reply: Null")
            val botReply =
                "There was some communication issue. Please Check Your Internet Connection"
            showTextView(
                "There was some communication issue. Please Check Your Internet Connection!",
                BOT
            )

//            TTS(mTTS).execute(botReply)
//            speak()
            startRecognition()
        }
    }

    private fun showTextView(message: String, type: Int) {
        val layout: FrameLayout
        when (type) {
            USER -> layout = userLayout
            BOT -> layout = botLayout
            else -> layout = botLayout
        }
        layout.isFocusableInTouchMode = true
        chatLayout!!.addView(layout) // move focus to text view to automatically make it scroll up if softfocus
        val tv = layout.findViewById<TextView>(R.id.chatMsg)
        tv.text = message
        layout.requestFocus()
        queryEditText!!.requestFocus() // change focus back to edit text to continue typing
    }

    //RecognitionCallback Functions
    override fun onDestroy() {
        recognitionManager.destroyRecognizer()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecognition()
        }
    }

    override fun onPause() {
        stopRecognition()
        super.onPause()
    }

    private fun startRecognition() {
        recognitionManager.startRecognition()
    }

    private fun stopRecognition() {
        recognitionManager.stopRecognition()
    }

    private fun getErrorText(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
        SpeechRecognizer.ERROR_SERVER -> "Error from server"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Didn't understand, please try again."
    }

    override fun onBeginningOfSpeech() {
        Log.i("Recognition", "onBeginningOfSpeech")
    }

    override fun onBufferReceived(buffer: ByteArray) {
        Log.i("Recognition", "onBufferReceived: $buffer")
    }

    override fun onEndOfSpeech() {
        Log.i("Recognition", "onEndOfSpeech")
    }

    override fun onError(errorCode: Int) {
        val errorMessage = getErrorText(errorCode)
        Log.i("Recognition", "onError: $errorMessage")
//        textView.text = errorMessage
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        Log.i("Recognition", "onEvent")
    }

    override fun onReadyForSpeech(params: Bundle) {
        Log.i("Recognition", "onReadyForSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {

    }

    override fun onPrepared(status: RecognitionStatus) {
        when (status) {
            RecognitionStatus.SUCCESS -> {
                Log.i("Recognition", "onPrepared: Success")
//                textView.text = "Recognition ready"
            }
            RecognitionStatus.FAILURE,
            RecognitionStatus.UNAVAILABLE -> {
                Log.i("Recognition", "onPrepared: Failure or unavailable")
                AlertDialog.Builder(this)
                    .setTitle("Speech Recognizer unavailable")
                    .setMessage("Your device does not support Speech Recognition. Sorry!")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }

    override fun onKeywordDetected() {
        Log.i("Recognition", "keyword detected !!!")
//        textView.text = "Keyword detected"
    }

    override fun onPartialResults(results: List<String>) {}

    override fun onResults(results: List<String>, scores: FloatArray?) {
        val text = results.joinToString(separator = "\n")
        Log.i("Recognition", "onResults : $text")
//        textView.text = results[0]
        sendMessage(results[0])
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RECORD_AUDIO_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startRecognition()
                }
            }
        }
    }
    //end Functions RecognitionCallback

    companion object {

        //        var recognitionManager: KontinuousRecognitionManager? = null
        private const val ACTIVATION_KEYWORD = "hello"
        private const val RECORD_AUDIO_REQUEST_CODE = 101

        private val TAG = "ME"
        private val USER = 10001
        private val BOT = 10002

    }
}
