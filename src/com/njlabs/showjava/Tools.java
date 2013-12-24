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

import org.apache.commons.io.FilenameUtils;

import android.app.ProgressDialog;
import android.os.Handler;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

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
	
	public static void ApkToJar(File newApkDir,File ApkFile) throws IOException
	{
		// DEX 2 JAR CONFIGS
		boolean reuseReg = false; // reuse register while generate java .class file
		boolean topologicalSort1 = false; // same with --topological-sort/-ts
		boolean topologicalSort = false; // sort block by topological, that will generate more readable code
		boolean verbose = true; // show progress
		boolean debugInfo = false; // translate debug info
		boolean printIR = false; // print ir to Syste.out
		boolean optmizeSynchronized = false; // Optimise-synchronised
		//////
		File file = new File(newApkDir+"/"+FilenameUtils.getBaseName(ApkFile.toString()) + ".jar");
		DexFileReader reader = new DexFileReader(new File(ApkFile.toString()));
		Dex2jar.from(reader).reUseReg(reuseReg)
        .topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
        .optimizeSynchronized(optmizeSynchronized).printIR(printIR).verbose(verbose).to(file);
	}
	
}
