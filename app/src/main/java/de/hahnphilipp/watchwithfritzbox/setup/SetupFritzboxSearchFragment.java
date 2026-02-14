package de.hahnphilipp.watchwithfritzbox.setup;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import java.util.List;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.GetFritzInfo;
import de.hahnphilipp.watchwithfritzbox.async.GetPlaylists;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;

public class SetupFritzboxSearchFragment extends Fragment {

    private static final String PARAM_IP = "ip";
    private String ip;

    private List<ChannelUtils.Channel> channelList;

    public static SetupFritzboxSearchFragment newInstance(String ip) {
        SetupFritzboxSearchFragment fragment = new SetupFritzboxSearchFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_IP, ip);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView sdSearchText = view.findViewById(R.id.setup_sd_search_text);
        TextView hdSearchText = view.findViewById(R.id.setup_hd_search_text);
        TextView radioSearchText = view.findViewById(R.id.setup_radio_search_text);

        final GetPlaylists getPlaylists = new GetPlaylists(ip);
        getPlaylists.callback = new GetPlaylists.GetPlaylistResult() {
            @Override
            public void onTypeLoaded(ChannelUtils.ChannelType type, int channelAmount) {
                requireActivity().runOnUiThread(() -> {
                    switch (type) {
                        case SD ->
                                sdSearchText.setText(getString(R.string.setup_search_sd_result, channelAmount));
                        case HD ->
                                hdSearchText.setText(getString(R.string.setup_search_hd_result, channelAmount));
                        case RADIO ->
                                radioSearchText.setText(getString(R.string.setup_search_radio_result, channelAmount));
                    }
                });
            }

            @Override
            public void onAllLoaded(boolean error, List<ChannelUtils.Channel> channelList) {
                AsyncTask.execute(() -> {
                    ChannelUtils.setChannels(requireContext(), channelList);
                    requireActivity().runOnUiThread(() -> {
                        if (error) {
                            Toast.makeText(requireContext(), R.string.setup_search_error, Toast.LENGTH_LONG).show();
                            ((OnboardingActivity)requireActivity()).previousScreen();
                        } else {
                            view.findViewById(R.id.setup_search_progressBar).setVisibility(View.INVISIBLE);
                            ((OnboardingActivity)requireActivity()).enableNextButton(true);
                            SetupFritzboxSearchFragment.this.channelList = channelList;
                        }
                    });
                });


            }
        };

        final GetFritzInfo getFritzInfo = new GetFritzInfo(ip);
        getFritzInfo.callback = (error, friendlyNames) -> {
            if (error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.setup_search_error_connect, Toast.LENGTH_LONG).show();
                    ((OnboardingActivity)requireActivity()).previousScreen();
                });
            } else {
                boolean supportedFritzBox = friendlyNames.stream()
                        .map(String::toLowerCase)
                        .anyMatch(s -> s.contains("cable") || s.contains("dvbc") || s.contains("dvb-c"));

                if (supportedFritzBox) {
                    getPlaylists.execute();
                } else {
                    requireActivity().runOnUiThread(() -> {
                        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.setup_search_error_not_supported_title)
                                .setMessage(R.string.setup_search_error_not_supported_msg)
                                .setPositiveButton(R.string.setup_search_error_not_supported_continue, (dialog1, which) -> getPlaylists.execute())
                                .setNegativeButton(R.string.setup_search_error_not_supported_abort, (dialog12, which) -> {
                                    ((OnboardingActivity)requireActivity()).previousScreen();
                                }).create();
                        dialog.show();
                    });
                }
            }
        };
        getFritzInfo.execute();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_fritzbox_search, container, false);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            ip = getArguments().getString(PARAM_IP);
        }
    }

}
