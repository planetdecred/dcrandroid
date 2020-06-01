/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.activities


import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
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
    var mActivityTestRule = ActivityScenarioRule(SplashScreenActivity::class.java)

    private val actionDelay = 1000L
    private val walletPassword = "espresso test"
    private val walletPassCode = "5552478"

    @Test
    fun createAndDeleteWalletTest() {

        // Splash screen delay, wait for create button to show up
        WaitForUIUpdate.waitForWithId(R.id.ll_create_wallet)

        val btnCreateNewWallet = onView(
                allOf(withId(R.id.ll_create_wallet), isDisplayed()))
        btnCreateNewWallet.perform(click())

        // Wait for password/pin dialog to show up. To achieve this
        // we'll block the code till R.id.ed_pass returns
        // true for visibility test.
        WaitForUIUpdate.waitForWithId(R.id.ed_pass)
        WaitForUIUpdate.waitFor(actionDelay)

        // Enter password
        val passInput = onView(allOf(withId(R.id.password_input_et), isDisplayed(), childHasParent(R.id.ed_pass)))
        passInput.perform(typeText(walletPassword), closeSoftKeyboard())

        val confirmPassInput = onView(allOf(withId(R.id.password_input_et),
                childHasParent(R.id.ed_confirm_pass), isDisplayed()))

        confirmPassInput.perform(typeText(walletPassword), closeSoftKeyboard())

        WaitForUIUpdate.waitFor(actionDelay)

        val revealConfirmPass = onView(
                allOf(withId(R.id.iv_conceal_reveal), childHasParent(R.id.ed_confirm_pass),
                        isDisplayed()))
        revealConfirmPass.perform(click())

        WaitForUIUpdate.waitFor(actionDelay)

        val btnCreateWallet = onView(allOf(withId(R.id.btn_create), isDisplayed())).inRoot(isDialog())
        btnCreateWallet.perform(click()) // Proceeds to home activity

        navigateToWalletSettings()

        // Wait for password input dialog to appear
        WaitForUIUpdate.waitForWithId(R.id.password_input_et)
        // Proceed to enter wallet passphrase

        // Enter password to confirm delete
        onView(allOf(withId(R.id.password_input_et), childHasParent(R.id.password_input)))
                .perform(typeText(walletPassword), closeSoftKeyboard())

        WaitForUIUpdate.waitFor(actionDelay)

        // Tap confirm button to delete wallet
        val dialogConfirmBtn = onView(withId(R.id.btn_confirm)).inRoot(isDialog())
        dialogConfirmBtn.perform(click())


        // App will delete wallet and go back to splash screen
        WaitForUIUpdate.waitFor(actionDelay)
        WaitForUIUpdate.waitForWithId(R.id.ll_create_wallet)

        onView(allOf(withId(R.id.ll_create_wallet), isDisplayed())).perform(click())

        WaitForUIUpdate.waitForWithId(R.id.ed_pass)
        WaitForUIUpdate.waitFor(actionDelay)

        // Switch to pin tab
        onView(allOf(childAtPosition(childAtPosition(withId(R.id.tab_layout), 0), 1), isDisplayed())).perform(click())

        WaitForUIUpdate.waitForWithId(R.id.pin_view)

        val pinView = onView(withId(R.id.pin_view))
        pinView.perform(typeText(walletPassCode))
        WaitForUIUpdate.waitFor(actionDelay)

        btnCreateWallet.perform(click()) // can be reused since it matches the filter
        WaitForUIUpdate.waitFor(actionDelay)

        pinView.perform(typeText(walletPassCode)) // confirm pin
        WaitForUIUpdate.waitFor(actionDelay)

        btnCreateWallet.perform(click()) // Proceeds to home activity

        navigateToWalletSettings()

        // Proceed to enter wallet enter pin
        pinView.perform(typeText(walletPassCode))
        WaitForUIUpdate.waitFor(actionDelay)

        dialogConfirmBtn.perform(click())

        WaitForUIUpdate.waitForWithId(R.id.ll_create_wallet) // wait for splash screen to appear
    }

    private fun navigateToWalletSettings() {
        // wait for the tabs to show up
        WaitForUIUpdate.waitForWithId(R.id.recycler_view_tabs)
        WaitForUIUpdate.waitFor(actionDelay)

        // If wifi is enabled, the app starts syncing else it will show a dialog, requesting
        // for the user's permission to sync with mobile data
        try {
            val linearLayout2 = onView(allOf(withId(R.id.syncing_cancel_layout), isDisplayed()))
            linearLayout2.perform(click())

            // sync might take a while to cancel so wait until the "Not Synced" text is visible
            WaitForUIUpdate.waitForWithId(R.id.tv_sync_state)
        } catch (e: Exception) {
            e.printStackTrace()
            // cancel sync button was not visible so we should close the dialog
            val notNowButton = onView(allOf(withId(R.id.btn_negative), isDisplayed())).inRoot(isDialog())
            notNowButton.perform(click())
        }

        WaitForUIUpdate.waitFor(actionDelay)

        val navigationTabs = onView(withId(R.id.recycler_view_tabs))
        navigationTabs.perform(actionOnItemAtPosition<ViewHolder>(2, click())) // switch to wallets page

        WaitForUIUpdate.waitFor(actionDelay)

        val walletOptionsButton = onView(allOf(withId(R.id.iv_more),
                isImmediateChild(withId(R.id.container)), isDisplayed()))
        walletOptionsButton.perform(click())

        WaitForUIUpdate.waitForWithId(R.id.popup_rv) // wait for popup to show up
        WaitForUIUpdate.waitFor(actionDelay)

        onView(withId(R.id.popup_rv))
                .perform(actionOnItemAtPosition<ViewHolder>(5, click())) // Wallet Settings

        WaitForUIUpdate.waitForWithId(R.id.wallet_settings_scroll_view) // wait for wallet settings activity
        WaitForUIUpdate.waitFor(actionDelay)

        // scroll to delete button(smaller screens) and perform click
        onView(withId(R.id.remove_wallet)).perform(scrollTo(), click())

        WaitForUIUpdate.waitFor(actionDelay)

        // Confirm delete wallet warning and proceed
        onView(withId(R.id.btn_positive)).perform(click())
        WaitForUIUpdate.waitFor(actionDelay)
    }
}

