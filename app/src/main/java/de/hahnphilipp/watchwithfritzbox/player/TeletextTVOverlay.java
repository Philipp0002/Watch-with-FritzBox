package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.videolan.libvlc.MediaPlayer;

import java.util.HashMap;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;
import de.hahnphilipp.watchwithfritzbox.utils.TeletextView;

public class TeletextTVOverlay extends Fragment implements KeyDownReceiver {

    public TVPlayerActivity context;
    private TeletextView teletextView;

    private final HashMap<Integer, String> teletextPages = new HashMap<>();
    private int currentPage = 100;
    private boolean isShown = false;

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
        setTeletextPage(currentPage);
    }

    public void setTeletextPage(int page) {
        currentPage = page;
        setTeletextViewPage(teletextPages.getOrDefault(page, null));
    }

    public void updateTeletextPage(int page, String teletextData) {
        teletextPages.put(page, teletextData);
        if(page == currentPage) {
            setTeletextPage(page);
        }
    }

    private void setTeletextViewPage(String teletextData) {
        if(teletextView == null) {
            return;
        }

        MediaPlayer.Teletext teletext = new Gson().fromJson(teletextData, MediaPlayer.Teletext.class);
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

    public boolean isShown() {
        return isShown;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        isShown = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isShown = false;
        teletextPages.clear();
    }
}