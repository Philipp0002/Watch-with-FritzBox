package de.hahnphilipp.watchwithfritzbox.player;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;

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
                time.format(
                        new DateTimeFormatterBuilder()
                                .appendValue(HOUR_OF_DAY, 2)
                                .appendLiteral(':')
                                .appendValue(MINUTE_OF_HOUR, 2)
                                .toFormatter()
                )
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
