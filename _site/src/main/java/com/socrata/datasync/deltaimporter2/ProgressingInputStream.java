package com.socrata.datasync.deltaimporter2;

import java.io.IOException;
import java.io.InputStream;

public abstract class ProgressingInputStream extends InputStream {
    private final InputStream underlying;
    private long count = 0L;
    private long lastSentAt = 0L;

    public ProgressingInputStream(InputStream underlying) {
        this.underlying = underlying;
    }

    @Override
    public int read() throws IOException {
        int r = underlying.read();
        if(r != -1) advanceBy(1);
        return r;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int r = underlying.read(buf, off, len);
        if(r != -1) advanceBy(r);
        return r;
    }

    @Override
    public void close() throws IOException {
        underlying.close();
    }

    private void advanceBy(int howMuch) {
        count += howMuch;

        long now = System.currentTimeMillis();
        if(now >= lastSentAt + 5000) {
            progress(count);
            lastSentAt = now;
        }
    }

    protected abstract void progress(long count);
}
