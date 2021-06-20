package com.bolo.meetinglib.constant;

import org.webrtc.PeerConnection;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Config {

//    public static List<> iceServers(String  appId){//SAchin
//
//    }
    public static LinkedList<PeerConnection.IceServer> getIceServers(String  appId){
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302","", "", PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));

        if(appId.equalsIgnoreCase( "testing")){
            iceServers.add(new PeerConnection.IceServer("stun:stundoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE));
            iceServers.add(new PeerConnection.IceServer("turn:turndoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE));
        }
        else if(appId .equalsIgnoreCase( "demo")){
            iceServers.add(new PeerConnection.IceServer("stun:stundoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turndoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "yqgpros")){
            iceServers.add(new PeerConnection.IceServer("stun:stun.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));        }
        else if(appId .equalsIgnoreCase( "uhc")){
            iceServers.add(new PeerConnection.IceServer("stun:stun.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));        }

        else if(appId .equalsIgnoreCase( "xpertflix")){
            iceServers.add(new PeerConnection.IceServer("stun:stun.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "ind1")){
            iceServers.add(new PeerConnection.IceServer("stun:stunin.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turnin.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "ind2")){
            iceServers.add(new PeerConnection.IceServer("stun:stunin1.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turnin1.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "ind3")){
            iceServers.add(new PeerConnection.IceServer("stun:stunin2.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turnin2.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "ind4")){
            iceServers.add(new PeerConnection.IceServer("stun:stun.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "fra1")){
            iceServers.add(new PeerConnection.IceServer("stun:stun2.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn2.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "nyc1")){
            iceServers.add(new PeerConnection.IceServer("stun:stun1.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn1.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else if(appId .equalsIgnoreCase( "trail")){
            iceServers.add(new PeerConnection.IceServer("stun:stun.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turn.onrx.ca:3478","uhc","12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }
        else{
            iceServers.add(new PeerConnection.IceServer("stun:stundoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
            iceServers.add(new PeerConnection.IceServer("turn:turndoubtconnect.vaniassistant.com:3478","sachin","Hhands@12345",PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK));
        }

        return iceServers;
    }
    public static String wssUrl(String  appId){
        if(appId .equalsIgnoreCase( "testing")){
            return "wss://testing.vaniassistant.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "demo")){
            return "wss://demoserver.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "yqgpros")){
            return "wss://meetingserver.yqgtech.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "uhc")){
            return "wss://meetingserver.yqgtech.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "xpertflix")){
            return "wss://testing.vaniassistant.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "ind1")){
            return "wss://ind1.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "ind2")){
            return "wss://ind2.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "ind3")){
            return "wss://ind3.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "ind4")){
            return "wss://ind4.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "fra1")){
            return "wss://fra1.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "nyc1")){
            return "wss://nyc1.vanimeetings.com/?connection=";
        }
        else if(appId .equalsIgnoreCase( "trail")){
            return "wss://trialserver.vanimeetings.com/?connection=";
        }
        return "wss://testing.vaniassistant.com/?connection=";
    }

    private long minBitrateConfig(String appId){
        return 40000;
    }
    public static Integer maxBitRateConfig(String appId){
        if(appId .equalsIgnoreCase( "uhc") || appId .equalsIgnoreCase( "yqgpros")){
            return 620000;
        }
        return 420000;
    }
    public static String urlToCheckInternetPresent(String appId){
        if(appId .equalsIgnoreCase( "yqgpros")){
            return "https://meetingserver.yqgpros.com/";
        }
        else if(appId .equalsIgnoreCase( "uhc")){
            return "https://meetingserver.onrx.ca/";
        }
        return "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&explaintext&redirects=1&titles=Stack%20Overflow";
    }
}
