package com.dcrandroid.util;

import com.dcrandroid.data.Transaction;

import java.util.Comparator;

/**
 * Created by collins on 4/28/18.
 */

public class TransactionSorter implements Comparator<TransactionsResponse.TransactionItem> {
    @Override
    public int compare(TransactionsResponse.TransactionItem o1, TransactionsResponse.TransactionItem o2) {
        if (o1.getTimestamp() == o2.getTimestamp()){
            return 0;
        }
        return o1.getTimestamp() > o2.getTimestamp() ? -1 : 1;
    }
}
