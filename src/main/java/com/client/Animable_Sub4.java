package com.client;

import com.client.definitions.GraphicsDefinition;
// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

final class Animable_Sub4 extends Renderable {

	public void method455(int i, int j, int k, int l) {
		if (!is_moving) {
			double d = l - anInt1580;
			double d2 = j - anInt1581;
			double d3 = Math.sqrt(d * d + d2 * d2);
			x = anInt1580 + (d * anInt1589) / d3;
			y = anInt1581 + (d2 * anInt1589) / d3;
			z = anInt1582;
		}
		double d1 = (anInt1572 + 1) - i;
		x_speed = (l - x) / d1;
		y_speed = (j - y) / d1;
		speed = Math.sqrt(x_speed * x_speed + y_speed
				* y_speed);
		if (!is_moving)
			z_speed = -speed * Math.tan(anInt1588 * 0.02454369D);
		z_acceleration = (2D * (k - z - z_speed * d1)) / (d1 * d1);
	}

	@Override
	public Model getRotatedModel() {
		Model model = spotanim.getModel();
		if (model == null)
			return null;
		int j = -1;
		if (spotanim.seqtype != null)
			j = cur_frameindex;
		Model model_1 = new Model(true, AnimFrame.noAnimationInProgress(j), false, model);
		if (j != -1) {
			model_1.apply_label_groups();
			model_1.animate_either(spotanim.seqtype, j);
			model_1.face_label_groups = null;
			model_1.vertex_label_groups = null;
		}
		if (spotanim.resizeXY != 128 || spotanim.resizeZ != 128)
			model_1.scale(spotanim.resizeXY, spotanim.resizeXY,
					spotanim.resizeZ);
		model_1.rotateZ(anInt1596);
		model_1.light(64 + spotanim.modelBrightness,
				850 + spotanim.modelShadow, -30, -50, -30, true);
		return model_1;
	}

	public Animable_Sub4(int i, int j, int l, int i1, int j1, int k1, int l1,
			int i2, int j2, int k2, int l2) {
		is_moving = false;
		spotanim = GraphicsDefinition.cache[l2];
		anInt1597 = k1;
		anInt1580 = j2;
		anInt1581 = i2;
		anInt1582 = l1;
		anInt1571 = l;
		anInt1572 = i1;
		anInt1588 = i;
		anInt1589 = j1;
		anInt1590 = k2;
		anInt1583 = j;
		is_moving = false;
	}

	public void method456(int i) {
		is_moving = true;
		x += x_speed * i;
		y += y_speed * i;
		z += z_speed * i + 0.5D * z_acceleration * i * i;
		z_speed += z_acceleration * i;
		anInt1595 = (int) (Math.atan2(x_speed, y_speed) * 325.94900000000001D) + 1024 & 0x7ff;
		anInt1596 = (int) (Math.atan2(z_speed, speed) * 325.94900000000001D) & 0x7ff;
		if (spotanim.seqtype != null)
			if (spotanim.seqtype.using_keyframes()) {
				this.cur_frameindex += i;
				int var3 = spotanim.seqtype.get_keyframe_duration();
				if (this.cur_frameindex >= var3) {
					this.cur_frameindex = var3 - spotanim.seqtype.loop_delay;
				}
			} else {
				for (frame_loop += i; frame_loop > spotanim.seqtype.anIntArray354[cur_frameindex]; ) {
					frame_loop -= spotanim.seqtype.anIntArray354[cur_frameindex] + 1;
					cur_frameindex++;
					if (cur_frameindex >= spotanim.seqtype.framecount)
						cur_frameindex = 0;
				}
			}

	}

	public final int anInt1571;
	public final int anInt1572;
	private double x_speed;
	private double y_speed;
	private double speed;
	private double z_speed;
	private double z_acceleration;
	private boolean is_moving;
	private final int anInt1580;
	private final int anInt1581;
	private final int anInt1582;
	public final int anInt1583;
	public double x;
	public double y;
	public double z;
	private final int anInt1588;
	private final int anInt1589;
	public final int anInt1590;
	private final GraphicsDefinition spotanim;
	private int cur_frameindex;
	private int frame_loop;
	public int anInt1595;
	private int anInt1596;
	public final int anInt1597;
}
