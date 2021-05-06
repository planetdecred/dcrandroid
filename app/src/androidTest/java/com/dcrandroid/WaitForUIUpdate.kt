/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid

import android.view.View
import androidx.annotation.CheckResult
import androidx.test.espresso.*
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.allOf
import org.junit.Assert
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

val actionDelay = 1000L

fun waitForWithId(id: Int) {

    var element: ViewInteraction
    do {
        waitFor(500)

        // simple example using withText Matcher.
        element = Espresso.onView(allOf(ViewMatchers.withId(id), ViewMatchers.isDisplayed()))
    } while (!MatcherExtension.exists(element))

}

fun waitFor(ms: Long) {
    val signal = CountDownLatch(1)

    try {
        signal.await(ms, TimeUnit.MILLISECONDS)
    } catch (e: InterruptedException) {
        Assert.fail(e.message)
    }
}

object MatcherExtension {
    @CheckResult
    fun exists(interaction: ViewInteraction): Boolean {
        return try {
            interaction.perform(object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return Matchers.any(View::class.java)
                }

                override fun getDescription(): String {
                    return "check for existence"
                }

                override fun perform(uiController: UiController?, view: View?) {
                    // no op, if this is run, then the execution will continue after .perform(...)
                }
            })
            true
        } catch (ex: AmbiguousViewMatcherException) {
            // if there's any interaction later with the same matcher, that'll fail anyway
            true // we found more than one
        } catch (ex: NoMatchingViewException) {
            false
        } catch (ex: NoMatchingRootException) {
            // optional depending on what you think "exists" means
            false
        }
    }
}