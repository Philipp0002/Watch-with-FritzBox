package de.hahnphilipp.watchwithfritzbox.epg;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGEventsAdapter extends RecyclerView.Adapter<EPGEventsAdapter.EventViewHolder> {
    private LocalDateTime initTime;
    private ChannelUtils.Channel channel;
    private ArrayList<EpgUtils.EpgEvent> eventList;
    private Context context;
    private OnEventListener listener;

    public EPGEventsAdapter(Context context, ChannelUtils.Channel channel, ArrayList<EpgUtils.EpgEvent> eventList, LocalDateTime initTime, OnEventListener listener) {
        this.channel = channel;
        this.context = context;
        this.initTime = initTime;
        this.listener = listener;
        this.eventList = eventList;
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
            duration -= initTime.toEpochSecond(ZoneOffset.UTC) - event.getStartLocalDateTime().toEpochSecond(ZoneOffset.UTC);
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
