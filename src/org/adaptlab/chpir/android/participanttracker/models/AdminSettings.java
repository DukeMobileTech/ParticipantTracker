package org.adaptlab.chpir.android.participanttracker.models;

import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import org.adaptlab.chpir.android.participanttracker.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

@Table(name = "AdminSettings")
public class AdminSettings extends Model {
    private static final String TAG = "AdminSettings";
    @Column(name = "DeviceIdentifier")
    private String mDeviceIdentifier;
    @Column(name = "SyncInterval")
    private int mSyncInterval;
    @Column(name = "ApiUrl")
    private String mApiUrl;
    @Column(name = "LastUpdateTime")
    private String mLastUpdateTime;
    @Column(name = "ApiVersion")
    private String mApiVersion;
    @Column(name = "ApiKey")
    private String mApiKey;
    @Column(name = "DeviceLabel")
    private String mDeviceLabel;
    @Column(name = "LastSyncTime")
    private String mLastSyncTime;
    @Column(name = "ProjectId")
    private Long mProjectId;

    public AdminSettings() {
        super();
    }

    public static AdminSettings getInstance() {
        AdminSettings adminSettings = new Select().from(AdminSettings.class).orderBy("Id asc").executeSingle();
        if (adminSettings == null) {
            if (BuildConfig.DEBUG) Log.i(TAG, "Creating new admin settings instance");
            adminSettings = new AdminSettings();
            adminSettings.save();
        }
        return adminSettings;
    }

    public String getDeviceIdentifier() {
        return mDeviceIdentifier;
    }

    public void setDeviceIdentifier(String id) {
        mDeviceIdentifier = id;
        save();
    }

    public int getSyncInterval() {
        return mSyncInterval;
    }

    public void setSyncInterval(int interval) {
        if (BuildConfig.DEBUG) Log.i(TAG, "Setting set interval: " + (interval * 1000 * 60));
        mSyncInterval = interval * 1000 * 60;
        save();
    }

    public int getSyncIntervalInMinutes() {
        return mSyncInterval / (60 * 1000);
    }

    public String getApiUrl() {
        return mApiUrl;
    }

    public void setApiUrl(String apiUrl) {
        if (BuildConfig.DEBUG) Log.i(TAG, "Setting api endpoint: " + apiUrl);
        mApiUrl = apiUrl;
        save();
    }

    public String getLastUpdateTime() {
        return mLastUpdateTime;
    }

    public void setLastUpdateTime(String time) {
        mLastUpdateTime = time;
    }

    public String getApiKey() {
        return mApiKey;
    }

    public void setApiKey(String apiKey) {
        mApiKey = apiKey;
        save();
    }

    public String getApiVersion() {
        return mApiVersion;
    }

    public void setApiVersion(String apiVersion) {
        mApiVersion = apiVersion;
        save();
    }

    public String getDeviceLabel() {
        return mDeviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        mDeviceLabel = deviceLabel;
        save();
    }

    public String getLastSyncTime() {
        String lastSyncTime = "";
        if (mLastSyncTime == null) {
            return lastSyncTime;
        }
        try {
            JSONObject projectLastSyncTime = new JSONObject(mLastSyncTime);
            if (!projectLastSyncTime.isNull(String.valueOf(getProjectId()))) {
                lastSyncTime = projectLastSyncTime.getString(String.valueOf(getProjectId()));
            }
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        return lastSyncTime;
    }

    // Set last sync times based on project Id
    public void setLastSyncTime(String time) {
        JSONObject projectTime = new JSONObject();
        try {
            if (mLastSyncTime == null) {
                projectTime = new JSONObject();
            } else {
                projectTime = new JSONObject(mLastSyncTime);
            }
            projectTime.put(String.valueOf(getProjectId()), time);
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        mLastSyncTime = projectTime.toString();
        save();
    }

    public void setProjectId(Long id) {
        mProjectId = id;
        save();
    }

    public Long getProjectId() {
        return mProjectId;
    }

}