package com.dcrandroid.util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by collins on 12/30/17.
 */

public class BalanceResponse {
    long total, spendable, immatureReward, immatureStakeGeneration,lockedByTickets, votingAuthority, unConfirmed;
    private BalanceResponse(){}
    public static BalanceResponse parse(String json) throws JSONException {
        BalanceResponse response = new BalanceResponse();
        JSONObject obj = new JSONObject(json);
        response.total = obj.getLong("Total");
        response.spendable = obj.getLong("Spendable");
        response.immatureReward = obj.getLong("ImmatureReward");
        response.immatureStakeGeneration = obj.getLong("ImmatureStakeGeneration");
        response.lockedByTickets = obj.getLong("LockedByTickets");
        response.votingAuthority = obj.getLong("VotingAuthority");
        response.unConfirmed = obj.getLong("UnConfirmed");
        return response;
    }
}
