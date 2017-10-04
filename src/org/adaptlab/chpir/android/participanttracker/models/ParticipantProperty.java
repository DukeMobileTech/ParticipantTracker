package org.adaptlab.chpir.android.participanttracker.models;

import android.util.Log;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import org.adaptlab.chpir.android.activerecordcloudsync.SendReceiveModel;
import org.adaptlab.chpir.android.participanttracker.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.UUID;

@Table(name = "ParticipantProperty")
public class ParticipantProperty extends SendReceiveModel {
    private static final String TAG = "ParticipantProperty";

    @Column(name = "SentToRemote")
    private boolean mSent;
    @Column(name = "Participant", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete = Column.ForeignKeyAction.CASCADE)
    private Participant mParticipant;
    @Column(name = "Property", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete = Column.ForeignKeyAction.CASCADE)
    private Property mProperty;
    @Column(name = "Value")
    private String mValue;
    @Column(name = "UUID")
    private String mUUID;
    @Column(name = "RemoteId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private Long mRemoteId;
    @Column(name = "Changed")
    private boolean mChanged;

    public ParticipantProperty() {
        super();
        mSent = false;
        mUUID = UUID.randomUUID().toString();
    }

    public ParticipantProperty(Participant participant, Property property, String value) {
        super();
        mSent = false;
        mParticipant = participant;
        mProperty = property;
        mValue = value;
        mUUID = UUID.randomUUID().toString();
    }

    public static ParticipantProperty findByRemoteId(Long id) {
        return new Select().from(ParticipantProperty.class).where("RemoteId = ?", id)
                .executeSingle();
    }

    public static List<ParticipantProperty> getAllByParticipant(Participant participant) {
        return new Select().from(ParticipantProperty.class).where("Participant = ?", participant
                .getId()).execute();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("participant_property", asJsonObject());
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        return json;
    }

    @Override
    public JSONObject asJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (getParticipant() != null) jsonObject.put("participant_uuid", getParticipant().getUUID());
            if (getProperty() != null) jsonObject.put("property_id", getProperty().getRemoteId());
            jsonObject.put("value", getValue());
            jsonObject.put("uuid", getUUID());
            if (getRemoteId() != null) {
                jsonObject.put("id", this.getRemoteId());
            }
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        return jsonObject;
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
        mChanged = false;
        save();
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    public Long getRemoteId() {
        return mRemoteId;
    }

    public void setRemoteId(Long id) {
        mRemoteId = id;
    }

    public Participant getParticipant() {
        return mParticipant;
    }

    private void setParticipant(Participant participant) {
        mParticipant = participant;
    }

    public Property getProperty() {
        return mProperty;
    }

    private void setProperty(Property property) {
        mProperty = property;
    }

    public String getValue() {
        return mValue;
    }

    public String getUUID() {
        return mUUID;
    }

    public void setUUID(String uuid) {
        mUUID = uuid;
    }

    public void setValue(String value) {
        mValue = value;
    }

    @Override
    public void createObjectFromJSON(JSONObject jsonObject) {
        try {
            String uuid = jsonObject.getString("uuid");
            ParticipantProperty participantProperty = ParticipantProperty.findByUUID(uuid);
            if (participantProperty == null) {
                participantProperty = this;
            }
            participantProperty.setUUID(uuid);
            Long remoteId = jsonObject.getLong("id");
            participantProperty.setRemoteId(remoteId);
            Participant participant = Participant.findByUUID(jsonObject.getString
                    ("participant_uuid"));
            if (participant != null) {
                participantProperty.setParticipant(participant);
            }

            Property property = Property.findByRemoteId(jsonObject.getLong("property_id"));

            if (property != null) {
                participantProperty.setProperty(property);
            }
            participantProperty.setValue(jsonObject.getString("value"));
            if (jsonObject.isNull("deleted_at")) {
                participantProperty.setChanged(false);
                participantProperty.save();
            } else {
                ParticipantProperty pp = ParticipantProperty.findByUUID(uuid);
                if (pp != null) {
                    pp.delete();
                }
            }

        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error parsing object json", je);
        }
    }

    @Override
    public boolean isChanged() {
        return mChanged;
    }

    @Override
    public boolean belongsToCurrentProject() {
        return AdminSettings.getInstance().getProjectId() != null && mParticipant != null && mParticipant.getProjectId() != null && mParticipant.getProjectId().equals(AdminSettings.getInstance().getProjectId());
    }

    public void setChanged(boolean changed) {
        mChanged = changed;
    }

    public static ParticipantProperty findByUUID(String uuid) {
        return new Select().from(ParticipantProperty.class).where("UUID = ?", uuid).executeSingle();
    }
}