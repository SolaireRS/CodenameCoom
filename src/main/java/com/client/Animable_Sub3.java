package com.client;

import com.client.definitions.GraphicsDefinition;
// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

final class Animable_Sub3 extends Renderable {

	public Animable_Sub3(int i, int j, int l, int i1, int j1, int k1, int l1) {
		aBoolean1567 = false;
		aSpotAnim_1568 = GraphicsDefinition.cache[i1];
		anInt1560 = i;
		anInt1561 = l1;
		anInt1562 = k1;
		anInt1563 = j1;
		anInt1564 = j + l;
		aBoolean1567 = false;
	}

	@Override
	public Model getRotatedModel() {
		Model model = aSpotAnim_1568.getModel();
		if (model == null)
			return null;
		int j = cur_frameindex;
		Model model_1 = new Model(true, AnimFrame.noAnimationInProgress(j), false, model);
		if (!aBoolean1567) {
			model_1.apply_label_groups();
			model_1.animate_either(aSpotAnim_1568.seqtype, j);
			model_1.face_label_groups = null;
			model_1.vertex_label_groups = null;
		}
		if (aSpotAnim_1568.resizeXY != 128 || aSpotAnim_1568.resizeZ != 128)
			model_1.scale(aSpotAnim_1568.resizeXY, aSpotAnim_1568.resizeXY,
					aSpotAnim_1568.resizeZ);
		if (aSpotAnim_1568.rotation != 0) {
			if (aSpotAnim_1568.rotation == 90)
				model_1.rotate90Degrees();
			if (aSpotAnim_1568.rotation == 180) {
				model_1.rotate90Degrees();
				model_1.rotate90Degrees();
			}
			if (aSpotAnim_1568.rotation == 270) {
				model_1.rotate90Degrees();
				model_1.rotate90Degrees();
				model_1.rotate90Degrees();
			}
		}
		model_1.light(64 + aSpotAnim_1568.modelBrightness,
				850 + aSpotAnim_1568.modelShadow, -30, -50, -30, true);
		return model_1;
	}

	public void method454(int i) {
		if(aSpotAnim_1568.seqtype.using_keyframes()) {
			frame_loop += i;//is this a mistake in jagex client?
			this.cur_frameindex += i;
			if (this.cur_frameindex >= aSpotAnim_1568.seqtype.get_keyframe_duration()) {
				this.aBoolean1567 = true;
			}
		} else {

			for (frame_loop += i; frame_loop > aSpotAnim_1568.seqtype.anIntArray354[cur_frameindex]; ) {
				frame_loop -= aSpotAnim_1568.seqtype.anIntArray354[cur_frameindex] + 1;
				cur_frameindex++;
				if (cur_frameindex >= aSpotAnim_1568.seqtype.framecount
						&& (cur_frameindex < 0 || cur_frameindex >= aSpotAnim_1568.seqtype.framecount)) {
					cur_frameindex = 0;
					aBoolean1567 = true;
				}
			}
		}

	}

	public final int anInt1560;
	public final int anInt1561;
	public final int anInt1562;
	public final int anInt1563;
	public final int anInt1564;
	public boolean aBoolean1567;
	private final GraphicsDefinition aSpotAnim_1568;
	private int cur_frameindex;
	private int frame_loop;
}
