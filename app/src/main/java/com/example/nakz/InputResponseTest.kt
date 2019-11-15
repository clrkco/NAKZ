package com.example.nakz

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Toast
import com.example.nakz.MAIN.Companion.bluetoothAdapter
import com.example.nakz.MAIN.Companion.bluetoothSocket
import com.example.nakz.MAIN.Companion.progress
import io.opencensus.stats.Aggregation
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.input_response.*

import java.io.IOException
import java.lang.Exception
import java.util.*

class InputResponseTest : Activity() {
    val timerSecs = 500
    lateinit var timer: CountDownTimer

    companion object {
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

        startBtn.setOnClickListener { start() }
        pauseBtn.setOnClickListener { pause() }
        resetBtn.setOnClickListener { reset() }

        try {
            address = intent.getStringExtra(BluetoothConnect.EXTRA_ADDRESS)!!
        } catch (e: Exception) {
            Log.e("onCreate", "address not found.")
        }

        ConnectToDevice(this).execute()

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

    private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        private var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

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

                isConnected = true
            } else {
                //send init servos to arduino
                isConnected = true
            }
            progress.dismiss()
        }
    }

    fun start() {
        Log.e("Sent: ", "Sent Command")
        sendCommand("z")
        timer = object : CountDownTimer(timerSecs.toLong(), 1) {
            override fun onFinish() {

            }

            override fun onTick(p0: Long) {
                textView2.text =
                    p0.toString()//To change body of created functions use File | Settings | File Templates.
            }

        }
        timer.start()

    }

    fun pause() {
//        textView2.text = textView2.text.toString()
        textView2.text = (500 - textView2.text.toString().toInt()).toString()
        timer.cancel()
    }

    fun reset() {

    }
}