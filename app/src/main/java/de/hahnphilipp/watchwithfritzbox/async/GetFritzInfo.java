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
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                doc = builder.newDocument();
                Element e = doc.createElement("friendlyName");
                e.setTextContent("Test FritzBox Cable");
                doc.insertBefore(e, null);
                return;
            }
            URL url = new URL("http://"+ip+":49000/satipdesc.xml");
            URLConnection conn = url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.parse(conn.getInputStream());

            List<String> friendlyNames = new ArrayList<>();
            NodeList friendlyNameNodes = doc.getElementsByTagName("friendlyName");
            for (int i = 0; i < friendlyNameNodes.getLength(); i++) {
                friendlyNames.add(friendlyNameNodes.item(i).getTextContent());
            }

            if(callback != null)
                callback.onFetched(false, friendlyNames);
        } catch (Exception e) {
            e.printStackTrace();
            if(callback != null)
                callback.onFetched(true, null);
        }
    }

    public interface FritzInfoCallback {
        void onFetched(boolean error, List<String> friendlyNames);
    }

}
