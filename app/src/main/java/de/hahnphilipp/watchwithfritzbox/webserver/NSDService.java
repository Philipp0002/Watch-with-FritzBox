package de.hahnphilipp.watchwithfritzbox.webserver;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hahnphilipp.watchwithfritzbox.utils.IPUtils;

/**
 * mDNS - NSD (Network Service Discovery)
 */
public class NSDService {

    private HashSet<String> discoveredIPs = new HashSet<>();

    private Context context;
    private static NSDService instance;
    private NsdManager.RegistrationListener registrationListener;
    private NsdManager.DiscoveryListener discoveryListener;

    private NSDService(Context context) {
        this.context = context;
    }

    public static NSDService getInstance() {
        return instance;
    }

    public static NSDService createInstance(Context context) {
        instance = new NSDService(context);
        return instance;
    }

    public void unregisterService() {
        NsdManager nsdManager = getSystemService(context, NsdManager.class);
        if(registrationListener != null) {
            nsdManager.unregisterService(registrationListener);
            registrationListener = null;
        }
        if(discoveryListener != null) {
            nsdManager.stopServiceDiscovery(discoveryListener);
            discoveryListener = null;
        }
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName("WatchWithFritzBox");
        serviceInfo.setServiceType("_http._tcp."); // Typ des Dienstes (Standard: HTTP)
        serviceInfo.setPort(port); // Port, auf dem der Dienst läuft

        NsdManager nsdManager = getSystemService(context, NsdManager.class);

        nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener = new NsdManager.RegistrationListener() {
                    @Override
                    public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                        Log.d("NSD", "Service registered: " + nsdServiceInfo.getServiceName());
                    }

                    @Override
                    public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e("NSD", "Service registration failed: " + errorCode);
                    }

                    @Override
                    public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                        Log.d("NSD", "Service unregistered: " + serviceInfo.getServiceName());
                    }

                    @Override
                    public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e("NSD", "Service unregistration failed: " + errorCode);
                    }
                });
    }

    public void discoverServices() {
        NsdManager nsdManager = getSystemService(context, NsdManager.class);

        nsdManager.discoverServices(
                "_http._tcp.",
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener = new NsdManager.DiscoveryListener() {
                    @Override
                    public void onDiscoveryStarted(String serviceType) {
                        Log.d("NSD", "Discovery started for service type: " + serviceType);
                    }

                    @Override
                    public void onServiceFound(NsdServiceInfo serviceInfo) {
                        Log.d("NSD", "Service found: " + serviceInfo.getServiceName());
                        // Optional: Prüfen, ob der gefundene Dienst der eigene ist
                        if(serviceInfo.getServiceName().equals("WatchWithFritzBox")) {
                            resolveService(serviceInfo);
                        }
                    }

                    @Override
                    public void onServiceLost(NsdServiceInfo serviceInfo) {
                        Log.d("NSD", "Service lost: " + serviceInfo.getServiceName());
                    }

                    @Override
                    public void onDiscoveryStopped(String serviceType) {
                        Log.d("NSD", "Discovery stopped for service type: " + serviceType);
                    }

                    @Override
                    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                        Log.e("NSD", "Discovery start failed: " + errorCode);
                        nsdManager.stopServiceDiscovery(this);
                    }

                    @Override
                    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                        Log.e("NSD", "Discovery stop failed: " + errorCode);
                        nsdManager.stopServiceDiscovery(this);
                    }
                });
    }

    public void resolveService(NsdServiceInfo serviceInfo) {
        NsdManager nsdManager = getSystemService(context, NsdManager.class);

        String ownIP = IPUtils.getIPAddress(true);

        nsdManager.resolveService(
                serviceInfo,
                new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        Log.e("NSD", "Resolve failed: " + errorCode);
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo resolvedServiceInfo) {
                        Log.d("NSD", "Service resolved: " + resolvedServiceInfo);
                        String host = resolvedServiceInfo.getHost().getHostAddress();
                        if(host.equalsIgnoreCase(ownIP)) {
                            return;
                        }
                        discoveredIPs.add(host);
                        Log.d("NSD", "Host: " + host);
                    }
                });
    }

}
