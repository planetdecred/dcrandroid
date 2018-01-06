package com.decrediton.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.decrediton.Activities.ConnectionActivity;
import com.decrediton.Activities.GetPeersActivity;
import com.decrediton.Activities.TransactionDetailsActivity;
import com.decrediton.Adapter.SettingsAdapter;
import com.decrediton.Adapter.TransactionAdapter;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.data.Settings;
import com.decrediton.data.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Macsleven on 29/11/2017.
 */

public class SettingsFragment extends Fragment{
    private List<Settings> settingsList = new ArrayList<>();
    SettingsAdapter settingsAdapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_settings, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        RecyclerView recyclerView = rootView.getRootView().findViewById(R.id.settings_recycler_view);
        settingsAdapter = new SettingsAdapter(settingsList, layoutInflater);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Settings settings = settingsList.get(position);
                SettingsFunction(settings.getSetting());
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        recyclerView.setAdapter(settingsAdapter);
        registerForContextMenu(recyclerView);
        prepareSettingsData();
        return rootView;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Settings");
    }
    private void prepareSettingsData(){
        Settings settings= new Settings("Connection");
        settingsList.add(settings);

    }

    private void SettingsFunction(String settings){
        if(settings.equals("Connection")){
            Intent intent = new Intent(getContext(),ConnectionActivity.class);
            startActivity(intent);
        }
    }
}
