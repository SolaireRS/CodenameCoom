package com.client;

import com.client.definitions.AnimationDefinition;
import com.client.model.rt7_anims.AnimationBone;
import com.client.model.rt7_anims.AnimationKeyFrame;
import com.client.model.rt7_anims.AnimKeyFrameSet;
import com.client.model.rt7_anims.SkeletalAnimBase;
import com.jagex.jagex3.math.Matrix4f;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;


public class Model extends Renderable {

    private static Set<Integer> failedModels = new HashSet<>();

    public static void clear() {
        modelHeaders = null;
        hasAnEdgeToRestrict = null;
        outOfReach = null;
        vertexScreenY = null;
        vertexScreenZ = null;
        vertexMovedX = null;
        vertexMovedY = null;
        vertexMovedZ = null;
        depth = null;
        faceLists = null;
        anIntArray1673 = null;
        anIntArrayArray1674 = null;
        anIntArray1675 = null;
        anIntArray1676 = null;
        anIntArray1677 = null;
        SINE = null;
        COSINE = null;
        modelColors = null;
        modelLocations = null;
    }

    public final int[] models662 = {60157,60158,60159,60160,60156,60154,60155,60153,60152,60148,60149,60150,60151,60147,60146,64093,60130,60131,60132,60133,60134,60135,60136,60137,60138,60139,60140,60141,60142,60143,60144,64985,63065,61393,62398,63389,63609,64952,64955,64995,64956,62115,64199,62744,61776,64813,59609,59612,62302,60980,63999,59150,63615,63050,65258,63230,63231,65233,65234,65235,65236,65237,65238,65239,65240,65241,63559,65242,65243,65244,65245,65246,63252,63253,63254,60599,62792,61183,61185,63119,61275,61276,61190,61187,60281,63991,61182,61828,61528,61539,63119,61276,61187,62753,62730,61182,61275,61185,60281,61278,61279,61182,61183,61184,61181,61280,65991,69828,69528,69539,65753,65730,64116,62138,62125,61287,61281,60672,60575,60574,60562,60561,60563,64091,64092,59811,59810,59809,60777,60776,65278,65231,65232,58550,58559,58555,59995,61558,60959,64890,62302,59610,63610,63608,59859,60301,61011,51847,63604,44733,59808,58905,65705,66259,66260,67800,61059,59988,63999,61181,53166,65544,65539,65449,65444,
            65442,65335,61008,55594,65519,29981,10430,45248,45262,44613,65342,51285,51848,62736,62704,62742,56294,40939,
            46700,51285,51852,52948,55404,59860,59861,59988,60377,60639,62231,62693,62733,62739,62741,62744,62900,62901,
            62902,62905,63335,23892
    };

    public final int[] resizeModels = {60157,60158,60159,60160,60156,60154,60155,60153,60152,60148,60149,60150,60151,60147,60146,60145,64093,60130,60131,60132,60133,60134,60135,60136,60137,60138,60139,60140,60141,60142,60143,60144,61393,62398,63389,62115,64199,61776,62730,61183,63119,61190,61187,61275,61276,61185,60281,61182,61184,61279,61181,61828,63991,61528,61539,64116,62753,62138,65753,61287,61281,62125,60672,60574,60575,60561,60562,60563,64091,64092,59811,59810,59809,59610,59808,55594,62710,62736,62744,40939,62741,62739,62693,62695,62736,62748,62742,56294,
            62709,62704,62700,30476,10430,65519
    };
    public static final int LOCAL_COORD_BITS = 7;
    public static final int LOCAL_TILE_SIZE = 1 << LOCAL_COORD_BITS; // 128 - size of a tile in local coordinates
    public static final int LOCAL_HALF_TILE_SIZE = LOCAL_TILE_SIZE / 2;


    private Model(int modelId) {
        this.vertex_count = 0;
        this.face_count = 0;
        this.facePriority = 0;
        this.isBoundsCalculated = false;

        byte[] data = modelHeaders[modelId].data;
        if (data[data.length - 1] == -3 && data[data.length - 2] == -1) {
            ModelLoader.decodeType3(this, data);
        } else if (data[data.length - 1] == -2 && data[data.length - 2] == -1) {
            ModelLoader.decodeType2(this, data);
        } else if (IntStream.of(models662).anyMatch(x -> x == modelId)) {
            if (data[data.length - 1] == -1 && data[data.length - 2] == -1) {
                decode662(data, modelId);
            } else {
                ModelLoader.decodeOldFormat(this, data);
            }
        } else if (data[data.length - 1] == -2 && data[data.length - 2] == -1) {
            ModelLoader.decodeType2(this, data);
        } else if (data[data.length - 1] == -1 && data[data.length - 2] == -1) {
            ModelLoader.decodeType1(this, data);
        } else {
            ModelLoader.decodeOldFormat(this, data);
        }

        if (IntStream.of(resizeModels).anyMatch(x -> x == modelId)) {
            scale(4);
            if (renderPriorities != null) {
                Arrays.fill(renderPriorities, (byte) 10);
            }
        }


    }
    public static void loadModel(final byte[] modelData, final int modelId) {
        if (modelData == null) {
            final ModelHeader modelHeader = modelHeaders[modelId] = new ModelHeader();
            modelHeader.vertexCount = 0;
            modelHeader.triangleCount = 0;
            modelHeader.texturedTriangleCount = 0;
            return;
        }
        final Buffer stream = new Buffer(modelData);
        stream.currentOffset = modelData.length - 18;
        final ModelHeader modelHeader = modelHeaders[modelId] = new ModelHeader();
        modelHeader.data = modelData;
        modelHeader.vertexCount = stream.readUShort();
        modelHeader.triangleCount = stream.readUShort();
        modelHeader.texturedTriangleCount = stream.get_unsignedbyte();
        final int useTextures = stream.get_unsignedbyte();
        final int useTrianglePriority = stream.get_unsignedbyte();
        final int useAlpha = stream.get_unsignedbyte();
        final int useTriangleSkins = stream.get_unsignedbyte();
        final int useVertexSkins = stream.get_unsignedbyte();
        final int dataLengthX = stream.readUShort();
        final int dataLengthY = stream.readUShort();
        final int dataLengthZ = stream.readUShort();
        final int dataLengthTriangle = stream.readUShort();
        int offset = 0;
        modelHeader.vertexDirectionOffset = offset;
        offset += modelHeader.vertexCount;
        modelHeader.triangleTypeOffset = offset;
        offset += modelHeader.triangleCount;
        modelHeader.trianglePriorityOffset = offset;
        if (useTrianglePriority == 255) {
            offset += modelHeader.triangleCount;
        } else {
            modelHeader.trianglePriorityOffset = -useTrianglePriority - 1;
        }
        modelHeader.triangleSkinOffset = offset;
        if (useTriangleSkins == 1) {
            offset += modelHeader.triangleCount;
        } else {
            modelHeader.triangleSkinOffset = -1;
        }
        modelHeader.texturePointerOffset = offset;
        if (useTextures == 1) {
            offset += modelHeader.triangleCount;
        } else {
            modelHeader.texturePointerOffset = -1;
        }
        modelHeader.vertexSkinOffset = offset;
        if (useVertexSkins == 1) {
            offset += modelHeader.vertexCount;
        } else {
            modelHeader.vertexSkinOffset = -1;
        }
        modelHeader.triangleAlphaOffset = offset;
        if (useAlpha == 1) {
            offset += modelHeader.triangleCount;
        } else {
            modelHeader.triangleAlphaOffset = -1;
        }
        modelHeader.triangleDataOffset = offset;
        offset += dataLengthTriangle;
        modelHeader.colourDataOffset = offset;
        offset += modelHeader.triangleCount * 2;
        modelHeader.texturedTriangleOffset = offset;
        offset += modelHeader.texturedTriangleCount * 6;
        modelHeader.dataOffsetX = offset;
        offset += dataLengthX;
        modelHeader.dataOffsetY = offset;
        offset += dataLengthY;
        modelHeader.dataOffsetZ = offset;
        offset += dataLengthZ;
    }

    public static void init() {
        modelHeaders = new ModelHeader[90000];
    }

    public static void resetModel(final int model) {
        modelHeaders[model] = null;
    }
    public static Model getModel(int file) {
        if (modelHeaders == null) {
            return null;
        }

        ModelHeader class21 = modelHeaders[file];
        if (class21 == null) {
            // Check if we've already tried and failed to load this model
            if (failedModels.contains(file)) {
                return null; // Don't spam the fetcher
            }

            Client.instance.onDemandFetcher.provide(0, file);
            System.out.println("ModelId: " + file);

            // DON'T check immediately - the fetcher needs time to load
            // Just return null and let the game try again next frame
            return null;
        } else {
            return new Model(file);
        }
    }

    public static boolean isCached(int file) {
        if (modelHeaders == null) {
            return false;
        }

        ModelHeader class21 = modelHeaders[file];
        if (class21 == null) {
            Client.instance.onDemandFetcher.provide(0, file);
            return false;
        } else {
            return true;
        }
    }

    Model() {
        vertex_count = 0;
        face_count = 0;
        texture_count = 0;
        facePriority = 0;
        singleTile = true;
        xMidOffset = -1;
        yMidOffset = -1;
        zMidOffset = -1;
        this.isBoundsCalculated = false;
    }
    public void computeTextureUvCoordinates()
    {
        final short[] faceTextures = getFaceTextures();
        if (faceTextures == null)
        {
            return;
        }

        final int[] vertexPositionsX = getVertexNormalsX();
        final int[] vertexPositionsY = getVertexNormalsY();
        final int[] vertexPositionsZ = getVertexNormalsZ();

        final int[] trianglePointsX = getFaceIndices1();
        final int[] trianglePointsY = getFaceIndices2();
        final int[] trianglePointsZ = getFaceIndices3();

        final short[] texTriangleX = textures_mapping_p;
        final short[] texTriangleY = textures_mapping_m;
        final short[] texTriangleZ = textures_mapping_n;

        final byte[] textureCoords = textures;

        int faceCount = face_count;
        float[] faceTextureUCoordinates = new float[faceCount * 6];

        for (int i = 0; i < faceCount; i++)
        {
            int trianglePointX = trianglePointsX[i];
            int trianglePointY = trianglePointsY[i];
            int trianglePointZ = trianglePointsZ[i];

            short textureIdx = faceTextures[i];

            if (textureIdx != -1)
            {
                int triangleVertexIdx1;
                int triangleVertexIdx2;
                int triangleVertexIdx3;

                if (textureCoords != null && textureCoords[i] != -1)
                {
                    int textureCoordinate = textureCoords[i] & 255;
                    triangleVertexIdx1 = texTriangleX[textureCoordinate];
                    triangleVertexIdx2 = texTriangleY[textureCoordinate];
                    triangleVertexIdx3 = texTriangleZ[textureCoordinate];
                }
                else
                {
                    triangleVertexIdx1 = trianglePointX;
                    triangleVertexIdx2 = trianglePointY;
                    triangleVertexIdx3 = trianglePointZ;
                }

                float triangleX = (float) vertexPositionsX[triangleVertexIdx1];
                float triangleY = (float) vertexPositionsY[triangleVertexIdx1];
                float triangleZ = (float) vertexPositionsZ[triangleVertexIdx1];

                float f_882_ = (float) vertexPositionsX[triangleVertexIdx2] - triangleX;
                float f_883_ = (float) vertexPositionsY[triangleVertexIdx2] - triangleY;
                float f_884_ = (float) vertexPositionsZ[triangleVertexIdx2] - triangleZ;
                float f_885_ = (float) vertexPositionsX[triangleVertexIdx3] - triangleX;
                float f_886_ = (float) vertexPositionsY[triangleVertexIdx3] - triangleY;
                float f_887_ = (float) vertexPositionsZ[triangleVertexIdx3] - triangleZ;
                float f_888_ = (float) vertexPositionsX[trianglePointX] - triangleX;
                float f_889_ = (float) vertexPositionsY[trianglePointX] - triangleY;
                float f_890_ = (float) vertexPositionsZ[trianglePointX] - triangleZ;
                float f_891_ = (float) vertexPositionsX[trianglePointY] - triangleX;
                float f_892_ = (float) vertexPositionsY[trianglePointY] - triangleY;
                float f_893_ = (float) vertexPositionsZ[trianglePointY] - triangleZ;
                float f_894_ = (float) vertexPositionsX[trianglePointZ] - triangleX;
                float f_895_ = (float) vertexPositionsY[trianglePointZ] - triangleY;
                float f_896_ = (float) vertexPositionsZ[trianglePointZ] - triangleZ;

                float f_897_ = f_883_ * f_887_ - f_884_ * f_886_;
                float f_898_ = f_884_ * f_885_ - f_882_ * f_887_;
                float f_899_ = f_882_ * f_886_ - f_883_ * f_885_;
                float f_900_ = f_886_ * f_899_ - f_887_ * f_898_;
                float f_901_ = f_887_ * f_897_ - f_885_ * f_899_;
                float f_902_ = f_885_ * f_898_ - f_886_ * f_897_;
                float f_903_ = 1.0F / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_);

                float u0 = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
                float u1 = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
                float u2 = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;

                f_900_ = f_883_ * f_899_ - f_884_ * f_898_;
                f_901_ = f_884_ * f_897_ - f_882_ * f_899_;
                f_902_ = f_882_ * f_898_ - f_883_ * f_897_;
                f_903_ = 1.0F / (f_900_ * f_885_ + f_901_ * f_886_ + f_902_ * f_887_);

                float v0 = (f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_) * f_903_;
                float v1 = (f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_) * f_903_;
                float v2 = (f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_) * f_903_;

                int idx = i * 6;
                faceTextureUCoordinates[idx] = u0;
                faceTextureUCoordinates[idx + 1] = v0;
                faceTextureUCoordinates[idx + 2] = u1;
                faceTextureUCoordinates[idx + 3] = v1;
                faceTextureUCoordinates[idx + 4] = u2;
                faceTextureUCoordinates[idx + 5] = v2;
            }
        }

        faceTextureUVCoordinates = faceTextureUCoordinates;
    }
    public int[] getVertexNormalsZ() {
        if(vertexNormalsZ == null)
            return getVerticesZ();
        return vertexNormalsZ;
    }
    public int[] getVertexNormalsY() {
        if(vertexNormalsY == null)
            return getVerticesY();
        return vertexNormalsY;
    }
    public int[] getVertexNormalsX() {
        if(vertexNormalsX == null)
            return getVerticesX();
        return vertexNormalsX;
    }
    public int[] getVerticesX() {
        return verticesX;
    }

    public int[] getVerticesY() {
        return verticesY;
    }

    public int[] getVerticesZ() {
        return verticesZ;
    }
    public Model(int length, Model[] models) {
        try {
            singleTile = false;
            boolean typeFlag = false;
            boolean priorityFlag = false;
            boolean alphaFlag = false;
            boolean tSkinFlag = false;
            boolean bonesFlag = false;
            boolean textureFlag = false;
            boolean coordinateFlag = false;
            vertex_count = 0;
            face_count = 0;
            texture_count = 0;
            facePriority = -1;
            xMidOffset = -1;
            yMidOffset = -1;
            zMidOffset = -1;
            Model build;
            for (int count = 0; count < length; count++) {
                build = models[count];
                if (build != null) {
                    vertex_count += build.vertex_count;
                    face_count += build.face_count;
                    texture_count += build.texture_count;
                    typeFlag |= build.drawType != null;
                    alphaFlag |= build.face_alphas != null;
                    if (build.renderPriorities != null) {
                        priorityFlag = true;
                    } else {
                        if (facePriority == -1)
                            facePriority = build.facePriority;

                        if (facePriority != build.facePriority)
                            priorityFlag = true;
                    }
                    tSkinFlag |= build.face_labels != null;
                    bonesFlag |= build.vertex_bone_origins != null;
                    textureFlag |= build.materials != null;
                    coordinateFlag |= build.textures != null;
                }
            }

            verticesX = new int[vertex_count];
            verticesY = new int[vertex_count];
            verticesZ = new int[vertex_count];
            vertex_labels = new int[vertex_count];
            trianglesX = new int[face_count];
            trianglesY = new int[face_count];
            trianglesZ = new int[face_count];

            if (typeFlag)
                drawType = new int[face_count];

            if (priorityFlag)
                renderPriorities = new byte[face_count];

            if (alphaFlag)
                face_alphas = new byte[face_count];

            if (tSkinFlag)
                face_labels = new int[face_count];

            if (textureFlag)
                materials = new short[face_count];

            if (coordinateFlag)
                textures = new byte[face_count];

            if (bonesFlag) {
                this.vertex_bone_origins = new int[this.vertex_count][];
                this.vertex_bone_scales = new int[this.vertex_count][];
            }

            colors = new short[face_count];
            if (texture_count > 0) {
                texture_mapping_type = new byte[texture_count];
                textures_mapping_p = new short[texture_count];
                textures_mapping_m = new short[texture_count];
                textures_mapping_n = new short[texture_count];
            }
            vertex_count = 0;
            face_count = 0;
            texture_count = 0;
            int textureFace = 0;
            for (int index = 0; index < length; index++) {
                build = models[index];
                if (build != null) {
                    for (int face = 0; face < build.face_count; face++) {
                        if (typeFlag && build.drawType != null)
                            drawType[face_count] = build.drawType[face];

                        if (priorityFlag)
                            if (build.renderPriorities == null)
                                renderPriorities[face_count] = build.facePriority;
                            else
                                renderPriorities[face_count] = build.renderPriorities[face];

                        if (alphaFlag && build.face_alphas != null)
                            face_alphas[face_count] = build.face_alphas[face];

                        if (tSkinFlag && build.face_labels != null)
                            face_labels[face_count] = build.face_labels[face];

                        if (textureFlag) {
                            if (build.materials != null)
                                materials[face_count] = build.materials[face];
                            else
                                materials[face_count] = -1;
                        }
                        if (coordinateFlag) {
                            if (build.textures != null && build.textures[face] != -1) {
                                textures[face_count] = (byte) (build.textures[face] + textureFace);
                            } else {
                                textures[face_count] = -1;
                            }
                        }

                        colors[face_count] = build.colors[face];
                        trianglesX[face_count] = getFirstIdenticalVertexId(build, build.trianglesX[face]);
                        trianglesY[face_count] = getFirstIdenticalVertexId(build, build.trianglesY[face]);
                        trianglesZ[face_count] = getFirstIdenticalVertexId(build, build.trianglesZ[face]);
                        face_count++;
                    }
                    for (int textureEdge = 0; textureEdge < build.texture_count; textureEdge++) {
                        textures_mapping_p[texture_count] = (short) getFirstIdenticalVertexId(build, build.textures_mapping_p[textureEdge]);
                        textures_mapping_m[texture_count] = (short) getFirstIdenticalVertexId(build, build.textures_mapping_m[textureEdge]);
                        textures_mapping_n[texture_count] = (short) getFirstIdenticalVertexId(build, build.textures_mapping_n[textureEdge]);
                        texture_count++;
                    }
                    textureFace += build.texture_count;
                }
            }
            if (getFaceTextures() != null)
            {
                int count = getFaceCount();
                float[] uv = new float[count * 6];
                int idx = 0;

                for (int i = 0; i < length; ++i)
                {
                    Model model = models[i];
                    if (model != null)
                    {
                        float[] modelUV = model.getFaceTextureUVCoordinates();

                        if (modelUV != null)
                        {
                            System.arraycopy(modelUV, 0, uv, idx, model.getFaceCount() * 6);
                        }

                        idx += model.getFaceCount() * 6;
                    }
                }

                setFaceTextureUVCoordinates(uv);
            }
            vertexNormals();
            this.resetBounds();
            invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public float[] getFaceTextureUVCoordinates() {
        if (faceTextureUVCoordinates == null) {
            computeTextureUvCoordinates();
        }
        return faceTextureUVCoordinates;
    }
    public void setFaceTextureUVCoordinates(float[] faceTextureUVCoordinates) {
        this.faceTextureUVCoordinates = faceTextureUVCoordinates;
    }
    public short[] getFaceTextures() {
        return materials;
    }

    public int getFaceCount() {
        return this.face_count;
    }

    public int[] getFaceIndices1() {
        return trianglesX;
    }

    public int[] getFaceIndices2() {
        return trianglesY;
    }

    public int[] getFaceIndices3() {
        return trianglesZ;
    }

    public int[] getFaceColors1() {
        return this.colorsX;
    }

    public int[] getFaceColors2() {
        return colorsY;
    }

    public int[] getFaceColors3() {
        return colorsZ;
    }

    public Model(Model models[]) {
        int modelCount = 2;
        singleTile = false;
        anInt1620++;
        boolean renderTypeFlag = false;
        boolean priorityFlag = false;
        boolean alphaFlag = false;
        boolean bonesFlag = false;
        boolean textureFlag = false;
        boolean coordinateFlag = false;
        vertex_count = 0;
        face_count = 0;
        face_count = 0;
        facePriority = -1;

        Model build;
        for (int currentModel = 0; currentModel < modelCount; currentModel++) {
            build = models[currentModel];
            if (build != null) {
                vertex_count += build.vertex_count;
                face_count += build.face_count;
                texture_count += build.texture_count;
                renderTypeFlag |= drawType != null;
                if (build.renderPriorities != null) {
                    priorityFlag = true;
                } else {
                    if (facePriority == -1)
                        facePriority = build.facePriority;

                    if (facePriority != build.facePriority)
                        priorityFlag = true;
                }
                alphaFlag |= build.face_alphas != null;
                bonesFlag |= build.vertex_bone_origins != null;
                textureFlag |= build.materials != null;
                coordinateFlag |= build.textures != null;
            }
        }

        verticesX = new int[vertex_count];
        verticesY = new int[vertex_count];
        verticesZ = new int[vertex_count];
        trianglesX = new int[face_count];
        trianglesY = new int[face_count];
        trianglesZ = new int[face_count];
        colorsX = new int[face_count];
        colorsY = new int[face_count];
        colorsZ = new int[face_count];
        textures_mapping_p = new short[texture_count];
        textures_mapping_m = new short[texture_count];
        textures_mapping_n = new short[texture_count];

        if (renderTypeFlag)
            drawType = new int[face_count];

        if (priorityFlag)
            renderPriorities = new byte[face_count];

        if (alphaFlag)
            face_alphas = new byte[face_count];

        if (textureFlag)
            materials = new short[face_count];

        if (coordinateFlag)
            textures = new byte[face_count];

        if (bonesFlag) {
            this.vertex_bone_origins = new int[this.vertex_count][];
            this.vertex_bone_scales = new int[this.vertex_count][];
        }
        colors = new short[face_count];
        if(texture_count > 0) {
            texture_mapping_type = new byte[texture_count];
            textures_mapping_p = new short[texture_count];
            textures_mapping_m = new short[texture_count];
            textures_mapping_n = new short[texture_count];
        }




        vertex_count = 0;
        face_count = 0;
        texture_count = 0;

        for (int currentModel = 0; currentModel < modelCount; currentModel++) {
            build = models[currentModel];
            if (build != null) {
                int vertex = vertex_count;
                for (int point = 0; point < build.vertex_count; point++) {
                    verticesX[vertex_count] = build.verticesX[point];
                    verticesY[vertex_count] = build.verticesY[point];
                    verticesZ[vertex_count] = build.verticesZ[point];
                    vertex_count++;
                }
                for (int face = 0; face < build.face_count; face++) {
                    trianglesX[face_count] = build.trianglesX[face] + vertex;
                    trianglesY[face_count] = build.trianglesY[face] + vertex;
                    trianglesZ[face_count] = build.trianglesZ[face] + vertex;
                    colorsX[face_count] = build.colorsX[face];
                    colorsY[face_count] = build.colorsY[face];
                    colorsZ[face_count] = build.colorsZ[face];

                    if(renderTypeFlag && build.drawType != null) {
                        drawType[face_count] = build.drawType[face];
                    }

                    if (alphaFlag && build.face_alphas != null) {
                        face_alphas[face_count] = build.face_alphas[face];
                    }

                    if (priorityFlag)
                        if (build.renderPriorities == null)
                            renderPriorities[face_count] = build.facePriority;
                        else
                            renderPriorities[face_count] = build.renderPriorities[face];

                    colors[face_count] = build.colors[face];

                    if(textureFlag) {
                        if(build.materials != null) {
                            materials[face_count] = build.materials[face];
                        } else
                            materials[face_count] = -1;
                    }
                    if(coordinateFlag) {
                        if(build.textures != null && build.textures[face] != -1) {
                            textures[face_count] = (byte) (build.textures[face] + texture_count);

                        } else
                            textures[face_count] = -1;

                    }

                    face_count++;
                }

                for (int texture = 0; texture < build.texture_count; texture++) {
                    textures_mapping_p[texture_count] = (short) (build.textures_mapping_p[texture] + vertex);
                    textures_mapping_m[texture_count] = (short) (build.textures_mapping_m[texture] + vertex);
                    textures_mapping_n[texture_count] = (short) (build.textures_mapping_n[texture] + vertex);
                    texture_count++;
                }
                texture_count += build.texture_count;
            }
        }
        calculateBoundsCylinder();
        this.resetBounds();
        invalidate();
    }

    public Model(boolean colorFlag, boolean alphaFlag, boolean animated, Model model) {
        this(colorFlag, alphaFlag, animated, false, model);
    }
    public Model bake_shared_animation_model(boolean alpha) {
        Model model_1 = Model.emptyModel;
        model_1.replaceModel(this, alpha);
        //Model new_model = new Model(true, alpha, false, this);
        model_1.apply_label_groups();
        return model_1;

    }
    public Model bake_shared_model(boolean alpha) {
        Model new_model = new Model(true, alpha, false, this);
        new_model.apply_label_groups();
        return new_model;

    }
    public Model(boolean colorFlag, boolean alphaFlag, boolean animated, boolean textureFlag, Model model) {
        singleTile = false;
        vertex_count = model.vertex_count;
        face_count = model.face_count;
        texture_count = model.texture_count;
        if (animated) {
            verticesX = model.verticesX;
            verticesY = model.verticesY;
            verticesZ = model.verticesZ;
        } else {
            verticesX = new int[vertex_count];
            verticesY = new int[vertex_count];
            verticesZ = new int[vertex_count];
            for (int j = 0; j < vertex_count; j++) {
                verticesX[j] = model.verticesX[j];
                verticesY[j] = model.verticesY[j];
                verticesZ[j] = model.verticesZ[j];
            }

        }
        if (colorFlag) {
            colors = model.colors;
        } else {
            colors = new short[face_count];
            System.arraycopy(model.colors, 0, colors, 0, face_count);
        }

        if (!textureFlag && model.materials != null) {
            materials = new short[face_count];
            System.arraycopy(model.materials, 0, materials, 0, face_count);
        } else {
            materials = model.materials;
        }

        if (alphaFlag) {
            face_alphas = model.face_alphas;
        } else {
            face_alphas = new byte[face_count];
            if (model.face_alphas == null) {
                for (int l = 0; l < face_count; l++) {
                    face_alphas[l] = 0;
                }

            } else {
                System.arraycopy(model.face_alphas, 0, face_alphas, 0, face_count);

            }
        }
        vertex_labels = model.vertex_labels;
        face_labels = model.face_labels;
        drawType = model.drawType;
        trianglesX = model.trianglesX;
        trianglesY = model.trianglesY;
        trianglesZ = model.trianglesZ;
        renderPriorities = model.renderPriorities;
        facePriority = model.facePriority;
        textures_mapping_p = model.textures_mapping_p;
        textures_mapping_m = model.textures_mapping_m;
        textures_mapping_n = model.textures_mapping_n;
        textures = model.textures;
        texture_mapping_type = model.texture_mapping_type;
        normals = model.normals;
        shared_normals = model.shared_normals;
        vertex_bone_scales = model.vertex_bone_scales;
        vertex_bone_origins = model.vertex_bone_origins;
    }


    public Model(boolean resetVertices, boolean resetColors, Model model) {
        singleTile = false;
        vertex_count = model.vertex_count;
        face_count = model.face_count;
        texture_count = model.texture_count;

        if (resetVertices) {
            verticesY = new int[vertex_count];
            System.arraycopy(model.verticesY, 0, verticesY, 0, vertex_count);
        } else {
            verticesY = model.verticesY;
        }

        if (resetColors) {
            colorsX = new int[face_count];
            colorsY = new int[face_count];
            colorsZ = new int[face_count];

            for (int k = 0; k < face_count; k++) {
                colorsX[k] = model.colorsX[k];
                colorsY[k] = model.colorsY[k];
                colorsZ[k] = model.colorsZ[k];
            }

            drawType = new int[face_count];
            if (model.drawType == null) {
                for (int l = 0; l < face_count; l++) {
                    drawType[l] = 0;
                }
            } else {
                System.arraycopy(model.drawType, 0, drawType, 0, face_count);
            }

        } else {
            colorsX = model.colorsX;
            colorsY = model.colorsY;
            colorsZ = model.colorsZ;
            drawType = model.drawType;
        }

        verticesX = model.verticesX;
        verticesZ = model.verticesZ;
        colors = model.colors;
        face_alphas = model.face_alphas;
        renderPriorities = model.renderPriorities;
        facePriority = model.facePriority;
        trianglesX = model.trianglesX;
        trianglesY = model.trianglesY;
        trianglesZ = model.trianglesZ;
        textures_mapping_p = model.textures_mapping_p;
        textures_mapping_m = model.textures_mapping_m;
        textures_mapping_n = model.textures_mapping_n;
        super.modelHeight = model.modelHeight;
        textures = model.textures;
        materials = model.materials;
        vertex_bone_origins = model.vertex_bone_origins;
        vertex_bone_scales = model.vertex_bone_scales;
        diagonal2DAboveOrigin = model.diagonal2DAboveOrigin;
        diagonal3DAboveOrigin = model.diagonal3DAboveOrigin;
        diagonal3D = model.diagonal3D;
        minX = model.minX;
        maxZ = model.maxZ;
        minZ = model.minZ;
        maxX = model.maxX;

        vertexNormalsX = model.vertexNormalsX;
        vertexNormalsY = model.vertexNormalsY;
        vertexNormalsZ = model.vertexNormalsZ;
        faceTextureUVCoordinates = model.faceTextureUVCoordinates;

    }
    AnimFrameSet frame_setref = null;
    public void animate(int frameId) {
        if (vertex_label_groups == null)
            return;

        if (frameId == -1)
            return;

        AnimFrame frame = frame_setref.frames[frameId];
        if (frame == null)
            return;

        AnimBase base = frame.base;
        transformTempX = 0;
        transformTempY = 0;
        transformTempZ = 0;
        for (int k = 0; k < frame.transformationCount; k++) {
            int l = frame.transformationIndices[k];
            apply_frame_anim_transformations(base.transform_types[l],
                    base.labels[l],
                    frame.transformX[k],
                    frame.transformY[k],
                    frame.transformZ[k]);
        }

        this.resetBounds();
        invalidate();
    }
    public void animate2(int[] label, int idle, int current) {
        if (current == -1)
            return;

        if (label == null || idle == -1) {
            animate(current);
            return;
        }

        AnimFrame anim = frame_setref.frames[current];
        if (anim == null)
            return;

        AnimFrame skin =  frame_setref.frames[idle];
        if (skin == null) {
            animate(current);
            return;
        }
        AnimBase list = anim.base;
        transformTempX = 0;
        transformTempY = 0;
        transformTempZ = 0;
        int id = 0;
        int table = label[id++];
        for (int index = 0; index < anim.transformationCount; index++) {
            int condition;
            for (condition = anim.transformationIndices[index]; condition > table; table = label[id++]) {
            }

            if (condition != table || list.transform_types[condition] == 0) {
                this.apply_frame_anim_transformations(list.transform_types[condition], list.labels[condition], anim.transformX[index], anim.transformY[index], anim.transformZ[index]);
            }
        }

        transformTempX = 0;
        transformTempY = 0;
        transformTempZ = 0;
        id = 0;
        table = label[id++];
        for (int index = 0; index < skin.transformationCount; index++) {
            int condition;
            for (condition = skin.transformationIndices[index]; condition > table; table = label[id++]) {
            }

            if (condition == table || list.transform_types[condition] == 0) {
                this.apply_frame_anim_transformations(list.transform_types[condition], list.labels[condition], skin.transformX[index], skin.transformY[index], skin.transformZ[index]);
            }
        }

        this.resetBounds();
        invalidate();
    }
    public void replaceModel(Model model, boolean replaceAlpha) {
        vertex_count = model.vertex_count;
        face_count = model.face_count;
        texture_count = model.texture_count;

        if (sharedVerticesX.length < vertex_count) {
            sharedVerticesX = new int[vertex_count + 100];
            sharedVerticesY = new int[vertex_count + 100];
            sharedVerticesZ = new int[vertex_count + 100];
        }

        verticesX = sharedVerticesX;
        verticesY = sharedVerticesY;
        verticesZ = sharedVerticesZ;
        for (int k = 0; k < vertex_count; k++) {
            verticesX[k] = model.verticesX[k];
            verticesY[k] = model.verticesY[k];
            verticesZ[k] = model.verticesZ[k];
        }

        if (replaceAlpha) {
            face_alphas = model.face_alphas;
        } else {
            if (sharedTriangleAlpha.length < face_count) {
                sharedTriangleAlpha = new byte[face_count + 100];
            }
            face_alphas = sharedTriangleAlpha;
            if (model.face_alphas == null) {
                for (int l = 0; l < face_count; l++) {
                    face_alphas[l] = 0;
                }
            } else {
                System.arraycopy(model.face_alphas, 0, face_alphas, 0, face_count);
            }
        }

        drawType = model.drawType;
        colors = model.colors;
        renderPriorities = model.renderPriorities;
        facePriority = model.facePriority;
        face_label_groups = model.face_label_groups;
        vertex_label_groups = model.vertex_label_groups;
        trianglesX = model.trianglesX;
        trianglesY = model.trianglesY;
        trianglesZ = model.trianglesZ;
        colorsX = model.colorsX;
        colorsY = model.colorsY;
        colorsZ = model.colorsZ;
        textures_mapping_p = model.textures_mapping_p;
        textures_mapping_m = model.textures_mapping_m;
        textures_mapping_n = model.textures_mapping_n;
        textures = model.textures;
        texture_mapping_type = model.texture_mapping_type;
        materials = model.materials;
        vertex_bone_origins = model.vertex_bone_origins;
        vertex_bone_scales = model.vertex_bone_scales;
        shared_normals = model.shared_normals;

        vertexNormalsX = model.vertexNormalsX;
        vertexNormalsY = model.vertexNormalsY;
        vertexNormalsZ = model.vertexNormalsZ;
        faceTextureUVCoordinates = model.faceTextureUVCoordinates;

        model.resetBounds();
    }

    private int getFirstIdenticalVertexId(final Model model, final int vertex) {
        int vertexId = -1;
        final int x = model.verticesX[vertex];
        final int y = model.verticesY[vertex];
        final int z = model.verticesZ[vertex];
        for (int v = 0; v < this.vertex_count; v++) {
            if (x != this.verticesX[v] || y != this.verticesY[v] || z != this.verticesZ[v]) {
                continue;
            }
            vertexId = v;
            break;
        }

        if (vertexId == -1) {
            this.verticesX[this.vertex_count] = x;
            this.verticesY[this.vertex_count] = y;
            this.verticesZ[this.vertex_count] = z;
            if (model.vertex_labels != null) {
                this.vertex_labels[this.vertex_count] = model.vertex_labels[vertex];
            }
            if (model.vertex_bone_origins != null) {
                this.vertex_bone_origins[this.vertex_count] = model.vertex_bone_origins[vertex];
                this.vertex_bone_scales[this.vertex_count] = model.vertex_bone_scales[vertex];
            }
            vertexId = this.vertex_count++;

        }
        return vertexId;
    }

    private int boundsType;

    public void calculateBoundsCylinder() {
        if (this.boundsType != 1) {
            this.boundsType = 1;
            super.modelHeight = 0;
            diagonal2DAboveOrigin = 0;
            maxY = 0;
            for (int vertex = 0; vertex < vertex_count; vertex++) {
                final int x = verticesX[vertex];
                final int y = verticesY[vertex];
                final int z = verticesZ[vertex];
                if (-y > super.modelHeight) {
                    super.modelHeight = -y;
                }
                if (y > maxY) {
                    maxY = y;
                }
                final int bounds = x * x + z * z;
                if (bounds > diagonal2DAboveOrigin) {
                    diagonal2DAboveOrigin = bounds;
                }
            }


            diagonal2DAboveOrigin = (int)(Math.sqrt(diagonal2DAboveOrigin) + 0.98999999999999999);
            diagonal3DAboveOrigin = (int)(Math.sqrt(diagonal2DAboveOrigin * diagonal2DAboveOrigin + super.modelHeight * super.modelHeight) + 0.98999999999999999);
            diagonal3D = diagonal3DAboveOrigin + (int)(Math.sqrt(diagonal2DAboveOrigin * diagonal2DAboveOrigin + maxY * maxY) + 0.98999999999999999);
        }
    }

    void calculateDiagonals() {
        if (this.boundsType != 2) {
            this.boundsType = 2;
            this.diagonal2DAboveOrigin = 0;

            for (int count = 0; count < this.vertex_count; ++count) {
                int x = this.verticesX[count];
                int y = this.verticesY[count];
                int z = this.verticesZ[count];
                int bounds = x * x + z * z + y * y;
                if (bounds > this.diagonal2DAboveOrigin) {
                    this.diagonal2DAboveOrigin = bounds;
                }
            }

            this.diagonal2DAboveOrigin = (int)(Math.sqrt((double)this.diagonal2DAboveOrigin) + 0.99D);
            this.diagonal3DAboveOrigin = this.diagonal2DAboveOrigin;
            this.diagonal3D = this.diagonal2DAboveOrigin + this.diagonal2DAboveOrigin;
        }
    }

    public void normalise() {
        super.modelHeight = 0;
        maxY = 0;
        for (int vertex = 0; vertex < vertex_count; vertex++) {
            final int y = verticesY[vertex];
            if (-y > super.modelHeight) {
                super.modelHeight = -y;
            }
            if (y > maxY) {
                maxY = y;
            }
        }

        this.diagonal3DAboveOrigin = (int)(Math.sqrt(diagonal2DAboveOrigin * diagonal2DAboveOrigin + super.modelHeight * super.modelHeight) + 0.98999999999999999);
        this.diagonal3D = diagonal3DAboveOrigin + (int)(Math.sqrt(diagonal2DAboveOrigin * diagonal2DAboveOrigin + maxY * maxY) + 0.98999999999999999);
    }

    public void calculateBounds() {
        if (!this.isBoundsCalculated) {
            super.modelHeight = 0;
            diagonal2DAboveOrigin = 0;
            maxY = 0;
            minX = 0xf423f;
            maxX = 0xfff0bdc1;
            maxZ = 0xfffe7961;
            minZ = 0x1869f;
            for (int vertex = 0; vertex < vertex_count; vertex++) {
                final int x = verticesX[vertex];
                final int y = verticesY[vertex];
                final int z = verticesZ[vertex];
                if (x < minX) {
                    minX = x;
                }
                if (x > maxX) {
                    maxX = x;
                }
                if (z < minZ) {
                    minZ = z;
                }
                if (z > maxZ) {
                    maxZ = z;
                }
                if (-y > super.modelHeight) {
                    super.modelHeight = -y;
                }
                if (y > maxY) {
                    maxY = y;
                }
                final int bounds = x * x + z * z;
                if (bounds > diagonal2DAboveOrigin) {
                    diagonal2DAboveOrigin = bounds;
                }
            }
            this.isBoundsCalculated = true;
        }
    }

    public void apply_label_groups() {
        if (vertex_labels != null) {
            int ai[] = new int[256];
            int j = 0;
            for (int l = 0; l < vertex_count; l++) {
                int j1 = vertex_labels[l];
                ai[j1]++;
                if (j1 > j)
                    j = j1;
            }
            vertex_label_groups = new int[j + 1][];
            for (int k1 = 0; k1 <= j; k1++) {
                vertex_label_groups[k1] = new int[ai[k1]];
                ai[k1] = 0;
            }
            for (int j2 = 0; j2 < vertex_count; j2++) {
                int l2 = vertex_labels[j2];
                vertex_label_groups[l2][ai[l2]++] = j2;
            }
            vertex_labels = null;
        }
        if (face_labels != null) {
            int ai1[] = new int[256];
            int k = 0;
            for (int i1 = 0; i1 < face_count; i1++) {
                int l1 = face_labels[i1];
                ai1[l1]++;
                if (l1 > k)
                    k = l1;
            }
            face_label_groups = new int[k + 1][];
            for (int uid = 0; uid <= k; uid++) {
                face_label_groups[uid] = new int[ai1[uid]];
                ai1[uid] = 0;
            }
            for (int k2 = 0; k2 < face_count; k2++) {
                int i3 = face_labels[k2];
                face_label_groups[i3][ai1[i3]++] = k2;
            }
            face_labels = null;
        }
    }


    private void apply_frame_anim_transformations(int transform_type, int labels[], int x, int y, int z) {

        int i1 = labels.length;
        if (transform_type == 0) {
            int j1 = 0;
            transformTempX = 0;
            transformTempY = 0;
            transformTempZ = 0;
            for (int k2 = 0; k2 < i1; k2++) {
                int l3 = labels[k2];
                if (l3 < vertex_label_groups.length) {
                    int ai5[] = vertex_label_groups[l3];
                    for (int i5 = 0; i5 < ai5.length; i5++) {
                        int j6 = ai5[i5];
                        transformTempX += verticesX[j6];
                        transformTempY += verticesY[j6];
                        transformTempZ += verticesZ[j6];
                        j1++;
                    }

                }
            }

            if (j1 > 0) {
                transformTempX = (int) (transformTempX / j1 + x);
                transformTempY = (int) (transformTempY / j1 + y);
                transformTempZ = (int) (transformTempZ / j1 + z);
                return;
            } else {
                transformTempX = (int) x;
                transformTempY = (int) y;
                transformTempZ = (int) z;
                return;
            }
        }
        if (transform_type == 1) {
            for (int k1 = 0; k1 < i1; k1++) {
                int l2 = labels[k1];
                if (l2 < vertex_label_groups.length) {
                    int ai1[] = vertex_label_groups[l2];
                    for (int i4 = 0; i4 < ai1.length; i4++) {
                        int j5 = ai1[i4];
                        verticesX[j5] += x;
                        verticesY[j5] += y;
                        verticesZ[j5] += z;
                    }

                }
            }

            return;
        }
        if (transform_type == 2) {
            for (int l1 = 0; l1 < i1; l1++) {
                int i3 = labels[l1];
                if (i3 < vertex_label_groups.length) {
                    int auid[] = vertex_label_groups[i3];
                    for (int j4 = 0; j4 < auid.length; j4++) {
                        int k5 = auid[j4];
                        verticesX[k5] -= transformTempX;
                        verticesY[k5] -= transformTempY;
                        verticesZ[k5] -= transformTempZ;
                        int k6 = (x & 0xff) * 8;
                        int l6 = (y & 0xff) * 8;
                        int i7 = (z & 0xff) * 8;
                        if (i7 != 0) {
                            int j7 = SINE[i7];
                            int i8 = COSINE[i7];
                            int l8 = verticesY[k5] * j7 + verticesX[k5] * i8 >> 16;
                            verticesY[k5] = verticesY[k5] * i8 - verticesX[k5] * j7 >> 16;
                            verticesX[k5] = l8;
                        }
                        if (k6 != 0) {
                            int k7 = SINE[k6];
                            int j8 = COSINE[k6];
                            int i9 = verticesY[k5] * j8 - verticesZ[k5] * k7 >> 16;
                            verticesZ[k5] = verticesY[k5] * k7 + verticesZ[k5] * j8 >> 16;
                            verticesY[k5] = i9;
                        }
                        if (l6 != 0) {
                            int l7 = SINE[l6];
                            int k8 = COSINE[l6];
                            int j9 = verticesZ[k5] * l7 + verticesX[k5] * k8 >> 16;
                            verticesZ[k5] = verticesZ[k5] * k8 - verticesX[k5] * l7 >> 16;
                            verticesX[k5] = j9;
                        }
                        verticesX[k5] += transformTempX;
                        verticesY[k5] += transformTempY;
                        verticesZ[k5] += transformTempZ;
                    }

                }
            }

            return;
        }
        if (transform_type == 3) {
            for (int uid = 0; uid < i1; uid++) {
                int j3 = labels[uid];
                if (j3 < vertex_label_groups.length) {
                    int ai3[] = vertex_label_groups[j3];
                    for (int k4 = 0; k4 < ai3.length; k4++) {
                        int l5 = ai3[k4];
                        verticesX[l5] -= transformTempX;
                        verticesY[l5] -= transformTempY;
                        verticesZ[l5] -= transformTempZ;
                        verticesX[l5] = (int) ((verticesX[l5] * x) / 128);
                        verticesY[l5] = (int) ((verticesY[l5] * y) / 128);
                        verticesZ[l5] = (int) ((verticesZ[l5] * z) / 128);
                        verticesX[l5] += transformTempX;
                        verticesY[l5] += transformTempY;
                        verticesZ[l5] += transformTempZ;
                    }

                }
            }

            return;
        }
        if (transform_type == 5 && face_label_groups != null && face_alphas != null) {
            for (int j2 = 0; j2 < i1; j2++) {
                int k3 = labels[j2];
                if (k3 < face_label_groups.length) {
                    int ai4[] = face_label_groups[k3];
                    for (int l4 = 0; l4 < ai4.length; l4++) {
                        int var13 = ai4[l4];
                        int var14 = (this.face_alphas[var13] & 255) + x * 8;
                        if (var14 < 0) {
                            var14 = 0;
                        } else if (var14 > 255) {
                            var14 = 255;
                        }

                        this.face_alphas[var13] = (byte)var14;
                    }

                }
            }

        }
    }

//    public void animate_frame(int frameId) {
//        if (vertexGroups == null)
//            return;
//
//        if (frameId == -1)
//            return;
//
//        Frame frame = Frame.get_frame(frameId);
//        if (frame == null)
//            return;
//
//        FrameBase base = frame.base;
//        transformTempX = 0;
//        transformTempY = 0;
//        transformTempZ = 0;
//
//        for (int index = 0; index < frame.transformationCount; index++) {
//            int pos = frame.transformationIndices[index];
//            apply_frame_anim_transformations(base.transform_types[pos], base.labels[pos], frame.transformX[index], frame.transformY[index], frame.transformZ[index]);
//        }
//
//        this.resetBounds();
//        invalidate();
//    }

    public void animate_frame_new(AnimFrameSet frame_set, int frameindex) {
        if (vertex_label_groups == null)
            return;

        if (frameindex == -1)
            return;
        if(frame_set == null) {
            System.err.println("null frameset ");
            return;
        }
        AnimFrame frame = frame_set.frames[frameindex];
        if(frame == null) {
            return;
        }
        AnimBase base = frame.base;

        transformTempX = 0;
        transformTempY = 0;
        transformTempZ = 0;

        for (int index = 0; index < frame.transformationCount; index++) {
            int pos = frame.transformationIndices[index];
            apply_frame_anim_transformations(base.transform_types[pos], base.labels[pos], frame.transformX[index], frame.transformY[index], frame.transformZ[index]);
        }

        this.resetBounds();
        invalidate();
    }
    public void animate_frames(AnimFrameSet frame_setref, int frame_index, int[] labels, boolean tweening) {
        if (labels == null) {
            this.animate_frame_new(frame_setref, frame_index);
        } else {
            AnimFrame frame = frame_setref.frames[frame_index];
            AnimBase base = frame.base;
            byte var7 = 0;
            int var11 = var7 + 1;
            int var8 = labels[var7];
            transformTempX = 0;
            transformTempY = 0;
            transformTempZ = 0;

            for(int var9 = 0; var9 < frame.transformationCount; ++var9) {
                int var10 = frame.transformationIndices[var9];

                while(var10 > var8) {
                    var8 = labels[var11++];
                }

                if (tweening) {
                    if (var10 == var8 || base.transform_types[var10] == 0) {
                        this.apply_frame_anim_transformations(base.transform_types[var10], base.labels[var10], frame.transformX[var9], frame.transformY[var9], frame.transformZ[var9]);
                    }
                } else if (var10 != var8 || base.transform_types[var10] == 0) {
                    this.apply_frame_anim_transformations(base.transform_types[var10], base.labels[var10], frame.transformX[var9], frame.transformY[var9], frame.transformZ[var9]);
                }
            }

        }
    }

    public void animate_either(AnimationDefinition seq, int frameindex) {

        animate_either(seq, frameindex, false);
    }
    // use this instead of animate_frame or animate_entity
    public void animate_either(AnimationDefinition seq, int frameindex, boolean use_secondary_frames) {
        if (frameindex == -1)
            return;

        if(seq.using_keyframes()) {
            AnimKeyFrameSet keyframeset = seq.get_keyframeset();
            animate_skeletal_keyframe(keyframeset, frameindex);
        } else {
            int frame = seq.anIntArray353[frameindex];//if anything do active ? seq.iftype_frames : seq.frames
            int frame_index = frame & 0xffff;
            if(frame == -1) {
                System.out.println("-1 bad frame, index " + frame_index + ", group: " + (frame >> 16) + " in anim " + seq.id + " frame " + seq.anIntArray353[frameindex] + ", secondary " + seq.secondary_frames[frameindex]);
                System.out.println(Arrays.toString(seq.anIntArray353));
                return;
            }
            AnimFrameSet frameset = AnimFrameSet.get_frameset(frame >> 16);
            if(frameset == null ) {
                System.out.println("null frame " + frame + ", index " + frame_index + ", group: " + (frame >> 16) + " in anim " + seq.id);
            } else {
                animate_frame_new(frameset, frame_index);
            }
        }

    }

    public void animate_skeletal_keyframe(AnimKeyFrameSet frame, int keyframeId) {
        if (keyframeId == -1)
            return;

        if (frame == null)
            return;

        AnimBase base = frame.base;
        SkeletalAnimBase skeletal_base = base.get_skeletal_animbase();
        if (skeletal_base != null) {
            skeletal_base.animate(frame, keyframeId);
            this.apply_skeletalanim_transformation(skeletal_base, frame.get_keyframeid());
        }

        if (frame.modifies_alpha()) {
            this.apply_skeletalanim_transparency(frame, keyframeId);
        }

        this.resetBounds();
        invalidate();
    }
    void apply_skeletalanim_transformation(SkeletalAnimBase skeleton, int keyframe) {
        if (this.vertex_bone_origins != null) {
            for(int vertex = 0; vertex < this.vertex_count; ++vertex) {
                int[] origin_bones = this.vertex_bone_origins[vertex];
                if (origin_bones != null && origin_bones.length != 0) {
                    int[] vertex_scale = this.vertex_bone_scales[vertex];
                    total_skeletal_transforms.set_zero();

                    for(int var6 = 0; var6 < origin_bones.length; ++var6) {
                        int bone_index = origin_bones[var6];
                        AnimationBone bone = skeleton.get_bone(bone_index);
                        if (bone != null) {
                            skeletal_scale_matrix.set_scale((float)vertex_scale[var6] / 255.0F);
                            skeletal_transform_matrix.set(bone.get_skinning(keyframe));
                            skeletal_transform_matrix.mul(skeletal_scale_matrix);
                            total_skeletal_transforms.add(skeletal_transform_matrix);
                        }
                    }

                    this.apply_skeletal_transformation_matrix_to_model(vertex, total_skeletal_transforms);
                }
            }

        }
    }

    void apply_skeletal_transformation_matrix_to_model(int vertex, Matrix4f matrix) {
        float vec_x = (float)this.verticesX[vertex];
        float vec_y = (float)(-this.verticesY[vertex]);
        float vec_z = (float)(-this.verticesZ[vertex]);
        float vec_w = 1.0F;
        this.verticesX[vertex] = (int)(matrix.values[0] * vec_x + matrix.values[4] * vec_y + matrix.values[8] * vec_z + matrix.values[12] * vec_w);
        this.verticesY[vertex] = -((int)(matrix.values[1] * vec_x + matrix.values[5] * vec_y + matrix.values[9] * vec_z + matrix.values[13] * vec_w));
        this.verticesZ[vertex] = -((int)(matrix.values[2] * vec_x + matrix.values[6] * vec_y + matrix.values[10] * vec_z + matrix.values[14] * vec_w));
    }

    void apply_skeletalanim_transparency(AnimKeyFrameSet keyframes, int keyframe) {
        AnimBase base = keyframes.base;

        for(int var4 = 0; var4 < base.count; ++var4) {
            int transform_type = base.transform_types[var4];
            if (transform_type == 5 && keyframes.transforms != null && keyframes.transforms[var4] != null && keyframes.transforms[var4][0] != null && this.face_label_groups != null && this.face_alphas != null) {
                AnimationKeyFrame var6 = keyframes.transforms[var4][0];
                int[] anim_labels = base.labels[var4];
                int anim_labels_count = anim_labels.length;

                for(int var9 = 0; var9 < anim_labels_count; ++var9) {
                    int anim_label = anim_labels[var9];
                    if (anim_label < this.face_label_groups.length) {
                        int[] face_labels = this.face_label_groups[anim_label];

                        for(int i = 0; i < face_labels.length; ++i) {
                            int face_label = face_labels[i];
                            int alpha = (int)((float)(this.face_alphas[face_label] & 255) + var6.get_value(keyframe) * 255.0F);
                            if (alpha < 0) {
                                alpha = 0;
                            } else if (alpha > 255) {
                                alpha = 255;
                            }

                            this.face_alphas[face_label] = (byte)alpha;
                        }
                    }
                }
            }
        }

    }
    public void animate_skeletal_keyframe_tween(AnimBase base, AnimKeyFrameSet keyframeset, int keyframe_index, boolean[] bone_groups, boolean is_tweening, boolean secondaryanim_uses_frames) {
        SkeletalAnimBase skeletal_base = base.get_skeletal_animbase();
        if (skeletal_base != null) {
            skeletal_base.animate(keyframeset, keyframe_index, bone_groups, is_tweening);
            if (secondaryanim_uses_frames) {
                this.apply_skeletalanim_transformation(skeletal_base, keyframeset.get_keyframeid());
            }
        }

        if (!is_tweening && keyframeset.modifies_alpha()) {
            this.apply_skeletalanim_transparency(keyframeset, keyframe_index);
        }

    }

    // = animate2 backwards compatible with the old frame system and the new skeletal keyframe system
    public void animate_dual_either(AnimationDefinition primary_seq, int primary_index, AnimationDefinition secondary_seq, int secondary_index) {
        AnimKeyFrameSet keyframeset_ref = primary_seq.get_keyframeset();
        AnimFrameSet frameset_ref = null;
        boolean skeletal = false;
        AnimBase var9 = null;
        if(primary_seq.using_keyframes()) {
            if (keyframeset_ref == null)
                return;

            if (secondary_seq.using_keyframes() && primary_seq.mergedbonegroups == null) {
                animate_skeletal_keyframe(keyframeset_ref, primary_index);
                return;
            }

            var9 = keyframeset_ref.base;
            animate_skeletal_keyframe_tween(keyframeset_ref.base, keyframeset_ref, primary_index, primary_seq.mergedbonegroups, false, !secondary_seq.using_keyframes());
        } else {
            int frame = primary_seq.anIntArray353[primary_index];
            frameset_ref = AnimFrameSet.get_frameset(frame >> 16);
            primary_index = frame & 0xffff;
            if (frameset_ref == null) {
                animate_either(secondary_seq, secondary_index);
                return;
            }

            if (!secondary_seq.using_keyframes() && (primary_seq.mergedseqgroups == null || secondary_index == -1)) {
                animate_frame_new(frameset_ref, primary_index);
                return;
            }

            if (primary_seq.mergedseqgroups == null || secondary_index == -1) {
                animate_frame_new(frameset_ref, primary_index);
                return;
            }

            skeletal = secondary_seq.using_keyframes();
            if (!skeletal) {
                animate_frames(frameset_ref, primary_index, primary_seq.mergedseqgroups, false);
            }
        }

        if(secondary_seq.using_keyframes()) {
            keyframeset_ref = secondary_seq.get_keyframeset();
            if (keyframeset_ref == null) {
                return;
            }

            if (var9 == null) {
                var9 = keyframeset_ref.base;
            }

            animate_skeletal_keyframe_tween(var9, keyframeset_ref, secondary_index, primary_seq.mergedbonegroups, true, true);
        } else {
            int var12 = secondary_seq.anIntArray353[secondary_index];
            AnimFrameSet var14 = AnimFrameSet.get_frameset(var12 >> 16);
            int var13 = var12 & 0xffff;
            if (var14 == null) {
                animate_either(primary_seq, primary_index);
                return;
            }

            animate_frames(var14, var13, primary_seq.mergedseqgroups, true);
        }

        if (skeletal && frameset_ref != null) {
            animate_frames(frameset_ref, primary_index, primary_seq.mergedseqgroups, false);
        }

        this.resetBounds();
        invalidate();
    }

//    public void animate2(int merged_seqs[], int secondary_index, int primary_index) {
//        if (primary_index == -1)
//            return;
//
//        if (merged_seqs == null || secondary_index == -1) {
//            animate_frame(primary_index);
//            return;
//        }
//        Frame anim = Frame.get_frame(primary_index);
//        if (anim == null)
//            return;
//
//        Frame skin = Frame.get_frame(secondary_index);
//        if (skin == null) {
//            animate_frame(primary_index);
//            return;
//        }
//        FrameBase list = anim.base;
//        transformTempX = 0;
//        transformTempY = 0;
//        transformTempZ = 0;
//        int id = 0;
//        int table = merged_seqs[id++];
//        for (int index = 0; index < anim.transformationCount; index++) {
//            int condition;
//            for (condition = anim.transformationIndices[index]; condition > table; table = merged_seqs[id++]) {
//            }
//
//            if (condition != table || list.transform_types[condition] == 0) {
//                apply_frame_anim_transformations(list.transform_types[condition], list.labels[condition], skin.transformX[index], skin.transformY[index], skin.transformZ[index]);
//            }
//        }
//
//        transformTempX = 0;
//        transformTempY = 0;
//        transformTempZ = 0;
//        id = 0;
//        table = merged_seqs[id++];
//        for (int index = 0; index < skin.transformationCount; index++) {
//            int condition;
//            for (condition = skin.transformationIndices[index]; condition > table; table = merged_seqs[id++]) {
//            }
//
//            if (condition == table || list.transform_types[condition] == 0) {
//                apply_frame_anim_transformations(list.transform_types[condition], list.labels[condition], skin.transformX[index], skin.transformY[index], skin.transformZ[index]);
//            }
//        }
//
//        this.resetBounds();
//        invalidate();
//    }


    public void rotate90Degrees() {
        for (int vertex = 0; vertex < vertex_count; vertex++) {
            final int vertexX = verticesX[vertex];
            verticesX[vertex] = verticesZ[vertex];
            verticesZ[vertex] = -vertexX;
        }

        this.resetBounds();
        invalidate();
    }

    public void rotateZ(final int degrees) {
        final int sine = SINE[degrees];
        final int cosine = COSINE[degrees];
        for (int vertex = 0; vertex < vertex_count; vertex++) {
            final int newVertexY = verticesY[vertex] * cosine - verticesZ[vertex] * sine >> 16;
            verticesZ[vertex] = verticesY[vertex] * sine + verticesZ[vertex] * cosine >> 16;
            verticesY[vertex] = newVertexY;
        }

        this.resetBounds();
        invalidate();
    }

    public void offsetBy(final int x, final int y, final int z) {
        for (int vertex = 0; vertex < this.vertex_count; vertex++) {
            verticesX[vertex] += x;
            verticesY[vertex] += y;
            verticesZ[vertex] += z;
        }

        this.resetBounds();
        invalidate();
    }

    public void scale(final int size) {
        for (int vertex = 0; vertex < this.vertex_count; vertex++) {
            verticesX[vertex] = verticesX[vertex] / size;
            verticesY[vertex] = verticesY[vertex] / size;
            verticesZ[vertex] = verticesZ[vertex] / size;
        }

        this.resetBounds();
        invalidate();
    }

    public void scale(final int x, final int z, final int y) {
        for (int vertex = 0; vertex < this.vertex_count; vertex++) {
            verticesX[vertex] = (verticesX[vertex] * x) / 128;
            verticesY[vertex] = (verticesY[vertex] * y) / 128;
            verticesZ[vertex] = (verticesZ[vertex] * z) / 128;
        }

        this.resetBounds();
        invalidate();
    }

    public void recolor(int found, int replace) {
        if (colors != null) {
            for (int face = 0; face < face_count; face++) {
                if (colors[face] == (short) found) {
                    colors[face] = (short) replace;
                }
            }
        }
    }

    public void retexture(short found, short replace) {
        if (materials != null) {
            for (int face = 0; face < face_count; face++) {
                if (materials[face] == found) {
                    materials[face] = replace;
                }
            }
        }
    }

    public void mirror() {
        for (int vertex = 0; vertex < vertex_count; vertex++) {
            verticesZ[vertex] = -verticesZ[vertex];
        }

        for (int triangle = 0; triangle < face_count; triangle++) {
            final int newTriangleC = trianglesX[triangle];
            trianglesX[triangle] = trianglesZ[triangle];
            trianglesZ[triangle] = newTriangleC;
        }
    }

    public void calculateVertexNormals() {
        if (normals == null) {
            normals = new VertexNormal[vertex_count];

            int var1;
            for (var1 = 0; var1 < vertex_count; ++var1) {
                normals[var1] = new VertexNormal();
            }

            for (var1 = 0; var1 < face_count; ++var1) {
                int var2 = trianglesX[var1];
                int var3 = trianglesY[var1];
                int var4 = trianglesZ[var1];
                int var5 = verticesX[var3] - verticesX[var2];
                int var6 = verticesY[var3] - verticesY[var2];
                int var7 = verticesZ[var3] - verticesZ[var2];
                int var8 = verticesX[var4] - verticesX[var2];
                int var9 = verticesY[var4] - verticesY[var2];
                int var10 = verticesZ[var4] - verticesZ[var2];
                int var11 = var6 * var10 - var9 * var7;
                int var12 = var7 * var8 - var10 * var5;

                int var13;
                for (var13 = var5 * var9 - var8 * var6; var11 > 8192 || var12 > 8192 || var13 > 8192 || var11 < -8192 || var12 < -8192 || var13 < -8192; var13 >>= 1) {
                    var11 >>= 1;
                    var12 >>= 1;
                }

                int var14 = (int) Math.sqrt(var11 * var11 + var12 * var12 + var13 * var13);
                if (var14 <= 0) {
                    var14 = 1;
                }

                var11 = var11 * 256 / var14;
                var12 = var12 * 256 / var14;
                var13 = var13 * 256 / var14;
                int var15;
                if (drawType == null) {
                    var15 = 0;
                } else {
                    var15 = drawType[var1];
                }

                if (var15 == 0) {
                    VertexNormal var16 = normals[var2];
                    var16.x += var11;
                    var16.y += var12;
                    var16.z += var13;
                    ++var16.magnitude;
                    var16 = normals[var3];
                    var16.x += var11;
                    var16.y += var12;
                    var16.z += var13;
                    ++var16.magnitude;
                    var16 = normals[var4];
                    var16.x += var11;
                    var16.y += var12;
                    var16.z += var13;
                    ++var16.magnitude;
                } else if (var15 == 1) {
                    if (faceNormals == null) {
                        faceNormals = new FaceNormal[face_count];
                    }

                    FaceNormal var17 = faceNormals[var1] = new FaceNormal();
                    var17.x = var11;
                    var17.y = var12;
                    var17.z = var13;
                }
            }
        }
    }

    public void light(int ambient, int contrast, int x, int y, int z, boolean flatShading) {
        this.calculateVertexNormals();
        int magnitude = (int)Math.sqrt((double)(z * z + x * x + y * y));
        int var7 = magnitude * contrast >> 8;
        Model model = new Model();
        model.colorsX = new int[this.face_count];
        model.colorsY = new int[this.face_count];
        model.colorsZ = new int[this.face_count];
        if (this.texture_count > 0 && this.textures != null) {
            int[] var9 = new int[this.texture_count];

            int var10;
            for (var10 = 0; var10 < this.face_count; ++var10) {
                if (this.textures[var10] != -1) {
                    ++var9[this.textures[var10] & 255];
                }
            }

            model.texture_count = 0;

            for (var10 = 0; var10 < this.texture_count; ++var10) {
                if (var9[var10] > 0 && this.texture_mapping_type[var10] == 0) {
                    ++model.texture_count;
                }
            }

            model.textures_mapping_p = new short[model.texture_count]; //should be int (Model is int, ModelData is short)
            model.textures_mapping_m = new short[model.texture_count]; //should be int
            model.textures_mapping_n = new short[model.texture_count]; //should be int
            var10 = 0;

            int var11;
            for (var11 = 0; var11 < this.texture_count; ++var11) {
                if (var9[var11] > 0 && this.texture_mapping_type[var11] == 0) {
                    model.textures_mapping_p[var10] = (short) (this.textures_mapping_p[var11] & '\uffff'); //should be int (short cast redundant)
                    model.textures_mapping_m[var10] = (short) (this.textures_mapping_m[var11] & '\uffff'); //should be int (short cast redundant)
                    model.textures_mapping_n[var10] = (short) (this.textures_mapping_n[var11] & '\uffff'); //should be int (short cast redundant)
                    var9[var11] = var10++;
                } else {
                    var9[var11] = -1;
                }
            }

            model.textures = new byte[this.face_count];

            for (var11 = 0; var11 < this.face_count; ++var11) {
                if (this.textures[var11] != -1) {
                    model.textures[var11] = (byte)var9[this.textures[var11] & 255];
                } else {
                    model.textures[var11] = -1;
                }
            }
        }

        for (int var16 = 0; var16 < this.face_count; ++var16) {
            int var17; //should be byte
            if (this.drawType == null) {
                var17 = 0;
            } else {
                var17 = this.drawType[var16];
            }

            byte var18;
            if (this.face_alphas == null) {
                var18 = 0;
            } else {
                var18 = this.face_alphas[var16];
            }

            short var12;
            if (this.materials == null) {
                var12 = -1;
            } else {
                var12 = this.materials[var16];
            }

            if (var18 == -2) {
                var17 = 3;
            }

            if (var18 == -1) {
                var17 = 2;
            }

            VertexNormal var13;
            int var14;
            FaceNormal var19;
            if (var12 == -1) {
                if (var17 != 0) {
                    if (var17 == 1) {
                        var19 = this.faceNormals[var16];
                        var14 = (y * var19.y + z * var19.z + x * var19.x) / (var7 / 2 + var7) + ambient;
                        model.colorsX[var16] = light(this.colors[var16] & '\uffff', var14);
                        model.colorsZ[var16] = -1;
                    } else if (var17 == 3) {
                        model.colorsX[var16] = 128;
                        model.colorsZ[var16] = -1;
                    } else {
                        model.colorsZ[var16] = -2;
                    }
                } else {
                    int var15 = this.colors[var16] & '\uffff';
                    if (this.shared_normals != null && this.shared_normals[this.trianglesX[var16]] != null) {
                        var13 = this.shared_normals[this.trianglesX[var16]];
                    } else {
                        var13 = this.normals[this.trianglesX[var16]];
                    }

                    var14 = (y * var13.y + z * var13.z + x * var13.x) / (var7 * var13.magnitude) + ambient;
                    model.colorsX[var16] = light(var15, var14);
                    if (this.shared_normals != null && this.shared_normals[this.trianglesY[var16]] != null) {
                        var13 = this.shared_normals[this.trianglesY[var16]];
                    } else {
                        var13 = this.normals[this.trianglesY[var16]];
                    }

                    var14 = (y * var13.y + z * var13.z + x * var13.x) / (var7 * var13.magnitude) + ambient;
                    model.colorsY[var16] = light(var15, var14);
                    if (this.shared_normals != null && this.shared_normals[this.trianglesZ[var16]] != null) {
                        var13 = this.shared_normals[this.trianglesZ[var16]];
                    } else {
                        var13 = this.normals[this.trianglesZ[var16]];
                    }

                    var14 = (y * var13.y + z * var13.z + x * var13.x) / (var7 * var13.magnitude) + ambient;
                    model.colorsZ[var16] = light(var15, var14);
                }
            } else if (var17 != 0) {
                if (var17 == 1) {
                    var19 = this.faceNormals[var16];
                    var14 = (y * var19.y + z * var19.z + x * var19.x) / (var7 / 2 + var7) + ambient;
                    model.colorsX[var16] = light(var14);
                    model.colorsZ[var16] = -1;
                } else {
                    model.colorsZ[var16] = -2;
                }
            } else {
                if (this.shared_normals != null && this.shared_normals[this.trianglesX[var16]] != null) {
                    var13 = this.shared_normals[this.trianglesX[var16]];
                } else {
                    var13 = this.normals[this.trianglesX[var16]];
                }

                var14 = (y * var13.y + z * var13.z + x * var13.x) / (var7 * var13.magnitude) + ambient;
                model.colorsX[var16] = light(var14);
                if (this.shared_normals != null && this.shared_normals[this.trianglesY[var16]] != null) {
                    var13 = this.shared_normals[this.trianglesY[var16]];
                } else {
                    var13 = this.normals[this.trianglesY[var16]];
                }

                var14 = (y * var13.y + z * var13.z + x * var13.x) / (var7 * var13.magnitude) + ambient;
                model.colorsY[var16] = light(var14);
                if (this.shared_normals != null && this.shared_normals[this.trianglesZ[var16]] != null) {
                    var13 = this.shared_normals[this.trianglesZ[var16]];
                } else {
                    var13 = this.normals[this.trianglesZ[var16]];
                }

                var14 = (y * var13.y + z * var13.z + x * var13.x) / (var7 * var13.magnitude) + ambient;
                model.colorsZ[var16] = light(var14);
            }
        }

        this.apply_label_groups();
        model.vertex_count = this.vertex_count;
        model.verticesX = this.verticesX;
        model.verticesY = this.verticesY;
        model.verticesZ = this.verticesZ;
        model.face_count = this.face_count;
        model.trianglesX = this.trianglesX;
        model.trianglesY = this.trianglesY;
        model.trianglesZ = this.trianglesZ;
        model.renderPriorities = this.renderPriorities;
        model.face_alphas = this.face_alphas;
        model.facePriority = this.facePriority;
        model.vertex_label_groups = this.vertex_label_groups;
        model.face_label_groups = this.face_label_groups;
        model.materials = this.materials;
        model.vertex_bone_origins = this.vertex_bone_origins;
        model.vertex_bone_scales = this.vertex_bone_scales;
        this.colorsX = model.colorsX;
        this.colorsY = model.colorsY;
        this.colorsZ = model.colorsZ;
        this.texture_count = model.texture_count;
        this.textures = model.textures;
        this.textures_mapping_p = model.textures_mapping_p;
        this.textures_mapping_m = model.textures_mapping_m;
        this.textures_mapping_n = model.textures_mapping_n;

        if (flatShading) {
            calculateBoundsCylinder();
        } else {//calculateBoundsAABB
            shared_normals = new VertexNormal[vertex_count];
            for (int point = 0; point < vertex_count; point++) {
                VertexNormal norm = super.normals[point];
                VertexNormal merge = shared_normals[point] = new VertexNormal();
                merge.x = norm.x;
                merge.y = norm.y;
                merge.z = norm.z;
                merge.magnitude = norm.magnitude;
            }

            calculateBounds();
        }

        resetBounds();

        if (textures == null) {
            calculateVertexNormals();
        }
        if (faceTextureUVCoordinates == null)
        {
            computeTextureUvCoordinates();
        }

        //Mixins
        VertexNormal[] vertexNormals2 = normals;
        VertexNormal[] vertexVertices = shared_normals;

        if (vertexNormals2 != null && vertexNormalsX == null)
        {
            int vertex_count = getvertex_count();

            vertexNormalsX = new int[vertex_count];
            vertexNormalsY = new int[vertex_count];
            vertexNormalsZ = new int[vertex_count];

            for (int i = 0; i < vertex_count; ++i) {
                VertexNormal vertexNormal;

                if (vertexVertices != null && (vertexNormal = vertexVertices[i]) != null) {
                    vertexNormalsX[i] = vertexNormal.x;
                    vertexNormalsY[i] = vertexNormal.y;
                    vertexNormalsZ[i] = vertexNormal.z;
                } else if ((vertexNormal = vertexNormals2[i]) != null) {
                    vertexNormalsX[i] = vertexNormal.x;
                    vertexNormalsY[i] = vertexNormal.y;
                    vertexNormalsZ[i] = vertexNormal.z;
                }
            }
        }
        this.normals = model.normals;
        this.shared_normals = model.shared_normals;

    }

    public void setLighting(int intensity, int diffusion, int lightX, int lightY, int lightZ) {
        for (int j1 = 0; j1 < face_count; j1++) {
            int k1 = trianglesX[j1];
            int i2 = trianglesY[j1];
            int j2 = trianglesZ[j1];
            if (drawType == null) {
                int i3 = colors[j1];
                VertexNormal vertexNormal = super.normals[k1];
                int k2 = intensity + (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / (diffusion * vertexNormal.magnitude);
                colorsX[j1] = mixLightness(i3, k2, 0);
                vertexNormal = super.normals[i2];
                k2 = intensity + (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / (diffusion * vertexNormal.magnitude);
                colorsY[j1] = mixLightness(i3, k2, 0);
                vertexNormal = super.normals[j2];
                k2 = intensity + (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / (diffusion * vertexNormal.magnitude);
                colorsZ[j1] = mixLightness(i3, k2, 0);
            } else if ((drawType[j1] & 1) == 0) {
                int j3 = colors[j1];
                int k3 = drawType[j1];
                VertexNormal vertexNormal = super.normals[k1];
                int l2 = intensity + (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / (diffusion * vertexNormal.magnitude);
                colorsX[j1] = mixLightness(j3, l2, k3);
                vertexNormal = super.normals[i2];
                l2 = intensity + (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / (diffusion * vertexNormal.magnitude);
                colorsY[j1] = mixLightness(j3, l2, k3);
                vertexNormal = super.normals[j2];
                l2 = intensity + (lightX * vertexNormal.x + lightY * vertexNormal.y + lightZ * vertexNormal.z) / (diffusion * vertexNormal.magnitude);
                colorsZ[j1] = mixLightness(j3, l2, k3);
            }
        }

        super.normals = null;
        shared_normals = null;
        vertex_labels = null;
        face_labels = null;
        if (drawType != null) {
            for (int l1 = 0; l1 < face_count; l1++)
                if ((drawType[l1] & 2) == 2)
                    return;

        }
        colors = null;
    }

    private static int light(int light) {
        if (light >= 2) {
            if (light > 126) {
                light = 126;
            }
        } else {
            light = 2;
        }
        return light;
    }

    private static int light(int hsl, int light) {
        light = light * (hsl & 127) >> 7;
        if (light < 2) {
            light = 2;
        } else if (light > 126) {
            light = 126;
        }
        return (hsl & '\uff80') + light;
    }

    public static int light(int hsl, int light, int type) {
        if ((type & 2) == 2)
            return light(light);

        return light(hsl, light);
    }

    private static int mixLightness(int i, int j, int k) {
        if (i == 65535) {
            return 0;
        }
        if ((k & 2) == 2) {
            if (j < 0) {
                j = 0;
            } else if (j > 127) {
                j = 127;
            }
            j = 127 - j;
            return j;
        }

        j = j * (i & 0x7f) >> 7;
        if (j < 2) {
            j = 2;
        } else if (j > 126) {
            j = 126;
        }
        return (i & 0xff80) + j;
    }

    public void renderModel(final int rotationY, final int rotationZ, final int rotationXW, final int translationX, final int translationY, final int translationZ) {

        if (this.boundsType != 2 && this.boundsType != 1) {
            this.calculateDiagonals();
        }

        final int centerX = Rasterizer.textureInt1;
        final int centerY = Rasterizer.textureInt2;
        final int sineY = SINE[rotationY];
        final int cosineY = COSINE[rotationY];
        final int sineZ = SINE[rotationZ];
        final int cosineZ = COSINE[rotationZ];
        final int sineXW = SINE[rotationXW];
        final int cosineXW = COSINE[rotationXW];
        final int transformation = translationY * sineXW + translationZ * cosineXW >> 16;
        for (int vertex = 0; vertex < vertex_count; vertex++) {
            int x = this.verticesX[vertex];
            int y = this.verticesY[vertex];
            int z = this.verticesZ[vertex];
            if (rotationZ != 0) {
                final int newX = y * sineZ + x * cosineZ >> 16;
                y = y * cosineZ - x * sineZ >> 16;
                x = newX;
            }
            if (rotationY != 0) {
                final int newX = z * sineY + x * cosineY >> 16;
                z = z * cosineY - x * sineY >> 16;
                x = newX;
            }
            x += translationX;
            y += translationY;
            z += translationZ;
            final int newY = y * cosineXW - z * sineXW >> 16;
            z = y * sineXW + z * cosineXW >> 16;
            y = newY;
            vertexScreenZ[vertex] = z - transformation;
            vertexScreenX[vertex] = centerX + (x << 9) / z;
            vertexScreenY[vertex] = centerY + (y << 9) / z;
            if (texture_count > 0) {
                vertexMovedX[vertex] = x;
                vertexMovedY[vertex] = y;
                vertexMovedZ[vertex] = z;
            }
        }

        try {
            this.withinObject(false, false, 0L);
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
    }

    private static boolean mouseInViewport = false;


    public static boolean method322(long var0) {
        boolean var2 = var0 != 0L;
        if (var2) {
            boolean var3 = (int)(var0 >>> 16 & 1L) == 1;
            var2 = !var3;
        }

        return var2;
    }

    private void calculateBoundingBox(int size) {
        if (this.xMidOffset == -1) {
            int minxX = 0;
            int minZ = 0;
            int minY = 0;
            int maxX = 0;
            int maxZ = 0;
            int maxY = 0;
            int var8 = COSINE[size];
            int var9 = SINE[size];

            for (int var10 = 0; var10 < this.vertex_count; ++var10) {
                int x = Rasterizer.method4045(this.verticesX[var10], this.verticesZ[var10], var8, var9);
                int z = this.verticesY[var10];
                int y = Rasterizer.method4046(this.verticesX[var10], this.verticesZ[var10], var8, var9);
                if (x < minxX) {
                    minxX = x;
                }

                if (x > maxX) {
                    maxX = x;
                }

                if (z < minZ) {
                    minZ = z;
                }

                if (z > maxZ) {
                    maxZ = z;
                }

                if (y < minY) {
                    minY = y;
                }

                if (y > maxY) {
                    maxY = y;
                }
            }

            this.xMid = (maxX + minxX) / 2;
            this.yMid = (maxZ + minZ) / 2;
            this.zMid = (maxY + minY) / 2;
            this.xMidOffset = (maxX - minxX + 1) / 2;
            this.yMidOffset = (maxZ - minZ + 1) / 2;
            this.zMidOffset = (maxY - minY + 1) / 2;
            if (this.xMidOffset < 32) {
                this.xMidOffset = 32;
            }

            if (this.zMidOffset < 32) {
                this.zMidOffset = 32;
            }

            if (this.singleTile) {
                this.xMidOffset += 8;
                this.zMidOffset += 8;
            }
        }
    }


    public final void renderAtPoint(int orientation, int pitchSine, int pitchCos, int yawSin, int yawCos, int offsetX, int offsetY, int offsetZ, long uid) {
        if (this.boundsType != 1) {
            this.calculateBoundsCylinder();
        }

        calculateBoundingBox(orientation);
        int sceneX = offsetZ * yawCos - offsetX * yawSin >> 16;
        int sceneY = offsetY * pitchSine + sceneX * pitchCos >> 16;
        int dimensionSinY = diagonal2DAboveOrigin * pitchCos >> 16;
        int pos = sceneY + dimensionSinY;
        if (pos <= 50 || (sceneY >= 6500))
            return;
        int var15 = offsetZ * yawSin + offsetX * yawCos >> 16;
        int var16 = (var15 - diagonal2DAboveOrigin) * WorldController.focalLength;
        if (var16 / pos >= DrawingArea.viewportCenterX)
            return;

        int var17 = (var15 + diagonal2DAboveOrigin) * WorldController.focalLength;
        if (var17 / pos <= -DrawingArea.viewportCenterX)
            return;

        int var18 = offsetY * pitchCos - sceneX * pitchSine >> 16;
        int var19 = diagonal2DAboveOrigin * pitchSine >> 16;

        int var20 = (pitchCos * this.maxY >> 16) + var19;
        int var21 = (var18 + var20) * WorldController.focalLength;
        if (var21 / pos <= -DrawingArea.viewportCenterY)
            return;

        int var22 = var19 + (super.modelHeight * pitchCos >> 16);
        int var23 = (var18 - var22) * WorldController.focalLength;
        if (var23 / pos >= DrawingArea.viewportCenterY)
            return;

        int size = dimensionSinY + (super.modelHeight * pitchSine >> 16);

        boolean var25 = false;
        boolean nearSight = false;
        if (sceneY - size <= 50)
            nearSight = true;

        boolean inView = nearSight || this.texture_count > 0;

        boolean highlighted = false;

        int var52;
        if (uid > 0 && objectExist) {
            boolean withinBounds = false;

            byte distanceMin = 50;
            short distanceMax = 3500;
            int var43 = (cursorX - Rasterizer.textureInt1) * distanceMin / WorldController.focalLength;
            int var44 = (cursorY - Rasterizer.textureInt2) * distanceMin / WorldController.focalLength;
            int var45 = (cursorX - Rasterizer.textureInt1) * distanceMax / WorldController.focalLength;
            int var46 = (cursorY - Rasterizer.textureInt2) * distanceMax / WorldController.focalLength;
            int var47 = Rasterizer.method4045(var44, distanceMin, WorldController.camUpDownX, WorldController.camUpDownY);
            int var53 = Rasterizer.method4046(var44, distanceMin, WorldController.camUpDownX, WorldController.camUpDownY);
            var44 = var47;
            var47 = Rasterizer.method4045(var46, distanceMax, WorldController.camUpDownX, WorldController.camUpDownY);
            int var54 = Rasterizer.method4046(var46, distanceMax, WorldController.camUpDownX, WorldController.camUpDownY);
            var46 = var47;
            var47 = Rasterizer.method4025(var43, var53, WorldController.camLeftRightX, WorldController.camLeftRightY);
            var53 = Rasterizer.method4044(var43, var53, WorldController.camLeftRightX, WorldController.camLeftRightY);
            var43 = var47;
            var47 = Rasterizer.method4025(var45, var54, WorldController.camLeftRightX, WorldController.camLeftRightY);
            var54 = Rasterizer.method4044(var45, var54, WorldController.camLeftRightX, WorldController.camLeftRightY);
            int var55;
            int var59;
            int var60;
            int var61;
            int ViewportMouse_field2588 = (var43 + var47) / 2;
            int GZipDecompressor_field4821 = (var46 + var44) / 2;
            int class340_field4138 = (var54 + var53) / 2;
            int ViewportMouse_field2589 = (var47 - var43) / 2;
            int ItemComposition_field2148 = (var46 - var44) / 2;
            int User_field4308 = (var54 - var53) / 2;
            int class421_field4607 = Math.abs(ViewportMouse_field2589);
            int ViewportMouse_field2590 = Math.abs(ItemComposition_field2148);
            int class136_field1612 = Math.abs(User_field4308);

            int var37 = offsetX + this.xMid;
            int var38 = offsetY + this.yMid;
            int var39 = offsetZ + this.zMid;
            var43 = ViewportMouse_field2588 - var37;
            var44 = GZipDecompressor_field4821 - var38;
            var45 = class340_field4138 - var39;
            if (Math.abs(var43) > xMidOffset + class421_field4607) {
                withinBounds = false;
            } else if (Math.abs(var44) > yMidOffset + ViewportMouse_field2590) {
                withinBounds = false;
            } else if (Math.abs(var45) > zMidOffset + class136_field1612) {
                withinBounds = false;
            } else if (Math.abs(var45 * ItemComposition_field2148 - var44 * User_field4308) > yMidOffset * class136_field1612 + zMidOffset * ViewportMouse_field2590) {
                withinBounds = false;
            } else if (Math.abs(var43 * User_field4308 - var45 * ViewportMouse_field2589) > zMidOffset * class421_field4607 + xMidOffset * class136_field1612) {
                withinBounds = false;
            } else if (Math.abs(var44 * ViewportMouse_field2589 - var43 * ItemComposition_field2148) > xMidOffset * ViewportMouse_field2590 + yMidOffset * class421_field4607) {
                withinBounds = false;
            } else {
                withinBounds = true;
            }
            var59 = pos - var53;
            if (var59 <= 50) {
                var59 = 50;
            }

            if (var15 > 0) {
                var16 /= pos;
                var17 /= var59;
            } else {
                var17 /= pos;
                var16 /= var59;
            }

            if (var18 > 0) {
                var23 /= pos;
                var21 /= var59;
            } else {
                var21 /= pos;
                var23 /= var59;
            }

            var55 = cursorX - Rasterizer.textureInt1;
            var46 = cursorY - Rasterizer.textureInt2;
            if (var55 > var16 && var55 < var17 && var46 > var23 && var46 < var21) {
                withinBounds = true;
            }
            if (withinBounds) {
                if (this.singleTile) {
                    hoveringObjects[objectsHovering++] = uid;
                } else {
                    highlighted = true;
                }
            }
        }

        int sineX = 0;
        int cosineX = 0;
        if (orientation != 0) {
            sineX = SINE[orientation];
            cosineX = COSINE[orientation];
        }

        for (int index = 0; index < this.vertex_count; ++index) {
            int positionX = this.verticesX[index];
            int rasterY = this.verticesY[index];
            int positionZ = this.verticesZ[index];
            if (orientation != 0) {
                int rotatedX = positionZ * sineX + positionX * cosineX >> 16;
                positionZ = positionZ * cosineX - positionX * sineX >> 16;
                positionX = rotatedX;
            }

            positionX += offsetX;
            rasterY += offsetY;
            positionZ += offsetZ;

            int positionY = positionZ * yawSin + yawCos * positionX >> 16;
            positionZ = yawCos * positionZ - positionX * yawSin >> 16;
            positionX = positionY;
            positionY = pitchCos * rasterY - positionZ * pitchSine >> 16;
            positionZ = rasterY * pitchSine + pitchCos * positionZ >> 16;
            vertexScreenZ[index] = positionZ - sceneY;
            if (positionZ >= 50) {
                vertexScreenX[index] = positionX * WorldController.focalLength / positionZ + Rasterizer.textureInt1;
                vertexScreenY[index] = positionY * WorldController.focalLength / positionZ + Rasterizer.textureInt2;
            } else {
                vertexScreenX[index] = -5000;
                var25 = true;
            }

            if (inView) {
                vertexMovedX[index] = positionX;
                vertexMovedY[index] = positionY;
                vertexMovedZ[index] = positionZ;
            }
        }
        try {
                withinObject(var25, highlighted, uid);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean inBounds(int x, int y, int z, int screenX, int screenY, int screenZ, int size) {
        int height = cursorY + size;
        if (height < x && height < y && height < z) {
            return false;
        } else {
            height = cursorY - size;
            if (height > x && height > y && height > z) {
                return false;
            } else {
                height = cursorX + size;
                if (height < screenX && height < screenY && height < screenZ) {
                    return false;
                } else {
                    height = cursorX - size;
                    return height <= screenX || height <= screenY || height <= screenZ;
                }
            }
        }
    }

    final void withinObject(boolean var25, boolean highlighted, long uid) {
        if (diagonal3D < 6000) {
            for (int diagonalIndex = 0; diagonalIndex < diagonal3D; diagonalIndex++) {
                depth[diagonalIndex] = 0;
            }

            int size = singleTile ? 20 : 5;

            int var15;
            int var16;
            int var18;
            for (int currentTriangle = 0; currentTriangle < this.face_count; ++currentTriangle) {
                if (this.colorsZ[currentTriangle] != -2) {
                    int triX = this.trianglesX[currentTriangle];
                    int triY = this.trianglesY[currentTriangle];
                    int triZ = this.trianglesZ[currentTriangle];
                    int screenXX = vertexScreenX[triX];
                    int screenXY = vertexScreenX[triY];
                    int screenXZ = vertexScreenX[triZ];
                    int index;

                    if (screenXX == -5000 || screenXY == -5000 || screenXZ == -5000) {
                        continue;
                    }
                    if (highlighted && inBounds(vertexScreenY[triX], vertexScreenY[triY], vertexScreenY[triZ], screenXX, screenXY, screenXZ,size)) {
                        hoveringObjects[objectsHovering++] = uid;
                    }
                    if (!var25 || screenXX != -5000 && screenXY != -5000 && screenXZ != -5000) {
                        if (highlighted && inBounds(vertexScreenY[triX], vertexScreenY[triY], vertexScreenY[triZ], screenXX, screenXY, screenXZ, size)) {
                            hoveringObjects[objectsHovering++] = uid;
                            highlighted = false;
                        }

                        if ((screenXX - screenXY) * (vertexScreenY[triZ] - vertexScreenY[triY]) - (screenXZ - screenXY) * (vertexScreenY[triX] - vertexScreenY[triY]) > 0) {
                            outOfReach[currentTriangle] = false;
                            if (screenXX >= 0 && screenXY >= 0 && screenXZ >= 0 && screenXX <= Rasterizer.lastX && screenXY <= Rasterizer.lastX && screenXZ <= Rasterizer.lastX) {
                                hasAnEdgeToRestrict[currentTriangle] = false;
                            } else {
                                hasAnEdgeToRestrict[currentTriangle] = true;
                            }

                            index = (vertexScreenZ[triX] + vertexScreenZ[triY] + vertexScreenZ[triZ]) / 3 + this.diagonal3DAboveOrigin;
                            if (index < 0)
                                index = 0;

                            faceLists[index][depth[index]++] = currentTriangle;
                        }
                    } else {
                        index = vertexMovedX[triX];
                        var15 = vertexMovedX[triY];
                        var16 = vertexMovedX[triZ];
                        int var30 = vertexMovedY[triX];
                        var18 = vertexMovedY[triY];
                        int var19 = vertexMovedY[triZ];
                        int var20 = vertexMovedZ[triX];
                        int var21 = vertexMovedZ[triY];
                        int var22 = vertexMovedZ[triZ];
                        index -= var15;
                        var16 -= var15;
                        var30 -= var18;
                        var19 -= var18;
                        var20 -= var21;
                        var22 -= var21;
                        int var23 = var30 * var22 - var20 * var19;
                        int var24 = var20 * var16 - index * var22;
                        int var25a = index * var19 - var30 * var16;
                        if (var15 * var23 + var18 * var24 + var21 * var25a > 0) {
                            outOfReach[currentTriangle] = true;
                            int var26 = (vertexScreenZ[triX] + vertexScreenZ[triY] + vertexScreenZ[triZ]) / 3 + this.diagonal3DAboveOrigin;
                            faceLists[var26][depth[var26]++] = currentTriangle;
                        }
                    }
                }
            }
            if (this.renderPriorities == null) {
                for (int faceIndex = this.diagonal3D - 1; faceIndex >= 0; --faceIndex) {
                    int depth = Model.depth[faceIndex];
                    if (depth > 0) {
                        for (int index = 0; index < depth; ++index) {
                            this.drawFace(faceLists[faceIndex][index]);
                        }
                    }
                }

            } else {
                for (int currentIndex = 0; currentIndex < 12; ++currentIndex) {
                    anIntArray1673[currentIndex] = 0;
                    anIntArray1677[currentIndex] = 0;
                }

                for (int depthIndex = this.diagonal3D - 1; depthIndex >= 0; --depthIndex) {
                    int var8 = depth[depthIndex];
                    if (var8 > 0) {

                        for (int var10 = 0; var10 < var8; ++var10) {
                            int var11 = faceLists[depthIndex][var10];
                            byte var31 = this.renderPriorities[var11];
                            int var28 = anIntArray1673[var31]++;
                            anIntArrayArray1674[var31][var28] = var11;
                            if (var31 < 10) {
                                anIntArray1677[var31] += depthIndex;
                            } else if (var31 == 10) {
                                anIntArray1675[var28] = depthIndex;
                            } else {
                                anIntArray1676[var28] = depthIndex;
                            }
                        }
                    }
                }

                int var7 = 0;
                if (anIntArray1673[1] > 0 || anIntArray1673[2] > 0) {
                    var7 = (anIntArray1677[1] + anIntArray1677[2]) / (anIntArray1673[1] + anIntArray1673[2]);
                }

                int var8 = 0;
                if (anIntArray1673[3] > 0 || anIntArray1673[4] > 0) {
                    var8 = (anIntArray1677[3] + anIntArray1677[4]) / (anIntArray1673[3] + anIntArray1673[4]);
                }

                int var9 = 0;
                if (anIntArray1673[6] > 0 || anIntArray1673[8] > 0) {
                    var9 = (anIntArray1677[8] + anIntArray1677[6]) / (anIntArray1673[8] + anIntArray1673[6]);
                }

                int var11 = 0;
                int var12 = anIntArray1673[10];
                int[] var13 = anIntArrayArray1674[10];
                int[] var14 = anIntArray1675;
                if (var11 == var12) {
                    var11 = 0;
                    var12 = anIntArray1673[11];
                    var13 = anIntArrayArray1674[11];
                    var14 = anIntArray1676;
                }

                int var10;
                if (var11 < var12) {
                    var10 = var14[var11];
                } else {
                    var10 = -1000;
                }

                for (var15 = 0; var15 < 10; ++var15) {
                    while (var15 == 0 && var10 > var7) {
                        this.drawFace(var13[var11++]);
                        if (var11 == var12 && var13 != anIntArrayArray1674[11]) {
                            var11 = 0;
                            var12 = anIntArray1673[11];
                            var13 = anIntArrayArray1674[11];
                            var14 = anIntArray1676;
                        }

                        if (var11 < var12) {
                            var10 = var14[var11];
                        } else {
                            var10 = -1000;
                        }
                    }

                    while (var15 == 3 && var10 > var8) {
                        this.drawFace(var13[var11++]);
                        if (var11 == var12 && var13 != anIntArrayArray1674[11]) {
                            var11 = 0;
                            var12 = anIntArray1673[11];
                            var13 = anIntArrayArray1674[11];
                            var14 = anIntArray1676;
                        }

                        if (var11 < var12) {
                            var10 = var14[var11];
                        } else {
                            var10 = -1000;
                        }
                    }

                    while (var15 == 5 && var10 > var9) {
                        this.drawFace(var13[var11++]);
                        if (var11 == var12 && var13 != anIntArrayArray1674[11]) {
                            var11 = 0;
                            var12 = anIntArray1673[11];
                            var13 = anIntArrayArray1674[11];
                            var14 = anIntArray1676;
                        }

                        if (var11 < var12) {
                            var10 = var14[var11];
                        } else {
                            var10 = -1000;
                        }
                    }

                    var16 = anIntArray1673[var15];
                    int[] var17 = anIntArrayArray1674[var15];

                    for (var18 = 0; var18 < var16; ++var18) {
                        this.drawFace(var17[var18]);
                    }
                }

                while (var10 != -1000) {
                    this.drawFace(var13[var11++]);
                    if (var11 == var12 && var13 != anIntArrayArray1674[11]) {
                        var11 = 0;
                        var13 = anIntArrayArray1674[11];
                        var12 = anIntArray1673[11];
                        var14 = anIntArray1676;
                    }

                    if (var11 < var12) {
                        var10 = var14[var11];
                    } else {
                        var10 = -1000;
                    }
                }

            }
        }
    }

    public void drawFace(int face) {
            if (outOfReach[face]) {
                faceRotation(face);
                return;
            }
            int triX = trianglesX[face];
            int triY = trianglesY[face];
            int triZ = trianglesZ[face];
            Rasterizer.textureOutOfDrawingBounds = hasAnEdgeToRestrict[face];
            if (face_alphas == null) {
                Rasterizer.alpha = 0;
            } else {
                Rasterizer.alpha = face_alphas[face] & 255;
            }
            int type;
            if (drawType == null) {
                type = 0;
            } else {
                type = drawType[face] & 3;
            }

            if (materials != null && materials[face] != -1) {
                int textureA = triX;
                int textureB = triY;
                int textureC = triZ;
                if (textures != null && textures[face] != -1) {
                    int coordinate = textures[face] & 0xff;
                    textureA = textures_mapping_p[coordinate];
                    textureB = textures_mapping_m[coordinate];
                    textureC = textures_mapping_n[coordinate];
                }
                if (colorsZ[face] == -1 || type == 3) {
                    Rasterizer.drawTexturedTriangle(
                            vertexScreenY[triX], vertexScreenY[triY], vertexScreenY[triZ],
                            vertexScreenX[triX], vertexScreenX[triY], vertexScreenX[triZ],
                            colorsX[face], colorsX[face], colorsX[face],
                            vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                            vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                            vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                            materials[face]);
                } else {
                    Rasterizer.drawTexturedTriangle(
                            vertexScreenY[triX], vertexScreenY[triY], vertexScreenY[triZ],
                            vertexScreenX[triX], vertexScreenX[triY], vertexScreenX[triZ],
                            colorsX[face], colorsY[face], colorsZ[face],
                            vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                            vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                            vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                            materials[face]);
                }
            } else if(colorsZ[face] == -1) {
                Rasterizer.drawFlatTriangle(vertexScreenY[triX],vertexScreenY[triY],vertexScreenY[triZ],
                        vertexScreenX[triX],vertexScreenX[triY],vertexScreenX[triZ],
                        modelColors[colorsX[face]], -1f, -1f, -1f);
            } else {
                if (type == 0) {
                    Rasterizer.drawGouraudTriangle(vertexScreenY[triX], vertexScreenY[triY],
                            vertexScreenY[triZ], vertexScreenX[triX], vertexScreenX[triY],
                            vertexScreenX[triZ], colorsX[face], colorsY[face], colorsZ[face], -1f, -1f, -1f);
                }
                if (type == 1) {
                    Rasterizer.drawFlatTriangle(vertexScreenY[triX], vertexScreenY[triY],
                            vertexScreenY[triZ], vertexScreenX[triX], vertexScreenX[triY],
                            vertexScreenX[triZ], modelColors[colorsX[face]], -1f, -1f, -1f);
                }
            }

    }

    private final void faceRotation(int triangle) {
        int centreX = Rasterizer.textureInt1;
        int centreY = Rasterizer.textureInt2;
        int counter = 0;
        int x = trianglesX[triangle];
        int y = trianglesY[triangle];
        int z = trianglesZ[triangle];
        int movedX = vertexMovedZ[x];
        int movedY = vertexMovedZ[y];
        int movedZ = vertexMovedZ[z];
        if (movedX >= 50) {
            xPosition[counter] = vertexScreenX[x];
            yPosition[counter] = vertexScreenY[x];
            zPosition[counter++] = colorsX[triangle];
        } else {
            int movedX2 = vertexMovedX[x];
            int movedY2 = vertexMovedY[x];
            int colour = colorsX[triangle];
            if (movedZ >= 50) {
                int k5 = (50 - movedX) * modelLocations[movedZ - movedX];
                xPosition[counter] = centreX + (movedX2 + ((vertexMovedX[z] - movedX2) * k5 >> 16)) * WorldController.focalLength / 50;
                yPosition[counter] = centreY + (movedY2 + ((vertexMovedY[z] - movedY2) * k5 >> 16)) * WorldController.focalLength / 50;
                zPosition[counter++] = colour + ((colorsZ[triangle] - colour) * k5 >> 16);
            }
            if (movedY >= 50) {
                int l5 = (50 - movedX) * modelLocations[movedY - movedX];
                xPosition[counter] = centreX + (movedX2 + ((vertexMovedX[y] - movedX2) * l5 >> 16)) * WorldController.focalLength / 50;
                yPosition[counter] = centreY + (movedY2 + ((vertexMovedY[y] - movedY2) * l5 >> 16)) * WorldController.focalLength / 50;
                zPosition[counter++] = colour + ((colorsY[triangle] - colour) * l5 >> 16);
            }
        }
        if (movedY >= 50) {
            xPosition[counter] = vertexScreenX[y];
            yPosition[counter] = vertexScreenY[y];
            zPosition[counter++] = colorsY[triangle];
        } else {
            int movedX2 = vertexMovedX[y];
            int movedY2 = vertexMovedY[y];
            int colour = colorsY[triangle];
            if (movedX >= 50) {
                int i6 = (50 - movedY) * modelLocations[movedX - movedY];
                xPosition[counter] = centreX + (movedX2 + ((vertexMovedX[x] - movedX2) * i6 >> 16)) * WorldController.focalLength / 50;
                yPosition[counter] = centreY + (movedY2 + ((vertexMovedY[x] - movedY2) * i6 >> 16)) * WorldController.focalLength / 50;
                zPosition[counter++] = colour + ((colorsX[triangle] - colour) * i6 >> 16);
            }
            if (movedZ >= 50) {
                int j6 = (50 - movedY) * modelLocations[movedZ - movedY];
                xPosition[counter] = centreX + (movedX2 + ((vertexMovedX[z] - movedX2) * j6 >> 16)) * WorldController.focalLength / 50;
                yPosition[counter] = centreY + (movedY2 + ((vertexMovedY[z] - movedY2) * j6 >> 16)) * WorldController.focalLength / 50;
                zPosition[counter++] = colour + ((colorsZ[triangle] - colour) * j6 >> 16);
            }
        }
        if (movedZ >= 50) {
            xPosition[counter] = vertexScreenX[z];
            yPosition[counter] = vertexScreenY[z];
            zPosition[counter++] = colorsZ[triangle];
        } else {
            int movedX2 = vertexMovedX[z];
            int movedY2 = vertexMovedY[z];
            int colour = colorsZ[triangle];
            if (movedY >= 50) {
                int k6 = (50 - movedZ) * modelLocations[movedY - movedZ];
                xPosition[counter] = centreX + (movedX2 + ((vertexMovedX[y] - movedX2) * k6 >> 16)) * WorldController.focalLength / 50;
                yPosition[counter] = centreY + (movedY2 + ((vertexMovedY[y] - movedY2) * k6 >> 16)) * WorldController.focalLength / 50;
                zPosition[counter++] = colour + ((colorsY[triangle] - colour) * k6 >> 16);
            }
            if (movedX >= 50) {
                int l6 = (50 - movedZ) * modelLocations[movedX - movedZ];
                xPosition[counter] = centreX + (movedX2 + ((vertexMovedX[x] - movedX2) * l6 >> 16)) * WorldController.focalLength / 50;
                yPosition[counter] = centreY + (movedY2 + ((vertexMovedY[x] - movedY2) * l6 >> 16)) * WorldController.focalLength / 50;
                zPosition[counter++] = colour + ((colorsX[triangle] - colour) * l6 >> 16);
            }
        }
        int xA = xPosition[0];
        int xB = xPosition[1];
        int xC = xPosition[2];
        int yA = yPosition[0];
        int yB = yPosition[1];
        int yC = yPosition[2];
        if ((xA - xB) * (yC - yB) - (yA - yB) * (xC - xB) > 0) {
            Rasterizer.textureOutOfDrawingBounds = false;
            int textureA = x;
            int textureB = y;
            int textureC = z;
            if (counter == 3) {
                if (xA < 0 || xB < 0 || xC < 0 || xA > DrawingArea.lastX || xB > DrawingArea.lastX || xC > DrawingArea.lastX) {
                    Rasterizer.textureOutOfDrawingBounds = true;
                }

                int drawType;
                if (this.drawType == null) {
                    drawType = 0;
                } else {
                    drawType = this.drawType[triangle] & 3;
                }

                if (materials != null && materials[triangle] != -1) {

                    if (textures != null && textures[triangle] != -1) {
                        int coordinate = textures[triangle] & 0xff;
                        textureA = textures_mapping_p[coordinate];
                        textureB = textures_mapping_m[coordinate];
                        textureC = textures_mapping_n[coordinate];
                    }

                    if (colorsZ[triangle] == -1) {
                        Rasterizer.drawTexturedTriangle(
                                yA, yB, yC,
                                xA, xB, xC,
                                colorsX[triangle], colorsX[triangle], colorsX[triangle],
                                vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                                vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                                vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                                materials[triangle]);
                    } else {
                        Rasterizer.drawTexturedTriangle(
                                yA, yB, yC,
                                xA, xB, xC,
                                zPosition[0], zPosition[1], zPosition[2],
                                vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                                vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                                vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                                materials[triangle]);
                    }
                } else {
                    if (drawType == 0) {
                        Rasterizer.drawGouraudTriangle(yA, yB, yC, xA, xB, xC, zPosition[0], zPosition[1], zPosition[2], -1f, -1f, -1f);
                    } else if (drawType == 1) {
                        Rasterizer.drawFlatTriangle(yA, yB, yC, xA, xB, xC, modelColors[colorsX[triangle]], -1f, -1f, -1f);
                    }
                }
            }
            if (counter == 4) {
                if (xA < 0 || xB < 0 || xC < 0 || xA > DrawingArea.lastX || xB > DrawingArea.lastX || xC > DrawingArea.lastX || xPosition[3] < 0 || xPosition[3] > DrawingArea.lastX) {
                    Rasterizer.textureOutOfDrawingBounds = true;
                }
                int drawType;
                if (this.drawType == null) {
                    drawType = 0;
                } else {
                    drawType = this.drawType[triangle] & 3;
                }

                if (materials != null && materials[triangle] != -1) {
                    if (textures != null && textures[triangle] != -1) {
                        int coordinate = textures[triangle] & 0xff;
                        textureA = textures_mapping_p[coordinate];
                        textureB = textures_mapping_m[coordinate];
                        textureC = textures_mapping_n[coordinate];
                    }
                    if (colorsZ[triangle] == -1) {
                        Rasterizer.drawTexturedTriangle(
                                yA, yB, yC,
                                xA, xB, xC,
                                colorsX[triangle], colorsX[triangle], colorsX[triangle],
                                vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                                vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                                vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                                materials[triangle]);
                        Rasterizer.drawTexturedTriangle(
                                yA, yC, yPosition[3],
                                xA, xC, xPosition[3],
                                colorsX[triangle], colorsX[triangle], colorsX[triangle],
                                vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                                vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                                vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                                materials[triangle]);
                    } else {
                        Rasterizer.drawTexturedTriangle(
                                yA, yB, yC,
                                xA, xB, xC,
                                zPosition[0], zPosition[1], zPosition[2],
                                vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                                vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                                vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                                materials[triangle]);
                        Rasterizer.drawTexturedTriangle(
                                yA, yC, yPosition[3],
                                xA, xC, xPosition[3],
                                zPosition[0], zPosition[2], zPosition[3],
                                vertexMovedX[textureA], vertexMovedX[textureB], vertexMovedX[textureC],
                                vertexMovedY[textureA], vertexMovedY[textureB], vertexMovedY[textureC],
                                vertexMovedZ[textureA], vertexMovedZ[textureB], vertexMovedZ[textureC],
                                materials[triangle]);
                    }
                } else {
                    if (drawType == 0) {
                        Rasterizer.drawGouraudTriangle(yA, yB, yC, xA, xB, xC, zPosition[0], zPosition[1], zPosition[2], -1f, -1f, -1f);
                        Rasterizer.drawGouraudTriangle(yA, yC, yPosition[3], xA, xC, xPosition[3], zPosition[0], zPosition[2], zPosition[3], -1f, -1f, -1f);
                        return;
                    }
                    if (drawType == 1) {
                        int l8 = modelColors[colorsX[triangle]];
                        Rasterizer.drawFlatTriangle(yA, yB, yC, xA, xB, xC, l8, -1f, -1f, -1f);
                        Rasterizer.drawFlatTriangle(yA, yC, yPosition[3], xA, xC, xPosition[3], l8, -1f, -1f, -1f);
                    }
                }
            }
        }
    }
    private void vertexNormals()
    {

        if (vertexNormalsX == null)
        {
            int vertex_count = getvertex_count();

            vertexNormalsX = new int[vertex_count];
            vertexNormalsY = new int[vertex_count];
            vertexNormalsZ = new int[vertex_count];

            int[] trianglesX = getFaceIndices1();
            int[] trianglesY = getFaceIndices2();
            int[] trianglesZ = getFaceIndices3();
            int[] verticesX = getVerticesX();
            int[] verticesY = getVerticesY();
            int[] verticesZ = getVerticesZ();

            for (int i = 0; i < face_count; ++i)
            {
                int var9 = trianglesX[i];
                int var10 = trianglesY[i];
                int var11 = trianglesZ[i];

                int var12 = verticesX[var10] - verticesX[var9];
                int var13 = verticesY[var10] - verticesY[var9];
                int var14 = verticesZ[var10] - verticesZ[var9];
                int var15 = verticesX[var11] - verticesX[var9];
                int var16 = verticesY[var11] - verticesY[var9];
                int var17 = verticesZ[var11] - verticesZ[var9];

                int var18 = var13 * var17 - var16 * var14;
                int var19 = var14 * var15 - var17 * var12;

                int var20;
                for (var20 = var12 * var16 - var15 * var13; var18 > 8192 || var19 > 8192 || var20 > 8192 || var18 < -8192 || var19 < -8192 || var20 < -8192; var20 >>= 1)
                {
                    var18 >>= 1;
                    var19 >>= 1;
                }

                int var21 = (int) Math.sqrt(var18 * var18 + var19 * var19 + var20 * var20);
                if (var21 <= 0)
                {
                    var21 = 1;
                }

                var18 = var18 * 256 / var21;
                var19 = var19 * 256 / var21;
                var20 = var20 * 256 / var21;

                vertexNormalsX[var9] += var18;
                vertexNormalsY[var9] += var19;
                vertexNormalsZ[var9] += var20;

                vertexNormalsX[var10] += var18;
                vertexNormalsY[var10] += var19;
                vertexNormalsZ[var10] += var20;

                vertexNormalsX[var11] += var18;
                vertexNormalsY[var11] += var19;
                vertexNormalsZ[var11] += var20;
            }
        }
    }

    public int getvertex_count() {
        return vertex_count;
    }

    boolean isBoundsCalculated;

    public int vertex_bone_origins[][];

    public int vertex_bone_scales[][];

    private int xMid;
    private int yMid;
    private int zMid;
    private int xMidOffset;
    private int yMidOffset;
    private int zMidOffset;

    private float[] faceTextureUVCoordinates;
    private int[] vertexNormalsX, vertexNormalsY, vertexNormalsZ;

    public short[] materials;
    public byte[] textures;
    public byte[] texture_mapping_type;

    public static int anInt1620;
    public static Model emptyModel = new Model();
    private static int sharedVerticesX[] = new int[4000];
    private static int sharedVerticesY[] = new int[4000];
    private static int sharedVerticesZ[] = new int[4000];
    private static byte sharedTriangleAlpha[] = new byte[4000];
    public int vertex_count;
    public int verticesX[];
    public int verticesY[];
    public int verticesZ[];
    public int face_count;
    public int trianglesX[];
    public int trianglesY[];
    public int trianglesZ[];
    public int colorsX[];
    public int colorsY[];
    public int colorsZ[];
    public int drawType[];
    public byte[] renderPriorities;
    public byte face_alphas[];
    public short colors[];
    public byte facePriority = 0;
    public int texture_count;
    public short textures_mapping_p[];
    public short textures_mapping_m[];
    public short textures_mapping_n[];
    public int minX;
    public int maxX;
    public int maxZ;
    public int minZ;
    public int diagonal2DAboveOrigin;
    public int maxY;
    public int diagonal3D;
    public int diagonal3DAboveOrigin;
    public int itemDropHeight;
    public int vertex_labels[];
    public int face_labels[];
    public int vertex_label_groups[][];
    public int face_label_groups[][];
    public boolean singleTile;
    public VertexNormal shared_normals[];
    private FaceNormal[] faceNormals;
    static OnDemandFetcherParent aOnDemandFetcherParent_1662;
    static ModelHeader modelHeaders[];
    static Matrix4f total_skeletal_transforms = new Matrix4f();
    static Matrix4f skeletal_scale_matrix = new Matrix4f();
    static Matrix4f skeletal_transform_matrix = new Matrix4f();
    static boolean hasAnEdgeToRestrict[] = new boolean[8500];
    static boolean outOfReach[] = new boolean[8500];
    static int vertexScreenX[] = new int[8500];
    static int vertexScreenY[] = new int[8500];
    static int vertexScreenZ[] = new int[8500];
    static int vertexMovedX[] = new int[8500];
    static int vertexMovedY[] = new int[8500];
    static int vertexMovedZ[] = new int[8500];
    static int depth[] = new int[8000];
    static int faceLists[][] = new int[8000][512];
    static int anIntArray1673[] = new int[12];
    static int anIntArrayArray1674[][] = new int[12][4000];
    static int anIntArray1676[] = new int[4000];
    static int anIntArray1675[] = new int[4000];
    static int anIntArray1677[] = new int[12];
    static int xPosition[] = new int[10];
    static int yPosition[] = new int[10];
    static int zPosition[] = new int[10];
    static int transformTempX;
    static int transformTempY;
    static int transformTempZ;
    public static boolean objectExist;
    public static int cursorX;
    public static int cursorY;
    public static int objectsHovering;
    public static long hoveringObjects[] = new long[1000];
    public static int SINE[];
    public static int COSINE[];
    static int modelColors[];
    static int modelLocations[];

    static {
        SINE = Rasterizer.Rasterizer3D_sine;
        COSINE = Rasterizer.Rasterizer3D_cosine;
        modelColors = Rasterizer.hslToRgb;
        modelLocations = Rasterizer.anIntArray1469;
    }


    public int bufferOffset;
    public int uvBufferOffset;
    
    
    public void resetBounds() {
        this.boundsType = 0;
        this.xMidOffset = -1;
    }

    public void invalidate() {
        this.shared_normals = null;
        this.normals = null;
        this.faceNormals = null;
        this.isBoundsCalculated = false;
    }
    

    public static void decode700Type(Model def, byte[] var1)
    {
        Buffer var2 = new Buffer(var1);
        Buffer var3 = new Buffer(var1);
        Buffer var4 = new Buffer(var1);
        Buffer var5 = new Buffer(var1);
        Buffer var6 = new Buffer(var1);
        Buffer var7 = new Buffer(var1);
        Buffer var8 = new Buffer(var1);
        var2.setOffset(var1.length - 26);
        int var9 = var2.readUShort();
        int var10 = var2.readUShort();
        int var11 = var2.readUnsignedByte();
        int var12 = var2.readUnsignedByte();
        int var13 = var2.readUnsignedByte();
        int var14 = var2.readUnsignedByte();
        int var15 = var2.readUnsignedByte();
        int var16 = var2.readUnsignedByte();
        int var17 = var2.readUnsignedByte();
        int var18 = var2.readUnsignedByte();
        int var19 = var2.readUShort();
        int var20 = var2.readUShort();
        int var21 = var2.readUShort();
        int var22 = var2.readUShort();
        int var23 = var2.readUShort();
        int var24 = var2.readUShort();
        int var25 = 0;
        int var26 = 0;
        int var27 = 0;
        int var28;


        if (var11 > 0)
        {
            def.texture_mapping_type = new byte[var11];
            var2.setOffset(0);

            for (var28 = 0; var28 < var11; ++var28)
            {
                byte var29 = def.texture_mapping_type[var28] = var2.readByte();
                if (var29 == 0)
                {
                    ++var25;
                }

                if (var29 >= 1 && var29 <= 3)
                {
                    ++var26;
                }

                if (var29 == 2)
                {
                    ++var27;
                }
            }
        }

        var28 = var11 + var9;
        int var58 = var28;
        if (var12 == 1)
        {
            var28 += var10;
        }

        int var30 = var28;
        var28 += var10;
        int var31 = var28;
        if (var13 == 255)
        {
            var28 += var10;
        }

        int var32 = var28;
        if (var15 == 1)
        {
            var28 += var10;
        }

        int var33 = var28;
        var28 += var24;
        int var34 = var28;
        if (var14 == 1)
        {
            var28 += var10;
        }

        int var35 = var28;
        var28 += var22;
        int var36 = var28;
        if (var16 == 1)
        {
            var28 += var10 * 2;
        }

        int var37 = var28;
        var28 += var23;
        int var38 = var28;
        var28 += var10 * 2;
        int var39 = var28;
        var28 += var19;
        int var40 = var28;
        var28 += var20;
        int var41 = var28;
        var28 += var21;
        int var42 = var28;
        var28 += var25 * 6;
        int var43 = var28;
        var28 += var26 * 6;
        int var44 = var28;
        var28 += var26 * 6;
        int var45 = var28;
        var28 += var26 * 2;
        int var46 = var28;
        var28 += var26;
        int var47 = var28;
        var28 = var28 + var26 * 2 + var27 * 2;


        def.vertex_count = var9;
        def.face_count = var10;
        def.texture_count = var11;
        def.verticesX = new int[var9];
        def.verticesY = new int[var9];
        def.verticesZ = new int[var9];
        def.trianglesX = new int[var10];
        def.trianglesY = new int[var10];
        def.trianglesZ = new int[var10];
        if (var17 == 1)
        {
            def.vertex_labels = new int[var9];
        }

        if (var12 == 1)
        {
            def.drawType = new int[var10];
        }

        if (var13 == 255)
        {
            def.renderPriorities = new byte[var10];
        }
        else
        {
            def.facePriority = (byte) var13;
        }

        if (var14 == 1)
        {
            def.face_alphas = new byte[var10];
        }

        if (var15 == 1)
        {
            def.face_labels = new int[var10];
        }

        if (var16 == 1)
        {
            def.materials = new short[var10];
        }

        if (var16 == 1 && var11 > 0)
        {
            def.textures = new byte[var10];
        }

        if (var18 == 1)
        {
            def.vertex_bone_origins = new int[var9][];
            def.vertex_bone_scales = new int[var9][];
        }

        def.colors = new short[var10];
        if (var11 > 0)
        {
            def.textures_mapping_p = new short[var11];
            def.textures_mapping_m = new short[var11];
            def.textures_mapping_n = new short[var11];
        }

        var2.setOffset(var11);
        var3.setOffset(var39);
        var4.setOffset(var40);
        var5.setOffset(var41);
        var6.setOffset(var33);
        int var48 = 0;
        int var49 = 0;
        int var50 = 0;

        int var51;
        int var52;
        int var53;
        int var54;
        int var55;
        for (var51 = 0; var51 < var9; ++var51)
        {
            var52 = var2.readUnsignedByte();
            var53 = 0;
            if ((var52 & 1) != 0)
            {
                var53 = var3.readShortSmartSub();
            }

            var54 = 0;
            if ((var52 & 2) != 0)
            {
                var54 = var4.readShortSmartSub();
            }

            var55 = 0;
            if ((var52 & 4) != 0)
            {
                var55 = var5.readShortSmartSub();
            }

            def.verticesX[var51] = var48 + var53;
            def.verticesY[var51] = var49 + var54;
            def.verticesZ[var51] = var50 + var55;
            var48 = def.verticesX[var51];
            var49 = def.verticesY[var51];
            var50 = def.verticesZ[var51];
            if (var17 == 1)
            {
                def.vertex_labels[var51] = var6.readUnsignedByte();
            }
        }

        if (var18 == 1)
        {
            for (var51 = 0; var51 < var9; ++var51)
            {
                var52 = var6.readUnsignedByte();
                def.vertex_bone_origins[var51] = new int[var52];
                def.vertex_bone_scales[var51] = new int[var52];

                for (var53 = 0; var53 < var52; ++var53)
                {
                    def.vertex_bone_origins[var51][var53] = var6.readUnsignedByte();
                    def.vertex_bone_scales[var51][var53] = var6.readUnsignedByte();
                }
            }
        }

        var2.setOffset(var38);
        var3.setOffset(var58);
        var4.setOffset(var31);
        var5.setOffset(var34);
        var6.setOffset(var32);
        var7.setOffset(var36);
        var8.setOffset(var37);

        for (var51 = 0; var51 < var10; ++var51)
        {
            def.colors[var51] = (short) var2.readUShort();
            if (var12 == 1)
            {
                def.drawType[var51] = var3.readByte();
            }

            if (var13 == 255)
            {
                def.renderPriorities[var51] = var4.readByte();
            }

            if (var14 == 1)
            {
                def.face_alphas[var51] = var5.readByte();
                if (def.face_alphas[var51] < 0) {
                    def.face_alphas[var51] = (byte) (256 + def.face_alphas[var51]);
                }
            }

            if (var15 == 1)
            {
                def.face_labels[var51] = var6.readUnsignedByte();
            }

            if (var16 == 1)
            {
                def.materials[var51] = (short) (var7.readUShort() - 1);
            }

            if (def.textures != null && def.materials[var51] != -1)
            {
                def.textures[var51] = (byte) (var8.readUnsignedByte() - 1);
            }
        }

        var2.setOffset(var35);
        var3.setOffset(var30);
        var51 = 0;
        var52 = 0;
        var53 = 0;
        var54 = 0;

        int var56;
        for (var55 = 0; var55 < var10; ++var55)
        {
            var56 = var3.readUnsignedByte();
            if (var56 == 1)
            {
                var51 = var2.readShortSmartSub() + var54;
                var52 = var2.readShortSmartSub() + var51;
                var53 = var2.readShortSmartSub() + var52;
                var54 = var53;
                def.trianglesX[var55] = var51;
                def.trianglesY[var55] = var52;
                def.trianglesZ[var55] = var53;
            }

            if (var56 == 2)
            {
                var52 = var53;
                var53 = var2.readShortSmartSub() + var54;
                var54 = var53;
                def.trianglesX[var55] = var51;
                def.trianglesY[var55] = var52;
                def.trianglesZ[var55] = var53;
            }

            if (var56 == 3)
            {
                var51 = var53;
                var53 = var2.readShortSmartSub() + var54;
                var54 = var53;
                def.trianglesX[var55] = var51;
                def.trianglesY[var55] = var52;
                def.trianglesZ[var55] = var53;
            }

            if (var56 == 4)
            {
                int var57 = var51;
                var51 = var52;
                var52 = var57;
                var53 = var2.readShortSmartSub() + var54;
                var54 = var53;
                def.trianglesX[var55] = var51;
                def.trianglesY[var55] = var57;
                def.trianglesZ[var55] = var53;
            }
        }

        var2.setOffset(var42);
        var3.setOffset(var43);
        var4.setOffset(var44);
        var5.setOffset(var45);
        var6.setOffset(var46);
        var7.setOffset(var47);

        for (var55 = 0; var55 < var11; ++var55)
        {
            var56 = def.texture_mapping_type[var55] & 255;
            if (var56 == 0)
            {
                def.textures_mapping_p[var55] = (short) var2.readUShort();
                def.textures_mapping_m[var55] = (short) var2.readUShort();
                def.textures_mapping_n[var55] = (short) var2.readUShort();
            }
        }

        var2.setOffset(var28);
        var55 = var2.readUnsignedByte();
        if (var55 != 0)
        {
            var2.readUShort();
            var2.readUShort();
            var2.readUShort();
            var2.readInt();
        }

    }

    public static void decodeCustom525TypeModel(Model def, byte[] var1)
    {
        boolean var2 = false;
        boolean var3 = false;
        Buffer var4 = new Buffer(var1);
        Buffer var5 = new Buffer(var1);
        Buffer var6 = new Buffer(var1);
        Buffer var7 = new Buffer(var1);
        Buffer var8 = new Buffer(var1);
        var4.setOffset(var1.length - 23);
        int var9 = var4.readUShort();
        int var10 = var4.readUShort();
        int var11 = var4.readUnsignedByte();
        int var12 = var4.readUnsignedByte();
        int var13 = var4.readUnsignedByte();
        int var14 = var4.readUnsignedByte();
        int var15 = var4.readUnsignedByte();
        int var16 = var4.readUnsignedByte();
        int var17 = var4.readUnsignedByte();
        int var18 = var4.readUShort();
        int var19 = var4.readUShort();
        int var20 = var4.readUShort();
        int var21 = var4.readUShort();
        int var22 = var4.readUShort();
        byte var23 = 0;
        int var24 = var23 + var9;
        int var25 = var24;
        var24 += var10;
        int var26 = var24;
        if (var13 == 255)
        {
            var24 += var10;
        }

        int var27 = var24;
        if (var15 == 1)
        {
            var24 += var10;
        }

        int var28 = var24;
        if (var12 == 1)
        {
            var24 += var10;
        }

        int var29 = var24;
        var24 += var22;
        int var30 = var24;
        if (var14 == 1)
        {
            var24 += var10;
        }

        int var31 = var24;
        var24 += var21;
        int var32 = var24;
        var24 += var10 * 2;
        int var33 = var24;
        var24 += var11 * 6;
        int var34 = var24;
        var24 += var18;
        int var35 = var24;
        var24 += var19;
        int var10000 = var24 + var20;
        def.vertex_count = var9;
        def.face_count = var10;
        def.texture_count = var11;
        def.verticesX = new int[var9];
        def.verticesY = new int[var9];
        def.verticesZ = new int[var9];
        def.trianglesX = new int[var10];
        def.trianglesY = new int[var10];
        def.trianglesZ = new int[var10];
        if (var11 > 0)
        {
            def.texture_mapping_type = new byte[var11];
            def.textures_mapping_p = new short[var11];
            def.textures_mapping_m = new short[var11];
            def.textures_mapping_n = new short[var11];
        }

        if (var16 == 1)
        {
            def.vertex_labels = new int[var9];
        }

        if (var12 == 1)
        {
            def.drawType = new int[var10];
            def.textures = new byte[var10];
            def.materials = new short[var10];
        }

        if (var13 == 255)
        {
            def.renderPriorities = new byte[var10];
        }
        else
        {
            def.facePriority = (byte) var13;
        }

        if (var14 == 1)
        {
            def.face_alphas = new byte[var10];
        }

        if (var15 == 1)
        {
            def.face_labels = new int[var10];
        }

        if (var17 == 1)
        {
            def.vertex_bone_origins = new int[var9][];
            def.vertex_bone_scales = new int[var9][];
        }

        def.colors = new short[var10];
        var4.setOffset(var23);
        var5.setOffset(var34);
        var6.setOffset(var35);
        var7.setOffset(var24);
        var8.setOffset(var29);
        int var37 = 0;
        int var38 = 0;
        int var39 = 0;

        int var40;
        int var41;
        int var42;
        int var43;
        int var44;
        for (var40 = 0; var40 < var9; ++var40)
        {
            var41 = var4.readUnsignedByte();
            var42 = 0;
            if ((var41 & 1) != 0)
            {
                var42 = var5.readShortSmartSub();
            }

            var43 = 0;
            if ((var41 & 2) != 0)
            {
                var43 = var6.readShortSmartSub();
            }

            var44 = 0;
            if ((var41 & 4) != 0)
            {
                var44 = var7.readShortSmartSub();
            }

            def.verticesX[var40] = var37 + var42;
            def.verticesY[var40] = var38 + var43;
            def.verticesZ[var40] = var39 + var44;
            var37 = def.verticesX[var40];
            var38 = def.verticesY[var40];
            var39 = def.verticesZ[var40];
            if (var16 == 1)
            {
                def.vertex_labels[var40] = var8.readUnsignedByte();
            }
        }

        if (var17 == 1)
        {
            for (var40 = 0; var40 < var9; ++var40)
            {
                var41 = var8.readUnsignedByte();
                def.vertex_bone_origins[var40] = new int[var41];
                def.vertex_bone_scales[var40] = new int[var41];

                for (var42 = 0; var42 < var41; ++var42)
                {
                    def.vertex_bone_origins[var40][var42] = var8.readUnsignedByte();
                    def.vertex_bone_scales[var40][var42] = var8.readUnsignedByte();
                }
            }
        }

        var4.setOffset(var32);
        var5.setOffset(var28);
        var6.setOffset(var26);
        var7.setOffset(var30);
        var8.setOffset(var27);

        for (var40 = 0; var40 < var10; ++var40)
        {
            def.colors[var40] = (short) var4.readUShort();
            if (var12 == 1)
            {
                var41 = var5.readUnsignedByte();
                if ((var41 & 1) == 1)
                {
                    def.drawType[var40] = 1;
                    var2 = true;
                }
                else
                {
                    def.drawType[var40] = 0;
                }

                if ((var41 & 2) == 2)
                {
                    def.textures[var40] = (byte) (var41 >> 2);
                    def.materials[var40] = def.colors[var40];
                    def.colors[var40] = 127;
                    if (def.materials[var40] != -1)
                    {
                        var3 = true;
                    }
                }
                else
                {
                    def.textures[var40] = -1;
                    def.materials[var40] = -1;
                }
            }

            if (var13 == 255)
            {
                def.renderPriorities[var40] = var6.readByte();
            }

            if (var14 == 1)
            {
                def.face_alphas[var40] = var7.readByte();
                if (def.face_alphas[var40] < 0) {
                    def.face_alphas[var40] = (byte) (256 + def.face_alphas[var40]);
                }
            }

            if (var15 == 1)
            {
                def.face_labels[var40] = var8.readUnsignedByte();
            }
        }

        var4.setOffset(var31);
        var5.setOffset(var25);
        var40 = 0;
        var41 = 0;
        var42 = 0;
        var43 = 0;

        int var45;
        int var46;
        for (var44 = 0; var44 < var10; ++var44)
        {
            var45 = var5.readUnsignedByte();
            if (var45 == 1)
            {
                var40 = var4.readShortSmartSub() + var43;
                var41 = var4.readShortSmartSub() + var40;
                var42 = var4.readShortSmartSub() + var41;
                var43 = var42;
                def.trianglesX[var44] = var40;
                def.trianglesY[var44] = var41;
                def.trianglesZ[var44] = var42;
            }

            if (var45 == 2)
            {
                var41 = var42;
                var42 = var4.readShortSmartSub() + var43;
                var43 = var42;
                def.trianglesX[var44] = var40;
                def.trianglesY[var44] = var41;
                def.trianglesZ[var44] = var42;
            }

            if (var45 == 3)
            {
                var40 = var42;
                var42 = var4.readShortSmartSub() + var43;
                var43 = var42;
                def.trianglesX[var44] = var40;
                def.trianglesY[var44] = var41;
                def.trianglesZ[var44] = var42;
            }

            if (var45 == 4)
            {
                var46 = var40;
                var40 = var41;
                var41 = var46;
                var42 = var4.readShortSmartSub() + var43;
                var43 = var42;
                def.trianglesX[var44] = var40;
                def.trianglesY[var44] = var46;
                def.trianglesZ[var44] = var42;
            }
        }

        var4.setOffset(var33);

        for (var44 = 0; var44 < var11; ++var44)
        {
            def.texture_mapping_type[var44] = 0;
            def.textures_mapping_p[var44] = (short) var4.readUShort();
            def.textures_mapping_m[var44] = (short) var4.readUShort();
            def.textures_mapping_n[var44] = (short) var4.readUShort();
        }

        if (def.textures != null)
        {
            boolean var47 = false;

            for (var45 = 0; var45 < var10; ++var45)
            {
                var46 = def.textures[var45] & 255;
                if (var46 != 255)
                {
                    if (def.trianglesX[var45] == (def.textures_mapping_p[var46] & '\uffff') && def.trianglesY[var45] == (def.textures_mapping_m[var46] & '\uffff') && def.trianglesZ[var45] == (def.textures_mapping_n[var46] & '\uffff'))
                    {
                        def.textures[var45] = -1;
                    }
                    else
                    {
                        var47 = true;
                    }
                }
            }

            if (!var47)
            {
                def.textures = null;
            }
        }

        if (!var3)
        {
            def.materials = null;
        }

        if (!var2)
        {
            def.drawType = null;
        }

    }
    //smh who made this shit
    public static void decode525TypeModel(Model def, byte[] var1)
    {
        Buffer var2 = new Buffer(var1);
        Buffer var3 = new Buffer(var1);
        Buffer var4 = new Buffer(var1);
        Buffer var5 = new Buffer(var1);
        Buffer var6 = new Buffer(var1);
        Buffer var7 = new Buffer(var1);
        Buffer var8 = new Buffer(var1);
        var2.setOffset(var1.length - 23);
        int var9 = var2.readUShort();
        int var10 = var2.readUShort();
        int var11 = var2.readUnsignedByte();
        int var12 = var2.readUnsignedByte();
        int var13 = var2.readUnsignedByte();
        int var14 = var2.readUnsignedByte();
        int var15 = var2.readUnsignedByte();
        int var16 = var2.readUnsignedByte();
        int var17 = var2.readUnsignedByte();
        int var18 = var2.readUShort();
        int var19 = var2.readUShort();
        int var20 = var2.readUShort();
        int var21 = var2.readUShort();
        int var22 = var2.readUShort();
        int var23 = 0;
        int var24 = 0;
        int var25 = 0;
        int var26;
        if (var11 > 0)
        {
            def.texture_mapping_type = new byte[var11];
            var2.setOffset(0);

            for (var26 = 0; var26 < var11; ++var26)
            {
                byte var27 = def.texture_mapping_type[var26] = var2.readByte();
                if (var27 == 0)
                {
                    ++var23;
                }

                if (var27 >= 1 && var27 <= 3)
                {
                    ++var24;
                }

                if (var27 == 2)
                {
                    ++var25;
                }
            }
        }

        var26 = var11 + var9;
        int var56 = var26;
        if (var12 == 1)
        {
            var26 += var10;
        }

        int var28 = var26;
        var26 += var10;
        int var29 = var26;
        if (var13 == 255)
        {
            var26 += var10;
        }

        int var30 = var26;
        if (var15 == 1)
        {
            var26 += var10;
        }

        int var31 = var26;
        if (var17 == 1)
        {
            var26 += var9;
        }

        int var32 = var26;
        if (var14 == 1)
        {
            var26 += var10;
        }

        int var33 = var26;
        var26 += var21;
        int var34 = var26;
        if (var16 == 1)
        {
            var26 += var10 * 2;
        }

        int var35 = var26;
        var26 += var22;
        int var36 = var26;
        var26 += var10 * 2;
        int var37 = var26;
        var26 += var18;
        int var38 = var26;
        var26 += var19;
        int var39 = var26;
        var26 += var20;
        int var40 = var26;
        var26 += var23 * 6;
        int var41 = var26;
        var26 += var24 * 6;
        int var42 = var26;
        var26 += var24 * 6;
        int var43 = var26;
        var26 += var24 * 2;
        int var44 = var26;
        var26 += var24;
        int var45 = var26;
        var26 = var26 + var24 * 2 + var25 * 2;
        def.vertex_count = var9;
        def.face_count = var10;
        def.texture_count = var11;
        def.verticesX = new int[var9];
        def.verticesY = new int[var9];
        def.verticesZ = new int[var9];
        def.trianglesX = new int[var10];
        def.trianglesY = new int[var10];
        def.trianglesZ = new int[var10];
        if (var17 == 1)
        {
            def.vertex_labels = new int[var9];
        }

        if (var12 == 1)
        {
            def.drawType = new int[var10];
        }

        if (var13 == 255)
        {
            def.renderPriorities = new byte[var10];
        }
        else
        {
            def.facePriority = (byte) var13;
        }

        if (var14 == 1)
        {
            def.face_alphas = new byte[var10];
        }

        if (var15 == 1)
        {
            def.face_labels = new int[var10];
        }

        if (var16 == 1)
        {
            def.materials = new short[var10];
        }

        if (var16 == 1 && var11 > 0)
        {
            def.textures = new byte[var10];
        }

        def.colors = new short[var10];
        if (var11 > 0)
        {
            def.textures_mapping_p = new short[var11];
            def.textures_mapping_m = new short[var11];
            def.textures_mapping_n = new short[var11];
        }

        var2.setOffset(var11);
        var3.setOffset(var37);
        var4.setOffset(var38);
        var5.setOffset(var39);
        var6.setOffset(var31);
        int var46 = 0;
        int var47 = 0;
        int var48 = 0;

        int var49;
        int var50;
        int var51;
        int var52;
        int var53;
        for (var49 = 0; var49 < var9; ++var49)
        {
            var50 = var2.readUnsignedByte();
            var51 = 0;
            if ((var50 & 1) != 0)
            {
                var51 = var3.readShortSmartSub();
            }

            var52 = 0;
            if ((var50 & 2) != 0)
            {
                var52 = var4.readShortSmartSub();
            }

            var53 = 0;
            if ((var50 & 4) != 0)
            {
                var53 = var5.readShortSmartSub();
            }

            def.verticesX[var49] = var46 + var51;
            def.verticesY[var49] = var47 + var52;
            def.verticesZ[var49] = var48 + var53;
            var46 = def.verticesX[var49];
            var47 = def.verticesY[var49];
            var48 = def.verticesZ[var49];
            if (var17 == 1)
            {
                def.vertex_labels[var49] = var6.readUnsignedByte();
            }
        }

        var2.setOffset(var36);
        var3.setOffset(var56);
        var4.setOffset(var29);
        var5.setOffset(var32);
        var6.setOffset(var30);
        var7.setOffset(var34);
        var8.setOffset(var35);

        for (var49 = 0; var49 < var10; ++var49)
        {
            def.colors[var49] = (short) var2.readUShort();
            if (var12 == 1)
            {
                def.drawType[var49] = var3.readByte();
            }

            if (var13 == 255)
            {
                def.renderPriorities[var49] = var4.readByte();
            }

            if (var14 == 1)
            {
                def.face_alphas[var49] = var5.readByte();
                if (def.face_alphas[var49] < 0) {
                    def.face_alphas[var49] = (byte) (256 + def.face_alphas[var40]);
                }
            }

            if (var15 == 1)
            {
                def.face_labels[var49] = var6.readUnsignedByte();
            }

            if (var16 == 1)
            {
                def.materials[var49] = (short) (var7.readUShort() - 1);
            }

            if (def.textures != null && def.materials[var49] != -1)
            {
                def.textures[var49] = (byte) (var8.readUnsignedByte() - 1);
            }
        }

        var2.setOffset(var33);
        var3.setOffset(var28);
        var49 = 0;
        var50 = 0;
        var51 = 0;
        var52 = 0;

        int var54;
        for (var53 = 0; var53 < var10; ++var53)
        {
            var54 = var3.readUnsignedByte();
            if (var54 == 1)
            {
                var49 = var2.readShortSmartSub() + var52;
                var50 = var2.readShortSmartSub() + var49;
                var51 = var2.readShortSmartSub() + var50;
                var52 = var51;
                def.trianglesX[var53] = var49;
                def.trianglesY[var53] = var50;
                def.trianglesZ[var53] = var51;
            }

            if (var54 == 2)
            {
                var50 = var51;
                var51 = var2.readShortSmartSub() + var52;
                var52 = var51;
                def.trianglesX[var53] = var49;
                def.trianglesY[var53] = var50;
                def.trianglesZ[var53] = var51;
            }

            if (var54 == 3)
            {
                var49 = var51;
                var51 = var2.readShortSmartSub() + var52;
                var52 = var51;
                def.trianglesX[var53] = var49;
                def.trianglesY[var53] = var50;
                def.trianglesZ[var53] = var51;
            }

            if (var54 == 4)
            {
                int var55 = var49;
                var49 = var50;
                var50 = var55;
                var51 = var2.readShortSmartSub() + var52;
                var52 = var51;
                def.trianglesX[var53] = var49;
                def.trianglesY[var53] = var55;
                def.trianglesZ[var53] = var51;
            }
        }

        var2.setOffset(var40);
        var3.setOffset(var41);
        var4.setOffset(var42);
        var5.setOffset(var43);
        var6.setOffset(var44);
        var7.setOffset(var45);

        for (var53 = 0; var53 < var11; ++var53)
        {
            var54 = def.texture_mapping_type[var53] & 255;
            if (var54 == 0)
            {
                def.textures_mapping_p[var53] = (short) var2.readUShort();
                def.textures_mapping_m[var53] = (short) var2.readUShort();
                def.textures_mapping_n[var53] = (short) var2.readUShort();
            }
        }

        var2.setOffset(var26);
        var53 = var2.readUnsignedByte();
        if (var53 != 0)
        {
            var2.readUShort();
            var2.readUShort();
            var2.readUShort();
            var2.readInt();
        }

    }

    public static void decodeOldFormat(Model def, byte[] var1)
    {

        boolean var2 = false;
        boolean var3 = false;
        Buffer var4 = new Buffer(var1);
        //System.out.println("old " + def.id + ", v? " + var4.readUnsignedByte());
        Buffer var5 = new Buffer(var1);
        Buffer var6 = new Buffer(var1);
        Buffer var7 = new Buffer(var1);
        Buffer var8 = new Buffer(var1);
        var4.setOffset(var1.length - 18);
        int var9 = var4.readUShort();
        int var10 = var4.readUShort();
        int var11 = var4.readUnsignedByte();
        int var12 = var4.readUnsignedByte();
        int var13 = var4.readUnsignedByte();
        int var14 = var4.readUnsignedByte();
        int var15 = var4.readUnsignedByte();
        int var16 = var4.readUnsignedByte();
        int var17 = var4.readUShort();
        int var18 = var4.readUShort();
        int var19 = var4.readUShort();
        int var20 = var4.readUShort();
        byte var21 = 0;
        int var22 = var21 + var9;
        int var23 = var22;
        var22 += var10;
        int var24 = var22;
        if (var13 == 255)
        {
            var22 += var10;
        }

        int var25 = var22;
        if (var15 == 1)
        {
            var22 += var10;
        }

        int var26 = var22;
        if (var12 == 1)
        {
            var22 += var10;
        }

        int var27 = var22;
        if (var16 == 1)
        {
            var22 += var9;
        }

        int var28 = var22;
        if (var14 == 1)
        {
            var22 += var10;
        }

        int var29 = var22;
        var22 += var20;
        int var30 = var22;
        var22 += var10 * 2;
        int var31 = var22;
        var22 += var11 * 6;
        int var32 = var22;
        var22 += var17;
        int var33 = var22;
        var22 += var18;
        int var10000 = var22 + var19;
        def.vertex_count = var9;
        def.face_count = var10;
        def.texture_count = var11;
        def.verticesX = new int[var9];
        def.verticesY = new int[var9];
        def.verticesZ = new int[var9];
        def.trianglesX = new int[var10];
        def.trianglesY = new int[var10];
        def.trianglesZ = new int[var10];
        if (var11 > 0)
        {
            def.texture_mapping_type = new byte[var11];
            def.textures_mapping_p = new short[var11];
            def.textures_mapping_m = new short[var11];
            def.textures_mapping_n = new short[var11];
        }

        if (var16 == 1)
        {
            def.vertex_labels = new int[var9];
        }

        if (var12 == 1)
        {
            def.drawType = new int[var10];
            def.textures = new byte[var10];
            def.materials = new short[var10];
        }

        if (var13 == 255)
        {
            def.renderPriorities = new byte[var10];
        }
        else
        {
            def.facePriority = (byte) var13;
        }

        if (var14 == 1)
        {
            def.face_alphas = new byte[var10];
        }

        if (var15 == 1)
        {
            def.face_labels = new int[var10];
        }

        def.colors = new short[var10];
        var4.setOffset(var21);
        var5.setOffset(var32);
        var6.setOffset(var33);
        var7.setOffset(var22);
        var8.setOffset(var27);
        int var35 = 0;
        int var36 = 0;
        int var37 = 0;

        int var38;
        int var39;
        int var40;
        int var41;
        int var42;
        for (var38 = 0; var38 < var9; ++var38)
        {
            var39 = var4.readUnsignedByte();
            var40 = 0;
            if ((var39 & 1) != 0)
            {
                var40 = var5.readShortSmartSub();
            }

            var41 = 0;
            if ((var39 & 2) != 0)
            {
                var41 = var6.readShortSmartSub();
            }

            var42 = 0;
            if ((var39 & 4) != 0)
            {
                var42 = var7.readShortSmartSub();
            }

            def.verticesX[var38] = var35 + var40;
            def.verticesY[var38] = var36 + var41;
            def.verticesZ[var38] = var37 + var42;
            var35 = def.verticesX[var38];
            var36 = def.verticesY[var38];
            var37 = def.verticesZ[var38];
            if (var16 == 1)
            {
                def.vertex_labels[var38] = var8.readUnsignedByte();
            }
        }

        var4.setOffset(var30);
        var5.setOffset(var26);
        var6.setOffset(var24);
        var7.setOffset(var28);
        var8.setOffset(var25);

        for (var38 = 0; var38 < var10; ++var38)
        {
            def.colors[var38] = (short) var4.readUShort();
            if (var12 == 1)
            {
                var39 = var5.readUnsignedByte();
                if ((var39 & 1) == 1)
                {
                    def.drawType[var38] = 1;
                    var2 = true;
                }
                else
                {
                    def.drawType[var38] = 0;
                }

                if ((var39 & 2) == 2)
                {
                    def.textures[var38] = (byte) (var39 >> 2);
                    def.materials[var38] = def.colors[var38];
                    def.colors[var38] = 127;
                    if (def.materials[var38] != -1)
                    {
                        var3 = true;
                    }
                }
                else
                {
                    def.textures[var38] = -1;
                    def.materials[var38] = -1;
                }
            }

            if (var13 == 255)
            {
                def.renderPriorities[var38] = var6.readByte();
            }

            if (var14 == 1)
            {
                def.face_alphas[var38] = var7.readByte();
                if (def.face_alphas[var38] < 0) {
                    def.face_alphas[var38] = (byte) (256 + def.face_alphas[var38]);
                }
            }

            if (var15 == 1)
            {
                def.face_labels[var38] = var8.readUnsignedByte();
            }
        }

        var4.setOffset(var29);
        var5.setOffset(var23);
        var38 = 0;
        var39 = 0;
        var40 = 0;
        var41 = 0;

        int var43;
        int var44;
        for (var42 = 0; var42 < var10; ++var42)
        {
            var43 = var5.readUnsignedByte();
            if (var43 == 1)
            {
                var38 = var4.readShortSmartSub() + var41;
                var39 = var4.readShortSmartSub() + var38;
                var40 = var4.readShortSmartSub() + var39;
                var41 = var40;
                def.trianglesX[var42] = var38;
                def.trianglesY[var42] = var39;
                def.trianglesZ[var42] = var40;
            }

            if (var43 == 2)
            {
                var39 = var40;
                var40 = var4.readShortSmartSub() + var41;
                var41 = var40;
                def.trianglesX[var42] = var38;
                def.trianglesY[var42] = var39;
                def.trianglesZ[var42] = var40;
            }

            if (var43 == 3)
            {
                var38 = var40;
                var40 = var4.readShortSmartSub() + var41;
                var41 = var40;
                def.trianglesX[var42] = var38;
                def.trianglesY[var42] = var39;
                def.trianglesZ[var42] = var40;
            }

            if (var43 == 4)
            {
                var44 = var38;
                var38 = var39;
                var39 = var44;
                var40 = var4.readShortSmartSub() + var41;
                var41 = var40;
                def.trianglesX[var42] = var38;
                def.trianglesY[var42] = var44;
                def.trianglesZ[var42] = var40;
            }
        }

        var4.setOffset(var31);

        for (var42 = 0; var42 < var11; ++var42)
        {
            def.texture_mapping_type[var42] = 0;
            def.textures_mapping_p[var42] = (short) var4.readUShort();
            def.textures_mapping_m[var42] = (short) var4.readUShort();
            def.textures_mapping_n[var42] = (short) var4.readUShort();
        }

        if (def.textures != null)
        {
            boolean var45 = false;

            for (var43 = 0; var43 < var10; ++var43)
            {
                var44 = def.textures[var43] & 255;
                if (var44 != 255)
                {
                    if (def.trianglesX[var43] == (def.textures_mapping_p[var44] & '\uffff') && def.trianglesY[var43] == (def.textures_mapping_m[var44] & '\uffff') && def.trianglesZ[var43] == (def.textures_mapping_n[var44] & '\uffff'))
                    {
                        def.textures[var43] = -1;
                    }
                    else
                    {
                        var45 = true;
                    }
                }
            }

            if (!var45)
            {
                def.textures = null;
            }
        }

        if (!var3)
        {
            def.materials = null;
        }

        if (!var2)
        {
            def.drawType = null;
        }

    }

    public void decode662(byte abyte0[], int modelID) {
        Buffer nc1 = new Buffer(abyte0);
        Buffer nc2 = new Buffer(abyte0);
        Buffer nc3 = new Buffer(abyte0);
        Buffer nc4 = new Buffer(abyte0);
        Buffer nc5 = new Buffer(abyte0);
        Buffer nc6 = new Buffer(abyte0);
        Buffer nc7 = new Buffer(abyte0);
        //System.out.println("622/ " + modelID+ ", v? " + nc1.readUnsignedByte());
        nc1.currentOffset = abyte0.length - 23;
        int vertex_count = nc1.readUShort();
        int numTriangles = nc1.readUShort();
        int numTexTriangles = nc1.readUnsignedByte();
        ModelHeader ModelDef_1 = modelHeaders[modelID] = new ModelHeader();
        ModelDef_1.data = abyte0;
        ModelDef_1.triangleCount = vertex_count;
        ModelDef_1.triangleCount = numTriangles;
        ModelDef_1.texturedTriangleCount = numTexTriangles;
        int l1 = nc1.readUnsignedByte();
        boolean bool = (0x1 & l1 ^ 0xffffffff) == -2;
        boolean bool_26_ = (0x8 & l1) == 8;

        if (!bool_26_) {
            read525Model(abyte0, modelID);
            return;
        }
        int newformat = 0;
        if (bool_26_) {
            nc1.currentOffset -= 7;
            newformat = nc1.readUnsignedByte();
            nc1.currentOffset += 6;
        }

        if (newformat >= 13) {
//            newModel[modelID] = true; // idk
        }
        int i2 = nc1.readUnsignedByte();
        int j2 = nc1.readUnsignedByte();
        int k2 = nc1.readUnsignedByte();
        int l2 = nc1.readUnsignedByte();
        int i3 = nc1.readUnsignedByte();
        int j3 = nc1.readUShort();
        int k3 = nc1.readUShort();
        int l3 = nc1.readUShort();
        int i4 = nc1.readUShort();
        int j4 = nc1.readUShort();
        int k4 = 0;
        int l4 = 0;
        int i5 = 0;
        byte[] x = null;
        byte[] O = null;
        byte[] J = null;
        byte[] F = null;
        byte[] cb = null;
        byte[] gb = null;
        byte[] lb = null;
        int[] kb = null;
        int[] y = null;
        int[] N = null;
        short[] D = null;
        short[] triangleColours2 = new short[numTriangles];
        if (numTexTriangles > 0) {
            O = new byte[numTexTriangles];
            nc1.currentOffset = 0;
            for (int j5 = 0; j5 < numTexTriangles; j5++) {
                byte byte0 = O[j5] = nc1.readSignedByte();
                if (byte0 == 0) {
                    k4++;
                }
                if (byte0 >= 1 && byte0 <= 3) {
                    l4++;
                }
                if (byte0 == 2) {
                    i5++;
                }
            }
        }
        int k5 = numTexTriangles;
        int l5 = k5;
        k5 += vertex_count;
        int i6 = k5;
        if (bool) {
            k5 += numTriangles;
        }
        if (l1 == 1) {
            k5 += numTriangles;
        }
        int j6 = k5;
        k5 += numTriangles;
        int k6 = k5;
        if (i2 == 255) {
            k5 += numTriangles;
        }
        int l6 = k5;
        if (k2 == 1) {
            k5 += numTriangles;
        }
        int i7 = k5;
        if (i3 == 1) {
            k5 += vertex_count;
        }
        int j7 = k5;
        if (j2 == 1) {
            k5 += numTriangles;
        }
        int k7 = k5;
        k5 += i4;
        int l7 = k5;
        if (l2 == 1) {
            k5 += numTriangles * 2;
        }
        int i8 = k5;
        k5 += j4;
        int j8 = k5;
        k5 += numTriangles * 2;
        int k8 = k5;
        k5 += j3;
        int l8 = k5;
        k5 += k3;
        int i9 = k5;
        k5 += l3;
        int j9 = k5;
        k5 += k4 * 6;
        int k9 = k5;
        k5 += l4 * 6;
        int i_59_ = 6;
        if (newformat != 14) {
            if (newformat >= 15) {
                i_59_ = 9;
            }
        } else {
            i_59_ = 7;
        }
        int l9 = k5;
        k5 += i_59_ * l4;
        int i10 = k5;
        k5 += l4;
        int j10 = k5;
        k5 += l4;
        int k10 = k5;
        k5 += l4 + i5 * 2;
        int[] verticesX = new int[vertex_count];
        int[] verticesY = new int[vertex_count];
        int[] verticesZ = new int[vertex_count];
        int[] facePoint1 = new int[numTriangles];
        int[] facePoint2 = new int[numTriangles];
        int[] facePoint3 = new int[numTriangles];
        vertex_labels = new int[vertex_count];
        drawType = new int[numTriangles];
        renderPriorities = new byte[numTriangles];
        face_alphas = new byte[numTriangles];
        face_labels = new int[numTriangles];
        if (i3 == 1) {
            vertex_labels = new int[vertex_count];
        }
        if (bool) {
            drawType = new int[numTriangles];
        }
        if (i2 == 255) {
            renderPriorities = new byte[numTriangles];
        } else {
        }
        if (j2 == 1) {
            face_alphas = new byte[numTriangles];
        }
        if (k2 == 1) {
            face_labels = new int[numTriangles];
        }
        if (l2 == 1) {
            D = new short[numTriangles];
        }
        if (l2 == 1 && numTexTriangles > 0) {
            x = new byte[numTriangles];
        }
        triangleColours2 = new short[numTriangles];
        int[] texTrianglesPoint1 = null;
        int[] texTrianglesPoint2 = null;
        int[] texTrianglesPoint3 = null;
        if (numTexTriangles > 0) {
            texTrianglesPoint1 = new int[numTexTriangles];
            texTrianglesPoint2 = new int[numTexTriangles];
            texTrianglesPoint3 = new int[numTexTriangles];
            if (l4 > 0) {
                kb = new int[l4];
                N = new int[l4];
                y = new int[l4];
                gb = new byte[l4];
                lb = new byte[l4];
                F = new byte[l4];
            }
            if (i5 > 0) {
                cb = new byte[i5];
                J = new byte[i5];
            }
        }
        nc1.currentOffset = l5;
        nc2.currentOffset = k8;
        nc3.currentOffset = l8;
        nc4.currentOffset = i9;
        nc5.currentOffset = i7;
        int l10 = 0;
        int i11 = 0;
        int j11 = 0;
        for (int k11 = 0; k11 < vertex_count; k11++) {
            int l11 = nc1.readUnsignedByte();
            int j12 = 0;
            if ((l11 & 1) != 0) {
                j12 = nc2.method421();
            }
            int l12 = 0;
            if ((l11 & 2) != 0) {
                l12 = nc3.method421();
            }
            int j13 = 0;
            if ((l11 & 4) != 0) {
                j13 = nc4.method421();
            }
            verticesX[k11] = l10 + j12;
            verticesY[k11] = i11 + l12;
            verticesZ[k11] = j11 + j13;
            l10 = verticesX[k11];
            i11 = verticesY[k11];
            j11 = verticesZ[k11];
            if (vertex_labels != null) {
                vertex_labels[k11] = nc5.readUnsignedByte();
            }
        }
        nc1.currentOffset = j8;
        nc2.currentOffset = i6;
        nc3.currentOffset = k6;
        nc4.currentOffset = j7;
        nc5.currentOffset = l6;
        nc6.currentOffset = l7;
        nc7.currentOffset = i8;
        for (int i12 = 0; i12 < numTriangles; i12++) {
            triangleColours2[i12] = (short) nc1.readUShort();
            if (l1 == 1) {
                drawType[i12] = nc2.readSignedByte();
                if (drawType[i12] == 2) {
                    triangleColours2[i12] = (short) 65535;
                }
                drawType[i12] = 0;
            }
            if (i2 == 255) {
                renderPriorities[i12] = nc3.readSignedByte();
            }
            if (j2 == 1) {
                face_alphas[i12] = nc4.readSignedByte();
                if (face_alphas[i12] < 0) {
                    face_alphas[i12] = (byte) (256 + face_alphas[i12]);
                }
            }
            if (k2 == 1) {
                face_labels[i12] = nc5.readUnsignedByte();
            }
            if (l2 == 1) {
                D[i12] = (short) (nc6.readUShort() - 1);
            }
            if (x != null) {
                if (D[i12] != -1) {
                    x[i12] = (byte) (nc7.readUnsignedByte() - 1);
                } else {
                    x[i12] = -1;
                }
            }
        }
        nc1.currentOffset = k7;
        nc2.currentOffset = j6;
        int k12 = 0;
        int i13 = 0;
        int k13 = 0;
        int l13 = 0;
        for (int i14 = 0; i14 < numTriangles; i14++) {
            int j14 = nc2.readUnsignedByte();
            if (j14 == 1) {
                k12 = nc1.method421() + l13;
                l13 = k12;
                i13 = nc1.method421() + l13;
                l13 = i13;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
            if (j14 == 2) {
                i13 = k13;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
            if (j14 == 3) {
                k12 = k13;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
            if (j14 == 4) {
                int l14 = k12;
                k12 = i13;
                i13 = l14;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
        }
        nc1.currentOffset = j9;
        nc2.currentOffset = k9;
        nc3.currentOffset = l9;
        nc4.currentOffset = i10;
        nc5.currentOffset = j10;
        nc6.currentOffset = k10;
        for (int k14 = 0; k14 < numTexTriangles; k14++) {
            int i15 = O[k14] & 0xff;
            if (i15 == 0) {
                texTrianglesPoint1[k14] = nc1.readUShort();
                texTrianglesPoint2[k14] = nc1.readUShort();
                texTrianglesPoint3[k14] = nc1.readUShort();
            }
            if (i15 == 1) {
                texTrianglesPoint1[k14] = nc2.readUShort();
                texTrianglesPoint2[k14] = nc2.readUShort();
                texTrianglesPoint3[k14] = nc2.readUShort();
                if (newformat < 15) {
                    kb[k14] = nc3.readUShort();
                    if (newformat >= 14) {
                        N[k14] = nc3.v(-1);
                    } else {
                        N[k14] = nc3.readUShort();
                    }
                    y[k14] = nc3.readUShort();
                } else {
                    kb[k14] = nc3.v(-1);
                    N[k14] = nc3.v(-1);
                    y[k14] = nc3.v(-1);
                }
                gb[k14] = nc4.readSignedByte();
                lb[k14] = nc5.readSignedByte();
                F[k14] = nc6.readSignedByte();
            }
            if (i15 == 2) {
                texTrianglesPoint1[k14] = nc2.readUShort();
                texTrianglesPoint2[k14] = nc2.readUShort();
                texTrianglesPoint3[k14] = nc2.readUShort();
                if (newformat >= 15) {
                    kb[k14] = nc3.v(-1);
                    N[k14] = nc3.v(-1);
                    y[k14] = nc3.v(-1);
                } else {
                    kb[k14] = nc3.readUShort();
                    if (newformat < 14) {
                        N[k14] = nc3.readUShort();
                    } else {
                        N[k14] = nc3.v(-1);
                    }
                    y[k14] = nc3.readUShort();
                }
                gb[k14] = nc4.readSignedByte();
                lb[k14] = nc5.readSignedByte();
                F[k14] = nc6.readSignedByte();
                cb[k14] = nc6.readSignedByte();
                J[k14] = nc6.readSignedByte();
            }
            if (i15 == 3) {
                texTrianglesPoint1[k14] = nc2.readUShort();
                texTrianglesPoint2[k14] = nc2.readUShort();
                texTrianglesPoint3[k14] = nc2.readUShort();
                if (newformat < 15) {
                    kb[k14] = nc3.readUShort();
                    if (newformat < 14) {
                        N[k14] = nc3.readUShort();
                    } else {
                        N[k14] = nc3.v(-1);
                    }
                    y[k14] = nc3.readUShort();
                } else {
                    kb[k14] = nc3.v(-1);
                    N[k14] = nc3.v(-1);
                    y[k14] = nc3.v(-1);
                }
                gb[k14] = nc4.readSignedByte();
                lb[k14] = nc5.readSignedByte();
                F[k14] = nc6.readSignedByte();
            }
        }
        if (i2 != 255) {
            for (int i12 = 0; i12 < numTriangles; i12++) {
                renderPriorities[i12] = (byte) i2;
            }
        }
        colors = triangleColours2;
        this.vertex_count = vertex_count;
        this.face_count = numTriangles;
        this.verticesX = verticesX;
        this.verticesY = verticesY;
        this.verticesZ = verticesZ;
        trianglesX = facePoint1;
        trianglesY = facePoint2;
        trianglesZ = facePoint3;
//        convertTexturesTo317(D, texTrianglesPoint1, texTrianglesPoint2, texTrianglesPoint3, false, x);
    }

    public void read525Model(byte abyte0[], int modelID) {
        Buffer nc1 = new Buffer(abyte0);
        Buffer nc2 = new Buffer(abyte0);
        Buffer nc3 = new Buffer(abyte0);
        Buffer nc4 = new Buffer(abyte0);
        Buffer nc5 = new Buffer(abyte0);
        Buffer nc6 = new Buffer(abyte0);
        Buffer nc7 = new Buffer(abyte0);
        nc1.currentOffset = abyte0.length - 23;
        int vertex_count = nc1.readUShort();
        int numTriangles = nc1.readUShort();
        int numTexTriangles = nc1.readUnsignedByte();
        ModelHeader ModelDef_1 = modelHeaders[modelID] = new ModelHeader();
        ModelDef_1.data = abyte0;
        ModelDef_1.triangleCount = vertex_count;
        ModelDef_1.triangleCount = numTriangles;
        ModelDef_1.texturedTriangleCount = numTexTriangles;
        int l1 = nc1.readUnsignedByte();
        boolean bool = (0x1 & l1 ^ 0xffffffff) == -2;
        int i2 = nc1.readUnsignedByte();
        int j2 = nc1.readUnsignedByte();
        int k2 = nc1.readUnsignedByte();
        int l2 = nc1.readUnsignedByte();
        int i3 = nc1.readUnsignedByte();
        int j3 = nc1.readUShort();
        int k3 = nc1.readUShort();
        int l3 = nc1.readUShort();
        int i4 = nc1.readUShort();
        int j4 = nc1.readUShort();
        int k4 = 0;
        int l4 = 0;
        int i5 = 0;
        byte[] x = null;
        byte[] O = null;
        byte[] J = null;
        byte[] F = null;
        byte[] cb = null;
        byte[] gb = null;
        byte[] lb = null;
        int[] kb = null;
        int[] y = null;
        int[] N = null;
        short[] D = null;
        short[] triangleColours2 = new short[numTriangles];
        if (numTexTriangles > 0) {
            O = new byte[numTexTriangles];
            nc1.currentOffset = 0;
            for (int j5 = 0; j5 < numTexTriangles; j5++) {
                byte byte0 = O[j5] = nc1.readSignedByte();
                if (byte0 == 0) {
                    k4++;
                }
                if (byte0 >= 1 && byte0 <= 3) {
                    l4++;
                }
                if (byte0 == 2) {
                    i5++;
                }
            }
        }
        int k5 = numTexTriangles;
        int l5 = k5;
        k5 += vertex_count;
        int i6 = k5;
        if (l1 == 1) {
            k5 += numTriangles;
        }
        int j6 = k5;
        k5 += numTriangles;
        int k6 = k5;
        if (i2 == 255) {
            k5 += numTriangles;
        }
        int l6 = k5;
        if (k2 == 1) {
            k5 += numTriangles;
        }
        int i7 = k5;
        if (i3 == 1) {
            k5 += vertex_count;
        }
        int j7 = k5;
        if (j2 == 1) {
            k5 += numTriangles;
        }
        int k7 = k5;
        k5 += i4;
        int l7 = k5;
        if (l2 == 1) {
            k5 += numTriangles * 2;
        }
        int i8 = k5;
        k5 += j4;
        int j8 = k5;
        k5 += numTriangles * 2;
        int k8 = k5;
        k5 += j3;
        int l8 = k5;
        k5 += k3;
        int i9 = k5;
        k5 += l3;
        int j9 = k5;
        k5 += k4 * 6;
        int k9 = k5;
        k5 += l4 * 6;
        int l9 = k5;
        k5 += l4 * 6;
        int i10 = k5;
        k5 += l4;
        int j10 = k5;
        k5 += l4;
        int k10 = k5;
        k5 += l4 + i5 * 2;
        int[] verticesX = new int[vertex_count];
        int[] verticesY = new int[vertex_count];
        int[] verticesZ = new int[vertex_count];
        int[] facePoint1 = new int[numTriangles];
        int[] facePoint2 = new int[numTriangles];
        int[] facePoint3 = new int[numTriangles];
        vertex_labels = new int[vertex_count];
        drawType = new int[numTriangles];
        renderPriorities = new byte[numTriangles];
        face_alphas = new byte[numTriangles];
        face_labels = new int[numTriangles];
        if (i3 == 1) {
            vertex_labels = new int[vertex_count];
        }
        if (bool) {
            drawType = new int[numTriangles];
        }
        if (i2 == 255) {
            renderPriorities = new byte[numTriangles];
        } else {
        }
        if (j2 == 1) {
            face_alphas = new byte[numTriangles];
        }
        if (k2 == 1) {
            face_labels = new int[numTriangles];
        }
        if (l2 == 1) {
            D = new short[numTriangles];
        }
        if (l2 == 1 && numTexTriangles > 0) {
            x = new byte[numTriangles];
        }
        triangleColours2 = new short[numTriangles];
        int[] texTrianglesPoint1 = null;
        int[] texTrianglesPoint2 = null;
        int[] texTrianglesPoint3 = null;
        if (numTexTriangles > 0) {
            texTrianglesPoint1 = new int[numTexTriangles];
            texTrianglesPoint2 = new int[numTexTriangles];
            texTrianglesPoint3 = new int[numTexTriangles];
            if (l4 > 0) {
                kb = new int[l4];
                N = new int[l4];
                y = new int[l4];
                gb = new byte[l4];
                lb = new byte[l4];
                F = new byte[l4];
            }
            if (i5 > 0) {
                cb = new byte[i5];
                J = new byte[i5];
            }
        }
        nc1.currentOffset = l5;
        nc2.currentOffset = k8;
        nc3.currentOffset = l8;
        nc4.currentOffset = i9;
        nc5.currentOffset = i7;
        int l10 = 0;
        int i11 = 0;
        int j11 = 0;
        for (int k11 = 0; k11 < vertex_count; k11++) {
            int l11 = nc1.readUnsignedByte();
            int j12 = 0;
            if ((l11 & 1) != 0) {
                j12 = nc2.method421();
            }
            int l12 = 0;
            if ((l11 & 2) != 0) {
                l12 = nc3.method421();
            }
            int j13 = 0;
            if ((l11 & 4) != 0) {
                j13 = nc4.method421();
            }
            verticesX[k11] = l10 + j12;
            verticesY[k11] = i11 + l12;
            verticesZ[k11] = j11 + j13;
            l10 = verticesX[k11];
            i11 = verticesY[k11];
            j11 = verticesZ[k11];
            if (vertex_labels != null) {
                vertex_labels[k11] = nc5.readUnsignedByte();
            }
        }
        nc1.currentOffset = j8;
        nc2.currentOffset = i6;
        nc3.currentOffset = k6;
        nc4.currentOffset = j7;
        nc5.currentOffset = l6;
        nc6.currentOffset = l7;
        nc7.currentOffset = i8;
        for (int i12 = 0; i12 < numTriangles; i12++) {
            triangleColours2[i12] = (short) nc1.readUShort();
            if (l1 == 1) {
                drawType[i12] = nc2.readSignedByte();
                if (drawType[i12] == 2) {
                    triangleColours2[i12] = (short) 65535;
                }
                drawType[i12] = 0;
            }
            if (i2 == 255) {
                renderPriorities[i12] = nc3.readSignedByte();
            }
            if (j2 == 1) {
                face_alphas[i12] = nc4.readSignedByte();
                if (face_alphas[i12] < 0) {
                    face_alphas[i12] = (byte) (256 + face_alphas[i12]);
                }
            }
            if (k2 == 1) {
                face_labels[i12] = nc5.readUnsignedByte();
            }
            if (l2 == 1) {
                D[i12] = (short) (nc6.readUShort() - 1);
            }
            if (x != null) {
                if (D[i12] != -1) {
                    x[i12] = (byte) (nc7.readUnsignedByte() - 1);
                } else {
                    x[i12] = -1;
                }
            }
        }
        // /fix's triangle issue, but fucked up - no need, loading all 474-
        // models
        /*
         * try { for(int i12 = 0; i12 < trianglesCount; i12++) {
         * triangleColours2[i12] = nc1.readUShort(); if(l1 == 1){
         * drawType[i12] = nc2.readSignedByte(); } if(i2 == 255){
         * renderPriorities[i12] = nc3.readSignedByte(); } if(j2 == 1){
         * anIntArray1639[i12] = nc4.readSignedByte(); if(anIntArray1639[i12] <
         * 0) anIntArray1639[i12] = (256+anIntArray1639[i12]); } if(k2 == 1)
         * triangleData[i12] = nc5.readUnsignedByte(); if(l2 == 1) D[i12] =
         * (short)(nc6.readUShort() - 1); if(x != null) if(D[i12] != -1)
         * x[i12] = (byte)(nc7.readUnsignedByte() -1); else x[i12] = -1; } }
         * catch (Exception ex) { }
         */
        nc1.currentOffset = k7;
        nc2.currentOffset = j6;
        int k12 = 0;
        int i13 = 0;
        int k13 = 0;
        int l13 = 0;
        for (int i14 = 0; i14 < numTriangles; i14++) {
            int j14 = nc2.readUnsignedByte();
            if (j14 == 1) {
                k12 = nc1.method421() + l13;
                l13 = k12;
                i13 = nc1.method421() + l13;
                l13 = i13;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
            if (j14 == 2) {
                i13 = k13;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
            if (j14 == 3) {
                k12 = k13;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
            if (j14 == 4) {
                int l14 = k12;
                k12 = i13;
                i13 = l14;
                k13 = nc1.method421() + l13;
                l13 = k13;
                facePoint1[i14] = k12;
                facePoint2[i14] = i13;
                facePoint3[i14] = k13;
            }
        }
        nc1.currentOffset = j9;
        nc2.currentOffset = k9;
        nc3.currentOffset = l9;
        nc4.currentOffset = i10;
        nc5.currentOffset = j10;
        nc6.currentOffset = k10;
        for (int k14 = 0; k14 < numTexTriangles; k14++) {
            int i15 = O[k14] & 0xff;
            if (i15 == 0) {
                texTrianglesPoint1[k14] = nc1.readUShort();
                texTrianglesPoint2[k14] = nc1.readUShort();
                texTrianglesPoint3[k14] = nc1.readUShort();
            }
            if (i15 == 1) {
                texTrianglesPoint1[k14] = nc2.readUShort();
                texTrianglesPoint2[k14] = nc2.readUShort();
                texTrianglesPoint3[k14] = nc2.readUShort();
                kb[k14] = nc3.readUShort();
                N[k14] = nc3.readUShort();
                y[k14] = nc3.readUShort();
                gb[k14] = nc4.readSignedByte();
                lb[k14] = nc5.readSignedByte();
                F[k14] = nc6.readSignedByte();
            }
            if (i15 == 2) {
                texTrianglesPoint1[k14] = nc2.readUShort();
                texTrianglesPoint2[k14] = nc2.readUShort();
                texTrianglesPoint3[k14] = nc2.readUShort();
                kb[k14] = nc3.readUShort();
                N[k14] = nc3.readUShort();
                y[k14] = nc3.readUShort();
                gb[k14] = nc4.readSignedByte();
                lb[k14] = nc5.readSignedByte();
                F[k14] = nc6.readSignedByte();
                cb[k14] = nc6.readSignedByte();
                J[k14] = nc6.readSignedByte();
            }
            if (i15 == 3) {
                texTrianglesPoint1[k14] = nc2.readUShort();
                texTrianglesPoint2[k14] = nc2.readUShort();
                texTrianglesPoint3[k14] = nc2.readUShort();
                kb[k14] = nc3.readUShort();
                N[k14] = nc3.readUShort();
                y[k14] = nc3.readUShort();
                gb[k14] = nc4.readSignedByte();
                lb[k14] = nc5.readSignedByte();
                F[k14] = nc6.readSignedByte();
            }
        }
        if (i2 != 255) {
            for (int i12 = 0; i12 < numTriangles; i12++) {
                renderPriorities[i12] = (byte) i2;
            }
        }
        colors = triangleColours2;
        this.vertex_count = vertex_count;
        this.face_count = numTriangles;
        this.verticesX = verticesX;
        this.verticesY = verticesY;
        this.verticesZ = verticesZ;
        trianglesX = facePoint1;
        trianglesY = facePoint2;
        trianglesZ = facePoint3;
//        convertTexturesTo317(D, texTrianglesPoint1, texTrianglesPoint2, texTrianglesPoint3, false, x);

    }


    public void method5187(int var1) {
        int var2 = SINE[var1];
        int var3 = COSINE[var1];

        for (int var4 = 0; var4 < this.vertex_count; ++var4) {
            int var5 = var2 * this.verticesZ[var4] + var3 * this.verticesX[var4] >> 16;
            this.verticesZ[var4] = var3 * this.verticesZ[var4] - var2 * this.verticesX[var4] >> 16;
            this.verticesX[var4] = var5;
        }

        this.invalidate();
    }
    public void method5184() {
        for (int var1 = 0; var1 < this.vertex_count; ++var1) {
            int var2 = this.verticesX[var1];
            this.verticesX[var1] = this.verticesZ[var1];
            this.verticesZ[var1] = -var2;
        }

        this.invalidate();
    }

    public void method5177() {
        for (int var1 = 0; var1 < this.vertex_count; ++var1) {
            this.verticesX[var1] = -this.verticesX[var1];
            this.verticesZ[var1] = -this.verticesZ[var1];
        }

        this.invalidate();
    }

    public void method5186() {
        for (int var1 = 0; var1 < this.vertex_count; ++var1) {
            int var2 = this.verticesZ[var1];
            this.verticesZ[var1] = this.verticesX[var1];
            this.verticesX[var1] = -var2;
        }

        this.invalidate();
    }
    public void method4488() {
        for(int var1 = 0; var1 < this.vertex_count; ++var1) {
            int var2 = this.verticesX[var1];
            this.verticesX[var1] = this.verticesZ[var1];
            this.verticesZ[var1] = -var2;
        }

        this.invalidate();
    }

    public void method4484() {
        for(int var1 = 0; var1 < this.vertex_count; ++var1) {
            this.verticesX[var1] = -this.verticesX[var1];
            this.verticesZ[var1] = -this.verticesZ[var1];
        }

        this.invalidate();
    }

    public void method4537() {
        for(int var1 = 0; var1 < this.vertex_count; ++var1) {
            int var2 = this.verticesZ[var1];
            this.verticesZ[var1] = this.verticesX[var1];
            this.verticesX[var1] = -var2;
        }

        this.invalidate();
    }

    public void changeOffset(int var1, int var2, int var3) {
        for (int var4 = 0; var4 < this.vertex_count; ++var4) {
            int[] var10000 = this.verticesX;
            var10000[var4] += var1;
            var10000 = this.verticesY;
            var10000[var4] += var2;
            var10000 = this.verticesZ;
            var10000[var4] += var3;
        }

        this.invalidate();
    }
}