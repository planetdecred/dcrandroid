package com.dcrandroid.util;

import java.util.ArrayList;
import java.util.List;

public class TransactionItemSorter {

    private static TransactionsResponse.TransactionItem itemA;
    private static int i;
    private static List<TransactionsResponse.TransactionItem> mainTransactionList = new ArrayList<>();

    public static void itemSorter(List<TransactionsResponse.TransactionItem> sortedTransactionList, List<TransactionsResponse.TransactionItem> fixedTransactionList, String type) {

        mainTransactionList.clear();
        mainTransactionList.addAll(sortedTransactionList);

        if (mainTransactionList.size() != fixedTransactionList.size()) {
            mainTransactionList.clear();
            mainTransactionList.addAll(fixedTransactionList);
        }


        for (i = 0; i < mainTransactionList.size(); i++) {
            itemA = mainTransactionList.get(i);
            sortBy(type);
        }

        sortedTransactionList.clear();
        sortedTransactionList.addAll(mainTransactionList);

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
