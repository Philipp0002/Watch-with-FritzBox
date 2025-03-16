package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class EPGChannelsAdapter extends RecyclerView.Adapter<EPGChannelsAdapter.ChannelViewHolder> {

    private List<ChannelUtils.Channel> channels;
    private EPGOverlay epgOverlay;
    private LocalDateTime initTime;
    private EPGEventsAdapter.OnEventListener listener;

    public HashSet<RecyclerView> allEventRecyclerViews = new HashSet<>();


    public EPGChannelsAdapter(EPGOverlay epgOverlay, List<ChannelUtils.Channel> channels, LocalDateTime initTime, EPGEventsAdapter.OnEventListener listener) {
        this.channels = channels;
        this.epgOverlay = epgOverlay;
        this.initTime = initTime;
        this.listener = listener;
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
        EPGEventsAdapter eventAdapter = new EPGEventsAdapter(epgOverlay.requireContext(), channel, initTime, listener);
        holder.eventRecyclerView.setAdapter(eventAdapter);
        holder.eventRecyclerView.setItemViewCacheSize(10);

        // RecyclerView zum Tracking hinzufügen
        if (!allEventRecyclerViews.contains(holder.eventRecyclerView)) {
            allEventRecyclerViews.add(holder.eventRecyclerView);
        }

        // Scroll-Listener setzen
        holder.eventRecyclerView.addOnScrollListener(epgOverlay.syncScrollListener);

        // Start-Position synchronisieren
        holder.eventRecyclerView.scrollBy(epgOverlay.currentScrollX, 0);
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    @Override
    public void onViewRecycled(@NonNull final ChannelViewHolder holder) {
        Glide.with(epgOverlay.requireContext()).clear(holder.channelIcon);
        holder.channelIcon.setImageDrawable(null);
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView channelIcon;
        TextView channelName;
        RecyclerView eventRecyclerView;

        public ChannelViewHolder(View itemView) {
            super(itemView);
            channelName = itemView.findViewById(R.id.channel_name);
            channelIcon = itemView.findViewById(R.id.channel_icon);
            eventRecyclerView = itemView.findViewById(R.id.event_recycler);


            eventRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        }
    }
}
