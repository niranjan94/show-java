package com.njlabs.showjava;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.acra.ACRA;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.benf.cfr.reader.Main;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

@SuppressWarnings("unused")
public class AppProcessActivity extends Activity {

	private ScrollView CommandScroller;
	private LinearLayout CommandDisplay;
	
	private String PackageId;
	private String PackageDir;

	String JavaOutputDir;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		      
		setContentView(R.layout.activity_app_process);

		getActionBar().hide();
		
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			PackageId = extras.getString("package_id");
			PackageDir = extras.getString("package_dir");
		}
		
		CommandScroller = (ScrollView) findViewById(R.id.CommandScroller);
		CommandDisplay = (LinearLayout) findViewById(R.id.CommandDisplay);
		Processor process=new Processor();
		process.execute();
	}
	
	public class Processor extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			CopyApk(this);
			ExtractJar(this);
			DecompileJar(this);
			return null;
		}
		
		@Override
		protected void onPostExecute(String output) {
			// execution of result of Long time consuming operation
			
		}
		
		public void doProgress(String value){
	        publishProgress(value);
	    }
		
		@Override
		protected void onPreExecute() {
			TextView CommandStatus=new TextView(getBaseContext());
			CommandStatus.setText(">> Starting Process");
			CommandDisplay.addView(CommandStatus);
			CommandScroller.fullScroll(View.FOCUS_DOWN);
			CommandStatus=new TextView(getBaseContext());
			CommandStatus.setText(">> The Process might take quite some time for larger apk's");
			CommandDisplay.addView(CommandStatus);
			CommandScroller.fullScroll(View.FOCUS_DOWN);
			CommandStatus=new TextView(getBaseContext());
			CommandStatus.setText(">> Processing APK file");
			CommandDisplay.addView(CommandStatus);
			CommandScroller.fullScroll(View.FOCUS_DOWN);
		}
		
		@Override
		protected void onProgressUpdate(String... text) {
			if(text[0].equals("start_activity"))
			{
				Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
				i.putExtra("java_source_dir",JavaOutputDir+"/");
				i.putExtra("package_id",PackageId);
				startActivityForResult(i,1);
			}
			else if(text[0].equals("exit_process_on_error"))
			{
				Toast.makeText(getApplicationContext(), "Cannot decompile this application. classes.dex was not found in the apk file.", Toast.LENGTH_LONG).show();
				finish();
			}
			else
			{
				TextView CommandStatus=new TextView(getBaseContext());
				CommandStatus.setText(">> "+text[0]);
				CommandDisplay.addView(CommandStatus);
				CommandScroller.fullScroll(View.FOCUS_DOWN);
			}
		}
	}
	private void CopyApk(Processor task)
	{
		//Log.d("DEBUG","Copy Apk");
		
		File path = Environment.getExternalStorageDirectory();
		
		File WorkingDirectory=new File(path + "/ShowJava");
		//Log.d("DEBUG","d"+WorkingDirectory);
		File PerAppWorkingDirectory=new File(WorkingDirectory + "/" + PackageId);
		File OutDirectory=new File(PerAppWorkingDirectory + "/out");
		if (!WorkingDirectory.isDirectory())
		{
			WorkingDirectory.mkdir();
		}
		if (!PerAppWorkingDirectory.isDirectory())
		{
			PerAppWorkingDirectory.mkdir();
		}
		else
		{
			PerAppWorkingDirectory.delete();
			PerAppWorkingDirectory.mkdir();
		}
		
		//String cmd="/system/bin/cp " + PackageDir + " " + PerAppWorkingDirectory;
		//String output=Tools.exec(cmd);
		try
		{
			FileUtils.copyFile(new File(PackageDir), new File(PerAppWorkingDirectory+"/"+PackageId+".apk"));
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			ACRA.getErrorReporter().handleSilentException(e);
		}

		File oldApkFile=new File(WorkingDirectory + "/"+ PackageId +"/"+FilenameUtils.getBaseName(PackageDir.toString())+".apk");
		File ApkFile=new File(WorkingDirectory + "/"+ PackageId +"/"+PackageId+".apk");
		oldApkFile.renameTo(ApkFile);
	}
	private void ExtractJar(Processor task)
	{
		Log.d("DEBUG","Extract Jar");
		task.doProgress("Extracting jar from the apk file");
		// DEX 2 JAR CONFIGS
		boolean reuseReg = false; // reuse register while generate java .class file
		boolean topologicalSort1 = false; // same with --topological-sort/-ts
		boolean topologicalSort = false; // sort block by topological, that will generate more readable code
		boolean verbose = true; // show progress
		boolean debugInfo = false; // translate debug info
		boolean printIR = false; // print ir to Syste.out
		boolean optmizeSynchronized = true; // Optimise-synchronised
		//////
		PrintStream printStream = new PrintStream(new ProgressStream(task));
		System.setErr(printStream);
		System.setOut(printStream);
		//////
	
		File WorkingDirectory=new File(Environment.getExternalStorageDirectory() + "/ShowJava");
		File PerAppWorkingDirectory=new File(WorkingDirectory + "/" + PackageId);
		File ApkFile=new File(WorkingDirectory + "/"+ PackageId +"/"+PackageId+".apk");
		
		File file = new File(PerAppWorkingDirectory+"/"+PackageId+ ".jar");
		
		try
		{
			DexFileReader reader = new DexFileReader(new File(ApkFile.toString()));
			Dex2jar.from(reader).reUseReg(reuseReg).topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
				.optimizeSynchronized(optmizeSynchronized).printIR(printIR).verbose(verbose).to(file);
		}
		catch(IOException e)
		{
			ACRA.getErrorReporter().handleSilentException(e);
			task.doProgress("exit_process_on_error");
		}

	}
	private void DecompileJar(final Processor task)
	{
		task.doProgress("------------------------------------------");
		task.doProgress("Decompiling jar to java files");
		task.doProgress("------------------------------------------");
		File JarInput = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/"+PackageId+".jar");
		final File JavaOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/java_output");
		if (!JavaOutputDir.isDirectory())
		{
			JavaOutputDir.mkdir();
		}
		this.JavaOutputDir=JavaOutputDir.toString();
		String[] args = {JarInput.toString(), "--outputdir", JavaOutputDir.toString()};
		//main(args);
		GetOptParser getOptParser = new GetOptParser();
		    
		Options options = null;
		try
		{
			options = (Options)getOptParser.parse(args, OptionsImpl.getFactory());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		final DCCommonState dcCommonState = new DCCommonState(options);
		final String path = options.getFileName();

		ThreadGroup group = new ThreadGroup("Jar 2 Java Group");
		
		Runnable run_process=new Runnable(){
			@Override
			public void run(){
				try
				{
					Main.doJar(dcCommonState,path);
				}
				catch(Exception e)
				{
					Exception CustomError=new Exception("APP PACKAGE ID: "+PackageId+" (ERROR : "+e+")");
					ACRA.getErrorReporter().handleSilentException(CustomError);
					Toast.makeText(getApplicationContext(), "Decompilation completed with errors. This incident has been reported to the developer.", Toast.LENGTH_LONG).show();
				}
				task.doProgress("start_activity");
			}
		};
		new Thread(group, run_process, "Jar to Java Thread", 15728640).start();
		
	}
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		  if (requestCode == 1) {
		     if(resultCode == RESULT_OK){      
		         String result=data.getStringExtra("result");    
		         finish();
		     }
		     if (resultCode == RESULT_CANCELED) {    
		    	 finish();
		     }else
		     {
		    	 finish();
		     }		     
		  }
	}
}














