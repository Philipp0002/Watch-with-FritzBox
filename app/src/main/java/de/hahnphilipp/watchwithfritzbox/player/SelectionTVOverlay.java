package de.hahnphilipp.watchwithfritzbox.player;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.leanback.widget.GuidedAction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.Timer;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.DialogFragment;
import de.hahnphilipp.watchwithfritzbox.utils.DialogFragmentCallback;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;

public class SelectionTVOverlay extends Fragment {

    TVSettingsOverlayRecyclerAdapter tvOverlayRecyclerAdapter;
    RecyclerView recyclerView;

    public ArrayList<TVSetting> tvSettings = new ArrayList<TVSetting>();
    public String title = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.selectiontvoverlay, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        if(title == null){
            view.findViewById(R.id.tvoverlayrecyclerTitle).setVisibility(View.GONE);
        }else{
            ((TextView)view.findViewById(R.id.tvoverlayrecyclerTitle)).setText(title);
        }



        tvOverlayRecyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), tvSettings);
        final LinearLayoutManager llm = new LinearLayoutManager(getContext());
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
    }

/*    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(isShown){
            if(event.getAction() == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    hideOverlays();
                    return true;
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    hideOverlays();
                    return true;
                }
            }
        }
    return false;

    }*/


}
