package de.steffzilla.weighttracker.ui;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.Matchers.allOf;
import static org.junit.Assume.assumeFalse;

import android.os.SystemClock;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import de.steffzilla.weighttracker.MainActivity;
import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.data.AppDatabase;

/**
 * Guards the add/edit sheet against configuration changes (simulated via
 * {@link ActivityScenario#recreate()}): a date picked in the MaterialDatePicker must
 * survive recreation all the way into the saved entry (the view state restores the
 * *displayed* date even when the internal selection has silently reset, so only saving
 * proves the fix), and a picker that was open across the recreation must still deliver
 * its result (the fragment manager restores the dialog but not its listeners).
 *
 * <p>Both tests pick day 1 of the current month, so they are skipped on the 1st, when
 * that day equals today and the assertions could not distinguish a fix from a reset.
 */
@RunWith(AndroidJUnit4.class)
public class AddEditDatePickerRecreateTest {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Before
    public void clearEntries() {
        AppDatabase.getInstance(ApplicationProvider.getApplicationContext())
                .getOpenHelper().getWritableDatabase()
                .execSQL("DELETE FROM weight_entries");
    }

    @Test
    public void pickedDate_survivesRecreate_intoSavedEntry() {
        assumeFalse(LocalDate.now().getDayOfMonth() == 1);
        String firstOfMonth = LocalDate.now().withDayOfMonth(1).format(DISPLAY_FORMAT);

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            onView(withId(R.id.editTextWeight)).perform(typeText("80"));
            closeSoftKeyboard();
            onView(withId(R.id.editTextDate)).perform(click());
            onView(allOf(withText("1"), isCompletelyDisplayed())).perform(click());
            onView(withId(com.google.android.material.R.id.confirm_button)).perform(click());
            onView(withId(R.id.editTextDate)).check(matches(withText(firstOfMonth)));

            scenario.recreate();

            onView(withId(R.id.buttonSave)).perform(click());
            // The entry must land on the picked date, not on today.
            waitForDisplayed(firstOfMonth);
        }
    }

    @Test
    public void datePicker_deliversResultAfterRecreate() {
        assumeFalse(LocalDate.now().getDayOfMonth() == 1);
        String firstOfMonth = LocalDate.now().withDayOfMonth(1).format(DISPLAY_FORMAT);

        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());
            onView(withId(R.id.editTextDate)).perform(click());

            scenario.recreate();

            // The month pager keeps adjacent months in the hierarchy, so "1" exists
            // several times; only the current month's cell is actually on screen.
            onView(allOf(withText("1"), isCompletelyDisplayed())).perform(click());
            onView(withId(com.google.android.material.R.id.confirm_button)).perform(click());
            onView(withId(R.id.editTextDate)).check(matches(withText(firstOfMonth)));
        }
    }

    /**
     * Retries until a view with {@code text} is on screen. Needed after saving because
     * the Room write runs on the ViewModel's executor, which Espresso does not
     * synchronize with.
     */
    private static void waitForDisplayed(String text) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (true) {
            try {
                onView(withText(text)).check(matches(isCompletelyDisplayed()));
                return;
            } catch (Throwable t) {
                if (SystemClock.uptimeMillis() > deadline) {
                    throw t;
                }
                SystemClock.sleep(100);
            }
        }
    }
}
