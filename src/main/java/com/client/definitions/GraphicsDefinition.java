package com.client.definitions;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;

import com.client.*;
import com.client.sound.SoundRequest;
import com.client.sound.SoundRequestQueue;

public final class GraphicsDefinition {

	public static void unpackConfig(StreamLoader streamLoader) {
		Buffer stream = new Buffer(streamLoader.getArchiveData("spotanim.dat"));
		int length = stream.readUShort();
		if (cache == null)
			cache = new GraphicsDefinition[length + 1000];
		for (int j = 0; j < length; j++) {
			if (cache[j] == null) {
				cache[j] = new GraphicsDefinition();
			}
			if (j == 65535) {
				j = -1;
			}
			cache[j].index = j;
			cache[j].setDefault();
			cache[j].readValues(stream);
		}

		if (Configuration.dumpDataLists) {
			gfxDump();
		}
	}

	public static void gfxDump() {
		try {
			BufferedWriter fw = new BufferedWriter(new FileWriter("./temp/gfx_list.txt"));
			for (int i = 0; i < cache.length; i++) {
				GraphicsDefinition item = cache[i];
				if (item == null)
					continue;
				fw.write("case " + i + ":");
				fw.write(System.getProperty("line.separator"));

				fw.write("gfx.anIntArray409 = \"" + Arrays.toString(item.recolorToReplace) + "\";");
				fw.write(System.getProperty("line.separator"));

				fw.write("gfx.modelId = \"" + item.modelId + "\";");
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
	public short[] textureReplace;
	public short[] textureFind;
	private void readValues(Buffer stream) {
		while(true) {
			int opcode = stream.readUnsignedByte();
			if (opcode == 0) {
				return;
			}
			if (opcode == 1) {
				modelId = stream.readUShort();
			} else if (opcode == 2) {
				animationId = stream.readUShort();
				if (AnimationDefinition.anims != null) {
					seqtype = AnimationDefinition.anims[animationId];
				}
			} else if (opcode == 4) {
				resizeXY = stream.readUShort();
			} else if (opcode == 5) {
				resizeZ = stream.readUShort();
			} else if (opcode == 6) {
				rotation = stream.readUShort();
			} else if (opcode == 7) {
				modelBrightness = stream.readUnsignedByte();
			} else if (opcode == 8) {
				modelShadow = stream.readUnsignedByte();
			} else if (opcode == 40) {
				int j = stream.readUnsignedByte();
				recolorToFind = new int[j];
				recolorToReplace = new int[j];

				for (int k = 0; k < j; k++) {
					recolorToFind[k] = stream.readUShort();
					recolorToReplace[k] = stream.readUShort();
				}
			} else if (opcode == 41) { // re-texture
				int len = stream.readUnsignedByte();
				textureFind = new short[len];
				textureReplace = new short[len];
				for (int i = 0; i < len; i++) {
					textureFind[i] = (short) stream.readUShort();
					textureReplace[i] = (short) stream.readUShort();
				}
			} else {
				System.out.println("Error unrecognised spotanim config code: " + opcode);
			}
		}
	}

	
	public static GraphicsDefinition fetch(int modelId) {
		for (GraphicsDefinition anim : cache) {
			if (anim == null) {
				continue;
			}
			if (anim.modelId == modelId) {
				return anim;
			}
		}
		return null;
	}

	public Model getModel() {
		Model model = (Model) recent_models.insertFromCache(index);
		if (model != null)
			return model;
		model = Model.getModel(modelId);
		if (model == null)
			return null;
		for (int i = 0; i < recolorToFind.length; i++)
			if (recolorToFind[0] != 0) //default frame id
				model.recolor(recolorToFind[i], recolorToReplace[i]);
		if (textureFind != null) {
			for (int i1 = 0; i1 < textureFind.length; i1++)
				model.retexture(textureFind[i1], textureReplace[i1]);
		}
		recent_models.removeFromCache(model, index);
		return model;
	}
	public Model get_transformed_model(int frameindex) {
		Model model = (Model) recent_models.insertFromCache(index);
		if(model == null) {
			model = Model.getModel(modelId);
			if (model == null)
				return null;
			for (int i = 0; i < recolorToFind.length; i++)
				if (recolorToFind[0] != 0) //default frame id
					model.recolor(recolorToFind[i], recolorToReplace[i]);
			if (textureReplace != null) {
				for (int i1 = 0; i1 < textureReplace.length; i1++)
					model.retexture(textureReplace[i1], textureFind[i1]);
			}
			recent_models.removeFromCache(model, index);
		}
		Model var6;
		if (animationId != -1 && frameindex != -1) {
			var6 = seqtype.bake_and_animate_spotanim(model, frameindex);
			if (seqtype.frameSounds != null && seqtype.frameSounds.length > seqtype.framecount) {
				int[] soundGroup = new int[]{seqtype.frameSounds[seqtype.framecount]};
				if (soundGroup != null) {
					for (int soundId : soundGroup) {
						// You may adjust volume mapping here based on how your mixer handles it
						// Queue download
						Client.instance.onDemandFetcher.provide(6, soundId);
						SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - com.client.sound.SoundPlayer.getVolume()), 0));
					}
				}
			}
		} else {
			var6 = model.bake_shared_model(true);
		}

		//new_model.animate_either(seqtype, frameindex);
		var6.face_label_groups = null;
		var6.vertex_label_groups = null;
		if (resizeXY != 128 || resizeZ != 128)
			var6.scale(resizeXY, resizeXY, resizeZ);
		var6.light(64 + modelBrightness, 850 + modelShadow, -30, -50, -30, true);

		if (this.rotation != 0) {
			if (this.rotation == 90) {
				var6.rotate90Degrees();
			}

			if (this.rotation == 180) {
				var6.rotate90Degrees();
				var6.rotate90Degrees();
			}

			if (this.rotation == 270) {
				var6.rotate90Degrees();
				var6.rotate90Degrees();
				var6.rotate90Degrees();
			}
		}
		return var6;
	}
	private void setDefault() {
		modelId = -1;
		animationId = -1;
		recolorToFind = new int[6];
		recolorToReplace = new int[6];
		resizeXY = 128;
		resizeZ = 128;
		rotation = 0;
		modelBrightness = 0;
		modelShadow = 0;
	}

	public GraphicsDefinition() {
		anInt400 = 9;
		animationId = -1;
		recolorToFind = new int[6];
		recolorToReplace = new int[6];
		resizeXY = 128;
		resizeZ = 128;
	}
	
	public int getModelId() {
		return modelId;
	}
	
	public int getIndex() {
		return index;
	}

	public final int anInt400;
	public static GraphicsDefinition cache[];
	private int index;
	private int modelId;
	public int animationId;
	public AnimationDefinition seqtype;
	public int[] recolorToFind;
	public int[] recolorToReplace;
	public int resizeXY;
	public int resizeZ;
	public int rotation;
	public int modelBrightness;
	public int modelShadow;
	public static MRUNodes recent_models = new MRUNodes(30);

}
