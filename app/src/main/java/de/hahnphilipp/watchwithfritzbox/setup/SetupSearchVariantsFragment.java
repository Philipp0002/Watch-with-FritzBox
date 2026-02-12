package de.hahnphilipp.watchwithfritzbox.setup;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVSettingsOverlayRecyclerAdapter;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;

public class SetupSearchVariantsFragment extends Fragment {

    private SearchVariant variant;

    public SetupSearchVariantsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_search_variants, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<Object> tvSettingsWithTitle = new ArrayList<>();
        tvSettingsWithTitle.add(new TVSetting(getString(R.string.setup_variant_fritzbox), getString(R.string.setup_variant_fritzbox_description), TVSetting.NavigationIcon.CHEVRON, R.drawable.round_router, () -> {
            variant = SearchVariant.FRITZBOX;
            ((OnboardingActivity) requireActivity()).nextScreen();
        }));
        tvSettingsWithTitle.add(new TVSetting(getString(R.string.setup_variant_dvbc), getString(R.string.setup_variant_dvbc_description), TVSetting.NavigationIcon.CHEVRON, R.drawable.round_cable, () -> {
            variant = SearchVariant.DVB;
            ((OnboardingActivity) requireActivity()).nextScreen();
        }));

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        TVSettingsOverlayRecyclerAdapter recyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), tvSettingsWithTitle, recyclerView);
        final LinearLayoutManager llm = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(recyclerAdapter);
    }

    public SearchVariant getSelectedVariant() {
        return variant;
    }

    public enum SearchVariant {
        FRITZBOX, DVB;
    }
}