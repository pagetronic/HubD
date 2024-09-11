package live.page.hubd.system.cosmetic.compress;

import live.page.hubd.blobs.Thumbnailer;
import live.page.hubd.system.utils.Fx;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;

/**
 * Brotli and Gzip compression tools
 */
public class Compressors {


    /**
     * Gzip algorithm compression
     *
     * @param data to compress
     * @return byte array of compressed data
     */
    public static byte[] gzipCompressor(byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            GzipParameters params = new GzipParameters();
            params.setCompressionLevel(Deflater.BEST_COMPRESSION);
            GzipCompressorOutputStream gzipped = new GzipCompressorOutputStream(out, params);
            gzipped.write(data);
            gzipped.close();
            out.close();
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    public static byte[] brCompressor(byte[] data) {
        try {
            File in = File.createTempFile("brotli", ".in");
            File out = File.createTempFile("brotli", ".out");
            FileUtils.writeByteArrayToFile(in, data);
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("brotli", "-f", "-o", out.getAbsolutePath(), in.getAbsolutePath());
            try {
                Process process = processBuilder.start();
                if (Thumbnailer.hasError(process.getErrorStream())) {
                    Fx.log("No command brotli");
                    return null;
                }
                return Files.readAllBytes(out.toPath());
            } finally {
                in.delete();
                out.delete();
            }
        } catch (Exception ignore) {

        }
        return null;
    }


}
