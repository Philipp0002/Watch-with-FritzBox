package de.hahnphilipp.watchwithfritzbox.player;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class EditChannelListTVOverlay extends Fragment {

    public TVPlayerActivity context;
    public boolean isShown = false;

    TVChannelListOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    RecyclerView recyclerView;
    LinearLayoutManager llm;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.editchannellisttvoverlay, container, false);
        return v;
    }

    public void updateChannelList(){
        tvOverlayRecyclerAdapter.objects = ChannelUtils.getAllChannels(getContext());
        tvOverlayRecyclerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        tvOverlayRecyclerAdapter = new TVChannelListOverlayRecyclerAdapter(this, ChannelUtils.getAllChannels(getContext()), Picasso.get());
        llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);

    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
