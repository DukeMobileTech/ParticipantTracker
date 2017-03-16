package org.adaptlab.chpir.android.participanttracker.models;

import android.util.Log;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import org.adaptlab.chpir.android.activerecordcloudsync.SendReceiveModel;
import org.adaptlab.chpir.android.participanttracker.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "Participant")
public class Participant extends SendReceiveModel {
    private static final String TAG = "Participant";
    private static final String LABEL_DELIMITER = " ";

    @Column(name = "SentToRemote")
    private boolean mSent;
    @Column(name = "ParticipantType", onUpdate = Column.ForeignKeyAction.CASCADE, onDelete =
            Column.ForeignKeyAction.CASCADE)
    private ParticipantType mParticipantType;
    @Column(name = "UUID")
    private String mUUID;
    @Column(name = "RemoteId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    private Long mRemoteId;
    @Column(name = "Changed")
    private boolean mChanged;
    @Column(name = "ProjectId")
    private Long mProjectId;

    public Participant() {
        super();
        mUUID = UUID.randomUUID().toString();
        mSent = false;
    }

    public Participant(ParticipantType participantType) {
        super();
        mParticipantType = participantType;
        mUUID = UUID.randomUUID().toString();
        mSent = false;
    }

    public static List<Participant> getAllByParticipantType(ParticipantType participantType,
                                                            String query) {
        return new Select("Participant.*")
                .distinct()
                .from(Participant.class)
                .innerJoin(ParticipantProperty.class)
                .on("Participant.Id = ParticipantProperty.Participant AND Participant" +
                                ".ParticipantType = ? AND Participant.ProjectId = ? AND " +
                        "ParticipantProperty.Value LIKE ?", participantType.getId(),
                        getCurrentProjectId(), "%" + query + "%")
                .orderBy("Participant.Id DESC")
                .execute();
    }

    private static String getCurrentProjectId() {
        return (AdminSettings.getInstance().getProjectId() == null) ? "-1" :
                    AdminSettings.getInstance().getProjectId().toString();
    }

    public static int getCount() {
        return getAll().size();
    }

    public static List<Participant> getAll() {
        return new Select().from(Participant.class).orderBy("Id ASC").execute();
    }

    public static int getCountByParticipantType(ParticipantType participantType) {
        return getAllByParticipantType(participantType).size();
    }

    public static List<Participant> getAllByParticipantType(ParticipantType participantType) {
        Property sortingProperty = new Select()
                .from(Property.class)
                .where("Property.ParticipantType = ? AND Property.UseToSort = ?", participantType
                        .getId(), 1)
                .executeSingle();
        if (sortingProperty != null) {
            return new Select("Participant.*")
                    .distinct()
                    .from(Participant.class)
                    .innerJoin(ParticipantProperty.class)
                    .on("Participant.Id = ParticipantProperty.Participant AND ParticipantProperty" +
                            ".Property = ? AND Participant.ProjectId = ?", sortingProperty.getId(),
                            getCurrentProjectId())
                    .orderBy("ParticipantProperty.Value")
                    .execute();
        } else {
            return new Select()
                    .from(Participant.class)
                    .where("ParticipantType = ? AND ProjectId = ?", participantType.getId(),
                            getCurrentProjectId())
                    .orderBy("Id DESC")
                    .execute();
        }
    }

    public static Participant findById(Long id) {
        return new Select().from(Participant.class).where("Id = ?", id).executeSingle();
    }

    public static Participant findByRemoteId(Long remoteId) {
        return new Select().from(Participant.class).where("RemoteId = ?", remoteId).executeSingle();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();

        try {
            JSONObject jsonObject = new JSONObject();
            // TODO: Change to participant id
            jsonObject.put("participant_type_id", getParticipantType().getRemoteId());
            jsonObject.put("uuid", getUUID());
            jsonObject.put("project_id", AdminSettings.getInstance().getProjectId());
            // TODO: 3/12/17 Delete participants that have been deleted remotely
            if (this.getRemoteId() == null) {
                jsonObject.put("device_uuid", AdminSettings.getInstance().getDeviceIdentifier());
                jsonObject.put("device_label", AdminSettings.getInstance().getDeviceLabel());
            }

            json.put("participant", jsonObject);
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        return json;
    }

    public ParticipantType getParticipantType() {
        return mParticipantType;
    }

    public void setParticipantType(ParticipantType participantType) {
        mParticipantType = participantType;
    }

    public String getUUID() {
        return mUUID;
    }

    public void setUUID(String uuid) {
        mUUID = uuid;
    }

    @Override
    public boolean isSent() {
        return mSent;
    }

    @Override
    public boolean readyToSend() {
        return true; //TODO: For Testing...FIX - also in ParticipantProperty
    }

    @Override
    public void setAsSent() {
        mSent = true;
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

    private void setRemoteId(Long id) {
        mRemoteId = id;
    }

    public Long getProjectId() {
        return mProjectId;
    }

    public void setProjectId(Long id) {
        mProjectId = id;
    }

    public boolean hasParticipantProperty(Property property) {
        if (getId() == null) return false;

        for (ParticipantProperty participantProperty : getParticipantProperties()) {
            if (participantProperty.getProperty().equals(property)) {
                return true;
            }
        }

        return false;
    }

    /*
     * If a participant property already exist for this type, return it.
     *
     * If it does not exist, return a new participant property initialized
     * with a null value.
     *
     */
    public ParticipantProperty getParticipantProperty(Property property) {
        for (ParticipantProperty participantProperty : getParticipantProperties()) {
            if (participantProperty.getProperty().equals(property)) {
                return participantProperty;
            }
        }

        return new ParticipantProperty(this, property, null);
    }

    /*
     * If a relationship already exist for this type, return it.
     *
     * If it does not exist, return a new relationship.
     *
     */
    public Relationship getRelationshipByRelationshipType(RelationshipType relationshipType) {
        for (Relationship relationship : getRelationships()) {
            if (relationship.getRelationshipType().equals(relationshipType)) {
                return relationship;
            }
        }

        return new Relationship(relationshipType);
    }

    public List<Relationship> getRelationships() {
        return new Select().from(Relationship.class).where("ParticpantOwner = ?", getId())
                .orderBy("RelationshipType ASC").execute();
    }

    public boolean hasRelationshipByRelationshipType(RelationshipType relationshipType) {
        if (getId() == null) return false;

        for (Relationship relationship : getRelationships()) {
            if (relationship.getRelationshipType().equals(relationshipType)) {
                return true;
            }
        }

        return false;
    }

    public List<Property> getProperties() {
        return Property.getAllByParticipantType(getParticipantType());
    }

    public String getLabel() {
        String label = "";
        for (ParticipantProperty participantProperty : getParticipantProperties()) {
            if (participantProperty.getProperty() != null && participantProperty.getProperty()
                    .getUseAsLabel()) {
                if (!label.isEmpty()) label += LABEL_DELIMITER;
                label += participantProperty.getValue();
            }
        }

        if (label.isEmpty()) {
            return mUUID;
        } else {
            return label;
        }
    }

    public List<ParticipantProperty> getParticipantProperties() {
        return new Select().from(ParticipantProperty.class).where("Participant = ?", getId())
                .execute();
    }

    @Override
    public void createObjectFromJSON(JSONObject jsonObject) {
        try {
            String uuid = jsonObject.getString("uuid");
            Participant participant = Participant.findByUUID(uuid);
            if (participant == null) {
                participant = this;
            }
            participant.setUUID(uuid);
            Long remoteId = jsonObject.getLong("id");
            participant.setRemoteId(remoteId);
            Long projectId = jsonObject.getLong("project_id");
            participant.setProjectId(projectId);
            Long participantTypeId = jsonObject.getLong("participant_type_id");
            ParticipantType participantType = ParticipantType.findByRemoteId(participantTypeId);
            if (participantType != null) {
                participant.setParticipantType(participantType);
            }

            if (jsonObject.isNull("deleted_at")) {
                participant.setChanged(false);
                participant.save();
            } else {
                if (BuildConfig.DEBUG) Log.i(TAG, "deleted participant: " + jsonObject.toString());
                Participant p = Participant.findByUUID(uuid);
                if (p != null) {
                    List<ParticipantProperty> properties = ParticipantProperty
                            .getAllByParticipant(p);
                    List<Relationship> relationships = Relationship.getAllByParticipant(p);
                    for (ParticipantProperty property : properties) {
                        property.delete();
                    }
                    for (Relationship relationship : relationships) {
                        relationship.delete();
                    }
                    p.delete();
                }
            }
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error parsing object json", je);
        }

    }

    public static Participant findByUUID(String uuid) {
        return new Select().from(Participant.class).where("UUID = ?", uuid).executeSingle();
    }

    @Override
    public boolean isChanged() {
        return mChanged;
    }

    @Override
    public boolean belongsToCurrentProject() {
        return AdminSettings.getInstance().getProjectId() != null &&
                mProjectId.equals(AdminSettings.getInstance().getProjectId());
    }

    public void setChanged(boolean changed) {
        mChanged = changed;
    }

    public String getMetadata() throws JSONException {
        JSONObject jsonObject = new JSONObject();

        jsonObject.put("participant_uuid", getUUID());
        jsonObject.put("participant_type", getParticipantType().getLabel());

        for (ParticipantProperty participantProperty : getMetadataParticipantProperties(this)) {
            jsonObject.put(participantProperty.getProperty().getLabel(), participantProperty
                    .getValue());
        }

        // Add metadata for relationships
        for (Relationship relationship : getRelationships()) {
            jsonObject.put(relationship.getRelationshipType().getLabel(), relationship
                    .getParticipantRelated().getUUID());
            for (ParticipantProperty participantProperty : getMetadataParticipantProperties
                    (relationship.getParticipantRelated())) {
                jsonObject.put(relationship.getRelationshipType().getLabel() + " - " +
                        participantProperty.getProperty().getLabel(), participantProperty
                        .getValue());
            }
        }

        //Add Survey label
        for (Property property : this.getProperties()) {
            if (property.getUseAsLabel() && this.hasParticipantProperty(property)) {
                jsonObject.put("survey_label", this.getParticipantType().getLabel() + " " + this
                        .getParticipantProperty(property).getValue());
            }
        }

        return jsonObject.toString();
    }

    private List<ParticipantProperty> getMetadataParticipantProperties(Participant participant) {
        List<ParticipantProperty> participantProperties = new ArrayList<ParticipantProperty>();

        for (Property property : participant.getProperties()) {
            if (property.isIncludedInMetadata()) {
                if (participant.hasParticipantProperty(property)) {
                    participantProperties.add(participant.getParticipantProperty(property));
                }
            }
        }

        return participantProperties;
    }
}