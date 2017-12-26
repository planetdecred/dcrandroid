package com.decrediton.Adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.decrediton.data.Seed;
import com.decrediton.R;

import java.util.List;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SeedAdapter extends RecyclerView.Adapter<SeedAdapter.MyViewHolder> {
    private List<Seed> seedList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView seed;

        public MyViewHolder(View view) {
            super(view);
            seed = view.findViewById(R.id.seed);

        }
    }
    public SeedAdapter(List<Seed> seedListList) {
        this.seedList = seedListList;
    }
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.seed_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Seed seed = seedList.get(position);
        holder.seed.setText(seed.getSeed());

    }

    @Override
    public int getItemCount() {
        return seedList.size();
    }
}