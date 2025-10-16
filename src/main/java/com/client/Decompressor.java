package com.client;

import java.io.*;

// https://www.rune-server.ee/runescape-development/rs2-server/informative-threads/534604-explanation-simplification-runescape-cache.html
final class Decompressor {

	/**
	 * Size of an entry in any index file.
	 * An error contains the file size and the sector where this file begins.
	 */
	public static final int INDEX_SIZE = 6;

	/**
	 * Size of a data block inside that data (.dat) file
	 */
	public static final int DATA_BLOCK_SIZE = 512;

	/**
	 * Size of a data block header inside that data (.dat) file
	 */
	public static final int DATA_HEADER_SIZE = 8;

	/**
	 * Total size of a Sector {@link Decompressor#DATA_BLOCK_SIZE} plus {@link Decompressor#DATA_HEADER_SIZE}
	 */
	public static final int DATA_SIZE = DATA_HEADER_SIZE + DATA_BLOCK_SIZE;

	private static final byte[] buffer = new byte[520];
	private final RandomAccessFile dataFile;
	private final RandomAccessFile indexFile;
	private final int fileType;

	public Decompressor(RandomAccessFile dataFile, RandomAccessFile indexFile, int fileTye) {
		fileType = fileTye;
		this.dataFile = dataFile;
		this.indexFile = indexFile;
	}


	/**
	 * Read a file from the data file.
	 *
	 * First it reads the index file, which contains 6 bytes of information: the total file size and the
	 * sector id where this file begins. It then begins reading the data file in 520 byte sectors. The sectors
	 * contains 8 bytes of header data first (fileId, chunkId, sectorId, typeId) then 512 bytes of file data.
	 *
	 * @param fileId The file id.
	 * @return the file data.
	 */
	public synchronized byte[] read(int fileId) {
		try {
			seekTo(indexFile, fileId * INDEX_SIZE); // Seek index file to position of file data
			int read;
			for (int offset = 0; offset < INDEX_SIZE; offset += read) {
				read = indexFile.read(buffer, offset, 6 - offset);
				if (read == -1)
					return null;
			}
			int fileSize = ((buffer[0] & 0xff) << 16) + ((buffer[1] & 0xff) << 8) + (buffer[2] & 0xff);
			int sectorId = ((buffer[3] & 0xff) << 16) + ((buffer[4] & 0xff) << 8) + (buffer[5] & 0xff);
			if (sectorId <= 0 || (long) sectorId > dataFile.length() / DATA_SIZE)
				return null;
			byte[] fileData = new byte[fileSize];
			int readerIndex = 0;
			int chunkLength = fileId <= 0xffff ? 512 : 510;
			int headerLength = fileId <= 0xffff ? 8 : 10;
			for (int chunk = 0; readerIndex < fileSize; chunk++) {
				if (sectorId == 0)
					return null;
				seekTo(dataFile, sectorId * 520); // Seek data file to data start position

				int remaining = fileSize - readerIndex;
				if (remaining > chunkLength) {
					remaining = chunkLength;
				}
				int offset = 0;
				for (; offset < remaining + headerLength; offset += read) {
					read = dataFile.read(buffer, offset, (remaining + headerLength) - offset);
					if (read == -1)
						return null;
				}
				int currentIndex;
				int currentPart;
				int nextSector;
				int currentFile;

				if(fileId <= 0xffff) {
					currentIndex = ((buffer[0] & 0xff) << 8) + (buffer[1] & 0xff);//Short
					currentPart = ((buffer[2] & 0xff) << 8) + (buffer[3] & 0xff);//Short
					nextSector = ((buffer[4] & 0xff) << 16) + ((buffer[5] & 0xff) << 8) + (buffer[6] & 0xff);//Medium
					currentFile = buffer[7] & 0xff;//Byte
				} else {
					currentIndex = ((buffer[0] & 0xff) << 24) + ((buffer[1] & 0xff) << 16) + ((buffer[2] & 0xff) << 8) + (buffer[3] & 0xff);//Int
					currentPart = ((buffer[4] & 0xff) << 8) + (buffer[5] & 0xff);//Short
					nextSector = ((buffer[6] & 0xff) << 16) + ((buffer[7] & 0xff) << 8) + (buffer[8] & 0xff);//Medium
					currentFile = buffer[9] & 0xff;//Byte
				}
				if (currentIndex != fileId || currentPart != chunk || currentFile != fileType) {
					return null;
				}
				if (nextSector < 0 || (long) nextSector > dataFile.length() / DATA_SIZE) {
					return null;
				}
				for (int dataReaderIndex = 0; dataReaderIndex < remaining; dataReaderIndex++) {
					fileData[readerIndex++] = buffer[dataReaderIndex + headerLength];
				}

				sectorId = nextSector;
			}

			return fileData;
		} catch (IOException _ex) {
			_ex.printStackTrace();
			return null;
		}
	}

	public synchronized boolean write(int fileSize, byte[] data, int fileId) {
		boolean flag = write(true, fileId, fileSize, data); // Attempt to overwrite existing index
		if (!flag)
			flag = write(false, fileId, fileSize, data); // Existing index doesn't exist, add a new index
		return flag;
	}

	private synchronized boolean write(boolean overwrite, int fileId, int fileSize, byte[] data) {
		try {
			int firstSectorId;
			if (overwrite) {
				seekTo(indexFile, fileId * INDEX_SIZE);
				int read;
				for (int offset = 0; offset < INDEX_SIZE; offset += read) {
					read = indexFile.read(buffer, offset, INDEX_SIZE - offset);
					if (read == -1)
						return false;
				}
				firstSectorId = ((buffer[3] & 0xff) << 16) + ((buffer[4] & 0xff) << 8) + (buffer[5] & 0xff);
				if (firstSectorId <= 0 || (long) firstSectorId > dataFile.length() / DATA_SIZE)
					return false;
			} else {
				firstSectorId = (int) ((dataFile.length() + 519L) / DATA_SIZE); // Set sector id to the next free 520 block in the dat file
				if (firstSectorId == 0)
					firstSectorId = 1;
			}
			buffer[0] = (byte) (fileSize >> 16);
			buffer[1] = (byte) (fileSize >> 8);
			buffer[2] = (byte) fileSize;
			buffer[3] = (byte) (firstSectorId >> 16);
			buffer[4] = (byte) (firstSectorId >> 8);
			buffer[5] = (byte) firstSectorId;
			seekTo(indexFile, fileId * INDEX_SIZE);
			indexFile.write(buffer, 0, INDEX_SIZE);
			int chunkLength = fileId <= 0xffff ? 512 : 510;
			int headerLength = fileId <= 0xffff ? 8 : 10;

			int j1 = 0;
			for (int chunkId = 0; j1 < fileSize; chunkId++) {
				int nextSector = 0;
				if (overwrite) {
					// Read stored data header
					seekTo(dataFile, firstSectorId * DATA_SIZE);
					int length;
					int read;
					for (length = 0; length < headerLength; length += read) {
						read = dataFile.read(buffer, length, headerLength - length);
						if (read == -1)
							break;
					}
					if (length == headerLength) {
						int currentIndex;
						int currentPart;
						int currentFile;

						if(fileId <= 0xffff) {
							currentIndex = ((buffer[0] & 0xff) << 8) + (buffer[1] & 0xff);//Short
							currentPart = ((buffer[2] & 0xff) << 8) + (buffer[3] & 0xff);//Short
							nextSector = ((buffer[4] & 0xff) << 16) + ((buffer[5] & 0xff) << 8) + (buffer[6] & 0xff);//Medium
							currentFile = buffer[7] & 0xff;//Byte
						} else {
							currentIndex = ((buffer[0] & 0xff) << 24) + ((buffer[1] & 0xff) << 16) + ((buffer[2] & 0xff) << 8) + (buffer[3] & 0xff);//Int
							currentPart = ((buffer[4] & 0xff) << 8) + (buffer[5] & 0xff);//Short
							nextSector = ((buffer[6] & 0xff) << 16) + ((buffer[7] & 0xff) << 8) + (buffer[8] & 0xff);//Medium
							currentFile = buffer[9] & 0xff;//Byte
						}

						if (currentIndex != fileId || currentPart != chunkId || currentFile != fileType)
							return false;
						if (nextSector < 0 || (long) nextSector > dataFile.length() / DATA_SIZE)
							return false;
					}
				}
				if (nextSector == 0) {
					overwrite = false;
					nextSector = (int) ((dataFile.length() + 519L) / DATA_SIZE); // Is it 519 because length starts 1 instead of 0?
					if (nextSector == 0)
						nextSector++;
					if (nextSector == firstSectorId)
						nextSector++;
				}
				if (fileSize - j1 <= chunkLength)
					nextSector = 0;
				/*buffer[0] = (byte) (fileId >> 8);
				buffer[1] = (byte) fileId;
				buffer[2] = (byte) (chunkId >> 8);
				buffer[3] = (byte) chunkId;
				buffer[4] = (byte) (nextSector >> 16);
				buffer[5] = (byte) (nextSector >> 8);
				buffer[6] = (byte) nextSector;
				buffer[7] = (byte) fileType;
				 */
				if(fileId <= 0xffff) {
					buffer[0] = (byte) (fileId >> 8);//Short
					buffer[1] = (byte) fileId;
					buffer[2] = (byte) (chunkId >> 8);//Short
					buffer[3] = (byte) chunkId;
					buffer[4] = (byte) (nextSector >> 16);//Medium
					buffer[5] = (byte) (nextSector >> 8);
					buffer[6] = (byte) nextSector;
					buffer[7] = (byte) fileType;//Byte
				} else {
					buffer[0] = (byte) (fileId >> 24);//Int
					buffer[1] = (byte) (fileId >> 16);
					buffer[2] = (byte) (fileId >> 8);
					buffer[3] = (byte) fileId;
					buffer[4] = (byte) (chunkId >> 8);//Short
					buffer[5] = (byte) chunkId;
					buffer[6] = (byte) (nextSector >> 16);//Medium
					buffer[7] = (byte) (nextSector >> 8);
					buffer[8] = (byte) nextSector;
					buffer[9] = (byte) fileType;//Byte
				}
				seekTo(dataFile, firstSectorId * DATA_SIZE);
				dataFile.write(buffer, 0, headerLength);
				int k2 = fileSize - j1;
				if (k2 > chunkLength)
					k2 = chunkLength;
				dataFile.write(data, j1, k2);
				j1 += k2;
				firstSectorId = nextSector;
			}

			return true;
		} catch (IOException _ex) {
			_ex.printStackTrace();
			return false;
		}
	}

	private synchronized void seekTo(RandomAccessFile randomaccessfile, int j) {
		try {
			randomaccessfile.seek(j);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
