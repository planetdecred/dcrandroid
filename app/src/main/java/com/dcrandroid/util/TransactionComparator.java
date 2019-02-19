/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.util;

import com.dcrandroid.data.Transaction;

import java.util.Comparator;

/**
 * Created by collins on 4/28/18.
 */

public class TransactionComparator {

    public static class MinConfirmationSort implements Comparator<Transaction> {
        @Override
        public int compare(Transaction o1, Transaction o2) {
            if (o1.getHeight() == o2.getHeight()) {
                return 0;
            }
            return o1.getHeight() > o2.getHeight() ? -1 : 1;
        }
    }
}
