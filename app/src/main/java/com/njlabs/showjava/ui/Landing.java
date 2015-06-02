package com.njlabs.showjava.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.njlabs.showjava.R;
import com.njlabs.showjava.modals.DecompileHistoryItem;
import com.njlabs.showjava.utils.DatabaseHandler;
import com.njlabs.showjava.utils.picker.FileChooser;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Landing extends BaseActivity {

	ProgressDialog PackageLoadDialog;
	DatabaseHandler db;
    List<DecompileHistoryItem> listFromDb;
	private static final int FILEPICKER = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupLayout(R.layout.activity_landing);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
	    PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);
        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
		PackageLoadDialog.setMessage("Loading Decompile History ...");
		
		//HistoryList = (ListView) findViewById(R.id.list);
		
		db = new DatabaseHandler(this);
		
		if(db.getHistoryItemCount()!=0)
		{
			//HistoryList.setVisibility(View.VISIBLE);

		}
		else
		{
			/*LinearLayout EmptyLayout=(LinearLayout) findViewById(R.id.EmptyLayout);
			EmptyLayout.setVisibility(View.VISIBLE);*/
		}
        HistoryLoader runner = new HistoryLoader();
        runner.execute();
				
	}
	private class HistoryLoader extends AsyncTask<String, String, List<DecompileHistoryItem>> {

		@Override
		protected List<DecompileHistoryItem> doInBackground(String... params) {
			return db.getAllHistoryItems();
		}	
		
		@Override
		protected void onPostExecute(List<DecompileHistoryItem> AllPackages) {
			// execution of result of Long time consuming operation
            listFromDb = AllPackages;
			SetupList(AllPackages);
			PackageLoadDialog.dismiss();
            ExistingHistoryLoader runner = new ExistingHistoryLoader();
            runner.execute();
		}
			
		@Override
		protected void onPreExecute() {
			PackageLoadDialog.show();
		}
		
		@Override
		protected void onProgressUpdate(String... text) {
			PackageLoadDialog.setMessage(text[0]);
		}
	}
	public void SetupList(List<DecompileHistoryItem> AllPackages)
	{
		ArrayAdapter<DecompileHistoryItem> aa = new ArrayAdapter<DecompileHistoryItem>(getBaseContext(), R.layout.history_list_item, AllPackages)
		{
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				if (convertView == null)
				{
					convertView = getLayoutInflater().inflate(R.layout.history_list_item, null);
				}
				DecompileHistoryItem pkg = getItem(position);
				TextView PkgName = (TextView) convertView.findViewById(R.id.history_pkg_name);
				TextView PkgId = (TextView) convertView.findViewById(R.id.history_pkg_id);
				Typeface face=Typeface.createFromAsset(getAssets(), "roboto_light.ttf"); 
				
				PkgName.setTypeface(face); 
				PkgId.setTypeface(face); 
				
				PkgName.setText(pkg._packagename);
				PkgId.setText(pkg._packageid);
				
				return convertView;	
			}
		};
		//setListAdapter(aa);

		/*HistoryList.setAdapter(aa);
		HistoryList.setTextFilterEnabled(true);
		/// CREATE AN ONCLICK LISTENER TO KNOW WHEN EACH ITEM IS CLICKED
		HistoryList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				final TextView CPkgId=(TextView) view.findViewById(R.id.history_pkg_id);
				Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
				File javaSourceOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+CPkgId.getText().toString()+"/java_output");
				i.putExtra("java_source_dir",javaSourceOutputDir.toString()+"/");
				i.putExtra("package_id",CPkgId.getText().toString());
				startActivity(i);
			}
		});*/
	}
	public void OpenAppListing(View v)
	{
		Intent i = new Intent(getApplicationContext(), AppListing.class);
		startActivity(i);
	}
	public void OpenFilePicker(View v)
	{
        Intent intent = new Intent(this, FileChooser.class);
        ArrayList<String> extensions = new ArrayList<String>();
        extensions.add(".apk");
        intent.putStringArrayListExtra("filterFileExtension", extensions);
        startActivityForResult(intent, FILEPICKER);
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    if(db.getHistoryItemCount()!=0)
		{
			/*LinearLayout EmptyLayout=(LinearLayout) findViewById(R.id.EmptyLayout);
	    	EmptyLayout.setVisibility(View.GONE);
			HistoryList.setVisibility(View.VISIBLE);
			HistoryLoader runner = new HistoryLoader();
			runner.execute();*/
		}
		else
		{
			/*LinearLayout EmptyLayout=(LinearLayout) findViewById(R.id.EmptyLayout);
			EmptyLayout.setVisibility(View.VISIBLE);
			HistoryList.setVisibility(View.GONE);*/
		}
	}


    private class ExistingHistoryLoader extends AsyncTask<String, String, List<DecompileHistoryItem>> {

        @Override
        protected List<DecompileHistoryItem> doInBackground(String... params) {

            // listFromDb
            File file = new File(Environment.getExternalStorageDirectory()+"/ShowJava");
            String[] directories = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory();
                }
            });
            if(directories!=null && directories.length>0) {
                for (String directory : directories) {
                    boolean alreadyExists = false;
                    for (DecompileHistoryItem item : listFromDb) {
                        if (directory.equals(item.getPackageID())) {
                            Log.d("SaA", "Already Exists!!");
                            alreadyExists = true;
                        }
                    }

                    if (alreadyExists == false) {
                        DecompileHistoryItem newItem = new DecompileHistoryItem(directory, directory, DateFormat.getDateInstance().format(new Date()));
                        db.addHistoryItem(newItem);
                        listFromDb.add(newItem);
                    }
                }
            }
            return listFromDb;
        }

        @Override
        protected void onPostExecute(List<DecompileHistoryItem> AllPackages) {
            // execution of result of Long time consuming operation
            SetupList(AllPackages);

        }

        @Override
        protected void onPreExecute() {

        }

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
	        	Intent i=new Intent(getBaseContext(),About.class);
	        	startActivity(i);
	        	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILEPICKER) {
            if (data != null) {
                String fileSelected = data.getStringExtra("fileSelected");
                //Toast.makeText(this, fileSelected, Toast.LENGTH_SHORT).show();
                String PackageDir = fileSelected;
                String PackageName;
                String PackageId;

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
                    PackageName = info.applicationInfo.loadLabel(getPackageManager()).toString();
                    PackageId = info.packageName;
                }
                else
                {
                    PackageName=FilenameUtils.getName(PackageDir);
                    PackageId=FilenameUtils.getName(PackageDir).replaceAll(" ", "_").toLowerCase();
                }

                Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                i.putExtra("package_id",PackageId);
                i.putExtra("package_name",PackageName);
                i.putExtra("package_dir",PackageDir);
                startActivity(i);

            }
        }
    }

}
