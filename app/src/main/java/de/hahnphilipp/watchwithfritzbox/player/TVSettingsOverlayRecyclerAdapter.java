package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.AnimationUtils;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;


public class TVSettingsOverlayRecyclerAdapter extends RecyclerView.Adapter<TVSettingsOverlayRecyclerAdapter.SettingViewHolder> {

    public List<TVSetting> objects;
    private Context context;
    private boolean firstItemSelected = false;

    public TVSettingsOverlayRecyclerAdapter(Context context, List<TVSetting> objects) {
        this.objects = objects;
        this.context = context;
    }

    @Override
    public SettingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == 0) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_overlay_settings_item_big, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_overlay_settings_item_small, parent, false);
        }
        return new SettingViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        if (objects.get(position).bigLayout) {
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(final SettingViewHolder holder, final int position) {
        //holder.setIsRecyclable(false);

        final TVSetting item = (TVSetting) objects.get(position);

        holder.settingName.setText(item.name);

        holder.settingIcon.setImageResource(item.drawableId);
        holder.cardView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                ((CardView) v).setCardBackgroundColor(Color.WHITE);
                holder.settingName.setTextColor(Color.BLACK);
                ImageViewCompat.setImageTintMode(holder.settingIcon, PorterDuff.Mode.SRC_ATOP);
                ImageViewCompat.setImageTintList(holder.settingIcon, ColorStateList.valueOf(Color.parseColor("#2a2939")));
                AnimationUtils.scaleView(holder.mainView, 1F, 1.05F, 1F, 1.05F, 100L);
                holder.mainView.setElevation(12);

            } else {
                ((CardView) v).setCardBackgroundColor(Color.parseColor("#2a2939"));
                holder.settingName.setTextColor(Color.WHITE);
                ImageViewCompat.setImageTintMode(holder.settingIcon, PorterDuff.Mode.SRC_ATOP);
                ImageViewCompat.setImageTintList(holder.settingIcon, ColorStateList.valueOf(Color.parseColor("#c4c3c8")));
                AnimationUtils.scaleView(holder.mainView, 1.05F, 1F, 1.05F, 1F, 20L);
                holder.mainView.setElevation(1);
            }
        });

        holder.cardView.setOnClickListener(v -> item.onClick.run());

        if (!firstItemSelected) {
            holder.cardView.requestFocus();
            firstItemSelected = true;
        }
    }

    @Override
    public int getItemCount() {
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

