package com.njlabs.showjava.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.njlabs.showjava.BuildConfig;
import com.njlabs.showjava.R;
import com.njlabs.showjava.modals.DecompileHistoryItem;
import com.njlabs.showjava.utils.logging.Ln;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

public class Landing extends BaseActivity {

	ProgressDialog PackageLoadDialog;
    List<DecompileHistoryItem> listFromDb;
	private static final int FILE_PICKER = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupLayout(R.layout.activity_landing);

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.navbar_header)
                .addProfiles (
                        new ProfileDrawerItem().withName("Show Java").withEmail("Version " + BuildConfig.VERSION_NAME).setSelectable(false)
                )
                .withSelectionListEnabledForSingleProfile(false)
                .build();

        Drawer result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Home").withIcon(R.drawable.ic_action_home).withCheckable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("About the app").withIcon(R.drawable.ic_action_info).withCheckable(false),
                        new PrimaryDrawerItem().withName("Settings").withIcon(R.drawable.ic_action_settings).withCheckable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        switch (position) {
                            case 2:
                                startActivity(new Intent(baseContext, About.class));
                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                break;
                            case 3:
                                //startActivity(new Intent(baseContext, SettingsActivity.class));
                                //overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                break;
                        }
                        return false;
                    }
                })
                .withCloseOnClick(true)
                .build();

        PackageLoadDialog = new ProgressDialog(this);
        PackageLoadDialog.setIndeterminate(false);
        PackageLoadDialog.setCancelable(false);
        PackageLoadDialog.setInverseBackgroundForced(false);
        PackageLoadDialog.setCanceledOnTouchOutside(false);
		PackageLoadDialog.setMessage("Loading Decompile History ...");

        HistoryLoader runner = new HistoryLoader();
        runner.execute();
				
	}
	private class HistoryLoader extends AsyncTask<String, String, List<DecompileHistoryItem>> {

		@Override
		protected List<DecompileHistoryItem> doInBackground(String... params) {
			return DecompileHistoryItem.listAll(DecompileHistoryItem.class);
		}	
		
		@Override
		protected void onPostExecute(List<DecompileHistoryItem> AllPackages) {
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

	public void SetupList(List<DecompileHistoryItem> AllPackages){

	}

	public void OpenAppListing(View v) {
		Intent i = new Intent(getApplicationContext(), AppListing.class);
		startActivity(i);
	}

	public void OpenFilePicker(View v) {
        Intent i = new Intent(this, FilePicker.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_PICKER);
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	}

    private class ExistingHistoryLoader extends AsyncTask<String, String, List<DecompileHistoryItem>> {

        @Override
        protected List<DecompileHistoryItem> doInBackground(String... params) {

            File file = new File(Environment.getExternalStorageDirectory()+"/ShowJava/sources");
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
                    if (!alreadyExists) {
                        DecompileHistoryItem newItem = new DecompileHistoryItem(directory, directory);
                        newItem.save();
                        listFromDb.add(newItem);
                    }
                }
            }
            return listFromDb;
        }

        @Override
        protected void onPostExecute(List<DecompileHistoryItem> AllPackages) {
            SetupList(AllPackages);
        }

        @Override
        protected void onPreExecute() {

        }

    }

	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_PICKER) {
            if (data != null) {

                Uri uri = data.getData();
                File apkFile = new File(uri.getPath());
                String PackageDir = apkFile.getAbsolutePath();

                Ln.d(PackageDir);

                String PackageName;
                String PackageId;

                if(FilenameUtils.isExtension(PackageDir, "apk")) {
                    PackageManager pm = getPackageManager();
                    PackageInfo info = pm.getPackageArchiveInfo(PackageDir, PackageManager.GET_ACTIVITIES);
                    if(info != null) {
                        ApplicationInfo appInfo = info.applicationInfo;

                        if(Build.VERSION.SDK_INT >= 8) {
                            appInfo.sourceDir = PackageDir;
                            appInfo.publicSourceDir = PackageDir;
                        }
						PackageName = info.applicationInfo.loadLabel(getPackageManager()).toString();
						PackageId = info.packageName;

					} else {
                        PackageName = "";
                        PackageId = "";
                    }
                    Ln.d(PackageName+" "+PackageId);
                    Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                    i.putExtra("package_id",PackageId);
                    i.putExtra("package_label",PackageName);
                    i.putExtra("package_file_path", PackageDir);
                    startActivity(i);

                }


            }
        }

    }

}
