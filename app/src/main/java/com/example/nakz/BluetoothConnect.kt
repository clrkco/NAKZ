package com.example.nakz

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

import org.jetbrains.anko.toast
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class BluetoothConnect : Activity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        val EXTRA_ADDRESS: String = "device_address"
        var mode = false

        var obj_coordinate_map: HashMap<String, FloatArray> = hashMapOf()
        var rTitle: ArrayList<String> = ArrayList()
        var rTime: ArrayList<String> = ArrayList()
        var count = 0
        var loop = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            toast("this device doesn't support bluetooth")
            return
        } else if (!bluetoothAdapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }


        //Check recording permissions
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.RECORD_AUDIO
        )

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        }

        listRefresh.setOnClickListener { pairedDeviceList() }

    }
    fun hasPermissions(context: Context, vararg permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it.toString()) == PackageManager.PERMISSION_GRANTED
    }

    private fun pairedDeviceList() {
        pairedDevices = bluetoothAdapter!!.bondedDevices
        val list: ArrayList<BluetoothDevice> = ArrayList()
        val nameList: ArrayList<String> = ArrayList()
        if (pairedDevices.isNotEmpty()) {
            for (device: BluetoothDevice in pairedDevices) {
                list.add(device)
                nameList.add(device.name)
                Log.i("device", "" + device)
            }
        } else {
            toast("no paired devices found.")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, nameList)
        btListView.adapter = adapter
        btListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->

            val device: BluetoothDevice = list[position]
            val address: String = device.address
            mode = modeSwitch.isChecked
            if(mode) {
                val intent = Intent(this, HotwordMain::class.java) //change to MAIN after test
                intent.putExtra(EXTRA_ADDRESS, address)
                startActivity(intent)
            } else{
                val intent = Intent(this, MAIN::class.java)
                intent.putExtra(EXTRA_ADDRESS, address)
                startActivity(intent)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                if (bluetoothAdapter!!.isEnabled) {
                    toast("Bluetooth has been enabled!")
                } else {
                    toast("Bluetooth has been disabled!")
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            toast("Bluetooth enabling is canceled.")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        // menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }*/
        return true
    }
}