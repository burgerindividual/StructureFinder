package main;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import amidst.mojangapi.file.LauncherProfile;
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
import amidst.mojangapi.world.icon.locationchecker.BuriedTreasureLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.LocationChecker;
import amidst.mojangapi.world.icon.locationchecker.NetherFortressAlgorithm;
import amidst.mojangapi.world.icon.locationchecker.PillagerOutpostLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.ScatteredFeaturesLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.VillageLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.WoodlandMansionLocationChecker;
import amidst.mojangapi.world.icon.producer.StrongholdProducer_128Algorithm;
import amidst.mojangapi.world.versionfeatures.DefaultVersionFeatures;
import amidst.mojangapi.world.versionfeatures.VersionFeatures;
import amidst.parsing.FormatException;

public class StructureFinder extends Thread {
	private static VersionFeatures versionFeatures;
	private static MinecraftInterface mcInterface;
	private static WorldBuilder worldBuilder;
	private final WorldSeed worldSeed;
	private final WorldType worldType;
	private final String structureType;
	private final int radius;
	private final CoordinatesInWorld startPos;
	
	private final Resolution resolution;
	private final boolean unlikelyEndCities;
	
	private LocationChecker locationChecker;
	private World world;
	private int structureOffset;
	private boolean isStrongholdSearch = false;
	
	public StructureFinder(String worldSeed, String worldType, String structureType, int radius,
			CoordinatesInWorld startPos, Resolution resolution, boolean unlikelyEndCities) {
		this.worldSeed = WorldSeed.fromUserInput(worldSeed);
		this.worldType = parseWorldType(worldType);
		this.structureType = structureType;
		this.radius = radius;
		this.startPos = startPos;
		this.resolution = resolution;
		this.unlikelyEndCities = unlikelyEndCities;
		setName("StructureWorker");
	}
	
	@Override
	public void run() {
		Main.setChangeVersions(false);
		createWorld(worldSeed, worldType);
		locationChecker = parseLocationChecker(structureType, worldSeed);
		Main.setIntermediate(false);
		if (!isStrongholdSearch) {
			scanForStructure(locationChecker, startPos, resolution, radius);
		} else {
			strongholdSearch(worldSeed.getLong(), radius, startPos);
		}
		world.dispose();
		Main.setChangeVersions(true);
	}
	
	public void createWorld(WorldSeed seed, WorldType type) {
		Consumer<World> onDispose = world -> {
			world = null;
		};
		WorldOptions worldOptions = new WorldOptions(seed, type);
		try {
			world = worldBuilder.from(mcInterface, onDispose, worldOptions);
		} catch (MinecraftInterfaceException | NullPointerException e) {
			Main.errorProcedure(e, false);
		}
	}
	
	public LocationChecker parseLocationChecker(String structtype, WorldSeed seed) {
		switch (structtype) {
		case "Village":
			structureOffset = 4;
			return new VillageLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesForStructure_Village(), versionFeatures.getDoComplexVillageCheck());
		case "Mineshaft":
			structureOffset = 8;
			return versionFeatures.getMineshaftAlgorithmFactory().apply(seed.getLong());
		case "Mansion":
			structureOffset = 8;
			return new WoodlandMansionLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesForStructure_WoodlandMansion());
		case "Jungle Temple":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_JungleTemple(),
					versionFeatures.getSeedForStructure_JungleTemple(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Desert Temple":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_DesertTemple(),
					versionFeatures.getSeedForStructure_DesertTemple(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Igloo":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_Igloo(), versionFeatures.getSeedForStructure_Igloo(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Shipwreck":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getMaxDistanceScatteredFeatures_Shipwreck(), (byte) 8,
					versionFeatures.getValidBiomesAtMiddleOfChunk_Shipwreck(),
					versionFeatures.getSeedForStructure_Shipwreck(), versionFeatures.getBuggyStructureCoordinateMath());
		case "Witch Hut":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_WitchHut(),
					versionFeatures.getSeedForStructure_WitchHut(), versionFeatures.getBuggyStructureCoordinateMath());
		case "Stronghold":
			structureOffset = 4;
			isStrongholdSearch = true;
			return null;
		case "Ocean Monument":
			structureOffset = 8;
			return versionFeatures.getOceanMonumentLocationCheckerFactory().apply(seed.getLong(),
					world.getBiomeDataOracle(), versionFeatures.getValidBiomesAtMiddleOfChunk_OceanMonument(),
					versionFeatures.getValidBiomesForStructure_OceanMonument());
		case "Ocean Ruin":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(), (byte) 16, (byte) 8,
					versionFeatures.getValidBiomesAtMiddleOfChunk_OceanRuins(),
					versionFeatures.getSeedForStructure_OceanRuins(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Nether Fortress":
			structureOffset = 88;
			return new NetherFortressAlgorithm(seed.getLong());
		case "End City":
			structureOffset = 8;
			return new RefinedEndCityLocationChecker(seed.getLong(), world.getEndIslandOracle(), unlikelyEndCities);
		case "Buried Treasure":
			structureOffset = 9;
			return new BuriedTreasureLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_BuriedTreasure(),
					versionFeatures.getSeedForStructure_BuriedTreasure());
		case "Pillager Outpost":
			structureOffset = 4;
			return new PillagerOutpostLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesForStructure_PillagerOutpost());
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
	
	public static void init(RecognisedVersion ver, File installLocation)
			throws FormatException, IOException, MinecraftInterfaceCreationException {
		versionFeatures = DefaultVersionFeatures.create(ver);
		final MinecraftInstallation minecraftInstallation = installLocation != null
				? MinecraftInstallation.newLocalMinecraftInstallation(installLocation)
				: MinecraftInstallation.newLocalMinecraftInstallation();
		LauncherProfile launcherProfile = null;
		launcherProfile = minecraftInstallation.newLauncherProfile(ver.getName());
		mcInterface = MinecraftInterfaces.fromLocalProfile(launcherProfile);
		worldBuilder = WorldBuilder.createSilentPlayerless();
	}
	
	private void strongholdSearch(long seed, int radius, CoordinatesInWorld start) {
		StrongholdProducer_128Algorithm shp = new StrongholdProducer_128Algorithm(seed, world.getBiomeDataOracle(),
				versionFeatures.getValidBiomesAtMiddleOfChunk_Stronghold());
		List<CoordinatesInWorld> coords = shp.getWorldIcons().stream().map(icon -> icon.getCoordinates())
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
	
	public void scanForStructure(LocationChecker checker, CoordinatesInWorld start, Resolution res, int r) {
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
	
	private void setProgress(int i) {
		try {
			SwingUtilities.invokeAndWait(() -> Main.getProgressBar().setValue(i));
		} catch (InvocationTargetException | InterruptedException e) {
			Main.errorProcedure(e, false);
		}
	}
	
	public WorldSeed getSeed() {
		return worldSeed;
	}
	
	public WorldType getWorldType() {
		return worldType;
	}
	
	public String getStructureType() {
		return structureType;
	}
	
	public int getRadius() {
		return radius;
	}
	
	public CoordinatesInWorld getStartPos() {
		return startPos;
	}
	
	public VersionFeatures getVersionFeatures() {
		return versionFeatures;
	}
	
}
