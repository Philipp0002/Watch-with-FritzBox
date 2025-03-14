package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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

import java.text.DateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGOverlay extends Fragment {

    public TVPlayerActivity context;
    private EPGChannelsAdapter epgChannelsAdapter;
    private RecyclerView recyclerView;
    private RecyclerView timeRecyclerView;
    private View liveTimeline;
    private Timer clockTimer;

    private OffsetDateTime initTime;

    public int currentScrollX = 0;
    private boolean isSyncingScroll = false;

    public RecyclerView.OnScrollListener syncScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (isSyncingScroll) return; // Verhindert Endlosschleife

            isSyncingScroll = true;
            currentScrollX = recyclerView.computeHorizontalScrollOffset(); // Neue Position speichern

            // Alle anderen RecyclerViews auf dieselbe Position setzen
            for (RecyclerView otherRecycler : epgChannelsAdapter.allEventRecyclerViews) {
                if (otherRecycler != recyclerView) {
                    otherRecycler.scrollBy(dx, 0);
                }
            }
            if (timeRecyclerView != recyclerView) {
                timeRecyclerView.scrollBy(dx, 0);
            }
            updateLiveTimeLine();
            isSyncingScroll = false;
        }
    };

    /*public void updateChannelList() {
        tvOverlayRecyclerAdapter.objects = ChannelUtils.getAllChannels(getContext());
        getActivity().runOnUiThread(() -> tvOverlayRecyclerAdapter.notifyDataSetChanged());
    }*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.overlay_epg, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initTime = OffsetDateTime.now(ZoneOffset.UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusHours(1);

        liveTimeline = view.findViewById(R.id.live_time_line);
        recyclerView = view.findViewById(R.id.epgchannelsrecycler);

        epgChannelsAdapter = new EPGChannelsAdapter(this, ChannelUtils.getAllChannels(context), initTime);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(epgChannelsAdapter);

        timeRecyclerView = view.findViewById(R.id.epgtimelineRecycler);
        timeRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        timeRecyclerView.setAdapter(new EPGTimeSlotAdapter(initTime));

        timeRecyclerView.addOnScrollListener(syncScrollListener);

        updateLiveTimeLine();

        int lastSelectedChannel = ChannelUtils.getLastSelectedChannel(context) - 1;
        //TODO epgChannelsAdapter.selectedChannel = lastSelectedChannel + 1;
        //recyclerView.scrollToPosition(lastSelectedChannel);

        /*BrowseFrameLayout browseFrameLayout = view.findViewById(R.id.epgchannelsrecyclerBrowse);
        browseFrameLayout.setOnFocusSearchListener(new BrowseFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                if (recyclerView.hasFocus())
                    return focused; // keep focus on recyclerview! DO NOT return recyclerview, but focused, which is a child of the recyclerview
                else
                    return null; // someone else will find the next focus
            }
        });*/

    }

    private void updateLiveTimeLine() {
        OffsetDateTime nowTime = OffsetDateTime.now(ZoneOffset.UTC);

        long secondsDiff = initTime.until(nowTime, ChronoUnit.SECONDS);
        int scrollOffset = timeRecyclerView.computeHorizontalScrollOffset();

        int px = EpgUtils.secondsToPx(secondsDiff);
        px -= scrollOffset;

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) liveTimeline.getLayoutParams();
        params.leftMargin = px;
        liveTimeline.setLayoutParams(params);
        if(px < 0) {
            liveTimeline.setVisibility(View.INVISIBLE);
        } else {
            liveTimeline.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startUpdateTimer();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if(recyclerView != null) {
            recyclerView.requestFocus();
        }
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

}
