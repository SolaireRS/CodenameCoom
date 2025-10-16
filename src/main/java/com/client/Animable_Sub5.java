package com.client;

import com.client.definitions.AnimationDefinition;
import com.client.definitions.ObjectDefinition;
import com.client.definitions.VarBit;

final class Animable_Sub5 extends Renderable {
	private int current_frameindex;
	private final int[] anIntArray1600;
	private final int anInt1601;
	private final int anInt1602;
	private final int anInt1603;
	private final int anInt1604;
	private final int anInt1605;
	private final int anInt1606;
	private AnimationDefinition seqtype;
	private int frame_tick;
	public static Client clientInstance;
	private final int anInt1610;
	private final int anInt1611;
	private final int anInt1612;

	/*
	 * private ObjectDef method457() { int i = -1; if(anInt1601 != -1) { try {
	 * VarBit varBit = VarBit.overlays[anInt1601]; int k = varBit.anInt648; int l =
	 * varBit.anInt649; int i1 = varBit.anInt650; int j1 =
	 * client.anIntArray1232[i1 - l]; i = clientInstance.variousSettings[k] >> l
	 * & j1; } catch(Exception ex){} } else if(anInt1602 != -1) i =
	 * clientInstance.variousSettings[anInt1602]; if(i < 0 || i >=
	 * anIntArray1600.length || anIntArray1600[i] == -1) return null; else
	 * return ObjectDef.forID(anIntArray1600[i]); }
	 */
	private ObjectDefinition method457() {
		int i = -1;
		if (anInt1601 != -1 && anInt1601 < VarBit.cache.length) {
			VarBit varBit = VarBit.cache[anInt1601];
			int k = varBit.configID;
			int l = varBit.lsb;
			int i1 = varBit.msb;
			int j1 = Client.anIntArray1232[i1 - l];
			i = clientInstance.variousSettings[k] >> l & j1;
		} else if (anInt1602 != -1
				&& anInt1602 < clientInstance.variousSettings.length)
			i = clientInstance.variousSettings[anInt1602];
		if (i < 0 || i >= anIntArray1600.length || anIntArray1600[i] == -1)
			return null;
		else
			return ObjectDefinition.forID(anIntArray1600[i]);
	}

	@Override
	public Model getRotatedModel() {
		int frame = -1;
		if (seqtype != null) {
			int ticks_to_process = Client.update_tick - frame_tick;
			if (ticks_to_process > 100 && seqtype.loop_delay > 0)
				ticks_to_process = 100;
			if(seqtype.using_keyframes()) {
				int var3 = this.seqtype.get_keyframe_duration();
				this.current_frameindex += ticks_to_process;
				ticks_to_process = 0;
				if (this.current_frameindex >= var3) {
					this.current_frameindex = var3 - this.seqtype.loop_delay;
					if (this.current_frameindex < 0 || this.current_frameindex > var3) {
						this.seqtype = null;
					}
				}
			} else {
				while (ticks_to_process > seqtype.anIntArray354[current_frameindex]) {
					ticks_to_process -= seqtype.anIntArray354[current_frameindex];
					current_frameindex++;
					if (current_frameindex < seqtype.framecount)
						continue;
					current_frameindex -= seqtype.loop_delay;
					if (current_frameindex >= 0 && current_frameindex < seqtype.framecount)
						continue;
					seqtype = null;
					break;
				}
			}
			frame_tick = Client.update_tick - ticks_to_process;
			if (seqtype != null) {
				frame = current_frameindex;
			}
		}
		ObjectDefinition class46;
		if (anIntArray1600 != null)
			class46 = method457();
		else
			class46 = ObjectDefinition.forID(anInt1610);
		if (class46 == null) {
			return null;
		} else {
			return class46.modelAt(anInt1611, anInt1612, anInt1603,
					anInt1604, anInt1605, anInt1606, frame, seqtype);
		}
	}

	public Animable_Sub5(int i, int j, int k, int l, int i1, int j1, int k1,
			int l1, boolean flag) {
		anInt1610 = i;
		anInt1611 = k;
		anInt1612 = j;
		anInt1603 = j1;
		anInt1604 = l;
		anInt1605 = i1;
		anInt1606 = k1;
		if (l1 != -1) {
			try {
				seqtype = AnimationDefinition.anims[l1];
				current_frameindex = 0;
				frame_tick = Client.update_tick;
				if (flag && seqtype.loop_delay != -1) {
					if (seqtype.using_keyframes()) {
						current_frameindex = (int)(Math.random() * (double)seqtype.get_keyframe_duration());
					} else {
						current_frameindex = (int) (Math.random() * seqtype.framecount);
						frame_tick -= (int) (Math.random() * seqtype.anIntArray354[current_frameindex]);
					}
				}
			}catch (Exception e) {

			}
		}
		ObjectDefinition class46 = ObjectDefinition.forID(anInt1610);
		anInt1601 = class46.varpId;
		anInt1602 = class46.configId;
		anIntArray1600 = class46.childrenIDs;
	}
}