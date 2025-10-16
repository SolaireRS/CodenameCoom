package com.client;

import com.client.features.gameframe.ScreenMode;
import java.awt.geom.Point2D;
import java.util.*;
import java.awt.*;
import java.awt.image.*;

public final class RSImageProducer {
	
	// Cache for scaled images to avoid recalculating every frame
	private static Point2D.Double cachedScale = new Point2D.Double(1, 1);
	private static long lastScaleCheck = 0;
	private static final long SCALE_CHECK_INTERVAL = 50; // Check scale every 50ms
	
	// GPU acceleration via VolatileImage
	private VolatileImage volatileImage;
	private GraphicsConfiguration gc;
	
	public RSImageProducer(int width, int height, Component component) {
		this.width = width;
		this.height = height;
		this.component = component;
		int count = width * height;
		canvasRaster = new int[count];
		image = new BufferedImage(COLOR_MODEL, Raster.createWritableRaster(
				COLOR_MODEL.createCompatibleSampleModel(width, height),
				new DataBufferInt(canvasRaster, count), null), false,
				new Hashtable<Object, Object>());
		initDrawingArea();
		
		// Initialize GPU-accelerated image if possible
		try {
			if (component != null) {
				gc = component.getGraphicsConfiguration();
				if (gc != null) {
					volatileImage = gc.createCompatibleVolatileImage(width, height, Transparency.OPAQUE);
					System.out.println("GPU acceleration enabled via VolatileImage");
				}
			}
		} catch (Exception e) {
			System.err.println("Could not create VolatileImage, falling back to BufferedImage: " + e.getMessage());
			volatileImage = null;
		}
	}
	
	public void drawGraphics(int x, int y, Graphics gfx) {
		draw(gfx, x, y);
	}
	
	public void draw(Graphics gfx, int x, int y) {
		// Use GPU-accelerated rendering if available
		if (volatileImage != null && gc != null) {
			drawWithVolatileImage(gfx, x, y);
		} else {
			// Fallback to regular rendering
			drawScaledImage(image, gfx, x, y, width, height);
		}
	}
	
	private void drawWithVolatileImage(Graphics gfx, int x, int y) {
		do {
			// Validate and recreate if needed
			int validationCode = volatileImage.validate(gc);
			if (validationCode == VolatileImage.IMAGE_INCOMPATIBLE) {
				volatileImage = gc.createCompatibleVolatileImage(width, height, Transparency.OPAQUE);
			}
			
			// Draw BufferedImage to VolatileImage (this stays in GPU memory)
			Graphics2D vg = volatileImage.createGraphics();
			
			// Fast rendering hints for the transfer
			vg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			vg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			
			vg.drawImage(image, 0, 0, null);
			vg.dispose();
			
			// Now draw the volatile image to screen (GPU accelerated)
			drawScaledImage(volatileImage, gfx, x, y, width, height);
			
		} while (volatileImage.contentsLost());
	}
	
	public void draw(Graphics gfx, int x, int y, int clipX, int clipY,
			int clipWidth, int clipHeight) {
		Shape tmp = gfx.getClip();
		try {
			clip.x = clipX;
			clip.y = clipY;
			clip.width = clipWidth;
			clip.height = clipHeight;
			gfx.setClip(clip);
			
			// Use GPU acceleration if available
			if (volatileImage != null && gc != null) {
				drawWithVolatileImage(gfx, x, y);
			} else {
				gfx.drawImage(image, x, y, component);
			}
		} finally {
			gfx.setClip(tmp);
		}
	}
	
	public void initDrawingArea() {
		DrawingArea.initDrawingArea(height, width, canvasRaster, null);
	}
	
	// Recreate volatile image when window is resized
	public void invalidateVolatileImage() {
		if (volatileImage != null && gc != null) {
			volatileImage = gc.createCompatibleVolatileImage(width, height, Transparency.OPAQUE);
		}
	}
	
	public final int[] canvasRaster;
	public final int width;
	public final int height;
	public final BufferedImage image;
	public final Component component;
	private final Rectangle clip = new Rectangle();
	private static final ColorModel COLOR_MODEL = new DirectColorModel(32, 0xff0000, 0xff00, 0xff);
	
	public static Point2D.Double getStretchScale() {
		if (Client.currentScreenMode == ScreenMode.FIXED && Client.stretched) {
			if (Client.instance == null || Client.instance.getGameComponent() == null) {
				return cachedScale;
			}
			
			// Throttle scale calculations
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastScaleCheck > SCALE_CHECK_INTERVAL) {
				double widthScale = (double) Client.instance.getGameComponent().getWidth() / 765.0;
				double heightScale = (double) Client.instance.getGameComponent().getHeight() / 503.0;
				cachedScale = new Point2D.Double(Math.max(1, widthScale), Math.max(1, heightScale));
				lastScaleCheck = currentTime;
			}
			return cachedScale;
		}
		return new Point2D.Double(1, 1);
	}
	
	public static void drawScaledImage(Image image, Graphics gfx, int x, int y, int width, int height) {
		if (Client.currentScreenMode == ScreenMode.FIXED && Client.stretched) {
			Graphics2D gfx2d = (Graphics2D) gfx;
			Point2D.Double scale = getStretchScale();
			
			// Performance mode: nearest neighbor (fastest)
			if (Client.instance != null && Client.instance.isStretchedFast()) {
				gfx2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				gfx2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
				gfx2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
				gfx2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
			} 
			// Ultra pretty mode: high quality
			else if (Client.ultraPretty) {
				gfx2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				gfx2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				gfx2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			} 
			// Default mode: balanced
			else {
				gfx2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				gfx2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			}
			
			int stretchedWidth = (int) Math.floor((double) width * scale.getX());
			int stretchedHeight = (int) Math.floor((double) height * scale.getY());
			x = (int) (x * scale.getX());
			y = (int) (y * scale.getY());
			
			gfx2d.drawImage(image, x, y, stretchedWidth, stretchedHeight, null);
		} else {
			gfx.drawImage(image, x, y, null);
		}
	}
	
	// Call this to force scale recalculation
	public static void invalidateScaleCache() {
		lastScaleCheck = 0;
	}
}