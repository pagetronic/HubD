/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.hubd.utils.http;

import live.page.hubd.utils.Fx;

import java.io.IOException;
import java.io.InputStream;

/**
 * Limit the size of an InputStream
 */
public class LimitedStream extends InputStream {

    private final InputStream original;
    private final long maxSize;
    private long total;


    /**
     * Constructor with size limitation
     *
     * @param in     input stream to limit
     * @param octets maximum octets
     * @return the inputStream limited
     */
    private LimitedStream(InputStream in, long octets) {
        this.original = in;
        this.maxSize = octets;
    }

    /**
     * Get an InputStream with size limitation
     *
     * @param in            input stream to limit
     * @param maxMegaOctets maximum mega octets
     * @return the inputStream limited
     */
    public static InputStream asLimited(InputStream in, int maxMegaOctets) {
        return new LimitedStream(in, maxMegaOctets * 1024L * 1024L);
    }

    /**
     * Get an InputStream with size limitation
     *
     * @param in     input stream to limit
     * @param octets maximum octets
     * @return the inputStream limited
     */
    public static InputStream asLimited(InputStream in, long octets) {
        return new LimitedStream(in, octets);
    }

    @Override
    public int read() throws IOException {
        int i = original.read();
        if (i >= 0) {
            incrementCounter(1);
        }
        return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i = original.read(b, off, len);
        if (i >= 0) {
            incrementCounter(i);
        }
        return i;
    }


    private void incrementCounter(int size) throws IOException {
        total += size;
        if (total > maxSize) {
            if (Fx.IS_DEBUG) {
                Fx.log("Stream exceeded maximum size");
            }
            throw new IOException("Stream exceeded maximum size");
        }
    }

}
