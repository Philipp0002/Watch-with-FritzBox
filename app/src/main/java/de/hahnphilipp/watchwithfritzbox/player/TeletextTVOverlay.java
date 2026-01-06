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
import de.hahnphilipp.watchwithfritzbox.utils.TeletextView;

public class TeletextTVOverlay extends Fragment implements KeyDownReceiver {

    public TVPlayerActivity context;
    private TeletextView teletextView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.overlay_teletext, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        teletextView = view.findViewById(R.id.teletextview);

    }

    public void experimentalSetTeletext(MediaPlayer.Teletext teletext) {
        if(teletextView == null) {
            return;
        }
        teletextView.setTeletext(teletext);
    }


    public boolean onKeyUp(int keyCode, KeyEvent event) {
        /*if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            context.popOverlayFragment();
            return true;
        }*/
        return false;
    }

    @Override
    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        return false;
    }

}