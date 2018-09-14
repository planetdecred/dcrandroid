package com.dcrandroid.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.dcrandroid.R;

import java.util.List;

public class NavigationListAdapter extends BaseAdapter {

    private List<NavigationBarItem> items;

    private LayoutInflater inflater;

    public static class NavigationBarItem{
        private String title;
        private int icon;

        public NavigationBarItem(String title, int icon){
            this.title = title;
            this.icon = icon;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setIcon(int icon) {
            this.icon = icon;
        }

        public int getIcon() {
            return icon;
        }
    }

    public NavigationListAdapter(Context ctx, List<NavigationBarItem> items){
        this.items = items;
        inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View vi, ViewGroup parent) {
        if(vi == null){
            vi = inflater.inflate(R.layout.nav_bar_list_item, parent, false);
        }

        NavigationBarItem item = items.get(position);

        TextView tv = vi.findViewById(R.id.title);
        ImageView icon = vi.findViewById(R.id.icon);

        tv.setText(item.getTitle());
        icon.setImageResource(item.getIcon());

        return vi;
    }
}
