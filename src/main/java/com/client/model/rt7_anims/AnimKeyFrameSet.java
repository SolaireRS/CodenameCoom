package com.client.model.rt7_anims;

import com.client.Buffer;
import com.client.AnimFrame;
import com.client.AnimBase;
import com.jagex.core.constants.SerialEnum;
import com.jagex.jagex3.math.Matrix4f;
import com.jagex.jagex3.math.Quaternion;

public class AnimKeyFrameSet {
   boolean modifies_trans;
   AnimationKeyFrame[][] skeletal_transforms = (AnimationKeyFrame[][])null;
   int frameset_id;// same as keyframe_id prob
   int keyframe_id = 0;
   public AnimationKeyFrame[][] transforms = (AnimationKeyFrame[][])null;
   public AnimBase base;


   public static AnimKeyFrameSet keyframesetset[];

   public AnimKeyFrameSet() {}
   public AnimKeyFrameSet get_keyframeset() {//should be only 1
      try {
         if(keyframesetset[frameset_id] == null) {
            AnimFrame.clientInstance.onDemandFetcher.provide(1, frameset_id);
            return null;
         }
         return keyframesetset[frameset_id];
      } catch(Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static AnimKeyFrameSet get_keyframeset(int skeletal_id) {
      try {
         String s = "";
         int file = skeletal_id >>> 16;
         int k = skeletal_id & 0xffff;// always 0 rn?
         if(keyframesetset[file] == null) {
            AnimFrame.clientInstance.onDemandFetcher.provide(1, file);
            return null;
         }
         return keyframesetset[file];
      } catch(Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static void init() {
      keyframesetset = new AnimKeyFrameSet[4000];
   }
   public static void load(int group, byte[] fileData){
      try {
         Buffer keyframe_buffer = new Buffer(fileData);
         if(keyframe_buffer.readUShort() != 420) {
            System.err.println("Not a keyframe file!");
         }
         int baseSize = keyframe_buffer.readInt();//need to write this
         byte[] base_data = new byte[baseSize];
         keyframe_buffer.readBytes(baseSize, 0, base_data);
         Buffer base_buffer = new Buffer(base_data);

         // Should always be 1?
         //int keyframesets_in_group = 1;//keyframe_buffer.get_unsignedbyte();//meed to write this

         int version = keyframe_buffer.readUnsignedByte();
         int base_id = keyframe_buffer.readUShort();
         System.out.println("Loading keyframeset " + group + ", base_id " + base_id + ", version " + version);
         AnimKeyFrameSet keyframe = keyframesetset[group] = new AnimKeyFrameSet();
         keyframe.frameset_id = group;
         try {
            keyframe.base = new AnimBase(base_buffer, false, baseSize);
         } catch (RuntimeException e) {
            keyframesetset[group] = null;
            System.err.println(e.getMessage());
            System.err.println("Error1 unpacking base for keyframe " + group);
            e.printStackTrace();
         }
         try {
            keyframe.decode(keyframe_buffer, version);
         } catch(RuntimeException exception) {
            keyframesetset[group] = null;
            System.err.println("Error1 unpacking keyframes " + group + " file size from cache = " + fileData.length);
            exception.printStackTrace();
         }
      } catch(Exception exception) {
         System.err.println("Error2 unpacking keyframes " + group);
         exception.printStackTrace();
      }
   }


   void decode(Buffer packet, int version) {
      int frame_size = packet.readInt();
      int before_read = packet.currentOffset;
      packet.readUShort();//starttick
      packet.readUShort();//endtick
      this.keyframe_id = packet.readUnsignedByte();//keyframe
      int var4 = packet.readUShort();
      this.skeletal_transforms = new AnimationKeyFrame[this.base.get_skeletal_animbase().get_num_bones()][];
      this.transforms = new AnimationKeyFrame[this.base.transforms_count()][];

      for(int var5 = 0; var5 < var4; ++var5) {
         int var7 = packet.readUnsignedByte();
         AnimTransform[] var8 = new AnimTransform[]{AnimTransform.NULL, AnimTransform.VERTEX, AnimTransform.field1210, AnimTransform.COLOUR, AnimTransform.TRANSPARENCY, AnimTransform.field1213};
         AnimTransform var9 = (AnimTransform) SerialEnum.for_id(var8, var7);
         if (var9 == null) {
            var9 = AnimTransform.NULL;
         }

         int var14 = packet.get_smart_byteorshort();
         AnimationChannel var10 = AnimationChannel.lookup_by_id(packet.readUnsignedByte());
         AnimationKeyFrame var11 = new AnimationKeyFrame();
         var11.deserialise(packet, version);
         int count = var9.get_dimensions();
         AnimationKeyFrame[][] transforms;
         if (AnimTransform.VERTEX == var9) {
            transforms = this.skeletal_transforms;
         } else {
            transforms = this.transforms;
         }

         if (transforms[var14] == null) {
            transforms[var14] = new AnimationKeyFrame[count];
         }

         transforms[var14][var10.get_component()] = var11;
         if (AnimTransform.TRANSPARENCY == var9) {
            this.modifies_trans = true;
         }
      }
      int read_size = packet.currentOffset - before_read;

      if(read_size != frame_size) {
         throw new RuntimeException("AnimKeyFrameSet size mismatch! keyframe " + keyframe_id + ", frame size: " + frame_size + ", actual read: " + read_size);

      }

   }

   public int get_keyframeid() {
      return this.keyframe_id;
   }

   public boolean modifies_alpha() {
      return this.modifies_trans;
   }

   public void apply_transforms(int tick, AnimationBone bone, int transform_index, int keyframe) {
      Matrix4f bone_matrix = Matrix4f.take();
      this.apply_rotation_transforms(bone_matrix, transform_index, bone, tick);
      this.apply_scale_transforms(bone_matrix, transform_index, bone, tick);
      this.apply_translation_transforms(bone_matrix, transform_index, bone, tick);
      bone.set_bone_transform(bone_matrix);
      bone_matrix.release();
   }

   void apply_rotation_transforms(Matrix4f trans_matrix, int transform_index, AnimationBone bone, int tick) {
      float[] rotations = bone.get_rotation(this.keyframe_id);
      float x = rotations[0];
      float y = rotations[1];
      float z = rotations[2];
      if (null != this.skeletal_transforms[transform_index]) {
         AnimationKeyFrame xrot_transform = this.skeletal_transforms[transform_index][0];
         AnimationKeyFrame yrot_transform = this.skeletal_transforms[transform_index][1];
         AnimationKeyFrame zrot_transform = this.skeletal_transforms[transform_index][2];
         if (xrot_transform != null) {
            x = xrot_transform.get_value(tick);
         }

         if (yrot_transform != null) {
            y = yrot_transform.get_value(tick);
         }

         if (zrot_transform != null) {
            z = zrot_transform.get_value(tick);
         }
      }

      Quaternion xrot = Quaternion.get();
      xrot.rotate(1.0F, 0.0F, 0.0F, x);
      Quaternion yrot = Quaternion.get();
      yrot.rotate(0.0F, 1.0F, 0.0F, y);
      Quaternion zrot = Quaternion.get();
      zrot.rotate(0.0F, 0.0F, 1.0F, z);
      Quaternion var13 = Quaternion.get();
      var13.mul(zrot);
      var13.mul(xrot);
      var13.mul(yrot);
      Matrix4f var14 = Matrix4f.take();
      var14.set_rotation(var13);
      trans_matrix.mul(var14);
      xrot.release();
      yrot.release();
      zrot.release();
      var13.release();
      var14.release();
   }

   void apply_translation_transforms(Matrix4f trans_matrix, int transform_index, AnimationBone bone, int tick) {
      float[] var6 = bone.get_translation(this.keyframe_id);
      float var7 = var6[0];
      float var8 = var6[1];
      float var9 = var6[2];
      if (this.skeletal_transforms[transform_index] != null) {
         AnimationKeyFrame var10 = this.skeletal_transforms[transform_index][3];
         AnimationKeyFrame var11 = this.skeletal_transforms[transform_index][4];
         AnimationKeyFrame var12 = this.skeletal_transforms[transform_index][5];
         if (var10 != null) {
            var7 = var10.get_value(tick);
         }

         if (null != var11) {
            var8 = var11.get_value(tick);
         }

         if (null != var12) {
            var9 = var12.get_value(tick);
         }
      }

      trans_matrix.values[12] = var7;
      trans_matrix.values[13] = var8;
      trans_matrix.values[14] = var9;
   }

   void apply_scale_transforms(Matrix4f transform_matrix, int transform_index, AnimationBone bone, int tick) {
      float[] var6 = bone.get_scale(this.keyframe_id);
      float var7 = var6[0];
      float var8 = var6[1];
      float var9 = var6[2];
      if (this.skeletal_transforms[transform_index] != null) {
         AnimationKeyFrame xscale_op = this.skeletal_transforms[transform_index][6];
         AnimationKeyFrame yscale_op = this.skeletal_transforms[transform_index][7];
         AnimationKeyFrame zscale_op = this.skeletal_transforms[transform_index][8];
         if (xscale_op != null) {
            var7 = xscale_op.get_value(tick);
         }

         if (yscale_op != null) {
            var8 = yscale_op.get_value(tick);
         }

         if (zscale_op != null) {
            var9 = zscale_op.get_value(tick);
         }
      }

      Matrix4f var13 = Matrix4f.take();
      var13.set_scale(var7, var8, var9);
      transform_matrix.mul(var13);
      var13.release();
   }

}
