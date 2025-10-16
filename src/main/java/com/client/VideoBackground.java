package com.client;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;

public class VideoBackground {
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private BufferedImage currentFrame;
    private WritableImage fxImage;
    private boolean initialized = false;
    private int targetWidth;
    private int targetHeight;

    public VideoBackground(String videoPath, int width, int height) {
        this.targetWidth = width;
        this.targetHeight = height;

        // Initialize JavaFX
        new JFXPanel();

        Platform.runLater(() -> {
            try {
                File videoFile = new File(videoPath);
                Media media = new Media(videoFile.toURI().toString());
                mediaPlayer = new MediaPlayer(media);
                mediaView = new MediaView(mediaPlayer);

                mediaView.setFitWidth(width);
                mediaView.setFitHeight(height);
                mediaView.setPreserveRatio(false);
                mediaView.setSmooth(true);

                mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                mediaPlayer.play();

                fxImage = new WritableImage(width, height);
                currentFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                initialized = true;

                System.out.println("Video player initialized: " + width + "x" + height);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        while (!initialized) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public BufferedImage getNextFrame() {
        if (!initialized || mediaView == null) {
            return currentFrame;
        }

        try {
            Platform.runLater(() -> {
                mediaView.snapshot(new SnapshotParameters(), fxImage);
                BufferedImage temp = SwingFXUtils.fromFXImage(fxImage, null);

                if (temp != null) {
                    Graphics2D g = currentFrame.createGraphics();
                    // Keep your original quality settings that worked
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                    g.drawImage(temp, 0, 0, targetWidth, targetHeight, null);
                    g.dispose();
                }
            });
        } catch (Exception e) {
            // Return last valid frame
        }

        return currentFrame;
    }

    public void stop() {
        if (mediaPlayer != null) {
            Platform.runLater(() -> {
                mediaPlayer.stop();
                mediaPlayer.dispose();
            });
        }
    }
}