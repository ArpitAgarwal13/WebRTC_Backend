package com.shivam.androidwebrtc.tutorial;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.gson.Gson;
import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.MessageEvent;
import com.myhexaville.androidwebrtc.R;
import com.myhexaville.androidwebrtc.databinding.ActivitySamplePeerConnectionBinding;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AddIceObserver;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.util.ArrayList;

import io.socket.client.Socket;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class CompleteActivity extends AppCompatActivity {
    private static final String TAG = "ArpitTest";
    private static final int RC_CALL = 111;
    public static final String VIDEO_TRACK_ID = "ARDAMSv0";
    public static final int VIDEO_RESOLUTION_WIDTH = 1280;
    public static final int VIDEO_RESOLUTION_HEIGHT = 720;
    public static final int FPS = 30;

    private Socket socket;
    private boolean isInitiator;
    private boolean isChannelReady;
    private boolean isStarted;


    MediaConstraints audioConstraints;
    MediaConstraints videoConstraints;
    MediaConstraints sdpConstraints;
    VideoSource videoSource;
    VideoTrack localVideoTrack;
    AudioSource audioSource;
    AudioTrack localAudioTrack;
    SurfaceTextureHelper surfaceTextureHelper;

    private ActivitySamplePeerConnectionBinding binding;
    private PeerConnection peerConnection;
    private EglBase rootEglBase;
    private PeerConnectionFactory factory;
    private VideoTrack videoTrackFromCamera;

    private IceServerModel iceServerModel;

    private HTTPClient httpClient = new HTTPClient();

    private String sessionId = "";
    private String peerId = "";

    private String remotePeerId = "";


    // joinSession
    // startSession
    private String requestType = "";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // from the bundle read iceServers, request and sessionId
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            // Extract the values from the Bundle
            String iceServers = extras.getString("iceServers");
            requestType = extras.getString("request");
            if (requestType.equals("joinSession"))
                sessionId = extras.getString("sessionId");

            iceServerModel = new Gson().fromJson(iceServers, IceServerModel.class);
            // Log the values to verify
//            Log.d(TAG, "IceServers: " + iceServers);
            Log.d(TAG, "IceServers Converted: " + iceServerModel);
            Log.d(TAG, "Request: " + requestType);
            Log.d(TAG, "SessionId: " + sessionId);
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_peer_connection);
        setSupportActionBar(binding.toolbar);
        start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onDestroy() {
        if (socket != null) {
            Log.d(TAG,"Important, sendMessage, bye");
            socket.disconnect();
        }
        super.onDestroy();
    }

    @AfterPermissionGranted(RC_CALL)
    private void start() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
//            connectToSignallingServer();

            initializeSurfaceViews();

            initializePeerConnectionFactory();

            // tracks
            createVideoTrackFromCameraAndShowIt();

            initializePeerConnections();


            if(requestType.equalsIgnoreCase("startSession")) {
                registerSession();
                openSSEConnection();
            }

            else if(requestType.equalsIgnoreCase("joinSession")) {
                registerSession();
                openSSEConnection();
            }
            // arpit change

            //arpit change



            startStreamingVideo();
        } else {
            EasyPermissions.requestPermissions(this, "Need some permissions", RC_CALL, perms);
        }
    }

    // Arpit change
    private void registerSession() {
        if(sessionId.isEmpty()) {
            sessionId = IdGenerator.generateSessionId();
        }
        peerId = IdGenerator.generatePeerId();
        Log.d(TAG, "Registering Session, sessionId: " + sessionId + " peerId: " + peerId);
        httpClient.registerSession(sessionId, peerId);
    }

    private void openSSEConnection() {
        Log.d(TAG, "Opening SSE Connection, sessionId: " + sessionId + " peerId: " + peerId);

        startListening(sessionId, peerId);
//        httpClient.openSSEConnection(sessionId, peerId);
    }

    private void startListening(String sessionId, String peerId) {

        String url = "https://api-dt1-dev-aps1.lightmetrics.co:3478/events/" + sessionId + "/" + peerId;
        EventSource eventSource = new EventSource.Builder(new EventHandler() {
            @Override
            public void onOpen() {
                Log.d(TAG, "Connected to SSE Server, connection opened");
            }

            @Override
            public void onClosed() throws Exception {
                Log.d(TAG, "Connected to SSE Server, connection closed");
            }

            @Override
            public void onMessage(String event, MessageEvent messageEvent) throws JSONException {
                Log.d(TAG, "Event: " + event);
                Log.d(TAG, "Data: " + messageEvent.getData());

//                Data: {"senderId":"peer-jx6bwcwef3","type":"new-peer"}

                // sending offer
                JSONObject json = new JSONObject(messageEvent.getData());
                Log.d(TAG, "json : " + json);
                try {
                    if(json != null
                            && json.getString("type") != null) {

                        if(json.getString("type").equals("new-peer")) {
                            remotePeerId = json.getString("senderId");
                            doCall(remotePeerId);
                        }
                        else if(json.getString("type").equals("answer")) {
                            String payload = json.getString("payload");
                            Log.d(TAG, "answer payload : " + payload);
                            JSONObject payloadJson = new JSONObject(payload);
                            String sdp = payloadJson.getString("sdp");

                            Log.d(TAG, "answer sdp : " + sdp);

                            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                                                                    @Override public void onSetSuccess() {
                                                                        super.onSetSuccess();
                                                                        Log.d(TAG, "onSetSuccess: sdp set successfully");
                                                                    }

                                                                    @Override
                                                                    public void onSetFailure(String s) {
                                                                        super.onSetFailure(s);
                                                                        Log.d(TAG, "onSetFailure: sdp failed" + s);
                                                                    }
                                                                },
                                    new SessionDescription(SessionDescription.Type.ANSWER, sdp)
                            );

                            Log.d(TAG, "received answer," +
                                    " iceGatheringState: " + peerConnection.iceGatheringState()
                            + " remoteSDP: " + peerConnection.getRemoteDescription() +
                                    " localSdp: " + peerConnection.getLocalDescription());
                        }
                        else if(json.getString("type").equals("ice-candidate")) {
                            String payload = json.getString("payload");
                            Log.d(TAG, "iceCandidate payload : " + payload);
                            JSONObject payloadJson = new JSONObject(payload);
                            String candidate = payloadJson.getString("candidate");
                            String sdpMid = payloadJson.getString("sdpMid");
                            int sdpMLineIndex = Integer.parseInt(payloadJson.getString("sdpMLineIndex"));
                            Log.d(TAG, "Important sdpMid: " + sdpMid +
                                    " sdpMLineIndex: " + sdpMLineIndex + " candidate: " + candidate);
                            IceCandidate iceCandidate = new IceCandidate(sdpMid, sdpMLineIndex, candidate);
                            peerConnection.addIceCandidate(iceCandidate, new AddIceObserver() {
                                        @Override
                                        public void onAddSuccess() {
                                            Log.d(TAG, "Ice Candidate Added Successfully");
                                        }

                                        @Override
                                        public void onAddFailure(String s) {
                                            Log.d(TAG, "Error Adding Ice Candidate: " + s);
                                        }
                                    });
                        }

                        else if(json.getString("type").equals("offer")) {
                            remotePeerId = json.getString("senderId");
                            String payload = json.getString("payload");
                            Log.d(TAG, "remotePeerId: " + remotePeerId + "offer payload : " + payload);
                            JSONObject payloadJson = new JSONObject(payload);
                            String sdp = payloadJson.getString("sdp");

                            Log.d(TAG, "offer sdp : " + sdp);

                            peerConnection.setRemoteDescription(new SimpleSdpObserver() {
                                                                    @Override public void onSetSuccess() {
                                                                        super.onSetSuccess();
                                                                        Log.d(TAG, "onSetSuccess: " +
                                                                                " sdp set successfully," +
                                                                                " remote connection created");
                                                                    }

                                                                    @Override
                                                                    public void onSetFailure(String s) {
                                                                        super.onSetFailure(s);
                                                                        Log.d(TAG, "onSetFailure: setting sdp failed" + s);
                                                                    }
                                                                },
                                    new SessionDescription(SessionDescription.Type.OFFER, sdp)
                            );

                            Log.d(TAG, "received answer," +
                                    " iceGatheringState: " + peerConnection.iceGatheringState()
                                    + " remoteSDP: " + peerConnection.getRemoteDescription() +
                                    " localSdp: " + peerConnection.getLocalDescription());
                            doAnswer();
                        }
                    }

                } catch (Exception e) {
                    Log.d(TAG, "Exception In Json Parsing, e: " + e.getMessage());
                }
            }

            @Override
            public void onComment(String comment) throws Exception {
                Log.d(TAG, "onComment: " + comment);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

        }, URI.create(url)).build();
        eventSource.start();
    }

    private void connectToSignallingServer() {
//        try {
//            // For me this was "http://192.168.1.220:3000";
//            // $ hostname -I
//            String URL = "https://androidwebrtc-production.up.railway.app/";// "https://calm-badlands-59575.herokuapp.com/"; //
//            Log.e(TAG, "REPLACE ME: IO Socket:" + URL);
//            socket = IO.socket(URL);
//
//            socket.on(EVENT_CONNECT, args -> {
//                Log.d(TAG, "on " + EVENT_CONNECT + ": connectToSignallingServer: connect");
//                // roomId - cuarto
//                socket.emit("create or join", "foo");
//            }).on("ipaddr", args -> {
//                Log.d(TAG, "on ipaddr: connectToSignallingServer: ipaddr");
//            }).on("created", args -> {
//                Log.d(TAG, "on created: connectToSignallingServer: created");
//                isInitiator = true;
//            }).on("full", args -> {
//                Log.d(TAG, "on full: connectToSignallingServer: full");
//            }).on("join", args -> {
//                Log.d(TAG, "on join: connectToSignallingServer: join");
//                Log.d(TAG, "on join: connectToSignallingServer: Another peer made a request to join room");
//                Log.d(TAG, "on join: connectToSignallingServer: This peer is the initiator of room");
//                isChannelReady = true;
//            }).on("joined", args -> {
//                Log.d(TAG, "on joined: connectToSignallingServer: joined");
//                isChannelReady = true;
//            }).on("log", args -> {
//                for (Object arg : args) {
//                    Log.d(TAG, "on log: connectToSignallingServer: " + arg);
//                }
//            }).on("message", args -> {
//                Log.d(TAG, "on message: connectToSignallingServer: got a message");
//            }).on("message", args -> {
//
//                // important
//                // getting message from server on behalf of that adding or sending sdp
//                try {
//                    if (args[0] instanceof String) {
//                        String message = (String) args[0];
//                        if (message.equals("got user media")) {
//                            maybeStart();
//                        }
//                    } else {
//                        JSONObject message = (JSONObject) args[0];
//                        Log.d(TAG, "on message: connectToSignallingServer: got message " + message);
//                        if (message.getString("type").equals("offer")) {
//                            Log.d(TAG, "on message: connectToSignallingServer: received an offer " + isInitiator + " " + isStarted);
//                            if (!isInitiator && !isStarted) {
//                                maybeStart();
//                            }
//                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(OFFER, message.getString("sdp")));
//                            doAnswer();
//                        } else if (message.getString("type").equals("answer") && isStarted) {
//                            peerConnection.setRemoteDescription(new SimpleSdpObserver(), new SessionDescription(ANSWER, message.getString("sdp")));
//                        } else if (message.getString("type").equals("candidate") && isStarted) {
//                            Log.d(TAG, "connectToSignallingServer: receiving candidates");
//                            IceCandidate candidate = new IceCandidate(message.getString("id"), message.getInt("label"), message.getString("candidate"));
//                            peerConnection.addIceCandidate(candidate);
//                        }
//                        /*else if (message === 'bye' && isStarted) {
//                        handleRemoteHangup();
//                    }*/
//                    }
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }).on(EVENT_DISCONNECT, args -> {
//                Log.d(TAG, "on " + EVENT_DISCONNECT + ": connectToSignallingServer: disconnect");
//            });
//            socket.connect();
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
    }



    //MirtDPM4
    private void doAnswer() {
        peerConnection.createAnswer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    message.put("target", remotePeerId);
                    message.put("type", "answer");
                    JSONObject payload = new JSONObject();
                    payload.put("type", "answer");
                    payload.put("sdp", sessionDescription.description);
                    message.put("payload", payload);
                    Log.d(TAG,"Important, sendMessage, doAnswer: " + message);
                    // call backend
                    httpClient.sendMessage(sessionId, peerId, message.toString());
//                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new MediaConstraints());
    }

    private void maybeStart() {
        Log.d(TAG, "maybeStart: " + isStarted + " " + isChannelReady);
        if (!isStarted && isChannelReady) {
            isStarted = true;
            if (isInitiator) {
//                doCall();
            }
        }
    }

    private void doCall(String senderId) {
        MediaConstraints sdpMediaConstraints = new MediaConstraints();

//        sdpMediaConstraints.mandatory.add(
//                new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
//        sdpMediaConstraints.mandatory.add(
//                new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        peerConnection.createOffer(new SimpleSdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onCreateSuccess: ");
                peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                JSONObject message = new JSONObject();
                try {
                    // peerId
                    message.put("target", senderId);
                    message.put("type", "offer");

                    JSONObject payload = new JSONObject();
                    payload.put("type", "offer");
                    payload.put("sdp", sessionDescription.description);
                    message.put("payload", payload);
                    // ArpitChange:  need to print here temporarily
                    Log.d(TAG, "Important: sendMessage, sending offer, message: " + message);
                    httpClient.sendMessage(sessionId, peerId, message.toString());
//                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, sdpMediaConstraints);
    }

//    private void sendMessage(Object message) {
//        socket.emit("message", message);
//    }

    /**
     * To show local and remote video
     */
    private void initializeSurfaceViews() {
        rootEglBase = EglBase.create();
        binding.surfaceView.init(rootEglBase.getEglBaseContext(), null);
        binding.surfaceView.setEnableHardwareScaler(true);
        binding.surfaceView.setMirror(true);

        binding.surfaceView2.init(rootEglBase.getEglBaseContext(), null);
        binding.surfaceView2.setEnableHardwareScaler(true);
        binding.surfaceView2.setMirror(true);

        //add one more
    }

    private void initializePeerConnectionFactory() {

        PeerConnectionFactory.InitializationOptions options = PeerConnectionFactory.InitializationOptions.builder(this)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions();
        PeerConnectionFactory.initialize(options);

        factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(), true, true))
                .setOptions(new PeerConnectionFactory.Options())
                .createPeerConnectionFactory();

        Log.d(TAG, "Peer Connection Factory Created: " + factory);

//        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(getApplicationContext()).createInitializationOptions());
//
//        factory = PeerConnectionFactory.builder().createPeerConnectionFactory();


//        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
//        factory = new PeerConnectionFactory(null);
//        factory.setVideoHwAccelerationOptions(rootEglBase.getEglBaseContext(), rootEglBase.getEglBaseContext());
    }

    private void createVideoTrackFromCameraAndShowIt() {
        audioConstraints = new MediaConstraints();
        VideoCapturer videoCapturer = createVideoCapturer();
        VideoSource videoSource = factory.createVideoSource(false); // false for camera capture
        SurfaceTextureHelper surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());
        videoCapturer.initialize(
                surfaceTextureHelper,
                getApplicationContext(),
                videoSource.getCapturerObserver()
        );
        videoCapturer.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, FPS);

        videoTrackFromCamera = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        videoTrackFromCamera.setEnabled(true);
        videoTrackFromCamera.addSink(binding.surfaceView);

        //create an AudioSource instance
        audioSource = factory.createAudioSource(audioConstraints);
        localAudioTrack = factory.createAudioTrack("101", audioSource);
    }

    private void initializePeerConnections() {
        peerConnection = createPeerConnection(factory);

        Log.d(TAG, "Peer Connection Created, iceConnectionState: " + peerConnection.iceConnectionState());
        Log.d(TAG, "Peer Connection Created, iceGatheringState: " + peerConnection.iceGatheringState());
    }

    private void startStreamingVideo() {
        MediaStream mediaStream = factory.createLocalMediaStream("ARDAMS");
        mediaStream.addTrack(videoTrackFromCamera);
        mediaStream.addTrack(localAudioTrack);
        peerConnection.addStream(mediaStream);

        Log.d(TAG,"Important, sendMessage, got user media");
    }

    private PeerConnection createPeerConnection(PeerConnectionFactory factory) {
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
//        String URL = "stun:stun.l.google.com:19302";

        if(iceServerModel == null) {
            Log.d(TAG, "Ice Servers Missing");
        }
        ArrayList<String> urls = iceServerModel.getUrls();
        for(String url : urls) {
            iceServers.add(PeerConnection.IceServer.builder(url)
                    .setUsername(iceServerModel.getUsername())
                    .setPassword(iceServerModel.getCredential())
                    .createIceServer());
        }



        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        Log.d(TAG, "Peer Connection Created: rtcConfig: " + rtcConfig);

//        MediaConstraints pcConstraints = new MediaConstraints();

        PeerConnection.Observer pcObserver = new PeerConnection.Observer() {
            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {
                Log.d(TAG, "onSignalingChange: ");
            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
                Log.d(TAG, "onIceConnectionChange: ");
            }

            @Override
            public void onIceConnectionReceivingChange(boolean b) {
                Log.d(TAG, "onIceConnectionReceivingChange: ");
            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
                Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);
            }

            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: ");
                JSONObject message = new JSONObject();

                try {
                    message.put("type", "ice-candidate");
//                    message.put("label", iceCandidate.sdpMLineIndex);
                    // peer2
                    message.put("target", remotePeerId);
                    message.put("payload", iceCandidate.sdp);

                    Log.d(TAG, "onIceCandidate: sending candidate " + message);

                    httpClient.sendMessage(sessionId, peerId, message.toString());
                    //
//                    sendMessage(message);

                    Log.d(TAG, "Important: sendMessage, onIceCandidate: message: " + message);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
                Log.d(TAG, "onIceCandidatesRemoved: " + iceCandidates);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                Log.d(TAG, "onAddStream: " + mediaStream.videoTracks.size());
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                remoteAudioTrack.setEnabled(true);
                remoteVideoTrack.setEnabled(true);
                remoteVideoTrack.addSink(binding.surfaceView2);
            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {
                Log.d(TAG, "onRemoveStream: ");
            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {
                Log.d(TAG, "onDataChannel: ");
            }

            @Override
            public void onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded: ");
            }
        };

        Log.d(TAG, "Creating Peer Connection");
        return factory.createPeerConnection(rtcConfig, pcObserver);
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        if (useCamera2()) {
            videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
        } else {
            videoCapturer = createCameraCapturer(new Camera1Enumerator(true));
        }
//        videoCapturer = new CustomVideoCapturer(1280,720,30);
        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(this);
    }

}
