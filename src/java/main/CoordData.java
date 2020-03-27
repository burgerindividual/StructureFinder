package main;

import java.text.DecimalFormat;

import amidst.mojangapi.world.coordinates.CoordinatesInWorld;

public class CoordData {
	private static final DecimalFormat FORMATTER = new DecimalFormat("###.#");
	
	private final long x;
	private final long z;
	private final double distance;
	private final float angle;
	
	public CoordData(CoordinatesInWorld start, CoordinatesInWorld newCoord) {
		x = newCoord.getX();
		z = newCoord.getY();
		distance = start.getDistance(newCoord);
		angle = AngleHelper.calculateAngle(start.getX(), start.getY(), x, z);
	}
	
	public long getX() {
		return x;
	}
	
	public long getZ() {
		return z;
	}
	
	public double getDistance() {
		return distance;
	}
	
	public float getAngle() {
		return angle;
	}
	
	public static String formatAngle(float angle) {
		return FORMATTER.format(angle) + (char) 176;
	}
	
	@Override
	public String toString() {
		return "(" + x + ", " + z + ")";
	}
}
