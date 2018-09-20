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

    public static class sortItemsBySent implements Comparator<TransactionsResponse.TransactionItem> {

        @Override
        public int compare(TransactionsResponse.TransactionItem o1, TransactionsResponse.TransactionItem o2) {
            if (o1.getDirection() == 0 && o2.getDirection() == 0) {
                return 0;
            }

            return o1.getDirection() < o2.getDirection() && o1.getDirection() == 0 ? -1 : 1;
        }
    }

    public static class sortItemsByReceived implements Comparator<TransactionsResponse.TransactionItem> {

        @Override
        public int compare(TransactionsResponse.TransactionItem o1, TransactionsResponse.TransactionItem o2) {
            if (o1.getDirection() == 1 && o2.getDirection() == 1) {
                return 0;
            }
            return o1.getDirection() > o2.getDirection() && o1.getDirection() == 1 ? -1 : 1;
        }
    }

    public static class sortItemsByRegularType implements Comparator<TransactionsResponse.TransactionItem> {

        @Override
        public int compare(TransactionsResponse.TransactionItem o1, TransactionsResponse.TransactionItem o2) {
            String regular = "regular";
            if (o1.type.equalsIgnoreCase(regular) && o2.type.equalsIgnoreCase(regular)) {
                return 0;
            }
            return o1.type.equalsIgnoreCase(regular) && !o2.type.equalsIgnoreCase(regular) ? -1 : 1;
        }

    }
}
