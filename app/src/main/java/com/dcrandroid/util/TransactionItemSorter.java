package com.dcrandroid.util;

import java.util.ArrayList;
import java.util.List;

public class TransactionItemSorter {

    private static TransactionsResponse.TransactionItem itemA;
    private static int i;
    private static List<TransactionsResponse.TransactionItem> mainTransactionList = new ArrayList<>();

    public static void itemSorter(List<TransactionsResponse.TransactionItem> transactionItemList, List<TransactionsResponse.TransactionItem> temp, String type) {

        mainTransactionList.addAll(transactionItemList);


        if (mainTransactionList.size() != temp.size()) {
            mainTransactionList.clear();
            mainTransactionList.addAll(temp);
        }


        for (i = 0; i < mainTransactionList.size(); i++) {
            itemA = mainTransactionList.get(i);
            sortBy(type);
        }

        transactionItemList.clear();
        transactionItemList.addAll(mainTransactionList);
    }

    private static void sortBy(String type) {
        if (type.equalsIgnoreCase("regular")) {
            if (!itemA.type.equalsIgnoreCase(type)) {
                mainTransactionList.remove(itemA);
                i--;
            }
        } else if (type.equalsIgnoreCase("ticket")) {
            if (!itemA.type.equalsIgnoreCase(type)) {
                mainTransactionList.remove(itemA);
                i--;
            }
        } else if (type.equalsIgnoreCase("vote")) {
            if (!itemA.type.equalsIgnoreCase(type)) {
                mainTransactionList.remove(itemA);
                i--;
            }
        } else if (type.equalsIgnoreCase("revoke")) {
            if (!itemA.type.equalsIgnoreCase(type)) {
                mainTransactionList.remove(itemA);
                i--;
            }
        } else if (Integer.parseInt(type) == 0) {
            if (itemA.getDirection() != Integer.parseInt(type)) {
                mainTransactionList.remove(itemA);
                i--;
            }
        } else if (Integer.parseInt(type) == 1) {
            if (itemA.getDirection() != Integer.parseInt(type)) {
                mainTransactionList.remove(itemA);
                i--;
            }
        }
    }


}
