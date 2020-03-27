package main;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.SwingUtilities;

import amidst.logging.AmidstLogger;
import amidst.mojangapi.file.MinecraftInstallation;
import amidst.mojangapi.minecraftinterface.MinecraftInterface;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceCreationException;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaces;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.World;
import amidst.mojangapi.world.WorldBuilder;
import amidst.mojangapi.world.WorldOptions;
import amidst.mojangapi.world.WorldSeed;
import amidst.mojangapi.world.WorldType;
import amidst.mojangapi.world.coordinates.CoordinatesInWorld;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.mojangapi.world.icon.locationchecker.LocationChecker;
import amidst.mojangapi.world.icon.producer.CachedWorldIconProducer;
import amidst.mojangapi.world.versionfeatures.DefaultVersionFeatures;
import amidst.mojangapi.world.versionfeatures.VersionFeatures;
import amidst.parsing.FormatException;

import static amidst.mojangapi.world.versionfeatures.FeatureKey.*;

public class StructureFinder {
	private static final WorldBuilder worldBuilder = WorldBuilder.createSilentPlayerless();
	
	private final StructureWorker structureWorker;
	private final MinecraftInterface mcInterface;
	
	public StructureFinder(RecognisedVersion ver, MinecraftInstallation mi)
			throws FormatException, IOException, MinecraftInterfaceCreationException {
		this.structureWorker = new StructureWorker();
		this.mcInterface = MinecraftInterfaces.fromLocalProfile(mi.newLauncherProfile(ver.getName()));
	}
	
	public void submit(String seed, String worldType, String structureType, int radius,
			CoordinatesInWorld startPos, Resolution resolution, boolean unlikelyEndCities) {
		structureWorker.submit(() -> {
			Main.setChangeVersions(false);
			
			WorldOptions worldOptions = new WorldOptions(WorldSeed.fromUserInput(seed), parseWorldType(worldType));
			World world = createWorld(worldOptions);
			VersionFeatures versionFeatures = DefaultVersionFeatures.builder(worldOptions, world.getBiomeDataOracle()).create(mcInterface.getRecognisedVersion());
			
			Main.setIntermediate(false);
			
			try {
				int structureOffset = parseStructureOffset(structureType);
				if (structureType.equals("Stronghold")) {
					strongholdSearch(worldOptions.getWorldSeed().getLong(), radius, startPos, versionFeatures.get(STRONGHOLD_PRODUCER), structureOffset);
				} else {
					scanForStructure(parseLocationChecker(structureType, versionFeatures, unlikelyEndCities), startPos, resolution, radius, structureOffset);
				}
			} catch (InterruptedException | InvocationTargetException e) {
				AmidstLogger.info("Stopping search...");
				SwingUtilities.invokeLater(() -> Main.getProgressBar().setValue(Main.getProgressBar().getMinimum()));
			}
			
			world.dispose();
			Main.setChangeVersions(true);
			Main.setButtonAction(true);
			Main.setButtonEnabled(true);
		});
	}
	
	public void cancel() {
		structureWorker.cancel();
	}
	
	public boolean isRunning() {
		return structureWorker.isRunning();
	}
	
	public World createWorld(WorldOptions worldOptions) {
		Consumer<World> onDispose = world -> {
			world = null;
		};
		try {
			return worldBuilder.from(mcInterface, onDispose, worldOptions);
		} catch (MinecraftInterfaceException | NullPointerException e) {
			Main.errorProcedure(e, true);
		}
		
		return null;
	}
	
	public int parseStructureOffset(String structtype) {
		switch (structtype) {
		case "Village":
			return 4;
		case "Mineshaft":
			return 8;
		case "Mansion":
			return 8;
		case "Jungle Temple":
			return 8;
		case "Desert Temple":
			return 8;
		case "Igloo":
			return 8;
		case "Shipwreck":
			return 8;
		case "Witch Hut":
			return 8;
		case "Stronghold":
			return 4;
		case "Ocean Monument":
			return 8;
		case "Ocean Ruin":
			return 8;
		case "Nether Fortress":
			return 88;
		case "End City":
			return 8;
		case "Buried Treasure":
			return 9;
		case "Pillager Outpost":
			return 4;
		default:
			Main.errorProcedure(
					"parseStructureOffset error: Input did not match any structure type, instead got " + structtype,
					false);
			break;
		}
		return 0;
	}
	
	public LocationChecker parseLocationChecker(String structtype, VersionFeatures versionFeatures, boolean unlikelyEndCities) {
		switch (structtype) {
		case "Village":
			return versionFeatures.get(VILLAGE_LOCATION_CHECKER);
		case "Mineshaft":
			return versionFeatures.get(MINESHAFT_LOCATION_CHECKER);
		case "Mansion":
			return versionFeatures.get(WOODLAND_MANSION_LOCATION_CHECKER);
		case "Jungle Temple":
			return versionFeatures.get(JUNGLE_TEMPLE_LOCATION_CHECKER);
		case "Desert Temple":
			return versionFeatures.get(DESERT_TEMPLE_LOCATION_CHECKER);
		case "Igloo":
			return versionFeatures.get(IGLOO_LOCATION_CHECKER);
		case "Shipwreck":
			return versionFeatures.get(SHIPWRECK_LOCATION_CHECKER);
		case "Witch Hut":
			return versionFeatures.get(WITCH_HUT_LOCATION_CHECKER);
		case "Ocean Monument":
			return versionFeatures.get(OCEAN_MONUMENT_LOCATION_CHECKER);
		case "Ocean Ruin":
			return versionFeatures.get(OCEAN_RUINS_LOCATION_CHECKER);
		case "Nether Fortress":
			return versionFeatures.get(NETHER_FORTRESS_LOCATION_CHECKER);
		case "End City":
			return new RefinedEndCityLocationChecker(
						   versionFeatures.get(WORLD_OPTIONS).getWorldSeed().getLong(),
						   versionFeatures.get(END_ISLAND_ORACLE),
						   unlikelyEndCities
					   );
		case "Buried Treasure":
			return versionFeatures.get(BURIED_TREASURE_LOCATION_CHECKER);
		case "Pillager Outpost":
			return versionFeatures.get(PILLAGER_OUTPOST_LOCATION_CHECKER);
		default:
			Main.errorProcedure(
					"parseLocationChecker error: Input did not match any structure type, instead got " + structtype,
					false);
			break;
		}
		return null;
	}
	
	public WorldType parseWorldType(String worldtype) {
		switch (worldtype) {
		case "Default":
			return WorldType.DEFAULT;
		case "Flat":
			return WorldType.FLAT;
		case "Large Biomes":
			return WorldType.LARGE_BIOMES;
		case "Amplified":
			return WorldType.AMPLIFIED;
		default:
			Main.errorProcedure("parseWorldType error: Input did not match any world type, instead got " + worldtype,
					false);
			break;
		}
		return null;
	}
	
	private void strongholdSearch(long seed, int radius, CoordinatesInWorld start, CachedWorldIconProducer strongholdProducer, int structureOffset)
			throws InvocationTargetException, InterruptedException {
		List<CoordinatesInWorld> coords = strongholdProducer.getWorldIcons().stream().map(icon -> icon.getCoordinates())
				.collect(Collectors.toList());
		int cradius = radius << 4;
		int i = 0;
		for (CoordinatesInWorld coord : coords) {
			setProgress(i);
			if (coord.getX() > start.getX() - cradius && coord.getX() < start.getX() + cradius
					&& coord.getY() > start.getY() - cradius && coord.getY() < start.getY() + cradius) {
				CoordinatesInWorld newCoord = CoordinatesInWorld.from(coord.getX() + structureOffset,
						coord.getY() + structureOffset);
				Main.addRow(new CoordData(start, newCoord));
			}
			i++;
		}
		setProgress(Main.getProgressBar().getMinimum());
	}
	
	public void scanForStructure(LocationChecker checker, CoordinatesInWorld start, Resolution res, int r, int structureOffset) throws InvocationTargetException, InterruptedException {
		boolean flag1 = Main.isStructTypeNetherFortress();
		boolean flag2 = Main.isCoordTypeNether();
		CoordinatesInWorld newCoords = null;
		for (long x = -r; x <= r; x++) {
			setProgress((int) x);
			for (long y = -r; y <= r; y++) {
				if (flag1 && flag2) {
					if (checker.isValidLocation((int) (start.getXAs(res) + x), (int) (start.getYAs(res) + y))) {
						newCoords = CoordinatesInWorld.from(
								start.getX() + res.convertFromThisToWorld(x)
										+ Resolution.NETHER.convertFromWorldToThis(structureOffset),
								start.getY() + res.convertFromThisToWorld(y)
										+ Resolution.NETHER.convertFromWorldToThis(structureOffset));
						Main.addRow(new CoordData(start, newCoords));
					}
				} else {
					if (checker.isValidLocation((int) (start.getXAs(res) + x), (int) (start.getYAs(res) + y))) {
						newCoords = CoordinatesInWorld.from(
								start.getX() + res.convertFromThisToWorld(x) + structureOffset,
								start.getY() + res.convertFromThisToWorld(y) + structureOffset);
						Main.addRow(new CoordData(start, newCoords));
					}
				}
			}
		}
		setProgress(Main.getProgressBar().getMinimum());
	}
	
	private void setProgress(int i) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(() -> Main.getProgressBar().setValue(i));
	}

	public RecognisedVersion getRecognisedVersion() {
		return mcInterface.getRecognisedVersion();
	}

	public MinecraftInterface getMinecraftInterface() {
		return mcInterface;
	}

	public WorldBuilder getWorldBuilder() {
		return worldBuilder;
	}
	
	public void dispose() {
		structureWorker.shutdownNow();
	}
	
	@Override
	public void finalize() {
		dispose();
	}
	
	private class StructureWorker extends ThreadPoolExecutor {
		private final List<Future<?>> futures;
		
		public StructureWorker() {
			super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					return new Thread(r, "StructureWorker");
				}
			});
			this.futures = new ArrayList<Future<?>>();
		}
		
		private void cleanList() {
			futures.removeIf(f -> f.isDone());
		}
		
		public Future<?> submit(Runnable r) {
			Future<?> future = super.submit(r);
			futures.add(future);
			cleanList();
			return future;
		}
		
		public void cancel() {
			futures.forEach(f -> f.cancel(true));
			cleanList();
		}
		
		public boolean isRunning() {
			return getActiveCount() > 0;
		}
		
	}
	
}
