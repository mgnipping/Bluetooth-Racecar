package com.example.mmg.bt_racecar;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by mmg on 2017-04-20.
 */
public class BTConFragment extends Fragment implements Button.OnClickListener{

    View view = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_control, container, false);

        Button btn_disconnect = (Button)view.findViewById(R.id.fragctrl_btn_disconnect);
        btn_disconnect.setOnClickListener(this);

        return view;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onClick(View v) {
        ((MainActivity)getContext()).killBTConnection();
    }
}
