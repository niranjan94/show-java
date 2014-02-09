package com.njlabs.showjava;

import java.io.File;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Landing extends Activity {

	ListView HistoryList;
	ProgressDialog PackageLoadDialog;
	DatabaseHandler db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_landing);
		getActionBar().setIcon(R.drawable.ic_action_bar);
		
		PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);
        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
		PackageLoadDialog.setMessage("Loading Decompile History ...");
		
		HistoryList = (ListView) findViewById(R.id.list);
		PackageLoadDialog.show();
		
		db=new DatabaseHandler(this);
		
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
			SetupList(AllPackages);
			PackageLoadDialog.dismiss();
		}
			
		@Override
		protected void onPreExecute() {
			
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
				TextView PkgName=(TextView) convertView.findViewById(R.id.pkg_name);
				TextView PkgId=(TextView) convertView.findViewById(R.id.pkg_id);

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
				final TextView CPkgId=(TextView) view.findViewById(R.id.pkg_id);
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
		
	}
	@Override
	public void onResume() {
	    super.onResume();  // Always call the superclass method first
	    HistoryLoader runner = new HistoryLoader();
		runner.execute();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.landing, menu);
		return true;
	}

}
