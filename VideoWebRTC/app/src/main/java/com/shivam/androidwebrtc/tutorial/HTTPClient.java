package com.shivam.androidwebrtc.tutorial;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HTTPClient {

    private static final String tag = "ArpitTest";

    private final OkHttpClient client = new OkHttpClient();
    private final String url = "https://api-dt1-dev-aps1.lightmetrics.co:3478/";

    String iceServerString = "";

    private static final Gson gson = new Gson();

    public void getIceServers() {
        Log.d(tag, "Get Ice Servers");
        Request request = new Request.Builder()
                .url(url + "IceServers")
                .build();

//        final IceServerModel[] iceServers = new IceServerModel[1];
//        final String[] responseData = new String[1];
        client.newCall(request).enqueue(new Callback() { // Async call
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.d(tag, "Server Error: " + response.code());
                    return;
                }
                String responseStr = response.body().string();
                Log.d(tag, "Response: " + response);
                iceServerString = responseStr;
//                iceServers[0] = gson.fromJson(responseData[0], IceServerModel.class);
//                Log.d(tag, "Response After Parsing " + iceServers[0]);
            }
        });

        Log.d(tag, "Response: " + iceServerString);
    }

    public void registerSession(String sessionId, String peerId) {
        Log.d(tag, "Register Session");


        MediaType JSON = MediaType.get("application/json; charset=utf-8");
//        String jsonBody = "{ \"name\": \"John Doe\", \"age\": 30 }";
        String jsonBody = "";
        RequestBody body = RequestBody.create(jsonBody, JSON);




        Request request = new Request.Builder()
                .url(url + "register/" + sessionId + "/" + peerId)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(!response.isSuccessful()) {
                    Log.d(tag, "Server Error: " + response.code());
                    return;
                }
                Log.d(tag, "Session Registered Successfully");
            }
        });
    }

    public void sendMessage(String sessionId,
                            String peerId,
                            String message) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        String jsonString = message.toString();
        RequestBody requestBody = RequestBody.create(jsonString, JSON);

        Request request = new Request.Builder()
                .url(url + "message/" + sessionId + "/" + peerId)
                .post(requestBody)
                .build();

        Log.d(tag, "sendOffer: request: " + request);


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(!response.isSuccessful()) {
                    Log.d(tag, "Server Error: " + response.code());
                    return;
                }
                Log.d(tag, "Send Offer Successfully");
            }
        });
    }

    public String getIceServerString() {
        return iceServerString;
    }

}
