package com.socrata.datasync;

import java.io.IOException;
import java.io.InputStream;

public class SizeCountingInputStream extends InputStream {
    private long total = 0;
    private final InputStream underlying;

    public SizeCountingInputStream(InputStream underlying) {
        this.underlying = underlying;
    }

    @Override
    public int read() throws IOException {
        int result = underlying.read();
        if(result != -1) total += 1;
        return result;
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        int result = underlying.read(buf, off, len);
        if(result != -1) total += result;
        return result;
    }

    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    public long getTotal() {
        return total;
    }
}
