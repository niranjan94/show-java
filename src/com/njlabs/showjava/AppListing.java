package com.njlabs.showjava;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

@SuppressWarnings("unused")
public class AppListing extends Activity {

	ArrayList<PInfo> UserPackages;
	ProgressDialog PackageLoadDialog;
	ProgressDialog GetJavaDialog;
	ListView listView=null;
	View alertView;
	/*
	 * 
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_app_listing);
		
		PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);
        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
		PackageLoadDialog.setMessage("Loading installed applications...");
		
		listView = (ListView) findViewById(R.id.list);
		PackageLoadDialog.show();
		getActionBar().setIcon(R.drawable.ic_action_bar);
		SharedPreferences preferences = getSharedPreferences("pref_showjava_core", Context.MODE_PRIVATE);
		Boolean FirstRun = preferences.getBoolean("FirstRun", true);
		if(FirstRun)
		{
			FirstRun(preferences);
		}
		ApplicationLoader runner = new ApplicationLoader();
		runner.execute();
		
	}
	
	private void FirstRun(final SharedPreferences preferences)
	{
		PackageLoadDialog.setMessage("Preparing aplication for first run...");
		(new Thread(new Runnable() { 
				@Override
				public void run() {
					try
					{
						String RetrievedString;
						int ch;
						StringBuilder str = new StringBuilder();
						InputStream is=getAssets().open("busybox");
						while ((ch = is.read()) != -1)
						{
							str.append((char) ch); 
						}
						is.close();

						RetrievedString = str.toString();

						FileOutputStream fs = openFileOutput("busybox", Context.MODE_PRIVATE); 
						fs.write(RetrievedString.getBytes());
						fs.flush();
						fs.close();
						Tools.exec("/system/bin/chmod 0777 /data/data/com.njlabs.getjava/files/busybox");
					}
					catch (IOException e)
					{
						Log.d("ERROR","IO Exception");
					}
					SharedPreferences.Editor editor = preferences.edit();
					editor.putBoolean("FirstRun", false);
					editor.commit();
				}
			})).start();		
	}
	
	private class ApplicationLoader extends AsyncTask<String, String, ArrayList<PInfo>> {

		@Override
		protected ArrayList<PInfo> doInBackground(String... params) {
			publishProgress("Sleeping..."); // Calls onProgressUpdate()
			return getInstalledApps(true,this);
		}
		
		@Override
		protected void onPostExecute(ArrayList<PInfo> AllPackages) {
			// execution of result of Long time consuming operation
			SetupList(AllPackages);
			PackageLoadDialog.dismiss();
		}
		
		public void doProgress(String value){
	        publishProgress(value);
	    }
		
		@Override
		protected void onPreExecute() {
			
		}
		
		@Override
		protected void onProgressUpdate(String... text) {
			PackageLoadDialog.setMessage(text[0]);
		}
	}
	
	public void SetupList(ArrayList<PInfo> AllPackages)
	{
		ArrayAdapter<PInfo> aa = new ArrayAdapter<PInfo>(getBaseContext(), R.layout.package_list_item, AllPackages)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.package_list_item, null);
				}
				PInfo pkg = getItem(position);
				TextView PkgName=(TextView) convertView.findViewById(R.id.pkg_name);
				TextView PkgId=(TextView) convertView.findViewById(R.id.pkg_id);
				TextView PkgVersion=(TextView) convertView.findViewById(R.id.pkg_version);
				TextView PkgDir=(TextView) convertView.findViewById(R.id.pkg_dir);
				ImageView PkgImg=(ImageView) convertView.findViewById(R.id.pkg_img);

				PkgName.setText(pkg.appname);
				PkgId.setText(pkg.pname);
				PkgVersion.setText("version " + pkg.versionName);
				PkgDir.setText(pkg.sourceDir);
				PkgImg.setImageDrawable(pkg.icon);
				return convertView;	
			}
		};
		//setListAdapter(aa);
		listView.setAdapter(aa);
		listView.setTextFilterEnabled(true);
		/// CREATE AN ONCLICK LISTENER TO KNOW WHEN EACH ITEM IS CLICKED
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				// When clicked, show a toast with the TextView text
				final TextView CPkgId=(TextView) view.findViewById(R.id.pkg_id);
				final TextView CPkgDir=(TextView) view.findViewById(R.id.pkg_dir);
				
				//Toast.makeText(getApplicationContext(), CPkgId.getText() + " --- " + CPkgDir.getText(), Toast.LENGTH_SHORT).show();
				String myapp="com.njlabs";
				if(CPkgId.getText().toString().toLowerCase().contains(myapp.toLowerCase()))
				{
					Toast.makeText(getApplicationContext(),"The application "+CPkgId.getText().toString()+" cannot be decompiled !", Toast.LENGTH_SHORT).show();
				}
				else
				{
	            	final File JavaOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+CPkgId.getText().toString()+"/java_output");
	            	if(JavaOutputDir.isDirectory())
	            	{
	            		AlertDialog.Builder alertDialog = new AlertDialog.Builder(AppListing.this);
				        alertDialog.setTitle("This Package has already been decompiled");
				        alertDialog.setMessage("This application has already been decompiled once and the source exists on your sdcard. What would you like to do ?");
				        //alertDialog.setIcon(R.drawable.delete);
				        alertDialog.setPositiveButton("Browse the Source", new DialogInterface.OnClickListener() {
				            public void onClick(DialogInterface dialog,int which) {
				            	Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
								i.putExtra("java_source_dir",JavaOutputDir+"/");
								i.putExtra("package_id",CPkgId.getText().toString());
								startActivity(i);
				            }
				        });
				        alertDialog.setNegativeButton("Decompile again", new DialogInterface.OnClickListener() {
				            public void onClick(DialogInterface dialog, int which) {
				            	JavaOutputDir.delete();
				            	Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
								i.putExtra("package_id",CPkgId.getText().toString());
								i.putExtra("package_dir",CPkgDir.getText().toString());
								startActivity(i);
				            }
				        });
				        alertDialog.show();
	            	}
	            	else
	            	{
						Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
						i.putExtra("package_id",CPkgId.getText().toString());
						i.putExtra("package_dir",CPkgDir.getText().toString());
						startActivity(i);
	            	}
					
					
				}
			}
		});
	}
	
	
	
	class PInfo
	{
		private String appname = "";
		private String pname = "";
		private String versionName = "";
		private String sourceDir = "";
		private int versionCode = 0;
		private Drawable icon;
		public String getAppname()
		{
			return appname;
		}
	}
	
	
	private ArrayList<PInfo> getInstalledApps(boolean getSysPackages,ApplicationLoader task)
	{
		ArrayList<PInfo> res = new ArrayList<PInfo>();
		List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
		for (int i=0;i < packs.size();i++)
		{
			PackageInfo p = packs.get(i);
			if ((!getSysPackages) && (p.versionName == null))
			{
				continue;
			}
			final int count=i + 1;
			final int total=packs.size();
			final int progressVal=(count / total) * 100;
			final PInfo newInfo = new PInfo();
			newInfo.appname = p.applicationInfo.loadLabel(getPackageManager()).toString();
			
			task.doProgress("Loading application " + count + " of " + total + " (" + newInfo.appname + ")");
			
			newInfo.pname = p.packageName;
			newInfo.versionName = p.versionName;
			newInfo.versionCode = p.versionCode;
			ApplicationInfo appinfo=null;
			try
			{
				appinfo = getPackageManager().getApplicationInfo(p.packageName, 0);
			}
			catch (PackageManager.NameNotFoundException e)
			{
				throw new RuntimeException(e);
			}
			if (appinfo != null)
			{
				newInfo.sourceDir = appinfo.publicSourceDir;
			}
			newInfo.icon = p.applicationInfo.loadIcon(getPackageManager());
			res.add(newInfo);
		}
		Comparator<PInfo> AppNameComparator = new Comparator<PInfo>(){
			public int compare(PInfo o1, PInfo o2)
			{
				return o1.getAppname().compareTo(o2.getAppname());
			}	
		};
		Collections.sort(res, AppNameComparator);
		return res; 
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            finish();
	            return true;
	            
	        case R.id.about_option:
	        	AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
	        	LayoutInflater factory = LayoutInflater.from(this);
	        	final View textEntryView = factory.inflate(R.layout.dialog_about, null);
	        	          
	        	alt_bld.setView(textEntryView).setCancelable(true)
	        	                   .setPositiveButton("Close !", new DialogInterface.OnClickListener() {
	        	         	          public void onClick(DialogInterface dialog, int id) {
	        	         	        	 dialog.cancel();
	        	        	          }
	        	        	      });
	        	                    
	        	AlertDialog alert = alt_bld.create();
	        	alert.setTitle("About");
	        	alert.setIcon(R.drawable.ic_action_bar);
	        	alert.show();
	        	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}

}
