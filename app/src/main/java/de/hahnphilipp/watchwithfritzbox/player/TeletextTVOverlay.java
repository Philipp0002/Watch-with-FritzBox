package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import org.videolan.libvlc.MediaPlayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;
import de.hahnphilipp.watchwithfritzbox.utils.TeletextView;
import de.hahnphilipp.watchwithfritzbox.utils.UIThread;

public class TeletextTVOverlay extends Fragment implements KeyDownReceiver {

    private static final String SP_KEYPAD_HIDDEN = "setting_teletext_keypad_hidden";
    public TVPlayerActivity context;

    private SharedPreferences sp;
    private SharedPreferences.Editor spEditor;

    private final HashMap<Integer, String> teletextPages = new HashMap<>();
    private int currentPage = 100;
    private boolean isShown = false;
    private Integer spuTrackBeforeShow = null;
    private boolean keypadHidden = false;


    private TeletextView teletextView;
    private View extraKeyRed, extraKeyBlue, extraKeyYellow, extraKeyGreen;
    private View key0, key1, key2, key3, key4, key5, key6, key7, key8, key9;
    private View keyUp, keyDown;
    private MaterialButton keypadHideButton;

    private View teletextKeypadWrapper;
    private LinearLayout pageNumberWrapperShown, pageNumberWrapperHidden;
    private TextView pageNumberView;
    private ViewAnimator teletextViewAnimator;
    private TextView teletextLoadingView, teletextReceivingView;

    private Integer pageRed, pageGreen, pageYellow, pageBlue;


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

        sp = context.getSharedPreferences(
                context.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        spEditor = sp.edit();

        teletextView = view.findViewById(R.id.teletextview);

        pageNumberView = view.findViewById(R.id.teletext_page_number);
        pageNumberWrapperHidden = view.findViewById(R.id.teletext_page_number_wrapper_hidden);
        pageNumberWrapperShown = view.findViewById(R.id.teletext_page_number_wrapper_shown);
        teletextKeypadWrapper = view.findViewById(R.id.teletext_keypad_wrapper);

        teletextViewAnimator = view.findViewById(R.id.teletext_view_animator);
        teletextLoadingView = view.findViewById(R.id.teletext_loading_text);
        teletextReceivingView = view.findViewById(R.id.teletext_receiving_text);

        extraKeyRed = view.findViewById(R.id.extra_key_red);
        extraKeyBlue = view.findViewById(R.id.extra_key_blue);
        extraKeyYellow = view.findViewById(R.id.extra_key_yellow);
        extraKeyGreen = view.findViewById(R.id.extra_key_green);

        keypadHideButton = view.findViewById(R.id.teletext_keys_hide);

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

        keypadHideButton.setOnClickListener(view1 -> hideKeypad(!keypadHidden, true));

        hideKeypad(sp.getBoolean(SP_KEYPAD_HIDDEN, false), false);

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

        pageNumberWrapperHidden.setVisibility(View.VISIBLE);

        if (enteredNumber.matches("\\d+")) {
            int page = Integer.parseInt(enteredNumber);
            if (page >= 100 && page <= 999) {
                pageNumberWrapperHidden.setVisibility(View.GONE);
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

                UIThread.run(() -> {
                    if (teletextViewAnimator.getDisplayedChild() != 1)
                        teletextViewAnimator.setDisplayedChild(1);
                });
            });
        } else {
            if (teletextViewAnimator.getDisplayedChild() != 0)
                teletextViewAnimator.setDisplayedChild(0);
        }
        teletextLoadingView.setText(getString(R.string.teletext_page_loading, currentPage));
    }

    private void setTeletextColorButtons(MediaPlayer.TeletextNav[] teletextNav) {
        extraKeyGreen.setOnClickListener(null);
        extraKeyBlue.setOnClickListener(null);
        extraKeyYellow.setOnClickListener(null);
        extraKeyRed.setOnClickListener(null);
        UIThread.run(() -> {
            View[] visibleButtons = new View[4];
            View[] invisibleButtons = new View[]{extraKeyRed, extraKeyGreen, extraKeyYellow, extraKeyBlue};
            pageRed = null;
            pageGreen = null;
            pageYellow = null;
            pageBlue = null;
            for (MediaPlayer.TeletextNav nav : teletextNav) {
                switch (nav.getLabel().toLowerCase()) {
                    case "red":
                        extraKeyRed.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[0] = extraKeyRed;
                        invisibleButtons[0] = null;
                        pageRed = nav.getPageNumber();
                        break;
                    case "blue":
                        extraKeyBlue.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[3] = extraKeyBlue;
                        invisibleButtons[3] = null;
                        pageBlue = nav.getPageNumber();
                        break;
                    case "yellow":
                        extraKeyYellow.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[2] = extraKeyYellow;
                        invisibleButtons[2] = null;
                        pageYellow = nav.getPageNumber();
                        break;
                    case "green":
                        extraKeyGreen.setOnClickListener(view1 -> setTeletextPage(nav.getPageNumber()));
                        visibleButtons[1] = extraKeyGreen;
                        invisibleButtons[1] = null;
                        pageGreen = nav.getPageNumber();
                        break;
                    case "index":

                        break;
                }
            }

            List<View> buttonList = Arrays.stream(visibleButtons).filter(Objects::nonNull).toList();
            Arrays.stream(invisibleButtons).filter(Objects::nonNull).forEach(v -> v.setVisibility(View.GONE));
            for (View v : buttonList) {
                v.setVisibility(View.VISIBLE);
                v.setBackgroundResource(R.drawable.button_center_wrapper);
            }
            if (buttonList.size() == 1) {
                buttonList.get(0).setBackgroundResource(R.drawable.button_single_wrapper);
            } else {
                if(!buttonList.isEmpty()) {
                    buttonList.get(0).setBackgroundResource(R.drawable.button_left_wrapper);
                    buttonList.get(buttonList.size() - 1).setBackgroundResource(R.drawable.button_right_wrapper);
                }
            }
        });


    }

    public void updateTeletextPage(int page, String teletextData) {
        if (teletextViewAnimator.getDisplayedChild() == 0) {
            teletextReceivingView.setText(getString(R.string.teletext_page_receiving, page));
        }
        teletextPages.put(page, teletextData);
        if (page == currentPage) {
            setTeletextPage(page);
        }
    }

    private void setTeletextViewPage(String teletextData) {
        if (teletextView == null || getActivity() == null || !isShown) {
            return;
        }

        MediaPlayer.Teletext teletext = new Gson().fromJson(teletextData, MediaPlayer.Teletext.class);
        setTeletextColorButtons(teletext.getNavigation());
        UIThread.run(() -> teletextView.setTeletext(teletext));
        enteredNumber = teletext.getPageNumber() + "";
        UIThread.run(() -> pageNumberView.setText(enteredNumber));
    }


    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(!isShown) {
            return false;
        }
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_0:
                    enterNumber(0);
                    return true;
                case KeyEvent.KEYCODE_1:
                    enterNumber(1);
                    return true;
                case KeyEvent.KEYCODE_2:
                    enterNumber(2);
                    return true;
                case KeyEvent.KEYCODE_3:
                    enterNumber(3);
                    return true;
                case KeyEvent.KEYCODE_4:
                    enterNumber(4);
                    return true;
                case KeyEvent.KEYCODE_5:
                    enterNumber(5);
                    return true;
                case KeyEvent.KEYCODE_6:
                    enterNumber(6);
                    return true;
                case KeyEvent.KEYCODE_7:
                    enterNumber(7);
                    return true;
                case KeyEvent.KEYCODE_8:
                    enterNumber(8);
                    return true;
                case KeyEvent.KEYCODE_9:
                    enterNumber(9);
                    return true;
                case KeyEvent.KEYCODE_PROG_RED:
                    if(pageRed != null)
                        setTeletextPage(pageRed);
                    return true;
                case KeyEvent.KEYCODE_PROG_BLUE:
                    if(pageBlue != null)
                        setTeletextPage(pageBlue);
                    return true;
                case KeyEvent.KEYCODE_PROG_YELLOW:
                    if(pageYellow != null)
                        setTeletextPage(pageYellow);
                    return true;
                case KeyEvent.KEYCODE_PROG_GREEN:
                    if(pageGreen != null)
                        setTeletextPage(pageGreen);
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean isShown() {
        return isShown;
    }

    public void hideKeypad(boolean hide, boolean checkState) {
        if(keypadHidden == hide && checkState) return;
        if(hide) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) teletextViewAnimator.getLayoutParams();
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            teletextViewAnimator.setLayoutParams(params);

            if(pageNumberWrapperShown.getChildCount() > 0) {
                View child = pageNumberWrapperShown.getChildAt(0);
                pageNumberWrapperShown.removeView(child);
                pageNumberWrapperHidden.addView(child);
            }
            teletextKeypadWrapper.setVisibility(View.GONE);

            keypadHideButton.setIconResource(R.drawable.round_grid_on);
        } else {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) teletextViewAnimator.getLayoutParams();
            params.endToEnd = ConstraintLayout.LayoutParams.UNSET;
            teletextViewAnimator.setLayoutParams(params);

            if(pageNumberWrapperHidden.getChildCount() > 0) {
                View child = pageNumberWrapperHidden.getChildAt(0);
                pageNumberWrapperHidden.removeView(child);
                pageNumberWrapperShown.addView(child);
            }
            teletextKeypadWrapper.setVisibility(View.VISIBLE);

            keypadHideButton.setIconResource(R.drawable.round_grid_off);
        }

        if(!enteredNumber.contains("-")) {
            pageNumberWrapperHidden.setVisibility(View.GONE);
        }

        spEditor.putBoolean(SP_KEYPAD_HIDDEN, hide).apply();
        this.keypadHidden = hide;
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
                this.context.runOnVLCThread(() -> this.context.mMediaPlayer.setTeletext(TVPlayerActivity.TELETEXT_IDLE_PAGE));
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
                this.context.runOnVLCThread(() -> this.context.mMediaPlayer.setSpuTrack(this.spuTrackBeforeShow));
            }
        }
        this.spuTrackBeforeShow = null;
    }
}