package org.adaptlab.chpir.android.models;

import java.util.List;
import java.util.UUID;

import org.adaptlab.chpir.android.activerecordcloudsync.SendModel;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

@Table(name = "Participant")
public class Participant extends SendModel {
    private static final String TAG = "Participant";
    
    @Column(name = "SentToRemote")
    private boolean mSent;
    @Column(name = "ParticipantType")
    private ParticipantType mParticipantType;
    @Column(name = "UUID")
    private String mUUID;
    
    public Participant(ParticipantType participantType) {
        super();
        mParticipantType = participantType;
        mUUID = UUID.randomUUID().toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        
        try {
            JSONObject jsonObject = new JSONObject();
            // TODO: Change to participant id
            jsonObject.put("participant_type_id", getParticipantType().getRemoteId());
            jsonObject.put("uuid", getUUID());
            
            json.put("participant", jsonObject);
        } catch (JSONException je) {
            Log.e(TAG, "JSON exception", je);
        }
        return json;
    }
    
    public ParticipantType getParticipantType() {
        return mParticipantType;
    }
    
    public String getUUID() {
        return mUUID;
    }
    
    public static List<Participant> getAll() {
        return new Select().from(Participant.class).orderBy("Id ASC").execute();
    }
    
    public static List<Participant> getAllByParticipantType(ParticipantType participantType) {
        return new Select().from(Participant.class).where("ParticipantType = ?", participantType.getId()).execute();
    }
    
    public static int getCount() {
        return getAll().size();
    }
    
    public static int getCountByParticipantType(ParticipantType participantType) {
        return getAllByParticipantType(participantType).size();
    }

    @Override
    public boolean isSent() {
        return mSent;
    }

    @Override
    public boolean readyToSend() {
        return true;
    }

    @Override
    public void setAsSent() {
        mSent = true;
    }
}
