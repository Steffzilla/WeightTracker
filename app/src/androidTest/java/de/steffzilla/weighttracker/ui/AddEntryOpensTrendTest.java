package de.steffzilla.weighttracker.ui;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.fail;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.steffzilla.weighttracker.MainActivity;
import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.data.AppDatabase;

/**
 * Capturing a weight is almost always followed by looking at the trend, so the app goes
 * there on its own. Only a stored entry may trigger it — a rejected save must leave the
 * user on the list with the error.
 */
@RunWith(AndroidJUnit4.class)
public class AddEntryOpensTrendTest {

    @Before
    public void clearEntries() {
        AppDatabase.getInstance(ApplicationProvider.getApplicationContext())
                .getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM weight_entries");
    }

    @Test
    public void savingANewEntry_opensTheTrendScreen() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            addWeight("80");

            // The range toggle exists only on the trend screen.
            waitForDisplayed(R.id.rangeToggle);
        }
    }

    @Test
    public void rejectedSave_staysOnTheList() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            addWeight("80");
            waitForDisplayed(R.id.rangeToggle);
            androidx.test.espresso.Espresso.pressBack();

            // Same date again: the entry is refused, so nothing should navigate.
            addWeight("81");

            SystemClock.sleep(1000);
            onView(withId(R.id.fabAdd)).check(matches(isDisplayed()));
        }
    }

    private static void addWeight(String weight) {
        onView(withId(R.id.fabAdd)).perform(click());
        onView(withId(R.id.editTextWeight)).perform(typeText(weight));
        closeSoftKeyboard();
        onView(withId(R.id.buttonSave)).perform(click());
    }

    /**
     * Retries until the view is on screen. The entry is written on the ViewModel's
     * executor, which Espresso does not synchronize with, so the navigation that follows
     * it cannot be awaited directly.
     */
    private static void waitForDisplayed(int viewId) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (true) {
            try {
                onView(withId(viewId)).check(matches(isDisplayed()));
                return;
            } catch (Throwable t) {
                if (SystemClock.uptimeMillis() > deadline) {
                    fail("view " + viewId + " never appeared: " + t.getMessage());
                }
                SystemClock.sleep(100);
            }
        }
    }
}
