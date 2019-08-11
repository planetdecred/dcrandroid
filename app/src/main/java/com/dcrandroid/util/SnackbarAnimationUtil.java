package com.dcrandroid.util;

/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

import android.view.animation.Animation;
import android.view.animation.Interpolator;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class SnackbarAnimationUtil {
    public static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();

    SnackbarAnimationUtil() {
    }

    static float lerp(float startValue, float endValue, float fraction) {
        return startValue + fraction * (endValue - startValue);
    }

    static int lerp(int startValue, int endValue, float fraction) {
        return startValue + Math.round(fraction * (float) (endValue - startValue));
    }

    public static class AnimationListenerAdapter implements Animation.AnimationListener {
        public AnimationListenerAdapter() {
        }

        public void onAnimationStart(Animation animation) {
        }

        public void onAnimationEnd(Animation animation) {
        }

        public void onAnimationRepeat(Animation animation) {
        }
    }
}