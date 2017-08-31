package org.adaptlab.chpir.android.participanttracker.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.activerecordcloudsync.NetworkNotificationUtils;
import org.adaptlab.chpir.android.participanttracker.R;

public class SyncTablesTask extends AsyncTask<Void, Void, Void> {
    private ProgressDialog mProgressDialog;
    private Context mContext;

    public SyncTablesTask(Context context) {
        mContext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (NetworkNotificationUtils.checkForNetworkErrors(mContext)) {
            ActiveRecordCloudSync.syncTables(mContext);
        }
        return null;
    }

    @Override
    protected void onPreExecute() {
        if (mContext != null && !((Activity) mContext).isFinishing()) {
            mProgressDialog = ProgressDialog.show(
                    mContext,
                    mContext.getString(R.string.participants_loading_header),
                    mContext.getString(R.string.participants_loading_message)
            );
        }
    }

    @Override
    protected void onPostExecute(Void param) {
        new LogoutUserTask(mContext).execute();
        ActiveRecordCloudSync.recordLastSyncTime();
        ((Activity) mContext).setResult(Activity.RESULT_OK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (mProgressDialog != null && mProgressDialog.isShowing() && !((Activity) mContext).isDestroyed()) {
                mProgressDialog.dismiss();
            }
        }
        ((Activity) mContext).finish();
    }
}

class LogoutUserTask extends AsyncTask<Void, Void, Void> {
    private Context lcontext;

    public LogoutUserTask(Context context) {
        lcontext = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (NetworkNotificationUtils.checkForNetworkErrors(lcontext)) {
            ActiveRecordCloudSync.logoutUser();
        }
        return null;
    }
}