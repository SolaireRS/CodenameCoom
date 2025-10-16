package com.client.graphics.interfaces.impl;

import com.client.TextDrawingArea;
import com.client.graphics.interfaces.RSInterface;

public class GrandExchange extends RSInterface {
	
	/**
	 * Interfaces:
	 * 
	 * Main GE Interface	:	35000
	 * Buy Interface		:	35600
	 * Sell Interface		:	35650
	 * Collect Interface	:	36000
	 * 
	 * Offer status interfaces
	 * Box 0	:	35800
	 * Box 1	:	35820
	 * Box 2	:	35840
	 * Box 3	:	35860
	 * Box 4	:	35880
	 * Box 5	:	35900
	 * Box 6	:	35920
	 * Box 7	:	35940
	 * 
	 */
	
	private static int GREEN_COLOR = 0x005F00;
	
	private static int RED_COLOR = 0x8F0000;
	
	public static void initializeInterfaces(TextDrawingArea[] tda) {
		mainInterface(tda);
		addBuyAndSellButtons(tda);
		addItemBoxes(tda);

		buyAndSellBackground(tda);
		buyScreen(tda);
		sellScreen(tda);

		offerStatusBackground(tda);
		addOfferStatusInterfaces(tda);

		collectInterface(tda);
	}
	
	public static int[] grandExchangeBuyAndSellBoxIds = new int[] {
		35100, 35120, 35140, 35160,
		35180, 35200, 35220, 35240
	};
	
	public static int[] grandExchangeItemBoxIds = new int[] {
		35300, 35320, 35340, 35360,
		35380, 35400, 35420, 35440
	};
	
	public static int[] grandExchangeOfferStatusInterfaceIds = new int[] {
		35800, 35820, 35840, 35860,
		35880, 35900, 35920, 35940
	};
	
	private static void mainInterface(TextDrawingArea[] tda) {
		RSInterface widget = addInterface(35000);
		
		addSprite(35001, 1, "Interfaces/GE/IMAGE");
		
		addHoverButton(35002, "Interfaces/GE/CLOSE", 1, 21, 21, "Close", -1, 35003, 3);
		addHoveredButton(35003, "Interfaces/GE/CLOSE", 2, 21, 21, 35004);
		
		addText(35005, "Grand Exchange", tda, 2, 0xFF981F, true, true);
		
		addGEBox(35010, 0);
		addGEBox(35011, 1);
		addGEBox(35012, 2);
		addGEBox(35013, 3);
		addGEBox(35014, 4);
		addGEBox(35015, 5);
		addGEBox(35016, 6);
		addGEBox(35017, 7);
		
		setChildren(12, widget);
		setBounds(35001, 20, 13, 0, widget);
		setBounds(35002, 476, 20, 1, widget);
		setBounds(35003, 476, 20, 2, widget);
		setBounds(35005, 360, 23, 3, widget);
		
		setBounds(35010, 29, 75, 4, widget);
		setBounds(35011, 146, 75, 5, widget);
		setBounds(35012, 363, 75, 6, widget);
		setBounds(35013, 380, 75, 7, widget);
		
		setBounds(35014, 29, 196, 8, widget);
		setBounds(35015, 146, 196, 9, widget);
		setBounds(35016, 363, 196, 10, widget);
		setBounds(35017, 380, 196, 11, widget);
	}
	
	private static void addGEBox(int identity, int slotType) {
		RSInterface component = addInterface(identity);
		component.id = identity;
		component.type = 22;
		component.width = 115;
		component.height = 110;
		component.grandExchangeSlot = slotType;
	}
	
	private static void addBuyAndSellButtons(TextDrawingArea[] tda) {
		RSInterface widget;
		int childId = 0;
		for(int index = 0; index < grandExchangeBuyAndSellBoxIds.length; index++) {
			
			childId = grandExchangeBuyAndSellBoxIds[index];
			widget = addInterface(childId);
			
			setChildren(6, widget);
			
			addSprite(childId + 1, 2, "Interfaces/GE/IMAGE");
			setBounds(childId + 1, 0, 0, 0, widget);
			
			addHoverButton(childId + 2, "Interfaces/GE/IMAGE", 3, 47, 46, "Create buy offer", 0, childId + 3, 1);
			addHoveredButton(childId + 3, "Interfaces/GE/IMAGE", 4, 47, 46, childId + 4);
			
			setBounds(childId + 2, 7, 44, 1, widget);
			setBounds(childId + 3, 7, 44, 2, widget);
			
			addHoverButton(childId + 5, "Interfaces/GE/IMAGE", 5, 47, 46, "Create sell offer", 0, childId + 6, 1);
			addHoveredButton(childId + 6, "Interfaces/GE/IMAGE", 6, 47, 46, childId + 7);
			
			setBounds(childId + 5, 61, 44, 3, widget);
			setBounds(childId + 6, 61, 44, 4, widget);
			
			addText(childId + 8, "Empty", tda, 2, 0xFF981F, true, true);
			setBounds(childId + 8, 58, 6, 5, widget);
		}
	}
	
	private static void addItemBoxes(TextDrawingArea[] tda) {
		RSInterface widget;
		int childId = 0;
		
		for(int index = 0; index < grandExchangeItemBoxIds.length; index++) {
			
			childId = grandExchangeItemBoxIds[index];
			widget = addInterface(childId);
			
			setChildren(8, widget);
			
			addHoverButton(childId + 1, "Interfaces/GE/IMAGE", 2, 115, 110, "View offer", 0, childId + 2, 11);
			addHoveredButton(childId + 2, "Interfaces/GE/IMAGE", 7, 115, 110, childId + 3);
			setBounds(childId + 1, 0, 0, 0, widget);
			setBounds(childId + 2, 0, 0, 1, widget);
			
			addSprite(childId + 3, 8, "Interfaces/GE/IMAGE");
			setBounds(childId + 3, 5, 33, 2, widget);
			
			addToItemGroup(childId + 4, 1, 1, 0, 0, false, "", "", "");
			setBounds(childId + 4, 7, 35, 3, widget);
			
			addProgressBar(childId + 5, 107, 15, new int[] { GREEN_COLOR, RED_COLOR });
			setBounds(childId + 5, 4, 75, 4, widget);
			
			addText(childId + 6, "" + (childId + 6), tda, 2, 0xFF981F, true, true);
			setBounds(childId + 6, 58, 6, 5, widget);
			
			addText(childId + 7, "" + (childId + 7), tda, 0, 0xFF981F, false, true);
			setBounds(childId + 7, 50, 35, 6, widget);
			
			addText(childId + 8, "" + (childId + 8), tda, 0, 0xFF981F, true, true);
			setBounds(childId + 8, 58, 92, 7, widget);
		}
	}

	private static void buyScreen(TextDrawingArea[] tda) {
		RSInterface widget = addInterface(35600);
		final int startX = 20, startY = 13;
		
		setChildren(5, widget);
		
		setBounds(35500, 0, 0, 0, widget);
		
		addSprite(35601, 15, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35601, startX + 152, startY + 48, 1, widget);
		
		addText(35602, "Buy offer", tda, 2, 0xFF981F, true, true);
		setBounds(35602, startX + 95, startY + 47, 2, widget);
		
		addHoverButton(35603, "Interfaces/GE/BuyAndSell/IMAGE", 16, 40, 36, "Search", 0, 35604, 1);
		addHoveredButton(35604, "Interfaces/GE/BuyAndSell/IMAGE", 16, 40, 36, 35605);
		
		setBounds(35603, startX + 76, startY + 71, 3, widget);
		setBounds(35604, startX + 76, startY + 71, 4, widget);
	}
	
	private static void sellScreen(TextDrawingArea[] tda) {
		RSInterface widget = addInterface(35650);
		final int startX = 20, startY = 13;
		
		setChildren(3, widget);
		
		setBounds(35500, 0, 0, 0, widget);
		
		addSprite(35651, 14, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35651, startX + 152, startY + 48, 1, widget);
		
		addText(35652, "Sell offer", tda, 2, 0xFF981F, true, true);
		setBounds(35652, startX + 95, startY + 47, 2, widget);
	}
	
	private static void buyAndSellBackground(TextDrawingArea[] tda) {
		RSInterface widget = addInterface(35500);
		
		final int startX = 20, startY = 13;
		
		setChildren(52, widget);
		
		addSprite(35501, 0, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35501, startX, startY, 0, widget);
		
		addHoverButton(35502, "Interfaces/GE/CLOSE", 1, 21, 21, "Close", -1, 35503, 3);
		addHoveredButton(35503, "Interfaces/GE/CLOSE", 2, 21, 21, 35504);
		setBounds(35502, 476, 20, 1, widget);
		setBounds(35503, 476, 20, 2, widget);
		
		addHoverButton(35505, "Interfaces/GE/BuyAndSell/IMAGE", 5, 13, 13, "Decrease amount", 0, 35506, 1);
		addHoveredButton(35506, "Interfaces/GE/BuyAndSell/IMAGE", 6, 13, 13, 35507);
		setBounds(35505, startX + 30, startY + 162, 3, widget);
		setBounds(35506, startX + 30, startY + 162, 4, widget);
		
		addHoverButton(35508, "Interfaces/GE/BuyAndSell/IMAGE", 3, 13, 13, "Increase amount", 0, 35509, 1);
		addHoveredButton(35509, "Interfaces/GE/BuyAndSell/IMAGE", 4, 13, 13, 35510);
		setBounds(35508, startX + 221, startY + 162, 5, widget);
		setBounds(35509, startX + 221, startY + 162, 6, widget);
		
		addHoverButton(35511, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Increase amount", 0, 35512, 1);
		addHoveredButton(35512, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35513);
		addText(35514, "+1", tda, 0, 0xFF981F, true, true);
		setBounds(35511, startX + 31, startY + 182, 7, widget);
		setBounds(35512, startX + 31, startY + 182, 8, widget);
		setBounds(35514, startX + 31 + 17, startY + 182 + 7, 9, widget);
		
		addHoverButton(35515, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Increase amount", 0, 35516, 1);
		addHoveredButton(35516, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35517);
		addText(35518, "+10", tda, 0, 0xFF981F, true, true);
		setBounds(35515, startX + 73, startY + 182, 10, widget);
		setBounds(35516, startX + 73, startY + 182, 11, widget);
		setBounds(35518, startX + 73 + 17, startY + 182 + 7, 12, widget);
		
		addHoverButton(35519, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Increase amount", 0, 35520, 1);
		addHoveredButton(35520, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35521);
		addText(35522, "+100", tda, 0, 0xFF981F, true, true);
		setBounds(35519, startX + 115, startY + 182, 13, widget);
		setBounds(35520, startX + 115, startY + 182, 14, widget);
		setBounds(35522, startX + 115 + 17, startY + 182 + 7, 15, widget);
		
		addHoverButton(35523, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Increase amount", 0, 35524, 1);
		addHoveredButton(35524, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35535);
		addText(35536, "All", tda, 0, 0xFF981F, true, true);
		setBounds(35523, startX + 157, startY + 182, 16, widget);
		setBounds(35524, startX + 157, startY + 182, 17, widget);
		setBounds(35536, startX + 157 + 17, startY + 182 + 7, 18, widget);
		
		addHoverButton(35527, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Increase amount", 0, 35528, 1);
		addHoveredButton(35528, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35529);
		addText(35530, "...", tda, 0, 0xFF981F, true, true);
		setBounds(35527, startX + 199, startY + 182, 19, widget);
		setBounds(35528, startX + 199, startY + 182, 20, widget);
		setBounds(35530, startX + 199 + 17, startY + 182 + 7, 21, widget);
		
		addHoverButton(35531, "Interfaces/GE/BuyAndSell/IMAGE", 5, 13, 13, "Decrease price", 0, 35532, 1);
		addHoveredButton(35532, "Interfaces/GE/BuyAndSell/IMAGE", 6, 13, 13, 35533);
		setBounds(35531, startX + 30 + 222, startY + 162, 22, widget);
		setBounds(35532, startX + 30 + 222, startY + 162, 23, widget);
		
		addHoverButton(35534, "Interfaces/GE/BuyAndSell/IMAGE", 3, 13, 13, "Increase price", 0, 35535, 1);
		addHoveredButton(35535, "Interfaces/GE/BuyAndSell/IMAGE", 4, 13, 13, 35536);
		setBounds(35534, startX + 221 + 222, startY + 162, 24, widget);
		setBounds(35535, startX + 221 + 222, startY + 162, 35, widget);
		
		addHoverButton(35537, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Decrease amount", 0, 35538, 1);
		addHoveredButton(35538, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35539);
		addSprite(35540, 11, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35537, startX + 30 + 222 + 7, startY + 162 + 21, 36, widget);
		setBounds(35538, startX + 30 + 222 + 7, startY + 162 + 21, 27, widget);
		setBounds(35540, startX + 30 + 222 + 7 + 11, startY + 162 + 21 + 10, 28, widget);
		
		addHoverButton(35541, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Decrease amount", 0, 35542, 1);
		addHoveredButton(35542, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35543);
		addSprite(35544, 13, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35541, startX + 30 + 222 + 7 + 58, startY + 162 + 21, 29, widget);
		setBounds(35542, startX + 30 + 222 + 7 + 58, startY + 162 + 21, 30, widget);
		setBounds(35544, startX + 30 + 222 + 7 + + 58 + 9, startY + 162 + 21 + 4, 31, widget);
		
		addHoverButton(35545, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Decrease amount", 0, 35546, 1);
		addHoveredButton(35546, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35547);
		addText(35548, "...", tda, 0, 0xFF981F, true, true);
		setBounds(35545, startX + 30 + 222 + 7 + 58 + 43, startY + 162 + 21, 32, widget);
		setBounds(35546, startX + 30 + 222 + 7 + 58 + 43, startY + 162 + 21, 33, widget);
		setBounds(35548, startX + 30 + 222 + 7 + 58 + 43 + 17, startY + 162 + 21 + 7, 34, widget);
		
		addHoverButton(35549, "Interfaces/GE/BuyAndSell/IMAGE", 1, 35, 35, "Decrease amount", 0, 35550, 1);
		addHoveredButton(35550, "Interfaces/GE/BuyAndSell/IMAGE", 2, 35, 35, 35551);
		addSprite(35552, 12, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35549, startX + 30 + 222 + 7 + 58 + 43 + 58, startY + 162 + 21, 35, widget);
		setBounds(35550, startX + 30 + 222 + 7 + 58 + 43 + 58, startY + 162 + 21, 36, widget);
		setBounds(35552, startX + 30 + 222 + 7 + 58 + 43 + 58 + 11, startY + 162 + 21 + 10, 37, widget);
		
		addHoverButton(35553, "Interfaces/GE/BuyAndSell/IMAGE", 7, 152, 40, "Confirm", 0, 35554, 1);
		addHoveredButton(35554, "Interfaces/GE/BuyAndSell/IMAGE", 8, 152, 40, 35555);
		addText(35556, "Confirm", tda, 2, 0xFFFFFF, true, true);
		setBounds(35553, startX + 157 + 10, startY + 182 + 69, 38, widget);
		setBounds(35554, startX + 157 + 10, startY + 182 + 69, 39, widget);
		setBounds(35556, startX + 157 + 10 + 76, startY + 182 + 69 + 12, 40, widget);
		
		addHoverButton(35557, "Interfaces/GE/BuyAndSell/IMAGE", 9, 30, 23, "Back", 0, 35558, 11);
		addHoveredButton(35558, "Interfaces/GE/BuyAndSell/IMAGE", 10, 30, 23, 35559);
		setBounds(35557, startX + 157 + 10 - 150, startY + 182 + 69 + 9, 41, widget);
		setBounds(35558, startX + 157 + 10 - 150, startY + 182 + 69 + 9, 42, widget);
		
		addText(35560, "Grand Exchange: Set up offer", tda, 2, 0xFF981F, true, true);
		setBounds(35560, startX + 245, startY + 10, 43, widget);
		
		addText(35561, "Choose an item...", tda, 2, 0xFF981F, false, true);
		setBounds(35561, startX + 180, startY + 50, 44, widget);
		
		addText(35562, Integer.toString(35562), tda, 0, 0xFFB83F, false, true);
		setBounds(35562, startX + 180, startY + 70, 45, widget);
		
		addText(35563, "Quantity:", tda, 2, 0xFF981F, true, true);
		setBounds(35563, startX + 130, startY + 140, 46, widget);
		
		addText(35564, "Price per item:", tda, 2, 0xFF981F, true, true);
		setBounds(35564, startX + 355, startY + 140, 47, widget);
		
		addText(35565, Integer.toString(35565), tda, 0, 0xFFB83F, true, true);
		setBounds(35565, startX + 52 + 81, startY + 159 + 4, 48, widget);
		
		addText(35566, Integer.toString(35566), tda, 0, 0xFFB83F, true, true);
		setBounds(35566, startX + 52 + 222 + 81, startY + 159 + 4, 49, widget);
		
		addText(35567, Integer.toString(35567), tda, 0, 0xFFFFFF, true, true);
		setBounds(35567, startX + 52 + 86 + 106, startY + 159 + 55 + 4, 50, widget);
		
		addToItemGroup(35568, 1, 1, 0, 0, false, "", "", "");
		setBounds(35568, startX + 79, startY + 74, 51, widget);
	}
	
	private static void offerStatusBackground(TextDrawingArea[] tda) {
		RSInterface widget = addInterface(35700);
		
		final int startX = 20, startY = 13;
		
		setChildren(15, widget);
		
		addSprite(35701, 17, "Interfaces/GE/BuyAndSell/IMAGE");
		setBounds(35701, startX, startY, 0, widget);
		
		addHoverButton(35702, "Interfaces/GE/CLOSE", 1, 21, 21, "Close", -1, 35703, 3);
		addHoveredButton(35703, "Interfaces/GE/CLOSE", 2, 21, 21, 35704);
		setBounds(35702, 476, 20, 1, widget);
		setBounds(35703, 476, 20, 2, widget);
		
		addHoverButton(35705, "Interfaces/GE/BuyAndSell/IMAGE", 9, 30, 23, "Back", 0, 35706, 11);
		addHoveredButton(35706, "Interfaces/GE/BuyAndSell/IMAGE", 10, 30, 23, 35707);
		setBounds(35705, startX + 157 + 10 - 150, startY + 182 + 69 + 9, 3, widget);
		setBounds(35706, startX + 157 + 10 - 150, startY + 182 + 69 + 9, 4, widget);
		
		addText(35708, "Grand Exchange: Offer status", tda, 2, 0xFF981F, true, true);
		setBounds(35708, startX + 245, startY + 10, 5, widget);
		
		addText(35709, "Choose an item...", tda, 2, 0xFF981F, false, true);
		setBounds(35709, startX + 180, startY + 50, 6, widget);
		
		addText(35710, Integer.toString(35562), tda, 0, 0xFFB83F, false, true);
		setBounds(35710, startX + 180, startY + 70, 7, widget);
		
		addText(35711, "Quantity:", tda, 2, 0xFF981F, true, true);
		setBounds(35711, startX + 130, startY + 140, 8, widget);
		
		addText(35712, "Price per item:", tda, 2, 0xFF981F, true, true);
		setBounds(35712, startX + 355, startY + 140, 9, widget);
		
		addText(35713, Integer.toString(35713), tda, 2, 0xFFB83F, true, true);
		setBounds(35713, startX + 52 + 81, startY + 159 + 1, 10, widget);
		
		addText(35714, Integer.toString(35714), tda, 2, 0xFFB83F, true, true);
		setBounds(35714, startX + 52 + 222 + 81, startY + 159 + 1, 11, widget);
		
		addText(35715, Integer.toString(35715), tda, 2, 0xFFFFFF, true, true);
		setBounds(35715, startX + 52 + 86 + 106, startY + 159 + 55 + 1, 12, widget);
		
		addText(35716, Integer.toString(35716), tda, 0, 0xFFB83F, true, true);
		setBounds(35716, startX + 210, startY + 350, 13, widget);
		
		addText(35717, Integer.toString(35717), tda, 0, 0xFFB83F, true, true);
		setBounds(35717, startX + 210, startY + 365, 14, widget);
		
	}
	
	private static void addOfferStatusInterfaces(TextDrawingArea[] tda) {
		RSInterface widget;
		int childId = 0;
		int itemBoxChildId = 0;
		final int startX = 20, startY = 13;
		
		for(int index = 0; index < grandExchangeOfferStatusInterfaceIds.length; index++) {
			
			childId = grandExchangeOfferStatusInterfaceIds[index];
			itemBoxChildId = grandExchangeItemBoxIds[index];
			widget = addInterface(childId);
			
			setChildren(6, widget);
			
			//Adds the main offer status interface
			setBounds(35700, 0, 0, 0, widget);
			
			//Adds the current GE item 
			setBounds(itemBoxChildId + 4, startX + 79, startY + 74, 1, widget);
			
			addProgressBar(childId + 1, 291, 15, new int[] { GREEN_COLOR, RED_COLOR });
			setBounds(childId + 1, startX + 60, startY + 280, 2, widget);
			
			addText(childId + 2, Integer.toString(childId + 2), tda, 2, 0xFF981F, true, true);
			setBounds(childId + 2, startX + 95, startY + 47, 3, widget);
			
			addToItemGroup(childId + 3, 1, 1, 0, 0, true, "Collect to inventory", "Collect to bank", "");
			setBounds(childId + 3, startX + 380, startY + 354, 4, widget);
			
			addToItemGroup(childId + 4, 1, 1, 0, 0, true, "Collect to inventory", "Collect to bank", "");
			setBounds(childId + 4, startX + 380 + 52, startY + 354, 5, widget);
		}
	}
	
	private static void collectInterface(TextDrawingArea[] tda) {
		RSInterface widget = addInterface(36000);
		
		final int startX = 30, startY = 45;
		
		setChildren(42, widget);
		
		addSprite(36001, 0, "Interfaces/GE/Collect/IMAGE");
		setBounds(36001, startX, startY, 0, widget);
		
		addHoverButton(36002, "Interfaces/GE/CLOSE", 1, 21, 21, "Close", -1, 36003, 3);
		addHoveredButton(36003, "Interfaces/GE/CLOSE", 2, 21, 21, 36004);
		setBounds(36002, startX + 432, startY + 7, 1, widget);
		setBounds(36003, startX + 432, startY + 7, 2, widget);

		int grandExchangeBox = 0;
		addProgressBar(36005, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36005, startX + 23, startY + 52, 3, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 88, startY + 53, 4, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 27, startY + 78, 5, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 72, startY + 78, 6, widget);
		grandExchangeBox++;
		
		addProgressBar(36006, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36006, startX + 133, startY + 52, 7, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 198, startY + 53, 8, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 137, startY + 78, 9, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 182, startY + 78, 10, widget);
		grandExchangeBox++;
		
		addProgressBar(36007, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36007, startX + 243, startY + 52, 11, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 308, startY + 53, 12, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 248, startY + 78, 13, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 304, startY + 78, 14, widget);
		grandExchangeBox++;
		
		addProgressBar(36008, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36008, startX + 353, startY + 52, 15, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 418, startY + 53, 16, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 359, startY + 78, 17, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 415, startY + 78, 18, widget);
		grandExchangeBox++;
		
		
		addProgressBar(36009, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36009, startX + 23, startY + 136, 19, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 88, startY + 137, 20, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 27, startY + 162, 21, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 72, startY + 162, 22, widget);
		grandExchangeBox++;
		
		addProgressBar(36010, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36010, startX + 133, startY + 136, 23, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 198, startY + 137, 24, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 137, startY + 162, 35, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 182, startY + 162, 36, widget);
		grandExchangeBox++;
		
		addProgressBar(36011, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36011, startX + 243, startY + 136, 27, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 308, startY + 137, 28, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 248, startY + 162, 29, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 304, startY + 162, 30, widget);
		grandExchangeBox++;
		
		addProgressBar(36012, 40, 18, new int[] { GREEN_COLOR, RED_COLOR });
		setBounds(36012, startX + 353, startY + 136, 31, widget);
		setBounds(grandExchangeItemBoxIds[grandExchangeBox] + 4, startX + 418, startY + 137, 32, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 3, startX + 359, startY + 162, 33, widget);
		setBounds(grandExchangeOfferStatusInterfaceIds[grandExchangeBox] + 4, startX + 415, startY + 162, 34, widget);
		
		
		
		addHoverButton(36013, "Interfaces/GE/Collect/IMAGE", 1, 83, 19, "Collect to inventory", 0, 36014, 1);
		addHoveredButton(36014, "Interfaces/GE/Collect/IMAGE", 2, 83, 19, 36015);
		addText(36016, "Inventory", tda, 0, 0xFFB83F, true, true);
		setBounds(36013, startX + 142, startY + 214, 35, widget);
		setBounds(36014, startX + 142, startY + 214, 36, widget);
		setBounds(36016, startX + 142 + 41, startY + 214 + 4, 37, widget);
		
		addHoverButton(36017, "Interfaces/GE/Collect/IMAGE", 1, 83, 19, "Collect to bank", 0, 36018, 1);
		addHoveredButton(36018, "Interfaces/GE/Collect/IMAGE", 2, 83, 19, 36019);
		addText(36020, "Bank", tda, 0, 0xFFB83F, true, true);
		setBounds(36017, startX + 142 + 96, startY + 214, 38, widget);
		setBounds(36018, startX + 142 + 96, startY + 214, 39, widget);
		setBounds(36020, startX + 142 + 96 + 41, startY + 214 + 4, 40, widget);
		
		addText(36021, "Collection Box", tda, 2, 0xFF981F, true, true);
		setBounds(36021, startX + 235, startY + 11, 41, widget);
	}
	
	public static boolean isSmallItemSprite(int childId) {
		for(int index = 0; index < grandExchangeItemBoxIds.length; index++)
			if(childId == grandExchangeItemBoxIds[index] + 4)
				return true;
		return false;
	}
	
}