package de.hahnphilipp.watchwithfritzbox.setup;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.GetFritzInfo;
import de.hahnphilipp.watchwithfritzbox.async.GetPlaylists;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SetupSearchActivity extends AppCompatActivity {

    private String ip;
    private List<ChannelUtils.Channel> channelList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_search);

        ip = getIntent().getStringExtra("ip");

        final GetPlaylists getPlaylists = new GetPlaylists(ip);
        getPlaylists.callback = new GetPlaylists.GetPlaylistResult() {
            @Override
            public void onTypeLoaded(ChannelUtils.ChannelType type, int channelAmount) {
                runOnUiThread(() -> {
                    switch (type) {
                        case SD ->
                                ((TextView) findViewById(R.id.setup_sd_search_text)).setText(getString(R.string.setup_search_sd_result, channelAmount));
                        case HD ->
                                ((TextView) findViewById(R.id.setup_hd_search_text)).setText(getString(R.string.setup_search_hd_result, channelAmount));
                        case RADIO ->
                                ((TextView) findViewById(R.id.setup_radio_search_text)).setText(getString(R.string.setup_search_radio_result, channelAmount));
                    }
                });
            }

            @Override
            public void onAllLoaded(boolean error, List<ChannelUtils.Channel> channelList) {
                runOnUiThread(() -> {
                    if (error) {
                        Toast.makeText(SetupSearchActivity.this, R.string.setup_search_error, Toast.LENGTH_LONG).show();
                        startActivity(new Intent(SetupSearchActivity.this, SetupIPActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                    } else {
                        findViewById(R.id.setup_search_progressBar).setVisibility(View.INVISIBLE);
                        findViewById(R.id.setup_search_continue_button).setVisibility(View.VISIBLE);
                        SetupSearchActivity.this.channelList = channelList;
                    }
                });

            }
        };

        final GetFritzInfo getFritzInfo = new GetFritzInfo(ip);
        getFritzInfo.callback = (error, friendlyNames) -> {
            if (error) {
                runOnUiThread(() -> {
                    Toast.makeText(SetupSearchActivity.this, R.string.setup_search_error_connect, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(SetupSearchActivity.this, SetupIPActivity.class));
                    finish();
                    overridePendingTransition(0, 0);
                });
            } else {
                boolean supportedFritzBox = friendlyNames.stream()
                        .map(String::toLowerCase)
                        .anyMatch(s -> s.contains("cable") || s.contains("dvbc") || s.contains("dvb-c"));

                if (supportedFritzBox) {
                    getPlaylists.execute();
                } else {
                    runOnUiThread(() -> {
                        AlertDialog dialog = new AlertDialog.Builder(SetupSearchActivity.this)
                                .setTitle(R.string.setup_search_error_not_supported_title)
                                .setMessage(R.string.setup_search_error_not_supported_msg)
                                .setPositiveButton(R.string.setup_search_error_not_supported_continue, (dialog1, which) -> getPlaylists.execute())
                                .setNegativeButton(R.string.setup_search_error_not_supported_abort, (dialog12, which) -> {
                                    startActivity(new Intent(SetupSearchActivity.this, SetupIPActivity.class));
                                    finish();
                                    overridePendingTransition(0, 0);
                                }).create();
                        dialog.show();
                    });
                }
            }
        };
        getFritzInfo.execute();

        findViewById(R.id.setup_search_continue_button).setOnClickListener(v -> {
            ChannelUtils.setChannels(SetupSearchActivity.this, channelList);

            setContentView(R.layout.activity_setup_order);
            findViewById(R.id.setup_order_no_button).setOnClickListener(view -> skipToNext());
            findViewById(R.id.setup_order_yes_button).setOnClickListener(view -> presortChannels());
        });

    }

    public void presortChannels() {
        findViewById(R.id.setup_order_no_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.setup_order_yes_button).setVisibility(View.INVISIBLE);
        findViewById(R.id.setup_order_progressBar).setVisibility(View.VISIBLE);

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
                runOnUiThread(() -> {
                    Toast.makeText(SetupSearchActivity.this, R.string.setup_order_error, Toast.LENGTH_LONG).show();

                    findViewById(R.id.setup_order_no_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.setup_order_yes_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.setup_order_progressBar).setVisibility(View.GONE);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ArrayList<ChannelUtils.Channel> channelsList = ChannelUtils.getAllChannels(SetupSearchActivity.this);
                int position = 0;

                ObjectMapper objectMapper = new ObjectMapper();
                TypeReference<List<List<String>>> serialType = new TypeReference<>() {};
                List<List<String>> responseBody = objectMapper.readValue(response.body().string(), serialType);
                for (List<String> channelNames : responseBody) {
                    for (String channelName : channelNames) {
                        Optional<ChannelUtils.Channel> channelToMove = channelsList
                                .stream()
                                .filter(channel -> channel.title.equalsIgnoreCase(channelName))
                                .findFirst();

                        if (channelToMove.isPresent()) {
                            position++;
                            channelsList = ChannelUtils.moveChannelToPosition(SetupSearchActivity.this, channelToMove.get().number, position);
                            break;
                        }
                    }
                }

                skipToNext();
            }
        });
    }

    public void skipToNext() {
        startActivity(new Intent(SetupSearchActivity.this, ShowcaseGesturesActivity.class));
        finish();
        overridePendingTransition(0, 0);
    }
}
