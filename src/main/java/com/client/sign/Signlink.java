package com.client.sign;

import java.applet.Applet;
import java.io.*;
import java.net.*;

import com.client.Configuration;
import com.google.common.base.Preconditions;
import javax.sound.midi.*;
import javax.sound.sampled.*;

public final class Signlink implements Runnable {

	public static void startpriv(InetAddress inetaddress) {
		threadliveid = (int) (Math.random() * 99999999D);
		if (active) {
			try {
				Thread.sleep(500L);
			} catch (Exception _ex) {
			}
			active = false;
		}
		socketreq = 0;
		threadreq = null;
		dnsreq = null;
		savereq = null;
		urlreq = null;
		socketip = inetaddress;
		Thread thread = new Thread(new Signlink());
		thread.setDaemon(true);
		thread.start();
		while (!active)
			try {
				Thread.sleep(50L);
			} catch (Exception _ex) {
			}
	}
	enum Position {
		LEFT, RIGHT, NORMAL
	};

	private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb
	private Position curPosition;

	public static void createCacheDirectory() {
		File file = new File(getCacheDirectory());
		if (!file.exists()) {
			Preconditions.checkState(file.mkdir());
		}
	}

	public void run() {
		active = true;
		String s = getCacheDirectory();
		uid = getuid(s);
		try {
			cache_dat = new RandomAccessFile(s + "main_file_cache.dat", "rw");
			for (int j = 0; j < 8; j++)
				cache_idx[j] = new RandomAccessFile(s + "main_file_cache.idx" + j, "rw");

		} catch (Exception exception) {
			exception.printStackTrace();
		}
		for (int i = threadliveid; threadliveid == i;) {
			if (socketreq != 0) {
				try {
					socket = new Socket(socketip, socketreq);
				} catch (Exception _ex) {
					socket = null;
				}
				socketreq = 0;
			} else if (threadreq != null) {
				Thread thread = new Thread(threadreq);
				thread.setDaemon(true);
				thread.start();
				thread.setPriority(threadreqpri);
				threadreq = null;
			} else if (dnsreq != null) {
				try {
					dns = InetAddress.getByName(dnsreq).getHostName();
				} catch (Exception _ex) {
					dns = "unknown";
				}
				dnsreq = null;
			} else if (savereq != null) {
				if (savebuf != null)
					try (FileOutputStream fileoutputstream = new FileOutputStream(s + savereq)) {
						fileoutputstream.write(savebuf, 0, savelen);
					} catch (IOException e) {
						e.printStackTrace();
					}
				if (waveplay) {
					String wave = s + savereq;
					waveplay = false;
					AudioInputStream audioInputStream = null;
					try {
						audioInputStream = AudioSystem.getAudioInputStream(new File(wave));
					} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
						return;
					} catch (IOException e1) {
						e1.printStackTrace();
						return;
					}
					AudioFormat format = audioInputStream.getFormat();
					SourceDataLine auline = null;
					DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
					try {
						auline = (SourceDataLine) AudioSystem.getLine(info);
						auline.open(format);
					} catch (LineUnavailableException e) {
						e.printStackTrace();
						return;
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					if (auline.isControlSupported(FloatControl.Type.PAN)) {
						FloatControl pan = (FloatControl) auline.getControl(FloatControl.Type.PAN);
						if (curPosition == Position.RIGHT)
							pan.setValue(1.0f);
						else if (curPosition == Position.LEFT)
							pan.setValue(-1.0f);
					}
					auline.start();
					int nBytesRead = 0;
					byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
					try {
						while (nBytesRead != -1) {
							nBytesRead = audioInputStream.read(abData, 0,
									abData.length);
							if (nBytesRead >= 0)
								auline.write(abData, 0, nBytesRead);
						}
					} catch (IOException e) {
						e.printStackTrace();
						return;
					} finally {
						auline.drain();
						auline.close();
					}
				}
				if (midiplay) {
					midi = s + savereq;
					try {
						if (music != null) {
							music.stop();
							music.close();
						}
						playMidi(midi);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					midiplay = false;
				}
				savereq = null;
			} else
			if(urlreq != null)
			{
				try
				{
					urlstream = new DataInputStream((new URL(mainapp.getCodeBase(), urlreq)).openStream());
				}
				catch(Exception _ex)
				{
					urlstream = null;
				}
				urlreq = null;
			}
			try
			{
				Thread.sleep(50L);
			}
			catch(Exception _ex) { }
		}

	}
	private void playMidi(String location) {
	    music = null;
	    synthesizer = null;
	    sequence = null;
	    File midiFile = new File(location);
	    try {
	        sequence = MidiSystem.getSequence(midiFile);
	        music = MidiSystem.getSequencer();
	        music.open();
	        music.setSequence(sequence);
	    } catch (Exception e) {
	        System.err.println("Problem loading MIDI file.");
	        e.printStackTrace();
	        return;
	    }
	    if (music instanceof Synthesizer) {
	        synthesizer = (Synthesizer) music;
	    } else {
	        try {
	            synthesizer = MidiSystem.getSynthesizer();
	            synthesizer.open();
	            if (synthesizer.getDefaultSoundbank() == null) {
	                music.getTransmitter().setReceiver(MidiSystem.getReceiver());
	            } else {
	                music.getTransmitter().setReceiver(synthesizer.getReceiver());
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            return;
	        }
	    }
	    music.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
	    music.start();
	    
	    // CRITICAL: Apply saved volume after music starts
	    setVolume(midivol);
	}

	public static void setVolume(int value) {
	    midivol = value;
	    //System.out.println("=== Signlink.setVolume called with: " + value + " ===");
	    
	    //System.out.println("setVolume called with: " + value);
	    
	    if (synthesizer == null || !synthesizer.isOpen()) {
	        //System.out.println("ERROR: Synthesizer is null or not open!");
	        return;
	    }
	    
	    if (music == null || !music.isOpen()) {
	        //System.out.println("ERROR: Sequencer is null or not open!");
	        return;
	    }
	    
	    try {
	        // Method 1: Set volume on all MIDI channels via the synthesizer's channels directly
	        MidiChannel[] channels = synthesizer.getChannels();
	        if (channels != null) {
	            for (int c = 0; c < channels.length; c++) {
	                if (channels[c] != null) {
	                    channels[c].controlChange(7, value);  // Main volume
	                    channels[c].controlChange(11, value); // Expression
	                }
	            }
	            //System.out.println("Set volume on synthesizer channels");
	        }
	        
	        // Method 2: Insert volume changes into the sequence itself
	        if (sequence != null) {
	            Track[] tracks = sequence.getTracks();
	            for (Track track : tracks) {
	                // Add volume control messages at the current position
	                for (int channel = 0; channel < 16; channel++) {
	                    ShortMessage volumeMsg = new ShortMessage();
	                    volumeMsg.setMessage(ShortMessage.CONTROL_CHANGE, channel, 7, value);
	                    MidiEvent event = new MidiEvent(volumeMsg, music.getTickPosition());
	                    track.add(event);
	                    
	                    ShortMessage expressionMsg = new ShortMessage();
	                    expressionMsg.setMessage(ShortMessage.CONTROL_CHANGE, channel, 11, value);
	                    MidiEvent expEvent = new MidiEvent(expressionMsg, music.getTickPosition());
	                    track.add(expEvent);
	                }
	            }
	            //System.out.println("Added volume events to sequence");
	        }
	        
	        // Method 3: Send messages through the receiver
	        Receiver receiver = synthesizer.getReceiver();
	        ShortMessage volumeMessage = new ShortMessage();
	        for (int channel = 0; channel < 16; channel++) {
	            volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, channel, 7, value);
	            receiver.send(volumeMessage, -1);
	            volumeMessage.setMessage(ShortMessage.CONTROL_CHANGE, channel, 11, value);
	            receiver.send(volumeMessage, -1);
	        }
	        //System.out.println("Sent volume messages via receiver");
	        
	    } catch (Exception e) {
	        //System.out.println("Error setting volume:");
	        e.printStackTrace();
	    }
	}

	public static Sequencer music = null;
	public static Sequence sequence = null;
	public static Synthesizer synthesizer = null;
	public static final String separator = System.getProperty("file.separator");
	private static final String devCache = "." + separator + Configuration.DEV_CACHE_NAME + separator;
	private static String cacheDir = null;

	private static String setCacheDir(String cacheDir) {
		Signlink.cacheDir = cacheDir;
		System.out.println("Using cache directory: " + cacheDir);
		return cacheDir;
	}

	public static boolean usingDevCache() {
		return cacheDir.equals(devCache);
	}

	public static final String getCacheDirectory() {
		if (cacheDir != null) {
			return cacheDir;
		}

		// Dev cache only loads in dev mode to allow for easy switching.
		if (new File(devCache).exists()) {
			if (Configuration.developerMode) {
				return setCacheDir(devCache);
			}

			System.out.println("Development cache detected but client was not launched in developer mode (-d run argument).");
		}

		// Home directory cache
		String home = System.getProperty("user.home");
		String cacheName = Configuration.cacheName;
		String cacheDir = home + separator + cacheName + separator+"cache"+ separator;
		File file = new File(cacheDir);
		if (file.exists() || file.mkdir()) {
			return setCacheDir(cacheDir);
		}
		return null;
	}

	public static String findcachedirORIG() {
		String as[] = { "c:/windows/", "c:/winnt/", "d:/windows/", "d:/winnt/",
				"e:/windows/", "e:/winnt/", "f:/windows/", "f:/winnt/", "c:/",
				"~/", "/tmp/", "", "c:/rscache", "/rscache" };
		if (storeid < 32 || storeid > 34)
			storeid = 32;
		String s = ".file_store_" + storeid;
		for (int i = 0; i < as.length; i++)
			try {
				String s1 = as[i];
				if (s1.length() > 0) {
					File file = new File(s1);
					if (!file.exists())
						continue;
				}
				File file1 = new File(s1 + s);
				if (file1.exists() || file1.mkdir())
					return s1 + s + "/";
			} catch (Exception _ex) {
			}

		return null;

	}

	public static int getuid(String s) {
		try {
			File file = new File(s + "uid.dat");
			if (!file.exists() || file.length() < 4L) {
				try(DataOutputStream dataoutputstream = new DataOutputStream(new FileOutputStream(s + "uid.dat"))) {
					dataoutputstream.writeInt((int) (Math.random() * 99999999D));
				}
			}
		} catch (Exception _ex) {
			_ex.printStackTrace();
		}
		try (DataInputStream datainputstream = new DataInputStream(new FileInputStream(s + "uid.dat"))) {
			int i = datainputstream.readInt();
			return i + 1;
		} catch (Exception _ex) {
			return 0;
		}
	}

	public static synchronized Socket opensocket(int i) throws IOException {
		for (socketreq = i; socketreq != 0;)
			try {
				Thread.sleep(50L);
			} catch (Exception _ex) {
			}

		if (socket == null)
			throw new IOException("could not open socket");
		else
			return socket;
	}

	public static synchronized DataInputStream openurl(String s)
			throws IOException {
		for (urlreq = s; urlreq != null;)
			try {
				Thread.sleep(50L);
			} catch (Exception _ex) {
			}

		if (urlstream == null)
			throw new IOException("could not open: " + s);
		else
			return urlstream;
	}

	public static synchronized void dnslookup(String s) {
		dns = s;
		dnsreq = s;
	}

	public static synchronized void startthread(Runnable runnable, int i) {
		threadreqpri = i;
		threadreq = runnable;
	}

	public static synchronized boolean wavesave(byte abyte0[], int i) {
		if (i > 0x1e8480)
			return false;
		if (savereq != null) {
			return false;
		} else {
			wavepos = (wavepos + 1) % 5;
			savelen = i;
			savebuf = abyte0;
			waveplay = true;
			savereq = "sound" + wavepos + ".wav";
			return true;
		}
	}

	public static synchronized boolean wavereplay() {
		if (savereq != null) {
			return false;
		} else {
			savebuf = null;
			waveplay = true;
			savereq = "sound" + wavepos + ".wav";
			return true;
		}
	}

	public static synchronized void midisave(byte abyte0[], int i) {
		if (i > 0x1e8480)
			return;
		if (savereq != null) {
		} else {
			midipos = (midipos + 1) % 5;
			savelen = i;
			savebuf = abyte0;
			midiplay = true;
			savereq = "jingle" + midipos + ".mid";
		}
	}

	public static void reporterror(String s) {
		System.err.println("Error: " + s);
	}

	public Signlink() {
	}

	public static final int clientversion = 317;
	public static int uid;
	public static int storeid = 32;
	public static RandomAccessFile cache_dat = null;
	public static final RandomAccessFile[] cache_idx = new RandomAccessFile[8];
	public static boolean sunjava;
	public static Applet mainapp = null;
	public static boolean active;
	public static int threadliveid;
	public static InetAddress socketip;
	public static int socketreq;
	public static Socket socket = null;
	public static int threadreqpri = 1;
	public static Runnable threadreq = null;
	public static String dnsreq = null;
	public static String dns = null;
	public static String urlreq = null;
	public static DataInputStream urlstream = null;
	public static int savelen;
	public static String savereq = null;
	public static byte[] savebuf = null;
	public static boolean midiplay;
	public static int midipos;
	public static String midi = null;
	public static int midivol;
	public static int midifade;
	public static boolean waveplay;
	public static int wavepos;
	public static int wavevol;
	public static boolean reporterror = true;
	public static String errorname = "";

}
