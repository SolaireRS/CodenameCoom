package com.client.definitions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.stream.IntStream;

import com.client.Configuration;
import com.client.DrawingArea;
import com.client.MRUNodes;
import com.client.Model;
import com.client.Rasterizer;
import com.client.Sprite;
import com.client.Buffer;
import com.client.StreamLoader;
import com.client.definitions.custom.ItemDefinition_Sub1;
import com.client.sign.Signlink;
import com.client.utilities.FieldGenerator;
import com.client.utilities.FileOperations;
import com.client.utilities.TempWriter;
import com.google.common.base.Preconditions;

public final class ItemDefinition {

	public byte[] customSpriteLocation;
	public byte[] customSmallSpriteLocation;
	public int field2142 = -1;
	public int field2157 = -1;
	public int field2158 = -1;
	private int field2182;
	public String[][] subOps;

	public static void unpackConfig(final StreamLoader streamLoader) {
		// stream = new Stream(streamLoader.getArchiveData("obj.dat"));
		// Stream stream = new Stream(streamLoader.getArchiveData("obj.idx"));
		stream = new Buffer(streamLoader.getArchiveData("obj.dat"));
		final Buffer stream = new Buffer(streamLoader.getArchiveData("obj.idx"));
		totalItems = stream.readUShort();
		streamIndices = new int[totalItems + 20_000];
		int i = 2;
		for (int j = 0; j < totalItems; j++) {
			streamIndices[j] = i;
			i += stream.readUShort();
		}

		cache = new ItemDefinition[10];
		for (int index = 0; index < 10; index++) {
			cache[index] = new ItemDefinition();
		}
		//dumpGameItemConfig();
		//dumpItems2();
		if (Configuration.dumpDataLists) {
			TempWriter writer2 = new TempWriter("item_fields");
			FieldGenerator generator = new FieldGenerator(writer2::writeLine);
			IntStream.range(0, 100_000).forEach(id -> {
				try {
					ItemDefinition definition = ItemDefinition.forID(id);
					generator.add(definition.name + (definition.certTemplateID != -1 ? " noted" : ""), id);
				} catch (Exception e) {
				}
			});
			writer2.close();
		}
	}

	public static ItemDefinition forID(int itemId) {
		for (int j = 0; j < 10; j++) {
			if (cache[j].id == itemId) {
				return cache[j];
			}
		}

		if (itemId == -1)
			itemId = 0;
		if (newCustomItems(itemId) != null) {
			return newCustomItems(itemId);
		}
		if (itemId > streamIndices.length)
			itemId = 0;

		cacheIndex = (cacheIndex + 1) % 10;
		ItemDefinition itemDef = cache[cacheIndex];

		stream.currentOffset = streamIndices[itemId];
		itemDef.id = itemId;
		itemDef.setDefaults();
		itemDef.readValues(stream);

		if (itemDef.certTemplateID != -1) {
			itemDef.toNote();
		}

		if (itemDef.notedId != -1) {
			itemDef.method2789(forID(itemDef.notedId), forID(itemDef.unnotedId));
		}

		if (itemDef.placeholderTemplateId != -1) {
			itemDef.method2790(forID(itemDef.placeholderTemplateId), forID(itemDef.placeholderId));
		}

		int id = itemDef.id;
		ItemDefinition_Sub1.itemDef(itemDef.id, itemDef);
		//ItemDefinition_Sub2.itemDef(itemDef.id, itemDef);
		//ItemDefinition_Sub3.itemDef(itemDef.id, itemDef);
		customItems(itemDef.id);
		itemDef.id = id; // Have to do this for some cases
		return itemDef;
	}
	public static ItemDefinition copy(ItemDefinition itemDef, int newId, int copyingItemId, String newName, String...actions) {
		ItemDefinition copyItemDef = forID(copyingItemId);
		itemDef.id = newId;
		itemDef.name = newName;
		itemDef.description = copyItemDef.description;
		itemDef.modifiedModelColors = copyItemDef.modifiedModelColors;
		itemDef.originalModelColors = copyItemDef.originalModelColors;
		itemDef.modelId = copyItemDef.modelId;
		itemDef.primaryMaleModel = copyItemDef.primaryMaleModel;
		itemDef.primaryFemaleModel = copyItemDef.primaryFemaleModel;
		itemDef.spriteScale = copyItemDef.spriteScale;
		itemDef.spritePitch = copyItemDef.spritePitch;
		itemDef.spriteCameraRoll = copyItemDef.spriteCameraRoll;
		itemDef.spriteTranslateX = copyItemDef.spriteTranslateX;
		itemDef.spriteTranslateY = copyItemDef.spriteTranslateY;
		itemDef.itemActions = copyItemDef.itemActions;
		itemDef.itemActions = new String[5];
		if (actions != null) {
			for (int index = 0; index < actions.length; index++) {
				itemDef.itemActions[index] = actions[index];
			}
		}
		return itemDef;
	}
	private int currentcolors;
	private int currenttextures;
	//Start item dump
	public static void dumpItems2() {
		for(int i = 0; i < totalItems; i++) {
			ItemDefinition itemDef = forID(i);
			BufferedWriter bw = null;

			try {
				itemDef.currentcolors = 0;
				bw = new BufferedWriter(new FileWriter(Signlink.getCacheDirectory() + "/dumps/211itemdump.txt", true));

				bw.newLine();
				bw.write("	if(i == "+i+") //ID");
				bw.newLine();
				bw.write("		{");
				bw.newLine();
				bw.write("			itemDef.setDefaults();");
				bw.newLine();
				bw.write("			itemDef.itemActions = new String[] {"+ Arrays.toString(itemDef.itemActions)+"};");
				bw.newLine();
				bw.write("			itemDef.groundActions = new String[] {"+Arrays.toString(itemDef.groundActions)+"};");
				bw.newLine();
				bw.write("			itemDef.name = \""+itemDef.name+"\"; //Name");
				bw.newLine();
				bw.write("			itemDef.description = \"Its an "+itemDef.name+"\"; //Description");
				bw.newLine();
				if(itemDef.originalModelColors != null) {
					for(int i2 = 0; i2 < itemDef.originalModelColors.length; i2++) {
						if(i2 == 0) {
						}
						if(i2 != itemDef.originalModelColors.length - 1) {
							itemDef.currentcolors += 1;
						} else {
							itemDef.currentcolors += 1;                         									if(itemDef.currentcolors != 0)
							{
								bw.write("			itemDef.originalModelColors = new int["+itemDef.currentcolors+"];");
								bw.newLine();
								bw.write("			itemDef.modifiedModelColors = new int["+itemDef.currentcolors+"];");
								bw.newLine();
							}
							itemDef.currentcolors = 0;
						}
					}
				}
				if(itemDef.originalModelColors != null) {
					for(int i2 = 0; i2 < itemDef.originalModelColors.length; i2++) {
						if(i2 == 0) {
						}
						if(i2 != itemDef.originalModelColors.length - 1) {                             	bw.write("			itemDef.originalModelColors["+itemDef.currentcolors+"] = " +itemDef.originalModelColors[i2]+";");
							itemDef.currentcolors += 1;
							bw.newLine();
						} else {                            						bw.write("			itemDef.originalModelColors["+itemDef.currentcolors+"] = " +itemDef.originalModelColors[i2]+";");
							itemDef.currentcolors = 0;
							bw.newLine();
						}
					}
				}
				if(itemDef.modifiedModelColors != null) {
					for(int i2 = 0; i2 < itemDef.modifiedModelColors.length; i2++) {
						if(i2 == 0) {
						}
						if(i2 != itemDef.modifiedModelColors.length - 1) {                             	bw.write("			itemDef.modifiedModelColors["+itemDef.currentcolors+"] = " +itemDef.modifiedModelColors[i2]+";");
							itemDef.currentcolors += 1;
							bw.newLine();
						} else {                            						bw.write("			itemDef.modifiedModelColors["+itemDef.currentcolors+"] = " +itemDef.modifiedModelColors[i2]+";");
							itemDef.currentcolors = 0;
							bw.newLine();
						}
					}
					if(itemDef.modifiedTextureColors != null) {
						for(int i2 = 0; i2 < itemDef.modifiedTextureColors.length; i2++) {
							if(i2 == 0) {
							}
							if(i2 != itemDef.modifiedTextureColors.length - 1) {
								itemDef.currenttextures += 1;
							} else {
								itemDef.currenttextures += 1;                         									if(itemDef.currenttextures != 0)
								{
									bw.write("			itemDef.originalTextureColors = new int["+itemDef.currenttextures+"];");
									bw.newLine();
									bw.write("			itemDef.modifiedTextureColors = new int["+itemDef.currenttextures+"];");
									bw.newLine();
								}
								itemDef.currenttextures = 0;
							}
						}
					}
					if(itemDef.modifiedTextureColors != null) {
						for(int i2 = 0; i2 < itemDef.modifiedTextureColors.length; i2++) {
							if(i2 == 0) {
							}
							if(i2 != itemDef.modifiedTextureColors.length - 1) {                             	bw.write("			itemDef.modifiedTextureColors["+itemDef.currenttextures+"] = " +itemDef.modifiedTextureColors[i2]+";");
								itemDef.currenttextures += 1;
								bw.newLine();
							} else {                            						bw.write("			itemDef.modifiedTextureColors["+itemDef.currenttextures+"] = " +itemDef.modifiedTextureColors[i2]+";");
								itemDef.currenttextures = 0;
								bw.newLine();
							}
						}
					}
					if(itemDef.originalTextureColors != null) {
						for(int i2 = 0; i2 < itemDef.originalTextureColors.length; i2++) {
							if(i2 == 0) {
							}
							if(i2 != itemDef.originalTextureColors.length - 1) {                             	bw.write("			itemDef.originalTextureColors["+itemDef.currenttextures+"] = " +itemDef.originalTextureColors[i2]+";");
								itemDef.currenttextures += 1;
								bw.newLine();
							} else {                            						bw.write("			itemDef.originalTextureColors["+itemDef.currenttextures+"] = " +itemDef.originalTextureColors[i2]+";");
								itemDef.currenttextures = 0;
								bw.newLine();
							}
						}
					}
					if(itemDef.stackAmounts != null) {
						for(int i2 = 0; i2 < itemDef.stackAmounts.length; i2++) {
							if(i2 == 0) {
							}
							if(i2 != itemDef.stackAmounts.length - 1) {
								itemDef.currentcolors += 1;
							} else {
								itemDef.currentcolors += 1;                         									if(itemDef.currentcolors != 0)
								{
									bw.write("			itemDef.stackAmounts = new int["+itemDef.currentcolors+"];");
									bw.newLine();
									bw.write("			itemDef.stackIds = new int["+itemDef.currentcolors+"];");
									bw.newLine();
								}
								itemDef.currentcolors = 0;
							}
						}
					}

					if(itemDef.stackAmounts != null) {
						for(int i2 = 0; i2 < itemDef.stackAmounts.length; i2++) {
							if(i2 == 0) {
							}
							if(i2 != itemDef.stackAmounts.length - 1) {                             	bw.write("			itemDef.stackAmounts["+itemDef.currentcolors+"] = " +itemDef.stackAmounts[i2]+";");
								itemDef.currentcolors += 1;
								bw.newLine();
							} else {                            						bw.write("			itemDef.stackAmounts["+itemDef.currentcolors+"] = " +itemDef.stackAmounts[i2]+";");
								itemDef.currentcolors = 0;
								bw.newLine();
							}
						}
					}
					if(itemDef.stackIDs != null) {
						for(int i2 = 0; i2 < itemDef.stackIDs.length; i2++) {
							if(i2 == 0) {
							}
							if(i2 != itemDef.stackIDs.length - 1) {                             	bw.write("			itemDef.stackIds["+itemDef.currentcolors+"] = " +itemDef.stackIDs[i2]+";");
								itemDef.currentcolors += 1;
								bw.newLine();
							} else {                            						bw.write("			itemDef.stackIds["+itemDef.currentcolors+"] = " +itemDef.stackIDs[i2]+";");
								itemDef.currentcolors = 0;
								bw.newLine();
							}
						}
					}
				}
				bw.write("			itemDef.modelId = "+itemDef.modelId+";");
				bw.newLine();
				bw.write("			itemDef.spriteScale = "+itemDef.spriteScale+";");
				bw.newLine();
				bw.write("			itemDef.spritePitch = "+itemDef.spritePitch+";");
				bw.newLine();
				bw.write("			itemDef.spriteCameraRoll = "+itemDef.spriteCameraRoll+";");
				bw.newLine();
				bw.write("			itemDef.spriteCameraYaw = "+itemDef.spriteCameraYaw+";");
				bw.newLine();
				bw.write("			itemDef.spriteTranslateX = "+itemDef.spriteTranslateX+";");
				bw.newLine();
				bw.write("			itemDef.spriteTranslateY = "+itemDef.spriteTranslateY+";");
				bw.newLine();
				bw.write("			itemDef.primaryMaleModel = "+itemDef.primaryMaleModel+";");
				bw.newLine();
				bw.write("			itemDef.primaryFemaleModel = "+itemDef.primaryFemaleModel+";");
				bw.newLine();
				bw.write("			itemDef.secondaryMaleModel = "+itemDef.secondaryMaleModel+";");
				bw.newLine();
				bw.write("			itemDef.secondaryFemaleModel = "+itemDef.secondaryFemaleModel+";");
				bw.newLine();
				bw.write("			itemDef.primaryMaleHeadPiece = "+itemDef.primaryMaleHeadPiece+";");
				bw.newLine();
				bw.write("			itemDef.primaryFemaleHeadPiece = "+itemDef.primaryFemaleHeadPiece+";");
				bw.newLine();
				bw.write("			itemDef.value = "+itemDef.value+";");
				bw.newLine();
				bw.write("			itemDef.unnotedId = " + itemDef.unnotedId + ";");
				bw.newLine();
				bw.write("			itemDef.notedId = " + itemDef.notedId + ";");
				bw.newLine();
				bw.write("			itemDef.certID = " + itemDef.certID + ";");
				bw.newLine();
				bw.write("			itemDef.certTemplateID = " + itemDef.certTemplateID + ";");
				bw.newLine();
				bw.write("			itemDef.stackable = " + itemDef.stackable + ";");
				bw.newLine();
				bw.write("			itemDef.placeholderId = " + itemDef.placeholderId + ";");
				bw.newLine();
				bw.write("			itemDef.placeholderTemplateId = " + itemDef.placeholderTemplateId + ";");
				bw.newLine();
				bw.write("		}");
				bw.newLine();
				bw.newLine();
				bw.flush();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				if (bw != null) try {
					bw.close();
				} catch (IOException ioe2) {
				}
			}
		}
	}
	public static void dumpGameItemConfig() {
		for(int i = 0; i < totalItems; i++) {
			ItemDefinition class5 = forID(i);
			BufferedWriter bw = null;
			try {
				bw = new BufferedWriter(new FileWriter("./temp/gameitem.txt", true));
				if(class5.name!= null) {
					bw.write("new GameItem("+i+"), // "+class5.name);
					bw.newLine();
					bw.flush();
					bw.close();
				}
			} catch (IOException ioe2) {
			}
		}
	}
	private static ItemDefinition newCustomItems(int itemId) {
		ItemDefinition itemDef = new ItemDefinition();
		itemDef.setDefaults();
		switch (itemId) {
			case 30000:
				return copy(itemDef, 30_000, 11738, "Resource box(small)", "Open");
			case 30001:
				return copy(itemDef, 30_001, 11738, "Resource box(medium)", "Open");
			case 30002:
				return copy(itemDef, 30_002, 11738, "Resource box(large)", "Open");
			case 22375:
				return copy(itemDef, 22375, 22374, "Mossy key");
			case 22689:
				return copy(itemDef, 22689, 0, "Null");
			case 22690:
				return copy(itemDef, 22690, 0, "Null");
			case 22692:
				return copy(itemDef, 22692, 0, "Null");
			case 22693:
				return copy(itemDef, 22693, 0, "Null");
			case 22695:
				return copy(itemDef, 22695, 0, "Null");
			case 22696:
				return copy(itemDef, 22696, 0, "Null");
			case 22698:
				return copy(itemDef, 22698, 0, "Null");
			case 22699:
				return copy(itemDef, 22699, 0, "Null");
			case 22701:
				return copy(itemDef, 22701, 0, "Null");
			case 22702:
				return copy(itemDef, 22702, 0, "Null");
			case 33056:
				itemDef.setDefaults();
				itemDef.id = 33056;
				itemDef.modelId = 65270;
				itemDef.name = "Completionist cape";
				itemDef.description = "A cape worn by those who've overachieved.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 65297;
				itemDef.primaryFemaleModel = 65316;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Teleports";
				itemDef.itemActions[3] = "Features";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 33057:
				itemDef.setDefaults();
				itemDef.id = 33057;
				itemDef.modelId = 65273;
				itemDef.name = "Completionist hood";
				itemDef.description = "A hood worn by those who've over achieved.";

				itemDef.spriteScale = 760;
				itemDef.spritePitch = 11;
				itemDef.spriteCameraRoll = 0;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 0;

				itemDef.primaryMaleModel = 65292;
				itemDef.primaryFemaleModel = 65310;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				return itemDef;
			case 8817:
				itemDef.setDefaults();
				itemDef.id = 8817;
				itemDef.modelId = 72300;
				itemDef.name = "scythe of Osiris";
				itemDef.description = "A scythe for the finest.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 72300;
				itemDef.primaryFemaleModel = 72300;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32499:
				itemDef.setDefaults();
				itemDef.id = 32499;
				itemDef.modelId = 72401;
				itemDef.name = "Lava Scythe";
				itemDef.description = "A scythe from lava.";
				itemDef.spriteScale = 2105;
				itemDef.spritePitch = 327;
				itemDef.spriteCameraRoll = 23;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 1;
				itemDef.spriteTranslateY = 17;

				itemDef.primaryMaleModel = 72400;
				itemDef.primaryFemaleModel = 72400;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32047:
				itemDef.setDefaults();
				itemDef.id = 32047;
				itemDef.modelId = 75742;
				itemDef.name = "Ice Scythe";
				itemDef.description = "A scythe from Ice.";
				itemDef.spriteScale = 2105;
				itemDef.spritePitch = 327;
				itemDef.spriteCameraRoll = 23;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 1;
				itemDef.spriteTranslateY = 17;

				itemDef.primaryMaleModel = 72906;
				itemDef.primaryFemaleModel = 72906;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;

			case 32180:
				itemDef.setDefaults();
				itemDef.id = 32180;
				itemDef.modelId = 70000;
				itemDef.name = "Osiris Chainmace";
				itemDef.description = "A Mace From Hell.";
				itemDef.spriteScale = 2105;
				itemDef.spritePitch = 23;
				itemDef.spriteCameraRoll = 327;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 1;
				itemDef.spriteTranslateY = 17;

				itemDef.primaryMaleModel = 70000;
				itemDef.primaryFemaleModel = 70000;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;

			case 38029:
				itemDef.setDefaults();
				itemDef.id = 38029;
				itemDef.modelId = 65266;
				itemDef.name = "Godly Twisted Bow";
				itemDef.description = "Godly T'Bow.";
				itemDef.spriteScale = 1814;
				itemDef.spritePitch = 23;
				itemDef.spriteCameraRoll = 720;
				itemDef.spriteCameraYaw = 1500;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 1;

				itemDef.primaryMaleModel = 65267;
				itemDef.primaryFemaleModel = 65267;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 38813:
				itemDef.setDefaults();
				itemDef.id = 38813;
				itemDef.name = "Lava Torva Platebody";
				itemDef.description = "A body from lava.";
				itemDef.modelId = 53227;
				itemDef.spriteScale = 1480;
				itemDef.spritePitch = 518;
				itemDef.spriteCameraRoll = 0;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = -1;
				itemDef.primaryMaleModel = 53135;
				itemDef.primaryFemaleModel = 53165;
				itemDef.secondaryMaleModel = 53145;
				itemDef.secondaryFemaleModel = 53167;
				itemDef.primaryMaleHeadPiece = -1;
				itemDef.primaryFemaleHeadPiece = -1;
				itemDef.value = 600000;
				itemDef.unnotedId = -1;
				itemDef.notedId = -1;
				itemDef.certID = -1;
				itemDef.certTemplateID = -1;
				itemDef.stackable = false;
				itemDef.placeholderId = -1;
				itemDef.placeholderTemplateId = -1;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 38812:
				itemDef.setDefaults();
				itemDef.id = 38812;
				itemDef.modelId = 65134;
				itemDef.name = "Lava Torva Platelegs";
				itemDef.description = "Platelegs made from lava.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 2045;
				itemDef.spriteCameraRoll = 474;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = 8;

				itemDef.primaryMaleModel = 65135;
				itemDef.primaryFemaleModel = 65135;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 31814:
				itemDef.setDefaults();
				itemDef.id = 38814;
				itemDef.modelId = 65130;
				itemDef.name = "Lava Torva Full Helm";
				itemDef.description = "A Helm from lava.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 1867;
				itemDef.spriteCameraRoll = 85;
				itemDef.spriteCameraYaw = 1;
				//itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = -2;
				itemDef.itemActions = new String[5];
				itemDef.primaryMaleModel = 65131;
				itemDef.primaryFemaleModel = 65131;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 19498:
				itemDef.setDefaults();
				itemDef.id = 19498;
				itemDef.modelId = 65145;
				itemDef.name = "Mike's Torva Platebody";
				itemDef.description = "Mike's plate.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 2042;
				itemDef.spriteCameraRoll = 473;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = -1;

				itemDef.primaryMaleModel = 65146;
				itemDef.secondaryMaleModel = 65147;
				itemDef.primaryFemaleModel = 65146;
				itemDef.secondaryFemaleModel = 65147;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32377:
				itemDef.setDefaults();
				itemDef.id = 32377;
				itemDef.modelId = 65248;
				itemDef.name = "Godly Torva Platebody";
				itemDef.description = "Godly plate.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 2042;
				itemDef.spriteCameraRoll = 473;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = -1;

				itemDef.primaryMaleModel = 65247;
				itemDef.secondaryMaleModel = 65246;
				itemDef.primaryFemaleModel = 65247;
				itemDef.secondaryFemaleModel = 65246;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 19495:
				itemDef.setDefaults();
				itemDef.id = 19495;
				itemDef.modelId = 65148;
				itemDef.name = "Mike's Torva Platelegs";
				itemDef.description = "Mike's Platelegs made from lava.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 2045;
				itemDef.spriteCameraRoll = 474;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = 8;

				itemDef.primaryMaleModel = 65149;
				itemDef.primaryFemaleModel = 65149;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32376:
				itemDef.setDefaults();
				itemDef.id = 32376;
				itemDef.modelId = 65249;
				itemDef.name = "Godly Torva Platelegs";
				itemDef.description = "Godly Platelegs made from lava.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 2045;
				itemDef.spriteCameraRoll = 474;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = 8;

				itemDef.primaryMaleModel = 65251;
				itemDef.primaryFemaleModel = 65251;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32380:
				itemDef.setDefaults();
				itemDef.id = 32380;
				itemDef.modelId = 65350;
				itemDef.name = "Godly Masori Chaps";
				itemDef.description = "Godly Chaps made from the gods.";
				itemDef.spriteScale = 1033;
				itemDef.spritePitch = 102;
				itemDef.spriteCameraRoll = 41;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = -2;
				itemDef.spriteTranslateY = -3;

				itemDef.primaryMaleModel = 65351;
				itemDef.primaryFemaleModel = 65351;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32382:
				itemDef.setDefaults();
				itemDef.id = 32382;
				itemDef.modelId = 65352;
				itemDef.name = "Godly Masori Body";
				itemDef.description = "Godly Body made from the gods.";
				itemDef.spriteScale = 1240;
				itemDef.spritePitch = 102;
				itemDef.spriteCameraRoll = 453;
				itemDef.spriteCameraYaw = 0;
				//itemDef.spriteTranslateX = -2;
				itemDef.spriteTranslateY = 11;

				itemDef.primaryMaleModel = 65353;
				itemDef.primaryFemaleModel = 65353;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32381:
				itemDef.setDefaults();
				itemDef.id = 32381;
				itemDef.modelId = 65354;
				itemDef.name = "Godly Masori Mask";
				itemDef.description = "Godly Mask made from the gods.";
			//	itemDef.spriteScale = 1240;
				itemDef.spritePitch = 102;
				itemDef.spriteCameraRoll = 555;
				itemDef.spriteCameraYaw = 0;
				//itemDef.spriteTranslateX = -2;
				itemDef.spriteTranslateY = 4;

				itemDef.primaryMaleModel = 65355;
				itemDef.primaryFemaleModel = 65355;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 19483:
				itemDef.setDefaults();
				itemDef.id = 19483;
				itemDef.modelId = 65150;
				itemDef.name = "Mike's Torva Full Helm";
				itemDef.description = "Mike's Torva Full Helm.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 1867;
				itemDef.spriteCameraRoll = 85;
				itemDef.spriteCameraYaw = 1;
				//itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = -2;
				itemDef.itemActions = new String[5];
				itemDef.primaryMaleModel = 65151;
				itemDef.primaryFemaleModel = 65151;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32378:
				itemDef.setDefaults();
				itemDef.id = 32378;
				itemDef.modelId = 65255;
				itemDef.name = "Godly Torva Full Helm";
				itemDef.description = "Godly Torva Full Helm.";
				itemDef.spriteScale = 1780;
				itemDef.spritePitch = 1867;
				itemDef.spriteCameraRoll = 85;
				itemDef.spriteCameraYaw = 1;
				//itemDef.spriteTranslateX = -1;
				itemDef.spriteTranslateY = -2;
				itemDef.itemActions = new String[5];
				itemDef.primaryMaleModel = 65254;
				itemDef.primaryFemaleModel = 65254;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wield";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 4371:
				itemDef.setDefaults();
				itemDef.id = 4371;
				itemDef.modelId = 76670;
				itemDef.name = "Admin Cape";
				itemDef.description = "A Cape for the finest.";
				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;
				itemDef.primaryMaleModel = 76670;
				itemDef.primaryFemaleModel = 76670;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Teleports";
				itemDef.itemActions[3] = "Features";
				itemDef.itemActions[4] = "Drop";
				return itemDef;
			case 32930:
				itemDef.setDefaults();
				itemDef.id = 32930;
				itemDef.modelId = 70500;
				itemDef.name = "burning skeleteon boots";
				itemDef.description = "spooky skeleton.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 70500;
				itemDef.primaryFemaleModel = 70500;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Drop";
				return itemDef;
			case 32931:
				itemDef.setDefaults();
				itemDef.id = 32931;
				itemDef.modelId = 70501;
				itemDef.name = "Burning skeleteon Gloves";
				itemDef.description = "spooky skeleton.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 70501;
				itemDef.primaryFemaleModel = 70501;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Drop";
				return itemDef;
			case 32932:
				itemDef.setDefaults();
				itemDef.id = 32932;
				itemDef.modelId = 70502;
				itemDef.name = "Burning skeleteon mask";
				itemDef.description = "spooky skeleton.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 70502;
				itemDef.primaryFemaleModel = 70502;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Drop";
				return itemDef;
			case 32933:
				itemDef.setDefaults();
				itemDef.id = 32933;
				itemDef.modelId = 70503;
				itemDef.name = "Burning skeleteon leggings";
				itemDef.description = "spooky skeleton.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 70503;
				itemDef.primaryFemaleModel = 70503;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Drop";
				return itemDef;
			case 32934:
				itemDef.setDefaults();
				itemDef.id = 32934;
				itemDef.modelId = 70505;
				itemDef.name = "Burning skeleteon Skirt";
				itemDef.description = "spooky skeleton.";

				itemDef.spriteScale = 1385;
				itemDef.spritePitch = 279;
				itemDef.spriteCameraRoll = 948;
				itemDef.spriteCameraYaw = 0;
				itemDef.spriteTranslateX = 0;
				itemDef.spriteTranslateY = 24;

				itemDef.primaryMaleModel = 70505;
				itemDef.secondaryMaleModel = 70504;
				itemDef.primaryFemaleModel = 70505;
				itemDef.secondaryFemaleModel = 70504;
				//itemDef.groundActions = new String[5];
				//itemDef.groundActions[2] = "Take";
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = "Drop";
				return itemDef;
		}

		return null;
	}

	private byte[] getCustomSprite(String img) {
		String location = (Sprite.location + Configuration.CUSTOM_ITEM_SPRITES_DIRECTORY + img).toLowerCase();
		byte[] spriteData = FileOperations.readFile(location);
		Preconditions.checkState(spriteData != null, "No sprite: " + location);
		return spriteData;
	}

	public void createCustomSprite(String img) {
		customSpriteLocation = getCustomSprite(img);
	}

	public void createSmallCustomSprite(String img) {
		customSmallSpriteLocation = getCustomSprite(img);
	}

	private static void customItems(int itemId) {
		ItemDefinition itemDef = forID(itemId);
		switch (itemId) {
			case 21726:
			case 21728:
				itemDef.stackable = true;
				break;
			case 12863:
				itemDef.itemActions = new String[] { "Open", null, null, null, null};
				break;
			case 13092: //this makes crystal halberds wieldable, weird af.
			case 13093:
			case 13094:
			case 13095:
			case 13096:
			case 13097:
			case 13098:
			case 13099:
			case 13100:
			case 13101:
				itemDef.itemActions = new String[] { null, "Wield", null, null, null};
				break;
			case 23933:
				itemDef.name = "Vote crystal";
				break;
			case 9698:
				itemDef.name = "Unfired burning rune";
				itemDef.description = "I should burn this.";
				itemDef.createCustomSprite("Unfired_burning_rune.png");
				itemDef.stackable = true;
				break;
			case 9699:
				itemDef.name = "Burning rune";
				itemDef.description = "Hot to the touch.";
				itemDef.createCustomSprite("Burning_rune.png");
				itemDef.stackable = true;
				break;
			case 23778:
				itemDef.name = "Uncut toxic gem";
				itemDef.description = "I should use a chisel on this.";
				break;
			case 22374:
				itemDef.name = "Hespori key";
				itemDef.description = "Can be used on the Hespori chest.";
				break;
			case 3460:
				itemDef.name = "@bla@Nex @red@key";
				break;
			case 23783:
				itemDef.name = "Toxic gem";
				itemDef.description = "I should be careful with this.";
				break;
			case 9017:
				itemDef.name = "Hespori essence";
				itemDef.description = "Maybe I should burn this.";
				itemDef.itemActions = new String[] {  null, null, null, null, "Drop" };
				break;
			case 19473:
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				break;
			case 10556:
			case 10557:
			case 10558:
			case 10559:
				itemDef.itemActions = new String[] { null, "Wear", "Feature", null, "Drop" };
				break;
			case 21898:
				itemDef.itemActions = new String[] { null, "Wear", "Teleports", "Features", null };
				break;
			case 12873:
			case 12875:
			case 12877:
			case 12879:
			case 12881:
			case 12883:
				itemDef.itemActions = new String[] { "Open", null, null, null, "Drop" };
				break;
			case 23804:
				itemDef.name = "Imbue Dust";
				break;
			case 22517:
				itemDef.name = "Crystal Shard";
				break;
			case 23951:
				itemDef.name = "Crystalline Key";
				break;
			case 691:
				itemDef.name = "@gre@10,000 FoE Point Certificate";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				break;
			case 692:
				itemDef.name = "@red@25,000 FoE Point Certificate";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				break;
			case 693:
				itemDef.name = "@cya@50,000 FoE Point Certificate";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				break;
			case 696:
				itemDef.name = "@yel@250,000 FoE Point Certificate";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				break;
			case 23877:
				itemDef.name = "Crystal Shard";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = true;
				break;
			case 23943:
				itemDef.itemActions = new String[] { null, "Wear", "Uncharge", "Check", "Drop" };
				break;
			case 2996:
				itemDef.name = "@red@PKP Ticket";
				break;
			case 23776:
				itemDef.name = "@red@Hunllef's Key";
				break;
			case 13148:
				itemDef.name = "@red@Reset Lamp";
				break;
			case 6792:
				itemDef.name = "@red@Seren's Key";
				break;
			case 4185:
				itemDef.name = "@red@Porazdir's Key";
				break;
			case 21880:
				itemDef.name = "Wrath Rune";
				itemDef.value = 1930;
				break;
			case 12885:
			case 13277:
			case 19701:
			case 13245:
			case 12007:
			case 22106:
			case 12936:
			case 24495:
				itemDef.itemActions = new String[] { null, null, "Open", null, "Drop" };
				break;
			case 21262:
				itemDef.name = "Vote Genie Pet";
				itemDef.itemActions = new String[] { null, null, null, null, "Release" };
				break;
			case 21817:
				itemDef.itemActions = new String[] { null, "Wear", "Dismantle", null, null, };
				break;
			case 21347:
				itemDef.itemActions = new String[] { null, null, null, "Chisel-Options", null, };
				break;
			case 21259:
				itemDef.name = "@red@Name Change Scroll";
				itemDef.itemActions = new String[] { null, null, "Read", null, null, };
				break;
			case 22547:
			case 22552:
			case 22542:
				itemDef.itemActions = new String[] { null, null, null, null, null, };
				break;
			case 22555:
			case 22550:
			case 22545:
				itemDef.itemActions = new String[] { null, "Wield", "Check", "Uncharge", null, };
				break;
			case 732:
				itemDef.name = "@blu@Imbuedeifer";
				itemDef.value = 1930;
				break;
			case 21881:
				itemDef.name = "Wrath Rune";
				itemDef.value = 1930;
				break;
			case 13226:
				itemDef.name = "Herb Sack";
				itemDef.description = "Thats a nice looking sack.";
				break;
			case 3456:
				itemDef.name = "@whi@Common Raids Key";
				itemDef.description = "Can be used on the storage unit.";
				break;
			case 3464:
				itemDef.name = "@pur@Rare Raids Key";
				itemDef.description = "Can be used on the storage unit.";
				break;
			case 6829:
				itemDef.name = "@red@YT Video Giveaway Box";
				itemDef.description = "Spawns items to giveaway for your youtube video.";
				itemDef.itemActions = new String[] { "Giveaway", null, null, null, "Drop" };
				break;
			case 6831:
				itemDef.name = "@red@YT Video Giveaway Box (t2)";
				itemDef.description = "Spawns items to giveaway for your youtube video.";
				itemDef.itemActions = new String[] { "Giveaway", null, null, null, "Drop" };

				break;
			case 6832:
				itemDef.name = "@red@YT Stream Giveaway Box";
				itemDef.description = "Spawns items to giveaway for your youtube stream.";
				itemDef.itemActions = new String[] { "Giveaway", null, null, null, "Drop" };

				break;
			case 6833:
				itemDef.name = "@red@YT Stream Giveaway Box (t2)";
				itemDef.description = "Spawns items to giveaway for your youtube stream.";
				itemDef.itemActions = new String[] { "Giveaway", null, null, null, "Drop" };

				break;
			case 13190:
				itemDef.name = "@yel@100m OSRS GP";
				itemDef.itemActions = new String[] { "Redeem", null, null, null, "Drop" };
				itemDef.description = "Redeem for 100m OSRS GP!";
				break;
			case 6121:
				itemDef.name = "Break Vials Instruction";
				itemDef.description = "How does one break a vial, its impossible?";
				break;
			case 2528:
				itemDef.name = "@red@Experience Lamp";
				itemDef.description = "Should I rub it......";
				break;
			case 5509:
				itemDef.name = "Small Pouch";
				itemDef.createCustomSprite("Small_pouch.png");
				itemDef.itemActions = new String[] { "Fill", "Empty", "Check", null, null };
				break;
			case 5510:
				itemDef.name = "Medium Pouch";
				itemDef.createCustomSprite("Medium_pouch.png");
				itemDef.itemActions = new String[] { "Fill", "Empty", "Check", null, null };
				break;
			case 5512:
				itemDef.name = "Large Pouch";
				itemDef.createCustomSprite("Large_pouch.png");
				itemDef.itemActions = new String[] { "Fill", "Empty", "Check", null, null };
				break;
			case 10724: //full skeleton
			case 10725:
			case 10726:
			case 10727:
			case 10728:
				itemDef.itemActions = new String[] { null, "Wield", null, null, "Drop" };
				break;
			case 5514:
				itemDef.name = "Giant Pouch";
				itemDef.createCustomSprite("Giant_pouch.png");
				break;
			case 22610: //vesta spear
				itemDef.itemActions = new String[] { null, "Wield", null, null, "Drop" };
				break;
			case 22613: //vesta longsword
				itemDef.itemActions = new String[] { null, "Wield", null, null, "Drop" };
				break;
			case 22504: //stat warhammer
				itemDef.itemActions = new String[] { null, "Wield", null, null, "Drop" };
				break;
			case 4224:
			case 4225:
			case 4226:
			case 4227:
			case 4228:
			case 4229:
			case 4230:
			case 4231:
			case 4232:
			case 4233:
			case 4234:
			case 4235://crystal sheild
				itemDef.itemActions = new String[] { null, "Wield", null, null, "Drop" };
				break;
			case 4212:
			case 4214:
			case 4215:
			case 4216:
			case 4217:
			case 4218:
			case 4219:
			case 4220:
			case 4221:
			case 4222:
			case 4223:
				itemDef.itemActions = new String[] { null, "Wield", null, null, "Drop" };
				break;
			case 2841:
				itemDef.name = "@red@Bonus Exp Scroll";
				itemDef.itemActions = new String[] { "@yel@Activate", null, null, null, "Drop" };
				itemDef.description = "You will get double experience using this scroll.";
				break;
			case 21791:
			case 21793:
			case 21795:
				itemDef.itemActions = new String[] { null, "Wear", null, null, "Drop" };
				break;
			case 19841:
				itemDef.name = "Master Casket";
				break;
			case 21034:
				itemDef.itemActions = new String[] { "Read", null, null, null, "Drop" };
				break;
			case 6830:
				itemDef.name = "@red@raids 2 mbox";
				itemDef.itemActions = new String[] { "Open", null, null, null, "Drop" };
				break;
			case 10025:
				itemDef.name = "@dre@raids 1 mbox";
				itemDef.itemActions = new String[] { "Open", null, null, null, "Drop" };
				break;
			case 21079:
				itemDef.itemActions = new String[] { "Read", null, null, null, "Drop" };
				break;
			case 22093:
				itemDef.name = "@gre@Vote Streak Key";
				itemDef.description = "Thanks for voting!";
				break;
			case 22885:
				itemDef.name = "@gre@Kronos seed";
				itemDef.description = "Provides whole server with bonus xp for 1 skill for 5 hours!";
				break;
			case 23824:
				itemDef.name = "Slaughter charge";
				itemDef.description = "Can be used on bracelet of slaughter to charge it.";
				break;
			case 22883:
				itemDef.name = "@gre@Iasor seed";
				itemDef.description = "Increased drop rate (+10%) for whole server for 5 hours!";
				break;
			case 22881:
				itemDef.name = "@gre@Attas seed";
				itemDef.description = "Provides the whole server with bonus xp for 5 hours!";
				break;
			case 20906:
				itemDef.name = "@gre@Golpar seed";
				itemDef.description = "Provides whole server with double c keys, resource boxes, coin bags, and clues!";
				break;
			case 6112:
				itemDef.name = "@gre@Kelda seed";
				itemDef.description = "Provides whole server with x2 Larren's keys for 1 hour!";
				break;
			case 20903:
				itemDef.name = "@gre@Noxifer seed";
				itemDef.description = "Provides whole server with x2 Slayer points for 1 hour!";
				break;
			case 20909:
				itemDef.name = "@gre@Buchu seed";
				itemDef.description = "Provides whole server with x2 Boss points for 1 hour!";
				break;
			case 22869:
				itemDef.name = "@gre@Celastrus seed";
				itemDef.description = "Provides whole server with x2 Brimstone keys for 1 hour!";
				break;
			case 4205:
				itemDef.name = "@gre@Consecration seed";
				itemDef.description = "Provides the whole server with +5 PC points for 1 hour.";
				itemDef.stackable = true;
				break;
			case 11864:
			case 11865:
			case 19639:
			case 19641:
			case 19643:
			case 19645:
			case 19647:
			case 19649:
			case 24444:
			case 24370:
			case 23075:
			case 23073:
			case 21888:
			case 21890:
			case 21264:
			case 21266:
				itemDef.equipActions[2] = "Log";
				itemDef.equipActions[1] = "Check";
				break;
			case 13136:
				itemDef.equipActions[2] = "Elidinis";
				itemDef.equipActions[1] = "Kalphite Hive";
				break;
			case 2550:
				itemDef.equipActions[2] = "Check";
				break;

			case 1712:
			case 1710:
			case 1708:
			case 1706:
			case 19707:
				itemDef.equipActions[1] = "Edgeville";
				itemDef.equipActions[2] = "Karamja";
				itemDef.equipActions[3] = "Draynor";
				itemDef.equipActions[4] = "Al-Kharid";
				break;
			case 21816:
				itemDef.itemActions = new String[] { null, "Wear", "Uncharge", null, "Drop" };
				itemDef.equipActions[1] = "Check";
				itemDef.equipActions[2] = "Toggle-absorption";
				break;
			case 2552:
			case 2554:
			case 2556:
			case 2558:
			case 2560:
			case 2562:
			case 2564:
			case 2566: // Ring of duelling
				itemDef.equipActions[2] = "Shantay Pass";
				itemDef.equipActions[1] = "Clan wars";
				break;
			case 11739:
				itemDef.name = "@gre@Vote Mystery Box";
				itemDef.description = "Probably contains cosmetics, or maybe not...";
				itemDef.itemActions = new String[] { "Open", null, null, null, "Drop" };
				break;
			case 6828:
				itemDef.name = "Super Mystery Box";
				itemDef.description = "Mystery box that contains goodies.";
				itemDef.itemActions = new String[] { "Open", null, "View-Loots", "Quick-Open", "Drop" };
				itemDef.createCustomSprite("Mystery_Box.png");
				itemDef.createSmallCustomSprite("Mystery_Box_Small.png");
				itemDef.stackable = false;
				break;
			case 30010:
				itemDef.setDefaults();
				itemDef.name = "Postie Pete";
				itemDef.description = "50% chance to pick up crystal keys that drop.";
				itemDef.createCustomSprite("Postie_Pete.png");
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				break;
			case 30011:
				itemDef.setDefaults();
				itemDef.name = "Imp";
				itemDef.description = "50% chance to pick up clue scrolls that drop.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Imp.png");
				break;
			case 30012:
				itemDef.setDefaults();
				itemDef.name = "Toucan";
				itemDef.description = "50% chance to pick up resource packs.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Toucan.png");
				break;
			case 30013:
				itemDef.setDefaults();
				itemDef.name = "Penguin King";
				itemDef.description = "50% chance to auto-pick up coin bags.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Penguin_King.png");
				break;
			case 30014:
				itemDef.setDefaults();
				itemDef.name = "K'klik";
				itemDef.description = "An extra 5% in drop rate boost.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("K'klik.png");
				break;
			case 30015:
				itemDef.setDefaults();
				itemDef.name = "Shadow warrior";
				itemDef.description = "50% chance for an additional +10% strength bonus in pvm.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Shadow_warrior.png");
				break;
			case 30016:
				itemDef.setDefaults();
				itemDef.name = "Shadow archer";
				itemDef.description = "50% chance for an additional +10% range str bonus in PvM.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Shadow_archer.png");
				break;
			case 30017:
				itemDef.setDefaults();
				itemDef.name = "Shadow wizard";
				itemDef.description = "50% chance for an additional +10% mage str bonus in PvM.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Shadow_wizard.png");
				break;
			case 30018:
				itemDef.setDefaults();
				itemDef.name = "Healer Death Spawn";
				itemDef.description = "5% chance hit restores HP.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Healer_Death_Spawn.png");
				break;
			case 30019:
				itemDef.setDefaults();
				itemDef.name = "Holy Death Spawn";
				itemDef.description = "5% chance 1/2 of your hit is restored into prayer.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Holy_Death_Spawn.png");
				break;
			case 30020:
				itemDef.setDefaults();
				itemDef.name = "Corrupt beast";
				itemDef.description = "50% chance for an additional +10% strength bonus for melee, mage, and range in pvm.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Corrupt_beast.png");
				break;
			case 30021:
				itemDef.setDefaults();
				itemDef.name = "Roc";
				itemDef.description = "An extra 10% in drop rate boost.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Roc.png");
				break;
			case 30022:
				itemDef.setDefaults();
				itemDef.name = "@red@Kratos";
				itemDef.description = "The most powerful pet, see ::foepets for full list of perks.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Yama.png");
				break;
			case 30023:
				itemDef.setDefaults();
				itemDef.name = "Rain cloud";
				itemDef.description = "Don't worry be happy.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("Rain_cloud.png");
				break;
			case 8866:
				itemDef.name = "Storage chest key (UIM)";
				itemDef.description = "Used to open the UIM storage chest 1 time.";
				itemDef.stackable = true;
				break;
			case 8868:
				itemDef.name = "Perm. storage chest key (UIM)";
				itemDef.description = "Permanently unlocks UIM storage chest.";
				break;
			case 771:
				itemDef.name = "@cya@Ancient branch";
				itemDef.description = "Burning items in the FoE with this branch provides a 1 time +10% FoE value increase.";
				break;
			case 6199:
				itemDef.name = "Mystery Box";
				itemDef.description = "Mystery box that contains goodies.";
				itemDef.itemActions = new String[] { "Open", null, null, "Quick-Open", "Drop" };
				break;
			case 12789:
				itemDef.name = "@red@Youtube Mystery Box";
				itemDef.description = "Mystery box that contains goodies.";
				itemDef.itemActions = new String[] { "Open", null, null, null, "Drop" };
				break;
			case 13346:
				itemDef.name = "Ultra Mystery Box";
				itemDef.itemActions = new String[] { "Open", null, null, "Quick-Open", "Drop" };
				break;
			case 8167:
				itemDef.name = "@or2@FoE Mystery Chest @red@(locked)";
				itemDef.itemActions = new String[] { "Unlock", null, null, "Quick-Open", "Drop" };
				break;
			case 13438:
				itemDef.name = "Slayer Mystery Chest";
				itemDef.itemActions = new String[] { "Open", null, null, null, "Drop" };
				break;
			case 2399:
				itemDef.name = "@or2@FoE Mystery Key";
				itemDef.description = "Used to unlock the FoE Mystery Chest.";
				break;
			case 10832:
				itemDef.name = "Small coin bag";
				itemDef.itemActions = new String[] { "Open", null, "Open-All", null, "Drop" };
				itemDef.description = "I can see some coins inside.";
				break;
			case 10833:
				itemDef.name = "Medium coin bag";
				itemDef.itemActions = new String[] { "Open", null, "Open-All", null, "Drop" };
				itemDef.description = "I can see some coins inside.";
				break;
			case 26500:
				itemDef.name = "Battle Pass";
				itemDef.itemActions = new String[] { "Redeem", null, null, null, null};
				itemDef.description = "Redeem this pass to unlock the premium rewards.";
				break;
			case 26502:
				itemDef.name = "Battle Pass";
				itemDef.itemActions = new String[] { "Redeem", null, null, null, null};
				itemDef.description = "Redeem this pass to unlock the premium rewards.";
				break;
			case 10834:
				itemDef.name = "Large coin bag";
				itemDef.itemActions = new String[] { "Open", null, "Open-All", null, "Drop" };
				itemDef.description = "I can see some coins inside.";
				break;
			case 22316:
				itemDef.name = "Sword of Exilius";
				itemDef.description = "The Sword of Exilius.";
				break;
			case 19942:
				itemDef.name = "Lil Mimic";
				itemDef.description = "It's a lil mimic.";
				break;
			case 30110:
				itemDef.setDefaults();
				itemDef.name = "Dark postie pete";
				itemDef.description = "Picks up all crystal keys and 25% chance to double.";
				itemDef.createCustomSprite("dark_Postie_Pete.png");
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				break;
			case 30111:
				itemDef.setDefaults();
				itemDef.name = "Dark imp";
				itemDef.description = "Picks up all clue scrolls and 25% chance to double.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Imp.png");
				break;
			case 30112:
				itemDef.setDefaults();
				itemDef.name = "Dark toucan";
				itemDef.description = "Picks up all resource boxes and 25% chance to double.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Toucan.png");
				break;
			case 30113:
				itemDef.setDefaults();
				itemDef.name = "Dark penguin King";
				itemDef.description = "Picks up all coin bags and 25% chance to double.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Penguin_King.png");
				break;
			case 30114:
				itemDef.setDefaults();
				itemDef.name = "Dark k'klik";
				itemDef.description = "An extra 10% in drop rate boost.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_K'klik.png");
				break;
			case 30115:
				itemDef.setDefaults();
				itemDef.name = "Dark shadow warrior";
				itemDef.description = "Gives constant +10% strength bonus in pvm.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Shadow_warrior.png");
				break;
			case 30116:
				itemDef.setDefaults();
				itemDef.name = "Dark shadow archer";
				itemDef.description = "Gives constant +10% range str bonus in PvM.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Shadow_archer.png");
				break;
			case 30117:
				itemDef.setDefaults();
				itemDef.name = "Dark shadow wizard";
				itemDef.description = "Gives constant +10% mage str bonus in PvM.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Shadow_wizard.png");
				break;
			case 30118:
				itemDef.setDefaults();
				itemDef.name = "Dark healer death spawn";
				itemDef.description = "10% chance hit restores HP.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Healer_Death_Spawn.png");
				break;
			case 30119:
				itemDef.setDefaults();
				itemDef.name = "Dark holy death spawn";
				itemDef.description = "10% chance 1/2 of your hit is restored into prayer.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Holy_Death_Spawn.png");
				break;
			case 30120:
				itemDef.setDefaults();
				itemDef.name = "Dark corrupt beast";
				itemDef.description = "Extra 10% in drop rate and constant +10% strength bonus for all styles in pvm.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Corrupt_beast.png");
				break;
			case 30121:
				itemDef.setDefaults();
				itemDef.name = "Dark roc";
				itemDef.description = "An extra 20% in drop rate boost.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_Roc.png");
				break;
			case 30122:
				itemDef.setDefaults();
				itemDef.name = "@red@Dark kratos";
				itemDef.description = "The most powerful pet, see ::foepets for full list of perks.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_yama.png");
				break;
			case 30123:
				itemDef.setDefaults();
				itemDef.name = "Dark seren";
				itemDef.description = "85% chance for Wildy Event Boss to hit a 0 and 25% chance to double key.";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = false;
				itemDef.createCustomSprite("dark_seren.png");

				break;
			case 23939:
				itemDef.name = "Seren";
				itemDef.description = "50% chance for wildy event bosses to hit a 0 on you.";
				itemDef.createCustomSprite("seren.png");
				break;
			case 21046:
				itemDef.name = "@cya@Chest rate bonus (+15%)";
				itemDef.description = "A single use +15% chance from chests, or to receive a rare raids key.";
				itemDef.stackable = true;
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				break;
			case 11666:
				itemDef.name = "Full Elite Void Token";
				itemDef.description = "Use this token to receive a full elite void set with all combat pieces.";
				itemDef.itemActions = new String[] { "Activate", null, null, null, "Drop" };
				break;
			case 1004:
				itemDef.name = "@gre@20m Coins";
				itemDef.description = "Lovely coins.";
				itemDef.stackable = false;
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 7629:
				itemDef.name = "@or3@2x Slayer point scroll";
				itemDef.itemActions = new String[] { null, null, null, null, "Drop" };
				itemDef.stackable = true;
				break;
			case 24460:
				itemDef.name = "@or3@Faster clues (30 mins)";
				itemDef.description = "Clue rates are halved for npcs and skilling.";
				itemDef.itemActions = new String[] { "Boost", null, null, null, "Drop" };
				itemDef.stackable = true;
				break;
			case 7968:
				itemDef.name = "@or3@+25% Skilling pet rate (30 mins)";
				itemDef.itemActions = new String[] { "Boost", null, null, null, "Drop" };
				itemDef.stackable = true;
				break;
			case 8899:
				itemDef.name = "@gre@50m Coins";
				itemDef.description = "Lovely coins.";
				itemDef.stackable = false;
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 4035:
				itemDef.itemActions = new String[] { "Teleport", null, null, null, null };
				break;
			case 10835:
				itemDef.name = "Buldging coin bag";
				itemDef.itemActions = new String[] { "Open", null, "Open-All", null, "Drop" };
				itemDef.description = "I can see some coins inside.";
				break;
			case 15098:
				itemDef.name = "Dice (up to 100)";
				itemDef.description = "A 100-sided dice.";
				itemDef.modelId = 31223;
				itemDef.spriteScale = 1104;
				itemDef.spriteCameraRoll = 215;
				itemDef.spritePitch = 94;
				itemDef.spriteTranslateY = -5;
				itemDef.spriteTranslateX = -18;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Public-roll";
				itemDef.itemActions[2] = null;
				itemDef.name = "Dice (up to 100)";
				itemDef.ambient = 15;
				itemDef.contrast = 25;
				itemDef.createCustomSprite("Dice_Bag.png");
				break;
			case 11773:
			case 11771:
			case 11770:
			case 11772:
				itemDef.ambient += 45;
				break;
			case 12792:
				itemDef.name = "Graceful Recolor Box";
				itemDef.itemActions = new String[] { null, "Use", null, null, "Drop" };
				break;
			case 6769:
				itemDef.name = "@yel@$5 Scroll";
				itemDef.description = "Claim this scroll to be rewarded with 5 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 2403:
				itemDef.name = "@yel@$10 Scroll";
				itemDef.description = "Claim this scroll to be rewarded with 10 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 2396:
				itemDef.name = "@yel@$25 Scroll";
				itemDef.description = "Claim this scroll to be rewarded with 25 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 786:
				itemDef.name = "@yel@$50 Donator";
				itemDef.description = "Claim this scroll to be rewarded with 50 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 761:
				itemDef.name = "@yel@$100 Donator";
				itemDef.description = "Claim this scroll to be rewarded with 100 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 607:
				itemDef.name = "@red@$250 Scroll";
				itemDef.description = "Claim this scroll to be rewarded with 250 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 608:
				itemDef.name = "@gre@$500 Scroll";
				itemDef.description = "Claim this scroll to be rewarded with 500 donator points.";
				itemDef.itemActions = new String[] { "Claim", null, null, null, "Drop" };
				break;
			case 1464:
				itemDef.name = "Vote ticket";
				itemDef.description = "Exchange this for a Vote Point.";
				break;

			case 33049:
				itemDef.setDefaults();
				itemDef.name = "Agility master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 677, 801, 43540, 43543, 43546, 43549, 43550, 43552, 43554, 43558,
						43560, 43575 };
				itemDef.modelId = 70030;
				itemDef.primaryMaleModel = 70031;
				itemDef.primaryFemaleModel = 70031;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33033:
				itemDef.setDefaults();
				itemDef.name = "Attack master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 7104, 9151, 911, 914, 917, 920, 921, 923, 925, 929, 931, 946 };
				itemDef.modelId = 70032;
				itemDef.primaryMaleModel = 70033;
				itemDef.primaryFemaleModel = 70033;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33055:
				itemDef.setDefaults();
				itemDef.name = "Construction master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 6061, 5945, 6327, 6330, 6333, 6336, 6337, 6339, 6341, 6345, 6347,
						6362 };
				itemDef.modelId = 70034;
				itemDef.primaryMaleModel = 70035;
				itemDef.primaryFemaleModel = 70035;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33040:
				itemDef.setDefaults();
				itemDef.name = "Cooking master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 920, 920, 51856, 51859, 51862, 51865, 51866, 51868, 51870, 51874,
						51876, 51891 };
				itemDef.modelId = 70036;
				itemDef.primaryMaleModel = 70037;
				itemDef.primaryFemaleModel = 70037;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33045:
				itemDef.setDefaults();
				itemDef.name = "Crafting master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 9142, 9152, 4511, 4514, 4517, 4520, 4521, 4523, 4525, 4529, 4531,
						4546 };
				itemDef.modelId = 70038;
				itemDef.primaryMaleModel = 70039;
				itemDef.primaryFemaleModel = 70039;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33034:
				itemDef.setDefaults();
				itemDef.name = "Defence master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 10460, 10473, 41410, 41413, 41416, 41419, 41420, 41422, 41424,
						41428, 41430, 41445 };
				itemDef.modelId = 70040;
				itemDef.primaryMaleModel = 70041;
				itemDef.primaryFemaleModel = 70041;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33052:
				itemDef.setDefaults();
				itemDef.name = "Farming master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 14775, 14792, 22026, 22029, 22032, 22035, 22036, 22038, 22040,
						22044, 22046, 22061 };
				itemDef.modelId = 70042;
				itemDef.primaryMaleModel = 70043;
				itemDef.primaryFemaleModel = 70043;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33044:
				itemDef.setDefaults();
				itemDef.name = "Firemaking master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 8125, 9152, 4015, 4018, 4021, 4024, 4025, 4027, 4029, 4033, 4035,
						4050 };
				itemDef.modelId = 70044;
				itemDef.primaryMaleModel = 70045;
				itemDef.primaryFemaleModel = 70045;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33043:
				itemDef.setDefaults();
				itemDef.name = "Fishing master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 9144, 9152, 38202, 38205, 38208, 38211, 38212, 38214, 38216,
						38220, 38222, 38237 };
				itemDef.modelId = 70046;
				itemDef.primaryMaleModel = 70047;
				itemDef.primaryFemaleModel = 70047;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33042:
				itemDef.setDefaults();
				itemDef.name = "Fletching master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 6067, 9152, 33670, 33673, 33676, 33679, 33680, 33682, 33684,
						33688, 33690, 33705 };
				itemDef.modelId = 70048;
				itemDef.primaryMaleModel = 70049;
				itemDef.primaryFemaleModel = 70049;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33048:
				itemDef.setDefaults();
				itemDef.name = "Herblore master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 9145, 9156, 22414, 22417, 22420, 22423, 22424, 22426, 22428,
						22432, 22434, 22449 };
				itemDef.modelId = 70050;
				itemDef.primaryMaleModel = 70051;
				itemDef.primaryFemaleModel = 70051;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33036:
				itemDef.setDefaults();
				itemDef.name = "Hitpoints master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 818, 951, 8291, 8294, 8297, 8300, 8301, 8303, 8305, 8309, 8311,
						8319 };
				itemDef.modelId = 70052;
				itemDef.primaryMaleModel = 70053;
				itemDef.primaryFemaleModel = 70053;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				//itemDef.femaleOffset = 4;
				break;
			case 33054:
				itemDef.setDefaults();
				itemDef.name = "Hunter master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 5262, 6020, 8472, 8475, 8478, 8481, 8482, 8484, 8486, 8490, 8492,
						8507 };
				itemDef.modelId = 70054;
				itemDef.primaryMaleModel = 70055;
				itemDef.primaryFemaleModel = 70055;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33039:
				itemDef.setDefaults();
				itemDef.name = "Magic master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 43569, 43685, 6336, 6339, 6342, 6345, 6346, 6348, 6350, 6354,
						6356, 6371 };
				itemDef.modelId = 70056;
				itemDef.primaryMaleModel = 70057;
				itemDef.primaryFemaleModel = 70057;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33047:
				itemDef.setDefaults();
				itemDef.name = "Mining master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 36296, 36279, 10386, 10389, 10392, 10395, 10396, 10398, 10400,
						10404, 10406, 10421 };
				itemDef.modelId = 70058;
				itemDef.primaryMaleModel = 70059;
				itemDef.primaryFemaleModel = 70059;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33038:
				itemDef.setDefaults();
				itemDef.name = "Prayer master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 9163, 9168, 117, 120, 123, 126, 127, 127, 127, 127, 127, 127 };
				itemDef.modelId = 70060;
				itemDef.primaryMaleModel = 70061;
				itemDef.primaryFemaleModel = 70061;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33037:
				itemDef.setDefaults();
				itemDef.name = "Range master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 3755, 3998, 15122, 15125, 15128, 15131, 15132, 15134, 15136,
						15140, 15142, 15157 };
				itemDef.modelId = 70062;
				itemDef.primaryMaleModel = 70063;
				itemDef.primaryFemaleModel = 70063;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33053:
				itemDef.setDefaults();
				itemDef.name = "Runecrafting master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				// 4 //7 //10 //13 //14//16//18//22 //24//39
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 9152, 8128, 10318, 10321, 10324, 10327, 10328, 10330, 10332,
						10336, 10338, 10353 };
				itemDef.modelId = 70064;
				itemDef.primaryMaleModel = 70065;
				itemDef.primaryFemaleModel = 70065;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33051:
				itemDef.setDefaults();
				itemDef.name = "Slayer master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				itemDef.originalModelColors = new int[] { 57022, 48811 };
				itemDef.modifiedModelColors = new int[] { 912, 920 };
				itemDef.modelId = 70066;
				itemDef.primaryMaleModel = 70067;
				itemDef.primaryFemaleModel = 70067;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33046:
				itemDef.setDefaults();
				itemDef.name = "Smithing master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 8115, 9148, 10386, 10389, 10392, 10395, 10396, 10398, 10400,
						10404, 10406, 10421 };
				itemDef.modelId = 70068;
				itemDef.primaryMaleModel = 70069;
				itemDef.primaryFemaleModel = 70069;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33035:
				itemDef.setDefaults();
				itemDef.name = "Strength master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 935, 931, 27538, 27541, 27544, 27547, 27548, 27550, 27552, 27556,
						27558, 27573 };
				itemDef.modelId = 70070;
				itemDef.primaryMaleModel = 70071;
				itemDef.primaryFemaleModel = 70071;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33050:
				itemDef.setDefaults();
				itemDef.name = "Thieving master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 11, 0, 58779, 58782, 58785, 58788, 58789, 57891, 58793, 58797,
						58799, 58814 };
				itemDef.modelId = 70072;
				itemDef.primaryMaleModel = 70073;
				itemDef.primaryFemaleModel = 70073;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = 5;
				break;
			case 33041:
				itemDef.setDefaults();
				itemDef.name = "Woodcutting master cape";
				itemDef.description = "	A cape worn by those who've overachieved.";
				itemDef.originalModelColors = new int[] { 57022, 48811, 2, 1029, 1032, 11, 12, 14, 16, 20, 22, 2 };
				itemDef.modifiedModelColors = new int[] { 25109, 24088, 6693, 6696, 6699, 6702, 6703, 6705, 6707, 6711,
						6713, 6728 };
				itemDef.modelId = 70074;
				itemDef.primaryMaleModel = 70075;
				itemDef.primaryFemaleModel = 70075;
				itemDef.spriteScale = 2300;
				itemDef.spritePitch = 400;
				itemDef.spriteCameraRoll = 1020;
				itemDef.spriteTranslateX = 3;
				itemDef.spriteTranslateY = 30;
				itemDef.itemActions = new String[5];
				itemDef.itemActions[1] = "Wear";
				itemDef.itemActions[2] = null;
				//itemDef.maleOffset = -2;
				break;
		}
	}

	void method2789(ItemDefinition var1, ItemDefinition var2) {
		modelId = var1.modelId * 1;
		spriteScale = var1.spriteScale * 1;
		spritePitch = 1 * var1.spritePitch;
		spriteCameraRoll = 1 * var1.spriteCameraRoll;
		spriteCameraYaw = 1 * var1.spriteCameraYaw;
		spriteTranslateX = 1 * var1.spriteTranslateX;
		spriteTranslateY = var1.spriteTranslateY * 1;
		originalModelColors = var2.originalModelColors;
		modifiedModelColors = var2.modifiedModelColors;
		 originalTextureColors = var2.originalTextureColors;
		 modifiedTextureColors = var2.modifiedTextureColors;
		name = var2.name;
		membersObject = var2.membersObject;
		stackable = var2.stackable;
		primaryMaleModel = 1 * var2.primaryMaleModel;
		secondaryMaleModel = 1 * var2.secondaryMaleModel;
		tertiaryMaleEquipmentModel = 1 * var2.tertiaryMaleEquipmentModel;
		primaryFemaleModel = var2.primaryFemaleModel * 1;
		secondaryFemaleModel = var2.secondaryFemaleModel * 1;
		tertiaryFemaleEquipmentModel = 1 * var2.tertiaryFemaleEquipmentModel;
		primaryMaleHeadPiece = 1 * var2.primaryMaleHeadPiece;
		secondaryMaleHeadPiece = var2.secondaryMaleHeadPiece * 1;
		primaryFemaleHeadPiece = var2.primaryFemaleHeadPiece * 1;
		secondaryFemaleHeadPiece = var2.secondaryFemaleHeadPiece * 1;
		team = var2.team * 1;
		groundActions = var2.groundActions;
		itemActions = new String[5];
		equipActions = new String[5];
		if (null != var2.itemActions) {
			for (int var4 = 0; var4 < 4; ++var4) {
				itemActions[var4] = var2.itemActions[var4];
			}
		}

		itemActions[4] = "Discard";
		value = 0;
	}

	void method2790(ItemDefinition var1, ItemDefinition var2) {
		modelId = var1.modelId * 1;
		spriteScale = 1 * var1.spriteScale;
		spritePitch = var1.spritePitch * 1;
		spriteCameraRoll = var1.spriteCameraRoll * 1;
		spriteCameraYaw = var1.spriteCameraYaw * 1;
		spriteTranslateX = 1 * var1.spriteTranslateX;
		spriteTranslateY = var1.spriteTranslateY * 1;
		originalModelColors = var1.originalModelColors;
		modifiedModelColors = var1.modifiedModelColors;
		originalTextureColors = var1.originalTextureColors;
		modifiedTextureColors = var1.modifiedTextureColors;
		stackable = var1.stackable;
		name = var2.name;
		value = 0;
	}

	/*
	 * private void readValues(Stream stream) { while(true) { int opcode =
	 * stream.readUnsignedByte(); if(opcode == 0) return; if(opcode == 1) modelId =
	 * stream.readUnsignedWord(); else if(opcode == 2) name = stream.readString();
	 * else if(opcode == 3) description = stream.readString(); else if(opcode == 4)
	 * modelZoom = stream.readUnsignedWord(); else if(opcode == 5) modelRotation1 =
	 * stream.readUnsignedWord(); else if(opcode == 6) modelRotation2 =
	 * stream.readUnsignedWord(); else if(opcode == 7) { modelOffset1 =
	 * stream.readUnsignedWord(); if(modelOffset1 > 32767) modelOffset1 -= 0x10000;
	 * } else if(opcode == 8) { modelOffset2 = stream.readUnsignedWord();
	 * if(modelOffset2 > 32767) modelOffset2 -= 0x10000; } else if(opcode == 11)
	 * stackable = true; else if(opcode == 12) value = stream.readDWord(); else
	 * if(opcode == 16) membersObject = true; else if(opcode == 23) { maleModel =
	 * stream.readUnsignedWord(); aByte205 = stream.readSignedByte(); } else if
	 * (opcode == 24) anInt188 = stream.readUnsignedWord(); else if (opcode == 25) {
	 * femaleModel = stream.readUnsignedWord(); aByte154 = stream.readSignedByte();
	 * } else if (opcode == 26) anInt164 = stream.readUnsignedWord(); else if(opcode
	 * >= 30 && opcode < 35) { if(groundOptions == null) groundOptions = new
	 * String[5]; groundOptions[opcode - 30] = stream.readString();
	 * if(groundOptions[opcode - 30].equalsIgnoreCase("hidden"))
	 * groundOptions[opcode - 30] = null; } else if(opcode >= 35 && opcode < 40) {
	 * if(inventoryOptions == null) inventoryOptions = new String[5];
	 * inventoryOptions[opcode - 35] = stream.readString(); } else if(opcode == 40)
	 * { int size = stream.readUnsignedByte(); originalModelColors = new int[size];
	 * modifiedModelColors = new int[size]; for(int index = 0; index < size;
	 * index++) { originalModelColors[index] = stream.readUnsignedWord();
	 * modifiedModelColors[index] = stream.readUnsignedWord(); } } else if(opcode ==
	 * 41) { int size = stream.readUnsignedByte(); originalTextureColors = new
	 * short[size]; modifiedTextureColors = new short[size]; for(int index = 0;
	 * index < size; index++) { originalTextureColors[index] = (short)
	 * stream.readUnsignedWord(); modifiedTextureColors[index] = (short)
	 * stream.readUnsignedWord(); } } else if(opcode == 65) { searchableItem = true;
	 * } else if(opcode == 78) anInt185 = stream.readUnsignedWord(); else if(opcode
	 * == 79) anInt162 = stream.readUnsignedWord(); else if(opcode == 90) anInt175 =
	 * stream.readUnsignedWord(); else if(opcode == 91) anInt197 =
	 * stream.readUnsignedWord(); else if(opcode == 92) anInt166 =
	 * stream.readUnsignedWord(); else if(opcode == 93) anInt173 =
	 * stream.readUnsignedWord(); else if(opcode == 95) anInt204 =
	 * stream.readUnsignedWord(); else if(opcode == 97) certID =
	 * stream.readUnsignedWord(); else if(opcode == 98) certTemplateID =
	 * stream.readUnsignedWord(); else if (opcode >= 100 && opcode < 110) { if
	 * (stackIDs == null) { stackIDs = new int[10]; stackAmounts = new int[10]; }
	 * stackIDs[opcode - 100] = stream.readUnsignedWord(); stackAmounts[opcode -
	 * 100] = stream.readUnsignedWord(); } else if(opcode == 110) anInt167 =
	 * stream.readUnsignedWord(); else if(opcode == 111) anInt192 =
	 * stream.readUnsignedWord(); else if(opcode == 112) anInt191 =
	 * stream.readUnsignedWord(); else if(opcode == 113) anInt196 =
	 * stream.readSignedByte(); else if(opcode == 114) anInt184 =
	 * stream.readSignedByte() * 5; else if(opcode == 115) team =
	 * stream.readUnsignedByte(); else if (opcode == 139) opcode139 =
	 * stream.readUnsignedWord(); else if (opcode == 140) opcode140 =
	 * stream.readUnsignedWord(); else if (opcode == 148) opcode148 =
	 * stream.readUnsignedWord(); else if (opcode == 149) opcode149 =
	 * stream.readUnsignedWord(); else { System.out.println("Error loading item " +
	 * id + ", opcode " + opcode); } } }
	 */

	private void readValues(Buffer stream) {
		while (true) {
			int opcode = stream.readUnsignedByte();
			if (opcode == 0)
				return;
			if (opcode == 1)
				modelId = stream.readUShort();
			else if (opcode == 2)
				name = stream.readString();
			else if (opcode == 3)
				description = stream.readString();
			else if (opcode == 4)
				spriteScale = stream.readUShort();
			else if (opcode == 5)
				spritePitch = stream.readUShort();
			else if (opcode == 6)
				spriteCameraRoll = stream.readUShort();
			else if (opcode == 7) {
				spriteTranslateX = stream.readUShort();
				if (spriteTranslateX > 32767)
					spriteTranslateX -= 0x10000;
			} else if (opcode == 8) {
				spriteTranslateY = stream.readUShort();
				if (spriteTranslateY > 32767)
					spriteTranslateY -= 0x10000;
			} else if(opcode == 9){
				stream.readString();
			} else if (opcode == 11)
				stackable = true;
			else if (opcode == 12)
				value = stream.readDWord();
		 else if(opcode == 13)
			this.field2142 = stream.readSignedByte();
		 else if(opcode == 14)
			this.field2157 = stream.readSignedByte();
			else if (opcode == 16)
				membersObject = true;
			else if (opcode == 23) {
				primaryMaleModel = stream.readUShort();
				maleTranslation = stream.readSignedByte();
				if (primaryMaleModel == 65535)
					primaryMaleModel = -1;
			} else if (opcode == 24)
				secondaryMaleModel = stream.readUShort();
			else if (opcode == 25) {
				primaryFemaleModel = stream.readUShort();
				femaleTranslation = stream.readSignedByte();
				if (primaryFemaleModel == 65535)
					primaryFemaleModel = -1;
			} else if (opcode == 26)
				secondaryFemaleModel = stream.readUShort();
		 else if(opcode == 27)
			this.field2158 = stream.readSignedByte();
			else if (opcode >= 30 && opcode < 35) {
				if (groundActions == null)
					groundActions = new String[5];
				groundActions[opcode - 30] = stream.readString();
				if (groundActions[opcode - 30].equalsIgnoreCase("hidden"))
					groundActions[opcode - 30] = null;
			} else if (opcode >= 35 && opcode < 40) {
				if (itemActions == null)
					itemActions = new String[5];
				itemActions[opcode - 35] = stream.readString();
			} else if (opcode == 40) {
				int size = stream.readUnsignedByte();
				originalModelColors = new int[size];
				modifiedModelColors = new int[size];
				for (int index = 0; index < size; index++) {
					originalModelColors[index] = stream.readUShort();
					modifiedModelColors[index] = stream.readUShort();
				}
			} else if (opcode == 41) {
				int size = stream.readUnsignedByte();
				originalTextureColors = new short[size];
				modifiedTextureColors = new short[size];
				for (int index = 0; index < size; index++) {
					originalTextureColors[index] = (short) stream.readUShort();
					modifiedTextureColors[index] = (short) stream.readUShort();
				}
			}else if (opcode == 42) {
					stream.readSignedByte();
			} else if (opcode == 43) {
				int var3 = stream.readUnsignedByte();
				if (this.subOps == null) {
					this.subOps = new String[5][];
				}

				boolean var7 = var3 >= 0 && var3 < 5;
				if (var7 && this.subOps[var3] == null) {
					this.subOps[var3] = new String[20];
				}

				while (true) {
					int var5 = stream.readUnsignedByte() - 1;
					if (var5 == -1) {
						break;
					}

					String var6 = stream.readString();
					if (var7 && var5 >= 0 && var5 < 20) {
						this.subOps[var3][var5] = var6;
					}
				}
			} else if (opcode == 65) {
				searchable = true;
			} else if (opcode == 75){
				this.field2182 = stream.readSignedWord();
			} else if (opcode == 78)
				tertiaryMaleEquipmentModel = stream.readUShort();
			else if (opcode == 79)
				tertiaryFemaleEquipmentModel = stream.readUShort();
			else if (opcode == 90)
				primaryMaleHeadPiece = stream.readUShort();
			else if (opcode == 91)
				primaryFemaleHeadPiece = stream.readUShort();
			else if (opcode == 92)
				secondaryMaleHeadPiece = stream.readUShort();
			else if (opcode == 93)
				secondaryFemaleHeadPiece = stream.readUShort();
			else if (opcode == 94)
				stream.readUShort();
			else if (opcode == 95)
				spriteCameraYaw = stream.readUShort();
			else if (opcode == 97)
				certID = stream.readUShort();
			else if (opcode == 98)
				certTemplateID = stream.readUShort();
			else if (opcode >= 100 && opcode < 110) {
				if (stackIDs == null) {
					stackIDs = new int[10];
					stackAmounts = new int[10];
				}
				stackIDs[opcode - 100] = stream.readUShort();
				stackAmounts[opcode - 100] = stream.readUShort();
			} else if (opcode == 110)
				groundScaleX = stream.readUShort();
			else if (opcode == 111)
				groundScaleY = stream.readUShort();
			else if (opcode == 112)
				groundScaleZ = stream.readUShort();
			else if (opcode == 113)
				ambient = stream.readSignedByte();
			else if (opcode == 114)
				contrast = stream.readSignedByte() * 5;
			else if (opcode == 115)
				team = stream.readUnsignedByte();
			else if (opcode == 139)
				unnotedId = stream.readUShort();
			else if (opcode == 140)
				notedId = stream.readUShort();
			else if (opcode == 148)
				placeholderId = stream.readUShort();
			else if (opcode == 149)
				placeholderTemplateId = stream.readUShort();
			else {
				// System.out.println("Error loading item " + id + ", opcode " + opcode);
			}
		}
	}

	public int unnotedId, notedId, placeholderId, placeholderTemplateId;

	public static void nullLoader() {
		mruNodes2 = null;
		mruNodes1 = null;
		streamIndices = null;
		cache = null;
		stream = null;
	}

	public boolean method192(int j) {
		int k = primaryMaleHeadPiece;
		int l = secondaryMaleHeadPiece;
		if (j == 1) {
			k = primaryFemaleHeadPiece;
			l = secondaryFemaleHeadPiece;
		}
		if (k == -1)
			return true;
		boolean flag = true;
		if (!Model.isCached(k))
			flag = false;
		if (l != -1 && !Model.isCached(l))
			flag = false;
		return flag;
	}

	public Model method194(int j) {
		int k = primaryMaleHeadPiece;
		int l = secondaryMaleHeadPiece;
		if (j == 1) {
			k = primaryFemaleHeadPiece;
			l = secondaryFemaleHeadPiece;
		}
		if (k == -1)
			return null;
		Model model = Model.getModel(k);
		if (l != -1) {
			Model model_1 = Model.getModel(l);
			Model aclass30_sub2_sub4_sub6s[] = { model, model_1 };
			model = new Model(2, aclass30_sub2_sub4_sub6s);
		}
		if (originalModelColors != null) {
			for (int i1 = 0; i1 < originalModelColors.length; i1++)
				model.recolor(originalModelColors[i1], modifiedModelColors[i1]);

		}
		if (originalTextureColors != null) {
			for (int k2 = 0; k2 < originalTextureColors.length; k2++)
				model.retexture(originalTextureColors[k2], modifiedTextureColors[k2]);

		}
		return model;
	}

	public boolean method195(int j) {
		int k = primaryMaleModel;
		int l = secondaryMaleModel;
		int i1 = tertiaryMaleEquipmentModel;
		if (j == 1) {
			k = primaryFemaleModel;
			l = secondaryFemaleModel;
			i1 = tertiaryFemaleEquipmentModel;
		}
		if (k == -1)
			return true;
		boolean flag = true;
		if (!Model.isCached(k))
			flag = false;
		if (l != -1 && !Model.isCached(l))
			flag = false;
		if (i1 != -1 && !Model.isCached(i1))
			flag = false;
		return flag;
	}

	public Model method196(int i) {
		int j = primaryMaleModel;
		int k = secondaryMaleModel;
		int l = tertiaryMaleEquipmentModel;
		if (i == 1) {
			j = primaryFemaleModel;
			k = secondaryFemaleModel;
			l = tertiaryFemaleEquipmentModel;
		}
		if (j == -1)
			return null;
		Model model = Model.getModel(j);
		if (k != -1)
			if (l != -1) {
				Model model_1 = Model.getModel(k);
				Model model_3 = Model.getModel(l);
				Model aclass30_sub2_sub4_sub6_1s[] = { model, model_1, model_3 };
				model = new Model(3, aclass30_sub2_sub4_sub6_1s);
			} else {
				Model model_2 = Model.getModel(k);
				Model aclass30_sub2_sub4_sub6s[] = { model, model_2 };
				model = new Model(2, aclass30_sub2_sub4_sub6s);
			}
		if (i == 0 && maleTranslation != 0)
			model.offsetBy(0, maleTranslation, 0);
		if (i == 1 && femaleTranslation != 0)
			model.offsetBy(0, femaleTranslation, 0);
		if (originalModelColors != null) {
			for (int i1 = 0; i1 < originalModelColors.length; i1++)
				model.recolor(originalModelColors[i1], modifiedModelColors[i1]);

		}
		if (originalTextureColors != null) {
			for (int k2 = 0; k2 < originalTextureColors.length; k2++)
				model.retexture(originalTextureColors[k2], modifiedTextureColors[k2]);

		}
		return model;
	}

	public void setDefaults() {
		// equipActions = new String[6];
		customSpriteLocation = null;
		customSmallSpriteLocation = null;
		equipActions = new String[] { "Remove", null, "Operate", null, null };
		modelId = 0;
		name = null;
		description = null;
		modifiedModelColors = null;
		originalModelColors = null;
		modifiedTextureColors = null;
		originalTextureColors = null;
		spriteScale = 2000;
		spritePitch = 0;
		spriteCameraRoll = 0;
		spriteCameraYaw = 0;
		spriteTranslateX = 0;
		spriteTranslateY = 0;
		stackable = false;
		value = 1;
		membersObject = false;
		groundActions = null;
		itemActions = null;
		primaryMaleModel = -1;
		secondaryMaleModel = -1;
		maleTranslation = 0;
		primaryFemaleModel = -1;
		secondaryFemaleModel = -1;
		femaleTranslation = 0;
		tertiaryMaleEquipmentModel = -1;
		tertiaryFemaleEquipmentModel = -1;
		primaryMaleHeadPiece = -1;
		secondaryMaleHeadPiece = -1;
		primaryFemaleHeadPiece = -1;
		secondaryFemaleHeadPiece = -1;
		stackIDs = null;
		stackAmounts = null;
		certID = -1;
		certTemplateID = -1;
		groundScaleX = 128;
		groundScaleY = 128;
		groundScaleZ = 128;
		ambient = 0;
		contrast = 0;
		team = 0;

		notedId = -1;
		unnotedId = -1;
		placeholderId = -1;
		placeholderTemplateId = -1;

		searchable = false;
	}

	public static void dumpBonuses() {
		int[] bonuses = new int[14];
		int bonus = 0;
		int amount = 0;
		for (int i = 21304; i < totalItems; i++) {
			ItemDefinition item = ItemDefinition.forID(i);
			URL url;
			try {
				try {
					try {
						url = new URL("http://2007.runescape.wikia.com/wiki/" + item.name.replaceAll(" ", "_"));
						URLConnection con = url.openConnection();
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
						String line;
						BufferedWriter writer = new BufferedWriter(new FileWriter("item.cfg", true));
						while ((line = in.readLine()) != null) {
							try {
								if (line.contains("<td style=\"text-align: center; width: 35px;\">")) {
									line = line.replace("</td>", "").replace("%", "").replace("?", "")
											.replace("\"\"", "")
											.replace("<td style=\"text-align: center; width: 35px;\">", "");
									bonuses[bonus] = Integer.parseInt(line);
									bonus++;
								} else if (line.contains("<td style=\"text-align: center; width: 30px;\">")) {
									line = line.replace("</td>", "").replace("%", "").replace("?", "").replace("%", "")
											.replace("<td style=\"text-align: center; width: 30px;\">", "");
									bonuses[bonus] = Integer.parseInt(line);
									bonus++;
								}
							} catch (NumberFormatException e) {

							}
							if (bonus >= 13)
								bonus = 0;
							// in.close();
						}
						in.close();
						writer.write("item	=	" + i + "	" + item.name.replace(" ", "_") + "	"
								+ item.description.replace(" ", "_") + "	" + item.value + "	" + item.value + "	"
								+ item.value + "	" + bonuses[0] + "	" + bonuses[1] + "	" + bonuses[2] + "	"
								+ bonuses[3] + "	" + bonuses[4] + "	" + bonuses[5] + "	" + bonuses[6] + "	"
								+ bonuses[7] + "	" + bonuses[8] + "	" + bonuses[9] + "	" + bonuses[10] + "	"
								+ bonuses[13]);
						bonuses[0] = bonuses[1] = bonuses[2] = bonuses[3] = bonuses[4] = bonuses[5] = bonuses[6] = bonuses[7] = bonuses[8] = bonuses[9] = bonuses[10] = bonuses[13] = 0;
						writer.newLine();
						amount++;
						writer.close();
					} catch (NullPointerException e) {

					}
				} catch (FileNotFoundException e) {

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Done dumping " + amount + " item bonuses!");
	}

	public static void dumpBonus() {
		final int[] wikiBonuses = new int[18];
		int bonus = 0;
		int amount = 0;
		System.out.println("Starting to dump item bonuses...");
		for (int i = 20000; i < totalItems; i++) {
			ItemDefinition item = ItemDefinition.forID(i);
			try {
				try {
					try {
						final URL url = new URL(
								"http://2007.runescape.wikia.com/wiki/" + item.name.replaceAll(" ", "_"));
						URLConnection con = url.openConnection();
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
						String line;
						writer = new BufferedWriter(new FileWriter("item.cfg", true));
						while ((line = in.readLine()) != null) {
							try {
								if (line.contains("<td style=\"text-align: center; width: 35px;\">")) {
									line = line.replace("</td>", "").replace("%", "").replace("?", "")
											.replace("\"\"", "")
											.replace("<td style=\"text-align: center; width: 35px;\">", "");
									wikiBonuses[bonus] = Integer.parseInt(line);
									bonus++;
								} else if (line.contains("<td style=\"text-align: center; width: 30px;\">")) {
									line = line.replace("</td>", "").replace("%", "").replace("?", "").replace("%", "")
											.replace("<td style=\"text-align: center; width: 30px;\">", "");
									wikiBonuses[bonus] = Integer.parseInt(line);
									bonus++;
								}
							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
							in.close();
							writer.write("item = " + i + "	" + item.name.replace(" ", "_") + "	"
									+ item.description.replace(" ", "_") + "	" + item.value + "	" + item.value
									+ "	" + item.value + "	" + wikiBonuses[0] + "	" + wikiBonuses[1] + "	"
									+ wikiBonuses[2] + "	" + wikiBonuses[3] + "	" + wikiBonuses[4] + "	"
									+ wikiBonuses[5] + "	" + wikiBonuses[6] + "	" + wikiBonuses[7] + "	"
									+ wikiBonuses[8] + "	" + wikiBonuses[9] + "	" + wikiBonuses[10] + "	"
									+ wikiBonuses[13]);
							amount++;
							wikiBonuses[0] = wikiBonuses[1] = wikiBonuses[2] = wikiBonuses[3] = wikiBonuses[4] = wikiBonuses[5] = wikiBonuses[6] = wikiBonuses[7] = wikiBonuses[8] = wikiBonuses[9] = wikiBonuses[10] = wikiBonuses[11] = wikiBonuses[13] = 0;
							writer.newLine();
							writer.close();
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Done dumping " + amount + " item bonuses!");
		}
	}

	public static void dumpItemDefs() {
		final int[] wikiBonuses = new int[18];
		int bonus = 0;
		int amount = 0;
		int value = 0;
		int slot = -1;
		// Testing Variables just so i know format is correct
		String fullmask = "false";
		// boolean stackable1 = false;
		String stackable = "false";
		//boolean noteable1 = false;
		String noteable = "true";
		// boolean tradeable1 = false;
		String tradeable = "true";
		// boolean wearable1 = false;
		String wearable = "true";
		String showBeard = "true";
		String members = "true";
		boolean twoHanded = false;
		System.out.println("Starting to dump item definitions...");
		for (int i = 21298; i < totalItems; i++) {
			ItemDefinition item = ItemDefinition.forID(i);
			try {
				try {
					try {
						final URL url = new URL(
								"https://oldschool.runescape.wiki/wiki/" + item.name.replaceAll(" ", "_"));
						URLConnection con = url.openConnection();
						BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
						String line;
						writer = new BufferedWriter(new FileWriter("itemDefs.json", true));
						while ((line = in.readLine()) != null) {
							try {
								if (line.contains("<td style=\"text-align: center; width: 35px;\">")) {
									line = line.replace("</td>", "").replace("%", "").replace("?", "")
											.replace("\"\"", "")
											.replace("<td style=\"text-align: center; width: 35px;\">", "");
									wikiBonuses[bonus] = Integer.parseInt(line);
									bonus++;
								} else if (line.contains("<td style=\"text-align: center; width: 30px;\">")) {
									line = line.replace("</td>", "").replace("%", "").replace("?", "").replace("%", "")
											.replace("<td style=\"text-align: center; width: 30px;\">", "");
									wikiBonuses[bonus] = Integer.parseInt(line);
									bonus++;
								}
								if (line.contains("<div id=\"GEPCalcResult\" style=\"display:inline;\">")) {
									line = line.replace("</div>", "").replace("%", "").replace("?", "").replace("%", "")
											.replace("<div id=\"GEPCalcResult\" style=\"display:inline;\">", "");
									value = Integer.parseInt(line);
								}

							} catch (NumberFormatException e) {
								e.printStackTrace();
							}
							in.close();
							// fw.write("ItemID: "+itemDefinition.id+" - "+itemDefinition.name);
							// fw.write(System.getProperty("line.separator"));
							// writer.write("[\n");
							writer.write("  {\n\t\"id\": " + item.id + ",\n\t\"name\": \"" + item.name
									+ "\",\n\t\"desc\": \"" + item.description.replace("_", " ") + "\",\n\t\"value\": "
									+ value + ",\n\t\"dropValue\": " + value + ",\n\t\"bonus\": [\n\t  "
									+ wikiBonuses[0] + ",\n\t  " + wikiBonuses[1] + ",\n\t  " + wikiBonuses[2]
									+ ",\n\t  " + wikiBonuses[3] + ",\n\t  " + wikiBonuses[4] + ",\n\t  "
									+ wikiBonuses[5] + ",\n\t  " + wikiBonuses[6] + ",\n\t  " + wikiBonuses[7]
									+ ",\n\t  " + wikiBonuses[8] + ",\n\t  " + wikiBonuses[9] + ",\n\t  "
									+ wikiBonuses[10] + ",\n\t  " + wikiBonuses[13] + ",\n\t],\n\t\"slot\": " + slot
									+ ",\n\t\"fullmask\": " + fullmask + ",\n\t\"stackable\": " + stackable
									+ ",\n\t\"noteable\": " + noteable + ",\n\t\"tradeable\": " + tradeable
									+ ",\n\t\"wearable\": " + wearable + ",\n\t\"showBeard\": " + showBeard
									+ ",\n\t\"members\": " + members + ",\n\t\"slot\": " + twoHanded
									+ ",\n\t\"requirements\": [\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t  0,\n\t]\n  },\n");
							/*
							 * writer.write("item = " + i + "	" + item.name.replace(" ", "_") + "	" +
							 * item.description.replace(" ", "_") + "	" + item.value + "	" + item.value +
							 * "	" + item.value + "	" + wikiBonuses[0] + "	" + wikiBonuses[1] + "	" +
							 * wikiBonuses[2] + "	" + wikiBonuses[3] + "	" + wikiBonuses[4] + "	" +
							 * wikiBonuses[5] + "	" + wikiBonuses[6] + "	" + wikiBonuses[7] + "	" +
							 * wikiBonuses[8] + "	" + wikiBonuses[9] + "	" + wikiBonuses[10] + "	" +
							 * wikiBonuses[13]);
							 */
							amount++;
							wikiBonuses[0] = wikiBonuses[1] = wikiBonuses[2] = wikiBonuses[3] = wikiBonuses[4] = wikiBonuses[5] = wikiBonuses[6] = wikiBonuses[7] = wikiBonuses[8] = wikiBonuses[9] = wikiBonuses[10] = wikiBonuses[11] = wikiBonuses[13] = 0;
							writer.newLine();
							writer.close();
						}
					} catch (NullPointerException e) {
						e.printStackTrace();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("Done dumping " + amount + " item definitions!");
		}
	}

	public static void itemDump() {
		try {
			FileWriter fw = new FileWriter("/temp/itemlist.txt");
			for (int i = totalItems - 9000; i < totalItems; i++) {
				ItemDefinition item = ItemDefinition.forID(i);
				fw.write("case " + i + ":");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.name = \"" + item.name + "\";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.modelID= " + item.modelId + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.maleModel= " + item.primaryMaleModel + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.femaleModel= " + item.primaryFemaleModel + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.modelZoom = " + item.spriteScale + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.modelRotationX = " + item.spritePitch + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.modelRotationY = " + item.spriteCameraRoll + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.modelOffset1 = " + item.spriteTranslateX + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.modelOffset2 = " + item.spriteTranslateY + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.description = \"" + item.description + "\";");
				fw.write(System.getProperty("line.separator"));

				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.value = " + item.value + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("itemDef.team = " + item.team + ";");
				fw.write(System.getProperty("line.separator"));
				fw.write("break;");
				fw.write(System.getProperty("line.separator"));
				fw.write(System.getProperty("line.separator"));
			}
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void dumpList() {
		try {
			FileWriter fw = new FileWriter("./temp/item_data.json");
			for (int i = 0; i < totalItems; i++) {
				ItemDefinition itemDefinition = ItemDefinition.forID(i);
				fw.write("id: " + itemDefinition.id + " - " + itemDefinition.name + "\n");
			}
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void dumpStackableList() {
		try {
			File file = new File("stackables.dat");

			if (!file.exists()) {
				file.createNewFile();
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				for (int i = 0; i < totalItems; i++) {
					ItemDefinition definition = forID(i);
					if (definition != null) {
						writer.write(definition.id + "\t" + definition.stackable);
						writer.newLine();
					} else {
						writer.write(i + "\tfalse");
						writer.newLine();
					}
				}
			}

			System.out.println("Finished dumping noted items definitions.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void dumpNotes() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter("./temp/item_config.txt"));
			for (int j = 0; j < streamIndices.length; j++) {
				try {
					ItemDefinition item = ItemDefinition.forID(j);
					if (item != null && item.id != 0 && (item.name == null || !item.name.equalsIgnoreCase("dwarf remains"))) {
						int noteId = 0;
						boolean noted = false;
						if (item.certTemplateID <= 0) {
							for (int k = j - 200; k < j + 200; k++) {
								if (k < 0) continue;
								ItemDefinition def2 = ItemDefinition.forID(k);
								if (def2 != null && def2.certID == j) {
									noteId = k;
									break;
								}
							}
						} else {
							noteId = item.certID;
							noted = true;
						}
						boolean stackable = (noted || item.stackable || item.stackIDs != null && item.stackIDs.length > 0 || item.stackAmounts != null && item.stackAmounts.length > 0);
						out.write(j + ":" + noted + ":" + noteId + ":" + stackable);
						out.newLine();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			out.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void dumpStackable() {
		try {
			FileOutputStream out = new FileOutputStream(new File("stackable.dat"));
			for (int j = 0; j < totalItems; j++) {
				ItemDefinition item = ItemDefinition.forID(j);
				out.write(item.stackable ? 1 : 0);
			}
			out.write(-1);
			out.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	public static void dumpNotableList() {
		try {
			File file = new File("note_id.dat");

			if (!file.exists()) {
				file.createNewFile();
			}

			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
				for (int i = 0; i < totalItems; i++) {
					ItemDefinition definition = ItemDefinition.forID(i);
					if (definition != null) {
						if (definition.certTemplateID == -1 && definition.certID != -1) {
							writer.write(definition.id + "\t" + definition.certID);
							writer.newLine();
						}
					} else {
						writer.write(i + "\t-1");
						writer.newLine();
					}
				}
			}

			System.out.println("Finished dumping noted items definitions.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void toNote() {
		ItemDefinition itemDef = forID(certTemplateID);
		modelId = itemDef.modelId;
		spriteScale = itemDef.spriteScale;
		spritePitch = itemDef.spritePitch;
		spriteCameraRoll = itemDef.spriteCameraRoll;

		spriteCameraYaw = itemDef.spriteCameraYaw;
		spriteTranslateX = itemDef.spriteTranslateX;
		spriteTranslateY = itemDef.spriteTranslateY;
		modifiedModelColors = itemDef.modifiedModelColors;
		originalModelColors = itemDef.originalModelColors;
		ItemDefinition itemDef_1 = forID(certID);
		name = itemDef_1.name;
		membersObject = itemDef_1.membersObject;
		value = itemDef_1.value;
		String s = "a";
		if (itemDef_1.name != null) {
			char c = itemDef_1.name.charAt(0);
			if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U')
				s = "an";
		}
		description = ("Swap this note at any bank for " + s + " " + itemDef_1.name + ".");
		stackable = true;
	}

	public static Sprite getSmallSprite(int itemId) {
		return getSmallSprite(itemId, 1);
	}

	public static Sprite getSmallSprite(int itemId, int itemAmount) {
		ItemDefinition itemDef = forID(itemId);
		if (itemDef.stackIDs == null)
			itemAmount = -1;
		if (itemAmount > 1) {
			int i1 = -1;
			for (int j1 = 0; j1 < 10; j1++)
				if (itemAmount >= itemDef.stackAmounts[j1] && itemDef.stackAmounts[j1] != 0)
					i1 = itemDef.stackIDs[j1];

			if (i1 != -1)
				itemDef = forID(i1);
		}

		Model model = itemDef.method201(1);
		if (model == null) {
			return null;
		}
		Sprite sprite1 = null;
		if (itemDef.certTemplateID != -1) {
			sprite1 = getSprite(itemDef.certID, 10, -1, 0,true);
			if (sprite1 == null) {
				return null;
			}
		}
		Sprite enabledSprite = new Sprite(18, 18);
		int k1 = Rasterizer.textureInt1;
		int l1 = Rasterizer.textureInt2;
		int ai[] = Rasterizer.anIntArray1472;
		int ai1[] = DrawingArea.pixels;
		int i2 = DrawingArea.width;
		int j2 = DrawingArea.height;
		int k2 = DrawingArea.topX;
		int l2 = DrawingArea.bottomX;
		int i3 = DrawingArea.topY;
		int j3 = DrawingArea.bottomY;
		Rasterizer.aBoolean1464 = false;
		DrawingArea.initDrawingArea(18, 18, enabledSprite.myPixels, new float[1024]);
		DrawingArea.method336(18, 0, 0, 0, 18);
		Rasterizer.method364();
		int k3 = (int) (itemDef.spriteScale * 1.6D);
		int sin = Rasterizer.Rasterizer3D_sine[itemDef.spritePitch] * k3 >> 16;
		int i4 = Rasterizer.Rasterizer3D_cosine[itemDef.spritePitch] * k3 >> 16;
		model.renderModel(itemDef.spriteCameraRoll, itemDef.spriteCameraYaw, itemDef.spritePitch, itemDef.spriteTranslateX,
				sin + model.modelHeight / 2 + itemDef.spriteTranslateY, i4 + itemDef.spriteTranslateY);
		if (itemDef.certTemplateID != -1) {
			int l5 = sprite1.maxWidth;
			int j6 = sprite1.maxHeight;
			sprite1.maxWidth = 18;
			sprite1.maxHeight = 18;
			sprite1.drawSprite(0, 0);
			sprite1.maxWidth = l5;
			sprite1.maxHeight = j6;
		}
		DrawingArea.initDrawingArea(j2, i2, ai1, new float[1024]);
		DrawingArea.setDrawingArea(j3, k2, l2, i3);
		Rasterizer.textureInt1 = k1;
		Rasterizer.textureInt2 = l1;
		Rasterizer.anIntArray1472 = ai;
		Rasterizer.aBoolean1464 = true;

		enabledSprite.maxWidth = 18;
		enabledSprite.maxHeight = 18;

		return enabledSprite;
	}

	public static Sprite getSprite(int itemId, int itemAmount, int highlightColor, int var3, boolean var5) {
		if (highlightColor == 0) {
			Sprite sprite = (Sprite) mruNodes1.insertFromCache(itemId);
			if (sprite != null && sprite.maxHeight != itemAmount && sprite.maxHeight != -1) {
				sprite.unlink();
				sprite = null;
			}
			if (sprite != null)
				return sprite;
		}
		ItemDefinition itemDef = forID(itemId);
		if (itemDef.stackIDs == null)
			itemAmount = -1;
		if (itemAmount > 1) {
			int i1 = -1;
			for (int j1 = 0; j1 < 10; j1++)
				if (itemAmount >= itemDef.stackAmounts[j1] && itemDef.stackAmounts[j1] != 0)
					i1 = itemDef.stackIDs[j1];

			if (i1 != -1)
				itemDef = forID(i1);
		}
		Model model = itemDef.method201(1);
		if (model == null)
			return null;
		Sprite sprite = null;
		if (itemDef.certTemplateID != -1) {
			sprite = getSprite(itemDef.certID, 10, -1, 0,true);
			if (sprite == null)
				return null;
		} else if (itemDef.notedId != -1) {
			sprite = getSprite(itemDef.unnotedId, itemAmount, -1, var3, false);
			if (sprite == null)
				return null;
		} else if (itemDef.placeholderTemplateId != -1) {
			sprite = getSprite(itemDef.placeholderId, itemAmount, -1, 0, false);
			if (sprite == null)
				return null;
		}
		Sprite sprite2 = new Sprite(32, 32);
		int k1 = Rasterizer.textureInt1;
		int l1 = Rasterizer.textureInt2;
		int ai[] = Rasterizer.anIntArray1472;
		int ai1[] = DrawingArea.pixels;
		int i2 = DrawingArea.width;
		int j2 = DrawingArea.height;
		int k2 = DrawingArea.topX;
		int l2 = DrawingArea.bottomX;
		int i3 = DrawingArea.topY;
		int j3 = DrawingArea.bottomY;
		Rasterizer.aBoolean1464 = false;
		DrawingArea.initDrawingArea(32, 32, sprite2.myPixels, new float[1024]);
		DrawingArea.method336(32, 0, 0, 0, 32);
		Rasterizer.method364();
		if (itemDef.placeholderTemplateId != -1) {
			int l5 = sprite.maxWidth;
			int j6 = sprite.maxHeight;
			sprite.maxWidth = 32;
			sprite.maxHeight = 32;
			sprite.drawSprite(0, 0);
			sprite.maxWidth = l5;
			sprite.maxHeight = j6;
		}
		int var16 = itemDef.spriteScale;
		if (var5) { // L: 409
			var16 = (int)((double)var16 * 1.5D);
		} else if (highlightColor == 2) { // L: 410
			var16 = (int)(1.04D * (double)var16);
		}

		int l3 = Rasterizer.Rasterizer3D_sine[itemDef.spritePitch] * var16 >> 16;
		int i4 = Rasterizer.Rasterizer3D_cosine[itemDef.spritePitch] * var16 >> 16;
		try {
			model.renderModel(itemDef.spriteCameraRoll, itemDef.spriteCameraYaw, itemDef.spritePitch, itemDef.spriteTranslateX,
					l3 + model.modelHeight / 2 + itemDef.spriteTranslateY, i4 + itemDef.spriteTranslateY);
		}  catch (ArrayIndexOutOfBoundsException ignored) {
		}  catch (Exception e) {
			e.printStackTrace();
		}
		if (itemDef.notedId != -1) {
			int l5 = sprite.maxWidth;
			int j6 = sprite.maxHeight;
			sprite.maxWidth = 32;
			sprite.maxHeight = 32;
			sprite.drawSprite(0, 0);
			sprite.maxWidth = l5;
			sprite.maxHeight = j6;
		}
		if (itemDef.certTemplateID != -1) {
			int l5 = sprite.maxWidth;
			int j6 = sprite.maxHeight;
			sprite.maxWidth = 32;
			sprite.maxHeight = 32;
			sprite.drawSprite(0, 0);
			sprite.maxWidth = l5;
			sprite.maxHeight = j6;
		}
		if (itemDef.certTemplateID != -1 && highlightColor == 0) {
			mruNodes1.removeFromCache(sprite2, itemId);
		}
		if (itemDef.certTemplateID != -1 && highlightColor == 1) {
			mruNodes1.removeFromCache(sprite2, itemId);
		}
		sprite2.outline(1);
		if (highlightColor > 0) {
			sprite2.outline(16777215);
		}
		if (var3 == 0) {
			sprite2.shadow(3153952);
		}
		DrawingArea.initDrawingArea(j2, i2, ai1, new float[1024]);
		DrawingArea.setDrawingArea(j3, k2, l2, i3);
		Rasterizer.textureInt1 = k1;
		Rasterizer.textureInt2 = l1;
		Rasterizer.anIntArray1472 = ai;
		Rasterizer.aBoolean1464 = true;
		if (itemDef.stackable)
			sprite2.maxWidth = 33;
		else
			sprite2.maxWidth = 32;
		sprite2.maxHeight = itemAmount;
		return sprite2;
	}

	public Model method201(int i) {
		if (stackIDs != null && i > 1) {
			int j = -1;
			for (int k = 0; k < 10; k++)
				if (i >= stackAmounts[k] && stackAmounts[k] != 0)
					j = stackIDs[k];

			if (j != -1)
				return forID(j).method201(1);
		}
		Model model = (Model) mruNodes2.insertFromCache(id);
		if (model != null)
			return model;
		model = Model.getModel(modelId);
		if (model == null)
			return null;
		if (groundScaleX != 128 || groundScaleY != 128 || groundScaleZ != 128)
			model.scale(groundScaleX, groundScaleZ, groundScaleY);
		if (originalModelColors != null) {
			for (int l = 0; l < originalModelColors.length; l++)
				model.recolor(originalModelColors[l], modifiedModelColors[l]);

		}
		if (originalTextureColors != null) {
			for (int k2 = 0; k2 < originalTextureColors.length; k2++)
				model.retexture(originalTextureColors[k2], modifiedTextureColors[k2]);

		}
		model.light(64 + ambient, 768 + contrast, -50, -10, -50, true);
		model.singleTile = true;
		mruNodes2.removeFromCache(model, id);
		return model;
	}

	public Model method202(int i) {
		if (stackIDs != null && i > 1) {
			int j = -1;
			for (int k = 0; k < 10; k++)
				if (i >= stackAmounts[k] && stackAmounts[k] != 0)
					j = stackIDs[k];

			if (j != -1)
				return forID(j).method202(1);
		}
		Model model = Model.getModel(modelId);
		if (model == null)
			return null;
		if (originalModelColors != null) {
			for (int l = 0; l < originalModelColors.length; l++)
				model.recolor(originalModelColors[l], modifiedModelColors[l]);

		}
		if (originalTextureColors != null) {
			for (int k2 = 0; k2 < originalTextureColors.length; k2++)
				model.retexture(originalTextureColors[k2], modifiedTextureColors[k2]);

		}
		return model;
	}

	private ItemDefinition() {
		id = -1;
	}

	private byte femaleTranslation;
	public int value;
	public int[] originalModelColors;
	public int[] modifiedModelColors;

	private short[] originalTextureColors;
	private short[] modifiedTextureColors;

	public int id;
	public static MRUNodes mruNodes1 = new MRUNodes(100);
	public static MRUNodes mruNodes2 = new MRUNodes(50);

	public boolean membersObject;
	private int tertiaryFemaleEquipmentModel;
	public int certTemplateID;
	public int secondaryFemaleModel;
	public int primaryMaleModel;
	private int secondaryMaleHeadPiece;
	private int groundScaleX;
	public String groundActions[];
	public int spriteTranslateX; // modelOffset1
	public String name;
	private static ItemDefinition[] cache;
	private int secondaryFemaleHeadPiece;
	public int modelId;
	public int primaryMaleHeadPiece;
	public boolean stackable;
	public String description;
	public int certID;
	private static int cacheIndex;
	public int spriteScale;
	private static Buffer stream;
	public int contrast;
	private int tertiaryMaleEquipmentModel;
	public int secondaryMaleModel;
	public String itemActions[];
	public String equipActions[];
	public int spritePitch; // modelRotation1
	private int groundScaleZ;
	private int groundScaleY;
	private int[] stackIDs;
	public int spriteTranslateY; // modelOffset2
	private static int[] streamIndices;
	public int ambient;
	public int primaryFemaleHeadPiece;
	public int spriteCameraRoll; // modelRotation2
	public int primaryFemaleModel;
	private int[] stackAmounts;
	public int team;
	public static int totalItems;
	public int spriteCameraYaw; // anInt204
	private byte maleTranslation; // aByte205
	public boolean searchable;
	private static BufferedWriter writer;

}
