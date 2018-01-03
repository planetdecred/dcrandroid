package com.decrediton.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.decrediton.R;
import com.decrediton.Util.DcrResponse;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import dcrwallet.Dcrwallet;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class OverviewFragment extends Fragment {
    Button rescan;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //returning our layout file
        //change R.layout.yourlayoutfilename for each of your fragments
        View vi = inflater.inflate(R.layout.content_overview, container, false);
        rescan = (Button) vi.findViewById(R.id.btnRescan);
        return vi;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        getActivity().setTitle("Overview");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rescan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public void run(){
                        try {
                            InputStream fin = getActivity().getAssets().open("rpc.cert");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
                            StringBuilder sb = new StringBuilder();
                            String s;
                            while((s = reader.readLine()) != null){
                                sb.append(s);
                            }
                            DcrResponse response = DcrResponse.parse(Dcrwallet.connectToDcrd("192.168.8.100:9109",sb.toString()));
                            System.out.println("Response: "+response.errorOccurred);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }
}
