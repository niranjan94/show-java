package com.njlabs.showjava.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

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
import com.njlabs.showjava.modals.HistoryItem;
import com.njlabs.showjava.utils.SourceInfo;
import com.njlabs.showjava.utils.logging.Ln;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import ollie.query.Select;


@SuppressWarnings("unused")
public class Landing extends BaseActivity {

    private static final int FILE_PICKER = 0;
    private ProgressDialog PackageLoadDialog;
    private List<HistoryItem> listFromDb;
    private boolean needsToCheckExisting = true;

    private LinearLayout welcomeLayout;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupLayout(R.layout.activity_landing);

        listView = (ListView) findViewById(R.id.history_list);
        View header = getLayoutInflater().inflate(R.layout.history_header_view, listView, false);
        listView.addHeaderView(header, null, false);

        welcomeLayout = (LinearLayout) findViewById(R.id.welcome_layout);

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.navbar_header)
                .addProfiles(
                        new ProfileDrawerItem().withName(getResources().getString(R.string.app_name)).withEmail("Version " + BuildConfig.VERSION_NAME).setSelectable(false)
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
                        new PrimaryDrawerItem().withName("About the app").withIcon(R.drawable.ic_action_info).withCheckable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        switch (position) {
                            case 2:
                                startActivity(new Intent(baseContext, About.class));
                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
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

        HistoryLoader historyLoader = new HistoryLoader();
        historyLoader.execute();
    }

    public void SetupList(List<HistoryItem> AllPackages) {

        if(AllPackages.size()<1){
            listView.setVisibility(View.GONE);
            welcomeLayout.setVisibility(View.VISIBLE);
        } else {
            welcomeLayout.setVisibility(View.INVISIBLE);

            ArrayAdapter<HistoryItem> decompileHistoryItemArrayAdapter = new ArrayAdapter<HistoryItem>(getBaseContext(), R.layout.history_list_item, AllPackages) {
                @SuppressLint("InflateParams")
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.history_list_item, null);
                    }

                    HistoryItem pkg = getItem(position);

                    ViewHolder holder = new ViewHolder();

                    holder.packageLabel = (TextView) convertView.findViewById(R.id.history_item_label);
                    holder.packageName = (TextView) convertView.findViewById(R.id.history_item_package);
                    holder.packageIcon = (ImageView) convertView.findViewById(R.id.history_item_icon);

                    convertView.setTag(holder);

                    holder.packageLabel.setText(pkg.getPackageLabel());
                    holder.packageName.setText(pkg.getPackageID());

                    if (pkg.getPackageLabel().equalsIgnoreCase(pkg.getPackageID())) {
                        holder.packageName.setVisibility(View.INVISIBLE);
                    }

                    String iconPath = Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + pkg.getPackageID() + "/icon.png";

                    if (new File(iconPath).exists()) {
                        Bitmap iconBitmap = BitmapFactory.decodeFile(iconPath);
                        holder.packageIcon.setImageDrawable(new BitmapDrawable(getResources(), iconBitmap));
                    }

                    return convertView;
                }
            };
            listView.setAdapter(decompileHistoryItemArrayAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final ViewHolder holder = (ViewHolder) view.getTag();
                    final File sourceDir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + holder.packageName.getText().toString() + "");
                    Intent i = new Intent(getApplicationContext(), JavaExplorer.class);
                    i.putExtra("java_source_dir", sourceDir + "/");
                    i.putExtra("package_id", holder.packageName.getText().toString());
                    startActivity(i);
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                }
            });

            listView.setVisibility(View.VISIBLE);

        }
    }

    public void OpenAppListing(View v) {
        Intent i = new Intent(getApplicationContext(), AppListing.class);
        startActivity(i);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
    }

    public void OpenFilePicker(View v) {
        Intent i = new Intent(this, FilePicker.class);

        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(i, FILE_PICKER);
        overridePendingTransition(R.anim.fadein, R.anim.fadeout);
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

                if (FilenameUtils.isExtension(PackageDir, "apk")) {
                    PackageManager pm = getPackageManager();
                    PackageInfo info = pm.getPackageArchiveInfo(PackageDir, PackageManager.GET_ACTIVITIES);
                    if (info != null) {
                        ApplicationInfo appInfo = info.applicationInfo;

                        if (Build.VERSION.SDK_INT >= 8) {
                            appInfo.sourceDir = PackageDir;
                            appInfo.publicSourceDir = PackageDir;
                        }
                        PackageName = info.applicationInfo.loadLabel(getPackageManager()).toString();
                        PackageId = info.packageName;

                    } else {
                        PackageName = "";
                        PackageId = "";
                    }

                    Ln.d(PackageName + " " + PackageId);
                    Intent i = new Intent(getApplicationContext(), AppProcessActivity.class);
                    i.putExtra("package_id", PackageId);
                    i.putExtra("package_label", PackageName);
                    i.putExtra("package_file_path", PackageDir);
                    startActivity(i);
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                }
            }
        }
    }

    private static class ViewHolder {
        TextView packageLabel;
        TextView packageName;
        ImageView packageIcon;
        int position;
    }

    private class HistoryLoader extends AsyncTask<String, String, List<HistoryItem>> {

        @Override
        protected List<HistoryItem> doInBackground(String... params) {
            return Select.from(HistoryItem.class).fetch();
        }

        @Override
        protected void onPostExecute(List<HistoryItem> AllPackages) {
            listFromDb = AllPackages;
            SetupList(AllPackages);
            PackageLoadDialog.dismiss();

            if(needsToCheckExisting){
                ExistingHistoryLoader runner = new ExistingHistoryLoader();
                runner.execute();
            } else {
                needsToCheckExisting = true;
            }
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

    private void cleanOldSources(){
        File dir = new File(Environment.getExternalStorageDirectory() + "/ShowJava");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.getName().equalsIgnoreCase("sources")) {
                    file.delete();
                }
            }
        } else {
            dir.mkdirs();
        }
    }

    private class ExistingHistoryLoader extends AsyncTask<String, String, List<HistoryItem>> {

        @Override
        protected List<HistoryItem> doInBackground(String... params) {

            cleanOldSources();

            File file = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources");
            if(!file.exists()){
                file.mkdirs();
            }
            String[] directories = file.list(new FilenameFilter() {
                @Override
                public boolean accept(File current, String name) {
                    return new File(current, name).isDirectory();
                }
            });

            if (directories != null && directories.length > 0) {
                for (String directory : directories) {
                    boolean alreadyExists = false;
                    for (HistoryItem item : listFromDb) {
                        if (directory.equalsIgnoreCase(item.getPackageID())) {
                            alreadyExists = true;
                        }
                    }
                    if (!alreadyExists) {
                        HistoryItem newItem = new HistoryItem();
                        newItem.setPackageID(directory);
                        if ((new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + directory + "/info.json")).exists()) {
                            String label = SourceInfo.getLabel(Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + directory);
                            newItem.setPackageLabel((label != null ? label : directory));
                        } else {
                            newItem = new HistoryItem(directory, directory);
                        }
                        newItem.save();
                        listFromDb.add(newItem);
                    }
                }
            }

            cleanupDatabase(directories);

            return listFromDb;
        }

        @Override
        protected void onPostExecute(List<HistoryItem> AllPackages) {
            SetupList(AllPackages);
            rerunHistoryLoader();
        }

        @Override
        protected void onPreExecute() {

        }

        private void cleanupDatabase(String[] directories) {
            int[] positions;
            for (HistoryItem item : listFromDb) {
                boolean exists = false;
                if (directories != null && directories.length > 0) {
                    for (String directory : directories) {
                        if (directory.equalsIgnoreCase(item.getPackageID())) {
                            exists = true;
                        }
                    }
                }
                if (!exists) {
                    item.delete();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        rerunHistoryLoader();
    }

    private void rerunHistoryLoader(){
        needsToCheckExisting = false;
        HistoryLoader historyLoaderTwo = new HistoryLoader();
        historyLoaderTwo.execute();
    }

}
