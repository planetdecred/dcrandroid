/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.dcrandroid.R
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class RestoreWalletTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SplashScreenActivity::class.java)

    private val seed = "peachy chambermaid upset escapade breakup graduate slingshot recipe blockade Saturday sugar caretaker egghead bookseller frighten holiness scorecard newsletter sweatband celebrate glitter unify drifter enterprise choking designing drumbeat outfielder island supportive puppy inertia stapler"
    private val walletPassword = "espresso test"

    @Test
    fun restoreWalletTest() {

        // Splash screen delay, wait for create button to show up
        waitForWithId(R.id.ll_restore_wallet)
        onView(allOf(withId(R.id.ll_restore_wallet), isDisplayed())).perform(click())

        waitForWithId(R.id.seed_input_list)

        val seedList = seed.split(" ")

        for (i in 0 until 33) {
            val seedRowMatcher = (childAtPosition(withId(R.id.seed_input_list), i))
            val autoCompleteSeed = onView(allOf(withId(R.id.seed_et), childHasParentMatcher(seedRowMatcher)))
            autoCompleteSeed.perform(typeText(seedList[i]), closeSoftKeyboard())

            waitFor(500L)
        }

        val restoreWalletBtn = onView(allOf(withId(R.id.btn_restore), isEnabled()))
        restoreWalletBtn.perform(click())

        waitForWithId(R.id.ed_pass)
        waitFor(actionDelay)

        // Enter password
        val passInput = onView(allOf(withId(R.id.password_input_et), isDisplayed(), childHasParentId(R.id.ed_pass)))
        passInput.perform(typeText(walletPassword), closeSoftKeyboard())

        val confirmPassInput = onView(allOf(withId(R.id.password_input_et),
                childHasParentId(R.id.ed_confirm_pass), isDisplayed()))

        confirmPassInput.perform(typeText(walletPassword), closeSoftKeyboard())

        waitFor(actionDelay)

        onView(allOf(withId(R.id.btn_create), isDisplayed())).inRoot(RootMatchers.isDialog())
                .perform(click()) // restore wallet success activity

        waitForWithId(R.id.tv_get_started)
        waitFor(actionDelay)

        // Proceed to home activity
        onView(allOf(withId(R.id.tv_get_started), isDisplayed())).perform(click())

        navigateToDeleteWallet()

        // Wait for password input dialog to appear
        waitForWithId(R.id.password_input_et)
        // Proceed to enter wallet passphrase

        // Enter password to confirm delete
        onView(allOf(withId(R.id.password_input_et), childHasParentId(R.id.password_input)))
                .perform(typeText(walletPassword), closeSoftKeyboard())

        waitFor(actionDelay)

        // Tap confirm button to delete wallet
        val dialogConfirmBtn = onView(withId(R.id.btn_confirm)).inRoot(RootMatchers.isDialog())
        dialogConfirmBtn.perform(click())


        // App will delete wallet and go back to splash screen
        waitFor(actionDelay)
        waitForWithId(R.id.ll_create_wallet)

    }
}
