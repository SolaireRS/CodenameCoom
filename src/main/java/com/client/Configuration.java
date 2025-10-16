package com.client;

import java.time.LocalDateTime;

public class Configuration {

	public static final int CLIENT_VERSION = 233;

	public static final int CACHE_VERSION = 4; 

	// US Client Download
	public static final String CACHE_LINK = "https://us.files-exilius.com/cache2/v0.049.zip";


	public static final int SERVER_VERSION = 3;
	public static final String CACHE_NAME = "SolaireOSRS";

	public static final String CLIENT_TITLE = "the Phoenix Client";
	public static final String WEBSITE = "exilius-osrs.com";
	public static final String DEDICATED_SERVER_ADDRESS = "127.0.0.1";//51.222.245.179
	public static final String TEST_SERVER_ADDRESS = "127.0.0.1";//51.79.50.152
	public static final int PORT = 43594;
	public static final int TEST_PORT = 43595;
	public static final int CACHE_FOLDER_VERSION = 0;
	public static final String DEV_CACHE_NAME = "local_cache";
	public static final String CACHE_NAME_DEV = CACHE_NAME + "_dev";

	public static final String CUSTOM_ITEM_SPRITES_DIRECTORY = "item_sprites/";
	public static String CUSTOM_MAP_DIRECTORY = "./data/custom_maps/";
	public static String CUSTOM_MODEL_DIRECTORY = "./data/custom_models/";
	public static String CUSTOM_ANIMATION_DIRECTORY = "./data/custom_animations/";
	public static String EXTERNAL_CACHE_ARCHIVE = "/archive_data/";
	public static String INDEX_DATA_DIRECTORY = "/index_data/";

	public static boolean developerMode;
	public static boolean loadExternalCacheArchives = false; // Always true because I can't seem to pack them correctly
	public static boolean packIndexData = false;
	public static boolean dumpMaps;
	public static boolean dumpAnimationData = false;
	public static boolean dumpDataLists = false;
	public static boolean newFonts; // TODO text offsets (i.e. spacing between characters) are incorrect, needs automatic fix from kourend
	public static String cacheName = CACHE_NAME;
	public static String clientTitle = "";

	public static final LocalDateTime LAUNCH_TIME = LocalDateTime.now();
	public static final String ERROR_LOG_DIRECTORY = "error_logs/";
	public static String ERROR_LOG_FILE = ("error_log_"
			+ LAUNCH_TIME.getYear() + "_"
			+ LAUNCH_TIME.getMonth() + "_"
			+ LAUNCH_TIME.getDayOfMonth()
			+ ".txt").toLowerCase();

	public static int playerAttackOptionPriority;
	public static int npcAttackOptionPriority = 2;

	public static final boolean DUMP_SPRITES = false;
	public static final boolean PRINT_EMPTY_INTERFACE_SECTIONS = false;

	public static boolean playerNames;

	public static boolean HALLOWEEN;
	public static boolean CHRISTMAS;
	public static boolean CHRISTMAS_EVENT;
	public static boolean EASTER;

	public static boolean osbuddyGameframe;

	public static int xpPosition;
	public static boolean escapeCloseInterface;
	public static boolean alwaysLeftClickAttack;
	public static boolean hideCombatOverlay;

	public static boolean enableRainbowFog;

    public static int fogColor = 0xDCDBDF;
	public static long fogDelay = 500;
	public static boolean enableFogRendering = false;
}
