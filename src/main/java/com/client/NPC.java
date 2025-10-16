package com.client;

import com.client.definitions.AnimationDefinition;
import com.client.definitions.NpcDefinition;
// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
import com.client.definitions.GraphicsDefinition;
import com.client.features.settings.Preferences;

public final class NPC extends Entity {

	private Model method450() {
		try {
			if (super.primaryanim >= 0 && super.primaryanim_pause == 0) {
				int current_index = super.primaryanim_frameindex;
				int next_index = -1;
				if (super.secondaryanim >= 0 && super.secondaryanim != super.readyanim)
					next_index = super.secondaryanim_frameindex;
				return desc.method164(current_index, AnimationDefinition.anims[super.primaryanim],
						AnimationDefinition.anims[super.primaryanim].mergedseqgroups, next_index, AnimationDefinition.anims[super.secondaryanim]);
			}
			int walk_index = -1;
			if (super.secondaryanim >= 0)
				walk_index = super.secondaryanim_frameindex;
			return desc.method164(walk_index, AnimationDefinition.anims[super.secondaryanim], null, -1, null);
		} catch (ArrayIndexOutOfBoundsException e) {
			//e.printStackTrace();
			return handleNonSkeletalNPCs();
		}
	}
	private Model handleNonSkeletalNPCs() {
		if (super.primaryanim >= 0 && super.primaryanim_pause == 0) {
			int k = super.primaryanim_frameindex;
			int i1 = -1;
			if (super.secondaryanim >= 0 && super.secondaryanim != super.readyanim)
				i1 = super.secondaryanim_frameindex;
			return desc.method166(i1, k,
					AnimationDefinition.anims[super.primaryanim].mergedseqgroups);
		}
		int l = -1;
		if (super.secondaryanim >= 0)
			l = super.secondaryanim_frameindex;
		return desc.method166(-1, l, null);
	}

	@Override
	public Model getRotatedModel() {
		if (desc == null)
			return null;
		Model model = method450();
		if (model == null)
			return null;
		super.height = model.modelHeight;
		if (super.spotanim != -1 && super.spotanimframe_index != -1) {
			GraphicsDefinition spotAnim = GraphicsDefinition.cache[super.spotanim];
			Model model_1 = spotAnim.get_transformed_model(super.spotanimframe_index);
			// Added
			if(model_1 != null) {
				model_1.offsetBy(0, -super.spotanim_height, 0);
				Model aModel[] = {model, model_1};
				model = new Model(aModel);
			}
		}
		if (desc.size == 1)
			model.singleTile = true;
		return model;
	}

	@Override
	public boolean isVisible() {
		return desc != null;
	}

	NPC() {
	}

	public boolean isShowMenuOnHover() {
		return npcPetType == 0 || npcPetType == 2 && !Preferences.getPreferences().hidePetOptions;
	}

	public int npcPetType;
	public NpcDefinition desc;
}
