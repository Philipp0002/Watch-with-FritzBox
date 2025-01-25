package de.hahnphilipp.watchwithfritzbox.player;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.BrowseFrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;

public class SelectionTVOverlay extends Fragment implements KeyDownReceiver {

    private RecyclerView recyclerView;

    public ArrayList<TVSetting> tvSettings = new ArrayList<TVSetting>();
    public String title = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.overlay_selectiontv, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        if (title == null) {
            view.findViewById(R.id.tvoverlayrecyclerTitle).setVisibility(View.GONE);
        } else {
            ((TextView) view.findViewById(R.id.tvoverlayrecyclerTitle)).setText(title);
        }

        TVSettingsOverlayRecyclerAdapter tvOverlayRecyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), tvSettings);
        final LinearLayoutManager llm = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);

        BrowseFrameLayout browseFrameLayout = view.findViewById(R.id.tvoverlayrecyclerBrowse);
        browseFrameLayout.setOnFocusSearchListener((focused, direction) -> {
            if (recyclerView.hasFocus()) {
                // keep focus on recyclerview! DO NOT return recyclerview, but focused, which is a child of the recyclerview
                return focused;
            } else {
                // someone else will find the next focus
                return null;
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            ((TVPlayerActivity)getActivity()).popOverlayFragment();
            return true;
        }
        return false;
    }
}
