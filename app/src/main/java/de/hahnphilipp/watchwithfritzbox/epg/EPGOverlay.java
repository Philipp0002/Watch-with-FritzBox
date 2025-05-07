package de.hahnphilipp.watchwithfritzbox.epg;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.GsonBuilder;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVPlayerActivity;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGOverlay extends Fragment implements EPGEventsAdapter.OnEventListener {

    public TVPlayerActivity context;
    private EPGChannelsAdapter epgChannelsAdapter;
    private VerticalGridView recyclerView;
    private RecyclerView timeRecyclerView;
    private View liveTimeline;
    private Timer clockTimer;
    private TextView channelTitle;
    private TextView epgTitle;
    private TextView epgSubtitle;
    private TextView epgTime;
    private TextView epgDescription;

    private LocalDateTime initTime;

    public boolean recyclerViewSelectedInit = false;

    public int currentScrollX = 0;
    private boolean isSyncingScroll = false;

    public RecyclerView.OnScrollListener syncScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            if (isSyncingScroll || dx == 0) return; // Verhindert Endlosschleife
            isSyncingScroll = true;


            // Alle anderen RecyclerViews auf dieselbe Position setzen
            for (RecyclerView otherRecycler : epgChannelsAdapter.allEventRecyclerViews) {
                if (otherRecycler != recyclerView) {
                    otherRecycler.removeOnScrollListener(syncScrollListener);
                    otherRecycler.scrollBy(dx, 0);
                    otherRecycler.addOnScrollListener(syncScrollListener);
                }
            }
            if (timeRecyclerView != recyclerView) {
                timeRecyclerView.removeOnScrollListener(syncScrollListener);
                timeRecyclerView.scrollBy(dx, 0);
                timeRecyclerView.removeOnScrollListener(syncScrollListener);
            }
            currentScrollX += dx; // Neue Position speichern

            updateLiveTimeLine();
            isSyncingScroll = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.overlay_epg, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initTime = LocalDateTime.now()
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusHours(1);

        liveTimeline = view.findViewById(R.id.live_time_line);
        recyclerView = view.findViewById(R.id.epgchannelsrecycler);
        channelTitle = view.findViewById(R.id.epgchanneltitle);
        epgTitle = view.findViewById(R.id.epgtitle);
        epgTime = view.findViewById(R.id.epgtime);
        epgSubtitle = view.findViewById(R.id.epgsubtitle);
        epgDescription = view.findViewById(R.id.epgdescription);

        timeRecyclerView = view.findViewById(R.id.epgtimelineRecycler);
        timeRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        timeRecyclerView.setAdapter(new EPGTimeSlotAdapter(initTime));
        //currentScrollX = EpgUtils.secondsToPx(initTime.until(LocalDateTime.now(), ChronoUnit.SECONDS)) - (timeRecyclerView.getWidth()/2);
        timeRecyclerView.addOnScrollListener(syncScrollListener);
        timeRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                timeRecyclerView.scrollBy(EpgUtils.secondsToPx(initTime.until(LocalDateTime.now(), ChronoUnit.SECONDS)) - (timeRecyclerView.getWidth()/2), 0);
            }
        });

        epgChannelsAdapter = new EPGChannelsAdapter(this, ChannelUtils.getAllChannels(context), initTime, this);
        recyclerView.setAdapter(epgChannelsAdapter);

        updateLiveTimeLine();

        int lastSelectedChannel = ChannelUtils.getLastSelectedChannel(context) - 1;
        recyclerView.scrollToPosition(lastSelectedChannel);
    }

    private void setDetails(ChannelUtils.Channel channel, EpgUtils.EpgEvent epgEvent) {
        if(epgEvent == null) {
            return;
        }

        LocalDateTime startTime = epgEvent.getStartLocalDateTime();
        LocalDateTime endTime = epgEvent.getEndLocalDateTime();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

        ArrayList<String> timeInfos = new ArrayList<>();
        if(epgEvent.duration < Long.MAX_VALUE / 2) {
            long durationMin = epgEvent.duration / 60;
            timeInfos.add(durationMin + " min");
        }
        if(endTime == null) {
            timeInfos.add(context.getString(R.string.epg_starting_from, startTime.format(timeFormatter)));
        } else {
            if (startTime.toLocalDate().equals(endTime.toLocalDate())) {
                timeInfos.add(startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter));
            } else {
                timeInfos.add(startTime.format(dateTimeFormatter) + " - " + endTime.format(dateTimeFormatter));
            }
        }

        channelTitle.setText(channel.title);
        epgTitle.setText(epgEvent.title);
        if(epgEvent.subtitle != null && !epgEvent.subtitle.isEmpty()) {
            epgSubtitle.setVisibility(VISIBLE);
            epgSubtitle.setText(epgEvent.subtitle);
        } else {
            epgSubtitle.setVisibility(GONE);
        }
        epgTime.setText(timeInfos.stream().collect(Collectors.joining(" | ")));
        epgDescription.setText(epgEvent.description);

        // Log.d("EPGOverlay", new GsonBuilder().setPrettyPrinting().create().toJson(epgEvent));
    }

    private void updateLiveTimeLine() {
        long secondsDiff = initTime.until(LocalDateTime.now(), ChronoUnit.SECONDS);
        int scrollOffset = timeRecyclerView.computeHorizontalScrollOffset();

        int px = EpgUtils.secondsToPx(secondsDiff);
        px -= scrollOffset;

        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) liveTimeline.getLayoutParams();
        params.leftMargin = px;
        liveTimeline.setLayoutParams(params);
        if(px < 0) {
            liveTimeline.setVisibility(View.INVISIBLE);
        } else {
            liveTimeline.setVisibility(VISIBLE);
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
            clockTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    requireActivity().runOnUiThread(() -> {
                        //updateInfo();
                        updateLiveTimeLine();
                    });
                }
            }, 0, 30000);
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

        if(getView() != null) {
            TextView dateView = getView().findViewById(R.id.tvoverlaydate);
            TextView timeView = getView().findViewById(R.id.tvoverlaytime);
            if (dateView == null || timeView == null) {
                return;
            }

            dateView.setText(date);
            timeView.setText(time);
        }
    }

    @Override
    public void onEventSelected(ChannelUtils.Channel channel, EpgUtils.EpgEvent event) {
        requireActivity().runOnUiThread(() -> setDetails(channel, event));
    }

    @Override
    public void onEventDeselected(ChannelUtils.Channel channel, EpgUtils.EpgEvent event) {

    }

    @Override
    public void onEventClicked(ChannelUtils.Channel channel, EpgUtils.EpgEvent event) {
        ChannelUtils.updateLastSelectedChannel(context, channel.number);
        context.launchPlayer(false);
        context.popAllOverlayFragments();
    }
}
