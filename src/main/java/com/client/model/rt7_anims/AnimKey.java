package com.client.model.rt7_anims;

import com.client.Buffer;

public class AnimKey {
   AnimKey next;
   float value;
   float start_val2 = Float.MAX_VALUE;
   float start_val1 = Float.MAX_VALUE;
   float end_val1 = Float.MAX_VALUE;
   float end_val2 = Float.MAX_VALUE;
   int tick;

   AnimKey() {
   }

   void deserialise(Buffer arg0, int version) {
      this.tick = arg0.get_short();
      this.value = arg0.get_float();
      this.start_val1 = arg0.get_float();
      this.start_val2 = arg0.get_float();
      this.end_val1 = arg0.get_float();
      this.end_val2 = arg0.get_float();
   }
}
