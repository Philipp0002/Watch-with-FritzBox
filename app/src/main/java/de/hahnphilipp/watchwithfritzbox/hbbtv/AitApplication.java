package de.hahnphilipp.watchwithfritzbox.hbbtv;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * https://developer.hbbtv.org/guide/launching-hbbtv-applications-from-a-broadcast-channel/introduction-to-ait-and-its-role-in-hbbtv/
 */
public class AitApplication {

    public long organisationId;
    public int applicationId;
    /**
     *     0x01 – AUTOSTART: The HbbTV application starts automatically when the viewer tunes into the channel.
     *     0x02 – PRESENT: The application will not start automatically but may continue to run if already running.
     *     0x03 – DESTROY: The application should be terminated if currently running.
     *     0x04 – KILL: Similar to DESTROY but ensures immediate termination.
     *     0x07 – DISABLED: The application shall not be started and attempts to start it shall fail.
     */
    public int controlCode;
    public List<AitDescriptor> descriptors;

    AitApplication() {}

    public static List<AitApplication> parseAitApplicationsFromHex(byte[] data) {
        List<AitApplication> apps = new ArrayList<>();
        int pos = 0;

        while (pos + 8 < data.length) { // mindestens ein vollständiger Header
            AitApplication app = new AitApplication();

            // 4 Byte organisation_id
            app.organisationId = ((data[pos] & 0xFFL) << 24)
                    | ((data[pos+1] & 0xFFL) << 16)
                    | ((data[pos+2] & 0xFFL) << 8)
                    | (data[pos+3] & 0xFFL);
            pos += 4;

            // 2 Byte application_id
            app.applicationId = ((data[pos] & 0xFF) << 8) | (data[pos+1] & 0xFF);
            pos += 2;

            // 1 Byte control_code
            app.controlCode = data[pos++] & 0xFF;

            // 2 Byte: upper 4 bits reserved, lower 12 bits = descriptors length
            int descriptorsLength = ((data[pos] & 0x0F) << 8) | (data[pos+1] & 0xFF);
            pos += 2;

            int end = pos + descriptorsLength;
            app.descriptors = new ArrayList<>();
            while (pos + 2 <= end && end <= data.length) {
                int tag = data[pos++] & 0xFF;
                int len = data[pos++] & 0xFF;

                if (pos + len > data.length) break; // Sicherheitsprüfung

                byte[] descData = Arrays.copyOfRange(data, pos, pos + len);
                AitDescriptor aitDescriptor = new AitDescriptor();
                aitDescriptor.data = descData;
                aitDescriptor.tag = tag;
                app.descriptors.add(aitDescriptor);

                pos += len;
            }

            apps.add(app);

            if (pos >= data.length) break;
        }

        return apps;
    }


}
