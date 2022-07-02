package de.hahnphilipp.watchwith.utils;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import java.util.ArrayList;
import java.util.List;

public class DialogFragment extends GuidedStepSupportFragment {

    public String title;
    public String description;
    public ArrayList<GuidedAction> guidedActions = new ArrayList<>();
    public DialogFragmentCallback callback;

    public static DialogFragment newInstance() {

        Bundle args = new Bundle();

        DialogFragment fragment = new DialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title,
                description,
                "ffff", null);
        return super.onCreateGuidance(savedInstanceState);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actionslist, Bundle savedInstanceState) {

        for(GuidedAction guidedAction : guidedActions){
            actionslist.add(guidedAction);
        }

    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        callback.onSelect(action);
    }

}
