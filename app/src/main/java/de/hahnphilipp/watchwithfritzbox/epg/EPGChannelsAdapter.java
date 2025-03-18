package de.hahnphilipp.watchwithfritzbox.epg;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EPGLinearLayoutManager;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;
import de.hahnphilipp.watchwithfritzbox.utils.FocusRecyclerView;

public class EPGChannelsAdapter extends RecyclerView.Adapter<EPGChannelsAdapter.ChannelViewHolder> {

    private List<ChannelUtils.Channel> channels;
    private List<ArrayList<EpgUtils.EpgEvent>> eventList;
    private EPGOverlay epgOverlay;
    private LocalDateTime initTime;
    private EPGEventsAdapter.OnEventListener listener;

    public HashSet<RecyclerView> allEventRecyclerViews = new HashSet<>();


    public EPGChannelsAdapter(EPGOverlay epgOverlay, List<ChannelUtils.Channel> channels, LocalDateTime initTime, EPGEventsAdapter.OnEventListener listener) {
        this.channels = channels;
        this.epgOverlay = epgOverlay;
        this.initTime = initTime;
        this.listener = listener;
        this.eventList = new ArrayList<>();
        for(ChannelUtils.Channel channel : channels) {
            eventList.add(new ArrayList<>());
            loadEvents(channel);
        }
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.epg_channel_row, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        ChannelUtils.Channel channel = channels.get(position);
        holder.channelName.setText(channel.title);
        holder.channelIcon.setVisibility(View.INVISIBLE);
        holder.channelName.setVisibility(View.VISIBLE);

        Glide.with(epgOverlay.requireContext())
                .load(Uri.parse(ChannelUtils.getIconURL(channel)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, @Nullable Object model, @NonNull Target<Drawable> target, boolean isFirstResource) {
                        holder.channelIcon.setVisibility(View.INVISIBLE);
                        holder.channelName.setVisibility(View.VISIBLE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model, Target<Drawable> target, @NonNull DataSource dataSource, boolean isFirstResource) {
                        holder.channelIcon.setVisibility(View.VISIBLE);
                        holder.channelName.setVisibility(View.INVISIBLE);
                        return false;
                    }
                })
                .into(holder.channelIcon);

        // Set Adapter für horizontale RecyclerView
        EPGEventsAdapter eventAdapter = new EPGEventsAdapter(epgOverlay.requireContext(), epgOverlay, channel, eventList.get(position), initTime, new EPGEventsAdapter.OnEventListener() {
            @Override
            public void onEventSelected(ChannelUtils.Channel channel, EpgUtils.EpgEvent event) {
                listener.onEventSelected(channel, event);
                holder.channelCard.setActivated(true);
            }

            @Override
            public void onEventDeselected(ChannelUtils.Channel channel, EpgUtils.EpgEvent event) {
                listener.onEventDeselected(channel, event);
                holder.channelCard.setActivated(false);
            }

            @Override
            public void onEventClicked(ChannelUtils.Channel channel, EpgUtils.EpgEvent event) {
                listener.onEventClicked(channel, event);
            }
        });
        holder.eventRecyclerView.setAdapter(eventAdapter);
        holder.eventRecyclerView.setItemViewCacheSize(10);
        holder.eventRecyclerView.customEpgHorizontalFocusSearch = true;

        // RecyclerView zum Tracking hinzufügen
        allEventRecyclerViews.add(holder.eventRecyclerView);

        // Start-Position synchronisieren
        holder.eventRecyclerView.scrollBy(epgOverlay.currentScrollX, 0);

        // Scroll-Listener setzen
        holder.eventRecyclerView.addOnScrollListener(epgOverlay.syncScrollListener);
    }

    public void loadEvents(ChannelUtils.Channel channel) {
        int channelIndex = channels.indexOf(channel);
        List<EpgUtils.EpgEvent> eventList = this.eventList.get(channelIndex);
        if(eventList == null) {
            eventList = new ArrayList<>();
        }

        List<EpgUtils.EpgEvent> fetchedEvents = new ArrayList<>(EpgUtils.getAllEvents(epgOverlay.requireContext(), channel.number).values());

        // Filter & Sortieren nach Startzeit
        fetchedEvents = fetchedEvents.stream()
                .filter(entry -> entry.getEndLocalDateTime().isAfter(initTime))
                .sorted(Comparator.comparingLong(o -> o.startTime))
                .collect(Collectors.toList());

        eventList.clear();

        if(fetchedEvents.size() == 1) {
            EpgUtils.EpgEvent firstEvent = fetchedEvents.get(0);
            if(firstEvent.getStartLocalDateTime().isAfter(initTime)) {
                eventList.add(EpgUtils.EpgEvent.createEmptyEvent(epgOverlay.requireContext(), initTime.atZone(ZoneId.systemDefault()).toEpochSecond(), initTime.until(firstEvent.getStartLocalDateTime(), ChronoUnit.SECONDS)));
            }
        }
        LocalDateTime lastEndTime = initTime;
        for (EpgUtils.EpgEvent event : fetchedEvents) {
            // Prüfen, ob eine Lücke vorhanden ist
            if (event.getStartLocalDateTime().isAfter(lastEndTime)) {
                long gapDuration = lastEndTime.until(event.getStartLocalDateTime(), ChronoUnit.SECONDS);

                eventList.add(EpgUtils.EpgEvent.createEmptyEvent(epgOverlay.requireContext(), lastEndTime.atZone(ZoneId.systemDefault()).toEpochSecond(), gapDuration));
            }

            // Aktuelles Event hinzufügen
            eventList.add(event);

            // Endzeit aktualisieren
            lastEndTime = event.getEndLocalDateTime();
        }
        eventList.add(EpgUtils.EpgEvent.createEmptyEvent(epgOverlay.requireContext(), lastEndTime.atZone(ZoneId.systemDefault()).toEpochSecond(), Long.MAX_VALUE / 2));
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    @Override
    public void onViewRecycled(@NonNull final ChannelViewHolder holder) {
        Glide.with(epgOverlay.requireContext()).clear(holder.channelIcon);
        holder.channelIcon.setImageDrawable(null);
        allEventRecyclerViews.remove(holder.eventRecyclerView);
        holder.eventRecyclerView.removeOnScrollListener(epgOverlay.syncScrollListener);
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView channelIcon;
        TextView channelName;
        FocusRecyclerView eventRecyclerView;
        CardView channelCard;
        EPGLinearLayoutManager layoutManager;

        public ChannelViewHolder(View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channel_name);
            channelIcon = itemView.findViewById(R.id.channel_icon);
            eventRecyclerView = itemView.findViewById(R.id.event_recycler);
            channelCard = itemView.findViewById(R.id.epg_channel_cardView);
            layoutManager = new EPGLinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false);

            eventRecyclerView.setLayoutManager(layoutManager);
        }
    }
}
