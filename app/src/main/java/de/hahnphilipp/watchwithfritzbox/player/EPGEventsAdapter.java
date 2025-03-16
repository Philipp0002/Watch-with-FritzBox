package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGEventsAdapter extends RecyclerView.Adapter<EPGEventsAdapter.EventViewHolder> {
    private LocalDateTime initTime;
    private ChannelUtils.Channel channel;
    private ArrayList<EpgUtils.EpgEvent> eventList;
    private Context context;
    private OnEventListener listener;

    public EPGEventsAdapter(Context context, ChannelUtils.Channel channel, LocalDateTime initTime, OnEventListener listener) {
        this.channel = channel;
        this.context = context;
        this.initTime = initTime;
        this.listener = listener;
        loadEvents();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.epg_event_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EpgUtils.EpgEvent event = eventList.get(position);

        long duration = event.duration;
        if(event.getStartLocalDateTime().isBefore(initTime)) {
            duration -= initTime.toEpochSecond(ZoneOffset.UTC) - event.startTime;
        }

        holder.itemView.setLayoutParams(new LinearLayout.LayoutParams(EpgUtils.secondsToPx(duration), ViewGroup.LayoutParams.WRAP_CONTENT));

        holder.eventTitle.setText(event.title);
        holder.itemView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            float offset = (float) - holder.itemView.getLeft();
            holder.containerView.setTranslationX(Math.max(offset,0f));
            holder.containerView.setTranslationZ(offset < 0 ? 2f : 0);
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus) {
                listener.onEventSelected(channel, event);
            }
        });

        holder.itemView.setOnClickListener(v -> listener.onEventClicked(channel, event));

        //if(ChannelUtils.getLastSelectedChannel(context) == channel.number && )
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    @Override
    public long getItemId(int position) {
        return eventList.get(position).id;
    }

    public void loadEvents() {
        if(eventList == null) {
            eventList = new ArrayList<>();
        }

        List<EpgUtils.EpgEvent> fetchedEvents = new ArrayList<>(EpgUtils.getAllEvents(context, channel.number).values());

        // Filter & Sortieren nach Startzeit
        fetchedEvents = fetchedEvents.stream()
                .filter(entry -> entry.getEndLocalDateTime().isAfter(initTime))
                .sorted(Comparator.comparingLong(o -> o.startTime))
                .collect(Collectors.toList());

        eventList.clear();

        if(!fetchedEvents.isEmpty()) {
            EpgUtils.EpgEvent firstEvent = fetchedEvents.get(0);
            if(firstEvent.getStartLocalDateTime().isAfter(initTime)) {
                eventList.add(EpgUtils.EpgEvent.createEmptyEvent(context, initTime.toEpochSecond(ZoneOffset.UTC), firstEvent.startTime - initTime.toEpochSecond(ZoneOffset.UTC)));
            }
        }
        long lastEndTime = 0;
        for (EpgUtils.EpgEvent event : fetchedEvents) {
            // Erste Event-Zeit setzen
            if (lastEndTime == 0) {
                lastEndTime = event.startTime;
            }

            // Prüfen, ob eine Lücke vorhanden ist
            if (event.startTime > lastEndTime) {
                long gapDuration = event.startTime - lastEndTime;

                eventList.add(EpgUtils.EpgEvent.createEmptyEvent(context, lastEndTime, gapDuration));
            }

            // Aktuelles Event hinzufügen
            eventList.add(event);

            // Endzeit aktualisieren
            lastEndTime = event.startTime + event.duration;
        }
        eventList.add(EpgUtils.EpgEvent.createEmptyEvent(context, Long.min(lastEndTime, System.currentTimeMillis() / 1000), Long.MAX_VALUE / 2));
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView eventTitle;
        TextView eventTime;
        View containerView;

        public EventViewHolder(View itemView) {
            super(itemView);
            eventTitle = itemView.findViewById(R.id.event_title);
            eventTime = itemView.findViewById(R.id.event_time);
            containerView = itemView.findViewById(R.id.event_container);
        }
    }

    public static interface OnEventListener {
        void onEventSelected(ChannelUtils.Channel channel, EpgUtils.EpgEvent event);
        void onEventClicked(ChannelUtils.Channel channel, EpgUtils.EpgEvent event);
    }
}
