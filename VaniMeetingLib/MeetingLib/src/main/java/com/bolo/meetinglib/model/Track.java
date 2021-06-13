package com.bolo.meetinglib.model;

import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.MediaStreamTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Track {

    public  String trackId;
    public  String videoType;
    public  String kind;
    public  String cameraType;
    public MediaStreamTrack track;
    public  boolean isLocalTrack;
    public Map<String,String> extraData = new HashMap<>();
    public Participant participant = null;
    public  boolean isStreamPresent = true;
//    public Consumer consumer = null;//Sachin

    public Track(){}
    public Track(String trackId, MediaStreamTrack track, boolean isLocalTrack,String videoType) {
        this.trackId = trackId + "_" + videoType;
        this.videoType = videoType;
        if(track != null){
            this.track = track;
            this.kind = track.kind();
            this.cameraType = "front";

        }
        else{
            this.kind = "video";
            this.cameraType = "back";
        }
        this.track = track;
        this.isLocalTrack = isLocalTrack;
    }

    public void updateTrackData(String trackId,MediaStreamTrack track){
        if(track != null){
            this.track = track;
            this.kind = track.kind();
            this.videoType = track.kind();
            this.cameraType = "front";
        }
    }

    public static Track checkAndRemoveTrack(List<Track> allTracks, String trackId){
        for(int index = 0 ; index < allTracks.size(); index ++ ){
            Track track = allTracks.get(index);
            if(track.trackId.equalsIgnoreCase( trackId)){
                allTracks.remove(index);
                return track;
            }
        }
        return null;
    }

    public static boolean checkAndAddTrack(List<Track> allTracks, Track _track){
        for(Track track : allTracks){
            if(track.trackId.equalsIgnoreCase(  _track.trackId ))
            {
                return false;
            }
        }
        allTracks.add(_track);
        return true;
    }

    public  static  Track getTrackOfUserId(List<Track> allTracks,String trackType,String userId,String videoType){
        for(Track track : allTracks){
            if(track.participant.userId .equalsIgnoreCase( userId )&& track.kind.equalsIgnoreCase( trackType  ))
            {
                if(trackType .equalsIgnoreCase( "audio")){
                    return track;

                }
                if(videoType .equalsIgnoreCase( track.videoType)){
                    return track;
                }
            }
        }
        return null;
    }

    public  static List<Track> getAllTrackOfUserId(List<Track> allTracks,String userId){
        List<Track> tracks = new ArrayList<>();
        for(Track track : allTracks){
            if(track.participant.userId .equalsIgnoreCase( userId  ))
            {
                tracks.add(track);
            }
        }
        return tracks;
    }

    public  static  Track  getLocalTrack (List<Track> allTracks,String kind,String videoType){
        for(Track track : allTracks){
            if(track.isLocalTrack && track.kind .equalsIgnoreCase( kind) && videoType .equalsIgnoreCase( track.videoType))
            {
                if(kind .equalsIgnoreCase( "audio")){
                    return track;

                }
                if(videoType .equalsIgnoreCase( track.videoType)){
                    return track;
                }
            }
        }
        return null;
    }

    public  static  Track   getTrackById(List<Track> allTracks,String trackId){
        for(Track track : allTracks){
            if(track.trackId .equalsIgnoreCase( trackId) )
            {
                return track;
            }
        }
        return null;
    }

    public  static  Track   trackByProducerId(List<Track>  allTracks,String producerId){
        for(Track track : allTracks){

            //Sachin
//            if(track.consumer != null && track.consumer.appData != null && track.consumer.appData.producerData != null && track.consumer.appData.producerData.producerId != null){
//                if(track.consumer.appData.producerData.producerId === producerId){
//                    return track;
//                }
//            }
        }
        return null;

    }
}
