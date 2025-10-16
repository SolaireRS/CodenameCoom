package com.client.definitions;

import com.client.Buffer;

import com.client.StreamLoader;
import com.client.sign.Signlink;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public final class AreaDefinition {

    public static int totalAreas;
    public static AreaDefinition[] cache;
    private static int cacheIndex;
    private static Buffer area_data;
    private static int[] streamIndices;


    public int id;
    public int spriteId = -1;
    public int field3294 = -1;
    public String name = "";
    public int field3296 = -1;
    public int field3297 = -1;
    public String[] actions;
    public int field3310 = -1;
    private int flags;
    private boolean field1944;
    private boolean field1945;
    private byte opcode8;
    private int[] anIntArray1982;
    private int[] anIntArray1981;
    private byte[] aByteArray1979;
    private String aString1970;
    private int opcode18;
    private int category;
    private int textSize = -1;
    private int int22;
    private byte int23_1;
    private byte int23_2;
    private byte int23_3;
    private short int24_1;
    private short int24_2;
    private int int25;
    private byte int28;
    private byte int29;
    private byte int30;

    private AreaDefinition() {
        id = -1;
    }

    public static void clear() {
        streamIndices = null;
        cache = null;
        area_data = null;
    }

    public static void unpackConfig(StreamLoader archive) {

        area_data = new Buffer(archive.getArchiveData("areas.dat"));
        Buffer stream = new Buffer(archive.getArchiveData("areas.idx"));

        totalAreas = stream.readUShort();
        streamIndices = new int[totalAreas+200];
        int offset = 2;

        for (int _ctr = 0; _ctr < totalAreas; _ctr++) {
            streamIndices[_ctr] = offset;
            offset += stream.readUShort();
        }

        cache = new AreaDefinition[10];

        for (int _ctr = 0; _ctr < 10; _ctr++) {
            cache[_ctr] = new AreaDefinition();
        }
         dumpObjectList();
        System.out.println("Loaded: " + totalAreas + " Areas");

    }
    public static void dumpObjectList() {
        for(int i = 0; i < totalAreas; i++) {
            AreaDefinition class5 = lookup(i);
            BufferedWriter bw = null;
            try {
                bw = new BufferedWriter(new FileWriter(Signlink.getCacheDirectory() + "/dumps/area.txt", true));
                if(class5.name!= null) {
                    bw.write("case "+i+";");
                    bw.newLine();
                    bw.write("type.spriteId "+class5.spriteId+";");
                    bw.newLine();
                    bw.write("type.field3294 "+class5.field3294+";");
                    bw.newLine();
                    bw.write("type.name "+class5.name+";");
                    bw.newLine();
                    bw.write("type.field3296 "+class5.field3296+";");
                    bw.newLine();
                    bw.write("type.field3297 "+class5.field3297+";");
                    bw.newLine();
                    bw.write("type.field32961 "+class5.textSize +";");
                    bw.newLine();
                    bw.write("type.flags "+class5.flags+";");
                    bw.newLine();
                    bw.write("type.opcode8 "+class5.opcode8+";");
                    bw.newLine();
                    bw.flush();
                    bw.close();
                }
            } catch (IOException ioe2) {
            }
        }
    }
    public static AreaDefinition lookup(int itemId) {

        for (int count = 0; count < 10; count++)
            if (cache[count].id == itemId)
                return cache[count];
        if (itemId == -1)
            itemId = 0;
        if (itemId > streamIndices.length)
            itemId = 0;
        cacheIndex = (cacheIndex + 1) % 10;
        AreaDefinition itemDef = cache[cacheIndex];
            area_data.currentOffset = streamIndices[itemId];
        itemDef.id = itemId;
        itemDef.readValues(area_data);
        switch(itemId){

        }
        return itemDef;
    }

    public void readValues(Buffer buffer) {
        do {
            int opcode = buffer.readUnsignedByte();
            if (opcode == 0)
                return;
            if (opcode == 1)
                spriteId = buffer.readInt();
            else if (opcode == 2)
                field3294 = buffer.readInt();
            else if (opcode == 3)
                name = buffer.readNewString();
            else if (opcode == 4)
                field3296 = buffer.read24Int();
            else if (opcode == 5)
                field3297 = buffer.read24Int();
            else if (opcode == 6)
                textSize = buffer.readUnsignedByte();
            else if(opcode == 7)
                flags = buffer.readUnsignedByte();
            if ((flags & 0x1) == 0) {
                this.field1944 = false;
            }
            if ((flags & 0x2) == 2) {
                this.field1945 = true;
            }
            else if (opcode == 8)
                opcode8 = buffer.readByte();
            else if (opcode >= 10 && opcode < 14) {
                if (actions  == null)
                    actions = new String[5];
                actions[opcode - 10] = buffer.readNewString();
            } else if (opcode == 15) {
                int size = buffer.readUnsignedByte();
                anIntArray1982 = new int[size * 2];

                for (int i = 0; i < size * 2; ++i) {
                    anIntArray1982[i] = buffer.readSignedWord();
                }

                buffer.readInt();
                int size2 = buffer.get_unsignedbyte();
                anIntArray1981 = new int[size2];

                for (int i = 0; i < anIntArray1981.length; ++i) {
                    anIntArray1981[i] = buffer.readInt();
                }

                aByteArray1979 = new byte[size];

                for (int i = 0; i < size; ++i) {
                    aByteArray1979[i] = buffer.readByte();
                }
            } else if(opcode == 17){
                aString1970 = buffer.readString();
            } else if(opcode == 18){
                opcode18 = buffer.readInt();
            } else if(opcode == 19){
                category = buffer.readUShort();
            } else if(opcode == 21){
                int22 = buffer.readInt();
            } else if (opcode == 23) {
                int23_1 = buffer.readByte();
                int23_2 = buffer.readByte();
                int23_3 = buffer.readByte();
            } else if (opcode == 24) {
                int24_1 = (short) buffer.readSignedWord();
                int24_2 = (short) buffer.readSignedWord();
            } else if (opcode == 25){
                int25 = buffer.readInt();
            } else if (opcode == 28) {
                int28 = buffer.readByte();
            } else if (opcode == 29) {
                int29 = buffer.readByte();
            } else if (opcode == 30) {
                int30 = buffer.readByte();
            }
        } while (true);
    }

}