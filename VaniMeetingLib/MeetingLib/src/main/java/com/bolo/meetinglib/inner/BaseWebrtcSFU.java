package com.bolo.meetinglib.inner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;

import com.bolo.meetinglib.MeetingHandler;
import com.bolo.meetinglib.constant.EglUtils;
import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.Track;

import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;

import static android.content.Intent.ACTION_CONFIGURATION_CHANGED;

public abstract class BaseWebrtcSFU {


    private HandlerDelegate handlerDelegate;
    private PeerConnectionFactory peerFactory;
    protected Context context;
    protected   EglBase rootEglBase = EglUtils.getRootEglBase();
    public CameraVideoCapturer cameraVideoCapturer = null;
    private VideoCapturer screenShareCapture = null;
    private SurfaceTextureHelper surfaceTextureHelper;
    private SurfaceTextureHelper ssSurfaceTextureHelper;
    private OrientationEventListener orientatationListener;
    private BroadcastReceiver mBroadcastReceiver;

    public void setHandlerDelegate(HandlerDelegate handlerDelegate) {
        this.handlerDelegate = handlerDelegate;
    }

    BaseWebrtcSFU(Context context){
        this.context = context;
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());

    }

    public abstract void optimizeStream(JSONObject data);

    public abstract void onAudioMuteUnmute(boolean isMuted);

    public abstract void onVideoPauseResume(boolean isPause);

    public abstract void pauseComsumer(Track track);
    public abstract void resumeComsumer(Track track);
    public abstract void startScreenshare(MediaStream screenshareStream);
    public abstract void updateAudioStream(MediaStream localStream);
    public abstract void updateVideoStream(MediaStream localStream);
    public abstract void handleHandshake(JSONObject data ,MediaStream localStream);
    public abstract void onReconnect(MediaStream localStream);

    public  void destory(){
        try {
            stopCameraCapture();
            onScreenShareStopped();
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
            if (peerFactory != null) {
                peerFactory.dispose();
                peerFactory = null;
            }
            if(ssSurfaceTextureHelper != null){
                ssSurfaceTextureHelper.dispose();
                ssSurfaceTextureHelper = null;
            }

        }
        catch (Exception e) {

        }
    }

    public void   stopCameraCapture(){
        if (cameraVideoCapturer != null) {
            try {
                cameraVideoCapturer.stopCapture();
                cameraVideoCapturer = null;

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
    protected void logEvent(String msg, boolean isError){
        MeetingHandler.getInstance().logEvent(msg,isError);
    }

    public PeerConnectionFactory getPeerFactory(){
        if (peerFactory == null){
            initPeerConnectionFactory();
        }
        return peerFactory;
    }

    private void initPeerConnectionFactory(){

        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(context)
                        .createInitializationOptions()
        );

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 0;


        peerFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(context)
                        .setUseHardwareAcousticEchoCanceler(false).setUseHardwareAcousticEchoCanceler(false).createAudioDeviceModule())
                .setVideoEncoderFactory( new DefaultVideoEncoderFactory(
                        rootEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
                .setOptions(options)

                .createPeerConnectionFactory();


    }
//    protected void initPeerConnectionFactory(){
//        PeerConnectionFactory.initialize(
//                PeerConnectionFactory.InitializationOptions.builder(context)
//                        .createInitializationOptions()
//        );
//
//        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
//        options.networkIgnoreMask = 0;
//
//
//
////        VideoDecoderFactory videoDecoderFactory = new HardwareVideoDecoderFactory(EglBase.create().getEglBaseContext());
//         peerFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
//
////        peerFactory = PeerConnectionFactory.builder()
////                .setAudioDeviceModule(JavaAudioDeviceModule.builder(context)
////                        .setUseHardwareAcousticEchoCanceler(false).setUseHardwareAcousticEchoCanceler(false).createAudioDeviceModule())
////                .setVideoEncoderFactory( new DefaultVideoEncoderFactory(
////                        rootEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, false))
////                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext()))
////                .setOptions(options)
////
////                .createPeerConnectionFactory();
//
//
//
//
//    }


    public MediaStreamTrack getLocalMediaTrack(String type){
        if (type.equalsIgnoreCase("audio")){
            AudioSource audioSource = getPeerFactory().createAudioSource(new MediaConstraints());
            AudioTrack audioTrack = getPeerFactory().createAudioTrack("ARDAMSa0", audioSource);
            return audioTrack;
        }
        if (type.equalsIgnoreCase("video")){
            return createVideoTrack(context);
        }
        return null;
    }


    public void switchCamera(){
        if(cameraVideoCapturer != null){
            cameraVideoCapturer.switchCamera(null);
        }
    }



    public VideoTrack createVideoTrack(Context context) {

        CameraEnumerator cameraEnumerator;
        if (useCamera2()){
            cameraEnumerator = new Camera2Enumerator(context);

        }
        else{
            cameraEnumerator = new Camera1Enumerator(false);
        }

         cameraVideoCapturer = createVideoCapturer(cameraEnumerator, MeetingHandler.getInstance().getMeetingStartRequestModel().defaultCameraDirection);
        boolean isScreencast = cameraVideoCapturer.isScreencast();
        VideoSource mVideoSource = getPeerFactory().createVideoSource(isScreencast);
        cameraVideoCapturer.initialize(surfaceTextureHelper,context, mVideoSource.getCapturerObserver());
        cameraVideoCapturer.startCapture(MeetingHandler.getInstance().getMeetingStartRequestModel().videoCaptureWidth,MeetingHandler.getInstance().getMeetingStartRequestModel().videoCaptureHeight,25);
        VideoTrack localVideoTrack = getPeerFactory().createVideoTrack("ARDAMSv0", mVideoSource);
        return localVideoTrack;
    }

    private boolean useCamera2() {
        return Camera2Enumerator.isSupported(context);
    }

    private CameraVideoCapturer createVideoCapturer(CameraEnumerator enumerator,
                                                    MeetingStartRequest.CameraDirection direction)
    {
        String[] deviceNames = enumerator.getDeviceNames();
        for (String deviceName : deviceNames) {
            if ((direction == MeetingStartRequest.CameraDirection.CAMERA_DIRECTION_FRONT && enumerator.isFrontFacing(deviceName)) ||
                    (direction == MeetingStartRequest.CameraDirection.CAMERA_DIRECTION_BACK  && enumerator.isBackFacing(deviceName)))
            {
                return enumerator.createCapturer(deviceName, null);
            }
        }

        return null;
    }

    public void onScreenShareStopped(){
        if(screenShareCapture != null){
            try{
                screenShareCapture.stopCapture();
                screenShareCapture.dispose();
                screenShareCapture = null;
            }
            catch (Exception e){
                Log.e("errp",e.toString());
            }

        }
        if(this.orientatationListener != null){
            this.orientatationListener.disable();
        }
        if(mBroadcastReceiver != null){
            try{
                context.unregisterReceiver(mBroadcastReceiver);
                mBroadcastReceiver = null;
            }
            catch (Exception e){

            }
        }

    }



    public VideoTrack createScreenShareVideoTrack(VideoCapturer videoCapturer){
        if(videoCapturer == null){
            return null;
        }
        this.screenShareCapture = videoCapturer;
        boolean isScreencast = videoCapturer.isScreencast();
        VideoSource mVideoSource = getPeerFactory().createVideoSource(isScreencast);
        ssSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", rootEglBase.getEglBaseContext());

        videoCapturer.initialize(ssSurfaceTextureHelper,context, mVideoSource.getCapturerObserver());
        videoCapturer.startCapture(MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingWidth,MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingHeight,25);
        VideoTrack localVideoTrack = getPeerFactory().createVideoTrack("SSV0", mVideoSource);


        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent myIntent) {

                if ( myIntent.getAction().equals( ACTION_CONFIGURATION_CHANGED ) ) {

                    if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
                        // it's Landscape
                        Log.d("sachin", "LANDSCAPE");
                        videoCapturer.changeCaptureFormat(MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingHeight,MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingWidth,25);
                    }
                    else {
                        videoCapturer.changeCaptureFormat(MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingHeight,MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingWidth,25);

//                        videoCapturer.changeCaptureFormat(MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingWidth,MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingHeight,25);
                        Log.d("sachin", "PORTRAIT");

                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONFIGURATION_CHANGED);
        context.registerReceiver(mBroadcastReceiver, filter);


//        this.orientatationListener = new OrientationEventListener(context) {
//            @Override
//            public void onOrientationChanged(int orientation) {
//                try {
//                    logEvent("onOrientationChanged" ,false);
//                    videoCapturer.changeCaptureFormat(MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingHeight,MeetingHandler.getInstance().getMeetingStartRequestModel().screenSharingWidth,25);
//                } catch (Exception ex) {
//                    // We ignore exceptions here. The video capturer runs on its own
//                    // thread and we cannot synchronize with it.
//                }
//            }
//        };
//        this.orientatationListener.enable();
        return localVideoTrack;

    }
}



