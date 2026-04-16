package de.hahnphilipp.watchwithfritzbox.player;

import android.content.Context;
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
import de.hahnphilipp.watchwithfritzbox.utils.CenterScrollLayoutManager;
import de.hahnphilipp.watchwithfritzbox.utils.KeyDownReceiver;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;

public class SelectionTVOverlay extends Fragment implements KeyDownReceiver {

    private RecyclerView recyclerView;

    public ArrayList<TVSetting> tvSettings = new ArrayList<TVSetting>();
    public String title = null;
    private TVSettingsOverlayRecyclerAdapter tvOverlayRecyclerAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.overlay_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Context context = view.getContext();
        recyclerView = view.findViewById(R.id.tvoverlayrecycler);

        ArrayList<Object> tvSettingsWithTitle = new ArrayList<>();
        if (title != null) {
            tvSettingsWithTitle.add(title);
        }
        tvSettingsWithTitle.addAll(tvSettings);


        tvOverlayRecyclerAdapter = new TVSettingsOverlayRecyclerAdapter(context, tvSettingsWithTitle, recyclerView);
        final CenterScrollLayoutManager llm = new CenterScrollLayoutManager(context);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(tvOverlayRecyclerAdapter);

        BrowseFrameLayout browseFrameLayout = view.findViewById(R.id.tvoverlayrecyclerBrowse);
        browseFrameLayout.setOnFocusSearchListener((focused, direction) -> recyclerView.hasFocus() ? focused : null);
    }

    public void updateTitle(String title) {
        this.title = title;
        if (tvOverlayRecyclerAdapter != null) {
            if(!tvOverlayRecyclerAdapter.objects.isEmpty() && tvOverlayRecyclerAdapter.objects.get(0) instanceof String) {
                tvOverlayRecyclerAdapter.objects.set(0, title);
                tvOverlayRecyclerAdapter.notifyItemChanged(0);
            } else {
                tvOverlayRecyclerAdapter.notifyItemInserted(0);
                tvOverlayRecyclerAdapter.objects.add(0, title);
            }
        }
    }



    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            ((TVPlayerActivity)getActivity()).popOverlayFragment();
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyDownLong(int keyCode, KeyEvent event) {
        return false;
    }
}
