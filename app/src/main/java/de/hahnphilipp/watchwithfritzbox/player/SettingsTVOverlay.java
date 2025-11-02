package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.setup.SetupIPActivity;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;

public class SettingsTVOverlay extends Fragment implements KeyDownReceiver {

    public TVPlayerActivity context;

    private TVSettingsOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    private RecyclerView recyclerView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.overlay_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);


        ArrayList<Object> tvSettings = new ArrayList<Object>();

        tvOverlayRecyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), tvSettings);
        final LinearLayoutManager llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);
        updateTVSettings();


        BrowseFrameLayout browseFrameLayout = view.findViewById(R.id.tvoverlayrecyclerBrowse);
        browseFrameLayout.setOnFocusSearchListener(new BrowseFrameLayout.OnFocusSearchListener() {
            @Override
            public View onFocusSearch(View focused, int direction) {
                if (recyclerView.hasFocus())
                    return focused; // keep focus on recyclerview! DO NOT return recyclerview, but focused, which is a child of the recyclerview
                else
                    return null; // someone else will find the next focus
            }
        });
    }


    public void updateTVSettings() {
        ArrayList<Object> tvSettings = new ArrayList<Object>();

        if (context == null)
            return;

        final MediaPlayer player = context.mMediaPlayer;
        MediaPlayer.TrackDescription[] descriptionsAudio = new MediaPlayer.TrackDescription[0];
        MediaPlayer.TrackDescription[] descriptionsSubtitle = new MediaPlayer.TrackDescription[0];

        if (player != null) {
            descriptionsAudio = player.getAudioTracks();
            descriptionsSubtitle = player.getSpuTracks();
        }

        tvSettings.add(context.getString(R.string.playback_title));

        tvSettings.add(new TVSetting(context.getString(R.string.settings_open_epg), R.drawable.round_remote, this::showEpg, true));

        if (descriptionsAudio != null && descriptionsAudio.length != 0) {
            tvSettings.add(new TVSetting(context.getString(R.string.audio_tracks), R.drawable.round_audiotrack, this::showAudioTrackSelection, true));
        }

        if (descriptionsSubtitle != null && descriptionsSubtitle.length != 0) {
            tvSettings.add(new TVSetting(context.getString(R.string.subtitles), R.drawable.round_closed_caption, this::showSubtitleTrackSelection, true));
        }

        if (ChannelUtils.getChannelByNumber(context, ChannelUtils.getLastSelectedChannel(context)).type != ChannelUtils.ChannelType.RADIO) {
            tvSettings.add(new TVSetting(context.getString(R.string.video_aspect), R.drawable.round_video_settings, this::showVideoFormatSelection, true));
        }


        tvSettings.add(context.getString(R.string.settings_title));

        tvSettings.add(new TVSetting(context.getString(R.string.settings_audio_delay), R.drawable.round_timeline, this::showAudioDelaySelection, false));
        tvSettings.add(new TVSetting(context.getString(R.string.settings_hardware_acceleration), R.drawable.round_speed, this::showHWAcelerationSelection, false));
        tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace), R.drawable.round_gradient, this::showDeinterlaceSelection, false));

        tvSettings.add(new TVSetting(context.getString(R.string.settings_reorder_channels), R.drawable.round_reorder, this::showChannelEditor, false));
        tvSettings.add(new TVSetting(context.getString(R.string.settings_reset_app), R.drawable.round_reset_settings, this::showAppResetSelection, false));

        if (tvOverlayRecyclerAdapter != null) {
            tvOverlayRecyclerAdapter.objects = tvSettings;
            tvOverlayRecyclerAdapter.notifyDataSetChanged();
        }
    }

    public void showChannelEditor() {
        EditChannelListTVOverlay editChannelListTVOverlay = new EditChannelListTVOverlay();

        context.addOverlayFragment(editChannelListTVOverlay);
    }

    public void showEpg() {
        context.addOverlayFragment(context.mEPGOverlayFragment);
    }

    public void showAppResetSelection() {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        selectionTVOverlay.title = context.getString(R.string.settings_reset_app);
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_reset_cancel), R.drawable.round_arrow_back, () -> {
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_reset_confirm), R.drawable.round_reset_settings, () -> {
            editor.clear();
            editor.commit();
            startActivity(new Intent(context, SetupIPActivity.class));
            context.finish();
            context.overridePendingTransition(0, 0);
        }, true));
        context.addOverlayFragment(selectionTVOverlay);
    }

    public void showAudioDelaySelection() {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        selectionTVOverlay.title = context.getString(R.string.settings_audio_delay_value, sp.getLong("setting_audio_delay", 0) + "ms");
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_audio_delay_plus), R.drawable.round_add, () -> {
            long val = sp.getLong("setting_audio_delay", 0);
            val += 250;
            editor.putLong("setting_audio_delay", val);
            editor.commit();
            if(context.mMediaPlayer != null) {
                context.mMediaPlayer.setAudioDelay(val * 1000);
            }
            selectionTVOverlay.updateTitle(context.getString(R.string.settings_audio_delay_value, sp.getLong("setting_audio_delay", 0) + "ms"));
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_audio_delay_minus), R.drawable.round_minus, () -> {
            long val = sp.getLong("setting_audio_delay", 0);
            val -= 250;
            editor.putLong("setting_audio_delay", val);
            editor.commit();
            if(context.mMediaPlayer != null) {
                context.mMediaPlayer.setAudioDelay(val * 1000);
            }
            selectionTVOverlay.updateTitle(context.getString(R.string.settings_audio_delay_value, sp.getLong("setting_audio_delay", 0) + "ms"));
        }, true));
        context.addOverlayFragment(selectionTVOverlay);
    }

    public void showHWAcelerationSelection() {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        selectionTVOverlay.title = context.getString(R.string.settings_hardware_acceleration);
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_hardware_acceleration_disable), R.drawable.round_power_off, () -> {
            editor.putInt("setting_hwaccel", 0);
            editor.commit();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_hardware_acceleration_auto), R.drawable.round_auto_awesome, () -> {
            editor.putInt("setting_hwaccel", 1);
            editor.commit();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_hardware_acceleration_force), R.drawable.round_power, () -> {
            editor.putInt("setting_hwaccel", 2);
            editor.commit();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        context.addOverlayFragment(selectionTVOverlay);
    }

    public void showDeinterlaceSelection() {
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        selectionTVOverlay.title = context.getString(R.string.settings_deinterlace);
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_disable), R.drawable.round_power_off, () -> {
            editor.remove("setting_deinterlace");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_auto), R.drawable.round_auto_awesome, () -> {
            editor.putString("setting_deinterlace", "auto");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_blend), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "blend");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_linear), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "linear");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_bob), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "bob");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_ivtc), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "ivtc");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_discard), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "discard");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_yadif), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "yadif");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_yadifx2), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "yadif2x");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_phosphor), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "phosphor");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));
        selectionTVOverlay.tvSettings.add(new TVSetting(context.getString(R.string.settings_deinterlace_mode_mean), R.drawable.round_tv, () -> {
            editor.putString("setting_deinterlace", "mean");
            editor.commit();
            context.unloadLibVLC();
            context.loadLibVLC();
            context.launchPlayer(false);
            context.popOverlayFragment();
        }, true));

        context.addOverlayFragment(selectionTVOverlay);
    }

    public void showVideoFormatSelection() {
        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        final MediaPlayer player = context.mMediaPlayer;
        selectionTVOverlay.title = context.getString(R.string.video_aspect);
        String[] aspect_ratios = {"16:9", "4:3", "21:9", "16:10"};
        //this should actually never be true, but just to be sure we do it anyways
        for (String aspect : aspect_ratios) {
            selectionTVOverlay.tvSettings.add(new TVSetting(aspect, R.drawable.round_video_settings, () -> {
                player.setAspectRatio(aspect);
                context.popOverlayFragment();
            }, true));
        }

        context.addOverlayFragment(selectionTVOverlay);
    }

    public void showSubtitleTrackSelection() {
        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        final MediaPlayer player = context.mMediaPlayer;
        selectionTVOverlay.title = context.getString(R.string.subtitles);
        MediaPlayer.TrackDescription[] descriptions = player.getSpuTracks();
        //this should actually never be true, but just to be sure we do it anyways
        if (descriptions == null || descriptions.length == 0) {
            Toast.makeText(getContext(), R.string.no_subtitle_tracks, Toast.LENGTH_SHORT).show();
            return;
        }
        for (final MediaPlayer.TrackDescription description : descriptions) {
            selectionTVOverlay.tvSettings.add(new TVSetting(description.name, R.drawable.round_closed_caption, () -> {
                player.setSpuTrack(description.id);
                context.popOverlayFragment();
            }, true));
        }

        context.addOverlayFragment(selectionTVOverlay);
    }

    public void showAudioTrackSelection() {
        SelectionTVOverlay selectionTVOverlay = new SelectionTVOverlay();
        final MediaPlayer player = context.mMediaPlayer;
        selectionTVOverlay.title = context.getString(R.string.audio_tracks);
        MediaPlayer.TrackDescription[] descriptions = player.getAudioTracks();
        //this should actually never be true, but just to be sure we do it anyways
        if (descriptions == null || descriptions.length == 0) {
            Toast.makeText(getContext(), R.string.no_audio_tracks, Toast.LENGTH_SHORT).show();
            return;
        }
        for (final MediaPlayer.TrackDescription description : descriptions) {
            selectionTVOverlay.tvSettings.add(new TVSetting(description.name, R.drawable.round_audiotrack, () -> {
                player.setAudioTrack(description.id);
                context.popOverlayFragment();
            }, true));
        }

        context.addOverlayFragment(selectionTVOverlay);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            context.popOverlayFragment();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        return false;
    }

}