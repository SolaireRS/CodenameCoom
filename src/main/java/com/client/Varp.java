package com.client;
// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class Varp {

	public static int cacheSize;
	
	public static void unpackConfig(StreamLoader streamLoader) {
		Buffer stream = new Buffer(streamLoader.getArchiveData("varp.dat"));
		anInt702 = 0;
		cacheSize = stream.readUShort();
		if (cache == null)
			cache = new Varp[cacheSize];
		if (anIntArray703 == null)
			anIntArray703 = new int[cacheSize];
		for (int j = 0; j < cacheSize; j++) {
			if (cache[j] == null)
				cache[j] = new Varp();
			cache[j].readValues(stream, j);
		}
		if (stream.currentOffset != stream.buffer.length)
			System.out.println("varptype load mismatch");
		// extend and dump varp
		/*int newSize = 2000;//config ids up to 1000
			Varp[] newData = new Varp[newSize];
			for(int id = 0; id < Varp.cacheSize; id++){
		newData[id] = Varp.cache[id];
			}
		try(DataOutputStream dos = new DataOutputStream(new FileOutputStream("./extended-varp.dat"))){
			dos.writeShort(newSize);
			for(int id = 0; id < newSize; id++){
				encode(dos, newData[id]);
			}
			dos.flush();
			dos.close();
		} catch(Exception e){
		e.printStackTrace();
			}*/
	}

	public static void encode(DataOutputStream dos, Varp varp) throws IOException, IOException {
		if(varp != null && varp.anInt709 != 0){
			dos.writeByte(5);
			dos.writeShort(varp.anInt709);
		}
		dos.writeByte(0);
	}
	private void readValues(Buffer stream, int i) {
		do {
			int j = stream.readUnsignedByte();
			if (j == 0)
				return;
			if (j == 1)
				stream.readUnsignedByte();
			else if (j == 2)
				stream.readUnsignedByte();
			else if (j == 3)
				anIntArray703[anInt702++] = i;
			else if (j == 4) {
			} else if (j == 5)
				anInt709 = stream.readUShort();
			else if (j == 6) {
			} else if (j == 7)
				stream.readDWord();
			else if (j == 8)
				aBoolean713 = true;
			else if (j == 10)
				stream.readString();
			else if (j == 11)
				aBoolean713 = true;
			else if (j == 12)
				stream.readDWord();
			else if (j == 13) {
			} else
				System.out.println("Error unrecognised config code: " + j);
		} while (true);
	}

	private Varp() {
		aBoolean713 = false;
	}

	public static Varp cache[];
	private static int anInt702;
	private static int[] anIntArray703;
	public int anInt709;
	public boolean aBoolean713;

}
