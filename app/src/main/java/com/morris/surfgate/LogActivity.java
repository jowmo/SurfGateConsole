package com.morris.surfgate;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

public class LogActivity extends AppCompatActivity {

    private View mLogContentView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        mLogContentView = findViewById(R.id.fullscreen_log_content);
        mLogContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);


        Date d;
        for(int i=0; i < 10; i++) {
            d = new Date();
            writeLog("log entry " + d.toString());
        }

        /*
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Date now = new Date();
                        writeLog("" + now.getTime());
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        */
    }

    public void writeLog(String msg) {
        TextView log;
        log = (TextView) findViewById(R.id.txt_log);
        log.setText(msg + "\n" + log.getText());
    }

    public void closeLogActivity(View view) {
        Intent myIntent = new Intent(this, FullscreenActivity.class);
        startActivity(myIntent);
    }

}
