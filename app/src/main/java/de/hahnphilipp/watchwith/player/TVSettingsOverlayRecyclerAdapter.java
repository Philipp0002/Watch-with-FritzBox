package de.hahnphilipp.watchwith.player;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.utils.TVSetting;


public class TVSettingsOverlayRecyclerAdapter extends RecyclerView.Adapter<TVSettingsOverlayRecyclerAdapter.SettingViewHolder> {

    public List<TVSetting> objects;
    private Context context;
    private boolean firstItemSelected = false;

    public TVSettingsOverlayRecyclerAdapter(Context context, List<TVSetting> objects) {
        this.objects = objects;
        this.context = context;
    }


    @Override public SettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = null;
        if(viewType == 0) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_overlay_settings_item_big, parent, false);
        }else{
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_overlay_settings_item_small, parent, false);
        }
        return new SettingViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        if(objects.get(position).bigLayout){
            return 0;
        }else{
            return 1;
        }
    }

    @Override public void onBindViewHolder(final SettingViewHolder holder, final int position) {


        //holder.setIsRecyclable(false);

        final TVSetting item = (TVSetting) objects.get(position);

        holder.settingName.setText(item.name);

        holder.settingIcon.setImageResource(item.drawableId);
        holder.cardView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    ((CardView)v).setCardBackgroundColor(Color.WHITE);
                    holder.settingName.setTextColor(Color.BLACK);
                    ImageViewCompat.setImageTintMode(holder.settingIcon, PorterDuff.Mode.SRC_ATOP);
                    ImageViewCompat.setImageTintList(holder.settingIcon, ColorStateList.valueOf(Color.parseColor("#2a2939")));
                    scaleView(holder.mainView, 1F, 1.05F, 1F, 1.05F);
                    holder.mainView.setElevation(12);

                }else{
                    ((CardView)v).setCardBackgroundColor(Color.parseColor("#2a2939"));
                    holder.settingName.setTextColor(Color.WHITE);
                    ImageViewCompat.setImageTintMode(holder.settingIcon, PorterDuff.Mode.SRC_ATOP);
                    ImageViewCompat.setImageTintList(holder.settingIcon, ColorStateList.valueOf(Color.parseColor("#c4c3c8")));
                    scaleView(holder.mainView, 1.05F, 1F, 1.05F, 1F);
                    holder.mainView.setElevation(1);
                }
            }
        });

        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                item.onClick.run();
            }
        });

        if(!firstItemSelected){
            holder.cardView.requestFocus();
            firstItemSelected = true;
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

    public static class SettingViewHolder extends RecyclerView.ViewHolder {

        public ImageView settingIcon;
        public TextView settingName;
        public View mainView;
        public CardView cardView;

        public SettingViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;
            settingName = itemView.findViewById(R.id.tvoverlaysetting_name);
            settingIcon = itemView.findViewById(R.id.tvoverlaysetting_logo);
            cardView = itemView.findViewById(R.id.tvoverlaysetting_cardView);

        }
    }


}

