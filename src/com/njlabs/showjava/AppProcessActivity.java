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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.dex2jar.reader.DexFileReader;
import com.googlecode.dex2jar.v3.Dex2jar;

public class AppProcessActivity extends Activity {

	private ScrollView CommandScroller;
	private LinearLayout CommandDisplay;
	
	private TextView CurrentStatus;
	private TextView CurrentLine;
	private String PackageId;
	private String PackageDir;
	private String PackageName;
	
	DatabaseHandler db;
	Boolean isJar=false;
	
	File TempDir;
	
	String JavaOutputDir;
	
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
        AppName.setText(PackageName);
        
        CurrentStatus=(TextView) findViewById(R.id.current_status);
        CurrentLine=(TextView) findViewById(R.id.current_line);
        
		/*CommandScroller = (ScrollView) findViewById(R.id.CommandScroller);
		CommandDisplay = (LinearLayout) findViewById(R.id.CommandDisplay);*/
		TempDir=this.getCacheDir();
		db=new DatabaseHandler(this);
		Processor process=new Processor();
		process.execute();
	}
	public class Processor extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {
			if(!isJar)
			{
				PrepareDex(this);
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
			/*TextView CommandStatus=new TextView(getBaseContext());
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
			CommandScroller.post(new Runnable() {
			    @Override
			    public void run() {
			    	CommandScroller.fullScroll(ScrollView.FOCUS_DOWN);
			    }
			});
			Bitmap bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_action_bar), 
	                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
	                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height), 
	                true);
	        PendingIntent pendingIntent = PendingIntent.getActivity(getBaseContext(), 01, new Intent(), Intent.FLAG_ACTIVITY_CLEAR_TASK);
	        
			Notification notification = new Notification.Builder(getBaseContext())
	        											.setContentTitle("Show Java is Decompiling ("+PackageId+")")
	        											.setContentText("Processing APK file")
	        											.setSmallIcon(R.drawable.ic_launcher)
	        											.setContentIntent(pendingIntent)
	        											.setNumber(101)
	        											.setOngoing(true)
	        											.setTicker("Decompiling "+PackageId)
	        											.setSmallIcon(R.drawable.ic_launcher)
	        											.setLargeIcon(bm)
	        											.setAutoCancel(false)
	        											.getNotification();	    
			notification.flags=Notification.FLAG_ONGOING_EVENT;
	        NotificationManager notificationManger = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	        notificationManger.notify(101, notification);
	        
			notification = new Notification.Builder(getBaseContext())
			.setContentText("Processing APK file")
			.getNotification();

			notificationManger = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManger.notify(101, notification);*/
			CurrentStatus.setText("Preparing Decompiler");
			
		}
		
		@Override
		protected void onProgressUpdate(String... text) {
			if(text[0].equals("start_activity"))
			{
				db.addHistoryItem(new DecompileHistoryItem(PackageId, PackageName,DateFormat.getDateInstance().format(new Date())));
				Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
				i.putExtra("java_source_dir",JavaOutputDir+"/");
				i.putExtra("package_id",PackageId);
				startActivityForResult(i,1); 
			}
			else if(text[0].equals("start_activity_with_error"))
			{
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
				/*TextView CommandStatus=new TextView(getBaseContext());
				CommandStatus.setText(">> "+text[0]);
				CommandDisplay.addView(CommandStatus);
				CommandScroller.post(new Runnable() {
				    @Override
				    public void run() {
				    	CommandScroller.fullScroll(ScrollView.FOCUS_DOWN);
				    }
				});
				
				Notification notification = new Notification.Builder(getBaseContext())
				.setContentText(text[0])
				.getNotification();

				NotificationManager notificationManger = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
				notificationManger.notify(101, notification);*/
				
				if(text[0].equals("optimising"))
				{
					CurrentStatus.setText("Optimising dex file");
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
					CurrentLine.setText(text[0]);
				}
			}
		}
	}
	private void PrepareDex(Processor task)
	{
		/*InputStream is;
		ZipInputStream zis;
	    try 
	    {
	    	String filename;
	        is = new FileInputStream(PackageDir);
	        zis = new ZipInputStream(new BufferedInputStream(is));          
	        ZipEntry ze;
	        byte[] buffer = new byte[1024];
	        int count;
	        while ((ze = zis.getNextEntry()) != null) 
	        {
	        	filename=ze.getName();
	            if(filename.equals("classes.dex"))
	            {
	            
	            	FileOutputStream fout = new FileOutputStream(TempDir+"/"+PackageId+".dex");
		            while ((count = zis.read(buffer)) != -1) 
		            {
		                fout.write(buffer, 0, count);             
		            }
		            fout.close();               
		            zis.closeEntry();
	            	break;
	            }
	        }
	        zis.close();
	    } 
	    catch(IOException e)
	    {
	    	e.printStackTrace();
	    }*/
		task.doProgress("optimising");
		DexFile dexFile = null;
		try 
		{
			dexFile = DexFileFactory.loadDexFile(PackageDir, 19);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		List<ClassDef> classes = new ArrayList<ClassDef>();
		for (ClassDef classDef: dexFile.getClasses()) {
		    if (!classDef.getType().startsWith("Landroid/support/")&&!classDef.getType().startsWith("Lcom/actionbarsherlock/")&&!classDef.getType().startsWith("Lorg/apache/")&&!classDef.getType().startsWith("Lorg/acra/")&&!classDef.getType().startsWith("Lcom/google/")&&!classDef.getType().startsWith("Lcom/google/android/gms/")&&!classDef.getType().startsWith("Lcom/google/api/")&&!classDef.getType().startsWith("Lcom/njlabs/")) {
		    	task.doProgress("Optimising "+classDef.getType());
		    	classes.add(classDef);
		    }
		}
		dexFile = new ImmutableDexFile(classes);
		try 
		{
			DexFileFactory.writeDexFile(TempDir+"/"+PackageId+".dex", dexFile);
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
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
			DexFileReader reader = new DexFileReader(new File(TempDir+"/"+PackageId+".dex"));
			Dex2jar.from(reader).reUseReg(reuseReg).topoLogicalSort(topologicalSort || topologicalSort1).skipDebug(!debugInfo)
				.optimizeSynchronized(optmizeSynchronized).printIR(printIR).verbose(verbose).to(file);
		}
		catch(IOException e)
		{
			ACRA.getErrorReporter().handleSilentException(e);
			task.doProgress("exit_process_on_error");
		}
		Log.i("STATUS","Clearing cache");
		File ClassDex=new File(TempDir+"/"+PackageId+".dex");
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














