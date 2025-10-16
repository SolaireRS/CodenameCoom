package com.client;

public class Renderable extends NodeSub {

    public boolean hidden = false;

    public void renderAtPoint(int i, int j, int k, int l, int i1, int j1, int k1,
                              int l1, long uid) {
        Model model = getRotatedModel();
        if (model != null) {
            modelHeight = model.modelHeight;
            model.renderAtPoint(i, j, k, l, i1, j1, k1, l1, uid);
        }
    }

    Model getRotatedModel() {
        return null;
    }

    Renderable() {
        modelHeight = 1000;
    }

    VertexNormal normals[];
    public int modelHeight; // modelHeight
}
