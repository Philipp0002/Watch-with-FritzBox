package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
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
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tv_overlay_settings_item, parent, false);
        return new SettingViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final SettingViewHolder holder, final int position) {

        final TVSetting item = objects.get(position);

        holder.settingName.setText(item.name);

        holder.settingIcon.setImageResource(item.drawableId);
        holder.cardView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                AnimationUtils.scaleView(holder.mainView, 1F, 1.025F, 1F, 1.025F, 100L);
                holder.mainView.setElevation(12);
            } else {
                AnimationUtils.scaleView(holder.mainView, 1.025F, 1F, 1.025F, 1F, 20L);
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

