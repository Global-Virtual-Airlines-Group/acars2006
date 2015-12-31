package org.deltava.acars.beans;

import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;

import junit.framework.TestCase;

@SuppressWarnings("static-method")
public class TestDataCompressor extends TestCase {
	
	private static String loadFile(String fileName) {
		StringBuilder buf = new StringBuilder();
		try (InputStream is = new FileInputStream(fileName)) {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(is)))	{
				String data = br.readLine();
				while (data != null) {
					buf.append(data);
					buf.append(System.getProperty("line.separator"));
					data = br.readLine(); 
				}
			}
		} catch (IOException ie) {
			// empty
		}
		
		return buf.toString();
	}
	
	public void testRaw() {
		DataCompressor cmp = new DataCompressor();
		assertEquals(Compression.NONE, cmp.getCompression());
		
		String data1 = loadFile("data/multiMsg.xml");
		assertNotNull(data1); assertTrue(data1.length() > 0);
		byte[] rawData = data1.getBytes(UTF_8);

		byte[] data = cmp.compress(rawData);
		assertNotNull(data); assertTrue(data.length > 0);
		assertEquals(rawData.length, data.length);
		assertEquals(data1, new String(data, UTF_8));
		
		byte[] rawData2 = cmp.decompress(data);
		assertEquals(rawData.length, rawData2.length);
		assertEquals(data1, new String(rawData2, UTF_8));
	}
	
	public void testGZIP() {
		DataCompressor cmp = new DataCompressor();
		cmp.setCompression(Compression.GZIP);
		assertEquals(Compression.GZIP, cmp.getCompression());
		
		String data1 = loadFile("data/multiMsg.xml");
		assertNotNull(data1); assertTrue(data1.length() > 0);
		byte[] rawData = data1.getBytes(UTF_8);
		
		byte[] data = cmp.compress(rawData);
		assertNotNull(data); assertTrue(data.length > 0);
		assertTrue(DataCompressor.isGZIP(data));
		
		byte[] rawData2 = cmp.decompress(data);
		assertNotNull(rawData2);
		assertEquals(rawData.length, rawData2.length);
		assertEquals(data1, new String(rawData2, UTF_8));
	}
	
	public void testReuse() {
		DataCompressor cmp = new DataCompressor();
		cmp.setCompression(Compression.GZIP);
		assertEquals(Compression.GZIP, cmp.getCompression());

		String data1 = loadFile("data/multiMsg.xml");
		assertNotNull(data1); assertTrue(data1.length() > 0);
		byte[] rawData = data1.getBytes(UTF_8);

		byte[] data = cmp.compress(rawData);
		assertNotNull(data); assertTrue(data.length > 0);
		
		byte[] rawData2 = cmp.decompress(data);
		assertEquals(rawData.length, rawData2.length);
		assertEquals(data1, new String(rawData2, UTF_8));
		
		String data2 = loadFile("data/positionMsg.xml");
		assertNotNull(data1); assertTrue(data1.length() > 0);
		rawData = data2.getBytes(UTF_8);
		
		data = cmp.compress(rawData);
		assertNotNull(data); assertTrue(data.length > 0);

		rawData2 = cmp.decompress(data);
		assertNotNull(rawData2);
		assertEquals(rawData.length, rawData2.length);
		assertEquals(data2, new String(rawData2, UTF_8));
	}
	
	public void testError() {
		DataCompressor cmp = new DataCompressor();
		cmp.setCompression(Compression.NONE);
		assertEquals(Compression.NONE, cmp.getCompression());

		DataCompressor cmp2 = new DataCompressor();
		cmp2.setCompression(Compression.GZIP);
		assertEquals(Compression.GZIP, cmp2.getCompression());

		String data1 = loadFile("data/multiMsg.xml");
		assertNotNull(data1); assertTrue(data1.length() > 0);
		byte[] rawData = data1.getBytes(UTF_8);

		byte[] data = cmp.compress(rawData);
		assertNotNull(data); assertTrue(data.length > 0);

		byte[] rawData2 = cmp2.decompress(data);
		assertNotNull(rawData2);
		assertEquals(0, rawData2.length);
	}
}