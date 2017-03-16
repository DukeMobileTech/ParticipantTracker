package org.adaptlab.chpir.android.participanttracker.tasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.activerecordcloudsync.NetworkNotificationUtils;
import org.adaptlab.chpir.android.participanttracker.AppUtil;
import org.adaptlab.chpir.android.participanttracker.BuildConfig;
import org.adaptlab.chpir.android.participanttracker.R;
import org.adaptlab.chpir.android.participanttracker.models.AdminSettings;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class ApkUpdateTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "ApkUpdateTask";
    private Context mContext;
    private int mApkId;
    private Integer mLatestVersion;
    private String mFileName;

    public ApkUpdateTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (NetworkNotificationUtils.checkForNetworkErrors(mContext)) {
            checkLatestApk();
        }
        return null;
    }

    private void checkLatestApk() {
        ActiveRecordCloudSync.setAccessToken(AdminSettings.getInstance().getApiKey());
        ActiveRecordCloudSync.setVersionCode(AppUtil.getVersionCode(mContext));
        String url = AppUtil.getAdminSettingsInstanceApiUrl() + "android_updates" +
                ActiveRecordCloudSync.getParams();
        try {
            String jsonString = getUrl(url);
            if (!jsonString.equals("null")) {
                JSONObject obj = new JSONObject(jsonString);
                mLatestVersion = obj.getInt("version");
                mApkId = obj.getInt("id");
                mFileName = UUID.randomUUID().toString() + ".apk";
            }
        } catch (ConnectException cre) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Connection was refused", cre);
        } catch (IOException ioe) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to fetch items", ioe);
        } catch (NullPointerException npe) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Url is null", npe);
        } catch (JSONException je) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Failed to parse items", je);
        }
    }

    @Override
    protected void onPostExecute(Void param) {
        if (mLatestVersion == null || mLatestVersion <= AppUtil.getVersionCode(mContext)) {
            Toast.makeText(mContext, R.string.up_to_date, Toast.LENGTH_LONG).show();
        } else {
            new AlertDialog.Builder(mContext)
                    .setTitle(R.string.new_apk_title)
                    .setMessage(R.string.new_apk_message)
                    .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int button) {
                            new DownloadApkTask().execute();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {}
                    }).show();
        }
    }

    private String getUrl(String urlSpec) throws IOException {
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

    private class DownloadApkTask extends AsyncTask<Void, Void, Void> {
        private File mFile;

        @Override
        protected Void doInBackground(Void... params) {
            if (NetworkNotificationUtils.checkForNetworkErrors(mContext)) {
                downloadLatestApk();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(mFile), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }

        private void downloadLatestApk() {
            String url = AppUtil.getAdminSettingsInstanceApiUrl() + "android_updates/" + mApkId +
                    "/" + ActiveRecordCloudSync.getParams();
            File path = Environment.getExternalStoragePublicDirectory(Environment
                    .DIRECTORY_DOWNLOADS);
            mFile = new File(path, mFileName);
            FileOutputStream fileWriter = null;
            try {
                byte[] imageBytes = getUrlBytes(url);
                fileWriter = new FileOutputStream(mFile);
                fileWriter.write(imageBytes);
                if (BuildConfig.DEBUG) Log.i(TAG, "APK saved in " + mFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileWriter != null)
                        fileWriter.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}