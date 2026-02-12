package de.hahnphilipp.watchwithfritzbox.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

import de.hahnphilipp.watchwithfritzbox.R;

public class SetupIPFragment extends Fragment {

    public SetupIPFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_ip, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.setup_ip_address_input_et).requestFocus();
        ((EditText)view.findViewById(R.id.setup_ip_address_input_et)).setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                ((OnboardingActivity)requireActivity()).nextScreen();
                return true;
            }
            return false;
        });
    }

    public String getEnteredIp(){
        if(getView() == null) return null;
        EditText et = ((TextInputLayout)requireView().findViewById(R.id.setup_ip_address_input)).getEditText();
        return et.getText().toString().trim();
    }

}
