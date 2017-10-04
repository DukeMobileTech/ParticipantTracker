package org.adaptlab.chpir.android.activerecordcloudsync;

import org.json.JSONObject;

public abstract class SendReceiveModel extends SendModel {
    public abstract void createObjectFromJSON(JSONObject jsonObject);
    public abstract boolean isChanged();
    public abstract boolean belongsToCurrentProject();
    public abstract JSONObject asJsonObject();
}