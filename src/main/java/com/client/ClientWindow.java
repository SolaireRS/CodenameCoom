package com.client;

import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.awt.*;
import javax.imageio.ImageIO;
import javax.swing.*;

import com.client.features.gameframe.ScreenMode;
import com.client.sign.Signlink;

public class ClientWindow extends Client implements ActionListener, WindowListener {

	private static final long serialVersionUID = -6978617783576386732L;
	public static JFrame frame;
	public static Insets insets;

	public static void popupMessage(String...messages) {
		StringBuilder builder = new StringBuilder();
		for (String message : messages) {
			builder.append(message);
			builder.append("\n");
		}
		JOptionPane.showMessageDialog(null, builder.toString());
	}

	public static void errorOccurredMessage(String code) {
		StringBuilder builder = new StringBuilder();
		builder.append("An error (" + code + ") has occurred that caused you to get kicked from the game.");
		builder.append("\n");
		builder.append("Please report this error by uploading the following file to Discord:");
		builder.append("\n");
		builder.append(Signlink.getCacheDirectory() + Configuration.ERROR_LOG_FILE);
		JOptionPane.showMessageDialog(null, builder.toString());
	}

	public void initUI() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			frame = new JFrame(Configuration.clientTitle);
			frame.setLayout(new BorderLayout());
			setFocusTraversalKeysEnabled(false);
			frame.setResizable(false);
			try {
				InputStream image = ClassLoader.getSystemResourceAsStream("icon.png");
				if (image == null) {
					System.err.println("Could not load icon image.");
				} else {
					BufferedImage bufferedImage = ImageIO.read(image);
					frame.setIconImage(bufferedImage);
				}
			} catch (IOException e) {
				System.err.println("Cannot get icon image from url.");
			}
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			//Runtime.getRuntime().addShutdownHook(new Thread(DiscordRPC::discordShutdown, "Shutdown-Thread"));
			JPanel gamePanel = new JPanel();
			gamePanel.setLayout(new BorderLayout());
			gamePanel.add(this);
			gamePanel.setPreferredSize(ScreenMode.FIXED.getDimensions());
			frame.getContentPane().add(gamePanel, BorderLayout.CENTER);
			frame.pack();
			insets = frame.getInsets();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
			init();

			frame.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent componentEvent) {
					if (Client.getUserSettings() == null)
						return;
					if (Client.currentScreenMode == ScreenMode.FIXED && Client.getUserSettings().isStretchedMode()) {
						Client.getUserSettings().setStretchedModeDimensions(componentEvent.getComponent().getSize());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean isResizable() {
		return isResizable(Client.currentScreenMode);
	}

	public static boolean isResizable(ScreenMode mode) {
		return mode.isResizable() || mode == ScreenMode.FIXED && Client.getUserSettings().isStretchedMode();
	}

	public static Point getStretchedMouseCoordinates(MouseEvent mouseEvent) {
		//return mouseEvent.getPoint();
		return getStretchedMouseCoordinates(mouseEvent.getPoint());
	}

	public static Point getStretchedMouseCoordinates(Point point) {
		Point2D.Double scale = RSImageProducer.getStretchScale();
		return new Point((int) (point.getX() / scale.getX()), (int) (point.getY() / scale.getY()));
	}

	public ClientWindow(String args[]) {
		super();
		try {
			com.client.sign.Signlink.startpriv(InetAddress.getByName(server));
			initUI();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public URL getCodeBase() {
		try {
			return new URL("http://" + server + "/overlays");
		} catch (Exception e) {
			return super.getCodeBase();
		}
	}

	@Override
	public URL getDocumentBase() {
		return getCodeBase();
	}

	public void loadError(String s) {
		System.out.println("loadError: " + s);
	}

	@Override
	public String getParameter(String key) {
		return "";
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		
	}
	
	public static JFrame getFrame() {
		return frame;
	}

	public static Insets getInset() {
		return insets;
	}

	public Client getActualClient() {
	    // This should return the actual Client instance that ClientWindow contains
	    // You need to adjust this based on how your ClientWindow is structured
	    
	    // If ClientWindow extends Client:
	    if (this instanceof Client) {
	        return (Client) this;
	    }
	    
	    // If ClientWindow contains a Client field:
	    // return this.client; // uncomment and adjust field name
	    
	    // If ClientWindow creates a Client internally:
	    // return this.gameClient; // uncomment and adjust field name
	    
	    // For now, return null with debug info
	    System.out.println("Warning: getActualClient() returning null - needs implementation");
	    return null;
	}

}