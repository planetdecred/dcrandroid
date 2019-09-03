/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.extensions

import android.view.View

fun View.toggleVisibility(): Int {
    this.visibility = if (this.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    return this.visibility
}

fun View.show() {
    this.visibility = View.VISIBLE
}

fun View.hide() {
    this.visibility = View.GONE
}

fun View.isShowing(): Boolean{
    return this.visibility == View.VISIBLE
}