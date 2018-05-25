package com.dcrandroid.data;

/**
 * Created by Macsleven on 28/12/2017.
 */

public class Account {
    private String accountName, hdPath, keys;
    private long spendable, total, immatureRewards, lockedByTickets, votingAuthority, immatureStakeGeneration;
    private int accountNumber;

    public String getAccountName(){
        return  accountName;
    }

    public void setAccountName(String accountName){
        this.accountName = accountName;
    }

    public long getSpendable() {
        return spendable;
    }

    public void setSpendable(long spendable) {
        this.spendable = spendable;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getImmatureRewards() {
        return immatureRewards;
    }

    public void setImmatureRewards(long immatureRewards) {
        this.immatureRewards = immatureRewards;
    }

    public long getLockedByTickets() {
        return lockedByTickets;
    }

    public void setLockedByTickets(long lockedByTickets) {
        this.lockedByTickets = lockedByTickets;
    }

    public long getVotingAuthority() {
        return votingAuthority;
    }

    public void setVotingAuthority(long votingAuthority) {
        this.votingAuthority = votingAuthority;
    }

    public long getImmatureStakeGeneration() {
        return immatureStakeGeneration;
    }

    public void setImmatureStakeGeneration(long immatureStakeGeneration) {
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
