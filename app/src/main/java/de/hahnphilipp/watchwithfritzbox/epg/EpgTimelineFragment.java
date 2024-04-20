package de.hahnphilipp.watchwithfritzbox.epg;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Spanned;
import android.text.SpannedString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.egeniq.androidtvprogramguide.ProgramGuideFragment;
import com.egeniq.androidtvprogramguide.entity.ProgramGuideChannel;
import com.egeniq.androidtvprogramguide.entity.ProgramGuideSchedule;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EpgTimelineFragment extends ProgramGuideFragment<EpgUtils.EpgEvent> {

    public EpgTimelineFragment() {
        // Required empty public constructor
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public boolean isTopMenuVisible() {
        return false;
    }

    @Override
    public void requestRefresh() {
        requestingProgramGuideFor(LocalDate.now());
    }

    @Override
    public void onScheduleSelected(@Nullable ProgramGuideSchedule<EpgUtils.EpgEvent> programGuideSchedule) {

    }

    @Override
    public void onScheduleClicked(@NonNull ProgramGuideSchedule<EpgUtils.EpgEvent> programGuideSchedule) {

    }

    @Override
    public void requestingProgramGuideFor(@NonNull LocalDate localDate) {
        setState(State.Loading.INSTANCE);

        ArrayList<ChannelUtils.Channel> channels = ChannelUtils.getAllChannels(getContext());

        List<ProgramGuideChannel> programGuideChannels =
                channels.stream().map(fritzChannel -> new ProgramGuideChannel() {
            @NonNull
            @Override
            public String getId() {
                return fritzChannel.number + "";
            }

            @Nullable
            @Override
            public Spanned getName() {
                return new SpannedString(fritzChannel.title);
            }

            @Nullable
            @Override
            public String getImageUrl() {
                return ChannelUtils.getIconURL(fritzChannel);
            }
        }).collect(Collectors.toList());

        HashMap<String, List<ProgramGuideSchedule<EpgUtils.EpgEvent>>> channelMap = new HashMap<>();

        for(ChannelUtils.Channel ch : channels) {
            List<ProgramGuideSchedule<EpgUtils.EpgEvent>> timelineEvents =
                    EpgUtils.getAllEvents(getContext(), ch.number).values().stream()
                    .map(event -> {
                        return ProgramGuideSchedule.Companion.createScheduleWithProgram(
                                event.id,
                                Instant.ofEpochSecond(event.startTime),
                                Instant.ofEpochSecond(event.startTime + event.duration),
                                false,
                                event.title,
                                event
                        );
                    }).collect(Collectors.toList());
            channelMap.put(ch.number + "", timelineEvents);
        }

        setData(programGuideChannels, channelMap,localDate);
        setState(State.Content.INSTANCE);

    }
}