package com.decrediton.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.decrediton.Activities.ConnectionActivity;
import com.decrediton.Adapter.SettingsAdapter;
import com.decrediton.R;
import com.decrediton.Util.RecyclerTouchListener;
import com.decrediton.data.Settings;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Macsleven on 29/11/2017.
 */

public class SettingsFragment extends Fragment{
    private List<Settings> settingsList = new ArrayList<>();
    SettingsAdapter settingsAdapter;
    RecyclerView recyclerView;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.content_settings, container, false);
        LayoutInflater layoutInflater = LayoutInflater.from(rootView.getContext());
        recyclerView = rootView.getRootView().findViewById(R.id.settings_recycler_view);
        settingsAdapter = new SettingsAdapter(settingsList, layoutInflater);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(rootView.getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration( getContext(), LinearLayoutManager.VERTICAL));
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                Settings settings = settingsList.get(position);
                SettingsFunction(settings,position);

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
        getActivity().setTitle(getString(R.string.settings));
    }
    private void prepareSettingsData(){
        Settings settings= new Settings(getString(R.string.connection));
        settingsList.add(settings);
        settings = new Settings(getString(R.string.transaction_confirmations),"0");
        settingsList.add(settings);
    }

    private void SettingsFunction(Settings settings, int position){
        if(settings.getSettingName().equals(getString(R.string.connection))){
            Intent intent = new Intent(getContext(),ConnectionActivity.class);
            startActivity(intent);
        }
        else if(settings.getSettingName().equals(getString(R.string.transaction_confirmations))){
           showTXConfirmDialog(settings.getRightValue(),position);
        }


    }

    public void showTXConfirmDialog(String righV,final int pos) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_tx_confirmation, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);
        final EditText confirm = (EditText) dialogView.findViewById(R.id.tx_confirmation_input);
        confirm.setText(righV);
        dialogBuilder.setMessage(R.string.confirmation_set_info);
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try{
                   settingsList.get(pos).setRightValue(confirm.getText().toString().trim());
                   settingsAdapter.notifyDataSetChanged();
                }
                catch (Exception e){
                    Toast.makeText(getContext(), R.string.invalid_amount,Toast.LENGTH_LONG).show();
                }
                //do something with edt.getText().toString();
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialogBuilder.setCancelable(true);
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }

}
