package br.ufg.inf.astor4android.utils;

import java.util.zip.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;


public class ZipUtils {
	private final int BUFFER = 2048;
    private List<String> fileList;

    public ZipUtils(){
        fileList  = new ArrayList<String>();
    }

	public void sendFolder(OutputStream os, File folder) throws FileNotFoundException, IOException {
		byte[] buffer = new byte[BUFFER];
        String source = folder.getAbsolutePath();

        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(os));
        List<String> files = generateFileList(source, folder);

        for (String file : files) {

            ZipEntry ze = new ZipEntry(source + File.separator + file);
            zos.putNextEntry(ze);
            
            FileInputStream in = new FileInputStream(source + File.separator + file);

            int len;
            while ((len = in.read(buffer)) > 0) 
                zos.write(buffer, 0, len);
            
            in.close();
    	}
        zos.close();
        fileList.clear();

	}

	private List<String> generateFileList(String source, File node) {
        // add file only
        if (node.isFile()) {
            fileList.add(generateZipEntry(source, node.toString()));
        }

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename: subNote) {
                generateFileList(source, new File(node, filename));
            }
        }

        return fileList;
    }

    private String generateZipEntry(String source, String file) {
        return file.substring(source.length() + 1, file.length());
    }
}