package com.client.definitions;

import com.client.Buffer;
import com.client.StreamLoader;

public final class VarBit {

	public static void unpackConfig(StreamLoader streamLoader) {
		Buffer stream = new Buffer(streamLoader.getArchiveData("varbit.dat"));
		int cacheSize = stream.readUShort();
		if (cache == null)
			cache = new VarBit[cacheSize];
		for (int j = 0; j < cacheSize; j++) {
			if (cache[j] == null)
				cache[j] = new VarBit();
			cache[j].readValues(stream);
		}

		if (stream.currentOffset != stream.buffer.length)
			System.out.println("varbit load mismatch");
	}

	private void readValues(Buffer stream) {
		int opcode = stream.readUnsignedByte();
		if (opcode == 0) {
			return;
		} else if (opcode == 1) {
		configID = stream.readUShort();
		lsb = stream.readUnsignedByte();
		msb = stream.readUnsignedByte();
		} else {
			System.out.println("Invalid varbit opcode: " + opcode);
		}
	}

	private VarBit() {
		
	}

	public static VarBit cache[];
	public int configID;
	public int lsb;
	public int msb;
	
}
