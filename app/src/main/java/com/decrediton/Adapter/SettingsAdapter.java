package com.decrediton.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decrediton.MainActivity;
import com.decrediton.R;
import com.decrediton.data.Settings;

import java.util.List;

/**
 * Created by Macsleven on 05/01/2018.
 */

public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.MyViewHolder> {

    private List<Settings> settingsList;
    private LayoutInflater layoutInflater;
    MainActivity mainActivity;
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView setting;
        public TextView rightValue;

        public MyViewHolder(View view) {
            super(view);
            setting = view.findViewById(R.id.settings_connection);
            rightValue = view.findViewById(R.id.settings_right_value);
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
        if(position==1){
            holder.setting.setText(settings.getSettingName());
            holder.rightValue.setText(settings.getRightValue());
        }
        else {
            holder.setting.setText(settings.getSettingName());
        }


    }

    @Override
    public int getItemCount() {
        return  settingsList.size();
    }
}
