package com.dcrandroid.data;

import org.json.JSONException;
import org.json.JSONObject;

public class Balance {
    private long spendable, total,immatureReward, immatureStakeGeneration,lockedByTickets, votingAuthority, unConfirmed;

    public static Balance parse(String json) throws JSONException{
        JSONObject obj = new JSONObject(json);
        Balance balance = new Balance();
        balance.total = obj.getLong("Total");
        balance.spendable = obj.getLong("Spendable");
        balance.immatureReward = obj.getLong("ImmatureReward");
        balance.immatureStakeGeneration = obj.getLong("ImmatureStakeGeneration");
        balance.lockedByTickets = obj.getLong("LockedByTickets");
        balance.votingAuthority = obj.getLong("VotingAuthority");
        balance.unConfirmed = obj.getLong("UnConfirmed");
        return balance;
    }

    public long getImmatureReward() {
        return immatureReward;
    }

    public void setImmatureReward(long immatureReward) {
        this.immatureReward = immatureReward;
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

    public void setImmatureStakeGeneration(long immatureStakeGeneration) {
        this.immatureStakeGeneration = immatureStakeGeneration;
    }

    public long getImmatureStakeGeneration() {
        return immatureStakeGeneration;
    }

    public long getLockedByTickets() {
        return lockedByTickets;
    }

    public void setLockedByTickets(long lockedByTickets) {
        this.lockedByTickets = lockedByTickets;
    }

    public long getUnConfirmed() {
        return unConfirmed;
    }

    public void setUnConfirmed(long unConfirmed) {
        this.unConfirmed = unConfirmed;
    }

    public long getVotingAuthority() {
        return votingAuthority;
    }

    public void setVotingAuthority(long votingAuthority) {
        this.votingAuthority = votingAuthority;
    }
}
