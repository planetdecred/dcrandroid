/*
 * Copyright (c) 2018-2019 The Decred developers
 * Use of this source code is governed by an ISC
 * license that can be found in the LICENSE file.
 */

package com.dcrandroid.data;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Balance implements Serializable {
    private long spendable, total, immatureReward, immatureStakeGeneration, lockedByTickets, votingAuthority, unConfirmed;

    public static Balance parse(String json) throws JSONException {
        JSONObject obj = new JSONObject(json);
        Balance balance = new Balance();
        balance.total = obj.getLong(Constants.TOTAL);
        balance.spendable = obj.getLong(Constants.SPENDABLE);
        balance.immatureReward = obj.getLong(Constants.IMMATURE_REWARDS);
        balance.immatureStakeGeneration = obj.getLong(Constants.IMMATURE_STAKE_GEN);
        balance.lockedByTickets = obj.getLong(Constants.LOCKED_BY_TICKETS);
        balance.votingAuthority = obj.getLong(Constants.VOTING_AUTHORITY);
        balance.unConfirmed = obj.getLong(Constants.UNCONFIRMED);
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

    public long getImmatureStakeGeneration() {
        return immatureStakeGeneration;
    }

    public void setImmatureStakeGeneration(long immatureStakeGeneration) {
        this.immatureStakeGeneration = immatureStakeGeneration;
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
