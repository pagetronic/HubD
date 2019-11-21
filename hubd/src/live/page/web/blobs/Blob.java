/*
 * Copyright 2019 Laurent PAGE, Apache Licence 2.0
 */
package live.page.web.blobs;

import java.io.File;

public class Blob {
	File file;
	String contentType;

	public Blob(File file, String contentType) {
		this.file = file;
		this.contentType = contentType;
	}

}