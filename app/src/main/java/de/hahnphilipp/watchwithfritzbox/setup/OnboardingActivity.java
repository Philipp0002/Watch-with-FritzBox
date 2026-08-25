package de.hahnphilipp.watchwithfritzbox.setup;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.transition.MaterialSharedAxis;

import java.util.List;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.async.GetFritzInfo;
import de.hahnphilipp.watchwithfritzbox.player.TVPlayerActivity;
import de.hahnphilipp.watchwithfritzbox.rich.RichTvUtils;
import de.hahnphilipp.watchwithfritzbox.utils.ChannelUtils;
import de.hahnphilipp.watchwithfritzbox.utils.UIThread;

public class OnboardingActivity extends FragmentActivity {

    private Fragment currentFragment;
    private String ip;
    private boolean bottomBarShown = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentFragment instanceof SetupDVBSearchFragment
                        || currentFragment instanceof SetupFritzboxSearchFragment) {
                    Toast.makeText(OnboardingActivity.this, R.string.setup_search_no_back, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!previousScreen()) {
                    finish();
                }
            }
        });

        SharedPreferences sp = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        if (sp.contains("channels")) {
            startActivity(new Intent(OnboardingActivity.this, TVPlayerActivity.class));
            finish();
            overridePendingTransition(0, 0);
            return;
        }

        if (!isDirectToTV()) {
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(OnboardingActivity.this).create();
            alertDialog.setTitle(getString(R.string.not_tv_title));
            alertDialog.setMessage(getString(R.string.not_tv_msg));
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    (dialog, which) -> dialog.dismiss());
            alertDialog.show();
        }

        setContentView(R.layout.activity_onboarding);

        findViewById(R.id.onboarding_continue_button).setOnClickListener(v -> nextScreen());

        showWelcomeScreen();
    }

    public void enableNextButton(boolean enable) {
        MaterialButton button = findViewById(R.id.onboarding_continue_button);
        UIThread.run(() -> {
            button.setEnabled(enable);
            if (enable) {
                button.requestFocus();
            }
        });
    }

    public void showBottomBar(boolean show) {
        if (bottomBarShown != show) {
            bottomBarShown = show;
            View bottomBar = findViewById(R.id.linearLayout);

            Animation animation = AnimationUtils.loadAnimation(
                    this,
                    show ? R.anim.slide_up_from_down : R.anim.slide_down
            );
            animation.setFillAfter(true);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (!show) {
                        bottomBar.setVisibility(INVISIBLE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            UIThread.run(() -> {
                bottomBar.setVisibility(VISIBLE);
                bottomBar.startAnimation(animation);
            });
        }
    }

    public boolean previousScreen() {
        if (currentFragment instanceof SetupSearchVariantsFragment) {
            showSetupIPScreen();
            return true;
        } else if (currentFragment instanceof SetupIPFragment) {
            showWelcomeScreen();
            return true;
        } else if (currentFragment instanceof SetupDVBSearchFragment ||
                currentFragment instanceof SetupFritzboxSearchFragment) {
            showSearchVariantsScreen();
            return true;
        }
        return false;
    }

    public void nextScreen() {
        enableNextButton(false);
        if (currentFragment instanceof SetupWelcomeFragment) {
            showSetupIPScreen();
        } else if (currentFragment instanceof SetupIPFragment sif) {
            String enteredIp = sif.getEnteredIp();
            if (enteredIp == null || enteredIp.isEmpty()) {
                return;
            }
            this.ip = enteredIp;

            enableNextButton(false);
            final GetFritzInfo getFritzInfo = new GetFritzInfo(ip);
            getFritzInfo.callback = (error, isSatIpServer) -> {
                if (error) {
                    UIThread.run(() -> Toast.makeText(OnboardingActivity.this, R.string.setup_search_error_connect, Toast.LENGTH_LONG).show());
                } else {
                    if (isSatIpServer) {
                        showSearchVariantsScreen();
                    } else {
                        UIThread.run(() -> {
                            AlertDialog dialog = new MaterialAlertDialogBuilder(OnboardingActivity.this)
                                    .setTitle(R.string.setup_search_error_not_supported_title)
                                    .setMessage(R.string.setup_search_error_not_supported_msg)
                                    .setPositiveButton(R.string.setup_search_error_not_supported_continue, (dialog1, which) -> showSearchVariantsScreen())
                                    .setNegativeButton(R.string.setup_search_error_not_supported_abort, (dialog12, which) -> {
                                        enableNextButton(true);
                                    }).create();
                            dialog.show();
                            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            if(positiveButton != null) {
                                positiveButton.requestFocus();
                            }
                        });
                    }
                }
            };
            getFritzInfo.execute();
        } else if (currentFragment instanceof SetupSearchVariantsFragment svf) {
            SetupSearchVariantsFragment.SearchVariant searchVariant = svf.getSelectedVariant();
            if (searchVariant == SetupSearchVariantsFragment.SearchVariant.FRITZBOX) {
                showFritzboxSearchScreen(this.ip);
            } else if (searchVariant == SetupSearchVariantsFragment.SearchVariant.DVB) {
                showDVBSearchScreen(this.ip);
            }
        } else if (currentFragment instanceof SetupFritzboxSearchFragment) {
            showSortChannelsScreen();
        } else if (currentFragment instanceof SetupDVBSearchFragment) {
            showSortChannelsScreen();
        } else if (currentFragment instanceof SetupSortChannelsFragment) {
            showShowcaseGesturesScreen();
        } else if (currentFragment instanceof SetupShowGesturesFragment) {
            startActivity(new Intent(this, TVPlayerActivity.class));
            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void showShowcaseGesturesScreen() {
        showBottomBar(true);
        enableNextButton(true);
        showFragment(new SetupShowGesturesFragment());
    }

    private void showFritzboxSearchScreen(String ip) {
        showBottomBar(true);
        enableNextButton(false);
        showFragment(SetupFritzboxSearchFragment.newInstance(ip));
    }

    private void showDVBSearchScreen(String ip) {
        showBottomBar(true);
        enableNextButton(false);
        showFragment(SetupDVBSearchFragment.newInstance(ip));
    }

    private void showSortChannelsScreen() {
        showBottomBar(false);
        enableNextButton(false);
        showFragment(new SetupSortChannelsFragment());
    }

    private void showSearchVariantsScreen() {
        showBottomBar(false);
        enableNextButton(false);
        showFragment(new SetupSearchVariantsFragment());
    }

    private void showSetupIPScreen() {
        showBottomBar(true);
        enableNextButton(true);
        showFragment(new SetupIPFragment());
    }

    private void showWelcomeScreen() {
        showBottomBar(true);
        enableNextButton(true);
        showFragment(new SetupWelcomeFragment());
    }

    private void showFragment(Fragment fragment) {
        this.currentFragment = fragment;
        fragment.setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.X, true));
        fragment.setReturnTransition(new MaterialSharedAxis(MaterialSharedAxis.X, false));

        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container_view, fragment, null)
                .commitAllowingStateLoss();
    }


    private boolean isDirectToTV() {
        return (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK));
    }

}
