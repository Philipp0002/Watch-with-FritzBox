package de.hahnphilipp.watchwithfritzbox.epg;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.egeniq.androidtvprogramguide.ProgramGuideFragment;
import com.egeniq.androidtvprogramguide.entity.ProgramGuideSchedule;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.ZoneOffset;

import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVPlayerActivity;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGFragment extends ProgramGuideFragment<EpgUtils.EpgEvent> {

    public TVPlayerActivity context;

    @Override
    public boolean isTopMenuVisible() {
        return false;
    }

    @Override
    protected @NotNull Locale getDISPLAY_LOCALE() {
        return Locale.getDefault();
    }

    @Override
    protected @NotNull ZoneId getDISPLAY_TIMEZONE() {
        return ZoneId.systemDefault();
    }

    @Override
    public void requestingProgramGuideFor(@NotNull LocalDate localDate) {
        AsyncTask.execute(() -> {
            ArrayList<ChannelUtils.Channel> channels = new ArrayList<>(ChannelUtils.getAllChannels(context));
            HashMap<Integer, List<ProgramGuideSchedule<EpgUtils.EpgEvent>>> channelEpgMap = new HashMap<>();
            for (ChannelUtils.Channel channel : channels) {
                List<ProgramGuideSchedule<EpgUtils.EpgEvent>> schedules = EpgUtils.getAllEvents(context, channel.number)
                        .stream()
                        .map(event -> ProgramGuideSchedule.Companion.createScheduleWithProgram(
                                event.id,
                                Instant.ofEpochSecond(event.startTime),
                                Instant.ofEpochSecond(event.startTime + event.duration),
                                false,
                                event.title,
                                event
                        ))
                        .collect(Collectors.toList());
                channelEpgMap.put(channel.number, schedules);
            }
            requireActivity().runOnUiThread(() -> {
                setData(channels, channelEpgMap, localDate);
                setState(State.Content.INSTANCE);
            });
        });
    }

    @Override
    public void requestRefresh() {
        requestingProgramGuideFor(getCurrentDate());
    }

    private void updateDetailView(ProgramGuideSchedule<EpgUtils.EpgEvent> programGuideSchedule) {
        if (getView() == null) return;
        TextView channelNameView = getView().findViewById(R.id.epgchanneltitle);
        TextView titleView = getView().findViewById(R.id.epgtitle);
        TextView metadataView = getView().findViewById(R.id.epgsubtitle);
        TextView descriptionView = getView().findViewById(R.id.epgdescription);
        TextView timeView = getView().findViewById(R.id.epgtime);

        if(programGuideSchedule != null && !programGuideSchedule.isGap()) {
            EpgUtils.EpgEvent epgEvent = programGuideSchedule.getProgram();
            if (epgEvent == null) return;
            ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(requireContext(), epgEvent.channelNumber);
            if (channel == null) return;

            LocalDateTime startTime = epgEvent.getStartLocalDateTime();
            LocalDateTime endTime = epgEvent.getEndLocalDateTime();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault());
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());

            ArrayList<String> timeInfos = new ArrayList<>();
            if (epgEvent.duration < Long.MAX_VALUE / 2) {
                long durationMin = epgEvent.duration / 60;
                timeInfos.add(durationMin + " min");
            }
            if (endTime == null) {
                timeInfos.add(context.getString(R.string.epg_starting_from, startTime.format(timeFormatter)));
            } else {
                if (startTime.toLocalDate().equals(endTime.toLocalDate())) {
                    timeInfos.add(startTime.format(timeFormatter) + " - " + endTime.format(timeFormatter));
                } else {
                    timeInfos.add(startTime.format(dateTimeFormatter) + " - " + endTime.format(dateTimeFormatter));
                }
            }

            channelNameView.setText(channel.title);
            titleView.setText(epgEvent.title);
            metadataView.setText(epgEvent.subtitle);
            descriptionView.setText(epgEvent.description);
            timeView.setText(timeInfos.stream().collect(Collectors.joining(" | ")));
        } else {
            channelNameView.setText("");
            titleView.setText(R.string.epg_no_program);
            metadataView.setText(R.string.epg_no_program_load_info);
            descriptionView.setText(R.string.epg_no_program);
            timeView.setText("-");
        }
    }

    @Override
    public void onScheduleSelected(@org.jetbrains.annotations.Nullable ProgramGuideSchedule<EpgUtils.EpgEvent> programGuideSchedule) {
        updateDetailView(programGuideSchedule);
    }

    @Override
    public void onScheduleClicked(@NotNull ProgramGuideSchedule<EpgUtils.EpgEvent> programGuideSchedule) {

    }
}
