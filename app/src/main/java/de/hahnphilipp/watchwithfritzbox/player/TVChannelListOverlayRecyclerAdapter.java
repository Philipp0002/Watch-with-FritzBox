package de.hahnphilipp.watchwithfritzbox.player;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.AnimationUtils;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;


public class TVChannelListOverlayRecyclerAdapter extends RecyclerView.Adapter<TVChannelListOverlayRecyclerAdapter.ChannelInfoViewHolder> {

    public ArrayList<ChannelUtils.Channel> objects;
    private Fragment context;
    public int selectedChannel = -1;
    private RecyclerView recyclerView;

    int focus = 0;

    public TVChannelListOverlayRecyclerAdapter(Fragment context, ArrayList<ChannelUtils.Channel> objects, RecyclerView recyclerView) {
        this.objects = objects;
        this.context = context;
        this.recyclerView = recyclerView;
    }


    @Override
    public ChannelInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.overlay_channel_list_item, parent, false);
        return new ChannelInfoViewHolder(v);
    }

    @Override
    public void onViewRecycled(@NonNull final ChannelInfoViewHolder holder) {
        Glide.with(context).clear(holder.channelIcon);
        holder.channelIcon.setImageDrawable(null);
    }

    @Override
    public void onBindViewHolder(final ChannelInfoViewHolder holder, final int position) {
        //holder.setIsRecyclable(false);
        final ChannelUtils.Channel item = objects.get(position);
        holder.channelName.setText(item.title);
        holder.channelNumber.setText("CH " + item.number);

        if (item.type == ChannelUtils.ChannelType.HD) {
            holder.channelTypeIcon.setImageResource(R.drawable.high_definition);
        } else if (item.type == ChannelUtils.ChannelType.SD) {
            holder.channelTypeIcon.setImageResource(R.drawable.standard_definition);
        } else if (item.type == ChannelUtils.ChannelType.RADIO) {
            holder.channelTypeIcon.setImageResource(R.drawable.radio_tower);
        }

        EpgUtils.EpgEvent event = EpgUtils.getEventAtTime(context.getContext(), item.number, System.currentTimeMillis() / 1000);
        if (event != null) {
            holder.channelEvent.setText(event.title);
            holder.channelEvent.setVisibility(View.VISIBLE);
        } else {
            holder.channelEvent.setText("");
            holder.channelEvent.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(Uri.parse(ChannelUtils.getIconURL(item)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.channelIcon);

        holder.cardView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (!(context instanceof EditChannelListTVOverlay)) {
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(position);
                    }
                    holder.mainView.setElevation(12);
                    AnimationUtils.scaleView(holder.mainView, 1F, 1.025F, 1F, 1.025F, 100L);
                    focus = position;
                }
                holder.channelEvent.setTextColor(Color.parseColor("#52525a"));
                holder.channelNumber.setTextColor(Color.parseColor("#52525a"));
                holder.channelNumberLayout.setBackgroundResource(R.drawable.channel_number_outline_black);
                holder.channelTypeIcon.setBackgroundResource(R.drawable.channel_quality_bg_black);
                ImageViewCompat.setImageTintMode(holder.channelTypeIcon, PorterDuff.Mode.SRC_ATOP);
                ImageViewCompat.setImageTintList(holder.channelTypeIcon, ColorStateList.valueOf(Color.parseColor("#ffffff")));


                if (context instanceof EditChannelListTVOverlay) {
                    if (item.number != selectedChannel && selectedChannel != -1) {
                        ChannelUtils.moveChannelToPosition(context.getContext(), selectedChannel, item.number);
                        selectedChannel = item.number;
                        ((EditChannelListTVOverlay) context).updateChannelList();
                    }

                }
            } else {
                holder.channelEvent.setTextColor(Color.parseColor("#c4c3c8"));
                holder.channelNumber.setTextColor(Color.parseColor("#c4c3c8"));
                holder.channelNumberLayout.setBackgroundResource(R.drawable.channel_number_outline_white);
                holder.channelTypeIcon.setBackgroundResource(R.drawable.channel_quality_bg_white);
                ImageViewCompat.setImageTintMode(holder.channelTypeIcon, PorterDuff.Mode.SRC_ATOP);
                ImageViewCompat.setImageTintList(holder.channelTypeIcon, ColorStateList.valueOf(Color.parseColor("#2a2939")));

                if (!(context instanceof EditChannelListTVOverlay)) {
                    AnimationUtils.scaleView(holder.mainView, 1.025F, 1F, 1.025F, 1F, 20L);
                    holder.mainView.setElevation(3);
                }
            }
        });

        holder.cardView.setOnClickListener(v -> {
            if (context instanceof ChannelListTVOverlay) {
                ChannelListTVOverlay overlay = (ChannelListTVOverlay) context;
                overlay.context.popOverlayFragment();
                ChannelUtils.updateLastSelectedChannel(((ChannelListTVOverlay) context).context, item.number);
                overlay.context.launchPlayer(false);
            } else if (context instanceof EditChannelListTVOverlay) {
                EditChannelListTVOverlay overlay = (EditChannelListTVOverlay) context;
                TextView editChannelListInfoText = overlay.getView().findViewById(R.id.editChannelListInfoText);
                TextView editChannelListInfoText2 = overlay.getView().findViewById(R.id.editChannelListInfoText2);
                ImageView editChannelListInfoImage = overlay.getView().findViewById(R.id.editChannelListInfoImage);
                if (selectedChannel == -1) {
                    selectedChannel = item.number;
                    editChannelListInfoText.setText(R.string.settings_reorder_channels_move);
                    editChannelListInfoText2.setText("");
                    editChannelListInfoImage.setImageResource(R.drawable.round_swap_vert);
                } else {
                    selectedChannel = -1;
                    editChannelListInfoText.setText(R.string.settings_reorder_channels_select);
                    if (overlay.ip != null) {
                        editChannelListInfoText2.setText(context.getResources().getString(R.string.settings_reorder_channels_webserver_info).replace("%d", overlay.ip));
                    }
                    editChannelListInfoImage.setImageResource(R.drawable.round_touch_app);
                }
            }
        });

        if (selectedChannel == item.number) {
            holder.cardView.requestFocus();

            if (context instanceof ChannelListTVOverlay) {
                selectedChannel = -1;
            }
        }
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    public static class ChannelInfoViewHolder extends RecyclerView.ViewHolder {

        public ImageView channelIcon;
        public ImageView channelTypeIcon;
        public TextView channelName;
        public TextView channelEvent;
        public TextView channelNumber;
        public ConstraintLayout channelNumberLayout;
        public View mainView;
        public CardView cardView;

        public ChannelInfoViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;
            channelName = itemView.findViewById(R.id.tvoverlaychannel_name);
            channelEvent = itemView.findViewById(R.id.tvoverlaychannel_event);
            channelNumber = itemView.findViewById(R.id.tvoverlaychannel_number);
            channelNumberLayout = itemView.findViewById(R.id.tvoverlaychannel_number_layout);
            channelIcon = itemView.findViewById(R.id.tvoverlaychannel_logo);
            channelTypeIcon = itemView.findViewById(R.id.tvoverlaychannel_type);
            cardView = itemView.findViewById(R.id.tvoverlaychannel_cardView);

        }
    }


}

