package main;

import java.awt.Component;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import amidst.logging.AmidstMessageBox;
import amidst.util.FileExtensionChecker;

public class FileSaveHelper {
	public static Path saveToPath(Component parent, String fileExtention, String seed, String structure) {
		String suggestedFilename = structure  + "s" + "_" + seed + "." + fileExtention;
		Path file = showSaveDialogAndGetSelectedFileOrNull(parent, createSaveFileChooser(suggestedFilename, fileExtention));
		if (file != null) {
			file = appendFileExtensionIfNecessary(file, fileExtention);
			boolean fileExists = Files.exists(file);
			if (fileExists && !Files.isRegularFile(file)) {
				String message = "Unable to write screenshot, because the target exists but is not a file: "
						+ file.toString();
				Main.errorProcedure(message, false);
			} else if (!canWriteToFile(file)) {
				String message = "Unable to write screenshot, because you have no writing permissions: "
						+ file.toString();
				Main.errorProcedure(message, false);
			} else if (!fileExists || AmidstMessageBox.askToConfirmYesNo(parent,
					"Replace file?",
					"File already exists. Do you want to replace it?\n" + file.toString() + "")) {
				return file;
			}
		}
		return null;
	}
	
	private static JFileChooser createSaveFileChooser(String suggestedFilename, String fileExtention) {
		JFileChooser result = new JFileChooser();
		result.setFileFilter(new FileNameExtensionFilter(fileExtention.toUpperCase() + " File", fileExtention));
		result.setAcceptAllFileFilterUsed(false);
		result.setSelectedFile(new java.io.File(suggestedFilename));
		return result;
	}
	
	private static Path appendFileExtensionIfNecessary(Path file, String fileExtention) {
		String filename = file.toAbsolutePath().toString();
		if (!FileExtensionChecker.hasFileExtension(filename, fileExtention)) {
			filename += fileExtention;
		}
		return Paths.get(filename);
	}
	
	private static boolean canWriteToFile(Path file) {
		Path parent = file.getParent();
		return Files.isWritable(file) || (!Files.exists(file) && parent != null && Files.isWritable(parent));
	}
	
	private static Path showSaveDialogAndGetSelectedFileOrNull(Component parent, JFileChooser fileChooser) {
		if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
			return fileChooser.getSelectedFile().toPath();
		} else {
			return null;
		}
	}
}
