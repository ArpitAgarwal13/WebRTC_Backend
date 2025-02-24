package com.shivam.androidwebrtc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.myhexaville.androidwebrtc.R;
import com.shivam.androidwebrtc.tutorial.CompleteActivity;
import com.shivam.androidwebrtc.tutorial.HTTPClient;

public class LauncherActivity extends AppCompatActivity {
    private static final String tag = "ArpitTest";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        HTTPClient httpClient = new HTTPClient();
        httpClient.getIceServers();
//        Log.d(tag, "ice server data response:" + responseData);


        Button startSession = findViewById(R.id.start_session);

        // when someone clicks on startSession button it should call CompleteActivity class and pass
        // iceServers and start session to that class
        startSession.setOnClickListener(v -> {
            Log.d(tag, "ice server data response:" + httpClient.getIceServerString());
            Intent intent = new Intent(LauncherActivity.this, CompleteActivity.class);
            intent.putExtra("iceServers", httpClient.getIceServerString());
            intent.putExtra("request", "startSession");
            startActivity(intent);
        });

        // create join_session button here, whenever someone click join_session it should read text
        // from get_session_id and create CompleteActivity with iceServers, request = "joinSession" and sessionId

        // read the sessionId from get_session_id edit text box

        Button joinSession = findViewById(R.id.join_session);
        joinSession.setOnClickListener(v -> {
            EditText sessionIdEditText = findViewById(R.id.get_session_id);
            // read sessionId from sessionIdEditText
            String sessionId = sessionIdEditText.getText().toString();

            Log.d(tag, "sessioEditaText: " + sessionIdEditText + "sessionId: " + sessionId + " length: " + sessionIdEditText.length() );

            Log.d(tag, "ice server data response:" + httpClient.getIceServerString());
            Intent intent = new Intent(LauncherActivity.this, CompleteActivity.class);
            intent.putExtra("iceServers", httpClient.getIceServerString());
            intent.putExtra("request", "joinSession");
            intent.putExtra("sessionId", sessionId);
            startActivity(intent);
        });
    }
//    public void openSampleSocketActivity(View view) {
//
//        startActivity(new Intent(this, CompleteActivity.class));
//    }
}
