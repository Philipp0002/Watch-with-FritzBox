package de.hahnphilipp.watchwithfritzbox.epg;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.egeniq.androidtvprogramguide.ProgramGuideFragment;
import com.egeniq.androidtvprogramguide.entity.ProgramGuideSchedule;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.jetbrains.annotations.NotNull;
import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVPlayerActivity;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.EpgUtils;

public class EPGFragment extends ProgramGuideFragment<EpgUtils.EpgEvent> {

    public TVPlayerActivity context;
    public boolean wasClosed = true;
    private boolean isScrollingDown = true;

    private Runnable scrollRunnable;
    private Handler handler = new Handler(Looper.getMainLooper());
    // Konfiguration
    private static final long SCROLL_DELAY = 50; // Millisekunden zwischen Scroll-Schritten
    private static final int SCROLL_STEP = 2; // Pixel pro Schritt (kleiner = langsamer)
    private static final long PAUSE_AT_END = 2000; // Pause am Ende/Anfang in Millisekunden

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
        setState(State.Loading.INSTANCE);
        TextView loadingTextView = getView().findViewById(R.id.loading_text);
        CircularProgressIndicator loadingProgressIndicator = getView().findViewById(R.id.loading_indicator);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            ArrayList<ChannelUtils.Channel> channels = new ArrayList<>(ChannelUtils.getAllChannels(context));
            HashMap<Integer, List<ProgramGuideSchedule>> channelEpgMap = new HashMap<>();
            for (ChannelUtils.Channel channel : channels) {
                requireActivity().runOnUiThread(() -> {
                    loadingTextView.setText(getString(R.string.epg_loading_channels, channel.number, channels.size()));
                    loadingProgressIndicator.setIndeterminate(false);
                    loadingProgressIndicator.setMax(channels.size());
                    loadingProgressIndicator.setProgress(channel.number);
                });
                List<ProgramGuideSchedule> schedules = EpgUtils.getAllEvents(context, channel.number)
                        .stream()
                        .map(event -> ProgramGuideSchedule.Companion.createScheduleWithProgram(
                                Instant.ofEpochSecond(event.startTime),
                                Instant.ofEpochSecond(event.startTime + event.duration),
                                false,
                                event
                        ))
                        .collect(Collectors.toList());
                channelEpgMap.put(channel.number, schedules);
            }
            requireActivity().runOnUiThread(() -> {
                loadingTextView.setText(getString(R.string.epg_loading_channels_wait));
                setData(channels, channelEpgMap, localDate);
                getView().post(() -> {
                    setState(State.Content.INSTANCE);
                    scrollToChannelWithId(ChannelUtils.getLastSelectedChannel(requireContext()));
                });
            });
        });
    }

    @Override
    public void requestRefresh() {
        if(wasClosed) {
            wasClosed = false;
            requestingProgramGuideFor(getCurrentDate());
        }
    }

    private void updateDetailView
            (ProgramGuideSchedule programGuideSchedule) {
        if (getView() == null) return;
        View detailView = getView().findViewById(R.id.epgdetails);
        TextView channelNameView = getView().findViewById(R.id.epgchanneltitle);
        TextView titleView = getView().findViewById(R.id.epgtitle);
        TextView subtitleView = getView().findViewById(R.id.epgsubtitle);
        TextView descriptionView = getView().findViewById(R.id.epgdescription);
        ScrollView descriptionScrollView = getView().findViewById(R.id.epgdescriptionscroll);
        TextView timeView = getView().findViewById(R.id.epgtime);
        View timeWrapperView = getView().findViewById(R.id.epgtimewrapper);

        if (programGuideSchedule == null) {
            detailView.setVisibility(GONE);
            return;
        } else {
            detailView.setVisibility(VISIBLE);
        }

        if (programGuideSchedule.isGap()) {
            channelNameView.setText("");
            titleView.setText(R.string.epg_no_program);
            subtitleView.setText(R.string.epg_no_program_load_info);
            descriptionView.setText("");
            timeWrapperView.setVisibility(GONE);
        } else {
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

            ArrayList<String> metaInfosList = new ArrayList<>();
            if(epgEvent.lang != null) {
                metaInfosList.add(epgEvent.lang);
            }
            if(epgEvent.rating != null) {
                metaInfosList.add(getString(R.string.epg_age_from, epgEvent.rating));
            }
            if(epgEvent.getGenreStringResId() != null) {
                metaInfosList.add(getString(epgEvent.getGenreStringResId()));
            }
            String description = metaInfosList.stream().collect(Collectors.joining(" · "));
            if(!description.isEmpty()) {
                description += "\n";
            }
            description += epgEvent.description;

            channelNameView.setText(channel.title);
            titleView.setText(epgEvent.title);
            subtitleView.setText(epgEvent.subtitle);
            descriptionView.setText(description);
            timeView.setText(timeInfos.stream().collect(Collectors.joining(" | ")));
            timeWrapperView.setVisibility(VISIBLE);

            isScrollingDown = true;
            stopAutoScroll();
            descriptionScrollView.scrollTo(0,0);
            startAutoScroll(descriptionScrollView, descriptionView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        requestRefresh();
    }

    @Override
    public void onScheduleSelected
            (@org.jetbrains.annotations.Nullable ProgramGuideSchedule programGuideSchedule) {
        updateDetailView(programGuideSchedule);
    }

    @Override
    public void onScheduleClicked
            (@NotNull ProgramGuideSchedule programGuideSchedule) {

    }

    private void startAutoScroll(ScrollView scrollView, TextView textView) {
        scrollRunnable = new Runnable() {
            @Override
            public void run() {
                int currentScrollY = scrollView.getScrollY();
                int maxScrollY = textView.getHeight() - scrollView.getHeight();

                // Prüfe ob wir scrollen können
                if (maxScrollY <= 0) {
                    // Kein Scroll nötig, Text passt komplett rein
                    return;
                }

                if (isScrollingDown) {
                    // Nach unten scrollen
                    if (currentScrollY < maxScrollY) {
                        scrollView.scrollTo(0, currentScrollY + SCROLL_STEP);
                        handler.postDelayed(this, SCROLL_DELAY);
                    } else {
                        // Am Ende angekommen, Richtung wechseln nach Pause
                        isScrollingDown = false;
                        handler.postDelayed(this, PAUSE_AT_END);
                    }
                } else {
                    // Nach oben scrollen
                    if (currentScrollY > 0) {
                        scrollView.scrollTo(0, currentScrollY - SCROLL_STEP);
                        handler.postDelayed(this, SCROLL_DELAY);
                    } else {
                        // Am Anfang angekommen, Richtung wechseln nach Pause
                        isScrollingDown = true;
                        handler.postDelayed(this, PAUSE_AT_END);
                    }
                }
            }
        };
        handler.postDelayed(scrollRunnable, PAUSE_AT_END);
    }

    private void stopAutoScroll() {
        if (scrollRunnable != null) {
            handler.removeCallbacks(scrollRunnable);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopAutoScroll();
        wasClosed = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAutoScroll();
    }
}
