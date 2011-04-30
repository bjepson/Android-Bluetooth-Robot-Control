// Football.java - control Arduino over Bluetooth to play ball
// (c) Tero Karvinen & Kimmo Karvinen http://sulautetut.fi

package fi.sulautetut.android.football;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;

public class Football extends Activity implements SensorEventListener {
	
    String robotBtAddress="00:06:66:04:27:7e"; // Change this 
    TextView statusTv;
    TextView messagesTv;
    TBlue tBlue; 
    SensorManager sensorManager;
    Sensor sensor;
    float g=9.81f; // m/s**2
    float x, y, z, l, r;
    boolean kick; 
    int skipped; // continuously skipped sending because robot not ready
    Handler timerHandler; 
    Runnable sendToArduino; 
    
    private static final int MENU_CONNECT = 1;
    private static final int MENU_DISCONNECT = 2;

    /*** Main - automatically called methods ***/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initGUI();
        timerHandler = new Handler();
        sendToArduino = new Runnable() {
            public void run() {
                sendLR();
                timerHandler.postDelayed(this, 250); 
            }
        };
    }

    @Override
    public void onResume() 
    {
        super.onResume(); 
        initAccel();



    } 

    @Override
    public void onPause() {
        super.onPause();
        stopController();
        msg("Paused. \n");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        x=event.values[1]/g;    // earth gravity along axis results 1.0
        y=event.values[2]/g;
        z=event.values[0]/g;
        updateLR();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Must have when Activity implements SensorEventListener. 
    }



    /*** User interface ***/

    public boolean onCreateOptionsMenu(Menu menu) { 
    	menu.add(Menu.NONE, MENU_CONNECT, 0, "Connect"); 
    	menu.add(Menu.NONE, MENU_DISCONNECT, 0, "Disconnect"); 
    	return true;
    }
    
    void initGUI()
    {
        // Window
        setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Contents 
        LinearLayout container=new LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        statusTv = new TextView(this);
        statusTv.setBackgroundColor(Color.WHITE);
        statusTv.setTextColor(Color.BLACK);
        
        Log.i("FB", "User interface half way.. ");
        container.addView(statusTv);
        //msg("statusTv added. ");  
        messagesTv = new TextView(this);
        messagesTv.setText("");
        container.addView(messagesTv);
        setContentView(container); 
    }

    public void msg(String s)
    {
        Log.i("FB", s);
        if (7<=messagesTv.getLineCount()) messagesTv.setText("");
        messagesTv.append(s);
    }

    void vibrate()
    {
        Vibrator vibra = (Vibrator) getSystemService(
                Context.VIBRATOR_SERVICE);
        vibra.vibrate(200); 
    } 



    /*** Accelerometer ***/

    void initAccel()
    {
        msg("Accelerometer initialization... ");
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        sensor=sensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(
                this, 
                sensor, 
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    void closeAccel()
    {
        msg("Accelerometer closing... ");
        sensorManager.unregisterListener(this, sensor);
    }



    /*** Bluetooth ***/

    public void startController() {
        msg("Preparing... ");
        timerHandler.postDelayed(sendToArduino, 1000); 
        skipped=9999; // force Bluetooth reconnection
        msg("Running... ");

    }
    public void stopController() {
        r = 0; 
        l = 0;
        sendLR();

        closeAccel();
        closeBluetooth();

        timerHandler.removeCallbacks(sendToArduino);    
    }

    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case MENU_CONNECT:
    		startController();
    		return true;
    	case MENU_DISCONNECT:
    		stopController();
    		return true;
    	}
    	return false;
    }
    
    void initBluetooth()
    {
        msg("Bluetooth initialization... ");
        skipped=0; 
        tBlue=new TBlue(robotBtAddress);
        if (tBlue.streaming()) {
            msg("Bluetooth OK. ");
        } else {
            msg("Error: Bluetooth connection failed. Is Bluetooth turned on? Is your phone paired with the RN42 module?");
        }
    }

    void closeBluetooth()
    {
        msg("Bluetooth closing...");
        tBlue.close();
    }



    /*** Motor calculations for left and right ***/

    void updateLR()
    {
        kick=false;
        if (1.5<Math.abs(y)) kick=true; 
        l=y;
        r=l;
        l+=x;
        r-=x;

        if (l+r<0) { // make reverse turn work like in a car
            float tmp=l;
            l=r;
            r=tmp;
        }

        l=constrain(l);
        r=constrain(r);
    }

    float constrain(float f) 
    {
        if (f<-1) f=-1;
        if (1<f) f=1;
        return f;
    }

    void sendLR()
    {
        if ( (skipped>20) ) {
            closeAccel();
            initBluetooth();
            initAccel();
        }
        if (!tBlue.streaming()) {
            msg("0");
            skipped++;
            return;
        }

        String s="";
        s+="S";
        s+=(char) Math.floor(l*5 + 5);    // 0 <= l <= 10
        s+=(char) Math.floor(r*5 + 5);    // 0 <= l <= 10
        if (kick) s+="k"; else s+="-";
        s+="U";

        statusTv.setText(String.format(
                "%s    left: %3.0f%% right: %3.0f%%, kick: %b.", 
                s, Math.floor(l*100), Math.floor(r*100), 
                kick));

        tBlue.write("?");
        String in=tBlue.read(); 
        msg(""+in);
        if (in.startsWith("L") && tBlue.streaming()) {
            Log.i("fb", "Clear to send, sending... ");
            tBlue.write(s); 
            skipped=0;
        } else {
            Log.i("fb", "Not ready, skipping send. in: \""+ in+"\"");
            skipped++;
            msg("!");
        }
        if (kick) vibrate();
    }

}
