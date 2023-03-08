package de.hahnphilipp.watchwith.player2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import de.hahnphilipp.watchwith.R;

public class TVWrapperActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tvwrapper);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.add(R.id.videoFragment, new TVPlayerFragment(),
                TVPlayerFragment.TAG);
        ft.commit();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // This part is necessary to ensure that getIntent returns the latest intent when
        // VideoExampleActivity is started. By default, getIntent() returns the initial intent
        // that was set from another activity that started VideoExampleActivity. However, we need
        // to update this intent when for example, user clicks on another video when the currently
        // playing video is in PIP mode, and a new video needs to be started.
        setIntent(intent);
    }

    public static boolean supportsPictureInPicture(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                context.getPackageManager().hasSystemFeature(
                        PackageManager.FEATURE_PICTURE_IN_PICTURE);
    }


}