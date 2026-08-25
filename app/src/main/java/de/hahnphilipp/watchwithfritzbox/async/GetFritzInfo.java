package de.hahnphilipp.watchwithfritzbox.async;

import android.os.AsyncTask;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


public class GetFritzInfo extends AsyncTask<Void, Void, Void> {

    public String ip;
    public FritzInfoCallback callback;

    public Document doc;

    public GetFritzInfo(String ip) {
        this.ip = ip;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        runFetch();
        return null;
    }

    public void runFetch() {
        try {
            //MOCK AN INVALID FRITZBOX CABLE FOR AMAZON TEST CENTER
            if(ip.contains("hahnphilipp.de")){
                if(callback != null) {
                    callback.onFetched(false, true);
                }
            } else {
                URL url = new URL("http://" + ip + ":49000/satipdesc.xml");
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(3000);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.parse(conn.getInputStream());


                NodeList deviceTypeNodes = doc.getElementsByTagName("deviceType");
                for (int i = 0; i < deviceTypeNodes.getLength(); i++) {
                    String value = deviceTypeNodes.item(i).getTextContent();
                    if(value.contains("urn:ses-com:device:SatIPServer:1")) {
                        if(callback != null) {
                            callback.onFetched(false, true);
                        }
                        return;
                    }
                }
            }

            if(callback != null)
                callback.onFetched(false, false);
        } catch (Exception e) {
            e.printStackTrace();
            if(callback != null)
                callback.onFetched(true, false);
        }
    }

    public interface FritzInfoCallback {
        void onFetched(boolean error, boolean isSatIpServer);
    }

}
