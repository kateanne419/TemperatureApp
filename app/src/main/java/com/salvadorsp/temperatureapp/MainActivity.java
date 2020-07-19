package com.salvadorsp.temperatureapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;
import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MainActivity extends AppCompatActivity{
    public final String ACTION_USB_PERMISSION = "com.salvadorsp.temperatureapp.USB_PERMISSION";
    Arduino arduino;
    Button tempbutton, connectbutton;
    TextView temperature, userstatus;
    int tempgaugeval;
    CustomGauge tempGauge;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;





    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                //tvAppend(userstatus, data);
                tempgaugeval = Integer.parseInt(data);
                tempGauge.setValue(tempgaugeval);
                if (tempgaugeval < 34) {
                    userstatus.setText("Below Normal");
                } else if (tempgaugeval <= 37.3 && tempgaugeval >= 34) {
                    userstatus.setText("Normal");
                } else if (tempgaugeval <= 38 && tempgaugeval > 37.3) {
                    userstatus.setText("Above Normal. Stay at home!");
                } else if (tempgaugeval > 38){
                    userstatus.setText("High, go to the nearest hospital!");
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            Toast.makeText(MainActivity.this, "Serial connection opened", Toast.LENGTH_SHORT).show();

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            }
//            else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
//                onClickStart(startButton);
//            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
//                onClickStop(stopButton);
//
//            }
        }

        ;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = findViewById(R.id.temperatureval);
        tempGauge = findViewById(R.id.tempgauge);
        userstatus = findViewById(R.id.status);
        connectbutton = findViewById(R.id.connect);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        arduino = new Arduino(this);

        connectbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                init();
            }
        });

    }

    public void init(){
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            Toast.makeText(MainActivity.this, "USB connection detected", Toast.LENGTH_SHORT).show();
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                    Toast.makeText(MainActivity.this, "Arduino connected.", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this, "Cannot detect Arduino.", Toast.LENGTH_SHORT).show();
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }else{
            Toast.makeText(MainActivity.this, "Arduino not detected.", Toast.LENGTH_SHORT).show();
        }
    }



    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        arduino.setArduinoListener(new ArduinoListener() {
//            @Override
//            public void onArduinoAttached(UsbDevice device) {
//                Toast.makeText(MainActivity.this, "Arduino is attached", Toast.LENGTH_SHORT).show();
//                arduino.open(device);
//            }
//
//            @Override
//            public void onArduinoDetached() {
//                Toast.makeText(MainActivity.this, "Arduino is detached.", Toast.LENGTH_SHORT).show();
//
//            }
//
//            @Override
//            public void onArduinoMessage(byte[] bytes) {
//                temperature.setText(new String(bytes));
//                tempgaugeval = convertByteArrayToInt(bytes);
//            }
//
//            @Override
//            public void onArduinoOpened() {
//                Toast.makeText(MainActivity.this, "Arduino is opened.", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        arduino.unsetArduinoListener();
//        arduino.close();
//    }
//
//    public static int convertByteArrayToInt(byte[] bytes) {
//        return ByteBuffer.wrap(bytes).getInt();
//    }

}
