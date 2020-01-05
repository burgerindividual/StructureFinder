package main;

public class AngleHelper {
	private static final double FRAC_BIAS = Double.longBitsToDouble(4805340802404319232L);
	private static final double[] ASINE_TAB = new double[257];
	private static final double[] COS_TAB = new double[257];
	
	public static float calculateAngle(long x1, long y1, long x2, long y2) {
		return wrapDegrees((float) (atan2(y2 - y1, x2 - x1) * (180F / (float) Math.PI)) - 90);
	}
	
	public static byte getAngleId(float angle) {
		if (angle >= 157.5 || angle <= -157.5) {
			return 1;
		} else if (angle > -157.5 && angle < -112.5) {
			return 2;
		} else if (angle >= -112.5 && angle <= -67.5) {
			return 3;
		} else if (angle > -67.5 && angle < -22.5) {
			return 4;
		} else if (angle >= -22.5 || angle <= 22.5) {
			return 5;
		} else if (angle > 22.5 && angle < 67.5) {
			return 6;
		} else if (angle >= 67.5 || angle <= 112.5) {
			return 7;
		} else if (angle > 112.5 || angle < 157.5) {
			return 8;
		} else {
			return 0;
		}
	}
	
	public static String getIdFromAbbreviation(byte angle) {
		switch (angle) {
		case 1:
			return "N";
		case 2:
			return "NE";
		case 3:
			return "E";
		case 4:
			return "SE";
		case 5:
			return "S";
		case 6:
			return "SW";
		case 7:
			return "W";
		case 8:
			return "NW";
		default:
			return "?";
		}
	}
	
	private static double atan2(double y, double x) {
		double d0 = x * x + y * y;
		if (Double.isNaN(d0)) {
			return Double.NaN;
		} else {
			boolean flag = y < 0.0D;
			if (flag) {
				y = -y;
			}
			
			boolean flag1 = x < 0.0D;
			if (flag1) {
				x = -x;
			}
			
			boolean flag2 = y > x;
			if (flag2) {
				double d1 = x;
				x = y;
				y = d1;
			}
			
			double d9 = fastInvSqrt(d0);
			x = x * d9;
			y = y * d9;
			double d2 = FRAC_BIAS + y;
			int i = (int) Double.doubleToRawLongBits(d2);
			double d3 = ASINE_TAB[i];
			double d4 = COS_TAB[i];
			double d5 = d2 - FRAC_BIAS;
			double d6 = y * d4 - x * d5;
			double d7 = (6.0D + d6 * d6) * d6 * 0.16666666666666666D;
			double d8 = d3 + d7;
			if (flag2) {
				d8 = Math.PI / 2D - d8;
			}
			
			if (flag1) {
				d8 = Math.PI - d8;
			}
			
			if (flag) {
				d8 = -d8;
			}
			
			return d8;
		}
	}
	
	private static double fastInvSqrt(double value) {
		double d0 = 0.5D * value;
		long i = Double.doubleToRawLongBits(value);
		i = 6910469410427058090L - (i >> 1);
		value = Double.longBitsToDouble(i);
		value = value * (1.5D - d0 * value * value);
		return value;
	}
	
	private static float wrapDegrees(float value) {
		float f = value % 360.0F;
		if (f >= 180.0F) {
			f -= 360.0F;
		}
		
		if (f < -180.0F) {
			f += 360.0F;
		}
		
		return f;
	}
	
	static {
		for (int i = 0; i < 257; ++i) {
			double d0 = i / 256.0D;
			double d1 = Math.asin(d0);
			COS_TAB[i] = Math.cos(d1);
			ASINE_TAB[i] = d1;
		}
		
	}
}
