package com.dcrandroid.util;

import java.util.Comparator;

/**
 * Created by collins on 4/28/18.
 */

public class TransactionComparator {

    public static class TimestampSort implements Comparator<TransactionsResponse.TransactionItem> {
        @Override
        public int compare(TransactionsResponse.TransactionItem o1, TransactionsResponse.TransactionItem o2) {
            if (o1.getTimestamp() == o2.getTimestamp()) {
                return 0;
            }
            return o1.getTimestamp() > o2.getTimestamp() ? -1 : 1;
        }
    }

    public static class MinConfirmationSort implements Comparator<TransactionsResponse.TransactionItem> {
        @Override
        public int compare(TransactionsResponse.TransactionItem o1, TransactionsResponse.TransactionItem o2) {
            if (o1.getHeight() == o2.getHeight()) {
                return 0;
            }
            return o1.getHeight() > o2.getHeight() ? -1 : 1;
        }
    }
}
