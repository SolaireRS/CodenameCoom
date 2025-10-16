package com.client;

import java.math.BigInteger;

import com.client.sign.Signlink;

public final class Buffer extends NodeSub {

    private static final BigInteger RSA_MODULUS = new BigInteger("111990957240867658169963694315483492685430661556120485557551858755016390063593759559876713527641542200028905665048018179549754820928475971526854200154444670127825695266301595352975450257554566292361593522199741929118582819674185606872275137325041664320457776393817755760243200983096732087551865842957772780017");

    private static final BigInteger RSA_EXPONENT = new BigInteger("65537");

    public static Buffer create() {
        synchronized (nodeList) {
            Buffer stream = null;
            if (anInt1412 > 0) {
                anInt1412--;
                stream = (Buffer) nodeList.popHead();
            }
            if (stream != null) {
                stream.currentOffset = 0;
                return stream;
            }
        }
        Buffer stream_1 = new Buffer();
        stream_1.currentOffset = 0;
        stream_1.buffer = new byte[40000];
        return stream_1;
    }

    public int getSmart() {
        try {
            // checks current without modifying position
            if (currentOffset >= buffer.length) {
                return buffer[buffer.length - 1] & 0xFF;
            }
            int value = buffer[currentOffset] & 0xFF;

            if (value < 128) {
                return readUnsignedByte();
            } else {
                return readUShortA() - 32768;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return readUShortA() - 32768;
        }
    }

    public static Buffer create(int initialCapacity) {
        synchronized (nodeList) {
            Buffer stream = null;
            if (anInt1412 > 0) {
                anInt1412--;
                stream = (Buffer) nodeList.popHead();
            }
            if (stream != null) {
                stream.currentOffset = 0;
                return stream;
            }
        }
        Buffer stream_1 = new Buffer();
        stream_1.currentOffset = 0;
        stream_1.buffer = new byte[initialCapacity];
        return stream_1;
    }

    public Buffer() {
    }

    public byte[] getData(byte[] buffer2) {
        for (int i = 0; i < buffer2.length; i++)
            buffer2[i] = buffer[currentOffset++];
        return buffer2;
    }

    public Buffer(byte abyte0[]) {
        buffer = abyte0;
        currentOffset = 0;
    }

    public void createFrame(int i) {
        if (Configuration.developerMode)
            Client.instance.devConsole.print_message("Outgoing Packet " + i, 0);
        buffer[currentOffset++] = (byte) (i + encryption.getNextKey());
    }

    public void writeUnsignedByte(int i) {
        buffer[currentOffset++] = (byte) i;
    }

    final int v(int i) {
        currentOffset += 3;
        return (0xff & buffer[currentOffset - 3] << 16)
                + (0xff & buffer[currentOffset - 2] << 8)
                + (0xff & buffer[currentOffset - 1]);
    }

    public int readUnsignedByte2() {
        return -263;
    }

    public int g2() {
        currentOffset += 2;
        return ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public int g4() {
        currentOffset += 4;
        return ((buffer[currentOffset - 4] & 0xff) << 24)
                + ((buffer[currentOffset - 3] & 0xff) << 16)
                + ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public int readShort2() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
        if (i > 60000)
            i = -65535 + i;
        return i;

    }

    public int readUSmart2() {
        int baseVal = 0;
        int lastVal = 0;
        while ((lastVal = readSmart()) == 32767) {
            baseVal += 32767;
        }
        return baseVal + lastVal;
    }


    public void writeWord(int i) {
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
    }

    public void method400(int i) {
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 8);
    }

    public void writeDWordBigEndian(int i) {
        buffer[currentOffset++] = (byte) (i >> 16);
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
    }

    public void writeDWord(int i) {
        buffer[currentOffset++] = (byte) (i >> 24);
        buffer[currentOffset++] = (byte) (i >> 16);
        buffer[currentOffset++] = (byte) (i >> 8);
        buffer[currentOffset++] = (byte) i;
    }

    public void method403(int j) {
        buffer[currentOffset++] = (byte) j;
        buffer[currentOffset++] = (byte) (j >> 8);
        buffer[currentOffset++] = (byte) (j >> 16);
        buffer[currentOffset++] = (byte) (j >> 24);
    }

    public void writeQWord(long l) {
        try {
            buffer[currentOffset++] = (byte) (int) (l >> 56);
            buffer[currentOffset++] = (byte) (int) (l >> 48);
            buffer[currentOffset++] = (byte) (int) (l >> 40);
            buffer[currentOffset++] = (byte) (int) (l >> 32);
            buffer[currentOffset++] = (byte) (int) (l >> 24);
            buffer[currentOffset++] = (byte) (int) (l >> 16);
            buffer[currentOffset++] = (byte) (int) (l >> 8);
            buffer[currentOffset++] = (byte) (int) l;
        } catch (RuntimeException runtimeexception) {
            Signlink.reporterror("14395, " + 5 + ", " + l + ", "
                    + runtimeexception.toString());
            throw new RuntimeException();
        }
    }

    public void writeString(String s) {
        System.arraycopy(s.getBytes(), 0, buffer, currentOffset, s.length());
        currentOffset += s.length();
        buffer[currentOffset++] = 10;
    }

    public void writeBytes(byte abyte0[], int length, int startingPosition) {
        for (int k = startingPosition; k < startingPosition + length; k++)
            buffer[currentOffset++] = abyte0[k];
    }

    public void writeHiddenString(String string) {
        writeByte(string.length());
        byte[] stringBytes = string.getBytes();
        for (int index = 0; index < stringBytes.length; index++)
            stringBytes[index] += 15;
        writeBytes(stringBytes, stringBytes.length, 0);
    }

    public String readHiddenString() {
        int length = readUnsignedByte();
        byte[] stringBytes = new byte[length];
        for (int index = 0; index < length; index++) {
            stringBytes[index] = (byte) (readUnsignedByte() - 15);
        }
        return new String(stringBytes);
    }

    public void writeByte(int i) {
        buffer[currentOffset++] = (byte) i;
    }

    public void writeBytes(int i) {
        buffer[currentOffset - i - 1] = (byte) i;
    }

    public int read24BitInt()
    {
        return (this.readUnsignedByte() << 16) + (this.readUnsignedByte() << 8) + this.readUnsignedByte();
    }
    public int readUnsignedIntSmartShortCompat() {
        int var1 = 0;

        int var2;
        for (var2 = this.readUSmart(); var2 == 32767; var2 = this.readUSmart()) {
            var1 += 32767;
        }

        var1 += var2;
        return var1;
    }
    public int readUnsignedByte() {
        return buffer[currentOffset++] & 0xff;
    }

    public byte readSignedByte() {
        return buffer[currentOffset++];
    }

    public int readUShort() {
        currentOffset += 2;
        return ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public int method1606() {
        int var2 = 0;

        int var3;
        for (var3 = this.readUShortSmart(); var3 == 32767; var3 = this.readUShortSmart()) {
            var2 += 32767;
        }

        var2 += var3;
        return var2;
    }

    public int readUShortSmart() {
        int var2 = this.buffer[this.currentOffset] & 255;
        return var2 < 128 ? this.readUnsignedByte() : this.readUShort() - 32768;
    }

    public int readSignedWord() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
        if (i > 32767)
            i -= 0x10000;
        return i;
    }

    public int read3Bytes() {
        currentOffset += 3;
        return ((buffer[currentOffset - 3] & 0xff) << 16)
                + ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public int readDWord() {
        currentOffset += 4;
        return ((buffer[currentOffset - 4] & 0xff) << 24)
                + ((buffer[currentOffset - 3] & 0xff) << 16)
                + ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
    }

    public long readQWord() {
        long l = readDWord() & 0xffffffffL;
        long l1 = readDWord() & 0xffffffffL;
        return (l << 32) + l1;
    }

    public String readString() {
        int i = currentOffset;
        while (buffer[currentOffset++] != 10)
            ;
        return new String(buffer, i, currentOffset - i - 1);
    }

    public String readNullTerminatedString() {
        int i = currentOffset;
        while (buffer[currentOffset++] != 0)
            ;
        return new String(buffer, i, currentOffset - i - 1);
    }

    private static final char[] OSRS_CHARACTERS = new char[]
            {
                    '\u20ac', '\u0000', '\u201a', '\u0192', '\u201e', '\u2026',
                    '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160', '\u2039',
                    '\u0152', '\u0000', '\u017d', '\u0000', '\u0000', '\u2018',
                    '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014',
                    '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u0000',
                    '\u017e', '\u0178'
            };

    public String readOSRSString()
    {
        StringBuilder sb = new StringBuilder();

        for (; ; )
        {
            int ch = this.readUnsignedByte();

            if (ch == 0)
            {
                break;
            }

            if (ch >= 128 && ch < 160)
            {
                char var7 = OSRS_CHARACTERS[ch - 128];
                if (0 == var7)
                {
                    var7 = '?';
                }

                ch = var7;
            }

            sb.append((char) ch);
        }
        return sb.toString();
    }

    public byte[] readBytes() {
        int i = currentOffset;
        while (buffer[currentOffset++] != 10)
            ;
        byte abyte0[] = new byte[currentOffset - i - 1];
        System.arraycopy(buffer, i, abyte0, i - i, currentOffset - 1 - i);
        return abyte0;
    }

    public void readBytes(int length, int offset, byte abyte0[]) {
        for (int l = offset; l < offset + length; l++)
            abyte0[l] = buffer[currentOffset++];
    }

    public void initBitAccess() {
        bitPosition = currentOffset * 8;
    }

    public int readBits(int i) {
        int k = bitPosition >> 3;
        int l = 8 - (bitPosition & 7);
        int i1 = 0;
        bitPosition += i;
        for (; i > l; l = 8) {
            i1 += (buffer[k++] & anIntArray1409[l]) << i - l;
            i -= l;
        }
        if (i == l)
            i1 += buffer[k] & anIntArray1409[l];
        else
            i1 += buffer[k] >> l - i & anIntArray1409[i];
        return i1;
    }

    public void finishBitAccess() {
        currentOffset = (bitPosition + 7) / 8;
    }

    public int method421() {
        try {
            int i = buffer[currentOffset] & 0xff;
            if (i < 128)
                return readUnsignedByte() - 64;
            else
                return readUShort() - 49152;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void doKeys() {
        int i = currentOffset;
        currentOffset = 0;
        byte abyte0[] = new byte[i];
        readBytes(i, 0, abyte0);
        BigInteger biginteger2 = new BigInteger(abyte0);
        BigInteger biginteger3 = biginteger2.modPow(RSA_EXPONENT, RSA_MODULUS);
        byte abyte1[] = biginteger3.toByteArray();
        currentOffset = 0;
        writeUnsignedByte(abyte1.length);
        writeBytes(abyte1, abyte1.length, 0);
    }

    public void method424(int i) {
        buffer[currentOffset++] = (byte) (-i);
    }

    public void method425(int j) {
        buffer[currentOffset++] = (byte) (128 - j);
    }

    public int method426() {
        return buffer[currentOffset++] - 128 & 0xff;
    }

    public int method427() {
        return -buffer[currentOffset++] & 0xff;
    }

    public int method428() {
        return 128 - buffer[currentOffset++] & 0xff;
    }

    public byte method429() {
        return (byte) (-buffer[currentOffset++]);
    }

    public byte method430() {
        return (byte) (128 - buffer[currentOffset++]);
    }

    public void method431(int i) {
        buffer[currentOffset++] = (byte) i;
        buffer[currentOffset++] = (byte) (i >> 8);
    }

    public void method432(int j) {
        buffer[currentOffset++] = (byte) (j >> 8);
        buffer[currentOffset++] = (byte) (j + 128);
    }

    public void method433(int j) {
        buffer[currentOffset++] = (byte) (j + 128);
        buffer[currentOffset++] = (byte) (j >> 8);
    }

    public int method434() {
        currentOffset += 2;
        return ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] & 0xff);
    }

    public int readUShortA() {
        currentOffset += 2;
        return ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] - 128 & 0xff);
    }

    public int method436() {
        currentOffset += 2;
        return ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] - 128 & 0xff);
    }

    public int method437() {
        currentOffset += 2;
        int j = ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] & 0xff);
        if (j > 32767)
            j -= 0x10000;
        return j;
    }

    public int method438() {
        currentOffset += 2;
        int j = ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] - 128 & 0xff);
        if (j > 32767)
            j -= 0x10000;
        return j;
    }

    public int method439() {
        currentOffset += 4;
        return ((buffer[currentOffset - 2] & 0xff) << 24)
                + ((buffer[currentOffset - 1] & 0xff) << 16)
                + ((buffer[currentOffset - 4] & 0xff) << 8)
                + (buffer[currentOffset - 3] & 0xff);
    }

    public int method440() {
        currentOffset += 4;
        return ((buffer[currentOffset - 3] & 0xff) << 24)
                + ((buffer[currentOffset - 4] & 0xff) << 16)
                + ((buffer[currentOffset - 1] & 0xff) << 8)
                + (buffer[currentOffset - 2] & 0xff);
    }
    public int readUnsignedIntSmarSthortCompat() {
        int var1 = 0;

        int var2;
        for (var2 = this.readUSmart(); var2 == 32767; var2 = this.readUSmart()) {
            var1 += 32767;
        }

        var1 += var2;
        return var1;
    }
    public int readtSmart() {
        int i = buffer[currentOffset] & 0xff;
        if (i < 128)
            return readUnsignedByte();
        else
            return readUShort() - 32768;
    }
    public int readUSmart() {
        int peek = buffer[currentOffset] & 0xFF;
        return peek < 128 ? this.readUnsignedByte() : this.readUShort() - 0x8000;
    }
    public int readSmart() {
        int i = buffer[currentOffset] & 0xff;
        if (i < 128)
            return readUnsignedByte();
        else
            return readUShort() - 32768;
    }

    public int readNewSmart() {
        int value = buffer[currentOffset] & 0xFF;
        if (value < 128) {
            return readUnsignedByte() - 64;
        } else {
            return readUShort() - 49152;
        }
    }
    public void setOffset(int var11) {
        currentOffset = var11;
    }
    public void method441(int i, byte abyte0[], int j) {
        for (int k = (i + j) - 1; k >= i; k--)
            buffer[currentOffset++] = (byte) (abyte0[k] + 128);

    }

    public void method442(int i, int j, byte abyte0[]) {
        for (int k = (j + i) - 1; k >= j; k--)
            abyte0[k] = buffer[currentOffset++];

    }
    public int getUIncrementalSmart() {
        int value = 0, remainder;
        for (remainder = readUSmart(); remainder == 32767; remainder = readUSmart()) {
            value += 32767;
        }
        value += remainder;
        return value;
    }
    public int getLength() {
        return buffer.length;
    }

    public byte buffer[];
    public int currentOffset;
    public int bitPosition;
    public static final int[] anIntArray1409 = {0, 1, 3, 7, 15, 31, 63, 127,
            255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535, 0x1ffff,
            0x3ffff, 0x7ffff, 0xfffff, 0x1fffff, 0x3fffff, 0x7fffff, 0xffffff,
            0x1ffffff, 0x3ffffff, 0x7ffffff, 0xfffffff, 0x1fffffff, 0x3fffffff,
            0x7fffffff, -1};
    public ISAACRandomGen encryption;
    public static int anInt1412;
    public static final NodeList nodeList = new NodeList();
    public byte readByte() {
        return this.buffer[++this.currentOffset - 1];
    }
    public int readInt() {
        currentOffset += 4;
        return ((buffer[currentOffset - 4] & 0xff) << 24) + ((buffer[currentOffset - 3] & 0xff) << 16) + ((buffer[currentOffset - 2] & 0xff) << 8) + (buffer[currentOffset - 1] & 0xff);
    }
    public int readNullableLargeSmart() {
        if (this.buffer[this.currentOffset] < 0) {
            return this.readInt() & Integer.MAX_VALUE;
        } else {
            int var1 = this.readUShort();
            return var1 == 32767 ? -1 : var1;
        }
    }

    public int readShortSmartSub() {
        int var1 = this.buffer[this.currentOffset] & 255;
        return var1 < 128 ? this.readUnsignedByte() - 1 : this.readUShort() - 0x8000;
    }
    public int get_smart_byteorshort() {
        int value = buffer[currentOffset] & 0xFF;
        if (value < 128) {
            return readUnsignedByte() - 0x40;
        } else {
            return readUShort() - 0xc000;
        }
    }
    public float get_float() {
        return Float.intBitsToFloat(this.readInt());
    }

    public int get_short() {
        currentOffset += 2;
        int i = ((buffer[currentOffset - 2] & 0xff) << 8)
                + (buffer[currentOffset - 1] & 0xff);
        if (i > 0x7fff) {
            i -= 0x10000;
        }
        return i;

    }
    public String readNewString() {
        int i = this.currentOffset;

        while(this.buffer[this.currentOffset++] != 0) {
            ;
        }

        return new String(this.buffer, i, this.currentOffset - i - 1);
    }
    public int get_unsignedbyte() {
        return buffer[currentOffset++] & 0xff;
    }

    public int read24Int() {
        currentOffset += 3;
        return ((buffer[currentOffset - 3] & 0xff) << 16) + ((buffer[currentOffset - 2] & 0xff) << 8) + (buffer[currentOffset - 1] & 0xff);
    }

    public String readStringCp1252NullTerminated() {
        int var1 = this.currentOffset;

        while (this.buffer[(this.currentOffset) - 1] != 0) {
        }

        int var2 = this.currentOffset - var1 - 1;
        return var2 == 0 ? "" : decodeStringCp1252(this.buffer, var1, var2);
    }
    public static String decodeStringCp1252(byte[] var0, int var1, int var2) {
        char[] var3 = new char[var2];
        int var4 = 0;

        for (int var5 = 0; var5 < var2; ++var5) {
            int var6 = var0[var5 + var1] & 255;
            if (var6 != 0) {
                if (var6 >= 128 && var6 < 160) {
                    char var7 = cp1252AsciiExtension[var6 - 128];
                    if (var7 == 0) {
                        var7 = '?';
                    }

                    var6 = var7;
                }

                var3[var4++] = (char)var6;
            }
        }

        return new String(var3, 0, var4);
    }
    public static final char[] cp1252AsciiExtension;

    static {
        cp1252AsciiExtension = new char[]{'€', '\u0000', '‚', 'ƒ', '„', '…', '†', '‡', 'ˆ', '‰', 'Š', '‹', 'Œ', '\u0000', 'Ž', '\u0000', '\u0000', '‘', '’', '“', '”', '•', '–', '—', '˜', '™', 'š', '›', 'œ', '\u0000', 'ž', 'Ÿ'};
    }
}
