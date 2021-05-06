/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matchers

fun navigateToDeleteWallet() {
    // wait for the tabs to show up
    waitFor(actionDelay)

    // If wifi is enabled, the app starts syncing else it will show a dialog, requesting
    // for the user's permission to sync with mobile data
    try {
        val linearLayout2 = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.syncing_cancel_layout), ViewMatchers.isDisplayed()))
        linearLayout2.perform(ViewActions.click())

        // sync might take a while to cancel so wait until the "Not Synced" text is visible
        waitForWithId(R.id.tv_sync_state)
    } catch (e: Exception) {
        e.printStackTrace()
        // cancel sync button was not visible so we should close the dialog
        val notNowButton = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.btn_negative), ViewMatchers.isDisplayed())).inRoot(RootMatchers.isDialog())
        notNowButton.perform(ViewActions.click())
    }

    waitFor(actionDelay)

    val navigationTabs = Espresso.onView(ViewMatchers.withId(R.id.recycler_view_tabs))
    navigationTabs.perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(2, ViewActions.click())) // switch to wallets page

    waitFor(actionDelay)

    val walletOptionsButton = Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.iv_more),
            isImmediateChild(ViewMatchers.withId(R.id.container)), ViewMatchers.isDisplayed()))
    walletOptionsButton.perform(ViewActions.click())

    waitForWithId(R.id.popup_rv) // wait for popup to show up
    waitFor(actionDelay)

    Espresso.onView(ViewMatchers.withId(R.id.popup_rv))
            .perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(5, ViewActions.click())) // Wallet Settings

    waitForWithId(R.id.wallet_settings_scroll_view) // wait for wallet settings activity
    waitFor(actionDelay)

    // scroll to delete button(smaller screens) and perform click
    Espresso.onView(ViewMatchers.withId(R.id.remove_wallet)).perform(ViewActions.scrollTo(), ViewActions.click())

    waitForWithId(R.id.btn_positive)
    waitFor(actionDelay)

    // Confirm delete wallet warning and proceed
    Espresso.onView(ViewMatchers.withId(R.id.btn_positive)).perform(ViewActions.click())
    waitFor(actionDelay)
}