package de.hahnphilipp.watchwith.player2;

import android.os.Bundle;

import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.media.PlaybackBannerControlGlue;

//SEE https://qiita.com/Daigorian/items/2b0e1780339d02442c15
//https://github.com/android/tv-samples/blob/main/LeanbackShowcase/app/src/main/java/androidx/leanback/leanbackshowcase/app/media/VideoConsumptionExampleFragment.java
//https://developer.android.com/training/tv/playback/transport-controls
public class TVPlayerFragment extends VideoSupportFragment {

    public static final String TAG = "TVPlayerFragment";
    public PlaybackBannerControlGlue<VLCPlayerAdapter> controlGlue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VideoSupportFragmentGlueHost glueHost = new VideoSupportFragmentGlueHost(this);
        VLCPlayerAdapter playerAdapter = new VLCPlayerAdapter(getContext());

        controlGlue = new TVPlayerGlue<VLCPlayerAdapter>(getContext(), new int[]{1}, playerAdapter);
        controlGlue.setHost(glueHost);
        controlGlue.setTitle("Videotitel");
        controlGlue.setSubtitle("Untertitel");
        controlGlue.playWhenPrepared();

        playerAdapter.setDataSource("URL");
    }

    @Override
    public void onPause() {
        super.onPause();
        controlGlue.pause();
    }
}
