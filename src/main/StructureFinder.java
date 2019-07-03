package main;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;

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
import amidst.mojangapi.world.icon.locationchecker.*;
import amidst.mojangapi.world.versionfeatures.DefaultVersionFeatures;
import amidst.mojangapi.world.versionfeatures.VersionFeatures;
import amidst.parsing.FormatException;

public class StructureFinder extends Thread {
	private static WorldBuilder worldBuilder;
	private WorldSeed worldSeed;
	private WorldType worldType;
	private String structureType;
	private LocationChecker locationChecker;
	private int radius;
	private CoordinatesInWorld startPos;
	private static MinecraftInterface mcInterface;
	private World world;
	private VersionFeatures versionFeatures;
	private int structureOffset;

	public StructureFinder(String seed, String worldtype, String structuretype, int radius, CoordinatesInWorld start) {
		this.versionFeatures = DefaultVersionFeatures.create(RecognisedVersion._1_13_1);
		this.worldSeed = WorldSeed.fromUserInput(seed);
		this.worldType = parseWorldType(worldtype);
		this.structureType = structuretype;
		this.radius = radius;
		this.startPos = start;
	}

	public void run() {
		createWorld(worldSeed, worldType);
		locationChecker = parseLocationChecker(structureType, worldSeed);
		scanForStructure(locationChecker, startPos, radius);
	}

	public void createWorld(WorldSeed seed, WorldType type) {
		Consumer<World> onDispose = world -> {
			world.dispose();
		};
		WorldOptions worldOptions = new WorldOptions(seed, type);
		try {
			world = worldBuilder.from(mcInterface, onDispose, worldOptions);
		} catch (MinecraftInterfaceException e) {
			e.printStackTrace();
		}
	}

	public LocationChecker parseLocationChecker(String structtype, WorldSeed seed) {
		switch (structtype) {
		case "Village": // works
			structureOffset = 4;
			return new VillageLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesForStructure_Village(), versionFeatures.getDoComplexVillageCheck());
		case "Mineshaft": // works
			structureOffset = 8;
			return versionFeatures.getMineshaftAlgorithmFactory().apply(seed.getLong());
		case "Mansion": // works
			structureOffset = 8;
			return new WoodlandMansionLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesForStructure_WoodlandMansion());
		case "Jungle Temple": // works
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_JungleTemple(),
					versionFeatures.getSeedForStructure_JungleTemple(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Desert Temple": // works
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_DesertTemple(),
					versionFeatures.getSeedForStructure_DesertTemple(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Igloo": // works
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_Igloo(), versionFeatures.getSeedForStructure_Igloo(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Shipwreck": // works
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getMaxDistanceScatteredFeatures_Shipwreck(), (byte) 8,
					versionFeatures.getValidBiomesAtMiddleOfChunk_Shipwreck(),
					versionFeatures.getSeedForStructure_Shipwreck(), versionFeatures.getBuggyStructureCoordinateMath());
		case "Swamp Hut": // works
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_WitchHut(),
					versionFeatures.getSeedForStructure_WitchHut(), versionFeatures.getBuggyStructureCoordinateMath());
		case "Stronghold": // doesn't work at all
			structureOffset = 0;
			return versionFeatures.getMineshaftAlgorithmFactory().apply(seed.getLong());
		case "Monument": // works
			structureOffset = 8;
			return versionFeatures.getOceanMonumentLocationCheckerFactory().apply(seed.getLong(),
					world.getBiomeDataOracle(), versionFeatures.getValidBiomesAtMiddleOfChunk_OceanMonument(),
					versionFeatures.getValidBiomesForStructure_OceanMonument());
		case "Ocean Ruin": // works
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(), (byte) 16, (byte) 8,
					versionFeatures.getValidBiomesAtMiddleOfChunk_OceanRuins(),
					versionFeatures.getSeedForStructure_OceanRuins(),
					versionFeatures.getBuggyStructureCoordinateMath());
		case "Nether Fortress": // doesn't work at all
			structureOffset = 88;
			return new NetherFortressAlgorithm(seed.getLong());
		case "End City": // lots of false positives due to no influence implementation
			structureOffset = 8;
			return new EndCityLocationChecker(seed.getLong());
		case "Buried Treasure":
			return new BuriedTreasureLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_BuriedTreasure(),
					versionFeatures.getSeedForStructure_BuriedTreasure());
		default:
			System.err.println(
					"parseLocationChecker error: Input did not match any structure type, instead got " + structtype);
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
			System.err.println("parseWorldType error: Input did not match any world type, instead got " + worldtype);
			break;
		}
		return null;
	}

	public static void init() {
		try {
			final MinecraftInstallation minecraftInstallation = MinecraftInstallation.newLocalMinecraftInstallation();
			LauncherProfile launcherProfile = null;
			launcherProfile = minecraftInstallation.newLauncherProfile("1.13.2");
			mcInterface = MinecraftInterfaces.fromLocalProfile(launcherProfile);
			worldBuilder = WorldBuilder.createSilentPlayerless();
		} catch (FormatException | IOException | MinecraftInterfaceCreationException e) {
			e.printStackTrace();
		}
	}

	public void scanForStructure(LocationChecker checker, CoordinatesInWorld start, int r) {
		for (long x = -r; x <= r; x++) {
			int intX = (int) x;
			SwingUtilities.invokeLater(() -> {
				Main.getProgressBar().setValue(intX);
			});
			for (long y = -r; y <= r; y++) {
				if (checker.isValidLocation((int) (start.getX() + x), (int) (start.getY() + y))) {
					CoordinatesInWorld newCoords = new CoordinatesInWorld(((start.getX() + x) << 4) + structureOffset,
							((start.getY() + y) << 4) + structureOffset);
					try {
						SwingUtilities.invokeAndWait(() -> {
							Main.appendText(newCoords.getX() + ", " + newCoords.getY());
						});
					} catch (InvocationTargetException | InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		SwingUtilities.invokeLater(() -> {
			Main.getProgressBar().setValue(Main.getProgressBar().getMinimum());
		});
	}

	public void setSeed(String seed) {
		this.worldSeed = WorldSeed.fromUserInput(seed);
	}

	public void setWorldType(String worldtype) {
		this.worldType = parseWorldType(worldtype);
	}

	public void setStructureType(String structuretype) {
		this.structureType = structuretype;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public void setStartPos(CoordinatesInWorld start) {
		this.startPos = start;
	}

	public WorldSeed getSeed() {
		return this.worldSeed;
	}

	public WorldType getWorldType() {
		return this.worldType;
	}

	public String getStructureType() {
		return this.structureType;
	}

	public int getRadius() {
		return this.radius;
	}

	public CoordinatesInWorld getStartPos() {
		return this.startPos;
	}

}
