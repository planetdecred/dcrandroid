package com.dcrandroid.activities;

import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.data.Proposal;
import com.dcrandroid.util.NetworkTask;
import com.dcrandroid.util.Utils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;

public class ProposalDetails extends AppCompatActivity {
    TextView tv_title, tv_description, tv_meta, tv_progress;
    Proposal proposal;
    ProgressBar progressBar, vote_progress;

    public CharSequence convert(CharSequence text) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        Pattern star = Pattern.compile("\\Q*\\E(.*?)\\Q*\\E");
        if (star != null) {
            Matcher matcher = star.matcher(text);
            int matchesSoFar = 0;
            while (matcher.find()) {
                int start = matcher.start() - (matchesSoFar * 2);
                int end = matcher.end() - (matchesSoFar * 2);
                ssb.setSpan(new BackgroundColorSpan(0xFF404040), start + 1, end - 1, 0);
                ssb.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.blue)), start + 1, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new RelativeSizeSpan(1f), start + 1, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new StyleSpan(Typeface.NORMAL), start + 1, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new TypefaceSpan("monospace"), start + 1, end - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.delete(start, start + 1);
                ssb.delete(end - 2, end - 1);
                matchesSoFar++;
            }
        }
        return ssb;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        setTitle("Proposal Details");
        setContentView(R.layout.activity_proposal_details);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tv_title = findViewById(R.id.title);
        tv_description = findViewById(R.id.description);
        progressBar = findViewById(R.id.progress);
        vote_progress = findViewById(R.id.progressBar);
        tv_progress = findViewById(R.id.vote_progress);

        tv_meta = findViewById(R.id.meta);

        proposal = (Proposal) getIntent().getSerializableExtra("proposal");
        loadProposal();
    }

    void loadContent() {
        String payload;
        StringBuilder description = new StringBuilder();
        for (int i = 0; proposal.getFiles() != null && i < proposal.getFiles().size(); i++) {
            if (proposal.getFiles().get(i).getName().equals("index.md")) {
                payload = proposal.getFiles().get(i).getPayload();
                byte[] data = Base64.decode(payload, Base64.DEFAULT);
                try {
                    description.append(new String(data, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        tv_description.setText(description.toString());
    }

    void loadProposal() {
        tv_title.setText(proposal.getName());
        String meta = String.format(Locale.getDefault(), "updated %s \nby %s \nversion %s - %d Comments",
                Utils.calculateTime(System.currentTimeMillis() / 1000 - proposal.getTimestamp(), this), proposal.getUsername(), proposal.getVersion(), proposal.getNumcomments());
        tv_meta.setText(meta, TextView.BufferType.SPANNABLE);

        if (proposal.getFiles() != null && proposal.getFiles().size() < 1) {
            progressBar.setVisibility(View.VISIBLE);
            HashMap<String, String> config = new HashMap<>();
            config.put("url", "https://proposals.decred.org/api/v1/proposals/" + proposal.getCensorshipRecord().getToken());
            new NetworkTask(config, new NetworkTask.AsyncResponse() {
                @Override
                public void onResponse(@NotNull String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        ArrayList<Proposal.File> files = new ArrayList<>();
                        JSONArray proposal_files = jsonObject.getJSONObject("proposal").getJSONArray("files");
                        for (int i = 0; i < proposal_files.length(); i++) {
                            Proposal.File file = new Proposal().new File();
                            file.setName(proposal_files.getJSONObject(i).getString("name"));
                            file.setMime(proposal_files.getJSONObject(i).getString("mime"));
                            file.setDigest(proposal_files.getJSONObject(i).getString("digest"));
                            file.setPayload(proposal_files.getJSONObject(i).getString("payload"));
                            files.add(file);
                        }
                        proposal.setFiles(files);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                loadContent();
                            }
                        });
                    } catch (final JSONException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(ProposalDetails.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ProposalDetails.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                    t.printStackTrace();
                }
            }).execute();
        } else {
            loadContent();
        }

        HashMap<String, String> config = new HashMap<>();
        config.put("url", "https://proposals.decred.org/api/v1/proposals/" + proposal.getCensorshipRecord().getToken() + "/votestatus");
        new NetworkTask(config, new NetworkTask.AsyncResponse() {
            @Override
            public void onResponse(@NotNull String votesReponse) {
                try {
                    JSONObject vote = new JSONObject(votesReponse);
                    Proposal.VoteStatus voteStatus = new Proposal().new VoteStatus();
                    voteStatus.setStatus(vote.getInt("status"));
                    voteStatus.setTotalvotes(vote.getInt("totalvotes"));
                    if (!vote.isNull("optionsresult")) {
                        voteStatus.setNo(vote.getJSONArray("optionsresult").getJSONObject(0).getInt("votesreceived"));
                        voteStatus.setYes(vote.getJSONArray("optionsresult").getJSONObject(1).getInt("votesreceived"));
                    }
                    voteStatus.setTotalvotes(vote.getInt("totalvotes"));
                    proposal.setVoteStatus(voteStatus);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (proposal.getVoteStatus() != null && proposal.getVoteStatus().getTotalvotes() != 0) {
                                tv_progress.setVisibility(View.VISIBLE);
                                vote_progress.setVisibility(View.VISIBLE);
                                float percentage = ((float) proposal.getVoteStatus().getYes() / (float) proposal.getVoteStatus().getTotalvotes()) * 100;

                                tv_progress.setText(String.format(Locale.getDefault(), "%.2f%%", percentage));
                                vote_progress.setProgress((int) percentage);
                            } else {
                                tv_progress.setVisibility(View.GONE);
                                vote_progress.setVisibility(View.GONE);
                            }
                        }
                    });
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.proposal_details_menu, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share_proposal:
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                share.putExtra(Intent.EXTRA_SUBJECT, proposal.getName());
                share.putExtra(Intent.EXTRA_TEXT, "http://proposals.decred.org/proposals/" + proposal.getCensorshipRecord().getToken());
                startActivity(Intent.createChooser(share, "Share Proposal Link"));
                break;
            case R.id.open_proposal:
                String url = "http://proposals.decred.org/proposals/" + proposal.getCensorshipRecord().getToken();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(Constants.NEW_POLITEIA_PROPOSAL_NOTIFICATION)) {
            proposal = (Proposal) intent.getSerializableExtra("proposal");
            loadProposal();
        }
    }
}