package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.graphics.PointF;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.ArrayList;
import java.util.Collections;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.AnimationUtils;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;


public class ChannelListRecyclerAdapter extends RecyclerView.Adapter<ChannelListRecyclerAdapter.ChannelInfoViewHolder> {

    public ArrayList<ChannelUtils.Channel> objects;
    private final Fragment context;
    public int selectedChannel = -1;
    private final RecyclerView recyclerView;
    private final boolean editMode;

    public ChannelListRecyclerAdapter(Fragment context, ArrayList<ChannelUtils.Channel> objects, RecyclerView recyclerView, boolean editMode) {
        this.objects = objects;
        this.context = context;
        this.recyclerView = recyclerView;
        this.editMode = editMode;
    }

    @NonNull
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
    public void onBindViewHolder(final ChannelInfoViewHolder holder, int position) {
        updateView(position, holder);
    }

    public void updateView(int indexPos) {
        ChannelInfoViewHolder holder = (ChannelInfoViewHolder) recyclerView.findViewHolderForAdapterPosition(indexPos);
        if (holder != null) {
            updateView(indexPos, holder);
        }
    }

    public void updateView(int indexPos, ChannelInfoViewHolder holder) {
        final ChannelUtils.Channel item = objects.get(indexPos);
        holder.channelName.setText(item.title);
        holder.channelNumber.setText("CH " + item.number);

        if (item.type == ChannelUtils.ChannelType.HD) {
            holder.channelTypeIcon.setImageResource(R.drawable.high_definition);
        } else if (item.type == ChannelUtils.ChannelType.SD) {
            holder.channelTypeIcon.setImageResource(R.drawable.standard_definition);
        } else if (item.type == ChannelUtils.ChannelType.RADIO) {
            holder.channelTypeIcon.setImageResource(R.drawable.radio_tower);
        } else if (item.type == ChannelUtils.ChannelType.OTHER) {
            holder.channelTypeIcon.setImageResource(R.drawable.round_feed);
        }

        holder.channelLockedIcon.setVisibility(item.free ? View.GONE : View.VISIBLE);

        Glide.with(context)
                .load(Uri.parse(ChannelUtils.getIconURL(item)))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.channelIcon);

        if (editMode) {
            holder.channelProgramNow.setVisibility(View.GONE);

            holder.itemView.setOnKeyListener((view, i, keyEvent) -> {
                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && (i == KeyEvent.KEYCODE_DPAD_UP || i == KeyEvent.KEYCODE_DPAD_DOWN)) {
                    if (selectedChannel != -1) {
                        int direction = (i == KeyEvent.KEYCODE_DPAD_UP) ? -1 : 1;

                        int pos = holder.getBindingAdapterPosition();

                        if ((pos + direction < 0 || pos + direction >= objects.size())) {
                            return true;
                        }
                        swapItems(pos, pos + direction);

                        ChannelUtils.Channel a = objects.get(pos);
                        ChannelUtils.Channel b = objects.get(pos + direction);
                        ChannelUtils.moveChannelToPosition(context.getContext(), a.number, b.number);
                        recyclerView.post(() -> {
                            updateView(pos);
                            updateView(pos + direction);
                        });
                        return true;
                    }
                }
                return false;
            });

            holder.itemView.setOnClickListener(v -> {
                EditChannelListTVOverlay overlay = (EditChannelListTVOverlay) context;
                if (selectedChannel == -1) {
                    selectedChannel = item.number;
                    overlay.showSidepanel(1);
                } else {
                    selectedChannel = -1;
                    overlay.showSidepanel(0);
                }
            });

        } else {
            EpgUtils.EpgEvent eventNow = EpgUtils.getEventNowFromCache(item.number);
            if (eventNow != null) {
                holder.channelProgramNow.setText(eventNow.title);
                holder.channelProgramNow.setVisibility(View.VISIBLE);
            } else {
                holder.channelProgramNow.setText("");
                holder.channelProgramNow.setVisibility(View.GONE);
            }

            holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(holder.getBindingAdapterPosition());
                    }
                    holder.cardView.setElevation(12);
                    AnimationUtils.scaleView(holder.cardView, 1F, 1.025F, 1F, 1.025F, 100L);
                    holder.channelProgramNow.setSelected(true);
                } else {
                    AnimationUtils.scaleView(holder.cardView, 1.025F, 1F, 1.025F, 1F, 20L);
                    holder.cardView.setElevation(0);
                    holder.channelProgramNow.setSelected(false);
                }
            });

            holder.itemView.setOnClickListener(v -> {
                ChannelListTVOverlay overlay = (ChannelListTVOverlay) context;
                overlay.context.popOverlayFragment();
                ChannelUtils.updateLastSelectedChannel(((ChannelListTVOverlay) context).context, item.number);
                overlay.context.launchPlayer(false);
            });
        }

        if (selectedChannel == item.number && !editMode) {
            holder.itemView.requestFocus();
            selectedChannel = -1;
        }
    }

    public void swapItems(int fromIndexPosition, int toIndexPosition) {
        Collections.swap(objects, fromIndexPosition, toIndexPosition);
        notifyItemMoved(fromIndexPosition, toIndexPosition);
    }

    public void selectChannel(int channelNumber) {
        selectedChannel = channelNumber;
        recyclerView.scrollToPosition(channelNumber - 1);
    }

    @Override
    public int getItemCount() {
        return objects.size();
    }

    public static class ChannelInfoViewHolder extends RecyclerView.ViewHolder {

        public ImageView channelIcon;
        public ImageView channelTypeIcon;
        public ImageView channelLockedIcon;
        public TextView channelName;
        public TextView channelNumber;
        public ConstraintLayout channelNumberLayout;
        public View mainView;
        public CardView cardView;
        public TextView channelProgramNow;

        public ChannelInfoViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;
            channelName = itemView.findViewById(R.id.tvoverlaychannel_name);
            channelProgramNow = itemView.findViewById(R.id.tvoverlaychannel_event_now);
            channelNumber = itemView.findViewById(R.id.tvoverlaychannel_number);
            channelNumberLayout = itemView.findViewById(R.id.tvoverlaychannel_number_layout);
            channelIcon = itemView.findViewById(R.id.tvoverlaychannel_logo);
            channelTypeIcon = itemView.findViewById(R.id.tvoverlaychannel_type);
            channelLockedIcon = itemView.findViewById(R.id.tvoverlaychannel_locked);
            cardView = itemView.findViewById(R.id.tvoverlaychannel_cardView);

        }
    }

}

