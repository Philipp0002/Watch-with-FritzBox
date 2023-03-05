package de.hahnphilipp.watchwith.player;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.utils.ChannelUtils;


public class TVChannelListOverlayRecyclerAdapter extends RecyclerView.Adapter<TVChannelListOverlayRecyclerAdapter.ChannelInfoViewHolder> {

    public ArrayList<ChannelUtils.Channel> objects;
    private Fragment context;
    public Picasso picasso;
    public int selectedChannel = -1;

    public TVChannelListOverlayRecyclerAdapter(Fragment context, ArrayList<ChannelUtils.Channel> objects, Picasso picasso) {
        this.objects = objects;
        this.context = context;
        this.picasso = picasso;
    }


    @Override public ChannelInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_overlay_channel_item, parent, false);
        return new ChannelInfoViewHolder(v);
    }

    @Override public void onBindViewHolder(final ChannelInfoViewHolder holder, final int position) {


        //holder.setIsRecyclable(false);

        Log.d("OVERLA", position+"");

        final ChannelUtils.Channel item = (ChannelUtils.Channel) objects.get(position);
        holder.channelName.setText(item.title);
        holder.channelNumber.setText("CH "+item.number);

        if(item.type == ChannelUtils.ChannelType.HD){
            holder.channelTypeIcon.setImageResource(R.drawable.ic_high_definition);
        }else if(item.type == ChannelUtils.ChannelType.SD){
            holder.channelTypeIcon.setImageResource(R.drawable.ic_standard_definition);
        }else if(item.type == ChannelUtils.ChannelType.RADIO){
            holder.channelTypeIcon.setImageResource(R.drawable.ic_radio_tower);
        }

        Picasso.get()
                .load(Uri.parse("https://tv.avm.de/tvapp/logos/" +
                        (item.type == ChannelUtils.ChannelType.HD ? "hd/": (item.type == ChannelUtils.ChannelType.RADIO ? "radio/" : "")) +
                        item.title.toLowerCase().replace(" ", "_").replace("+", "") +
                        ".png"))
                .into(holder.channelIcon);

        holder.cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    if(context instanceof EditChannelListTVOverlay) {
                        if(item.number == selectedChannel) {
                            ((CardView) v).setCardBackgroundColor(Color.parseColor("#c7c7f2"));
                        }else{
                            ((CardView) v).setCardBackgroundColor(Color.parseColor("#f5f5f7"));
                        }
                    }else{
                        ((CardView) v).setCardBackgroundColor(Color.parseColor("#f5f5f7"));
                    }
                    holder.channelName.setTextColor(Color.BLACK);
                    holder.channelNumber.setTextColor(Color.parseColor("#52525a"));
                    holder.channelNumberLayout.setBackgroundResource(R.drawable.channel_number_outline_black);
                    holder.channelTypeIcon.setBackgroundResource(R.drawable.channel_quality_bg_black);
                    ImageViewCompat.setImageTintMode(holder.channelTypeIcon, PorterDuff.Mode.SRC_ATOP);
                    ImageViewCompat.setImageTintList(holder.channelTypeIcon, ColorStateList.valueOf(Color.parseColor("#ffffff")));

                    scaleView(holder.mainView, 1F, 1.05F, 1F, 1.05F);
                    holder.mainView.setElevation(12);

                    if(context instanceof EditChannelListTVOverlay) {
                        if(item.number != selectedChannel && selectedChannel != -1){
                           ChannelUtils.moveChannelToPosition(((EditChannelListTVOverlay) context).getContext(), selectedChannel, item.number);
                           selectedChannel = item.number;
                            ((EditChannelListTVOverlay) context).updateChannelList();
                        }

                    }
                }else{
                    ((CardView)v).setCardBackgroundColor(Color.parseColor("#2a2939"));
                    holder.channelName.setTextColor(Color.WHITE);
                    holder.channelNumber.setTextColor(Color.parseColor("#c4c3c8"));
                    holder.channelNumberLayout.setBackgroundResource(R.drawable.channel_number_outline_white);
                    holder.channelTypeIcon.setBackgroundResource(R.drawable.channel_quality_bg_white);
                    ImageViewCompat.setImageTintMode(holder.channelTypeIcon, PorterDuff.Mode.SRC_ATOP);
                    ImageViewCompat.setImageTintList(holder.channelTypeIcon, ColorStateList.valueOf(Color.parseColor("#2a2939")));

                    scaleView(holder.mainView, 1.05F, 1F, 1.05F, 1F);
                    holder.mainView.setElevation(1);
                }
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(context instanceof ChannelListTVOverlay) {
                    ((ChannelListTVOverlay) context).hideOverlays();
                    ChannelUtils.updateLastSelectedChannel(((ChannelListTVOverlay) context).context, item.number);
                    ((ChannelListTVOverlay) context).context.launchPlayer(false);
                }else if(context instanceof EditChannelListTVOverlay) {
                    if(selectedChannel == -1) {
                        selectedChannel = item.number;
                        ((CardView) v).setCardBackgroundColor(Color.parseColor("#c7c7f2"));
                        ((TextView)((EditChannelListTVOverlay) context).getView().findViewById(R.id.editChannelListInfoText)).setText(R.string.settings_reorder_channels_move);
                        ((TextView)((EditChannelListTVOverlay) context).getView().findViewById(R.id.editChannelListInfoText2)).setText("");
                        ((ImageView)((EditChannelListTVOverlay) context).getView().findViewById(R.id.editChannelListInfoImage)).setImageResource(R.drawable.ic_baseline_swap_vert_24);
                    }else{
                        selectedChannel = -1;
                        ((CardView) v).setCardBackgroundColor(Color.parseColor("#f5f5f7"));
                        ((TextView)((EditChannelListTVOverlay) context).getView().findViewById(R.id.editChannelListInfoText)).setText(R.string.settings_reorder_channels_select);
                        ((TextView)((EditChannelListTVOverlay) context).getView().findViewById(R.id.editChannelListInfoText2)).setText(context.getResources().getString(R.string.settings_reorder_channels_webserver_info).replace("%d",((EditChannelListTVOverlay) context).ip));
                        ((ImageView)((EditChannelListTVOverlay) context).getView().findViewById(R.id.editChannelListInfoImage)).setImageResource(R.drawable.ic_baseline_touch_app_24);
                    }
                }
            }
        });

        if(selectedChannel == item.number){
            holder.cardView.requestFocus();

            if(context instanceof ChannelListTVOverlay) {
                selectedChannel = -1;
            }
        }



    }


    public void scaleView(View v, float startScaleX, float endScaleX, float startScaleY, float endScaleY) {
        Animation anim = new ScaleAnimation(
                startScaleX, endScaleX, // Start and end values for the X axis scaling
                startScaleY, endScaleY, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        anim.setFillAfter(true); // Needed to keep the result of the animation
        anim.setFillEnabled(true);
        anim.setDuration(100);
        v.startAnimation(anim);
    }

    @Override public int getItemCount() {
        return objects.size();
    }

    public static class ChannelInfoViewHolder extends RecyclerView.ViewHolder {

        public ImageView channelIcon;
        public ImageView channelTypeIcon;
        public TextView channelName;
        public TextView channelNumber;
        public ConstraintLayout channelNumberLayout;
        public View mainView;
        public CardView cardView;

        public ChannelInfoViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;
            channelName = itemView.findViewById(R.id.tvoverlaychannel_name);
            channelNumber = itemView.findViewById(R.id.tvoverlaychannel_number);
            channelNumberLayout = itemView.findViewById(R.id.tvoverlaychannel_number_layout);
            channelIcon = itemView.findViewById(R.id.tvoverlaychannel_logo);
            channelTypeIcon = itemView.findViewById(R.id.tvoverlaychannel_type);
            cardView = itemView.findViewById(R.id.tvoverlaychannel_cardView);

        }
    }


}

