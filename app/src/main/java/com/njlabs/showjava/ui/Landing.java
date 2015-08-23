package com.njlabs.showjava.ui;

import android.annotation.SuppressLint;
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
import com.njlabs.showjava.utils.SourceInfo;
import com.njlabs.showjava.utils.logging.Ln;
import com.nononsenseapps.filepicker.FilePickerActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@SuppressWarnings("unused")
public class Landing extends BaseActivity {

    private static final int FILE_PICKER = 0;

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
                        new PrimaryDrawerItem().withName("Report a Bug").withIcon(R.drawable.ic_action_bug_report).withCheckable(false),
                        new PrimaryDrawerItem().withName("About the app").withIcon(R.drawable.ic_action_info).withCheckable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        switch (position) {
                            case 2:
                                Uri uri = Uri.parse("https://github.com/niranjan94/show-java/issues/new");
                                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                break;
                            case 3:
                                startActivity(new Intent(baseContext, About.class));
                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                break;
                        }
                        return false;
                    }
                })
                .withCloseOnClick(true)
                .build();


        HistoryLoader historyLoader = new HistoryLoader();
        historyLoader.execute();
    }

    public void SetupList(List<SourceInfo> AllPackages) {

        if(AllPackages.size()<1){
            listView.setVisibility(View.GONE);
            welcomeLayout.setVisibility(View.VISIBLE);
        } else {
            welcomeLayout.setVisibility(View.INVISIBLE);

            ArrayAdapter<SourceInfo> decompileHistoryItemArrayAdapter = new ArrayAdapter<SourceInfo>(getBaseContext(), R.layout.history_list_item, AllPackages) {
                @SuppressLint("InflateParams")
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = getLayoutInflater().inflate(R.layout.history_list_item, null);
                    }

                    SourceInfo pkg = getItem(position);

                    ViewHolder holder = new ViewHolder();

                    holder.packageLabel = (TextView) convertView.findViewById(R.id.history_item_label);
                    holder.packageName = (TextView) convertView.findViewById(R.id.history_item_package);
                    holder.packageIcon = (ImageView) convertView.findViewById(R.id.history_item_icon);

                    convertView.setTag(holder);

                    holder.packageLabel.setText(pkg.getPackageLabel());
                    holder.packageName.setText(pkg.getPackageName());

                    if (pkg.getPackageLabel().equalsIgnoreCase(pkg.getPackageName())) {
                        holder.packageName.setVisibility(View.INVISIBLE);
                    }

                    String iconPath = Environment.getExternalStorageDirectory() + "/ShowJava/sources/" + pkg.getPackageName() + "/icon.png";

                    if (new File(iconPath).exists()) {
                        Bitmap iconBitmap = BitmapFactory.decodeFile(iconPath);
                        holder.packageIcon.setImageDrawable(new BitmapDrawable(getResources(), iconBitmap));
                    } else {
                        holder.packageIcon.setImageResource(R.drawable.generic_icon);
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

    private class HistoryLoader extends AsyncTask<String, String, List<SourceInfo>> {

        @Override
        protected List<SourceInfo> doInBackground(String... params) {

            List<SourceInfo> historyItems = new ArrayList<>();

            File dir = new File(Environment.getExternalStorageDirectory() + "/ShowJava/sources");
            if (dir.exists()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (file.isDirectory()) {
                        File infoFile = new File(file + "/info.json");
                        if(infoFile.exists() && infoFile.isFile()){
                            SourceInfo sourceInfo = SourceInfo.getSourceInfo(infoFile);
                            if(sourceInfo != null) {
                                historyItems.add(sourceInfo);
                            } else {
                                // DELETE IF INFO FILE IS MISSING
                                try {
                                    FileUtils.cleanDirectory(file);
                                    file.delete();
                                } catch (IOException e) {
                                    Ln.d(e);
                                }

                            }
                        }
                    } else {
                        file.delete();
                    }
                }
            }

            return historyItems;
        }

        @Override
        protected void onPostExecute(List<SourceInfo> AllPackages) {
            SetupList(AllPackages);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(String... text) {
        }
    }

    private void cleanOldSources(){
        File dir = new File(Environment.getExternalStorageDirectory() + "/ShowJava");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files) {
                if (!file.getName().equalsIgnoreCase("sources")) {
                    try {
                        FileUtils.cleanDirectory(file);
                        file.delete();
                    } catch (IOException e) {
                        Ln.d(e);
                    }
                }
            }
        } else {
            dir.mkdirs();
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        rerunHistoryLoader();
    }

    private void rerunHistoryLoader(){
        HistoryLoader historyLoaderTwo = new HistoryLoader();
        historyLoaderTwo.execute();
    }

}
