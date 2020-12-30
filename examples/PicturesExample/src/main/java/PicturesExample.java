// SPDX-License-Identifier: BSD-3-Clause
/* Copyright 2020, Intel Corporation */

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import java.io.File;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.pmem.pmemkv.Database;
import io.pmem.pmemkv.Converter;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

class ByteBufferBackedInputStream extends InputStream {

	ByteBuffer buff;

	public ByteBufferBackedInputStream(ByteBuffer buff) {
		this.buff = buff;
		this.buff.rewind();
	}

	public int read() {
		throw new UnsupportedOperationException();
	}

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

public class PicturesExample extends Canvas {

	private Database<String, BufferedImage> db;
	private String engine = "cmap";
	static final long serialVersionUID = 9101254512891724823L;

	public PicturesExample(String Path, int size) {
		System.out.println("Creating new database in path: " + Path + " with size: " + size);
		db = new Database.Builder<String, BufferedImage>(engine)
				.setSize(size)
				.setPath(Path)
				.setKeyConverter(new StringConverter())
				.setValueConverter(new ImageConverter())
				.setForceCreate(true)
				.build();
	}

	public PicturesExample(String Path) {
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

	public void paint(Graphics g) {
		System.out.println("Draw images from pmemkv database");
		AtomicInteger yPosition = new AtomicInteger(0);
		db.getAll((k, v) -> {
			System.out.println("\tDraw" + k);
			Graphics2D g2 = (Graphics2D) g;
			g.drawImage(v, 0, yPosition.getAndAdd(v.getHeight()), null);
		});
	}

	public static void main(String[] args) {
		String input_dir = System.getenv("InputDir");
		String pmemkvPath = System.getenv("PmemkvPath");
		String pmemkvSize = System.getenv("PmemkvSize");

		System.out.println("Parameters:");
		System.out.println("InputDir" + input_dir);
		System.out.println("Path: " + pmemkvPath);
		System.out.println("Size: " + pmemkvSize);

		PicturesExample m = null;
		if (pmemkvSize != null && pmemkvPath != null) {
			try {
				m = new PicturesExample(pmemkvPath, Integer.parseInt(pmemkvSize));
			} catch (NumberFormatException e) {
				System.out.println("Wrong size: " + e);
				System.exit(1);
			}
		} else if (pmemkvPath != null) {
			m = new PicturesExample(pmemkvPath);
		} else {
			System.out.println("Provide at least PmemkvPath parameter. See examples' README for usage");
			System.exit(0);
		}

		if (input_dir != null) {
			System.out.println("Loading files from " + input_dir + " to pmemkv database");
			m.putAllPicturesFromDirectory(input_dir);
		}
		JFrame f = new JFrame();
		f.add(m);
		f.setSize(512, 512);
		f.setVisible(true);
	}
}
