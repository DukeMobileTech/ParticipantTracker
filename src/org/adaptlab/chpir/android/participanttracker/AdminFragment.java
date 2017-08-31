package org.adaptlab.chpir.android.participanttracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.adaptlab.chpir.android.activerecordcloudsync.ActiveRecordCloudSync;
import org.adaptlab.chpir.android.participanttracker.models.AdminSettings;
import org.adaptlab.chpir.android.participanttracker.tasks.ApkUpdateTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.adaptlab.chpir.android.participanttracker.AppUtil.WRITE_EXTERNAL_STORAGE_CODE;

public class AdminFragment extends Fragment {
    private EditText mDeviceIdentifierEditText;
    private EditText mDeviceLabelEditText;
    private EditText mApiEndPointEditText;
    private EditText mProjectIdEditText;
    private EditText mApiVersionEditText;
    private EditText mApiKeyEditText;
    private ArrayList<EditText> mTransformableFields;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(R.string.admin_settings);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_settings, parent, false);
        mDeviceIdentifierEditText = (EditText) view.findViewById(R.id.device_identifier_edit_text);
        mDeviceIdentifierEditText.setText(getAdminSettingsInstanceDeviceId());

        mDeviceLabelEditText = (EditText) view.findViewById(R.id.device_label_edit_text);
        mDeviceLabelEditText.setText(AdminSettings.getInstance().getDeviceLabel());

        mApiEndPointEditText = (EditText) view.findViewById(R.id.api_endpoint_edit_text);
        mApiEndPointEditText.setText(getAdminSettingsInstanceApiDomainName());
        if (!TextUtils.isEmpty(getAdminSettingsInstanceApiDomainName())) setOnClickListener(mApiEndPointEditText);

        mApiVersionEditText = (EditText) view.findViewById(R.id.api_version_text);
        mApiVersionEditText.setText(getAdminSettingsInstanceApiVersion());
        if (!TextUtils.isEmpty(getAdminSettingsInstanceApiVersion())) setOnClickListener(mApiVersionEditText);

        mApiKeyEditText = (EditText) view.findViewById(R.id.api_key_text);
        mApiKeyEditText.setText(getAdminSettingsInstanceApiKey());
        if (!TextUtils.isEmpty(getAdminSettingsInstanceApiKey())) setOnClickListener(mApiKeyEditText);

        mProjectIdEditText = (EditText) view.findViewById(R.id.project_id_text);
        mProjectIdEditText.setText(getAdminSettingsInstanceProjectId());
        if (!TextUtils.isEmpty(getAdminSettingsInstanceProjectId())) setOnClickListener(mProjectIdEditText);

        mTransformableFields = new ArrayList<>(Arrays.asList(mApiEndPointEditText, mApiVersionEditText, mProjectIdEditText, mApiKeyEditText, mProjectIdEditText));

        final TextView lastUpdateTextView = (TextView) view.findViewById(R.id.last_update_label);
        lastUpdateTextView.setText(String.format(Locale.getDefault(), "%s%s%s", getString(R.string.last_update), " ", getLastSyncTime()));

        Button resetLastSyncTime = (Button) view.findViewById(R.id.reset_last_sync_time_button);
        resetLastSyncTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdminSettings.getInstance().setLastSyncTime(null);
                lastUpdateTextView.setText(String.format(Locale.getDefault(), "%s%s%s",
                        getString(R.string.last_update), " ", getLastSyncTime()));
            }
        });

        TextView versionCodeTextView = (TextView) view.findViewById(R.id.version_code_label);
        versionCodeTextView.setText(String.format(Locale.getDefault(), "%s%s%d",
                getString(R.string.version_code), " ", AppUtil.getVersionCode(getActivity())));

        TextView versionNameTextView = (TextView) view.findViewById(R.id.version_name_label);
        versionNameTextView.setText(String.format(Locale.getDefault(), "%s%s%s",
                getString(R.string.version_name), " ", AppUtil.getVersionName(getActivity())));

        Button updatesCheckButton = (Button) view.findViewById(R.id.updates_check_button);
        updatesCheckButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (AppUtil.isPermissionNeeded(getActivity())) {
                    ActivityCompat.requestPermissions(getActivity(),  new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_CODE);
                } else {
                    new ApkUpdateTask(getActivity()).execute();
                }
            }
        });

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.admin_setting_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_admin_settings_button:
                saveAdminSettings();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveAdminSettings() {
        AdminSettings.getInstance().setDeviceIdentifier(mDeviceIdentifierEditText.getText().toString());
        AdminSettings.getInstance().setApiUrl(setApiUrl(mApiEndPointEditText.getText().toString()));
        AdminSettings.getInstance().setApiVersion(mApiVersionEditText.getText().toString());
        if (!TextUtils.isEmpty(mProjectIdEditText.getText().toString())) {
            AdminSettings.getInstance().setProjectId(Long.parseLong(mProjectIdEditText.getText().toString()));
        }
        AdminSettings.getInstance().setApiKey(mApiKeyEditText.getText().toString());
        AdminSettings.getInstance().setDeviceLabel(mDeviceLabelEditText.getText().toString());
        ActiveRecordCloudSync.setAccessToken(getAdminSettingsInstanceApiKey());
        ActiveRecordCloudSync.setEndPoint(getAdminSettingsInstanceApiUrl());
        getActivity().finish();
    }

    private String setApiUrl(String string) {
        char lastChar = string.charAt(string.length() - 1);
        if (lastChar != '/') string = string + "/";
        return string;
    }

    private Date getLastSyncTime() {
        Calendar calendar = Calendar.getInstance();
        if (AdminSettings.getInstance().getLastSyncTime().equals("")) {
            calendar.setTimeInMillis(0);
        } else {
            calendar.setTimeInMillis(Long.parseLong(AdminSettings.getInstance().getLastSyncTime()));
        }
        return calendar.getTime();
    }

    public String getAdminSettingsInstanceDeviceId() {
        return AdminSettings.getInstance().getDeviceIdentifier();
    }

    public String getAdminSettingsInstanceApiDomainName() {
        return AdminSettings.getInstance().getApiUrl();
    }

    public String getAdminSettingsInstanceApiVersion() {
        return AdminSettings.getInstance().getApiVersion();
    }

    public String getAdminSettingsInstanceApiKey() {
        return AdminSettings.getInstance().getApiKey();
    }

    public String getAdminSettingsInstanceProjectId() {
        if (AdminSettings.getInstance().getProjectId() == null) return "";
        return Long.toString(AdminSettings.getInstance().getProjectId());
    }

    private String getAdminSettingsInstanceApiUrl() {
        String domainName = AdminSettings.getInstance().getApiUrl();
        return domainName + "api/" + AdminSettings.getInstance().getApiVersion() + "/" +
                "projects/" + AdminSettings.getInstance().getProjectId() + "/";
    }

    private void setOnClickListener(final EditText editText) {
        editText.setTransformationMethod(new PasswordTransformationMethod());
        editText.setFocusable(false);
        editText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayPasswordPrompt();
            }
        });
    }

    private void displayPasswordPrompt() {
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.password_title)
                .setMessage(R.string.password_message)
                .setView(input)
                .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int button) {
                        if (AppUtil.checkAdminPassword(input.getText().toString())) {
                            for (EditText editText : mTransformableFields) {
                                editText.setTransformationMethod(null);
                                editText.setFocusableInTouchMode(true);
                                editText.setClickable(false);
                            }
                        } else {
                            Toast.makeText(getActivity(), R.string.incorrect_password, Toast
                                    .LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int button) {
            }
        }).show();
    }

    /*
    Called from the parent activity
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new ApkUpdateTask(getActivity()).execute();
                }
            }
        }
    }

}
