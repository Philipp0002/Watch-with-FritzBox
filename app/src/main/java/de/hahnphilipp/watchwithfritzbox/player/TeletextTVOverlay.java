package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;
import de.hahnphilipp.watchwithfritzbox.utils.TeletextView;

public class TeletextTVOverlay extends Fragment implements KeyDownReceiver {

    public TVPlayerActivity context;

    private final HashMap<Integer, String> teletextPages = new HashMap<>();
    private int currentPage = 100;
    private boolean isShown = false;
    private Integer spuTrackBeforeShow = null;


    private TeletextView teletextView;
    private View extraKeyRed, extraKeyBlue, extraKeyYellow, extraKeyGreen;
    private View key0, key1, key2, key3, key4, key5, key6, key7, key8, key9;
    private View keyUp, keyDown;

    private TextView pageNumberView;
    private ViewAnimator teletextViewAnimator;
    private TextView teletextLoadingView;


    private String enteredNumber = currentPage + "";

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

        pageNumberView = view.findViewById(R.id.teletext_page_number);

        teletextViewAnimator = view.findViewById(R.id.teletext_view_animator);
        teletextLoadingView = view.findViewById(R.id.teletext_loading_text);

        extraKeyRed = view.findViewById(R.id.extra_key_red);
        extraKeyBlue = view.findViewById(R.id.extra_key_blue);
        extraKeyYellow = view.findViewById(R.id.extra_key_yellow);
        extraKeyGreen = view.findViewById(R.id.extra_key_green);

        keyUp = view.findViewById(R.id.teletext_key_page_up);
        keyUp.setOnClickListener(view1 -> changePage(1));
        keyDown = view.findViewById(R.id.teletext_key_page_down);
        keyDown.setOnClickListener(view1 -> changePage(-1));

        key0 = view.findViewById(R.id.teletext_key_0);
        key0.setOnClickListener(view1 -> enterNumber(0));
        key1 = view.findViewById(R.id.teletext_key_1);
        key1.setOnClickListener(view1 -> enterNumber(1));
        key2 = view.findViewById(R.id.teletext_key_2);
        key2.setOnClickListener(view1 -> enterNumber(2));
        key3 = view.findViewById(R.id.teletext_key_3);
        key3.setOnClickListener(view1 -> enterNumber(3));
        key4 = view.findViewById(R.id.teletext_key_4);
        key4.setOnClickListener(view1 -> enterNumber(4));
        key5 = view.findViewById(R.id.teletext_key_5);
        key5.setOnClickListener(view1 -> enterNumber(5));
        key6 = view.findViewById(R.id.teletext_key_6);
        key6.setOnClickListener(view1 -> enterNumber(6));
        key7 = view.findViewById(R.id.teletext_key_7);
        key7.setOnClickListener(view1 -> enterNumber(7));
        key8 = view.findViewById(R.id.teletext_key_8);
        key8.setOnClickListener(view1 -> enterNumber(8));
        key9 = view.findViewById(R.id.teletext_key_9);
        key9.setOnClickListener(view1 -> enterNumber(9));

        pageNumberView.setText(enteredNumber);
        setTeletextPage(currentPage);
    }

    private void enterNumber(int number) {
        if (enteredNumber.equals(currentPage + "")) {
            if (number == 0) {
                // prevent leading zero
                return;
            }
            enteredNumber = "---";
        }
        enteredNumber = enteredNumber + number;
        enteredNumber = enteredNumber.substring(1, enteredNumber.length());

        pageNumberView.setText(enteredNumber);

        if (enteredNumber.matches("\\d+")) {
            int page = Integer.parseInt(enteredNumber);
            if (page >= 100 && page <= 999) {
                setTeletextPage(page);
            }
        }
    }

    private void changePage(int delta) {
        int newPage = currentPage + delta;
        if (newPage < 100) {
            newPage = 999;
        } else if (newPage > 999) {
            newPage = 100;
        }
        setTeletextPage(newPage);
    }

    public void setTeletextPage(int page) {
        if (page != currentPage) {
            enteredNumber = page + "";
            pageNumberView.setText(enteredNumber);
        }
        currentPage = page;
        String teletextData = teletextPages.getOrDefault(page, null);

        if (teletextData != null) {
            AsyncTask.execute(() -> {
                setTeletextViewPage(teletextData);
                requireActivity().runOnUiThread(() -> {
                    if (teletextViewAnimator.getDisplayedChild() != 1)
                        teletextViewAnimator.setDisplayedChild(1);
                });
            });
        } else {
            if (teletextViewAnimator.getDisplayedChild() != 0)
                teletextViewAnimator.setDisplayedChild(0);
            teletextLoadingView.setText(getString(R.string.teletext_page_loading, currentPage));
        }
    }

    private void setTeletextColorButtons(MediaPlayer.TeletextNav[] teletextNav) {

        extraKeyGreen.setOnClickListener(null);
        extraKeyBlue.setOnClickListener(null);
        extraKeyYellow.setOnClickListener(null);
        extraKeyRed.setOnClickListener(null);
        requireActivity().runOnUiThread(() -> {
            extraKeyGreen.setVisibility(View.GONE);
            extraKeyBlue.setVisibility(View.GONE);
            extraKeyYellow.setVisibility(View.GONE);
            extraKeyRed.setVisibility(View.GONE);
            View[] visibleButtons = new View[4];
            for (MediaPlayer.TeletextNav nav : teletextNav) {
                Log.d("TeletextNav", "page " + nav.getPageNumber() + " - subPage " + nav.getSubPageNumber() + " - label " + nav.getLabel());
                switch (nav.getLabel().toLowerCase()) {
                    case "red":
                        extraKeyRed.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[0] = extraKeyRed;
                        break;
                    case "blue":
                        extraKeyBlue.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[3] = extraKeyBlue;
                        break;
                    case "yellow":
                        extraKeyYellow.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[2] = extraKeyYellow;
                        break;
                    case "green":
                        extraKeyGreen.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[1] = extraKeyGreen;
                        break;
                    case "index":

                        break;
                }
            }

            List<View> buttonList = Arrays.stream(visibleButtons).filter(Objects::nonNull).toList();
            for (View v : buttonList) {
                v.setVisibility(View.VISIBLE);
                v.setBackgroundResource(R.drawable.center_button_wrapper);
            }
            if (buttonList.size() == 1) {
                buttonList.get(0).setBackgroundResource(R.drawable.single_button_wrapper);
            } else {
                buttonList.get(0).setBackgroundResource(R.drawable.left_button_wrapper);
                buttonList.get(buttonList.size() - 1).setBackgroundResource(R.drawable.right_button_wrapper);
            }
        });


    }

    public void updateTeletextPage(int page, String teletextData) {
        teletextPages.put(page, teletextData);
        if (page == currentPage) {
            setTeletextPage(page);
        }
    }

    private void setTeletextViewPage(String teletextData) {
        if (teletextView == null) {
            return;
        }

        MediaPlayer.Teletext teletext = new Gson().fromJson(teletextData, MediaPlayer.Teletext.class);
        setTeletextColorButtons(teletext.getNavigation());
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
        if (this.context != null && this.context.mMediaPlayer != null) {
            MediaPlayer.TrackDescription spuTrack = Arrays.stream(this.context.mMediaPlayer.getSpuTracks())
                    .filter(spu -> spu.id == this.context.mMediaPlayer.getSpuTrack())
                    .findFirst()
                    .orElse(null);
            if (spuTrack != null && !spuTrack.name.contains("Teletext")) {
                this.spuTrackBeforeShow = this.context.mMediaPlayer.getSpuTrack();
                this.context.mMediaPlayer.setTeletext(TVPlayerActivity.TELETEXT_IDLE_PAGE);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isShown = false;
        teletextPages.clear();

        if (this.context != null && this.context.mMediaPlayer != null) {
            if (this.spuTrackBeforeShow != null) {
                this.context.mMediaPlayer.setSpuTrack(this.spuTrackBeforeShow);
            }
        }
        this.spuTrackBeforeShow = null;
    }
}