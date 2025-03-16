package de.hahnphilipp.watchwithfritzbox.epg;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGTimeSlotAdapter extends RecyclerView.Adapter<EPGTimeSlotAdapter.TimeSlotViewHolder> {
    private LocalDateTime initTime;

    public EPGTimeSlotAdapter(LocalDateTime initTime) {
        this.initTime = initTime;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.epg_time_slot_item, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        LocalDateTime time = initTime.plusHours(position);

        int seconds = 60 * 60;
        /*if (time.isBefore(LocalDateTime.now())) {
            seconds = (int) (LocalDateTime.now().until(time, java.time.temporal.ChronoUnit.SECONDS));
            time = LocalDateTime.now();
        }*/

        holder.itemView.setLayoutParams(new LinearLayout.LayoutParams(EpgUtils.secondsToPx(seconds), ViewGroup.LayoutParams.WRAP_CONTENT));
        holder.timeLabel.setText(
                DateTimeFormatter
                        .ofLocalizedTime(FormatStyle.SHORT)
                        .withLocale(Locale.getDefault())
                        .format(time)
        );
    }

    @Override
    public int getItemCount() {
        return Integer.MAX_VALUE;
    }

    static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeLabel;

        public TimeSlotViewHolder(View itemView) {
            super(itemView);
            timeLabel = itemView.findViewById(R.id.time_label);
        }
    }
}
