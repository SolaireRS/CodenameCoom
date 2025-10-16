package com.client;

import com.client.plugins.gpu.GPUPlugin;

public final class Rasterizer extends DrawingArea {
	private static int effectCounter = 0;
	private static final float NEAR_CLIP_DISTANCE = 50.0f; // Adjust this value as needed
	public static boolean saveDepth;
	public static float[] depthBuffer;
	private static int mipMapLevel;
	public static int textureAmount = 130;
	static boolean textureOutOfDrawingBounds;
	private static boolean aBoolean1463;
	public static boolean lowMem = false;
	public static boolean aBoolean1464 = true;
	public static int alpha;
	public static int textureInt1;
	public static int textureInt2;
	public static int fieldOfView = 512;
	public static int textureInt3;
	public static int textureInt4;
	private static int[] anIntArray1468;
	public static final int[] anIntArray1469;
	public static int Rasterizer3D_sine[];
	public static int Rasterizer3D_cosine[];
	public static int anIntArray1472[];
	private static int textureCount;
	public static Background textures[] = new Background[textureAmount];
	private static boolean[] textureIsTransparant = new boolean[textureAmount];
	private static int[] averageTextureColours = new int[textureAmount];
	private static int textureRequestBufferPointer;
	private static int[][] textureRequestPixelBuffer;
	private static int[][] texturesPixelBuffer = new int[textureAmount][];
	public static int textureLastUsed[] = new int[textureAmount];
	public static int lastTextureRetrievalCount;
	public static int hslToRgb[] = new int[0x10000];
	private static int[][] currentPalette = new int[textureAmount][];
	public static GPUPlugin gpuPlugin;
	static {
		anIntArray1468 = new int[512];
		anIntArray1469 = new int[2048];
		Rasterizer3D_sine = new int[2048];
		Rasterizer3D_cosine = new int[2048];
		for (int i = 1; i < 512; i++) {
			anIntArray1468[i] = 32768 / i;
		}
		for (int j = 1; j < 2048; j++) {
			anIntArray1469[j] = 0x10000 / j;
		}
		for (int k = 0; k < 2048; k++) {
			Rasterizer3D_sine[k] = (int) (65536D * Math.sin((double) k * 0.0030679614999999999D));
			Rasterizer3D_cosine[k] = (int) (65536D * Math.cos((double) k * 0.0030679614999999999D));
		}
	}

	public static void nullLoader() {
		anIntArray1468 = null;
		anIntArray1468 = null;
		Rasterizer3D_sine = null;
		Rasterizer3D_cosine = null;
		anIntArray1472 = null;
		textures = null;
		textureIsTransparant = null;
		averageTextureColours = null;
		textureRequestPixelBuffer = null;
		texturesPixelBuffer = null;
		textureLastUsed = null;
		hslToRgb = null;
		currentPalette = null;
	}

	public static void method364() {
		anIntArray1472 = new int[DrawingArea.height];
		for (int j = 0; j < DrawingArea.height; j++) {
			anIntArray1472[j] = DrawingArea.width * j;
		}
		textureInt1 = DrawingArea.width / 2;
		textureInt2 = DrawingArea.height / 2;
	}

	public static void method365(int width, int height) {
		anIntArray1472 = new int[height];
		for (int l = 0; l < height; l++) {
			anIntArray1472[l] = width * l;
		}
		textureInt1 = width / 2;
		textureInt2 = height / 2;
	}
	public static void drawFog(int rgb, int begin, int end) {
	    if (depthBuffer == null || pixels == null) return;
	    
	    if (depthBuffer.length != pixels.length) {
	        System.err.println("BUFFER MISMATCH! Skipping fog.");
	        return;
	    }
	    
	    float length = end - begin;
	    
	    for (int index = 0; index < pixels.length; index++) {
	        float depth = depthBuffer[index];
	        int pixelColor = pixels[index];
	        
	        // Skip invalid depths
	        if (Float.isNaN(depth) || Float.isInfinite(depth) || depth < 0) {
	            continue;
	        }
	        
	        // CRITICAL FIX: If depth is at the clear value (10000) but the pixel is NOT black,
	        // then this is an object that didn't write depth properly - DON'T FOG IT
	        if (depth >= 9900) {
	            // Only fog if it's actually black sky (0x000000)
	            if (pixelColor == 0x000000 || pixelColor == 0xFF000000) {
	                pixels[index] = rgb; // Full fog for black sky
	            }
	            // Skip all other pixels at far depth - they're objects with missing depth
	            continue;
	        }
	        
	        // Don't fog objects closer than the fog start
	        if (depth < begin) {
	            continue;
	        }
	        
	        // Apply graduated fog
	        float factor = (depth - begin) / length;
	        if (factor > 0.85f) {
	            factor = 0.85f;
	        }
	        
	        pixels[index] = blend(pixels[index], rgb, factor);
	    }
	}
	// Replace shouldClipTriangle with this safer version:
	private static boolean shouldClipTriangle(float z1, float z2, float z3) {
	    // Only clip invalid depth values that would cause crashes/artifacts
	    if (Float.isNaN(z1) || Float.isNaN(z2) || Float.isNaN(z3)) {
	        return true;
	    }
	    if (Float.isInfinite(z1) || Float.isInfinite(z2) || Float.isInfinite(z3)) {
	        return true;
	    }
	    return false;
	}
	private static int blend(int c1, int c2, float factor) {
	    // CRITICAL: Clamp factor FIRST before any calculations
	    if (factor <= 0f) {
	        return c1; // No fog, return original color
	    }
	    if (factor >= 1f) {
	        return c2; // Full fog
	    }

	    int r1 = (c1 >> 16) & 0xff;
	    int g1 = (c1 >> 8) & 0xff;
	    int b1 = (c1) & 0xff;

	    int r2 = (c2 >> 16) & 0xff;
	    int g2 = (c2 >> 8) & 0xff;
	    int b2 = (c2) & 0xff;

	    int r3 = r2 - r1;
	    int g3 = g2 - g1;
	    int b3 = b2 - b1;

	    int r = (int) (r1 + (r3 * factor));
	    int g = (int) (g1 + (g3 * factor));
	    int b = (int) (b1 + (b3 * factor));

	    return (r << 16) + (g << 8) + b;
	}

	public static int texelPos(int defaultIndex) {
		int x = (defaultIndex & 127) >> mipMapLevel;
		int y = (defaultIndex >> 7) >> mipMapLevel;
		return x + (y << (7 - mipMapLevel));
	}

	public static boolean enableMipmapping = true;
	public static boolean enableDistanceFog = true;

	private static final int[] ids = { 17, 24, 25, 31, 34, 40, 49, 53, 54, 56, 57, 58, 59,91, 96 };

	public static void setMipmapLevel(int y1, int y2, int y3, int x1, int x2, int x3, int tex) {
		if (!enableMipmapping) {
			if (mipMapLevel != 0) {
				mipMapLevel = 0;
			}
			return;
		}
		for (int tex2 : ids) {
			if (tex == tex2) {
				mipMapLevel = 0;
				return;
			}
		}
		int textureArea = x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2) >> 1;
		if (textureArea < 0) {
			textureArea = -textureArea;
		}
		if (textureArea > 16384) {
			mipMapLevel = 0;
		} else if (textureArea > 4096) {
			mipMapLevel = 1;
		} else if (textureArea > 1024) {
			mipMapLevel = 1;
		} else if (textureArea > 256) {
			mipMapLevel = 2;
		} else if (textureArea > 64) {
			mipMapLevel = 3;
		} else if (textureArea > 16) {
			mipMapLevel = 4;
		} else if (textureArea > 4) {
			mipMapLevel = 5;
		} else if (textureArea > 1) {
			mipMapLevel = 6;
		} else {
			mipMapLevel = 7;
		}
	}

	public static void method366() {
		textureRequestPixelBuffer = null;
		for (int j = 0; j < textureAmount; j++) {
			texturesPixelBuffer[j] = null;
		}
	}

	public static void method367() {
		if (textureRequestPixelBuffer == null) {
			textureRequestBufferPointer = 20;
			textureRequestPixelBuffer = new int[textureRequestBufferPointer][0x10000];
			for (int k = 0; k < textureAmount; k++) {
				texturesPixelBuffer[k] = null;
			}
		}
	}

	public static void method368(StreamLoader streamLoader) {
		textureCount = 0;
		for (int index = 0; index < textureAmount; index++) {
			try {
				textures[index] = new Background(streamLoader, String.valueOf(index), 0);
				if (lowMem && textures[index].anInt1456 == 128) {
					textures[index].method356();
				} else {
					textures[index].method357();
					textures[index].setTransparency(255, 0, 255);
				}
				textureCount++;
				//                System.out.println("Texture amount "+textureCount);
			} catch (Exception ex) {
			}
		}
	}

	public static int method369(int texture) {
		if (averageTextureColours[texture] != 0) {
			return averageTextureColours[texture];
		}
		int r = 0;
		int g = 0;
		int b = 0;
		final int textureColorCount = currentPalette[texture].length;
		for (int index = 0; index < textureColorCount; index++) {
			r += currentPalette[texture][index] >> 16 & 0xff;
		g += currentPalette[texture][index] >> 8 & 0xff;
			b += currentPalette[texture][index] & 0xff;
		}
		int color = (r / textureColorCount << 16) + (g / textureColorCount << 8) + b / textureColorCount;
		color = adjustBrightness(color, 1.3999999999999999D);
		if (color == 0) {
			color = 1;
		}
		averageTextureColours[texture] = color;
		return color;
	}

	public static void method370(int texture) {
		try {
			if (texturesPixelBuffer[texture] == null) {
				return;
			}
			textureRequestPixelBuffer[textureRequestBufferPointer++] = texturesPixelBuffer[texture];
			texturesPixelBuffer[texture] = null;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	public static int getOverallColour(int textureId) {
		if (averageTextureColours[textureId] != 0)
			return averageTextureColours[textureId];
		int totalRed = 0;
		int totalGreen = 0;
		int totalBlue = 0;
		int colourCount = currentPalette[textureId].length;
		for (int ptr = 0; ptr < colourCount; ptr++) {
			totalRed += currentPalette[textureId][ptr] >> 16 & 0xff;
		totalGreen += currentPalette[textureId][ptr] >> 8 & 0xff;
		totalBlue += currentPalette[textureId][ptr] & 0xff;
		}

		int avgPaletteColour = (totalRed / colourCount << 16) + (totalGreen / colourCount << 8) + totalBlue / colourCount;
		avgPaletteColour = adjustBrightness(avgPaletteColour, 1.3999999999999999D);
		if (avgPaletteColour == 0)
			avgPaletteColour = 1;
		averageTextureColours[textureId] = avgPaletteColour;
		return avgPaletteColour;
	}

	public static void requestTextureUpdate(int i) {
		if (i > 50 && i != 59 && i != 96){
			return;
		}
		if (texturesPixelBuffer[i] == null)
			return;
		textureRequestPixelBuffer[textureRequestBufferPointer++] = texturesPixelBuffer[i];
		texturesPixelBuffer[i] = null;
	}

	public static int[] getTexturePixels(int textureId) {
		textureLastUsed[textureId] = lastTextureRetrievalCount++;
		if (texturesPixelBuffer[textureId] != null) {
			return texturesPixelBuffer[textureId];
		}
		int texturePixels[];
		if (textureRequestBufferPointer > 0) {
			texturePixels = textureRequestPixelBuffer[--textureRequestBufferPointer];
			textureRequestPixelBuffer[textureRequestBufferPointer] = null;
		} else {
			int lastUsed = 0;
			int target = -1;
			for (int l = 0; l < textureCount; l++) {
				if (texturesPixelBuffer[l] != null && (textureLastUsed[l] < lastUsed || target == -1)) {
					lastUsed = textureLastUsed[l];
					target = l;
				}
			}

			texturePixels = texturesPixelBuffer[target];
			texturesPixelBuffer[target] = null;
		}
		texturesPixelBuffer[textureId] = texturePixels;
		Background background = textures[textureId];
		int texturePalette[] = currentPalette[textureId];
		if (lowMem) {
			textureIsTransparant[textureId] = false;
			for (int i1 = 0; i1 < 4096; i1++) {
				int colour = texturePixels[i1] = texturePalette[background.palettePixels[i1]] & 0xf8f8ff;
				if (colour == 0) {
					textureIsTransparant[textureId] = true;
				}
				texturePixels[4096 + i1] = colour - (colour >>> 3) & 0xf8f8ff;
				texturePixels[8192 + i1] = colour - (colour >>> 2) & 0xf8f8ff;
				texturePixels[12288 + i1] = colour - (colour >>> 2) - (colour >>> 3) & 0xf8f8ff;
			}

		} else {
			if (background.width == 64) {
				for (int x = 0; x < 128; x++) {
					for (int y = 0; y < 128; y++) {
						texturePixels[y
						              + (x << 7)] = texturePalette[background.palettePixels[(y >> 1) + ((x >> 1) << 6)]];
					}
				}
			} else {
				for (int i = 0; i < 16384; i++) {
					try {
						texturePixels[i] = texturePalette[background.palettePixels[i]];
					} catch (Exception e) {

					}

				}
			}
			textureIsTransparant[textureId] = false;
			for (int i = 0; i < 16384; i++) {
				texturePixels[i] &= 0xf8f8ff;
				int colour = texturePixels[i];
				if (colour == 0) {
					textureIsTransparant[textureId] = true;
				}
				texturePixels[16384 + i] = colour - (colour >>> 3) & 0xf8f8ff;
				texturePixels[32768 + i] = colour - (colour >>> 2) & 0xf8f8ff;
				texturePixels[49152 + i] = colour - (colour >>> 2) - (colour >>> 3) & 0xf8f8ff;
			}

		}
		return texturePixels;
	}

	public static void setBrightness(double d) {
		Texture.setBrightness(d * 2);
		int j = 0;
		for (int k = 0; k < 512; k++) {
			double d1 = k / 8 / 64D + 0.0078125D;
			double d2 = (k & 7) / 8D + 0.0625D;
			for (int k1 = 0; k1 < 128; k1++) {
				double d3 = k1 / 128D;
				double d4 = d3;
				double d5 = d3;
				double d6 = d3;
				if (d2 != 0.0D) {
					double d7;
					if (d3 < 0.5D)
						d7 = d3 * (1.0D + d2);
					else
						d7 = (d3 + d2) - d3 * d2;
					double d8 = 2D * d3 - d7;
					double d9 = d1 + 0.33333333333333331D;
					if (d9 > 1.0D)
						d9--;
					double d10 = d1;
					double d11 = d1 - 0.33333333333333331D;
					if (d11 < 0.0D)
						d11++;
					if (6D * d9 < 1.0D)
						d4 = d8 + (d7 - d8) * 6D * d9;
					else if (2D * d9 < 1.0D)
						d4 = d7;
					else if (3D * d9 < 2D)
						d4 = d8 + (d7 - d8) * (0.66666666666666663D - d9) * 6D;
					else
						d4 = d8;
					if (6D * d10 < 1.0D)
						d5 = d8 + (d7 - d8) * 6D * d10;
					else if (2D * d10 < 1.0D)
						d5 = d7;
					else if (3D * d10 < 2D)
						d5 = d8 + (d7 - d8) * (0.66666666666666663D - d10) * 6D;
					else
						d5 = d8;
					if (6D * d11 < 1.0D)
						d6 = d8 + (d7 - d8) * 6D * d11;
					else if (2D * d11 < 1.0D)
						d6 = d7;
					else if (3D * d11 < 2D)
						d6 = d8 + (d7 - d8) * (0.66666666666666663D - d11) * 6D;
					else
						d6 = d8;
				}
				int byteR = (int) (d4 * 256D);
				int byteG = (int) (d5 * 256D);
				int byteB = (int) (d6 * 256D);
				int rgb = (byteR << 16) + (byteG << 8) + byteB;
				rgb = adjustBrightness(rgb, d);
				if (rgb == 0)
					rgb = 1;
				hslToRgb[j++] = rgb;
			}

		}

		for (int textureId = 0; textureId < textureAmount; textureId++)
			if (textures[textureId] != null) {
				int originalPalette[] = textures[textureId].palette;
				currentPalette[textureId] = new int[originalPalette.length];
				for (int colourId = 0; colourId < originalPalette.length; colourId++) {
					currentPalette[textureId][colourId] = adjustBrightness(originalPalette[colourId], d);
					if ((currentPalette[textureId][colourId] & 0xf8f8ff) == 0 && colourId != 0)
						currentPalette[textureId][colourId] = 1;
				}

			}

		for (int textureId = 0; textureId < textureAmount + 1; textureId++)
			requestTextureUpdate(textureId);
	}
	static int adjustBrightness(int color, double amt) {
		double red = (color >> 16) / 256D;
		double green = (color >> 8 & 0xff) / 256D;
		double blue = (color & 0xff) / 256D;
		red = Math.pow(red, amt);
		green = Math.pow(green, amt);
		blue = Math.pow(blue, amt);
		final int red2 = (int) (red * 256D);
		final int green2 = (int) (green * 256D);
		final int blue2 = (int) (blue * 256D);
		return (red2 << 16) + (green2 << 8) + blue2;
	}

	public static boolean enableHDTextures = false;

	public static void drawMaterializedTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int hsl1, int hsl2,
			int hsl3, int tx1, int tx2, int tx3, int ty1, int ty2, int ty3, int tz1, int tz2, int tz3, int tex, float z1,
			float z2, float z3) {
		// CRITICAL FIX: Clip triangles too close to camera
		if (shouldClipTriangle(z1, z2, z3)) {
			return;
		}

		if (!enableHDTextures || Texture.get(tex) == null) {
			drawGouraudTriangle(y1, y2, y3, x1, x2, x3, hsl1, hsl2, hsl3, z1, z2, z3);
			return;
		}
		setMipmapLevel(y1, y2, y3, x1, x2, x3, tex);
		int[] texels = Texture.get(tex).mipmaps[mipMapLevel];
		tx2 = tx1 - tx2;
		ty2 = ty1 - ty2;
		tz2 = tz1 - tz2;
		tx3 -= tx1;
		ty3 -= ty1;
		tz3 -= tz1;
		int l4 = (tx3 * ty1 - ty3 * tx1) * WorldController.focalLength << 5;
		int i5 = ty3 * tz1 - tz3 * ty1 << 8;
		int j5 = tz3 * tx1 - tx3 * tz1 << 5;
		int k5 = (tx2 * ty1 - ty2 * tx1) * WorldController.focalLength << 5;
		int l5 = ty2 * tz1 - tz2 * ty1 << 8;
		int i6 = tz2 * tx1 - tx2 * tz1 << 5;
		int j6 = (ty2 * tx3 - tx2 * ty3) * WorldController.focalLength << 5;
		int k6 = tz2 * ty3 - ty2 * tz3 << 8;
		int l6 = tx2 * tz3 - tz2 * tx3 << 5;
		int i7 = 0;
		int j7 = 0;
		if (y2 != y1) {
			i7 = (x2 - x1 << 16) / (y2 - y1);
			j7 = (hsl2 - hsl1 << 15) / (y2 - y1);
		}
		int k7 = 0;
		int l7 = 0;
		if (y3 != y2) {
			k7 = (x3 - x2 << 16) / (y3 - y2);
			l7 = (hsl3 - hsl2 << 15) / (y3 - y2);
		}
		int i8 = 0;
		int j8 = 0;
		if (y3 != y1) {
			i8 = (x1 - x3 << 16) / (y1 - y3);
			j8 = (hsl1 - hsl3 << 15) / (y1 - y3);
		}

		float x21 = x2 - x1;
		float y32 = y2 - y1;
		float x31 = x3 - x1;
		float y31 = y3 - y1;
		float z21 = z2 - z1;
		float z31 = z3 - z1;

		float div = x21 * y31 - x31 * y32;
		float depthSlope = (z21 * y31 - z31 * y32) / div;
		float depthScale = (z31 * x21 - z21 * x31) / div;

		if (y1 <= y2 && y1 <= y3) {
			if (y1 >= DrawingArea.bottomY) {
				return;
			}
			if (y2 > DrawingArea.bottomY) {
				y2 = DrawingArea.bottomY;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			z1 = z1 - depthSlope * x1 + depthSlope;
			if (y2 < y3) {
				x3 = x1 <<= 16;
				hsl3 = hsl1 <<= 15;
				if (y1 < 0) {
					x3 -= i8 * y1;
					x1 -= i7 * y1;
					z1 -= depthScale * y1;
					hsl3 -= j8 * y1;
					hsl1 -= j7 * y1;
					y1 = 0;
				}
				x2 <<= 16;
				hsl2 <<= 15;
				if (y2 < 0) {
					x2 -= k7 * y2;
					hsl2 -= l7 * y2;
					y2 = 0;
				}
				int k8 = y1 - textureInt2;
				l4 += j5 * k8;
				k5 += i6 * k8;
				j6 += l6 * k8;
				if (y1 != y2 && i8 < i7 || y1 == y2 && i8 > k7) {
					y3 -= y2;
					y2 -= y1;
					y1 = anIntArray1472[y1];
					while (--y2 >= 0) {
						drawMaterializedScanline(DrawingArea.pixels, texels, y1, x3 >> 16, x1 >> 16, hsl3 >> 7,
						hsl1 >> 7, l4, k5, j6, i5, l5, k6, z1, depthSlope);
						x3 += i8;
						x1 += i7;
						z1 += depthScale;
						hsl3 += j8;
						hsl1 += j7;
						y1 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y3 >= 0) {
						drawMaterializedScanline(DrawingArea.pixels, texels, y1, x3 >> 16, x2 >> 16, hsl3 >> 7,
					hsl2 >> 7, l4, k5, j6, i5, l5, k6, z1, depthSlope);
						x3 += i8;
						x2 += k7;
						z1 += depthScale;
						hsl3 += j8;
						hsl2 += l7;
						y1 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					texels = null;
					return;
				}
				y3 -= y2;
				y2 -= y1;
				y1 = anIntArray1472[y1];
				while (--y2 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y1, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7,
				l4, k5, j6, i5, l5, k6, z1, depthSlope);
					x3 += i8;
					x1 += i7;
					z1 += depthScale;
					hsl3 += j8;
					hsl1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y3 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y1, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7,
				l4, k5, j6, i5, l5, k6, z1, depthSlope);
					x3 += i8;
					x2 += k7;
					z1 += depthScale;
					hsl3 += j8;
					hsl2 += l7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				texels = null;
				return;
			}
			x2 = x1 <<= 16;
			hsl2 = hsl1 <<= 15;
			if (y1 < 0) {
				x2 -= i8 * y1;
				x1 -= i7 * y1;
				z1 -= depthScale * y1;
				hsl2 -= j8 * y1;
				hsl1 -= j7 * y1;
				y1 = 0;
			}
			x3 <<= 16;
			hsl3 <<= 15;
			if (y3 < 0) {
				x3 -= k7 * y3;
				hsl3 -= l7 * y3;
				y3 = 0;
			}
			int l8 = y1 - textureInt2;
			l4 += j5 * l8;
			k5 += i6 * l8;
			j6 += l6 * l8;
			if (y1 != y3 && i8 < i7 || y1 == y3 && k7 > i7) {
				y2 -= y3;
				y3 -= y1;
				y1 = anIntArray1472[y1];
				while (--y3 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y1, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7,
					l4, k5, j6, i5, l5, k6, z1, depthSlope);
					x2 += i8;
					x1 += i7;
					z1 += depthScale;
					hsl2 += j8;
					hsl1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y2 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y1, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7,
				l4, k5, j6, i5, l5, k6, z1, depthSlope);
					x3 += k7;
					x1 += i7;
					z1 += depthScale;
					hsl3 += l7;
					hsl1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				texels = null;
				return;
			}
			y2 -= y3;
			y3 -= y1;
			y1 = anIntArray1472[y1];
			while (--y3 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y1, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, l4,
							k5, j6, i5, l5, k6, z1, depthSlope);
				x2 += i8;
				x1 += i7;
				z1 += depthScale;
				hsl2 += j8;
				hsl1 += j7;
				y1 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y2 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y1, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, l4,
				k5, j6, i5, l5, k6, z1, depthSlope);
				x3 += k7;
				x1 += i7;
				z1 += depthScale;
				hsl3 += l7;
				hsl1 += j7;
				y1 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			texels = null;
			return;
		}
		if (y2 <= y3) {
			if (y2 >= DrawingArea.bottomY) {
				return;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			if (y1 > DrawingArea.bottomY) {
				y1 = DrawingArea.bottomY;
			}
			z2 = z2 - depthSlope * x2 + depthSlope;
			if (y3 < y1) {
				x1 = x2 <<= 16;
				hsl1 = hsl2 <<= 15;
				if (y2 < 0) {
					x1 -= i7 * y2;
					x2 -= k7 * y2;
					z2 -= depthScale * y2;
					hsl1 -= j7 * y2;
					hsl2 -= l7 * y2;
					y2 = 0;
				}
				x3 <<= 16;
				hsl3 <<= 15;
				if (y3 < 0) {
					x3 -= i8 * y3;
					hsl3 -= j8 * y3;
					y3 = 0;
				}
				int i9 = y2 - textureInt2;
				l4 += j5 * i9;
				k5 += i6 * i9;
				j6 += l6 * i9;
				if (y2 != y3 && i7 < k7 || y2 == y3 && i7 > i8) {
					y1 -= y3;
					y3 -= y2;
					y2 = anIntArray1472[y2];
					while (--y3 >= 0) {
						drawMaterializedScanline(DrawingArea.pixels, texels, y2, x1 >> 16, x2 >> 16, hsl1 >> 7,
						hsl2 >> 7, l4, k5, j6, i5, l5, k6, z2, depthSlope);
						x1 += i7;
						x2 += k7;
						z2 += depthScale;
						hsl1 += j7;
						hsl2 += l7;
						y2 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y1 >= 0) {
						drawMaterializedScanline(DrawingArea.pixels, texels, y2, x1 >> 16, x3 >> 16, hsl1 >> 7,
					hsl3 >> 7, l4, k5, j6, i5, l5, k6, z2, depthSlope);
						x1 += i7;
						x3 += i8;
						z2 += depthScale;
						hsl1 += j7;
						hsl3 += j8;
						y2 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					texels = null;
					return;
				}
				y1 -= y3;
				y3 -= y2;
				y2 = anIntArray1472[y2];
				while (--y3 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y2, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7,
							l4, k5, j6, i5, l5, k6, z2, depthSlope);
					x1 += i7;
					x2 += k7;
					z2 += depthScale;
					hsl1 += j7;
					hsl2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y1 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y2, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7,
							l4, k5, j6, i5, l5, k6, z2, depthSlope);
					x1 += i7;
					x3 += i8;
					z2 += depthScale;
					hsl1 += j7;
					hsl3 += j8;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				texels = null;
				return;
			}
			x3 = x2 <<= 16;
			hsl3 = hsl2 <<= 15;
			if (y2 < 0) {
				x3 -= i7 * y2;
				x2 -= k7 * y2;
				z2 -= depthScale * y2;
				hsl3 -= j7 * y2;
				hsl2 -= l7 * y2;
				y2 = 0;
			}
			x1 <<= 16;
			hsl1 <<= 15;
			if (y1 < 0) {
				x1 -= i8 * y1;
				hsl1 -= j8 * y1;
				y1 = 0;
			}
			int j9 = y2 - textureInt2;
			l4 += j5 * j9;
			k5 += i6 * j9;
			j6 += l6 * j9;
			if (i7 < k7) {
				y3 -= y1;
				y1 -= y2;
				y2 = anIntArray1472[y2];
				while (--y1 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y2, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7,
					l4, k5, j6, i5, l5, k6, z2, depthSlope);
					x3 += i7;
					x2 += k7;
					z2 += depthScale;
					hsl3 += j7;
					hsl2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y3 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y2, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7,
					l4, k5, j6, i5, l5, k6, z2, depthSlope);
					x1 += i8;
					x2 += k7;
					z2 += depthScale;
					hsl1 += j8;
					hsl2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				texels = null;
				return;
			}
			y3 -= y1;
			y1 -= y2;
			y2 = anIntArray1472[y2];
			while (--y1 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y2, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, l4,
						k5, j6, i5, l5, k6, z2, depthSlope);
				x3 += i7;
				x2 += k7;
				z2 += depthScale;
				hsl3 += j7;
				hsl2 += l7;
				y2 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y3 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y2, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, l4,
						k5, j6, i5, l5, k6, z2, depthSlope);
				x1 += i8;
				x2 += k7;
				z2 += depthScale;
				hsl1 += j8;
				hsl2 += l7;
				y2 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			texels = null;
			return;
		}
		if (y3 >= DrawingArea.bottomY) {
			return;
		}
		if (y1 > DrawingArea.bottomY) {
			y1 = DrawingArea.bottomY;
		}
		if (y2 > DrawingArea.bottomY) {
			y2 = DrawingArea.bottomY;
		}
		z3 = z3 - depthSlope * x3 + depthSlope;
		if (y1 < y2) {
			x2 = x3 <<= 16;
			hsl2 = hsl3 <<= 15;
			if (y3 < 0) {
				x2 -= k7 * y3;
				x3 -= i8 * y3;
				z3 -= depthScale * y3;
				hsl2 -= l7 * y3;
				hsl3 -= j8 * y3;
				y3 = 0;
			}
			x1 <<= 16;
			hsl1 <<= 15;
			if (y1 < 0) {
				x1 -= i7 * y1;
				hsl1 -= j7 * y1;
				y1 = 0;
			}
			int k9 = y3 - textureInt2;
			l4 += j5 * k9;
			k5 += i6 * k9;
			j6 += l6 * k9;
			if (k7 < i8) {
				y2 -= y1;
				y1 -= y3;
				y3 = anIntArray1472[y3];
				while (--y1 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y3, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7,
					l4, k5, j6, i5, l5, k6, z3, depthSlope);
					x2 += k7;
					x3 += i8;
					z3 += depthScale;
					hsl2 += l7;
					hsl3 += j8;
					y3 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y2 >= 0) {
					drawMaterializedScanline(DrawingArea.pixels, texels, y3, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7,
					l4, k5, j6, i5, l5, k6, z3, depthSlope);
					x2 += k7;
					x1 += i7;
					z3 += depthScale;
					hsl2 += l7;
					hsl1 += j7;
					y3 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				texels = null;
				return;
			}
			y2 -= y1;
			y1 -= y3;
			y3 = anIntArray1472[y3];
			while (--y1 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y3, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, l4,
						k5, j6, i5, l5, k6, z3, depthSlope);
				x2 += k7;
				x3 += i8;
				z3 += depthScale;
				hsl2 += l7;
				hsl3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y2 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y3, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, l4,
						k5, j6, i5, l5, k6, z3, depthSlope);
				x2 += k7;
				x1 += i7;
				z3 += depthScale;
				hsl2 += l7;
				hsl1 += j7;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			texels = null;
			return;
		}
		x1 = x3 <<= 16;
		hsl1 = hsl3 <<= 15;
		if (y3 < 0) {
			x1 -= k7 * y3;
			x3 -= i8 * y3;
			z3 -= depthScale * y3;
			hsl1 -= l7 * y3;
			hsl3 -= j8 * y3;
			y3 = 0;
		}
		x2 <<= 16;
		hsl2 <<= 15;
		if (y2 < 0) {
			x2 -= i7 * y2;
			hsl2 -= j7 * y2;
			y2 = 0;
		}
		int l9 = y3 - textureInt2;
		l4 += j5 * l9;
		k5 += i6 * l9;
		j6 += l6 * l9;
		if (k7 < i8) {
			y1 -= y2;
			y2 -= y3;
			y3 = anIntArray1472[y3];
			while (--y2 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y3, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, l4,
				k5, j6, i5, l5, k6, z3, depthSlope);
				x1 += k7;
				x3 += i8;
				z3 += depthScale;
				hsl1 += l7;
				hsl3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y1 >= 0) {
				drawMaterializedScanline(DrawingArea.pixels, texels, y3, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, l4,
				k5, j6, i5, l5, k6, z3, depthSlope);
				x2 += i7;
				x3 += i8;
				z3 += depthScale;
				hsl2 += j7;
				hsl3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			texels = null;
			return;
		}
		y1 -= y2;
		y2 -= y3;
		y3 = anIntArray1472[y3];
		while (--y2 >= 0) {
			drawMaterializedScanline(DrawingArea.pixels, texels, y3, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, l4, k5,
					j6, i5, l5, k6, z3, depthSlope);
			x1 += k7;
			x3 += i8;
			z3 += depthScale;
			hsl1 += l7;
			hsl3 += j8;
			y3 += DrawingArea.width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
		while (--y1 >= 0) {
			drawMaterializedScanline(DrawingArea.pixels, texels, y3, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, l4, k5,
					j6, i5, l5, k6, z3, depthSlope);
			x2 += i7;
			x3 += i8;
			z3 += depthScale;
			hsl2 += j7;
			hsl3 += j8;
			y3 += DrawingArea.width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
		texels = null;
	}

	private static final void drawMaterializedScanline(int[] dest, int[] texels, int offset, int x1, int x2, int hsl1,
			int hsl2, int t1, int t2, int t3, int t4, int t5, int t6, float z1, float z2) {
		if (x2 <= x1) {
			return;
		}

		int texPos = 0;
		int rgb = 0;

		if (textureOutOfDrawingBounds) {
			if (x2 > DrawingArea.lastX) {
				x2 = DrawingArea.lastX;
			}
			if (x1 < 0) {
				// CRITICAL FIX: Adjust ALL parameters when clipping
				int clipAmount = -x1;
				hsl1 += ((hsl2 - hsl1) * clipAmount) / (x2 - x1);
				t1 += (t4 >> 3) * clipAmount;
				t2 += (t5 >> 3) * clipAmount;
				t3 += (t6 >> 3) * clipAmount;
				z1 += z2 * clipAmount;
				x1 = 0;
			}
		}

		if (x1 >= x2) {
			return;
		}

		offset += x1;

		// Bounds checking
		if (offset < 0 || offset >= dest.length) {
			return;
		}

		int n = x2 - x1;
		int maxPixels = dest.length - offset;
		if (n > maxPixels) {
			n = maxPixels;
		}

		if (n <= 0) {
			return;
		}

		int dhsl = 0;
		if (n > 0) {
			dhsl = (hsl2 - hsl1) / n;
		}

		int dist = x1 - textureInt1;
		t1 += (t4 >> 3) * dist;
		t2 += (t5 >> 3) * dist;
		t3 += (t6 >> 3) * dist;

		// CRITICAL FIX: Better perspective divide with clamping
		int i_57_ = t3 >> 14;
		int i_58_ = 0;
		int i_59_ = 0;

		if (i_57_ > 16) { // Prevent extreme values
			i_58_ = t1 / i_57_;
			i_59_ = t2 / i_57_;
			// Clamp to valid texture coordinate range
			if (i_58_ < 0) i_58_ = 0;
			if (i_58_ > 16383) i_58_ = 16383;
			if (i_59_ < 0) i_59_ = 0;
			if (i_59_ > 16383) i_59_ = 16383;
		}

		t1 += t4;
		t2 += t5;
		t3 += t6;
		i_57_ = t3 >> 14;

		int i_60_ = 0;
		int i_61_ = 0;

		if (i_57_ > 16) {
			i_60_ = t1 / i_57_;
			i_61_ = t2 / i_57_;
			if (i_60_ < 0) i_60_ = 0;
			if (i_60_ > 16383) i_60_ = 16383;
			if (i_61_ < 0) i_61_ = 0;
			if (i_61_ > 16383) i_61_ = 16383;
		}

		texPos = (i_58_ << 18) + i_59_;
		int dtexPos = ((i_60_ - i_58_) << 15) + ((i_61_ - i_59_) << 15);

		int light;
		int updateCounter = 0;

		// Main rendering loop
		for (int i = 0; i < n; i++) {
			if (offset >= dest.length) break;

			// CRITICAL FIX: Better texture coordinate extraction
			int texX = (texPos >>> 25);
			int texY = (texPos >> 7) & 0x7F;

			// Clamp texture coordinates
			if (texX < 0) texX = 0;
			if (texX > 127) texX = 127;
			if (texY < 0) texY = 0;
			if (texY > 127) texY = 127;

			int texIndex = texelPos((texY << 7) + texX);

			// Bounds check texture array
			if (texIndex >= 0 && texIndex < texels.length) {
				rgb = texels[texIndex];

				// Calculate lighting with better clamping
				int hslValue = hsl1 >> 8;
			if (hslValue < 0) hslValue = 0;
			if (hslValue > 65535) hslValue = 65535;

			int brightness = (hslValue >> 7) & 0xFF;

			// Calculate average RGB for lighting
			int avgColor = ((rgb >> 16 & 0xff) + (rgb >> 8 & 0xff) + (rgb & 0xff)) / 3;
			light = (brightness * avgColor) / 384;

			if (light < 0) light = 0;
			if (light > 127) light = 127;

			// Bounds check hslToRgb lookup
			int colorIndex = ((hslValue >> 7) & 0xFF80) | light;
			if (colorIndex < 0) colorIndex = 0;
			if (colorIndex >= hslToRgb.length) colorIndex = hslToRgb.length - 1;

			dest[offset] = hslToRgb[colorIndex];

			if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
				depthBuffer[offset] = z1;
			}
			}

			z1 += z2;
			texPos += dtexPos;
			hsl1 += dhsl;
			offset++;
			updateCounter++;

			// Update texture coordinates every 8 pixels for better accuracy
			if (updateCounter >= 8) {
				updateCounter = 0;

				i_58_ = i_60_;
				i_59_ = i_61_;
				t1 += t4;
				t2 += t5;
				t3 += t6;
				i_57_ = t3 >> 14;

				if (i_57_ > 16) {
					i_60_ = t1 / i_57_;
					i_61_ = t2 / i_57_;
					if (i_60_ < 0) i_60_ = 0;
					if (i_60_ > 16383) i_60_ = 16383;
					if (i_61_ < 0) i_61_ = 0;
					if (i_61_ > 16383) i_61_ = 16383;
				}

				texPos = (i_58_ << 18) + i_59_;
				dtexPos = ((i_60_ - i_58_) << 15) + ((i_61_ - i_59_) << 15);
			}
		}

		texels = dest = null;
	}

	public static void drawGouraudTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int hsl1, int hsl2, int hsl3,
			float z1, float z2, float z3) {
		// CRITICAL FIX: Clip triangles too close to camera
		if (shouldClipTriangle(z1, z2, z3)) {
			return;
		}

		if (Client.getUserSettings().isSmoothShading() && aBoolean1464) {
			drawHDGouraudTriangle(y1, y2, y3, x1, x2, x3, hsl1, hsl2, hsl3, z1, z2, z3);
		} else {
			drawLDGouraudTriangle(y1, y2, y3, x1, x2, x3, hsl1, hsl2, hsl3, z1, z2, z3);
		}
	}
	public static void drawLDGouraudTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int hsl1, int hsl2,
	        int hsl3, float z1, float z2, float z3) {
	    
	    // CRITICAL FIX: Clip triangles too close to camera
	    if (shouldClipTriangle(z1, z2, z3)) {
	        return;
	    }
	    
	    // Debug first few triangles
	    if (z1 < 5000 && z2 < 5000 && z3 < 5000) {
	        //System.out.println("Triangle - z1=" + z1 + ", z2=" + z2 + ", z3=" + z3);
	    }
		if (!saveDepth) {
			z1 = z2 = z3 = 0;
		}
		int dx1 = 0;
		int dhsl1 = 0;
		if (y2 != y1) {
			int dy = y2 - y1;
			dx1 = (x2 - x1 << 16) / dy;
			dhsl1 = (hsl2 - hsl1 << 15) / dy;
		}
		int dx2 = 0;
		int dhsl2 = 0;
		if (y3 != y2) {
			int dy = y3 - y2;
			dx2 = (x3 - x2 << 16) / dy;
			dhsl2 = (hsl3 - hsl2 << 15) / dy;
		}
		int dx3 = 0;
		int dhsl3 = 0;
		if (y3 != y1) {
			int dy = y1 - y3;
			dx3 = (x1 - x3 << 16) / dy;
			dhsl3 = (hsl1 - hsl3 << 15) / dy;
		}

		float x21 = x2 - x1;
		float y32 = y2 - y1;
		float x31 = x3 - x1;
		float y31 = y3 - y1;
		float z21 = z2 - z1;
		float z31 = z3 - z1;

		float div = x21 * y31 - x31 * y32;
		if (div == 0) return; // CRITICAL FIX: Prevent division by zero

		float depthSlope = (z21 * y31 - z31 * y32) / div;
		float depthScale = (z31 * x21 - z21 * x31) / div;

		if (y1 <= y2 && y1 <= y3) {
			if (y1 >= DrawingArea.bottomY) {
				return;
			}
			if (y2 > DrawingArea.bottomY) {
				y2 = DrawingArea.bottomY;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			z1 = z1 - depthSlope * x1 + depthSlope;
			if (y2 < y3) {
				x3 = x1 <<= 16;
				hsl3 = hsl1 <<= 15;
				if (y1 < 0) {
					x3 -= dx3 * y1;
					x1 -= dx1 * y1;
					z1 -= depthScale * y1;
					hsl3 -= dhsl3 * y1;
					hsl1 -= dhsl1 * y1;
					y1 = 0;
				}
				x2 <<= 16;
				hsl2 <<= 15;
				if (y2 < 0) {
					x2 -= dx2 * y2;
					hsl2 -= dhsl2 * y2;
					y2 = 0;
				}
				if (y1 != y2 && dx3 < dx1 || y1 == y2 && dx3 > dx2) {
					y3 -= y2;
					y2 -= y1;
					// CRITICAL FIX: Bounds check
					if (y1 < 0 || y1 >= anIntArray1472.length) {
						return;
					}
					for (y1 = anIntArray1472[y1]; --y2 >= 0; y1 += DrawingArea.width) {
						drawLDGouraudScanline(DrawingArea.pixels, y1, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z1,
						depthSlope);
						z1 += depthScale;
						x3 += dx3;
						x1 += dx1;
						hsl3 += dhsl3;
						hsl1 += dhsl1;
					}
					while (--y3 >= 0) {
						drawLDGouraudScanline(DrawingArea.pixels, y1, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z1,
					depthSlope);
						z1 += depthScale;
						x3 += dx3;
						x2 += dx2;
						hsl3 += dhsl3;
						hsl2 += dhsl2;
						y1 += DrawingArea.width;
					}
					return;
				}
				y3 -= y2;
				y2 -= y1;
				if (y1 < 0 || y1 >= anIntArray1472.length) {
					return;
				}
				for (y1 = anIntArray1472[y1]; --y2 >= 0; y1 += DrawingArea.width) {
					drawLDGouraudScanline(DrawingArea.pixels, y1, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z1,
					depthSlope);
					z1 += depthScale;
					x3 += dx3;
					x1 += dx1;
					hsl3 += dhsl3;
					hsl1 += dhsl1;
				}
				while (--y3 >= 0) {
					drawLDGouraudScanline(DrawingArea.pixels, y1, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z1,
							depthSlope);
					z1 += depthScale;
					x3 += dx3;
					x2 += dx2;
					hsl3 += dhsl3;
					hsl2 += dhsl2;
					y1 += DrawingArea.width;
				}
				return;
			}
			x2 = x1 <<= 16;
			hsl2 = hsl1 <<= 15;
			if (y1 < 0) {
				x2 -= dx3 * y1;
				x1 -= dx1 * y1;
				z1 -= depthScale * y1;
				hsl2 -= dhsl3 * y1;
				hsl1 -= dhsl1 * y1;
				y1 = 0;
			}
			x3 <<= 16;
			hsl3 <<= 15;
			if (y3 < 0) {
				x3 -= dx2 * y3;
				hsl3 -= dhsl2 * y3;
				y3 = 0;
			}
			if (y1 != y3 && dx3 < dx1 || y1 == y3 && dx2 > dx1) {
				y2 -= y3;
				y3 -= y1;
				if (y1 < 0 || y1 >= anIntArray1472.length) {
					return;
				}
				for (y1 = anIntArray1472[y1]; --y3 >= 0; y1 += DrawingArea.width) {
					drawLDGouraudScanline(DrawingArea.pixels, y1, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z1,
					depthSlope);
					z1 += depthScale;
					x2 += dx3;
					x1 += dx1;
					hsl2 += dhsl3;
					hsl1 += dhsl1;
				}
				while (--y2 >= 0) {
					drawLDGouraudScanline(DrawingArea.pixels, y1, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z1,
							depthSlope);
					z1 += depthScale;
					x3 += dx2;
					x1 += dx1;
					hsl3 += dhsl2;
					hsl1 += dhsl1;
					y1 += DrawingArea.width;
				}
				return;
			}
			y2 -= y3;
			y3 -= y1;
			if (y1 < 0 || y1 >= anIntArray1472.length) {
				return;
			}
			for (y1 = anIntArray1472[y1]; --y3 >= 0; y1 += DrawingArea.width) {
				drawLDGouraudScanline(DrawingArea.pixels, y1, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z1, depthSlope);
				z1 += depthScale;
				x2 += dx3;
				x1 += dx1;
				hsl2 += dhsl3;
				hsl1 += dhsl1;
			}
			while (--y2 >= 0) {
				drawLDGouraudScanline(DrawingArea.pixels, y1, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z1, depthSlope);
				z1 += depthScale;
				x3 += dx2;
				x1 += dx1;
				hsl3 += dhsl2;
				hsl1 += dhsl1;
				y1 += DrawingArea.width;
			}
			return;
		}
		if (y2 <= y3) {
			if (y2 >= DrawingArea.bottomY) {
				return;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			if (y1 > DrawingArea.bottomY) {
				y1 = DrawingArea.bottomY;
			}
			z2 = z2 - depthSlope * x2 + depthSlope;
			if (y3 < y1) {
				x1 = x2 <<= 16;
				hsl1 = hsl2 <<= 15;
				if (y2 < 0) {
					x1 -= dx1 * y2;
					x2 -= dx2 * y2;
					z2 -= depthScale * y2;
					hsl1 -= dhsl1 * y2;
					hsl2 -= dhsl2 * y2;
					y2 = 0;
				}
				x3 <<= 16;
				hsl3 <<= 15;
				if (y3 < 0) {
					x3 -= dx3 * y3;
					hsl3 -= dhsl3 * y3;
					y3 = 0;
				}
				if (y2 != y3 && dx1 < dx2 || y2 == y3 && dx1 > dx3) {
					y1 -= y3;
					y3 -= y2;
					if (y2 < 0 || y2 >= anIntArray1472.length) {
						return;
					}
					for (y2 = anIntArray1472[y2]; --y3 >= 0; y2 += DrawingArea.width) {
						drawLDGouraudScanline(DrawingArea.pixels, y2, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z2,
						depthSlope);
						z2 += depthScale;
						x1 += dx1;
						x2 += dx2;
						hsl1 += dhsl1;
						hsl2 += dhsl2;
					}

					while (--y1 >= 0) {
						drawLDGouraudScanline(DrawingArea.pixels, y2, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z2,
					depthSlope);
						z2 += depthScale;
						x1 += dx1;
						x3 += dx3;
						hsl1 += dhsl1;
						hsl3 += dhsl3;
						y2 += DrawingArea.width;
					}
					return;
				}
				y1 -= y3;
				y3 -= y2;
				if (y2 < 0 || y2 >= anIntArray1472.length) {
					return;
				}
				for (y2 = anIntArray1472[y2]; --y3 >= 0; y2 += DrawingArea.width) {
					drawLDGouraudScanline(DrawingArea.pixels, y2, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z2,
							depthSlope);
					z2 += depthScale;
					x1 += dx1;
					x2 += dx2;
					hsl1 += dhsl1;
					hsl2 += dhsl2;
				}

				while (--y1 >= 0) {
					drawLDGouraudScanline(DrawingArea.pixels, y2, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z2,
							depthSlope);
					z2 += depthScale;
					x1 += dx1;
					x3 += dx3;
					hsl1 += dhsl1;
					hsl3 += dhsl3;
					y2 += DrawingArea.width;
				}
				return;
			}
			x3 = x2 <<= 16;
			hsl3 = hsl2 <<= 15;
			if (y2 < 0) {
				x3 -= dx1 * y2;
				x2 -= dx2 * y2;
				z2 -= depthScale * y2;
				hsl3 -= dhsl1 * y2;
				hsl2 -= dhsl2 * y2;
				y2 = 0;
			}
			x1 <<= 16;
			hsl1 <<= 15;
			if (y1 < 0) {
				x1 -= dx3 * y1;
				hsl1 -= dhsl3 * y1;
				y1 = 0;
			}
			if (dx1 < dx2) {
				y3 -= y1;
				y1 -= y2;
				if (y2 < 0 || y2 >= anIntArray1472.length) {
					return;
				}
				for (y2 = anIntArray1472[y2]; --y1 >= 0; y2 += DrawingArea.width) {
					drawLDGouraudScanline(DrawingArea.pixels, y2, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z2,
					depthSlope);
					z2 += depthScale;
					x3 += dx1;
					x2 += dx2;
					hsl3 += dhsl1;
					hsl2 += dhsl2;
				}
				while (--y3 >= 0) {
					drawLDGouraudScanline(DrawingArea.pixels, y2, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z2,
					depthSlope);
					z2 += depthScale;
					x1 += dx3;
					x2 += dx2;
					hsl1 += dhsl3;
					hsl2 += dhsl2;
					y2 += DrawingArea.width;
				}
				return;
			}
			y3 -= y1;
			y1 -= y2;
			if (y2 < 0 || y2 >= anIntArray1472.length) {
				return;
			}
			for (y2 = anIntArray1472[y2]; --y1 >= 0; y2 += DrawingArea.width) {
				drawLDGouraudScanline(DrawingArea.pixels, y2, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z2, depthSlope);
				z2 += depthScale;
				x3 += dx1;
				x2 += dx2;
				hsl3 += dhsl1;
				hsl2 += dhsl2;
			}

			while (--y3 >= 0) {
				drawLDGouraudScanline(DrawingArea.pixels, y2, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z2, depthSlope);
				z2 += depthScale;
				x1 += dx3;
				x2 += dx2;
				hsl1 += dhsl3;
				hsl2 += dhsl2;
				y2 += DrawingArea.width;
			}
			return;
		}
		if (y3 >= DrawingArea.bottomY) {
			return;
		}
		if (y1 > DrawingArea.bottomY) {
			y1 = DrawingArea.bottomY;
		}
		if (y2 > DrawingArea.bottomY) {
			y2 = DrawingArea.bottomY;
		}
		z3 = z3 - depthSlope * x3 + depthSlope;
		if (y1 < y2) {
			x2 = x3 <<= 16;
			hsl2 = hsl3 <<= 15;
			if (y3 < 0) {
				x2 -= dx2 * y3;
				x3 -= dx3 * y3;
				z3 -= depthScale * y3;
				hsl2 -= dhsl2 * y3;
				hsl3 -= dhsl3 * y3;
				y3 = 0;
			}
			x1 <<= 16;
			hsl1 <<= 15;
			if (y1 < 0) {
				x1 -= dx1 * y1;
				hsl1 -= dhsl1 * y1;
				y1 = 0;
			}
			if (dx2 < dx3) {
				y2 -= y1;
				y1 -= y3;
				if (y3 < 0 || y3 >= anIntArray1472.length) {
					return;
				}
				for (y3 = anIntArray1472[y3]; --y1 >= 0; y3 += DrawingArea.width) {
					drawLDGouraudScanline(DrawingArea.pixels, y3, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z3,
					depthSlope);
					z3 += depthScale;
					x2 += dx2;
					x3 += dx3;
					hsl2 += dhsl2;
					hsl3 += dhsl3;
				}
				while (--y2 >= 0) {
					drawLDGouraudScanline(DrawingArea.pixels, y3, x2 >> 16, x1 >> 16, hsl2 >> 7, hsl1 >> 7, z3,
					depthSlope);
					z3 += depthScale;
					x2 += dx2;
					x1 += dx1;
					hsl2 += dhsl2;
					hsl1 += dhsl1;
					y3 += DrawingArea.width;
				}
				return;
			}
			y2 -= y1;
			y1 -= y3;
			if (y3 < 0 || y3 >= anIntArray1472.length) {
				return;
			}
			for (y3 = anIntArray1472[y3]; --y1 >= 0; y3 += DrawingArea.width) {
				drawLDGouraudScanline(DrawingArea.pixels, y3, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z3, depthSlope);
				z3 += depthScale;
				x2 += dx2;
				x3 += dx3;
				hsl2 += dhsl2;
				hsl3 += dhsl3;
			}

			while (--y2 >= 0) {
				drawLDGouraudScanline(DrawingArea.pixels, y3, x1 >> 16, x2 >> 16, hsl1 >> 7, hsl2 >> 7, z3, depthSlope);
				z3 += depthScale;
				x2 += dx2;
				x1 += dx1;
				hsl2 += dhsl2;
				hsl1 += dhsl1;
				y3 += DrawingArea.width;
			}
			return;
		}
		x1 = x3 <<= 16;
		hsl1 = hsl3 <<= 15;
		if (y3 < 0) {
			x1 -= dx2 * y3;
			x3 -= dx3 * y3;
			z3 -= depthScale * y3;
			hsl1 -= dhsl2 * y3;
			hsl3 -= dhsl3 * y3;
			y3 = 0;
		}
		x2 <<= 16;
		hsl2 <<= 15;
		if (y2 < 0) {
			x2 -= dx1 * y2;
			hsl2 -= dhsl1 * y2;
			y2 = 0;
		}
		if (dx2 < dx3) {
			y1 -= y2;
			y2 -= y3;
			if (y3 < 0 || y3 >= anIntArray1472.length) {
				return;
			}
			for (y3 = anIntArray1472[y3]; --y2 >= 0; y3 += DrawingArea.width) {
				drawLDGouraudScanline(DrawingArea.pixels, y3, x1 >> 16, x3 >> 16, hsl1 >> 7, hsl3 >> 7, z3, depthSlope);
				z3 += depthScale;
				x1 += dx2;
				x3 += dx3;
				hsl1 += dhsl2;
				hsl3 += dhsl3;
			}
			while (--y1 >= 0) {
				drawLDGouraudScanline(DrawingArea.pixels, y3, x2 >> 16, x3 >> 16, hsl2 >> 7, hsl3 >> 7, z3, depthSlope);
				z3 += depthScale;
				x2 += dx1;
				x3 += dx3;
				hsl2 += dhsl1;
				hsl3 += dhsl3;
				y3 += DrawingArea.width;
			}
			return;
		}
		y1 -= y2;
		y2 -= y3;
		if (y3 < 0 || y3 >= anIntArray1472.length) {
			return;
		}
		for (y3 = anIntArray1472[y3]; --y2 >= 0; y3 += DrawingArea.width) {
			drawLDGouraudScanline(DrawingArea.pixels, y3, x3 >> 16, x1 >> 16, hsl3 >> 7, hsl1 >> 7, z3, depthSlope);
			z3 += depthScale;
			x1 += dx2;
			x3 += dx3;
			hsl1 += dhsl2;
			hsl3 += dhsl3;
		}
		while (--y1 >= 0) {
			drawLDGouraudScanline(DrawingArea.pixels, y3, x3 >> 16, x2 >> 16, hsl3 >> 7, hsl2 >> 7, z3, depthSlope);
			z3 += depthScale;
			x2 += dx1;
			x3 += dx3;
			hsl2 += dhsl1;
			hsl3 += dhsl3;
			y3 += DrawingArea.width;
		}
	}
	private static void drawLDGouraudScanline(int dest[], int offset, int x1, int x2, int hsl1, int hsl2, float z1,
			float z2) {
		if (x1 >= x2) {
			return;
		}

		int rgb;
		int dhsl;

		if (aBoolean1464) {
			// Calculate color delta
			if (textureOutOfDrawingBounds) {
				if (x2 - x1 > 3) {
					dhsl = (hsl2 - hsl1) / (x2 - x1);
				} else {
					dhsl = 0;
				}
				if (x2 > DrawingArea.lastX) {
					x2 = DrawingArea.lastX;
				}
				if (x1 < 0) {
					hsl1 -= x1 * dhsl;
					z1 -= x1 * z2;
					x1 = 0;
				}
				if (x1 >= x2) {
					return;
				}
			} else {
				if (x2 > DrawingArea.lastX) {
					x2 = DrawingArea.lastX;
				}
				if (x1 < 0) {
					x1 = 0;
				}
				if (x1 >= x2) {
					return;
				}
				dhsl = (x2 - x1 > 0) ? ((hsl2 - hsl1) / (x2 - x1)) : 0;
			}

			// Bounds checking
			offset += x1;
			z1 += z2 * x1;

			if (offset < 0 || offset >= dest.length) {
				return;
			}

			int remaining = x2 - x1;
			int maxPixels = dest.length - offset;
			if (remaining > maxPixels) {
				remaining = maxPixels;
			}

			if (alpha == 0) {
				// Solid rendering
				for (int i = 0; i < remaining; i++) {
					if (offset >= dest.length) break;

					int hslIndex = hsl1 >> 8;
					// Clamp HSL index to valid range
					hslIndex = Math.max(0, Math.min(hslToRgb.length - 1, hslIndex));

					rgb = hslToRgb[hslIndex];
					dest[offset] = rgb;

					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}

					z1 += z2;
					hsl1 += dhsl;
					offset++;
				}
			} else {
				// Alpha blending
				int a1 = alpha;
				int a2 = 256 - alpha;

				for (int i = 0; i < remaining; i++) {
					if (offset >= dest.length) break;

					int hslIndex = hsl1 >> 8;
					hslIndex = Math.max(0, Math.min(hslToRgb.length - 1, hslIndex));

					rgb = hslToRgb[hslIndex];
					int srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
					int dst = dest[offset];
					int dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);

					dest[offset] = srcBlend + dstBlend;

					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}

					z1 += z2;
					hsl1 += dhsl;
					offset++;
				}
			}
		} else {
			// Non-optimized path
			if (x2 > DrawingArea.lastX) {
				x2 = DrawingArea.lastX;
			}
			if (x1 < 0) {
				x1 = 0;
			}
			if (x1 >= x2) {
				return;
			}

			int dhsl2 = (x2 - x1 > 0) ? ((hsl2 - hsl1) / (x2 - x1)) : 0;

			offset += x1;
			z1 += z2 * x1;

			if (offset < 0 || offset >= dest.length) {
				return;
			}

			int remaining = x2 - x1;
			int maxPixels = dest.length - offset;
			if (remaining > maxPixels) {
				remaining = maxPixels;
			}

			if (alpha == 0) {
				for (int i = 0; i < remaining; i++) {
					if (offset >= dest.length) break;

					int hslIndex = hsl1 >> 8;
					hslIndex = Math.max(0, Math.min(hslToRgb.length - 1, hslIndex));

					dest[offset] = hslToRgb[hslIndex];

					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}

					z1 += z2;
					hsl1 += dhsl2;
					offset++;
				}
			} else {
				int a1 = alpha;
				int a2 = 256 - alpha;

				for (int i = 0; i < remaining; i++) {
					if (offset >= dest.length) break;

					int hslIndex = hsl1 >> 8;
					hslIndex = Math.max(0, Math.min(hslToRgb.length - 1, hslIndex));

					rgb = hslToRgb[hslIndex];
					int srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
					int dst = dest[offset];
					int dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);

					dest[offset] = srcBlend + dstBlend;

					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}

					z1 += z2;
					hsl1 += dhsl2;
					offset++;
				}
			}
		}
	}
	public static void drawHDGouraudTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int hsl1, int hsl2,
			int hsl3, float z1, float z2, float z3) {
		if (!saveDepth) {
			z1 = z2 = z3 = 0;
		}

		int rgb1 = hslToRgb[hsl1];
		int rgb2 = hslToRgb[hsl2];
		int rgb3 = hslToRgb[hsl3];
		int r1 = rgb1 >> 16 & 0xff;
					int g1 = rgb1 >> 8 & 0xff;
					int b1 = rgb1 & 0xff;
					int r2 = rgb2 >> 16 & 0xff;
					int g2 = rgb2 >> 8 & 0xff;
					int b2 = rgb2 & 0xff;
					int r3 = rgb3 >> 16 & 0xff;
					int g3 = rgb3 >> 8 & 0xff;
					int b3 = rgb3 & 0xff;

					int dx1 = 0;
					int dr1 = 0;
					int dg1 = 0;
					int db1 = 0;
					if (y2 != y1) {
						int dy = y2 - y1;
						dx1 = (x2 - x1 << 16) / dy;
						dr1 = (r2 - r1 << 16) / dy;
						dg1 = (g2 - g1 << 16) / dy;
						db1 = (b2 - b1 << 16) / dy;
					}

					int dx2 = 0;
					int dr2 = 0;
					int dg2 = 0;
					int db2 = 0;
					if (y3 != y2) {
						int dy = y3 - y2;
						dx2 = (x3 - x2 << 16) / dy;
						dr2 = (r3 - r2 << 16) / dy;
						dg2 = (g3 - g2 << 16) / dy;
						db2 = (b3 - b2 << 16) / dy;
					}

					int dx3 = 0;
					int dr3 = 0;
					int dg3 = 0;
					int db3 = 0;
					if (y3 != y1) {
						int dy = y1 - y3;
						dx3 = (x1 - x3 << 16) / dy;
						dr3 = (r1 - r3 << 16) / dy;
						dg3 = (g1 - g3 << 16) / dy;
						db3 = (b1 - b3 << 16) / dy;
					}

					float x21 = x2 - x1;
					float y32 = y2 - y1;
					float x31 = x3 - x1;
					float y31 = y3 - y1;
					float z21 = z2 - z1;
					float z31 = z3 - z1;

					float div = x21 * y31 - x31 * y32;
					if (div == 0) return; // CRITICAL FIX: Prevent division by zero

					float depthSlope = (z21 * y31 - z31 * y32) / div;
					float depthScale = (z31 * x21 - z21 * x31) / div;

					if (y1 <= y2 && y1 <= y3) {
						if (y1 >= DrawingArea.bottomY) {
							return;
						}
						if (y2 > DrawingArea.bottomY) {
							y2 = DrawingArea.bottomY;
						}
						if (y3 > DrawingArea.bottomY) {
							y3 = DrawingArea.bottomY;
						}
						z1 = z1 - depthSlope * x1 + depthSlope;
						if (y2 < y3) {
							x3 = x1 <<= 16;
							r3 = r1 <<= 16;
							g3 = g1 <<= 16;
							b3 = b1 <<= 16;
							if (y1 < 0) {
								x3 -= dx3 * y1;
								x1 -= dx1 * y1;
								r3 -= dr3 * y1;
								g3 -= dg3 * y1;
								b3 -= db3 * y1;
								r1 -= dr1 * y1;
								g1 -= dg1 * y1;
								b1 -= db1 * y1;
								z1 -= depthScale * y1;
								y1 = 0;
							}
							x2 <<= 16;
							r2 <<= 16;
							g2 <<= 16;
							b2 <<= 16;
							if (y2 < 0) {
								x2 -= dx2 * y2;
								r2 -= dr2 * y2;
								g2 -= dg2 * y2;
								b2 -= db2 * y2;
								y2 = 0;
							}
							if (y1 != y2 && dx3 < dx1 || y1 == y2 && dx3 > dx2) {
								y3 -= y2;
								y2 -= y1;
								// CRITICAL FIX: Bounds check before array access
								if (y1 < 0 || y1 >= anIntArray1472.length) {
									return;
								}
								for (y1 = anIntArray1472[y1]; --y2 >= 0; y1 += DrawingArea.width) {
									drawHDGouraudScanline(DrawingArea.pixels, y1, x3 >> 16, x1 >> 16, r3, g3, b3, r1, g1, b1, z1,
							depthSlope);
									x3 += dx3;
									x1 += dx1;
									r3 += dr3;
									g3 += dg3;
									b3 += db3;
									r1 += dr1;
									g1 += dg1;
									b1 += db1;
									z1 += depthScale;
								}
								while (--y3 >= 0) {
									drawHDGouraudScanline(DrawingArea.pixels, y1, x3 >> 16, x2 >> 16, r3, g3, b3, r2, g2, b2, z1,
							depthSlope);
									x3 += dx3;
									x2 += dx2;
									r3 += dr3;
									g3 += dg3;
									b3 += db3;
									r2 += dr2;
									g2 += dg2;
									b2 += db2;
									y1 += DrawingArea.width;
									z1 += depthScale;
								}
								return;
							}
							y3 -= y2;
							y2 -= y1;
							if (y1 < 0 || y1 >= anIntArray1472.length) {
								return;
							}
							for (y1 = anIntArray1472[y1]; --y2 >= 0; y1 += DrawingArea.width) {
								drawHDGouraudScanline(DrawingArea.pixels, y1, x1 >> 16, x3 >> 16, r1, g1, b1, r3, g3, b3, z1,
									depthSlope);
								x3 += dx3;
								x1 += dx1;
								r3 += dr3;
								g3 += dg3;
								b3 += db3;
								r1 += dr1;
								g1 += dg1;
								b1 += db1;
								z1 += depthScale;
							}
							while (--y3 >= 0) {
								drawHDGouraudScanline(DrawingArea.pixels, y1, x2 >> 16, x3 >> 16, r2, g2, b2, r3, g3, b3, z1,
								depthSlope);
								x3 += dx3;
								x2 += dx2;
								r3 += dr3;
								g3 += dg3;
								b3 += db3;
								r2 += dr2;
								g2 += dg2;
								b2 += db2;
								y1 += DrawingArea.width;
								z1 += depthScale;
							}
							return;
						}
						x2 = x1 <<= 16;
						r2 = r1 <<= 16;
						g2 = g1 <<= 16;
						b2 = b1 <<= 16;
						if (y1 < 0) {
							x2 -= dx3 * y1;
							x1 -= dx1 * y1;
							r2 -= dr3 * y1;
							g2 -= dg3 * y1;
							b2 -= db3 * y1;
							r1 -= dr1 * y1;
							g1 -= dg1 * y1;
							b1 -= db1 * y1;
							z1 -= depthScale * y1;
							y1 = 0;
						}
						x3 <<= 16;
						r3 <<= 16;
						g3 <<= 16;
						b3 <<= 16;
						if (y3 < 0) {
							x3 -= dx2 * y3;
							r3 -= dr2 * y3;
							g3 -= dg2 * y3;
							b3 -= db2 * y3;
							y3 = 0;
						}
						if (y1 != y3 && dx3 < dx1 || y1 == y3 && dx2 > dx1) {
							y2 -= y3;
							y3 -= y1;
							if (y1 < 0 || y1 >= anIntArray1472.length) {
								return;
							}
							for (y1 = anIntArray1472[y1]; --y3 >= 0; y1 += DrawingArea.width) {
								drawHDGouraudScanline(DrawingArea.pixels, y1, x2 >> 16, x1 >> 16, r2, g2, b2, r1, g1, b1, z1,
						depthSlope);
								x2 += dx3;
								x1 += dx1;
								r2 += dr3;
								g2 += dg3;
								b2 += db3;
								r1 += dr1;
								g1 += dg1;
								b1 += db1;
								z1 += depthScale;
							}
							while (--y2 >= 0) {
								drawHDGouraudScanline(DrawingArea.pixels, y1, x3 >> 16, x1 >> 16, r3, g3, b3, r1, g1, b1, z1,
						depthSlope);
								x3 += dx2;
								x1 += dx1;
								r3 += dr2;
								g3 += dg2;
								b3 += db2;
								r1 += dr1;
								g1 += dg1;
								b1 += db1;
								y1 += DrawingArea.width;
								z1 += depthScale;
							}
							return;
						}
						y2 -= y3;
						y3 -= y1;
						if (y1 < 0 || y1 >= anIntArray1472.length) {
							return;
						}
						for (y1 = anIntArray1472[y1]; --y3 >= 0; y1 += DrawingArea.width) {
							drawHDGouraudScanline(DrawingArea.pixels, y1, x1 >> 16, x2 >> 16, r1, g1, b1, r2, g2, b2, z1,
								depthSlope);
							x2 += dx3;
							x1 += dx1;
							r2 += dr3;
							g2 += dg3;
							b2 += db3;
							r1 += dr1;
							g1 += dg1;
							b1 += db1;
							z1 += depthScale;
						}
						while (--y2 >= 0) {
							drawHDGouraudScanline(DrawingArea.pixels, y1, x1 >> 16, x3 >> 16, r1, g1, b1, r3, g3, b3, z1,
								depthSlope);
							x3 += dx2;
							x1 += dx1;
							r3 += dr2;
							g3 += dg2;
							b3 += db2;
							r1 += dr1;
							g1 += dg1;
							b1 += db1;
							y1 += DrawingArea.width;
							z1 += depthScale;
						}
						return;
					}
					if (y2 <= y3) {
						if (y2 >= DrawingArea.bottomY) {
							return;
						}
						if (y3 > DrawingArea.bottomY) {
							y3 = DrawingArea.bottomY;
						}
						if (y1 > DrawingArea.bottomY) {
							y1 = DrawingArea.bottomY;
						}
						z2 = z2 - depthSlope * x2 + depthSlope;
						if (y3 < y1) {
							x1 = x2 <<= 16;
							r1 = r2 <<= 16;
							g1 = g2 <<= 16;
							b1 = b2 <<= 16;
							if (y2 < 0) {
								x1 -= dx1 * y2;
								x2 -= dx2 * y2;
								r1 -= dr1 * y2;
								g1 -= dg1 * y2;
								b1 -= db1 * y2;
								r2 -= dr2 * y2;
								g2 -= dg2 * y2;
								b2 -= db2 * y2;
								z2 -= depthScale * y2;
								y2 = 0;
							}
							x3 <<= 16;
							r3 <<= 16;
							g3 <<= 16;
							b3 <<= 16;
							if (y3 < 0) {
								x3 -= dx3 * y3;
								r3 -= dr3 * y3;
								g3 -= dg3 * y3;
								b3 -= db3 * y3;
								y3 = 0;
							}
							if (y2 != y3 && dx1 < dx2 || y2 == y3 && dx1 > dx3) {
								y1 -= y3;
								y3 -= y2;
								if (y2 < 0 || y2 >= anIntArray1472.length) {
									return;
								}
								for (y2 = anIntArray1472[y2]; --y3 >= 0; y2 += DrawingArea.width) {
									drawHDGouraudScanline(DrawingArea.pixels, y2, x1 >> 16, x2 >> 16, r1, g1, b1, r2, g2, b2, z2,
							depthSlope);
									x1 += dx1;
									x2 += dx2;
									r1 += dr1;
									g1 += dg1;
									b1 += db1;
									r2 += dr2;
									g2 += dg2;
									b2 += db2;
									z2 += depthScale;
								}
								while (--y1 >= 0) {
									drawHDGouraudScanline(DrawingArea.pixels, y2, x1 >> 16, x3 >> 16, r1, g1, b1, r3, g3, b3, z2,
							depthSlope);
									x1 += dx1;
									x3 += dx3;
									r1 += dr1;
									g1 += dg1;
									b1 += db1;
									r3 += dr3;
									g3 += dg3;
									b3 += db3;
									y2 += DrawingArea.width;
									z2 += depthScale;
								}
								return;
							}
							y1 -= y3;
							y3 -= y2;
							if (y2 < 0 || y2 >= anIntArray1472.length) {
								return;
							}
							for (y2 = anIntArray1472[y2]; --y3 >= 0; y2 += DrawingArea.width) {
								drawHDGouraudScanline(DrawingArea.pixels, y2, x2 >> 16, x1 >> 16, r2, g2, b2, r1, g1, b1, z2,
									depthSlope);
								x1 += dx1;
								x2 += dx2;
								r1 += dr1;
								g1 += dg1;
								b1 += db1;
								r2 += dr2;
								g2 += dg2;
								b2 += db2;
								z2 += depthScale;
							}
							while (--y1 >= 0) {
								drawHDGouraudScanline(DrawingArea.pixels, y2, x3 >> 16, x1 >> 16, r3, g3, b3, r1, g1, b1, z2,
								depthSlope);
								x1 += dx1;
								x3 += dx3;
								r1 += dr1;
								g1 += dg1;
								b1 += db1;
								r3 += dr3;
								g3 += dg3;
								b3 += db3;
								y2 += DrawingArea.width;
								z2 += depthScale;
							}
							return;
						}
						x3 = x2 <<= 16;
						r3 = r2 <<= 16;
						g3 = g2 <<= 16;
						b3 = b2 <<= 16;
						if (y2 < 0) {
							x3 -= dx1 * y2;
							x2 -= dx2 * y2;
							r3 -= dr1 * y2;
							g3 -= dg1 * y2;
							b3 -= db1 * y2;
							r2 -= dr2 * y2;
							g2 -= dg2 * y2;
							b2 -= db2 * y2;
							z2 -= depthScale * y2;
							y2 = 0;
						}
						x1 <<= 16;
						r1 <<= 16;
						g1 <<= 16;
						b1 <<= 16;
						if (y1 < 0) {
							x1 -= dx3 * y1;
							r1 -= dr3 * y1;
							g1 -= dg3 * y1;
							b1 -= db3 * y1;
							y1 = 0;
						}
						if (dx1 < dx2) {
							y3 -= y1;
							y1 -= y2;
							if (y2 < 0 || y2 >= anIntArray1472.length) {
								return;
							}
							for (y2 = anIntArray1472[y2]; --y1 >= 0; y2 += DrawingArea.width) {
								drawHDGouraudScanline(DrawingArea.pixels, y2, x3 >> 16, x2 >> 16, r3, g3, b3, r2, g2, b2, z2,
						depthSlope);
								x3 += dx1;
								x2 += dx2;
								r3 += dr1;
								g3 += dg1;
								b3 += db1;
								r2 += dr2;
								g2 += dg2;
								b2 += db2;
								z2 += depthScale;
							}
							while (--y3 >= 0) {
								drawHDGouraudScanline(DrawingArea.pixels, y2, x1 >> 16, x2 >> 16, r1, g1, b1, r2, g2, b2, z2,
						depthSlope);
								x1 += dx3;
								x2 += dx2;
								r1 += dr3;
								g1 += dg3;
								b1 += db3;
								r2 += dr2;
								g2 += dg2;
								b2 += db2;
								y2 += DrawingArea.width;
								z2 += depthScale;
							}
							return;
						}
						y3 -= y1;
						y1 -= y2;
						if (y2 < 0 || y2 >= anIntArray1472.length) {
							return;
						}
						for (y2 = anIntArray1472[y2]; --y1 >= 0; y2 += DrawingArea.width) {
							drawHDGouraudScanline(DrawingArea.pixels, y2, x2 >> 16, x3 >> 16, r2, g2, b2, r3, g3, b3, z2,
								depthSlope);
							x3 += dx1;
							x2 += dx2;
							r3 += dr1;
							g3 += dg1;
							b3 += db1;
							r2 += dr2;
							g2 += dg2;
							b2 += db2;
							z2 += depthScale;
						}
						while (--y3 >= 0) {
							drawHDGouraudScanline(DrawingArea.pixels, y2, x2 >> 16, x1 >> 16, r2, g2, b2, r1, g1, b1, z2,
								depthSlope);
							x1 += dx3;
							x2 += dx2;
							r1 += dr3;
							g1 += dg3;
							b1 += db3;
							r2 += dr2;
							g2 += dg2;
							b2 += db2;
							y2 += DrawingArea.width;
							z2 += depthScale;
						}
						return;
					}
					if (y3 >= DrawingArea.bottomY) {
						return;
					}
					if (y1 > DrawingArea.bottomY) {
						y1 = DrawingArea.bottomY;
					}
					if (y2 > DrawingArea.bottomY) {
						y2 = DrawingArea.bottomY;
					}
					z3 = z3 - depthSlope * x3 + depthSlope;
					if (y1 < y2) {
						x2 = x3 <<= 16;
						r2 = r3 <<= 16;
						g2 = g3 <<= 16;
						b2 = b3 <<= 16;
						if (y3 < 0) {
							x2 -= dx2 * y3;
							x3 -= dx3 * y3;
							r2 -= dr2 * y3;
							g2 -= dg2 * y3;
							b2 -= db2 * y3;
							r3 -= dr3 * y3;
							g3 -= dg3 * y3;
							b3 -= db3 * y3;
							z3 -= depthScale * y3;
							y3 = 0;
						}
						x1 <<= 16;
						r1 <<= 16;
						g1 <<= 16;
						b1 <<= 16;
						if (y1 < 0) {
							x1 -= dx1 * y1;
							r1 -= dr1 * y1;
							g1 -= dg1 * y1;
							b1 -= db1 * y1;
							y1 = 0;
						}
						if (dx2 < dx3) {
							y2 -= y1;
							y1 -= y3;
							if (y3 < 0 || y3 >= anIntArray1472.length) {
								return;
							}
							for (y3 = anIntArray1472[y3]; --y1 >= 0; y3 += DrawingArea.width) {
								drawHDGouraudScanline(DrawingArea.pixels, y3, x2 >> 16, x3 >> 16, r2, g2, b2, r3, g3, b3, z3,
						depthSlope);
								x2 += dx2;
								x3 += dx3;
								r2 += dr2;
								g2 += dg2;
								b2 += db2;
								r3 += dr3;
								g3 += dg3;
								b3 += db3;
								z3 += depthScale;
							}
							while (--y2 >= 0) {
								drawHDGouraudScanline(DrawingArea.pixels, y3, x2 >> 16, x1 >> 16, r2, g2, b2, r1, g1, b1, z3,
						depthSlope);
								x2 += dx2;
								x1 += dx1;
								r2 += dr2;
								g2 += dg2;
								b2 += db2;
								r1 += dr1;
								g1 += dg1;
								b1 += db1;
								y3 += DrawingArea.width;
								z3 += depthScale;
							}
							return;
						}
						y2 -= y1;
						y1 -= y3;
						if (y3 < 0 || y3 >= anIntArray1472.length) {
							return;
						}
						for (y3 = anIntArray1472[y3]; --y1 >= 0; y3 += DrawingArea.width) {
							drawHDGouraudScanline(DrawingArea.pixels, y3, x3 >> 16, x2 >> 16, r3, g3, b3, r2, g2, b2, z3,
								depthSlope);
							x2 += dx2;
							x3 += dx3;
							r2 += dr2;
							g2 += dg2;
							b2 += db2;
							r3 += dr3;
							g3 += dg3;
							b3 += db3;
							z3 += depthScale;
						}
						while (--y2 >= 0) {
							drawHDGouraudScanline(DrawingArea.pixels, y3, x1 >> 16, x2 >> 16, r1, g1, b1, r2, g2, b2, z3,
								depthSlope);
							x2 += dx2;
							x1 += dx1;
							r2 += dr2;
							g2 += dg2;
							b2 += db2;
							r1 += dr1;
							g1 += dg1;
							b1 += db1;
							z3 += depthScale;
							y3 += DrawingArea.width;
						}
						return;
					}
					x1 = x3 <<= 16;
					r1 = r3 <<= 16;
					g1 = g3 <<= 16;
					b1 = b3 <<= 16;
					if (y3 < 0) {
						x1 -= dx2 * y3;
						x3 -= dx3 * y3;
						r1 -= dr2 * y3;
						g1 -= dg2 * y3;
						b1 -= db2 * y3;
						r3 -= dr3 * y3;
						g3 -= dg3 * y3;
						b3 -= db3 * y3;
						z3 -= depthScale * y3;
						y3 = 0;
					}
					x2 <<= 16;
					r2 <<= 16;
					g2 <<= 16;
					b2 <<= 16;
					if (y2 < 0) {
						x2 -= dx1 * y2;
						r2 -= dr1 * y2;
						g2 -= dg1 * y2;
						b2 -= db1 * y2;
						y2 = 0;
					}
					if (dx2 < dx3) {
						y1 -= y2;
						y2 -= y3;
						if (y3 < 0 || y3 >= anIntArray1472.length) {
							return;
						}
						for (y3 = anIntArray1472[y3]; --y2 >= 0; y3 += DrawingArea.width) {
							drawHDGouraudScanline(DrawingArea.pixels, y3, x1 >> 16, x3 >> 16, r1, g1, b1, r3, g3, b3, z3,
					depthSlope);
							x1 += dx2;
							x3 += dx3;
							r1 += dr2;
							g1 += dg2;
							b1 += db2;
							r3 += dr3;
							g3 += dg3;
							b3 += db3;
							z3 += depthScale;
						}
						while (--y1 >= 0) {
							drawHDGouraudScanline(DrawingArea.pixels, y3, x2 >> 16, x3 >> 16, r2, g2, b2, r3, g3, b3, z3,
					depthSlope);
							x2 += dx1;
							x3 += dx3;
							r2 += dr1;
							g2 += dg1;
							b2 += db1;
							r3 += dr3;
							g3 += dg3;
							b3 += db3;
							z3 += depthScale;
							y3 += DrawingArea.width;
						}
						return;
					}
					y1 -= y2;
					y2 -= y3;
					if (y3 < 0 || y3 >= anIntArray1472.length) {
						return;
					}
					for (y3 = anIntArray1472[y3]; --y2 >= 0; y3 += DrawingArea.width) {
						drawHDGouraudScanline(DrawingArea.pixels, y3, x3 >> 16, x1 >> 16, r3, g3, b3, r1, g1, b1, z3, depthSlope);
						x1 += dx2;
						x3 += dx3;
						r1 += dr2;
						g1 += dg2;
						b1 += db2;
						r3 += dr3;
						g3 += dg3;
						b3 += db3;
						z3 += depthScale;
					}
					while (--y1 >= 0) {
						drawHDGouraudScanline(DrawingArea.pixels, y3, x3 >> 16, x2 >> 16, r3, g3, b3, r2, g2, b2, z3, depthSlope);
						x2 += dx1;
						x3 += dx3;
						r2 += dr1;
						g2 += dg1;
						b2 += db1;
						r3 += dr3;
						g3 += dg3;
						b3 += db3;
						y3 += DrawingArea.width;
						z3 += depthScale;
					}
	}
	public static void drawHDGouraudScanline(int[] dest, int offset, int x1, int x2, int r1, int g1, int b1, int r2,
	        int g2, int b2, float z1, float z2) {
	    int n = x2 - x1;
	    if (n <= 0) {
	        return;
	    }
	    
	    // Calculate color deltas
	    int r_delta = (r2 - r1) / n;
	    int g_delta = (g2 - g1) / n;
	    int b_delta = (b2 - b1) / n;
	    
	    // Apply texture bounds clipping
	    if (textureOutOfDrawingBounds) {
	        if (x2 > DrawingArea.lastX) {
	            n = DrawingArea.lastX - x1;
	            x2 = DrawingArea.lastX;
	        }
	        if (x1 < 0) {
	            r1 -= x1 * r_delta;
	            g1 -= x1 * g_delta;
	            b1 -= x1 * b_delta;
	            z1 -= x1 * z2;
	            n = x2;
	            x1 = 0;
	        }
	    }
	    
	    if (x1 < 0 || x1 >= dest.length) {
	        return;
	    }
	    
	    offset += x1;
	    z1 += z2 * x1;
	    
	    int maxOffset = dest.length - 1;
	    if (offset < 0 || offset > maxOffset) {
	        return;
	    }
	    
	    if (offset + n > maxOffset) {
	        n = maxOffset - offset;
	    }
	    
	    if (n <= 0) {
	        return;
	    }
	    
	    // Process in blocks of 4 pixels for better performance
	    int loops = n >> 2;
	    int remainder = n & 3;
	    
	    if (alpha == 0) {
	        // Solid rendering - unrolled 4 pixels at a time
	        while (loops-- > 0) {
	            if (offset >= dest.length) break;
	            
	            // Pixel 1
	            int finalR = (r1 >> 16) & 0xFF;
	            int finalG = (g1 >> 16) & 0xFF;
	            int finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            dest[offset] = (finalR << 16) | (finalG << 8) | finalB;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            offset++;
	            
	            // Pixel 2
	            if (offset >= dest.length) break;
	            finalR = (r1 >> 16) & 0xFF;
	            finalG = (g1 >> 16) & 0xFF;
	            finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            dest[offset] = (finalR << 16) | (finalG << 8) | finalB;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            offset++;
	            
	            // Pixel 3
	            if (offset >= dest.length) break;
	            finalR = (r1 >> 16) & 0xFF;
	            finalG = (g1 >> 16) & 0xFF;
	            finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            dest[offset] = (finalR << 16) | (finalG << 8) | finalB;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            offset++;
	            
	            // Pixel 4
	            if (offset >= dest.length) break;
	            finalR = (r1 >> 16) & 0xFF;
	            finalG = (g1 >> 16) & 0xFF;
	            finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            dest[offset] = (finalR << 16) | (finalG << 8) | finalB;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            offset++;
	        }
	        
	        // Handle remaining pixels
	        while (remainder-- > 0) {
	            if (offset >= dest.length) break;
	            int finalR = (r1 >> 16) & 0xFF;
	            int finalG = (g1 >> 16) & 0xFF;
	            int finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            dest[offset] = (finalR << 16) | (finalG << 8) | finalB;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            offset++;
	        }
	    } else {
	        // Alpha blending - unrolled
	        final int a1 = alpha;
	        final int a2 = 256 - alpha;
	        
	        while (loops-- > 0) {
	            if (offset >= dest.length) break;
	            
	            // Pixel 1
	            int finalR = (r1 >> 16) & 0xFF;
	            int finalG = (g1 >> 16) & 0xFF;
	            int finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            int rgb = (finalR << 16) | (finalG << 8) | finalB;
	            int srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
	            int dst = dest[offset];
	            int dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);
	            dest[offset] = srcBlend + dstBlend;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            offset++;
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            
	            // Pixel 2
	            if (offset >= dest.length) break;
	            finalR = (r1 >> 16) & 0xFF;
	            finalG = (g1 >> 16) & 0xFF;
	            finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            rgb = (finalR << 16) | (finalG << 8) | finalB;
	            srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
	            dst = dest[offset];
	            dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);
	            dest[offset] = srcBlend + dstBlend;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            offset++;
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            
	            // Pixel 3
	            if (offset >= dest.length) break;
	            finalR = (r1 >> 16) & 0xFF;
	            finalG = (g1 >> 16) & 0xFF;
	            finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            rgb = (finalR << 16) | (finalG << 8) | finalB;
	            srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
	            dst = dest[offset];
	            dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);
	            dest[offset] = srcBlend + dstBlend;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            offset++;
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	            
	            // Pixel 4
	            if (offset >= dest.length) break;
	            finalR = (r1 >> 16) & 0xFF;
	            finalG = (g1 >> 16) & 0xFF;
	            finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            rgb = (finalR << 16) | (finalG << 8) | finalB;
	            srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
	            dst = dest[offset];
	            dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);
	            dest[offset] = srcBlend + dstBlend;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            offset++;
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	        }
	        
	        // Handle remaining pixels
	        while (remainder-- > 0) {
	            if (offset >= dest.length) break;
	            int finalR = (r1 >> 16) & 0xFF;
	            int finalG = (g1 >> 16) & 0xFF;
	            int finalB = (b1 >> 16) & 0xFF;
	            finalR = Math.max(0, Math.min(255, finalR));
	            finalG = Math.max(0, Math.min(255, finalG));
	            finalB = Math.max(0, Math.min(255, finalB));
	            int rgb = (finalR << 16) | (finalG << 8) | finalB;
	            int srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
	            int dst = dest[offset];
	            int dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);
	            dest[offset] = srcBlend + dstBlend;
	            if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
	                depthBuffer[offset] = z1;
	            }
	            offset++;
	            z1 += z2;
	            r1 += r_delta;
	            g1 += g_delta;
	            b1 += b_delta;
	        }
	    }
	}
	public static void drawFlatTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int rgb, float z1, float z2,
			float z3) {
		// CRITICAL FIX: Clip triangles too close to camera
		if (shouldClipTriangle(z1, z2, z3)) {
			return;
		}

		if (!saveDepth) {
			z1 = z2 = z3 = 0;
		}
		int dx1 = 0;
		if (y2 != y1) {
			final int d = (y2 - y1);
			dx1 = (x2 - x1 << 16) / d;
		}
		int dx2 = 0;
		if (y3 != y2) {
			final int d = (y3 - y2);
			dx2 = (x3 - x2 << 16) / d;
		}
		int dx3 = 0;
		if (y3 != y1) {
			final int d = (y1 - y3);
			dx3 = (x1 - x3 << 16) / d;
		}

		float x21 = x2 - x1;
		float y32 = y2 - y1;
		float x31 = x3 - x1;
		float y31 = y3 - y1;
		float z21 = z2 - z1;
		float z31 = z3 - z1;

		float div = x21 * y31 - x31 * y32;
		float depthSlope = (z21 * y31 - z31 * y32) / div;
		float depthScale = (z31 * x21 - z21 * x31) / div;

		if (y1 <= y2 && y1 <= y3) {
			if (y1 >= DrawingArea.bottomY) {
				return;
			}
			if (y2 > DrawingArea.bottomY) {
				y2 = DrawingArea.bottomY;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			z1 = z1 - depthSlope * x1 + depthSlope;
			if (y2 < y3) {
				x3 = x1 <<= 16;
				if (y1 < 0) {
					x3 -= dx3 * y1;
					x1 -= dx1 * y1;
					z1 -= depthScale * y1;
					y1 = 0;
				}
				x2 <<= 16;
				if (y2 < 0) {
					x2 -= dx2 * y2;
					y2 = 0;
				}
				if (y1 != y2 && dx3 < dx1 || y1 == y2 && dx3 > dx2) {
					y3 -= y2;
					y2 -= y1;
					for (y1 = anIntArray1472[y1]; --y2 >= 0; y1 += DrawingArea.width) {
						drawFlatScanline(DrawingArea.pixels, y1, rgb, x3 >> 16, x1 >> 16, z1, depthSlope);
						z1 += depthScale;
						x3 += dx3;
						x1 += dx1;
					}
					while (--y3 >= 0) {
						drawFlatScanline(DrawingArea.pixels, y1, rgb, x3 >> 16, x2 >> 16, z1, depthSlope);
						z1 += depthScale;
						x3 += dx3;
						x2 += dx2;
						y1 += DrawingArea.width;
					}
					return;
				}
				y3 -= y2;
				y2 -= y1;
				for (y1 = anIntArray1472[y1]; --y2 >= 0; y1 += DrawingArea.width) {
					drawFlatScanline(DrawingArea.pixels, y1, rgb, x1 >> 16, x3 >> 16, z1, depthSlope);
					z1 += depthScale;
					x3 += dx3;
					x1 += dx1;
				}
				while (--y3 >= 0) {
					drawFlatScanline(DrawingArea.pixels, y1, rgb, x2 >> 16, x3 >> 16, z1, depthSlope);
					z1 += depthScale;
					x3 += dx3;
					x2 += dx2;
					y1 += DrawingArea.width;
				}
				return;
			}
			x2 = x1 <<= 16;
			if (y1 < 0) {
				x2 -= dx3 * y1;
				x1 -= dx1 * y1;
				z1 -= depthScale * y1;
				y1 = 0;
			}
			x3 <<= 16;
			if (y3 < 0) {
				x3 -= dx2 * y3;
				y3 = 0;
			}
			if (y1 != y3 && dx3 < dx1 || y1 == y3 && dx2 > dx1) {
				y2 -= y3;
				y3 -= y1;
				for (y1 = anIntArray1472[y1]; --y3 >= 0; y1 += DrawingArea.width) {
					drawFlatScanline(DrawingArea.pixels, y1, rgb, x2 >> 16, x1 >> 16, z1, depthSlope);
					z1 += depthScale;
					x2 += dx3;
					x1 += dx1;
				}
				while (--y2 >= 0) {
					drawFlatScanline(DrawingArea.pixels, y1, rgb, x3 >> 16, x1 >> 16, z1, depthSlope);
					z1 += depthScale;
					x3 += dx2;
					x1 += dx1;
					y1 += DrawingArea.width;
				}
				return;
			}
			y2 -= y3;
			y3 -= y1;
			for (y1 = anIntArray1472[y1]; --y3 >= 0; y1 += DrawingArea.width) {
				drawFlatScanline(DrawingArea.pixels, y1, rgb, x1 >> 16, x2 >> 16, z1, depthSlope);
				z1 += depthScale;
				x2 += dx3;
				x1 += dx1;
			}
			while (--y2 >= 0) {
				drawFlatScanline(DrawingArea.pixels, y1, rgb, x1 >> 16, x3 >> 16, z1, depthSlope);
				z1 += depthScale;
				x3 += dx2;
				x1 += dx1;
				y1 += DrawingArea.width;
			}
			return;
		}
		if (y2 <= y3) {
			if (y2 >= DrawingArea.bottomY) {
				return;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			if (y1 > DrawingArea.bottomY) {
				y1 = DrawingArea.bottomY;
			}
			z2 = z2 - depthSlope * x2 + depthSlope;
			if (y3 < y1) {
				x1 = x2 <<= 16;
				if (y2 < 0) {
					x1 -= dx1 * y2;
					x2 -= dx2 * y2;
					z2 -= depthScale * y2;
					y2 = 0;
				}
				x3 <<= 16;
				if (y3 < 0) {
					x3 -= dx3 * y3;
					y3 = 0;
				}
				if (y2 != y3 && dx1 < dx2 || y2 == y3 && dx1 > dx3) {
					y1 -= y3;
					y3 -= y2;
					for (y2 = anIntArray1472[y2]; --y3 >= 0; y2 += DrawingArea.width) {
						drawFlatScanline(DrawingArea.pixels, y2, rgb, x1 >> 16, x2 >> 16, z2, depthSlope);
						z2 += depthScale;
						x1 += dx1;
						x2 += dx2;
					}
					while (--y1 >= 0) {
						drawFlatScanline(DrawingArea.pixels, y2, rgb, x1 >> 16, x3 >> 16, z2, depthSlope);
						z2 += depthScale;
						x1 += dx1;
						x3 += dx3;
						y2 += DrawingArea.width;
					}
					return;
				}
				y1 -= y3;
				y3 -= y2;
				for (y2 = anIntArray1472[y2]; --y3 >= 0; y2 += DrawingArea.width) {
					drawFlatScanline(DrawingArea.pixels, y2, rgb, x2 >> 16, x1 >> 16, z2, depthSlope);
					z2 += depthScale;
					x1 += dx1;
					x2 += dx2;
				}
				while (--y1 >= 0) {
					drawFlatScanline(DrawingArea.pixels, y2, rgb, x3 >> 16, x1 >> 16, z2, depthSlope);
					z2 += depthScale;
					x1 += dx1;
					x3 += dx3;
					y2 += DrawingArea.width;
				}
				return;
			}
			x3 = x2 <<= 16;
			if (y2 < 0) {
				x3 -= dx1 * y2;
				x2 -= dx2 * y2;
				z2 -= depthScale * y2;
				y2 = 0;
			}
			x1 <<= 16;
			if (y1 < 0) {
				x1 -= dx3 * y1;
				y1 = 0;
			}
			if (dx1 < dx2) {
				y3 -= y1;
				y1 -= y2;
				for (y2 = anIntArray1472[y2]; --y1 >= 0; y2 += DrawingArea.width) {
					drawFlatScanline(DrawingArea.pixels, y2, rgb, x3 >> 16, x2 >> 16, z2, depthSlope);
					z2 += depthScale;
					x3 += dx1;
					x2 += dx2;
				}
				while (--y3 >= 0) {
					drawFlatScanline(DrawingArea.pixels, y2, rgb, x1 >> 16, x2 >> 16, z2, depthSlope);
					z2 += depthScale;
					x1 += dx3;
					x2 += dx2;
					y2 += DrawingArea.width;
				}
				return;
			}
			y3 -= y1;
			y1 -= y2;
			for (y2 = anIntArray1472[y2]; --y1 >= 0; y2 += DrawingArea.width) {
				drawFlatScanline(DrawingArea.pixels, y2, rgb, x2 >> 16, x3 >> 16, z2, depthSlope);
				z2 += depthScale;
				x3 += dx1;
				x2 += dx2;
			}
			while (--y3 >= 0) {
				drawFlatScanline(DrawingArea.pixels, y2, rgb, x2 >> 16, x1 >> 16, z2, depthSlope);
				z2 += depthScale;
				x1 += dx3;
				x2 += dx2;
				y2 += DrawingArea.width;
			}
			return;
		}
		if (y3 >= DrawingArea.bottomY) {
			return;
		}
		if (y1 > DrawingArea.bottomY) {
			y1 = DrawingArea.bottomY;
		}
		if (y2 > DrawingArea.bottomY) {
			y2 = DrawingArea.bottomY;
		}
		z3 = z3 - depthSlope * x3 + depthSlope;
		if (y1 < y2) {
			x2 = x3 <<= 16;
			if (y3 < 0) {
				x2 -= dx2 * y3;
				x3 -= dx3 * y3;
				z3 -= depthScale * y3;
				y3 = 0;
			}
			x1 <<= 16;
			if (y1 < 0) {
				x1 -= dx1 * y1;
				y1 = 0;
			}
			if (dx2 < dx3) {
				y2 -= y1;
				y1 -= y3;
				for (y3 = anIntArray1472[y3]; --y1 >= 0; y3 += DrawingArea.width) {
					drawFlatScanline(DrawingArea.pixels, y3, rgb, x2 >> 16, x3 >> 16, z3, depthSlope);
					z3 += depthScale;
					x2 += dx2;
					x3 += dx3;
				}
				while (--y2 >= 0) {
					drawFlatScanline(DrawingArea.pixels, y3, rgb, x2 >> 16, x1 >> 16, z3, depthSlope);
					z3 += depthScale;
					x2 += dx2;
					x1 += dx1;
					y3 += DrawingArea.width;
				}
				return;
			}
			y2 -= y1;
			y1 -= y3;
			for (y3 = anIntArray1472[y3]; --y1 >= 0; y3 += DrawingArea.width) {
				drawFlatScanline(DrawingArea.pixels, y3, rgb, x3 >> 16, x2 >> 16, z3, depthSlope);
				z3 += depthScale;
				x2 += dx2;
				x3 += dx3;
			}
			while (--y2 >= 0) {
				drawFlatScanline(DrawingArea.pixels, y3, rgb, x1 >> 16, x2 >> 16, z3, depthSlope);
				z3 += depthScale;
				x2 += dx2;
				x1 += dx1;
				y3 += DrawingArea.width;
			}
			return;
		}
		x1 = x3 <<= 16;
		if (y3 < 0) {
			x1 -= dx2 * y3;
			x3 -= dx3 * y3;
			z3 -= depthScale * y3;
			y3 = 0;
		}
		x2 <<= 16;
		if (y2 < 0) {
			x2 -= dx1 * y2;
			y2 = 0;
		}
		if (dx2 < dx3) {
			y1 -= y2;
			y2 -= y3;
			for (y3 = anIntArray1472[y3]; --y2 >= 0; y3 += DrawingArea.width) {
				drawFlatScanline(DrawingArea.pixels, y3, rgb, x1 >> 16, x3 >> 16, z3, depthSlope);
				z3 += depthScale;
				x1 += dx2;
				x3 += dx3;
			}
			while (--y1 >= 0) {
				drawFlatScanline(DrawingArea.pixels, y3, rgb, x2 >> 16, x3 >> 16, z3, depthSlope);
				z3 += depthScale;
				x2 += dx1;
				x3 += dx3;
				y3 += DrawingArea.width;
			}
			return;
		}
		y1 -= y2;
		y2 -= y3;
		for (y3 = anIntArray1472[y3]; --y2 >= 0; y3 += DrawingArea.width) {
			drawFlatScanline(DrawingArea.pixels, y3, rgb, x3 >> 16, x1 >> 16, z3, depthSlope);
			z3 += depthScale;
			x1 += dx2;
			x3 += dx3;
		}
		while (--y1 >= 0) {
			drawFlatScanline(DrawingArea.pixels, y3, rgb, x3 >> 16, x2 >> 16, z3, depthSlope);
			z3 += depthScale;
			x2 += dx1;
			x3 += dx3;
			y3 += DrawingArea.width;
		}
	}

	private static void drawFlatScanline(int[] dest, int offset, int rgb, int x1, int x2, float z1, float z2) {
		if (x1 >= x2) {
			return;
		}

		if (textureOutOfDrawingBounds) {
			if (x2 > DrawingArea.lastX) {
				x2 = DrawingArea.lastX;
			}
			if (x1 < 0) {
				z1 -= x1 * z2;
				x1 = 0;
			}
		}

		if (x1 >= x2) {
			return;
		}

		offset += x1;
		z1 += z2 * x1;

		// Bounds checking
		if (offset < 0 || offset >= dest.length) {
			return;
		}

		int n = x2 - x1;
		int maxPixels = dest.length - offset;
		if (n > maxPixels) {
			n = maxPixels;
		}

		if (n <= 0) {
			return;
		}

		if (alpha == 0) {
			// Solid fill - no alpha blending
			for (int i = 0; i < n; i++) {
				if (offset >= dest.length) break;

				dest[offset] = rgb;

				if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
					depthBuffer[offset] = z1;
				}

				z1 += z2;
				offset++;
			}
		} else {
			// Alpha blending
			final int a1 = alpha;
			final int a2 = 256 - alpha;

			// Pre-calculate source blend
			int srcBlend = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);

			for (int i = 0; i < n; i++) {
				if (offset >= dest.length) break;

				int dst = dest[offset];
				int dstBlend = ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff) + ((dst & 0xff00) * a1 >> 8 & 0xff00);

				dest[offset] = srcBlend + dstBlend;

				if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
					depthBuffer[offset] = z1;
				}

				z1 += z2;
				offset++;
			}
		}
	}

	public static void drawTexturedTriangle317(int y1, int y2, int y3, int x1, int x2, int x3, int c1, int c2, int c3,
			int t1, int t2, int t3, int t4, int t5, int t6, int t7, int t8, int t9, int tex, float z1, float z2, float z3) {
		// CRITICAL FIX: Clip triangles too close to camera
		if (shouldClipTriangle(z1, z2, z3)) {
			return;
		}

		if (!saveDepth) {
			z1 = z2 = z3 = 0;
		}
		int[] texels = getTexturePixels(tex);
		aBoolean1463 = !textureIsTransparant[tex];
		t2 = t1 - t2;
		t5 = t4 - t5;
		t8 = t7 - t8;
		t3 -= t1;
		t6 -= t4;
		t9 -= t7;
		int l4 = (t3 * t4 - t6 * t1) * WorldController.focalLength << 5;
		int i5 = t6 * t7 - t9 * t4 << 8;
		int j5 = t9 * t1 - t3 * t7 << 5;
		int k5 = (t2 * t4 - t5 * t1) * WorldController.focalLength << 5;
		int l5 = t5 * t7 - t8 * t4 << 8;
		int i6 = t8 * t1 - t2 * t7 << 5;
		int j6 = (t5 * t3 - t2 * t6) * WorldController.focalLength << 5;
		int k6 = t8 * t6 - t5 * t9 << 8;
		int l6 = t2 * t9 - t8 * t3 << 5;
		int i7 = 0;
		int j7 = 0;
		if (y2 != y1) {
			i7 = (x2 - x1 << 16) / (y2 - y1);
			j7 = (c2 - c1 << 16) / (y2 - y1);
		}
		int k7 = 0;
		int l7 = 0;
		if (y3 != y2) {
			k7 = (x3 - x2 << 16) / (y3 - y2);
			l7 = (c3 - c2 << 16) / (y3 - y2);
		}
		int i8 = 0;
		int j8 = 0;
		if (y3 != y1) {
			i8 = (x1 - x3 << 16) / (y1 - y3);
			j8 = (c1 - c3 << 16) / (y1 - y3);
		}

		float x21 = x2 - x1;
		float y32 = y2 - y1;
		float x31 = x3 - x1;
		float y31 = y3 - y1;
		float z21 = z2 - z1;
		float z31 = z3 - z1;

		float div = x21 * y31 - x31 * y32;
		float depthSlope = (z21 * y31 - z31 * y32) / div;
		float depthScale = (z31 * x21 - z21 * x31) / div;

		if (y1 <= y2 && y1 <= y3) {
			if (y1 >= DrawingArea.bottomY) {
				return;
			}
			if (y2 > DrawingArea.bottomY) {
				y2 = DrawingArea.bottomY;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			z1 = z1 - depthSlope * x1 + depthSlope;
			if (y2 < y3) {
				x3 = x1 <<= 16;
				c3 = c1 <<= 16;
				if (y1 < 0) {
					y1 -= 0;
					x3 -= i8 * y1;
					x1 -= i7 * y1;
					z1 -= depthScale * y1;
					c3 -= j8 * y1;
					c1 -= j7 * y1;
					y1 = 0;
				}
				x2 <<= 16;
				c2 <<= 16;
				if (y2 < 0) {
					y2 -= 0;
					x2 -= k7 * y2;
					c2 -= l7 * y2;
					y2 = 0;
				}
				int k8 = y1 - textureInt2;
				l4 += j5 * k8;
				k5 += i6 * k8;
				j6 += l6 * k8;
				if (y1 != y2 && i8 < i7 || y1 == y2 && i8 > k7) {
					y3 -= y2;
					y2 -= y1;
					y1 = anIntArray1472[y1];
					while (--y2 >= 0) {
						drawTexturedScanline317(DrawingArea.pixels, texels, y1, x3 >> 16, x1 >> 16, c3 >> 8, c1 >> 8,
						l4, k5, j6, i5, l5, k6, z1, depthSlope);
						z1 += depthScale;
						x3 += i8;
						x1 += i7;
						c3 += j8;
						c1 += j7;
						y1 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y3 >= 0) {
						drawTexturedScanline317(DrawingArea.pixels, texels, y1, x3 >> 16, x2 >> 16, c3 >> 8, c2 >> 8,
					l4, k5, j6, i5, l5, k6, z1, depthSlope);
						z1 += depthScale;
						x3 += i8;
						x2 += k7;
						c3 += j8;
						c2 += l7;
						y1 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					return;
				}
				y3 -= y2;
				y2 -= y1;
				y1 = anIntArray1472[y1];
				while (--y2 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y1, x1 >> 16, x3 >> 16, c1 >> 8, c3 >> 8, l4,
				k5, j6, i5, l5, k6, z1, depthSlope);
					z1 += depthScale;
					x3 += i8;
					x1 += i7;
					c3 += j8;
					c1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y3 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y1, x2 >> 16, x3 >> 16, c2 >> 8, c3 >> 8, l4,
				k5, j6, i5, l5, k6, z1, depthSlope);
					z1 += depthScale;
					x3 += i8;
					x2 += k7;
					c3 += j8;
					c2 += l7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			x2 = x1 <<= 16;
			c2 = c1 <<= 16;
			if (y1 < 0) {
				y1 -= 0;
				x2 -= i8 * y1;
				z1 -= depthScale * y1;
				x1 -= i7 * y1;
				c2 -= j8 * y1;
				c1 -= j7 * y1;
				y1 = 0;
			}
			x3 <<= 16;
			c3 <<= 16;
			if (y3 < 0) {
				y3 -= 0;
				x3 -= k7 * y3;
				c3 -= l7 * y3;
				y3 = 0;
			}
			int l8 = y1 - textureInt2;
			l4 += j5 * l8;
			k5 += i6 * l8;
			j6 += l6 * l8;
			if (y1 != y3 && i8 < i7 || y1 == y3 && k7 > i7) {
				y2 -= y3;
				y3 -= y1;
				y1 = anIntArray1472[y1];
				while (--y3 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y1, x2 >> 16, x1 >> 16, c2 >> 8, c1 >> 8, l4,
					k5, j6, i5, l5, k6, z1, depthSlope);
					z1 += depthScale;
					x2 += i8;
					x1 += i7;
					c2 += j8;
					c1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y2 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y1, x3 >> 16, x1 >> 16, c3 >> 8, c1 >> 8, l4,
						k5, j6, i5, l5, k6, z1, depthSlope);
					z1 += depthScale;
					x3 += k7;
					x1 += i7;
					c3 += l7;
					c1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y2 -= y3;
			y3 -= y1;
			y1 = anIntArray1472[y1];
			while (--y3 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y1, x1 >> 16, x2 >> 16, c1 >> 8, c2 >> 8, l4, k5,
						j6, i5, l5, k6, z1, depthSlope);
				z1 += depthScale;
				x2 += i8;
				x1 += i7;
				c2 += j8;
				c1 += j7;
				y1 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y2 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y1, x1 >> 16, x3 >> 16, c1 >> 8, c3 >> 8, l4, k5,
						j6, i5, l5, k6, z1, depthSlope);
				z1 += depthScale;
				x3 += k7;
				x1 += i7;
				c3 += l7;
				c1 += j7;
				y1 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		if (y2 <= y3) {
			if (y2 >= DrawingArea.bottomY) {
				return;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			if (y1 > DrawingArea.bottomY) {
				y1 = DrawingArea.bottomY;
			}
			z2 = z2 - depthSlope * x2 + depthSlope;
			if (y3 < y1) {
				x1 = x2 <<= 16;
				c1 = c2 <<= 16;
				if (y2 < 0) {
					y2 -= 0;
					x1 -= i7 * y2;
					x2 -= k7 * y2;
					z2 -= depthScale * y2;
					c1 -= j7 * y2;
					c2 -= l7 * y2;
					y2 = 0;
				}
				x3 <<= 16;
				c3 <<= 16;
				if (y3 < 0) {
					y3 -= 0;
					x3 -= i8 * y3;
					c3 -= j8 * y3;
					y3 = 0;
				}
				int i9 = y2 - textureInt2;
				l4 += j5 * i9;
				k5 += i6 * i9;
				j6 += l6 * i9;
				if (y2 != y3 && i7 < k7 || y2 == y3 && i7 > i8) {
					y1 -= y3;
					y3 -= y2;
					y2 = anIntArray1472[y2];
					while (--y3 >= 0) {
						drawTexturedScanline317(DrawingArea.pixels, texels, y2, x1 >> 16, x2 >> 16, c1 >> 8, c2 >> 8,
						l4, k5, j6, i5, l5, k6, z2, depthSlope);
						z2 += depthScale;
						x1 += i7;
						x2 += k7;
						c1 += j7;
						c2 += l7;
						y2 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y1 >= 0) {
						drawTexturedScanline317(DrawingArea.pixels, texels, y2, x1 >> 16, x3 >> 16, c1 >> 8, c3 >> 8,
					l4, k5, j6, i5, l5, k6, z2, depthSlope);
						z2 += depthScale;
						x1 += i7;
						x3 += i8;
						c1 += j7;
						c3 += j8;
						y2 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					return;
				}
				y1 -= y3;
				y3 -= y2;
				y2 = anIntArray1472[y2];
				while (--y3 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y2, x2 >> 16, x1 >> 16, c2 >> 8, c1 >> 8, l4,
							k5, j6, i5, l5, k6, z2, depthSlope);
					z2 += depthScale;
					x1 += i7;
					x2 += k7;
					c1 += j7;
					c2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y1 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y2, x3 >> 16, x1 >> 16, c3 >> 8, c1 >> 8, l4,
							k5, j6, i5, l5, k6, z2, depthSlope);
					z2 += depthScale;
					x1 += i7;
					x3 += i8;
					c1 += j7;
					c3 += j8;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			x3 = x2 <<= 16;
			c3 = c2 <<= 16;
			if (y2 < 0) {
				y2 -= 0;
				x3 -= i7 * y2;
				z2 -= depthScale * y2;
				x2 -= k7 * y2;
				c3 -= j7 * y2;
				c2 -= l7 * y2;
				y2 = 0;
			}
			x1 <<= 16;
			c1 <<= 16;
			if (y1 < 0) {
				y1 -= 0;
				x1 -= i8 * y1;
				c1 -= j8 * y1;
				y1 = 0;
			}
			int j9 = y2 - textureInt2;
			l4 += j5 * j9;
			k5 += i6 * j9;
			j6 += l6 * j9;
			if (i7 < k7) {
				y3 -= y1;
				y1 -= y2;
				y2 = anIntArray1472[y2];
				while (--y1 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y2, x3 >> 16, x2 >> 16, c3 >> 8, c2 >> 8, l4,
					k5, j6, i5, l5, k6, z2, depthSlope);
					z2 += depthScale;
					x3 += i7;
					x2 += k7;
					c3 += j7;
					c2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y3 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y2, x1 >> 16, x2 >> 16, c1 >> 8, c2 >> 8, l4,
					k5, j6, i5, l5, k6, z2, depthSlope);
					z2 += depthScale;
					x1 += i8;
					x2 += k7;
					c1 += j8;
					c2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y3 -= y1;
			y1 -= y2;
			y2 = anIntArray1472[y2];
			while (--y1 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y2, x2 >> 16, x3 >> 16, c2 >> 8, c3 >> 8, l4, k5,
						j6, i5, l5, k6, z2, depthSlope);
				z2 += depthScale;
				x3 += i7;
				x2 += k7;
				c3 += j7;
				c2 += l7;
				y2 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y3 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y2, x2 >> 16, x1 >> 16, c2 >> 8, c1 >> 8, l4, k5,
						j6, i5, l5, k6, z2, depthSlope);
				z2 += depthScale;
				x1 += i8;
				x2 += k7;
				c1 += j8;
				c2 += l7;
				y2 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		if (y3 >= DrawingArea.bottomY) {
			return;
		}
		if (y1 > DrawingArea.bottomY) {
			y1 = DrawingArea.bottomY;
		}
		if (y2 > DrawingArea.bottomY) {
			y2 = DrawingArea.bottomY;
		}
		z3 = z3 - depthSlope * x3 + depthSlope;
		if (y1 < y2) {
			x2 = x3 <<= 16;
			c2 = c3 <<= 16;
			if (y3 < 0) {
				y3 -= 0;
				x2 -= k7 * y3;
				z3 -= depthScale * y3;
				x3 -= i8 * y3;
				c2 -= l7 * y3;
				c3 -= j8 * y3;
				y3 = 0;
			}
			x1 <<= 16;
			c1 <<= 16;
			if (y1 < 0) {
				y1 -= 0;
				x1 -= i7 * y1;
				c1 -= j7 * y1;
				y1 = 0;
			}
			final int k9 = y3 - textureInt2;
			l4 += j5 * k9;
			k5 += i6 * k9;
			j6 += l6 * k9;
			if (k7 < i8) {
				y2 -= y1;
				y1 -= y3;
				y3 = anIntArray1472[y3];
				while (--y1 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y3, x2 >> 16, x3 >> 16, c2 >> 8, c3 >> 8, l4,
					k5, j6, i5, l5, k6, z3, depthSlope);
					z3 += depthScale;
					x2 += k7;
					x3 += i8;
					c2 += l7;
					c3 += j8;
					y3 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y2 >= 0) {
					drawTexturedScanline317(DrawingArea.pixels, texels, y3, x2 >> 16, x1 >> 16, c2 >> 8, c1 >> 8, l4,
					k5, j6, i5, l5, k6, z3, depthSlope);
					z3 += depthScale;
					x2 += k7;
					x1 += i7;
					c2 += l7;
					c1 += j7;
					y3 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y2 -= y1;
			y1 -= y3;
			y3 = anIntArray1472[y3];
			while (--y1 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y3, x3 >> 16, x2 >> 16, c3 >> 8, c2 >> 8, l4, k5,
						j6, i5, l5, k6, z3, depthSlope);
				z3 += depthScale;
				x2 += k7;
				x3 += i8;
				c2 += l7;
				c3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y2 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y3, x1 >> 16, x2 >> 16, c1 >> 8, c2 >> 8, l4, k5,
						j6, i5, l5, k6, z3, depthSlope);
				z3 += depthScale;
				x2 += k7;
				x1 += i7;
				c2 += l7;
				c1 += j7;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		x1 = x3 <<= 16;
		c1 = c3 <<= 16;
		if (y3 < 0) {
			y3 -= 0;
			x1 -= k7 * y3;
			z3 -= depthScale * y3;
			x3 -= i8 * y3;
			c1 -= l7 * y3;
			c3 -= j8 * y3;
			y3 = 0;
		}
		x2 <<= 16;
		c2 <<= 16;
		if (y2 < 0) {
			y2 -= 0;
			x2 -= i7 * y2;
			c2 -= j7 * y2;
			y2 = 0;
		}
		int l9 = y3 - textureInt2;
		l4 += j5 * l9;
		k5 += i6 * l9;
		j6 += l6 * l9;
		if (k7 < i8) {
			y1 -= y2;
			y2 -= y3;
			y3 = anIntArray1472[y3];
			while (--y2 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y3, x1 >> 16, x3 >> 16, c1 >> 8, c3 >> 8, l4, k5,
				j6, i5, l5, k6, z3, depthSlope);
				z3 += depthScale;
				x1 += k7;
				x3 += i8;
				c1 += l7;
				c3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y1 >= 0) {
				drawTexturedScanline317(DrawingArea.pixels, texels, y3, x2 >> 16, x3 >> 16, c2 >> 8, c3 >> 8, l4, k5,
				j6, i5, l5, k6, z3, depthSlope);
				z3 += depthScale;
				x2 += i7;
				x3 += i8;
				c2 += j7;
				c3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		y1 -= y2;
		y2 -= y3;
		y3 = anIntArray1472[y3];
		while (--y2 >= 0) {
			drawTexturedScanline317(DrawingArea.pixels, texels, y3, x3 >> 16, x1 >> 16, c3 >> 8, c1 >> 8, l4, k5, j6,
					i5, l5, k6, z3, depthSlope);
			z3 += depthScale;
			x1 += k7;
			x3 += i8;
			c1 += l7;
			c3 += j8;
			y3 += DrawingArea.width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
		while (--y1 >= 0) {
			drawTexturedScanline317(DrawingArea.pixels, texels, y3, x3 >> 16, x2 >> 16, c3 >> 8, c2 >> 8, l4, k5, j6,
					i5, l5, k6, z3, depthSlope);
			z3 += depthScale;
			x2 += i7;
			x3 += i8;
			c2 += j7;
			c3 += j8;
			y3 += DrawingArea.width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
	}

	private static void drawTexturedScanline317(int[] dest, int[] src, int offset, int x1, int x2, int hsl1, int hsl2,
			int t1, int t2, int t3, int t4, int t5, int t6, float z1, float z2) {
		int rgb = 0;
		int pos = 0;

		if (x1 >= x2) {
			return;
		}

		int rotation;
		int n;

		if (textureOutOfDrawingBounds) {
			rotation = (hsl2 - hsl1) / (x2 - x1);
			if (x2 > DrawingArea.lastX) {
				x2 = DrawingArea.lastX;
			}
			if (x1 < 0) {
				hsl1 -= x1 * rotation;
				z1 -= x1 * z2;
				x1 = 0;
			}
			if (x1 >= x2) {
				return;
			}
			n = x2 - x1;
			rotation <<= 12;
			hsl1 <<= 9;
		} else {
			if (x2 - x1 > 7) {
				n = x2 - x1;
				rotation = (hsl2 - hsl1) * anIntArray1468[Math.min(n, anIntArray1468.length - 1)] >> 6;
			} else {
				n = x2 - x1;
				rotation = 0;
			}
			hsl1 <<= 9;
		}

		offset += x1;
		z1 += z2 * x1;

		// Bounds checking
		if (offset < 0 || offset >= dest.length) {
			return;
		}

		int maxPixels = dest.length - offset;
		if (n > maxPixels) {
			n = maxPixels;
		}

		if (n <= 0) {
			return;
		}

		int j4 = 0;
		int l4 = 0;
		int l6 = x1 - textureInt1;
		t1 += (t4 >> 3) * l6;
		t2 += (t5 >> 3) * l6;
		t3 += (t6 >> 3) * l6;

		int l5 = t3 >> 14;
		if (l5 != 0) {
			rgb = t1 / l5;
			pos = t2 / l5;
			// Clamp texture coordinates
			rgb = Math.max(0, Math.min(16256, rgb));
			pos = Math.max(0, Math.min(16256, pos));
		}

		t1 += t4;
		t2 += t5;
		t3 += t6;
		l5 = t3 >> 14;

		if (l5 != 0) {
			j4 = t1 / l5;
			l4 = t2 / l5;
			j4 = Math.max(7, Math.min(16256, j4));
			l4 = Math.max(0, Math.min(16256, l4));
		} else {
			j4 = 7;
			l4 = 0;
		}

		int pixelBrightness = (j4 - rgb) >> 3;
		int pixelPos = (l4 - pos) >> 3;
		rgb += hsl1 & 0x600000;
		int brightness = hsl1 >> 23;
		brightness = Math.max(0, Math.min(31, brightness)); // Clamp brightness

		if (aBoolean1463) {
			// Opaque rendering
			for (int i = 0; i < n; i++) {
				if (offset >= dest.length) break;

				// Calculate texture index with bounds checking
				int texIndex = (pos & 0x3f80) + (rgb >> 7);
				texIndex = Math.max(0, Math.min(src.length - 1, texIndex));

				int color = src[texIndex] >>> brightness;
				dest[offset] = color;

				if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
					depthBuffer[offset] = z1;
				}

				z1 += z2;
				offset++;
				rgb += pixelBrightness;
				pos += pixelPos;

				// Update texture coordinates every 8 pixels
				if ((i & 7) == 7) {
					t1 += t4;
					t2 += t5;
					t3 += t6;
					int i6 = t3 >> 14;
				if (i6 != 0) {
					j4 = t1 / i6;
					l4 = t2 / i6;
					j4 = Math.max(7, Math.min(16256, j4));
					l4 = Math.max(0, Math.min(16256, l4));
				} else {
					j4 = 7;
					l4 = 0;
				}

				pixelBrightness = (j4 - rgb) >> 3;
				pixelPos = (l4 - pos) >> 3;
				hsl1 += rotation;
				rgb = (rgb & 0x1FFFF) + (hsl1 & 0x600000);
				brightness = hsl1 >> 23;
				brightness = Math.max(0, Math.min(31, brightness));
				}
			}
		} else {
			// Transparent rendering
			for (int i = 0; i < n; i++) {
				if (offset >= dest.length) break;

				int texIndex = (pos & 0x3f80) + (rgb >> 7);
				texIndex = Math.max(0, Math.min(src.length - 1, texIndex));

				int color = src[texIndex];
				if (color != 0) {
					color = color >>> brightness;
					dest[offset] = color;

					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}

				z1 += z2;
				offset++;
				rgb += pixelBrightness;
				pos += pixelPos;

				if ((i & 7) == 7) {
					t1 += t4;
					t2 += t5;
					t3 += t6;
					int j6 = t3 >> 14;
					if (j6 != 0) {
						j4 = t1 / j6;
						l4 = t2 / j6;
						j4 = Math.max(7, Math.min(16256, j4));
						l4 = Math.max(0, Math.min(16256, l4));
					} else {
						j4 = 7;
						l4 = 0;
					}

					pixelBrightness = (j4 - rgb) >> 3;
					pixelPos = (l4 - pos) >> 3;
					hsl1 += rotation;
					rgb = (rgb & 0x1FFFF) + (hsl1 & 0x600000);
					brightness = hsl1 >> 23;
					brightness = Math.max(0, Math.min(31, brightness));
				}
			}
		}
	}
	public static void drawTexturedTriangle(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, int var14, int var15, int var16, int var17, int var18) {
		int[] texturePixels = getTexturePixels(var18);
		int var21;
		aBoolean1463 = !textureIsTransparant[var18];
		var21 = var4 - var3;
		int var26 = var1 - var0;
		int var27 = var5 - var3;
		int var31 = var2 - var0;
		int var28 = var7 - var6;
		int var23 = var8 - var6;
		int var29 = 0;
		if(var1 != var0) {
			var29 = (var4 - var3 << 16) / (var1 - var0);
		}

		int var30 = 0;
		if(var2 != var1) {
			var30 = (var5 - var4 << 16) / (var2 - var1);
		}

		int var22 = 0;
		if(var2 != var0) {
			var22 = (var3 - var5 << 16) / (var0 - var2);
		}

		int var32 = var21 * var31 - var27 * var26;
		if(var32 != 0) {
			int var41 = (var28 * var31 - var23 * var26 << 9) / var32;
			int var20 = (var23 * var21 - var28 * var27 << 9) / var32;
			var10 = var9 - var10;
			var13 = var12 - var13;
			var16 = var15 - var16;
			var11 -= var9;
			var14 -= var12;
			var17 -= var15;
			final int FOV = (aBoolean1464 ? WorldController.focalLength : 512);
			int var24 = var11 * var12 - var14 * var9 << 14;
			int var38 = (int)(((long)(var14 * var15 - var17 * var12) << 3 << 14) / (long)FOV);
			int var25 = (int)(((long)(var17 * var9 - var11 * var15) << 14) / (long)FOV);
			int var36 = var10 * var12 - var13 * var9 << 14;
			int var39 = (int)(((long)(var13 * var15 - var16 * var12) << 3 << 14) / (long)FOV);
			int var37 = (int)(((long)(var16 * var9 - var10 * var15) << 14) / (long)FOV);
			int var33 = var13 * var11 - var10 * var14 << 14;
			int var40 = (int)(((long)(var16 * var14 - var13 * var17) << 3 << 14) / (long)FOV);
			int var34 = (int)(((long)(var10 * var17 - var16 * var11) << 14) / (long)FOV);


			int var35;
			if(var0 <= var1 && var0 <= var2) {
				if(var0 < DrawingArea.bottomY) {
					if(var1 > DrawingArea.bottomY) {
						var1 = DrawingArea.bottomY;
					}

					if(var2 > DrawingArea.bottomY) {
						var2 = DrawingArea.bottomY;
					}

					var6 = (var6 << 9) - var41 * var3 + var41;
					if(var1 < var2) {
						var5 = var3 <<= 16;
						if(var0 < 0) {
							var5 -= var22 * var0;
							var3 -= var29 * var0;
							var6 -= var20 * var0;
							var0 = 0;
						}

						var4 <<= 16;
						if(var1 < 0) {
							var4 -= var30 * var1;
							var1 = 0;
						}

						var35 = var0 - textureInt2;
						var24 += var25 * var35;
						var36 += var37 * var35;
						var33 += var34 * var35;
						if((var0 == var1 || var22 >= var29) && (var0 != var1 || var22 <= var30)) {
							var2 -= var1;
							var1 -= var0;
							var0 = anIntArray1472[var0];

							while(true) {
								--var1;
								if(var1 < 0) {
									while(true) {
										--var2;
										if(var2 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var4 >> 16, var5 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
										var5 += var22;
										var4 += var30;
										var6 += var20;
										var0 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var3 >> 16, var5 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
								var5 += var22;
								var3 += var29;
								var6 += var20;
								var0 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						} else {
							var2 -= var1;
							var1 -= var0;
							var0 = anIntArray1472[var0];

							while(true) {
								--var1;
								if(var1 < 0) {
									while(true) {
										--var2;
										if(var2 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var5 >> 16, var4 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
										var5 += var22;
										var4 += var30;
										var6 += var20;
										var0 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var5 >> 16, var3 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
								var5 += var22;
								var3 += var29;
								var6 += var20;
								var0 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						}
					} else {
						var4 = var3 <<= 16;
						if(var0 < 0) {
							var4 -= var22 * var0;
							var3 -= var29 * var0;
							var6 -= var20 * var0;
							var0 = 0;
						}

						var5 <<= 16;
						if(var2 < 0) {
							var5 -= var30 * var2;
							var2 = 0;
						}

						var35 = var0 - textureInt2;
						var24 += var25 * var35;
						var36 += var37 * var35;
						var33 += var34 * var35;
						if((var0 == var2 || var22 >= var29) && (var0 != var2 || var30 <= var29)) {
							var1 -= var2;
							var2 -= var0;
							var0 = anIntArray1472[var0];

							while(true) {
								--var2;
								if(var2 < 0) {
									while(true) {
										--var1;
										if(var1 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var3 >> 16, var5 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
										var5 += var30;
										var3 += var29;
										var6 += var20;
										var0 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var3 >> 16, var4 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
								var4 += var22;
								var3 += var29;
								var6 += var20;
								var0 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						} else {
							var1 -= var2;
							var2 -= var0;
							var0 = anIntArray1472[var0];

							while(true) {
								--var2;
								if(var2 < 0) {
									while(true) {
										--var1;
										if(var1 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var5 >> 16, var3 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
										var5 += var30;
										var3 += var29;
										var6 += var20;
										var0 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var0, var4 >> 16, var3 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
								var4 += var22;
								var3 += var29;
								var6 += var20;
								var0 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						}
					}
				}
			} else if(var1 <= var2) {
				if(var1 < DrawingArea.bottomY) {
					if(var2 > DrawingArea.bottomY) {
						var2 = DrawingArea.bottomY;
					}

					if(var0 > DrawingArea.bottomY) {
						var0 = DrawingArea.bottomY;
					}

					var7 = (var7 << 9) - var41 * var4 + var41;
					if(var2 < var0) {
						var3 = var4 <<= 16;
						if(var1 < 0) {
							var3 -= var29 * var1;
							var4 -= var30 * var1;
							var7 -= var20 * var1;
							var1 = 0;
						}

						var5 <<= 16;
						if(var2 < 0) {
							var5 -= var22 * var2;
							var2 = 0;
						}

						var35 = var1 - textureInt2;
						var24 += var25 * var35;
						var36 += var37 * var35;
						var33 += var34 * var35;
						if((var1 == var2 || var29 >= var30) && (var1 != var2 || var29 <= var22)) {
							var0 -= var2;
							var2 -= var1;
							var1 = anIntArray1472[var1];

							while(true) {
								--var2;
								if(var2 < 0) {
									while(true) {
										--var0;
										if(var0 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var5 >> 16, var3 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
										var3 += var29;
										var5 += var22;
										var7 += var20;
										var1 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var4 >> 16, var3 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
								var3 += var29;
								var4 += var30;
								var7 += var20;
								var1 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						} else {
							var0 -= var2;
							var2 -= var1;
							var1 = anIntArray1472[var1];

							while(true) {
								--var2;
								if(var2 < 0) {
									while(true) {
										--var0;
										if(var0 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var3 >> 16, var5 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
										var3 += var29;
										var5 += var22;
										var7 += var20;
										var1 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var3 >> 16, var4 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
								var3 += var29;
								var4 += var30;
								var7 += var20;
								var1 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						}
					} else {
						var5 = var4 <<= 16;
						if(var1 < 0) {
							var5 -= var29 * var1;
							var4 -= var30 * var1;
							var7 -= var20 * var1;
							var1 = 0;
						}

						var3 <<= 16;
						if(var0 < 0) {
							var3 -= var22 * var0;
							var0 = 0;
						}

						var35 = var1 - textureInt2;
						var24 += var25 * var35;
						var36 += var37 * var35;
						var33 += var34 * var35;
						if(var29 < var30) {
							var2 -= var0;
							var0 -= var1;
							var1 = anIntArray1472[var1];

							while(true) {
								--var0;
								if(var0 < 0) {
									while(true) {
										--var2;
										if(var2 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var3 >> 16, var4 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
										var3 += var22;
										var4 += var30;
										var7 += var20;
										var1 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var5 >> 16, var4 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
								var5 += var29;
								var4 += var30;
								var7 += var20;
								var1 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						} else {
							var2 -= var0;
							var0 -= var1;
							var1 = anIntArray1472[var1];

							while(true) {
								--var0;
								if(var0 < 0) {
									while(true) {
										--var2;
										if(var2 < 0) {
											return;
										}

										drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var4 >> 16, var3 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
										var3 += var22;
										var4 += var30;
										var7 += var20;
										var1 += DrawingArea.width;
										var24 += var25;
										var36 += var37;
										var33 += var34;
									}
								}

								drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var1, var4 >> 16, var5 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
								var5 += var29;
								var4 += var30;
								var7 += var20;
								var1 += DrawingArea.width;
								var24 += var25;
								var36 += var37;
								var33 += var34;
							}
						}
					}
				}
			} else if(var2 < DrawingArea.bottomY) {
				if(var0 > DrawingArea.bottomY) {
					var0 = DrawingArea.bottomY;
				}

				if(var1 > DrawingArea.bottomY) {
					var1 = DrawingArea.bottomY;
				}

				var8 = (var8 << 9) - var41 * var5 + var41;
				if(var0 < var1) {
					var4 = var5 <<= 16;
					if(var2 < 0) {
						var4 -= var30 * var2;
						var5 -= var22 * var2;
						var8 -= var20 * var2;
						var2 = 0;
					}

					var3 <<= 16;
					if(var0 < 0) {
						var3 -= var29 * var0;
						var0 = 0;
					}

					var35 = var2 - textureInt2;
					var24 += var25 * var35;
					var36 += var37 * var35;
					var33 += var34 * var35;
					if(var30 < var22) {
						var1 -= var0;
						var0 -= var2;
						var2 = anIntArray1472[var2];

						while(true) {
							--var0;
							if(var0 < 0) {
								while(true) {
									--var1;
									if(var1 < 0) {
										return;
									}

									drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var4 >> 16, var3 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
									var4 += var30;
									var3 += var29;
									var8 += var20;
									var2 += DrawingArea.width;
									var24 += var25;
									var36 += var37;
									var33 += var34;
								}
							}

							drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var4 >> 16, var5 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
							var4 += var30;
							var5 += var22;
							var8 += var20;
							var2 += DrawingArea.width;
							var24 += var25;
							var36 += var37;
							var33 += var34;
						}
					} else {
						var1 -= var0;
						var0 -= var2;
						var2 = anIntArray1472[var2];

						while(true) {
							--var0;
							if(var0 < 0) {
								while(true) {
									--var1;
									if(var1 < 0) {
										return;
									}

									drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var3 >> 16, var4 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
									var4 += var30;
									var3 += var29;
									var8 += var20;
									var2 += DrawingArea.width;
									var24 += var25;
									var36 += var37;
									var33 += var34;
								}
							}

							drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var5 >> 16, var4 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
							var4 += var30;
							var5 += var22;
							var8 += var20;
							var2 += DrawingArea.width;
							var24 += var25;
							var36 += var37;
							var33 += var34;
						}
					}
				} else {
					var3 = var5 <<= 16;
					if(var2 < 0) {
						var3 -= var30 * var2;
						var5 -= var22 * var2;
						var8 -= var20 * var2;
						var2 = 0;
					}

					var4 <<= 16;
					if(var1 < 0) {
						var4 -= var29 * var1;
						var1 = 0;
					}

					var35 = var2 - textureInt2;
					var24 += var25 * var35;
					var36 += var37 * var35;
					var33 += var34 * var35;
					if(var30 < var22) {
						var0 -= var1;
						var1 -= var2;
						var2 = anIntArray1472[var2];

						while(true) {
							--var1;
							if(var1 < 0) {
								while(true) {
									--var0;
									if(var0 < 0) {
										return;
									}

									drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var4 >> 16, var5 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
									var4 += var29;
									var5 += var22;
									var8 += var20;
									var2 += DrawingArea.width;
									var24 += var25;
									var36 += var37;
									var33 += var34;
								}
							}

							drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var3 >> 16, var5 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
							var3 += var30;
							var5 += var22;
							var8 += var20;
							var2 += DrawingArea.width;
							var24 += var25;
							var36 += var37;
							var33 += var34;
						}
					} else {
						var0 -= var1;
						var1 -= var2;
						var2 = anIntArray1472[var2];

						while(true) {
							--var1;
							if(var1 < 0) {
								while(true) {
									--var0;
									if(var0 < 0) {
										return;
									}

									drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var5 >> 16, var4 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
									var4 += var29;
									var5 += var22;
									var8 += var20;
									var2 += DrawingArea.width;
									var24 += var25;
									var36 += var37;
									var33 += var34;
								}
							}

							drawTexturedLine(DrawingArea.pixels, texturePixels, 0, 0, var2, var5 >> 16, var3 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
							var3 += var30;
							var5 += var22;
							var8 += var20;
							var2 += DrawingArea.width;
							var24 += var25;
							var36 += var37;
							var33 += var34;
						}
					}
				}
			}
		}
	}

	static void drawTexturedLine(int[] var0, int[] var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, int var14) {
		if(textureOutOfDrawingBounds) {
			if(var6 > DrawingArea.lastX) {
				var6 = DrawingArea.lastX;
			}

			if(var5 < 0) {
				var5 = 0;
			}
		}

		if(var5 < var6) {
			var4 += var5;
			var7 += var8 * var5;
			int var17 = var6 - var5;
			int var15;
			int var16;
			int var18;
			int var19;
			int var20;
			int var21;
			int var22;
			int var23;
			if(false) {
				var15 = var5 - textureInt1;
				var9 += (var12 >> 3) * var15;
				var10 += (var13 >> 3) * var15;
				var11 += (var14 >> 3) * var15;
				var19 = var11 >> 12;
			if(var19 != 0) {
				var20 = var9 / var19;
				var18 = var10 / var19;
				if(var20 < 0) {
					var20 = 0;
				} else if(var20 > 4032) {
					var20 = 4032;
				}
			} else {
				var20 = 0;
				var18 = 0;
			}

			var9 += var12;
			var10 += var13;
			var11 += var14;
			var19 = var11 >> 12;
			if(var19 != 0) {
				var22 = var9 / var19;
				var16 = var10 / var19;
				if(var22 < 0) {
					var22 = 0;
				} else if(var22 > 4032) {
					var22 = 4032;
				}
			} else {
				var22 = 0;
				var16 = 0;
			}

			var2 = (var20 << 20) + var18;
			var23 = (var22 - var20 >> 3 << 20) + (var16 - var18 >> 3);
			var17 >>= 3;
			var8 <<= 3;
			var21 = var7 >> 8;
			if(aBoolean1463) {
				if(var17 > 0) {
					do {
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var20 = var22;
						var18 = var16;
						var9 += var12;
						var10 += var13;
						var11 += var14;
						var19 = var11 >> 12;
						if(var19 != 0) {
							var22 = var9 / var19;
							var16 = var10 / var19;
							if(var22 < 0) {
								var22 = 0;
							} else if(var22 > 4032) {
								var22 = 4032;
							}
						} else {
							var22 = 0;
							var16 = 0;
						}

						var2 = (var20 << 20) + var18;
						var23 = (var22 - var20 >> 3 << 20) + (var16 - var18 >> 3);
						var7 += var8;
						var21 = var7 >> 8;
						--var17;
					} while(var17 > 0);
				}

				var17 = var6 - var5 & 7;
				if(var17 > 0) {
					do {
						var3 = var1[(var2 & 4032) + (var2 >>> 26)];
						var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						var2 += var23;
						--var17;
					} while(var17 > 0);

				}
			} else {
				if(var17 > 0) {
					do {
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var20 = var22;
						var18 = var16;
						var9 += var12;
						var10 += var13;
						var11 += var14;
						var19 = var11 >> 12;
						if(var19 != 0) {
							var22 = var9 / var19;
							var16 = var10 / var19;
							if(var22 < 0) {
								var22 = 0;
							} else if(var22 > 4032) {
								var22 = 4032;
							}
						} else {
							var22 = 0;
							var16 = 0;
						}

						var2 = (var20 << 20) + var18;
						var23 = (var22 - var20 >> 3 << 20) + (var16 - var18 >> 3);
						var7 += var8;
						var21 = var7 >> 8;
						--var17;
					} while(var17 > 0);
				}

				var17 = var6 - var5 & 7;
				if(var17 > 0) {
					do {
						if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
							var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
						}

						++var4;
						var2 += var23;
						--var17;
					} while(var17 > 0);

				}
			}
			} else {
				var15 = var5 - textureInt1;
				var9 += (var12 >> 3) * var15;
				var10 += (var13 >> 3) * var15;
				var11 += (var14 >> 3) * var15;
				var19 = var11 >> 14;
				if(var19 != 0) {
					var20 = var9 / var19;
					var18 = var10 / var19;
					if(var20 < 0) {
						var20 = 0;
					} else if(var20 > 16256) {
						var20 = 16256;
					}
				} else {
					var20 = 0;
					var18 = 0;
				}

				var9 += var12;
				var10 += var13;
				var11 += var14;
				var19 = var11 >> 14;
				if(var19 != 0) {
					var22 = var9 / var19;
					var16 = var10 / var19;
					if(var22 < 0) {
						var22 = 0;
					} else if(var22 > 16256) {
						var22 = 16256;
					}
				} else {
					var22 = 0;
					var16 = 0;
				}

				var2 = (var20 << 18) + var18;
				var23 = (var22 - var20 >> 3 << 18) + (var16 - var18 >> 3);
				var17 >>= 3;
				var8 <<= 3;
				var21 = var7 >> 8;
				if(aBoolean1463) {
					if(var17 > 0) {
						do {
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var20 = var22;
							var18 = var16;
							var9 += var12;
							var10 += var13;
							var11 += var14;
							var19 = var11 >> 14;
							if(var19 != 0) {
								var22 = var9 / var19;
								var16 = var10 / var19;
								if(var22 < 0) {
									var22 = 0;
								} else if(var22 > 16256) {
									var22 = 16256;
								}
							} else {
								var22 = 0;
								var16 = 0;
							}

							var2 = (var20 << 18) + var18;
							var23 = (var22 - var20 >> 3 << 18) + (var16 - var18 >> 3);
							var7 += var8;
							var21 = var7 >> 8;
							--var17;
						} while(var17 > 0);
					}

					var17 = var6 - var5 & 7;
					if(var17 > 0) {
						do {
							var3 = var1[(var2 & 16256) + (var2 >>> 25)];
							var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							var2 += var23;
							--var17;
						} while(var17 > 0);

					}
				} else {
					if(var17 > 0) {
						do {
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var20 = var22;
							var18 = var16;
							var9 += var12;
							var10 += var13;
							var11 += var14;
							var19 = var11 >> 14;
							if(var19 != 0) {
								var22 = var9 / var19;
								var16 = var10 / var19;
								if(var22 < 0) {
									var22 = 0;
								} else if(var22 > 16256) {
									var22 = 16256;
								}
							} else {
								var22 = 0;
								var16 = 0;
							}

							var2 = (var20 << 18) + var18;
							var23 = (var22 - var20 >> 3 << 18) + (var16 - var18 >> 3);
							var7 += var8;
							var21 = var7 >> 8;
							--var17;
						} while(var17 > 0);
					}

					var17 = var6 - var5 & 7;
					if(var17 > 0) {
						do {
							if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
								var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
							}

							++var4;
							var2 += var23;
							--var17;
						} while(var17 > 0);

					}
				}
			}
		}
	}

	public static void drawTexturedTriangleold(int y1, int y2, int y3, int x1, int x2, int x3, int c1, int c2, int c3,
			int tx1, int tx2, int tx3, int ty1, int ty2, int ty3, int tz1, int tz2, int tz3, int tex, float z1, float z2,
			float z3) {
		if (!saveDepth) {
			z1 = z2 = z3 = 0;
		}
		c1 = 0x7f - c1 << 1;
		c2 = 0x7f - c2 << 1;
		c3 = 0x7f - c3 << 1;
		int texels[] = getTexturePixels(tex);
		aBoolean1463 = !textureIsTransparant[tex];
		tx2 = tx1 - tx2;
		ty2 = ty1 - ty2;
		tz2 = tz1 - tz2;
		tx3 -= tx1;
		ty3 -= ty1;
		tz3 -= tz1;
		int l4 = (tx3 * ty1 - ty3 * tx1) * WorldController.focalLength << 5;
		int i5 = ty3 * tz1 - tz3 * ty1 << 8;
		int j5 = tz3 * tx1 - tx3 * tz1 << 5;
		int k5 = (tx2 * ty1 - ty2 * tx1) * WorldController.focalLength << 5;
		;
		int l5 = ty2 * tz1 - tz2 * ty1 << 8;
		int i6 = tz2 * tx1 - tx2 * tz1 << 5;
		int j6 = (ty2 * tx3 - tx2 * ty3) * WorldController.focalLength << 5;
		;
		int k6 = tz2 * ty3 - ty2 * tz3 << 8;
		int l6 = tx2 * tz3 - tz2 * tx3 << 5;
		int i7 = 0;
		int j7 = 0;
		if (y2 != y1) {
			i7 = (x2 - x1 << 16) / (y2 - y1);
			j7 = (c2 - c1 << 16) / (y2 - y1);
		}
		int k7 = 0;
		int l7 = 0;
		if (y3 != y2) {
			k7 = (x3 - x2 << 16) / (y3 - y2);
			l7 = (c3 - c2 << 16) / (y3 - y2);
		}
		int i8 = 0;
		int j8 = 0;
		if (y3 != y1) {
			i8 = (x1 - x3 << 16) / (y1 - y3);
			j8 = (c1 - c3 << 16) / (y1 - y3);
		}

		float x21 = x2 - x1;
		float y32 = y2 - y1;
		float x31 = x3 - x1;
		float y31 = y3 - y1;
		float z21 = z2 - z1;
		float z31 = z3 - z1;

		float div = x21 * y31 - x31 * y32;
		float depthSlope = (z21 * y31 - z31 * y32) / div;
		float depthScale = (z31 * x21 - z21 * x31) / div;

		if (y1 <= y2 && y1 <= y3) {
			if (y1 >= DrawingArea.bottomY) {
				return;
			}
			if (y2 > DrawingArea.bottomY) {
				y2 = DrawingArea.bottomY;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			z1 = z1 - depthSlope * x1 + depthSlope;
			if (y2 < y3) {
				x3 = x1 <<= 16;
				c3 = c1 <<= 16;
				if (y1 < 0) {
					x3 -= i8 * y1;
					x1 -= i7 * y1;
					z1 -= depthScale * y1;
					c3 -= j8 * y1;
					c1 -= j7 * y1;
					y1 = 0;
				}
				x2 <<= 16;
				c2 <<= 16;
				if (y2 < 0) {
					x2 -= k7 * y2;
					c2 -= l7 * y2;
					y2 = 0;
				}
				int k8 = y1 - textureInt2;
				l4 += j5 * k8;
				k5 += i6 * k8;
				j6 += l6 * k8;
				if (y1 != y2 && i8 < i7 || y1 == y2 && i8 > k7) {
					y3 -= y2;
					y2 -= y1;
					y1 = anIntArray1472[y1];
					while (--y2 >= 0) {
						drawTexturedScanline(DrawingArea.pixels, texels, y1, x3 >> 16, x1 >> 16, c3, c1, l4, k5, j6, i5,
				l5, k6, z1, depthSlope);
						z1 += depthScale;
						x3 += i8;
						x1 += i7;
						c3 += j8;
						c1 += j7;
						y1 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y3 >= 0) {
						drawTexturedScanline(DrawingArea.pixels, texels, y1, x3 >> 16, x2 >> 16, c3, c2, l4, k5, j6, i5,
						l5, k6, z1, depthSlope);
						z1 += depthScale;
						x3 += i8;
						x2 += k7;
						c3 += j8;
						c2 += l7;
						y1 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					return;
				}
				y3 -= y2;
				y2 -= y1;
				y1 = anIntArray1472[y1];
				while (--y2 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y1, x1 >> 16, x3 >> 16, c1, c3, l4, k5, j6, i5, l5,
					k6, z1, depthSlope);
					z1 += depthScale;
					x3 += i8;
					x1 += i7;
					c3 += j8;
					c1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y3 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y1, x2 >> 16, x3 >> 16, c2, c3, l4, k5, j6, i5, l5,
					k6, z1, depthSlope);
					z1 += depthScale;
					x3 += i8;
					x2 += k7;
					c3 += j8;
					c2 += l7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			x2 = x1 <<= 16;
			c2 = c1 <<= 16;
			if (y1 < 0) {
				x2 -= i8 * y1;
				z1 -= depthScale * y1;
				x1 -= i7 * y1;
				c2 -= j8 * y1;
				c1 -= j7 * y1;
				y1 = 0;
			}
			x3 <<= 16;
			c3 <<= 16;
			if (y3 < 0) {
				x3 -= k7 * y3;
				c3 -= l7 * y3;
				y3 = 0;
			}
			int l8 = y1 - textureInt2;
			l4 += j5 * l8;
			k5 += i6 * l8;
			j6 += l6 * l8;
			if (y1 != y3 && i8 < i7 || y1 == y3 && k7 > i7) {
				y2 -= y3;
				y3 -= y1;
				y1 = anIntArray1472[y1];
				while (--y3 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y1, x2 >> 16, x1 >> 16, c2, c1, l4, k5, j6, i5, l5,
			k6, z1, depthSlope);
					z1 += depthScale;
					x2 += i8;
					x1 += i7;
					c2 += j8;
					c1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y2 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y1, x3 >> 16, x1 >> 16, c3, c1, l4, k5, j6, i5, l5,
					k6, z1, depthSlope);
					z1 += depthScale;
					x3 += k7;
					x1 += i7;
					c3 += l7;
					c1 += j7;
					y1 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y2 -= y3;
			y3 -= y1;
			y1 = anIntArray1472[y1];
			while (--y3 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y1, x1 >> 16, x2 >> 16, c1, c2, l4, k5, j6, i5, l5, k6,
				z1, depthSlope);
				z1 += depthScale;
				x2 += i8;
				x1 += i7;
				c2 += j8;
				c1 += j7;
				y1 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y2 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y1, x1 >> 16, x3 >> 16, c1, c3, l4, k5, j6, i5, l5, k6,
				z1, depthSlope);
				z1 += depthScale;
				x3 += k7;
				x1 += i7;
				c3 += l7;
				c1 += j7;
				y1 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		if (y2 <= y3) {
			if (y2 >= DrawingArea.bottomY) {
				return;
			}
			if (y3 > DrawingArea.bottomY) {
				y3 = DrawingArea.bottomY;
			}
			if (y1 > DrawingArea.bottomY) {
				y1 = DrawingArea.bottomY;
			}
			z2 = z2 - depthSlope * x2 + depthSlope;
			if (y3 < y1) {
				x1 = x2 <<= 16;
				c1 = c2 <<= 16;
				if (y2 < 0) {
					x1 -= i7 * y2;
					x2 -= k7 * y2;
					z2 -= depthScale * y2;
					c1 -= j7 * y2;
					c2 -= l7 * y2;
					y2 = 0;
				}
				x3 <<= 16;
				c3 <<= 16;
				if (y3 < 0) {
					x3 -= i8 * y3;
					c3 -= j8 * y3;
					y3 = 0;
				}
				int i9 = y2 - textureInt2;
				l4 += j5 * i9;
				k5 += i6 * i9;
				j6 += l6 * i9;
				if (y2 != y3 && i7 < k7 || y2 == y3 && i7 > i8) {
					y1 -= y3;
					y3 -= y2;
					y2 = anIntArray1472[y2];
					while (--y3 >= 0) {
						drawTexturedScanline(DrawingArea.pixels, texels, y2, x1 >> 16, x2 >> 16, c1, c2, l4, k5, j6, i5,
				l5, k6, z2, depthSlope);
						z2 += depthScale;
						x1 += i7;
						x2 += k7;
						c1 += j7;
						c2 += l7;
						y2 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					while (--y1 >= 0) {
						drawTexturedScanline(DrawingArea.pixels, texels, y2, x1 >> 16, x3 >> 16, c1, c3, l4, k5, j6, i5,
						l5, k6, z2, depthSlope);
						z2 += depthScale;
						x1 += i7;
						x3 += i8;
						c1 += j7;
						c3 += j8;
						y2 += DrawingArea.width;
						l4 += j5;
						k5 += i6;
						j6 += l6;
					}
					return;
				}
				y1 -= y3;
				y3 -= y2;
				y2 = anIntArray1472[y2];
				while (--y3 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y2, x2 >> 16, x1 >> 16, c2, c1, l4, k5, j6, i5, l5,
				k6, z2, depthSlope);
					z2 += depthScale;
					x1 += i7;
					x2 += k7;
					c1 += j7;
					c2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y1 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y2, x3 >> 16, x1 >> 16, c3, c1, l4, k5, j6, i5, l5,
				k6, z2, depthSlope);
					z2 += depthScale;
					x1 += i7;
					x3 += i8;
					c1 += j7;
					c3 += j8;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			x3 = x2 <<= 16;
			c3 = c2 <<= 16;
			if (y2 < 0) {
				x3 -= i7 * y2;
				z2 -= depthScale * y2;
				x2 -= k7 * y2;
				c3 -= j7 * y2;
				c2 -= l7 * y2;
				y2 = 0;
			}
			x1 <<= 16;
			c1 <<= 16;
			if (y1 < 0) {
				x1 -= i8 * y1;
				c1 -= j8 * y1;
				y1 = 0;
			}
			int j9 = y2 - textureInt2;
			l4 += j5 * j9;
			k5 += i6 * j9;
			j6 += l6 * j9;
			if (i7 < k7) {
				y3 -= y1;
				y1 -= y2;
				y2 = anIntArray1472[y2];
				while (--y1 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y2, x3 >> 16, x2 >> 16, c3, c2, l4, k5, j6, i5, l5,
			k6, z2, depthSlope);
					z2 += depthScale;
					x3 += i7;
					x2 += k7;
					c3 += j7;
					c2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y3 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y2, x1 >> 16, x2 >> 16, c1, c2, l4, k5, j6, i5, l5,
					k6, z2, depthSlope);
					z2 += depthScale;
					x1 += i8;
					x2 += k7;
					c1 += j8;
					c2 += l7;
					y2 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y3 -= y1;
			y1 -= y2;
			y2 = anIntArray1472[y2];
			while (--y1 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y2, x2 >> 16, x3 >> 16, c2, c3, l4, k5, j6, i5, l5, k6,
				z2, depthSlope);
				z2 += depthScale;
				x3 += i7;
				x2 += k7;
				c3 += j7;
				c2 += l7;
				y2 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y3 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y2, x2 >> 16, x1 >> 16, c2, c1, l4, k5, j6, i5, l5, k6,
						z2, depthSlope);
				z2 += depthScale;
				x1 += i8;
				x2 += k7;
				c1 += j8;
				c2 += l7;
				y2 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		if (y3 >= DrawingArea.bottomY) {
			return;
		}
		if (y1 > DrawingArea.bottomY) {
			y1 = DrawingArea.bottomY;
		}
		if (y2 > DrawingArea.bottomY) {
			y2 = DrawingArea.bottomY;
		}
		z3 = z3 - depthSlope * x3 + depthSlope;
		if (y1 < y2) {
			x2 = x3 <<= 16;
			c2 = c3 <<= 16;
			if (y3 < 0) {
				x2 -= k7 * y3;
				x3 -= i8 * y3;
				z3 -= depthScale * y3;
				c2 -= l7 * y3;
				c3 -= j8 * y3;
				y3 = 0;
			}
			x1 <<= 16;
			c1 <<= 16;
			if (y1 < 0) {
				x1 -= i7 * y1;
				c1 -= j7 * y1;
				y1 = 0;
			}
			int k9 = y3 - textureInt2;
			l4 += j5 * k9;
			k5 += i6 * k9;
			j6 += l6 * k9;
			if (k7 < i8) {
				y2 -= y1;
				y1 -= y3;
				y3 = anIntArray1472[y3];
				while (--y1 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y3, x2 >> 16, x3 >> 16, c2, c3, l4, k5, j6, i5, l5,
			k6, z3, depthSlope);
					z3 += depthScale;
					x2 += k7;
					x3 += i8;
					c2 += l7;
					c3 += j8;
					y3 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				while (--y2 >= 0) {
					drawTexturedScanline(DrawingArea.pixels, texels, y3, x2 >> 16, x1 >> 16, c2, c1, l4, k5, j6, i5, l5,
					k6, z3, depthSlope);
					z3 += depthScale;
					x2 += k7;
					x1 += i7;
					c2 += l7;
					c1 += j7;
					y3 += DrawingArea.width;
					l4 += j5;
					k5 += i6;
					j6 += l6;
				}
				return;
			}
			y2 -= y1;
			y1 -= y3;
			y3 = anIntArray1472[y3];
			while (--y1 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y3, x3 >> 16, x2 >> 16, c3, c2, l4, k5, j6, i5, l5, k6,
				z3, depthSlope);
				z3 += depthScale;
				x2 += k7;
				x3 += i8;
				c2 += l7;
				c3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y2 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y3, x1 >> 16, x2 >> 16, c1, c2, l4, k5, j6, i5, l5, k6,
						z3, depthSlope);
				z3 += depthScale;
				x2 += k7;
				x1 += i7;
				c2 += l7;
				c1 += j7;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		x1 = x3 <<= 16;
		c1 = c3 <<= 16;
		if (y3 < 0) {
			x1 -= k7 * y3;
			x3 -= i8 * y3;
			z3 -= depthScale * y3;
			c1 -= l7 * y3;
			c3 -= j8 * y3;
			y3 = 0;
		}
		x2 <<= 16;
		c2 <<= 16;
		if (y2 < 0) {
			x2 -= i7 * y2;
			c2 -= j7 * y2;
			y2 = 0;
		}
		int l9 = y3 - textureInt2;
		l4 += j5 * l9;
		k5 += i6 * l9;
		j6 += l6 * l9;
		if (k7 < i8) {
			y1 -= y2;
			y2 -= y3;
			y3 = anIntArray1472[y3];
			while (--y2 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y3, x1 >> 16, x3 >> 16, c1, c3, l4, k5, j6, i5, l5, k6,
		z3, depthSlope);
				z3 += depthScale;
				x1 += k7;
				x3 += i8;
				c1 += l7;
				c3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			while (--y1 >= 0) {
				drawTexturedScanline(DrawingArea.pixels, texels, y3, x2 >> 16, x3 >> 16, c2, c3, l4, k5, j6, i5, l5, k6,
				z3, depthSlope);
				z3 += depthScale;
				x2 += i7;
				x3 += i8;
				c2 += j7;
				c3 += j8;
				y3 += DrawingArea.width;
				l4 += j5;
				k5 += i6;
				j6 += l6;
			}
			return;
		}
		y1 -= y2;
		y2 -= y3;
		y3 = anIntArray1472[y3];
		while (--y2 >= 0) {
			drawTexturedScanline(DrawingArea.pixels, texels, y3, x3 >> 16, x1 >> 16, c3, c1, l4, k5, j6, i5, l5, k6, z3,
					depthSlope);
			z3 += depthScale;
			x1 += k7;
			x3 += i8;
			c1 += l7;
			c3 += j8;
			y3 += DrawingArea.width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
		while (--y1 >= 0) {
			drawTexturedScanline(DrawingArea.pixels, texels, y3, x3 >> 16, x2 >> 16, c3, c2, l4, k5, j6, i5, l5, k6, z3,
					depthSlope);
			z3 += depthScale;
			x2 += i7;
			x3 += i8;
			c2 += j7;
			c3 += j8;
			y3 += DrawingArea.width;
			l4 += j5;
			k5 += i6;
			j6 += l6;
		}
	}

	private static void drawTexturedScanline(int dest[], int src[], int offset, int x1, int x2, int hsl1, int hsl2,
			int t1, int t2, int t3, int t4, int t5, int t6, float z1, float z2) {
		int darken = 0;
		int srcPos = 0;

		if (x1 >= x2) {
			return;
		}

		int dl = 0;
		int n;

		if (x2 - x1 > 0) {
			dl = (hsl2 - hsl1) / (x2 - x1);
		}

		if (textureOutOfDrawingBounds) {
			if (x2 > DrawingArea.lastX) {
				x2 = DrawingArea.lastX;
			}
			if (x1 < 0) {
				hsl1 -= x1 * dl;
				z1 -= x1 * z2;  // CRITICAL FIX: was missing this
				x1 = 0;
			}
		}

		if (x1 >= x2) {
			return;
		}

		n = x2 - x1;
		offset += x1;
		z1 += z2 * x1;

		// CRITICAL FIX: Bounds checking
		if (offset < 0 || offset >= dest.length) {
			return;
		}

		int maxPixels = dest.length - offset;
		if (n > maxPixels) {
			n = maxPixels;
		}

		if (n <= 0) {
			return;
		}

		int j4 = 0;
		int l4 = 0;
		int l6 = x1 - textureInt1;
		t1 += (t4 >> 3) * l6;
		t2 += (t5 >> 3) * l6;
		t3 += (t6 >> 3) * l6;

		int l5 = t3 >> 14;
		if (l5 != 0) {
			darken = t1 / l5;
			srcPos = t2 / l5;
			// CRITICAL FIX: Clamp texture coordinates
			if (darken < 0) darken = 0;
			else if (darken > 16256) darken = 16256;
			if (srcPos < 0) srcPos = 0;
			else if (srcPos > 16256) srcPos = 16256;
		}

		t1 += t4;
		t2 += t5;
		t3 += t6;
		l5 = t3 >> 14;

		if (l5 != 0) {
			j4 = t1 / l5;
			l4 = t2 / l5;
			if (j4 < 7) j4 = 7;
			else if (j4 > 16256) j4 = 16256;
			if (l4 < 0) l4 = 0;
			else if (l4 > 16256) l4 = 16256;
		} else {
			j4 = 7;
			l4 = 0;
		}

		int j7 = (j4 - darken) >> 3;
		int l7 = (l4 - srcPos) >> 3;

		// Process 8 pixels at a time for performance
		int loops = n >> 3;
		int remainder = n & 7;

		if (aBoolean1463) {
			// Opaque rendering - unrolled loop for performance
			while (loops-- > 0) {
				// CRITICAL FIX: Bounds check texture index
				int texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;  // CRITICAL FIX: Clamp brightness
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				// Repeat 7 more times (unrolled)
				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				offset++;
				z1 += z2;

				// Update perspective texture coordinates
				darken = j4;
				srcPos = l4;
				t1 += t4;
				t2 += t5;
				t3 += t6;
				int i6 = t3 >> 14;
					if (i6 != 0) {
						j4 = t1 / i6;
						l4 = t2 / i6;
						if (j4 < 7) j4 = 7;
						else if (j4 > 16256) j4 = 16256;
						if (l4 < 0) l4 = 0;
						else if (l4 > 16256) l4 = 16256;
					} else {
						j4 = 7;
						l4 = 0;
					}
					j7 = (j4 - darken) >> 3;
					l7 = (l4 - srcPos) >> 3;
					hsl1 += dl;
			}

			// Handle remaining pixels
			while (remainder-- > 0) {
				int texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int rgb = src[texIndex];
					int l = (hsl1 >> 16) & 0xFF;
					dest[offset] = ((rgb & 0xff00ff) * l & ~0xff00ff) + ((rgb & 0xff00) * l & 0xff0000) >> 8;
					if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
						depthBuffer[offset] = z1;
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;
			}
		} else {
			// Transparent rendering - similar unrolled loop
			while (loops-- > 0) {
				int texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				// Repeat 7 more times
				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;

				texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;

				// Update perspective
				darken = j4;
				srcPos = l4;
				t1 += t4;
				t2 += t5;
				t3 += t6;
				int j6 = t3 >> 14;
						if (j6 != 0) {
							j4 = t1 / j6;
							l4 = t2 / j6;
							if (j4 < 7) j4 = 7;
							else if (j4 > 16256) j4 = 16256;
							if (l4 < 0) l4 = 0;
							else if (l4 > 16256) l4 = 16256;
						} else {
							j4 = 7;
							l4 = 0;
						}
						j7 = (j4 - darken) >> 3;
						l7 = (l4 - srcPos) >> 3;
						hsl1 += dl;
			}

			// Handle remaining pixels
			while (remainder-- > 0) {
				int texIndex = (srcPos & 0x3f80) + (darken >> 7);
				if (texIndex >= 0 && texIndex < src.length && offset < dest.length) {
					int i9 = src[texIndex];
					if (i9 != 0) {
						int l = (hsl1 >> 16) & 0xFF;
						dest[offset] = ((i9 & 0xff00ff) * l & ~0xff00ff) + ((i9 & 0xff00) * l & 0xff0000) >> 8;
						if (saveDepth && depthBuffer != null && offset < depthBuffer.length) {
							depthBuffer[offset] = z1;
						}
					}
				}
				z1 += z2;
				offset++;
				darken += j7;
				srcPos += l7;
				hsl1 += dl;
			}
		}
	}
	public static void drawArc(int x, int y, int width, int height, int strokeWidth, double v, double arcLength, int i, int alpha, int closure, boolean fill) {
	}

	public static final int method4025(int var0, int var1, int var2, int var3) {
		return var0 * var2 - var3 * var1 >> 16; // L: 2666
	}

	public static final int method4044(int var0, int var1, int var2, int var3) {
		return var3 * var0 + var2 * var1 >> 16; // L: 2670
	}

	public static final int method4045(int var0, int var1, int var2, int var3) {
		return var0 * var2 + var3 * var1 >> 16; // L: 2674
	}

	public static final int method4046(int var0, int var1, int var2, int var3) {
		return var2 * var1 - var3 * var0 >> 16; // L: 2678
	}

	public static void finalizeFrame() {
		applyFrameEffects();
		
	}
	
	// Add these methods to your existing Rasterizer.java file:

	public static void initializeGPUPlugin(CustomGameFrame gameFrame) {
		try {
			System.out.println("Initializing GPU Plugin...");
			gpuPlugin = gameFrame.getGPUPlugin();

			if (gpuPlugin.initialize(DrawingArea.width, DrawingArea.height)) {
				System.out.println("GPU Plugin initialized successfully");
				gpuPlugin.enable();
				System.out.println("GPU Plugin enabled");
			} else {
				System.out.println("GPU Plugin initialization failed - using software rendering");
			}
		} catch (Exception e) {
			System.err.println("Failed to initialize GPU Plugin: " + e.getMessage());
			e.printStackTrace();
			gpuPlugin = null;
		}
	}

	public static void applyGPUEffects() {
		if (gpuPlugin != null && gpuPlugin.isEnabled()) {
			try {
				gpuPlugin.updatePixelBuffer(pixels, width, height);
				gpuPlugin.applyEffects();
			} catch (Exception e) {
				System.err.println("GPU effects application failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	// Add this method to Rasterizer.java
	public static void applyFrameEffects() {
		if (gpuPlugin != null && gpuPlugin.isEnabled()) {
			//System.out.println("DEBUG: GPU plugin found and enabled");
			try {
				gpuPlugin.updatePixelBuffer(pixels, width, height);
				gpuPlugin.applyEffects();
				//System.out.println("DEBUG: Effects applied successfully");
			} catch (Exception e) {
				//System.err.println("Frame effects failed: " + e.getMessage());
			}
		} else {
			//System.out.println("DEBUG: GPU plugin is null or disabled");
		}
	}

	// Add this helper method to Rasterizer.java
	private static boolean isGPUDebugModeEnabled() {
		try {
			return gpuPlugin != null && gpuPlugin.getConfig() != null && 
					gpuPlugin.getConfig().debugMode;
		} catch (Exception e) {
			return false; // Fallback if config access fails
		}
	}
	public static void onFrameComplete() {
		applyGPUEffects();
	}

	public static void testGPUPlugin() {
		System.out.println("=== GPU Plugin Test ===");
		if (gpuPlugin == null) {
			//System.out.println("GPU Plugin is null - not initialized");
			return;
		}

		//System.out.println("GPU Plugin state:");
		//System.out.println("  - Initialized: " + gpuPlugin.isInitialized());
		//System.out.println("  - Enabled: " + gpuPlugin.isEnabled());
		//System.out.println("  - Pixel buffer: " + (pixels != null ? pixels.length + " pixels" : "null"));
		//System.out.println("  - Dimensions: " + width + "x" + height);

		if (pixels != null && pixels.length > 0) {
			int originalPixel = pixels[0];
			System.out.println("  - First pixel before: 0x" + Integer.toHexString(originalPixel));

			applyGPUEffects();

			int newPixel = pixels[0];
			System.out.println("  - First pixel after: 0x" + Integer.toHexString(newPixel));

			if (newPixel != originalPixel) {
				System.out.println("SUCCESS: GPU Plugin is working!");
			} else {
				//System.out.println("ISSUE: GPU Plugin didn't modify pixels");
			}
		}
		//System.out.println("=== End GPU Test ===");
	}
	
}
