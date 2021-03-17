// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020-2021, Intel Corporation */

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.Converter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

class ByteBufferBackedInputStream extends InputStream {

	ByteBuffer buff;

	public ByteBufferBackedInputStream(ByteBuffer buff) {
		this.buff = buff;
		this.buff.rewind();
	}

	@Override
	public int read() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int read(byte[] bytes, int off, int len)
			throws IOException {
		if (!buff.hasRemaining()) {
			return -1;
		}
		len = Math.min(len, buff.remaining());
		buff.get(bytes, off, len);
		return len;
	}
}

class StringConverter implements Converter<String> {
	public ByteBuffer toByteBuffer(String entry) {
		return ByteBuffer.wrap(entry.getBytes());
	}

	public String fromByteBuffer(ByteBuffer entry) {
		byte[] bytes;
		bytes = new byte[entry.capacity()];
		entry.get(bytes);
		return new String(bytes);
	}
}

class ImageConverter implements Converter<BufferedImage> {
	public ByteBuffer toByteBuffer(BufferedImage entry) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(entry, "png", out);
		} catch (IOException e) {
			return null;
		}
		return ByteBuffer.wrap(out.toByteArray());
	}

	public BufferedImage fromByteBuffer(ByteBuffer entry) {
		BufferedImage out = null;
		try {
			out = ImageIO.read(new ByteBufferBackedInputStream(entry));
		} catch (IOException e) {
			return null;
		}
		return out;
	}
}

/* Add Pmemkv superpowers to the Canvas class */
class PmemkvPicture extends Canvas {
	private static final long serialVersionUID = 705612541135496879L;
	private Database<String, BufferedImage> db;
	private String engine = "cmap";

	public PmemkvPicture(String Path, int size) {
		System.out.println("Creating new database in path: " + Path + " with size: " + size);
		db = new Database.Builder<String, BufferedImage>(engine)
				.setSize(size)
				.setPath(Path)
				.setKeyConverter(new StringConverter())
				.setValueConverter(new ImageConverter())
				.setForceCreate(true)
				.build();
	}

	public PmemkvPicture(String Path) {
		System.out.println("Using already existing database: " + Path);
		db = new Database.Builder<String, BufferedImage>(engine)
				.setPath(Path)
				.setKeyConverter(new StringConverter())
				.setValueConverter(new ImageConverter())
				.build();
	}

	public void putAllPicturesFromDirectory(String dir_path) {
		File[] images = new File(dir_path).listFiles((dir, name) -> name.endsWith(".png"));
		for (File image : images) {
			System.out.println(image.getAbsolutePath());

			BufferedImage image_buffer = null;
			try {
				image_buffer = ImageIO.read(image);
			} catch (IOException e) {
				System.exit(1);
			}
			db.put(image.getName(), image_buffer);
		}
	}

	@Override
	public void paint(Graphics g) {
		System.out.println("Draw images from pmemkv database");
		AtomicInteger yPosition = new AtomicInteger(0);
		db.getAll((k, v) -> {
			System.out.println("\tDraw " + k);
			g.drawImage(v, 0, yPosition.getAndAdd(v.getHeight()), null);
		});
	}
}

public class PicturesExample {
	public static void main(String[] args) {
		String inputDirEnv = System.getenv("InputDir");
		String pmemkvPathEnv = System.getenv("PmemkvPath");
		String pmemkvSizeEnv = System.getenv("PmemkvSize");
		int pmemkvSize = 0;

		/* PmemkvPath is obligatory for this example */
		if (pmemkvPathEnv == null) {
			System.out.println("Provide at least PmemkvPath parameter. See examples' README for usage.");
			System.exit(0);
		}

		try {
			pmemkvSize = Integer.parseInt(pmemkvSizeEnv);
		} catch (NumberFormatException e) {
			System.out.println("Wrong size: " + e);
			System.exit(1);
		}

		System.out.println("Parameters:");
		System.out.println("InputDir: " + inputDirEnv);
		System.out.println("Path: " + pmemkvPathEnv);
		System.out.println("Size: " + pmemkvSizeEnv);

		PmemkvPicture m = null;
		if (pmemkvSize != 0) {
			m = new PmemkvPicture(pmemkvPathEnv, pmemkvSize);
		} else {
			m = new PmemkvPicture(pmemkvPathEnv);
		}
		if (inputDirEnv != null) {
			System.out.println("Loading files from " + inputDirEnv + " to pmemkv database.");
			m.putAllPicturesFromDirectory(inputDirEnv);
		}
		JFrame f = new JFrame();
		f.add(m);
		f.setSize(512, 512);
		f.setVisible(true);
	}
}
