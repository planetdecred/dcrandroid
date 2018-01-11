package com.decrediton.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decrediton.R;
import com.decrediton.data.Settings;

import java.util.List;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.MyViewHolder> {

    private List<Settings> settingsList;
    private LayoutInflater layoutInflater;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView setting;

        public MyViewHolder(View view) {
            super(view);
            setting = view.findViewById(R.id.settings_connection);
        }
    }
    public SettingsAdapter(List<Settings> settingsListList , LayoutInflater inflater) {
        this.settingsList = settingsListList;
        this.layoutInflater = inflater;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView =layoutInflater.inflate(R.layout.settings_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {

        Settings settings = settingsList.get(position);
        holder.setting.setText(settings.getSetting());
    }

    @Override
    public int getItemCount() {
        return  settingsList.size();
    }
}
