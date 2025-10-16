package com.client;

import java.applet.AppletContext;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import ch.qos.logback.classic.Level;
import com.client.broadcast.Broadcast;
import com.client.broadcast.BroadcastManager;
import com.client.definitions.*;
import com.client.definitions.server.ItemDef;
import com.client.features.settings.Preferences;
import com.client.graphics.interfaces.Configs;
import com.client.graphics.interfaces.daily.DailyRewards;
import com.client.graphics.interfaces.dropdown.StretchedModeMenu;
import com.client.graphics.interfaces.eventcalendar.EventCalendar;
import com.client.graphics.interfaces.impl.Autocast;
import com.client.graphics.interfaces.impl.Bank;
import com.client.graphics.interfaces.impl.DonatorRewards;
import com.client.graphics.interfaces.impl.Interfaces;
import com.client.graphics.interfaces.impl.MonsterDropViewer;
import com.client.graphics.interfaces.impl.Nightmare;
import com.client.graphics.interfaces.impl.QuestTab;

import com.client.features.EntityTarget;
import com.client.features.ExperienceDrop;
import com.client.features.gameframe.ScreenMode;
import com.client.features.gametimers.GameTimer;
import com.client.features.gametimers.GameTimerHandler;
import com.client.features.settings.InformationFile;
import com.client.graphics.interfaces.RSInterface;
import com.client.graphics.interfaces.impl.DropdownMenu;
import com.client.graphics.interfaces.impl.GrandExchange;
import com.client.graphics.interfaces.impl.Keybinding;
import com.client.graphics.interfaces.impl.SpawnContainer;
import com.client.graphics.interfaces.settings.Setting;
import com.client.graphics.interfaces.settings.SettingsInterface;
import com.client.graphics.interfaces.impl.SettingsTabWidget;
import com.client.graphics.interfaces.impl.Slider;
import com.client.graphics.loaders.*;
import com.client.model.Items;
import com.client.model.rt7_anims.AnimKeyFrameSet;
import com.client.plugins.ammo.AmmoPlugin;
import com.client.plugins.gpu.GPUConfig;
import com.client.plugins.gpu.GPUPlugin;
import com.client.plugins.tilemarkers.TileCoordinate;
import com.client.plugins.tilemarkers.TileMarker;
import com.client.plugins.tilemarkers.TileMarkersPlugin;
import com.client.script.ClientScripts;
import com.client.sign.Signlink;
import com.client.sound.SoundPlayer;
import com.client.sound.SoundRequest;
import com.client.sound.SoundRequestQueue;
import com.client.ui.DevConsole;
import com.client.utilities.*;
import com.client.utilities.settings.Settings;
import com.client.utilities.settings.SettingsManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.util.FileUtility;
import dorkbox.notify.Notify;
import dorkbox.notify.Pos;
import dorkbox.util.ActionHandler;
import javafx.geometry.Point3D;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client extends RSApplet {
	private static final int THREAD_COUNT = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
	private static final ExecutorService entityThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
	private static long lastFpsTime = System.nanoTime();
	private static int frameCount = 0;
	private static int currentFPS = 0;
	// Temporary storage arrays (to avoid threading issues with shared state)
	private int[][] tempSpriteData = new int[500][7]; // Store sprite positions per entity
	private String[] tempTextData = new String[500];
	private int tempEntityCount = 0;
	private Sprite loginBoxOverlay;
	private Sprite videoOverlay;
	private VideoBackground loginVideo;
	private long lastFrameTime = 0;
	private static final int FRAME_DELAY = 33; // ~30 FPS (1000ms / 30 = 33ms)
	// Mute Audio Start Screen
	private MusicSettings musicSettings = MusicSettings.getInstance();
	private Sprite muteButtonOff;
	private Sprite muteButtonOn;
	private boolean muteButtonHover;
	private boolean loginMusicMuted = false;
	// Stretched mode fields
	public static boolean ultraPretty = false;
	private boolean stretchedEnabled = false;
	private boolean stretchedKeepAspectRatio = true;
	private boolean stretchedIntegerScaling = false;
	private boolean stretchedFast = false;
	private BufferedImage stretchedCompositeBuffer = null;
	private int scalingFactor = 100;
    public static TileMarkersPlugin tileMarkersPlugin;
    public static ExpTrackerPanel expTrackerPanel;
    private static NPC lastAttackedNPC = null;
    private static long lastNPCAttackTime = 0;
    private static final Queue<Runnable> spriteLoadQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static final Logger logger = LoggerFactory.getLogger("Client");
	private static Map<Integer, Sprite> cachedGameSprites = new ConcurrentHashMap<>();
	public String lastViewedDropTable;
	// Stretched mode methods
	public void invalidateStretching(boolean refresh) {
	    if (refresh) {
	        SwingUtilities.invokeLater(() -> {
	            this.repaint();
	        });
	    }
	}
	public void setStretchedEnabled(boolean enabled) {
	    this.stretchedEnabled = enabled;
	}

	public boolean isStretchedEnabled() {
	    return stretchedEnabled;
	}

	public void setStretchedKeepAspectRatio(boolean keepAspectRatio) {
	    this.stretchedKeepAspectRatio = keepAspectRatio;
	}

	public boolean isStretchedKeepAspectRatio() {
	    return stretchedKeepAspectRatio;
	}

	public void setStretchedIntegerScaling(boolean integerScaling) {
	    this.stretchedIntegerScaling = integerScaling;
	}

	public boolean isStretchedIntegerScaling() {
	    return stretchedIntegerScaling;
	}

	public void setStretchedFast(boolean fast) {
	    this.stretchedFast = fast;
	}

	public boolean isStretchedFast() {
	    return stretchedFast;
	}

	public void setScalingFactor(int factor) {
	    this.scalingFactor = factor;
	}

	public int getScalingFactor() {
	    return scalingFactor;
	}

	public WorldController getScene() {
	    return scene;
	}

	public void setScene(WorldController scene) {
	    this.scene = scene;
	}
	public int getSpriteDrawX() {
	    return spriteDrawX;
	}

	public void setSpriteDrawX(int spriteDrawX) {
	    this.spriteDrawX = spriteDrawX;
	}

	public int getSpriteDrawY() {
	    return spriteDrawY;
	}

	public void setSpriteDrawY(int spriteDrawY) {
	    this.spriteDrawY = spriteDrawY;
	}
	public boolean isServerUpdating() {
		return anInt1104 > 0;
	}

	// Setter for tile markers plugin
	public void setTileMarkersPlugin(TileMarkersPlugin plugin) {
	    this.tileMarkersPlugin = plugin;
	}

	// You might also need a static getter if you're accessing it statically:
	public static TileMarkersPlugin getTileMarkersPluginStatic() {
	    return instance.tileMarkersPlugin;
	}
	public static Player getMyPlayer() {
	    return myPlayer;
	}

	public int getPlane() {
	    return plane;
	}
	private int getPlayerX() {
	    try {
	        return myPlayer.x >> 7; // Convert to tile coordinates
	    } catch (Exception e) {
	        return 3200; // Fallback
	    }
	}

	private int getPlayerY() {
	    try {
	        return myPlayer.y >> 7; // Convert to tile coordinates  
	    } catch (Exception e) {
	        return 3200; // Fallback
	    }
	}

	private int getCurrentPlane() {
	    try {
	        return plane; // Your current plane variable
	    } catch (Exception e) {
	        return 0; // Fallback
	    }
	}
	public DevConsole devConsole = new DevConsole();
	public int scrollbar_position;
	int scrollMax = 0;
	public void draw_scrollbar(int x, int y, int width, int height, int offset, int max_lines, int height_seperator, int y_pad)
	{
		scrollMax = (offset * max_lines + (height_seperator - 1));
		if(scrollMax < height - 3)
			scrollMax = height - 3;

		aClass9_1059.scrollPosition = scrollMax - scrollbar_position - (height - 4);
		if(scrollMax > height)
			method65(x, height, super.getMouseX() - 0, super.getMouseY() - y, aClass9_1059, y_pad, true, scrollMax);

		int pos = scrollMax - (height - 4) - aClass9_1059.scrollPosition;
		if(pos < 0)
			pos = 0;

		if(pos > scrollMax - (height - 4))
			pos = scrollMax - (height - 4);

		if(scrollbar_position != pos)
			scrollbar_position = pos;

		drawScrollbar(height, scrollMax - scrollbar_position - (height - 1), x, y, scrollMax);
	}

	private void drawGrid() {
		for (int index = 0; index < 516; index += 10) {
			if (index < 334) {
				DrawingArea.drawHorizontalLine2(0, index, 516, 0xff0000);
			}
			DrawingArea.drawVerticalLine2(index, 0, 334, 0xff0000);
		}

		int xPos = super.getMouseX() - 4 - ((super.getMouseX() - 4) % 10);
		int yPos = super.getMouseY() - 4 - ((super.getMouseY() - 4) % 10);

		DrawingArea.drawTransparentBox(xPos, yPos, 10, 10, 0xffffff, 255);
		newSmallFont.drawCenteredString("(" + (xPos + 4) + ", " + (yPos + 4) + ")", xPos + 4, yPos - 1, 0xffff00, 0);
	}
	private GPUPlugin gpuPlugin;
	private boolean gpuEnabled = false;
    public CustomGameFrame parentFrame; 
	private boolean screenFlashDrawing;
    static boolean playingJingle;
	private boolean screenFlashOpacityDownward;
	private double screenFlashOpacity = 0d;
	private int screenFlashColor = 0;
	private int screenFlashMaxIntensity = 30;
	private boolean screenFlashAutoFadeOut;
	private Socket aSocket832;
	public boolean fogEnabled;
	public int fogOpacity;
	private boolean wikiHover;
	Sprite wiki1 = new Sprite("/Interfaces/Wiki/2420");
	Sprite wiki2 = new Sprite("/Interfaces/Wiki/2421");

	public void drawScreenBox() {
		if (screenFlashDrawing) {
			double rate = 1_000d / super.getFps() / 20d;
			if (screenFlashOpacityDownward) {
				screenFlashOpacity -= rate;
				if (screenFlashOpacity <= 10) {
					screenFlashOpacityDownward = false;
					screenFlashDrawing = false;
				}
			} else {
				screenFlashOpacity += rate;
				if (screenFlashOpacity >= screenFlashMaxIntensity) {
					screenFlashOpacityDownward = true;
				}
			}
			DrawingArea.drawAlphaGradient(0, 0, currentGameWidth, currentGameHeight, screenFlashColor, screenFlashColor, (int) screenFlashOpacity);
		}

		if (fogEnabled) {
		    // Change from 0xE67D2D (brown) to 0x000000 (black)
		    DrawingArea.drawAlphaBox(0, 0, currentGameWidth, currentGameHeight, 0x000000, fogOpacity);
		}
	}


	public int getDragSetting(int interfaceId) {
		return interfaceId == 3214 ? Preferences.getPreferences().dragTime : 5;
	}

	public void method456() {
		int[] ai = new int[9];
		for (int i8 = 0; i8 < 9; i8++) {
			int k8 = 128 + i8 * 32 + 15;
			int l8 = 600 + k8 * 3;
			int i9 = Rasterizer.Rasterizer3D_sine[k8];
			ai[i8] = (l8 * i9 >> 16);
		}
		WorldController.method310(500, 800, currentScreenMode == ScreenMode.FIXED ? 516 : currentGameWidth,
				currentScreenMode == ScreenMode.FIXED ? 335 : currentGameHeight, ai);
		if (this.loggedIn) {
			this.mainGameGraphicsBuffer = new GraphicsBuffer(worldViewportWidth, worldViewportHeight);
		}
	}

	public static int IDLE_TIME = 30000; // 1 minute = 3000
	public boolean hintMenu;
	public String hintName;
	public int hintId;

	private boolean pollActive;

	private Pattern pattern;

	private Matcher matcher;
	private Sprite[] OsrsSprite;

	private boolean placeHolders = true;

	public static boolean debugModels = false;

	private EntityTarget entityTarget;

	private InformationFile informationFile = new InformationFile();

	public static boolean snowVisible = Configuration.CHRISTMAS ? true : false;

	public static int[][] runePouch = new int[][] { { -1, -1 }, { -1, -1 }, { -1, -1 } };

	public static int itemX = 0, itemY = 0;

	private static boolean shiftDrop = true;

	public static boolean shiftDown;

	private static boolean removeShiftDropOnMenuOpen;

	public static Settings getUserSettings() {
		return userSettings;
	}

	public static void setUserSettings(Settings settings) {
		Client.userSettings = settings;
	}

	private static Settings userSettings;

	/*
	 * @Override public void keyPressed(KeyEvent event) { super.keyPressed(event);
	 * if(loggedIn) { stream.createFrame(186); stream.writeWord(event.getKeyCode());
	 * } }
	 */
	public static void mouseMoved() {
		if (loggedIn)
			// if(idleTime >= (Client.IDLE_TIME - 500))
			stream.createFrame(187);
	}

	/**
	 * The maximum boundary of the screen as specified by the
	 * {@link GraphicsEnvironment} utility class.
	 */
	public static final Rectangle MAXIMUM_SCREEN_BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment()
			.getMaximumWindowBounds();

	int winheight = Toolkit.getDefaultToolkit().getScreenSize().height;

	int taskbarheight = winheight - MAXIMUM_SCREEN_BOUNDS.height;

	public static ScreenMode currentScreenMode = ScreenMode.FIXED;
	public static boolean stretched;

	public static int currentGameWidth = currentScreenMode.equals(ScreenMode.FIXED) ? 765 : 765;
	public static int currentGameHeight = currentScreenMode.equals(ScreenMode.FIXED) ? 503 : 503;

	/**
	 * Handles rune pouch input
	 * @return
	 * @author Sky
	 */
	private void loadPlayerData() throws IOException {
		File file = new File(Signlink.getCacheDirectory() + "/fkeys.dat");
		if (!file.exists()) {
			return;
		}

		DataInputStream stream = new DataInputStream(new FileInputStream(file));

		try {


			for(int i = 0; i < Keybinding.KEYBINDINGS.length; i++) {
				Keybinding.KEYBINDINGS[i] = stream.readByte();
			}

		} catch (IOException e) {
			System.out.println("Unable to load player data.");
			file.delete();
		} finally {
			stream.close();
		}
	}
	public void savePlayerData() {
		try {
			File file = new File(Signlink.getCacheDirectory() + "/fkeys.dat");
			if (!file.exists()) {
				file.createNewFile();
			}
			DataOutputStream stream = new DataOutputStream(new FileOutputStream(file));

			try {
				for(int i = 0; i < 14; i++) {
					stream.writeByte(Keybinding.KEYBINDINGS[i]);
				}
			} finally {
				stream.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void handleScrollPosition(String text, int frame) {
		if (text.startsWith(":scp:")) {
			int scrollPosition = Integer.parseInt(text.split(" ")[1]);
			RSInterface widget = RSInterface.interfaceCache[frame];
			widget.scrollPosition = scrollPosition;
		} else if (text.startsWith(":scpfind:")) {
			RSInterface widget = RSInterface.interfaceCache[frame];
			sendString(7, widget.scrollPosition + "");
		}

		return;
	}
	public static boolean handleRunePouch(String text, int frame) {
		if (!(text.startsWith("#") && text.endsWith("$") && frame == 49999)) {
			return false;
		}
		try {
			// System.out.println("got here");
			text = text.replace("#", "");
			text = text.replace("$", "");
			String[] runes = text.split("-");
			for (int index = 0; index < runes.length; index++) {
				String[] args = runes[index].split(":");
				int id = Integer.parseInt(args[0]);
				int amt = Integer.parseInt(args[1]);
				runePouch[index][0] = id;
				runePouch[index][1] = amt;
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return true;
		}
	}

	private boolean updateGameMode = false;

	public void updateGameMode() {
	    if (updateGameMode) {
	        // GET ACTUAL SCREEN SIZE FIRST
	        int width = currentScreenMode == ScreenMode.FIXED ? 516 : currentGameWidth;
	        int height = currentScreenMode == ScreenMode.FIXED ? 338 : currentGameHeight;
	        
	        // CAP RESOLUTION FOR RESIZABLE MODE (FPS OPTIMIZATION)
	        if (currentScreenMode == ScreenMode.RESIZABLE) {
	            if (width > 1920) {
	                System.out.println("Capping width from " + width + " to 1920 for better FPS");
	                width = 1920;
	            }
	            if (height > 1080) {
	                System.out.println("Capping height from " + height + " to 1080 for better FPS");
	                height = 1080;
	            }
	        }
	        
	        // Set the (possibly capped) viewport size
	        worldViewportWidth = width;
	        worldViewportHeight = height;
	        
	        if (currentScreenMode == ScreenMode.FIXED) {
	            cameraZoom = 600;
	            //WorldController.viewDistance = 9;
	        } else if (currentScreenMode == ScreenMode.RESIZABLE) {
	            cameraZoom = 700;
	            //WorldController.viewDistance = 10;
	        } else if (currentScreenMode == ScreenMode.FULLSCREEN) {
	            cameraZoom = 800;
	            //WorldController.viewDistance = 10;
	        }
	        
	        updateGameScreen();
	        updateGameMode = false;
	    }
	}

	public void setGameMode(ScreenMode mode, boolean serialize) {
	    if (currentScreenMode == mode)
	        return;

	    Dimension size = new Dimension(mode.getWidth(), mode.getHeight());
	    if (mode == ScreenMode.FIXED && getUserSettings().isStretchedMode()) {
	        if (getUserSettings().getStretchedModeDimensions() == null)
	            getUserSettings().setStretchedModeDimensions(ScreenMode.FIXED.getDimensions());
	        size = getUserSettings().getStretchedModeDimensions();
	    }

	    setGameMode(mode, size, serialize);
	}

	public void setGameMode(ScreenMode mode, Dimension size, boolean serialize) {
	    if (currentScreenMode == mode)
	        return;

	    refreshMode(mode, size);
	    if (serialize) {
	        updateScreenPreferences(mode, new Dimension(currentGameWidth, currentGameHeight));
	    }
	}

	public void updateScreenPreferences(ScreenMode mode, Dimension size) {
	    Preferences.getPreferences().mode = mode;
	    Preferences.getPreferences().screenWidth = (int) size.getWidth();
	    Preferences.getPreferences().screenHeight = (int) size.getHeight();
	}

	public void refreshMode(ScreenMode mode, Dimension size) {
	    SwingUtilities.invokeLater(() -> {
	        System.out.println("DEBUG: refreshMode called with mode=" + mode + ", size=" + size);
	        
	        // Handle null appFrame but preserve existing CustomGameFrame
	        if (appFrame == null) {
	            if (ClientWindow.frame instanceof CustomGameFrame) {
	                System.out.println("Using existing CustomGameFrame from ClientWindow.frame");
	                appFrame = ClientWindow.frame;
	                gameContainer = ClientWindow.frame;
	            } else {
	                System.out.println("Creating new CustomGameFrame in refreshMode");
	                CustomGameFrame customFrame = new CustomGameFrame(Configuration.clientTitle != null ? Configuration.clientTitle : "Phoenix Client");
	                ClientWindow.frame = customFrame;
	                appFrame = customFrame;
	                gameContainer = customFrame;
	                
	                if (instance != null) {
	                    customFrame.setGameClient(instance);
	                }
	                customFrame.setVisible(true);
	            }
	        }
	        
	        if (appFrame == null) {
	            System.err.println("ERROR: Still no appFrame after creation attempt");
	            return;
	        }
	        
	        System.out.println("DEBUG: appFrame is " + appFrame.getClass().getSimpleName());
	        
	        Dimension minimum = new Dimension(765, mode == ScreenMode.FIXED ? 503 : 610);
	        int clientWidth = (int) size.getWidth();
	        int clientHeight = (int) size.getHeight();

	        boolean isCustomFrame = appFrame instanceof CustomGameFrame;
	        System.out.println("DEBUG: isCustomFrame = " + isCustomFrame);

	        if (!runelite) {
	            if (isCustomFrame) {
	                CustomGameFrame customFrame = (CustomGameFrame) appFrame;
	                
	                // Add null check for getInset()
	                Insets insets = null;
	                try {
	                    insets = ClientWindow.getInset();
	                    System.out.println("DEBUG: Got insets = " + insets);
	                } catch (Exception e) {
	                    System.err.println("ERROR: Failed to get insets: " + e.getMessage());
	                    insets = new Insets(0, 0, 0, 0); // fallback
	                }
	                
	                if (insets != null) {
	                    clientWidth += insets.left + insets.right;
	                    clientHeight += insets.top + insets.bottom;
	                }
	                
	                // Try the handleGameModeChange with error handling
	                try {
	                    System.out.println("DEBUG: Calling handleGameModeChange");
	                    customFrame.handleGameModeChange(mode, clientWidth, clientHeight);
	                    System.out.println("DEBUG: handleGameModeChange completed");
	                } catch (Exception e) {
	                    System.err.println("ERROR: handleGameModeChange failed: " + e.getMessage());
	                    e.printStackTrace();
	                }
	                
	                if (insets != null) {
	                    clientWidth -= insets.left + insets.right;
	                    clientHeight -= insets.top + insets.bottom;
	                }
	            } else {
	                // Regular frame handling...
	                System.out.println("DEBUG: Handling regular frame");
	                // ... rest of regular frame code
	            }
	        } else {
	            System.out.println("DEBUG: RuneLite mode");
	            // RuneLite handling...
	        }

	        currentScreenMode = mode;
	        currentGameWidth = clientWidth;
	        currentGameHeight = clientHeight;

	        // Set camera zoom and view distance based on mode
	        if (mode == ScreenMode.FIXED) {
	            cameraZoom = 600;
	            WorldController.viewDistance = 9;
	        } else if (mode == ScreenMode.RESIZABLE) {
	            cameraZoom = 700;
	            WorldController.viewDistance = 10;
	        } else if (mode == ScreenMode.FULLSCREEN) {
	            cameraZoom = 800;
	            WorldController.viewDistance = 10;
	        }

	        updateGameMode = true;

	        // Add error handling for frame operations
	        try {
	            if (appFrame != null) {
	                System.out.println("DEBUG: Setting frame properties");
	                appFrame.setLocationRelativeTo(null);
	                appFrame.setResizable(ClientWindow.isResizable());
	                appFrame.setVisible(true);
	                System.out.println("DEBUG: Frame properties set successfully");
	            }
	        } catch (Exception e) {
	            System.err.println("ERROR: Failed to set frame properties: " + e.getMessage());
	            e.printStackTrace();
	        }

	        // Critical: Set the graphics context
	        try {
	            graphics = getGraphics();
	            System.out.println("Graphics object: " + graphics);
	            System.out.println("Game component: " + super.getGameComponent());
	            updateGameScreen();
	        } catch (Exception e) {
	            System.err.println("ERROR: Failed to set graphics context: " + e.getMessage());
	            e.printStackTrace();
	        }
	    });
	}

	/**
	 * Width of the viewport into the game world, which doesn't include tab/chat/minimap interface area on fixed.
	 */
	public static int worldViewportWidth = 516;

	/**
	 * Height of the viewport into the game world, which doesn't include tab/chat/minimap interface area on fixed.
	 */
	public int worldViewportHeight = 338;

	public void updateGameScreen() {
		if (getUserSettings().isAntiAliasing()) {
			antialiasingPixels = new int[Client.worldViewportWidth * worldViewportHeight << 2];
		}
		Rasterizer.method365(Client.worldViewportWidth << 1, worldViewportHeight << 1);
		antialiasingOffsets = Rasterizer.anIntArray1472;
		Rasterizer.method365(worldViewportHeight, worldViewportHeight);
		Rasterizer.method365(worldViewportWidth, worldViewportHeight);
		this.fullScreenTextureArray = Rasterizer.anIntArray1472;
		Rasterizer.method365(
				currentScreenMode == ScreenMode.FIXED ? 516
						: worldViewportWidth,
				currentScreenMode == ScreenMode.FIXED ? 168
						: worldViewportHeight);
		anIntArray1180 = Rasterizer.anIntArray1472;
		Rasterizer.method365(
				currentScreenMode == ScreenMode.FIXED ? 249
						: worldViewportWidth,
				currentScreenMode == ScreenMode.FIXED ? 335
						: worldViewportHeight);
		this.anIntArray1181 = Rasterizer.anIntArray1472;
		Rasterizer.method365(worldViewportWidth, worldViewportHeight);
		this.anIntArray1182 = Rasterizer.anIntArray1472;
		method456();
	}
	private long experienceCounter;
	private Sprite mapBack;
	private Sprite[] smallXpSprites = new Sprite[22];
	public static Sprite[] cacheSprite474;
	private static final long serialVersionUID = 1L;
	private Sprite[] inputSprites = new Sprite[7];

	private int modifiableXValue = 1; // u dont care if it starts at 1? Can't see a real problem with it :P kk
	private int achievementCutoff = 100;
	private Sprite[] minimapIcons = new Sprite[2];
	private String macAddress;

	public static void dumpModels() {
		int id = 6610;
		NpcDefinition npc = NpcDefinition.forID(id);
		System.out.println(npc.name);
		if (npc.models != null) {
			for (int modelid = 0; modelid < npc.models.length; modelid++) {
				System.out.println(npc.models[modelid]);
			}
		}
	}

	private static String intToKOrMilLongName(int i) {
		String s = String.valueOf(i);
		for (int k = s.length() - 3; k > 0; k -= 3)
			s = s.substring(0, k) + "," + s.substring(k);
		if (s.length() > 8)
			s = "@gre@" + s.substring(0, s.length() - 8) + " million @whi@(" + s + ")";
		else if (s.length() > 4)
			s = "@cya@" + s.substring(0, s.length() - 4) + "K @whi@(" + s + ")";
		return " " + s;
	}

	private static String intToKOrMil(int j) {
		if (j < 0x186a0)
			return String.valueOf(j);
		if (j < 0x989680)
			return j / 1000 + "K";
		else
			return j / 0xf4240 + "M";
	}


	public static int spellID = 0;

	public static void setTab(int id) {
		needDrawTabArea = true;
		tabID = id;
		tabAreaAltered = true;
	}

	public void setSidebarInterface(int sidebarID, int interfaceID) {
		tabInterfaceIDs[sidebarID] = interfaceID;
		tabID = sidebarID;
		needDrawTabArea = true;
		tabAreaAltered = true;
	}

	private boolean menuHasAddFriend(int j) {
		if (j < 0)
			return false;
		int k = menuActionID[j];
		if (k >= 2000)
			k -= 2000;
		return k == 337;
	}

	private Sprite channelButtons;

	private void drawChannelButtons() {
		String text[] = { "On", "Friends", "Off", "Hide" };
		int textColor[] = { 65280, 0xffff00, 0xff0000, 65535 };
		int yOffset = currentScreenMode == ScreenMode.FIXED ? 0 : currentGameHeight - 165;

		if (hideChatArea) {
			channelButtons.drawSprite(0, currentGameHeight - 23);
		}

		switch (channelButtonClickPosition) {
			case 0:
				chatButtons[1].drawSprite(5, 142 + yOffset);
				break;
			case 1:
				chatButtons[1].drawSprite(71, 142 + yOffset);
				break;
			case 2:
				chatButtons[1].drawSprite(137, 142 + yOffset);
				break;
			case 3:
				chatButtons[1].drawSprite(203, 142 + yOffset);
				break;
			case 4:
				chatButtons[1].drawSprite(269, 142 + yOffset);
				break;
			case 5:
				chatButtons[1].drawSprite(335, 142 + yOffset);
				break;
		}
		if (channelButtonHoverPosition == channelButtonClickPosition) {
			switch (channelButtonHoverPosition) {
				case 0:
					chatButtons[2].drawSprite(5, 142 + yOffset);
					break;
				case 1:
					chatButtons[2].drawSprite(71, 142 + yOffset);
					break;
				case 2:
					chatButtons[2].drawSprite(137, 142 + yOffset);
					break;
				case 3:
					chatButtons[2].drawSprite(203, 142 + yOffset);
					break;
				case 4:
					chatButtons[2].drawSprite(269, 142 + yOffset);
					break;
				case 5:
					chatButtons[2].drawSprite(335, 142 + yOffset);
					break;
				case 6:
					chatButtons[3].drawSprite(404, 142 + yOffset);
					break;
			}
		} else {
			switch (channelButtonHoverPosition) {
				case 0:
					chatButtons[0].drawSprite(5, 142 + yOffset);
					break;
				case 1:
					chatButtons[0].drawSprite(71, 142 + yOffset);
					break;
				case 2:
					chatButtons[0].drawSprite(137, 142 + yOffset);
					break;
				case 3:
					chatButtons[0].drawSprite(203, 142 + yOffset);
					break;
				case 4:
					chatButtons[0].drawSprite(269, 142 + yOffset);
					break;
				case 5:
					chatButtons[0].drawSprite(335, 142 + yOffset);
					break;
				case 6:
					chatButtons[3].drawSprite(404, 142 + yOffset);
					break;
			}
		}
		/**
		 * Simple format.
		 */
		SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm a");
		/**
		 * Sets timezone to USA
		 */
		dateFormat.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		/**
		 * Updates button per tick.
		 */
		newSmallFont.drawCenteredString(""+dateFormat.format(new Date().getTime()), 459, 157 + yOffset, 0xffffff, 0);


		smallText.method389(true, 26, 0xffffff, "All", 157 + yOffset);
		smallText.method389(true, 86, 0xffffff, "Game", 152 + yOffset);
		smallText.method389(true, 150, 0xffffff, "Public", 152 + yOffset);
		smallText.method389(true, 212, 0xffffff, "Private", 152 + yOffset);
		smallText.method389(true, 286, 0xffffff, "Clan", 152 + yOffset);
		smallText.method389(true, 349, 0xffffff, "Trade", 152 + yOffset);
		smallText.method382(textColor[gameMode], 98, text[gameMode], 163 + yOffset, true);
		smallText.method382(textColor[publicChatMode], 164, text[publicChatMode], 163 + yOffset, true);
		smallText.method382(textColor[privateChatMode], 230, text[privateChatMode], 163 + yOffset, true);
		smallText.method382(textColor[clanChatMode], 296, text[clanChatMode], 163 + yOffset, true);
		smallText.method382(textColor[tradeMode], 362, text[tradeMode], 163 + yOffset, true);
	}

	ItemSearch grandExchangeItemSearch = null;

	public static int grandExchangeSearchScrollPostion;

	private static void handleGEItemSearchClick(int itemId) {
		// TODO Send Item Packet
		System.out.println("Clicking Item: " + itemId);
	}

	private void drawChatArea() {
		if (currentScreenMode == ScreenMode.FIXED)
			hideChatArea = false;

		if (currentScreenMode == ScreenMode.FIXED && loginScreenGraphicsBuffer == null)
			chatAreaGraphicsBuffer.initDrawingArea();

		final int yOffset = currentScreenMode == ScreenMode.FIXED ? 0 : currentGameHeight - 165;
		final int yOffset2 = currentScreenMode == ScreenMode.FIXED ? 338 : 0;

		Rasterizer.anIntArray1472 = anIntArray1180;

		if (hideChatArea)
			drawChannelButtons();

		if (!hideChatArea) {
			chatArea.drawSprite(0, yOffset);
			drawChannelButtons();

			TextDrawingArea textDrawingArea = aTextDrawingArea_1271;
			if (super.getSaveClickX() >= 0 && super.getSaveClickX() <= 518) {
				if (super.getSaveClickY() >= (currentScreenMode == ScreenMode.FIXED ? 335 : currentGameHeight - 164)
						&& super.getSaveClickY() <= (currentScreenMode == ScreenMode.FIXED ? 484 : currentGameHeight - 30)) {
					if (this.isFieldInFocus()) {
						Client.inputString = "";
						this.resetInputFieldFocus();
					}
				}
			}
			if (messagePromptRaised) {
				newBoldFont.drawCenteredString(aString1121, 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(promptInput + "*", 259, 80 + yOffset, 128, -1);
			} else if (inputDialogState == 1) {
				newBoldFont.drawCenteredString(enterInputInChatString, 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128, -1);
			} else if (inputDialogState == 2) {
				newBoldFont.drawCenteredString(enterInputInChatString, 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128, -1);
			} else if (inputDialogState == 7) {
				newBoldFont.drawCenteredString("Enter the price for the item:", 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128, -1);
			} else if (inputDialogState == 8) {
				newBoldFont.drawCenteredString("Amount you want to sell:", 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128, -1);
			} else if (Bank.isSearchingBank()) {
				newBoldFont.drawCenteredString("Enter an item to search for:", 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(Bank.searchingBankString + "*", 259, 80 + yOffset, 128, -1);
			} else if (inputDialogState == 10) {
				newBoldFont.drawCenteredString("Enter an players name to join:", 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString(amountOrNameInput + "*", 259, 80 + yOffset, 128, -1);
			} else if (inputDialogState == 3) {
				DrawingArea.fillPixels(8, 505, 108, 0x463214, 28 + yOffset);
				DrawingArea.drawAlphaBox(8, 28 + yOffset, 505, 108, 0x746346, 75);

				newBoldFont.drawCenteredString("@bla@What would you like to buy? @blu@" + amountOrNameInput + "*", 259,
						20 + yOffset, 128, -1);
				if (amountOrNameInput != "") {
					grandExchangeItemSearch = new ItemSearch(amountOrNameInput, 100, true);

					final int xPosition = 15;
					final int yPosition = 32 + yOffset - grandExchangeSearchScrollPostion;
					int rowCountX = 0;
					int rowCountY = 0;

					int itemAmount = grandExchangeItemSearch.getItemSearchResultAmount();

					if (amountOrNameInput.length() == 0) {
						newRegularFont.drawCenteredString("Start typing the name of an item to search for it.", 259,
								70 + yOffset, 0, -1);
					} else if (itemAmount == 0) {
						newRegularFont.drawCenteredString("No matching items found!", 259, 70 + yOffset, 0, -1);
					} else {
						DrawingArea.setDrawingArea(134 + yOffset, 8, 497, 29 + yOffset);
						for (int itemId = 0; itemId < itemAmount; itemId++) {
							int itemResults[] = grandExchangeItemSearch.getItemSearchResults();

							if (itemResults[itemId] != -1) {
								final int startX = xPosition + rowCountX * 160;
								final int startY = yPosition + rowCountY * 35;
								Sprite itemSprite = ItemDefinition.getSprite(itemResults[itemId], 1, 0, 0, false);
								if (itemSprite != null)
									itemSprite.drawSprite(startX, startY);

								ItemDefinition itemDef = ItemDefinition.forID(itemResults[itemId]);
								newRegularFont.drawBasicString(itemDef.name, startX + 40, startY + 14, 0, -1);

								if (super.getMouseX() >= startX && super.getMouseX() <= startX + 160) {
									if (super.getMouseY() >= (startY + yOffset2)
											&& super.getMouseY() <= (startY + yOffset2) + 35) {
										DrawingArea.drawAlphaBox(startX, startY, 160, 35, 0xFFFFFF, 120);

										if (super.clickMode3 == 1)
											handleGEItemSearchClick(itemDef.id);
									}
								}
							}

							rowCountX++;

							if (rowCountX > 2) {
								rowCountY++;
								rowCountX = 0;
							}
						}
						DrawingArea.defaultDrawingAreaSize();
					}

					int maxScrollPosition = itemAmount / 3 * 35;
					if (itemAmount > 9)
						drawScrollbar(106, grandExchangeSearchScrollPostion > maxScrollPosition ? 0
								: grandExchangeSearchScrollPostion, 29 + yOffset, 496, itemAmount / 3 * 35);

				}
			} else if (aString844 != null) {
				newBoldFont.drawCenteredString(aString844, 259, 60 + yOffset, 0, -1);
				newBoldFont.drawCenteredString("Click to continue", 259, 80 + yOffset, 128, -1);
			} else if (backDialogID != -1) {
				drawInterface(0, 20, RSInterface.interfaceCache[backDialogID], 20 + yOffset);
			} else if (dialogID != -1) {
				drawInterface(0, 20, RSInterface.interfaceCache[dialogID], 20 + yOffset);
			} else {
				int j77 = -3;
				int j = 0;
				DrawingArea.setDrawingArea(122 + yOffset, 8, 497, 7 + yOffset);
				for (int k = 0; k < 500; k++)
					if (chatMessages[k] != null) {
						// System.out.println(chatMessages[k]);
						int chatType = chatTypes[k];
						int yPos = (70 - j77 * 14) + anInt1089 + 5;
						String s1 = chatNames[k];
						//byte byte0 = 0;

						List<Integer> crowns = new ArrayList<>();

						while (s1.startsWith("@cr")) {
							String s2 = s1.substring(3, s1.length());
							int index = s2.indexOf("@");
							if (index != -1) {
								s2 = s2.substring(0, index);
								crowns.add(Integer.parseInt(s2));
								s1 = s1.substring(4 + s2.length());
							}
						}
						if (chatType == 0) {
							if (chatTypeView == 5 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210) {
									newRegularFont.drawBasicString(chatMessages[k], 10, yPos - 1 + yOffset, 0, -1);
								}
								j++;
								j77++;
							}
						}
						if ((chatType == 1 || chatType == 2) && (chatType == 1 || publicChatMode == 0
								|| publicChatMode == 1 && isFriendOrSelf(s1))) {
							if (chatTypeView == 1 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210) {
									int xPos = 11;
									if (!crowns.isEmpty()) {
										for (int crown : crowns) {
											for (int right = 0; right < modIcons.length; right++) {
												if (right == (crown - 1) && modIcons[right] != null) {
													modIcons[right].drawAdvancedSprite(xPos - 1,
															yPos + yOffset - modIcons[right].myHeight);
													xPos += modIcons[right].myWidth;
													xPos += 2;
													break;
												}
											}
										}
									}
									newRegularFont.drawBasicString(s1 + ":", xPos - 1, yPos - 1 + yOffset, 0, -1);
									xPos += newRegularFont.getTextWidth(s1) + 5;
									newRegularFont.drawBasicString(chatMessages[k], xPos - 1, yPos - 1 + yOffset, 255,-1, false);
								}
								j++;
								j77++;
							}
						}

						if (chatType == 99) {
							if (yPos > 0 && yPos < 210) {
								lastViewedDropTable = chatMessages[k];
								newRegularFont.drawBasicString(s1 + " " + chatMessages[k], 10, yPos - 1 + yOffset, 0x7e3200, -1, false);
							}
							j++;
							j77++;
						}


						if ((chatType == 3 || chatType == 7) && (!splitPrivateChat || chatTypeView == 2)
								&& (chatType == 7 || privateChatMode == 0
								|| privateChatMode == 1 && isFriendOrSelf(s1))) {
							if (chatTypeView == 2 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210) {
									int k1 = 11;
									newRegularFont.drawBasicString("From", k1, yPos - 1 + yOffset, 0, -1);
									k1 += textDrawingArea.getTextWidth("From ");
									if (!crowns.isEmpty()) {
										for (int crown : crowns) {
											for (int right = 0; right < modIcons.length; right++) {
												if (right == (crown - 1) && modIcons[right] != null) {
													modIcons[right].drawAdvancedSprite(k1,
															yPos + yOffset - modIcons[right].myHeight);
													k1 += modIcons[right].myWidth;
													break;
												}
											}
										}
									}
									newRegularFont.drawBasicString(s1 + ":", k1, yPos - 1 + yOffset, 0, -1);
									k1 += newRegularFont.getTextWidth(s1) + 8;
									newRegularFont.drawBasicString(chatMessages[k], k1, yPos - 1 + yOffset, 0x800000,-1, false);
								}
								j++;
								j77++;
							}
						}
						if (chatType == 4 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s1))) {
							if (chatTypeView == 3 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210)
									newRegularFont.drawBasicString(s1 + " " + chatMessages[k], 11, yPos - 1 + yOffset,0x800080, -1, false);
								j++;
								j77++;
							}
						}
						if (chatType == 31 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s1))) {
							int crownId = 29;
							if (chatTypeView == 3 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210) {
									int xPos = 11;
									modIcons[crownId].drawAdvancedSprite(xPos - 1, yPos + yOffset - modIcons[crownId].myHeight);
									xPos += modIcons[crownId].myWidth;
									newRegularFont.drawBasicString(s1 + " " + chatMessages[k], xPos - 1, yPos - 1 + yOffset, 0x1950ce, -1, false);
								}
								j++;
								j77++;
							}
						}
						if (chatType == 5 && !splitPrivateChat && privateChatMode < 2) {
							if (chatTypeView == 2 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210)
									newRegularFont.drawBasicString(chatMessages[k], 10, yPos - 1 + yOffset, 0x800000,-1, false);
								j++;
								j77++;
							}
						}
						if (chatType == 6 && (!splitPrivateChat || chatTypeView == 2) && privateChatMode < 2) {
							if (chatTypeView == 2 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210) {
									int k1 = 15;
									k1 += textDrawingArea.getTextWidth("To " + s1);
									newRegularFont.drawBasicString("To " + s1 + ":", 10, yPos - 1 + yOffset, 0, -1);
									// newRegularFont.drawBasicString(chatMessages[k], 15 +
									// newRegularFont.getTextWidth("To :" + s1), yPos, 0x800000, -1);
									newRegularFont.drawBasicString(chatMessages[k], k1, yPos - 1 + yOffset, 0x800000,-1, false);
								}
								j++;
								j77++;
							}
						}

						// Clan chat
						if (chatType == 11 && (clanChatMode == 0)) {
							if (chatTypeView == 11 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210) {
									try {
										String lastChatNameEnd = "@dre@";
										String clanChatMessage = chatMessages[k];
										String chatMessage = clanChatMessage.substring(clanChatMessage.indexOf(lastChatNameEnd) + lastChatNameEnd.length());
										String usernameAndFormatting = clanChatMessage.substring(0, clanChatMessage.indexOf(lastChatNameEnd) + lastChatNameEnd.length());
										newRegularFont.drawBasicString(usernameAndFormatting, 8, yPos - 1 + yOffset, 0x7e3200, -1, true);
										newRegularFont.drawBasicString(chatMessage, newRegularFont.getTextWidth(usernameAndFormatting) + 8, yPos - 1 + yOffset, 0x7e3200, -1, false);
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								j++;
								j77++;
							}
						}
						if (chatType == 13 && chatTypeView == 12) {
							if (yPos > 0 && yPos < 210)
								newRegularFont.drawBasicString(s1 + " " + chatMessages[k], 8, yPos - 1 + yOffset,0x7e3200, -1, false);
							j++;
							j77++;
						}
						if (chatType == 8 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s1))) {
							if (chatTypeView == 3 || chatTypeView == 0) {
								if (yPos > 0 && yPos < 210)
									newRegularFont.drawBasicString(s1 + " " + chatMessages[k], 10, yPos - 1 + yOffset,0x7e3200, -1, false);
								j++;
								j77++;
							}
						}
						if (chatType == 16) {
							int j2 = 40 + 11;
							String clanname = clanList[k];
							int clanNameWidth = textDrawingArea.getTextWidth(clanname);
							if (chatTypeView == 11 || chatTypeView == 0) {
								newRegularFont.drawBasicString("[", 19, yPos - 1 + yOffset, 0, -1);
								newRegularFont.drawBasicString("]", clanNameWidth + 16 + 10, yPos - 1 + yOffset, 0, -1);
								newRegularFont.drawBasicString("" + capitalize(clanname) + "", 25, yPos - 1 + yOffset,
										255, -1);
								newRegularFont.drawBasicString(chatNames[k] + ":", j2 - 17, yPos);
								j2 += newRegularFont.getTextWidth(chatNames[k]) + 7;
								newRegularFont.drawBasicString(capitalize(chatMessages[k]), j2 - 16, yPos - 1 + yOffset,0x800000, -1);

								j++;
								j77++;
							}
						}
					}
				DrawingArea.defaultDrawingAreaSize();
				chatAreaScrollLength = j * 14 + 7 + 5;
				if (chatAreaScrollLength < 111)
					chatAreaScrollLength = 111;
				drawScrollbar(114, chatAreaScrollLength - anInt1089 - 113, 6 + yOffset, 496, chatAreaScrollLength);
				String fixedString;
				if (myPlayer != null && myPlayer.displayName != null) {
					if (myPlayer.title.length() > 0) {
						fixedString = "<col=" + myPlayer.titleColor + ">" + myPlayer.title + "</col>" + " "
								+ myPlayer.displayName;
					} else {
						fixedString = myPlayer.displayName;
					}
				} else {
					fixedString = StringUtils.fixName(capitalize(myUsername));
				}
				String s;
				if (myPlayer != null && myPlayer.displayName != null && myPlayer.title.length() > 1)
					s = myPlayer.title + " " + myPlayer.displayName;
				else
					s = StringUtils.fixName(capitalize(myUsername));

				int xOffset = 0;
				if (myPlayer.hasRightsOtherThan(PlayerRights.PLAYER)) {
					for (PlayerRights right : myPlayer.getDisplayedRights()) {
						if (right.hasCrown()) {
							modIcons[right.spriteId()].drawSprite(9 + xOffset, 134 + yOffset - modIcons[right.spriteId()].myHeight);
							xOffset += modIcons[right.spriteId()].myWidth;
							xOffset += 2;
						}
					}

					newRegularFont.drawBasicString(fixedString, 10 + xOffset, 133 + yOffset, 0, -1);
				} else {
					newRegularFont.drawBasicString(fixedString, 10 + xOffset, 133 + yOffset, 0, -1);
				}

				xOffset += newRegularFont.getTextWidth(fixedString) + 10;
				newRegularFont.drawBasicString(": ", xOffset, 133 + yOffset, 0, -1);
				xOffset += textDrawingArea.getTextWidth(": ");

				if (!isFieldInFocus()) {
					newRegularFont.drawBasicString(inputString + "*",xOffset, 133 + yOffset, 255, -1, false);
				}

				DrawingArea.method339(120 + yOffset, 0x807660, 506, 7);
			}

		}
		if (menuOpen) {
			drawMenu(0, currentScreenMode == ScreenMode.FIXED ? 338 : 0);
		}
		if (currentScreenMode == ScreenMode.FIXED)
			chatAreaGraphicsBuffer.drawGraphics(0, 338, super.graphics);

		mainGameGraphicsBuffer.setCanvas();
		Rasterizer.anIntArray1472 = anIntArray1182;
	}

	private String clanUsername;
	private String clanMessage;
	private String clanTitle;
	private String[] clanTitles;
	private EnumSet channelRights;

	@Override
	public void startRunnable(Runnable runnable, int i) {
		if (i > 10)
			i = 10;
		if (Signlink.mainapp != null) {
			Signlink.startthread(runnable, i);
		} else {
			super.startRunnable(runnable, i);
		}
	}

	public Socket openSocket(int port) throws IOException {
		return new Socket(InetAddress.getByName(server), port);
	}

	public boolean processMenuClick() {
		if (activeInterfaceType != 0)
			return false;
		int j = super.clickMode3;
		if (spellSelected == 1 && super.getSaveClickX() >= 516 && super.getSaveClickY() >= 160 && super.getSaveClickX() <= 765
				&& super.getSaveClickY() <= 205)
			j = 0;
		if (menuOpen) {
			if (j != 1) {
				int k = super.getMouseX();
				int j1 = super.getMouseY();
				if (menuScreenArea == 0) {
					k -= 0;
					j1 -= 0;
				}
				if (menuScreenArea == 1) {
					k -= 516;
					j1 -= 160;
				}
				if (menuScreenArea == 2) {
					k -= 17;
					j1 -= 343;
				}
				if (menuScreenArea == 3) {
					k -= 516;
					j1 -= 0;
				}
				if (k < menuOffsetX - 10 || k > menuOffsetX + menuWidth + 10 || j1 < menuOffsetY - 10
						|| j1 > menuOffsetY + menuHeight + 10) {
					menuOpen = false;
					if (menuScreenArea == 1)
						needDrawTabArea = true;
					if (menuScreenArea == 2)
						inputTaken = true;
				}
			}
			if (j == 1) {
				int l = menuOffsetX;
				int k1 = menuOffsetY;
				int i2 = menuWidth;
				int k2 = super.getSaveClickX();
				int l2 = super.getSaveClickY();
				if (menuScreenArea == 0) {
					k2 -= 0;
					l2 -= 0;
				}
				if (menuScreenArea == 1) {
					k2 -= 516;
					l2 -= 160;
				}
				if (menuScreenArea == 2) {
					k2 -= 17;
					l2 -= 343;
				}
				if (menuScreenArea == 3) {
					k2 -= 516;
					l2 -= 0;
				}
				int i3 = -1;
				for (int j3 = 0; j3 < menuActionRow; j3++) {
					int k3 = k1 + 31 + (menuActionRow - 1 - j3) * 15;
					if (k2 > l && k2 < l + i2 && l2 > k3 - 13 && l2 < k3 + 3)
						i3 = j3;
				}
				if (i3 != -1)
					doAction(i3);
				menuOpen = false;
				if (menuScreenArea == 1)
					needDrawTabArea = true;
				if (menuScreenArea == 2) {
					inputTaken = true;
				}
			}
			return true;
		} else {
			if (j == 1 && menuActionRow > 0) {
				int i1 = menuActionID[menuActionRow - 1];
				if (i1 == 632 || i1 == 78 || i1 == 867 || i1 == 431 || i1 == 53 || i1 == 74 || i1 == 454 || i1 == 539
						|| i1 == 493 || i1 == 847 || i1 == 447 || i1 == 1125) {
					int l1 = menuActionCmd2[menuActionRow - 1];
					int j2 = menuActionCmd3[menuActionRow - 1];
					RSInterface class9 = RSInterface.interfaceCache[j2];
					if (class9.aBoolean259 || class9.aBoolean235) {
						aBoolean1242 = false;
						anInt989 = 0;
						draggingItemInterfaceId = j2;
						itemDraggingSlot = l1;
						activeInterfaceType = 2;
						anInt1087 = super.getSaveClickX();
						anInt1088 = super.getSaveClickY();
						if (RSInterface.interfaceCache[j2].parentID == openInterfaceID)
							activeInterfaceType = 1;
						if (RSInterface.interfaceCache[j2].parentID == backDialogID)
							activeInterfaceType = 3;
						return true;
					}
				}
			}
			if (j == 1 && (anInt1253 == 1 || menuHasAddFriend(menuActionRow - 1)) && menuActionRow > 2)
				j = 2;
			if (j == 1 && menuActionRow > 0)
				doAction(menuActionRow - 1);
			if (j == 2 && menuActionRow > 0)
				determineMenuSize();
			minimapHovers();
			return false;
		}
	}

	private final void minimapHovers() {
		final boolean fixed = currentScreenMode == ScreenMode.FIXED;
		prayHover = fixed
				? prayHover = super.getMouseX() >= 517 && super.getMouseX() <= 573
				&& super.getMouseY() >= (Configuration.osbuddyGameframe ? 81 : 76)
				&& super.getMouseY() < (Configuration.osbuddyGameframe ? 109 : 107)
				: super.getMouseX() >= currentGameWidth - 210 && super.getMouseX() <= currentGameWidth - 157
				&& super.getMouseY() >= (Configuration.osbuddyGameframe ? 90 : 55)
				&& super.getMouseY() < (Configuration.osbuddyGameframe ? 119 : 104);
		runHover = fixed
				? runHover = super.getMouseX() >= (Configuration.osbuddyGameframe ? 530 : 522)
				&& super.getMouseX() <= (Configuration.osbuddyGameframe ? 582 : 594)
				&& super.getMouseY() >= (Configuration.osbuddyGameframe ? 114 : 109)
				&& super.getMouseY() < (Configuration.osbuddyGameframe ? 142 : 142)
				: super.getMouseX() >= currentGameWidth - (Configuration.osbuddyGameframe ? 196 : 186)
				&& super.getMouseX() <= currentGameWidth - (Configuration.osbuddyGameframe ? 142 : 132)
				&& super.getMouseY() >= (Configuration.osbuddyGameframe ? 123 : 132)
				&& super.getMouseY() < (Configuration.osbuddyGameframe ? 150 : 159);
		counterHover = fixed ? super.getMouseX() >= 522 && super.getMouseX() <= 544 && super.getMouseY() >= 20 && super.getMouseY() <= 47
				: super.getMouseX() >= currentGameWidth - 205 && super.getMouseX() <= currentGameWidth - 184 && super.getMouseY() >= 27
				&& super.getMouseY() <= 44;
		worldHover = fixed ? super.getMouseX() >= 714 && super.getMouseX() <= 740 && super.getMouseY() >= 121 && super.getMouseY() <= 146
				: super.getMouseX() >= currentGameWidth - 34 && super.getMouseX() <= currentGameWidth - 5 && super.getMouseY() >= 143
				&& super.getMouseY() <= 172;
		wikiHover = fixed ? super.getMouseX() >= 706 && super.getMouseX() <= 742 && super.getMouseY() >= 160 && super.getMouseY() <= 147
				: super.getMouseX() >= currentGameWidth - 34 && super.getMouseX() <= currentGameWidth - 5 && super.getMouseY() >= 143
				&& super.getMouseY() <= 172;
	}

	private boolean prayHover, prayClicked;

	public static boolean counterOn;

	private boolean counterHover;

	private boolean worldHover;

	private boolean showClanOptions;

	public static int totalRead = 0;

	public static String getFileNameWithoutExtension(String fileName) {
		File tmpFile = new File(fileName);
		tmpFile.getName();
		int whereDot = tmpFile.getName().lastIndexOf('.');
		if (0 < whereDot && whereDot <= tmpFile.getName().length() - 2) {
			return tmpFile.getName().substring(0, whereDot);
		}
		return "";
	}

	public String indexLocation(int cacheIndex, int index) {
		return "E:\\dump\\index" + cacheIndex + "/" + (index != -1 ? index + ".gz" : "");
	}

	public void repackCacheAll() {
		for (int index = 0; index < 10; index++) {
			try {
				repackCacheIndex(index);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//packCustomMaps();
		//packCustomModels();
		//packCustomAnimations();
	}

	public byte[] fileToByteArray(File file) {
		try {
			byte[] fileData = new byte[(int) file.length()];
			FileInputStream fis = new FileInputStream(file);
			fis.read(fileData);
			fis.close();
			return fileData;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public void repackCacheIndex(int cacheIndex) {
		if (!new File(indexLocation(cacheIndex, -1)).exists()) {
			System.out.println("No index data found for: " + cacheIndex);
			return;
		}

		System.out.println("Started repacking index " + cacheIndex + ".");
		int indexLength = new File(indexLocation(cacheIndex, -1)).listFiles().length;
		File[] file = new File(indexLocation(cacheIndex, -1)).listFiles();
		int packed = 0;
		try {
			for (int index = 0; index < indexLength; index++) {
				if (file[index] != null) {
					if (pack(file[index], cacheIndex)) {
						packed++;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error packing index " + cacheIndex + ".");
		}

		System.out.println("Packed " + (packed + "/" + indexLength) + " files to index " + cacheIndex + ".");
	}

	public boolean pack(File file, int cacheIndex) {
		int fileIndex = Integer.parseInt(getFileNameWithoutExtension(file.toString()));
		byte[] data = fileToByteArray(file);
		if (data != null && data.length > 0) {
			decompressors[cacheIndex].write(data.length, data, fileIndex);
			System.out.println("Packed " + file.toString() + " to index " + cacheIndex);
			return true;
		} else {
			System.out.println("No file to pack: " + file);
			return false;
		}
	}

	public void packCustomMaps() {
		if (new File(Configuration.CUSTOM_MAP_DIRECTORY).exists()) {
			System.out.println("Repacking custom maps..");
			packToIndex(4, new File(Configuration.CUSTOM_MAP_DIRECTORY));
		}
	}

	public void packCustomModels() {
		if (new File(Configuration.CUSTOM_MODEL_DIRECTORY).exists()) {
			System.out.println("Repacking custom models..");
			packToIndex(1, new File(Configuration.CUSTOM_MODEL_DIRECTORY));
		}
	}

	public void packCustomAnimations() {
		if (new File(Configuration.CUSTOM_ANIMATION_DIRECTORY).exists()) {
			System.out.println("Repacking custom animations..");
			packToIndex(2, new File(Configuration.CUSTOM_ANIMATION_DIRECTORY));
		}
	}

	private void packToIndex(int index, File file) {
		if (file.isDirectory()) {
			File[] list = file.listFiles();
			if (list != null) {
				Arrays.stream(list).forEach(f -> packToIndex(index, f));
			}
		} else {
			pack(file, index);
			System.out.println("Pack " + file.getName() + " to index " + index);
		}
	}

	public void preloadModels() {
		File models = new File(Signlink.getCacheDirectory() + "/Raw/");
		File[] modelList = models.listFiles();
		for (int model = 0; model < modelList.length; model++) {
			String modelID = modelList[model].getName();
			byte[] modelData = ReadFile(Signlink.getCacheDirectory() + "/Raw/" + modelID);
			Model.loadModel(modelData, Integer.parseInt(getFileNameWithoutExtension(modelID)));
		}
	}

	public static final byte[] ReadFile(String s) {
		try {
			byte abyte0[];
			File file = new File(s);
			int i = (int) file.length();
			abyte0 = new byte[i];
			DataInputStream datainputstream = new DataInputStream(new BufferedInputStream(new FileInputStream(s)));
			datainputstream.readFully(abyte0, 0, i);
			datainputstream.close();
			totalRead++;
			return abyte0;
		} catch (Exception e) {
			System.out.println((new StringBuilder()).append("Read Error: ").append(s).toString());
			return null;
		}
	}

	public void addModels() {
		for (int ModelIndex = 0; ModelIndex < 50000; ModelIndex++) {
			byte[] abyte0 = getModel(ModelIndex);
			if (abyte0 != null && abyte0.length > 0) {
				decompressors[1].write(abyte0.length, abyte0, ModelIndex);
			}
		}
	}

	public byte[] getModel(int Index) {
		try {
			File Model = new File(Signlink.getCacheDirectory() + "./pModels/" + Index + ".gz");
			byte[] aByte = new byte[(int) Model.length()];
			FileInputStream fis = new FileInputStream(Model);
			fis.read(aByte);
			System.out.println("" + Index + " aByte = [" + aByte + "]!");
			fis.close();
			return aByte;
		} catch (Exception e) {
			return null;
		}
	}

	public void addMaps() {
		for (int MapIndex = 0; MapIndex < 3536; MapIndex++) {
			byte[] abyte0 = getMaps(MapIndex);
			if (abyte0 != null && abyte0.length > 0) {
				decompressors[4].write(abyte0.length, abyte0, MapIndex);
				System.out.println("Maps Added");
			}
		}
	}

	public byte[] getMaps(int Index) {
		try {
			File Map = new File(Signlink.getCacheDirectory() + "./maps/" + Index + ".gz");
			byte[] aByte = new byte[(int) Map.length()];
			FileInputStream fis = new FileInputStream(Map);
			fis.read(aByte);
			pushMessage("aByte = [" + aByte + "]!", 0, "");
			fis.close();
			return aByte;
		} catch (Exception e) {
			return null;
		}
	}
	public final Socket method19(int i)
			throws IOException {
		if (Signlink.mainapp != null)
			return Signlink.opensocket(i);
		else
			return new Socket(InetAddress.getByName(getCodeBase().getHost()), i);
	}

	public final DataInputStream method132(String s)
			throws IOException {
		//if(!aBoolean872)
		//    if(signlink.mainapp != null)
		//        return signlink.openurl(s);
		//    else
		//        return new DataInputStream((new URL(getCodeBase(), s)).openStream());
		if (aSocket832 != null) {
			try {
				aSocket832.close();
			} catch (Exception _ex) {
			}
			aSocket832 = null;
		}
		aSocket832 = method19(43595);
		aSocket832.setSoTimeout(10000);
		java.io.InputStream inputstream = aSocket832.getInputStream();
		OutputStream outputstream = aSocket832.getOutputStream();
		outputstream.write(("JAGGRAB /" + s + "\n\n").getBytes());
		System.out.println("JAGGRAB /" + s + "\n\n");
		return new DataInputStream(inputstream);
	}
	public void saveMidi(boolean flag, byte abyte0[]) {
		Signlink.midifade = flag ? 1 : 0;
		Signlink.midisave(abyte0, abyte0.length);
	}

	public final void loadRegion() {
		try {
			anInt985 = -1;
			aClass19_1056.removeAll();
			aClass19_1013.removeAll();
			Rasterizer.method366();
			unlinkMRUNodes();
			scene.initToNull();
			System.gc();
			//load_objects();
			for (int i = 0; i < 4; i++)
				collisionMaps[i].setDefault();

			for (int l = 0; l < 4; l++) {
				for (int k1 = 0; k1 < 104; k1++) {
					for (int j2 = 0; j2 < 104; j2++)
						byteGroundArray[l][k1][j2] = 0;
				}
			}
			ObjectManager currentMapRegion = new ObjectManager(byteGroundArray, intGroundArray);

			int k2 = terrainData.length;

			/*
			 * int k18 = 62; for (int A = 0; A < k2; A++) for (int B = 0; B < 2000; B++) if
			 * (anIntArray1234[A] == positions[B]) { anIntArray1235[A] = landScapes[B];
			 * anIntArray1236[A] = objects[B]; }
			 */

			stream.createFrame(0);

			if (!isDynamicRegion) {
				for (int i3 = 0; i3 < k2; i3++) {
					int i4 = (mapCoordinates[i3] >> 8) * 64 - baseX;
					int k5 = (mapCoordinates[i3] & 0xff) * 64 - baseY;

					byte abyte0[] = terrainData[i3];

					if (abyte0 != null)
						currentMapRegion.method180(abyte0, k5, i4, (currentRegionX - 6) * 8, (currentRegionY - 6) * 8,
								collisionMaps);
				}

				for (int j4 = 0; j4 < k2; j4++) {
					int l5 = (mapCoordinates[j4] >> 8) * 64 - baseX;
					int k7 = (mapCoordinates[j4] & 0xff) * 64 - baseY;
					byte abyte2[] = terrainData[j4];
					if (abyte2 == null && currentRegionY < 800)
						currentMapRegion.method6248(l5,  k7, 64, 64);
				}

				anInt1097++;
				if (anInt1097 > 160) {
					anInt1097 = 0;
					stream.createFrame(238);
					stream.writeUnsignedByte(96);

				}
				stream.createFrame(0);

				for (int i6 = 0; i6 < k2; i6++) {
					byte abyte1[] = objectData[i6];
					if (abyte1 != null) {
						int l8 = (mapCoordinates[i6] >> 8) * 64 - baseX;
						int k9 = (mapCoordinates[i6] & 0xff) * 64 - baseY;
						currentMapRegion.method190(l8, collisionMaps, k9, scene, abyte1);
					}
				}

			}
			if (isDynamicRegion) {
				for (int j3 = 0; j3 < 4; j3++) {
					for (int k4 = 0; k4 < 13; k4++) {
						for (int j6 = 0; j6 < 13; j6++) {
							int l7 = constructRegionData[j3][k4][j6];
							if (l7 != -1) {
								int i9 = l7 >> 24 & 3;
								int l9 = l7 >> 1 & 3;
								int j10 = l7 >> 14 & 0x3ff;
								int l10 = l7 >> 3 & 0x7ff;
								int j11 = (j10 / 8 << 8) + l10 / 8;
								for (int l11 = 0; l11 < mapCoordinates.length; l11++) {
									if (mapCoordinates[l11] != j11 || terrainData[l11] == null)
										continue;
									currentMapRegion.loadMapChunk(i9, l9, collisionMaps, k4 * 8, (j10 & 7) * 8,
											terrainData[l11], (l10 & 7) * 8, j3, j6 * 8);
									break;
								}

							}
						}

					}

				}

				for (int l4 = 0; l4 < 13; l4++) {
					for (int k6 = 0; k6 < 13; k6++) {
						int i8 = constructRegionData[0][l4][k6];
						if (i8 == -1)
							currentMapRegion.method6248(l4 * 8,  k6 * 8, 8, 8);
					}

				}

				stream.createFrame(0);

				for (int var2 = 0; var2 < 4; var2++) {
					for (int var3 = 0; var3 < 13; var3++) {
						for (int var4 = 0; var4 < 13; var4++) {
							boolean var18 = false;
							int chunkBits = constructRegionData[var2][var3][var4];
							if (chunkBits != -1) {
								int var7 = chunkBits >> 24 & 3;
								int var8 = chunkBits >> 1 & 3;
								int var9 = chunkBits >> 14 & 0x3ff;
								int var10 = chunkBits >> 3 & 0x7ff;
								int var11 = (var9 / 8 << 8) + var10 / 8;
								for (int var12 = 0; var12 < mapCoordinates.length; var12++) {
									if (mapCoordinates[var12] != var11 || objectData[var12] == null)
										continue;
									int var13 = (var9 - var3) * 8;
									int var14 = (var10 - var4) * 8;
									currentMapRegion.method3200(objectData[var12], var2, var3 * 8, var4 * 8, var7, (var9 & 7) * 8, (var10 & 7) * 8, var8, var13, var14, collisionMaps);
									var18 = true;
									break;
								}

							}
							if(!var18) {
								currentMapRegion.method3716(var2, var3 * 8, var4 * 8);
							}
						}

					}

				}

			}
			stream.createFrame(0);
			currentMapRegion.createRegionScene(collisionMaps, scene);
			mainGameGraphicsBuffer.setCanvas();
			stream.createFrame(0);

			int k3 = ObjectManager.maximumPlane;
			if (k3 > plane)
				k3 = plane;

			if (k3 < plane - 1)
				k3 = plane - 1;
			if (lowMem)

				scene.method275(ObjectManager.maximumPlane);
			else
				scene.method275(0);
			for (int i5 = 0; i5 < 104; i5++) {
				for (int i7 = 0; i7 < 104; i7++) {
					spawnGroundItem(i5, i7);
				}
			}

			anInt1051++;
			if (anInt1051 > 98) {
				anInt1051 = 0;
				stream.createFrame(150);

			}
			method63();
		} catch (Exception e) {
			e.printStackTrace();
		}
		ObjectDefinition.mruNodes1.unlinkAll();
		if (lowMem && Signlink.cache_dat != null) {
			int j = onDemandFetcher.getVersionCount(0);
			for (int i1 = 0; i1 < j; i1++) {
				int l1 = onDemandFetcher.getModelIndex(i1);
				if ((l1 & 0x79) == 0)
					Model.resetModel(i1);
			}

		}
		stream.createFrame(210);
		stream.writeDWord(0x3f008edd);
		System.gc();
		Rasterizer.method367();
		onDemandFetcher.method566();

		int k = (currentRegionX - 6) / 8 - 1;
		int j1 = (currentRegionX + 6) / 8 + 1;
		int i2 = (currentRegionY - 6) / 8 - 1;
		int l2 = (currentRegionY + 6) / 8 + 1;
		if (inTutorialIsland) {
			k = 49;
			j1 = 50;
			i2 = 49;
			l2 = 50;
		}
		for (int l3 = k; l3 <= j1; l3++) {
			for (int j5 = i2; j5 <= l2; j5++)
				if (l3 == k || l3 == j1 || j5 == i2 || j5 == l2) {
					int j7 = onDemandFetcher.getMapFiles(0, j5, l3);
					if (j7 != -1)
						onDemandFetcher.method560(j7, 3);
					int k8 = onDemandFetcher.getMapFiles(1, j5, l3);
					if (k8 != -1)
						onDemandFetcher.method560(k8, 3);
				}

		}

	}

	public void unlinkMRUNodes() {
		ObjectDefinition.mruNodes1.unlinkAll();
		ObjectDefinition.mruNodes2.unlinkAll();
		NpcDefinition.mruNodes.unlinkAll();
		ItemDefinition.mruNodes2.unlinkAll();
		ItemDefinition.mruNodes1.unlinkAll();
		Player.mruNodes.unlinkAll();
		GraphicsDefinition.recent_models.unlinkAll();
	}

	public void method24(int i) {
		int ai[] = minimapImage.myPixels;
		int j = ai.length;

		for (int k = 0; k < j; k++)
			ai[k] = 0;

		for (int l = 1; l < 103; l++) {
			int i1 = 24628 + (103 - l) * 512 * 4;
			for (int k1 = 1; k1 < 103; k1++) {
				if ((byteGroundArray[i][k1][l] & 0x18) == 0)
					scene.method309(ai, i1, i, k1, l);
				if (i < 3 && (byteGroundArray[i + 1][k1][l] & 8) != 0)
					scene.method309(ai, i1, i + 1, k1, l);
				i1 += 4;
			}

		}

		int j1 = ((238 + (int) (Math.random() * 20D)) - 10 << 16) + ((238 + (int) (Math.random() * 20D)) - 10 << 8)
				+ ((238 + (int) (Math.random() * 20D)) - 10);
		int l1 = (238 + (int) (Math.random() * 20D)) - 10 << 16;
		minimapImage.method343();
		for (int i2 = 1; i2 < 103; i2++) {
			for (int j2 = 1; j2 < 103; j2++) {
				if ((byteGroundArray[i][j2][i2] & 0x18) == 0)
					method50(i2, j1, j2, l1, i);
				if (i < 3 && (byteGroundArray[i + 1][j2][i2] & 8) != 0)
					method50(i2, j1, j2, l1, i + 1);
			}

		}

		mainGameGraphicsBuffer.setCanvas();
		mapIconAmount = 0;
		for (int k2 = 0; k2 < 104; k2++) {
			for (int l2 = 0; l2 < 104; l2++) {
				long i3 = scene.method303(plane, k2, l2);
				if (i3 != 0) {
					int objId = ObjectKey.getObjectId(i3);
					int j3 = ObjectDefinition.forID(objId).mapFunctionId;
					if (j3 >= 0) {
						int sprite = AreaDefinition.lookup(j3).spriteId;
						if (sprite != -1) {
							int k3 = k2;
							int l3 = l2;
							System.out.println("Area id: " + j3);
							mapIconSprite[mapIconAmount] = OsrsSprite[sprite];
							anIntArray1072[mapIconAmount] = k3;
							anIntArray1073[mapIconAmount] = l3;
							mapIconAmount++;
						}
					}
				}
			}

		}

	}

	public void spawnGroundItem(int i, int j) {
		NodeList class19 = groundItems[plane][i][j];
		if (class19 == null) {
			scene.method295(plane, i, j);
			return;
		}
		int k = 0xfa0a1f01;
		Object obj = null;
		for (Item item = (Item) class19.reverseGetFirst(); item != null; item = (Item) class19.reverseGetNext()) {
			ItemDefinition itemDef = ItemDefinition.forID(item.ID);

			int l = itemDef.value;
			if (itemDef.stackable)
				l *= item.anInt1559 + 1;
			if (l > k) {
				k = l;
				obj = item;
			}
			obj = item;
		}

		class19.insertTail(((Node) (obj)));
		Object obj1 = null;
		Object obj2 = null;
		for (Item class30_sub2_sub4_sub2_1 = (Item) class19
				.reverseGetFirst(); class30_sub2_sub4_sub2_1 != null; class30_sub2_sub4_sub2_1 = (Item) class19
				.reverseGetNext()) {
			if (class30_sub2_sub4_sub2_1.ID != ((Item) (obj)).ID && obj1 == null)
				obj1 = class30_sub2_sub4_sub2_1;
			if (class30_sub2_sub4_sub2_1.ID != ((Item) (obj)).ID && class30_sub2_sub4_sub2_1.ID != ((Item) (obj1)).ID
					&& obj2 == null)
				obj2 = class30_sub2_sub4_sub2_1;
		}

		int i1 = i + (j << 7) + 0x60000000;
		scene.method281(i, i1, ((Renderable) (obj1)), getCenterHeight(plane, j * 128 + 64, i * 128 + 64),
				((Renderable) (obj2)), ((Renderable) (obj)), plane, j);
	}

	public void method26(boolean flag) {
		for (int j = 0; j < npcCount; j++) {
			NPC npc = npcs[npcIndices[j]];
			long k = 0x20000000L | (long) npcIndices[j] << 32;
			if (npc == null || !npc.isVisible() || npc.desc.aBoolean93 != flag)
				continue;
			int l = npc.x >> 7;
			int i1 = npc.y >> 7;
			if (l < 0 || l >= 104 || i1 < 0 || i1 >= 104)
				continue;
			if (npc.size == 1 && (npc.x & 0x7f) == 64 && (npc.y & 0x7f) == 64) {
				if (anIntArrayArray929[l][i1] == anInt1265)
					continue;
				anIntArrayArray929[l][i1] = anInt1265;
			}if (!npc.desc.aBoolean84)
				k |= ~0x7fffffffffffffffL;
			scene.method285(plane, npc.target_direction, getCenterHeight(plane, npc.y, npc.x), k, npc.y,
					(npc.size - 1) * 64 + 60, npc.x, npc, npc.is_walking);
		}
	}

	public void loadError() {
		String s = "ondemand";// was a constant parameter
		System.out.println(s);
		try {
			getAppletContext().showDocument(new URL(getCodeBase(), "loaderror_" + s + ".html"));
		} catch (Exception exception) {
			exception.printStackTrace();
		}
		do
			try {
				Thread.sleep(1000L);
			} catch (Exception _ex) {
			}
		while (true);
	}

	public int drawTabInterfaceHoverParent;
	public int drawTabInterfaceHover;
	public int drawTabInterfaceHoverLast;
	public long drawTabInterfaceHoverTimer;

	public RSInterface lastHovered;

	public void resetTabInterfaceHover() {
		drawTabInterfaceHover = 0;
		if (lastHovered != null) {
			lastHovered.isHovered = false;
			lastHovered.hoverRunnable.run();
			lastHovered = null;
		}
	}

	// interfaceClick
	private void buildInterfaceMenu(int xPosition, RSInterface class9, int mouseX, int yPosition, int mouseY, int j1) {
		if (class9 == null) {
			return;
		}
		if (class9.type != 0 || class9.children == null || class9.isMouseoverTriggered)
			return;
		if (mouseX < xPosition || mouseY < yPosition || mouseX > xPosition + class9.width || mouseY > yPosition + class9.height)
			return;
		int childCount = class9.children.length;
		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			try {
				int drawX = class9.childX[childIndex] + xPosition;
				int drawY = (class9.childY[childIndex] + yPosition) - j1;
				RSInterface class9_1 = RSInterface.interfaceCache[class9.children[childIndex]];
				if (class9_1 == null) {
					break;
				}
				drawX += class9_1.anInt263;
				drawY += class9_1.anInt265;

				if (mouseX >= drawX && mouseY >= drawY && mouseX <= drawX + class9_1.width && mouseY <= drawY + class9_1.height) {
					if (class9_1.hoverInterfaceId != 0) {
						drawTabInterfaceHoverParent = class9_1.id;
						drawTabInterfaceHover = class9_1.hoverInterfaceId;
					}

					if (class9_1.hoverRunnable != null && lastHovered == null) {
						class9_1.isHovered = true;
						class9_1.hoverRunnable.run();
						lastHovered = class9_1;
					}
				}

				if ((class9_1.mOverInterToTrigger >= 0 || class9_1.anInt216 != 0) && mouseX >= drawX && mouseY >= drawY
						&& mouseX < drawX + class9_1.width && mouseY < drawY + class9_1.height) {
					if (class9_1.mOverInterToTrigger >= 0) {
						anInt886 = class9_1.mOverInterToTrigger;
					} else {
						anInt886 = class9_1.id;
					}
				}
				if (class9_1.atActionType == RSInterface.AT_ACTION_TYPE_OPTION_DROPDOWN) {

					boolean flag = false;
					class9_1.hovered = false;
					class9_1.dropdownHover = -1;

					if (class9_1.dropdown.isOpen()) {

						// Inverted keybinds dropdown
						if (class9_1.type == RSInterface.TYPE_KEYBINDS_DROPDOWN && class9_1.inverted && mouseX >= drawX
								&& mouseX < drawX + (class9_1.dropdown.getWidth() - 16)
								&& mouseY >= drawY - class9_1.dropdown.getHeight() - 10 && mouseY < drawY) {
							resetTabInterfaceHover();
							int yy = mouseY - (drawY - class9_1.dropdown.getHeight());

							if (mouseX > drawX + (class9_1.dropdown.getWidth() / 2)) {
								class9_1.dropdownHover = ((yy / 15) * 2) + 1;
							} else {
								class9_1.dropdownHover = (yy / 15) * 2;
							}
							flag = true;
						} else if (!class9_1.inverted && mouseX >= drawX && mouseX < drawX + (class9_1.dropdown.getWidth() - 16)
								&& mouseY >= drawY + 19 && mouseY < drawY + 19 + class9_1.dropdown.getHeight()) {
							resetTabInterfaceHover();
							int yy = mouseY - (drawY + 19);

							if (class9_1.type == RSInterface.TYPE_KEYBINDS_DROPDOWN && class9_1.dropdown.doesSplit()) {
								if (mouseX > drawX + (class9_1.dropdown.getWidth() / 2)) {
									class9_1.dropdownHover = ((yy / 15) * 2) + 1;
								} else {
									class9_1.dropdownHover = (yy / 15) * 2;
								}
							} else {
								class9_1.dropdownHover = yy / 14; // Regular dropdown hover
							}
							flag = true;
						}
						if (flag) {
							if (menuActionRow != 1) {
								menuActionRow = 1;
							}
							menuActionName[menuActionRow] = "Select";
							menuActionID[menuActionRow] = 770;
							menuActionCmd2[menuActionRow] = class9_1.id;
							menuActionCmd1[menuActionRow] = class9_1.dropdownHover;
							menuActionCmd3[menuActionRow] = class9.id;
							menuActionRow++;
						}
					}
					if (mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.dropdown.getWidth() && mouseY < drawY + 24
							&& menuActionRow == 1) {
						class9_1.hovered = true;
						menuActionName[menuActionRow] = class9_1.dropdown.isOpen() ? "Hide" : "Show";
						menuActionID[menuActionRow] = 769;
						menuActionCmd2[menuActionRow] = class9_1.id;
						menuActionCmd3[menuActionRow] = class9.id;
						menuActionRow++;
					}
				}

				if (mouseX >= drawX && mouseY >= drawY && mouseX < drawX + (class9_1.type == 4 ? 100 : class9_1.width)
						&& mouseY < drawY + class9_1.height) {
					if (class9_1.actions != null && !class9_1.invisible && !class9_1.drawingDisabled) {

						if (!(class9_1.contentType == 206 && cs1_selected(class9_1))) {
							if (/*(class9_1.type == 4 && class9_1.message.length() > 0) || */class9_1.type == 5) {

								boolean drawOptions = true;

								// HARDCODE CLICKABLE TEXT HERE
								if (class9_1.parentID == 37128) { // Clan chat interface, dont show options for guests
									drawOptions = showClanOptions;
								}

								if (drawOptions) {
									for (int action = class9_1.actions.length - 1; action >= 0; action--) {
										if (class9_1.actions[action] != null) {
											String s = class9_1.actions[action]
													+ (class9_1.type == 4 ? " @or1@" + class9_1.message : "");

											if (s.contains("img")) {
												int prefix = s.indexOf("<img=");
												int suffix = s.indexOf(">");
												s = s.replaceAll(s.substring(prefix + 5, suffix), "");
												s = s.replaceAll("</img>", "");
												s = s.replaceAll("<img=>", "");
											}
											if (s.contains("ico")) {
												int prefix = s.indexOf("<ico=");
												int suffix = s.indexOf(">");
												s = s.replaceAll(s.substring(prefix + 5, suffix), "");
												s = s.replaceAll("</ico>", "");
												s = s.replaceAll("<ico=>", "");
											}
											menuActionName[menuActionRow] = s;
											menuActionID[menuActionRow] = 647;
											menuActionCmd1[menuActionRow] = action;
											menuActionCmd2[menuActionRow] = class9_1.id;
											menuActionRow++;
										}
									}
								}
							}
						}
					}
				}
				if (class9_1.type == 22) {
					RSInterface class9_2;
					int slot = class9_1.grandExchangeSlot;
					if (grandExchangeInformation[slot][0] == -1)
						class9_2 = RSInterface.interfaceCache[GrandExchange.grandExchangeBuyAndSellBoxIds[slot]];
					else
						class9_2 = RSInterface.interfaceCache[GrandExchange.grandExchangeItemBoxIds[slot]];
					buildInterfaceMenu(drawX, class9_2, mouseX, drawY, mouseY, j1);
				}
				if (class9_1.type == 9 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width && mouseY < drawY + class9_1.height) {
					anInt1315 = class9_1.id;
				}
				if (class9_1.type == 5 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width && mouseY < drawY + class9_1.height) {
					hoverId = class9_1.id;
				}

				if (class9_1.type == 0) {
					buildInterfaceMenu(drawX, class9_1, mouseX, drawY, mouseY, class9_1.scrollPosition);
					if (class9_1.scrollMax > class9_1.height) {
						if (scrollbarVisible(class9_1)) {
							method65(drawX + class9_1.width, class9_1.height, mouseX, mouseY, class9_1, drawY, true, class9_1.scrollMax);
						}
					}
				} else {
					if (class9_1.atActionType == RSInterface.OPTION_OK && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						boolean flag = false;
						if (class9_1.contentType != 0)
							flag = buildFriendsListMenu(class9_1);
						if (!flag) {
							menuActionName[menuActionRow] = class9_1.tooltip;
							menuActionID[menuActionRow] = 315;
							menuActionCmd3[menuActionRow] = class9_1.id;
							menuActionRow++;
						}
						if (class9_1.type == RSInterface.TYPE_HOVER || class9_1.type == RSInterface.TYPE_CONFIG_HOVER
								|| class9_1.type == RSInterface.TYPE_ADJUSTABLE_CONFIG
								|| class9_1.type == RSInterface.TYPE_BOX) {
							class9_1.toggled = true;
						}

					}
					if (class9_1.atActionType == 2 && spellSelected == 0 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						String s = class9_1.selectedActionName;
						if (s.indexOf(" ") != -1)
							s = s.substring(0, s.indexOf(" "));
						menuActionName[menuActionRow] = s + " @gre@" + class9_1.spellName;
						menuActionID[menuActionRow] = 626;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					// close interface button
					if (class9_1.atActionType == 3 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = "Close";
						menuActionID[menuActionRow] = 200;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					if (class9_1.atActionType == 4 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.tooltip;
						menuActionID[menuActionRow] = 169;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					if (class9_1.atActionType == 5 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.tooltip;
						menuActionID[menuActionRow] = 646;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					if (class9_1.atActionType == 6 && !aBoolean1149 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.tooltip;
						menuActionID[menuActionRow] = 679;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}

					if (spellSelected == 0 && class9_1.atActionType == RSInterface.AT_ACTION_TYPE_AUTOCAST && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.tooltip;
						menuActionID[menuActionRow] = Autocast.AUTOCAST_MENU_ACTION_ID;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}

					if (class9_1.type == 8 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width && mouseY < drawY + class9_1.height) {
						anInt1315 = class9_1.id;
					}
					if (class9_1.atActionType == 8 && !aBoolean1149 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						for (int s1 = 0; s1 < class9_1.tooltips.length; s1++) {
								/*if (!RSInterface.interfaceCache[32007].isMouseoverTriggered) {
									if (class9_1.id > 32016) {
										continue;
									}
								}*/
							menuActionName[menuActionRow] = class9_1.tooltips[s1];
							menuActionID[menuActionRow] = 1700 + s1;
							menuActionCmd3[menuActionRow] = class9_1.id;
							menuActionRow++;
						}
					}
					if (class9_1.atActionType == 9 && !aBoolean1149 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.tooltip;
						menuActionID[menuActionRow] = 1100;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					if (class9_1.atActionType == 10 && !aBoolean1149 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.getMenuItem().getText();
						menuActionID[menuActionRow] = 1200;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					if (class9_1.atActionType == 11 && mouseX >= drawX && mouseY >= drawY && mouseX < drawX + class9_1.width
							&& mouseY < drawY + class9_1.height) {
						menuActionName[menuActionRow] = class9_1.tooltip;
						menuActionID[menuActionRow] = 201;
						menuActionCmd3[menuActionRow] = class9_1.id;
						menuActionRow++;
					}
					if (mouseX >= drawX && mouseY >= drawY && mouseX < drawX + (class9_1.type == 4 ? 100 : class9_1.width)
							&& mouseY < drawY + class9_1.height) {

						if (class9_1.actions != null) {
							if ((class9_1.type == 4 && class9_1.message.length() > 0) || class9_1.type == 5) {
								for (int action = class9_1.actions.length - 1; action >= 0; action--) {
									if (class9_1.actions[action] != null) {
										menuActionName[menuActionRow] = class9_1.actions[action]
												+ (class9_1.type == 4 ? " " + class9_1.message : "");
										menuActionID[menuActionRow] = 647;
										menuActionCmd2[menuActionRow] = action;
										menuActionCmd3[menuActionRow] = class9_1.id;
										menuActionRow++;
									}
								}
							}
						}
					}
					if (class9_1.type == 2) {
						int k2 = 0;
						for (int l2 = 0; l2 < class9_1.height; l2++) {
							for (int i3 = 0; i3 < class9_1.width; i3++) {
								boolean smallSprite = openInterfaceID == 26000
										&& GrandExchange.isSmallItemSprite(class9_1.id) || class9_1.smallInvSprites;
								int size = smallSprite ? 18 : 32;
								int j3 = drawX + i3 * (size + class9_1.invSpritePadX);
								int k3 = drawY + l2 * (size + class9_1.invSpritePadY);
								if (k2 < 20) {
									j3 += class9_1.spritesX[k2];
									k3 += class9_1.spritesY[k2];
								}
								if (mouseX >= j3 && mouseY >= k3 && mouseX < j3 + size && mouseY < k3 + size) {
									mouseInvInterfaceIndex = k2;
									lastActiveInvInterface = class9_1.id;
									int itemID = class9_1.inventoryItemId[k2] - 1;
									if (class9_1.id == 30000) {
										itemID = (class9_1.inventoryItemId[k2] & 0x7FFF) - 1;
									}
									if (class9_1.inventoryItemId[k2] > 0) {
										ItemDefinition itemDef = ItemDefinition.forID(itemID);
										boolean hasDestroyOption = false;
										if (itemSelected == 1 && class9_1.isInventoryInterface) {
											if (class9_1.id != anInt1284 || k2 != anInt1283) {
												menuActionName[menuActionRow] = "Use " + selectedItemName + " with @lre@"
														+ itemDef.name;
												menuActionID[menuActionRow] = 870;
												menuActionCmd1[menuActionRow] = itemDef.id;
												menuActionCmd2[menuActionRow] = k2;
												menuActionCmd3[menuActionRow] = class9_1.id;
												menuActionRow++;
											}
										} else if (spellSelected == 1 && class9_1.isInventoryInterface) {
											if ((spellUsableOn & 0x10) == 16) {
												menuActionName[menuActionRow] = spellTooltip + " @lre@" + itemDef.name;
												menuActionID[menuActionRow] = 543;
												menuActionCmd1[menuActionRow] = itemDef.id;
												menuActionCmd2[menuActionRow] = k2;
												menuActionCmd3[menuActionRow] = class9_1.id;
												menuActionRow++;
											}
										} else {
											if (class9_1.isInventoryInterface) {

												for (int l3 = 4; l3 >= 3; l3--)
													if (itemDef.itemActions != null
															&& itemDef.itemActions[l3] != null) {
														menuActionName[menuActionRow] = itemDef.itemActions[l3]
																+ " @lre@" + itemDef.name;
														if (itemDef.itemActions[l3].contains("Wield") || itemDef.itemActions[l3].contains("Wear")) {
															hintMenu = true;
															hintName = itemDef.name;
															hintId = itemDef.id;
														} else {
															hintMenu = false;
														}
														if (l3 == 3)
															menuActionID[menuActionRow] = 493;
														if (l3 == 4)
															menuActionID[menuActionRow] = 847;
														hasDestroyOption = itemDef.itemActions[l3].contains("Destroy");
														menuActionCmd1[menuActionRow] = itemDef.id;
														menuActionCmd2[menuActionRow] = k2;
														menuActionCmd3[menuActionRow] = class9_1.id;
														menuActionRow++;
													} else if (l3 == 4) {
														menuActionName[menuActionRow] = "Drop @lre@" + itemDef.name;
														menuActionID[menuActionRow] = 847;
														menuActionCmd1[menuActionRow] = itemDef.id;
														menuActionCmd2[menuActionRow] = k2;
														menuActionCmd3[menuActionRow] = class9_1.id;
														menuActionRow++;
													}

											}
											if (class9_1.usableItemInterface) {
												hintMenu = false;
												menuActionName[menuActionRow] = "Use @lre@" + itemDef.name;
												menuActionID[menuActionRow] = 447;
												menuActionCmd1[menuActionRow] = itemDef.id;
												// k2 = inventory spot
												// System.out.println(k2);
												menuActionCmd2[menuActionRow] = k2;
												menuActionCmd3[menuActionRow] = class9_1.id;
												menuActionRow++;
												if (!hasDestroyOption && !menuOpen && shiftDrop && shiftDown) {
													menuActionsRow("Drop @lre@" + itemDef.name, 1, 847, 2);
													removeShiftDropOnMenuOpen = true;
												}
											}
											if (class9_1.isInventoryInterface && itemDef.itemActions != null) {
												for (int i4 = 2; i4 >= 0; i4--)
													if (itemDef.itemActions[i4] != null) {

														if (itemDef.itemActions[i4].contains("Wield") || itemDef.itemActions[i4].contains("Wear")) {
															hintMenu = true;
															hintName = itemDef.name;
															hintId = itemDef.id;
														} else {
															hintMenu = false;
														}

														menuActionName[menuActionRow] = itemDef.itemActions[i4]
																+ " @lre@" + itemDef.name;
														if (i4 == 0)
															menuActionID[menuActionRow] = 74;
														if (i4 == 1)
															menuActionID[menuActionRow] = 454;
														if (i4 == 2)
															menuActionID[menuActionRow] = 539;
														menuActionCmd1[menuActionRow] = itemDef.id;
														menuActionCmd2[menuActionRow] = k2;
														menuActionCmd3[menuActionRow] = class9_1.id;
														menuActionRow++;
														if (!hasDestroyOption && !menuOpen && shiftDrop && shiftDown) {
															menuActionsRow("Drop @lre@" + itemDef.name, 1, 847, 2);
															removeShiftDropOnMenuOpen = true;
														}
													}

											}

											if (class9_1.actions != null) {
												if (openInterfaceID == 21553 && class9_1.parentID == 3213) {
													// Equipment
													String[] options = new String[]{"Place in inventory", "Use as equipment"};
													for (int j4 = 8; j4 >= 0; j4--) {
														if (j4 > options.length - 1)
															continue;
														if (options[j4] != null) {
															menuActionName[menuActionRow] = options[j4] + " @lre@"
																	+ itemDef.name;
															if (j4 == 0)
																menuActionID[menuActionRow] = 632;
															if (j4 == 1)
																menuActionID[menuActionRow] = 78;
															if (j4 == 2)
																menuActionID[menuActionRow] = 867;
															if (j4 == 3)
																menuActionID[menuActionRow] = 431;
															if (j4 == 4)
																menuActionID[menuActionRow] = 53;
															if (j4 == 7)
																menuActionID[menuActionRow] = 1337;
															menuActionCmd1[menuActionRow] = itemDef.id;
															menuActionCmd2[menuActionRow] = k2;
															menuActionCmd3[menuActionRow] = class9_1.id;
															menuActionRow++;
														}
													}

												} else {
													// Standard containers
													if (Bank.isBankContainer(class9_1)) {
														if (class9_1.id == 5382 && placeHolders && class9_1.inventoryAmounts[k2] <= 0) {
															class9_1.actions = new String[8];
															class9_1.actions[1] = "Release";
														} else {
															if (modifiableXValue > 0) {// so issue is when x = 0
																if (class9_1.actions.length < 9) {
																	class9_1.actions = new String[]{
																			"Withdraw-1",
																			"Withdraw-5",
																			"Withdraw-10",
																			"Withdraw-All",
																			"Withdraw-X",
																			"Withdraw-" + modifiableXValue,
																			"Withdraw-All-but-1"
																	};
																} else {
																	class9_1.actions[5] = "Withdraw-" + modifiableXValue;
																}
															}
														}
													}

													for (int j4 = 8; j4 >= 0; j4--) {
														if (j4 > class9_1.actions.length - 1)
															continue;
														if (class9_1.actions[j4] != null) {
															menuActionName[menuActionRow] = class9_1.actions[j4] + " @lre@" + itemDef.name;
															if (class9_1.id != 1688/*equipment*/) {
																if (j4 == 0)
																	menuActionID[menuActionRow] = 632;
																if (j4 == 1)
																	menuActionID[menuActionRow] = 78;
																if (j4 == 2)
																	menuActionID[menuActionRow] = 867;
																if (j4 == 3)
																	menuActionID[menuActionRow] = 431;
																if (j4 == 4)
																	menuActionID[menuActionRow] = 53;
																if (j4 == 5)
																	menuActionID[menuActionRow] = 300;
																if (j4 == 6)
																	menuActionID[menuActionRow] = 291;

															} else {
																if (itemDef.equipActions[j4] == null) {
																	menuActionName[3] = "Operate @lre@" + itemDef.name;
																	menuActionName[4] = "Remove @lre@" + itemDef.name;
																} else {
																	class9_1.actions = itemDef.equipActions;
																	menuActionName[menuActionRow] = itemDef.equipActions[j4]
																			+ " @lre@" + itemDef.name;
																}
																if (j4 == 0)
																	menuActionID[menuActionRow] = 632; // remove
																if (j4 == 1)
																	menuActionID[menuActionRow] = 661; // operate 1
																if (j4 == 2)
																	menuActionID[menuActionRow] = 662; // operate 2
																if (j4 == 3)
																	menuActionID[menuActionRow] = 663; // operate 3
																if (j4 == 4)
																	menuActionID[menuActionRow] = 664; // operate 4
															}

															menuActionCmd1[menuActionRow] = itemDef.id;
															menuActionCmd2[menuActionRow] = k2;
															menuActionCmd3[menuActionRow] = class9_1.id;
															menuActionRow++;
														}
													}
												}
											}
										}

										if (class9_1.parentID >= 58040 && class9_1.parentID <= 58048
												|| class9_1.parentID >= 32100 && class9_1.parentID <= 32156
												|| class9_1.parentID >= 32200 && class9_1.parentID <= 32222) {
											return;
										}

										if (class9_1.isItemSearchComponent) {
											menuActionName[menuActionRow] = "Select @lre@" + itemDef.name;
											menuActionID[menuActionRow] = 1130;
											menuActionCmd1[menuActionRow] = itemDef.id;
											menuActionCmd2[menuActionRow] = k2;
											menuActionCmd3[menuActionRow] = class9_1.id;
											menuActionRow++;
										} else {
											if (debugModels) {
												if (System.currentTimeMillis() - debugDelay > 1000) {
													debugDelay = System.currentTimeMillis();
													pushMessage("<col=255>" + itemDef.name + ":</col> Male: <col=255>"
															+ itemDef.primaryMaleModel + "</col> Female: <col=255>"
															+ itemDef.primaryFemaleModel + "</col> Model id: <col=255>"
															+ itemDef.modelId, 0, "");
													pushMessage("Zoom: <col=255>" + itemDef.spriteScale
															+ "</col> Rotation: <col=255>x" + itemDef.spritePitch + " y"
															+ itemDef.spriteCameraRoll + "", 0, "");
													pushMessage("Offset: <col=255>x" + itemDef.spriteTranslateX + " y"
															+ itemDef.spriteTranslateY, 0, "");
													menuActionName[menuActionRow] = "#2 (@whi@"
															+ Arrays.toString(itemDef.originalModelColors) + ")@gre@)(@whi@"
															+ Arrays.toString(itemDef.modifiedModelColors) + ")@gre@)";
												}
											}

											if (myPlayer.isAdminRights()) {
												menuActionName[menuActionRow] = "Examine @lre@" + itemDef.name + " @whi@(" + (itemID) + ")";
											} else {
												menuActionName[menuActionRow] = "Examine @lre@" + itemDef.name;
											}
											menuActionID[menuActionRow] = 1125;
											menuActionCmd1[menuActionRow] = itemDef.id;
											menuActionCmd2[menuActionRow] = k2;
											menuActionCmd3[menuActionRow] = class9_1.id;
											menuActionRow++;
										}
									}
								}
								k2++;
							}

						}

					}
				}

			} catch (Exception | StackOverflowError e) {
				System.err.println(String.format("Error building interface menu: rootId=%d, childIndex=%d, childId=%d", class9.id, childIndex, class9.children[childIndex]));
				e.printStackTrace();
			}
		}
	}

	private void menuActionsRow(String action, int index, int actionID, int row) {
		if (menuOpen)
			return;
		menuActionName[index] = action;
		menuActionID[index] = actionID;
		menuActionRow = row;
	}

	public void drawScrollbar(int height, int scrollPosition, int yPosition, int xPosition, int scrollMax) {
		scrollBar1.drawSprite(xPosition, yPosition);
		scrollBar2.drawSprite(xPosition, (yPosition + height) - 16);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x000001, 16);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x3d3426, 15);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x342d21, 13);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x2e281d, 11);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x29241b, 10);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x252019, 9);
		DrawingArea.drawPixels(height - 32, yPosition + 16, xPosition, 0x000001, 1);
		int k1 = ((height - 32) * height) / scrollMax;
		if (k1 < 8)
			k1 = 8;
		int l1 = ((height - 32 - k1) * scrollPosition) / (scrollMax - height);
		DrawingArea.drawPixels(k1, yPosition + 16 + l1, xPosition, barFillColor, 16);
		DrawingArea.method341(yPosition + 16 + l1, 0x000001, k1, xPosition);
		DrawingArea.method341(yPosition + 16 + l1, 0x817051, k1, xPosition + 1);
		DrawingArea.method341(yPosition + 16 + l1, 0x73654a, k1, xPosition + 2);
		DrawingArea.method341(yPosition + 16 + l1, 0x6a5c43, k1, xPosition + 3);
		DrawingArea.method341(yPosition + 16 + l1, 0x6a5c43, k1, xPosition + 4);
		DrawingArea.method341(yPosition + 16 + l1, 0x655841, k1, xPosition + 5);
		DrawingArea.method341(yPosition + 16 + l1, 0x655841, k1, xPosition + 6);
		DrawingArea.method341(yPosition + 16 + l1, 0x61553e, k1, xPosition + 7);
		DrawingArea.method341(yPosition + 16 + l1, 0x61553e, k1, xPosition + 8);
		DrawingArea.method341(yPosition + 16 + l1, 0x5d513c, k1, xPosition + 9);
		DrawingArea.method341(yPosition + 16 + l1, 0x5d513c, k1, xPosition + 10);
		DrawingArea.method341(yPosition + 16 + l1, 0x594e3a, k1, xPosition + 11);
		DrawingArea.method341(yPosition + 16 + l1, 0x594e3a, k1, xPosition + 12);
		DrawingArea.method341(yPosition + 16 + l1, 0x514635, k1, xPosition + 13);
		DrawingArea.method341(yPosition + 16 + l1, 0x4b4131, k1, xPosition + 14);
		DrawingArea.method339(yPosition + 16 + l1, 0x000001, 15, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x000001, 15, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x655841, 14, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x6a5c43, 13, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x6d5f48, 11, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x73654a, 10, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x76684b, 7, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x7b6a4d, 5, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x7e6e50, 4, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x817051, 3, xPosition);
		DrawingArea.method339(yPosition + 17 + l1, 0x000001, 2, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x564b38, 15, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x5d513c, 14, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x625640, 11, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x655841, 10, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x6a5c43, 7, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x6e6046, 5, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x716247, 4, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x7b6a4d, 3, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x817051, 2, xPosition);
		DrawingArea.method339(yPosition + 18 + l1, 0x000001, 1, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x514635, 15, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x564b38, 14, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x5d513c, 11, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x61553e, 9, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x655841, 7, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x6a5c43, 5, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x6e6046, 4, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x73654a, 3, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x817051, 2, xPosition);
		DrawingArea.method339(yPosition + 19 + l1, 0x000001, 1, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x4b4131, 15, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x544936, 14, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x594e3a, 13, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x5d513c, 10, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x61553e, 8, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x655841, 6, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x6a5c43, 4, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x73654a, 3, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x817051, 2, xPosition);
		DrawingArea.method339(yPosition + 20 + l1, 0x000001, 1, xPosition);
		DrawingArea.method341(yPosition + 16 + l1, 0x000001, k1, xPosition + 15);
		DrawingArea.method339(yPosition + 15 + l1 + k1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x000001, 15, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x3f372a, 14, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x443c2d, 10, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x483e2f, 9, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x4a402f, 7, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x4b4131, 4, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x564b38, 3, xPosition);
		DrawingArea.method339(yPosition + 14 + l1 + k1, 0x000001, 2, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x443c2d, 15, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x4b4131, 11, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x514635, 9, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x544936, 7, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x564b38, 6, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x594e3a, 4, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x625640, 3, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x6a5c43, 2, xPosition);
		DrawingArea.method339(yPosition + 13 + l1 + k1, 0x000001, 1, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x443c2d, 15, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x4b4131, 14, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x544936, 12, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x564b38, 11, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x594e3a, 10, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x5d513c, 7, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x61553e, 4, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x6e6046, 3, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x7b6a4d, 2, xPosition);
		DrawingArea.method339(yPosition + 12 + l1 + k1, 0x000001, 1, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x000001, 16, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x4b4131, 15, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x514635, 14, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x564b38, 13, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x594e3a, 11, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x5d513c, 9, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x61553e, 7, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x655841, 5, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x6a5c43, 4, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x73654a, 3, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x7b6a4d, 2, xPosition);
		DrawingArea.method339(yPosition + 11 + l1 + k1, 0x000001, 1, xPosition);
	}

	/**
	 * NPC Updating
	 * <p>
	 * There is a crash at random npcs so i did some sort of cheap fix to stop it
	 * from throwing runtime exception which will ultimately log a player out, even
	 * regardless of where and when they are in the game, causes to many issues.
	 *
	 * @param stream
	 * @param i
	 */
	public void updateNPCs(Buffer stream, int i) {
		anInt839 = 0;
		anInt893 = 0;
		method139(stream);
		method46(i, stream);
		updateNpcState(stream);
		for (int k = 0; k < anInt839; k++) {
			int l = anIntArray840[k];
			if (npcs[l].anInt1537 != update_tick) {
				npcs[l].desc = null;
				npcs[l] = null;
			}
		}

		// Cheaphax fix to it doesnt throw exception and boot player
		if (stream.currentOffset == -1 || stream.currentOffset != i) {
			System.out.println("[NPC] Size mismatch : returning");
			return;
		}

		if (stream.currentOffset != i) {
			Signlink.reporterror(
					myUsername + " size mismatch in getnpcpos - pos:" + stream.currentOffset + " psize:" + i);
			throw new RuntimeException("eek");
		}
		for (int i1 = 0; i1 < npcCount; i1++)
			if (npcs[npcIndices[i1]] == null) {
				Signlink.reporterror(myUsername + " null entry in npc list - pos:" + i1 + " size:" + npcCount);
				throw new RuntimeException("eek");
			}

	}

	private int channelButtonHoverPosition;
	private int channelButtonClickPosition;

	public void processChatModeClick() {
		if (super.getMouseY() >= currentGameHeight - 22 && super.getMouseY() <= currentGameHeight) {
			if (super.getMouseX() >= 5 && super.getMouseX() <= 61) {
				channelButtonHoverPosition = 0;
				inputTaken = true;
			} else if (super.getMouseX() >= 71 && super.getMouseX() <= 127) {
				channelButtonHoverPosition = 1;
				inputTaken = true;
			} else if (super.getMouseX() >= 137 && super.getMouseX() <= 193) {
				channelButtonHoverPosition = 2;
				inputTaken = true;
			} else if (super.getMouseX() >= 203 && super.getMouseX() <= 259) {
				channelButtonHoverPosition = 3;
				inputTaken = true;
			} else if (super.getMouseX() >= 269 && super.getMouseX() <= 325) {
				channelButtonHoverPosition = 4;
				inputTaken = true;
			} else if (super.getMouseX() >= 335 && super.getMouseX() <= 391) {
				channelButtonHoverPosition = 5;
				inputTaken = true;
			} else if (super.getMouseX() >= 404 && super.getMouseX() <= 515) {
				channelButtonHoverPosition = 6;
				inputTaken = true;
			}
		} else {
			channelButtonHoverPosition = -1;
			inputTaken = true;
		}
		if (super.clickMode3 == 1) {
			if (super.getSaveClickY() >= currentGameHeight - 22 && super.getSaveClickY() <= currentGameHeight) {
				if (super.getSaveClickX() >= 5 && super.getSaveClickX() <= 61) {
					if (channelButtonClickPosition == 0)
						toggleHideChatArea();
					channelButtonClickPosition = 0;
					chatTypeView = 0;
					inputTaken = true;
				} else if (super.getSaveClickX() >= 71 && super.getSaveClickX() <= 127) {
					if (channelButtonClickPosition == 1)
						toggleHideChatArea();
					channelButtonClickPosition = 1;
					chatTypeView = 5;
					inputTaken = true;
				} else if (super.getSaveClickX() >= 137 && super.getSaveClickX() <= 193) {
					if (channelButtonClickPosition == 2)
						toggleHideChatArea();
					channelButtonClickPosition = 2;
					chatTypeView = 1;
					inputTaken = true;
				} else if (super.getSaveClickX() >= 203 && super.getSaveClickX() <= 259) {
					if (channelButtonClickPosition == 3)
						toggleHideChatArea();
					channelButtonClickPosition = 3;
					chatTypeView = 2;
					inputTaken = true;
				} else if (super.getSaveClickX() >= 269 && super.getSaveClickX() <= 325) {
					if (channelButtonClickPosition == 4)
						toggleHideChatArea();
					channelButtonClickPosition = 4;
					chatTypeView = 11;
					inputTaken = true;
				} else if (super.getSaveClickX() >= 335 && super.getSaveClickX() <= 391) {
					if (channelButtonClickPosition == 5)
						toggleHideChatArea();
					channelButtonClickPosition = 5;
					chatTypeView = 3;
					inputTaken = true;
				}
			}
		}
	}

	private static boolean hideChatArea = false;

	private static void toggleHideChatArea() {
		if (currentScreenMode == ScreenMode.FIXED)
			return;
		hideChatArea = !hideChatArea;
	}
	public void stopMidi() {
		if(Signlink.music != null) {
			Signlink.music.stop();
			Signlink.midifade = 0;
			Signlink.midi = "stop";
		}
	}


	public void method33(int i) {
		if (i > Varp.cacheSize)
			return;

		if (Varp.cache[i] == null)
			return;

		int j = Varp.cache[i].anInt709;
		if (j == 0)
			return;
		int k = variousSettings[i];
		if (j == 1) {
			ItemDefinition.mruNodes1.unlinkAll();
			welcomeScreenRaised = true;
		}
		if (j == 3) {
			boolean flag1 = this.musicEnabled;
			if(k == 0) {
				this.adjustVolume(this.musicEnabled, 500);
				this.musicEnabled = true;
			}

			if(k == 1) {
				this.adjustVolume(this.musicEnabled, 300);
				this.musicEnabled = true;
			}

			if(k == 2) {
				this.musicEnabled = false;
				this.musicEnabled = true;
			}

			if(k == 3) {
				adjustVolume(musicEnabled, -1200);
				this.musicEnabled = true;
			}

			if(k == 4) {
				this.musicEnabled = false;
			}

			if(this.musicEnabled != flag1) {
				if(this.musicEnabled) {
					this.nextSong = this.currentSong;
					this.songChanging = true;
					onDemandFetcher.provide(2, this.nextSong);
				} else {
					this.stopMidi();
				}

				this.prevSong = 0;
			}
		}
		if (j == 4) {
			if (k == 0) {
				soundEffectVolume = 127;
			} else if (k == 1) {
				soundEffectVolume = 96;
			} else if (k == 2) {
				soundEffectVolume = 64;
			} else if (k == 3) {
				soundEffectVolume = 32;
			} else if (k == 4) {
				soundEffectVolume = 0;
			}
		}
		if (j == 5)
			anInt1253 = k;
		if (j == 6)
			anInt1249 = k;
		if (j == 9)
			anInt913 = k;
	}

	public void updateEntities() {
	    if (anInt1265 % 50 == 0) {
	        //System.out.println("=== updateEntities called - using " + THREAD_COUNT + " threads ===");
	    }
	    try {
	        tempEntityCount = 0;
	        int anInt974 = 0;
	        
	        // PHASE 1: Calculate positions in parallel (the expensive part)
	        List<Future<EntityRenderData>> futures = new ArrayList<>();
	        
	        for (int j = -1; j < playerCount + npcCount; j++) {
	            final int entityIndex = j;
	            
	            futures.add(entityThreadPool.submit(() -> {
	                return calculateEntityData(entityIndex);
	            }));
	        }
	        
	        // PHASE 2: Collect results and render (must be single-threaded for drawing)
	        List<EntityRenderData> renderData = new ArrayList<>();
	        for (Future<EntityRenderData> future : futures) {
	            try {
	                EntityRenderData data = future.get();
	                if (data != null && data.shouldRender) {
	                    renderData.add(data);
	                }
	            } catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
	        
	        // PHASE 3: Single-threaded rendering (drawing must be sequential)
	        for (EntityRenderData data : renderData) {
	            renderEntityData(data, anInt974);
	            if (data.hasOverheadText) {
	                // Store overhead text data
	                if (anInt974 < anInt975) {
	                    anIntArray979[anInt974] = data.textWidth;
	                    anIntArray978[anInt974] = data.textHeight;
	                    anIntArray976[anInt974] = data.spriteX;
	                    anIntArray977[anInt974] = data.spriteY;
	                    anIntArray980[anInt974] = data.textColor;
	                    anIntArray981[anInt974] = data.textEffect;
	                    anIntArray982[anInt974] = data.textCycle;
	                    aStringArray983[anInt974++] = data.overheadText;
	                }
	            }
	        }
	        
	        // PHASE 4: Render overhead text (unchanged from original)
	        for (int k = 0; k < anInt974; k++) {
	            int k1 = anIntArray976[k];
	            int l1 = anIntArray977[k];
	            int j2 = anIntArray979[k];
	            int k2 = anIntArray978[k];
	            boolean flag = true;
	            while (flag) {
	                flag = false;
	                for (int l2 = 0; l2 < k; l2++)
	                    if (l1 + 2 > anIntArray977[l2] - anIntArray978[l2] && l1 - k2 < anIntArray977[l2] + 2
	                            && k1 - j2 < anIntArray976[l2] + anIntArray979[l2]
	                            && k1 + j2 > anIntArray976[l2] - anIntArray979[l2]
	                            && anIntArray977[l2] - anIntArray978[l2] < l1) {
	                        l1 = anIntArray977[l2] - anIntArray978[l2];
	                        flag = true;
	                    }
	            }
	            spriteDrawX = anIntArray976[k];
	            spriteDrawY = anIntArray977[k] = l1;

	            String s = aStringArray983[k];
	            if (anInt1249 == 0) {
	                int i3 = 0xffff00;
	                if (anIntArray980[k] < 6)
	                    i3 = anIntArray965[anIntArray980[k]];
	                if (anIntArray980[k] == 6)
	                    i3 = anInt1265 % 20 >= 10 ? 0xffff00 : 0xff0000;
	                if (anIntArray980[k] == 7)
	                    i3 = anInt1265 % 20 >= 10 ? 65535 : 255;
	                if (anIntArray980[k] == 8)
	                    i3 = anInt1265 % 20 >= 10 ? 0x80ff80 : 45056;
	                if (anIntArray980[k] == 9) {
	                    int j3 = 150 - anIntArray982[k];
	                    if (j3 < 50)
	                        i3 = 0xff0000 + 1280 * j3;
	                    else if (j3 < 100)
	                        i3 = 0xffff00 - 0x50000 * (j3 - 50);
	                    else if (j3 < 150)
	                        i3 = 65280 + 5 * (j3 - 100);
	                }
	                if (anIntArray980[k] == 10) {
	                    int k3 = 150 - anIntArray982[k];
	                    if (k3 < 50)
	                        i3 = 0xff0000 + 5 * k3;
	                    else if (k3 < 100)
	                        i3 = 0xff00ff - 0x50000 * (k3 - 50);
	                    else if (k3 < 150)
	                        i3 = (255 + 0x50000 * (k3 - 100)) - 5 * (k3 - 100);
	                }
	                if (anIntArray980[k] == 11) {
	                    int l3 = 150 - anIntArray982[k];
	                    if (l3 < 50)
	                        i3 = 0xffffff - 0x50005 * l3;
	                    else if (l3 < 100)
	                        i3 = 65280 + 0x50005 * (l3 - 50);
	                    else if (l3 < 150)
	                        i3 = 0xffffff - 0x50000 * (l3 - 100);
	                }
	                
	                // Render text based on effect type
	                if (anIntArray981[k] == 0) {
	                    chatTextDrawingArea.drawText(0, s, spriteDrawY + 1, spriteDrawX + 1);
	                    chatTextDrawingArea.drawText(i3, s, spriteDrawY, spriteDrawX);
	                }
	                if (anIntArray981[k] == 1) {
	                    chatTextDrawingArea.method386(0, s, spriteDrawX + 1, anInt1265, spriteDrawY + 1);
	                    chatTextDrawingArea.method386(i3, s, spriteDrawX, anInt1265, spriteDrawY);
	                }
	                if (anIntArray981[k] == 2) {
	                    chatTextDrawingArea.method387(spriteDrawX + 1, s, anInt1265, spriteDrawY + 1, 0);
	                    chatTextDrawingArea.method387(spriteDrawX, s, anInt1265, spriteDrawY, i3);
	                }
	                if (anIntArray981[k] == 3) {
	                    chatTextDrawingArea.method388(150 - anIntArray982[k], s, anInt1265, spriteDrawY + 1,
	                            spriteDrawX + 1, 0);
	                    chatTextDrawingArea.method388(150 - anIntArray982[k], s, anInt1265, spriteDrawY, spriteDrawX, i3);
	                }
	                if (anIntArray981[k] == 4) {
	                    int i4 = chatTextDrawingArea.method384(s);
	                    int k4 = ((150 - anIntArray982[k]) * (i4 + 100)) / 150;
	                    DrawingArea.setDrawingArea(334, spriteDrawX - 50, spriteDrawX + 50, 0);
	                    chatTextDrawingArea.method385(0, s, spriteDrawY + 1, (spriteDrawX + 50) - k4);
	                    chatTextDrawingArea.method385(i3, s, spriteDrawY, (spriteDrawX + 50) - k4);
	                    DrawingArea.defaultDrawingAreaSize();
	                }
	                if (anIntArray981[k] == 5) {
	                    int j4 = 150 - anIntArray982[k];
	                    int l4 = 0;
	                    if (j4 < 25)
	                        l4 = j4 - 25;
	                    else if (j4 > 125)
	                        l4 = j4 - 125;
	                    DrawingArea.setDrawingArea(spriteDrawY + 5, 0, 516,
	                            spriteDrawY - chatTextDrawingArea.anInt1497 - 1);
	                    chatTextDrawingArea.drawText(0, s, spriteDrawY + 1 + l4, spriteDrawX);
	                    chatTextDrawingArea.drawText(i3, s, spriteDrawY + l4, spriteDrawX);
	                    DrawingArea.defaultDrawingAreaSize();
	                }
	            } else {
	                chatTextDrawingArea.drawText(0, s, spriteDrawY + 1, spriteDrawX);
	                chatTextDrawingArea.drawText(0xffff00, s, spriteDrawY, spriteDrawX);
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	// ===== HELPER METHOD: Calculate entity data in parallel =====
	private EntityRenderData calculateEntityData(int j) {
	    try {
	        Object obj;
	        if (j == -1)
	            obj = myPlayer;
	        else if (j < playerCount)
	            obj = players[playerIndices[j]];
	        else
	            obj = npcs[npcIndices[j - playerCount]];
	            
	        if (obj == null || !((Entity) obj).isVisible())
	            return null;
	            
	        if (obj instanceof NPC) {
	            NpcDefinition entityDef = ((NPC) obj).desc;
	            if (entityDef.childrenIDs != null)
	                entityDef = entityDef.method161();
	            if (entityDef == null)
	                return null;
	        }
	        
	        EntityRenderData data = new EntityRenderData();
	        data.entity = (Entity) obj;
	        data.entityIndex = j;
	        data.isPlayer = j < playerCount;
	        data.shouldRender = true;
	        
	        // Calculate screen position
	        npcScreenPos(data.entity, data.entity.height + 15);
	        data.spriteX = spriteDrawX;
	        data.spriteY = spriteDrawY;
	        
	        // Check if has overhead text
	        if (data.entity.textSpoken != null && 
	            (j >= playerCount || publicChatMode == 0 || publicChatMode == 3 || 
	             publicChatMode == 1 && isFriendOrSelf(((Player) obj).displayName))) {
	            data.hasOverheadText = true;
	            data.overheadText = data.entity.textSpoken;
	            data.textWidth = chatTextDrawingArea.method384(data.entity.textSpoken) / 2;
	            data.textHeight = chatTextDrawingArea.anInt1497;
	            data.textColor = data.entity.anInt1513;
	            data.textEffect = data.entity.anInt1531;
	            data.textCycle = data.entity.textCycle;
	        }
	        
	        // Store player/NPC specific data
	        if (data.isPlayer) {
	            Player player = (Player) obj;
	            data.headIcon = player.headIcon;
	            data.skullIcon = player.skullIcon;
	            data.displayName = player.displayName;
	            data.hintIcon = player.hintIcon;
	        } else {
	            NPC npc = (NPC) obj;
	            data.npcHeadIcon = npc.desc.anInt75;
	        }
	        
	        // Health bar data
	        data.loopCycleStatus = data.entity.loopCycleStatus;
	        data.currentHealth = data.entity.currentHealth;
	        data.maxHealth = data.entity.maxHealth;
	        
	        // Hitsplat data
	        for (int i = 0; i < 4; i++) {
	            data.hitsLoopCycle[i] = data.entity.hitsLoopCycle[i];
	            data.hitMarkTypes[i] = data.entity.hitMarkTypes[i];
	            data.hitArray[i] = data.entity.hitArray[i];
	        }
	        
	        return data;
	    } catch (Exception e) {
	        return null;
	    }
	}

	// ===== HELPER METHOD: Render entity data (single-threaded) =====
	private void renderEntityData(EntityRenderData data, int textIndex) {
	    try {
	        Entity entity = data.entity;
	        int j = data.entityIndex;
	        
	        // Render player icons
	        if (data.isPlayer && data.spriteX > -1) {
	            int l = 30;
	            if (data.headIcon >= 0) {
	                if (data.skullIcon < 2) {
	                    skullIcons[data.skullIcon].drawSprite(data.spriteX - 12, data.spriteY - l);
	                    l += 25;
	                }
	                if (data.headIcon < 7) {
	                    headIcons[data.headIcon].drawSprite(data.spriteX - 12, data.spriteY - l);
	                    l += 18;
	                }
	            }
	            
	            // Hint icons
	            if (j >= 0 && hintType == 10 && anInt933 == playerIndices[j]) {
	                headIconsHint[data.hintIcon].drawSprite(data.spriteX - 12, data.spriteY - l);
	            }
	            
	            // Player names
	            if (Configuration.playerNames) {
	                latoBold.drawBasicString(data.displayName, 
	                    (data.spriteX - (data.displayName.length() * 5)) + 3, 
	                    data.spriteY + 7, 0x6495ed, 1);
	            }
	        }
	        
	        // Render NPC icons
	        if (!data.isPlayer && data.npcHeadIcon >= 0 && data.npcHeadIcon < headIcons.length) {
	            if (data.spriteX > -1) {
	                headIcons[data.npcHeadIcon].drawSprite(data.spriteX - 12, data.spriteY - 30);
	            }
	            
	            // NPC hint arrow
	            if (hintType == 1 && anInt1222 == npcIndices[j - playerCount] && update_tick % 20 < 10) {
	                headIconsHint[0].drawSprite(data.spriteX - 12, data.spriteY - 28);
	            }
	        }
	        
	        // Health bars
	        if (data.loopCycleStatus > update_tick && data.spriteX > -1) {
	            int i1 = (data.currentHealth * 30) / data.maxHealth;
	            int i2 = (data.currentHealth * 30) / data.maxHealth;
	            if (i1 > 30) i1 = 30;
	            
	            if (data.maxHealth >= 255) {
	                DrawingArea.drawPixels(5, data.spriteY - 3, data.spriteX - 15, 65280, i2);
	                DrawingArea.drawPixels(5, data.spriteY - 3, (data.spriteX - 15) + i2, 0xff0000, 30 - i2);
	            } else {
	                DrawingArea.drawPixels(5, data.spriteY - 3, data.spriteX - 15, 65280, i1);
	                DrawingArea.drawPixels(5, data.spriteY - 3, (data.spriteX - 15) + i1, 0xff0000, 30 - i1);
	            }
	        }
	        
	        // Hitsplats
	        for (int j1 = 0; j1 < 4; j1++) {
	            if (data.hitsLoopCycle[j1] > update_tick) {
	                npcScreenPos(entity, entity.height / 2);
	                if (spriteDrawX > -1) {
	                    int adjustedY = spriteDrawY;
	                    int adjustedX = spriteDrawX;
	                    
	                    if (j1 == 1) adjustedY -= 20;
	                    if (j1 == 2) { adjustedX -= 15; adjustedY -= 10; }
	                    if (j1 == 3) { adjustedX += 15; adjustedY -= 10; }
	                    if (j1 == 19) { adjustedX += 1; adjustedY -= 1; }
	                    
	                    hitMarks[data.hitMarkTypes[j1]].drawSprite(adjustedX - 12, adjustedY - 12);
	                    smallText.drawText(0, String.valueOf(data.hitArray[j1]), adjustedY + 4, adjustedX);
	                    smallText.drawText(0xffffff, String.valueOf(data.hitArray[j1]), adjustedY + 3, adjustedX - 1);
	                }
	            }
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}

	// ===== DATA CLASS =====
	private class EntityRenderData {
	    Entity entity;
	    int entityIndex;
	    boolean isPlayer;
	    boolean shouldRender;
	    
	    int spriteX, spriteY;
	    
	    // Player data
	    int headIcon = -1;
	    int skullIcon = -1;
	    int hintIcon = -1;
	    String displayName = "";
	    
	    // NPC data
	    int npcHeadIcon = -1;
	    
	    // Overhead text
	    boolean hasOverheadText = false;
	    String overheadText = "";
	    int textWidth, textHeight;
	    int textColor, textEffect, textCycle;
	    
	    // Health
	    int loopCycleStatus;
	    int currentHealth, maxHealth;
	    
	    // Hitsplats
	    int[] hitsLoopCycle = new int[4];
	    int[] hitMarkTypes = new int[4];
	    int[] hitArray = new int[4];
	}

	private void delFriend(long l) {
		try {
			if (l == 0L)
				return;
			for (int i = 0; i < friendsCount; i++) {
				if (friendsListAsLongs[i] != l)
					continue;
				friendsCount--;
				needDrawTabArea = true;
				for (int j = i; j < friendsCount; j++) {
					friendsList[j] = friendsList[j + 1];
					friendsNodeIDs[j] = friendsNodeIDs[j + 1];
					friendsListAsLongs[j] = friendsListAsLongs[j + 1];
				}

				stream.createFrame(215);
				stream.writeQWord(l);
				break;
			}
		} catch (RuntimeException runtimeexception) {
			Signlink.reporterror("18622, " + false + ", " + l + ", " + runtimeexception.toString());
			throw new RuntimeException();
		}
	}

	Sprite tabAreaFixed;
	Sprite[] tabAreaResizable = new Sprite[3];
	private boolean drawingTabArea = false;

	public void drawTabArea() {
		drawingTabArea = true;
		boolean fixedMode = currentScreenMode == ScreenMode.FIXED;
		if (fixedMode && loginScreenGraphicsBuffer == null && tabAreaGraphicsBuffer != null)
			tabAreaGraphicsBuffer.initDrawingArea();

		Rasterizer.anIntArray1472 = anIntArray1181;
		if (fixedMode) {
			tabAreaFixed.drawSprite(0, 0);
			if (invOverlayInterfaceID == -1)
				drawTabs();

		} else {
			(stackTabs() ? tabAreaResizable[1] : tabAreaResizable[2]).drawSpriteWithOpacity(
					Client.currentGameWidth - (stackTabs() ? 231 : 462),
					Client.currentGameHeight - (stackTabs() ? 73 : 37), 220);

			tabAreaResizable[0].drawSpriteWithOpacity(Client.currentGameWidth - 204,
					Client.currentGameHeight - 275 - (stackTabs() ? 73 : 37), 220);

			if (invOverlayInterfaceID == -1)
				drawTabs();
		}
		int y = stackTabs() ? 73 : 37;

		// Set open tab to one that has an interface
		if (tabInterfaceIDs[tabID] <= 0) {
			for (int i = tabID; i >= 0; i--) {
				if (tabInterfaceIDs[i] >= 1) {
					tabID = i;
					break;
				}
			}

			if (tabInterfaceIDs[tabID] <= 0)
				tabID = 3;
		}

		if (invOverlayInterfaceID != -1) {
			drawInterface(0, fixedMode ? 31 : currentGameWidth - 197,
					RSInterface.interfaceCache[invOverlayInterfaceID],
					fixedMode ? 37 : currentGameHeight - 275 - y + 10);
		} else if (Client.tabInterfaceIDs[Client.tabID] != -1) {
			try {
				drawInterface(0, fixedMode ? 31 : currentGameWidth - 197,
						RSInterface.interfaceCache[Client.tabInterfaceIDs[Client.tabID]],
						fixedMode ? 37 : currentGameHeight - 275 - y + 10);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (drawTabInterfaceHover != 0) {
			RSInterface parent = RSInterface.interfaceCache[drawTabInterfaceHoverParent];
			if (drawTabInterfaceHoverTimer == 0 || drawTabInterfaceHoverLast != drawTabInterfaceHover)
				drawTabInterfaceHoverTimer = System.currentTimeMillis();
			drawTabInterfaceHoverLast = drawTabInterfaceHover;
			if (System.currentTimeMillis() - drawTabInterfaceHoverTimer >= parent.hoverInterfaceDelay) {
				RSInterface hover = RSInterface.interfaceCache[drawTabInterfaceHover];
				int xOffset = fixedMode ? 516 : 0;
				int yOffset = fixedMode ? 168 : 0;
				int drawingAreaWidth = currentGameWidth - xOffset;
				int drawingAreaHeight = currentGameHeight - yOffset;
				int mouseX = super.getMouseX() - xOffset;
				int mouseY = super.getMouseY() - yOffset;
				int interfaceWidth = hover.width + 12; // Was drawing offscreen a bit, don't have time to proper fix

				if (mouseX + interfaceWidth > drawingAreaWidth) {
					mouseX = drawingAreaWidth - interfaceWidth;
				}

				if (mouseY + hover.height > drawingAreaHeight) {
					mouseY = drawingAreaHeight - hover.height;
				}

				DrawingArea.setDrawingArea(worldViewportHeight, 0, worldViewportWidth, 0);
				drawInterface(0, mouseX, hover, mouseY, true);
			}
		} else {
			drawTabInterfaceHoverTimer = 0;
		}

		//if (currentScreenMode == ScreenMode.FIXED) {
		///	mapArea[6].drawSprite(0, 178);
		//}
		if (menuOpen) {
			drawMenu(fixedMode ? 516 : 0, fixedMode ? 168 : 0);
		}
		if(getUserSettings().isInventoryContextMenu() && hintMenu){
			drawHintMenu(hintName,hintId, getUserSettings().getStartMenuColor());
		}
		if (fixedMode && loginScreenGraphicsBuffer == null && tabAreaGraphicsBuffer != null)
			tabAreaGraphicsBuffer.drawGraphics(516, 168, super.graphics);

		mainGameGraphicsBuffer.setCanvas();
		Rasterizer.anIntArray1472 = anIntArray1182;
		drawingTabArea = false;
	}

	Sprite[] sideIcons = new Sprite[15];
	Sprite[] redStones = new Sprite[6];

	private void drawTabs() {
		if (currentScreenMode == ScreenMode.FIXED) {
			final int[][] sideIconCoordinates = new int[][] { { 17, 17 }, { 49, 15 }, { 83, 15 }, { 113, 13 },
					{ 146, 10 }, { 180, 11 }, { 214, 15 }, { 14, 311 }, { 49, 314 }, { 82, 314 }, { 116, 310 },
					{ 148, 312 }, { 184, 311 }, { 216, 311 }, { 216, 311 } };
			final int[][] sideIconCoordinates1 = new int[][] { { 24, 8 }, { 49, 5}, { 79, 5 }, { 108, 3}, { 147, 5 },
					{ 176, 5 }, { 205, 8 }, { 22, 300 }, { 49, 304 }, { 77, 304 }, { 111, 303 }, { 147, 301 },
					{ 180, 303 }, { 204, 303 }, { 204, 303 } };
			if (Client.tabInterfaceIDs[Client.tabID] != -1) {
				if(getUserSettings().isOldGameframe() == false) {
					if (Client.tabID == 0)
						redStones[0].drawSprite(5, 0);
					if (Client.tabID == 1)
						redStones[4].drawSprite(43, 0);
					if (Client.tabID == 2)
						redStones[4].drawSprite(76, 0);
					if (Client.tabID == 3)
						redStones[4].drawSprite(109, 0);
					if (Client.tabID == 4)
						redStones[4].drawSprite(142, 0);
					if (Client.tabID == 5)
						redStones[4].drawSprite(175, 0);
					if (Client.tabID == 6)
						redStones[1].drawSprite(208, 0);
					if (Client.tabID == 7)
						redStones[2].drawSprite(5, 298);
					if (Client.tabID == 8)
						redStones[4].drawSprite(43, 298);
					if (Client.tabID == 9)
						redStones[4].drawSprite(76, 298);
					if (Client.tabID == 10)
						redStones[4].drawSprite(109, 298);
					if (Client.tabID == 11)
						redStones[4].drawSprite(142, 298);
					if (Client.tabID == 12)
						redStones[4].drawSprite(175, 298);
					if (Client.tabID == 13)
						redStones[3].drawSprite(208, 298);
				}else {
					if (Client.tabID == 0)
						redStones[1].drawSprite(14, 0);
					if (Client.tabID == 1)
						redStones[2].drawSprite(47, 0);
					if (Client.tabID == 2)
						redStones[2].drawSprite(74, 0);
					if (Client.tabID == 3)
						redStones[3].drawSprite(102, 0);
					if (Client.tabID == 4)
						redStones[2].drawSprite(144, 0);
					if (Client.tabID == 5)
						redStones[2].drawSprite(172, 0);
					if (Client.tabID == 6)
						redStones[0].drawSprite(201, 0);
					if (Client.tabID == 7)
						redStones[4].drawSprite(13, 296);
					if (Client.tabID == 8)
						redStones[2].drawSprite(46, 297);
					if (Client.tabID == 9)
						redStones[2].drawSprite(74, 298);
					if (Client.tabID == 10)
						redStones[3].drawSprite(102, 297);
					if (Client.tabID == 11)
						redStones[2].drawSprite(144, 296);
					if (Client.tabID == 12)
						redStones[2].drawSprite(171, 296);
					if (Client.tabID == 13)
						redStones[5].drawSprite(201, 298);
				}

			}
			for (int index = 0; index <= 14; index++) {
				if (Client.tabInterfaceIDs[index] > 0) {
					if (!getUserSettings().isOldGameframe()) {
						if (index != 13 || (index == 13 && pollActive)) {
							sideIcons[index].drawSprite(sideIconCoordinates[index][0], sideIconCoordinates[index][1] - 8);
						} else {
							sideIcons[index + 1].drawSprite(sideIconCoordinates[index][0], sideIconCoordinates[index][1] - 8);
						}
					} else {
						sideIcons[index].drawSprite(sideIconCoordinates1[index][0], sideIconCoordinates1[index][1]);
					}
				}
			}
		} else {
			final int[][] sideIconOffsets = new int[][] { { 7, 8 }, { 4, 6 }, { 6, 7 }, { 3, 4 }, { 3, 2 }, { 4, 3 },
					{ 4, 6 }, { 5, 5 }, { 5, 6 }, { 5, 6 }, { 6, 3 }, { 5, 5 }, { 6, 4 }, { 5, 5 } };
			int x = Client.currentGameWidth - (stackTabs() ? 231 : 462);
			int y = Client.currentGameHeight - (stackTabs() ? 73 : 37);
			for (int tabIndex = 0; tabIndex < 14; tabIndex++) {
				if (Client.tabID == tabIndex) {
					redStones[4].drawSprite(x, y);
				}
				if (stackTabs()) {
					if (tabIndex != 6) {
						x += 33;
					} else if (tabIndex == 6) {
						y += 36;
						x = Client.currentGameWidth - 231;
					}
				} else {
					x += 33;
				}
			}
			x = Client.currentGameWidth - (stackTabs() ? 231 : 462);
			y = Client.currentGameHeight - (stackTabs() ? 73 : 37);
			for (int index = 0; index < 14; index++) {
				if (Client.tabInterfaceIDs[index] != -1) {
					sideIcons[index].drawSprite(x + sideIconOffsets[index][0], y + sideIconOffsets[index][1]);
				}
				if (stackTabs()) {
					if (index != 6) {
						x += 33;
					} else if (index == 6) {
						y += 36;
						x = Client.currentGameWidth - 231;
					}
				} else {
					x += 33;
				}
			}
		}

		if (openInterfaceID == 25650) {
			if (currentScreenMode == ScreenMode.FIXED) {
				grandExchangeSprite4.flashSprite(29, 37, 162 + (int) (50 * Math.sin(update_tick / 15.0)));
			} else {
				grandExchangeSprite4.flashSprite(currentGameWidth - 197,
						stackTabs() ? currentGameHeight - 341 : currentGameHeight - 305,
						162 + (int) (50 * Math.sin(update_tick / 15.0)));
			}
		}
	}

	public void writeBackgroundTexture(int j) {
		if (!lowMem) {
			if (Rasterizer.textureLastUsed[17] >= j) {
				Background background = Rasterizer.textures[17];
				int k = background.width * background.anInt1453 - 1;
				int j1 = background.width * tickDelta * 2;
				byte abyte0[] = background.palettePixels;
				byte abyte3[] = aByteArray912;
				for (int i2 = 0; i2 <= k; i2++)
					abyte3[i2] = abyte0[i2 - j1 & k];

				background.palettePixels = abyte3;
				aByteArray912 = abyte0;
				Rasterizer.requestTextureUpdate(17);
				anInt854++;
				if (anInt854 > 1235) {
					anInt854 = 0;
					stream.createFrame(226);
					stream.writeUnsignedByte(0);
					int l2 = stream.currentOffset;
					stream.writeWord(58722);
					stream.writeUnsignedByte(240);
					stream.writeWord((int) (Math.random() * 65536D));
					stream.writeUnsignedByte((int) (Math.random() * 256D));
					if ((int) (Math.random() * 2D) == 0)
						stream.writeWord(51825);
					stream.writeUnsignedByte((int) (Math.random() * 256D));
					stream.writeWord((int) (Math.random() * 65536D));
					stream.writeWord(7130);
					stream.writeWord((int) (Math.random() * 65536D));
					stream.writeWord(61657);
					stream.writeBytes(stream.currentOffset - l2);
				}
			}
			if (Rasterizer.textureLastUsed[24] >= j) {
				Background background_1 = Rasterizer.textures[24];
				int l = background_1.width * background_1.anInt1453 - 1;
				int k1 = (int) (background_1.width * tickDelta * 1);
				byte abyte1[] = background_1.palettePixels;
				byte abyte4[] = aByteArray912;
				for (int j2 = 0; j2 <= l; j2++)
					abyte4[j2] = abyte1[j2 - k1 & l];

				background_1.palettePixels = abyte4;
				aByteArray912 = abyte1;
				Rasterizer.requestTextureUpdate(24);
			}
			if (Rasterizer.textureLastUsed[59] >= j) {
				Background background_2 = Rasterizer.textures[59];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];

				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(59);
			}
			if (Rasterizer.textureLastUsed[34] >= j) {
				Background background_2 = Rasterizer.textures[34];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 1;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];

				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(34);
			}
			if (Rasterizer.textureLastUsed[40] >= j) {
				Background background_2 = Rasterizer.textures[40];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(40);
			}
			if (Rasterizer.textureLastUsed[25] >= j) {
				Background background_2 = Rasterizer.textures[25];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				//int l1 = (int) (background_2.width * tickDelta * 0.5);
				int l1 = (int)(background_2.width * tickDelta * 1);
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(25);
			}
			if (Rasterizer.textureLastUsed[56] >= j) {
				Background background_2 = Rasterizer.textures[56];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(56);
			}
			if (Rasterizer.textureLastUsed[54] >= j) {
				Background background_2 = Rasterizer.textures[54];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(54);
			}
			if (Rasterizer.textureLastUsed[53] >= j) {
				Background background_2 = Rasterizer.textures[53];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(53);
			}
			if (Rasterizer.textureLastUsed[91] >= j) {
				Background background_2 = Rasterizer.textures[91];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(91);
			}
			if (Rasterizer.textureLastUsed[49] >= j) {
				Background background_2 = Rasterizer.textures[49];
				int i1 = background_2.width * background_2.anInt1453 - 1;
				int l1 = background_2.width * tickDelta * 2;
				byte abyte2[] = background_2.palettePixels;
				byte abyte5[] = aByteArray912;
				for (int k2 = 0; k2 <= i1; k2++)
					abyte5[k2] = abyte2[k2 - l1 & i1];
				background_2.palettePixels = abyte5;
				aByteArray912 = abyte2;
				Rasterizer.requestTextureUpdate(49);
			}
		}
	}

	public void processMobChatText() {
		for (int i = -1; i < playerCount; i++) {
			int j;
			if (i == -1)
				j = maxPlayerCount;
			else
				j = playerIndices[i];
			Player player = players[j];
			if (player != null && player.textCycle > 0) {
				player.textCycle--;
				if (player.textCycle == 0)
					player.textSpoken = null;
			}
		}
		for (int k = 0; k < npcCount; k++) {
			int l = npcIndices[k];
			NPC npc = npcs[l];
			if (npc != null && npc.textCycle > 0) {
				npc.textCycle--;
				if (npc.textCycle == 0)
					npc.textSpoken = null;
			}
		}
	}

	public void calcCameraPos() {
		int i = x * 128 + 64;
		int j = y * 128 + 64;
		int k = getCenterHeight(plane, j, i) - height;
		if (xCameraPos < i) {
			xCameraPos += anInt1101 + ((i - xCameraPos) * angle) / 1000;
			if (xCameraPos > i)
				xCameraPos = i;
		}
		if (xCameraPos > i) {
			xCameraPos -= anInt1101 + ((xCameraPos - i) * angle) / 1000;
			if (xCameraPos < i)
				xCameraPos = i;
		}
		if (zCameraPos < k) {
			zCameraPos += anInt1101 + ((k - zCameraPos) * angle) / 1000;
			if (zCameraPos > k)
				zCameraPos = k;
		}
		if (zCameraPos > k) {
			zCameraPos -= anInt1101 + ((zCameraPos - k) * angle) / 1000;
			if (zCameraPos < k)
				zCameraPos = k;
		}
		if (yCameraPos < j) {
			yCameraPos += anInt1101 + ((j - yCameraPos) * angle) / 1000;
			if (yCameraPos > j)
				yCameraPos = j;
		}
		if (yCameraPos > j) {
			yCameraPos -= anInt1101 + ((yCameraPos - j) * angle) / 1000;
			if (yCameraPos < j)
				yCameraPos = j;
		}
		i = cinematicCamXViewpointLoc * 128 + 64;
		j = cinematicCamYViewpointLoc * 128 + 64;
		k = getCenterHeight(plane, j, i) - cinematicCamZViewpointLoc;
		int l = i - xCameraPos;
		int i1 = k - zCameraPos;
		int j1 = j - yCameraPos;
		int k1 = (int) Math.sqrt(l * l + j1 * j1);
		int l1 = (int) (Math.atan2(i1, k1) * 325.94900000000001D) & 0x7ff;
		int i2 = (int) (Math.atan2(l, j1) * -325.94900000000001D) & 0x7ff;
		if (l1 < 128)
			l1 = 128;
		if (l1 > 383)
			l1 = 383;
		if (yCameraCurve < l1) {
			yCameraCurve += anInt998 + ((l1 - yCameraCurve) * anInt999) / 1000;
			if (yCameraCurve > l1)
				yCameraCurve = l1;
		}
		if (yCameraCurve > l1) {
			yCameraCurve -= anInt998 + ((yCameraCurve - l1) * anInt999) / 1000;
			if (yCameraCurve < l1)
				yCameraCurve = l1;
		}
		int j2 = i2 - xCameraCurve;
		if (j2 > 1024)
			j2 -= 2048;
		if (j2 < -1024)
			j2 += 2048;
		if (j2 > 0) {
			xCameraCurve += anInt998 + (j2 * anInt999) / 1000;
			xCameraCurve &= 0x7ff;
		}
		if (j2 < 0) {
			xCameraCurve -= anInt998 + (-j2 * anInt999) / 1000;
			xCameraCurve &= 0x7ff;
		}
		int k2 = i2 - xCameraCurve;
		if (k2 > 1024)
			k2 -= 2048;
		if (k2 < -1024)
			k2 += 2048;
		if (k2 < 0 && j2 > 0 || k2 > 0 && j2 < 0)
			xCameraCurve = i2;
	}

	private void drawMenu(int xOffSet, int yOffSet) {
		int xPos = menuOffsetX - (xOffSet);
		int yPos = (-yOffSet) + menuOffsetY;
		int menuW = menuWidth;
		int menuH = menuHeight + 1;
		needDrawTabArea = true;
		inputTaken = true;
		tabAreaAltered = true;
		DrawingArea.drawBox(xPos, yPos, menuW, menuH, 0x5d5447);
		DrawingArea.drawBox(xPos + 1, yPos + 1, menuW - 2, 16, 0);
		DrawingArea.drawBoxOutline(xPos + 1, yPos + 18, menuW - 2, menuH - 19, 0);
		newBoldFont.drawBasicString("Choose Option", xPos + 3, yPos + 14, 0x5d5447, 0x000000);
		int mouseX = super.getMouseX() - (xOffSet);
		int mouseY = (-yOffSet) + super.getMouseY();
		for (int l1 = 0; l1 < menuActionRow; l1++) {
			int textY = yPos + 31 + (menuActionRow - 1 - l1) * 15;
			int disColor = 0xffffff;
			if (mouseX > xPos && mouseX < xPos + menuW && mouseY > textY - 13 && mouseY < textY + 3) {
				disColor = 0xffff00;
			}
			newBoldFont.drawBasicString(menuActionName[l1], xPos + 3, textY, disColor, 0x000000);
		}
	}

	private void addFriend(long l) {
		try {
			if (l == 0L)
				return;

			String username = StringUtils.fixName(StringUtils.nameForLong(l));

			if (username.equalsIgnoreCase(myPlayer.displayName)) {
				System.err.println("blocked from adding self as friend..");
				return;
			}
			if (friendsCount >= 100 && anInt1046 != 1) {
				pushMessage("Your friendlist is full.", 0, "");
				return;
			}
			if (friendsCount >= 200) {
				pushMessage("Your friendlist is full.", 0, "");
				return;
			}

			for (int i = 0; i < friendsCount; i++)
				if (friendsListAsLongs[i] == l) {

					return;
				}
			for (int j = 0; j < ignoreCount; j++)
				if (ignoreListAsLongs[j] == l) {
					pushMessage("Please remove " + username + " from your ignore list first", 0, "");
					return;
				}

			if (username.equals(myPlayer.displayName)) {
				return;
			} else {
				//friendsList[friendsCount] = s;
				//friendsListAsLongs[friendsCount] = l;
				//friendsNodeIDs[friendsCount] = 0;
				//friendsCount++;
				//needDrawTabArea = true;
				stream.createFrame(188);
				stream.writeQWord(l);
				return;
			}
		} catch (RuntimeException runtimeexception) {
			Signlink.reporterror("15283, " + (byte) 68 + ", " + l + ", " + runtimeexception.toString());
		}
		throw new RuntimeException();
	}

	public int getCenterHeight(int i, int j, int k) {
		int l = k >> 7;
		int i1 = j >> 7;
		if (l < 0 || i1 < 0 || l > 103 || i1 > 103)
			return 0;
		int j1 = i;
		if (j1 < 3 && (byteGroundArray[1][l][i1] & 2) == 2)
			j1++;
		int k1 = k & 0x7f;
		int l1 = j & 0x7f;
		int i2 = intGroundArray[j1][l][i1] * (128 - k1) + intGroundArray[j1][l + 1][i1] * k1 >> 7;
		int j2 = intGroundArray[j1][l][i1 + 1] * (128 - k1) + intGroundArray[j1][l + 1][i1 + 1] * k1 >> 7;
		return i2 * (128 - l1) + j2 * l1 >> 7;
	}

	public void resetLogout() {
	    logger.debug("being logged out.. from: "+new Throwable().getStackTrace()[1].toString());
	    logger.debug("Logging out..");
	    
	    // Reset ExpTracker on logout
	    if (ClientWindow.frame instanceof CustomGameFrame) {
	        CustomGameFrame customFrame = (CustomGameFrame) ClientWindow.frame;
	        ExpTrackerPanel tracker = customFrame.getExpTracker();
	        if (tracker != null) {
	            tracker.resetTracker();
	        }
	    }
		loggedIn = false;
		prayClicked = false;
		loginScreenState = LoginScreenState.LOGIN;
		firstLoginMessage = "";
		try {
			if (socketStream != null)
				socketStream.close();
		} catch (Exception _ex) {
		}
		socketStream = null;
		PacketLog.clear();
		if (entityTarget != null) {
			entityTarget.stop();
		}
		// myUsername = "";
		// myPassword = "";
		unlinkMRUNodes();
		scene.initToNull();
		for (int i = 0; i < 4; i++)
			collisionMaps[i].setDefault();
		System.gc();
		stopMidi();
		currentSong = -1;
		BroadcastManager.broadcasts = new Broadcast[1000];
		nextSong = -1;
		prevSong = 0;
		experienceCounter = 0;
		GameTimerHandler.getSingleton().stopAll();
		Preferences.save();
		setGameMode(ScreenMode.FIXED, false);
	}

	public void method45() {
		aBoolean1031 = true;
		for (int j = 0; j < 7; j++) {
			anIntArray1065[j] = -1;
			for (int k = 0; k < IDK.length; k++) {
				if (IDK.cache[k].nonSelectable || IDK.cache[k].bodyPartId != j + (aBoolean1047 ? 0 : 7))
					continue;
				anIntArray1065[j] = k;
				break;
			}
		}
	}

	private void method46(int i, Buffer stream) {
		while (stream.bitPosition + 21 < i * 8) {
			int k = stream.readBits(14);
			if (k == 16383)
				break;
			if (npcs[k] == null)
				npcs[k] = new NPC(); // Teleports works fine if we create a new npc?
			NPC npc = npcs[k];
			npcIndices[npcCount++] = k;
			npc.anInt1537 = update_tick;
			int l = stream.readBits(5);
			if (l > 15)
				l -= 32;
			int i1 = stream.readBits(5);
			if (i1 > 15)
				i1 -= 32;
			int j1 = stream.readBits(1);
			int npcId = stream.readBits(16);
			npc.npcPetType = stream.readBits(2);
			npc.desc = NpcDefinition.forID(npcId);
			int k1 = stream.readBits(1);
			if (k1 == 1)
				anIntArray894[anInt893++] = k;
			npc.size = npc.desc.size;
			npc.turnspeed = npc.desc.getDegreesToTurn;
			npc.walkanim = npc.desc.walkAnimation;
			npc.walkanim_b = npc.desc.rotate180Animation;
			npc.walkanim_l = npc.desc.rotate90RightAnimation;
			npc.walkanim_r = npc.desc.rotate90LeftAnimation;
			npc.readyanim = npc.desc.standAnimation;
			npc.runanim = npc.desc.field1914;
			npc.runanim_b = npc.desc.field1919;
			npc.runanim_r = npc.desc.field1938;
			npc.runanim_l = npc.desc.field1918;
			npc.crawlanim = npc.desc.field1920;
			npc.crawlanim_b = npc.desc.field1933;
			npc.crawlanim_l = npc.desc.field1922;
			npc.crawlanim_r = npc.desc.field1923;
			npc.face_x = 0;
			npc.face_y = 0;
			npc.setPos(myPlayer.waypoint_x[0] + i1, myPlayer.waypoint_y[0] + l, j1 == 1);
		}
		stream.finishBitAccess();
	}

	@Override
	public void processGameLoop() {
		if (rsAlreadyLoaded || loadingError || genericLoadingError)
			return;
		update_tick++;

		if (!loggedIn)
			processLoginScreenInput();
		else
			mainGameProcessor();

		processOnDemandQueue();
		   processSpriteQueue();
		 //method49();
		 //handleSounds();
	}
	
	public static void queueSpriteLoad(Runnable loadTask) {
	    spriteLoadQueue.offer(loadTask);
	}

	// Add this method  
	public static void processSpriteQueue() {
	    if (!loggedIn) {
	        return;
	    }
	    
	    Runnable task = spriteLoadQueue.poll();
	    if (task != null) {
	        try {
	            task.run();
	        } catch (Exception e) {
	            System.err.println("Sprite load error: " + e.getMessage());
	            e.printStackTrace();
	        }
	    }
	}
	public void method47(boolean flag) {
		if (myPlayer.x >> 7 == destX && myPlayer.y >> 7 == destY)
			destX = 0;
		int j = playerCount;
		if (flag)
			j = 1;
		for (int l = 0; l < j; l++) {
			Player player;
			long i1;
			if (flag) {
				player = myPlayer;
				i1 = (long) maxPlayerCount << 32;
			} else {
				player = players[playerIndices[l]];
				i1 = (long) playerIndices[l] << 32;
			}
			if (player == null || !player.isVisible())
				continue;
			player.aBoolean1699 = (lowMem && playerCount > 50 || playerCount > 200) && !flag
					&& player.secondaryanim == player.readyanim;
			int j1 = player.x >> 7;
			int k1 = player.y >> 7;
			if (j1 < 0 || j1 >= 104 || k1 < 0 || k1 >= 104)
				continue;
			if (player.aModel_1714 != null && update_tick >= player.anInt1707 && update_tick < player.anInt1708) {
				player.aBoolean1699 = false;
				player.anInt1709 = getCenterHeight(plane, player.y, player.x);
				scene.method286(plane, player.y, player, player.target_direction, player.anInt1722, player.x,
						player.anInt1709, player.anInt1719, player.anInt1721, i1, player.anInt1720);
				continue;
			}
			if ((player.x & 0x7f) == 64 && (player.y & 0x7f) == 64) {
				if (anIntArrayArray929[j1][k1] == anInt1265)
					continue;
				anIntArrayArray929[j1][k1] = anInt1265;
			}
			player.anInt1709 = getCenterHeight(plane, player.y, player.x);
			scene.method285(plane, player.target_direction, player.anInt1709, i1, player.y, 60, player.x, player,
					player.is_walking);
		}
	}

	private boolean promptUserForInput(RSInterface class9) {
		int j = class9.contentType;
		if (anInt900 == 2) {
			if (j == 201) {
				inputTaken = true;
				inputDialogState = 0;
				messagePromptRaised = true;
				promptInput = "";
				friendsListAction = 1;
				aString1121 = "Enter name of friend to add to list";
			}
			if (j == 202) {
				inputTaken = true;
				inputDialogState = 0;
				messagePromptRaised = true;
				promptInput = "";
				friendsListAction = 2;
				aString1121 = "Enter name of friend to delete from list";
			}
		}
		if (j == 205) {
			anInt1011 = 250;
			return true;
		}
		if (j == 501) {
			inputTaken = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 4;
			aString1121 = "Enter name of player to add to list";
		}
		if (j == 502) {
			inputTaken = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 5;
			aString1121 = "Enter name of player to delete from list";
		}
		if (j == 550) {
			inputTaken = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 6;
			aString1121 = "Enter the name of the chat you wish to join";
		}
		if (j == 554) {
			inputTaken = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 15;
			aString1121 = "Enter the name of the player that u want to join";
		}
		if (j >= 300 && j <= 313) {
			int k = (j - 300) / 2;
			int j1 = j & 1;
			int i2 = anIntArray1065[k];
			if (i2 != -1) {
				do {
					if (j1 == 0 && --i2 < 0)
						i2 = IDK.length - 1;
					if (j1 == 1 && ++i2 >= IDK.length)
						i2 = 0;
				} while (IDK.cache[i2].nonSelectable || IDK.cache[i2].bodyPartId != k + (aBoolean1047 ? 0 : 7));
				anIntArray1065[k] = i2;
				aBoolean1031 = true;
			}
		}
		if (j >= 314 && j <= 323) {
			int l = (j - 314) / 2;
			int k1 = j & 1;
			int j2 = anIntArray990[l];
			if (k1 == 0 && --j2 < 0)
				j2 = anIntArrayArray1003[l].length - 1;
			if (k1 == 1 && ++j2 >= anIntArrayArray1003[l].length)
				j2 = 0;
			anIntArray990[l] = j2;
			aBoolean1031 = true;
		}
		if (j == 324 && !aBoolean1047) {
			aBoolean1047 = true;
			method45();
		}
		if (j == 325 && aBoolean1047) {
			aBoolean1047 = false;
			method45();
		}
		if (j == 326) {
			stream.createFrame(101);
			stream.writeUnsignedByte(aBoolean1047 ? 0 : 1);
			for (int i1 = 0; i1 < 7; i1++)
				stream.writeUnsignedByte(anIntArray1065[i1]);

			for (int l1 = 0; l1 < 5; l1++)
				stream.writeUnsignedByte(anIntArray990[l1]);

			return true;
		}
		if (j == 613)
			canMute = !canMute;
		if (j >= 601 && j <= 612) {
			clearTopInterfaces();
			if (reportAbuseInput.length() > 0) {
				stream.createFrame(218);
				stream.writeQWord(StringUtils.longForName(reportAbuseInput));
				stream.writeUnsignedByte(j - 601);
				stream.writeUnsignedByte(canMute ? 1 : 0);
			}
		}
		return false;
	}

	public void method49(Buffer stream) {
		for (int j = 0; j < anInt893; j++) {
			int k = anIntArray894[j];
			Player player = players[k];
			int l = stream.readUnsignedByte();
			if ((l & 0x40) != 0)
				l += stream.readUnsignedByte() << 8;
			method107(l, k, stream, player);
		}
	}

	public void method50(int i, int k, int l, int i1, int j1) {
		long k1 = scene.method300(j1, l, i);
		if (k1 != 0) {
			int l1 = scene.method304(j1, l, i, k1);
			int k2 = l1 >> 6 & 3;
			int i3 = l1 & 0x1f;
			int k3 = k;
			if (k1 > 0)
				k3 = i1;
			int ai[] = minimapImage.myPixels;
			int k4 = 24624 + l * 4 + (103 - i) * 512 * 4;
			int i5 = ObjectKey.getObjectId(k1);
			ObjectDefinition class46_2 = ObjectDefinition.forID(i5);
			if (class46_2.mapSceneId != -1) {
				Sprite background_2 = mapScenes[class46_2.mapSceneId];
				if (background_2 != null) {
					int i6 = (class46_2.xLength * 4 - background_2.myWidth) / 2;
					int j6 = (class46_2.yLength * 4 - background_2.myHeight) / 2;
					background_2.drawSprite(48 + l * 4 + i6, 48 + (104 - i - class46_2.yLength) * 4 + j6);
				}
			} else {
				if (i3 == 0 || i3 == 2)
					if (k2 == 0) {
						ai[k4] = k3;
						ai[k4 + 512] = k3;
						ai[k4 + 1024] = k3;
						ai[k4 + 1536] = k3;
					} else if (k2 == 1) {
						ai[k4] = k3;
						ai[k4 + 1] = k3;
						ai[k4 + 2] = k3;
						ai[k4 + 3] = k3;
					} else if (k2 == 2) {
						ai[k4 + 3] = k3;
						ai[k4 + 3 + 512] = k3;
						ai[k4 + 3 + 1024] = k3;
						ai[k4 + 3 + 1536] = k3;
					} else if (k2 == 3) {
						ai[k4 + 1536] = k3;
						ai[k4 + 1536 + 1] = k3;
						ai[k4 + 1536 + 2] = k3;
						ai[k4 + 1536 + 3] = k3;
					}
				if (i3 == 3)
					if (k2 == 0)
						ai[k4] = k3;
					else if (k2 == 1)
						ai[k4 + 3] = k3;
					else if (k2 == 2)
						ai[k4 + 3 + 1536] = k3;
					else if (k2 == 3)
						ai[k4 + 1536] = k3;
				if (i3 == 2)
					if (k2 == 3) {
						ai[k4] = k3;
						ai[k4 + 512] = k3;
						ai[k4 + 1024] = k3;
						ai[k4 + 1536] = k3;
					} else if (k2 == 0) {
						ai[k4] = k3;
						ai[k4 + 1] = k3;
						ai[k4 + 2] = k3;
						ai[k4 + 3] = k3;
					} else if (k2 == 1) {
						ai[k4 + 3] = k3;
						ai[k4 + 3 + 512] = k3;
						ai[k4 + 3 + 1024] = k3;
						ai[k4 + 3 + 1536] = k3;
					} else if (k2 == 2) {
						ai[k4 + 1536] = k3;
						ai[k4 + 1536 + 1] = k3;
						ai[k4 + 1536 + 2] = k3;
						ai[k4 + 1536 + 3] = k3;
					}
			}
		}
		k1 = scene.method302(j1, l, i);
		if (k1 != 0) {
			int i2 = scene.method304(j1, l, i, k1);
			int l2 = i2 >> 6 & 3;
			int j3 = i2 & 0x1f;
			int l3 = ObjectKey.getObjectId(k1);
			ObjectDefinition class46_1 = ObjectDefinition.forID(l3);
			if (class46_1.mapSceneId != -1) {
				if (class46_1.mapSceneId < mapScenes.length) {
					Sprite background_1 = mapScenes[class46_1.mapSceneId];
					if (background_1 != null) {
						int j5 = (class46_1.xLength * 4 - background_1.myWidth) / 2;
						int k5 = (class46_1.yLength * 4 - background_1.myHeight) / 2;
						background_1.drawSprite(48 + l * 4 + j5, 48 + (104 - i - class46_1.yLength) * 4 + k5);
					}
				} else if (Configuration.developerMode) {
					System.err.println("Missing map scene: " + class46_1.mapSceneId);
				}
			} else if (j3 == 9) {
				int l4 = 0xeeeeee;
				if (k1 > 0)
					l4 = 0xee0000;
				int ai1[] = minimapImage.myPixels;
				int l5 = 24624 + l * 4 + (103 - i) * 512 * 4;
				if (l2 == 0 || l2 == 2) {
					ai1[l5 + 1536] = l4;
					ai1[l5 + 1024 + 1] = l4;
					ai1[l5 + 512 + 2] = l4;
					ai1[l5 + 3] = l4;
				} else {
					ai1[l5] = l4;
					ai1[l5 + 512 + 1] = l4;
					ai1[l5 + 1024 + 2] = l4;
					ai1[l5 + 1536 + 3] = l4;
				}
			}
		}
		k1 = scene.method303(j1, l, i);
		if (k1 != 0) {
			int j2 = ObjectKey.getObjectId(k1);
			ObjectDefinition class46 = ObjectDefinition.forID(j2);
			if (class46.mapSceneId != -1) {
				Sprite background = mapScenes[class46.mapSceneId];
				if (background != null) {
					int i4 = (class46.xLength * 4 - background.myWidth) / 2;
					int j4 = (class46.yLength * 4 - background.myHeight) / 2;
					background.drawSprite(48 + l * 4 + i4, 48 + (104 - i - class46.yLength) * 4 + j4);
				}
			}
		}
	}

	private Sprite aSprite_1201;
	private Sprite aSprite_1202;

	private void loadTitleScreen() {
	}

	private static void setHighMem() {
		WorldController.lowMem = false;
		 Rasterizer.lowMem = false;
		lowMem = false;
		ObjectManager.lowMem = false;
		ObjectDefinition.lowMem = false;
	}

	public static final String[] SKILL_NAME = { "Attack", "Defence", "Strength", "Hitpoints", "Ranged", "Prayer",
			"Magic", "Cooking", "Woodcutting", "Fletching", "Fishing", "Firemaking", "Crafting", "Smithing", "Mining",
			"Herblore", "Agility", "Thieving", "Slayer", "Farming", "Runecrafting", "Hunter", "Construction",
			"Summoning" };

	public static int getXPForLevel(int level) {
		int points = 0;
		int output = 0;
		for (int lvl = 1; lvl <= level; lvl++) {
			points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			if (lvl >= level) {
				return output;
			}
			output = (int) Math.floor(points / 4);
		}
		return 0;
	}

	/**
	 * An array containing all the player's experience.
	 */
	public static double[] experience;
	public static int totalExperience;

	public static int getStaticLevelByExperience(int slot) {
		double exp = experience[slot];

		int points = 0;
		int output = 0;
		for (byte lvl = 1; lvl < 100; lvl++) {
			points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			output = (int) Math.floor(points / 4);
			if ((output - 1) >= exp) {
				return lvl;
			}
		}
		return 99;
	}

	private static String[] args = null;
	public static Client instance;
	public static boolean runelite;

	private static int getVersion() {
		String version = System.getProperty("java.version");
		if(version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if(dot != -1) { version = version.substring(0, dot); }
		} return Integer.parseInt(version);
	}

	public static void main(String args[]) {
		System.out.println("Launching " + Configuration.CLIENT_TITLE + "..");
		getClient(false, args);
	}

	public static JFrame appFrame = null;
	public static Container gameContainer = null;

	private static String getErrorLogDirectory() {
		return Signlink.getCacheDirectory() + Configuration.ERROR_LOG_DIRECTORY;
	}

	private static void enableExceptionLogging() throws IOException {
		if (!new File(getErrorLogDirectory()).exists()) {
			Preconditions.checkState(new File(getErrorLogDirectory()).mkdirs());
		}

		TeeOutputStream outputStream = new TeeOutputStream(System.err, new FileOutputStream(getErrorLogDirectory() + Configuration.ERROR_LOG_FILE, true));
		System.setErr(new PrintStream(outputStream));
	}

	private static void setLoggingLevel(ch.qos.logback.classic.Level level) {
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.setLevel(level);
	}
	public static Client getClient() {
	    return getClient(false);
	}
	public static Client getClient(boolean runelite, String...args) {
		try {
			/*try {
				ClientDiscordRPC.initialize();
			} catch (Exception e) {
				System.err.println("Failed to establish Discord RPC.");
				e.printStackTrace();
			}*/

			System.out.println("Running Java version " + getVersion());
			Client.runelite = runelite;
			Client.args = args;
			StringBuilder windowTitleBuilder = new StringBuilder();
			windowTitleBuilder.append(Configuration.CLIENT_TITLE);
			server = Configuration.DEDICATED_SERVER_ADDRESS;
			setLoggingLevel(Level.INFO);

			if (args.length > 0) {
				StringBuilder configurationBuilder = new StringBuilder();
				configurationBuilder.append("[");

				for (int index = 0; index < args.length; index++) {
					configurationBuilder.append(args[index].replace("--", ""));
					if (index != args.length - 1)
						configurationBuilder.append(" ");

					if (args[index].startsWith("server=")) {
						server = args[index].replace("server=", "");
						System.out.println("Custom connection address set through run arguments: " + server);
					} else {
						switch (args[index]) {
							case "--debug":
							case "-db":
								setLoggingLevel(Level.DEBUG);
								System.out.println("Setting log level to debug.");
								break;

							case "--developer":
							case "-d":
								Configuration.developerMode = true;
								Configuration.cacheName = Configuration.CACHE_NAME_DEV;
								System.out.println("Developer mode enabled.");
								break;
							case "--local":
							case "-l":
							case "localhost":
							case "local":
								if (server.equals(Configuration.DEDICATED_SERVER_ADDRESS)) {
									server = "127.0.0.1";
									System.out.println("Localhost client enabled.");
								} else {
									throw new IllegalArgumentException("Cannot have custom ip and local enabled.");
								}
								break;
							case "test_server":
								port = Configuration.TEST_PORT;
								System.out.println("Test server enabled.");
								break;
							case "external_archives":
								Configuration.loadExternalCacheArchives = true;
								break;
							case "pack_data":
								Configuration.packIndexData = true;
								break;
							case "dump_maps":
								Configuration.dumpMaps = true;
								break;
							case "dump_animation_data":
								Configuration.dumpAnimationData = true;
								break;
							case "dump_data_lists":
								Configuration.dumpDataLists = true;
								break;
							default:
								throw new IllegalArgumentException("Run argument not recognized: " + args[index]);
						}
					}
				}

				// Build the client title with configuration arguments
				configurationBuilder.append("]");
				windowTitleBuilder.append(" ");
				windowTitleBuilder.append(configurationBuilder.toString().trim());
			}

			Signlink.createCacheDirectory();
			enableExceptionLogging(); // Don't remove this!
			Configuration.clientTitle = windowTitleBuilder.toString();
			nodeID = 1;
			portOff = 0;
			setHighMem();
			isMembers = true;
			Sprite.init();
			Signlink.storeid = 32;
			Signlink.startpriv(InetAddress.getLocalHost());
			if (runelite) {
				instance = new Client();
				gameContainer = instance;
			} else {
			    ClientWindow clientWindow = new ClientWindow(args);
			    instance = clientWindow.getClient(false);
			    
			    // Add debug logging
			    System.out.println("DEBUG: ClientWindow.getFrame() = " + ClientWindow.getFrame());
			    
			    // Fix: Create CustomGameFrame if it doesn't exist
			    if (ClientWindow.getFrame() == null) {
			        System.out.println("Creating CustomGameFrame for normal client launch");
			        CustomGameFrame customFrame = new CustomGameFrame(Configuration.clientTitle);
			        ClientWindow.frame = customFrame; // This sets the static appFrame
			        customFrame.setGameClient(clientWindow);
			        customFrame.setVisible(true);
			        
			        // Add more debug logging
			        System.out.println("DEBUG: CustomGameFrame created: " + customFrame);
			        System.out.println("DEBUG: ClientWindow.frame set to: " + ClientWindow.frame);
			    }
			    
			    appFrame = ClientWindow.getFrame();
			    gameContainer = ClientWindow.getFrame();
			    
			    // Final debug check
			    System.out.println("DEBUG: Final appFrame = " + appFrame);
			    System.out.println("DEBUG: Final gameContainer = " + gameContainer);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
		}

		return Client.instance;
	}

	@Override
	public void init() {
		try {
			initClientFrame(currentScreenMode.getWidth(), currentScreenMode.getHeight());
		} catch (Exception exception) {
			exception.printStackTrace();
		}
	}

	public static Client getInstance() {
		return instance;
	}

	public void loadingStages() {
		if (lowMem && loadingStage == 2 && ObjectManager.anInt131 != plane) {
			mainGameGraphicsBuffer.setCanvas();
			drawLoadingMessages(1, "Loading - please wait.", null);
			mainGameGraphicsBuffer.drawGraphics(0, super.graphics, 0);
			loadingStage = 1;
			longStartTime = System.currentTimeMillis();
		}
		if (loadingStage == 1) {
			int j = method54();
			if (j != 0 && System.currentTimeMillis() - longStartTime > 0x57e40L) {
				Signlink.reporterror(
						myUsername + " glcfb " + aLong1215 + "," + j + "," + lowMem + "," + decompressors[0] + ","
								+ onDemandFetcher.getNodeCount() + "," + plane + "," + currentRegionX + "," + currentRegionY);
				longStartTime = System.currentTimeMillis();
			}
		}
		if (loadingStage == 2 && plane != anInt985) {
			anInt985 = plane;
			method24(plane);
			stream.createFrame(121);
		}
	}

	private int method54() {
		for (int i = 0; i < terrainData.length; i++) {
			if (terrainData[i] == null && terrainIndices[i] != -1)
				return -1;
			if (objectData[i] == null && objectIndices[i] != -1)
				return -2;
		}
		boolean flag = true;
		for (int j = 0; j < terrainData.length; j++) {
			byte abyte0[] = objectData[j];
			if (abyte0 != null) {
				try {
					int k = (mapCoordinates[j] >> 8) * 64 - baseX;
					int l = (mapCoordinates[j] & 0xff) * 64 - baseY;
					if (isDynamicRegion) {
						k = 10;
						l = 10;
					}
					flag &= ObjectManager.method189(k, abyte0, l);
				} catch (Exception e) {
					if (objectIndices[j] != -1)
						System.err.println("Error on map file: " + objectIndices[j]);
					e.printStackTrace();
				}
			}
		}
		if (!flag)
			return -3;// couldn't parse all landscapes
		if (loadingMap) {
			return -4;
		} else {
			loadingStage = 2;
			ObjectManager.anInt131 = plane;
			loadRegion();
			stream.createFrame(121);
			logger.debug("Map region loaded.");
			return 0;
		}
	}

	public void method55() {
		for (Animable_Sub4 class30_sub2_sub4_sub4 = (Animable_Sub4) aClass19_1013
				.reverseGetFirst(); class30_sub2_sub4_sub4 != null; class30_sub2_sub4_sub4 = (Animable_Sub4) aClass19_1013
				.reverseGetNext())
			if (class30_sub2_sub4_sub4.anInt1597 != plane || update_tick > class30_sub2_sub4_sub4.anInt1572)
				class30_sub2_sub4_sub4.unlink();
			else if (update_tick >= class30_sub2_sub4_sub4.anInt1571) {
				if (class30_sub2_sub4_sub4.anInt1590 > 0) {
					NPC npc = npcs[class30_sub2_sub4_sub4.anInt1590 - 1];
					if (npc != null && npc.x >= 0 && npc.x < 13312 && npc.y >= 0 && npc.y < 13312)
						class30_sub2_sub4_sub4.method455(update_tick, npc.y,
								getCenterHeight(class30_sub2_sub4_sub4.anInt1597, npc.y, npc.x)
										- class30_sub2_sub4_sub4.anInt1583,
								npc.x);
				}
				if (class30_sub2_sub4_sub4.anInt1590 < 0) {
					int j = -class30_sub2_sub4_sub4.anInt1590 - 1;
					Player player;
					if (j == unknownInt10)
						player = myPlayer;
					else
						player = players[j];
					if (player != null && player.x >= 0 && player.x < 13312 && player.y >= 0 && player.y < 13312)
						class30_sub2_sub4_sub4.method455(update_tick, player.y,
								getCenterHeight(class30_sub2_sub4_sub4.anInt1597, player.y, player.x)
										- class30_sub2_sub4_sub4.anInt1583,
								player.x);
				}
				class30_sub2_sub4_sub4.method456(tickDelta);
				scene.method285(plane, class30_sub2_sub4_sub4.anInt1595,
						(int) class30_sub2_sub4_sub4.z, -1, (int) class30_sub2_sub4_sub4.y, 60,
						(int) class30_sub2_sub4_sub4.x, class30_sub2_sub4_sub4, false);
			}

	}

	@Override
	public AppletContext getAppletContext() {
		if (Signlink.mainapp != null)
			return Signlink.mainapp.getAppletContext();
		else
			return super.getAppletContext();
	}

	public static String capitalize(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (i == 0) {
				s = String.format("%s%s", Character.toUpperCase(s.charAt(0)), s.substring(1));
			}
			if (!Character.isLetterOrDigit(s.charAt(i))) {
				if (i + 1 < s.length()) {
					s = String.format("%s%s%s", s.subSequence(0, i + 1), Character.toUpperCase(s.charAt(i + 1)),
							s.substring(i + 2));
				}
			}
		}
		return s;
	}
	public void playSound(int id, int type, int delay, int volume) {
		sound[currentSound] = id;
		soundType[currentSound] = type;
		soundDelay[currentSound] = delay + Sounds.anIntArray326[id];
		soundVolume[currentSound] = volume;
		currentSound++;
	}
	public final void method90() {
		for (int index = 0; index < currentSound; index++) {
			if (soundDelay[index] <= 0) {
			boolean flag1 = false;
			try {
				Buffer stream = Sounds.method241(soundType[index], sound[index]);
				new SoundPlayer(new ByteArrayInputStream(stream.buffer, 0, stream.currentOffset), this.soundVolume[index], this.soundDelay[index]);
				if (System.currentTimeMillis() + (long) (stream.currentOffset / 22) > aLong1172 + (long) (anInt1257 / 22)) {
					anInt1257 = stream.currentOffset;
					aLong1172 = System.currentTimeMillis();
				}
			} catch (Exception exception) {
				exception.printStackTrace();
			}
			if (!flag1 || soundDelay[index] == -5) {
				currentSound--;
				for (int j = index; j < currentSound; j++) {
					sound[j] = sound[j + 1];
					soundType[j] = soundType[j + 1];
					soundDelay[j] = soundDelay[j + 1];
					soundVolume[j] = soundVolume[j + 1];
				}
				index--;
			} else {
				soundDelay[index] = -5;
			}
			} else {
				soundDelay[index]--;
			}
		}

		if (prevSong > 0) {
			prevSong -= 20;
			if (prevSong < 0)
				prevSong = 0;
			if (prevSong == 0 && musicEnabled) {
				nextSong = currentSong;
				songChanging = true;
				onDemandFetcher.provide(2, nextSong);
			}
		}
	}
	private void processOnDemandQueue() {
		do {
			OnDemandData onDemandData;
			do {
				onDemandData = onDemandFetcher.getNextNode();
				if (onDemandData == null)
					return;
				if (onDemandData.dataType == 0) {
					Model.loadModel(onDemandData.buffer, onDemandData.ID);
					needDrawTabArea = true;
					if (backDialogID != -1)
						inputTaken = true;
				}
				if (onDemandData.dataType == 1 && onDemandData.buffer != null) {
					int magic = (onDemandData.buffer[1] & 0xff) + ((onDemandData.buffer[0] & 0xff) << 8);
				if(magic == 420) {//load skeletal
					AnimKeyFrameSet.load(onDemandData.ID, onDemandData.buffer);
				} else if(magic == 710) {
					AnimFrameSet.load_osrs(onDemandData.ID, onDemandData.buffer);
				} /*else if (onDemandData.ID == 3366) {
						AnimFrameSet.load_641(onDemandData.ID, onDemandData.buffer);
					}*/
				//641 ANIMS GO HERE
			}
				if (onDemandData.dataType == 2 && onDemandData.ID == nextSong && onDemandData.buffer != null)
					saveMidi(songChanging, onDemandData.buffer);
				if (onDemandData.dataType == 3 && loadingStage == 1) {
					for (int i = 0; i < terrainData.length; i++) {
						if (terrainIndices[i] == onDemandData.ID) {
							if (terrainData[i] == null)
								terrainData[i] = onDemandData.buffer;
							if (onDemandData.buffer == null)
								terrainIndices[i] = -1;
							break;
						}
						if (objectIndices[i] != onDemandData.ID)
							continue;
						if (objectData[i] == null)
							objectData[i] = onDemandData.buffer;
						if (onDemandData.buffer == null)
							objectIndices[i] = -1;
						break;
					}

				}
				if (onDemandData.dataType == 5 && onDemandData.ID == nextSong && onDemandData.buffer != null)
					saveMidi(songChanging, onDemandData.buffer);
				if (onDemandData.dataType == 6) {
				    System.out.println("📦 Received sound data: ID=" + onDemandData.ID + " dataID=" + onDemandData.dataID + " dataType=" + onDemandData.dataType);
				    SoundRequestQueue.process(onDemandData);
				}
			} while (onDemandData.dataType != 93 || !onDemandFetcher.method564(onDemandData.ID));
			ObjectManager.passiveRequestGameObjectModels(new Buffer(onDemandData.buffer), onDemandFetcher);
		} while (true);
	}
	public void playJingle(int id) {
		if (id < 0) return;
		//expectedJingle = id;
		//onDemandFetcher.request(5, id); // Type 5 is music/jingle
	}
	public void calcFlamesPosition() {
		char c = '\u0100';
		for (int j = 10; j < 117; j++) {
			int k = (int) (Math.random() * 100D);
			if (k < 50)
				anIntArray828[j + (c - 2 << 7)] = 255;
		}
		for (int l = 0; l < 100; l++) {
			int i1 = (int) (Math.random() * 124D) + 2;
			int k1 = (int) (Math.random() * 128D) + 128;
			int k2 = i1 + (k1 << 7);
			anIntArray828[k2] = 192;
		}

		for (int j1 = 1; j1 < c - 1; j1++) {
			for (int l1 = 1; l1 < 127; l1++) {
				int l2 = l1 + (j1 << 7);
				anIntArray829[l2] = (anIntArray828[l2 - 1] + anIntArray828[l2 + 1] + anIntArray828[l2 - 128]
						+ anIntArray828[l2 + 128]) / 4;
			}

		}

		anInt1275 += 128;
		if (anInt1275 > anIntArray1190.length) {
			anInt1275 -= anIntArray1190.length;
			int i2 = (int) (Math.random() * 12D);
			randomizeBackground(aBackgroundArray1152s[i2]);
		}
		for (int j2 = 1; j2 < c - 1; j2++) {
			for (int i3 = 1; i3 < 127; i3++) {
				int k3 = i3 + (j2 << 7);
				int i4 = anIntArray829[k3 + 128] - anIntArray1190[k3 + anInt1275 & anIntArray1190.length - 1] / 5;
				if (i4 < 0)
					i4 = 0;
				anIntArray828[k3] = i4;
			}

		}

		System.arraycopy(anIntArray969, 1, anIntArray969, 0, c - 1);

		anIntArray969[c - 1] = (int) (Math.sin(update_tick / 14D) * 16D + Math.sin(update_tick / 15D) * 14D
				+ Math.sin(update_tick / 16D) * 12D);
		if (anInt1040 > 0)
			anInt1040 -= 4;
		if (anInt1041 > 0)
			anInt1041 -= 4;
		if (anInt1040 == 0 && anInt1041 == 0) {
			int l3 = (int) (Math.random() * 2000D);
			if (l3 == 0)
				anInt1040 = 1024;
			if (l3 == 1)
				anInt1041 = 1024;
		}
	}

	public void method60(int i) {
		RSInterface class9 = RSInterface.interfaceCache[i];
		for (int j = 0; j < class9.children.length; j++) {
			if (class9.children[j] == -1)
				break;
			RSInterface class9_1 = RSInterface.interfaceCache[class9.children[j]];
			if (class9_1.type == 1)
				method60(class9_1.id);
			class9_1.iftype_frameindex = 0;
			class9_1.iftype_loops_remaining = 0;
		}
	}

	public void drawHeadIcon() {
		if (hintType != 2)
			return;
		calcEntityScreenPos((hintIconXpos - baseX << 7) + anInt937, hintIconFloorPos * 2, (hintIconYpos - baseY << 7) + anInt938);
		if (spriteDrawX > -1 && update_tick % 20 < 10)
			headIconsHint[0].drawSprite(spriteDrawX - 12, spriteDrawY - 28);
	}

	private GameTimer gameTimer;

	private void mainGameProcessor() {
	    timeOutHasLoggedMessages();
	    updateGameMode();
	    updateScreenPreferences(currentScreenMode, new Dimension(currentGameWidth, currentGameHeight));

	    if (gameTimer != null) {
	        if (gameTimer.isCompleted()) {
	            gameTimer.stop();
	        }
	    }

	    if(getWidth() == 0 || getHeight() == 0) {
	        this.resize(currentGameWidth, currentGameHeight);
	        return;
	    }
	    
	    // Handle resize mode viewport updates
	    if (currentScreenMode != ScreenMode.FIXED && currentScreenMode != ScreenMode.FULLSCREEN) {
	        boolean resized = false;
	        
	        if (currentGameWidth != getWidth()) {
	            currentGameWidth = getWidth();
	            worldViewportWidth = getWidth();
	            graphics = getGraphics();
	            resized = true;
	        }
	        if (currentGameHeight != getHeight()) {
	            currentGameHeight = getHeight();
	            worldViewportHeight = getHeight();
	            graphics = getGraphics();
	            resized = true;
	        }
	        
	        // Update rendering parameters when resized
	        if (resized) {
	            updateGameScreen();
	            
	            // Update focal length for new viewport dimensions
	            int minDimension = Math.min(worldViewportWidth, worldViewportHeight);
	            WorldController.focalLength = minDimension / 2;
	            
	            // Update Rasterizer center points
	            Rasterizer.textureInt1 = worldViewportWidth / 2;
	            Rasterizer.textureInt2 = worldViewportHeight / 2;
	            
	            // Remove the calcEntityScreenPos() call - it's not needed here
	            // The visibility map will be recalculated automatically when needed
	        }
	    } else {
	        // Fixed mode - use default focal length
	        WorldController.focalLength = 512;
	        Rasterizer.textureInt1 = 256;
	        Rasterizer.textureInt2 = 167;
	    }
	    
	    spin();
	    if (anInt1104 > 1)
	        anInt1104--;
	    if (anInt1011 > 0)
	        anInt1011--;

	    for (int j = 0; j < 100; j++)
	        if (!parsePacket())
	            break;

	    if (!loggedIn)
	        return;
	        
	    if (anInt1016 > 0)
	        anInt1016--;
	        
	    if (super.keyArray[1] == 1 || super.keyArray[2] == 1 || super.keyArray[3] == 1 || super.keyArray[4] == 1)
	        aBoolean1017 = true;
	        
	    if (aBoolean1017 && anInt1016 <= 0) {
	        anInt1016 = 20;
	        aBoolean1017 = false;
	        stream.createFrame(86);
	        stream.writeWord(camAngleX);
	        stream.method432(viewRotation);
	    }
	    
	    if (super.awtFocus && !aBoolean954) {
	        aBoolean954 = true;
	        stream.createFrame(3);
	        stream.writeUnsignedByte(1);
	    }
	    
	    if (!super.awtFocus && aBoolean954) {
	        aBoolean954 = false;
	        stream.createFrame(3);
	        stream.writeUnsignedByte(0);
	    }
	    
	    loadingStages();
	    method115();
	    method90();
	    anInt1009++;
	    
	    if (anInt1009 > 750) {
	        logger.debug("Dropping client due to packets not being received.");
	        dropClient();
	    }
	    
	    method114();
	    method95();
	    processMobChatText();
	    tickDelta++;
	    
	    if (crossType != 0) {
	        crossIndex += 20;
	        if (crossIndex >= 400)
	            crossType = 0;
	    }
	    
	    if (atInventoryInterfaceType != 0) {
	        atInventoryLoopCycle++;
	        if (atInventoryLoopCycle >= 15) {
	            if (atInventoryInterfaceType == 2)
	                needDrawTabArea = true;
	            if (atInventoryInterfaceType == 3)
	                inputTaken = true;
	            atInventoryInterfaceType = 0;
	        }
	    }
	    
	    if (activeInterfaceType != 0) {
	        anInt989++;
	        if (super.getMouseX() > anInt1087 + 5 || super.getMouseX() < anInt1087 - 5 || 
	            super.getMouseY() > anInt1088 + 5 || super.getMouseY() < anInt1088 - 5)
	            aBoolean1242 = true;
	            
	        if (super.clickMode2 == 0) {
	            if (activeInterfaceType == 2)
	                needDrawTabArea = true;
	            if (activeInterfaceType == 3)
	                inputTaken = true;
	            activeInterfaceType = 0;
	            
	            if (aBoolean1242 && anInt989 >= getDragSetting(draggingItemInterfaceId)) {
	                lastActiveInvInterface = -1;
	                processRightClick();

	                // Bank interface dragging logic
	                if (Bank.isBankContainer(RSInterface.interfaceCache[draggingItemInterfaceId]) || 
	                    draggingItemInterfaceId == Bank.SEARCH_CONTAINER) {
	                    
	                    Point southWest, northEast;
	                    int xOffset = currentScreenMode == ScreenMode.FIXED ? 0 : (currentGameWidth / 2) - 356;
	                    int yOffset = currentScreenMode == ScreenMode.FIXED ? 0 : (currentGameHeight / 2) - 230;

	                    southWest = new Point(68 + xOffset, 75 + yOffset);
	                    northEast = new Point(457 + xOffset, 41 + yOffset);
	                    int[] slots = new int[9];
	                    
	                    for (int i = 0; i < slots.length; i++)
	                        slots[i] = (41 * i) + (int) southWest.getX();
	                        
	                    for (int i = 0; i < slots.length; i++) {
	                        if (super.getMouseX() >= slots[i] && super.getMouseX() <= slots[i] + 42 &&
	                            super.getMouseY() >= northEast.getY() && super.getMouseY() <= southWest.getY()) {

	                            if (draggingItemInterfaceId != Bank.SEARCH_CONTAINER) {
	                                RSInterface rsi = RSInterface.interfaceCache[58050 + i];
	                                if (rsi.isMouseoverTriggered) {
	                                    continue;
	                                }
	                            }

	                            if (Bank.getCurrentBankTab() == 0) {
	                                OptionalInt fromTabOptional = Arrays.stream(Bank.ITEM_CONTAINERS)
	                                    .filter(id -> draggingItemInterfaceId == id).findFirst();
	                                if (fromTabOptional.isPresent()) {
	                                    RSInterface fromTab = RSInterface.interfaceCache[fromTabOptional.getAsInt()];
	                                    RSInterface toTab = RSInterface.interfaceCache[Bank.ITEM_CONTAINERS[i]];
	                                    if (toTab.getInventoryContainerFreeSlots() > 0 && fromTab.id != toTab.id) {
	                                        RSInterface.insertInventoryItem(fromTab, itemDraggingSlot, toTab);
	                                    }
	                                }
	                            }

	                            if (draggingItemInterfaceId != Bank.SEARCH_CONTAINER) {
	                                stream.createFrame(214);
	                                stream.method433(draggingItemInterfaceId);
	                                stream.method424(0);
	                                stream.method433(itemDraggingSlot);
	                                stream.method431(1000 + i);
	                            } else {
	                                stream.createFrame(243);
	                                stream.writeWord(i);
	                                stream.writeWord(RSInterface.get(draggingItemInterfaceId).inventoryItemId[itemDraggingSlot]);
	                            }
	                            return;
	                        }
	                    }
	                }

	                // Handle inventory dragging
	                try {
	                    RSInterface class9 = RSInterface.interfaceCache[draggingItemInterfaceId];
	                    if (class9 != null) {
	                        if (mouseInvInterfaceIndex != itemDraggingSlot && lastActiveInvInterface == draggingItemInterfaceId) {
	                            int insertMode = 0;
	                            if (anInt913 == 1 && class9.contentType == 206)
	                                insertMode = 1;
	                            if (class9.inventoryItemId[mouseInvInterfaceIndex] <= 0)
	                                insertMode = 0;

	                            if (class9.aBoolean235) {
	                                int l2 = itemDraggingSlot;
	                                int l3 = mouseInvInterfaceIndex;
	                                class9.inventoryItemId[l3] = class9.inventoryItemId[l2];
	                                class9.inventoryAmounts[l3] = class9.inventoryAmounts[l2];
	                                class9.inventoryItemId[l2] = -1;
	                                class9.inventoryAmounts[l2] = 0;
	                            } else if (insertMode == 1) {
	                                int i3 = itemDraggingSlot;
	                                for (int i4 = mouseInvInterfaceIndex; i3 != i4; ) {
	                                    if (i3 > i4) {
	                                        class9.swapInventoryItems(i3, i3 - 1);
	                                        i3--;
	                                    } else if (i3 < i4) {
	                                        class9.swapInventoryItems(i3, i3 + 1);
	                                        i3++;
	                                    }
	                                }
	                            } else {
	                                class9.swapInventoryItems(itemDraggingSlot, mouseInvInterfaceIndex);
	                            }

	                            Bank.shiftTabs();
	                            stream.createFrame(214);
	                            stream.method433(draggingItemInterfaceId);
	                            stream.method424(insertMode);
	                            stream.method433(itemDraggingSlot);
	                            stream.method431(mouseInvInterfaceIndex);
	                            
	                        } else if (class9.allowInvDraggingToOtherContainers && lastActiveInvInterface != draggingItemInterfaceId) {
	                            if (lastActiveInvInterface != -1 && draggingItemInterfaceId != -1) {
	                                RSInterface draggingFrom = RSInterface.interfaceCache[draggingItemInterfaceId];
	                                RSInterface draggingTo = RSInterface.interfaceCache[lastActiveInvInterface];
	                                
	                                if (draggingTo != null && draggingFrom != null &&
	                                    draggingTo.allowInvDraggingToOtherContainers &&
	                                    draggingFrom.allowInvDraggingToOtherContainers) {
	                                    
	                                    int fromSlot = itemDraggingSlot;
	                                    int toSlot = mouseInvInterfaceIndex;
	                                    int insertMode = 0;
	                                    
	                                    if (anInt913 == 1 && class9.contentType == 206)
	                                        insertMode = 1;

	                                    if (insertMode == 1) {
	                                        if (draggingTo.getInventoryContainerFreeSlots() > 0) {
	                                            RSInterface.insertInventoryItem(draggingFrom, fromSlot, draggingTo, toSlot);
	                                        } else {
	                                            return;
	                                        }
	                                    } else {
	                                        RSInterface.swapInventoryItems(draggingFrom, fromSlot, draggingTo, toSlot);
	                                    }

	                                    stream.createFrame(242);
	                                    stream.writeWord(draggingTo.id);
	                                    stream.writeWord(draggingFrom.id);
	                                    stream.method424(insertMode);
	                                    stream.writeWord(fromSlot);
	                                    stream.writeWord(toSlot);
	                                }
	                            }
	                        }
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }

	            } else if ((anInt1253 == 1 || menuHasAddFriend(menuActionRow - 1)) && menuActionRow > 2)
	                determineMenuSize();
	            else if (menuActionRow > 0)
	                doAction(menuActionRow - 1);
	                
	            atInventoryLoopCycle = 10;
	            super.clickMode3 = 0;
	        }
	    }
	    
	    if (WorldController.anInt470 != -1) {
	        int k = WorldController.anInt470;
	        int k1 = WorldController.anInt471;
	        boolean flag = false;
	        
	        if (myPlayer.isAdminRights() && controlIsDown) {
	            teleport(baseX + k, baseY + k1);
	        } else {
	            flag = doWalkTo(0, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, k1, true, k);
	        }
	        
	        WorldController.anInt470 = -1;
	        if (flag) {
	            crossX = super.getSaveClickX();
	            crossY = super.getSaveClickY();
	            crossType = 1;
	            crossIndex = 0;
	        }
	    }
	    
	    if (super.clickMode3 == 1 && aString844 != null) {
	        aString844 = null;
	        inputTaken = true;
	        super.clickMode3 = 0;
	    }
	    
	    if (!processMenuClick()) {
	        processTabClick();
	        processMainScreenClick();
	        processChatModeClick();
	    }

	    if (super.clickMode2 == 1 || super.clickMode3 == 1)
	        mouseClickCount++;
	        
	    if (anInt1500 != 0 || anInt1044 != 0 || anInt1129 != 0) {
	        if (anInt1501 < 50 && !menuOpen) {
	            anInt1501++;
	            if (anInt1501 == 50) {
	                if (anInt1500 != 0) {
	                    inputTaken = true;
	                }
	                if (anInt1044 != 0) {
	                    needDrawTabArea = true;
	                }
	            }
	        }
	    } else if (anInt1501 > 0) {
	        anInt1501--;
	    }
	    
	    if (loadingStage == 2)
	        method108();
	        
	    if (loadingStage == 2 && inCutScene)
	        calcCameraPos();
	        
	    for (int i1 = 0; i1 < 5; i1++)
	        anIntArray1030[i1]++;
	        
	    method73();
	    super.idleTime++;
	    
	    if (super.idleTime > IDLE_TIME) {
	        anInt1011 = 250;
	        super.idleTime -= 500;
	        stream.createFrame(202);
	    }
	    
	    anInt1010++;
	    if (anInt1010 > 50)
	        stream.createFrame(0);

	    try {
	        if (socketStream != null && stream.currentOffset > 0) {
	            socketStream.queueBytes(stream.currentOffset, stream.buffer);
	            stream.currentOffset = 0;
	            anInt1010 = 0;
	        }
	    } catch (IOException _ex) {
	        dropClient();
	    } catch (Exception exception) {
	        resetLogout();
	        if (Configuration.developerMode) {
	            exception.printStackTrace();
	        }
	    }
	}
	public void method63() {
		SpawnedObject spawnedObject = (SpawnedObject) spawns.reverseGetFirst();
		for (; spawnedObject != null; spawnedObject = (SpawnedObject) spawns.reverseGetNext())
			if (spawnedObject.getLongetivity == -1) {
				spawnedObject.delay = 0;
				method89(spawnedObject);
			} else {
				spawnedObject.unlink();
			}

	}

	private void resetImageProducers() {
		if (loginScreenGraphicsBuffer != null)
			return;
		super.fullGameScreen = null;
		chatAreaGraphicsBuffer = null;

		tabAreaGraphicsBuffer = null;
		mainGameGraphicsBuffer = null;
		mapAreaGraphicsBuffer = null;
		leftSideFlame = new RSImageProducer(128, 265, getGameComponent());
		DrawingArea.setAllPixelsToZero();
		rightSideFlame = new RSImageProducer(128, 265, getGameComponent());
		DrawingArea.setAllPixelsToZero();
		loginScreenGraphicsBuffer = new RSImageProducer(currentGameWidth, currentGameHeight, getGameComponent());
		DrawingArea.setAllPixelsToZero();
		if (titleStreamLoader != null)
			welcomeScreenRaised = true;
	}

	/**
	 * Runescape Loading Bar
	 *
	 * @param percentage
	 * @param s
	 * @param downloadSpeed
	 * @param secondsRemaining
	 * @trees
	 */
	public Sprite logo, loginBackground;
	public void drawLoadingText(int percentage, String s) {
		anInt1079 = percentage;
		aString1049 = s;
		resetImageProducers();

		if (titleStreamLoader == null) {
			super.drawLoadingText(percentage,s,-1,-1);
			return;
		}

		loginScreenGraphicsBuffer.initDrawingArea();
		Sprite background = new Sprite("loginscreen/background");
		background.drawAdvancedSprite(0, 0);
		int x = 765 / 2 - 543 / 2;
		int y = 475 - 20 + 8;
		int width = 540;
		int height = 32;
		double offset = 5.43;
		new Sprite("loginscreen/general/emptybar").drawAdvancedSprite(765 / 2 - width / 2, y + 11 - height / 2);
		new Sprite("loginscreen/general/fullbar").drawAdvancedSprite(765 / 2 - width / 2, y + 11 - height / 2);
		new DrawingArea().drawAlphaGradient(x + ((int) Math.round(percentage * offset) / 2), y,
				width - ((int) Math.round(percentage * offset) / 2), height, 0x000000, 0x000000, 200);
		if (percentage >= 198) {
			newBoldFont.drawCenteredString("Finished loading " + Configuration.CLIENT_TITLE, (765 / 2), y + height / 2, 0xffffff, 1);
		} else {
			newBoldFont.drawCenteredString("Loading " + Configuration.CLIENT_TITLE + " - Please wait - " + (percentage / 2) + "%", (765 / 2),
					y + height / 2, 0xffffff, 1);
		}
		loginScreenGraphicsBuffer.drawGraphics(0, 0, super.graphics);
	}

	public void method65(int width, int height, int mouseX, int mouseY, RSInterface class9, int i1, boolean flag, int j1) {
		int anInt992;
		if (aBoolean972)
			anInt992 = 32;
		else
			anInt992 = 0;
		aBoolean972 = false;
		if (mouseX >= width && mouseX < width + 16 && mouseY >= i1 && mouseY < i1 + 16) {
			class9.scrollPosition -= mouseClickCount * 4;
			if (flag) {
				needDrawTabArea = true;
			}
		} else if (mouseX >= width && mouseX < width + 16 && mouseY >= (i1 + height) - 16 && mouseY < i1 + height) {
			class9.scrollPosition += mouseClickCount * 4;
			if (flag) {
				needDrawTabArea = true;
			}
		} else if (mouseX >= width - anInt992 && mouseX < width + 16 + anInt992 && mouseY >= i1 + 16 && mouseY < (i1 + height) - 16 && mouseClickCount > 0) {
			int l1 = ((height - 32) * height) / j1;
			if (l1 < 8)
				l1 = 8;
			int i2 = mouseY - i1 - 16 - l1 / 2;
			int j2 = height - 32 - l1;
			class9.scrollPosition = ((j1 - height) * i2) / j2;
			if (flag)
				needDrawTabArea = true;
			aBoolean972 = true;
		}
	}

	private boolean method66(long i, int j, int k) {
		try {
			int i1 = ObjectKey.getObjectId(i);
			int j1 = scene.method304(plane, k, j, i);
			if (j1 == -1)
				return false;
			int k1 = j1 & 0x1f;
			int l1 = j1 >> 6 & 3;
			if (k1 == 10 || k1 == 11 || k1 == 22) {
				ObjectDefinition class46 = ObjectDefinition.forID(i1);
				int i2;
				int j2;
				if (l1 == 0 || l1 == 2) {
					i2 = class46.xLength;
					j2 = class46.yLength;
				} else {
					i2 = class46.yLength;
					j2 = class46.xLength;
				}
				int k2 = class46.anInt768;
				if (l1 != 0)
					k2 = (k2 << l1 & 0xf) + (k2 >> 4 - l1);
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, j2, 0, i2, k2, j, false, k);
			} else {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], l1, 0, k1 + 1, 0, 0, j, false, k);
			}
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			return true;
		} catch (Exception e) {
			System.err.println("Error while walking.");
			e.printStackTrace();
			return false;
		}
	}

	private StreamLoader streamLoaderForName(int i, String archiveName) {
		byte abyte0[] = null;
		try {
			if (decompressors[0] != null) {
				abyte0 = decompressors[0].read(i);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (abyte0 != null) {
			StreamLoader streamLoader = new StreamLoader(abyte0, archiveName);
			return streamLoader;
		}
		return null;
	}

	public void sendStringAsLong(String string) {
		stream.createFrame(60);
		stream.writeUnsignedByte(1 + string.length());
		stream.writeString(string);
	}

	public void sendString(int identifier, String text) {
		text = identifier + "," + text;
		stream.createFrame(127);
		stream.writeUnsignedByte(text.length() + 1);
		stream.writeString(text);
	}

	public void dropClient() {
		if (anInt1011 > 0) {
			resetLogout();
			return;
		}
		logger.debug("Dropping client.");
		mainGameGraphicsBuffer.setCanvas();
		DrawingArea.fillPixels(2, 229, 39, 0xffffff, 2);
		DrawingArea.drawPixels(37, 3, 3, 0, 227);
		drawLoadingMessages(2, "Connection lost.", "Please wait - attempting to reestablish.");
		mainGameGraphicsBuffer.drawGraphics(0, super.graphics, 0);
		minimapState = 0;
		Preferences.save();
		destX = 0;
		RSSocket rsSocket = socketStream;
		loggedIn = false;
		loginFailures = 0;
		//login(myUsername, getPassword(), true);
		if (!loggedIn)
			resetLogout();
		try {
			rsSocket.close();
		} catch (Exception _ex) {
		}
	}

	public static String formatNumber(double number) {
		return NumberFormat.getInstance().format(number);
	}

	public int settings[];

	private void updateSettings() {
		settings[809] = Configuration.alwaysLeftClickAttack ? 1 : 0;
		settings[817] = Configuration.escapeCloseInterface ? 1 : 0;
	}

	public void onRealButtonClick(int buttonId) {
		Interfaces.taskInterface.actions.onButtonClick(buttonId);
	}

	private void doAction(int i) {
		if (i < 0)
			return;
		if (inputDialogState != 0 && inputDialogState != 3) {
			inputDialogState = 0;
			inputTaken = true;
		}
		int j = menuActionCmd2[i];
		int buttonPressed = menuActionCmd3[i];
		int l = menuActionID[i];

		int i1 = (int) menuActionCmd1[i];
		long keyLong = menuActionCmd1[i];

		switch (buttonPressed) {
			case 42522:
				if (currentScreenMode != ScreenMode.FIXED) {
					setConfigButton(i, true);
					setConfigButton(23003, false);
					setConfigButton(23005, false);
					setGameMode(ScreenMode.FIXED, true);

				}
				break;

			case 42523:
				if (currentScreenMode != ScreenMode.RESIZABLE) {
					setConfigButton(i, true);
					setConfigButton(23001, false);
					setConfigButton(23005, false);
					setGameMode(ScreenMode.RESIZABLE, true);
				}
				break;
		}

		if (buttonPressed == 941) {
			cameraZoom = 200;
		}
		if (buttonPressed == 942) {
			cameraZoom = 400;
		}
		if (buttonPressed == 943) {
			cameraZoom = 600;
		}
		if (buttonPressed == 944) {
			cameraZoom = 800;
		}
		if (buttonPressed == 945) {
			cameraZoom = 1000;
		}
		if (l >= 2000)
			l -= 2000;
		if (l == 1100) {
			RSInterface button = RSInterface.interfaceCache[buttonPressed];
			button.setMenuVisible(button.isMenuVisible() ? false : true);
		}
		if (l == 474) {
			drawExperienceCounter = !drawExperienceCounter;
		}

		if (l == 8992) {
			/**
			 * for drop interface
			 */

			String messageToSearch = lastViewedDropTable;

			if (messageToSearch == null) {
				return;
			}

			NPCDropInfo info = NPCDropInfo.getEntry(messageToSearch);

			if (info == null) {
				return;
			}
			stream.createFrame(177);
			stream.writeWord(info.npcIndex);
			return;
		}


		if (l == 475) {
			stream.createFrame(185);
			stream.writeWord(-1);
			experienceCounter = 0L;
		}
		if(l==852){//goon
			launchURL("https://explv.github.io/");
		}
		if (l == 1850) {
			stream.createFrame(185);
			stream.writeWord(5100);
		}
		if(l==855){//wiki search

			inputTaken = true;
			inputDialogState = 0;
			messagePromptRaised = true;
			promptInput = "";
			friendsListAction = 14;
			aString1121 = "Search the OSRS wiki (opens browser)";
		}
		if (l == 769) {
			RSInterface d = RSInterface.interfaceCache[j];
			RSInterface p = RSInterface.interfaceCache[buttonPressed];
			if (!d.dropdown.isOpen()) {
				if (p.dropdownOpen != null) {
					p.dropdownOpen.dropdown.setOpen(false);
				}
				p.dropdownOpen = d;
			} else {
				p.dropdownOpen = null;
			}
			d.dropdown.setOpen(!d.dropdown.isOpen());
		} else if (l == 770) {
			RSInterface d = RSInterface.interfaceCache[j];
			RSInterface p = RSInterface.interfaceCache[buttonPressed];
			if (i1 >= d.dropdown.getOptions().length)
				return;
			d.dropdown.setSelected(d.dropdown.getOptions()[i1]);
			d.dropdown.setOpen(false);
			d.dropdown.getMenuItem().select(i1, d);
			try {
				SettingsManager.saveSettings(Client.instance);
			} catch (IOException io) {
				io.printStackTrace();
			}
			p.dropdownOpen = null;
		}
		if (l == 850) {
			if (tabInterfaceIDs[tabID] == 17200) {
				stream.createFrame(185);
				stream.writeWord(5200 + buttonPressed);
			}
		}
		if (l == 661) { // intid, slot, itemid;
			stream.createFrame(232);
			stream.method432(1);
			stream.writeWord(i1);
		}
		if (l == 662) {
			stream.createFrame(232);
			stream.method432(2);
			stream.writeWord(i1);
		}
		if (l == 663) {
			stream.createFrame(232);
			stream.method432(3);
			stream.writeWord(i1);
		}
		if (l == 664) {
			stream.createFrame(232);
			stream.method432(4);
			stream.writeWord(i1);
		}

		switch (l) {
			case 1500: // Toggle quick prayers
				prayClicked = !prayClicked;
				stream.createFrame(185);
				stream.writeWord(5000);
				break;

			case 1506: // Select quick prayers
				stream.createFrame(185);
				stream.writeWord(5001);
				setTab(5);
				break;
		}
		if (l == 1200) {
			stream.createFrame(185);
			stream.writeWord(buttonPressed);
			RSInterface item = RSInterface.interfaceCache[buttonPressed];
			RSInterface menu = RSInterface.interfaceCache[item.mOverInterToTrigger];
			menu.setMenuItem(item.getMenuItem());
			menu.setMenuVisible(false);
		}
		if (l >= 1700 && l <= 1710) {
			RSInterface button = RSInterface.get(buttonPressed);
			if (button.buttonListener != null)
				button.buttonListener.accept(buttonPressed);
			// Button Click Packet
			if (button.newButtonClicking) {
				stream.createFrame(184);
				stream.writeWord(buttonPressed);
				onRealButtonClick(buttonPressed);
			} else {
				stream.createFrame(185);
				int offset = buttonPressed + (buttonPressed - 58030) * 10 + (l - 1700);
				stream.writeWord(offset);
				Bank.handleButton(offset);
			}
		}
		if (l == 300) {
			stream.createFrame(141);
			stream.method432(j);
			stream.writeWord(buttonPressed);
			stream.method432(i1);
			stream.writeDWord(modifiableXValue);
		}
		if (l == 291) {
			stream.createFrame(140);
			stream.writeWord(buttonPressed);
			stream.method433(i1);
			stream.method431(j);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 582) {
			NPC npc = npcs[i1];
			if (npc != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, npc.waypoint_y[0], false, npc.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(57);
				stream.method432(anInt1285);
				stream.method432(i1);
				stream.method431(anInt1283);
				stream.method432(anInt1284);
			}
		}
		if (l == 234) {
			boolean flag1 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag1)
				flag1 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(236);
			stream.method431(buttonPressed + baseY);
			stream.writeWord(i1);
			stream.method431(j + baseX);
		}
		if (l == 62 && method66(keyLong, buttonPressed, j)) {
			stream.createFrame(192);
			stream.writeWord(anInt1284);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
			stream.method433(buttonPressed + baseY);
			stream.method431(anInt1283);
			stream.method433(j + baseX);
			stream.writeWord(anInt1285);
		}
		if (l == 511) {
			boolean flag2 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag2)
				flag2 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(25);
			stream.method431(anInt1284);
			stream.method432(anInt1285);
			stream.writeWord(i1);
			stream.method432(buttonPressed + baseY);
			stream.method433(anInt1283);
			stream.writeWord(j + baseX);
		}
		if (l == 74) {
			stream.createFrame(122);
			stream.writeWord(buttonPressed);
			stream.writeWord(j);
			stream.writeWord(i1);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 315) {
			RSInterface class9 = RSInterface.interfaceCache[buttonPressed];
			boolean flag8 = true;
			if (class9.type == RSInterface.TYPE_CONFIG || class9.id == 50009) { // Placeholder toggle
				class9.active = !class9.active;
			} else if (class9.type == RSInterface.TYPE_CONFIG_HOVER) {
				RSInterface.handleConfigHover(class9);
			}
			if (class9.contentType > 0)
				flag8 = promptUserForInput(class9);
			if (flag8) {
				SettingsTabWidget.settings(buttonPressed);
				switch (buttonPressed) {
					case 23003:
						Configuration.alwaysLeftClickAttack = !Configuration.alwaysLeftClickAttack;
						// savePlayerData();
						updateSettings();
						break;
					case 23005:
						Configuration.hideCombatOverlay = !Configuration.hideCombatOverlay;
						// savePlayerData();
						updateSettings();
						break;
					case 37708:
						if (coloredItemColor != 0xffffff) {
							inputTaken = true;
							inputDialogState = 0;
							messagePromptRaised = true;
							promptInput = "";
							friendsListAction = 25;// 25 = enter item name
							aString1121 = "Enter an item name to set this color to. (Name must match exact)";
						} else {
							pushMessage("You must select a color first...", 0, "");
						}
						break;
					case 37710:
						inputTaken = true;
						inputDialogState = 0;
						messagePromptRaised = true;
						promptInput = "";
						friendsListAction = 26;// 25 = enter item name
						aString1121 = "Enter a minimum item value to display on the ground.";
						break;
					case 37706:
						Robot robot2;
						PointerInfo pointer2;
						pointer2 = MouseInfo.getPointerInfo();
						Point coord2 = pointer2.getLocation();
						try {
							robot2 = new Robot();
							coord2 = MouseInfo.getPointerInfo().getLocation();
							Color color = robot2.getPixelColor((int) coord2.getX(), (int) coord2.getY());
							String hex2 = String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
							int hex3 = Integer.parseInt(hex2, 16);
							// pushMessage("<shad="+hex3+">Yell Color set.</shad>", 0, "");
							coloredItemColor = hex3;
							RSInterface.interfaceCache[37707].message = "Current color chosen!";
							RSInterface.interfaceCache[37707].textColor = coloredItemColor;
						} catch (AWTException e) {
							e.printStackTrace();
						}
						break;

					case 62013:
						RSInterface input = RSInterface.interfaceCache[RSInterface.selectedItemInterfaceId + 1];
						RSInterface itemContainer = RSInterface.interfaceCache[RSInterface.selectedItemInterfaceId];
						if (RSInterface.selectedItemInterfaceId <= 0) {
							return;
						}
						if (input != null && itemContainer != null) {
							int amount = -1;
							try {
								amount = Integer.parseInt(input.message);
							} catch (NumberFormatException nfe) {
								pushMessage("The amount must be a non-negative numerical value.", 0, "");
								break;
							}
							if (itemContainer.itemSearchSelectedId < 0) {
								itemContainer.itemSearchSelectedId = 0;
							}
							if (itemContainer.itemSearchSelectedSlot < 0) {
								itemContainer.itemSearchSelectedSlot = 0;
							}
							stream.createFrame(124);
							stream.writeDWord(RSInterface.selectedItemInterfaceId);
							stream.writeDWord(itemContainer.itemSearchSelectedSlot);
							stream.writeDWord(itemContainer.itemSearchSelectedId - 1);
							stream.writeDWord(amount);
						}
						break;

					case 32013:
						RSInterface input2 = RSInterface.interfaceCache[RSInterface.selectedItemInterfaceId + 1];
						RSInterface itemContainer2 = RSInterface.interfaceCache[RSInterface.selectedItemInterfaceId];
						if (RSInterface.selectedItemInterfaceId <= 0) {
							return;
						}
						if (input2 != null && itemContainer2 != null) {
							int amount = -1;
							try {
								amount = Integer.parseInt(input2.message);
							} catch (NumberFormatException nfe) {
								pushMessage("The amount must be a non-negative numerical value.", 0, "");
								break;
							}
							if (itemContainer2.itemSearchSelectedId < 0) {
								itemContainer2.itemSearchSelectedId = 0;
							}
							if (itemContainer2.itemSearchSelectedSlot < 0) {
								itemContainer2.itemSearchSelectedSlot = 0;
							}
							stream.createFrame(124);
							stream.writeDWord(RSInterface.selectedItemInterfaceId);
							stream.writeDWord(itemContainer2.itemSearchSelectedSlot);
							stream.writeDWord(itemContainer2.itemSearchSelectedId - 1);
							stream.writeDWord(amount);
						}
						break;
					case 19144:
						sendFrame248(15106, 3213);
						method60(15106);
						inputTaken = true;
						break;
					default:
						if (RSInterface.get(buttonPressed) != null && RSInterface.get(buttonPressed).newButtonClicking) {
							stream.createFrame(184);
							stream.writeWord(buttonPressed);
						} else {
							RSInterface widget = RSInterface.get(buttonPressed);

							// Determine if a special attack button is being pressed
							if (widget.hasTooltip()) {
								String tooltip = widget.tooltip.toLowerCase();
								boolean isSpecialAttackButton = tooltip.contains("use") && tooltip.contains("special attack");
								if (isSpecialAttackButton) {
									// Send custom packet for special attacks
									stream.createFrame(209);
									stream.writeWord(buttonPressed);
									return;
								}
							}

							// If special attack button is not being pressed process normally
							stream.createFrame(185);
							stream.writeWord(buttonPressed);

							switch (buttonPressed) {
								case SettingsTabWidget.HIDE_LOCAL_PET_OPTIONS:
									SettingsTabWidget.toggleHidePetOption();
									break;
							}
						}

						if (buttonPressed >= 61101 && buttonPressed <= 61200) {
							int selected = buttonPressed - 61101;
							for (int ii = 0, slot = -1; ii < ItemDefinition.totalItems && slot < 100; ii++) {
								ItemDefinition def = ItemDefinition.forID(ii);

								if (def.name == null || def.certTemplateID == ii - 1 || def.certID == ii - 1
										|| RSInterface.interfaceCache[61254].message.length() == 0) {
									continue;
								}

								if (def.name.toLowerCase()
										.contains(RSInterface.interfaceCache[61254].message.toLowerCase())) {
									slot++;
								}

								if (slot != selected) {
									continue;
								}

								int id = def.id;
								long num = Long.valueOf(RSInterface.interfaceCache[61255].message.replaceAll(",", ""));

								if (num > Integer.MAX_VALUE) {
									num = Integer.MAX_VALUE;
								}

								stream.createFrame(149);
								stream.writeWord(id);
								stream.writeDWord((int) num);
								stream.writeUnsignedByte(variousSettings[1075]);
								break;
							}
						}

						break;

				}
			}
		}
		if (l == 561) {
			Player player = players[i1];
			if (player != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, player.waypoint_y[0], false,
						player.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				anInt1188 += i1;
				if (anInt1188 >= 90) {
					stream.createFrame(136);
					anInt1188 = 0;
				}
				stream.createFrame(128);
				stream.writeWord(i1);
			}
		}
		if (l == 745) {
			stream.createFrame(8);
			stream.writeDWord(i1);
		}
		if (l == 20) {
			NPC class30_sub2_sub4_sub1_sub1_1 = npcs[i1];
			if (class30_sub2_sub4_sub1_sub1_1 != null) {
		        lastAttackedNPC = class30_sub2_sub4_sub1_sub1_1;  // ADD THIS
		        lastNPCAttackTime = System.currentTimeMillis(); 
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub1_1.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub1_1.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(155);
				stream.method431(i1);
			}
		}
		if (l == 779) {
			Player class30_sub2_sub4_sub1_sub2_1 = players[i1];
			if (class30_sub2_sub4_sub1_sub2_1 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_1.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub2_1.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(153);
				stream.method431(i1);
			}
		}
		if (l == 516) {
			int y;
			int x;
			if (!this.menuOpen) {
				x = this.getSaveClickX() - 4;
				y = this.getSaveClickY() - 4;
			} else {
				x = j - 4;
				y = buttonPressed - 4;
			}
			if (getUserSettings().isAntiAliasing() == true) {
				x <<= 1;
				y <<= 1;
			}
			this.scene.method312(y, x);
		}
		if (l == 1062) {
			anInt924 += baseX;
			if (anInt924 >= 113) {
				stream.createFrame(183);
				stream.writeDWordBigEndian(0xe63271);
				anInt924 = 0;
			}
			method66(keyLong, buttonPressed, j);
			stream.createFrame(228);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
			stream.method432(buttonPressed + baseY);
			stream.writeWord(j + baseX);
		}
		if (l == 679 && !aBoolean1149) {
			stream.createFrame(40);
			stream.writeWord(buttonPressed);
			aBoolean1149 = true;
		}
		if (l == Autocast.AUTOCAST_MENU_ACTION_ID) {
			RSInterface button = RSInterface.get(buttonPressed);
			stream.createFrame(5);
			stream.writeWord(button.autocastSpellId);
			stream.writeByte(button.autocastDefensive ? 1 : 0);
			if (Configuration.developerMode) {
				System.out.println("Autocast: " + button.autocastSpellId + ", defensive: " + button.autocastDefensive);
			}
		}
		if (l == 431) {
			stream.createFrame(129);
			stream.method432(j);
			stream.writeWord(buttonPressed);
			stream.method432(i1);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 337 || l == 42 || l == 792 || l == 322) {
			String s = menuActionName[i];
			int k1 = s.indexOf("@whi@");
			if (k1 != -1) {
				long l3 = StringUtils.longForName(s.substring(k1 + 5).trim());
				if (l == 337)
					addFriend(l3);
				if (l == 42)
					addIgnore(l3);
				if (l == 792)
					delFriend(l3);
				if (l == 322)
					delIgnore(l3);
			}
		}
		if (l == 1337) { // Placeholders
			inputString = "::placeholder-" + j + "-" + i1;
			stream.createFrame(103);
			stream.writeUnsignedByte(inputString.length() - 1);
			stream.writeString(inputString.substring(2));
			inputString = "";
		}
		if (l == 53) {
			stream.createFrame(135);
			stream.method431(j);
			stream.method432(buttonPressed);
			stream.method431(i1);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 539) {
			stream.createFrame(16);
			stream.writeWord(i1);
			stream.method433(j);
			stream.method433(buttonPressed);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}

		if (l == 9111 || l == 9112 || l == 9115) {

			Broadcast bc = BroadcastManager.getCurrentBroadcast();

			if (bc == null) {
				System.err.println("No broadcast found for this msg..");
				return;
			}
			if (l == 9115) {
				BroadcastManager.removeIndex(bc.index);
				return;
			}
			stream.createFrame(199);
			stream.writeByte(bc.index);
			return;
		}

		if (l == 484 || l == 6 || l == 9501) {
			String s1 = menuActionName[i];
			int l1 = s1.indexOf("@whi@");
			if (l1 != -1) {
				s1 = s1.substring(l1 + 5).trim();
				String s7 = StringUtils.fixName(StringUtils.nameForLong(StringUtils.longForName(s1)));
				boolean flag9 = false;
				for (int j3 = 0; j3 < playerCount; j3++) {
					Player class30_sub2_sub4_sub1_sub2_7 = players[playerIndices[j3]];
					if (class30_sub2_sub4_sub1_sub2_7 == null || class30_sub2_sub4_sub1_sub2_7.displayName == null
							|| !class30_sub2_sub4_sub1_sub2_7.displayName.equalsIgnoreCase(s7))
						continue;
//					doWalkTo(2, myPlayer.smallX[0], myPlayer.smallY[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_7.smallY[0],
//							false, class30_sub2_sub4_sub1_sub2_7.smallX[0]);
					if (l == 484) {
						stream.createFrame(39);
						stream.method431(playerIndices[j3]);
					}
					if (l == 6 || l == 9501) {
						anInt1188 += i1;
						if (anInt1188 >= 90) {
							stream.createFrame(136);
							anInt1188 = 0;
						}
						stream.createFrame(128);
						stream.writeWord(playerIndices[j3]);
					}
					flag9 = true;
					break;
				}

				if (!flag9)
					pushMessage("Unable to find " + s7, 0, "");
			}
		}
		if (l == 870) {
			stream.createFrame(53);
			stream.writeWord(j);
			stream.method432(anInt1283);
			stream.method433(i1);
			stream.writeWord(anInt1284);
			stream.method431(anInt1285);
			stream.writeWord(buttonPressed);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 847) {
			stream.createFrame(87);
			stream.method432(i1);
			stream.writeWord(buttonPressed);
			stream.method432(j);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 626) {
			RSInterface class9_1 = RSInterface.interfaceCache[buttonPressed];
			spellSelected = 1;
			spellID = class9_1.id;
			anInt1137 = buttonPressed;
			spellUsableOn = class9_1.spellUsableOn;
			System.out.println("Spell id=" + spellID);
			itemSelected = 0;
			needDrawTabArea = true;
			spellID = class9_1.id;
			String s4 = class9_1.selectedActionName;
			if (s4.indexOf(" ") != -1)
				s4 = s4.substring(0, s4.indexOf(" "));
			String s8 = class9_1.selectedActionName;
			if (s8.indexOf(" ") != -1)
				s8 = s8.substring(s8.indexOf(" ") + 1);
			spellTooltip = s4 + " " + class9_1.spellName + " " + s8;
			// class9_1.sprite1.drawSprite(class9_1.anInt263, class9_1.anInt265,
			// 0xffffff);
			// class9_1.sprite1.drawSprite(200,200);
			// System.out.println("Sprite: " + class9_1.sprite1.toString());
			if (spellUsableOn == 16) {
				needDrawTabArea = true;
				tabID = 3;
				tabAreaAltered = true;
			}
			return;
		}
		if (l == 104) {
			RSInterface class9_1 = RSInterface.interfaceCache[buttonPressed];
			spellID = class9_1.id;
			if (!autocast) {
				autocast = true;
				autocastId = class9_1.id;
				stream.createFrame(185);
				stream.writeWord(class9_1.id);
			} else if (autocastId == class9_1.id) {
				autocast = false;
				autocastId = 0;
				stream.createFrame(185);
				stream.writeWord(6666); // reset server side
			} else if (autocastId != class9_1.id) {
				autocast = true;
				autocastId = class9_1.id;
				stream.createFrame(185);
				stream.writeWord(class9_1.id);
			}
		}
		if (l == 78) {
			stream.createFrame(117);
			stream.writeWord(buttonPressed);
			stream.writeWord(i1);
			stream.method431(j);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 27) {
			Player class30_sub2_sub4_sub1_sub2_2 = players[i1];
			if (class30_sub2_sub4_sub1_sub2_2 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_2.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub2_2.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				anInt986 += i1;
				if (anInt986 >= 54) {
					stream.createFrame(189);
					stream.writeUnsignedByte(234);
					anInt986 = 0;
				}
				stream.createFrame(73);
				stream.method431(i1);
			}
		}
		if (l == 213) {
			boolean flag3 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag3)
				flag3 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(79);
			stream.method431(buttonPressed + baseY);
			stream.writeWord(i1);
			stream.method432(j + baseX);
		}
		if (l == 632) {
			stream.createFrame(145);
			stream.method432(buttonPressed);
			stream.method432(j);
			stream.method432(i1);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 1050) {
			if (!runClicked) {
				runClicked = true;
				stream.createFrame(185);
				stream.writeWord(152);
			} else {
				runClicked = false;
				stream.createFrame(185);
				stream.writeWord(152);
			}
		}
		if (l == 1004) {
			if (tabInterfaceIDs[10] != -1) {
				needDrawTabArea = true;
				setSidebarInterface(14, 2449);
				tabID = 10;
				tabAreaAltered = true;
			}
		}
		if (l == 1003) {
			clanChatMode = 2;
			inputTaken = true;
		}
		if (l == 1002) {
			clanChatMode = 1;
			inputTaken = true;
		}
		if (l == 1001) {
			clanChatMode = 0;
			inputTaken = true;
		}
		if (l == 1000) {
			// chatButtonClickPosition = 4;
			chatTypeView = 11;
			inputTaken = true;
		}
		if (l == 999) {
			// chatButtonClickPosition = 0;
			chatTypeView = 0;
			inputTaken = true;
		}
		if (l == 998) {
			// chatButtonClickPosition = 1;
			chatTypeView = 5;
			inputTaken = true;
		}
		if (l == 1005) {
			// chatButtonClickPosition = 1;
			chatTypeView = 12;
			inputTaken = true;
		}
		if (l == 997) {
			publicChatMode = 3;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 996) {
			publicChatMode = 2;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 995) {
			publicChatMode = 1;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 994) {
			publicChatMode = 0;
			inputTaken = true;
		}
		if (l == 993) {
			// chatButtonClickPosition = 2;
			chatTypeView = 1;
			inputTaken = true;
		}
		if (l == 1006) {
			for (int index = 0; index < chatMessages.length; index++) {
				if (chatMessages[index] != null) {
					int type = chatTypes[index];
					if (type == 3 || type == 7 || type == 5 || type == 6) {
						chatMessages[index] = null;
						chatTypes[index] = 0;
						chatNames[index] = null;
					}
					/*
	chatMessages

	chatTypes
	3, 7, 5, 6,

	chatNames
	 */

				}
			}
			inputTaken = true;
		}
		if (l == 992) {
			privateChatMode = 2;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 991) {
			privateChatMode = 1;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 990) {
			privateChatMode = 0;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 989) {
			// chatButtonClickPosition = 3;
			chatTypeView = 2;
			inputTaken = true;
		}
		if (l == 987) {
			tradeMode = 2;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 986) {
			tradeMode = 1;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 985) {
			tradeMode = 0;
			stream.createFrame(95);
			stream.writeUnsignedByte(publicChatMode);
			stream.writeUnsignedByte(privateChatMode);
			stream.writeUnsignedByte(tradeMode);
			inputTaken = true;
		}
		if (l == 984) {
			// chatButtonClickPosition = 5;
			chatTypeView = 3;
			inputTaken = true;
		}
		if (l == 983) {
			duelMode = 2;
			inputTaken = true;
		}
		if (l == 982) {
			duelMode = 1;
			inputTaken = true;
		}
		if (l == 981) {
			duelMode = 0;
			inputTaken = true;
		}
		if (l == 980) {
			// chatButtonClickPosition = 6;
			chatTypeView = 4;
			inputTaken = true;
		}
		if (l == 493) {
			stream.createFrame(75);
			stream.method433(buttonPressed);
			stream.method431(j);
			stream.writeWord(i1);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 652) {
			boolean flag4 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag4)
				flag4 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(156);
			stream.method432(j + baseX);
			stream.method431(buttonPressed + baseY);
			stream.method433(i1);
		}
		if (l == 94) {
			boolean flag5 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag5)
				flag5 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(181);
			stream.method431(buttonPressed + baseY);
			stream.writeWord(i1);
			stream.method431(j + baseX);
			stream.method432(anInt1137);
		}
		// clan chat
		if (l == 647) {
			stream.createFrame(213);
			stream.writeWord(buttonPressed);
			stream.writeWord(j);
			switch (buttonPressed) {
				case 18304:
					if (j == 0) {
						inputTaken = true;
						inputDialogState = 0;
						messagePromptRaised = true;
						promptInput = "";
						friendsListAction = 8;
						aString1121 = "Enter your clan chat title";
					}
					break;
			}
		}
		switch (buttonPressed) {
			case 48612:
				inputTaken = true;
				inputDialogState = 0;
				messagePromptRaised = true;
				promptInput = "";
				friendsListAction = 12;
				aString1121 = "Enter the name of the item you want to lookup";
				break;
			case 48615:
				inputTaken = true;
				inputDialogState = 0;
				messagePromptRaised = true;
				promptInput = "";
				friendsListAction = 13;
				aString1121 = "Enter the name of the player you want to lookup";
				break;
		}
		if (l == 646) {
			Bank.handleButton(buttonPressed);
			stream.createFrame(185);
			stream.writeWord(buttonPressed);
			if (!clickConfigButton(buttonPressed)) {
				RSInterface class9_2 = RSInterface.interfaceCache[buttonPressed];
				if (!class9_2.ignoreConfigClicking) {
					if (class9_2.scripts != null && class9_2.scripts[0][0] == 5) {
						int i2 = class9_2.scripts[0][1];
						if (variousSettings[i2] != class9_2.anIntArray212[0]) {
							variousSettings[i2] = class9_2.anIntArray212[0];
							method33(i2);
							needDrawTabArea = true;
						}

						//System.out.println(class9_2.id + ", " + i2 + ", " + variousSettings[i2] + ", " + class9_2.anIntArray212[0]);
					}
					switch (buttonPressed) {
						// clan chat
						case 18129:
							if (RSInterface.interfaceCache[18135].message.toLowerCase().contains("join")) {
								inputTaken = true;
								inputDialogState = 0;
								messagePromptRaised = true;
								promptInput = "";
								friendsListAction = 6;
								aString1121 = "Enter the name of the chat you wish to join";
							} else {
								sendString(0, "");
							}
							break;

						case 18132:
							openInterfaceID = 18300;
							break;

						case 18527:
							inputTaken = true;
							inputDialogState = 0;
							messagePromptRaised = true;
							promptInput = "";
							friendsListAction = 9;
							aString1121 = "Enter a name to add";
							break;

						case 18528:
							inputTaken = true;
							inputDialogState = 0;
							messagePromptRaised = true;
							promptInput = "";
							friendsListAction = 10;
							aString1121 = "Enter a name to add";
							break;
					}
				}
			}
		}
		if (l == 225) {
		    NPC class30_sub2_sub4_sub1_sub1_2 = npcs[i1];  // Notice the _2
		    if (class30_sub2_sub4_sub1_sub1_2 != null) {
		        lastAttackedNPC = class30_sub2_sub4_sub1_sub1_2;  // Use _2
		        lastNPCAttackTime = System.currentTimeMillis();
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub1_2.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub1_2.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				anInt1226 += i1;
				if (anInt1226 >= 85) {
					stream.createFrame(230);
					stream.writeUnsignedByte(239);
					anInt1226 = 0;
				}
				stream.createFrame(17);
				stream.method433(i1);
			}
		}
		if (l == 965) {
		    NPC class30_sub2_sub4_sub1_sub1_3 = npcs[i1];  // _3
		    if (class30_sub2_sub4_sub1_sub1_3 != null) {
		        lastAttackedNPC = class30_sub2_sub4_sub1_sub1_3;  // Use _3
		        lastNPCAttackTime = System.currentTimeMillis();
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub1_3.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub1_3.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				anInt1134++;
				if (anInt1134 >= 96) {
					stream.createFrame(152);
					stream.writeUnsignedByte(88);
					anInt1134 = 0;
				}
				stream.createFrame(21);
				stream.writeWord(i1);
			}
		}
		if (l == 413) {
			NPC class30_sub2_sub4_sub1_sub1_4 = npcs[i1];
			if (class30_sub2_sub4_sub1_sub1_4 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub1_4.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub1_4.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(131);
				stream.method433(i1);
				stream.method432(anInt1137);
			}
		}
		/*
		 * if (l == 200) clearTopInterfaces();
		 */
		if (l == 200) {
			// Trading post interface won't close automatically, not sure why
			if (openInterfaceID != 48599 && openInterfaceID != 48598 && openInterfaceID != 48600)
				clearTopInterfaces();
			else {
				stream.createFrame(185);
				stream.writeWord(15333);
			}
		}

		if (l == 201) {
			for (int index = 0; index < GrandExchange.grandExchangeItemBoxIds.length; index++)
				if (buttonPressed == GrandExchange.grandExchangeItemBoxIds[index] + 1)
					openInterfaceID = GrandExchange.grandExchangeOfferStatusInterfaceIds[index];

			switch (buttonPressed) {

				case 25705:
				case 25557:
					openInterfaceID = 25000;
					break;

			}
		}

		if (l == 1025) {
			NPC class30_sub2_sub4_sub1_sub1_5 = npcs[i1];
			if (class30_sub2_sub4_sub1_sub1_5 != null) {
				NpcDefinition entityDef = class30_sub2_sub4_sub1_sub1_5.desc;
				if (entityDef != null) {
					if (entityDef.childrenIDs != null)
						entityDef = entityDef.method161();
					if (entityDef != null) {
						String s9 = null;
						if (entityDef.description != null)
							s9 = new String(entityDef.description);
						else if (entityDef.combatLevel == 0)
							s9 = "It's a " + entityDef.name + ".";
						stream.createFrame(6);
						stream.writeWord((int) entityDef.npcId);
						stream.writeWord(i1);
					}

					if (entityDef.combatLevel > 0) {
						stream.createFrame(137);
						stream.writeWord((int) i1);
					}
				}
			}
		}
		if (l == 900) {
			method66(keyLong, buttonPressed, j);
			stream.createFrame(252);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
			stream.method431(buttonPressed + baseY);
			stream.method432(j + baseX);
		}
		if (l == 412) {
		    NPC class30_sub2_sub4_sub1_sub1_6 = npcs[i1];  // _6
		    if (class30_sub2_sub4_sub1_sub1_6 != null) {
		        lastAttackedNPC = class30_sub2_sub4_sub1_sub1_6;  // Use _6
		        lastNPCAttackTime = System.currentTimeMillis();
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub1_6.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub1_6.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(72);
				stream.method432(i1);
			}
		}
		if (l == 365) {
			Player class30_sub2_sub4_sub1_sub2_3 = players[i1];
			if (class30_sub2_sub4_sub1_sub2_3 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_3.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub2_3.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(249);
				stream.method432(i1);
				stream.method431(anInt1137);
			}
		}
		if (l == 729) {
			Player class30_sub2_sub4_sub1_sub2_4 = players[i1];
			if (class30_sub2_sub4_sub1_sub2_4 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_4.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub2_4.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(39);
				stream.method431(i1);
			}
		}
		if (l == 577) {
			Player class30_sub2_sub4_sub1_sub2_5 = players[i1];
			if (class30_sub2_sub4_sub1_sub2_5 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_5.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub2_5.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(139);
				stream.method431(i1);
			}
		}
		if (l == 956 && method66(keyLong, buttonPressed, j)) {
			stream.createFrame(35);
			stream.method431(j + baseX);
			stream.method432(anInt1137);
			stream.method432(buttonPressed + baseY);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
		}
		if (l == 567) {
			boolean flag6 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag6)
				flag6 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(23);
			stream.method431(buttonPressed + baseY);
			stream.method431(i1);
			stream.method431(j + baseX);
		}
		if (l == 867) {
			if ((i1 & 3) == 0)
				anInt1175++;
			if (anInt1175 >= 59) {
				stream.createFrame(200);
				stream.createFrame(201);
				stream.writeWord(25501);
				anInt1175 = 0;
			}
			stream.createFrame(43);
			stream.method431(buttonPressed);
			stream.method432(i1);
			stream.method432(j);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 543) {
			stream.createFrame(237);
			stream.writeWord(j);
			stream.method432(i1);
			stream.writeWord(buttonPressed);
			stream.method432(anInt1137);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 606) {
			String s2 = menuActionName[i];
			int j2 = s2.indexOf("@whi@");
			if (j2 != -1)
				if (openInterfaceID == -1) {
					clearTopInterfaces();
					reportAbuseInput = s2.substring(j2 + 5).trim();
					canMute = false;
					for (int i3 = 0; i3 < RSInterface.interfaceCache.length; i3++) {
						if (RSInterface.interfaceCache[i3] == null || RSInterface.interfaceCache[i3].contentType != 600)
							continue;
						reportAbuseInterfaceID = openInterfaceID = RSInterface.interfaceCache[i3].parentID;
						break;
					}

				} else {
					pushMessage("Please close the interface you have open before using 'report abuse'", 0, "");
				}
		}
		if (l == 491) {
			Player class30_sub2_sub4_sub1_sub2_6 = players[i1];
			if (class30_sub2_sub4_sub1_sub2_6 != null) {
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub2_6.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub2_6.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				stream.createFrame(14);
				stream.method432(anInt1284);
				stream.writeWord(i1);
				stream.writeWord(anInt1285);
				stream.method431(anInt1283);
			}
		}
		if (l == 639) {
			String s3 = menuActionName[i];
			int k2 = s3.indexOf("@whi@");
			if (k2 != -1) {
				long l4 = StringUtils.longForName(s3.substring(k2 + 5).trim());
				int k3 = -1;
				for (int i4 = 0; i4 < friendsCount; i4++) {
					if (friendsListAsLongs[i4] != l4)
						continue;
					k3 = i4;
					break;
				}

				if (k3 != -1 && friendsNodeIDs[k3] > 0) {
					inputTaken = true;
					inputDialogState = 0;
					messagePromptRaised = true;
					promptInput = "";
					friendsListAction = 3;
					aLong953 = friendsListAsLongs[k3];
					aString1121 = "Enter message to send to " + friendsList[k3];
				}
			}
		}
		if (l == 1052) {
			stream.createFrame(185);
			stream.writeWord(154);
		}

		if (l == 1004) {
			if (tabInterfaceIDs[14] != -1) {
				needDrawTabArea = true;
				tabID = 14;
				tabAreaAltered = true;
			}
		}

		if (l == 454) {
			stream.createFrame(41);
			stream.writeWord(i1);
			stream.method432(j);
			stream.method432(buttonPressed);
			atInventoryLoopCycle = 0;
			atInventoryInterface = buttonPressed;
			atInventoryIndex = j;
			atInventoryInterfaceType = 2;
			if (RSInterface.interfaceCache[buttonPressed].parentID == openInterfaceID)
				atInventoryInterfaceType = 1;
			if (RSInterface.interfaceCache[buttonPressed].parentID == backDialogID)
				atInventoryInterfaceType = 3;
		}
		if (l == 1150) {
			viewRotation = 0;
			camAngleX = 140;

		}
		if (l == 1151) {
			viewRotation = 1000;
			camAngleX = 140;

		}
		if (l == 1152) {
			viewRotation = 1500;
			camAngleX = 140;

		}
		if (l == 1153) {
			viewRotation = 500;
			camAngleX = 140;

		}
		if (l == 478) {
		    NPC class30_sub2_sub4_sub1_sub1_7 = npcs[i1];  // _7
		    if (class30_sub2_sub4_sub1_sub1_7 != null) {
		        lastAttackedNPC = class30_sub2_sub4_sub1_sub1_7;  // Use _7
		        lastNPCAttackTime = System.currentTimeMillis();
				doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, class30_sub2_sub4_sub1_sub1_7.waypoint_y[0],
						false, class30_sub2_sub4_sub1_sub1_7.waypoint_x[0]);
				crossX = super.getSaveClickX();
				crossY = super.getSaveClickY();
				crossType = 2;
				crossIndex = 0;
				if ((i1 & 3) == 0)
					anInt1155++;
				if (anInt1155 >= 53) {
					stream.createFrame(85);
					stream.writeUnsignedByte(66);
					anInt1155 = 0;
				}
				stream.createFrame(18);
				stream.method431(i1);
			}
		}
		if (l == 113) {
			method66(keyLong, buttonPressed, j);
			stream.createFrame(70);
			stream.method431(j + baseX);
			stream.writeWord(buttonPressed + baseY);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
		}
		if (l == 872) {
			method66(keyLong, buttonPressed, j);
			stream.createFrame(234);
			stream.method433(j + baseX);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
			stream.method433(buttonPressed + baseY);
		}
		if (l == 502) {
			method66(keyLong, buttonPressed, j);
			stream.createFrame(132);
			stream.method433(j + baseX);
			stream.writeDWord(ObjectKey.getObjectId(keyLong));
			stream.method432(buttonPressed + baseY);
		}
		if(l == 1125)
		{
			ItemDefinition class8 = ItemDefinition.forID(i1);
			RSInterface class9_4 = RSInterface.interfaceCache[buttonPressed];
			String s5;
			if(class9_4 != null && class9_4.inventoryAmounts[j] >= 0x186a0)
				s5 = class9_4.inventoryAmounts[j] + " x " + class8.name;
			else
			if(class8.description != null)
				s5 = new String(class8.description);
			else
				s5 = "It's a " + class8.name + ".";
			pushMessage(s5, 0, "");
		}
		if (l == 1130) {
			RSInterface class9_4 = RSInterface.interfaceCache[buttonPressed];
			if (class9_4 != null) {
				class9_4.itemSearchSelectedId = class9_4.inventoryItemId[j];
				class9_4.itemSearchSelectedSlot = j;
				RSInterface.selectedItemInterfaceId = class9_4.id;
			}
		}
		if (l == 169) {
			if (buttonPressed == 58002) {
				anInt913 = 0;
				variousSettings[304] = 1;
				variousSettings[305] = 0;
			} else if (buttonPressed == 18929) {
				anInt913 = 1;
				variousSettings[304] = 0;
				variousSettings[305] = 1;
			} else {
				RSInterface button = RSInterface.get(buttonPressed);
				if (button.newButtonClicking) {
					stream.createFrame(184);
					stream.writeWord(buttonPressed);
					onRealButtonClick(buttonPressed);
				} else {
					stream.createFrame(185);
					stream.writeWord(buttonPressed);
					if (button.buttonListener != null)
						button.buttonListener.accept(buttonPressed);
					if (clientData) {
						pushMessage("Client side button id: " + buttonPressed);
					}
				}

				RSInterface class9_3 = RSInterface.interfaceCache[buttonPressed];
				if (!class9_3.ignoreConfigClicking) {
					if (class9_3.scripts != null && class9_3.scripts[0][0] == 5) {
						int l2 = class9_3.scripts[0][1];
						variousSettings[l2] = 1 - variousSettings[l2];
						method33(l2);
						needDrawTabArea = true;
					}
				}
			}
		}
		if (l == 447) {
			itemSelected = 1;
			anInt1283 = j;
			anInt1284 = buttonPressed;
			anInt1285 = i1;
			selectedItemName = ItemDefinition.forID(i1).name;
			spellSelected = 0;
			needDrawTabArea = true;
			return;
		}
		if (l == 1226) {
			int j1 = ObjectKey.getObjectId(keyLong);
			ObjectDefinition class46 = ObjectDefinition.forID(j1);
			String s10;
			if (class46.description != null)
				s10 = new String(class46.description);
			else
				s10 = "It's a " + class46.name + ".";
			stream.createFrame(146);
			stream.method433(x + baseX);
			stream.writeWord(j1);
			stream.method432(y + baseY);
		}
		if (l == 244) {
			boolean flag7 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, buttonPressed, false, j);
			if (!flag7)
				flag7 = doWalkTo(2, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 1, 0, 1, 0, buttonPressed, false, j);
			crossX = super.getSaveClickX();
			crossY = super.getSaveClickY();
			crossType = 2;
			crossIndex = 0;
			stream.createFrame(253);
			stream.method431(j + baseX);
			stream.method433(buttonPressed + baseY);
			stream.method432(i1);
		}
		if (l == 1448) {
			ItemDefinition itemDef_1 = ItemDefinition.forID(i1);
			String s6;
			if (itemDef_1.description != null)
				s6 = new String(itemDef_1.description);
			else
				s6 = "It's a " + itemDef_1.name + ".";
			pushMessage(s6, 0, "");
		}
		itemSelected = 0;
		spellSelected = 0;
		needDrawTabArea = true;

	}

	public static boolean removeRoofs = true, leftClickAttack = true, chatEffects = true, drawOrbs = true;

	private void setConfigButtonsAtStartup() {
		Preferences.getPreferences().updateClientConfiguration();
		setConfigButton(23101, drawOrbs);
		setConfigButton(23109, splitPrivateChat);
		setConfigButton(23107, chatEffects);
		setConfigButton(23103, informationFile.isRememberRoof() ? true : false);
		setConfigButton(23105, leftClickAttack);
		setConfigButton(23111, getUserSettings().isGameTimers());
		setConfigButton(23113, getUserSettings().isShowEntityTarget());
		setConfigButton(23115, informationFile.isRememberVisibleItemNames() ? true : false);
		setConfigButton(23118, shiftDrop ? true : false);
		setConfigButton(23001, true);
		setConfigButton(953, true);

		setDropDown(SettingsInterface.OLD_GAMEFRAME, getUserSettings().isOldGameframe());
		setDropDown(SettingsInterface.GAME_TIMERS, getUserSettings().isGameTimers());
		setDropDown(SettingsInterface.ANTI_ALIASING, getUserSettings().isAntiAliasing());
		setDropDown(SettingsInterface.GROUND_ITEM_NAMES, getUserSettings().isGroundItemOverlay());
		setDropDown(SettingsInterface.FOG, getUserSettings().isFog());
		setDropDown(SettingsInterface.SMOOTH_SHADING, getUserSettings().isSmoothShading());
		setDropDown(SettingsInterface.TILE_BLENDING, getUserSettings().isTileBlending());
		setDropDown(SettingsInterface.BOUNTY_HUNTER, getUserSettings().isBountyHunter());
		setDropDown(SettingsInterface.ENTITY_TARGET, getUserSettings().isShowEntityTarget());
		setDropDown(SettingsInterface.CHAT_EFFECT, getUserSettings().getChatColor());
		setDropDown(SettingsInterface.INVENTORY_MENU, getUserSettings().isInventoryContextMenu() ? 1 : 0);
		setDropDown(SettingsInterface.STRETCHED_MODE, getUserSettings().isStretchedMode());

		// Draw distance
		if (getUserSettings().getDrawDistance() < WorldController.MIN_FAR_Z || getUserSettings().getDrawDistance() > WorldController.MAX_FAR_Z) {
			getUserSettings().setDrawDistance(WorldController.MIN_FAR_Z);
		}
		setDropDown(SettingsInterface.DRAW_DISTANCE, getUserSettings().getDrawDistance() == 30 ? 0 : getUserSettings().getDrawDistance() == 40 ? 1
				: getUserSettings().getDrawDistance() == 50 ? 2 : getUserSettings().getDrawDistance() == 60 ? 3 : 4);
		WorldController.farZ = getUserSettings().getDrawDistance();

		setDropDown(SettingsInterface.PM_NOTIFICATION, Preferences.getPreferences().pmNotifications ? 0 : 1);

		/** Sets the brightness level **/
		RSInterface class9_2 = RSInterface.interfaceCache[910];
		if (class9_2.scripts != null && class9_2.scripts[0][0] == 5) {
			int i2 = class9_2.scripts[0][1];
			if (variousSettings[i2] != class9_2.anIntArray212[0]) {
				variousSettings[i2] = class9_2.anIntArray212[0];
				method33(i2);
				needDrawTabArea = true;
			}
		}
	}

	private boolean clickConfigButton(int i) {
		switch (i) {

			case 953:
				if (tabInterfaceIDs[11] != 23000) {
					tabInterfaceIDs[11] = 23000;
					setConfigButton(i, true);
					setConfigButton(959, false);
				}
				return true;

			case 959:
				if (tabInterfaceIDs[11] != 23100) {
					tabInterfaceIDs[11] = 23100;
					setConfigButton(i, true);
					setConfigButton(953, false);
				}
				return true;

			case 42502:
				setConfigButton(42502, true);
				setConfigButton(42503, false);
				setConfigButton(42504, false);
				setConfigButton(42505, false);
				break;
			case 42503:
				setConfigButton(42502, false);
				setConfigButton(42503, true);
				setConfigButton(42504, false);
				setConfigButton(42505, false);
				break;

			case 166026:
				return true;

			case 166027:
				pushMessage("Coming real soon",  0, "");
				return false;

			case 23005:
				pushMessage("Coming real soon",  0, "");
				return true;

			case 23101:
				setConfigButton(i, drawOrbs = !drawOrbs);
				return true;

			case 23109:
				setConfigButton(i, splitPrivateChat = !splitPrivateChat);
				return true;

			case 23103:
				setConfigButton(i, removeRoofs = !removeRoofs);
				informationFile.setRememberRoof(informationFile.isRememberRoof() ? false : true);
				try {
					informationFile.write();
					System.out.println("Written to: " + informationFile.isRememberRoof());
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;

			case 23105:
				setConfigButton(i, leftClickAttack = !leftClickAttack);
				return true;

			case 23107:
				setConfigButton(i, chatEffects = !chatEffects);
				return true;

			case 23111:
				//setConfigButton(i, getUserSettings().setGameTimers(!getUserSettings().isGameTimers());
				return true;

			case 23113:
				//setConfigButton(i, showEntityTarget = !showEntityTarget);
				return true;

			case 23118:
				setConfigButton(i, shiftDrop = !shiftDrop);
				return true;

			case 23115:
				//setConfigButton(i, groundItemsOn = !groundItemsOn);
				informationFile.setRememberRoof(informationFile.isRememberVisibleItemNames() ? false : true);
				try {
					informationFile.write();
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;

		}

		return false;
	}

	private void setDropDown(Setting setting, Object value) {
		RSInterface widget = RSInterface.interfaceCache[setting.getInterfaceId()];
		if (widget == null) {
			throw new NullPointerException("Widget is null: " + setting.getInterfaceId());
		}
		if (widget.dropdown == null) {
			throw new NullPointerException("Widget dropdown is null: " + setting.getInterfaceId());
		}
		if (widget.dropdown.getOptions() == null) {
			throw new NullPointerException("Widget dropdown options is null: " + setting.getInterfaceId());
		}
		if (value instanceof Boolean) {
			boolean choice = (Boolean) value;
			widget.dropdown.setSelected(widget.dropdown.getOptions()[choice ? 0 : 1]);
		} else if (value instanceof Integer) {
			int choice = (int) value;
			widget.dropdown.setSelected(widget.dropdown.getOptions()[choice]);
		}
	}

	private void setConfigButton(int interfaceFrame, boolean configSetting) {
		int config = configSetting ? 1 : 0;
		anIntArray1045[interfaceFrame] = config;
		if (variousSettings[interfaceFrame] != config) {
			variousSettings[interfaceFrame] = config;
			needDrawTabArea = true;
			if (dialogID != -1)
				inputTaken = true;
		}
	}

	public void method70() {
		anInt1251 = 0;
		int j = (myPlayer.x >> 7) + baseX;
		int k = (myPlayer.y >> 7) + baseY;
		if (j >= 3053 && j <= 3156 && k >= 3056 && k <= 3136)
			anInt1251 = 1;
		if (j >= 3072 && j <= 3118 && k >= 9492 && k <= 9535)
			anInt1251 = 1;
		if (anInt1251 == 1 && j >= 3139 && j <= 3199 && k >= 3008 && k <= 3062)
			anInt1251 = 0;
	}

	@Override
	public void run() {
	    if (drawFlames) {
	        drawFlames();
	    } else {
	        try {
	            super.run();
	        } catch (OutOfMemoryError e) {
	            ClientWindow.popupMessage("An error has occurred that caused the game to crash.",
	                    "Contact us in the Discord #support channel for help.");
	            Misc.dumpHeap(true);
	            e.printStackTrace();
	            throw e;
	        }
	    }

	    // Move GPU testing to AFTER the main client run() completes
	    if (!gpuTested && loggedIn) { // Only test after client is logged in and stable
	        System.out.println("Testing GPU after client initialization...");
	        try {
	            if (this instanceof Component) {
	                Container parent = ((Component) this).getParent();
	                while (parent != null && !(parent instanceof CustomGameFrame)) {
	                    parent = parent.getParent();
	                }
	                if (parent instanceof CustomGameFrame) {
	                    Rasterizer.initializeGPUPlugin((CustomGameFrame) parent);
	                }
	            }
	            Rasterizer.testGPUPlugin();
	            gpuTested = true;
	        } catch (Exception e) {
	            System.out.println("GPU plugin failed, continuing without it: " + e.getMessage());
	            gpuTested = true; // Don't keep retrying
	        }
	    }

	    Rasterizer.onFrameComplete();
	}

	private void build3dScreenMenu() {
		if (itemSelected == 0 && spellSelected == 0) {
			menuActionName[menuActionRow] = "Walk here";
			menuActionID[menuActionRow] = 516;
			menuActionCmd2[menuActionRow] = super.getMouseX();
			menuActionCmd3[menuActionRow] = super.getMouseY();
			menuActionRow++;
		}
		long j = -1;
		for (int k = 0; k < Model.objectsHovering; k++) {
			long l = Model.hoveringObjects[k];
			int i1 = ObjectKey.getObjectX(l);
			int j1 = ObjectKey.getObjectY(l);
			int k1 = ObjectKey.getObjectOpcode(l);
			int l1 = ObjectKey.getObjectId(l);
			int type = ObjectKey.getObjectType(l);
			if (l == j)
				continue;
			j = l;
			if (k1 == 2 && scene.method304(plane, i1, j1, l) >= 0) {
				ObjectDefinition class46 = ObjectDefinition.forID(l1);
				if (class46.childrenIDs != null) {
					class46 = class46.method580();
				}
				if (class46 == null) {
					continue;
				}
				if (class46.name == null) {
					continue;
				}
				if (itemSelected == 1) {
					menuActionName[menuActionRow] = "Use " + selectedItemName + " with @cya@" + class46.name;
					menuActionID[menuActionRow] = 62;
					menuActionCmd1[menuActionRow] = l;
					menuActionCmd2[menuActionRow] = i1;
					menuActionCmd3[menuActionRow] = j1;
					menuActionRow++;
				} else if (spellSelected == 1) {
					if ((spellUsableOn & 4) == 4) {
						menuActionName[menuActionRow] = spellTooltip + " @cya@" + class46.name;
						menuActionID[menuActionRow] = 956;
						menuActionCmd1[menuActionRow] = l;
						menuActionCmd2[menuActionRow] = i1;
						menuActionCmd3[menuActionRow] = j1;
						menuActionRow++;
					}
				} else {
					if (class46.actions != null) {
						for (int i2 = 4; i2 >= 0; i2--)
							if (class46.actions[i2] != null) {
								menuActionName[menuActionRow] = class46.actions[i2] + " @cya@" + class46.name;
								if (i2 == 0)
									menuActionID[menuActionRow] = 502;
								if (i2 == 1)
									menuActionID[menuActionRow] = 900;
								if (i2 == 2)
									menuActionID[menuActionRow] = 113;
								if (i2 == 3)
									menuActionID[menuActionRow] = 872;
								if (i2 == 4)
									menuActionID[menuActionRow] = 1062;
								menuActionCmd1[menuActionRow] = l;
								menuActionCmd2[menuActionRow] = i1;
								menuActionCmd3[menuActionRow] = j1;
								menuActionRow++;
							}

					}
					if (myPlayer.isAdminRights())
						menuActionName[menuActionRow] = "Examine @cya@" + class46.name + " @whi@(" + l1 + ") ("
								+ (i1 + baseX) + "," + (j1 + baseY) + ")";
					else
						menuActionName[menuActionRow] = "Examine @cya@" + class46.name;
					if (debugModels == true) {
						menuActionName[menuActionRow] = "Examine @cya@" + class46.name + " @gre@ID: @whi@" + l1
								+ " @gre@X, Y: @whi@" + (i1 + baseX) + "," + (j1 + baseY) + " @gre@Models: @whi@"
								+ Arrays.toString(class46.modelIds);

					}

					menuActionID[menuActionRow] = 1226;
					menuActionCmd1[menuActionRow] = l;
					menuActionCmd2[menuActionRow] = i1;
					menuActionCmd3[menuActionRow] = j1;
					menuActionRow++;
				}
			}
			if (k1 == 1) {
				NPC npc = npcs[l1];
				if (npc.desc.size == 1 && (npc.x & 0x7f) == 64 && (npc.y & 0x7f) == 64) {
					for (int j2 = 0; j2 < npcCount; j2++) {
						NPC npc2 = npcs[npcIndices[j2]];
						if (npc2 != null && npc2 != npc && npc2.desc.size == 1 && npc2.x == npc.x && npc2.y == npc.y && npc2.isShowMenuOnHover()) {
							buildAtNPCMenu(npc2.desc, npcIndices[j2], j1, i1);
						}
					}

					for (int l2 = 0; l2 < playerCount; l2++) {
						Player player = players[playerIndices[l2]];
						if (player != null && player.x == npc.x && player.y == npc.y) {
							buildAtPlayerMenu(i1, playerIndices[l2], player, j1);
						}
					}

				}
				if (npc.isShowMenuOnHover()) {
					buildAtNPCMenu(npc.desc, l1, j1, i1);
				}
			}
			if (k1 == 0) {
				Player player = players[l1];
				if ((player.x & 0x7f) == 64 && (player.y & 0x7f) == 64) {
					for (int k2 = 0; k2 < npcCount; k2++) {
						NPC class30_sub2_sub4_sub1_sub1_2 = npcs[npcIndices[k2]];
						if (class30_sub2_sub4_sub1_sub1_2 != null && class30_sub2_sub4_sub1_sub1_2.desc.size == 1
								&& class30_sub2_sub4_sub1_sub1_2.x == player.x
								&& class30_sub2_sub4_sub1_sub1_2.y == player.y && class30_sub2_sub4_sub1_sub1_2.isShowMenuOnHover())
							buildAtNPCMenu(class30_sub2_sub4_sub1_sub1_2.desc, npcIndices[k2], j1, i1);
					}

					for (int i3 = 0; i3 < playerCount; i3++) {
						Player class30_sub2_sub4_sub1_sub2_2 = players[playerIndices[i3]];
						if (class30_sub2_sub4_sub1_sub2_2 != null && class30_sub2_sub4_sub1_sub2_2 != player
								&& class30_sub2_sub4_sub1_sub2_2.x == player.x
								&& class30_sub2_sub4_sub1_sub2_2.y == player.y)
							buildAtPlayerMenu(i1, playerIndices[i3], class30_sub2_sub4_sub1_sub2_2, j1);
					}

				}
				buildAtPlayerMenu(i1, l1, player, j1);
			}
			if (k1 == 3) {
				NodeList class19 = groundItems[plane][i1][j1];
				if (class19 != null) {
					for (Item item = (Item) class19.getFirst(); item != null; item = (Item) class19.getNext()) {
						ItemDefinition itemDef = ItemDefinition.forID(item.ID);
						if (itemSelected == 1) {
							menuActionName[menuActionRow] = "Use " + selectedItemName + " with @lre@" + itemDef.name;
							menuActionID[menuActionRow] = 511;
							menuActionCmd1[menuActionRow] = item.ID;
							menuActionCmd2[menuActionRow] = i1;
							menuActionCmd3[menuActionRow] = j1;
							menuActionRow++;
						} else if (spellSelected == 1) {
							if ((spellUsableOn & 1) == 1) {
								menuActionName[menuActionRow] = spellTooltip + " @lre@" + itemDef.name + "";
								menuActionID[menuActionRow] = 94;
								menuActionCmd1[menuActionRow] = item.ID;
								menuActionCmd2[menuActionRow] = i1;
								menuActionCmd3[menuActionRow] = j1;
								menuActionRow++;
							}
						} else {
							for (int j3 = 4; j3 >= 0; j3--)
								if (itemDef.groundActions != null && itemDef.groundActions[j3] != null) {
									menuActionName[menuActionRow] = itemDef.groundActions[j3] + " @lre@" + itemDef.name;
									if (j3 == 0)
										menuActionID[menuActionRow] = 652;
									if (j3 == 1)
										menuActionID[menuActionRow] = 567;
									if (j3 == 2)
										menuActionID[menuActionRow] = 234;
									if (j3 == 3)
										menuActionID[menuActionRow] = 244;
									if (j3 == 4)
										menuActionID[menuActionRow] = 213;
									menuActionCmd1[menuActionRow] = item.ID;
									menuActionCmd2[menuActionRow] = i1;
									menuActionCmd3[menuActionRow] = j1;
									menuActionRow++;
								} else if (j3 == 2) {
									menuActionName[menuActionRow] = "Take @lre@" + itemDef.name;
									menuActionID[menuActionRow] = 234;
									menuActionCmd1[menuActionRow] = item.ID;
									menuActionCmd2[menuActionRow] = i1;
									menuActionCmd3[menuActionRow] = j1;
									menuActionRow++;
								}
							menuActionName[menuActionRow] = "Examine @lre@" + itemDef.name;
							if (debugModels == true) {
								menuActionName[menuActionRow] = "Examine @lre@" + itemDef.name + " @gre@(@whi@"
										+ item.ID + "@gre@)";
							}
							menuActionID[menuActionRow] = 1125;
							menuActionCmd1[menuActionRow] = item.ID;
							menuActionCmd2[menuActionRow] = i1;
							menuActionCmd3[menuActionRow] = j1;
							menuActionRow++;
						}
					}
				}
			}
		}
	}

	@Override
	public void cleanUpForQuit() {
		Signlink.reporterror = false;
		try {
			if (socketStream != null)
				socketStream.close();
		} catch (Exception _ex) {
		}
		socketStream = null;
		stopMidi();
		mapBack = null;
		cacheSprite = null;
		cacheSprite1 = null;
		cacheSprite2 = null;
		cacheSprite3 = null;
		cacheSprite4 = null;
		cacheInterface = null;
		mouseDetection = null;
		onDemandFetcher.disable();
		mapIcon7 = null;
		mapIcon8 = null;
		mapIcon6 = null;
		mapIcon5 = null;
		mapIcon9 = null;
		onDemandFetcher = null;
		aStream_834 = null;
		loginScreenGraphicsBuffer = null;
		stream = null;
		aStream_847 = null;
		inStream = null;
		mapCoordinates = null;
		terrainData = null;
		objectData = null;
		terrainIndices = null;
		objectIndices = null;
		intGroundArray = null;
		byteGroundArray = null;
		scene = null;
		collisionMaps = null;
		anIntArrayArray901 = null;
		anIntArrayArray825 = null;
		bigX = null;
		bigY = null;
		aByteArray912 = null;
		tabAreaGraphicsBuffer = null;
		mapAreaGraphicsBuffer = null;
		mainGameGraphicsBuffer = null;
		loginScreenGraphicsBuffer = null;
		chatAreaGraphicsBuffer = null;

		tabAreaFixed = null;
		tabAreaResizable = null;
		compassImage = null;
		redStones = null;
		sideIcons = null;
		mapArea = null;

		/* Null pointers for custom sprites */
		multiOverlay = null;
		chatArea = null;
		chatButtons = null;
		mapArea = null;
		channelButtons = null;
		venomOrb = null;
		/**/

		hitMarks = null;
		headIcons = null;
		skullIcons = null;
		headIconsHint = null;
		crosses = null;
		mapDotItem = null;
		mapDotNPC = null;
		mapDotPlayer = null;
		mapDotFriend = null;
		mapScenes = null;
		mapFunctions = null;
		wiki = null;
		anIntArrayArray929 = null;
		players = null;
		playerIndices = null;
		anIntArray894 = null;
		aStreamArray895s = null;
		anIntArray840 = null;
		npcs = null;
		npcIndices = null;
		groundItems = null;
		spawns = null;
		aClass19_1013 = null;
		aClass19_1056 = null;
		menuActionCmd2 = null;
		menuActionCmd3 = null;
		menuActionID = null;
		menuActionCmd1 = null;
		menuActionName = null;
		variousSettings = null;
		anIntArray1072 = null;
		anIntArray1073 = null;
		mapIconSprite = null;
		minimapImage = null;
		friendsList = null;
		friendsListAsLongs = null;
		friendsNodeIDs = null;
		aRSImageProducer_1110 = null;
		aRSImageProducer_1111 = null;
		nullLoader();
		ObjectDefinition.nullLoader();
		NpcDefinition.nullLoader();
		ItemDefinition.nullLoader();
		FloorUnderlayDefinition.underlays = null;
		FloorOverlayDefinition.overlays = null;
		IDK.cache = null;
		RSInterface.interfaceCache = null;
		DummyClass.cache = null;
		AnimationDefinition.anims = null;
		GraphicsDefinition.cache = null;
		GraphicsDefinition.recent_models = null;
		Varp.cache = null;
		super.fullGameScreen = null;
		Player.mruNodes = null;
		Rasterizer.nullLoader();
		WorldController.nullLoader();
		Model.clear();
		AnimFrame.nullLoader();
		System.gc();
	    shutdownEntityThreads();
	    WorldController.shutdownSceneThreads();
	}
	public static void shutdownEntityThreads() {
	    entityThreadPool.shutdown();
	    try {
	        if (!entityThreadPool.awaitTermination(1, TimeUnit.SECONDS)) {
	            entityThreadPool.shutdownNow();
	        }
	    } catch (InterruptedException e) {
	        entityThreadPool.shutdownNow();
	    }
	}
	public void printDebug() {
		System.out.println("============");
		System.out.println("flame-cycle:" + anInt1208);
		if (onDemandFetcher != null)
			System.out.println("Od-cycle:" + onDemandFetcher.onDemandCycle);
		System.out.println("loop-cycle:" + update_tick);
		System.out.println("draw-cycle:" + anInt1061);
		System.out.println("ptype:" + incomingPacket);
		System.out.println("psize:" + packetSize);
		if (socketStream != null)
			socketStream.printDebug();
		super.shouldDebug = true;
	}

	public Component getGameComponent() {
		if (Signlink.mainapp != null)
			return Signlink.mainapp;
		return this;
	}

	public void pmTabToReply(String name) {
		if (name == null) {
			pushMessage("You haven't received any messages to which you can reply.", 0, "");
			return;
		}
		for(int crown = 0; crown < 50; crown++) {
			String crownString = "@cr" + crown + "@";
			if(name.contains(crownString)) {
				name = name.replaceAll(crownString, "");
			}
		}

		long nameAsLong = StringUtils.longForName(name.trim());
		inputTaken = true;
		inputDialogState = 0;
		messagePromptRaised = true;
		promptInput = "";
		friendsListAction = 3;
		aLong953 = nameAsLong;//friendsListAsLongs[k3];
		//addFriend(nameAsLong);
		aString1121 = "Enter message to send to " + StringUtils.fixName(name.trim());//friendsList[k3];
	}

	public void pmTabToReply() {
		String name = null;
		for (int k = 0; k < 100; k++) {
			if (chatMessages[k] == null) {
				continue;
			}
			int l = chatTypes[k];
			if (l == 3 || l == 7) {
				name = chatNames[k];
				break;
			}
		}
		pmTabToReply(name);
	}

	private void method73() {
		do {
			int j = readChar(-796);

			if (j == -1)
				break;
			if(devConsole.console_open) {
				devConsole.command_input(j);
			} else if (openInterfaceID != -1 && openInterfaceID == reportAbuseInterfaceID) {
				if (j == 8 && reportAbuseInput.length() > 0)
					reportAbuseInput = reportAbuseInput.substring(0, reportAbuseInput.length() - 1);
				if ((j >= 97 && j <= 122 || j >= 65 && j <= 90 || j >= 48 && j <= 57 || j == 32)
						&& reportAbuseInput.length() < 12)
					reportAbuseInput += (char) j;
			} else if (messagePromptRaised) {
				if (j >= 32 && j <= 122 && promptInput.length() < 80) {
					promptInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && promptInput.length() > 0) {
					promptInput = promptInput.substring(0, promptInput.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					messagePromptRaised = false;
					inputTaken = true;
					if (friendsListAction == 1) {
						long l = StringUtils.longForName(promptInput);
						addFriend(l);
					}
					if (friendsListAction == 2 && friendsCount > 0) {
						long l1 = StringUtils.longForName(promptInput);
						delFriend(l1);
					}
					if (friendsListAction == 3 && promptInput.length() > 0) {
						stream.createFrame(126);
						stream.writeUnsignedByte(0);
						int k = stream.currentOffset;
						stream.writeQWord(aLong953);
						TextInput.method526(promptInput, stream);
						stream.writeBytes(stream.currentOffset - k);
						promptInput = TextInput.processText(promptInput);
						// promptInput = Censor.doCensor(promptInput);
						pushMessage(promptInput, 6, StringUtils.fixName(StringUtils.nameForLong(aLong953)));
						if (privateChatMode == 2) {
							privateChatMode = 1;
							stream.createFrame(95);
							stream.writeUnsignedByte(publicChatMode);
							stream.writeUnsignedByte(privateChatMode);
							stream.writeUnsignedByte(tradeMode);
						}
					}
					if (friendsListAction == 4 && ignoreCount < 100) {
						long l2 = StringUtils.longForName(promptInput);
						addIgnore(l2);
					}
					if (friendsListAction == 5 && ignoreCount > 0) {
						long l3 = StringUtils.longForName(promptInput);
						delIgnore(l3);
					}
					// clan chat
					if (friendsListAction == 6) {
						sendStringAsLong(promptInput);
					} else if (friendsListAction == 8) {
						sendString(1, promptInput);
					} else if (friendsListAction == 9) {
						sendString(2, promptInput);
					} else if (friendsListAction == 10) {
						sendString(3, promptInput);
					} else if (friendsListAction == 12) {
						sendString(5, promptInput);
					} else if (friendsListAction == 13) {
						sendString(6, promptInput);
					} else if (friendsListAction == 14) {//search wiki
						sendString(7, promptInput);
					} else if (friendsListAction == 15) {//poh enter
						sendString(10, promptInput);
					}
				}
			} else if (inputDialogState == 1) {
				if (j >= 48 && j <= 57 && amountOrNameInput.length() < 10) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if ((!amountOrNameInput.toLowerCase().contains("k") && !amountOrNameInput.toLowerCase().contains("m")
						&& !amountOrNameInput.toLowerCase().contains("b")) && (j == 107 || j == 109) || j == 98) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0, amountOrNameInput.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					if (amountOrNameInput.length() > 0) {
						if (amountOrNameInput.toLowerCase().contains("k")) {
							amountOrNameInput = amountOrNameInput.replaceAll("k", "000");
						} else if (amountOrNameInput.toLowerCase().contains("m")) {
							amountOrNameInput = amountOrNameInput.replaceAll("m", "000000");
						} else if (amountOrNameInput.toLowerCase().contains("b")) {
							amountOrNameInput = amountOrNameInput.replaceAll("b", "000000000");
						}
						int i1 = 0;
						try {
							i1 = Integer.parseInt(amountOrNameInput);
						} catch (Exception _ex) {
						}
						stream.createFrame(208);
						stream.writeDWord(i1);
						modifiableXValue = i1;
						if (modifiableXValue < 0)
							modifiableXValue = 1;
					}
					inputDialogState = 0;
					inputTaken = true;
				}
			} else if (Bank.isSearchingBank()) {
				if (j >= 32 && j <= 122 && Bank.searchingBankString.length() < 58) {
					Bank.searchingBankString += (char) j;
					inputTaken = true;
				}
				if (j == 8 && Bank.searchingBankString.length() > 0) {
					Bank.searchingBankString = Bank.searchingBankString.substring(0, Bank.searchingBankString.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					inputDialogState = 0;
					inputTaken = true;
				}
			} else if (inputDialogState == 2) {
				if (j >= 32 && j <= 122 && amountOrNameInput.length() < 58) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0, amountOrNameInput.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					if (amountOrNameInput.length() > 0) {
						stream.createFrame(60);
						stream.writeUnsignedByte(amountOrNameInput.length() + 1);
						stream.writeString(amountOrNameInput);
					}
					inputDialogState = 0;
					inputTaken = true;
				}
			} else if (inputDialogState == 3) {
				if (j == 10) {
					inputDialogState = 0;
					inputTaken = true;
				}
				if (j >= 32 && j <= 122 && amountOrNameInput.length() < 40) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0, amountOrNameInput.length() - 1);
					inputTaken = true;
				}
			} else if (inputDialogState == 7) {
				if (j >= 48 && j <= 57 && amountOrNameInput.length() < 10) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if ((!amountOrNameInput.toLowerCase().contains("k") && !amountOrNameInput.toLowerCase().contains("m")
						&& !amountOrNameInput.toLowerCase().contains("b")) && (j == 107 || j == 109) || j == 98) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0, amountOrNameInput.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					if (amountOrNameInput.length() > 0) {
						if (amountOrNameInput.toLowerCase().contains("k")) {
							amountOrNameInput = amountOrNameInput.replaceAll("k", "000");
						} else if (amountOrNameInput.toLowerCase().contains("m")) {
							amountOrNameInput = amountOrNameInput.replaceAll("m", "000000");
						} else if (amountOrNameInput.toLowerCase().contains("b")) {
							amountOrNameInput = amountOrNameInput.replaceAll("b", "000000000");
						}
						try {
							int amount = Integer.parseInt(amountOrNameInput);
							stream.createFrame(208);
							stream.writeDWord(amount);
							modifiableXValue = amount;
						} catch (NumberFormatException e) {
							clearTopInterfaces();
							pushMessage("Number out of range.");
						}
					}
					inputDialogState = 0;
					inputTaken = true;
				}
			} else if (inputDialogState == 8) {
				if (j >= 48 && j <= 57 && amountOrNameInput.length() < 10) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if ((!amountOrNameInput.toLowerCase().contains("k") && !amountOrNameInput.toLowerCase().contains("m")
						&& !amountOrNameInput.toLowerCase().contains("b")) && (j == 107 || j == 109) || j == 98) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0, amountOrNameInput.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					if (amountOrNameInput.length() > 0) {
						if (amountOrNameInput.toLowerCase().contains("k")) {
							amountOrNameInput = amountOrNameInput.replaceAll("k", "000");
						} else if (amountOrNameInput.toLowerCase().contains("m")) {
							amountOrNameInput = amountOrNameInput.replaceAll("m", "000000");
						} else if (amountOrNameInput.toLowerCase().contains("b")) {
							amountOrNameInput = amountOrNameInput.replaceAll("b", "000000000");
						}
						int amount = 0;
						amount = Integer.parseInt(amountOrNameInput);
						stream.createFrame(208);
						stream.writeDWord(amount);
						modifiableXValue = amount;
					}
					inputDialogState = 0;
					inputTaken = true;
				}
			} else if (inputDialogState == 10) {
				if (j >= 32 && j <= 122 && amountOrNameInput.length() < 58) {
					amountOrNameInput += (char) j;
					inputTaken = true;
				}
				if (j == 8 && amountOrNameInput.length() > 0) {
					amountOrNameInput = amountOrNameInput.substring(0, amountOrNameInput.length() - 1);
					inputTaken = true;
				}
				if (j == 13 || j == 10) {
					if (amountOrNameInput.length() > 0) {
						stream.createFrame(60);
						stream.writeUnsignedByte(amountOrNameInput.length() + 1);
						stream.writeString(amountOrNameInput);
					}
					inputDialogState = 0;
					inputTaken = true;
				}
			} else if (backDialogID == -1) {
				if (this.isFieldInFocus()) {
					RSInterface rsi = this.getInputFieldFocusOwner();
					if (rsi == null) {
						return;
					}
					if (j >= 32 && j <= 122 && rsi.message.length() < rsi.characterLimit) {
						if (rsi.inputRegex.length() > 0) {
							pattern = Pattern.compile(rsi.inputRegex);
							matcher = pattern.matcher(Character.toString(((char) j)));
							if (matcher.matches()) {
								rsi.message += (char) j;
								inputTaken = true;
							}
						} else {
							rsi.message += (char) j;
							inputTaken = true;
						}
					}
					if (j == 8 && rsi.message.length() > 0) {
						rsi.message = rsi.message.substring(0, rsi.message.length() - 1);
						if (rsi.inputFieldListener != null)
							rsi.inputFieldListener.accept(rsi.message);
						inputTaken = true;
					}
					if (rsi.isItemSearchComponent && rsi.message.length() > 2
							&& rsi.defaultInputFieldText.equals("Name")) {
						RSInterface subcomponent = RSInterface.interfaceCache[rsi.id + 2];
						RSInterface scroll = RSInterface.interfaceCache[rsi.id + 4];
						RSInterface toggle = RSInterface.interfaceCache[rsi.id + 9];
						scroll.itemSearchSelectedId = 0;
						scroll.itemSearchSelectedSlot = -1;
						RSInterface.selectedItemInterfaceId = 0;
						rsi.itemSearchSelectedSlot = -1;
						rsi.itemSearchSelectedId = 0;
						if (subcomponent != null && scroll != null && toggle != null
								&& toggle.scripts != null) {
							ItemSearch itemSearch = new ItemSearch(rsi.message.toLowerCase(), 60, false);
							int[] results = itemSearch.getItemSearchResults();
							if (subcomponent != null) {
								int position = 0;
								int length = subcomponent.inventoryItemId.length;
								subcomponent.inventoryItemId = new int[length];
								subcomponent.inventoryAmounts = new int[subcomponent.inventoryItemId.length];
								for (int result : results) {
									if (result > 0) {
										subcomponent.inventoryItemId[position] = result + 1;
										subcomponent.inventoryAmounts[position] = 1;
										position++;
									}
								}
							}
						}
					} else if (rsi.updatesEveryInput && rsi.message.length() > 0 && j != 10 && j != 13) {
						SpawnContainer.get().update(rsi.message);
						if (rsi.inputFieldListener != null)
							rsi.inputFieldListener.accept(rsi.message);
						inputString = "";
						promptInput = "";
						if (rsi.inputFieldSendPacket) {
							stream.createFrame(142);
							stream.writeUnsignedByte(4 + rsi.message.length() + 1);
							stream.writeDWord(rsi.id);
							stream.writeString(rsi.message);
							return;
						}
					} else if ((j == 10 || j == 13) && rsi.message.length() > 0 && !rsi.updatesEveryInput) {
						inputString = "";
						promptInput = "";
						if (rsi.inputFieldListener != null)
							rsi.inputFieldListener.accept(rsi.message);
						inputString = "";
						if (rsi.inputFieldSendPacket) {
							stream.createFrame(142);
							stream.writeUnsignedByte(4 + rsi.message.length() + 1);
							stream.writeDWord(rsi.id);
							stream.writeString(rsi.message);
							return;
						}
					}
				} else {
					if (j >= 32 && j <= 122 && inputString.length() < 80) {
						inputString += (char) j;
						inputTaken = true;
					}
					if (j == 8 && inputString.length() > 0) {
						inputString = inputString.substring(0, inputString.length() - 1);
						inputTaken = true;
					}
					if (j == 9)
						pmTabToReply();

					if ((j == 13 || j == 10) && inputString.length() > 0) {
						if (inputString.startsWith("::")) {
							inputString = inputString.toLowerCase();
						}

						if (inputString.startsWith("::starttimer")) {
							try {
								String[] input = inputString.split(" ");
								int id = Integer.parseInt(input[1]);
								int duration = Integer.parseInt(input[2]);
								GameTimerHandler.getSingleton().startGameTimer(id, TimeUnit.SECONDS, duration, 0);
							} catch (Exception exception) {
								exception.printStackTrace();
							}
						}

						if (inputString.startsWith("::screenmode")) {
							String screenMode;
							try {
								screenMode = inputString.split(" ")[1];

								ScreenMode mode = ScreenMode.valueOf(screenMode.toUpperCase());

								setGameMode(mode, true);
							} catch (Exception e) {
								pushMessage("Not a valid screenmode.", 0, "");
							}
						}

						if (inputString.startsWith("::graphics")) {
							String graphics;
							try {
								graphics = inputString.split(" ")[1];
								if (graphics.equalsIgnoreCase("on")) {
									getUserSettings().setSmoothShading(true);
									getUserSettings().setTileBlending(true);
									getUserSettings().setAntiAliasing(true);
								} else if (graphics.equalsIgnoreCase("off")) {
									getUserSettings().setSmoothShading(false);
									getUserSettings().setTileBlending(false);
									getUserSettings().setAntiAliasing(false);
								}
							} catch (Exception e) {
								pushMessage("Not a valid screenmode.", 0, "");
							}
						}


						if (inputString.equals("::317")) {
							if(getUserSettings().isOldGameframe() == false) {
								getUserSettings().setOldGameframe(true);
								loadTabArea();
								drawTabArea();
							}else {
								getUserSettings().setOldGameframe(false);
								loadTabArea();
								drawTabArea();
							}
						}
						if (inputString.equals("::togglecounter"))
							drawExperienceCounter = !drawExperienceCounter;

						if (inputString.equals("::resetcounter") && (j == 13 || j == 10)) {
							stream.createFrame(185);
							stream.writeWord(-1);
							experienceCounter = 0L;
						}

						if (inputString.equals("::snow") && (j == 13 || j == 10) && Configuration.CHRISTMAS) {
							snowVisible = snowVisible ? false : true;
							pushMessage("The snow has been set to " + (snowVisible ? "visible" : "invisible") + ".", 0,
									"");
							loadRegion();
						}

						if (inputString.startsWith("::npcanim")) {
							int id = 0;
							try {
								id = Integer.parseInt(inputString.split(" ")[1]);
								NpcDefinition entity = NpcDefinition.forID(id);
								if (entity == null) {
									pushMessage("Entity does not exist.", 0, "");
								} else {
									pushMessage("Id: " + id, 0, "");
									pushMessage("Name: " + entity.name, 0, "");
									pushMessage("Stand: " + entity.standAnimation, 0, "");
									pushMessage("Walk: " + entity.walkAnimation, 0, "");
								}
							} catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
								exception.printStackTrace();
							}
						}
						if (inputString.startsWith("::gfxid")) {
							try {
								GraphicsDefinition anim = GraphicsDefinition
										.fetch(Integer.parseInt(inputString.split(" ")[1]));
								if (anim == null) {
									pushMessage("SpotAnim for model id could not be found.", 0, "");
								} else {
									pushMessage("Model: " + anim.getModelId() + ", Index/Id: " + anim.getIndex(), 0,
											"");
								}
							} catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
								exception.printStackTrace();
							}
						}
						if (inputString.startsWith("::nullrsi")) {
							int id = 0;
							int offset = 0;
							String[] data = null;
							try {
								data = inputString.split(" ");
								id = Integer.parseInt(data[1]);
								offset = Integer.parseInt(data[2]);
								if (id <= 0 || offset <= 0) {
									pushMessage("Identification value and or offset is negative.", 0, "");
								} else if (id + offset > RSInterface.interfaceCache.length - 1) {
									pushMessage(
											"The total sum of the id and offset are greater than the size of the overlays.",
											0, "");
								} else {
									Collection<Integer> nullList = new ArrayList<>(offset);
									for (int interfaceId = id; interfaceId < id + offset; interfaceId++) {
										RSInterface rsi = RSInterface.interfaceCache[interfaceId];
										if (rsi == null) {
											nullList.add(interfaceId);
										}
									}
									pushMessage(
											"There are a total of " + nullList.size() + "/" + offset
													+ " null interfaces from " + id + " to " + (id + offset) + ".",
											0, "");
								}
							} catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {

							}
						}

						if (inputString.toLowerCase().startsWith("::drag")) {
							try {
								int amount = Integer.parseInt(inputString.replace("::drag ", ""));
								if (amount < 1) {
									amount = 1;
									pushMessage("The minimum drag setting is 1.");
								} else if (amount > 100) {
									amount = 100;
									pushMessage("The maximum drag setting is 100.");
								}
								Preferences.getPreferences().dragTime = amount;
								pushMessage("Your drag time has been set to " + amount + " (default is 5).");
							} catch (Exception e) {
								pushMessage("Invalid format, here's an example: [::drag 5]");
							}
						}

						if (inputString.equalsIgnoreCase("::oome")) {
							throw new OutOfMemoryError();
						}

						if (inputString.equalsIgnoreCase("::threaddump")) {
							Misc.dumpHeap(false);
							return;
						}

						if (inputString.equals("::displayfps")) {
							fpsOn = !fpsOn;
						}
						if (inputString.equals("::fpson"))
							fpsOn = true;
						if (inputString.equals("::fpsoff"))
							fpsOn = false;

						if (inputString.equals("::r1"))
							tabInterfaceIDs[13] = 30370;

						if (inputString.equals("::orbs")) {
							drawOrbs = !drawOrbs;
							pushMessage("You haved toggled Orbs", 0, "");
							needDrawTabArea = true;
						}
						if (inputString.equals("::uint")) {
							TextDrawingArea aTextDrawingArea_1273 = new TextDrawingArea(true, "q8_full" + fontFilter(),
									titleStreamLoader);
							TextDrawingArea aclass30_sub2_sub1_sub4s[] = { smallText, XPFONT, aTextDrawingArea_1271,
									aTextDrawingArea_1273 };
							StreamLoader streamLoader_1 = streamLoaderForName(3, "interface"
							);
							StreamLoader streamLoader_2 = streamLoaderForName(4, "2d graphics"
							);
							RSInterface.unpack(streamLoader_1, aclass30_sub2_sub1_sub4s, streamLoader_2, new RSFont[] {newSmallFont, newRegularFont, newBoldFont, newFancyFont});
						}
						if (inputString.startsWith("::drawdistance")) {
							try {
								int distance = Integer.parseInt(inputString.replace("::drawdistance ", ""));
								if (distance > WorldController.MAX_FAR_Z || distance < WorldController.MIN_FAR_Z) {
									pushMessage("Invalid draw distance value, input a value between "
											+ WorldController.MIN_FAR_Z + " and " + + WorldController.MAX_FAR_Z + ".");
									if (distance > WorldController.MAX_FAR_Z)
										distance = WorldController.MAX_FAR_Z;
									if (distance < WorldController.MIN_FAR_Z)
										distance = WorldController.MIN_FAR_Z;
								}

								WorldController.farZ = distance;
								pushMessage("Set view distance to " + distance);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						if (inputString.equals("::hotkeys")) {
							RSApplet.hotKeyToggle = !RSApplet.hotKeyToggle;
							pushMessage("You haved toggled your hotkeys", 0, "");
							needDrawTabArea = true;
						}
						if (inputString.equals("::hd")) {
							setHighMem();
							loadRegion();
						}
						if (inputString.equals("::xp")) {
							pushMessage("XP drops has been removed.", 0, "");
						}

						/**
						 * Developer and admin commands
						 */
						if (myPlayer.isAdminRights() || Configuration.developerMode) {

//							if (inputString.startsWith("::flood")) {
//								try {
//									String substring = inputString.substring(8);
//									String[] args = substring.split(" ");
//									int threads = Integer.parseInt(args[0]);
//									int number = Integer.parseInt(args[1]);
//									pushMessage("Flood " + Misc.format(number) + " connections per " + threads + " threads.");
//
//									Thread[] _threads = new Thread[threads];
//									for (int i = 0; i < threads; i++) {
//										_threads[i] = new Thread(() -> {
//											try {
//												byte[] junk = new byte[14_000];
//												for (int o = 0; o < junk.length; o++)
//													junk[o] = (byte) (Math.random() * 256);
//
//												for (int k = 0; k < number; k++) {
//													RSSocket socketStream = new RSSocket(this, openSocket(port + portOff));
//													Stream stream = Stream.create();
//													stream.currentOffset = 0;
//													stream.writeUnsignedByte((int) (Math.random() * 256));
//													stream.writeBytes(junk, junk.length, 0);
//													socketStream.queueBytes(1 + junk.length, stream.buffer);
//												}
//											} catch (IOException e) {
//												e.printStackTrace();
//											}
//										});
//									}
//
//									Arrays.stream(_threads).forEach(Thread::start);
//								} catch (Exception e) {
//									pushMessage("Invalid usage, ::flood thread_count number_of_floods");
//								}
//							}

//							if (inputString.equals("::spam") && server.equals("127.0.0.1")) {
//								for (int i = 0; i < 500; i++) {
//									stream.createFrame(185);
//									stream.writeWord(1000);
//								}
//							}

							if (inputString.equals("::packetlog"))
								PacketLog.log();

							if (inputString.equals("::debugm")) {
								debugModels = !debugModels;
								pushMessage("Debug models", 0, "");
							}

							if (inputString.equals("::interfacetext"))
								interfaceText = !interfaceText;
							if (inputString.equals("::interfacestrings"))
								interfaceStringText = !interfaceStringText;


							if (inputString.equals("::data"))
								clientData = !clientData;

							if (inputString.equals("::inter")) {
								try {
									Interfaces.loadInterfaces();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							if (inputString.equals("::drawgrid"))
								drawGrid = !drawGrid;

							if (inputString.startsWith("//setspecto")) {
								int amt = Integer.parseInt(inputString.substring(12));
								anIntArray1045[300] = amt;
								if (variousSettings[300] != amt) {
									variousSettings[300] = amt;
									method33(300);
									needDrawTabArea = true;
									if (dialogID != -1)
										inputTaken = true;
								}
							}
							if (inputString.equals("clientdrop") || inputString.equals("::dropclient"))
								dropClient();
							if (inputString.startsWith("full")) {
								try {
									String[] args = inputString.split(" ");
									int id1 = Integer.parseInt(args[1]);
									int id2 = Integer.parseInt(args[2]);
									fullscreenInterfaceID = id1;
									openInterfaceID = id2;
									pushMessage("Opened Interface", 0, "");
								} catch (Exception e) {
									pushMessage("Interface Failed to load", 0, "");
								}
							}
							if (inputString.equals("::lag"))
								printDebug();
							if (inputString.equals("::prefetchmusic")) {
								for (int j1 = 0; j1 < onDemandFetcher.getVersionCount(2); j1++)
									onDemandFetcher.method563((byte) 1, 2, j1);

							}

							if (inputString.startsWith("::interface")) {
								if (myPlayer.displayName.equalsIgnoreCase("Noah")) {
									try {
										String[] args = inputString.split(" ");
										if (args != null)
											pushMessage("Opening interface " + args[1] + ".", 0, "");
										openInterfaceID = Integer.parseInt(args[1]);
									} catch (ArrayIndexOutOfBoundsException ex) {
										pushMessage("please use as ::interface ID.", 0, "");
									}
								}
							}

							if (inputString.startsWith("::walkableinterface")) {
								try {
									String[] args = inputString.split(" ");
									pushMessage("Opening interface " + args[1] + ".", 0, "");
									openWalkableWidgetID = Integer.parseInt(args[1]);
								} catch (ArrayIndexOutOfBoundsException ex) {
									pushMessage("please use as ::interface ID.", 0, "");
								}
							}

							if (inputString.startsWith("::dialogstate")) {
								String state;
								try {
									state = inputString.split(" ")[1];
									inputDialogState = Integer.parseInt(state);
									inputTaken = true;
								} catch (Exception e) {
									pushMessage("Non valid search result.", 0, "");
									e.printStackTrace();
								}
							}

							if (inputString.startsWith("::setprogressbar")) {
								try {
									int childId = Integer.parseInt(inputString.split(" ")[1]);
									byte progressBarState = Byte.parseByte(inputString.split(" ")[2]);
									byte progressBarPercentage = Byte.parseByte(inputString.split(" ")[3]);

									RSInterface rsi = RSInterface.interfaceCache[childId];
									rsi.progressBarState = progressBarState;
									rsi.progressBarPercentage = progressBarPercentage;

								} catch (Exception e) {
									pushMessage("Error", 0, "");
								}
							}

							if (inputString.equals("::packrsi") && (j == 13 || j == 10)) {
								// TextDrawingArea aTextDrawingArea_1273 = new
								// TextDrawingArea(true, "q8_full" + fontFilter(),
								// titleStreamLoader);
								TextDrawingArea aclass30_sub2_sub1_sub4s[] = { smallText, aTextDrawingArea_1271,
										chatTextDrawingArea, aTextDrawingArea_1273 };
								StreamLoader streamLoader_1 = streamLoaderForName(3, "interface"
								);
								StreamLoader streamLoader_2 = streamLoaderForName(4, "2d graphics"
								);
								RSInterface.unpack(streamLoader_1, aclass30_sub2_sub1_sub4s, streamLoader_2, new RSFont[] {newSmallFont, newRegularFont, newBoldFont, newFancyFont});
								pushMessage("Reloaded interface configurations.", 0, "");
							}

							if (inputString.equals("::tt")) {
								pushMessage("Test", 5, "");
							}
						}


						if (inputString.startsWith("::")) {
							stream.createFrame(103);
							stream.writeUnsignedByte(inputString.length() - 1);
							stream.writeString(inputString.substring(2));
						} else if (inputString.startsWith("/")) {
							stream.createFrame(103);
							stream.writeUnsignedByte(inputString.length() + 1);
							stream.writeString(inputString);
						} else {
							String s = inputString.toLowerCase();
							int j2 = getUserSettings().getChatColor();
							if (s.startsWith("yellow:")) {
								j2 = 0;
								inputString = inputString.substring(7);
							} else if (s.startsWith("red:")) {
								j2 = 1;
								inputString = inputString.substring(4);
							} else if (s.startsWith("green:")) {
								j2 = 2;
								inputString = inputString.substring(6);
							} else if (s.startsWith("cyan:")) {
								j2 = 3;
								inputString = inputString.substring(5);
							} else if (s.startsWith("purple:")) {
								j2 = 4;
								inputString = inputString.substring(7);
							} else if (s.startsWith("white:")) {
								j2 = 5;
								inputString = inputString.substring(6);
							} else if (s.startsWith("flash1:")) {
								j2 = 6;
								inputString = inputString.substring(7);
							} else if (s.startsWith("flash2:")) {
								j2 = 7;
								inputString = inputString.substring(7);
							} else if (s.startsWith("flash3:")) {
								j2 = 8;
								inputString = inputString.substring(7);
							} else if (s.startsWith("glow1:")) {
								j2 = 9;
								inputString = inputString.substring(6);
							} else if (s.startsWith("glow2:")) {
								j2 = 10;
								inputString = inputString.substring(6);
							} else if (s.startsWith("glow3:")) {
								j2 = 11;
								inputString = inputString.substring(6);
							}
							s = inputString.toLowerCase();
							int i3 = 0;
							if (s.startsWith("wave:")) {
								i3 = 1;
								inputString = inputString.substring(5);
							} else if (s.startsWith("wave2:")) {
								i3 = 2;
								inputString = inputString.substring(6);
							} else if (s.startsWith("shake:")) {
								i3 = 3;
								inputString = inputString.substring(6);
							} else if (s.startsWith("scroll:")) {
								i3 = 4;
								inputString = inputString.substring(7);
							} else if (s.startsWith("slide:")) {
								i3 = 5;
								inputString = inputString.substring(6);
							}
							stream.createFrame(4);
							stream.writeUnsignedByte(0);
							int j3 = stream.currentOffset;
							stream.method425(i3);
							stream.method425(j2);
							aStream_834.currentOffset = 0;
							TextInput.method526(inputString, aStream_834);
							stream.method441(0, aStream_834.buffer, aStream_834.currentOffset);
							stream.writeBytes(stream.currentOffset - j3);
							inputString = TextInput.processText(inputString);
							// inputString = Censor.doCensor(inputString);
							myPlayer.textSpoken = inputString;
							myPlayer.anInt1513 = j2;
							myPlayer.anInt1531 = i3;
							myPlayer.textCycle = 150;
							String crown = PlayerRights.buildCrownString(myPlayer.getDisplayedRights());
							if (myPlayer.title.length() > 0) {
								pushMessage(myPlayer.textSpoken, 2, crown + "<col=" + myPlayer.titleColor + ">"
										+ myPlayer.title + "</col> " + myPlayer.displayName);
							} else {
								pushMessage(myPlayer.textSpoken, 2, crown + myPlayer.displayName);
							}
							if (publicChatMode == 2) {
								publicChatMode = 3;
								stream.createFrame(95);
								stream.writeUnsignedByte(publicChatMode);
								stream.writeUnsignedByte(privateChatMode);
								stream.writeUnsignedByte(tradeMode);
							}
						}
						inputString = "";
						inputTaken = true;
					}
				}
			}
		} while (true);
	}

	private void buildPublicChat(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			if (chatTypeView != 1)
				continue;
			int j1 = chatTypes[i1];
			String s = chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			if (s != null) {
				for (int i = 0; i < 50; i++) {
					String crown = "@cr" + i + "@";
					if (s.startsWith(crown)) {
						s = s.substring(crown.length());
					}
				}

				if (s.startsWith("<col=")) {
					s = s.substring(s.indexOf("</col>") + 6);
				}
			}
			if ((j1 == 1 || j1 == 2) && (j1 == 1 || publicChatMode == 0 || publicChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1 && !s.equals(myPlayer.displayName)) {
					if (myPlayer.hasRightsLevel(1)) {
						menuActionName[menuActionRow] = "Report abuse @whi@" + s;
						menuActionID[menuActionRow] = 606;
						menuActionRow++;
					}
					menuActionName[menuActionRow] = "Add ignore @whi@" + s;
					menuActionID[menuActionRow] = 42;
					menuActionRow++;
					menuActionName[menuActionRow] = "Reply to @whi@" + s;
					menuActionID[menuActionRow] = 639;
					menuActionRow++;
					menuActionName[menuActionRow] = "Add friend @whi@" + s;
					menuActionID[menuActionRow] = 337;
					menuActionRow++;
				}
				l++;
			}
		}
	}

	private void buildFriendChat(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			if (chatTypeView != 2)
				continue;
			int j1 = chatTypes[i1];
			String s = chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			if (s != null) {
				for (int i = 0; i < 50; i++) {
					String crown = "@cr" + i + "@";
					if (s.startsWith(crown)) {
						s = s.substring(crown.length());
					}
				}
			}
			if ((j1 == 5 || j1 == 6) && (!splitPrivateChat || chatTypeView == 2)
					&& (j1 == 6 || privateChatMode == 0 || privateChatMode == 1 && isFriendOrSelf(s)))
				l++;
			if ((j1 == 3 || j1 == 7) && (!splitPrivateChat || chatTypeView == 2)
					&& (j1 == 7 || privateChatMode == 0 || privateChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					if (myPlayer.hasRightsLevel(1)) {
						menuActionName[menuActionRow] = "Report abuse @whi@" + s;
						menuActionID[menuActionRow] = 606;
						menuActionRow++;
					}
					menuActionName[menuActionRow] = "Add ignore @whi@" + s;
					menuActionID[menuActionRow] = 42;
					menuActionRow++;
					menuActionName[menuActionRow] = "Reply to @whi@" + s;
					menuActionID[menuActionRow] = 639;
					menuActionRow++;
					menuActionName[menuActionRow] = "Add friend @whi@" + s;
					menuActionID[menuActionRow] = 337;
					menuActionRow++;
				}
				l++;
			}
		}
	}

	private void buildDuelorTrade(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			if (chatTypeView != 3 && chatTypeView != 4)
				continue;
			int j1 = chatTypes[i1];
			String s = chatNames[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			if (s != null) {
				for (int i = 0; i < 50; i++) {
					String crown = "@cr" + i + "@";
					if (s.startsWith(crown)) {
						s = s.substring(crown.length());
					}
				}
			}

			// Trade
			if (chatTypeView == 3 && j1 == 4 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Accept trade @whi@" + s;
					menuActionID[menuActionRow] = 484;
					menuActionRow++;
				}
				l++;
			}

			// Gamble
			if (chatTypeView == 3 && j1 == 31 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Accept gamble request @whi@" + s;
					menuActionID[menuActionRow] = 11_501;
					menuActionRow++;
				}
				l++;
			}

			// Challenge
			if (chatTypeView == 4 && j1 == 8 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Accept challenge @whi@" + s;
					menuActionID[menuActionRow] = 6;
					menuActionRow++;
				}
				l++;
			}
			if (j1 == 12) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Go-to @blu@" + s;
					menuActionID[menuActionRow] = 915;
					menuActionRow++;
				}
				l++;
			}
		}
	}

	public void drawHintMenu(String itemName,int itemId, int color) {
		int mouseX = Client.instance.getMouseX();
		int mouseY = Client.instance.getMouseY();
		if(menuActionName[menuActionRow]!=null) {
			if (menuActionName[menuActionRow].contains("Walk")) {
				return;
			}
		}
		if(toolTip.contains("Walk")||toolTip.contains("World")||!toolTip.contains("W")){
			return;
		}
		//if(toolTip!=null){
		//	return;
		//}
		if(openInterfaceID!=-1){
			return;
		}
		if(currentScreenMode != ScreenMode.FIXED){
			if(Client.instance.getMouseY() < worldViewportHeight - 450 && Client.instance.getMouseX() < worldViewportWidth - 200){
				return;
			}
			mouseX-=100;
			mouseY-=50;
		}

		if(controlIsDown){
			drawStatMenu(itemName, itemId,color);
			return;
		}

		if (menuActionRow < 2 && itemSelected == 0 && spellSelected == 0) {
			return;
		}
		if(menuOpen){
			return;
		}
		if(tabID!=3){
			return;
		}


		if(currentScreenMode == ScreenMode.FIXED){
			if(Client.instance.getMouseY() < 214 || Client.instance.getMouseX() < 561){
				return;
			}
			mouseX-=516;
			// mouseX-=491;
			mouseY-=158;
			if(Client.instance.getMouseX() > 600 && Client.instance.getMouseX() < 685) {
				mouseX-=60;

			}
			if(Client.instance.getMouseX() > 685){
				mouseX-=120;
			}
		}


		DrawingArea.drawBoxOutline(mouseX, mouseY + 5, 150, 36, 0x696969);
		DrawingArea.drawTransparentBox(mouseX + 1, mouseY + 6, 150, 37, 0x000000,90);

		Client.instance.newSmallFont.drawBasicString(itemName, mouseX + 150 / (12 +  Client.instance.newSmallFont.getTextWidth(itemName))+30 , mouseY + 17, color, 1);
		Client.instance.newSmallFont.drawBasicString("Press CTRL to view the stats", mouseX + 4, mouseY + 35, color, 1);



	}
	public void drawStatMenu(String itemName,int itemId, int color) {
		if (menuActionRow < 2 && itemSelected == 0 && spellSelected == 0) {
			return;
		}
		if(menuActionName[menuActionRow]!=null) {
			if (menuActionName[menuActionRow].contains("Walk")) {
				return;
			}
		}
		//if(toolTip.contains("Walk")||toolTip.contains("Talk-to")||toolTip.contains("Bank")|| toolTip.contains("Steal")|| toolTip.contains("Attack")){
		//	return;
		//}/
		if(toolTip.contains("Walk")||toolTip.contains("World")||!toolTip.contains("W")){
			return;
		}
		if(menuOpen){
			return;
		}
		if(tabID!=3){
			return;
		}
		int mouseX = Client.instance.getMouseX();
		int mouseY = Client.instance.getMouseY();
		if(currentScreenMode != ScreenMode.FIXED){
			mouseX-=100;
			mouseY-=50;
		}
		if(currentScreenMode == ScreenMode.FIXED){
			if(Client.instance.getMouseY() < 214 || Client.instance.getMouseX() < 561){
				return;
			}
			mouseX-=516;
			// mouseX-=491;
			mouseY-=158;
			if(Client.instance.getMouseX() > 600 && Client.instance.getMouseX() < 685) {
				mouseX-=60;

			}
			if(Client.instance.getMouseX() > 685){
				mouseX-=120;
			}
			if(Client.instance.getMouseY() > 392){
				mouseY-=130;
			}
		}
		short stabAtk = 0;
		int slashAtk = 0;
		int crushAtk = 0;
		int magicAtk = 0;
		int rangedAtk = 0;

		int stabDef = 0;
		int slashDef = 0;
		int crushDef = 0;
		int magicDef = 0;
		int rangedDef = 0;

		int prayerBonus = 0;
		int strengthBonus = 0;

		DrawingArea.drawBoxOutline(mouseX, mouseY + 5, 150, 120, 0x696969);
		DrawingArea.drawTransparentBox(mouseX + 1, mouseY + 6, 150, 121, 0x000000,90);

		Client.instance.newSmallFont.drawBasicString(itemName, mouseX + 150 / (12 +  Client.instance.newSmallFont.getTextWidth(itemName))+30 , mouseY + 17, color, 1);

		Client.instance.newSmallFont.drawBasicString("ATK", mouseX + 62, mouseY + 30, color, 1);
		Client.instance.newSmallFont.drawBasicString("DEF", mouseX + 112, mouseY + 30, color, 1);

		Client.instance.newSmallFont.drawBasicString("Stab", mouseX + 2, mouseY + 43, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(stabAtk), mouseX + 62, mouseY + 43, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(stabDef), mouseX + 112, mouseY + 43, color, 1);

		Client.instance.newSmallFont.drawBasicString("Slash", mouseX + 2, mouseY + 56, 0xFF00FF, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(slashAtk), mouseX + 62, mouseY + 56, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(slashDef), mouseX + 112, mouseY + 56, color, 1);


		Client.instance.newSmallFont.drawBasicString("Crush", mouseX + 2, mouseY + 69, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(crushAtk), mouseX + 62, mouseY + 69, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(crushDef), mouseX + 112, mouseY + 69, color, 1);


		Client.instance.newSmallFont.drawBasicString("Magic", mouseX + 2, mouseY + 80, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(magicAtk), mouseX + 62, mouseY + 80, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(magicDef), mouseX + 112, mouseY + 80, color, 1);


		Client.instance.newSmallFont.drawBasicString("Ranged", mouseX + 2, mouseY + 95, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(rangedAtk), mouseX + 62, mouseY + 95, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(rangedDef), mouseX + 112, mouseY + 95, color, 1);


		Client.instance.newSmallFont.drawBasicString("Strength", mouseX + 2, mouseY + 108, color, 1);
		Client.instance.newSmallFont.drawBasicString("Prayer", mouseX + 2, mouseY + 121, color, 1);

		Client.instance.newSmallFont.drawBasicString(Integer.toString(strengthBonus), mouseX + 112, mouseY + 108, color, 1);
		Client.instance.newSmallFont.drawBasicString(Integer.toString(prayerBonus), mouseX + 112, mouseY + 121, color, 1);


		Client.instance.newSmallFont.drawBasicString("Stab", mouseX + 2, mouseY + 43, color, 1);
		Client.instance.newSmallFont.drawBasicString("Slash", mouseX + 2, mouseY + 56, color, 1);
		Client.instance.newSmallFont.drawBasicString("Crush", mouseX + 2, mouseY + 69, color, 1);
		Client.instance.newSmallFont.drawBasicString("Magic", mouseX + 2, mouseY + 80, color, 1);
		Client.instance.newSmallFont.drawBasicString("Range", mouseX + 2, mouseY + 95, color, 1);
		Client.instance.newSmallFont.drawBasicString("Strength", mouseX + 2, mouseY + 108, color, 1);
		Client.instance.newSmallFont.drawBasicString("Prayer", mouseX + 2, mouseY + 121, color, 1);
	}

	private void buildChatAreaMenu(int j) {
		int l = 0;
		for (int i1 = 0; i1 < 500; i1++) {
			if (chatMessages[i1] == null)
				continue;
			int j1 = chatTypes[i1];
			int k1 = (70 - l * 14 + 42) + anInt1089 + 4 + 5;
			if (k1 < -23)
				break;
			String s = chatNames[i1];
			if (chatTypeView == 1) {
				buildPublicChat(j);
				break;
			}
			if (chatTypeView == 2) {
				buildFriendChat(j);
				break;
			}
			if (chatTypeView == 3 || chatTypeView == 4) {
				buildDuelorTrade(j);
				break;
			}
			if (chatTypeView == 5) {
				break;
			}
			while (s.startsWith("@cr")) {
				String s2 = s.substring(3, s.length());
				int index = s2.indexOf("@");
				if (index != -1) {
					s2 = s2.substring(0, index);
					s = s.substring(4 + s2.length());
				}
			}
			if (s != null && s.startsWith("<col=") && s.indexOf("</col>") != -1) {
				s = s.substring(s.indexOf("</col>") + 6);
			}
			if (j1 == 0)
				l++;
			if ((j1 == 1 || j1 == 2) && (j1 == 1 || publicChatMode == 0 || publicChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1 && !s.equals(myPlayer.displayName)) {
					if (myPlayer.hasRightsLevel(1)) {
						menuActionName[menuActionRow] = "Report abuse @whi@" + s;
						menuActionID[menuActionRow] = 606;
						menuActionRow++;
					}
					menuActionName[menuActionRow] = "Add ignore @whi@" + s;
					menuActionID[menuActionRow] = 42;
					menuActionRow++;
					menuActionName[menuActionRow] = "Reply to @whi@" + s;
					menuActionID[menuActionRow] = 639;
					menuActionRow++;
					menuActionName[menuActionRow] = "Add friend @whi@" + s;
					menuActionID[menuActionRow] = 337;
					menuActionRow++;
				}
				l++;
			}
			if ((j1 == 3 || j1 == 7) && !splitPrivateChat
					&& (j1 == 7 || privateChatMode == 0 || privateChatMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					if (myPlayer.hasRightsLevel(1)) {
						menuActionName[menuActionRow] = "Report abuse @whi@" + s;
						menuActionID[menuActionRow] = 606;
						menuActionRow++;
					}
					menuActionName[menuActionRow] = "Add ignore @whi@" + s;
					menuActionID[menuActionRow] = 42;
					menuActionRow++;
					menuActionName[menuActionRow] = "Reply to @whi@" + s;
					menuActionID[menuActionRow] = 639;
					menuActionRow++;
					menuActionName[menuActionRow] = "Add friend @whi@" + s;
					menuActionID[menuActionRow] = 337;
					menuActionRow++;
				}
				l++;
			}

			if (j1 == 99) {
				lastViewedDropTable = chatMessages[j] == null ? "" : chatMessages[j];
				menuActionName[menuActionRow] = "Open Drop Table";
				menuActionID[menuActionRow] = 10_992;
				menuActionRow++;
				l++;
			}

			// Trade
			if (j1 == 4 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Accept trade @whi@" + s;
					menuActionID[menuActionRow] = 484;
					menuActionRow++;
				}
				l++;
			}
			if ((j1 == 5 || j1 == 6) && !splitPrivateChat && privateChatMode < 2)
				l++;
			if (j1 == 8 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Accept challenge @whi@" + s;
					menuActionID[menuActionRow] = 6;
					menuActionRow++;
				}
				l++;
			}

			if (j1 == 31 && (tradeMode == 0 || tradeMode == 1 && isFriendOrSelf(s))) {
				if (j > k1 - 14 && j <= k1) {
					menuActionName[menuActionRow] = "Accept Gamble Request From@whi@ " + s;
					menuActionID[menuActionRow] = 11_501;
					menuActionRow++;
				}
				l++;
			}
		}
	}

	public String setSkillTooltip(int skillLevel) {
		String[] getToolTipText = new String[4];
		String setToolTipText = "";
		int maxLevel = 99;

		if (maxStats[skillLevel] > maxLevel) {
			if (skillLevel != 24) {
				maxStats[skillLevel] = 99;
			} else if (maxStats[skillLevel] > 120) {
				maxStats[skillLevel] = 120;
			}
		}

		NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
		int[] getSkillId = { 0, 0, 2, 1, 4, 5, 6, 20, 22, 3, 16, 15, 17, 12, 9, 18, 21, 14, 14, 13, 10, 7, 11, 8, 19,
				24 };

		if (!Skills.SKILL_NAMES[skillLevel].equals("-1")) {

			if (maxStats[getSkillId[skillLevel]] >= 99) {
				getToolTipText[0] = Skills.SKILL_NAMES[skillLevel] + " XP: "
						+ numberFormat.format(currentExp[getSkillId[skillLevel]]) + "\\n";
				setToolTipText = getToolTipText[0];
			} else {
				getToolTipText[0] = Skills.SKILL_NAMES[skillLevel] + " XP: " + "\\r"
						+ numberFormat.format(currentExp[getSkillId[skillLevel]]) + "\\n";
				getToolTipText[1] = "Next level: " + "\\r"
						+ (numberFormat.format(getXPForLevel(maxStats[getSkillId[skillLevel]] + 1))) + "\\n";
				getToolTipText[2] = "Remainder: " + "\\r" + numberFormat.format(
						getXPForLevel(maxStats[getSkillId[skillLevel]] + 1) - currentExp[getSkillId[skillLevel]])
						+ "\\n";
				getToolTipText[3] = "";

				setToolTipText = getToolTipText[0] + getToolTipText[1] + getToolTipText[2];
			}
		} else {
			setToolTipText = "Click here to logout";
		}
		return setToolTipText;
	}

	public void drawFriendsListOrWelcomeScreen(RSInterface class9) {
		int j = class9.contentType;
		if ((j >= 205) && (j <= (205 + 25))) {
			j -= 205;
			class9.message = setSkillTooltip(j);
			return;
		}
		if (j >= 1 && j <= 100 || j >= 701 && j <= 800) {
			if (j == 1 && anInt900 == 0) {
				class9.message = "Loading friend list";
				class9.atActionType = 0;
				return;
			}
			if (j == 1 && anInt900 == 1) {
				class9.message = "Connecting to friendserver";
				class9.atActionType = 0;
				return;
			}
			if (j == 2 && anInt900 != 2) {
				class9.message = "Please wait...";
				class9.atActionType = 0;
				return;
			}
			int k = friendsCount;
			if (anInt900 != 2)
				k = 0;
			if (j > 700)
				j -= 601;
			else
				j--;
			if (j >= k) {
				class9.message = "";
				class9.atActionType = 0;
				return;
			} else {
				class9.message = friendsList[j];
				class9.atActionType = 1;
				return;
			}
		}
		if (j >= 101 && j <= 200 || j >= 801 && j <= 900) {
			int l = friendsCount;
			if (anInt900 != 2)
				l = 0;
			if (j > 800)
				j -= 701;
			else
				j -= 101;
			if (j >= l) {
				class9.message = "";
				class9.atActionType = 0;
				return;
			}
			if (friendsNodeIDs[j] == 0)
				class9.message = "@red@Offline";
			else if (friendsNodeIDs[j] == nodeID)
				class9.message = "@gre@Online";
			else
				class9.message = "@red@Offline";
			class9.atActionType = 1;
			return;
		}
		if (j == 203) {
			int i1 = friendsCount;
			if (anInt900 != 2)
				i1 = 0;
			class9.scrollMax = i1 * 15 + 20;
			if (class9.scrollMax <= class9.height)
				class9.scrollMax = class9.height + 1;
			return;
		}
		if (j >= 401 && j <= 500) {
			if ((j -= 401) == 0 && anInt900 == 0) {
				class9.message = "Loading ignore list";
				class9.atActionType = 0;
				return;
			}
			if (j == 1 && anInt900 == 0) {
				class9.message = "Please wait...";
				class9.atActionType = 0;
				return;
			}
			int j1 = ignoreCount;
			if (anInt900 == 0)
				j1 = 0;
			if (j >= j1) {
				class9.message = "";
				class9.atActionType = 0;
				return;
			} else {
				class9.message = StringUtils.fixName(StringUtils.nameForLong(ignoreListAsLongs[j]));
				class9.atActionType = 1;
				return;
			}
		}
		if (j == 503) {
			class9.scrollMax = ignoreCount * 15 + 20;
			if (class9.scrollMax <= class9.height)
				class9.scrollMax = class9.height + 1;
			return;
		}
		if (j == 327) {
			class9.modelRotation1 = 150;
			class9.modelRotation2 = (int) (Math.sin(update_tick / 40D) * 256D) & 0x7ff;
			if (aBoolean1031) {
				for (int k1 = 0; k1 < 7; k1++) {
					int l1 = anIntArray1065[k1];
					if (l1 >= 0 && !IDK.cache[l1].method537())
						return;
				}

				aBoolean1031 = false;
				Model aclass30_sub2_sub4_sub6s[] = new Model[7];
				int i2 = 0;
				for (int j2 = 0; j2 < 7; j2++) {
					int k2 = anIntArray1065[j2];
					if (k2 >= 0)
						aclass30_sub2_sub4_sub6s[i2++] = IDK.cache[k2].method538();
				}

				Model model = new Model(i2, aclass30_sub2_sub4_sub6s);
				for (int l2 = 0; l2 < 5; l2++)
					if (anIntArray990[l2] != 0) {
						model.recolor(anIntArrayArray1003[l2][0], anIntArrayArray1003[l2][anIntArray990[l2]]);
						if (l2 == 1)
							model.recolor(anIntArray1204[0], anIntArray1204[anIntArray990[l2]]);
						// if(l2 == 1)
						// model.method476(Legs2[0], Legs2[anIntArray990[l2]]);
					}

				AnimationDefinition.anims[myPlayer.readyanim].animate_iftype_model(model, 0);
				model.light(64, 1300, 0, -570, 0, true);
				class9.anInt233 = 5;
				class9.mediaID = 0;
				RSInterface.method208(aBoolean994, model);
			}
			return;
		}
		if (j == 328) {
			RSInterface rsInterface = class9;
			int verticleTilt = 150;
			int animationSpeed = (int) (Math.sin(update_tick / 40D) * 256D) & 0x7ff;
			rsInterface.modelRotation1 = verticleTilt;
			rsInterface.modelRotation2 = animationSpeed;
			if (aBoolean1031) {
				Model characterDisplay = myPlayer.method452();
				for (int l2 = 0; l2 < 5; l2++)
					if (anIntArray990[l2] != 0) {
						characterDisplay.recolor(anIntArrayArray1003[l2][0],
								anIntArrayArray1003[l2][anIntArray990[l2]]);
						if (l2 == 1)
							characterDisplay.recolor(anIntArray1204[0], anIntArray1204[anIntArray990[l2]]);
					}
				int staticFrame = myPlayer.readyanim;
				characterDisplay.apply_label_groups();
				characterDisplay.animate(AnimationDefinition.anims[staticFrame].anIntArray353[0]);
				 characterDisplay.light(64, 850, -30, -50, -30, true);
				rsInterface.anInt233 = 5;
				rsInterface.mediaID = 0;
				RSInterface.method208(aBoolean994, characterDisplay);
			}
			return;
		}
		if (j == 324) {
			if (aClass30_Sub2_Sub1_Sub1_931 == null) {
				aClass30_Sub2_Sub1_Sub1_931 = class9.sprite1;
				aClass30_Sub2_Sub1_Sub1_932 = class9.sprite2;
			}
			if (aBoolean1047) {
				class9.sprite1 = aClass30_Sub2_Sub1_Sub1_932;
				return;
			} else {
				class9.sprite1 = aClass30_Sub2_Sub1_Sub1_931;
				return;
			}
		}
		if (j == 325) {
			if (aClass30_Sub2_Sub1_Sub1_931 == null) {
				aClass30_Sub2_Sub1_Sub1_931 = class9.sprite1;
				aClass30_Sub2_Sub1_Sub1_932 = class9.sprite2;
			}
			if (aBoolean1047) {
				class9.sprite1 = aClass30_Sub2_Sub1_Sub1_931;
				return;
			} else {
				class9.sprite1 = aClass30_Sub2_Sub1_Sub1_932;
				return;
			}
		}
		if (j == 600) {
			class9.message = reportAbuseInput;
			if (update_tick % 20 < 10) {
				class9.message += "|";
				return;
			} else {
				class9.message += " ";
				return;
			}
		}
		if (j == 613)
			if (myPlayer.hasRightsLevel(1)) {
				if (canMute) {
					class9.textColor = 0xff0000;
					// class9.message =
					// "Moderator option: Mute player for 48 hours: <ON>";
				} else {
					class9.textColor = 0xffffff;
					// class9.message =
					// "Moderator option: Mute player for 48 hours: <OFF>";
				}
			} else {
				class9.message = "";
			}
		if (j == 650 || j == 655)
			if (anInt1193 != 0) {
				String s;
				if (daysSinceLastLogin == 0)
					s = "earlier today";
				else if (daysSinceLastLogin == 1)
					s = "yesterday";
				else
					s = daysSinceLastLogin + " days ago";
				class9.message = "You last logged in " + s + " from: " + Signlink.dns;
			} else {
				class9.message = "";
			}
		if (j == 651) {
			if (unreadMessages == 0) {
				class9.message = "0 unread messages";
				class9.textColor = 0xffff00;
			}
			if (unreadMessages == 1) {
				class9.message = "1 unread message";
				class9.textColor = 65280;
			}
			if (unreadMessages > 1) {
				class9.message = unreadMessages + " unread messages";
				class9.textColor = 65280;
			}
		}
		if (j == 652)
			if (daysSinceRecovChange == 201) {
				if (membersInt == 1)
					class9.message = "@yel@This is a non-members world: @whi@Since you are a member we";
				else
					class9.message = "";
			} else if (daysSinceRecovChange == 200) {
				class9.message = "You have not yet set any password recovery questions.";
			} else {
				String s1;
				if (daysSinceRecovChange == 0)
					s1 = "Earlier today";
				else if (daysSinceRecovChange == 1)
					s1 = "Yesterday";
				else
					s1 = daysSinceRecovChange + " days ago";
				class9.message = s1 + " you changed your recovery questions";
			}
		if (j == 653)
			if (daysSinceRecovChange == 201) {
				if (membersInt == 1)
					class9.message = "@whi@recommend you use a members world instead. You may use";
				else
					class9.message = "";
			} else if (daysSinceRecovChange == 200)
				class9.message = "We strongly recommend you do so now to secure your account.";
			else
				class9.message = "If you do not remember making this change then cancel it immediately";
		if (j == 654) {
			if (daysSinceRecovChange == 201)
				if (membersInt == 1) {
					class9.message = "@whi@this world but member benefits are unavailable whilst here.";
					return;
				} else {
					class9.message = "";
					return;
				}
			if (daysSinceRecovChange == 200) {
				class9.message = "Do this from the 'account management' area on our front webpage";
				return;
			}
			class9.message = "Do this from the 'account management' area on our front webpage";
		}
	}

	private void sortFriendsList() {
		for (boolean flag6 = false; !flag6;) {
			flag6 = true;
			for (int k29 = 0; k29 < friendsCount - 1; k29++)
				if (friendsNodeIDs[k29] != nodeID && friendsNodeIDs[k29 + 1] == nodeID
						|| friendsNodeIDs[k29] == 0 && friendsNodeIDs[k29 + 1] != 0) {
					int j31 = friendsNodeIDs[k29];
					friendsNodeIDs[k29] = friendsNodeIDs[k29 + 1];
					friendsNodeIDs[k29 + 1] = j31;
					String s10 = friendsList[k29];
					friendsList[k29] = friendsList[k29 + 1];
					friendsList[k29 + 1] = s10;
					long l32 = friendsListAsLongs[k29];
					friendsListAsLongs[k29] = friendsListAsLongs[k29 + 1];
					friendsListAsLongs[k29 + 1] = l32;
					needDrawTabArea = true;
					flag6 = false;
				}
		}
	}

	private void drawSplitPrivateChat() {
		if (!splitPrivateChat)
			return;
		RSFont font = newRegularFont;

		int listPosition = 0;
		boolean update = anInt1104 != 0;
		boolean broadcast = BroadcastManager.isDisplayed();
		listPosition = update && broadcast ? 2 : broadcast || update ? 1 : 0;
		int xPosition = 0;
		int yPosition = 0;
		for (int j = 0; j < 100; j++) {
			if (chatMessages[j] != null) {
				int k = chatTypes[j];
				String s = chatNames[j];
				List<Integer> crowns = new ArrayList<>();
				while (s.startsWith("@cr")) {
					String s2 = s.substring(3, s.length());
					int index = s2.indexOf("@");
					if (index != -1) {
						s2 = s2.substring(0, index);
						crowns.add(Integer.parseInt(s2));
						s = s.substring(4 + s2.length());
					}
				}
				if ((k == 3 || k == 7) && (k == 7 || privateChatMode == 0 || privateChatMode == 1 && isFriendOrSelf(s))) {
					yPosition = (currentScreenMode == ScreenMode.FIXED ? 330 : currentGameHeight - 173) - listPosition * 13;
					xPosition = currentScreenMode == ScreenMode.FIXED ? 5 : 0;
					font.drawBasicString("From", xPosition, yPosition, 65535, 0);
					xPosition += font.getTextWidth("From ");
					if (!crowns.isEmpty()) {
						for (int crown : crowns) {
							for (int right = 0; right < modIcons.length; right++) {
								if (right == (crown - 1) && modIcons[right] != null) {
									modIcons[right].drawAdvancedSprite(xPosition, yPosition - modIcons[right].myHeight);
									xPosition += modIcons[right].myWidth;
									xPosition += 2;
									break;
								}
							}
						}
					}
					font.drawBasicString(s + ": " + chatMessages[j], xPosition, yPosition, 65535, 0);
					if (++listPosition >= 5)
						return;
				}
				if (k == 5 && privateChatMode < 2) {
					yPosition = (currentScreenMode == ScreenMode.FIXED ? 330 : currentGameHeight - 173) - listPosition * 13;
					xPosition = currentScreenMode == ScreenMode.FIXED ? 5 : 0;
					font.drawBasicString(chatMessages[j], xPosition, yPosition, 65535, 0);
					if (++listPosition >= 5)
						return;
				}
				if (k == 6 && privateChatMode < 2) {
					yPosition = (currentScreenMode == ScreenMode.FIXED ? 330 : currentGameHeight - 173) - listPosition * 13;
					xPosition = currentScreenMode == ScreenMode.FIXED ? 5 : 0;
					font.drawBasicString("To " + s + ": " + chatMessages[j], xPosition, yPosition, 65535, 0);
					if (++listPosition >= 5)
						return;
				}
			}
		}
	}

	private void buildSplitPrivateChatMenu() {
		if (!splitPrivateChat)
			return;
		int i = 0;
		if (anInt1104 != 0)
			i = 1;
		for (int j = 0; j < 100; j++) {
			if (chatMessages[j] != null) {
				int k = chatTypes[j];
				String s = chatNames[j];
				while (s.startsWith("@cr")) {
					String s2 = s.substring(3, s.length());
					int index = s2.indexOf("@");
					if (index != -1) {
						s2 = s2.substring(0, index);
						s = s.substring(4 + s2.length());
					}
				}
				if ((k == 3 || k == 7)
						&& (k == 7 || privateChatMode == 0 || privateChatMode == 1 && isFriendOrSelf(s))) {
					int yPosition = (currentScreenMode == ScreenMode.FIXED ? 330 : currentGameHeight - 173) - i * 13;
					int messageLength = newRegularFont.getTextWidth("From:  " + s + chatMessages[j]) + 25;
					if (super.getMouseX() >= 0 && super.getMouseX() <= messageLength) {
						if (super.getMouseY() >= yPosition - 10 && getMouseY() <= yPosition + 3) {
							if (messageLength > 450)
								messageLength = 450;
							if (myPlayer.hasRightsLevel(1)) {
								menuActionName[menuActionRow] = "Report abuse @whi@" + s;
								menuActionID[menuActionRow] = 2606;
								menuActionRow++;
							}
							menuActionName[menuActionRow] = "Add ignore @whi@" + s;
							menuActionID[menuActionRow] = 2042;
							menuActionRow++;
							menuActionName[menuActionRow] = "Reply to @whi@" + s;
							menuActionID[menuActionRow] = 2639;
							menuActionRow++;
							menuActionName[menuActionRow] = "Add friend @whi@" + s;
							menuActionID[menuActionRow] = 2337;
							menuActionRow++;
						}
					}
					if (++i >= 5)
						return;
				}
				if ((k == 5 || k == 6) && privateChatMode < 2 && ++i >= 5)
					return;
			}
		}
	}

	private void buildBroadcasts() {

		int i = 0;

		if (BroadcastManager.isDisplayed()) {

			Broadcast bc = BroadcastManager.getCurrentBroadcast();

			if (bc == null)
				return;

			boolean update = isServerUpdating();

			int yPosition = (currentScreenMode == ScreenMode.FIXED ? 330 : currentGameHeight - 173) - i * 13;
			yPosition -= update ? 13 : 0;
			int messageLength = newRegularFont.getTextWidth(bc.getDecoratedMessage());
			if (openInterfaceID <= 0 && super.getMouseX() >= 0 && super.getMouseX() <= messageLength) {
				if (super.getMouseY() >= yPosition - 10 && getMouseY() <= yPosition) {
					menuActionName[menuActionRow] = "Dismiss";
					menuActionID[menuActionRow] = 11_115;
					menuActionRow++;
					if (bc.type != 0) {
						menuActionName[menuActionRow] = bc.type == 1 ? "Open URL" : bc.type == 2 ? "Teleport To Location" : null;
						menuActionID[menuActionRow] = bc.type == 1 ? 11_111 : 11_112;
						menuActionRow++;
					}
				}
			}
			if (++i >= 1)
				return;
		}
	}

	public void timeOutHasLoggedMessages() {
		for (int i = 0; i < chatTimes.length; i++) {
			if (chatMessages[i] != null && chatMessages[i].contains("has logged") && System.currentTimeMillis() - chatTimes[i] >= 15_000) {
				chatMessages[i] = "";
				shiftMessages();
			}
		}
	}

	public void pushMessage(String s) {
		pushMessage(s, 0, "");
	}

	public void pushMessage(String s, int i, String s1) {
		if (i == 0 && dialogID != -1) {
			aString844 = s;
			super.clickMode3 = 0;
		}
		if (backDialogID == -1)
			inputTaken = true;
		for (int j = 499; j > 0; j--) {
			chatTypes[j] = chatTypes[j - 1];
			chatNames[j] = chatNames[j - 1];
			chatMessages[j] = chatMessages[j - 1];
			clanTitles[j] = clanTitles[j - 1];
			chatTimes[j] = chatTimes[j - 1];
		}
		chatTypes[0] = i;
		chatNames[0] = s1;
		chatMessages[0] = s;
		clanTitles[0] = clanTitle;
		chatTimes[0] = System.currentTimeMillis();
	}

	public void shiftMessages() {
		int index = 0;
		int[] chatTypes = new int[500];
		String[] chatNames = new String[500];
		String[] chatMessages = new String[500];
		String[] clanTitles = new String[500];
		long[] chatTimes = new long[500];

		for (int i = 0; i < chatTypes.length; i++) {
			if (this.chatMessages[i] != null && this.chatMessages[i].length() > 0) {
				chatMessages[index] = this.chatMessages[i];
				chatTypes[index] = this.chatTypes[i];
				chatNames[index] = this.chatNames[i];
				clanTitles[index] = this.clanTitles[i];
				chatTimes[index] = this.chatTimes[i];
				index++;
			}
		}

		this.chatTypes = chatTypes;
		this.chatMessages = chatMessages;
		this.chatNames = chatNames;
		this.clanTitles = clanTitles;
		this.chatTimes = chatTimes;
	}

	public void setNorth() {
		cameraOffsetX = 0;
		cameraOffsetY = 0;
		viewRotationOffset = 0;
		viewRotation = 0;
		minimapRotation = 0;
		minimapZoom = 0;
	}

	public void processTabClick() {
		if (clickMode3 == 1) {
			if (currentScreenMode == ScreenMode.FIXED) {
				int x = 516;
				int y = 168;
				int[] points = new int[] { 3, 41, 74, 107, 140, 173, 206, 244 };
				for (int index = 0; index < points.length - 1; index++) {
					if (super.getSaveClickX() >= x + points[index] && super.getSaveClickX() <= x + points[index + 1]) {
						if (super.getSaveClickY() >= y && super.getSaveClickY() <= y + 36) {
							if (Client.tabInterfaceIDs[index] != -1) {
								Client.tabID = index;
								Client.needDrawTabArea = true;
								Client.tabAreaAltered = true;
							}
						} else if (super.getSaveClickY() >= y + 298 && super.getSaveClickY() <= y + 36 + 298) {
							if (Client.tabInterfaceIDs[index + 7] != -1) {
								if (index + 7 == 13) {
									stream.createFrame(185);
									stream.writeWord(21406);
								}
								Client.tabID = index + 7;
								Client.needDrawTabArea = true;
								Client.tabAreaAltered = true;
							}
						}
						Client.needDrawTabArea = true;
						Client.tabAreaAltered = true;
					}
				}
			} else {
				int x = Client.currentGameWidth - (stackTabs() ? 231 : 462);
				int y = Client.currentGameHeight - (stackTabs() ? 73 : 37);
				for (int index = 0; index < 14; index++) {
					if (super.getSaveClickX() >= x && super.getSaveClickX() <= x + 33) {
						if (super.getSaveClickY() >= y && super.getSaveClickY() <= y + 36) {
							if (Client.tabInterfaceIDs[index] != -1) {
								Client.tabID = index;
								Client.needDrawTabArea = true;
								Client.tabAreaAltered = true;
							}
						} else if (stackTabs() && super.getSaveClickY() >= y + 36 && super.getSaveClickY() <= y + 36 + 36) {
							if (Client.tabInterfaceIDs[index + 7] != -1) {
								Client.tabID = index + 7;
								Client.needDrawTabArea = true;
								Client.tabAreaAltered = true;
							}
						}
					}
					x += 33;
				}
			}
		}
	}

	private void resetImageProducers2() {
	    if (chatAreaGraphicsBuffer != null)
	        return;
	    nullLoader();
	    loginScreenGraphicsBuffer = null;
	    fullGameScreen = null;
	    leftSideFlame = null;
	    rightSideFlame = null;
	    chatAreaGraphicsBuffer = new RSImageProducer(516, 165, getGameComponent());
	    mapAreaGraphicsBuffer = new RSImageProducer(249, 168, getGameComponent());
	    DrawingArea.setAllPixelsToZero();
	    tabAreaGraphicsBuffer = new RSImageProducer(249, 335, getGameComponent());
	    mainGameGraphicsBuffer = new GraphicsBuffer(
	        currentScreenMode == ScreenMode.FIXED ? 516 : currentScreenMode.getWidth(),
	        currentScreenMode == ScreenMode.FIXED ? 338 : currentScreenMode.getHeight());
	    DrawingArea.setAllPixelsToZero();
	    welcomeScreenRaised = true;
	}
	public String getDocumentBaseHost() {
		if (Signlink.mainapp != null) {
			return Signlink.mainapp.getDocumentBase().getHost().toLowerCase();
		}
		return "";
	}

	private void method81(Sprite sprite, int j, int k) {
		int l = k * k + j * j;
		if (l > 4225 && l < 0x15f90) {
			int i1 = viewRotation + minimapRotation & 0x7ff;
			int j1 = Model.SINE[i1];
			int k1 = Model.COSINE[i1];
			j1 = (j1 * 256) / (minimapZoom + 256);
			k1 = (k1 * 256) / (minimapZoom + 256);
			int l1 = j * j1 + k * k1 >> 16;
			int i2 = j * k1 - k * j1 >> 16;
			double d = Math.atan2(l1, i2);
			int j2 = (int) (Math.sin(d) * 63D);
			int k2 = (int) (Math.cos(d) * 57D);
			mapEdge.method353(83 - k2 - 20, d, (94 + j2 + 4) - 10);
		} else {
			markMinimap(sprite, k, j);
		}
	}

	private void rightClickChatButtons() {
		if (super.getMouseY() >= (currentScreenMode == ScreenMode.FIXED ? 482 : currentGameHeight - 22)
				&& super.getMouseY() <= (currentScreenMode == ScreenMode.FIXED ? 503 : currentGameHeight)) {

			if (super.getMouseX() >= 5 && super.getMouseX() <= 61) {
				menuActionName[1] = "View All";
				menuActionID[1] = 999;
				menuActionRow = 2;
			} else if (super.getMouseX() >= 71 && super.getMouseX() <= 127) {

				menuActionName[1] = "Switch tab";
				menuActionID[1] = 998;
				menuActionName[2] = "Configure filter";
				menuActionID[2] = 4005;
				menuActionName[3] = "@yel@Game: @whi@Filter";
				menuActionID[3] = 1005;
				menuActionRow = 4;
			} else if (super.getMouseX() >= 137 && super.getMouseX() <= 193) {
				menuActionName[1] = "@bl3@Setup your autochat";
				menuActionID[1] = 496;
				menuActionName[2] = "@gre@Filter public chat";
				menuActionID[2] = 495;
				menuActionName[3] = "@yel@Public: @whi@Clear history";
				menuActionID[3] = 497;
				menuActionName[4] = "@yel@Public: @whi@Hide";
				menuActionID[4] = 997;
				menuActionName[5] = "@yel@Public: @whi@Off";
				menuActionID[5] = 996;
				menuActionName[6] = "@yel@Public: @whi@Show friends";
				menuActionID[6] = 995;
				menuActionName[7] = "@yel@Public: @whi@Show standard";
				menuActionID[7] = 994;
				menuActionName[8] = "@yel@Public: @whi@Show autochat";
				menuActionID[8] = 494;
				menuActionName[9] = "Switch tab";
				menuActionID[9] = 993;
				menuActionRow = 10;
			} else if (super.getMouseX() >= 203 && super.getMouseX() <= 259) {
				menuActionName[1] = "@yel@Private: @whi@Clear history";
				menuActionID[1] = 1006;
				menuActionName[2] = "@yel@Private: @whi@Off";
				menuActionID[2] = 992;
				menuActionName[3] = "@yel@Private: @whi@Show friends";
				menuActionID[3] = 991;
				menuActionName[4] = "@yel@Private: @whi@Show all";
				menuActionID[4] = 990;
				menuActionName[5] = "Switch tab";
				menuActionID[5] = 989;
				menuActionRow = 6;
			} else if (super.getMouseX() >= 269 && super.getMouseX() <= 325) {
				menuActionName[1] = "@yel@Clan: @whi@Off";
				menuActionID[1] = 1003;
				menuActionName[2] = "@yel@Clan: @whi@Show friends";
				menuActionID[2] = 1002;
				menuActionName[3] = "@yel@Clan: @whi@Show all";
				menuActionID[3] = 1001;
				menuActionName[4] = "Switch tab";
				menuActionID[4] = 1000;
				menuActionRow = 5;
			} else if (super.getMouseX() >= 335 && super.getMouseX() <= 391) {
				menuActionName[1] = "@yel@Trade: @whi@Off";
				menuActionID[1] = 987;
				menuActionName[2] = "@yel@Trade: @whi@Show friends";
				menuActionID[2] = 986;
				menuActionName[3] = "@yel@Trade: @whi@Show all";
				menuActionID[3] = 985;
				menuActionName[4] = "Switch tab";
				menuActionID[4] = 984;
				menuActionRow = 5;
			}
		}
	}

	private boolean checkMainScreenBounds() {
		if (checkBounds(0, currentGameWidth - (stackTabs() ? 231 : 462), currentGameHeight - (stackTabs() ? 73 : 37),
				currentGameWidth, currentGameHeight)) {
			return false;
		}
		if (checkBounds(0, currentGameWidth - 225, 0, currentGameWidth, 170)) {
			return false;
		}
		if (checkBounds(0, currentGameWidth - 204, currentGameHeight - (stackTabs() ? 73 : 37) - 275, currentGameWidth,
				currentGameHeight)) {
			return false;
		}
		if (checkBounds(0, 0, currentGameHeight - 168, 516, currentGameHeight)) {
			return false;
		}
		return true;
	}

	private boolean checkBounds(int type, int x1, int y1, int x2, int y2) {
		if (type == 0)
			return getMouseX() >= x1 && getMouseX() <= x2 && getMouseY() >= y1 && getMouseY() <= y2;
		else
			return Math.pow((x1 + type - x2), 2) + Math.pow((y1 + type - y2), 2) < Math.pow(type, 2);
	}

	private boolean stackTabs() {
		return !(currentGameWidth >= 1100);
	}

	private void processRightClick() {
		if (loggedIn) {
			if (activeInterfaceType != 0)
				return;
			menuActionName[0] = "Cancel";
			menuActionID[0] = 1107;
			menuActionRow = 1;
			if (currentScreenMode != ScreenMode.FIXED) {
				if (fullscreenInterfaceID != -1) {
					anInt886 = 0;
					anInt1315 = 0;
					buildInterfaceMenu((currentGameWidth / 2) - 765 / 2,
							RSInterface.interfaceCache[fullscreenInterfaceID], super.getMouseX(),
							(currentGameHeight / 2) - 503 / 2, super.getMouseY(), 0);
					if (anInt886 != anInt1026) {
						anInt1026 = anInt886;
					}
					if (anInt1315 != anInt1129) {
						anInt1129 = anInt1315;
					}
					return;
				}
			}
			buildSplitPrivateChatMenu();
			buildBroadcasts();
			anInt886 = 0;
			anInt1315 = 0;

			if (currentScreenMode == ScreenMode.FIXED) {
				if (getMouseX() > 0 && getMouseY() > 0 && getMouseX() < 516 && getMouseY() < 343) {
					if (openInterfaceID != -1) {
						buildInterfaceMenu(0, RSInterface.interfaceCache[openInterfaceID], getMouseX(), 0, getMouseY(), 0);
					} else {
						build3dScreenMenu();
					}
				}
			} else {
				if (checkMainScreenBounds()) {
					int interfaceX = (currentGameWidth / 2) - 256 - 99;
					int interfaceY = (currentGameHeight / 2) - 167 - 63;
					if (openInterfaceID != -1) {
						buildInterfaceMenu(interfaceX, RSInterface.interfaceCache[openInterfaceID],
								getMouseX(), interfaceY, getMouseY(), 0);
					}

					if (openInterfaceID == -1 || getMouseX() < interfaceX || getMouseY() < interfaceY || getMouseX() > interfaceX + 516 || getMouseY() > interfaceY + 338) {
						build3dScreenMenu();
					}
				}
			}

			if (anInt1315 != anInt1129) {
				anInt1129 = anInt1315;
			}

			anInt886 = 0;
			anInt1315 = 0;
			resetTabInterfaceHover();
			if (currentScreenMode == ScreenMode.FIXED) {
				if (getMouseX() > 516 && getMouseY() > 205 && getMouseX() < 765 && getMouseY() < 466) {
					if (invOverlayInterfaceID != -1) {
						buildInterfaceMenu(547, RSInterface.interfaceCache[invOverlayInterfaceID], getMouseX(), 205, getMouseY(),
								0);
					} else if (tabInterfaceIDs[tabID] != -1) {
						buildInterfaceMenu(547, RSInterface.interfaceCache[tabInterfaceIDs[tabID]], getMouseX(), 205, getMouseY(),
								0);
					}
				}
			} else {
				int y = stackTabs() ? 73 : 37;
				if (getMouseX() > currentGameWidth - 197 && getMouseY() > currentGameHeight - 275 - y + 10
						&& getMouseX() < currentGameWidth - 7 && getMouseY() < currentGameHeight - y - 5) {
					if (invOverlayInterfaceID != -1) {
						buildInterfaceMenu(currentGameWidth - 197, RSInterface.interfaceCache[invOverlayInterfaceID],
								getMouseX(), currentGameHeight - 275 - y + 10, getMouseY(), 0);
					} else if (tabInterfaceIDs[tabID] != -1) {
						buildInterfaceMenu(currentGameWidth - 197, RSInterface.interfaceCache[tabInterfaceIDs[tabID]],
								getMouseX(), currentGameHeight - 275 - y + 10, getMouseY(), 0);
					}
				}
			}
			if (anInt886 != anInt1048) {
				needDrawTabArea = true;
				tabAreaAltered = true;
				anInt1048 = anInt886;
			}
			if (anInt1315 != anInt1044) {
				needDrawTabArea = true;
				tabAreaAltered = true;
				anInt1044 = anInt1315;
			}
			anInt886 = 0;
			anInt1315 = 0;
			/* Chat area clicking */
			if (currentScreenMode == ScreenMode.FIXED) {
				if (getMouseX() > 0 && getMouseY() > 338 && getMouseX() < 490 && getMouseY() < 463) {
					if (backDialogID != -1)
						buildInterfaceMenu(20, RSInterface.interfaceCache[backDialogID], getMouseX(), 358, getMouseY(), 0);
					else if (getMouseY() < 463 && getMouseX() < 490)
						buildChatAreaMenu(getMouseY() - 338);
				}
			} else {
				if (getMouseX() > 0 && getMouseY() > currentGameHeight - 165 && getMouseX() < 490 && getMouseY() < currentGameHeight - 40) {
					if (backDialogID != -1)
						buildInterfaceMenu(20, RSInterface.interfaceCache[backDialogID], getMouseX(),
								currentGameHeight - 145, getMouseY(), 0);
					else if (getMouseY() < currentGameHeight - 40 && getMouseX() < 490)
						buildChatAreaMenu(getMouseY() - (currentGameHeight - 165));
				}
			}
			if (backDialogID != -1 && anInt886 != anInt1039) {
				inputTaken = true;
				anInt1039 = anInt886;
			}
			if (backDialogID != -1 && anInt1315 != anInt1500) {
				inputTaken = true;
				anInt1500 = anInt1315;
			}
			/* Enable custom right click areas */
			if (anInt886 != anInt1026)
				anInt1026 = anInt886;
			anInt886 = 0;

			rightClickChatButtons();
			processMinimapActions();

			// Reverses menu actions!
			boolean flag = false;
			while (!flag) {
				flag = true;

				for (int j = 0; j < menuActionRow - 1; j++) {
					if (menuActionID[j] < 1000 && menuActionID[j + 1] > 1000) {
						String s = menuActionName[j];
						menuActionName[j] = menuActionName[j + 1];
						menuActionName[j + 1] = s;
						int k = menuActionID[j];
						menuActionID[j] = menuActionID[j + 1];
						menuActionID[j + 1] = k;
						k = menuActionCmd2[j];
						menuActionCmd2[j] = menuActionCmd2[j + 1];
						menuActionCmd2[j + 1] = k;
						k = menuActionCmd3[j];
						menuActionCmd3[j] = menuActionCmd3[j + 1];
						menuActionCmd3[j + 1] = k;
						long k2 = menuActionCmd1[j];
						menuActionCmd1[j] = menuActionCmd1[j + 1];
						menuActionCmd1[j + 1] = k2;
						flag = false;
					}
				}
			}
		}
	}

	private void processMinimapActions() {
		final boolean fixed = currentScreenMode == ScreenMode.FIXED;
		if (fixed ? super.getMouseX() >= 542 && super.getMouseX() <= 579 && super.getMouseY() >= 2 && super.getMouseY() <= 38
				: super.getMouseX() >= Client.currentGameWidth - 180 && super.getMouseX() <= Client.currentGameWidth - 139
				&& super.getMouseY() >= 0 && super.getMouseY() <= 40) {
			menuActionName[4] = "Look North";
			menuActionID[4] = 1150;
			menuActionName[3] = "Look South";
			menuActionID[3] = 1151;
			menuActionName[2] = "Look West";
			menuActionID[2] = 1152;
			menuActionName[1] = "Look East";
			menuActionID[1] = 1153;
			menuActionRow = 5;
		}
		if (counterHover && drawOrbs) {
			menuActionName[1] = "Reset @or1@XP total";
			menuActionID[1] = 475;
			menuActionName[2] = drawExperienceCounter ? "Hide @or1@XP drops" : "Show @or1@XP drops";
			menuActionID[2] = 474;
			menuActionRow = 3;
		}
		if (worldHover && drawOrbs) {
			menuActionName[2] = "Floating @or1@World Map";
			menuActionID[2] = 852;
			menuActionRow = 2;
			menuActionName[1] = "Fullscreen @or1@World Map";
			menuActionID[1] = 851;
			menuActionRow = 3;
		}
		if (wikiHover && drawOrbs) {
			menuActionName[1] = "search @or1@wiki";//2nd option
			menuActionID[1] = 855;
			menuActionRow = 2;
		}
		int mouseX1 = currentScreenMode == ScreenMode.FIXED ? 572 : currentGameWidth - 175;
		int mouseX2 = currentScreenMode == ScreenMode.FIXED ? 600 : currentGameWidth - 150;
		if (runHover) {
			menuActionName[1] = "Toggle Run";
			menuActionID[1] = 1050;
			menuActionRow = 2;
		}
		if (prayHover && drawOrbs) {
			menuActionName[2] = prayClicked ? "Turn Quick Prayers Off" : "Turn Quick Prayers On";
			menuActionID[2] = 1500;
			menuActionRow = 2;
			menuActionName[1] = "Select Quick Prayers";
			menuActionID[1] = 1506;
			menuActionRow = 3;
		}
	}

	private int method83(int i, int j, int k) {
		int l = 256 - k;
		return ((i & 0xff00ff) * l + (j & 0xff00ff) * k & 0xff00ff00)
				+ ((i & 0xff00) * l + (j & 0xff00) * k & 0xff0000) >> 8;
	}

	public boolean missingPassword() {
		if (getPassword() == null || getPassword().isEmpty()) {
			System.out.println("Empty password detected!");
			loginScreenCursorPos = 0;
			firstLoginMessage = "Please enter your password.";
			return true;
		}
		return false;
	}

	public boolean nameWhitespace() {
		if (myUsername != null && myUsername.startsWith(" ") || myUsername.endsWith(" ") || myUsername.contains("  ")) {
			firstLoginMessage = "Invalid username whitespace usage. Please try again.";
			return true;
		}
		return false;
	}

	public void login(String s, String s1, boolean flag) {
		if (loggedIn)
			return;
		if (Configuration.developerMode) {
			System.out.println("Attempting connection to " + server + ":" + port);
		}
		if (missingPassword()) {
			loginScreenState = LoginScreenState.LOGIN;
			return;
		}
		fogEnabled = false;
		fogOpacity = 0;
		firstLoginMessage = "Connecting to server..";
		Signlink.errorname = s;
		resetTabInterfaceHover();

		try {
			if(myUsername.length() < 3) {
				firstLoginMessage = "Your username must be at least 3 characters long";
				loginScreenState = LoginScreenState.LOGIN;
				return;
			}
			if (!flag) {
				drawLoginScreen(true);
			}
			setConfigButton(23103, informationFile.isRememberRoof() ? true : false);
			socketStream = new RSSocket(this, openSocket(port + portOff));
			long l = StringUtils.longForName(s);
			int i = (int) (l >> 16 & 31L);
			stream.currentOffset = 0;
			stream.writeUnsignedByte(14);
			stream.writeUnsignedByte(i);
			socketStream.queueBytes(2, stream.buffer);
			for (int j = 0; j < 8; j++)
				socketStream.read();

			int responseCode = socketStream.read();
			int i1 = responseCode;
			if (responseCode == 0) {
				new Identity().createIdentity();
				try {
					new Identity().loadIdentity();
				} catch (FileNotFoundException fnfe) {
					fnfe.printStackTrace();
				}
				socketStream.flushInputStream(inStream.buffer, 8);
				inStream.currentOffset = 0;
				aLong1215 = inStream.readQWord();

				// Pow request
				stream.currentOffset = 0;
				stream.writeUnsignedByte(19);
				socketStream.queueBytes(1, stream.buffer);
				int powRequestReturnCode = socketStream.read();
				if (powRequestReturnCode == 60) {
					int size = ((socketStream.read()) << 8) + (socketStream.read() & 0xff);
					socketStream.flushInputStream(inStream.buffer, size);
					inStream.currentOffset = 0;
					int randomValue = inStream.readUShort();
					int difficulty = inStream.readUShort();
					String seed = inStream.readNewString();

					// Pow process
					long start = System.currentTimeMillis();
					long answer = Hash.run(randomValue, difficulty, seed);
					long time = System.currentTimeMillis() - start;
					logger.debug("Took {}ms to finish POW.", time);

					// Pow check
					Buffer powStream = Buffer.create();
					powStream.writeByte(20); // Pow check opcode
					powStream.writeQWord(answer);
					this.socketStream.queueBytes(powStream.currentOffset, powStream.buffer);

					int response = socketStream.read();
					if (response == 0) {
						socketStream.flushInputStream(inStream.buffer, 8);
						inStream.currentOffset = 0;
						inStream.readQWord();
					}

					int ai[] = new int[4];
					ai[0] = (int) (Math.random() * 99999999D);
					ai[1] = (int) (Math.random() * 99999999D);
					ai[2] = (int) (aLong1215 >> 32);
					ai[3] = (int) aLong1215;
					stream.currentOffset = 0;
					stream.writeUnsignedByte(10);
					stream.writeDWord(ai[0]);
					stream.writeDWord(ai[1]);
					stream.writeDWord(ai[2]);
					stream.writeDWord(ai[3]);
					stream.writeDWord(Signlink.uid);
					stream.writeString(s);
					stream.writeString(s1);
					stream.writeString("");
					stream.writeString(macAddress);
					stream.writeString(FingerPrint.getFingerprint());
					stream.doKeys();
					aStream_847.currentOffset = 0;
					if (flag)
						aStream_847.writeUnsignedByte(18);
					else
						aStream_847.writeUnsignedByte(16);
					aStream_847.writeUnsignedByte(stream.currentOffset + 36 + 1 + 1 + 2);
					aStream_847.writeUnsignedByte(255);
					aStream_847.writeWord(Configuration.CLIENT_VERSION);
					aStream_847.writeUnsignedByte(lowMem ? 1 : 0);
					for (int l1 = 0; l1 < 9; l1++)
						aStream_847.writeDWord(expectedCRCs[l1]);

					aStream_847.writeBytes(stream.buffer, stream.currentOffset, 0);
					stream.encryption = new ISAACRandomGen(ai);
					for (int j2 = 0; j2 < 4; j2++)
						ai[j2] += 50;

					encryption = new ISAACRandomGen(ai);
					socketStream.queueBytes(aStream_847.currentOffset, aStream_847.buffer);
					responseCode = socketStream.read();
				}
			}
			if (responseCode == 1) {
				try {
					Thread.sleep(2000L);
				} catch (Exception _ex) {
				}
				login(s, s1, flag);
				return;
			}
			if (responseCode == 2) {
				Arrays.stream(RSInterface.interfaceCache).filter(Objects::nonNull).forEach(rsi -> rsi.scrollPosition = 0);

				Preferences.getPreferences().updateClientConfiguration();
				Preferences.getPreferences().updateClientConfiguration();
				@SuppressWarnings("unused")
				int rights = socketStream.read();
				flagged = socketStream.read() == 1;
				logger.debug("Login accepted, rights={}, flagged={}", rights, flagged);
				aLong1220 = 0L;
				anInt1022 = 0;
				Signlink.midiplay = false;
				stopMidi();
				//mouseDetection.coordsIndex = 0;
				super.awtFocus = true;
				aBoolean954 = true;
				loggedIn = true;
				stream.currentOffset = 0;
				inStream.currentOffset = 0;
				incomingPacket = -1;
				this.currentSound = 0;
				dealtWithPacket = -1;
				previousPacket1 = -1;
				dealtWithPacketSize = -1;
				previousPacket2 = -1;
				previousPacketSize1 = -1;
				previousPacketSize2 = -1;
				packetSize = 0;
				anInt1009 = 0;
				anInt1104 = 0;
				anInt1011 = 0;
				hintType = 0;
				menuActionRow = 0;
				menuOpen = false;
				super.idleTime = 0;
				for (int j1 = 0; j1 < 500; j1++)
					chatMessages[j1] = null;

				itemSelected = 0;
				spellSelected = 0;
				loadingStage = 0;
				setNorth();
				updateGameScreen();
				minimapState = 0;
				anInt985 = -1;
				destX = 0;
				destY = 0;
				playerCount = 0;
				npcCount = 0;
				for (int i2 = 0; i2 < maxPlayers; i2++) {
					players[i2] = null;
					aStreamArray895s[i2] = null;
				}

				for (int k2 = 0; k2 < 16384; k2++)
					npcs[k2] = null;

				myPlayer = players[maxPlayerCount] = new Player();
				aClass19_1013.removeAll();
				aClass19_1056.removeAll();
				for (int l2 = 0; l2 < 4; l2++) {
					for (int i3 = 0; i3 < 104; i3++) {
						for (int k3 = 0; k3 < 104; k3++)
							groundItems[l2][i3][k3] = null;

					}

				}

				spawns = new NodeList();
				fullscreenInterfaceID = -1;
				anInt900 = 0;
				friendsCount = 0;
				dialogID = -1;
				backDialogID = -1;
				openInterfaceID = -1;
				invOverlayInterfaceID = -1;
				openWalkableWidgetID = -1;
				aBoolean1149 = false;
				tabID = 3;
				inputDialogState = 0;
				menuOpen = false;
				messagePromptRaised = false;
				aString844 = null;
				anInt1055 = 0;
				aBoolean1047 = true;
				method45();
				for (int j3 = 0; j3 < 5; j3++)
					anIntArray990[j3] = 0;

				for (int l3 = 0; l3 < 6; l3++) {
					atPlayerActions[l3] = null;
					atPlayerArray[l3] = false;
				}

				anInt1175 = 0;
				anInt1134 = 0;
				anInt986 = 0;
				anInt1288 = 0;
				anInt924 = 0;
				anInt1188 = 0;
				anInt1155 = 0;
				anInt1226 = 0;
				updateGameScreen();
				resetImageProducers2();

				if (informationFile.isUsernameRemembered()) {
					informationFile.setStoredUsername(myUsername);
				}

				if (informationFile.isPasswordRemembered()) {
					informationFile.setStoredPassword(getPassword());
				}

				informationFile.write();
				Preferences.getPreferences().updateScreenMode();
				if (Client.currentScreenMode == ScreenMode.FIXED && getUserSettings().isStretchedMode()) {
					StretchedModeMenu.updateStretchedMode(true);
				}
				return;
			}
			if (responseCode == 3) {
				loginScreenState = LoginScreenState.LOGIN;
				if (missingPassword()) {
					return;
				}
				if (nameWhitespace()) {
					return;
				}
				firstLoginMessage = "Invalid username or password.";
				return;
			}
			if (responseCode == 4) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Your account has been disabled.";
				return;
			}
			if (responseCode == 5) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Your account is already logged in.";
				return;
			}
			if (responseCode == 6) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = Configuration.CLIENT_TITLE + " has been updated - restart the launcher!";
				return;
			}
			if (responseCode == 7) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "This world is full.";
				return;
			}
			if (responseCode == 8) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unable to connect.";
				return;
			}
			if (responseCode == 9) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Login limit exceeded.";
				return;
			}
			if (responseCode == 10) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unable to connect. Bad session id.";
				return;
			}
			if (responseCode == 11) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Your username is too long.";
				return;
			}
			if (responseCode == 12) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 12.";
				return;
			}
			if (responseCode == 13) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Too many connection attempts, please wait before trying again.";
				return;
			}
			if (responseCode == 14) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = Configuration.CLIENT_TITLE + " is currently being updated.";
				return;
			}
			if (responseCode == 15) {
				loginScreenState = LoginScreenState.LOGIN;
				loggedIn = true;
				stream.currentOffset = 0;
				inStream.currentOffset = 0;
				incomingPacket = -1;
				dealtWithPacket = -1;
				previousPacket1 = -1;
				previousPacket2 = -1;
				packetSize = 0;
				anInt1009 = 0;
				anInt1104 = 0;
				menuActionRow = 0;
				menuOpen = false;
				longStartTime = System.currentTimeMillis();
				return;
			}
			if (responseCode == 16) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 16.";
				return;
			}
			if (responseCode == 17) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 17.";
				return;
			}
			if (responseCode == 20) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 20.";
				return;
			}
			if (responseCode == 21) {
				loginScreenState = LoginScreenState.LOGIN;
				for (int k1 = socketStream.read(); k1 >= 0; k1--) {

					drawLoginScreen(true);
					try {
						Thread.sleep(1000L);
					} catch (Exception _ex) {
					}
				}

				login(s, s1, flag);
				return;
			}
			if (responseCode == 22) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 22.";
				return;
			}
			if (responseCode == 23) {
				loginScreenState = LoginScreenState.LOGIN;
				for (int k1 = socketStream.read(); k1 >= 0; k1--) {

					drawLoginScreen(true);
					try {
						Thread.sleep(1000L);
					} catch (Exception _ex) {
					}
				}

				login(s, s1, flag);
				return;
			}
			if (responseCode == 24) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 24.";
				return;
			}
			if (responseCode == 25) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "Unknown error 25.";
				return;
			}
			if (responseCode == 26) {
				loginScreenState = LoginScreenState.LOGIN;
				firstLoginMessage = "An error occurred while loading your file, contact support.";
				return;
			}
			if (responseCode == -1) {
				if (i1 == 0) {
					if (loginFailures < 2) {
						try {
							Thread.sleep(2000L);
						} catch (Exception _ex) {
						}
						loginFailures++;
						login(s, s1, flag);
						return;
					}
				} else {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ignored) { }
					firstLoginMessage = "Error connecting to server, please try again.";
					return;
				}
			} else {
				return;
			}
		} catch (IOException _ex) {
			if (Configuration.developerMode)
				_ex.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (missingPassword()) {
			loginScreenState = LoginScreenState.LOGIN;
			return;
		}
		loginScreenState = LoginScreenState.LOGIN;
		firstLoginMessage = "";
	}


	// Original signature int clickType, int j, int k, int i1, int localY, int k1, int l1, int i2, int localX, boolean flag, int k2
	private boolean doWalkTo(int clickType, int localX, int localY, int j, int k, int i1, int k1, int l1, int i2, boolean flag, int k2) {
		byte byte0 = 104;
		byte byte1 = 104;
		for (int l2 = 0; l2 < byte0; l2++) {
			for (int i3 = 0; i3 < byte1; i3++) {
				anIntArrayArray901[l2][i3] = 0;
				anIntArrayArray825[l2][i3] = 0x5f5e0ff;
			}
		}
		int j3 = localX;
		int k3 = localY;
		anIntArrayArray901[localX][localY] = 99;
		anIntArrayArray825[localX][localY] = 0;
		int l3 = 0;
		int i4 = 0;
		bigX[l3] = localX;
		bigY[l3++] = localY;
		boolean flag1 = false;
		int j4 = bigX.length;
		int ai[][] = collisionMaps[plane].clipData;
		while (i4 != l3) {
			j3 = bigX[i4];
			k3 = bigY[i4];
			i4 = (i4 + 1) % j4;
			if (j3 == k2 && k3 == i2) {
				flag1 = true;
				break;
			}
			if (i1 != 0) {
				if ((i1 < 5 || i1 == 10) && collisionMaps[plane].method219(k2, j3, k3, j, i1 - 1, i2)) {
					flag1 = true;
					break;
				}
				if (i1 < 10 && collisionMaps[plane].method220(k2, i2, k3, i1 - 1, j, j3)) {
					flag1 = true;
					break;
				}
			}
			if (k1 != 0 && k != 0 && collisionMaps[plane].method221(i2, k2, j3, k, l1, k1, k3)) {
				flag1 = true;
				break;
			}
			int l4 = anIntArrayArray825[j3][k3] + 1;
			if (j3 > 0 && anIntArrayArray901[j3 - 1][k3] == 0 && (ai[j3 - 1][k3] & 0x1280108) == 0) {
				bigX[l3] = j3 - 1;
				bigY[l3] = k3;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3 - 1][k3] = 2;
				anIntArrayArray825[j3 - 1][k3] = l4;
			}
			if (j3 < byte0 - 1 && anIntArrayArray901[j3 + 1][k3] == 0 && (ai[j3 + 1][k3] & 0x1280180) == 0) {
				bigX[l3] = j3 + 1;
				bigY[l3] = k3;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3 + 1][k3] = 8;
				anIntArrayArray825[j3 + 1][k3] = l4;
			}
			if (k3 > 0 && anIntArrayArray901[j3][k3 - 1] == 0 && (ai[j3][k3 - 1] & 0x1280102) == 0) {
				bigX[l3] = j3;
				bigY[l3] = k3 - 1;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3][k3 - 1] = 1;
				anIntArrayArray825[j3][k3 - 1] = l4;
			}
			if (k3 < byte1 - 1 && anIntArrayArray901[j3][k3 + 1] == 0 && (ai[j3][k3 + 1] & 0x1280120) == 0) {
				bigX[l3] = j3;
				bigY[l3] = k3 + 1;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3][k3 + 1] = 4;
				anIntArrayArray825[j3][k3 + 1] = l4;
			}
			if (j3 > 0 && k3 > 0 && anIntArrayArray901[j3 - 1][k3 - 1] == 0 && (ai[j3 - 1][k3 - 1] & 0x128010e) == 0
					&& (ai[j3 - 1][k3] & 0x1280108) == 0 && (ai[j3][k3 - 1] & 0x1280102) == 0) {
				bigX[l3] = j3 - 1;
				bigY[l3] = k3 - 1;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3 - 1][k3 - 1] = 3;
				anIntArrayArray825[j3 - 1][k3 - 1] = l4;
			}
			if (j3 < byte0 - 1 && k3 > 0 && anIntArrayArray901[j3 + 1][k3 - 1] == 0
					&& (ai[j3 + 1][k3 - 1] & 0x1280183) == 0 && (ai[j3 + 1][k3] & 0x1280180) == 0
					&& (ai[j3][k3 - 1] & 0x1280102) == 0) {
				bigX[l3] = j3 + 1;
				bigY[l3] = k3 - 1;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3 + 1][k3 - 1] = 9;
				anIntArrayArray825[j3 + 1][k3 - 1] = l4;
			}
			if (j3 > 0 && k3 < byte1 - 1 && anIntArrayArray901[j3 - 1][k3 + 1] == 0
					&& (ai[j3 - 1][k3 + 1] & 0x1280138) == 0 && (ai[j3 - 1][k3] & 0x1280108) == 0
					&& (ai[j3][k3 + 1] & 0x1280120) == 0) {
				bigX[l3] = j3 - 1;
				bigY[l3] = k3 + 1;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3 - 1][k3 + 1] = 6;
				anIntArrayArray825[j3 - 1][k3 + 1] = l4;
			}
			if (j3 < byte0 - 1 && k3 < byte1 - 1 && anIntArrayArray901[j3 + 1][k3 + 1] == 0
					&& (ai[j3 + 1][k3 + 1] & 0x12801e0) == 0 && (ai[j3 + 1][k3] & 0x1280180) == 0
					&& (ai[j3][k3 + 1] & 0x1280120) == 0) {
				bigX[l3] = j3 + 1;
				bigY[l3] = k3 + 1;
				l3 = (l3 + 1) % j4;
				anIntArrayArray901[j3 + 1][k3 + 1] = 12;
				anIntArrayArray825[j3 + 1][k3 + 1] = l4;
			}
		}
		anInt1264 = 0;
		if (!flag1) {
			if (flag) {
				int i5 = 100;
				for (int k5 = 1; k5 < 2; k5++) {
					for (int i6 = k2 - k5; i6 <= k2 + k5; i6++) {
						for (int l6 = i2 - k5; l6 <= i2 + k5; l6++)
							if (i6 >= 0 && l6 >= 0 && i6 < 104 && l6 < 104 && anIntArrayArray825[i6][l6] < i5) {
								i5 = anIntArrayArray825[i6][l6];
								j3 = i6;
								k3 = l6;
								anInt1264 = 1;
								flag1 = true;
							}

					}

					if (flag1)
						break;
				}

			}
			if (!flag1)
				return false;
		}
		i4 = 0;
		bigX[i4] = j3;
		bigY[i4++] = k3;
		int l5;
		for (int j5 = l5 = anIntArrayArray901[j3][k3]; j3 != localX || k3 != localY; j5 = anIntArrayArray901[j3][k3]) {
			if (j5 != l5) {
				l5 = j5;
				bigX[i4] = j3;
				bigY[i4++] = k3;
			}
			if ((j5 & 2) != 0)
				j3++;
			else if ((j5 & 8) != 0)
				j3--;
			if ((j5 & 1) != 0)
				k3++;
			else if ((j5 & 4) != 0)
				k3--;
		}
		// if(cancelWalk) { return i4 > 0; }

		if (i4 > 0) {
			int k4 = i4;
			if (k4 > 25)
				k4 = 25;
			i4--;
			int k6 = bigX[i4];
			int i7 = bigY[i4];
			anInt1288 += k4;
			if (anInt1288 >= 92) {
				stream.createFrame(36);
				stream.writeDWord(0);
				anInt1288 = 0;
			}
			if (clickType == 0) {
				stream.createFrame(164);
				stream.writeUnsignedByte(k4 + k4 + 3);
			}
			if (clickType == 1) {
				stream.createFrame(248);
				stream.writeUnsignedByte(k4 + k4 + 3 + 14);
			}
			if (clickType == 2) {
				stream.createFrame(98);
				stream.writeUnsignedByte(k4 + k4 + 3);
			}
			stream.method433(k6 + baseX);
			destX = bigX[0];
			destY = bigY[0];
			for (int j7 = 1; j7 < k4; j7++) {
				i4--;
				stream.writeUnsignedByte(bigX[i4] - k6);
				stream.writeUnsignedByte(bigY[i4] - i7);
			}

			stream.method431(i7 + baseY);
			stream.method424(super.keyArray[5] != 1 ? 0 : 1);
			return true;
		}
		return clickType != 1;
	}

	public void updateNpcState(Buffer stream) {
		for (int j = 0; j < anInt893; j++) {
			int k = anIntArray894[j];
			NPC npc = npcs[k];
			int l = stream.readUShort();
			if((l & 0x100) != 0)
			{
				npc.exactmove_x1 = stream.readUnsignedByte();
				npc.exactmove_y1 = stream.readUnsignedByte();
				npc.anInt1544 = stream.readUnsignedByte();
				npc.anInt1546 = stream.readUnsignedByte();
				npc.exactmove_start = stream.readUShort() + update_tick;
				npc.exactmove_end = stream.readUShort() + update_tick;
				npc.forceMovementDirection = stream.readUnsignedByte();
				npc.method446();
			}
			if ((l & 0x10) != 0) {
				int i1 = stream.method434();
				if (i1 == 65535)
					i1 = -1;
				int i2 = stream.readUnsignedByte();
				if (i1 == npc.primaryanim && i1 != -1) {
					int l2 = AnimationDefinition.anims[i1].looptype;
					if (l2 == 1) {
						npc.primaryanim_frameindex = 0;
						npc.primaryanim_loops_remaining = 0;
						npc.primaryanim_pause = i2;
						npc.primaryanim_replaycount = 0;
					}
					if (l2 == 2)
						npc.primaryanim_replaycount = 0;
				} else if (i1 == -1 || npc.primaryanim == -1
						|| AnimationDefinition.anims[i1].anInt359 >= AnimationDefinition.anims[npc.primaryanim].anInt359) {
					npc.primaryanim = i1;
					npc.primaryanim_frameindex = 0;
					npc.primaryanim_loops_remaining = 0;
					npc.primaryanim_pause = i2;
					npc.primaryanim_replaycount = 0;
					npc.anim_delay = npc.waypoint_count;
				}
			}
			if ((l & 8) != 0) {
				int j1 = stream.method426();
				int j2 = stream.method427();
				npc.updateHitData(j2, j1, update_tick);
				npc.loopCycleStatus = update_tick + 300;
				npc.currentHealth = stream.readUShort();
				npc.maxHealth = stream.readUShort();
			}
			if ((l & 0x80) != 0) {
				npc.spotanim = stream.readUShort();
				int k1 = stream.readDWord();
				npc.spotanim_height = k1 >> 16;
				npc.spotanim_start_loop = update_tick + (k1 & 0xffff);
				npc.spotanimframe_index = 0;
				npc.spotanim_loop = 0;
				if (npc.spotanim_start_loop > update_tick)
					npc.spotanimframe_index = -1;
				if (npc.spotanim == 65535)
					npc.spotanim = -1;
			}
			if ((l & 0x20) != 0) {
				npc.interactingEntity = stream.readUShort();
				if (npc.interactingEntity == 65535)
					npc.interactingEntity = -1;
			}
			if ((l & 1) != 0) {
				npc.textSpoken = stream.readString();
				npc.textCycle = 100;
			}
			if ((l & 0x40) != 0) {
				int l1 = stream.method427();
				int k2 = stream.method428();
				npc.updateHitData(k2, l1, update_tick);
				npc.loopCycleStatus = update_tick + 300;
				npc.currentHealth = stream.readUShort();
				npc.maxHealth = stream.readUShort();
			}
			if ((l & 2) != 0) {
				npc.desc = NpcDefinition.forID(stream.method436());
				npc.size = npc.desc.size;
				npc.turnspeed = npc.desc.getDegreesToTurn;
				npc.walkanim = npc.desc.walkAnimation;
				npc.walkanim_b = npc.desc.rotate180Animation;
				npc.walkanim_l = npc.desc.rotate90RightAnimation;
				npc.walkanim_r = npc.desc.rotate90LeftAnimation;
				npc.readyanim = npc.desc.standAnimation;
			}
			if ((l & 4) != 0) {
				npc.face_x = stream.method434();
				npc.face_y = stream.method434();
				npc.instant_facing = stream.readByte() == 1;
			}
		}
	}

	private long debugDelay;

	public void buildAtNPCMenu(NpcDefinition entityDef, int i, int j, int k) {
		if (menuActionRow >= 400)
			return;
		if (entityDef.childrenIDs != null)
			entityDef = entityDef.method161();
		if (entityDef != null) {
		if (entityDef.aBoolean84) {
				String s = entityDef.name;
				if (entityDef.combatLevel != 0)
					s = s + combatDiffColor(myPlayer.combatLevel, entityDef.combatLevel) + " (level-" + entityDef.combatLevel
							+ ")";
				if (itemSelected == 1) {
					menuActionName[menuActionRow] = "Use " + selectedItemName + " with @yel@" + s;
					menuActionID[menuActionRow] = 582;
					menuActionCmd1[menuActionRow] = i;
					menuActionCmd2[menuActionRow] = k;
					menuActionCmd3[menuActionRow] = j;
					menuActionRow++;
					return;
				}
				if (spellSelected == 1) {
					if ((spellUsableOn & 2) == 2) {
						menuActionName[menuActionRow] = spellTooltip + " @yel@" + s;
						menuActionID[menuActionRow] = 413;
						menuActionCmd1[menuActionRow] = i;
						menuActionCmd2[menuActionRow] = k;
						menuActionCmd3[menuActionRow] = j;
						menuActionRow++;
					}
				} else {
					if (entityDef.actions != null) {
						for (int l = 4; l >= 0; l--)
							if (entityDef.actions[l] != null && !entityDef.actions[l].equalsIgnoreCase("attack")) {
								menuActionName[menuActionRow] = entityDef.actions[l] + " @yel@" + s;
								if (l == 0)
									menuActionID[menuActionRow] = 20;
								if (l == 1)
									menuActionID[menuActionRow] = 412;
								if (l == 2)
									menuActionID[menuActionRow] = 225;
								if (l == 3)
									menuActionID[menuActionRow] = 965;
								if (l == 4)
									menuActionID[menuActionRow] = 478;
								menuActionCmd1[menuActionRow] = i;
								menuActionCmd2[menuActionRow] = k;
								menuActionCmd3[menuActionRow] = j;
								menuActionRow++;
							}

					}
					if (entityDef.actions != null) {
						for (int i1 = 4; i1 >= 0; i1--) {
							if (entityDef.actions[i1] != null && entityDef.actions[i1].equalsIgnoreCase("attack")) {
								char c = '\0';
								if (Configuration.npcAttackOptionPriority == 3)
									continue;
								if (Configuration.npcAttackOptionPriority == 0 && entityDef.combatLevel > myPlayer.combatLevel
										|| Configuration.npcAttackOptionPriority == 1)
									c = '\u07D0';
								menuActionName[menuActionRow] = entityDef.actions[i1] + " @yel@" + s;
								if (i1 == 0)
									menuActionID[menuActionRow] = 20 + c;
								if (i1 == 1)
									menuActionID[menuActionRow] = 412 + c;
								if (i1 == 2)
									menuActionID[menuActionRow] = 225 + c;
								if (i1 == 3)
									menuActionID[menuActionRow] = 965 + c;
								if (i1 == 4)
									menuActionID[menuActionRow] = 478 + c;
								menuActionCmd1[menuActionRow] = i;
								menuActionCmd2[menuActionRow] = k;
								menuActionCmd3[menuActionRow] = j;
								menuActionRow++;
							}
						}

					}
					if (clientData) {
						NPC npc = npcs[i];
						menuActionName[menuActionRow] = "Examine @yel@" + s
								+ " @whi@("
								+ "id=" + entityDef.npcId
								+ ", index=" + i
								+ ", stand=" + entityDef.standAnimation
								+ ", pos=[" + npc.getAbsoluteX() + ", " + npc.getAbsoluteY() + "]"
								+ ", size=" + entityDef.size
								+ ")";
					} else
						menuActionName[menuActionRow] = "Examine @yel@" + s;
					menuActionID[menuActionRow] = 1025;
					menuActionCmd1[menuActionRow] = i;
					menuActionCmd2[menuActionRow] = k;
					menuActionCmd3[menuActionRow] = j;
					menuActionRow++;

					if (debugModels == true) {
						if (System.currentTimeMillis() - debugDelay > 1000 && entityDef.models != null) {
							String modelIds = Arrays.toString(entityDef.models);
							String regColors = Arrays.toString(entityDef.recolorFrom);
							String newColors = Arrays.toString(entityDef.recolorTo);
							int standAnim = entityDef.standAnimation;
							int walkAnim = entityDef.walkAnimation;
							String name = entityDef.name;
							System.out.println(name + modelIds);
							pushMessage(name + ": " + modelIds, 0, "");
							pushMessage("Reg: " + regColors, 0, "");
							pushMessage("New: " + newColors, 0, "");
							pushMessage("stand: " + standAnim, 0, "");
							pushMessage("walk: " + walkAnim, 0, "");
							debugDelay = System.currentTimeMillis();
							// menuActionName[menuActionRow] = "Examine
							// @gre@O(@whi@"+Arrays.toString(entityDef.models)+")@gre@)";
							// menuActionName[menuActionRow] = "#2
							// (@whi@"+Arrays.toString(entityDef.originalColors)+")@gre@)(@whi@"+Arrays.toString(entityDef.newColors)+")@gre@)";
						}
					}
				}
		}
		}
	}

	public void buildAtPlayerMenu(int i, int j, Player player, int k) {
		if (player == myPlayer)
			return;
		if (!player.visible)
			return;
		if (menuActionRow >= 400)
			return;
		String s;
		if (player.title.length() < 0)
			s = player.displayName + combatDiffColor(myPlayer.combatLevel, player.combatLevel) + " (level: "
					+ player.combatLevel + ")";
		else if (player.title.length() != 0)
			s = "@or1@" + player.title + "@whi@ " + player.displayName
					+ combatDiffColor(myPlayer.combatLevel, player.combatLevel) + " (level: " + player.combatLevel
					+ ")";
		else
			s = player.displayName + combatDiffColor(myPlayer.combatLevel, player.combatLevel) + " (level: "
					+ player.combatLevel + ")";
		if (itemSelected == 1) {
			menuActionName[menuActionRow] = "Use " + selectedItemName + " with @whi@" + s;
			menuActionID[menuActionRow] = 491;
			menuActionCmd1[menuActionRow] = j;
			menuActionCmd2[menuActionRow] = i;
			menuActionCmd3[menuActionRow] = k;
			menuActionRow++;
		} else if (spellSelected == 1) {
			if ((spellUsableOn & 8) == 8) {
				menuActionName[menuActionRow] = spellTooltip + " @whi@" + s;
				menuActionID[menuActionRow] = 365;
				menuActionCmd1[menuActionRow] = j;
				menuActionCmd2[menuActionRow] = i;
				menuActionCmd3[menuActionRow] = k;
				menuActionRow++;
			}
		} else {
			for (int l = 5; l >= 0; l--)
				if (atPlayerActions[l] != null) {
					menuActionName[menuActionRow] = atPlayerActions[l] + " @whi@" + s;
					char c = '\0';
					if (atPlayerActions[l].equalsIgnoreCase("attack")) {
						if (Configuration.playerAttackOptionPriority == 3)
							continue;
						if (Configuration.playerAttackOptionPriority == 0 && player.combatLevel > myPlayer.combatLevel
								|| Configuration.playerAttackOptionPriority == 1)
							c = '\u07D0';
						if (myPlayer.team != 0 && player.team != 0)
							if (myPlayer.team == player.team)
								c = '\u07D0';
							else
								c = '\0';
					} else if (atPlayerArray[l])
						c = '\u07D0';
					if (l == 0)
						menuActionID[menuActionRow] = 561 + c;
					if (l == 1)
						menuActionID[menuActionRow] = 779 + c;
					if (l == 2)
						menuActionID[menuActionRow] = 27 + c;
					if (l == 3)
						menuActionID[menuActionRow] = 577 + c;
					if (l == 4)
						menuActionID[menuActionRow] = 729 + c;
					if (l == 5) {
						if (myPlayer.hasRightsBetween(0, 4)) {
							menuActionID[menuActionRow] = 745 + c;
						} else {
							continue;
						}
					}
					menuActionCmd1[menuActionRow] = j;
					menuActionCmd2[menuActionRow] = i;
					menuActionCmd3[menuActionRow] = k;
					menuActionRow++;
				}

		}
		for (int i1 = 0; i1 < menuActionRow; i1++)
			if (menuActionID[i1] == 516) {
				menuActionName[i1] = "Walk here @whi@" + s;
				return;
			}

	}

	public void method89(SpawnedObject spawnedObject) {
		long i = 0;
		int j = -1;
		int k = 0;
		int l = 0;
		if (spawnedObject.group == 0)
			i = scene.method300(spawnedObject.plane, spawnedObject.x, spawnedObject.y);
		if (spawnedObject.group == 1)
			i = scene.method301(spawnedObject.plane, spawnedObject.x, spawnedObject.y);
		if (spawnedObject.group == 2)
			i = scene.method302(spawnedObject.plane, spawnedObject.x, spawnedObject.y);
		if (spawnedObject.group == 3)
			i = scene.method303(spawnedObject.plane, spawnedObject.x, spawnedObject.y);
		if (i != 0) {
			int i1 = scene.method304(spawnedObject.plane, spawnedObject.x, spawnedObject.y,
					i);
			j = ObjectKey.getObjectId(i);
			k = i1 & 0x1f;
			l = i1 >> 6;
		}
		spawnedObject.getPreviousId = j;
		spawnedObject.previousType = k;
		spawnedObject.previousOrientation = l;
	}

	public void objectFill(final int objectId, final int x1, final int y1, final int x2, final int y2, final int type,
						   final int face, final int height) {
		// if(height == height) // dunno your height variable but refactor
		// yourself
		for (int x = x1; x <= x2; x++) {
			for (int y = y1; y <= y2; y++) {
				addObject(objectId, x, y, face, type, height);
			}
		}
	}

	public void addObject(int objectId, int x, int y, int face, int type, int height) {
		int mX = currentRegionX - 6;
		int mY = currentRegionY - 6;
		int x2 = x - (mX * 8);
		int y2 = y - (mY * 8);
		int i15 = 40 >> 2;
		int l17 = objectTypes[i15];
		if (y2 > 0 && y2 < 103 && x2 > 0 && x2 < 103) {
			method130(-1, objectId, face, l17, y2, type, height, x2, 0);
		}
	}


	public void load_objects() {
		/**
		 * @link addObject objectId, x, y, face, type, height
		 */

		addObject(0, 1544, 3687, 0, 10, 0);
		// Carts
		addObject(7029, 1656, 3542, 0, 10, 0); // Hos
		addObject(7029, 1591, 3621, 0, 10, 0); // Shay
		addObject(7029, 1760, 3710, 0, 10, 0); // Pisc
		addObject(7029, 1254, 3548, 0, 10, 0); // Qu
		addObject(7029, 1670, 3832, 0, 10, 0); // Arc
		addObject(7029, 1518, 3732, 0, 10, 0); // love
		// Abyss
		addObject(24101, 3039, 4834, 0, 10, 0);
		addObject(13642, 3039, 4831, 2, 10, 0);

		// Home objects

		addObject(2030, 1716, 3469, 1, 10, 0);
		addObject(2030, 1672, 3745, 3, 10, 0);

		addObject(28900, 1547, 3921, 1, 10, 0);
		addObject(172, 1619, 3659, 3, 10, 0); // ckey
		addObject(6552, 1647, 3676, 3, 10, 0); // anchients
		addObject(10321, 1663, 3635, 3, 10, 0); // molehole
		addObject(3828, 1845, 3810, 3, 10, 0); // kqtunnel
		addObject(882, 1422, 3586, 3, 10, 0); // gw
		// addObject(4150, 3089, 3495, 3, 10, 0); //warriors guild
		addObject(1738, 1661, 3529, 1, 10, 0); // Tavelery
		// addObject(4151, 3089, 3494, 1, 10, 0); //barrows
		addObject(20877, 1572, 3657, 1, 10, 0); // brimhaven
		addObject(26709, 1280, 3551, 1, 10, 0); // stronghold slayer
		addObject(2123, 1257, 3501, 3, 10, 0); // rellekka slayer
		// addObject(11803, 1650, 3619, 0, 10, 0); //icedung

		addObject(29734, 1349, 3591, 0, 10, 0); // demonicentrance
		addObject(2823, 1792, 3708, 0, 10, 0); // mith entrance
		addObject(4152, 1547, 3570, 1, 10, 0); // corpent
		addObject(4153, 1547, 3567, 1, 10, 0); // corpext
		addObject(4154, 1476, 3688, 1, 10, 0); // lizards
		addObject(4154, 1454, 3693, 1, 10, 0); // lizards
		// addObject(4155, 3089, 3496, 1, 10, 0); //zulrah
		addObject(8356, 1626, 3680, 3, 10, 0); // spirittreekourend
		addObject(8356, 1268, 3561, 0, 10, 0); // spirittreeMountQ
		addObject(8356, 1315, 3619, 0, 10, 0); // spirittreeXeric
		addObject(8356, 1477, 3555, 0, 10, 0); // spirittreeHeros
		//addObject(11835, 1213, 3539, 1, 10, 0); // tzhaar
		addObject(678, 1605, 3707, 3, 10, 0); // CorpPortal
		addObject(2544, 1672, 3557, 1, 10, 0); // Dagentrence

		// wildysigns
		addObject(14503, 1465, 3704, 2, 10, 0); // wildysign
		addObject(14503, 1470, 3704, 2, 10, 0); // wildysign
		addObject(14503, 1469, 3704, 2, 10, 0); // wildysign
		addObject(14503, 1468, 3704, 2, 10, 0); // wildysign
		// 2=s,1=e
		// RFD
		// Resource area
		addObject(9030, 3190, 3929, 1, 10, 0);
		addObject(9030, 3191, 3930, 1, 10, 0);
		addObject(9030, 3190, 3931, 1, 10, 0);


		// Barrows
		addObject(3610, 3550, 9695, 0, 10, 0);

		// Moonclan
		objectFill(0, 2097, 3854, 2097, 3954, 10, 0, 0);

		// addObject(11731, 3003, 3384, 0, 10, 0); //Gem Fally

		// Crafting guild
		// objectFill(-1, 2939, 3282, 2943, 3290, 10, 0, 0); // Crafting guild
		addObject(-1, 2938, 3285, 0, 10, 0);
		addObject(-1, 2937, 3284, 0, 10, 0);
		addObject(-1, 2942, 3291, 0, 10, 0);


		/**
		 * Shilo
		 */
		// obelsisk reg
		addObject(2153, 1506, 3869, 0, 10, 0); // fire
		addObject(2152, 1600, 3632, 0, 10, 0); // air
		addObject(2151, 1793, 3704, 0, 10, 0); // water
		addObject(2150, 1240, 3538, 0, 10, 0); // earth

		// Bank room
		addObject(1113, 2850, 2952, 0, 10, 0); // Chair
		objectFill(24101, 2851, 2952, 2853, 2952, 10, 0, 0); // Bank

		// Vet
		//addObject(0, 3189, 3784, 0, 10, 0);
		//addObject(0, 3189, 3782, 0, 10, 0);
		//addObject(0, 3188, 3783, 0, 10, 0);
		//addObject(0, 3191, 3784, 0, 10, 0);
		//addObject(-1, 3189, 3783, 0, 10, 0);
		/**
		 * @link objectFill objectId, SWX, SWY, NEX, NEY, type, face, height
		 */
		objectFill(-1, 3010, 3371, 3014, 3375, 10, 0, 0);
		objectFill(-1, 1748, 5322, 1755, 5327, 22, 0, 1);
		objectFill(14896, 3010, 3372, 3014, 3374, 10, 0, 0);
		objectFill(14896, 3011, 3371, 3013, 3371, 10, 0, 0);
		objectFill(14896, 3011, 3375, 3013, 3375, 10, 0, 0);

	}

	public static String fontFilter() {
		if (Configuration.newFonts) {
			return "_2";
		}
		return "";
	}

	@Override
	void startUp() {
		expTrackerPanel = new ExpTrackerPanel();
		SettingsManager.loadSettings();
		ClientScripts.load();
		drawLoadingText(10, "Loading title screen - 0%");
		if (Signlink.sunjava) {
			super.minDelay = 5;
		}
		getDocumentBaseHost();
		variousSettings[304] = 1;
		if (Signlink.cache_dat != null) {
			for (int i = 0; i < 8; i++)
				decompressors[i] = new Decompressor(Signlink.cache_dat, Signlink.cache_idx[i], i + 1);
		}

		new CacheDownloader(this).downloadCache();

		SpriteLoader1.loadSprites();
		cacheSprite1 = SpriteLoader1.sprites;
		SpriteLoader1.sprites = null;

		SpriteLoader2.loadSprites();
		cacheSprite2 = SpriteLoader2.sprites;
		SpriteLoader2.sprites = null;

		SpriteLoader3.loadSprites();
		cacheSprite3 = SpriteLoader3.sprites;
		SpriteLoader3.sprites = null;

		SpriteLoader4.loadSprites();
		cacheSprite4 = SpriteLoader4.sprites;
		SpriteLoader4.sprites = null;
		SpriteLoader.load474Sprites();
		cacheSprite474 = SpriteLoader.sprites474;
		OsrsSpriteLoader.loadSprites();
		OsrsSprite = OsrsSpriteLoader.sprites;
		OsrsSpriteLoader.sprites = null;
		try {
			ItemDef.load();
			titleStreamLoader = streamLoaderForName(1, "title screen");
			smallText = new TextDrawingArea(false, "p11_full" + fontFilter(), titleStreamLoader);
			XPFONT = new TextDrawingArea(true, "q8_full" + fontFilter(), titleStreamLoader);
			aTextDrawingArea_1271 = new TextDrawingArea(false, "p12_full" + fontFilter(), titleStreamLoader);
			chatTextDrawingArea = new TextDrawingArea(false, "b12_full", titleStreamLoader);
			aTextDrawingArea_1273 = new TextDrawingArea(true, "q8_full" + fontFilter(), titleStreamLoader);
			newSmallFont = new RSFont(false, "p11_full" + fontFilter(), titleStreamLoader);
			newRegularFont = new RSFont(false, "p12_full" + fontFilter(), titleStreamLoader);
			newBoldFont = new RSFont(false, "b12_full" + fontFilter(), titleStreamLoader);
			newFancyFont = new RSFont(true, "q8_full" + fontFilter(), titleStreamLoader);

			/**
			 * New fonts
			 */
			lato = new RSFont(true, "lato_full", titleStreamLoader);
			latoBold = new RSFont(true, "lato_bold_full", titleStreamLoader);
			kingthingsPetrock = new RSFont(true, "kingthings_petrock_full", titleStreamLoader);
			kingthingsPetrockLight = new RSFont(true, "kingthings_petrock_light_full", titleStreamLoader);

			loadTitleScreen();
			createScreenImages();
			StreamLoader streamLoader = streamLoaderForName(2, "config");
			StreamLoader streamLoader_1 = streamLoaderForName(3, "interface");
			StreamLoader streamLoader_2 = streamLoaderForName(4, "2d graphics");
			StreamLoader streamLoader_3 = streamLoaderForName(6, "textures");
			StreamLoader streamLoader_4 = streamLoaderForName(7, "chat system");
			@SuppressWarnings("unused")
			StreamLoader streamLoader_5 = streamLoaderForName(8, "sound effects");
			byteGroundArray = new byte[4][104][104];
			intGroundArray = new int[4][105][105];
			scene = new WorldController(intGroundArray, parentFrame);
			for (int j = 0; j < 4; j++)
				collisionMaps[j] = new CollisionMap();

			minimapImage = new Sprite(512, 512);
			StreamLoader streamLoader_6 = streamLoaderForName(5, "update list");
			drawLoadingText(60, "Connecting to update server");
			onDemandFetcher = new OnDemandFetcher();
			onDemandFetcher.start(streamLoader_6, this);

			if (Configuration.packIndexData) {
				repackCacheAll();
			}

			if (Configuration.dumpMaps) {
				onDemandFetcher.dumpMaps();
			}

			AnimFrameSet.init();
			AnimKeyFrameSet.init();


			Model.init();
			System.out.println("After Model.init() - modelHeaders: " + 
				    (Model.modelHeaders != null ? Model.modelHeaders.length : "NULL"));
			
			// Load music settings
			try {
			    musicSettings.read();
			} catch (Exception e) {
			    e.printStackTrace();
			}

			// Check if music should be muted
			if (musicSettings.isLoginMusicMuted()) {
			    loginMusicMuted = true;
			    this.nextSong = -1; // Don't play music
			} else {
			    this.nextSong = 54; // Play login music
			    this.songChanging = true;
			    onDemandFetcher.provide(2, this.nextSong);
			}
			
			drawLoadingText(80, "Unpacking media");
			mapIcon7 = new Sprite(streamLoader_2, "mapfunction", 1);
			mapIcon8 = new Sprite(streamLoader_2, "mapfunction", 51);
			mapIcon6 = new Sprite(streamLoader_2, "mapfunction", 74);
			mapIcon5 = new Sprite(streamLoader_2, "mapfunction", 5);
			mapIcon9 = new Sprite(streamLoader_2, "mapfunction", 56);
			multiOverlay = new Sprite(streamLoader_2, "overlay_multiway", 0);

			eventIcon = new Sprite(streamLoader_2, "mapfunction", 72);
			bankDivider = new Sprite("bank_divider");

			// Login
			loginAsset0 = new Sprite("Login/remember0");
			loginAsset1 = new Sprite("Login/remember1");
			loginAsset2 = new Sprite("Login/remember2");
			loginAsset3 = new Sprite("Login/remember3");
			loginAsset4 = new Sprite("Login/logo");
			loginScreenBackground = new Sprite("loginscreen/background2"); // Keep this as fallback
			logo2021 = new Sprite("/loginscreen/logo");

			// Load the login box overlay SEPARATELY
			loginBoxOverlay = new Sprite("loginscreen/background2"); // Your login box PNG

			// Initialize video
			try {
			    loginVideo = new VideoBackground(Signlink.getCacheDirectory() + "sprites/Login/background.mp4", 765, 503);
			} catch (Exception e) {
			    e.printStackTrace();
			    loginVideo = null;
			}
			
			muteButtonOff = new Sprite("Login/mute_off"); // When music is playing
			muteButtonOn = new Sprite("Login/mute_on");   // When music is muted
			File[] file = new File(Signlink.getCacheDirectory() + "/sprites/sprites/").listFiles();
			int size = file.length;
			cacheSprite = new Sprite[size];
			for (int i = 0; i < size; i++) {
				cacheSprite[i] = new Sprite("Sprites/" + i);
			}

			xpSprite = new Sprite("medal");
			for (int i = 0; i < inputSprites.length; i++)
				inputSprites[i] = new Sprite("Interfaces/Inputfield/SPRITE " + (i + 1));

			for (int i = 0; i < tabAreaResizable.length; i++)
				tabAreaResizable[i] = new Sprite("Gameframe/resizable/tabArea " + i);

			loadTabArea();

			infinity = new Sprite("infinity");
			chatArea = new Sprite("Gameframe/chatarea");
			channelButtons = new Sprite("Gameframe/channelbuttons");
			venomOrb = new Sprite("orbs/venom");

			for (int index = 0; index < smallXpSprites.length; index++) {
				smallXpSprites[index] = new Sprite("expdrop/" + index);

			}

			for (int c1 = 0; c1 <= 3; c1++)
				chatButtons[c1] = new Sprite(streamLoader_2, "chatbuttons", c1);
			chatButtons[3] = new Sprite("1025_0");
			Sprite[] clanIcons = new Sprite[9];
			for (int index = 0; index < clanIcons.length; index++) {
				clanIcons[index] = new Sprite("Clan Chat/Icons/" + index);
			}

			String iconPackDir = Signlink.getCacheDirectory() + "sprites" + Signlink.separator + "icon_pack";
			Sprite[] iconPack = new Sprite[FileUtility.getFileCount(iconPackDir)];
			for (int index = 0; index < iconPack.length; index++) {
				iconPack[index] = new Sprite("icon_pack/" + index);
			}

			RSFont.unpackImages(modIcons, clanIcons, iconPack);

			mapEdge = new Sprite(streamLoader_2, "mapedge", 0);
			mapEdge.method345();

			try {
				for (int k3 = 0; k3 < 199; k3++)
					mapScenes[k3] = new Sprite("mapScenes/" + k3);
					//mapScenes[k3] = new Background(streamLoader_2, "mapscene", k3);
			} catch (Exception _ex) {
			}
			try {
				for (int l3 = 0; l3 < 125; l3++)
					mapFunctions[l3] = new Sprite("MapFunctions/" + l3);
			} catch (Exception _ex) {
			}
			try {
				for (int l3 = 0; l3 < 1; l3++)
					wiki[l3] = new Sprite("Wiki/" + l3);
			} catch (Exception _ex) {
			}
			try {
				for (int i4 = 0; i4 < 20; i4++) {
					hitMarks[i4] = new Sprite(streamLoader_2, "hitmarks", i4);
				}
			} catch (Exception _ex) {
			}

			hitMarks[19] = new Sprite("heal_hitsplat");

			try {
				for (int h1 = 0; h1 < 6; h1++)
					headIconsHint[h1] = new Sprite(streamLoader_2, "headicons_hint", h1);
			} catch (Exception _ex) {
			}
			try {
				for (int j4 = 0; j4 < 8; j4++)
					headIcons[j4] = new Sprite(streamLoader_2, "headicons_prayer", j4);
				for (int j45 = 0; j45 < 3; j45++)
					skullIcons[j45] = new Sprite(streamLoader_2, "headicons_pk", j45);
			} catch (Exception _ex) {
			}
			for (int i = 0; i < minimapIcons.length; i++) {
				minimapIcons[i] = new Sprite("Mapicons/ICON " + i);
			}
			//loginBackground2 = 	new AnimatedSprite(new URL("https://cdn.discordapp.com/attachments/454138282780131329/454147023046836235/Ascend-CB.gif"));
			mapFlag = new Sprite(streamLoader_2, "mapmarker", 0);
			mapMarker = new Sprite(streamLoader_2, "mapmarker", 1);
			for (int k4 = 0; k4 < 8; k4++)
				crosses[k4] = new Sprite(streamLoader_2, "cross", k4);

			mapDotItem = new Sprite(streamLoader_2, "mapdots", 0);
			mapDotNPC = new Sprite(streamLoader_2, "mapdots", 1);
			mapDotPlayer = new Sprite(streamLoader_2, "mapdots", 2);
			mapDotFriend = new Sprite(streamLoader_2, "mapdots", 3);
			mapDotTeam = new Sprite(streamLoader_2, "mapdots", 4);
			mapDotClan = new Sprite(streamLoader_2, "mapdots", 5);
			new Sprite(streamLoader_2, "mapdots", 4);
			scrollBar1 = new Sprite(streamLoader_2, "scrollbar", 0);
			scrollBar2 = new Sprite(streamLoader_2, "scrollbar", 1);
			for (int i = 0; i < modIcons.length; i++) {
				modIcons[i] = new Sprite("Player/MODICONS " + i + "");
			}

			for (int index = 0; index < GameTimerHandler.TIMER_IMAGES.length; index++) {
				GameTimerHandler.TIMER_IMAGES[index] = new Sprite("GameTimer/TIMER " + index);
			}

			int i5 = (int) (Math.random() * 21D) - 10;
			int j5 = (int) (Math.random() * 21D) - 10;
			int k5 = (int) (Math.random() * 21D) - 10;
			int l5 = (int) (Math.random() * 41D) - 20;
			for (int i6 = 0; i6 < 114; i6++) {
				if (mapFunctions[i6] != null)
					mapFunctions[i6].method344(i5 + l5, j5 + l5, k5 + l5);
			}
			for (int i6 = 0; i6 < 199; i6++) {
				if (mapScenes[i6] != null)
					mapScenes[i6].method344(i5 + l5, j5 + l5, k5 + l5);
			}

			drawLoadingText(83, "Unpacking textures");
			Rasterizer.method368(streamLoader_3);
			Rasterizer.method367();
			drawLoadingText(83, "Unpacking config");
			AnimationDefinition.unpackConfig(streamLoader);
			System.out.println("AnimationDefinition loaded");
			ObjectDefinition.unpackConfig(streamLoader);
			System.out.println("ObjectDefinition loaded");
			FloorUnderlayDefinition.unpackConfig(streamLoader);
			System.out.println("FloorUnderlayDefinition loaded");
			ItemDefinition.unpackConfig(streamLoader);
			System.out.println("ItemDefinition loaded");
			NpcDefinition.unpackConfig(streamLoader);
			System.out.println("NpcDefinition loaded");
			IDK.unpackConfig(streamLoader);
			System.out.println("Idk Loaded");
			GraphicsDefinition.unpackConfig(streamLoader);
			System.out.println("GraphicsDefinition loaded");
			AreaDefinition.unpackConfig(streamLoader);
			System.out.println("AreaDefinition loaded");
			Varp.unpackConfig(streamLoader);
			System.out.println("Varp Loaded");
			VarBit.unpackConfig(streamLoader);
			System.out.println("Varbit Loaded");
				this.drawLoadingText(92,  "Unpacking sounds");
				byte[] var36 = streamLoader_5.getArchiveData("sounds.dat");
				Buffer soundbuffer = new Buffer(var36);
				Sounds.unpack(soundbuffer);
			loadPlayerData();

			if (Configuration.dumpDataLists) {
				ItemDefinition.dumpList();
				NpcDefinition.dumpList();
				ItemDefinition.dumpList();
				ObjectDefinition.dumpList();
				onDemandFetcher.dumpModels();
			}

			drawLoadingText(95, "Unpacking interfaces");
			TextDrawingArea allFonts[] = { smallText, aTextDrawingArea_1271, chatTextDrawingArea,
					aTextDrawingArea_1273 };
			RSInterface.unpack(streamLoader_1, allFonts, streamLoader_2, new RSFont[] {newSmallFont, newRegularFont, newBoldFont, newFancyFont});
			drawLoadingText(100, "Preparing game engine");

			if(getUserSettings().isOldGameframe() == false) {
				mapBack = new Sprite("Gameframe/fixed/mapBack");
			}else {
				mapBack = new Sprite("Gameframe317/fixed/mapBack");
			}
			for (int pixelY = 0; pixelY < 33; pixelY++) {
				int k6 = 999;
				int i7 = 0;
				for (int pixelX = 0; pixelX < 34; pixelX++) {
					if (mapBack.myPixels[pixelX + pixelY * mapBack.myWidth] == 0) {
						if (k6 == 999)
							k6 = pixelX;
						continue;
					}
					if (k6 == 999)
						continue;
					i7 = pixelX;
					break;
				}
				anIntArray968[pixelY] = k6;
				anIntArray1057[pixelY] = i7 - k6;
			}
			for (int pixelY = 1; pixelY < 153; pixelY++) {
				int j7 = 999;
				int l7 = 0;
				for (int pixelX = 24; pixelX < 177; pixelX++) {
					if (mapBack.myPixels[pixelX + pixelY * mapBack.myWidth] == 0 && (pixelX > 34 || pixelY > 34)) {
						if (j7 == 999) {
							j7 = pixelX;
						}
						continue;
					}
					if (j7 == 999) {
						continue;
					}
					l7 = pixelX;
					break;
				}
				anIntArray1052[pixelY - 1] = j7 - 24;
				anIntArray1229[pixelY - 1] = l7 - j7;
			}

			try {
				macAddress = GetNetworkAddress.GetAddress("mac");
				if (macAddress == null)
					macAddress = "";
				if (Configuration.developerMode) {
					System.out.println(macAddress);
				}
			} catch (Exception e) {
				e.printStackTrace();
				macAddress = "";
			}

			//Censor.loadConfig(streamLoader_4);
			setConfigButtonsAtStartup();
			//mouseDetection = new MouseDetection(this);

			try {
				informationFile.read();
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (informationFile.isUsernameRemembered()) {
				myUsername = informationFile.getStoredUsername();
			}
			if (informationFile.isPasswordRemembered()) {
				setPassword(informationFile.getStoredPassword());
			}
			if (informationFile.isRememberRoof()) {
				removeRoofs = true;
			}


			Animable_Sub5.clientInstance = this;
			ObjectDefinition.clientInstance = this;
			NpcDefinition.clientInstance = this;
			AnimFrame.clientInstance = this;

			if (Configuration.PRINT_EMPTY_INTERFACE_SECTIONS) {
				if (Configuration.developerMode) {
					RSInterface.printEmptyInterfaceSections();
				} else {
					System.err.println("PRINT_EMPTY_INTERFACE_SECTIONS is toggled but you must be in dev mode.");
				}
			}

			Preferences.load();

			logger.debug("Loaded {} sprites.", Sprite.loadedSprites
					+ Arrays.stream(cacheSprite1).filter(Objects::nonNull).count()
					+ Arrays.stream(cacheSprite2).filter(Objects::nonNull).count()
					+ Arrays.stream(cacheSprite3).filter(Objects::nonNull).count()
					+ Arrays.stream(cacheSprite4).filter(Objects::nonNull).count()
					+ Arrays.stream(cacheSprite).filter(Objects::nonNull).count()
			);
			return;
		} catch (Exception exception) {
			exception.printStackTrace();
			Signlink.reporterror("loaderror " + aString1049 + " " + anInt1079);
		}
		loadingError = true;
	}
	public void setGameFrame(CustomGameFrame frame) {
	    this.parentFrame = frame;
	}

	public CustomGameFrame getGameFrame() {
	    return parentFrame;
	}
	public void method91(Buffer stream, int i) {
		while (stream.bitPosition + 10 < i * 8) {
			int j = stream.readBits(11);
			if (j == 2047)
				break;
			if (players[j] == null) {
				players[j] = new Player();
				if (aStreamArray895s[j] != null)
					players[j].updatePlayer(aStreamArray895s[j]);
			}
			playerIndices[playerCount++] = j;
			Player player = players[j];
			player.anInt1537 = update_tick;
			int k = stream.readBits(1);
			if (k == 1)
				anIntArray894[anInt893++] = j;
			int l = stream.readBits(1);
			int i1 = stream.readBits(5);
			if (i1 > 15)
				i1 -= 32;
			int j1 = stream.readBits(5);
			if (j1 > 15)
				j1 -= 32;
			player.setPos(myPlayer.waypoint_x[0] + j1, myPlayer.waypoint_y[0] + i1, l == 1);
		}
		stream.finishBitAccess();
	}

	public boolean inCircle(int circleX, int circleY, int clickX, int clickY, int radius) {
		return java.lang.Math.pow((circleX + radius - clickX), 2)
				+ java.lang.Math.pow((circleY + radius - clickY), 2) < java.lang.Math.pow(radius, 2);
	}

	public boolean mouseMapPosition() {
		if (super.getMouseX() >= currentGameWidth - 21 && super.getMouseX() <= currentGameWidth && super.getMouseY() >= 0
				&& super.getMouseY() <= 21) {
			return false;
		}
		return true;
	}

	private void processMainScreenClick() {
		if (minimapState != 0)
			return;
		if (super.clickMode3 == 1) {
			int i = super.getSaveClickX() - 25 - 547;
			int j = super.getSaveClickY() - 5 - 3;
			if (currentScreenMode != ScreenMode.FIXED) {
				i = super.getSaveClickX() - (currentGameWidth - 182 + 24);
				j = super.getSaveClickY() - 8;
			}
			if (inCircle(0, 0, i, j, 76) && mouseMapPosition() && !runHover) {
				i -= 73;
				j -= 75;
				int k = viewRotation + minimapRotation & 0x7ff;
				int i1 = Rasterizer.Rasterizer3D_sine[k];
				int j1 = Rasterizer.Rasterizer3D_cosine[k];
				i1 = i1 * (minimapZoom + 256) >> 8;
				j1 = j1 * (minimapZoom + 256) >> 8;
				int k1 = j * i1 + i * j1 >> 11;
				int l1 = j * j1 - i * i1 >> 11;
				int i2 = myPlayer.x + k1 >> 7;
				int j2 = myPlayer.y - l1 >> 7;
				if (myPlayer.isAdminRights() && controlIsDown) {
					teleport(baseX + i2, baseY + j2);
				} else {
					boolean flag1 = doWalkTo(1, myPlayer.waypoint_x[0], myPlayer.waypoint_y[0], 0, 0, 0, 0, 0, j2, true, i2);
					if (flag1) {
						stream.writeUnsignedByte(i);
						stream.writeUnsignedByte(j);
						stream.writeWord(viewRotation);
						stream.writeUnsignedByte(57);
						stream.writeUnsignedByte(minimapRotation);
						stream.writeUnsignedByte(minimapZoom);
						stream.writeUnsignedByte(89);
						stream.writeWord(myPlayer.x);
						stream.writeWord(myPlayer.y);
						stream.writeUnsignedByte(anInt1264);
						stream.writeUnsignedByte(63);
					}
				}
			}
			anInt1117++;
			if (anInt1117 > 1151) {
				anInt1117 = 0;
				stream.createFrame(246);
				stream.writeUnsignedByte(0);
				int l = stream.currentOffset;
				if ((int) (Math.random() * 2D) == 0)
					stream.writeUnsignedByte(101);
				stream.writeUnsignedByte(197);
				stream.writeWord((int) (Math.random() * 65536D));
				stream.writeUnsignedByte((int) (Math.random() * 256D));
				stream.writeUnsignedByte(67);
				stream.writeWord(14214);
				if ((int) (Math.random() * 2D) == 0)
					stream.writeWord(29487);
				stream.writeWord((int) (Math.random() * 65536D));
				if ((int) (Math.random() * 2D) == 0)
					stream.writeUnsignedByte(220);
				stream.writeUnsignedByte(180);
				stream.writeBytes(stream.currentOffset - l);
			}
		}
	}
	/**
	 * Get the String residing on the clipboard.
	 *
	 * @return any text found on the Clipboard; if none found, return an empty
	 *         String.
	 */
	public static String getClipboardContents() {
		if (System.currentTimeMillis() - timer < 2000) {
			return "";
		}
		String result = "";
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (hasTransferableText) {
			try {
				result = (String) contents.getTransferData(DataFlavor.stringFlavor);
				timer = System.currentTimeMillis();
			} catch (UnsupportedFlavorException | IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}

	static long timer = 0;

	private void teleport(int x, int y) {
		String text = "::tele " + x + " " + y;
		stream.createFrame(103);
		stream.writeUnsignedByte(text.length() - 1);
		stream.writeString(text.substring(2));
	}

	public static void teleportInterface() {
		if (System.currentTimeMillis() - timer < 400) {
			return;
		}
		timer = System.currentTimeMillis();
		String text = "::teleport";
		stream.createFrame(103);
		stream.writeUnsignedByte(text.length() - 1);
		stream.writeString(text.substring(2));
	}

	public static void closeInterface() {
		String text = "::close";
		stream.createFrame(103);
		stream.writeUnsignedByte(text.length() - 1);
		stream.writeString(text.substring(2));
	}

	public static void continueDialogue() {
		if (System.currentTimeMillis() - timer < 400) {
			return;
		}
		timer = System.currentTimeMillis();
		String text = "::dialoguecontinuation continue";
		stream.createFrame(103);
		stream.writeUnsignedByte(text.length() - 1);
		stream.writeString(text.substring(2));
	}

	public static void dialogueOptions(String option) {
		if (System.currentTimeMillis() - timer < 400) {
			return;
		}
		timer = System.currentTimeMillis();
		String text = option == "one" ? "::dialoguecontinuation option_one"
				: option == "two" ? "::dialoguecontinuation option_two"
				: option == "three" ? "::dialoguecontinuation option_three"
				: option == "four" ? "::dialoguecontinuation option_four"
				: "::dialoguecontinuation option_five";
		stream.createFrame(103);
		stream.writeUnsignedByte(text.length() - 1);
		stream.writeString(text.substring(2));
	}

	private String interfaceIntToString(int j) {
		if (j < 0x3b9ac9ff)
			return String.valueOf(j);
		else
			return "*";
	}

	public void showErrorScreen() {
		Graphics g = getGameComponent().getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, 765, 503);
		method4(1);
		if (loadingError) {
			aBoolean831 = false;
			g.setFont(new Font("Helvetica", 1, 16));
			g.setColor(Color.yellow);
			int k = 35;
			g.drawString("Sorry, an error has occured whilst loading " + Configuration.CLIENT_TITLE, 30, k);
			k += 50;
			g.setColor(Color.white);
			g.drawString("To fix this try the following (in order):", 30, k);
			k += 50;
			g.setColor(Color.white);
			g.setFont(new Font("Helvetica", 1, 12));
			g.drawString("1: Try restarting your client.", 30, k);
			k += 30;
			g.drawString("2: Try deleting the cache folder: " + Signlink.getCacheDirectory(), 30, k);
			k += 30;
			g.drawString("3: Try rebooting your computer", 30, k);
			k += 30;
			g.drawString("4: Try installing a different version of Java", 30, k);
			k += 30;
			g.drawString("5: Ask on discord", 30, k);
		}
		if (genericLoadingError) {
			aBoolean831 = false;
			g.setFont(new Font("Helvetica", 1, 20));
			g.setColor(Color.white);
			g.drawString("Error - unable to load game!", 50, 50);
			g.drawString("To play RuneScape make sure you play from", 50, 100);
			g.drawString("http://www.RuneScape.com", 50, 150);
		}
		if (rsAlreadyLoaded) {
			aBoolean831 = false;
			g.setColor(Color.yellow);
			int l = 35;
			g.drawString("Error a copy of RuneScape already appears to be loaded", 30, l);
			l += 50;
			g.setColor(Color.white);
			g.drawString("To fix this try the following (in order):", 30, l);
			l += 50;
			g.setColor(Color.white);
			g.setFont(new Font("Helvetica", 1, 12));
			g.drawString("1: Try closing ALL open web-browser windows, and reloading", 30, l);
			l += 30;
			g.drawString("2: Try rebooting your computer, and reloading", 30, l);
			l += 30;
		}
	}

	@Override
	public URL getCodeBase() {
		try {
			return new URL(server + ":" + (80 + portOff));
		} catch (Exception _ex) {
		}
		return null;
	}

	public void method95() {
		for (int j = 0; j < npcCount; j++) {
			int k = npcIndices[j];
			NPC npc = npcs[k];
			if (npc != null)
				method96(npc);
		}
	}

	public void method96(Entity entity) {
		if (entity.x < 128 || entity.y < 128 || entity.x >= 13184 || entity.y >= 13184) {
			entity.primaryanim = -1;
			entity.spotanim = -1;
			entity.exactmove_start = 0;
			entity.exactmove_end = 0;
			entity.x = entity.waypoint_x[0] * 128 + entity.size * 64;
			entity.y = entity.waypoint_y[0] * 128 + entity.size * 64;
			entity.method446();
		}
		if (entity == myPlayer && (entity.x < 1536 || entity.y < 1536 || entity.x >= 11776 || entity.y >= 11776)) {
			entity.primaryanim = -1;
			entity.spotanim = -1;
			entity.exactmove_start = 0;
			entity.exactmove_end = 0;
			entity.x = entity.waypoint_x[0] * 128 + entity.size * 64;
			entity.y = entity.waypoint_y[0] * 128 + entity.size * 64;
			entity.method446();
		}
		if (entity.exactmove_start > update_tick)
			method97(entity);
		else if (entity.exactmove_end >= update_tick)
			method98(entity);
		else
			method99(entity);
		method100(entity);
		method101(entity);
	}

	public void method97(Entity entity) {
		int i = entity.exactmove_start - update_tick;
		int j = entity.exactmove_x1 * 128 + entity.size * 64;
		int k = entity.exactmove_y1 * 128 + entity.size * 64;
		entity.x += (j - entity.x) / i;
		entity.y += (k - entity.y) / i;
		entity.walkanim_pause = 0;
		if (entity.forceMovementDirection == 0)
			entity.setTurnDirection(1024);
		if (entity.forceMovementDirection == 1)
			entity.setTurnDirection(1536);
		if (entity.forceMovementDirection == 2)
			entity.setTurnDirection(0);
		if (entity.forceMovementDirection == 3)
			entity.setTurnDirection(512);
	}

	public void method98(Entity entity) {
		boolean var2 = entity.exactmove_end == update_tick || entity.primaryanim == -1 || entity.primaryanim_pause != 0;
		if (!var2) {
			AnimationDefinition var3 = AnimationDefinition.anims[entity.primaryanim];
			if (null != var3 && !var3.using_keyframes()) {
				var2 = entity.primaryanim_loops_remaining + 1 > var3.anIntArray354[entity.primaryanim_frameindex];
			} else {
				var2 = true;
			}
		}

		if (var2) {
			int i = entity.exactmove_end - entity.exactmove_start;
			int j = update_tick - entity.exactmove_start;
			int k = entity.exactmove_x1 * 128 + entity.size * 64;
			int l = entity.exactmove_y1 * 128 + entity.size * 64;
			int i1 = entity.anInt1544 * 128 + entity.size * 64;
			int j1 = entity.anInt1546 * 128 + entity.size * 64;
			entity.x = (k * (i - j) + i1 * j) / i;
			entity.y = (l * (i - j) + j1 * j) / i;
		}
		entity.walkanim_pause = 0;
		if (entity.forceMovementDirection == 0)
			entity.setTurnDirection(1024);
		if (entity.forceMovementDirection == 1)
			entity.setTurnDirection(1536);
		if (entity.forceMovementDirection == 2)
			entity.setTurnDirection(0);
		if (entity.forceMovementDirection == 3)
			entity.setTurnDirection(512);
		entity.target_direction = entity.getTurnDirection();
	}

	public void method99(Entity entity) {
		entity.secondaryanim = entity.readyanim;
		if (entity.waypoint_count == 0) {
			entity.walkanim_pause = 0;
			return;
		}
		if (entity.primaryanim != -1 && entity.primaryanim_pause == 0) {
			AnimationDefinition animation = AnimationDefinition.anims[entity.primaryanim];
			if (entity.anim_delay > 0 && animation.exactmove == 0) {
				entity.walkanim_pause++;
				return;
			}
			if (entity.anim_delay <= 0 && animation.movetype == 0) {
				entity.walkanim_pause++;
				return;
			}
		}
		int i = entity.x;
		int j = entity.y;
		int k = entity.waypoint_x[entity.waypoint_count - 1] * 128 + entity.size * 64;
		int l = entity.waypoint_y[entity.waypoint_count - 1] * 128 + entity.size * 64;
		if (k - i > 256 || k - i < -256 || l - j > 256 || l - j < -256) {
			entity.x = k;
			entity.y = l;
			return;
		}
		if (i < k) {
			if (j < l)
				entity.setTurnDirection(1280);
			else if (j > l)
				entity.setTurnDirection(1792);
			else
				entity.setTurnDirection(1536);
		} else if (i > k) {
			if (j < l)
				entity.setTurnDirection(768);
			else if (j > l)
				entity.setTurnDirection(256);
			else
				entity.setTurnDirection(512);
		} else if (j < l)
			entity.setTurnDirection(1024);
		else
			entity.setTurnDirection(0);
		int i1 = entity.getTurnDirection() - entity.target_direction & 0x7ff;
		if (i1 > 1024)
			i1 -= 2048;
		int j1 = entity.walkanim_b;
		if (i1 >= -256 && i1 <= 256)
			j1 = entity.walkanim;
		else if (i1 >= 256 && i1 < 768)
			j1 = entity.walkanim_r;
		else if (i1 >= -768 && i1 <= -256)
			j1 = entity.walkanim_l;
		if (j1 == -1)
			j1 = entity.walkanim;
		entity.secondaryanim = j1;
		int k1 = 4;
		if (entity.target_direction != entity.getTurnDirection() && entity.interactingEntity == -1 && entity.turnspeed != 0)
			k1 = 2;
		if (entity.waypoint_count > 2)
			k1 = 6;
		if (entity.waypoint_count > 3)
			k1 = 8;
		if (entity.walkanim_pause > 0 && entity.waypoint_count > 1) {
			k1 = 8;
			entity.walkanim_pause--;
		}
		if (entity.aBooleanArray1553[entity.waypoint_count - 1] == (byte)2)
			k1 <<= 1;
		if (k1 >= 8 && entity.secondaryanim == entity.walkanim && entity.runanim != -1)
			entity.secondaryanim = entity.runanim;
		if (i < k) {
			entity.x += k1;
			if (entity.x > k)
				entity.x = k;
		} else if (i > k) {
			entity.x -= k1;
			if (entity.x < k)
				entity.x = k;
		}
		if (j < l) {
			entity.y += k1;
			if (entity.y > l)
				entity.y = l;
		} else if (j > l) {
			entity.y -= k1;
			if (entity.y < l)
				entity.y = l;
		}
		if (entity.x == k && entity.y == l) {
			entity.waypoint_count--;
			if (entity.anim_delay > 0)
				entity.anim_delay--;
		}
	}

	public void method100(Entity entity) {
		if (entity.turnspeed == 0)
			return;
		if (entity.interactingEntity != -1 && entity.interactingEntity < 32768) {
			NPC npc = npcs[entity.interactingEntity];
			if (npc != null) {
				int i1 = entity.x - npc.x;
				int k1 = entity.y - npc.y;
				if (i1 != 0 || k1 != 0)
					entity.setTurnDirection((int) (Math.atan2(i1, k1) * 325.94900000000001D) & 0x7ff);
			}
		}
		if (entity.interactingEntity >= 32768) {
			int j = entity.interactingEntity - 32768;
			if (j == unknownInt10)
				j = maxPlayerCount;
			Player player = players[j];
			if (player != null) {
				int l1 = entity.x - player.x;
				int i2 = entity.y - player.y;
				if (l1 != 0 || i2 != 0)
					entity.setTurnDirection((int) (Math.atan2(l1, i2) * 325.94900000000001D) & 0x7ff);
			}
		}
		if ((entity.face_x != 0 || entity.face_y != 0) && (entity.waypoint_count == 0 || entity.walkanim_pause > 0)) {
			int k = entity.x - (entity.face_x - baseX - baseX) * 64;
			int j1 = entity.y - (entity.face_y - baseY - baseY) * 64;
			if (k != 0 || j1 != 0) {
				entity.setTurnDirection((int) (Math.atan2(k, j1) * 325.94900000000001D) & 0x7ff);
			}
			entity.face_x = 0;
			entity.face_y = 0;
		}
		int turn_angle = entity.getTurnDirection() - entity.target_direction & 0x7ff;
		if (turn_angle == 0) {
			entity.direction_change_tick = 0;
		} else {
			entity.direction_change_tick++;
			boolean change_dir;
			if (turn_angle > 1024) {
				entity.target_direction -= entity.instant_facing ? turn_angle : entity.turnspeed;
				change_dir = true;
				if (turn_angle < entity.turnspeed || turn_angle > 2048 - entity.turnspeed) {
					entity.target_direction = entity.getTurnDirection();
					change_dir = false;
				}

				if (!entity.instant_facing && entity.secondaryanim == entity.readyanim && (entity.direction_change_tick > 25 || change_dir)) {
					if (entity.readyanim_l != -1) {
						entity.secondaryanim = entity.readyanim_l;
					} else {
						entity.secondaryanim = entity.walkanim;
					}
				}
			} else {
				entity.target_direction += entity.instant_facing ? turn_angle : entity.turnspeed;
				change_dir = true;
				if (turn_angle < entity.turnspeed || turn_angle > 2048 - entity.turnspeed) {
					entity.target_direction = entity.getTurnDirection();
					change_dir = false;
				}

				if (!entity.instant_facing && entity.secondaryanim == entity.readyanim && (entity.direction_change_tick > 25 || change_dir)) {
					if (entity.readyanim_r != -1) {
						entity.secondaryanim = entity.readyanim_r;
					} else {
						entity.secondaryanim = entity.walkanim;
					}
				}
			}

			entity.target_direction &= 0x7ff;
			entity.instant_facing = false;
		}
	}

	public void method101(Entity entity) {
		entity.is_walking = false;

		if (entity.secondaryanim >= 65535)
			entity.secondaryanim = -1;

		if (entity.secondaryanim != -1) {
			AnimationDefinition secondaryAnimation = AnimationDefinition.anims[entity.secondaryanim];

			if (secondaryAnimation == null) {
				entity.secondaryanim = -1;
			} else if (!secondaryAnimation.using_keyframes() && secondaryAnimation.anIntArray353 != null) {
				++entity.secondaryanim_loops_remaining;

				if (entity.secondaryanim_frameindex < secondaryAnimation.anIntArray353.length &&
						entity.secondaryanim_loops_remaining > secondaryAnimation.anIntArray354[entity.secondaryanim_frameindex]) {

					entity.secondaryanim_loops_remaining = 1;
					++entity.secondaryanim_frameindex;
					play_frames_sound(secondaryAnimation, entity.primaryanim_frameindex, entity);
				}

				if (entity.secondaryanim_frameindex >= secondaryAnimation.anIntArray353.length) {
					if (secondaryAnimation.loop_delay > 0) {
						entity.secondaryanim_frameindex -= secondaryAnimation.loop_delay;

						if (secondaryAnimation.replay) {
							++entity.secondaryanim_replaycount;
						}

						if (entity.secondaryanim_frameindex < 0 ||
								entity.secondaryanim_frameindex >= secondaryAnimation.anIntArray353.length ||
								(secondaryAnimation.replay && entity.secondaryanim_replaycount >= secondaryAnimation.replaycount)) {

							entity.secondaryanim_loops_remaining = 0;
							entity.secondaryanim_frameindex = 0;
							entity.secondaryanim_replaycount = 0;
						}
					} else {
						entity.secondaryanim_loops_remaining = 0;
						entity.secondaryanim_frameindex = 0;
					}

					play_frames_sound(secondaryAnimation, entity.primaryanim_frameindex, entity);
				}

			} else if (secondaryAnimation.using_keyframes()) {
				++entity.secondaryanim_frameindex;
				int var6 = secondaryAnimation.get_keyframe_duration();

				if (entity.secondaryanim_frameindex < var6) {
					play_frames_sound(secondaryAnimation, entity.primaryanim_frameindex, entity);
				} else {
					if (secondaryAnimation.loop_delay > 0) {
						entity.secondaryanim_frameindex -= secondaryAnimation.loop_delay;

						if (secondaryAnimation.replay) {
							++entity.secondaryanim_replaycount;
						}

						if (entity.secondaryanim_frameindex < 0 ||
								entity.secondaryanim_frameindex >= var6 ||
								(secondaryAnimation.replay && entity.secondaryanim_replaycount >= secondaryAnimation.replaycount)) {

							entity.secondaryanim_frameindex = 0;
							entity.secondaryanim_loops_remaining = 0;
							entity.secondaryanim_replaycount = 0;
						}
					} else {
						entity.secondaryanim_loops_remaining = 0;
						entity.secondaryanim_frameindex = 0;
					}

					play_frames_sound(secondaryAnimation, entity.primaryanim_frameindex, entity);
				}
			} else {
				entity.secondaryanim = -1;
			}
		}

		if (entity.spotanim != -1 && update_tick >= entity.spotanim_start_loop) {
			if (entity.spotanimframe_index < 0)
				entity.spotanimframe_index = 0;

			if (GraphicsDefinition.cache[entity.spotanim] == null || GraphicsDefinition.cache[entity.spotanim].seqtype == null) {
				entity.spotanim = -1;
			} else {
				AnimationDefinition spotanim_seq = GraphicsDefinition.cache[entity.spotanim].seqtype;

				if (spotanim_seq != null && spotanim_seq.anIntArray353 != null && !spotanim_seq.using_keyframes()) {
					++entity.spotanim_loop;

					if (entity.spotanimframe_index < spotanim_seq.anIntArray353.length &&
							entity.spotanim_loop > spotanim_seq.anIntArray354[entity.spotanimframe_index]) {

						entity.spotanim_loop = 1;
						++entity.spotanimframe_index;
						play_frames_sound(spotanim_seq, entity.primaryanim_frameindex, entity);
					}

					if (entity.spotanimframe_index >= spotanim_seq.anIntArray353.length &&
							(entity.spotanimframe_index < 0 || entity.spotanimframe_index >= spotanim_seq.anIntArray353.length)) {

						entity.spotanim = -1;
					}
				} else if (spotanim_seq.using_keyframes()) {
					++entity.spotanimframe_index;
					int var4 = spotanim_seq.get_keyframe_duration();

					if (entity.spotanimframe_index < var4) {
						play_frames_sound(spotanim_seq, entity.primaryanim_frameindex, entity);
					} else if (entity.spotanimframe_index < 0 || entity.spotanimframe_index >= var4) {
						entity.spotanim = -1;
					}
				} else {
					entity.spotanim = -1;
				}
			}
		}

		if (entity.primaryanim != -1 && entity.primaryanim_pause <= 1) {
			if (entity.primaryanim >= AnimationDefinition.anims.length) {
				entity.primaryanim = -1;
				return;
			}

			AnimationDefinition animation_2 = AnimationDefinition.anims[entity.primaryanim];

			if (animation_2.exactmove == 1 && entity.anim_delay > 0 &&
					entity.exactmove_start <= update_tick && entity.exactmove_end < update_tick) {

				entity.primaryanim_pause = 1;
				return;
			}
		}

		if (entity.primaryanim != -1 && entity.primaryanim_pause == 0) {
			AnimationDefinition seqtype = AnimationDefinition.anims[entity.primaryanim];

			if (seqtype == null) {
				entity.primaryanim = -1;
			} else if (seqtype.using_keyframes() || seqtype.anIntArray353 == null) {
				if (seqtype.using_keyframes()) {
					++entity.primaryanim_frameindex;
					int skeletal_duration = seqtype.get_keyframe_duration();

					if (entity.primaryanim_frameindex < skeletal_duration) {
						play_frames_sound(seqtype, entity.primaryanim_frameindex, entity);
					} else {
						entity.primaryanim_frameindex -= seqtype.loop_delay;
						++entity.primaryanim_replaycount;

						if (entity.primaryanim_replaycount >= seqtype.replaycount) {
							entity.primaryanim = -1;
						} else if (entity.primaryanim_frameindex >= 0 && entity.primaryanim_frameindex < skeletal_duration) {
							play_frames_sound(seqtype, entity.primaryanim_frameindex, entity);
						} else {
							entity.primaryanim = -1;
						}
					}
				} else {
					entity.primaryanim = -1;
				}
			} else {
				++entity.primaryanim_loops_remaining;

				if (entity.primaryanim_frameindex < seqtype.anIntArray353.length &&
						entity.primaryanim_loops_remaining > seqtype.anIntArray354[entity.primaryanim_frameindex]) {

					entity.primaryanim_loops_remaining = 1;
					++entity.primaryanim_frameindex;
					play_frames_sound(seqtype, entity.primaryanim_frameindex, entity);
				}

				if (entity.primaryanim_frameindex >= seqtype.anIntArray353.length) {
					entity.primaryanim_frameindex -= seqtype.loop_delay;
					++entity.primaryanim_replaycount;

					if (entity.primaryanim_replaycount >= seqtype.replaycount) {
						entity.primaryanim = -1;
					} else if (entity.primaryanim_frameindex >= 0 && entity.primaryanim_frameindex < seqtype.anIntArray353.length) {
						play_frames_sound(seqtype, entity.primaryanim_frameindex, entity);
					} else {
						entity.primaryanim = -1;
					}
				}

				entity.is_walking = seqtype.stretches;
			}
		}

		if (entity.primaryanim_pause > 0)
			entity.primaryanim_pause--;
	}
	private static void play_frames_sound(AnimationDefinition seqtype, int frameindex, Entity entity) {
		if (seqtype.getFrameSound(frameindex) != -1) {
			entity.makeSound(seqtype.getFrameSound(frameindex));
		}
	}

	private static void play_keyframes_sound(int frameindex, Entity entity, AnimationDefinition seqtype) {
		if (seqtype.keyframe_soundeffects != null && seqtype.keyframe_soundeffects.containsKey(frameindex)) {
			entity.makeSound((Integer) seqtype.keyframe_soundeffects.get(frameindex));
		}
	}

	private void drawGameScreen() {
	    // Cache screen mode to avoid repeated comparisons
	    final boolean isFixed = (currentScreenMode == ScreenMode.FIXED);
	    
	    // Welcome screen handling
	    if (welcomeScreenRaised) {
	        handleWelcomeScreen(isFixed);
	    }

	    // Early exit for fullscreen interfaces
	    if (fullscreenInterfaceID != -1 && (loadingStage == 2 || super.fullGameScreen != null)) {
	        drawFullscreenInterface(isFixed);
	        return;
	    }

	    // Main game rendering
	    if (loadingStage == 2) {
	        method146(); // <-- 3D scene rendering (THE BOTTLENECK)
	    }

	    // Update flags - batch these checks
	    updateInterfaceFlags(isFixed);

	    // Draw UI components only if needed
	    if (isFixed) {
	        drawTabArea();
	    }
	    
	    // Chat area handling
	    if (backDialogID == -1) {
	        handleChatScrolling(isFixed);
	    } else {
	        if (method119(tickDelta, backDialogID)) {
	            inputTaken = true;
	        }
	    }

	    // Batch all input-related redraws
	    if (shouldRedrawInput()) {
	        drawChatArea();
	        inputTaken = false;
	    }

	    // Hover menu setup (only when loading stage == 2)
	    if (loadingStage == 2 && super.getMouseY() >= 127 && super.getMouseY() <= 158) {
	        setupHoverMenus();
	    }

	    // Minimap (expensive - only if needed)
	    if (isFixed && !stretched) {
	        drawMinimap();
	        mapAreaGraphicsBuffer.drawGraphics(516, 0, super.graphics);
	    }

	    tickDelta = 0;
	}

	// Helper methods to reduce complexity
	private void handleWelcomeScreen(boolean isFixed) {
	    welcomeScreenRaised = false;
	    needDrawTabArea = true;
	    inputTaken = true;
	    tabAreaAltered = true;
	    if (loadingStage != 2 && !stretched) {
	        mainGameGraphicsBuffer.drawGraphics(0, super.graphics, 0);
	        if (isFixed) {
	            mapAreaGraphicsBuffer.drawGraphics(516, 0, super.graphics);
	        }
	    }
	}

	private void updateInterfaceFlags(boolean isFixed) {
	    // Batch all flag checks
	    if (menuOpen && menuScreenArea == 1) needDrawTabArea = true;
	    if (atInventoryInterfaceType == 2) needDrawTabArea = true;
	    if (activeInterfaceType == 2) needDrawTabArea = true;
	    if (atInventoryInterfaceType == 3) inputTaken = true;
	    if (activeInterfaceType == 3) inputTaken = true;
	    if (aString844 != null) inputTaken = true;
	    if (menuOpen && menuScreenArea == 2) inputTaken = true;
	    
	    // Interface updates
	    if (invOverlayInterfaceID != -1) {
	        if (method119(tickDelta, invOverlayInterfaceID)) {
	            needDrawTabArea = true;
	        }
	    }
	}

	private boolean shouldRedrawInput() {
	    return inputTaken || 
	           atInventoryInterfaceType == 3 || 
	           activeInterfaceType == 3 || 
	           aString844 != null || 
	           (menuOpen && menuScreenArea == 2);
	}

	private void handleChatScrolling(boolean isFixed) {
	    if (inputDialogState != 3) {
	        aClass9_1059.scrollPosition = chatAreaScrollLength - anInt1089 - 110;
	        
	        int mouseY = getMouseY();
	        int chatBaseY = isFixed ? 348 : currentGameHeight - 155;
	        int chatCheckY = isFixed ? 342 : currentGameHeight - 159;
	        
	        if (getMouseX() > 478 && getMouseX() < 580 && mouseY > chatCheckY) {
	            method65(494, 110, getMouseX(), mouseY - chatBaseY, 
	                    aClass9_1059, 0, false, chatAreaScrollLength);
	        }
	        
	        int newScroll = Math.max(0, Math.min(chatAreaScrollLength - 110, 
	                chatAreaScrollLength - 110 - aClass9_1059.scrollPosition));
	        
	        if (anInt1089 != newScroll) {
	            anInt1089 = newScroll;
	            inputTaken = true;
	        }
	    }
	}

	private void setupHoverMenus() {
	    // Consolidated hover menu logic
	    if (runHover) {
	        menuActionName[1] = "Toggle Run";
	        menuActionID[1] = 1050;
	        menuActionRow = 2;
	    }
	    if (counterHover) {
	        menuActionName[2] = counterOn ? "Toggle XP Display" : "Show XP Display";
	        menuActionID[2] = 474;
	        menuActionName[1] = "Reset XP Total";
	        menuActionID[1] = 475;
	        menuActionRow = 3;
	    }
	    if (worldHover) {
	        menuActionName[2] = "Heat @or1@World Map";
	        menuActionID[2] = 852;
	        menuActionName[1] = "Regional @or1@World Map";
	        menuActionID[1] = 851;
	        menuActionRow = 3;
	    }
	    if (wikiHover) {
	        menuActionName[1] = "search @or1@Wiki";
	        menuActionID[1] = 855;
	        menuActionRow = 2;
	    }
	    if (prayHover) {
	        menuActionName[2] = prayClicked ? "Turn Quick Prayers Off" : "Turn Quick Prayers On";
	        menuActionID[2] = 1500;
	        menuActionName[1] = "Select Quick Prayers";
	        menuActionID[1] = 1506;
	        menuActionRow = 3;
	    }
	}

	private void drawFullscreenInterface(boolean isFixed) {
	    if (loadingStage == 2) {
	        method119(tickDelta, fullscreenInterfaceID);
	        if (openInterfaceID != -1) {
	            method119(tickDelta, openInterfaceID);
	        }
	        tickDelta = 0;
	        resetAllImageProducers();
	        super.fullGameScreen.initDrawingArea();
	        Rasterizer.anIntArray1472 = fullScreenTextureArray;
	        DrawingArea.setAllPixelsToZero();
	        welcomeScreenRaised = true;
	        
	        // Draw open interface if present
	        if (openInterfaceID != -1) {
	            RSInterface rsInterface = RSInterface.interfaceCache[openInterfaceID];
	            resizeInterfaceIfNeeded(rsInterface);
	            drawInterface(
	                isFixed ? 0 : worldViewportHeight / 2 - 503,
	                isFixed ? 0 : worldViewportWidth / 2 - 383,
	                rsInterface, 8
	            );
	        }
	        
	        // Draw fullscreen interface
	        RSInterface rsInterface = RSInterface.interfaceCache[fullscreenInterfaceID];
	        resizeInterfaceIfNeeded(rsInterface);
	        drawInterface(
	            isFixed ? 0 : worldViewportHeight / 2 - 400,
	            isFixed ? 0 : worldViewportWidth / 2 - 383,
	            rsInterface, 8
	        );
	        
	        // Menu and tooltips
	        if (!menuOpen) {
	            processRightClick();
	            drawTopLeftTooltip();
	        } else {
	            drawMenu(0, 0);
	        }
	    }
	    
	    drawCount++;
	    super.fullGameScreen.drawGraphics(0, 0, super.graphics);
	}

	private void resizeInterfaceIfNeeded(RSInterface rsInterface) {
	    // Consolidate all the repeated resize checks
	    if (rsInterface.type == 0 || rsInterface.type == 3) {
	        if ((rsInterface.width == 512 && rsInterface.height == 334) ||
	            (rsInterface.width == 513 && rsInterface.height == 335) ||
	            (rsInterface.width == 562 && rsInterface.height == 362)) {
	            rsInterface.width = worldViewportWidth;
	            rsInterface.height = worldViewportHeight;
	        }
	    }
	}

	private boolean buildFriendsListMenu(RSInterface class9) {
		int i = class9.contentType;
		if (i >= 1 && i <= 200 || i >= 701 && i <= 900) {
			if (i >= 801)
				i -= 701;
			else if (i >= 701)
				i -= 601;
			else if (i >= 101)
				i -= 101;
			else
				i--;
			if (tabInterfaceIDs[tabID] != 42500) {
				menuActionName[menuActionRow] = "Remove @whi@" + friendsList[i];
				menuActionID[menuActionRow] = 792;
				menuActionRow++;
				menuActionName[menuActionRow] = "Message @whi@" + friendsList[i];
				menuActionID[menuActionRow] = 639;
				menuActionRow++;
			}

			return true;
		}
		if (i >= 401 && i <= 500) {
			menuActionName[menuActionRow] = "Remove @whi@" + class9.message;
			menuActionID[menuActionRow] = 322;
			menuActionRow++;
			return true;
		} else {
			return false;
		}
	}

	private int hoverId;

	public void method104() {
		Animable_Sub3 class30_sub2_sub4_sub3 = (Animable_Sub3) aClass19_1056.reverseGetFirst();
		for (; class30_sub2_sub4_sub3 != null; class30_sub2_sub4_sub3 = (Animable_Sub3) aClass19_1056.reverseGetNext())
			if (class30_sub2_sub4_sub3.anInt1560 != plane || class30_sub2_sub4_sub3.aBoolean1567)
				class30_sub2_sub4_sub3.unlink();
			else if (update_tick >= class30_sub2_sub4_sub3.anInt1564) {
				class30_sub2_sub4_sub3.method454(tickDelta);
				if (class30_sub2_sub4_sub3.aBoolean1567)
					class30_sub2_sub4_sub3.unlink();
				else
					scene.method285(class30_sub2_sub4_sub3.anInt1560, 0, class30_sub2_sub4_sub3.anInt1563, -1,
							class30_sub2_sub4_sub3.anInt1562, 60, class30_sub2_sub4_sub3.anInt1561,
							class30_sub2_sub4_sub3, false);
			}

	}

	public int dropdownInversionFlag;

	public Sprite grandExchangeSprite0 = new Sprite("Grand Exchange/0"),
			grandExchangeSprite1 = new Sprite("Grand Exchange/1"),
			grandExchangeSprite2 = new Sprite("Grand Exchange/2"),
			grandExchangeSprite3 = new Sprite("Grand Exchange/3"),
			grandExchangeSprite4 = new Sprite("Grand Exchange/4");

	private int interfaceDrawY;
	public boolean interfaceText = false;
	public boolean interfaceStringText = false;

	public static final int[] runeChildren = { 1202, 1203, 1209, 1210, 1211, 1218, 1219, 1220, 1227, 1228, 1234, 1235, 1236, 1243,
			1244, 1245, 1252, 1253, 1254, 1261, 1262, 1263, 1270, 1271, 1277, 1278, 1279, 1286, 1287, 1293,
			1294, 1295, 1302, 1303, 1304, 1311, 1312, 1318, 1319, 1320, 1327, 1328, 1329, 1336, 1337, 1343,
			1344, 1345, 1352, 1353, 1354, 1361, 1362, 1363, 1370, 1371, 1377, 1378, 1384, 1385, 1391, 1392,
			1393, 1400, 1401, 1407, 1408, 1410, 1417, 1418, 1424, 1425, 1426, 1433, 1434, 1440, 1441, 1442,
			1449, 1450, 1456, 1457, 1463, 1464, 1465, 1472, 1473, 1474, 1481, 1482, 1488, 1489, 1490, 1497,
			1498, 1499, 1506, 1507, 1508, 1515, 1516, 1517, 1524, 1525, 1526, 1533, 1534, 1535, 1547, 1548,
			1549, 1556, 1557, 1558, 1566, 1567, 1568, 1576, 1577, 1578, 1586, 1587, 1588, 1596, 1597, 1598,
			1605, 1606, 1607, 1616, 1617, 1618, 1627, 1628, 1629, 1638, 1639, 1640, 6007, 6008, 6011, 8673,
			8674, 12041, 12042, 12429, 12430, 12431, 12439, 12440, 12441, 12449, 12450, 12451, 12459, 12460,
			15881, 15882, 15885, 18474, 18475, 18478 };

	public static final int[] SOME_IDS = { 1196, 1199, 1206, 1215, 1224, 1231, 1240, 1249, 1258, 1267, 1274, 1283, 1573, 1290, 1299,
			1308, 1315, 1324, 1333, 1340, 1349, 1358, 1367, 1374, 1381, 1388, 1397, 1404, 1583, 12038, 1414,
			1421, 1430, 1437, 1446, 1453, 1460, 1469, 15878, 1602, 1613, 1624, 7456, 1478, 1485, 1494, 1503,
			1512, 1521, 1530, 1544, 1553, 1563, 1593, 1635, 12426, 12436, 12446, 12456, 6004, 18471,
			/* Ancients */
			12940, 12988, 13036, 12902, 12862, 13046, 12964, 13012, 13054, 12920, 12882, 13062, 12952, 13000,
			13070, 12912, 12872, 13080, 12976, 13024, 13088, 12930, 12892, 13096 };


	private static final int[] BOUNTY_INTERFACE_IDS = {
			21001, 28030, 28006, 28021, 28003, 28004, 28005, 28029,
			28023, 28024, 28025, 28026, 28027, 28028, 28001, 28022
	};

	public void drawInterface(int scrollPosition, int xPosition, RSInterface rsInterface, int yPosition) {
		drawInterface(scrollPosition, xPosition, rsInterface, yPosition, false);
	}

	public void drawInterface(int scrollPosition, int xPosition, RSInterface rsInterface, int yPosition, boolean inheritDrawingArea) {
		try {
			if (rsInterface == null) {
				rsInterface = RSInterface.interfaceCache[70000];
			}
			if (rsInterface.type != 0 || rsInterface.children == null)
				return;
			if (rsInterface.isMouseoverTriggered && anInt1026 != rsInterface.id && anInt1048 != rsInterface.id
					&& anInt1039 != rsInterface.id)
				return;


			int clipLeft = DrawingArea.topX;
			int clipTop = DrawingArea.topY;
			int clipRight = DrawingArea.bottomX;
			int clipBottom = DrawingArea.bottomY;

			// Sets the drawing area so things don't draw outside the boundary (scrollbars are a good example)
			// If you have a type 0 (container) inside another type 0 the latest one of the tree will set the drawing area
			// and possibly breaking scrolling (making things draw above or below the scrolling container)
			//if (rsInterface.id <= 24507)
			if (!inheritDrawingArea) {
				DrawingArea.setDrawingArea(yPosition + rsInterface.height, xPosition, xPosition + rsInterface.width, yPosition);
			}
			int childCount = rsInterface.children.length;
			for (int childId = 0; childId < childCount; childId++) {
				try {
					int _x = rsInterface.childX[childId] + xPosition;
					int _y = (rsInterface.childY[childId] + yPosition) - scrollPosition;
					RSInterface class9_1 = RSInterface.interfaceCache[rsInterface.children[childId]];

					_x += class9_1.anInt263;
					_y += class9_1.anInt265;

					if (class9_1.interfaceHidden)
						continue;

					if (!getUserSettings().isBountyHunter() && Arrays.stream(BOUNTY_INTERFACE_IDS).anyMatch(id -> id == class9_1.id)) {
						continue;
					}

					if (class9_1.contentType > 0)
						drawFriendsListOrWelcomeScreen(class9_1);

					if (class9_1.id != 28060 && class9_1.id != 28061) {
						for (int m5 = 0; m5 < SOME_IDS.length; m5++) {
							if (class9_1.id == SOME_IDS[m5] + 1) {
								if (m5 > 61) {
									drawBlackBox(_x + 1, _y);
								} else {
									drawBlackBox(_x, _y + 1);
								}
							}
						}
					}

					for (int r = 0; r < runeChildren.length; r++)
						if (class9_1.id == runeChildren[r])
							class9_1.modelZoom = 775;

					if (class9_1.type == 0) {
						if (class9_1.scrollPosition > class9_1.scrollMax - class9_1.height)
							class9_1.scrollPosition = class9_1.scrollMax - class9_1.height;
						if (class9_1.scrollPosition < 0)
							class9_1.scrollPosition = 0;
						drawInterface(class9_1.scrollPosition, _x, class9_1, _y, inheritDrawingArea || rsInterface.scrollMax > 0);

						// Hardcodes
						if (class9_1.scrollMax > class9_1.height) {
							// clan chat
							if (class9_1.id == 18143) {
								int clanMates = 0;
								for (int i = 18155; i < 18244; i++) {
									RSInterface line = RSInterface.interfaceCache[i];
									if (line.message.length() > 0) {
										clanMates++;
									}
								}
								class9_1.scrollMax = (clanMates * 14) + class9_1.height + 1;
							}
							if (class9_1.id == 18322 || class9_1.id == 18423) {
								int members = 0;
								for (int i = class9_1.id + 1; i < class9_1.id + 1 + 100; i++) {
									RSInterface line = RSInterface.interfaceCache[i];
									if (line != null && line.message != null) {
										if (line.message.length() > 0) {
											members++;
										}
									}
								}
								class9_1.scrollMax = (members * 14) + 1;
							}
							if (rsInterface.parentID == 49000 || rsInterface.parentID == 49100 || rsInterface.parentID == 51100
									|| rsInterface.parentID == 53100) {
								int scrollMax = class9_1.scrollMax;

								if (achievementCutoff > 1) {
									scrollMax = achievementCutoff * 65;
								} else {
									achievementCutoff = 282;
								}
								class9_1.scrollMax = scrollMax;
							}
							if (scrollbarVisible(class9_1)) {
								drawScrollbar(class9_1.height, class9_1.scrollPosition, _y, _x + class9_1.width,
										class9_1.scrollMax);
							}
						}

					} else if (class9_1.type != 1)
						if (class9_1.type == 2) {
							if (interfaceText) {
								newSmallFont.drawBasicString("Container: " + class9_1.id, _x, _y);
							}
							try {
								Bank.setupMainTab(class9_1, _x, _y);
							} catch (Exception e) {
								e.printStackTrace();
							}

							// Item container
							if (class9_1.invAutoScrollHeight) {
								int lastRow = -1;
								int rowCount = 0;
								int i3 = 0;

								for (int l3 = 0; l3 < class9_1.height; l3++) {
									for (int l4 = 0; l4 < class9_1.width; l4++) {
										if (class9_1.inventoryItemId[i3] > 1) {
											if (lastRow != l3) {
												lastRow = l3;
												rowCount++;
											}
										}

										i3++;
									}
								}

								RSInterface scrollable = RSInterface.interfaceCache[class9_1.invAutoScrollInterfaceId];
								scrollable.scrollMax = scrollable.height + 1;
								int heightPerRow = class9_1.invSpritePadY + 32;
								if (heightPerRow * rowCount > scrollable.scrollMax) {
									scrollable.scrollMax += (heightPerRow * rowCount) - scrollable.scrollMax + scrollable.invAutoScrollHeightOffset;
								}
							}

							int i3 = 0;
							for (int l3 = 0; l3 < class9_1.height; l3++) {
								for (int l4 = 0; l4 < class9_1.width; l4++) {
									// System.out.println("id? " + class9_1.inv[i3]);
									int k5 = _x + l4 * (32 + class9_1.invSpritePadX);
									int j6 = _y + l3 * (32 + class9_1.invSpritePadY);
									if (i3 < 20) {
										k5 += class9_1.spritesX[i3];
										j6 += class9_1.spritesY[i3];
									}
									if (class9_1.inventoryItemId[i3] > 0) {
										int k6 = 0;
										int j7 = 0;
										int j9 = class9_1.inventoryItemId[i3] - 1;
										if (class9_1.id == 30000) {
											j9 = (class9_1.inventoryItemId[i3] & 0x7FFF) - 1;
										}
										if (k5 > DrawingArea.topX - 32 && k5 < DrawingArea.bottomX && j6 > DrawingArea.topY - 32
												&& j6 < DrawingArea.bottomY || activeInterfaceType != 0 && itemDraggingSlot == i3) {
											int l9 = 0;
											if (itemSelected == 1 && anInt1283 == i3 && anInt1284 == class9_1.id)
												l9 = 0xffffff;

											int itemSpriteOpacity = 256;
											/**
											 * Placeholder opacity editing
											 */
											if (class9_1.inventoryAmounts[i3] <= 0)
											    itemSpriteOpacity = 100;

											Sprite itemSprite = null;

											if (openInterfaceID == 29875) {
											    if (class9_1.inventoryItemId[i3] < 555
											            || class9_1.inventoryItemId[i3] > 567 && class9_1.inventoryItemId[i3] != 9076) {
											        itemSpriteOpacity = 100;
											    }
											}

											if (class9_1.id == 23121) {
											    final boolean isLastBitEnabled = ((class9_1.inventoryItemId[i3] >> 15) & 0x1) == 1;
											    if (isLastBitEnabled) {
											        itemSpriteOpacity = 90;
											    }
											}

											boolean smallSprite = openInterfaceID == 26000
											        && GrandExchange.isSmallItemSprite(class9_1.id) || class9_1.smallInvSprites;

											ItemDefinition itemDef = ItemDefinition.forID(j9);
											if (smallSprite) {
											    itemSprite = ItemDefinition.getSmallSprite(j9, class9_1.inventoryAmounts[i3]);									    
											    if (itemDef.customSmallSpriteLocation != null)
											    {
											        itemSprite = new Sprite(itemDef.customSmallSpriteLocation);
											    } else if (itemDef.customSpriteLocation != null)
											    {
											        itemSprite = new Sprite(itemDef.customSpriteLocation);
											    }
											} else {
											    itemSprite = ItemDefinition.getSprite(j9, class9_1.inventoryAmounts[i3], l9, 0, false);
											    if (itemDef.customSpriteLocation != null)
											    {
											        itemSprite = new Sprite(itemDef.customSpriteLocation);
											    }
											}

											if (class9_1.id >= 32212 && class9_1.id <= 32212 + 11) {
											    if (class9_1.inventoryItemId[i3] > 0) {
											        if (class9_1.sprite2 != null) {
											            class9_1.sprite2.drawSprite(k5 + k6 - 2, j6 + j7 - 2);
											        }
											    }
											}
											if (itemSprite != null) {
											    if (activeInterfaceType != 0 && itemDraggingSlot == i3 && draggingItemInterfaceId == class9_1.id) {
											        k6 = super.getMouseX() - anInt1087;
											        j7 = super.getMouseY() - anInt1088;
											        if (k6 < 5 && k6 > -5)
											            k6 = 0;
											        if (j7 < 5 && j7 > -5)
											            j7 = 0;
											        if (anInt989 < getDragSetting(class9_1.id)) {
											            k6 = 0;
											            j7 = 0;
											        }
											        itemSprite.drawTransparentSprite(k5 + k6, j6 + j7, 128);
											        if (j6 + j7 < DrawingArea.topY && rsInterface.scrollPosition > 0) {
											            int i10 = (tickDelta * (DrawingArea.topY - j6 - j7)) / 3;
											            if (i10 > tickDelta * 10)
											                i10 = tickDelta * 10;
											            if (i10 > rsInterface.scrollPosition)
											                i10 = rsInterface.scrollPosition;
											            rsInterface.scrollPosition -= i10;
											            anInt1088 += i10;
											        }
											        if (j6 + j7 + 32 > DrawingArea.bottomY
											                && rsInterface.scrollPosition < rsInterface.scrollMax
											                - rsInterface.height) {
											            int j10 = (tickDelta * ((j6 + j7 + 32) - DrawingArea.bottomY)) / 3;
											            if (j10 > tickDelta * 10)
											                j10 = tickDelta * 10;
											            if (j10 > rsInterface.scrollMax - rsInterface.height
											                    - rsInterface.scrollPosition)
											                j10 = rsInterface.scrollMax - rsInterface.height
											                        - rsInterface.scrollPosition;
											            rsInterface.scrollPosition += j10;
											            anInt1088 -= j10;
											        }
											    } else if (atInventoryInterfaceType != 0 && atInventoryIndex == i3
											            && atInventoryInterface == class9_1.id)
											        itemSprite.drawTransparentSprite(k5, j6, 128);
											    else
											        /**
											         * Draws item sprite
											         */
											        itemSprite.drawTransparentSprite(k5, j6, itemSpriteOpacity);
											    if (class9_1.id == RSInterface.selectedItemInterfaceId
											            && class9_1.itemSearchSelectedSlot > -1
											            && class9_1.itemSearchSelectedSlot == i3) {
											        for (int i = 32; i > 0; i--) {
											            DrawingArea.method338(j6 + j7, i, 256 - Byte.MAX_VALUE, 0x395D84, i,
											                    k5 + k6);
											        }
											        DrawingArea.method338(j6 + j7, 32, 256, 0x395D84, 32, k5 + k6);
											    }

											    if (class9_1.forceInvStackSizes || !smallSprite && !class9_1.hideInvStackSizes) {
											        if (class9_1.parentID < 58040 || class9_1.parentID > 58048) {
											            if (itemSprite.maxWidth == 33 || class9_1.inventoryAmounts[i3] != 1) {
											                if (class9_1.id != 23121 || class9_1.id == 23121 && ((class9_1.inventoryItemId[i3] >> 15) & 0x1) == 0) {
											                    if (class9_1.invAlwaysInfinity || class9_1.id == Interfaces.SHOP_CONTAINER && class9_1.inventoryAmounts[i3] == 1_000_000_000) {
											                        infinity.drawSprite(k5 + k6, j6 + j7);
											                    } else {
											                        int k10 = class9_1.inventoryAmounts[i3];
											                        int xOffset = class9_1.forceInvStackSizes ? 10 : 0;
											                        if (k10 >= 1)
											                            smallText.method385(0xFFFF00, intToKOrMil(k10), j6 + 9 + j7,
											                                    xOffset+ k5 + k6);
											                        smallText.method385(0, intToKOrMil(k10), j6 + 10 + j7, xOffset+ k5 + 1 + k6);
											                        if (k10 > 99999 && k10 < 10000000) {
											                            smallText.method385(0xFFFFFF, intToKOrMil(k10), j6 + 9 + j7,
											                                    xOffset+ k5 + k6);
											                        } else if (k10 > 9999999) {
											                            smallText.method385(0x00ff80, intToKOrMil(k10), j6 + 9 + j7,
											                                    xOffset+ k5 + k6);
											                        } else {
											                            smallText.method385(0xFFFF00, intToKOrMil(k10), j6 + 9 + j7,
											                                    xOffset+ k5 + k6);
											                        }
											                    }
											                }
											            }
											        }
											    }
											}
										}
									} else if (class9_1.sprites != null && i3 < 20) {
										Sprite class30_sub2_sub1_sub1_1 = class9_1.sprites[i3];
										if (class30_sub2_sub1_sub1_1 != null)
											class30_sub2_sub1_sub1_1.drawSprite(k5, j6);
									}
									i3++;
								}
							}
						} else if (class9_1.type == 3) {
							boolean flag = false;
							if (anInt1039 == class9_1.id || anInt1048 == class9_1.id || anInt1026 == class9_1.id)
								flag = true;
							int colour;
							if (cs1_selected(class9_1)) {
								colour = class9_1.secondaryColor;
								if (flag && class9_1.anInt239 != 0)
									colour = class9_1.anInt239;
							} else {
								colour = class9_1.textColor;
								if (flag && class9_1.anInt216 != 0)
									colour = class9_1.anInt216;
							}
							if (class9_1.aByte254 == 0) {
								if (class9_1.aBoolean227) {
									if (class9_1.width == 562 || class9_1.height == 362) {
										class9_1.width = worldViewportWidth;
										class9_1.height = worldViewportHeight;
									}
									DrawingArea.drawPixels(class9_1.height, _y, _x, colour, class9_1.width);
								} else {
									DrawingArea.fillPixels(_x, class9_1.width, class9_1.height, colour, _y);
								}
							} else if (class9_1.aBoolean227) {
								DrawingArea.method335(colour, _y, class9_1.width, class9_1.height,
										256 - (class9_1.aByte254 & 0xff), _x);
							} else {
								DrawingArea.method338(_y, class9_1.height, 256 - (class9_1.aByte254 & 0xff), colour,
										class9_1.width, _x);
							}
						} else if (class9_1.type == 4 || class9_1.type == RSInterface.TYPE_TEXT_DRAW_FROM_LEFT) {
							TextDrawingArea textDrawingArea = class9_1.textDrawingAreas;
							String s = class9_1.message;
							if (interfaceStringText) {
								s = class9_1.id + "";
							}
							boolean flag1 = false;
							if (anInt1039 == class9_1.id || anInt1048 == class9_1.id || anInt1026 == class9_1.id)
								flag1 = true;
							int i4 = 0;
							if (cs1_selected(class9_1)) {
								i4 = class9_1.secondaryColor;
								if (flag1 && class9_1.anInt239 != 0)
									i4 = class9_1.anInt239;
								if (class9_1.aString228.length() > 0)
									s = class9_1.aString228;
							} else {
								i4 = class9_1.textColor;
								if (flag1 && class9_1.anInt216 != 0) {
									i4 = class9_1.anInt216;
									if (i4 == 49152)
										i4 = 16777215;
								}
							}
							if (class9_1.atActionType == 6 && aBoolean1149) {
								s = "Please wait...";
								i4 = 255;
							}
							if ((class9_1.parentID == 1151) || (class9_1.parentID == 12855)) {
								switch (i4) {
									case 16773120:
										i4 = 0xFE981F;
										break;
									case 7040819:
										i4 = 0xAF6A1A;
										break;
								}
							}
							if (class9_1.hoverText != null && !class9_1.hoverText.isEmpty()) {
								if (super.getMouseX() > _x
										&& super.getMouseX() < _x + class9_1.textDrawingAreas.getTextWidth(class9_1.message)
										&& super.getMouseY() > _y && super.getMouseY() < _y + 15) {
									s = class9_1.hoverText;
									i4 = class9_1.hoverTextColor;
								}
							}
							if((backDialogID != -1 || dialogID != -1 || class9_1.message.contains("Click here to continue")) &&
									(rsInterface.id == backDialogID || rsInterface.id == dialogID )){
								if(i4 == 0xffff00)
									i4 = 255;
								if(i4 == 49152)
									i4 = 0xffffff;
							}
							for (int l6 = _y + textDrawingArea.anInt1497; s.length() > 0; l6 += textDrawingArea.anInt1497) {
								if (s.indexOf("%") != -1) {
									do {
										int k7 = s.indexOf("%1");
										if (k7 == -1)
											break;
										if (class9_1.id < 4000 || class9_1.id > 5000 && class9_1.id != 13921
												&& class9_1.id != 13922 && class9_1.id != 12171 && class9_1.id != 12172)
											s = s.substring(0, k7) + methodR(extractInterfaceValues(class9_1, 0))
													+ s.substring(k7 + 2);
										else
											s = s.substring(0, k7) + interfaceIntToString(extractInterfaceValues(class9_1, 0))
													+ s.substring(k7 + 2);
									} while (true);
									do {
										int l7 = s.indexOf("%2");
										if (l7 == -1)
											break;
										s = s.substring(0, l7) + interfaceIntToString(extractInterfaceValues(class9_1, 1))
												+ s.substring(l7 + 2);
									} while (true);
									do {
										int i8 = s.indexOf("%3");
										if (i8 == -1)
											break;
										s = s.substring(0, i8) + interfaceIntToString(extractInterfaceValues(class9_1, 2))
												+ s.substring(i8 + 2);
									} while (true);
									do {
										int j8 = s.indexOf("%4");
										if (j8 == -1)
											break;
										s = s.substring(0, j8) + interfaceIntToString(extractInterfaceValues(class9_1, 3))
												+ s.substring(j8 + 2);
									} while (true);
									do {
										int k8 = s.indexOf("%5");
										if (k8 == -1)
											break;
										s = s.substring(0, k8) + interfaceIntToString(extractInterfaceValues(class9_1, 4))
												+ s.substring(k8 + 2);
									} while (true);
								}
								int l8 = s.indexOf("\\n");
								String s1;
								if (l8 != -1) {
									s1 = s.substring(0, l8);
									s = s.substring(l8 + 2);
								} else {
									s1 = s;
									s = "";
								}
								RSFont font = null;
								if (textDrawingArea == smallText) {
									font = newSmallFont;
								} else if (textDrawingArea == aTextDrawingArea_1271) {
									font = newRegularFont;
								} else if (textDrawingArea == chatTextDrawingArea) {
									font = newBoldFont;
								} else if (textDrawingArea == aTextDrawingArea_1273) {
									font = newFancyFont;
								}
								if (rsInterface.parentID == 49100 || rsInterface.parentID == 51100 || rsInterface.parentID == 53100) {
									int parent = rsInterface.parentID == 49100 ? 49100 : rsInterface.parentID == 51100 ? 51100 : 53100;
									int subId = (class9_1.id - parent) % 100;
									if (subId > achievementCutoff) {
										continue;
									}
								}
								if (interfaceStringText) {
									s1 = "" + class9_1.id;
								}

								if (class9_1.type == RSInterface.TYPE_TEXT_DRAW_FROM_LEFT) {
									int width = font.getTextWidth(s1);
									font.drawBasicString(s1, _x - width, l6, i4, class9_1.textShadow ? 0 : -1);
								} else if (class9_1.centerText) {
									font.drawCenteredString(s1, _x + class9_1.width / 2, l6, i4, class9_1.textShadow ? 0 : -1);
								} else {
									font.drawBasicString(s1, _x, l6, i4, class9_1.textShadow ? 0 : -1);
								}
							}
						} else if (class9_1.type == 5) {
							Sprite sprite;
							if(cs1_selected(class9_1) || class9_1.active) {
								sprite = class9_1.sprite2;
							} else {
								sprite = class9_1.sprite1;
							}



							if(spellSelected == 1 && class9_1.id == spellID && spellID != 0 && sprite != null) {
								sprite.drawSprite(_x, _y, 0xffffff);
							}

							if(sprite != null) {
								if (class9_1.drawsTransparent) {
									sprite.drawTransparentSprite(_x, _y, class9_1.transparency);
								} else {
									sprite.drawSprite(_x, _y);
								}
							}
						} else if (class9_1.type == 6) {
							int k3 = Rasterizer.textureInt1;
							int j4 = Rasterizer.textureInt2;
							Rasterizer.textureInt1 = _x + class9_1.width / 2;
							Rasterizer.textureInt2 = _y + class9_1.height / 2;
							int i5 = Rasterizer.Rasterizer3D_sine[class9_1.modelRotation1] * class9_1.modelZoom >> 16;
							int l5 = Rasterizer.Rasterizer3D_cosine[class9_1.modelRotation1] * class9_1.modelZoom >> 16;
							boolean flag2 = cs1_selected(class9_1);
							int seq_id;
							if (flag2)
								seq_id = class9_1.active_seq_id;
							else
								seq_id = class9_1.seq_id;
							Model model;
							if (seq_id == -1) {
								model = class9_1.get_if_model(null, -1, flag2);
							} else {
								AnimationDefinition animation = AnimationDefinition.anims[seq_id];
								if (class9_1.iftype_frameindex >= animation.anIntArray354.length || class9_1.iftype_frameindex >= animation.anIntArray353.length) {
									class9_1.iftype_frameindex = 0; // Fixes array index out of bounds on npc dialogues
								}

								model = class9_1.get_if_model(animation, class9_1.iftype_frameindex, flag2);
							}
							if (model != null)
								model.renderModel(class9_1.modelRotation2, 0, class9_1.modelRotation1, 0, i5, l5);
							Rasterizer.textureInt1 = k3;
							Rasterizer.textureInt2 = j4;
						} else if (class9_1.type == 7) {
							TextDrawingArea textDrawingArea_1 = class9_1.textDrawingAreas;
							int k4 = 0;
							for (int j5 = 0; j5 < class9_1.height; j5++) {
								for (int i6 = 0; i6 < class9_1.width; i6++) {
									if (class9_1.inventoryItemId[k4] > 0) {
										ItemDefinition itemDef = ItemDefinition.forID(class9_1.inventoryItemId[k4] - 1);
										String s2 = itemDef.name;
										if (itemDef.stackable || class9_1.inventoryAmounts[k4] != 1)
											s2 = s2 + " x" + intToKOrMilLongName(class9_1.inventoryAmounts[k4]);
										int i9 = _x + i6 * (115 + class9_1.invSpritePadX);
										int k9 = _y + j5 * (12 + class9_1.invSpritePadY);
										if (class9_1.centerText)
											textDrawingArea_1.method382(class9_1.textColor, i9 + class9_1.width / 2, s2, k9,
													class9_1.textShadow);
										else
											textDrawingArea_1.method389(class9_1.textShadow, i9, class9_1.textColor, s2, k9);
									}
									k4++;
								}
							}
						} else if (class9_1.type == 8
								&& (anInt1500 == class9_1.id || anInt1044 == class9_1.id || anInt1129 == class9_1.id)
								&& anInt1501 == 50 && !menuOpen) {
							if (class9_1.parentID == 3917) {
								return;
							}
							int boxWidth = 0;
							int boxHeight = 0;

							/**
							 * Skill tab hovers Remove "next level at" and "remaining" for xp if we're level
							 * 99.
							 */

							TextDrawingArea textDrawingArea_2 = aTextDrawingArea_1271;
							for (String s1 = class9_1.message; s1.length() > 0;) {
								if (s1.indexOf("%") != -1) {
									do {
										int k7 = s1.indexOf("%1");
										if (k7 == -1)
											break;
										s1 = s1.substring(0, k7) + interfaceIntToString(extractInterfaceValues(class9_1, 0))
												+ s1.substring(k7 + 2);
									} while (true);
									do {
										int l7 = s1.indexOf("%2");
										if (l7 == -1)
											break;
										s1 = s1.substring(0, l7) + interfaceIntToString(extractInterfaceValues(class9_1, 1))
												+ s1.substring(l7 + 2);
									} while (true);
									do {
										int i8 = s1.indexOf("%3");
										if (i8 == -1)
											break;
										s1 = s1.substring(0, i8) + interfaceIntToString(extractInterfaceValues(class9_1, 2))
												+ s1.substring(i8 + 2);
									} while (true);
									do {
										int j8 = s1.indexOf("%4");
										if (j8 == -1)
											break;
										s1 = s1.substring(0, j8) + interfaceIntToString(extractInterfaceValues(class9_1, 3))
												+ s1.substring(j8 + 2);
									} while (true);
									do {
										int k8 = s1.indexOf("%5");
										if (k8 == -1)
											break;
										s1 = s1.substring(0, k8) + interfaceIntToString(extractInterfaceValues(class9_1, 4))
												+ s1.substring(k8 + 2);
									} while (true);
								}
								int l7 = s1.indexOf("\\n");

								String s4;
								if (l7 != -1) {
									s4 = s1.substring(0, l7);
									s1 = s1.substring(l7 + 2);
								} else {
									s4 = s1;
									s1 = "";
								}
								int j10 = textDrawingArea_2.getTextWidth(s4);
								if (j10 > boxWidth) {
									boxWidth = j10;
								}
								boxHeight += textDrawingArea_2.anInt1497 + 1;
							}

							if (tabInterfaceIDs[tabID] == 17200) {
								return;
							}
							boxWidth += 6;
							boxHeight += 7;
							int xPos = (_x + class9_1.width) - 5 - boxWidth;
							int yPos = _y + class9_1.height + 5;
							if (xPos < _x + 5)
								xPos = _x + 5;
							if (xPos + boxWidth > xPosition + rsInterface.width)
								xPos = (xPosition + rsInterface.width) - boxWidth;
							if (yPos + boxHeight > yPosition + rsInterface.height)
								yPos = (_y - boxHeight);
							switch (class9_1.id) {
								case 25528:
								case 25531:
								case 25534:
								case 25537:
								case 25540:
								case 25547:
								case 25550:
								case 9238:
									xPos -= 80;
									break;
								case 25551:
									case 25552:
									yPos -= 100;
									break;
							}
							if (currentScreenMode == ScreenMode.FIXED) {
								if (class9_1.inventoryhover) {

									if (xPos + boxWidth > 249) {
										xPos = 265 - boxWidth - xPosition;
									}
									if (yPos + boxHeight + interfaceDrawY > 291 && yPos + boxHeight + interfaceDrawY < 321) {
										yPos = 255 - boxHeight - yPosition;
									} else if (yPos + boxHeight + interfaceDrawY > 322) {
										yPos = 275 - boxHeight - yPosition;
									}
								}
							} else {
								if (class9_1.inventoryhover) {

									//System.out.println(currentGameWidth - 648);
									if (xPos + boxWidth > currentGameWidth - 8 - boxWidth + 100) {
										xPos = currentGameWidth - 8 - boxWidth;
									}
									if (yPos + boxHeight > currentGameHeight - 118 - boxHeight + 100
											&& yPos + boxHeight < currentGameHeight - 118 - boxHeight + 120) {
										yPos = currentGameHeight - 148 - boxHeight;
									} else if (yPos + boxHeight > currentGameHeight - 118 - boxHeight + 100) {
										yPos = currentGameHeight - 118 - boxHeight;
									}
								}
							}
							DrawingArea.drawPixels(boxHeight, yPos, xPos, 0xFFFFA0, boxWidth);
							DrawingArea.fillPixels(xPos, boxWidth, boxHeight, 0, yPos);
							String s2 = class9_1.message;
							for (int j11 = yPos + textDrawingArea_2.anInt1497 + 2; s2
									.length() > 0; j11 += textDrawingArea_2.anInt1497 + 1) {// anInt1497
								if (s2.indexOf("%") != -1) {
									do {
										int k7 = s2.indexOf("%1");
										if (k7 == -1)
											break;
										s2 = s2.substring(0, k7) + interfaceIntToString(extractInterfaceValues(class9_1, 0))
												+ s2.substring(k7 + 2);
									} while (true);
									do {
										int l7 = s2.indexOf("%2");
										if (l7 == -1)
											break;
										s2 = s2.substring(0, l7) + interfaceIntToString(extractInterfaceValues(class9_1, 1))
												+ s2.substring(l7 + 2);
									} while (true);
									do {
										int i8 = s2.indexOf("%3");
										if (i8 == -1)
											break;
										s2 = s2.substring(0, i8) + interfaceIntToString(extractInterfaceValues(class9_1, 2))
												+ s2.substring(i8 + 2);
									} while (true);
									do {
										int j8 = s2.indexOf("%4");
										if (j8 == -1)
											break;
										s2 = s2.substring(0, j8) + interfaceIntToString(extractInterfaceValues(class9_1, 3))
												+ s2.substring(j8 + 2);
									} while (true);
									do {
										int k8 = s2.indexOf("%5");
										if (k8 == -1)
											break;
										s2 = s2.substring(0, k8) + interfaceIntToString(extractInterfaceValues(class9_1, 4))
												+ s2.substring(k8 + 2);
									} while (true);
								}
								int l11 = s2.indexOf("\\n");
								String s5;
								if (l11 != -1) {
									s5 = s2.substring(0, l11);
									s2 = s2.substring(l11 + 2);
								} else {
									s5 = s2;
									s2 = "";
								}
								if (class9_1.centerText) {
									textDrawingArea_2.method382(yPos, xPos + class9_1.width / 2, s5, j11, false);
								} else {
									if (s5.contains("\\r")) {
										String text = s5.substring(0, s5.indexOf("\\r"));
										String text2 = s5.substring(s5.indexOf("\\r") + 2);
										textDrawingArea_2.method389(false, xPos + 3, 0, text, j11);
										int rightX = boxWidth + xPos - textDrawingArea_2.getTextWidth(text2) - 2;
										textDrawingArea_2.method389(false, rightX, 0, text2, j11);
									} else
										textDrawingArea_2.method389(false, xPos + 3, 0, s5, j11);
								}
							}

						} else if (class9_1.type == RSInterface.TYPE_HOVER || class9_1.type == RSInterface.TYPE_CONFIG_HOVER) {
							// Draw sprite
							boolean flag = false;

							if (class9_1.toggled) {
								class9_1.sprite1.drawAdvancedTransparentSprite(_x, _y, class9_1.spriteOpacity);
								flag = true;
								class9_1.toggled = false;
							} else {
								class9_1.sprite2.drawAdvancedTransparentSprite(_x, _y, class9_1.spriteOpacity);
							}

							// Draw text
							if (class9_1.message == null) {
								continue;
							}
							if (class9_1.centerText) {
								newRegularFont.drawCenteredString(class9_1.message, _x + class9_1.msgX, _y + class9_1.msgY,
										flag ? class9_1.anInt216 : class9_1.textColor, 0x000000);

								// class9_1.rsFont.drawCenteredString(class9_1.message, _x + class9_1.msgX, _y +
								// class9_1.msgY,
								// flag ? class9_1.anInt216 : class9_1.textColor, 0);
							} else {
								newRegularFont.drawCenteredString(class9_1.message, _x + class9_1.msgX, _y + class9_1.msgY,
										flag ? class9_1.anInt216 : class9_1.textColor, 0x000000);
								// class9_1.rsFont.drawBasicString(class9_1.message, _x + 5, _y + class9_1.msgY,
								// flag ? class9_1.anInt216 : class9_1.textColor, 0);
							}
						} else if (class9_1.type == RSInterface.TYPE_CONFIG) {
							Sprite sprite = class9_1.active ? class9_1.sprite2 : class9_1.sprite1;
							sprite.drawSprite(_x, _y);
						} else if (class9_1.type == RSInterface.TYPE_SLIDER) {
							Slider slider = class9_1.slider;
							if (slider != null) {
								slider.draw(_x, _y, 255);
							}
						} else if (class9_1.type == RSInterface.TYPE_DROPDOWN) {

							DropdownMenu d = class9_1.dropdown;

							int bgColour = class9_1.dropdownColours[2];
							int fontColour = 0xfe971e;
							int downArrow = 30;

							if (class9_1.hovered || d.isOpen()) {
								downArrow = 31;
								fontColour = 0xffb83f;
								bgColour = class9_1.dropdownColours[3];
							}

							DrawingArea.drawPixels(20, _y, _x, class9_1.dropdownColours[0], d.getWidth());
							DrawingArea.drawPixels(18, _y + 1, _x + 1, class9_1.dropdownColours[1], d.getWidth() - 2);
							DrawingArea.drawPixels(16, _y + 2, _x + 2, bgColour, d.getWidth() - 4);

							int xOffset = class9_1.centerText ? 3 : 16;
							if (rsInterface.id == 41900) {
								newRegularFont.drawCenteredString(d.getSelected(), _x + (d.getWidth() - xOffset) / 2, _y + 14,
										fontColour, 0);
							} else {
								newSmallFont.drawCenteredString(d.getSelected(), _x + (d.getWidth() - xOffset) / 2, _y + 14,
										fontColour, 0);
							}

							if (d.isOpen()) {
								// Up arrow
								cacheSprite3[29].drawSprite(_x + d.getWidth() - 18, _y + 2);

								DrawingArea.drawPixels(d.getHeight(), _y + 19, _x, class9_1.dropdownColours[0], d.getWidth());
								DrawingArea.drawPixels(d.getHeight() - 2, _y + 20, _x + 1, class9_1.dropdownColours[1],
										d.getWidth() - 2);
								DrawingArea.drawPixels(d.getHeight() - 4, _y + 21, _x + 2, class9_1.dropdownColours[3],
										d.getWidth() - 4);

								int yy = 2;
								for (int i = 0; i < d.getOptions().length; i++) {
									if (class9_1.dropdownHover == i) {
										if (class9_1.id == 28102) {
											DrawingArea.drawAlphaBox(_x + 2, _y + 19 + yy, d.getWidth() - 4, 13, 0xd0914d, 80);
										} else {
											DrawingArea.drawPixels(13, _y + 19 + yy, _x + 2, class9_1.dropdownColours[4],
													d.getWidth() - 4);
										}
										if (rsInterface.id == 41900) {
											newRegularFont.drawCenteredString(d.getOptions()[i],
													_x + (d.getWidth() - xOffset) / 2, _y + 29 + yy, 0xffb83f, 0);
										} else {
											newSmallFont.drawCenteredString(d.getOptions()[i],
													_x + (d.getWidth() - xOffset) / 2, _y + 29 + yy, 0xffb83f, 0);
										}

									} else {
										DrawingArea.drawPixels(13, _y + 19 + yy, _x + 2, class9_1.dropdownColours[3],
												d.getWidth() - 4);
										if (rsInterface.id == 41900) {
											newRegularFont.drawCenteredString(d.getOptions()[i],
													_x + (d.getWidth() - xOffset) / 2, _y + 29 + yy, 0xfe971e, 0);
										} else {
											newSmallFont.drawCenteredString(d.getOptions()[i],
													_x + (d.getWidth() - xOffset) / 2, _y + 29 + yy, 0xfe971e, 0);
										}

									}
									yy += 14;
								}
								drawScrollbar(d.getHeight() - 4, class9_1.scrollPosition, _y + 21, _x + d.getWidth() - 18,
										d.getHeight() - 5);

							} else {
								cacheSprite3[downArrow].drawSprite(_x + d.getWidth() - 18, _y + 2);
							}
						} else if (class9_1.type == RSInterface.TYPE_KEYBINDS_DROPDOWN) {

							DropdownMenu d = class9_1.dropdown;

							// If dropdown inverted, don't draw following 2 menus
							if (dropdownInversionFlag > 0) {
								dropdownInversionFlag--;
								continue;
							}

							DrawingArea.drawPixels(18, _y + 1, _x + 1, 0x544834, d.getWidth() - 2);
							DrawingArea.drawPixels(16, _y + 2, _x + 2, 0x2e281d, d.getWidth() - 4);
							newRegularFont.drawBasicString(d.getSelected(), _x + 7, _y + 15, 0xff8a1f, 0);
							cacheSprite3[82].drawSprite(_x + d.getWidth() - 18, _y + 2); // Arrow TODO

							if (d.isOpen()) {

								RSInterface.interfaceCache[class9_1.id - 1].active = true; // Alter stone colour

								int yPos = _y + 18;

								// Dropdown inversion for lower stones
								if (class9_1.inverted) {
									yPos = _y - d.getHeight() - 10;
									dropdownInversionFlag = 2;
								}

								DrawingArea.drawPixels(d.getHeight() + 12, yPos, _x + 1, 0x544834, d.getWidth() - 2);
								DrawingArea.drawPixels(d.getHeight() + 10, yPos + 1, _x + 2, 0x2e281d, d.getWidth() - 4);

								int yy = 2;
								int xx = 0;
								int bb = d.getWidth() / 2;

								for (int i = 0; i < d.getOptions().length; i++) {

									int fontColour = 0xff981f;
									if (class9_1.dropdownHover == i) {
										fontColour = 0xffffff;
									}

									if (xx == 0) {
										newRegularFont.drawBasicString(d.getOptions()[i], _x + 5, yPos + 14 + yy, fontColour,
												0x2e281d);
										xx = 1;

									} else {
										newRegularFont.drawBasicString(d.getOptions()[i], _x + 5 + bb, yPos + 14 + yy,
												fontColour, 0x2e281d);
										xx = 0;
										yy += 15;
									}
								}
							} else {
								RSInterface.interfaceCache[class9_1.id - 1].active = false;
							}
						} else if (class9_1.type == RSInterface.TYPE_ADJUSTABLE_CONFIG) {
							if (class9_1.id != 37010) {
								DrawingArea.setDrawingArea(currentGameHeight, 0, currentGameWidth, 0);
							}
							if(class9_1.id == 37100) {
								if (_y < 41 || _y > 230) {
									return;
								}
							}
							DrawingArea.drawAlphaBox(_x, _y, class9_1.width, class9_1.height, class9_1.fillColor, class9_1.opacity);
							DrawingArea.setDrawingArea(yPosition + class9_1.height, xPosition, xPosition + class9_1.width, yPosition);
							/**
							 int totalWidth = class9_1.width;
							 int spriteWidth = class9_1.sprite2.myWidth;
							 int totalHeight = class9_1.height;
							 int spriteHeight = class9_1.sprite2.myHeight;
							 Sprite behindSprite = class9_1.active ? class9_1.enabledAltSprite : class9_1.disabledAltSprite;

							 if (class9_1.toggled) {
							 behindSprite.drawSprite(_x, _y);
							 class9_1.sprite2.drawSprite(_x + (totalWidth / 2) - spriteWidth / 2,
							 _y + (totalHeight / 2) - spriteHeight / 2, class9_1.spriteOpacity);
							 class9_1.toggled = false;
							 } else {
							 behindSprite.drawSprite(_x, _y);
							 class9_1.sprite2.drawSprite(_x + (totalWidth / 2) - spriteWidth / 2,
							 _y + (totalHeight / 2) - spriteHeight / 2);
							 }
							 **/
						} else if (class9_1.type == 16) {
							drawInputField(class9_1, _x, _y, class9_1.width, class9_1.height);
						}else if (class9_1.type == RSInterface.TYPE_BOX) {
							// Draw outline
							DrawingArea.drawBox(_x - 2, _y - 2, class9_1.width + 4, class9_1.height + 4, 0x0e0e0c);
							DrawingArea.drawBox(_x - 1, _y - 1, class9_1.width + 2, class9_1.height + 2, 0x474745);
							// Draw base box
							if (class9_1.toggled) {
								DrawingArea.drawBox(_x, _y, class9_1.width, class9_1.height, class9_1.anInt239);
								class9_1.toggled = false;
							} else {
								DrawingArea.drawBox(_x, _y, class9_1.width, class9_1.height, class9_1.hoverTextColor);
							}
						} else if (class9_1.type == 19) {
							if (class9_1.backgroundSprites.length > 1) {
								if (class9_1.sprite1 != null) {
									class9_1.sprite1.drawAdvancedSprite(_x, _y);
								}
							}
						} else if (class9_1.type == RSInterface.TYPE_STRING_CONTAINER) {
							int x = _x;
							int y = _y;

							// Set the scroll max based on the strings
							if (class9_1.scrollableContainerInterfaceId != 0) {
								RSInterface container = RSInterface.get(class9_1.scrollableContainerInterfaceId);
								int scrollMax = class9_1.invAutoScrollHeightOffset;
								for (String string : class9_1.stringContainer)
									scrollMax += class9_1.invSpritePadY;
								if (scrollMax > container.height + 1) {
									container.scrollMax = scrollMax;
								} else {
									container.scrollMax = container.height + 1;
								}

								container.scrollMax += container.stringContainerContainerExtraScroll;
							}

							// Draw the container
							for (String string : class9_1.stringContainer) {
								if (class9_1.centerText) {
									class9_1.font.drawCenteredString(string, x, y, class9_1.textColor, class9_1.textShadow ? 0 : -1);
								} else {
									class9_1.font.drawBasicString(string, x, y, class9_1.textColor, class9_1.textShadow ? 0 : -1);
								}
								y += class9_1.invSpritePadY;
							}
						} else if (class9_1.type == RSInterface.TYPE_HORIZONTAL_STRING_CONTAINER) {
							int x = _x;
							int y = _y;

							for (int i = 0; i < class9_1.stringContainer.size(); i++) {
								String string = class9_1.stringContainer.get(i);
								if (class9_1.centerText) {
									class9_1.font.drawCenteredString(string, x, y, class9_1.textColor, class9_1.textShadow ? 0 : -1);
								} else {
									class9_1.font.drawBasicString(string, x, y, class9_1.textColor, class9_1.textShadow ? 0 : -1);
								}
								x += 32 + class9_1.invSpritePadX;
								if ((i + 1) % class9_1.width == 0) {
									y += 32 + class9_1.invSpritePadY;
									x = _x;
								}
							}
						} else if (class9_1.type == RSInterface.TYPE_PROGRESS_BAR) {
							DrawingArea.drawPixels(class9_1.height, _y, _x, class9_1.fillColor, class9_1.width);
						} else if (class9_1.type == RSInterface.TYPE_PROGRESS_BAR_2021) {
							double percentage = class9_1.progressBar2021Percentage;
							int color = RSInterface.getRgbProgressColor(percentage);

							int progressBarWidth = (int) ((double) class9_1.width * percentage);
							DrawingArea.drawPixels(class9_1.height, _y, _x, color, progressBarWidth);
							DrawingArea.drawBorder(_x, _y, class9_1.width, class9_1.height, class9_1.fillColor);
						} else if (class9_1.type == RSInterface.TYPE_DRAW_BOX) {
							//DrawingArea.drawRoundedRectangle(_x, _y, class9_1.width, class9_1.height, class9_1.fillColor, class9_1.transparency, true, true);

							DrawingArea.drawTransparentBox(_x, _y, class9_1.width, class9_1.height, class9_1.fillColor, class9_1.transparency);
							DrawingArea.drawBorder(_x, _y, class9_1.width, class9_1.height, class9_1.borderColor);
						}

					if (interfaceText) {
						newSmallFont.drawString(class9_1.id + "", _x - 12, _y, 0xFFFFFFFF, 0, 256);
					}
				} catch (Exception | StackOverflowError e) {
					System.err.println(String.format("Error on interface child, parentId=%s, childIndex=%s, childId=%s", rsInterface.id, childId, rsInterface.children[childId]));
					e.printStackTrace();
				}
			}

			Bank.drawOnBank(rsInterface, 0, 0);

			if (!inheritDrawingArea) {
				DrawingArea.setDrawingArea(clipBottom, clipLeft, clipRight, clipTop);
			}
			if (rsInterface.id == 42000) {
				cacheSprite2[76].flashSprite(24, 280, 200 + (int) (50 * Math.sin(update_tick / 15.0)));
			}
			if (rsInterface.id == 16244) {
				if (super.getMouseX() > 165 && super.getMouseX() < 610 && super.getMouseY() > 428 && super.getMouseY() < 470) {
					DrawingArea.drawAlphaBox(165, 428, 444, 42, 0xffffff, 40);
				}
				// cacheSprite2[76].drawSprite1(24, 280,
				// 200 + (int) (50 * Math.sin(tick / 15.0)));
				// DrawingArea.drawBox(30, 400, 400, 400, 0xffffff);
			}
			if (tabID == 0) {
				// TODO blue spec bar
				// DrawingArea.drawBox(53, 250, 148, 24, 0xffffff);
			}
		} catch (Exception | StackOverflowError e) {
			System.err.println(String.format("Error on interface j=%d, xPosition=%d, id=%d, yPosition=%d", scrollPosition, xPosition, rsInterface.id, yPosition));
			e.printStackTrace();
		}
	}

	/**
	 * int state -1 for buy and sell buttons, 0 for buying, 1 for selling, 2 for
	 * canceled
	 * <p>
	 * int itemId The item that the player wants to buy
	 * <p>
	 * int currentlyBought/Sold How many items the player has bought/sold so far.
	 * <p>
	 * int totalAmount How many items the player wants to buy/sell in total.
	 */
	private int[][] grandExchangeInformation = new int[][] { { -1, -1, -1, -1 }, { -1, -1, -1, -1 }, { -1, -1, -1, -1 },
			{ -1, -1, -1, -1 }, { -1, -1, -1, -1 }, { -1, -1, -1, -1 }, { -1, -1, -1, -1 }, { -1, -1, -1, -1 },
			{ -1, -1, -1, -1 },
			// { 0, 11802, 0, 10 },
			// { 0, 11804, 4, 14 },
			// { 2, 11826, 2, 2 },
			// { 1, 555, 47500, 52500 },
			// { 0, 560, 35000, 50000 },
			// { 1, 11847, 0, 1 },
			// { -1, -1, -1, -1 }
	};

	public final String methodR(int j) {
		if (j >= 0 && j < 10000)
			return String.valueOf(j);
		if (j >= 10000 && j < 10000000)
			return j / 1000 + "K";
		if (j >= 10000000 && j < 999999999)
			return j / 1000000 + "M";
		if (j >= 999999999)
			return "*";
		else
			return "?";
	}

	public void drawHoverBox(int xPos, int yPos, String text) {
		String[] results = text.split("\n");
		int height = (results.length * 16) + 3;
		int width;
		width = aTextDrawingArea_1271.getTextWidth(results[0]) + 6;
		for (int i = 1; i < results.length; i++)
			if (width <= aTextDrawingArea_1271.getTextWidth(results[i]) + 6)
				width = aTextDrawingArea_1271.getTextWidth(results[i]) + 6;
		DrawingArea.drawPixels(height, yPos, xPos, 0xFFFFA0, width);
		DrawingArea.fillPixels(xPos, width, height, 0, yPos);
		yPos += 14;
		for (int i = 0; i < results.length; i++) {
			aTextDrawingArea_1271.method389(false, xPos + 3, 0, results[i], yPos);
			yPos += 16;
		}
	}

	public void drawBlackBox(int xPos, int yPos) {
		DrawingArea.drawPixels(71, yPos - 1, xPos - 2, 0x726451, 1);
		DrawingArea.drawPixels(69, yPos, xPos + 174, 0x726451, 1);
		DrawingArea.drawPixels(1, yPos - 2, xPos - 2, 0x726451, 178);
		DrawingArea.drawPixels(1, yPos + 68, xPos, 0x726451, 174);
		DrawingArea.drawPixels(71, yPos - 1, xPos - 1, 0x2E2B23, 1);
		DrawingArea.drawPixels(71, yPos - 1, xPos + 175, 0x2E2B23, 1);
		DrawingArea.drawPixels(1, yPos - 1, xPos, 0x2E2B23, 175);
		DrawingArea.drawPixels(1, yPos + 69, xPos, 0x2E2B23, 175);
		DrawingArea.method335(0, yPos, 174, 68, 220, xPos);
	}

	public void loadTabArea() {
		if(getUserSettings().isOldGameframe() == false) {
			for (int i = 0; i < redStones.length; i++)
				redStones[i] = new Sprite("Gameframe/redstones/redstone" + i);

			for (int i = 0; i < sideIcons.length; i++)
				sideIcons[i] = new Sprite("Gameframe/sideicons/sideicon" + i);

			mapArea[0] = new Sprite("Gameframe/fixed/mapArea");
			mapArea[1] = new Sprite("Gameframe/fixed/mapBorder");
			mapArea[2] = new Sprite("Gameframe/resizable/mapArea");
			mapArea[3] = new Sprite("Gameframe/fixed/blackMapArea");
			mapArea[4] = new Sprite("Gameframe/resizable/mapBlack");
			mapArea[5] = new Sprite("Gameframe/fixed/topframe");
			mapArea[6] = new Sprite("Gameframe/fixed/chatborder");
			mapArea[7] = new Sprite("Gameframe/fixed/frame");

			tabAreaFixed = new Sprite("Gameframe/fixed/tabArea");
			compassImage = new Sprite("Gameframe/compassImage");
		}else {
			for (int i = 0; i < redStones.length; i++)
				redStones[i] = new Sprite("Gameframe317/redstones/redstone" + i);

			for (int i = 0; i < sideIcons.length; i++)
				sideIcons[i] = new Sprite("Gameframe317/sideicons/sideicon" + i);

			mapArea[0] = new Sprite("Gameframe/fixed/mapArea");
			mapArea[1] = new Sprite("Gameframe317/fixed/mapBorder");
			mapArea[2] = new Sprite("Gameframe317/resizable/mapArea");
			mapArea[3] = new Sprite("Gameframe317/fixed/blackMapArea");
			mapArea[4] = new Sprite("Gameframe317/resizable/mapBlack");
			mapArea[5] = new Sprite("Gameframe/fixed/topframe");
			mapArea[6] = new Sprite("Gameframe/fixed/chatborder");
			mapArea[7] = new Sprite("Gameframe/fixed/frame");

			tabAreaFixed = new Sprite("Gameframe317/fixed/tabArea");
			compassImage = new Sprite("Gameframe317/compassImage");
		}
	}
	public void randomizeBackground(Background background) {
		int j = 256;
		for (int k = 0; k < anIntArray1190.length; k++)
			anIntArray1190[k] = 0;

		for (int l = 0; l < 5000; l++) {
			int i1 = (int) (Math.random() * 128D * j);
			anIntArray1190[i1] = (int) (Math.random() * 256D);
		}
		for (int j1 = 0; j1 < 20; j1++) {
			for (int k1 = 1; k1 < j - 1; k1++) {
				for (int i2 = 1; i2 < 127; i2++) {
					int k2 = i2 + (k1 << 7);
					anIntArray1191[k2] = (anIntArray1190[k2 - 1] + anIntArray1190[k2 + 1] + anIntArray1190[k2 - 128]
							+ anIntArray1190[k2 + 128]) / 4;
				}

			}
			int ai[] = anIntArray1190;
			anIntArray1190 = anIntArray1191;
			anIntArray1191 = ai;
		}
		if (background != null) {
			int l1 = 0;
			for (int j2 = 0; j2 < background.anInt1453; j2++) {
				for (int l2 = 0; l2 < background.width; l2++)
					if (background.palettePixels[l1++] != 0) {
						int i3 = l2 + 16 + background.anInt1454;
						int j3 = j2 + 16 + background.anInt1455;
						int k3 = i3 + (j3 << 7);
						anIntArray1190[k3] = 0;
					}
			}
		}
	}

	private void method107(int i, int j, Buffer stream, Player player) {
		if ((i & 0x400) != 0) {
			player.exactmove_x1 = stream.method428();
			player.exactmove_y1 = stream.method428();
			player.anInt1544 = stream.method428();
			player.anInt1546 = stream.method428();
			player.exactmove_start = stream.method436() + update_tick;
			player.exactmove_end = stream.readUShortA() + update_tick;
			player.forceMovementDirection = stream.method428();
			player.method446();
		}
		if ((i & 0x100) != 0) {
			player.spotanim = stream.method434();
			int k = stream.readDWord();
			player.spotanim_height = k >> 16;
			player.spotanim_start_loop = update_tick + (k & 0xffff);
			player.spotanimframe_index = 0;
			player.spotanim_loop = 0;
			if (player.spotanim_start_loop > update_tick)
				player.spotanimframe_index = -1;
			if (player.spotanim == 65535)
				player.spotanim = -1;
		}
		if ((i & 8) != 0) {
			int l = stream.method434();
			if (l == 65535)
				l = -1;
			int i2 = stream.method427();
			if (l == player.primaryanim && l != -1) {
				int i3 = AnimationDefinition.anims[l].looptype;
				if (i3 == 1) {
					player.primaryanim_frameindex = 0;
					player.primaryanim_loops_remaining = 0;
					player.primaryanim_pause = i2;
					player.primaryanim_replaycount = 0;
				}
				if (i3 == 2)
					player.primaryanim_replaycount = 0;
			} else if (l == -1 || player.primaryanim == -1
					|| AnimationDefinition.anims[l].anInt359 >= AnimationDefinition.anims[player.primaryanim].anInt359) {
				player.primaryanim = l;
				player.primaryanim_frameindex = 0;
				player.primaryanim_loops_remaining = 0;
				player.primaryanim_pause = i2;
				player.primaryanim_replaycount = 0;
				player.anim_delay = player.waypoint_count;
			}
		}
		if ((i & 4) != 0) {
			String textSpoken = stream.readString();

			// Only show flower poker chat to people within the area
			if (!player.inFlowerPokerArea() || myPlayer.inFlowerPokerChatProximity()) {
				player.textSpoken = textSpoken;
				if (player.textSpoken.charAt(0) == '~') {
					player.textSpoken = player.textSpoken.substring(1);
					if (player.lastForceChat == null || !player.lastForceChat.equals(textSpoken))
						pushMessage(player.textSpoken, 2, player.displayName);
				} else if (player == myPlayer) {
					if (player.lastForceChat == null || !player.lastForceChat.equals(textSpoken))
						pushMessage(player.textSpoken, 2, player.displayName);
				}
				player.anInt1513 = 0;
				player.anInt1531 = 0;
				player.textCycle = 150;
				player.lastForceChat = textSpoken;
			}
		}
		if ((i & 0x80) != 0) {
			int i1 = stream.method434();
			int rightsPrimaryValue = stream.readUnsignedByte();
			int j3 = stream.method427();
			int k3 = stream.currentOffset;
			PlayerRights rights = PlayerRights.forRightsValue(rightsPrimaryValue);
			if (player.displayName != null && player.visible) {
				long l3 = StringUtils.longForName(player.displayName);
				boolean flag = false;
				if (!rights.isStaffPosition()) {
					for (int i4 = 0; i4 < ignoreCount; i4++) {
						if (ignoreListAsLongs[i4] != l3)
							continue;
						flag = true;
						break;
					}
				}
				if (!flag && anInt1251 == 0)
					try {
						aStream_834.currentOffset = 0;
						stream.method442(j3, 0, aStream_834.buffer);
						aStream_834.currentOffset = 0;
						String s = TextInput.method525(j3, aStream_834);

						// Only show flower poker chat to people within the area
						if (!player.inFlowerPokerArea() || myPlayer.inFlowerPokerChatProximity()) {
							player.textSpoken = s;
							player.anInt1513 = i1 >> 8;
							player.privelage = rightsPrimaryValue; // TODO remove privilege sending in send text player update flag
							player.anInt1531 = i1 & 0xff;
							player.textCycle = 150;
							String crown = PlayerRights.buildCrownString(player.getDisplayedRights());
							String title = player.title != null && !player.title.isEmpty()
									? "<col=" + player.titleColor + ">" + player.title + "</col> "
									: "";
							pushMessage(s, 2, crown + title + player.displayName);
						}
					} catch (Exception exception) {
						exception.printStackTrace();
						Signlink.reporterror("cde2");
					}
			}
			stream.currentOffset = k3 + j3;
		}
		if ((i & 1) != 0) {
			player.interactingEntity = stream.method434();
			if (player.interactingEntity == 65535)
				player.interactingEntity = -1;
		}
		if ((i & 0x10) != 0) {
			int j1 = stream.method427();
			byte abyte0[] = new byte[j1];
			Buffer stream_1 = new Buffer(abyte0);
			stream.readBytes(j1, 0, abyte0);
			aStreamArray895s[j] = stream_1;
			player.updatePlayer(stream_1);
		}
		if ((i & 2) != 0) {
			player.face_x = stream.method436();
			player.face_y = stream.method434();
		}
		if ((i & 0x20) != 0) {
			int k1 = stream.readUnsignedByte();
			int k2 = stream.method426();
			player.updateHitData(k2, k1, update_tick);
			player.loopCycleStatus = update_tick + 300;
			player.currentHealth = stream.method427();
			player.maxHealth = stream.readUnsignedByte();
		}
		if ((i & 0x200) != 0) {
			int l1 = stream.readUnsignedByte();
			int l2 = stream.method428();
			player.updateHitData(l2, l1, update_tick);
			player.loopCycleStatus = update_tick + 300;
			player.currentHealth = stream.readUnsignedByte();
			player.maxHealth = stream.method427();
		}
	}

	public void method108() {
		try {
			int j = myPlayer.x + cameraOffsetX;
			int k = myPlayer.y + cameraOffsetY;
			if (anInt1014 - j < -500 || anInt1014 - j > 500 || anInt1015 - k < -500 || anInt1015 - k > 500) {
				anInt1014 = j;
				anInt1015 = k;
			}
			if (anInt1014 != j)
				anInt1014 += (j - anInt1014) / 16;
			if (anInt1015 != k)
				anInt1015 += (k - anInt1015) / 16;
			if (super.keyArray[1] == 1)
				anInt1186 += (-24 - anInt1186) / 2;
			else if (super.keyArray[2] == 1)
				anInt1186 += (24 - anInt1186) / 2;
			else
				anInt1186 /= 2;
			if (super.keyArray[3] == 1)
				anInt1187 += (12 - anInt1187) / 2;
			else if (super.keyArray[4] == 1)
				anInt1187 += (-12 - anInt1187) / 2;
			else
				anInt1187 /= 2;
			viewRotation = viewRotation + anInt1186 / 2 & 0x7ff;
			camAngleX += anInt1187 / 2;
			if (camAngleX < 128)
				camAngleX = 128;
			if (camAngleX > 383)
				camAngleX = 383;
			int l = anInt1014 >> 7;
			int i1 = anInt1015 >> 7;
			int j1 = getCenterHeight(plane, anInt1015, anInt1014);
			int k1 = 0;
			if (l > 3 && i1 > 3 && l < 100 && i1 < 100) {
				for (int l1 = l - 4; l1 <= l + 4; l1++) {
					for (int k2 = i1 - 4; k2 <= i1 + 4; k2++) {
						int l2 = plane;
						if (l2 < 3 && (byteGroundArray[1][l1][k2] & 2) == 2)
							l2++;
						int i3 = j1 - intGroundArray[l2][l1][k2];
						if (i3 > k1)
							k1 = i3;
					}

				}

			}
			anInt1005++;
			if (anInt1005 > 1512) {
				anInt1005 = 0;
				stream.createFrame(77);
				stream.writeUnsignedByte(0);
				int i2 = stream.currentOffset;
				stream.writeUnsignedByte((int) (Math.random() * 256D));
				stream.writeUnsignedByte(101);
				stream.writeUnsignedByte(233);
				stream.writeWord(45092);
				if ((int) (Math.random() * 2D) == 0)
					stream.writeWord(35784);
				stream.writeUnsignedByte((int) (Math.random() * 256D));
				stream.writeUnsignedByte(64);
				stream.writeUnsignedByte(38);
				stream.writeWord((int) (Math.random() * 65536D));
				stream.writeWord((int) (Math.random() * 65536D));
				stream.writeBytes(stream.currentOffset - i2);
			}
			int j2 = k1 * 192;
			if (j2 > 0x17f00)
				j2 = 0x17f00;
			if (j2 < 32768)
				j2 = 32768;
			if (j2 > field556) {
				field556 += (j2 - field556) / 24;
				return;
			}
			if (j2 < field556) {
				field556 += (j2 - field556) / 80;
			}
		} catch (Exception _ex) {
			Signlink.reporterror("glfc_ex " + myPlayer.x + "," + myPlayer.y + "," + anInt1014 + "," + anInt1015 + ","
					+ currentRegionX + "," + currentRegionY + "," + baseX + "," + baseY);
			throw new RuntimeException("eek");
		}
	}

	@Override
	public void processDrawing() {
		if (rsAlreadyLoaded || loadingError || genericLoadingError) {
			showErrorScreen();
			return;
		}
		anInt1061++;
		if (!loggedIn) {
			drawLoginScreen(false);
		} else {
			if (loginScreenGraphicsBuffer == null) {
				drawGameScreen();
			}
		}
		mouseClickCount = 0;
	}

	private boolean isFriendOrSelf(String s) {
		if (s == null)
			return false;
		for (int i = 0; i < friendsCount; i++)
			if (s.equalsIgnoreCase(friendsList[i]))
				return true;
		return s.equalsIgnoreCase(myPlayer.displayName);
	}

	private static String combatDiffColor(int i, int j) {
		int k = i - j;
		if (k < -9)
			return "@red@";
		if (k < -6)
			return "@or3@";
		if (k < -3)
			return "@or2@";
		if (k < 0)
			return "@or1@";
		if (k > 9)
			return "@gre@";
		if (k > 6)
			return "@gr3@";
		if (k > 3)
			return "@gr2@";
		if (k > 0)
			return "@gr1@";
		else
			return "@yel@";
	}

	public void setWaveVolume(int i) {
		Signlink.wavevol = i;
	}

	public static boolean centerInterface() {
		int minimumScreenWidth = 765, minimumScreenHeight = 610;
		if (currentGameWidth >= minimumScreenWidth && currentGameHeight >= minimumScreenHeight)
			return true;
		return false;
	}


	public static boolean goodDistance(int objectX, int objectY, int playerX, int playerY, int distance) {
		return ((objectX - playerX <= distance && objectX - playerX >= -distance)
				&& (objectY - playerY <= distance && objectY - playerY >= -distance));
	}

	public static int distanceToPoint(int x1, int y1, int x2, int y2) {
		int x = (int) Math.pow(x1 - x2, 2.0D);
		int y = (int) Math.pow(y1 - y2, 2.0D);
		return (int) Math.floor(Math.sqrt(x + y));
	}

	private void displayGroundItems() {
		/**
		 * Loop thru all tiles in region
		 */

		for (int x = 0; x < 104; x++) {
			for (int y = 0; y < 104; y++) {
				NodeList class19 = groundItems[plane][x][y];
				int count = 0;
				if (class19 != null) {
					Map<Integer, Long> amounts = new HashMap<>();
					HashSet<Integer> drawn = new HashSet<>();

					for (Item item = (Item) class19.getFirst(); item != null; item = (Item) class19.getNext()) {
						amounts.put(item.ID, item.anInt1559 + amounts.getOrDefault(item.ID, 0L));
					}

					for (Item item = (Item) class19.getFirst(); item != null; item = (Item) class19.getNext()) {
						if (drawn.contains(item.ID)) {
							continue;
						}

						ItemDefinition itemDef = ItemDefinition.forID(item.ID);
						long amount = amounts.get(item.ID);
						long value = (long) itemDef.value * amount;
						drawn.add(item.ID);

						String valueText = Preferences.getPreferences().groundItemTextShowMoreThan;
						int showValue = 0;
						try {
							showValue = valueText.length() == 0 ? 0 : Integer.parseInt(valueText);
						} catch (NumberFormatException e) {
							Preferences.getPreferences().groundItemTextShowMoreThan = "";
							if (Configuration.developerMode) {
								e.printStackTrace();
							}
						}

						boolean rareDropColor = value > 100_000;

						if (itemDef.searchable || !Preferences.getPreferences().groundItemAlwaysShowUntradables) {
							String[] hide = Preferences.getPreferences().groundItemTextHide.split(",");
							String[] show = Preferences.getPreferences().groundItemTextShow.split(",");
							String itemNameFormatted = itemDef.name.toLowerCase();

							boolean showItem = Arrays.stream(show).anyMatch(showTxt -> showTxt.length() > 0 && itemNameFormatted.contains(showTxt.trim()));
							boolean hideItem = Arrays.stream(hide).anyMatch(hideTxt -> hideTxt.length() > 0 && itemNameFormatted.contains(hideTxt.trim()));

							if (!showItem && (hideItem || value < showValue)) {
								continue;
							}
						} else {
							rareDropColor = true;
						}

						calcEntityScreenPos((x * 128 + 64), 25, (y * 128 + 64));

						int itemX = baseX + x;
						int itemY = baseY + y;
						int playerX = baseX + (myPlayer.x - 6 >> 7);
						int playerY = baseY + (myPlayer.y - 6 >> 7);

						/**
						 * Fading
						 */
						int transparencyDistance = distanceToPoint(playerX, playerY, itemX, itemY);
						int transparency = 256 - (transparencyDistance * 16);

						if (goodDistance(itemX, itemY, playerX, playerY, 16)) {
							int yMod = (count * 12);

							StringBuilder builder = new StringBuilder();
							builder.append(itemDef.name);
							if (amount > 1) {
								builder.append(" (" + (int) amount + ")");
							}
							if (value > 1 && itemDef.searchable) {
								builder.append(" ("+ StringUtils.insertCommas(value) + " gp)");
							}

							latoBold.drawCenteredString(builder.toString(), spriteDrawX, spriteDrawY - yMod, rareDropColor ? 0xDA6EA2 : 0xffffff, 0x00000, transparency);
							count++;
						}
					}
				}
			}
		}


	}

	private void draw3dScreen() {
		if (currentScreenMode == ScreenMode.FIXED) {
			mapArea[1].drawSprite(516, 0);
			mapArea[7].drawSprite(0, 0);
		}
		if (snowVisible && Configuration.CHRISTMAS && Client.currentScreenMode == ScreenMode.FIXED) {
			method119(tickDelta, 11877);
			drawInterface(0, 0, RSInterface.interfaceCache[11877], 0);
		}

		if (getUserSettings().isShowEntityTarget()) {
			if (entityTarget != null) {
				entityTarget.draw();
			}
		}

		if (getUserSettings().isGameTimers()) {
			try {
				int startX = 516;
				int startY = Client.currentScreenMode == ScreenMode.FIXED ? 294 : Client.currentGameHeight - 209;
				GameTimerHandler.getSingleton().drawGameTimers(this, startX, startY);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		drawSplitPrivateChat();

		if (crossType == 1) {
			crosses[crossIndex / 100].drawSprite(crossX - 8, crossY - 8);
			anInt1142++;
			if (anInt1142 > 67) {
				anInt1142 = 0;
				stream.createFrame(78);
			}
		}
		if (crossType == 2) {
			crosses[4 + crossIndex / 100].drawSprite(crossX - 8, crossY - 8);
		}

		if (openWalkableWidgetID != -1) {
			method119(tickDelta, openWalkableWidgetID);
			if (!Nightmare.instance.drawNightmareInterfaces(openWalkableWidgetID)) {
				RSInterface rsinterface = RSInterface.interfaceCache[openWalkableWidgetID];
				if (currentScreenMode == ScreenMode.FIXED) {
					drawInterface(0, 0, rsinterface, 0);
				} else {
					if (openWalkableWidgetID == 28000 || openWalkableWidgetID == 28020 || openWalkableWidgetID == 16210
							|| openWalkableWidgetID == 27500 || openWalkableWidgetID == 196 || openWalkableWidgetID == 27553) {
						/**
						 * Interfaces to draw at the top right corner nex to the minimap (Ex. Wildy
						 * Target)
						 **/
						drawInterface(0, currentGameWidth - 730, rsinterface, 20);
					} else if (openWalkableWidgetID == 197) {
						drawInterface(0, currentGameWidth - 530, rsinterface, -70);
					} else if (openWalkableWidgetID == 27553) {
					} else if (openWalkableWidgetID == 21100 || openWalkableWidgetID == 21119
							|| openWalkableWidgetID == 29230) {
						/**
						 * Interfaces to draw at top left corner (Ex. Pest Control)
						 **/
						drawInterface(0, 0, rsinterface, 0);
					} else if (openWalkableWidgetID == 201) {
						/** Duel arena interface **/
						drawInterface(0, currentGameWidth - 510, rsinterface, -110);
					} else if (centerInterface() || openInterfaceID == 29230) {
						drawInterface(0, (currentGameWidth / 2) - 356, rsinterface,
								currentScreenMode == ScreenMode.FIXED ? 0 : (currentGameHeight / 2) - 230);
					} else {
						if (currentGameWidth >= 900 && currentGameHeight >= 650) {
							drawInterface(0, (currentGameWidth / 2) - 356, RSInterface.interfaceCache[openWalkableWidgetID], currentScreenMode == ScreenMode.FIXED ? 0 : (currentGameHeight / 2) - 230);
						} else {
							drawInterface(0, 0, RSInterface.interfaceCache[openWalkableWidgetID], 0);
						}
					}
				}
			}
		}

		BroadcastManager.display(this);

		if (anInt1055 == 1) {
			int x = currentScreenMode == ScreenMode.FIXED ? 476 : (currentGameWidth - 30);
			int y = currentScreenMode == ScreenMode.FIXED ? 300 : 175;
			if (currentScreenMode == ScreenMode.FIXED && GameTimerHandler.getSingleton().hasActiveGameTimers()) {
				y -= 32;
			}
			multiOverlay.drawSprite(x, y);
		}

		if (fpsOn) {
			// char c = '\u01FB';
			int yPosition = 30;
			int xPosition = currentScreenMode == ScreenMode.FIXED ? 515 : currentGameWidth - 222;
			int textColor = 0xffff00;
			if (super.getFps() < 15)
				textColor = 0xff0000;
			aTextDrawingArea_1271.method380("Fps: " + super.getFps(), xPosition, textColor, yPosition);
			yPosition += 15;
			Runtime runtime = Runtime.getRuntime();
			int j1 = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
			textColor = 0xffff00;
			if (j1 > 0x2000000 && lowMem)
				textColor = 0xff0000;
			aTextDrawingArea_1271.method380("Mem: " + j1 / 1000 + " mb", xPosition, textColor, yPosition);
			yPosition += 15;
		}

		int x = baseX + (myPlayer.x - 6 >> 7);
		int y = baseY + (myPlayer.y - 6 >> 7);
		int var131 = x >> 6;
		int var12 = y >> 6;
		int chunkX = x >> 3;
		int var141 = y >> 3;
		int regionid = var131 * 256 + var12;
		int mapx = currentRegionX; // map region x
		int mapy = currentRegionY; // map region y
		if (clientData) {
			Runtime runtime = Runtime.getRuntime();
			int j1 = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L);
			aTextDrawingArea_1271.method385(0x00FF00, "Players Nearby: " + playerCount, 27, 5);
			aTextDrawingArea_1271.method385(0x00FF00, "Npcs Nearby: " + npcCount, 41, 5);

			if (mapx > 1000 || mapy > 1000) {
				aTextDrawingArea_1271.method385(0xffff00, "Current Region: " + mapx + ", " + mapy + ", Region ID: " + regionid, 55, 5);
			} else {
				aTextDrawingArea_1271.method385(0xffff00, "Current Region: 0" + mapx + ", 0" + mapy + ", Region ID: " + regionid, 55, 5);
			}
			for (int num = 0; num < terrainIndices.length; num++) {
				int[] flo = terrainIndices;
				aTextDrawingArea_1271.method385(0xffff00, "Floor map: " + Arrays.toString(flo), 69, 5);
			}
			for (int num = 0; num < objectIndices.length; num++) {
				int[] obj = objectIndices;
				aTextDrawingArea_1271.method385(0xffff00, "Object map: " + Arrays.toString(obj), 83, 5);
				// output: "Object map: "[1, 3, 5, 7, 9]"
			}

			aTextDrawingArea_1271.method385(0xffff00, "Map Data: " + terrainIndices[0] + ".dat", 97, 5);
			aTextDrawingArea_1271.method385(0xffff00, "Fps: " + super.getFps(), 111, 5);
			aTextDrawingArea_1271.method385(0xffff00, "Memory Used: " + j1/1024 + "MB", 125, 5);
			aTextDrawingArea_1271.method385(0xffff00,
					"Mouse: [" + super.getMouseX() + ", " + super.getMouseY() + "] [" + super.getRawMouseX() + ", " + super.getRawMouseY() + "]", 139, 5);
			aTextDrawingArea_1271.method385(0xffff00, "Coordinates: X: " + x + ", Y: " + y, 153, 5);

			aTextDrawingArea_1271.method385(0xffff00,
					"Camera Position: X: " + xCameraPos + ", Y: " + yCameraPos + ", Z: " + zCameraPos, 167, 5);
			aTextDrawingArea_1271.method385(0xffff00, "Camera Curve: X: " + xCameraCurve + ", Y: " + yCameraCurve, 181,
					5);
			y = 181;
			y += 15;
			aTextDrawingArea_1271.method385(0xffff00, "FOV: " + WorldController.viewDistance, y,5);
		}

		if (openInterfaceID != -1) {
			method119(tickDelta, openInterfaceID);
			RSInterface rsinterface = RSInterface.interfaceCache[openInterfaceID];
			if (currentScreenMode == ScreenMode.FIXED)
				drawInterface(0, 0, rsinterface, 0);
			else
				drawInterface(0, (currentGameWidth / 2) - 356, rsinterface, currentScreenMode == ScreenMode.FIXED ? 0 : (currentGameHeight / 2) - 230);
		}
		method70();

		if (!menuOpen) {
			processRightClick();
			drawTopLeftTooltip();
		} else if (menuScreenArea == 0) {
			drawMenu(currentScreenMode == ScreenMode.FIXED ? 0 : 0, currentScreenMode == ScreenMode.FIXED ? 0 : 0);
		}

		if (drawGrid) {
			drawGrid();
		}


		if (anInt1104 != 0) {
			int j = anInt1104 / 50;
			int l = j / 60;
			j %= 60;
			int yPosition = currentScreenMode == ScreenMode.FIXED ? 329 : currentGameHeight - 165;
			if (j < 10)
				aTextDrawingArea_1271.method385(0xffff00, "system update in:  " + l + ":0" + j, yPosition, 5);
			else
				aTextDrawingArea_1271.method385(0xffff00, "system update in:  " + l + ":" + j, yPosition, 5);

			anInt849++;
			if (anInt849 > 75) {
				anInt849 = 0;
				stream.createFrame(148);
			}
		}

		drawScreenBox();
		devConsole.draw_console();
	}


	private void addIgnore(long l) {
		try {
			if (l == 0L)
				return;

			String username = StringUtils.fixName(StringUtils.nameForLong(l));

			if (username.equalsIgnoreCase(myPlayer.displayName))
				return;

			if (ignoreCount >= 100) {
				pushMessage("Your ignore list is full. Max of 100 hit", 0, "");
				return;
			}
			for (int j = 0; j < ignoreCount; j++)
				if (ignoreListAsLongs[j] == l) {
					pushMessage(username + " is already on your ignore list", 0, "");
					return;
				}
			for (int k = 0; k < friendsCount; k++)
				if (friendsListAsLongs[k] == l) {
					pushMessage("Please remove " + username + " from your friend list first", 0, "");
					return;
				}

			//ignoreListAsLongs[ignoreCount++] = l;
			//needDrawTabArea = true;
			stream.createFrame(133);
			stream.writeQWord(l);
			return;
		} catch (RuntimeException runtimeexception) {
			Signlink.reporterror("45688, " + l + ", " + 4 + ", " + runtimeexception.toString());
		}
		throw new RuntimeException();
	}

	public void method114() {
		for (int i = -1; i < playerCount; i++) {
			int j;
			if (i == -1)
				j = maxPlayerCount;
			else
				j = playerIndices[i];
			Player player = players[j];
			if (player != null)
				method96(player);
		}

	}

	private void method115() {
		if (loadingStage == 2) {
			boolean passedRequest = false;
			for (SpawnedObject spawnedObject = (SpawnedObject) spawns
					.reverseGetFirst(); spawnedObject != null; spawnedObject = (SpawnedObject) spawns
					.reverseGetNext()) {
				if (spawnedObject.getLongetivity > 0)
					spawnedObject.getLongetivity--;
				if (spawnedObject.getLongetivity == 0) {
					if (spawnedObject.getPreviousId < 0
							|| ObjectManager.method178(spawnedObject.getPreviousId, spawnedObject.previousType)) {
						method142(spawnedObject.y, spawnedObject.plane, spawnedObject.previousOrientation,
								spawnedObject.previousType, spawnedObject.x, spawnedObject.group,
								spawnedObject.getPreviousId);
						spawnedObject.unlink();
					}
				} else {
					if (spawnedObject.delay > 0)
						spawnedObject.delay--;
					if (spawnedObject.delay == 0 && spawnedObject.x >= 1 && spawnedObject.y >= 1
							&& spawnedObject.x <= 102 && spawnedObject.y <= 102
							&& (spawnedObject.objectId < 0
							|| ObjectManager.method178(spawnedObject.objectId, spawnedObject.type))) {
						method142(spawnedObject.y, spawnedObject.plane, spawnedObject.orientation,
								spawnedObject.type, spawnedObject.x, spawnedObject.group,
								spawnedObject.objectId);
						spawnedObject.delay = -1;
						if (spawnedObject.objectId == spawnedObject.getPreviousId && spawnedObject.getPreviousId == -1)
							spawnedObject.unlink();
						else if (spawnedObject.objectId == spawnedObject.getPreviousId
								&& spawnedObject.orientation == spawnedObject.previousOrientation
								&& spawnedObject.type == spawnedObject.previousType)
							spawnedObject.unlink();
						passedRequest = true;
					}
				}
			}
			if (passedRequest)
				// anInt985 = plane;
				method24(plane);

		}
	}

	private void determineMenuSize() {
		int i = newBoldFont.getTextWidth("Choose Option");
		for (int j = 0; j < menuActionRow; j++) {
			int k = newBoldFont.getTextWidth(menuActionName[j]);
			if (k > i)
				i = k;
		}
		i += 8;
		int l = 15 * menuActionRow + 21;
		if (super.getSaveClickX() > 0 && super.getSaveClickY() > 0 && super.getSaveClickX() < currentGameWidth
				&& super.getSaveClickY() < currentGameHeight) {
			int xClick = super.getSaveClickX() - i / 2;
			if (xClick + i > currentGameWidth - 4) {
				xClick = currentGameWidth - 4 - i;
			}
			if (xClick < 0) {
				xClick = 0;
			}
			int yClick = super.getSaveClickY() - 0;
			if (yClick + l > currentGameHeight - 2) {
				yClick = currentGameHeight - 2 - l;
			}
			if (yClick < 0) {
				yClick = 0;
			}
			menuOpen = true;
			menuOffsetX = xClick;
			menuOffsetY = yClick;
			menuWidth = i;
			menuHeight = 15 * menuActionRow + 22;
		}

	}

	public void method117(Buffer stream) {
		stream.initBitAccess();
		int j = stream.readBits(1);
		if (j == 0)
			return;
		int k = stream.readBits(2);
		if (k == 0) {
			anIntArray894[anInt893++] = maxPlayerCount;
			return;
		}
		if (k == 1) {
			int l = stream.readBits(3);
			myPlayer.moveInDir((byte)1, l);
			int k1 = stream.readBits(1);
			if (k1 == 1)
				anIntArray894[anInt893++] = maxPlayerCount;
			return;
		}
		if (k == 2) {
			if (stream.readBits(1) == 1) {
			int i1 = stream.readBits(3);
			myPlayer.moveInDir((byte)2, i1);
			int l1 = stream.readBits(3);
			myPlayer.moveInDir((byte)2, l1);
			} else {
				int l1 = stream.readBits(3);
				myPlayer.moveInDir((byte)0, l1);
			}
			int j2 = stream.readBits(1);
			if (j2 == 1)
				anIntArray894[anInt893++] = maxPlayerCount;
			return;
		}
		if (k == 3) {
			plane = stream.readBits(2);
			int j1 = stream.readBits(1);
			int i2 = stream.readBits(1);
			if (i2 == 1)
				anIntArray894[anInt893++] = maxPlayerCount;
			int k2 = stream.readBits(7);
			int l2 = stream.readBits(7);
			myPlayer.setPos(l2, k2, j1 == 1);
		}
	}

	public void nullLoader() {
		aBoolean831 = false;
		while (drawingFlames) {
			aBoolean831 = false;
			try {
				Thread.sleep(50L);
			} catch (Exception _ex) {
			}
		}
		aBackgroundArray1152s = null;
		anIntArray850 = null;
		anIntArray851 = null;
		anIntArray852 = null;
		anIntArray853 = null;
		anIntArray1190 = null;
		anIntArray1191 = null;
		anIntArray828 = null;
		anIntArray829 = null;
		aClass30_Sub2_Sub1_Sub1_1201 = null;
		aClass30_Sub2_Sub1_Sub1_1202 = null;
	}

	private boolean method119(int i, int j) {
		boolean dirty = false;
		RSInterface class9 = RSInterface.interfaceCache[j];
		for (int k = 0; k < class9.children.length; k++) {
			if (class9.children[k] == -1)
				break;
			RSInterface class9_1 = RSInterface.interfaceCache[class9.children[k]];
			if (class9_1.type == 1)
				dirty |= method119(i, class9_1.id);
			if (class9_1.type == 6 && (class9_1.seq_id != -1 || class9_1.active_seq_id != -1)) {
				boolean flag2 = cs1_selected(class9_1);
				int anim_id;
				if (flag2)
					anim_id = class9_1.active_seq_id;
				else
					anim_id = class9_1.seq_id;
				if (anim_id != -1) {
					AnimationDefinition seqtype = AnimationDefinition.anims[anim_id];
					if (seqtype.using_keyframes()) {
						class9_1.iftype_frameindex += i;
						int var8 = seqtype.get_keyframe_duration();
						if (class9_1.iftype_frameindex >= var8) {
							class9_1.iftype_frameindex -= seqtype.loop_delay;
							if (class9_1.iftype_frameindex < 0 || class9_1.iftype_frameindex >= var8) {
								class9_1.iftype_frameindex = 0;
							}
						}

						dirty = true;
					} else {
						for (class9_1.iftype_loops_remaining += i; class9_1.iftype_loops_remaining > seqtype.anIntArray354[class9_1.iftype_frameindex]; ) {
							class9_1.iftype_loops_remaining -= seqtype.anIntArray354[class9_1.iftype_frameindex] + 1;
							class9_1.iftype_frameindex++;
							if (class9_1.iftype_frameindex >= seqtype.framecount) {
								class9_1.iftype_frameindex -= seqtype.loop_delay;
								if (class9_1.iftype_frameindex < 0 || class9_1.iftype_frameindex >= seqtype.framecount)
									class9_1.iftype_frameindex = 0;
							}
							dirty = true;
						}
					}

				}
			}
		}

		return dirty;
	}

	public int getXCameraPosShift() {
		int i = xCameraPos >> 7;
		return i < 0 ? 0 : i >= 104 ? 103 : i;
	}

	public int getYCameraPosShift() {
		int i = yCameraPos >> 7;
		return i < 0 ? 0 : i >= 104 ? 103 : i;
	}

	private int method120() {
		int j = 3;
		if (yCameraCurve < 310 || removeRoofs) {
			int k = getXCameraPosShift();
			int l = getYCameraPosShift();
			int i1 = myPlayer.x >> 7;
			int j1 = myPlayer.y >> 7;
			if ((byteGroundArray[plane][k][l] & 4) != 0)
				j = plane;
			int k1;
			if (i1 > k)
				k1 = i1 - k;
			else
				k1 = k - i1;
			int l1;
			if (j1 > l)
				l1 = j1 - l;
			else
				l1 = l - j1;
			if (k1 > l1) {
				int i2 = (l1 * 0x10000) / k1;
				int k2 = 32768;
				while (k != i1) {
					if (k < i1)
						k++;
					else if (k > i1)
						k--;
					if ((byteGroundArray[plane][k][l] & 4) != 0 || removeRoofs)
						j = plane;
					k2 += i2;
					if (k2 >= 0x10000) {
						k2 -= 0x10000;
						if (l < j1)
							l++;
						else if (l > j1)
							l--;
						if ((byteGroundArray[plane][k][l] & 4) != 0 || removeRoofs)
							j = plane;
					}
				}
			} else {
				if (l1 == 0) // /zero crash fix
					l1 = 1;
				int j2 = (k1 * 0x10000) / l1;
				int l2 = 32768;
				while (l != j1) {
					if (l < j1)
						l++;
					else if (l > j1)
						l--;
					if ((byteGroundArray[plane][k][l] & 4) != 0 || removeRoofs)
						j = plane;
					l2 += j2;
					if (l2 >= 0x10000) {
						l2 -= 0x10000;
						if (k < i1)
							k++;
						else if (k > i1)
							k--;
						if ((byteGroundArray[plane][k][l] & 4) != 0 || removeRoofs)
							j = plane;
					}
				}
			}
		}
		if ((byteGroundArray[plane][myPlayer.x >> 7][myPlayer.y >> 7] & 4) != 0)
			j = plane;
		return j;
	}

	private int method121() {
		int j = getCenterHeight(plane, yCameraPos, xCameraPos);
		int x = getXCameraPosShift();
		int y = getYCameraPosShift();
		if (j - zCameraPos < 800 && (byteGroundArray[plane][x][y] & 4) != 0)
			return plane;
		else
			return 3;
	}

	private void delIgnore(long l) {
		try {
			if (l == 0L)
				return;
			for (int j = 0; j < ignoreCount; j++)
				if (ignoreListAsLongs[j] == l) {
					ignoreCount--;
					needDrawTabArea = true;
					System.arraycopy(ignoreListAsLongs, j + 1, ignoreListAsLongs, j, ignoreCount - j);

					stream.createFrame(74);
					stream.writeQWord(l);
					return;
				}

			return;
		} catch (RuntimeException runtimeexception) {
			Signlink.reporterror("47229, " + 3 + ", " + l + ", " + runtimeexception.toString());
		}
		throw new RuntimeException();
	}

	@Override
	public String getParameter(String s) {
		if (Signlink.mainapp != null)
			return Signlink.mainapp.getParameter(s);
		else
			return super.getParameter(s);
	}

	public void adjustVolume(boolean flag, int i) {
		Signlink.midivol = i;
		if (flag)
			Signlink.midi = "voladjust";
	}

	private int extractInterfaceValues(RSInterface widget, int id) {
		if (widget.scripts == null || id >= widget.scripts.length)
			return -2;
		try {
			int script[] = widget.scripts[id];
			int accumulator = 0;
			int counter = 0;
			int operator = 0;
			do {
				int instruction = script[counter++];
				int value = 0;
				byte next = 0;
				if (instruction == 0)
					return accumulator;
				if (instruction == 1)
					value = currentStats[script[counter++]];
				if (instruction == 2)
					value = maxStats[script[counter++]];
				if (instruction == 3)
					value = currentExp[script[counter++]];
				if (instruction == 4) { // Check for item inside interface, interfaceId = ai[l++]
					RSInterface inventoryContainer = RSInterface.interfaceCache[script[counter++]];
					int item = script[counter++];
					if (item >= 0 && item < ItemDefinition.totalItems
							&& (!ItemDefinition.forID(item).membersObject || isMembers)) {
						int itemId = item + 1;
						int actualItemId = item;
						for (int slot = 0; slot < inventoryContainer.inventoryItemId.length; slot++) {
							if (inventoryContainer.inventoryItemId[slot] == itemId)
								value += inventoryContainer.inventoryAmounts[slot];
						}

						if (inventoryContainer.id == 3214) {
							RSInterface equipment = RSInterface.interfaceCache[1688];
							if (actualItemId == Items.NATURE_RUNE) {
								if (equipment.hasItem(Items.BRYOPHYTAS_STAFF)) {
									return Integer.MAX_VALUE;
								}
							}
							if (actualItemId == Items.FIRE_RUNE) {
								if (equipment.hasItem(Items.TOME_OF_FIRE)) {
									return Integer.MAX_VALUE;
								}
							}
						}

						if ((runePouch[0][0] + 1) == itemId) {
							value += runePouch[0][1];
						}
						if ((runePouch[1][0] + 1) == itemId) {
							value += runePouch[1][1];
						}
						if ((runePouch[2][0] + 1) == itemId) {
							value += runePouch[2][1];
						}
					}
				}
				if (instruction == 5) {
					value = variousSettings[script[counter++]];
				}
				if (instruction == 6)
					value = SKILL_EXPERIENCE[maxStats[script[counter++]] - 1];
				if (instruction == 7)
					value = (variousSettings[script[counter++]] * 100) / 46875;
				if (instruction == 8)
					value = myPlayer.combatLevel;
				if (instruction == 9) {
					for (int l1 = 0; l1 < Skills.SKILLS_COUNT; l1++)
						if (Skills.SKILLS_ENABLED[l1])
							value += maxStats[l1];
				}
				if (instruction == 10) {
					RSInterface equipmentContainer = RSInterface.interfaceCache[script[counter++]];
					int item = script[counter++] + 1;
					if (item >= 0 && item < ItemDefinition.totalItems
							&& (!ItemDefinition.forID(item).membersObject || isMembers)) {
						for (int stored = 0; stored < equipmentContainer.inventoryItemId.length; stored++) {
							if (equipmentContainer.inventoryItemId[stored] != item)
								continue;
							value = 0x3b9ac9ff;
							break;
						}

					}
				}
				if (instruction == 11)
					value = energy;
				if (instruction == 12)
					value = weight;
				if (instruction == 13) {
					int bool = variousSettings[script[counter++]];
					int shift = script[counter++];
					value = (bool & 1 << shift) == 0 ? 0 : 1;
				}
				if (instruction == 14) {
					int index = script[counter++];
					VarBit bits = VarBit.cache[index];
					int setting = bits.configID;
					int low = bits.lsb;
					int high = bits.msb;
					int mask = anIntArray1232[high - low];
					value = variousSettings[setting] >> low & mask;
				}
				if (instruction == 15)
					next = 1;
				if (instruction == 16)
					next = 2;
				if (instruction == 17)
					next = 3;
				if (instruction == 18)
					value = (myPlayer.x >> 7) + baseX;
				if (instruction == 19)
					value = (myPlayer.y >> 7) + baseY;
				if (instruction == 20)
					value = script[counter++];
				if (next == 0) {
					if (operator == 0)
						accumulator += value;
					if (operator == 1)
						accumulator -= value;
					if (operator == 2 && value != 0)
						accumulator /= value;
					if (operator == 3)
						accumulator *= value;
					operator = 0;
				} else {
					operator = next;
				}
			} while (true);
		} catch (Exception _ex) {
			return -1;
		}
	}

	public void drawTopLeftTooltip() {
		if (devConsole.console_open)
			return;
		if (tabInterfaceIDs[tabID] == 17200 && menuActionName[1].contains("ctivate")) {
			menuActionName[1] = "Select";
			menuActionID[1] = 850;
			menuActionRow = 2;
		}
		if (menuActionRow < 2 && itemSelected == 0 && spellSelected == 0)
			return;
		String s;
		if (itemSelected == 1 && menuActionRow < 2)
			s = "Use " + selectedItemName + " with...";
		else if (spellSelected == 1 && menuActionRow < 2)
			s = spellTooltip + "...";
		else
			s = menuActionName[menuActionRow - 1];
		if (menuActionRow > 2)
			s = s + "@whi@ / " + (menuActionRow - 2) + " more options";
		toolTip=s;
		newBoldFont.drawString(s, 8, 19, 0xffffff, 0, 255);
	}
	private String toolTip;

	private Sprite compassImage;
	private Sprite[] mapArea = new Sprite[8];

	private Sprite eventIcon;
	public Sprite bankDivider;

	private int[][] getCustomMapIcons() {
		return new int[][] {
		};
	}
	private void drawMinimap() {
		if (currentScreenMode == ScreenMode.FIXED)
			mapAreaGraphicsBuffer.initDrawingArea();

		/** Black minimap **/
		if (minimapState == 2) {
			if (currentScreenMode == ScreenMode.FIXED) {
				mapArea[3].drawSprite(0, 4);
			} else {
				mapArea[2].drawSprite(currentGameWidth - 183, 0);
				mapArea[4].drawSprite(currentGameWidth - 160, 8);
			}
			if(getUserSettings().isOldGameframe() && currentScreenMode == ScreenMode.FIXED ){
				compassImage.method352(33, viewRotation, anIntArray1057, 256, anIntArray968,
						(currentScreenMode == ScreenMode.FIXED ? 28 : 25), 4,
						(currentScreenMode == ScreenMode.FIXED ? 27 : currentGameWidth - 178), 33, 25);
			}else {
				compassImage.method352(33, viewRotation, anIntArray1057, 256, anIntArray968,
						(currentScreenMode == ScreenMode.FIXED ? 25 : 25), 4,
						(currentScreenMode == ScreenMode.FIXED ? 29 : currentGameWidth - 178), 33, 25);
			}
			// if (drawOrbs)
			// loadAllOrbs(currentScreenMode == ScreenMode.FIXED ? 0 : currentGameWidth -
			// 217);
			if (menuOpen) {
				drawMenu(currentScreenMode == ScreenMode.FIXED ? 516 : 0, 0);
			}
			mainGameGraphicsBuffer.setCanvas();
			return;
		}

		// If not loaded in don't draw mapback
		if (loadingStage == 2) {

			int i = viewRotation + minimapRotation & 0x7ff;
			int j = 48 + myPlayer.x / 32;
			int l2 = 464 - myPlayer.y / 32;

			int positionX = (currentScreenMode == ScreenMode.FIXED ? 9 : 7);
			int positionY = (currentScreenMode == ScreenMode.FIXED ? 51 : currentGameWidth - 160);

			minimapImage.method352(151, i, anIntArray1229, 256 + minimapZoom, anIntArray1052, l2, positionX, positionY, 151,
					j);


			for (int j5 = 0; j5 < mapIconAmount; j5++) {
				int k = (anIntArray1072[j5] * 4 + 2) - myPlayer.x / 32;
				int i3 = (anIntArray1073[j5] * 4 + 2) - myPlayer.y / 32;
				markMinimap(mapIconSprite[j5], k, i3);
			}


			for (int k5 = 0; k5 < 104; k5++) {
				for (int l5 = 0; l5 < 104; l5++) {
					NodeList class19 = groundItems[plane][k5][l5];
					if (class19 != null) {
						int l = (k5 * 4 + 2) - myPlayer.x / 32;
						int j3 = (l5 * 4 + 2) - myPlayer.y / 32;
						markMinimap(mapDotItem, l, j3);
					}
				}
			}

			for (int i6 = 0; i6 < npcCount; i6++) {
				NPC npc = npcs[npcIndices[i6]];
				if (npc != null && npc.isVisible()) {
					NpcDefinition entityDef = npc.desc;
					if (entityDef.childrenIDs != null)
						entityDef = entityDef.method161();
					if (entityDef != null && entityDef.onMinimap && entityDef.aBoolean84) {
						int i1 = npc.x / 32 - myPlayer.x / 32;
						int k3 = npc.y / 32 - myPlayer.y / 32;
						markMinimap(mapDotNPC, i1, k3);
					}
				}
			}

			for (int j6 = 0; j6 < playerCount; j6++) {
				Player player = players[playerIndices[j6]];
				if (player != null && player.isVisible()) {
					int j1 = player.x / 32 - myPlayer.x / 32;
					int l3 = player.y / 32 - myPlayer.y / 32;
					boolean flag1 = false;
					boolean flag3 = false;
					String clanname;
					for (int j3 = 0; j3 < clanList.length; j3++) {
						if (clanList[j3] == null)
							continue;
						clanname = clanList[j3];
						if (clanname.startsWith("<clan"))
							clanname = clanname.substring(clanname.indexOf(">") + 1, clanname.length());
						if (!clanname.equalsIgnoreCase(player.displayName))
							continue;
						flag3 = true;
						break;
					}
					long l6 = StringUtils.longForName(player.displayName);
					for (int k6 = 0; k6 < friendsCount; k6++) {
						if (l6 != friendsListAsLongs[k6] || friendsNodeIDs[k6] == 0)
							continue;
						flag1 = true;
						break;
					}
					boolean flag2 = false;
					if (myPlayer.team != 0 && player.team != 0 && myPlayer.team == player.team)
						flag2 = true;
					if (flag1)
						markMinimap(mapDotFriend, j1, l3);
					else if (flag3)
						markMinimap(mapDotClan, j1, l3);
					else if (flag2)
						markMinimap(mapDotTeam, j1, l3);
					else
						markMinimap(mapDotPlayer, j1, l3);
				}
			}

			if (hintType != 0 && update_tick % 20 < 10) {
				if (hintType == 1 && anInt1222 >= 0 && anInt1222 < npcs.length) {
					NPC npc = npcs[anInt1222];
					if (npc != null) {
						int k1 = npc.x / 32 - myPlayer.x / 32;
						int i4 = npc.y / 32 - myPlayer.y / 32;
						method81(mapMarker, i4, k1);
					}
				}
				if (hintType == 2) {
					int l1 = ((hintIconXpos - baseX) * 4 + 2) - myPlayer.x / 32;
					int j4 = ((hintIconYpos - baseY) * 4 + 2) - myPlayer.y / 32;
					method81(mapMarker, j4, l1);
				}
				if (hintType == 10 && anInt933 >= 0 && anInt933 < players.length) {
					Player player = players[anInt933];
					if (player != null) {
						int i2 = player.x / 32 - myPlayer.x / 32;
						int k4 = player.y / 32 - myPlayer.y / 32;
						method81(mapMarker, k4, i2);
					}
				}
			}

			if (destX != 0) {
				int j2 = (destX * 4 + 2) - myPlayer.x / 32;
				int l4 = (destY * 4 + 2) - myPlayer.y / 32;
				markMinimap(mapFlag, j2, l4);
			}
			DrawingArea.drawPixels(3, (currentScreenMode == ScreenMode.FIXED ? 83 : 81),
					(currentScreenMode == ScreenMode.FIXED ? 125 : currentGameWidth - 85), 0xffffff, 3);
		}
		if (currentScreenMode == ScreenMode.FIXED) {

			mapArea[0].drawSprite(0, 4);
			mapArea[5].drawSprite(0, 0);
		} else
			mapArea[2].drawSprite(currentGameWidth - 183, 0);

		compassImage.method352(33, viewRotation, anIntArray1057, 256, anIntArray968,
				(currentScreenMode == ScreenMode.FIXED ? 25 : 25), 4,
				(currentScreenMode == ScreenMode.FIXED ? 29 : currentGameWidth - 178), 33, 25);
		if (drawOrbs)
			loadAllOrbs(currentScreenMode == ScreenMode.FIXED ? 0 : currentGameWidth - 217);
		if (currentScreenMode == ScreenMode.FIXED) {
			cacheSprite2[6].drawSprite(198 - 2, 19 + 100);
			if (worldHover) {
				cacheSprite2[1].drawSprite(202 - 2, 123);
			} else {
				cacheSprite2[0].drawSprite(202 - 2, 123);
			}
			if (wikiHover) {
				wiki[1].drawSprite(190, 149);

			} else {
				wiki[0].drawSprite(190, 149);

			}
		} else {


			cacheSprite2[6].drawSprite(currentGameWidth - 35, 125);
			if (worldHover) {
				cacheSprite2[1].drawSprite(currentGameWidth - 31, 129);
			} else {
				cacheSprite2[0].drawSprite(currentGameWidth - 31, 129);
			}
				wiki[0].drawSprite(currentGameWidth - 43, 155);
		}
		if (menuOpen) {
			drawMenu(currentScreenMode == ScreenMode.FIXED ? 516 : 0, 0);
		}
		mainGameGraphicsBuffer.setCanvas();
	}

	private boolean hpHover = false;

	private void loadHpOrb(int xOffset) {
		int yOff = Configuration.osbuddyGameframe ? currentScreenMode == ScreenMode.FIXED ? 0 : -5
				: currentScreenMode == ScreenMode.FIXED ? 0 : -5;
		int xOff = Configuration.osbuddyGameframe ? currentScreenMode == ScreenMode.FIXED ? 0 : -6
				: currentScreenMode == ScreenMode.FIXED ? 0 : -6;
		String cHP = RSInterface.interfaceCache[4016].message;
		String mHP = RSInterface.interfaceCache[4017].message;
		int currentHP = Integer.parseInt(cHP);
		int maxHP = Integer.parseInt(mHP);
		int health = (int) (((double) currentHP / (double) maxHP) * 100D);
		int poisonType = variousSettings[Configs.POISON_CONFIG];
		int hover = poisonType == 0 ? 173 : 173;
		Sprite bg = cacheSprite3[hpHover ? hover : 172];
		Sprite fg = null;
		if (poisonType == 0) {
			fg = cacheSprite3[161];
		}
		if (poisonType == 1) {
			fg = cacheSprite3[162];
		}
		if (poisonType == 2) {
			fg = venomOrb;
		}
		bg.drawSprite(0 + xOffset - xOff, 41 - yOff);
		fg.drawSprite(27 + xOffset - xOff, 45 - yOff);
		if (getOrbFill(health) <= 26) {
			cacheSprite3[160].myHeight = getOrbFill(health);
		} else {
			cacheSprite3[160].myHeight = 26;
		}
		cacheSprite3[160].drawSprite(27 + xOffset - xOff, 45 - yOff);
		cacheSprite3[168].drawSprite(27 + xOffset - xOff, 45 - yOff);
		if (health > 1_000_000_000) {
			infinity.drawSprite(10 + xOffset - xOff, 59 - yOff);
		} else {
			smallText.method382(getOrbTextColor(health), 15 + xOffset - xOff, "" + cHP, 67 - yOff, true);
		}
	}

	public double fillHP;

	public Sprite worldMap1, worldMap2, worldMap3;

	private boolean specialHover;
	private int specialEnabled;
	public int specialAttack=100;

	private void drawSpecialOrb(int xOffset) {
		Sprite image = cacheSprite1[specialHover ? 8 : 7];
		Sprite fill = cacheSprite[specialEnabled == 0 ? 9 : 6];
		Sprite sword = cacheSprite[12];
		double percent = specialAttack  / (double) 100;
		//int percent = 100;
		boolean isFixed = currentScreenMode == ScreenMode.FIXED;
		image.drawSprite((isFixed ? 37 : 37) + xOffset, isFixed ? 134 : 139);
		if(specialEnabled==1) {
			//fill.drawSprite((isFixed ? 60 : 133) + xOffset, isFixed ? 134 : 151);
			fill.drawSprite((isFixed ? 63 : 63) + xOffset, isFixed ? 137 : 141);
		}else{
			fill.drawSprite((isFixed ? 64 : 64) + xOffset, isFixed ? 138 : 143);
			//fill.drawSprite((isFixed ? 65 : 133) + xOffset, isFixed ? 139 : 151);
		}
		sword.drawSprite((isFixed ? 64 : 64) + xOffset, isFixed ? 138 : 143);

		smallText.method382(getOrbTextColor((int) (percent * 100)),
				(isFixed ? 53 : 53) + xOffset, specialAttack + "", isFixed ? 159 : 164,
				true);
	}

	private void loadPrayerOrb(int xOffset) {
		int yOff = Configuration.osbuddyGameframe ? currentScreenMode == ScreenMode.FIXED ? 10 : 2
				: currentScreenMode == ScreenMode.FIXED ? 0 : -5;
		int xOff = Configuration.osbuddyGameframe ? currentScreenMode == ScreenMode.FIXED ? -1 : -7
				: currentScreenMode == ScreenMode.FIXED ? -1 : -7;
		Sprite bg = cacheSprite1[prayHover ? 8 : 7];
		Sprite fg = prayClicked ? new Sprite("Gameframe/newprayclicked") : cacheSprite1[1];
		bg.drawSprite(0 + xOffset - xOff, 75 - yOff);
		fg.drawSprite(27 + xOffset - xOff, 79 - yOff);
		int level = Integer.parseInt(RSInterface.interfaceCache[4012].message.replaceAll("%", ""));
		int max = maxStats[5];
		double percent = level / (double) max;
		cacheSprite1[14].myHeight = (int) (26 * (1 - percent));
		cacheSprite1[14].drawSprite(27 + xOffset - xOff, 79 - yOff);
		if (percent <= .25) {
			cacheSprite1[10].drawSprite(30 + xOffset - xOff, 82 - yOff);
		} else {
			cacheSprite1[10].drawSprite(30 + xOffset - xOff, 82 - yOff);
		}
		if (level > 1_000_000_000) {
			infinity.drawSprite(11 + xOffset - xOff, 94 - yOff);
		} else {
			smallText.method382(getOrbTextColor((int) (percent * 100)), 14 + xOffset - xOff, level + "", 101 - yOff, true);
		}


	}

	private void loadRunOrb(int xOffset) {
		int current = Integer.parseInt(RSInterface.interfaceCache[22539].message.replaceAll("%", ""));
		int yOff = Configuration.osbuddyGameframe ? currentScreenMode == ScreenMode.FIXED ? 15 : 5
				: currentScreenMode == ScreenMode.FIXED ? 1 : -4;
		int xMinus = Configuration.osbuddyGameframe ? currentScreenMode == ScreenMode.FIXED ? 11 : 5
				: currentScreenMode == ScreenMode.FIXED ? -1 : -6;
		Sprite bg = cacheSprite1[runHover ? 8 : 7];
		boolean running = anIntArray1045[173] == 1;
		Sprite fg = cacheSprite1[running ? 4 : 3];
		bg.drawSprite(10 + xOffset - xMinus, 109 - yOff);
		fg.drawSprite(37 + xOffset - xMinus, 113 - yOff);
		int level = current;
		double percent = level / (double) 100;
		cacheSprite1[14].myHeight = (int) (26 * (1 - percent));
		cacheSprite1[14].drawSprite(37 + xOffset - xMinus, 113 - yOff);
		if (percent <= .25) {
			cacheSprite1[running ? 12 : 11].drawSprite(43 + xOffset - xMinus, 117 - yOff);
		} else {
			cacheSprite1[running ? 12 : 11].drawSprite(43 + xOffset - xMinus, 117 - yOff);
		}
		smallText.method382(getOrbTextColor((int) (percent * 100)), 25 + xOffset - xMinus, level + "", 135 - yOff,
				true);
	}

	private Sprite venomOrb;

	private void loadAllOrbs(int xOffset) {
		try {
			loadHpOrb(xOffset);
			loadPrayerOrb(xOffset);
			loadRunOrb(xOffset);
			drawSpecialOrb(xOffset);
		} catch (Exception e) {}
		if (drawExperienceCounter) {
			if (counterHover) {
				cacheSprite2[5].drawSprite(
						drawOrbs && currentScreenMode == ScreenMode.FIXED ? 0 : currentGameWidth - 211,
						currentScreenMode == ScreenMode.FIXED ? 21 : 25);
			} else {
				cacheSprite2[3].drawSprite(
						drawOrbs && currentScreenMode == ScreenMode.FIXED ? 0 : currentGameWidth - 211,
						currentScreenMode == ScreenMode.FIXED ? 21 : 25);
			}
		} else {
			if (counterHover) {
				cacheSprite2[4].drawSprite(
						drawOrbs && currentScreenMode == ScreenMode.FIXED ? 0 : currentGameWidth - 211,
						currentScreenMode == ScreenMode.FIXED ? 21 : 25);
			} else {
				cacheSprite2[2].drawSprite(
						drawOrbs && currentScreenMode == ScreenMode.FIXED ? 0 : currentGameWidth - 211,
						currentScreenMode == ScreenMode.FIXED ? 21 : 25);
			}
		}
		if (currentScreenMode == ScreenMode.FIXED) {
			// cacheSprite[worldHover ? 5 : 4].drawSprite(202, 20);
		} else {
			// cacheSprite[worldHover ? 3 : 2].drawSprite(Client.currentGameWidth - 118,
			// 154);
		}
		if (Configuration.osbuddyGameframe) {
			//loadSpecialOrb(xOffset);
		}
		DrawingArea.drawAlphaBox(0, 0, 1, 200, 0x332B16, 250);
	}

	public boolean runClicked = false;

	public int getOrbTextColor(int i) {
		if (i >= 75 && i <= 0x7fffffff)
			return 65280;
		if (i >= 50 && i <= 74)
			return 0xffff00;
		return i < 25 || i > 49 ? 0xff0000 : 0xFFB400;
	}

	private Sprite mapIcon9, mapIcon7, mapIcon8, mapIcon6, mapIcon5;

	public int getOrbFill(int statusInt) {
		if (statusInt <= Integer.MAX_VALUE && statusInt >= 97)
			return 0;
		else if (statusInt <= 96 && statusInt >= 93)
			return 1;
		else if (statusInt <= 92 && statusInt >= 89)
			return 2;
		else if (statusInt <= 88 && statusInt >= 85)
			return 3;
		else if (statusInt <= 84 && statusInt >= 81)
			return 4;
		else if (statusInt <= 80 && statusInt >= 77)
			return 5;
		else if (statusInt <= 76 && statusInt >= 73)
			return 6;
		else if (statusInt <= 72 && statusInt >= 69)
			return 7;
		else if (statusInt <= 68 && statusInt >= 65)
			return 8;
		else if (statusInt <= 64 && statusInt >= 61)
			return 9;
		else if (statusInt <= 60 && statusInt >= 57)
			return 10;
		else if (statusInt <= 56 && statusInt >= 53)
			return 11;
		else if (statusInt <= 52 && statusInt >= 49)
			return 12;
		else if (statusInt <= 48 && statusInt >= 45)
			return 13;
		else if (statusInt <= 44 && statusInt >= 41)
			return 14;
		else if (statusInt <= 40 && statusInt >= 37)
			return 15;
		else if (statusInt <= 36 && statusInt >= 33)
			return 16;
		else if (statusInt <= 32 && statusInt >= 29)
			return 17;
		else if (statusInt <= 28 && statusInt >= 25)
			return 18;
		else if (statusInt <= 24 && statusInt >= 21)
			return 19;
		else if (statusInt <= 20 && statusInt >= 17)
			return 20;
		else if (statusInt <= 16 && statusInt >= 13)
			return 21;
		else if (statusInt <= 12 && statusInt >= 9)
			return 22;
		else if (statusInt <= 8 && statusInt >= 7)
			return 23;
		else if (statusInt <= 6 && statusInt >= 5)
			return 24;
		else if (statusInt <= 4 && statusInt >= 3)
			return 25;
		else if (statusInt <= 2 && statusInt >= 1)
			return 26;
		else if (statusInt <= 0)
			return 27;
		return 0;
	}

	public Sprite xpSprite;
	public Sprite xpbg1 = new Sprite("487");
	public Sprite xpbg2 = new Sprite("488");

	private Queue<ExperienceDrop> experienceDrops = new LinkedList<>();

	private void processExperienceCounter() {
		if (update_tick % 1 <= 1 && !experienceDrops.isEmpty()) {
			Collection<ExperienceDrop> remove = new ArrayList<>();
			for (ExperienceDrop drop : experienceDrops) {
				drop.pulse();
				if (drop.getYPosition() == -1) {
					experienceCounter += drop.getAmount();
					remove.add(drop);
				}
			}
			experienceDrops.removeAll(remove);
		}

		if (!drawExperienceCounter || openInterfaceID > -1) {
			return;
		}

		for (ExperienceDrop drop : experienceDrops) {
			String text = drop.toString();
			int x = (currentScreenMode == ScreenMode.FIXED ? 507 : currentGameWidth - 246)
					- newSmallFont.getTextWidth(text);
			int y = drop.getYPosition() - 15;
			int transparency = 256;
			newSmallFont.drawString(text, x, y, 0xFFFFFF, 0x000000, 256);

			for (int skill : drop.getSkills()) {
				if(skill==22){
					continue;
				}

				Sprite sprite = smallXpSprites[skill];
				x -= sprite.myWidth + 3;
				y -= sprite.myHeight - 4;
				sprite.drawAdvancedTransparentSprite(x, y, transparency);
				y += sprite.myHeight - 4;
			}
		}

		String experience = NumberFormat.getInstance().format(experienceCounter);

		xpbg1.drawAdvancedSprite(currentScreenMode == ScreenMode.FIXED ? 395 : currentGameWidth - 365, 6);
		xpbg2.drawSprite(currentScreenMode == ScreenMode.FIXED ? 398 : currentGameWidth - 363, 9);

		newSmallFont.drawBasicString(experience, (currentScreenMode == ScreenMode.FIXED ? 510 : currentGameWidth - 252)
				- newSmallFont.getTextWidth(experience), 24, 0xFFFFFF, 0x000000);
	}

	private boolean drawExperienceCounter = true;

	public void npcScreenPos(Entity entity, int i) {
		calcEntityScreenPos(entity.x, i, entity.y);
	}

	public void calcEntityScreenPos(int i, int j, int l) {
		WorldController.focalLength = Rasterizer.width;
		if ((i < 128) || (l < 128) || (i > 13056) || (l > 13056)) {
			this.spriteDrawX = -1;
			this.spriteDrawY = -1;
			return;
		}
		int i1 = getCenterHeight(this.plane, l, i) - j; //getCenterHeight
		i -= this.xCameraPos;
		i1 -= this.zCameraPos;
		l -= this.yCameraPos;
		int j1 = Model.SINE[this.yCameraCurve];
		int k1 = Model.COSINE[this.yCameraCurve];
		int l1 = Model.SINE[this.xCameraCurve];
		int i2 = Model.COSINE[this.xCameraCurve];
		int j2 = l * l1 + i * i2 >> 16;
		l = l * i2 - i * l1 >> 16;
		i = j2;
		j2 = i1 * k1 - l * j1 >> 16;
		l = i1 * j1 + l * k1 >> 16;
		i1 = j2;
		if (l >= 50) {
			spriteDrawX = Rasterizer.textureInt1 + (i << WorldController.viewDistance) / l;
			spriteDrawY = Rasterizer.textureInt2 + (i1 << WorldController.viewDistance) / l;
		}else {
			this.spriteDrawX = -1;
			this.spriteDrawY = -1;
		}
		WorldController.focalLength = 512;
	}

	public void method130(int j, int k, int l, int i1, int j1, int k1, int l1, int i2, int j2) {
		SpawnedObject spawnedObject = null;
		for (spawnedObject = (SpawnedObject) spawns
				.reverseGetFirst(); spawnedObject != null; spawnedObject = (SpawnedObject) spawns
				.reverseGetNext()) {
			if (spawnedObject.plane != l1 || spawnedObject.x != i2 || spawnedObject.y != j1
					|| spawnedObject.group != i1)
				continue;
			spawnedObject = spawnedObject;
			break;
		}

		if (spawnedObject == null) {
			spawnedObject = new SpawnedObject();
			spawnedObject.plane = l1;
			spawnedObject.group = i1;
			spawnedObject.x = i2;
			spawnedObject.y = j1;
			method89(spawnedObject);
			spawns.insertHead(spawnedObject);
		}
		spawnedObject.objectId = k;
		spawnedObject.type = k1;
		spawnedObject.orientation = l;
		spawnedObject.delay = j2;
		spawnedObject.getLongetivity = j;
	}

	private boolean cs1_selected(RSInterface class9) {
		if (class9.anIntArray245 == null)
			return false;
		for (int i = 0; i < class9.anIntArray245.length; i++) {
			int j = extractInterfaceValues(class9, i);
			int k = class9.anIntArray212[i];
			if (class9.anIntArray245[i] == 2) {
				if (j >= k)
					return false;
			} else if (class9.anIntArray245[i] == 3) {
				if (j <= k)
					return false;
			} else if (class9.anIntArray245[i] == 4) {
				if (j == k)
					return false;
			} else if (j != k)
				return false;
		}

		return true;
	}

	public void doFlamesDrawing() {
		char c = '\u0100';
		if (anInt1040 > 0) {
			for (int i = 0; i < 256; i++)
				if (anInt1040 > 768)
					anIntArray850[i] = method83(anIntArray851[i], anIntArray852[i], 1024 - anInt1040);
				else if (anInt1040 > 256)
					anIntArray850[i] = anIntArray852[i];
				else
					anIntArray850[i] = method83(anIntArray852[i], anIntArray851[i], 256 - anInt1040);

		} else if (anInt1041 > 0) {
			for (int j = 0; j < 256; j++)
				if (anInt1041 > 768)
					anIntArray850[j] = method83(anIntArray851[j], anIntArray853[j], 1024 - anInt1041);
				else if (anInt1041 > 256)
					anIntArray850[j] = anIntArray853[j];
				else
					anIntArray850[j] = method83(anIntArray853[j], anIntArray851[j], 256 - anInt1041);

		} else {
			System.arraycopy(anIntArray851, 0, anIntArray850, 0, 256);

		}
		System.arraycopy(aClass30_Sub2_Sub1_Sub1_1201.myPixels, 0, aRSImageProducer_1110.canvasRaster, 0, 33920);

		int i1 = 0;
		int j1 = 1152;
		for (int k1 = 1; k1 < c - 1; k1++) {
			int l1 = (anIntArray969[k1] * (c - k1)) / c;
			int j2 = 22 + l1;
			if (j2 < 0)
				j2 = 0;
			i1 += j2;
			for (int l2 = j2; l2 < 128; l2++) {
				int j3 = anIntArray828[i1++];
				if (j3 != 0) {
					int l3 = j3;
					int j4 = 256 - j3;
					j3 = anIntArray850[j3];
					int l4 = aRSImageProducer_1110.canvasRaster[j1];
					aRSImageProducer_1110.canvasRaster[j1++] = ((j3 & 0xff00ff) * l3 + (l4 & 0xff00ff) * j4
							& 0xff00ff00) + ((j3 & 0xff00) * l3 + (l4 & 0xff00) * j4 & 0xff0000) >> 8;
				} else {
					j1++;
				}
			}

			j1 += j2;
		}

		aRSImageProducer_1110.drawGraphics(0, 0, super.graphics);
		System.arraycopy(aClass30_Sub2_Sub1_Sub1_1202.myPixels, 0, aRSImageProducer_1111.canvasRaster, 0, 33920);

		i1 = 0;
		j1 = 1176;
		for (int k2 = 1; k2 < c - 1; k2++) {
			int i3 = (anIntArray969[k2] * (c - k2)) / c;
			int k3 = 103 - i3;
			j1 += i3;
			for (int i4 = 0; i4 < k3; i4++) {
				int k4 = anIntArray828[i1++];
				if (k4 != 0) {
					int i5 = k4;
					int j5 = 256 - k4;
					k4 = anIntArray850[k4];
					int k5 = aRSImageProducer_1111.canvasRaster[j1];
					aRSImageProducer_1111.canvasRaster[j1++] = ((k4 & 0xff00ff) * i5 + (k5 & 0xff00ff) * j5
							& 0xff00ff00) + ((k4 & 0xff00) * i5 + (k5 & 0xff00) * j5 & 0xff0000) >> 8;
				} else {
					j1++;
				}
			}

			i1 += 128 - k3;
			j1 += 128 - k3 - i3;
		}

		aRSImageProducer_1111.drawGraphics(637, 0, super.graphics);
	}

	public void method134(Buffer stream) {
		int j = stream.readBits(8);
		if (j < playerCount) {
			for (int k = j; k < playerCount; k++)
				anIntArray840[anInt839++] = playerIndices[k];

		}
		if (j > playerCount) {
			Signlink.reporterror(myUsername + " Too many players");
			throw new RuntimeException("eek");
		}
		playerCount = 0;
		for (int l = 0; l < j; l++) {
			int i1 = playerIndices[l];
			Player player = players[i1];
			int j1 = stream.readBits(1);
			if (j1 == 0) {
				playerIndices[playerCount++] = i1;
				player.anInt1537 = update_tick;
			} else {
				int k1 = stream.readBits(2);
				if (k1 == 0) {
					playerIndices[playerCount++] = i1;
					player.anInt1537 = update_tick;
					anIntArray894[anInt893++] = i1;
				} else if (k1 == 1) {
					playerIndices[playerCount++] = i1;
					player.anInt1537 = update_tick;
					int l1 = stream.readBits(3);
					player.moveInDir((byte)1, l1);
					int j2 = stream.readBits(1);
					if (j2 == 1)
						anIntArray894[anInt893++] = i1;
				} else if (k1 == 2) {
					playerIndices[playerCount++] = i1;
					player.anInt1537 = update_tick;
					if(stream.readBits(1) == 1) {
					int i2 = stream.readBits(3);
					player.moveInDir((byte)2, i2);
					int k2 = stream.readBits(3);
					player.moveInDir((byte)2, k2);
					} else {
						int k2 = stream.readBits(3);
						player.moveInDir((byte)0, k2);
					}
					int l2 = stream.readBits(1);
					if (l2 == 1)
						anIntArray894[anInt893++] = i1;
				} else if (k1 == 3)
					anIntArray840[anInt839++] = i1;
			}
		}
	}

	private Map<String, Sprite> screenImages;

	private final void createScreenImages() {
		if (screenImages != null) {
			return;
		}
		screenImages = new HashMap<>();
		screenImages.put("background", new Sprite("Login/background")); // not used
	}

	public Sprite loginAsset0;
	public Sprite loginAsset1;
	public Sprite loginAsset2;
	public Sprite loginAsset3;
	public Sprite loginAsset4;
	private String firstLoginMessage = "";
	Image icon = null;
	String loginscreen_welcome_message_header = "Welcome to " + Configuration.CLIENT_TITLE;
	String loginscreen_message_footer = "Footer here";

	public boolean rememberMeHover;
	long lastLogin = 0;
	long loadDelay = 0;
	private Sprite loginScreenBackground;
	private Sprite logo2021;


	public void drawLoginScreen(boolean flag) {
	    int centerX = myWidth / 2, centerY = myHeight / 2;
	    resetImageProducers();
	    loginScreenGraphicsBuffer.initDrawingArea();

	    if (loginVideo != null) {
	        BufferedImage frame = loginVideo.getNextFrame();
	        if (frame != null) {
	            int frameWidth = frame.getWidth();
	            int frameHeight = frame.getHeight();

	            BufferedImage scaledFrame = frame;
	            if (frameWidth != myWidth || frameHeight != myHeight) {
	                scaledFrame = new BufferedImage(myWidth, myHeight, BufferedImage.TYPE_INT_RGB);
	                Graphics2D g2d = scaledFrame.createGraphics();
	                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	                g2d.drawImage(frame, 0, 0, myWidth, myHeight, null);
	                g2d.dispose();
	            }

	            scaledFrame.getRGB(0, 0, myWidth, myHeight, DrawingArea.pixels, 0, myWidth);
	        }
	    } else {
	        loginScreenBackground.drawAdvancedSprite(0,0);
	    }

	 // Draw the login box overlay on top of video
	    if (loginBoxOverlay != null) {
	        loginBoxOverlay.drawAdvancedSprite(0, 0);
	    } else {
	        System.out.println("loginBoxOverlay is NULL!");
	    }
	    
	    if (Configuration.developerMode) {
	        newSmallFont.drawString("FPS: " + super.getFps(), 4, 12, Integer.MAX_VALUE, 0, 255);
	        newSmallFont.drawString("Mouse: [" + super.getMouseX() + ", " + super.getMouseY() + "] [" + super.getRawMouseX() + ", " + super.getRawMouseY() + "]", 4, 12 + 12, Integer.MAX_VALUE, 0, 255);
	    }

	    int j = centerY - 40;

	    if (firstLoginMessage.length() > 0) {
	        newBoldFont.drawCenteredString(firstLoginMessage, centerX - 3, j - 11, 0xffffff, 0x191919, 255);
	        j += 30;
	    }

	    newBoldFont.drawString(
	        "Login: " + myUsername + ((loginScreenCursorPos == 0) & (update_tick % 40 < 20) ? "|" : ""),
	        (myWidth / 2) - 119, myHeight / 2 - 21, 0xffffff, 0x191919, 255);
	    j += 15;
	    newBoldFont.drawString(
	        "Password: " + StringUtils.passwordAsterisks(getPassword())
	        + ((loginScreenCursorPos == 1) & (update_tick % 40 < 20) ? "|" : ""),
	        (myWidth / 2) - 119, myHeight / 2 + 31, 0xffffff, 0x191919, 255);

	    int rememberYOffset = 6;

	    int extraPos = 18;
	    rememberMeHover = mouseInRegion(286 - extraPos, 300, 301 - extraPos, 313);
	    rememberPasswordHover = mouseInRegion(416 - extraPos, 300, 433 - extraPos, 313);

	    newSmallFont.drawString("Remember username", 283, 308 + rememberYOffset, 0xffffff, 0x191919, 255);
	    newSmallFont.drawString("Remember password", 414, 308 + rememberYOffset, 0xffffff, 0x191919, 255);

	    if (!informationFile.isUsernameRemembered()) {
	        if (!rememberMeHover) {
	            loginAsset0.drawSprite(264, 295 + rememberYOffset);
	        } else {
	            loginAsset1.drawSprite(264, 295 + rememberYOffset);
	        }
	    } else {
	        if (!rememberMeHover) {
	            loginAsset2.drawSprite(264, 295 + rememberYOffset);
	        } else {
	            loginAsset3.drawSprite(264, 295 + rememberYOffset);
	        }
	    }

	    if (!informationFile.isPasswordRemembered()) {
	        if (!rememberPasswordHover) {
	            loginAsset0.drawSprite(395, 295 + rememberYOffset);
	        } else {
	            loginAsset1.drawSprite(395, 295 + rememberYOffset);
	        }
	    } else {
	        if (!rememberPasswordHover) {
	            loginAsset2.drawSprite(395, 295 + rememberYOffset);
	        } else {
	            loginAsset3.drawSprite(395, 295 + rememberYOffset);
	        }
	    }

	    loginAsset4.drawARGBSprite2(currentGameWidth / 2 - (336 / 2), 25 + rememberYOffset);

	    // Mute button
	    int muteButtonX = myWidth - 75;
	    int muteButtonY = 440;

	    muteButtonHover = mouseInRegion(muteButtonX, muteButtonY,
	                                     muteButtonX + 30, muteButtonY + 30);

	    if (!loginMusicMuted) {
	        if (!muteButtonHover) {
	            muteButtonOff.drawSprite(muteButtonX, muteButtonY);
	        } else {
	            muteButtonOff.drawTransparentSprite(muteButtonX, muteButtonY, 200);
	        }
	    } else {
	        if (!muteButtonHover) {
	            muteButtonOn.drawSprite(muteButtonX, muteButtonY);
	        } else {
	            muteButtonOn.drawTransparentSprite(muteButtonX, muteButtonY, 200);
	        }
	    }

	    loginScreenGraphicsBuffer.drawGraphics(0, 0, super.graphics);
	}
	private int blendPixels(int background, int foreground, int alpha) {
	    int invAlpha = 255 - alpha;
	    int r = ((background >> 16) & 0xFF) * invAlpha + ((foreground >> 16) & 0xFF) * alpha >> 8;
	    int g = ((background >> 8) & 0xFF) * invAlpha + ((foreground >> 8) & 0xFF) * alpha >> 8;
	    int b = (background & 0xFF) * invAlpha + (foreground & 0xFF) * alpha >> 8;
	    return (r << 16) | (g << 8) | b;
	}
	private void drawFlames() {
		try {
			long l = System.currentTimeMillis();
			int i = 0;
			int j = 20;
			while (aBoolean831) {
				anInt1208++;
				if (++i > 10) {
					long l1 = System.currentTimeMillis();
					int k = (int) (l1 - l) / 10 - j;
					j = 40 - k;
					if (j < 5)
						j = 5;
					i = 0;
					l = l1;
				}
				try {
					Thread.sleep(j);
				} catch (Exception _ex) {
				}
			}
		} catch (Exception _ex) {
			drawingFlames = false;
		}
	}

	@Override
	public void raiseWelcomeScreen() {
		welcomeScreenRaised = true;
	}

	public void method137(Buffer stream, int j) {
		if (j == 84) {
			int k = stream.readUnsignedByte();
			int j3 = anInt1268 + (k >> 4 & 7);
			int i6 = anInt1269 + (k & 7);
			int l8 = stream.readUShort();
			int k11 = stream.readUShort();
			int l13 = stream.readUShort();// edit
			if (j3 >= 0 && i6 >= 0 && j3 < 104 && i6 < 104) {
				NodeList class19_1 = groundItems[plane][j3][i6];
				if (class19_1 != null) {
					for (Item class30_sub2_sub4_sub2_3 = (Item) class19_1
							.reverseGetFirst(); class30_sub2_sub4_sub2_3 != null; class30_sub2_sub4_sub2_3 = (Item) class19_1
							.reverseGetNext()) {
						if (class30_sub2_sub4_sub2_3.ID != (l8 & 0x7fff) || class30_sub2_sub4_sub2_3.anInt1559 != k11)
							continue;
						class30_sub2_sub4_sub2_3.anInt1559 = l13;
						break;
					}

					spawnGroundItem(j3, i6);
				}
			}
			return;
		}
		if (j == 105) {
			int l = stream.readUnsignedByte();
			int k3 = anInt1268 + (l >> 4 & 7);
			int j6 = anInt1269 + (l & 7);
			int i9 = stream.readUShort();
			int l11 = stream.readUnsignedByte();
			int i14 = l11 >> 4 & 0xf;
			int i16 = l11 & 7;
			if (myPlayer.waypoint_x[0] >= k3 - i14 && myPlayer.waypoint_x[0] <= k3 + i14 && myPlayer.waypoint_y[0] >= j6 - i14
					&& myPlayer.waypoint_y[0] <= j6 + i14 && soundEffectVolume != 0 && aBoolean848 && !Client.lowMem
					&& currentSound < 50) {
				sound[currentSound] = i9;
				soundType[currentSound] = i16;
				soundDelay[currentSound] = Sounds.anIntArray326[i9];
				// aClass26Array1468[soundCount] = null;
				currentSound++;
			}
		}
		if (j == 215) {
			int i1 = stream.readUShortA();
			int l3 = stream.method428();
			int k6 = anInt1268 + (l3 >> 4 & 7);
			int j9 = anInt1269 + (l3 & 7);
			int i12 = stream.readUShortA();
			int j14 = stream.readUShort();
			if (k6 >= 0 && j9 >= 0 && k6 < 104 && j9 < 104 && i12 != unknownInt10) {
				Item class30_sub2_sub4_sub2_2 = new Item();
				class30_sub2_sub4_sub2_2.ID = i1;
				class30_sub2_sub4_sub2_2.anInt1559 = j14;
				if (groundItems[plane][k6][j9] == null)
					groundItems[plane][k6][j9] = new NodeList();
				groundItems[plane][k6][j9].insertHead(class30_sub2_sub4_sub2_2);
				spawnGroundItem(k6, j9);
			}
			return;
		}
		if (j == 156) {
			int j1 = stream.method426();
			int i4 = anInt1268 + (j1 >> 4 & 7);
			int l6 = anInt1269 + (j1 & 7);
			int k9 = stream.readUShort();
			if (i4 >= 0 && l6 >= 0 && i4 < 104 && l6 < 104) {
				NodeList class19 = groundItems[plane][i4][l6];
				if (class19 != null) {
					for (Item item = (Item) class19.reverseGetFirst(); item != null; item = (Item) class19
							.reverseGetNext()) {
						if (item.ID != k9)
							continue;
						item.unlink();
						break;
					}

					if (class19.reverseGetFirst() == null)
						groundItems[plane][i4][l6] = null;
					spawnGroundItem(i4, l6);
				}
			}
			return;
		}
		if (j == 160) {
			int k1 = stream.method428();
			int j4 = anInt1268 + (k1 >> 4 & 7);
			int i7 = anInt1269 + (k1 & 7);
			int l9 = stream.method428();
			int objectType = l9 >> 2;
			int orientation = l9 & 3;
			int objectTypeGroup = objectTypes[objectType];
			int animationId = stream.readUShortA();
			if (j4 >= 0 && i7 >= 0 && j4 < 103 && i7 < 103) {
				int j18 = intGroundArray[plane][j4][i7];
				int i19 = intGroundArray[plane][j4 + 1][i7];
				int l19 = intGroundArray[plane][j4 + 1][i7 + 1];
				int k20 = intGroundArray[plane][j4][i7 + 1];
				if (objectTypeGroup == 0) {
					Object1 class10 = scene.method296(plane, j4, i7);
					if (class10 != null) {
						int k21 = ObjectKey.getObjectId(class10.uid);
						if (objectType == 2) {
							class10.aClass30_Sub2_Sub4_278 = new Animable_Sub5(k21, 4 + orientation, 2, i19, l19, j18, k20, animationId,
									false);
							class10.aClass30_Sub2_Sub4_279 = new Animable_Sub5(k21, orientation + 1 & 3, 2, i19, l19, j18, k20,
									animationId, false);
						} else {
							class10.aClass30_Sub2_Sub4_278 = new Animable_Sub5(k21, orientation, objectType, i19, l19, j18, k20, animationId,
									false);
						}
					}
				}
				if (objectTypeGroup == 1) {
					Object2 class26 = scene.method297(j4, i7, plane);
					if (class26 != null)
						class26.aClass30_Sub2_Sub4_504 = new Animable_Sub5(ObjectKey.getObjectId(class26.uid), 0, 4, i19, l19,
								j18, k20, animationId, false);
				}
				if (objectTypeGroup == 2) {
					StaticObject class28 = scene.method298(j4, i7, plane);
					if (objectType == 11)
						objectType = 10;
					if (class28 != null) {
						class28.aClass30_Sub2_Sub4_521 = new Animable_Sub5(ObjectKey.getObjectId(class28.uid), orientation, objectType, i19,
								l19, j18, k20, animationId, false);
					}
				}
				if (objectTypeGroup == 3) {
					Object3 class49 = scene.method299(i7, j4, plane);
					if (class49 != null)
						class49.aClass30_Sub2_Sub4_814 = new Animable_Sub5(ObjectKey.getObjectId(class49.uid), orientation, 22, i19,
								l19, j18, k20, animationId, false);
				}
			}
			return;
		}
		if (j == 147) {
			int l1 = stream.method428();
			int k4 = anInt1268 + (l1 >> 4 & 7);
			int j7 = anInt1269 + (l1 & 7);
			int i10 = stream.readUShort();
			byte byte0 = stream.method430();
			int l14 = stream.method434();
			byte byte1 = stream.method429();
			int k17 = stream.readUShort();
			int k18 = stream.method428();
			int j19 = k18 >> 2;
			int i20 = k18 & 3;
			int l20 = objectTypes[j19];
			byte byte2 = stream.readSignedByte();
			int l21 = stream.readUShort();
			byte byte3 = stream.method429();
			Player player;
			if (i10 == unknownInt10)
				player = myPlayer;
			else
				player = players[i10];
			if (player != null) {
				ObjectDefinition class46 = ObjectDefinition.forID(l21);
				int i22 = intGroundArray[plane][k4][j7];
				int j22 = intGroundArray[plane][k4 + 1][j7];
				int k22 = intGroundArray[plane][k4 + 1][j7 + 1];
				int l22 = intGroundArray[plane][k4][j7 + 1];
				Model model = class46.modelAt(j19, i20, i22, j22, k22, l22, -1, null);
				if (model != null) {
					method130(k17 + 1, -1, 0, l20, j7, 0, plane, k4, l14 + 1);
					player.anInt1707 = l14 + update_tick;
					player.anInt1708 = k17 + update_tick;
					player.aModel_1714 = model;
					int i23 = class46.xLength;
					int j23 = class46.yLength;
					if (i20 == 1 || i20 == 3) {
						i23 = class46.yLength;
						j23 = class46.xLength;
					}
					player.anInt1711 = k4 * 128 + i23 * 64;
					player.anInt1713 = j7 * 128 + j23 * 64;
					player.anInt1712 = getCenterHeight(plane, player.anInt1713, player.anInt1711);
					if (byte2 > byte0) {
						byte byte4 = byte2;
						byte2 = byte0;
						byte0 = byte4;
					}
					if (byte3 > byte1) {
						byte byte5 = byte3;
						byte3 = byte1;
						byte1 = byte5;
					}
					player.anInt1719 = k4 + byte2;
					player.anInt1721 = k4 + byte0;
					player.anInt1720 = j7 + byte3;
					player.anInt1722 = j7 + byte1;
				}
			}
		}
		if (j == 151) {
			int i2 = stream.method426();
			int l4 = anInt1268 + (i2 >> 4 & 7);
			int k7 = anInt1269 + (i2 & 7);
			int j10 = stream.method434();
			int k12 = stream.method428();
			int i15 = k12 >> 2;
			int k16 = k12 & 3;
			int l17 = objectTypes[i15];
			if (l4 >= 0 && k7 >= 0 && l4 < 104 && k7 < 104)
				method130(-1, j10, k16, l17, k7, i15, plane, l4, 0);
			return;
		}
		if (j == 4) {
			int j2 = stream.readUnsignedByte();
			int i5 = anInt1268 + (j2 >> 4 & 7);
			int l7 = anInt1269 + (j2 & 7);
			int k10 = stream.readUShort();
			int l12 = stream.readUnsignedByte();
			int j15 = stream.readUShort();
			if (i5 >= 0 && l7 >= 0 && i5 < 104 && l7 < 104) {
				i5 = i5 * 128 + 64;
				l7 = l7 * 128 + 64;
				Animable_Sub3 class30_sub2_sub4_sub3 = new Animable_Sub3(plane, update_tick, j15, k10,
						getCenterHeight(plane, l7, i5) - l12, l7, i5);
				aClass19_1056.insertHead(class30_sub2_sub4_sub3);
			}
			return;
		}
		if (j == 44) {
		    int k2 = stream.method436(); // item ID
		    int j5 = stream.method439(); // item amount  
		    int i8 = stream.readUnsignedByte();
		    int l10 = anInt1268 + (i8 >> 4 & 7);
		    int i13 = anInt1269 + (i8 & 7);
		    
		    if (l10 >= 0 && i13 >= 0 && l10 < 104 && i13 < 104) {
		        Item class30_sub2_sub4_sub2_1 = new Item();
		        class30_sub2_sub4_sub2_1.ID = k2;
		        class30_sub2_sub4_sub2_1.anInt1559 = j5;
		        
		        if (groundItems[plane][l10][i13] == null)
		            groundItems[plane][l10][i13] = new NodeList();
		        groundItems[plane][l10][i13].insertHead(class30_sub2_sub4_sub2_1);
		        spawnGroundItem(l10, i13);
		        
		        // Track drop for loot tracker
		        trackGroundItemDrop(k2, j5, l10, i13);
		    }
		    return;
		}
		if (j == 101) {
			int l2 = stream.method427();
			int k5 = l2 >> 2;
			int j8 = l2 & 3;
			int i11 = objectTypes[k5];
			int j13 = stream.readUnsignedByte();
			int k15 = anInt1268 + (j13 >> 4 & 7);
			int l16 = anInt1269 + (j13 & 7);
			if (k15 >= 0 && l16 >= 0 && k15 < 104 && l16 < 104)
				method130(-1, -1, j8, i11, l16, k5, plane, k15, 0);
			return;
		}
		if (j == 117) {
			int i3 = stream.readUnsignedByte();
			int l5 = anInt1268 + (i3 >> 4 & 7);
			int k8 = anInt1269 + (i3 & 7);
			int j11 = l5 + stream.readSignedByte();
			int k13 = k8 + stream.readSignedByte();
			int l15 = stream.readSignedWord();
			int i17 = stream.readUShort();
			int i18 = stream.readUnsignedByte() * 4;
			int l18 = stream.readUnsignedByte() * 4;
			int k19 = stream.readUShort();
			int j20 = stream.readUShort();
			int i21 = stream.readUnsignedByte();
			int j21 = stream.readUnsignedByte();
			if (l5 >= 0 && k8 >= 0 && l5 < 104 && k8 < 104 && j11 >= 0 && k13 >= 0 && j11 < 104 && k13 < 104
					&& i17 != 65535) {
				l5 = l5 * 128 + 64;
				k8 = k8 * 128 + 64;
				j11 = j11 * 128 + 64;
				k13 = k13 * 128 + 64;
				Animable_Sub4 class30_sub2_sub4_sub4 = new Animable_Sub4(i21, l18, k19 + update_tick, j20 + update_tick,
						j21, plane, getCenterHeight(plane, k8, l5) - i18, k8, l5, l15, i17);
				class30_sub2_sub4_sub4.method455(k19 + update_tick, k13, getCenterHeight(plane, k13, j11) - l18, j11);
				aClass19_1013.insertHead(class30_sub2_sub4_sub4);
			}
		}
	}
	private void trackGroundItemDrop(int itemId, int quantity, int x, int y) {
	    try {
	        if (myPlayer == null) return;
	        
	        int playerX = myPlayer.x / 128;
	        int playerY = myPlayer.y / 128;
	        int distance = Math.abs(playerX - x) + Math.abs(playerY - y);
	        
	        if (distance > 15) return;
	        
	        String source = "Ground loot";
	        
	        // DEBUG
	        System.out.println("=== DROP DEBUG ===");
	        System.out.println("lastAttackedNPC: " + lastAttackedNPC);
	        System.out.println("Time since attack: " + (System.currentTimeMillis() - lastNPCAttackTime) + "ms");
	        
	        if (lastAttackedNPC != null) {
	            System.out.println("NPC desc: " + lastAttackedNPC.desc);
	            if (lastAttackedNPC.desc != null) {
	                System.out.println("NPC name: " + lastAttackedNPC.desc.name);
	            }
	        }
	        
	        if (lastAttackedNPC != null && 
	            System.currentTimeMillis() - lastNPCAttackTime < 10000) {
	            if (lastAttackedNPC.desc != null && lastAttackedNPC.desc.name != null) {
	                source = lastAttackedNPC.desc.name;
	                System.out.println("Source set to: " + source);
	            }
	        }
	        
	        ItemDefinition itemDef = ItemDefinition.forID(itemId);
	        long itemValue = itemDef != null ? itemDef.value : 0;
	        
	        if (ClientWindow.frame instanceof CustomGameFrame) {
	            CustomGameFrame customFrame = (CustomGameFrame) ClientWindow.frame;
	            LootTrackerPanel tracker = customFrame.getLootTracker();
	            if (tracker != null) {
	                tracker.recordDrop(source, itemId, quantity, itemValue);
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error tracking drop: " + e.getMessage());
	        e.printStackTrace();
	    }
	}
	public void method139(Buffer stream) {
		try {
			stream.initBitAccess();
			int localNpcListSize = stream.readBits(8);
			if (localNpcListSize < npcCount) {
				for (int l = localNpcListSize; l < npcCount; l++)
					anIntArray840[anInt839++] = npcIndices[l];

			}
			if (localNpcListSize > npcCount) {
				Signlink.reporterror(myUsername + " Too many npcs");
				throw new RuntimeException("eek");
			}
			npcCount = 0;
			for (int i1 = 0; i1 < localNpcListSize; i1++) {
				int npcIndex = npcIndices[i1];
				NPC npc = npcs[npcIndex];
				int k1 = stream.readBits(1);
				if (k1 == 0) {
					npcIndices[npcCount++] = npcIndex;
					npc.anInt1537 = update_tick;
				} else {
					int l1 = stream.readBits(2);
					if (l1 == 0) {
						npcIndices[npcCount++] = npcIndex;
						npc.anInt1537 = update_tick;
						anIntArray894[anInt893++] = npcIndex;
					} else if (l1 == 1) {
						npcIndices[npcCount++] = npcIndex;
						npc.anInt1537 = update_tick;
						int i2 = stream.readBits(3);
						npc.moveInDir((byte)1, i2);
						int k2 = stream.readBits(1);
						if (k2 == 1)
							anIntArray894[anInt893++] = npcIndex;
					} else if (l1 == 2) {
						npcIndices[npcCount++] = npcIndex;
						npc.anInt1537 = update_tick;
						int dir;
						if (stream.readBits(1) == 1) {
							dir = stream.readBits(3);
						npc.moveInDir((byte)2, dir);
							dir = stream.readBits(3);
						npc.moveInDir((byte)2, dir);
						} else {
							 dir = stream.readBits(3);
							npc.moveInDir((byte)0, dir);
						}
						int i3 = stream.readBits(1);
						if (i3 == 1)
							anIntArray894[anInt893++] = npcIndex;
					} else if (l1 == 3)
						anIntArray840[anInt839++] = npcIndex;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean clickInRegion(int x1, int y1, int x2, int y2) {
		if (super.getSaveClickX() >= x1 && super.getSaveClickX() <= x2 && super.getSaveClickY() >= y1 && super.getSaveClickY() <= y2) {
			return true;
		}
		return false;
	}

	public boolean mouseInRegion(int x1, int y1, int x2, int y2) {
		if (super.getMouseX() >= x1 && super.getMouseX() <= x2 && super.getMouseY() >= y1 && super.getMouseY() <= y2) {
			return true;
		}
		return false;
	}

	public boolean rememberPasswordHover;

	@SuppressWarnings("static-access")
	private void processLoginScreenInput() {
	    int j = super.myHeight / 2 - 40;
	    j += 30;
	    j += 25;
	    if (super.clickMode3 == 1 && super.getSaveClickX() >= 272 && super.getSaveClickX() <= 525 && super.getSaveClickY() >= 210
	        && super.getSaveClickY() <= 234)
	        loginScreenCursorPos = 0;
	    j += 15;
	    if (super.clickMode3 == 1 && super.getSaveClickX() >= 272 && super.getSaveClickX() <= 525 && super.getSaveClickY() >= 264
	        && super.getSaveClickY() <= 294)
	        loginScreenCursorPos = 1;
	    j += 15;
	    int extraPos = 18;
	    // System.out.println("saveClickX = " + super.saveClickX + ", saveClickY = " +
	    // super.saveClickY);

	    if (super.clickMode3 == 1 && super.getSaveClickY() >= 349 && super.getSaveClickY() <= 359 && super.getSaveClickX() >= 288
	        && super.getSaveClickX() <= 475) {
	        try {
	            // **TODO** launch forgot pw url
	            // MiscUtils.launchURL(Configuration.forgotPasswordURL);
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }

	    if (super.clickMode3 == 1 && super.getSaveClickY() >= 300 && super.getSaveClickY() <= 313
	        && super.getSaveClickX() >= 286 - extraPos && super.getSaveClickX() <= 301 - extraPos) {
	        informationFile.setUsernameRemembered(!informationFile.isUsernameRemembered());
	    }
	    if (super.clickMode3 == 1 && super.getSaveClickY() >= 300 && super.getSaveClickY() <= 313
	        && super.getSaveClickX() >= 416 - extraPos && super.getSaveClickX() <= 433 - extraPos) {
	        informationFile.setPasswordRemembered(!informationFile.isPasswordRemembered());
	    }
	 // ADD THIS CODE HERE - Mute button click handler
	    if (super.clickMode3 == 1 && muteButtonHover) {
	        loginMusicMuted = !loginMusicMuted;
	        musicSettings.setLoginMusicMuted(loginMusicMuted);
	        musicSettings.write();

	        if (loginMusicMuted) {
	            stopMidi();
	            this.nextSong = -1;
	        } else {
	            this.nextSong = 54;
	            this.songChanging = true;
	            onDemandFetcher.provide(2, this.nextSong);
	        }
	    }
	    // END OF MUTE BUTTON CODE
	    int i1 = super.myWidth / 2 - 80;
	    int k1 = super.myHeight / 2 + 50;
	    k1 += 20;

	    if (super.clickMode3 == 1 && super.getSaveClickX() >= 244 && super.getSaveClickX() <= 525 && super.getSaveClickY() >= 323
	        && super.getSaveClickY() <= 353) {

	        // loginScreenState = 3;
	        loginFailures = 0;
	        login(myUsername, getPassword(), false);
	        if (loggedIn)
	            return;

	        // loginScreenState = 2;
	    }
	    i1 = super.myWidth / 2 + 80;
	    if (super.clickMode3 == 1 && super.getSaveClickX() >= i1 - 75 && super.getSaveClickX() <= i1 + 75
	        && super.getSaveClickY() >= k1 - 20 && super.getSaveClickY() <= k1 + 20) {
	        loginScreenState = LoginScreenState.LOGIN;
	        // myUsername = "";
	        // myPassword = "";
	    }
	    do {
	        int l1 = readChar(-796);
	        if (l1 == -1)
	            break;
	        boolean flag1 = false;
	        for (int i2 = 0; i2 < validUserPassChars.length(); i2++) {
	            if (l1 != validUserPassChars.charAt(i2))
	                continue;
	            flag1 = true;
	            break;
	        }

	        if (loginScreenCursorPos == 0) {
	            myUsername = loginScreenInput(myUsername, l1, flag1, 12,
	                () -> loginScreenCursorPos = 1,
	                () -> loginScreenCursorPos = 1
	            );
	        } else if (loginScreenCursorPos == 1) {
	            myPassword = loginScreenInput(myPassword, l1, flag1, 15,
	                () -> loginScreenCursorPos = 0,
	                () -> login(myUsername, getPassword(), false)
	            );
	        }
	    } while (true);
	}
	
	private String loginScreenInput(String current, int l1, boolean flag1, int maxWidth, Runnable tab, Runnable enter) {
		if (l1 == 8 && current.length() > 0)
			current = current.substring(0, current.length() - 1);
		if (l1 == 9) {
			if (tab != null)
				tab.run();
		} else if (l1 == 10 || l1 == 13) {
			if (enter != null)
				enter.run();
			return current;
		}
		if (flag1)
			current += (char) l1;
		if (current.length() > maxWidth)
			current = current.substring(0, maxWidth);
		return current;
	}

	private void markMinimap(Sprite sprite, int xPosition, int yPosition) {
		int k = viewRotation + minimapRotation & 0x7ff;
		int boundry = xPosition * xPosition + yPosition * yPosition;
		if (boundry > 5750)
			return;
		int i1 = Model.SINE[k];
		int j1 = Model.COSINE[k];
		i1 = (i1 * 256) / (minimapZoom + 256);
		j1 = (j1 * 256) / (minimapZoom + 256);
		int k1 = yPosition * i1 + xPosition * j1 >> 16;
		int l1 = yPosition * j1 - xPosition * i1 >> 16;

		try {
			if (currentScreenMode == ScreenMode.FIXED)
				sprite.drawSprite(((97 + k1) - sprite.maxWidth / 2) + 30, 83 - l1 - sprite.maxHeight / 2 - 4 + 5);
			else
				sprite.drawSprite(((77 + k1) - sprite.maxWidth / 2) + 4 + (currentGameWidth - 167),
						85 - l1 - sprite.maxHeight / 2 - 4);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	public void method142(int i, int j, int k, int l, int i1, int j1, int k1) {
		if (i1 >= 1 && i >= 1 && i1 <= 102 && i <= 102) {
			if (lowMem && j != plane)
				return;
			long i2 = 0;
			if (j1 == 0)
				i2 = scene.method300(j, i1, i);
			if (j1 == 1)
				i2 = scene.method301(j, i1, i);
			if (j1 == 2)
				i2 = scene.method302(j, i1, i);
			if (j1 == 3)
				i2 = scene.method303(j, i1, i);
			if (i2 != 0) {
				int i3 = scene.method304(j, i1, i, i2);
				int j2 = ObjectKey.getObjectId(i2);
				int k2 = i3 & 0x1f;
				int l2 = i3 >> 6;
				if (j1 == 0) {
					scene.method291(i1, j, i, (byte) -119);
					ObjectDefinition class46 = ObjectDefinition.forID(j2);
					if (class46.interactType != 0) {
						collisionMaps[j].method215(l2, k2, class46.aBoolean757, i1, i);
					}
				}
				if (j1 == 1)
					scene.method292(i, j, i1);
				if (j1 == 2) {
					scene.method293(j, i1, i);
					ObjectDefinition class46_1 = ObjectDefinition.forID(j2);
					if (i1 + class46_1.xLength > 103 || i + class46_1.xLength > 103 || i1 + class46_1.yLength > 103 || i + class46_1.yLength > 103) {
						return;
					}
					if (class46_1.interactType != 0) {
						collisionMaps[j].method216(l2, class46_1.xLength, i1, i, class46_1.yLength,
								class46_1.aBoolean757);
					}
				}
				if (j1 == 3) {
					scene.method294(j, i, i1);
					ObjectDefinition class46_2 = ObjectDefinition.forID(j2);
					if (class46_2.interactType == 1)
						collisionMaps[j].method218(i, i1);
				}
			}
			if (k1 >= 0) {
				int j3 = j;
				if (j3 < 3 && (byteGroundArray[1][i1][i] & 2) == 2)
					j3++;
				ObjectManager.method188(scene, k, i, l, j3, collisionMaps[j], intGroundArray, i1, k1, j);
			}
		}
	}

	public void updatePlayers(int i, Buffer stream) {
		anInt839 = 0;
		anInt893 = 0;
		method117(stream);
		method134(stream);
		method91(stream, i);
		method49(stream);
		for (int k = 0; k < anInt839; k++) {
			int l = anIntArray840[k];
			if (players[l].anInt1537 != update_tick)
				players[l] = null;
		}

		if (stream.currentOffset != i) {
			Signlink.reporterror("Error packet size mismatch in getplayer pos:" + stream.currentOffset + " psize:" + i);
			throw new RuntimeException("eek");
		}
		for (int i1 = 0; i1 < playerCount; i1++)
			if (players[playerIndices[i1]] == null) {
				Signlink.reporterror(myUsername + " null entry in pl list - pos:" + i1 + " size:" + playerCount);
				throw new RuntimeException("eek");
			}

	}

	private void setCameraPos(int j, int k, int l, int i1, int j1, int k1) {
		int l1 = 2048 - k & 0x7ff;
		int i2 = 2048 - j1 & 0x7ff;
		int j2 = 0;
		int k2 = 0;
		int l2 = j;
		if (l1 != 0) {
			int i3 = Model.SINE[l1];
			int k3 = Model.COSINE[l1];
			int i4 = k2 * k3 - (int) (l2 * i3) >> 16;
			l2 = k2 * i3 + (int) (l2 * k3) >> 16;
			k2 = i4;
		}
		if (i2 != 0) {
			int j3 = Model.SINE[i2];
			int l3 = Model.COSINE[i2];
			int j4 = (int) (l2 * j3) + j2 * l3 >> 16;
			l2 = (int) (l2 * l3) - j2 * j3 >> 16;
			j2 = j4;
		}
		xCameraPos = l - j2;
		zCameraPos = i1 - k2;
		yCameraPos = k1 - l2;
		yCameraCurve = k;
		xCameraCurve = j1;
	}

	public void updateStrings(String str, int i) {
		switch (i) {
			case 49020:
				achievementCutoff = Integer.parseInt(str);
				break;
			case 1675:
				sendFrame126(str, 17508);
				break;// Stab
			case 1676:
				sendFrame126(str, 17509);
				break;// Slash
			case 1678:
				sendFrame126(str, 17511);
				break;// Magic
			case 1679:
				sendFrame126(str, 17512);
				break;// Range
			case 1680:
				sendFrame126(str, 17513);
				break;// Stab
			case 1681:
				sendFrame126(str, 17514);
				break;// Slash
			case 1682:
				sendFrame126(str, 17515);
				break;// Crush
			case 1683:
				sendFrame126(str, 17516);
				break;// Magic
			case 1684:
				sendFrame126(str, 17517);
				break;// Range
			case 1686:
				sendFrame126(str, 17518);
				break;// Strength
			case 1687:
				sendFrame126(str, 17519);
				break;// Prayer
		}
	}

	public void sendFrame126(String str, int i) {
		RSInterface component = RSInterface.interfaceCache[i];
		if (component != null) {
			component.message = str;
			if (component.type == 4 && component.atActionType == 1) {
				component.hoverText = str;
			}
			if (component.parentID == tabInterfaceIDs[tabID])
				needDrawTabArea = true;
		}
	}

	public void sendFrame219() {
		if (invOverlayInterfaceID != -1) {
			invOverlayInterfaceID = -1;
			needDrawTabArea = true;
			tabAreaAltered = true;
		}
		if (backDialogID != -1) {
			backDialogID = -1;
			inputTaken = true;
		}
		if (inputDialogState != 0) {
			inputDialogState = 0;
			inputTaken = true;
		}
		openInterfaceID = -1;
		aBoolean1149 = false;
	}

	public void sendKeyboardAction(int action) {
		stream.createFrame(201);
		stream.writeByte(action);
	}

	public void sendFrame248(int interfaceID, int sideInterfaceID) {
		if (backDialogID != -1) {
			backDialogID = -1;
			inputTaken = true;
		}
		if (inputDialogState != 0) {
			inputDialogState = 0;
			inputTaken = true;
		}
		openInterfaceID = interfaceID;
		invOverlayInterfaceID = sideInterfaceID;
		needDrawTabArea = true;
		tabAreaAltered = true;
		aBoolean1149 = false;
	}

	private int groundItemValueLimit = 7_000;

	private static final Comparator<ExperienceDrop> HIGHEST_POSITION = new Comparator<ExperienceDrop>() {

		@Override
		public int compare(ExperienceDrop o1, ExperienceDrop o2) {
			return Integer.compare(o2.getYPosition(), o1.getYPosition());
		}

	};

	private boolean parsePacket() {
		if (socketStream == null)
			return false;
		try {
			int i = socketStream.available();
			if (i == 0)
				return false;
			if (incomingPacket == -1) {
				socketStream.flushInputStream(inStream.buffer, 1);
				incomingPacket = inStream.buffer[0] & 0xff;
				if (encryption != null)
					incomingPacket = incomingPacket - encryption.getNextKey() & 0xff;
				packetSize = SizeConstants.packetSizes[incomingPacket];
				i--;
			}
			if (packetSize == -1)
				if (i > 0) {
					socketStream.flushInputStream(inStream.buffer, 1);
					packetSize = inStream.buffer[0] & 0xff;
					i--;
				} else {
					return false;
				}
			if (packetSize == -2)
				if (i > 1) {
					socketStream.flushInputStream(inStream.buffer, 2);
					inStream.currentOffset = 0;
					packetSize = inStream.readUShort();
					i -= 2;
				} else {
					return false;
				}
			if (i < packetSize) {
//				String message = "";
//				message = String.format("Available=%d less than packet-size=%d, ", i, packetSize);
//				message += " - Packet: " + incomingPacket + ", Packet Size: " + packetSize
//						+ " - Previous packet: " + previousPacket1 + " Previous packet size: " + previousPacketSize1
//						+ ", 2nd Previous packet: " + previousPacket2 + ", 2nd Previous packet size: "
//						+ previousPacketSize2;
//				System.err.println(message);
				return false;
			}

			inStream.currentOffset = 0;
			socketStream.flushInputStream(inStream.buffer, packetSize);
			anInt1009 = 0;

			previousPacketSize2 = previousPacketSize1;
			previousPacket2 = previousPacket1;

			previousPacketSize1 = dealtWithPacketSize;
			previousPacket1 = dealtWithPacket;

			dealtWithPacket = incomingPacket;
			dealtWithPacketSize = packetSize;

			PacketLog.add(incomingPacket, packetSize, inStream.buffer);

			switch (incomingPacket) {
				case 13: // Client script
					String types = inStream.readString();
					Object[] arguments = new Object[types.length()];

					for (int index = 0; index < types.length(); index++) {
						if (types.charAt(index) == 's') {
							arguments[index] = inStream.readNullTerminatedString();
						} else { // 'i'
							arguments[index] = inStream.readDWord();
						}
					}

					int scriptId = inStream.readUShort();
					ClientScripts.execute(scriptId, arguments);
					incomingPacket = -1;
					return true;
				case 12:
				    int soundId = inStream.readUShort();
				    int incomingSoundType = inStream.readUnsignedByte();
				    int entitySoundSource = inStream.readUShort();
				    if (entitySoundSource == 0) {
				        instance.onDemandFetcher.provide(6, soundId);
				        
				        // Use actual volume setting instead of hardcoded 7
				        SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - SoundPlayer.getVolume()), 0));
				    } else {
				        Entity entity;
				        if (entitySoundSource >= Short.MAX_VALUE) {
				            entitySoundSource -= Short.MAX_VALUE;
				            entity = players[entitySoundSource];
				        } else {
				            entity = npcs[entitySoundSource];
				        }

				        if (entity != null) {
				            // Use actual volume setting instead of hardcoded 7
				            SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - SoundPlayer.getVolume()), 0));
				        } else {
				            // Use actual volume setting instead of hardcoded 7
				            SoundRequestQueue.add(new SoundRequest(soundId, Math.max(1, 10 - SoundPlayer.getVolume()), 0));
				        }
				    }
				    incomingPacket = -1;
				    return true;
				case 10:
					screenFlashDrawing = true;
					screenFlashOpacityDownward = false;
					screenFlashOpacity = 0d;
					screenFlashColor = inStream.readDWord();
					screenFlashMaxIntensity = inStream.readUnsignedByte();
					screenFlashAutoFadeOut = inStream.readUnsignedByte() == 0;
					incomingPacket = -1;
					return true;

				// Set strings inside string container
				case 5:
					int stringContainerId = inStream.readUShort();
					int strings = inStream.readUShort();
					RSInterface stringContainer = RSInterface.get(stringContainerId);
					Preconditions.checkState(stringContainer != null && stringContainer.stringContainer != null, "No string container at id: " + stringContainer);
					stringContainer.stringContainer.clear();
					for (int index = 0; index < strings; index++) {
						stringContainer.stringContainer.add(inStream.readString());
					}
					EventCalendar.getCalendar().onStringContainerUpdated(stringContainerId);
					incomingPacket = -1;
					return true;

				// Reset scroll position
				case 2:
					int resetScrollInterfaceId = inStream.readUShort();
					RSInterface.interfaceCache[resetScrollInterfaceId].scrollPosition = 0;
					incomingPacket = -1;
					return true;

				case 3:
					int setScrollMaxInterfaceId = inStream.readUShort();
					int scrollMax = inStream.readUShort();
					RSInterface.interfaceCache[setScrollMaxInterfaceId].scrollMax = scrollMax;
					incomingPacket = -1;
					return true;


				/**
				 * Progress Bar Update Packet
				 */
				case 77:
					while (inStream.currentOffset < packetSize) {
						int interfaceId = inStream.readDWord();
						byte progressBarState = inStream.readSignedByte();
						byte progressBarPercentage = inStream.readSignedByte();

						RSInterface rsInterface = RSInterface.interfaceCache[interfaceId];
						rsInterface.progressBarState = progressBarState;
						rsInterface.progressBarPercentage = progressBarPercentage;
					}
					incomingPacket = -1;
					return true;

				case 137:
					specialAttack = inStream.readUnsignedByte();
					incomingPacket = -1;
					return true;

				/**
				 * EntityTarget Update Packet
				 */
				case 222:
					byte entityState = (byte) inStream.readUnsignedByte();
					if (entityState != 0) {
						short entityIndex = (short) inStream.readUShort();
						short currentHealth = (short) inStream.readUShort();
						short maximumHealth = (short) inStream.readUShort();
						entityTarget = new EntityTarget(entityState, entityIndex, currentHealth, maximumHealth, newSmallFont);
					} else {
						if (entityTarget != null) {
							entityTarget.stop();
						}
						entityTarget = null;
					}
					incomingPacket = -1;
					return true;

				/**
				 * Timer Update Packet
				 */
				case 223:
					int timerId = inStream.readUnsignedByte();
					int secondsToAdd = inStream.readUShort();
					GameTimerHandler.getSingleton().startGameTimer(timerId, TimeUnit.SECONDS, secondsToAdd, 0);
					incomingPacket = -1;
					return true;

				case 224:
					timerId = inStream.readUShort();
					int itemId = inStream.readUShort();
					secondsToAdd = inStream.readUShort();
					GameTimerHandler.getSingleton().startGameTimer(timerId, TimeUnit.SECONDS, secondsToAdd, itemId);
					incomingPacket = -1;
					return true;

				case 11:
					long experience = inStream.readQWord();
					byte length = inStream.readSignedByte();
					int[] skills = new int[length];

					for (int j = 0; j < length; j++) {
						skills[j] = inStream.readSignedByte();
					}

					ExperienceDrop drop = new ExperienceDrop(experience, skills);

					if (!experienceDrops.isEmpty()) {
						List<ExperienceDrop> sorted = new ArrayList<ExperienceDrop>(experienceDrops);
						Collections.sort(sorted, HIGHEST_POSITION);
						ExperienceDrop highest = sorted.get(0);
						if (highest.getYPosition() >= ExperienceDrop.START_Y - 5) {
							drop.increasePosition(highest.getYPosition() - ExperienceDrop.START_Y + 20);
						}
					}

					experienceDrops.offer(drop);
					incomingPacket = -1;
					return true;

				case 81: // update player packet
					updatePlayers(packetSize, inStream);
					loadingMap = false;
					incomingPacket = -1;
					return true;

				case 176:
					daysSinceRecovChange = inStream.method427();
					unreadMessages = inStream.readUShortA();
					membersInt = inStream.readUnsignedByte();
					anInt1193 = inStream.method440();
					daysSinceLastLogin = inStream.readUShort();
					if (anInt1193 != 0 && openInterfaceID == -1) {
						Signlink.dnslookup(StringUtils.method586(anInt1193));
						clearTopInterfaces();
						char c = '\u028A';
						if (daysSinceRecovChange != 201 || membersInt == 1)
							c = '\u028F';
						reportAbuseInput = "";
						canMute = false;
						for (int k9 = 0; k9 < RSInterface.interfaceCache.length; k9++) {
							if (RSInterface.interfaceCache[k9] == null || RSInterface.interfaceCache[k9].contentType != c)
								continue;
							openInterfaceID = RSInterface.interfaceCache[k9].parentID;

						}
					}
					incomingPacket = -1;
					return true;

				case 64:
					anInt1268 = inStream.method427();
					anInt1269 = inStream.method428();
					for (int j = anInt1268; j < anInt1268 + 8; j++) {
						for (int l9 = anInt1269; l9 < anInt1269 + 8; l9++)
							if (groundItems[plane][j][l9] != null) {
								groundItems[plane][j][l9] = null;
								spawnGroundItem(j, l9);
							}
					}
					for (SpawnedObject spawnedObject = (SpawnedObject) spawns
							.reverseGetFirst(); spawnedObject != null; spawnedObject = (SpawnedObject) spawns
							.reverseGetNext())
						if (spawnedObject.x >= anInt1268 && spawnedObject.x < anInt1268 + 8
								&& spawnedObject.y >= anInt1269 && spawnedObject.y < anInt1269 + 8
								&& spawnedObject.plane == plane)
							spawnedObject.getLongetivity = 0;
					incomingPacket = -1;
					return true;

				case 9:
					String text = inStream.readString();
					byte state = inStream.readSignedByte();
					byte seconds = inStream.readSignedByte();
					int drawingWidth = Client.currentScreenMode == ScreenMode.FIXED ? 519 : Client.currentGameWidth;
					int drawingHeight = Client.currentScreenMode == ScreenMode.FIXED ? 338 : Client.currentGameHeight;

					incomingPacket = -1;
					return true;

				case 185:
					int k = inStream.method436();
					RSInterface.interfaceCache[k].anInt233 = 3;
					if (myPlayer.desc == null)
						RSInterface.interfaceCache[k].mediaID = (myPlayer.anIntArray1700[0] << 25)
								+ (myPlayer.anIntArray1700[4] << 20) + (myPlayer.equipment[0] << 15)
								+ (myPlayer.equipment[8] << 10) + (myPlayer.equipment[11] << 5) + myPlayer.equipment[1];
					else
						RSInterface.interfaceCache[k].mediaID = (int) (0x12345678L + myPlayer.desc.npcId);
					incomingPacket = -1;
					return true;


				case 107:
					inCutScene = false;
					for (int l = 0; l < 5; l++)
						aBooleanArray876[l] = false;
					xpCounter = 0;
					incomingPacket = -1;
					return true;

				case 72:
					int i1 = inStream.method434();
					RSInterface class9 = RSInterface.interfaceCache[i1];
					for (int k15 = 0; k15 < class9.inventoryItemId.length; k15++) {
						class9.inventoryItemId[k15] = -1;
						class9.inventoryItemId[k15] = 0;
					}
					incomingPacket = -1;
					return true;

				case 214:
					ignoreCount = packetSize / 8;
					for (int j1 = 0; j1 < ignoreCount; j1++)
						ignoreListAsLongs[j1] = inStream.readQWord();
					incomingPacket = -1;
					return true;

				case 166:
					inCutScene = true;
					x = inStream.readUnsignedByte();
					y = inStream.readUnsignedByte();
					height = inStream.readUShort();
					anInt1101 = inStream.readUnsignedByte();
					angle = inStream.readUnsignedByte();
					if (angle >= 100) {
						xCameraPos = x * 128 + 64;
						yCameraPos = y * 128 + 64;
						zCameraPos = getCenterHeight(plane, yCameraPos, xCameraPos) - height;
					}
					incomingPacket = -1;
					return true;

				case 134:
				    needDrawTabArea = true;
				    int skillId = inStream.readUnsignedByte();
				    int experience2 = inStream.method439();
				    int currentLevel = inStream.readUnsignedByte();
				    int xp = currentExp[skillId];

				    currentExp[skillId] = experience2;
				    currentStats[skillId] = currentLevel;
				    maxStats[skillId] = 1;
				    xpCounter += currentExp[skillId] - xp;
				    expAdded = currentExp[skillId] - xp;
				    
				    // ADD THIS BLOCK:
				    if (ClientWindow.frame instanceof CustomGameFrame) {
				        CustomGameFrame customFrame = (CustomGameFrame) ClientWindow.frame;
				        ExpTrackerPanel tracker = customFrame.getExpTracker();
				        if (tracker != null) {
				            tracker.updateExperience(skillId, experience2);
				        }
				    }
				    
				    for (int k20 = 0; k20 < 98; k20++)
				        if (experience2 >= SKILL_EXPERIENCE[k20])
				            maxStats[skillId] = k20 + 2;
				    updateSkillStrings(skillId);
				    incomingPacket = -1;
				    return true;
				    
				case 71:
					int l1 = inStream.readUShort();
					int j10 = inStream.method426();
					if (l1 == 65535)
						l1 = -1;
					tabInterfaceIDs[j10] = l1;
					needDrawTabArea = true;
					tabAreaAltered = true;
					incomingPacket = -1;
					return true;

				case 74:
				    int songId = inStream.method434();
				    if (songId == 65535) {
				        songId = -1;
				    }
				    if (songId != this.currentSong && this.musicEnabled && !lowMem && this.prevSong == 0) {
				        this.nextSong = songId;
				        this.songChanging = true;
				        onDemandFetcher.provide(2, this.nextSong);
				        
				        // Apply saved volume preference when new song starts
				        int savedVolume = (int) ((Preferences.getPreferences().musicVolume / 10.0) * 127);
				        Signlink.midivol = savedVolume;
				        System.out.println("Set midivol for next song: " + savedVolume);
				    }
				    currentSong = songId;
				    incomingPacket = -1;
				    return true;
				case 234:
					int jingleId = inStream.readUShort();
					if (jingleId == 65535) {
						jingleId = -1;
					}
					if (jingleId != -1) {
						nextSong = jingleId;
						onDemandFetcher.provide(5, jingleId); // index 5 for Jingles (not 3!)
					}
					incomingPacket = -1;
					return true;

				case 121:
					int j2 = inStream.method436();
					int k10 = inStream.readUShortA();
					if (musicEnabled && !lowMem) {
						nextSong = j2;
						songChanging = false;
						onDemandFetcher.provide(2, nextSong);
						prevSong = k10;
					}
					incomingPacket = -1;
					return true;

				case 7:
					int componentId = inStream.readDWord();
					byte spriteIndex = inStream.readSignedByte();
					RSInterface component = RSInterface.interfaceCache[componentId];
					if (component != null) {
						if (component.backgroundSprites != null && spriteIndex <= component.backgroundSprites.length - 1) {
							Sprite sprite = component.backgroundSprites[spriteIndex];
							if (sprite != null) {
								component.sprite1 = component.backgroundSprites[spriteIndex];
							}
						}
					}
					incomingPacket = -1;
					return true;

				case 109:
					resetLogout();
					incomingPacket = -1;
					return false;

				case 70:
					int k2 = inStream.readSignedWord();
					int l10 = inStream.method437();
					int i16 = inStream.method434();
					RSInterface class9_5 = RSInterface.interfaceCache[i16];
					class9_5.anInt263 = k2;
					class9_5.anInt265 = l10;
					incomingPacket = -1;
					return true;

				case 243:
					int fog = inStream.readUnsignedByte();
					fogEnabled = fog == 1;
					fogOpacity = inStream.readDWord();
					incomingPacket = -1;
					return true;

				case 73:
				case 241:
					int mapRegionX = currentRegionX;
					int mapRegionY = currentRegionY;
					if (incomingPacket == 73) {
						mapRegionX = inStream.readUShortA();
						mapRegionY = inStream.readUShort();
						isDynamicRegion = false;
					}
					if (incomingPacket == 241) {
						mapRegionY = inStream.readUShortA();
						inStream.initBitAccess();
						for (int z = 0; z < 4; z++) {
							for (int x = 0; x < 13; x++) {
								for (int y = 0; y < 13; y++) {
									int visible = inStream.readBits(1);
									if (visible == 1)
										constructRegionData[z][x][y] = inStream.readBits(26);
									else
										constructRegionData[z][x][y] = -1;
								}
							}
						}
						inStream.finishBitAccess();
						mapRegionX = inStream.readUShort();
						isDynamicRegion = true;
					}
					if (incomingPacket != 241 && currentRegionX == mapRegionX && currentRegionY == mapRegionY && loadingStage == 2) {
						incomingPacket = -1;
						return true;
					}
					currentRegionX = mapRegionX;
					currentRegionY = mapRegionY;
					baseX = (currentRegionX - 6) * 8;
					baseY = (currentRegionY - 6) * 8;
					inTutorialIsland = (currentRegionX / 8 == 48 || currentRegionX / 8 == 49) && currentRegionY / 8 == 48;
					if (currentRegionX / 8 == 48 && currentRegionY / 8 == 148)
						inTutorialIsland = true;
					loadingStage = 1;
					longStartTime = System.currentTimeMillis();
					mainGameGraphicsBuffer.setCanvas();
					drawLoadingMessages(1, "Loading - please wait.", null);
					mainGameGraphicsBuffer.drawGraphics(0, super.graphics, 0);
					if (incomingPacket == 73) {
						int regionCount = 0;
						for (int x = (currentRegionX - 6) / 8; x <= (currentRegionX + 6) / 8; x++) {
							for (int y = (currentRegionY - 6) / 8; y <= (currentRegionY + 6) / 8; y++)
								regionCount++;
						}
						terrainData = new byte[regionCount][];
						objectData = new byte[regionCount][];
						mapCoordinates = new int[regionCount];
						terrainIndices = new int[regionCount];
						objectIndices = new int[regionCount];
						regionCount = 0;
						List<Integer> mapFiles = Lists.newArrayList();
						for (int x = (currentRegionX - 6) / 8; x <= (currentRegionX + 6) / 8; x++) {
							for (int y = (currentRegionY - 6) / 8; y <= (currentRegionY + 6) / 8; y++) {
								mapCoordinates[regionCount] = (x << 8) + y;
								if (inTutorialIsland
										&& (y == 49 || y == 149 || y == 147 || x == 50 || x == 49 && y == 47)) {
									terrainIndices[regionCount] = -1;
									objectIndices[regionCount] = -1;
									regionCount++;
								} else {
									int map = terrainIndices[regionCount] = onDemandFetcher.getMapFiles(0, y, x);
									if (map != -1) {
										onDemandFetcher.provide(3, map);
										mapFiles.add(map);
									}
									int landscape = objectIndices[regionCount] = onDemandFetcher.getMapFiles(1, y, x);
									if (landscape != -1) {
										onDemandFetcher.provide(3, landscape);
										mapFiles.add(landscape);
									}
									regionCount++;
								}
							}
						}

						if (Configuration.developerMode) {
							System.out.println("Map files: " + mapFiles.toString());
						}
					}
					if (incomingPacket == 241) {
						int totalLegitChunks = 0;
						int totalChunks[] = new int[676];
						for (int z = 0; z < 4; z++) {
							for (int x = 0; x < 13; x++) {
								for (int y = 0; y < 13; y++) {
									int tileBits = constructRegionData[z][x][y];
									if (tileBits != -1) {
										int xCoord = tileBits >> 14 & 0x3ff;
										int yCoord = tileBits >> 3 & 0x7ff;
										int mapRegion = (xCoord / 8 << 8) + yCoord / 8;
										for (int idx = 0; idx < totalLegitChunks; idx++) {
											if (totalChunks[idx] != mapRegion)
												continue;
											mapRegion = -1;

										}
										if (mapRegion != -1)
											totalChunks[totalLegitChunks++] = mapRegion;
									}
								}
							}
						}
						terrainData = new byte[totalLegitChunks][];
						objectData = new byte[totalLegitChunks][];
						mapCoordinates = new int[totalLegitChunks];
						terrainIndices = new int[totalLegitChunks];
						objectIndices = new int[totalLegitChunks];
						for (int idx = 0; idx < totalLegitChunks; idx++) {
							int region = mapCoordinates[idx] = totalChunks[idx];
							int localX = region >> 8 & 0xff;
							int localY = region & 0xff;
							int terrainMapId = terrainIndices[idx] = onDemandFetcher.getMapFiles(0, localY, localX);
							if (terrainMapId != -1)
								onDemandFetcher.provide(3, terrainMapId);
							int objectMapId = objectIndices[idx] = onDemandFetcher.getMapFiles(1, localY, localX);
							if (objectMapId != -1)
								onDemandFetcher.provide(3, objectMapId);
						}
					}
					int dx = baseX - previousAbsoluteX;
					int dy = baseY - previousAbsoluteY;
					previousAbsoluteX = baseX;
					previousAbsoluteY = baseY;
					for (int index = 0; index < 16384; index++) {
						NPC npc = npcs[index];
						if (npc != null) {
							for (int poiint = 0; poiint < 10; poiint++) {
								npc.waypoint_x[poiint] -= dx;
								npc.waypoint_y[poiint] -= dy;
							}
							npc.x -= dx * 128;
							npc.y -= dy * 128;
						}
					}
					for (int index = 0; index < maxPlayers; index++) {
						Player player = players[index];
						if (player != null) {
							for (int i31 = 0; i31 < 10; i31++) {
								player.waypoint_x[i31] -= dx;
								player.waypoint_y[i31] -= dy;
							}
							player.x -= dx * 128;
							player.y -= dy * 128;
						}
					}
					loadingMap = true;
					byte startX = 0;
					byte endX = 104;
					byte stepX = 1;
					if (dx < 0) {
						startX = 103;
						endX = -1;
						stepX = -1;
					}
					byte startY = 0;
					byte endY = 104;
					byte stepY = 1;
					if (dy < 0) {
						startY = 103;
						endY = -1;
						stepY = -1;
					}
					for (int x = startX; x != endX; x += stepX) {
						for (int y = startY; y != endY; y += stepY) {
							int shiftedX = x + dx;
							int shiftedY = y + dy;
							for (int plane = 0; plane < 4; plane++)
								if (shiftedX >= 0 && shiftedY >= 0 && shiftedX < 104 && shiftedY < 104)
									groundItems[plane][x][y] = groundItems[plane][shiftedX][shiftedY];
								else
									groundItems[plane][x][y] = null;
						}
					}
					for (SpawnedObject object = (SpawnedObject) spawns
							.reverseGetFirst(); object != null; object = (SpawnedObject) spawns
							.reverseGetNext()) {
						object.x -= dx;
						object.y -= dy;
						if (object.x < 0 || object.y < 0 || object.x >= 104
								|| object.y >= 104)
							object.unlink();
					}
					if (destX != 0) {
						destX -= dx;
						destY -= dy;
					}
					inCutScene = false;
					incomingPacket = -1;
					return true;

				case 208:
					int i3 = inStream.readUShort();
					if (i3 == 65535)
						i3 = -1; // Changed to unsigned short so need to manually make it -1
					if (i3 >= 0)
						method60(i3);
					openWalkableWidgetID = i3;
					incomingPacket = -1;
					return true;

				case 99:
					minimapState = inStream.readUnsignedByte();
					incomingPacket = -1;
					return true;

				case 75:
					int j3 = inStream.method436();
					int j11 = inStream.method436();
					RSInterface.interfaceCache[j11].anInt233 = 2;
					RSInterface.interfaceCache[j11].mediaID = j3;
					incomingPacket = -1;
					return true;

				case 114:
					anInt1104 = inStream.method434() * 30;
					//broadcastActive = false;
					broadcastTimer = 0;
					incomingPacket = -1;
					return true;

				case 60:
					anInt1269 = inStream.readUnsignedByte();
					anInt1268 = inStream.method427();
					while (inStream.currentOffset < packetSize) {
						int k3 = inStream.readUnsignedByte();
						method137(inStream, k3);
					}
					incomingPacket = -1;
					return true;

				case 35:
					int l3 = inStream.readUnsignedByte();
					int k11 = inStream.readUnsignedByte();
					int j17 = inStream.readUnsignedByte();
					int k21 = inStream.readUnsignedByte();
					aBooleanArray876[l3] = true;
					anIntArray873[l3] = k11;
					anIntArray1203[l3] = j17;
					anIntArray928[l3] = k21;
					anIntArray1030[l3] = 0;
					incomingPacket = -1;
					return true;

				case 174:
				    int id = inStream.readUShort();
				    byte var44 = 1;
				    int delay = inStream.readUShort();
				    int type = inStream.readUnsignedByte();
				    // Queue download
				    instance.onDemandFetcher.provide(6, id);

				    // Use actual volume setting instead of hardcoded 7
				    SoundRequestQueue.add(new SoundRequest(id, Math.max(1, 10 - SoundPlayer.getVolume()), delay));
				    incomingPacket = -1;
				    return true;

				case 104:
					int j4 = inStream.method427();
					int i12 = inStream.method426();
					String s6 = inStream.readString();
					if (j4 >= 1 && j4 <= atPlayerActions.length) {
						if (s6.equalsIgnoreCase("null"))
							s6 = null;
						atPlayerActions[j4 - 1] = s6;
						atPlayerArray[j4 - 1] = i12 == 0;
					}
					incomingPacket = -1;
					return true;

				case 78:
					destX = 0;
					incomingPacket = -1;
					return true;

				case 253:
					String s = inStream.readString();
					if (s.equals(":prayertrue:")) {
						prayClicked = true;
					} else if (s.equals(":prayerfalse:")) {
						prayClicked = false;
					} else if (s.equals(":spin")) {
						setSpinClick(true);
						spin();
						incomingPacket = -1;
						return true;
					} else if (s.equals(":resetpost:")) {
						RSInterface listingWidget = RSInterface.interfaceCache[48020];
						if (listingWidget != null) {
							listingWidget.scrollPosition = 0;
						}
					}
					else if (s.equals(":resetBox")) {
						reset();
						incomingPacket = -1;
						return true;
					} else if (s.endsWith(":gamblereq:")) {
						String name = s.substring(0, s.indexOf(":"));
						long nameToLongFormat = StringUtils.longForName(name);
						boolean isIgnored = false;
						for (int index = 0; index < ignoreCount; index++) {
							if (ignoreListAsLongs[index] != nameToLongFormat)
								continue;
							isIgnored = true;
						}
						if (!isIgnored && anInt1251 == 0)
							pushMessage("wishes to Gamble with you.", 31, name);
					} else if (s.endsWith(":tradereq:")) {
						String s3 = s.substring(0, s.indexOf(":"));
						long l17 = StringUtils.longForName(s3);
						boolean flag2 = false;
						for (int j27 = 0; j27 < ignoreCount; j27++) {
							if (ignoreListAsLongs[j27] != l17)
								continue;
							flag2 = true;

						}
						if (!flag2 && anInt1251 == 0)
							pushMessage("wishes to trade with you.", 4, s3);
					} else if (s.startsWith("//")) {
						s = s.replaceAll("//", "");
						pushMessage(s, 13, "");
					} else if (s.startsWith("/")) {
						s = s.replaceFirst("/", "");
						pushMessage(s, 11, "");
					} else if (s.endsWith("#url#")) {
						String link = s.substring(0, s.indexOf("#"));
						pushMessage("Join us at: ", 9, link);
					} else if (s.endsWith(":duelreq:")) {
						String s4 = s.substring(0, s.indexOf(":"));
						long l18 = StringUtils.longForName(s4);
						boolean flag3 = false;
						for (int k27 = 0; k27 < ignoreCount; k27++) {
							if (ignoreListAsLongs[k27] != l18)
								continue;
							flag3 = true;

						}
						if (!flag3 && anInt1251 == 0)
							pushMessage("wishes to duel with you.", 8, s4);
					} else if (s.endsWith(":chalreq:")) {
						String s5 = s.substring(0, s.indexOf(":"));
						long l19 = StringUtils.longForName(s5);
						boolean flag4 = false;
						for (int l27 = 0; l27 < ignoreCount; l27++) {
							if (ignoreListAsLongs[l27] != l19)
								continue;
							flag4 = true;

						}
						if (!flag4 && anInt1251 == 0) {
							String s8 = s.substring(s.indexOf(":") + 1, s.length() - 9);
							pushMessage(s8, 8, s5);
						}
					} else {
						pushMessage(s, 0, "");
					}
					incomingPacket = -1;
					return true;

				case 1:
					for (int k4 = 0; k4 < players.length; k4++)
						if (players[k4] != null)
							players[k4].primaryanim = -1;
					for (int j12 = 0; j12 < npcs.length; j12++)
						if (npcs[j12] != null)
							npcs[j12].primaryanim = -1;
					incomingPacket = -1;
					return true;

				case 50:
					long l4 = inStream.readQWord();
					int i18 = inStream.readUnsignedByte();
					//String s7 = StringUtils.fixName(StringUtils.nameForLong(l4));
					for (int k24 = 0; k24 < friendsCount; k24++) {
						if (l4 != friendsListAsLongs[k24])
							continue;
						if (friendsNodeIDs[k24] != i18) {
							friendsNodeIDs[k24] = i18;
							needDrawTabArea = true;
							if (i18 > 0)
								pushMessage(friendsList[k24] + " has logged in.", 5, "");
							if (i18 == 0)
								pushMessage(friendsList[k24] + " has logged out.", 5, "");
						}
						//s7 = null;
						break;
					}
//					if (s7 != null && friendsCount < 200) {
//						friendsListAsLongs[friendsCount] = l4;
//						friendsList[friendsCount] = s7;
//						friendsNodeIDs[friendsCount] = i18;
//						friendsCount++;
//						needDrawTabArea = true;
//					}
					sortFriendsList();
					incomingPacket = -1;
					return true;

				case 18: // Add friend or ignore
					String displayName = inStream.readNullTerminatedString();
					long displayNameLong = StringUtils.longForName(displayName.toLowerCase());
					boolean friend = inStream.readSignedByte() == 1;
					int world = friend ? inStream.readUnsignedByte() : 0;

					if (friend) {
						friendsList[friendsCount] = displayName;
						friendsListAsLongs[friendsCount] = displayNameLong;
						friendsNodeIDs[friendsCount] = world;
						friendsCount++;
					} else {
						ignoreListAsLongs[ignoreCount++] = displayNameLong;
					}

					sortFriendsList();
					needDrawTabArea = true;
					incomingPacket = -1;
					return true;

				case 19: // Friend updates display name
					String oldDisplayName = inStream.readNullTerminatedString();
					String newDisplayName = inStream.readNullTerminatedString();
					long oldDisplayNameLong = StringUtils.longForName(oldDisplayName.toLowerCase());
					long newDisplayNameLong = StringUtils.longForName(newDisplayName.toLowerCase());

					for (int i22 = 0; i22 < friendsList.length; i22++) {
						if (friendsList[i22] == null)
							continue;
						if (friendsListAsLongs[i22] == oldDisplayNameLong) {
							friendsList[i22] = newDisplayName;
							friendsListAsLongs[i22] = newDisplayNameLong;
							needDrawTabArea = true;
							pushMessage(String.format("Friend '%s' updated display name to '%s'.", oldDisplayName, newDisplayName));
						}
					}

					sortFriendsList();
					incomingPacket = -1;
					return true;

				case 110:
					if (tabID == 12)
						needDrawTabArea = true;
					energy = inStream.readUnsignedByte();
					incomingPacket = -1;
					return true;

				case 254:
					hintType = inStream.readUnsignedByte();
					if (hintType == 1)
						anInt1222 = inStream.readUShort();
					/**
					 * For static icons
					 */
					if (hintType >= 2 && hintType <= 6) {
						if (hintType == 2) {
							anInt937 = 64;
							anInt938 = 64;
						}
						if (hintType == 3) {
							anInt937 = 0;
							anInt938 = 64;
						}
						if (hintType == 4) {
							anInt937 = 128;
							anInt938 = 64;
						}
						if (hintType == 5) {
							anInt937 = 64;
							anInt938 = 0;
						}
						if (hintType == 6) {
							anInt937 = 64;
							anInt938 = 128;
						}
						hintType = 2;
						hintIconXpos = inStream.readUShort();
						hintIconYpos = inStream.readUShort();
						hintIconFloorPos = inStream.readUnsignedByte();
					}
					if (hintType == 10)
						anInt933 = inStream.readUShort();
					incomingPacket = -1;
					return true;

				case 248:
					int i5 = inStream.readUShortA();
					int k12 = inStream.readUShort();
					if (backDialogID != -1) {
						backDialogID = -1;
						inputTaken = true;
					}
					if (inputDialogState != 0) {
						inputDialogState = 0;
						inputTaken = true;
					}
					if (i5 == 55000) {
						RSInterface.interfaceCache[55010].scrollPosition = 0;
						RSInterface.interfaceCache[55050].scrollPosition = 0;
					}
					openInterfaceID = i5;
					invOverlayInterfaceID = k12;
					needDrawTabArea = true;
					tabAreaAltered = true;
					aBoolean1149 = false;
					incomingPacket = -1;
					return true;

				case 79:
					int j5 = inStream.method434();
					int l12 = inStream.readUShortA();
					RSInterface class9_3 = RSInterface.interfaceCache[j5];
					if (class9_3 != null && class9_3.type == 0) {
						if (l12 < 0)
							l12 = 0;
						if (l12 > class9_3.scrollMax - class9_3.height)
							l12 = class9_3.scrollMax - class9_3.height;
						class9_3.scrollPosition = l12;
					}
					incomingPacket = -1;
					return true;

				case 68:
					for (int k5 = 0; k5 < variousSettings.length; k5++)
						if (variousSettings[k5] != anIntArray1045[k5]) {
							variousSettings[k5] = anIntArray1045[k5];
							method33(k5);
							needDrawTabArea = true;
						}
					incomingPacket = -1;
					return true;

				case 196:
					String l5 = inStream.readNullTerminatedString();
					long l5Long = StringUtils.longForName(l5.toLowerCase());
					inStream.readDWord();
					Pair<Integer, PlayerRights[]> rights = PlayerRights.readRightsFromPacket(inStream);
					boolean flag5 = false;
					if (!PlayerRights.hasRightsBetween(rights.getRight(), 1, 4)) {
						for (int l29 = 0; l29 < ignoreCount; l29++) {
							if (ignoreListAsLongs[l29] != l5Long)
								continue;
							flag5 = true;

						}
					}
					if (!flag5 && anInt1251 == 0)
						try {
							String s9 = TextInput.method525(packetSize - 4 - (l5.length() + 1) - rights.getLeft() - 1, inStream);
							String rightsString = PlayerRights.buildCrownString(rights.getRight());
							pushMessage(s9, 7, rightsString + l5);
							if (Preferences.getPreferences().pmNotifications && !appFrame.isFocused()) {
								Notify.create()
										.title(Configuration.CLIENT_TITLE + " private message from " + l5)
										.text(s9)
										.position(Pos.BOTTOM_RIGHT)
										.onAction( new ActionHandler<Notify>() {
											@Override
											public void handle(Notify value) {
												pmTabToReply(l5);
											}
										})
										.hideAfter(5000)
										.shake(250, 5)
										.darkStyle()      // There are two default themes darkStyle() and default.
										.showConfirm();   // You can use warnings and error as well.
							}
						} catch (Exception exception1) {
							exception1.printStackTrace();
							Signlink.reporterror("cde1");
						}
					incomingPacket = -1;
					return true;

				case 85:
					anInt1269 = inStream.method427();
					anInt1268 = inStream.method427();
					incomingPacket = -1;
					return true;

				case 24:
					tabID = inStream.readUnsignedByte();
					incomingPacket = -1;
					return true;

				case 246:
					int i6 = inStream.method434();
					int i13 = inStream.readUShort();
					int k18 = inStream.readUShort();
					if (k18 == 65535) {
						RSInterface.interfaceCache[i6].anInt233 = 0;
						incomingPacket = -1;
						return true;
					} else {
						ItemDefinition itemDef = ItemDefinition.forID(k18);
						RSInterface.interfaceCache[i6].anInt233 = 4;
						RSInterface.interfaceCache[i6].mediaID = k18;
						RSInterface.interfaceCache[i6].modelRotation1 = itemDef.spritePitch;
						RSInterface.interfaceCache[i6].modelRotation2 = itemDef.spriteCameraRoll;
						RSInterface.interfaceCache[i6].modelZoom = (itemDef.spriteScale * 100) / i13;
						incomingPacket = -1;
						return true;
					}

				case 171:
					boolean flag1 = inStream.readUnsignedByte() == 1;
					int j13 = inStream.readUShort();
					if (RSInterface.interfaceCache[j13] != null) {
						RSInterface.interfaceCache[j13].isMouseoverTriggered = flag1;
						RSInterface.interfaceCache[j13].interfaceHidden = flag1;
					}
					incomingPacket = -1;
					return true;

				case 142:
					int j6 = inStream.readUShort();
					if (j6 != 0) {
						method60(j6);
					}
					if (backDialogID != -1) {
						backDialogID = -1;
						inputTaken = true;
					}
					if (inputDialogState != 0) {
						inputDialogState = 0;
						inputTaken = true;
					}
					invOverlayInterfaceID = j6;
					needDrawTabArea = true;
					tabAreaAltered = true;
					openInterfaceID = -1;
					aBoolean1149 = false;
					incomingPacket = -1;
					return true;

				case 126:
					try {
						text = inStream.readString();
						int frame = inStream.readUShortA();

						handleScrollPosition(text, frame);
						handleRunePouch(text, frame);

						if (text.startsWith("www.") || text.startsWith("http")) {
							launchURL(text);
							incomingPacket = -1;
							return true;
						}
						if (text.startsWith(":pollHeight")) {
							int rows = Integer.parseInt(text.split("-")[1]);
							RSInterface rsi = RSInterface.interfaceCache[21406];
							rsi.childY[0] = (rows * 14);
							incomingPacket = -1;
							return true;
						}
						if (text.startsWith(":pollOn")) {
							String option[] = text.split("-");
							pollActive = Boolean.parseBoolean(option[1]);
							incomingPacket = -1;
							return true;
						}
						updateStrings(text, frame);
						sendFrame126(text, frame);
						if (frame >= 18144 && frame <= 18244) {
							clanList[frame - 18144] = text;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					incomingPacket = -1;
					return true;

				case 170:
					try {
						text = inStream.readString();
						// broadcastActive = true;
						// broadcastMessage = text;
						// broadcastTimer = System.currentTimeMillis();
					} catch (Exception e) {
						e.printStackTrace();
					}
					incomingPacket = -1;
					return true;

				case 179: {
					try {
						int broadcastType = inStream.readUnsignedByte();

						int broadcastIndex = inStream.readUnsignedByte();

						if (broadcastType == -1) {
							BroadcastManager.removeIndex(broadcastIndex);
							return true;
						}

						Broadcast broadcast = new Broadcast();

						broadcast.type = broadcastType;

						broadcast.index = broadcastIndex;

						broadcast.message = inStream.readString();

						if (broadcastType == 1) {
							broadcast.url = inStream.readString();
						} else if (broadcastType == 2) {
							broadcast.x = inStream.readDWord();
							broadcast.y = inStream.readDWord();
							broadcast.z = inStream.readUnsignedByte();
						}
						broadcast.setExpireDelay();
						BroadcastManager.addBoradcast(broadcast);
					} catch (Exception e) {
						e.printStackTrace();
					}
					incomingPacket = -1;
					return true;
				}

				case 206:
					publicChatMode = inStream.readUnsignedByte();
					privateChatMode = inStream.readUnsignedByte();
					tradeMode = inStream.readUnsignedByte();
					inputTaken = true;
					incomingPacket = -1;
					return true;
				case 204:
					specialAttack = inStream.readSignedByte();
					specialEnabled = inStream.readSignedByte();
					incomingPacket = -1;
					return true;
				case 240:
					if (tabID == 12)
						needDrawTabArea = true;
					weight = inStream.readSignedWord();
					incomingPacket = -1;
					return true;

				case 8:
					int k6 = inStream.method436();
					int l13 = inStream.readUShort();
					RSInterface.interfaceCache[k6].anInt233 = 1;
					RSInterface.interfaceCache[k6].mediaID = l13;
					incomingPacket = -1;
					return true;

				case 122:
					int l6 = inStream.method436();
					int i14 = inStream.method436();
					int i19 = i14 >> 10 & 0x1f;
					int i22 = i14 >> 5 & 0x1f;
					int l24 = i14 & 0x1f;
					RSInterface.interfaceCache[l6].textColor = (i19 << 19) + (i22 << 11) + (l24 << 3);
					incomingPacket = -1;
					return true;

				case 53:
					needDrawTabArea = true;
					int i7 = inStream.readUShort();
					RSInterface class9_1 = RSInterface.interfaceCache[i7];
					int j19 = inStream.readUShort();

					try {
						System.out.println("Item container size: " + j19);
						for (int j22 = 0; j22 < j19; j22++) {
							int i25 = inStream.readUnsignedByte();
							if (i25 == 255) {
								i25 = inStream.method440();
							}
							class9_1.inventoryItemId[j22] = inStream.method436();
							class9_1.inventoryAmounts[j22] = i25;
//							System.out.println(String.format("Added item [%d, %d] to container %d", class9_1.inventoryItemId[j22], i25, i7));
						}
						for (int j25 = j19; j25 < class9_1.inventoryItemId.length; j25++) {
							class9_1.inventoryItemId[j25] = 0;
							class9_1.inventoryAmounts[j25] = 0;
						}
					} catch (Exception e) {
						System.err.println("Error in container " + i7 + ", length " + j19 + ", actual length " + class9_1.inventoryItemId.length);
						e.printStackTrace();
					}
					incomingPacket = -1;
					return true;

				case 230:
					int j7 = inStream.readUShortA();
					int j14 = inStream.readUShort();
					int k19 = inStream.readUShort();
					int k22 = inStream.method436();
					RSInterface.interfaceCache[j14].modelRotation1 = k19;
					RSInterface.interfaceCache[j14].modelRotation2 = k22;
					RSInterface.interfaceCache[j14].modelZoom = j7;
					incomingPacket = -1;
					return true;

				case 221:
					anInt900 = inStream.readUnsignedByte();
					needDrawTabArea = true;
					incomingPacket = -1;
					return true;

				case 177:
					// setNorth();
					inCutScene = true;
					cinematicCamXViewpointLoc = inStream.readUnsignedByte();
					cinematicCamYViewpointLoc = inStream.readUnsignedByte();
					cinematicCamZViewpointLoc = inStream.readUShort();
					anInt998 = inStream.readUnsignedByte();
					anInt999 = inStream.readUnsignedByte();
					if (anInt999 >= 100) {
						int k7 = cinematicCamXViewpointLoc * 128 + 64;
						int k14 = cinematicCamYViewpointLoc * 128 + 64;
						int i20 = getCenterHeight(plane, k14, k7) - cinematicCamZViewpointLoc;
						int l22 = k7 - xCameraPos;
						int k25 = i20 - zCameraPos;
						int j28 = k14 - yCameraPos;
						int i30 = (int) Math.sqrt(l22 * l22 + j28 * j28);
						yCameraCurve = (int) (Math.atan2(k25, i30) * 325.94900000000001D) & 0x7ff;
						xCameraCurve = (int) (Math.atan2(l22, j28) * -325.94900000000001D) & 0x7ff;
						if (yCameraCurve < 128)
							yCameraCurve = 128;
						if (yCameraCurve > 383)
							yCameraCurve = 383;
					}
					incomingPacket = -1;
					return true;

				case 249:
					anInt1046 = inStream.method426();
					unknownInt10 = inStream.method436();
					incomingPacket = -1;
					return true;

				case 65:
					updateNPCs(inStream, packetSize);
					incomingPacket = -1;
					return true;

				case 27:
					enterInputInChatString = inStream.readString();
					messagePromptRaised = false;
					inputDialogState = 1;
					amountOrNameInput = "";
					inputTaken = true;
					incomingPacket = -1;
					return true;

				case 187:
					enterInputInChatString = inStream.readString();
					messagePromptRaised = false;
					inputDialogState = 2;
					amountOrNameInput = "";
					inputTaken = true;
					incomingPacket = -1;
					return true;

				case 191:
					messagePromptRaised = false;
					inputDialogState = 7;
					amountOrNameInput = "";
					inputTaken = true;
					incomingPacket = -1;
					return true;

				case 192:
					messagePromptRaised = false;
					inputDialogState = 8;
					amountOrNameInput = "";
					inputTaken = true;
					incomingPacket = -1;
					return true;
				case 193:
					enterInputInChatString = inStream.readString();
					messagePromptRaised = false;
					inputDialogState = 10;
					amountOrNameInput = "";
					inputTaken = true;
					aString1121 = "Enter the name of the player you want to join";
					incomingPacket = -1;
					return true;
				case 97:
					int l7 = inStream.readUShort();
					method60(l7);
					if (invOverlayInterfaceID != -1) {
						invOverlayInterfaceID = -1;
						needDrawTabArea = true;
						tabAreaAltered = true;
					}
					if (backDialogID != -1) {
						backDialogID = -1;
						inputTaken = true;
					}
					if (inputDialogState != 0) {
						inputDialogState = 0;
						inputTaken = true;
					}
					// 17511 = Question Type
					// 15819 = Christmas Type
					// 15812 = Security Type
					// 15801 = Item Scam Type
					// 15791 = Password Safety ?
					// 15774 = Good/Bad Password
					// 15767 = Drama Type ????
					if (l7 == 15244) {
						openInterfaceID = 15767;
						fullscreenInterfaceID = 15244;
					} else {
						openInterfaceID = l7;
					}
					aBoolean1149 = false;
					incomingPacket = -1;
					return true;

				case 15:
					autocast = inStream.readUnsignedByte() == 1 ? true : false;
					incomingPacket = -1;
					return true;

				case 218:
					int i8 = inStream.method438();
					dialogID = i8;
					inputTaken = true;
					incomingPacket = -1;
					return true;

				case 87:
					int j8 = inStream.method434();
					int l14 = inStream.method439();
					anIntArray1045[j8] = l14;
					Bank.onConfigChanged(j8, l14);
					Nightmare.instance.handleConfig(j8, l14);
					Autocast.getSingleton().onConfigChanged(j8, l14);
					DailyRewards.get().onConfigReceived(j8, l14);
					DonatorRewards.getInstance().onConfigChanged(j8, l14);
					if (variousSettings[j8] != l14) {
						QuestTab.onConfigChanged(j8, l14);
						MonsterDropViewer.onConfigChanged(j8, l14);
						variousSettings[j8] = l14;
						method33(j8);
						needDrawTabArea = true;
						if (dialogID != -1)
							inputTaken = true;
					}
					incomingPacket = -1;
					return true;

				case 36:
					int k8 = inStream.method434();
					byte byte0 = inStream.readSignedByte();
					anIntArray1045[k8] = byte0;
					Bank.onConfigChanged(k8, byte0);
					EventCalendar.getCalendar().onConfigReceived(k8, byte0);
					Nightmare.instance.handleConfig(k8, byte0);
					Autocast.getSingleton().onConfigChanged(k8, byte0);
					DailyRewards.get().onConfigReceived(k8, byte0);
					DonatorRewards.getInstance().onConfigChanged(k8, byte0);
					if (variousSettings[k8] != byte0) {
						QuestTab.onConfigChanged(k8, byte0);
						MonsterDropViewer.onConfigChanged(k8, byte0);
						variousSettings[k8] = byte0;
						method33(k8);
						needDrawTabArea = true;
						if (dialogID != -1)
							inputTaken = true;
					}
					incomingPacket = -1;
					return true;

				case 61:
					anInt1055 = inStream.readUnsignedByte();
					incomingPacket = -1;
					return true;

				case 197:
					String message = inStream.readString();
					int npcIndex = inStream.readDWord();
					NPCDropInfo info = new NPCDropInfo();
					info.message = message;
					info.npcIndex = npcIndex;
					NPCDropInfo.addEntry(info);
					System.err.println(" "+message+" - "+npcIndex);
					pushMessage(message, 99, "");
					incomingPacket = -1;
					return true;

				case 200:
					int l8 = inStream.readUShort();
					int i15 = inStream.readSignedWord();
					RSInterface class9_4 = RSInterface.interfaceCache[l8];
					class9_4.seq_id = i15;
					if (i15 == 591 || i15 == 588) {
						class9_4.modelZoom = 900; // anInt269
					}
					if (i15 == -1) {
						class9_4.iftype_frameindex = 0;
						class9_4.iftype_loops_remaining = 0;
					}
					incomingPacket = -1;
					return true;

				case 219:
					if (invOverlayInterfaceID != -1) {
						invOverlayInterfaceID = -1;
						needDrawTabArea = true;
						tabAreaAltered = true;
					}
					if (backDialogID != -1) {
						backDialogID = -1;
						inputTaken = true;
					}
					if (inputDialogState != 0) {
						inputDialogState = 0;
						inputTaken = true;
					}
					if (this.isFieldInFocus()) {
						this.resetInputFieldFocus();
						Client.inputString = "";
					}
					openInterfaceID = -1;
					aBoolean1149 = false;
					incomingPacket = -1;
					return true;

				case 34:
					needDrawTabArea = true;
					int i9 = inStream.readUShort();
					if (handledPacket34(i9)) {
						incomingPacket = -1;
						return true;
					}
					RSInterface class9_2 = RSInterface.interfaceCache[i9];
					while (inStream.currentOffset < packetSize) {
						int j20 = (i9 == 30000 ? inStream.readUnsignedByte() : inStream.readDWord());
						int i23 = inStream.readUShort(); //Item ID
						int l25 = inStream.readUnsignedByte(); // Amount
						if (l25 == 255)
							l25 = inStream.readDWord();
						if (j20 >= 0 && j20 < class9_2.inventoryItemId.length) {
							class9_2.inventoryItemId[j20] = i23;
							class9_2.inventoryAmounts[j20] = l25;
						}
						System.out.println(i9 + " / " + i23 + " / " + l25  + " / " + j20) ;
					}
					incomingPacket = -1;
					return true;

				case 4:
				case 44:
				case 84:
				case 101:
				case 105:
				case 117:
				case 147:
				case 151:
				case 156:
				case 160:
				case 215:
					method137(inStream, incomingPacket);
					incomingPacket = -1;
					return true;

				case 106:
					tabID = inStream.method427();
					needDrawTabArea = true;
					tabAreaAltered = true;
					incomingPacket = -1;
					return true;

				case 164:
					int j9 = inStream.method434();
					method60(j9);
					if (invOverlayInterfaceID != -1) {
						invOverlayInterfaceID = -1;
						needDrawTabArea = true;
						tabAreaAltered = true;
					}
					backDialogID = j9;
					inputTaken = true;
					openInterfaceID = -1;
					aBoolean1149 = false;
					incomingPacket = -1;
					return true;

			}

			Signlink.reporterror("T1 - Packet: " + incomingPacket + ", Packet Size: " + packetSize
					+ " - Previous packet: " + previousPacket1 + " Previous packet size: " + previousPacketSize1
					+ ", 2nd Previous packet: " + previousPacket2 + ", 2nd Previous packet size: "
					+ previousPacketSize2);
			PacketLog.log();
			resetLogout();
			ClientWindow.errorOccurredMessage("t1");
		} catch (IOException _ex) {
			PacketLog.log();
			_ex.printStackTrace();
			dropClient();
		} catch (Exception exception) {
			PacketLog.log();
			exception.printStackTrace();
			String s2 = "T2 - " + incomingPacket + "," + previousPacket1 + "," + previousPacket2 + " - " + packetSize
					+ "," + (baseX + myPlayer.waypoint_x[0]) + "," + (baseY + myPlayer.waypoint_y[0]) + " - ";
			for (int j15 = 0; j15 < packetSize && j15 < 50; j15++)
				s2 = s2 + inStream.buffer[j15] + ",";
			Signlink.reporterror(s2);
			resetLogout();
			ClientWindow.errorOccurredMessage("t2");
		}
		return true;
	}

	public static final int INTERFACE_ID = 47000;
	public static final int BOXES64 = 28; // 28 * 64 boxes
	private boolean spinClick;
	private int spins;
	private int spinNum;

	public void setSpinClick(boolean spinClick) { this.spinClick = spinClick; }

	public void spin() {
		if (openInterfaceID != INTERFACE_ID || !spinClick) {
			return;
		}

		RSInterface items = RSInterface.interfaceCache[47100];
		RSInterface boxes = RSInterface.interfaceCache[47200];
		if (spins < 100) {
			shift(items, boxes, 8);
		}
		else if (spins < 200) {
			shift(items, boxes, 5);
		}
		else if (spins < 300) {
			shift(items, boxes, 4);
		}
		else if (spins < 400) {
			shift(items, boxes, 3);
		}
		else if (spins < 488) {
			shift(items, boxes, 2);
		}
		else if (spins < 562) {
			shift(items, boxes, 1);
		}
		else {
			spinComplete();
		}
	}

	private void shift(RSInterface items, RSInterface boxes, int shiftAmount) {
		items.childX[0] -= shiftAmount;
		for(int i=0; i<BOXES64; i++){ boxes.childX[i] -= shiftAmount; }
		spins++;
	}

	private void spinComplete() {
		// Reset
		spins = 0;
		spinClick = false;
		spinNum++;
		// Notify server: spin complete
		stream.createFrame(145);
		stream.method432(696969);
		stream.method432(0);
		stream.method432(0);
	}

	public boolean handledPacket34(int frame) {
		if (openInterfaceID != INTERFACE_ID) {
			return false;
		}

		RSInterface items = RSInterface.interfaceCache[frame];
		while (inStream.currentOffset < packetSize) {
			int slot = inStream.readSmart();
			int itemId = inStream.readUShort();


			inStream.readUnsignedByte();
			int amount = inStream.readDWord();

			if (slot >= 0 && slot < items.inventoryItemId.length) {
				items.inventoryItemId[slot] = itemId;
				items.inventoryAmounts[slot] = amount;

				System.out.println("item: "+itemId+", amount: "+amount);
			}
		}
		return true;
	}
    public void initializePlugins() {
        tileMarkersPlugin = new TileMarkersPlugin();
        tileMarkersPlugin.setClientInstance(this);
        tileMarkersPlugin.initialize();
        tileMarkersPlugin.enable();
    }
	public void reset() {
		//System.out.println("test1");
		if (spinClick) {
			return;
		}

		//System.out.println("test2");

		spinNum = 0;
		RSInterface items = RSInterface.interfaceCache[47100];
		RSInterface boxes = RSInterface.interfaceCache[47200];
		items.childX[0] = 0;
		int x = 0;
		for (int z=0; z<BOXES64; z++) {
			boxes.childX[z] = x;
			x += 2880;
		}
	}

	private String coloredItemName = "";
	private int coloredItemColor = 0xffffff;

	public int currentFog = 0;
	public int[] rainbowFog = {0xFF0000,0xFF7F00,0xFFFF00,0x00FF00,0x0000FF,0x4B0082,0x9400D3};
	public long lastFog=0;

	public boolean runHover;

	public static int cameraZoom = 600;

	public static int[] antialiasingPixels;
	public static int[] antialiasingOffsets;

	public void method146() {
	    // ===== SETUP PHASE =====
	    anInt1265++;
	    
	    // Batch entity updates
	    method47(true);
	    method26(true);
	    method47(false);
	    method26(false);
	    method55();
	    method104();
	    
	    // Enable depth buffer for fog
	    Rasterizer.saveDepth = true;
	    
	    // ===== CAMERA SETUP =====
	    setupCamera();
	    
	    // ===== CAMERA SHAKE =====
	    int l = xCameraPos;
	    int i1 = zCameraPos;
	    int j1 = yCameraPos;
	    int k1 = yCameraCurve;
	    int l1 = xCameraCurve;
	    
	    applyCameraShake();
	    
	    // ===== RENDERING SETUP =====
	    int k2 = Rasterizer.lastTextureRetrievalCount;
	    Model.objectsHovering = 0;
	    Model.objectExist = true;
	    Model.cursorX = super.getMouseX() - 4;
	    Model.cursorY = super.getMouseY() - 4;
	    WorldController.focalLength = 519;

	    // ===== BUFFER CLEAR (EXPENSIVE!) =====
	    clearBuffersOptimized();

	    // ===== MAIN SCENE RENDERING (80% OF FRAME TIME!) =====
	    scene.draw(xCameraPos, yCameraPos, xCameraCurve, zCameraPos, 
	               inCutScene ? method121() : method120(), yCameraCurve);
	    
	    // ===== POST-SCENE RENDERING =====
	    try {
	        if (scene != null) {
	            scene.renderTileMarkers();
	        }
	    } catch (Exception e) {
	        System.err.println("Error rendering tile markers: " + e.getMessage());
	    }
	    
	    renderFogEffects();
	    
	    WorldController.focalLength = 519;
	    scene.clearObj5Cache();
	    updateEntities();
	    drawHeadIcon();
	    writeBackgroundTexture(k2);

	    // ===== UI RENDERING =====
	    if (loggedIn) {
	        if (loginScreenGraphicsBuffer == null && 
	            (currentScreenMode != ScreenMode.FIXED || stretched)) {
	            drawMinimap();
	            drawTabArea();
	            drawChatArea();
	        }
	        draw3dScreen();
	    }
	    
	    if (getUserSettings().isGroundItemOverlay()) {
	        displayGroundItems();
	    }
	    
	    draw3dScreen(); // Why called twice?
	    processExperienceCounter();
	    // processExperienceCounter(); // Why called twice? Remove!

	    // ===== GPU PLUGIN =====
	    applyGPUEffects();

	    // ===== PRESENT TO SCREEN =====
	    presentFrame();
	    
	    // Restore camera
	    xCameraPos = l;
	    zCameraPos = i1;
	    yCameraPos = j1;
	    yCameraCurve = k1;
	    xCameraCurve = l1;
	}

	// ===== OPTIMIZED HELPER METHODS =====

	private void setupCamera() {
	    if (!inCutScene) {
	        int i = camAngleX;
	        if (field556 / 256 > i) {
	            i = field556 / 256;
	        }
	        if (aBooleanArray876[4] && anIntArray1203[4] + 128 > i) {
	            i = anIntArray1203[4] + 128;
	        }

	        int k = viewRotation + viewRotationOffset & 0x7ff;
	        int zoomMultiplier = (WorldController.viewDistance == 9 && 
	                              currentScreenMode == ScreenMode.RESIZABLE) ? 2 
	                           : (WorldController.viewDistance == 10) ? 5 : 3;
	        
	        setCameraPos(
	            cameraZoom + i * zoomMultiplier, 
	            i, 
	            anInt1014, 
	            getCenterHeight(plane, myPlayer.y, myPlayer.x) - 100, 
	            k, 
	            anInt1015
	        );
	    }
	}

	private void applyCameraShake() {
	    for (int i2 = 0; i2 < 5; i2++) {
	        if (!aBooleanArray876[i2]) continue;
	        
	        int shake = (int) ((Math.random() * (anIntArray873[i2] * 2 + 1) - anIntArray873[i2])
	                + Math.sin(anIntArray1030[i2] * (anIntArray928[i2] / 100D)) * anIntArray1203[i2]);
	        
	        switch (i2) {
	            case 0: xCameraPos += shake; break;
	            case 1: zCameraPos += shake; break;
	            case 2: yCameraPos += shake; break;
	            case 3: xCameraCurve = (xCameraCurve + shake) & 0x7ff; break;
	            case 4:
	                yCameraCurve += shake;
	                yCameraCurve = Math.max(128, Math.min(383, yCameraCurve));
	                break;
	        }
	    }
	}

	private void clearBuffersOptimized() {
	    // OPTIMIZATION: Only clear what's needed
	    int bufferSize = DrawingArea.pixels.length;
	    
	    // Clear pixel buffer
	    java.util.Arrays.fill(DrawingArea.pixels, 0);

	    // Ensure depth buffer exists
	    if (Rasterizer.depthBuffer == null || 
	        Rasterizer.depthBuffer.length != bufferSize) {
	        Rasterizer.depthBuffer = new float[bufferSize];
	    }

	    // Clear depth buffer
	    if (Rasterizer.saveDepth) {
	        java.util.Arrays.fill(Rasterizer.depthBuffer, 10000f);
	    }
	}

	private void applyGPUEffects() {
	    try {
	        if (Rasterizer.gpuPlugin != null && 
	            Rasterizer.gpuPlugin.isEnabled() && 
	            loggedIn && 
	            DrawingArea.pixels != null) {
	            
	            Rasterizer.gpuPlugin.setLoggedIn(loggedIn);
	            
	            int bufferSize = DrawingArea.width * DrawingArea.height;
	            if (DrawingArea.pixels.length >= bufferSize) {
	                Rasterizer.gpuPlugin.updatePixelBuffer(
	                    DrawingArea.pixels, 
	                    DrawingArea.width, 
	                    DrawingArea.height
	                );
	                Rasterizer.gpuPlugin.applyEffects();
	                
	                // Redraw UI on top in resizable mode
	                if (currentScreenMode == ScreenMode.RESIZABLE) {
	                    drawMinimap();
	                    drawTabArea();
	                    drawChatArea();
	                }
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("GPU Plugin error: " + e.getMessage());
	    }
	}

	private void presentFrame() {
	    mainGameGraphicsBuffer.drawGraphics(0, super.graphics, 0);
	    
	    // Draw UI buffers when stretched
	    if (currentScreenMode == ScreenMode.FIXED && stretched) {
	        if (mapAreaGraphicsBuffer != null) {
	            mapAreaGraphicsBuffer.drawGraphics(516, 0, super.graphics);
	        }
	        if (tabAreaGraphicsBuffer != null) {
	            tabAreaGraphicsBuffer.drawGraphics(516, 168, super.graphics);
	        }
	        if (chatAreaGraphicsBuffer != null) {
	            chatAreaGraphicsBuffer.drawGraphics(0, 338, super.graphics);
	        }
	    }
	}
	private void renderFogEffects() {
	    GPUConfig gpuConfig = Rasterizer.gpuPlugin != null ? Rasterizer.gpuPlugin.getConfig() : null;
	    
	    if (gpuConfig == null || !gpuConfig.fog) {
	        return;
	    }
	    
	    int fogColor;
	    switch (gpuConfig.fogType) {
	        case GREY: 
	            fogColor = 0x808080; 
	            break;
	        case SISLE: 
	            fogColor = 0xB0C4DE; 
	            break;
	        case DARK: 
	            fogColor = 0x404040; 
	            break;
	        case MAROON: 
	            fogColor = 0x800000; 
	            break;
	        case RAINBOW:
	            if (System.currentTimeMillis() - lastFog > 100) {
	                currentFog = (currentFog + 1) % rainbowFog.length;
	                lastFog = System.currentTimeMillis();
	            }
	            fogColor = rainbowFog[currentFog];
	            break;
	        case OFF:
	        default:
	            return;
	    }
	    
	    // Only fog the sky area - ADD THIS SECTION
	    if (currentScreenMode == ScreenMode.FIXED) {
	        Rasterizer.drawFog(fogColor, 9000, 10000); // Very narrow range at sky depth
	    } else {
	        Rasterizer.drawFog(fogColor, 9000, 10000);
	    }
	}
	/**
	 * Render tile markers if the plugin is enabled
	 */
	private void renderTileMarkers() {
	    try {
	        System.out.println("DEBUG: renderTileMarkers() called in Client.java");
	        
	        if (parentFrame != null && parentFrame.getPluginPanel() != null) {
	            System.out.println("DEBUG: parentFrame and pluginPanel exist");
	            
	            TileMarkersPlugin tileMarkers = parentFrame.getPluginPanel().getTileMarkersPlugin();
	            System.out.println("DEBUG: tileMarkers = " + (tileMarkers != null ? "EXISTS" : "NULL"));
	            
	            if (tileMarkers != null && tileMarkers.isEnabled()) {
	                Map<TileCoordinate, TileMarker> markers = tileMarkers.getTileMarkers();
	                System.out.println("DEBUG: Found " + markers.size() + " markers, current plane = " + Client.plane);

	                for (TileMarker marker : markers.values()) {
	                    int tileX = marker.getWorldX();
	                    int tileY = marker.getWorldY();
	                    int markerPlane = marker.getPlane();
	                    
	                    System.out.println("DEBUG: Marker at tile (" + tileX + "," + tileY + ") plane " + markerPlane + " vs current plane " + Client.plane);
	                    
	                    if (markerPlane != Client.plane) {
	                        System.out.println("DEBUG: Skipping marker - wrong plane");
	                        continue;
	                    }

	                    System.out.println("DEBUG: Rendering marker at tile (" + tileX + "," + tileY + ") with color " + marker.getColor());
	                    renderTileMarker3D(tileX, tileY, marker.getColor());

	                    // Render the label if it exists
	                    String label = marker.getLabel();
	                    if (label != null && !label.trim().isEmpty()) {
	                        renderTileLabel3D(tileX, tileY, label.trim());
	                    }
	                }
	            } else {
	                System.out.println("DEBUG: tileMarkers is null or not enabled");
	            }
	        } else {
	            System.out.println("DEBUG: parentFrame or pluginPanel is null");
	        }
	    } catch (Exception e) {
	        System.err.println("Error rendering tile markers: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

	private void renderTileMarker3D(int tileX, int tileY, Color color) {
	    try {
	        System.out.println("DEBUG: renderTileMarker3D() called for tile (" + tileX + "," + tileY + ")");
	        System.out.println("DEBUG: Player tile position: (" + (myPlayer.x >> 7) + "," + (myPlayer.y >> 7) + ")");
	        System.out.println("DEBUG: Camera world position: (" + xCameraPos + "," + yCameraPos + "," + zCameraPos + ")");
	        
	        // These ARE tile coordinates, so we need to convert to world coordinates
	        int worldX = tileX * 128;
	        int worldY = tileY * 128;
	        
	        System.out.println("DEBUG: Marker world coordinates: (" + worldX + "," + worldY + ")");
	        System.out.println("DEBUG: Distance from camera: " + Math.sqrt(Math.pow(worldX - xCameraPos, 2) + Math.pow(worldY - zCameraPos, 2)));

	        // Get heights at each corner of the tile
	        int h1 = getCenterHeight(Client.plane, worldY, worldX);           // Bottom-left
	        int h2 = getCenterHeight(Client.plane, worldY, worldX + 128);     // Bottom-right  
	        int h3 = getCenterHeight(Client.plane, worldY + 128, worldX + 128); // Top-right
	        int h4 = getCenterHeight(Client.plane, worldY + 128, worldX);     // Top-left
	        
	        System.out.println("DEBUG: Corner heights: " + h1 + ", " + h2 + ", " + h3 + ", " + h4);

	        // Use worldToScreen3D for correct coordinate transformation
	        Point3D corner1 = worldToScreen3D(worldX, worldY, h1);
	        Point3D corner2 = worldToScreen3D(worldX + 128, worldY, h2);
	        Point3D corner3 = worldToScreen3D(worldX + 128, worldY + 128, h3);
	        Point3D corner4 = worldToScreen3D(worldX, worldY + 128, h4);

	        System.out.println("DEBUG: Projected corners:");
	        System.out.println("  corner1 = " + (corner1 != null ? "(" + corner1.x + "," + corner1.y + "," + corner1.z + ")" : "NULL"));
	        System.out.println("  corner2 = " + (corner2 != null ? "(" + corner2.x + "," + corner2.y + "," + corner2.z + ")" : "NULL"));
	        System.out.println("  corner3 = " + (corner3 != null ? "(" + corner3.x + "," + corner3.y + "," + corner3.z + ")" : "NULL"));
	        System.out.println("  corner4 = " + (corner4 != null ? "(" + corner4.x + "," + corner4.y + "," + corner4.z + ")" : "NULL"));

	        // Draw ground quad if all corners are valid
	        if (corner1 != null && corner2 != null && corner3 != null && corner4 != null) {
	            System.out.println("DEBUG: All corners valid! Calling drawGroundQuad with color " + color);
	            drawGroundQuad(corner1, corner2, corner3, corner4, color);
	            System.out.println("DEBUG: drawGroundQuad completed");
	        } else {
	            System.out.println("DEBUG: Some corners are null - cannot draw quad");
	        }

	    } catch (Exception e) {
	        System.err.println("Error rendering ground tile marker: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

	private void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Color borderColor) {
	    // Sort vertices by Y coordinate
	    if (y1 > y2) { int tx = x1, ty = y1; x1 = x2; y1 = y2; x2 = tx; y2 = ty; }
	    if (y2 > y3) { int tx = x2, ty = y2; x2 = x3; y2 = y3; x3 = tx; y3 = ty; }
	    if (y1 > y2) { int tx = x1, ty = y1; x1 = x2; y1 = y2; x2 = tx; y2 = ty; }
	    
	    // Dark gray fill color (consistent for all markers)
	    int fillR = 40;  // Dark gray
	    int fillG = 40;
	    int fillB = 40;
	    
	    // Fill triangle scanlines
	    for (int y = Math.max(0, y1); y <= Math.min(DrawingArea.height - 1, y3); y++) {
	        int leftX = x1, rightX = x1;
	        
	        // Calculate left and right edges
	        if (y <= y2) {
	            if (y2 != y1) leftX = x1 + (x2 - x1) * (y - y1) / (y2 - y1);
	            if (y3 != y1) rightX = x1 + (x3 - x1) * (y - y1) / (y3 - y1);
	        } else {
	            if (y3 != y2) leftX = x2 + (x3 - x2) * (y - y2) / (y3 - y2);
	            if (y3 != y1) rightX = x1 + (x3 - x1) * (y - y1) / (y3 - y1);
	        }
	        
	        if (leftX > rightX) { int temp = leftX; leftX = rightX; rightX = temp; }
	        
	        // Fill horizontal line with dark gray
	        for (int x = Math.max(0, leftX); x <= Math.min(DrawingArea.width - 1, rightX); x++) {
	            int pixelIndex = x + y * DrawingArea.width;
	            if (pixelIndex >= 0 && pixelIndex < DrawingArea.pixels.length) {
	                int existingPixel = DrawingArea.pixels[pixelIndex];
	                
	                // Blend dark gray fill with existing pixel (light transparency)
	                DrawingArea.pixels[pixelIndex] = blendColors(existingPixel, 
	                    (fillR << 16) | (fillG << 8) | fillB, 0.3f);
	            }
	        }
	    }
	}

	private void drawGroundQuad(Point3D c1, Point3D c2, Point3D c3, Point3D c4, Color color) {
	    // Fill with dark gray
	    fillTriangle((int)c1.x, (int)c1.y, (int)c2.x, (int)c2.y, (int)c3.x, (int)c3.y, color);
	    fillTriangle((int)c1.x, (int)c1.y, (int)c3.x, (int)c3.y, (int)c4.x, (int)c4.y, color);
	    
	    // Draw colored border outline
	    drawLine((int)c1.x, (int)c1.y, (int)c2.x, (int)c2.y, color, true);  // Border
	    drawLine((int)c2.x, (int)c2.y, (int)c3.x, (int)c3.y, color, true);  // Border
	    drawLine((int)c3.x, (int)c3.y, (int)c4.x, (int)c4.y, color, true);  // Border
	    drawLine((int)c4.x, (int)c4.y, (int)c1.x, (int)c1.y, color, true);  // Border
	}

	private void drawLine(int x1, int y1, int x2, int y2, Color color, boolean isBorder) {
	    // Bresenham's line algorithm
	    int dx = Math.abs(x2 - x1);
	    int dy = Math.abs(y2 - y1);
	    int sx = x1 < x2 ? 1 : -1;
	    int sy = y1 < y2 ? 1 : -1;
	    int err = dx - dy;
	    
	    int x = x1, y = y1;
	    
	    while (true) {
	        if (x >= 0 && x < DrawingArea.width && y >= 0 && y < DrawingArea.height) {
	            int index = x + y * DrawingArea.width;
	            if (index >= 0 && index < DrawingArea.pixels.length) {
	                int existing = DrawingArea.pixels[index];
	                if (isBorder) {
	                    // Strong colored border
	                    DrawingArea.pixels[index] = blendColors(existing, color.getRGB(), 0.8f);
	                }
	            }
	        }
	        
	        if (x == x2 && y == y2) break;
	        
	        int e2 = 2 * err;
	        if (e2 > -dy) { err -= dy; x += sx; }
	        if (e2 < dx) { err += dx; y += sy; }
	    }
	}

	private void fillScanline(int[] xPoints, int[] yPoints, int y, Color color) {
	    // Simple scanline fill - find intersections and fill between them
	    // This is a simplified version, you may need a more robust polygon fill algorithm
	    int x1 = DrawingArea.width, x2 = 0;
	    
	    // Find leftmost and rightmost intersections (simplified)
	    for (int i = 0; i < 4; i++) {
	        int j = (i + 1) % 4;
	        if ((yPoints[i] <= y && y < yPoints[j]) || (yPoints[j] <= y && y < yPoints[i])) {
	            int x = xPoints[i] + (y - yPoints[i]) * (xPoints[j] - xPoints[i]) / (yPoints[j] - yPoints[i]);
	            x1 = Math.min(x1, x);
	            x2 = Math.max(x2, x);
	        }
	    }
	    
	    // Fill the scanline
	    for (int x = Math.max(0, x1); x <= Math.min(DrawingArea.width - 1, x2); x++) {
	        int index = x + y * DrawingArea.width;
	        if (index >= 0 && index < DrawingArea.pixels.length) {
	            int existing = DrawingArea.pixels[index];
	            DrawingArea.pixels[index] = blendColors(existing, color.getRGB(), 0.3f); // Light fill
	        }
	    }
	}

	private int blendColors(int background, int foreground, float alpha) {
	    int bgR = (background >> 16) & 0xFF;
	    int bgG = (background >> 8) & 0xFF;
	    int bgB = background & 0xFF;
	    
	    int fgR = (foreground >> 16) & 0xFF;
	    int fgG = (foreground >> 8) & 0xFF;
	    int fgB = foreground & 0xFF;
	    
	    int r = (int)(bgR * (1 - alpha) + fgR * alpha);
	    int g = (int)(bgG * (1 - alpha) + fgG * alpha);
	    int b = (int)(bgB * (1 - alpha) + fgB * alpha);
	    
	    return (r << 16) | (g << 8) | b;
	}

	// Helper class for 3D points
	private class Point3D {
	    double x, y, z;
	    Point3D(double x, double y, double z) {
	        this.x = x; this.y = y; this.z = z;
	    }
	}

	private Point3D worldToScreen3D(int worldX, int worldY, int worldZ) {
	    try {
	        System.out.println("DEBUG: worldToScreen3D input: worldX=" + worldX + " worldY=" + worldY + " worldZ=" + worldZ);
	        System.out.println("DEBUG: Camera: xCameraPos=" + xCameraPos + " yCameraPos=" + yCameraPos + " zCameraPos=" + zCameraPos);
	        
	        // Transform to camera space - CORRECT coordinate mapping
	        int relX = worldX - xCameraPos;
	        int relY = worldZ - yCameraPos;  // Z world = Y camera (height)
	        int relZ = worldY - zCameraPos;  // Y world = Z camera (depth)
	        
	        System.out.println("DEBUG: Relative coords: relX=" + relX + " relY=" + relY + " relZ=" + relZ);
	        
	        // Apply camera rotations (EXACT same as your WorldController)
	        int rotX = relZ * Model.SINE[xCameraCurve] + relX * Model.COSINE[xCameraCurve] >> 16;
	        int rotY = relY * Model.COSINE[xCameraCurve] - relZ * Model.SINE[xCameraCurve] >> 16;
	        int rotZ = relY * Model.SINE[xCameraCurve] + relZ * Model.COSINE[xCameraCurve] >> 16;
	        
	        System.out.println("DEBUG: After X rotation: rotX=" + rotX + " rotY=" + rotY + " rotZ=" + rotZ);
	        
	        int finalX = rotY * Model.SINE[yCameraCurve] + rotX * Model.COSINE[yCameraCurve] >> 16;
	        int finalY = rotY * Model.COSINE[yCameraCurve] - rotX * Model.SINE[yCameraCurve] >> 16;
	        int finalZ = rotZ;
	        
	        System.out.println("DEBUG: After Y rotation: finalX=" + finalX + " finalY=" + finalY + " finalZ=" + finalZ);
	        
	        // Depth test
	        if (finalY < 50) {
	            System.out.println("DEBUG: Failed depth test (finalY < 50)");
	            return null; // Behind camera
	        }
	        
	        // Project to screen
	        double screenX = Rasterizer.textureInt1 + (finalX * WorldController.focalLength) / (double)finalY;
	        double screenY = Rasterizer.textureInt2 + (finalZ * WorldController.focalLength) / (double)finalY;
	        
	        System.out.println("DEBUG: Screen projection: screenX=" + screenX + " screenY=" + screenY + " depth=" + finalY);
	        System.out.println("DEBUG: Rasterizer center: textureInt1=" + Rasterizer.textureInt1 + " textureInt2=" + Rasterizer.textureInt2);
	        System.out.println("DEBUG: focalLength=" + WorldController.focalLength);
	        
	        return new Point3D(screenX, screenY, finalY);
	        
	    } catch (Exception e) {
	        System.out.println("DEBUG: Exception in worldToScreen3D: " + e.getMessage());
	        e.printStackTrace();
	        return null;
	    }
	}

	// Check if any point is visible on screen
	private boolean isAnyPointOnScreen(Point3D p1, Point3D p2, Point3D p3) {
	    return isPointOnScreen(p1) || isPointOnScreen(p2) || isPointOnScreen(p3);
	}

	private boolean isPointOnScreen(Point3D p) {
	    return p.x >= -50 && p.x <= DrawingArea.width + 50 && 
	           p.y >= -50 && p.y <= DrawingArea.height + 50;
	}

	/**
	 * Render ammo overlay if enabled
	 */
	private void renderAmmoOverlay() {
	    try {
	        if (parentFrame != null && parentFrame.getPluginPanel() != null) {
	            AmmoPlugin ammoPlugin = parentFrame.getPluginPanel().getAmmoPlugin();
	            if (ammoPlugin != null) {
	                ammoPlugin.renderAmmoOverlay();
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Error rendering ammo overlay: " + e.getMessage());
	    }
	}

	private void renderTileLabel3D(int tileX, int tileY, String label) {
	    try {
	        // Convert world coordinates to screen coordinates
	        int worldX = tileX * 128 + 64;
	        int worldY = tileY * 128 + 64;
	        
	        if (worldX < 128 || worldY < 128 || worldX > 13056 || worldY > 13056) {
	            return;
	        }
	        
	        int i = worldX;
	        int j = 0; // Height offset - you can adjust this to raise/lower text
	        int l = worldY;
	        
	        int i1 = getTerrainHeight(Client.plane, l, i) - j;
	        i -= xCameraPos;
	        i1 -= zCameraPos;
	        l -= yCameraPos;
	        
	        int j1 = Model.SINE[yCameraCurve];
	        int k1 = Model.COSINE[yCameraCurve];
	        int l1 = Model.SINE[xCameraCurve];
	        int i2 = Model.COSINE[xCameraCurve];
	        
	        int j2 = l * l1 + i * i2 >> 16;
	        l = l * i2 - i * l1 >> 16;
	        i = j2;
	        j2 = i1 * k1 - l * j1 >> 16;
	        l = i1 * j1 + l * k1 >> 16;
	        i1 = j2;
	        
	        if (l >= 50) {
	            int focalLength = 512;
	            int screenX = Rasterizer.textureInt1 + i * focalLength / l;
	            int screenY = Rasterizer.textureInt2 + i1 * focalLength / l;
	            
	            // Check if position is on screen
	            if (screenX >= 0 && screenX <= 765 && screenY >= 0 && screenY <= 503) {
	                // Draw text using your client's text rendering system
	                // Black outline
	                chatTextDrawingArea.drawText(0, label, screenY + 1, screenX + 1);
	                // White text
	                chatTextDrawingArea.drawText(0xFFFFFF, label, screenY, screenX);
	            }
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Error rendering tile label: " + e.getMessage());
	    }
	}

	public int getTerrainHeight(int z, int y, int x) {
	    return getCenterHeight(z, y, x); // Calls your existing private method
	}

	private TileMarkersPlugin getTileMarkersPlugin() {
	    try {
	        if (parentFrame != null && parentFrame.getPluginPanel() != null) {
	            return parentFrame.getPluginPanel().getTileMarkersPlugin();
	        }
	        return null;
	    } catch (Exception e) {
	        return null;
	    }
	}
	public void clearTopInterfaces() {
		stream.createFrame(130);
		if (invOverlayInterfaceID != -1) {
			invOverlayInterfaceID = -1;
			needDrawTabArea = true;
			aBoolean1149 = false;
			tabAreaAltered = true;
		}
		if (backDialogID != -1) {
			backDialogID = -1;
			inputTaken = true;
			aBoolean1149 = false;
		}
		openInterfaceID = -1;
		fullscreenInterfaceID = -1;
	}

	private void drawLoadingMessages(int used, String s, String s1) {
		int width = aTextDrawingArea_1271.getTextWidth((used == 1 ? s : s1));
		int height = (s1 == null ? 25 : 38);
		DrawingArea.drawPixels(height, 1, 1, 0, width + 6);
		DrawingArea.drawPixels(1, 1, 1, 0xffffff, width + 6);
		DrawingArea.drawPixels(height, 1, 1, 0xffffff, 1);
		DrawingArea.drawPixels(1, height, 1, 0xffffff, width + 6);
		DrawingArea.drawPixels(height, 1, width + 6, 0xffffff, 1);
		aTextDrawingArea_1271.drawText(0xffffff, s, 18, width / 2 + 5);
		if (s1 != null)
			aTextDrawingArea_1271.drawText(0xffffff, s1, 31, width / 2 + 5);
	}

	private Sprite[] chatButtons;

	Client() {
		firstLoginMessage = "";
		xpAddedPos = expAdded = 0;
		xpLock = false;
		experienceCounter = 0;
		fullscreenInterfaceID = -1;
		playingJingle = false;
		soundType = new int[50];
		soundDelay = new int[50];
		soundVolume = new int[50];
		sound = new int[50];
		soundLocations = new int[50];
		chatTypeView = 0;
		clanTitles = new String[500];
		clanChatMode = 0;
		channelButtonHoverPosition = -1;
		channelButtonClickPosition = 0;
		anIntArrayArray825 = new int[104][104];
		friendsNodeIDs = new int[200];
		groundItems = new NodeList[4][104][104];
		aBoolean831 = false;
		aStream_834 = new Buffer(new byte[5000]);
		npcs = new NPC[16384];
		npcIndices = new int[16384];
		anIntArray840 = new int[1000];
		aStream_847 = Buffer.create();
		aBoolean848 = true;
		openInterfaceID = -1;
		currentExp = new int[Skills.SKILLS_COUNT];
		anIntArray873 = new int[5];
		aBooleanArray876 = new boolean[5];
		drawFlames = false;
		reportAbuseInput = "";
		unknownInt10 = -1;
		menuOpen = false;
		inputString = "";
		maxPlayers = 2048;
		maxPlayerCount = 2047;
		players = new Player[maxPlayers];
		playerIndices = new int[maxPlayers];
		anIntArray894 = new int[maxPlayers];
		aStreamArray895s = new Buffer[maxPlayers];
		anIntArrayArray901 = new int[104][104];
		aByteArray912 = new byte[16384];
		currentStats = new int[Skills.SKILLS_COUNT];
		ignoreListAsLongs = new long[100];
		loadingError = false;
		anIntArray928 = new int[5];
		anIntArrayArray929 = new int[104][104];
		chatTypes = new int[500];
		chatNames = new String[500];
		chatMessages = new String[500];
		chatButtons = new Sprite[4];
		aBoolean954 = true;
		friendsListAsLongs = new long[200];
		currentSong = -1;
		drawingFlames = false;
		spriteDrawX = -1;
		spriteDrawY = -1;
		anIntArray968 = new int[33];
		anIntArray969 = new int[256];
		decompressors = new Decompressor[8];
		variousSettings = new int[25000];
		aBoolean972 = false;
		anInt975 = 50;
		anIntArray976 = new int[anInt975];
		anIntArray977 = new int[anInt975];
		anIntArray978 = new int[anInt975];
		anIntArray979 = new int[anInt975];
		anIntArray980 = new int[anInt975];
		anIntArray981 = new int[anInt975];
		anIntArray982 = new int[anInt975];
		aStringArray983 = new String[anInt975];
		anInt985 = -1;
		hitMarks = new Sprite[20];
		anIntArray990 = new int[5];
		aBoolean994 = false;
		amountOrNameInput = "";
		aClass19_1013 = new NodeList();
		aBoolean1017 = false;
		openWalkableWidgetID = -1;
		anIntArray1030 = new int[5];
		aBoolean1031 = false;
		mapFunctions = new Sprite[125];
		wiki = new Sprite[2];
		dialogID = -1;
		maxStats = new int[Skills.SKILLS_COUNT];
		anIntArray1045 = new int[25000];
		aBoolean1047 = true;
		anIntArray1052 = new int[152];
		anIntArray1229 = new int[152];
		aClass19_1056 = new NodeList();
		anIntArray1057 = new int[33];
		aClass9_1059 = new RSInterface();
		mapScenes = new Sprite[200];
		barFillColor = 0x4d4233;
		anIntArray1065 = new int[7];
		anIntArray1072 = new int[1000];
		anIntArray1073 = new int[1000];
		loadingMap = false;
		friendsList = new String[200];
		inStream = Buffer.create();
		expectedCRCs = new int[9];
		menuActionCmd2 = new int[500];
		menuActionCmd3 = new int[500];
		menuActionID = new int[500];
		menuActionCmd1 = new long[500];
		headIcons = new Sprite[20];
		skullIcons = new Sprite[20];
		headIconsHint = new Sprite[20];
		tabAreaAltered = false;
		aString1121 = "";
		atPlayerActions = new String[6];
		atPlayerArray = new boolean[6];
		constructRegionData = new int[4][13][13];
		mapIconSprite = new Sprite[1000];
		inTutorialIsland = false;
		aBoolean1149 = false;
		crosses = new Sprite[8];
		musicEnabled = true;
		needDrawTabArea = false;
		loggedIn = false;
		canMute = false;
		isDynamicRegion = false;
		inCutScene = false;
		myUsername = "";
		setPassword("");
		genericLoadingError = false;
		reportAbuseInterfaceID = -1;
		spawns = new NodeList();
		camAngleX = 128;
		invOverlayInterfaceID = -1;
		stream = Buffer.create();
		menuActionName = new String[500];
		anIntArray1203 = new int[5];
		chatAreaScrollLength = 78;
		promptInput = "";
		modIcons = new Sprite[41];
		tabID = 3;
		inputTaken = false;
		songChanging = true;
		collisionMaps = new CollisionMap[4];
		aBoolean1242 = false;
		rsAlreadyLoaded = false;
		welcomeScreenRaised = true;
		messagePromptRaised = false;
		backDialogID = -1;
		bigX = new int[4000];
		bigY = new int[4000];
		chatTimes = new long[500];
	}

	public int getLocalPlayerX() {
		return baseX + (myPlayer.x - 6 >> 7);
	}

	public int getLocalPlayerY() {
		return baseY + (myPlayer.y - 6 >> 7);
	}

	public int xpCounter;
	public int expAdded;
	public int xpAddedPos;
	public boolean xpLock;
	private static boolean gpuTested = false;
	private Sprite chatArea;
	private Sprite infinity;

	public String name;
	public String message;
	public int chatTypeView;
	public int clanChatMode;
	public int duelMode;
	public int autocastId = 0;
	public boolean autocast = false;

	public static Sprite[] cacheSprite, cacheSprite1, cacheSprite2, cacheSprite3, cacheSprite4;
	public static Sprite[] cacheInterface;

	private Background titleButton;
	private int ignoreCount;
	private long longStartTime;
	private int[][] anIntArrayArray825;
	private int[] friendsNodeIDs;
	private NodeList[][][] groundItems;
	private int[] anIntArray828;
	private int[] anIntArray829;
	private volatile boolean aBoolean831;
	private LoginScreenState loginScreenState = LoginScreenState.LOGIN;
	private Buffer aStream_834;
	public NPC[] npcs;
	private int npcCount;
	private int[] npcIndices;
	private int anInt839;
	private int[] anIntArray840;
	private int dealtWithPacket;
	private int dealtWithPacketSize;
	private int previousPacket1;
	private int previousPacket2;
	private int previousPacketSize2;
	private int previousPacketSize1;
	private String aString844;
	private int privateChatMode;
	private Buffer aStream_847;
	private boolean aBoolean848;
	private static int anInt849;
	private int[] anIntArray850;
	private int[] anIntArray851;
	private int[] anIntArray852;
	private int[] anIntArray853;
	private static int anInt854;
	private int hintType;
	public static int openInterfaceID;
	public int xCameraPos;
	public int zCameraPos;
	public int yCameraPos;
	public int yCameraCurve;
	public int xCameraCurve;
	private final int[] currentExp;
	private Sprite mapFlag;
	private Sprite mapMarker;
	private AnimatedSprite loginBackground2;
	private final int[] anIntArray873;
	private final boolean[] aBooleanArray876;
	private int weight;
	private MouseDetection mouseDetection;
	private volatile boolean drawFlames;
	private String reportAbuseInput;
	private int unknownInt10;
	private boolean menuOpen;
	private int anInt886;
	public static String inputString;
	private final int maxPlayers;
	public final int maxPlayerCount;
	public Player[] players;
	private int playerCount;
	private int[] playerIndices;
	private int anInt893;
	private int[] anIntArray894;
	private Buffer[] aStreamArray895s;
	private int viewRotationOffset;
	private int friendsCount;
	private int anInt900;
	private int[][] anIntArrayArray901;
	private byte[] aByteArray912;
	private int anInt913;
	private int crossX;
	private int crossY;
	private int crossIndex;
	private int crossType;
	public static int plane;
	public final int[] currentStats;
	private int anInt924;
	private final long[] ignoreListAsLongs;
	private boolean loadingError;
	private final int[] anIntArray928;
	private int[][] anIntArrayArray929;
	private Sprite aClass30_Sub2_Sub1_Sub1_931;
	private Sprite aClass30_Sub2_Sub1_Sub1_932;
	private int anInt933;
	private int hintIconXpos;
	private int hintIconYpos;
	private int hintIconFloorPos;
	private int anInt937;
	private int anInt938;
	private long[] chatTimes;
	private int[] chatTypes;
	private String[] chatNames;
	private String[] chatMessages;
	private int tickDelta;
	private WorldController scene;
	private int menuScreenArea;
	private int menuOffsetX;
	private int menuOffsetY;
	private int menuWidth;
	private int menuHeight;
	private long aLong953;
	private boolean aBoolean954;
	private long[] friendsListAsLongs;
	private int currentSong;
	private static int nodeID = 1;
	static int portOff;
	static boolean clientData;
	private static boolean isMembers = true;
	private static boolean lowMem;
	private volatile boolean drawingFlames;
	private int spriteDrawX;
	private int spriteDrawY;
	private final int[] anIntArray965 = { 0xffff00, 0xff0000, 65280, 65535, 0xff00ff, 0xffffff };
	private final int[] anIntArray968;
	private final int[] anIntArray969;
	final Decompressor[] decompressors;
	public int variousSettings[];
	private boolean aBoolean972;
	private final int anInt975;
	private final int[] anIntArray976;
	private final int[] anIntArray977;
	private final int[] anIntArray978;
	private final int[] anIntArray979;
	private final int[] anIntArray980;
	private final int[] anIntArray981;
	private final int[] anIntArray982;
	private final String[] aStringArray983;
	private int field556;
	private int anInt985;
	private int anInt986;
	private Sprite[] hitMarks;
	private int anInt989;
	private final int[] anIntArray990;
	private final boolean aBoolean994;
	private int cinematicCamXViewpointLoc;
	private int cinematicCamYViewpointLoc;
	private int cinematicCamZViewpointLoc;
	private int anInt998;
	private int anInt999;
	private ISAACRandomGen encryption;
	private Sprite mapEdge;
	public static int[][] anIntArrayArray1003 = {
			{ 6798, 107, 10283, 16, 4797, 7744, 5799, 4634, -31839, 22433, 2983, -11343, 8, 5281, 10438, 3650, -27322,
					-21845, 200, 571, 908, 21830, 28946, -15701, -14010 },
			{ 8741, 12, -1506, -22374, 7735, 8404, 1701, -27106, 24094, 10153, -8915, 4783, 1341, 16578, -30533, 25239,
					8, 5281, 10438, 3650, -27322, -21845, 200, 571, 908, 21830, 28946, -15701, -14010 },
			{ 25238, 8742, 12, -1506, -22374, 7735, 8404, 1701, -27106, 24094, 10153, -8915, 4783, 1341, 16578, -30533,
					8, 5281, 10438, 3650, -27322, -21845, 200, 571, 908, 21830, 28946, -15701, -14010 },
			{ 4626, 11146, 6439, 12, 4758, 10270 },

			{ 4550, 4537, 5681, 5673, 5790, 6806, 8076, 4574, 0, 12821, 100 } };

	private Sprite multiOverlay;
	public String amountOrNameInput;
	private int anInt1005;
	private int daysSinceLastLogin;
	private int packetSize;
	private int incomingPacket;
	private int anInt1009;
	private int anInt1010;
	private int anInt1011;
	private NodeList aClass19_1013;
	private int anInt1014;
	private int anInt1015;
	private int anInt1016;
	private boolean aBoolean1017;
	public int openWalkableWidgetID;
	private static final int[] SKILL_EXPERIENCE;
	private int minimapState;
	private int anInt1022;
	private int loadingStage;
	private Sprite scrollBar1;
	private Sprite scrollBar2;
	private int anInt1026;
	private final int[] anIntArray1030;
	private boolean aBoolean1031;
	private static Sprite[] mapFunctions;
	private static Sprite[] wiki;
	public static int baseX;
	public static int baseY;
	private int previousAbsoluteX;
	private int previousAbsoluteY;
	private int loginFailures;
	private int anInt1039;
	private int anInt1040;
	private int anInt1041;
	private int dialogID;
	public final int[] maxStats;
	private final int[] anIntArray1045;
	private int anInt1046;
	private boolean aBoolean1047;
	public TextDrawingArea smallText;
	public TextDrawingArea XPFONT;
	public TextDrawingArea aTextDrawingArea_1271;
	public TextDrawingArea chatTextDrawingArea;
	public TextDrawingArea aTextDrawingArea_1273;
	public RSFont newSmallFont;
	public RSFont newRegularFont;
	public RSFont newBoldFont;
	public RSFont newFancyFont;

	/**
	 * New fonts
	 */
	public static RSFont lato, latoBold, kingthingsPetrock, kingthingsPetrockLight;

	private int anInt1048;
	private String aString1049;
	private static int anInt1051;
	private final int[] anIntArray1052;
	private StreamLoader titleStreamLoader;
	private int anInt1055;
	private NodeList aClass19_1056;
	private final int[] anIntArray1057;
	public final RSInterface aClass9_1059;
	private Sprite[] mapScenes;
	private static int anInt1061;
	private final int barFillColor;
	private int friendsListAction;
	private final int[] anIntArray1065;
	private int mouseInvInterfaceIndex;
	private int lastActiveInvInterface;
	public OnDemandFetcher onDemandFetcher;
	private int currentRegionX;
	private int currentRegionY;
	private int mapIconAmount;
	private int[] anIntArray1072;
	private int[] anIntArray1073;
	private Sprite mapDotItem;
	private Sprite mapDotNPC;
	private Sprite mapDotPlayer;
	private Sprite mapDotFriend;
	private Sprite mapDotTeam;
	private Sprite mapDotClan;
	private int anInt1079;
	private boolean loadingMap;
	private String[] friendsList;
	private Buffer inStream;
	private int draggingItemInterfaceId;
	private int itemDraggingSlot;
	private int activeInterfaceType;
	private int anInt1087;
	private int anInt1088;
	public static int anInt1089;
	private final int[] expectedCRCs;
	private int[] menuActionCmd2;
	private int[] menuActionCmd3;
	private int[] menuActionID;
	private long[] menuActionCmd1;
	private Sprite[] headIcons;
	private Sprite[] skullIcons;
	private Sprite[] headIconsHint;
	private static int anInt1097;
	private int x;
	private int y;
	private int height;
	private int anInt1101;
	private int angle;
	public static boolean tabAreaAltered;
	private RSImageProducer loginScreenGraphicsBuffer;
	private RSImageProducer leftSideFlame;
	private RSImageProducer rightSideFlame;
	private int anInt1104;
	private RSImageProducer aRSImageProducer_1110;
	private RSImageProducer aRSImageProducer_1111;
	private static int anInt1117;
	private int membersInt;
	private String aString1121;
	private String enterInputInChatString;
	public static Player myPlayer;
	public boolean broadcastActive;
	public long broadcastTimer;
	public String broadcastMessage;
	private final String[] atPlayerActions;
	private final boolean[] atPlayerArray;
	private final int[][][] constructRegionData;
	public final static int[] tabInterfaceIDs = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
	private int cameraOffsetY;
	private int menuActionRow;
	private static int anInt1134;
	private int spellSelected;
	private int anInt1137;
	private int spellUsableOn;
	private String spellTooltip;
	private Sprite[] mapIconSprite;
	private boolean inTutorialIsland;
	private static int anInt1142;
	private int energy;
	private boolean aBoolean1149;
	private Sprite[] crosses;
	private boolean musicEnabled;
	private Background[] aBackgroundArray1152s;
	public static boolean needDrawTabArea;
	private int unreadMessages;
	private static int anInt1155;
	private static boolean fpsOn;
	private static boolean drawGrid;
	public static boolean loggedIn;
	private boolean canMute;
	private boolean isDynamicRegion;
	private boolean inCutScene;
	public static int update_tick;
	private static final String validUserPassChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!\"\243$%^&*()-_=+[{]};:'@#~,<.>/?\\| ";
	private RSImageProducer tabAreaGraphicsBuffer;
	private RSImageProducer mapAreaGraphicsBuffer;
	public GraphicsBuffer mainGameGraphicsBuffer;
	private RSImageProducer chatAreaGraphicsBuffer;
	private int daysSinceRecovChange;
	private RSSocket socketStream;
	public static int minimapZoom;
	private static int anInt1175;
	private boolean genericLoadingError;
	private final int[] objectTypes = { 0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3 };
	private int reportAbuseInterfaceID;
	private NodeList spawns;
	private int[] anIntArray1180;
	private int[] anIntArray1181;
	private int[] anIntArray1182;
	private byte[][] terrainData;
	private int camAngleX;
	private static int viewRotation;
	private int anInt1186;
	private int anInt1187;
	private static int anInt1188;
	private int invOverlayInterfaceID;
	private int[] anIntArray1190;
	private int[] anIntArray1191;
	public static Buffer stream;
	private int anInt1193;
	private boolean splitPrivateChat = true;
	private String[] clanList = new String[100];
	private String[] menuActionName;
	private Sprite aClass30_Sub2_Sub1_Sub1_1201;
	private Sprite aClass30_Sub2_Sub1_Sub1_1202;
	private final int[] anIntArray1203;
	public static int anIntArray1204[] = { 9104, 10275, 7595, 3610, 7975, 8526, 918, -26734, 24466, 10145, -6882, 5027,
			1457, 16565, -30545, 25486, 24, 5392, 10429, 3673, -27335, -21957, 192, 687, 412, 21821, 28835, -15460,
			-14019 };
	private static boolean flagged;
	private int anInt1208;
	private int minimapRotation;
	public static int chatAreaScrollLength;
	private String promptInput;
	private int mouseClickCount;
	private int[][][] intGroundArray;
	private long aLong1215;
	private int loginScreenCursorPos;
	private final Sprite[] modIcons;
	private long aLong1220;
	public static int tabID;
	private int anInt1222;
	public static boolean inputTaken;
	public static int inputDialogState;
	private static int anInt1226;
	private int nextSong;
	private boolean songChanging;
	private final int[] anIntArray1229;
	private CollisionMap[] collisionMaps;
	public static int anIntArray1232[];
	private int[] mapCoordinates;
	private int[] terrainIndices;
	private int[] objectIndices;
	private int anInt1237;
	private int anInt1238;
	public final int anInt1239 = 100;
	private boolean aBoolean1242;
	private int atInventoryLoopCycle;
	private int atInventoryInterface;
	private int atInventoryIndex;
	private int atInventoryInterfaceType;
	private byte[][] objectData;
	private int tradeMode;
	private int gameMode;
	private int anInt1249;
	private int anInt1251;
	private final boolean rsAlreadyLoaded;
	private int anInt1253;
	private boolean welcomeScreenRaised;
	private boolean messagePromptRaised;
	private byte[][][] byteGroundArray;
	private int prevSong;
	private int destX;
	private int destY;
	private Sprite minimapImage;
	public Sprite backgroundFix;
	private int anInt1257;
	private long aLong1172;
	private int anInt1264;
	private int anInt1265;
	private int anInt1268;
	private int anInt1269;
	private int anInt1275;
	public static int backDialogID;
	private int cameraOffsetX;
	private int[] bigX;
	private int[] bigY;
	private int itemSelected;
	private int anInt1283;
	private int anInt1284;
	private int anInt1285;
	private String selectedItemName;
	private int publicChatMode;
	private static int anInt1288;
	public static int anInt1290;
	public static String server = Configuration.DEDICATED_SERVER_ADDRESS;
	public static int port = Configuration.PORT;
	public static boolean controlIsDown;
	public int drawCount;
	public int fullscreenInterfaceID;
	public int anInt1044;// 377
	public int anInt1129;// 377
	public int anInt1315;// 377
	public int anInt1500;// 377
	public int anInt1501;// 377
	public int[] fullScreenTextureArray;

	private String myUsername;
	private String myPassword;

	public void setPassword(String password) {
		myPassword = password;
	}

	public String getPassword() {
		return myPassword;
	}

	public void resetAllImageProducers() {
		if (super.fullGameScreen != null) {
			return;
		}
		loginScreenGraphicsBuffer = null;
		chatAreaGraphicsBuffer = null;
		mapAreaGraphicsBuffer = null;
		tabAreaGraphicsBuffer = null;
		mainGameGraphicsBuffer = null;
		leftSideFlame = null;
		rightSideFlame = null;
		fullGameScreen = new RSImageProducer(worldViewportWidth, worldViewportHeight, getGameComponent());
		welcomeScreenRaised = true;
	}
	public void launchURL(String url) {
		if (!url.toLowerCase().startsWith("http")) {
			url = "http://" + url;
		}
		String osName = System.getProperty("os.name");
		try {
			if (osName.startsWith("Mac OS")) {
				@SuppressWarnings("rawtypes")
				Class fileMgr = Class.forName("com.apple.eio.FileManager");
				@SuppressWarnings("unchecked")
				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[]{String.class});
				openURL.invoke(null, new Object[]{url});
			} else if (osName.startsWith("Windows"))
				Runtime.getRuntime().exec("cmd /c start \"j\" \"" + url + "\"");
			else { // assume Unix or Linux
				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
					Desktop.getDesktop().browse(new URI(url));
				}
			}
		} catch (Exception e) {
			pushMessage("Failed to open URL.", 0, "");
			System.err.println("Failed to launcher url on operating system " + osName);
			e.printStackTrace();
		}
	}

//	public void launchURLold(String url) {
////		if (server.equals("173.185.70.167")) {
////			javax.swing.JOptionPane.showMessageDialog(this, "Staff just tried to direct you to: " + url);
////			return;
////		}
//		if (!url.toLowerCase().startsWith("http")) {
//			url = "http://" + url;
//		}
//		String osName = System.getProperty("os.name");
//		try {
//			if (osName.startsWith("Mac OS")) {
//				@SuppressWarnings("rawtypes")
//				Class fileMgr = Class.forName("com.apple.eio.FileManager");
//				@SuppressWarnings("unchecked")
//				Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] { String.class });
//				openURL.invoke(null, new Object[] { url });
//			} else if (osName.startsWith("Windows"))
//				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
//			else { // assume Unix or Linux
//				if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
//					Desktop.getDesktop().browse(new URI(url));
//				}
//			}
//		} catch (Exception e) {
//			pushMessage("Failed to open URL.", 0, "");
//			System.err.println("Failed to launcher url on operating system " + osName);
//			e.printStackTrace();
//		}
//	}
	private void drawInputField(RSInterface child, int xPosition, int yPosition, int width, int height) {
		int clickX = super.getSaveClickX(), clickY = super.getSaveClickY();
		Sprite[] inputSprites = this.inputSprites;
		int xModification = 0, yModification = 0;
		for (int row = 0; row < width; row += 12) {
			if (row + 12 > width)
				row -= 12 - (width - row);
			inputSprites[6].drawSprite(xModification <= 0 ? xPosition + row : xPosition + xModification, yPosition);
			for (int collumn = 0; collumn < height; collumn += 12) {
				if (collumn + 12 > height)
					collumn -= 12 - (height - collumn);
				inputSprites[6].drawSprite(xPosition + row,
						yModification <= 0 ? yPosition + collumn : yPosition + yModification);
			}
		}
		inputSprites[1].drawSprite(xPosition, yPosition);
		inputSprites[0].drawSprite(xPosition, yPosition + height - 8);
		inputSprites[2].drawSprite(xPosition + width - 4, yPosition);
		inputSprites[3].drawSprite(xPosition + width - 4, yPosition + height - 8);
		xModification = 0;
		yModification = 0;
		for (int top = 0; top < width; top += 8) {
			if (top + 8 > width)
				top -= 8 - (width - top);
			inputSprites[5].drawSprite(xPosition + top, yPosition);
			inputSprites[5].drawSprite(xPosition + top, yPosition + height - 1);
		}
		for (int bottom = 0; bottom < height; bottom += 8) {
			if (bottom + 8 > height)
				bottom -= 8 - (height - bottom);
			inputSprites[4].drawSprite(xPosition, yPosition + bottom);
			inputSprites[4].drawSprite(xPosition + width - 1, yPosition + bottom);
		}
		String message = child.message;
		if (aTextDrawingArea_1271.getTextWidth(message) > child.width - 10)
			message = message.substring(message.length() - (child.width / 10) - 1, message.length());
		if (child.displayAsterisks)
			this.aTextDrawingArea_1271.method389(false, (xPosition + 4), child.textColor,
					new StringBuilder().append("").append(StringUtils.passwordAsterisks(message))
							.append(((!child.isInFocus ? 0 : 1) & (update_tick % 40 < 20 ? 1 : 0)) != 0 ? "|" : "")
							.toString(),
					(yPosition + (height / 2) + 6));
		else
			this.aTextDrawingArea_1271.method389(false, (xPosition + 4), child.textColor,
					new StringBuilder().append("").append(message)
							.append(((!child.isInFocus ? 0 : 1) & (update_tick % 40 < 20 ? 1 : 0)) != 0 ? "|" : "")
							.toString(),
					(yPosition + (height / 2) + 6));


		boolean clicked = false;
		if (drawingTabArea && currentScreenMode == ScreenMode.FIXED) {
			int x = 516;
			int y = 168;
			clicked = clickX >= xPosition + x && clickX <= xPosition + x + child.width && clickY >= yPosition + y && clickY <= yPosition + y + child.height;
		} else {
			clicked = clickX >= xPosition && clickX <= xPosition + child.width && clickY >= yPosition && clickY <= yPosition + child.height;
		}

		if (clicked && !child.isInFocus && getInputFieldFocusOwner() != child) {
			if ((super.clickMode2 == 1 && !menuOpen)) {
				RSInterface.currentInputFieldId = child.id;
				setInputFieldFocusOwner(child);
				if (child.message != null && child.message.equals(child.defaultInputFieldText))
					child.message = "";
				if (child.message == null)
					child.message = "";
			}
		}
	}
	public void setInputFieldFocusOwner(RSInterface owner) {
		for (RSInterface rsi : RSInterface.interfaceCache)
			if (rsi != null)
				if (rsi == owner)
					rsi.isInFocus = true;
				else
					rsi.isInFocus = false;
	}

	public RSInterface getInputFieldFocusOwner() {
		for (RSInterface rsi : RSInterface.interfaceCache)
			if (rsi != null)
				if (rsi.isInFocus)
					return rsi;
		return null;
	}

	public void resetInputFieldFocus() {
		for (RSInterface rsi : RSInterface.interfaceCache)
			if (rsi != null)
				rsi.isInFocus = false;
		RSInterface.currentInputFieldId = -1;
	}

	public boolean isFieldInFocus() {
		if (openInterfaceID == -1 && invOverlayInterfaceID <= 0) {
			return false;
		}
		for (RSInterface rsi : RSInterface.interfaceCache) {
			if (rsi != null) {
				if (rsi.type == 16 && rsi.isInFocus) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean scrollbarVisible(RSInterface widget) {
		if (widget.id == 55010) {
			if (RSInterface.interfaceCache[55024].message.length() <= 0) {
				return false;
			}
		} else if (widget.id == 55050) {
			if (RSInterface.interfaceCache[55064].message.length() <= 0) {
				return false;
			}
		}
		return true;
	}

	void mouseWheelDragged(int i, int j) {
		if (!mouseWheelDown)
			return;
		this.anInt1186 += i * 3;
		this.anInt1187 += (j << 1);
	}

	public static int anInt1401 = 256;
	private int musicVolume = 255;
	public static long aLong1432;
	private final int[] sound;
	public int currentSound;
	private final int[] soundDelay;
	private final int[] soundType;
	private final int[] soundVolume;
	public int[][][] tileHeights;
	static int[] soundLocations;
	private static int soundEffectVolume = 127;
	public static int[] anIntArray385 = new int[] { 12800, 12800, 12800, 12800, 12800, 12800, 12800, 12800, 12800,
			12800, 12800, 12800, 12800, 12800, 12800, 12800 };
	public static boolean LOOP_MUSIC = false;

	public static final void sleep(long time) {
		if (time > 0L) {
			if (time % 10L != 0L) {
				threadSleep(time);
			} else {
				threadSleep(time - 1L);
				threadSleep(1L);
			}
		}
	}

	private static final void threadSleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException ex) {
		}
	}

	static {
		SKILL_EXPERIENCE = new int[99];
		int i = 0;
		for (int j = 0; j < 99; j++) {
			int l = j + 1;
			int i1 = (int) (l + 300D * Math.pow(2D, l / 7D));
			i += i1;
			SKILL_EXPERIENCE[j] = i / 4;
		}
		anIntArray1232 = new int[32];
		i = 2;
		for (int k = 0; k < 32; k++) {
			anIntArray1232[k] = i - 1;
			i += i;
		}
	}

	public static String md5Hash(String md5) {
		try {

			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());

			StringBuffer sb = new StringBuffer();

			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
			}

			return sb.toString();

		} catch (java.security.NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getLevelForXP(int exp) {
		int points = 0;
		int output;
		if (exp > 13034430)
			return 99;
		for (int lvl = 1; lvl <= 99; lvl++) {
			points += Math.floor(lvl + 300.0 * Math.pow(2.0, lvl / 7.0));
			output = (int) Math.floor(points / 4);
			if (output >= exp) {
				return lvl;
			}
		}
		return 0;
	}

	public void updateSkillStrings(int i) {
		switch (i) {
			case 0:
				sendFrame126("" + currentStats[0] + "", 4004);
				sendFrame126("" + getLevelForXP(currentExp[0]) + "", 4005);
				sendFrame126("" + currentExp[0] + "", 4044);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[0]) + 1) + "", 4045);
				break;

			case 1:
				sendFrame126("" + currentStats[1] + "", 4008);
				sendFrame126("" + getLevelForXP(currentExp[1]) + "", 4009);
				sendFrame126("" + currentExp[1] + "", 4056);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[1]) + 1) + "", 4057);
				break;

			case 2:
				sendFrame126("" + currentStats[2] + "", 4006);
				sendFrame126("" + getLevelForXP(currentExp[2]) + "", 4007);
				sendFrame126("" + currentExp[2] + "", 4050);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[2]) + 1) + "", 4051);
				break;

			case 3:
				sendFrame126("" + currentStats[3] + "", 4016);
				sendFrame126("" + getLevelForXP(currentExp[3]) + "", 4017);
				sendFrame126("" + currentExp[3] + "", 4080);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[3]) + 1) + "", 4081);
				break;

			case 4:
				sendFrame126("" + currentStats[4] + "", 4010);
				sendFrame126("" + getLevelForXP(currentExp[4]) + "", 4011);
				sendFrame126("" + currentExp[4] + "", 4062);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[4]) + 1) + "", 4063);
				break;

			case 5:
				sendFrame126("" + currentStats[5] + "", 4012);
				sendFrame126("" + getLevelForXP(currentExp[5]) + "", 4013);
				sendFrame126("" + currentExp[5] + "", 4068);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[5]) + 1) + "", 4069);
				sendFrame126("" + currentStats[5] + "/" + getLevelForXP(currentExp[5]) + "", 687);// Prayer
				// frame
				break;

			case 6:
				sendFrame126("" + currentStats[6] + "", 4014);
				sendFrame126("" + getLevelForXP(currentExp[6]) + "", 4015);
				sendFrame126("" + currentExp[6] + "", 4074);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[6]) + 1) + "", 4075);
				break;

			case 7:
				sendFrame126("" + currentStats[7] + "", 4034);
				sendFrame126("" + getLevelForXP(currentExp[7]) + "", 4035);
				sendFrame126("" + currentExp[7] + "", 4134);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[7]) + 1) + "", 4135);
				break;

			case 8:
				sendFrame126("" + currentStats[8] + "", 4038);
				sendFrame126("" + getLevelForXP(currentExp[8]) + "", 4039);
				sendFrame126("" + currentExp[8] + "", 4146);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[8]) + 1) + "", 4147);
				break;

			case 9:
				sendFrame126("" + currentStats[9] + "", 4026);
				sendFrame126("" + getLevelForXP(currentExp[9]) + "", 4027);
				sendFrame126("" + currentExp[9] + "", 4110);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[9]) + 1) + "", 4111);
				break;

			case 10:
				sendFrame126("" + currentStats[10] + "", 4032);
				sendFrame126("" + getLevelForXP(currentExp[10]) + "", 4033);
				sendFrame126("" + currentExp[10] + "", 4128);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[10]) + 1) + "", 4129);
				break;

			case 11:
				sendFrame126("" + currentStats[11] + "", 4036);
				sendFrame126("" + getLevelForXP(currentExp[11]) + "", 4037);
				sendFrame126("" + currentExp[11] + "", 4140);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[11]) + 1) + "", 4141);
				break;

			case 12:
				sendFrame126("" + currentStats[12] + "", 4024);
				sendFrame126("" + getLevelForXP(currentExp[12]) + "", 4025);
				sendFrame126("" + currentExp[12] + "", 4104);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[12]) + 1) + "", 4105);
				break;

			case 13:
				sendFrame126("" + currentStats[13] + "", 4030);
				sendFrame126("" + getLevelForXP(currentExp[13]) + "", 4031);
				sendFrame126("" + currentExp[13] + "", 4122);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[13]) + 1) + "", 4123);
				break;

			case 14:
				sendFrame126("" + currentStats[14] + "", 4028);
				sendFrame126("" + getLevelForXP(currentExp[14]) + "", 4029);
				sendFrame126("" + currentExp[14] + "", 4116);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[14]) + 1) + "", 4117);
				break;

			case 15:
				sendFrame126("" + currentStats[15] + "", 4020);
				sendFrame126("" + getLevelForXP(currentExp[15]) + "", 4021);
				sendFrame126("" + currentExp[15] + "", 4092);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[15]) + 1) + "", 4093);
				break;

			case 16:
				sendFrame126("" + currentStats[16] + "", 4018);
				sendFrame126("" + getLevelForXP(currentExp[16]) + "", 4019);
				sendFrame126("" + currentExp[16] + "", 4086);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[16]) + 1) + "", 4087);
				break;

			case 17:
				sendFrame126("" + currentStats[17] + "", 4022);
				sendFrame126("" + getLevelForXP(currentExp[17]) + "", 4023);
				sendFrame126("" + currentExp[17] + "", 4098);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[17]) + 1) + "", 4099);
				break;

			case 18:
				sendFrame126("" + currentStats[18] + "", 12166);
				sendFrame126("" + getLevelForXP(currentExp[18]) + "", 12167);
				sendFrame126("" + currentExp[18] + "", 12171);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[18]) + 1) + "", 12172);
				break;

			case 19:
				sendFrame126("" + currentStats[19] + "", 13926);
				sendFrame126("" + getLevelForXP(currentExp[19]) + "", 13927);
				sendFrame126("" + currentExp[19] + "", 13921);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[19]) + 1) + "", 13922);
				break;

			case 20: // runecraft
				sendFrame126("" + currentStats[20] + "", 4152);
				sendFrame126("" + getLevelForXP(currentExp[20]) + "", 4153);
				sendFrame126("" + currentExp[20] + "", 4157);
				sendFrame126("" + getXPForLevel(getLevelForXP(currentExp[20]) + 1) + "", 4159);
				break;

			// hunter is 21
		}
	}
	/**
	 * Sets the music volume based on slider value (0-10 scale)
	 * @param volume The volume level from 0 (muted) to 10 (max)
	 */
	public void setMusicVolume(int volume) {
	    System.out.println("=== setMusicVolume called with: " + volume + " ===");
		
		if (volume == 0) {
			// Mute music
			this.musicEnabled = false;
			this.stopMidi();
		} else {
			// Enable music if it was disabled
			boolean wasDisabled = !this.musicEnabled;
			this.musicEnabled = true;
			
			// Convert 0-10 scale to MIDI volume with better curve
			// Raised the minimum so quietest setting is more usable
			int midiVolume;
			switch(volume) {
				case 1: midiVolume = 40; break;   // Quiet but audible
				case 2: midiVolume = 55; break;   // Low
				case 3: midiVolume = 65; break;   // Medium-low
				case 4: midiVolume = 75; break;   // Medium
				case 5: midiVolume = 85; break;   // Medium-high
				case 6: midiVolume = 95; break;   // High
				case 7: midiVolume = 105; break;  // Higher
				case 8: midiVolume = 112; break;  // Very high
				case 9: midiVolume = 120; break;  // Almost max
				case 10: midiVolume = 127; break; // Maximum
				default: midiVolume = 85; break;  // Default to medium-high
			}
			
			System.out.println("Setting MIDI volume to: " + midiVolume); // Debug
			
			// Apply the volume using Signlink's setVolume method
			Signlink.setVolume(midiVolume);
			
			// If music was disabled and we just enabled it, restart the current song
			if (wasDisabled && this.currentSong > 0) {
				this.nextSong = this.currentSong;
				this.songChanging = true;
				onDemandFetcher.provide(2, this.nextSong);
			}
		}
	}

	/**
	 * Sets the sound effect volume based on slider value (0-10 scale)
	 * @param volume The volume level from 0 (muted) to 10 (max)
	 */
	public void setSoundVolume(int volume) {
	    // SoundPlayer.volume is inverted: 0 = max volume, 10 = mute
	    // So we need to invert our 0-10 scale
	    int invertedVolume = 10 - volume;
	    SoundPlayer.setVolume(invertedVolume);
	    System.out.println("Set sound volume to: " + volume + " (SoundPlayer.volume=" + invertedVolume + ")");
	}
	/**
	 * Sets the area sound volume based on slider value (0-10 scale)
	 * @param volume The volume level from 0 (muted) to 10 (max)
	 */
	public void setAreaSoundVolume(int volume) {
		// Convert 0-10 scale to 0-127 scale
		// Area sounds typically use the same volume system as sound effects
		// If you have a separate areaSoundVolume field, uncomment the line below
		// this.areaSoundVolume = (int) ((volume / 10.0) * 127);
		
		// For now, we'll just store it in preferences
		// The actual implementation depends on how your area sounds are played
	}
}
