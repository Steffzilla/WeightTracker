package de.steffzilla.weighttracker.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.text.InputType;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import de.steffzilla.weighttracker.MainActivity;
import de.steffzilla.weighttracker.R;

/**
 * Verifies that the weight field accepts the locale decimal separator.
 * With a German default locale the separator is ',', which the default
 * {@code numberDecimal} key listener would otherwise filter out
 * (see AddEditWeightBottomSheet's locale-aware DigitsKeyListener).
 */
@RunWith(AndroidJUnit4.class)
public class AddEditWeightInputTest {

    private Locale originalLocale;

    @Before
    public void setGermanLocale() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(originalLocale);
    }

    @Test
    public void weightField_acceptsCommaAsDecimalSeparator() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());

            onView(withId(R.id.editTextWeight)).perform(typeText("80,1"));
            closeSoftKeyboard();

            // If the comma were filtered out the field would read "801".
            onView(withId(R.id.editTextWeight)).check(matches(withText("80,1")));
        }
    }

    @Test
    public void weightField_usesDecimalNumberKeyboard() {
        try (ActivityScenario<MainActivity> scenario =
                     ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.fabAdd)).perform(click());

            // The locale-aware key listener must not drag in the full text keyboard:
            // the field should request the decimal number IME.
            onView(withId(R.id.editTextWeight)).check(matches(withInputType(
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)));
        }
    }

    private static Matcher<View> withInputType(int expected) {
        return new TypeSafeMatcher<>() {
            @Override
            protected boolean matchesSafely(View view) {
                return view instanceof TextView
                        && ((TextView) view).getInputType() == expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with inputType " + expected);
            }
        };
    }
}
