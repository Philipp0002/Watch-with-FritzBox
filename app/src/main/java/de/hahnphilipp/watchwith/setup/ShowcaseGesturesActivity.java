package de.hahnphilipp.watchwith.setup;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.hahnphilipp.watchwith.R;
import de.hahnphilipp.watchwith.player.TVPlayerActivity;
import de.hahnphilipp.watchwith.player2.TVWrapperActivity;

public class ShowcaseGesturesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        setContentView(R.layout.showcase_gestures_activity);


        findViewById(R.id.showcase_continue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShowcaseGesturesActivity.this, TVWrapperActivity.class));
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }
}
