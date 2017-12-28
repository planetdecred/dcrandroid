package com.decrediton.fragments;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.decrediton.R;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class ReceiveFragment extends android.support.v4.app.Fragment {
    ImageView imageView;
    private TextView address;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        return inflater.inflate(R.layout.account_details_view, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView imageView;
        TextView txtResult;
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Receive");
    }
}
