package org.adaptlab.chpir.android.participanttracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.crashlytics.android.Crashlytics;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.participanttracker.models.AdminSettings;
import org.adaptlab.chpir.android.participanttracker.models.DeviceSyncEntry;
import org.adaptlab.chpir.android.participanttracker.models.Participant;
import org.adaptlab.chpir.android.participanttracker.models.ParticipantProperty;
import org.adaptlab.chpir.android.participanttracker.models.ParticipantType;
import org.adaptlab.chpir.android.participanttracker.models.Property;
import org.adaptlab.chpir.android.participanttracker.models.Relationship;
import org.adaptlab.chpir.android.participanttracker.models.RelationshipType;
import org.adaptlab.chpir.android.vendor.BCrypt;

import java.util.UUID;

import io.fabric.sdk.android.Fabric;

public class AppUtil {
    private final static boolean REQUIRE_SECURITY_CHECKS = !BuildConfig.DEBUG;
    private static final String TAG = "AppUtil";
    private static final boolean SEED_DB = false;
    public static final int WRITE_EXTERNAL_STORAGE_CODE = 1;
    public static String ADMIN_PASSWORD_HASH;
    public static String ACCESS_TOKEN;
    private static Context mContext;

    public static final void appInit(Context context) {
        if (AppUtil.REQUIRE_SECURITY_CHECKS) {
            if (!AppUtil.hasPassedDeviceSecurityChecks(context)) {
                return;
            }
        }

        mContext = context;

        ADMIN_PASSWORD_HASH = context.getResources().getString(R.string.admin_password_hash);
        ACCESS_TOKEN = AdminSettings.getInstance().getApiKey();

        setDeviceProperties();

        if (!BuildConfig.DEBUG) {
            Fabric.with(mContext, new Crashlytics());
            Crashlytics.setUserIdentifier(AdminSettings.getInstance().getDeviceIdentifier());
            Crashlytics.setString("device label", AdminSettings.getInstance().getDeviceLabel());
        }

        ActiveRecordCloudSync.setAccessToken(ACCESS_TOKEN);
        ActiveRecordCloudSync.setVersionCode(AppUtil.getVersionCode(context));
        ActiveRecordCloudSync.setEndPoint(getAdminSettingsInstanceApiUrl());
        addDataTables();
        seedDb();
        requestNeededPermissions();
    }

    private static void setDeviceProperties() {
        if (TextUtils.isEmpty(AdminSettings.getInstance().getDeviceIdentifier())) {
            AdminSettings.getInstance().setDeviceIdentifier(UUID.randomUUID().toString());
        }
        if (TextUtils.isEmpty(AdminSettings.getInstance().getDeviceLabel())) {
            AdminSettings.getInstance().setDeviceLabel(getBuildName());
        }
    }

    private static void requestNeededPermissions() {
        if (isPermissionNeeded((Activity) mContext)) {
            ActivityCompat.requestPermissions((Activity) mContext,  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_CODE);
        }
    }

    static boolean isPermissionNeeded(Activity context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED;
    }

    public static String getAdminSettingsInstanceApiUrl() {
        String domainName = AdminSettings.getInstance().getApiUrl();
        return domainName + "api/" + AdminSettings.getInstance().getApiVersion() + "/" +
                "projects/" + AdminSettings.getInstance().getProjectId() + "/";
    }

    private static boolean hasPassedDeviceSecurityChecks(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService
                (Context.DEVICE_POLICY_SERVICE);
        if (devicePolicyManager.getStorageEncryptionStatus() != DevicePolicyManager
                .ENCRYPTION_STATUS_ACTIVE) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.encryption_required_title)
                    .setMessage(R.string.encryption_required_text)
                    .setCancelable(false)
                    .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            int pid = android.os.Process.myPid();
                            android.os.Process.killProcess(pid);
                        }
                    })
                    .show();
            return false;
        }
        return true;
    }

    private static void addDataTables() {
        ActiveRecordCloudSync.addReceiveTable("participant_types", ParticipantType.class);
        ActiveRecordCloudSync.addReceiveTable("properties", Property.class);
        ActiveRecordCloudSync.addReceiveTable("relationship_types", RelationshipType.class);
        ActiveRecordCloudSync.addSendReceiveTable("participants", Participant.class);
        ActiveRecordCloudSync.addSendReceiveTable("participant_properties", ParticipantProperty.class);
        ActiveRecordCloudSync.addSendReceiveTable("relationships", Relationship.class);
        ActiveRecordCloudSync.addSendTable("device_sync_entries", DeviceSyncEntry.class);
    }

    @SuppressLint("UseValueOf")
    public static void seedDb() {
        if (SEED_DB) {
            String[] dummyParticipantTypes = {"Child", "Caregiver", "Center"};
            for (String participantType : dummyParticipantTypes) {
                ActiveAndroid.beginTransaction();
                try {
                    ParticipantType p = new ParticipantType();
                    p.setLabel(participantType);
                    p.save();
                    ActiveAndroid.setTransactionSuccessful();
                } finally {
                    ActiveAndroid.endTransaction();
                }
            }

            for (ParticipantType participantType : ParticipantType.getAll()) {
                Property nameProperty = new Property("Name",
                        Property.PropertyType.STRING, true, participantType, "PARTICIPANT_ID");
                Property ageProperty = new Property("Age",
                        Property.PropertyType.INTEGER, false, participantType, "PARTICIPANT_ID");
                Property dateProperty = new Property("Birthday",
                        Property.PropertyType.DATE, false, participantType, "PARTICIPANT_ID");

                nameProperty.setUseAsLabel(true);

                nameProperty.save();
                ageProperty.save();
                dateProperty.save();

                if (participantType.getLabel().equals("Child")) {
                    Log.i(TAG, "Creating relationship");
                    RelationshipType relationshipType = new RelationshipType(
                            "Caregiver",
                            ParticipantType.findById(new Long(1)),
                            ParticipantType.findById(new Long(2))
                    );
                    relationshipType.save();
                }

                for (int i = 0; i < 10; i++) {
                    Participant participant = new Participant(participantType);
                    participant.save();

                    ParticipantProperty participantProperty = new ParticipantProperty(
                            participant, nameProperty, participantType + " " + i
                    );
                    participantProperty.save();

                    participantProperty = new ParticipantProperty(participant,
                            ageProperty, String.valueOf(i));
                    participantProperty.save();
                }
            }
        }
    }

    public static boolean checkAdminPassword(String password) {
        return BCrypt.checkpw(password, ADMIN_PASSWORD_HASH);
    }

    public static int getVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (NameNotFoundException nnfe) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error finding version code: " + nnfe);
        }
        return -1;
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (NameNotFoundException nnfe) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error finding version code: " + nnfe);
        }
        return "";
    }

    public static Context getContext() {
        return mContext;
    }

    public static String getBuildName() {
        return Build.MODEL;
    }

}
