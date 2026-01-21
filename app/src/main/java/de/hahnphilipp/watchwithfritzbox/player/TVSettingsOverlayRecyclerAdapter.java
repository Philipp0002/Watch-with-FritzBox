package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.AnimationUtils;
import de.hahnphilipp.watchwithfritzbox.utils.CustomTVSetting;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;


public class TVSettingsOverlayRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public List<Object> objects;
    private Context context;
    private RecyclerView recyclerView;
    private boolean firstItemSelected = false;

    public TVSettingsOverlayRecyclerAdapter(Context context, List<Object> objects, RecyclerView recyclerView) {
        this.objects = objects;
        this.context = context;
        this.recyclerView = recyclerView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == 0) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.overlay_settings_item, parent, false);
            return new SettingViewHolder(v);
        } else if(viewType == 1) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.overlay_settings_title, parent, false);
            return new TitleViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(viewType - 2, parent, false);
            return new CustomViewHolder(v);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if(objects.get(position) instanceof TVSetting) {
            if(objects.get(position) instanceof CustomTVSetting customSetting) {
                return 2 + customSetting.customLayoutRes;
            }
            return 0;
        } else {
            return 1;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder _holder, final int position) {
        if(_holder instanceof TitleViewHolder holder) {
            holder.titleView.setText((String) objects.get(position));
        } else if(_holder instanceof SettingViewHolder holder) {
            final TVSetting item = (TVSetting) objects.get(position);

            holder.settingName.setText(item.name);

            if(item.drawableId != null) {
                holder.settingIcon.setImageResource(item.drawableId);
            } else {
                holder.settingIcon.setImageDrawable(null);
            }
            holder.cardView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    AnimationUtils.scaleView(holder.mainView, 1F, 1.025F, 1F, 1.025F, 100L);
                    holder.mainView.setElevation(12);
                    for(Object object : objects) {
                        if(object instanceof TVSetting otherItem) {
                            if(otherItem == item) {
                                recyclerView.post(() -> recyclerView.smoothScrollBy(0, -500));
                            }
                            break;
                        }
                    }
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
        } else if(_holder instanceof CustomViewHolder holder) {
            final CustomTVSetting item = (CustomTVSetting) objects.get(position);
            if(item.layoutCallback != null) {
                item.layoutCallback.onBindView(holder.mainView);
            }
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

    public static class TitleViewHolder extends RecyclerView.ViewHolder {

        public TextView titleView;
        public View mainView;

        public TitleViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;
            titleView = itemView.findViewById(R.id.tvoverlaysetting_title);

        }
    }

    public static class CustomViewHolder extends RecyclerView.ViewHolder {

        public View mainView;

        public CustomViewHolder(View itemView) {
            super(itemView);
            mainView = itemView;

        }
    }


}

