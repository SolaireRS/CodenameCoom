package com.client;

public final class AnimFrame {


	public static void load_osrs_frames(Buffer stream, int group_id, int framegroup_count, AnimBase class18, AnimFrame[] out) {
		try {

			int indices[] = new int[500];
			int ai1[] = new int[500];
			int ai2[] = new int[500];
			int ai3[] = new int[500];
			for(int l1 = 0; l1 < framegroup_count; l1++) {
				int frame_id = stream.readUShort();
				int frame_size = stream.readUShort();
				int before_frame = stream.currentOffset;
				int frame_end = frame_size + before_frame;
				if(frame_id == 65535) {
					System.out.println("Empty frame at anim" + group_id + ", read frameidx" + frame_id +", iterated frameidx" + l1 + ", framecount:" + framegroup_count);
					continue;
				}
				if(frame_id != l1) {
					System.out.println("Frame id mismatch " + group_id + ", read frameidx" + frame_id +", iterated frameidx" + l1 + ", framecount:" + framegroup_count);
					continue;
				}

				//might need to put frame_id here and forget about using the l1
				AnimFrame frame = out[l1] = new AnimFrame();
				frame.frameset_id = group_id;
				frame.base = class18;
				int transform_count = stream.get_unsignedbyte();
				int attr_pos = stream.currentOffset;
				int data_pos = attr_pos + transform_count;
				int read_attrs = 0;
				int used = 0;
				int last = -1;

				for(int index = 0; index < transform_count; index++) {
					stream.currentOffset = attr_pos;
					int attr = stream.get_unsignedbyte();
					attr_pos = stream.currentOffset;
					if(attr > 0) {
						if(class18.transform_types[index] != 0) {
							for(int l3 = index - 1; l3 > last; l3--) {
								if(class18.transform_types[l3] != 0)
									continue;
								indices[used] = l3;
								ai1[used] = 0;
								ai2[used] = 0;
								ai3[used] = 0;
								used++;
								break;
							}
						}
						indices[used] = index;
						short value = 0;
						stream.currentOffset = data_pos;
						int startpos = stream.currentOffset;
						if(class18.transform_types[index] == 3)
							value = (short)128;
						if((attr & 1) != 0)
							ai1[used] = stream.get_smart_byteorshort();
						else
							ai1[used] = value;
						if((attr & 2) != 0)
							ai2[used] = stream.get_smart_byteorshort();
						else
							ai2[used] = value;
						if((attr & 4) != 0)
							ai3[used] = stream.get_smart_byteorshort();
						else
							ai3[used] = value;
						last = index;
						used++;
						if (frame.base.transform_types[index] == 5) {//alpha
							frame.mod_alpha = true;
						}
						data_pos = stream.currentOffset;

					}
				}
				if(data_pos != frame_end) {
					System.err.println("Frame decoding read size mismatch pos " + data_pos +", expected: " + frame_end +", " +  group_id + ", read frameidx" + frame_id +", iterated frameidx" + l1 + ", framecount:" + framegroup_count);
				}
				frame.transformationCount = used;
				frame.transformationIndices = new int[used];
				frame.transformX = new int[used];
				frame.transformY = new int[used];
				frame.transformZ = new int[used];
				for(int k3 = 0; k3 < used; k3++) {
					frame.transformationIndices[k3] = indices[k3];
					frame.transformX[k3] = ai1[k3];
					frame.transformY[k3] = ai2[k3];
					frame.transformZ[k3] = ai3[k3];
				}
			}
		} catch(Exception exception) {
			exception.printStackTrace();
			System.err.println("Error unpacking anim frames: " + group_id);
		}
	}

	public static int get_fileid(int frame_id) {
		try {
			int file = frame_id >>> 16;
			int k = frame_id & 0xffff;
			return file;
		} catch (NumberFormatException | StringIndexOutOfBoundsException e) {
			return 0;
		}
	}


	public static void nullLoader() {
		AnimFrameSet.frameset = null;
	}

	public static boolean noAnimationInProgress(int i) {
		return i == -1;
	}

	public int frameset_id;
	public int delay;
	public AnimBase base;
	public int transformationCount;
	public int transformationIndices[];
	public int transformX[];
	public int transformY[];
	public int transformZ[];
	boolean mod_alpha = false;
	public static Client clientInstance;

	public static void load_641(int file, Buffer stream, int transform_count, AnimBase class18, AnimFrame[] out){
		try {
			int ai[] = new int[500];
			int ai1[] = new int[500];
			int ai2[] = new int[500];
			int ai3[] = new int[500];
			for(int l1 = 0; l1 < transform_count; l1++)
			{
				int i2 = stream.readUShort();
				AnimFrame class36 = AnimFrameSet.frameset[file].frames[i2] = new AnimFrame();
				class36.base = class18;
				int j2 = stream.get_unsignedbyte();
				int l2 = 0;
				int k2 = -1;
				for(int i3 = 0; i3 < j2; i3++)
				{
					int j3 = stream.get_unsignedbyte();

					if(j3 > 0)
					{
						if(class18.transform_types[i3] != 0)
						{
							for(int l3 = i3 - 1; l3 > k2; l3--)
							{
								if(class18.transform_types[l3] != 0)
									continue;
								ai[l2] = l3;
								ai1[l2] = 0;
								ai2[l2] = 0;
								ai3[l2] = 0;
								l2++;
								break;
							}

						}
						ai[l2] = i3;
						short c = 0;
						if(class18.transform_types[i3] == 3)
							c = (short)128;

						if((j3 & 1) != 0)
							ai1[l2] = (short)stream.get_short();
						else
							ai1[l2] = c;
						if((j3 & 2) != 0)
							ai2[l2] = stream.get_short();
						else
							ai2[l2] = c;
						if((j3 & 4) != 0)
							ai3[l2] = stream.get_short();
						else
							ai3[l2] = c;
						k2 = i3;
						l2++;
					}
				}

				class36.transformationCount = l2;
				class36.transformationIndices = new int[l2];
				class36.transformX = new int[l2];
				class36.transformY = new int[l2];
				class36.transformZ = new int[l2];
				for(int k3 = 0; k3 < l2; k3++)
				{
					class36.transformationIndices[k3] = ai[k3];
					class36.transformX[k3] = ai1[k3];
					class36.transformY[k3] = ai2[k3];
					class36.transformZ[k3] = ai3[k3];
				}

			}
		}catch(Exception exception) { }
	}
}

