package com.dcrandroid.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.dcrandroid.MainActivity;
import com.dcrandroid.R;
import com.dcrandroid.activities.ProposalDetails;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Proposal;
import com.dcrandroid.util.NetworkTask;
import com.dcrandroid.util.PreferenceUtil;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.core.app.NotificationCompat;

import static android.content.Context.NOTIFICATION_SERVICE;


public class AlarmReceiver extends BroadcastReceiver {
    Context context;
    PreferenceUtil util;
    NotificationManager notificationManager;
    boolean voting_start_notifications;
    boolean voting_end_notifications;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        util = new PreferenceUtil(context);
        voting_start_notifications = util.getBoolean("voting_start_notifications", false);
        voting_end_notifications = util.getBoolean("voting_end_notifications", false);
        if (voting_start_notifications || voting_end_notifications) {
            if (intent.getAction() != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
                new MainActivity().enablePoliteiaNotifs();
            }
            registerProposalNotificationChannel();
            processProposalNotification();
        }
    }

    private void registerProposalNotificationChannel() {
        notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("new politeia proposal", context.getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setImportance(NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void processProposalNotification() {
        final HashMap<String, String> config = new HashMap<>();
        config.put("url", "https://proposals.decred.org/api/v1/proposals/votestatus");
        new NetworkTask(config, new NetworkTask.AsyncResponse() {
            @Override
            public void onResponse(@NotNull final String votesReponse) {
                HashMap<String, String> config = new HashMap<>();
                config.put("url", context.getString(R.string.politeia_proposals_url));
                new NetworkTask(config, new NetworkTask.AsyncResponse() {
                    @Override
                    public void onResponse(@NotNull String proposalsResponse) {
                        try {
                            ArrayList<String> vote_started_tokens = new ArrayList<>();
                            ArrayList<String> old_vote_started_tokens = util.getStringList("vote_started_tokens");
                            ArrayList<String> vote_finished_tokens = new ArrayList<>();
                            ArrayList<String> old_vote_finished_tokens = util.getStringList("vote_started_tokens");

                            JSONObject jsonObject = new JSONObject(votesReponse);
                            JSONArray votes = jsonObject.getJSONArray("votesstatus");

                            JSONObject jsonObject1 = new JSONObject(proposalsResponse);
                            JSONArray jsonProposals = jsonObject1.getJSONArray("proposals");
                            if (jsonProposals != null) {
                                for (int i = 0; i < jsonProposals.length(); i++) {
                                    JSONObject jsonProposal = jsonProposals.getJSONObject(i);
                                    Proposal proposal = new Proposal();
                                    proposal.setName(jsonProposal.getString("name"));
                                    proposal.setState(jsonProposal.getInt("state"));
                                    proposal.setStatus(jsonProposal.getInt("status"));
                                    proposal.setTimestamp(jsonProposal.getLong("timestamp"));
                                    proposal.setUserid(jsonProposal.getString("userid"));
                                    proposal.setUsername(jsonProposal.getString("username"));
                                    proposal.setPublickey(jsonProposal.getString("publickey"));
                                    proposal.setSignature(jsonProposal.getString("signature"));
                                    proposal.setVersion(jsonProposal.getString("version"));
                                    proposal.setNumcomments(jsonProposal.getInt("numcomments"));

                                    JSONArray jsonFiles = jsonProposal.getJSONArray("files");
                                    ArrayList<Proposal.File> files = new ArrayList<>();

                                    for (int j = 0; j < jsonFiles.length(); j++) {
                                        JSONObject jsonFile = jsonFiles.getJSONObject(j);
                                        Proposal.File file = new Proposal().new File();
                                        file.setName(jsonFile.getString("name"));
                                        file.setMime(jsonFile.getString("mime"));
                                        file.setDigest(jsonFile.getString("digest"));
                                        file.setPayload(jsonFile.getString("payload"));
                                        files.add(file);
                                    }

                                    Proposal.CensorshipRecord record = new Proposal().new CensorshipRecord();
                                    record.setToken(jsonProposal.getJSONObject("censorshiprecord").getString("token"));
                                    record.setMerkle(jsonProposal.getJSONObject("censorshiprecord").getString("merkle"));
                                    record.setSignature(jsonProposal.getJSONObject("censorshiprecord").getString("signature"));

                                    Proposal.VoteStatus voteStatus = new Proposal().new VoteStatus();
                                    for (int k = 0; votes != null && k < votes.length(); k++) {
                                        JSONObject vote = votes.getJSONObject(k);
                                        if (vote.getString("token").equals(record.getToken())) {
                                            voteStatus.setStatus(vote.getInt("status"));
                                        }
                                    }
                                    proposal.setFiles(files);
                                    proposal.setCensorshipRecord(record);
                                    proposal.setVoteStatus(voteStatus);

                                    if (proposal.getVoteStatus() == null || proposal.getCensorshipRecord() == null)
                                        continue;

                                    if (!old_vote_started_tokens.contains(proposal.getCensorshipRecord().getToken()) && proposal.getVoteStatus().getStatus() == 2 && voting_start_notifications) {
                                        sendNotification(proposal, i, "Voting has started on");
                                    }
                                    if (!old_vote_finished_tokens.contains(proposal.getCensorshipRecord().getToken()) && proposal.getVoteStatus().getStatus() == 3 && voting_end_notifications) {
                                        sendNotification(proposal, i, "Voting has finished on");
                                    }
                                    vote_started_tokens.add(proposal.getCensorshipRecord().getToken());
                                    vote_finished_tokens.add(proposal.getCensorshipRecord().getToken());
                                }
                                util.setStringList("vote_started_tokens", vote_started_tokens);
                                util.setStringList("vote_finished_tokens", vote_finished_tokens);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        t.printStackTrace();
                    }
                }).execute();
            }

            @Override
            public void onFailure(final Throwable t) {
                t.printStackTrace();
            }
        }).execute();
    }

    void sendNotification(Proposal proposal, int index, final String text) {
        Intent launchIntent = new Intent(context, ProposalDetails.class);
        launchIntent.setAction(Constants.NEW_POLITEIA_PROPOSAL_NOTIFICATION);
        launchIntent.putExtra("proposal", proposal);

        PendingIntent launchPendingIntent = PendingIntent.getActivity(context, index, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context, "new politeia proposal")
                .setContentTitle(text + " " + proposal.getName())
                .setSmallIcon(R.drawable.politeia2)
                .setOngoing(false)
                .setAutoCancel(true)
                .setGroup("com.dcrandroid.NEW_PROPOSAL_NOTIFS")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(launchPendingIntent)
                .build();

        Notification groupSummary = new NotificationCompat.Builder(context, "new politeia proposal")
                .setContentTitle(text + " " + proposal.getName())
                .setSmallIcon(R.drawable.politeia2)
                .setGroup("com.dcrandroid.NEW_PROPOSAL_NOTIFS")
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        synchronized (notificationManager) {
            notificationManager.notify(index + 100, notification);
            notificationManager.notify(383829, groupSummary);
        }
    }
}