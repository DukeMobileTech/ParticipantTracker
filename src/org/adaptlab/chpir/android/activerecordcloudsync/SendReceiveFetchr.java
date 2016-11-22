package org.adaptlab.chpir.android.activerecordcloudsync;

import android.util.Log;

import com.activeandroid.ActiveAndroid;

import org.adaptlab.chpir.android.participanttracker.BuildConfig;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendReceiveFetchr {
    private static final String TAG = "SendReceiveFetchr";
    private Class<? extends SendReceiveModel> mReceiveTableClass;
    private String mRemoteTableName;

    public SendReceiveFetchr(String remoteTableName, Class<? extends SendReceiveModel>
            receiveTableClass) {
        mReceiveTableClass = receiveTableClass;
        mRemoteTableName = remoteTableName;
    }

    public void fetch() {
        if (ActiveRecordCloudSync.getEndPoint() == null) {
            if (BuildConfig.DEBUG) Log.i(TAG, "ActiveRecordCloudSync end point is not set!");
            return;
        }

        try {
            String url = ActiveRecordCloudSync.getEndPoint() + mRemoteTableName
                    + ActiveRecordCloudSync.getParams();
            if (BuildConfig.DEBUG) Log.i(TAG, "Attempting to access " + url);
            String jsonString = getUrl(url);
            if (BuildConfig.DEBUG) Log.i(TAG, "Got JSON String: " + jsonString);
            JSONArray jsonArray = new JSONArray(jsonString);
            if (BuildConfig.DEBUG) Log.i(TAG, "Received json result: " + jsonArray);

            ActiveAndroid.beginTransaction();
            try {
                for (int i = 0; i < jsonArray.length(); i++) {
                    SendReceiveModel tableInstance = mReceiveTableClass.newInstance();
                    tableInstance.createObjectFromJSON(jsonArray.getJSONObject(i));
                }
                ActiveAndroid.setTransactionSuccessful();
            } finally {
                ActiveAndroid.endTransaction();
            }

        } catch (ConnectException cre) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Connection was refused", cre);
        } catch (IOException ioe) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to parse items", je);
        } catch (InstantiationException ie) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to instantiate receive table", ie);
        } catch (IllegalAccessException iae) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to access receive table", iae);
        } catch (NullPointerException npe) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Url is null", npe);
        }
    }

    public String getUrl(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    private byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }

            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }
}