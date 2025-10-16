package com.client;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public final class GraphicsBuffer {
	public final int[] canvasRaster;
	public final int canvasWidth;
	public final int canvasHeight;
	public BufferedImage bufferedImage;
	public final float[] depthBuffer;

	public void resetDepthBuffer() {
		if (this.depthBuffer == null) {
			return;
		}
		int length = this.depthBuffer.length;
		int loops = length - (length & 0x7);
		int position = 0;
		while (position < loops) {
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
			this.depthBuffer[(position++)] = 2.14748365E9F;
		}
		while (position < length) {
			this.depthBuffer[(position++)] = 2.14748365E9F;
		}
	}

	public GraphicsBuffer(int canvasWidth, int canvasHeight) {
		this.canvasWidth = canvasWidth;
		this.canvasHeight = canvasHeight;
		this.bufferedImage = new BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_RGB);
		this.canvasRaster = ((DataBufferInt) this.bufferedImage.getRaster().getDataBuffer()).getData();
		this.depthBuffer = new float[canvasWidth * canvasHeight];
		setCanvas();
	}

	public void drawGraphics(int x, Graphics graphics, int y) {
	    if (Client.stretched && Client.instance != null && Client.instance.isStretchedEnabled()) {
	        Graphics2D g2d = (Graphics2D) graphics;
	        
	        Point2D.Double scale = RSImageProducer.getStretchScale();
	        int scaledWidth = (int)(this.canvasWidth * scale.getX());
	        int scaledHeight = (int)(this.canvasHeight * scale.getY());
	        
	        // Always use fastest rendering for performance
	        if (Client.instance.isStretchedFast()) {
	            // Nearest neighbor - fastest, pixelated look
	            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
	        } else {
	            // Bilinear - balanced quality and speed
	            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
	        }
	        
	        g2d.drawImage(this.bufferedImage, y, x, scaledWidth, scaledHeight, null);
	    } else {
	        graphics.drawImage(this.bufferedImage, y, x, null);
	    }
	}

	public void setCanvas() {
		DrawingArea.initDrawingArea(this.canvasHeight, this.canvasWidth, this.canvasRaster, this.depthBuffer);
	}
}