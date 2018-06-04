package com.dcrandroid.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Macsleven on 01/01/2018.
 */

public class Transaction implements Serializable{

    private long amount, TransactionFee, time;
    public long totalInput, totalOutput;
    private String type, hash, txDate;
    private int height, direction;
    public boolean animate = false;
    private ArrayList<String> usedInput,walletOutput;

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public void setTime(long time){
        this.time = time;
    }

    public long getTime() {
        return time;
    }

    public void setTotalInput(long totalInput) {
        this.totalInput = totalInput;
    }

    public long getTotalInput() {
        return totalInput;
    }

    public void setTotalOutput(long totalOutput) {
        this.totalOutput = totalOutput;
    }

    public long getTotalOutput() {
        return totalOutput;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getTxDate() {
        return txDate;
    }

    public void setTxDate(String txDate) {
        this.txDate = txDate;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getTransactionFee() {
        return TransactionFee;
    }

    public void setTransactionFee(long transactionFee) {
        this.TransactionFee = transactionFee;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ArrayList<String> getUsedInput() {
        return usedInput;
    }

    public void setUsedInput(ArrayList<String> input) {
        this.usedInput = input;
    }

    public ArrayList<String> getWalletOutput() {
        return walletOutput;
    }

    public void setWalletOutput(ArrayList<String> walletOutput) {
        this.walletOutput = walletOutput;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
