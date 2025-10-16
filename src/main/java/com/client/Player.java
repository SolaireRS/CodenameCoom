package com.client;

import java.util.ArrayList;
import java.util.List;

import com.client.definitions.AnimationDefinition;
import com.client.definitions.ItemDefinition;
import com.client.definitions.NpcDefinition;
import com.client.definitions.GraphicsDefinition;
import com.client.sound.SoundRequest;
import com.client.sound.SoundRequestQueue;

public final class Player extends Entity {

	@Override
	public Model getRotatedModel() {
		if (!visible)
			return null;
		Model model = method452();
		if (model == null)
			return null;
		super.height = model.modelHeight;
		model.singleTile = true;
		if (aBoolean1699)
			return model;
		if (super.spotanim != -1 && super.spotanimframe_index != -1) {
			GraphicsDefinition spotAnim = GraphicsDefinition.cache[super.spotanim];
			Model model_3 = spotAnim.get_transformed_model(super.spotanimframe_index);
			// Added
			if(model_3 != null) {
				model_3.offsetBy(0, -super.spotanim_height, 0);
				Model aclass30_sub2_sub4_sub6_1s[] = {model, model_3};
				model = new Model(aclass30_sub2_sub4_sub6_1s);
			}
		}

		if (aModel_1714 != null) {
			if (Client.update_tick >= anInt1708)
				aModel_1714 = null;
			if (Client.update_tick >= anInt1707 && Client.update_tick < anInt1708) {
				Model model_1 = aModel_1714;
				model_1.offsetBy(anInt1711 - super.x, anInt1712 - anInt1709,
						anInt1713 - super.y);
				if (super.getTurnDirection() == 512) {
					model_1.rotate90Degrees();
					model_1.rotate90Degrees();
					model_1.rotate90Degrees();
				} else if (super.getTurnDirection() == 1024) {
					model_1.rotate90Degrees();
					model_1.rotate90Degrees();
				} else if (super.getTurnDirection() == 1536)
					model_1.rotate90Degrees();
				Model aclass30_sub2_sub4_sub6s[] = { model, model_1 };
				model = new Model(aclass30_sub2_sub4_sub6s);
				if (super.getTurnDirection() == 512)
					model_1.rotate90Degrees();
				else if (super.getTurnDirection() == 1024) {
					model_1.rotate90Degrees();
					model_1.rotate90Degrees();
				} else if (super.getTurnDirection() == 1536) {
					model_1.rotate90Degrees();
					model_1.rotate90Degrees();
					model_1.rotate90Degrees();
				}
				model_1.offsetBy(super.x - anInt1711, anInt1709 - anInt1712,
						super.y - anInt1713);
			}
		}
		model.singleTile = true;
		return model;
	}
	public int usedItemID;
	public String title;
	public String titleColor;

	public void updatePlayer(Buffer stream) {
		stream.currentOffset = 0;
		anInt1702 = stream.readUnsignedByte();
		title = stream.readString();
		titleColor = stream.readString();
		healthState = stream.readUnsignedByte();
		headIcon = stream.readUnsignedByte();
		skullIcon = stream.readUnsignedByte();
		desc = null;
		team = 0;
		for (int j = 0; j < 12; j++) {
			int k = stream.readUnsignedByte();
			if (k == 0) {
				equipment[j] = 0;
				continue;
			}
			int i1 = stream.readUnsignedByte();
			equipment[j] = (k << 8) + i1;
			if (j == 0 && equipment[0] == 65535) {
				desc = NpcDefinition.forID(stream.readUShort());
				break;
			}
			if (equipment[j] >= 512 && equipment[j] - 512 < ItemDefinition.totalItems) {
				int l1 = ItemDefinition.forID(equipment[j] - 512).team;
				if (l1 != 0)
					team = l1;
			}
		}

		for (int l = 0; l < 5; l++) {
			int j1 = stream.readUnsignedByte();
			if (j1 < 0 || j1 >= Client.anIntArrayArray1003[l].length)
				j1 = 0;
			anIntArray1700[l] = j1;
		}

		super.readyanim = stream.readUShort();
		if (super.readyanim == 65535)
			super.readyanim = -1;
		super.readyanim_l = stream.readUShort();
		if (super.readyanim_l == 65535)
			super.readyanim_l = -1;
		super.walkanim = stream.readUShort();
		if (super.walkanim == 65535)
			super.walkanim = -1;
		super.walkanim_b = stream.readUShort();
		if (super.walkanim_b == 65535)
			super.walkanim_b = -1;
		super.walkanim_l = stream.readUShort();
		if (super.walkanim_l == 65535)
			super.walkanim_l = -1;
		super.walkanim_r = stream.readUShort();
		if (super.walkanim_r == 65535)
			super.walkanim_r = -1;
		super.runanim = stream.readUShort();
		if (super.runanim == 65535)
			super.runanim = -1;
		displayName = stream.readString();
		visible = stream.readUnsignedByte() == 0;
		combatLevel = stream.readUnsignedByte();
		rights = PlayerRights.readRightsFromPacket(stream).getRight();
		displayedRights = PlayerRights.getDisplayedRights(rights);
		skill = stream.readUShort();
		aLong1718 = 0L;
		for (int k1 = 0; k1 < 12; k1++) {
			aLong1718 <<= 4;
			if (equipment[k1] >= 256)
				aLong1718 += equipment[k1] - 256;
		}

		if (equipment[0] >= 256)
			aLong1718 += equipment[0] - 256 >> 4;
		if (equipment[1] >= 256)
			aLong1718 += equipment[1] - 256 >> 8;
		for (int i2 = 0; i2 < 5; i2++) {
			aLong1718 <<= 3;
			aLong1718 += anIntArray1700[i2];
		}

		aLong1718 <<= 1;
		aLong1718 += anInt1702;
	}

	public Model method452() {
		AnimationDefinition readyanim_seqtype = super.primaryanim != -1 && super.primaryanim_pause == 0
				? AnimationDefinition.anims[super.primaryanim] : null;
		AnimationDefinition walkanim_seqtype =
				(super.secondaryanim == -1 ||
						(super.secondaryanim == super.readyanim && readyanim_seqtype != null))
						? null : AnimationDefinition.anims[super.secondaryanim];
		if (desc != null) {
//			int single_index = -1;
//			AnimationDefinition single_seq = null;
//			if (super.primaryanim >= 0 && super.primaryanim_pause == 0) {
//				single_seq = AnimationDefinition.anims[super.primaryanim];
//				single_index = super.primaryanim_frameindex;
//			} else if (super.secondaryanim >= 0) {
//				single_seq = AnimationDefinition.anims[super.secondaryanim];
//				single_index = super.secondaryanim_frameindex;
//			}
			Model model = desc.method164(super.primaryanim_frameindex, readyanim_seqtype, null, super.secondaryanim_frameindex, walkanim_seqtype);
			return model;
		}
		long l = aLong1718;
		int k = -1;
		int i1 = -1;
		int j1 = -1;
		int k1 = -1;
		if (super.primaryanim >= 0 && super.primaryanim_pause == 0) {
			AnimationDefinition animation = AnimationDefinition.anims[super.primaryanim];
			k = animation.anIntArray353[super.primaryanim_frameindex];
			if (animation.frameSounds != null && animation.frameSounds.length > super.primaryanim_frameindex) {
				int[] soundGroup = new int[]{animation.frameSounds[super.primaryanim_frameindex]};
				if (soundGroup != null) {
					for (int soundId : soundGroup) {
						// You may adjust volume mapping here based on how your mixer handles it
						// Queue download
						Client.instance.onDemandFetcher.provide(6, soundId);
						SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - com.client.sound.SoundPlayer.getVolume()), 0));
					}
				}
			}
			if (super.secondaryanim >= 0 && super.secondaryanim != super.readyanim)
				i1 = AnimationDefinition.anims[super.secondaryanim].anIntArray353[super.secondaryanim_replaycount];
			if (animation.frameSounds != null && animation.frameSounds.length > super.secondaryanim_replaycount) {
				int[] soundGroup = new int[]{animation.frameSounds[super.secondaryanim_replaycount]};
				if (soundGroup != null) {
					for (int soundId : soundGroup) {
						// You may adjust volume mapping here based on how your mixer handles it
						// Queue download
						Client.instance.onDemandFetcher.provide(6, soundId);
						SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - com.client.sound.SoundPlayer.getVolume()), 0));
					}
				}
			}
			if (animation.shield >= 0) {
				j1 = animation.shield;
				l += j1 - equipment[5] << 40;
			}
			if (animation.weapon >= 0) {
				k1 = animation.weapon;
				l += k1 - equipment[3] << 48;
			}
		} else if (super.secondaryanim >= 0)
			k = AnimationDefinition.anims[super.secondaryanim].anIntArray353[super.secondaryanim_replaycount];
		if (AnimationDefinition.anims[super.secondaryanim].frameSounds != null && AnimationDefinition.anims[super.secondaryanim].frameSounds.length > super.secondaryanim_replaycount) {
			int[] soundGroup = new int[]{AnimationDefinition.anims[super.secondaryanim].frameSounds[super.secondaryanim_replaycount]};
			if (soundGroup != null) {
				for (int soundId : soundGroup) {
					// You may adjust volume mapping here based on how your mixer handles it
					// Queue download
					Client.instance.onDemandFetcher.provide(6, soundId);
					SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - com.client.sound.SoundPlayer.getVolume()), 0));
				}
			}
		}
		Model model_1 = (Model) mruNodes.insertFromCache(l);
		if (model_1 == null) {
			boolean flag = false;
			for (int i2 = 0; i2 < 12; i2++) {
				int k2 = equipment[i2];
				if (k1 >= 0 && i2 == 3)
					k2 = k1;
				if (j1 >= 0 && i2 == 5)
					k2 = j1;
				if (k2 >= 256 && k2 < 512 && !IDK.cache[k2 - 256].method537())
					flag = true;
				if (k2 >= 512 && !ItemDefinition.forID(k2 - 512).method195(anInt1702))
					flag = true;
			}

			if (flag) {
				if (aLong1697 != -1L)
					model_1 = (Model) mruNodes.insertFromCache(aLong1697);
				if (model_1 == null)
					return null;
			}
		}
		if (model_1 == null) {
			Model aclass30_sub2_sub4_sub6s[] = new Model[12];
			int j2 = 0;
			for (int l2 = 0; l2 < 12; l2++) {
				int i3 = equipment[l2];
				if (k1 >= 0 && l2 == 3)
					i3 = k1;
				if (j1 >= 0 && l2 == 5)
					i3 = j1;
				if (i3 >= 256 && i3 < 512) {
					Model model_3 = IDK.cache[i3 - 256].method538();
					if (model_3 != null)
						aclass30_sub2_sub4_sub6s[j2++] = model_3;
				}
				if (i3 >= 512) {
					Model model_4 = ItemDefinition.forID(i3 - 512)
							.method196(anInt1702);
					if (model_4 != null)
						aclass30_sub2_sub4_sub6s[j2++] = model_4;
				}
			}

			model_1 = new Model(j2, aclass30_sub2_sub4_sub6s);
			for (int j3 = 0; j3 < 5; j3++)
				if (anIntArray1700[j3] != 0) {
					model_1.recolor(Client.anIntArrayArray1003[j3][0],
							Client.anIntArrayArray1003[j3][anIntArray1700[j3]]);
					if (j3 == 1)
						model_1.recolor(Client.anIntArray1204[0],
								Client.anIntArray1204[anIntArray1700[j3]]);
				}

			model_1.apply_label_groups();
			 model_1.light(64, 850, -30, -50, -30, true);
			//model_1.method479(84, 1000, -90, -580, -90, true);
			mruNodes.removeFromCache(model_1, l);
			aLong1697 = l;
		}
		if (aBoolean1699)
			return model_1;
		if (readyanim_seqtype == null && walkanim_seqtype == null) {
			return model_1;
		} else {
			Model var22;
			if (readyanim_seqtype != null && walkanim_seqtype != null) {
				var22 = readyanim_seqtype.animate_multiple(model_1, super.primaryanim_frameindex, walkanim_seqtype, super.secondaryanim_frameindex);
			} else if (readyanim_seqtype == null) {
				var22 = walkanim_seqtype.animate_either(model_1, super.secondaryanim_frameindex);
			} else {
				var22 = readyanim_seqtype.animate_either(model_1, super.primaryanim_frameindex);
			}

			var22.calculateBoundsCylinder();
			var22.face_label_groups = null;
			var22.vertex_label_groups = null;

			return var22;
		}
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	public int privelage;

	public boolean isAdminRights() {
		return hasRights(PlayerRights.ADMINISTRATOR)
				|| hasRights(PlayerRights.OWNER)
				|| hasRights(PlayerRights.hidden_owner)
				|| hasRights(PlayerRights.GAME_DEVELOPER);
	}

	public boolean hasRightsOtherThan(PlayerRights playerRight) {
		return PlayerRights.hasRightsOtherThan(rights, playerRight);
	}

	public boolean hasRights(PlayerRights playerRights) {
		return PlayerRights.hasRights(rights, playerRights);
	}

	public boolean hasRightsLevel(int rightsId) {
		return PlayerRights.hasRightsLevel(rights, rightsId);
	}

	public boolean hasRightsBetween(int low, int high) {
		return PlayerRights.hasRightsBetween(rights, low, high);
	}

	public Model method453() {
		if (!visible)
			return null;
		if (desc != null)
			return desc.method160();
		boolean flag = false;
		for (int i = 0; i < 12; i++) {
			int j = equipment[i];
			if (j >= 256 && j < 512 && !IDK.cache[j - 256].method539())
				flag = true;
			if (j >= 512 && !ItemDefinition.forID(j - 512).method192(anInt1702))
				flag = true;
		}

		if (flag)
			return null;
		Model aclass30_sub2_sub4_sub6s[] = new Model[12];
		int k = 0;
		for (int l = 0; l < 12; l++) {
			int i1 = equipment[l];
			if (i1 >= 256 && i1 < 512) {
				Model model_1 = IDK.cache[i1 - 256].method540();
				if (model_1 != null)
					aclass30_sub2_sub4_sub6s[k++] = model_1;
			}
			if (i1 >= 512) {
				Model model_2 = ItemDefinition.forID(i1 - 512).method194(anInt1702);
				if (model_2 != null)
					aclass30_sub2_sub4_sub6s[k++] = model_2;
			}
		}

		Model model = new Model(k, aclass30_sub2_sub4_sub6s);
		for (int j1 = 0; j1 < 5; j1++)
			if (anIntArray1700[j1] != 0) {
				model.recolor(Client.anIntArrayArray1003[j1][0],
						Client.anIntArrayArray1003[j1][anIntArray1700[j1]]);
				if (j1 == 1)
					model.recolor(Client.anIntArray1204[0],
							Client.anIntArray1204[anIntArray1700[j1]]);
			}

		return model;
	}

	Player() {
		aLong1697 = -1L;
		aBoolean1699 = false;
		anIntArray1700 = new int[5];
		visible = false;
		equipment = new int[12];
	}

	public boolean inFlowerPokerArea() {
		int x = getAbsoluteX();
		int y = getAbsoluteY();
		return x >= 3109 && y >= 3504 && x <= 3121 && y <= 3515;
	}

	public boolean inFlowerPokerChatProximity() {
		int x = getAbsoluteX();
		int y = getAbsoluteY();
		return x >= 3106 && y >= 3502 && x <= 3123 && y <= 3517;
	}
	
	public PlayerRights[] getRights() {
		return rights;
	}

	public List<PlayerRights> getDisplayedRights() {
		return displayedRights;
	}
	
	public int getHealthState() {
		return healthState;
	}

	private PlayerRights[] rights = new PlayerRights[] {PlayerRights.PLAYER};
	private List<PlayerRights> displayedRights = new ArrayList<>();
	private long aLong1697;
	public NpcDefinition desc;
	boolean aBoolean1699;
	final int[] anIntArray1700;
	public int team;
	private int anInt1702;
	public String displayName;
	static MRUNodes mruNodes = new MRUNodes(260);
	public int combatLevel;
	public int headIcon;
	public int skullIcon;
	public int hintIcon;
	public int anInt1707;
	int anInt1708;
	int anInt1709;
	boolean visible;
	int anInt1711;
	int anInt1712;
	int anInt1713;
	Model aModel_1714;
	public final int[] equipment;
	private long aLong1718;
	int anInt1719;
	int anInt1720;
	int anInt1721;
	int anInt1722;
	int skill;
	private int healthState;

}
