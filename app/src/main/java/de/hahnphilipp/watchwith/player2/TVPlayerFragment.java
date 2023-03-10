package de.hahnphilipp.watchwith.player2;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.app.VideoFragment;
import androidx.leanback.app.VideoFragmentGlueHost;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.DiffCallback;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;

import de.hahnphilipp.watchwith.utils.ChannelUtils;

//SEE https://qiita.com/Daigorian/items/2b0e1780339d02442c15
//https://github.com/android/tv-samples/blob/main/LeanbackShowcase/app/src/main/java/androidx/leanback/leanbackshowcase/app/media/VideoConsumptionExampleFragment.java
//https://developer.android.com/training/tv/playback/transport-controls
public class TVPlayerFragment extends VideoFragment {

    public static final String TAG = "TVPlayerFragment";
    public TVPlayerGlue<VLCPlayerAdapter> controlGlue;

    ArrayObjectAdapter otherChannelsAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VideoFragmentGlueHost glueHost = new VideoFragmentGlueHost(this);
        VLCPlayerAdapter playerAdapter = new VLCPlayerAdapter(getActivity());

        controlGlue = new TVPlayerGlue<>(getActivity(), new int[]{1}, playerAdapter);
        controlGlue.setHost(glueHost);
        controlGlue.setTitle("Videotitel");
        controlGlue.setSubtitle("Untertitel");
        controlGlue.playWhenPrepared();

        int lastChannelNumber = ChannelUtils.getLastSelectedChannel(getActivity());
        ChannelUtils.Channel channel = ChannelUtils.getChannelByNumber(getActivity(), 5);

        playerAdapter.setDataSource(channel.url);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ClassPresenterSelector)this.getAdapter().getPresenterSelector()).addClassPresenter(ListRow.class, new ListRowPresenter());
        otherChannelsAdapter = new ArrayObjectAdapter(new TVChannelCardPresenter());
        ListRow otherChannelsRow = new ListRow(1L, new HeaderItem("Andere Sender"), otherChannelsAdapter);
        ((ArrayObjectAdapter) this.getAdapter()).add(otherChannelsRow);
        otherChannelsAdapter.setItems(ChannelUtils.getAllChannels(getActivity()), new DiffCallback() {
            @Override
            public boolean areItemsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull Object oldItem, @NonNull Object newItem) {
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        controlGlue.pause();
    }
}
