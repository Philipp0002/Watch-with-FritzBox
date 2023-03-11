package de.hahnphilipp.watchwith.player;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.utils.ChannelUtils;

public class ChannelListTVOverlay extends Fragment {

    public TVPlayerActivity context;
    public boolean isShown = false;

    TVChannelListOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    RecyclerView recyclerView;
    LinearLayoutManager llm;

    Timer t;

    private static ChannelListTVOverlay INSTANCE;

    public static void notifyChannelListChanged(){
        if(INSTANCE != null){
            INSTANCE.updateChannelList();
        }
    }

    public void updateChannelList(){
        tvOverlayRecyclerAdapter.objects = ChannelUtils.getAllChannels(getContext());
        getActivity().runOnUiThread(() -> tvOverlayRecyclerAdapter.notifyDataSetChanged());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        INSTANCE = this;
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.channellisttvoverlay, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        INSTANCE = this;

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        tvOverlayRecyclerAdapter = new TVChannelListOverlayRecyclerAdapter(this, ChannelUtils.getAllChannels(getContext()), Picasso.get(), recyclerView);
        llm = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);

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

        hideOverlays();
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startUpdateTimer();
    }


    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(!isShown && !context.mSettingsOverlayFragment.isShown){
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
                    Animation a = AnimationUtils.loadAnimation(context, R.anim.slide_up);
                    a.setFillEnabled(false);
                    a.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            ChannelUtils.Channel next = ChannelUtils.getNextChannel(context, ChannelUtils.getLastSelectedChannel(context));
                            ChannelUtils.updateLastSelectedChannel(context, next.number);
                            context.launchPlayer(true);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    context.findViewById(R.id.video_layout).startAnimation(a);

                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
                    Animation a = AnimationUtils.loadAnimation(context, R.anim.slide_down);
                    a.setFillEnabled(false);
                    a.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            ChannelUtils.Channel previous = ChannelUtils.getPreviousChannel(context, ChannelUtils.getLastSelectedChannel(context));
                            ChannelUtils.updateLastSelectedChannel(context, previous.number);
                            context.launchPlayer(true);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    context.findViewById(R.id.video_layout).startAnimation(a);

                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                    showOverlays();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    showOverlays();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    context.mSettingsOverlayFragment.showOverlays();
                    return true;
                } else {
                    if(keyCode == KeyEvent.KEYCODE_0){
                        context.enterNumber(0);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_1){
                        context.enterNumber(1);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_2){
                        context.enterNumber(2);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_3){
                        context.enterNumber(3);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_4){
                        context.enterNumber(4);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_5){
                        context.enterNumber(5);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_6){
                        context.enterNumber(6);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_7){
                        context.enterNumber(7);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_8){
                        context.enterNumber(8);
                        return true;
                    }else if(keyCode == KeyEvent.KEYCODE_9){
                        context.enterNumber(9);
                        return true;
                    }
                    return false;
                }

            }
        }else{
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK && isShown) {
                    hideOverlays();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && isShown) {
                    hideOverlays();
                    return true;
                }
            }
        }
    return false;

    }



    public void startUpdateTimer(){
        if(t == null){
            t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    updateInfo();
                }
            }, 1000, 1000);
        }
    }

    public void stopUpdateTimer(){
        if(t != null){
            t.cancel();
            t = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startUpdateTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUpdateTimer();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopUpdateTimer();
    }

    public void updateInfo(){
        DateFormat dfdate = DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault());
        final String date = dfdate.format(new Date());
        DateFormat dftime = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
        final String time = dftime.format(new Date());

        if(context != null)
            context.runOnUiThread(() -> {
                ((TextView)getView().findViewById(R.id.tvoverlaydate)).setText(date);
                ((TextView)getView().findViewById(R.id.tvoverlaytime)).setText(time);
            });
    }

    public void showOverlays(){
        isShown = true;
        int lastSelectedChannel = ChannelUtils.getLastSelectedChannel(getContext())-1;
        tvOverlayRecyclerAdapter.selectedChannel = lastSelectedChannel+1;
        tvOverlayRecyclerAdapter.notifyDataSetChanged();
        getView().setVisibility(View.VISIBLE);
        recyclerView.scrollToPosition(lastSelectedChannel);

    }

    public void hideOverlays(){
        isShown = false;
        getView().setVisibility(View.GONE);
    }

}
