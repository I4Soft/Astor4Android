package com.example.root.bugapp2;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.rule.ActivityTestRule;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 *
 *
 */



@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Rule
    public ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void useAppContext() throws Exception {
        onView(withId(R.id.x)).perform(closeSoftKeyboard());
        onView(withId(R.id.x)).perform(typeText("5"));
        onView(withId(R.id.y)).perform(closeSoftKeyboard());
        onView(withId(R.id.y)).perform(typeText("5"));
        onView(withId(R.id.buttonMultiplicar)).perform(closeSoftKeyboard());
        onView(withId(R.id.buttonMultiplicar)).perform(click());
        onView(withId(R.id.result)).perform(closeSoftKeyboard());
        onView(withId(R.id.result)).check(matches(withText("25.0")));
    }

}
