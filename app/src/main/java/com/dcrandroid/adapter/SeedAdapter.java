package com.dcrandroid.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dcrandroid.R;

import java.util.List;

/**
 * Created by Macsleven on 25/12/2017.
 */

public class SeedAdapter extends RecyclerView.Adapter<SeedAdapter.MyViewHolder> {
    private List<String> seedList;
    private List<Integer> positionOfItems;

    public SeedAdapter(List<String> seedListList, List<Integer> positionOfItems) {
        this.seedList = seedListList;
        this.positionOfItems = positionOfItems;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.seed_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        String seed = seedList.get(position);
        int seedPosition = positionOfItems.get(position);
        holder.seed.setText(String.format("%s. %s", String.valueOf(seedPosition + 1), seed));
    }

    @Override
    public int getItemCount() {
        return seedList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView seed;

        public MyViewHolder(View view) {
            super(view);
            seed = view.findViewById(R.id.seed);

        }
    }
}