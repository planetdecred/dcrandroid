package com.dcrandroid.data;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class Account {
    private String accountName, hdPath, keys;
    private float spendable, total, immatureRewards, lockedByTickets, votingAuthority, immatureStakeGeneration;
    private int accountNumber;
    public Account(){
    }
    public Account(String accountName, float spendable, float total, float immatureRewards, float lockedByTickets, float votingAuthority, float immatureStakeGeneration, int accountNumber, String hdPath, String keys){
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.spendable = spendable;
        this.total = total;
        this.immatureRewards = immatureRewards;
        this.lockedByTickets = lockedByTickets;
        this.votingAuthority =votingAuthority;
        this.immatureStakeGeneration = immatureStakeGeneration;
        this.hdPath = hdPath;
        this.keys =keys;

    }

    public String getAccountName(){
        return  accountName;
    }

    public void setAccountName(String accountName){
        this.accountName = accountName;
    }

    public float getSpendable() {
        return spendable;
    }

    public void setSpendable(float spendable) {
        this.spendable = spendable;
    }

    public float getTotal() {
        return total;
    }

    public void setTotal(float total) {
        this.total = total;
    }

    public float getImmatureRewards() {
        return immatureRewards;
    }

    public void setImmatureRewards(float immatureRewards) {
        this.immatureRewards = immatureRewards;
    }

    public float getLockedByTickets() {
        return lockedByTickets;
    }

    public void setLockedByTickets(float lockedByTickets) {
        this.lockedByTickets = lockedByTickets;
    }

    public float getVotingAuthority() {
        return votingAuthority;
    }

    public void setVotingAuthority(float votingAuthority) {
        this.votingAuthority = votingAuthority;
    }

    public float getImmatureStakeGeneration() {
        return immatureStakeGeneration;
    }

    public void setImmatureStakeGeneration(float immatureStakeGeneration) {
        this.immatureStakeGeneration = immatureStakeGeneration;
    }

    public int getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(int accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getHDPath() {
        return hdPath;
    }

    public void setHDPath(String HDPath) {
        this.hdPath = HDPath;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }
}
