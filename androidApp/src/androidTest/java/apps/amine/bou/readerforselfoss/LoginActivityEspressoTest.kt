package bou.amine.apps.readerforselfossv2.android

import android.content.Context
import android.content.Intent
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.pressBack
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import bou.amine.apps.readerforselfossv2.android.HomeActivity
import bou.amine.apps.readerforselfossv2.android.LoginActivity
import bou.amine.apps.readerforselfossv2.android.utils.Config
import com.mikepenz.aboutlibraries.ui.LibsActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityEspressoTest {

    @Rule @JvmField
    val rule = ActivityTestRule(LoginActivity::class.java, true, false)

    private lateinit var context: Context
    private lateinit var url: String
    private lateinit var username: String
    private lateinit var password: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val editor =
                context
                        .getSharedPreferences(Config.settingsName, Context.MODE_PRIVATE)
                        .edit()
        editor.clear()
        editor.commit()


        url = BuildConfig.LOGIN_URL
        username = BuildConfig.LOGIN_USERNAME
        password = BuildConfig.LOGIN_PASSWORD

        Intents.init()
    }

    @Test
    fun menuItems() {

        rule.launchActivity(Intent())

        openActionBarOverflowOrOptionsMenu(context)

        onView(withText(R.string.action_about)).perform(click())

        intended(hasComponent(LibsActivity::class.java.name), times(1))

        onView(isRoot()).perform(pressBack())

        intended(hasComponent(LoginActivity::class.java.name))
    }

    @Test
    fun wrongLoginUrl() {
        rule.launchActivity(Intent())

        onView(withId(R.id.loginProgress))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

        onView(withId(R.id.urlView)).perform(click()).perform(typeText("WRONGURL"))

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.urlView)).check(matches(isHintOrErrorEnabled()))
    }

    // TODO: Add tests for multiple false urls with dialog

    @Test
    fun emptyAuthData() {

        rule.launchActivity(Intent())

        onView(withId(R.id.urlView)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.loginView)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.passwordView)).check(matches(isHintOrErrorEnabled()))

        onView(withId(R.id.loginView)).perform(click()).perform(
                typeText(username),
                closeSoftKeyboard()
        )

        onView(withId(R.id.passwordView)).check(matches(isHintOrErrorEnabled()))

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.passwordView)).check(
                matches(
                        isHintOrErrorEnabled()
                )
        )
    }

    @Test
    fun wrongAuthData() {

        rule.launchActivity(Intent())

        onView(withId(R.id.urlView)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.loginView)).perform(click()).perform(
                typeText(username),
                closeSoftKeyboard()
        )

        onView(withId(R.id.passwordView)).perform(click()).perform(
                typeText("WRONGPASS"),
                closeSoftKeyboard()
        )

        onView(withId(R.id.signInButton)).perform(click())

        onView(withId(R.id.urlView)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.loginView)).check(matches(isHintOrErrorEnabled()))
        onView(withId(R.id.passwordView)).check(matches(isHintOrErrorEnabled()))
    }

    @Test
    fun workingAuth() {

        rule.launchActivity(Intent())

        onView(withId(R.id.urlView)).perform(click()).perform(typeText(url), closeSoftKeyboard())

        onView(withId(R.id.withLogin)).perform(click())

        onView(withId(R.id.loginView)).perform(click()).perform(
                typeText(username),
                closeSoftKeyboard()
        )

        onView(withId(R.id.passwordView)).perform(click()).perform(
                typeText(password),
                closeSoftKeyboard()
        )

        onView(withId(R.id.signInButton)).perform(click())

        Thread.sleep(2000)
        intended(hasComponent(HomeActivity::class.java.name))
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }
}