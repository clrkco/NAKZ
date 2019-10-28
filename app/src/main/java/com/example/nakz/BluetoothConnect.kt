package com.example.nakz

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*

import org.jetbrains.anko.toast

class BluetoothConnect: Activity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var pairedDevices: Set<BluetoothDevice>
    private val  REQUEST_ENABLE_BLUETOOTH = 1

    companion object{
        val EXTRA_ADDRESS: String = "device_address"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(bluetoothAdapter == null){
            toast("this device doesn't support bluetooth")
            return
        }
        else if(!bluetoothAdapter!!.isEnabled){
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent,REQUEST_ENABLE_BLUETOOTH)
        }

        listRefresh.setOnClickListener{pairedDeviceList()}

    }

    private fun pairedDeviceList(){
        pairedDevices = bluetoothAdapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()
        val nameList : ArrayList<String> = ArrayList()
        if(pairedDevices.isNotEmpty()){
            for(device: BluetoothDevice in pairedDevices){
                list.add(device)
                nameList.add(device.name)
                Log.i("device", ""+ device)
            }
        } else{
            toast("no paired devices found.")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1,nameList)
        btListView.adapter = adapter
        btListView.onItemClickListener = AdapterView.OnItemClickListener{_,_,position,_ ->

            val device: BluetoothDevice = list[position]
            val address: String = device.address

            val intent = Intent(this,MAIN::class.java)
            intent.putExtra(EXTRA_ADDRESS, address)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_ENABLE_BLUETOOTH){
            if(resultCode == RESULT_OK){
                if(bluetoothAdapter!!.isEnabled){
                    toast("Bluetooth has been enabled!")
                }
                else{
                    toast("Bluetooth has been disabled!")
                }
            }
        } else if(resultCode == RESULT_CANCELED){
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