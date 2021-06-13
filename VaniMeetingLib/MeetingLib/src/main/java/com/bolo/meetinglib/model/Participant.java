package com.bolo.meetinglib.model;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Participant {

    public String userId;
    public Map<String ,String> userData = new HashMap<>();
    public boolean isAdmin;
    public boolean isVideoBlockedByAdmin;
    public boolean isAudioBlockedByAdmin;
    public boolean isMessageBlockedByAdmin;
    public boolean isWhiteboardBlockedByAdmin;
    public String  roomId = "";
    public String socketId = "";
    public boolean  isVideoEnable = false;
    public boolean  isAudioEnable = false;
    public boolean  isStartMeetingCalled = false;


    public Participant(){
        super();
        Log.d("VaniMeeting","Blank Participant");
    }
    public Participant(String userId,String roomId, boolean isAdmin, Map<String ,String > userData) {
        this.userId = userId;
        this.userData = userData;
        this.isAdmin = isAdmin;
        this.roomId = roomId;
    }

    @JsonSetter("userData")
    void setUserData(Map<String, String> userData) {
        try{
            this.userData = userData;
        }
        catch (Exception e){

        }
    }

    public static boolean  isParticipantPresent(List<Participant> allParticipant , String participantId){

        for(Participant participant : allParticipant){
            if(participant.userId == participantId){
                return true;
            }
        }
        return false;
    }

    public static Participant participantByUserId(List<Participant> allParticipant , String participantId){
        for(Participant participant : allParticipant){
            if(participant.userId.equalsIgnoreCase(participantId)){
                return participant;
            }
        }
        return null;
    }



    public static boolean addParticipantIfNotExist(List<Participant> allParticipant, Participant newParticipant){
        for(Participant participant : allParticipant){
            if(participant.userId.equalsIgnoreCase(newParticipant.userId)){
                return false;
            }
        }
        allParticipant.add(newParticipant);
        return true;
    }


    public static void removeParticipant(List<Participant> allParticipant,Participant participant){
        for(int index = 0; index < allParticipant.size(); index ++){
            Participant oldParticipant = allParticipant.get(index);
            if(participant.userId.equalsIgnoreCase( oldParticipant.userId)){
                allParticipant.remove(index);
                break;
            }
        }
    }

    public static void removeParticipantWithId(List<Participant> allParticipant,String participantUserId){
        for(int index = 0; index < allParticipant.size(); index ++){
            Participant oldParticipant = allParticipant.get(index);
            if(participantUserId.equalsIgnoreCase( oldParticipant.userId)){
                allParticipant.remove(index);
                break;
            }
        }
    }

    public JSONObject toJsonObject(){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            String selfParticipantString = objectMapper.writeValueAsString(this);
            return new JSONObject(selfParticipantString);
        }
        catch (Exception e){
            Log.e("erro",e.toString());
        }
        return null;

    }

}
