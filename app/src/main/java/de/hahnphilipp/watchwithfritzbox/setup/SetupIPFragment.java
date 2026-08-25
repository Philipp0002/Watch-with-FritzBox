package de.hahnphilipp.watchwithfritzbox.setup;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ViewAnimator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import de.hahnphilipp.watchwithfritzbox.R;
import de.hahnphilipp.watchwithfritzbox.player.TVSettingsOverlayRecyclerAdapter;
import de.hahnphilipp.watchwithfritzbox.utils.TVSetting;
import de.hahnphilipp.watchwithfritzbox.utils.UpnpDeviceDiscovery;

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

        ArrayList<Object> foundDevices = new ArrayList<>();
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        TVSettingsOverlayRecyclerAdapter recyclerAdapter = new TVSettingsOverlayRecyclerAdapter(getContext(), foundDevices, recyclerView, false);
        final LinearLayoutManager llm = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(recyclerAdapter);

        UpnpDeviceDiscovery discovery = new UpnpDeviceDiscovery(requireContext());
        String targetUrn = "urn:ses-com:device:SatIPServer:1";
        discovery.discoverDeviceByUrn(targetUrn, 5000, new UpnpDeviceDiscovery.DiscoveryCallback() {
            @Override
            public void onDeviceFound(final String xmlUrl, final String server) {
                URI uri = URI.create(xmlUrl);
                String ipAddress = uri.getHost();
                foundDevices.add(new TVSetting(server, ipAddress, TVSetting.NavigationIcon.CHEVRON, R.drawable.round_router, () -> {
                    ((EditText)view.findViewById(R.id.setup_ip_address_input_et)).setText(ipAddress);
                    ((OnboardingActivity) requireActivity()).nextScreen();
                }));
                recyclerAdapter.notifyItemInserted(foundDevices.size() - 1);
                Log.d("UPnP", "Gerät gefunden unter: " + xmlUrl);
                view.post(() -> ((ViewAnimator)view.findViewById(R.id.devices_view_animator)).setDisplayedChild(1));
            }

            @Override
            public void onTimeout() {
                //runOnUiThread(() -> {
                    Log.d("UPnP", "Suche beendet. Kein Gerät gefunden.");
                //});
            }

            @Override
            public void onError(final Exception e) {
                //runOnUiThread(() -> {
                    Log.e("UPnP", "Fehler bei der Suche: " + e.getMessage());
                //});
            }
        });

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
