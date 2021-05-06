/*
 * Copyright (c) 2018-2021 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

internal fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {

    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Child at position $position in parent ")
            parentMatcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            val parent = view.parent
            return parent is ViewGroup && parentMatcher.matches(parent)
                    && view == parent.getChildAt(position)
        }
    }
}

internal fun childHasParentId(parentId: Int): Matcher<View> {

    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Has parent Id ")
        }

        public override fun matchesSafely(view: View): Boolean {
            var viewParent: View? = view.parent as View
            do {
                if (viewParent!!.id == parentId) {
                    return true
                }

                viewParent = viewParent.parent as? View // this casting will return null if it's not a View instance
            } while (viewParent != null)

            return false
        }
    }
}

internal fun childHasParentMatcher(parentMatcher: Matcher<View>): Matcher<View> {

    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Child has parent matcher ")
            parentMatcher.describeTo(description)
        }

        public override fun matchesSafely(view: View): Boolean {
            var viewParent: View? = view.parent as? View
            do {
                if (parentMatcher.matches(viewParent!!)) {
                    return true
                }

                viewParent = viewParent.parent as? View // this casting will return null if it's not a View instance
            } while (viewParent != null)

            return false
        }
    }
}

internal fun isImmediateChild(parentMatcher: Matcher<View>): Matcher<View> {

    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("isImmediateChild")
        }

        public override fun matchesSafely(view: View): Boolean {
            val parent = view.parent
            return parent is ViewGroup && parentMatcher.matches(parent)
        }
    }
}