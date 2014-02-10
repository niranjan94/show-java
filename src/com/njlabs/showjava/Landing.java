package com.njlabs.showjava;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import ru.bartwell.exfilepicker.ExFilePicker;
import ru.bartwell.exfilepicker.ExFilePickerParcelObject;
import android.app.Activity;
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
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class Landing extends Activity {

	ListView HistoryList;
	ProgressDialog PackageLoadDialog;
	DatabaseHandler db;
	private static final int EX_FILE_PICKER_RESULT = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);
		getActionBar().setIcon(R.drawable.ic_action_bar);
		
		TextView tv=(TextView)findViewById(R.id.title_history); 
		Typeface face=Typeface.createFromAsset(getAssets(), "roboto_thin.ttf"); 
		tv.setTypeface(face); 
  		
		face=Typeface.createFromAsset(getAssets(), "roboto_light.ttf"); 
		 
		Button btn = (Button)findViewById(R.id.btn_file_pick);
		btn.setTypeface(face);
		
		btn = (Button)findViewById(R.id.btn_install_pick);
		btn.setTypeface(face);
		
		SpannableString s = new SpannableString(getResources().getString(R.string.title_activity_landing));
	    s.setSpan(new CustomTypefaceSpan("sans-serif",face), 0, s.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    s.setSpan(new RelativeSizeSpan(1.1f), 0, s.length(),Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	    
	    getActionBar().setTitle(s);
	    
	    PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);
        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
		PackageLoadDialog.setMessage("Loading Decompile History ...");
		
		HistoryList = (ListView) findViewById(R.id.list);
		
		db=new DatabaseHandler(this);
		
		if(db.getHistoryItemCount()!=0)
		{
			HistoryList.setVisibility(View.VISIBLE);
			HistoryLoader runner = new HistoryLoader();
			runner.execute();
		}
		else
		{
			LinearLayout EmptyLayout=(LinearLayout) findViewById(R.id.EmptyLayout);
			EmptyLayout.setVisibility(View.VISIBLE);	 
			tv = (TextView)findViewById(R.id.tv_history);
			tv.setTypeface(face);
			tv = (TextView)findViewById(R.id.tv_helper);
			tv.setTypeface(face);
			tv = (TextView)findViewById(R.id.tv_helper_contd);
			tv.setTypeface(face);
		}
				
	}
	private class HistoryLoader extends AsyncTask<String, String, List<DecompileHistoryItem>> {

		@Override
		protected List<DecompileHistoryItem> doInBackground(String... params) {
			return db.getAllHistoryItems();
		}	
		
		@Override
		protected void onPostExecute(List<DecompileHistoryItem> AllPackages) {
			// execution of result of Long time consuming operation
			SetupList(AllPackages);
			PackageLoadDialog.dismiss();
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
		HistoryList.setAdapter(aa);
		HistoryList.setTextFilterEnabled(true);
		/// CREATE AN ONCLICK LISTENER TO KNOW WHEN EACH ITEM IS CLICKED
		HistoryList.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				final TextView CPkgId=(TextView) view.findViewById(R.id.history_pkg_id);
				Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
				File JavaOutputDir = new File(Environment.getExternalStorageDirectory()+"/ShowJava"+"/"+CPkgId.getText().toString()+"/java_output");
				i.putExtra("java_source_dir",JavaOutputDir.toString()+"/");
				i.putExtra("package_id",CPkgId.getText().toString());
				startActivity(i);
			}
		});
	}
	public void OpenAppListing(View v)
	{
		Intent i = new Intent(getApplicationContext(), AppListing.class);
		startActivity(i);
	}
	public void OpenFilePicker(View v)
	{
		Intent intent = new Intent(getApplicationContext(), ru.bartwell.exfilepicker.ExFilePickerActivity.class);
		intent.putExtra(ExFilePicker.SET_ONLY_ONE_ITEM,true);
		intent.putExtra(ExFilePicker.SET_FILTER_BY_EXTENSION,new String[] { "apk", "jar" });
		intent.putExtra(ExFilePicker.SET_CHOICE_TYPE,ExFilePicker.CHOICE_TYPE_FILES);
		intent.putExtra(ExFilePicker.DISABLE_NEW_FOLDER_BUTTON,true);
        startActivityForResult(intent, EX_FILE_PICKER_RESULT);
	}
	
	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
	    if(db.getHistoryItemCount()!=0)
		{
			LinearLayout EmptyLayout=(LinearLayout) findViewById(R.id.EmptyLayout);
	    	EmptyLayout.setVisibility(View.GONE);
			HistoryList.setVisibility(View.VISIBLE);
			HistoryLoader runner = new HistoryLoader();
			runner.execute();
		}
		else
		{
			LinearLayout EmptyLayout=(LinearLayout) findViewById(R.id.EmptyLayout);
			EmptyLayout.setVisibility(View.VISIBLE);
			HistoryList.setVisibility(View.GONE);
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
        if (requestCode == EX_FILE_PICKER_RESULT) {
            if (data != null) {
                ExFilePickerParcelObject object = (ExFilePickerParcelObject) data.getParcelableExtra(ExFilePickerParcelObject.class.getCanonicalName());
                if (object.count > 0) {
                	String PackageDir=object.path+object.names.get(0);
                	String PackageName = null;
                	String PackageId = null;
                	
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

}
