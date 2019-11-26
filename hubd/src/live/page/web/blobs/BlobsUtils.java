/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.blobs;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import com.mongodb.client.MongoCollection;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.http.HttpClient;
import live.page.web.utils.http.LimitedStream;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;

public class BlobsUtils {


	/**
	 * Put file in database
	 */
	public static String putFile(File file, String user_id, String ip, String name, String type) {
		try {
			autoRotate(file);
			Json blob = new Json("user", user_id).put("ip", ip).put("type", type).put("name", name).put("size", file.length()).put("date", new Date());
			Db.save("BlobFiles", blob);
			FileInputStream bin = new FileInputStream(file);
			byte[] buffer = new byte[(int) Math.min(file.length(), Settings.CHUNCK_SIZE)];
			int order = 0;
			MongoCollection<Json> blobChunks = Db.getDb("BlobChunks");
			while (bin.read(buffer) != -1) {
				blobChunks.insertOne(new Json("_id", Db.getKey()).put("b", buffer).put("o", order++).put("f", blob.getId()));
			}
			bin.close();
			return blob.getId();
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Rotate file with Exif data in the correct order
	 */
	private static void autoRotate(File file) {
		try {
			BufferedImage originalImage = ImageIO.read(file);

			if (originalImage == null) {
				return;
			}

			Metadata metadata = ImageMetadataReader.readMetadata(file);
			Iterator<ExifIFD0Directory> exifIFD0Directory = metadata.getDirectoriesOfType(ExifIFD0Directory.class).iterator();
			Iterator<JpegDirectory> jpegDirectory = metadata.getDirectoriesOfType(JpegDirectory.class).iterator();
			if (!jpegDirectory.hasNext()) {
				return;
			}
			if (!exifIFD0Directory.hasNext()) {
				return;
			}

			int orientation = 1;
			try {
				orientation = exifIFD0Directory.next().getInt(ExifIFD0Directory.TAG_ORIENTATION);
			} catch (Exception e) {
			}
			JpegDirectory jpg = jpegDirectory.next();
			int width = jpg.getImageWidth();
			int height = jpg.getImageHeight();

			AffineTransform affineTransform = new AffineTransform();

			switch (orientation) {
				case 1:
					return;
				case 2: // Flip X
					affineTransform.scale(-1.0, 1.0);
					affineTransform.translate(-width, 0);
					break;
				case 3: // PI rotation
					affineTransform.translate(width, height);
					affineTransform.rotate(Math.PI);
					break;
				case 4: // Flip Y
					affineTransform.scale(1.0, -1.0);
					affineTransform.translate(0, -height);
					break;
				case 5: // - PI/2 and Flip X
					affineTransform.rotate(-Math.PI / 2);
					affineTransform.scale(-1.0, 1.0);
					break;
				case 6: // -PI/2 and -width
					affineTransform.translate(height, 0);
					affineTransform.rotate(Math.PI / 2);
					break;
				case 7: // PI/2 and Flip
					affineTransform.scale(-1.0, 1.0);
					affineTransform.translate(-height, 0);
					affineTransform.translate(0, width);
					affineTransform.rotate(3 * Math.PI / 2);
					break;
				case 8: // PI / 2
					affineTransform.translate(0, width);
					affineTransform.rotate(3 * Math.PI / 2);
					break;
				default:
					break;
			}

			AffineTransformOp affineTransformOp = new AffineTransformOp(affineTransform, AffineTransformOp.TYPE_BILINEAR);
			BufferedImage destinationImage = new BufferedImage(originalImage.getHeight(), originalImage.getWidth(), originalImage.getType());
			destinationImage = affineTransformOp.filter(originalImage, destinationImage);
			ImageIO.write(destinationImage, "jpg", file);

		} catch (Exception e) {

		}
	}


	/**
	 * Download external image and return file and contentType
	 */
	public static Blob downloadAsBlob(String url) {
		CloseableHttpClient httpclient = null;
		HttpGet request = null;
		CloseableHttpResponse response = null;
		HttpEntity entity = null;
		InputStream in = null;
		File file = null;
		try {

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, new TrustManager[]{new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
			}
			}, new java.security.SecureRandom());
			httpclient = HttpClients.custom()
					.setRedirectStrategy(LaxRedirectStrategy.INSTANCE)
					.setDefaultRequestConfig(RequestConfig.custom()
							.setConnectTimeout(10000)
							.setContentCompressionEnabled(true)
							.setNormalizeUri(true)
							.setMaxRedirects(20)
							.setConnectionRequestTimeout(10000)
							.setSocketTimeout(10000).build()
					).setSSLContext(sc).setSSLHostnameVerifier((hostname, session) -> true).build();

			file = File.createTempFile("download.", ".file");
			file.deleteOnExit();

			request = new HttpGet(HttpClient.encodeUrlPath(url));

			response = httpclient.execute(request);
			if (response.getStatusLine().getStatusCode() != 200) {
				file.delete();
				return null;
			}
			String contentType = response.getLastHeader(HttpHeaders.CONTENT_TYPE).getValue();
			entity = response.getEntity();
			in = LimitedStream.asLimited(entity.getContent(), 10);

			FileUtils.copyInputStreamToFile(in, file);

			if (file.length() == 0) {
				file.delete();
				return null;
			}
			return new Blob(file, contentType);
		} catch (Exception e) {
			if (Fx.IS_DEBUG) {
				Fx.log("error downloadAsBlob");
			}
			try {
				file.delete();
			} catch (Exception ex) {
			}
			return null;
		} finally {
			try {
				in.close();
			} catch (Exception e) {
			}
			try {
				response.close();
			} catch (Exception e) {
			}
			try {
				EntityUtils.consume(entity);
			} catch (Exception e) {
			}
			try {
				request.completed();
			} catch (Exception e) {
			}
			try {
				httpclient.close();
			} catch (Exception e) {
			}
		}
	}


	/**
	 * Download external image and resize big images
	 */
	public static String downloadToDb(String url, int maxwidth) {
		return downloadToDb(url, new Json(), maxwidth);
	}

	/**
	 * Download external image and put in Db and return ID from DB
	 */
	public static String downloadToDb(String url) {
		return downloadToDb(url, new Json(), -1);
	}

	/**
	 * Download external image and put in Db with initial infos
	 */
	public static String downloadToDb(String url, Json blob, int maxwidth) {

		try {
			Blob download = downloadAsBlob(url);
			if (download == null) {
				return null;
			}
			if (maxwidth > 0) {

				BufferedImage image = ImageIO.read(download.file);
				if (image.getHeight() > maxwidth || image.getWidth() > maxwidth) {
					ImageInputStream iis = ImageIO.createImageInputStream(download.file);
					String format = ImageIO.getImageReaders(iis).next().getFormatName().toLowerCase();
					iis.close();
					image = Thumbnailer.resize(image, maxwidth, maxwidth * image.getHeight() / image.getWidth(), format);
					ImageIO.write(image, format, download.file);
				}
			}

			blob.put("type", download.contentType).put("size", download.file.length()).put("date", new Date());

			Db.save("BlobFiles", blob);

			FileInputStream bin = new FileInputStream(download.file);

			byte[] buffer = new byte[(int) Math.min(download.file.length(), Settings.CHUNCK_SIZE)];
			int order = 0;
			MongoCollection<Json> blobChunks = Db.getDb("BlobChunks");
			while (bin.read(buffer) != -1) {
				blobChunks.insertOne(new Json("_id", Db.getKey()).put("b", buffer).put("o", order++).put("f", blob.getId()));
			}
			bin.close();
			download.file.delete();
			return blob.getId();
		} catch (Exception e) {
			e.printStackTrace();

		}
		return null;
	}


	/**
	 * Get external image size
	 */
	public static int[] getSize(String url) {

		Blob blob = downloadAsBlob(url);
		if (blob == null) {
			return new int[]{};
		}
		try {
			BufferedImage image = ImageIO.read(blob.file);
			blob.file.delete();
			return new int[]{image.getWidth(), image.getHeight()};
		} catch (Exception e) {
			return new int[]{};
		}
	}

}
