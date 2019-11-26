/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.system.cosmetic;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.io.FileUtils;
import org.meteogroup.jbrotli.Brotli;
import org.meteogroup.jbrotli.BrotliCompressor;
import org.meteogroup.jbrotli.BrotliException;
import org.meteogroup.jbrotli.libloader.BrotliLibraryLoader;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;

/**
 * Brotli and Gzip compression tools
 */
public class Compressors {

	/**
	 * Brotli algorithm compression
	 *
	 * @param data to compress
	 * @return byte array of compressed data
	 */
	public static byte[] brotliCompressor(byte[] data) {
		if (data == null) {
			return null;
		}
		try {
			BrotliLibraryLoader.loadBrotli();
			BrotliCompressor compressor = new BrotliCompressor();
			ByteBuffer compressedBuf = ByteBuffer.allocate(1024 * 1024);
			int outLength = compressor.compress(Brotli.DEFAULT_PARAMETER, ByteBuffer.wrap(data), compressedBuf);
			return java.util.Arrays.copyOfRange(compressedBuf.array(), 0, outLength);
		} catch (BrotliException e) {
			e.printStackTrace();
		}
		return null;
	}

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
			GzipCompressorOutputStream gzipper = new GzipCompressorOutputStream(out, params);
			gzipper.write(data);
			gzipper.close();
			out.close();
			return out.toByteArray();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * yuiCompressor compression algorithm
	 *
	 * @param css cascading style sheets content to compress
	 * @return String of compressed css
	 */
	public static String compressCss(String css) throws IOException {
		File tempfile = File.createTempFile("uiCompress", ".css");
		tempfile.deleteOnExit();
		FileReader reader = new FileReader(tempfile);
		StringWriter writer = new StringWriter();
		try {
			FileUtils.write(tempfile, css);
			new CssCompressor(reader).compress(writer, Integer.MAX_VALUE);
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return css;
		} finally {
			reader.close();
			writer.close();
			tempfile.delete();
		}
	}

	/**
	 * yuiCompressor compression algorithm
	 *
	 * @param js JavaScript content to compress
	 * @return String of compressed JavaScript
	 */
	public static String compressJs(String js) throws IOException {
		File tempfile = File.createTempFile("uiCompress", ".js");
		tempfile.deleteOnExit();
		FileReader freader = new FileReader(tempfile);
		StringWriter writer = new StringWriter();
		try {
			FileUtils.write(tempfile, js);
			new JavaScriptCompressor(freader, new YuiCompressorErrorReporter()).compress(writer, -1, false, true, false, true);
			return writer.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return js;
		} finally {
			freader.close();
			writer.close();
			tempfile.delete();
		}
	}

	/**
	 * YUI Error reported, needed to verify good compression
	 */
	public static class YuiCompressorErrorReporter implements ErrorReporter {

		@Override
		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
			//if (line < 0) {
			//System.err.println("Warning: " + message);
			//} else {
			//System.err.println("Warning: " + line + ':' + lineOffset + ':' + message);
			//}
		}

		@Override
		public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
			if (line < 0) {
				System.err.println("Error: " + message);
			} else {
				System.err.println("Error: " + line + ':' + lineOffset + ':' + message + ':' + sourceName + ":" + lineSource);
			}
		}

		@Override
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
			error(message, sourceName, line, lineSource, lineOffset);
			return new EvaluatorException(message);
		}

	}
}
