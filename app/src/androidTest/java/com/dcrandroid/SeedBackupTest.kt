/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid


import android.view.View
import android.view.View.FIND_VIEWS_WITH_TEXT
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.dcrandroid.activities.SplashScreenActivity
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class SeedBackupTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SplashScreenActivity::class.java)

    private val walletPassword = "espresso test"

    @Test
    fun seedBackupTest() {

        // Splash screen delay, wait for create button to show up
        waitForWithId(R.id.ll_create_wallet)

        onView(allOf(withId(R.id.ll_create_wallet), isDisplayed())).perform(click())

        // Wait for password/pin dialog to show up. To achieve this
        // we'll block the code till R.id.ed_pass returns
        // true for visibility test.
        waitForWithId(R.id.ed_pass)
        waitFor(actionDelay)

        // Enter password
        val passInput = onView(allOf(withId(R.id.password_input_et), isDisplayed(), childHasParentId(R.id.ed_pass)))
        passInput.perform(ViewActions.typeText(walletPassword), ViewActions.closeSoftKeyboard())

        val confirmPassInput = onView(allOf(withId(R.id.password_input_et),
                childHasParentId(R.id.ed_confirm_pass), isDisplayed()))

        confirmPassInput.perform(ViewActions.typeText(walletPassword), ViewActions.closeSoftKeyboard())

        // Proceeds to home activity
        onView(allOf(withId(R.id.btn_create), isDisplayed())).inRoot(RootMatchers.isDialog()).perform(click())

        waitForWithId(R.id.recycler_view_tabs)
        waitFor(actionDelay)

        // If wifi is enabled, the app starts syncing else it will show a dialog, requesting
        // for the user's permission to sync with mobile data
        try {
            onView(allOf(withId(R.id.syncing_cancel_layout), isDisplayed())).perform(click())

            // sync might take a while to cancel so wait until the "Not Synced" text is visible
            waitForWithId(R.id.tv_sync_state)
        } catch (e: Exception) {
            e.printStackTrace()
            // cancel sync button was not visible so we should close the dialog
            onView(allOf(withId(R.id.btn_negative), isDisplayed())).inRoot(RootMatchers.isDialog()).perform(click())
        }

        waitFor(actionDelay)

        val navigationTabs = onView(withId(R.id.recycler_view_tabs))
        navigationTabs.perform(actionOnItemAtPosition<ViewHolder>(2, click())) // switch to wallets page

        waitFor(actionDelay)

        onView(allOf(withId(R.id.backup_warning), isDisplayed())).perform(click())

        onView(withId(R.id.seed_check_1)).perform(scrollTo(), click())
        waitFor(actionDelay)

        onView(withId(R.id.seed_check_2)).perform(scrollTo(), click())
        waitFor(actionDelay)

        onView(withId(R.id.seed_check_3)).perform(scrollTo(), click())
        waitFor(actionDelay)

        onView(withId(R.id.seed_check_4)).perform(scrollTo(), click())
        waitFor(actionDelay)

        onView(withId(R.id.seed_check_5)).perform(scrollTo(), click())
        waitFor(actionDelay)

        onView(withId(R.id.btn_verify)).perform(click())

        typePassword()

        waitForWithId(R.id.recycler_view_seeds)

        val seeds = ArrayList<String>()
        for (i in 0..32) {
            val parentMatcher = childAtPosition(withId(R.id.recycler_view_seeds), i)
            val seed = onView(allOf(withId(R.id.seed), childHasParentMatcher(parentMatcher)))
            seeds.add(getText(seed))
        }

        onView(withId(R.id.step_2)).perform(click())
        waitForWithId(R.id.btn_verify)

        for (i in 1..33) {
            onView(withId(R.id.recycler_view_seeds))
                    .perform(actionOnItemAtPosition<ViewHolder>(i, tapSeed(seeds[i - 1])))
            waitFor(500L)
        }

        onView(withId(R.id.btn_verify)).perform(click())

        typePassword()

        waitForWithId(R.id.btn_back_to_wallet)
        waitFor(actionDelay)

        onView(withId(R.id.btn_back_to_wallet)).perform(click())
        waitFor(actionDelay)

        waitFor(actionDelay)

        val walletOptionsButton = onView(allOf(withId(R.id.iv_more),
                isImmediateChild(withId(R.id.container)), isDisplayed()))
        walletOptionsButton.perform(click())

        waitForWithId(R.id.popup_rv) // wait for popup to show up
        waitFor(actionDelay)

        onView(withId(R.id.popup_rv))
                .perform(actionOnItemAtPosition<ViewHolder>(5, click())) // Wallet Settings

        waitForWithId(R.id.wallet_settings_scroll_view) // wait for wallet settings activity
        waitFor(actionDelay)

        // scroll to delete button(smaller screens) and perform click
        onView(withId(R.id.remove_wallet)).perform(scrollTo(), click())

        waitForWithId(R.id.btn_positive)
        waitFor(actionDelay)

        // Confirm delete wallet warning and proceed
        onView(withId(R.id.btn_positive)).perform(click())
        waitFor(actionDelay)

        typePassword()

        waitForWithId(R.id.ll_create_wallet)
    }

    private fun typePassword() {
        // Wait for password input dialog to appear
        waitForWithId(R.id.password_input_et)
        waitFor(actionDelay)

        // Proceed to enter wallet passphrase
        onView(allOf(withId(R.id.password_input_et), childHasParentId(R.id.password_input)))
                .perform(ViewActions.typeText(walletPassword), ViewActions.closeSoftKeyboard())

        waitFor(actionDelay)

        onView(withId(R.id.btn_confirm)).inRoot(RootMatchers.isDialog()).perform(click())
    }


    private fun getText(matcher: ViewInteraction): String {
        var text = String()
        matcher.perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "Text of the view"
            }

            override fun perform(uiController: UiController, view: View) {
                val tv = view as TextView
                text = tv.text.toString()
            }
        })

        return text
    }


    private fun tapSeed(seed: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View>? {
                return null
            }

            override fun getDescription(): String {
                return ""
            }

            override fun perform(uiController: UiController, view: View) {
                val views = ArrayList<View>()
                view.findViewsWithText(views, seed, FIND_VIEWS_WITH_TEXT)
                views.onEach { it.performClick() } // expected to find just one
            }
        }
    }
}
