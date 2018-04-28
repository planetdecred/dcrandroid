package com.dcrandroid.util;

import com.dcrandroid.data.Transaction;

import java.util.Comparator;

/**
 * Created by collins on 4/28/18.
 */

public class TransactionSorter implements Comparator<Transaction> {
    @Override
    public int compare(Transaction o1, Transaction o2) {
        return o1.getTime() > o2.getTime() ? -1 : 1;
    }
}
