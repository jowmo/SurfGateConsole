package com.morris.surfgate;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private static int mProgressValue = 0;


// bluetooth test 2
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
// bluetooth test 2


    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    //private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            //mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        setProgressBar(mProgressValue);

        //Intent intent = new Intent(this, MyService.class);
        //bindService(intent, mConnnection, Context.BIND_AUTO_CREATE);

        //btAdapter = BluetoothAdapter.getDefaultAdapter();
        //CheckBTState();

        findBT();

        try
        {
            openBT();
        }
        catch (IOException ex) {
            AlertBox("Bluetooth Exception in openBT()", ex.getMessage());
        }


    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        setProgressBar(mProgressValue);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    public void setStatus(String message) {
        TextView text = (TextView) findViewById(R.id.txt_status);
        text.setText(message);
    }

    public void setProgressBar(int progress) {
        ProgressBar bar = (ProgressBar) findViewById(R.id.progress_bar);
        bar.setProgress(progress);
        updateProgressText();
    }


    /*
     * Bluetooth Stuff...
     */
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    // Bluetooth SPP UUID and SurfGate Controller (Arduino) Mac Address
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String ARDUINO_MAC_ADDRESS = "20:15:07:24:52:99";



    // format: fix, satellies, knots, direction, in_motion, percent_deployed
    public void ParseResponse(String data) {
        try {
            sendMessage("OK");
        }
        catch (IOException ioe) {

        }

        String fix = "0";
        int sats = 0;
        float knots = 0;
        String direction = "";
        String in_motion = "";
        int gate_angle = 0;

        if (data.contains(",")) {
            String[] parts = data.split(",");
            fix = parts[0];
            sats = SafeInt(parts[1], 0);
            knots = SafeFloat(parts[2], 0);
            direction = parts[3];
            in_motion = parts[4];
            gate_angle = SafeInt(parts[5], 0);
        }

        // Update UI
        setFixStatus(fix);
        setSatelliteCount(sats);
        setSpeed(knots, true);
        setDirection(direction);
        setMotionStatus(in_motion);
        setProgressBar(gate_angle);

    }

    private void setSpeed(float speed, boolean in_knots) {
        TextView text = (TextView) findViewById(R.id.txt_speed);
        if (in_knots) {
            speed *= 1.15078;
        }
        text.setText(String.format("%.2f", speed));
    }

    public int SafeInt(String s, int def) {
        int tmp;
        try {
            tmp = Integer.parseInt(s);
        }
        catch (Exception e) {
            tmp = def;
        }
        return tmp;
    }


    public float SafeFloat(String s, float def) {
        float tmp;
        try {
            tmp = Float.parseFloat(s);
        }
        catch (Exception e) {
            tmp = def;
        }
        return tmp;
    }


    public void AlertBox( String title, String message ){
        new AlertDialog.Builder(this)
        .setTitle( title )
        .setMessage( message + " Press OK to exit." )
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
            finish();
        }
            }).show();
        }


    /**
     * Functional Stuff...
     */
    public void rightClicked(View view) {
        try {
            sendMessage("STARBOARD");
        } catch (IOException ignore) {}
    }

    public void offClicked(View view) {
        setStatus("SYSTEM DISABLED");
        try {
            sendMessage("DISABLED");
        } catch (IOException ignore) {}

    }

    public void leftClicked(View view) {
        try {
            sendMessage("PORT");
        } catch (IOException ignore) {
            setStatus("sendMessage Exception!");
        }
    }

    private void updateProgressText() {
        TextView text = (TextView) findViewById(R.id.txt_progress);
        String textProgress = mProgressValue + "%";
        text.setText(textProgress);
    }

    public void gateAdjustIncrease(View view) {
        if (mProgressValue < 100) {
            mProgressValue += 10;
        }
        setProgressBar(mProgressValue);
    }

    public void gateAdjustDecrease(View view) {
        if (mProgressValue > 0) {
            mProgressValue -= 10;
        }
        setProgressBar(mProgressValue);
    }

    public void openLogView(View view) {
        Intent myIntent = new Intent(this, LogActivity.class);
        startActivity(myIntent);
    }


    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            setStatus("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        //mmDevice = mBluetoothAdapter.getRemoteDevice(ARDUINO_MAC_ADDRESS);

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("SurfGate"))
                {
                    mmDevice = device;
                    setStatus("Bluetooth Device Found");
                    break;
                }
            }
        }
        else {
            setStatus("Bluetooth Device NOT Found");
        }


    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        setStatus("Bluetooth Opened");
        beginListenForData();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        for (int i=0; i<10000; i++) {
            try {
                int bytesAvailable = mmInputStream.available();
                if (bytesAvailable > 0) {
                    setStatus("Bytes!!!");
                }
            } catch (IOException ig) {
            }
        }
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try {
                        Thread.currentThread().sleep(1000);
                    } catch (Exception time) {}
                    try
                    {
                        final int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            ParseResponse(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendMessage(String message) throws IOException
    {
        message += "\n";
        mmOutputStream.write(message.getBytes());
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        setStatus("Bluetooth Closed");
    }


    public void setSatelliteCount(int acquiredSats) {
        TextView text = (TextView) findViewById(R.id.txt_satelites);
        text.setText(Integer.toString(acquiredSats));
    }

    public void setFixStatus(String fixStatus) {
        //this.fixStatus = fixStatus;
    }

    public void setDirection(String direction) {
        setStatus("DIRECTION: " + direction);
    }

    public void setMotionStatus(String motionStatus) {
        //this.motionStatus = motionStatus;
    }
}
