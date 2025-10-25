package de.hahnphilipp.watchwithfritzbox.player;

import android.net.Uri;

import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import java.io.InputStream;

public class InputStreamDataSource implements DataSource {
    private final InputStream in;

    public InputStreamDataSource(InputStream in) {
        this.in = in;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {}

    @Override
    public long open(DataSpec dataSpec) {
        return C.LENGTH_UNSET;
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) {
        try {
            return in.read(buffer, offset, readLength);
        } catch (Exception e) {
            return C.RESULT_END_OF_INPUT;
        }
    }

    @Override
    public Uri getUri() {
        return null;
    }

    @Override
    public void close() {
        try {
            in.close();
        } catch (Exception ignored) {}
    }
}
