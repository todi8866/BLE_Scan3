package com.example.ble_scan3

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask
import java.util.UUID


//import androidx.core.content.ContextCompat



class MainActivity : AppCompatActivity() {

    private var isScanning = false
    private var isConnecting = false

    private var lastcurrenttime:Long = 0
    private var scancount:Long = 0
    private var bleSendCount = 0
    private var bleCharacteristicWrite = 0
    private var bleWriteErrorCount = 0
    private lateinit var usergatt: BluetoothGatt

    private lateinit var proteusGatt: BluetoothGatt
    private lateinit var proteusGattService: BluetoothGattService
    private lateinit var proteusRXCharacteristic: BluetoothGattCharacteristic
    private lateinit var proteusTXCharacteristic: BluetoothGattCharacteristic

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }


    private var scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()


    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val builder = AlertDialog.Builder(this)
            //set title for alert dialog
            builder.setTitle("Bluetooth Warnung")
            //set message for alert dialog
            builder.setMessage("\n\rBluetooth ist ausgeschaltet.")
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton(
                "Beenden",
                DialogInterface.OnClickListener { dialog, id -> finishAndRemoveTask() })

            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()
        }
    }


    private fun MD3ScanStart() {
        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        } else { /* TODO: Actually perform scan */ }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                RUNTIME_PERMISSION_REQUEST_CODE
            )
           // return
        }
        isScanning = true
        scancount = 0

        val SERVICE_DATA_UUID = UUID.fromString("6e400001-c352-11e5-953d-0002a5d5c51b")
        val filters: ArrayList<ScanFilter> = ArrayList<ScanFilter>()
        val uuid = SERVICE_DATA_UUID
      //  val filter = ScanFilter.Builder().setServiceUuid(SERVICE_DATA_UUID).build()
      //  filters.add(filter)

        val parcelUuid = ParcelUuid(UUID.fromString("6e400001-c352-11e5-953d-0002a5d5c51b"))
        val filter = ScanFilter.Builder().setServiceUuid(parcelUuid).build()
        filters.add(filter)
    //    scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()


       // val settings: ScanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()
       val settings = ScanSettings.Builder().setCallbackType(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        bleScanner.startScan(filters, settings, scanCallback)
       // bleScanner.startScan(null, scanSettings, scanCallback)
    }

    private fun MD3ScanStop() {
        isScanning = false
        val scantext = findViewById<TextView>(R.id.textView_scan)
        scantext.text = ""
        bleScanner.stopScan(scanCallback)
    }


    private val bleScanner by lazy {
        bluetoothAdapter.bluetoothLeScanner
    }


    // From the previous section:
    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val TAG = "ScanCallback"
    private val RUNTIME_PERMISSION_REQUEST_CODE = 2





    fun onCharacteristicWrite(
       // gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        with(characteristic) {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> {
                    Log.i("BluetoothGattCallback", "Wrote to characteristic $uuid | value: ${value}")
                }
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                    Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                }
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                    Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                }
                else -> {
                    Log.e("BluetoothGattCallback", "Characteristic write failed for $uuid, error: $status")
                }
            }
        }
    }





    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    // TODO: Store a reference to BluetoothGatt
                    usergatt = gatt
                    gatt.discoverServices()

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d("onCharacteristicWrite", "Failed write, retrying: $status")
                gatt!!.writeCharacteristic(characteristic)
                bleCharacteristicWrite = 0
            }
            super.onCharacteristicWrite(gatt, characteristic, status);
            bleCharacteristicWrite = 0
        }


        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            with(characteristic) {
                Log.i("BluetoothGattCallback", "Characteristic $uuid changed | value: ${value}")

                if (characteristic.uuid.toString() == "6e400003-c352-11e5-953d-0002a5d5c51b") {
                    val scantextreceived = findViewById<TextView>(R.id.textView_received)
                    var receive = proteusRXCharacteristic.value
                    receive = receive.copyOfRange(1, receive.size)
                    scantextreceived.text = scantextreceived.text.toString() + "\r\n" + String(receive)
                }

            }
        }


        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}, ${device.uuids}:")

                services.forEach { service ->
                    val characteristicsTable = service.characteristics.joinToString(
                        separator = "\n|--",
                        prefix = "|--"
                    ) { it.uuid.toString() }
                    Log.i(
                        "printGattTable",
                        "\nService ${service.uuid}\nCharacteristics:\n$characteristicsTable"
                    )
                    // Service
                    // 00001800-0000-1000-8000-00805f9b34fb
                    // Characteristics
                    //  |--00002a00-0000-1000-8000-00805f9b34fb
                    //  |--00002a01-0000-1000-8000-00805f9b34fb
                    //  |--00002a04-0000-1000-8000-00805f9b34fb
                    //  |--00002aa6-0000-1000-8000-00805f9b34fb
                    // 00001801-0000-1000-8000-00805f9b34fb
                    // 6e400001-c352-11e5-953d-0002a5d5c51b             Proteus 3 primary service
                    //  |--6e400002-c352-11e5-953d-0002a5d5c51b      RX
                    //  |--6e400003-c352-11e5-953d-0002a5d5c51b      TX
                }
                    proteusGatt = gatt
                    proteusGattService = gatt.getService(UUID.fromString("6e400001-c352-11e5-953d-0002a5d5c51b"))
                    proteusTXCharacteristic = proteusGattService.getCharacteristic(UUID.fromString("6e400002-c352-11e5-953d-0002a5d5c51b"))
                    proteusRXCharacteristic = proteusGattService.getCharacteristic(UUID.fromString("6e400003-c352-11e5-953d-0002a5d5c51b"))

                    proteusGatt.setCharacteristicNotification(proteusRXCharacteristic, true)

                    val descriptor = proteusRXCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor.setValue(ENABLE_NOTIFICATION_VALUE)
                    // finally connect with blue led on board
                    proteusGatt.writeDescriptor(descriptor)
                    bleCharacteristicWrite = 0   // we just wrote something

                //      proteusRXCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                //proteusTXCharacteristic.value = byteArrayOf(1,49,50,51,52,32,65,66,67)
                //proteusGatt.writeCharacteristic(proteusTXCharacteristic)

                Timer().scheduleAtFixedRate(object: TimerTask() {
                    override fun run() {
                        val c = Calendar.getInstance()

                        val millisec = (c.get(Calendar.MILLISECOND) / 10)
                        var millisec1 = (millisec / 10).toByte()
                        var millisec2 = (millisec - (millisec1 * 10)).toByte()
                        millisec1 = (millisec1 + 48).toByte()
                        millisec2 = (millisec2 + 48).toByte()

                        val seconds = c.get(Calendar.SECOND)
                        var secbyte1 = (seconds / 10).toByte()
                        var secbyte2 = (seconds - (secbyte1 * 10)).toByte()
                        secbyte1 = (secbyte1 + 48).toByte()
                        secbyte2 = (secbyte2 + 48).toByte()

                        val minutes = c.get(Calendar.MINUTE)
                        var minbyte1 = (minutes / 10).toByte()
                        var minbyte2 = (minutes - (minbyte1 * 10)).toByte()
                        minbyte1 = (minbyte1 + 48).toByte()
                        minbyte2 = (minbyte2 + 48).toByte()


                        var sendcount1 = (bleSendCount / 100).toByte()
                        var sendcount2 = ((bleSendCount - (sendcount1 * 100)) / 10).toByte()
                        var sendcount3 = (bleSendCount - (sendcount1 * 100) - (sendcount2 * 10)).toByte()
                        sendcount1 = (sendcount1 + 48).toByte()
                        sendcount2 = (sendcount2 + 48).toByte()
                        sendcount3 = (sendcount3 + 48).toByte()

                        var dummy:Byte = 32
                        if (bleWriteErrorCount != 0)
                            dummy = 0x41

                        proteusTXCharacteristic.value = byteArrayOf(1,
                            48,49,50,51,52,53,54,55,56,57,
                            48,49,50,51,52,53,54,55,56,57,
                            48,49,50,51,52,53,54,55,56,57,
                            48,49,50,51,52,53,54,55,56,57,
                         //   48,49,50,51,52,53,54,55,56,57,
                         //   48,49,50,51,52,53,54,55,56,57,
                         //   48,49,50,51,52,53,54,55,56,57,
                         //   48,49,50,51,52,53,54,55,56,57,
                         //   48,49,50,51,52,53,54,55,56,57,
                         //   48,49,50,51,52,53,54,55,56,57,
                            48,49,50,51,52,53,54,55,56,57,
                            48,49,50,51,52,53,54,55,56,57,32,
                            minbyte1,   minbyte2,   58,
                            secbyte1,   secbyte2,   46,
                            millisec1,  millisec2,  32,
                            sendcount1, sendcount2, sendcount3, 32, dummy, dummy,
                            13,10)

                        if (bleCharacteristicWrite == 0) {
                            bleWriteErrorCount = 0
                            bleSendCount++
                            if (bleSendCount > 999) bleSendCount = 0
                            proteusGatt.writeCharacteristic(proteusTXCharacteristic)
                            bleCharacteristicWrite = bleSendCount + 1
                        }
                        else {
                            bleWriteErrorCount++
                            Log.i(
                                "printGattTable",
                                "\nWriteCharacteristic ${bleSendCount} Err: ${bleWriteErrorCount}")
                            if (bleWriteErrorCount >= 1)
                                bleCharacteristicWrite = 0  // forget about error, keep on

                        }
                    }
                }, 500, 25)



            }
        }
    }


    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            scancount++

            val scantext = findViewById<TextView>(R.id.textView_scan)

            // CALLBACK_TYPE_FIRST_MATCH = 2
            // CALLBACK_TYPE_MATCH_LOST = 4
            // CALLBACK_TYPE_ALL_MATCHES = 1
            if (callbackType == ScanSettings.CALLBACK_TYPE_MATCH_LOST)
                scantext.text = "Lost"
            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                scantext.text = "All"
        //   if (((result.device.name == "MD3") || (result.device.name == "MoDat")) &&
        //    if ((result.scanRecord.serviceUuids[0]="6e400001-c352-11e5-953d-0002a5d5c51b") &&

             if ((lastcurrenttime < (System.currentTimeMillis() - 500)) || !isScanning) {

                    with(result.device) {
                        if (isScanning) {
                         //   val scantext = findViewById<TextView>(R.id.textView_scan)
                            Log.i(
                                TAG,
                                "Found BLE device! Name: ${name ?: "Unnamed"}, address: $address, rssi: ${result.rssi}"
                            )
                            val buttonConnect = findViewById<Button>(R.id.button_connect)
                            buttonConnect.visibility = View.VISIBLE

                            var md3_BTStrength = "not found"
                            if (result.rssi <  -70) md3_BTStrength = "weak"
                            if (result.rssi <= -65) md3_BTStrength = "good"
                            if (result.rssi >  -65) md3_BTStrength = "strong"

                            scantext.text =
                                "${name}  ${result.rssi}  " + md3_BTStrength + " " + scancount.toString()
                            lastcurrenttime = System.currentTimeMillis()
                        }
                        if (isScanning && isConnecting)
                            with(result.device) {
                                Log.w("ScanResultAdapter", "Connecting to $address")
                                isScanning = false
                                MD3ScanStop()
                                connectGatt(this@MainActivity, false, gattCallback)

                            }
                    }
            }
        //     else {
        //         if (lastcurrenttime < (System.currentTimeMillis() - 1000) && isScanning)
        //             scantext.text = ""
        //     }
        }
    }

//***********************************************************************************

//is_checked
    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

// is_checked
    fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

// is checked
    private fun Activity.requestRelevantRuntimePermissions() {
        if (hasRequiredRuntimePermissions()) { return }
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    RUNTIME_PERMISSION_REQUEST_CODE
                )
                //requestLocationPermission()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ),
                    RUNTIME_PERMISSION_REQUEST_CODE
                )
            }
            //       requestBluetoothPermissions()
            //    }
        }
    }

    //  private fun requestLocationPermission() {
    //      runOnUiThread {
    //          alert {
    //              title = "Location permission required"
    //              message = "Starting from Android M (6.0), the system requires apps to be granted " +
    //                      "location access in order to scan for BLE devices."
    //              isCancelable = false
    //              positiveButton(android.R.string.ok) {
    //                  ActivityCompat.requestPermissions(
    //                      this,
    //                      arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
    //                      RUNTIME_PERMISSION_REQUEST_CODE
    //                  )
    //              }
    //          }.show()
    //      }
    //  }


    //   private fun requestBluetoothPermissions() {
    //       runOnUiThread {
    //           alert {
    //               title = "Bluetooth permissions required"
    //               message = "Starting from Android 12, the system requires apps to be granted " +
    //                       "Bluetooth access in order to scan for and connect to BLE devices."
    //               isCancelable = false
    //               positiveButton(android.R.string.ok) {
    //                   ActivityCompat.requestPermissions(
    //                       this,
    //                       arrayOf(
    //                           Manifest.permission.BLUETOOTH_SCAN,
    //                           Manifest.permission.BLUETOOTH_CONNECT
    //                       ),
    //                       RUNTIME_PERMISSION_REQUEST_CODE
    //                   )
    //               }
    //           }.show()
    //       }
    //   }

//is checked
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            RUNTIME_PERMISSION_REQUEST_CODE -> {
                val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
                    it.second == PackageManager.PERMISSION_DENIED &&
                            !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
                }
                val containsDenial = grantResults.any { it == PackageManager.PERMISSION_DENIED }
                val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                when {
                    containsPermanentDenial -> {
                        // TODO: Handle permanent denial (e.g., show AlertDialog with justification)
                        // Note: The user will need to navigate to App Settings and manually grant
                        // permissions that were permanently denied
                    }
                    containsDenial -> {
                        requestRelevantRuntimePermissions()
                    }
                    allGranted && hasRequiredRuntimePermissions() -> {
                      //  MD3ScanStart()
                    }
                    else -> {
                        // Unexpected scenario encountered when handling permissions
                        recreate()
                    }
                }
            }
        }
    }



/*
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.


        if (!hasRequiredRuntimePermissions()) {
            requestRelevantRuntimePermissions()
        } else { /* TODO: Actually perform scan */ }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                RUNTIME_PERMISSION_REQUEST_CODE
            )
            return false
        }


        bleScanner.startScan(null, scanSettings, scanCallback)

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

*/

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val scantext = findViewById<TextView>(R.id.textView_scan)
        scantext.text = ""  // make no text for the start

        val scantextreceived = findViewById<TextView>(R.id.textView_received)
        scantextreceived.text = ""  // make no text for the start

        val buttonStart = findViewById<Button>(R.id.button_scan)
        buttonStart.text = "Scan"

        val buttonConnect = findViewById<Button>(R.id.button_connect)
        buttonConnect.text = "Connect"
        buttonConnect.visibility = View.INVISIBLE

        buttonStart.setOnClickListener {
            if (!isScanning) {
                MD3ScanStart()
                buttonStart.text = "Stop"
                scantext.text = "Searching..."
            }
            else
            {
                MD3ScanStop()
                buttonStart.text = "Start"
                scantext.text = ""
                buttonConnect.visibility = View.INVISIBLE
            }
        }

        buttonConnect.setOnClickListener {
            isConnecting = true
            buttonConnect.text = "Disconnect"

        }
    }
}