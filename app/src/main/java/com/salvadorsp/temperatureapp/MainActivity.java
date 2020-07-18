package com.salvadorsp.temperatureapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.ByteBuffer;

import me.aflak.arduino.Arduino;
import me.aflak.arduino.ArduinoListener;
import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MainActivity extends AppCompatActivity{
    Arduino arduino;
    Button button;
    TextView temperature, userstatus;
    int tempgaugeval;
    CustomGauge tempGauge;
    String data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperature = findViewById(R.id.temperatureval);
        tempGauge = findViewById(R.id.tempgauge);
        userstatus = findViewById(R.id.status);
        button = findViewById(R.id.gettemp);

        arduino = new Arduino(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data = "1";
                arduino.send(data.getBytes());
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
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        arduino.setArduinoListener(new ArduinoListener() {
            @Override
            public void onArduinoAttached(UsbDevice device) {
                Toast.makeText(MainActivity.this, "Arduino is attached", Toast.LENGTH_SHORT).show();
                arduino.open(device);
            }

            @Override
            public void onArduinoDetached() {
                Toast.makeText(MainActivity.this, "Arduino is detached.", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onArduinoMessage(byte[] bytes) {
                temperature.setText(new String(bytes));
                tempgaugeval = convertByteArrayToInt(bytes);
            }

            @Override
            public void onArduinoOpened() {
                Toast.makeText(MainActivity.this, "Arduino is opened.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        arduino.unsetArduinoListener();
        arduino.close();
    }

    public static int convertByteArrayToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

}
