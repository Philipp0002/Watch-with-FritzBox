package de.hahnphilipp.watchwithfritzbox.player;

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

import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.IPUtils;

public class EditChannelListTVOverlay extends Fragment {

    public TVPlayerActivity context;
    public String ip = null;

    TVChannelListOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    RecyclerView recyclerView;
    LinearLayoutManager llm;

    private static EditChannelListTVOverlay INSTANCE;

    public static void notifyChannelListChanged() {
        if (INSTANCE != null) {
            INSTANCE.updateChannelList();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        INSTANCE = this;
        return inflater.inflate(R.layout.overlay_edit_channel_list, container, false);
    }

    public void updateChannelList() {
        ArrayList<Integer> changedPositions = new ArrayList<>();
        ArrayList<ChannelUtils.Channel> newChannels = ChannelUtils.getAllChannels(requireContext());
        for(int i = 0; i < tvOverlayRecyclerAdapter.objects.size(); i++) {
            ChannelUtils.Channel oldChannel = tvOverlayRecyclerAdapter.objects.get(i);
            ChannelUtils.Channel newChannel = newChannels.get(i);
            if (!oldChannel.equals(newChannel)) {
                changedPositions.add(i);
            }
        }

        tvOverlayRecyclerAdapter.objects = newChannels;
        getActivity().runOnUiThread(() -> changedPositions.stream().forEach(pos -> tvOverlayRecyclerAdapter.notifyItemChanged(pos)));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        INSTANCE = this;

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        tvOverlayRecyclerAdapter = new TVChannelListOverlayRecyclerAdapter(this, ChannelUtils.getAllChannels(requireContext()), recyclerView);
        llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);

        String rawIP = IPUtils.getIPAddress(true);
        if (!rawIP.isEmpty()) {
            ip = "http://" + rawIP + ":8080";
            ((TextView) view.findViewById(R.id.editChannelListInfoText2)).setText(getResources().getString(R.string.settings_reorder_channels_webserver_info).replace("%d", ip));
        }

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

}
