package org.adaptlab.chpir.android.activerecordcloudsync;

import android.util.Log;

import com.activeandroid.query.Select;

import org.adaptlab.chpir.android.participanttracker.BuildConfig;
import org.adaptlab.chpir.android.participanttracker.models.AdminSettings;
import org.apache.commons.codec.CharEncoding;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HttpPushr {
    private static final String TAG = "HttpPushr";
    private static final int TIMEOUT = 100000;
    private Class<? extends SendModel> mSendTableClass;
    private String mRemoteTableName;

    public HttpPushr(String remoteTableName, Class<? extends SendModel> sendTableClass) {
        mSendTableClass = sendTableClass;
        mRemoteTableName = remoteTableName;
    }

    public void push() {
        if (ActiveRecordCloudSync.getEndPoint() == null) {
            if (BuildConfig.DEBUG) Log.i(TAG, "ActiveRecordCloudSync end point is not set!");
            return;
        }

        List<? extends SendReceiveModel> allElements = getElements();

        try {
            if (isPersistent()) {
                List<SendReceiveModel> postElements = new ArrayList<>();
                List<SendReceiveModel> putElements = new ArrayList<>();
                for (SendReceiveModel element : allElements) {
                    if ((element.isChanged() && element.belongsToCurrentProject()) || (!element.isSent() && element.readyToSend())) {
                        if (element.getRemoteId() == null) {
                            postElements.add(element);
                        } else {
                            putElements.add(element);
                        }
                    }
                }
                sendJsonData("POST", postElements);
                sendJsonData("PUT", putElements);
            } else {
                SendModel element = mSendTableClass.newInstance();
                if (!element.isSent() && element.readyToSend()) {
                    sendData(element);
                }
            }
        } catch (InstantiationException ie) {
            if (BuildConfig.DEBUG) Log.e(TAG, "InstantiationException: " + ie);
        } catch (IllegalAccessException ie) {
            if (BuildConfig.DEBUG) Log.e(TAG, "IllegalAccessException: " + ie);
        }
    }

    private List<? extends SendReceiveModel> getElements() {
        return new Select().from(mSendTableClass).orderBy("Id ASC").execute();
    }

    private boolean isPersistent() throws InstantiationException, IllegalAccessException {
        SendModel sendModel = mSendTableClass.newInstance();
        return sendModel.isPersistent();
    }

    // TODO: 10/4/17 Separate out roster elements
    private void sendJsonData(String method, List<SendReceiveModel> jsonData) {
        if (jsonData.size() == 0) return;
        HttpURLConnection connection = null;
        String endPoint = "";
        if (method.equals("POST")) {
            endPoint = ActiveRecordCloudSync.getEndPoint() + mRemoteTableName + "/batch_create" + ActiveRecordCloudSync.getParams();
        } else if (method.equals("PUT")) {
            endPoint = ActiveRecordCloudSync.getEndPoint() + mRemoteTableName + "/batch_update" + ActiveRecordCloudSync.getParams();
        }
        if (endPoint.equals("")) return;
        if (BuildConfig.DEBUG) Log.i(TAG, "Batch EndPoint: " + endPoint);
        JSONArray json = new JSONArray();
        for (SendReceiveModel element : jsonData) {
            json.put(element.asJsonObject());
        }
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(mRemoteTableName, json);
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "JSON exception", je);
        }
        try {
            connection = (HttpURLConnection) new URL(endPoint).openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setDoOutput(true);

            byte[] outputInBytes = jsonObject.toString().getBytes(CharEncoding.UTF_8);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(outputInBytes);
            outputStream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                if (BuildConfig.DEBUG) Log.i(TAG, "Received OK HTTP code for data sent to " + mRemoteTableName);
                for (SendReceiveModel element : jsonData) {
                    element.setAsSent();
                }
            } else {
                if (BuildConfig.DEBUG) Log.e(TAG, "Received BAD HTTP code " + responseCode + " for data sent to " + mRemoteTableName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

    }

    private void sendData(SendModel element) {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000); // Timeout limit

        HttpResponse response;

        try {
            StringEntity se = new StringEntity(element.toJSON().toString(), CharEncoding.UTF_8);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            if (element.getRemoteId() != null) {
                HttpPut put = new HttpPut(ActiveRecordCloudSync.getEndPoint() + mRemoteTableName + '/' + element.getRemoteId().toString() + '/' + ActiveRecordCloudSync.getParams());
                put.setEntity(se);
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "Sending put request: " + element.toJSON().toString());
                response = client.execute(put);
            } else {
                HttpPost post = new HttpPost(ActiveRecordCloudSync.getEndPoint() + mRemoteTableName + ActiveRecordCloudSync.getParams());
                post.setEntity(se);
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "Sending post request: " + element.toJSON().toString());
                response = client.execute(post);
            }
                /* Checking for successful response */
            if (response.getStatusLine().getStatusCode() >= 200 && response.getStatusLine().getStatusCode() < 300) {
                if (BuildConfig.DEBUG)
                    Log.i(TAG, "Received OK HTTP status for " + element.toJSON());
                InputStream in = response.getEntity().getContent();
                element.setAsSent();
                AdminSettings.getInstance().setLastUpdateTime(new Date().toString());
            } else {
                if (BuildConfig.DEBUG)
                    Log.e(TAG, "Received BAD HTTP status code " + response.getStatusLine().getStatusCode() + " for " + element.toJSON());
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Cannot establish connection", e);
        }
    }
}