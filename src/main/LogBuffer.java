package main;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;

public class LogBuffer extends PrintStream {

	public final PrintStream underlying;

	LogBuffer(OutputStream os, PrintStream ul) {
		super(os);
		this.underlying = ul;
	}

	public static LogBuffer create(PrintStream ul) {
		try {
			Field f = FilterOutputStream.class.getDeclaredField("out");
			f.setAccessible(true);
			OutputStream psout = (OutputStream) f.get(ul);

			return new LogBuffer(new FilterOutputStream(psout) {
				public void write(int b) throws IOException {
					ul.write(b);
					Main.logAppend(Character.toString((char) b));
				}
			}, ul);

		} catch (Exception e) {
		}
		return null;
	}
}
