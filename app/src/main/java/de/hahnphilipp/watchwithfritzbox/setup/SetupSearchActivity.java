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
import com.w3ma.m3u8parser.data.Track;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    String ip;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_search);

        ip = getIntent().getStringExtra("ip");

        final GetPlaylists getPlaylists = new GetPlaylists();
        getPlaylists.ip = ip;
        getPlaylists.futureRunSD = () -> runOnUiThread(() -> ((TextView) findViewById(R.id.setup_sd_search_text)).setText((getPlaylists.playlistSD.getTrackSetMap().get("")).size() + " " + getString(R.string.setup_search_sd_result)));
        getPlaylists.futureRunHD = () -> runOnUiThread(() -> ((TextView) findViewById(R.id.setup_hd_search_text)).setText((getPlaylists.playlistHD.getTrackSetMap().get("")).size() + " " + getString(R.string.setup_search_hd_result)));
        getPlaylists.futureRunRadio = () -> runOnUiThread(() -> ((TextView) findViewById(R.id.setup_radio_search_text)).setText((getPlaylists.playlistRadio.getTrackSetMap().get("")).size() + " " + getString(R.string.setup_search_radio_result)));
        getPlaylists.futureRunFinished = () -> runOnUiThread(() -> {
            if (getPlaylists.error) {
                Toast.makeText(SetupSearchActivity.this, R.string.setup_search_error, Toast.LENGTH_LONG).show();
                startActivity(new Intent(SetupSearchActivity.this, SetupIPActivity.class));
                finish();
                overridePendingTransition(0, 0);
            } else {
                findViewById(R.id.setup_search_progressBar).setVisibility(View.INVISIBLE);
                findViewById(R.id.setup_search_continue_button).setVisibility(View.VISIBLE);
            }
        });


        final GetFritzInfo getFritzInfo = new GetFritzInfo();
        getFritzInfo.ip = ip;
        getFritzInfo.futureRunFinished = () -> {
            if (getFritzInfo.error) {
                runOnUiThread(() -> {
                    Toast.makeText(SetupSearchActivity.this, R.string.setup_search_error_connect, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(SetupSearchActivity.this, SetupIPActivity.class));
                    finish();
                    overridePendingTransition(0, 0);
                });
            } else {
                Document doc = getFritzInfo.doc;

                boolean supportedFritzBox = false;
                NodeList friendlyNames = doc.getElementsByTagName("friendlyName");
                for (int i = 0; i < friendlyNames.getLength(); i++) {
                    if (friendlyNames.item(i).getTextContent().toLowerCase().contains("cable") ||
                            friendlyNames.item(i).getTextContent().toLowerCase().contains("dvbc") ||
                            friendlyNames.item(i).getTextContent().toLowerCase().contains("dvb-c")
                    ) {
                        supportedFritzBox = true;
                        break;
                    }
                }

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

            int channelNumber = 1;

            ArrayList<ChannelUtils.Channel> channels = new ArrayList<>();
            for (Track t : getPlaylists.playlistHD.getTrackSetMap().get("")) {
                ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), t.getUrl(), ChannelUtils.ChannelType.HD);
                channels.add(channel);
                channelNumber++;
            }

            for (Track t : getPlaylists.playlistSD.getTrackSetMap().get("")) {
                ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), t.getUrl(), ChannelUtils.ChannelType.SD);
                channels.add(channel);
                channelNumber++;
            }

            Collections.sort(channels, new Comparator<ChannelUtils.Channel>() {
                @Override
                public int compare(ChannelUtils.Channel channel, ChannelUtils.Channel t1) {
                    return channel.url.compareTo(t1.url);
                }
            });

            for (Track t : getPlaylists.playlistRadio.getTrackSetMap().get("")) {
                ChannelUtils.Channel channel = new ChannelUtils.Channel(channelNumber, t.getExtInfo().getTitle(), t.getUrl(), ChannelUtils.ChannelType.RADIO);
                channels.add(channel);
                channelNumber++;
            }

            ChannelUtils.setChannels(SetupSearchActivity.this, channels);

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
