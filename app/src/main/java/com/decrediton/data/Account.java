package com.decrediton.data;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class Account {
    private String accountName;
    private String spendable;
    private String total;
    private String immatureRewards = "";
    private String lockedByTickets = "";
    private String votingAuthority = "";
    private String immatureStakeGeneration = "";
    private String accountNumber = "";
    private String hDPath = "";
    private String keys = "";
    public Account(){
    }
    public Account(String accountName,String spendable,String total,String immatureRewards,String lockedByTickets,String votingAuthority,String immatureStakeGeneration,String accountNumber,String hDPath,String keys){
        this.accountName = accountName;
        this.accountNumber = accountNumber;
        this.spendable = spendable;
        this.total = total;
        this.immatureRewards = immatureRewards;
        this.lockedByTickets = lockedByTickets;
        this.votingAuthority =votingAuthority;
        this.immatureStakeGeneration = immatureStakeGeneration;
        this.hDPath = hDPath;
        this.keys =keys;

    }

    public String getAccountName(){
        return  accountName;
    }

    public void setAccountName(String accountName){
        this.accountName = accountName;
    }

    public String getSpendable() {
        return spendable;
    }

    public void setSpendable(String spendable) {
        this.spendable = spendable;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getImmatureRewards() {
        return immatureRewards;
    }

    public void setImmatureRewards(String immatureRewards) {
        this.immatureRewards = immatureRewards;
    }

    public String getLockedByTickets() {
        return lockedByTickets;
    }

    public void setLockedByTickets(String lockedByTickets) {
        this.lockedByTickets = lockedByTickets;
    }

    public String getVotingAuthority() {
        return votingAuthority;
    }

    public void setVotingAuthority(String votingAuthority) {
        this.votingAuthority = votingAuthority;
    }

    public String getImmatureStakeGeneration() {
        return immatureStakeGeneration;
    }

    public void setImmatureStakeGeneration(String immatureStakeGeneration) {
        this.immatureStakeGeneration = immatureStakeGeneration;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String gethDPath() {
        return hDPath;
    }

    public void sethDPath(String hDPath) {
        this.hDPath = hDPath;
    }

    public String getKeys() {
        return keys;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }
}
