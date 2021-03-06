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

@Table(name = "Relationship")
public class Relationship extends SendReceiveModel {
    private static final String TAG = "Relationship";

    @Column(name = "RemoteId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private Long mRemoteId;
    @Column(name = "SentToRemote")
    private boolean mSent;
    @Column(name = "ParticpantOwner", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete =
            Column.ForeignKeyAction.CASCADE)
    private Participant mParticipantOwner;
    @Column(name = "ParticipantRelated", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete =
            Column.ForeignKeyAction.CASCADE)
    private Participant mParticipantRelated;
    @Column(name = "UUID")
    private String mUUID;
    @Column(name = "RelationshipType")
    private RelationshipType mRelationshipType;
    @Column(name = "Changed")
    private boolean mChanged;

    public Relationship() {
        super();
    }

    public Relationship(RelationshipType relationshipType) {
        super();
        mUUID = UUID.randomUUID().toString();
        mRelationshipType = relationshipType;
    }

    public static Relationship findByRemoteId(Long id) {
        return new Select().from(Relationship.class).where("RemoteId = ?", id).executeSingle();
    }

    public static List<Relationship> getAll() {
        return new Select().from(Relationship.class).orderBy("Id ASC").execute();
    }

    public static List<Relationship> getAllByParticipant(Participant participant) {
        return new Select().from(Relationship.class).where("ParticpantOwner = ? OR " +
                "ParticipantRelated = ?", participant.getId(), participant.getId()).execute();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("relationship", asJsonObject());
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        return json;
    }

    @Override
    public JSONObject asJsonObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (getParticipantOwner() != null) jsonObject.put("participant_owner_uuid", getParticipantOwner().getUUID());
            if (getParticipantRelated() != null) jsonObject.put("participant_related_uuid", getParticipantRelated().getUUID());
            jsonObject.put("relationship_type_id", getRelationshipType().getRemoteId());
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

    private void setRemoteId(Long remoteId) {
        mRemoteId = remoteId;
    }

    public Participant getParticipantOwner() {
        return mParticipantOwner;
    }

    public void setParticipantOwner(Participant participantOwner) {
        mParticipantOwner = participantOwner;
    }

    public Participant getParticipantRelated() {
        return mParticipantRelated;
    }

    public void setParticipantRelated(Participant participantRelated) {
        mParticipantRelated = participantRelated;
    }

    public RelationshipType getRelationshipType() {
        return mRelationshipType;
    }

    public String getUUID() {
        return mUUID;
    }

    private void setUUID(String uUID) {
        mUUID = uUID;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        mRelationshipType = relationshipType;
    }

    @Override
    public void createObjectFromJSON(JSONObject jsonObject) {
        try {
            String uuid = jsonObject.getString("uuid");
            Relationship relationship = Relationship.findByUUID(uuid);
            if (relationship == null) {
                relationship = this;
            }
            relationship.setUUID(uuid);
            Long remoteId = jsonObject.getLong("id");
            relationship.setRemoteId(remoteId);
            Participant participantOwner = Participant.findByUUID(jsonObject.getString
                    ("participant_owner_uuid"));
            if (participantOwner != null) {
                relationship.setParticipantOwner(participantOwner);
            }

            Participant participantRelated = Participant.findByUUID(jsonObject.getString
                    ("participant_related_uuid"));
            if (participantRelated != null) {
                relationship.setParticipantRelated(participantRelated);
            }

            relationship.setRelationshipType(RelationshipType.findByRemoteId(jsonObject.getLong
                    ("relationship_type_id")));

            if (jsonObject.isNull("deleted_at")) {
                relationship.setChanged(false);
                relationship.save();
            } else {
                Relationship rs = Relationship.findByUUID(uuid);
                if (rs != null) {
                    rs.delete();
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
        return AdminSettings.getInstance().getProjectId() != null && mParticipantOwner != null && mParticipantOwner.getProjectId() != null && mParticipantOwner.getProjectId().equals(AdminSettings.getInstance().getProjectId());
    }

    public void setChanged(boolean changed) {
        mChanged = changed;
    }

    public static Relationship findByUUID(String uuid) {
        return new Select().from(Relationship.class).where("UUID = ?", uuid).executeSingle();
    }

}