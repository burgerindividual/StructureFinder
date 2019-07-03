package main;

import java.awt.Color;
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
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.mojangapi.world.icon.locationchecker.BuriedTreasureLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.EndCityLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.LocationChecker;
import amidst.mojangapi.world.icon.locationchecker.NetherFortressAlgorithm;
import amidst.mojangapi.world.icon.locationchecker.PillagerOutpostLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.ScatteredFeaturesLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.VillageLocationChecker;
import amidst.mojangapi.world.icon.locationchecker.WoodlandMansionLocationChecker;
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
	private Resolution resolution;

	public StructureFinder(String seed, String worldtype, String structuretype, int radius, CoordinatesInWorld start,
			Resolution resolution) {
		versionFeatures = DefaultVersionFeatures.create(RecognisedVersion._1_14_3);
		worldSeed = WorldSeed.fromUserInput(seed);
		worldType = parseWorldType(worldtype);
		structureType = structuretype;
		this.radius = radius;
		startPos = start;
		this.resolution = resolution;
	}

	@Override
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
		case "Swamp Hut":
			structureOffset = 8;
			return new ScatteredFeaturesLocationChecker(seed.getLong(), world.getBiomeDataOracle(),
					versionFeatures.getValidBiomesAtMiddleOfChunk_WitchHut(),
					versionFeatures.getSeedForStructure_WitchHut(), versionFeatures.getBuggyStructureCoordinateMath());
		case "Stronghold": // doesn't work at all
			structureOffset = 0;
			return versionFeatures.getMineshaftAlgorithmFactory().apply(seed.getLong());
		case "Monument":
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
		case "Nether Fortress": // mostly works
			structureOffset = 88;
			return new NetherFortressAlgorithm(seed.getLong());
		case "End City": // lots of false positives due to no influence implementation
			structureOffset = 8;
			return new EndCityLocationChecker(seed.getLong());
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
			launcherProfile = minecraftInstallation.newLauncherProfile("1.14.3");
			mcInterface = MinecraftInterfaces.fromLocalProfile(launcherProfile);
			worldBuilder = WorldBuilder.createSilentPlayerless();
		} catch (FormatException | IOException | MinecraftInterfaceCreationException e) {
			SwingUtilities.invokeLater(() -> {
				Main.appendText("No 1.14.3 Minecraft profile detected, please launch 1.14.3 atleast once and try again", Color.RED);
			});
			e.printStackTrace();
		}
	}

	public void scanForStructure(LocationChecker checker, CoordinatesInWorld start, int r) {
		boolean flag1 = Main.isStructTypeNetherFortress();
		boolean flag2 = Main.isCoordTypeNether();
		for (long x = -r; x <= r; x++) {
			int intX = (int) x;
			SwingUtilities.invokeLater(() -> {
				Main.getProgressBar().setValue(intX);
			});
			for (long y = -r; y <= r; y++) {
				if (checker.isValidLocation((int) (start.getX() + x), (int) (start.getY() + y))) {
					CoordinatesInWorld newCoords = new CoordinatesInWorld(
							resolution.convertFromThisToWorld(start.getX() + x) + structureOffset,
							resolution.convertFromThisToWorld(start.getY() + y) + structureOffset);
					try {
						SwingUtilities.invokeAndWait(() -> {
							if (flag1 && flag2) {
								Main.appendText(newCoords.getXAs(Resolution.NETHER) + ", " + newCoords.getYAs(Resolution.NETHER));
							} else {
								Main.appendText(newCoords.getX() + ", " + newCoords.getY());
							}
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
		worldSeed = WorldSeed.fromUserInput(seed);
	}

	public void setWorldType(String worldtype) {
		worldType = parseWorldType(worldtype);
	}

	public void setStructureType(String structuretype) {
		structureType = structuretype;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}

	public void setStartPos(CoordinatesInWorld start) {
		startPos = start;
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

}
