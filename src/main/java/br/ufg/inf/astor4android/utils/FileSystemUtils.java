package br.ufg.inf.astor4android.utils;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Set;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

public class FileSystemUtils {
	
	public static File createTemporaryCopyDirectory(File directory) throws Exception {
		File tmpDirectory = FileUtils.getTempDirectory();
		FileUtils.deleteDirectory(new File(tmpDirectory.getAbsolutePath() + File.separator + directory.getName()));
		FileUtils.copyDirectoryToDirectory(directory, tmpDirectory);
		return new File(tmpDirectory.getAbsolutePath() + File.separator + directory.getName());
	}

	private static void getAllPermissions(File file) throws Exception {
	    Set<PosixFilePermission> perms = Files.readAttributes(file.toPath(),PosixFileAttributes.class).permissions();
		perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        Files.setPosixFilePermissions(file.toPath(), perms);
	}

	public static void getPermissionsForDirectory(File directory) throws Exception {

		ArrayList<File> files = new ArrayList<>(Arrays.asList(directory.listFiles()));

    	for (File child : files){
    		if(child.isDirectory())
    			getPermissionsForDirectory(child);

    		getAllPermissions(child);
   	 	}

   	 	getAllPermissions(directory);
	}

	public static List<String> findFilesWithExtension(File directory, String extension, boolean fullPath) throws Exception {
		ArrayList<String> filesFound = new ArrayList<>();
		ArrayList<File> files = new ArrayList<>(Arrays.asList(directory.listFiles()));

    	for (File child : files){
    		if(child.isDirectory())
    			filesFound.addAll(findFilesWithExtension(child, extension, fullPath));

    		if(child.getName().contains("." + extension)){
                if(fullPath)
                    filesFound.add(child.getAbsolutePath());
                else
                    filesFound.add(child.getName());
            }
   	 	}

   	 	return filesFound;
	}

    public static List<String> listContentsDirectory(File directory) throws Exception {
        ArrayList<String> filesFound = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>(Arrays.asList(directory.listFiles()));

        for (File child : files)
            filesFound.add(child.getName());
        
        return filesFound;
    }

    /* Replace '/' with the correct file separator */
    public static String fixPath(String path) {
        return path.replace("/",File.separator);
    }

}