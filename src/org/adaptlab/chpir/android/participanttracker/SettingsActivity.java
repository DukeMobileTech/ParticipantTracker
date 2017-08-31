package org.adaptlab.chpir.android.participanttracker;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.List;

public class SettingsActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new SettingsFragment();
    }

    /*
    This method is Overriden here (in the activity) and then called on the actual fragments hosted by the activity. Calling it directly on the hosted fragment doesn't trigger it.
    Source: https://issuetracker.google.com/issues/37065121#c5
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

}