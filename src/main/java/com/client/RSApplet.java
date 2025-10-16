package com.client;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.client.features.KeyboardAction;
import com.client.features.gameframe.ScreenMode;
import com.client.graphics.interfaces.RSInterface;
import com.client.graphics.interfaces.impl.Keybinding;
import com.client.graphics.interfaces.impl.SettingsTabWidget;
import com.client.graphics.interfaces.impl.Slider;

public class RSApplet extends Applet implements Runnable, MouseListener, MouseMotionListener, MouseWheelListener,
		KeyListener, FocusListener, WindowListener {

	private static final long serialVersionUID = 1473917011474991756L;
	public static int clickType;
	public final int LEFT = 0;
	public final int RIGHT = 1;
	public final int DRAG = 2;
	public final int RELEASED = 3;
	public final int MOVE = 4;
	boolean mouseWheelDown;
	int mouseWheelX, mouseWheelY;

	final void initClientFrame(int clientWidth, int clientHeight) {
		myWidth = clientWidth;
		myHeight = clientHeight;
		graphics = getGameComponent().getGraphics();
		fullGameScreen = new RSImageProducer(myWidth, myHeight, getGameComponent());
		startRunnable(this, 1);
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		int rotation = event.getWheelRotation();
		if (!handleInterfaceScrolling(event)) {
			if (getMouseX() > 0 && getMouseX() < 512 && getMouseY() > Client.currentGameHeight - 165
					&& getMouseY() < Client.currentGameHeight - 25) {
				int scrollPos = Client.anInt1089;
				scrollPos -= rotation * 30;
				if (scrollPos < 0)
					scrollPos = 0;

				if (Client.anInt1089 != scrollPos) {
					Client.anInt1089 = scrollPos;
				}
			} else if (Client.loggedIn) {

				/** ZOOMING **/
				boolean zoom = Client.currentScreenMode == ScreenMode.FIXED ? (getMouseX() < 512)
						: (getMouseX() < Client.currentGameWidth - 200);

				boolean mapZoom = Client.currentScreenMode == ScreenMode.FIXED ? (getMouseX() > 526 && getMouseY() < 150)
						: ((Client.currentGameWidth - getMouseX()) < 180 && (Client.currentGameHeight - getMouseY()) > 635);

				if (Client.openInterfaceID == -1 && zoom) {
					int setting = 1;
					if (Client.cameraZoom > 2000) {
						setting = 6;
					} else if (Client.cameraZoom > 1800) {
						setting = 5;
					} else if (Client.cameraZoom > 1400) {
						setting = 5;
					} else if (Client.cameraZoom > 1000) {
						setting = 4;
					} else if (Client.cameraZoom > 500) {
						setting = 3;
					} else if (Client.cameraZoom > 50) {
						setting = 2;
					}
					Client.cameraZoom += rotation * (35 * setting);

					int max_zoom_1 = (Client.currentScreenMode == ScreenMode.FIXED ? -200 : -300);
					if (Client.cameraZoom < max_zoom_1) {
						Client.cameraZoom = max_zoom_1;
					}
					if (Client.cameraZoom > 3000) {
						Client.cameraZoom = 3000;
					}
					if (Client.currentScreenMode != ScreenMode.FIXED) {
						if (Client.cameraZoom < 150) {
							Client.cameraZoom = 150;
						}
					}

					RSInterface.interfaceCache[SettingsTabWidget.ZOOMTOGGLE].active = true;
					RSInterface.interfaceCache[SettingsTabWidget.ZOOM_SLIDER].slider.setValue(Client.cameraZoom);
				}
			}
		}

	}

	public boolean handleInterfaceScrolling(MouseWheelEvent event) {
		int rotation = event.getWheelRotation();
		int offsetX;
		int offsetY;

		/* Tab interface scrolling */
		int tabInterfaceID = Client.tabInterfaceIDs[Client.tabID];
		if (tabInterfaceID != -1) {
			offsetX = Client.currentScreenMode.equals(ScreenMode.FIXED) ? 765 - 218 : Client.currentGameWidth - 197;
			offsetY = Client.currentScreenMode.equals(ScreenMode.FIXED) ? 503 - 298
					: Client.currentGameHeight - (Client.currentGameWidth >= 960 ? 37 : 74) - 267;

			handleTabInterfaceScrolling(RSInterface.get(tabInterfaceID), rotation, offsetX, offsetY);
		}

		/* Main interface scrolling */
		if (Client.openInterfaceID != -1) {
			offsetX = Client.currentScreenMode.equals(ScreenMode.FIXED) ? 4
					: (Client.currentGameWidth / 2) - 356;
			offsetY = Client.currentScreenMode.equals(ScreenMode.FIXED)? 4
					: (Client.currentGameHeight / 2) - 230;
			return handleMainInterfaceScrolling(Client.openInterfaceID, offsetX, offsetY, rotation);
		}

		return false;
	}

	private void handleTabInterfaceScrolling(RSInterface tab, int rotation, int offsetX, int offsetY) {
		int positionX = 0;
		int positionY = 0;
		int width = 0;
		int height = 0;
		int childID = 0;


		if (tab.children != null) {
			for (int index = 0; index < tab.children.length; index++) {
				RSInterface child = RSInterface.interfaceCache[tab.children[index]];
				handleTabInterfaceScrolling(child, rotation, offsetX + tab.childX[index], offsetY + tab.childY[index]);
				if (child.scrollMax > 0) {
					childID = index;
					positionX = tab.childX[index];
					positionY = tab.childY[index];
					width = child.width + 16;
					height = child.height;
					break;
				}
			}
		}
		if (getMouseX() > offsetX + positionX && getMouseY() > offsetY + positionY && getMouseX() < offsetX + positionX + width
				&& getMouseY() < offsetY + positionY + height) {
			RSInterface rsInterface = RSInterface.interfaceCache[tab.children[childID]];
			rsInterface.scrollPosition += rotation * 30;
			if (rsInterface.scrollPosition < 0)
				rsInterface.scrollPosition = 0;
			if (rsInterface.scrollPosition > rsInterface.scrollMax - rsInterface.height)
				rsInterface.scrollPosition = rsInterface.scrollMax - rsInterface.height;
			Client.tabAreaAltered = true;
			Client.needDrawTabArea = true;
			return;
		}
	}

	private boolean handleMainInterfaceScrolling(int interfaceId, int offsetX, int offsetY, int rotation) {
		RSInterface rsi = RSInterface.interfaceCache[interfaceId];
		if (rsi.children != null) {
			for (int index = 0; index < rsi.children.length; index++) {
				handleMainInterfaceScrolling(rsi.children[index], offsetX + rsi.childX[index], offsetY + rsi.childY[index], rotation);
				if (RSInterface.interfaceCache[rsi.children[index]].scrollMax <= 0) {
					continue;
				}
				if (getMouseX() > offsetX + rsi.childX[index] && getMouseY() > offsetY + rsi.childY[index]
						&& getMouseX() < offsetX + rsi.childX[index] + RSInterface.interfaceCache[rsi.children[index]].width + 16
						&& getMouseY() < offsetY + rsi.childY[index]
						+ RSInterface.interfaceCache[rsi.children[index]].height) {

					RSInterface rsInterface = RSInterface.interfaceCache[rsi.children[index]];
					rsInterface.scrollPosition += rotation * 30;
					if (rsInterface.scrollPosition < 0)
						rsInterface.scrollPosition = 0;
					if (rsInterface.scrollPosition > rsInterface.scrollMax - rsInterface.height)
						rsInterface.scrollPosition = rsInterface.scrollMax - rsInterface.height;
					return true;
				}
			}
		}

		return false;
	}

	void drawLoadingText(int percentage, String s, int downloadSpeed, int secondsRemaining) {
		while (graphics == null) {
			graphics = getGameComponent().getGraphics();
			try {
				getGameComponent().repaint();
			} catch (Exception _ex) {
			}
			try {
				Thread.sleep(1000L);
			} catch (Exception _ex) {
			}
		}
		Font font = new Font("Helvetica", 1, 13);
		FontMetrics fontmetrics = getGameComponent().getFontMetrics(font);
		Font font1 = new Font("Helvetica", 0, 13);
		getGameComponent().getFontMetrics(font1);
		getGameComponent().setMinimumSize(ScreenMode.FIXED.getDimensions());
		//if (shouldClearScreen) {
		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, myWidth, myHeight);
		shouldClearScreen = false;
		//}
		Color color = new Color(140, 17, 17);
		int j = myHeight / 2 - 18;
		graphics.setColor(color);
		graphics.drawRect(myWidth / 2 - 152, j, 304, 34);
		graphics.fillRect(myWidth / 2 - 150, j + 2, percentage * 3, 30);
		graphics.setColor(Color.black);
		graphics.fillRect((myWidth / 2 - 150) + percentage * 3, j + 2, 300 - percentage * 3, 30);
		graphics.setFont(font);
		graphics.setColor(Color.white);
		graphics.drawString(s, (myWidth - fontmetrics.stringWidth(s)) / 2,
				j + 22);


		if(downloadSpeed != -1 && secondsRemaining != -1) {
			String text = "Current download speed: " + downloadSpeed + "Kb/s - Seconds remaining: " + secondsRemaining;
			graphics.drawString(text, (myWidth - fontmetrics.stringWidth(text)) / 2, j + 70);
		}
	}
	@Override
	public void run() {
		getGameComponent().addMouseListener(this);
		getGameComponent().addMouseMotionListener(this);
		getGameComponent().addKeyListener(this);
		getGameComponent().addFocusListener(this);
		getGameComponent().addMouseWheelListener(this);
		// drawLoadingText(0, "Loading...");
		startUp();
		int i = 0;
		int j = 256;
		int k = 1;
		int i1 = 0;
		int j1 = 0;
		for (int k1 = 0; k1 < 10; k1++)
			aLongArray7[k1] = System.currentTimeMillis();

		System.currentTimeMillis();
		while (anInt4 >= 0) {
			if (anInt4 > 0) {
				anInt4--;
				if (anInt4 == 0) {
					exit();
					return;
				}
			}
			int i2 = j;
			int j2 = k;
			j = 300;
			k = 1;
			long l1 = System.currentTimeMillis();
			if (aLongArray7[i] == 0L) {
				j = i2;
				k = j2;
			} else if (l1 > aLongArray7[i])
				j = (int) (2560 * delayTime / (l1 - aLongArray7[i]));
			if (j < 25)
				j = 25;
			if (j > 256) {
				j = 256;
				k = (int) (delayTime - (l1 - aLongArray7[i]) / 10L);
			}
			if (k > delayTime)
				k = delayTime;
			aLongArray7[i] = l1;
			i = (i + 1) % 10;
			if (k > 1) {
				for (int k2 = 0; k2 < 10; k2++)
					if (aLongArray7[k2] != 0L)
						aLongArray7[k2] += k;

			}
			if (k < minDelay)
				k = minDelay;
			try {
				Thread.sleep(k);
			} catch (InterruptedException _ex) {
				j1++;
			}
			for (; i1 < 256; i1 += j) {
				clickMode3 = clickMode1;
				setSaveClickX(clickX);
				setSaveClickY(clickY);
				aLong29 = clickTime;
				clickMode1 = 0;
				processGameLoop();
				readIndex = writeIndex;
			}

			i1 &= 0xff;
			if (delayTime > 0)
				setFps((1000 * j) / (delayTime * 256));
			processDrawing();
			if (shouldDebug) {
				//System.out.println("ntime:" + l1);
				for (int l2 = 0; l2 < 10; l2++) {
					int i3 = ((i - l2 - 1) + 20) % 10;
					//System.out.println("otim" + i3 + ":" + aLongArray7[i3]);
				}

				//System.out.println("fps:" + getFps() + " ratio:" + j + " count:" + i1);
				//System.out.println("del:" + k + " deltime:" + delayTime + " mindel:" + minDelay);
				//System.out.println("intex:" + j1 + " opos:" + i);
				shouldDebug = false;
				j1 = 0;
			}
		}
		if (anInt4 == -1)
			exit();
	}

	private void exit() {
		anInt4 = -2;
		cleanUpForQuit();
		System.exit(0);
	}

	final void method4(int i) {
		delayTime = 1000 / i;
	}

	@Override
	public final void start() {
		if (anInt4 >= 0)
			anInt4 = 0;
	}

	@Override
	public final void stop() {
		if (anInt4 >= 0)
			anInt4 = 4000 / delayTime;
	}

	@Override
	public final void destroy() {
		anInt4 = -1;
		try {
			Thread.sleep(5000L);
		} catch (Exception _ex) {
		}
		if (anInt4 == -1)
			exit();
	}

	@Override
	public final void update(Graphics g) {
		if (graphics == null)
			graphics = g;
		shouldClearScreen = true;
		raiseWelcomeScreen();
	}

	@Override
	public final void paint(Graphics g) {
		if (graphics == null)
			graphics = g;
		shouldClearScreen = true;
		raiseWelcomeScreen();
	}

	@Override
	public final void mousePressed(MouseEvent mouseevent) {
	    int i = mouseevent.getX();
	    int j = mouseevent.getY();
	    ////System.out.println("DEBUG: mousePressed called at (" + i + "," + j + ") button=" + mouseevent.getButton());
	    
	    idleTime = 0;
	    clickX = i;
	    clickY = j;
	    clickTime = System.currentTimeMillis();
	    
	    if (SwingUtilities.isMiddleMouseButton(mouseevent)) {
	        mouseWheelDown = true;
	        mouseWheelX = i;
	        mouseWheelY = j;
	        return;
	    }
	    
	    if (SwingUtilities.isRightMouseButton(mouseevent)) {
	        clickType = RIGHT;
	        clickMode1 = 2;
	        clickMode2 = 2;
	        //System.out.println("DEBUG: Right mouse button detected");
	    } else if (SwingUtilities.isLeftMouseButton(mouseevent)) {
	        clickType = LEFT;
	        clickMode1 = 1;
	        clickMode2 = 1;
	        //System.out.println("DEBUG: Left mouse button detected");
	    }
	    
	    // Forward mouse events to tile markers plugin
	    //System.out.println("DEBUG: Checking tile markers plugin...");
	    if (Client.tileMarkersPlugin != null) {
	        //System.out.println("DEBUG: Plugin exists, enabled=" + Client.tileMarkersPlugin.isEnabled());
	        if (Client.tileMarkersPlugin.isEnabled()) {
	            try {
	                //System.out.println("DEBUG: About to call handleMouseClick");
	                Client.tileMarkersPlugin.handleMouseClick(mouseevent);
	            } catch (Exception e) {
	                System.err.println("Error handling tile markers mouse click: " + e.getMessage());
	                e.printStackTrace();
	            }
	        }
	    } else {
	        //System.out.println("DEBUG: Tile markers plugin is null!");
	    }
	    
	    Slider.handleSlider(getMouseX(), getMouseY());
	}

	@Override
	public final void mouseReleased(MouseEvent mouseevent) {
		idleTime = 0;
		clickMode2 = 0;
		mouseWheelDown = false;
		clickType = RELEASED;
		sliderShowAlpha = false;
	}

	@Override
	public final void mouseClicked(MouseEvent mouseevent) {
	}

	@Override
	public final void mouseEntered(MouseEvent mouseevent) {
	}

	@Override
	public final void mouseExited(MouseEvent mouseevent) {
		idleTime = 0;
		setMouseX(-1);
		setMouseY(-1);
	}
	
	public static boolean sliderShowAlpha = false;

	@Override
	public final void mouseDragged(MouseEvent mouseevent) {
		int i = mouseevent.getX();
		int j = mouseevent.getY();
		if (System.currentTimeMillis() - clickTime >= 250L || Math.abs(getRawSaveClickX() - i) > 5
				|| Math.abs(getRawSaveClickY() - j) > 5) {
			idleTime = 5;
			setMouseX(i);
			setMouseY(j);
		}
		if (mouseWheelDown) {
			j = mouseWheelX - mouseevent.getX();
			int k = mouseWheelY - mouseevent.getY();
			Client.getInstance().mouseWheelDragged(j, -k);
			mouseWheelX = mouseevent.getX();
			mouseWheelY = mouseevent.getY();
			return;
		}
		if (clickType != 0) {
			sliderShowAlpha = true;
		}
		clickType = DRAG;
		Slider.handleSlider(getMouseX(), getMouseY());
	}

	@Override
	public void mouseMoved(MouseEvent mouseevent) {
		int i = mouseevent.getX();
		int j = mouseevent.getY();
		if (System.currentTimeMillis() - clickTime >= 250L || Math.abs(getRawSaveClickX() - i) > 5
				|| Math.abs(getRawSaveClickY() - j) > 5) {
			idleTime = 3;
			setMouseX(i);
			setMouseY(j);
		}
		clickType = MOVE;
	}

	public static boolean hotKeyToggle = true;

	@Override
	public void keyPressed(KeyEvent keyevent) {
		idleTime = 0;
		int i = keyevent.getKeyCode();
		int j = keyevent.getKeyChar();

		if (keyevent.isShiftDown()) {
			Client.shiftDown = true;
		}
		if (i == KeyEvent.VK_SPACE) {
			if (Client.backDialogID == 979 || Client.backDialogID == 968 || Client.backDialogID == 973
					|| Client.backDialogID == 986 || Client.backDialogID == 306 || Client.backDialogID == 4887
					|| Client.backDialogID == 4893 || Client.backDialogID == 356 || Client.backDialogID == 359
					|| Client.backDialogID == 310 || Client.backDialogID == 4882 || Client.backDialogID == 4900) {
				Client.stream.createFrame(40);
				Client.stream.writeWord(4892);
				// Client.setDialogueOptionsShowing(true);
			}
		}
		// if (i == KeyEvent.VK_SPACE) {
		// Client
		// }

		if (i == 192 && Client.myPlayer != null && Client.myPlayer.isAdminRights()) {
			Client.instance.devConsole.console_open = !Client.instance.devConsole.console_open;
		}

		if (keyevent.isControlDown()) {
			if (i == KeyEvent.VK_SPACE) {
				Client.continueDialogue();
			}
			if (i == KeyEvent.VK_1 || i == KeyEvent.VK_NUMPAD1) {
				Client.dialogueOptions("one");
			}
			if (i == KeyEvent.VK_2 || i == KeyEvent.VK_NUMPAD2) {
				Client.dialogueOptions("two");
			}
			if (i == KeyEvent.VK_3 || i == KeyEvent.VK_NUMPAD3) {
				Client.dialogueOptions("three");
			}
			if (i == KeyEvent.VK_4 || i == KeyEvent.VK_NUMPAD4) {
				Client.dialogueOptions("four");
			}
			if (i == KeyEvent.VK_5 || i == KeyEvent.VK_NUMPAD5) {
				Client.dialogueOptions("five");
			}
			switch (i) {
			case KeyEvent.VK_V:
				Client.inputString += Client.getClipboardContents();
				Client.inputTaken = true;
				break;

			case KeyEvent.VK_T:
				Client.teleportInterface();
				break;

			}
		}

		if (i == KeyEvent.VK_F1) {
			Keybinding.isBound(KeyEvent.VK_F1);
		} else if (i == KeyEvent.VK_F2) {
			Keybinding.isBound(KeyEvent.VK_F2);
		} else if (i == KeyEvent.VK_F3) {
			Keybinding.isBound(KeyEvent.VK_F3);
		} else if (i == KeyEvent.VK_F4) {
			Keybinding.isBound(KeyEvent.VK_F4);
		} else if (i == KeyEvent.VK_F5) {
			Keybinding.isBound(KeyEvent.VK_F5);
		} else if (i == KeyEvent.VK_F6) {
			Keybinding.isBound(KeyEvent.VK_F6);
		} else if (i == KeyEvent.VK_F7) {
			Keybinding.isBound(KeyEvent.VK_F7);
		} else if (i == KeyEvent.VK_F8) {
			Keybinding.isBound(KeyEvent.VK_F8);
		} else if (i == KeyEvent.VK_F9) {
			Keybinding.isBound(KeyEvent.VK_F9);
		} else if (i == KeyEvent.VK_F10) {
			Keybinding.isBound(KeyEvent.VK_F10);
		} else if (i == KeyEvent.VK_F11) {
			Keybinding.isBound(KeyEvent.VK_F11);
		} else if (i == KeyEvent.VK_F12) {
			Keybinding.isBound(KeyEvent.VK_F12);
		}

		if (i == KeyEvent.VK_ESCAPE) {
			Keybinding.isBound(KeyEvent.VK_ESCAPE);
			Client.closeInterface();
		}
		/**
		if (hotKeyToggle == true) {
			if (i == KeyEvent.VK_F5) {
				Client.setTab(0);
			} else if (i == KeyEvent.VK_F11) {
				Client.setTab(1);
			} else if (i == KeyEvent.VK_F12) {
				Client.setTab(2);
			} else if (i == KeyEvent.VK_F1) {
				Client.setTab(3);
			} else if (i == KeyEvent.VK_F2) {
				Client.setTab(4);
			} else if (i == KeyEvent.VK_F3) {
				Client.setTab(5);
			} else if (i == KeyEvent.VK_F4) {
				Client.setTab(6);
			} else if (i == KeyEvent.VK_F8) {
				Client.setTab(7);
			} else if (i == KeyEvent.VK_F9) {
				Client.setTab(8);
			} else if (i == KeyEvent.VK_F10) {
				Client.setTab(9);
			} else if (i == KeyEvent.VK_F11) {
				Client.setTab(10);
			} else if (i == KeyEvent.VK_F12) {
				Client.setTab(11);
			}
		} else {
			if (i == KeyEvent.VK_F1) {
				Client.setTab(3);
			} else if (i == KeyEvent.VK_F2) {
				Client.setTab(1);
			} else if (i == KeyEvent.VK_F3) {
				Client.setTab(2);
			} else if (i == KeyEvent.VK_F4) {
				Client.setTab(3);
			} else if (i == KeyEvent.VK_F5) {
				Client.setTab(4);
			} else if (i == KeyEvent.VK_F6) {
				Client.setTab(5);
			} else if (i == KeyEvent.VK_F7) {
				Client.setTab(6);
			} else if (i == KeyEvent.VK_F8) {
				Client.setTab(7);
			} else if (i == KeyEvent.VK_F9) {
				Client.setTab(8);
			} else if (i == KeyEvent.VK_F10) {
				Client.setTab(9);
			} else if (i == KeyEvent.VK_F11) {
				Client.setTab(10);
			} else if (i == KeyEvent.VK_F12) {
				Client.setTab(11);
			}
		}
**/
		if (keyevent.isControlDown()) {
			Client.controlIsDown = true;
		}

		for(KeyboardAction action : KeyboardAction.values()) {
			if (Client.loggedIn) {
				if (action.canActivate(keyevent.getKeyCode(), keyevent.isControlDown(),
						keyevent.isShiftDown(), keyevent.isAltDown())) {
					Client.instance.sendKeyboardAction(action.getAction());
					break;
				}
			}
		}

		if (j < 30)
			j = 0;
		if (i == 37)
			j = 1;
		if (i == 39)
			j = 2;
		if (i == 38)
			j = 3;
		if (i == 40)
			j = 4;
		if (i == 17)
			j = 5;
		if (i == 8)
			j = 8;
		if (i == 127)
			j = 8;
		if (i == 9)
			j = 9;
		if (i == 10)
			j = 10;
		if (i >= 112 && i <= 123)
			j = (1008 + i) - 112;
		if (i == 36)
			j = 1000;
		if (i == 35)
			j = 1001;
		if (i == 33)
			j = 1002;
		if (i == 34)
			j = 1003;
		if (j > 0 && j < 128)
			keyArray[j] = 1;
		if (j > 4) {
			charQueue[writeIndex] = j;
			writeIndex = writeIndex + 1 & 0x7f;
		}
		if (Client.tileMarkersPlugin != null && Client.tileMarkersPlugin.isEnabled()) {
		    try {
		        Client.tileMarkersPlugin.handleKeyPress(keyevent);
		    } catch (Exception e) {
		        System.err.println("Error handling tile markers key press: " + e.getMessage());
		    }
		}
	}

	@Override
	public final void keyReleased(KeyEvent keyevent) {
		idleTime = 0;
		int i = keyevent.getKeyCode();
		char c = keyevent.getKeyChar();
		if (c < '\036')
			c = '\0';
		if (i == 37)
			c = '\001';
		if (i == 39)
			c = '\002';
		if (i == 38)
			c = '\003';
		if (i == 40)
			c = '\004';
		if (i == 17)
			c = '\005';
		if (i == 8)
			c = '\b';
		if (i == 127)
			c = '\b';
		if (i == 9)
			c = '\t';
		if (i == 10)
			c = '\n';
		if (i == KeyEvent.VK_SHIFT) {
			Client.shiftDown = false;
		}
		if (i == KeyEvent.VK_CONTROL) {
			Client.controlIsDown = false;
		}
		if (c > 0 && c < '\200')
			keyArray[c] = 0;
	}

	@Override
	public final void keyTyped(KeyEvent keyevent) {
	}

	final int readChar(int dummy) {
		while (dummy >= 0) {
			for (int j = 1; j > 0; j++)
				;
		}
		int k = -1;
		if (writeIndex != readIndex) {
			k = charQueue[readIndex];
			readIndex = readIndex + 1 & 0x7f;
		}
		return k;
	}

	@Override
	public final void focusGained(FocusEvent focusevent) {
		awtFocus = true;
		shouldClearScreen = true;
		raiseWelcomeScreen();
		Client.controlIsDown = false;
		Client.shiftDown = false;
	}

	@Override
	public final void focusLost(FocusEvent focusevent) {
		awtFocus = false;
		for (int i = 0; i < 128; i++)
			keyArray[i] = 0;
		Client.controlIsDown = false;
		Client.shiftDown = false;
	}

	@Override
	public final void windowActivated(WindowEvent windowevent) {
	}

	@Override
	public final void windowClosed(WindowEvent windowevent) {
	}

	@Override
	public void windowClosing(WindowEvent windowevent) {
		String options[] = { "Yes", "No" };
		int userPrompt = JOptionPane.showOptionDialog(null, "Are you sure you wish to exit?", Configuration.CLIENT_TITLE,
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		if (userPrompt == JOptionPane.YES_OPTION) {
			destroy();
			System.exit(0);
		}
	}

	@Override
	public final void windowDeactivated(WindowEvent windowevent) {
	}

	@Override
	public final void windowDeiconified(WindowEvent windowevent) {
	}

	@Override
	public final void windowIconified(WindowEvent windowevent) {
	}

	@Override
	public final void windowOpened(WindowEvent windowevent) {
	}

	void startUp() {
	}

	void processGameLoop() {
	}

	void cleanUpForQuit() {
	}

	void processDrawing() {
	}

	void raiseWelcomeScreen() {
	}

	public Component getGameComponent() {
		return this;
	}

	public void startRunnable(Runnable runnable, int priority) {
		Thread thread = new Thread(runnable);
		thread.start();
		thread.setPriority(priority);
	}

	void drawLoadingScreen(int percentage, String s, int downloadSpeed, int secondsRemaining) {
		while (graphics == null) {
			graphics = getGameComponent().getGraphics();
			try {
				getGameComponent().repaint();
			} catch (Exception _ex) {
			}
			try {
				Thread.sleep(1000L);
			} catch (Exception _ex) {
			}
		}
		Font font = new Font("Helvetica", 1, 13);
		FontMetrics fontmetrics = getGameComponent().getFontMetrics(font);
		Font font1 = new Font("Helvetica", 0, 13);
		getGameComponent().getFontMetrics(font1);
		// if (shouldClearScreen) {
		graphics.setColor(Color.black);
		graphics.fillRect(0, 0, myWidth, myHeight);
		shouldClearScreen = false;
		// }
		Color color = new Color(140, 17, 17);
		int j = myHeight / 2 - 18;
		graphics.setColor(color);
		graphics.drawRect(myWidth / 2 - 152, j, 304, 34);
		graphics.fillRect(myWidth / 2 - 150, j + 2, percentage * 3, 30);
		graphics.setColor(Color.black);
		graphics.fillRect((myWidth / 2 - 150) + percentage * 3, j + 2, 300 - percentage * 3, 30);
		graphics.setFont(font);
		graphics.setColor(Color.white);
		graphics.drawString(s, (myWidth - fontmetrics.stringWidth(s)) / 2, j + 22);

		if (downloadSpeed != -1 && secondsRemaining != -1) {
			String text = "Current download speed: " + downloadSpeed + "Kb/s - Seconds remaining: " + secondsRemaining;
			graphics.drawString(text, (myWidth - fontmetrics.stringWidth(text)) / 2, j + 70);
		}
	}

	RSApplet() {
		delayTime = 20;
		minDelay = 1;
		aLongArray7 = new long[10];
		shouldDebug = false;
		shouldClearScreen = true;
		awtFocus = true;
		keyArray = new int[128];
		charQueue = new int[128];
	}

	private int anInt4;
	private int delayTime;
	int minDelay;
	private final long[] aLongArray7;
	private int fps;
	boolean shouldDebug;
	int myWidth;
	int myHeight;
	Graphics graphics;
	RSImageProducer fullGameScreen;
	@SuppressWarnings("unused")
	private boolean shouldClearScreen;
	boolean awtFocus;
	int idleTime;
	int clickMode2;
	private int mouseX;
	private int mouseY;
	private int clickMode1;
	protected int clickX;
	protected int clickY;
	private long clickTime;
	int clickMode3;
	private int saveClickX;
	private int saveClickY;
	long aLong29;
	final int[] keyArray;
	private final int[] charQueue;
	private int readIndex;
	private int writeIndex;
	public static int anInt34;

	public int getFps() {
		return fps;
	}

	public void setFps(int fps) {
		this.fps = fps;
	}

	public int getMouseX() {
		return (int) ClientWindow.getStretchedMouseCoordinates(new Point(mouseX, mouseY)).getX();
	}

	public int getRawMouseX() {
		return mouseX;
	}

	public void setMouseX(int mouseX) {
		this.mouseX = mouseX;
	}

	public int getMouseY() {
		return (int) ClientWindow.getStretchedMouseCoordinates(new Point(mouseX, mouseY)).getY();
	}

	public int getRawMouseY() {
		return mouseY;
	}

	public void setMouseY(int mouseY) {
		this.mouseY = mouseY;
	}

	public int getSaveClickX() {
		return (int) ClientWindow.getStretchedMouseCoordinates(new Point(saveClickX, saveClickY)).getX();
	}

	public int getRawSaveClickX() {
		return saveClickX;
	}

	public void setSaveClickX(int saveClickX) {
		this.saveClickX = saveClickX;
	}

	public int getSaveClickY() {
		return (int) ClientWindow.getStretchedMouseCoordinates(new Point(saveClickX, saveClickY)).getY();
	}

	public int getRawSaveClickY() {
		return saveClickY;
	}

	public void setSaveClickY(int saveClickY) {
		this.saveClickY = saveClickY;
	}
}
