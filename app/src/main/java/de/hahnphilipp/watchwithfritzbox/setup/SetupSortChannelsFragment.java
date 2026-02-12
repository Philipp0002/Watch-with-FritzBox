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
import android.widget.Toast;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVSettingsOverlayRecyclerAdapter;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SetupSortChannelsFragment extends Fragment {


    public SetupSortChannelsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ArrayList<Object> tvSettingsWithTitle = new ArrayList<>();
        tvSettingsWithTitle.add(new TVSetting(getString(R.string.setup_order_yes), getString(R.string.setup_order_yes_subtitle), TVSetting.NavigationIcon.CHEVRON, R.drawable.round_sort, () -> {
            presortChannels();
        }));
        tvSettingsWithTitle.add(new TVSetting(getString(R.string.setup_order_no), getString(R.string.setup_order_no_subtitle), TVSetting.NavigationIcon.CHEVRON, R.drawable.round_filter_list_off, () -> {
            ((OnboardingActivity) requireActivity()).nextScreen();
        }));

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        TVSettingsOverlayRecyclerAdapter recyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), tvSettingsWithTitle, recyclerView);
        final LinearLayoutManager llm = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(recyclerAdapter);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup_sort_channels, container, false);
    }

    public void presortChannels() {
        requireView().findViewById(R.id.recyclerView).setVisibility(View.INVISIBLE);
        requireView().findViewById(R.id.setup_order_progressBar).setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build();
        Request request = new Request.Builder()
                .url("https://hahnphilipp.de/watchwithfritzbox/presetOrder.json")
                .build();

        // OkHttp Request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.setup_order_error, Toast.LENGTH_LONG).show();

                    requireView().findViewById(R.id.recyclerView).setVisibility(View.VISIBLE);
                    requireView().findViewById(R.id.setup_order_progressBar).setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ArrayList<ChannelUtils.Channel> channelsList = ChannelUtils.getAllChannels(requireContext());
                int position = 0;

                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<List<List<String>>> serialType = new TypeReference<>() {};
                List<List<String>> responseBody = objectMapper.readValue(response.body().string(), serialType);
                for (List<String> channelNames : responseBody) {
                    for (String channelName : channelNames) {
                        Optional<ChannelUtils.Channel> channelToMove = channelsList
                                .stream()
                                .filter(channel -> channel.title.equalsIgnoreCase(channelName) && channel.free)
                                .findFirst();

                        if (channelToMove.isPresent()) {
                            position++;
                            channelsList = ChannelUtils.moveChannelToPosition(requireContext(), channelToMove.get().number, position);
                            break;
                        }
                    }
                }

                ((OnboardingActivity)requireActivity()).nextScreen();
            }
        });
    }
}