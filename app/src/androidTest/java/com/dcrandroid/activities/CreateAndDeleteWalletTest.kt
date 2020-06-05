/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import com.dcrandroid.R
import org.hamcrest.Matchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// This tests creating and deleting of wallets
// and should be run on a device without a wallet.
@LargeTest
@RunWith(AndroidJUnit4ClassRunner::class)
class CreateAndDeleteWalletTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(SplashScreenActivity::class.java)

    private val walletPassword = "espresso test"
    private val walletPassCode = "5552478"

    @Test
    fun createAndDeleteWalletTest() {

        // Splash screen delay, wait for create button to show up
        waitForWithId(R.id.ll_create_wallet)

        val btnCreateNewWallet = onView(
                allOf(withId(R.id.ll_create_wallet), isDisplayed()))
        btnCreateNewWallet.perform(click())

        // Wait for password/pin dialog to show up. To achieve this
        // we'll block the code till R.id.ed_pass returns
        // true for visibility test.
        waitForWithId(R.id.ed_pass)
        waitFor(actionDelay)

        // Enter password
        val passInput = onView(allOf(withId(R.id.password_input_et), isDisplayed(), childHasParentId(R.id.ed_pass)))
        passInput.perform(typeText(walletPassword), closeSoftKeyboard())

        val confirmPassInput = onView(allOf(withId(R.id.password_input_et),
                childHasParentId(R.id.ed_confirm_pass), isDisplayed()))

        confirmPassInput.perform(typeText(walletPassword), closeSoftKeyboard())

        waitFor(actionDelay)

        val revealConfirmPass = onView(
                allOf(withId(R.id.iv_conceal_reveal), childHasParentId(R.id.ed_confirm_pass),
                        isDisplayed()))
        revealConfirmPass.perform(click())

        waitFor(actionDelay)

        val btnCreateWallet = onView(allOf(withId(R.id.btn_create), isDisplayed())).inRoot(isDialog())
        btnCreateWallet.perform(click()) // Proceeds to home activity

        waitForWithId(R.id.recycler_view_tabs)
        navigateToDeleteWallet()

        // Wait for password input dialog to appear
        waitForWithId(R.id.password_input_et)
        // Proceed to enter wallet passphrase

        // Enter password to confirm delete
        onView(allOf(withId(R.id.password_input_et), childHasParentId(R.id.password_input)))
                .perform(typeText(walletPassword), closeSoftKeyboard())

        waitFor(actionDelay)

        // Tap confirm button to delete wallet
        val dialogConfirmBtn = onView(withId(R.id.btn_confirm)).inRoot(isDialog())
        dialogConfirmBtn.perform(click())


        // App will delete wallet and go back to splash screen
        waitFor(actionDelay)
        waitForWithId(R.id.ll_create_wallet)

        onView(allOf(withId(R.id.ll_create_wallet), isDisplayed())).perform(click())

        waitForWithId(R.id.ed_pass)
        waitFor(actionDelay)

        // Switch to pin tab
        onView(allOf(childAtPosition(childAtPosition(withId(R.id.tab_layout), 0), 1), isDisplayed())).perform(click())

        waitForWithId(R.id.pin_view)

        val pinView = onView(withId(R.id.pin_view))
        pinView.perform(typeText(walletPassCode))
        waitFor(actionDelay)

        btnCreateWallet.perform(click()) // can be reused since it matches the filter
        waitFor(actionDelay)

        pinView.perform(typeText(walletPassCode)) // confirm pin
        waitFor(actionDelay)

        btnCreateWallet.perform(click()) // Proceeds to home activity

        waitForWithId(R.id.recycler_view_tabs)
        navigateToDeleteWallet()

        // Proceed to enter wallet enter pin
        pinView.perform(typeText(walletPassCode))
        waitFor(actionDelay)

        dialogConfirmBtn.perform(click())

        waitForWithId(R.id.ll_create_wallet) // wait for splash screen to appear
    }
}

