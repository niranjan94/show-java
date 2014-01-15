package com.njlabs.showjava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import android.app.ProgressDialog;
import android.os.Handler;

public class Tools {
	
	public static void unzipJar(String destinationDir, String jarPath,Handler myHandler,final ProgressDialog GetJavaDialog) throws IOException {
		File file = new File(jarPath);
		JarFile jar = new JarFile(file);
 
		// fist get all directories,
		// then make those directory on the destination Path
		for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
			JarEntry entry = (JarEntry) enums.nextElement();
 
			String fileName = destinationDir + File.separator + entry.getName();
			File f = new File(fileName);
 
			if (fileName.endsWith("/"))
			{
				f.mkdirs();
			}
 
		}
 
		//now create all files
		for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {
			JarEntry entry = (JarEntry) enums.nextElement();
 
			final String fileName = destinationDir + File.separator + entry.getName();
			File f = new File(fileName);
			
			myHandler.post(new Runnable() {
				@Override
				public void run()
				{
					GetJavaDialog.setMessage("Extracting jar ("+fileName+")");	
				}
			});
			
			if (!fileName.endsWith("/")) {
				InputStream is = jar.getInputStream(entry);
				FileOutputStream fos = new FileOutputStream(f);
 
				// write contents of 'is' to 'fos'
				while (is.available() > 0) {
					fos.write(is.read());
				}
 
				fos.close();
				is.close();
			}
		}
		jar.close();
	}
	
	public static String exec(String command)
	{
        try
		{
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0)
			{
                output.append(buffer, 0, read);
            }
            reader.close();
            process.waitFor();
            return output.toString();
        }
		catch (IOException e)
		{
            throw new RuntimeException(e);
        }
		catch (InterruptedException e)
		{
            throw new RuntimeException(e);
        }
    }
	
	
}
