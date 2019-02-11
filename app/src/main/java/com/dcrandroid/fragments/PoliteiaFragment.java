package com.dcrandroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.adapter.ProposalAdapter;
import com.dcrandroid.data.Proposal;
import com.dcrandroid.util.NetworkTask;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class PoliteiaFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private List<String> filters = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayList<Proposal> proposals = new ArrayList<>();
    private ArrayList<Proposal> filtered_proposals = new ArrayList<>();
    private RecyclerView recyclerView;
    private ProposalAdapter proposalAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private Spinner spinner;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_politeia, container, false);

        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);


        spinner = view.findViewById(R.id.spinnerFilter);


        filters.add("All");

        spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);


        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filter_proposals();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        swipeRefreshLayout.setOnRefreshListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        proposalAdapter = new ProposalAdapter(filtered_proposals, getContext());
        recyclerView.setAdapter(proposalAdapter);

        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        loadProposals();
                    }
                }
        );

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getActivity() == null || getContext() == null) {
            return;
        }
        getActivity().setTitle(getString(R.string.politeia));
    }

    void loadProposals() {
        swipeRefreshLayout.setRefreshing(true);
        final HashMap<String, String> config = new HashMap<>();
        config.put("url", getString(R.string.politeia_proposals_url));
        new NetworkTask(config, new NetworkTask.AsyncResponse() {
            @Override
            public void onResponse(@NotNull final String proposalsResponse) {
                HashMap<String, String> config = new HashMap<>();
                config.put("url", "https://proposals.decred.org/api/v1/proposals/votestatus");
                new NetworkTask(config, new NetworkTask.AsyncResponse() {
                    @Override
                    public void onResponse(@NotNull String votesReponse) {
                        try {

                            swipeRefreshLayout.setRefreshing(false);
                            JSONObject jsonObject1 = new JSONObject(proposalsResponse);
                            JSONArray jsonProposals = jsonObject1.getJSONArray("proposals");
                            if (jsonProposals != null) {
                                proposals.clear();
                                int abandoned = 0;
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

                                    if (jsonProposal.getInt("status") == 6) abandoned++;

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
                                    proposal.setFiles(files);
                                    proposal.setCensorshipRecord(record);
                                    proposals.add(proposal);
                                }


                                JSONObject jsonObject = new JSONObject(votesReponse);
                                JSONArray votes = jsonObject.getJSONArray("votesstatus");
                                int pre = 0, active = 0, finished = 0;
                                for (int i = 0; i < proposals.size(); i++) {
                                    for (int j = 0; votes != null && j < votes.length(); j++) {
                                        JSONObject vote = votes.getJSONObject(j);
                                        if (vote.getString("token").equals(proposals.get(i).getCensorshipRecord().getToken())) {
                                            if (vote.getInt("status") == 1) pre++;
                                            if (vote.getInt("status") == 2) active++;
                                            if (vote.getInt("status") == 3) finished++;
                                            Proposal.VoteStatus voteStatus = new Proposal().new VoteStatus();
                                            voteStatus.setStatus(vote.getInt("status"));
                                            voteStatus.setTotalvotes(vote.getInt("totalvotes"));
                                            if (!vote.isNull("optionsresult")) {
                                                voteStatus.setNo(vote.getJSONArray("optionsresult").getJSONObject(0).getInt("votesreceived"));
                                                voteStatus.setYes(vote.getJSONArray("optionsresult").getJSONObject(1).getInt("votesreceived"));
                                            }
                                            voteStatus.setTotalvotes(vote.getInt("totalvotes"));
                                            proposals.get(i).setVoteStatus(voteStatus);
                                        }

                                    }
                                }

                                if (getActivity() != null) {
                                    final int finalPre = pre, finalActive = active, finalFinished = finished, finalAbandoned = abandoned;
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            filters.clear();
                                            filters.add("All (" + proposals.size() + ")");
                                            filters.add("Pre-Voting (" + finalPre + ")");
                                            filters.add("Active Voting (" + finalActive + ")");
                                            filters.add("Finished Voting (" + finalFinished + ")");
                                            filters.add("Abandoned (" + finalAbandoned + ")");
                                            spinnerAdapter.notifyDataSetChanged();
                                            filter_proposals();
                                        }
                                    });
                                }
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
                swipeRefreshLayout.setRefreshing(false);
                t.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getActivity(), t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).execute();
    }

    void filter_proposals() {
        int pos = spinner.getSelectedItemPosition();
        filtered_proposals.clear();
        switch (pos) {
            case 0:
                filtered_proposals.addAll(proposals);
                break;
            case 1:
            case 2:
            case 3:
                for (int i = 0; i < proposals.size(); i++) {
                    if (proposals.get(i).getVoteStatus() == null) continue;
                    if (proposals.get(i).getVoteStatus().getStatus() == pos) {
                        filtered_proposals.add(proposals.get(i));
                    }
                }
                break;
            case 4:
                for (int i = 0; i < proposals.size(); i++) {
                    if (proposals.get(i).getStatus() == 6) {
                        filtered_proposals.add(proposals.get(i));
                    }
                }

                break;
            default:
                filtered_proposals.addAll(proposals);
        }
        proposalAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRefresh() {
        loadProposals();
    }
}
