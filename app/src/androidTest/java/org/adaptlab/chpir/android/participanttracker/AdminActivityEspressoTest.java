package org.adaptlab.chpir.android.participanttracker;


import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class AdminActivityEspressoTest {

    @Rule
    public final ActivityTestRule<AdminActivity> admin = new ActivityTestRule<>(AdminActivity.class);

    @Test
    public void shouldHaveSyncSettingsLabel() {
        onView(withText("Sync Settings")).check(ViewAssertions.matches(isDisplayed()));
    }
}