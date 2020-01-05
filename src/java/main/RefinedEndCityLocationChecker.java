package main;

import java.util.List;

import amidst.mojangapi.world.coordinates.CoordinatesInWorld;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.mojangapi.world.icon.locationchecker.EndCityLocationChecker;
import amidst.mojangapi.world.oracle.EndIsland;
import amidst.mojangapi.world.oracle.EndIslandOracle;

public class RefinedEndCityLocationChecker extends EndCityLocationChecker {
	private final EndIslandOracle endIslandOracle;
	private final boolean unlikelyEndCities;
	
	public RefinedEndCityLocationChecker(long seed, EndIslandOracle endIslandOracle, boolean unlikelyEndCities) {
		super(seed);
		this.endIslandOracle = endIslandOracle;
		this.unlikelyEndCities = unlikelyEndCities;
	}
	
	@Override
	public boolean isValidLocation(int x, int y) {
		if (super.isValidLocation(x, y) && hasSuitableIslandFoundation(x, y, endIslandOracle)) {
			return true;
		}
		return false;
	}
	
	public boolean hasSuitableIslandFoundation(int x, int y, EndIslandOracle endIslandOracle) {
		List<EndIsland> endIslands = endIslandOracle.getAt(CoordinatesInWorld
				.from(Resolution.CHUNK.convertFromThisToWorld(x), Resolution.CHUNK.convertFromThisToWorld(y)));
		for (EndIsland island : endIslands) {
			float influence = island.influenceAtChunk(x, y);
			if (x * x + y * y > 4096 && (influence >= 60 || influence >= 0 && unlikelyEndCities)) {
				return true;
			}
		}
		return false;
	}
}
