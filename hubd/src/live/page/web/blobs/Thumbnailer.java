/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.blobs;

import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import live.page.web.system.Settings;
import live.page.web.system.db.Db;
import live.page.web.system.db.IndexBuilder;
import live.page.web.system.json.Json;
import live.page.web.utils.Fx;
import live.page.web.utils.http.HttpClient;
import net.sf.image4j.codec.ico.ICODecoder;
import org.apache.commons.io.FileUtils;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Thumbnail service
 */
public class Thumbnailer {

	/**
	 * Make a thumbnail from URL or DB image
	 */
	public static Json makeDb(String blob, String type, int width, int height) {
		try {
			if (blob.matches("^https?://.*")) {
				Blob download = BlobsUtils.downloadAsBlob(blob);

				if ((download == null || download.file.length() < 100L || download.contentType.contains("html")) && blob.endsWith("/favicon.ico")) {
					if (download != null) {
						download.file.delete();
					}
					download = getFavicon(blob);
				}

				if (download == null) {
					return null;
				}

				Json cache = makeFile(download.file, blob, type, width, height);

				download.file.delete();
				return cache;

			} else {

				Json file = Db.getDb("BlobFiles").find(Filters.eq("_id", blob)).first();

				if (file.getString("type").startsWith("image/")) {

					Bson filter = Filters.eq("f", blob);
					Bson sort = Sorts.ascending("o");
					MongoCursor<Json> chunks = Db.getDb("BlobChunks").find(filter).sort(sort).iterator();
					File tmp = File.createTempFile("pagebase_thumb", ".file");
					FileOutputStream outStream = new FileOutputStream(tmp);
					try {
						if (!chunks.hasNext()) {
							return null;
						}
						while (chunks.hasNext()) {
							outStream.write(chunks.next().getBinary("b").getData());
						}
						return makeFile(tmp, blob, type, width, height);
					} catch (Exception e) {
						return null;
					} finally {
						try {
							chunks.close();
						} catch (Exception e) {
							if (Fx.IS_DEBUG) {
								e.printStackTrace();
							}
						}
						try {
							tmp.delete();
						} catch (Exception e) {
							if (Fx.IS_DEBUG) {
								e.printStackTrace();
							}
						}
						try {
							outStream.close();
						} catch (Exception e) {
							if (Fx.IS_DEBUG) {
								e.printStackTrace();
							}
						}
					}
				} else if (file.getString("type").equals("application/pdf")) {
					InputStream sr = Thumbnailer.class.getResourceAsStream("/res/pdf.png");
					File tmp = File.createTempFile("pagebase_thumb", ".file");
					FileUtils.copyInputStreamToFile(sr, tmp);
					try {
						return makeFile(tmp, "pdf", type, width, height);
					} finally {
						sr.close();
						tmp.delete();
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	/**
	 * Make a thumbnail from File
	 */
	public static Json makeFile(File file, String file_id, String type, int width, int height) {

		Json cache = new Json().put("blob", file_id).put("width", width).put("height", height).put("format", type).put("date", new Date());

		try {

			ImageInputStream iis = ImageIO.createImageInputStream(file);
			String format = null;
			try {
				format = ImageIO.getImageReaders(iis).next().getFormatName().toLowerCase();
			} catch (Exception e) {
				try {
					ImageIO.write(ICODecoder.read(file).get(0), "png", file);
					format = "png";
				} catch (EOFException eo) {
					return null;
				}
			}
			iis.close();

			BufferedImage image = ImageIO.read(file);

			if (format == null) {
				cache.put("type", null);
			} else if (format.equalsIgnoreCase("png")) {
				cache.put("type", "image/png");
			} else if (format.equalsIgnoreCase("gif")) {
				cache.put("type", "image/gif");
			} else if (format.equalsIgnoreCase("jpeg") || format.equalsIgnoreCase("jpg")) {
				cache.put("type", "image/jpeg");
			} else {
				cache.put("type", null);
			}


			String outputformat = format == null ? "png" : format;
			if (type == null) {
				if (format.equalsIgnoreCase("gif")) {
					outputformat = "png";
				} else {
					outputformat = format;
				}

			} else if (type.equals(".jpg")) {
				cache.put("format", "jpg");
				outputformat = "jpeg";

			} else if (type.equals(".png")) {
				cache.put("format", "png");
				outputformat = "png";
			}

			if (height > 0) {
				int tempheight = (image.getHeight() * width) / image.getWidth();
				int tempwidth = width;
				if (tempheight < height) {
					tempwidth = (image.getWidth() * height) / image.getHeight();
					tempheight = height;
				}
				image = resize(image, tempwidth, tempheight, outputformat);
				if (height > 0) {
					image = image.getSubimage((tempwidth - width) / 2, (tempheight - height) / 2, width, height);
				}
			} else {
				int tempheight = (image.getHeight() * width) / image.getWidth();
				image = resize(image, width, tempheight, outputformat);
			}

			ImageWriter writer = ImageIO.getImageWritersByFormatName(outputformat).next();
			ImageWriteParam param = writer.getDefaultWriteParam();// Needed see javadoc

			if (outputformat.equals("jpeg") || outputformat.equals("jpg")) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.7F);
				param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
			}

			param.setDestinationType(new ImageTypeSpecifier(image));

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
			try {
				writer.setOutput(ios);
				writer.write(null, new IIOImage(image, null, null), param);
				baos.flush();
				writer.reset();
				writer.abort();
				writer.dispose();
			} catch (Exception ignore) {
				return null;
			} finally {
				ios.close();
				baos.close();
			}

			byte[] bt = baos.toByteArray();
			cache.put("size", bt.length);

			if (outputformat.equalsIgnoreCase("png")) {
				cache.put("type", "image/png");
			} else if (outputformat.equalsIgnoreCase("jpeg") || outputformat.equalsIgnoreCase("jpg")) {
				cache.put("type", "image/jpeg");
			}

			List<Binary> binaries = new ArrayList<>();
			ByteArrayInputStream bin = new ByteArrayInputStream(bt);
			try {
				byte[] buffer = new byte[Math.min(bt.length, Settings.CHUNCK_SIZE)];
				while (bin.read(buffer) != -1) {
					binaries.add(new Binary(buffer));
				}
			} catch (Exception ignore) {
				return null;
			} finally {
				bin.close();
			}
			cache.put("binaries", binaries);
			Db.save("BlobCache", cache);
			return cache;
		} catch (Exception e) {
			cache.put("expire", 900);
			Db.save("BlobCache", cache);
			return null;
		}

	}

	/**
	 * Resize image
	 */
	public static BufferedImage resize(BufferedImage img, int width, int height, String outputformat) {
		BufferedImage resized = new BufferedImage(width, height,
				outputformat.equals("jpg") || outputformat.equals("jpeg") ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB
		);
		Graphics2D g2d = resized.createGraphics();
		if (outputformat.equals("jpg") || outputformat.equals("jpeg")) {
			g2d.drawImage(img.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, Color.WHITE, null);
		} else {
			g2d.drawImage(img.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
		}
		g2d.dispose();
		return resized;
	}


	/**
	 * Get cached thumbnail from DB
	 */
	public static Json getCache(String blob, String format, int width, int height) {

		List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq("blob", blob));

		if (width > 0) {
			filters.add(Filters.eq("width", width));
		}
		filters.add(Filters.eq("height", height));

		if (format != null) {
			filters.add(Filters.eq("format", format.replace("jpeg", "jpg")));
		}
		Json cache = Db.getDb("BlobCache").find(Filters.and(filters)).hint(IndexBuilder.getHint("BlobCache", "blob")).first();
		if (cache != null && cache.containsKey("expire") && new Date(cache.getDate("date").getTime() + (cache.getInteger("expire") * 1000L)).before(new Date())) {
			Db.deleteOne("BlobCache", Filters.eq("_id", cache.getId()));
			return null;
		}
		return cache;
	}

	/**
	 * Search and find favicon
	 */
	private static Blob getFavicon(String blob) {

		try {
			String url = blob.replaceAll("(.*)favicon.ico$", "$1");
			Document page = Jsoup.parse(HttpClient.getAsFacebook(url), url);
			if (page != null) {
				Elements icons = page.select("head link[rel=shortcut icon],head link[rel=icon]");
				if (icons != null && icons.size() > 0) {
					for (Element icon : icons) {
						Blob favicon = BlobsUtils.downloadAsBlob(icon.absUrl("href"));
						if (favicon != null) {
							return favicon;
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}