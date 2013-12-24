package com.njlabs.showjava;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("unused")
public class MainActivity extends Activity
{
	ArrayList<PInfo> UserPackages;
	ProgressDialog PackageLoadDialog;
	ProgressDialog GetJavaDialog;
	//AlertDialog alert;
	ListView listView=null;
	View alertView;
	Context thisC;

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		thisC = this;
		PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);

        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
		
		PackageLoadDialog.setMessage("Loading installed applications...");
		//dialog.setProgress(0);

		/*AlertDialog.Builder alt_bld = new AlertDialog.Builder(this);
		 LayoutInflater factory = LayoutInflater.from(this);
		 alertView = factory.inflate(R.layout.dialog_package_progress, null);
		 alt_bld.setView(alertView).setCancelable(false);

		 alert = alt_bld.create();
		 alert.show();*/

		listView = (ListView) findViewById(R.id.list);
		PackageLoadDialog.show();
		
		final SharedPreferences preferences = getSharedPreferences("pref_getjava_core", Context.MODE_PRIVATE);
		Boolean FirstRun = preferences.getBoolean("FirstRun", true);
		
		if(FirstRun)
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
		
		final Handler PackageLoadHandler = new Handler();
		(new Thread(new Runnable() {
			@Override
			public void run()
			{
				final ArrayList<PInfo> AllPackages = getInstalledApps(false, PackageLoadHandler);
				PackageLoadHandler.post(new Runnable() {
					@Override
					public void run()
					{
						SetupList(AllPackages);
						PackageLoadDialog.dismiss();
					}
				});
			}
		})).start();
    }
	public void SetupList(ArrayList<PInfo> AllPackages)
	{
		ArrayAdapter<PInfo> aa = new ArrayAdapter<PInfo>(thisC, R.layout.package_list_item, AllPackages)
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

					Toast.makeText(getApplicationContext(), CPkgId.getText() + " --- " + CPkgDir.getText(), Toast.LENGTH_SHORT).show();

					GetJavaDialog = new ProgressDialog(thisC);
					GetJavaDialog.setIndeterminate(false);
					GetJavaDialog.setCancelable(false);

					GetJavaDialog.setInverseBackgroundForced(false);
					GetJavaDialog.setCanceledOnTouchOutside(false);

					GetJavaDialog.setMessage("Extracting java code from apk...");

					GetJavaDialog.show();
					//Toast.makeText(getApplicationContext(),Environment.getExternalStorageDirectory().toString(), Toast.LENGTH_SHORT).show();
					final Handler GetJavaProcessHandler = new Handler();
					(new Thread(new Runnable() {
						@Override
						public void run()
						{
							final String OutPut = GetJavaProcess(CPkgId.getText().toString(), CPkgDir.getText().toString(), GetJavaProcessHandler);
							GetJavaProcessHandler.post(new Runnable() {
								@Override
								public void run()
								{
									Toast.makeText(getApplicationContext(), OutPut, Toast.LENGTH_SHORT).show();
									GetJavaDialog.dismiss();
								}
							});
						}
					})).start();
				}
			});
	}
	public String GetJavaProcess(String pkgName, String apkDir, Handler myHandler)
	{
		File path = Environment.getExternalStorageDirectory();
		
		File WorkingDirectory=new File(path + "/ShowJava");
		File PerAppWorkingDirectory=new File(WorkingDirectory + "/" + pkgName);
		
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
		myHandler.post(new Runnable() {
			@Override
			public void run()
			{
				GetJavaDialog.setMessage("Preparing apk file for extraction...");	
			}
		});
		
		
		String cmd="/system/bin/cp " + apkDir + " " + PerAppWorkingDirectory;
		String output=Tools.exec(cmd);
		File oldApkFile=new File(WorkingDirectory + "/"+ pkgName +"/"+FilenameUtils.getBaseName(apkDir.toString())+".apk");
		File ApkFile=new File(WorkingDirectory + "/"+ pkgName +"/"+pkgName+".apk");
		oldApkFile.renameTo(ApkFile);

		myHandler.post(new Runnable() {
			@Override
			public void run()
			{
				GetJavaDialog.setMessage("Extracting jar from the APK");	
			}
		});
		///
		
		try 
		{
			Tools.ApkToJar(PerAppWorkingDirectory,ApkFile);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		myHandler.post(new Runnable() {
			@Override
			public void run()
			{
				GetJavaDialog.setMessage("Extracting jar...");	
			}
		});
		
    	try
    	{
			Tools.unzipJar(OutDirectory.toString(), PerAppWorkingDirectory+"/"+FilenameUtils.getBaseName(ApkFile.toString()) + ".jar",myHandler,GetJavaDialog);
		} 
    	catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
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
	
	
	private ArrayList<PInfo> getInstalledApps(boolean getSysPackages, Handler myHandler)
	{
		Looper.prepare();
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

			myHandler.post(new Runnable() {
				@Override
				public void run()
				{
					/*((TextView) alertView.findViewById(R.id.package_progress_name)).setText("Loading "+newInfo.appname);
					 ((ProgressBar) alertView.findViewById(R.id.package_progress_bar)).setProgress(progressVal);
					 ((TextView) alertView.findViewById(R.id.package_progress_val)).setText(count+"/"+total);*/
					PackageLoadDialog.setMessage("Loading application " + count + " of " + total + " (" + newInfo.appname + ")");
					//PackageLoadDialog.setProgress(progressVal);
				}
			});
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
		myHandler.post(new Runnable() {
			@Override
			public void run()
			{
				PackageLoadDialog.setMessage("Sorting the list of Applications...");
			}
		});
		Comparator<PInfo> AppNameComparator = new Comparator<PInfo>(){
			public int compare(PInfo o1, PInfo o2)
			{
				return o1.getAppname().compareTo(o2.getAppname());
			}	
		};
		Collections.sort(res, AppNameComparator);
		return res; 
	}
}
