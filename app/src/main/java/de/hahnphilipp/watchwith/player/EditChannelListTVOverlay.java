package de.hahnphilipp.watchwith.player;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.utils.ChannelUtils;
import de.hahnphilipp.watchwith.utils.IPUtils;

public class EditChannelListTVOverlay extends Fragment {

    public TVPlayerActivity context;
    public String ip;

    TVChannelListOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    RecyclerView recyclerView;
    LinearLayoutManager llm;

    private static EditChannelListTVOverlay INSTANCE;

    public static void notifyChannelListChanged(){
        if(INSTANCE != null){
            INSTANCE.updateChannelList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        INSTANCE = this;
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.editchannellisttvoverlay, container, false);
        return v;
    }

    public void updateChannelList(){
        tvOverlayRecyclerAdapter.objects = ChannelUtils.getAllChannels(getContext());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvOverlayRecyclerAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        INSTANCE = this;

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        tvOverlayRecyclerAdapter = new TVChannelListOverlayRecyclerAdapter(this, ChannelUtils.getAllChannels(getContext()), Picasso.get());
        llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);

        ip = "http://"+IPUtils.getIPAddress(true)+":8080";
        ((TextView)view.findViewById(R.id.editChannelListInfoText2)).setText(getResources().getString(R.string.settings_reorder_channels_webserver_info).replace("%d",ip));

    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
