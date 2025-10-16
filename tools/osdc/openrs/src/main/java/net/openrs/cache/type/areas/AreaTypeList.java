package net.openrs.cache.type.areas;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.openrs.cache.Archive;
import net.openrs.cache.Cache;
import net.openrs.cache.Constants;
import net.openrs.cache.Container;
import net.openrs.cache.ReferenceTable;
import net.openrs.cache.ReferenceTable.ChildEntry;
import net.openrs.cache.ReferenceTable.Entry;
import net.openrs.cache.type.CacheIndex;
import net.openrs.cache.type.ConfigArchive;
import net.openrs.cache.type.TypeList;
import net.openrs.cache.type.TypePrinter;
import net.openrs.util.Preconditions;
import org.jetbrains.annotations.NotNull;

/**
 * @author Kyle Friz
 * 
 * @since May 26, 2015
 */
public class AreaTypeList implements TypeList<AreaType> {

	private Logger logger = Logger.getLogger(AreaTypeList.class.getName());

	private AreaType[] areas;

	@Override
	public void initialize(Cache cache) {
		int count = 0;
		try {
			ReferenceTable table = cache.getReferenceTable(CacheIndex.CONFIGS);
			Entry entry = table.getEntry(ConfigArchive.AREA);
			Archive archive = Archive.decode(cache.read(CacheIndex.CONFIGS, ConfigArchive.AREA).getData(),
					entry.size());

			areas = new AreaType[entry.capacity()];
			for (int id = 0; id < entry.capacity(); id++) {
				ChildEntry child = entry.getEntry(id);
				if (child == null)
					continue;

				ByteBuffer buffer = archive.getEntry(child.index());
				AreaType type = new AreaType(id);
				type.decode(buffer);
				areas[id] = type;
				count++;
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error Loading AreaType(s)!", e);
		}
		logger.info("Loaded " + count + " AreaType(s)!");
	}

	@Override
	public AreaType list(int id) {
		Preconditions.checkArgument(id >= 0, "ID can't be negative!");
		Preconditions.checkArgument(id < areas.length, "ID can't be greater than the max area id!");
		return areas[id];
	}

	@Override
	public void print() {
	      
	  File dir = new File(Constants.TYPE_PATH);

	  if (!dir.exists()) {
	        dir.mkdir();
	  }
	      
		File file = new File(Constants.TYPE_PATH, "areas.txt");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			Arrays.stream(areas).filter(Objects::nonNull).forEach((AreaType t) -> {
				TypePrinter.print(t, writer);
			});
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public int size() {
		return areas.length;
	}

	@NotNull
	public List<Integer> getSpriteIds() {
		return Stream.of(areas).map(AreaType::getSpriteId).filter(id -> id >= 0).distinct().collect(Collectors.toList());
	}
}
