package de.hahnphilipp.watchwithfritzbox.setup;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVPlayerActivity;

public class SetupIPActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(!isDirectToTV()){
            AlertDialog alertDialog = new AlertDialog.Builder(SetupIPActivity.this).create();
            alertDialog.setTitle(getString(R.string.not_tv_title));
            alertDialog.setMessage(getString(R.string.not_tv_msg));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        SharedPreferences sp = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        if(sp.contains("channels")){
            startActivity(new Intent(SetupIPActivity.this, TVPlayerActivity.class));
            finish();
            overridePendingTransition(0, 0);
            return;
        }
        setContentView(R.layout.setup_ip_activity);

        ((EditText)findViewById(R.id.setup_ip_address_input_et)).setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                startSetup();
                return true;
            }
            return false;
        });


        findViewById(R.id.setup_ip_continue_button).setOnClickListener(v -> startSetup());
    }

    public void startSetup() {
        EditText et = ((TextInputLayout)findViewById(R.id.setup_ip_address_input)).getEditText();

        if(et.getText().toString().trim().isEmpty()){
            return;
        }

        Intent mainIntent = new Intent(SetupIPActivity.this, SetupSearchActivity.class);
        mainIntent.putExtra("ip", et.getText().toString().trim());
        startActivity(mainIntent);
        finish();
        overridePendingTransition(0, 0);
    }

    private boolean isDirectToTV() {
        return(getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK));
    }

}
