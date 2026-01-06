package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;

public class ChannelListTVOverlay extends Fragment implements KeyDownReceiver, EpgUtils.EpgUpdateListener {

    public TVPlayerActivity context;
    private TVChannelListOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    private VerticalGridView recyclerView;
    private Timer clockTimer;

    private static ChannelListTVOverlay INSTANCE;

    public static void notifyChannelListChanged() {
        if (INSTANCE != null) {
            INSTANCE.updateChannelList();
        }
    }

    public void updateChannelList() {
        tvOverlayRecyclerAdapter.objects = ChannelUtils.getAllChannels(requireContext());
        requireActivity().runOnUiThread(() -> tvOverlayRecyclerAdapter.notifyDataSetChanged());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        INSTANCE = this;
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.overlay_channel_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        INSTANCE = this;

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        tvOverlayRecyclerAdapter = new TVChannelListOverlayRecyclerAdapter(this, ChannelUtils.getAllChannels(requireContext()), recyclerView);


        LinearLayoutManager llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);
        int lastSelectedChannel = ChannelUtils.getLastSelectedChannel(requireContext()) - 1;
        tvOverlayRecyclerAdapter.selectedChannel = lastSelectedChannel + 1;
        recyclerView.scrollToPosition(lastSelectedChannel);

        BrowseFrameLayout browseFrameLayout = view.findViewById(R.id.tvoverlayrecyclerBrowse);
        browseFrameLayout.setOnFocusSearchListener(new BrowseFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                if (recyclerView.hasFocus())
                    return focused; // keep focus on recyclerview! DO NOT return recyclerview, but focused, which is a child of the recyclerview
                else
                    return null; // someone else will find the next focus
            }
        });

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startUpdateTimer();
        EpgUtils.addEpgUpdateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EpgUtils.removeEpgUpdateListener(this);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(recyclerView != null) {
            int lastSelectedChannel = ChannelUtils.getLastSelectedChannel(requireContext());
            tvOverlayRecyclerAdapter.selectedChannel = lastSelectedChannel - 1;
            recyclerView.scrollToPosition(tvOverlayRecyclerAdapter.selectedChannel);
            tvOverlayRecyclerAdapter.notifyItemChanged(tvOverlayRecyclerAdapter.selectedChannel);
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            context.popOverlayFragment();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        return false;
    }


    public void startUpdateTimer() {
        if (clockTimer == null) {
            clockTimer = new Timer();
            clockTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateInfo();
                }
            }, 0, 1000);
        }
    }

    public void stopUpdateTimer() {
        if (clockTimer != null) {
            clockTimer.cancel();
            clockTimer = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startUpdateTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdateTimer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        INSTANCE = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        stopUpdateTimer();
    }

    public void updateInfo() {
        DateFormat dfdate = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        final String date = dfdate.format(new Date());
        DateFormat dftime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        final String time = dftime.format(new Date());

        if (context != null)
            context.runOnUiThread(() -> {
                if(getView() != null) {
                    TextView dateView = getView().findViewById(R.id.tvoverlaydate);
                    TextView timeView = getView().findViewById(R.id.tvoverlaytime);
                    if (dateView == null || timeView == null) {
                        return;
                    }

                    dateView.setText(date);
                    timeView.setText(time);
                }
            });
    }

    @Override
    public void onEpgNowChanged(ChannelUtils.Channel channel, EpgUtils.EpgEvent newEpgEvent) {
        if(getActivity() == null || tvOverlayRecyclerAdapter == null) {
            return;
        }
        requireActivity().runOnUiThread(() -> tvOverlayRecyclerAdapter.notifyItemChanged(channel.number-1));
    }
}
