package com.njlabs.showjava;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.acra.ACRA;
import org.apache.commons.io.FilenameUtils;
import org.benf.cfr.reader.Main;
import org.benf.cfr.reader.state.DCCommonState;
import org.benf.cfr.reader.util.getopt.GetOptParser;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

public class AppProcessActivity extends Activity {
	
	private TextView CurrentStatus;
	private TextView CurrentLine;
	private String PackageId;
	private String PackageDir;
	private String PackageName;
	
	private DatabaseHandler db;
	private Boolean isJar=false;
	
	private String JavaOutputDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {		
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_progress);
		getActionBar().hide();
		
        if((getIntent().getDataString()!=null&&getIntent().getDataString().equals(""))||getIntent().getDataString()==null)
        {
        	Bundle extras = getIntent().getExtras();
    		if (extras != null) {
    			PackageName = extras.getString("package_name");
    			PackageId = extras.getString("package_id");
    			PackageDir = extras.getString("package_dir");
    		}
        }
        else
		{
        	PackageDir = (new File(URI.create(getIntent().getDataString()))).getAbsolutePath();
			if(FilenameUtils.isExtension(PackageDir, "apk"))
			{
				PackageManager pm = getPackageManager();      
			    PackageInfo info = pm.getPackageArchiveInfo(PackageDir, PackageManager.GET_ACTIVITIES);      
			    if(info != null){      
			        ApplicationInfo appInfo = info.applicationInfo;

			        if(Build.VERSION.SDK_INT >= 8){
			            appInfo.sourceDir = PackageDir;
			            appInfo.publicSourceDir = PackageDir;
			        }
			    }
			    PackageName=info.applicationInfo.loadLabel(getPackageManager()).toString();
			    PackageId=info.packageName;
			}
			else
			{
				isJar=true;
				PackageName=FilenameUtils.getName(PackageDir);
			    PackageId=FilenameUtils.getName(PackageDir).replaceAll(" ", "_").toLowerCase();
			}
		}
        TextView AppName=(TextView) findViewById(R.id.current_package_name);
        AppName.setSingleLine(false);
        AppName.setEllipsize(TextUtils.TruncateAt.END);
        AppName.setLines(1);
        AppName.setText(PackageName);
        
        CurrentStatus=(TextView) findViewById(R.id.current_status);
        CurrentLine=(TextView) findViewById(R.id.current_line);
        
        Typeface face=Typeface.createFromAsset(getAssets(), "roboto_light.ttf"); 
		
        AppName.setTypeface(face); 
        CurrentStatus.setTypeface(face);
        CurrentLine.setTypeface(face);
		
        CurrentStatus.setSingleLine(false);
        CurrentStatus.setEllipsize(TextUtils.TruncateAt.END);
        CurrentStatus.setLines(1);
        
        CurrentLine.setSingleLine(false);
        CurrentLine.setEllipsize(TextUtils.TruncateAt.END);
        CurrentLine.setLines(1);
        
        final ImageView GearProgressLeft = (ImageView) findViewById(R.id.gear_progress_left);
        final ImageView GearProgressRight = (ImageView) findViewById(R.id.gear_progress_right);
        
        final RotateAnimation GearProgressLeftAnim = new RotateAnimation(0.0f,360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        GearProgressLeftAnim.setRepeatCount(Animation.INFINITE);
        GearProgressLeftAnim.setDuration((long) 2*1500);
        GearProgressLeftAnim.setInterpolator(this,android.R.anim.linear_interpolator);
        
        final RotateAnimation GearProgressRightAnim = new RotateAnimation(360.0f,0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        GearProgressRightAnim.setRepeatCount(Animation.INFINITE);
        GearProgressRightAnim.setDuration((long) 1*1500);
        GearProgressRightAnim.setInterpolator(this,android.R.anim.linear_interpolator);
                
        GearProgressLeft.post(new Runnable() {   
            @Override
            public void run() {
            	GearProgressLeft.setAnimation(GearProgressLeftAnim);
            }
        });
        GearProgressLeft.post(new Runnable() {   
            @Override
            public void run() {
            	GearProgressRight.setAnimation(GearProgressRightAnim);
            }
        });
        
		
		
		db=new DatabaseHandler(this);
		final Processor process=new Processor();
		
		if(!isJar)
		{			
			ThreadGroup group = new ThreadGroup("Optimise Dex Group");
			CurrentStatus.setText("Preparing Decompiler");
			CurrentStatus.setText("Optimising dex file");
			final Handler UIHandler=new Handler();
			Runnable run_process=new Runnable(){
				@Override
				public void run(){
					
					DexFile dexFile = null;
					try 
					{
						dexFile = DexFileFactory.loadDexFile(PackageDir, 19);
					} 
					catch (IOException e) 
					{
						ACRA.getErrorReporter().handleException(e);
					}
					List<ClassDef> classes = new ArrayList<ClassDef>();
					for (ClassDef classDef: dexFile.getClasses()) {
					    if (
					    	classDef.getType().startsWith("Lcom/google/apps/")
					    	||!classDef.getType().startsWith("Landroid/support/")
					    	&&!classDef.getType().startsWith("Lcom/njlabs/")
					    	&&!classDef.getType().startsWith("Lcom/androidquery/")
					    	&&!classDef.getType().startsWith("Lcom/parse/")
					    	&&!classDef.getType().startsWith("Lcom/android/")
					    	&&!classDef.getType().startsWith("Lcom/actionbarsherlock/")
					    	&&!classDef.getType().startsWith("Lorg/apache/")
					    	&&!classDef.getType().startsWith("Lorg/acra/")
					    	&&!classDef.getType().startsWith("Ljavax/")
					    	&&!classDef.getType().startsWith("Lorg/joda/")
					    	&&!classDef.getType().startsWith("Lorg/antlr/")
					    	&&!classDef.getType().startsWith("Ljunit/")
					    	&&!classDef.getType().startsWith("Lorg/codehaus/jackson/")
					    	&&!classDef.getType().startsWith("Lcom/fasterxml/")
					    	&&!classDef.getType().startsWith("Lcom/google/")
					    	&&!classDef.getType().startsWith("Lcom/bugsense/")
					    	&&!classDef.getType().startsWith("Lorg/andengine/")
					    	&&!classDef.getType().startsWith("Lorg/andengine/")
					    	&&!classDef.getType().startsWith("Landroid/")
					    	&&!classDef.getType().startsWith("Lcom/google/android/gms/")
					    	&&!classDef.getType().startsWith("Lcom/google/api/")
					    	&&!classDef.getType().startsWith("Lcom/njlabs/")) {
					    	
					    	final String CurrentClass=classDef.getType();
					    	UIHandler.post(new Runnable() {   
					    		@Override
					    		public void run() {
					    			CurrentLine.setText(CurrentClass.replaceAll("Processing ", ""));
					    		}
					    	});
					    	classes.add(classDef);
					    }
					}
					File WorkingDirectory=new File(Environment.getExternalStorageDirectory() + "/ShowJava");
					File PerAppWorkingDirectory=new File(WorkingDirectory + "/" + PackageId);
					PerAppWorkingDirectory.mkdirs();
					Log.d("DEBUGGER","Prepare Writing");
					
					
					UIHandler.post(new Runnable() {   
			            @Override
			            public void run() {
			            	CurrentStatus.setText("Finishing optimisation");
							CurrentLine.setText("");
			            }
			        });
					
					dexFile = new ImmutableDexFile(classes);
					
					
					try 
					{
						Log.d("DEBUGGER","Start Writing");
						DexFileFactory.writeDexFile(PerAppWorkingDirectory+"/optimised_classes.dex", dexFile);
						Log.d("DEBUGGER","Writing done!");
					} 
					catch (IOException e) 
					{
						ACRA.getErrorReporter().handleException(e);
					}
					UIHandler.post(new Runnable() {   
			            @Override
			            public void run() {
			            	process.execute();
			            }
			        });
				}
			};
			new Thread(group, run_process, "Optimise Dex Thread", 10485760).start();
		}
		else
		{
			process.execute();
		}
	}
	public class Processor extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			if(!isJar)
			{
				//PrepareDex(this);
				ExtractJar(this);
			}
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
			CurrentStatus.setText("Preparing Decompiler");
		}
		
		@Override
		protected void onProgressUpdate(String... text) {
			if(text[0].equals("start_activity"))
			{
				if(!db.packageExistsInHistory(PackageId))
					db.addHistoryItem(new DecompileHistoryItem(PackageId, PackageName,DateFormat.getDateInstance().format(new Date())));
				Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
				i.putExtra("java_source_dir",JavaOutputDir+"/");
				i.putExtra("package_id",PackageId);
				startActivityForResult(i,1); 
			}
			else if(text[0].equals("start_activity_with_error"))
			{
				if(!db.packageExistsInHistory(PackageId))
					db.addHistoryItem(new DecompileHistoryItem(PackageId, PackageName,DateFormat.getDateInstance().format(new Date())));
				Toast.makeText(getApplicationContext(), "Decompilation completed with errors. This incident has been reported to the developer.", Toast.LENGTH_LONG).show();
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
				if(text[0].equals("optimising"))
				{
					CurrentStatus.setText("Optimising dex file");
				}
				else if(text[0].equals("finaldex"))
				{
					CurrentStatus.setText("Finishing optimisation");
					CurrentLine.setText("");
				}
				else if(text[0].equals("dex2jar"))
				{
					CurrentStatus.setText("Decompiling dex to jar");
				}
				else if(text[0].equals("jar2java"))
				{
					CurrentStatus.setText("Decompiling to java");
				}
				else
				{
					CurrentLine.setText(text[0].replaceAll("Processing ", ""));
					
				}
			}
		}
	}
	
	private void ExtractJar(Processor task)
	{
		Log.i("STATUS","Jar Extraction Started");
		
		task.doProgress("dex2jar");
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
		File file = new File(PerAppWorkingDirectory+"/"+PackageId+ ".jar");
		
		try
		{
			DexFileReader reader = new DexFileReader(new File(PerAppWorkingDirectory+"/optimised_classes.dex"));
			Dex2jar.from(reader).reUseReg(reuseReg).topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
				.optimizeSynchronized(optmizeSynchronized).printIR(printIR).verbose(verbose).to(file);
		}
		catch(IOException e)
		{
			ACRA.getErrorReporter().handleSilentException(e);
			task.doProgress("exit_process_on_error");
		}
		Log.i("STATUS","Clearing cache");
		File ClassDex=new File(PerAppWorkingDirectory+"/optimised_classes.dex");
		ClassDex.delete();
	}
	private void DecompileJar(final Processor task)
	{
		task.doProgress("jar2java");
		File JarInput;
		if(!isJar)
		{
			JarInput = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/"+PackageId+".jar");
		}
		else
		{
			JarInput = new File(PackageDir);
		}
		
		final File JavaOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+PackageId+"/java_output");
		
		if (!JavaOutputDir.isDirectory())
		{
			JavaOutputDir.mkdirs();
		}
		this.JavaOutputDir=JavaOutputDir.toString();
		String[] args = {JarInput.toString(), "--outputdir", JavaOutputDir.toString()};
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
					task.doProgress("start_activity_with_error");
				}
				catch (java.lang.StackOverflowError e) 
				{
					Exception CustomError=new Exception("APP PACKAGE ID: "+PackageId+" (ERROR : "+e+")");
					ACRA.getErrorReporter().handleSilentException(CustomError);
					task.doProgress("start_activity_with_error");
				}	
				task.doProgress("start_activity");
			}
		};
		new Thread(group, run_process, "Jar to Java Thread", 20971520).start();
	}
	@SuppressWarnings("unused")
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














