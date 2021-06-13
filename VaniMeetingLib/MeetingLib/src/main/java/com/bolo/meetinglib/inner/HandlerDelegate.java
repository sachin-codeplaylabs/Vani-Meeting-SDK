package com.bolo.meetinglib.inner;

import com.bolo.meetinglib.model.MeetingStartRequest;
import com.bolo.meetinglib.model.Participant;
import com.bolo.meetinglib.model.Track;

import org.json.JSONObject;

import java.util.List;

public interface HandlerDelegate {
    public void onIceCandidateDisconnected();
    public void sendMessageToSocket(JSONObject msgToSend);
    public MeetingStartRequest getMeetingStartRequestModel();
    public void sendSFUMessageToSocket(JSONObject msgToSend);
    public void emitToSource(String  type  , Object emitData );
    public List<Participant> getParticipantFromLocal();
    public boolean addTrack(Track track);
    public List<Track> getAllTracks();
}
